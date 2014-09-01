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
 * $Id: AMLoginContext.java,v 1.24 2009/12/23 20:03:04 mrudul_uchil Exp $
 *
 */

/**
 * Portions Copyrighted 2011-2012 ForgeRock Inc
 */
package com.sun.identity.authentication.service;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.authentication.AuthContext;
import com.sun.identity.authentication.config.AMAuthConfigUtils;
import com.sun.identity.authentication.config.AMAuthLevelManager;
import com.sun.identity.authentication.config.AMAuthenticationInstance;
import com.sun.identity.authentication.config.AMAuthenticationManager;
import com.sun.identity.authentication.config.AMConfiguration;
import com.sun.identity.authentication.config.AMConfigurationException;
import com.sun.identity.authentication.server.AuthContextLocal;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.authentication.spi.InvalidPasswordException;
import com.sun.identity.authentication.spi.MessageLoginException;
import com.sun.identity.authentication.spi.RedirectCallback;
import com.sun.identity.authentication.util.ISAuthConstants;
import com.sun.identity.authentication.util.AMAuthUtils;
import com.sun.identity.common.DNUtils;
import com.sun.identity.sm.DNMapper;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.encode.URLEncDec;
import com.sun.identity.shared.locale.AMResourceBundleCache;
import java.security.Principal;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.ResourceBundle;
import java.util.Set;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.Configuration;
import javax.security.auth.login.LoginException;
import javax.servlet.http.HttpServletRequest;

import com.sun.identity.monitoring.Agent;
import com.sun.identity.monitoring.MonitoringUtil;
import com.sun.identity.monitoring.SsoServerAuthSvcImpl;
import com.sun.identity.security.AdminTokenAction;
import java.security.AccessController;

/**
 * <code>AMLoginContext</code> class is the core layer in the authentication 
 * middle tier which connects user clients to the JAAS <code>LoginModule</code>.
 * The <code>AMLoginContext</code> executes pre and post authentication process
 * based on authentication status.
 * <p>
 * <code>AMLoginContext</code> provides a synchronous layer on top of the JAAS
 * framework for appropriate user interaction and communication between clients
 * and authentication module via callbacks requirements
 * <p>
 * <code>AMLoginContext</code> sets and retrieves the authenticaton
 * configuration entry
 * <p>
 * This class actually starts the JAAS login process by instantiating the
 * <code>LoginContext</code> object with the JAAS configuration name and the
 * <code>CallbackHandler</code> followed by calling the 
 * <code>LoginContext::login() method.
 *
 */
public class AMLoginContext {

    static final String LIST_DELIMITER = "|";
    /**
     * AuthThreadManager associated with this AMLoginContext
     */
    public static AuthThreadManager authThread  = null;
    private String exceedRetryLimit=null;
    private static final String bundleName = "amAuth";
    
    String configName; // jaas configuration name.
    String orgDN = null;
    javax.security.auth.login.LoginContext lc = null;
    com.sun.identity.authentication.jaas.LoginContext jlc = null;
    LoginStatus st;
    LoginState loginState;
    AuthContextLocal authContext;
    Subject subject;
    AuthContext.IndexType indexType;  // index type
    String indexName;  // index name
    String clientType;
    boolean pCookieMode = false;
    String lockoutMsg = null;
    Set moduleSet = null;
    String sid = null;
    boolean accountLocked = false;
    boolean isFailed = false;
    boolean internalAuthError=false;
    boolean processDone = false;
    private int jaasCheck;
    private Thread jaasThread = null;
    private AppConfigurationEntry[] entries = null;
    Callback[] recdCallback;

    private static SsoServerAuthSvcImpl authImpl;
    static Configuration defaultConfig = null;
    private static AuthD ad;
    private static Debug debug;
    
    /**
     * Bundle to be used for localized error message. users can be differnt
     * locale. Since we create an  AMLoginContext for each user, we can cache
     * the bundle reference in the class
     */
    ResourceBundle bundle ;
    

    static {
        // set the auth configuration programmatically.
        // this getConfiguration() call throws null exception
        // when no default config is available, which looks like
        // a bug of JDK.
        try {
            defaultConfig = Configuration.getConfiguration();
        } catch (java.lang.SecurityException e) {
            //Continue
        }
        AMConfiguration ISConfig = new AMConfiguration(defaultConfig);
        try {
            Configuration.setConfiguration(ISConfig);
        } catch (java.lang.SecurityException e) {
            System.err.println("AMLoginContext:Set AM config error:"
                + e.getMessage());
        }

        if (MonitoringUtil.isRunning()) {
            authImpl = Agent.getAuthSvcMBean();
        }
    }

    /**
     * Sets the JAAS configuration to the default container's configuration.
     */
    public static void resetJAASConfig() {
        try {
            Configuration.setConfiguration(defaultConfig);
        } catch (java.lang.SecurityException e) {
            System.err.println("AMLoginContext:resetJAASConfig to default:"
                + e.getMessage());
        }
    }

    /**
     * Sets the configuration entries
     * @param entries configuration entries
     */
    public void setConfigEntries(AppConfigurationEntry[] entries) {
        this.entries = entries;
    }
    
    /**
     * Creates <code>AMLoginContext</code> object
     * @param authContext <code>AuthContextLocal</code> object
     */
    public AMLoginContext(AuthContextLocal authContext) {
        ad = AuthD.getAuth();
        debug = AuthD.debug;
        if (debug.messageEnabled()) {
            debug.message("AMLoginContext:initialThread name is... :"
            + Thread.currentThread().getName());
        }
        this.authContext = authContext;
        st = new LoginStatus();
        st.setStatus(LoginStatus.AUTH_IN_PROGRESS);
        bundle = ad.bundle; //default value for bundle until we find out
        //user login locale from LoginState object
    }
    
