/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2005 Sun Microsystems Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * https://opensso.dev.java.net/public/CDDLv1.0.html or
 * opensso/legal/CDDLv1.0.txt
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at opensso/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id: LoginServlet.java,v 1.9 2009/02/18 03:38:42 222713 Exp $
 *
 */

/*
 * Portions Copyrighted 2011 ForgeRock AS
 */
package com.sun.identity.authentication.UI;

import com.iplanet.am.util.SystemProperties;
import com.iplanet.dpro.session.Session;
import com.iplanet.dpro.session.SessionID;
import com.iplanet.jato.CompleteRequestException;
import com.iplanet.jato.RequestContext;
import com.iplanet.jato.RequestContextImpl;
import com.iplanet.jato.ViewBeanManager;
import com.iplanet.services.cdm.G11NSettings;
import com.sun.identity.authentication.service.AuthUtils;
import com.sun.identity.common.ISLocaleContext;
import com.sun.identity.common.RequestUtils;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.locale.L10NMessageImpl;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * This is the front controller of authentication UI
 */
public class LoginServlet
extends com.sun.identity.authentication.UI.AuthenticationServletBase {

    /**
     * Creates <code>LoginServlet</code> object.
     */
    public LoginServlet() {
        super();
    }
    
    /**
     *
     *
     */
    protected void initializeRequestContext(RequestContext requestContext) {
        super.initializeRequestContext(requestContext);
        // Set a view bean manager in the request context.  This must be
        // done at the module level because the view bean manager is
        // module specifc.
        ViewBeanManager viewBeanManager =
        new ViewBeanManager(requestContext,PACKAGE_NAME);
        
        ((RequestContextImpl)requestContext).setViewBeanManager(
        viewBeanManager);
        
        HttpServletRequest request = requestContext.getRequest();
        HttpServletResponse response = requestContext.getResponse();

        // Check whether to detect the browser capability to support cookies
        // by redirecting the response with dummy cookie.
        if (checkForCookiesInBrowser(request, response, debug)) {
            throw new CompleteRequestException();
        }

        // Check content length
        try {
            RequestUtils.checkContentLength(request);
        } catch (L10NMessageImpl e) {
            if (debug.messageEnabled()) {
                ISLocaleContext localeContext = new ISLocaleContext();
                localeContext.setLocale(request);
                java.util.Locale locale = localeContext.getLocale();
                debug.message("LoginServlet: " + e.getL10NMessage(locale));
            }
            AuthExceptionViewBean vb = (AuthExceptionViewBean)
            viewBeanManager.getViewBean(
            com.sun.identity.authentication.UI.AuthExceptionViewBean.class);
            vb.forwardTo(requestContext);
            throw new CompleteRequestException();
        }
        
        // Check if the hostname in the URL is an FQDN else
        // redirect to the fqdn
        String client_type = AuthUtils.getClientType(request);
        if (debug.messageEnabled()) {
            debug.message("Client Type = " + client_type);
        }
        String hostName = AuthUtils.getHostName(request);
        if (!AuthUtils.isValidFQDNRequest(hostName)) {
            try {
                String newHN =
                AuthUtils.getValidFQDNResource(hostName,request);
                if (debug.messageEnabled()) {
                    debug.message("FQDN = " + newHN);
                }
                if (AuthUtils.isGenericHTMLClient(client_type)) {
                    debug.message("This is HTML");
                    response.sendRedirect(newHN);
                } else {
                    String fileName = AuthUtils.getDefaultFileName(
                        request, REDIRECT_JSP);
                    if (debug.messageEnabled()) {
                        debug.message("Forward to : " + fileName);
                    }
                    RequestDispatcher dispatcher =
                    request.getRequestDispatcher(fileName);
                    dispatcher.forward(request, response);
                }
            } catch (Exception e) {
                // came here continue
            }
            throw new CompleteRequestException();
        }

        String cookieURL = AuthUtils.getCookieURLForSessionUpgrade(request);
        if (cookieURL != null) {
            rerouteRequest(request, response, cookieURL);
            return;
        }
        // Check whether this is the correct server to accept the client
        // response.
        String authCookieValue = AuthUtils.getAuthCookieValue(request);
        if ((authCookieValue != null) && (authCookieValue.length() != 0) &&
            (!authCookieValue.equalsIgnoreCase("LOGOUT"))) {
            //if cookie server does not match to this local server then
            //send Auth request to cookie (original) server
            try {
                SessionID sessionID = new SessionID(authCookieValue);
                cookieURL = AuthUtils.getCookieURL(sessionID);
            } catch (Exception e) {
                if (debug.messageEnabled()) {
                    debug.message("LoginServlet error in Session : "
                        + e.toString());
                }
            }
            if (debug.messageEnabled()) {
                debug.message("cookieURL : " + cookieURL);
            }
            if ((cookieURL != null) && (cookieURL.length() != 0) &&
                (!AuthUtils.isLocalServer(cookieURL,true))) {
                rerouteRequest(request, response, cookieURL);
            }
        }
    }

    private void rerouteRequest(HttpServletRequest request, HttpServletResponse response, String cookieURL) {
        debug.message("Routing the request to Original Auth server");
        try {
            HashMap origRequestData =
                    AuthUtils.sendAuthRequestToOrigServer(
                    request, response, cookieURL);
            Exception fwdEx = (Exception) origRequestData.get("EXCEPTION");
            if (fwdEx != null) {
                AuthUtils.clearHostUrlCookie(response);
                AuthUtils.clearlbCookie(request, response);
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                throw fwdEx;
            }
            String redirect_url = null;
            String clientType = null;
            String output_data = null;
            String contentType = null;
            Map<String, List<String>> headers = null;
            int responseCode = HttpServletResponse.SC_OK; // OK by default, origRequestData should override it
            if (origRequestData != null && !origRequestData.isEmpty()) {
                redirect_url =
                        (String) origRequestData.get("AM_REDIRECT_URL");
                output_data =
                        (String) origRequestData.get("OUTPUT_DATA");
                clientType =
                        (String) origRequestData.get("AM_CLIENT_TYPE");
                contentType =
                        (String) origRequestData.get("CONTENT_TYPE");
                headers =
                        (Map<String, List<String>>) origRequestData.get("HTTP_HEADERS");
                responseCode = (Integer) origRequestData.get("RESPONSE_CODE");
            }
            if (debug.messageEnabled()) {
                debug.message("redirect_url : " + redirect_url);
                debug.message("clientType : " + clientType);
            }
            if (headers != null) {
                for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
                    String headerName = entry.getKey();
                    if (headerName != null) {
                        if (RETAINED_HTTP_HEADERS.contains(headerName.toLowerCase())) {
                            List<String> headerValues = entry.getValue();
                            if (headerValues != null) {
                                for (String headerValue : headerValues) {
                                    response.addHeader(headerName, headerValue);
                                }
                            }
                        }
                    }
                }
            }
            response.setStatus(responseCode);
            if (responseCode >= HttpServletResponse.SC_BAD_REQUEST) {
                if (debug.warningEnabled()) {
                    debug.warning("Received " + responseCode + " response code "
                            + "while forwarding request, throwing CompleteRequestException");
                }
                AuthUtils.clearHostUrlCookie(response);
                AuthUtils.clearlbCookie(request, response);
                throw new CompleteRequestException();
            }
            if (((redirect_url != null) && !redirect_url.equals(""))
                    && (AuthUtils.isGenericHTMLClient(clientType))) {
                debug.message("Redirecting the response");
                response.sendRedirect(redirect_url);
            }
            if ((output_data != null) && (!output_data.equals(""))) {
                debug.message("Printing the forwarded response");
                if (contentType != null) {
                    if (debug.messageEnabled()) {
                        debug.message("Content type is " + contentType);
                    }
                    response.setContentType(contentType);
                } else {
                    if (debug.messageEnabled()) {
                        debug.message("Content type is default; " + DEFAULT_CONTENT_TYPE);
                    }
                    response.setContentType(DEFAULT_CONTENT_TYPE);
                }

                java.io.PrintWriter outP = response.getWriter();
                outP.println(output_data);
            }
            if ((redirect_url == null || redirect_url.length() == 0)
                    && (output_data == null || output_data.length()
                    == 0) && (responseCode == 200 || responseCode == -1)) {
                if (debug.messageEnabled()) {
                    debug.message("LoginServlet:initializeRequestContext"
                            + " No Response from original Auth server");
                }
                String refererURL = request.getHeader("Referer");
                String refererDomain = null;
                if (refererURL != null && !(refererURL.length() == 0)) {
                    URL u = new URL(refererURL);
                    int pos = u.getHost().indexOf(".");
                    if (pos != -1) {
                        refererDomain = u.getHost().substring(pos);
                    }
                } else {
                    refererURL = request.getRequestURL().toString();
                    if (request.getQueryString() != null) {
                        refererURL = refererURL + "?"
                                + request.getQueryString();
                    }
                }
                if (debug.messageEnabled()) {
                    debug.message("LoginServlet:initializeRequestContext"
                            + " referer domain is " + refererDomain);
                }
                //remove amAuthCookie and amLBCookie cookies
                Cookie[] cookies = request.getCookies();
                Set domains = AuthUtils.getCookieDomains();
                if (cookies != null && cookies.length > 0) {
                    for (int i = 0; i < cookies.length; i++) {
                        if (cookies[i].getName().equalsIgnoreCase(
                                AuthUtils.getAuthCookieName())
                                || cookies[i].getName().equalsIgnoreCase(AuthUtils.getlbCookieName())) {
                            if (debug.messageEnabled()) {
                                debug.message("LoginServlet:"
                                        + "initializeRequestContext removing"
                                        + "cookie " + cookies[i].getName());
                            }
                            cookies[i].setValue("");
                            cookies[i].setMaxAge(0);
                            response.addCookie(cookies[i]);
                            if (!domains.isEmpty()) {
                                for (Iterator it = domains.iterator();
                                        it.hasNext();) {
                                    String domain = (String) it.next();
                                    if (debug.messageEnabled()) {
                                        debug.message("LoginServlet:"
                                                + "initializeRequestContext"
                                                + " removing cookie "
                                                + domain);
                                    }
                                    Cookie cookie = AuthUtils.createCookie(
                                            cookies[i].getName(), "", domain);
                                    cookie.setMaxAge(0);
                                    response.addCookie(cookie);
                                } //end for
                            } else {
                                //using domain name from referer
                                if (refererDomain != null) {
                                    Cookie cookie = AuthUtils.createCookie(cookies[i].getName(), "", refererDomain);
                                    cookie.setMaxAge(0);
                                    response.addCookie(cookie);
                                }
                            }
                        }
                    }
                }
                if (debug.messageEnabled()) {
                    debug.message("LoginServlet:initializeRequestContext"
                            + "redirecting to: " + refererURL);
                }
                response.sendRedirect(refererURL);
            }
        } catch (Exception e) {
            if (debug.warningEnabled()) {
                debug.warning("LoginServlet error in Request Routing : ", e);
            }
            String authCookieName = AuthUtils.getAuthCookieName();
            Set<String> domains = (Set<String>) AuthUtils.getCookieDomains();
            if (domains == null || domains.isEmpty()) {
                response.addCookie(AuthUtils.createPersistentCookie(authCookieName, "LOGOUT", 0, null));
            } else {
                for (String domain : domains) {
                    response.addCookie(AuthUtils.createPersistentCookie(authCookieName, "LOGOUT", 0, domain));
                    if (debug.messageEnabled()) {
                        debug.message("LoginServlet reset Auth Cookie in domain: " + domain);
                    }
                }
            }
        }
        throw new CompleteRequestException();
    }

    // Checks whether the browser supports or has enabled cookie
    // Returns "true" if browser has no cookies and need to redirect to
    // the Login URL with dummy cookie in order to detect the browser
    // capability to support cookies.
    // Returns "false" if there is no redirection required, which could be
    // to generate the error page to the end user to warn him/her to
    // enable cookies in the browser or to proceed with normal Login process.
    private boolean checkForCookiesInBrowser(HttpServletRequest request,
            HttpServletResponse response, Debug debug) {
        String agentOrder = null;
        if (amCookieCheck != null && amCookieCheck.equalsIgnoreCase("false")) {
            // this is another way of enable cookie check, send this 
            // parameter from agent/client
            agentOrder = request.getParameter("amagentcookiecheck");
        }

        if (debug.messageEnabled()) {
            debug.message("LoginServlet:checkForCookiesInBrowser " +
                    " amCookieCheck: " + amCookieCheck +
                    " agentOrder: " + agentOrder);
        }

        if ((amCookieCheck != null && amCookieCheck.equalsIgnoreCase("true")) ||
                (agentOrder != null && agentOrder.equalsIgnoreCase("true"))) {
            String redirectFlag = request.getParameter("AMTESTCOOKIE");
            String requestURL = request.getRequestURL().toString();
            Cookie[] allCookies = request.getCookies();
            int numCookies = 0;
            if (allCookies != null) {
                numCookies = allCookies.length;
            }

            if (numCookies == 0 && redirectFlag == null) {
                Cookie dummyCookie = new Cookie("AMTESTCOOKIE", "amtestcookie");
                response.addCookie(dummyCookie);
                String queryStr = request.getQueryString();
                try {
                    if (queryStr == null || queryStr.length() == 0) {
                        response.sendRedirect(requestURL+
                        "?AMTESTCOOKIE=amtestcookie");
                    } else {
                        response.sendRedirect(requestURL+
                        "?"+queryStr+"&AMTESTCOOKIE=amtestcookie");
                    }
                } catch (Exception e) {
                    debug.message("LoginServlet:checkForCookiesInBrowser " +
                            " error in Request Routing : " + e.toString());
                }
                return true;
            }

            if (redirectFlag != null && numCookies == 0) {
                debug.message("LoginServlet:checkForCookiesInBrowser " +
                        "This browser does not support cookie");
                request.setAttribute("displayCookieError", "true");
            }
        }
        return false;
    }

    /**
     * Returns url for auth module.
     * @return url for auth module.
     */
    public String getModuleURL() {
        // The superclass can be configured from init params specified at
        // deployment time.  If the superclass has been configured with
        // a different module URL, it will return a non-null value here.
        // If it has not been configured with a different URL, we use our
        // (hopefully) sensible default.
        String result = super.getModuleURL();
        if (result != null)
            return result;
        else
            return DEFAULT_MODULE_URL;
    }
    
    /**
     *
     *
     */
    protected void onSessionTimeout(RequestContext requestContext)
    throws ServletException {
        // Do nothing
    }
    
    
    ////////////////////////////////////////////////////////////////////////////
    // Class variables
    ////////////////////////////////////////////////////////////////////////////
    
    private final String amCookieCheck =
            SystemProperties.get(Constants.AM_COOKIE_CHECK, "false");

    /** Default module uri. */
    public static final String DEFAULT_MODULE_URL="../UI";
    /** Confiured page name for configured servlet */
    public static String PACKAGE_NAME=
    getPackageName(LoginServlet.class.getName());
    
    private static final String REDIRECT_JSP = "Redirect.jsp";
    private static final String DEFAULT_CONTENT_TYPE = "text/html; charset=" + G11NSettings.CDM_DEFAULT_CHARSET;
    private static final List<String> RETAINED_HTTP_HEADERS = new ArrayList<String>();
    private static final List<String> FORBIDDEN_TO_COPY_HEADERS = new ArrayList<String>();
    // the debug file
    private static final Debug debug = Debug.getInstance("amLoginServlet");
    
    private static String serviceURI = SystemProperties.get(
        Constants.AM_SERVICES_DEPLOYMENT_DESCRIPTOR) + "/UI/Login";    
    
    static {
        initialize();
    }

    private static void initialize() {
        String retainedHeaders = SystemProperties.get(
                Constants.RETAINED_HTTP_HEADERS_LIST);
        String forbiddenHeaders = SystemProperties.get(
                Constants.FORBIDDEN_TO_COPY_HEADERS);
        if (retainedHeaders != null) {
            RETAINED_HTTP_HEADERS.addAll(Arrays.asList(retainedHeaders.toLowerCase().split(",")));
        }
        if (forbiddenHeaders != null) {
            FORBIDDEN_TO_COPY_HEADERS.addAll(Arrays.asList(forbiddenHeaders.toLowerCase().split(",")));
        }
        //configuration sanity check
        RETAINED_HTTP_HEADERS.removeAll(FORBIDDEN_TO_COPY_HEADERS);
    }
    ////////////////////////////////////////////////////////////////////////////
    // Instance variables
    ////////////////////////////////////////////////////////////////////////////
}

