/**
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
 * $Id: SAMLAwareServlet.java,v 1.5 2009/06/12 22:21:39 mallas Exp $
 *
 * Portions Copyrighted 2013 ForgeRock AS
 */
package com.sun.identity.saml.servlet;

import com.sun.identity.shared.encode.URLEncDec;

import com.sun.identity.shared.encode.CookieUtils;

import com.sun.identity.plugin.session.SessionException;
import com.sun.identity.plugin.session.SessionManager;
import com.sun.identity.plugin.session.SessionProvider;
import com.sun.identity.saml.AssertionManager;
import com.sun.identity.saml.common.LogUtils;
import com.sun.identity.saml.common.SAMLConstants;
import com.sun.identity.saml.common.SAMLException;
import com.sun.identity.saml.common.SAMLServiceManager;
import com.sun.identity.saml.common.SAMLUtils;
import com.sun.identity.saml.protocol.AssertionArtifact;

import java.io.IOException;

import java.net.URL;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
/**
 * Endpoint that supports <code>SAML</code> web browser artifact profile.
 */
public class SAMLAwareServlet extends HttpServlet {
    
    /**
     * Overrides doGet method to support <code>SAML</code> web browser artifact
     * profile in two ways:
     * <pre>
     * - Initiates <code>SAML</code> single sign-on
     * - Accepts <code>SAML</code> artifact to complete single sign-on
     * </pre>
     *
     * @param request <code>HttpServletRequest</code> instance.
     * @param response <code>HttpServletResponse</code> instance.
     * @throws IOException,ServletException if there is an error.
     */
    public void doGet(HttpServletRequest request,HttpServletResponse response) 
                      throws IOException, ServletException {
        if (request == null || response == null) {
            String[] data = {SAMLUtils.bundle.getString("nullInputParameter")};
            LogUtils.error(java.util.logging.Level.INFO,
                LogUtils.NULL_PARAMETER, data);
            SAMLUtils.sendError(request, response,
                HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "nullInputParameter",
                SAMLUtils.bundle.getString("nullInputParameter"));
            return;
        }
        // avoid dos attack
        SAMLUtils.checkHTTPContentLength(request);
        
        String TargetUrl = request.getParameter((String) SAMLServiceManager.
        getAttribute(SAMLConstants.TARGET_SPECIFIER));
        String SamlArt = request.getParameter((String) SAMLServiceManager.
        getAttribute(SAMLConstants.ARTIFACT_NAME));
        if (TargetUrl == null || TargetUrl.length() == 0) {
            String[] data = {SAMLUtils.bundle.getString("missingTargetSite")};
            LogUtils.error(java.util.logging.Level.INFO,
                LogUtils.MISSING_TARGET, data);
            SAMLUtils.sendError(request, response,
                HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "invalidConfig",
                SAMLUtils.bundle.getString("invalidConfig"));
            return;
        }
        response.setContentType("text/html; charset=UTF-8");
        if (SamlArt == null || SamlArt.length() == 0) {
            IntersiteTransfer(request, response,TargetUrl);
        } else {
            ArtifactHandler(request, response);
        }
    }
    
    /**
     * Overrides doPost method. It simply calls <code>doGet</code> method.
     *
     * @param request <code>HttpServletRequest</code> instance.
     * @param response <code>HttpServletResponse</code> instance.
     * @throws IOException,ServletException if there is an error.
     */
    public void doPost(HttpServletRequest request,HttpServletResponse response)
                       throws IOException, ServletException {
        doGet(request, response);
    }
    
