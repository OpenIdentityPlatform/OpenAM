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
 * $Id: SMAuthModule.java,v 1.6 2009/07/16 17:04:26 ericow Exp $
 *
 * Portions Copyrighted 2015 ForgeRock AS.
 */

package com.sun.identity.authentication.siteminder;

import java.util.Map;
import java.util.Set;
import java.util.Enumeration;
import java.security.Principal;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.login.LoginException;
import com.sun.identity.authentication.spi.AMLoginModule;
import com.sun.identity.authentication.spi.AuthLoginException;
import netegrity.siteminder.javaagent.AgentAPI;
import netegrity.siteminder.javaagent.InitDef;
import netegrity.siteminder.javaagent.Attribute;
import netegrity.siteminder.javaagent.AttributeList;
import netegrity.siteminder.javaagent.ServerDef;
import netegrity.siteminder.javaagent.TokenDescriptor;
import com.sun.identity.authentication.util.ISAuthConstants;
import com.sun.identity.shared.datastruct.CollectionHelper;
import com.sun.identity.shared.xml.XMLUtils;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

/**
 * Custom authentication module for validating siteminder user session
 * to enable SSO integration between OpenAM and
 * Siteminder access server.
 * Siteminder is the trade mark of Computer Associates, the usage of the
 * Siteminder API is subject to Siteminder License terms.
 */
public class SMAuthModule extends AMLoginModule {

    private static final String COOKIE_NAME = "SMCookieName"; 
    private static final String SHARED_SECRET = "SharedSecret";
    private static final String SERVER_IP = "PolicyServerIPAddress";
    private static final String CHECK_REMOTE_USER_ONLY = "CheckRemoteUserOnly";
    private static final String TRUST_HOSTNAME = "TrustedHostName";
    private static final String ACCOUNT_PORT = "AccountingPort"; 
    private static final String AUTHN_PORT = "AuthenticationPort";
    private static final String AUTHZ_PORT = "AuthorizationPort";
    private static final String MIN_CONNECTION = "MinimumConnection";
    private static final String MAX_CONNECTION = "MaximumConnection"; 
    private static final String STEP_CONNECTION = "StepConnection";
    private static final String REQUEST_TIMEOUT = "RequestTimeout";
    private static final String REMOTE_USER_HEADER_NAME = 
                                "RemoteUserHeaderName";

    private String smCookieName = null;
    private String sharedSecret = null;    
    private String policyServerIP = null;
    private boolean checkRemoteUserOnly = false; 
    private String hostName = null;
    private int accountingPort = 0;
    private int authenticationPort = 44442;
    private int authorizationPort = 44441;
    private int connectionMin = 0;
    private int connectionMax = 0;
    private int connectionStep = 0;
    private int timeout = 0;
    private String userId = null;
    private Principal userPrincipal = null;
    private String remoteUserHeader = "REMOTE_USER";
    private Set configuredHTTPHeaders = null;

    public SMAuthModule() throws LoginException{
	System.out.println("SMAuthModule()");
    }

