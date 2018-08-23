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
 * $Id: LoginState.java,v 1.57 2010/01/20 21:30:40 qcheng Exp $
 *
 * Portions Copyrighted 2010-2016 ForgeRock AS.
 * Portions Copyrighted 2016 Nomura Research Institute, Ltd.
 */

package com.sun.identity.authentication.service;

import static java.util.Collections.unmodifiableSet;
import static org.forgerock.openam.audit.AuditConstants.AuthenticationFailureReason.INVALID_REALM;
import static org.forgerock.openam.audit.AuditConstants.AuthenticationFailureReason.REALM_INACTIVE;
import static org.forgerock.openam.audit.AuditConstants.AuthenticationFailureReason.USER_NOT_FOUND;
import static org.forgerock.openam.utils.CollectionUtils.asSet;
import static org.forgerock.openam.utils.Time.newDate;

import java.net.InetAddress;
import java.security.AccessController;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.StringTokenizer;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.openam.authentication.service.DefaultSessionPropertyUpgrader;
import org.forgerock.openam.authentication.service.SessionPropertyUpgrader;
import org.forgerock.openam.authentication.service.SessionUpgradeHandler;
import org.forgerock.openam.authentication.service.activators.ForceAuthSessionActivator;
import org.forgerock.openam.identity.idm.IdentityUtils;
import org.forgerock.openam.ldap.LDAPUtils;
import org.forgerock.openam.session.SessionURL;
import org.forgerock.openam.session.service.SessionAccessManager;
import org.forgerock.openam.sso.providers.stateless.StatelessAdminRestriction;
import org.forgerock.openam.sso.providers.stateless.StatelessSession;
import org.forgerock.openam.utils.ClientUtils;
import org.forgerock.opendj.ldap.DN;
import org.forgerock.opendj.ldap.SearchScope;

import com.iplanet.am.sdk.AMException;
import com.iplanet.am.sdk.AMObject;
import com.iplanet.am.sdk.AMStoreConnection;
import com.iplanet.am.util.Misc;
import com.iplanet.am.util.SystemProperties;
import com.iplanet.dpro.session.SessionException;
import com.iplanet.dpro.session.SessionID;
import com.iplanet.dpro.session.service.AuthenticationSessionStore;
import com.iplanet.dpro.session.service.InternalSession;
import com.iplanet.dpro.session.service.SessionState;
import com.iplanet.dpro.session.service.SessionType;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.authentication.AuthContext;
import com.sun.identity.authentication.audit.AuthenticationProcessEventAuditor;
import com.sun.identity.authentication.client.ZeroPageLoginConfig;
import com.sun.identity.authentication.config.AMAuthConfigUtils;
import com.sun.identity.authentication.config.AMAuthenticationInstance;
import com.sun.identity.authentication.config.AMAuthenticationManager;
import com.sun.identity.authentication.config.AMConfigurationException;
import com.sun.identity.authentication.server.AuthContextLocal;
import com.sun.identity.authentication.spi.AMLoginModule;
import com.sun.identity.authentication.spi.AMPostAuthProcess;
import com.sun.identity.authentication.spi.AMPostAuthProcessInterface;
import com.sun.identity.authentication.spi.AuthenticationException;
import com.sun.identity.authentication.util.AMAuthUtils;
import com.sun.identity.authentication.util.ISAuthConstants;
import com.sun.identity.common.DNUtils;
import com.sun.identity.common.ISLocaleContext;
import com.sun.identity.common.admin.AdminInterfaceUtils;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.AMIdentityRepository;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdSearchControl;
import com.sun.identity.idm.IdSearchOpModifier;
import com.sun.identity.idm.IdSearchResults;
import com.sun.identity.idm.IdType;
import com.sun.identity.idm.IdUtils;
import com.sun.identity.log.LogConstants;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.session.util.SessionUtils;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.DateUtils;
import com.sun.identity.shared.datastruct.CollectionHelper;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.encode.Base64;
import com.sun.identity.sm.DNMapper;
import com.sun.identity.sm.OrganizationConfigManager;
import com.sun.identity.sm.SMSEntry;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceManager;

/**
 * This class maintains the User's login state information from the time user
 * requests for authentication till the time the user either logs out of the
 * OpenAM system or the session is destroyed by any privileged application of
 * the OpenAM system.
 */
public class LoginState {

    /* Define internal users
     * For these users we would allow authentication only at root realm
     * and require to be authenticated to configuration datastore.
     */
    public static final Set<String> INTERNAL_USERS = unmodifiableSet(asSet("amadmin", "dsameuser", "urlaccessagent"));

    private static final boolean URL_REWRITE_IN_PATH = SystemProperties.getAsBoolean(Constants.REWRITE_AS_PATH);
    private static final String NO_SESSION_QUERY_PARAM = "noSession";
    private static final Set<String> USER_ATTRIBUTES;
    private static final long AGENT_SESSION_IDLE_TIME;
    private static final SecureRandom SECURE_RANDOM;
    private static final Debug DEBUG = Debug.getInstance(ISAuthConstants.AUTH_BUNDLE_NAME);
    private static final List<String> SHARED_STATE_ATTRIBUTES = 
            Arrays.asList(ISAuthConstants.SHARED_STATE_PASSWORD, ISAuthConstants.SHARED_STATE_USERNAME);
    private static volatile List<SessionUpgradeHandler> sessionUpgradeHandlers = null;

    /**
     * Lazy initialisation holder to allow unit testing without loading the world.
     */
    private static class LazyConfig {
        private static final AuthD AUTHD = AuthD.getAuth();
        private static final SessionPropertyUpgrader SESSION_PROPERTY_UPGRADER = loadPropertyUpgrader();
    }

    static {

        Set<String> attrs = new HashSet<String>();
        attrs.add(ISAuthConstants.LOGIN_SUCCESS_URL);
        attrs.add(ISAuthConstants.LOGIN_FAILURE_URL);
        attrs.add(ISAuthConstants.USER_ALIAS_ATTR);
        attrs.add(ISAuthConstants.MAX_SESSION_TIME);
        attrs.add(ISAuthConstants.SESS_MAX_IDLE_TIME);
        attrs.add(ISAuthConstants.SESS_MAX_CACHING_TIME);
        attrs.add(ISAuthConstants.INETUSER_STATUS);
        attrs.add(ISAuthConstants.NSACCOUNT_LOCK);
        attrs.add(ISAuthConstants.PREFERRED_LOCALE);
        attrs.add(ISAuthConstants.LOGIN_STATUS);
        attrs.add(ISAuthConstants.ACCOUNT_LIFE);
        attrs.add(ISAuthConstants.USER_SUCCESS_URL);
        attrs.add(ISAuthConstants.USER_FAILURE_URL);
        attrs.add(ISAuthConstants.POST_LOGIN_PROCESS);
        USER_ATTRIBUTES = unmodifiableSet(attrs);

        // App session timeout is default to 0 => non-expiring
        long agSessIdleTime = SystemProperties.getAsLong(Constants.AGENT_SESSION_IDLE_TIME, 0L);
        long minAgentSessionIdleTime = 30L;
        AGENT_SESSION_IDLE_TIME = (agSessIdleTime > 0 && agSessIdleTime < minAgentSessionIdleTime)
                ? minAgentSessionIdleTime : agSessIdleTime;

        // Obtain the secureRandom instance
        try {
            SECURE_RANDOM = SecureRandom.getInstance("SHA1PRNG");
        } catch (NoSuchAlgorithmException ex) {
            DEBUG.error("LoginState.static() : LoginState : SecureRandom.getInstance() Failed", ex);
            throw new IllegalStateException("Unable to obtain SecureRandom", ex);
        }

    }

    private boolean mandatory2fa;
    private boolean userIDGeneratorEnabled;
    private String userIDGeneratorClassName;
    private boolean loginFailureLockoutMode = false;
    private boolean loginFailureLockoutStoreInDS = true;
    private String accountLife = null;
    private long loginFailureLockoutDuration = 0;
    private int loginFailureLockoutMultiplier = 0;
    private long loginFailureLockoutTime = 300000;
    private int loginFailureLockoutCount = 5;
    private String loginLockoutNotification = null;
    private String loginLockoutAttrName = null;
    private String loginLockoutAttrValue = null;
    private String invalidAttemptsDataAttrName = null;
    private int loginLockoutUserWarning = 3;
    private String failureTokenId = null;
    private Callback[] receivedCallbackInfo;
    private Callback[] prevCallback;
    private Callback[] submittedCallbackInfo;
    private final Map<String, Callback[]> callbacksPerState = new HashMap<String, Callback[]>();
    private SessionID sessionReference = null;
    private HttpServletRequest servletRequest;
    private HttpServletResponse servletResponse;
    private String orgName;
    private String userOrg;
    private String orgDN = null;
    private int loginStatus = LoginStatus.AUTH_IN_PROGRESS;
    private Map<String, String> requestHash;
    private boolean newRequest;  // new or existing request
    private Set<String> aliasAttrNames = null;
    private String userContainerDN = null;
    private String userNamingAttr = null;
    private boolean dynamicProfileCreation = false;
    private boolean ignoreUserProfile = false;
    private boolean createWithAlias = false;
    private Subject subject;
    private String token = null;
    private String userDN = null;
    private int maxSession;
    private int idleTime;
    private int cacheTime;
    private int authLevel = 0;
    private int moduleAuthLevel = Integer.MIN_VALUE;
    private String client = null;
    private String authMethName = "";
    private String pAuthMethName = null;
    private String queryOrg = null;

    private SessionID finalSessionId;
    private String activatedSessionTrackingId;

    private boolean cookieSupported = true;
    private boolean cookieSet = false;
    private boolean userEnabled = true;
    private AMIdentity amIdentityRole = null;
    private AuthContext.IndexType indexType;
    private String indexName = null;
    private AuthContext.IndexType prevIndexType = null;
    private Set<String> userAliasList = null;
    private String gotoOnFailURL = null;
    private String failureLoginURL = null;
    private String successLoginURL = null;
    private String moduleSuccessLoginURL = null;
    private String moduleFailureLoginURL = null;
    private String clientOrgSuccessLoginURL = null;
    private String defaultOrgSuccessLoginURL = null;
    private String clientOrgFailureLoginURL = null;
    private String defaultOrgFailureLoginURL = null;
    private final Map<String, String> requestMap = new HashMap<>();
    private Set<String> domainAuthenticators = null;
    private Set<String> moduleInstances = null;
    private boolean sessionUpgrade = false;
    private String loginURL = null;
    private long pageTimeOut = 60;
    private long lastCallbackSent = 0;
    private AMIdentity amIdentityUser = null;
    // error code
    private String errorCode = null;
    private String errorMessage = null;
    private String errorTemplate = null;
    private String moduleErrorTemplate = null;
    private String lockoutMsg = null;
    // timed out
    private boolean timedOut = false;
    private String principalList = null;
    private X509Certificate cert = null;
    private String defaultUserSuccessURL;
    private String clientUserSuccessURL;
    private String clientUserFailureURL;
    private String defaultUserFailureURL;
    private String clientSuccessRoleURL;
    private String defaultSuccessRoleURL;
    private String clientFailureRoleURL;
    private String defaultFailureRoleURL;
    private String userAuthConfig = "";
    private String orgAuthConfig = null;
    private String orgAdminAuthConfig = null;
    private String roleAuthConfig = null;
    private Set<String> orgPostLoginClassSet = Collections.emptySet();
    private Map<String, Set<String>> serviceAttributesMap = new HashMap<>();
    private String moduleErrorMessage = null;
    private String tempDefaultURL = null;
    private boolean isLocaleSet = false;
    private boolean cookieDetect = false;
    private Map<String, Object> userCreationAttributes = null;
    private Set<String> externalAliasList = null;
    private final Set<String> successModuleSet = new HashSet<String>();
    private final Set<String> failureModuleSet = new HashSet<String>();
    private String failureModuleList = ISAuthConstants.EMPTY_STRING;
    private String fqdnFailureLoginURL = null;
    private Map<String, String> moduleMap = null;
    private Map<String, Set<String>> roleAttributeMap = null;
    private Set<String> identityTypes = Collections.emptySet();
    private Set<String> userSessionMapping = Collections.emptySet();
    private AMIdentityRepository amIdRepo = null;
    private int compositeAdviceType;
    private String compositeAdvice;
    private String qualifiedOrgDN = null;
    // Variable indicating a request "forward" after
    // authentication success
    private boolean forwardSuccess = false;

    // Indicates Session is stateless
    public boolean stateless = false;

    /**
     * Indicates if orgnization is active
     */
    private boolean inetDomainStatus = true;
    /**
     * Default roles for user
     */
    private Set<String> defaultRoles = null;
    /**
     * Default auth level for each auth module
     */
    private String defaultAuthLevel = "0";
    private ZeroPageLoginConfig zeroPageLoginConfig;
    private SessionID oldSessionReference = null;
    private StatelessSession oldStatelessSession = null;
    private SSOToken oldSSOToken = null;
    private boolean forceAuth;
    private boolean cookieTimeToLiveEnabledFlag = false;
    private int cookieTimeToLive = 0;
    // Enable Module based Auth
    private boolean enableModuleBasedAuth = true;
    private ISLocaleContext localeContext = new ISLocaleContext();
    
    /**
     * The sharedState Map of the {@link AMLoginModule} and subclasses.
     */
    private Map<Object, Object> sharedState;

    /**
     * Stores the principals corresponding to the successful authentication modules within the current authentication
     * session.
     */
    private final Set<String> authenticatedPrincipals = new HashSet<String>();

    private final AuthenticationProcessEventAuditor auditor = InjectorHolder.getInstance(
            AuthenticationProcessEventAuditor.class);

    private final StatelessAdminRestriction restriction =
            InjectorHolder.getInstance(StatelessAdminRestriction.class);

    private final SessionAccessManager sessionAccessManager =
            InjectorHolder.getInstance(SessionAccessManager.class);

    private final AuthenticationSessionStore authenticationSessionStore =
            InjectorHolder.getInstance(AuthenticationSessionStore.class);

    /**
     * Attempts to load the configured session property upgrader class.
     */
    private static SessionPropertyUpgrader loadPropertyUpgrader() {
        String upgraderClass = SystemProperties.get(Constants.SESSION_UPGRADER_IMPL,
                Constants.DEFAULT_SESSION_UPGRADER_IMPL);

        SessionPropertyUpgrader upgrader = null;
        try {
            upgrader = Class.forName(upgraderClass).asSubclass(SessionPropertyUpgrader.class).newInstance();
            if (DEBUG.messageEnabled()) {
                DEBUG.message("SessionUpgrader implementation ('" + upgraderClass
                        + ") successfully loaded.");
            }
        } catch (Exception ex) {
            DEBUG.error("Unable to load the following Session Upgrader implementation: " +
                    upgraderClass + "\nFallbacking to DefaultSessionUpgrader", ex);
            upgrader = new DefaultSessionPropertyUpgrader();
        }

        return upgrader;
    }

    /**
     * Converts a byte array to a hex string.
     */
    private static String byteArrayToHexString(byte[] byteArray) {
        int readBytes = byteArray.length;
        StringBuilder hexData = new StringBuilder();
        int onebyte;
        for (int i = 0; i < readBytes; i++) {
            onebyte = ((0x000000ff & byteArray[i]) | 0xffffff00);
            hexData.append(Integer.toHexString(onebyte).substring(6));
        }
        return hexData.toString();
    }

    String getDefaultAuthLevel() {
        return defaultAuthLevel;
    }

    /**
     * Returns servlet request object.
     *
     * @return servlet request object.
     */
    public HttpServletRequest getHttpServletRequest() {
        return servletRequest;
    }

    /**
     * Sets servlet request.
     *
     * @param servletRequest Servlet request.
     */
    public void setHttpServletRequest(HttpServletRequest servletRequest) {
        this.servletRequest = servletRequest;
    }

    /**
     * Returns session, Returns null if session state is <code>INACTIVE</code>
     * or <code>DESTROYED</code>.
     *
     * @return session;
     */
    public InternalSession getSession() {
        if (null == sessionReference) {
            DEBUG.message("Session is null :{}", sessionReference);
            return null;
        }
        InternalSession session = getReferencedSession();
        if (session == null || session.getState() == SessionState.DESTROYED) {
            DEBUG.message("Session is DESTROYED :{}", session);
            return null;
        }
        return session;
    }

    /**
     * Sets the internal session for the request.
     *
     * @param sess Internal session for the request.
     */
    public void setSession(InternalSession sess) {
        if (sess != null) {
            this.sessionReference = sess.getSessionID();
            this.finalSessionId = sess.getID();
        } else {
            this.sessionReference = null;
            this.finalSessionId = null;
        }
    }

    /**
     * Sets the session id, independently of the session. Used by stateless session activation to update the
     * serialised session id while leaving the InternalSession null.
     *
     * @param sid the new session id to set.
     */
    void setSessionID(SessionID sid) {
        this.finalSessionId = sid;
    }

    /**
     * Sets the callbacks recieved and notify waiting thread.
     *
     * @param callback
     * @param amLoginContext
     */
    void setReceivedCallback(
            Callback[] callback,
            AMLoginContext amLoginContext) {
        synchronized (amLoginContext) {
            submittedCallbackInfo = null;
            receivedCallbackInfo = callback;
            prevCallback = callback;
            amLoginContext.notify();
        }
    }

    /**
     * Sets the callbacks recieved and notify waiting thread.
     * Used in non-jaas thread mode only.
     *
     * @param callback
     */
    public void setReceivedCallback_NoThread(Callback[] callback) {
        submittedCallbackInfo = null;
        receivedCallbackInfo = callback;
        prevCallback = callback;
    }

    /**
     * Sets the callbacks submitted by login module and notify waiting thread.
     *
     * @param callback
     * @param amLoginContext
     */
     void setSubmittedCallback(
            Callback[] callback,
            AMLoginContext amLoginContext) {
        synchronized (amLoginContext) {
            prevCallback = receivedCallbackInfo;
            receivedCallbackInfo = null;
            submittedCallbackInfo = callback;
            amLoginContext.notify();
        }
    }

    /**
     * Sets the callbacks submitted by login module and notify waiting thread.
     * Used in non-jaas thread mode only.
     *
     * @param callback
     */
    void setSubmittedCallback_NoThread(Callback[] callback) {
        prevCallback = receivedCallbackInfo;
        receivedCallbackInfo = null;
        submittedCallbackInfo = callback;
    }

    /**
     * Returns recieved callback info from loginmodule.
     *
     * @return recieved callback info from loginmodule.
     */
    public Callback[] getReceivedInfo() {
        return receivedCallbackInfo;
    }

    /**
     * Returns callbacks submitted by client.
     *
     * @return callbacks submitted by client.
     */
    public Callback[] getSubmittedInfo() {
        return submittedCallbackInfo;
    }

    /**
     * Returns the organization DN example <code>o=iplanet.com,o=isp</code>.
     *
     * @return the organization DN example <code>o=iplanet.com,o=isp</code>.
     */
    public String getOrgDN() {
        if (orgDN == null) {
            try {
                orgDN = LazyConfig.AUTHD.getOrgDN(userOrg);
            } catch (Exception e) {
                DEBUG.message("Error getting orgDN: ", e);
            }
        }
        return orgDN;
    }

    /**
     * Returns the organization name.
     *
     * @return the organization name.
     */
    public String getOrgName() {
        if (orgName == null) {
            orgName = DNMapper.orgNameToRealmName(getOrgDN());
        }
        return orgName;
    }

    /**
     * Returns the authentication login status.
     *
     * @return the authentication login status.
     */
    public int getLoginStatus() {
        return loginStatus;
    }

    /**
     * Sets the authentication login status.
     *
     * @param loginStatus authentication login status.
     */
    public synchronized void setLoginStatus(int loginStatus) {
        this.loginStatus = loginStatus;
    }

    /**
     * Sets the request parameters hash.
     * Side Effect: Also updates the requestMap at the same time.
     *
     * @param requestHash Request parameters hash.
     */
    public void setParamHash(Map<String, String> requestHash) {
        this.requestHash = requestHash;

        /* copy these parameters to HashMap */
        this.requestMap.putAll(requestHash);
    }

    /**
     * Sets the request type.
     *
     * @param newRequest <code>true</code> for new request type;
     *                    <code>false</code> for existing request type.
     */
    void setNewRequest(boolean newRequest) {
        this.newRequest = newRequest;
    }

    /**
     * Returns the request type.
     *
     * @return the request type.
     */
    boolean isNewRequest() {
        return newRequest;
    }

    /**
     * Returns <code>true</code> if dynamic profile is enabled.
     *
     * @return <code>true</code> if dynamic profile is enabled.
     */
    public boolean isDynamicProfileCreationEnabled() {
        return dynamicProfileCreation;
    }

    /**
     * Gets the external ID of the activated session.  See {@link com.sun.identity.shared.Constants#AM_CTX_ID}
     * @return the external session ID string.
     */
    public String getActivatedSessionTrackingId() {
        return activatedSessionTrackingId;
    }