    /**
     * Starts login process, the map passed to this method is the parameters
     * required to start the login process. These parameters are
     * <code>indexType</code>, <code>indexName</code> , <code>principal</code>,
     * <code>subject</code>, </code>password</code>,
     * <code>organization name</code>. Based on these parameters Module
     * Configuration name is retrieved using Configuration component. Creates
     * a new LoginContext and starts login process and returns. On error
     * LoginException is thrown.
     *
     * @param loginParamsMap login parameters HashMap
     * @throws AuthLoginException if execute login fails
     */
    public void executeLogin(HashMap loginParamsMap) throws AuthLoginException {
        boolean errorState = false;
        internalAuthError=false;
        processDone = false;
        isFailed = false;
        setLoginHash();

        /* if loginState is null then there has to be some problem in the
         * init of Auth Service, throw error
         */
        if (loginState == null || loginParamsMap == null) {
            debug.error("Error: loginState or loginParams is null");
            st.setStatus(LoginStatus.AUTH_FAILED);
            loginState.setErrorCode(AMAuthErrorCode.AUTH_ERROR);
            setErrorMsgAndTemplate();
            internalAuthError=true;
            throw new AuthLoginException(bundleName,
                AMAuthErrorCode.AUTH_ERROR, null);
        } else {
            String llc = loginState.getLocale();
            java.util.Locale loc = 
                com.sun.identity.shared.locale.Locale.getLocale(llc);
            bundle = AMResourceBundleCache.getInstance().getResBundle(
                bundleName, loc);
            exceedRetryLimit = AMResourceBundleCache.getInstance().
                getResBundle("amAuthLDAP",loc).
                getString(ISAuthConstants.EXCEED_RETRY_LIMIT);
        }
        if (debug.messageEnabled()) {
            debug.message("LoginState : " + loginState);
        }
        
        // check if this is the redirection case
        String redirectUrl = (String) 
            loginParamsMap.get(AuthContextLocal.REDIRECT_URL);
        if (redirectUrl != null) {
            // Resource/IP/Env based auth case with Redirection Advice
            Callback[] redirectCallback = new Callback[1];
            redirectCallback[0] = 
                new RedirectCallback(redirectUrl, null, "GET");
            if (isPureJAAS()) {
                loginState.setReceivedCallback_NoThread(redirectCallback);
            } else {
                loginState.setReceivedCallback(redirectCallback, this);
            }
            return;
        }
        parseLoginParams(loginParamsMap);

        String moduleClassName = null;
        if (indexType == AuthContext.IndexType.MODULE_INSTANCE
                && !loginState.getEnableModuleBasedAuth() && !indexName.equals(
                ISAuthConstants.APPLICATION_MODULE)) {
            try {
                AMAuthenticationManager authManager = new AMAuthenticationManager(
                        AccessController.doPrivileged(AdminTokenAction.getInstance()), orgDN);
                AMAuthenticationInstance authInstance =
                        authManager.getAuthenticationInstance(indexName);
                moduleClassName = authInstance.getType();
            } catch (AMConfigurationException amce) {
                debug.warning("AMLoginContext.executeLogin(): Unable to get authentication config", amce);
            }
            if (moduleClassName != null && !moduleClassName.equalsIgnoreCase(
                    ISAuthConstants.FEDERATION_MODULE)) {
                debug.error("Error: Module Based Auth is not allowed");
                st.setStatus(LoginStatus.AUTH_FAILED);
                loginState.setErrorCode(
                        AMAuthErrorCode.MODULE_BASED_AUTH_NOT_ALLOWED);
                setErrorMsgAndTemplate();
                throw new AuthLoginException(bundleName,
                        AMAuthErrorCode.MODULE_BASED_AUTH_NOT_ALLOWED, null);
            }
        }

        
        if ((authContext.getOrgDN() != null) && 
            ((authContext.getOrgDN()).length() != 0)){
            this.orgDN = authContext.getOrgDN();
            loginState.setQualifiedOrgDN(this.orgDN);
        } else {
            this.orgDN = loginState.getOrgDN();
        }
        clientType = loginState.getClientType();

        if (debug.messageEnabled()) {
            debug.message("orgDN : " + orgDN);
            debug.message("clientType : " + clientType);
        }
        
        AuthContext.IndexType prevIndexType = loginState.getIndexType();
        // get the previous index type and check if it was
        // level based auth. If yes then retreive the
        // key for the localized module name and
        // set that as the indexName
        if (prevIndexType != null &&
            (prevIndexType == AuthContext.IndexType.LEVEL ||
            prevIndexType == AuthContext.IndexType.COMPOSITE_ADVICE)) {
            // this is saved for HTTP callback processing.
            loginState.setPreviousIndexType(prevIndexType);
            //if (indexType == AuthContext.IndexType.MODULE_INSTANCE) {
            //    indexName = loginState.getModuleName(indexName);
            //}
        }

        loginState.setIndexType(indexType);
        loginState.setIndexName(indexName);
        
        // do required processing for diff. indexTypes
        try {
            if (processIndexType(indexType,indexName,orgDN)) {
                return;
            }
        } catch (AuthLoginException le) {
	    if (MonitoringUtil.isRunning()) {
		if (authImpl == null) {
		    authImpl = Agent.getAuthSvcMBean();
		}
		if (authImpl != null) {
	            authImpl.incSsoServerAuthenticationFailureCount();
		}
	    }
            debug.message("Error  : " ,le);
            throw le;
        } catch (Exception e) {
	    if (MonitoringUtil.isRunning()) {
		if (authImpl == null) {
		    authImpl = Agent.getAuthSvcMBean();
		}
		if (authImpl != null) {
	            authImpl.incSsoServerAuthenticationFailureCount();
		}
	    }
            debug.message("Error : " , e);
            throw new AuthLoginException(e);
        }
        
        // call config component to retrieve configname
        // if null throw exception
        
        configName = getConfigName(indexType,indexName,orgDN,clientType);
        
        // if configName is null then error
        if (configName == null) {
            loginState.setErrorCode(AMAuthErrorCode.AUTH_CONFIG_NOT_FOUND);
            debug.message("Config not found");
            setErrorMsgAndTemplate();
            internalAuthError = true;
            st.setStatus(LoginStatus.AUTH_FAILED);
            loginState.logFailed(bundle.getString("noConfig"),"NOCONFIG");
	    if (MonitoringUtil.isRunning()) {
		if (authImpl == null) {
		    authImpl = Agent.getAuthSvcMBean();
		}
		if (authImpl != null) {
	            authImpl.incSsoServerAuthenticationFailureCount();
		}
	    }
            throw new AuthLoginException(bundleName,
            AMAuthErrorCode.AUTH_CONFIG_NOT_FOUND, null);
        }
        
        if (debug.messageEnabled()) {
            debug.message("Creating login context object\n" +
            "\n orgDN : " + orgDN +
            "\n configName : " + configName);
        }
        try {
            jaasCheck = AuthUtils.isPureJAASModulePresent(configName, this);
            
            if (isPureJAAS()) {
                debug.message("Using pure jaas mode.");
                if (authThread == null) {
                    authThread = new AuthThreadManager();
                    authThread.start();
                }
            }
            
            DSAMECallbackHandler dsameCallbackHandler =
            new DSAMECallbackHandler(this);
            
            if (isPureJAAS()) {
                if (subject != null)  {
                    lc = new javax.security.auth.login.LoginContext(configName,
                    subject,dsameCallbackHandler);
                } else {
                    lc = new javax.security.auth.login.LoginContext(configName,
                    dsameCallbackHandler);
                }
            } else {
                debug.message("Using non pure jaas mode.");
                if (subject != null)  {
                    jlc = new com.sun.identity.authentication.jaas.LoginContext(
                    entries, subject,dsameCallbackHandler);
                } else {
                    jlc = new com.sun.identity.authentication.jaas.LoginContext(
                    entries, dsameCallbackHandler);
                }
            }
        } catch (AuthLoginException ae) {
            debug.error("JAAS module for config: " + configName +
            ", " + ae.getMessage());
            if (debug.messageEnabled()) {
                debug.message("AuthLoginException", ae);
            }
            /* The user based authentication errors should not be different
             * for users who exist and who don't, which can lead to 
             * possiblity of enumerating existing users.
             * The AMAuthErrorCode.AUTH_LOGIN_FAILED error code is used for
             * all user based authentication errors.
             * Refer issue3278 
             */
            if(indexType == AuthContext.IndexType.USER &&
            AMAuthErrorCode.AUTH_CONFIG_NOT_FOUND.equals(ae.getErrorCode())){
                loginState.setErrorCode(AMAuthErrorCode.AUTH_LOGIN_FAILED);
            }else{
                loginState.setErrorCode(ae.getErrorCode());
            }
            setErrorMsgAndTemplate();
            loginState.logFailed(bundle.getString("loginContextCreateFailed"));
            internalAuthError=true;
            st.setStatus(LoginStatus.AUTH_FAILED);
	    if (MonitoringUtil.isRunning()) {
		if (authImpl == null) {
		    authImpl = Agent.getAuthSvcMBean();
		}
		if (authImpl != null) {
	            authImpl.incSsoServerAuthenticationFailureCount();
		}
	    }
            throw ae;
        } catch (LoginException le) {
            debug.error("in creating LoginContext.");
            if (debug.messageEnabled()) {
                debug.message("Exception " , le);
            }
            loginState.setErrorCode(AMAuthErrorCode.AUTH_ERROR);
            loginState.logFailed(bundle.getString("loginContextCreateFailed"));
            setErrorMsgAndTemplate();
            st.setStatus(LoginStatus.AUTH_FAILED);
            internalAuthError=true;
	    if (MonitoringUtil.isRunning()) {
		if (authImpl == null) {
		    authImpl = Agent.getAuthSvcMBean();
		}
		if (authImpl != null) {
	            authImpl.incSsoServerAuthenticationFailureCount();
		}
	    }
            throw new AuthLoginException(bundleName,
            AMAuthErrorCode.AUTH_ERROR, null, le);
        } catch (SecurityException se) {
            debug.error("security in creating LoginContext.");
            if (debug.messageEnabled()) {
                debug.message("Exception " , se);
            }
            loginState.setErrorCode(AMAuthErrorCode.AUTH_ERROR);
            setErrorMsgAndTemplate();
            loginState.logFailed(bundle.getString("loginContextCreateFailed"));
            internalAuthError=true;
            st.setStatus(LoginStatus.AUTH_FAILED);
	    if (MonitoringUtil.isRunning()) {
		if (authImpl == null) {
		    authImpl = Agent.getAuthSvcMBean();
		}
		if (authImpl != null) {
	            authImpl.incSsoServerAuthenticationFailureCount();
		}
	    }
            throw new AuthLoginException(bundleName,
            AMAuthErrorCode.AUTH_ERROR, null);
        } catch (Exception e) {
            debug.error("Creating DSAMECallbackHandler: " + e.getMessage());
            loginState.setErrorCode(AMAuthErrorCode.AUTH_ERROR);
            setErrorMsgAndTemplate();
            loginState.logFailed(bundle.getString("loginContextCreateFailed"));
            internalAuthError=true;
	    if (MonitoringUtil.isRunning()) {
		if (authImpl == null) {
		    authImpl = Agent.getAuthSvcMBean();
		}
		if (authImpl != null) {
	            authImpl.incSsoServerAuthenticationFailureCount();
		}
	    }
            st.setStatus(LoginStatus.AUTH_FAILED);
            throw new AuthLoginException(bundleName, AMAuthErrorCode.AUTH_ERROR,
            null, e);
        }
        try {
            if (isPureJAAS()) {
                if (jaasThread != null) {
                    jaasThread.interrupt();
                    jaasThread = null;
                    errorState = true;
                } else {
                    jaasThread = new JAASLoginThread(this);
                    jaasThread.start();
                }
            } else {
                runLogin();
            }
        } catch (IllegalThreadStateException ite) {
            errorState = true;
        } catch (Exception e) {
            errorState = true;
        }
        
        if (errorState) {
            st.setStatus(LoginStatus.AUTH_RESET);
            loginState.setErrorCode(AMAuthErrorCode.AUTH_ERROR);
            setErrorMsgAndTemplate();
            internalAuthError=true;
	    if (MonitoringUtil.isRunning()) {
		if (authImpl == null) {
		    authImpl = Agent.getAuthSvcMBean();
		}
		if (authImpl != null) {
	            authImpl.incSsoServerAuthenticationFailureCount();
		}
	    }
            throw new AuthLoginException(bundleName,
            AMAuthErrorCode.AUTH_ERROR, null);
            
        }
        debug.message("AMLoginContext:Thread started... returning.");
    }
    
