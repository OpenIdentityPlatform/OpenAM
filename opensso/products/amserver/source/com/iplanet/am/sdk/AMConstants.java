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
 * $Id: AMConstants.java,v 1.6 2009/01/28 05:34:47 ww203982 Exp $
 *
 */

package com.iplanet.am.sdk;

import java.util.HashSet;
import java.util.Set;

/**
 * This interface defines constants used by <code>AM SDK</code>.
 * @supported.all.api
 * 
 * <br>
 * @deprecated  As of Sun Java System Access Manager 7.1.
 */
public interface AMConstants {

    // search scope
    /**
     * Specifies search scope to be just for the object specified
     */
    public static final int SCOPE_BASE = com.sun.identity.shared.ldap.LDAPv2.SCOPE_BASE;

    /**
     * Specifies search scope to be a one level search.
     */
    public static final int SCOPE_ONE = com.sun.identity.shared.ldap.LDAPv2.SCOPE_ONE;

    /**
     * Specifies search scope to be a sub tree search.
     */
    public static final int SCOPE_SUB = com.sun.identity.shared.ldap.LDAPv2.SCOPE_SUB;

    /*
     * The above constants SCOPE_BASE, SCOPE_ONE, and SCOPE_SUB should be kept
     * in synch with the corresponding constants defined in
     * com.iplanet.ums.SearchControl
     */

    /**
     * Subscribable attribute
     */
    public static String SUBSCRIBABLE_ATTRIBUTE = 
        "iplanet-am-group-subscribable";

    /**
     * Unique member attribute
     */
    public static final String UNIQUE_MEMBER_ATTRIBUTE = "uniquemember";

    // All Protected Constants used with in SDK package
    // Sting Constants NOT Public
    // Attributes
    static final String STATIC_GROUP_DN_ATTRIBUTE = 
        "iplanet-am-static-group-dn";

    static final String CONTAINER_SUPPORTED_TYPES_ATTRIBUTE = 
        "sunIdentityServerSupportedTypes";

    static final String INET_DOMAIN_STATUS_ATTRIBUTE = "inetdomainstatus";

    static final String SERVICE_STATUS_ATTRIBUTE = "sunRegisteredServiceName";

    static final String ROLE_MANAGED_CONTAINER_DN_ATTRIBUTE = 
        "iplanet-am-role-managed-container-dn";

    static final String UNIQUE_ATTRIBUTE_LIST_ATTRIBUTE = 
        "sunnamespaceuniqueattrs";

    static final String USER_PASSWORD_ATTRIBUTE = "userpassword";

    static final String USER_ENCRYPTED_PASSWORD_ATTRIBUTE = 
        "encrypteduserpassword";

    static final String REQUIRED_SERVICES_ATTR = "iplanet-am-required-services";

    static final String USER_SEARCH_RETURN_ATTR = 
        "iplanet-am-admin-console-user-return-attribute";

    static final String DCT_ENABLED_ATTR = "iplanet-am-admin-console-dctree";

    static final String DCT_ATTRIBUTE_LIST_ATTR = 
        "iplanet-am-admin-console-dctree-attr-list";

    static final String ADMIN_GROUPS_ENABLED_ATTR = 
        "iplanet-am-admin-console-compliance-admin-groups";

    static final String COMPLIANCE_USER_DELETION_ATTR = 
        "iplanet-am-admin-console-compliance-user-deletion";

    static final String COMPLIANCE_SPECIAL_FILTER_ATTR = 
        "iplanet-am-admin-console-special-search-filters";

    static final String ADMIN_ROLE_ATTR = "adminrole";

    static final String EMAIL_ATTRIBUTE = "mail";

    static final String INET_DOMAIN_STATUS_ATTR = "inetdomainstatus";

    static final String DOMAIN_ADMINISTRATORS = "DomainAdministrators";

    static final String DOMAIN_HELP_DESK_ADMINISTRATORS = 
        "DomainHelpDeskAdministrators";

    static final String INET_ADMIN_OBJECT_CLASS = "inetadmin";

    // Pre Post Processing Impl attribute
    static final String PRE_POST_PROCESSING_MODULES_ATTR = 
        "iplanet-am-admin-console-pre-post-processing-modules";

    // notification attribute names
    static final String USER_CREATE_NOTIFICATION_LIST = 
        "iplanet-am-user-create-notification-list";

    static final String USER_DELETE_NOTIFICATION_LIST = 
        "iplanet-am-user-delete-notification-list";

    static final String USER_MODIFY_NOTIFICATION_LIST = 
        "iplanet-am-user-modify-notification-list";

    static final String FILTER_ATTR_NAME = "nsRoleFilter";

    static final String USERID_PASSWORD_VALIDATION_CLASS = 
        "iplanet-am-admin-console-user-password-validation-class";

    static final String INVALID_USERID_CHARACTERS = 
        "iplanet-am-admin-console-invalid-chars";

    // Other Constants
    static final String CONTAINER_DEFAULT_TEMPLATE_ROLE = 
        "ContainerDefaultTemplateRole";

    // Service Names
    static final String ADMINISTRATION_SERVICE = 
        "iPlanetAMAdminConsoleService";

    // Properties
    static final String CACHE_ENABLED_DISABLED_KEY = 
        "com.iplanet.am.sdk.caching.enabled";

    static final String CACHE_MAX_SIZE_KEY = "com.iplanet.am.sdk.cache.maxSize";

    // Plugin interface for processing user create/delete/modify
    static final String USER_ENTRY_PROCESSING_IMPL = 
        "com.iplanet.am.sdk.userEntryProcessingImpl";

    // COS Attribute type could default, operational, override,
    // For policy attributes, will use "override" since user won't be
    // able to customize them
    // suffix for policy COSDefinition, i.e. &lt;serviceName>Policy
    static final String POLICY_SUFFIX = "Policy";

    static final String POLICY_COSATTR_TYPE = " override";

    // For other attributes, user will be able to customize them
    // so use default
    static final String OTHER_COSATTR_TYPE = " default";

    // Other constant values
    static final int ADD_MEMBER = 1;

    static final int REMOVE_MEMBER = 2;

    // Constant for removing attribute
    public static final Set REMOVE_ATTRIBUTE = new HashSet();
}
