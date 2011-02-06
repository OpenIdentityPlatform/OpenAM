/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: HTTPRequestHandler.java,v 1.10 2009/07/23 20:42:52 mrudul_uchil Exp $
 *
 */

package com.sun.identity.wss.security.handler;

import javax.security.auth.Subject;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.Principal;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.util.Set;
import java.util.Map;
import java.util.List;
import java.util.Iterator;
import java.io.FileOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ResourceBundle;

import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.iplanet.sso.SSOException;
import com.sun.identity.common.SystemConfigurationUtil;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.Constants;
import com.sun.identity.wss.provider.ProviderConfig;
import com.sun.identity.wss.provider.ProviderException;
import com.sun.identity.wss.security.SecurityException;
import com.sun.identity.wss.security.SecurityMechanism;
import com.sun.identity.wss.security.WSSUtils;
import com.sun.identity.wss.security.SecurityPrincipal;

/* iPlanet-PUBLIC-CLASS */

/**
 * This class <code>HTTPRequestHandler</code> is to process and secure the 
 * in-bound or out-bound HTTPRequest of the web service clients
 * and web service providers. 
 *
 */

public class HTTPRequestHandler implements HTTPRequestHandlerInterface {

    private static final String GOTO = "goto";
    private static final String AUTHENTICATED_USERS = "AUTHENTICATED_USERS";
    private static Debug debug = WSSUtils.debug;
    private static ResourceBundle bundle = WSSUtils.bundle;
    private static final String PROVIDER_NAME = "providername";
    private String providername = null;

   /**
     * Initialize the HTTP Request handler with a configuration map.
     * @param config the configuration map.
     */
    public void init(Map config) {        
        providername = (String)config.get(PROVIDER_NAME);
    }

    /**
     * Checks whether client should be authenticated or not. 
     *
     * @param subject the subject that may be used by the callers
     *        to store Principals and credentials validated in the request.
     *
     * @param request the <code>HttpServletRequest</code> associated with 
     *        this Client message request.
     *
     * @return true if the client should be authenticated 
     */
    public boolean shouldAuthenticate(Subject subject, 
              HttpServletRequest request) {

        if(setTokenInSubject(subject, request)) {
           if(debug.messageEnabled()) {
              debug.message("HTTPRequestHandler.shouldAuthenticate:: " + 
               "valid SSOToken exists");
           }
           return false;
        }
 
        if(debug.messageEnabled()) {
            debug.message("HTTPRequestHandler.shouldAuthenticate:: providername : " 
                + providername);
        }
        
        if((providername == null) || (providername.length() == 0)) {
            return true;
        }

        try {
            ProviderConfig pc = 
                ProviderConfig.getProvider(providername,ProviderConfig.WSC);
            if(pc == null) {
               return true;
            }
            
            List secMechs = pc.getSecurityMechanisms();
            if(secMechs.contains(
                SecurityMechanism.LIBERTY_DS_SECURITY_URI)) {
               return true;
            }
            if(!pc.forceUserAuthentication()) {
               setPrincipal(subject);
               return false;
            }
        } catch (ProviderException pe) {
            debug.error("HTTPProvider.shouldAuthenticate::  " +
                  "provider exception", pe);
        }
        return true;
    }

    /**
     * Validates and sets SSOToken into client's Subject. 
     *
     * @param subject the subject that may be used by the callers
     *        to store Principals and credentials validated in the request.
     *
     * @param request the <code>HttpServletRequest</code> associated with 
     *        this Client message request.
     *
     * @return false if any error occured during validating
     *        SSOToken or setting SSOToken into Subject, otherwise true.
     */
    private boolean setTokenInSubject(Subject subject,
            HttpServletRequest request) {
        try {
            SSOTokenManager manager = SSOTokenManager.getInstance();
            SSOToken ssoToken = manager.createSSOToken(request);
            if(manager.isValidToken(ssoToken)) {
               setPrincipal(subject);
               addSSOToken(ssoToken, subject);
               if(debug.messageEnabled()) {
                  debug.message("HTTPRequestHandler.setTokenInSubject: " + 
                           " Valid SSOToken ");
               }
               return true;
            } else {
                return false;
            }
        } catch (SSOException se) {
            if(debug.messageEnabled()) {
                debug.message("HTTPRequestHandler.setTokenInSubject: " + 
                        "Invalid SSOToken ");
            }
            return false;
        } catch (Exception e) {
            if(debug.messageEnabled()) {
                debug.message("HTTPRequestHandler.setTokenInSubject: " + 
                        "Can not set SSOToken in Subject ", e);
            }
            return false;
        }
    }

    /**
     * Returns Login URL for client to be redirected.
     * @param request the <code>HttpServletRequest</code>.
     *
     * @return String Login URL
     */
    public String getLoginURL(HttpServletRequest request) {
        String loginURL = 
            SystemConfigurationUtil.getProperty(Constants.LOGIN_URL);
        StringBuffer requestURL = request.getRequestURL();
        
        // This is useful for SAML2 integrations.
        String gotoparam = SystemConfigurationUtil.getProperty(
                "com.sun.identity.loginurl.goto", GOTO);
        loginURL = loginURL + "?" + gotoparam + "=" + requestURL.toString();
        String query = request.getQueryString();
        if(query != null) {
           loginURL = loginURL + "&" + query;
        }
        return loginURL;
        
    }

    /**
     * Adds SSOToken Id as private credential of given Subject.
     * @param httpAuthParam
     * @param subject
     *
     * @exception AuthException
     */
    private void addSSOToken(SSOToken ssoToken, Subject subject)
            throws Exception {

         final SSOToken sToken = ssoToken;
         final Subject subj = subject;
         AccessController.doPrivileged(new PrivilegedAction() {
                public java.lang.Object run() {
                    subj.getPrivateCredentials().add(sToken);
                    return null;
                }
         });
         //ThreadLocalService.setSSOToken(ssoToken);
    }

    // Sets the authenticated principal to the subject.
    private void setPrincipal(Subject subject) {
        Principal p = new SecurityPrincipal(
                        SystemConfigurationUtil.getProperty(
                        "com.sun.identity.jsr196.authenticated.user",
                        AUTHENTICATED_USERS));
         subject.getPrincipals().add(p);
    }

}
