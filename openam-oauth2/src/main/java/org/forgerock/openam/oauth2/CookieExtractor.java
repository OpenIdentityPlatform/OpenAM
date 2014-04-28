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

package org.forgerock.openam.oauth2;

import javax.inject.Singleton;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

/**
 * Extracts the value of the cookie from a HttpServletRequest.
 *
 * @since 12.0.0
 */
@Singleton
public class CookieExtractor {

    /**
     * Extracts the value of the cookie with the specified name on the given request.
     *
     * @param request The HttpServletRequest.
     * @param cookieName The cookie name.
     * @return The cookie value or {@code null} if the cookie is not present on the request.
     */
    public String extract(HttpServletRequest request, String cookieName) {

        if (request.getCookies() != null) {
            for (final Cookie cookie : request.getCookies()) {
                if (cookie.getName().equals(cookieName)) {
                    return cookie.getValue();
                }
            }
        }

        return null;
    }
}
