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
 * Copyright 2014 ForgeRock AS.
 */

package org.forgerock.openam.oauth2;

import com.sun.identity.shared.debug.Debug;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.oauth2.core.AccessToken;
import org.forgerock.oauth2.core.AuthorizationCode;
import org.forgerock.oauth2.core.OAuth2Constants;
import org.forgerock.oauth2.core.OAuth2ProviderSettings;
import org.forgerock.oauth2.core.OAuth2ProviderSettingsFactory;
import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.oauth2.core.RefreshToken;
import org.forgerock.oauth2.core.exceptions.BadRequestException;
import org.forgerock.oauth2.core.exceptions.InvalidClientException;
import org.forgerock.oauth2.core.exceptions.InvalidGrantException;
import org.forgerock.oauth2.core.exceptions.InvalidRequestException;
import org.forgerock.oauth2.core.exceptions.ServerException;
import org.forgerock.openam.cts.exceptions.CoreTokenException;
import org.forgerock.openam.openidconnect.OpenAMOpenIdConnectToken;
import org.forgerock.openidconnect.OpenIdConnectClientRegistration;
import org.forgerock.openidconnect.OpenIdConnectClientRegistrationStore;
import org.forgerock.openidconnect.OpenIdConnectToken;
import org.forgerock.openidconnect.OpenIdConnectTokenStore;
import org.restlet.Request;
import org.restlet.data.Status;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.nio.charset.Charset;
import java.security.PrivateKey;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Implementation of the OpenId Connect Token Store which the OpenId Connect Provider will implement.
 *
 * @since 12.0.0
 */
@Singleton
public class OpenAMTokenStore implements OpenIdConnectTokenStore {

    private final Debug logger = Debug.getInstance("OAuth2Provider");
    private final OAuthTokenStore tokenStore;
    private final OAuth2ProviderSettingsFactory providerSettingsFactory;
    private final OpenIdConnectClientRegistrationStore clientRegistrationStore;
    private final RealmNormaliser realmNormaliser;

    /**
     * Constructs a new OpenAMTokenStore.
     *
     * @param tokenStore An instance of the OAuthTokenStore.
     * @param providerSettingsFactory An instance of the OAuth2ProviderSettingsFactory.
     * @param clientRegistrationStore An instance of the OpenIdConnectClientRegistrationStore.
     * @param realmNormaliser An instance of the RealmNormaliser.
     */
    @Inject
    public OpenAMTokenStore(OAuthTokenStore tokenStore, OAuth2ProviderSettingsFactory providerSettingsFactory,
            OpenIdConnectClientRegistrationStore clientRegistrationStore, RealmNormaliser realmNormaliser) {
        this.tokenStore = tokenStore;
        this.providerSettingsFactory = providerSettingsFactory;
        this.clientRegistrationStore = clientRegistrationStore;
        this.realmNormaliser = realmNormaliser;
    }

    /**
     * {@inheritDoc}
     */
    public AuthorizationCode createAuthorizationCode(Set<String> scope, String resourceOwnerId, String clientId,
            String redirectUri, String nonce, OAuth2Request request) throws ServerException {

        logger.message("DefaultOAuthTokenStoreImpl::Creating Authorization code");
        final OAuth2ProviderSettings providerSettings = providerSettingsFactory.get(request);
        final String code = UUID.randomUUID().toString();
        final long expiryTime = (providerSettings.getAuthorizationCodeLifetime() * 1000) + System.currentTimeMillis();

        final AuthorizationCode authorizationCode = new OpenAMAuthorizationCode(code, resourceOwnerId, clientId,
                redirectUri, scope, expiryTime, nonce, realmNormaliser.normalise(request.<String>getParameter("realm")));

        // Store in CTS
        try {
            tokenStore.create(authorizationCode);
        } catch (CoreTokenException e) {
            logger.error("Unable to create authorization code " + authorizationCode.getTokenInfo(), e);
            throw new ServerException("Could not create token in CTS");
        }

        return authorizationCode;
    }