    /**
     * Initialize the authentication module with it's configuration
     */
    public void init(Subject subject, Map sharedState, Map options) {
	System.out.println("SMAuthModule initialization" + options);

        smCookieName = CollectionHelper.getMapAttr(options, 
                       COOKIE_NAME, "SMSESSION");

        sharedSecret = CollectionHelper.getMapAttr(options, SHARED_SECRET);
        policyServerIP = CollectionHelper.getMapAttr(options, SERVER_IP);
        checkRemoteUserOnly = Boolean.valueOf(CollectionHelper.getMapAttr(
                   options, CHECK_REMOTE_USER_ONLY, "false")).booleanValue(); 
        hostName = CollectionHelper.getMapAttr(options, TRUST_HOSTNAME);
        configuredHTTPHeaders = (Set)options.get("HTTPHeaders");
        try {
            String tmp = CollectionHelper.getMapAttr(options, 
                     ACCOUNT_PORT, "44443");
            accountingPort = Integer.parseInt(tmp);

            tmp = CollectionHelper.getMapAttr(options,
                  AUTHN_PORT, "44442"); 
            authenticationPort = Integer.parseInt(tmp);

            tmp = CollectionHelper.getMapAttr(options,
                  AUTHZ_PORT, "44441");
            authorizationPort = Integer.parseInt(tmp);

            tmp = CollectionHelper.getMapAttr(options, MIN_CONNECTION);
            connectionMin = Integer.parseInt(tmp);

            tmp = CollectionHelper.getMapAttr(options, MAX_CONNECTION);
            connectionMax = Integer.parseInt(tmp);

            tmp =  CollectionHelper.getMapAttr(options, STEP_CONNECTION); 
            connectionStep = Integer.parseInt(tmp);

            tmp =  CollectionHelper.getMapAttr(options,  REQUEST_TIMEOUT);
            timeout = Integer.parseInt(tmp);
                  
        } catch (Exception e) {
            e.printStackTrace();
        }

        remoteUserHeader = CollectionHelper.getMapAttr(options,
                           REMOTE_USER_HEADER_NAME, "REMOTE_USER");
        
    } 

    /**
     * This method process the login procedure for this authentication
     * module. In this auth module, if the user chooses to just validate
     * the HTTP headers set by the siteminder agent, this will not further
     * validate the SMSESSION by the siteminder SDK since the same thing
     * might have already been validated by the agent.
     */
    public int process(Callback[] callbacks, int state) 
                 throws AuthLoginException {

        HttpServletRequest request = getHttpServletRequest();

        if(configuredHTTPHeaders != null) {
           request.setAttribute("SM-HTTPHeaders", configuredHTTPHeaders);
        }
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
        String SMCookie =  null;
        String principal = null;
        boolean cookieFound = false;
        for (int i=0; i < cookies.length; i++) {
             Cookie cookie = cookies[i];
             if(cookie.getName().equals("SMSESSION")) {
                cookieFound = true;
                String value = cookie.getValue();
                System.out.println("cookie value" + value);
                //value = java.net.URLEncoder.encode(value);
                value = value.replaceAll(" ", "+");
                value = value.replaceAll("%3D", "=");
                System.out.println("cookie value afer replacing: " + value);
                InitDef id = new InitDef(hostName, sharedSecret, true, 
                                      new ServerDef());
                id.addServerDef(policyServerIP,
                             connectionMin,
                             connectionMin,
                             connectionStep,
                             timeout,
                             authorizationPort,
                             authenticationPort,
                             authorizationPort);
                AgentAPI agentAPI = new AgentAPI();
                int initStat = agentAPI.init(id);
                if(initStat == AgentAPI.SUCCESS) {
                   System.out.println("Agent API init succeeded");
                }
                int version = 0;
                boolean thirdParty = false;
                TokenDescriptor td = new TokenDescriptor(version, thirdParty);
                AttributeList al  = new AttributeList();
                StringBuffer token = new StringBuffer();
                int status = agentAPI.decodeSSOToken(value, td, 
                             al, true, token);
                if(status == AgentAPI.FAILURE) {
                   System.out.println("SM session decode failed");
                   throw new AuthLoginException("SMSession decode failed");
                } else {
                   Enumeration attributes = al.attributes();
                   while(attributes.hasMoreElements()) {
                       Attribute attr =  (Attribute)attributes.nextElement();
                       int attrId = attr.id;
                       // debugging
                       System.out.println("Attribute Id: " + attrId);
                       String attrValue = XMLUtils.removeNullCharAtEnd(
                               new String(attr.value));
                       System.out.println("Attribute value: " + attrValue);
                       if(attrId == AgentAPI.ATTR_USERDN)
                           userId = attrValue;
                       }
                 }

            }
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
            userPrincipal = new SMPrincipal(userId);
            return userPrincipal;
        } else {
            return null;
        }
    }
}
