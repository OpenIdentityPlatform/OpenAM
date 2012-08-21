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
 * $Id: SetupConstants.java,v 1.46 2009/12/23 00:21:38 goodearth Exp $
 *
 */

/*
 * Portions Copyrighted 2010-2011 ForgeRock AS
 */

package com.sun.identity.setup;

/**
 * This defines the constants used in setup package.
 */
public interface SetupConstants {
    /**
     * Setup Debug name.
     */
    static final String DEBUG_NAME = "amSetupServlet";
    
    /**
     * Default Platform Locale.
     */
    static final String DEFAULT_PLATFORM_LOCALE = "en_US";

    /**
     * Flag to overwrite <code>AMConfig.properties</code>
     */
    static final String AMC_OVERRIDE_PROPERTY = "com.sun.identity.overrideAMC";

    /**
     * Encryption property in configuration file.
     */
    static final String ENC_PWD_PROPERTY = "am.encryption.pwd";

    /**
     * Configurator plugins properties file name.
     */
    static final String PROPERTY_CONFIGURATOR_PLUGINS = "configuratorPlugins";

    /**
     * Configurator plugins class name.
     */
    static final String KEY_CONFIGURATOR_PLUGINS = "configurator.plugins";
    
    /**
     * <code>AMConfig.properties</code> file name.
     */
    static final String AMCONFIG_PROPERTIES = "AMConfig.properties";

    /**
     * Server properties file name.
     */
    static final String SITE_PROPERTIES = "site.properties";

    /**
     * Encrypted Admin password.
     */
    static final String ENCRYPTED_ADMIN_PWD = "ENCADMINPASSWD";

    /**
     * Hash Admin password.
     */
    static final String HASH_ADMIN_PWD = "HASHADMINPASSWD";

    /**
     * Userstore password.
     */
    static final String USERSTORE_PWD = "USERSTORE_PASSWD";

    /**
     * LDAP user password.
     */
    static final String LDAP_USER_PWD = "AMLDAPUSERPASSWD";

    /**
     * Encrypted LDAP user password.
     */
    static final String ENCRYPTED_LDAP_USER_PWD = "ENCLDAPUSERPASSWD";

    /**
     * Hash LDAP user password.
     */
    static final String HASH_LDAP_USER_PWD = "HASHLDAPUSERPASSWD";

    /**
     * Encrypted directory Admin password.
     */
    static final String ENCRYPTED_AD_ADMIN_PWD = "ENCADADMINPASSWD";

    /**
     * SSHA512 Encrypted directory Admin password.
     */
    static final String SSHA512_LDAP_USERPWD = "SSHA512LDAPUSERPWD";

    /**
     * Properties file name that contains the names of all services that need
     * to be registered by the configurator.
     */
    static final String PROPERTY_FILENAME = "serviceNames";
    
    /**
     * Property key in <code>PROPERTY_FILENAME</code> file that has all
     * services that need to be registered by the configurator.
     */
    static final String SERVICE_NAMES = "serviceNames";

    /**
     * Properties file name that contains the names of service schema 
     * files be loaded by the configurator.
     */
    static final String SCHEMA_PROPERTY_FILENAME = "schemaNames";

    /**
     * Property keys in <code>SCHEMA_PROPERTY_FILENAME</code> file.
     * Sun Directory Server Schema File for configuration data.
     */
    static final String DS_SMS_PROPERTY_FILENAME = "dsSmsSchema";
    
    /**
     * Property keys in <code>SCHEMA_PROPERTY_FILENAME</code> file.
     * Microsoft Active Directory Schema File for configuration data.
     */
    static final String AD_SMS_PROPERTY_FILENAME = "adSmsSchema";
    
    /**
     * Property keys in <code>SCHEMA_PROPERTY_FILENAME</code> file.
     * Microsoft Active Directory Schema File for configuration data.
     */
    static final String OPENDS_SMS_PROPERTY_FILENAME = "opendsSmsSchema";
    
    /**
     * Property keys in <code>SCHEMA_PROPERTY_FILENAME</code> file.
     * Sun Directory Server Schema File for user management data.
     */
    static final String SUNDS_LDIF = "sundsSchema";

