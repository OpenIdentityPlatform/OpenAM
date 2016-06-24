/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: SAMLPOSTProfileServlet.java,v 1.4 2009/06/12 22:21:39 mallas Exp $
 *
 * Portions Copyrighted 2011-2016 ForgeRock AS
 */
package com.sun.identity.saml.servlet;

import com.sun.identity.shared.encode.Base64;

import com.sun.identity.plugin.session.SessionException;
import com.sun.identity.plugin.session.SessionManager;
import com.sun.identity.plugin.session.SessionProvider;

import com.sun.identity.saml.AssertionManager;

import com.sun.identity.saml.assertion.Assertion;

import com.sun.identity.saml.common.LogUtils;
import com.sun.identity.saml.common.SAMLConstants;
import com.sun.identity.saml.common.SAMLException;
import com.sun.identity.saml.common.SAMLServiceManager;
import com.sun.identity.saml.common.SAMLUtils;

import com.sun.identity.saml.protocol.Response;
import com.sun.identity.saml.protocol.Status;
import com.sun.identity.saml.protocol.StatusCode;

import java.io.IOException;
import java.io.PrintWriter;

import java.net.URL;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.owasp.esapi.ESAPI;

/**
 * This servlet is used to support SAML 1.x Web Browser/POST Profile.
 */
public class SAMLPOSTProfileServlet extends HttpServlet {
    
