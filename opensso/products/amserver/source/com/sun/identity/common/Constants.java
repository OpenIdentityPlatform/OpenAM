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
 * $Id: Constants.java,v 1.18 2009/10/23 18:51:24 bhavnab Exp $
 *
 */

package com.sun.identity.common;

/**
 * This interface contains all the property names defined in in
 * <code>AMConfig.properties</code> and may be expanded with other constant
 * values that are used for OpenSSO server development.
 *
 * @deprecated As of OpenSSO version 8.0
 *             {@link com.sun.identity.shared.Constants}
 */
public interface Constants {
    /**
     * property string for debug level
     */
    public static final String SERVICES_DEBUG_LEVEL = 
        "com.iplanet.services.debug.level";

    /**
     * property string for debug directory
     */
    public static final String SERVICES_DEBUG_DIRECTORY = 
        "com.iplanet.services.debug.directory";

    /**
     * property string for interval of <code>Stats</code> service
     */
    public static final String AM_STATS_INTERVAL = 
        "com.iplanet.am.stats.interval";

    /**
     * property string for state of <code>Stats</code> service
     */
    public static final String SERVICES_STATS_STATE =
        "com.iplanet.services.stats.state";

    /**
     * property string for directory of <code>Stats</code> service
     */
    public static final String SERVICES_STATS_DIRECTORY = 
        "com.iplanet.services.stats.directory";

    /**
     * property string for SDK caching size
     */
    public static final String AM_SDK_CACHE_MAXSIZE = 
        "com.iplanet.am.sdk.cache.maxSize";

    /**
     * property string for module for processing user handling
     */
    public static final String AM_SDK_USER_ENTRY_PROCESSING_IMPL = 
        "com.iplanet.am.sdk.userEntryProcessingImpl";

    /**
     * property string for SSL enabled
     */
    public static final String AM_DIRECTORY_SSL_ENABLED = 
        "com.iplanet.am.directory.ssl.enabled";

    /**
     * property string for directory host
     */
    public static final String AM_DIRECTORY_HOST = 
        "com.iplanet.am.directory.host";

    /**
     * property string for directory port
     */
    public static final String AM_DIRECTORY_PORT = 
        "com.iplanet.am.directory.port";

    /**
     * property string for server protocol
     */
    public static final String AM_SERVER_PROTOCOL = 
        "com.iplanet.am.server.protocol";

    /**
     * property string for server host
     */
    public static final String AM_SERVER_HOST = 
        "com.iplanet.am.server.host";

    /**
     * property string for server port
     */
    public static final String AM_SERVER_PORT = 
        "com.iplanet.am.server.port";

    /**
     * property string for Distributed Authentication server protocol
     */
    public static final String DISTAUTH_SERVER_PROTOCOL = 
        "com.iplanet.distAuth.server.protocol";

    /**
     * property string for Distributed Authentication server host
     */
    public static final String DISTAUTH_SERVER_HOST = 
        "com.iplanet.distAuth.server.host";

    /**
     * property string for Distributed Authentication server port
     */
    public static final String DISTAUTH_SERVER_PORT = 
        "com.iplanet.distAuth.server.port";

    /**
     * property string for console protocol
     */
    public static final String AM_CONSOLE_PROTOCOL = 
        "com.iplanet.am.console.protocol";

    /**
     * property string for console host
     */
    public static final String AM_CONSOLE_HOST = 
        "com.iplanet.am.console.host";

    /**
     * property string for console port
     */
    public static final String AM_CONSOLE_PORT = 
        "com.iplanet.am.console.port";

    /**
     * property string for profile host
     */
    public static final String AM_PROFILE_HOST = 
        "com.iplanet.am.profile.host";

    /**
     * property string for profile port
     */
    public static final String AM_PROFILE_PORT = 
        "com.iplanet.am.profile.port";

    /**
     * property string for naming URL
     */
    public static final String AM_NAMING_URL = 
        "com.iplanet.am.naming.url";

    /**
     * property string for notification URL
     */
    public static final String AM_NOTIFICATION_URL = 
        "com.iplanet.am.notification.url";

