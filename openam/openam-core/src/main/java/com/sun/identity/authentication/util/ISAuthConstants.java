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
 * $Id: ISAuthConstants.java,v 1.21 2009/11/25 12:05:07 manish_rustagi Exp $
 *
 */

/*
 * Portions Copyrighted 2010-2013 ForgeRock Inc
 */

package com.sun.identity.authentication.util;

/**
 * This interface contains all the constants defined in in Authentication
 * Service.
 */
public interface ISAuthConstants {
    /**
     * org parameter
     */
    public static final String ORG_PARAM = "org";

    /**
     * realm parameter
     */
    public static final String REALM_PARAM = "realm";

    /**
     * domain parameter
     */
    public static final String DOMAIN_PARAM = "domain";

    /**
     * Module Param
     */
    public static final String MODULE_PARAM = "module";
    
    /**
     * Query param name for User based authentication 
     */
    public String USER_PARAM = "user";
    
    /**
     * Query param name for Role based authentication
     */
    public String ROLE_PARAM = "role";
    
    /**
     * Query param name for AuthLevel based authentication
     */
    public String AUTH_LEVEL_PARAM = "authlevel";
    
    /**
     * Query param name for Service based authenticatin 
     * (a.k.a. authentication chain)
     */
    public String SERVICE_PARAM = "service";
    
    /**
     * Query param name for IP/Resource/Environment based authentication
     */
    public String IP_RESOURCE_ENV_PARAM = "resource";
    
    /**
     * Query param name for specify resource URL for the 
     * IP/Resource/Environment based authentication.
     */
    public String RESOURCE_URL_PARAM = "resourceURL";
    
    /**
     * Param name to specify redirect URL advice in Policy condition for the 
     * IP/Resource/Environment based authentication.
     */
    public String REDIRECT_URL_PARAM = "redirectURL";
    
    /**
     * Key name to specify the IP address value in environment map for the 
     * IP/Resource/Environment based authentication.
     */
    public static final String REQUEST_IP = "requestIp";
            
    /**
     * Param name for goto redirection
     */
    public String GOTO_PARAM = "goto";
    
    /**
     * Forward parameter, used by UI to inform Liberty federation if this is a
     * forward request or not after successful authentication.
     *
     * @deprecated As of OpenSSO version 8.0
     *             {@link com.sun.identity.shared.Constants#FORWARD_PARAM}
     *
     */
    public static final String FORWARD_PARAM = "forwardrequest";

    /**
     * Value for <code>FORWARD_PARAM</code> indicating this is a forward
     * request.
     *
     * @deprecated As of OpenSSO version 8.0
     *             {@link com.sun.identity.shared.Constants#FORWARD_YES_VALUE}
     */
    public static final String FORWARD_YES_VALUE = "yes";

    /**
     * Application user prefix
     */
    public static final String APPLICATION_USER_PREFIX = "amService-";

    /**
     * Application user naming attribute
     */
    public static final String APPLICATION_USER_NAMING_ATTR = "cn";

    /**
     * Application special users container
     */
    public static final String SPECIAL_USERS_CONTAINER = "ou=DSAME Users";

    public static final String APPLICATION_CLASSNAME = 
        "com.sun.identity.authentication.modules.application.Application";

    /**
     * Active
     */
    public static final String ACTIVE = "ACTIVE";

    /**
     * Authentication Service Name
     */
    public static final String AUTH_SERVICE_NAME = "iPlanetAMAuthService";

    /**
     * LDAP Authentication Service Name
     */
    public static final String LDAP_SERVICE_NAME = "iPlanetAMAuthLDAPService";

    /**
     * HTTP Basic Authentication Service Name
     */
    public static final String AUTH_HTTP_BASIC_SERVICE_NAME =
        "iPlanetAMAuthHTTPBasicService";

    /**
     * Auth Configuration Service Name
     */
    public static final String AUTHCONFIG_SERVICE_NAME = 
        "iPlanetAMAuthConfiguration";

    /**
     * Platform Service Name
     */
    public static final String PLATFORM_SERVICE_NAME = 
        "iPlanetAMPlatformService";

    /**
     * Session Service Name
     */
    public static final String SESSION_SERVICE_NAME = "iPlanetAMSessionService";

