/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: DMConstants.java,v 1.4 2008/08/19 19:09:07 veiming Exp $
 *
 */

package com.sun.identity.console.dm.model;

/* - NEED NOT LOG - */

public interface DMConstants {
    public static final String NAME = "tfPCName";

    public static final String ACTIVE = "Active";
    public static final String STRING_LOGICAL_AND = "AND";
    public static final String STRING_LOGICAL_OR = "OR";
    public static final String ATTR_NAME_LOGICAL_OPERATOR = "logicalOp";
    public static final String USER_SERVICE_UID = "uid";
    public static final String USER_SERVICE_ACTIVE_STATUS = "inetuserstatus";
    public static final String ENTRY_NAME_ATTRIBUTE_NAME = "entryName";
    
    /**
     * Miscellaneuous group related constants.
     */     
    public static final String GROUP_CONTAINERS = "groupContainers";
    public static final String GROUPS = "groups";
    public static final String SUB_SCHEMA_FILTERED_GROUP = "DynamicGroup";
    public static final String SUB_SCHEMA_GROUP_CONTAINER = "GroupContainer";
    public static final String SUB_SCHEMA_GROUP = "Group";
    public static final String FILTERED_GROUP_FILTERINFO = "filterinfo";
    public static final String SERVICE_HELP_DESK_ADMINS =
        "ServiceHelpDeskAdministrators";
    public static final String CONSOLE_GROUP_DEFAULT_PC_ATTR =
        "iplanet-am-admin-console-group-default-pc";
    public static final String CONSOLE_GROUP_TYPE_ATTR =
	"iplanet-am-admin-console-group-type";
    public static final String CONSOLE_GROUP_PC_LIST_ATTR =
        "iplanet-am-admin-console-group-pclist";

    /**
     * Miscellaneuous role related constants.
     */
    public static final String ROLES = "roles";
    public static final String ROLE_TYPE_ATTR = "iplanet-am-role-type";
    public static final String SUB_SCHEMA_FILTERED_ROLE = "FilteredRole";
    public static final String FILTERED_ROLE_FILTERINFO = "filterinfo";
    public static final String ROLE_ACI_DESCRIPTION_ATTR =
        "iplanet-am-role-aci-description";
    public static final String ROLE_ACI_LIST_ATTR =
        "iplanet-am-role-aci-list";
    public static final String ROLE_CONTAINER_DN =
        "iplanet-am-role-managed-container-dn";
    public static final String ROLE_DESCRIPTION_ATTR =
        "iplanet-am-role-description";
    public static final String ROLE_DEFAULT_ACI_ATTR =
        "iplanet-am-admin-console-role-default-acis";

    /**
     * Miscellaneuous people container related constants.
     */
    public static final String PEOPLE_CONTAINERS = "peopleContainers";
    public static final String SUB_SCHEMA_PEOPLE_CONTAINER = "PeopleContainer";

    /**
     * Miscellaneuous org/org unit related constants.
     */
    public static final String ORGANIZATION_STATUS = "inetdomainstatus";
    public static final String ORGANIZATIONS = "organizations";
    public static final String SUB_SCHEMA_ORGANIZATION = "Organization";
    public static final String ORGANIZATIONAL_UNITS = "organizationalUnits";


    public static final String SERVICE_ADMINS = "ServiceAdministrators";
    public static final String DOMAIN_ADMINS = "DomainAdministrators";
    public static final String DOMAIN_HELP_DESK_ADMINS =
        "DomainHelpDeskAdministrators";

    /**
     * Search constants.
     */
    public static final String CONSOLE_SEARCH_LIMIT_ATTR =
        "iplanet-am-admin-console-search-limit";
    public static final String CONSOLE_SEARCH_TIMEOUT_ATTR =
        "iplanet-am-admin-console-search-timeout";
    public static final int DEFAULT_SEARCH_LIMIT = 100;
    public static final int DEFAULT_SEARCH_TIMEOUT = 5;
    public static final int DEFAULT_PAGE_SIZE = 25;


    /** OpenSSO's entry specific service name */
    public static final String ENTRY_SPECIFIC_SERVICE =
        "iPlanetAMEntrySpecificService";

    /** Name for navigation user window used for navigation search off */
    public static final String USERS = "users";


    /**
     * Display constants for controling the views of the console
     */
    public static final String CONSOLE_PC_DISPLAY_ATTR =
        "iplanet-am-admin-console-pc-display";
    public static final String CONSOLE_GC_DISPLAY_ATTR =
        "iplanet-am-admin-console-gc-display";
    public static final String CONSOLE_OU_DISPLAY_ATTR =
        "iplanet-am-admin-console-ou-display";
    public static final String CONSOLE_UM_ENABLED_ATTR =
	"iplanet-am-admin-console-um-enabled";
    public static final String CONSOLE_USER_SERVICE_DISPLAY_ATTR = 
	"iplanet-am-admin-console-user-service-display";

    public static final String CONSOLE_ADMIN_GROUPS_DISPLAY_ATTR =
            "iplanet-am-admin-console-compliance-admin-groups";
}
