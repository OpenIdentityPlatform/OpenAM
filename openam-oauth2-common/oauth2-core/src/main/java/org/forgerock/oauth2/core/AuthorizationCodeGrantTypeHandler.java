/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2014-2015 ForgeRock AS.
 */

package org.forgerock.oauth2.core;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.forgerock.oauth2.core.exceptions.InvalidClientException;
import org.forgerock.oauth2.core.exceptions.InvalidCodeException;
import org.forgerock.oauth2.core.exceptions.InvalidGrantException;
import org.forgerock.oauth2.core.exceptions.InvalidRequestException;
import org.forgerock.oauth2.core.exceptions.NotFoundException;
import org.forgerock.oauth2.core.exceptions.RedirectUriMismatchException;
import org.forgerock.oauth2.core.exceptions.ServerException;
import org.forgerock.util.encode.Base64url;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.forgerock.oauth2.core.Utils.joinScope;

/**
 * Implementation of the GrantTypeHandler for the OAuth2 Authorization Code grant.
 *
 * @since 12.0.0
 */
@Singleton
public class AuthorizationCodeGrantTypeHandler extends GrantTypeHandler {

    private final Logger logger = LoggerFactory.getLogger("OAuth2Provider");
    private final List<AuthorizationCodeRequestValidator> requestValidators;
    private final TokenStore tokenStore;
    private final TokenInvalidator tokenInvalidator;
    private final GrantTypeAccessTokenGenerator accessTokenGenerator;

    /**
     * Constructs a new AuthorizationCodeGrantTypeHandler.
     *
     * @param requestValidators A {@code List} of AuthorizationCodeRequestValidator.
     * @param clientAuthenticator An instance of the ClientAuthenticator.
     * @param tokenStore An instance of the TokenStore.
     * @param tokenInvalidator An instance of the TokenInvalidator.
     * @param providerSettingsFactory An instance of the OAuth2ProviderSettingsFactory.
     * @param urisFactory An instance of the OAuthUrisFactory.
     * @param accessTokenGenerator An instance of the GrantTypeAccessTokenGenerator.
     */
    @Inject
    public AuthorizationCodeGrantTypeHandler(List<AuthorizationCodeRequestValidator> requestValidators,
            ClientAuthenticator clientAuthenticator, TokenStore tokenStore, TokenInvalidator tokenInvalidator,
            OAuth2ProviderSettingsFactory providerSettingsFactory, OAuth2UrisFactory urisFactory,
            GrantTypeAccessTokenGenerator accessTokenGenerator) {
        super(providerSettingsFactory, urisFactory, clientAuthenticator);
        this.requestValidators = requestValidators;
        this.tokenStore = tokenStore;
        this.tokenInvalidator = tokenInvalidator;
        this.accessTokenGenerator = accessTokenGenerator;
    }

