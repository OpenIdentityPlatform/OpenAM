/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.sun.identity.qatest.common.authentication;

/**
 *
 * @author cmwesley
 */
public interface AuthConstants {
    /**
     * Prefix for authentication module instance properties in this format:
     * <instance-prefix>.<attribute>.<instance-index>
     */
    public static final String LDAP_INSTANCE_PREFIX = "ldap";
    public static final String DATASTORE_INSTANCE_PREFIX = "datastore";
    public static final String MEMBERSHIP_INSTANCE_PREFIX = "membership";
    public static final String ANONYMOUS_INSTANCE_PREFIX = "anonymous";
    public static final String AD_INSTANCE_PREFIX = "ad";
    public static final String NT_INSTANCE_PREFIX = "nt";
    public static final String JDBC_INSTANCE_PREFIX = "jdbc";
    public static final String RADIUS_INSTANCE_PREFIX = "radius";
    public static final String UNIX_INSTANCE_PREFIX = "unix";
    public static final String SECURID_INSTANCE_PREFIX = "securid";

    /**
     * Constant strings for all instance attributes
     */
    public static final String NUMBER_OF_INSTANCES = "number-of-instances";
    public static final String INSTANCE_NAME = "module-subconfig-name";
    public static final String INSTANCE_REALM = "realm";
    public static final String INSTANCE_SERVICE = "module-service-name";

    /**
     * Constant strings for LDAP instance attributes
     */
    public static final String LDAP_AUTH_SERVER = "iplanet-am-auth-ldap-server";
    public static final String LDAP_AUTH_SERVER2 =
            "iplanet-am-auth-ldap-server2";
    public static final String LDAP_AUTH_BASEDN =
            "iplanet-am-auth-ldap-base-dn";
    public static final String LDAP_AUTH_BINDDN =
            "iplanet-am-auth-ldap-bind-dn";
    public static final String LDAP_AUTH_BIND_PASSWD =
            "iplanet-am-auth-ldap-bind-passwd";
    public static final String LDAP_AUTH_USER_NAMING_ATTR =
            "iplanet-am-auth-ldap-user-naming-attribute";
    public static final String LDAP_AUTH_USER_SEARCH_ATTR =
            "iplanet-am-auth-ldap-user-search-attribute";
    public static final String LDAP_AUTH_SEARCH_FILTER =
            "iplanet-am-auth-ldap-search-filter";
    public static final String LDAP_AUTH_SEARCH_SCOPE =
            "iplanet-am-auth-ldap-search-scope";
    public static final String LDAP_AUTH_SSL_ENABLED =
            "iplanet-am-auth-ldap-ssl-enabled";
    public static final String LDAP_AUTH_RETURN_USERDN =
            "iplanet-am-auth-ldap-return-user-dn";
    public static final String LDAP_AUTH_LEVEL =
            "iplanet-am-auth-ldap-auth-level";
    public static final String LDAP_AUTH_SERVER_CHECK =
            "iplanet-am-auth-ldap-server-check";
    public static final String LDAP_AUTH_USER_CREATION_ATTR_LIST =
            "iplanet-am-ldap-user-creation-attr-list";

    /**
     * Constant strings for DataStore authentication instance attributes
     */
    public static final String DATASTORE_AUTH_LEVEL =
            "sunAMAuthDataStoreAuthLevel";

    /**
     * Constant strings for Membership authentication instance attributes
     */
    public static final String MEMBERSHIP_AUTH_MIN_PASSWORD_LENGTH =
            "iplanet-am-auth-membership-min-password-length";
    public static final String MEMBERSHIP_AUTH_DEFAULT_ROLES =
            "iplanet-am-auth-membership-default-roles";
    public static final String MEMBERSHIP_AUTH_DEFAULT_USER_STATUS =
            "iplanet-am-auth-membership-default-user-status";
    public static final String MEMBERSHIP_AUTH_LEVEL =
            "iplanet-am-auth-membership-auth-level";

    /**
     * Constant strings for Anonymous authentication instance attributes
     */
    public static final String ANONYMOUS_AUTH_LEVEL =
            "iplanet-am-auth-anonymous-auth-level";
    public static final String ANONYMOUS_AUTH_USERS_LIST =
            "iplanet-am-auth-anonymous-users-list";
    public static final String ANONYMOUS_AUTH_CASE_SENSITIVE =
            "iplanet-am-auth-anonymouls-case-sensitive";
    public static final String ANONYMOUS_AUTH_DEFAULT_USER_NAME =
            "iplanet-am-auth-anonymous-default-user-name";
    
