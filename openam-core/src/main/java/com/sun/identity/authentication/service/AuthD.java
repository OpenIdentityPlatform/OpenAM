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
 * $Id: AuthD.java,v 1.23 2009/11/25 12:02:02 manish_rustagi Exp $
 *
 * Portions Copyrighted 2010-2015 ForgeRock AS.
 */
package com.sun.identity.authentication.service;

import com.iplanet.am.sdk.AMStoreConnection;
import com.iplanet.am.util.Misc;
import com.iplanet.am.util.SystemProperties;
import com.iplanet.dpro.session.Session;
import com.iplanet.dpro.session.SessionException;
import com.iplanet.dpro.session.SessionID;
import com.iplanet.dpro.session.service.InternalSession;
import com.iplanet.dpro.session.service.SessionService;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.authentication.AuthContext;
import com.sun.identity.authentication.util.ISAuthConstants;
import com.sun.identity.common.DNUtils;
import com.sun.identity.common.RequestUtils;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.AMIdentityRepository;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdSearchControl;
import com.sun.identity.idm.IdSearchResults;
import com.sun.identity.idm.IdType;
import com.sun.identity.idm.IdUtils;
import com.sun.identity.log.LogConstants;
import com.sun.identity.log.Logger;
import com.sun.identity.log.messageid.LogMessageProviderBase;
import com.sun.identity.log.messageid.MessageProviderFactory;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.datastruct.CollectionHelper;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.ldap.util.DN;
import com.sun.identity.sm.OrganizationConfigManager;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceManager;
import com.sun.identity.sm.ServiceSchema;
import com.sun.identity.sm.ServiceSchemaManager;
import org.forgerock.openam.security.whitelist.ValidGotoUrlExtractor;
import org.forgerock.openam.shared.security.whitelist.RedirectUrlValidator;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.security.AccessController;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.StringTokenizer;
import org.forgerock.guice.core.InjectorHolder;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;


/**
 * This class is used to initialize the Authentication service and retrieve 
 * the Global attributes for the Authentication service.
 * It also initializes the other dependent services in the OpenAM system and
 * hence used as bootstrap class for the authentication server.
 */
public class AuthD  {
    /**
     * Configured bundle name for auth service
     */
    public static final String BUNDLE_NAME = ISAuthConstants.AUTH_BUNDLE_NAME;

    /**
     * Debug instance for error / message logging
     */
    public static final Debug debug = Debug.getInstance(BUNDLE_NAME);

    private static final ConcurrentMap<String, ResourceBundle> bundles =
            new ConcurrentHashMap<String, ResourceBundle>();

    /**
     * Lazy initialisation holder idiom for the singleton instance.
     */
    private static final class SingletonHolder {
        private static final AuthD INSTANCE = new AuthD();
    }

    /**
     * Lazy initialisation holder idiom for other lazily-loaded configuration.
     */
    private static final class LazyConfig {
        private static final AMIdentity superUserIdentity = new AMIdentity(
                AccessController.doPrivileged(AdminTokenAction.getInstance()),
                superAdmin,
                IdType.USER,
                "/",
                null);
    }

    private static final String superAdmin = DNUtils.normalizeDN(
            SystemProperties.get(Constants.AUTHENTICATION_SUPER_USER, ""));
    private static final String specialUser =
            SystemProperties.get(Constants.AUTHENTICATION_SPECIAL_USERS, "");

    // Admin Console properties
    private static final String consoleProto =
            SystemProperties.get(Constants.AM_CONSOLE_PROTOCOL, "http");
    private static final String consoleHost =
            SystemProperties.get(Constants.AM_CONSOLE_HOST);
    private static final String consolePort =
            SystemProperties.get(Constants.AM_CONSOLE_PORT);
    private static final boolean isConsoleRemote =
            SystemProperties.getAsBoolean(Constants.AM_CONSOLE_REMOTE);

    /**
     * Default auth level for auth module
     */  
    private static final String DEFAULT_AUTH_LEVEL = "0";
    /**
     * Configured value for access logging
     */  
    static final int LOG_ACCESS = 0;
    /**
     * Configured value for error logging
     */  
    static final int LOG_ERROR  = 1;

    private static final boolean enforceJAASThread = SystemProperties.getAsBoolean(Constants.ENFORCE_JAAS_THREAD);
    /**
     * Configured directory server host name for auth
     */
    public static final String directoryHostName = SystemProperties.get(Constants.AM_DIRECTORY_HOST);
    /**
     * Configured directory server port number for auth
     */
    public static final int directoryPort = SystemProperties.getAsInt(Constants.AM_DIRECTORY_PORT, 0);

    private static final boolean logStatus = "ACTIVE".equalsIgnoreCase(SystemProperties.get(Constants.AM_LOGSTATUS,
            "INACTIVE"));
    /**
     * Configured revisionNumber for auth service
     */
    public static int revisionNumber;

    private final ConcurrentMap<String, AMIdentityRepository> idRepoMap =
            new ConcurrentHashMap<String, AMIdentityRepository>();
    private final ConcurrentMap<String, OrganizationConfigManager> orgMap =
            new ConcurrentHashMap<String, OrganizationConfigManager>();


    private final String defaultOrg;
    private String platformLocale;
    private final String platformCharset;
    /**
     * ResourceBundle for auth service
     */
    final ResourceBundle bundle;

    private final SSOToken ssoAuthSession;
    private AMStoreConnection dpStore = null;

