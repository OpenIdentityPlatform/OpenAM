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
 * $Id: Application.java,v 1.9 2009/07/23 18:54:17 qcheng Exp $
 *
 */

/*
 * Portions Copyrighted 2011 ForgeRock AS
 */

package com.sun.identity.authentication.modules.application;

import java.security.AccessController;
import java.security.Principal;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.servlet.http.HttpServletRequest;

import com.iplanet.am.util.SystemProperties;
import com.sun.identity.authentication.spi.InvalidPasswordException;
import com.sun.identity.authentication.spi.AMLoginModule;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.authentication.service.AuthD;
import com.sun.identity.authentication.util.ISAuthConstants;
import com.sun.identity.idm.AMIdentityRepository;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.plugins.ldapv3.LDAPAuthUtils;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.datastruct.CollectionHelper;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.security.DecodeAction;
import com.sun.identity.shared.ldap.util.LDAPUtilException;
import com.sun.identity.sm.SMSEntry;

/**
 * Application login module.<br>
 * Use <code>IDToken0</code> to specify application name and
 * <code>IDToken1</code> to specify secret.
 * (Old usage : <code>Login.Token0</code> to specify application name and 
 *  <code>Login.Token1</code> to specify secret.) For example:
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
    private ResourceBundle bundle = null;
    private LDAPAuthUtils ldapUtil;
    private Map currentConfig;
    private static boolean ldapSSL = false;
    
    static {
        debug.message("Application module getting secret");
        String tmp = SystemProperties.get(
            Constants.AM_SERVICES_SECRET).trim();
        secret = (String) AccessController.doPrivileged(
            new DecodeAction(tmp));
        ldapSSL = Boolean.valueOf(SystemProperties.get(
            Constants.AM_DIRECTORY_SSL_ENABLED, "false")).booleanValue();
    }
    
    public Application() {
    }
    
    public void init(Subject subject, Map sharedState, Map options) {
        try {
            debug.message("in initialize...");
            java.util.Locale locale  = getLoginLocale();
            bundle = amCache.getResBundle(amAuthApplication, locale);
            if (debug.messageEnabled()) {
                debug.message("ApplicationAuth resbundle locale="+locale);
            }
        } catch (Exception e) {
            debug.error("ApplicationAuthModule Init: " + e.getMessage());
            if (debug.messageEnabled()) {
                debug.message("Stack trace: ", e);
            }
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
                throw new AuthLoginException(amAuthApplication, "wrongSecret",
                null);
            }
            secretParam = (String) map.get("secret");
            userName = (String) map.get("uid");
        }
        if (secretParam == null || secretParam.length() == 0) {
            throw new AuthLoginException(amAuthApplication, "noPassword", null);
        }
        
        if (secret != null && secret.length() != 0 &&
            secretParam.equals(secret)
        ) {
            if (debug.messageEnabled()) {
                debug.message(
                    "App.validate, secret matched for user : " + userName);
            }
            if (userName == null || userName.length() == 0) {
                // backward compatible with the gateway for portal
                newUserName =
                    ISAuthConstants.APPLICATION_USER_PREFIX + "gateway";
            }
            else {
                newUserName =
                    ISAuthConstants.APPLICATION_USER_PREFIX + userName;
            }
            String userDNString = ISAuthConstants.APPLICATION_USER_NAMING_ATTR +
                "=" + newUserName + "," +
                ISAuthConstants.SPECIAL_USERS_CONTAINER + "," +
                SMSEntry.getRootSuffix();

            if (!isValidUserEntry(userDNString)) {
                debug.message(
                    userDNString + " is not a valid special user entry");
                if (!doFallbackAuth(userName, secretParam)) {
                    debug.error(
                        "App validation failed, User not Valid: " + userName);
                    setFailureID(userName);
                    throw new AuthLoginException(
                        amAuthApplication, "userInvalid",
                    null);
                }
            } else {
                userTokenId = userDNString;
            }
        } else if (!doFallbackAuth(userName, secretParam)) {
            debug.error("App validation failed, User not Valid: " + userName);
            setFailureID(userName);
            throw new AuthLoginException(amAuthApplication, "userInvalid",
                null);
        }
        
        return ISAuthConstants.LOGIN_SUCCEED;
    }
    
    private boolean doFallbackAuth(String userName, String userPassword)
        throws AuthLoginException {
        boolean success = false;

        if (debug.messageEnabled()){
            debug.message("doFallbackAuth : User = " + userName);
        }
        
        if (userName != null && (userName.length() != 0)) {
            if (authenticateToDatastore(userName, userPassword)) {
                if (debug.messageEnabled()){
                    debug.message("Application.doFallbackAuth: Authenticating "
                    + "to DataStore Auth Module.");
                }
                if (userTokenId == null) {
                    userTokenId = userName;
                }
                success = true;
            } else if (authenticateToLDAP(userName, userPassword) ==
                LDAPAuthUtils.SUCCESS
            ) {
                if (userTokenId == null) {
                    userTokenId = ldapUtil.getUserId();
                }
                if (debug.messageEnabled()){
                    debug.message(
                        "Auth is successful,returning User = " + userTokenId);
                }
                success = true;
            }
        }
        return success;
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
            if (debug.messageEnabled()) {
                debug.message("Callback is.. :" + callbacks);
            }
            callbackHandler.handle(callbacks);
            
            // map to hold return
            Map map = new HashMap();
            
            // process return
            int len = callbacks.length;
            for (int i = 0; i < len; i ++) {
                Callback cb = callbacks[i];
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
            debug.error("sendCallback: " + e.getMessage());
            if (debug.messageEnabled()){
                debug.message("Stack trace: ", e);
            }
        }
        return null;
    }

    private int authenticateToLDAP(String userName, String userPassword)
            throws AuthLoginException {
        if (debug.messageEnabled()){
            debug.message("In authenticateToLDAP with User : " +  userName);
        }
        try {
            if (isSuperAdmin(userName)) {
                ldapUtil = new LDAPAuthUtils(AuthD.directoryHostName,
                AuthD.directoryPort, ldapSSL, getLoginLocale(), debug);
                ldapUtil.authenticateSuperAdmin(userName, userPassword);
                if (ldapUtil.getState() == LDAPAuthUtils.SUCCESS) {
                    userTokenId = userName;
                } else {
                    debug.message("Invalid adminID or admin Password");
                    setFailureID(ldapUtil.getUserId(userName));
                    throw new AuthLoginException(amAuthApplication,
                        "InvalidUP", null);
                }
            } else {
                if (initLDAPAttributes(ISAuthConstants.LDAP_SERVICE_NAME)) {
                    ldapUtil.authenticateUser(userName, userPassword);
                } else {
                    debug.message("Invalid userID or user Password");
                    setFailureID(userName);
                    throw new AuthLoginException(amAuthApplication,
                        "basicLDAPex", null);
                }
            }
            return ldapUtil.getState();
        } catch (LDAPUtilException ex) {
            setFailureID(userName);
            switch ( ex.getLDAPResultCode() ) {
                case LDAPUtilException.NO_SUCH_OBJECT:
                    debug.message( "The specified user does not exist." );
                    throw new AuthLoginException(
                        amAuthApplication, "NoUser", null);
                case LDAPUtilException.INVALID_CREDENTIALS:
                    debug.message( "Invalid password." );
                    String failureUserID = ldapUtil.getUserId();
                    throw new InvalidPasswordException(
                        amAuthApplication, "InvalidUP", null,
                        failureUserID, ex);
                default:
                    throw new AuthLoginException(amAuthApplication,
                    "basicLDAPex", null);
            }
        }
    }

    /**
     * Authenticates to the datastore using idRepo API
     *
     * @param userName User Name
     * @param userPassword User Password
     * @return <code>true</code> if success. <code>false</code> if failure
     * @throws <code> AuthLoginException </code> 
     */
    private boolean authenticateToDatastore(String userName, 
        String userPassword) throws AuthLoginException {
        boolean retval = false;
        Callback[] callbacks = new Callback[2];
        NameCallback nameCallback = new NameCallback("NamePrompt");
        nameCallback.setName(userName);
        callbacks[0] = nameCallback;
        PasswordCallback passwordCallback = new PasswordCallback(
            "PasswordPrompt",false);
        passwordCallback.setPassword(userPassword.toCharArray());
        callbacks[1] = passwordCallback;
        try {
            AMIdentityRepository idrepo = getAMIdentityRepository(
                getRequestOrg());
            retval = idrepo.authenticate(callbacks);
        } catch (IdRepoException idrepoExp) {
            if (debug.messageEnabled()){
                debug.message("Application.authenticateToDatastore:  "
                    + "IdRepo Exception", idrepoExp);
            }
        }
        return retval;

    }
    
    private boolean initLDAPAttributes(String serviceName)
            throws AuthLoginException {
        String serverHost = null;
        currentConfig =  getOrgServiceTemplate(getRequestOrg(),serviceName);
        
        try {
            // All LDAP module Attribute Initialization done here ...
            serverHost = CollectionHelper.getServerMapAttr(currentConfig,
                ISAuthConstants.LDAP_SERVER);
            
            if (serverHost == null) {
                debug.message("No server for configuring");
                return false;
            }
            
            String baseDN  = CollectionHelper.getServerMapAttr(currentConfig,
                ISAuthConstants.LDAP_BASEDN);

            if (baseDN == null) {
                debug.error(
                    "Fatal error: baseDN for search has invalid value");
                throw new AuthLoginException(amAuthApplication, "basednnull",
                    null);
            }
            
            String bindDN = CollectionHelper.getMapAttr(currentConfig,
                ISAuthConstants.LDAP_BINDDN, "");
            String bindPassword = CollectionHelper.getMapAttr(
                currentConfig, ISAuthConstants.LDAP_BINDPWD, "");
            String userNamingAttr = CollectionHelper.getMapAttr(
                currentConfig, ISAuthConstants.LDAP_UNA, "uid");
            Set userSearchAttrs =
                (Set)currentConfig.get(ISAuthConstants.LDAP_USERSEARCH);
            String searchFilter = CollectionHelper.getMapAttr(currentConfig,
                ISAuthConstants.LDAP_SEARCHFILTER, "");
            boolean ssl = Boolean.valueOf(CollectionHelper.getMapAttr(
                currentConfig, ISAuthConstants.LDAP_SSL, "false")
                ).booleanValue();

            String tmp = CollectionHelper.getMapAttr(currentConfig,
                ISAuthConstants.LDAP_SEARCHSCOPE, "SUBTREE");
            
            int searchScope = 2;// SUBTREE is the default
            if (tmp.equalsIgnoreCase("OBJECT")) {
                searchScope=0;
            } else if (tmp.equalsIgnoreCase("ONELEVEL")) {
                searchScope=1;
            }

            String returnUserDN = CollectionHelper.getMapAttr(currentConfig,
                ISAuthConstants.LDAP_RETURNUSERDN, "true");
            
            // set LDAP Parameters
            int index = serverHost.indexOf(':');
            int serverPort =389;
            String port = null;
            
            if (index != -1) {
                port = serverHost.substring(index+1);
                serverPort = Integer.parseInt(port);
                serverHost = serverHost.substring(0,index);
            }
            
            // set the optional attributes here
            ldapUtil = new LDAPAuthUtils(serverHost,serverPort,ssl,getLoginLocale(),
                baseDN, debug);
            ldapUtil.setScope(searchScope) ;
            ldapUtil.setFilter(searchFilter);
            ldapUtil.setUserNamingAttribute(userNamingAttr);
            ldapUtil.setUserSearchAttribute(userSearchAttrs);
            ldapUtil.setAuthPassword(bindPassword);
            ldapUtil.setAuthDN(bindDN);
            ldapUtil.setReturnUserDN(returnUserDN);
            
            if (debug.messageEnabled()) {
                debug.message("bindDN-> " + bindDN +
                    "\nbaseDN-> " + baseDN +
                    "\nuserNamingAttr-> " + userNamingAttr+
                    "\nuserSearchAttr(s)-> " + userSearchAttrs+
                    "\nsearchFilter-> " + searchFilter +
                    "\nsearchScope-> " + searchScope +
                    "\nssl-> " + ssl+
                    "\nHost: "+serverHost+
                    "\nINDEDX : "+index+
                    "\nPORT : "+serverPort);
            }
            return true;
            
        } catch(Exception ex) {
            debug.error("LDAP Init Exception", ex);
            throw new AuthLoginException(amAuthApplication, "basicLDAPex",
                null, ex);
        }
        
    }
    
    public void destroyModuleState() {
        userTokenId = null;
        userPrincipal = null;
    }
    
    public void nullifyUsedVars() {
        errorMsg = null;
        bundle = null;
        ldapUtil = null;
        currentConfig = null;
    }
}
