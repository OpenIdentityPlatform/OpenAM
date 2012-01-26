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
 * $Id: SearchFilterManager.java,v 1.6 2009/01/28 05:34:48 ww203982 Exp $
 *
 */

/**
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.iplanet.am.sdk.ldap;

import com.iplanet.am.sdk.AMConstants;
import com.iplanet.am.sdk.AMException;
import com.iplanet.am.sdk.AMObject;
import com.iplanet.am.util.Cache;
import com.iplanet.ums.Guid;
import com.iplanet.ums.SearchTemplate;
import com.iplanet.ums.TemplateManager;
import com.iplanet.ums.UMSException;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.SMSEntry;
import java.util.HashMap;
import java.util.Map;
import com.sun.identity.shared.ldap.util.DN;

/**
 * A Class which manages the search filters correponding to each of the AMObject
 * types. It obtains the search filters from the Search Templates created under
 * a Organization where the AMObject resides. If a search template is not
 * defined under an Orgnization, it obtains the default values defined at the
 * Global level
 * 
 * After obtaining the search filter for a particular AMObject, it is cached.
 * 
 * <b>NOTE:</b> This Cache does not receive notifications right now. Hence,
 * once the information is cached, the values will not change until a server
 * restart. Also, any new Search Templates added will not take affect, if the
 * Cache already has store global default values corresponding to that AMObject
 * type.
 * 
 */
public class SearchFilterManager {

    private static final String ROOT_SUFFIX_DN =
        (new DN(SMSEntry.getAMSdkBaseDN()).toRFCString().toLowerCase());

    private static Debug debug = CommonUtils.getDebugInstance();

    // Search Template Names
    private static final String USER_SEARCH_TEMPLATE = 
        "BasicUserSearch";

    private static final String ROLE_SEARCH_TEMPLATE = 
        "BasicManagedRoleSearch";

    
    private static final String FILTERED_ROLE_SEARCH_TEMPLATE = 
        "BasicFilteredRoleSearch";

    private static final String GROUP_SEARCH_TEMPLATE = 
        "BasicGroupSearch";

    private static final String DYNAMIC_GROUP_SEARCH_TEMPLATE = 
        "BasicDynamicGroupSearch";

    private static final String ORGANIZATION_SEARCH_TEMPLATE = 
        "BasicOrganizationSearch";

    private static final String PEOPLE_CONTAINER_SEARCH_TEMPLATE = 
        "BasicPeopleContainerSearch";

    private static final String ORGANIZATIONAL_UNIT_SEARCH_TEMPLATE = 
        "BasicOrganizationalUnitSearch";

    private static final String ASSIGNABLE_DYNAMIC_GROUP_SEARCH_TEMPLATE = 
        "BasicAssignableDynamicGroupSearch";

    private static final String GROUP_CONTAINER_SEARCH_TEMPLATE = 
        "BasicGroupContainerSearch";

    private static final String RESOURCE_SEARCH_TEMPLATE = 
        "BasicResourceSearch";

    // Default Search Filters
    private static final String DEFAULT_USER_SEARCH_FILTER = 
        "(objectclass=inetorgperson)";

    private static final String DEFAULT_ROLE_SEARCH_FILTER = 
        "(objectclass=nsmanagedroledefinition)";

    private static final String DEFAULT_FILTERED_ROLE_SEARCH_FILTER = 
        "(&(objectclass=nsfilteredroledefinition)(!(cn="
            + AMConstants.CONTAINER_DEFAULT_TEMPLATE_ROLE + ")))";

    private static final String DEFAULT_GROUP_SEARCH_FILTER = 
        "(objectclass=groupofuniquenames)";

    private static final String DEFAULT_ASSIGNABLE_DYNAMIC_GROUP_SEARCH_FILTER =
        "(objectclass=iplanet-am-managed-assignable-group)";

    private static final String DEFAULT_DYNAMIC_GROUP_SEARCH_FILTER = 
        "(objectclass=groupofurls)";

    private static final String DEFAULT_ORGANIZATION_SEARCH_FILTER = 
        "(objectclass=organization)";

    private static final String DEFAULT_PEOPLE_CONTAINER_SEARCH_FILTER = 
        "(objectclass=nsManagedPeopleContainer)";

    private static final String DEFAULT_ORGANIZATIONAL_UNIT_SEARCH_FILTER = 
        "(objectclass=organizationalunit)";

    private static final String DEFAULT_GROUP_CONTAINER_SEARCH_FILTER = 
        "(objectclass=iplanet-am-managed-group-container)";

