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
 * Portions Copyrighted 2011-2016 ForgeRock AS.
 * Portions Copyrighted 2014 Nomura Research Institute, Ltd
 * Portions Copyrighted 2019 Open Source Solution Technology Corporation
 * Portions Copyrighted 2023 3A Systems LLC
 */
package com.sun.identity.authentication.service;

import static org.forgerock.openam.audit.AuditConstants.AuthenticationFailureReason.ACCOUNT_EXPIRED;
import static org.forgerock.openam.audit.AuditConstants.AuthenticationFailureReason.INVALID_PASSWORD;
import static org.forgerock.openam.audit.AuditConstants.AuthenticationFailureReason.LOCKED_OUT;
import static org.forgerock.openam.audit.AuditConstants.AuthenticationFailureReason.LOGIN_TIMEOUT;
import static org.forgerock.openam.audit.AuditConstants.AuthenticationFailureReason.MAX_SESSION_REACHED;
import static org.forgerock.openam.audit.AuditConstants.AuthenticationFailureReason.MODULE_DENIED;
import static org.forgerock.openam.audit.AuditConstants.AuthenticationFailureReason.MODULE_NOT_FOUND;
import static org.forgerock.openam.audit.AuditConstants.AuthenticationFailureReason.NO_CONFIG;
import static org.forgerock.openam.audit.AuditConstants.AuthenticationFailureReason.NO_USER_PROFILE;
import static org.forgerock.openam.audit.AuditConstants.AuthenticationFailureReason.USER_INACTIVE;

import java.security.AccessController;
import java.text.MessageFormat;
import java.util.HashSet;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.AppConfigurationEntry.LoginModuleControlFlag;
import javax.security.auth.login.Configuration;
import javax.security.auth.login.LoginException;
import javax.servlet.http.HttpServletRequest;

import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.openam.audit.AuditConstants.AuthenticationFailureReason;
import org.forgerock.openam.authentication.service.JAASModuleDetector;
import org.forgerock.openam.authentication.service.LoginContext;
import org.forgerock.openam.authentication.service.LoginContextFactory;
import org.forgerock.openam.utils.StringUtils;
import org.forgerock.util.Reject;

import com.iplanet.am.util.SystemProperties;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.authentication.AuthContext.IndexType;
import com.sun.identity.authentication.audit.AuthenticationProcessEventAuditor;
import com.sun.identity.authentication.config.AMAuthConfigUtils;
import com.sun.identity.authentication.config.AMAuthLevelManager;
import com.sun.identity.authentication.config.AMAuthenticationInstance;
import com.sun.identity.authentication.config.AMAuthenticationManager;
import com.sun.identity.authentication.config.AMConfiguration;
import com.sun.identity.authentication.config.AMConfigurationException;
import com.sun.identity.authentication.server.AuthContextLocal;
import com.sun.identity.authentication.service.DSAMECallbackHandler.DSAMECallbackHandlerError;
import com.sun.identity.authentication.spi.AuthErrorCodeException;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.authentication.spi.InvalidPasswordException;
import com.sun.identity.authentication.spi.MessageLoginException;
import com.sun.identity.authentication.spi.RedirectCallback;
import com.sun.identity.authentication.util.AMAuthUtils;
import com.sun.identity.authentication.util.ISAuthConstants;
import com.sun.identity.common.DNUtils;
import com.sun.identity.monitoring.Agent;
import com.sun.identity.monitoring.MonitoringUtil;
import com.sun.identity.monitoring.SsoServerAuthSvcImpl;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.encode.URLEncDec;
import com.sun.identity.shared.locale.AMResourceBundleCache;
import com.sun.identity.sm.DNMapper;

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
 * <code>AMLoginContext</code> sets and retrieves the authentication
 * configuration entry
 * <p>
 * This class actually starts the JAAS login process by instantiating the
 * <code>LoginContext</code> object with the JAAS configuration name and the
 * <code>CallbackHandler</code> followed by calling the
 * <code>LoginContext::login()</code> method.
 *
 */
public class AMLoginContext {

    private static final String LIST_DELIMITER = "|";
    /**
     * AuthThreadManager associated with this AMLoginContext.
     */
    public static AuthThreadManager authThread  = null;
    private final JAASModuleDetector jaasModuleDetector;
    private String exceedRetryLimit = null;
    private static final String BUNDLE_NAME = "amAuth";

    private String configName; // jaas configuration name.
    private String orgDN = null;
    private LoginContext loginContext;
    private LoginStatus loginStatus;
    private final AuthContextLocal authContext;
    private IndexType indexType;
    private String indexName;
    private String lockoutMsg = null;
    private Set<String> successModuleSet = null;
    private boolean accountLocked = false;
    private boolean isFailed = false;
    private boolean internalAuthError = false;
    private boolean processDone = false;
    private boolean jaasCheck = false;
    private Thread jaasThread = null;
    private Callback[] recdCallback;
    private final AuthenticationProcessEventAuditor auditor;

    private static SsoServerAuthSvcImpl authImpl;
    private static Configuration defaultConfig = null;
    private static AuthD ad;
    private static Debug debug;

    //OPENAM-3959
    private static boolean excludeRequiredOrRequisite = false;

