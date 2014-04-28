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

import org.forgerock.oauth2.core.OAuth2Request;
import org.restlet.Request;
import org.restlet.ext.servlet.ServletUtils;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

/**
 * @since 12.0.0
 */
@Singleton
public class IdTokenResponseTypeHandlerImpl extends IdTokenResponseTypeHandler {

    @Inject
    public IdTokenResponseTypeHandlerImpl(OpenIdConnectTokenStore tokenStore) {
        super(tokenStore);
    }

    @Override
    protected String getOps(OAuth2Request request) {
        final HttpServletRequest req = ServletUtils.getRequest(Request.getCurrent());

        if (req.getCookies() != null) {
            for (final Cookie cookie : req.getCookies()) {
                if (cookie.getName().equals("FR_OAUTH2_SESSION_ID")) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}