    public static Map searchtemplateMap = new HashMap();

    private static Cache searchfilterMap = new Cache(1000);

    static String getSearchFilterFromTemplate(int objectType, String orgDN,
            String searchTemplateName) {
        SearchTemplate searchTemp = null;
        String filter;
        try {
            String searchTempName = ((searchTemplateName == null) ? 
                    getSearchTemplateName(objectType) : searchTemplateName);

            if (searchTempName == null) {
                debug.warning("SearchFilterManager."
                        + "getSearchFilterFromTemplate(): Search template name"
                        + " is null. Unable to retrieve search filter. " 
                        + "Returning <empty> value.");
                return "";
            }

            TemplateManager mgr = TemplateManager.getTemplateManager();
            Guid orgGuid = ((orgDN == null) ? null : new Guid(orgDN));
            searchTemp = mgr.getSearchTemplate(searchTempName, orgGuid,
                    TemplateManager.SCOPE_TOP);

            // Get the Original search filter
            // Check to see if the filter starts with "(" and ends with ")"
            // In which case there is no need to add opening and closing braces.
            // otherwise add the opening and closing braces.
        } catch (UMSException ue) {
            if (debug.messageEnabled()) {
                debug.message("SearchFilterManager." + "getSearchFilterFrom" +
                    "Template() Got Exception", ue);
            }
        }
        if (searchTemp != null) {
            filter = searchTemp.getSearchFilter();
        } else {
            // FIXME: Why do we need to make it objectclass=*, can't we send the
            // default filter here?
            filter = "(objectclass=*)";
        }
        if (!filter.startsWith("(") || !filter.endsWith(")")) {
            filter = "(" + filter + ")";
        }

        // Perform any required filter modifications that need to be cached
        // filter = modifyFilter(filter, objectType);

        if (debug.messageEnabled()) {
            if (searchTemp != null) {
                debug.message("SearchFilterManager." + "getSearchFilterFrom" +
                    "Template() SearchTemplate Name = " +
                    searchTemp.getName() + ", objectType = " + objectType +
                    ", orgDN = " + orgDN + ", Obtained Filter = " +
                    searchTemp.getSearchFilter() + ", Modified Filter = " +
                    filter);
            } else {
                debug.message("SearchFilterManager." + "getSearchFilterFrom" +
                    "Template() Filter = " + filter);
            }
        }

        return filter;
    }

    /**
     * Method to perform any custom filter modifications required on the
     * original filter, depending on the object type
     * 
     * @param filter
     *            the original filter
     * @param objectType
     *            the type of AMObject
     * @return a modified filter String
     */
    private static String modifyFilter(String filter, int objectType) {
        switch (objectType) {
        case AMObject.ROLE:
        case AMObject.FILTERED_ROLE:
            // Filter out Access Manager private filtered role
            StringBuilder sb = new StringBuilder();
            sb.append("(&").append(filter);
            sb.append("(objectClass=ldapsubentry)").append("(!(cn=");
            sb.append(AMConstants.CONTAINER_DEFAULT_TEMPLATE_ROLE);
            sb.append(")))");
            return sb.toString();
        // Add any filter modifications necessary for other filters below
        }
        return filter;
    }

    /**
     * Method to get search filter for the specified object type defined at the
     * specified Organization. If a search template corresponding to the
     * AMObject, is not found at the specified Organization, then one defined at
     * global one will be returned.
     * 
     * @param objectType
     *            type of AMObject
     * @param orgDN
     *            the DN of the organization where the AMObject resides.
     * @param searchTemplateName
     *            name of the search template to be used. If this is null, then
     *            default search templates are used.
     * @param ignoreComplianceFilter
     *            if true then modify the compliance related search filters will
     *            not be applied. If false, compliance related filters will be
     *            applied.
     * @return a search filter String in lower case.
     */
    public static String getSearchFilter(int objectType, String orgDN,
            String searchTemplateName, boolean ignoreComplianceFilter) {
        String filter;
        String organizationDN = new DN(orgDN).toRFCString();

        if (orgDN != null && organizationDN.equals(ROOT_SUFFIX_DN)) {
            orgDN = null;
        }
        String cacheKey = (new Integer(objectType)).toString() + ":"
                + searchTemplateName + ":" + organizationDN;
        if ((filter = (String) searchfilterMap.get(cacheKey)) == null) {
            filter = getSearchFilterFromTemplate(objectType, orgDN,
                    searchTemplateName);
            searchfilterMap.put(cacheKey, filter);
        }

        // Now modify the obtained search filter if necessary. Also, mostly
        // do the modification here, if you do not want the modified filter
        // to be cached. Applicable to cases where filter may change dynamically
        // Note: Always add lowercase filters, to guarantee a lowercase filter
        // String to be returned.
        filter = modifyFilter(filter, objectType);
        filter = addAdminGroupFilters(filter, orgDN, objectType);
        filter = addComplianceModeFilters(filter, objectType,
                ignoreComplianceFilter);
        return filter;
    }