    /**
     * Populates the organization profile.
     *
     * @throws AuthException
     */
    private void populateOrgProfile() throws AuthException {
        try {
            // get inetdomainstatus for the org
            // check if org is active
            inetDomainStatus = LazyConfig.AUTHD.getInetDomainStatus(getOrgDN());
            if (!inetDomainStatus) {
                // org inactive
                logFailed(AuthUtils.getErrorVal(AMAuthErrorCode.AUTH_ORG_INACTIVE,
                        AuthUtils.ERROR_MESSAGE), "ORGINACTIVE");
                auditor.auditLoginFailure(this, REALM_INACTIVE);
                throw new AuthException(AMAuthErrorCode.AUTH_ORG_INACTIVE, null);
            }
            // get handle to org config manager object to retrieve auth service
            // attributes.
            OrganizationConfigManager orgConfigMgr =
                    LazyConfig.AUTHD.getOrgConfigManager(getOrgDN());
            ServiceConfig svcConfig =
                    orgConfigMgr.getServiceConfig(ISAuthConstants.AUTH_SERVICE_NAME);

            Map<String, Set<String>> attrs = svcConfig.getAttributes();
            stateless = CollectionHelper.getBooleanMapAttr(attrs, ISAuthConstants.AUTH_STATELESS_SESSIONS, false);

            aliasAttrNames = attrs.get(ISAuthConstants.AUTH_ALIAS_ATTR);
            identityTypes = attrs.get(ISAuthConstants.AUTH_ID_TYPE_ATTR);
            userSessionMapping = attrs.get(ISAuthConstants.
                    USER_SESSION_MAPPING);

            userNamingAttr = CollectionHelper.getMapAttr(
                    attrs, ISAuthConstants.AUTH_NAMING_ATTR, "uid");

            defaultRoles = attrs.get(ISAuthConstants.AUTH_DEFAULT_ROLE);

            String tmp = CollectionHelper.getMapAttr(
                    attrs, ISAuthConstants.DYNAMIC_PROFILE);
            if (tmp.equalsIgnoreCase("true")) {
                dynamicProfileCreation = true;
            } else if (tmp.equalsIgnoreCase("ignore")) {
                ignoreUserProfile = true;
            } else if (tmp.equalsIgnoreCase("createAlias")) {
                createWithAlias = true;
                dynamicProfileCreation = true;
            }

            tmp = CollectionHelper.getMapAttr(attrs, Constants.ZERO_PAGE_LOGIN_ENABLED);
            boolean zplEnabled = Boolean.valueOf(tmp);

            Set<String> zplWhitelist = attrs.get(Constants.ZERO_PAGE_LOGIN_WHITELIST);
            if (zplWhitelist == null) {
                zplWhitelist = Collections.emptySet();
            }

            boolean allowZPLWithoutReferer = CollectionHelper.getBooleanMapAttr(attrs,
                    Constants.ZERO_PAGE_LOGIN_ALLOW_MISSING_REFERER, true);

            this.zeroPageLoginConfig = new ZeroPageLoginConfig(zplEnabled, zplWhitelist, allowZPLWithoutReferer);

            AMAuthenticationManager authManager =
                    new AMAuthenticationManager(LazyConfig.AUTHD.getSSOAuthSession(), getOrgDN());

            domainAuthenticators = authManager.getAllowedModuleNames();
            if (domainAuthenticators == null) {
                domainAuthenticators = Collections.emptySet();
            }

            defaultAuthLevel = CollectionHelper.getMapAttr(
                    attrs, ISAuthConstants.DEFAULT_AUTH_LEVEL, LazyConfig.AUTHD.defaultAuthLevel);

            localeContext.setOrgLocale(getOrgDN());

            Set<String> orgSuccessLoginURLSet = attrs.get(ISAuthConstants.LOGIN_SUCCESS_URL);

            if (orgSuccessLoginURLSet == null) {
                orgSuccessLoginURLSet = Collections.emptySet();
            }

            clientOrgSuccessLoginURL = getRedirectUrl(orgSuccessLoginURLSet);
            defaultOrgSuccessLoginURL = tempDefaultURL;

            Set<String> orgFailureLoginURLSet = attrs.get(ISAuthConstants.LOGIN_FAILURE_URL);
            if (orgFailureLoginURLSet == null) {
                orgFailureLoginURLSet = Collections.emptySet();
            }

            clientOrgFailureLoginURL = getRedirectUrl(orgFailureLoginURLSet);
            defaultOrgFailureLoginURL = tempDefaultURL;
            orgAuthConfig = CollectionHelper.getMapAttr(attrs, ISAuthConstants.AUTHCONFIG_ORG);
            orgAdminAuthConfig = CollectionHelper.getMapAttr(attrs, ISAuthConstants.AUTHCONFIG_ADMIN);
            orgPostLoginClassSet = attrs.get(ISAuthConstants.POST_LOGIN_PROCESS);
            if (orgPostLoginClassSet == null) {
                orgPostLoginClassSet = Collections.emptySet();
            }

            tmp = CollectionHelper.getMapAttr(attrs, ISAuthConstants.MODULE_BASED_AUTH);
            if (tmp != null) {
                if (tmp.equalsIgnoreCase("false")) {
                    enableModuleBasedAuth = false;
                }
            }


            // retrieve account locking specific attributes
            tmp = CollectionHelper.getMapAttr(attrs, ISAuthConstants.LOGIN_FAILURE_LOCKOUT);
            if (tmp != null) {
                if (tmp.equalsIgnoreCase("true")) {
                    setLoginFailureLockoutMode(true);
                }
            }
            tmp = CollectionHelper.getMapAttr(attrs, ISAuthConstants.LOGIN_FAILURE_STORE_IN_DS);
            if (tmp != null) {
                if (tmp.equalsIgnoreCase("false")) {
                    setLoginFailureLockoutStoreInDS(false);
                }
            }
            tmp = CollectionHelper.getMapAttr(attrs, ISAuthConstants.LOCKOUT_DURATION);
            if (tmp != null) {
                try {
                    setLoginFailureLockoutDuration(Long.parseLong(tmp));
                } catch (NumberFormatException e) {
                    DEBUG.error("auth-lockout-duration bad format.");
                }
                setLoginFailureLockoutDuration(getLoginFailureLockoutDuration() * 60 * 1000);
            }

            tmp = Misc.getMapAttr(attrs, ISAuthConstants.LOCKOUT_MULTIPLIER);
            if (tmp != null) {
                try {
                    setLoginFailureLockoutMultiplier(Integer.parseInt(tmp));
                } catch (NumberFormatException e) {
                    DEBUG.error("auth-lockout-multiplier bad format.");
                }
            }

            tmp = CollectionHelper.getMapAttr(attrs, ISAuthConstants.LOGIN_FAILURE_COUNT);
            if (tmp != null) {
                try {
                    setLoginFailureLockoutCount(Integer.parseInt(tmp));
                } catch (NumberFormatException e) {
                    DEBUG.error("auth-lockout-count bad format.");
                }
            }

            tmp = CollectionHelper.getMapAttr(attrs, ISAuthConstants.LOGIN_FAILURE_DURATION);
            if (tmp != null) {
                try {
                    setLoginFailureLockoutTime(Long.parseLong(tmp));
                } catch (NumberFormatException e) {
                    DEBUG.error("auth-login-failure-duration bad format.");
                }
                setLoginFailureLockoutTime(getLoginFailureLockoutTime() * 60 * 1000);
            }

            tmp = CollectionHelper.getMapAttr(attrs, ISAuthConstants.LOCKOUT_WARN_USER);
            if (tmp != null) {
                try {
                    setLoginLockoutUserWarning(Integer.parseInt(tmp));
                } catch (NumberFormatException e) {
                    DEBUG.error("auth-lockout-warn-user bad format.");
                }
            }

            setLoginLockoutNotification(CollectionHelper.getMapAttr(attrs, ISAuthConstants.LOCKOUT_EMAIL));

            tmp = CollectionHelper.getMapAttr(attrs, ISAuthConstants.USERNAME_GENERATOR);
            if (tmp != null) {
                setUserIDGeneratorEnabled(Boolean.valueOf(tmp));
            }

            tmp = CollectionHelper.getMapAttr(attrs, ISAuthConstants.TWO_FACTOR_AUTH_MANDATORY);
            if (tmp != null) {
                setMandatory2FAValue(tmp);
            }

            setUserIDGeneratorClassName(CollectionHelper.getMapAttr(attrs, ISAuthConstants.USERNAME_GENERATOR_CLASS));
            tmp = CollectionHelper.getMapAttr(attrs, ISAuthConstants.LOCKOUT_ATTR_NAME);
            setLoginLockoutAttrName(tmp);

            tmp = CollectionHelper.getMapAttr(attrs, ISAuthConstants.LOCKOUT_ATTR_VALUE);
            setLoginLockoutAttrValue(tmp);

            setInvalidAttemptsDataAttrName(CollectionHelper.getMapAttr(
                    attrs, ISAuthConstants.INVALID_ATTEMPTS_DATA_ATTR_NAME));

            if (DEBUG.messageEnabled()) {
                DEBUG.message("Getting Org Profile: " + orgDN
                        + "\nlocale->" + localeContext.getLocale()
                        + "\ncharset->" + localeContext.getMIMECharset()
                        + "\ndynamicProfileCreation->" + dynamicProfileCreation
                        + "\ndefaultAuthLevel->" + defaultAuthLevel
                        + "\norgSucessLoginURLSet->" + orgSuccessLoginURLSet
                        + "\norgFailureLoginURLSet->" + orgFailureLoginURLSet
                        + "\nclientSuccessLoginURL ->" + clientOrgSuccessLoginURL
                        + "\ndefaultSuccessLoginURL ->" + defaultOrgSuccessLoginURL
                        + "\norgPostLoginClassSet ->" + orgPostLoginClassSet
                        + "\norgAuthConfig ->" + orgAuthConfig
                        + "\norgAdminAuthConfig ->" + orgAdminAuthConfig
                        + "\nclientFailureLoginURL ->" + clientOrgFailureLoginURL
                        + "\ndefaultFailureLoginURL ->" + defaultOrgFailureLoginURL
                        + "\nenableModuleBasedAuth ->" + enableModuleBasedAuth
                        + "\nloginFailureLockoutMode->" + isLoginFailureLockoutMode()
                        + "\nloginFailureLockoutStoreInDS->"
                        + isLoginFailureLockoutStoreInDS()
                        + "\nloginFailureLockoutCount->" + getLoginFailureLockoutCount()
                        + "\nloginFailureLockoutTime->" + getLoginFailureLockoutTime()
                        + "\nloginLockoutUserWarning->" + getLoginLockoutUserWarning()
                        + "\nloginLockoutNotification->" + getLoginLockoutNotification()
                        + "\ninvalidAttemptsDataAttrName->" + getInvalidAttemptsDataAttrName()
                        + "\nzeroPageLoginConfig->" + zeroPageLoginConfig
                        + "\nidentityTypes->" + identityTypes
                        + "\naliasAttrNames ->" + aliasAttrNames);
            }
        } catch (AuthException ae) {
            DEBUG.error("Error in populateOrgProfile", ae);
            throw new AuthException(ae);
        } catch (Exception ex) {
            DEBUG.error("Error in populateOrgProfile", ex);
            throw new AuthException(AMAuthErrorCode.AUTH_ERROR, null);
        }
    }

    /**
     * Returns the authenticated subject.
     *
     * @return Authenticated subject
     */
    public Subject getSubject() {
        return subject;
    }

    /**
     * Sets the authenticated subject.
     *
     * @param subject Authenticated subject.
     */
    public void setSubject(Subject subject) {
        this.subject = subject;
    }

    /**
     * Returns session idle time.
     *
     * @return session idle time.
     */
    public int getIdleTime() {
        return idleTime;
    }

    public int getMaxSession() {
        return maxSession;
    }
    /**
     * Returns session cache time.
     *
     * @return session cache time.
     */
    public int getCacheTime() {
        return cacheTime;
    }

    /**
     * Returns user DN.
     *
     * @return user DN.
     */
    public String getUserDN() {
        if (DEBUG.messageEnabled()) {
            DEBUG.message("getUserDN: " + userDN);
        }
        return userDN;
    }

    /**
     * Returns authentication level.
     *
     * @return authentication level.
     */
    public int getAuthLevel() {
        /* for AMLoginModule */
        /* It is not a clean way to call setAuthLevel() in
         * this method. To make reference to authLevel(like
         * in sessionUpgrade()), make sure setAuthLevel() has
         * been called before, NOT getAuthLevel() ! or it will
         * return zero.
         */
        return authLevel;
    }

    /**
     * Sets the authentication level.
     * checks if <code>moduleAuthLevel</code> is set and if
     * it is greater then the authentications level then
     * <code>moduleAuthLevel</code> will be the set level.
     *
     * @param authLevel Authentication Level.
     */
    public void setAuthLevel(String authLevel) {
        // check if module Level is set and is greater
        // then authenticated modules level
        if (authLevel == null) {
            this.authLevel = 0;
        } else {
            try {
                this.authLevel = Integer.parseInt(authLevel);
            } catch (NumberFormatException e) {
                this.authLevel = 0;
            }
        }

        if (this.authLevel < moduleAuthLevel) {
            this.authLevel = moduleAuthLevel;
        }

        if (DEBUG.messageEnabled()) {
            DEBUG.message("AuthLevel is set to : " + this.authLevel);
        }
    }

    /**
     * Returns the client address.
     *
     * @return the client address.
     */
    public String getClient() {
        if (client != null) {
            return client;
        }
        String clientHost = "";
        try {
            String cli = null;
            if (requestHash != null) {
                cli = (String) requestHash.get("client");
            }
            if (DEBUG.messageEnabled()) {
                DEBUG.message("getClient : servletRequest is : " + servletRequest);
                DEBUG.message("getClient : cli is : " + cli);
            }
            if (cli == null || cli.length() == 0) {
                if (servletRequest != null) {
                    clientHost = ClientUtils.getClientIPAddress(servletRequest);
                } else {
                    InetAddress localHost = InetAddress.getLocalHost();
                    clientHost = localHost.getHostAddress();
                }
            }
        } catch (Exception e) {
            if (DEBUG.messageEnabled()) {
                DEBUG.message("Error getting client Type ", e);
            }
        }

        if (DEBUG.messageEnabled()) {
            DEBUG.message("Client is : " + clientHost);
        }
        client = clientHost;
        return clientHost;
    }

    /**
     * Sets the client address.
     *
     * @param remoteAddr Client address.
     */
    public void setClient(String remoteAddr) {
        client = remoteAddr;
    }

    /**
     * convert a token to DN
     * FOR BACKWARD COMPATIBILITY SUPPORT
     *
     * @param token0 <code>SSOToken</code> ID has user principal
     * @return DN for user principal
     */
    private String tokenToDN(String token0) {
        try {
            String token = token0.toLowerCase();
            int pipe = token.indexOf("|");
            if (pipe != -1) {
                token = token.substring(0, pipe);
            }

            // Return if module returns the token in the form of DN
            if (DN.valueOf(token).isInScopeOf(getOrgDN(), SearchScope.WHOLE_SUBTREE)) {
                return token;
            }

            if (LazyConfig.AUTHD.isSuperAdmin(token)) {
                return token;
            }

            // check if Application module user
            // Application module user starts with
            // amService-
            String applicationUser =
                    ISAuthConstants.APPLICATION_USER_PREFIX.toLowerCase();
            if (token.startsWith(applicationUser)) {
                return "cn=" + token + ",ou=DSAME Users," +
                        SMSEntry.getRootSuffix();
            }

            String id = DNUtils.DNtoName(token);

            String userDN = userNamingAttr + "=" + id + "," + userContainerDN;
            if (DEBUG.messageEnabled()) {
                DEBUG.message("token=" + token0 + ", id=" + id +
                        ", DN=" + userDN);
            }
            return userDN;
        } catch (Exception e) {
            DEBUG.error("tokenToDN : " + e.getMessage());
            return token0;
        }
    }

    /**
     * Returns the client type.
     *
     * @return the client type.
     */
    public String getClientType() {
        return (servletRequest != null) ?
                AuthUtils.getClientType(servletRequest) : AuthUtils.getDefaultClientType();
    }

    /**
     * Activates session on successful authentication.
     * <p/>
     * Unless the noSession query parameter was set on the request and then in that case no new permanent session is
     * activated and <code>true</code>.
     *
     * @param subject
     * @return <code>true</code> if user session is activated successfully, <code>false if failed to activated</code>
     * or <code>true</code> if the noSession parameter is set to true.
     */
    public boolean activateSession(Subject subject) throws AuthException {
        try {
            if (DEBUG.messageEnabled()) {
                DEBUG.message("activateSession - Token is : " + token);
                DEBUG.message("activateSession - userDN is : " + userDN);
            }

            if (indexType != null || indexName != null) {
                setSuccessLoginURL(indexType, indexName);
            } else {
                setSuccessLoginURL(AuthContext.IndexType.SERVICE, getAuthConfigName(indexType, indexName));
            }

            InternalSession internalSession = getReferencedSession();
            
            final SessionActivator sa=getSessionActivator();
            DEBUG.message("activate before isSessionUpgrade={} forceAuth={} getSessionActivator={} oldSessionReference={} getOldSession={} sessionReference={} getSession={} finalSessionId={}" , 
            		isSessionUpgrade(),forceAuth,sa.getClass().getSimpleName(),oldSessionReference,getOldSession(),sessionReference,getSession(),finalSessionId);
            
            final boolean isSessionActivated = sa.activateSession(this, AuthD.getSessionService(),internalSession, subject);
            
            DEBUG.message("activate after isSessionUpgrade={} forceAuth={} getSessionActivator={} oldSessionReference={} getOldSession={} sessionReference={} getSession={} finalSessionId={}" , 
            		isSessionUpgrade(),forceAuth,sa.getClass().getSimpleName(),oldSessionReference,getOldSession(),sessionReference,getSession(),finalSessionId);
            
            if (isSessionActivated) {
                this.activatedSessionTrackingId = internalSession.getProperty(Constants.AM_CTX_ID);
            }
            if (sessionUpgrade && !getForceFlag() && isSessionActivated && oldStatelessSession == null) {
                invokeSessionUpgradeHandlers();
            }

            return isSessionActivated;
        } catch (AuthException ae) {
            DEBUG.error("Error setting session properties: ", ae);
            throw ae;
        } catch (Exception e) {
            DEBUG.error("Error activating session: ", e);
            throw new AuthException("sessionActivationFailed", null);
        }
    }

    private SessionActivator getSessionActivator() {
        if (isNoSession()) {
            return NoSessionActivator.INSTANCE;
        }

        if (restriction.isRestricted(getUserDN())) {
            DEBUG.message("Using stateful session activation for super admin");
            return DefaultSessionActivator.INSTANCE;
        }

        if (stateless) {
            return StatelessSessionActivator.INSTANCE;
        }

        if (getForceFlag()) {
            return ForceAuthSessionActivator.getInstance();
        }

        return DefaultSessionActivator.INSTANCE;
    }

    void setOldStatelessSession(StatelessSession session) {
        this.oldStatelessSession = session;
    }

    /**
     * Populates session with properties.
     *
     * @param session
     * @throws AuthException
     */
    void setSessionProperties(InternalSession session) throws AuthException {
        if (DEBUG.messageEnabled()) {
            DEBUG.message("LoginState getSession = " +
                    session + " \nrequest token = " + token);
        }

        if (token == null) {
            throw new AuthException(AMAuthErrorCode.AUTH_ERROR, null);
        }

        String cookieSupport = (cookieSupported) ? "true" : "false";

        // for user based DN is already set
        if (userDN == null) {
            userDN = getUserDN(amIdentityUser);
        }

        AMIdentity newAMIdentity = null;
        String oldUserDN = null;
        String oldAuthenticationModuleInstanceName = null;
        AMIdentity oldAMIdentity = null;
        InternalSession oldSession = getReferencedOldSession();
        if (oldSession != null || oldStatelessSession != null) {
            if (oldSession != null) {
                oldUserDN = oldSession.getProperty(ISAuthConstants.PRINCIPAL);
                oldAuthenticationModuleInstanceName = oldSession.getProperty(ISAuthConstants.AUTH_TYPE);
            } else {
                try {
                    oldUserDN = oldStatelessSession.getProperty(ISAuthConstants.PRINCIPAL);
                    oldAuthenticationModuleInstanceName = oldStatelessSession.getProperty(ISAuthConstants.AUTH_TYPE);
                } catch (SessionException e) {
                    throw new AuthException(AMAuthErrorCode.SESSION_UPGRADE_FAILED, null);
                }
            }

            if (!ignoreUserProfile) {
                newAMIdentity = LazyConfig.AUTHD.getIdentity(IdType.USER, userDN, getOrgDN());
                oldAMIdentity = LazyConfig.AUTHD.getIdentity(IdType.USER, oldUserDN, getOrgDN());
                if (DEBUG.messageEnabled()) {
                    DEBUG.message("LoginState.setSessionProperties()" +
                            " newAMIdentity is: " + newAMIdentity);
                    DEBUG.message("LoginState.setSessionProperties()" +
                            " oldAMIdentity is: " + oldAMIdentity);
                }
            }
        }

        if (DEBUG.messageEnabled()) {
            DEBUG.message("LoginState.setSessionProperties()" +
                    " userDN is: " + userDN);
            DEBUG.message("LoginState.setSessionProperties()" +
                    " oldUserDN is: " + oldUserDN);
            DEBUG.message("LoginState.setSessionProperties()" +
                    " sessionUpgrade is: " + sessionUpgrade);
        }

        if (sessionUpgrade) {
            String oldAuthenticationModuleClassName = null;
            if ((oldAuthenticationModuleInstanceName != null) &&
                    (!oldAuthenticationModuleInstanceName.contains("|"))) {
                try {
                    SSOToken adminToken =
                            AccessController.doPrivileged(
                                    AdminTokenAction.getInstance());
                    AMAuthenticationManager authManager =
                            new AMAuthenticationManager(adminToken, getOrgName());
                    AMAuthenticationInstance authInstance =
                            authManager.getAuthenticationInstance(
                                    oldAuthenticationModuleInstanceName);
                    if (authInstance!=null)
                    	oldAuthenticationModuleClassName = authInstance.getType();
                } catch (AMConfigurationException ace) {
                    if (DEBUG.messageEnabled()) {
                        DEBUG.message("LoginState.setSessionProperties()"
                                + ":Unable to create AMAuthenticationManager"
                                + "Instance:"
                                + ace.getMessage());
                    }
                    throw new AuthException(ace);
                }
            }

            if ("Anonymous".equalsIgnoreCase(oldAuthenticationModuleClassName)) {
                sessionUpgrade();
            } else if (!ignoreUserProfile) {
                if ((oldAMIdentity != null) && oldAMIdentity.equals(newAMIdentity)) {
                    sessionUpgrade();
                } else {
                    if (DEBUG.messageEnabled()) {
                        DEBUG.message("LoginState.setSessionProperties()" +
                                "Resetting session upgrade to false " +
                                "since oldAMIdentity and newAMIdentity doesn't match");
                    }
                    throw new AuthException(
                            AMAuthErrorCode.SESSION_UPGRADE_FAILED, null);
                }
            } else {
                if ((oldUserDN != null) && (DNUtils.normalizeDN(userDN)).equals(DNUtils.normalizeDN(oldUserDN))) {
                    sessionUpgrade();
                } else {
                    if (DEBUG.messageEnabled()) {
                        DEBUG.message("LoginState.setSessionProperties()" +
                                "Resetting session upgrade to false " +
                                "since Old UserDN and New UserDN doesn't match");
                    }
                    throw new AuthException(
                            AMAuthErrorCode.SESSION_UPGRADE_FAILED, null);
                }
            }
        }

        if (getForceFlag() && sessionUpgrade) {
            session = oldSession;
        }

        Date authInstantDate = newDate();
        String authInstant = DateUtils.toUTCDateFormat(authInstantDate);

        String moduleAuthTime = null;
        if (sessionUpgrade) {
            try {

                if (oldStatelessSession != null) {
                    oldSSOToken = SSOTokenManager.getInstance().createSSOToken(oldStatelessSession.getID().toString());
                } else {
                    oldSSOToken = SSOTokenManager.getInstance().createSSOToken(oldSession.getID().toString());
                }
            } catch (SSOException ssoExp) {
                DEBUG.error("LoginState.setSessionProperties: Cannot get "
                        + "oldSSOToken.");
            }
            Map<String, String> moduleTimeMap = null;
            if (oldSSOToken != null) {
                moduleTimeMap = AMAuthUtils.getModuleAuthTimeMap(oldSSOToken);
            }
            if (moduleTimeMap == null) {
                moduleTimeMap = new HashMap<String, String>();
            }
            StringTokenizer tokenizer = new StringTokenizer(authMethName,
                    ISAuthConstants.PIPE_SEPARATOR);
            while (tokenizer.hasMoreTokens()) {
                String moduleName = tokenizer.nextToken();
                moduleTimeMap.put(moduleName, authInstant);
            }
            boolean firstElement = true;
            for (Map.Entry<String, String> entry : moduleTimeMap.entrySet()) {
                String moduleName = entry.getKey();
                String authTime = entry.getValue();
                StringBuilder sb = new StringBuilder();
                if (!firstElement) {
                    sb.append(ISAuthConstants.PIPE_SEPARATOR);
                }
                firstElement = false;
                if (moduleAuthTime == null) {
                    moduleAuthTime = (sb.append(moduleName).append(
                            "+").append(authTime)).toString();
                } else {
                    moduleAuthTime += sb.append(moduleName).append(
                            "+").append(authTime);
                }
            }
        }

        //Sets the User profile option used, in session.
        String userProfile = ISAuthConstants.REQUIRED;
        if (dynamicProfileCreation) {
            userProfile = ISAuthConstants.CREATE;
        } else if (ignoreUserProfile) {
            userProfile = ISAuthConstants.IGNORE;
        } else if (createWithAlias) {
            userProfile = ISAuthConstants.CREATE_WITH_ALIAS;
        }
        session.putProperty(ISAuthConstants.USER_PROFILE, userProfile);

        String defaultLoginURL = null;
        HttpServletRequest request = getHttpServletRequest();
        if (request != null) {
            loginURL = AuthUtils.constructLoginURL(request);
            int questionMark = loginURL.indexOf("?");
            defaultLoginURL = loginURL;
            if (questionMark != -1) {
                defaultLoginURL = loginURL.substring(0, questionMark);
            }
            session.putProperty(ISAuthConstants.LOGIN_URL, defaultLoginURL);
            session.putProperty(ISAuthConstants.FULL_LOGIN_URL, loginURL);
        }

        String sessionSuccessURL = LazyConfig.AUTHD.processURL(successLoginURL, servletRequest);
        sessionSuccessURL = encodeURL(sessionSuccessURL, true);
        if (sessionSuccessURL != null) {
            session.putProperty(ISAuthConstants.SUCCESS_URL, sessionSuccessURL);
        }

        // Get the universal ID
        String univId = null;
        if (amIdentityUser != null) {
            univId = IdUtils.getUniversalId(amIdentityUser);
        }

        String userId = DNUtils.DNtoName(userDN);
        if (DEBUG.messageEnabled()) {
            DEBUG.message(
                    "setSessionProperties Principal = " + userDN + "\n" +
                            "UserId = " + token + "\n" +
                            "client = " + getClient() + "\n" +
                            "Organization = " + orgDN + "\n" +
                            "locale = " + localeContext.getLocale() + "\n" +
                            "charset = " + localeContext.getMIMECharset() + "\n" +
                            "idleTime = " + idleTime + "\n" +
                            "cacheTime = " + cacheTime + "\n" +
                            "maxSession = " + maxSession + "\n" +
                            "AuthLevel = " + authLevel + "\n" +
                            "AuthType = " + authMethName + "\n" +
                            "Subject = " + subject.toString() + "\n" +
                            "UniversalId = " + univId + "\n" +
                            "cookieSupport = " + cookieSupport + "\n" +
                            "principals = " + principalList + "\n" +
                            "defaultLoginURL = " + defaultLoginURL + "\n" +
                            "successURL = " + sessionSuccessURL + "\n" +
                            "IndexType = " + indexType + "\n" +
                            "UserProfile = " + userProfile + "\n" +
                            "AuthInstant = " + authInstant + "\n" +
                            "ModuleAuthTime = " + moduleAuthTime);
        }

        try {
            if ((isApplicationModule(authMethName) &&
                    (LazyConfig.AUTHD.isSuperUser(userDN) || LazyConfig.AUTHD.isSpecialUser(userDN)))
                    || isAgent(amIdentityUser)) {

                session.setClientID(token);
                session.setType(SessionType.APPLICATION);
                if (isAgent(amIdentityUser) && AGENT_SESSION_IDLE_TIME > 0) {
                    if (DEBUG.messageEnabled()) {
                        DEBUG.message("setSessionProperties for agent " +
                                userDN + " with idletimeout to " +
                                AGENT_SESSION_IDLE_TIME);
                    }
                    session.setMaxSessionTime(Long.MAX_VALUE / 60);
                    session.setMaxIdleTime(AGENT_SESSION_IDLE_TIME);
                    session.setMaxCachingTime(AGENT_SESSION_IDLE_TIME);
                } else {
                    if (DEBUG.messageEnabled()) {
                        DEBUG.message("setSessionProperties for non-expiring session");
                    }
                    session.setNonExpiring();
                }
            } else {
                DEBUG.message("request: in putProperty stuff");
                session.setClientID(userDN);
                session.setType(SessionType.USER);
                session.setMaxSessionTime(maxSession);
                session.setMaxIdleTime(idleTime);
                session.setMaxCachingTime(cacheTime);
            }

            session.setClientDomain(getOrgDN());
            if ((client = getClient()) != null) {
                session.putProperty(ISAuthConstants.HOST, client);
            }
            if (!sessionUpgrade) {
                session.putProperty(ISAuthConstants.AUTH_LEVEL,
                        Integer.toString(authLevel));
                session.putProperty(ISAuthConstants.AUTH_TYPE, authMethName);
            }
            session.putProperty(ISAuthConstants.PRINCIPAL, userDN);

            if (userId == null && userDN != null) {
                DN dnObj = DN.valueOf(userDN);
                if (dnObj.size() > 0) {
                    userId = LDAPUtils.rdnValueFromDn(dnObj);
                }
            }
            session.putProperty(ISAuthConstants.USER_ID, userId);
            session.putProperty(ISAuthConstants.USER_TOKEN, token);
            session.putProperty(ISAuthConstants.ORGANIZATION, getOrgDN());
            session.putProperty(ISAuthConstants.LOCALE,
                    localeContext.getLocale().toString());
            session.putProperty(ISAuthConstants.CHARSET,
                    localeContext.getMIMECharset());
            session.putProperty(ISAuthConstants.CLIENT_TYPE, getClientType());
            session.putProperty(ISAuthConstants.COOKIE_SUPPORT_PROPERTY,
                    cookieSupport);
            session.putProperty(ISAuthConstants.AUTH_INSTANT, authInstant);
            if ((moduleAuthTime != null) && (moduleAuthTime.length() != 0)) {
                session.putProperty(ISAuthConstants.MODULE_AUTH_TIME,
                        moduleAuthTime);
            }
            if (principalList != null) {
                session.putProperty(ISAuthConstants.PRINCIPALS, principalList);
            }
            if (indexType != null) {
                session.putProperty(ISAuthConstants.INDEX_TYPE,
                        indexType.toString());
            }

            if (univId != null) {
                session.putProperty(Constants.UNIVERSAL_IDENTIFIER, univId);
            } else if (userDN != null) {
                session.putProperty(Constants.UNIVERSAL_IDENTIFIER, userDN);
            }
            if ((indexType == AuthContext.IndexType.ROLE) &&
                    (indexName != null)
                    ) {
                if (!sessionUpgrade) {
                    session.putProperty(ISAuthConstants.ROLE, indexName);
                }
            }

            if (!sessionUpgrade) {
                String finalAuthConfig = getAuthConfigName(indexType, indexName);
                if ((finalAuthConfig != null) && (
                        finalAuthConfig.length() != 0)) {
                    session.putProperty(ISAuthConstants.SERVICE,
                            finalAuthConfig);
                }
            }
            if ((userSessionMapping != null) &&
                    !(userSessionMapping.isEmpty()) && !ignoreUserProfile) {
                for (final String mapping : userSessionMapping) {
                    if ((mapping != null) && (mapping.length() != 0)) {
                        StringTokenizer tokenizer = new StringTokenizer(
                                mapping, "|");
                        String userAttribute = null;
                        String sessionAttribute = null;
                        if (tokenizer.hasMoreTokens()) {
                            userAttribute = tokenizer.nextToken();
                        }
                        if (tokenizer.hasMoreTokens()) {
                            sessionAttribute = tokenizer.nextToken();
                        }
                        if ((userAttribute != null) &&
                                (userAttribute.length() != 0)) {
                            Set userAttrValueSet = amIdentityUser.getAttribute(
                                    userAttribute);
                            if ((userAttrValueSet != null) &&
                                    !(userAttrValueSet.isEmpty())) {
                                Iterator valueIter = userAttrValueSet.
                                        iterator();
                                StringBuilder strBuffValues = new StringBuilder();
                                while (valueIter.hasNext()) {
                                    String userAttrValue = (String)
                                            valueIter.next();
                                    if (strBuffValues.length() == 0) {
                                        strBuffValues.append(userAttrValue);
                                    } else {
                                        strBuffValues.append("|").append
                                                (userAttrValue);
                                    }
                                }
                                if (sessionAttribute != null) {
                                    session.putProperty(
                                            Constants.AM_PROTECTED_PROPERTY_PREFIX
                                                    + "." + sessionAttribute,
                                            strBuffValues.toString());
                                } else {
                                    session.putProperty(
                                            Constants.AM_PROTECTED_PROPERTY_PREFIX
                                                    + "." + userAttribute,
                                            strBuffValues.toString());
                                }
                            }
                        }
                    }
                }
            }

            // Set Attribute Map for Authentication module
            AuthenticationPrincipalDataRetriever principalDataRetriever =
                    AuthenticationPrincipalDataRetrieverFactory.
                            getPrincipalDataRetriever();
            if (principalDataRetriever != null) {
                Map<String, String> attrMap = principalDataRetriever.getAttrMapForAuthenticationModule(subject);
                if (attrMap != null && !attrMap.isEmpty()) {
                    for (Map.Entry<String, String> entry : attrMap.entrySet()) {
                        String attrName = entry.getKey();
                        String attrValue = entry.getValue();
                        session.putProperty(attrName, attrValue);
                        if (DEBUG.messageEnabled()) {
                            DEBUG.message("AttrMap for SAML : " + attrName + " , " + attrValue);
                        }
                    }
                }
            }

        } catch (Exception e) {
            DEBUG.error("Exception in setSession ", e);
            throw new AuthException(e);
        }
    }

