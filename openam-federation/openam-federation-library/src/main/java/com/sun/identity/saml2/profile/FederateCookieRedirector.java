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
*/

package com.sun.identity.saml2.profile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Interface to describe an object that can check a cookie and then perform SAML2 Redirect based on the result.
 */
public interface FederateCookieRedirector {

    /**
     * Establishes whether or not the SAML2 cookie is set.
     *
     * @param request  the SAML2 request
     * @param response the saml2 Response
     * @param isIDP    whether this request was from an IDP
     * @return true if the cookie is set.
     */
    boolean isCookieSet(
            HttpServletRequest request, HttpServletResponse response,
            boolean isIDP);

    /**
     * Sets the cookie for the SAML2 Request and redirects the request.
     *
     * @param request the SAML2 Request object
     * @param response the SAML2 Resposne object
     * @param isIDP whether this request was from and idp
     * @throws UnableToRedirectException if there was a problem preforming the redirect.
     */
    void setCookieAndRedirect(HttpServletRequest request, HttpServletResponse response,
                              boolean isIDP) throws UnableToRedirectException;

    /**
     * Sets the cookie for the SAML2 Request and redirects the request.
     *
     * @param request the SAML2 Request object
     * @param response the SAML2 Resposne object
     * @param isIDP whether this request was from and idp
     */
    boolean ifNoCookieIsSetThenSetTheCookieThenRedirectToANewRequestAndReturnTrue(
            HttpServletRequest request, HttpServletResponse response,
            boolean isIDP);

    /**
     * Sets the cookie if required and then redirects the SAML2 request.  Returns a boolean to indicate whether the
     * redirect action was taken.
     *
     * @param request the SAML2 Request object
     * @param response the SAML2 Resposne object
     * @param isIDP whether this request was from and idp
     * @return true if the redirect action was performed
     */
    boolean needSetLBCookieAndRedirect(
            HttpServletRequest request, HttpServletResponse response,
            boolean isIDP);
}