    // session service schema
    private ServiceSchema sessionSchema;

    private Set defaultSuccessURLSet = null;
    private String defaultSuccessURL = null;
    private Set defaultFailureURLSet = null;
    private String defaultFailureURL = null;
    private Set defaultServiceSuccessURLSet = null;
    private Set defaultServiceFailureURLSet = null;
    private String adminAuthModule;
    /**
     * Default auth level for module
     */
    public String defaultAuthLevel;
    private final ConcurrentMap<String, String> authMethods = new ConcurrentHashMap<String, String>();
    private long defaultSleepTime = 300; /* 5 minutes */
    private static final RedirectUrlValidator<String> REDIRECT_URL_VALIDATOR =
            new RedirectUrlValidator<String>(ValidGotoUrlExtractor.getInstance());
    
    private ServletContext servletContext;

    private final String rootSuffix;

    static {
        if (debug.messageEnabled()) {
            debug.message("Directory Host: "+ directoryHostName +
            "\nDirectory PORT : "+ directoryPort);
        }
    }
    

    private AuthD() {
        debug.message("AuthD initializing");
        try {
            rootSuffix = defaultOrg = ServiceManager.getBaseDN();
            final Session authSession = initAuthSession();
            ssoAuthSession = initSsoAuthSession(authSession);
            initAuthServiceGlobalSettings();
            initPlatformServiceGlobalSettings();
            initSessionServiceDynamicSettings();
            initAuthConfigGlobalSettings();
            bundle = com.sun.identity.shared.locale.Locale.
                getInstallResourceBundle(BUNDLE_NAME);
            ResourceBundle platBundle =
                com.sun.identity.shared.locale.Locale.getInstallResourceBundle(
                    "amPlatform");
            platformCharset = platBundle.getString(
                ISAuthConstants.PLATFORM_CHARSET_ATTR);
            printProfileAttrs();
            // Initialize AuthXMLHandler so that AdminTokenAction can
            // generate DPro Session's SSOToken
            new com.sun.identity.authentication.server.AuthXMLHandler();
        } catch (Exception ex) {
            debug.error("AuthD init()", ex);
            throw new IllegalStateException("Unable to initialize AuthD", ex);
        }
    }

    /**
     * Initialized auth service global attributes
     * @throws SMSException if it fails to get auth service for name
     * @throws SSOException if admin <code>SSOToken</code> is not valid 
     * @throws Exception
     */
    private void initAuthServiceGlobalSettings() throws Exception {
        ServiceSchemaManager scm = new ServiceSchemaManager(ISAuthConstants.AUTH_SERVICE_NAME, ssoAuthSession);
        revisionNumber = scm.getRevisionNumber();
        if (debug.messageEnabled()) {
            debug.message("revision number = " + revisionNumber);
        }
        updateAuthServiceGlobals(scm);
        new AuthConfigMonitor(scm);
    }
    
    /**
     * Update the AuthService global and organization settings.
     * most of the code is moved in from AuthenticatorManager.java.
     * @param scm <code>ServiceSchemaManager</code> to be used for update
     * @throws SMSException if it fails to update auth service
     * @throws Exception
     */
    synchronized void updateAuthServiceGlobals(ServiceSchemaManager scm) throws Exception {
        
        ServiceSchema schema = scm.getOrganizationSchema();
        Map attrs = schema.getAttributeDefaults();
        
        // get Global type attributes for iPlanetAMAuthService
        schema = scm.getGlobalSchema();
        
        attrs.putAll(schema.getAttributeDefaults());
        if (debug.messageEnabled()) {
            debug.message("attrs : " + attrs);
        }
        
        adminAuthModule = CollectionHelper.getMapAttr(
            attrs, ISAuthConstants.ADMIN_AUTH_MODULE);
        defaultAuthLevel = CollectionHelper.getMapAttr(
            attrs, ISAuthConstants.DEFAULT_AUTH_LEVEL,DEFAULT_AUTH_LEVEL);
        
        Set s = (Set)attrs.get(ISAuthConstants.AUTHENTICATORS);
        for (final Object value : s) {
            String name = (String) value;
            int dot = name.lastIndexOf('.');
            if (dot > -1) {
                String tmp = name.substring(dot + 1, name.length());
                authMethods.put(tmp, name);
            } else {
                authMethods.put(name, name);
            }
        }
        if (debug.messageEnabled()) {
            debug.message("AM.update authMethods = " + authMethods.toString());
        }
        
        defaultSuccessURLSet =
        (Set) attrs.get(ISAuthConstants.LOGIN_SUCCESS_URL);
        defaultFailureURLSet =
        (Set) attrs.get(ISAuthConstants.LOGIN_FAILURE_URL);
        
        if (debug.messageEnabled()) {
            debug.message("Default Success URL Set = " + defaultSuccessURLSet);
            debug.message("Default Failure URL Set = " + defaultFailureURLSet);
        }
        
        Integer sleepTime = new Integer(CollectionHelper.getMapAttr(
            attrs, ISAuthConstants.SLEEP_INTERVAL));
        defaultSleepTime = sleepTime.longValue();
        
    }
    
