<?php
/* The contents of this file are subject to the terms
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
 * $Id: Constants.php,v 1.1 2007/03/09 21:13:19 chicchiricco Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */

/**
 * This interface contains all the property names defined in in
 * product configurations and may be expanded with other constant
 * values that are used for Access and Federation Manager development.
 */
interface Constants {

    /**
     * Property const for debug level.
     */
    const SERVICES_DEBUG_LEVEL = "com.iplanet.services.debug.level";

    /**
     * Property const for debug directory.
     */
    const SERVICES_DEBUG_DIRECTORY = "com.iplanet.services.debug.directory";

    /**
     * Property const for interval of <code>Stats</code> service.
     */
    const AM_STATS_INTERVAL = "com.iplanet.am.stats.interval";

    /**
     * Property const for state of <code>Stats</code> service.
     */
    const SERVICES_STATS_STATE = "com.iplanet.services.stats.state";

    /**
     * Property const for directory of <code>Stats</code> service.
     */
    const SERVICES_STATS_DIRECTORY = "com.iplanet.services.stats.directory";

    /**
     * Property const for SDK caching size.
     */
    const AM_SDK_CACHE_MAXSIZE =
        "com.iplanet.am.sdk.cache.maxSize";

    /**
     * Property const for module for processing user handling.
     */
    const AM_SDK_USER_ENTRY_PROCESSING_IMPL =
        "com.iplanet.am.sdk.userEntryProcessingImpl";

    /**
     * Property const for default organization.
     */
    const AM_DEFAULT_ORG =
        "com.iplanet.am.defaultOrg";

    /**
     * Property const for SSL enabled.
     */
    const AM_DIRECTORY_SSL_ENABLED =
        "com.iplanet.am.directory.ssl.enabled";

    /**
     * Property const for directory host.
     */
    const AM_DIRECTORY_HOST =
        "com.iplanet.am.directory.host";

    /**
     * Property const for directory port.
     */
    const AM_DIRECTORY_PORT =
        "com.iplanet.am.directory.port";

    /**
     * Property const for server protocol.
     */
    const AM_SERVER_PROTOCOL =
        "com.iplanet.am.server.protocol";

    /**
     * Property const for server host.
     */
    const AM_SERVER_HOST =
        "com.iplanet.am.server.host";

    /**
     * Property const for server port.
     */
    const AM_SERVER_PORT =
        "com.iplanet.am.server.port";

    /**
     * Property const for Distributed Authentication server protocol.
     */
    const DISTAUTH_SERVER_PROTOCOL =
        "com.iplanet.distAuth.server.protocol";

    /**
     * Property const for Distributed Authentication server host.
     */
    const DISTAUTH_SERVER_HOST =
        "com.iplanet.distAuth.server.host";

    /**
     * Property const for Distributed Authentication server port.
     */
    const DISTAUTH_SERVER_PORT =
        "com.iplanet.distAuth.server.port";

    /**
     * Property const for console protocol.
     */
    const AM_CONSOLE_PROTOCOL =
        "com.iplanet.am.console.protocol";

    /**
     * Property const for console host.
     */
    const AM_CONSOLE_HOST =
        "com.iplanet.am.console.host";

    /**
     * Property const for console port.
     */
    const AM_CONSOLE_PORT =
        "com.iplanet.am.console.port";

    /**
     * Property const for profile host.
     */
    const AM_PROFILE_HOST =
        "com.iplanet.am.profile.host";

    /**
     * Property const for profile port.
     */
    const AM_PROFILE_PORT =
        "com.iplanet.am.profile.port";

    /**
     * Property const for naming URL.
     */
    const AM_NAMING_URL =
        "com.iplanet.am.naming.url";

    /**
     * Property const for notification URL.
     */
    const AM_NOTIFICATION_URL = "com.iplanet.am.notification.url";

    /**
     * Property const for load balancer.
     */
    const AM_REDIRECT = "com.sun.identity.url.redirect";

