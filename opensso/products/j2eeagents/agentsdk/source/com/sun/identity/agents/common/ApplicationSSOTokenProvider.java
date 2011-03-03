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
 * $Id: ApplicationSSOTokenProvider.java,v 1.4 2009/04/02 00:02:11 leiming Exp $
 *
 */

package com.sun.identity.agents.common;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;

import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.agents.arch.AgentConfiguration;
import com.sun.identity.agents.arch.AgentException;
import com.sun.identity.agents.arch.Module;
import com.sun.identity.agents.arch.SurrogateBase;
import com.sun.identity.authentication.AuthContext;

/**
 * The class provides the Single Sign-on token for an application
 */
public class ApplicationSSOTokenProvider extends SurrogateBase 
                implements IApplicationSSOTokenProvider {
    
    public ApplicationSSOTokenProvider(Module module) {
        super(module);
    }
    
    public void initialize() {
        //Nothing to initialize       
    }
    
    public SSOToken getApplicationSSOToken(boolean addHook)
            throws AgentException {
        SSOToken result = null;
        try {         
            AuthContext authContext = 
                new AuthContext(AgentConfiguration.getOrganizationName());
            authContext.login(AuthContext.IndexType.MODULE_INSTANCE,
                          MODULE_APPLICATION);
            if(authContext.hasMoreRequirements()) {
                Callback[] callbacks = authContext.getRequirements();
                if(callbacks != null) {
                    addLoginCallbackMessage(callbacks, getApplicationUser(), 
                            getApplicationPassword());
                    authContext.submitRequirements(callbacks);
                }
            }
            if(authContext.getStatus() == AuthContext.Status.SUCCESS) {
                result = authContext.getSSOToken();
            }
        } catch (Exception ex) {
            logError("ApplicationSSOTokenProvider.getApplicationSSOToken(): " +
                "Failed to get Application SSO Token with exception : ",
                ex);
            throw new AgentException("ApplicationSSOTokenProvider." +
                    "getApplicationSSOToken(): Unable to get Application " +
                    "SSO Token", ex);
        }

        if (addHook) {
            if (result != null) {
                if (isLogMessageEnabled()) {
                    logMessage(
                        "ApplicationSSOTokenProvider." +
                        "getApplicationSSOToken(): Shutdown hook added for " +
                        "Application SSO token.");
                }
                addShutDownHookForApplicationSSOToken(result);
            }
        }
        return result;
    }

    private void addShutDownHookForApplicationSSOToken(SSOToken appSSO) {
        Runtime.getRuntime().addShutdownHook(
            new Thread(new ApplicationSSOTokenShutdownHook(appSSO)));
    }
    
    private String getApplicationUser() {
        return AgentConfiguration.getApplicationUser();
    }
    
    private String getApplicationPassword() {
        return AgentConfiguration.getApplicationPassword();
    }
    
    private void addLoginCallbackMessage(
            Callback[] callbacks, String appUserName, String password)
                throws UnsupportedCallbackException {

        for(int i = 0; i < callbacks.length; i++) {
            if(callbacks[i] instanceof NameCallback) {
                NameCallback nameCallback = (NameCallback) callbacks[i];

                nameCallback.setName(appUserName);
            } else if(callbacks[i] instanceof PasswordCallback) {
                PasswordCallback pwdCallback =
                    (PasswordCallback) callbacks[i];

                pwdCallback.setPassword(password.toCharArray());
            }
        }
    }
    
    /*
     * 
     * @author krishc
     *
     * Class to implement shutdown hook for Application SSO Token
     * 
     */
    private class ApplicationSSOTokenShutdownHook implements Runnable {
        
            public void run() {
                try {
                    if (SSOTokenManager.getInstance().isValidToken(
                            getAppSSOToken())) {
                        SSOTokenManager.getInstance().destroyToken(
                            getAppSSOToken());
                    }
                } catch (Exception ex) {
                    logError("ApplicationSSOTokenShutdownHook.run() : failed " +
                    "with exception ",ex);
                } finally {
                  if (isLogMessageEnabled()) {
                    logMessage("ApplicationSSOTokenShutdownHook.run() :" +
                        " Destroyed Application SSO token.");
                }

            }
            }
    
            private ApplicationSSOTokenShutdownHook(SSOToken token) {
                setAppSSOToken(token);
            }
            
            private void setAppSSOToken(SSOToken sso) {
                _appSSOToken = sso;
            }
            
            private SSOToken getAppSSOToken() {
                return _appSSOToken;
            }
            
            private SSOToken _appSSOToken;
    }

}
