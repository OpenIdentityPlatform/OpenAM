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
 * Copyright 2013-2016 ForgeRock AS.
 */

package org.forgerock.openam.ldap;

/**
 * Provides constants for the LDAP IdRepo implementation.
 */
public final class LDAPConstants {

    /** AD notification OID. */
    public static final String AD_NOTIFICATION_OID = "1.2.840.113556.1.4.528";

    /** AD when created attribute property key. */
    public static final String AD_WHEN_CREATED_ATTR = "whenCreated";

    /** AD when changed attribute property key. */
    public static final String AD_WHEN_CHANGED_ATTR = "whenChanged";

    /** AD is deleted attribute property key. */
    public static final String AD_IS_DELETED_ATTR = "isDeleted";

    /** AD unicode password attribute property key. */
    public static final String AD_UNICODE_PWD_ATTR = "unicodePwd";

    /** DN property key. */
    public static final String DN_ATTR = "dn";

    /** Object class attribute property key. */
    public static final String OBJECT_CLASS_ATTR = "objectclass";

    /** Role attribute property key. */
    public static final String ROLE_ATTR = "nsRole";

    /** Role DN attribute property key. */
    public static final String ROLE_DN_ATTR = "nsRoleDN";

    /** Role filter attribute property key. */
    public static final String ROLE_FILTER_ATTR = "nsRoleFilter";

    /** Default user status attribute property key. */
    public static final String DEFAULT_USER_STATUS_ATTR = "inetUserStatus";

    /** Unique member attribute property key. */
    public static final String UNIQUE_MEMBER_ATTR = "uniqueMember";

    /** Status active. */
    public static final String STATUS_ACTIVE = "Active";

    /** Status inactive. */
    public static final String STATUS_INACTIVE = "Inactive";

    /** LDAP DN cache enabled property key. */
    public static final String LDAP_DNCACHE_ENABLED = "sun-idrepo-ldapv3-dncache-enabled";

    /** LDAP DN cache size property key. */
    public static final String LDAP_DNCACHE_SIZE = "sun-idrepo-ldapv3-dncache-size";

    /** LDAP server list property key. */
    public static final String LDAP_SERVER_LIST = "sun-idrepo-ldapv3-config-ldap-server";

    /** LDAP server username property key. */
    public static final String LDAP_SERVER_USER_NAME = "sun-idrepo-ldapv3-config-authid";

    /** LDAP server password property key. */
    public static final String LDAP_SERVER_PASSWORD = "sun-idrepo-ldapv3-config-authpw";

    /** LDAP server heartbeat interval property key. */
    public static final String LDAP_SERVER_HEARTBEAT_INTERVAL = "openam-idrepo-ldapv3-heartbeat-interval";

    /** LDAP server heartbeat time unit property key. */
    public static final String LDAP_SERVER_HEARTBEAT_TIME_UNIT = "openam-idrepo-ldapv3-heartbeat-timeunit";

    /** LDAP server root suffix property key. */
    public static final String LDAP_SERVER_ROOT_SUFFIX = "sun-idrepo-ldapv3-config-organization_name";

    /** LDAP connection pool maximum size property key. */
    public static final String LDAP_CONNECTION_POOL_MAX_SIZE = "sun-idrepo-ldapv3-config-connection_pool_max_size";

    /** LDAP connection mode property key. */
    public static final String LDAP_CONNECTION_MODE = "sun-idrepo-ldapv3-config-connection-mode";

    /** LDAP connection mode LDAPS. */
    public static final String LDAP_CONNECTION_MODE_LDAPS = "LDAPS";

    /** LDAP connection mode start TLS. */
    public static final String LDAP_CONNECTION_MODE_STARTTLS = "StartTLS";

    /** LDAP persistent search base DN property key. */
    public static final String LDAP_PERSISTENT_SEARCH_BASE_DN = "sun-idrepo-ldapv3-config-psearchbase";

    /** LDAP persistent search filter property key. */
    public static final String LDAP_PERSISTENT_SEARCH_FILTER = "sun-idrepo-ldapv3-config-psearch-filter";

    /** LDAP persistent search scope property key. */
    public static final String LDAP_PERSISTENT_SEARCH_SCOPE = "sun-idrepo-ldapv3-config-psearch-scope";

    /** LDAP retry interval property key. */
    public static final String LDAP_RETRY_INTERVAL = "com.iplanet.am.ldap.connection.delay.between.retries";

    /** LDAP supported types and operations property key. */
    public static final String LDAP_SUPPORTED_TYPES_AND_OPERATIONS = "sunIdRepoSupportedOperations";

    /** LDAP user status attribute name property key. */
    public static final String LDAP_USER_STATUS_ATTR_NAME = "sun-idrepo-ldapv3-config-isactive";

    /** LDAP status active property key. */
    public static final String LDAP_STATUS_ACTIVE = "sun-idrepo-ldapv3-config-active";

    /** LDAP status inactive property key. */
    public static final String LDAP_STATUS_INACTIVE = "sun-idrepo-ldapv3-config-inactive";

    /** LDAP creation attribute mapping property key. */
    public static final String LDAP_CREATION_ATTR_MAPPING = "sun-idrepo-ldapv3-config-createuser-attr-mapping";

    /** LDAP user naming attribute property key. */
    public static final String LDAP_USER_NAMING_ATTR = "sun-idrepo-ldapv3-config-auth-naming-attr";

    /** LDAP user search attribute property key. */
    public static final String LDAP_USER_SEARCH_ATTR = "sun-idrepo-ldapv3-config-users-search-attribute";

    /** LDAP group naming attribute property key. */
    public static final String LDAP_GROUP_NAMING_ATTR = "sun-idrepo-ldapv3-config-groups-search-attribute";

