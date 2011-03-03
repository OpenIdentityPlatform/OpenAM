/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: Constants.java,v 1.47 2009/08/12 23:10:44 ericow Exp $
 *
 */

/*
 * Portions Copyrighted 2010-2011 ForgeRock AS
 */

package com.sun.identity.shared;

/**
 * This interface contains all the property names defined in in
 * product configurations and may be expanded with other constant
 * values that are used for Access and Federation Manager development.
 */
public interface Constants {
    /**
     * Property string for debug level.
     */
    String SERVICES_DEBUG_LEVEL = "com.iplanet.services.debug.level";

    /**
     * Property string for debug file merge.
     */
    String SERVICES_DEBUG_MERGEALL = "com.sun.services.debug.mergeall";

    /**
     * property string for debug directory
     */
    String SERVICES_DEBUG_DIRECTORY = "com.iplanet.services.debug.directory";

    /**
     * Property string for interval of <code>Stats</code> service.
     */
    String AM_STATS_INTERVAL = "com.iplanet.am.stats.interval";

    /**
     *  property string representing set of invalid strings in a goto or target
     *  query parameter for a CDC Servlet
     */
    public static final String INVALID_GOTO_STRINGS =
    "com.iplanet.services.cdc.invalidGotoStrings";

    /**
     * Property string for state of <code>Stats</code> service.
     */
    String SERVICES_STATS_STATE = "com.iplanet.services.stats.state";

    /**
     * Property string for directory of <code>Stats</code> service.
     */
    String SERVICES_STATS_DIRECTORY = "com.iplanet.services.stats.directory";

    /**
     * Property string for SSL enabled.
     */
    String AM_DIRECTORY_SSL_ENABLED = "com.iplanet.am.directory.ssl.enabled";

    /**
     * Property string for directory host.
     */
    String AM_DIRECTORY_HOST = "com.iplanet.am.directory.host";

    /**
     * Property string for directory port.
     */
    String AM_DIRECTORY_PORT = "com.iplanet.am.directory.port";

    /**
     * Property string for server protocol.
     */
    String AM_SERVER_PROTOCOL = "com.iplanet.am.server.protocol";

    /**
     * Property string for server host.
     */
    String AM_SERVER_HOST = "com.iplanet.am.server.host";

    /**
     * Property string for server port.
     */
    String AM_SERVER_PORT = "com.iplanet.am.server.port";

    /**
     * Property string for Distributed Authentication server protocol.
     */
    String DISTAUTH_SERVER_PROTOCOL = "com.iplanet.distAuth.server.protocol";

    /**
     * Property string for Distributed Authentication server host.
     */
    String DISTAUTH_SERVER_HOST = "com.iplanet.distAuth.server.host";

    /**
     * Property string for Distributed Authentication server port.
     */
    String DISTAUTH_SERVER_PORT = "com.iplanet.distAuth.server.port";

    /**
     * Property string for console protocol.
     */
    String AM_CONSOLE_PROTOCOL = "com.iplanet.am.console.protocol";

    /**
     * Property string for console host.
     */
    String AM_CONSOLE_HOST = "com.iplanet.am.console.host";

    /**
     * Property string for console port.
     */
    String AM_CONSOLE_PORT = "com.iplanet.am.console.port";

    /**
     * Property string for naming URL.
     */
    String AM_NAMING_URL = "com.iplanet.am.naming.url";

    /**
     * Property string for client notification URL.
     */
    String CLIENT_NOTIFICATION_URL = "com.sun.identity.client.notification.url";

    /**
     * Property string for load balancer.
     */
    String AM_REDIRECT = "com.sun.identity.url.redirect";

    /**
     * Property string for cookie name.
     */
    String AM_COOKIE_NAME = "com.iplanet.am.cookie.name";

    /**
     * Property that determines whether to c66 encode session id 
     * to convert to cookie string. Value would be read as boolean.
     * Any value other than "true", case ignored, would be treated
     * as <code>false</code>. c66 encoding is opensso specific
     * url safe char66 encoding
     *
     * @see com.iplanet.dpro.session.SessionID#c66EncodeCookie()
     * @see com.iplanet.dpro.session.SessionID#c66EncodeSidString(java.lang.String)
     * @see com.iplanet.dpro.session.SessionID#c66DecodeCookieString(java.lang.String)
     */
    String C66_ENCODE_AM_COOKIE = "com.iplanet.am.cookie.c66Encode";