    /**
     * Starts the login process ,calls JAAS Login Context
     */
    public void runLogin() {
        Thread thread = Thread.currentThread();
        String logFailedMessage =  bundle.getString("loginFailed");
        String logFailedError   =  null;
        AMAccountLockout amAccountLockout = null;
        boolean loginSuccess=false;
        try {
            if (isPureJAAS()) {
                lc.login();
                subject = lc.getSubject();
            } else {
                jlc.login();
                subject = jlc.getSubject();
            }

            loginState.setSubject(subject);

            if (!loginState.isAuthValidForInternalUser()) {
                if (debug.warningEnabled()) {
                    debug.warning(
                        "AMLoginContext.runLogin():auth failed, "
                        +  "using invalid realm name for internal user");
                }
                logFailedMessage = AuthUtils.getErrorVal(
                        AMAuthErrorCode.AUTH_MODULE_DENIED, 
                        AuthUtils.ERROR_MESSAGE);
                logFailedError = "AUTH_MODULE_DENIED";
                throw new AuthException(
                        AMAuthErrorCode.AUTH_MODULE_DENIED, null);
            }

            debug.message("user authentication successful");
            
            // retrieve authenticated user's profile or create
            // a user profile if dynamic profile creation is
            // is true
            
            debug.message("searchUserProfile for Subject :");
            boolean profileState = loginState.searchUserProfile(subject,
            indexType,indexName);
            loginSuccess = true;
            if (!profileState) {
                debug.error("Profile not found ");
                logFailedMessage = bundle.getString("noUserProfile");
                logFailedError = "NOUSERPROFILE";
                loginState.setErrorCode(AMAuthErrorCode.AUTH_PROFILE_ERROR);
                isFailed = true;
            } else {
                //update loginstate with authlevel , moduleName , role etc.
                amAccountLockout = new AMAccountLockout(loginState);
                if (amAccountLockout.isLockedOut()) {
                    debug.message("User locked out!!");
                    logFailedMessage = bundle.getString("lockOut");
                    logFailedError = "LOCKEDOUT";
                    loginState.setErrorCode(AMAuthErrorCode.AUTH_USER_LOCKED);
                    isFailed = true;
                } else {
                    boolean accountExpired = false;
                    if (!loginState.ignoreProfile()) {
                        accountExpired = amAccountLockout.isAccountExpired();
                    }
                    if (accountExpired) {
                        debug.message("Account expired!!");
                        logFailedMessage = bundle.getString("accountExpired");
                        logFailedError = "ACCOUNTEXPIRED";
                        loginState.setErrorCode(
                            AMAuthErrorCode.AUTH_ACCOUNT_EXPIRED);
                        isFailed = true;
                    } else {
                        // came here successful auth.
                        if (debug.messageEnabled()) {
                            debug.message("authContext is : " + authContext);
                            debug.message("loginSTate is : " + loginState);
                        }
                        
                        updateLoginState(
                            loginState,indexType,indexName,configName,orgDN);
                        //activate session
                        Object lcInSession = null;
                        if (isPureJAAS()) {
                            lcInSession = lc;
                        } else {
                            lcInSession = jlc;
                        } 
                        boolean sessionActivated = 
                            loginState.activateSession(subject, authContext,
                            lcInSession);
                        if (sessionActivated) {
                            loginState.logSuccess();
                            if (amAccountLockout.isLockoutEnabled()) {
                                amAccountLockout.resetPasswdLockout(
                                    loginState.getUserDN(), true);
                            }
                            st.setStatus(LoginStatus.AUTH_SUCCESS);
                            loginState.updateSessionForFailover();
                            debug.message("login success");
                        } else {
                            logFailedMessage = AuthUtils.getErrorVal(AMAuthErrorCode.
                            AUTH_MAX_SESSION_REACHED,AuthUtils.ERROR_MESSAGE);
                            logFailedError = "MAXSESSIONREACHED";
                            throw new AuthException(
                            AMAuthErrorCode.AUTH_MAX_SESSION_REACHED, null);
                        }
                    }
                }
            }
        } catch (InvalidPasswordException ipe) {
            debug.message("Invalid Password : ");
            if (debug.messageEnabled()) {
                debug.message("Exception " , ipe);
            }
            
            String failedUserId = ipe.getTokenId();
            
            if (debug.messageEnabled()) {
                debug.message("Invalid Password Exception " + failedUserId);
            }
            
            if (failedUserId != null) {
                amAccountLockout = new AMAccountLockout(loginState);
                accountLocked = amAccountLockout.isLockedOut(failedUserId);
                if ((!accountLocked) && (amAccountLockout.isLockoutEnabled())) {
                    amAccountLockout.invalidPasswd(failedUserId);
                    checkWarningCount(amAccountLockout);
                    accountLocked =
                    amAccountLockout.isAccountLocked(failedUserId);
                }
            }
            
            logFailedMessage = bundle.getString("invalidPasswd");
            logFailedError = "INVALIDPASSWORD";
            if (accountLocked) {
                loginState.setErrorCode(AMAuthErrorCode.AUTH_USER_INACTIVE);
                if (failedUserId != null) {
                    loginState.logFailed(failedUserId, "LOCKEDOUT");
                } else {
                    loginState.logFailed("LOCKEDOUT");
                }
            } else {
                loginState.setErrorCode(AMAuthErrorCode.AUTH_INVALID_PASSWORD);
            }
            isFailed = true;
            authContext.setLoginException(ipe);
        } catch (MessageLoginException me) {
            if (debug.messageEnabled()) {
                debug.message("LOGINFAILED MessageAuthLoginException....");
                debug.message("Exception " , me);
            }
            
            java.util.Locale locale = 
                com.sun.identity.shared.locale.Locale.getLocale(
                    loginState.getLocale());
            loginState.setModuleErrorMessage(me.getL10NMessage(locale));
            isFailed = true;
            authContext.setLoginException(me);
        } catch (AuthLoginException le) {
            loginState.setErrorCode(AMAuthErrorCode.AUTH_LOGIN_FAILED);
            if (AMAuthErrorCode.AUTH_MODULE_DENIED.equals(le.getMessage())) {
                if (debug.warningEnabled()) {
                    debug.warning(
                        "AMLoginContext.runLogin():auth failed, "
                        +  "using invalid auth module name for internal user");
                }
                logFailedMessage = AuthUtils.getErrorVal(
                        AMAuthErrorCode.AUTH_MODULE_DENIED, 
                        AuthUtils.ERROR_MESSAGE);
                logFailedError = "AUTH_MODULE_DENIED";
                loginState.setErrorCode(AMAuthErrorCode.AUTH_MODULE_DENIED);
            } else if ( AMAuthErrorCode.AUTH_TIMEOUT.equals(le.getMessage())) {
                debug.message("LOGINFAILED Error Timed Out....");
            } else if (ISAuthConstants.EXCEED_RETRY_LIMIT.
                    equals(le.getErrorCode())) {
                ad.debug.message("LOGINFAILED ExceedRetryLimit");
            } else {
                debug.message("LOGINFAILED Error....");
            }
            if (debug.messageEnabled()) {
                debug.message("Exception : " , le);
            }
            isFailed = true;
            if (loginState.isTimedOut()) {
                logFailedMessage = bundle.getString("loginTimeout");
                logFailedError = "LOGINTIMEOUT";
                loginState.setErrorCode(AMAuthErrorCode.AUTH_TIMEOUT);
            } else if (ISAuthConstants.EXCEED_RETRY_LIMIT.
                    equals(le.getErrorCode())) {
                loginState.setErrorMessage(exceedRetryLimit);
                loginState.setErrorCode(
                        AMAuthErrorCode.AUTH_USER_LOCKED_IN_DS);
            } else if (ISAuthConstants.SERVER_UNWILLING.equals(le.getErrorCode())) {
                loginState.setErrorCode(
                        AMAuthErrorCode.AUTH_ERROR);
            }
            authContext.setLoginException(le);
        } catch (AuthException e) {
            if (debug.messageEnabled()) {
                debug.message("Exception : " + e.getMessage());
            }
            isFailed = true;
            loginState.setErrorCode(e.getErrorCode());
            loginState.logFailed(bundle.getString("loginFailed"));
            logFailedError = null;
            authContext.setLoginException(new AuthLoginException(
            bundleName, "loginFailed", null, e));
        } catch (Exception e) {
            debug.message("Error during login.. ");
            if (debug.messageEnabled()) {
                debug.message("Exception " , e);
            }
            isFailed =true;
            loginState.setErrorCode(AMAuthErrorCode.AUTH_ERROR);
            loginState.logFailed(bundle.getString("loginFailed"));
            logFailedError = null;
            authContext.setLoginException(new AuthLoginException(
            bundleName, "loginFailed", null, e));
        } catch (java.lang.Error er) {
            debug.message(
                "Caught java.lang.Error returned from DSAMEHandler", er);
            return;
        }
        debug.message("Came to before if Failed loop");
        if (isFailed) {
	    if (MonitoringUtil.isRunning()) {
		if (authImpl == null) {
		    authImpl = Agent.getAuthSvcMBean();
		}
		if (authImpl != null) {
	            authImpl.incSsoServerAuthenticationFailureCount();
		}
	    }
            if (loginSuccess) {
                // this is the case where authentication to modules
                // succeeded but framework failed to validate the
                // user, in this case populate with all module user
                // successfully authenticated as.
                loginState.setFailureModuleList(getSuccessModuleString(orgDN));
                
            } else {
                loginState.setFailureModuleList(getFailureModuleList(orgDN));
            }
            loginState.logFailed(logFailedMessage,logFailedError);
            setErrorMsgAndTemplate();
            st.setStatus(LoginStatus.AUTH_FAILED);
            if (indexType == AuthContext.IndexType.USER) {
                if (debug.messageEnabled()) {
                    debug.message("Set failureId in user based auth "
                    + indexName);
                }
                loginState.setFailedUserId(indexName);
            }
        } else {
	    if (debug.messageEnabled()) {
		debug.message("AMLoginContext.runLogin:" +
		    "calling incSsoServerAuthenticationSuccessCount");
	    }
	    if (MonitoringUtil.isRunning()) {
		if (authImpl == null) {
		    authImpl = Agent.getAuthSvcMBean();
		}
		if (authImpl != null) {
	            authImpl.incSsoServerAuthenticationSuccessCount();
		}
	    }
	}
        
        if (debug.messageEnabled()) {
            debug.message("finished...login notify all threads\n"+
            "AMLoginContext:LoginStatus: " + st.getStatus());
        }
        if (isPureJAAS()) {
            authThread.removeFromHash(thread,"timeoutHash");
            
            // notify possible waiting thread
            loginState.setReceivedCallback(null, this);
        }
        
        isFailed=false;
        nullifyUsedVars();
        return;
    }
    
