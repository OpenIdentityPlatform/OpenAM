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
 * $Id: LoginContext.java,v 1.2 2008/06/25 05:41:53 qcheng Exp $
 *
 */

package com.sun.identity.authentication.internal;

import java.util.HashMap;
import java.util.StringTokenizer;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.login.LoginException;

import com.iplanet.am.util.SystemProperties;
import com.sun.identity.authentication.util.ISAuthConstants;

public class LoginContext {

    private AuthSubject subject;

    private CallbackHandler cbHandler;

    private String organization;

    private LoginModule module;

    private HashMap sharedState = new HashMap();

    public final static String LDAP_AUTH_URL = "ldap://";

    public final static String LDAPS_AUTH_URL = "ldaps://";

    public final static String ORGNAME = "ORGANIZATION";

    public final static String PASSWORD = "PASSWORD";

    public final static String AUTH_MODULES = "admin.auth.classname";

    public final static String DONOT_INCLUDE_SMS_MODULE = 
        "admin.auth.donotIncludeSMSModule";

    /* Pre-configured login modules */
    private final static String LDAP_LOGIN_MODULE = 
        "com.sun.identity.authentication.internal.server.LocalLdapAuthModule";

    private final static String SMS_LOGIN_MODULE = 
        "com.sun.identity.authentication.internal.server.SMSAuthModule";

    protected LoginContext(String name, CallbackHandler handler)
            throws LoginException {
        organization = name;
        subject = new AuthSubject();
        cbHandler = handler;
    }

    protected LoginContext(String name, AuthSubject subject,
            CallbackHandler handler) throws LoginException {
        organization = name;
        this.subject = subject;
        cbHandler = handler;
    }

    protected AuthSubject getSubject() {
        return (subject);
    }

    protected void login() throws LoginException {
        HashMap map = new HashMap();
        if (organization != null)
            map.put(ORGNAME, organization);

        // Check for DPro auth login module, unless
        // LDAP authenication is explicitly called
        module = null;
        LoginModule[] modules = null;
        try {
            modules = getLoginModules();
        } catch (Exception e) {
            if (AuthContext.authDebug.warningEnabled()) {
                AuthContext.authDebug.warning("LoginContext::login() "
                        + "Got exception while getting auth module", e);
                AuthContext.authDebug.warning("LoginContext::login() "
                        + "Using default auth modules");
            }
            try {
                modules = getDefaultLoginModules();
            } catch (Exception ee) {
                throw (new LoginException(e.getMessage() + "\n"
                        + ee.getMessage()));
            }
        }

        // Try each module, stop at first successful auth module
        LoginException loginException = null;
        boolean success = false;
        for (int i = 0; i < modules.length; i++) {
            module = modules[i];
            module.initialize(subject, cbHandler, sharedState, map);
            success = false;
            try {
                success = module.login();
            } catch (LoginException le) {
                loginException = le;
            }
            if (success) {
                module.commit();
                break;
            } else {
                module.abort();
            }
        }
        if (!success && (loginException != null)) {
            // None of the modules were successful
            throw (loginException);
        }
    }

    // Package protected method called by AuthLoginThread to set
    // variables in shared state
    void updateSharedState(Callback[] callbacks) {
        for (int i = 0; callbacks != null && i < callbacks.length; i++) {
            if (callbacks[i] instanceof NameCallback) {
                String username = ((NameCallback) callbacks[i]).getName();
                if (username != null) {
                    sharedState.put(ISAuthConstants.SHARED_STATE_USERNAME,
                            username);
                }
            } else if (callbacks[i] instanceof PasswordCallback) {
                char[] passwd = ((PasswordCallback) callbacks[i]).getPassword();
                if (passwd != null) {
                    sharedState.put(ISAuthConstants.SHARED_STATE_PASSWORD,
                            new String(passwd));
                }
            }
        }
    }

    // Package protected method called by AuthContext to set
    // variables in shared state
    void updateSharedState(String username, char[] passwd) {
        sharedState.put(ISAuthConstants.SHARED_STATE_USERNAME, username);
        sharedState.put(ISAuthConstants.SHARED_STATE_PASSWORD, new String(
                passwd));
    }

    protected LoginModule[] getLoginModules() throws Exception {
        LoginModule[] answer = null;
        String modules = SystemProperties.get(AUTH_MODULES);
        if (modules == null) {
            answer = getDefaultLoginModules();
            if (AuthContext.authDebug.messageEnabled()) {
                AuthContext.authDebug.message("LoginContext:getLoginModules() "
                        + "Using default modules");
            }
        } else {
            StringTokenizer st = new StringTokenizer(modules, "|");
            String donotIncludeSMSModule = SystemProperties
                    .get(DONOT_INCLUDE_SMS_MODULE);
            if (donotIncludeSMSModule != null
                    && donotIncludeSMSModule.equalsIgnoreCase("true")) {
                answer = new LoginModule[st.countTokens()];
            } else {
                answer = new LoginModule[st.countTokens() + 1];
                answer[answer.length - 1] = (LoginModule) Class.forName(
                        SMS_LOGIN_MODULE).newInstance();
            }
            for (int i = 0; st.hasMoreTokens(); i++) {
                String moduleClass = st.nextToken();
                answer[i] = (LoginModule) Class.forName(moduleClass)
                        .newInstance();
                if (AuthContext.authDebug.messageEnabled()) {
                    AuthContext.authDebug
                            .message("LoginContext:getLoginModules() "
                                    + "Adding module: " + moduleClass);
                }
            }
        }
        return (answer);
    }

    protected LoginModule[] getDefaultLoginModules() throws Exception {
        LoginModule[] answer = new LoginModule[2];
        answer[0] = (LoginModule) Class.forName(SMS_LOGIN_MODULE).newInstance();
        answer[1] = (LoginModule) Class.forName(LDAP_LOGIN_MODULE)
                .newInstance();
        return (answer);
    }

    protected void logout() throws LoginException {
        module.logout();
    }
}
