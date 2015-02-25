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
 * $Id: DelegationUtils.java,v 1.7 2008/07/06 05:48:31 arviranga Exp $
 *
 */

package com.sun.identity.delegation;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.sun.identity.shared.debug.Debug;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdUtils;
import com.sun.identity.sm.DNMapper;
import com.sun.identity.sm.OrganizationConfigManager;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceConfigManager;
import com.sun.identity.sm.ServiceSchemaManager;

/**
 * A utility class for delegation service providing utility methods.
 * All methods in this class are <code>static</code>, so can be invoked
 * directly.
 */
public class DelegationUtils {

    static final Debug debug = DelegationManager.debug;

    static final String REALM_NAME_TAG = "REALM";

    static final String SUBJECTS_IN_LEGACY_MODE = "defaultSubjectInLegacyMode";
    
    static final int AM70_DELEGATION_REVISION = 10;

    private static int revisionNum = 0;

    /**
     * Creates default privileges for a newly created realm This method should 
     * be called in legacy mode only.
     * @param token <code>SSOToken</code> of a privileged user who has
     *        permission to create the privileges, mostly the admin token.
     * @param realmName name of the realm under which privileges need
     *        to be created.
     */
    public static void createRealmPrivileges(SSOToken token, String realmName)
        throws SSOException, DelegationException {
        String realmId = DNMapper.orgNameToDN(realmName);
        DelegationManager dm = new DelegationManager(token, realmName);
        Set privs = dm.getConfiguredPrivilegeNames();

        if ((privs == null) || privs.isEmpty()) {
            return;
        }

        if (debug.messageEnabled()) {
            debug.message("DelegationUtils:Getting global privileges");
        }
        Iterator it = privs.iterator();
        while (it.hasNext()) {
            ServiceConfig perm = null;
            String privName = (String) it.next();
            if (getRevisionNumber() == AM70_DELEGATION_REVISION) {
                perm = DelegationUtils.getPermissionConfig(
                    null, privName, true);
            } else {
                perm = DelegationUtils.getPrivilegeConfig(
                    null, privName, true);
            }
            // get the defaultSubjectInLegacyMode in the privilege
            Map attrs = perm.getAttributes();
            if ((attrs == null) || attrs.isEmpty()) {
                throw new DelegationException(ResBundleUtils.rbName,
                        "get_privilege_attrs_failed", null, null);
            }

            Set subjects = null;
            Set configSubjects = (Set) attrs.get(SUBJECTS_IN_LEGACY_MODE);
            if ((configSubjects != null) && (!configSubjects.isEmpty())) {
                Iterator sIter = configSubjects.iterator();
                subjects = new HashSet();
                while (sIter.hasNext()) {
                    String sv = (String) sIter.next();
                    subjects.add(swapRealmTag(realmId, sv));
                }
            }
            DelegationPrivilege priv = new DelegationPrivilege(
                privName, subjects, realmName);
            dm.addPrivilege(priv);
            if (debug.messageEnabled()) {
                debug.message("added " + privName + " privilege in realm "
                        + realmName);
            }
        }
    }