    /**
     * Application Module Name
     */
    public static final String APPLICATION_MODULE = "Application";

    /**
     * FederationModule Name
     */
    public static final String FEDERATION_MODULE = "Federation";

    /**
     * SAML Module Name
     */
    public static final String SAML_MODULE = "SAML";

    /**
     * Attributes
     */
    public static final String AUTH_ATTR_PREFIX = "iplanet-am-auth-";

    public static final String AUTH_ATTR_PREFIX_NEW = "sunAMAuth";

    /**
     * LDAP Service Attributes
     */
    public static final String LDAP_SERVICE_PREFIX = AUTH_ATTR_PREFIX + "ldap-";

    public static final String LDAP_SERVER = LDAP_SERVICE_PREFIX + "server";

    public static final String LDAP_UNA = LDAP_SERVICE_PREFIX
            + "user-naming-attribute";

    public static final String LDAP_USERSEARCH = LDAP_SERVICE_PREFIX
            + "user-search-attributes";

    public static final String LDAP_SEARCHFILTER = LDAP_SERVICE_PREFIX
            + "search-filter";

    public static final String LDAP_BINDPWD = LDAP_SERVICE_PREFIX
            + "bind-passwd";

    public static final String LDAP_BASEDN = LDAP_SERVICE_PREFIX + "base-dn";

    public static final String LDAP_BINDDN = LDAP_SERVICE_PREFIX + "bind-dn";

    public static final String LDAP_SSL = LDAP_SERVICE_PREFIX + "ssl-enabled";

    public static final String LDAP_SEARCHSCOPE = LDAP_SERVICE_PREFIX
            + "search-scope";

    public static final String LDAP_RETURNUSERDN = LDAP_SERVICE_PREFIX
            + "return-user-dn";

    /**
     * SecurID Service Attribute
     */
    public static final String SECURID_SERVICE_PREFIX =
        AUTH_ATTR_PREFIX + "securid-";
    public static final String SECURID_CONFIG_PATH =
        SECURID_SERVICE_PREFIX + "server-config-path";

    /**
     * Platform Service Attributes
     */
    public static final String PLATFORM_CHARSET_ATTR = 
        "iplanet-am-platform-html-char-set";

    public static final String PLATFORM_LOCALE_ATTR = 
        "iplanet-am-platform-locale";

    public static final String PLATFORM_CLIENT_CHARSET_ATTR = 
        "iplanet-am-platform-client-charsets";

    public static final String PLATFORM_COOKIE_DOMAIN_ATTR = 
        "iplanet-am-platform-cookie-domains";

    public static final String SERVICE_STATUS_ATTR = 
        "iplanet-am-service-status";

    /**
     * Session Service Max Session Time Attribute
     */

    public static final String MAX_SESSION_TIME = 
        "iplanet-am-session-max-session-time";

    /**
     * Session Service Max Idle Time Attribute
     */

    public static final String SESS_MAX_IDLE_TIME = 
        "iplanet-am-session-max-idle-time";

    /**
     * Session Service Max Idle Time Attribute
     */

    public static final String SESS_MAX_CACHING_TIME = 
        "iplanet-am-session-max-caching-time";

    /**
     * inetdomainstatus
     */

    public static final String INETDOMAINSTATUS = "inetdoaminstatus";

    public static final String INETUSER_STATUS = "inetuserstatus";

    public static final String NSACCOUNT_LOCK = "nsaccountlock";

    public static final String PREFERRED_LOCALE = "preferredlocale";

    /**
     * Auth Locale Attribute
     */
    public static final String AUTH_LOCALE_ATTR = AUTH_ATTR_PREFIX + "locale";

    /**
     * Auth redirect url attributes
     */
    public static final String LOGIN_SUCCESS_URL = AUTH_ATTR_PREFIX
            + "login-success-url";

    public static final String LOGIN_FAILURE_URL = AUTH_ATTR_PREFIX
            + "login-failure-url";

    /**
     * User attributes
     */
    public static final String USER_ALIAS_ATTR = "iplanet-am-user-alias-list";

    public static final String LOGIN_STATUS = "iplanet-am-user-login-status";

    public static final String ACCOUNT_LIFE = "iplanet-am-user-account-life";

    public static final String USER_SUCCESS_URL = "iplanet-am-user-success-url";

