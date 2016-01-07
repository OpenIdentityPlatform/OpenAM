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
*/

package org.forgerock.openam.saml2;

import com.sun.identity.federation.common.FSUtils;
import com.sun.identity.federation.common.IFSConstants;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.profile.FederateCookieRedirector;
import com.sun.identity.saml2.profile.UnableToRedirectException;
import com.sun.identity.shared.configuration.SystemPropertiesManager;
import com.sun.identity.shared.encode.CookieUtils;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
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

    @Override
    public void setCookieAndRedirect(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final boolean isIDP) throws UnableToRedirectException {

        FSUtils.setlbCookie(request, response);

        // turn off cookie hash redirect by default
        String tmpStr = SystemPropertiesManager.get(
                "com.sun.identity.federation.cookieHashRedirectEnabled");
        if ((tmpStr == null) || (!(tmpStr.equalsIgnoreCase("true")))) {
            throw new UnableToRedirectException();
        }

        String redirected = request.getParameter("redirected");
        if (redirected != null) {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("FSUtils.needSetLBCookieAndRedirect: " +
                        " redirected already and lbCookie not set correctly.");
            }
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
            FSUtils.debug.error("FSUtils.needSetLBCookieAndRedirect: ", ioe);
            throw new UnableToRedirectException();
        } catch (SAML2Exception saml2E) {
            FSUtils.debug.error("FSUtils.needSetLBCookieAndRedirect: ", saml2E);
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