    /**
     * Logs out.
     *
     * @throws AuthLoginException when fails to logout
     */
    public void logout() throws AuthLoginException {
        debug.message("in logout:");
        try {
            if (isPureJAAS()) {
                if (lc != null) {
                    lc.logout();
                }
            } else {
                if (jlc != null) {
                    jlc.logout();
                }
            }
            loginState.logLogout();
            loginState.postProcess(indexType,indexName,
                    LoginState.POSTPROCESS_LOGOUT);           
            destroySession();
            st.setStatus(LoginStatus.AUTH_COMPLETED);
        } catch (AuthLoginException le) {
            debug.message("Error during logout : ");
            if (debug.messageEnabled()) {
                debug.message("Exception " , le);
            }
            //logout - ignore this error since logout will be done
            throw new AuthLoginException(bundleName, "failedLogout", null, le);
        } catch (Exception e) {
            debug.message("Error during logout : ");
            if (debug.messageEnabled()) {
                debug.message("Exception " , e);
            }
        }
    }
    
    /* destroy Session on a logout OR abort */
    void destroySession() {
        if (debug.messageEnabled()) {
            debug.message("AMLoginContext:destroySession: " + loginState);
        }
        loginState.destroySession();
        return;
    }
    
    /**
     * Returns array of recieved callbacks from module.
     *
     * @return array of recieved callbacks from module.
     */
    public Callback[] getRequiredInfo() {
        if (st.getStatus() != LoginStatus.AUTH_IN_PROGRESS) {
            return null;
        }
        if (indexType == AuthContext.IndexType.LEVEL ||
            indexType == AuthContext.IndexType.COMPOSITE_ADVICE) {
            debug.message(
                "IndexType level/composite_advice, send choice callback");
            // reset indexType since UI will start module based auth
            indexType = null;
        } else {
            if (isPureJAAS()) {
                recdCallback = getRequiredInfoCallback();
            } else {
                recdCallback = getRequiredInfoCallback_NoThread();
            }
        }
        
        if (recdCallback != null ) {
            for (int i = 0; i < recdCallback.length; i++) {
                if (debug.messageEnabled()) {
                    debug.message("Recd Callback in amlc.getRequiredInfo : "
                    + recdCallback[i]);
                }
            }
        } else {
            debug.message("Recd Callback in amlc.getRequiredInfo is NULL");
        }
        
        return recdCallback;
    }
    
    /**
     * Returns array of  required callback information non-JAAS thread mode
     * @return callbacks required <code>Callbacks</code> array to be submitted
     */
    public Callback[] getRequiredInfoCallback_NoThread() {
        return loginState.getReceivedInfo();
    }
    
    
    /**
     * Returns the array of required Callbacks from <code>CallbackHandler</code>
     * waits till <code>loginState::getReceivedInfo()</code> OR
     * authentication status is not <code>AUTH_IN_PROGRESS</code> OR
     * if thread recieves a notify .
     *
     * @return array of Required Callbacks from <code>CallbackHandler</code>.
     */
    public synchronized Callback[] getRequiredInfoCallback() {
        if (debug.messageEnabled()) {
            debug.message("getRequiredInfo.. " + st.getStatus());
        }
        if (  isFailed ||  (st.getStatus() != LoginStatus.AUTH_IN_PROGRESS) ){
            debug.message("no more requirements returning null");
            return null;
        }
        Thread thread = Thread.currentThread();
        long lastCallbackSent = loginState.getLastCallbackSent();
        long pageTimeOut = loginState.getPageTimeOut();
        if (debug.messageEnabled()) {
            debug.message("getRequiredInfo. ThreadName is.. :" + thread);
            debug.message("lastCallbackSent : " + lastCallbackSent);
            debug.message("pageTimeOut : " + pageTimeOut );
        }
        authThread.setHash(thread,pageTimeOut,lastCallbackSent);

        while ((!isFailed) && (loginState.getReceivedInfo() == null) &&
            (st.getStatus() == LoginStatus.AUTH_IN_PROGRESS)
        ) {
            try {
                if (debug.messageEnabled()) {
                    debug.message(
                        Thread.currentThread() + "Waiting.." + st.getStatus());
                }
                if (st.getStatus() != LoginStatus.AUTH_IN_PROGRESS) {
                    return null;
                }
                if ((!isFailed) &&
                    (st.getStatus() == LoginStatus.AUTH_IN_PROGRESS) &&
                    (loginState.getReceivedInfo() == null)
                ) {
                    this.wait();
                }
            } catch (InterruptedException e) {
                debug.message("getRecdinfo INTERRUPTED");
                break;
            }
        }
        if (debug.messageEnabled()) {
            debug.message(
                "Thread woke up... "+ loginState.getReceivedInfo());
        }
        Callback[] getRequiredInfo = loginState.getReceivedInfo();
        if (debug.messageEnabled()) {
            debug.message(
                "Returning getRequiredInfo... :" + getRequiredInfo);
        }
        authThread.removeFromHash(thread,"timeoutHash");
        return getRequiredInfo;
    }
    
    /**
     * Sets the submitted requirements, called by
     * <code>AuthContext.submitRequirements</code>
     * <code>loginState.setSubmittedCallback</code> is update.
     *
     * @param callback submit the required <code>Callbacks</code>
     */
    public void submitRequiredInfo(Callback callback[]) {
        if (debug.messageEnabled() && callback != null && callback.length > 0) {
            debug.message("submit required info... :" + callback[0]);
        }
        if (isPureJAAS()) {
            loginState.setSubmittedCallback(callback,this);
        } else {
            loginState.setSubmittedCallback_NoThread(callback);
        }
        if (debug.messageEnabled()) {
            debug.message("Retunring from submitRequiredInfo");
        }
    }
    
    
    /**
     * <code>CallbackHandler</code> calls this to retrieve the submitted
     * credentials/callbacks waits till
     * <code>loginState.setSubmittedCallback</code> is set OR
     * <code>LoginStatus</code> is not <code>AUTH_IN_PROGRESS</code>.
     *
     * @return submitted credentials/callbacks.
     */
    public synchronized Callback[] submitCallbackInfo() {
        if (debug.messageEnabled()) {
            debug.message("submitRequiredInfo. ThreadName is.. :" +
            Thread.currentThread().getName());
        }
        
        if ((st.getStatus() != LoginStatus.AUTH_IN_PROGRESS) || (isFailed)) {
            debug.message("submitReq no more requirements returning null");
            return null;
        }
        
        Thread thread = Thread.currentThread();
        long lastCallbackSent = loginState.getLastCallbackSent();
        long pageTimeOut = loginState.getPageTimeOut();
        if (debug.messageEnabled()) {
            debug.message("submitRequiredInfo. ThreadName is.. :" +
            thread);
            debug.message("lastCallbackSent : " + lastCallbackSent);
            debug.message("pageTimeOut : " + pageTimeOut );
        }
        authThread.setHash(thread,pageTimeOut,lastCallbackSent);
        while ((loginState.getSubmittedInfo() == null)
        && (st.getStatus() == LoginStatus.AUTH_IN_PROGRESS) ) {
            try {
                if (debug.messageEnabled()) {
                    debug.message(Thread.currentThread() +
                    " Waiting...." + st.getStatus());
                }
                if (st.getStatus() != LoginStatus.AUTH_IN_PROGRESS) {
                    return null;
                }
                if ((loginState.getSubmittedInfo() == null)) {
                    wait();
                }
            } catch (InterruptedException e) {
                debug.message("submitRequired info INTERRUPTED");
                break;
            }
        }
        debug.message("Threadwaking up go submit info..."  );
        authThread.removeFromHash(thread,"timeoutHash");
        Callback[] setSubmittedInfo = loginState.getSubmittedInfo();
        debug.message("Returning submitted info: ");
        return setSubmittedInfo;
    }
    