    /**
     * {@inheritDoc}
     */
    public AccessToken handle(OAuth2Request request, ClientRegistration clientRegistration,
            OAuth2ProviderSettings providerSettings) throws RedirectUriMismatchException, InvalidClientException,
            InvalidRequestException,  InvalidCodeException, InvalidGrantException,
            ServerException, NotFoundException {

        for (final AuthorizationCodeRequestValidator requestValidator : requestValidators) {
            requestValidator.validateRequest(request, clientRegistration);
        }

        final String code = request.getParameter(OAuth2Constants.Params.CODE);
        final String redirectUri = request.getParameter(OAuth2Constants.Params.REDIRECT_URI);

        final AuthorizationCode authorizationCode = tokenStore.readAuthorizationCode(request, code);

        if (authorizationCode == null) {
            logger.error("Authorization code doesn't exist, " + code);
            throw new InvalidRequestException("Authorization code doesn't exist.");
        }

        final String codeVerifier = request.getParameter(OAuth2Constants.Custom.CODE_VERIFIER);
        if (providerSettings.isCodeVerifierRequired()) {
            if (codeVerifier == null) {
                String message = "code_verifier parameter required";
                throw new InvalidRequestException(message);
            }
        }

        AccessToken accessToken;
        Set<String> authorizationScope;
        // Only allow one request per code through here at a time, to prevent replay.
        synchronized (code.intern()) {
            if (authorizationCode.isIssued()) {
                tokenInvalidator.invalidateTokens(code);
                tokenStore.deleteAuthorizationCode(code);
                logger.error("Authorization Code has already been issued, " + code);
                throw new InvalidGrantException();
            }

            if (!authorizationCode.getRedirectUri().equalsIgnoreCase(redirectUri)) {
                logger.error("Authorization code was issued with a different redirect URI, " + code + ". Expected, "
                        + authorizationCode.getRedirectUri() + ", actual, " + redirectUri);
                throw new InvalidGrantException();
            }

            if (!authorizationCode.getClientId().equalsIgnoreCase(clientRegistration.getClientId())) {
                logger.error("Authorization Code was issued to a different client, " + code + ". Expected, "
                        + authorizationCode.getClientId() + ", actual, " + clientRegistration.getClientId());
                throw new InvalidGrantException();
            }

            if (authorizationCode.isExpired()) {
                logger.error("Authorization code has expired, " + code);
                throw new InvalidCodeException("Authorization code expired.");
            }

            if (providerSettings.isCodeVerifierRequired()) {
                checkCodeVerifier(authorizationCode, codeVerifier);
            }

            final String grantType = request.getParameter(OAuth2Constants.Params.GRANT_TYPE);
            authorizationScope = authorizationCode.getScope();
            final String resourceOwnerId = authorizationCode.getResourceOwnerId();
            final String validatedClaims = providerSettings.validateRequestedClaims(
                    authorizationCode.getStringProperty(OAuth2Constants.Custom.CLAIMS));

            accessToken = accessTokenGenerator.generateAccessToken(providerSettings, grantType,
                    clientRegistration.getClientId(), resourceOwnerId, redirectUri, authorizationScope, validatedClaims,
                    code, authorizationCode.getNonce(), request);

            authorizationCode.setIssued();
            tokenStore.updateAuthorizationCode(authorizationCode);
        }

        final String nonce = authorizationCode.getNonce();
        accessToken.addExtraData(OAuth2Constants.Custom.NONCE, nonce);
        accessToken.addExtraData(OAuth2Constants.Custom.SSO_TOKEN_ID, authorizationCode.getSessionId());

        providerSettings.additionalDataToReturnFromTokenEndpoint(accessToken, request);
        accessToken.addExtraData(OAuth2Constants.Custom.SSO_TOKEN_ID, null);

        // We should report the scope originally consented to and not the scope added to this request
        if (authorizationScope != null && !authorizationScope.isEmpty()) {
            accessToken.addExtraData(OAuth2Constants.Params.SCOPE, joinScope(authorizationScope));
        }

        return accessToken;
    }

    private void checkCodeVerifier(AuthorizationCode authorizationCode, String codeVerifier) throws
            InvalidGrantException, InvalidRequestException {
        final String codeChallenge = authorizationCode.getCodeChallenge();
        final String codeChallengeMethod = authorizationCode.getCodeChallengeMethod();

        if (OAuth2Constants.Custom.CODE_CHALLENGE_METHOD_PLAIN.equals(codeChallengeMethod)) {
            checkCodeChallenge(codeChallenge, codeVerifier);
        } else if (OAuth2Constants.Custom.CODE_CHALLENGE_METHOD_S_256.equals(codeChallengeMethod)){
            String encodedCodeVerifier = null;
            try {
                encodedCodeVerifier = Base64url.encode(
                        MessageDigest.getInstance("SHA-256").digest(codeVerifier.getBytes(StandardCharsets.US_ASCII)));
                checkCodeChallenge(codeChallenge, encodedCodeVerifier);
            } catch (NoSuchAlgorithmException e) {
                logger.error("Error encoding code verifier.");
                throw new InvalidGrantException();
            }
        } else {
            throw new InvalidRequestException("Invalid code challenge method specified.");
        }
    }

    private void checkCodeChallenge(String codeChallenge, String codeVerifier) throws InvalidGrantException {
        if (!MessageDigest.isEqual(codeVerifier.getBytes(), codeChallenge.getBytes())) {
            logger.error("Incorrect Code Verifier,");
            throw new InvalidGrantException();
        }
    }
}