    /**
     * Property const for daemon process.
     */
    const AM_DAEMONS = "com.iplanet.am.daemons";

    /**
     * Property const for cookie name.
     */
    const AM_COOKIE_NAME = "com.iplanet.am.cookie.name";

    /**
     * Property const for load balancer cookie name.
     */
    const AM_LB_COOKIE_NAME = "com.iplanet.am.lbcookie.name";

    /**
     * Property const for load balancer cookie value.
     */
    const AM_LB_COOKIE_VALUE = "com.iplanet.am.lbcookie.value";

    /**
     * Property const for secure cookie.
     */
    const AM_COOKIE_SECURE = "com.iplanet.am.cookie.secure";

    /**
     * Property const for cookie encoding.
     */
    const AM_COOKIE_ENCODE = "com.iplanet.am.cookie.encode";

    /**
     * Property const for <code>pcookie</code> name.
     */
    const AM_PCOOKIE_NAME = "com.iplanet.am.pcookie.name";

    /**
     * Property const for locale.
     */
    const AM_LOCALE = "com.iplanet.am.locale";

    /**
     * Property const for log status.
     */
    const AM_LOGSTATUS = "com.iplanet.am.logstatus";

    /**
     * Property const for directory root suffix.
     */
    const AM_ROOT_SUFFIX = "com.iplanet.am.rootsuffix";

    /**
     * Property const for domain component.
     */
    const AM_DOMAIN_COMPONENT = "com.iplanet.am.domaincomponent";

    /**
     * Property const for version number.
     */
    const AM_VERSION = "com.iplanet.am.version";

    /**
     * Property const for <code>CertDB</code> directory.
     */
    const AM_ADMIN_CLI_CERTDB_DIR = "com.iplanet.am.admin.cli.certdb.dir";

    /**
     * Property const for <code>CertDB</code> prefix.
     */
    const AM_ADMIN_CLI_CERTDB_PREFIX ="com.iplanet.am.admin.cli.certdb.prefix";

    /**
     * Property const for <code>CertDB</code> password file.
     */
    const AM_ADMIN_CLI_CERTDB_PASSFILE =
        "com.iplanet.am.admin.cli.certdb.passfile";

    /**
     * Property const for OCSP responder URL.
     */
    const AUTHENTICATION_OCSP_RESPONDER_URL =
        "com.sun.identity.authentication.ocsp.responder.url";

    /**
     * Property const for OCSP responder nickname.
     */
    const AUTHENTICATION_OCSP_RESPONDER_NICKNAME =
        "com.sun.identity.authentication.ocsp.responder.nickname";

    /**
     * Property const for SAML XML signature key store file.
     */
    const SAML_XMLSIG_KEYSTORE =
        "com.sun.identity.saml.xmlsig.keystore";

    /**
     * Property const for SAML XML signature key store password file.
     */
    const SAML_XMLSIG_STORE_PASS =
        "com.sun.identity.saml.xmlsig.storepass";

    /**
     * Property const for SAML XML signature key password file.
     */
    const SAML_XMLSIG_KEYPASS = "com.sun.identity.saml.xmlsig.keypass";

    /**
     * Property const for SAML XML signature CERT alias.
     */
    const SAML_XMLSIG_CERT_ALIAS = "com.sun.identity.saml.xmlsig.certalias";

    /**
     * Property const for authentication super user.
     */
    const AUTHENTICATION_SUPER_USER =
        "com.sun.identity.authentication.super.user";

    /**
     * Property const for authentication super user.
     */
    const AUTHENTICATION_SPECIAL_USERS =
        "com.sun.identity.authentication.special.users";

    /**
     * Property const for replica retry number.
     */
    const AM_REPLICA_NUM_RETRIES = "com.iplanet.am.replica.num.retries";

    /**
     * Property const for delay between replica retries.
     */
    const AM_REPLICA_DELAY_BETWEEN_RETRIES =
        "com.iplanet.am.replica.delay.between.retries";