    /**
     * Property string for load balancer cookie name.
     */
    String AM_LB_COOKIE_NAME = "com.iplanet.am.lbcookie.name";

    /**
     * Property string for load balancer cookie value.
     */
    String AM_LB_COOKIE_VALUE = "com.iplanet.am.lbcookie.value";

    /**
     * Property string for secure cookie.
     */
    String AM_COOKIE_SECURE = "com.iplanet.am.cookie.secure";

    /**
     * Property string for cookie httponly flag.
     */
    String AM_COOKIE_HTTPONLY = "com.sun.identity.cookie.httponly";

    /**
     * Property string for cookie encoding.
     */
    String AM_COOKIE_ENCODE = "com.iplanet.am.cookie.encode";

    /**
     * Property string for <code>pcookie</code> name.
     */
    String AM_PCOOKIE_NAME = "com.iplanet.am.pcookie.name";

    /**
     * Property string for locale.
     */
    String AM_LOCALE = "com.iplanet.am.locale";

    /**
     * Property string for log status.
     */
    String AM_LOGSTATUS = "com.iplanet.am.logstatus";

    /**
     * Property string for version number.
     */
    String AM_VERSION = "com.iplanet.am.version";

    /**
     * Property string for <code>CertDB</code> directory.
     */
    String AM_ADMIN_CLI_CERTDB_DIR = "com.iplanet.am.admin.cli.certdb.dir";

    /**
     * Property string for SAML XML signature key store file.
     */
    String SAML_XMLSIG_KEYSTORE =
        "com.sun.identity.saml.xmlsig.keystore";

    /**
     * Property string for SAML XML signature key store password file.
     */
    String SAML_XMLSIG_STORE_PASS = 
        "com.sun.identity.saml.xmlsig.storepass";

    /**
     * Property string for SAML XML signature key password file.
     */
    String SAML_XMLSIG_KEYPASS = "com.sun.identity.saml.xmlsig.keypass";

    /**
     * Property string for SAML XML signature CERT alias.
     */
    String SAML_XMLSIG_CERT_ALIAS = "com.sun.identity.saml.xmlsig.certalias";

    /**
     * Property string for authentication super user.
     */
    String AUTHENTICATION_SUPER_USER = 
        "com.sun.identity.authentication.super.user";

    /**
     * Property string for authentication super user.
     */
    String AUTHENTICATION_SPECIAL_USERS = 
        "com.sun.identity.authentication.special.users";

    /**
     * Property string for installation directory
     */
    String AM_INSTALL_DIR = "com.iplanet.am.installdir";

    /**
     * Property string for new configuraton file in case of single war
     * deployment
     */
    String AM_NEW_CONFIGFILE_PATH = "com.sun.identity.configFilePath";

    /**
     * Property string for shared secret for application authentication module
     */
    String AM_SERVICES_SECRET = "com.iplanet.am.service.secret";

    /**
     * Property string for service deployment descriptor
     */
    String AM_SERVICES_DEPLOYMENT_DESCRIPTOR =
        "com.iplanet.am.services.deploymentDescriptor";

    /**
     * Property string for console deployment descriptor
     */
    String AM_CONSOLE_DEPLOYMENT_DESCRIPTOR =
        "com.iplanet.am.console.deploymentDescriptor";

    /**
     * property string which contains the name of HTTP session tracking cookie
     */
    String AM_SESSION_HTTP_SESSION_TRACKING_COOKIE_NAME = 
        "com.iplanet.am.session.failover.httpSessionTrackingCookieName";

    /**
     * property string to choose whether local or remote saving method is used
     */
    String AM_SESSION_FAILOVER_USE_REMOTE_SAVE_METHOD = 
        "com.iplanet.am.session.failover.useRemoteSaveMethod";