    /**
     * Adds filters to eliminate admin groups if the Admin Group option is not
     * enabled.
     */
    private static String addAdminGroupFilters(String originalFilter,
            String orgDN, int objectType) {
        try {
            // Filter out Admin Groups if the option is NOT enabled
            if (!ComplianceServicesImpl.isAdminGroupsEnabled(orgDN)) {
                String modifiedFilter = originalFilter;
                switch (objectType) {
                case AMObject.STATIC_GROUP:
                case AMObject.DYNAMIC_GROUP:
                case AMObject.ASSIGNABLE_DYNAMIC_GROUP:
                case AMObject.GROUP:
                    StringBuilder sb = new StringBuilder();
                    sb.append("(&").append(originalFilter).append("(!(");
                    sb.append(NamingAttributeManager.getNamingAttribute(
                            AMObject.ASSIGNABLE_DYNAMIC_GROUP));
                    sb.append("=serviceadministrators))").append("(!(");
                    sb.append(NamingAttributeManager.getNamingAttribute(
                            AMObject.ASSIGNABLE_DYNAMIC_GROUP));
                    sb.append("=servicehelpdeskadministrators)))");
                    modifiedFilter = sb.toString();
                }

                if (debug.messageEnabled()) {
                    debug.message("SearchFilterManager."
                            + "addAdminGroupFilters() - objectType = "
                            + objectType + ", orgDN = " + orgDN
                            + ", Original filter: " + originalFilter
                            + ", Modified filter = " + modifiedFilter);
                }
                return modifiedFilter;
            }
        } catch (AMException ae) {
            if (debug.warningEnabled()) {
                debug.warning("SearchFilterManager.addAdminGroupFilters() "
                        + "Unable to determine if \"Admin Groups\" option is "  
                        + "enabled or disabled. Exception : ", ae);
            }   
        }
        return originalFilter;
    }

    /**
     * Adds Compliance Mode Filters to the original filter if running in
     * Compliance mode. The addition of filters can be by-passed by setting the
     * ignoreComplianceFilter to true.
     */
    private static String addComplianceModeFilters(String originalFilter,
            int objectType, boolean ignoreComplianceFilter) {
        try {
            // Modify search filters if complaince user deletion enabled
            // Ignore if explicitly specified.
            String modifiedFilter = originalFilter;
            if (!ignoreComplianceFilter
                    && ComplianceServicesImpl.isComplianceUserDeletionEnabled())
            {
                StringBuilder sb = new StringBuilder();
                switch (objectType) {
                case AMObject.USER:
                    sb.append("(&").append(originalFilter);
                    sb.append("(!(inetuserstatus=deleted)))");
                    modifiedFilter = sb.toString();
                    break;
                case AMObject.ORGANIZATION:
                    sb.append("(&").append(originalFilter);
                    sb.append("(!(inetdomainstatus=deleted)))");
                    modifiedFilter = sb.toString();
                    break;
                case AMObject.STATIC_GROUP:
                case AMObject.DYNAMIC_GROUP:
                case AMObject.ASSIGNABLE_DYNAMIC_GROUP:
                case AMObject.GROUP:
                    sb.append("(&").append(originalFilter);
                    sb.append("(!(inetgroupstatus=deleted)))");
                    modifiedFilter = sb.toString();
                    break;
                }
                if (debug.messageEnabled()) {
                    debug.message("SearchFilterManager." + ""
                            + "addComplainceModeFilters() - objectType = "
                            + objectType + ", Original Filter = "
                            + originalFilter + ", Modified Filter = "
                            + modifiedFilter);
                }
                return modifiedFilter;
            }
        } catch (AMException ae) {
            if (debug.warningEnabled()) {
                debug.warning("SearchFilterManager."
                        + "addComplianceModeFilters() Unable to determine if "
                        + "\"User Compliance deletion mode\" is enabled or "
                        + "disabled. Exception : ", ae);
            }
        }
        return originalFilter;
    }