    /**
     * Property const for retry number for event connection.
     */
    const AM_EVENT_CONNECTION_NUM_RETRIES =
        "com.iplanet.am.event.connection.num.retries";

    /**
     * Property const for delay time between retries for event connection.
     */
    const AM_EVENT_CONNECTION_DELAY_BETWEEN_RETRIES =
        "com.iplanet.am.event.connection.delay.between.retries";

    /**
     * Property const for <code>LDAPException</code> error codes that retries
     * will happen for event connection.
     */
    const AM_EVENT_CONNECCTION_LDAP_ERROR_CODES_RETRIES =
        "com.iplanet.am.event.connection.ldap.error.codes.retries";

    /**
     * Property const for number of time to retry for LDAP connection.
     */
    const AM_LDAP_CONNECTION_NUM_RETRIES =
        "com.iplanet.am.ldap.connection.num.retries";

    /**
     * Property const for delay time between retries for LDAP connection.
     */
    const AM_LDAP_CONNECTION_DELAY_BETWEEN_RETRIES =
        "com.iplanet.am.ldap.connection.delay.between.retries";

    /**
     * Property const for <code>LDAPException</code> error codes that retries
     * will happen for LDAP connection.
     */
    const AM_LDAP_CONNECTION_LDAP_ERROR_CODES_RETRIES =
        "com.iplanet.am.ldap.connection.ldap.error.codes.retries";

    /**
     * Property const for installation directory
     */
    const AM_INSTALL_DIR = "com.iplanet.am.installdir";

    /**
     * Property const for installation base directory
     */
    const AM_INSTALL_BASEDIR =
        "com.iplanet.am.install.basedir";

    /**
     * Property const for new configuraton file in case of single war
     * deployment
     */
    const AM_NEW_CONFIGFILE_PATH =
        "com.sun.identity.configFilePath";

    /**
     * Property const for installation config directory
     */
    const AM_INSTALL_VARDIR =
        "com.iplanet.am.install.vardir";

    /**
     * Property const for shared secret for application authentication module
     */
    const AM_SERVICES_SECRET =
        "com.iplanet.am.service.secret";

    /**
     * Property const for service deployment descriptor
     */
    const AM_SERVICES_DEPLOYMENT_DESCRIPTOR =
        "com.iplanet.am.services.deploymentDescriptor";

    /**
     * Property const for console deployment descriptor
     */
    const AM_CONSOLE_DEPLOYMENT_DESCRIPTOR =
        "com.iplanet.am.console.deploymentDescriptor";

    /**
     * Property const for agent URL deployment descriptor
     */
    const AM_POLICY_AGENTS_URL_DEPLOYMENT_DESCRIPTOR =
        "com.iplanet.am.policy.agents.url.deploymentDescriptor";

    /**
     * property const which contains the name of HTTP session tracking cookie
     */
    const AM_SESSION_HTTP_SESSION_TRACKING_COOKIE_NAME =
        "com.iplanet.am.session.failover.httpSessionTrackingCookieName";

    /**
     * property const to choose whether local or remote saving method is used
     */
    const AM_SESSION_FAILOVER_USE_REMOTE_SAVE_METHOD =
        "com.iplanet.am.session.failover.useRemoteSaveMethod";

    /**
     * property const to choose whether we rely on app server load balancer to
     * do the request routing or use our own
     */
    const
        AM_SESSION_FAILOVER_USE_INTERNAL_REQUEST_ROUTING =
            "com.iplanet.am.session.failover.useInternalRequestRouting";

    /**
     * Property const for failover cluster state check timeout
     */
    const AM_SESSION_FAILOVER_CLUSTER_SERVER_LIST =
        "com.iplanet.am.session.failover.cluster.serverList";

    /**
     * Property const for failover cluster state check timeout
     */
    const
        AM_SESSION_FAILOVER_CLUSTER_STATE_CHECK_TIMEOUT =
            "com.iplanet.am.session.failover.cluster.stateCheck.timeout";