    /**
     * property string to choose whether we rely on app server load balancer to
     * do the request routing or use our own
     */
    String AM_SESSION_FAILOVER_USE_INTERNAL_REQUEST_ROUTING = 
        "com.iplanet.am.session.failover.useInternalRequestRouting";

    /**
     * Property string for failover cluster state check timeout
     */
    String AM_SESSION_FAILOVER_CLUSTER_STATE_CHECK_TIMEOUT = 
        "com.iplanet.am.session.failover.cluster.stateCheck.timeout";

    /**
     * Property string for failover cluster state check period
     */
    String AM_SESSION_FAILOVER_CLUSTER_STATE_CHECK_PERIOD = 
        "com.iplanet.am.session.failover.cluster.stateCheck.period";

    /**
     * Property string for max number of sessions
     */
    String AM_SESSION_MAX_SESSIONS = "com.iplanet.am.session.maxSessions";

    /**
     * Property string for security provider package.
     */
    String SECURITY_PROVIDER_PKG = "com.sun.identity.security.x509.pkg";

    /**
     * Property string for sun security provider package.
     */
    String SUN_SECURITY_PROVIDER_PKG = "com.sun.identity.security.x509.impl";

    /**
     * Property string for SMTP host.
     */
    String AM_SMTP_HOST = "com.iplanet.am.smtphost";

    /**
     * Property string for SMTP port.
     */
    String SM_SMTP_PORT = "com.iplanet.am.smtpport";

    /**
     * Property string for CDSSO cookie domain.
     */
    String SERVICES_CDSSO_COOKIE_DOMAIN = 
        "com.iplanet.services.cdsso.cookiedomain";

    /**
     * Property string for maximum content-length accepted in HttpRequest.
     */
    String SERVICES_COMM_SERVER_PLLREQUEST_MAX_CONTENT_LENGTH = 
            "com.iplanet.services.comm.server.pllrequest.maxContentLength";

    /**
     * Property string for encrypting class implementation.
     */
    String SECURITY_ENCRYPTOR = "com.iplanet.security.encryptor";

    /**
     * Property string for checking if console is remote.
     */
    String AM_CONSOLE_REMOTE = "com.iplanet.am.console.remote";

    /**
     * Property string for federation service cookie.
     */
    String FEDERATION_FED_COOKIE_NAME = 
        "com.sun.identity.federation.fedCookieName";

    /**
     * Property string for session notification thread pool size.
     */
    String NOTIFICATION_THREADPOOL_SIZE = 
        "com.iplanet.am.notification.threadpool.size";

    /**
     * Property string for name of the webcontainer.
     */
    String IDENTITY_WEB_CONTAINER = "com.sun.identity.webcontainer";

    /**
     * Property string for session notification thread pool queue size.
     */
    String NOTIFICATION_THREADPOOL_THRESHOLD = 
        "com.iplanet.am.notification.threadpool.threshold";

    /**
     * Property string for fully qualified host name map.
     */
    String AM_FQDN_MAP = "com.sun.identity.server.fqdnMap";

    /**
     * Client detection module content type property name.
     */
    String CDM_CONTENT_TYPE_PROPERTY_NAME = "contentType";

    /**
     * Default charset to be used in case the client detection has failed.
     */
    String CONSOLE_UI_DEFAULT_CHARSET = "UTF-8";

    /**
     * Attribute name of the user preferred locale located in amUser service.
     */
    String USER_LOCALE_ATTR = "preferredlocale";

    /**
     * Property string for checking if <code>HostLookUp</code> is enabled.
     */
    String ENABLE_HOST_LOOKUP = "com.sun.am.session.enableHostLookUp";

    /**
     * Property string for checking if <code>HostLookUp</code> is enabled.
     */
    String WEBCONTAINER = "com.sun.identity.webcontainer";

    /**
     * Property string for determining if cookie needs to be written in the URL
     * as a path info.
     */
    String REWRITE_AS_PATH = 
        "com.sun.identity.cookieRewritingInPath";

    /**
     * Property string for determining if session cookie needs to be appended
     * in the URL
     */
    String APPEND_SESS_COOKIE_IN_URL = 
        "com.sun.identity.appendSessionCookieInURL";