    /**
     * Returns the <code>inetDomainStatus</code>.
     *
     * @return <code>inetDomainStatus</code>.
     */
    public boolean getInetDomainStatus() {
        return inetDomainStatus;
    }

    /**
     * Returns the query Organization.
     *
     * @return Query Organization.
     */
    String getQueryOrg() {
        return queryOrg;
    }

    /**
     * Sets the query organization.
     *
     * @param queryOrg Query organization.
     */
    void setQueryOrg(String queryOrg) {
        this.queryOrg = queryOrg;
    }

    /**
     * Returns locale.
     *
     * @return locale.
     */
    public String getLocale() {
        if (!isLocaleSet) {
            return SystemProperties.get(Constants.AM_LOCALE);
        } else {
            return localeContext.getLocale().toString();
        }
    }

    /**
     * Sets locale
     *
     * @param locale locale setting
     */
    public void setLocale(String locale) {
        localeContext.setUserLocale(locale);
        isLocaleSet = true;
    }

    /* destroy session */
    void destroySession() {
        if (sessionReference != null) {
            AuthUtils.removeAuthContext(finalSessionId);
            LazyConfig.AUTHD.destroySession(finalSessionId);
            finalSessionId = null;
            sessionReference = null;
        }
    }

    /**
     * Returns Session ID.
     *
     * @return Session ID.
     */
    public SessionID getSid() {
        return finalSessionId;
    }

    public boolean getForceFlag() {
        return forceAuth && oldSessionReference!=null;
    }

    public void setForceAuth(boolean force) {
        forceAuth = force;
    }

    /**
     * Enables AM session cookie time to live
     *
     * @param flag if <code>true</code> enables AM session cookie time to live,
     *             otherwise disables AM session cookie time to live
     */
    public void enableCookieTimeToLive(boolean flag) {
        cookieTimeToLiveEnabledFlag = flag;
        if (DEBUG.messageEnabled()) {
            DEBUG.message("LoginState.enableCookieTimeToLive(): enable=" + cookieTimeToLiveEnabledFlag);
        }
    }

    /**
     * Checks whether AM session cookie time to live is enabled
     *
     * @return <code>true</code> if AM session cookie time to live
     * is enabled, otherwise returns <code>false</code>
     */
    public boolean isCookieTimeToLiveEnabled() {
        if (DEBUG.messageEnabled()) {
            DEBUG.message("LoginState.isCookieTimeToLiveEnabled(): enabled=" + cookieTimeToLiveEnabledFlag);
        }
        return cookieTimeToLiveEnabledFlag;
    }

    /**
     * Returns AM session cookie time to live
     *
     * @return AM session cookie time to live in seconds
     */
    public int getCookieTimeToLive() {
        if (DEBUG.messageEnabled()) {
            DEBUG.message("LoginState.getCookieTimeToLive(): cookieTimeToLive=" + cookieTimeToLive);
        }
        return cookieTimeToLive;
    }

    /**
     * Sets AM session cookie time to live
     *
     * @param timeToLive AM session cookie time to live in seconds
     */
    public void setCookieTimeToLive(int timeToLive) {
        cookieTimeToLive = timeToLive;
        if (DEBUG.messageEnabled()) {
            DEBUG.message("LoginState.setCookieTimeToLive(): cookieTimeToLive=" + cookieTimeToLive);
        }
    }

    /**
     * Returns user domain.
     *
     * @return user domain.
     */
    private String getUserDomain(
            HttpServletRequest request,
            SessionID sid,
            Map<String, String> requestHash) {
        String userOrg;
        if (AuthUtils.newSessionArgExists(requestHash, sid) && sid.toString().length() > 0) {
            userOrg = sid.getSessionDomain();
        } else {
            userOrg = AuthUtils.getDomainNameByRequest(request, requestHash);
        }

        if (DEBUG.messageEnabled()) {
            DEBUG.message("returning from getUserDomain : " + userOrg);
        }
        return userOrg;
    }

    /**
     * Returns authentication context for new request.
     *
     * @return Authentication context for new request.
     * @throws AuthException if it fails to instantiate <code>AuthContext</code>
     */
    AuthContextLocal createAuthContext(
            HttpServletRequest request,
            HttpServletResponse response,
            SessionID sid,
            Map<String, String> requestHash
    ) throws AuthException {
        // Get / Construct the Original Login URL
        this.loginURL = AuthUtils.constructLoginURL(request);

        // Get query param indicating a request "forward" after
        // successful authentication.
        this.forwardSuccess = AuthUtils.forwardSuccessExists(request);

        // set the locale
        setRequestLocale(request);

        if (DEBUG.messageEnabled()) {
            DEBUG.message("locale : " + localeContext.getLocale());
        }

        this.userOrg = getUserDomain(request, sid, requestHash);
        if (DEBUG.messageEnabled()) {
            DEBUG.message("createAuthContext: userOrg is : " + userOrg);
        }

        if ((this.userOrg == null) || this.userOrg.length() == 0) {
            DEBUG.message("domain is null, error condition");
            logFailed(LazyConfig.AUTHD.bundle.getString("invalidDomain"), "INVALIDDOMAIN");
            auditor.auditLoginFailure(this, INVALID_REALM);
            throw new AuthException(AMAuthErrorCode.AUTH_INVALID_DOMAIN, null);
        }

        if (DEBUG.messageEnabled()) {
            DEBUG.message("AuthUtil:getAuthContext: Creating new AuthContextLocal & LoginState");
        }
        AuthContextLocal authContext = new AuthContextLocal(this.userOrg);
        newRequest = true;
        servletRequest = request;
        servletResponse = response;
        setParamHash(requestHash);
        client = getClient();
        this.finalSessionId = sid;
        if (DEBUG.messageEnabled()) {
            DEBUG.message("requestType : " + newRequest);
            DEBUG.message("client : " + client);
            DEBUG.message("sid : " + sid);
        }

        try {
            createSession(request, authContext);
        } catch (Exception e) {
            DEBUG.error("Exception creating session .. :", e);
            throw new AuthException(e);
        }
        String cookieSupport = AuthUtils.getCookieSupport(getClientType());
        cookieDetect = AuthUtils.getCookieDetect(cookieSupport);
        if ((cookieSupport != null) && cookieSupport.equals("false")) {
            cookieSupported = false;
        }
        if (DEBUG.messageEnabled()) {
            DEBUG.message("cookieSupport is : " + cookieSupport);
            DEBUG.message("cookieDetect is .. : " + cookieDetect);
            DEBUG.message("cookieSupported is .. : " + cookieSupported);
        }
        if (AuthUtils.isClientDetectionEnabled() && cookieDetect) {
            cookieSet = true;
        }
        setDecodedGoToOnFailURL();
        amIdRepo = LazyConfig.AUTHD.getAMIdentityRepository(getOrgDN());
        populateOrgProfile();
        return authContext;
    }

    /* create new session */
    private void createSession(
            HttpServletRequest req,
            AuthContextLocal authContext
    ) throws AuthException {
        DEBUG.message("LoginState: createSession: Creating new session: ");
        InternalSession session = LazyConfig.AUTHD.newSession(getOrgDN(), false);
        DEBUG.message("Save authContext in InternalSession");
        finalSessionId = session.getID();
        sessionReference = session.getSessionID();
        session.setAuthContext(authContext);

        if (DEBUG.messageEnabled()) {
            DEBUG.message(
                    "LoginState:createSession: New session/sid=" + finalSessionId);
            DEBUG.message("LoginState:New session: ac=" + authContext);
        }
    }

    /**
     * Returns the single sign on token associated with the session.
     *
     * @return the single sign on token associated with the session.
     * @throws SSOException
     */
    public SSOToken getSSOToken() throws SSOException {
        if (null == sessionReference || isNoSession()) {
            return null;
        }
        InternalSession session = sessionAccessManager.getInternalSession((getForceFlag())?oldSessionReference:sessionReference);
        if (!stateless && session == null) {
            return null;
        }

        try {
            SSOTokenManager ssoManager = SSOTokenManager.getInstance();
            SSOToken ssoToken = ssoManager.createSSOToken((getForceFlag())?oldSessionReference.toString() :finalSessionId.toString());
            return ssoToken;
        } catch (SSOException ex) {
            DEBUG.message("Error retrieving SSOToken :", ex);
            throw new SSOException(AuthD.BUNDLE_NAME,
                    AMAuthErrorCode.AUTH_ERROR, null);
        }
    }

    /**
     * Returns URL with the cookie value in the URL.
     *
     * @param url      URL.
     * @return Encoded URL.
     */
    public String encodeURL(String url) {
        return encodeURL(url, false);
    }

    /**
     * Returns URL with the cookie value in the URL.
     * The cookie in the rewritten url will have
     * the AM cookie if session is active/inactive and
     * auth cookie if cookie is invalid
     *
     * @return the encoded URL
     */
    public String encodeURL(final String url, final boolean useAMCookie) {

        if (DEBUG.messageEnabled()) {
            DEBUG.message("in encodeURL");
        }
        boolean appendSessCookieInURL = SystemProperties.getAsBoolean(Constants.APPEND_SESS_COOKIE_IN_URL, true);
        if (!appendSessCookieInURL) {
            return url;
        }

        if (DEBUG.messageEnabled()) {
            DEBUG.message("cookieDetect : " + cookieDetect);
            DEBUG.message("cookieSupported : " + cookieSupported);
        }
        if (!cookieDetect && cookieSupported) {
            return url;
        }

        InternalSession session = getReferencedSession();
        if (null == session) {
            return url;
        }

        String cookieName = AuthUtils.getCookieName();
        if (!useAMCookie && session.getState() == SessionState.INVALID) {
            cookieName = AuthUtils.getAuthCookieName();
        }

        String encodedURL;
        if (URL_REWRITE_IN_PATH) {
            encodedURL = SessionURL.getInstance().encodeInternalSessionURL(
                    url, SessionUtils.SEMICOLON, false, cookieName, session);
        } else {
            encodedURL = SessionURL.getInstance().encodeInternalSessionURL(
                    url, SessionUtils.QUERY, false, cookieName, session);
        }

        if (DEBUG.messageEnabled()) {
            DEBUG.message("AuthRequest encodeURL : URL=" + url +
                    ", Rewritten URL=" + encodedURL);
        }
        return (encodedURL);
    }

    private InternalSession getReferencedSession() {
        if (null == sessionReference) {
            return null;
        }
        return AuthD.getSession(sessionReference);
    }

    private InternalSession getReferencedOldSession() {
        if (null == oldSessionReference) {
            return null;
        }
        return AuthD.getSession(oldSessionReference);
    }

    /**
     * Returns the filename . This method uses ResourceLookup API
     * to locate the resource/file. The resource/file search path is
     * <pre>
     * fileRoot_locale/orgPath/filePath/filename
     * fileRoot/orgPath/filePath/filename
     * default_locale/orgPath/filePath/filename
     * default/orgPath/filePath/filename
     * where filePath =
     *            clientPath (html/wml etc) + serviceName
     * eg. if orgDN = o=solaris.eng,o=eng.com,o=sun.com,dc=iplanet,dc=com
     *    clientPath = html
     *    service name = paycheck
     *    locale=en
     *    filename=Login.jsp
     * </pre>
     * then the search will be as follows :
     * <pre>
     * iplanet_en/sun.com/eng.com/solaris.eng/html/paycheck/Login.jsp
     * iplanet_en/sun.com/eng.com/solaris.eng/html/Login.jsp
     * iplanet_en/sun.com/eng.com/solaris.eng/Login.jsp
     * iplanet_en/sun.com/eng.com/html/paycheck/Login.jsp
     * iplanet_en/sun.com/eng.com/html/Login.jsp
     * iplanet_en/sun.com/eng.com/Login.jsp
     * iplanet_en/sun.com/html/paycheck/Login.jsp
     * iplanet_en/sun.com/html/Login.jsp
     * iplanet_en/sun.com/Login.jsp
     * iplanet_en/html/paycheck/Login.jsp
     * iplanet_en/html/Login.jsp
     * iplanet_en/Login.jsp
     *
     * iplanet/sun.com/eng.com/solaris.eng/html/paycheck/Login.jsp
     * iplanet/sun.com/eng.com/solaris.eng/html/Login.jsp
     * iplanet/sun.com/eng.com/solaris.eng/Login.jsp
     * iplanet/sun.com/eng.com/html/paycheck/Login.jsp
     * iplanet/sun.com/eng.com/html/Login.jsp
     * iplanet/sun.com/eng.com/Login.jsp
     * iplanet/sun.com/html/paycheck/Login.jsp
     * iplanet/sun.com/html/Login.jsp
     * iplanet/sun.com/Login.jsp
     * iplanet/html/paycheck/Login.jsp
     * iplanet/html/Login.jsp
     * iplanet/Login.jsp
     *
     * default_en/sun.com/eng.com/solaris.eng/html/paycheck/Login.jsp
     * default_en/sun.com/eng.com/solaris.eng/html/Login.jsp
     * default_en/sun.com/eng.com/solaris.eng/Login.jsp
     * default_en/sun.com/eng.com/html/paycheck/Login.jsp
     * default_en/sun.com/eng.com/html/Login.jsp
     * default_en/sun.com/eng.com/Login.jsp
     * default_en/sun.com/html/paycheck/Login.jsp
     * default_en/sun.com/html/Login.jsp
     * default_en/sun.com/Login.jsp
     * default_en/html/paycheck/Login.jsp
     * default_en/html/Login.jsp
     * default_en/Login.jsp
     *
     * default/sun.com/eng.com/solaris.eng/html/paycheck/Login.jsp
     * default/sun.com/eng.com/solaris.eng/html/Login.jsp
     * default/sun.com/eng.com/solaris.eng/Login.jsp
     * default/sun.com/eng.com/html/paycheck/Login.jsp
     * default/sun.com/eng.com/html/Login.jsp
     * default/sun.com/eng.com/Login.jsp
     * default/sun.com/html/paycheck/Login.jsp
     * default/sun.com/html/Login.jsp
     * default/sun.com/Login.jsp
     * default/html/paycheck/Login.jsp
     * default/html/Login.jsp
     * default/Login.jsp
     * </pre>
     * In case of non-HTML client, it will try to find
     * <code>Login_&lt;charset>.jsp</code>.
     * If not found, it then try <coed>Login.jsp</code>.
     *
     * @return configured jsp file name
     */
    public String getFileName(String fileName) {

        return AuthUtils.getFileName(fileName, getLocale(), getOrgDN(),
                servletRequest, LazyConfig.AUTHD.getServletContext(), indexType, indexName);
    }

    /**
     * Create user profile.
     *
     * @return <code>true</code> if profile is successfully created.
     */
    private boolean createUserProfile(String token, Set aliasList) {
        try {
            if (!dynamicProfileCreation) {
                DEBUG.message("Error this user requires a profile to login");
                return false;
            }
            // If the module is "Application" then do not create user profile

            if (isApplicationModule(authMethName)) {
                DEBUG.message("No profile created for Application module");
                return false;
            }
            if (DEBUG.messageEnabled()) {
                DEBUG.message("Creating user entry: " + token);
                DEBUG.message("aliasList : " + aliasList);
            }

            if (userCreationAttributes == null) {
                userCreationAttributes = new HashMap<String, Object>();
            }
            // get alias list
            Map<String, Object> aliasMap = Collections.emptyMap();
            if ((aliasList != null) && !aliasList.isEmpty()) {
                // set alias attribute
                DEBUG.message("Adding alias list to user profile");
                if ((externalAliasList != null) &&
                        (!externalAliasList.isEmpty())) {
                    aliasList.addAll(externalAliasList);
                }
                aliasMap.put(ISAuthConstants.USER_ALIAS_ATTR, aliasList);
            }
            if (!aliasMap.isEmpty()) {
                userCreationAttributes.putAll(aliasMap);
            }
            if (DEBUG.messageEnabled()) {
                DEBUG.message("userCreationAttributes is : "
                        + userCreationAttributes);
            }

            Set<String> userPasswordSet = new HashSet<String>(1);
            byte bytes[] = new byte[20];
            SECURE_RANDOM.nextBytes(bytes);
            userPasswordSet.add(byteArrayToHexString(bytes));
            userCreationAttributes.put(
                    ISAuthConstants.ATTR_USER_PASSWORD, userPasswordSet);

            amIdentityUser =
                    createUserIdentity(token, userCreationAttributes, defaultRoles);

            userDN = getUserDN(amIdentityUser);

            Map userAttributes = amIdentityUser.getAttributes();
            if (amIdentityRole != null) {
                // retrieve the session attributes for the default role
                Map sattrs = amIdentityRole.getServiceAttributes(
                        ISAuthConstants.SESSION_SERVICE_NAME);
                if (sattrs != null && !sattrs.isEmpty()) {
                    userAttributes.putAll(sattrs);
                }
            }
            populateUserAttributes(userAttributes, true, null);
            return true;
        } catch (Exception ex) {
            DEBUG.error("Cannot create user profile for: " + token);
            if (DEBUG.messageEnabled()) {
                DEBUG.message("Stack trace: ", ex);
            }
        }
        return false;
    }

    private int[] getDefaultSessionAttributes(String orgDN) {
        Map<String, Set<String>> map =
                LazyConfig.AUTHD.getOrgServiceAttributes(orgDN, ISAuthConstants.SESSION_SERVICE_NAME);
        return new int[] {
                CollectionHelper.getIntMapAttr(map, ISAuthConstants.MAX_SESSION_TIME,
                        LazyConfig.AUTHD.getDefaultMaxSessionTime(), DEBUG),
                CollectionHelper.getIntMapAttr(map, ISAuthConstants.SESS_MAX_IDLE_TIME,
                        LazyConfig.AUTHD.getDefaultMaxIdleTime(), DEBUG),
                CollectionHelper.getIntMapAttr(map, ISAuthConstants.SESS_MAX_CACHING_TIME,
                        LazyConfig.AUTHD.getDefaultMaxCachingTime(), DEBUG) };
    }

    private void populateUserAttributes(Map<String, Set<String>> userAttributes, boolean loginStatus,
                                        AMIdentity amIdentity) throws AMException {
        int[] sessionAttrs = getDefaultSessionAttributes(getOrgDN());

        if (DEBUG.messageEnabled()) {
            DEBUG.message("default max session time: " + sessionAttrs[0]
                    + "\ndefault max idle time: " + sessionAttrs[1]
                    + "\ndefault max caching time: " + sessionAttrs[2]);
        }

        try {
            userAuthConfig = CollectionHelper.getMapAttr(
                    userAttributes, ISAuthConstants.AUTHCONFIG_USER, null);
            if (!loginStatus) {
                Set<String> userFailureURLSet = userAttributes.get(ISAuthConstants.USER_FAILURE_URL);
                clientUserFailureURL = getRedirectUrl(userFailureURLSet);
                defaultUserFailureURL = tempDefaultURL;
                Set<String> failureRoleURLSet = userAttributes.get(ISAuthConstants.LOGIN_FAILURE_URL);
                clientFailureRoleURL = getRedirectUrl(failureRoleURLSet);
                defaultFailureRoleURL = tempDefaultURL;
                return;
            }

            maxSession = CollectionHelper.getIntMapAttr(
                    userAttributes, ISAuthConstants.MAX_SESSION_TIME,
                    sessionAttrs[0], DEBUG);
            idleTime = CollectionHelper.getIntMapAttr(
                    userAttributes, ISAuthConstants.SESS_MAX_IDLE_TIME, sessionAttrs[1], DEBUG);
            cacheTime = CollectionHelper.getIntMapAttr(
                    userAttributes, ISAuthConstants.SESS_MAX_CACHING_TIME, sessionAttrs[2],
                    DEBUG);

            // Status determination
            String tmp = CollectionHelper.getMapAttr(
                    userAttributes, ISAuthConstants.INETUSER_STATUS, "active");

            // OPEN ISSUE- amIdentity.isActive return true even if
            // user status is set to inactive.
            if (amIdentity != null) {
                tmp = amIdentity.isActive() ? "active" : "inactive";
            }
            String tmp1 = CollectionHelper.getMapAttr(
                    userAttributes, ISAuthConstants.LOGIN_STATUS, "active");
            String tmp2 = CollectionHelper.getMapAttr(
                    userAttributes, ISAuthConstants.NSACCOUNT_LOCK, ISAuthConstants.FALSE_VALUE);
            if (DEBUG.messageEnabled()) {
                DEBUG.message("entity status is : " + tmp);
                DEBUG.message("user-login-status is : " + tmp1);
                DEBUG.message("nsaccountlock is : " + tmp2);
            }
            if (!tmp1.equalsIgnoreCase("active") ||
                    !tmp.equalsIgnoreCase("active") ||
                    !tmp2.equalsIgnoreCase("false")) {
                userEnabled = false;
            }

            String ulocale = CollectionHelper.getMapAttr(
                    userAttributes, ISAuthConstants.PREFERRED_LOCALE, null);
            localeContext.setUserLocale(ulocale);

            userAliasList = (Set) userAttributes.get(ISAuthConstants.USER_ALIAS_ATTR);
            // add value from attributes in iplanet-am-auth-alias-attr-name
            if (aliasAttrNames != null && !aliasAttrNames.isEmpty()) {
                Iterator<String> it = aliasAttrNames.iterator();
                while (it.hasNext()) {
                    String attrName = it.next();
                    Set attrVals = (Set) userAttributes.get(attrName);
                    if (attrVals != null) {
                        if (userAliasList == null) {
                            userAliasList = new HashSet<String>();
                        }
                        userAliasList.addAll(attrVals);
                    }
                }
            }
            setAccountLife(CollectionHelper.getMapAttr(
                    userAttributes, ISAuthConstants.ACCOUNT_LIFE));

            // retrieve the user default success url
            // at user's role level
            Set<String> userSuccessURLSet = userAttributes.get(ISAuthConstants.USER_SUCCESS_URL);
            clientUserSuccessURL = getRedirectUrl(userSuccessURLSet);
            defaultUserSuccessURL = tempDefaultURL;

            Set<String> successRoleURLSet = userAttributes.get(ISAuthConstants.LOGIN_SUCCESS_URL);
            clientSuccessRoleURL = getRedirectUrl(successRoleURLSet);
            defaultSuccessRoleURL = tempDefaultURL;

            if (DEBUG.messageEnabled()) {
                DEBUG.message("Populate User attributes" +
                        "\n  idle->" + idleTime +
                        "\n  cache->" + cacheTime +
                        "\n  max->" + maxSession +
                        "\n  userLoginEnabled->" + userEnabled +
                        "\n  charset->" + localeContext.getMIMECharset() +
                        "\n  locale->" + localeContext.getLocale().toString() +
                        "\n  userAlias->  :" + userAliasList +
                        "\n  userSuccessURLSet-> :" + userSuccessURLSet +
                        "\n  clientUserSuccessURL->  :" + clientUserSuccessURL +
                        "\n  defaultUserSuccessURL->  :" + defaultUserSuccessURL +
                        "\n  clientUserFailureURL->  :" + clientUserFailureURL +
                        "\n  defaultUserFailureURL->  :" + defaultUserFailureURL +
                        "\n  clientSuccessRoleURL ->  :" + clientSuccessRoleURL +
                        "\n  defaultSuccessRoleURL ->  :" + defaultSuccessRoleURL +
                        "\n  clientFailureRoleURL ->  :" + clientFailureRoleURL +
                        "\n  defaultFailureRoleURL ->  :" + defaultFailureRoleURL +
                        "\n  userAuthConfig -> : " + userAuthConfig +
                        "\n  accountLife->" + getAccountLife());
            }
        } catch (Exception e) {
            if (DEBUG.messageEnabled()) {
                DEBUG.message("Eception in populateUserAttributes : ", e);
            }
            throw new AMException(e.getMessage(), e.toString());
        }
    }

