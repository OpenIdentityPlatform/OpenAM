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
* Copyright 2015-2016 ForgeRock AS.
* Portions copyright 2025-2026 3A Systems LLC.
*/

package org.forgerock.openam.saml2;

import com.sun.identity.federation.common.FSUtils;
import com.sun.identity.federation.common.IFSConstants;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.profile.FederateCookieRedirector;
import com.sun.identity.saml2.profile.UnableToRedirectException;
import com.sun.identity.shared.encode.CookieUtils;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * A FederateCookieRedirector that uses the existing methods in Utility classes to implement cookie checking
 * and redirect.
 */
public class UtilProxyCookieRedirector implements FederateCookieRedirector {

    @Override
    public boolean isCookieSet(HttpServletRequest request, HttpServletResponse response, boolean isIDP) {

        List remoteServiceURLs = FSUtils.getRemoteServiceURLs(request);
        if ((remoteServiceURLs == null) || (remoteServiceURLs.isEmpty())) {
            return false;
        }

        Cookie lbCookie = CookieUtils.getCookieFromReq(request,
                FSUtils.getlbCookieName());
        if (lbCookie != null) {
            return false;
        }
        return true;
    }

    /**
     * A second copy of the bounce logic that {@link FSUtils#needSetLBCookieAndRedirect} carries,
     * signalling through an exception rather than a boolean. Nothing in this repository calls it -
     * the live path through this class is {@link #needSetLBCookieAndRedirect}, which delegates
     * straight back to <code>FSUtils</code> - so keeping it in step is parity maintenance, not a
     * second hardened path. It is kept in step anyway, because the two copies having drifted apart
     * is what produced GHSA-v796-mg6j-9c5m.
     */
    @Override
    public void setCookieAndRedirect(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final boolean isIDP) throws UnableToRedirectException {

        FSUtils.setlbCookie(request, response);

        // The gate itself is not copied: FSUtils.requireRedirect holds the flag lookup and the
        // already-bounced check, and returns false in exactly the two cases this method has to
        // decline in.
        if (!FSUtils.requireRedirect(request)) {
            throw new UnableToRedirectException();
        }

        String queryString = request.getQueryString();
        StringBuffer reqURLSB = new StringBuffer();
        reqURLSB.append(request.getRequestURL().toString())
                .append("?redirected=1");
        if (queryString != null) {
            reqURLSB.append("&").append(queryString);
        }

        try {
            String reqMethod = request.getMethod();
            if (reqMethod.equals("POST")) {
                String samlMessageName = null;
                String samlMessage = null;
                if (isIDP) {
                    samlMessageName = IFSConstants.SAML_REQUEST;
                    samlMessage = request.getParameter(samlMessageName);
                } else {
                    samlMessageName = IFSConstants.SAML_RESPONSE;
                    samlMessage = request.getParameter(samlMessageName);
                    if (samlMessage == null) {
                        samlMessageName = IFSConstants.SAML_ART;
                        samlMessage = request.getParameter(samlMessageName);
                    }
                }
                if (samlMessage == null) {
                    throw new UnableToRedirectException();
                }
                String relayState = request.getParameter(
                        IFSConstants.RELAY_STATE);
                FSUtils.postToTarget(request, response, samlMessageName,
                        samlMessage, IFSConstants.RELAY_STATE, relayState,
                        reqURLSB.toString());
            } else if (reqMethod.equals("GET")) {
                response.sendRedirect(reqURLSB.toString());
            } else {
                throw new UnableToRedirectException();
            }
        } catch (IOException ioe) {
            handleFailedBounce(response, ioe);
        } catch (SAML2Exception saml2E) {
            handleFailedBounce(response, saml2E);
        }
    }

    /**
     * Mirrors {@link FSUtils#requireStopAfterFailedBounce} for the exception based signalling of
     * this class: an <code>UnableToRedirectException</code> tells the caller to carry on, which is
     * only safe while the response is untouched.
     * <p>
     * So a bounce that failed after the response was committed returns normally, the same way a
     * bounce that succeeded does. That is not the failure being hidden: a void method has no other
     * way to say "stop", and stopping is the only safe thing left once the response is spoiled. The
     * interface documents the pair that way.
     */
    private void handleFailedBounce(HttpServletResponse response, Exception cause)
            throws UnableToRedirectException {
        if (!FSUtils.requireStopAfterFailedBounce(response, cause)) {
            throw new UnableToRedirectException();
        }
    }

    @Override
    public boolean ifNoCookieIsSetThenSetTheCookieThenRedirectToANewRequestAndReturnTrue(
            HttpServletRequest request, HttpServletResponse response, boolean isIDP) {
        return FSUtils.needSetLBCookieAndRedirect(request, response, isIDP);
    }

    @Override
    public boolean needSetLBCookieAndRedirect(HttpServletRequest request, HttpServletResponse response, boolean isIDP) {
        return FSUtils.needSetLBCookieAndRedirect(request, response, isIDP);
    }
}