    /**
     * Property string for Application session max-caching-time.
     */
    String APPLICATION_SESSION_MAX_CACHING_TIME = 
        "com.sun.identity.session.application.maxCacheTime";

    /**
     * Property string to enable Session/Cookie hijacking mode in Access
     * Manager.
     */
    String IS_ENABLE_UNIQUE_COOKIE =
        "com.sun.identity.enableUniqueSSOTokenCookie";

    /**
     * Property string for 'HostUrl' Cookie name in Session/Cookie hijacking
     * mode.
     */
    String AUTH_UNIQUE_COOKIE_NAME = 
        "com.sun.identity.authentication.uniqueCookieName";

    /**
     * Property string for unique Cookie domain in Session/Cookie hijacking
     * mode.
     */
    String AUTH_UNIQUE_COOKIE_DOMAIN =
        "com.sun.identity.authentication.uniqueCookieDomain";

    /**
     * Property string for checking if remote method
     * <code>AddListenerOnAllSessions</code> is enabled.
     */
    String ENABLE_ADD_LISTENER_ON_ALL_SESSIONS =
        "com.sun.am.session.enableAddListenerOnAllSessions";

    /**
     * Property string for list of IP address of remote clients which are
     * considered trusted to forward the context used to check <code>restricted
     * token usage</code> is enabled.
     */
    String TRUSTED_SOURCE_LIST = "com.sun.am.session.trustedSourceList";

    /**
     * Property string to Identify the Http Header which returns the Client IP
     * address when running in loadbalancer configuration.
     */
    String HTTP_CLIENT_IP_HEADER = 
        "com.sun.identity.session.httpClientIPHeader";

    /**
     * Property string to ensure more stringent (security-wise) check If enabled
     * the <code>DN is converted to lowercase</code> for comparison.
     */
    String CASE_INSENSITIVE_DN = "com.sun.am.session.caseInsensitiveDN";

    /**
     * Property string to determine if validation is required when parsing XML
     * documents using OpenSSO XMLUtils class.
     */
    String XML_VALIDATING = "com.iplanet.am.util.xml.validating";

    /**
     * Property string to determine if authentication enforces using seperate
     * JAAS thread or not.
     */
    String ENFORCE_JAAS_THREAD = 
        "com.sun.identity.authentication.usingJaasThread";

    /**
     * Property string to list all the Session properties that should be
     * protected.
     */
    String PROTECTED_PROPERTIES_LIST = 
        "com.iplanet.am.session.protectedPropertiesList";

    /**
     * Property string to set max idle timeout for agent sessions
     */
    String AGENT_SESSION_IDLE_TIME =
        "com.iplanet.am.session.agentSessionIdleTime";

    /**
     * Property for Login URL.
     */
    String LOGIN_URL = "com.sun.identity.loginurl";

    /**
     * Property for checking the cookie support / cookie enabled in the browser
     */
    public static final String AM_COOKIE_CHECK =
        "com.sun.identity.am.cookie.check";

    /**
     * System property name that is a list of package name prefixes is used to
     * resolve protocol names into actual handler class names. 
     */
    String PROTOCOL_HANDLER = "opensso.protocol.handler.pkgs";

    /**
     * The package name prefix for JSSE based protocol implementations.
     */
    String JSSE_HANDLER = "com.sun.identity.protocol";

    /**
     * The package name prefix for JSS based protocol implementations.
     */
    String JSS_HANDLER = "com.iplanet.services.comm";

    /**
     * Property for passing the organization name when retrieving attribute
     * choice values.
     */
    String ORGANIZATION_NAME = "organization_name";

    /**
     * Organization name in Session/SSOToken Properties.
     */
    String ORGANIZATION = "Organization";

    /**
     * Property for auth cookie name.
     */
    String AM_AUTH_COOKIE_NAME = "com.sun.identity.auth.cookieName";

    /**
     * Unique Id set as a session property which is used for logging.
     */
    String AM_CTX_ID = "AMCtxId";

    /**
     * Global schema property name in Session Service.
     */
    String PROPERTY_CHANGE_NOTIFICATION = 
        "iplanet-am-session-property-change-notification";

