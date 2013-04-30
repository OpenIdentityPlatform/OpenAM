
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
 * $Id: LoginState.java,v 1.57 2010/01/20 21:30:40 qcheng Exp $
 *
 */

/**
 * Portions Copyrighted 2010-2013 ForgeRock, Inc.
 */

package com.sun.identity.authentication.service;

import com.iplanet.am.sdk.AMException;
import com.iplanet.am.sdk.AMObject;
import com.iplanet.am.sdk.AMStoreConnection;
import com.iplanet.am.util.Misc;
import com.iplanet.am.util.SystemProperties;
import com.iplanet.dpro.session.Session;
import com.iplanet.dpro.session.SessionID;
import com.iplanet.dpro.session.service.InternalSession;
import com.sun.identity.shared.encode.CookieUtils;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.authentication.AuthContext;
import com.sun.identity.authentication.util.AMAuthUtils;
import com.sun.identity.authentication.config.AMAuthConfigUtils;
import com.sun.identity.authentication.config.AMAuthenticationManager;
import com.sun.identity.authentication.config.AMAuthenticationInstance;
import com.sun.identity.authentication.config.AMConfigurationException;
import com.sun.identity.authentication.server.AuthContextLocal;
import com.sun.identity.authentication.spi.AMPostAuthProcessInterface;
import com.sun.identity.authentication.spi.AuthenticationException;
import com.sun.identity.authentication.util.ISAuthConstants;
import com.sun.identity.common.DNUtils;
import com.sun.identity.sm.DNMapper;
import com.sun.identity.shared.DateUtils;
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
import com.sun.identity.security.DecodeAction;
import com.sun.identity.security.EncodeAction;
import com.sun.identity.session.util.SessionUtils;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.datastruct.CollectionHelper;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.openam.utils.ClientUtils;
import com.sun.identity.sm.OrganizationConfigManager;
import com.sun.identity.sm.SMSEntry;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceManager;
import java.net.InetAddress;
import java.security.AccessController;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.sun.identity.shared.ldap.util.DN;
import com.sun.identity.shared.ldap.util.RDN;
import com.sun.identity.security.AdminTokenAction;
import org.forgerock.openam.authentication.service.DefaultSessionPropertyUpgrader;
import org.forgerock.openam.authentication.service.SessionPropertyUpgrader;

/**
 * This class maintains the User's login state information from the time user 
 * requests for authentication till the time the user either logs out of the 
 * OpenSSO system or the session is destroyed by any privileged application of 
 * the OpenSSO system.
 */
public class LoginState {
    
    private static final boolean urlRewriteInPath =
        Boolean.valueOf(SystemProperties.get(
            Constants.REWRITE_AS_PATH,"")).booleanValue();
    private static AuthD ad =AuthD.getAuth();
    private static Debug debug = ad.debug;
    
    private static String pCookieName = AuthUtils.getPersistentCookieName();
    private static Set userAttributes = new HashSet();
    
    Callback[] receivedCallbackInfo;
    Callback[] prevCallback;
    Callback[] submittedCallbackInfo;
    HashMap callbacksPerState = new HashMap();
    InternalSession session = null;
    HttpServletRequest servletRequest;
    HttpServletResponse servletResponse;
    String orgName;
    String userOrg;
    String orgDN = null;
    int loginStatus = LoginStatus.AUTH_IN_PROGRESS;
    Hashtable requestHash;
    boolean requestType;  // new or existing request
    
    Set aliasAttrNames = null;
    /**
     * Indicates if orgnization is active
     */
    public boolean inetDomainStatus = true;
    String userContainerDN = null;
    // indicate if the userContainerDN is null
    boolean nullUserContainerDN = false;
    //indicate if the user DN is constructed from userContainerDN in tokenToDN()
    boolean dnByUserContainer = false;
    String userNamingAttr = null;
    /**
     * Default role for user
     */
    public Set defaultRoles = null;
    boolean dynamicProfileCreation = false;
    boolean ignoreUserProfile = false;
    boolean createWithAlias = false;
    boolean persistentCookieMode = false;
    boolean zeroPageLoginEnabled = false;
    /**
     * Max. cookie time in seconad. Integer range is 0 - 2147483.
     * persistentCookieOn has to be true.
     */
    public String persistentCookieTime = null;
    /**
     * Indicate if the persistent cookie mode is enabled. 
     * set to false for production.
     */
    public boolean persistentCookieOn = false;
    /**
     * Default auth level for each auth module
     */
    public String defaultAuthLevel = "0";
    Subject subject;
    String token=null;
    String userDN=null;
    int maxSession ;
    int idleTime ;
    int cacheTime;
    int authLevel=0;
    int moduleAuthLevel=Integer.MIN_VALUE;
    String client= null;
    String authMethName="";
    String pAuthMethName=null;
    String queryOrg = null;
    SessionID sid;
    boolean cookieSupported = true;
    boolean cookieSet = false;
    String filePath;
    boolean userEnabled=true;;
    boolean isAdmin;
    boolean isApp=false;
    AMIdentity amIdentityRole = null;
    Set tokenSet;
    AuthContext.IndexType indexType;
    String indexName = null;
    AuthContext.IndexType prevIndexType = null;
    Set userAliasList = null;
    boolean hasAdminToken=false;
    String gotoURL= null;
    String gotoOnFailURL= null;
    String failureLoginURL=null;
    String successLoginURL=null;
    String moduleSuccessLoginURL=null;
    String moduleFailureLoginURL=null;
    Set orgSuccessLoginURLSet = null;
    String clientOrgSuccessLoginURL=null;
    String defaultOrgSuccessLoginURL=null;
    String clientOrgFailureLoginURL=null;
    String defaultOrgFailureLoginURL=null;
    Set orgFailureLoginURLSet = null;
    Map requestMap = new HashMap();
    /**
     * Indicates userID generate mode is enabled
     */
    public boolean userIDGeneratorEnabled;
    /**
     * Indicates provider class name for userIDGenerator
     */
    public String userIDGeneratorClassName;
    Set domainAuthenticators = null;
    Set moduleInstances= null;
    private InternalSession oldSession = null;
    private SSOToken oldSSOToken = null;
    private boolean forceAuth;
    private boolean cookieTimeToLiveEnabledFlag = false;
    private int cookieTimeToLive = 0;
    boolean sessionUpgrade = false;
    String loginURL = null;
    long pageTimeOut = 60;
    long lastCallbackSent = 0;
    AMIdentity amIdentityUser =null;
    // Enable Module based Auth
    private boolean enableModuleBasedAuth = true;
    /**
     * Indicates accountlocking mode is enabled.
     */
    public boolean loginFailureLockoutMode = false;
    /**
     * Indicates loginFailureLockoutStoreInDS mode is enabled.
     */
    public boolean loginFailureLockoutStoreInDS = true;
    /**
     * Indicates loginFailureLockoutStoreInDS mode is enabled.
     */
    public String accountLife=null;
    /**
     * Max time for loginFailureLockout.
     */
    public long loginFailureLockoutDuration = 0;
    /**
     * Multiplier for Memory Lockout Duration
     */
    public int loginFailureLockoutMultiplier = 0;
    /**
     * Default max time for loginFailureLockout.
     */
    public long loginFailureLockoutTime = 300000;
    /**
     * Default count for loginFailureLockout.
     */
    public int loginFailureLockoutCount = 5;
    /**
     * Default notification for loginFailureLockout.
     */
    public String loginLockoutNotification = null;
    /**
     * Attribute name for loginFailureLockout.
     */
    public String loginLockoutAttrName = null;
    /**
     * Attribute value for loginFailureLockout.
     */
    public String loginLockoutAttrValue = null;
    /**
     * Attribute name for storing invalid attempts data.
     */
    public String invalidAttemptsDataAttrName = null;
    /**
     * Default number of count for loginFailureLockout warning
     */
    public int loginLockoutUserWarning = 3;
    /**
     * Current number of count for loginFailureLockout warning
     */
    public int userWarningCount = 0;
    
    // error code
    String errorCode = null;
    String errorMessage = null;
    String errorTemplate = null;
    String moduleErrorTemplate = null;
    String lockoutMsg = null;
    
    // timed out
    boolean timedOut = false;
    
    /**
     * <code>SSOToken</code> ID for login failed
     */
    public String failureTokenId = null;
    String principalList = null;
    String pCookieUserName = null;
    
    private ISLocaleContext localeContext = new ISLocaleContext();
    
    
    X509Certificate cert = null;
    
    String defaultUserSuccessURL ;
    String clientUserSuccessURL ;
    Set userSuccessURLSet = Collections.EMPTY_SET;
    String clientUserFailureURL ;
    String defaultUserFailureURL ;
    Set userFailureURLSet = Collections.EMPTY_SET;
    String clientSuccessRoleURL ;
    String defaultSuccessRoleURL ;
    Set successRoleURLSet = Collections.EMPTY_SET;
    String clientFailureRoleURL ;
    String defaultFailureRoleURL ;
    Set failureRoleURLSet = Collections.EMPTY_SET;
    String userAuthConfig = "";
    String orgAuthConfig = null;
    String orgAdminAuthConfig = null;
    String roleAuthConfig = null;
    Set orgPostLoginClassSet = Collections.EMPTY_SET;
    Map serviceAttributesMap = new HashMap();
    String moduleErrorMessage = null;
    String defaultSuccessURL = null;
    String defaultFailureURL = null;
    String tempDefaultURL = null;
    String sessionSuccessURL = null;
    Set postLoginInstanceSet = null;
    boolean isLocaleSet=false;
    boolean cookieDetect=false;
    HashMap userCreationAttributes = null;
    Set externalAliasList = null;
    Set successModuleSet = new HashSet();
    Set failureModuleSet = new HashSet();
    String failureModuleList = ISAuthConstants.EMPTY_STRING;
    String fqdnFailureLoginURL=null;
    Map moduleMap = null;
    Map roleAttributeMap = null;
    Boolean foundPCookie =null;
    long pCookieTimeCreated = 0;
    Set identityTypes = Collections.EMPTY_SET;
    Set userSessionMapping = Collections.EMPTY_SET;
    Hashtable idRepoHash = new Hashtable();
    AMIdentityRepository amIdRepo = null;
    private static boolean messageEnabled;
    private static String serverURL = null;
    private static long agentSessionIdleTime;
    private static long minAgentSessionIdleTime = 30;

    int compositeAdviceType;
    String compositeAdvice;
    String qualifiedOrgDN = null;
    
    static final int POSTPROCESS_SUCCESS = 1;
    static final int POSTPROCESS_FAILURE = 2;
    static final int POSTPROCESS_LOGOUT = 3;    
    
    // Variable indicating a request "forward" after 
    // authentication success
    boolean forwardSuccess = false;
    boolean postProcessInSession = false;
    boolean modulesInSession = false;
    
    public static Set internalUsers = new HashSet();
    
    private static SecureRandom secureRandom = null;
    private static SessionPropertyUpgrader propertyUpgrader = null;