    public static final String USER_FAILURE_URL = "iplanet-am-user-failure-url";

    public static final String POST_LOGIN_PROCESS = AUTH_ATTR_PREFIX
            + "post-login-process-class";

    /**
     * Auth attributes
     */

    public static final String AUTH_ALIAS_ATTR = AUTH_ATTR_PREFIX
            + "alias-attr-name";

    public static final String AUTH_USER_CONTAINER = AUTH_ATTR_PREFIX
            + "user-container";

    public static final String AUTH_DEFAULT_ROLE = AUTH_ATTR_PREFIX
            + "default-role";

    public static final String AUTH_NAMING_ATTR = AUTH_ATTR_PREFIX
            + "user-naming-attr";

    public static final String DYNAMIC_PROFILE = AUTH_ATTR_PREFIX
            + "dynamic-profile-creation";

    public static final String PERSISTENT_COOKIE_MODE = AUTH_ATTR_PREFIX
            + "persistent-cookie-mode";

    public static final String PERSISTENT_COOKIE_TIME = AUTH_ATTR_PREFIX
            + "persistent-cookie-time";

    public static final String AUTH_ALLOWED_MODULES = AUTH_ATTR_PREFIX
            + "allowed-modules";

    public static final String DEFAULT_AUTH_LEVEL = AUTH_ATTR_PREFIX
            + "default-auth-level";

    public static final String LOGIN_FAILURE_LOCKOUT = AUTH_ATTR_PREFIX
            + "login-failure-lockout-mode";

    public static final String LOGIN_FAILURE_STORE_IN_DS = 
        "sunStoreInvalidAttemptsInDS";

    public static final String LOCKOUT_DURATION = AUTH_ATTR_PREFIX
            + "lockout-duration";

    public static final String LOCKOUT_MULTIPLIER = 
            "sunLockoutDurationMultiplier";

    public static final String LOGIN_FAILURE_COUNT = AUTH_ATTR_PREFIX
            + "login-failure-count";

    public static final String LOGIN_FAILURE_DURATION = AUTH_ATTR_PREFIX
            + "login-failure-duration";

    public static final String USERNAME_GENERATOR = AUTH_ATTR_PREFIX
            + "username-generator-enabled";

    public static final String USERNAME_GENERATOR_CLASS = AUTH_ATTR_PREFIX
            + "username-generator-class";

    public static final String LOCKOUT_WARN_USER = AUTH_ATTR_PREFIX
            + "lockout-warn-user";

    public static final String LOCKOUT_ATTR_NAME = AUTH_ATTR_PREFIX
            + "lockout-attribute-name";

    public static final String LOCKOUT_ATTR_VALUE = AUTH_ATTR_PREFIX
            + "lockout-attribute-value";

    public static final String LOCKOUT_EMAIL = AUTH_ATTR_PREFIX
            + "lockout-email-address";

    public static final String INVALID_ATTEMPTS_DATA_ATTR_NAME = 
            "sunAMAuthInvalidAttemptsDataAttrName";

    public static final String ADMIN_AUTH_MODULE = AUTH_ATTR_PREFIX
            + "admin-auth-module";

    public static final String AUTHENTICATORS = AUTH_ATTR_PREFIX
            + "authenticators";

    public static final String SLEEP_INTERVAL = AUTH_ATTR_PREFIX
            + "sleep-interval";

    public static final String AUTH_ID_TYPE_ATTR = "sunAMIdentityType";

    /**
     * SPI related constants
     */
    public static final String DEFAULT_USERID_GENERATOR_CLASS = 
        "com.sun.identity.authentication.spi.DefaultUserIDGenerator";

    public static final String ADMINISTRATION_SERVICE = 
        "iPlanetAMAdminConsoleService";

    public static final String CONSOLE_SERVICE = "adminconsoleservice";

    public static final String USERID_PASSWORD_VALIDATION_CLASS = 
        "iplanet-am-admin-console-user-password-validation-class";

    public static final String SHARED_STATE_USERNAME = 
        "javax.security.auth.login.name";

    public static final String SHARED_STATE_PASSWORD = 
        "javax.security.auth.login.password";

    public static final String SHARED_STATE_ENABLED = AUTH_ATTR_PREFIX
            + "shared-state-enabled";