    /**
     * Initiates <code>SAML</code> web browser POST profile.
     * This method takes in a TARGET in the request, creates a SAMLResponse,
     * then redirects user to the destination site.
     *
     * @param request <code>HttpServletRequest</code> instance
     * @param response <code>HttpServletResponse</code> instance
     * @throws ServletException if there is an error.
     * @throws IOException if there is an error.
     */
    public void doGet(HttpServletRequest request, HttpServletResponse response)
                      throws ServletException, IOException {
        if ((request == null) || (response == null)) {
            String[] data = {SAMLUtils.bundle.getString("nullInputParameter")};
            LogUtils.error(java.util.logging.Level.INFO,
                LogUtils.NULL_PARAMETER, data);
            SAMLUtils.sendError(request, response,
                HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "nullInputParameter",
                SAMLUtils.bundle.getString("nullInputParameter"));
            return;
        }
        
        SAMLUtils.checkHTTPContentLength(request);
        
        // get Session
        Object token = getSession(request);
        if (token == null) {
            response.sendRedirect(SAMLUtils.getLoginRedirectURL(request));
            return;
        }
        
        // obtain TARGET
        String target = request.getParameter(SAMLConstants.POST_TARGET_PARAM);
        if (target == null || target.length() == 0) {
            String[] data = {SAMLUtils.bundle.getString("missingTargetSite")};
            LogUtils.error(java.util.logging.Level.INFO,
                LogUtils.MISSING_TARGET, data, token);
            SAMLUtils.sendError(request, response,
                HttpServletResponse.SC_BAD_REQUEST,
                "missingTargetSite",
                SAMLUtils.bundle.getString("missingTargetSite"));
            return;
        }
        
        // Get the Destination site Entry
        // find the destSite POST URL, which is the Receipient
        SAMLServiceManager.SiteEntry destSite = getDestSite(target);
        String destSiteUrl = null;
        if ((destSite == null) ||
        ((destSiteUrl = destSite.getPOSTUrl()) == null)) {
            String[] data = {SAMLUtils.bundle.getString("targetForbidden"),
                target};
            LogUtils.error(java.util.logging.Level.INFO,
                LogUtils.TARGET_FORBIDDEN, data, token);
            SAMLUtils.sendError(request, response,
                response.SC_BAD_REQUEST,
                "targetForbidden",
                SAMLUtils.bundle.getString("targetForbidden") + " " + target);
            return;
        }
        
        Response samlResponse = null;
        try {
            String version = destSite.getVersion();
            int majorVersion = SAMLConstants.PROTOCOL_MAJOR_VERSION;
            int minorVersion = SAMLConstants.PROTOCOL_MINOR_VERSION;
            if (version != null) {
                StringTokenizer st = new StringTokenizer(version,".");
                if (st.countTokens() == 2) {
                    majorVersion = Integer.parseInt(st.nextToken().trim());
                    minorVersion = Integer.parseInt(st.nextToken().trim());
                }
            }
            // create assertion
            AssertionManager am = AssertionManager.getInstance();
            SessionProvider sessionProvider = SessionManager.getProvider();
            Assertion assertion = am.createSSOAssertion(
                sessionProvider.getSessionID(token), null,
                request, response, destSite.getSourceID(), target,
                majorVersion + "." + minorVersion);

            // create SAMLResponse
            StatusCode statusCode = new StatusCode(
            SAMLConstants.STATUS_CODE_SUCCESS);
            Status status = new Status(statusCode);
            List contents = new ArrayList();
            contents.add(assertion);
            samlResponse = new Response(null, status, destSiteUrl, contents);
            samlResponse.setMajorVersion(majorVersion);
            samlResponse.setMinorVersion(minorVersion);
        } catch (SessionException sse) {
            SAMLUtils.debug.error("SAMLPOSTProfileServlet.doGet: Exception "
                + "Couldn't get SessionProvider:", sse); 
           SAMLUtils.sendError(request, response,
                HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "couldNotCreateResponse",
                sse.getMessage());
            return;
        } catch (NumberFormatException ne) {
            SAMLUtils.debug.error("SAMLPOSTProfileServlet.doGet: Exception "
                + "when creating Response: ", ne);
            SAMLUtils.sendError(request, response,
                HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "couldNotCreateResponse",
                ne.getMessage());
            return;
        } catch (SAMLException se) {
            SAMLUtils.debug.error("SAMLPOSTProfileServlet.doGet: Exception "
                + "when creating Response: ", se);
            SAMLUtils.sendError(request, response,
                HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "couldNotCreateResponse",
                se.getMessage());
            return;
        }
        
        // sign the samlResponse
        byte signedBytes[] = null;
        try {
            samlResponse.signXML();
            if (SAMLUtils.debug.messageEnabled()) {
                SAMLUtils.debug.message("SAMLPOSTProfileServlet.doGet: " +
                "signed samlResponse is" +
                samlResponse.toString(true, true, true));
            }
            signedBytes = SAMLUtils.getResponseBytes(samlResponse);
        } catch (Exception e) {
            SAMLUtils.debug.error("SAMLPOSTProfileServlet.doGet: Exception "
            + "when signing the response:", e);
            SAMLUtils.sendError(request, response,
                HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "errorSigningResponse",
                SAMLUtils.bundle.getString("errorSigningResponse"));
            return;
        }
        
        // base64 encode the signed samlResponse
        String encodedResponse = null;
        try {
            encodedResponse = Base64.encode(signedBytes, true).trim();
        } catch (Exception e) {
            SAMLUtils.debug.error("SAMLPOSTProfileServlet.doGet: Exception "
            + "when encoding the response:", e);
             SAMLUtils.sendError(request, response,
                HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "errorEncodeResponse",
                SAMLUtils.bundle.getString("errorEncodeResponse"));
            return;
        }
        
        if (LogUtils.isAccessLoggable(java.util.logging.Level.FINE)) {
            String[] data = {SAMLUtils.bundle.getString("redirectTo"),
                target, destSiteUrl, new String(signedBytes, "UTF-8")};
            LogUtils.access(java.util.logging.Level.FINE,
                LogUtils.REDIRECT_TO_URL, data, token);
        } else {
            String[] data = {SAMLUtils.bundle.getString("redirectTo"),
                target, destSiteUrl};
            LogUtils.access(java.util.logging.Level.INFO,
                LogUtils.REDIRECT_TO_URL, data, token);
        }
        response.setContentType("text/html; charset=UTF-8");
        try (PrintWriter out = response.getWriter()) {
            out.println("<HTML>");
            out.println("<BODY Onload=\"document.forms[0].submit()\">");
            out.println("<FORM METHOD=\"POST\" ACTION=\""
                    + ESAPI.encoder().encodeForHTMLAttribute(destSiteUrl) + "\">");
            out.println("<INPUT TYPE=\"HIDDEN\" NAME=\"" +
                    SAMLConstants.POST_SAML_RESPONSE_PARAM + "\" ");
            out.println("VALUE=\"" + ESAPI.encoder().encodeForHTMLAttribute(encodedResponse) + "\">");
            out.println("<INPUT TYPE=\"HIDDEN\" NAME=\"" +
                    SAMLConstants.POST_TARGET_PARAM + "\" VALUE=\"" + ESAPI.encoder().encodeForHTMLAttribute(target)
                    + "\"> </FORM>");
            out.println("</BODY></HTML>");
        }
    }
    
