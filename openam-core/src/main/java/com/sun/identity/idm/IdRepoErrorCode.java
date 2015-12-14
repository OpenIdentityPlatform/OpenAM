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
 * Copyright 2015-2016 ForgeRock AS.
 */
package com.sun.identity.idm;

/**
 * Class is representing error code for different error states
 */
public class IdRepoErrorCode {

    // Service related error messages
    public static final String SERVICE_NOT_ASSIGNED = "101";
    public static final String UNABLE_GET_SERVICE_SCHEMA = "102";
    public static final String DATA_INVALID_FOR_SERVICE = "103";
    public static final String UNABLE_TO_ASSIGN_SERVICE = "104";
    public static final String SERVICE_ALREADY_ASSIGNED = "105";
    public static final String SERVICE_MANAGER_INITIALIZATION_FAILED = "106";

    // Identity create/read/edit related error messages
    public static final String UNABLE_READ_ATTRIBUTES = "200";
    public static final String ILLEGAL_ARGUMENTS = "201";
    public static final String NOT_VALID_ENTRY = "202";
    public static final String MEMBERSHIP_TO_USERS_AND_AGENTS_NOT_ALLOWED = "203";
    public static final String MEMBERSHIP_NOT_SUPPORTED = "204";
    public static final String UNABLE_GET_MEMBERSHIP = "205";
    public static final String MEMBERSHIPS_FOR_NOT_USERS_NOT_ALLOWED = "206";
    public static final String UNABLE_TO_MODIFY_MEMBERS = "208";
    public static final String MEMBERSHIP_CANNOT_BE_MODIFIED = "209";
    public static final String SEARCH_OPERATION_NOT_SUPPORTED = "210";
    public static final String ERROR_DURING_SEARCH = "211";
    public static final String ERROR_SETTING_ATTRIBUTES = "212";
    public static final String SERVICES_NOT_SUPPORTED_FOR_AGENTS_AND_GROUPS = "213";
    public static final String CANNOT_MODIFY_SERVICE = "214";
    public static final String ILLEGAL_UNIVERSAL_IDENTIFIER = "215";
    public static final String TOO_MANY_USERS_MATCHNING_SEARCH_CRITERIA = "216";
    public static final String NOT_SUPPORTED_TYPE = "217";
    public static final String UNABLE_ADD_LISTENER = "218";
    public static final String SEARCH_FAILED = "219";
    public static final String UNABLE_FIND_ENTRY = "220";
    public static final String UNABLE_TO_AUTHENTICATE = "221";
    public static final String MORE_THAN_ONE_MATCH_FOUND = "222";
    public static final String TYPE_NOT_FOUND = "223";
    public static final String IDENTITY_OF_TYPE_ALREADY_EXISTS = "224";
    public static final String MEMBERSHIPS_OTHER_THAN_AGENTS_NOT_ALLOWED = "225";
    public static final String UNABLE_CREATE_AGENT = "226";
    public static final String UNABLE_CREATE_USER = "227";
    public static final String CHANGE_USER_PASSWORD_NOT_SUPPORTED = "228";
    public static final String CHANGE_PASSWORD_ONLY_FOR_USER = "229";
    public static final String MINIMUM_PASSWORD_LENGTH = "230";
    public static final String PERMISSION_DENIED_SETTING_ATTRIBUTES = "231";
    public static final String UNABLE_SYNC_URL_ACCESS_AGENT = "232";
    public static final String NO_SPACE_IDENTITY_NAMES = "233";
    public static final String OLD_PASSWORD_INCORRECT = "234";

    // Plugin related error messages
    public static final String NO_PLUGINS_CONFIGURED = "301";
    public static final String OPERATION_NOT_SUPPORTED = "302";
    public static final String PLUGIN_NOT_CONFIGURED_CORRECTLY = "303";
    public static final String UNABLE_INITIALIZE_PLUGIN = "304";
    public static final String PLUGIN_OPERATION_NOT_SUPPORTED = "305";
    public static final String LDAP_EXCEPTION_OCCURRED = "306";
    public static final String INITIALIZATION_ERROR = "307";
    public static final String NOT_DIRECTORY = "308";
    public static final String UNABLE_CREATE_DIRECTORY = "309";
    public static final String NAME_ALREADY_EXISTS = "310";
    public static final String REALM_DOESNT_EXIST = "312";
    public static final String LDAP_EXCEPTION = "313";
    public static final String UNABLE_READ_PLUGIN_FOR_REALM = "314";
    public static final String UNABLE_READ_PLUGING_FOR_REALM_SSOTOKEN_NOT_VALID = "315";
    public static final String UNABLE_LOAD_SCHEMA_FOR_PLUGIN_FOR_REALM = "316";
    public static final String PLUGIN_DOESNT_EXIST_FOR_REALM = "317";

    // Misc. error message
    public static final String NO_MAPPING_FOUND = "401";
    public static final String ACCESS_DENIED = "402";
    public static final String REALM_NAME_NOT_MATCH_AUTHENTICATION_REALM = "403";
    public static final String MULTIPLE_MAPPINGS_FOUND = "404";
    public static final String UNABLE_AUTHENTICATE_LDAP_SERVER = "405";

    // Migration related messages
    public static final String MIGRATION_START = "500";
    public static final String MIGRATION_GETTING_SUBREALMS = "501";
    public static final String MIGRATION_IDNAME = "502";
    public static final String MIGRATION_AGENT_ATTRIBUTES = "503";
    public static final String MIGRATION_TO_FAM80_FAILED = "504";
    public static final String MIGRATION_COMPLETED = "505";
}
