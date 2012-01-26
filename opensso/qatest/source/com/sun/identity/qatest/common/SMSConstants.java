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
 * $Id: SMSConstants.java,v 1.7 2009/05/27 23:06:35 rmisra Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.qatest.common;

/**
 * <code>SMSConstants</code> is an interface which contains datastore 
 * attributes, keys, and values for LDAPv3 and flatfile datastores.
 */
public interface SMSConstants {
    
    /**
     * Prefix for the parameter in datastore property file in 
     * this format: <datastore prefix>.<attribute>
     */
    static final String UM_DATASTORE_PARAMS_PREFIX = 
            "UMGlobalDatastoreConfig";
    
    /**
     * Tags for qatest execution mode:
     * QATEST_EXEC_MODE_SINGLE - single server tests are being executed
     * QATEST_EXEC_MODE_DUAL - multi (two) server tests are being executed
     * QATEST_EXEC_MODE_ALL - multi (four) server tests are being executed
     */
    static final String QATEST_EXEC_MODE_SINGLE = "single";
    static final String QATEST_EXEC_MODE_DUAL = "dual";
    static final String QATEST_EXEC_MODE_ALL = "all";

    /**
     * Prefix for datastore key.  Any key is not datastore attributes should
     * have this prefix i.e. datastore-count, datastore-type, etc.
     */
    static final String UM_DATASTORE_KEY_PREFIX = "datastore";

    /**
     * Datastore count
     */
    static final String UM_DATASTORE_COUNT = "datastore-count";
    
    /**
     * Datastore realm
     */
    static final String UM_DATASTORE_REALM = "datastore-realm";
    
    /**
     * Datastore name
     */
    static final String UM_DATASTORE_NAME = "datastore-name";
    
    /**
     * Datastore admin id
     */
    static final String UM_DATASTORE_ADMINID = "datastore-adminid";
    
    /**
     * Datastore admin pw
     */
    static final String UM_DATASTORE_ADMINPW = "datastore-adminpw";
    
    /**
     * Datastore type
     */
    static final String UM_DATASTORE_TYPE = "datastore-type";
    
    /**
     * Datastore keystore
     */
    static final String UM_DATASTORE_KEYSTORE = "datastore-keystore";

    /**
     * Datastore root suffix
     */
    static final String UM_DATASTORE_ROOT_SUFFIX = "datastore-root-suffix";

    /**
     * Datastore schema type name for Sun DS
     */
    static final String UM_DATASTORE_SCHEMA_TYPE_AMDS = "LDAPv3ForAMDS";
    
    /**
     * Datastore schema type name for AD
     */
    static final String UM_DATASTORE_SCHEMA_TYPE_AD = "LDAPv3ForAD";
    
    /**
     * Datastore schema type name for generic LDAP
     */
    static final String UM_DATASTORE_SCHEMA_TYPE_LDAP = "LDAPv3";
    
    /**
     * Datastore schema type name for AM SDK
     */
    static final String UM_DATASTORE_SCHEMA_TYPE_AMSDK = "amSDK";
    
    /**
     * Access Manager user schema list key in property file
     */
    static final String UM_SCHEMNA_LIST = "UMGlobalConfig.schemalist";
    
    /**
     * Access Manager user schema attributes key in property file
     */
    static final String UM_SCHEMNA_ATTR = "UMGlobalConfig.schema_attributes";
    
     /**
     * Access Manager realm services key in property file
     */
    static final String REALM_SERVICE = "sunidentityrepositoryservice";
    
