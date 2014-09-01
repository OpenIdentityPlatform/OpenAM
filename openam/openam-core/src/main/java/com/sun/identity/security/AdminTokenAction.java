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
 * Portions Copyrighted 2010-2011 ForgeRock AS
 */
package com.sun.identity.security;

import java.security.PrivilegedAction;

import com.iplanet.am.util.AdminUtils;
import com.iplanet.am.util.SystemProperties;
import com.iplanet.services.util.Crypt;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.authentication.internal.AuthContext;
import com.sun.identity.authentication.internal.AuthPrincipal;
import com.sun.identity.common.ShutdownListener;
import com.sun.identity.common.ShutdownManager;
import com.sun.identity.shared.debug.Debug;

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
 * Note: Java security permissions check for OpenSSO can be enabled
 * by setting the property <code>com.sun.identity.security.checkcaller</code> to
 * true in <code>AMConfig.properties</code> file.
 * 
 * </PRE>
 * 
 * @supported.all.api
 */
public class AdminTokenAction implements PrivilegedAction<SSOToken> {
    
    private static SSOTokenManager tokenManager;
    
    private static SSOToken appSSOToken;
    
    private static SSOToken internalAppSSOToken;
    
    private static AdminTokenAction instance;

    private static boolean authInitialized;

    static Debug debug = Debug.getInstance("amSecurity");
    
    static final String ADMIN_TOKEN_PROVIDER =
        "com.sun.identity.security.AdminToken";
    
    static final String APP_USERNAME =
	"com.sun.identity.agents.app.username";
    
    static final String APP_SECRET =
	"com.iplanet.am.service.secret";
    
    static final String APP_PASSWORD =
	"com.iplanet.am.service.password";
    
    public static final String AMADMIN_MODE =
	"com.sun.identity.security.amadmin";

    /**
     * Returns a cached instance <code>AdminTokenAction</code>.
     *
     * @return instance of <code>AdminTokenAction</code>.
     */
    public static AdminTokenAction getInstance() {
	if (instance == null) {
	    instance = new AdminTokenAction();
	}
	return instance;
    }

    /**
     * Default constructor
     */
    public AdminTokenAction() {
	if (tokenManager == null) {
	    try {
		tokenManager = SSOTokenManager.getInstance();
                ShutdownManager.getInstance().addApplicationSSOTokenDestoryer(
                    new ShutdownListener() {
                        public void shutdown() {
                            AdminTokenAction.reset();
                        }
                    }); 
	    } catch (SSOException ssoe) {
		debug.error("AdminTokenAction::init Unable to get " +
		    "SSOTokenManager", ssoe);
	    }
	}
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
        appSSOToken = null;

        if (debug.messageEnabled()) {
            debug.message("AdminTokenAction:invalid called");
        }
   }

    /**
     * Resets cached SSOToken.
     */
    public static void reset() {
        if (appSSOToken != null) {
	    if (tokenManager != null) {
                try {
                    tokenManager.destroyToken(appSSOToken);
                } catch (SSOException ssoe) {
                    debug.error(
                        "AdminTokenAction.reset: cannot destroy appSSOToken.",
                        ssoe);
                }
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
	if ((appSSOToken != null) && tokenManager.isValidToken(appSSOToken)) {
	    return (appSSOToken);
	}

        // Check if internalAppSSOToken is present
        if ((internalAppSSOToken != null) &&
            tokenManager.isValidToken(internalAppSSOToken)) {
            return (internalAppSSOToken);
        }

	// Try getting the token from serverconfig.xml
	SSOToken answer = getSSOToken();
	if (answer != null) {
	    if (!SystemProperties.isServerMode() || authInitialized) {
		appSSOToken = answer;
	    }
	    return answer;
	} else if (debug.messageEnabled()) {
	    debug.message("AdminTokenAction::run Unable to get SSOToken " +
		" from serverconfig.xml");
        }

	// Check for configured Application Token Provider
	// in AMConfig.properties
	String appTokenProviderName = SystemProperties.get(
             ADMIN_TOKEN_PROVIDER);
 	if (appTokenProviderName != null) {
	    try {
                AppSSOTokenProvider appSSOTokenProvider = 
            	    (AppSSOTokenProvider) Class.
            	    forName(appTokenProviderName).newInstance(); 
            	
                answer = appSSOTokenProvider.getAppSSOToken();
            } catch (Throwable ce) {
		if (debug.warningEnabled()) {
		    debug.warning("AdminTokenAction: Exception " +
			"while calling appSSOToken provider plugin.", ce);
		}
            } 
        } else {
            String appUserName = SystemProperties.get(APP_USERNAME);
            String tmp = SystemProperties.get(APP_SECRET);
            String passwd = SystemProperties.get(APP_PASSWORD);
            String appPassword = null; 

            if (passwd != null && passwd.length() != 0) {
                appPassword = passwd; 
            } else if (tmp != null && tmp.length() != 0) {
		try {
		    appPassword = Crypt.decode(tmp);
		} catch (Throwable t) {
		    if (debug.messageEnabled()) {
			debug.message("AdminTokenAction::run Unable to " +
			    " decrypt secret password", t);
		    }
		}
            }

            if ((appUserName == null) || (appUserName.length() == 0) || 
               (appPassword == null) || (appPassword.length() == 0)) {
		debug.message(
		    "AdminTokenAction: App user name or password is empty");
            } else {
	        if (AdminTokenAction.debug.messageEnabled()) {
	            debug.message("App user name: " + appUserName);
	    	}
                SystemAppTokenProvider tokenProd =
		    new SystemAppTokenProvider(appUserName, appPassword);
                answer = tokenProd.getAppSSOToken();  
            }
        }    

	// If SSOToken is NULL, AM would not bootstrap: fatal error
        if (answer == null) {
            debug.error("AdminTokenAction: FATAL ERROR: " +
		"Cannot obtain Application SSO token." +
		"\nCheck AMConfig.properties for the following properties" +
		"\n\tcom.sun.identity.agents.app.username" +
		"\n\tcom.iplanet.am.service.password");
	    throw new AMSecurityPropertiesException("AdminTokenAction: " + 
                " FATAL ERROR: Cannot obtain Application SSO token." +
		"\nCheck AMConfig.properties for the following properties" +
		"\n\tcom.sun.identity.agents.app.username" +
		"\n\tcom.iplanet.am.service.password");
        } else if (!SystemProperties.isServerMode() || authInitialized) {
	    // Cache the SSOToken if not in server mode (i.e., in the
	    // case of client sdk) or if the authN has been initialized
	    appSSOToken = answer;
	}
        return answer;
    }
    
    private static SSOToken getSSOToken() {
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
		    ssoAuthToken = (new SystemAppTokenProvider(adminDN,
		        adminPassword)).getAppSSOToken();

		    // Restore the authentication state
		    if (authInit && ssoAuthToken != null) {
		        authInitialized = true;
		    }
	        }
	    }
        } catch (NoClassDefFoundError ne) {
	    if (debug.messageEnabled()) {
		debug.message("AdminTokenAction::getSSOToken " +
		    "Not found AdminDN and AdminPassword.", ne);
	    }
        } catch (Throwable t) {
	    if (debug.messageEnabled()) {
		debug.message("AdminTokenAction::getSSOToken " +
		    "Exception reading from serverconfig.xml", t);
	    }
	}
        return ssoAuthToken;
    }
}