    /** LDAP role naming attribute property key. */
    public static final String LDAP_ROLE_NAMING_ATTR = "sun-idrepo-ldapv3-config-roles-search-attribute";

    /** LDAP filtered role naming attribute property key. */
    public static final String LDAP_FILTERED_ROLE_NAMING_ATTR =
            "sun-idrepo-ldapv3-config-filterroles-search-attribute";

    /** LDAP user object class property key. */
    public static final String LDAP_USER_OBJECT_CLASS = "sun-idrepo-ldapv3-config-user-objectclass";

    /** LDAP group object class property key. */
    public static final String LDAP_GROUP_OBJECT_CLASS = "sun-idrepo-ldapv3-config-group-objectclass";

    /** LDAP role object class property key. */
    public static final String LDAP_ROLE_OBJECT_CLASS = "sun-idrepo-ldapv3-config-role-objectclass";

    /** LDAP filtered role object class property key. */
    public static final String LDAP_FILTERED_ROLE_OBJECT_CLASS = "sun-idrepo-ldapv3-config-filterrole-objectclass";

    /** LDAP user attributes property key. */
    public static final String LDAP_USER_ATTRS = "sun-idrepo-ldapv3-config-user-attributes";

    /** LDAP group attributes property key. */
    public static final String LDAP_GROUP_ATTRS = "sun-idrepo-ldapv3-config-group-attributes";

    /** LDAP role attributes property key. */
    public static final String LDAP_ROLE_ATTRS = "sun-idrepo-ldapv3-config-role-attributes";

    /** LDAP filtered role attributes property key. */
    public static final String LDAP_FILTERED_ROLE_ATTRS = "sun-idrepo-ldapv3-config-filterrole-attributes";

    /** LDAP default group member property key. */
    public static final String LDAP_DEFAULT_GROUP_MEMBER = "sun-idrepo-ldapv3-config-dftgroupmember";

    /** LDAP unique member property key. */
    public static final String LDAP_UNIQUE_MEMBER = "sun-idrepo-ldapv3-config-uniquemember";

    /** LDAP member url property key. */
    public static final String LDAP_MEMBER_URL = "sun-idrepo-ldapv3-config-memberurl";

    /** LDAP member of property key. */
    public static final String LDAP_MEMBER_OF = "sun-idrepo-ldapv3-config-memberof";

    /** LDAP people container name property key. */
    public static final String LDAP_PEOPLE_CONTAINER_NAME = "sun-idrepo-ldapv3-config-people-container-name";

    /** LDAP people container value property key. */
    public static final String LDAP_PEOPLE_CONTAINER_VALUE = "sun-idrepo-ldapv3-config-people-container-value";

    /** LDAP group container name property key. */
    public static final String LDAP_GROUP_CONTAINER_NAME = "sun-idrepo-ldapv3-config-group-container-name";

    /** LDAP group container value property key. */
    public static final String LDAP_GROUP_CONTAINER_VALUE = "sun-idrepo-ldapv3-config-group-container-value";

    /** LDAP role attribute property key. */
    public static final String LDAP_ROLE_ATTR = "sun-idrepo-ldapv3-config-nsrole";

    /** LDAP role DN attribute property key. */
    public static final String LDAP_ROLE_DN_ATTR = "sun-idrepo-ldapv3-config-nsroledn";

    /** LDAP role filter attribute property key. */
    public static final String LDAP_ROLE_FILTER_ATTR = "sun-idrepo-ldapv3-config-nsrolefilter";

    /** LDAP user search filter property key. */
    public static final String LDAP_USER_SEARCH_FILTER = "sun-idrepo-ldapv3-config-users-search-filter";

    /** LDAP group search filter property key. */
    public static final String LDAP_GROUP_SEARCH_FILTER = "sun-idrepo-ldapv3-config-groups-search-filter";

    /** LDAP role search filter property key. */
    public static final String LDAP_ROLE_SEARCH_FILTER = "sun-idrepo-ldapv3-config-roles-search-filter";

    /** LDAP filtered role search filter property key. */
    public static final String LDAP_FILTERED_ROLE_SEARCH_FILTER = "sun-idrepo-ldapv3-config-filterroles-search-filter";

    /** LDAP maximum results property key. */
    public static final String LDAP_MAX_RESULTS = "sun-idrepo-ldapv3-config-max-result";

    /** LDAP time limit property key. */
    public static final String LDAP_TIME_LIMIT = "sun-idrepo-ldapv3-config-time-limit";

    /** LDAP service attributes property key. */
    public static final String LDAP_SERVICE_ATTRS = "sun-idrepo-ldapv3-config-service-attributes";

    /** LDAP search scope property key. */
    public static final String LDAP_SEARCH_SCOPE = "sun-idrepo-ldapv3-config-search-scope";

    /** LDAP role search scope property key. */
    public static final String LDAP_ROLE_SEARCH_SCOPE = "sun-idrepo-ldapv3-config-role-search-scope";

    /** LDAP AD type property key. */
    public static final String LDAP_AD_TYPE = "sun-idrepo-ldapv3-ldapv3AD";

    /** LDAP Adam type property key. */
    public static final String LDAP_ADAM_TYPE = "sun-idrepo-ldapv3-ldapv3ADAM";

    /** Constraint violation. */
    public static final int LDAP_CONSTRAINT_VIOLATION = 19;

    /** Unrecognized or invalid syntax for an attribute. */
    public static final String LDAP_INVALID_SYNTAX = "21";

    /** An attribute is already set to the requested value. */
    public static final String LDAP_TYPE_OR_VALUE_EXISTS = "20";

    /** Invalid credentials used for bind. */
    public static final String LDAP_INVALID_CREDENTIALS = "49";

    private LDAPConstants() {
    }
}