    /**
     * Property const for failover cluster state check period
     */
    const AM_SESSION_FAILOVER_CLUSTER_STATE_CHECK_PERIOD =
        "com.iplanet.am.session.failover.cluster.stateCheck.period";

    /**
     * Property const for naming <code>failover</code> URL
     */
    const AM_NAMING_FAILOVER_URL =
        "com.iplanet.am.naming.failover.url";

    /**
     * Property const for max number of sessions
     */
    const AM_SESSION_MAX_SESSIONS =
        "com.iplanet.am.session.maxSessions";

    /**
     * Property const for checking if HTTP session is enabled
     */
    const AM_SESSION_HTTP_SESSION_ENABLED =
        "com.iplanet.am.session.httpSession.enabled";

    /**
     * Property const for max session time for invalid session
     */
    const AM_SESSION_INVALID_SESSION_MAXTIME =
        "com.iplanet.am.session.invalidsessionmaxtime";

    /**
     * Property const for checking if session client polling is enabled
     */
    const AM_SESSION_CLIENT_POLLING_ENABLED =
        "com.iplanet.am.session.client.polling.enable";

    /**
     * Property const for session client polling period.
     */
    const AM_SESSION_CLIENT_POLLING_PERIOD =
        "com.iplanet.am.session.client.polling.period";

    /**
     * Property const for security provider package.
     */
    const SECURITY_PROVIDER_PKG =
        "com.sun.identity.security.x509.pkg";

    /**
     * Property const for iplanet security provider package.
     */
    const IPLANET_SECURITY_PROVIDER_PKG =
        "com.iplanet.security.x509.impl";

    /**
     * Property const for sun security provider package.
     */
    const SUN_SECURITY_PROVIDER_PKG =
        "com.sun.identity.security.x509.impl";

    /**
     * Property const for UNIX helper port.
     */
    const UNIX_HELPER_PORT =
        "unixHelper.port";

    /**
     * Property const for <code>securid</code> authentication module and
     * helper port.
     */
    const SECURID_HELPER_PORTS = "securidHelper.ports";

    /**
     * Property const for SMTP host.
     */
    const AM_SMTP_HOST = "com.iplanet.am.smtphost";

    /**
     * Property const for SMTP port.
     */
    const SM_SMTP_PORT = "com.iplanet.am.smtpport";

    /**
     * Property const for CDSSO URL.
     */
    const SERVICES_CDSSO_CDCURL = "com.iplanet.services.cdsso.CDCURL";

    /**
     * Property const for CDSSO cookie domain.
     */
    const SERVICES_CDSSO_COOKIE_DOMAIN =
        "com.iplanet.services.cdsso.cookiedomain";

    /**
     * Property const for CDC auth login URL.
     */
    const SERVICES_CDC_AUTH_LOGIN_URL =
        "com.iplanet.services.cdc.authLoginUrl";

    /**
     * Property const for maximum content-length accepted in HttpRequest.
     */
    const
        SERVICES_COMM_SERVER_PLLREQUEST_MAX_CONTENT_LENGTH =
            "com.iplanet.services.comm.server.pllrequest.maxContentLength";

    /**
     * Property const for factory class name for
     * <code>SecureRandomFactory</code>.
     */
    const SECURITY_SECURE_RANDOM_FACTORY_IMPL =
        "com.iplanet.security.SecureRandomFactoryImpl";

    /**
     * Property const for factory class name for <code>LDAPSocketFactory</code>
     */
    const SECURITY_SSL_SOCKET_FACTORY_IMPL =
        "com.iplanet.security.SSLSocketFactoryImpl";

    /**
     * Property const for encrypting class implementation.
     */
    const SECURITY_ENCRYPTOR = "com.iplanet.security.encryptor";

    /**
     * Property const for checking if console is remote.
     */
    const AM_CONSOLE_REMOTE = "com.iplanet.am.console.remote";

    /**
     * Property const for checking if client IP check is enabled.
     */
    const AM_CLIENT_IP_CHECK_ENABLED = "com.iplanet.am.clientIPCheckEnabled";