    /**
     * Returns <code>true</code> if user profile found.
     *
     * @param token
     * @param populate
     * @return <code>true</code> if user profile found.
     * @throws AuthException if multiple user match found in search
     */
    boolean getUserProfile(String token, boolean populate)
            throws AuthException {
        try {
            return getUserProfile(token, populate, true);
        } catch (Exception e) {
            if (DEBUG.messageEnabled()) {
                DEBUG.message("getUserProfile(string,boolean)", e);
            }
            throw new AuthException(e);
        }
    }

    /**
     * Returns <code>true</code> if user profile found.
     *
     * @param user        userID for profile
     * @param populate
     * @param loginStatus current login status for profile
     * @return <code>true</code> if user profile found.
     * @throws AuthException if multiple user match found in search
     */
    private boolean getUserProfile(
            String user,
            boolean populate,
            boolean loginStatus
    ) throws AuthException {
        // Fix to cover SDK Bug which returns a user object
        // even if the user is null or empty string
        // if this check is not added SDK goes into a loop
        if ((user == null) || (user.length() == 0)) {
            throw new AuthException(AMAuthErrorCode.AUTH_ERROR, null);
        }

        IdType idt = null;
        try {
            if (DEBUG.messageEnabled()) {
                DEBUG.message("In getUserProfile : Search for user " + user);
            }

            Set<AMIdentity> amIdentitySet = Collections.emptySet();
            IdSearchResults searchResults = null;
            if (LazyConfig.AUTHD.isSuperAdmin(user)) {
                // get the AMIdentity to get the universal
                // id of amAdmin, currently there is no support
                // for special users so the universal id in
                // the ssotoken will be amAdmin's id.
                AMIdentity amIdentity = LazyConfig.AUTHD.getIdentity(IdType.USER,
                        user, getOrgDN());
                amIdentitySet = new HashSet<AMIdentity>();
                amIdentitySet.add(amIdentity);
            } else {
                // Try getting the AMIdentity object assuming AMSDK
                // is present i.e., using IdUtils
                try {
                    if (DEBUG.messageEnabled()) {
                        DEBUG.message("LoginState: gettingIdentity " +
                                "using IdUtil.getIdentity: " + user +
                                " Org: " + getOrgDN());
                    }
                    AMIdentity amIdentity = IdUtils.getIdentity(
                            LazyConfig.AUTHD.getSSOAuthSession(), user, getOrgDN());
                    if (amIdentity != null && amIdentity.isExists()) {
                        amIdentitySet = new HashSet<>();
                        amIdentitySet.add(amIdentity);
                        idt = amIdentity.getType();
                        if (DEBUG.messageEnabled()) {
                            DEBUG.message("LoginState: getIdentity " +
                                    "using IdUtil.getIdentity: " + amIdentity);
                        }
                    }
                } catch (IdRepoException e) {
                    // Ignore the exception and continue
                    if (DEBUG.messageEnabled()) {
                        DEBUG.message("LoginState: getting identity " +
                                "Got IdRepException in IdUtils.getIdentity", e);
                    }
                } catch (SSOException se) {
                    // Ignore the exception and continue
                    if (DEBUG.messageEnabled()) {
                        DEBUG.message("LoginState: getting identity " +
                                "Got SSOException in IdUtils.getIdentity", se);
                    }
                }

                // If amIdentitySet is still empty, or IdType does not match
                // search for all configured Identity Types
                if (amIdentitySet == Collections.EMPTY_SET ||
                        idt != null && !identityTypes.contains(idt.getName())) {
                    if (DEBUG.messageEnabled()) {
                        DEBUG.message("LoginState: getIdentity " +
                                "performing IdRepo search to obtain AMIdentity");
                    }
                    String userTokenID = DNUtils.DNtoName(user);

                    if (DEBUG.messageEnabled()) {
                        DEBUG.message("Search for Identity " + userTokenID);
                    }

                    Set<String> tmpIdentityTypes = new HashSet<String>(identityTypes);
                    if (identityTypes.contains("user")) {
                        tmpIdentityTypes.remove("user");
                        searchResults = searchIdentity(IdUtils.getType("user"), userTokenID, populate);
                        if (searchResults != null) {
                            amIdentitySet = searchResults.getSearchResults();
                        }
                    }
                    if (amIdentitySet.isEmpty()) {
                        for (final String strIdType : tmpIdentityTypes) {
                            // Get identity by searching
                            searchResults = searchIdentity(IdUtils.getType(strIdType), userTokenID, populate);
                            if (searchResults != null) {
                                amIdentitySet = searchResults.getSearchResults();
                            }
                            if (!amIdentitySet.isEmpty()) {
                                break;
                            }
                        }
                    }

                }
            }

            if (DEBUG.messageEnabled()) {
                DEBUG.message("result is :" + amIdentitySet);
            }
            if (amIdentitySet.isEmpty()) {
                return false;
            }
            // check if there is multiple match
            if (amIdentitySet.size() > 1) {
                // multiple user match found, throw exception,
                // user need to login as super admin to fix it
                DEBUG.error("getUserProfile : Multiple matches found for " +
                        "user '" + token + "' in org " + orgDN +
                        "\nPlease make sure user is unique within the login " +
                        "organization, and contact your admin to fix the problem");
                throw new AuthException(AMAuthErrorCode.AUTH_ERROR, null);
            }
            amIdentityUser = amIdentitySet.iterator().next();
            userDN = getUserDN(amIdentityUser);
            idt = amIdentityUser.getType();
            if (DEBUG.messageEnabled()) {
                DEBUG.message("userDN is : " + userDN);
                DEBUG.message("userID(token) is : " + token);
                DEBUG.message("idType is : " + idt);
            }
            if (populate) {
                Map basicAttrs = null;
                Map serviceAttrs = null;
                if (searchResults != null) {
                    basicAttrs = (Map) searchResults.getResultAttributes().get(amIdentityUser);
                } else {
                    basicAttrs = amIdentityUser.getAttributes();
                }

                if (amIdentityRole != null) {
                    // role based auth. the specified role takes preference.
                    DEBUG.message("retrieving session service from role");
                    if (amIdentityRole != null) {
                        //Fix for OPENAM-612 - this request is cached most of the time
                        Set oc = amIdentityRole.getAttribute("objectclass");
                        if (oc != null && oc.contains("iplanet-am-session-service")) {
                            serviceAttrs = amIdentityRole.getServiceAttributes(
                                    ISAuthConstants.SESSION_SERVICE_NAME);
                        }
                    }
                } else if (idt.equals(IdType.USER)) {
                    DEBUG.message("retrieving session service from user");
                    //Fix for OPENAM-612 - this request is cached most of the time
                    Set oc = amIdentityUser.getAttribute("objectclass");
                    if (oc != null && oc.contains("iplanet-am-session-service")) {
                        serviceAttrs = amIdentityUser.getServiceAttributes(
                                ISAuthConstants.SESSION_SERVICE_NAME);
                    }
                }
                if (serviceAttrs != null && !serviceAttrs.isEmpty()) {
                    basicAttrs.putAll(serviceAttrs);
                }

                populateUserAttributes(basicAttrs, loginStatus, amIdentityUser);
            }
            return true;
        } catch (SSOException ex) {
            DEBUG.error("SSOException");
            if (DEBUG.messageEnabled()) {
                DEBUG.message("Stack trace: ", ex);
            }
        } catch (AMException ex) {
            DEBUG.error("No aliases for: " + aliasAttrNames + "=" + token);
            if (DEBUG.messageEnabled()) {
                DEBUG.message("Stack trace: ", ex);
            }
        } catch (IdRepoException ee) {
            if (DEBUG.messageEnabled()) {
                DEBUG.error("IdReporException ", ee);
            }
        }

        return false;
    }

    /**
     * Populate all the default user attribute for profile
     *
     * @throws AMException if it fails to populate default user attributes
     */
    public void populateDefaultUserAttributes() throws AMException {
        int[] sessionAttrs = getDefaultSessionAttributes(getOrgDN());
        maxSession = sessionAttrs[0];
        idleTime = sessionAttrs[1];
        cacheTime = sessionAttrs[2];
        userEnabled = true;

        if (DEBUG.messageEnabled()) {
            DEBUG.message("Populate Default User attributes" +
                    "\n  idle->" + idleTime +
                    "\n  cache->" + cacheTime +
                    "\n  max->" + maxSession +
                    "\n  userLoginEnabled->" + userEnabled +
                    "\n  clientUserSuccessURL ->" + clientUserSuccessURL +
                    "\n  defaultUserSuccessURL ->" + defaultUserSuccessURL +
                    "\n  clientUserFailureURL ->" + clientUserFailureURL +
                    "\n  defaultUserFailureURL ->" + defaultUserFailureURL +
                    "\n  clientSuccessRoleURL ->" + clientSuccessRoleURL +
                    "\n  defaultSuccessRoleURL ->" + defaultSuccessRoleURL +
                    "\n  clientFailureRoleURL ->" + clientFailureRoleURL +
                    "\n  defaultFailureRoleURL ->" + defaultFailureRoleURL +
                    "\n  userAuthConfig ->" + userAuthConfig +
                    "\n  charset->" + localeContext.getMIMECharset() +
                    "\n  locale->" + localeContext.getLocale().toString());
        }
    }

    /**
     * Search the user profile
     * if <code>IndexType</code> is USER and if number of tokens is 1 and
     * token is <code>superAdmin</code> then return. If more then 1 tokens
     * are found then make sure the user tokens are in
     * <code>iplanet-am-useralias-list</code>
     * <p/>
     * If <code>IndexType</code> is <code>LEVEL</code>, <code>MODULE</code>
     * then there is only 1 user token retrieve the profile for the
     * authenticated user and create profile if dynamic profile creation
     * enabled.
     * <p/>
     * If <code>IndexType</code> is <code>ORG</code>, <code>SERVICE</code>,
     * <code>ROLE</code> then retrieve the user profile for first token, if the
     * profile is found and <code>user-alias-list</code> contains other
     * tokens then continue, else try to retrieve remaining tokens till a match
     * is found.
     * <p/>
     * Checks all the users in the tokenSet are active else error
     * For ROLE based authentication checks if all user belong to the same Role.
     *
     * @param subject
     * @param indexType
     * @param indexName
     * @return <code>true</code> if it found user profile
     * @throws AuthException
     */
    boolean searchUserProfile(
            Subject subject,
            AuthContext.IndexType indexType,
            String indexName
    ) throws AuthException {
        Set<String> tokenSet = getTokenFromPrincipal(subject);
        // check for all users user authenticated as
        if (DEBUG.messageEnabled()) {
            DEBUG.message("in searchUserProfile");
            DEBUG.message("indexType is.. :" + indexType);
            DEBUG.message("indexName is.. :" + indexName);
            DEBUG.message("Subject is.. :" + subject);
            DEBUG.message("token is.. :" + token);
            DEBUG.message("tokenSet is.. :" + tokenSet);
            DEBUG.message("ignoreUserProfile.. :" + ignoreUserProfile);
            DEBUG.message("userDN is.. :" + userDN);
        }

        // retreive the tokens from the subject
        try {
            boolean gotUserProfile = true;
            if (((ignoreUserProfile && !isApplicationModule(indexName))) ||
                    (isApplicationModule(indexName) && LazyConfig.AUTHD.isSuperAdmin(userDN))) {
                if (LazyConfig.AUTHD.isSuperAdmin(userDN)) {
                    amIdentityUser = LazyConfig.AUTHD.getIdentity(IdType.USER, userDN, getOrgDN());
                } else {
                    amIdentityUser =
                            new AMIdentity(null, userDN, IdType.USER, getOrgDN(), null);
                }
                userDN = getUserDN(amIdentityUser);
                populateDefaultUserAttributes();
                return true;
            }

            // for IndexType USER check all the token user
            // authenticated as is present
            // in the user-alias-list

            if ((indexType == AuthContext.IndexType.USER)) {
                if (token == null) {
                    return false;
                }
                getUserProfile(token, true);
                Map<String, Boolean> aliasFound = searchUserAliases(token, tokenSet);
                if (!checkAliasList(aliasFound)) {
                    if (createWithAlias) {
                        if (amIdentityUser == null) {
                            addAliasToUserProfile(amIdentityUser, aliasFound);
                        } else {
                            addAliasToUserProfile(token, aliasFound);
                        }
                    } else {
                        throw new AuthException(
                                AMAuthErrorCode.AUTH_LOGIN_FAILED, null);
                    }
                }
            } else {
                // for ORG / SERVICE / ROLE / MODULE / LEVEL
                boolean gotProfile = true;
                if (tokenSet.isEmpty()) {
                    DEBUG.message("tokenset empty");
                    throw new AuthException(AMAuthErrorCode.AUTH_ERROR, null);
                } else if (tokenSet.size() == 1) {
                    if (isAccountLocked(getUserUniversalId(token))) {
                        DEBUG.message("User account \"{}\" locked", token);
                        throw new AuthException(AMAuthErrorCode.AUTH_USER_LOCKED, null);
                    }
                    DEBUG.message("tokenset size is 1");
                    gotUserProfile = getCreateUserProfile(true);
                    if (!userEnabled) {
                        setFailedUserId(token);
                        throw new AuthException(
                                AMAuthErrorCode.AUTH_USER_INACTIVE, null);
                    }
                    if (LazyConfig.AUTHD.isSuperAdmin(userDN)) {
                        return true;
                    }
                    if (gotUserProfile) {
                        if (indexType == AuthContext.IndexType.ROLE) {
                            boolean userRoleFound = getUserForRole(
                                    getIdentityRole(indexName));
                            if (DEBUG.messageEnabled()) {
                                DEBUG.message("userRoleFound: "
                                        + userRoleFound);
                            }
                            if (!userRoleFound) {
                                logFailed(AuthUtils.getErrorVal(AMAuthErrorCode.
                                                AUTH_USER_NOT_FOUND, AuthUtils.ERROR_MESSAGE),
                                        "USERNOTFOUND");
                                auditor.auditLoginFailure(this, USER_NOT_FOUND);
                                throw new AuthException(
                                        AMAuthErrorCode.AUTH_USER_NOT_FOUND, null);
                            }
                        }
                    }
                } else { // came here multiple users found
                    DEBUG.message("came here !! multiple modules , users ");

                    // initialize variables required

                    String validToken = null;
                    boolean foundUserAlias = false;
                    boolean userRoleFound = true;
                    Map<String, Boolean> userEnabledMap = new HashMap<String, Boolean>();
                    Map<String, Boolean> userRoleFoundMap = new HashMap<String, Boolean>();
                    Map<String, Boolean> foundAliasMap = new HashMap<String, Boolean>();
                    Map<String, Boolean> gotUserProfileMap = new HashMap<String, Boolean>();
                    String aliasToken = null;

                    for (final String tok : tokenSet) {
                        token = tok;
                        if (DEBUG.messageEnabled()) {
                            DEBUG.message("BEGIN WHILE: Token is.. : "
                                    + token);
                        }
                        gotUserProfile = getUserProfile(token, true);
                        gotUserProfileMap.put(token, gotUserProfile);
                        if (DEBUG.messageEnabled()) {
                            DEBUG.message("gotUserProfile : " + gotUserProfile);
                        }
                        if (gotUserProfile) {
                            if (validToken == null) {
                                validToken = token;
                            }
                            userEnabledMap.put(token, userEnabled);

                            if (indexType == AuthContext.IndexType.ROLE) {
                                userRoleFound = getUserForRole(
                                        getIdentityRole(indexName));
                                userRoleFoundMap.put(token, userRoleFound);
                            }
                            foundAliasMap = searchUserAliases(token, tokenSet);
                            if (foundUserAlias =
                                    getFoundUserAlias(foundAliasMap)) {
                                aliasToken = token;
                                if (DEBUG.messageEnabled()) {
                                    DEBUG.message(
                                            "found aliases exiting while:"
                                                    + foundAliasMap);
                                }
                                break;
                            }
                        }
                    } // end while
                    if (DEBUG.messageEnabled()) {
                        DEBUG.message("Alias Token is : " + aliasToken);
                        DEBUG.message("Profile Token :" + validToken);
                        DEBUG.message("Token is : " + token);
                    }
                    if (aliasToken != null) {
                        token = aliasToken;
                    }
                    boolean userEnabled = getUserEnabled(userEnabledMap);
                    if (!userEnabled) {
                        setFailedUserId(DNUtils.DNtoName(token));
                        throw new AuthException(
                                AMAuthErrorCode.AUTH_USER_INACTIVE, null);
                    }

                    if (indexType == AuthContext.IndexType.ROLE) {
                        userRoleFound = getUserRoleFound(userRoleFoundMap);
                        if (!userRoleFound) {
                            logFailed(AuthUtils.getErrorVal(AMAuthErrorCode.
                                            AUTH_USER_NOT_FOUND, AuthUtils.ERROR_MESSAGE),
                                    "USERNOTFOUND");
                            auditor.auditLoginFailure(this, USER_NOT_FOUND);
                            throw new AuthException(
                                    AMAuthErrorCode.AUTH_USER_NOT_FOUND, null);
                        }
                        DEBUG.message("userRoleFound:true");
                    }

                    gotUserProfile = getGotUserProfile(gotUserProfileMap);
                    DEBUG.message("userEnabled : true");

                /* if user profile is found but other tokens in do
                 * are not found in iplanet-am-user-alias list and
                 * if dynamic profile creation with user alias is
                 * enabled then add tokens to iplanet-am-user-alias-list
                 * to the token's profile
                 */

                    if ((gotUserProfile) && (!foundUserAlias)) {
                        if (createWithAlias) {
                            if (DEBUG.messageEnabled()) {
                                DEBUG.message("dynamicProfileCreation : "
                                        + dynamicProfileCreation);
                                DEBUG.message("foundUserAliasMap : "
                                        + foundAliasMap);
                                DEBUG.message("foundUserAliasMap : "
                                        + foundUserAlias);
                            }
                            addAliasToUserProfile(validToken, foundAliasMap);
                        } else { //end dynamic profile creation
                            throw new AuthException(
                                    AMAuthErrorCode.AUTH_LOGIN_FAILED, null);
                        }
                    }
                    if (createWithAlias && !gotUserProfile) {
                        gotUserProfile = createUserProfileForTokens(tokenSet);
                    }
                }
            }
            if (DEBUG.messageEnabled()) {
                DEBUG.message("LoginState:searchUserProfile:returning: "
                        + gotUserProfile);
            }
            return gotUserProfile;
        } catch (AuthException e) {
            throw new AuthException(e);
        } catch (Exception e) {
            DEBUG.error("Error retrieving profile", e);
            throw new AuthException(e);
        }
    }

    /**
     * Returns user's profile , if not found then create user profile.
     *
     * @param populate indicate if populate all default user attributes
     * @return <code>true</code> if created user profile successfully
     * @throws AuthException if fails create user profile
     */
    private boolean getCreateUserProfile(boolean populate)
            throws AuthException {
        boolean gotProfile = false;
        if (userDN != null) {
            gotProfile = getUserProfile(userDN, populate);
        } else {
            gotProfile = getUserProfile(token, populate);
        }
        if (!gotProfile) {
            if (!LazyConfig.AUTHD.isSuperAdmin(userDN)) {
                gotProfile = createUserProfile(token, null);
            }
        }
        return gotProfile;
    }

    /* if multiple users in the Subject then create User profile
     * for the first user and add the other tokens in the alias
     * list

     * NOTE: Currently we pick up the first token and just add the
     * other tokens to the alias list. Can make it configurable by
     * specifying isPrimary against a module in the JAAS confiugration
     * then read that and whichever module is PRIMARY , create profile
     * for that user and add the other user to the aliasList.
     */
    private boolean createUserProfileForTokens(Set<String> tokenSet) {

        // retrieve the first token
        // put the other tokens in a list
        // these will put in the alias list attribute
        // of first token's profile

        Set<String> tokensList = new HashSet<String>();
        String token = null;
        Iterator<String> tokenIterator = tokenSet.iterator();

        while (tokenIterator.hasNext()) {
            token = tokenIterator.next();
            if (LazyConfig.AUTHD.isSuperAdmin(token)) {
                break;
            }
            while (tokenIterator.hasNext()) {
                String alias = tokenIterator.next();
                if (DEBUG.messageEnabled()) {
                    DEBUG.message("alias list add token:" + alias);
                }
                tokensList.add(alias);
            }
        }

        if (DEBUG.messageEnabled()) {
            DEBUG.message("Tokens List is.. :" + tokensList);
        }

        try {
            return createUserProfile(token, tokensList);
        } catch (Exception e) {
            DEBUG.error("Cannot create user profile for: " + token);
            if (DEBUG.messageEnabled()) {
                DEBUG.message("Stack trace: ", e);
            }
            return false;
        }
    }

    /* search the user-alias-list for token names */
    private Map<String, Boolean> searchUserAliases(String userToken, Set<String> tokenSet) {
        Map<String, Boolean> foundUserAliasMap = new HashMap<String, Boolean>();

        if (DEBUG.messageEnabled()) {
            DEBUG.message("userAliastList is.. :" + userAliasList);
            DEBUG.message("userToken is.. :" + userToken);
            DEBUG.message("tokenSet is.. :" + tokenSet);
        }
        if ((tokenSet != null) && (!tokenSet.isEmpty())) {

            // iterate through the tokens in the token set
            // and check if the tokens are  in the user alias
            // list of the user token. if yes then update
            // the found user alias map with the token and boolean
            // true else boolean false.
            for (final String authToken : tokenSet) {
                if ((userAliasList != null) && !userAliasList.isEmpty()) {
                    if (DEBUG.messageEnabled()) {
                        DEBUG.message("AuthToken is : " + authToken);
                        DEBUG.message("userToken is : " + userToken);
                    }
                    if (((authToken != null)
                            && authToken.equalsIgnoreCase(userToken))
                            && (!foundUserAliasMap.containsKey(authToken))) {
                        foundUserAliasMap.put(authToken, Boolean.TRUE);
                    } else if (userAliasList.contains(authToken)) {
                        foundUserAliasMap.put(authToken, Boolean.TRUE);
                    } else {
                        foundUserAliasMap.put(authToken, Boolean.FALSE);
                    }
                } else {
                    if ((authToken != null)
                            && authToken.equalsIgnoreCase(userToken)) {
                        foundUserAliasMap.put(authToken, Boolean.TRUE);
                    } else {
                        foundUserAliasMap.put(authToken, Boolean.FALSE);
                    }
                }
            }
            if (DEBUG.messageEnabled()) {
                DEBUG.message("searchUserAliases: foundUserAliasMap : "
                        + foundUserAliasMap);
            }
        }
        return foundUserAliasMap;
    }

    /**
     * Returns tokens from Principals of a subject
     * returns a Set of tokens
     * TODO - DN to Universal ID?
     *
     * @param subject Principals of a subject associated with
     *                <code>SSOToken</code>
     * @return set of  <code>SSOToken</code> associated with subject
     */
    private Set<String> getTokenFromPrincipal(Subject subject) {
        Set<String> tokenSet = new HashSet<>();
        List<String> principalNames = new ArrayList<>(subject.getPrincipals().size());

        for (Principal p : subject.getPrincipals()) {
            token = p.getName();
            if (token != null && !principalNames.contains(token)) {
                principalNames.add(token);
                if (LDAPUtils.isDN(token)) {
                    userDN = token;
                    token = DNUtils.DNtoName(token);
                } else if (!LDAPUtils.isDN(token) && userDN == null) {
                    userDN = token;
                }
            }

            if (!tokenSet.contains(token)) {
                tokenSet.add(token);
            }

            if (DEBUG.messageEnabled()) {
                DEBUG.message("principal name is... :" + this.token);
            }
        }

        principalList = Joiner.on('|').join(principalNames);

        if (DEBUG.messageEnabled()) {
            DEBUG.message("Principal List is :" + principalList);
        }

        return tokenSet;
    }

