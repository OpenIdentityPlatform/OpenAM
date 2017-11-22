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
 * $Id: SMSAuthModule.java,v 1.9 2009/12/11 06:51:37 hengming Exp $
 *
 * Portions Copyrighted 2011-2016 ForgeRock AS.
 */
package com.sun.identity.authentication.internal.server;

import com.iplanet.sso.SSOToken;
import com.sun.identity.authentication.internal.AuthPrincipal;
import com.sun.identity.authentication.internal.AuthSubject;
import com.sun.identity.authentication.internal.LoginModule;
import com.sun.identity.authentication.spi.InvalidPasswordException;
import com.sun.identity.authentication.util.ISAuthConstants;
import com.sun.identity.common.DNUtils;
import com.sun.identity.security.AdminDNAction;
import com.sun.identity.security.AdminPasswordAction;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.encode.Hash;
import com.sun.identity.sm.SMSEntry;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceConfigManager;
import com.sun.identity.sm.ServiceListener;
import com.sun.identity.sm.ServiceSchemaManager;
import java.io.IOException;
import java.security.AccessController;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListMap;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginException;
import com.sun.identity.shared.ldap.util.DN;

/**
 * AM's internal user's authentication module
 */
public class SMSAuthModule implements LoginModule {

    // Static variables
    private static volatile boolean initialized = false;

    private static volatile boolean loadedInternalUsers = false;

    private static volatile boolean registeredCallbackHandler = false;

    private static final Map<String, String> users = new ConcurrentSkipListMap<>(String.CASE_INSENSITIVE_ORDER);

    private static final Map<String, String> userNameToDN = new ConcurrentSkipListMap<>(String.CASE_INSENSITIVE_ORDER);

    private static final Debug debug = Debug.getInstance("amAuthInternalSMModule");

    // Instance variables
    AuthSubject subject;

    String userDN;

    CallbackHandler cb;

    Map sharedState;

    Map options;

    // Constants
    private static final String IDREPO_SERVICE = "sunIdentityRepositoryService";

    private static final String USERS = "users";

    private static final String DN = "dn";

    private static final String PASSWORD = "userPassword";

    public SMSAuthModule() {
        // do nothing
        if (debug.messageEnabled()) {
            debug.message("SMSAuthModule constructor called");
        }
    }

    public void initialize(AuthSubject subject, CallbackHandler cb,
            Map isharedstate, Map ioptions) {
        this.subject = subject;
        this.cb = cb;
        this.sharedState = (isharedstate != null) ? isharedstate
                : Collections.EMPTY_MAP;
        this.options = (ioptions != null) ? ioptions : Collections.EMPTY_MAP;

        if (debug.messageEnabled()) {
            // Copy the shared state and remove password for debugging
            Map ss = new HashMap(sharedState);
            boolean passwordPresent = (ss
                    .remove(ISAuthConstants.SHARED_STATE_PASSWORD) == null) 
                    ? false
                    : true;
            debug.message("SMSAuthModule::initialize called "
                    + "\nPrincipals: "
                    + ((subject != null) ? subject.getPrincipals().toString()
                            : "null")
                    + "\nSharedState: "
                    + ss
                    + "\n"
                    + (passwordPresent ? "<Password Present> "
                            : "<Password Absent>") + "\nOptions: " + options);
        }

        if (!initialized) {
            initialize();
        }
    }
    
    public static void initialize() {
        if (debug.messageEnabled()) {
            debug.message("SMSAuthModule.initialize() Initializing "
                    + "Username and password from serverconfig.xml");
        }
        
        // reset so that internal users set will be reloaded later in time.
        loadedInternalUsers = false;
        
        // initialize caches.
        users.clear();
        userNameToDN.clear();

        // Get internal user names and passwords from serverconfig.xml
        // %%% Might have to get them directory from DSConfigMgr %%%
        // if other than "default" needs to be used
        String name = (String) AccessController
            .doPrivileged(new AdminDNAction());
        String passwd = (String) AccessController
            .doPrivileged(new AdminPasswordAction());
        addUserToCache(name, Hash.hash(passwd));
        if (debug.messageEnabled()) {
            debug.message("SMSAuthModule.initialize() Username "
                    + "serviceconfig.xml: " + name);
        }
        initialized = true;
    }