    /**
     * Property keys in <code>SCHEMA_PROPERTY_FILENAME</code> file.
     * OpenDS Schema File for user management data.
     */
    static final String OpenDS_LDIF = "opendsUserSchema";

    /**
     * Property keys in <code>SCHEMA_PROPERTY_FILENAME</code> file.
     * Active Directory Schema File for user management data.
     */
    static final String AD_LDIF = "adUserSchema";

    /**
     * Property keys in <code>SCHEMA_PROPERTY_FILENAME</code> file.
     * Active Directory Application Mode Schema File for user management data.
     */
    static final String ADAM_LDIF = "adamUserSchema";

    /**
     * Property keys in <code>SCHEMA_PROPERTY_FILENAME</code> file.
     * IBM Tivoli Directory Schema File for user management data.
     */
    static final String TIVOLI_LDIF = "tivoliUserSchema";

    /**
     * User Management datastore option - Embedded DS
     */
    static final String UM_EMBEDDED = "embedded";

    /**
     * User Management datastore option - Sun DS.
     */
    static final String UM_LDAPv3ForSUNDS = "LDAPv3ForSUNDS";

    /**
     * User Management datastore option - OpenDS.
     */
    static final String UM_LDAPv3ForOpenDS = "LDAPv3ForOpenDS";

    /**
     * User Management datastore option - AD with Host & Port.
     */
    static final String UM_LDAPv3ForAD = "LDAPv3ForAD";

    /**
     * User Management datastore option - AD with Domain Name.
     */
    static final String UM_LDAPv3ForADDC = "LDAPv3ForADDC";

    /**
     * User Management datastore option - ADAM.
     */
    static final String UM_LDAPv3ForADAM = "LDAPv3ForADAM";

    /**
     * User Management datastore option - IBM Tivoli DS.
     */
    static final String UM_LDAPv3ForTivoli = "LDAPv3ForTivoli";

    /**
     * Type of Data store used for storing the configuration files.
     * Embedded
     */
    static final String SMS_EMBED_DATASTORE = "embedded";

    /**
     * Type of Data store used for storing the configuration files.
     * Remote
     */
    static final String SMS_REMOTE_DATASTORE = "remote";

    /**
     * Type of Data store used for storing the configuration files.
     * Sun Directory Server.
     */
    static final String SMS_DS_DATASTORE = "dirServer";

    /**
     * Type of Data store used for storing the configuration files.
     * Active Directory.
     */
    static final String SMS_AD_DATASTORE = "activeDir";
    
    /**
     * Type of Data store used for storing the configuration files.
     * <code>OpenDS</code>.
     */
    static final String SMS_OPENDS_DATASTORE = "opends";
    
    /**
     * Type of Data store used for storing the configuration files.
     * Native Operating System file system.
     */
    static final String SMS_FF_DATASTORE = "flatfile";

    /**
     * Variable for org root suffix.
     */
    static final String ORG_ROOT_SUFFIX = "ORG_ROOT_SUFFIX";

    /**
     * Variable for relative distinguish name.
     */
    static final String RS_RDN = "RS_RDN";

    /**
     * Variable for default organization.
     */
    static final String DEFAULT_ORG = "DEFAULT_ORG";

    /**
     * Variable for default organization base suffix.
     */
    static final String ORG_BASE = "ORG_BASE";

    /**
     * Variable for Directory server protocol.
     */
    static final String DS_PROTO = "DS_PROTO";

    /**
     * Variable for normalized organization base suffix.
     */
    static final String NORMALIZED_ORG_BASE = "NORMALIZED_ORGBASE";

    /**
     * Variable for normalized relative distinguish name.
     */
    static final String NORMALIZED_RS = "NORMALIZED_RS";

    /**
     * Variable for normalized root suffix.
     */
    static final String NORMALIZED_ROOT_SUFFIX = "NM_ORG_ROOT_SUFFIX";

    /**
     * Flag to indicate if DIT is loaded in Directory Server.
     */
    static final String DIT_LOADED = "DIT_LOADED";
    
    /**
     * Configuration Variable for product name.
     */
    static final String CONFIG_VAR_PRODUCT_NAME = "IS_PRODNAME";
    