    private SAMLServiceManager.SiteEntry getDestSite(String target) {
        SAMLServiceManager.SiteEntry destSite = null;
        try {
            URL targetUrl = new URL(target);
            String targetHost = targetUrl.getHost();
            int targetPort = targetUrl.getPort();
            if (targetHost == null) {
                SAMLUtils.debug.error("SAMLPOSTProfileServlet.getDestSite:"
                + " missing host in target.");
                return null;
            }
            // loop through the trusted server list and try to find the server
            SAMLServiceManager.SiteEntry serverSite = null;
            Iterator iter = ((Set) SAMLServiceManager.getAttribute(
            SAMLConstants.TRUSTED_SERVER_LIST)).iterator();
            String serverHost = null;
            int serverPort = -1;
            while (iter.hasNext()) {
                serverSite = (SAMLServiceManager.SiteEntry) iter.next();
                serverHost = serverSite.getHostName();
                serverPort = serverSite.getPort();
                if (serverHost == null) {
                    continue;
                }
                if (targetHost.indexOf(serverHost) != -1) {
                    if (serverPort != -1) {
                        if (serverPort == targetPort) {
                            destSite = serverSite;
                            break;
                        }
                    } else {
                        destSite = serverSite;
                    }
                }
            }
            
            if (destSite == null) {
                SAMLUtils.debug.error("SAMLPOSTProfileServlet.getDestSite: "
                + " No destSite found from the target.");
                return null;
            }
        } catch (Exception e) {
            SAMLUtils.debug.error("SAMLPOSTProfileServlet.getDestSite: ", e);
            return null;
        }
        
        return destSite;
    }
    
    private Object getSession(HttpServletRequest request) {
        Object token = null;
        try { 
            SessionProvider sessionProvider =
                SessionManager.getProvider();
            token = sessionProvider.getSession(request);
            if (token == null) {
                SAMLUtils.debug.error("SAMLPOSTProfileServlet.getSession: "
                + "Session is null.");
                return null;
            }
            if (!sessionProvider.isValid(token)) {
                SAMLUtils.debug.error("SAMLPOSTProfileServlet.getSession: "
                + "Session is invalid.");
                return null;
            }
        } catch (SessionException se) {
            SAMLUtils.debug.error("SAMLPOSTProfileServlet.getSession: "
            + "Exception when getting Session:", se);
            return null;
        }
        return token;
    }
    
