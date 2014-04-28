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

import org.forgerock.common.SessionManager;
import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.oauth2.core.exceptions.ServerException;
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
public class OpenIdConnectProviderImpl implements OpenIDConnectProvider {

    private final SessionManager sessionManager;

    @Inject
    public OpenIdConnectProviderImpl(final SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    public boolean isUserValid(String userId, OAuth2Request request) {
        return sessionManager.isValid(getSessionId(ServletUtils.getRequest(request.<Request>getRequest())));
    }

    private String getSessionId(final HttpServletRequest request) {
        if (request.getCookies() != null) {
            final String cookieName = "FR_OAUTH2_SESSION_ID";
            for (final Cookie cookie : request.getCookies()) {
                if (cookie.getName().equals(cookieName)) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    public void destroySession(String sessionId) throws ServerException {
        sessionManager.delete(sessionId);
    }
}
