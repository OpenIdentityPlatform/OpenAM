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

package org.forgerock.oauth2.restlet;

import org.forgerock.oauth2.core.AccessToken;
import org.forgerock.oauth2.core.AccessTokenVerifier;
import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.oauth2.core.TokenStore;
import org.forgerock.oauth2.core.exceptions.BadRequestException;
import org.forgerock.oauth2.core.exceptions.InvalidGrantException;
import org.forgerock.oauth2.core.exceptions.ServerException;
import org.restlet.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    /**
     * {@inheritDoc}
     */
    protected String obtainTokenId(OAuth2Request request) {
        final Request req = request.getRequest();

        if (req.getChallengeResponse() == null) {
            logger.debug("Request does not contain Authorization header.");
            return null;
        }

        return req.getChallengeResponse().getRawValue();
    }
}
