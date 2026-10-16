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
 * Portions copyright 2025-2026 3A Systems LLC.
 */

package org.forgerock.openidconnect;

import jakarta.inject.Inject;

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
     * @param idToken The verified OpenId Token supplied as the id_token_hint.
     * @throws BadRequestException If the request is malformed.
     * @throws ServerException If any internal server error occurs.
     */
    public void endSession(OAuth2Request request, SignedJwt idToken) throws BadRequestException, ServerException {

        if (idToken == null) {
            // Unreachable from the endpoint - IdTokenHintValidator refuses an empty hint before it
            // can produce a token - but this is public, and a null here would otherwise be an NPE.
            logger.warn("No verified id_token supplied to endSession");
            throw new BadRequestException("Ending a session requires a verified id_token");
        }

        // GHSA-6f8c-crwq-jqm3: this method used to take the hint as a string and parse it here,
        // which left the ops claim below - the identifier of the session to destroy - resting on a
        // JWT nothing had verified. It now takes a token that IdTokenHintValidator has checked, so
        // the verification cannot be skipped by a caller.
        JwtClaimsSet claims = idToken.getClaimsSet();
        // Verified only means this provider signed it. A client registered for HMAC holds the key
        // its own id_tokens are signed with, so it writes every claim here; casting one that is not
        // a string would throw past the OAuth2Exception handler of the endpoint as a server error.
        String opsId = sessionIdentifier(claims, OAuth2Constants.JWTTokenParams.OPS);
        if (opsId == null) {
            opsId = sessionIdentifier(claims, OAuth2Constants.JWTTokenParams.LEGACY_OPS);
        }

        request.setToken(OpenIdConnectToken.class, new OpenIdConnectToken(claims));

        openIDConnectProvider.destroySession(opsId);
    }

    /**
     * Reads a claim naming the session to destroy, treating one that is not a string as one that is
     * not there. No session is identified either way, and destroySession reports a session it
     * cannot find as the ServerException the endpoint already tolerates.
     */
    private String sessionIdentifier(JwtClaimsSet claims, String claim) {
        final Object value = claims.getClaim(claim);
        if (value != null && !(value instanceof String)) {
            logger.warn("The id_token supplied to the endSession endpoint names its session in a '{}' claim that "
                    + "is not a string", claim);
            return null;
        }
        return (String) value;
    }
}