    /**
     * Creates default privileges for a newly created realm This method should 
     * be called in realm mode only.
     * @param token <code>SSOToken</code> of a privileged user who has
     *        permission to create the privileges, mostly the admin token.
     * @param parent parent realm's <code>OrganizationConfigManager</code>
              where default privileges are picked from.
     * @param child realm where default privileges are copied from the
     *        </code>parent</code>.
     */
    public static void copyRealmPrivilegesFromParent(SSOToken token,
            OrganizationConfigManager parent, OrganizationConfigManager child)
            throws SSOException, DelegationException {
        if (debug.messageEnabled()) {
            debug.message("DelegationUtils.copyRealmPrivilegesFromParent"
                    + " Parent org: " + parent.getOrganizationName()
                    + " Child org: " + child.getOrganizationName());
        }
        DelegationManager pdm = new DelegationManager(token, parent
                .getOrganizationName());
        DelegationManager cdm = new DelegationManager(token, child
                .getOrganizationName());
        String childOrgName = DNMapper.orgNameToDN(child.getOrganizationName());
        Set pdps = pdm.getPrivileges();
        if (pdps == null || pdps.isEmpty()) {
            if (debug.messageEnabled()) {
                debug.message("DelegationUtils.copyRealmPrivileges"
                        + "FromParent: No privilege subjects in parent");
            }
            return;
        }
        // Set cdps = new HashSet();
        for (Iterator items = pdps.iterator(); items.hasNext();) {
            DelegationPrivilege dp = (DelegationPrivilege) items.next();
            Set subjects = dp.getSubjects();
            if (subjects == null || subjects.isEmpty()) {
                if (debug.messageEnabled()) {
                    debug.message("DelegationUtils.copyRealmPrivileges"
                            + "FromParent: No subjects in privilege: " + dp);
                }
                continue;
            }
            Set newSubjects = new HashSet();
            for (Iterator subs = subjects.iterator(); subs.hasNext();) {
                String sName = (String) subs.next();
                try {
                    AMIdentity id = IdUtils.getIdentity(token, sName);
                    // Construct a new AMIdentity object with child realm
                    AMIdentity newId = new AMIdentity(token, id.getName(), 
                        id.getType(), childOrgName, id.getDN());
                    newSubjects.add(IdUtils.getUniversalId(newId));
                } catch (IdRepoException ide) {
                    if (debug.messageEnabled()) {
                        debug.message("DelegationUtils.copyRealmPrivileges" +
                            "FromParent: IdRepoException for: " + dp, ide);
                    }
                    continue;
                }
            }
            dp.setSubjects(newSubjects);
            Set permissions = dp.getPermissions();
            if ((permissions != null) && (!permissions.isEmpty())) {
                Iterator it = permissions.iterator();
                while (it.hasNext()) {
                    DelegationPermission perm = (DelegationPermission)it.next();
                    perm.setOrganizationName("*" + childOrgName);
                }
            }
            cdm.addPrivilege(dp);
            if (debug.messageEnabled()) {
                debug.message("DelegationUtils.copyRealmPrivileges"
                        + "FromParent: Privilege copied from parent: " + dp);
            }

        }
    }

    /**
     * Deletes all the delegation privileges of a specific realm
     * @param token <code>SSOToken</code> of a privileged user who has
     *        permission to delete the privileges, mostly the admin token.
     * @param realmName name of the realm from where the delegation privileges
     *        need to be removed.
     */
    public static void deleteRealmPrivileges(SSOToken token, String realmName)
            throws SSOException, DelegationException {

        DelegationManager dm = new DelegationManager(token, realmName);
        Set privs = dm.getPrivileges();
        if ((privs == null) || privs.isEmpty()) {
            return;
        }

        Iterator it = privs.iterator();
        while (it.hasNext()) {
            DelegationPrivilege dp = (DelegationPrivilege) it.next();
            String privName = dp.getName();
            dm.removePrivilege(privName);
            if (debug.messageEnabled()) {
                debug.message("removed " + privName + " privilege from realm "
                        + realmName);
            }
        }
    }

