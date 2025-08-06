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
package org.forgerock.oauth2.restlet;

import org.forgerock.oauth2.core.AccessTokenVerifier;
import org.forgerock.openam.oauth2.OAuth2Constants;
import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.oauth2.core.TokenStore;
import org.restlet.Request;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.representation.Representation;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

/**
 * Verifies that a OAuth2 request that is made to one of the protected endpoints on the OAuth2 provider,
 * (i.e. tokeninfo, userinfo) contains a valid access token specified in the request body.
 *
 * @since 12.0.0
 */
@Singleton
public class RestletFormBodyAccessTokenVerifier extends AccessTokenVerifier {

    @Inject
    public RestletFormBodyAccessTokenVerifier(TokenStore tokenStore) {
        super(tokenStore);
    }

    /**
     * {@inheritDoc}
     */
    protected String obtainTokenId(OAuth2Request request) {
        final Request req = request.getRequest();
        final Representation body = req.getEntity();

        if (body == null || !MediaType.APPLICATION_WWW_FORM.equals(body.getMediaType())) {
            logger.debug("Request does not contain form.");
            return null;
        }

        Form formBody = new Form(body);

        if (!formBody.getNames().contains(OAuth2Constants.Params.ACCESS_TOKEN)) {
            logger.debug("Request form does not contain access_token.");
            return null;
        }

        return formBody.getFirstValue(OAuth2Constants.Params.ACCESS_TOKEN);
    }
}