    /**
     * Initialize the AuthConfiguration global attributes.
     * @throws SMSException if it fails to get auth service for name
     * @throws SSOException if admin <code>SSOToken</code> is not valid 
     * @throws Exception
     */
    private void initAuthConfigGlobalSettings() throws Exception {
        
        ServiceSchemaManager scm = new ServiceSchemaManager(
        ISAuthConstants.AUTHCONFIG_SERVICE_NAME, ssoAuthSession);
        updateAuthConfigGlobals(scm);
        new AuthConfigMonitor(scm);
    }
    
    /**
     * Update the AuthConfiguration organization attributes.
     * @param scm <code>ServiceSchemaManager</code> to be used for update
     * @throws SMSException if it fails to update auth service
     */
    synchronized void updateAuthConfigGlobals(ServiceSchemaManager scm)
    throws SMSException {
        
        ServiceSchema schema = scm.getOrganizationSchema();
        
        schema = schema.getSubSchema("Configurations");
        schema = schema.getSubSchema("NamedConfiguration");
        Map attrs = schema.getAttributeDefaults();
        
        if (attrs != null) {
            defaultServiceSuccessURLSet =
            (Set)attrs.get(ISAuthConstants.LOGIN_SUCCESS_URL);
            defaultServiceFailureURLSet =
            (Set)attrs.get(ISAuthConstants.LOGIN_FAILURE_URL);
        }
        if (debug.messageEnabled()) {
            debug.message("Default Service Success URL Set = " +
            defaultServiceSuccessURLSet);
            debug.message("Default Service Failure URL Set = " +
            defaultServiceFailureURLSet);
        }
    }
    
    /**
     * Initialized platform service global attributes
     * @throws SMSException if it fails to initialize platform service
     * @throws SSOException if admin <code>SSOToken</code> is not valid 
     */
    private void initPlatformServiceGlobalSettings()
    throws SMSException, SSOException {
        ServiceSchemaManager scm = new ServiceSchemaManager(
        ISAuthConstants.PLATFORM_SERVICE_NAME, ssoAuthSession);
        updatePlatformServiceGlobals(scm);
        new AuthConfigMonitor(scm);
    }
    
    /**
     * Update the PlatformService global attributes.
     * @param scm <code>ServiceSchemaManager</code> to be used for update
     * @throws SMSException if it fails to initialize platform service
     */
    synchronized void updatePlatformServiceGlobals(ServiceSchemaManager scm)
    throws SMSException {
        ServiceSchema platformSchema = scm.getGlobalSchema();
        Map attrs = platformSchema.getAttributeDefaults();
        
        platformLocale = CollectionHelper.getMapAttr(
            attrs, ISAuthConstants.PLATFORM_LOCALE_ATTR);
        
        if (debug.messageEnabled()) {
            debug.message("PlatformLocale = " + platformLocale);
        }
    }
    
    /**
     * Initialize iPlanetAMSessionService Dynamic attributes
     * @throws SMSException if it fails to initialize session service
     * @throws SSOException if admin <code>SSOToken</code> is not valid 
     */
    private void initSessionServiceDynamicSettings()
    throws SMSException, SSOException {
        ServiceSchemaManager scm = new ServiceSchemaManager(
        ISAuthConstants.SESSION_SERVICE_NAME, ssoAuthSession);
        updateSessionServiceDynamics(scm);
        new AuthConfigMonitor(scm);
    }
    
    /**
     * Update the SessionService dynamic attributes.
     * @param scm <code>ServiceSchemaManager</code> to be used for update
     * @throws SMSException if it fails to update session service
     */
    synchronized void updateSessionServiceDynamics(ServiceSchemaManager scm)
    throws SMSException {
        
        sessionSchema = scm.getDynamicSchema();
        if (debug.messageEnabled()) {
            Map attrs = sessionSchema.getAttributeDefaults();
            String defaultMaxSessionTime = CollectionHelper.getMapAttr(
                attrs, ISAuthConstants.MAX_SESSION_TIME, "120");
            String defaultMaxIdleTime = CollectionHelper.getMapAttr(
                attrs, ISAuthConstants.SESS_MAX_IDLE_TIME, "30");
            String defaultMaxCachingTime = CollectionHelper.getMapAttr(
                attrs, ISAuthConstants.SESS_MAX_CACHING_TIME, "3");
            debug.message("AuthD.defaultMaxSessionTime=" + defaultMaxSessionTime
            + "\nAuthD.defaultMaxIdleTime=" + defaultMaxIdleTime
            + "\nAuthD.defaultMaxCachingTime=" + defaultMaxCachingTime);
        }
    }
    
    /**
     * Return max session time
     * @return max session time
     */
    String getDefaultMaxSessionTime() {
        return CollectionHelper.getMapAttr(sessionSchema.getAttributeDefaults(),
        ISAuthConstants.MAX_SESSION_TIME, "120");
    }
    
    /**
     * Return max session idle time
     * @return max session idle time
     */
    String getDefaultMaxIdleTime() {
        return CollectionHelper.getMapAttr(sessionSchema.getAttributeDefaults(),
        ISAuthConstants.SESS_MAX_IDLE_TIME, "30");
    }
    
    /**
     * Return  max session caching time
     * @return  max session caching time
     */
    String getDefaultMaxCachingTime() {
        return CollectionHelper.getMapAttr(sessionSchema.getAttributeDefaults(),
        ISAuthConstants.SESS_MAX_CACHING_TIME, "3");
    }