    /**
     * Returns <code>true</code> if user is active.
     *
     * @return <code>true</code> if user is active.
     */
    boolean isUserEnabled() {
        return userEnabled;
    }

    /**
     * Returns the DN for role.
     *
     * @param roleName Name of role.
     * @return the DN for role.
     */
    private AMIdentity getIdentityRole(String roleName) {
        if (amIdentityRole == null) {
            amIdentityRole = searchIdentityRole(roleName);
        }
        return amIdentityRole;
    }

    /**
     * Returns Identity Role.
     *
     * @param role  Name of role.
     * @return Identity Role.
     */
    private AMIdentity searchIdentityRole(String role) {
        if (DEBUG.messageEnabled()) {
            DEBUG.message("rolename : " + role);
        }
        if (role == null) {
            return null;
        }

        AMIdentity amIdRole = null;

        try {
            // search for this role name in organization
            amIdRole = getRole(role);
        } catch (Exception e) {
            DEBUG.error("getRole: Error : ", e);
        }

        return amIdRole;
    }

    /**
     * Sets auth module name.
     *
     * @param authMethName Module Name.
     */
    void setAuthModuleName(String authMethName) {
        if (DEBUG.messageEnabled()) {
            DEBUG.message("authethName" + authMethName);
            DEBUG.message("pAuthMethName " + pAuthMethName);
        }
        StringBuilder sb = null;
        if ((this.pAuthMethName != null) && (this.pAuthMethName.length() > 0)) {
            sb = new StringBuilder().append(this.pAuthMethName);
        }
        if ((authMethName != null) && (authMethName.length() > 0)) {
            if (sb != null) {
                sb.append(ISAuthConstants.PIPE_SEPARATOR).append(authMethName);
            } else {
                sb = new StringBuilder().append(authMethName);
            }
        }
        if (sb != null) {
            this.authMethName = sb.toString();
        }
        if (DEBUG.messageEnabled()) {
            DEBUG.message("setAuthModuleName: " + this.authMethName);
        }
    }

    /**
     * Get the auth module name(s).
     *
     * @return The list of auth module names in a pipe separated string.
     */
    public String getAuthModuleNames() {
        return authMethName;
    }

    /**
     * Returns <code>true</code> if the user belongs to role.
     *
     * @param amIdentityRole Role object.
     * @return <code>true</code> if the user belongs to role.
     */
    private boolean getUserForRole(AMIdentity amIdentityRole) {

        boolean foundUser = false;
        try {
            if (amIdentityUser.isMember(amIdentityRole)) {
                foundUser = true;
            }
        } catch (Exception e) {
            if (DEBUG.messageEnabled()) {
                DEBUG.message("Error getRoleName : ", e);
            }
        }
        return foundUser;
    }

    /**
     * Returns the configuration for whether Zero Page Login (ZPL) should be allowed or not.
     *
     * @return the ZPL configuration
     */
    public ZeroPageLoginConfig getZeroPageLoginConfig() {
        return zeroPageLoginConfig;
    }

    void setToken(String token) {
        this.token = token;
    }

    /**
     * Return saved request parameters in <code>Hashtable</code>
     *
     * @return saved request parameters in <code>Hashtable</code>
     */
    public Map<String, String> getRequestParamHash() {
        return requestHash;
    }

    /* check if the user list has inactive user */
    private boolean getUserEnabled(Map<String, Boolean> userEnabledMap) {
        userEnabled = !userEnabledMap.containsValue(Boolean.FALSE);

        return userEnabled;
    }

    /* check if the users belong to role */
    private boolean getUserRoleFound(Map<String, Boolean> userRoleFoundMap) {
        boolean userRoleFound = true;
        if (userRoleFoundMap.containsValue(Boolean.FALSE)) {
            userRoleFound = false;
        }
        return userRoleFound;
    }

    /* check if the users map to the same user */
    private boolean getFoundUserAlias(Map<String, Boolean> foundAliasMap) {
        if (DEBUG.messageEnabled()) {
            DEBUG.message("foundAliasMap :" + foundAliasMap);
        }
        boolean foundUserAlias = true;

        if (foundAliasMap == null || foundAliasMap.isEmpty() ||
                foundAliasMap.containsValue(Boolean.FALSE)) {
            foundUserAlias = false;
        }
        if (DEBUG.messageEnabled()) {
            DEBUG.message("foundUserAlias : " + foundUserAlias);
        }
        return foundUserAlias;
    }

    /* check if profile for the user was retrieve */
    private boolean getGotUserProfile(Map<String, Boolean> gotUserProfileMap) {
        if (DEBUG.messageEnabled()) {
            DEBUG.message("GotUserProfileMAP is: " + gotUserProfileMap);
        }
        boolean gotUserProfile = false;
        if (gotUserProfileMap.containsValue(Boolean.TRUE)) {
            gotUserProfile = true;
        }
        if (DEBUG.messageEnabled()) {
            DEBUG.message("gotUserProfile :" + gotUserProfile);
        }
        return gotUserProfile;
    }

    /* add token to iplanet-am-user-alias-list of the token which has
     * a profile
     */
    private void addAliasToUserProfile(String token, Map<String, Boolean> foundUserAliasMap)
            throws AuthException {
        if (DEBUG.messageEnabled()) {
            DEBUG.message("Token : " + token);
        }

        AMIdentity amIdentityUser =
                LazyConfig.AUTHD.getIdentity(IdType.USER, token, getOrgDN());
        addAliasToUserProfile(amIdentityUser, foundUserAliasMap);
    }

    /* add token to iplanet-am-user-alias-list of the identity which has
     * a profile
     */
    private void addAliasToUserProfile(AMIdentity amIdentity, Map<String, Boolean> foundUserAliasMap) {

        if (DEBUG.messageEnabled()) {
            DEBUG.message("foundUserAliasMap : " + foundUserAliasMap);
        }

        try {
            if ((foundUserAliasMap != null) && !foundUserAliasMap.isEmpty()) {
                // set alias attribute
                Set<String> aliasKeySet = foundUserAliasMap.keySet();
                Iterator<String> aliasIterator = aliasKeySet.iterator();
                while (aliasIterator.hasNext()) {
                    String token1 = aliasIterator.next();
                    if ((token != null && !token.equalsIgnoreCase(token1)) &&
                            (!userAliasList.contains(token1))) {
                        userAliasList.add(token1);
                    }
                }
                DEBUG.message("Adding alias list to user profile");
                Map<String, Set<String>> aliasMap = new HashMap<String, Set<String>>();
                if ((externalAliasList != null)
                        && (!externalAliasList.isEmpty())) {
                    userAliasList.addAll(externalAliasList);
                }
                aliasMap.put(ISAuthConstants.USER_ALIAS_ATTR, userAliasList);
                amIdentity.setAttributes(aliasMap);
                amIdentity.store();
            }
        } catch (Exception e) {
            DEBUG.error("Exception : " + e.getMessage(), e);
        }
    }

    /* check alias list for tokens , if superAdmin token
     * exists then amAdmin need not exist in the user-alias-list
     */
    private boolean checkAliasList(Map<String, Boolean> userAliasList) {

        if (DEBUG.messageEnabled()) {
            DEBUG.message("UserAliasList is.. : " + userAliasList);
        }
        boolean aliasFound = true;
        Set<String> aliasKeySet = userAliasList.keySet();
        Iterator<String> aliasIterator = aliasKeySet.iterator();
        while (aliasIterator.hasNext()) {
            Object token1 = aliasIterator.next();
            if (DEBUG.messageEnabled()) {
                DEBUG.message("Token is.. : " + token1);
            }
            String newToken = tokenToDN((String) token1);
            if (!LazyConfig.AUTHD.isSuperAdmin(newToken)) {
                Boolean val = userAliasList.get(token1);
                if (val.toString().equals("false")) {
                    aliasFound = false;
                    break;
                }
            }
        }
        return aliasFound;
    }

    /**
     * set Load Balance Cookie
     *
     * @param cookieDomain name of cookie domain for persistent cookie
     * @return persistent cookie in request
     * @throws SSOException
     * @throws AMException
     */
    Cookie setlbCookie(String cookieDomain)
            throws SSOException, AMException {
        String cookieName = AuthUtils.getlbCookieName();
        String cookieValue = AuthUtils.getlbCookieValue();
        Cookie lbCookie = AuthUtils.createCookie(cookieName, cookieValue, -1, cookieDomain);
        return lbCookie;
    }

    /**
     * Returns the current index type.
     *
     * @return the current index type.
     */
    public AuthContext.IndexType getIndexType() {
        return indexType;
    }

    /**
     * Sets the indexType.
     *
     * @param indexType name of indexType to be set
     */
    void setIndexType(AuthContext.IndexType indexType) {
        this.indexType = indexType;
    }

    /**
     * Returns the previous index type in authentication level after module
     * selection.
     *
     * @return the previous index type in authentication level after module
     * selection.
     */
    AuthContext.IndexType getPreviousIndexType() {
        return prevIndexType;
    }

    /**
     * Sets the previous index type in authlevel after the choice is made.
     *
     * @param prevIndexType name of indexType to be set
     */
    void setPreviousIndexType(AuthContext.IndexType prevIndexType) {
        this.prevIndexType = prevIndexType;
    }

    /**
     * Sets gotoOnFail URL.
     */
    private void setDecodedGoToOnFailURL() {
        String arg = (String) requestHash.get("gotoOnFail");
        if (arg != null && arg.length() != 0) {
            gotoOnFailURL = arg;
        }
    }

    /**
     * Returns success login URL.
     *
     * @return success login URL.
     */
    public String getSuccessLoginURL() {
        String postProcessGoto = AuthUtils.getPostProcessURL(servletRequest,
                AMPostAuthProcessInterface.POST_PROCESS_LOGIN_SUCCESS_URL);
        //check from postAuthModule URL is set
        //if not try to retrieve it from session property
        //Success URL from Post Auth takes precedence
        InternalSession session = getReferencedSession();
        if ((postProcessGoto == null) && (session != null))
            postProcessGoto =
                    session.getProperty(ISAuthConstants.POST_PROCESS_SUCCESS_URL);
        if ((postProcessGoto != null) && (postProcessGoto.length() > 0)) {
            return postProcessGoto;
        }
        String currentGoto = (servletRequest == null) ?
                null : servletRequest.getParameter("goto");
        if (DEBUG.messageEnabled()) {
            DEBUG.message("currentGoto : " + currentGoto);
        }
        if ((currentGoto != null) && (currentGoto.length() != 0) &&
                (!currentGoto.equalsIgnoreCase("null"))) {
            String encoded = servletRequest.getParameter("encoded");
            if (encoded != null && encoded.equals("true")) {
                String decodedGoTo = Base64.decodeAsUTF8String(currentGoto);
                if (decodedGoTo == null && DEBUG.warningEnabled()) {
                    DEBUG.warning("As parameter 'encoded' is true, goto '{}' should be base64 encoded", currentGoto);
                }
                currentGoto = decodedGoTo;
            }
            if (!LazyConfig.AUTHD.isGotoUrlValid(currentGoto, getOrgDN())) {
                if (DEBUG.messageEnabled()) {
                    DEBUG.message("LoginState.getSuccessLoginURL():Original goto URL is " + currentGoto
                            + " which is invalid");
                }
                currentGoto = null;
            }
        }

        String fqdnURL;
        if ((currentGoto != null) && (currentGoto.length() != 0) && (!currentGoto.equalsIgnoreCase("null"))) {
            fqdnURL = LazyConfig.AUTHD.processURL(currentGoto, servletRequest);
        } else {
            fqdnURL = LazyConfig.AUTHD.processURL(successLoginURL, servletRequest);
        }

        String encodedSuccessURL = encodeURL(fqdnURL, true);
        if (DEBUG.messageEnabled()) {
            DEBUG.message("get fqdnURL : " + fqdnURL);
            DEBUG.message("get successLoginURL : "
                    + successLoginURL);
            DEBUG.message("get encodedSuccessURL : "
                    + encodedSuccessURL);
        }
        return encodedSuccessURL;
    }

    /**
     * Sets success login URL.
     *
     * @param url success login URL.
     */
    public void setSuccessLoginURL(String url) {
        /* this is for AMLoginModule to set the success url */
        if (DEBUG.messageEnabled()) {
            DEBUG.message("URL : from modle  : " + url);
        }
        moduleSuccessLoginURL = url;
    }

    private String getSuccessURLForRole() {
        String roleURL = null;
        try {
            Map roleAttrMap = getRoleServiceAttributes();
            roleURL = getRoleURLFromAttribute(roleAttrMap,
                    ISAuthConstants.LOGIN_SUCCESS_URL);
        } catch (Exception e) {
            if (DEBUG.messageEnabled()) {
                DEBUG.message("Execption:getSuccessURLForRole : ", e);
            }
        }

        return roleURL;
    }

    private String getFailureURLForRole() {

        String roleFailureURL = null;
        try {
            Map roleAttrMap = getRoleServiceAttributes();
            roleFailureURL = getRoleURLFromAttribute(roleAttrMap,
                    ISAuthConstants.LOGIN_FAILURE_URL);
        } catch (Exception e) {
            if (DEBUG.messageEnabled()) {
                DEBUG.message("Error retrieving url ");
                DEBUG.message("Exception : ", e);
            }
        }
        return roleFailureURL;
    }

    /**
     * Returns the AMTemplate for role.
     *
     * @return the AMTemplate for role.
     * @throws Exception if fails to get role attribute
     */
    private Map<String, Set<String>> getRoleServiceAttributes() throws Exception {
        try {
            if (roleAttributeMap == null) {
                Map roleServiceAttrMap = amIdentityRole.
                        getServiceAttributes(
                                ISAuthConstants.AUTHCONFIG_SERVICE_NAME);
                String serviceName = (String) ((Set) roleServiceAttrMap.get(
                        AMAuthConfigUtils.ATTR_NAME)).iterator().next();
                if ((serviceName != null) &&
                        (!serviceName.equals(ISAuthConstants.BLANK))) {
                    roleAuthConfig = serviceName;
                    roleAttributeMap = getServiceAttributes(serviceName);
                }
            }
            if (roleAttributeMap == null) {
                roleAttributeMap = Collections.emptyMap();
            }
            if (DEBUG.messageEnabled()) {
                DEBUG.message("Returning Service Attributes: " +
                        roleAttributeMap);
                DEBUG.message("for Role : " + amIdentityRole.getName());
            }

            return roleAttributeMap;
        } catch (Exception e) {
            DEBUG.error("Error getting Role Attributes : ", e);
            throw new Exception(AMAuthErrorCode.AUTH_ERROR);
        }
    }

    /**
     * Returns success url for a service.
     *
     * @param indexName name of auth index
     * @return success url for a service.
     */
    private String getSuccessURLForService(String indexName) {

        String successServiceURL = null;

        try {
            if ((serviceAttributesMap != null)
                    && (serviceAttributesMap.isEmpty())) {
                serviceAttributesMap = getServiceAttributes(indexName);
            }

            if (DEBUG.messageEnabled()) {
                DEBUG.message("AttributeMAP is.. :" + serviceAttributesMap);
            }

            successServiceURL = getServiceURLFromAttribute(
                    serviceAttributesMap, ISAuthConstants.LOGIN_SUCCESS_URL);

            if (DEBUG.messageEnabled()) {
                DEBUG.message("service successURL : " + successServiceURL);
            }
        } catch (Exception e) {
            if (DEBUG.messageEnabled()) {
                DEBUG.message("Error retrieving url ");
                DEBUG.message("Exception : ", e);
            }
        }

        return successServiceURL;
    }

    /**
     * Returns the service login failure URL.
     *
     * @param indexName name of auth index
     * @return the service login failure URL.
     */
    private String getFailureURLForService(String indexName) {
        String serviceFailureURL = null;
        try {
            if (serviceAttributesMap.isEmpty()) {
                serviceAttributesMap = getServiceAttributes(indexName);
            }
            serviceFailureURL = getServiceURLFromAttribute(
                    serviceAttributesMap, ISAuthConstants.LOGIN_FAILURE_URL);

            if (DEBUG.messageEnabled()) {
                DEBUG.message("Service failureURL: " + serviceFailureURL);
            }
        } catch (Exception e) {
            if (DEBUG.messageEnabled()) {
                DEBUG.message("Error retrieving url ");
                DEBUG.message("Exception : ", e);
            }
        }

        return serviceFailureURL;
    }

    /**
     * Returns service login url attributes.
     *
     * @param indexName name of auth index
     * @return service login url attributes.
     * @throws Exception if fails to get service attribute
     */
    private Map<String, Set<String>> getServiceAttributes(String indexName) throws Exception {

        try {
            return AuthenticationServiceAttributeCache.getServiceAttribute(orgDN, indexName);
        } catch (Exception e) {
            if (DEBUG.messageEnabled()) {
                DEBUG.message("Error getting service attribute: ");
                DEBUG.message(" Exception : " + e.getMessage());
            }
            throw new Exception(e.getMessage());
        }
    }

    /* create an instance of PostLoginProcessInterface Class */
    private AMPostAuthProcessInterface getPostLoginProcessInstance(String className) {
        if (DEBUG.messageEnabled()) {
            DEBUG.message("postLoginProcess Class Name is : " + className);
        }
        if ((className == null) || (className.length() == 0)) {
            return null;
        }
        try {
            AMPostAuthProcessInterface loginPostProcessInstance =
                    (AMPostAuthProcessInterface)
                            (Class.forName(className).newInstance());
            return loginPostProcessInstance;
        } catch (ClassNotFoundException ce) {
            if (DEBUG.messageEnabled()) {
                DEBUG.message("Class not Found :", ce);
            }
            return null;
        } catch (Exception e) {
            if (DEBUG.messageEnabled()) {
                DEBUG.message("Error: ", e);
            }
            return null;
        }
    }

    /**
     * this is called by AMLoginContext on a successful authentication to set
     * the <code>successURL</code> based on the <code>indexType</code>,
     * <code>indexName</code> order to figure out Success Login URL  is :
     * Authentication Method Success URL Order
     * <pre>
     * ==========================================================================
     * Org,Level,Module,User
     *   1. set by module
     *         2. goto parameter
     *         3. URL set for the clientType in iplanet-am-user-success-url
     *            in User Entry
     *         4. URL set for the clientType in
     *            iplanet-am-auth-login-success-url set at user's Role
     *         5. URL set for clientType in iplanet-am-auth-login-success-url
     *            set at Org
     *         6. URL set for clientType in iplanet-am-auth-login-success-url
     *            - Organization Schema
     *         7. iplanet-am-user-success-url set in User Entry
     *         8. iplanet-am-auth-login-success-url set at user's Role
     *         9. iplanet-am-auth-login-success-url set at Org
     *        10. iplanet-am-auth-login-success-url - Organization Schema
     *
     * Role
     *   1. set by module
     *   2. goto parameter
     *   3. URL matching clientType in iplanet-am-user-success-url
     *            set in User Entry
     *         4. URL matching clientType in iplanet-am-auth-login-success-url
     *            set at the role user authenticates to.
     *   5. URL matching clientType in iplanet-am-auth-login-success-url
     *            set at user's Role
     *   6. URL matching clientType in iplanet-am-auth-login-success-url
     *            set at Org
     *   7. URL matching clientType in iplanet-am-auth-login-success-url
     *            - Organization Schema
     *   8. iplanet-am-user-success-url set in User Entry
     *         9. iplanet-am-auth-login-success-url set at the
     *            role user authenticates to.
     *  10. iplanet-am-auth-login-success-url set at user's Role
     *  11. iplanet-am-auth-login-success-url set at Org
     *        12. iplanet-am-auth-login-success-url - Organization Schema
     *
     * Service
     *   1. set by module
     *   2. goto parameter
     *   3. URL matching clientType in iplanet-am-user-success-url
     *            set in User Entry
     *   4. URL matching clientType in iplanet-am-auth-login-success-url
     *            set at the service entry.
     *   5. URL matching clientType in iplanet-am-auth-login-success-url
     *            set at user's Role
     *   6. URL matching clientType in iplanet-am-auth-login-success-url
     *            set at Org
     *   7. URL matchhing clientType in iplanet-am-auth-login-success-url
     *            - Organization Schema
     *   8. iplanet-am-user-success-url set in User Entry
     *   9. iplanet-am-auth-login-success-url set at the service entry.
     *  10. iplanet-am-auth-login-success-url set at user's Role
     *  11. iplanet-am-auth-login-success-url set at Org
     *  12. iplanet-am-auth-login-success-url - Organization Schema
     * </pre>
     * NOTE: same order used for failure URL.
     *
     * @param indexType
     * @param indexName
     */
    private void setSuccessLoginURL(
            AuthContext.IndexType indexType,
            String indexName) {
        /* if module sets  the url then return the URL module set */
        if (DEBUG.messageEnabled()) {
            DEBUG.message("moduleSucessLoginURL : " + moduleSuccessLoginURL);
        }
        if ((moduleSuccessLoginURL != null) &&
                (moduleSuccessLoginURL.length() != 0)) {
            successLoginURL = moduleSuccessLoginURL;
            return;
        }

        /* if goto parameter was specified  then return the gotoURL */
        /*if ( (gotoURL != null) && (!gotoURL.length() == 0) ) {
            successLoginURL = gotoURL;
            return ;
        }*/

        String defSuccessURL = null;
        /* if user profile has successurl set for the client type then return
         * else get the default user url and continue
         */
        if ((clientUserSuccessURL != null) &&
                (clientUserSuccessURL.length() != 0)
                ) {
            successLoginURL = clientUserSuccessURL;
            if (successLoginURL != null) {
                return;
            }
        }

        defSuccessURL = defaultUserSuccessURL;
        if (indexType == AuthContext.IndexType.ROLE) {
            String successURL = getSuccessURLForRole();
            if ((successURL != null) && (successURL.length() != 0)) {
                successLoginURL = successURL;
                return;
            }
            if (defSuccessURL == null || defSuccessURL.length() == 0) {
                defSuccessURL = tempDefaultURL;
            }
        }

        if (indexType == AuthContext.IndexType.SERVICE) {
            String successURL = getSuccessURLForService(indexName);
            if ((successURL != null) && (successURL.length() != 0)) {
                successLoginURL = successURL;
                return;
            }
            if (defSuccessURL == null || defSuccessURL.length() == 0) {
                defSuccessURL = tempDefaultURL;
            }
        }

        if ((clientSuccessRoleURL != null) &&
                (clientSuccessRoleURL.length() != 0)
                ) {
            successLoginURL = clientSuccessRoleURL;
            return;
        }

        if (defSuccessURL == null || defSuccessURL.length() == 0) {
            defSuccessURL = defaultSuccessRoleURL;
        }

        if ((clientOrgSuccessLoginURL != null) &&
                (clientOrgSuccessLoginURL.length() != 0)
                ) {
            successLoginURL = clientOrgSuccessLoginURL;
            return;
        }

        if (defSuccessURL == null || defSuccessURL.length() == 0) {
            defSuccessURL = defaultOrgSuccessLoginURL;
        }

        // get global default
        String defaultSuccessURL;
        if (indexType == AuthContext.IndexType.SERVICE ||
                indexType == AuthContext.IndexType.ROLE) {
            defaultSuccessURL = getRedirectUrl(LazyConfig.AUTHD.getDefaultServiceSuccessURLSet());
        } else {
            defaultSuccessURL = getRedirectUrl(LazyConfig.AUTHD.getDefaultSuccessURLSet());
            LazyConfig.AUTHD.setDefaultSuccessURL(tempDefaultURL);
        }

        if ((defaultSuccessURL != null) && (defaultSuccessURL.length() != 0)) {
            successLoginURL = defaultSuccessURL;
            return;
        }

        if (defSuccessURL == null || defSuccessURL.length() == 0) {
            defSuccessURL = tempDefaultURL;
        }

        // now assign back to the success url
        successLoginURL = defSuccessURL;
        if (DEBUG.messageEnabled()) {
            DEBUG.message("SUCCESS Login url : " + successLoginURL);
        }
    }

    /**
     * Sets failure login URL.
     *
     * @param indexType
     * @param indexName
     */
    void setFailureLoginURL(
            AuthContext.IndexType indexType,
            String indexName) {
        /*
         * this is called by AMLoginContext on a failed authentication to set
         * the successURL based on the indexType,indexName
         */
        // if module set the url then return the URL module set
        if ((moduleFailureLoginURL != null) &&
                (moduleFailureLoginURL.length() != 0)) {
            failureLoginURL = moduleFailureLoginURL;
            return;
        }

        if (DEBUG.messageEnabled()) {
            DEBUG.message("failureTokenId in setFailureLoginURL = "
                    + getFailureTokenId());
        }

        String defFailureURL = null;
        /* if user profile has failure url set then return */

        if (getFailureTokenId() != null) {
            // get the user profile
            try {
                getUserProfile(getFailureTokenId(), true, false);
                if ((clientUserFailureURL != null) &&
                        (clientUserFailureURL.length() != 0)) {
                    failureLoginURL = clientUserFailureURL;
                    return;
                }
                defFailureURL = defaultUserFailureURL;
            } catch (Exception e) {
                if (DEBUG.messageEnabled()) {
                    DEBUG.message("Error retreiving profile for : " +
                            getFailureTokenId(), e);
                }
            }
        }

        if (indexType == AuthContext.IndexType.ROLE) {
            String failureURL = getFailureURLForRole();
            if ((failureURL != null) && (failureURL.length() != 0)) {
                failureLoginURL = failureURL;
                return;
            }

            if ((defFailureURL == null) || (defFailureURL.length() == 0)) {
                defFailureURL = tempDefaultURL;
            }
        }

        if (indexType == AuthContext.IndexType.SERVICE) {
            String failureURL = getFailureURLForService(indexName);
            if ((failureURL != null) && (failureURL.length() != 0)) {
                failureLoginURL = failureURL;
                return;
            }
            if ((defFailureURL == null) || (defFailureURL.length() == 0)) {
                defFailureURL = tempDefaultURL;
            }
        }

        if ((clientFailureRoleURL != null) &&
                (clientFailureRoleURL.length() != 0)
                ) {
            failureLoginURL = clientFailureRoleURL;
            return;
        }

        if ((defFailureURL == null) || (defFailureURL.length() == 0)) {
            defFailureURL = defaultFailureRoleURL;
        }

        if ((clientOrgFailureLoginURL != null) &&
                (clientOrgFailureLoginURL.length() != 0)
                ) {
            failureLoginURL = clientOrgFailureLoginURL;
            return;
        }
        if ((defFailureURL == null) || (defFailureURL.length() == 0)) {
            defFailureURL = defaultOrgFailureLoginURL;
        }

        String defaultFailureURL;
        if (indexType == AuthContext.IndexType.SERVICE ||
                indexType == AuthContext.IndexType.ROLE) {
            defaultFailureURL = getRedirectUrl(LazyConfig.AUTHD.getDefaultServiceFailureURLSet());
        } else {
            defaultFailureURL = getRedirectUrl(LazyConfig.AUTHD.getDefaultFailureURLSet());
            LazyConfig.AUTHD.setDefaultFailureURL(tempDefaultURL);
        }
        if ((defaultFailureURL != null) && (defaultFailureURL.length() != 0)) {
            failureLoginURL = defaultFailureURL;
            return;
        }

        if ((defFailureURL == null) || (defFailureURL.length() == 0)) {
            defFailureURL = tempDefaultURL;
        }
        // now assign back to the Failure url
        failureLoginURL = defFailureURL;
        if (DEBUG.messageEnabled()) {
            DEBUG.message("defaultFailureURL : " + failureLoginURL);
        }

        return;
    }

