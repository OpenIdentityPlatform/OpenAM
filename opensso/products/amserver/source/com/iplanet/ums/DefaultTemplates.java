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
 * $Id: DefaultTemplates.java,v 1.2 2008/06/25 05:41:44 qcheng Exp $
 *
 */

package com.iplanet.ums;

/**
 * Default templates - until we have persistent storage in the directory
 * 
 * @see Template
 */

class DefaultTemplates {
    // Names of default templates
    public static final String BASIC_USER_TEMPLATE = "BasicUser";

    public static final String BASIC_GROUP_TEMPLATE = "BasicGroup";

    public static final String BASIC_DYNAMIC_GROUP_TEMPLATE = 
        "BasicDynamicGroup";

    public static final String BASIC_ORGANIZATION_TEMPLATE = 
        "BasicOrganization";

    public static final String BASIC_ORGUNIT_TEMPLATE = 
        "BasicOrganizationalUnit";

    public static final String BASIC_PEOPLECONTAINER_TEMPLATE = 
        "BasicPeopleContainer";

    public static final String BASIC_MANAGEDROLE_TEMPLATE = "BasicManagedRole";

    public static final String BASIC_FILTEREDROLE_TEMPLATE = 
        "BasicFilteredRole";

    public static final String BASIC_COSDEF_TEMPLATE = "BasicCOSDef";

    public static final String BASIC_DIRECTCOSDEF_TEMPLATE = 
        "BasicDirectCOSDef";

    public static final String BASIC_COSTEMPLATE_TEMPLATE = "BasicCOSTemplate";

    public static final String BASIC_MANAGED_GROUP_TEMPLATE = 
        "BasicManagedGroupTemplate";

    public static final String BASIC_USER_SEARCH_TEMPLATE = "BasicUserSearch";

    public static final String BASIC_GROUP_SEARCH_TEMPLATE = "BasicGroupSearch";

    public static final String BASIC_DYNAMIC_GROUP_SEARCH_TEMPLATE = 
        "BasicDynamicGroupSearch";

    public static final String BASIC_ORGANIZATION_SEARCH_TEMPLATE = 
        "BasicOrganizationSearch";

    public static final String BASIC_ORGUNIT_SEARCH_TEMPLATE =
        "BasicOrganizationalUnitSearch";

    public static final String BASIC_PEOPLECONTAINER_SEARCH_TEMPLATE = 
        "BasicPeopleContainerSearch";

    public static final String BASIC_MANAGEDROLE_SEARCH_TEMPLATE = 
        "BasicManagedRoleSearch";

    public static final String BASIC_FILTEREDROLE_SEARCH_TEMPLATE = 
        "BasicFilteredRoleSearch";

    public static final String BASIC_COSDEF_SEARCH_TEMPLATE = 
        "BasicCOSDefSearch";

    public static final String BASIC_DIRECTCOSDEF_SEARCH_TEMPLATE = 
        "BasicDirectCOSDefSearch";

    public static final String BASIC_COSTEMPLATE_SEARCH_TEMPLATE =
        "BasicCOSTemplateSearch";

    static final String userObjectclasses[] = { "top", "person",
            "organizationalPerson", "inetOrgPerson", "inetUser" };

    static final String userRequiredAttributes[] = { "cn", "sn", "uid" };

    // TODO: Need review for a reasonable set of attributes to represent a user
    //
    static final String userOptionalAttributes[] = { "userpassword",
            "telephonenumber", "givenname", "displayname", "title",
            "description", "mail", "postaladdress", "usercertificate" };

    static final String userSearchAttributes[] = { "objectclass", "cn", "sn",
            "uid", "telephonenumber" };

    static final String userSearchFilter = "objectclass=inetorgperson";

    static final String groupObjectclasses[] = { "top", "groupofuniquenames" };

    static final String groupRequiredAttributes[] = { "cn" };

    static final String groupOptionalAttributes[] = { "uniquemember" };

    static final String groupSearchAttributes[] = { "objectclass", "cn",
            "uniquemember" };

    static final String groupSearchFilter = "objectclass=groupofuniquenames";

    static final String dynGroupObjectclasses[] = { "top", "groupofurls" };

    static final String dynGroupRequiredAttributes[] = { "cn" };

    static final String dynGroupOptionalAttributes[] = { "memberurl" };

    static final String dynGroupSearchAttributes[] = { "objectclass", "cn",
            "memberurl" };

    static final String dynGroupSearchFilter = "objectclass=groupofurls";

    static final String mgGroupObjectClasses[] = { "top", "nsmanagedgroup" };

    static final String mgGroupRequiredAttributes[] = { "cn" };

    static final String mgGroupOptionalAttributes[] = { "memberurl",
            "nsmaxusers", "nsmaxsubgroups", "owner" };

    static final String orgObjectclasses[] = { "top", "organization" };

    static final String orgRequiredAttributes[] = { "o" };