    /**
     * Returns the authentication status.
     *
     * @return the authentication status.
     */
    public int getStatus() {
        int status  = st.getStatus();

        if (isFailed || status == LoginStatus.AUTH_FAILED) {
            postProcessOnFail();
        } else if (status == LoginStatus.AUTH_SUCCESS) {
            postProcessOnSuccess();
        }
        if (debug.messageEnabled()) {
            debug.message("getStatus : status is... : " + status);
        }
        return status;
        
    }
    
    /**
     * Returns login state for the authentication context.
     *
     * @return login state for the authentication context.
     */
    public LoginState getLoginState() {
        return AuthUtils.getLoginState(authContext);
    }
    
    
    /**
     * Terminates an ongoing login process.
     *
     * @throws AuthLoginException when fails to abort
     */
    public void abort() throws AuthLoginException {
        
        debug.message("in abort");
        try {
            logout();
        } catch (Exception ae) {
            if (debug.messageEnabled()) {
                debug.message("Error logging out.. :");
                debug.message("Exception " , ae);
            }
            try {
                destroySession();
                st.setStatus(LoginStatus.AUTH_COMPLETED);
            } catch (Exception e) {
                debug.message("Error aborting");
                if (debug.messageEnabled()) {
                    debug.message("Exception " , e);
                }
                
                // abort this error - since abort will be done
                throw new AuthLoginException(bundleName, "abortFailed", null);
            }
        }
    }
    
    
    /**
     * Returns authentication modules configured for a given organization.
     *
     * @return authentication modules configured for a given organization.
    */
    public Set getModuleInstanceNames() {
        try {
            LoginState loginState =
            (LoginState) AuthUtils.getLoginState(authContext);
            
            if (loginState != null) {
                moduleSet = loginState.getModuleInstances();
            }
            
            if (debug.messageEnabled()) {
                debug.message("moduleSet is : "+ moduleSet);
            }
        } catch (Exception e) {
            debug.message("Error : " , e);
        }
        
        return moduleSet;
    }
    
    /**
     * Returns organization/suborganization for a request.
     *
     * @return organization/suborganization for a request.
     */
    public String getOrganizationName() {
        return loginState.getQueryOrg();
    }
    
    /**
     * Returns Single Sign On Token for authenticated user, returns null if
     * session is inactive.
     *
     * @return Single Sign On Token for authenticated user.
     */
    public SSOToken getSSOToken() {
        try {
            return loginState.getSSOToken();
        } catch (SSOException e) {
            if (debug.messageEnabled()) {
                debug.message("error getting ssoToken : " );
                debug.message("Exception " , e);
            }
            return null;
        }
    }
    
    /**
     * Returns Login Success URL for authenticated user.
     *
     * @return Login Success URL for authenticated user.
     */
    public String getSuccessURL() {
        try {
            return loginState.getSuccessLoginURL();
        } catch (Exception e) {
            if (debug.messageEnabled()) {
                debug.message("error getting successURL : " + e.toString() );
            }
            return null;
        }
    }
    
    /**
     * Returns Login Failure URL for authenticated user.
     *
     * @return Login Failure URL for authenticated user.
     */
    public String getFailureURL() {
        try {
            return loginState.getFailureLoginURL();
        } catch (Exception e) {
            if (debug.messageEnabled()) {
                debug.message("error getting failureURL : " + e.toString());
            }
            return null;
        }
    }
    
    /**
     * Returns the current <code>authIdentifier</code> of the authentication
     * process as String Session ID.
     *
     * @return <code>authIdentifier</code> of the authentication process.
     */
    public String getAuthIdentifier() {
        String sidString = null;
        try {
            sidString = loginState.getSid().toString();
        } catch (Exception e) {
            if (debug.messageEnabled()) {
                debug.message("Error retreiving sid from LoginState : " +
                e.getMessage());
            }
        }
        return sidString;
    }
    
    /**
     * Returns the subject of authenticated user.
     *
     * @return the subject of authenticated user.
     */
    public Subject getSubject() {
        
        try {
            return loginState.getSubject();
        } catch (Exception e) {
            if (debug.messageEnabled()) {
                debug.message("error getting Subject :");
                debug.message("Exception " , e);
            }
            
            return null;
        }
    }
    
    /* retreive login parameters */
    private void parseLoginParams(HashMap loginParamsMap) {
        
        if (debug.messageEnabled()) {
            debug.message("loginParamsMap is.. :" + loginParamsMap);
        }
        
        try {
            indexType = (AuthContext.IndexType) loginParamsMap.get("indexType");
            indexName = (String) loginParamsMap.get("indexName");
            if (debug.messageEnabled()){
                debug.message("indexType = " + indexType +
                "\nindexName = " + indexName);
            }
            //principal = (Principal) loginParamsMap.get("principal");
            //password = (char[]) loginParamsMap.get("password");
            subject = (Subject)loginParamsMap.get("subject");
            Boolean pCookieObject = (Boolean) loginParamsMap.get("pCookieMode");
            if (pCookieObject != null) {
                pCookieMode = pCookieObject.booleanValue();
            }

            String locale = (String)loginParamsMap.get("locale");  
            if (locale != null && locale.length() > 0) {
                loginState.setLocale(locale);
            }
        } catch (Exception e) {
            if (debug.messageEnabled()) {
                debug.message("Error parsing login Params");
                debug.message("Exception " , e);
            }
        }
    }
    
    /* retrieve config name from config component based on the
     * indexType , indexName , orgDN and clientType
     * if indexType , indexName are null then indexType is assumed
     * to be org
     */
    String getConfigName(
        AuthContext.IndexType indexType,
        String indexName,
        String orgDN,
        String clientType) {
        String configName = null;
        String universalID = null;
        
        // if index type is null assume org based authentication
        if (indexType == null) {
            configName = AMAuthConfigUtils.getAuthConfigName(orgDN, "html");
        } else {
            if (indexType == AuthContext.IndexType.USER) {
                universalID = loginState.getUserUniversalId(indexName);
            } else if (indexType == AuthContext.IndexType.ROLE) {
                universalID = loginState.getRoleUniversalId(indexName);
            } else {
                // means the index type is not ROLE or USER
                // for SERVICE , MODULE pass the indexName as is
                universalID = indexName;
            }
            try {
                if (universalID != null ) {
                    configName = AMAuthConfigUtils.getAuthConfigName(indexType,
                    universalID,orgDN,clientType);
                }
            } catch (Exception e) {
                if (debug.messageEnabled()) {
                    debug.message("Error retrieving configName ");
                    debug.message("Exception : " , e);
                }
            }
        }
        return configName;
    }
    
    /* for indexType level retreive the module names .
     * if the more then 1 modules has the same level
     * then generate choice callback , else if module
     * is 1 then start module based authentication.
     * throws Exception if no modules are found
     */
    boolean processLevel(
        AuthContext.IndexType indexType,
        String indexName,
        String orgDN,
        String clientType
    ) throws AuthException {
        indexType= AuthContext.IndexType.LEVEL;
        
        java.util.Locale loc = com.sun.identity.shared.locale.Locale.getLocale(
            loginState.getLocale());
        AuthLevel authLevel = new AuthLevel(indexType,indexName,orgDN,
        clientType,loc);
        int numberOfModules = authLevel.getNumberOfAuthModules();
        if (debug.messageEnabled()) {
            debug.message("number of Modules : " + numberOfModules);
        }
        
        if (numberOfModules <= 0) {
            loginState.logFailed(bundle.getString("noConfig"),"NOCONFIG");
            throw new AuthException(
                AMAuthErrorCode.AUTH_CONFIG_NOT_FOUND, null);
        } else if (numberOfModules == 1) {
            this.indexType = AuthContext.IndexType.MODULE_INSTANCE;
            loginState.setIndexType(this.indexType);
            this.indexName = authLevel.getModuleName();
            return false;
        } else {
            try {
                recdCallback = authLevel.createChoiceCallback();
                loginState.setPrevCallback(recdCallback);
                loginState.setModuleMap(authLevel.getModuleMap());
                return true;
            } catch (AuthException ae) {
                if (debug.messageEnabled()) {
                    debug.message("Error creating choiceCallback");
                    debug.message("Exception " , ae);
                }
                return false;
            }
        }
    }
    