    /**
     * Returns failure login URL.
     *
     * @return failure login URL.
     */
    public String getFailureLoginURL() {
        String postProcessURL = AuthUtils.getPostProcessURL(servletRequest,
                AMPostAuthProcessInterface.POST_PROCESS_LOGIN_FAILURE_URL);
        if (postProcessURL != null) {
            return postProcessURL;
        }
        if (gotoOnFailURL != null && !gotoOnFailURL.isEmpty()) {
            if (!LazyConfig.AUTHD.isGotoUrlValid(gotoOnFailURL, getOrgDN())) {
                if (DEBUG.messageEnabled()) {
                    DEBUG.message("LoginState.getFailureLoginURL(): Original gotoOnFail URL is " + gotoOnFailURL
                            + " which is invalid");
                }
                gotoOnFailURL = null;
            }
        }
        if (gotoOnFailURL != null && !gotoOnFailURL.isEmpty() && !gotoOnFailURL.equalsIgnoreCase("null")) {
            fqdnFailureLoginURL = LazyConfig.AUTHD.processURL(gotoOnFailURL, servletRequest);
        } else if (fqdnFailureLoginURL == null || !fqdnFailureLoginURL.isEmpty()) {
            fqdnFailureLoginURL = LazyConfig.AUTHD.processURL(failureLoginURL, servletRequest);
        }
        return fqdnFailureLoginURL;
    }

    /**
     * Sets failure login URL.
     *
     * @param url failure login URL.
     */
    public void setFailureLoginURL(String url) {
        /* this is for AMLoginModule to set the failure url */
        moduleFailureLoginURL = url;
    }

    String getLogoutURL() {

        return AuthUtils.getPostProcessURL(servletRequest, AMPostAuthProcessInterface.POST_PROCESS_LOGOUT_URL);
    }

    /**
     * Returns the role login url attribute value.
     *
     * @param roleAttrMap map object has login url attribute
     * @param attrName    attribute name for login url
     * @return the role login url attribute value.
     */
    private String getRoleURLFromAttribute(Map roleAttrMap, String attrName) {
        try {
            Set roleURLSet = (Set) roleAttrMap.get(attrName);
            return getRedirectUrl(roleURLSet);
        } catch (Exception e) {
            if (DEBUG.messageEnabled()) {
                DEBUG.message("Error getting role attribute ", e);
            }
            return null;
        }
    }

    /**
     * Returns service url from attribute value.
     *
     * @param attributeMap map object has service url attribute
     * @param attrName     attribute name for service url
     * @return service url from attribute value.
     */
    private String getServiceURLFromAttribute(Map attributeMap, String attrName) {
        Set serviceURLSet =
                (Set) attributeMap.get(attrName);

        String serviceURL = getRedirectUrl(serviceURLSet);
        if (DEBUG.messageEnabled()) {
            DEBUG.message("attr map: " + attributeMap +
                    "\nserviceURL : " + serviceURL);
        }
        return serviceURL;
    }

    /**
     * Returns servlet response object.
     *
     * @return servlet response object.
     */
    public HttpServletResponse getHttpServletResponse() {
        return servletResponse;
    }

    /**
     * Sets servlet response.
     *
     * @param servletResponse servletResponse object to be set
     */
    public void setHttpServletResponse(HttpServletResponse servletResponse) {
        this.servletResponse = servletResponse;
    }

    /**
     * Return previously received callback
     *
     * @return previously received callback
     */
    Callback[] getRecdCallback() {
        return prevCallback;
    }

    /**
     * Set previously received callback
     *
     * @param prevCallback previously received callback
     */
    synchronized void setPrevCallback(Callback[] prevCallback) {
        this.prevCallback = prevCallback;
    }

    /**
     * @return <code>true</code> if noSession mode was enabled in the request.
     */
    boolean isNoSession() {
        return Boolean.parseBoolean(requestMap.get(NO_SESSION_QUERY_PARAM))
                || (servletRequest != null && Boolean.parseBoolean(
                (String) servletRequest.getAttribute(ISAuthConstants.NO_SESSION_REQUEST_ATTR)));
    }

    /**
     * Indicates loginFailureLockoutStoreInDS mode is enabled.
     */
    String getAccountLife() {
        return accountLife;
    }

    protected String getUserToken() {
        return token;
    }

    boolean getEnableModuleBasedAuth() {
        return enableModuleBasedAuth;
    }

    public boolean getLoginFailureLockoutMode() {
        return isLoginFailureLockoutMode();
    }

    public boolean getLoginFailureLockoutStoreInDS() {
        return isLoginFailureLockoutStoreInDS();
    }

    /**
     * Default max time for loginFailureLockout.
     */
    public long getLoginFailureLockoutTime() {
        return loginFailureLockoutTime;
    }

    /**
     * Default count for loginFailureLockout.
     */
    public int getLoginFailureLockoutCount() {
        return loginFailureLockoutCount;
    }

    /**
     * Default notification for loginFailureLockout.
     */
    public String getLoginLockoutNotification() {
        return loginLockoutNotification;
    }

    public void incrementFailCount(String failedUserId) {
        if (failedUserId != null) {
            AMAccountLockout amAccountLockout = new AMAccountLockout(this);
            boolean accountLocked = amAccountLockout.isLockedOut(failedUserId);

            if ((!accountLocked) && (amAccountLockout.isLockoutEnabled())) {
                amAccountLockout.invalidPasswd(failedUserId);

                if (DEBUG.messageEnabled()) {
                    DEBUG.message("LoginState::incrementFailCount incremented fail count for " + failedUserId);
                }
            }
        } else {
            DEBUG.error("LoginState::incrementFailCount called with null user id");
        }
    }

    public boolean isAccountLocked(String username) {
        AMAccountLockout amAccountLockout = new AMAccountLockout(this);
        return amAccountLockout.isLockedOut(username) || amAccountLockout.isAccountLocked(username);
    }

    /**
     * Default number of count for loginFailureLockout warning
     */ /**
     * Returns lockout warning message.
     *
     * @return lockout warning message.
     */
    public int getLoginLockoutUserWarning() {
        return loginLockoutUserWarning;
    }

    /**
     * Returns the error code .
     *
     * @return the error code .
     */
    public String getErrorCode() {
        return errorCode;
    }

    /**
     * Sets the error code.
     *
     * @param errorCode Error code.
     */
    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    /**
     * Returns the error message.
     *
     * @return the error message.
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * Sets the error message.
     *
     * @param errorMessage Error message.
     */
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    /**
     * Returns the error template generated by framework.
     *
     * @return the error template generated by framework.
     */
    public String getErrorTemplate() {
        return errorTemplate;
    }

    /**
     * Sets the error template generated by framework.
     *
     * @param errorTemplate Error template.
     */
    public void setErrorTemplate(String errorTemplate) {
        this.errorTemplate = errorTemplate;
    }

    /**
     * Returns the error template set by module.
     *
     * @return Error template set by module.
     */
    String getModuleErrorTemplate() {
        return moduleErrorTemplate;
    }

    /**
     * Sets the error module template sent by login module.
     *
     * @param moduleErrorTemplate Module error template.
     */
    public void setModuleErrorTemplate(String moduleErrorTemplate) {
        this.moduleErrorTemplate = moduleErrorTemplate;
    }

    /**
     * Returns <code>true</code> if page times out.
     *
     * @return <code>true</code> if page times out.
     */
    public boolean isTimedOut() {
        return timedOut;
    }

    /**
     * Sets the time out value.
     *
     * @param timedOut <code>true</code> to set timed out.
     */
    void setTimedOut(boolean timedOut) {
        this.timedOut = timedOut;
    }

    /**
     * Returns the lockout message.
     *
     * @return the lockout message.
     */
    public String getLockoutMsg() {
        return lockoutMsg;
    }

    /**
     * Sets the lockout message.
     *
     * @param lockoutMsg the lockout message.
     */
    public void setLockoutMsg(String lockoutMsg) {
        if (DEBUG.messageEnabled()) {
            DEBUG.message("setLockoutMsg :" + lockoutMsg);
        }
        this.lockoutMsg = lockoutMsg;
    }

    /**
     * Returns the index name.
     *
     * @return the index name.
     */
    public String getIndexName() {
        return this.indexName;
    }

    /**
     * Set index name
     *
     * @param indexName indexName to be set
     */
    public void setIndexName(String indexName) {
        this.indexName = indexName;
    }

    /**
     * Creates <code>AuthContextLocal</code> for new requests.
     *
     * @return the created <code>AuthContextLocal</code>
     * @throws AuthException if fails to create <code>AuthContextLocal</code>
     */
    AuthContextLocal createAuthContext(
            SessionID sid,
            String orgName,
            HttpServletRequest req
    ) throws AuthException {
        this.userOrg = getDomainNameByOrg(orgName);

        if (DEBUG.messageEnabled()) {
            DEBUG.message("createAuthContext: userOrg is : " + userOrg);
        }

        if ((this.userOrg == null) || (this.userOrg.equals(""))) {
            DEBUG.error("domain is null, error condtion");
            logFailed(LazyConfig.AUTHD.bundle.getString("invalidDomain"), "INVALIDDOMAIN");
            auditor.auditLoginFailure(this, INVALID_REALM);
            throw new AuthException(AMAuthErrorCode.AUTH_INVALID_DOMAIN, null);
        }

        if (DEBUG.messageEnabled()) {
            DEBUG.message("AuthUtil::getAuthContext::Creating new " +
                    "AuthContextLocal & LoginState");
        }
        AuthContextLocal authContext = new AuthContextLocal(this.userOrg);
        newRequest = true;
        this.finalSessionId = sid;
        if (DEBUG.messageEnabled()) {
            DEBUG.message("requestType : " + newRequest);
            DEBUG.message("sid : " + sid);
            DEBUG.message("orgName passed: " + orgName);
        }

        try {
            createSession(req, authContext);
        } catch (Exception e) {
            DEBUG.error("Exception creating session .. :", e);
            throw new AuthException(AMAuthErrorCode.AUTH_ERROR, null);
        }
        amIdRepo = LazyConfig.AUTHD.getAMIdentityRepository(getOrgDN());
        populateOrgProfile();
        isLocaleSet = false;
        return authContext;
    }

    /**
     * Sets the module <code>AuthLevel</code>.
     * The authentication level being set cannot be downgraded
     * below that set by the module configuration.This method
     * is called by <code>AMLoginModule</code> SPI
     *
     * @param authLevel authentication level string to be set
     * @return <code>true</code> if setting is successful, false otherwise
     */
    public boolean setModuleAuthLevel(int authLevel) {
        boolean levelSet = false;
        moduleAuthLevel = authLevel;
        if (this.authLevel < moduleAuthLevel) {
            this.authLevel = moduleAuthLevel;
            levelSet = true;
        }
        if (DEBUG.messageEnabled()) {
            DEBUG.message("spi authLevel :" + authLevel);
            DEBUG.message(
                    "module configuration authLevel :" + this.authLevel);
            DEBUG.message("levelSet :" + levelSet);
        }
        return levelSet;
    }

    /**
     * This method is used for requests coming from the
     * <code>AuthContext</code> API to determine the <code>orgDN</code> of the
     * request. The <code>orgDN</code> is determined based on and in order:
     * <pre>
     * 1. Domain - check if org is a domain by trying to get domain component
     * 2. OrgDN/path- check if the orgName passed is a path (eg."/suborg1")
     * 3. URL - check if the orgName passed is a DNS alias (URL).
     * 4. If no orgDN is found null is returned.
     * </pre>
     *
     * @param orgName is the name of the Organization or Suborganzation whose
     *                <code>orgDN</code> is to be determined.
     * @return a String which is the orgDN of the request
     */
    String getDomainNameByOrg(String orgName) {
        String orgDN = null;
        try {
            orgDN = AuthUtils.getOrganizationDN(orgName, false, null);

        } catch (Exception e) {
            if (DEBUG.messageEnabled()) {
                DEBUG.message("Incorrect orgName passed:" + orgName, e);
            }
        }
        return orgDN;
    }

    /**
     * Returns the module instances for a organization.
     *
     * @return the module instances for a organization.
     */
    Set<String> getModuleInstances() {
        try {

            if ((moduleInstances != null) && (!moduleInstances.isEmpty())) {
                return moduleInstances;
            }

            moduleInstances = domainAuthenticators;

            if (DEBUG.messageEnabled()) {
                DEBUG.message("moduleInstances are : " + moduleInstances);
            }
        } catch (Exception e) {
            if (DEBUG.messageEnabled()) {
                DEBUG.message("Error getting moduleInstances ", e);
            }
        }

        if (moduleInstances == null) {
            moduleInstances = Collections.emptySet();
        }

        return moduleInstances;
    }

    /**
     * Returns the allowed authentication modules for organization
     *
     * @return the allowed authentication modules for organization
     */
    Set<String> getDomainAuthenticators() {
        return domainAuthenticators;
    }

    /**
     * Returns the X509 certificate.
     *
     * @return the X509 certificate.
     */
    public X509Certificate getX509Certificate(HttpServletRequest servletrequest) {
        if ((servletrequest != null) && (servletrequest.isSecure())) {
            Object obj = servletrequest.getAttribute(
                    "javax.servlet.request.X509Certificate");
            X509Certificate[] allCerts = (X509Certificate[]) obj;
            if ((allCerts != null) && (allCerts.length != 0)) {
                if (DEBUG.messageEnabled()) {
                    DEBUG.message("LoginState.getX509Certificate :" +
                            "length of cert array " + allCerts.length);
                }
                cert = allCerts[0];
            }
        }

        return cert;
    }

    /**
     * TODO-JAVADOC
     */
    void logSuccess() {
        try {
            String logSuccess = LazyConfig.AUTHD.bundle.getString("loginSuccess");
            ArrayList<String> dataList = new ArrayList<String>();
            dataList.add(logSuccess);
            StringBuilder messageId = new StringBuilder();
            messageId.append("LOGIN_SUCCESS");
            if (indexType != null) {
                messageId.append("_").append(indexType.toString()
                        .toUpperCase());
                dataList.add(indexType.toString());
                if (indexName != null) {
                    dataList.add(indexName);
                }
            }
            dataList.add("isNoSession=" + isNoSession());
            String[] data = dataList.toArray(new String[dataList.size()]);
            String contextId = null;
            SSOToken localSSOToken = null;
            if (!isNoSession()) {
                localSSOToken = getSSOToken();
            }
            if (localSSOToken != null) {
                contextId = localSSOToken.getProperty(Constants.AM_CTX_ID);
            }

            Hashtable<String, String> props = new Hashtable<String, String>();
            if (client != null) {
                props.put(LogConstants.IP_ADDR, client);
            }
            if (userDN != null) {
                props.put(LogConstants.LOGIN_ID, userDN);
            }
            if (orgDN != null) {
                props.put(LogConstants.DOMAIN, orgDN);
            }
            if (authMethName != null) {
                props.put(LogConstants.MODULE_NAME, authMethName);
            }
            InternalSession session = getReferencedSession();
            if (session != null) {
                props.put(LogConstants.LOGIN_ID_SID, finalSessionId.toString());
            }
            if (contextId != null) {
                props.put(LogConstants.CONTEXT_ID, contextId);
            }

            LazyConfig.AUTHD.logIt(data, AuthD.LOG_ACCESS, messageId.toString(), props);
        } catch (Exception e) {
            DEBUG.message("Error creating logSuccess message", e);
        }

    }

    /**
     * Adds log message to authentication access log.
     *
     * @param msgId I18n key of the localized message.
     * @param logId Logging message Id
     */
    public void logSuccess(String msgId, String logId) {
        try {
            String logSuccess = LazyConfig.AUTHD.bundle.getString(msgId);
            List<String> dataList = new ArrayList<String>();
            dataList.add(logSuccess);

            dataList.add("isNoSession=" + isNoSession());
            String[] data = dataList.toArray(new String[dataList.size()]);

            Hashtable<String, String> props = new Hashtable<String, String>();
            if (client != null) {
                props.put(LogConstants.IP_ADDR, client);
            }
            if (userDN != null) {
                props.put(LogConstants.LOGIN_ID, userDN);
            }
            if (orgDN != null) {
                props.put(LogConstants.DOMAIN, orgDN);
            }
            if (authMethName != null) {
                props.put(LogConstants.MODULE_NAME, authMethName);
            }
            InternalSession session = getReferencedSession();
            if (session != null) {
                props.put(LogConstants.LOGIN_ID_SID, finalSessionId.toString());
            }

            LazyConfig.AUTHD.logIt(data, AuthD.LOG_ACCESS, logId, props);
        } catch (Exception e) {
            DEBUG.message("Error creating logSuccess message", e);
        }
    }

    /**
     * Log login failed
     *
     * @param str message for login failed
     */
    void logFailed(String str) {
        logFailed(str, "LOGIN_FAILED", true, null);
    }

    /**
     * Log login failed
     *
     * @param str   message for login failed
     * @param error error message for login failed
     */
    void logFailed(String str, String error) {
        logFailed(str, "LOGIN_FAILED", true, error);
    }

    /**
     * Adds log message to authentication error log.
     *
     * @param str            localized message to be logged.
     * @param logId          logging message Id.
     * @param appendAuthType if true, append authentication type to the logId
     *                       to form new logging message Id. for example:
     *                       "LOGIN_FAILED_LEVEL".
     * @param error          error Id to be append to logId to form new logging
     *                       message Id. for example : "LOGIN_FAILED_LEVEL_INVALIDPASSWORD"
     */
    public void logFailed(String str, String logId, boolean appendAuthType,
                          String error) {

        try {
            String logFailed = str;
            if (str == null) {
                logFailed = LazyConfig.AUTHD.bundle.getString("loginFailed");
            }
            List<String> dataList = new ArrayList<String>();
            dataList.add(logFailed);
            StringBuilder messageId = new StringBuilder();
            messageId.append(logId);
            if ((indexType != null) &&
                    (indexType != AuthContext.IndexType.COMPOSITE_ADVICE)) {
                if (appendAuthType) {
                    messageId.append("_").append(indexType.toString()
                            .toUpperCase());
                }
                dataList.add(indexType.toString());
                if (indexName != null) {
                    dataList.add(indexName);
                }
            }
            if (error != null) {
                messageId.append("_").append(error);
            }
            String[] data = dataList.toArray(new String[dataList.size()]);
            String contextId = null;
            try {
                SSOToken localSSOToken = getSSOToken();
                if (localSSOToken != null) {
                    contextId = localSSOToken.getProperty(Constants.AM_CTX_ID);
                }
            } catch (SSOException ssoe) {
                DEBUG.message("Error while retrieving SSOToken for login failure: "
                        + ssoe.getMessage());
            }

            Hashtable<String, String> props = new Hashtable<String, String>();
            if (client != null) {
                props.put(LogConstants.IP_ADDR, client);
            }
            if (userDN != null) {
                props.put(LogConstants.LOGIN_ID, userDN);
            } else if (getFailureTokenId() != null) {
                props.put(LogConstants.LOGIN_ID, getFailureTokenId());
            } else if (callbacksPerState != null && !callbacksPerState.values().isEmpty()) {
                for (Callback[] cb : callbacksPerState.values()) {
                    for (Callback aCb : cb) {
                        if (aCb instanceof NameCallback && userDN==null && ((NameCallback) aCb).getName()!=null) {
                            userDN = ((NameCallback) aCb).getName();
                            if (DEBUG.messageEnabled()) {
                                DEBUG.message("userDN is null, setting to " + userDN);
                            }
                            props.put(LogConstants.LOGIN_ID, userDN);
                        }
                    }
                }
            }
            if (orgDN != null) {
                props.put(LogConstants.DOMAIN, orgDN);
            }
            if ((failureModuleList != null) &&
                    (failureModuleList.length() > 0)) {
                props.put(LogConstants.MODULE_NAME, failureModuleList);
            }
            InternalSession session = getReferencedSession();
            if (session != null) {
                props.put(LogConstants.LOGIN_ID_SID, finalSessionId.toString());
            }
            if (contextId != null) {
                props.put(LogConstants.CONTEXT_ID, contextId);
            }

            LazyConfig.AUTHD.logIt(data, LazyConfig.AUTHD.LOG_ERROR, messageId.toString(), props);
        } catch (Exception e) {
            DEBUG.error("Error creating logFailed message", e);
        }
    }

    /**
     * Log Logout status
     */
    void logLogout() {
        try {
            String logLogout = LazyConfig.AUTHD.bundle.getString("logout");
            List<String> dataList = new ArrayList<String>();
            dataList.add(logLogout);
            StringBuilder messageId = new StringBuilder();
            messageId.append("LOGOUT");
            if (indexType != null) {
                messageId.append("_").append(indexType.toString()
                        .toUpperCase());
                dataList.add(indexType.toString());
                if (indexName != null) {
                    dataList.add(indexName);
                }
            }
            String[] data = dataList.toArray(new String[dataList.size()]);
            String contextId = null;
            SSOToken localSSOToken = getSSOToken();
            if (localSSOToken != null) {
                contextId = localSSOToken.getProperty(Constants.AM_CTX_ID);
            }

            Hashtable<String, String> props = new Hashtable<String, String>();
            if (client != null) {
                props.put(LogConstants.IP_ADDR, client);
            }
            if (userDN != null) {
                props.put(LogConstants.LOGIN_ID, userDN);
            }
            if (orgDN != null) {
                props.put(LogConstants.DOMAIN, orgDN);
            }
            if (authMethName != null) {
                props.put(LogConstants.MODULE_NAME, authMethName);
            }
            InternalSession session = getReferencedSession();
            if (session != null) {
                props.put(LogConstants.LOGIN_ID_SID, finalSessionId.toString());
            }
            if (contextId != null) {
                props.put(LogConstants.CONTEXT_ID, contextId);
            }
            LazyConfig.AUTHD.logIt(data, AuthD.LOG_ACCESS, messageId.toString(), props);
        } catch (Exception e) {
            DEBUG.error("Error creating logout message", e);
        }
    }

    /**
     * Attribute name for loginFailureLockout.
     */ /**
     * Return attribute name for LoginLockout
     *
     * @return attribute name for LoginLockout
     */
    public String getLoginLockoutAttrName() {
        return loginLockoutAttrName;
    }

    /**
     * Attribute value for loginFailureLockout.
     */ /**
     * Return attribute value for LoginLockout
     *
     * @return attribute value for LoginLockout
     */
    public String getLoginLockoutAttrValue() {
        return loginLockoutAttrValue;
    }

    /**
     * Attribute name for storing invalid attempts data.
     */ /**
     * Return attribute name for storing invalid attempts data
     *
     * @return attribute name for storing invalid attempts data
     */
    public String getInvalidAttemptsDataAttrName() {
        return invalidAttemptsDataAttrName;
    }

    /**
     * Max time for loginFailureLockout.
     */ /**
     * Return LoginLockout duration
     *
     * @return LoginLockout duration
     */
    public long getLoginFailureLockoutDuration() {
        return loginFailureLockoutDuration;
    }

    /**
     * Multiplier for Memory Lockout Duration
     */ /**
     * Return multiplier for Memory Lockout
     *
     * @return LoginLockout multiplier
     */
    public int getLoginFailureLockoutMultiplier() {
        return loginFailureLockoutMultiplier;
    }

    /**
     * Returns old Session
     *
     * @return old Session
     */
    public InternalSession getOldSession() {
        return getReferencedOldSession();
    }

    /**
     * Sets old Session
     *
     * @param oldSession Old InternalSession Object
     */
    public void setOldSession(InternalSession oldSession) {
        if (null == oldSession) {
            this.oldSessionReference = null;
            return;
        }
        this.oldSessionReference = oldSession.getSessionID();
    }

    /**
     * Returns session upgrade.
     *
     * @return session upgrade.
     */
    public boolean isSessionUpgrade() {
        return sessionUpgrade;
    }

    /**
     * Sets session upgrade.
     *
     * @param sessionUpgrade <code>true</code> if session upgrade.
     */
    public void setSessionUpgrade(boolean sessionUpgrade) {
        this.sessionUpgrade = sessionUpgrade;
    }