    /**
     * Global schema property name in Session Service.
     */
    String NOTIFICATION_PROPERTY_LIST =
        "iplanet-am-session-notification-property-list";

    /**
     * The session property name of the universal identifier used for IDRepo.
     */
    String UNIVERSAL_IDENTIFIER = "sun.am.UniversalIdentifier";

    /**
     * Property string for session polling thread pool size.
     */
    String POLLING_THREADPOOL_SIZE =
        "com.sun.identity.session.polling.threadpool.size";

    /**
     * Property string for session polling thread pool queue size.
     */
    String POLLING_THREADPOOL_THRESHOLD = 
        "com.sun.identity.session.polling.threadpool.threshold";

    /**
     * Property for enabling or disabling encryption for Session Repository.
     */
    String SESSION_REPOSITORY_ENCRYPTION = 
        "com.sun.identity.session.repository.enableEncryption";

    /**
     * Property string for determining whether or not appplication sessions
     * should be returned via the getValidSessions() call.
     */
    String SESSION_RETURN_APP_SESSION = 
        "com.sun.identity.session.returnAppSession";

    /**
     * HTTP Form Parameter name used by PEP for posting policy advices to
     * OpenSSO.
     */
    String COMPOSITE_ADVICE = "sunamcompositeadvice";

    /**
     * XML tag name used for Advices message.
     */
    String ADVICES_TAG_NAME = "Advices";

    /**
     * Key that is used to identify the advice messages from
     * <code>AuthSchemeCondition</code>.
     */
    String AUTH_SCHEME_CONDITION_ADVICE = "AuthSchemeConditionAdvice";

    /** Key that is used to identify the advice messages from
     * <code>AuthLevelCondition</code>.
     */   
    String AUTH_LEVEL_CONDITION_ADVICE = "AuthLevelConditionAdvice";

    /**
     * Property string for determining whether server mode or client mode.
     */
    String SERVER_MODE = "com.iplanet.am.serverMode";

    /**
     * Property to determine the login URL.
     */
    String ATTR_LOGIN_URL = "iplanet-am-platform-login-url";

    /**
     * Property to determine the cookie domains.
     */
    String ATTR_COOKIE_DOMAINS = "iplanet-am-platform-cookie-domains";

    /**
     * Key name for platform server list in naming table.
     */
    String PLATFORM_LIST = "iplanet-am-platform-server-list";

    /**
     * Key name for site list in naming table.
     */
    String SITE_LIST = "iplanet-am-platform-site-list";

    /**
     * Key name for site ID list in naming table.
     */
    String SITE_ID_LIST = "iplanet-am-platform-site-id-list";

    /**
     * Key name for site ID list in naming table.
     */
    String CLUSTER_SERVER_LIST = "iplanet-am-session-cluster-serverlist";

    /**
     * This value is used by LDAP connection pool to reap connections
     * if they are idle for the number of seconds specified by the
     * value of this property.  If the value is set at 0, the connection
     * will not be reaped.
     */
    String LDAP_CONN_IDLE_TIME_IN_SECS =
        "com.sun.am.ldap.connnection.idle.seconds";

    /**
     *  Property string for Fallback Monitoring thread polling interval
     */
    String LDAP_FALLBACK_SLEEP_TIME_IN_MINS =
        "com.sun.am.ldap.fallback.sleep.minutes";

    /**
     * Constant for file separator.
     */
    String FILE_SEPARATOR = "/";

    /**
     * Install Time System property key.
     */
    String SYS_PROPERTY_INSTALL_TIME = "installTime";

    /**
     * This is a HTTP parameter to indicate to the authentication component
     * to either forward the request or redirect it after authentication
     * succeed.
     */
    String FORWARD_PARAM = "forwardrequest";
                                                                                
    /**
     * Value is for <code>FORWARD_PARAM</code> to indicate that the 
     * authentication component should forward request.
     */
    String FORWARD_YES_VALUE = "yes";
    
    /**
     * Attribute name for the load balancer cookie in the
     * Naming Response.
     */
    String NAMING_AM_LB_COOKIE = "am_load_balancer_cookie";

