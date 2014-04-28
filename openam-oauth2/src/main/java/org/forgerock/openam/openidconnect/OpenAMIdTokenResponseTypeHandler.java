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

package org.forgerock.openam.openidconnect;

import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.openidconnect.IdTokenResponseTypeHandler;
import org.forgerock.openidconnect.OpenIdConnectTokenStore;
import org.forgerock.openam.oauth2.OpenAMSettings;
import org.restlet.Request;
import org.restlet.ext.servlet.ServletUtils;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

/**
 * Implementation of the ResponseTypeHandler for handling OpenId Connect token response types in OpenAM.
 *
 * @since 12.0.0
 */
@Singleton
public class OpenAMIdTokenResponseTypeHandler extends IdTokenResponseTypeHandler {

    private final OpenAMSettings openAMSettings;

    /**
     * Constructs a new OpenAMIdTokenResponseTypeHandler.
     *
     * @param tokenStore An instance of the OpenIdConnectTokenStore.
     * @param openAMSettings An instance of the OpenAMSettings.
     */
    @Inject
    public OpenAMIdTokenResponseTypeHandler(OpenIdConnectTokenStore tokenStore, OpenAMSettings openAMSettings) {
        super(tokenStore);
        this.openAMSettings = openAMSettings;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getOps(OAuth2Request request) {

        final HttpServletRequest req = ServletUtils.getRequest(request.<Request>getRequest());
        if (req.getCookies() != null) {
            final String cookieName = openAMSettings.getSSOCookieName();
            for (final Cookie cookie : req.getCookies()) {
                if (cookie.getName().equals(cookieName)) {
                    return cookie.getValue();
                }
            }
        }

        return null;
    }
}