    private void sessionUpgrade() throws AuthException {
        // set the larger authlevel
        InternalSession oldSession = getReferencedOldSession();
        if (oldSession == null && oldStatelessSession == null) {
            return;
        }

        String strPrevAuthLevel;
        String prevServiceName;
        String prevRoleName;
        String prevModuleList;

        if (oldSession != null) {
            strPrevAuthLevel = AMAuthUtils.getDataFromRealmQualifiedData(oldSession.getProperty("AuthLevel"));
            prevServiceName = oldSession.getProperty("Service");
            prevRoleName = oldSession.getProperty("Role");
            prevModuleList = oldSession.getProperty("AuthType");
            upgradeAllProperties(oldSession);
        } else {
            try {
                strPrevAuthLevel = AMAuthUtils.getDataFromRealmQualifiedData(oldStatelessSession.getProperty("AuthLevel"));
                prevServiceName = oldStatelessSession.getProperty("Service");
                prevRoleName = oldStatelessSession.getProperty("Role");
                prevModuleList = oldStatelessSession.getProperty("AuthType");
                upgradeAllPropertiesFromStateless(oldStatelessSession);
            } catch (SessionException se) {
                throw new AuthException(AMAuthErrorCode.SESSION_UPGRADE_FAILED, null);
            }
        }

        int prevAuthLevel = 0;
        String upgradeAuthLevel;

        try {
            prevAuthLevel = Integer.parseInt(strPrevAuthLevel);
        } catch (NumberFormatException e) {
            DEBUG.error("AuthLevel from session property bad format");
        }

        if (DEBUG.messageEnabled()) {
            DEBUG.message("prevAuthLevel : " + prevAuthLevel);
        }

        if (prevAuthLevel > authLevel) {
            upgradeAuthLevel = Integer.toString(prevAuthLevel);
        } else {
            upgradeAuthLevel = Integer.toString(authLevel);
        }

        if ((qualifiedOrgDN != null) && (qualifiedOrgDN.length() != 0)) {
            upgradeAuthLevel = AMAuthUtils.toRealmQualifiedAuthnData(
                    DNMapper.orgNameToRealmName(qualifiedOrgDN),
                    upgradeAuthLevel);
        }

        // update service name if indextype is service
        String upgradeServiceName = prevServiceName;
        String newServiceName;
        newServiceName = getAuthConfigName(indexType, indexName);

        if ((newServiceName != null) && (newServiceName.length() != 0)) {
            if ((qualifiedOrgDN != null)
                    && (qualifiedOrgDN.length() != 0)) {
                newServiceName = AMAuthUtils.toRealmQualifiedAuthnData(
                        DNMapper.orgNameToRealmName(qualifiedOrgDN),
                        newServiceName);
            }
            if (prevServiceName != null) {
                upgradeServiceName = prevServiceName;
                if ((newServiceName != null) &&
                        (!prevServiceName.contains(newServiceName))) {
                    upgradeServiceName = newServiceName + "|" +
                            prevServiceName;
                }
            } else {
                upgradeServiceName = newServiceName;
            }
        }

        // update role if indexType is role
        String upgradeRoleName = prevRoleName;
        if (indexType == AuthContext.IndexType.ROLE) {
            if (prevRoleName != null) {
                upgradeRoleName = prevRoleName;
                if ((indexName != null)
                        && (!prevRoleName.contains(indexName))) {
                    upgradeRoleName = indexName + "|" + prevRoleName;
                }
            } else {
                upgradeRoleName = indexName;
            }
        }

        // update auth meth name

        String newModuleList = authMethName;
        if ((qualifiedOrgDN != null) && (qualifiedOrgDN.length() != 0)) {
            newModuleList = getRealmQualifiedModulesList(
                    DNMapper.orgNameToRealmName(qualifiedOrgDN), authMethName);
        }
        if (DEBUG.messageEnabled()) {
            DEBUG.message("newModuleList : " + newModuleList);
            DEBUG.message("prevModuleList : " + prevModuleList);
        }
        String upgradeModuleList = null;
        StringBuilder sb = new StringBuilder();
        sb.append(newModuleList);
        if (prevModuleList == null ? newModuleList != null : !prevModuleList.equals(newModuleList)) {
            upgradeModuleList = parsePropertyList(prevModuleList, newModuleList);
        } else {
            upgradeModuleList = sb.toString();
        }

        if (DEBUG.messageEnabled()) {
            DEBUG.message("oldAuthLevel : " + prevAuthLevel);
            DEBUG.message("newAuthLevel : " + authLevel);
            DEBUG.message("upgradeAuthLevel : " + upgradeAuthLevel);
            DEBUG.message("prevServiceName : " + prevServiceName);
            DEBUG.message("upgradeServiceName : " + upgradeServiceName);
            DEBUG.message("preRoleName : " + prevRoleName);
            DEBUG.message("upgradeRoleName : " + upgradeRoleName);
            DEBUG.message("prevModuleList: " + prevModuleList);
            DEBUG.message("newModuleList: " + newModuleList);
            DEBUG.message("upgradeModuleList: " + upgradeModuleList);
        }

        updateSessionProperty("AuthLevel", upgradeAuthLevel);
        updateSessionProperty("AuthType", upgradeModuleList);
        updateSessionProperty("Service", upgradeServiceName);
        updateSessionProperty("Role", upgradeRoleName);
        InternalSession session = getReferencedSession();
        session.setIsSessionUpgrade(true);
    }

    /* upgrade session properties - old session and new session proeprties
     * will be concatenated , seperated by |
     */

    private void updateSessionProperty(final String property, final String value) {
        if (value == null) {
            return;
        }
        InternalSession session = null;

        if (!getForceFlag()) {
            session = getReferencedSession();
        } else {
            session = getReferencedOldSession();
        }

        if (null != session) {
            session.putProperty(property, value);
        }
    }

    /* update session with the property and value */

    /* Get realm qualified modules list */
    private String getRealmQualifiedModulesList(String realm, String oldModulesList) {
        if (DEBUG.messageEnabled()) {
            DEBUG.message("getRealmQualifiedModulesList:realm : "
                    + realm);
            DEBUG.message("getRealmQualifiedModulesList:oldModulesList : "
                    + oldModulesList);
        }

        StringBuilder sb = new StringBuilder();
        StringTokenizer st = new StringTokenizer(oldModulesList, "|");
        while (st.hasMoreTokens()) {
            String module = st.nextToken();
            sb.append(AMAuthUtils.toRealmQualifiedAuthnData(realm, module))
                    .append("|");
        }

        String realmQualifiedModulesList = sb.toString();
        int i = realmQualifiedModulesList.lastIndexOf("|");
        if (i != -1) {
            realmQualifiedModulesList =
                    realmQualifiedModulesList.substring(0, i);
        }

        if (DEBUG.messageEnabled()) {
            DEBUG.message("RealmQualifiedModulesList is : "
                    + realmQualifiedModulesList);
        }
        return realmQualifiedModulesList;
    }

    /* compare old session property and new session property */
    private String parsePropertyList(String oldProperty, String newProperty) {
        if (DEBUG.messageEnabled()) {
            DEBUG.message("oldProperty : " + oldProperty);
            DEBUG.message("newProperty : " + newProperty);
        }

        StringBuilder sb = new StringBuilder();
        sb.append(newProperty);
        StringTokenizer st = new StringTokenizer(oldProperty, "|");
        while (st.hasMoreTokens()) {
            String s = st.nextToken();
            if (!newProperty.equals(s)) {
                sb.append("|").append(s);
            }
        }

        String propertyList = sb.toString();

        if (DEBUG.messageEnabled()) {
            DEBUG.message("propertyList is : " + propertyList);
        }
        return propertyList;
    }

    // Upgrade all Properties from the existing (old) session to new session
    private void upgradeAllProperties(InternalSession oldSession) {
        if (DEBUG.messageEnabled()) {
            DEBUG.message("LoginState::upgradeAllProperties() : Calling SessionPropertyUpgrader");
        }
        InternalSession session = getReferencedSession();
        LazyConfig.SESSION_PROPERTY_UPGRADER.populateProperties(oldSession, session, getForceFlag());
    }

    // Upgrade all Properties from the existing (old) session to new session
    private void upgradeAllPropertiesFromStateless(StatelessSession oldSession) throws SessionException {
        if (DEBUG.messageEnabled()) {
            DEBUG.message("LoginState::upgradeAllProperties() : Calling SessionPropertyUpgrader");
        }
        InternalSession session = getReferencedSession();
        LazyConfig.SESSION_PROPERTY_UPGRADER.populatePropertiesFromStateless(oldSession, session);
    }

    private void invokeSessionUpgradeHandlers() {
        if (sessionUpgradeHandlers == null) {
            loadSessionUpgradeHandlers();
        }
        for (SessionUpgradeHandler sessionUpgradeHandler : sessionUpgradeHandlers) {
            InternalSession session = getReferencedSession();
            InternalSession oldSession = getReferencedOldSession();
            sessionUpgradeHandler.handleSessionUpgrade(oldSession, session);
        }
    }

    private static synchronized void loadSessionUpgradeHandlers() {
        if (sessionUpgradeHandlers == null) {
            List<SessionUpgradeHandler> instances = new ArrayList<>(1);
            final ServiceLoader<SessionUpgradeHandler> impls = ServiceLoader.load(SessionUpgradeHandler.class);
            for (SessionUpgradeHandler sessionUpgradeHandler : impls) {
                instances.add(sessionUpgradeHandler);
            }
            sessionUpgradeHandlers = ImmutableList.copyOf(instances);
        }
    }

    boolean isCookieSet() {
        return cookieSet;
    }

    void setCookieSet(boolean flag) {
        cookieSet = flag;
    }

    boolean isCookieSupported() {
        return cookieSupported;
    }

    void setCookieSupported(boolean flag) {
        cookieSupported = flag;
    }

    /* Order to determine execution of Post processing SPI is :
     *
     * Authentication Method         SPI execution order
     * ==========================================================
     * Org , Level , User ,Module
     *  1. Set at the user's role entry
     *  2. Set at the user's org entry
     *
     * Role
     *  1. Set that the role user's authenticates to
     *         2. Set at the user's role entry.
     *        3. Set at the user's org entry.
     *
     * Service
     *  1. Set at the service.
     *  2. Set at the user's role entry.
     *  3. Set at the user's org entry.
     *
     * @param indexType Index type for post process
     * @param indexName Index name for post process
     * @param type indicates success, failure or logout
     */
    void postProcess(AMLoginContext amlc, AuthContext.IndexType indexType, String indexName, PostProcessEvent type) {
        Set<AMPostAuthProcessInterface> postLoginInstanceSet = getPostLoginInstances(getPostLoginClassSet(indexType, indexName));
        if ((postLoginInstanceSet != null) &&
                (!postLoginInstanceSet.isEmpty())) {
        	for (AMPostAuthProcessInterface postLoginInstance : postLoginInstanceSet) {
        		if(postLoginInstance instanceof AMPostAuthProcess) {
        			((AMPostAuthProcess)postLoginInstance).amlc = amlc;
        		}
	        		if (!isNoSession())
	        			executePostProcessSPI(postLoginInstance, type);
            }
        }
    }

    /**
     * Returns an instance of the spi and execute it based on whether
     * the login status is success or failed
     *
     * @param postProcessInstance <code>AMPostAuthProcessInterface</code>
     *                            object to be processes in post login
     * @param type                indicates success, failure or logout
     */
    private void executePostProcessSPI(AMPostAuthProcessInterface postProcessInstance,
                               PostProcessEvent type) {
        /* Reset Post Process URLs in servletRequest so
        * that plugin can set new values (just a safety measure) */
        AuthUtils.resetPostProcessURLs(servletRequest);

        if (requestMap.isEmpty() && (servletRequest != null)) {
            @SuppressWarnings("unchecked")
            Map<String, String[]> map = servletRequest.getParameterMap();
            for (Map.Entry<String, String[]> e : map.entrySet()) {
                requestMap.put(e.getKey(), e.getValue()[0]);
            }
        }
        /* execute the post process spi */
        try {
            switch (type) {
                case SUCCESS:
                    final SSOToken ssoToken = getSSOToken();
                    postProcessInstance.onLoginSuccess(requestMap, servletRequest, servletResponse, ssoToken);
                    // Regenerate the session ID based on the sso token in case this is a stateless session that
                    // has been updated.
                    setSessionID(new SessionID(ssoToken.getTokenID().toString()));
                    break;
                case FAILURE:
                    postProcessInstance.onLoginFailure(requestMap, servletRequest, servletResponse);
                    break;
                case LOGOUT:
                    postProcessInstance.onLogout(servletRequest, servletResponse, getSSOToken());
                    break;
                default:
                    if (DEBUG.messageEnabled()) {
                        DEBUG.message("executePostProcessSPI: invalid input type: " + type);
                    }
            }
        } catch (AuthenticationException ae) {
            if (DEBUG.messageEnabled()) {
                DEBUG.message("Error ", ae);
            }
        } catch (Exception e) {
            if (DEBUG.messageEnabled()) {
                DEBUG.message("Error ", e);
            }
        }
    }

    /**
     * Creates a set of instances that are implementation of classes of type
     * AMPostAuthProcessInterface. The classes are picked based on index type
     * and auth configuration.
     *
     * @param indexType Index type for post login process
     * @param indexName Index name for post login process
     */
    private Set<String> getPostLoginClassSet(AuthContext.IndexType indexType, String indexName) {
        Set<String> postLoginClassSet = Collections.emptySet();
        if (indexType == AuthContext.IndexType.ROLE) {

            /* If role based auth then get post process classes from
             * auth config of that role.
             */

            postLoginClassSet = getRolePostLoginClassSet();
        } else if (indexType == AuthContext.IndexType.SERVICE) {

            /* For service based auth if service name is console service
             * then use admin auth config otherwise use the index name
             */

            if (indexName.equals(ISAuthConstants.CONSOLE_SERVICE)) {
                if ((orgAdminAuthConfig != null) &&
                        (!orgAdminAuthConfig.equals(ISAuthConstants.BLANK))) {
                    postLoginClassSet = getServicePostLoginClassSet
                            (orgAdminAuthConfig);
                }
            } else {
                postLoginClassSet = getServicePostLoginClassSet(indexName);
            }
        } else if ((indexType == AuthContext.IndexType.USER)) {

            /* For user based auth, take the auth config from users attributes
             */

            if (((userAuthConfig != null) && (!userAuthConfig.equals(
                    ISAuthConstants.BLANK)))) {
                postLoginClassSet = getServicePostLoginClassSet(userAuthConfig);
            }
        }

        if (((postLoginClassSet == null) || (postLoginClassSet.isEmpty())) &&
                ((orgPostLoginClassSet != null) && (!orgPostLoginClassSet.isEmpty()))) {

            /* If no Post Process class is found or module based auth then
             * default to org level  only if they are defined.
             */
            postLoginClassSet = orgPostLoginClassSet;
        } else if (indexType == null) {

            /* For org based auth, if post process classes are not defined at
             * org level then use or default config.
             */

            if ((orgAuthConfig != null) && (!orgAuthConfig.
                    equals(ISAuthConstants.BLANK))) {
                postLoginClassSet = getServicePostLoginClassSet(orgAuthConfig);
            }
        }

        if (DEBUG.messageEnabled()) {
            DEBUG.message("postLoginClassSet = " + postLoginClassSet);
        }
        return postLoginClassSet;
    }

    /**
     * Note that as a side effect, this sets the post auth process instance property on the session.
     */
    private Set<AMPostAuthProcessInterface> getPostLoginInstances(Set<String> postLoginClassSet) {
        Set<AMPostAuthProcessInterface> postLoginInstanceSet = new HashSet<>();
        if ((postLoginClassSet != null) && (!postLoginClassSet.isEmpty())) {
            StringBuilder sb = new StringBuilder();
            for (String postLoginClassName : postLoginClassSet) {
                if (sb.length() > 0) {
                    sb.append("|");
                }
                if (DEBUG.messageEnabled()) {
                    DEBUG.message("setPostLoginInstances : "
                            + postLoginClassName);
                    DEBUG.message("setPostLoginInstances : "
                            + postLoginClassSet.size());
                }
                AMPostAuthProcessInterface postProcessInstance = getPostLoginProcessInstance(postLoginClassName);
                if (postProcessInstance != null) {
                    postLoginInstanceSet.add(postProcessInstance);
                    sb.append(postLoginClassName);
                }
            }
            InternalSession session = getReferencedSession();
            if(session != null)
            	session.putProperty(ISAuthConstants.POST_AUTH_PROCESS_INSTANCE, sb.toString());
        }
        return postLoginInstanceSet;
    }

    /**
     * Returns role post login class set
     *
     * @return role post login class set
     */
    private Set<String> getRolePostLoginClassSet() {

        Set<String> postLoginClassSet = null;
        try {
            Map<String, Set<String>> roleAttrMap = getRoleServiceAttributes();
            postLoginClassSet = roleAttrMap.get(ISAuthConstants.POST_LOGIN_PROCESS);
            if (postLoginClassSet == null) {
                postLoginClassSet = Collections.emptySet();
            }

            if (DEBUG.messageEnabled()) {
                DEBUG.message("Role Post Login Class Set : " +
                        postLoginClassSet);
            }

        } catch (Exception e) {
            if (DEBUG.messageEnabled()) {
                DEBUG.message("Error get role class set ", e);
            }
        }
        return postLoginClassSet;
    }

    /**
     * Returns the service post login process class set
     *
     * @param indexName Index name for post login
     * @return the service post login process class set
     */
    private Set<String> getServicePostLoginClassSet(String indexName) {

        Set<String> postLoginClassSet = null;
        try {
            if ((serviceAttributesMap != null)
                    && (serviceAttributesMap.isEmpty())) {
                serviceAttributesMap = getServiceAttributes(indexName);
            }

            if (DEBUG.messageEnabled()) {
                DEBUG.message("Service Attributes are . :" +
                        serviceAttributesMap);
            }

            postLoginClassSet = serviceAttributesMap.get(ISAuthConstants.POST_LOGIN_PROCESS);
            if (postLoginClassSet == null) {
                postLoginClassSet = Collections.emptySet();
            }

            if (DEBUG.messageEnabled()) {
                DEBUG.message("postLoginClassName: " + postLoginClassSet);
            }

        } catch (Exception e) {
            if (DEBUG.messageEnabled()) {
                DEBUG.message("Error get service post login class name "
                        + e.getMessage());
            }
        }
        return postLoginClassSet;
    }

    /**
     * Returns the error message set by module.
     *
     * @return the error message set by module.
     */
    String getModuleErrorMessage() {
        return moduleErrorMessage;
    }

    void setModuleErrorMessage(String message) {
        moduleErrorMessage = message;
    }

    /**
     * Returns the Login URL user input.
     *
     * @return the Login URL user input.
     */
    public String getLoginURL() {
        return loginURL;
    }

    /**
     * Returns the flag indicating a request "forward" after
     * successful authentication.
     *
     * @return the boolean flag.
     */
    boolean isForwardSuccess() {
        return forwardSuccess;
    }

    /**
     * Returns page timeout.
     *
     * @return Page timeout.
     */
    long getPageTimeOut() {
        if (DEBUG.messageEnabled()) {
            DEBUG.message("Returning page timeout :" + pageTimeOut);
        }
        return pageTimeOut;
    }

    /**
     * Sets the page timeout.
     *
     * @param pageTimeOut Page timeout.
     */
    public synchronized void setPageTimeOut(long pageTimeOut) {
        if (DEBUG.messageEnabled()) {
            DEBUG.message("Setting page timeout :" + pageTimeOut);
        }
        this.pageTimeOut = pageTimeOut;
    }

    /**
     * Returns last callback sent.
     *
     * @return Last callback sent.
     */
    long getLastCallbackSent() {
        if (DEBUG.messageEnabled()) {
            DEBUG.message(
                    "Returning Last Callback Sent :" + lastCallbackSent);
        }
        return lastCallbackSent;
    }

    /**
     * Sets the last callback sent.
     *
     * @param lastCallbackSent Last callback sent.
     */
    void setLastCallbackSent(long lastCallbackSent) {
        if (DEBUG.messageEnabled()) {
            DEBUG.message("setting Last Callback Sent :" + lastCallbackSent);
        }
        this.lastCallbackSent = lastCallbackSent;
    }

    /**
     * This function is to get the redirect url from a set of urls
     * based on client type. Each url will be of the form
     * clienttype|url, applications need to provide redirect urls
     * along with the client type so that it can be client aware.
     * If it does not find the specified url along with
     * client type, it returns the default client type
     *
     * @param urls set of urls with client type.
     * @return redirect url from a set of urls for client type
     */
    private String getRedirectUrl(Set<String> urls) {

        //If the urls set is null, return the default url
        String clientURL = null;
        tempDefaultURL = null;
        if ((urls != null) && (!urls.isEmpty())) {
            String defaultURL = null;
            for (final String url : urls) {
                if (DEBUG.messageEnabled()) {
                    DEBUG.message("URL is : " + url);
                }
                if ((url != null) && (url.length() > 0)) {
                    int i = url.indexOf(ISAuthConstants.PIPE_SEPARATOR);
                    if (i != -1) {
                        if (clientURL == null) {
                            clientURL = AuthUtils.getClientURLFromString(
                                    url, i, servletRequest);
                        }
                    } else {
                        // There is no delimiter, ok check the size of the
                        // urls
                        if ((defaultURL == null) ||
                                (defaultURL.length() == 0)) {
                            defaultURL = url;
                        }
                    }
                }
            } //end while
            tempDefaultURL = defaultURL;
            if (DEBUG.messageEnabled()) {
                DEBUG.message("defaultURL : " + defaultURL);
                DEBUG.message("tempDefaultURL : " + tempDefaultURL);
            }
        }

        return clientURL;
    }

    /**
     * Return ignoreUserProfile
     *
     * @return ignoreUserProfile
     */
    public boolean ignoreProfile() {
        return ignoreUserProfile;
    }

    /**
     * Return set which contains union of the two sets
     * if one of the set is null or empty, return the other set
     *
     * @param set1 First set will be joined
     * @param set2 Second set will be joined
     * @return set which contains union of the two sets
     */
    private Set mergeSet(Set<String> set1, Set<String> set2) {
        if (set1 == null || set1.isEmpty()) {
            if (set2 == null || set2.isEmpty()) {
                return Collections.EMPTY_SET;
            } else {
                return set2;
            }
        } else {
            if (set2 == null || set2.isEmpty()) {
                return set1;
            } else {
                Set<String> returnSet = new HashSet<String>(set1);
                returnSet.addAll(set2);
                return returnSet;
            }
        }
    }

    /**
     * Converts a set to a map, keys are the elements in the set, value is
     * the token. User naming attribute is always added as one of the key.
     *
     * @param names set of names are used key for map
     * @param token value for each named key
     * @return map that is converted from set
     */
    private Map toAvPairMap(Set<String> names, String token) {
        if (token == null) {
            return Collections.EMPTY_MAP;
        }
        Map map = new HashMap();
        Set<String> set = new HashSet<String>();
        set.add(token);
        //map.put(AMStoreConnection.getNamingAttribute(AMObject.USER), set);
        //map.put(userNamingAttr, set);
        if (names == null || names.isEmpty()) {
            return map;
        }
        for (final Object name : names) {
            map.put(name, set);
        }
        return map;
    }

    /**
     * Sets the <code>failureTokenId</code> - set by modules
     * if this is set the logs will show the user id.
     *
     * @param userID User ID.
     */
    public void setFailedUserId(String userID) {
        if (DEBUG.messageEnabled()) {
            DEBUG.message("setting userID : " + userID);
        }
        setFailureTokenId(userID);
    }

    /**
     * If the session should be persisted to the CTS, do so.
     * Note that administrator users don't use stateless sessions.
     */
    void persistSession() {
        if ((stateless && !restriction.isRestricted(getUserDN()) || isNoSession())) {
            return;
        }
        authenticationSessionStore.promoteSession((getForceFlag())?oldSessionReference:sessionReference);
    }

    /**
     * Get all the received Callbacks.
     *
     * @return The received Callbacks.
     */
    public Map<String, Callback[]> getAllReceivedCallbacks() {
        return callbacksPerState;
    }

    /**
     * Returns Callbacks per Page state.
     *
     * @return Callbacks per Page state.
     */
    Callback[] getCallbacksPerState(String pageState) {
        Callback[] rtnCallbacks = null;
        rtnCallbacks = callbacksPerState.get(pageState);
        return rtnCallbacks;
    }

    /**
     * Sets Callbacks per Page state.
     */
    public void setCallbacksPerState(String pageState, Callback[] callbacks) {
        this.callbacksPerState.put(pageState, callbacks);
    }

    /**
     * Returns <code>true<code> if cookie detected.
     *
     * @return <code>true<code> if cookie detected.
     */
    public boolean isCookieDetect() {
        return cookieDetect;
    }

    /**
     * Sets the cookie detection value - <code>true</code> if
     * <code>cookieSupport</code> is null.
     *
     * @param cookieDetect Cookie Detect flag.
     */
    public void setCookieDetect(boolean cookieDetect) {
        this.cookieDetect = cookieDetect;
    }


    /**
     * Sets a Map of attribute value pairs to be used when the authentication
     * service is configured to dynamically create a user.
     *
     * @param attributeValuePairs Map of attribute name to a set of values.
     */
    public void setUserCreationAttributes(Map attributeValuePairs) {
        if (DEBUG.messageEnabled()) {
            DEBUG.message("attributeValuePairs : " + attributeValuePairs);
        }
        if ((attributeValuePairs != null) && (!attributeValuePairs.isEmpty())) {
            if (userCreationAttributes == null) {
                userCreationAttributes = new HashMap();
            }

            if (attributeValuePairs.containsKey(
                    ISAuthConstants.USER_ALIAS_ATTR)
                    ) {
                externalAliasList = (HashSet) attributeValuePairs.get(ISAuthConstants.USER_ALIAS_ATTR);
                if (DEBUG.messageEnabled()) {
                    DEBUG.message("externalAliasList:" + externalAliasList);
                }
                attributeValuePairs.remove(ISAuthConstants.USER_ALIAS_ATTR);
            }
            userCreationAttributes.putAll(attributeValuePairs);

        }
    }

