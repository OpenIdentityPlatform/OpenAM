/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2013-2014 ForgeRock Inc.
 */
package org.forgerock.openam.idrepo.ldap;

/**
 * Provides constants for the LDAP IdRepo implementation.
 *
 * @author Peter Major
 */
public class LDAPConstants {

    //AD related constants
    public static final String AD_NOTIFICATION_OID = "1.2.840.113556.1.4.528";
    public static final String AD_WHEN_CREATED_ATTR = "whenCreated";
    public static final String AD_WHEN_CHANGED_ATTR = "whenChanged";
    public static final String AD_IS_DELETED_ATTR = "isDeleted";
    public static final String AD_UNICODE_PWD_ATTR = "unicodePwd";
    //attribute name constants
    public static final String DN_ATTR = "dn";
    public static final String OBJECT_CLASS_ATTR = "objectclass";
    public static final String ROLE_ATTR = "nsRole";
    public static final String ROLE_DN_ATTR = "nsRoleDN";
    public static final String ROLE_FILTER_ATTR = "nsRoleFilter";
    public static final String DEFAULT_USER_STATUS_ATTR = "inetUserStatus";
    public static final String UNIQUE_MEMBER_ATTR = "uniqueMember";
    //status constants
    public static final String STATUS_ACTIVE = "Active";
    public static final String STATUS_INACTIVE = "Inactive";
    //Data store configuration property names
    public static final String LDAP_DNCACHE_ENABLED = "sun-idrepo-ldapv3-dncache-enabled";
    public static final String LDAP_DNCACHE_SIZE = "sun-idrepo-ldapv3-dncache-size";
    public static final String LDAP_SERVER_LIST = "sun-idrepo-ldapv3-config-ldap-server";
    public static final String LDAP_SERVER_USER_NAME = "sun-idrepo-ldapv3-config-authid";
    public static final String LDAP_SERVER_PASSWORD = "sun-idrepo-ldapv3-config-authpw";
    public static final String LDAP_SERVER_HEARTBEAT_INTERVAL = "openam-idrepo-ldapv3-heartbeat-interval";
    public static final String LDAP_SERVER_HEARTBEAT_TIME_UNIT = "openam-idrepo-ldapv3-heartbeat-timeunit";
    public static final String LDAP_SERVER_ROOT_SUFFIX = "sun-idrepo-ldapv3-config-organization_name";
    public static final String LDAP_CONNECTION_POOL_MAX_SIZE = "sun-idrepo-ldapv3-config-connection_pool_max_size";
    public static final String LDAP_SSL_ENABLED = "sun-idrepo-ldapv3-config-ssl-enabled";
    public static final String LDAP_PERSISTENT_SEARCH_BASE_DN = "sun-idrepo-ldapv3-config-psearchbase";
    public static final String LDAP_PERSISTENT_SEARCH_FILTER = "sun-idrepo-ldapv3-config-psearch-filter";
    public static final String LDAP_PERSISTENT_SEARCH_SCOPE = "sun-idrepo-ldapv3-config-psearch-scope";
    public static final String LDAP_RETRY_INTERVAL = "com.iplanet.am.ldap.connection.delay.between.retries";
    public static final String LDAP_SUPPORTED_TYPES_AND_OPERATIONS = "sunIdRepoSupportedOperations";
    public static final String LDAP_USER_STATUS_ATTR_NAME = "sun-idrepo-ldapv3-config-isactive";
    public static final String LDAP_STATUS_ACTIVE = "sun-idrepo-ldapv3-config-active";
    public static final String LDAP_STATUS_INACTIVE = "sun-idrepo-ldapv3-config-inactive";
    public static final String LDAP_CREATION_ATTR_MAPPING = "sun-idrepo-ldapv3-config-createuser-attr-mapping";
    public static final String LDAP_USER_NAMING_ATTR = "sun-idrepo-ldapv3-config-auth-naming-attr";
    public static final String LDAP_USER_SEARCH_ATTR = "sun-idrepo-ldapv3-config-users-search-attribute";
    public static final String LDAP_GROUP_NAMING_ATTR = "sun-idrepo-ldapv3-config-groups-search-attribute";
    public static final String LDAP_ROLE_NAMING_ATTR = "sun-idrepo-ldapv3-config-roles-search-attribute";
    public static final String LDAP_FILTERED_ROLE_NAMING_ATTR =
            "sun-idrepo-ldapv3-config-filterroles-search-attribute";
    public static final String LDAP_USER_OBJECT_CLASS = "sun-idrepo-ldapv3-config-user-objectclass";
    public static final String LDAP_GROUP_OBJECT_CLASS = "sun-idrepo-ldapv3-config-group-objectclass";
    public static final String LDAP_ROLE_OBJECT_CLASS = "sun-idrepo-ldapv3-config-role-objectclass";
    public static final String LDAP_FILTERED_ROLE_OBJECT_CLASS = "sun-idrepo-ldapv3-config-filterrole-objectclass";
    public static final String LDAP_USER_ATTRS = "sun-idrepo-ldapv3-config-user-attributes";
    public static final String LDAP_GROUP_ATTRS = "sun-idrepo-ldapv3-config-group-attributes";
    public static final String LDAP_ROLE_ATTRS = "sun-idrepo-ldapv3-config-role-attributes";
    public static final String LDAP_FILTERED_ROLE_ATTRS = "sun-idrepo-ldapv3-config-filterrole-attributes";
    public static final String LDAP_DEFAULT_GROUP_MEMBER = "sun-idrepo-ldapv3-config-dftgroupmember";
    public static final String LDAP_UNIQUE_MEMBER = "sun-idrepo-ldapv3-config-uniquemember";
    public static final String LDAP_MEMBER_URL = "sun-idrepo-ldapv3-config-memberurl";
    public static final String LDAP_MEMBER_OF = "sun-idrepo-ldapv3-config-memberof";
    public static final String LDAP_PEOPLE_CONTAINER_NAME = "sun-idrepo-ldapv3-config-people-container-name";
    public static final String LDAP_PEOPLE_CONTAINER_VALUE = "sun-idrepo-ldapv3-config-people-container-value";
    public static final String LDAP_GROUP_CONTAINER_NAME = "sun-idrepo-ldapv3-config-group-container-name";
    public static final String LDAP_GROUP_CONTAINER_VALUE = "sun-idrepo-ldapv3-config-group-container-value";
    public static final String LDAP_ROLE_ATTR = "sun-idrepo-ldapv3-config-nsrole";
    public static final String LDAP_ROLE_DN_ATTR = "sun-idrepo-ldapv3-config-nsroledn";
    public static final String LDAP_ROLE_FILTER_ATTR = "sun-idrepo-ldapv3-config-nsrolefilter";
    public static final String LDAP_USER_SEARCH_FILTER = "sun-idrepo-ldapv3-config-users-search-filter";
    public static final String LDAP_GROUP_SEARCH_FILTER = "sun-idrepo-ldapv3-config-groups-search-filter";
    public static final String LDAP_ROLE_SEARCH_FILTER = "sun-idrepo-ldapv3-config-roles-search-filter";
    public static final String LDAP_FILTERED_ROLE_SEARCH_FILTER = "sun-idrepo-ldapv3-config-filterroles-search-filter";
    public static final String LDAP_MAX_RESULTS = "sun-idrepo-ldapv3-config-max-result";
    public static final String LDAP_TIME_LIMIT = "sun-idrepo-ldapv3-config-time-limit";
    public static final String LDAP_SERVICE_ATTRS = "sun-idrepo-ldapv3-config-service-attributes";
    public static final String LDAP_SEARCH_SCOPE = "sun-idrepo-ldapv3-config-search-scope";
    public static final String LDAP_ROLE_SEARCH_SCOPE = "sun-idrepo-ldapv3-config-role-search-scope";
    public static final String LDAP_AD_TYPE = "sun-idrepo-ldapv3-ldapv3AD";
    public static final String LDAP_ADAM_TYPE = "sun-idrepo-ldapv3-ldapv3ADAM";

    private LDAPConstants() {
    }
}
