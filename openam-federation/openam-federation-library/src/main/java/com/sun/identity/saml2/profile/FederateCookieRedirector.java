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
* Copyright 2015 ForgeRock AS.
* Portions copyright 2025-2026 3A Systems LLC.
*/

package com.sun.identity.saml2.profile;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Interface to describe an object that can check a cookie and then perform SAML2 Redirect based on the result.
 */
public interface FederateCookieRedirector {

    /**
     * Establishes whether the load balancer cookie still has to be added to this request.
     * <p>
     * Note the sense: the only implementation returns <code>true</code> when the cookie is
     * <em>absent</em> and the request is on a multi server platform, matching
     * {@link com.sun.identity.federation.common.FSUtils#requireAddCookie}. The name reads the other
     * way round and is kept for compatibility.
     *
     * @param request  the SAML2 request
     * @param response the saml2 Response
     * @param isIDP    whether this request was from an IDP
     * @return true if the request is on a multi server platform and carries no load balancer cookie
     */
    boolean isCookieSet(
            HttpServletRequest request, HttpServletResponse response,
            boolean isIDP);

    /**
     * Sets the cookie for the SAML2 Request and redirects the request.
     * <p>
     * A normal return means the caller must stop and write nothing further: either the redirect was
     * performed, or it failed after the response had already been committed, which leaves the caller
     * nothing it can safely do with the response. The exception is raised only while the response is
     * still usable, so that the caller can carry on with it.
     *
     * @param request the SAML2 Request object
     * @param response the SAML2 Resposne object
     * @param isIDP whether this request was from and idp
     * @throws UnableToRedirectException if no redirect was performed and the caller still owns an
     *     uncommitted, usable response. Not an untouched one - the load balancer cookie is added
     *     before any of the throw paths, and a failed bounce leaves the sink's no-cache headers
     *     behind.
     */
    void setCookieAndRedirect(HttpServletRequest request, HttpServletResponse response,
                              boolean isIDP) throws UnableToRedirectException;

    /**
     * Sets the cookie for the SAML2 Request and redirects the request.
     *
     * @param request the SAML2 Request object
     * @param response the SAML2 Resposne object
     * @param isIDP whether this request was from and idp
     * @return true if the caller must return without writing anything further, see
     *     {@link #needSetLBCookieAndRedirect}.
     */
    boolean ifNoCookieIsSetThenSetTheCookieThenRedirectToANewRequestAndReturnTrue(
            HttpServletRequest request, HttpServletResponse response,
            boolean isIDP);

    /**
     * Sets the cookie if required and then redirects the SAML2 request.  Returns a boolean to indicate whether the
     * caller still owns the response.
     * <p>
     * <code>true</code> means the request has been answered and the caller must return without
     * writing anything further. That covers a redirect that was performed, and also a redirect that
     * failed after the response had already been committed - there is nothing the caller can do with
     * a spoiled response but stop.
     *
     * @param request the SAML2 Request object
     * @param response the SAML2 Resposne object
     * @param isIDP whether this request was from and idp
     * @return true if the caller must return without writing anything further, false if no redirect
     *     was needed and the caller still owns a usable response
     */
    boolean needSetLBCookieAndRedirect(
            HttpServletRequest request, HttpServletResponse response,
            boolean isIDP);
}