    /**
     * Constant strings for AD authentication instance attributes
     */
    public static final String AD_AUTH_SERVER = "iplanet-am-auth-ldap-server";
    public static final String AD_AUTH_SERVER2 = "iplanet-am-auth-ldap-server2";
    public static final String AD_AUTH_BASEDN = "iplanet-am-auth-ldap-base-dn";
    public static final String AD_AUTH_BINDDN = "iplanet-am-auth-ldap-bind-dn";
    public static final String AD_AUTH_BIND_PASSWD =
            "iplanet-am-auth-ldap-bind-passwd";
    public static final String AD_AUTH_USER_NAMING_ATTR =
            "iplanet-am-auth-ldap-user-naming-attribute";
    public static final String AD_AUTH_USER_SEARCH_ATTR =
            "iplanet-am-auth-ldap-user-search-attribute";
    public static final String AD_AUTH_SEARCH_FILTER =
            "iplanet-am-auth-ldap-search-filter";
    public static final String AD_AUTH_SEARCH_SCOPE =
            "iplanet-am-auth-ldap-search-scope";
    public static final String AD_AUTH_SSL_ENABLED =
            "iplanet-am-auth-ldap-ssl-enabled";
    public static final String AD_AUTH_RETURN_USERDN =
            "iplanet-am-auth-ldap-return-user-dn";
    public static final String AD_AUTH_LEVEL = "sunAMAuthADAuthLevel";
    public static final String AD_AUTH_SERVER_CHECK =
            "iplanet-am-auth-ldap-server-check";
    public static final String AD_AUTH_USER_CREATION_ATTR_LIST =
            "iplanet-am-ldap-user-creation-attr-list";  
    
    /**
     * Constant strings for Windows NT authentication instance attributes
     */
    public static final String NT_AUTH_DOMAIN = "iplanet-am-auth-nt-domain";
    public static final String NT_AUTH_HOST = "iplanet-am-auth-nt-host";
    public static final String NT_AUTH_SAMBA_CONFIG_FILE =
            "iplanet-am-auth-samba-config-file";
    public static final String NT_AUTH_LEVEL = "iplanet-am-auth-nt-auth-level";
    
    /**
     * Constant strings for JDBC authentication instance attributes
     */
    public static final String JDBC_AUTH_CONNECTION_TYPE =
            "sunAMAuthJDBCConectionType";
    public static final String JDBC_AUTH_JNDI_NAME = "sunAMAuthJDBCJndiName";
    public static final String JDBC_AUTH_DRIVER = "sunAMAuthJDBCDriver";
    public static final String JDBC_AUTH_URL = "sunAMAuthJDBCUrl";
    public static final String JDBC_AUTH_DBUSER = "sunAMAuthJDBCDbuser";
    public static final String JDBC_AUTH_DBPASSWORD = "sunAMAuthJDBCDbpassword";
    public static final String JDBC_AUTH_PASSWORD_COLUMN =
            "sunAMAuthJDBCPasswordColumn";
    public static final String JDBC_AUTH_STATEMENT = "sunAMAuthJDBCStatement";
    public static final String JDBC_AUTH_PASSWORD_SYNTAX_TRANSFORM_PLUGIN =
            "sunAMAuthJDBCPasswordSyntaxTransformPlugin";
    public static final String JDBC_AUTH_LEVEL = "sunAMAuthJDBCAuthLevel";

    /**
     * Constant strings for RADIUS authentication instance attributes
     */
    public static final String RADIUS_AUTH_SERVER =
            "iplanet-am-auth-radius-server1";
    public static final String RADIUS_AUTH_SERVER2 =
            "iplanet-am-auth-radius-server2";
    public static final String RADIUS_AUTH_SECRET =
            "iplanet-am-auth-radius-secret";
    public static final String RADIUS_AUTH_SERVER_PORT =
            "iplanet-am-auth-radius-server-port";
    public static final String RADIUS_AUTH_LEVEL =
            "iplanet-am-auth-radius-auth-level";
    public static final String RADIUS_AUTH_TIMEOUT =
            "iplanet-am-auth-radius-timeout";

    /**
     * Constant strings for Unix authentication instance attributes
     */
    public static final String UNIX_AUTH_LEVEL =
            "iplanet-am-auth-unix-auth-level";
    public static final String UNIX_AUTH_PAM_SERVICE_NAME =
            "iplanet-am-auth-unix-pam-service-name";

    /**
     * Constant strings for SecurID authentication instance attributes
     */
    public static final String SECURID_AUTH_CONFIG_PATH =
            "iplanet-am-auth-securid-server-config-path";
    public static final String SECURID_AUTH_LEVEL =
            "iplanet-am-auth-securid-auth-level";
}