    /* for indexType composite_advice retreive the module names .
     * if there is more then one modules required in composite advice
     * then generate choice callback , else if module
     * is 1 then start module based authentication.
     * throws Exception if no modules are found
     */
    boolean processCompositeAdvice(
        AuthContext.IndexType indexType,
        String indexName,
        String orgDN,
        String clientType
    ) throws AuthException {
        String moduleName = null;
        java.util.Locale loc = com.sun.identity.shared.locale.Locale.getLocale(
        loginState.getLocale());
        CompositeAdvices compositeAdvice = new CompositeAdvices(indexName,orgDN,
        clientType,loc);
        int numberOfModules = compositeAdvice.getNumberOfAuthModules();
        if (debug.messageEnabled()) {
            debug.message("processCompositeAdvice:number of Modules/Services : " 
                + numberOfModules);
        }
        loginState.setCompositeAdviceType(compositeAdvice.getType());
        
        if (numberOfModules <= 0) {
            loginState.logFailed(bundle.getString("noConfig"));
            throw new AuthException(AMAuthErrorCode.AUTH_CONFIG_NOT_FOUND, null);
        } else if (numberOfModules == 1) {
            this.indexName = AMAuthUtils.getDataFromRealmQualifiedData(
                compositeAdvice.getModuleName());
            String qualifiedRealm = 
                AMAuthUtils.getRealmFromRealmQualifiedData(
                    compositeAdvice.getModuleName());            
            if ((qualifiedRealm != null) && (qualifiedRealm.length() != 0)) {
                this.orgDN = DNMapper.orgNameToDN(qualifiedRealm);
                loginState.setQualifiedOrgDN(this.orgDN);
            }
            if (compositeAdvice.getType() == AuthUtils.MODULE) {
                this.indexType = AuthContext.IndexType.MODULE_INSTANCE;
            } else if (compositeAdvice.getType() == AuthUtils.SERVICE) {
                this.indexType = AuthContext.IndexType.SERVICE;
            } else if (compositeAdvice.getType() == AuthUtils.REALM) {                
                this.orgDN = DNMapper.orgNameToDN(compositeAdvice.getModuleName());
                loginState.setQualifiedOrgDN(this.orgDN);
                this.indexName = AuthUtils.getOrgConfiguredAuthenticationChain(this.orgDN);
                this.indexType = AuthContext.IndexType.SERVICE;
            }
            loginState.setIndexType(this.indexType);
            loginState.setIndexName(this.indexName);
            if (debug.messageEnabled()) {
                debug.message("processCompositeAdvice:indexType : " 
                    + this.indexType);
                debug.message("processCompositeAdvice:indexName : " 
                    + this.indexName);
            }
            return false;
        } else {
            try {
                recdCallback = compositeAdvice.createChoiceCallback();
                loginState.setPrevCallback(recdCallback);
                loginState.setModuleMap(compositeAdvice.getModuleMap());
                return true;
            } catch (AuthException ae) {
                if (debug.messageEnabled()) {
                    debug.message("Error creating choiceCallback");
                    debug.message("Exception " , ae);
                }
                return false;
            }
        }
    }
    
    /* update login state with indexType,indexName */
    void updateLoginState(
        LoginState loginState,
        AuthContext.IndexType indexType,
        String indexName,
        String configName,
        String orgDN) {
        // set authLevel in LoginState
        
        String authLevel;
        if (indexType == AuthContext.IndexType.LEVEL) {
            authLevel = indexName;
        } else {
            // retreive from config component check with Qingwen
            // config component will return the max level in case
            // of multiple authentication.
            //authLevel=AMAuthConfigUtils.getAuthLevel(configName);
            authLevel = getAuthLevel(orgDN);
        }
        
        loginState.setAuthLevel(authLevel);
        
        // set the module name
        String moduleName=null;
        
        if (indexType == AuthContext.IndexType.MODULE_INSTANCE) {
            moduleName = indexName;
        } else {
            moduleName = getSuccessModuleString(orgDN);
        }
        
        if (debug.messageEnabled()) {
            debug.message("moduleName : " + moduleName);
        }
        
        loginState.setAuthModuleName(moduleName);
        
        // set username
        
        if ((indexType == AuthContext.IndexType.USER) && 
            (pCookieMode)) {
            loginState.setToken(indexName);
        }
    }
    
    /* check if user exists and is enabled if not return
     * false - login process should not continue
     */
    boolean validateUser(String userName) {
        try {
            boolean userProfileExists = loginState.getUserProfile(
                userName,true);
            return ((userProfileExists) && (loginState.isUserEnabled()));
        } catch (Exception e) {
            if (debug.messageEnabled()) {
                debug.message("Error retrieving profile for : " + userName);
            }
            return false;
        }
    }
    
    /**
     * Checks the warning count to determine the lockout message
     * to be displayed to the user.
     *
     * @param amAccountLockout the account lockout object.
     */
    void checkWarningCount(AMAccountLockout amAccountLockout) {
        try {
            int warningCount = amAccountLockout.getWarnUserCount();
            if (warningCount == 0) {
                lockoutMsg = ISAuthConstants.EMPTY_STRING;
            } else {
                if (warningCount < 0) {
                    accountLocked=true;
                } else  {
                    String lockoutMsgFmt = bundle.getString("lockOutWarning");
                    Object [] params = new Object[1];
                    params[0] = new Integer(warningCount);
                    lockoutMsg = MessageFormat.format(lockoutMsgFmt, params);
                    loginState.setLockoutMsg(lockoutMsg);
                    accountLocked=false;
                }
            }
            
            if (debug.messageEnabled()) {
                debug.message("WARNING COUNT : " + warningCount);
                debug.message("WARNING COUNT MESSAGE: " + lockoutMsg);
            }
        } catch (Exception e) {
            debug.message("Error : " ,e);
        }
    }
    
    /**
     * Sets the error message and template
     */
    void setErrorMsgAndTemplate() {
        if (loginState == null) {
            return;
        }
        String errorCode = loginState.getErrorCode();
        
        if (errorCode != null) {
            String resProperty = bundle.getString(errorCode);
            String errorMsg =  null;
            String templateName =  null;
            
            if (debug.messageEnabled()) {
                debug.message("resProperty is.. :" + resProperty);
            }
            errorMsg = AuthUtils.getErrorVal(errorCode,AuthUtils.ERROR_MESSAGE);
            templateName = AuthUtils.getErrorVal(errorCode,AuthUtils.ERROR_TEMPLATE);
            
            if (debug.messageEnabled()) {
                debug.message("Error Message : " + errorMsg);
                debug.message("Error Template: " + templateName);
            }
            
            loginState.setErrorMessage(errorMsg);
            loginState.setErrorTemplate(templateName);
        }
    }
    
    /* for error handling - methods to return error code , module error
     * template , framework error template , error message
     */
    String getTimedOutTemplate() {
        loginState.setErrorCode(AMAuthErrorCode.AUTH_TIMEOUT);
        loginState.logFailed(bundle.getString("loginTimeout"),"LOGINTIMEOUT");
        loginState.setErrorMessage(
            AuthUtils.getErrorVal(
                AMAuthErrorCode.AUTH_TIMEOUT,AuthUtils.ERROR_MESSAGE));
        return AuthUtils.getErrorVal(
            AMAuthErrorCode.AUTH_TIMEOUT,AuthUtils.ERROR_TEMPLATE);
    }
    
    String getModuleErrorTemplate() {
        
        String moduleErrorTemplate = loginState.getModuleErrorTemplate();
        if (debug.messageEnabled()) {
            debug.message("Error Template is : " + moduleErrorTemplate);
        }
        return moduleErrorTemplate;
    }
    
    /**
     * Returns error template.
     *
     * @return error template.
     */
    public String getErrorTemplate() {
        
        String errorTemplate = null;
        if (loginState == null) {
            errorTemplate = AuthUtils.getErrorVal(AMAuthErrorCode.AUTH_ERROR,
            AuthUtils.ERROR_TEMPLATE);
            return errorTemplate;
        }
        if (loginState.isTimedOut()) {
            errorTemplate = getTimedOutTemplate();
        } else {
            errorTemplate = loginState.getModuleErrorTemplate();
            if ((errorTemplate == null) ||
                (errorTemplate.equals(ISAuthConstants.EMPTY_STRING))
            ) {
                errorTemplate = loginState.getErrorTemplate();
            }
        }
        if (debug.messageEnabled()) {
            debug.message("Error Template is : " + errorTemplate);
        }
        loginState.setErrorTemplate(errorTemplate);
        return errorTemplate;
    }
    
    /**
     * Returns error message.
     *
     * @return error message.
     */
    public String getErrorMessage() {
        String errorMsg = null;
        
        if (loginState == null) {
            errorMsg = AuthUtils.getErrorVal(AMAuthErrorCode.AUTH_ERROR,
            AuthUtils.ERROR_MESSAGE);
            return errorMsg;
        }
        
        errorMsg = loginState.getModuleErrorMessage();
        if (errorMsg == null) {
            errorMsg = loginState.getErrorMessage();
        }
        
        if (debug.messageEnabled()) {
            debug.message("Error message is : " + errorMsg);
        }
        
        return errorMsg;
    }
    
    /**
     * Returns error code.
     *
     * @return Authentication error code.
     */
    public String getErrorCode() {
        
        if (loginState == null) {
            return AMAuthErrorCode.AUTH_ERROR;
        }
        String errorCode = loginState.getErrorCode();
        
        if (debug.messageEnabled()) {
            debug.message("Error Code is.. : " + errorCode);
        }
        
        return errorCode;
    }
    
