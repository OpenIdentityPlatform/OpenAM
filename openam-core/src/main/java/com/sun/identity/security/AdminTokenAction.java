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
 * $Id: AdminTokenAction.java,v 1.14 2009/06/19 02:35:11 bigfatrat Exp $
 *
 */

/*
 * Portions Copyrighted 2010-2015 ForgeRock AS
 */
package com.sun.identity.security;

import com.iplanet.am.util.AdminUtils;
import com.iplanet.am.util.SystemProperties;
import com.iplanet.services.util.Crypt;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.authentication.internal.AuthContext;
import com.sun.identity.authentication.internal.AuthPrincipal;
import com.sun.identity.common.ShutdownManager;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.util.thread.listener.ShutdownListener;

import java.security.PrivilegedAction;

/**
 * The class is used to perform privileged operations using
 * <code>java.security.AccessController.doPrivileged()
 * </code> when trying to
 * get Application single sign on token. There are four approaches to get single
 * sign on token. 1. Return the single sign on token of the administrator
 * configured in <code>serverconfig.xml</code> if the code runs on server
 * site. 2. If #1 fails, it implies the client is using remote SDK. If
 * <code>com.sun.identity.security.AdminToken</code> is specified in
 * <code>AMConfig.properties</code>, we will call this application token
 * provider plug-in to retrieve the single sign on token. 3. If #2 fails, we
 * look for <code>com.sun.identity.agents.app.username</code> and
 * <code>com.iplanet.am.service.password</code> in
 * <code>AMConfig.properties</code>, if so, we will generate single sign
 * token of administrator based on the user name and password. 4. If #3 fails,
 * we look for <code>com.sun.identity.agents.app.username</code> and
 * <code>com.iplanet.am.service.secret</code> in
 * <code>AMConfig.properties</code>. If so, we will generate single sign on
 * token based on the user name and secret.
 *
 * Note: Java security permissions check for OpenAM can be enabled
 * by setting the property <code>com.sun.identity.security.checkcaller</code> to
 * true in <code>AMConfig.properties</code> file.
 *
 * </PRE>
 *
 * @supported.all.api
 */
public class AdminTokenAction implements PrivilegedAction<SSOToken> {
    public static final String AMADMIN_MODE = "com.sun.identity.security.amadmin";

    //OPENAM-1109 admin token doesn't get cleared
    //because SSOTokenManager#isValidToken() doesn't get
    //real session status. This flag makes AdminTokenAction
    //to refresh session status and get the true status.
    public static final String VALIDATE_SESSION = "openam.identity.security.validateSession";

    static final Debug debug = Debug.getInstance("amSecurity");

    private static final String ADMIN_TOKEN_PROVIDER = "com.sun.identity.security.AdminToken";
    private static final String APP_USERNAME = "com.sun.identity.agents.app.username";
    private static final String APP_SECRET = "com.iplanet.am.service.secret";
    private static final String APP_PASSWORD = "com.iplanet.am.service.password";

    /**
     * Singleton instance.
     */
    private static volatile AdminTokenAction instance;

    private final SSOTokenManager tokenManager;
    private SSOToken appSSOToken;
    private SSOToken internalAppSSOToken;

    private boolean authInitialized;
    private final boolean validateSession;

    /**
     * Returns a cached instance <code>AdminTokenAction</code>.
     *
     * @return instance of <code>AdminTokenAction</code>.
     */
    public static AdminTokenAction getInstance() {
        // Safe double-checked locking pattern (instance is volatile):
        if (instance == null) {
            synchronized (AdminTokenAction.class) {
                if (instance == null) {
                    try {
                        instance = new AdminTokenAction();
                    } catch (SSOException e) {
                        debug.error("AdminTokenAction::init Unable to get SSOTokenManager", e);
                    }
                }
            }
        }
        return instance;
    }

    /**
     * Default constructor
     */
    private AdminTokenAction() throws SSOException {
        tokenManager = SSOTokenManager.getInstance();
        ShutdownManager.getInstance().addApplicationSSOTokenDestroyer(new ShutdownListener() {
            @Override
            public void shutdown() {
                AdminTokenAction.reset();
            }
        });
        validateSession = SystemProperties.getAsBoolean(VALIDATE_SESSION);
    }

    /**
     * Informs AdminTokenAction that Authentication has been initialized
     * This class will start using Authentication service to obtain
     * SSOToken for admin users
     */
    public void authenticationInitialized() {
        authInitialized = true;
        // Generate the DPro's SSOToken
        appSSOToken = getSSOToken();
        if (debug.messageEnabled()) {
            debug.message("AdminTokenAction:authenticationInit " +
                    "called. AppSSOToken className=" + (String)
                    ((appSSOToken == null) ? "null" :
                            appSSOToken.getClass().getName()));
        }
        // Clear internalAppSSOToken
        internalAppSSOToken = null;
    }

    /**
     * Resets cached SSOToken. WITHOUT destroying.  Called when we know the 
     * token is invalid
     */
    public static void invalid() {
        getInstance().invalidate();

        if (debug.messageEnabled()) {
            debug.message("AdminTokenAction:invalid called");
        }
    }

    private void invalidate() {
        appSSOToken = null;
    }

    /**
     * Resets cached SSOToken.
     */
    public static void reset() {
        getInstance().resetInstance();
    }

