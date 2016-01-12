/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: OblixAuthModule.java,v 1.2 2008/10/18 00:37:54 mallas Exp $
 *
 * Portions Copyrighted 2015 ForgeRock AS.
 */

package com.sun.identity.authentication.oblix;

import java.util.Map;
import java.util.Enumeration;
import java.security.Principal;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.login.LoginException;
import com.sun.identity.authentication.spi.AMLoginModule;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.authentication.util.ISAuthConstants;
import com.sun.identity.shared.datastruct.CollectionHelper;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import com.oblix.access.*;

/**
 * Custom authentication module for validating oracle Acess Manager(Oblix)
 *  user session  to enable SSO integration between OpenAM and
 * Oracle access manager.
 */
public class OblixAuthModule extends AMLoginModule {

    private static final String COOKIE_NAME = "OblixCookieName"; 
    private static final String OAM_SDK_INSTALL_DIR = "OblixSDKInstallDir";
    private static final String CHECK_REMOTE_USER_ONLY = "CheckRemoteUserOnly";
    private static final String REMOTE_USER_HEADER_NAME = 
                                "RemoteUserHeaderName";

    private String oamCookieName = null;
    private String oamSDKInstallDir = null;    
    private boolean checkRemoteUserOnly = false; 
    private String userId = null;
    private Principal userPrincipal = null;
    private String remoteUserHeader = "REMOTE_USER";
    private ObConfig obconfig = null;

    public OblixAuthModule() throws LoginException{
	System.out.println("OblixAuthModule()");
    }

    /**
     * Initialize the authentication module with it's configuration
     */
    public void init(Subject subject, Map sharedState, Map options) {
	System.out.println("OblixAuthModule initialization" + options);

        oamCookieName = CollectionHelper.getMapAttr(options, 
                       COOKIE_NAME, "ObSSOCookie");

        oamSDKInstallDir = CollectionHelper.getMapAttr(
                       options, OAM_SDK_INSTALL_DIR);
        checkRemoteUserOnly = Boolean.valueOf(CollectionHelper.getMapAttr(
                   options, CHECK_REMOTE_USER_ONLY, "false")).booleanValue(); 

        remoteUserHeader = CollectionHelper.getMapAttr(options,
                           REMOTE_USER_HEADER_NAME, "REMOTE_USER");
        try {
            obconfig = new ObConfig();
            obconfig.initialize(oamSDKInstallDir);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    } 

    /**
     * This method process the login procedure for this authentication
     * module. In this auth module, if the user chooses to just validate
     * the HTTP headers set by the oracle webgent, this will not further
     * validate the OblixSesson by the Oracle AM SDK since the same thing
     * might have already been validated by the agent.
     */
    public int process(Callback[] callbacks, int state) 
                 throws AuthLoginException {

        HttpServletRequest request = getHttpServletRequest();

        if(checkRemoteUserOnly) {
           Enumeration headers = request.getHeaderNames();
           while(headers.hasMoreElements()) {
               String headerName = (String)headers.nextElement();
               if(headerName.equals(remoteUserHeader)) {
                  userId = request.getHeader(headerName);
               }
           }
           if(userId == null) {
              throw new AuthLoginException("No remote user header found");
           }
           return ISAuthConstants.LOGIN_SUCCEED;
        }

        Cookie[] cookies = request.getCookies();
        String OAMCookie =  null;
        String principal = null;
        boolean cookieFound = false;
        for (int i=0; i < cookies.length; i++) {
             Cookie cookie = cookies[i];
             if(cookie.getName().equals(oamCookieName)) {
                cookieFound = true;
                String value = cookie.getValue();
                System.out.println("cookie value" + value);
                //value = java.net.URLEncoder.encode(value);
                value = value.replaceAll(" ", "+");
                value = value.replaceAll("%3D", "=");
                System.out.println("cookie value afer replacing: " + value);
                try {
                    ObUserSession userSession = new ObUserSession(value);
                    if((userSession != null) && 
                         (userSession.getStatus() == ObUserSession.LOGGEDIN)) {
                        userId = userSession.getUserIdentity(); 
                    } else {
                       System.out.println("Oblix session decode failed");
                       throw new AuthLoginException(
                            "OblixSession decode failed");
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                       throw new AuthLoginException(
                            "OblixSession decode failed");
                }
            }
        }
        if(!cookieFound) {
           throw new AuthLoginException("Authentication failed. " +
                     "No Oblix cookie found");
        }
        return ISAuthConstants.LOGIN_SUCCEED;

    }

    /**
     * Returns the authenticated principal.
     * This is consumed by the authentication framework to set the 
     * principal
     */
    public java.security.Principal getPrincipal() {
        if (userPrincipal != null) {
            return userPrincipal;
        } else if (userId != null) {
            userPrincipal = new OblixPrincipal(userId);
            return userPrincipal;
        } else {
            return null;
        }
    }
}