    public static final String STORE_SHARED_STATE_ENABLED = AUTH_ATTR_PREFIX
            + "store-shared-state-enabled";

    public static final String SHARED_STATE_BEHAVIOR_PATTERN = AUTH_ATTR_PREFIX
            + "shared-state-behavior-pattern";


    /**
     * Log and debug file names
     */

    public static final String AUTH_ACCESS_LOG_NAME = "amAuthentication.access";

    public static final String AUTH_ERROR_LOG_NAME = "amAuthentication.error";

    public static final String LDAP_DEBUG_NAME = "amAuthLDAP";

    /**
     * Resource bundle names
     */

    public static final String AUTH_BUNDLE_NAME = "amAuth";

    /**
     * invalidate PCookie
     */
    public static final String PCOOKIE = "iPSPCookie";

    public static final String INVALID_PCOOKIE = "inPersistentCookie";

    /**
     * Default Values
     */

    public static final String DEFAULT_MAX_SESS_TIME = "120";

    public static final String DEFAULT_MAX_SESS_IDLE_TIME = "30";

    public static final String DEFAULT_MAX_SESS_CACHING_TIME = "3";

    public static final String DEFAULT_LOCALE = "en_US";
    
    /**
     * Option key values for User Profile choice selection
     */

    public static final String REQUIRED = "Required";

    public static final String CREATE = "Create";

    public static final String IGNORE = "Ignore";

    public static final String CREATE_WITH_ALIAS = "CreateWithAlias";

    /**
     * Property Names to be stored in SSOToken
     */

    public static final String PRINCIPAL = "Principal";

    public static final String ORGANIZATION = "Organization";

    public static final String AUTH_TYPE = "AuthType";

    public static final String AUTH_LEVEL = "AuthLevel";

    public static final String SERVICE = "Service";

    public static final String HOST = "Host";
    
    public static final String USER_PROFILE = "UserProfile";
    
    public static final String LOGIN_URL = "loginURL";

    public static final String FULL_LOGIN_URL = "FullLoginURL";
    
    public static final String SUCCESS_URL = "successURL";

    public static final String POST_PROCESS_SUCCESS_URL =
        "PostProcessSuccessURL";
    
    public static final String USER_ID = "UserId";
    
    public static final String USER_TOKEN = "UserToken";
    
    public static final String LOCALE = "Locale";
    
    public static final String CHARSET = "CharSet";
    
    public static final String CLIENT_TYPE = "clientType";  
    
    public static final String AUTH_INSTANT = "authInstant";
    
    public static final String MODULE_AUTH_TIME = "moduleAuthTime";
    
    public static final String PRINCIPALS = "Principals";
    
    public static final String INDEX_TYPE = "IndexType";
    
    public static final String ROLE = "Role";    

    public static final String FILE_PATH_PROPERTY = "filePath";

    public static final String CONTENT_TYPE_PROPERTY = "contentType";

    public static final String COOKIE_SUPPORT_PROPERTY = "cookieSupport";

    public static final String IGNORE_HOST_HEADER_PROPERTY = "ignoreHostHeader";

    public static final String CHARSETS_PROPERTY = "charsets";

    public static final String COOKIE_DETECT_PROPERTY = "cookieDetect";

    public static final String ACCEPT_LANG_HEADER = "Accept-Language";

    public static final String HOST_HEADER = "host";

    public static final String TRUE_VALUE = "true";

    public static final String FALSE_VALUE = "false";

    public static final String POST_AUTH_PROCESS_INSTANCE = 
            "PostAuthProcessInstance";

    /**
     * Delimiters
     */

    public static final String PIPE_SEPARATOR = "|";

    public static final String ASTERISK = "*";

    public static final String URL_SEPARATOR = "://";

    public static final String SEMICOLON = ";";

    public static final String COLON = ":";

    public static final String QUERY = "?";

    public static final String COMMA = ",";

    public static final String EMPTY_STRING = "";

    public static final String EQUAL = "=";

    public static final String PERCENT = "%";

    /**
     * login states
     */
    public static final int LOGIN_IGNORE = 0;

    public static final int LOGIN_START = 1;

    public static final int LOGIN_SUCCEED = -1;

    public static final int LOGIN_CHALLENGE = 2;

    // next three added for SecurID
    public static final int LOGIN_NEXT_TOKEN = 3;