    public boolean login() throws LoginException {
        // Check if the user is already present
        String username = (String) sharedState
                .get(ISAuthConstants.SHARED_STATE_USERNAME);
        String password = (String) sharedState
                .get(ISAuthConstants.SHARED_STATE_PASSWORD);
        if (debug.messageEnabled()) {
            debug.message("SMSAuthModule::login() From shared state: "
                    + "Username: " + username + " Password: "
                    + ((password == null) ? "<not present>" : "<present>"));
        }

        // Check if we have username and password, if not send callbacks
        if (username == null || password == null) {
            // Request for both username and password
            Callback cbs[] = new Callback[2];
            cbs[0] = new NameCallback("User name: ");
            cbs[1] = new PasswordCallback("Password: ", false);
            try {
                if (debug.messageEnabled()) {
                    debug.message("SMSAuthModule::login() Sending "
                            + "Name & Password Callback");
                }
                cb.handle(cbs);
            } catch (UnsupportedCallbackException e) {
                throw (new LoginException(e.getMessage()));
            } catch (IOException ioe) {
                throw (new LoginException(ioe.getMessage()));
            }
            username = ((NameCallback) cbs[0]).getName();
            char[] passwd = ((PasswordCallback) cbs[1]).getPassword();
            if (passwd != null) {
                password = new String(password);
            }
        }

        // Authenticate the user, return false is username or password is null
        boolean authenticated = false;
        if (username != null && password != null) {
            if (debug.messageEnabled()) {
                debug.message("SMSAuthModule::login() For authentication: "
                        + "Username: " + username + " Password: <present>");
            }
            DN userDNObject = new DN(username);
            if (userDNObject.isDN()) {
                userDN = username;
                username = userDNObject.explodeDN(true)[0];
            } else {
                userDN = (String) userNameToDN.get(username);
                if (userDN == null && !loadedInternalUsers) {
                    // Load the internal users and try to get userDN
                    loadInternalUsers();
                    userDN = (String) userNameToDN.get(username);
                }
            }
            // Need to make sure userDN is not null, since this
            // be set in the subject
            if (userDN != null) {
                // Get the hashed password for the user
                String hash = (String) users.get(username);
                String cachedUserDN = (String) userNameToDN.get(username);
                if (cachedUserDN != null) {
                    String normalizedUserDN = DNUtils.normalizeDN(userDN);
                    if ((normalizedUserDN == null) || 
                        !normalizedUserDN.equals(DNUtils.
                        normalizeDN(cachedUserDN))) {
                        debug.message("SMSAuthModule::login() Invalid User DN");
                        return false;
                    }
                }
                // Compare the hashed password
                boolean invalidPassword = false;
                if (hash != null && hash.equals(Hash.hash(password))) {
                    if (debug.messageEnabled()) {
                        debug.message("SMSAuthModule::login() Success AuthN");
                    }
                    authenticated = true;
                } else if (!loadedInternalUsers) {
                    // Load the internal users and compare hashed passwords
                    if (debug.messageEnabled()) {
                        debug.message("SMSAuthModule::login() "
                                + "Loading internal users");
                    }
                    loadInternalUsers();
                    cachedUserDN = (String) userNameToDN.get(username);
                    if (cachedUserDN != null) {
                        String normalizedUserDN = DNUtils.normalizeDN(userDN);
                        if ((normalizedUserDN == null) || 
                            !normalizedUserDN.equals(DNUtils.
                            normalizeDN(cachedUserDN))) {
                            if (debug.messageEnabled()) {
                                debug.message("SMSAuthModule::login() "
                                + "Invalid User DN");
                            }
                            return false;
                        }
                    } else {
                        return false;
                    }
                    hash = (String) users.get(username);
                    if (hash != null && hash.equals(Hash.hash(password))) {
                        if (debug.messageEnabled()) {
                            debug.message("SMSAuthModule::login() "
                                    + "Success AuthN");
                        }
                        authenticated = true;
                    } else if (hash != null) {
                        // Password must be invalid
                        invalidPassword = true;
                    }
                } else if (hash != null) {
                    // Password must be invalid
                    invalidPassword = true;
                }
                if (invalidPassword) {
                    throw (new InvalidPasswordException("invalid password",
                            userDN));
                }
            }
        }
        return (authenticated);
    }

    public boolean abort() throws LoginException {
        // do nothing
        return (true);
    }