    /**
     * Creates a list of AssertionArtifact's id.
     *
     * @param sso the user Session object
     * @param target A String representing the target host
     * @param targetUrl A URL String representing the target site
     * @param version The relying party preferred Assertion version number
     * @return a List representing a list of AssertionArtifact's id
     * @throws SAMLException if there is an error.
     */
    private List createArtifact(Object sso,String target,
        HttpServletRequest request, HttpServletResponse response,
        String targetUrl, String version) throws SAMLException {
        if (sso == null || target == null || target.length() == 0 ||
            version == null || version.length() == 0) {
            throw new SAMLException(
            SAMLUtils.bundle.getString("createArtifactError"));
        }
        List artifactList = new ArrayList();
        AssertionManager assertManager = AssertionManager.getInstance();
        try {
            SessionProvider sessionProvider = SessionManager.getProvider();
            AssertionArtifact artifact =
                assertManager.createAssertionArtifact(
                sessionProvider.getSessionID(sso), target,
                request, response, targetUrl, version);
            if (SAMLUtils.debug.messageEnabled()) {
                SAMLUtils.debug.message("AssertionArtifact id = " +
                artifact.toString());
            }
            String artid = artifact.getAssertionArtifact();
            artifactList.add(artid);
        } catch (SessionException se) {
            SAMLUtils.debug.error("Couldn't get SessionProvider."); 
            throw new SAMLException(SAMLUtils.bundle.getString(
                "nullSessionProvider"));
        }
        return artifactList;
    }
    
    /**
     * Creates a list of AssertionArtifact's id.
     *
     * @param request the <code>HttpServletRequest</code> object.
     * @param response the <code>HttpServletResponse</code> object.
     * @param target String representing the target host.
     * @throws IOException  if there is an error.
     * @throws SAMLException if there is an error. 
     */
    private void IntersiteTransfer(HttpServletRequest request,
                                   HttpServletResponse response, String target) 
                                   throws IOException, ServletException {
        // put _Sites as HashSet, loop through _Sites.
        // to check if the real target contains the siteid from the config
        // and if the targte port number equals the port number in config
        // (the port number is optional)
        URL theTarget = new URL(target);
        String theHost = theTarget.getHost();
        int thePort = theTarget.getPort();
        if (theHost == null) {
            if (SAMLUtils.debug.messageEnabled()) {
                SAMLUtils.debug.error("SAMLAwareServlet:IntersiteTransfer:" +
                "Failed to get host name of target URL.");
            }
            SAMLUtils.sendError(request, response,
                HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "missingTargetHost",
                SAMLUtils.bundle.getString("missingTargetHost"));
            return;
        }
        if (SAMLUtils.debug.messageEnabled()) {
            SAMLUtils.debug.message("TargetUrl Host = " + theHost+
            " Port= " + thePort);
        }
        // target break on ":"
        SAMLServiceManager.SiteEntry thisSite = null;
        Set trustedserver = (Set) SAMLServiceManager.
        getAttribute(SAMLConstants.TRUSTED_SERVER_LIST);
        if (trustedserver == null) {
            SAMLUtils.sendError(request, response,
                HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "nullTrustedSite",
                SAMLUtils.bundle.getString("nullTrustedSite"));
            return;
        }
        Iterator iter = trustedserver.iterator();
        while (iter.hasNext()) {
            String key = null;
            int portNum = 0;
            SAMLServiceManager.SiteEntry se =
            (SAMLServiceManager.SiteEntry) iter.next();
            key = se.getHostName();
            portNum = se.getPort();
            if (portNum != -1) {
                if (theHost.indexOf(key) != -1) {
                    if (thePort != -1) {
                        if (thePort == portNum) {
                            thisSite = se;
                            break;
                        }
                    }
                }
            } else {
                // there is no port number specified in the SiteEntry:Target
                if (theHost.indexOf(key) != -1) {
                    thisSite = se;
                }
            }
        }
        
        if (thisSite != null) {
            //create Session
            Object ssoToken = null; 
            boolean loggedIn = false;

            try {
                SessionProvider sessionProvider =
                    SessionManager.getProvider();
                ssoToken = sessionProvider.getSession(request);
                if (ssoToken != null && sessionProvider.isValid(ssoToken)) {
                    loggedIn = true; 
                }  
            } catch (SessionException se) {
                SAMLUtils.debug.message("Invalid SSO!");
            }
            
            if (!loggedIn) {
               response.sendRedirect(SAMLUtils.getLoginRedirectURL(request));
               return; 
            }   

            // create AssertionArtifact(s)
            List artis = new ArrayList();
            try {
                artis = createArtifact(ssoToken, thisSite.getSourceID(),
                request, response, target, thisSite.getVersion());
            } catch (SAMLException se) {
                SAMLUtils.debug.error("IntersiteTransfer:Failed to create" +
                " AssertionArtifact(s)");
                SAMLUtils.sendError(request, response,
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "errorCreateArtifact",
                    se.getMessage());;
                return;
            }
            
            //bounce the user off to the remote site, pointing them to the
            //location of SamlAwareServlet at that site, and adding the
            //assertion artifact
            String targetName = (String) SAMLServiceManager.
            getAttribute(SAMLConstants.TARGET_SPECIFIER);
            String artifactName = (String) SAMLServiceManager.
            getAttribute(SAMLConstants.ARTIFACT_NAME);
            iter = artis.iterator();
            StringBuffer sb = new StringBuffer(1000);
            String samltmp = null;
            while (iter.hasNext()) {
                samltmp = URLEncDec.encode((String)iter.next());
                if (SAMLUtils.debug.messageEnabled()) {
                    SAMLUtils.debug.message("Encoded SAML AssertionArtifact " +
                    samltmp);
                }
                sb.append("&").append(artifactName).append("=").append(samltmp);
            }
            String redirecto = thisSite.getSAMLUrl() + "?" + targetName + "=" +
            URLEncDec.encode(target) + sb.toString();
            response.setStatus(response.SC_MOVED_TEMPORARILY);
            response.setHeader("Location", redirecto);
            String[] data = {SAMLUtils.bundle.getString("redirectTo"), 
                target, redirecto};
            LogUtils.access(java.util.logging.Level.FINE, 
                LogUtils.REDIRECT_TO_URL, data, ssoToken);
            response.sendRedirect(redirecto);
        } else {
            String[] data = {SAMLUtils.bundle.getString("targetForbidden"),
                target};
            LogUtils.error(java.util.logging.Level.INFO,
                LogUtils.TARGET_FORBIDDEN, data);
            SAMLUtils.sendError(request, response,
                HttpServletResponse.SC_FORBIDDEN,
                "targetForbidden",
                SAMLUtils.bundle.getString("targetForbidden") + " " + target);
            return;
        }
    }
    