    /**
     * {@inheritDoc}
     */
    public OpenIdConnectToken createOpenIDToken(String resourceOwnerId, String clientId, String authorizationParty,
            String nonce, String ops, OAuth2Request request) throws ServerException, InvalidClientException {

        final OAuth2ProviderSettings providerSettings = providerSettingsFactory.get(request);

        final OpenIdConnectClientRegistration clientRegistration = clientRegistrationStore.get(clientId, request);
        final PrivateKey privateKey = providerSettings.getServerKeyPair().getPrivate();
        final String algorithm = clientRegistration.getIDTokenSignedResponseAlgorithm();

        final long timeInSeconds = System.currentTimeMillis()/1000;
        final long tokenLifetime = providerSettings.getOpenIdTokenLifetime();
        final long exp = timeInSeconds + tokenLifetime;

        final long iat = timeInSeconds;
        final long ath = timeInSeconds;
        final String realm = realmNormaliser.normalise(request.<String>getParameter("realm"));

        final Request req = request.getRequest();
        final String iss = req.getHostRef().toString() + "/" + req.getResourceRef().getSegments().get(0);

        return new OpenAMOpenIdConnectToken(clientRegistration.getClientSecret().getBytes(Charset.forName("UTF-8")),
                algorithm, iss, resourceOwnerId, clientId, authorizationParty, exp, iat, ath, nonce, ops, realm);
    }

    /**
     * {@inheritDoc}
     */
    public AccessToken createAccessToken(String grantType, String accessTokenType, String authorizationCode,
            String resourceOwnerId, String clientId, String redirectUri, Set<String> scope, RefreshToken refreshToken,
            String nonce, OAuth2Request request) throws ServerException {

        final OAuth2ProviderSettings providerSettings = providerSettingsFactory.get(request);
        final String id = UUID.randomUUID().toString();
        final long expiryTime = (providerSettings.getAccessTokenLifetime() * 1000) + System.currentTimeMillis();

        final AccessToken accessToken;
        if (refreshToken == null) {
            accessToken = new OpenAMAccessToken(id, authorizationCode, resourceOwnerId, clientId, redirectUri, scope,
                    expiryTime, null, OAuth2Constants.Token.OAUTH_ACCESS_TOKEN, grantType, nonce,
                    realmNormaliser.normalise(request.<String>getParameter("realm")));
        } else {
            accessToken = new OpenAMAccessToken(id, authorizationCode, resourceOwnerId, clientId, redirectUri, scope,
                    expiryTime, refreshToken.getTokenId(), OAuth2Constants.Token.OAUTH_ACCESS_TOKEN, grantType, nonce,
                    realmNormaliser.normalise(request.<String>getParameter("realm")));
        }

        try {
            tokenStore.create(accessToken);
        } catch (CoreTokenException e) {
            if (logger.errorEnabled()) {
                logger.error("Could not create token in CTS: " + e.getMessage());
            }
            throw new ServerException("Could not create token in CTS: " + e.getMessage());
        }

        return accessToken;
    }

    /**
     * {@inheritDoc}
     */
    public RefreshToken createRefreshToken(String grantType, String clientId, String resourceOwnerId,
            String redirectUri, Set<String> scope, OAuth2Request request) throws ServerException {

        final String realm = realmNormaliser.normalise(request.<String>getParameter("realm"));

        logger.message("Create refresh token");
        final OAuth2ProviderSettings providerSettings = providerSettingsFactory.get(request);

        final String id = UUID.randomUUID().toString();
        final long expiryTime = (providerSettings.getRefreshTokenLifetime() * 1000) + System.currentTimeMillis();

        RefreshToken refreshToken = new OpenAMRefreshToken(id, resourceOwnerId, clientId, redirectUri, scope,
                expiryTime, "Bearer", OAuth2Constants.Token.OAUTH_REFRESH_TOKEN, grantType, realm);

        try {
            tokenStore.create(refreshToken);
        } catch (CoreTokenException e) {
            logger.error("Unable to create refresh token: " + refreshToken.getTokenInfo(), e);
            throw new ServerException("Could not create token in CTS: " + e.getMessage());
        }

        return refreshToken;
    }

    /**
     * {@inheritDoc}
     */
    public AuthorizationCode readAuthorizationCode(String code) throws InvalidGrantException, ServerException {
        if (logger.messageEnabled()) {
            logger.message("Reading Authorization code: " + code);
        }
        final JsonValue token;

        // Read from CTS
        try {
            token = tokenStore.read(code);
        } catch (CoreTokenException e) {
            logger.error("Unable to read authorization code corresponding to id: " + code, e);
            throw new ServerException("Could not read token from CTS: " + e.getMessage());
        }

        if (token == null) {
            logger.error("Unable to read authorization code corresponding to id: " + code);
            throw new InvalidGrantException("The provided access grant is invalid, expired, or revoked.");
        }

        return new OpenAMAuthorizationCode(token);
    }