    /**
     * property string for load balancer
     */
    public static final String AM_REDIRECT = 
        "com.sun.identity.url.redirect";

    /**
     * property string for daemon process
     */
    public static final String AM_DAEMONS = 
        "com.iplanet.am.daemons";

    /**
     * property string for cookie name
     */
    public static final String AM_COOKIE_NAME = 
        "com.iplanet.am.cookie.name";

    /**
     * property string for LB cookie name
     */
    public static final String AM_LB_COOKIE_NAME = 
        "com.iplanet.am.lbcookie.name";

    /**
     * property string for LB cookie value
     */
    public static final String AM_LB_COOKIE_VALUE = 
        "com.iplanet.am.lbcookie.value";

    /**
     * property string for secure cookie
     */
    public static final String AM_COOKIE_SECURE = 
        "com.iplanet.am.cookie.secure";

    /**
     *  URL Query parameter to indicate whether to persist AM session cookie,
     *  only value that would enable persistence is "true". This works along
     *  with the property <code>AM_COOKIE_TIME_TO_LIVE</code>. The expires time
     *  of cookie is set only when the value of this property is "true"
     *  and value of <code>AM_COOKIE_TIME_TO_LIVE<code> is a positive integer
     */
     public static final String PERSIST_AM_COOKIE = "PersistAMCookie";
     
     /**
      *  property string for time to live of AM cookie, in minutes.
      * If authentication was initiated with query parameter,
      * <code>PERSIST_AM_COOKIE</code>=true, maxAge of AM session
      * cookie is set to this value converted to seconds
      */
      public static final String AM_COOKIE_TIME_TO_LIVE
      		= "com.iplanet.am.cookie.timeToLive";

    /**
     * property string for cookie encoding
     */
    public static final String AM_COOKIE_ENCODE = 
        "com.iplanet.am.cookie.encode";

    /**
     * property string for <code>pcookie</code> name
     */
    public static final String AM_PCOOKIE_NAME = 
        "com.iplanet.am.pcookie.name";

    /**
     * property string for locale
     */
    public static final String AM_LOCALE = 
        "com.iplanet.am.locale";

    /**
     * property string for log status
     */
    public static final String AM_LOGSTATUS = 
        "com.iplanet.am.logstatus";

    /**
     * property string for domain component
     */
    public static final String AM_DOMAIN_COMPONENT = 
        "com.iplanet.am.domaincomponent";

    /**
     * property string for version number
     */
    public static final String AM_VERSION = "com.iplanet.am.version";

    /**
     * property string for <code>CertDB</code> directory
     */
    public static final String AM_ADMIN_CLI_CERTDB_DIR = 
        "com.iplanet.am.admin.cli.certdb.dir";

    /**
     * property string for <code>CertDB</code> prefix
     */
    public static final String AM_ADMIN_CLI_CERTDB_PREFIX = 
        "com.iplanet.am.admin.cli.certdb.prefix";

    /**
     * property string for <code>CertDB</code> password file
     */
    public static final String AM_ADMIN_CLI_CERTDB_PASSFILE = 
        "com.iplanet.am.admin.cli.certdb.passfile";

    /**
     * property string for OCSP responder URL
     */
    public static final String AUTHENTICATION_OCSP_RESPONDER_URL = 
        "com.sun.identity.authentication.ocsp.responder.url";

    /**
     * property string for OCSP responder nickname
     */
    public static final String AUTHENTICATION_OCSP_RESPONDER_NICKNAME = 
        "com.sun.identity.authentication.ocsp.responder.nickname";

    /**
     * property string for SAML XML signature key store file
     */
    public static final String SAML_XMLSIG_KEYSTORE =
        "com.sun.identity.saml.xmlsig.keystore";

    /**
     * property string for SAML XML signature key store password file
     */
    public static final String SAML_XMLSIG_STORE_PASS = 
        "com.sun.identity.saml.xmlsig.storepass";

    /**
     * property string for SAML XML signature key password file
     */
    public static final String SAML_XMLSIG_KEYPASS = 
        "com.sun.identity.saml.xmlsig.keypass";

