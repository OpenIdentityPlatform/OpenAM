/*
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
 * Portions Copyrighted 2010-2016 ForgeRock AS.
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
    static final String SERVICES_DEBUG_LEVEL = "com.iplanet.services.debug.level";

    /**
     * Property string for debug file merge.
     */
    static final String SERVICES_DEBUG_MERGEALL = "com.sun.services.debug.mergeall";

    /**
     * property string for debug directory
     */
    static final String SERVICES_DEBUG_DIRECTORY = "com.iplanet.services.debug.directory";

    /**
     * Property string for interval of <code>Stats</code> service.
     */
    static final String AM_STATS_INTERVAL = "com.iplanet.am.stats.interval";

    /**
     * property string representing set of invalid strings in a goto or target
     * query parameter for a CDC Servlet
     */
    static final String INVALID_GOTO_STRINGS =
            "com.iplanet.services.cdc.invalidGotoStrings";

    /**
     * Property string representing set of valid strings in a loginURI query parameter for a CDC Servlet. Values are
     * delmited by "," (comma) character.
     */
    static final String VALID_LOGIN_URIS = "org.forgerock.openam.cdc.validLoginURIs";

    /**
     * Property string for state of <code>Stats</code> service.
     */
    static final String SERVICES_STATS_STATE = "com.iplanet.services.stats.state";

    /**
     * Property string for directory of <code>Stats</code> service.
     */
    static final String SERVICES_STATS_DIRECTORY = "com.iplanet.services.stats.directory";

    /**
     * Property string for SSL enabled.
     */
    static final String AM_DIRECTORY_SSL_ENABLED = "com.iplanet.am.directory.ssl.enabled";

    /**
     * Property string for directory host.
     */
    static final String AM_DIRECTORY_HOST = "com.iplanet.am.directory.host";

    /**
     * Property string for directory port.
     */
    static final String AM_DIRECTORY_PORT = "com.iplanet.am.directory.port";

    /**
     * Property string for server protocol.
     */
    static final String AM_SERVER_PROTOCOL = "com.iplanet.am.server.protocol";

    /**
     * Property string for server host.
     */
    static final String AM_SERVER_HOST = "com.iplanet.am.server.host";

    /**
     * Property string for server port.
     */
    static final String AM_SERVER_PORT = "com.iplanet.am.server.port";

    /**
     * Property string for Distributed Authentication server protocol.
     */
    static final String DISTAUTH_SERVER_PROTOCOL = "com.iplanet.distAuth.server.protocol";

    /**
     * Property string for Distributed Authentication server host.
     */
    static final String DISTAUTH_SERVER_HOST = "com.iplanet.distAuth.server.host";

    /**
     * Property string for Distributed Authentication server port.
     */
    static final String DISTAUTH_SERVER_PORT = "com.iplanet.distAuth.server.port";

    /**
     * Property string for console protocol.
     */
    static final String AM_CONSOLE_PROTOCOL = "com.iplanet.am.console.protocol";

    /**
     * Property string for console host.
     */
    static final String AM_CONSOLE_HOST = "com.iplanet.am.console.host";

    /**
     * Property string for console port.
     */
    static final String AM_CONSOLE_PORT = "com.iplanet.am.console.port";

    /**
     * Property string for naming URL.
     */
    static final String AM_NAMING_URL = "com.iplanet.am.naming.url";

    /**
     * Property string for client notification URL.
     */
    static final String CLIENT_NOTIFICATION_URL = "com.sun.identity.client.notification.url";

    /**
     * Property string for load balancer.
     */
    static final String AM_REDIRECT = "com.sun.identity.url.redirect";

    /**
     * Property string for cookie name.
     */
    static final String AM_COOKIE_NAME = "com.iplanet.am.cookie.name";

    /**
     *  property string for time to live of AM cookie, in minutes.
     * If authentication was initiated with query parameter,
     * <code>PERSIST_AM_COOKIE</code>=true, maxAge of AM session
     * cookie is set to this value converted to seconds
     */
    static final String AM_COOKIE_TIME_TO_LIVE
            = "com.iplanet.am.cookie.timeToLive";

    /**
     * Property that determines whether to c66 encode session id
     * to convert to cookie string. Value would be read as boolean.
     * Any value other than "true", case ignored, would be treated
     * as <code>false</code>. c66 encoding is opensso specific
     * url safe char66 encoding
     *
     * @see <code>com.iplanet.dpro.session.SessionID#c66EncodeCookie()</code>
     * @see <code>com.iplanet.dpro.session.SessionID#c66EncodeSidString(java.lang.String)</code>
     * @see <code>com.iplanet.dpro.session.SessionID#c66DecodeCookieString(java.lang.String)</code>
     */
    static final String C66_ENCODE_AM_COOKIE = "com.iplanet.am.cookie.c66Encode";

    /**
     * Property string for load balancer cookie name.
     */
    static final String AM_LB_COOKIE_NAME = "com.iplanet.am.lbcookie.name";

    /**
     * Property string for load balancer cookie value.
     */
    static final String AM_LB_COOKIE_VALUE = "com.iplanet.am.lbcookie.value";

    /**
     * Property string for secure cookie.
     */
    static final String AM_COOKIE_SECURE = "com.iplanet.am.cookie.secure";

    /**
     * Property string for cookie httponly flag.
     */
    static final String AM_COOKIE_HTTPONLY = "com.sun.identity.cookie.httponly";

    /**
     * Property string for cookie encoding.
     */
    static final String AM_COOKIE_ENCODE = "com.iplanet.am.cookie.encode";

    /**
     * Property string for <code>pcookie</code> name.
     */
    static final String AM_PCOOKIE_NAME = "com.iplanet.am.pcookie.name";

    /**
     * Property string for locale.
     */
    static final String AM_LOCALE = "com.iplanet.am.locale";

    /**
     * Property string for log status.
     */
    static final String AM_LOGSTATUS = "com.iplanet.am.logstatus";

    /**
     * Property string for version number.
     */
    static final String AM_VERSION = "com.iplanet.am.version";

    /**
     * Property string for build version number.
     */
    static final String AM_BUILD_VERSION = "com.iplanet.am.buildVersion";

    /**
     * Property string for build revision number.
     */
    static final String AM_BUILD_REVISION = "com.iplanet.am.buildRevision";

    /**
     * Property string for build date.
     */
    static final String AM_BUILD_DATE = "com.iplanet.am.buildDate";

    /**
     * Property string for <code>CertDB</code> directory.
     */
    static final String AM_ADMIN_CLI_CERTDB_DIR = "com.iplanet.am.admin.cli.certdb.dir";

    /**
     * Property string for SAML XML signature key store file.
     */
    static final String SAML_XMLSIG_KEYSTORE =
            "com.sun.identity.saml.xmlsig.keystore";

    /**
     * Property string for SAML XML signature key store password file.
     */
    static final String SAML_XMLSIG_STORE_PASS =
            "com.sun.identity.saml.xmlsig.storepass";

    /**
     * Property string for SAML XML signature key password file.
     */
    static final String SAML_XMLSIG_KEYPASS = "com.sun.identity.saml.xmlsig.keypass";

    /**
     * Property string for SAML XML signature CERT alias.
     */
    static final String SAML_XMLSIG_CERT_ALIAS = "com.sun.identity.saml.xmlsig.certalias";

    /**
     * Property string for authentication super user.
     */
    static final String AUTHENTICATION_SUPER_USER =
            "com.sun.identity.authentication.super.user";

    /**
     * Property string for authentication super user.
     */
    static final String AUTHENTICATION_SPECIAL_USERS =
            "com.sun.identity.authentication.special.users";

    /**
     * Property string for installation directory
     */
    static final String AM_INSTALL_DIR = "com.iplanet.am.installdir";

    /**
     * Property string for new configuraton file in case of single war
     * deployment
     */
    static final String AM_NEW_CONFIGFILE_PATH = "com.sun.identity.configFilePath";

    /**
     * Property string for shared secret for application authentication module
     */
    static final String AM_SERVICES_SECRET = "com.iplanet.am.service.secret";

    /**
     * Property string for service deployment descriptor
     */
    static final String AM_SERVICES_DEPLOYMENT_DESCRIPTOR =
            "com.iplanet.am.services.deploymentDescriptor";

    /**
     * Property string for console deployment descriptor
     */
    static final String AM_CONSOLE_DEPLOYMENT_DESCRIPTOR =
            "com.iplanet.am.console.deploymentDescriptor";

    /**
     * property string which contains the name of HTTP session tracking cookie
     */
    static final String AM_SESSION_HTTP_SESSION_TRACKING_COOKIE_NAME =
            "com.iplanet.am.session.failover.httpSessionTrackingCookieName";

    /**
     * Property string for failover cluster state check timeout
     */
    static final String AM_SESSION_FAILOVER_CLUSTER_STATE_CHECK_TIMEOUT =
            "com.iplanet.am.session.failover.cluster.stateCheck.timeout";

    /**
     * Property string for failover cluster state check period
     */
    static final String AM_SESSION_FAILOVER_CLUSTER_STATE_CHECK_PERIOD =
            "com.iplanet.am.session.failover.cluster.stateCheck.period";

    static final String AM_SESSION_ENABLE_SESSION_CONSTRAINT =
            "iplanet-am-session-enable-session-constraint";

    static final String AM_SESSION_DENY_LOGIN_IF_DB_IS_DOWN =
            "iplanet-am-session-deny-login-if-db-is-down";

    static final String AM_SESSION_CONSTRAINT_HANDLER =
            "iplanet-am-session-constraint-handler";

    static final String AM_SESSION_SESSION_LIST_RETRIEVAL_TIMEOUT =
            "iplanet-am-session-session-list-retrieval-timeout";

    static final String AM_SESSION_MAX_SESSION_LIST_SIZE =
            "iplanet-am-session-max-session-list-size";

    static final String AM_SESSION_CONSTRAINT_MAX_WAIT_TIME =
            "iplanet-am-session-constraint-max-wait-time";

    /**
     * Property name for maximum size of the internal session cache.
     */
    String AM_SESSION_MAX_CACHE_SIZE = "org.forgerock.openam.session.service.access.persistence.caching.maxsize";

    /**
     * Property string for security provider package.
     */
    static final String SECURITY_PROVIDER_PKG = "com.sun.identity.security.x509.pkg";

    /**
     * Property string for sun security provider package.
     */
    static final String SUN_SECURITY_PROVIDER_PKG = "com.sun.identity.security.x509.impl";

    /**
     * Property string for SMTP host.
     */
    static final String AM_SMTP_HOST = "com.iplanet.am.smtphost";

    /**
     * Property string for SMTP port.
     */
    static final String SM_SMTP_PORT = "com.iplanet.am.smtpport";

    /**
     * Property string for CDSSO cookie domain.
     */
    static final String SERVICES_CDSSO_COOKIE_DOMAIN =
            "com.iplanet.services.cdsso.cookiedomain";

    /**
     * Property string for maximum content-length accepted in HttpRequest.
     */
    static final String SERVICES_COMM_SERVER_PLLREQUEST_MAX_CONTENT_LENGTH =
            "com.iplanet.services.comm.server.pllrequest.maxContentLength";

    /**
     * Property string for encrypting class implementation.
     */
    static final String SECURITY_ENCRYPTOR = "com.iplanet.security.encryptor";

    /**
     * Property string for checking if console is remote.
     */
    static final String AM_CONSOLE_REMOTE = "com.iplanet.am.console.remote";

    /**
     * Property string for federation service cookie.
     */
    static final String FEDERATION_FED_COOKIE_NAME =
            "com.sun.identity.federation.fedCookieName";

    /**
     * Property string for session notification thread pool size.
     */
    static final String NOTIFICATION_THREADPOOL_SIZE =
            "com.iplanet.am.notification.threadpool.size";

    /**
     * Property string for name of the webcontainer.
     */
    static final String IDENTITY_WEB_CONTAINER = "com.sun.identity.webcontainer";

    /**
     * Property string for session notification thread pool queue size.
     */
    static final String NOTIFICATION_THREADPOOL_THRESHOLD =
            "com.iplanet.am.notification.threadpool.threshold";

    /**
     * Property string for fully qualified host name map.
     */
    static final String AM_FQDN_MAP = "com.sun.identity.server.fqdnMap";

    /**
     * Client detection module content type property name.
     */
    static final String CDM_CONTENT_TYPE_PROPERTY_NAME = "contentType";

    /**
     * Default charset to be used in case the client detection has failed.
     */
    static final String CONSOLE_UI_DEFAULT_CHARSET = "UTF-8";

    /**
     * Attribute name of the user preferred locale located in amUser service.
     */
    static final String USER_LOCALE_ATTR = "preferredlocale";

    /**
     * Property string for checking if <code>HostLookUp</code> is enabled.
     */
    static final String ENABLE_HOST_LOOKUP = "com.sun.am.session.enableHostLookUp";

    /**
     * Property string for checking if <code>HostLookUp</code> is enabled.
     */
    static final String WEBCONTAINER = "com.sun.identity.webcontainer";

    /**
     * Property string for determining if cookie needs to be written in the URL
     * as a path info.
     */
    static final String REWRITE_AS_PATH =
            "com.sun.identity.cookieRewritingInPath";

    /**
     * Property string for determining if session cookie needs to be appended
     * in the URL
     */
    static final String APPEND_SESS_COOKIE_IN_URL =
            "com.sun.identity.appendSessionCookieInURL";

    /**
     * Property string for Application session max-caching-time.
     */
    static final String APPLICATION_SESSION_MAX_CACHING_TIME =
            "com.sun.identity.session.application.maxCacheTime";

    /**
     * Property string to enable Session/Cookie hijacking mode in Access
     * Manager.
     */
    static final String IS_ENABLE_UNIQUE_COOKIE =
            "com.sun.identity.enableUniqueSSOTokenCookie";

    /**
     * Property string for 'HostUrl' Cookie name in Session/Cookie hijacking
     * mode.
     */
    static final String AUTH_UNIQUE_COOKIE_NAME =
            "com.sun.identity.authentication.uniqueCookieName";

    /**
     * Property string for unique Cookie domain in Session/Cookie hijacking
     * mode.
     */
    static final String AUTH_UNIQUE_COOKIE_DOMAIN =
            "com.sun.identity.authentication.uniqueCookieDomain";

    /**
     * Property string for list of IP address of remote clients which are
     * considered trusted to forward the context used to check <code>restricted
     * token usage</code> is enabled.
     */
    static final String TRUSTED_SOURCE_LIST = "com.sun.am.session.trustedSourceList";

    /**
     * Property string to ensure more stringent (security-wise) check If enabled
     * the <code>DN is converted to lowercase</code> for comparison.
     */
    static final String CASE_INSENSITIVE_DN = "com.sun.am.session.caseInsensitiveDN";

    /**
     * Property string to determine if validation is required when parsing XML
     * documents using OpenAM XMLUtils class.
     */
    static final String XML_VALIDATING = "com.iplanet.am.util.xml.validating";

    /**
     * Property string to determine if authentication enforces using seperate
     * JAAS thread or not.
     */
    static final String ENFORCE_JAAS_THREAD =
            "com.sun.identity.authentication.usingJaasThread";

    /**
     * Property string to list all the Session properties that should be
     * protected.
     */
    static final String PROTECTED_PROPERTIES_LIST =
            "com.iplanet.am.session.protectedPropertiesList";

    /**
     * Property string to set max idle timeout for agent sessions
     */
    static final String AGENT_SESSION_IDLE_TIME =
            "com.iplanet.am.session.agentSessionIdleTime";

    /**
     * Property for Login URL.
     */
    static final String LOGIN_URL = "com.sun.identity.loginurl";

    /**
     * Property for checking the cookie support / cookie enabled in the browser
     */
    static final String AM_COOKIE_CHECK =
            "com.sun.identity.am.cookie.check";

    /**
     * System property name that is a list of package name prefixes is used to
     * resolve protocol names into actual handler class names.
     */
    static final String PROTOCOL_HANDLER = "opensso.protocol.handler.pkgs";

    /**
     * The package name prefix for JSSE based protocol implementations.
     */
    static final String JSSE_HANDLER = "com.sun.identity.protocol";

    /**
     * The package name prefix for JSS based protocol implementations.
     */
    static final String JSS_HANDLER = "com.iplanet.services.comm";

    /**
     * Property for passing the organization name when retrieving attribute
     * choice values.
     */
    static final String ORGANIZATION_NAME = "organization_name";

    /**
     * Organization name in Session/SSOToken Properties.
     */
    static final String ORGANIZATION = "Organization";

    /**
     * Property for auth cookie name.
     */
    static final String AM_AUTH_COOKIE_NAME = "com.sun.identity.auth.cookieName";

    /**
     * Unique Id set as a session property which is used for logging.
     */
    static final String AM_CTX_ID = "AMCtxId";

    /**
     * Global schema property name in Session Service.
     */
    static final String PROPERTY_CHANGE_NOTIFICATION =
            "iplanet-am-session-property-change-notification";

    /**
     * Global schema property name in Session Service.
     */
    static final String NOTIFICATION_PROPERTY_LIST =
            "iplanet-am-session-notification-property-list";

    static final String TIMEOUT_HANDLER_LIST =
            "openam-session-timeout-handler-list";

    /**
     * The session property name of the universal identifier used for IDRepo.
     */
    static final String UNIVERSAL_IDENTIFIER = "sun.am.UniversalIdentifier";

    /**
     * Property string for session polling thread pool size.
     */
    static final String POLLING_THREADPOOL_SIZE =
            "com.sun.identity.session.polling.threadpool.size";

    /**
     * Property string for session polling thread pool queue size.
     */
    static final String POLLING_THREADPOOL_THRESHOLD =
            "com.sun.identity.session.polling.threadpool.threshold";

    /**
     * Property for enabling or disabling encryption for Session Repository.
     */
    static final String SESSION_REPOSITORY_ENCRYPTION =
            "com.sun.identity.session.repository.enableEncryption";

    /**
     * Sessions that are stored in a compressed state will take less storage space and replicate quicker.
     */
    static final String SESSION_REPOSITORY_COMPRESSION =
            "com.sun.identity.session.repository.enableCompression";

    /**
     * Additional compression option for Session Tokens.
     */
    static final String SESSION_REPOSITORY_ATTRIBUTE_NAME_COMPRESSION =
            "com.sun.identity.session.repository.enableAttributeCompression";

    /**
     * Property string for determining whether or not appplication sessions
     * should be returned via the getValidSessions() call.
     */
    static final String SESSION_RETURN_APP_SESSION =
            "com.sun.identity.session.returnAppSession";

    /**
     * HTTP Form Parameter name used by PEP for posting policy advices to
     * OpenAM.
     */
    static final String COMPOSITE_ADVICE = "sunamcompositeadvice";

    /**
     * XML tag name used for Advices message.
     */
    static final String ADVICES_TAG_NAME = "Advices";

    /**
     * Key that is used to identify the advice messages from
     * <code>AuthSchemeCondition</code>.
     */
    static final String AUTH_SCHEME_CONDITION_ADVICE = "AuthSchemeConditionAdvice";

    /**
     * Key that is used to identify the advice messages from
     * <code>AuthLevelCondition</code>.
     */
    static final String AUTH_LEVEL_CONDITION_ADVICE = "AuthLevelConditionAdvice";

    /**
     * Property string for determining whether server mode or client mode.
     */
    static final String SERVER_MODE = "com.iplanet.am.serverMode";

    /**
     * Property to determine the login URL.
     */
    static final String ATTR_LOGIN_URL = "iplanet-am-platform-login-url";

    /**
     * Property to determine the cookie domains.
     */
    static final String ATTR_COOKIE_DOMAINS = "iplanet-am-platform-cookie-domains";

    /**
     * Key name for platform server list in naming table.
     */
    static final String PLATFORM_LIST = "iplanet-am-platform-server-list";

    /**
     * Key name for site list in naming table.
     */
    static final String SITE_LIST = "iplanet-am-platform-site-list";

    /**
     * Key name for site ID list in naming table.
     */
    static final String SITE_ID_LIST = "iplanet-am-platform-site-id-list";

    /**
     * Key name for site ID list in naming table.
     */
    static final String CLUSTER_SERVER_LIST = "iplanet-am-session-cluster-serverlist";

    /**
     * This value is used by LDAP connection pool to reap connections
     * if they are idle for the number of seconds specified by the
     * value of this property.  If the value is set at 0, the connection
     * will not be reaped.
     */
    static final String LDAP_CONN_IDLE_TIME_IN_SECS =
            "com.sun.am.ldap.connnection.idle.seconds";

    /**
     * Property string for Fallback Monitoring thread polling interval
     */
    static final String LDAP_FALLBACK_SLEEP_TIME_IN_MINS =
            "com.sun.am.ldap.fallback.sleep.minutes";

    /**
     * Install Time System property key.
     */
    static final String SYS_PROPERTY_INSTALL_TIME = "installTime";

    /**
     * This is a HTTP parameter to indicate to the authentication component
     * to either forward the request or redirect it after authentication
     * succeed.
     */
    static final String FORWARD_PARAM = "forwardrequest";

    /**
     * Value is for <code>FORWARD_PARAM</code> to indicate that the
     * authentication component should forward request.
     */
    static final String FORWARD_YES_VALUE = "yes";

    /**
     * Attribute name for the load balancer cookie in the
     * Naming Response.
     */
    static final String NAMING_AM_LB_COOKIE = "am_load_balancer_cookie";

    /**
     * Property string for Site Monitoring thread polling interval
     */
    static final String MONITORING_INTERVAL = "com.sun.identity.sitemonitor.interval";

    /**
     * Property string for URL Checker Target URL
     */
    static final String URLCHECKER_TARGET_URL = "com.sun.identity.urlchecker.targeturl";

    /**
     * Configuration property to enable the GET request for ClusterState
     * OPENAM-255
     */
    static final String URLCHECKER_DOREQUEST = "com.sun.identity.urlchecker.dorequest";

    /**
     * Property string for URL Checker Target URL
     */
    static final String URLCHECKER_INVALIDATE_INTERVAL =
            "com.sun.identity.urlchecker.invalidate.interval";

    /**
     * Property string for URL Checker Sleep Interval
     */
    static final String URLCHECKER_SLEEP_INTERVAL =
            "com.sun.identity.urlchecker.sleep.interval";

    /**
     * Property string for URL Checker Retry Interval
     */
    static final String URLCHECKER_RETRY_INTERVAL =
            "com.sun.identity.urlchecker.retry.interval";
    /**
     * Property string for URL Checker Retry Limit
     */
    static final String URLCHECKER_RETRY_LIMIT =
            "com.sun.identity.urlchecker.retry.limit";

    /**
     * Property string for Site Status Check Class name
     */
    static final String SITE_STATUS_CHECK_CLASS =
            "com.sun.identity.sitemonitor.SiteStatusCheck.class";

    /**
     * Property string for Site Status Check timeout
     */
    static final String MONITORING_TIMEOUT = "com.sun.identity.sitemonitor.timeout";

    /**
     * String identifying the prefix for all AM protected
     * properties. Even if a property is not defined in the
     * PROCTED_PROPERTIES_LIST but starts with this prefix
     * its considered protected.
     */
    static final String AM_PROTECTED_PROPERTY_PREFIX =
            "am.protected";

    /**
     * Property string to determine whether to set auth cookies to all
     * domains in the domain list.
     */
    static final String SET_COOKIE_TO_ALL_DOMAINS =
            "com.sun.identity.authentication.setCookieToAllDomains";

    /**
     * Property Name for cache polling interval.
     */
    static final String CACHE_POLLING_TIME_PROPERTY = "com.sun.identity.sm.cacheTime";

    /**
     * Default cache polling interval (1 minute).
     */
    int DEFAULT_CACHE_POLLING_TIME = 1;

    /**
     * Key for SSOToken Object in envMap passed from SM
     */
    static final String SSO_TOKEN = "SSOToken";

    /**
     * Tag for server protocol.
     */
    static final String TAG_SERVER_PROTO = "%SERVER_PROTO%";

    /**
     * Tag for server host.
     */
    static final String TAG_SERVER_HOST = "%SERVER_HOST%";

    /**
     * Tag for server port.
     */
    static final String TAG_SERVER_PORT = "%SERVER_PORT%";

    /**
     * Tag for server deployment URI.
     */
    static final String TAG_SERVER_URI = "%SERVER_URI%";

    /**
     * Platform service name.
     */
    static final String SVC_NAME_PLATFORM = "iPlanetAMPlatformService";

    /**
     * LDAP server host name for saml2 crl cache
     */
    static final String CRL_CACHE_DIR_HOST =
            "com.sun.identity.crl.cache.directory.host";

    /**
     * LDAP server port number for saml2 crl cache
     */
    static final String CRL_CACHE_DIR_PORT =
            "com.sun.identity.crl.cache.directory.port";

    /**
     * LDAP server ssl config for saml2 crl cache
     */
    static final String CRL_CACHE_DIR_SSL_ENABLED =
            "com.sun.identity.crl.cache.directory.ssl";

    /**
     * LDAP Server bind user name for saml2 crl cache
     */
    static final String CRL_CACHE_DIR_USER =
            "com.sun.identity.crl.cache.directory.user";

    /**
     * LDAP Server bind password for saml2 crl cache
     */
    static final String CRL_CACHE_DIR_PASSWD =
            "com.sun.identity.crl.cache.directory.password";

    /**
     * LDAP Server search base dn for saml2 crl cache
     */
    static final String CRL_CACHE_DIR_SEARCH_LOC =
            "com.sun.identity.crl.cache.directory.searchlocs";

    /**
     * LDAP attribute name for searching crl entry
     */
    static final String CRL_CACHE_DIR_SEARCH_ATTR =
            "com.sun.identity.crl.cache.directory.searchattr";

    /**
     * Naming service name.
     */
    static final String SVC_NAME_NAMING = "iPlanetAMNamingService";

    /**
     * Certificate Alias name for SSL Client Auth
     */
    static final String CLIENT_CERTIFICATE_ALIAS =
            "com.sun.identity.security.keyStore.clientAlias";

    /**
     * User service name.
     */
    static final String SVC_NAME_USER = "iPlanetAMUserService";

    /**
     * Authentication Configuration service name.
     */
    static final String SVC_NAME_AUTH_CONFIG = "iPlanetAMAuthConfiguration";

    /**
     * SAML service name.
     */
    static final String SVC_NAME_SAML = "iPlanetAMSAMLService";

    /**
     * Certificate Alias name for SSL Client Auth
     */
    static final String URL_CONNECTION_USE_CACHE =
            "com.sun.identity.urlconnection.useCache";

    /**
     * Property string for distauth deployment descriptor
     */
    static final String AM_DISTAUTH_DEPLOYMENT_DESCRIPTOR =
            "com.iplanet.am.distauth.deploymentDescriptor";

    /**
     * Property string for cdc servlet login url
     */
    static final String CDCSERVLET_LOGIN_URL =
            "com.sun.identity.cdcservlet.loginurl";

    /**
     * Property name for data encryption key
     */
    static final String ENC_PWD_PROPERTY = "am.encryption.pwd";

    /**
     * Property string for load balancer cookie value.
     */
    static final String PROPERTY_NAME_LB_COOKIE_VALUE =
            "com.iplanet.am.lbcookie.value";

    /**
     * Key name for serverid-cookievalue list in naming table.
     */
    static final String SERVERID_LBCOOKIEVALUE_LIST =
            "iplanet-am-platform-lb-cookie-value-list";

    static final String DISTAUTH_BOOTSTRAP_FILE = "openam.das.bootstrap.file";

    /**
     * Configuration Variable for distauth bootstrap file base directory.
     */
    static final String CONFIG_VAR_DISTAUTH_BOOTSTRAP_BASE_DIR = "FAMDistAuth";

    /**
     * Configuration Variable for distauth bootstrap file name.
     */
    static final String CONFIG_VAR_DISTAUTH_BOOTSTRAP_FILENAME = "AMDistAuthConfig.properties";

    /**
     * property string for enabling SMS datastore notification
     */
    static final String SMS_ENABLE_DB_NOTIFICATION =
            "com.sun.identity.sm.enableDataStoreNotification";

    /**
     * property string for controlling SMS, AMSDK & IdRepo cache
     */
    static final String SDK_GLOBAL_CACHE_PROPERTY =
            "com.iplanet.am.sdk.caching.enabled";

    /**
     * property string for controlling SMS cache.
     * Active only if "com.iplanet.am.sdk.caching.enabled" is "false"
     */
    static final String SMS_CACHE_PROPERTY = "com.sun.identity.sm.cache.enabled";

    /**
     * property string to enable SMS cache expiry time.
     */
    static final String SMS_CACHE_TTL_ENABLE = "com.sun.identity.sm.cache.ttl.enable";

    /**
     * property string for controlling SMS cache expiry time, in minutes.
     * The default values is 30 minutes. After the expiry time the next access
     * to the object will fetched from the backend datastore.
     */
    static final String SMS_CACHE_TTL = "com.sun.identity.sm.cache.ttl";

    /**
     * property string to manage the persistent connection to directory
     */
    static final String EVENT_LISTENER_DISABLE_LIST =
            "com.sun.am.event.connection.disable.list";

    /**
     * property string to cache past event changes in minutes
     */
    static final String EVENT_LISTENER_REMOTE_CLIENT_BACKLOG_CACHE =
            "com.sun.am.event.notification.expire.time";

    /**
     * property string to the size of SystemTimerPool
     */
    static final String SYSTEM_TIMERPOOL_SIZE =
            "com.sun.identity.common.systemtimerpool.size";

    /**
     * property string for Distributed Authentication cluster
     */
    static final String DISTAUTH_CLUSTER =
            "com.sun.identity.distauth.cluster";

    /**
     * property string for Krb5LoginModule class name
     */
    static final String KRB5_LOGINMODULE =
            "com.sun.identity.authentication.module.WindowsDesktopSSO.Krb5LoginModule";
    static final String DEFAULT_KRB5_LOGINMODULE =
            "com.sun.security.auth.module.Krb5LoginModule";
    static final String KRB5_CREDENTIAL_TYPE =
            "com.sun.identity.authentication.module.WindowsDesktopSSO.credsType";

    /**
     * property to control whether remote auth includes request/response
     */
    static final String REMOTEAUTH_INCLUDE_REQRES =
            "openam.remoteauth.include.reqres";

    /**
     * property to control if the OpenAM session cookie should be made
     * persistent
     */
    static final String PERSIST_AM_COOKIE =
            "openam.session.persist_am_cookie";

    /**
     * property to control if the OpenAM server will persist the OpenAM
     * session cookie if the following parameter is in the incoming request
     * <code>PersistAMCookie</code>.
     */
    static final String ALLOW_PERSIST_AM_COOKIE =
            "openam.session.allow_persist_am_cookie";

    /**
     * Server configuration property for the OpenDS admin port
     */
    static final String DS_ADMIN_PORT =
            "org.forgerock.embedded.dsadminport";

    /**
     * OpenDS Replication Port
     */
    static final String EMBED_REPL_PORT =
            "com.sun.embedded.replicationport";

    /**
     * OpenDS Replication Port
     */
    static final String EMBED_SYNC_SERVERS =
            "com.sun.embedded.sync.servers";

    /**
     * Configuration property to enable the site monitor in the naming service
     */
    static final String SITEMONITOR_DISABLED =
            "openam.naming.sitemonitor.disabled";

    /**
     * EQUALS sign
     */
    static final String EQUALS = "=";

    /**
     * semi-colon sign
     */
    static final String SEMI_COLON = ";";

    /**
     * colon sign
     */
    static final String COLON = ":";

    /**
     * colon sign
     */
    static final String COMMA = ",";

    /**
     * amp sign
     */
    static final String AMP = "&";

    /**
     * at sign
     */
    static final String AT = "@";

    /**
     * empty string
     */
    static final String EMPTY = "";

    /**
     * Constant for file separator
     */
    static final String FILE_SEPARATOR = "/";

    /**
     * Constant for string "local".
     */
    static final String LOCAL = "local";

    /**
     * Property string for sm and um notification thread pool size
     */
    public static String SM_THREADPOOL_SIZE =
            "com.sun.identity.sm.notification.threadpool.size";

    /**
     * Key to indicate if the customer is performing auths via mutiple tabs
     * of the same browser.
     */
    static final String MULTIPLE_TABS_USED =
            "com.sun.identity.authentication.multiple.tabs.used";

    /**
     * empty string
     */
    static final String DELIMITER_PREF_LEFT =
            "openam.entitlement.delimiter.precedence.left";

    static final String USE_OLD_LOG_FORMAT =
            "openam.logging.use.old.log.format";

    static final String SESSION_UPGRADER_IMPL =
            "openam.auth.session_property_upgrader";

    static final String DEFAULT_SESSION_UPGRADER_IMPL =
            "org.forgerock.openam.authentication.service.DefaultSessionPropertyUpgrader";

    /**
     * Property for dist auth cookie name.
     */
    static final String AM_DIST_AUTH_COOKIE_NAME =
            "openam.auth.distAuthCookieName";

    static final String DESTROY_SESSION_AFTER_UPGRADE =
            "openam.auth.destroy_session_after_upgrade";

    static final String AUTH_RATE_MONITORING_INTERVAL =
            "openam.auth.rate_monitoring_interval";

    static final String CASE_SENSITIVE_UUID =
            "openam.session.case.sensitive.uuid";

    static final String RETAINED_HTTP_HEADERS_LIST =
            "openam.retained.http.headers";

    static final String FORBIDDEN_TO_COPY_HEADERS =
            "openam.forbidden.to.copy.headers";

    static final String RETAINED_HTTP_REQUEST_HEADERS_LIST =
            "openam.retained.http.request.headers";

    static final String FORBIDDEN_TO_COPY_REQUEST_HEADERS =
            "openam.forbidden.to.copy.request.headers";

    static final String IGNORE_GOTO_DURING_LOGOUT =
            "openam.authentication.ignore_goto_during_logout";

    static final String AM_DISTAUTH_LB_COOKIE_NAME =
            "openam.auth.distauth.lb_cookie_name";

    static final String AM_DISTAUTH_LB_COOKIE_VALUE =
            "openam.auth.distauth.lb_cookie_value";

    static final String AM_DISTAUTH_SITES =
            "openam.auth.distauth.sites";

    static final String AM_VERSION_HEADER_ENABLED =
            "openam.auth.version.header.enabled";

    /**
     * Key name for site ID list in naming table.
     */
    static final String SITE_NAMES_LIST =
            "openam-am-platform-site-names-list";

    static final String RUNTIME_SHUTDOWN_HOOK_ENABLED =
            "openam.runtime.shutdown.hook.enabled";
    /**
     * Property string for client IP address header.
     */
    static final String CLIENT_IP_ADDR_HEADER =
            "com.sun.identity.authentication.client.ipAddressHeader";

    static final String VERSION_DATE_FORMAT =
            "yyyy-MMMM-dd HH:mm";

    /**
     * Switch to allow for a generic Authentication Exception rather than
     * the more specific InvalidPassword Exception from the SOAP and REST API
     */
    static final String GENERIC_SOAP_REST_AUTHENTICATION_EXCEPTION =
            "openam.auth.soap.rest.generic.authentication.exception";

    /**
     * Default Domain Attribute
     */
    static final String DEFAULT_ROOT_NAMING_ATTRIBUTE = "dc";

    /**
     * Default Domain Attribute
     */
    static final String ORGANIZATION_NAMING_ATTRIBUTE = "o";

    /**
     * Default Root Context
     */
    static final String DEFAULT_ROOT_SUFFIX = "dc=openam,dc=openidentityplatform,dc=org";

    /**
     * Default Token Root Context, this will be used to create a Secondary Suffix during installation.
     */
    static final String DEFAULT_TOKEN_ROOT_SUFFIX = "ou=tokens";

    /**
     * Default Session SFO/HA Root DN
     */
    static final String DEFAULT_SESSION_HA_ROOT_SUFFIX = "ou=openam-session";

    /**
     * Default SAML2 Root Suffix
     */
    static final String DEFAULT_SAML2_HA_ROOT_SUFFIX = "ou=openam-saml2";

    /**
     * Default SAML2 Root Suffix
     */
    static final String DEFAULT_OAUTH2_HA_ROOT_SUFFIX = "ou=openam-oauth2";

    /**
     * Default Session SFO/HA Store Type.
     */
    static final String DEFAULT_SESSION_HA_STORE_TYPE = "none";

    public static final String ATTR_NAME_AGENT_TYPE = "AgentType";
    /**
     * Additional Directory Constants
     */

    static final String TOP = "top";

    static final String ASTERISK = "*";

    static final String OBJECTCLASS = "objectClass";

    /**
     * When a non-admin is logged into the XUI, enabling this will ensure the XUI calls the server periodically to check
     * the user still has a valid session. This ensures sensative user information will not remain on-screen and instead
     * they will directed to the login screen.
     */
    static final String XUI_USER_SESSION_VALIDATION_ENABLED = "org.forgerock.openam.xui.user.session.validation.enabled";

    /**
     * AMSetupFilter will redirect to this external URL in case the configuration store
     * is not available but the bootstrap file exists
     */
    public static final String CONFIG_STORE_DOWN_REDIRECT_URL =
        "openam.configstore.down.redirect.url";

    /**
     * System property/service attribute name for CAS/DAS that should tell whether Zero Page Login is enabled or not.
     */
    public static final String ZERO_PAGE_LOGIN_ENABLED = "openam.auth.zero.page.login.enabled";

    /**
     * System property/service attribute name for property giving whitelist of allowed HTTP Referer URLs that
     * are allowed. This provides some mitigation against Login CSRF attacks. When used as a system property, this
     * should be a space-delimited list of referer urls.
     */
    public static final String ZERO_PAGE_LOGIN_WHITELIST = "openam.auth.zero.page.login.referer.whitelist";

    /**
     * System property/service attribute name for whether to allow Zero Page Login requests if the HTTP Referer
     * header is not set.
     */
    public static final String ZERO_PAGE_LOGIN_ALLOW_MISSING_REFERER = "openam.auth.zero.page.login.allow.null.referer";

    /**
     * Heartbeat in seconds of the LDAP Store
     */
    public static final String LDAP_HEARTBEAT = "org.forgerock.services.cts.store.heartbeat";

    /**
     * Size of XML shared DocumentBuilder cache.
     */
    final String XML_DOCUMENT_BUILDER_CACHE_SIZE = "org.forgerock.openam.utils.xml.documentbuilder.cache.size";

    /**
     * Size of XML shared SAXParser cache.
     */
    final String XML_SAXPARSER_CACHE_SIZE = "org.forgerock.openam.utils.xml.saxparser.cache.size";

    /**
     * OPENAM-3959
     * set true, calculate auth level only with successful login module
     * and skip the rest of REQUIRED/REQUISITE in chain
     */
    public static final String AUTH_LEVEL_EXCLUDE_REQUIRED_REQUISITE
        = "org.forgerock.openam.authLevel.excludeRequiredOrRequisite";

    /**
     * Size of XML shared TransformerFactory cache.
     */
    public static final String XML_TRANSFORMER_FACTORY_CACHE_SIZE =
            "org.forgerock.openam.utils.xml.transformerfactory.cache.size";

    /**
     * Size of XML shared XPathFactory cache.
     */
    final String XPATHFACTORY_CACHE_SIZE = "org.forgerock.openam.utils.xml.xpathfactory.cache.size";

    /**
     * Property to enable/disable resource lookup caching.
     */
    public static final String RESOURCE_LOOKUP_CACHE_ENABLED =
            "org.forgerock.openam.core.resource.lookup.cache.enabled";

    /**
     * Property to enable/disable autocomplete on password/form fields.
     */
    public static final String AUTOCOMPLETE_ENABLED = "org.forgerock.openam.console.autocomplete.enabled";

    /**
     * Property to enable/disable access to CoreTokenResource REST endpoint.
     */
    public static final String CORE_TOKEN_RESOURCE_ENABLED = "org.forgerock.openam.cts.rest.enabled";

    /**
     * Property to define the default time limit for LDAP operations performed by the Netscape LDAP SDK.
     */
    public static final String DEFAULT_LDAP_TIME_LIMIT = "org.forgerock.openam.ldap.default.time.limit";

    /**
     * Property that defines a comma separated list of classes that are valid during deserialisation of Java classes
     * in OpenAM, for example, in the JATO framework
     */
    public static final String DESERIALISATION_CLASSES_WHITELIST = "openam.deserialisation.classes.whitelist";

    /**
     * Property used by the XML builder to retrieve a configuration specific name for an attribute schema.
     */
    public static final String CONFIGURATION_NAME = "configuration_name";

    /**
     * Heartbeat timeout in seconds of the HeartBeatConnectionFactory
     * The heartbeat timeout after which a connection will be marked as failed
     */
    public static final String LDAP_HEARTBEAT_TIMEOUT = "org.forgerock.openam.ldap.heartbeat.timeout";

    /**
     * Sets the maximum file upload size - if not set the default will be 750k.
     */
    public static final String MAX_FILE_UPLOAD_SIZE = "org.forgerock.openam.console.max.file.upload.size";

    /**
     * Property that allows the AM_ACCESS_ATTEMPT event name to be audited.
     */
    public static final String AUDIT_AM_ACCESS_ATTEMPT_ENABLED = "org.forgerock.openam.audit.access.attempt.enabled";

    /**
     * Property to specify the TLS version to connect to the secure ldap server.
     */
    public static final String LDAP_SERVER_TLS_VERSION = "org.forgerock.openam.ldap.secure.protocol.version";

    /**
     * Property to enable capturing trace-level messages from Log4J world when in message-level debug mode.
     */
    public static final String ENABLE_TRACE_IN_MESSAGE_MODE = "org.forgerock.openam.slf4j.enableTraceInMessage";

    /**
     * Property to determine whether notifications should be published for agent consumption.
     */
    String NOTIFICATIONS_AGENTS_ENABLED = "org.forgerock.openam.notifications.agents.enabled";

    /** Service name for the REST APIs service. */
    String REST_APIS_SERVICE_NAME = "RestApisService";
    
    /** Service version for the REST APIs service. */
    String REST_APIS_SERVICE_VERSION = "1.0";

    /**
     * The name of the request attribute that tells whether this authentication happened via WS-Fed AR profile.
     */
    String WSFED_ACTIVE_LOGIN = "org.forgerock.openam.federation.wsfed.active.login";
}