    /**
     * Property const for user name for application authentication module.
     */
    const AGENTS_APP_USER_NAME = "com.sun.identity.agents.app.username";

    /**
     * Property const for log file name for logging messages.
     */
    const AGENTS_SERVER_LOG_FILE_NAME =
        "com.sun.identity.agents.server.log.file.name";

    /**
     * Property const for resource result cache size.
     */
    const AGENTS_CACHE_SIZE = "com.sun.identity.agents.cache.size";

    /**
     * Property const for agent polling interval.
     */
    const AGENTS_POLLING_INTERVAL = "com.sun.identity.agents.polling.interval";

    /**
     * Property const for checking if agent notification is enabled.
     */
    const AGENTS_NOTIFICATION_ENABLED =
        "com.sun.identity.agents.notification.enabled";

    /**
     * Property const for agent notification URL.
     */
    const AGENTS_NOTIFICATION_URL = "com.sun.identity.agents.notification.url";

    /**
     * Property const for checking whether to use wildcard for resource name
     * comparison.
     */
    const AGENTS_USE_WILDCARD = "com.sun.identity.agents.use.wildcard";

    /**
     * Property const for attributes to be returned by policy evaluator.
     */
    const AGENTS_HEADER_ATRIBUTES =
        "com.sun.identity.agents.header.attributes";

    /**
     * Property const for resource comparator class name.
     */
    const AGENTS_RESOURCE_COMPARATOR_CLASS =
        "com.sun.identity.agents.resource.comparator.class";

    /**
     * Property const for resource comparator class name.
     */
    const AGENTS_RESOURCE_WILDCARD =
        "com.sun.identity.agents.resource.wildcard";

    /**
     * Property const for resource name's delimiter.
     */
    const AGENTS_RESOURCE_DELIMITER =
        "com.sun.identity.agents.resource.delimiter";

    /**
     * Property const for indicator if case sensitive is on during policy
     * evaluation.
     */
    const AGENTS_RESOURCE_CASE_SENSITIVE =
        "com.sun.identity.agents.resource.caseSensitive";

    /**
     * Property const for true value of policy action.
     */
    const AGENTS_TRUE_VALUE = "com.sun.identity.agents.true.value";

    /**
     * Property const for federation service cookie.
     */
    const FEDERATION_FED_COOKIE_NAME =
        "com.sun.identity.federation.fedCookieName";

    /**
     * Property const for federation signing on indicator.
     */
    const FEDERATION_SERVICES_SIGNING_ON =
        "com.sun.identity.federation.services.signingOn";

    /**
     * Property const for session notification thread pool size.
     */
    const NOTIFICATION_THREADPOOL_SIZE =
        "com.iplanet.am.notification.threadpool.size";

    /**
     * Property const for name of the webcontainer.
     */
    const IDENTITY_WEB_CONTAINER = "com.sun.identity.webcontainer";

    /**
     * Property const for session notification thread pool queue size.
     */
    const NOTIFICATION_THREADPOOL_THRESHOLD =
        "com.iplanet.am.notification.threadpool.threshold";

    /**
     * Property const for fully qualified host name map.
     */
    const AM_FQDN_MAP = "com.sun.identity.server.fqdnMap";

    /**
     * Client detection module content type property name.
     */
    const CDM_CONTENT_TYPE_PROPERTY_NAME = "contentType";

    /**
     * Default charset to be used in case the client detection has failed.
     */
    const CONSOLE_UI_DEFAULT_CHARSET = "UTF-8";

    /**
     * Attribute name of the user preferred locale located in amUser service.
     */
    const USER_LOCALE_ATTR = "preferredlocale";

    /**
     * Property const for checking if <code>HostLookUp</code> is enabled.
     */
    const ENABLE_HOST_LOOKUP = "com.sun.am.session.enableHostLookUp";