    /**
     * property string for SAML XML signature CERT alias
     */
    public static final String SAML_XMLSIG_CERT_ALIAS = 
        "com.sun.identity.saml.xmlsig.certalias";

    /**
     * property string for authentication super user
     */
    public static final String AUTHENTICATION_SUPER_USER = 
        "com.sun.identity.authentication.super.user";

    /**
     * property string for authentication super user
     */
    public static final String AUTHENTICATION_SPECIAL_USERS = 
        "com.sun.identity.authentication.special.users";

    /**
     * property string for replica retry number
     */
    public static final String AM_REPLICA_NUM_RETRIES = 
        "com.iplanet.am.replica.num.retries";

    /**
     * property string for delay between replica retries
     */
    public static final String AM_REPLICA_DELAY_BETWEEN_RETRIES =
        "com.iplanet.am.replica.delay.between.retries";

    /**
     * property string for retry number for event connection
     */
    public static final String AM_EVENT_CONNECTION_NUM_RETRIES = 
        "com.iplanet.am.event.connection.num.retries";

    /**
     * property string for delay time between retries for event connection
     */
    public static final String AM_EVENT_CONNECTION_DELAY_BETWEEN_RETRIES = 
        "com.iplanet.am.event.connection.delay.between.retries";

    /**
     * property string for <code>LDAPException</code> error codes that retries
     * will happen for event connection
     */
    public static final String AM_EVENT_CONNECCTION_LDAP_ERROR_CODES_RETRIES =
        "com.iplanet.am.event.connection.ldap.error.codes.retries";

    /**
     * property string for number of time to retry for LDAP connection
     */
    public static final String AM_LDAP_CONNECTION_NUM_RETRIES = 
        "com.iplanet.am.ldap.connection.num.retries";

    /**
     * property string for delay time between retries for LDAP connection
     */
    public static final String AM_LDAP_CONNECTION_DELAY_BETWEEN_RETRIES = 
        "com.iplanet.am.ldap.connection.delay.between.retries";

    /**
     * property string for <code>LDAPException</code> error codes that retries
     * will happen for LDAP connection
     */
    public static final String AM_LDAP_CONNECTION_LDAP_ERROR_CODES_RETRIES = 
        "com.iplanet.am.ldap.connection.ldap.error.codes.retries";

    /**
     * property string for new configuraton file in case of single war
     * deployment
     */
    public static final String AM_NEW_CONFIGFILE_PATH = 
        "com.sun.identity.configFilePath";

    /**
     * property string for shared secret for application authentication module
     */
    public static final String AM_SERVICES_SECRET = 
        "com.iplanet.am.service.secret";

    /**
     * property string for service deployment descriptor
     */
    public static final String AM_SERVICES_DEPLOYMENT_DESCRIPTOR =
        "com.iplanet.am.services.deploymentDescriptor";

    /**
     * property string for console deployment descriptor
     */
    public static final String AM_CONSOLE_DEPLOYMENT_DESCRIPTOR =
        "com.iplanet.am.console.deploymentDescriptor";

    /**
     * property string for agent URL deployment descriptor
     */
    public static final String AM_POLICY_AGENTS_URL_DEPLOYMENT_DESCRIPTOR =
        "com.iplanet.am.policy.agents.url.deploymentDescriptor";

    /**
     * property string which contains the name of HTTP session tracking cookie
     */
    public static final String AM_SESSION_HTTP_SESSION_TRACKING_COOKIE_NAME = 
        "com.iplanet.am.session.failover.httpSessionTrackingCookieName";

    /**
     * property string to choose whether local or remote saving method is used
     */
    public static final String AM_SESSION_FAILOVER_USE_REMOTE_SAVE_METHOD = 
        "com.iplanet.am.session.failover.useRemoteSaveMethod";

    /**
     * property string to choose whether we rely on app server load balancer to
     * do the request routing or use our own
     */
    public static final String 
        AM_SESSION_FAILOVER_USE_INTERNAL_REQUEST_ROUTING = 
            "com.iplanet.am.session.failover.useInternalRequestRouting";

    /**
     * property string for failover cluster state check timeout
     */
    public static final String AM_SESSION_FAILOVER_CLUSTER_SERVER_LIST =
        "com.iplanet.am.session.failover.cluster.serverList";

