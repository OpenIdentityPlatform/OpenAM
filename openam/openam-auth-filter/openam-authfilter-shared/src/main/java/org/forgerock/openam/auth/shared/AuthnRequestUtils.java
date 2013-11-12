/**
 * Copyright 2013 ForgeRock, Inc.
 *
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
 */
package org.forgerock.openam.auth.shared;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.sun.identity.shared.encode.CookieUtils;

import javax.servlet.http.HttpServletRequest;

/**
 * Responsible for performing common operations based on the SSOToken stored in the
 * request headers.
 *
 * @author robert.wapshott@forgerock.com
 */
public class AuthnRequestUtils {
    public static final String SSOTOKEN_COOKIE_NAME = "SSOTokenCookieName";

    private final String ssoTokenCookieName;

    /**
     * @param ssoTokenCookieName Intiailise the utility class with the name of the SSOToken Cookie name.
     */
    @Inject
    public AuthnRequestUtils(@Named(SSOTOKEN_COOKIE_NAME) String ssoTokenCookieName) {
        this.ssoTokenCookieName = ssoTokenCookieName;
    }

    /**
     * Finds and extracts the Token ID from the request.
     *
     * @param request Non null HttpServletRequest to validate.
     * @return The Token ID as extracted from the HttpServletRequest. Null if it was not present.
     */
    public String getTokenId(HttpServletRequest request) {
        return CookieUtils.getCookieValueFromReq(request, ssoTokenCookieName);
    }
}