    /**
     * Returns attribute map of the specified service in the specified
     * organization.
     *
     * @param orgDN Organization DN in which the service exists.
     * @param serviceName Service name of which the attributes are retrieved.
     * @return Map containing the attributes of the service.
     */
    public Map getOrgServiceAttributes(String orgDN, String serviceName) {
        Map map = Collections.EMPTY_MAP;
        try {
            AMIdentityRepository idRepo = getAMIdentityRepository(orgDN);
            AMIdentity realmIdentity = idRepo.getRealmIdentity();
            Set set = realmIdentity.getAssignedServices();
            if (set.contains(serviceName)) {
                map = realmIdentity.getServiceAttributes(serviceName);
            }
        } catch (Exception e) {
            if (debug.messageEnabled()) {
                debug.message("Exception in getting service attributes for "
                    + serviceName + " in org " + orgDN);
            }
        }
        return map;
    }
    
    /**
     * Returns Authenticator singleton instance.
     *
     * @return Authenticator singleton instance.
     */
    public static AuthD getAuth() {
        return SingletonHolder.INSTANCE;
    }
    
    /**
     * Destroy sessionfor given <code>SessionID</code>
     * @param sid <code>SessionID</code> to be destroyed
     */
    public void destroySession(SessionID sid) {
        getSessionService().destroyInternalSession(sid);
    }
    
    /**
     * Creates a new session.
     *
     * @param domain Domain Name.
     * @param httpSession HTTP Session.
     * @return new <code>InternalSession</code>
     */
    public static InternalSession newSession(
        String domain, 
        HttpSession httpSession) {
        InternalSession is = null;
        try {
            is = getSessionService().newInternalSession(domain, httpSession);
        } catch (Exception ex) {
            ex.printStackTrace();
            debug.error("Error creating session: ", ex);
        }
        return is;
    }
    
    /**
     * Returns the session associated with a session ID.
     *
     * @param sessId Session ID.
     * @return the <code>InternalSession</code> associated with a session ID.
     */
    public static InternalSession getSession(String sessId) {
        if (debug.messageEnabled()) {
            debug.message("getSession for " + sessId);
        }
        InternalSession is = null;
        if (sessId != null) {
            SessionID sid = new SessionID(sessId);
            is = getSession(sid);
        }
        if (is == null) {
            debug.message("getSession returned null");
        }
        return is;
    }

    /**
     * Returns the session associated with a session ID.
     *
     * @param sid Session ID.
     * @return the <code>InternalSession</code> associated with a session ID.
     */
    public static InternalSession getSession(SessionID sid) {
        InternalSession is = null;
        if (sid != null) {
            is = getSessionService().getInternalSession(sid);
        } 
        return is;
    }
        
        
    /**
     * Returns the session associated with an HTTP Servlet Request.
     *
     * @param req HTTP Servlet Request.
     * @return the <code>InternalSession</code> associated with 
     *   anHTTP Servlet Request.
     */
    public InternalSession getSession(HttpServletRequest req) {
        SessionID sid = new SessionID(req);
        return getSession(sid);
    }
    
    ////////////////////////////////////////////////////////////////
    //  AuthD utilities
    ////////////////////////////////////////////////////////////////
    
    /**
     * Returns an Authenticator for a specific module name.
     *
     * @param moduleName Module name example <code>LDAP</code>.
     * @return Authenticator for a specific module name.
     */
    public String getAuthenticatorForName(String moduleName) {
        return authMethods.get(moduleName);
    }
    
    /**
     * Return configured Authenticators
     * @return list of configured Authenticators
     */
    public Iterator getAuthenticators() {
        return authMethods.keySet().iterator();
    }
    
    /**
     * Return configured PlatformLocale
     * @return configured PlatformLocale
     */
    public String getPlatformLocale() {
        return platformLocale;
    }

    /**
     * Log Logout status 
     */
    public void logLogout(SSOToken ssot){
        try {
            String logLogout = bundle.getString("logout");
            List<String> dataList = new ArrayList<String>();
            dataList.add(logLogout);
            StringBuilder messageId = new StringBuilder();
            messageId.append("LOGOUT");
            String indexType = ssot.getProperty(ISAuthConstants.INDEX_TYPE);
            if (indexType != null) {
                messageId.append("_").append(indexType.toUpperCase());
                dataList.add(indexType);
                if (indexType.equals(AuthContext.IndexType.USER.toString())) { 
                    dataList.add(ssot.getProperty(ISAuthConstants.PRINCIPAL));
                } else if (indexType.equals(
                        AuthContext.IndexType.ROLE.toString())) {
                    dataList.add(ssot.getProperty(ISAuthConstants.ROLE));
                } else if (indexType.equals(
                        AuthContext.IndexType.SERVICE.toString())) {
                    dataList.add(ssot.getProperty(ISAuthConstants.SERVICE));
                } else if (indexType.equals(
                        AuthContext.IndexType.LEVEL.toString())) {
                    dataList.add(ssot.getProperty(ISAuthConstants.AUTH_LEVEL));
                } else if (indexType.equals(
                        AuthContext.IndexType.MODULE_INSTANCE.toString())) {
                    dataList.add(ssot.getProperty(
                            ISAuthConstants.AUTH_TYPE));
                }
            }
            
            Hashtable<String, String> props = new Hashtable<String, String>();
            String client = ssot.getProperty(ISAuthConstants.HOST);
            if (client != null) {
                props.put(LogConstants.IP_ADDR, client);
            }
            String userDN = ssot.getProperty(ISAuthConstants.PRINCIPAL);
            if (userDN != null) {
                props.put(LogConstants.LOGIN_ID, userDN);
            }
            String orgDN = ssot.getProperty(ISAuthConstants.ORGANIZATION);
            if (orgDN != null) {
                props.put(LogConstants.DOMAIN, orgDN);
            }
            String authMethName = ssot.getProperty(ISAuthConstants.AUTH_TYPE);
            if (authMethName != null) {
                props.put(LogConstants.MODULE_NAME, authMethName);
            }
            String contextId = null;
            contextId = ssot.getProperty(Constants.AM_CTX_ID);
            if (contextId != null) {
                props.put(LogConstants.CONTEXT_ID, contextId);
            }
            props.put(LogConstants.LOGIN_ID_SID, ssot.getTokenID()
                .toString());

            String[] data = dataList.toArray(new String[dataList.size()]);
            this.logIt(data, LOG_ACCESS, messageId.toString(), props);
        } catch (SSOException ssoExp) {
            debug.error("AuthD.logLogout: SSO Error", ssoExp);
        } catch (Exception e) {
            debug.error("AuthD.logLogout: Error " , e);
        }
    }