    /**
     * Property const for checking if <code>HostLookUp</code> is enabled.
     */
    const WEBCONTAINER = "com.sun.identity.webcontainer";

    /**
     * Property const for determining if cookie needs to be written in the URL
     * as a path info.
     */
    const REWRITE_AS_PATH =
        "com.sun.identity.cookieRewritingInPath";

    /**
     * Property const for Application session max-caching-time.
     */
    const APPLICATION_SESSION_MAX_CACHING_TIME =
        "com.sun.identity.session.application.maxCacheTime";

    /**
     * Property const to enable Session/Cookie hijacking mode in Access
     * Manager.
     */
    const IS_ENABLE_UNIQUE_COOKIE =
        "com.sun.identity.enableUniqueSSOTokenCookie";

    /**
     * Property const for 'HostUrl' Cookie name in Session/Cookie hijacking
     * mode.
     */
    const AUTH_UNIQUE_COOKIE_NAME =
        "com.sun.identity.authentication.uniqueCookieName";

    /**
     * Property const for unique Cookie domain in Session/Cookie hijacking
     * mode.
     */
    const AUTH_UNIQUE_COOKIE_DOMAIN =
        "com.sun.identity.authentication.uniqueCookieDomain";

    /**
     * Property const for checking if remote method
     * <code>AddListenerOnAllSessions</code> is enabled.
     */
    const ENABLE_ADD_LISTENER_ON_ALL_SESSIONS =
        "com.sun.am.session.enableAddListenerOnAllSessions";

    /**
     * Property const for list of IP address of remote clients which are
     * considered trusted to forward the context used to check <code>restricted
     * token usage</code> is enabled.
     */
    const TRUSTED_SOURCE_LIST = "com.sun.am.session.trustedSourceList";

    /**
     * Property const to Identify the Http Header which returns the Client IP
     * address when running in loadbalancer configuration.
     */
    const HTTP_CLIENT_IP_HEADER =
        "com.sun.identity.session.httpClientIPHeader";

    /**
     * User object type.
     */
    const OBJECT_TYPE_USER = "user";

    /**
     * Agent object type.
     */
    const OBJECT_TYPE_AGENT = "Agent";

    /**
     * Property const to ensure more constent (security-wise) check If enabled
     * the <code>DN is converted to lowercase</code> for comparison.
     */
    const CASE_INSENSITIVE_DN = "com.sun.am.session.caseInsensitiveDN";

    /**
     * Property const to determine if validation is required when parsing XML
     * documents using Access Manager XMLUtils class.
     */
    const XML_VALIDATING = "com.iplanet.am.util.xml.validating";

    /**
     * Property const to determine if authentication enforces using seperate
     * JAAS thread or not.
     */
    const ENFORCE_JAAS_THREAD =
        "com.sun.identity.authentication.usingJaasThread";

    /**
     * Property const to list all the Session properties that should be
     * protected.
     */
    const PROTECTED_PROPERTIES_LIST =
        "com.iplanet.am.session.protectedPropertiesList";

    /**
     * Property for Login URL.
     */
    const LOGIN_URL = "com.sun.identity.loginurl";

    /**
     * System property name that is a list of package name prefixes is used to
     * resolve protocol names into actual handler class names.
     */
    const PROTOCOL_HANDLER = "java.protocol.handler.pkgs";

    /**
     * The package name prefix for JSS based protocol implementations.
     */
    const JSS_HANDLER = "com.iplanet.services.comm";

    /**
     * The package name prefix for JSSE based protocol implementations.
     */
    const JSSE_HANDLER = "com.sun.identity.protocol";

    /**
     * Property for passing the organization name when retrieving attribute
     * choice values.
     */
    const ORGANIZATION_NAME = "organization_name";

    /**
     * Organization name in Session/SSOToken Properties.
     */
    const ORGANIZATION = "Organization";

    /**
     * Property for auth cookie name.
     */
    const AM_AUTH_COOKIE_NAME = "com.sun.identity.auth.cookieName";