    /**
     * Sets the module name of successful <code>LoginModule</code>.
     * This module name will be populated in the session property
     * <code>AuthType</code>.
     *
     * @param moduleName Name of module.
     */
    public void setSuccessModuleName(String moduleName) {
        successModuleSet.add(moduleName);
        if (DEBUG.messageEnabled()) {
            DEBUG.message("Module name is .. " + moduleName);
            DEBUG.message("successModuleSet is : " + successModuleSet);
        }
    }

    /**
     * Returns a Set which contains the modules names which user
     * successfully authenticated.
     *
     * @return a Set which contains the modules names which user
     * successfully authenticated.
     */
    Set<String> getSuccessModuleSet() {
        if (DEBUG.messageEnabled()) {
            DEBUG.message("getSuccessModuleSet : " + successModuleSet);
        }
        return successModuleSet;
    }

    /**
     * Checks if module is Application.
     *
     * @param moduleName is the module name to be compared with
     *                   Application module name.
     * @return true if module is Application else false.
     */
    private boolean isApplicationModule(String moduleName) {
        boolean isApp = (moduleName != null) &&
                (moduleName.equalsIgnoreCase(
                        ISAuthConstants.APPLICATION_MODULE));

        if (DEBUG.messageEnabled()) {
            DEBUG.message("is Application Module : " + isApp);
        }
        return isApp;
    }

    /**
     * Adds the failed module name to a set.
     *
     * @param moduleName Failed module name.
     */
    public void setFailureModuleName(String moduleName) {
        failureModuleSet.add(moduleName);
        if (DEBUG.messageEnabled()) {
            DEBUG.message("Module name is .. " + moduleName);
            DEBUG.message("failureModuleSet is : " + failureModuleSet);
        }
    }

    /**
     * Returns the failure module list.
     *
     * @return Failure module list.
     */
    Set<String> getFailureModuleSet() {
        return failureModuleSet;
    }

    /**
     * Sets the failure module list.
     *
     * @param failureModuleList which is the list of failed modules.
     */
    void setFailureModuleList(String failureModuleList) {
        this.failureModuleList = failureModuleList;
        if (DEBUG.messageEnabled()) {
            DEBUG.message("failureModulelist :" + failureModuleList);
        }
    }

    /**
     * Gets the failure module names.
     *
     * @return The list of failure auth module names in a pipe separated string.
     */
    public String getFailureModuleNames() {
        return failureModuleList;
    }

    /**
     * Returns <code>true</code> if the logged in user is 'Agent'.
     *
     * @param amIdentityUser OpenSSO Identity user.
     * @return <code>true</code> if the logged in user is 'Agent'.
     */
    private boolean isAgent(AMIdentity amIdentityUser) {
        boolean isAgent = false;
        try {
            if (amIdentityUser != null &&
                    amIdentityUser.getType().equals(IdType.AGENT)) {
                isAgent = true;
                DEBUG.message("user is of type 'Agent'");
            }
        } catch (Exception e) {
            if (DEBUG.messageEnabled()) {
                DEBUG.message("Error isAgent : " + e.toString());
            }
        }
        return isAgent;
    }

    /**
     * Sets the module map which has key as module localized name and
     * value is module name
     *
     * @param moduleMap module containing map of module localized name
     *                  and module name.
     */
    void setModuleMap(Map<String, String> moduleMap) {
        this.moduleMap = moduleMap;
    }

    /**
     * Returns the key for the localized module name.
     *
     * @param localizedModuleName , the localized module name
     * @return a string, the module name
     */
    protected String getModuleName(String localizedModuleName) {
        return moduleMap.get(localizedModuleName);
    }

    /**
     * Sets locale from servlet request.
     *
     * @param request HTTP Servlet Request.
     */
    void setRequestLocale(HttpServletRequest request) {
        localeContext.setLocale(request);
        isLocaleSet = true;
    }

    /**
     * Sets remote locale passed by client
     *
     * @param localeStr remote client locale string.
     */
    public void setRemoteLocale(String localeStr) {
        localeContext.setLocale(ISLocaleContext.URL_LOCALE, localeStr);
        isLocaleSet = true;
    }

    /**
     * Returns <code>true</code> if session state is invalid.
     *
     * @return <code>true</code> if session state is invalid.
     */
    boolean isSessionInvalid() {
        InternalSession session = getReferencedSession();
        return (session == null || session.getState() == SessionState.INVALID ||
                session.getState() == SessionState.DESTROYED);
    }

    /**
     * Returns <code>AMIdentity</code> object for a Role.
     *
     * @param roleName role name.
     * @return <code>AMIdentity</code> object.
     * @throws AuthException
     */
    public AMIdentity getRole(String roleName) throws AuthException {
        try {
            amIdentityRole =
                    LazyConfig.AUTHD.getIdentity(IdType.ROLE, roleName, getOrgDN());
        } catch (AuthException ae) {
            DEBUG.message("role not found or is not a static role");
        }
        if (amIdentityRole == null) {
            amIdentityRole =
                    LazyConfig.AUTHD.getIdentity(IdType.FILTEREDROLE, roleName, getOrgDN());
        }
        return amIdentityRole;
    }

    /**
     * Returns Universal Identifier of a role.
     *
     * @param roleName Role Name.
     * @return universal identifier of role name.
     */
    String getRoleUniversalId(String roleName) {
        String roleUnivId = null;
        try {
            AMIdentity amIdentity = getRole(roleName);
            roleUnivId = IdUtils.getUniversalId(amIdentity);
        } catch (Exception ae) {
            if (DEBUG.messageEnabled()) {
                DEBUG.message("Error getting role : " + ae.getMessage());
            }
        }
        return roleUnivId;
    }

    /**
     * Returns user DN of an Identity.
     *
     * @param amIdentityUser <code>AMIdentity</code> object.
     * @return Identity user DN.
     */
    public String getUserDN(AMIdentity amIdentityUser) {
        String returnUserDN = null;

        if (principalList != null) {
            if (principalList.contains("|")) {
                StringTokenizer st = new StringTokenizer(principalList, "|");
                while (st.hasMoreTokens()) {
                    String sToken = st.nextToken();
                    if (LDAPUtils.isDN(sToken)) {
                        returnUserDN = sToken;
                        break;
                    }
                }
            } else if (LDAPUtils.isDN(principalList)) {
                returnUserDN = principalList;
            }
        }

        if (returnUserDN == null || (returnUserDN.length() == 0)) {
            if (amIdentityUser != null) {
                returnUserDN = IdentityUtils.getDN(amIdentityUser);
            } else if (userDN != null) {
                returnUserDN = userDN;
            } else {
                returnUserDN = tokenToDN(principalList);
            }
        }
        return returnUserDN;
    }

    /**
     * Return DN for container
     *
     * @param containerDNs set of DN for containers
     * @throws AuthException if container name is invalid
     */
    private void getContainerDN(Set<String> containerDNs) throws AuthException {

        String userOrgDN = null;
        String agentContainerDN = null;
        // Check Container DNs for NULL
        if ((containerDNs == null) || (containerDNs.isEmpty())) {
            DEBUG.message("Container DNs is null");
        } else {
            for (String containerName : containerDNs) {
                try {
                    if (DN.valueOf(containerName).isInScopeOf(getOrgDN(), SearchScope.WHOLE_SUBTREE)) {
                        int containerType =
                                LazyConfig.AUTHD.getSDK().getAMObjectType(containerName);
                        if (DEBUG.messageEnabled()) {
                            DEBUG.message("Container Type = "
                                    + containerType);
                            DEBUG.message("Container Name = "
                                    + containerName);
                        }
                        if ((containerType == AMObject.ORGANIZATIONAL_UNIT)
                                && (agentContainerDN == null)) {
                            agentContainerDN = containerName;
                            identityTypes.add("agent");
                        } else if ((containerType == AMObject.ORGANIZATION)
                                && (userOrgDN == null)) {
                            userOrgDN = containerName;
                            identityTypes.add("agent");
                            identityTypes.add("user");
                        } else if ((containerType == AMObject.PEOPLE_CONTAINER)
                                && (userContainerDN == null)) {
                            userContainerDN = containerName;
                            identityTypes.add("user");
                        }
                    }
                    if (userContainerDN != null && agentContainerDN != null
                            && userOrgDN != null) {
                        break;
                    }
                } catch (Exception e) {
                    DEBUG.error("Container - " + containerName +
                            " is INVALID :- ", e);
                    continue;
                }
            }
        }

        if (userContainerDN == null) {
            try {
                userContainerDN = AMStoreConnection.getNamingAttribute(
                        AMObject.PEOPLE_CONTAINER) + "=" +
                        AdminInterfaceUtils.defaultPeopleContainerName() + "," +
                        getOrgDN();
                identityTypes.add("user");
            } catch (AMException aec) {
                DEBUG.message("Cannot get userContainer DN");
            }
        }

        if (userContainerDN == null && agentContainerDN == null) {
            DEBUG.message("No Valid Container in the list");
            throw new AuthException(AMAuthErrorCode.AUTH_ERROR, null);
        }

        if (DEBUG.messageEnabled()) {
            DEBUG.message("agentContainerDN = " + agentContainerDN);
            DEBUG.message("userContainerDN = " + userContainerDN);
            DEBUG.message("userOrgDN set in PC atrr = " + userOrgDN);
        }

    }

    /**
     * Search for identities given the identity type, identity name
     * Use common method from LazyConfig.AUTHD for getIdentity
     *
     * @param idType      identity type for user
     * @param userTokenID user token identifier
     * @param populate whether to retrieve all attributes or not
     * @return IdSearchResults for given the identity type and identity name
     * @throws IdRepoException if it fails to search user
     * @throws SSOException    if <code>SSOToken</code> is not valid
     */
    private IdSearchResults searchIdentity(IdType idType, String userTokenID, boolean populate)
            throws IdRepoException, SSOException {
        if (DEBUG.messageEnabled()) {
            DEBUG.message("In searchAutehnticatedUser: idType " + idType);
            DEBUG.message("In getUserProfile : Search for user " +
                    userTokenID);
        }
        IdSearchResults searchResults = null;
        Set returnSet = mergeSet(aliasAttrNames, USER_ATTRIBUTES);
        int maxResults = 2;
        int maxTime = 0;
        String pattern;
        Map avPairs;
        boolean isRecursive = true;

        IdSearchControl idsc = new IdSearchControl();
        idsc.setRecursive(isRecursive);
        idsc.setTimeOut(maxTime);
        if (populate) {
            idsc.setAllReturnAttributes(true);
        } else {
            idsc.setReturnAttributes(returnSet);
        }

        if (DEBUG.messageEnabled()) {
            DEBUG.message("alias attr=" + aliasAttrNames +
                    ", attr=" + USER_ATTRIBUTES + ",merge=" + returnSet);
        }

        if (DEBUG.messageEnabled()) {
            DEBUG.message("Search for Identity " + userTokenID);
        }
        // search for the identity
        Set result = Collections.EMPTY_SET;
        try {
            idsc.setMaxResults(0);
            searchResults =
                    amIdRepo.searchIdentities(idType, userTokenID, idsc);
            if (searchResults != null) {
                result = searchResults.getSearchResults();
            }
        } catch (SSOException sso) {
            if (DEBUG.messageEnabled()) {
                DEBUG.message("SSOException Error searching Identity " +
                        " with username " + sso.getMessage());
            }
        } catch (IdRepoException e) {
            if (DEBUG.messageEnabled()) {
                DEBUG.message("IdRepoException : Error searching "
                        + " Identities with username : "
                        + e.getMessage());
            }
        }

        if (result.isEmpty() && (aliasAttrNames != null) &&
                (!aliasAttrNames.isEmpty())) {
            if (DEBUG.messageEnabled()) {
                DEBUG.message("No identity found, try Alias attrname.");
            }
            pattern = "*";
            avPairs = toAvPairMap(aliasAttrNames, userTokenID);
            if (DEBUG.messageEnabled()) {
                DEBUG.message("Search for Filter (avPairs) :" + avPairs);
                DEBUG.message("userTokenID : " + userTokenID);
                DEBUG.message("userDN : " + userDN);
                DEBUG.message("idType :" + idType);
                DEBUG.message("pattern :" + pattern);
                DEBUG.message("isRecursive :" + isRecursive);
                DEBUG.message("maxResults :" + maxResults);
                DEBUG.message("maxTime :" + maxTime);
                DEBUG.message("returnSet :" + returnSet);
            }
            Set resultAlias = Collections.EMPTY_SET;
            try {
                idsc.setMaxResults(maxResults);
                idsc.setSearchModifiers(IdSearchOpModifier.OR, avPairs);
                searchResults = amIdRepo.searchIdentities(idType, pattern, idsc);
                if (searchResults != null) {
                    resultAlias = searchResults.getSearchResults();
                }
                if ((resultAlias.isEmpty()) && (userDN != null) &&
                        (!userDN.equalsIgnoreCase(userTokenID))) {
                    avPairs = toAvPairMap(aliasAttrNames, userDN);
                    if (DEBUG.messageEnabled()) {
                        DEBUG.message("Search for Filter (avPairs) " +
                                "with userDN : " + avPairs);
                    }
                    idsc.setMaxResults(maxResults);
                    idsc.setSearchModifiers(IdSearchOpModifier.OR, avPairs);
                    searchResults =
                            amIdRepo.searchIdentities(idType, pattern, idsc);
                }
            } catch (SSOException sso) {
                if (DEBUG.messageEnabled()) {
                    DEBUG.message("SSOException : Error searching "
                            + "Identities with aliasattrname : "
                            + sso.getMessage());
                }
            } catch (IdRepoException e) {
                if (DEBUG.messageEnabled()) {
                    DEBUG.message("IdRepoException : Error searching "
                            + "Identities : " + e.getMessage());
                }
            }
        }
        return searchResults;
    } // end

    /**
     * Creates <code>AMIdentity</code> in the repository.
     *
     * @param userName       name of user to be created.
     * @param userAttributes Map of default attributes.
     * @param userRoles      Set of default roles.
     * @return <code>AMIdentity</code> object of created user.
     * @throws IdRepoException if it fails to create <code>AMIdentity</code>
     * @throws SSOException    if <code>SSOToken</code> for admin is not valid
     */
    public AMIdentity createUserIdentity(
            String userName,
            Map userAttributes,
            Set userRoles
    ) throws IdRepoException, SSOException {
        AMIdentity amIdentityUser = amIdRepo.createIdentity(IdType.USER,
                userName, userAttributes);
        if (userRoles != null && !userRoles.isEmpty()) {
            for (final Object userRole : userRoles) {
                String trole = (String) userRole;
                try {
                    if (trole.length() != 0) {
                        amIdentityRole = getRole(trole);
                        amIdentityRole.addMember(amIdentityUser);
                    }
                } catch (Exception e) {
                    DEBUG.message("createUserProfile():invalid role: ", e);
                    //ignore invalid Roles
                }
            }
        }
        return amIdentityUser;
    }

    /**
     * Returns the universal id associated with a user name.
     *
     * @param userName name of user to be created.
     * @return universal identifier of the user.
     */
    public String getUserUniversalId(String userName) {
        AMIdentity amIdUser = amIdentityUser;
        String universalId = null;
        try {
            if (amIdUser == null) {
                amIdUser = LazyConfig.AUTHD.getIdentity(IdType.USER, userName, getOrgDN());
            }
            universalId = IdUtils.getUniversalId(amIdUser);
        } catch (Exception e) {
            DEBUG.message(
                    "Error getting Identity for user :" + e.getMessage());
        }
        if (DEBUG.messageEnabled()) {
            DEBUG.message("getUserUniversalId:universalId : " + universalId);
        }
        return universalId;
    }

    /**
     * Returns the type of authentication to be used after Composite Advices.
     *
     * @return an integer type indicating the type of authentication required.
     */
    int getCompositeAdviceType() {
        return compositeAdviceType;
    }

    /**
     * Sets the type of authentication to be used after Composite Advices.
     *
     * @param type Type of authentication.
     */
    void setCompositeAdviceType(int type) {
        this.compositeAdviceType = type;
    }

    /**
     * Returns the Composite Advice for this Authentication request.
     *
     * @return String of Composite Advice.
     */
    public String getCompositeAdvice() {
        return compositeAdvice;
    }

    /**
     * Sets the Composite Advice for this Authentication request.
     *
     * @param compositeAdvice Composite Advice for authentication.
     */
    void setCompositeAdvice(String compositeAdvice) {
        this.compositeAdvice = compositeAdvice;
    }

    /**
     * Sets the qualified OrgDN for Policy conditions
     * to be used after Composite Advices.
     *
     * @param qualifiedOrgDN qualifiedOrgDN for Policy conditions.
     */
    void setQualifiedOrgDN(String qualifiedOrgDN) {
        this.qualifiedOrgDN = qualifiedOrgDN;
    }

    /**
     * Returns the Authentication configuration / Authentication
     * chain name used for current authentication process.
     *
     * @param indexType AuthContext.IndexType
     * @param indexName Index Name for AuthContext.IndexType
     */
    private String getAuthConfigName(AuthContext.IndexType indexType,
                             String indexName) {
        String finalAuthConfigName = null;
        if (indexType == AuthContext.IndexType.ROLE) {
            finalAuthConfigName = roleAuthConfig;
        } else if (indexType == AuthContext.IndexType.SERVICE) {
            if (indexName.equals(ISAuthConstants.CONSOLE_SERVICE)) {
                if ((orgAdminAuthConfig != null) &&
                        (!orgAdminAuthConfig.equals(ISAuthConstants.BLANK))) {
                    finalAuthConfigName = orgAdminAuthConfig;
                }
            } else {
                finalAuthConfigName = indexName;
            }
        } else if ((indexType == AuthContext.IndexType.USER)) {
            if (((userAuthConfig != null) && (!userAuthConfig.equals(
                    ISAuthConstants.BLANK)))) {
                finalAuthConfigName = userAuthConfig;
            }
        } else if (indexType == null) {
            if ((orgAuthConfig != null) && (!orgAuthConfig.
                    equals(ISAuthConstants.BLANK))) {
                finalAuthConfigName = orgAuthConfig;
            }
        }

        if (DEBUG.messageEnabled()) {
            DEBUG.message("getAuthConfigName:finalAuthConfigName = "
                    + finalAuthConfigName);
        }

        return finalAuthConfigName;
    }

    boolean isAuthValidForInternalUser() {
        boolean authValid = true;
        InternalSession session = getReferencedSession();
        if (session == null) {
            DEBUG.warning(
                    "LoginState.isValidAuthForInternalUser():session is null");
            return false;
        }
        getTokenFromPrincipal(subject);
        String userId = token;
        if (userId == null) {
            DEBUG.warning(
                    "LoginState.isValidAuthForInternalUser():userId is null");
            return false;
        }
        if (INTERNAL_USERS.contains(userId.toLowerCase())) {
            String authRealm = orgDN;
            String authModule = "";
            if (!successModuleSet.isEmpty()) {
                authModule = (successModuleSet.iterator().next());
            }
            if (authRealm == null) {
                DEBUG.warning(
                        "LoginState.isValidAuthForInternalUser():authRealm is null");
                return false;
            }
            if (authModule == null) {
                if (DEBUG.warningEnabled()) {
                    DEBUG.warning("LoginState.isValidAuthForInternalUser():"
                            + "authModule is null");
                }
                return false;
            }
            if (DEBUG.messageEnabled()) {
                DEBUG.message("LoginState.isValidAuthForInternalUser():"
                        + "Attempt to login as:" + userId
                        + ", to module:" + authModule
                        + ", at realm:" + authRealm);
            }
            if (!authRealm.equals(ServiceManager.getBaseDN())) {
                authValid = false;
                if (DEBUG.warningEnabled()) {
                    DEBUG.warning("LoginState.isValidAuthForInternalUser():"
                            + "Attempt to login as:" + userId
                            + ", to module:" + authModule
                            + ", at realm:" + authRealm
                            + ", denied due to internal users restriction ");
                }
            }
        }
        return authValid;
    }

    /**
     * Sets userDN - called by <code>AMLoginContext</code>.
     */
    public void setUserName(String username) {
        userDN = username;
    }

    /**
     * Restores the old session (if one exists). Used in the case of a failed session upgrade or successful force-auth
     * to restore the original session object. If no old session exists then this method does nothing.
     */
    public void restoreOldSession() {
        InternalSession oldSession = getReferencedOldSession();
        if (oldSession != null) {
            DEBUG.message("Restoring old session");
            setSession(oldSession);
        }
    }

    /**
     * Indicates userID generate mode is enabled
     */
    public boolean isUserIDGeneratorEnabled() {
        return userIDGeneratorEnabled;
    }

    private void setUserIDGeneratorEnabled(final boolean userIDGeneratorEnabled) {
        this.userIDGeneratorEnabled = userIDGeneratorEnabled;
    }

    /**
     * Indicates provider class name for userIDGenerator
     */
    public String getUserIDGeneratorClassName() {
        return userIDGeneratorClassName;
    }

    private void setUserIDGeneratorClassName(final String userIDGeneratorClassName) {
        this.userIDGeneratorClassName = userIDGeneratorClassName;
    }

    /**
     * Indicates accountlocking mode is enabled.
     */
    private boolean isLoginFailureLockoutMode() {
        return loginFailureLockoutMode;
    }

    private void setLoginFailureLockoutMode(final boolean loginFailureLockoutMode) {
        this.loginFailureLockoutMode = loginFailureLockoutMode;
    }

    /**
     * Indicates loginFailureLockoutStoreInDS mode is enabled.
     */
    private boolean isLoginFailureLockoutStoreInDS() {
        return loginFailureLockoutStoreInDS;
    }

    private void setLoginFailureLockoutStoreInDS(final boolean loginFailureLockoutStoreInDS) {
        this.loginFailureLockoutStoreInDS = loginFailureLockoutStoreInDS;
    }

    private void setAccountLife(final String accountLife) {
        this.accountLife = accountLife;
    }

    private void setLoginFailureLockoutDuration(final long loginFailureLockoutDuration) {
        this.loginFailureLockoutDuration = loginFailureLockoutDuration;
    }

    private void setLoginFailureLockoutMultiplier(final int loginFailureLockoutMultiplier) {
        this.loginFailureLockoutMultiplier = loginFailureLockoutMultiplier;
    }

    private void setLoginFailureLockoutTime(final long loginFailureLockoutTime) {
        this.loginFailureLockoutTime = loginFailureLockoutTime;
    }

    private void setLoginFailureLockoutCount(final int loginFailureLockoutCount) {
        this.loginFailureLockoutCount = loginFailureLockoutCount;
    }

    private void setLoginLockoutNotification(final String loginLockoutNotification) {
        this.loginLockoutNotification = loginLockoutNotification;
    }

    private void setLoginLockoutAttrName(final String loginLockoutAttrName) {
        this.loginLockoutAttrName = loginLockoutAttrName;
    }

    private void setMandatory2FAValue(final String mandatory2FAValue) {
        this.mandatory2fa = Boolean.parseBoolean(mandatory2FAValue);
    }

    private void setLoginLockoutAttrValue(final String loginLockoutAttrValue) {
        this.loginLockoutAttrValue = loginLockoutAttrValue;
    }

    private void setInvalidAttemptsDataAttrName(final String invalidAttemptsDataAttrName) {
        this.invalidAttemptsDataAttrName = invalidAttemptsDataAttrName;
    }

    private void setLoginLockoutUserWarning(final int loginLockoutUserWarning) {
        this.loginLockoutUserWarning = loginLockoutUserWarning;
    }

    /**
     * <code>SSOToken</code> ID for login failed
     */
    public String getFailureTokenId() {
        return failureTokenId;
    }

    private void setFailureTokenId(final String failureTokenId) {
        this.failureTokenId = failureTokenId;
    }

    public boolean is2faMandatory() {
        return mandatory2fa;
    }
    
    /**
     * Sets a shared state map from the {@link AMLoginModule}.
     * @param sharedState
     */
    public void setSharedState(Map sharedState) {
        this.sharedState = sharedState;
    }

    /**
     * The shared state map.
     * @return sharedState 
     */
    public Map getSharedState() {
        return sharedState;
    }

    /**
     * Saves the attributes specified by the sharedStateAttributes into requestMap.
     */
    public void saveSharedStateAttributes() {
        if (sharedState != null) {
            for (String sharedStateKey : SHARED_STATE_ATTRIBUTES) {
                requestMap.put(sharedStateKey, (String) sharedState.get(sharedStateKey));
            }
        }
    }

    /**
     * Save the principalList that is generated by successful LoginContext authentication, to the requestMap.
     */
    void saveSubjectState() {
        if (principalList != null) {
            requestMap.put(ISAuthConstants.PRINCIPAL_LIST, principalList);
        }
    }

    /**
     * Saves the principals successfully created in the authentication process whether all modules or identity searches
     * are successful or not. This differs from the principalList which is generated by the logincontext as that is only
     * generated when all modules have been completed successfully.
     * 
     * @param principalName
     */
    public void saveAuthenticatedPrincipal(String principalName) {
        authenticatedPrincipals.add(principalName);
        // store in the requestmap
        requestMap.put(ISAuthConstants.AUTHENTICATED_PRINCIPALS, StringUtils.join(authenticatedPrincipals, "|"));
    }
    
    /**
     * Returns a list of the authenticated principals in the current authentication process. 
     * @return  authenticatedPrincipals
     */
    public Set<String> getAuthenticatedPrincipals() {
        return authenticatedPrincipals;
    }

    /**
     * get orgPostLoginClassSet
     */
    public Set<String> getPostLoginClassSet(){
    			return orgPostLoginClassSet;
    }
    
    /**
     * Indicates the type of post-processing that should be performed.
     */
    enum PostProcessEvent {
        SUCCESS,
        FAILURE,
        LOGOUT
    }
}