    ////////////////////////////////////////////////////////////////
    //  Other utilities
    ////////////////////////////////////////////////////////////////

    /**
      * Writes a log record.
      *
      * @param s Array of data information for the log record.
      * @param type Type of log either <code>LOG_ERROR</code> or
      *        <code>LOG_ACCESS</code>.
      * @param messageName Message ID for the log record.
      * @param ssoProperties Single Sign On Properties to be written to the
      *        log record. If this is <code>null</code>, properties will be
      *        retrieved from administrator Single Sign On Token.
      */
    public void logIt(
        String[] s,
        int type, 
        String messageName, 
        Hashtable ssoProperties) {
        if (logStatus && (s != null)) {
            try {
                LogMessageProviderBase provider = 
                    (LogMessageProviderBase)MessageProviderFactory.getProvider(
                        "Authentication");

                com.sun.identity.log.LogRecord lr = null;
                
                SSOToken ssot = AccessController.doPrivileged(AdminTokenAction.getInstance());
                if(ssoProperties == null) {
                    lr = provider.createLogRecord(messageName, s, ssot);
                } else {
                    lr = provider.createLogRecord(messageName, s,
                        ssoProperties);
                }

                com.sun.identity.log.Logger logger;
                switch (type) {
                    case LOG_ACCESS:
                        logger = (com.sun.identity.log.Logger)
                        Logger.getLogger("amAuthentication.access");
                        logger.log(lr,ssot);
                        break;
                    case LOG_ERROR:
                        logger = (com.sun.identity.log.Logger)
                        Logger.getLogger("amAuthentication.error");
                        logger.log(lr,ssot);
                        break;
                    default:
                        logger = (com.sun.identity.log.Logger)
                        Logger.getLogger("amAuthentication.access");
                        logger.log(lr,ssot);
                        break;
                }
            } catch(IOException ex) {
                ex.printStackTrace();
                debug.error("Logging exception : " + ex.getMessage());
            }
        }
    }
 
    /**
     * Returns connection for AM store.
     * Only used for backward compatibilty support,
     * for retrieving user container DN and usernaming attr.
     * @return connection for AM store
     */
    public AMStoreConnection getSDK() {
        if (dpStore == null) {
            try {
                dpStore = new AMStoreConnection(ssoAuthSession);
            } catch (SSOException e) {
                debug.warning("AuthD.getSDK", e);
            }
        }
        return dpStore;
    }
    
    void printProfileAttrs() {
        if (!debug.messageEnabled()) {
            return;
        }
        debug.message("Authd Profile Attributes");
        
        String adminAuthName = adminAuthModule;
        int index = adminAuthModule.lastIndexOf(".");
        if (index > 0) {
            adminAuthName = adminAuthModule.substring(index+1);
        }
        if (debug.messageEnabled()) {
            debug.message("adminAuthModule->" + adminAuthModule +
            "\nadminAuthName->" + adminAuthName +
            "\ndefaultOrg->" + defaultOrg +
            "\nlocale->" + platformLocale +
            "\ncharset>" + platformCharset);
        }
    }
    
    static SessionService getSessionService() {
        SessionService sessionService = InjectorHolder.getInstance(SessionService.class);
        if (sessionService == null) {
            debug.error("AuthD failed to get session service instance");
        }
        return sessionService;
    }

    /**
     * Return current sso session for auth
     * @return current sso session for auth
     */
    public SSOToken getSSOAuthSession()  {
        return ssoAuthSession;
    }

    private Session initAuthSession() throws SSOException, SessionException {
        final Session authSession = getSessionService().getAuthenticationSession(defaultOrg, null);
        if (authSession == null) {
            debug.error("AuthD failed to get auth session");
            throw new SessionException(BUNDLE_NAME, "gettingSessionFailed", null);
        }

        String clientID = authSession.getClientID();
        authSession.setProperty("Principal", clientID);
        authSession.setProperty("Organization", defaultOrg);
        authSession.setProperty("Host",
                authSession.getID().getSessionServer());
        DN dn = new DN(clientID);

        if (dn.isDN()) {
            String[] tokens = dn.explodeDN(true);
            String id = "id=" + tokens[0] + ",ou=user," + ServiceManager.getBaseDN();
            authSession.setProperty(Constants.UNIVERSAL_IDENTIFIER, id);
        }

        return authSession;
    }
    