    /**
     * Property string for Site Monitoring thread polling interval
     */
    String MONITORING_INTERVAL = "com.sun.identity.sitemonitor.interval";

    /**
     * Property string for URL Checker Target URL
     */
    String URLCHECKER_TARGET_URL = "com.sun.identity.urlchecker.targeturl";

    /**
     * Property string for URL Checker Target URL
     */
    String URLCHECKER_INVALIDATE_INTERVAL =
        "com.sun.identity.urlchecker.invalidate.interval";

    /**
     * Property string for URL Checker Sleep Interval
     */
    String URLCHECKER_SLEEP_INTERVAL =
        "com.sun.identity.urlchecker.sleep.interval";

    /**
     *  Property string for URL Checker Retry Interval
     */
    public static final String URLCHECKER_RETRY_INTERVAL = 
                  "com.sun.identity.urlchecker.retry.interval";
    /**
     *  Property string for URL Checker Retry Limit
     */
    public static final String URLCHECKER_RETRY_LIMIT = 
                  "com.sun.identity.urlchecker.retry.limit";

    /**
     * Property string for Site Status Check Class name
     */
    String SITE_STATUS_CHECK_CLASS =
        "com.sun.identity.sitemonitor.SiteStatusCheck.class";

    /**
     * Property string for Site Status Check timeout
     */
    String MONITORING_TIMEOUT = "com.sun.identity.sitemonitor.timeout";

    /**
     * String identifying the prefix for all AM protected
     * properties. Even if a property is not defined in the
     * PROCTED_PROPERTIES_LIST but starts with this prefix
     * its considered protected.
     */
    String AM_PROTECTED_PROPERTY_PREFIX =
        "am.protected";

    /**
     * Property string to determine whether to set auth cookies to all
     * domains in the domain list.
     */
    public static final String SET_COOKIE_TO_ALL_DOMAINS =
            "com.sun.identity.authentication.setCookieToAllDomains";

    /**
     * Property Name for cache polling interval.
     */
    String CACHE_POLLING_TIME_PROPERTY = "com.sun.identity.sm.cacheTime";

    /**
     * Default cache polling interval (1 minute).
     */
    int DEFAULT_CACHE_POLLING_TIME = 1;

    /**
     * Key for SSOToken Object in envMap passed from SM
     */
    String SSO_TOKEN = "SSOToken";

    /**
     * Tag for server protocol.
     */
    String TAG_SERVER_PROTO = "%SERVER_PROTO%";

    /**
     * Tag for server host.
     */
    String TAG_SERVER_HOST = "%SERVER_HOST%";

    /**
     * Tag for server port.
     */
    String TAG_SERVER_PORT = "%SERVER_PORT%";

    /**
     * Tag for server deployment URI.
     */
    String TAG_SERVER_URI = "%SERVER_URI%";

    /**
     * Platform service name.
     */
    String SVC_NAME_PLATFORM = "iPlanetAMPlatformService";
    
    /**
     * LDAP server host name for saml2 crl cache
     */
    String CRL_CACHE_DIR_HOST =
        "com.sun.identity.crl.cache.directory.host";

    /**
     * LDAP server port number for saml2 crl cache
     */
    String CRL_CACHE_DIR_PORT =
        "com.sun.identity.crl.cache.directory.port";

    /**
     * LDAP server ssl config for saml2 crl cache
     */
    String CRL_CACHE_DIR_SSL_ENABLED =
        "com.sun.identity.crl.cache.directory.ssl";

    /**
     * LDAP Server bind user name for saml2 crl cache
     */
    String CRL_CACHE_DIR_USER =
        "com.sun.identity.crl.cache.directory.user";

    /**
     * LDAP Server bind password for saml2 crl cache
     */
    String CRL_CACHE_DIR_PASSWD =
        "com.sun.identity.crl.cache.directory.password";

    /**
     * LDAP Server search base dn for saml2 crl cache
     */
    String CRL_CACHE_DIR_SEARCH_LOC =
        "com.sun.identity.crl.cache.directory.searchlocs";