    /**
     * property string for failover cluster state check timeout
     */
    public static final String 
        AM_SESSION_FAILOVER_CLUSTER_STATE_CHECK_TIMEOUT = 
            "com.iplanet.am.session.failover.cluster.stateCheck.timeout";

    /**
     * property string for failover cluster state check period
     */
    public static final String AM_SESSION_FAILOVER_CLUSTER_STATE_CHECK_PERIOD = 
        "com.iplanet.am.session.failover.cluster.stateCheck.period";

    /**
     * property string for naming <code>failover</code> URL
     */
    public static final String AM_NAMING_FAILOVER_URL = 
        "com.iplanet.am.naming.failover.url";

    /**
     * property string for max number of sessions
     */
    public static final String AM_SESSION_MAX_SESSIONS = 
        "com.iplanet.am.session.maxSessions";

    /**
     * property string for checking if HTTP session is enabled
     */
    public static final String AM_SESSION_HTTP_SESSION_ENABLED = 
        "com.iplanet.am.session.httpSession.enabled";

    /**
     * property string for max session time for invalid session
     */
    public static final String AM_SESSION_INVALID_SESSION_MAXTIME = 
        "com.iplanet.am.session.invalidsessionmaxtime";

    /**
     * property string for checking if session client polling is enabled
     */
    public static final String AM_SESSION_CLIENT_POLLING_ENABLED = 
        "com.iplanet.am.session.client.polling.enable";

    /**
     * property string for session client polling period
     */
    public static final String AM_SESSION_CLIENT_POLLING_PERIOD = 
        "com.iplanet.am.session.client.polling.period";

    /**
     * property string for security provider package
     */
    public static final String SECURITY_PROVIDER_PKG = 
        "com.sun.identity.security.x509.pkg";
    
    /**
     * property string for iplanet security provider package
     */
    public static final String IPLANET_SECURITY_PROVIDER_PKG = 
        "com.iplanet.security.x509.impl";

    /**
     * property string for sun security provider package
     */
    public static final String SUN_SECURITY_PROVIDER_PKG = 
        "com.sun.identity.security.x509.impl";

    /**
     * property string for UNIX helper port
     */
    public static final String UNIX_HELPER_PORT = 
        "unixHelper.port";

    /**
     * property string for <code>securid</code> authentication module and
     * helper port
     */
    public static final String SECURID_HELPER_PORTS = "securidHelper.ports";

    /**
     * property string for SMTP host
     */
    public static final String AM_SMTP_HOST = "com.iplanet.am.smtphost";

    /**
     * property string for SMTP port
     */
    public static final String SM_SMTP_PORT = "com.iplanet.am.smtpport";

    /**
     * property string for CDSSO URL
     */
    public static final String SERVICES_CDSSO_CDCURL =
        "com.iplanet.services.cdsso.CDCURL";

    /**
     * property string for CDSSO cookie domain
     */
    public static final String SERVICES_CDSSO_COOKIE_DOMAIN = 
        "com.iplanet.services.cdsso.cookiedomain";

    /**
     * property string for CDC auth login url
     */
    public static final String SERVICES_CDC_AUTH_LOGIN_URL = 
        "com.iplanet.services.cdc.authLoginUrl";

    /**
     * property string for maximum content-length accepted in HttpRequest
     */
    public static final String 
        SERVICES_COMM_SERVER_PLLREQUEST_MAX_CONTENT_LENGTH = 
            "com.iplanet.services.comm.server.pllrequest.maxContentLength";

    /**
     * property string for factory class name for
     * <code>SecureRandomFactory</code>
     */
    public static final String SECURITY_SECURE_RANDOM_FACTORY_IMPL = 
        "com.iplanet.security.SecureRandomFactoryImpl";

    /**
     * property string for factory class name for <code>LDAPSocketFactory</code>
     */
    public static final String SECURITY_SSL_SOCKET_FACTORY_IMPL = 
        "com.iplanet.security.SSLSocketFactoryImpl";

    /**
     * property string for encrypting class implementation
     */
    public static final String SECURITY_ENCRYPTOR = 
        "com.iplanet.security.encryptor";