    private SSOToken initSsoAuthSession(Session authSession) throws SSOException, SessionException {
        SSOTokenManager ssoManager = SSOTokenManager.getInstance();
        return ssoManager.createSSOToken(authSession.getID().toString());
    }
    
    /**
     * get inetDomainStatus attribute for the org
     * @param orgName org name to check inetDomainStatus
     * @return true if org is active
     * @throws IdRepoException if can not can any information for org
     * @throws SSOException if can not use <code>SSOToken</code> for admin
     */
    boolean getInetDomainStatus(String orgName)
        throws IdRepoException, SSOException {
        return IdUtils.isOrganizationActive(ssoAuthSession,orgName);
    }
    
    /**
     * Returns <code>true</code> if distinguished user name is a super
     * administrator DN.
     *
     * @param dn Distinguished name of user.
     * @return <code>true</code> if user is super administrator.
     */
    public boolean isSuperAdmin(String dn) {
        boolean isAdmin = false;
        String nDN = DNUtils.normalizeDN(dn);
        if ((nDN != null) && (superAdmin != null || specialUser != null)) {
            if (debug.messageEnabled()) {
                debug.message("passed dn is :" + dn);
            }
            if (superAdmin != null) {
                if (debug.messageEnabled()) {
                    debug.message("normalized super dn is :" + superAdmin);
                }
                isAdmin = nDN.equals(superAdmin);
            }
            if (!isAdmin) {
                isAdmin = isSpecialUser(nDN);
            }
        }
        if (debug.messageEnabled()) {
            debug.message("is Super Admin :" + isAdmin);
        }
        return isAdmin;
    }

    /**
     * Returns <code>true</code> if and only if the user name belongs to a
     * super user
     *
     * @param dn DN of the user
     * @return <code>true</code> if the user is an admin user.
     */
    public boolean isSuperUser(String dn) {
        return LazyConfig.superUserIdentity.getUniversalId().equalsIgnoreCase(dn);
    }

    /**
     * Returns <code>true</code> if distinguished user name is a special user
     * DN.
     *
     * @param dn Distinguished name of user.
     * @return <code>true</code> if user is a special user.
     */
    public boolean isSpecialUser(String dn) {
        // dn in all the invocation is normalized.
        boolean isSpecialUser = false;
        String nDN = DNUtils.normalizeDN(dn);
        if ((nDN != null) && (specialUser != null)) {
            StringTokenizer st = new StringTokenizer(specialUser,"|");
            while (st.hasMoreTokens()) {
                 String specialAdminDN = st.nextToken();
                 if (specialAdminDN != null) {
                    String normSpecialAdmin = 
                        DNUtils.normalizeDN(specialAdminDN);
                
                    if (debug.messageEnabled()) {
                         debug.message("normalized special dn is :" +
                            normSpecialAdmin);
                     }
                     if (nDN.equals(normSpecialAdmin)) {
                         isSpecialUser = true;
                         break;
                     }
                }
             }
         }
        if (debug.messageEnabled()) {
            debug.message("is Special User :" + isSpecialUser);
        }
        return isSpecialUser;
    }
    
    /**
     * Returns Resource bundle of a locale.
     *
     * @param locale Locale.
     * @return Resource bundle of a locale.
     */
    public ResourceBundle getResourceBundle(String locale) {
        if (locale == null) {
            return bundle;
        }
        
        ResourceBundle rb = bundles.get(locale);
        if (rb == null) {
            rb = com.sun.identity.shared.locale.Locale.getResourceBundle(BUNDLE_NAME, locale);

            if (rb == null) {
                rb = bundle;
            }
            bundles.put(locale, rb);
        }
        
        return rb;
    }

    /**
     * Return default sleep time
     * @return default sleep time
     */
    public long getDefaultSleepTime() {
        return defaultSleepTime * 1000;
    }
    
    /**
     * Returns the organization DN.
     * <p>
     * If the organization name matches the root suffix or has the
     * root suffix in it then the DN will be returned as string.
     * Otherwise the DN will be constructed from the organization Name DN
     * and the root suffix DN.
     *
     * @param userOrg Organization Name
     * @return Organization DN of the organization
     */
    public String getOrgDN(String userOrg) {
        DN userOrgDN = new DN(userOrg);
        DN rootSuffixDN = new DN(rootSuffix);
        String orgDN = null;

        if (debug.messageEnabled()) {
            debug.message("userOrg is : " + userOrg);
            debug.message("rootSuffix is : " + rootSuffix);
            debug.message("rootSuffixDN is : " + rootSuffixDN);
            debug.message("userOrgDN is : " +  userOrgDN);
        }

        if (userOrg == null) {
            return rootSuffixDN.toString();
        }
        if ( (userOrgDN.equals(rootSuffixDN))
            || (userOrgDN.isDescendantOf(rootSuffixDN))
        ) {
            orgDN = userOrgDN.toString();
        } else {
            orgDN = (new StringBuffer(50)).append(userOrgDN.toString())
            .append(",").append(rootSuffixDN).toString();
        }
        
        if (debug.messageEnabled()) {
            debug.message("Returning OrgDN is : " + orgDN);
        }
        return orgDN;
    }
    