    public static final int LOGIN_SYS_GEN_PIN = 4;

    public static final int LOGIN_NEW_PIN_NEXT_TOKEN = 5;

    /** Organization Attribute */

    public static final String ORG_ATTRIBUTE = "o";

    /** Resource Lookup Related variableds */

    public static final String CONFIG_DIR = "config";

    public static final String AUTH_DIR = "auth";

    public static final String DEFAULT_DIR = "default";

    /** Auth Cookie Name */
    public static final String AUTH_COOKIE_NAME = "AMAuthCookie";

    /** Dist Auth Cookie Name */
    public static final String DIST_AUTH_COOKIE_NAME = "AMDistAuthCookie";

    public static final String SERVER_SUBSCHEMA = "serverconfig";

    public static final String MODULE_INSTANCES_ATTR = AUTH_ATTR_PREFIX
            + "module-instances";

    public static final String AUTHCONFIG_ADMIN = AUTH_ATTR_PREFIX
            + "admin-auth-module";

    public static final String AUTHCONFIG_ORG = AUTH_ATTR_PREFIX + "org-config";

    public static final String AUTHCONFIG_ROLE = AUTH_ATTR_PREFIX
            + "configuration";

    public static final String AUTHCONFIG_USER = "iplanet-am-user-auth-config";

    public static final String IDREPO_SVC_NAME = "sunIdentityRepositoryService";

    public static final String AGENT_ID_TYPE = "Agent";

    public static final String USER_ID_TYPE = "User";

    // blank string for service configurations.
    public static final String BLANK = "[Empty]";

    // revision number for iPlanetAMAuthService in 7.0
    public static final int AUTHSERVICE_REVISION7_0 = 30;

    // attribute to identify the auth module instance name passed in
    // AMConfiguration
    public static final String MODULE_INSTANCE_NAME = "moduleInstanceName";

    // AuthContextLocal object index name in the transient HttpSession
    public static final String AUTH_CONTEXT_OBJ = "authContextObject";

    // Attribute to enable or disable module based auth
    public static final String MODULE_BASED_AUTH = "sunEnableModuleBasedAuth";

    // Attribute to check if Remote Auth Security is enabled
    public static final String REMOTE_AUTH_APP_TOKEN_ENABLED = 
        "sunRemoteAuthSecurityEnabled";

    // Key in locale file for exceeding auth retry limit error
    public static final String EXCEED_RETRY_LIMIT = "ExceedRetryLimit";

    // Key in locale file for server unwilling error (mapped to Connection Failed)
    public static final String SERVER_UNWILLING = "FConnect";

    // Property to store the Distributed Authentication Login URL in SSOToken
    public static final String DISTAUTH_LOGINURL = "DistAuthLoginURL";

    // Property to store the  User Attribute to Session Attribute Mapping
    public static final String USER_SESSION_MAPPING = 
        "sunAMUserAttributesSessionMapping";
    
     // Key in shared state for composite advice
     public static final String COMPOSITE_ADVICE_XML =
         "CompositeAdviceXML";

    // Indicates if Post Process Instances Need be added to Session
    public static final String KEEP_POSTPROCESS_IN_SESSION = 
        "sunAMAuthKeepPostProcessInstances";

    // Indicates if Auth Module Instances Need be added to Session
    public static final String KEEP_MODULES_IN_SESSION = 
        "sunAMAuthKeepAuthModuleIntances";

    // Property name for Post Process Instances in Session
    public static final String POSTPROCESS_INSTANCE_SET = 
        "sunAMAuthPostProcessInstanceSet";

    // Property name for Login Context in Session
    public static final String LOGIN_CONTEXT = 
        "sunAMAuthLoginContext";
    
    // Property name for user password attribute
    public static final String ATTR_USER_PASSWORD = "userpassword";
    
    // Property name for valid go to url domains attribute
    public static final String AUTH_GOTO_DOMAINS = AUTH_ATTR_PREFIX	 
        + "valid-goto-domains";    

    /**
     * Property name for persistent cookie auth level.
     */
    public static final String PCOOKIE_AUTH_LEVEL = "openam-auth-pcookie-auth-level";

    /**
     * The AuthType string for persistent cookie based logins.
     */
    public static final String PCOOKIE_AUTH_TYPE = "PCookie";
}
