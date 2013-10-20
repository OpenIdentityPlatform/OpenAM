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
 * $Id: HTTPBasic.java,v 1.5 2009/06/19 17:54:14 ericow Exp $
 *
 */

/**
 * Portions Copyrighted 2011-2013 ForgeRock AS
 */
package com.sun.identity.authentication.modules.httpbasic;

import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.datastruct.CollectionHelper;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.authentication.spi.HttpCallback;
import com.sun.identity.authentication.spi.InvalidPasswordException;
import com.sun.identity.authentication.spi.AMLoginModule;
import com.sun.identity.authentication.config.AMAuthenticationManager;
import com.sun.identity.authentication.config.AMAuthenticationInstance;
import com.sun.identity.authentication.util.ISAuthConstants;
import com.sun.identity.authentication.service.AuthD;
import java.security.Principal;
import java.util.ResourceBundle;
import java.util.Map;
import javax.security.auth.Subject;
import javax.security.auth.login.LoginException;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import java.io.IOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.encode.Base64;
import java.security.AccessController;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * HTTP Basic login module.
 */
public class HTTPBasic extends AMLoginModule {
    
    private static final String amAuthHTTPBasic = "amAuthHTTPBasic";
    private static Debug debug = Debug.getInstance(amAuthHTTPBasic);
    private static String MODCONFIG =
    "iplanet-am-auth-http-basic-module-configured";
    private static String AUTHLEVEL = "iplanet-am-auth-httpbasic-auth-level";
    private Principal userPrincipal = null;
    private ResourceBundle bundle = null;
    private String validatedUserID;
    private String instanceName = null;
    private String userName;
    private String userPassword;
    private Map currentConfig;
    private Map options;
    private AMLoginModule amLoginModule = null;
    
    public HTTPBasic() {
    }
    
    public void init(Subject subject, Map sharedState, Map options) {
        java.util.Locale locale  = getLoginLocale();
        bundle = amCache.getResBundle(amAuthHTTPBasic, locale);
        if (debug.messageEnabled()) {
            debug.message("HttpBasicAuth resbundle locale="+locale);
        }
        this.options = options;
        instanceName  = CollectionHelper.getMapAttr(options, MODCONFIG);
        String authLevel = CollectionHelper.getMapAttr(options, AUTHLEVEL);
        if (authLevel != null) {
            try {
                setAuthLevel(Integer.parseInt(authLevel));
            } catch (Exception e) {
                debug.error("Unable to set auth level " + authLevel,e);
            }
        }
        try {
            SSOToken adminToken = (SSOToken)AccessController.doPrivileged(
                AdminTokenAction.getInstance());
            AMAuthenticationManager amAM = new
                AMAuthenticationManager(adminToken, getRequestOrg());
            AMAuthenticationInstance amInstance =
                amAM.getAuthenticationInstance(instanceName);
            currentConfig = amInstance.getAttributeValues();
            String moduleType = amInstance.getType();
            String moduleClassName = AuthD.getAuth().getAuthenticatorForName(
                moduleType);
            Class moduleClass = Class.forName(moduleClassName);
            amLoginModule = (AMLoginModule)moduleClass.newInstance();
            CallbackHandler handler = getCallbackHandler();
            amLoginModule.initialize(subject,handler,sharedState,currentConfig);
        } catch (Exception exp) {
            debug.error("Could not initialize the module instance"+
                instanceName,
                exp); 
        }

    }

    public int process(Callback[] callbacks, int state)
    throws LoginException {
        if ((instanceName == null) || (instanceName.length() == 0) ) {
            throw new AuthLoginException(amAuthHTTPBasic, "noModule", null);
        }

        int status = 0;
        HttpServletRequest req = getHttpServletRequest();
        HttpServletResponse resp = getHttpServletResponse();
        String auth = null;
        if (callbacks != null && callbacks.length != 0) { 
            auth = ((HttpCallback)callbacks[0]).getAuthorization();
        }

        if ((req == null || resp == null) && auth == null) {
            debug.message("Servlet Request and Response cannot be null");
            throw new AuthLoginException(amAuthHTTPBasic, "reqRespNull", 
                null);
        }
        try {
            debug.message("Process HTTPBasic Auth started ...");
            if (auth == null || auth.length() == 0) {
                auth = req.getHeader("Authorization");
            }
            if (debug.messageEnabled()) {
                debug.message("AUTH : "+auth);
            }
            int retVal = authenticate(auth);
            validatedUserID = userName;
            return retVal;
        } catch(Exception ex) {
            debug.error("login: unknown exception = ", ex);
            setFailureID(userName);
            if (ex instanceof InvalidPasswordException) {
                throw new InvalidPasswordException(ex);
            } else {
                throw new AuthLoginException(amAuthHTTPBasic, "sendError", 
                    null,ex);
            }
        }
        
    }
    
    public java.security.Principal getPrincipal() {
        if (userPrincipal != null) {
            return userPrincipal;
        } else if (amLoginModule != null ) {
            validatedUserID = amLoginModule.getPrincipal().getName();
            userPrincipal = new HTTPBasicPrincipal(validatedUserID);
            return userPrincipal;
        } else {
            return null;
        }
    }
    
    public void destroyModuleState() {
        validatedUserID = null;
        userPrincipal = null;
    }
    
    public void nullifyUserdVars() {
        bundle = null;
        userName = null;
        userPassword = null;
        currentConfig = null;
        options = null;
    }
    
    private int authenticate(String auth)
    throws LoginException,IOException{
        if(auth == null || !auth.toUpperCase().startsWith("BASIC")){
            throw new AuthLoginException(amAuthHTTPBasic, "wrong header", 
            null, null);
        }
        String userPwdEncoded = auth.substring(6);  // removes 'BASIC '
        String decode = new String(Base64.decode(userPwdEncoded));
        int idx = decode.indexOf(':');
        if (idx != -1) {
            userPassword = decode.substring(idx+1);
            userName = decode.substring(0,idx);
        }
        storeUsernamePasswd(userName, userPassword);
        return authenticateToBackEndModule();
        
    }
    
    private int authenticateToBackEndModule() throws LoginException {
        Callback[] callbacks = new Callback[2];
        NameCallback nameCallback = new NameCallback("dummy");
        nameCallback.setName(userName);
        callbacks[0] = nameCallback;
        PasswordCallback passwordCallback = new PasswordCallback(
            "dummy",false);
            passwordCallback.setPassword(userPassword.toCharArray());
        callbacks[1] = passwordCallback;
        return amLoginModule.process(callbacks,ISAuthConstants.LOGIN_START);
    }
}