    static final String orgOptionalAttributes[] = {};

    static final String orgSearchAttributes[] = { "objectclass", "o" };

    static final String orgSearchFilter = "objectclass=organization";

    static final String orgUnitObjectclasses[] = 
                            { "top", "organizationalUnit" };

    static final String orgUnitRequiredAttributes[] = { "ou" };

    static final String orgUnitOptionalAttributes[] = {};

    static final String orgUnitSearchAttributes[] = { "objectclass", "ou" };

    static final String orgUnitSearchFilter = "objectclass=organizationalunit";

    static final String peopleContainerObjectclasses[] = { "top",
            "nsManagedPeopleContainer", "organizationalUnit" };

    static final String peopleContainerRequiredAttributes[] = { "ou" };

    static final String peopleContainerOptionalAttributes[] = {};

    static final String peopleContainerSearchAttributes[] = { "objectclass",
            "ou" };

    static final String peopleContainerSearchFilter = 
        "objectclass=nsManagedPeopleContainer";

    static final String managedRoleObjectclasses[] = { "top", "ldapsubentry",
            "nsroledefinition", "nssimpleroledefinition",
            "nsmanagedroledefinition" };

    static final String managedRoleRequiredAttributes[] = { "cn" };

    static final String managedRoleOptionalAttributes[] = {};

    static final String managedRoleSearchAttributes[] = { "objectclass", "cn" };

    static final String managedRoleSearchFilter = 
        "objectclass=nsmanagedroledefinition";

    static final String filteredRoleObjectclasses[] = { "top", "ldapsubentry",
            "nsroledefinition", "nscomplexroledefinition",
            "nsfilteredroledefinition" };

    static final String filteredRoleRequiredAttributes[] = { "cn",
            "nsrolefilter" };

    static final String filteredRoleOptionalAttributes[] = {};

    static final String filteredRoleSearchAttributes[] = { "objectclass", "cn",
            "nsrolefilter" };

    static final String filteredRoleSearchFilter = 
        "objectclass=nsfilteredroledefinition";

    static final String cosDefObjectclasses[] = { "top", "cosdefinition" };

    static final String cosDefRequiredAttributes[] = { "cn", "cosspecifier",
            "cosattribute" };

    static final String cosDefOptionalAttributes[] = {};

    static final String cosDefSearchAttributes[] = { "objectclass", "cn",
            "costemplatedn", "cosspecifier", "cosattribute", "costargettree" };

    static final String cosDefSearchFilter = "objectclass=cosdefinition";

    static final String directCOSDefObjectclasses[] = { "top", "ldapsubentry",
            "cossuperdefinition", "cosclassicdefinition" };

    static final String directCOSDefRequiredAttributes[] = { "cn",
            "cosspecifier", "cosattribute" };

    static final String directCOSDefOptionalAttributes[] = {};

    static final String directCOSDefSearchAttributes[] = { "objectclass", "cn",
            "costemplatedn", "cosspecifier", "cosattribute" };

    static final String directCOSDefSearchFilter = 
        "objectclass=cosclassicdefinition";

    static final String COSTemplateObjectclasses[] = { "top", "costemplate" };

    static final String COSTemplateRequiredAttributes[] = { "cn" };

    static final String COSTemplateOptionalAttributes[] = {};

    static final String COSTemplateSearchAttributes[] = { "objectclass", "cn" };

    static final String COSTemplateSearchFilter = "objectclass=costemplate";

    /**
     * Defines the mapping between ldap entry objectclasses and the UMS Java
     * class. This is an array of objectclass, java class pairs. The
     * Objectclass/Javaclass pair for a superclass should be defined before that
     * of a subclass.
     */
    static final String[][] OC_JC_MAP = {
            { "organization", "com.iplanet.ums.Organization" },
            { "nsmanagedpeoplecontainer", "com.iplanet.ums.PeopleContainer" },
            { "organizationalunit", "com.iplanet.ums.OrganizationalUnit" },
            { "inetorgperson", "com.iplanet.ums.User" },
            { "groupofuniquenames", "com.iplanet.ums.StaticGroup" },
            { "groupofurls", "com.iplanet.ums.DynamicGroup" },
            { "nspolicy", "com.iplanet.ums.policy.Policy" },
            { "nsmanagedroledefinition", "com.iplanet.ums.ManagedRole" },
            { "nsfilteredroledefinition", "com.iplanet.ums.FilteredRole" },
            { "cosdefinition", "com.iplanet.ums.cos.COSDefinition" },
            { "cosclassicdefinition", 
                "com.iplanet.ums.cos.DirectCOSDefinition"},
            { "costemplate", "com.iplanet.ums.cos.COSTemplate" },
            { "inetdomain", "com.iplanet.ums.dctree.DomainComponent" },
            { "nsmanagedgroup", "com.iplanet.ums.ManagedGroup" } };

}
