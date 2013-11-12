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
 * Copyright 2013 ForgeRock Inc.
 */

package com.sun.identity.authentication.share;

import com.sun.identity.authentication.client.AuthClientUtils;
import com.sun.identity.authentication.service.AuthUtils;
import com.sun.identity.authentication.spi.RedirectCallback;
import com.sun.identity.shared.debug.Debug;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * A Callback Handler for RedirectCallbacks, so that multiple places in OpenAM can re-use the same code.
 */
public class RedirectCallbackHandler {

    private static final Debug debug = Debug.getInstance("amAuthentication");

    /**
     * Handles RedirectCallbacks by forwarding the response to the url in the callback.
     *
     * @param request The HttpServletRequest.
     * @param response The HttpServletResponse.
     * @param redirectCallback The RedirectCallback.
     * @param loginURL The url used to login o be set as a cookie on the response.
     * @throws IOException If a problem occurs when sending the redirect.
     */
    public void handleRedirectCallback(HttpServletRequest request, HttpServletResponse response,
            RedirectCallback redirectCallback, String loginURL) throws IOException {

        if (debug.messageEnabled()){
            debug.message("Redirect to external web site...");
            debug.message("RedirectUrl : " + redirectCallback.getRedirectUrl()
                    + ", RedirectMethod : " + redirectCallback.getMethod()
                    + ", RedirectData : " + redirectCallback.getRedirectData());
        }

        String qString = AuthUtils.getQueryStrFromParameters(redirectCallback.getRedirectData());

        String requestURL = request.getRequestURL().toString();
        String requestURI = request.getRequestURI();
        int index = requestURL.indexOf(requestURI);
        String redirectBackServerCookieValue = null;
        if (index != -1) {
            redirectBackServerCookieValue = requestURL.substring(0, index) + loginURL;
        }
        // Create Cookie
        try {
            AuthUtils.setRedirectBackServerCookie(redirectCallback.getRedirectBackUrlCookieName(),
                    redirectBackServerCookieValue, request, response);
        } catch (Exception e) {
            if (debug.messageEnabled()){
                debug.message("Could not set RedirectBackUrlCookie!" + e.toString());
            }
        }

        StringBuilder redirectUrl = new StringBuilder(redirectCallback.getRedirectUrl());
        if (qString != null && qString.length() != 0) {
            redirectUrl.append(qString);
        }

        String rUrl = redirectUrl.toString();
        if (rUrl.startsWith("/UI/Login")) {
            if (debug.messageEnabled()) {
                debug.message("LoginViewBean.processRedirectCallback :"
                        + " redirect URL " + rUrl
                        + ", serviceuri=" + AuthClientUtils.getServiceURI());
            }
            // prepend deployment URI
            response.sendRedirect(AuthClientUtils.getServiceURI() + rUrl);
        } else {
            response.sendRedirect(rUrl);
        }
    }
}