    /**
     * Configuration Variable for lagency console deployment URI.
     */
    static final String CONFIG_VAR_OLD_CONSOLE_URI  = "OLDCON_DEPLOY_URI";

    /**
     * Configuration variable for Platform Locale.
     */
    static final String CONFIG_VAR_PLATFORM_LOCALE = "PLATFORM_LOCALE";

    /**
     * Configuration Variable for console deployment URI.
     */
    static final String CONFIG_VAR_CONSOLE_URI  = "CONSOLE_URI";
    
    /**
     * Configuration Variable for server protocol.
     */
    static final String CONFIG_VAR_SERVER_PROTO = "SERVER_PROTO";
    
    /**
     * Configuration Variable for server host.
     */
    static final String CONFIG_VAR_SERVER_HOST = "SERVER_HOST";
    
    /**
     * Configuration Variable for server port.
     */
    static final String CONFIG_VAR_SERVER_PORT = "SERVER_PORT";
    
    /**
     * Configuration Variable for server deployment URI.
     */
    static final String CONFIG_VAR_SERVER_URI = "SERVER_URI";

    /**
     * Configuration Variable for server URL.
     */
    static final String CONFIG_VAR_SERVER_URL = "SERVER_URL";

    /**
     * Configuration Variable for encryption key.
     */
    static final String CONFIG_VAR_ENCRYPTION_KEY = "AM_ENC_KEY";

    /**
     * Configuration Variable for directory server administrator password.
     */
    static final String CONFIG_VAR_DS_MGR_PWD = "DS_DIRMGRPASSWD";

    /**
     * Configuration Variable for encrypted directory server administrator
     * password.
     */
    static final String ENCRYPTED_SM_DS_PWD = "ENCDSDIRMGRPASSWD";

    /**
     * Configuration Variable for directory server administrator DN.
     */
    static final String CONFIG_VAR_DS_MGR_DN = "DS_DIRMGRDN";

    /**
     * Configuration Variable for directory server protocol.
     */
    static final String CONFIG_VAR_DIRECTORY_SERVER_SSL = "DIRECTORY_SSL";

    /**
     * Configuration Variable for directory server host.
     */
    static final String CONFIG_VAR_DIRECTORY_SERVER_HOST = "DIRECTORY_SERVER";

    /**
     * Configuration Variable for directory server port.
     */
    static final String CONFIG_VAR_DIRECTORY_SERVER_PORT = "DIRECTORY_PORT";

    /**
     * Configuration Variable for directory server admin port.
     */
    static final String CONFIG_VAR_DIRECTORY_ADMIN_SERVER_PORT = "DIRECTORY_ADMIN_PORT";

    /**
     * Configuration Variable for directory server jmx port.
     */
    static final String CONFIG_VAR_DIRECTORY_JMX_SERVER_PORT = "DIRECTORY_JMX_PORT";

    /**
     * Configuration Variable for administrator password.
     */
    static final String CONFIG_VAR_ADMIN_PWD = "ADMIN_PWD";

    /**
     * Configuration Variable for confirm administrator password.
     */
    static final String CONFIG_VAR_CONFIRM_ADMIN_PWD  = "ADMIN_CONFIRM_PWD";

    /**
     * Configuration Variable for <code>URLAccessAgent</code> password.
     */
    static final String CONFIG_VAR_AMLDAPUSERPASSWD = "AMLDAPUSERPASSWD";

    /**
     * Configuration Variable for <code>URLAccessAgent</code> password.
     */
    static final String CONFIG_VAR_AMLDAPUSERPASSWD_CONFIRM = "AMLDAPUSERPASSWD_CONFIRM";

    /**
     * Configuration Variable for server cookie domain.
     */
    static final String CONFIG_VAR_COOKIE_DOMAIN = "COOKIE_DOMAIN";
    
    /**
     * Configuration Variable for installation base directory.
     */
    static final String CONFIG_VAR_BASE_DIR  = "BASE_DIR";
    
    /**
     * Configuration Variable for root suffix.
     */
    static final String CONFIG_VAR_ROOT_SUFFIX = "ROOT_SUFFIX";

    /**
     * Configuration Variable for Session root suffix.
     */
    static final String CONFIG_VAR_SESSION_ROOT_DN = "SESSION_ROOT_DN";