    /**
     * Returns the dynamic replacement of the URL from the Success or Failure 
     * URLs.
     *
     * @param URL
     * @param servletRequest
     * @return the dynamic replacement of the URL from the Success or Failure 
     * URLs.
     */
    public String processURL(String URL, HttpServletRequest servletRequest) {
        String url = URL;
        
        if (url != null) {
            url = processDynamicVariables(url, servletRequest);
        }
        if (debug.messageEnabled()) {
            debug.message("processURL : " + url);
        }
        return url;
    }
    
    /**
     * This function returns the dynamic replacement of the protocol
     * from the Success or Failure urls
     * @param rawURL Raw url with out real protocol
     * @param servletRequest Servlet request has real protocol value
     * @return the dynamic replacement of the protocol
     * from the Success or Failure urls
     */
    private String processDynamicVariables(
        String rawURL,
        HttpServletRequest servletRequest) {
        if (rawURL.contains("%")) {
            int index;
            StringBuilder sb = new StringBuilder(200);
            // protocol processing
            if ((index = rawURL.indexOf("%protocol")) != -1) {
                sb.append(rawURL.substring(0,index));
                if (isConsoleRemote) {
                    sb.append(consoleProto);
                } else {
                    String protocol = null;
                    if ( servletRequest != null ) {
                        protocol = RequestUtils.getRedirectProtocol(
                        servletRequest.getScheme(),
                        servletRequest.getServerName());
                    }
                    if ( protocol != null ) {
                        sb.append(protocol);
                    } else {
                        sb.append(consoleProto);
                    }
                }
                sb.append(rawURL.substring(index+"%protocol".length()));
                rawURL = sb.toString();
            }
            if ((index = rawURL.indexOf("%host")) != -1) {
                int hostlen = "%host".length();
                sb.delete(0, 200);
                sb.append(rawURL.substring(0,index));
                if (isConsoleRemote) {
                    sb.append(consoleHost);
                } else {
                    String host = null;
                    if ( servletRequest != null ) {
                        host = servletRequest.getHeader("Host");
                    }
                    if(host != null) {
                        sb.append(host);
                        //This is to remove extra ":"
                        hostlen = hostlen+1;
                    } else {
                        sb.append(consoleHost);
                    }
                }
                sb.append(rawURL.substring(index + hostlen));
                rawURL  = sb.toString();
            }
            if ((index =rawURL.indexOf("%port")) != -1) {
                sb.delete(0, 200);
                sb.append(rawURL.substring(0,index));
                if (isConsoleRemote) {
                    sb.append(consolePort);
                }
                sb.append(rawURL.substring(index +"%port".length()));
                rawURL = sb.toString();
            }
        }
        return rawURL;
    }
    
    /**
     * Sets the Servlet Context.
     *
     * @param servletContext Servlet Context to be set.
     */
    public void setServletContext(ServletContext servletContext) {
        this.servletContext = servletContext;
        if (debug.messageEnabled()) {
            debug.message("Setting servletContext" + servletContext);
        }
    }
    
    /**
     * Returns the Servlet Context.
     *
     * @return Servlet Context.
     */
    public ServletContext getServletContext() {
        return servletContext;
    }
    
    /**
     * Returns the OpenAM Identity Repository for an organization.
     *
     * @param orgDN name of the organization
     * @return OpenAM Identity Repository.
     */
    public AMIdentityRepository getAMIdentityRepository(String orgDN) {
        AMIdentityRepository amIdentityRepository = null;
        try {
            amIdentityRepository = idRepoMap.get(orgDN);
            if (amIdentityRepository == null) {
                amIdentityRepository = new AMIdentityRepository(ssoAuthSession, orgDN);
                AMIdentityRepository winner = idRepoMap.putIfAbsent(orgDN, amIdentityRepository);
                if (winner != null) {
                    // We lost the race
                    amIdentityRepository = winner;
                }
            }
        } catch (Exception id) {
            if (debug.messageEnabled()) {
                debug.message("Error getAMIdentityRepository",id);
            }
        }
        return amIdentityRepository;
    }
    