    /**
     * LDAP attribute name for searching crl entry
     */
    String CRL_CACHE_DIR_SEARCH_ATTR =
        "com.sun.identity.crl.cache.directory.searchattr";        

    /**
     * Naming service name.
     */
    String SVC_NAME_NAMING = "iPlanetAMNamingService";

    /**
     * Certificate Alias name for SSL Client Auth 
     */
    String CLIENT_CERTIFICATE_ALIAS =
        "com.sun.identity.security.keyStore.clientAlias";        

    /**
     * User service name.
     */
    String SVC_NAME_USER = "iPlanetAMUserService";

    /**
     * Authentication Configuration service name.
     */
    String SVC_NAME_AUTH_CONFIG = "iPlanetAMAuthConfiguration";

    /**
     * SAML service name.
     */
    String SVC_NAME_SAML = "iPlanetAMSAMLService";

    /**
     * Certificate Alias name for SSL Client Auth 
     */
    String URL_CONNECTION_USE_CACHE =
        "com.sun.identity.urlconnection.useCache";        
        
    /**
     * Property string for distauth deployment descriptor
     */
    String AM_DISTAUTH_DEPLOYMENT_DESCRIPTOR =
        "com.iplanet.am.distauth.deploymentDescriptor";        
       
    /**
     * Property string for cdc servlet login url
     */
    String CDCSERVLET_LOGIN_URL =
        "com.sun.identity.cdcservlet.loginurl";        

    /**
     * Property name for data encryption key
     */
    String ENC_PWD_PROPERTY = "am.encryption.pwd";

    /**
     * Property string for load balancer cookie value.
     */
    String PROPERTY_NAME_LB_COOKIE_VALUE =
        "com.iplanet.am.lbcookie.value";

    /**
     * Key name for serverid-cookievalue list in naming table.
     */
    String SERVERID_LBCOOKIEVALUE_LIST =
        "iplanet-am-platform-lb-cookie-value-list";
        
    /**
     * Configuration Variable for distauth bootstrap file base directory.
     */
    String CONFIG_VAR_DISTAUTH_BOOTSTRAP_BASE_DIR = "FAMDistAuth";

    /**
     * Configuration Variable for distauth bootstrap file name.
     */
    String CONFIG_VAR_DISTAUTH_BOOTSTRAP_FILENAME = "AMDistAuthConfig.properties";
    
    /**
     * property string for enabling SMS datastore notification
     */
    public static final String SMS_ENABLE_DB_NOTIFICATION =
        "com.sun.identity.sm.enableDataStoreNotification";

    /**
     * property string for controlling SMS, AMSDK & IdRepo cache
     */
    static String SDK_GLOBAL_CACHE_PROPERTY =
        "com.iplanet.am.sdk.caching.enabled";

    /**
     * property string for controlling SMS cache.
     * Active only if "com.iplanet.am.sdk.caching.enabled" is "false"
     */
    static String SMS_CACHE_PROPERTY = "com.sun.identity.sm.cache.enabled";
    
    /**
     * property string to enable SMS cache expiry time.
     */
    static String SMS_CACHE_TTL_ENABLE = "com.sun.identity.sm.cache.ttl.enable";
    
    /**
     * property string for controlling SMS cache expiry time, in minutes.
     * The default values is 30 minutes. After the expiry time the next access
     * to the object will fetched from the backend datastore.
     */
    static String SMS_CACHE_TTL = "com.sun.identity.sm.cache.ttl";
    
    /**
     * property string to manage the persistent connection to directory
     */
    static final String EVENT_LISTENER_DISABLE_LIST =
        "com.sun.am.event.connection.disable.list";
    
    /**
     *  property string to cache past event changes in minutes
     */
    static final String EVENT_LISTENER_REMOTE_CLIENT_BACKLOG_CACHE =
        "com.sun.am.event.notification.expire.time";
                
    /**
     * Global schema property name in Session Service.
     * constant used for session trimming when purge delay > 0 
     */
    static final String ENABLE_TRIM_SESSION = 
        "iplanet-am-session-enable-session-trimming";        

    /**
      * property string to the size of SystemTimerPool
      */
    public static final String SYSTEM_TIMERPOOL_SIZE =
        "com.sun.identity.common.systemtimerpool.size";