    public boolean commit() throws LoginException {
        // add username to Subject
        if (debug.messageEnabled()) {
            debug.message("SMSAuthModule::commit() Adding Principal: " + userDN
                    + " to Subject");
        }
        Set principals = subject.getPrincipals();
        if (principals.isEmpty()) {
            principals.add(new AuthPrincipal(userDN));
        }
        return (true);
    }

    public boolean logout() throws LoginException {
        // do nothing
        return (true);
    }

    private static synchronized void loadInternalUsers() {
        if (loadedInternalUsers) {
            return;
        }

        // Get AdminSSOToken
        try {
            SSOToken ssoToken = (SSOToken) AccessController
                    .doPrivileged(AdminTokenAction.getInstance());
            ServiceConfigManager scm = new ServiceConfigManager(IDREPO_SERVICE,
                    ssoToken);
            ServiceSchemaManager ssm = new ServiceSchemaManager(IDREPO_SERVICE,
                    ssoToken);
            ServiceConfig sc = scm.getGlobalConfig(null);
            sc = sc.getSubConfig(USERS);
            for (Iterator items = sc.getSubConfigNames().iterator(); items
                    .hasNext();) {
                String scName = (String) items.next();
                ServiceConfig s = sc.getSubConfig(scName);
                Map attrs = s.getAttributes();
                String name = null;
                Set set = (Set) attrs.get(DN);
                if (set != null && !set.isEmpty()) {
                    name = (String) set.iterator().next();
                    // Add root suffix, if revision is greater than 30
                    if (ssm.getRevisionNumber() >= 30) {
                        // In the case of upgrade the DN will have the suffix
                        // Hence check if it ends with SMS root suffix
                        if (name.toLowerCase().endsWith(
                            SMSEntry.getRootSuffix().toLowerCase())) {
                            // Replace only if the they are different
                            if (!SMSEntry.getRootSuffix().equals(
                                SMSEntry.getAMSdkBaseDN())) {
                                name = name.substring(0, name.length() -
                                    SMSEntry.getRootSuffix().length());
                                name = name + SMSEntry.getAMSdkBaseDN();
                            }
                        } else {
                            name = name + SMSEntry.getAMSdkBaseDN();
                        }
                    }
                }
                String hash = null;
                set = (Set) attrs.get(PASSWORD);
                if (set != null && !set.isEmpty()) {
                    hash = (String) set.iterator().next();
                }
                if (debug.messageEnabled()) {
                    debug.message("SMSAuthModule::loadInternalUsers() "
                            + "Added user: " + name);
                }
                addUserToCache(name, hash);
            }
            loadedInternalUsers = true;
            // Setup listeners
            if (!registeredCallbackHandler) {
                scm.addListener(new SMSAuthModuleListener());
                registeredCallbackHandler = true;
            }
        } catch (Exception e) {
            // Handle the exception
        }
    }

    private static void addUserToCache(String name, String hash) {
        // Add the DN
        users.put(name, hash);
        // Add the "name"
        String[] rdns = (new DN(name)).explodeDN(true);
        users.put(rdns[0], hash);
        userNameToDN.put(rdns[0], name);
    }

    // Inner class for receiving SMS notifications
    static class SMSAuthModuleListener implements ServiceListener {

        SMSAuthModuleListener() {
            // Do nothing
            if (debug.messageEnabled()) {
                debug.message("SMSAuthModuleListener::init called");
            }
        }

        public void schemaChanged(String serviceName, String version) {
            // Ignore
            if (debug.messageEnabled()) {
                debug.message("SMSAuthModuleListener::schemaChanged called");
            }
        }

        public void globalConfigChanged(String serviceName, String version,
                String groupName, String serviceComponent, int type) {
            if (debug.messageEnabled()) {
                debug.message("SMSAuthModuleListener::globalConfigChanged");
            }
            if (serviceName.equalsIgnoreCase(IDREPO_SERVICE)) {
                // Force the loading of internal users
                loadedInternalUsers = false;
                users.clear();
            }
        }

        public void organizationConfigChanged(String serviceName,
                String version, String orgName, String groupName,
                String serviceComponent, int type) {
            // Ignore
            if (debug.messageEnabled()) {
                debug.message("SMSAuthModuleListener::orgConfigChanged");
            }
        }
    }
}