    /**
     * property string for checking if console is remote
     */
    public static final String AM_CONSOLE_REMOTE = 
        "com.iplanet.am.console.remote";

    /**
     * property string for checking if client IP check is enabled
     */
    public static final String AM_CLIENT_IP_CHECK_ENABLED =
        "com.iplanet.am.clientIPCheckEnabled";

    /**
     * property string for user name for application authentication module
     */
    public static final String AGENTS_APP_USER_NAME = 
        "com.sun.identity.agents.app.username";

    /**
     * property string for log file name for logging messages
     */
    public static final String AGENTS_SERVER_LOG_FILE_NAME = 
        "com.sun.identity.agents.server.log.file.name";

    /**
     * property string for resource result cache size
     */
    public static final String AGENTS_CACHE_SIZE = 
        "com.sun.identity.agents.cache.size";

    /**
     * property string for agent polling interval
     */
    public static final String AGENTS_POLLING_INTERVAL =
        "com.sun.identity.agents.polling.interval";

    /**
     * property string for checking if agent notification is enabled
     */
    public static final String AGENTS_NOTIFICATION_ENABLED = 
        "com.sun.identity.agents.notification.enabled";

    /**
     * property string for agent notification URL
     */
    public static final String AGENTS_NOTIFICATION_URL = 
        "com.sun.identity.agents.notification.url";

    /**
     * property string for checking whether to use wildcard for resource name
     * comparison
     */
    public static final String AGENTS_USE_WILDCARD = 
        "com.sun.identity.agents.use.wildcard";

    /**
     * property string for attributes to be returned by policy evaluator
     */
    public static final String AGENTS_HEADER_ATRIBUTES = 
        "com.sun.identity.agents.header.attributes";

    /**
     * property string for resource comparator class name
     */
    public static final String AGENTS_RESOURCE_COMPARATOR_CLASS = 
        "com.sun.identity.agents.resource.comparator.class";

    /**
     * property string for resource comparator class name
     */
    public static final String AGENTS_RESOURCE_WILDCARD = 
        "com.sun.identity.agents.resource.wildcard";

    /**
     * property string for resource name's delimiter
     */
    public static final String AGENTS_RESOURCE_DELIMITER = 
        "com.sun.identity.agents.resource.delimiter";

    /**
     * property string for indicator if case sensitive is on during policy
     * evaluation
     */
    public static final String AGENTS_RESOURCE_CASE_SENSITIVE = 
        "com.sun.identity.agents.resource.caseSensitive";

    /**
     * property string for true value of policy action
     */
    public static final String AGENTS_TRUE_VALUE = 
        "com.sun.identity.agents.true.value";

    /**
     * property string for federation service cookie
     */
    public static final String FEDERATION_FED_COOKIE_NAME = 
        "com.sun.identity.federation.fedCookieName";

    /**
     * property string for federation signing on indicator
     */
    public static final String FEDERATION_SERVICES_SIGNING_ON = 
        "com.sun.identity.federation.services.signingOn";

    /**
     * property string for session notification thread pool size
     */
    public static final String NOTIFICATION_THREADPOOL_SIZE = 
        "com.iplanet.am.notification.threadpool.size";

    /**
     * property string for name of the webcontainer
     */
    public static final String IDENTITY_WEB_CONTAINER = 
        "com.sun.identity.webcontainer";

    /**
     * property string for session notification thread pool queue size
     */
    public static final String NOTIFICATION_THREADPOOL_THRESHOLD = 
        "com.iplanet.am.notification.threadpool.threshold";

    /**
     * Property String for fully qualified host name map.
     */
    String AM_FQDN_MAP = "com.sun.identity.server.fqdnMap";

    /**
     * Client detection module content type property name
     */
    public static final String CDM_CONTENT_TYPE_PROPERTY_NAME = "contentType";

    /**
     * Default charset to be used in case the client detection has failed.
     */
    public static final String CONSOLE_UI_DEFAULT_CHARSET = "UTF-8";

    /**
     * Attribute name of the user preferred locale located in amUser service.
     */
    public static final String USER_LOCALE_ATTR = "preferredlocale";

