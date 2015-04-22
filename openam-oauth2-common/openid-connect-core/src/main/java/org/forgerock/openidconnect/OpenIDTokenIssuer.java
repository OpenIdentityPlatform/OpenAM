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

package org.forgerock.openidconnect;

import java.security.SignatureException;
import java.util.AbstractMap;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import org.forgerock.json.jose.jws.SignedJwt;
import org.forgerock.oauth2.core.AccessToken;
import org.forgerock.oauth2.core.OAuth2Constants;
import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.oauth2.core.ResourceOwner;
import org.forgerock.oauth2.core.ResourceOwnerSessionValidator;
import org.forgerock.oauth2.core.exceptions.InvalidClientException;
import org.forgerock.oauth2.core.exceptions.NotFoundException;
import org.forgerock.oauth2.core.exceptions.OAuth2Exception;
import org.forgerock.oauth2.core.exceptions.ServerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Issues OpenId Connect tokens and stores them in the OpenID Connect Token Store, when an access token is required
 * and the OAuth2 request scope contains 'openid'.
 *
 * @since 12.0.0
 */
public class OpenIDTokenIssuer {

    private final Logger logger = LoggerFactory.getLogger("OAuth2Provider");
    private final OpenIdConnectTokenStore tokenStore;
    private final ResourceOwnerSessionValidator resourceOwnerSessionValidator;

    /**
     * Constructs a new OpenIDTokenIssuer.
     *
     * @param tokenStore An instance of the OpenIdConnectTokenStore.
     */
    @Inject
    public OpenIDTokenIssuer(OpenIdConnectTokenStore tokenStore,
                             ResourceOwnerSessionValidator resourceOwnerSessionValidator) {
        this.tokenStore = tokenStore;
        this.resourceOwnerSessionValidator = resourceOwnerSessionValidator;
    }

    /**
     * Issues an OpenId Connect token, using the details of the access token.
     *
     * @param accessToken The access token requested by the OAuth2 request.
     * @param request The OAuth2 request.
     * @return A {@code Map.Entry} of the token name with the Token instance.
     * @throws ServerException If any internal server error occurs.
     * @throws InvalidClientException If either the request does not contain the client's id or the client fails to be
     *          authenticated.
     * @throws NotFoundException If the realm does not have an OAuth 2.0 provider service.
     */
    public Map.Entry<String, String> issueToken(AccessToken accessToken, OAuth2Request request)
            throws ServerException, InvalidClientException, NotFoundException {

        final Set<String> scope = accessToken.getScope();
        if (scope != null && scope.contains(OAuth2Constants.Params.OPENID)) {

            final ResourceOwner resourceOwner;
            try {
                request.setSession(accessToken.getSessionId());

                resourceOwner = resourceOwnerSessionValidator.validate(request);

                final String nonce = accessToken.getNonce();
                final OpenIdConnectToken openIdToken = tokenStore.createOpenIDToken(
                        resourceOwner,
                        accessToken.getClientId(),
                        accessToken.getClientId(),
                        nonce,
                        getOps(accessToken, request),
                        request);
                final SignedJwt signedJwt = openIdToken.sign();
                return new AbstractMap.SimpleEntry<String, String>(
                        OAuth2Constants.JWTTokenParams.ID_TOKEN, signedJwt.build());
            } catch (SignatureException e) {
                logger.error("Unable to sign JWT", e);
                throw new ServerException("Cant sign JWT");
            } catch (OAuth2Exception e) {
                logger.error("User must be authenticated to issue ID tokens.", e);
                throw new ServerException("User must be authenticated to issue ID tokens.");
            }

        }

        return null;
    }

    /**
     * Gets the ops value for the OpenId Token.
     *
     * @param accessToken The access token requested by the OAuth2 request.
     * @param request The OAuth2 request.
     * @return The ops value.
     */
    protected String getOps(AccessToken accessToken, OAuth2Request request) {
        return null;
    }
}
