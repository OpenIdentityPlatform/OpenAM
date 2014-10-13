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

package org.forgerock.openidconnect.restlet;

import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.oauth2.restlet.AuthorizeRequestHook;
import org.forgerock.oauth2.restlet.TokenRequestHook;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.CookieSetting;
import org.restlet.util.Series;

import static org.forgerock.oauth2.core.OAuth2Constants.Custom.*;
import static org.forgerock.oauth2.core.OAuth2Constants.Params.*;

/**
 * Hooks into the authorize and token request to set/unset a cookie containing the login_hint OIDC parameter, which
 * may be used by the authentication chain.
 */
public class LoginHintHook implements AuthorizeRequestHook, TokenRequestHook {

    /**
     * Adds the login_hint value to cookie.
     * @param o2request The current OAuth2 request.
     * @param request The restlet request.
     * @param response The restlet response.
     */
    @Override
    public void beforeAuthorizeHandling(OAuth2Request o2request, Request request, Response response) {
        String loginHint = o2request.getParameter(LOGIN_HINT);
        if (loginHint != null && !loginHint.equals(request.getCookies().getFirstValue(LOGIN_HINT_COOKIE))) {
            CookieSetting cookie = new CookieSetting(0, LOGIN_HINT_COOKIE, loginHint);
            cookie.setPath("/");
            // set HttpOnly flag
            cookie.setAccessRestricted(true);
            response.getCookieSettings().add(cookie);
        }

    }

    /**
     * Once we're returning an auth code we can remove the login hint cookie.
     * @param o2request The current OAuth2 request.
     * @param request The restlet request.
     * @param response The restlet response.
     */
    @Override
    public void afterAuthorizeSuccess(OAuth2Request o2request, Request request, Response response) {
        // If we're still in the original authorize request, stop setting the cookie in the response
        Series<CookieSetting> cookiesSetInThisResponse = response.getCookieSettings();
        CookieSetting loginHintCookieSetting = cookiesSetInThisResponse.getFirst(LOGIN_HINT_COOKIE);
        if (loginHintCookieSetting != null && loginHintCookieSetting.getMaxAge() != 0) {
            cookiesSetInThisResponse.removeFirst(LOGIN_HINT_COOKIE);
        }
        removeCookie(request, response);
    }

    /**
     * Authentication has completed - remove the cookie.
     * @param o2request The current OAuth2 request.
     * @param request The restlet request.
     * @param response The restlet response.
     */
    @Override
    public void afterTokenHandling(OAuth2Request o2request, Request request, Response response) {
        removeCookie(request, response);
    }

    private void removeCookie(Request request, Response response) {
        // Delete the login hint cookie if it exists
        if (request.getCookies().getFirst(LOGIN_HINT_COOKIE) != null) {
            CookieSetting cookie = new CookieSetting(0, LOGIN_HINT_COOKIE, "");
            cookie.setMaxAge(0);
            response.getCookieSettings().add(cookie);
        }
    }

}
