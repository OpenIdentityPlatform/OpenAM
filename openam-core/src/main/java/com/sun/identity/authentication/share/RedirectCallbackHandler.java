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
 * Copyright 2013-2015 ForgeRock AS.
 */

package com.sun.identity.authentication.share;

import com.sun.identity.authentication.client.AuthClientUtils;
import com.sun.identity.authentication.spi.RedirectCallback;
import com.sun.identity.shared.debug.Debug;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.forgerock.openam.authentication.RedirectException;

/**
 * A Callback Handler for RedirectCallbacks, so that multiple places in OpenAM can re-use the same code.
 */
public class RedirectCallbackHandler {

    private static final Debug debug = Debug.getInstance("amAuthentication");

    private static final String FORWARDING_PLACE = "/postRedirect.jsp";

    /**
     * Handles RedirectCallbacks by forwarding the response to the url in the callback.
     *
     * @param request The HttpServletRequest.
     * @param response The HttpServletResponse.
     * @param redirectCallback The RedirectCallback.
     * @param loginURL The url used to login o be set as a cookie on the response.
     */
    public void setRedirectCallbackCookie(HttpServletRequest request, HttpServletResponse response,
            RedirectCallback redirectCallback, String loginURL) {

        if (debug.messageEnabled()) {
            debug.message("Redirect to external web site...");
            debug.message("RedirectUrl : " + redirectCallback.getRedirectUrl()
                    + ", RedirectMethod : " + redirectCallback.getMethod()
                    + ", RedirectData : " + redirectCallback.getRedirectData());
        }

        // Create Cookie
        try {
            AuthClientUtils.setRedirectBackServerCookie(redirectCallback.getRedirectBackUrlCookieName(),
                    loginURL, request, response);
        } catch (Exception e) {
            if (debug.messageEnabled()) {
                debug.message("Could not set RedirectBackUrlCookie!" + e.toString());
            }
        }
    }

    public void handleRedirectCallback(HttpServletRequest request, HttpServletResponse response,
            RedirectCallback redirectCallback, String loginURL) throws IOException {

        setRedirectCallbackCookie(request, response, redirectCallback, loginURL);

        String qString = AuthClientUtils.getQueryStrFromParameters(redirectCallback.getRedirectData());

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
            if (redirectCallback.getMethod().equalsIgnoreCase("post")) {

                Map<String, String> dataMap = redirectCallback.getRedirectData();

                if (dataMap == null) {
                    dataMap = new HashMap<>();
                }

                request.setAttribute("postData", dataMap);
                request.setAttribute("postURL", redirectCallback.getRedirectUrl());

                //forward post
                try {
                    request.getRequestDispatcher(FORWARDING_PLACE).forward(request, response);
                } catch (ServletException e) {
                    if (debug.warningEnabled()) {
                        debug.warning("Could not set RedirectBackUrlCookie!" + e.toString());
                    }
                    throw new RedirectException("Could not forward request.", e);
                }
            } else {
                response.sendRedirect(rUrl);
            }
        }
    }
}