    /**
     * Bundle to be used for localized error message. users can be in different
     * locales. Since we create an AMLoginContext for each user, we can cache
     * the bundle reference in the class
     */
    private ResourceBundle bundle;

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
            System.err.println("AMLoginContext:Set AM config error:" + e.getMessage());
        }

        if (MonitoringUtil.isRunning()) {
            authImpl = Agent.getAuthSvcMBean();
        }

        excludeRequiredOrRequisite =
                SystemProperties.getAsBoolean(Constants.AUTH_LEVEL_EXCLUDE_REQUIRED_REQUISITE, false);
    }

    /**
     * Creates <code>AMLoginContext</code> object.
     * @param authContext <code>AuthContextLocal</code> object. Must not be null;
     */
    public AMLoginContext(AuthContextLocal authContext) {
        Reject.ifNull(authContext);
        ad = AuthD.getAuth();
        debug = AuthD.debug;
        if (debug.messageEnabled()) {
            debug.message("AMLoginContext:initialThread name is... :" + Thread.currentThread().getName());
        }
        this.authContext = authContext;
        loginStatus = new LoginStatus();
        loginStatus.setStatus(LoginStatus.AUTH_IN_PROGRESS);
        auditor = InjectorHolder.getInstance(AuthenticationProcessEventAuditor.class);
        jaasModuleDetector = InjectorHolder.getInstance(JAASModuleDetector.class);
        bundle = ad.bundle; //default value for bundle until we find out
    }

    /**
     * Sets the JAAS configuration to the default container's configuration.
     */
    public static void resetJAASConfig() {
        try {
            Configuration.setConfiguration(defaultConfig);
        } catch (java.lang.SecurityException e) {
            System.err.println("AMLoginContext:resetJAASConfig to default:" + e.getMessage());
        }
    }

    /**
     * Used to explicitly set jaas to prevent ambiguity of usage.
     * Intentionally private.
     * @param jaasCheck The value to set jaasCheck to.
     */
    private void setJAASCheck(boolean jaasCheck) {
        this.jaasCheck = jaasCheck;
    }

    /**
     * Starts login process, the map passed to this method is the parameters
     * required to start the login process. These parameters are
     * <code>indexType</code>, <code>indexName</code> , <code>principal</code>,
     * <code>subject</code>, <code>password</code>,
     * <code>organization name</code>. Based on these parameters Module
     * Configuration name is retrieved using Configuration component. Creates
     * a new LoginContext and starts login process and returns. On error
     * LoginException is thrown.
     *
     * @throws AuthLoginException if execute login fails
     */
    public void executeLogin(Subject subject, IndexType loginIndexType, String loginIndexName, String locale, String redirectUrl)
            throws AuthLoginException {
        boolean errorState = false;
        internalAuthError = false;
        processDone = false;
        isFailed = false;
        /*
         * Ensure authContext.getLoginState() created and loginParamsMap provided
         */
        if (authContext.getLoginState() == null) {
            debug.error("Error: authContext.getLoginState()");
            loginStatus.setStatus(LoginStatus.AUTH_FAILED);
            if (authContext.getLoginState() != null) {
                authContext.getLoginState().setErrorCode(AMAuthErrorCode.AUTH_ERROR);
            }
            setErrorMsgAndTemplate();
            internalAuthError = true;
            throw new AuthLoginException(BUNDLE_NAME, AMAuthErrorCode.AUTH_ERROR, null);
        }

        /*
         * Lookup resource bundle and locale specific settings based on locale associated with authContext.getLoginState()
         */
        java.util.Locale loginLocale = com.sun.identity.shared.locale.Locale.getLocale(authContext.getLoginState().getLocale());
        bundle = AMResourceBundleCache.getInstance().getResBundle(BUNDLE_NAME, loginLocale);
        exceedRetryLimit = AMResourceBundleCache.getInstance()
                .getResBundle("amAuthLDAP", loginLocale).getString(ISAuthConstants.EXCEED_RETRY_LIMIT);
        if (debug.messageEnabled()) {
            debug.message("authContext.getLoginState() : " + authContext.getLoginState());
        }

        /*
         * Handle redirection if applicable
         */
        if (redirectUrl != null) {
            // Resource/IP/Env based auth case with Redirection Advice
            Callback[] redirectCallback = new Callback[1];
            redirectCallback[0] = new RedirectCallback(redirectUrl, null, "GET");
            if (isPureJAAS()) {
                authContext.getLoginState().setReceivedCallback_NoThread(redirectCallback);
            } else {
                authContext.getLoginState().setReceivedCallback(redirectCallback, this);
            }
            return;
        }

        /*
         * Initialize instance fields
         */
        this.indexType = loginIndexType;
        this.indexName = loginIndexName;
        try {
            if (StringUtils.isNotEmpty(locale)) {
                authContext.getLoginState().setLocale(locale);
            }
        } catch (Exception e) {
            if (debug.messageEnabled()) {
                debug.message("Error setting locale");
                debug.message("Exception " , e);
            }
        }


        /*
         * Copy orgDN and clientType values from authContext.getLoginState()
         */
        if (authContext.getOrgDN() != null && !authContext.getOrgDN().isEmpty()) {
            orgDN = authContext.getOrgDN();
            authContext.getLoginState().setQualifiedOrgDN(orgDN);
        } else {
            orgDN = authContext.getLoginState().getOrgDN();
        }
        String clientType = authContext.getLoginState().getClientType();
        if (debug.messageEnabled()) {
            debug.message("orgDN : " + orgDN);
            debug.message("clientType : " + clientType);
        }

        /*
         * Throw an exception if module-based authentication is disabled and an authentication module other
         * than APPLICATION_MODULE or FEDERATION_MODULE is explicitly requested.
         */
        if (indexType == IndexType.MODULE_INSTANCE
                && !authContext.getLoginState().getEnableModuleBasedAuth()
                && !indexName.equals(ISAuthConstants.APPLICATION_MODULE)) {
            String moduleClassName = null;
            try {
                AMAuthenticationManager authManager = new AMAuthenticationManager(
                        AccessController.doPrivileged(AdminTokenAction.getInstance()), orgDN);
                AMAuthenticationInstance authInstance = authManager.getAuthenticationInstance(indexName);
                moduleClassName = authInstance.getType();
            } catch (AMConfigurationException amce) {
                debug.error("AMLoginContext.executeLogin(): Unable to get authentication config", amce);
            }
            if (moduleClassName != null && !moduleClassName.equalsIgnoreCase(ISAuthConstants.FEDERATION_MODULE)) {
                throwExceptionIfModuleBasedAuthenticationDisabled();
            }
        }

        /*
         * Update authContext.getLoginState() indexType and indexName
         * (after storing current authContext.getLoginState() indexType if required for HTTP callback processing)
         */
        IndexType prevIndexType = authContext.getLoginState().getIndexType();
        if (prevIndexType == IndexType.LEVEL || prevIndexType == IndexType.COMPOSITE_ADVICE) {
            authContext.getLoginState().setPreviousIndexType(prevIndexType);
        }
        authContext.getLoginState().setIndexType(indexType);
        authContext.getLoginState().setIndexName(indexName);

        /*
         * Delegate actual processing of requested authentication type to the dispatch method 'processIndexType'
         */
        try {
            if (processIndexType(indexType, indexName, orgDN, clientType)) {
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
            debug.message("Error  : ", le);
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
            debug.message("Error : ", e);
            throw new AuthLoginException(e);
        }

        /*
         * Establish configName based on indexType, indexName, orgDN and clientType
         *
         * If configName can't be established, throw an exception
         */
        configName = getConfigName(indexType, indexName, orgDN, clientType);
        if (configName == null) {
            authContext.getLoginState().setErrorCode(AMAuthErrorCode.AUTH_CONFIG_NOT_FOUND);
            debug.message("Config not found");
            setErrorMsgAndTemplate();
            internalAuthError = true;
            loginStatus.setStatus(LoginStatus.AUTH_FAILED);
            authContext.getLoginState().logFailed(bundle.getString("noConfig"), "NOCONFIG");
            auditor.auditLoginFailure(authContext.getLoginState(), NO_CONFIG);

            if (MonitoringUtil.isRunning()) {
                if (authImpl == null) {
                    authImpl = Agent.getAuthSvcMBean();
                }
                if (authImpl != null) {
                    authImpl.incSsoServerAuthenticationFailureCount();
                }
            }
            throw new AuthLoginException(BUNDLE_NAME, AMAuthErrorCode.AUTH_CONFIG_NOT_FOUND, null);
        }

        /*
         * Create the LoginContext object that actually handles login/logout
         */
        if (debug.messageEnabled()) {
            debug.message("Creating login context object\n"
                    + "\n orgDN : " + orgDN
                    + "\n configName : " + configName);
        }
        try {

            Configuration configuration = getConfiguration();
            boolean jaasCheck = jaasModuleDetector.isPureJAASModulePresent(configName, configuration);

            if (jaasCheck) {
                debug.message("Using pure jaas mode.");
                if (authThread == null) {
                    authThread = new AuthThreadManager();
                    authThread.start();
                }
            }

            loginContext = LoginContextFactory.getInstance()
                    .createLoginContext(this, subject, configName, jaasCheck, configuration);

            setJAASCheck(jaasCheck);
        } catch (AuthLoginException ae) {
            debug.error("JAAS module for config: " + configName + ", " + ae.getMessage());
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
            if (indexType == IndexType.USER && AMAuthErrorCode.AUTH_CONFIG_NOT_FOUND.equals(ae.getErrorCode())) {
                authContext.getLoginState().setErrorCode(AMAuthErrorCode.AUTH_LOGIN_FAILED);
            } else {
                authContext.getLoginState().setErrorCode(ae.getErrorCode());
            }
            setErrorMsgAndTemplate();
            authContext.getLoginState().logFailed(bundle.getString("loginContextCreateFailed"));
            auditor.auditLoginFailure(authContext.getLoginState());
            internalAuthError = true;
            loginStatus.setStatus(LoginStatus.AUTH_FAILED);
            if (MonitoringUtil.isRunning()) {
                if (authImpl == null) {
                    authImpl = Agent.getAuthSvcMBean();
                }
                if (authImpl != null) {
                    authImpl.incSsoServerAuthenticationFailureCount();
                }
            }
            if (indexType == IndexType.USER && AMAuthErrorCode.AUTH_CONFIG_NOT_FOUND.equals(ae.getErrorCode())) {
                throw new AuthLoginException(BUNDLE_NAME, AMAuthErrorCode.AUTH_LOGIN_FAILED, null, ae);
            }
            throw ae;
        } catch (LoginException le) {
            debug.error("in creating LoginContext.");
            if (debug.messageEnabled()) {
                debug.message("Exception ", le);
            }
            authContext.getLoginState().setErrorCode(AMAuthErrorCode.AUTH_ERROR);
            authContext.getLoginState().logFailed(bundle.getString("loginContextCreateFailed"));
            auditor.auditLoginFailure(authContext.getLoginState());
            setErrorMsgAndTemplate();
            loginStatus.setStatus(LoginStatus.AUTH_FAILED);
            internalAuthError = true;
            if (MonitoringUtil.isRunning()) {
                if (authImpl == null) {
                    authImpl = Agent.getAuthSvcMBean();
                }
                if (authImpl != null) {
                    authImpl.incSsoServerAuthenticationFailureCount();
                }
            }
            throw new AuthLoginException(BUNDLE_NAME, AMAuthErrorCode.AUTH_ERROR, null, le);
        } catch (SecurityException se) {
            debug.error("security in creating LoginContext.");
            if (debug.messageEnabled()) {
                debug.message("Exception " , se);
            }
            authContext.getLoginState().setErrorCode(AMAuthErrorCode.AUTH_ERROR);
            setErrorMsgAndTemplate();
            authContext.getLoginState().logFailed(bundle.getString("loginContextCreateFailed"));
            auditor.auditLoginFailure(authContext.getLoginState());
            internalAuthError = true;
            loginStatus.setStatus(LoginStatus.AUTH_FAILED);
            if (MonitoringUtil.isRunning()) {
                if (authImpl == null) {
                    authImpl = Agent.getAuthSvcMBean();
                }
                if (authImpl != null) {
                    authImpl.incSsoServerAuthenticationFailureCount();
                }
            }
            throw new AuthLoginException(BUNDLE_NAME, AMAuthErrorCode.AUTH_ERROR, null);
        } catch (Exception e) {
            debug.error("Creating DSAMECallbackHandler: " + e.getMessage());
            authContext.getLoginState().setErrorCode(AMAuthErrorCode.AUTH_ERROR);
            setErrorMsgAndTemplate();
            authContext.getLoginState().logFailed(bundle.getString("loginContextCreateFailed"));
            auditor.auditLoginFailure(authContext.getLoginState());
            internalAuthError = true;
            if (MonitoringUtil.isRunning()) {
                if (authImpl == null) {
                    authImpl = Agent.getAuthSvcMBean();
                }
                if (authImpl != null) {
                    authImpl.incSsoServerAuthenticationFailureCount();
                }
            }
            loginStatus.setStatus(LoginStatus.AUTH_FAILED);
            throw new AuthLoginException(BUNDLE_NAME, AMAuthErrorCode.AUTH_ERROR, null, e);
        }

        /*
         * Perform the login using the objects this method has setup
         */
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
            loginStatus.setStatus(LoginStatus.AUTH_RESET);
            authContext.getLoginState().setErrorCode(AMAuthErrorCode.AUTH_ERROR);
            setErrorMsgAndTemplate();
            internalAuthError = true;
            if (MonitoringUtil.isRunning()) {
                if (authImpl == null) {
                    authImpl = Agent.getAuthSvcMBean();
                }
                if (authImpl != null) {
                    authImpl.incSsoServerAuthenticationFailureCount();
                }
            }
            throw new AuthLoginException(BUNDLE_NAME, AMAuthErrorCode.AUTH_ERROR, null);

        }
        debug.message("AMLoginContext:Thread started... returning.");
    }

    /**
     * Starts the login process ,calls JAAS Login Context
     */
    public void runLogin() {
        Thread thread = Thread.currentThread();
        String logFailedMessage = bundle.getString("loginFailed");
        String logFailedError = null;
        AuthenticationFailureReason failureReason = null;
        AMAccountLockout amAccountLockout;
        boolean loginSuccess = false;
        try {
            loginContext.login();
            Subject subject = loginContext.getSubject();

            authContext.getLoginState().setSubject(subject);

            if (!authContext.getLoginState().isAuthValidForInternalUser()) {
                if (debug.warningEnabled()) {
                    debug.warning("AMLoginContext.runLogin():auth failed, "
                            +  "using invalid realm name for internal user");
                }
                logFailedMessage = AuthUtils.getErrorVal(AMAuthErrorCode.AUTH_MODULE_DENIED, AuthUtils.ERROR_MESSAGE);
                logFailedError = "MODULEDENIED";
                failureReason = MODULE_DENIED;
                throw new AuthException(AMAuthErrorCode.AUTH_MODULE_DENIED, null);
            }

            debug.message("user authentication successful");

            // retrieve authenticated user's profile or create
            // a user profile if dynamic profile creation is
            // is true

            debug.message("searchUserProfile for Subject :");
            boolean profileState = authContext.getLoginState().searchUserProfile(subject, indexType, indexName);
            authContext.getLoginState().saveSubjectState();
            loginSuccess = true;
            if (!profileState) {
                debug.error("Profile not found ");
                logFailedMessage = bundle.getString("noUserProfile");
                logFailedError = "NOUSERPROFILE";
                failureReason = NO_USER_PROFILE;
                authContext.getLoginState().setErrorCode(AMAuthErrorCode.AUTH_PROFILE_ERROR);
                isFailed = true;
            } else {
                //update authContext.getLoginState() with authlevel , moduleName , role etc.
                amAccountLockout = new AMAccountLockout(authContext.getLoginState());
                if (amAccountLockout.isLockedOut()) {
                    debug.message("User locked out!!");
                    logFailedMessage = bundle.getString("lockOut");
                    logFailedError = "LOCKEDOUT";
                    failureReason = LOCKED_OUT;
                    authContext.getLoginState().setErrorCode(AMAuthErrorCode.AUTH_USER_LOCKED);
                    isFailed = true;
                } else {
                    boolean accountExpired = false;
                    if (!authContext.getLoginState().ignoreProfile()) {
                        accountExpired = amAccountLockout.isAccountExpired();
                    }
                    if (accountExpired) {
                        debug.message("Account expired!!");
                        logFailedMessage = bundle.getString("accountExpired");
                        logFailedError = "ACCOUNTEXPIRED";
                        failureReason = ACCOUNT_EXPIRED;
                        authContext.getLoginState().setErrorCode(AMAuthErrorCode.AUTH_ACCOUNT_EXPIRED);
                        isFailed = true;
                    } else {
                        // came here successful auth.
                        if (debug.messageEnabled()) {
                            debug.message("authContext is : " + authContext);
                            debug.message("authContext.getLoginState() is : " + authContext.getLoginState());
                        }

                        updateLoginState(indexType, indexName, configName, orgDN);
                        //activate session

                        boolean sessionActivated = authContext.getLoginState().activateSession(subject);
                        if (sessionActivated) {
                            if (amAccountLockout.isLockoutEnabled()) {
                                amAccountLockout.resetPasswdLockout(authContext.getLoginState().getUserUniversalId(
                                        authContext.getLoginState().getUserDN()), true);
                            }
                            loginStatus.setStatus(LoginStatus.AUTH_SUCCESS);
                            authContext.getLoginState().persistSession();
                            authContext.getLoginState().logSuccess();
                            auditor.auditLoginSuccess(authContext.getLoginState());
                            debug.message("login success");
                        } else {
                            logFailedMessage = AuthUtils.getErrorVal(AMAuthErrorCode.AUTH_MAX_SESSION_REACHED,
                                    AuthUtils.ERROR_MESSAGE);
                            logFailedError = "MAXSESSIONREACHED";
                            failureReason = MAX_SESSION_REACHED;
                            throw new AuthException(AMAuthErrorCode.AUTH_MAX_SESSION_REACHED, null);
                        }
                    }
                }
            }
        } catch (InvalidPasswordException ipe) {
            String failedUserId = ipe.getTokenId();
            if (failedUserId == null) {
                failedUserId = authContext.getLoginState().getFailureTokenId();
            }

            if (debug.messageEnabled()) {
                debug.message("Invalid Password : failedUserId " + failedUserId);
                debug.message("Invalid Password : Exception ", ipe);
            }

            if (failedUserId != null) {
                amAccountLockout = new AMAccountLockout(authContext.getLoginState());
                accountLocked = amAccountLockout.isAccountLocked(failedUserId);
                if ((!accountLocked) && (amAccountLockout.isLockoutEnabled())) {
                    amAccountLockout.invalidPasswd(failedUserId);
                    checkWarningCount(amAccountLockout);
                    accountLocked = amAccountLockout.isAccountLocked(failedUserId);
                }
            }

            logFailedMessage = bundle.getString("invalidPasswd");
            logFailedError = "INVALIDPASSWORD";
            failureReason = INVALID_PASSWORD;
            if (accountLocked) {
                authContext.getLoginState().setErrorCode(AMAuthErrorCode.AUTH_USER_LOCKED);
                if (failedUserId != null) {
                    authContext.getLoginState().logFailed(failedUserId, "LOCKEDOUT");
                } else {
                    authContext.getLoginState().logFailed("LOCKEDOUT");
                }
                auditor.auditLoginFailure(authContext.getLoginState(), LOCKED_OUT);
            } else {
                authContext.getLoginState().setErrorCode(AMAuthErrorCode.AUTH_LOGIN_FAILED);
            }

            isFailed = true;
            authContext.setLoginException(ipe);
        } catch (AuthErrorCodeException e) {
            if (debug.messageEnabled()) {
                debug.message(e.getMessage());
            }
            isFailed = true;
            java.util.Locale locale = com.sun.identity.shared.locale.Locale.getLocale(authContext.getLoginState().getLocale());
            authContext.getLoginState().setModuleErrorMessage(e.getL10NMessage(locale));
            authContext.getLoginState().setErrorCode(e.getAuthErrorCode());
            authContext.setLoginException(e);
        } catch (MessageLoginException me) {
            if (debug.messageEnabled()) {
                debug.message("LOGINFAILED MessageAuthLoginException....");
                debug.message("Exception " , me);
            }

            java.util.Locale locale = com.sun.identity.shared.locale.Locale.getLocale(authContext.getLoginState().getLocale());
            authContext.getLoginState().setModuleErrorMessage(me.getL10NMessage(locale));
            authContext.getLoginState().setErrorMessage(me.getL10NMessage(locale));
            isFailed = true;
            authContext.setLoginException(me);
        } catch (AuthLoginException le) {
            authContext.getLoginState().setErrorCode(AMAuthErrorCode.AUTH_LOGIN_FAILED);
            if (AMAuthErrorCode.AUTH_MODULE_DENIED.equals(le.getMessage())) {
                if (debug.warningEnabled()) {
                    debug.warning(
                            "AMLoginContext.runLogin():auth failed, using invalid auth module name for internal user");
                }
                logFailedMessage = AuthUtils.getErrorVal(AMAuthErrorCode.AUTH_MODULE_DENIED, AuthUtils.ERROR_MESSAGE);
                logFailedError = "MODULEDENIED";
                failureReason = MODULE_DENIED;
                authContext.getLoginState().setErrorCode(AMAuthErrorCode.AUTH_MODULE_DENIED);
            } else if (AMAuthErrorCode.AUTH_TIMEOUT.equals(le.getMessage())) {
                debug.message("LOGINFAILED Error Timed Out....");
            } else if (ISAuthConstants.EXCEED_RETRY_LIMIT.equals(le.getErrorCode())) {
                debug.message("LOGINFAILED ExceedRetryLimit");
            } else {
                debug.message("LOGINFAILED Error....");
            }
            if (debug.messageEnabled()) {
                debug.message("Exception : ", le);
            }
            isFailed = true;
            if (authContext.getLoginState().isTimedOut()) {
                logFailedMessage = bundle.getString("loginTimeout");
                logFailedError = "LOGINTIMEOUT";
                failureReason = LOGIN_TIMEOUT;
                authContext.getLoginState().setErrorCode(AMAuthErrorCode.AUTH_TIMEOUT);
            } else if (ISAuthConstants.EXCEED_RETRY_LIMIT.equals(le.getErrorCode())) {
                authContext.getLoginState().setErrorMessage(exceedRetryLimit);
                authContext.getLoginState().setErrorCode(AMAuthErrorCode.AUTH_USER_LOCKED_IN_DS);
            } else if (ISAuthConstants.SERVER_UNWILLING.equals(le.getErrorCode())) {
                authContext.getLoginState().setErrorCode(AMAuthErrorCode.AUTH_ERROR);
            }
            authContext.setLoginException(le);
        } catch (AuthException e) {
            if (debug.messageEnabled()) {
                debug.message("Exception : " + e.getMessage());
            }
            isFailed = true;
            authContext.getLoginState().setErrorCode(e.getErrorCode());
            authContext.getLoginState().logFailed(bundle.getString("loginFailed"));
            logFailedError = null;
            authContext.setLoginException(new AuthLoginException(BUNDLE_NAME, "loginFailed", null, e));
        } catch (Exception e) {
            debug.message("Error during login.. ");
            if (debug.messageEnabled()) {
                debug.message("Exception ", e);
            }
            isFailed = true;
            authContext.getLoginState().setErrorCode(AMAuthErrorCode.AUTH_ERROR);
            authContext.getLoginState().logFailed(bundle.getString("loginFailed"));
            logFailedError = null;
            authContext.setLoginException(new AuthLoginException(BUNDLE_NAME, "loginFailed", null, e));
        } catch (DSAMECallbackHandlerError error) {
            debug.message("Caught error returned from DSAMEHandler");
            return;
        } catch (Throwable e) {
        		debug.error("Error during login.. ",e);
        		throw e;
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
                authContext.getLoginState().setFailureModuleList(getSuccessModuleString(orgDN));

            } else {
                authContext.getLoginState().setFailureModuleList(getFailureModuleList(orgDN));
            }
            authContext.getLoginState().logFailed(logFailedMessage, logFailedError);
            auditor.auditLoginFailure(authContext.getLoginState(), failureReason);
            setErrorMsgAndTemplate();
            loginStatus.setStatus(LoginStatus.AUTH_FAILED);
            if (indexType == IndexType.USER) {
                if (debug.messageEnabled()) {
                    debug.message("Set failureId in user based auth " + indexName);
                }
                authContext.getLoginState().setFailedUserId(indexName);
            }
        } else {
            if (debug.messageEnabled()) {
                debug.message("AMLoginContext.runLogin: calling incSsoServerAuthenticationSuccessCount");
            }
            if (MonitoringUtil.isRunning()) {
                if (authImpl == null) {
                    authImpl = Agent.getAuthSvcMBean();
                }
            }
            if (authImpl != null && !authContext.getLoginState().isNoSession()) {
                authImpl.incSsoServerAuthenticationSuccessCount();
            }
        }

        if (debug.messageEnabled()) {
            debug.message("finished...login notify all threads\n"
                    + "AMLoginContext:LoginStatus: " + loginStatus.getStatus());
        }
        if (isPureJAAS()) {
            authThread.removeFromHash(thread, "timeoutHash");

            // notify possible waiting thread
            authContext.getLoginState().setReceivedCallback(null, this);
        }

        isFailed = false;
        nullifyUsedVars();
    }

    /**
     * Logs out.
     *
     * @throws AuthLoginException when fails to logout
     */
    public void logout() throws AuthLoginException {
        debug.message("in logout:");
        try {
            if (null != loginContext) {
                loginContext.logout();
            }

            authContext.getLoginState().logLogout();
            auditor.auditLogout(getSSOToken());
            authContext.getLoginState().postProcess(this, indexType, indexName, LoginState.PostProcessEvent.LOGOUT);
            destroySession();
            loginStatus.setStatus(LoginStatus.AUTH_COMPLETED);
        } catch (AuthLoginException le) {
            debug.message("Error during logout : ");
            if (debug.messageEnabled()) {
                debug.message("Exception " , le);
            }
            //logout - ignore this error since logout will be done
            throw new AuthLoginException(BUNDLE_NAME, "failedLogout", null, le);
        } catch (Exception e) {
            debug.message("Error during logout : ");
            if (debug.messageEnabled()) {
                debug.message("Exception " , e);
            }
        }
    }

    /* destroy Session on a logout OR abort */
    private void destroySession() {
        if (debug.messageEnabled()) {
            debug.message("AMLoginContext:destroySession: " + authContext.getLoginState());
        }
        authContext.getLoginState().destroySession();
    }

    /**
     * Returns array of received callbacks from module.
     *
     * @return array of received callbacks from module.
     */
    public Callback[] getRequiredInfo() {
        if (loginStatus.getStatus() != LoginStatus.AUTH_IN_PROGRESS) {
            return null;
        }
        if (indexType == IndexType.LEVEL || indexType == IndexType.COMPOSITE_ADVICE) {
            debug.message("IndexType level/composite_advice, send choice callback");
            // reset indexType since UI will start module based auth
            indexType = null;
        } else {
            if (isPureJAAS()) {
                recdCallback = getRequiredInfoCallback();
            } else {
                recdCallback = getRequiredInfoCallback_NoThread();
            }
        }

        if (recdCallback != null) {
            if (debug.messageEnabled()) {
                for (Callback callback : recdCallback) {
                    debug.message("Recd Callback in amlc.getRequiredInfo : " + callback);
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
    private Callback[] getRequiredInfoCallback_NoThread() {
        return authContext.getLoginState().getReceivedInfo();
    }


    /**
     * Returns the array of required Callbacks from <code>CallbackHandler</code>
     * waits till <code>authContext.getLoginState()::getReceivedInfo()</code> OR
     * authentication status is not <code>AUTH_IN_PROGRESS</code> OR
     * if thread receives a notify .
     *
     * @return array of Required Callbacks from <code>CallbackHandler</code>.
     */
    private synchronized Callback[] getRequiredInfoCallback() {
        if (debug.messageEnabled()) {
            debug.message("getRequiredInfo.. " + loginStatus.getStatus());
        }
        if (isFailed || (loginStatus.getStatus() != LoginStatus.AUTH_IN_PROGRESS)) {
            debug.message("no more requirements returning null");
            return null;
        }
        Thread thread = Thread.currentThread();
        long lastCallbackSent = authContext.getLoginState().getLastCallbackSent();
        long pageTimeOut = authContext.getLoginState().getPageTimeOut();
        if (debug.messageEnabled()) {
            debug.message("getRequiredInfo. ThreadName is.. :" + thread);
            debug.message("lastCallbackSent : " + lastCallbackSent);
            debug.message("pageTimeOut : " + pageTimeOut);
        }
        authThread.setHash(thread, pageTimeOut, lastCallbackSent);

        while ((!isFailed) && (authContext.getLoginState().getReceivedInfo() == null)
                && (loginStatus.getStatus() == LoginStatus.AUTH_IN_PROGRESS)) {
            try {
                if (debug.messageEnabled()) {
                    debug.message(Thread.currentThread() + "Waiting.." + loginStatus.getStatus());
                }
                if (loginStatus.getStatus() != LoginStatus.AUTH_IN_PROGRESS) {
                    return null;
                }
                if (!isFailed
                        && loginStatus.getStatus() == LoginStatus.AUTH_IN_PROGRESS
                        && authContext.getLoginState().getReceivedInfo() == null) {
                    this.wait();
                }
            } catch (InterruptedException e) {
                debug.message("getRecdinfo INTERRUPTED");
                break;
            }
        }
        if (debug.messageEnabled()) {
            debug.message("Thread woke up... " + authContext.getLoginState().getReceivedInfo());
        }
        Callback[] getRequiredInfo = authContext.getLoginState().getReceivedInfo();
        if (debug.messageEnabled()) {
            debug.message("Returning getRequiredInfo... :" + getRequiredInfo);
        }
        authThread.removeFromHash(thread, "timeoutHash");
        return getRequiredInfo;
    }

    /**
     * Sets the submitted requirements, called by
     * <code>AuthContext.submitRequirements</code>
     * <code>authContext.getLoginState().setSubmittedCallback</code> is update.
     *
     * @param callback submit the required <code>Callbacks</code>
     */
    public void submitRequiredInfo(Callback[] callback) {
        if (debug.messageEnabled() && callback != null && callback.length > 0) {
            debug.message("submit required info... :" + callback[0]);
        }
        if (isPureJAAS()) {
            authContext.getLoginState().setSubmittedCallback(callback, this);
        } else {
            authContext.getLoginState().setSubmittedCallback_NoThread(callback);
        }
        if (debug.messageEnabled()) {
            debug.message("Returning from submitRequiredInfo");
        }
    }


    /**
     * <code>CallbackHandler</code> calls this to retrieve the submitted
     * credentials/callbacks waits till
     * <code>authContext.getLoginState().setSubmittedCallback</code> is set OR
     * <code>LoginStatus</code> is not <code>AUTH_IN_PROGRESS</code>.
     *
     * @return submitted credentials/callbacks.
     */
    public synchronized Callback[] submitCallbackInfo() {
        if (debug.messageEnabled()) {
            debug.message("submitRequiredInfo. ThreadName is.. :" + Thread.currentThread().getName());
        }

        if (loginStatus.getStatus() != LoginStatus.AUTH_IN_PROGRESS || isFailed) {
            debug.message("submitReq no more requirements returning null");
            return null;
        }

        Thread thread = Thread.currentThread();
        long lastCallbackSent = authContext.getLoginState().getLastCallbackSent();
        long pageTimeOut = authContext.getLoginState().getPageTimeOut();
        if (debug.messageEnabled()) {
            debug.message("submitRequiredInfo. ThreadName is.. :" + thread);
            debug.message("lastCallbackSent : " + lastCallbackSent);
            debug.message("pageTimeOut : " + pageTimeOut);
        }
        authThread.setHash(thread,pageTimeOut, lastCallbackSent);
        while (authContext.getLoginState().getSubmittedInfo() == null && loginStatus.getStatus() == LoginStatus.AUTH_IN_PROGRESS) {
            try {
                if (debug.messageEnabled()) {
                    debug.message(Thread.currentThread() + " Waiting...." + loginStatus.getStatus());
                }
                if (loginStatus.getStatus() != LoginStatus.AUTH_IN_PROGRESS) {
                    return null;
                }
                if ((authContext.getLoginState().getSubmittedInfo() == null)) {
                    wait();
                }
            } catch (InterruptedException e) {
                debug.message("submitRequired info INTERRUPTED");
                break;
            }
        }
        debug.message("Threadwaking up go submit info...");
        authThread.removeFromHash(thread, "timeoutHash");
        Callback[] setSubmittedInfo = authContext.getLoginState().getSubmittedInfo();
        debug.message("Returning submitted info: ");
        return setSubmittedInfo;
    }

    /**
     * Returns the authentication status.
     *
     * @return the authentication status.
     */
    public int getStatus() {
        int status  = loginStatus.getStatus();

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
                debug.message("Exception ", ae);
            }
            try {
                destroySession();
                loginStatus.setStatus(LoginStatus.AUTH_COMPLETED);
            } catch (Exception e) {
                debug.message("Error aborting");
                if (debug.messageEnabled()) {
                    debug.message("Exception ", e);
                }

                // abort this error - since abort will be done
                throw new AuthLoginException(BUNDLE_NAME, "abortFailed", null);
            }
        }
    }


    /**
     * Returns authentication modules configured for a given organization.
     *
     * @return authentication modules configured for a given organization.
     */
    public Set<String> getModuleInstanceNames() {
        Set<String> moduleSet = new HashSet<>();
        try {
            LoginState loginState =  authContext.getLoginState();

            if (loginState != null) {
                moduleSet = loginState.getModuleInstances();
            }

            if (debug.messageEnabled()) {
                debug.message("successModuleSet is : " + moduleSet);
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
        return authContext.getLoginState().getQueryOrg();
    }

    /**
     * Returns Single Sign On Token for authenticated user, returns null if
     * session is inactive.
     *
     * @return Single Sign On Token for authenticated user.
     */
    public SSOToken getSSOToken() {
        try {
            return authContext.getLoginState().getSSOToken();
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
            return authContext.getLoginState().getSuccessLoginURL();
        } catch (Exception e) {
            if (debug.messageEnabled()) {
                debug.message("error getting successURL : " + e.toString());
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
            return authContext.getLoginState().getFailureLoginURL();
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
            sidString = authContext.getLoginState().getSid().toString();
        } catch (Exception e) {
            if (debug.messageEnabled()) {
                debug.message("Error retrieving sid from authContext.getLoginState() : " + e.getMessage());
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
            return authContext.getLoginState().getSubject();
        } catch (Exception e) {
            if (debug.messageEnabled()) {
                debug.message("error getting Subject :");
                debug.message("Exception " , e);
            }

            return null;
        }
    }
    
    /**
     * Returns current login context
     * 
     * @return login context
     */
    public LoginContext getLoginContext() {
    	return this.loginContext;
    }
    

    /* retrieve config name from config component based on the
     * indexType , indexName , orgDN and clientType
     * if indexType , indexName are null then indexType is assumed
     * to be org
     */
    private String getConfigName(IndexType indexType, String indexName, String orgDN, String clientType) {
        String configName = null;
        String universalID;

        // if index type is null assume org based authentication
        if (indexType == null) {
            configName = AMAuthConfigUtils.getAuthConfigName(orgDN, "html");
        } else {
            if (indexType == IndexType.USER) {
                universalID = authContext.getLoginState().getUserUniversalId(indexName);
            } else if (indexType == IndexType.ROLE) {
                universalID = authContext.getLoginState().getRoleUniversalId(indexName);
            } else {
                // means the index type is not ROLE or USER
                // for SERVICE , MODULE pass the indexName as is
                universalID = indexName;
            }
            try {
                if (universalID != null ) {
                    configName = AMAuthConfigUtils.getAuthConfigName(indexType, universalID, orgDN, clientType);
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
    private boolean processLevel(String indexName, String orgDN, String clientType)
            throws AuthException, AuthLoginException {

        throwExceptionIfModuleBasedAuthenticationDisabled();

        indexType= IndexType.LEVEL;

        java.util.Locale loc = com.sun.identity.shared.locale.Locale.getLocale(authContext.getLoginState().getLocale());
        AuthLevel authLevel = new AuthLevel(indexType, indexName, orgDN, clientType, loc);
        int numberOfModules = authLevel.getNumberOfAuthModules();
        if (debug.messageEnabled()) {
            debug.message("number of Modules : " + numberOfModules);
        }

        if (numberOfModules <= 0) {
            authContext.getLoginState().logFailed(bundle.getString("noConfig"), "NOCONFIG");
            auditor.auditLoginFailure(authContext.getLoginState(), NO_CONFIG);
            throw new AuthException(AMAuthErrorCode.AUTH_CONFIG_NOT_FOUND, null);
        } else if (numberOfModules == 1) {
            this.indexType = IndexType.MODULE_INSTANCE;
            authContext.getLoginState().setIndexType(this.indexType);
            this.indexName = authLevel.getModuleName();
            return false;
        } else {
            try {
                recdCallback = authLevel.createChoiceCallback();
                authContext.getLoginState().setPrevCallback(recdCallback);
                authContext.getLoginState().setModuleMap(authLevel.getModuleMap());
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

    /* for indexType composite_advice retrieves the module names .
     * if there is more then one modules required in composite advice
     * then generate choice callback , else if module
     * is 1 then start module based authentication.
     * throws Exception if no modules are found
     */
    private boolean processCompositeAdvice(String indexName, String orgDN, String clientType)
            throws AuthException, AuthLoginException {

        java.util.Locale loc = com.sun.identity.shared.locale.Locale.getLocale(authContext.getLoginState().getLocale());
        CompositeAdvices compositeAdvice = new CompositeAdvices(indexName, orgDN, clientType, loc);

        if (compositeAdvice.getType() == AuthUtils.MODULE) {
            throwExceptionIfModuleBasedAuthenticationDisabled();
        }

        int numberOfModules = compositeAdvice.getNumberOfAuthModules();
        if (debug.messageEnabled()) {
            debug.message("processCompositeAdvice:number of Modules/Services : " + numberOfModules);
        }
        authContext.getLoginState().setCompositeAdviceType(compositeAdvice.getType());

        if (numberOfModules <= 0) {

            authContext.getLoginState().logFailed(bundle.getString("noConfig"));
            auditor.auditLoginFailure(authContext.getLoginState(), NO_CONFIG);
            throw new AuthException(AMAuthErrorCode.AUTH_CONFIG_NOT_FOUND, null);

        } else if (numberOfModules == 1) {

            this.indexName = AMAuthUtils.getDataFromRealmQualifiedData(compositeAdvice.getModuleName());
            String qualifiedRealm = AMAuthUtils.getRealmFromRealmQualifiedData(compositeAdvice.getModuleName());
            if (StringUtils.isNotEmpty(qualifiedRealm)) {
                this.orgDN = DNMapper.orgNameToDN(qualifiedRealm);
                authContext.getLoginState().setQualifiedOrgDN(this.orgDN);
            }
            if (compositeAdvice.getType() == AuthUtils.MODULE) {
                this.indexType = IndexType.MODULE_INSTANCE;
            } else if (compositeAdvice.getType() == AuthUtils.SERVICE) {
                this.indexType = IndexType.SERVICE;
            } else if (compositeAdvice.getType() == AuthUtils.REALM) {
                this.orgDN = DNMapper.orgNameToDN(compositeAdvice.getModuleName());
                authContext.getLoginState().setQualifiedOrgDN(this.orgDN);
                this.indexName = AuthUtils.getOrgConfiguredAuthenticationChain(this.orgDN);
                this.indexType = IndexType.SERVICE;
            }
            authContext.getLoginState().setIndexType(this.indexType);
            authContext.getLoginState().setIndexName(this.indexName);
            if (debug.messageEnabled()) {
                debug.message("processCompositeAdvice:indexType : " + this.indexType);
                debug.message("processCompositeAdvice:indexName : " + this.indexName);
            }
            return false;

        } else {

            try {
                recdCallback = compositeAdvice.createChoiceCallback();
                authContext.getLoginState().setPrevCallback(recdCallback);
                authContext.getLoginState().setModuleMap(compositeAdvice.getModuleMap());
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

    /*
     * Throw an exception as module-based authentication is disabled.
     */
    private void throwExceptionIfModuleBasedAuthenticationDisabled() throws AuthLoginException {
        if (!authContext.getLoginState().getEnableModuleBasedAuth()) {
            debug.error("Error: Module Based Auth is not allowed");
            loginStatus.setStatus(LoginStatus.AUTH_FAILED);
            authContext.getLoginState().setErrorCode(AMAuthErrorCode.MODULE_BASED_AUTH_NOT_ALLOWED);
            setErrorMsgAndTemplate();
            throw new AuthLoginException(BUNDLE_NAME, AMAuthErrorCode.MODULE_BASED_AUTH_NOT_ALLOWED, null);
        }
    }

    /* update login state with indexType,indexName */
    private void updateLoginState(IndexType indexType, String indexName, String configName, String orgDN) {
        // set authLevel in authContext.getLoginState()

        String authLevel;
        if (indexType == IndexType.LEVEL) {
            authLevel = indexName;
        } else {
            // retrieve from config component check with Qingwen
            // config component will return the max level in case
            // of multiple authentication.
            authLevel = getAuthLevel(orgDN);
        }

        authContext.getLoginState().setAuthLevel(authLevel);

        // set the module name
        String moduleName;

        if (indexType == IndexType.MODULE_INSTANCE) {
            moduleName = indexName;
        } else {
            moduleName = getSuccessModuleString(orgDN);
        }

        if (debug.messageEnabled()) {
            debug.message("moduleName : " + moduleName);
        }

        authContext.getLoginState().setAuthModuleName(moduleName);
    }

    /* check if user exists and is enabled if not return
     * false - login process should not continue
     */
    boolean validateUser(String userName) {
        try {
            boolean userProfileExists = authContext.getLoginState().getUserProfile(userName, true);
            return ((userProfileExists) && (authContext.getLoginState().isUserEnabled()));
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
    private void checkWarningCount(AMAccountLockout amAccountLockout) {
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
                    authContext.getLoginState().setLockoutMsg(lockoutMsg);
                    accountLocked = false;
                }
            }

            if (debug.messageEnabled()) {
                debug.message("WARNING COUNT : " + warningCount);
                debug.message("WARNING COUNT MESSAGE: " + lockoutMsg);
            }
        } catch (Exception e) {
            debug.message("Error : ", e);
        }
    }

    /**
     * Sets the error message and template
     */
    private void setErrorMsgAndTemplate() {
        if (authContext.getLoginState() == null) {
            return;
        }
        String errorCode = authContext.getLoginState().getErrorCode();

        if (errorCode != null) {
            String resProperty = bundle.getString(errorCode);
            if (debug.messageEnabled()) {
                debug.message("resProperty is.. :" + resProperty);
            }
            String errorMsg = AuthUtils.getErrorVal(errorCode, AuthUtils.ERROR_MESSAGE, bundle);
            String templateName = AuthUtils.getErrorVal(errorCode, AuthUtils.ERROR_TEMPLATE);

            if (debug.messageEnabled()) {
                debug.message("Error Message : " + errorMsg);
                debug.message("Error Template: " + templateName);
            }

            authContext.getLoginState().setErrorMessage(errorMsg);
            authContext.getLoginState().setErrorTemplate(templateName);
        }
    }

    /* for error handling - methods to return error code , module error
     * template , framework error template , error message
     */
    private String getTimedOutTemplate() {
        authContext.getLoginState().setErrorCode(AMAuthErrorCode.AUTH_TIMEOUT);
        authContext.getLoginState().logFailed(bundle.getString("loginTimeout"), "LOGINTIMEOUT");
        auditor.auditLoginFailure(authContext.getLoginState(), LOGIN_TIMEOUT);
        authContext.getLoginState().setErrorMessage(AuthUtils.getErrorVal(AMAuthErrorCode.AUTH_TIMEOUT, AuthUtils.ERROR_MESSAGE));
        return AuthUtils.getErrorVal(AMAuthErrorCode.AUTH_TIMEOUT, AuthUtils.ERROR_TEMPLATE);
    }

    /**
     * Returns error template.
     *
     * @return error template.
     */
    public String getErrorTemplate() {

        String errorTemplate;
        if (authContext.getLoginState() == null) {
            errorTemplate = AuthUtils.getErrorVal(AMAuthErrorCode.AUTH_ERROR, AuthUtils.ERROR_TEMPLATE);
            return errorTemplate;
        }
        if (authContext.getLoginState().isTimedOut()) {
            errorTemplate = getTimedOutTemplate();
        } else {
            errorTemplate = authContext.getLoginState().getModuleErrorTemplate();
            if (errorTemplate == null || errorTemplate.equals(ISAuthConstants.EMPTY_STRING)) {
                errorTemplate = authContext.getLoginState().getErrorTemplate();
            }
        }
        if (debug.messageEnabled()) {
            debug.message("Error Template is : " + errorTemplate);
        }
        authContext.getLoginState().setErrorTemplate(errorTemplate);
        return errorTemplate;
    }

    /**
     * Returns error message.
     *
     * @return error message.
     */
    public String getErrorMessage() {

        if (authContext.getLoginState() == null) {
            return AuthUtils.getErrorVal(AMAuthErrorCode.AUTH_ERROR, AuthUtils.ERROR_MESSAGE);
        }

        String errorMsg = authContext.getLoginState().getModuleErrorMessage();
        if (errorMsg == null) {
            errorMsg = authContext.getLoginState().getErrorMessage();
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

        if (authContext.getLoginState() == null) {
            return AMAuthErrorCode.AUTH_ERROR;
        }
        String errorCode = authContext.getLoginState().getErrorCode();

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
    private String getAuthLevel(String orgDN) {

        AMAuthLevelManager levelManager = AMAuthLevelManager.getInstance();
        int maxLevel = Integer.MIN_VALUE;

        if (successModuleSet == null || successModuleSet.isEmpty()) {
            successModuleSet = getSuccessModuleSet(orgDN);
        }

        for (String moduleName : successModuleSet) {
            int authLevel = levelManager.getLevelForModule(moduleName, orgDN, authContext.getLoginState().getDefaultAuthLevel());
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
     * if org.forgerock.openam.authLevel.excludeRequiredOrRequisite is false
     */
    private Set<String> getSuccessModuleSet(String orgDN) {
        Set<String> moduleSet = new HashSet<>();
        try {
            Set<String> successModuleSet = authContext.getLoginState().getSuccessModuleSet();
            if (excludeRequiredOrRequisite) {
                if (debug.messageEnabled()) {
                    debug.message("get success modules excluding REQUIRED or REQUISITE in chain.");
                }
                moduleSet = successModuleSet;
            } else {
                if (debug.messageEnabled()) {
                    debug.message("retrieve all modules names with option REQUIRED or REQUISITE.");
                }
                moduleSet = getModuleFromAuthConfiguration(successModuleSet, orgDN);
            }

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
    private String getModuleString(Set<String> moduleSet) {

        final String moduleList = moduleSet == null || moduleSet.isEmpty() ?
                ISAuthConstants.EMPTY_STRING :
                org.apache.commons.lang.StringUtils.join(moduleSet, LIST_DELIMITER);

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
    private boolean processIndexType(IndexType indexType, String indexName, String orgDN, String clientType) throws AuthLoginException {
        boolean ignoreProfile = false;
        IndexType previousType = authContext.getLoginState().getPreviousIndexType();

        /*
         * Throw an exception if org specified in query does not match org specified in authContext/authContext.getLoginState()
         *
         * (unless previous index type was LEVEL or COMPOSITE_ADVICE, or current index type is MODULE_INSTANCE)
         */
        String normOrgDN = DNUtils.normalizeDN(orgDN);
        if ((previousType != IndexType.LEVEL && previousType != IndexType.COMPOSITE_ADVICE)
                || indexType != IndexType.MODULE_INSTANCE) {
            // proceed only when the org in the auth context matches
            // that in the query. otherwise it means a call with a new org.
            HttpServletRequest hreq = authContext.getLoginState().getHttpServletRequest();
            boolean isTokenValid = false;
            final boolean isFederation = (hreq != null && hreq.getAttribute(Constants.WSFED_ACTIVE_LOGIN) != null)
                    || (indexType == IndexType.MODULE_INSTANCE && ISAuthConstants.FEDERATION_MODULE.equals(indexName));
            if (hreq != null && !isFederation) {
                try {
                    SSOTokenManager manager = SSOTokenManager.getInstance();
                    SSOToken ssoToken = manager.createSSOToken(hreq);
                    if (manager.isValidToken(ssoToken)) {
                        debug.message("Existing Valid session");
                        isTokenValid = true;
                    }
                } catch (Exception e) {
                    debug.message("ERROR processIndexType/SSOToken validation - " + e.toString());
                }

                if (!isTokenValid) {
                    debug.message("No existing valid session");
                    Map<String, String> requestHash = authContext.getLoginState().getRequestParamHash();
                    String newOrgDN = AuthUtils.getDomainNameByRequest(hreq, requestHash);
                    if (debug.messageEnabled()) {
                        debug.message("orgDN from existing auth context: " + orgDN +
                                ", orgDN from query string: " + newOrgDN);
                    }
                    if (normOrgDN != null) {
                        if (!normOrgDN.equals(newOrgDN)) {
                            loginStatus.setStatus(LoginStatus.AUTH_RESET);
                            authContext.getLoginState().setErrorCode(AMAuthErrorCode.AUTH_ERROR);
                            setErrorMsgAndTemplate();
                            internalAuthError = true;
                            throw new AuthLoginException(BUNDLE_NAME, AMAuthErrorCode.AUTH_ERROR, null);
                        }
                    }
                }

            }
        }

        if (indexType == IndexType.COMPOSITE_ADVICE)  {
            /*
             * Configure login following COMPOSITE_ADVICE
             */

            debug.message("IndexType is COMPOSITE_ADVICE");
            // Set the Composite Advice in Login State after decoding
            String compositeAdvice = URLEncDec.decode(indexName);
            authContext.getLoginState().setCompositeAdvice(compositeAdvice);
            // if multiple modules are found then return
            // else continue with login process
            try {
                if (processCompositeAdvice(indexName, orgDN, clientType)) {
                    debug.message("multiple modules found");
                    return true;
                } else {
                    return false;
                }
            } catch (AuthException ae) {
                // no modules configured
                authContext.getLoginState().setErrorCode(ae.getErrorCode());
                authContext.getLoginState().logFailed(ae.getMessage());
                auditor.auditLoginFailure(authContext.getLoginState());
                setErrorMsgAndTemplate();
                loginStatus.setStatus(LoginStatus.AUTH_FAILED);
                throw new AuthLoginException(ae);
            }

        } else if (indexType == IndexType.LEVEL)  {
            /*
             * Configure login so that successful authentication achieve specified authentication LEVEL
             */

            debug.message("IndexType is level");
            // if multiple modules are found then return
            // else continue with login process
            try {
                if (processLevel(indexName, orgDN, clientType)) {
                    debug.message("multiple modules found");
                    return true;
                } else {
                    return false;
                }
            } catch (AuthException ae) {
                // no modules configured
                authContext.getLoginState().setErrorCode(ae.getErrorCode());
                authContext.getLoginState().logFailed(ae.getMessage());
                auditor.auditLoginFailure(authContext.getLoginState());
                setErrorMsgAndTemplate();
                loginStatus.setStatus(LoginStatus.AUTH_FAILED);
                throw new AuthLoginException(ae);
            }

        } else if (indexType == IndexType.USER) {
            /*
             * Configure login for specified user
             */

            debug.message("IndexType is user");
            // if user is not active throw exception
            // else continue with login
            boolean userValid = false;
            if (!authContext.getLoginState().ignoreProfile()) {
                userValid = validateUser(indexName);
            } else {
                ignoreProfile = true;
            }
            if ((!userValid) && (!ignoreProfile)) {
                debug.message("User is not active");
                authContext.getLoginState().logFailed(bundle.getString("userInactive"), "USERINACTIVE");
                auditor.auditLoginFailure(authContext.getLoginState(), USER_INACTIVE);
                /* The user based authentication errors should not be different
                 * for users who exist and who don't, which can lead to
                 * possibility of enumerating existing users.
                 * The AMAuthErrorCode.AUTH_LOGIN_FAILED error code is used for
                 * all user based authentication errors.
                 * Refer issue3278
                 */
                authContext.getLoginState().setErrorCode(AMAuthErrorCode.AUTH_LOGIN_FAILED);
                setErrorMsgAndTemplate();
                //destroySession();
                loginStatus.setStatus(LoginStatus.AUTH_FAILED);
                throw new AuthLoginException(BUNDLE_NAME, AMAuthErrorCode.AUTH_LOGIN_FAILED, null);
            } else if (ignoreProfile) {
                setAuthError(AMAuthErrorCode.AUTH_PROFILE_ERROR, "loginDenied");
                throw new AuthLoginException(BUNDLE_NAME, AMAuthErrorCode.AUTH_PROFILE_ERROR, null);
            } else {
                return false;
            }

        } else if (indexType == IndexType.MODULE_INSTANCE) {
            /*
             * Configure login for specified authentication module
             */

            // check if module exists in the allowed modules list
            debug.message("indexType is module");
            boolean instanceExists = authContext.getLoginState().getDomainAuthenticators().contains(indexName);
            if (!indexName.equals(ISAuthConstants.APPLICATION_MODULE) && !instanceExists) {
                debug.message("Module {} Not Found!!", indexName);
                authContext.getLoginState().setErrorCode(AMAuthErrorCode.AUTH_MODULE_NOT_FOUND);
                authContext.getLoginState().logFailed(bundle.getString("moduleNotFound"), "MODULENOTFOUND");
                auditor.auditLoginFailure(authContext.getLoginState(), MODULE_NOT_FOUND);
                setErrorMsgAndTemplate();
                loginStatus.setStatus(LoginStatus.AUTH_FAILED);
                throw new AuthLoginException(BUNDLE_NAME, AMAuthErrorCode.AUTH_MODULE_NOT_FOUND, null);
            } else {
                return false;
            }

        } else if (indexType == IndexType.ROLE) {
            /*
             * Configure login for specified role - No longer supported, throw an exception
             */

            debug.message("indexType is Role");
            if (authContext.getLoginState().ignoreProfile()) {
                setAuthError(AMAuthErrorCode.AUTH_TYPE_DENIED, "loginDenied");
                throw new AuthLoginException(BUNDLE_NAME, AMAuthErrorCode.AUTH_TYPE_DENIED, null);
            }
        }

        /*
         * IndexType not processed by this method
         */
        return false;
    }

    private void setAuthError(String errorCode, String resString) {
        authContext.getLoginState().setErrorCode(errorCode);
        setErrorMsgAndTemplate();
        authContext.getLoginState().logFailed(bundle.getString(resString));
        auditor.auditLoginFailure(authContext.getLoginState());
        loginStatus.setStatus(LoginStatus.AUTH_FAILED);
    }

    /**
     * Sets the failure URL and execute the post process login SPI.
     * for <code>internalAutherror</code> and if already executed
     * just skip this,
     */
    private void postProcessOnFail() {
        if (!internalAuthError && !processDone) {
            if (debug.messageEnabled()) {
                debug.message("postProcessOnFail ");
            }
            //setErrorMsgAndTemplate();
            authContext.getLoginState().postProcess(this, indexType, indexName, LoginState.PostProcessEvent.FAILURE);
            authContext.getLoginState().setFailureLoginURL(indexType, indexName);
            processDone = true;
        }
    }

    /**
     * Sets the success URL and execute the post process login
     * SPI. for <code>internalAutherror</code> and if already executed
     * just skip this.
     */
    private void postProcessOnSuccess() {
        if (!processDone) {
            if (debug.messageEnabled()) {
                debug.message("postProcessOnSuccess ");
            }
            authContext.getLoginState().postProcess(this, indexType, indexName, LoginState.PostProcessEvent.SUCCESS);
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
    private Set<String> getModuleFromAuthConfiguration(Set<String> moduleListSet, String orgDN) {
        Configuration config = Configuration.getConfiguration();
        if (configName == null) {
            configName = getConfigName(indexType, indexName, orgDN, authContext.getLoginState().getClientType());
        }
        AppConfigurationEntry[] moduleList = config.getAppConfigurationEntry(configName);
        if (debug.messageEnabled()) {
            debug.message("configName is : " + configName);
        }
        String moduleName;
        if (moduleList != null && moduleList.length > 0) {
            if (moduleList.length == 1) {
                moduleName = (String) moduleList[0].getOptions().get(ISAuthConstants.MODULE_INSTANCE_NAME);
                moduleListSet.add(moduleName);
            } else {
                for (AppConfigurationEntry moduleListEntry : moduleList) {
                    LoginModuleControlFlag controlFlag = moduleListEntry.getControlFlag();
                    moduleName = (String) moduleListEntry.getOptions().get(ISAuthConstants.MODULE_INSTANCE_NAME);
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
    private String getFailureModuleList(String orgDN) {

        String moduleList = ISAuthConstants.EMPTY_STRING;
        try {
            Set<String> failureModuleSet = authContext.getLoginState().getFailureModuleSet();
            Set<String> moduleSet = getModuleFromAuthConfiguration(failureModuleSet, orgDN);

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
    private boolean isControlFlagMatchFound(LoginModuleControlFlag flag) {
        return flag == LoginModuleControlFlag.REQUIRED || flag == LoginModuleControlFlag.REQUISITE;
    }

    /* Returns the successful list of modules names */
    private String getSuccessModuleString(String orgDN) {
        if (successModuleSet == null || successModuleSet.isEmpty()) {
            successModuleSet = getSuccessModuleSet(orgDN);
        }
        return getModuleString(successModuleSet);
    }

    /**
     * Checks if is pure JAAS mode
     * @return <code>true</code> if pure JAAS
     */
    public boolean isPureJAAS() {
        return jaasCheck;
    }

    private Configuration getConfiguration() {
        try {
            return Configuration.getConfiguration();
        } catch (SecurityException securityException) {
            return null;
        } catch (RuntimeException ex) {
            // kept to keep functional consistency with previous code which caught all exceptions
            return null;
        }
    }

    private void nullifyUsedVars() {
        configName = null; // jaas configuration name.
        successModuleSet = null;
        recdCallback = null;
    }

    /**
     * This thread is created when a pure JAAS module is found in the
     * authentication chain,  which used to be created in
     * <code>AMLoginContext</code>.
     */
    private class JAASLoginThread  extends Thread {

        private AMLoginContext amlc;

        /**
         * Creates <code>JAASLoginThread</code> object.
         *
         * @param amlc <code>AMLoginContext</code> in which the running method is
         *        defined.
         */
        JAASLoginThread(AMLoginContext amlc) {
            this.amlc = amlc;
        }

        /**
         * Run the thread task which is defined in <code>AMLoginContext</code>.
         */
        public void run() {
            amlc.runLogin();
        }
    }
}
