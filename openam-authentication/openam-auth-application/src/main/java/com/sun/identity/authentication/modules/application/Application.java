/*
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
 * $Id: Application.java,v 1.9 2009/07/23 18:54:17 qcheng Exp $
 *
 * Portions Copyrighted 2011-2016 ForgeRock AS.
 */

package com.sun.identity.authentication.modules.application;

import static com.sun.identity.authentication.util.ISAuthConstants.SPECIAL_USERS_CONTAINER;
import static com.sun.identity.sm.SMSEntry.getRootSuffix;

import java.security.AccessController;
import java.security.Principal;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.servlet.http.HttpServletRequest;

import org.forgerock.openam.ldap.LDAPUtils;
import org.forgerock.opendj.ldap.DN;

import com.iplanet.am.util.SystemProperties;
import com.sun.identity.authentication.spi.AMLoginModule;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.authentication.util.ISAuthConstants;
import com.sun.identity.idm.AMIdentityRepository;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdType;
import com.sun.identity.security.DecodeAction;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.debug.Debug;

/**
 * Application login module. This is used to authenticate agents and users of ssoadm.
 * <p>
 * Use <code>IDToken0</code> to specify application name and
 * <code>IDToken1</code> to specify secret.
 * (Old usage : <code>Login.Token0</code> to specify application name and 
 *  <code>Login.Token1</code> to specify secret.) For example:
 * </p>
 * <pre>
 * "module=Application&IDToken0=UrlAccessAgent&IDToken1=secret"
 * </pre>
 * OR
 * <pre>
 * "module=Application&IDToken0=<user id for Agent>&IDToken1=
 *     <password for Agent user>"
 * </pre>
 * Old usage:
 * <pre>
 * "module=Application&Login.Token0=UrlAccessAgent&Login.Token1=secret"
 * </pre>
 */
public class Application extends AMLoginModule {
    private String userTokenId = null;
    private Principal userPrincipal = null;
    private String errorMsg = null;
    private static String secret = null;
    private static final String amAuthApplication = "amAuthApplication";
    private static Debug debug = Debug.getInstance(amAuthApplication);
    private static final DN SPECIAL_USERS_ROOT = DN.valueOf(SPECIAL_USERS_CONTAINER + "," + getRootSuffix());
    private ResourceBundle bundle = null;

    static {
        debug.message("Application module getting secret");
        String tmp = SystemProperties.get(Constants.AM_SERVICES_SECRET).trim();
        secret = AccessController.doPrivileged(new DecodeAction(tmp));
    }
    
    public void init(Subject subject, Map sharedState, Map options) {
        try {
            debug.message("in initialize...");
            java.util.Locale locale  = getLoginLocale();
            bundle = amCache.getResBundle(amAuthApplication, locale);
            debug.message("ApplicationAuth resbundle locale={}", locale);
        } catch (Exception e) {
            debug.error("ApplicationAuthModule Init: {}", e.getMessage());
            debug.message("Stack trace: ", e);
            errorMsg = "appInitFalied";
        }
        
        if (secret == null || secret.length() == 0) {
            debug.message("Init : NULL secret in AMConfig.properties");
        }
    }
    
    /**
     * Implementation of <code>AMLoginModule</code> abstract method.
     * Refer to <code>AMLoginModule</code> for method syntax.
     */
    public int process(Callback[] callbacks, int state)
        throws AuthLoginException {
        // check if there is any error during initialize
        if (errorMsg != null) {
            throw new AuthLoginException(amAuthApplication, errorMsg, null);
        }
        HttpServletRequest req = getHttpServletRequest();
        String userName = null;
        String newUserName = null;
        String secretParam = null;
        if (req != null) {
            userName = req.getParameter("IDToken0");
            secretParam = req.getParameter("IDToken1");
            if (userName == null && secretParam == null) {
                userName = req.getParameter("Login.Token0");
                secretParam = req.getParameter("Login.Token1");
            }
        }
        if (secretParam == null && userName == null) {
            Map map = sendCallback();
            if (map == null || map.isEmpty()) {
                throw new AuthLoginException(amAuthApplication, "wrongSecret", null);
            }
            secretParam = (String) map.get("secret");
            userName = (String) map.get("uid");
        }
        if (secretParam == null || secretParam.length() == 0) {
            throw new AuthLoginException(amAuthApplication, "noPassword", null);
        }
        
        if (secret != null && secret.length() != 0 && secretParam.equals(secret)) {
            debug.message("App.validate, secret matched for user : {}", userName);
            if (userName == null || userName.length() == 0) {
                // backward compatible with the gateway for portal
                newUserName = ISAuthConstants.APPLICATION_USER_PREFIX + "gateway";
            } else {
                newUserName = ISAuthConstants.APPLICATION_USER_PREFIX + userName;
            }
            String userDNString = ISAuthConstants.APPLICATION_USER_NAMING_ATTR +
                "=" + newUserName + "," + SPECIAL_USERS_CONTAINER + "," + getRootSuffix();

            if (!isValidUserEntry(userDNString)) {
                debug.message("{} is not a valid special user entry", userDNString);
                if (!doFallbackAuth(userName, secretParam)) {
                    debug.error("App validation failed, User not Valid: {}", userName);
                    setFailureID(userName);
                    throw new AuthLoginException(amAuthApplication, "userInvalid", null);
                }
            } else {
                userTokenId = userDNString;
            }
        } else if (!doFallbackAuth(userName, secretParam)) {
            debug.error("App validation failed, User not Valid: " + userName);
            setFailureID(userName);
            throw new AuthLoginException(amAuthApplication, "userInvalid", null);
        }
        
        return ISAuthConstants.LOGIN_SUCCEED;
    }
    