    /**
     * Property string for checking if <code>HostLookUp</code> is enabled.
     */
    public static final String ENABLE_HOST_LOOKUP = 
        "com.sun.am.session.enableHostLookUp";

    /**
     * Property string for checking if <code>HostLookUp</code> is enabled.
     */
    public static final String WEBCONTAINER = "com.sun.identity.webcontainer";

    /**
     * Property string for determining if cookie needs to be written in the URL
     * as a path info.
     */
    public static final String REWRITE_AS_PATH = 
        "com.sun.identity.cookieRewritingInPath";

    /**
     * Property string for Application session max-caching-time.
     */
    public static final String APPLICATION_SESSION_MAX_CACHING_TIME = 
        "com.sun.identity.session.application.maxCacheTime";

    /**
     * Property string to enable Session/Cookie hijacking mode in OpenSSO
     * Enterprise.
     */
    public static final String IS_ENABLE_UNIQUE_COOKIE = 
        "com.sun.identity.enableUniqueSSOTokenCookie";

    /**
     * Property string for 'HostUrl' Cookie name in Session/Cookie hijacking
     * mode.
     */
    public static final String AUTH_UNIQUE_COOKIE_NAME = 
        "com.sun.identity.authentication.uniqueCookieName";

    /**
     * Property string for unique Cookie domain in Session/Cookie hijacking
     * mode.
     */
    public static final String AUTH_UNIQUE_COOKIE_DOMAIN =
        "com.sun.identity.authentication.uniqueCookieDomain";

    /**
     * Property string for checking if remote method
     * <code>AddListenerOnAllSessions</code> is enabled
     */
    public static final String ENABLE_ADD_LISTENER_ON_ALL_SESSIONS =
        "com.sun.am.session.enableAddListenerOnAllSessions";

    /**
     * Property string for list of IP address of remote clients which are
     * considered trusted to forward the context used to check <code>restricted
     * token usage</code>
     * is enabled
     */
    public static final String TRUSTED_SOURCE_LIST = 
        "com.sun.am.session.trustedSourceList";

    /**
     * Property string to Identify the Http Header which returns the Client IP
     * address when running in loadbalancer configuration.
     */
    public static final String HTTP_CLIENT_IP_HEADER = 
        "com.sun.identity.session.httpClientIPHeader";

    /**
     * User object type.
     */
    public static final String OBJECT_TYPE_USER = "user";

    /**
     * Agent object type.
     */
    public static final String OBJECT_TYPE_AGENT = "Agent";

    /**
     * Property string to ensure more stringent (security-wise) check If enabled
     * the <code>DN is converted to lowercase</code> for comparison
     */
    public static final String CASE_INSENSITIVE_DN = 
        "com.sun.am.session.caseInsensitiveDN";

    /**
     * Property string to determine if validation is required when parsing XML
     * documents using OpenSSO XMLUtils class.
     */
    public static final String XML_VALIDATING = 
        "com.iplanet.am.util.xml.validating";

    /**
     * Property string to determine if authentication enforces using seperate
     * JAAS thread or not
     */
    public static final String ENFORCE_JAAS_THREAD = 
        "com.sun.identity.authentication.usingJaasThread";

    /**
     * Property string to list all the Session properties that should be
     * protected.
     */
    public static final String PROTECTED_PROPERTIES_LIST = 
        "com.iplanet.am.session.protectedPropertiesList";

    /**
     * Property for Login URL.
     */
    public static final String LOGIN_URL = "com.sun.identity.loginurl";

    /**
     * Property for Protocol Handler.
     */
    public static final String PROTOCOL_HANDLER = "opensso.protocol.handler.pkgs";

    public static final String JSS_HANDLER = "com.iplanet.services.comm";

    public static final String JSSE_HANDLER = "com.sun.identity.protocol";

    /**
     * Property for passing the organization name when retrieving attribute
     * choice values
     */
    public static final String ORGANIZATION_NAME = "organization_name";

    /**
     * Organization name in Session/SSOToken Properties
     * 
     */
    public static final String ORGANIZATION = "Organization";

