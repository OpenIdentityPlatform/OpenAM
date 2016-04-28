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

package org.forgerock.oauth2.restlet;

import org.forgerock.oauth2.core.AccessTokenVerifier;
import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.oauth2.core.TokenStore;
import org.restlet.Request;
import org.restlet.data.ChallengeResponse;
import org.restlet.data.ChallengeScheme;
import org.restlet.data.Header;
import org.restlet.engine.adapter.HttpRequest;
import org.restlet.engine.header.HeaderConstants;
import org.restlet.util.Series;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Verifies that a OAuth2 request that is made to one of the protected endpoints on the OAuth2 provider,
 * (i.e. tokeninfo, userinfo) contains a valid access token specified in the request header.
 *
 * @since 12.0.0
 */
@Singleton
public class RestletHeaderAccessTokenVerifier extends AccessTokenVerifier {

    @Inject
    public RestletHeaderAccessTokenVerifier(TokenStore tokenStore) {
        super(tokenStore);
    }

    /**
     * {@inheritDoc}
     */
    protected String obtainTokenId(OAuth2Request request) {
        final Request req = request.getRequest();
        ChallengeResponse result = getChallengeResponse(req);

        if (result == null) {
            logger.debug("Request does not contain Authorization header.");
            return null;
        }

        return result.getRawValue();
    }

    /**
     * Returns the authentication response sent by a client to an origin server
     * instead of org.restlet.engine.adapter.HttpRequest.
     *
     * @return The authentication response sent by a client to an origin server.
     */
    public ChallengeResponse getChallengeResponse(Request request) {
        if (request instanceof HttpRequest) {
            // Extract the header value
            final Series<Header> headers = ((HttpRequest)request).getHttpCall().getRequestHeaders();
            final String authorization = headers.getValues(HeaderConstants.HEADER_AUTHORIZATION);

            if (authorization != null) {
                int space = authorization.indexOf(' ');

                if (space != -1) {
                    String scheme = authorization.substring(0, space);

                    if (scheme.equalsIgnoreCase("Bearer")) {
                        ChallengeResponse result = new ChallengeResponse(new ChallengeScheme("HTTP_"
                                + scheme, scheme));
                        result.setRawValue(authorization.substring(space + 1));
                        request.setChallengeResponse(result);
                        return result;
                    }
                }
            }
        }
        return request.getChallengeResponse();
    }

}