    /**
     * This method processes TARGET and SAMLResponse info from the request,
     * validates the response/assertion(s), then redirects user to the
     * TARGET resource if all are valid.
     *
     * @param request <code>HttpServletRequest</code> instance
     * @param response <code>HttpServletResponse</code> instance
     * @throws  ServletException if there is an error.
     * @throws  IOException if there is an error.
     */
    public void doPost(HttpServletRequest request, HttpServletResponse response)
                       throws ServletException, IOException {
        response.setContentType("text/html; charset=UTF-8");
        
        if ((request == null) || (response == null)) {
            String[] data = {SAMLUtils.bundle.getString("nullInputParameter")};
            LogUtils.error(java.util.logging.Level.INFO,
                LogUtils.NULL_PARAMETER, data);
            SAMLUtils.sendError(request, response,
                HttpServletResponse.SC_BAD_REQUEST,
                "nullInputParameter",
                SAMLUtils.bundle.getString("nullInputParameter"));
            return;
        }
        
        SAMLUtils.checkHTTPContentLength(request);
        
        // obtain TARGET
        String target = request.getParameter(SAMLConstants.POST_TARGET_PARAM);
        if (target == null || target.length() == 0) {
            String[] data = {SAMLUtils.bundle.getString("missingTargetSite")};
            LogUtils.error(java.util.logging.Level.INFO,
                LogUtils.MISSING_TARGET, data);
            SAMLUtils.sendError(request, response,
                HttpServletResponse.SC_BAD_REQUEST,
                "missingTargetSite",
                SAMLUtils.bundle.getString("missingTargetSite"));
            return;
        }
        
        // obtain SAMLResponse
        String samlResponse = request.getParameter(
        SAMLConstants.POST_SAML_RESPONSE_PARAM);
        if (samlResponse == null) {
            String[] data = {SAMLUtils.bundle.getString("missingSAMLResponse")};
            LogUtils.error(java.util.logging.Level.INFO,
                LogUtils.MISSING_RESPONSE, data);
            SAMLUtils.sendError(request, response,
                HttpServletResponse.SC_BAD_REQUEST,
                "missingSAMLResponse",
                SAMLUtils.bundle.getString("missingSAMLResponse"));
            return;
        }
        
        // decode the Response
        byte raw[] = null;
        try {
            raw = Base64.decode(samlResponse);
        } catch (Exception e) {
            SAMLUtils.debug.error("SAMLPOSTProfileServlet.doPost: Exception "
            + "when decoding SAMLResponse:", e);
             SAMLUtils.sendError(request, response,
                response.SC_INTERNAL_SERVER_ERROR,
                "errorDecodeResponse",
                SAMLUtils.bundle.getString("errorDecodeResponse"));
            return;
        }
        
        // Get Response back
        Response sResponse = SAMLUtils.getResponse(raw);
        if (sResponse == null) {
            String[] data = {SAMLUtils.bundle.getString("errorObtainResponse")};
            LogUtils.error(java.util.logging.Level.INFO,
               LogUtils.RESPONSE_MESSAGE_ERROR, data);
            SAMLUtils.sendError(request, response,
                HttpServletResponse.SC_BAD_REQUEST,
                "errorObtainResponse",
                SAMLUtils.bundle.getString("errorObtainResponse"));
            return;
        }
        
        if (SAMLUtils.debug.messageEnabled()) {
            SAMLUtils.debug.message("SAMLPOSTProfileServlet.doPost: Received "
            + sResponse.toString());
        }
        
        // verify that Response is correct
        StringBuffer requestUrl = request.getRequestURL();
        if (SAMLUtils.debug.messageEnabled()) {
            SAMLUtils.debug.message("SAMLPOSTProfileServlet.doPost: "
            + "requestUrl=" + requestUrl);
        }
        boolean valid = SAMLUtils.verifyResponse(sResponse,
                                            requestUrl.toString(),
                                            request);
        if (!valid) {
            String[] data = {SAMLUtils.bundle.getString("invalidResponse")};
            LogUtils.error(java.util.logging.Level.INFO,
                LogUtils.INVALID_RESPONSE, data);
            SAMLUtils.sendError(request, response,
                HttpServletResponse.SC_BAD_REQUEST,
                "invalidResponse",
                SAMLUtils.bundle.getString("invalidResponse"));
            return;
        }
        
        Map attrMap = null;
        List assertions = null;
        javax.security.auth.Subject authSubject = null;
        try {
            Map sessionAttr = SAMLUtils.processResponse(
                sResponse, target);
            Object token = SAMLUtils.generateSession(request,
                response, sessionAttr);
        } catch (Exception ex) {
            SAMLUtils.debug.error("generateSession: ", ex);
            String[] data = {SAMLUtils.bundle.getString(
                "failedCreateSSOToken")};
            LogUtils.error(java.util.logging.Level.INFO,
                LogUtils.FAILED_TO_CREATE_SSO_TOKEN, data);
            SAMLUtils.sendError(request, response,
                HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "failedCreateSSOToken",
                ex.getMessage());;
            return;
        }
        
        if (LogUtils.isAccessLoggable(java.util.logging.Level.FINE)) {
            String[] data = {SAMLUtils.bundle.getString("accessGranted"),
                new String(raw, "UTF-8")};
            LogUtils.access(java.util.logging.Level.FINE,
                LogUtils.ACCESS_GRANTED, data);
        } else {
            String[] data = {SAMLUtils.bundle.getString("accessGranted")};
            LogUtils.access(java.util.logging.Level.INFO,
                LogUtils.ACCESS_GRANTED, data);
        }
        if (SAMLUtils.postYN(target)) {
            if (SAMLUtils.debug.messageEnabled()) {
                SAMLUtils.debug.message("POST to target:"+target);
            }
            SAMLUtils.postToTarget(response, response.getWriter(), assertions, target,attrMap);
        } else {
            response.setHeader("Location", target);
            response.sendRedirect(target);
        }
    }
}