    private void resetInstance() {
        if (appSSOToken != null) {
            try {
                getInstance().tokenManager.destroyToken(appSSOToken);
            } catch (SSOException ssoe) {
                debug.error("AdminTokenAction.reset: cannot destroy appSSOToken.", ssoe);
            }
            appSSOToken = null;
        }
        internalAppSSOToken = null;
    }

    /* (non-Javadoc)
     * @see java.security.PrivilegedAction#run()
     */
    public SSOToken run() {
        // Check if we have a valid cached SSOToken
        if (appSSOToken != null && tokenManager.isValidToken(appSSOToken)) {
            try {
                if (validateSession) {
                    tokenManager.refreshSession(appSSOToken);
                }
                if (tokenManager.isValidToken(appSSOToken)) {
                    return appSSOToken;
                }
            } catch (SSOException ssoe) {
                debug.error("AdminTokenAction.reset: couldn't retrieve valid token.", ssoe);
            }
        }

        // Check if internalAppSSOToken is present
        if (internalAppSSOToken != null && tokenManager.isValidToken(internalAppSSOToken)) {
            return internalAppSSOToken;
        }

        // Try getting the token from serverconfig.xml
        SSOToken answer = getSSOToken();
        if (answer != null) {
            if (!SystemProperties.isServerMode() || authInitialized) {
                appSSOToken = answer;
            }
            return answer;
        } else if (debug.messageEnabled()) {
            debug.message("AdminTokenAction::run Unable to get SSOToken from serverconfig.xml");
        }

        // Check for configured Application Token Provider in AMConfig.properties
        String appTokenProviderName = SystemProperties.get(ADMIN_TOKEN_PROVIDER);
        if (appTokenProviderName != null) {
            try {
                AppSSOTokenProvider appSSOTokenProvider =
                        Class.forName(appTokenProviderName).asSubclass(AppSSOTokenProvider.class).newInstance();

                answer = appSSOTokenProvider.getAppSSOToken();
            } catch (Throwable ce) {
                debug.error("AdminTokenAction: Exception while calling appSSOToken provider plugin.", ce);
            }
        } else {
            String appUserName = SystemProperties.get(APP_USERNAME);
            String encryptedPassword = SystemProperties.get(APP_SECRET);
            String password = SystemProperties.get(APP_PASSWORD);
            String appPassword = null;

            if (password != null && !password.isEmpty()) {
                appPassword = password;
            } else if (encryptedPassword != null && !encryptedPassword.isEmpty()) {
                try {
                    appPassword = Crypt.decode(encryptedPassword);
                } catch (Throwable t) {
                    debug.error("AdminTokenAction::run Unable to decrypt secret password", t);
                }
            }

            if (appUserName == null || appUserName.isEmpty() || appPassword == null || appPassword.isEmpty()) {
                debug.error("AdminTokenAction: App user name or password is empty");
            } else {
                if (debug.messageEnabled()) {
                    debug.message("App user name: " + appUserName);
                }
                SystemAppTokenProvider tokenProd =
                        new SystemAppTokenProvider(appUserName, appPassword);
                answer = tokenProd.getAppSSOToken();
            }
        }

        // If SSOToken is NULL, AM would not bootstrap: fatal error
        if (answer == null) {
            final String errorMessage = "AdminTokenAction: FATAL ERROR: Cannot obtain Application SSO token.";
            debug.error(errorMessage);
            throw new AMSecurityPropertiesException(errorMessage);
        } else if (!SystemProperties.isServerMode() || authInitialized) {
            // Cache the SSOToken if not in server mode (i.e., in the
            // case of client sdk) or if the authN has been initialized
            appSSOToken = answer;
        }
        return answer;
    }

    private SSOToken getSSOToken() {
        // Please NEVER make this method public!!!!!!!!!!
        // This can only be used in server site. 
        SSOToken ssoAuthToken = null;

        try {
            //call method directly
            if (AdminUtils.getAdminPassword() != null) {
                String adminDN = AdminUtils.getAdminDN();
                String adminPassword = new String(AdminUtils.getAdminPassword());
                if (!authInitialized && (SystemProperties.isServerMode() ||
                        SystemProperties.get(AMADMIN_MODE) != null)) {
                    // Use internal auth context to get the SSOToken
                    AuthContext ac = new AuthContext(new AuthPrincipal(adminDN),
                            adminPassword.toCharArray());
                    internalAppSSOToken = ssoAuthToken = ac.getSSOToken();
                } else {
                    // Copy the authentication state
                    boolean authInit = authInitialized;
                    if (authInit) {
                        authInitialized = false;
                    }

                    // Obtain SSOToken using AuthN service
                    ssoAuthToken = new SystemAppTokenProvider(adminDN, adminPassword).getAppSSOToken();

                    // Restore the authentication state
                    if (authInit && ssoAuthToken != null) {
                        authInitialized = true;
                    }
                }
            }
        } catch (NoClassDefFoundError ne) {
            debug.error("AdminTokenAction::getSSOToken Not found AdminDN and AdminPassword.", ne);

        } catch (Throwable t) {
            debug.error("AdminTokenAction::getSSOToken Exception reading from serverconfig.xml", t);
        }
        return ssoAuthToken;
    }
}
