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
 * $Id: AMSearchFilterManager.java,v 1.7 2009/01/28 05:34:47 ww203982 Exp $
 *
 */

/**
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.iplanet.am.sdk;

import com.iplanet.am.sdk.common.IDirectoryServices;
import com.iplanet.am.util.Cache;
import com.sun.identity.shared.debug.Debug;
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
 * <b>NOTE:</b> This Cache does not recieve notifications right now. Hence,
 * once the information is cached, the values will not change until a server
 * restart. Also, any new Search Templates added will not take affect, if the
 * Cache already has store global default values corresponding to that AMObject
 * type.
 * 
 * @deprecated  As of Sun Java System Access Manager 7.1.
 */
public class AMSearchFilterManager {

    private static Debug debug = AMCommonUtils.debug;

    public static Map searchtemplateMap = new HashMap();

    private static Cache searchfilterMap = new Cache(1000);

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
        // Already in RFC
        String rootSuffixDN = AMStoreConnection.getAMSdkBaseDN(); 
        if (orgDN != null && organizationDN.equals(rootSuffixDN)) {
            orgDN = null;
        }
        String cacheKey = (new Integer(objectType)).toString() + ":"
                + searchTemplateName + ":" + organizationDN;
        if ((filter = (String) searchfilterMap.get(cacheKey)) == null) {
            IDirectoryServices dsServices = AMDirectoryAccessFactory
                    .getDirectoryServices();
            filter = dsServices.getSearchFilterFromTemplate(objectType, orgDN,
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
            if (!AMCompliance.isAdminGroupsEnabled(orgDN)) {
                String modifiedFilter = originalFilter;
                switch (objectType) {
                case AMObject.STATIC_GROUP:
                case AMObject.DYNAMIC_GROUP:
                case AMObject.ASSIGNABLE_DYNAMIC_GROUP:
                case AMObject.GROUP:
                    StringBuilder sb = new StringBuilder();
                    sb.append("(&").append(originalFilter).append("(!(");
                    sb.append(AMNamingAttrManager
                            .getNamingAttr(AMObject.ASSIGNABLE_DYNAMIC_GROUP));
                    sb.append("=serviceadministrators))").append("(!(");
                    sb.append(AMNamingAttrManager
                            .getNamingAttr(AMObject.ASSIGNABLE_DYNAMIC_GROUP));
                    sb.append("=servicehelpdeskadministrators)))");
                    modifiedFilter = sb.toString();
                }

                if (debug.messageEnabled()) {
                    debug.message("AMSearchFilterManager."
                            + "addAdminGroupFilters() - objectType = "
                            + objectType + ", orgDN = " + orgDN
                            + ", Original filter: " + originalFilter
                            + ", Modified filter = " + modifiedFilter);
                }
                return modifiedFilter;
            }
        } catch (AMException ae) {
            if (debug.warningEnabled()) {
                debug.warning("AMSearchFilterManager.addAdminGroupFilters() "
                        + "Unable to determine if \"Admin Groups\" " 
                        + "option is enabled or disabled. Exception : ", ae);
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
                    && AMCompliance.isComplianceUserDeletionEnabled()) {
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
                    debug.message("AMSearchFilterManager." + ""
                            + "addComplainceModeFilters() - objectType = "
                            + objectType + ", Original Filter = "
                            + originalFilter + ", Modified Filter = "
                            + modifiedFilter);
                }
                return modifiedFilter;
            }
        } catch (AMException ae) {
            if (debug.warningEnabled()) {
                debug.warning("AMSearchFilterManager."
                        + "addComplianceModeFilters() Unable to determine if "
                        + "\"User Compliance deletion mode\" is enabled or "
                        + "disabled. Exception : ", ae);
            }
        }
        return originalFilter;
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