    /**
     * Configuration Variable for Session Store Type.
     */
    static final String CONFIG_VAR_SESSION_STORE_TYPE = "SESSION_STORE_TYPE";

    /**
     * Configuration Variable for bootstarp file base directory.
     */
    static final String CONFIG_VAR_BOOTSTRAP_BASE_DIR = ".openamcfg";
    static final String CONFIG_VAR_BOOTSTRAP_LEGACY_BASE_DIR = ".openssocfg";

    /**
     * Configuration Variable for bootstarp file base prefix.
     */
    static final String CONFIG_VAR_BOOTSTRAP_BASE_PREFIX = "AMConfig";


    /**
     * Configuration Variable for the type of data store used.
     */
    static final String CONFIG_VAR_DATA_STORE = "DATA_STORE";

    /**
     * Configuration Variable for Directory Server config store property.
     */
    static final String CONFIG_VAR_DS_DATASTORE_CLASS = "DS_OBJECT_CLASS";

    /**
     * Configuration Variable for Embedded DS config store property.
     */
    static final String CONFIG_VAR_EMBEDDED_DATASTORE_CLASS = "EMBEDDED_DS_OBJECT_CLASS";

    /**
     * Configuration Variable for Flat-file config store property.
     */
    static final String CONFIG_VAR_SMS_DATASTORE_CLASS = "SMS_OBJECT_CLASS";

    /**
     * Configuration Variable for default shared secret key.
     */
    static final String CONFIG_VAR_DEFAULT_SHARED_KEY = "KmhUnWR1MYWDYW4xuqdF5nbm+CXIyOVt";

    /**
     * Configuration variable for storing site confiuration data.
     */
    static final String CONFIG_VAR_SITE_CONFIGURATION = "SITE_CONFIGURATION_MAP";
    
    /**
     * Configuration variable for name of site configuration being created.
     */
    static final String LB_SITE_NAME = "wizardLoadBalancerSiteName";

    /**
     * Configuration variable for name of site configuration primary url.
     */
    static final String LB_PRIMARY_URL = "wizardLoadBalancerURL";

    /**
     * Schema Template Directory.
     */
    static final String SCHEMA_TEMPLATE_DIRECTORY = "/WEB-INF/template/sms";

    /**
     * Configuration Variable for service management root suffix with carat 
     * suffix.
     */
    static final String SM_ROOT_SUFFIX_HAT = "SM_ROOT_SUFFIX_HAT";

    /**
     * Variable for default organization base suffix for service management 
     * node.
     */
    static final String SM_CONFIG_BASEDN = "SM_CONFIG_BASEDN";

    /**
     * Variable for default organization base suffix RDN's value part. 
     */

    static final String SM_CONFIG_BASEDN_RDNV = "SM_CONFIG_BASEDN_RDNV";

    /**
     * Configuration Variable for service management root suffix.
     */
    static final String SM_CONFIG_ROOT_SUFFIX = "SM_CONFIG_ROOT_SUFFIX";

    /**
     * Datastore notification flag.
     */
    static final String DATASTORE_NOTIFICATION = "DATASTORE_NOTIFICATION";

    /**
     * Properties file that contain bootstrap information.
     */
    static final String BOOTSTRAP_PROPERTIES_FILE = "bootstrap";

    /**
     * Property to set the root of configuration directory.
     */
    static final String PRESET_CONFIG_DIR = "configuration.dir";

    /**
     * JVM System Property to set the root of configuration directory.
     */
    static final String JVM_PROP_PRESET_CONFIG_DIR =
        "com.sun.identity.configuration.directory";
    
    /**
     * Tag in bootstrap file for real path substitution.
     */
    static final String TAG_REALPATH = "@REAL_PATH@";
  
    /**
     * Disabling Persistent Searches in Directory Server.
     */

    static final String DISABLE_PERSISTENT_SEARCH = "DISABLE_PERSISTENT_SEARCH";

    /**
     * OpenDS Replication : Flag to indicate whether multi server sharing 
     * needs configured.
     */
    static final String DS_EMB_REPL_FLAG = "DS_EMB_REPL_FLAG";