    /**
     * Partner SAML aware servlet part.
     * Responsible for
     * <ol type="1">
     * <li>communicate with SOAP Receiver
     * <li>parse the replied SOAP Message
     * <li>analyze the SOAP Message and SSO Assertion inside the msg
     * <li>check the validity of the SSO assertion, if so, generate
     *     Session and set to cookie
     * </ol>
     *
     * @param request the <code>HttpServletRequest</code> object.
     * @param response the <code>HttpServletResponse</code> object.
     * @throws IOException if there is an error.
     * @throws ServletException if there is an error.
     */
    private void ArtifactHandler( HttpServletRequest request,
                                  HttpServletResponse response)
                                  throws IOException,  ServletException {
        javax.security.auth.Subject authSubject = null;
        String targeturl = request.getParameter((String) SAMLServiceManager.
            getAttribute(SAMLConstants.TARGET_SPECIFIER));
        String artifactName = (String) SAMLServiceManager.
        getAttribute(SAMLConstants.ARTIFACT_NAME);
        String[] arti = request.getParameterValues(artifactName);
        List assts = null;
        Map attrMap = null;
        try {
            Map sessionAttr = SAMLUtils.processArtifact(arti, targeturl);
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
                SAMLUtils.bundle.getString("failedCreateSSOToken"));
            return;
        }
        
        String[] data = {SAMLUtils.bundle.getString("accessGranted")};
        LogUtils.access(java.util.logging.Level.INFO,
            LogUtils.ACCESS_GRANTED, data);
        // now we know the assertions are valid, so use those to POST if
        // this target is in the POST to target list
        if (SAMLUtils.postYN(targeturl)) {
            if (SAMLUtils.debug.messageEnabled()) {
                SAMLUtils.debug.message("POST to target:"+targeturl);
            }
            SAMLUtils.postToTarget(response, response.getWriter(), assts, targeturl, attrMap);
        } else {
            response.sendRedirect(targeturl);
        }
    }
}