    /**
     * Returns service config information for a delegation privilege
     * @param orgName name of the organization whose organization config
     *        is searched.
     * @param name name of the delegation privilege whose information is
              sought.
     * @param global <code>boolean</code> indicating if global config
     *        of the delegation service is expected.
     *
     * @return <code>ServiceConfig</code> information for a delegation privilege
     */
    static ServiceConfig getPrivilegeConfig(String orgName, String name,
            boolean global) throws SSOException, DelegationException {
        ServiceConfig orgConfig = null;
        ServiceConfig privsConfig = null;
        ServiceConfig priv = null;
        try {
            // get the service configuration manager of the
            // delegation service
            ServiceConfigManager scm = new ServiceConfigManager(
                    DelegationManager.DELEGATION_SERVICE, DelegationManager
                            .getAdminToken());

            // get the organization configuration of this realm
            if (global) {
                orgConfig = scm.getGlobalConfig(null);
            } else {
                orgConfig = scm.getOrganizationConfig(orgName, null);
            }
        } catch (SMSException se) {
            throw new DelegationException(ResBundleUtils.rbName,
                    "get_org_config_failed", null, se);
        }

        if (orgConfig == null) {
            throw new DelegationException(ResBundleUtils.rbName,
                    "get_privs_config_failed", null, null);
        }

        try {
            // get the sub configuration "Privileges"
            privsConfig = orgConfig.getSubConfig(DelegationManager.PRIVILEGES);
        } catch (SMSException se) {
            throw new DelegationException(ResBundleUtils.rbName,
                    "get_privs_config_failed", null, se);
        }

        try {
            // get the sub configuration for the defined privilege
            priv = privsConfig.getSubConfig(name);
        } catch (SMSException se) {
            throw new DelegationException(ResBundleUtils.rbName,
                    "get_privilege_config_failed", null, se);
        }
        return (priv);
    }

     
    /**
     * Returns service config information for a delegation permission.
     * @param  orgName name of the organization whose organization config
     *         is searched.
     * @param  name name of the delegation permission whose information is.
               sought.
     * @param  global <code>boolean</code> indicating if global config
     *         of the delegation service is expected.
     * @return <code>ServiceConfig</code> information for a delegation 
     *         permission
     */
    static ServiceConfig getPermissionConfig(String orgName, String name,
            boolean global) throws SSOException, DelegationException {
        ServiceConfig orgConfig = null;
        ServiceConfig permsConfig = null;
        ServiceConfig perm = null;
        try {
            // get the service configuration manager of the
            // delegation service
            ServiceConfigManager scm = new ServiceConfigManager(
                    DelegationManager.DELEGATION_SERVICE, DelegationManager
                            .getAdminToken());

            // get the organization configuration of this realm
            if (global) {
                orgConfig = scm.getGlobalConfig(null);
            } else {
                orgConfig = scm.getOrganizationConfig(orgName, null);
            }
        } catch (SMSException se) {
            throw new DelegationException(ResBundleUtils.rbName,
                    "get_org_config_failed", null, se);
        }

        if (orgConfig == null) {
            throw new DelegationException(ResBundleUtils.rbName,
                    "get_perms_config_failed", null, null);
        }

        try {
            // get the sub configuration "Permissions"
            permsConfig = orgConfig.getSubConfig(DelegationManager.PERMISSIONS);
        } catch (SMSException se) {
            throw new DelegationException(ResBundleUtils.rbName,
                    "get_perms_config_failed", null, se);
        }

        try {
            // get the sub configuration for the defined permission
            perm = permsConfig.getSubConfig(name);
        } catch (SMSException se) {
            throw new DelegationException(ResBundleUtils.rbName,
                    "get_permission_config_failed", null, se);
        }
        return (perm);
    }

    /**
     * replaces the realm name tag with the real realm name
     * @param realm name of the realm 
     * @param value String on which the REALM_NAME_TAG needs to be replaced
     *        with the realm name <code>realm</code>
     */
    static String swapRealmTag(String realm, String value) {
        int tagLen = REALM_NAME_TAG.length();
        int idx = value.indexOf(REALM_NAME_TAG);
        while (idx >= 0) {
            String prefix = value.substring(0, idx);
            String suffix = value.substring(idx + tagLen);
            value = prefix + realm + suffix;
            idx = value.indexOf(REALM_NAME_TAG);
        }
        return value;
    }

    /** 
     * gets the Delegation Service revision number
     * @return the revision number of the delegation service.
     * 
     */
    static int getRevisionNumber() {
        if (revisionNum == 0) {
            try {
                ServiceSchemaManager ssm = new ServiceSchemaManager(
                                 DelegationManager.DELEGATION_SERVICE,
                                 DelegationManager.getAdminToken());
                revisionNum = ssm.getRevisionNumber();
                if (debug.messageEnabled()) {
                    debug.message("DelegationUtils.getRevisionNumber(): " +
                        "Delegation Service revision number is " + 
                        revisionNum);
                }
            } catch (SMSException sme) {
                debug.error("DelegationUtils.getRevisionNumber(): " +
                    "Unable to get Delegation revision number", sme);
            } catch (SSOException ssoe) {
                debug.error("DelegationUtils.getRevisionNumber(): " +
                    "Unable to get Delegation revision number", ssoe);
            }
        }
        return revisionNum;
    }
}