    /**
     * Attributes for LDAPv3 datastore
     */
    static final String UM_LDAP_SCOPE_BASE = "SCOPE_BASE";
    static final String UM_LDAP_SCOPE_ONE = "SCOPE_ONE";
    static final String UM_LDAP_SCOPE_SUB = "SCOPE_SUB";    
    static final String UM_PLUGIN_CLASS = "sunIdRepoClass";
    static final String UM_LDAPv3_PREFIX = "sun-idrepo-ldapv3-config-";
    static final String UM_LDAPv3_LDAP_SERVER =
        "sun-idrepo-ldapv3-config-ldap-server";
    static final String UM_LDAPv3_LDAP_PORT =
        "sun-idrepo-ldapv3-config-ldap-port";
    static final String UM_LDAPv3_AUTHID =
        "sun-idrepo-ldapv3-config-authid";
    static final String UM_LDAPv3_AUTHPW =
        "sun-idrepo-ldapv3-config-authpw";
    static final String UM_LDAPv3_LDAP_SSL_ENABLED =
        "sun-idrepo-ldapv3-config-ssl-enabled";
    static final String UM_LDAPv3_ORGANIZATION_NAME =
        "sun-idrepo-ldapv3-config-organization_name";  
    static final String UM_LDAPv3_LDAP_CONNECTION_POOL_MIN_SIZE =
        "sun-idrepo-ldapv3-config-connection_pool_min_size";
    static final String UM_LDAPv3_LDAP_CONNECTION_POOL_MAX_SIZE =
        "sun-idrepo-ldapv3-config-connection_pool_max_size";
    static final String UM_LDAPv3_ATTR_MAPPING =  "sunIdRepoAttributeMapping";
    static final String UM_LDAPv3_SUPPORT_OPERATION =  
            "sunIdRepoSupportedOperations";
    static final String UM_LDAPv3_LDAP_GROUP_SEARCH_FILTER =
        "sun-idrepo-ldapv3-config-groups-search-filter";
    static final String UM_LDAPv3_LDAP_USERS_SEARCH_FILTER =
        "sun-idrepo-ldapv3-config-users-search-filter";
    static final String UM_LDAPv3_LDAP_ROLES_SEARCH_FILTER =
        "sun-idrepo-ldapv3-config-roles-search-filter";
    static final String UM_LDAPv3_LDAP_FILTERROLES_SEARCH_FILTER =
        "sun-idrepo-ldapv3-config-filterroles-search-filter";
    static final String UM_LDAPv3_LDAP_AGENT_SEARCH_FILTER =
        "sun-idrepo-ldapv3-config-agent-search-filter";
    static final String UM_LDAPv3_LDAP_ROLES_SEARCH_ATTRIBUTE =
        "sun-idrepo-ldapv3-config-roles-search-attribute";
    static final String UM_LDAPv3_LDAP_FILTERROLES_SEARCH_ATTRIBUTE =
        "sun-idrepo-ldapv3-config-filterroles-search-attribute";
    static final String UM_LDAPv3_LDAP_GROUPS_SEARCH_ATTRIBUTE =
        "sun-idrepo-ldapv3-config-groups-search-attribute";
    static final String UM_LDAPv3_LDAP_USERS_SEARCH_ATTRIBUTE =
        "sun-idrepo-ldapv3-config-users-search-attribute";
    static final String UM_LDAPv3_LDAP_AGENT_SEARCH_ATTRIBUTE =
        "sun-idrepo-ldapv3-config-agent-search-attribute";
    static final String UM_LDAPv3_LDAP_ROLES_SEARCH_SCOPE =
        "sun-idrepo-ldapv3-config-role-search-scope";
    static final String UM_LDAPv3_LDAP_SEARCH_SCOPE =
        "sun-idrepo-ldapv3-config-search-scope";
    static final String UM_LDAPv3_LDAP_GROUP_CONTAINER_NAME =
        "sun-idrepo-ldapv3-config-group-container-name";
    static final String UM_LDAPv3_LDAP_AGENT_CONTAINER_NAME =
        "sun-idrepo-ldapv3-config-agent-container-name";
    static final String UM_LDAPv3_LDAP_PEOPLE_CONTAINER_NAME =
        "sun-idrepo-ldapv3-config-people-container-name";
    static final String UM_LDAPv3_LDAP_GROUP_CONTAINER_VALUE =
        "sun-idrepo-ldapv3-config-group-container-value";
    static final String UM_LDAPv3_LDAP_PEOPLE_CONTAINER_VALUE =
        "sun-idrepo-ldapv3-config-people-container-value";
    static final String UM_LDAPv3_LDAP_AGENT_CONTAINER_VALUE =
        "sun-idrepo-ldapv3-config-agent-container-value";
    static final String UM_LDAPv3_LDAP_TIME_LIMIT =
        "sun-idrepo-ldapv3-config-time-limit";
    static final String UM_LDAPv3_LDAP_MAX_RESULT =
        "sun-idrepo-ldapv3-config-max-result";
    static final String UM_LDAPv3_REFERRALS =
        "sun-idrepo-ldapv3-config-referrals";
    static final String UM_LDAPv3_ROLE_OBJECT_CLASS =
        "sun-idrepo-ldapv3-config-role-objectclass";
    static final String UM_LDAPv3_FILTERROLE_OBJECT_CLASS =
        "sun-idrepo-ldapv3-config-filterrole-objectclass";
    static final String UM_LDAPv3_GROUP_OBJECT_CLASS =
        "sun-idrepo-ldapv3-config-group-objectclass";
    static final String UM_LDAPv3_USER_OBJECT_CLASS =
        "sun-idrepo-ldapv3-config-user-objectclass";
    static final String UM_LDAPv3_AGENT_OBJECT_CLASS =
        "sun-idrepo-ldapv3-config-agent-objectclass";
    static final String UM_LDAPv3_GROUP_ATTR =
        "sun-idrepo-ldapv3-config-group-attributes";
    static final String UM_LDAPv3_USER_ATTR =
        "sun-idrepo-ldapv3-config-user-attributes";
    static final String UM_LDAPv3_AGENT_ATTR =
        "sun-idrepo-ldapv3-config-agent-attributes";
    static final String UM_LDAPv3_ROLE_ATTR =
        "sun-idrepo-ldapv3-config-role-attributes";
    static final String UM_LDAPv3_FILTERROLE_ATTR =
        "sun-idrepo-ldapv3-config-filterrole-attributes";
    static final String UM_LDAPv3_NSROLE =
        "sun-idrepo-ldapv3-config-nsrole";
    static final String UM_LDAPv3_NSROLEDN =
        "sun-idrepo-ldapv3-config-nsroledn";
    static final String UM_LDAPv3_NSROLEFILTER =
        "sun-idrepo-ldapv3-config-nsrolefilter";
    static final String UM_LDAPv3_MEMBEROF =
        "sun-idrepo-ldapv3-config-memberof";
    static final String UM_LDAPv3_UNIQUEMEMBER =
        "sun-idrepo-ldapv3-config-uniquemember";
    static final String UM_LDAPv3_MEMBERURL =
        "sun-idrepo-ldapv3-config-memberurl";
    static final String UM_LDAPv3_LDAP_IDLETIMEOUT =
        "sun-idrepo-ldapv3-config-idletimeout";
    static final String UM_LDAPv3_LDAP_PSEARCHBASE =
            "sun-idrepo-ldapv3-config-psearchbase";
    static final String UM_LDAPv3_LDAP_PSEARCHFILTER =
            "sun-idrepo-ldapv3-config-psearch-filter";
    static final String UM_LDAPv3_LDAP_ISACTIVEATTRNAME =
            "sun-idrepo-ldapv3-config-isactive";
    static final String UM_LDAPv3_LDAP_INETUSERACTIVE =
            "sun-idrepo-ldapv3-config-active";
    static final String UM_LDAPv3_LDAP_INETUSERINACTIVE =
            "sun-idrepo-ldapv3-config-inactive";
    static final String UM_LDAPv3_LDAP_CREATEUSERMAPPING =
            "sun-idrepo-ldapv3-config-createuser-attr-mapping";
    static final String UM_LDAPv3_LDAP_AUTHENTICATABLE =
            "sun-idrepo-ldapv3-config-authenticatable-type";
    static final String UM_LDAPv3_LDAP_AUTHENTICATION_NAME_ATTR = 
            "sun-idrepo-ldapv3-config-auth-naming-attr";
    static final String UM_LDAPv3_LDAP_CACHEENABLED =
            "sun-idrepo-ldapv3-config-cache-enabled";
    static final String UM_LDAPv3_LDAP_CACHETTL =
            "sun-idrepo-ldapv3-config-cache-ttl";
    static final String UM_LDAPv3_LDAP_CACHESIZE =
            "sun-idrepo-ldapv3-config-cache-size";
    static final String UM_LDAPv3_LDAP_RETRIES = 
            "sun-idrepo-ldapv3-config-numretires";
    static final String UM_LDAPv3_LDAP_DEPLAY = 
            "com.iplanet.am.ldap.connection.delay.between.retries";
    static final String UM_LDAPv3_LDAP_ERRORCODE = 
            "sun-idrepo-ldapv3-config-errorcodes";
    
    /** 
     * Attributes for am sdk idRepo
     */
    static final String UM_AMSDK_CLASS = "sunIdRepoClass";
    static final String UM_AMSDK_ORG_NAME  ="amSDKOrgName";
    static final String UM_AMSDK_PEOPLE_CONTAINER_NAME = 
            "sun-idrepo-amSDK-config-people-container-name";
    static final String UM_AMSDK_PEOPLE_CONTAINER_VALUE = 
            "sun-idrepo-amSDK-config-people-container-value";
    static final String UM_AMSDK_AGENT_CONTAINER_NAME = 
            "sun-idrepo-amSDK-config-agent-container-name";
    static final String UM_AMSDK_AGENT_CONTAINER_VALUE = 
            "sun-idrepo-amSDK-config-agent-container-value";
    static final String UM_AMSDK_RECURSIVE_ENABLED = 
            "sun-idrepo-amSDK-config-recursive-enabled";
    static final String UM_AMSDK_COPYCONFIG_ENABLED = 
            "sun-idrepo-amSDK-config-copyconfig-enabled";
}