    /**
     * {@inheritDoc}
     */
    public void updateAuthorizationCode(AuthorizationCode authorizationCode) {
        deleteAuthorizationCode(authorizationCode.getTokenId());

        // Store in CTS
        try {
            tokenStore.create(authorizationCode);
        } catch (CoreTokenException e) {
            logger.error("DefaultOAuthTokenStoreImpl::Unable to create authorization code "
                    + authorizationCode.getTokenInfo(), e);
            throw new OAuthProblemException(Status.SERVER_ERROR_INTERNAL.getCode(),
                    "Internal error", "Could not create token in CTS", null);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void deleteAuthorizationCode(String authorizationCode) {
        if (logger.messageEnabled()){
            logger.message("DefaultOAuthTokenStoreImpl::Deleting Authorization code: " + authorizationCode);
        }
        JsonValue oAuthToken;

        // Read from CTS
        try {
            oAuthToken = tokenStore.read(authorizationCode);
        } catch (CoreTokenException e) {
            logger.error("DefaultOAuthTokenStoreImpl::Unable to read authorization code corresponding to id: "
                    + authorizationCode, e);
            throw new OAuthProblemException(Status.SERVER_ERROR_INTERNAL.getCode(),
                    "Internal error", "Could not read token from CTS: " + e.getMessage(), null);
        }

        if (oAuthToken == null) {
            logger.error("DefaultOAuthTokenStoreImpl::Unable to read authorization code corresponding to id: "
                    + authorizationCode);
            throw new OAuthProblemException(Status.CLIENT_ERROR_NOT_FOUND.getCode(), "Not found",
                    "Could not find token using CTS", null);
        }

        // Delete the code
        try {
            tokenStore.delete(authorizationCode);
        } catch (CoreTokenException e) {
            logger.error("DefaultOAuthTokenStoreImpl::Unable to delete authorization code corresponding to id: "
                    + authorizationCode, e);
            throw new OAuthProblemException(Status.SERVER_ERROR_INTERNAL.getCode(),
                    "Internal error", "Could not delete token from CTS: " + e.getMessage(), null);
        }
    }

    /**
     * {@inheritDoc}
     */
    public JsonValue queryForToken(String tokenId) throws InvalidRequestException {

        JsonValue results;

        //construct the filter
        Map<String, Object> query = new HashMap<String, Object>();
        query.put(OAuth2Constants.CoreTokenParams.PARENT, tokenId);
        query.put(OAuth2Constants.CoreTokenParams.REFRESH_TOKEN, tokenId);

        try {
            results = tokenStore.query(query);
        } catch (CoreTokenException e) {
            logger.error("Unable to query refresh token corresponding to id: " + tokenId, e);
            throw new InvalidRequestException();
        }

        return results;
    }

    /**
     * {@inheritDoc}
     */
    public void deleteAccessToken(String accessTokenId) throws ServerException {
        logger.message("Deleting access token");

        // Delete the code
        try {
            tokenStore.delete(accessTokenId);
        } catch (CoreTokenException e) {
            logger.error("Unable to delete access token corresponding to id: " + accessTokenId, e);
            throw new ServerException("Could not delete token from CTS: " + e.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     */
    public void deleteRefreshToken(String refreshTokenId) throws InvalidRequestException {

        // Delete the code
        try {
            tokenStore.delete(refreshTokenId);
        } catch (CoreTokenException e) {
            logger.error("Unable to delete refresh token corresponding to id: " + refreshTokenId, e);
            throw new InvalidRequestException();
        }
    }

    /**
     * {@inheritDoc}
     */
    public AccessToken readAccessToken(String tokenId) throws ServerException, BadRequestException,
            InvalidGrantException {

        logger.message("Reading access token");

        JsonValue token;

        // Read from CTS
        try {
            token = tokenStore.read(tokenId);
        } catch (CoreTokenException e) {
            logger.error("Unable to read access token corresponding to id: " + tokenId, e);
            throw new ServerException("Could not read token in CTS: " + e.getMessage());
        }

        if (token == null) {
            logger.error("Unable to read access token corresponding to id: " + tokenId);
            throw new InvalidGrantException("Could not read token in CTS");
        }

        return new OpenAMAccessToken(token);
    }

    /**
     * {@inheritDoc}
     */
    public RefreshToken readRefreshToken(String tokenId) throws BadRequestException, InvalidRequestException,
            InvalidGrantException {

        logger.message("Read refresh token");
        JsonValue token;

        try {
            token = tokenStore.read(tokenId);
        } catch (CoreTokenException e) {
            logger.error("Unable to read refresh token corresponding to id: " + tokenId, e);
            throw new InvalidRequestException();
        }

        if (token == null) {
            logger.error("Unable to read refresh token corresponding to id: " + tokenId);
            throw new BadRequestException("Could not find token from CTS");
        }

        return new OpenAMRefreshToken(token);
    }
}