    /**
     * property string for Distributed Authentication cluster
     */
    public static final String DISTAUTH_CLUSTER = 
        "com.sun.identity.distauth.cluster";

    /**
     * property string for Krb5LoginModule class name
     */
    public static final String KRB5_LOGINMODULE = 
     "com.sun.identity.authentication.module.WindowsDesktopSSO.Krb5LoginModule";
    public static final String DEFAULT_KRB5_LOGINMODULE = 
        "com.sun.security.auth.module.Krb5LoginModule";
    public static final String KRB5_CREDENTIAL_TYPE =
        "com.sun.identity.authentication.module.WindowsDesktopSSO.credsType";

    /**
     * property to control whether remote auth includes request/response
     */
    public static final String REMOTEAUTH_INCLUDE_REQRES =
        "openam.remoteauth.include.reqres";

    /**
     * property to control if the OpenAM session cookie should be made
     * persistent
     */
    public static final String PERSIST_AM_COOKIE =
        "openam.session.persist_am_cookie";

    /**
     * property to control if the OpenAM server will persist the OpenAM
     * session cookie if the following parameter is in the incoming request
     * <code>PersistAMCookie</code>.
     */
    public static final String ALLOW_PERSIST_AM_COOKIE =
        "openam.session.allow_persist_am_cookie";

    /**
     * Server configuration property for the OpenDS admin port
     */
    public static final String DS_ADMIN_PORT =
        "org.forgerock.embedded.dsadminport";

    /**
     * OpenDS Replication Port
     */
    public static final String EMBED_REPL_PORT =
        "com.sun.embedded.replicationport";

    /**
     * OpenDS Replication Port
     */
    public static final String EMBED_SYNC_SERVERS =
        "com.sun.embedded.sync.servers";

    /**
     * Configuration property to enable the site monitor in the naming service
     */
    public static final String SITEMONITOR_DISABLED =
        "openam.naming.sitemonitor.disabled";

    /**
     * EQUALS sign
     */
    public static final String EQUALS = "=";

    /**
     * semi-colon sign
     */
    public static final String SEMI_COLON = ";";

    /**
     * colon sign
     */
    public static final String COLON = ":";

    /**
     * amp sign
     */
    public static final String AMP = "&";

    /**
     * at sign
     */
    public static final String AT = "@";

    /**
     * empty string
     */
    public static final String EMPTY = "";
     
    /**
     * empty string
     */
    public static final String DELIMITER_PREF_LEFT =
        "openam.entitlement.delimiter.precedence.left";

    public static final String USE_OLD_LOG_FORMAT =
        "openam.logging.use.old.log.format";

    public static final String SESSION_UPGRADER_IMPL =
        "openam.auth.session_property_upgrader";

    public static final String DEFAULT_SESSION_UPGRADER_IMPL =
        "org.forgerock.openam.authentication.service.DefaultSessionPropertyUpgrader";

    /**
     * Property for dist auth cookie name.
     */
    public static final String AM_DIST_AUTH_COOKIE_NAME =
        "openam.auth.distAuthCookieName";

    public static final String DESTROY_SESSION_AFTER_UPGRADE =
        "openam.auth.destroy_session_after_upgrade";
    
    public static final String AUTH_RATE_MONITORING_INTERVAL =
        "openam.auth.rate_monitoring_interval";

    public static final String DESTROY_ALL_SESSIONS =
        "openam.session.destroy_all_sessions";

    public static final String RETAINED_HTTP_HEADERS_LIST =
            "openam.retained.http.headers";

    public static final String FORBIDDEN_TO_COPY_HEADERS =
            "openam.forbidden.to.copy.headers";

    public static final String IGNORE_GOTO_DURING_LOGOUT =
            "openam.authentication.ignore_goto_during_logout";

    public static final String AM_DISTAUTH_LB_COOKIE_NAME =
            "openam.auth.distauth.lb_cookie_name";

    public static final String AM_DISTAUTH_LB_COOKIE_VALUE =
            "openam.auth.distauth.lb_cookie_value";
}