    /**
     * Returns the Organization Configuration Manager for an organization.
     *
     * @param orgDN Name of the organization.
     * @return Organization Configuration Manager for an organization.
     */
    public OrganizationConfigManager getOrgConfigManager(String orgDN) {
        OrganizationConfigManager orgConfigMgr = null;
        try {
            orgConfigMgr = orgMap.get(orgDN);
            if (orgConfigMgr == null) {
                orgConfigMgr = new OrganizationConfigManager(ssoAuthSession, orgDN);
                OrganizationConfigManager winner = orgMap.putIfAbsent(orgDN, orgConfigMgr);
                if (winner != null) {
                    // We lost the race
                    orgConfigMgr = winner;
                }
            }
        } catch (Exception id) {
            if (debug.messageEnabled()) {
                debug.message("Error getAMIdentityRepository",id);
            }
        }
        return orgConfigMgr;
    }
    
    
    /**
     * Returns the <code>AMIdentity</code> object for the given parameters.
     * If there is no such identity, or there is more then one matching identity,
     * then an AuthException will be thrown.
     *
     * @param idType Identity Type.
     * @param idName Identity Name.
     * @param orgName organization name.
     * @return <code>AMIdentity</code> object.
     * @throws AuthException if there was no result, or if there was more results
     * then one.
     */
    public AMIdentity getIdentity(IdType idType,String idName,String orgName)
            throws AuthException {
        if (debug.messageEnabled()) {
            debug.message("IdType is :" +idType);
            debug.message("IdName is :" +idName);
            debug.message("orgName is :" +orgName);
        }
        AMIdentity amIdentity = null;

        // Try getting the identity using IdUtils.getIdentity(...)
        try {
            if (debug.messageEnabled()) {
                debug.message("AuthD.getIdentity() from IdUtils Name: " +
                    idName + " Org: " + orgName);
            }
            amIdentity = IdUtils.getIdentity(
                getSSOAuthSession(), idName, orgName);
            if ((amIdentity != null) && (amIdentity.isExists()) && 
                (amIdentity.getType().equals(idType)) && 
                (amIdentity.getAttributes() != null)) {
                if (debug.messageEnabled()) {
                    debug.message("AuthD.getIdentity obtained identity" +
                        "using IdUtil.getIdentity: " + amIdentity);
                }
                return (amIdentity);
            } 
        } catch (IdRepoException e) {
            // Ignore this exception and continue with search
            if (debug.messageEnabled()) {
                debug.message("AuthD.getIdentity: Got IdRepoException while " +
                    "getting Identity from IdUtils: "+e.getMessage());
            }
        } catch (SSOException ssoe) {
            // Ignore this exception and continue with search
            if (debug.messageEnabled()) {
                debug.message("AuthD.getIdentity: Got SSOException while " +
                    "getting Identity from IdUtils: "+ssoe.getMessage());
            }
        } 

        // Obtain AMIdentity object by searching within IdRepo
        try {
            amIdentity = null;
            idName = DNUtils.DNtoName(idName);
            AMIdentityRepository amIdRepo = getAMIdentityRepository(orgName);
            IdSearchControl idsc = new IdSearchControl();
            idsc.setRecursive(true);
            idsc.setTimeOut(0);
            idsc.setMaxResults(0);
            idsc.setAllReturnAttributes(false);
            IdSearchResults searchResults =
            amIdRepo.searchIdentities(idType,idName,idsc);
            Set results = Collections.EMPTY_SET;
            if (searchResults != null) {
                results = searchResults.getSearchResults();
            }
            
            if ((results != null) && (results.size() > 1)) {
                // multiple user match found, throw exception,
                // user need to login as super admin to fix it
                debug.error("getIdentity: Multiple matches found for " +
                "user '"+ idName);
                throw new AuthException(AMAuthErrorCode.AUTH_ERROR, null);
            }
            
            Iterator users = results.iterator();
            if (users.hasNext()) {
                amIdentity = (AMIdentity) users.next();
            }
        } catch (SSOException sso) {
            if (debug.messageEnabled()) {
                debug.message("getIdentity error " + sso.getMessage());
            }
        } catch (IdRepoException ide) {
            if (debug.messageEnabled()) {
                debug.message("IdRepoException error " + ide.getMessage());
            }
        }
        if (amIdentity == null) {
            throw new AuthException(AMAuthErrorCode.AUTH_PROFILE_ERROR, null);
        }
        
        return amIdentity;
    }

    /**
     * Returns the authentication service or chain configured for the
     * given organization.
     *
     * @param orgDN organization DN.
     * @return the authentication service or chain configured for the
     * given organization.
     */
    public String getOrgConfiguredAuthenticationChain(String orgDN) {
        String orgAuthConfig = null;
        try {
            OrganizationConfigManager orgConfigMgr =
                getOrgConfigManager(orgDN);
            ServiceConfig svcConfig =
                orgConfigMgr.getServiceConfig(
                    ISAuthConstants.AUTH_SERVICE_NAME);
            Map attrs = svcConfig.getAttributes();
            orgAuthConfig = Misc.getMapAttr(attrs,
                ISAuthConstants.AUTHCONFIG_ORG);
        } catch (Exception e) {
            debug.error("Error in getOrgConfiguredAuthenticationChain : ", e);
        }
        return orgAuthConfig;
    }

    /**
     * Checks whether an input URL is valid in an organization.
     *
     * @param url The URL to be validated.
     * @param orgDN The organization DN.
     * @return <code>true</code> if input URL is valid, <code>false</code> otherwise.
     */
    public boolean isGotoUrlValid(String url, String orgDN) {
        return REDIRECT_URL_VALIDATOR.isRedirectUrlValid(url, orgDN);
    }

    /**
     * Set of default URLs for login success
     */
    Set getDefaultSuccessURLSet() {
        return defaultSuccessURLSet;
    }

    /**
     * Current default URL for login success
     */
    String getDefaultSuccessURL() {
        return defaultSuccessURL;
    }

    void setDefaultSuccessURL(final String defaultSuccessURL) {
        this.defaultSuccessURL = defaultSuccessURL;
    }

    /**
     * Set of default URLs for login failure
     */
    Set getDefaultFailureURLSet() {
        return defaultFailureURLSet;
    }

    /**
     * Current default URLs for login failure
     */
    String getDefaultFailureURL() {
        return defaultFailureURL;
    }

    void setDefaultFailureURL(final String defaultFailureURL) {
        this.defaultFailureURL = defaultFailureURL;
    }

    /**
     * Set of default URLs for service success
     */
    Set getDefaultServiceSuccessURLSet() {
        return defaultServiceSuccessURLSet;
    }

    /**
     * Set of default URLs for service failure
     */
    Set getDefaultServiceFailureURLSet() {
        return defaultServiceFailureURLSet;
    }


    /**
     * Flag to force to use JAAS thread.
     * Default is false.
     */
    static boolean isEnforceJAASThread() {
        return enforceJAASThread;
    }
}
