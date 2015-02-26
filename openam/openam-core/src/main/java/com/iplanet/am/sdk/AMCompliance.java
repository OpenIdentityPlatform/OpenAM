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
 * $Id: AMCompliance.java,v 1.8 2009/01/28 05:34:47 ww203982 Exp $
 *
 */

/**
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.iplanet.am.sdk;

import com.iplanet.am.sdk.common.IComplianceServices;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceSchema;
import com.sun.identity.sm.ServiceSchemaManager;
import java.security.AccessController;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import com.sun.identity.shared.ldap.util.DN;

/**
 * This class <code>AMCompliance</code> contains the functionality to support
 * iPlanet Compliant DIT. The methods of this class will be used by other
 * classes in <code>com.iplanet.am.sdk package</code>.<p>
 * 
 * In order to determine if iPlanet Compliance mode is required or not, the
 * parameter <code>com.iplanet.am.compliance</code> will be verified. A value
 * of <code>true</code> for this parameter, means iPlanet Compliance mode.<p>
 *
 * NOTE: An explicit check must be performed using AMCompliance.
 * isIplanetCompliant() method before calling any other methods in this
 * class. 
 *
 * @deprecated  As of Sun Java System Access Manager 7.1.
 */
class AMCompliance implements AMConstants {
    // Map to keep role->group name mapping
    private static IComplianceServices complianceServices = 
        AMDirectoryAccessFactory.getComplianceServices();

    static private Map deletedOrg = new HashMap();

    protected static final String ADMIN_GROUPS_ENABLED_ATTR = 
        "iplanet-am-admin-console-compliance-admin-groups";

    protected static final String COMPLIANCE_USER_DELETION_ATTR = 
        "iplanet-am-admin-console-compliance-user-deletion";

    static private String rootSuffix;

    static protected ServiceSchema gsc = null;

    static Debug debug = AMCommonUtils.debug;

    static {
        init();
    }

    /**
     * Method to intialize all the AMCompliance class static variables
     */
    protected static void init() {
        rootSuffix = AMStoreConnection.getAMSdkBaseDN();
        if (rootSuffix == null || rootSuffix.equals("")) {
            debug.error("com.iplanet.am.rootsuffix property value "
                    + "should not be null");
            return;
        }
    }

    /**
     * Method which checks all the parent organizations of this entry
     * till the base DN, and  returns true if any one of them is
     * deleted.
     *
     * @param token SSO token of user
     * @param DN string representing dn of the object.
     * @param profileType the profile type of the object whose ancestor is
     *        is being checked.
     **/
    protected static boolean isAncestorOrgDeleted(SSOToken token, String dn,
            int profileType) throws AMException {
        return complianceServices.isAncestorOrgDeleted(token, dn, profileType);
    }

    /**
     * Method to clean up the deletedOrg cache, when an event notification
     * occurs from the directory
     * @param orgDN DN of organization that has been modified
     */
    protected static void cleanDeletedOrgCache(String orgDN) {
        String tdn = orgDN;
        while (!tdn.equalsIgnoreCase(rootSuffix)) {
            // check to see if this dn is in the deletedOrg cache.
            // delete this entry if it is
            if (deletedOrg.containsKey(tdn)) {
                synchronized (deletedOrg) {
                    deletedOrg.remove(tdn);
                }
            }
            // Get the parent DN..
            tdn = (new DN(tdn)).getParent().toRFCString().toLowerCase();
        }
    }

    /**
     * Method which checks if Admin Groups need to be created for an
     * organization.
     * @param orgDN organization dn
     * @return true if Admin Groups need to be created
     * @exception AMException if an error is encountered
     */
    protected static boolean isAdminGroupsEnabled(String orgDN)
            throws AMException {
        if (!isUnderRootSuffix(orgDN)) {
            return false;
        }

        try {
            if (AMDCTree.gsc == null) {
                ServiceSchemaManager scm = new ServiceSchemaManager(
                        ADMINISTRATION_SERVICE, (SSOToken) AccessController
                                .doPrivileged(AdminTokenAction.getInstance()));
                AMDCTree.gsc = scm.getGlobalSchema();
            }
            Map attrMap = AMDCTree.gsc.getReadOnlyAttributeDefaults();
            Set values = (Set) attrMap.get(ADMIN_GROUPS_ENABLED_ATTR);
            boolean enabled = false;
            if (values == null || values.isEmpty()) {
                enabled = false;
            } else {
                String val = (String) values.iterator().next();
                enabled = (val.equalsIgnoreCase("true"));
            }

            if (debug.messageEnabled()) {
                debug.message("Compliance.isAdminGroupsEnabled = " + enabled);
            }
            return enabled;
        } catch (SMSException ex) {
            debug.error(AMSDKBundle.getString("357"), ex);
            throw new AMException(AMSDKBundle.getString("357"), "357");
        } catch (SSOException ex) {
            debug.error(AMSDKBundle.getString("357"), ex);
            throw new AMException(AMSDKBundle.getString("357"), "357");
        }

        //return compl.isAdminGroupsEnabled(orgDN);
    }

    /**
     * Method which checks if the object is directly under root suffix
     * @param objDN object dn
     * @return true if the object is directly under root suffix
     */
    protected static boolean isUnderRootSuffix(String objDN) {
        if ((objDN == null) || (objDN.length() == 0)) {
            // Will be null only in special cases during search filter 
            // construction (AMSearchFilterMaanager.getSearchFilter())
            return true;
        }

        DN rootDN = new DN(rootSuffix);
        DN objectDN = new DN(objDN);
        if (rootDN.equals(objectDN) || rootDN.equals(objectDN.getParent())) {
            return true;
        }
        return false;
    }

    /**
     * Method which checks if Compliance User Deletion is enabled
     * @return true if Compliance User Deletion is enabled
     * @exception AMException if an error is encountered
     */
    protected static boolean isComplianceUserDeletionEnabled()
            throws AMException {
        try {
            if (AMDCTree.gsc == null) {
                ServiceSchemaManager scm = new ServiceSchemaManager(
                        ADMINISTRATION_SERVICE, (SSOToken) AccessController
                                .doPrivileged(AdminTokenAction.getInstance()));
                AMDCTree.gsc = scm.getGlobalSchema();
            }
            Map attrMap = AMDCTree.gsc.getReadOnlyAttributeDefaults();
            Set values = (Set) attrMap.get(COMPLIANCE_USER_DELETION_ATTR);
            boolean enabled = false;
            if (values == null || values.isEmpty()) {
                enabled = false;
            } else {
                String val = (String) values.iterator().next();
                enabled = (val.equalsIgnoreCase("true"));
            }

            if (debug.messageEnabled()) {
                debug.message("Compliance.isComplianceUserDeletionEnabled = "
                        + enabled);
            }
            return enabled;
        } catch (SMSException ex) {
            debug.error(AMSDKBundle.getString("359"), ex);
            throw new AMException(AMSDKBundle.getString("359"), "359");
        } catch (SSOException ex) {
            debug.error(AMSDKBundle.getString("359"), ex);
            throw new AMException(AMSDKBundle.getString("359"), "359");
        }

    }

    protected static void verifyAndDeleteObject(SSOToken token, 
            String profileDN) throws AMException {
        complianceServices.verifyAndDeleteObject(token, profileDN);
    }

    /**
     * Protected method to get the search filter to be used for
     * searching for deleted objects.
     *
     **/
    protected static String getDeletedObjectFilter(int objectType)
            throws AMException, SSOException {
        return complianceServices.getDeletedObjectFilter(objectType);
    }
}