    /**
     * 
     * Get the name of the search template to use for specified object type.
     */
    private static String getSearchTemplateName(int objectType) {
        String st = (String) CommonUtils.searchtemplateMap.get(Integer
                .toString(objectType));
        if (st != null) {
            return st;
        }
        switch (objectType) {
        case AMObject.USER:
            return USER_SEARCH_TEMPLATE;
        case AMObject.ROLE:
            return ROLE_SEARCH_TEMPLATE;
        case AMObject.FILTERED_ROLE:
            return FILTERED_ROLE_SEARCH_TEMPLATE;
        case AMObject.GROUP:
            return GROUP_SEARCH_TEMPLATE;
        case AMObject.DYNAMIC_GROUP:
            return DYNAMIC_GROUP_SEARCH_TEMPLATE;
        case AMObject.ORGANIZATION:
            return ORGANIZATION_SEARCH_TEMPLATE;
        case AMObject.PEOPLE_CONTAINER:
            return PEOPLE_CONTAINER_SEARCH_TEMPLATE;
        case AMObject.ORGANIZATIONAL_UNIT:
            return ORGANIZATIONAL_UNIT_SEARCH_TEMPLATE;
        case AMObject.ASSIGNABLE_DYNAMIC_GROUP:
            return ASSIGNABLE_DYNAMIC_GROUP_SEARCH_TEMPLATE;
        case AMObject.GROUP_CONTAINER:
            return GROUP_CONTAINER_SEARCH_TEMPLATE;
        case AMObject.RESOURCE:
            return RESOURCE_SEARCH_TEMPLATE;
        default:
            // TODO: Should throw an exception here
            // This should'nt occur; A right thing would be to throw exception
            debug.warning("SearchFilterManager.getSearchTemplateName(): "
                    + "Unknown object type is passed. Returning null value");
            return null;
        }
    }

    private static String getDefaultSearchFilter(int objectType) {
        switch (objectType) {
        case AMObject.USER:
            return DEFAULT_USER_SEARCH_FILTER;
        case AMObject.ROLE:
            return DEFAULT_ROLE_SEARCH_FILTER;
        case AMObject.FILTERED_ROLE:
            return DEFAULT_FILTERED_ROLE_SEARCH_FILTER;
        case AMObject.GROUP:
            return DEFAULT_GROUP_SEARCH_FILTER;
        case AMObject.ASSIGNABLE_DYNAMIC_GROUP:
            return DEFAULT_ASSIGNABLE_DYNAMIC_GROUP_SEARCH_FILTER;
        case AMObject.DYNAMIC_GROUP:
            return DEFAULT_DYNAMIC_GROUP_SEARCH_FILTER;
        case AMObject.ORGANIZATION:
            return DEFAULT_ORGANIZATION_SEARCH_FILTER;
        case AMObject.PEOPLE_CONTAINER:
            return DEFAULT_PEOPLE_CONTAINER_SEARCH_FILTER;
        case AMObject.ORGANIZATIONAL_UNIT:
            return DEFAULT_ORGANIZATIONAL_UNIT_SEARCH_FILTER;
        case AMObject.GROUP_CONTAINER:
            return DEFAULT_GROUP_CONTAINER_SEARCH_FILTER;
        default:
            // TODO: Should throw an exception here
            // This should not occur; A right thing would be to throw exception
            debug.warning("SearchFilterManager.getDefaultSearchFilter(): "
                    + "Unknown object type is passed. Returning <empty> value");

            return "";
        }
    }

    /**
     * Method to get the search filter for the object type defined at the
     * specified Organization. If a search template corresponding to the
     * AMObject, is not found at the specified Organization, a global filter
     * will be returned.
     * 
     * @param objectType
     *            type of AMObject
     * @param orgDN
     *            the Organization DN String
     * @return the search filter String in lower case
     */
    public static String getSearchFilter(int objectType, String orgDN) {
        return getSearchFilter(objectType, orgDN, null, true);
    }

    /**
     * Method to get the search filter from the Global default search templates.
     * 
     * @param objectType
     *            type of AMObject
     * @return the search filter String in lower case
     */
    public static String getGlobalSearchFilter(int objectType) {
        return getGlobalSearchFilter(objectType, null);
    }

    /**
     * Method to get the search filter from a specified Global search template.
     * 
     * @param objectType
     *            type of AMObject
     * @param searchTemplateName
     *            the name of the search template to use
     * @return the search filter String in lower case
     */
    public static String getGlobalSearchFilter(int objectType,
            String searchTemplateName) {
        return getSearchFilter(objectType, null, searchTemplateName, true);
    }

}