    /**
     * Unique Id set as a session property which is used for logging.
     */
    const AM_CTX_ID = "AMCtxId";

    /**
     * Global schema property name in Session Service.
     */
    const PROPERTY_CHANGE_NOTIFICATION =
        "iplanet-am-session-property-change-notification";

    /**
     * Global schema property name in Session Service.
     */
    const NOTIFICATION_PROPERTY_LIST =
        "iplanet-am-session-notification-property-list";

    /**
     * The session property name of the universal identifier used for IDRepo.
     */
    const UNIVERSAL_IDENTIFIER = "sun.am.UniversalIdentifier";

    /**
     * Property const for session polling thread pool size.
     */
    const POLLING_THREADPOOL_SIZE =
        "com.sun.identity.session.polling.threadpool.size";

    /**
     * Property const for session polling thread pool queue size.
     */
    const POLLING_THREADPOOL_THRESHOLD =
        "com.sun.identity.session.polling.threadpool.threshold";

    /**
     * Property for enabling or disabling encryption for Session Repository.
     */
    const SESSION_REPOSITORY_ENCRYPTION =
        "com.sun.identity.session.repository.enableEncryption";

    /**
     * Property const for determining whether or not appplication sessions
     * should be returned via the getValidSessions() call.
     */
    const SESSION_RETURN_APP_SESSION =
        "com.sun.identity.session.returnAppSession";

    /**
     * HTTP Form Parameter name used by PEP for posting policy advices to
     * Access Manager.
     */
    const COMPOSITE_ADVICE = "sunamcompositeadvice";

    /**
     * XML tag name used for Advices message.
     */
    const ADVICES_TAG_NAME = "Advices";

    /**
     * Key that is used to identify the advice messages from
     * <code>AuthSchemeCondition</code>.
     */
    const AUTH_SCHEME_CONDITION_ADVICE = "AuthSchemeConditionAdvice";

    /** Key that is used to identify the advice messages from
     * <code>AuthLevelCondition</code>.
     */
    const AUTH_LEVEL_CONDITION_ADVICE = "AuthLevelConditionAdvice";

    /**
     * Property const for determining whether server mode or client mode.
     */
    const SERVER_MODE = "com.iplanet.am.serverMode";

    /**
     * Key name for platform server list in naming table.
     */
    const PLATFORM_LIST = "iplanet-am-platform-server-list";

    /**
     * Key name for site list in naming table.
     */
    const SITE_LIST = "iplanet-am-platform-site-list";

    /**
     * Key name for site ID list in naming table.
     */
    const SITE_ID_LIST = "iplanet-am-platform-site-id-list";

    /**
     * Key name for site ID list in naming table.
     */
    const CLUSTER_SERVER_LIST = "iplanet-am-session-cluster-serverlist";

    /**
     * Default organization location properties name.
     */
    const DEFAULT_ORGANIZATION = "com.iplanet.am.defaultOrg";

    /**
     * This value is used by LDAP connection pool to reap connections
     * if they are idle for the number of seconds specified by the
     * value of this property.  If the value is set at 0, the connection
     * will not be reaped.
     */
    const LDAP_CONN_IDLE_TIME_IN_SECS =
        "com.sun.am.ldap.connnection.idle.seconds";

    /**
     * Constant for file separator.
     */
    const FILE_SEPARATOR = "/";

    /**
     * Install Time System property key.
     */
    const SYS_PROPERTY_INSTALL_TIME = "installTime";

    /**
     * This is a HTTP parameter to indicate to the authentication component
     * to either forward the request or redirect it after authentication
     * succeed.
     */
    const FORWARD_PARAM = "forwardrequest";

    /**
     * Value is for <code>FORWARD_PARAM</code> to indicate that the
     * authentication component should forward request.
     */
    const FORWARD_YES_VALUE = "yes";

    /**
     * Attribute name for the load balancer cookie in the
     * Naming Response.
     */
    const NAMING_AM_LB_COOKIE = "am_load_balancer_cookie";
}
?>