    private boolean doFallbackAuth(String userName, String userPassword)
        throws AuthLoginException {
        boolean success = false;

        debug.message("doFallbackAuth : User = {}", userName);

        if (userName != null && (userName.length() != 0)) {
            if (authenticateToDatastore(userName, userPassword)) {
                debug.message("Application.doFallbackAuth: Authenticated to AgentsRepo.");
                if (userTokenId == null) {
                    userTokenId = userName;
                }
                success = true;
            }
        }
        return success;
    }

    /**
     * Authenticates to the datastore using idRepo API
     *
     * @param userName User Name
     * @param userPassword User Password
     * @return <code>true</code> if success. <code>false</code> if failure
     * @throws AuthLoginException
     */
    private boolean authenticateToDatastore(String userName, String userPassword) throws AuthLoginException {
        boolean retval = false;
        Callback[] callbacks = new Callback[2];
        NameCallback nameCallback = new NameCallback("NamePrompt");
        nameCallback.setName(userName);
        callbacks[0] = nameCallback;
        PasswordCallback passwordCallback = new PasswordCallback("PasswordPrompt",false);
        passwordCallback.setPassword(userPassword.toCharArray());
        callbacks[1] = passwordCallback;
        try {
            AMIdentityRepository idrepo = getAMIdentityRepository(getRequestOrg());
            retval = idrepo.authenticate(IdType.AGENT, callbacks);
        } catch (IdRepoException idrepoExp) {
            debug.message("Application.authenticateToDatastore: IdRepo Exception", idrepoExp);
        }
        return retval;

    }

    /**
     * Returns Principal for the authenticated user.
     *
     * @return Principal for the authenticated user or null if
     *         authentication did not succeed.
     */
    public java.security.Principal getPrincipal() {
        if (userPrincipal != null) {
            return userPrincipal;
        } else if (userTokenId != null) {
            userPrincipal = new ApplicationPrincipal(userTokenId);
            return userPrincipal;
        } else {
            return null;
        }
    }

    /**
     * Sends callbacks to get appname and/or secret
     * @return Map contains appname and/or secret, key "uid" corresponding
     *         to appname, key "secret" corresponds to secret
     */
    private Map sendCallback() {
        try {
            CallbackHandler callbackHandler = getCallbackHandler();
            if (callbackHandler == null) {
                throw new AuthLoginException(amAuthApplication,
                "NoCallbackHandler", null);
            }
            Callback[] callbacks = new Callback[2];
            callbacks[0] = new NameCallback(bundle.getString("appname"));
            callbacks[1] = new PasswordCallback(
            bundle.getString("secret"), true);
            debug.message("Callback is.. : {}", callbacks);
            callbackHandler.handle(callbacks);

            // map to hold return
            Map<String, String> map = new HashMap<>();

            // process return
            for (Callback cb : callbacks) {
                if (cb instanceof PasswordCallback) {
                    char[] pass = ((PasswordCallback) cb).getPassword();
                    if (pass != null) {
                        map.put("secret", new String(pass));
                    }
                } else if (cb instanceof NameCallback) {
                    String username = ((NameCallback) cb).getName();
                    if (username != null) {
                        map.put("uid", username);
                    }
                }
            }
            return map;
        } catch (Exception e) {
            debug.error("sendCallback: {}", e.getMessage());
            debug.message("Stack trace: ", e);
        }
        return null;
    }

    public void destroyModuleState() {
        userTokenId = null;
        userPrincipal = null;
    }
    
    public void nullifyUsedVars() {
        errorMsg = null;
        bundle = null;
    }
}