    /**
     * Property for auth cookie name.
     */
    public static final String AM_AUTH_COOKIE_NAME = 
        "com.sun.identity.auth.cookieName";

    /**
     * Unique Id set as a session property which is used for logging.
     */
    public static final String AM_CTX_ID = "AMCtxId";

    /**
     * Global schema property name in Session Service.
     */
    public static final String PROPERTY_CHANGE_NOTIFICATION = 
        "iplanet-am-session-property-change-notification";

    /**
     * Global schema property name in Session Service.
     */
    public static final String NOTIFICATION_PROPERTY_LIST =
        "iplanet-am-session-notification-property-list";

    /**
     * The session property name of the universal identifier used for IDRepo.
     */
    public static final String UNIVERSAL_IDENTIFIER = 
        "sun.am.UniversalIdentifier";

    /**
     * property string for session polling thread pool size
     */
    public static final String POLLING_THREADPOOL_SIZE =
        "com.sun.identity.session.polling.threadpool.size";

    /**
     * property string for session polling thread pool queue size
     */
    public static final String POLLING_THREADPOOL_THRESHOLD = 
        "com.sun.identity.session.polling.threadpool.threshold";

    /**
     * Property for enabling or disabling encryption for Session Repository.
     */
    public static final String SESSION_REPOSITORY_ENCRYPTION = 
        "com.sun.identity.session.repository.enableEncryption";

    /**
     * property string for determining whether or not appplication sessions
     * should be returned via the getValidSessions() call.
     */
    public static final String SESSION_RETURN_APP_SESSION = 
        "com.sun.identity.session.returnAppSession";

    /**
     * HTTP Form Parameter name used by PEP for posting policy advices to AM
     */
    public static final String COMPOSITE_ADVICE = "sunamcompositeadvice";

    /**
     * XML tag name used for Advices message
     */
    public static final String ADVICES_TAG_NAME = "Advices";

    /**
     * Key that is used to identify the advice messages from
     * <code>AuthSchemeCondition</code>
     */
    public static final String AUTH_SCHEME_CONDITION_ADVICE =
        "AuthSchemeConditionAdvice";

    /** Key that is used to identify the advice messages from
     * <code>AuthLevelCondition</code>.
     */   
    public static final String AUTH_LEVEL_CONDITION_ADVICE =
        "AuthLevelConditionAdvice";

    /**
     * property string for determining whether server mode or client mode
     */
    public static final String SERVER_MODE = "com.iplanet.am.serverMode";

    /**
     * Key name for platform server list in naming table
     
    public static final String PLATFORM_LIST = 
        "iplanet-am-platform-server-list";

    /**
     * Key name for site list in naming table
     
    public static final String SITE_LIST = "iplanet-am-platform-site-list";
    */

    /**
     * Key name for site ID list in naming table
     */
    public static final String SITE_ID_LIST = 
        "iplanet-am-platform-site-id-list";

    /**
     * Key name for site ID list in naming table
     */
    public static final String CLUSTER_SERVER_LIST = 
        "iplanet-am-session-cluster-serverlist";

    /**
     * This value is used by LDAP connection pool to reap connections
     * if they are idle for the number of seconds specified by the
     * value of this property.  If the value is set at 0, the connection
     * will not be reaped.
     */
    public static final String LDAP_CONN_IDLE_TIME_IN_SECS =
        "com.sun.am.ldap.connnection.idle.seconds";

    /**
     * Constant for file separator
     */
    String FILE_SEPARATOR = "/";

    /**
     * Install Time System property key.
     */
    String SYS_PROPERTY_INSTALL_TIME = "installTime";

    /**
     * Property string for sm and um notification thread pool size
     */
    String SM_THREADPOOL_SIZE =
        "com.sun.identity.sm.notification.threadpool.size";

    /**
     * Key to indicate if the customer is performing auths via mutiple tabs
     * of the same browser.
     */
    public static final String MULTIPLE_TABS_USED =
        "com.sun.identity.authentication.multiple.tabs.used";
    /**
     * Property string for client IP address header.
     */
    String CLIENT_IP_ADDR_HEADER = 
        "com.sun.identity.authentication.client.ipAddressHeader";
}