    /**
     * Gets the account lockout message
     * @return account lockout message
     */
    public String getLockoutMsg() {
        if (debug.messageEnabled()) {
            debug.message("lockout Msg returned  : " + lockoutMsg);
        }
        return lockoutMsg;
    }
   
    /**
     * Checks if the account is locked
     * @return <code>true</code> if account is locked
     */
    public boolean isLockedOut() {
        return accountLocked;
    }
    
    /* get the authlevel
     * gets the module list for a given config for all
     * modules having option REQUIRED, REQUISITE
     * gets the level for each module in the list
     * the highest level will be set.
     */
    String getAuthLevel(String orgDN) {
        
        AMAuthLevelManager levelManager = AMAuthLevelManager.getInstance();
        int maxLevel = Integer.MIN_VALUE;
        if (moduleSet == null || moduleSet.isEmpty()) {
            moduleSet = getSuccessModuleSet(orgDN);
        }
        Iterator mIterator = moduleSet.iterator();
        while (mIterator.hasNext()) {
            String moduleName =  (String) mIterator.next();
            int authLevel = levelManager.getLevelForModule(moduleName,
            orgDN, loginState.defaultAuthLevel);
            if (authLevel > maxLevel)  {
                maxLevel = authLevel;
            }
            
            if (debug.messageEnabled()) {
                debug.message("AuthLevel is : " + authLevel);
                debug.message("New AuthLevel is : " + maxLevel);
            }
        }
        
        if (debug.messageEnabled()) {
            debug.message("Returning AuthLevel is : " + maxLevel);
        }
        
        return (new Integer(maxLevel)).toString();
    }
    
    /* return the module list
     * this methods gets the configuration list for a given configName
     * retreives all module names which have option REQUIRED , REQUISITE
     */
    Set getSuccessModuleSet(String orgDN) {
        
        try {
            Set successModuleSet = loginState.getSuccessModuleSet();
            moduleSet = 
                getModuleFromAuthConfiguration(successModuleSet,orgDN);
            
            if (debug.messageEnabled()) {
                debug.message("ModuleSet is : " + moduleSet);
            }
        } catch (Exception e) {
            debug.message("Exception : getSuccessModuleList " , e);
        }
        return moduleSet;
    }
    
    /* constructs a module list string where each module is
     * separated by a "|" e.g module1 | module2 | module3
     */
    String getModuleString(Set moduleSet) {
        
        String moduleList=ISAuthConstants.EMPTY_STRING;
        
        if ((moduleSet != null) && (!moduleSet.isEmpty())) {
            Iterator mIterator = moduleSet.iterator();
            StringBuilder moduleString = new StringBuilder();
            
            while (mIterator.hasNext()) {
                String mClassName = (String)mIterator.next();
                moduleString.append(mClassName)
                .append(LIST_DELIMITER);
            }
            
            String mString = moduleString.toString();
            int i = mString.lastIndexOf(LIST_DELIMITER);
            
            if (i != -1) {
                moduleList = mString.substring(0,i);
            } else {
                moduleList = mString;
            }
        }
        
        if (debug.messageEnabled()) {
            debug.message("ModuleList is : " + moduleList);
        }
        return moduleList;
    }
    
    
    /* do the required process for different indextypes
     * return true if needs to return back
     * false if needs to continue
     * Exception if error
     */
    boolean processIndexType(
        AuthContext.IndexType indexType,
        String indexName, String orgDN
    ) throws AuthLoginException {
        boolean ignoreProfile = false;
        AuthContext.IndexType previousType = loginState.getPreviousIndexType();
        
        String normOrgDN = DNUtils.normalizeDN(orgDN);
        if ((previousType != AuthContext.IndexType.LEVEL &&
        previousType != AuthContext.IndexType.COMPOSITE_ADVICE) ||
        indexType != AuthContext.IndexType.MODULE_INSTANCE) {
            // proceed only when the org in the auth context matches
            // that in the query. otherwise it means a call with a new org.
            HttpServletRequest hreq = loginState.getHttpServletRequest();
            boolean isTokenValid = false;
            if (hreq != null) {
                try {
                    SSOTokenManager manager = SSOTokenManager.getInstance();
                    SSOToken ssoToken = manager.createSSOToken(hreq);
                    if (manager.isValidToken(ssoToken)) {
                        debug.message("Existing Valid session");
                        isTokenValid = true;
                    }
                } catch (Exception e) {
                    debug.message("ERROR processIndexType/SSOToken validation - "
                    + e.toString());
                }
                
                if (!isTokenValid) {
                    debug.message("No existing valid session");
                    Hashtable requestHash = loginState.getRequestParamHash();
                    String newOrgDN = AuthUtils.getDomainNameByRequest(hreq, requestHash);
                    if (debug.messageEnabled()){
                        debug.message("orgDN from existing auth context: " +
                        orgDN + ", orgDN from query string: " + newOrgDN);
                    }
                    if (normOrgDN != null ) {
                        if (!normOrgDN.equals(newOrgDN) && !pCookieMode) {
	                        st.setStatus(LoginStatus.AUTH_RESET);
	                        loginState.setErrorCode(AMAuthErrorCode.AUTH_ERROR);
	                        setErrorMsgAndTemplate();
	                        internalAuthError=true;
	                        throw new AuthLoginException(bundleName,
	                        AMAuthErrorCode.AUTH_ERROR, null);
                        }
                    }
                }
                
            }
        }
        if (indexType == AuthContext.IndexType.COMPOSITE_ADVICE)  {
            debug.message("IndexType is COMPOSITE_ADVICE");
                // Set the Composite Advice in Login State after decoding
                String compositeAdvice = URLEncDec.decode(indexName);
                loginState.setCompositeAdvice(compositeAdvice);
            // if is multiple modules are found then return
            // else continue with login process
            try {
                if (processCompositeAdvice(indexType,indexName,orgDN,clientType)) {
                    debug.message("multiple modules found");
                    return true;
                } else {
                    return false;
                }
            } catch (AuthException ae) {
                // no modules configured
                loginState.setErrorCode(ae.getErrorCode());
                loginState.logFailed(ae.getMessage());
                setErrorMsgAndTemplate();
                st.setStatus(LoginStatus.AUTH_FAILED);
                throw new AuthLoginException(ae);
            }
        } else if (indexType == AuthContext.IndexType.LEVEL)  {
            debug.message("IndexType is level");
            // if is multiple modules are found then return
            // else continue with login process
            try {
                if (processLevel(indexType,indexName,orgDN,clientType)) {
                    debug.message("multiple modules found");
                    return true;
                } else {
                    return false;
                }
            } catch (AuthException ae) {
                // no modules configured
                loginState.setErrorCode(ae.getErrorCode());
                loginState.logFailed(ae.getMessage());
                setErrorMsgAndTemplate();
                st.setStatus(LoginStatus.AUTH_FAILED);
                throw new AuthLoginException(ae);
            }
        } else if (indexType == AuthContext.IndexType.USER) {
            debug.message("IndexType is user");
            // if user is not active throw exception
            // else continue with login
            boolean userValid = false;
            if (!loginState.ignoreProfile()) {
                userValid = validateUser(indexName);
            } else {
                ignoreProfile = true;
            }
            if (pCookieMode) {
                processPCookieMode(userValid);
                return true;
            } else if ((!userValid) && (!ignoreProfile)) {
                debug.message("User is not active");
                loginState.logFailed(
                    bundle.getString("userInactive"),"USERINACTIVE");
                /* The user based authentication errors should not be different
                 * for users who exist and who don't, which can lead to 
                 * possiblity of enumerating existing users.
                 * The AMAuthErrorCode.AUTH_LOGIN_FAILED error code is used for
                 * all user based authentication errors.
                 * Refer issue3278 
                 */                
                loginState.setErrorCode(AMAuthErrorCode.AUTH_LOGIN_FAILED) ;
                setErrorMsgAndTemplate();
                //destroySession();
                st.setStatus(LoginStatus.AUTH_FAILED);
                throw new AuthLoginException(bundleName,
                AMAuthErrorCode.AUTH_USER_INACTIVE, null);
            } else if (ignoreProfile) {
                setAuthError(AMAuthErrorCode.AUTH_PROFILE_ERROR,"loginDenied");
                throw new AuthLoginException(bundleName,
                AMAuthErrorCode.AUTH_PROFILE_ERROR, null);
            } else {
                return false;
            }
        } else if (indexType == AuthContext.IndexType.MODULE_INSTANCE) {
            // check if module exists in the allowed modules list
            debug.message("indexType is module");
            boolean instanceExists =
            loginState.getDomainAuthenticators().contains(indexName);
            if (!indexName.equals(ISAuthConstants.APPLICATION_MODULE) &&
            !instanceExists) {
                debug.message("Module denied!!");
                loginState.setErrorCode(AMAuthErrorCode.AUTH_MODULE_DENIED);
                loginState.logFailed(
                    bundle.getString("moduleDenied"),"MODULEDENIED");
                setErrorMsgAndTemplate();
                st.setStatus(LoginStatus.AUTH_FAILED);
                throw new AuthLoginException(bundleName,
                AMAuthErrorCode.AUTH_MODULE_DENIED, null);
            } else {
                return false;
            }
        } else if (indexType == AuthContext.IndexType.ROLE) {
            debug.message("indexType is Role");
            if (loginState.ignoreProfile()) {
                setAuthError(AMAuthErrorCode.AUTH_TYPE_DENIED,"loginDenied");
                throw new AuthLoginException(bundleName,
                AMAuthErrorCode.AUTH_TYPE_DENIED, null);
            }
        }
        
        return false;
    }
    