    static {
        
        userAttributes.add(ISAuthConstants.LOGIN_SUCCESS_URL);
        userAttributes.add(ISAuthConstants.LOGIN_FAILURE_URL);
        userAttributes.add(ISAuthConstants.USER_ALIAS_ATTR);
        userAttributes.add(ISAuthConstants.MAX_SESSION_TIME);
        userAttributes.add(ISAuthConstants.SESS_MAX_IDLE_TIME);
        userAttributes.add(ISAuthConstants.SESS_MAX_CACHING_TIME);
        userAttributes.add(ISAuthConstants.INETUSER_STATUS);
        userAttributes.add(ISAuthConstants.NSACCOUNT_LOCK);
        userAttributes.add(ISAuthConstants.PREFERRED_LOCALE);
        userAttributes.add(ISAuthConstants.LOGIN_STATUS);
        userAttributes.add(ISAuthConstants.ACCOUNT_LIFE);
        userAttributes.add(ISAuthConstants.USER_SUCCESS_URL);
        userAttributes.add(ISAuthConstants.USER_FAILURE_URL);
        userAttributes.add(ISAuthConstants.POST_LOGIN_PROCESS);
        
        messageEnabled = debug.messageEnabled();

        String proto = SystemProperties.get(Constants.DISTAUTH_SERVER_PROTOCOL);
        String host = null;
        String port = null;
        if (proto != null && proto.length() != 0 ) {
            host = SystemProperties.get(Constants.DISTAUTH_SERVER_HOST);
            port = SystemProperties.get(Constants.DISTAUTH_SERVER_PORT);
        } else {
            proto = SystemProperties.get(Constants.AM_SERVER_PROTOCOL);
            host = SystemProperties.get(Constants.AM_SERVER_HOST);
            port = SystemProperties.get(Constants.AM_SERVER_PORT);
        }
        serverURL = proto + "://" + host + ":" + port;

        // App session timeout is default to 0 => non-expiring 
        String asitStr = SystemProperties.get(
                Constants.AGENT_SESSION_IDLE_TIME);
        if (asitStr != null && asitStr.length() > 0) {
            try {
                agentSessionIdleTime = Long.parseLong(asitStr);
                // inappropriate to set to a too small number
                if ((agentSessionIdleTime > 0) &&
                        (agentSessionIdleTime < minAgentSessionIdleTime)) {
                    agentSessionIdleTime = minAgentSessionIdleTime;
                }
            } catch (Exception le) { }
        }

        /* Define internal users
         * For these users we would allow authentication only at root realm
         * and require to be authenticated to configuration datastore.
         */
        internalUsers.add("amadmin");
        internalUsers.add("dsameuser");
        internalUsers.add("urlaccessagent");
        internalUsers.add("amldapuser");
        
        // Obtain the secureRandom instance
        try {
            secureRandom = SecureRandom.getInstance("SHA1PRNG");
        } catch (NoSuchAlgorithmException ex) {
            debug.error("LoginState.static() : " + "LoginState : "
            + "SecureRandom.getInstance() Failed", ex);
        }

        String upgraderClass = SystemProperties.get(Constants.SESSION_UPGRADER_IMPL,
                Constants.DEFAULT_SESSION_UPGRADER_IMPL);
        try {
            propertyUpgrader = Class.forName(upgraderClass).
                    asSubclass(SessionPropertyUpgrader.class).newInstance();
            if (debug.messageEnabled()) {
                debug.message("SessionUpgrader implementation ('" + upgraderClass
                        + ") successfully loaded.");
            }
        } catch (Exception ex) {
            debug.error("Unable to load the following Session Upgrader implementation: " +
                    upgraderClass + "\nFallbacking to DefaultSessionUpgrader", ex);
            propertyUpgrader = new DefaultSessionPropertyUpgrader();
        }
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
        if (session == null || session.getState() == Session.INACTIVE ||
            session.getState() == Session.DESTROYED) {
            if (messageEnabled) {
                debug.message(
                    "Session is null OR INACTIVE OR DESTROYED :" + session);
            }
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
        this.session = sess;
    }
    
    /**
     * Sets the callbacks recieved and notify waiting thread.
     *
     * @param callback
     * @param amLoginContext
     */
    public void setReceivedCallback(
        Callback[] callback,
        AMLoginContext amLoginContext) {
        synchronized(amLoginContext) {
            submittedCallbackInfo=null;
            receivedCallbackInfo= callback;
            prevCallback = callback;
            amLoginContext.notify();
        }
    }
    
    /**
     * Sets the callbacks recieved and notify waiting thread.
     * Used in non-jaas thread mode only.
     * @param callback
     */
    public void setReceivedCallback_NoThread(Callback[] callback) {
        submittedCallbackInfo=null;
        receivedCallbackInfo= callback;
        prevCallback = callback;
    }
    
    
    /**
     * Sets the callbacks submitted by login module and notify waiting thread.
     *
     * @param callback
     * @param amLoginContext
     */
    public  void setSubmittedCallback(
        Callback[] callback,
        AMLoginContext amLoginContext) {
        synchronized(amLoginContext) {
            receivedCallbackInfo=null;
            prevCallback = receivedCallbackInfo;
            submittedCallbackInfo = callback;
            amLoginContext.notify();
        }
    }
    
    /**
     * Sets the callbacks submitted by login module and notify waiting thread.
     * Used in non-jaas thread mode only. 
     * @param callback
     */
    public  void setSubmittedCallback_NoThread(Callback[] callback) {
        receivedCallbackInfo=null;
        prevCallback = receivedCallbackInfo;
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
                orgDN = ad.getOrgDN(userOrg);
            } catch (Exception e) {
                debug.message("Error getting orgDN: " ,e);
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
     *
     * @param requestHash Request parameters hash.
     */
    public void setParamHash(Hashtable requestHash) {
        this.requestHash = requestHash;
        
        /* copy these parameters to HashMap */
        if (requestHash != null) {
            Enumeration hashKeys = requestHash.keys();
            while (hashKeys.hasMoreElements()) {
                Object key = hashKeys.nextElement();
                Object value = requestHash.get(key);
                this.requestMap.put(key,value);
            }
        }
    }
    
    /**
     * Sets the request type.
     *
     * @param requestType <code>true</code> for new request type;
     *        <code>false</code> for existing request type.
     */ 
    public void setRequestType(boolean requestType) {
        this.requestType = requestType;
    }
    
    /**
     * Returns the request type.
     *
     * @return the request type.
     */
    public boolean isNewRequest() {
        return requestType;
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
     * Populates the organization profile.
     *
     * @throws AuthException
     */
    public void populateOrgProfile() throws AuthException {
        try {
            // get inetdomainstatus for the org
            // check if org is active
            inetDomainStatus = ad.getInetDomainStatus(getOrgDN());
            if (!inetDomainStatus) {
                // org inactive
                logFailed(AuthUtils.getErrorVal(AMAuthErrorCode.AUTH_ORG_INACTIVE,
                AuthUtils.ERROR_MESSAGE),"ORGINACTIVE");
                throw new AuthException(AMAuthErrorCode.AUTH_ORG_INACTIVE,null);
            }
            // get handle to org config manager object to retrieve auth service
            // attributes.
            OrganizationConfigManager orgConfigMgr = 
                ad.getOrgConfigManager(getOrgDN());
            ServiceConfig svcConfig =
            orgConfigMgr.getServiceConfig(ISAuthConstants.AUTH_SERVICE_NAME);
            
            Map attrs = svcConfig.getAttributes();
            aliasAttrNames = (Set) attrs.get(ISAuthConstants.AUTH_ALIAS_ATTR);
            // NEEDED FOR BACKWARD COMPATIBILITY SUPPORT - OPEN ISSUE
            // TODO: Remove backward compat stuff
            if (AuthD.revisionNumber >=
            ISAuthConstants.AUTHSERVICE_REVISION7_0) {
                identityTypes = (Set)attrs.get(ISAuthConstants.
                AUTH_ID_TYPE_ATTR);
            } else {
                identityTypes = new HashSet();
                Set containerDNs = (Set) attrs.get(
                ISAuthConstants.AUTH_USER_CONTAINER);
                getContainerDN(containerDNs);
            }
            userSessionMapping = (Set)attrs.get(ISAuthConstants.
                USER_SESSION_MAPPING);
            
            userNamingAttr = CollectionHelper.getMapAttr(
                attrs, ISAuthConstants.AUTH_NAMING_ATTR, "uid");
            // END BACKWARD COMPATIBILITY SUPPORT
            
            defaultRoles = (Set)attrs.get(ISAuthConstants.AUTH_DEFAULT_ROLE);
            
            String tmp = CollectionHelper.getMapAttr(
                attrs, ISAuthConstants.DYNAMIC_PROFILE);
            if (tmp.equalsIgnoreCase("true")) {
                dynamicProfileCreation = true;
            } else if (tmp.equalsIgnoreCase("ignore")) {
                ignoreUserProfile = true;
            } else if (tmp.equalsIgnoreCase("createAlias")) {
                createWithAlias = true;
                dynamicProfileCreation=true;
            }

            tmp = CollectionHelper.getMapAttr(
                attrs, ISAuthConstants.PERSISTENT_COOKIE_MODE);
            if (tmp.equalsIgnoreCase("true")) {
                persistentCookieMode = true;
            }
            
            tmp = null;
            tmp = CollectionHelper.getMapAttr(
                attrs, ISAuthConstants.PERSISTENT_COOKIE_TIME);
            persistentCookieTime = tmp;

            tmp = CollectionHelper.getMapAttr(attrs, Constants.ZERO_PAGE_LOGIN_ENABLED);
            zeroPageLoginEnabled = Boolean.valueOf(tmp);
            
            AMAuthenticationManager authManager =
            new AMAuthenticationManager(ad.getSSOAuthSession(),getOrgDN());
            
            domainAuthenticators = authManager.getAllowedModuleNames();
            if (domainAuthenticators == null) {
                domainAuthenticators = Collections.EMPTY_SET;
            }
            
            defaultAuthLevel = CollectionHelper.getMapAttr(
                attrs, ISAuthConstants.DEFAULT_AUTH_LEVEL,ad.defaultAuthLevel);
            
            localeContext.setOrgLocale( getOrgDN() );
            
            orgSuccessLoginURLSet =
            (Set)attrs.get(ISAuthConstants.LOGIN_SUCCESS_URL);
            
            if (orgSuccessLoginURLSet == null) {
                orgSuccessLoginURLSet = Collections.EMPTY_SET;
            }
            
            clientOrgSuccessLoginURL = getRedirectUrl(orgSuccessLoginURLSet);
            defaultOrgSuccessLoginURL = tempDefaultURL;
            
            orgFailureLoginURLSet=
            (Set)attrs.get(ISAuthConstants.LOGIN_FAILURE_URL);
            if (orgFailureLoginURLSet == null) {
                orgFailureLoginURLSet = Collections.EMPTY_SET;
            }
            
            clientOrgFailureLoginURL = getRedirectUrl(orgFailureLoginURLSet);
            defaultOrgFailureLoginURL = tempDefaultURL;
            orgAuthConfig = CollectionHelper.getMapAttr(attrs, 
                ISAuthConstants.AUTHCONFIG_ORG);
            orgAdminAuthConfig = CollectionHelper.getMapAttr(attrs, 
                ISAuthConstants.AUTHCONFIG_ADMIN);
            orgPostLoginClassSet =
            (Set) attrs.get(ISAuthConstants.POST_LOGIN_PROCESS);
            if (orgPostLoginClassSet == null) {
                orgPostLoginClassSet = Collections.EMPTY_SET;
            }
            
            tmp = CollectionHelper.getMapAttr(
                attrs, ISAuthConstants.MODULE_BASED_AUTH);
            if (tmp != null) {
                if (tmp.equalsIgnoreCase("false")) {
                    enableModuleBasedAuth = false;
                }
            }

            
            // retrieve account locking specific attributes
            tmp = CollectionHelper.getMapAttr(
                attrs, ISAuthConstants.LOGIN_FAILURE_LOCKOUT);
            if (tmp != null) {
                if (tmp.equalsIgnoreCase("true")) {
                    loginFailureLockoutMode = true;
                }
            }
            tmp = CollectionHelper.getMapAttr(
                attrs, ISAuthConstants.LOGIN_FAILURE_STORE_IN_DS);
            if (tmp != null) {
                if (tmp.equalsIgnoreCase("false")) {
                    loginFailureLockoutStoreInDS = false;
                }
            }
            tmp = CollectionHelper.getMapAttr(
                attrs, ISAuthConstants.LOCKOUT_DURATION);
            if (tmp != null) {
                try {
                    loginFailureLockoutDuration = Long.parseLong(tmp);
                }catch(NumberFormatException e) {
                    debug.error("auth-lockout-duration bad format.");
                }
                loginFailureLockoutDuration *= 60*1000;
            }

            tmp = Misc.getMapAttr(attrs, ISAuthConstants.LOCKOUT_MULTIPLIER);
            if (tmp != null) {
                try {
                    loginFailureLockoutMultiplier = Integer.parseInt(tmp);
                }catch(NumberFormatException e) {
                    ad.debug.error("auth-lockout-multiplier bad format.");
                }
            }
            
            tmp = CollectionHelper.getMapAttr(
                attrs, ISAuthConstants.LOGIN_FAILURE_COUNT);
            if (tmp != null) {
                try {
                    loginFailureLockoutCount = Integer.parseInt(tmp);
                }catch(NumberFormatException e) {
                    debug.error("auth-lockout-count bad format.");
                }
            }
            
            tmp = CollectionHelper.getMapAttr(
                attrs, ISAuthConstants.LOGIN_FAILURE_DURATION);
            if (tmp != null) {
                try {
                    loginFailureLockoutTime = Long.parseLong(tmp);
                }catch(NumberFormatException e) {
                    debug.error("auth-login-failure-duration bad format.");
                }
                loginFailureLockoutTime *= 60*1000;
            }
            
            tmp = CollectionHelper.getMapAttr(
                attrs, ISAuthConstants.LOCKOUT_WARN_USER);
            if (tmp != null) {
                try {
                    loginLockoutUserWarning = Integer.parseInt(tmp);
                }catch(NumberFormatException e) {
                    debug.error("auth-lockout-warn-user bad format.");
                }
            }
            
            loginLockoutNotification = CollectionHelper.getMapAttr(
                attrs, ISAuthConstants.LOCKOUT_EMAIL);
            
            tmp = CollectionHelper.getMapAttr(
                attrs, ISAuthConstants.USERNAME_GENERATOR);
            if (tmp != null) {
                userIDGeneratorEnabled = Boolean.valueOf(tmp).booleanValue();
            }
            
            userIDGeneratorClassName = CollectionHelper.getMapAttr(
                attrs, ISAuthConstants.USERNAME_GENERATOR_CLASS);
            tmp = CollectionHelper.getMapAttr(
                attrs, ISAuthConstants.LOCKOUT_ATTR_NAME);
            loginLockoutAttrName = tmp;
            
            tmp = CollectionHelper.getMapAttr(
                attrs, ISAuthConstants.LOCKOUT_ATTR_VALUE);
            loginLockoutAttrValue = tmp;
            
            invalidAttemptsDataAttrName = CollectionHelper.getMapAttr(
                attrs, ISAuthConstants.INVALID_ATTEMPTS_DATA_ATTR_NAME);
            
            if (messageEnabled) {
                debug.message("Getting Org Profile: " + orgDN
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
                        + "\nloginFailureLockoutMode->" + loginFailureLockoutMode
                        + "\nloginFailureLockoutStoreInDS->"
                        + loginFailureLockoutStoreInDS
                        + "\nloginFailureLockoutCount->" + loginFailureLockoutCount
                        + "\nloginFailureLockoutTime->" + loginFailureLockoutTime
                        + "\nloginLockoutUserWarning->" + loginLockoutUserWarning
                        + "\nloginLockoutNotification->" + loginLockoutNotification
                        + "\ninvalidAttemptsDataAttrName->" + invalidAttemptsDataAttrName
                        + "\npersistentCookieMode->" + persistentCookieMode
                        + "\nzeroPageLoginEnabled->" + zeroPageLoginEnabled
                        + "\nidentityTypes->" + identityTypes
                        + "\naliasAttrNames ->" + aliasAttrNames);
            }
        } catch (AuthException ae) {
            debug.error("Error in populateOrgProfile", ae);
            throw new AuthException(ae);
        } catch (Exception ex) {
            debug.error("Error in populateOrgProfile", ex);
            throw new AuthException(AMAuthErrorCode.AUTH_ERROR, null);
        }
    }
    
    /**
     * Populates the global profile.
     *
     * @throws AuthException
     */
    public void populateGlobalProfile() throws AuthException {
        Map attrs = AuthUtils.getGlobalAttributes("iPlanetAMAuthService");
        String tmpPostProcess = (String)Misc.getMapAttr(attrs,
         ISAuthConstants.KEEP_POSTPROCESS_IN_SESSION);
        postProcessInSession = Boolean.parseBoolean(tmpPostProcess); 
        String tmpModules = (String)Misc.getMapAttr(attrs,
         ISAuthConstants.KEEP_MODULES_IN_SESSION);
        modulesInSession = Boolean.parseBoolean(tmpModules); 
        if (messageEnabled) {
            debug.message("LoginState.populateGlobalProfile: "
                + "Getting Global Profile: " +
                "\npostProcessInSession ->" + postProcessInSession +
                "\nmodulesInSession ->" + modulesInSession);
        }
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
     * Returns the authenticated subject.
     *
     * @return Authenticated subject
     */
    public Subject getSubject() {
        return subject;
    }
    
    /**
     * Returns the maximum session time.
     *
     * @return Maximum session time.
     */
    public int getMaxSession() {
        return maxSession;
    }
    
    /**
     * Returns session idle time.
     *
     * @return session idle time.
     */
    public int getIdleTime() {
        return idleTime;
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
        if (messageEnabled) {
            debug.message("getUserDN: " + userDN);
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
     * Sets the client address.
     *
     * @param remoteAddr Client address.
     */
    public void setClient(String remoteAddr) {
        client = remoteAddr;
    }
    
    /**
     * Returns the client address.
     *
     * @return the client address.
     */
    public String getClient() {
        if (client  != null) {
            return client;
        }
        String clientHost ="";
        try {
            String cli = null;
            if (requestHash != null) {
                cli = (String) requestHash.get("client");
            }
            if (messageEnabled) {
                debug.message("getClient : servletRequest is : " + servletRequest);
                debug.message("getClient : cli is : " + cli);
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
            if (messageEnabled) {
                debug.message("Error getting client Type " , e);
            }
        }
        
        if (messageEnabled) {
            debug.message("Client is : " + clientHost);
        }
        client = clientHost;
        return clientHost;
    }
    
    /**
     * convert a token to DN
     * FOR BACKWARD COMPATIBILITY SUPPORT
     * @param token0 <code>SSOToken</code> ID has user principal
     * @return DN for user principal
     */
    public String tokenToDN(String token0) {
        try {
            String token = token0.toLowerCase();
            int pipe = token.indexOf("|");
            if (pipe != -1) {
                token = token.substring(0, pipe);
            }
            
            // Return if module returns the token in the form of DN
            if (Misc.isDescendantOf(token, getOrgDN())) {
                return token;
            }
            
            if (ad.isSuperAdmin(token)) {
                return token;
            }
            
            // check if Application module user
            // Application module user starts with
            // amService-
            String applicationUser=
            ISAuthConstants.APPLICATION_USER_PREFIX.toLowerCase();
            if (token.startsWith(applicationUser)) {
                return "cn=" + token + ",ou=DSAME Users," +
                    SMSEntry.getRootSuffix();
            }
            
            String id = token;
            id = DNUtils.DNtoName(token);
            
            StringBuilder s = new StringBuilder(200);
            s.append(userNamingAttr).append("=").append(id).append(",")
            .append(userContainerDN);
            // set flag, indicate user DN is constructed from userContainerDN
            dnByUserContainer = true;
            String userDN = s.toString();
            if (messageEnabled) {
                debug.message("token=" + token0 + ", id=" + id +
                ", DN=" + userDN);
            }
            return userDN;
        } catch (Exception e) {
            debug.error("tokenToDN : " + e.getMessage());
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
     * Activates session on successful authenticaton.
     *
     * @param subject
     * @param ac
     * @return true if user session is activated successfully 
     */
    public boolean activateSession(Subject subject, AuthContextLocal ac)
            throws AuthException {
        return activateSession(subject, ac, null);
    }
    
    /**
     * Activates session on successful authenticaton.
     *
     * @param subject
     * @param ac
     * @param loginContext instance of JAAS <code>LoginContext</code>
     * @return true if user session is activated successfully 
     */
    public boolean activateSession(Subject subject, AuthContextLocal ac, Object
        loginContext) throws AuthException {
        try {
            if (messageEnabled) {
                debug.message("activateSession - Token is : "+ token);
                debug.message("activateSession - userDN is : "+ userDN);
            }

            setSuccessLoginURL(indexType,indexName);
            //Fix for OPENAM-75
            //Create a new session upon successful authentication instead using old session and change state from
            //INVALID to VALID only.
            SessionID oldSessId = session.getID();
            InternalSession authSession = session;
            //Generating a new session ID for the successfully logged in user
            session = AuthD.newSession(getOrgDN(), null);
            sid = session.getID();
            session.removeObject(ISAuthConstants.AUTH_CONTEXT_OBJ);

            this.subject = addSSOTokenPrincipal(subject);
            setSessionProperties(session);
            //copying over the session properties that were set on the authentication session onto the new session
            Enumeration<String> authSessionProperties = authSession.getPropertyNames();
            while (authSessionProperties.hasMoreElements()) {
                String key = authSessionProperties.nextElement();
                String value = authSession.getProperty(key);
                updateSessionProperty(key, value);
            }

            //destroying the authentication session
            AuthD.getSS().destroyInternalSession(oldSessId);
            if ((modulesInSession) && (loginContext != null)) {
                session.setObject(ISAuthConstants.LOGIN_CONTEXT,
                loginContext);
            }
            if (messageEnabled) {
		debug.message("Activating session: " + session);
            }
            return session.activate(userDN);
        } catch (AuthException ae) {
            debug.error("Error setting session properties: ", ae);
            throw ae;
        } catch (Exception e) {
            debug.error("Error activating session: ", e);
            throw new AuthException("sessionActivationFailed", null);
        }
    }
    
    /**
     * Populates session with properties.
     *
     * @param session
     * @throws AuthException
     */
    public void setSessionProperties(InternalSession session)
        throws AuthException {
        if (messageEnabled) {
            debug.message("LoginState getSession = " +
            session +" \nrequest token = " + token);
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
        if (oldSession != null) {
            oldUserDN = oldSession.getProperty(ISAuthConstants.PRINCIPAL);
            oldAuthenticationModuleInstanceName =oldSession.getProperty(
                ISAuthConstants.AUTH_TYPE); 
            if(!ignoreUserProfile){
                newAMIdentity = 
                    ad.getIdentity(IdType.USER,userDN,getOrgDN());          
                oldAMIdentity = 
                    ad.getIdentity(IdType.USER,oldUserDN,getOrgDN());
                if (messageEnabled) {
                    debug.message("LoginState.setSessionProperties()" + 
                              " newAMIdentity is: " + newAMIdentity);
                    debug.message("LoginState.setSessionProperties()" + 
                              " oldAMIdentity is: " + oldAMIdentity);        	
                }
            }
        }
        
        if (messageEnabled) {
            debug.message("LoginState.setSessionProperties()" + 
            		      " userDN is: " + userDN);
            debug.message("LoginState.setSessionProperties()" +
            		      " oldUserDN is: " + oldUserDN);        	
            debug.message("LoginState.setSessionProperties()" +
                          " sessionUpgrade is: " + sessionUpgrade);            
        }
        
        if (sessionUpgrade){
            String oldAuthenticationModuleClassName = null;
            if((oldAuthenticationModuleInstanceName != null) &&
                (oldAuthenticationModuleInstanceName.indexOf("|") == -1)){
                try{
                    SSOToken adminToken = 
                        (SSOToken) AccessController.doPrivileged(
                            AdminTokenAction.getInstance());
                    AMAuthenticationManager authManager =
                        new AMAuthenticationManager(adminToken,getOrgName());
                    AMAuthenticationInstance authInstance =
                        authManager.getAuthenticationInstance(
                            oldAuthenticationModuleInstanceName);
                    oldAuthenticationModuleClassName = 
                        authInstance.getType();
                }catch (AMConfigurationException ace) {
                    if (messageEnabled) {
                        debug.message("LoginState.setSessionProperties()" 
                        + ":Unable to create AMAuthenticationManager"
                        + "Instance:"
                        + ace.getMessage());
                    }
                    throw new AuthException(ace);
                }
        	}
        	
            if("Anonymous".equalsIgnoreCase(oldAuthenticationModuleClassName)){
                sessionUpgrade();
            } else if(!ignoreUserProfile){
                if((oldAMIdentity != null) && 
                        oldAMIdentity.equals(newAMIdentity)){
                    sessionUpgrade();
                }else {
                    if (messageEnabled) {
                        debug.message("LoginState.setSessionProperties()" +
                        "Resetting session upgrade to false " +
                        "since oldAMIdentity and newAMIdentity doesn't match");
                    }                	
                    throw new AuthException(
                        AMAuthErrorCode.SESSION_UPGRADE_FAILED, null);
                }
            } else {
                if((oldUserDN != null) && 
                        (DNUtils.normalizeDN(userDN)).equals(
                            DNUtils.normalizeDN(oldUserDN))){
                    sessionUpgrade();
                } else {
                    if (messageEnabled) {
                        debug.message("LoginState.setSessionProperties()" +
                        "Resetting session upgrade to false " +
                        "since Old UserDN and New UserDN doesn't match");
                    }
                	throw new AuthException(
                        AMAuthErrorCode.SESSION_UPGRADE_FAILED, null);
                }
            }
        }
        
        if (forceAuth && sessionUpgrade) {
            session = oldSession;
        }        

        Date authInstantDate = new Date();
        String authInstant = DateUtils.toUTCDateFormat(authInstantDate);
        
        String moduleAuthTime = null;
        if (sessionUpgrade) {
            try {
                oldSSOToken = SSOTokenManager.getInstance().createSSOToken
                    (oldSession.getID().toString());
            } catch (SSOException ssoExp) {
                debug.error("LoginState.setSessionProperties: Cannot get "
                    + "oldSSOToken.");
            }
            Map moduleTimeMap = null;
            if (oldSSOToken != null) {
                moduleTimeMap = AMAuthUtils.getModuleAuthTimeMap(oldSSOToken);
            }
            if (moduleTimeMap == null) {
                moduleTimeMap = new HashMap();
            }
            StringTokenizer tokenizer = new StringTokenizer(authMethName,
                ISAuthConstants.PIPE_SEPARATOR);
            while (tokenizer.hasMoreTokens()) {
                String moduleName = (String) tokenizer.nextToken();
                moduleTimeMap.put(moduleName,authInstant);
            }
            Set entrySet = moduleTimeMap.entrySet();
            boolean firstElement = true;
            for(Iterator iter = entrySet.iterator(); iter.hasNext();) {
                Map.Entry entry = (Map.Entry)iter.next();
                String moduleName = (String)entry.getKey();
                String authTime = (String)entry.getValue();
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
        if (loginURL != null) {
            int questionMark = loginURL.indexOf("?");
            defaultLoginURL = loginURL;
            if (questionMark != -1) {
                defaultLoginURL = loginURL.substring(0, questionMark);
            }
            session.putProperty(ISAuthConstants.LOGIN_URL, defaultLoginURL);
            session.putProperty(ISAuthConstants.FULL_LOGIN_URL, loginURL);
        }
        
        sessionSuccessURL = ad.processURL(successLoginURL, servletRequest);
        sessionSuccessURL = encodeURL(sessionSuccessURL,servletResponse, true);
        if (sessionSuccessURL != null) {
            session.putProperty(ISAuthConstants.SUCCESS_URL, sessionSuccessURL);
        }
        
        // Get the universal ID
        String univId = null;
        if (amIdentityUser != null) {
            univId = IdUtils.getUniversalId(amIdentityUser);
        }
        
        String userId = DNUtils.DNtoName(userDN);
        if (messageEnabled) {
            debug.message(
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
                "principals = " + principalList+ "\n" +
                "defaultLoginURL = " + defaultLoginURL+ "\n" +
                "successURL = " + sessionSuccessURL+ "\n" +
                "IndexType = " + indexType+ "\n" +
                "UserProfile = " + userProfile+ "\n" +
                "AuthInstant = " + authInstant+ "\n" +
                "ModuleAuthTime = " + moduleAuthTime);
        }
        
        try {
            if ((isApplicationModule(authMethName) && 
                    (ad.isSuperUser(userDN) || ad.isSpecialUser(userDN)))
                    || isAgent(amIdentityUser)) {

                session.setClientID(token);
                session.setType(Session.APPLICATION_SESSION);
                if (isAgent(amIdentityUser) && agentSessionIdleTime > 0) {
                    if (ad.debug.messageEnabled()) {
                        ad.debug.message("setSessionProperties for agent " +
                                userDN + " with idletimeout to " +
                                agentSessionIdleTime);
                    }
                    session.setMaxSessionTime(Long.MAX_VALUE/60);
                    session.setMaxIdleTime(agentSessionIdleTime);
                    session.setMaxCachingTime(agentSessionIdleTime);
                } else {
                    if (ad.debug.messageEnabled()) {
                        ad.debug.message("setSessionProperties for non-expiring session");
                    }
                    session.setExpire(false);
                }
            } else {
                debug.message("request: in putProperty stuff");
                session.setClientID(userDN);
                session.setMaxSessionTime(maxSession);
                session.setMaxIdleTime(idleTime);
                session.setMaxCachingTime(cacheTime);
            }
            
            session.setClientDomain(getOrgDN());
            session.setType(Session.USER_SESSION);
            if ((client = getClient()) != null) {
                session.putProperty(ISAuthConstants.HOST, client);
            }
            if (!sessionUpgrade) {
                session.putProperty(ISAuthConstants.AUTH_LEVEL,
                    new Integer(authLevel).toString());
                session.putProperty(ISAuthConstants.AUTH_TYPE, authMethName);
            }
            session.putProperty(ISAuthConstants.PRINCIPAL, userDN);

            if (userId == null && userDN != null) {
                DN dnObj  = new DN(userDN);
                List rdn = dnObj.getRDNs();
                if (rdn!=null && rdn.size()>0) {
                    userId = ((RDN)rdn.get(0)).getValues()[0];
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
            } else if (univId == null && userDN != null) {
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
                    finalAuthConfig.length() != 0)){
                    session.putProperty(ISAuthConstants.SERVICE,
                        finalAuthConfig);
                }
            }
            if ((userSessionMapping != null) && 
                !(userSessionMapping.isEmpty()) && !ignoreUserProfile) {
                Iterator tmpIterator = userSessionMapping.iterator();
                while (tmpIterator.hasNext()) {
                    String mapping = (String) tmpIterator.next();
                    if ((mapping != null) && (mapping.length() != 0)) {
                        StringTokenizer tokenizer = new StringTokenizer(
                            mapping, "|");
                        String userAttribute = null;
                        String sessionAttribute = null;
                        if (tokenizer.hasMoreTokens()) {
                            userAttribute = (String) tokenizer.nextToken();
                        }
                        if (tokenizer.hasMoreTokens()) {
                            sessionAttribute = (String) tokenizer.nextToken();
                        }
                        if ((userAttribute != null)&& 
                            (userAttribute.length() != 0)){
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
                                    if (strBuffValues.length() == 0){
                                        strBuffValues.append(userAttrValue);
                                    } else {
                                        strBuffValues.append("|").append
                                            (userAttrValue);
                                    }
                                }
                                if (sessionAttribute != null){
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
                Map attrMap = 
                    principalDataRetriever.getAttrMapForAuthenticationModule(
                        subject);
                if (attrMap != null && !attrMap.isEmpty()) {
                    Set entrySet = attrMap.entrySet();
                    for(Iterator iter = entrySet.iterator(); iter.hasNext();) {
                        Map.Entry entry = (Map.Entry)iter.next();
                        String attrName = (String)entry.getKey();
                        String attrValue = (String)entry.getValue();
                        session.putProperty(attrName, attrValue);
                        if (messageEnabled) {
                            debug.message("AttrMap for SAML : " +
                            attrName + " , " + attrValue);
                        }
                    }
                }
            }
            
        } catch (Exception e) {
            debug.error("Exception in setSession ", e);
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
     * Sets the query organization.
     *
     * @param queryOrg Query organization.
     */
    public void setQueryOrg(String queryOrg) {
        this.queryOrg = queryOrg;
    }
    
    /**
     * Returns the query Organization.
     *
     * @return Query Organization.
     */
    public String getQueryOrg() {
        return queryOrg;
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

    /**
     * Returns locale.
     *
     * @return locale.
     */
    public String getLocale() {
        if (!isLocaleSet) {
            return ad.platLocale;
        } else {
            return localeContext.getLocale().toString();
        }
    }
    
    /* destroy session */
    void destroySession() {
        if (session != null) {
            AuthUtils.removeAuthContext(sid);
            ad.destroySession(sid);
            sid = null;
            session = null;
        } 
    }
    
    /**
     * Checks for <code>persistentCookie</code>.
     */
    public void persistentCookieArgExists() {
        String arg = (String)requestHash.get(ISAuthConstants.PCOOKIE);
        if (arg != null && arg.length() != 0) {
            persistentCookieOn = arg.equalsIgnoreCase("yes");
        }
    }
    
    /**
     * Returns Session ID.
     *
     * @return Session ID.
     */
    public SessionID getSid() {
        return sid; 
    }
    
    public void setSid(SessionID aSid) {
        sid = aSid; 
    }

    public boolean getForceFlag() {
         return forceAuth;
    }

    public void setForceAuth(boolean force) {
        forceAuth = force;
    }

    /**
     * Enables AM session cookie time to live
     * @param flag if <code>true</code> enables AM session cookie time to live,
     *      otherwise disables AM session cookie time to live
     */
    public void enableCookieTimeToLive(boolean flag) {
        cookieTimeToLiveEnabledFlag = flag;
        if (ad.debug.messageEnabled()) {
            ad.debug.message("LoginState.enableCookieTimeToLive():"
                    + "enable=" + cookieTimeToLiveEnabledFlag);
        }
    }
 
    /**
     * Checks whether AM session cookie time to live is enabled
     * @return <code>true</code> if AM session cookie time to live
     *         is enabled, otherwise returns <code>false</code>
     */
    public boolean isCookieTimeToLiveEnabled() {
        if (ad.debug.messageEnabled()) {
            ad.debug.message("LoginState.isCookieTimeToLiveEnabled():"
                    + "enabled=" + cookieTimeToLiveEnabledFlag);
        }
        return cookieTimeToLiveEnabledFlag;
    }

    /**
     * Sets AM session cookie time to live
     * @param timeToLive AM session cookie time to live in seconds
     */
    public void setCookieTimeToLive(int timeToLive) {
        cookieTimeToLive = timeToLive;
        if (ad.debug.messageEnabled()) {
            ad.debug.message("LoginState.setCookieTimeToLive():"
                    + "cookieTimeToLive=" + cookieTimeToLive);
        }
    }

    /**
     * Returns AM session cookie time to live
     * @return AM session cookie time to live in seconds
     */
    public int getCookieTimeToLive() {
        if (ad.debug.messageEnabled()) {
            ad.debug.message("LoginState.getCookieTimeToLive():"
                    + "cookieTimeToLive=" + cookieTimeToLive);
        }
        return cookieTimeToLive;
    }

    /**
     * Returns user domain.
     *
     * @param request
     * @param sid
     * @param requestHash
     * @return user domain.
     */
    public String getUserDomain(
        HttpServletRequest request,
        SessionID sid,
        Hashtable requestHash) {
        String userOrg = null;
        
        try {
            SSOTokenManager manager = SSOTokenManager.getInstance();
            SSOToken ssoToken = manager.createSSOToken(request);
            if (!sessionUpgrade && !requestHash.isEmpty()
            && (manager.isValidToken(ssoToken))) {
                userOrg = ssoToken.getProperty("Organization");
                debug.message("User org from existing valid session");
            }
        } catch (Exception e) {
            debug.message("ERROR in getUserDomain - " + e.toString());
        }
        
        //org profile is not loaded yet so we can't check with getPersistentCookieMode()
        //but we will check if persistentCookie is there and use it because we will
        //verify if the pcookie is valid later anyways.
        String username =null;
        String cookieValue = CookieUtils.getCookieValueFromReq(request, pCookieName);
        if (cookieValue != null) {
            username = parsePersistentCookie(cookieValue);
            if (username != null) {
                //call to searchPersistentCookie should set userOrg
                //getDN or AuthD.getOrgDN doesn't return correct dn so using DNMapper
                orgDN = DNMapper.orgNameToDN(this.userOrg);
                if (!username.endsWith(orgDN)) {
                    orgDN = ServiceManager.getBaseDN();
                }
                userOrg = orgDN;
            }
        }

        if (userOrg == null) {
            if (AuthUtils.newSessionArgExists(requestHash, sid) &&
            sid.toString().length() > 0) {
                userOrg = sid.getSessionDomain();
            } else {
                userOrg = AuthUtils.getDomainNameByRequest(request,requestHash);
            }
        }
        
        if (messageEnabled) {
            debug.message("returning from getUserDomain : " + userOrg);
        }
        return userOrg;
    }
    
    
    /**
     * Returns authentication context for new request.
     *
     * @param request
     * @param response
     * @param sid
     * @param requestHash
     * @return Authentication context for new request.
     * @throws AuthException if it fails to instantiate <code>AuthContext</code>
     */
    public AuthContextLocal createAuthContext(
        HttpServletRequest request,
        HttpServletResponse response,
        SessionID sid,
        Hashtable requestHash
    ) throws AuthException {
        // Get / Construct the Original Login URL
        this.loginURL = AuthUtils.constructLoginURL(request);

        // Get query param indicating a request "forward" after
        // successful authentication.
        this.forwardSuccess = AuthUtils.forwardSuccessExists(request);

        // set the locale
        setRequestLocale(request);
        
        if (messageEnabled) {
            debug.message("locale : " + localeContext.getLocale());
        }

        this.userOrg = getUserDomain(request,sid,requestHash);
        if (messageEnabled) {
            debug.message("createAuthContext: userOrg is : " + userOrg);
        }

        if ((this.userOrg == null) || this.userOrg.length() == 0) {
            debug.message("domain is null, error condtion");
            logFailed(ad.bundle.getString("invalidDomain"),"INVALIDDOMAIN");
            throw new AuthException(AMAuthErrorCode.AUTH_INVALID_DOMAIN, null);
        }
        
        if (messageEnabled) {
            debug.message("AuthUtil:getAuthContext:" +
                "Creating new AuthContextLocal & LoginState");
        }
        AuthContextLocal authContext = new AuthContextLocal(this.userOrg);
        requestType = true;
        servletRequest = request;
        servletResponse = response;
        this.requestHash = requestHash;
        client = getClient();
        this.sid = sid;
        if (messageEnabled) {
            debug.message("requestType : " + requestType);
            debug.message("client : " + client);
            debug.message("sid : " + sid);
        }
        
        try {
            createSession(request,authContext);
        } catch (Exception e) {
            debug.error("Exception creating session .. :", e);
            throw new AuthException(e);
        }
        String cookieSupport = AuthUtils.getCookieSupport(getClientType());
        cookieDetect = AuthUtils.getCookieDetect(cookieSupport);
        if ((cookieSupport != null) && cookieSupport.equals("false")){
            cookieSupported = false;
        }
        if (messageEnabled) {
            debug.message("cookieSupport is : " + cookieSupport);
            debug.message("cookieDetect is .. : "+ cookieDetect);
            debug.message("cookieSupported is .. : "+ cookieSupported);
        }
        if (AuthUtils.isClientDetectionEnabled() && cookieDetect) {
            cookieSet = true;
        }
        setGoToURL();
        setGoToOnFailURL();
        amIdRepo = ad.getAMIdentityRepository(getOrgDN());
        persistentCookieArgExists();
        populateOrgProfile();
        populateGlobalProfile();
        return authContext;
    }
    
    /* create new session */
    void createSession(
        HttpServletRequest req,
        AuthContextLocal authContext
    ) throws AuthException {
        debug.message("LoginState: createSession: Creating new session: ");
        SessionID sid = null;
        debug.message("Save authContext in InternalSession");
        session = AuthD.newSession(getOrgDN(), null);
        //save the AuthContext object in Session
        sid = session.getID();
        session.setObject(ISAuthConstants.AUTH_CONTEXT_OBJ, authContext);

        this.sid = sid;
        if (debug.messageEnabled()) {
            debug.message(
                "LoginState:createSession: New session/sid=" + sid);
            debug.message("LoginState:New session: ac=" + authContext);
        }
    }
    
    /**
     * Returns the single sign on token associated with the session.
     *
     * @return the single sign on token associated with the session.
     * @throws SSOException
     */
    public SSOToken getSSOToken() throws SSOException {
        if ((session != null) && (session.getState() == Session.INACTIVE)) {
            return null;
        }

        try {
            SSOTokenManager ssoManager = SSOTokenManager.getInstance();
            SSOToken ssoToken = ssoManager.createSSOToken(session.getID().toString());
            return ssoToken;
        } catch (SSOException ex) {
            debug.message("Error retrieving SSOToken :", ex);
            throw new SSOException(AuthD.BUNDLE_NAME,
                AMAuthErrorCode.AUTH_ERROR, null);
        }
    }
    
    /**
     * Returns URL with the cookie value in the URL.
     *
     * @param url URL.
     * @param response HTTP Servlet Response.
     * @return Encoded URL.
     */
    public String encodeURL(String url,HttpServletResponse response) {
        return encodeURL(url,response,false);
    }
    
    /**
     * Returns URL with the cookie value in the URL.
     * The cookie in the rewritten url will have
     * the AM cookie if session is active/inactive and
     * auth cookie if cookie is invalid
     *
     * @param url
     * @param response HTTP Servlet Response.
     * @param useAMCookie
     * @return the encoded URL
     */
    public String encodeURL(
        String url,
        HttpServletResponse response,
        boolean useAMCookie) {

        if (messageEnabled) {
            debug.message("in encodeURL");
        }
        boolean appendSessCookieInURL = Boolean.valueOf(SystemProperties.get(
            Constants.APPEND_SESS_COOKIE_IN_URL,"true")).booleanValue();
        if (!appendSessCookieInURL) {
            return url;
        }

        if (messageEnabled) {
            debug.message("cookieDetect : " + cookieDetect);
            debug.message("cookieSupported : " + cookieSupported);
        }
        if (!cookieDetect && cookieSupported) {
            return url;
        }
        
        if (session == null) {
            return url;
        }
        
        String cookieName = AuthUtils.getCookieName();
        if (!useAMCookie && session.getState() == Session.INVALID) {
            cookieName = AuthUtils.getAuthCookieName();
        }
        
        String encodedURL = url;
        if (urlRewriteInPath) {
            encodedURL = session.encodeURL(
                url, SessionUtils.SEMICOLON, false, cookieName);
        } else {
            encodedURL = session.encodeURL(
                url, SessionUtils.QUERY, false, cookieName);
        }
        
        if (messageEnabled) {
            debug.message("AuthRequest encodeURL : URL=" + url +
            ", Rewritten URL=" + encodedURL);
        }
        return (encodedURL);
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
     * @param fileName
     * @return configured jsp file name
     */
    public String getFileName(String fileName) {
        String templateFile = AuthUtils.getFileName(fileName,getLocale(),getOrgDN(),
        servletRequest,ad.getServletContext(),indexType,indexName);
        
        return templateFile;
    }
    
    /**
     * Converts a byte array to a hex string.
     */
    private static String byteArrayToHexString(byte[] byteArray) {
        int readBytes = byteArray.length;
        StringBuilder hexData = new StringBuilder();
        int onebyte;
        for (int i=0; i < readBytes; i++) {
          onebyte = ((0x000000ff & byteArray[i]) | 0xffffff00);
          hexData.append(Integer.toHexString(onebyte).substring(6));
        }
        return hexData.toString();
    }    
    
    /**
     * Create user profile.
     *
     * @param token
     * @param aliasList
     * @return <code>true</code> if profile is successfully created.
     */
    public boolean createUserProfile( String token , Set aliasList ){
        try {
            if (!dynamicProfileCreation) {
                debug.message("Error this user requires a profile to login");
                return false;
            }
            // If the module is "Application" then do not create user profile
            
            if (isApplicationModule(authMethName)) {
                debug.message("No profile created for Application module");
                return false;
            }
            if (messageEnabled) {
                debug.message("Creating user entry: " + token);
                debug.message("aliasList : " + aliasList);
            }
            
            if (userCreationAttributes == null)  {
                userCreationAttributes = new HashMap();
            }
            // get alias list
            Map aliasMap = Collections.EMPTY_MAP;
            if ((aliasList != null) && !aliasList.isEmpty()) {
                // set alias attribute
                debug.message("Adding alias list to user profile");
                if ((externalAliasList != null) &&
                (!externalAliasList.isEmpty())) {
                    aliasList.addAll(externalAliasList);
                }
                aliasMap.put(ISAuthConstants.USER_ALIAS_ATTR,aliasList);
            }
            if (!aliasMap.isEmpty()) {
                userCreationAttributes.putAll(aliasMap);
            }
            if (messageEnabled) {
                debug.message("userCreationAttributes is : "
                + userCreationAttributes);
            }
            
            Set userPasswordSet = new HashSet(1);
            byte bytes[] = new byte[20];
            secureRandom.nextBytes(bytes);
            userPasswordSet.add(byteArrayToHexString(bytes));
            userCreationAttributes.put(
                ISAuthConstants.ATTR_USER_PASSWORD, userPasswordSet);
            
            amIdentityUser =
            createUserIdentity(token,userCreationAttributes,defaultRoles);
            
            userDN = getUserDN(amIdentityUser);
            
            Map p = amIdentityUser.getAttributes();
            if (amIdentityRole != null) {
                // retrieve the session attributes for the default role
                Map sattrs = amIdentityRole.getServiceAttributes(
                ISAuthConstants.SESSION_SERVICE_NAME);
                if (sattrs != null && !sattrs.isEmpty()) {
                    p.putAll(sattrs);
                }
            }
            populateUserAttributes(p,true,null);
            return true;
        } catch (Exception ex) {
            debug.error("Cannot create user profile for: " + token);
            if (messageEnabled){
                debug.message("Stack trace: ", ex);
            }
        }
        return false;
    }
    
    private String[] getDefaultSessionAttributes(String orgDN) {
        String defaultMaxSession = ad.getDefaultMaxSessionTime();
        String defaultIdleTime = ad.getDefaultMaxIdleTime();
        String defaultCacheTime = ad.getDefaultMaxCachingTime();
        
        Map map = ad.getOrgServiceAttributes(orgDN,
        ISAuthConstants.SESSION_SERVICE_NAME);
        if (!map.isEmpty()) {
            if (map.containsKey(ISAuthConstants.MAX_SESSION_TIME)) {
                defaultMaxSession = (String)((Set)map.get(
                ISAuthConstants.MAX_SESSION_TIME)).iterator().next();
            }
            if (map.containsKey(ISAuthConstants.SESS_MAX_IDLE_TIME)) {
                defaultIdleTime = (String)((Set)map.get(
                ISAuthConstants.SESS_MAX_IDLE_TIME)).iterator().next();
            }
            if (map.containsKey(ISAuthConstants.SESS_MAX_CACHING_TIME)) {
                defaultCacheTime = (String)((Set)map.get(
                ISAuthConstants.SESS_MAX_CACHING_TIME)).iterator().next();
            }
        }
        
        String[] attrs = new String[3];
        attrs[0] = defaultMaxSession;
        attrs[1] = defaultIdleTime;
        attrs[2] = defaultCacheTime;
        
        return attrs;
    }
    
    void populateUserAttributes(
        Map p,
        boolean loginStatus,
        AMIdentity amIdentity
    ) throws AMException {
        String[] sessionAttrs = getDefaultSessionAttributes(getOrgDN());
        
        if (messageEnabled) {
            debug.message("default max session time: " + sessionAttrs[0]
            + "\ndefault max idle time: " + sessionAttrs[1]
            + "\ndefault max caching time: " + sessionAttrs[2]);
        }
        
        try {
            userAuthConfig = CollectionHelper.getMapAttr(
                p, ISAuthConstants.AUTHCONFIG_USER, null);
            if (!loginStatus) {
                userFailureURLSet = (Set)p.get(
                ISAuthConstants.USER_FAILURE_URL);
                clientUserFailureURL = getRedirectUrl(userFailureURLSet);
                defaultUserFailureURL = tempDefaultURL;
                failureRoleURLSet =
                    (Set)p.get(ISAuthConstants.LOGIN_FAILURE_URL);
                clientFailureRoleURL = getRedirectUrl(failureRoleURLSet);
                defaultFailureRoleURL = tempDefaultURL;
                return;
            }
            
            maxSession = CollectionHelper.getIntMapAttr(
                p, ISAuthConstants.MAX_SESSION_TIME,
            sessionAttrs[0], debug);
            idleTime = CollectionHelper.getIntMapAttr(
                p, ISAuthConstants.SESS_MAX_IDLE_TIME, sessionAttrs[1], debug);
            cacheTime = CollectionHelper.getIntMapAttr(
                p, ISAuthConstants.SESS_MAX_CACHING_TIME, sessionAttrs[2],
                    debug);
            
            // Status determination
            String tmp = CollectionHelper.getMapAttr(
                p, ISAuthConstants.INETUSER_STATUS, "active");
            
            // OPEN ISSUE- amIdentity.isActive return true even if
            // user status is set to inactive.
            if (amIdentity != null) {
                tmp = amIdentity.isActive()? "active" : "inactive";
            }
            String tmp1 = CollectionHelper.getMapAttr(
                p, ISAuthConstants.LOGIN_STATUS, "active");
            String tmp2 = CollectionHelper.getMapAttr(
                p, ISAuthConstants.NSACCOUNT_LOCK, ISAuthConstants.FALSE_VALUE);
            if (messageEnabled) {
                debug.message("entity status is : " + tmp);
                debug.message("user-login-status is : " + tmp1);
                debug.message("nsaccountlock is : " + tmp2);
            }
            if (!tmp1.equalsIgnoreCase("active") ||
            !tmp.equalsIgnoreCase("active")  ||
            !tmp2.equalsIgnoreCase("false")) {
                userEnabled = false;
            }
            
            String ulocale = CollectionHelper.getMapAttr(
                p, ISAuthConstants.PREFERRED_LOCALE, null);
            localeContext.setUserLocale(ulocale);
            
            userAliasList = (Set)  p.get(ISAuthConstants.USER_ALIAS_ATTR);
            // add value from attributes in iplanet-am-auth-alias-attr-name
            if (aliasAttrNames != null && !aliasAttrNames.isEmpty()) {
                Iterator it = aliasAttrNames.iterator();
                while (it.hasNext()) {
                    String attrName = (String) it.next();
                    Set attrVals = (Set) p.get(attrName);
                    if (attrVals != null) {
                        if (userAliasList == null) {
                            userAliasList = new HashSet();
                        }
                        userAliasList.addAll(attrVals);
                    }
                }
            }
            accountLife = CollectionHelper.getMapAttr(
                p, ISAuthConstants.ACCOUNT_LIFE);
            
            // retrieve the user default success url
            // at user's role level
            userSuccessURLSet = (Set) p.get(ISAuthConstants.USER_SUCCESS_URL);
            clientUserSuccessURL = getRedirectUrl(userSuccessURLSet);
            defaultUserSuccessURL = tempDefaultURL;
            
            successRoleURLSet = (Set)p.get(ISAuthConstants.LOGIN_SUCCESS_URL);
            clientSuccessRoleURL = getRedirectUrl(successRoleURLSet);
            defaultSuccessRoleURL = tempDefaultURL;
            
            if (messageEnabled) {
                debug.message("Populate User attributes"+
                "\n  idle->" + idleTime +
                "\n  cache->" + cacheTime+
                "\n  max->" + maxSession+
                "\n  userLoginEnabled->" + userEnabled +
                "\n  charset->" + localeContext.getMIMECharset()+
                "\n  locale->" + localeContext.getLocale().toString() +
                "\n  userAlias->  :" + userAliasList+
                "\n  userSuccessURLSet-> :" + userSuccessURLSet+
                "\n  clientUserSuccessURL->  :" + clientUserSuccessURL+
                "\n  defaultUserSuccessURL->  :" + defaultUserSuccessURL+
                "\n  userFailureURLSet-> :" + userFailureURLSet+
                "\n  clientUserFailureURL->  :" + clientUserFailureURL+
                "\n  defaultUserFailureURL->  :" + defaultUserFailureURL+
                "\n  clientSuccessRoleURL ->  :" + clientSuccessRoleURL+
                "\n  defaultSuccessRoleURL ->  :" + defaultSuccessRoleURL+
                "\n  clientFailureRoleURL ->  :" + clientFailureRoleURL+
                "\n  defaultFailureRoleURL ->  :" + defaultFailureRoleURL+
                "\n  userAuthConfig -> : " + userAuthConfig +
                "\n  accountLife->" + accountLife);
            }
        } catch (Exception e) {
            if (messageEnabled) {
                debug.message("Eception in populateUserAttributes : ", e);
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
    public boolean getUserProfile(String token, boolean populate)
            throws AuthException {
        try {
            return getUserProfile(token,populate,true);
        } catch (Exception e) {
            if (messageEnabled) {
                debug.message("getUserProfile(string,boolean)", e);
            }
            throw new AuthException(e);
        }
    }

    /**
     * Returns <code>true</code> if user profile found.
     *
     * @param user userID for profile 
     * @param populate
     * @param loginStatus current login status for profile
     * @return <code>true</code> if user profile found. 
     * @throws AuthException if multiple user match found in search
     */
    public boolean getUserProfile(
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
            if (messageEnabled) {
                debug.message("In getUserProfile : Search for user " + user);
            }
            
            Set amIdentitySet = Collections.EMPTY_SET;
            IdSearchResults searchResults = null;
            if (ad.isSuperAdmin(user)) {
                // get the AMIdentity to get the universal
                // id of amAdmin, currently there is no support
                // for special users so the universal id in
                // the ssotoken will be amAdmin's id.
                AMIdentity amIdentity = ad.getIdentity(IdType.USER,
                    user, getOrgDN());
                amIdentitySet = new HashSet();
                amIdentitySet.add(amIdentity);
            } else {
                // Try getting the AMIdentity object assuming AMSDK
                // is present i.e., using IdUtils
                try {
                    if (messageEnabled) {
                        debug.message("LoginState: gettingIdentity " +
                        "using IdUtil.getIdentity: " + user +
                        " Org: " + getOrgDN());
                    }
                    AMIdentity amIdentity = IdUtils.getIdentity(
                    ad.getSSOAuthSession(), user, getOrgDN());
                    if (amIdentity != null &&
                    amIdentity.getAttributes() != null) {
                        amIdentitySet = new HashSet();
                        amIdentitySet.add(amIdentity);
                        idt = amIdentity.getType();
                        if (messageEnabled) {
                            debug.message("LoginState: getIdentity " +
                            "using IdUtil.getIdentity: " + amIdentity);
                        }
                    }
                } catch (IdRepoException e) {
                    // Ignore the exception and continue
                    if (messageEnabled) {
                        debug.message("LoginState: getting identity " +
                        "Got IdRepException in IdUtils.getIdentity", e);
                    }
                } catch (SSOException se) {
                    // Ignore the exception and continue
                    if (messageEnabled) {
                        debug.message("LoginState: getting identity " +
                        "Got SSOException in IdUtils.getIdentity", se);
                    }
                }
                
                // If amIdentitySet is still empty, or IdType does not match
                // search for all configured Identity Types
                if (amIdentitySet == Collections.EMPTY_SET ||
                !identityTypes.contains(idt.getName())) {
                    if (messageEnabled) {
                        debug.message("LoginState: getIdentity " +
                        "performing IdRepo search to obtain AMIdentity");
                    }
                        String userTokenID = DNUtils.DNtoName(user);
            
                    if (messageEnabled) {
                        debug.message("Search for Identity " + userTokenID);
                    }

                    Set tmpIdentityTypes = new HashSet(identityTypes);
                    if (identityTypes.contains("user")) {
                        tmpIdentityTypes.remove("user");
                        searchResults = searchIdentity(IdUtils.getType("user"),
                            userTokenID);
                        if (searchResults != null) {
                            amIdentitySet = searchResults.getSearchResults();
                        }
                    }
                    if (amIdentitySet.isEmpty()) {
                         Iterator identityIterator = tmpIdentityTypes.iterator();
                         while (identityIterator.hasNext()) {
                             String strIdType = (String) identityIterator.next();
                             // Get identity by searching
                             searchResults = searchIdentity(
                                 IdUtils.getType(strIdType),userTokenID);
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
            
            if (messageEnabled) {
                debug.message("result is :" + amIdentitySet);
            }
            if (amIdentitySet.isEmpty()) {
                return false;
            }
            // check if there is multiple match
            if (amIdentitySet.size() > 1) {
                // multiple user match found, throw exception,
                // user need to login as super admin to fix it
                debug.error("getUserProfile : Multiple matches found for " +
                "user '"+ token + "' in org " + orgDN +
                "\nPlease make sure user is unique within the login " +
                "organization, and contact your admin to fix the problem");
                throw new AuthException(AMAuthErrorCode.AUTH_ERROR, null);
            }
            amIdentityUser= (AMIdentity) amIdentitySet.iterator().next();
            userDN = getUserDN(amIdentityUser);
            idt = amIdentityUser.getType();
            if (messageEnabled) {
                debug.message("userDN is : "+ userDN);
                debug.message("userID(token) is : " + token);
                debug.message("idType is : " + idt);
            }
            if (populate) {
                Map basicAttrs = null;
                Map serviceAttrs = null;
                if (searchResults != null) {
                    basicAttrs = (Map)searchResults.getResultAttributes().get(
                    amIdentityUser);
                } else {
                    basicAttrs = amIdentityUser.getAttributes();
                }
                
                if (amIdentityRole != null) {
                    // role based auth. the specified role takes preference.
                    debug.message("retrieving session service from role");
                    if (amIdentityRole != null) {
                        //Fix for OPENAM-612 - this request is cached most of the time
                        Set oc = amIdentityRole.getAttribute("objectclass");
                        if (oc != null && oc.contains("iplanet-am-session-service")) {
                            serviceAttrs = amIdentityRole.getServiceAttributes(
                                    ISAuthConstants.SESSION_SERVICE_NAME);
                        }
                    }
                } else if (idt.equals(IdType.USER)) {
                    debug.message("retrieving session service from user");
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
        } catch(SSOException ex) {
            debug.error("SSOException");
            if (messageEnabled){
                debug.message("Stack trace: " , ex);
            }
        } catch(AMException ex) {
            debug.error("No aliases for: " + aliasAttrNames + "=" + token);
            if (messageEnabled){
                debug.message("Stack trace: " , ex);
            }
        } catch (IdRepoException ee) {
            if (messageEnabled) {
                debug.error("IdReporException ", ee);
            }
        }
        
        return false;
    }
    
    /**
     * Populate all the default user attribute for profile
     * @throws AMException if it fails to populate default user attributes
     */
    public void populateDefaultUserAttributes() throws AMException {
        String[] sessionAttrs = getDefaultSessionAttributes(getOrgDN());
        try {
            maxSession = Integer.parseInt(sessionAttrs[0]);
        } catch (Exception e) {
            maxSession = 120;
        }
        try {
            idleTime = Integer.parseInt(sessionAttrs[1]);
        } catch (Exception e) {
            idleTime = 30;
        }
        try {
            cacheTime = Integer.parseInt(sessionAttrs[2]);
        } catch (Exception e) {
            cacheTime = 3;
        }
        userEnabled = true;
        
        if (messageEnabled) {
            debug.message("Populate Default User attributes"+
            "\n  idle->" + idleTime +
            "\n  cache->" + cacheTime+
            "\n  max->" + maxSession+
            "\n  userLoginEnabled->" + userEnabled +
            "\n  clientUserSuccessURL ->" + clientUserSuccessURL +
            "\n  defaultUserSuccessURL ->" + defaultUserSuccessURL +
            "\n  clientUserFailureURL ->" + clientUserFailureURL +
            "\n  defaultUserFailureURL ->" + defaultUserFailureURL +
            "\n  clientSuccessRoleURL ->" + clientSuccessRoleURL+
            "\n  defaultSuccessRoleURL ->" + defaultSuccessRoleURL+
            "\n  clientFailureRoleURL ->" + clientFailureRoleURL+
            "\n  defaultFailureRoleURL ->" + defaultFailureRoleURL+
            "\n  userAuthConfig ->" + userAuthConfig+
            "\n  charset->" + localeContext.getMIMECharset()+
            "\n  locale->" + localeContext.getLocale().toString());
        }
    }
    
    /**
     * Search the user profile
     * if <code>IndexType</code> is USER and if number of tokens is 1 and
     * token is <code>superAdmin</code> then return. If more then 1 tokens
     * are found then make sure the user tokens are in
     * <code>iplanet-am-useralias-list</code>
     * <p>
     * If <code>IndexType</code> is <code>LEVEL</code>, <code>MODULE</code>
     * then there is only 1 user token retrieve the profile for the
     * authenticated user and create profile if dynamic profile creation
     * enabled.
     * <p>
     * If <code>IndexType</code> is <code>ORG</code>, <code>SERVICE</code>,
     * <code>ROLE</code> then retrieve the user profile for first token, if the
     * profile is found and <code>user-alias-list</code> contains other
     * tokens then continue, else try to retrieve remaining tokens till a match
     * is found.
     *
     * Checks all the users in the tokenSet are active else error
     * For ROLE based authentication checks if all user belong to the same Role.
     *
     * @param subject
     * @param indexType
     * @param indexName
     * @return <code>true</code> if it found user profile
     * @throws AuthException
     */
    public boolean searchUserProfile(
        Subject subject,
        AuthContext.IndexType indexType,
        String indexName
    ) throws AuthException {
        tokenSet = getTokenFromPrincipal(subject); 
        // check for all users user authenticated as
        if (messageEnabled) {
            debug.message("in searchUserProfile");
            debug.message("indexType is.. :" + indexType);
            debug.message("indexName is.. :" + indexName);
            debug.message("Subject is.. :" + subject);
            debug.message("token is.. :" + token);
            debug.message("tokenSet is.. :" + tokenSet);
            debug.message("pCookieUserName is.. :" + pCookieUserName);
            debug.message("ignoreUserProfile.. :" + ignoreUserProfile);
            debug.message("userDN is.. :" + userDN);
        }
        
        // retreive the tokens from the subject
        try {
            boolean gotUserProfile=true;
            if (((ignoreUserProfile && !isApplicationModule(indexName))) || 
                (isApplicationModule(indexName) && ad.isSuperAdmin(userDN))) {
                if (ad.isSuperAdmin(userDN)) {
                    amIdentityUser = ad.getIdentity(IdType.USER,userDN,getOrgDN());
                } else {
                    amIdentityUser = 
                        new AMIdentity(null,userDN,IdType.USER,getOrgDN(),null);
                }
                userDN = getUserDN(amIdentityUser);
                populateDefaultUserAttributes();
                return true;
            }            
            
            // for IndexType USER check all the token user
            // authenticated as is present
            // in the user-alias-list
            
            if ( (indexType == AuthContext.IndexType.USER) ||
            (pCookieUserName != null) ) {
                if ((token == null) && (pCookieUserName != null)) {
                    token = pCookieUserName;
                }
                if (token == null) {
                    return false;
                }
                getUserProfile(token, true);
                Map aliasFound = searchUserAliases(token,tokenSet);
                if (!checkAliasList(aliasFound)) {
                    if (createWithAlias) {
                        if (amIdentityUser == null) {
                            addAliasToUserProfile(amIdentityUser,aliasFound);
                        } else {
                            addAliasToUserProfile(token,aliasFound);
                        }
                    } else {
                        throw new AuthException(
                            AMAuthErrorCode.AUTH_LOGIN_FAILED,null);
                    }
                }
            } else {
                // for ORG / SERVICE / ROLE / MODULE / LEVEL
                boolean gotProfile=true;
                if (tokenSet.isEmpty()) {
                    debug.message("tokenset empty");
                    throw new AuthException(AMAuthErrorCode.AUTH_ERROR, null);
                } else if (tokenSet.size() == 1) {  // for level , module
                    debug.message("tokenset size is 1");
                    gotUserProfile = getCreateUserProfile(true);
                    if (!userEnabled) {
                        setFailedUserId(token);
                        throw new AuthException(
                        AMAuthErrorCode.AUTH_USER_INACTIVE, null);
                    }
                    if (ad.isSuperAdmin(userDN)) {
                        return true;
                    }
                    if (gotUserProfile) {
                        if (indexType == AuthContext.IndexType.ROLE) {
                            boolean userRoleFound = getUserForRole(
                                getIdentityRole(indexName,getOrgDN()));
                            if (messageEnabled) {
                                debug.message("userRoleFound: "
                                + userRoleFound);
                            }
                            if (!userRoleFound) {
                                logFailed(AuthUtils.getErrorVal(AMAuthErrorCode.
                                AUTH_USER_NOT_FOUND,AuthUtils.ERROR_MESSAGE),
                                "USERNOTFOUND");
                                throw new AuthException(
                                AMAuthErrorCode.AUTH_USER_NOT_FOUND, null);
                            }
                        }
                    }
                } else { // came here multiple users found
                    debug.message("came here !! multiple modules , users ");
                    
                    // initialize variables required
                    
                    String validToken = null;
                    gotUserProfile = false;
                    boolean foundUserAlias=false;
                    boolean userRoleFound = true;
                    Map userEnabledMap =  new HashMap();
                    Map userRoleFoundMap = new HashMap();
                    Map foundAliasMap = new HashMap();
                    Map gotUserProfileMap = new HashMap();
                    Boolean boolValFalse = Boolean.FALSE;
                    String aliasToken = null;
                    
                    Iterator tokenIterator = tokenSet.iterator();
                    while (tokenIterator.hasNext()) {
                        token = (String) tokenIterator.next();
                        if (messageEnabled) {
                            debug.message("BEGIN WHILE: Token is.. : "
                            + token);
                        }
                        gotUserProfile = getUserProfile(token,true);
                        gotUserProfileMap.put(token,
                            Boolean.valueOf(gotUserProfile));
                        if (messageEnabled) {
                            debug.message("gotUserProfile : " + gotUserProfile);
                        }
                        if (gotUserProfile) {
                            if (validToken == null) {
                                validToken = token;
                            }
                            userEnabledMap.put(token, 
                                Boolean.valueOf(userEnabled));
                            
                            if (indexType == AuthContext.IndexType.ROLE) {
                                userRoleFound = getUserForRole(
                                    getIdentityRole(indexName, getOrgDN()));
                                userRoleFoundMap.put(token, 
                                    Boolean.valueOf(userRoleFound));
                            }
                            foundAliasMap = searchUserAliases(token, tokenSet);
                            if (foundUserAlias=
                                getFoundUserAlias(foundAliasMap)) {
                                aliasToken =token;
                                if (messageEnabled) {
                                    debug.message(
                                        "found aliases exiting while:"
                                        + foundAliasMap);
                                }
                                break;
                            }
                        }
                    } // end while
                    if (messageEnabled) {
                        debug.message("Alias Token is : " + aliasToken);
                        debug.message("Profile Token :" + validToken);
                        debug.message("Token is : " + token);
                    }
                    if (aliasToken != null) {
                        token = aliasToken;
                    }
                    if (!hasAdminToken) {
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
                                AUTH_USER_NOT_FOUND,AuthUtils.ERROR_MESSAGE),
                                "USERNOTFOUND");
                                throw new AuthException(
                                AMAuthErrorCode.AUTH_USER_NOT_FOUND, null);
                            }
                            if (messageEnabled) {
                                debug.message("userRoleFound:"
                                +userRoleFound);
                            }
                        }
                        
                        gotUserProfile = getGotUserProfile(gotUserProfileMap);
                        
                        if (messageEnabled) {
                            debug.message("userEnabled : " + userEnabled);
                        }
                        
                /* if user profile is found but other tokens in do
                 * are not found in iplanet-am-user-alias list and
                 * if dynamic profile creation with user alias is
                 * enabled then add tokens to iplanet-am-user-alias-list
                 * to the token's profile
                 */
                        
                        if ((gotUserProfile) && (!foundUserAlias)) {
                            if (createWithAlias) {
                                if (messageEnabled) {
                                    debug.message("dynamicProfileCreation : "
                                    + dynamicProfileCreation);
                                    debug.message("foundUserAliasMap : "
                                    + foundAliasMap);
                                    debug.message("foundUserAliasMap : "
                                    + foundUserAlias);
                                }
                                addAliasToUserProfile(validToken,foundAliasMap);
                            } else { //end dynamic profile creation
                                throw new AuthException(
                                AMAuthErrorCode.AUTH_LOGIN_FAILED, null);
                            }
                        }
                        if (createWithAlias && !gotUserProfile) {
                            gotUserProfile =
                            createUserProfileForTokens(tokenSet,
                            gotUserProfileMap);
                        }
                    }
                }
            }
            if (messageEnabled) {
                debug.message("LoginState:searchUserProfile:returning: "
                + gotUserProfile);
            }
            return gotUserProfile;
        } catch (AuthException e) {
            throw new AuthException(e);
        } catch (Exception e) {
            debug.error("Error retrieving profile", e);
            throw new AuthException(e);
        }
    }
    
    /**
     * Returns user's profile , if not found then create user profile.
     * @param populate indicate if populate all default user attributes
     * @return <code>true</code> if created user profile successfully
     * @throws AuthException if fails create user profile
     */
    boolean getCreateUserProfile(boolean populate)
            throws AuthException {
        boolean gotProfile = false;
        if (userDN != null) {
            gotProfile = getUserProfile(userDN,populate);
        } else {
            gotProfile = getUserProfile(token,populate);
        }
        if ( !gotProfile) { 
            if (!ad.isSuperAdmin(userDN)) {
                 gotProfile = createUserProfile(token,null);
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
    boolean createUserProfileForTokens(Set tokenSet,Map gotUserProfileMap) {
        
        // retrieve the first token
        // put the other tokens in a list
        // these will put in the alias list attribute
        // of first token's profile
        
        Set tokensList = new HashSet();
        String token=null;
        Iterator tokenIterator = tokenSet.iterator();
        
        while (tokenIterator.hasNext()) {
            token = (String) tokenIterator.next();
            if (ad.isSuperAdmin(token)) {
                break;
            }
            while(tokenIterator.hasNext()) {
                Object alias = tokenIterator.next();
                if (messageEnabled) {
                    debug.message("alias list add token:" + (String) alias);
                }
                tokensList.add(alias);
            }
        }
        
        if (messageEnabled) {
            debug.message("Tokens List is.. :" + tokensList);
        }
        
        try {
            boolean profileCreated = createUserProfile(token,tokensList);
            return profileCreated;
        } catch (Exception e) {
            debug.error("Cannot create user profile for: " + token);
            if (messageEnabled){
                debug.message("Stack trace: ", e);
            }
            return false;
        }
    }
    
    
    /* search the user-alias-list for token names */
    Map searchUserAliases(String userToken,Set tokenSet) {
        Map foundUserAliasMap = new HashMap();
        
        if (messageEnabled) {
            debug.message("userAliastList is.. :" + userAliasList);
            debug.message("userToken is.. :" + userToken);
            debug.message("tokenSet is.. :" + tokenSet);
        }
        if ((tokenSet != null) && (!tokenSet.isEmpty())) {
            Iterator tokenIterator = tokenSet.iterator();
            
            // iterate through the tokens in the token set
            // and check if the tokens are  in the user alias
            // list of the user token. if yes then update
            // the found user alias map with the token and boolean
            // true else boolean false.
            while (tokenIterator.hasNext()) {
                String authToken = (String)tokenIterator.next();
                if ((userAliasList != null) && !userAliasList.isEmpty()) {
                    if (messageEnabled) {
                        debug.message("AuthToken is : " + authToken);
                        debug.message("userToken is : " + userToken);
                    }
                    if (((authToken != null)
                    && authToken.equalsIgnoreCase(userToken))
                    && (!foundUserAliasMap.containsKey(authToken))) {
                        foundUserAliasMap.put(authToken,Boolean.TRUE);
                    } else if (userAliasList.contains(authToken))  {
                        foundUserAliasMap.put(authToken,Boolean.TRUE);
                    } else {
                        foundUserAliasMap.put(authToken,Boolean.FALSE);
                    }
                } else {
                    if ((authToken!=null)
                    && authToken.equalsIgnoreCase(userToken)) {
                        foundUserAliasMap.put(authToken,Boolean.TRUE);
                    } else {
                        foundUserAliasMap.put(authToken,Boolean.FALSE);
                    }
                }
            }
            if (messageEnabled) {
                debug.message("searchUserAliases: foundUserAliasMap : "
                + foundUserAliasMap);
            }
        }
        return foundUserAliasMap;
    }
    
    /**
     * Returns tokens from Principals of a subject
     * returns a Set of tokens
     * TODO - DN to Universal ID?
     * @param subject Principals of a subject associated with 
     *        <code>SSOToken</code>
     * @return set of  <code>SSOToken</code> associated with subject
     */
    Set getTokenFromPrincipal(Subject subject) {
        Set principal = subject.getPrincipals();
        Set tokenSet = new HashSet();
        StringBuffer pList = new StringBuffer();
        Iterator p = principal.iterator();

        while (p.hasNext()) {
            this.token = ((Principal)p.next()).getName();
            if (this.token != null && !containsToken(pList, token)) {
                pList.append(this.token).append("|");
                String tmpDN = DNUtils.normalizeDN(this.token);
                if (tmpDN != null) {
                    this.userDN = tmpDN;
                    this.token = DNUtils.DNtoName(this.token);
                } else if (tmpDN == null && this.userDN == null) {
                    this.userDN = this.token;
                }
            }

            if (!tokenSet.contains(this.token)) {
                tokenSet.add(this.token);
            }

            if (messageEnabled) {
                debug.message("principal name is... :" + this.token);
            }
        }
        
        principalList = pList.toString();
        if (principalList != null) {
            principalList = principalList.substring(0, 
                principalList.length() - 1); // remove the last "|"
        }
        
        if (messageEnabled) {
            debug.message("Principal List is :" + principalList);
        }
        
        return tokenSet;
    }
    
    /**
     * Returns <code>true</code> if user is active.
     *
     * @return <code>true</code> if user is active.
     */
    public boolean isUserEnabled() {
        return userEnabled;
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
            }catch(NumberFormatException e) {
                this.authLevel = 0;
            }
        }
        
        if (this.authLevel < moduleAuthLevel) {
            this.authLevel = moduleAuthLevel;
        }
        
        if (messageEnabled) {
            debug.message("AuthLevel is set to : " + this.authLevel);
        }
    }
    
    /**
     * Returns the DN for role.
     *
     * @param roleName Name of role.
     * @param orgDN Organization DN.
     * @return the DN for role.
     */ 
    public AMIdentity getIdentityRole(String roleName,String orgDN) {
        if (amIdentityRole == null) {
            amIdentityRole = searchIdentityRole(roleName,orgDN);
        }
        return amIdentityRole;
    }
    
    
    /**
     * Returns Identity Role.
     *
     * @param role Name of role.
     * @param orgDN Organization DN.
     * @return Identity Role.
     */
    AMIdentity searchIdentityRole(String role,String orgDN) {
        if (messageEnabled) {
            debug.message("rolename : " + role);
        }
        if (role == null) {
            return null;
        }
        
        AMIdentity amIdRole = null;
        
        try {
            // search for this role name in organization
            amIdRole = getRole(role);
        } catch (Exception e) {
            debug.error("getRole: Error : ", e);
        }
        
        return amIdRole;
    }
    
    /**
     * Sets auth module name.
     *
     * @param authMethName Module Name.
     */
    public void setAuthModuleName(String authMethName) {
        if (messageEnabled) {
            debug.message("authethName" + authMethName);
            debug.message("pAuthMethName " + pAuthMethName);
        }
        StringBuffer sb = null;
        if ((this.pAuthMethName != null) && (this.pAuthMethName.length() > 0)){
            sb=new StringBuffer().append(this.pAuthMethName);
        }
        if ((authMethName != null) && (authMethName.length() > 0)) {
            if (sb !=null) {
                sb.append(ISAuthConstants.PIPE_SEPARATOR).append(authMethName);
            } else {
                sb = new StringBuffer().append(authMethName);
            }
        }
        if (sb != null) {
            this.authMethName = sb.toString();
        }
        if (messageEnabled) {
            debug.message("setAuthModuleName: " + this.authMethName);
        }
    }
    
    /**
     * Returns <code>true</code> if the user belongs to role.
     *
     * @param amIdentityRole Role object.
     * @return <code>true</code> if the user belongs to role.
     */
    public boolean getUserForRole(AMIdentity amIdentityRole) {
        
        String val=null;
        boolean foundUser=false;
        try {
            if (amIdentityUser.isMember(amIdentityRole)) {
                foundUser = true;
            }
        } catch (Exception e) {
            if (messageEnabled) {
                debug.message("Error getRoleName : " , e);
            }
        }
        return foundUser;
    }
    
    /**
     * Sets the indexType.
     * @param indexType name of indexType to be set
     */
    void setIndexType(AuthContext.IndexType indexType) {
        this.indexType = indexType;
    }
    
    /**
     * Sets the previous index type in authlevel after the choice is made.
     * @param prevIndexType name of indexType to be set 
     */
    void setPreviousIndexType(AuthContext.IndexType prevIndexType) {
        this.prevIndexType = indexType;
    }
    
    /**
     * Returns persistent cookie argument.
     *
     * @return persistent cookie argument.
     */
    public boolean isPersistentCookieOn() {
        return persistentCookieOn;
    }
    
    /**
     * Returns persistent cookie profie.
     *
     * @return persistent cookie profie.
     */
    public boolean getPersistentCookieMode() {
        return persistentCookieMode;
    }

    /**
     * Tells whether zero page login is enabled
     *
     * @return <code>true</code> if zero page login is enabled
     */
    public boolean isZeroPageLoginEnabled() {
        return zeroPageLoginEnabled;
    }

    void setToken(String token) {
        this.token = token;
    }
   
    /**
     * Return saved request parameters in <code>Hashtable</code>
     * @return saved request parameters in <code>Hashtable</code> 
     */
    public Hashtable getRequestParamHash() {
        return requestHash;
    }
    
    /* check if the user list has inactive user */
    boolean getUserEnabled(Map userEnabledMap) {
        Boolean boolValFalse = Boolean.FALSE;
        if (userEnabledMap.containsValue(boolValFalse)) {
            userEnabled = false;
        } else {
            userEnabled = true;
        }
        
        return userEnabled;
    }
    
    /* check if the users belong to role */
    boolean getUserRoleFound(Map userRoleFoundMap) {
        boolean userRoleFound = true;
        Boolean boolValFalse = Boolean.FALSE;
        if (userRoleFoundMap.containsValue(boolValFalse)) {
            userRoleFound=false;
        }
        return userRoleFound;
    }
    
    /* check if the users map to the same user */
    boolean getFoundUserAlias(Map foundAliasMap) {
        if (messageEnabled) {
            debug.message("foundAliasMap :" + foundAliasMap);
        }
        boolean foundUserAlias= true;
        Boolean boolValFalse= Boolean.FALSE;
        
        if (foundAliasMap == null || foundAliasMap.isEmpty() ||
        foundAliasMap.containsValue(boolValFalse)) {
            foundUserAlias = false;
        }
        if (messageEnabled) {
            debug.message("foundUserAlias : " + foundUserAlias);
        }
        return foundUserAlias;
    }
    
    /* check if profile for the user was retrieve */
    boolean getGotUserProfile(Map gotUserProfileMap) {
        if (messageEnabled) {
            debug.message("GotUserProfileMAP is: " + gotUserProfileMap);
        }
        boolean gotUserProfile = false;
        Boolean boolValTrue = Boolean.TRUE;
        if (gotUserProfileMap.containsValue(boolValTrue)) {
            gotUserProfile=true;
        }
        if (messageEnabled) {
            debug.message("gotUserProfile :" + gotUserProfile);
        }
        return gotUserProfile;
    }
    
    /* add token to iplanet-am-user-alias-list of the token which has
     * a profile
     */
    void addAliasToUserProfile(String token,Map foundUserAliasMap)
            throws AuthException {
        if (messageEnabled) {
            debug.message("Token : " + token);
        }
        
        AMIdentity amIdentityUser =
            ad.getIdentity(IdType.USER, token, getOrgDN());
        addAliasToUserProfile(amIdentityUser,foundUserAliasMap);
        return;
    }
    
    /* add token to iplanet-am-user-alias-list of the identity which has
     * a profile
     */
    void addAliasToUserProfile(AMIdentity amIdentity,Map foundUserAliasMap) {
        
        if (messageEnabled) {
            debug.message("foundUserAliasMap : " + foundUserAliasMap);
        }
        
        try {
            if ((foundUserAliasMap != null) && !foundUserAliasMap.isEmpty()) {
                // set alias attribute
                Set aliasKeySet = foundUserAliasMap.keySet();
                Iterator aliasIterator = aliasKeySet.iterator();
                while (aliasIterator.hasNext()) {
                    String token1 = (String) aliasIterator.next();
                    if ((token != null && !token.equalsIgnoreCase(token1)) &&
                    (!userAliasList.contains(token1))) {
                        userAliasList.add(token1);
                    }
                }
                debug.message("Adding alias list to user profile");
                Map aliasMap = new HashMap();
                if ((externalAliasList != null)
                && (!externalAliasList.isEmpty())) {
                    userAliasList.addAll(externalAliasList);
                }
                aliasMap.put(ISAuthConstants.USER_ALIAS_ATTR, userAliasList);
                amIdentity.setAttributes(aliasMap);
                amIdentity.store();
            }
        } catch (Exception e) {
            debug.error("Exception : " + e.getMessage(), e);
        }
    }
    
    
    /* check alias list for tokens , if superAdmin token
     * exists then amAdmin need not exist in the user-alias-list
     */
    boolean checkAliasList(Map userAliasList) {
        
        if (messageEnabled) {
            debug.message("UserAliasList is.. : "+ userAliasList);
        }
        boolean aliasFound=true;
        Set aliasKeySet = userAliasList.keySet();
        Iterator aliasIterator = aliasKeySet.iterator();
        while (aliasIterator.hasNext()) {
            Object token1 =  aliasIterator.next();
            if (messageEnabled) {
                debug.message("Token is.. : "+ (String)token1);
            }
            String newToken = tokenToDN((String)token1);
            if (!ad.isSuperAdmin(newToken)) {
                Boolean val = (Boolean) userAliasList.get(token1);
                if (val.toString().equals("false")) {
                    aliasFound = false;
                    break;
                }
            }
        }
        return aliasFound;
    }
    
    /**
     * Searches persistent cookie in request.
     *
     * @return user name set in persistent cookie, null if no persistent cookie
     *         found
     */
    public String searchPersistentCookie() {
        try {
            String username = null;
            // trying to find persistent cookie
            String cookieValue = CookieUtils.getCookieValueFromReq(
            servletRequest, pCookieName);
            if (cookieValue != null) {
                username = parsePersistentCookie(cookieValue);
            }
            
            return username;
        } catch (Exception e) {
            if (messageEnabled) {
                debug.message("ERROR searchPersistentCookie ",e);
            }
            return null;
        }
    }
    
    /**
     * parse persistent cookie parameters
     * persistent cookie has form:
     * [pCookie]=
     *    username%domainname%authMethod%maxSession%idleTime%cacheTime%sid%time
     * @param   cookieValue     Value of the persistent cookie
     * @return user name set in persistent cookie, return if no PC found
     */
    private String parsePersistentCookie(String cookieValue) {
        try {
            foundPCookie=Boolean.FALSE;
            String encodedInvalidCookie = (String) AccessController.
            doPrivileged(new EncodeAction(ISAuthConstants.INVALID_PCOOKIE));
            if (cookieValue == null || cookieValue.length() == 0 ||
            cookieValue.equals(encodedInvalidCookie)) {
                return null;
            }
            
            // some decryption/extraction to be done here
            String decode_cookieValue = (String) AccessController.
            doPrivileged(new DecodeAction(cookieValue));
            
            // check domain param in cookie value
            int domainIndex = decode_cookieValue.indexOf(
            ISAuthConstants.PERCENT);
            if (domainIndex == -1) {
                //  treat as if cookie not found
                return null;
            }
            
            // set user name
            String usernameStr = decode_cookieValue.substring(0, domainIndex);
            // tmpstr points to start of domainname
            String tmpstr = decode_cookieValue.substring(domainIndex+1);
            // check auth method param in cookie value
            int authMethIndex = tmpstr.indexOf(ISAuthConstants.PERCENT);
            if (authMethIndex == -1) {
                // clear our cookie
                //  treat as if cookie not found
                return null;
            }
            
            // set domain name string
            String domainStr = tmpstr.substring(0, authMethIndex);
            // tmpstr2 point to the start of auth method
            String tmpstr2 = tmpstr.substring(authMethIndex+1);
            // check maxSession param in cookie value
            int tmpIndex = tmpstr2.indexOf(ISAuthConstants.PERCENT);
            if (tmpIndex == -1) {
                // clear our cookie
                //  treat as if cookie not found
                return null;
            }
            
            // set auth method string
            String authMethStr = tmpstr2.substring(0, tmpIndex);
            // tmpstr point to the start of maxSession
            tmpstr = tmpstr2.substring(tmpIndex+1);
            // check idle Time string
            tmpIndex = tmpstr.indexOf(ISAuthConstants.PERCENT);
            if (tmpIndex == -1) {
                //  treat as if cookie not found
                return null;
            }
            
            // set max session
            int maxSession = Integer.parseInt(tmpstr.substring(0, tmpIndex));
            // tmpstr2 point to the start of idleTime
            tmpstr2 = tmpstr.substring(tmpIndex+1);
            // check cacheTime in cookie value
            tmpIndex = tmpstr2.indexOf(ISAuthConstants.PERCENT);
            if (tmpIndex == -1) {
                //  treat as if cookie not found
                return null;
            }
            
            // set idle time
            int idleTime = Integer.parseInt(tmpstr2.substring(0, tmpIndex));
            
            // set cache time
            tmpstr2 = tmpstr2.substring(tmpIndex + 1);
            tmpIndex = tmpstr2.indexOf(ISAuthConstants.PERCENT);
            if (tmpIndex == -1) {
                // treat as if cookie  not found
                return null;
            }
            
            int cacheTime = Integer.parseInt(tmpstr2.substring(0,tmpIndex));
            
            tmpstr2 = tmpstr2.substring(tmpIndex + 1);
            tmpIndex = tmpstr2.indexOf(ISAuthConstants.PERCENT);
            if (tmpIndex == -1) {
                // treat as if cookie  not found
                return null;
            }
            
            // get sid
            String oldSidString = tmpstr2.substring(0,tmpIndex);
            
            // get the time the pCookie was created
            pCookieTimeCreated = Long.parseLong(tmpstr2.substring(tmpIndex+1));
            if (messageEnabled) {
                debug.message("pCookieTimeCreated : " + pCookieTimeCreated);
            }
            // clean up auth internal tables
            if (!sessionUpgrade) {
                SessionID oldSessionID = new SessionID(oldSidString);
                AuthD.getSS().destroyInternalSession(oldSessionID);
            }

            userOrg = domainStr;

            String orgDN = DNMapper.orgNameToDN(userOrg);
            if (!usernameStr.endsWith(orgDN)) {
                orgDN = ServiceManager.getBaseDN();
            }

            OrganizationConfigManager orgConfigMgr = ad.getOrgConfigManager(orgDN);
            ServiceConfig svcConfig = orgConfigMgr.getServiceConfig(ISAuthConstants.AUTH_SERVICE_NAME);

            Map attrs = svcConfig.getAttributes();
            persistentCookieTime = CollectionHelper.getMapAttr(attrs, ISAuthConstants.PERSISTENT_COOKIE_TIME);
            int value = -1;
            try {
                value = Integer.parseInt(persistentCookieTime);
            } catch (NumberFormatException nfe) {
                //ignore
            }

            if ((pCookieTimeCreated + value * 1000) < System.currentTimeMillis()) {
                //the cookie should have already reach its lifetime
                return null;
            }
            
            if (messageEnabled) {
                debug.message("authMethStr: " + authMethStr);
            }
            pAuthMethName = authMethStr;
            if (messageEnabled) {
                debug.message("Found valid PC : username=" + usernameStr +
                    "\ndomainname=" + domainStr + "\nauthMethod=" +
                    pAuthMethName + "\nmaxSession=" + maxSession +
                    "\nidleTime=" + idleTime + "\ncacheTime=" + cacheTime +
                    "\norgDN=" + orgDN);
            }
            
            foundPCookie=Boolean.TRUE;
            return usernameStr;
        } catch (Exception e) {
            if (messageEnabled) {
                debug.message("ERROR:parsePersistentCookie : " , e);
            }
            return null;
        }
    }

    /**
     * Encode persistent cookie
     * @return encoded value of persistent cookie
     */
    public static String encodePCookie() {
        return (String) AccessController.doPrivileged(
        new EncodeAction(ISAuthConstants.INVALID_PCOOKIE));
    }
    
    /**
     * Sets persistent cookie in request
     * TO TAKE CARE OF LOAD BALANCING COOKIE
     * @param cookieDomain name of cookie domain for persistent cookie
     * @return persistent cookie in request
     * @throws SSOException
     * @throws AMException
     */
    public Cookie setPersistentCookie(String cookieDomain)
            throws SSOException, AMException {
        String maxage_str = persistentCookieTime;
        Cookie pCookie = null;
        if (maxage_str != null) {
            int maxAge;
            try {
                maxAge = Integer.parseInt(maxage_str);
                if (foundPCookie !=null && foundPCookie.booleanValue()) {
                    long timeRem =
                    System.currentTimeMillis()-pCookieTimeCreated;
                    maxAge = maxAge - (new Long(timeRem/1000)).intValue();
                }
            } catch (Exception Ex2) {
                maxAge = 0;
            }
            if (messageEnabled) {
                debug.message("Add Cookie: maxage=" + maxAge);
                debug.message("Add Cookie: maxage_str=" + maxage_str);
            }
            
            if (maxAge > 0) {
                String cookiestr = getUserDN(amIdentityUser) +
                    "%" + getOrgName() +
                    "%" + authMethName + "%" + Integer.toString(maxSession) +
                    "%" + Integer.toString(idleTime) +
                    "%" + Integer.toString(cacheTime) +
                    "%" + sid.toString() +
                    "%" + (pCookieTimeCreated != 0 ? pCookieTimeCreated : System.currentTimeMillis());
                String pCookieValue = (String) AccessController.doPrivileged(
                    new EncodeAction(cookiestr));
                pCookie = AuthUtils.createPersistentCookie(
                    AuthUtils.getPersistentCookieName(),
                    pCookieValue, maxAge, cookieDomain);

                if (messageEnabled) {
                    debug.message("Add PCookie = " + cookiestr);
                }
            } else {
                if (messageEnabled) {
                    debug.message("Persistent Cookie Mode"+
                        " configured for domain " + orgName +
                        ", but no persistentCookieTime = " + maxage_str);
                }
                
            }
        }
        return pCookie;
    }
    
    /**
     * set Load Balance Cookie
     * @param cookieDomain name of cookie domain for persistent cookie
     * @param persist indicates if cookie is persistent
     * @return persistent cookie in request
     * @throws SSOException
     * @throws AMException
     */
    public Cookie setlbCookie(String cookieDomain, boolean persist)
            throws SSOException, AMException {
        String cookieName = AuthUtils.getlbCookieName();
        String cookieValue = AuthUtils.getlbCookieValue();
        String maxage_str = persistentCookieTime;
        Cookie lbCookie = null;
        if ((maxage_str!= null) && persist) {
            int maxAge;
            try {
                maxAge = Integer.parseInt(maxage_str);
            } catch (Exception Ex2) {
                maxAge = 0;
            }
            if (messageEnabled) {
                debug.message("Add Load Balance Cookie: maxage=" + maxAge);
            }
            
            if (maxAge > 0) {
                lbCookie = AuthUtils.createPersistentCookie(
                    cookieName, cookieValue,
                maxAge, cookieDomain);
                debug.message("Add Load Balance Cookie!");
            } else {
                debug.message("No Load Balance Cookie set!");
            }
        } else {
            lbCookie = AuthUtils.createPersistentCookie(
                cookieName, cookieValue, -1, cookieDomain);
        }
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
     * Returns the previous index type in authentication level after module
     * selection.
     *
     * @return the previous index type in authentication level after module
     * selection.
     */
    public AuthContext.IndexType getPreviousIndexType() {
        return prevIndexType;
    }
    
    /**
     * Sets goto URL.
     */
    void setGoToURL() {
        String arg = (String)requestHash.get("goto");
        if (arg != null && arg.length() != 0) {
            String encoded = servletRequest.getParameter("encoded");
            if (encoded != null && encoded.equals("true")) {
                gotoURL = AuthUtils.getBase64DecodedValue(arg);
            } else {
                gotoURL = arg;
            }
        }
    }
    
    /**
     * Sets gotoOnFail URL.
     */
    void setGoToOnFailURL() {
        String arg = (String)requestHash.get("gotoOnFail");
        if (arg != null && arg.length() != 0) {
            String encoded = servletRequest.getParameter("encoded");
            if (encoded != null && encoded.equals("true")) {
                gotoOnFailURL = AuthUtils.getBase64DecodedValue(arg);
            } else {
                gotoOnFailURL = arg;
            }
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
        //if not try to retrive it from session property
        //Success URL from Post Auth takes pracedence
        if ((postProcessGoto == null) && (session != null))
                postProcessGoto =
                    session.getProperty(ISAuthConstants.POST_PROCESS_SUCCESS_URL);
        if ((postProcessGoto != null) && (postProcessGoto.length() > 0)) {
             return postProcessGoto;
        }
        String currentGoto = (servletRequest == null)?
        null: servletRequest.getParameter("goto");
        if (messageEnabled) {
            ad.debug.message("currentGoto : " + currentGoto);
        }
        String fqdnURL = null;
        if ((currentGoto != null) && (currentGoto.length() != 0) &&
        (!currentGoto.equalsIgnoreCase("null"))) {
            String encoded = servletRequest.getParameter("encoded");
            if (encoded != null && encoded.equals("true")) {
                currentGoto = AuthUtils.getBase64DecodedValue(currentGoto);
            }
            if (!ad.isGotoUrlValid(currentGoto, getOrgDN())) {
                if (messageEnabled) {
                    ad.debug.message("LoginState.getSuccessLoginURL():" +
                    "Original goto URL is " + currentGoto + " which is " +
                    "invalid");
                }
	            currentGoto = null;                
            }
        }

        if ((currentGoto != null) && (currentGoto.length() != 0) &&
                 (!currentGoto.equalsIgnoreCase("null"))) {            
            fqdnURL = ad.processURL(currentGoto, servletRequest);
        } else if ((fqdnURL == null) || (fqdnURL.length() == 0))  {
            fqdnURL = ad.processURL(successLoginURL, servletRequest);
        }        
        
        String encodedSuccessURL = encodeURL(fqdnURL,servletResponse,true);
        if (messageEnabled) {
            ad.debug.message("get fqdnURL : " + fqdnURL);
            ad.debug.message("get successLoginURL : " 
                             + successLoginURL);
            ad.debug.message("get encodedSuccessURL : " 
                             + encodedSuccessURL);
        }
        return encodedSuccessURL;
    }
   
    /**
     * Returns configured success login URL.
     *
     * @return configured success login URL.
     */
    public String getConfiguredSuccessLoginURL() {
    /* this method for UI, called from AuthUtils */
        String encodedSuccessURL =
        encodeURL(ad.processURL(
            successLoginURL,servletRequest),servletResponse,true);
        if (messageEnabled) {
            debug.message("getSuccessLoginURL : " + successLoginURL);
            debug.message(
                "getSuccessLoginURL (encoded) : " + encodedSuccessURL);
        }
        return encodedSuccessURL;
    }
    
    String getSuccessURLForRole() {
        String roleURL = null;
        try {
            Map roleAttrMap = getRoleServiceAttributes();
                roleURL =getRoleURLFromAttribute(roleAttrMap,
                ISAuthConstants.LOGIN_SUCCESS_URL);
        } catch (Exception e) {
            if (messageEnabled) {
                debug.message("Execption:getSuccessURLForRole : " , e);
            }
        }
        
        return roleURL;
    }
    
    String getFailureURLForRole() {
        
        String roleFailureURL = null;
        try {
            Map roleAttrMap = getRoleServiceAttributes();
                roleFailureURL = getRoleURLFromAttribute(roleAttrMap,
                ISAuthConstants.LOGIN_FAILURE_URL);
        } catch (Exception e) {
            if (messageEnabled) {
                debug.message("Error retrieving url " );
                debug.message("Exception : " , e);
            }
        }
        return roleFailureURL;
    }
    
    /**
     * Returns the AMTemplate for role.
     * @return the AMTemplate for role.
     * @throws Exception if fails to get role attribute
     */
    Map getRoleServiceAttributes() throws Exception {
        try {
            if (roleAttributeMap == null) {
                if (AuthD.revisionNumber < 
                ISAuthConstants.AUTHSERVICE_REVISION7_0){
                    roleAttributeMap = amIdentityRole.getServiceAttributes(
                    ISAuthConstants.AUTHCONFIG_SERVICE_NAME);
                } else {
                    Map roleServiceAttrMap = amIdentityRole.
                    getServiceAttributes(
                    ISAuthConstants.AUTHCONFIG_SERVICE_NAME);
                    String serviceName =(String)((Set)roleServiceAttrMap.get(
                    AMAuthConfigUtils.ATTR_NAME)).iterator().next();
                    if ((serviceName != null) && 
                        (!serviceName.equals(ISAuthConstants.BLANK))) {
                        roleAuthConfig = serviceName;
                        roleAttributeMap = getServiceAttributes(serviceName);
                    }
                }
            }
            if (roleAttributeMap == null) {
                roleAttributeMap = Collections.EMPTY_MAP;
            }
            if (messageEnabled) {
                debug.message("Returning Service Attributes: " +
                roleAttributeMap);
                debug.message("for Role : " + amIdentityRole.getName());
            }
            
            return roleAttributeMap;
        } catch  (Exception e) {
            debug.error("Error getting Role Attributes : " , e);
            throw new Exception(AMAuthErrorCode.AUTH_ERROR);
        }
    }
    
    /**
     * Returns success url for a service.
     * @param indexName name of auth index
     * @return success url for a service.
     */
    String getSuccessURLForService(String indexName) {
        
        String successServiceURL = null;
        
        try {
            if ((serviceAttributesMap != null)
            && (serviceAttributesMap.isEmpty()) ){
                serviceAttributesMap= getServiceAttributes(indexName);
            }
            
            if (messageEnabled) {
                debug.message("AttributeMAP is.. :" + serviceAttributesMap);
            }
            
            successServiceURL= getServiceURLFromAttribute(
            serviceAttributesMap, ISAuthConstants.LOGIN_SUCCESS_URL);
            
            if (messageEnabled) {
                debug.message("service successURL : " + successServiceURL);
            }
        } catch (Exception e) {
            if (messageEnabled) {
                debug.message("Error retrieving url ");
                debug.message("Exception : ",e);
            }
        }
        
        return successServiceURL;
    }
    
    /**
     * Returns the service login failure URL.
     * @param indexName name of auth index
     * @return the service login failure URL.
     */
    String getFailureURLForService(String indexName) {
        String serviceFailureURL = null;
        try {
            if (serviceAttributesMap.isEmpty()) {
                serviceAttributesMap = getServiceAttributes(indexName);
            }
            serviceFailureURL = getServiceURLFromAttribute(
            serviceAttributesMap, ISAuthConstants.LOGIN_FAILURE_URL);
            
            if (messageEnabled) {
                debug.message("Service failureURL: " + serviceFailureURL);
            }
        } catch (Exception e) {
            if (messageEnabled) {
                debug.message("Error retrieving url ");
                debug.message("Exception : ",e);
            }
        }
        
        return serviceFailureURL;
    }
    
    /**
     * Returns service login url attributes.
     * @param indexName name of auth index
     * @return service login url attributes.
     * @throws Exception if fails to get service attribute
     */
    Map getServiceAttributes(String indexName) throws Exception {
        
        try {
            String orgDN = getOrgDN();
            Map attributeDataMap = null;
            attributeDataMap = AuthServiceListener.getServiceAttributeCache(
                orgDN, indexName);
            if (attributeDataMap != null) {
                return attributeDataMap;
            }
            attributeDataMap =
            AMAuthConfigUtils.getNamedConfig(indexName, orgDN, 
                ad.getSSOAuthSession());
            AuthServiceListener.setServiceAttributeCache( orgDN, indexName, 
                attributeDataMap);
            return attributeDataMap;
        } catch (Exception e) {
            if (messageEnabled) {
                debug.message("Error getting service attribute: ");
                debug.message(" Exception : " + e.getMessage());
            }
            throw new Exception(e.getMessage());
        }
    }
    
    /* create an instance of PostLoginProcessInterface Class */
    AMPostAuthProcessInterface getPostLoginProcessInstance(String className) {
        if (messageEnabled) {
            debug.message("postLoginProcess Class Name is : " + className);
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
            if (messageEnabled) {
                debug.message("Class not Found :" , ce);
            }
            return null;
        } catch (Exception e) {
            if (messageEnabled) {
                debug.message("Error: " , e);
            }
            return null;
        }
    }
    
    /**
     * Sets success login URL.
     *
     * @param url success login URL.
     */
    public void setSuccessLoginURL(String url) {
        /* this is for AMLoginModule to set the success url */
        if (messageEnabled) {
            debug.message("URL : from modle  : " + url);
        }
        moduleSuccessLoginURL = url;
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
    
    /**
     * this is called by AMLoginContext on a successful authentication to set
     * the <code>successURL</code> based on the <code>indexType</code>,
     * <code>indexName</code> order to figure out Success Login URL  is :
     * Authentication Method Success URL Order
     * <pre>
     *==========================================================================
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
    public void setSuccessLoginURL(
        AuthContext.IndexType indexType,
        String indexName) {
        /* if module sets  the url then return the URL module set */
        if (messageEnabled) {
            debug.message("moduleSucessLoginURL : " + moduleSuccessLoginURL);
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
                return ;
            }
        }
        
        defSuccessURL = defaultUserSuccessURL;
        if (indexType == AuthContext.IndexType.ROLE) {
            String successURL = getSuccessURLForRole();
            if ((successURL != null) && (successURL.length() != 0)) {
                successLoginURL = successURL;
                return ;
            }
            if (defSuccessURL == null || defSuccessURL.length() == 0) {
                defSuccessURL = tempDefaultURL;
            }
        }
        
        if (indexType == AuthContext.IndexType.SERVICE) {
            String successURL = getSuccessURLForService(indexName);
            if ((successURL != null) && (successURL.length() != 0)) {
                successLoginURL = successURL;
                return ;
            }
            if (defSuccessURL == null || defSuccessURL.length() == 0) {
                defSuccessURL = tempDefaultURL;
            }
        }
        
        if ((clientSuccessRoleURL != null) &&
            (clientSuccessRoleURL.length() != 0)
        ) {
            successLoginURL = clientSuccessRoleURL;
            return ;
        }

        if (defSuccessURL == null || defSuccessURL.length() == 0) {
            defSuccessURL = defaultSuccessRoleURL;
        }
        
        if ((clientOrgSuccessLoginURL!=null) &&
            (clientOrgSuccessLoginURL.length() != 0)
        ) {
            successLoginURL = clientOrgSuccessLoginURL;
            return ;
        }
        
        if (defSuccessURL == null || defSuccessURL.length() == 0) {
            defSuccessURL = defaultOrgSuccessLoginURL;
        }
        
        // get global default
        if (indexType == AuthContext.IndexType.SERVICE ||
        indexType == AuthContext.IndexType.ROLE) {
            defaultSuccessURL = getRedirectUrl(ad.defaultServiceSuccessURLSet);
        } else {
            defaultSuccessURL = getRedirectUrl(ad.defaultSuccessURLSet);
            ad.defaultSuccessURL = tempDefaultURL;
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
        if(messageEnabled) {
            debug.message("SUCCESS Login url : " + successLoginURL);
        }
        
        return;
    }
    
    /**
     * Sets failure login URL.
     *
     * @param indexType
     * @param indexName
     */
    public void setFailureLoginURL(
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
        
        
        /*
         * if gotoOnFail parameter was specified  then return the gotoOnFailURL
         */
        if ( (gotoOnFailURL != null) && (gotoOnFailURL.length() != 0) ) {
            failureLoginURL = gotoOnFailURL;
            return;
        }
        
        if (messageEnabled) {
            debug.message("failureTokenId in setFailureLoginURL = "
            + failureTokenId);
        }
        
        String defFailureURL = null;
        /* if user profile has failure url set then return */
        
        if (failureTokenId != null) {
            // get the user profile
            try {
                getUserProfile(failureTokenId,true,false);
                if ((clientUserFailureURL != null) && 
                    (clientUserFailureURL.length() != 0)) {
                    failureLoginURL = clientUserFailureURL;
                    return ;
                }
                defFailureURL = defaultUserFailureURL;
            } catch (Exception e){
                if (messageEnabled) {
                    debug.message("Error retreiving profile for : " +
                    failureTokenId, e);
                }
            }
        }
        
        if (indexType == AuthContext.IndexType.ROLE) {
            String failureURL = getFailureURLForRole();
            if ((failureURL != null) && (failureURL.length() != 0)) {
                failureLoginURL = failureURL;
                return ;
            }
            
            if ((defFailureURL == null) || (defFailureURL.length() == 0))  {
                defFailureURL = tempDefaultURL;
            }
        }
        
        if (indexType == AuthContext.IndexType.SERVICE) {
            String failureURL = getFailureURLForService(indexName);
            if ((failureURL != null) && (failureURL.length() != 0)) {
                failureLoginURL = failureURL;
                return ;
            }
            if ((defFailureURL ==  null) || (defFailureURL.length() == 0)) {
                defFailureURL = tempDefaultURL;
            }
        }
        
        if ((clientFailureRoleURL != null) &&
            (clientFailureRoleURL.length() != 0)
        ) {
            failureLoginURL = clientFailureRoleURL;
            return ;
        }
        
        if ((defFailureURL == null) || (defFailureURL.length() == 0)) {
            defFailureURL = defaultFailureRoleURL;
        }
        
        if ((clientOrgFailureLoginURL!=null) &&
            (clientOrgFailureLoginURL.length() != 0)
        ) {
            failureLoginURL = clientOrgFailureLoginURL;
            return ;
        }
        if ((defFailureURL == null) || (defFailureURL.length() == 0)) {
            defFailureURL = defaultOrgFailureLoginURL;
        }
        
        if (indexType == AuthContext.IndexType.SERVICE ||
        indexType == AuthContext.IndexType.ROLE) {
            defaultFailureURL = getRedirectUrl(ad.defaultServiceFailureURLSet);
        } else {
            defaultFailureURL = getRedirectUrl(ad.defaultFailureURLSet);
            ad.defaultFailureURL = tempDefaultURL;
        }
        if ((defaultFailureURL != null) && (defaultFailureURL.length() != 0)) {
            failureLoginURL = defaultFailureURL;
            return;
        }
        
        if ((defFailureURL== null) || (defFailureURL.length() == 0)) {
            defFailureURL = tempDefaultURL;
        }
        // now assign back to the Failure url
        failureLoginURL = defFailureURL;
        if (messageEnabled) {
            debug.message("defaultFailureURL : " + failureLoginURL);
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
        /* this method for UI called from AuthUtils */
        if ((fqdnFailureLoginURL == null) || (fqdnFailureLoginURL.length() == 0)
        ) {
            fqdnFailureLoginURL = ad.processURL(failureLoginURL,servletRequest);
        }
        return fqdnFailureLoginURL;
    }

    public String getLogoutURL() {

        String postProcessURL = AuthUtils.getPostProcessURL(servletRequest,
        AMPostAuthProcessInterface.POST_PROCESS_LOGOUT_URL);

        return postProcessURL;
    }

    /**
     * Returns the role login url attribute value.
     * @param roleAttrMap map object has login url attribute
     * @param attrName attribute name for login url
     * @return the role login url attribute value.
     */
    String getRoleURLFromAttribute(Map roleAttrMap,String attrName) {
        try{
            Set roleURLSet = (Set) roleAttrMap.get(attrName);
            return getRedirectUrl(roleURLSet);
        } catch (Exception e) {
            if (messageEnabled) {
                debug.message("Error getting role attribute " ,e );
            }
            return null;
        }
    }
    
    /**
     * Returns service url from attribute value.
     * @param attributeMap map object has service url attribute
     * @param attrName attribute name for service url
     * @return service url from attribute value.
     */
    String getServiceURLFromAttribute(Map attributeMap,String attrName) {
        Set serviceURLSet =
        (Set) attributeMap.get(attrName);
        
        String serviceURL = getRedirectUrl(serviceURLSet);
        if (messageEnabled) {
            debug.message("attr map: " + attributeMap +
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
     * @param servletResponse servletResponse object to be set
     */
    public void setHttpServletResponse(HttpServletResponse servletResponse) {
        this.servletResponse = servletResponse;
    }
    
    /**
     * Sets persistent cookie to true - called by <code>AMLoginModule</code>.
     */
    public synchronized void setPersistentCookieOn() {
        persistentCookieOn = true;
    }

    /**
     * Return previously received callback
     * @return previously received callback
     */
    public Callback[] getRecdCallback() {
        return prevCallback;
    }
    
    /**
     * Set previously received callback
     * @param prevCallback previously received callback
     */
    public synchronized void setPrevCallback(Callback[] prevCallback) {
        this.prevCallback = prevCallback;
    }
    
    protected String getAccountLife() {
        return accountLife;
    }
    
    protected String getUserToken() {
        return token;
    }
   
    protected boolean getEnableModuleBasedAuth() {
        return enableModuleBasedAuth;
    }
 
    public boolean getLoginFailureLockoutMode() {
        return loginFailureLockoutMode;
    }
    public boolean getLoginFailureLockoutStoreInDS() {
        return loginFailureLockoutStoreInDS;
    }
    
    public long getLoginFailureLockoutTime() {
        return loginFailureLockoutTime;
    }
    
    public int getLoginFailureLockoutCount() {
        return loginFailureLockoutCount;
    }
    
    public String getLoginLockoutNotification() {
        return loginLockoutNotification;
    }

    public void incrementFailCount(String failedUserId) {
        if (failedUserId != null) {
            AMAccountLockout amAccountLockout = new AMAccountLockout(this);
            boolean accountLocked = amAccountLockout.isLockedOut(failedUserId);

            if ((!accountLocked) && (amAccountLockout.isLockoutEnabled())) {
                amAccountLockout.invalidPasswd(failedUserId);

                if (debug.messageEnabled()) {
                    debug.message("LoginState::incrementFailCount incremented fail count for " + failedUserId);
                }
            }
        } else {
            debug.error("LoginState::incrementFailCount called with null user id");
        }
    }

    public boolean isAccountLocked(String username) {
        AMAccountLockout amAccountLockout = new AMAccountLockout(this);
        return amAccountLockout.isLockedOut(username);
    }
    
    /**
     * Returns lockout warning message.
     * @return lockout warning message.
     */
    public int getLoginLockoutUserWarning() {
        return loginLockoutUserWarning;
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
     * Returns the error code .
     *
     * @return the error code .
     */
    public String getErrorCode() {
        return errorCode;
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
     * Returns the error message.
     *
     * @return the error message.
     */
    public String getErrorMessage() {
        return errorMessage;
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
     * Returns the error template generated by framework.
     *
     * @return the error template generated by framework.
     */
    public String getErrorTemplate() {
        return errorTemplate;
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
     * Returns the error template set by module.
     *
     * @return Error template set by module.
     */
    public String getModuleErrorTemplate() {
        return moduleErrorTemplate;
    }
    
    /**
     * Sets the time out value.
     *
     * @param timedOut <code>true</code> to set timed out.
     */
    public void setTimedOut(boolean timedOut) {
        this.timedOut = timedOut;
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
     * Sets the lockout message.
     *
     * @param lockoutMsg the lockout message.
     */
    public void setLockoutMsg(String lockoutMsg) {
        if (messageEnabled) {
            debug.message("setLockoutMsg :" + lockoutMsg);
        }
        this.lockoutMsg = lockoutMsg;
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
     * Set index name
     * @param indexName indexName to be set
     */
    public void setIndexName(String indexName) {
        this.indexName = indexName;
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
     * Creates <code>AuthContextLocal</code> for new requests.
     *
     * @param sid
     * @param orgName
     * @param req
     * @return the created <code>AuthContextLocal</code>
     * @throws AuthException if fails to create <code>AuthContextLocal</code>
     */
    public AuthContextLocal createAuthContext(
        SessionID sid,
        String orgName,
        HttpServletRequest req
    ) throws AuthException {
        this.userOrg = getDomainNameByOrg(orgName);
        
        if (messageEnabled) {
            debug.message("createAuthContext: userOrg is : " + userOrg);
        }
        
        if ((this.userOrg == null) || (this.userOrg.equals(""))) {
            debug.error("domain is null, error condtion");
            logFailed(ad.bundle.getString("invalidDomain"),"INVALIDDOMAIN");
            throw new AuthException(AMAuthErrorCode.AUTH_INVALID_DOMAIN, null);
        }
        
        if (messageEnabled) {
            debug.message("AuthUtil::getAuthContext::Creating new " + 
                "AuthContextLocal & LoginState");
        }
        AuthContextLocal authContext = new AuthContextLocal(this.userOrg);
        requestType = true;
        this.sid = sid;
        if (messageEnabled) {
            debug.message("requestType : " + requestType);
            debug.message("sid : " + sid);
            debug.message("orgName passed: " + orgName);
        }
        
        try {
            createSession(req,authContext);
        } catch (Exception e) {
            debug.error("Exception creating session .. :", e);
            throw new AuthException(AMAuthErrorCode.AUTH_ERROR, null);
        }
        amIdRepo = ad.getAMIdentityRepository(getOrgDN());
        populateOrgProfile();
        isLocaleSet=false;
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
            levelSet=true;
        }
        if (messageEnabled) {
            debug.message("spi authLevel :" + authLevel);
            debug.message(
                "module configuration authLevel :" + this.authLevel);
            debug.message("levelSet :" + levelSet);
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
     *        <code>orgDN</code> is to be determined.
     * @return a String which is the orgDN of the request
     */
    public String getDomainNameByOrg(String orgName) {
        String orgDN = null;
        try {
            orgDN = AuthUtils.getOrganizationDN(orgName,false,null);
            
        } catch (Exception e) {
            if (messageEnabled) {
                debug.message("Incorrect orgName passed:" + orgName,e);
            }
        }
        return orgDN ;
    }
    
    /**
     * Returns the module instances for a organization.
     *
     * @return the module instances for a organization.
     */
    public Set getModuleInstances() {
        try {
            
            if ((moduleInstances !=null) && (!moduleInstances.isEmpty())) {
                return moduleInstances;
            }
            
            moduleInstances = domainAuthenticators;
            
            if (messageEnabled) {
                debug.message("moduleInstances are : " + moduleInstances);
            }
        } catch (Exception e) {
            if (messageEnabled) {
                debug.message("Error getting moduleInstances " , e);
            }
        }
        
        if (moduleInstances == null) {
            moduleInstances = Collections.EMPTY_SET;
        }
        
        return moduleInstances;
    }
    
    /**
     * Returns the allowed authentication modules for organization
     *
     * @return the allowed authentication modules for organization
     */
    public Set getDomainAuthenticators() {
        return domainAuthenticators;
    }
    
    /**
     * Sets the x509 certificate.
     *
     * @param cert x509 certificate.
     */
    public void setX509Certificate(X509Certificate cert) {
        this.cert = cert;
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
           if ((allCerts != null) && (allCerts.length != 0) ) {
               if (debug.messageEnabled()) {
                   debug.message("LoginState.getX509Certificate :" +
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
    public void logSuccess() {
        try {
            String logSuccess = ad.bundle.getString("loginSuccess");
            ArrayList dataList = new ArrayList();
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
            String[] data = (String[])dataList.toArray(new String[0]);
            String contextId = null;
            SSOToken localSSOToken = getSSOToken();
            if (localSSOToken != null) {
                contextId = localSSOToken.getProperty(Constants.AM_CTX_ID);
            }
            
            Hashtable props = new Hashtable();
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
            if (session != null) {
                props.put(LogConstants.LOGIN_ID_SID, sid.toString());
            }
            if (contextId != null) {
                props.put(LogConstants.CONTEXT_ID, contextId);
            }
            
            ad.logIt(data,ad.LOG_ACCESS,messageId.toString(), props);
        } catch (Exception e) {
            debug.message("Error creating logSuccess message",e);
        }
        
    }

    /**
     * Adds log message to authentication access log.
     * @param msgId I18n key of the localized message.
     * @param logId Logging message Id
     */
    public void logSuccess(String msgId, String logId) {
        try {
            String logSuccess = ad.bundle.getString(msgId);
            ArrayList dataList = new ArrayList();
            dataList.add(logSuccess);

            String[] data = (String[])dataList.toArray(new String[0]);
            
            Hashtable props = new Hashtable();
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
            if (session != null) {
                props.put(LogConstants.LOGIN_ID_SID, sid.toString());
            }

            ad.logIt(data, ad.LOG_ACCESS, logId, props);
        } catch (Exception e) {
            debug.message("Error creating logSuccess message", e);
        }
    }

    /**
     * Log login failed
     * @param str message for login failed
     */
    public void logFailed(String str) {
        logFailed(str, "LOGIN_FAILED", true, null);
    }

    /**
     * Log login failed
     * @param str message for login failed
     * @param error error message for login failed
     */
    public void logFailed(String str,String error) {
        logFailed(str, "LOGIN_FAILED", true, error);    
    }

    
    /**
     * Adds log message to authentication error log.
     * @param str localized message to be logged.
     * @param logId logging message Id.
     * @param appendAuthType if true, append authentication type to the logId
     *          to form new logging message Id. for example:
     *          "LOGIN_FAILED_LEVEL".
     * @param error error Id to be append to logId to form new logging
     *          message Id. for example : "LOGIN_FAILED_LEVEL_INVALIDPASSWORD"
     */
    public void logFailed(String str, String logId, boolean appendAuthType,
        String error) {
        
        try {
            String logFailed = str;
            if (str == null) {
                logFailed = ad.bundle.getString("loginFailed");
            }
            ArrayList dataList = new ArrayList();
            dataList.add(logFailed);
            StringBuilder messageId = new StringBuilder();
            messageId.append(logId);
            if ((indexType != null) &&
            (indexType != AuthContext.IndexType.COMPOSITE_ADVICE)){
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
            String[] data = (String[])dataList.toArray(new String[0]);
            String contextId = null;
            try {
                SSOToken localSSOToken = getSSOToken();
                if (localSSOToken != null) {
                    contextId = localSSOToken.getProperty(Constants.AM_CTX_ID);
                }
            } catch (SSOException ssoe) {
                debug.message("Error while retrieving SSOToken for login failure: "
                        + ssoe.getMessage());
            }
            
            Hashtable props = new Hashtable();
            if (client != null) {
                props.put(LogConstants.IP_ADDR, client);
            }
            if (userDN != null) {
                props.put(LogConstants.LOGIN_ID, userDN);
            } else if (failureTokenId != null) {
                props.put(LogConstants.LOGIN_ID, failureTokenId);
            } else if (callbacksPerState != null && callbacksPerState.values() != null
            		&& callbacksPerState.values().size() > 0) {
                Object[] ob = callbacksPerState.values().toArray();
                for (int i = 0; i < ob.length; i++) {
                    if (ob[i] instanceof Callback[]) {
                        Callback[] cb = (Callback[]) ob[i];
                        for (int j = 0; j < cb.length; j++) {
                            if (cb[j] instanceof NameCallback) {
                                userDN = ((NameCallback) cb[j]).getName();
                                if (ad.debug.messageEnabled()) {
                                    ad.debug.message("userDN is null, setting to " + userDN);
                                }
                                props.put(LogConstants.LOGIN_ID, userDN);
                            }
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
            if (session != null) {
                props.put(LogConstants.LOGIN_ID_SID, sid.toString());
            }
            if (contextId != null) {
                props.put(LogConstants.CONTEXT_ID, contextId);
            }

            ad.logIt(data,ad.LOG_ERROR,messageId.toString(), props);
        } catch (Exception e) {
            debug.error("Error creating logFailed message" ,e );
        }
    }
    
    /**
     * Log Logout status 
     */
    public void logLogout(){
        try {
            String logLogout = ad.bundle.getString("logout");
            ArrayList dataList = new ArrayList();
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
            String[] data = (String[])dataList.toArray(new String[0]);
            String contextId = null;
            SSOToken localSSOToken = getSSOToken();
            if (localSSOToken != null) {
                contextId = localSSOToken.getProperty(Constants.AM_CTX_ID);
            }
            
            Hashtable props = new Hashtable();
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
            if (session != null) {
                props.put(LogConstants.LOGIN_ID_SID, sid.toString());
            }
            if (contextId != null) {
                props.put(LogConstants.CONTEXT_ID, contextId);
            }
            ad.logIt(data,ad.LOG_ACCESS, messageId.toString(), props);
        } catch (Exception e) {
            debug.error("Error creating logout message" , e);
        }
    }
    
    /**
     * Return attribute name for LoginLockout
     * @return attribute name for LoginLockout
     */
    public String getLoginLockoutAttrName()  {
        return loginLockoutAttrName;
    }
    
    /**
     * Return attribute value for LoginLockout
     * @return attribute value for LoginLockout
     */
    public String getLoginLockoutAttrValue() {
        return loginLockoutAttrValue;
    }

    /**
     * Return attribute name for storing invalid attempts data
     * @return attribute name for storing invalid attempts data
     */
    public String getInvalidAttemptsDataAttrName()  {
        return invalidAttemptsDataAttrName;
    }

    /**
     * Return LoginLockout duration
     * @return LoginLockout duration
     */
    public long getLoginFailureLockoutDuration() {
        return loginFailureLockoutDuration;
    }

    /**
     * Return multiplier for Memory Lockout
     * @return LoginLockout multiplier
     */
    public int getLoginFailureLockoutMultiplier() {
        return loginFailureLockoutMultiplier;
    }

    /**
     * Sets old Session
     *
     * @param oldSession Old InternalSession Object
     */
    public void setOldSession(InternalSession oldSession) {
        this.oldSession = oldSession;
    }
    
    /**
     * Returns old Session
     *
     * @return old Session
     */
    public InternalSession getOldSession() {
        return oldSession;
    }
    
    /**
     * Sets session upgrade.
     *
     * @param sessionUpgrade <code>true</code> if session upgrade.
     */
    public void setSessionUpgrade(boolean sessionUpgrade) {
        this.sessionUpgrade = sessionUpgrade;
    }
    
    /**
     * Returns session upgrade.
     *
     * @return session upgrade.
     */
    public boolean isSessionUpgrade() {
        return sessionUpgrade;
    }
    
    /* upgrade session properties - old session and new session proeprties
     * will be concatenated , seperated by |
     */
    
    void sessionUpgrade() {
        // set the larger authlevel
        if (oldSession == null) {
            return;
        }
        
        upgradeAllProperties(oldSession);
        
        int prevAuthLevel = 0;
        String upgradeAuthLevel = null;
        String strPrevAuthLevel = AMAuthUtils.getDataFromRealmQualifiedData(
            (String)oldSession.getProperty("AuthLevel"));
        try {
            prevAuthLevel = Integer.parseInt(strPrevAuthLevel);
        } catch (NumberFormatException e) {
            debug.error("AuthLevel from session property bad format");
        }
        
        if (messageEnabled) {
            debug.message("prevAuthLevel : " + prevAuthLevel);
        }
        
        if (prevAuthLevel > authLevel) {
            upgradeAuthLevel = new Integer(prevAuthLevel).toString();
        } else {
            upgradeAuthLevel = new Integer(authLevel).toString();
        }
        
        if ((qualifiedOrgDN != null) && (qualifiedOrgDN.length() != 0)) {                
            upgradeAuthLevel = AMAuthUtils.toRealmQualifiedAuthnData(
                    DNMapper.orgNameToRealmName(qualifiedOrgDN), 
                        upgradeAuthLevel);                
        }
        
        // update service name if indextype is service
        String prevServiceName = oldSession.getProperty("Service");
        String upgradeServiceName = prevServiceName;
        String newServiceName = null;
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
                (prevServiceName.indexOf(newServiceName) == -1)) {
                    upgradeServiceName = newServiceName + "|" + 
                        prevServiceName;
                }
            } else {
                upgradeServiceName = newServiceName ;
            }
        }
        
        
        // update role if indexType is role
        
        String prevRoleName = oldSession.getProperty("Role");
        String upgradeRoleName = prevRoleName;
        if (indexType == AuthContext.IndexType.ROLE) {
            if (prevRoleName != null) {
                upgradeRoleName = prevRoleName;
                if ((indexName != null)
                && (prevRoleName.indexOf(indexName) == -1)) {
                    upgradeRoleName = indexName + "|" + prevRoleName;
                }
            } else {
                upgradeRoleName = indexName;
            }
        }
        
        // update auth meth name
        
        String prevModuleList = oldSession.getProperty("AuthType");
        String newModuleList =  authMethName;
        if ((qualifiedOrgDN != null) && (qualifiedOrgDN.length() != 0)) {
            newModuleList = getRealmQualifiedModulesList(
                DNMapper.orgNameToRealmName(qualifiedOrgDN), authMethName);            
        }
        if (messageEnabled) {
            debug.message("newModuleList : " + newModuleList);
            debug.message("prevModuleList : " + prevModuleList);
        }
        String upgradeModuleList = null;
        StringBuilder sb = new StringBuilder();
        sb.append(newModuleList);
        if (prevModuleList == null ? newModuleList != null : !prevModuleList.equals(newModuleList)) {
            upgradeModuleList = parsePropertyList(prevModuleList,newModuleList);
        } else {
            upgradeModuleList = sb.toString();
        }
        
        if (messageEnabled) {
            debug.message("oldAuthLevel : " + prevAuthLevel);
            debug.message("newAuthLevel : " + authLevel);
            debug.message("upgradeAuthLevel : " + upgradeAuthLevel);
            debug.message("prevServiceName : " + prevServiceName);            
            debug.message("upgradeServiceName : " + upgradeServiceName);
            debug.message("preRoleName : " + prevRoleName);
            debug.message("upgradeRoleName : " + upgradeRoleName);
            debug.message("prevModuleList: " + prevModuleList);
            debug.message("newModuleList: " + newModuleList);
            debug.message("upgradeModuleList: " + upgradeModuleList);
        }
        
        
        updateSessionProperty("AuthLevel",upgradeAuthLevel);
        updateSessionProperty("AuthType",upgradeModuleList);
        updateSessionProperty("Service",upgradeServiceName);
        updateSessionProperty("Role",upgradeRoleName);
        session.setIsSessionUpgrade(true);
    }
    
    /* update session with the property and value */
    
    void updateSessionProperty(String property,String value) {
        if (value == null) {
            return;
        }
        if (!forceAuth) {
            session.putProperty(property,value);
        } else {
            oldSession.putProperty(property,value);
        }
    }

    /* Get realm qualified modules list */
    String getRealmQualifiedModulesList(String realm,String oldModulesList) {
        if (messageEnabled) {
            debug.message("getRealmQualifiedModulesList:realm : " 
                + realm);
            debug.message("getRealmQualifiedModulesList:oldModulesList : " 
                + oldModulesList);
        }
        
        StringBuilder sb = new StringBuilder();
        StringTokenizer st = new StringTokenizer(oldModulesList,"|");
        while (st.hasMoreTokens()) {
            String module = (String)st.nextToken();
            sb.append(AMAuthUtils.toRealmQualifiedAuthnData(realm,module))
            .append("|");           
        }
        
        String realmQualifiedModulesList = sb.toString();
        int i = realmQualifiedModulesList.lastIndexOf("|");
        if (i != -1) {
            realmQualifiedModulesList = 
                realmQualifiedModulesList.substring(0,i);
        }
        
        if (messageEnabled) {
            debug.message("RealmQualifiedModulesList is : " 
                + realmQualifiedModulesList);
        }
        return realmQualifiedModulesList;
    }
    
    /* compare old session property and new session property */
    String parsePropertyList(String oldProperty,String newProperty) {
        if (messageEnabled) {
            debug.message("oldProperty : " + oldProperty);
            debug.message("newProperty : " + newProperty);
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append(newProperty);
        StringTokenizer st = new StringTokenizer(oldProperty,"|");
        while (st.hasMoreTokens()) {
            String s = (String)st.nextToken();
            if (!newProperty.equals(s)) {
                sb.append("|").append(s);
            }
        }
        
        String propertyList = sb.toString();
        
        if (messageEnabled) {
            debug.message("propertyList is : " + propertyList);
        }
        return propertyList;
    }
    
    // Upgrade all Properties from the existing (old) session to new session
    void upgradeAllProperties(InternalSession oldSession) {
        if (debug.messageEnabled()) {
            debug.message("LoginState::upgradeAllProperties() : Calling SessionPropertyUpgrader");
        }
        propertyUpgrader.populateProperties(oldSession, session, forceAuth);
    }
    
    void setCookieSet(boolean flag) {
        cookieSet = flag;
    }
    
    boolean isCookieSet() {
        return cookieSet;
    }
    
    void setCookieSupported(boolean flag) {
        cookieSupported = flag;
    }
    
    boolean isCookieSupported() {
        return cookieSupported;
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
    void postProcess(AuthContext.IndexType indexType,String indexName, 
            int type) {
        setPostLoginInstances(indexType,indexName);
        if ((postProcessInSession) && ((postLoginInstanceSet != null) &&
            (!postLoginInstanceSet.isEmpty()))) {
            if (messageEnabled) {
                debug.message("LoginState.setPostLoginInstances : " 
                    + "Setting post process class in session "
                    + postLoginInstanceSet);
            }
            session.setObject(ISAuthConstants.POSTPROCESS_INSTANCE_SET,
                postLoginInstanceSet);
        }
        AMPostAuthProcessInterface postLoginInstance=null;
        if ((postLoginInstanceSet != null) && 
            (!postLoginInstanceSet.isEmpty())) {
            for(Iterator iter = postLoginInstanceSet.iterator(); 
            iter.hasNext();) {
                postLoginInstance = 
                (AMPostAuthProcessInterface) iter.next();
                executePostProcessSPI(postLoginInstance,type);
            }
        }
    }
    
    
    /**
     * Returns an instance of the spi and execute it based on whether
     * the login status is success or failed
     * @param postProcessInstance <code>AMPostAuthProcessInterface</code>
     * object to be processes in post login 
     * @param type indicates success, failure or logout
     */
    void executePostProcessSPI(AMPostAuthProcessInterface postProcessInstance, 
        int type) {
        /* Reset Post Process URLs in servletRequest so
        * that plugin can set new values (just a safety measure) */
        AuthUtils.resetPostProcessURLs(servletRequest);

        if (requestMap.isEmpty() && (servletRequest != null)) {
            Map map = servletRequest.getParameterMap();
            for (Iterator i = map.entrySet().iterator(); i.hasNext();){
                Map.Entry e = (Map.Entry)i.next();
                requestMap.put(e.getKey(),((Object[])e.getValue())[0]);
            }
        }
        /* execute the post process spi */
        try{
            switch (type) { 
            case POSTPROCESS_SUCCESS:
                postProcessInstance.onLoginSuccess(requestMap,servletRequest
                ,servletResponse,getSSOToken());
                break;
            case POSTPROCESS_FAILURE:
                postProcessInstance.onLoginFailure(requestMap,servletRequest,
                servletResponse);
                break;
            case POSTPROCESS_LOGOUT:
                postProcessInstance.onLogout(servletRequest,servletResponse,
                        getSSOToken());
                break;
            default:
                if (messageEnabled) {
                    ad.debug.message("executePostProcessSPI: " +
                        "invalid input type: "+type);
                }
            }
        } catch (AuthenticationException ae) {
            if (messageEnabled){
                ad.debug.message("Error " , ae);
            }
        } catch (Exception e) {
            if (messageEnabled){
                ad.debug.message("Error " , e);
            }
        }
    }    
    
    
    /**
     * Creates a set of instances that are implementation of classes of type 
     * AMPostAuthProcessInterface. The classes are picked based on index type 
     * and auth configuration.
     * @param indexType Index type for post login process
     * @param indexName Index name for post login process
     */
    void setPostLoginInstances(
        AuthContext.IndexType indexType,
        String indexName) {
        AMPostAuthProcessInterface postProcessInstance= null;
        String postLoginClassName = null;
        Set postLoginClassSet   = Collections.EMPTY_SET;
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
                if (AuthD.revisionNumber >= ISAuthConstants.
                AUTHSERVICE_REVISION7_0) {
                    if ((orgAdminAuthConfig != null) && 
                    (!orgAdminAuthConfig.equals(ISAuthConstants.BLANK))) {
                        postLoginClassSet = getServicePostLoginClassSet
                        (orgAdminAuthConfig);
                    }
                }
            } else {
                postLoginClassSet = getServicePostLoginClassSet(indexName);
            }
        } else if ((indexType == AuthContext.IndexType.USER) && 
          (AuthD.revisionNumber >= ISAuthConstants.AUTHSERVICE_REVISION7_0)) {

        /* For user based auth, take the auth config from users attributes
         */

            if (((userAuthConfig != null) && (!userAuthConfig.equals(
            ISAuthConstants.BLANK)))){
                 postLoginClassSet = getServicePostLoginClassSet(
                 userAuthConfig);
            }
        }

        if (((postLoginClassSet == null) || (postLoginClassSet.isEmpty())) && 
        ((orgPostLoginClassSet != null) && (!orgPostLoginClassSet.isEmpty()))) {

        /* If no Post Process class is found or module based auth then 
         * default to org level  only if they are defined.
         */
            postLoginClassSet = orgPostLoginClassSet;
        } else if ((AuthD.revisionNumber >= ISAuthConstants.
          AUTHSERVICE_REVISION7_0) && (indexType == null)) {

          /* For org based auth, if post process classes are not defined at
           * org level then use or default config.
           */

            if ((orgAuthConfig != null) && (!orgAuthConfig.
            equals(ISAuthConstants.BLANK)))  {
                postLoginClassSet = getServicePostLoginClassSet(orgAuthConfig);
            }
        }

        if (messageEnabled){
              debug.message("postLoginClassSet = "+postLoginClassSet);
        }

        if ((postLoginClassSet != null) && (!postLoginClassSet.isEmpty())) {
            postLoginInstanceSet = new HashSet();
            StringBuilder sb = new StringBuilder();
            for(Iterator iter = postLoginClassSet.iterator(); iter.hasNext();) {
                postLoginClassName = (String)iter.next();
                if (sb.length() > 0) {
                    sb.append("|");
                }
                if (messageEnabled) {
                    debug.message("setPostLoginInstances : " 
                    + postLoginClassName);
                    debug.message("setPostLoginInstances : " 
                    + postLoginClassSet.size());
                }
                postProcessInstance
                = getPostLoginProcessInstance(postLoginClassName);
                if (postProcessInstance != null) {
                    postLoginInstanceSet.add(postProcessInstance);
                    sb.append(postLoginClassName);
                }
            }
            session.putProperty(ISAuthConstants.POST_AUTH_PROCESS_INSTANCE,
                    sb.toString()); 
        }
    }
    
    /**
     * Returns role post login class set 
     * @return role post login class set 
     */
    Set getRolePostLoginClassSet() {
        
        Set postLoginClassSet = null;
        try {
            Map roleAttrMap = getRoleServiceAttributes();
                postLoginClassSet = (Set) roleAttrMap.get(
                ISAuthConstants.POST_LOGIN_PROCESS);
            if (postLoginClassSet == null) {
             postLoginClassSet = Collections.EMPTY_SET;
            }
            
            if (messageEnabled) {
                debug.message("Role Post Login Class Set : " + 
                postLoginClassSet);
            }
            
        } catch (Exception e) {
            if (messageEnabled) {
                debug.message("Error get role class set " , e);
            }
        }
        return postLoginClassSet;
    }
    
    /**
     * Returns the service post login process class set 
     * @param indexName Index name for post login
     * @return the service post login process class set 
     */
    Set getServicePostLoginClassSet(String indexName) {
        
        Set postLoginClassSet = null;
        try {
            if ((serviceAttributesMap != null)
            && (serviceAttributesMap.isEmpty())) {
                serviceAttributesMap = getServiceAttributes(indexName);
            }
            
            if (messageEnabled) {
                debug.message("Service Attributes are . :" + 
                serviceAttributesMap);
            }
            
            postLoginClassSet =
            (Set) serviceAttributesMap.get(ISAuthConstants.POST_LOGIN_PROCESS);
            if (postLoginClassSet == null) {
             postLoginClassSet = Collections.EMPTY_SET;
            }
            
            if (messageEnabled) {
                debug.message("postLoginClassName: " + postLoginClassSet);
            }
            
        } catch (Exception e) {
            if (messageEnabled){
                debug.message("Error get service post login class name " 
                + e.getMessage());
            }
        }
        return postLoginClassSet;
    }
    
    void setModuleErrorMessage(String message) {
        moduleErrorMessage = message;
    }
    
    /**
     * Returns the error message set by module.
     *
     * @return the error message set by module.
     */
    public String getModuleErrorMessage() {
        return moduleErrorMessage;
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
    public boolean isForwardSuccess() {
        return forwardSuccess;
    }
    
    /**
     * Sets the page timeout.
     *
     * @param pageTimeOut Page timeout.
     */
    public synchronized void setPageTimeOut(long pageTimeOut) {
        if (messageEnabled) {
            debug.message("Setting page timeout :" + pageTimeOut);
        }
        this.pageTimeOut = pageTimeOut;
    }
    
    /**
     * Returns page timeout.
     *
     * @return Page timeout.
     */
    public long getPageTimeOut() {
        if (messageEnabled) {
            debug.message("Returning page timeout :" + pageTimeOut);
        }
        return pageTimeOut;
    }
    
    /**
     * Sets the last callback sent.
     *
     * @param lastCallbackSent Last callback sent.
     */
    public void setLastCallbackSent(long lastCallbackSent) {
        if (messageEnabled) {
            debug.message("setting Last Callback Sent :" + lastCallbackSent);
        }
        this.lastCallbackSent = lastCallbackSent;
    }
    
    /**
     * Returns last callback sent.
     *
     * @return Last callback sent.
     */
    public long getLastCallbackSent() {
        if (messageEnabled) {
            debug.message(
                "Returning Last Callback Sent :" + lastCallbackSent);
        }
        return lastCallbackSent;
    }
    
    /**
     * This function is to get the redirect url from a set of urls
     * based on client type. Each url will be of the form
     * clienttype|url, applications need to provide redirect urls
     * along with the client type so that it can be client aware.
     * If it does not find the specified url along with
     * client type, it returns the default client type
     * @param urls set of urls with client type. 
     * @return redirect url from a set of urls for client type
     */
    private String getRedirectUrl(Set urls) {
        
        //If the urls set is null, return the default url
        String clientURL = null;
        tempDefaultURL = null;
        if ((urls != null) && (!urls.isEmpty())) {
            String defaultURL = null;
            Iterator iter = urls.iterator();
            while (iter.hasNext()) {
                String url = (String)iter.next();
                if (messageEnabled) {
                    debug.message("URL is : " + url);
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
            if (messageEnabled) {
                debug.message("defaultURL : " + defaultURL);
                debug.message("tempDefaultURL : " + tempDefaultURL);
            }
        }
        
        return clientURL;
    }
    
    /**
     * Return ignoreUserProfile
     * @return ignoreUserProfile
     */
    public boolean ignoreProfile() {
        return ignoreUserProfile;
    }

    boolean containsToken(StringBuffer principalBuffer, String token) {
        String principalString = principalBuffer.toString();
        if (debug.messageEnabled()) {
            debug.message("principalString : " + principalString);
        }
        if (principalString == null) {
            return false;
        }

               try {
            StringTokenizer st = new StringTokenizer(principalString,"|");
            while (st.hasMoreTokens()) {
                String s = (String)st.nextToken();
                if (s.equals(token)) {
                    return true;
                    }
            }
        } catch (Exception e) {
            if (debug.warningEnabled()) {
                    debug.warning("getToken: " , e);
            }
        }
        return false;
    }

    
    /**
     * Return set which contains union of the two sets
     * if one of the set is null or empty, return the other set
     * @param set1 First set will be joined
     * @param set2 Second set will be joined
     * @return set which contains union of the two sets
     */
    private Set mergeSet(Set set1, Set set2) {
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
                Set returnSet = new HashSet(set1);
                returnSet.addAll(set2);
                return returnSet;
            }
        }
    }
    
    /**
     * Converts a set to a map, keys are the elements in the set, value is
     * the token. User naming attribute is always added as one of the key.
     * @param names set of names are used key for map
     * @param token value for each named key
     * @return map that is converted from set
     */
    private Map toAvPairMap(Set names, String token) {
        if (token == null) {
            return Collections.EMPTY_MAP;
        }
        Map map = new HashMap();
        Set set = new HashSet();
        set.add(token);
        //map.put(AMStoreConnection.getNamingAttribute(AMObject.USER), set);
        //map.put(userNamingAttr, set);
        if (names == null || names.isEmpty()) {
            return map;
        }
        Iterator it = names.iterator();
        while (it.hasNext()) {
            map.put((String) it.next(), set);
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
        if (messageEnabled) {
            debug.message("setting userID : " + userID);
        }
        failureTokenId  = userID;
    }

    /* update the httpsession with internalsession if
     * failover is enabled
     */
    void updateSessionForFailover() {
        InternalSession intSess = getSession();
        intSess.setIsISStored(true);
    }
    
    /**
     * Returns Callbacks per Page state.
     *
     * @param pageState
     * @return Callbacks per Page state.
     */
    public Callback[] getCallbacksPerState(String pageState) {
        Callback[] rtnCallbacks = null;
        rtnCallbacks = (Callback[]) callbacksPerState.get(pageState);
        return rtnCallbacks;
    }
    
    /**
     * Sets Callbacks per Page state.
     *
     * @param pageState
     * @param callbacks
     */
    public void setCallbacksPerState(String pageState, Callback[] callbacks) {
        this.callbacksPerState.put(pageState, callbacks);
    }
    
    /**
     * Sets the persistent cookie user name.
     *
     * @param indexName Persistent cookie user name.
     */
    public void setPCookieUserName(String indexName) {
        this.pCookieUserName = indexName;
        if (messageEnabled) {
            debug.message("Setting Pcookie user name : " + pCookieUserName);
        }
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
     * Returns <code>true<code> if cookie detected.
     *
     * @return <code>true<code> if cookie detected.
     */
    public boolean isCookieDetect() {
        return cookieDetect;
    }
    
   /* add the SSOTokenPrincipal to the Subject */
    private Subject addSSOTokenPrincipal(Subject subject) {
        if (subject == null) {
            subject = new Subject();
        }
        String sidStr = sid.toString();
        if (messageEnabled) {
            debug.message("sid string is.. " + sidStr);
        }
        Principal ssoTokenPrincipal = new SSOTokenPrincipal(sidStr);
        subject.getPrincipals().add(ssoTokenPrincipal);
        if (messageEnabled) {
            debug.message("Subject is.. :" + subject);
        }
        
        return subject;
    }
    
    /**
     * Sets a Map of attribute value pairs to be used when the authentication
     * service is configured to dynamically create a user.
     *
     * @param attributeValuePairs Map of attribute name to a set of values.
     */
    public void setUserCreationAttributes(Map attributeValuePairs) {
        if (messageEnabled) {
            debug.message("attributeValuePairs : " + attributeValuePairs);
        }
        if ((attributeValuePairs != null) && (!attributeValuePairs.isEmpty())) {
            if (userCreationAttributes == null) {
                userCreationAttributes= new HashMap();
            }
            
            if (attributeValuePairs.containsKey(
                ISAuthConstants.USER_ALIAS_ATTR)
            ) {
                externalAliasList = (HashSet)attributeValuePairs.get(
                ISAuthConstants.USER_ALIAS_ATTR);
                if (messageEnabled){
                    debug.message("externalAliasList:" +externalAliasList);
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
        if (messageEnabled) {
            debug.message("Module name is .. " + moduleName);
            debug.message("successModuleSet is : " + successModuleSet);
        }
    }
    
    /**
     * Returns a Set which contains the modules names which user
     * successfully authenticated.
     * @return a Set which contains the modules names which user
     * successfully authenticated.
     */
    protected Set getSuccessModuleSet() {
        if (messageEnabled) {
            debug.message("getSuccessModuleSet : " + successModuleSet);
        }
        return successModuleSet;
    }
    
    /**
     * Checks if module is Application.
     * @param moduleName is the module name to be compared with 
     * Application module name.
     * @return true if module is Application else false.
     */
    private boolean isApplicationModule(String moduleName) {
        boolean isApp = (moduleName != null) &&
        (moduleName.equalsIgnoreCase(
        ISAuthConstants.APPLICATION_MODULE));
        
        if (ad.debug.messageEnabled()) {
            ad.debug.message("is Application Module : " + isApp);
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
        if (messageEnabled) {
            debug.message("Module name is .. " + moduleName);
            debug.message("failureModuleSet is : " + failureModuleSet);
        }
        return;
    }
    
    /**
     * Returns the failure module list.
     *
     * @return Failure module list.
     */
    public Set getFailureModuleSet() {
        return failureModuleSet;
    }
    
    /**
     * Sets the failure module list.
     *
     * @param failureModuleList which is the list of failed modules.
     */
    public void setFailureModuleList(String failureModuleList) {
        this.failureModuleList = failureModuleList;
        if (messageEnabled) {
            debug.message("failureModulelist :" + failureModuleList);
        }
    }
    
    /**
     * Returns <code>true</code> if the logged in user is 'Agent'.
     *
     * @param amIdentityUser OpenSSO Identity user.
     * @return <code>true</code> if the logged in user is 'Agent'.
     */
    public boolean isAgent(AMIdentity amIdentityUser) {
        boolean isAgent = false;
        try {
            if (amIdentityUser != null &&
                amIdentityUser.getType().equals(IdType.AGENT)) {
                isAgent = true;
                debug.message("user is of type 'Agent'");
            }
        } catch (Exception e) {
            if (messageEnabled) {
                debug.message("Error isAgent : " + e.toString());
            }
        }
        return isAgent;
    }
    
    /** Sets the module map which has key as module localized name and
     *  value is module name
     *
     * @param moduleMap module containing map of module localized name
     *               and module name.
     */
    protected void setModuleMap(Map moduleMap) {
        this.moduleMap = moduleMap;
    }
    
    /** Returns the key for the localized module name.
     *
     * @param localizedModuleName , the localized module name
     *  @return a string, the module name
     */
    protected String getModuleName(String localizedModuleName) {
        return  (String) moduleMap.get(localizedModuleName);
    }

    /**
     * TODO-JAVADOC
     */
    public void nullifyUsedVars() {
        receivedCallbackInfo = null;
        prevCallback = null;
        submittedCallbackInfo = null;
        callbacksPerState = null;
        
        // not 100% sure this one should be nullified
        // but don't seem it's being used there after
        requestHash = null;
        
        aliasAttrNames = null;
        defaultRoles = null;
        token = null;
        tokenSet = null;
        prevIndexType = null;
        userAliasList = null;
        Map requestMap = null;
        accountLife=null;
        loginLockoutNotification = null;
        loginLockoutAttrName = null;
        loginLockoutAttrValue = null;
        invalidAttemptsDataAttrName = null;
        lockoutMsg = null;
        principalList = null;
        cert = null;
        userCreationAttributes = null;
        externalAliasList = null;
        failureModuleSet = null;
        failureModuleList = ISAuthConstants.EMPTY_STRING;
        moduleMap = null;
        if ( (!(persistentCookieOn && persistentCookieMode)) ||
        (foundPCookie != null && !foundPCookie.booleanValue())) {
            userContainerDN=null;
            userNamingAttr=null;
        }
        
        /**
         * The rest variables are all url related.
         * Too many are being used. There should be
         * a way to consolidate them to a smaller number.
         * But the logic of these variables are too much
         * tangled so I will leave it to future cleanup
         * or somebody could volunteer to do it.
         *
         * gotoURL= null;
         * gotoOnFailURL= null;
         *
         * failureLoginURL=null;
         * successLoginURL=null;
         *
         * moduleSuccessLoginURL=null;
         * moduleFailureLoginURL=null;
         *
         * orgSuccessLoginURLSet = null;
         * orgFailureLoginURLSet = null;
         *
         * clientOrgSuccessLoginURL=null;
         * clientOrgFailureLoginURL=null;
         *
         * defaultOrgSuccessLoginURL=null;
         * defaultOrgFailureLoginURL=null;
         *
         * defaultUserSuccessURL ;
         * defaultUserFailureURL ;
         *
         * clientUserSuccessURL ;
         * clientUserFailureURL ;
         *
         * userSuccessURLSet = Collections.EMPTY_SET;
         * userFailureURLSet = Collections.EMPTY_SET;
         *
         * clientSuccessRoleURL ;
         * clientFailureRoleURL ;
         *
         * defaultSuccessRoleURL ;
         * defaultFailureRoleURL ;
         *
         * successRoleURLSet = Collections.EMPTY_SET;
         * failureRoleURLSet = Collections.EMPTY_SET;
         *
         * defaultSuccessURL = null;
         * defaultFailureURL = null;
         *
         * tempDefaultURL = null;
         * fqdnFailureLoginURL=null;
         */
    }
    
    /**
     * Sets locale from servlet request.
     *
     * @param request HTTP Servlet Request.
     */
    public void setRequestLocale(HttpServletRequest request) {
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
    public boolean isSessionInvalid() {
        return (session == null || session.getState() ==  Session.INVALID ||
                session.getState() == Session.DESTROYED);
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
                ad.getIdentity(IdType.ROLE, roleName, getOrgDN());
        } catch (AuthException ae) {
            debug.message("role not found or is not a static role");
        }
        if (amIdentityRole == null) {
            amIdentityRole =
                ad.getIdentity(IdType.FILTEREDROLE, roleName, getOrgDN());
        }
        return amIdentityRole;
    }
    
    /**
     * Returns Universal Identifier of a role.
     *
     * @param roleName Role Name.
     * @return universal identifier of role name.
     */
    public String getRoleUniversalId(String roleName) {
        String roleUnivId = null;
        try {
            AMIdentity amIdentity = getRole(roleName);
            roleUnivId = IdUtils.getUniversalId(amIdentity);
        } catch (Exception ae) {
            if (messageEnabled) {
                debug.message("Error getting role : " +ae.getMessage());
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
            if (principalList.indexOf("|") != -1) {
                StringTokenizer st = new StringTokenizer(principalList,"|");
                while (st.hasMoreTokens()) {
                    String sToken = (String)st.nextToken();
                    if (DN.isDN(sToken)) {
                        returnUserDN = sToken;
                        break;
                    }
                }
            } else if (DN.isDN(principalList)) {
                returnUserDN = principalList;
            }
        }
    
        if (returnUserDN == null || (returnUserDN.length() == 0)) {
            if (amIdentityUser != null) {
                returnUserDN = IdUtils.getDN(amIdentityUser);
            } else if (userDN != null) {
                returnUserDN = userDN;
            } else {
                returnUserDN = tokenToDN(principalList);
            }
        }
        return returnUserDN;
    }

    /*
     * For backward compatibility only
     * OPEN ISSUE - will remove once IdRepo takes care
     * of this
     */
    /**  
     * Return DN for container
     * @param containerDNs set of DN for containers
     * @throws AuthException if container name is invalid
     */
    void getContainerDN(Set containerDNs) throws AuthException {
        
        String userOrgDN = null;
        String agentContainerDN = null;
        // Check Container DNs for NULL
        if ((containerDNs == null) || (containerDNs.isEmpty())) {
            debug.message("Container DNs is null");
            nullUserContainerDN = true;
        } else {
            Iterator it = containerDNs.iterator();
            while (it.hasNext()) {
                String containerName = (String) it.next();
                try {
                    if (Misc.isDescendantOf(containerName, getOrgDN())) {
                        int containerType =
                        ad.getSDK().getAMObjectType(containerName);
                        if (messageEnabled) {
                            debug.message("Container Type = "
                            + containerType);
                            debug.message("Container Name = "
                            + containerName);
                        }
                        if ((containerType == AMObject.ORGANIZATIONAL_UNIT)
                        && (agentContainerDN == null)){
                            agentContainerDN = containerName;
                            identityTypes.add("agent");
                        } else if ((containerType == AMObject.ORGANIZATION)
                        && (userOrgDN == null)){
                            userOrgDN = containerName;
                            identityTypes.add("agent");
                            identityTypes.add("user");
                        } else if ((containerType == AMObject.PEOPLE_CONTAINER)
                        && (userContainerDN == null)){
                            userContainerDN = containerName;
                            identityTypes.add("user");
                        }
                    }
                    if (userContainerDN != null && agentContainerDN != null
                    && userOrgDN != null) {
                        break;
                    }
                } catch (Exception e) {
                    debug.error("Container - " +  containerName +
                    " is INVALID :- " , e);
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
                debug.message("Cannot get userContainer DN");
            }
        }
        
        if (userContainerDN == null && agentContainerDN == null) {
            debug.message("No Valid Container in the list");
            throw new AuthException(AMAuthErrorCode.AUTH_ERROR, null);
        }
        
        if (messageEnabled) {
            debug.message("agentContainerDN = " + agentContainerDN);
            debug.message("userContainerDN = " + userContainerDN);
            debug.message("userOrgDN set in PC atrr = " + userOrgDN);
        }
        
    }
    
    /** 
     * Search for identities given the identity type, identity name
     *  Use common method from AuthD for getIdentity
     *  @param idType identity type for user
     *  @param userTokenID user token identifier
     *  @return IdSearchResults for given the identity type and identity name
     *  @throws IdRepoException if it fails to search user
     *  @throws SSOException if <code>SSOToken</code> is not valid
     */
    IdSearchResults searchIdentity(IdType idType,String userTokenID)
            throws IdRepoException,SSOException  {
        if (messageEnabled) {
            debug.message("In searchAutehnticatedUser: idType " + idType);
            debug.message("In getUserProfile : Search for user " +
            userTokenID);
        }
        IdSearchResults searchResults = null;
        Set returnSet = mergeSet(aliasAttrNames, userAttributes);
        int maxResults = 2;
        int maxTime = 0;
        String pattern = null;
        Map avPairs=null;
        boolean isRecursive = true;
        
        IdSearchControl idsc = new IdSearchControl();
        idsc.setRecursive(isRecursive);
        idsc.setTimeOut(maxTime);
        idsc.setAllReturnAttributes(true);
        //idsc.setReturnAttributes(returnSet);
        
        if (messageEnabled) {
            debug.message("alias attr=" + aliasAttrNames +
            ", attr=" + userAttributes + ",merge=" + returnSet);
        }
        
        if (messageEnabled) {
            debug.message("Search for Identity " + userTokenID);
        }
        // search for the identity
        Set result = Collections.EMPTY_SET;
        try {
            idsc.setMaxResults(0);
            searchResults =
            amIdRepo.searchIdentities(idType,userTokenID,idsc);
            if (searchResults != null) {
                result = searchResults.getSearchResults();
            }
        } catch (SSOException sso) {
            if (messageEnabled) {
                debug.message("SSOException Error searching Identity " +
                " with username " + sso.getMessage());
            }
        } catch (IdRepoException e) {
            if (messageEnabled) {
                debug.message("IdRepoException : Error searching "
                + " Identities with username : "
                + e.getMessage());
            }
        }
        
        if ( result.isEmpty() && (aliasAttrNames != null) &&
        (!aliasAttrNames.isEmpty())) {
            if (messageEnabled) {
                debug.message("No identity found, try Alias attrname.");
            }
            pattern="*";
            avPairs= toAvPairMap(aliasAttrNames, userTokenID);
            if (messageEnabled) {
                debug.message("Search for Filter (avPairs) :" + avPairs);
                debug.message("userTokenID : " + userTokenID);
                debug.message("userDN : " + userDN);
                debug.message("idType :" + idType);
                debug.message("pattern :" + pattern);                
                debug.message("isRecursive :" + isRecursive);
                debug.message("maxResults :" + maxResults);
                debug.message("maxTime :" + maxTime);
                debug.message("returnSet :" + returnSet);
            }
            Set resultAlias = Collections.EMPTY_SET;
            try {
                idsc.setMaxResults(maxResults);
                idsc.setSearchModifiers(IdSearchOpModifier.OR, avPairs);
                searchResults = amIdRepo.searchIdentities(idType,pattern,idsc);
                if (searchResults != null) {
                    resultAlias = searchResults.getSearchResults();
                }
                if ((resultAlias.isEmpty()) && (userDN != null) &&
                    (!userDN.equalsIgnoreCase(userTokenID))) {
                    avPairs= toAvPairMap(aliasAttrNames, userDN);
                    if (messageEnabled) {
                        debug.message("Search for Filter (avPairs) " + 
                        "with userDN : " + avPairs);
                    }
                    idsc.setMaxResults(maxResults);
                    idsc.setSearchModifiers(IdSearchOpModifier.OR, avPairs);
                    searchResults = 
                        amIdRepo.searchIdentities(idType,pattern,idsc);
                }
            } catch (SSOException sso) {
                if (messageEnabled) {
                    debug.message("SSOException : Error searching "
                    + "Identities with aliasattrname : "
                    + sso.getMessage());
                }
            } catch (IdRepoException e) {
                if (messageEnabled) {
                    debug.message("IdRepoException : Error searching "
                    + "Identities : "+e.getMessage());
                }
            }
        }
        return searchResults;
    } // end
    
    
    /**
     * Creates <code>AMIdentity</code> in the repository.
     *
     * @param userName name of user to be created.
     * @param userAttributes Map of default attributes.
     * @param userRoles Set of default roles.
     * @return <code>AMIdentity</code> object of created user.
     *  @throws IdRepoException if it fails to create <code>AMIdentity</code>
     *  @throws SSOException if <code>SSOToken</code> for admin is not valid
     */
    public AMIdentity createUserIdentity(
        String userName,
        Map userAttributes,
        Set userRoles
    ) throws IdRepoException, SSOException {
        AMIdentity amIdentityUser = amIdRepo.createIdentity(IdType.USER,
            userName, userAttributes);
        if (userRoles != null && !userRoles.isEmpty()) {
            Iterator iter = userRoles.iterator();
            while (iter.hasNext()) {
                String trole = (String)iter.next();
                try {
                    if (trole.length() != 0) {
                        amIdentityRole =  getRole(trole);
                        amIdentityRole.addMember(amIdentityUser);
                    }
                } catch (Exception e) {
                    debug.message("createUserProfile():invalid role: ",e);
                    //ignore invalid Roles
                    continue;
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
                amIdUser = ad.getIdentity(IdType.USER, userName, getOrgDN());
            }
            universalId =  IdUtils.getUniversalId(amIdentityUser);
        } catch (Exception e) {
            debug.message(
                "Error getting Identity for user :" + e.getMessage());
        }
        if (messageEnabled) {
            debug.message("getUserUniversalId:universalId : " + universalId);
        }
        return universalId;
    }

    /**
     * Sets the type of authentication to be used after Composite Advices.
     *
     * @param type Type of authentication.
     *
     */
    public void setCompositeAdviceType(int type) {
        this.compositeAdviceType = type;
    }
    
    /**
     * Returns the type of authentication to be used after Composite Advices.
     * 
     * @return an integer type indicating the type of authentication required.
     *
     */
    public int getCompositeAdviceType() {
        return compositeAdviceType;
    }
    
    /**
     * Sets the Composite Advice for this Authentication request.
     *
     * @param compositeAdvice Composite Advice for authentication.
     *
     */
    public void setCompositeAdvice(String compositeAdvice) {
       this.compositeAdvice = compositeAdvice;
    }

    /**
     * Returns the Composite Advice for this Authentication request.
     *
     * @return String of Composite Advice.
     *
     */
    public String getCompositeAdvice() {
        return compositeAdvice;
    }
    
    /**
     * Sets the qualified OrgDN for Policy conditions 
     * to be used after Composite Advices.
     *
     * @param qualifiedOrgDN qualifiedOrgDN for Policy conditions.
     *
     */
    public void setQualifiedOrgDN(String qualifiedOrgDN) {
        this.qualifiedOrgDN = qualifiedOrgDN;
    }
    
    /**
     * Returns the Authentication configuration / Authentication 
     * chain name used for current authentication process.
     * 
     * @param indexType AuthContext.IndexType
     * @param indexName Index Name for AuthContext.IndexType  
     *
     */
    String getAuthConfigName(AuthContext.IndexType indexType,
    String indexName) {                
        String finalAuthConfigName = null;        
        if (indexType == AuthContext.IndexType.ROLE) {      
            finalAuthConfigName = roleAuthConfig;
        } else if (indexType == AuthContext.IndexType.SERVICE) {
            if (indexName.equals(ISAuthConstants.CONSOLE_SERVICE)) {
                if (AuthD.revisionNumber >= ISAuthConstants.
                AUTHSERVICE_REVISION7_0) {
                    if ((orgAdminAuthConfig != null) && 
                    (!orgAdminAuthConfig.equals(ISAuthConstants.BLANK))) {
                        finalAuthConfigName = orgAdminAuthConfig;
                    }
                }
            } else {
                finalAuthConfigName = indexName;
            }
        } else if ((indexType == AuthContext.IndexType.USER) && 
          (AuthD.revisionNumber >= ISAuthConstants.AUTHSERVICE_REVISION7_0)) {
            if (((userAuthConfig != null) && (!userAuthConfig.equals(
            ISAuthConstants.BLANK)))){
                 finalAuthConfigName = userAuthConfig;
            }
        } else if ((AuthD.revisionNumber >= ISAuthConstants.
          AUTHSERVICE_REVISION7_0) && (indexType == null)) {
            if ((orgAuthConfig != null) && (!orgAuthConfig.
            equals(ISAuthConstants.BLANK)))  {
                finalAuthConfigName = orgAuthConfig;
            }
        }

        if (messageEnabled){
            debug.message("getAuthConfigName:finalAuthConfigName = " 
                + finalAuthConfigName);
        }

        return finalAuthConfigName;
    }

    boolean isAuthValidForInternalUser() {
        boolean authValid = true;
        if (session == null) {
            debug.warning(
                "LoginState.isValidAuthForInternalUser():session is null");
            return false;
        }
        getTokenFromPrincipal(subject);
        String userId = token;
        if (userId == null) {
            debug.warning(
                "LoginState.isValidAuthForInternalUser():userId is null");
            return false;
        }
        if (internalUsers.contains(userId.toLowerCase())) {
            String authRealm = orgDN;
            String authModule = "";
            if (!successModuleSet.isEmpty()) {
                authModule = (String)(successModuleSet.iterator().next());
            }
            if (authRealm == null) {
                debug.warning(
                    "LoginState.isValidAuthForInternalUser():authRealm is null");
                return false;
            }
            if (authModule == null) {
                if (debug.warningEnabled()) {
                    debug.warning("LoginState.isValidAuthForInternalUser():"
                            + "authModule is null");
                }
                return false;
            }
            if (debug.messageEnabled()) {
                debug.message("LoginState.isValidAuthForInternalUser():"
                        + "Attempt to login as:" + userId
                        + ", to module:" + authModule
                        + ", at realm:" + authRealm);
            }
            if (!authRealm.equals(ServiceManager.getBaseDN())) {
                authValid = false;
                if (debug.warningEnabled()) {
                    debug.warning("LoginState.isValidAuthForInternalUser():"
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
     * Sets userDN based on pcookie - called by <code>AMLoginContext</code>.
     */
    public void setUserName(String username) {
        userDN = username;
    }
}