    /**
     * Value of configurator checkbox representing 
     * <code>DS_EMB_REPL_FLAG</code>
     */
    static final String DS_EMP_REPL_FLAG_VAL = "embReplFlag";

    /**
     * OpenDS Replication : local server  replication port.
     */
    static final String DS_EMB_REPL_REPLPORT1 = "DS_EMB_REPL_REPLPORT1";

    /**
     * OpenDS Replication : remote server replication port.
     */
    static final String DS_EMB_REPL_REPLPORT2 = "DS_EMB_REPL_REPLPORT2";

    /**
     * OpenDS Replication : remote server host.
     */
    static final String DS_EMB_REPL_HOST2 = "DS_EMB_REPL_HOST2";

    /**
     * OpenDS Replication : remote server host.
     */
    static final String DS_EMB_EXISTING_SERVERID = "existingserverid";

    /**
     * OpenDS Replication : remote OpenDS server admin port.
     */
    static final String DS_EMB_REPL_ADMINPORT2 = "DS_EMB_REPL_ADMINPORT2";
    
    /**
     * Protocol of IDRepo entry to create for user data store.
     */
    static final String USER_STORE_SSL = "userStoreSSL";
    
    /**
     * Type of IDRepo entry to create for user data store.
     */
    static final String USER_STORE_TYPE = "userStoreType";
    
    /**
     * Domain Name of the Active Directory ldap store being used.
     */
    static final String USER_STORE_DOMAINNAME = "userStoreDomainName";

    /**
     * Host name for the ldap store being used.
     */
    static final String USER_STORE_HOST = "userStoreHostName";
    
    /**
     * Port number of the ldap server.
     */
    static final String USER_STORE_PORT = "userStorePort";
    
    /**
     * Root suffix for the user data store.
     */
    static final String USER_STORE_ROOT_SUFFIX = "userStoreRootSuffix";
    
    /**
     * Login ID of the use with access rights to the data store.
     */
    static final String USER_STORE_LOGIN_ID = "userStoreLoginID";
    
    /**
     * Password for the user login ID.
     */
    static final String USER_STORE_LOGIN_PWD = "userStoreLoginPassword";

    /**
     * <code>rsa_api.properties</code> file name.
     */
    static final String SECURID_PROPERTIES = "rsa_api.properties";

    /**
     * <code>VERSION</code> property in serviceDefaultValues.properties.
     */
    static final String VERSION = "VERSION";

    /**
     * Solaris operating system
     */
    static final String SOLARIS = "solaris";

    /**
     * Solaris/x86 operating system
     */
    static final String X86SOLARIS = "x86solaris";

    /**
     * SunOS operating system
     */
    static final String SUNOS = "sunos";

    /**
     * Linux operating system
     */
    static final String LINUX = "linux";

    /**
     * Windows operating system
     */
    static final String WINDOWS = "windows";

    /**
     * Default PAM Service Name for Linux
     */
    static final String LINUX_PAM_SVC_NAME = "password";

    /**
     * PAM Service Name tag static final String
     */
    static final String PAM_SERVICE_NAME = "PAM_SERVICE_NAME";

    /**
     * PC-based architecture
     */
    static final String X86 = "86";
    static final String AMD = "amd";

    /**
     * User Management Datastore SSL tag.
     */
    static final String UM_SSL = "UM_SSL";
    
    /**
     * User Management Datastore Bind DN.
     */
    static final String UM_DS_DIRMGRDN = "UM_DS_DIRMGRDN";

    /**
     * User Management Datastore Bind Password.
     */
    static final String UM_DS_DIRMGRPASSWD = "UM_DS_DIRMGRPASSWD";
    
    /**
     * User Management Datastore Host tag.
     */
    static final String UM_DIRECTORY_SERVER = "UM_DIRECTORY_SERVER";

    /**
     * User Management Datastore Port tag.
     */
    static final String UM_DIRECTORY_PORT = "UM_DIRECTORY_PORT";

    /**
     * User Management Datastore Normalized Root Suffix tag.
     */
    static final String UM_NORMALIZED_ORGBASE = "UM_NORMALIZED_ORGBASE";
    
    /**
     * Name of the config param file
     */
    static final String CONFIG_PARAM_FILE = "/.configParam";
}