    /* do required processing for persistent cookie */
    void processPCookieMode(boolean userValid) throws AuthLoginException {
        
        // check if user account has expired
        
        if (!loginState.ignoreProfile()) {
            if (!userValid) {
                if (debug.messageEnabled()) {
                    debug.message("user is not valid");
                }
                loginState.setErrorCode(AMAuthErrorCode.AUTH_USER_INACTIVE);
                setErrorMsgAndTemplate();
                st.setStatus(LoginStatus.AUTH_FAILED);
                throw new AuthLoginException(bundleName,
                AMAuthErrorCode.AUTH_USER_INACTIVE, null);
            }
            AMAccountLockout amAccountLockout =new AMAccountLockout(loginState);
            boolean accountLocked = amAccountLockout.isLockedOut();
            if (accountLocked) {
                loginState.logFailed(bundle.getString("lockOut"),"LOCKEDOUT");
                loginState.setErrorCode(AMAuthErrorCode.AUTH_USER_LOCKED);
                setErrorMsgAndTemplate();
                st.setStatus(LoginStatus.AUTH_FAILED);
                throw new AuthLoginException(bundleName,
                AMAuthErrorCode.AUTH_USER_LOCKED, null);
            }
            boolean accountExpired = amAccountLockout.isAccountExpired();
            if (accountExpired) {
                loginState.logFailed(
                    bundle.getString("accountExpired"),"ACCOUNTEXPIRED");
                loginState.setErrorCode(AMAuthErrorCode.AUTH_ACCOUNT_EXPIRED);
                setErrorMsgAndTemplate();
                st.setStatus(LoginStatus.AUTH_FAILED);
                throw new AuthLoginException(bundleName,
                AMAuthErrorCode.AUTH_ACCOUNT_EXPIRED, null);
            }
        }
        if (loginState.ignoreProfile()) {
            try {
                loginState.populateDefaultUserAttributes();
                loginState.setUserName(indexName);
            } catch (Exception e) {
                debug.message("Error get default attributes " , e);
                setAuthError(AMAuthErrorCode.AUTH_ERROR,"loginFailed");
                throw new AuthLoginException(bundleName,
                AMAuthErrorCode.AUTH_ERROR, null);
            }
        }
        // if pCookie is valid and if sessionUpgrade case
        // then don't update loginState and activate session
        // just return to continue with normal auth process.
        if (loginState.isSessionUpgrade()) {
            loginState.setPCookieUserName(indexName);
            return;
        }
        
        updateLoginState(loginState,indexType,indexName,configName,orgDN);
        Subject subject = new Subject();
        Principal userPrincipal = new UserPrincipal(indexName);
        subject.getPrincipals().add(userPrincipal);
        if (debug.messageEnabled()) {
            debug.message("Subject is.. :" + subject);
        }
        //activate session
        try {
            loginState.activateSession(subject, authContext);
            loginState.updateSessionForFailover();
            loginState.logSuccess();
        } catch (Exception e) {
            debug.message("Error activating session ");
            setAuthError(AMAuthErrorCode.AUTH_ERROR,"loginFailed");
            throw new AuthLoginException(bundleName,
            AMAuthErrorCode.AUTH_ERROR, null);
        }
        st.setStatus(LoginStatus.AUTH_SUCCESS);
        debug.message("login success");
        return;
    }
    
    /* set sid and loginState */
    void setLoginHash() {
        try {
            this.sid = AuthUtils.getSidString(authContext);
            this.loginState = AuthUtils.getLoginState(authContext);
            if (debug.messageEnabled()) {
                debug.message("sid .. "  + sid);
                debug.message("login state is .. : " + loginState);
            }
        } catch (Exception e) {
            debug.message("executLogin exception : " ,e);
        }
    }
    
    void setAuthError(String errorCode,String resString) {
        loginState.setErrorCode(errorCode);
        setErrorMsgAndTemplate();
        loginState.logFailed(bundle.getString(resString));
        st.setStatus(LoginStatus.AUTH_FAILED);
    }
    
    /**
     * Sets the failure URL and execute the post process login SPI.
     * for <code>internalAutherror</code> and if already executed
     * just skip this,
     */
    public void postProcessOnFail() {
        if ( (!internalAuthError) && (!processDone) ) {
            if (debug.messageEnabled()) {
                debug.message("postProcessOnFail ");
            }
            //setErrorMsgAndTemplate();
            loginState.postProcess(indexType,indexName,
                    LoginState.POSTPROCESS_FAILURE);            
            loginState.setFailureLoginURL(indexType,indexName);
            processDone = true;
        }
    }
    
    /**
     * Sets the success URL and execute the post process login
     * SPI. for <code>internalAutherror</code> and if already executed
     * just skip this.
     */
    public void postProcessOnSuccess() {
        if (!processDone ) {
            if (debug.messageEnabled()) {
                debug.message("postProcessOnSuccess ");
            }
            loginState.postProcess(indexType,indexName,
                    LoginState.POSTPROCESS_SUCCESS);
            processDone = true;
        }
    }
    
    /** This method returns a Set with is the list of
     * modules for a Authentication Configuration.
     * Only modules with control flag REQUIRED and
     * REQUISITE are returned.
     * @param moduleListSet list of configured auth module 
     * @return set of configured auth module with control flag REQUIRED and
     *         REQUISITE are returned
     */
    private Set getModuleFromAuthConfiguration(Set moduleListSet, 
                                               String orgDN) {
        Configuration config = Configuration.getConfiguration();
        if (configName == null) {
            configName = getConfigName(indexType,indexName,
            orgDN,
            loginState.getClientType());
        }
        AppConfigurationEntry[] moduleList =
        config.getAppConfigurationEntry(configName);
        if (debug.messageEnabled()) {
            debug.message("configName is : " + configName);
        }
        String moduleName = null;
        if ((moduleList != null) && (moduleList.length != 0)) {
            if (moduleList.length == 1) {
                moduleName = (String)moduleList[0].getOptions().get(
                    ISAuthConstants.MODULE_INSTANCE_NAME);
                moduleListSet.add(moduleName);
            } else {
                for (int i = 0; i < moduleList.length; i++) {
                    AppConfigurationEntry.LoginModuleControlFlag controlFlag =
                    moduleList[i].getControlFlag();
                    moduleName = (String)moduleList[i].getOptions().get(    
                        ISAuthConstants.MODULE_INSTANCE_NAME);
                    if (isControlFlagMatchFound(controlFlag)) {
                        moduleListSet.add(moduleName);
                    }
                }
            }
        }
        if (debug.messageEnabled()) {
            debug.message("ModuleSet is : " + moduleListSet);
        }
        
        return moduleListSet;
    }
    
    /* return the failure module list */
    String getFailureModuleList(String orgDN) {
        
        String moduleList=ISAuthConstants.EMPTY_STRING;
        try {
            Set failureModuleSet = loginState.getFailureModuleSet();
            Set moduleSet = 
                getModuleFromAuthConfiguration(failureModuleSet,orgDN);
            
            if (debug.messageEnabled()) {
                debug.message("ModuleSet is : " + moduleSet);
            }
            moduleList = getModuleString(moduleSet);
        } catch (Exception e) {
            debug.message("Exception : getFailureModuleList " , e);
        }
        if (debug.messageEnabled()) {
            debug.message("moduleList is :" + moduleList);
        }
        return moduleList;
    }
    
    
    /* Checks if the control flag matches the JAAS flags,
     * REQUIRED and REQUISITE flags
     */
    boolean isControlFlagMatchFound(
        AppConfigurationEntry.LoginModuleControlFlag controlFlag) {
        boolean isFlagMatchFound = false;
        if (controlFlag != null) {
            isFlagMatchFound  =
                ((controlFlag ==
                    AppConfigurationEntry.LoginModuleControlFlag.REQUIRED) ||
                (controlFlag ==
                    AppConfigurationEntry.LoginModuleControlFlag.REQUISITE));
        }
        
        return isFlagMatchFound;
    }
    
    /* Returns the successful list of modules names */
    String getSuccessModuleString(String orgDN) {
        if (moduleSet == null || moduleSet.isEmpty()) {
            moduleSet = getSuccessModuleSet(orgDN);
        }
        return getModuleString(moduleSet);
    }

    /**
     * Checks if is pure JAAS mode
     * @return <code>true</code> if pure JAAS
     */
    public boolean isPureJAAS() {
        return jaasCheck == 1;
    }
    
    private void nullifyUsedVars() {
        configName = null; // jaas configuration name.
        subject = null;
        clientType = null;
        moduleSet = null;
        entries = null;
        recdCallback = null;
        loginState.nullifyUsedVars();
    }
}
