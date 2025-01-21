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
 * Copyright 2014-2016 ForgeRock AS.
 * Portions copyright 2025 3A Systems LLC.
 */

package org.forgerock.openidconnect;

import jakarta.inject.Inject;

import org.forgerock.json.jose.common.JwtReconstruction;
import org.forgerock.json.jose.jws.SignedJwt;
import org.forgerock.json.jose.jwt.JwtClaimsSet;
import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.oauth2.core.exceptions.BadRequestException;
import org.forgerock.oauth2.core.exceptions.ServerException;
import org.forgerock.openam.oauth2.OAuth2Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * OpenId Connect service for ending OpenId Connect session.
 *
 * @since 12.0.0
 */
public class OpenIDConnectEndSession {

    private final Logger logger = LoggerFactory.getLogger("OAuth2Provider");
    private final OpenIDConnectProvider openIDConnectProvider;

    /**
     * Constructs a new OpenIdConnectEndSession.
     *
     * @param openIDConnectProvider An instance of the OpenIDConnectProvider.
     */
    @Inject
    public OpenIDConnectEndSession(final OpenIDConnectProvider openIDConnectProvider) {
        this.openIDConnectProvider = openIDConnectProvider;
    }

    /**
     * Ends an OpenId Connect session.
     *
     *
     * @param request The request.
     * @param idToken The OpenId Token.
     * @throws BadRequestException If the request is malformed.
     * @throws ServerException If any internal server error occurs.
     */
    public void endSession(OAuth2Request request, String idToken) throws BadRequestException, ServerException {

        if (idToken == null || idToken.isEmpty()) {
            logger.warn("No id_token_hint parameter supplied to the endSession endpoint");
            throw new BadRequestException("The endSession endpoint requires an id_token_hint parameter");
        }
        JwtReconstruction jwtReconstruction = new JwtReconstruction();
        SignedJwt jwt = jwtReconstruction.reconstructJwt(idToken, SignedJwt.class);

        JwtClaimsSet claims = jwt.getClaimsSet();
        String opsId = (String) claims.getClaim(OAuth2Constants.JWTTokenParams.OPS);
        if (opsId == null) {
            opsId = (String) claims.getClaim(OAuth2Constants.JWTTokenParams.LEGACY_OPS);
        }

        request.setToken(OpenIdConnectToken.class, new OpenIdConnectToken(claims));

        openIDConnectProvider.destroySession(opsId);
    }
}
