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

import org.forgerock.oauth2.core.AccessToken;
import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.oauth2.core.exceptions.InvalidClientException;
import org.forgerock.oauth2.core.exceptions.ServerException;
import org.forgerock.json.jose.jws.SignedJwt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.security.SignatureException;
import java.util.AbstractMap;
import java.util.Map;
import java.util.Set;

/**
 * Issues OpenId Connect tokens and stores them in the OpenID Connect Token Store, when an access token is required
 * and the OAuth2 request scope contains 'openid'.
 *
 * @since 12.0.0
 */
public class OpenIDTokenIssuer {

    private final Logger logger = LoggerFactory.getLogger("OAuth2Provider");
    private final OpenIdConnectTokenStore tokenStore;

    /**
     * Constructs a new OpenIDTokenIssuer.
     *
     * @param tokenStore An instance of the OpenIdConnectTokenStore.
     */
    @Inject
    public OpenIDTokenIssuer(OpenIdConnectTokenStore tokenStore) {
        this.tokenStore = tokenStore;
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
     */
    public Map.Entry<String, String> issueToken(AccessToken accessToken, OAuth2Request request)
            throws ServerException, InvalidClientException {

        final Set<String> scope = accessToken.getScope();
        if (scope != null && scope.contains("openid")) {
            final String nonce = accessToken.getNonce();
            final OpenIdConnectToken openIdToken = tokenStore.createOpenIDToken(
                    accessToken.getResourceOwnerId(),
                    accessToken.getClientId(),
                    accessToken.getClientId(),
                    nonce,
                    getOps(accessToken, request),
                    request);
            final SignedJwt signedJwt;
            try {
                signedJwt = openIdToken.sign();
            } catch (SignatureException e) {
                logger.error("Unable to sign JWT", e);
                throw new ServerException("Cant sign JWT");
            }

            return new AbstractMap.SimpleEntry<String, String>("id_token", signedJwt.build());
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
