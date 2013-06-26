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
 * $Id: DelegationManager.java,v 1.10 2008/06/25 05:43:24 qcheng Exp $
 *
 */

/**
 * Portions Copyrighted 2013 ForgeRock, Inc.
 */
package com.sun.identity.delegation;

import com.sun.identity.sm.SMSException;
import java.security.AccessController;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.sun.identity.shared.debug.Debug;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.common.CaseInsensitiveHashSet;
import com.sun.identity.delegation.interfaces.DelegationInterface;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdUtils;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.sm.DNMapper;
import com.sun.identity.sm.PluginSchema;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceConfigManager;
import com.sun.identity.sm.ServiceManager;
import com.sun.identity.sm.ServiceSchema;
import com.sun.identity.sm.ServiceSchemaManager;

/**
 * The <code>DelegationManager</code> class manages delegation privileges for
 * a specific realm. This class is the starting point for delegation management,
 * and provides methods to create/remove/get delegation privileges.
 * <p>
 * It is a final class and hence cannot be further extended.
 */

public final class DelegationManager {
    
    public static final String DELEGATION_SERVICE = "sunAMDelegationService";
    
    static final String DELEGATION_PLUGIN_INTERFACE = "DelegationInterface";
    
    static final String PERMISSIONS = "Permissions";
    
    static final String PRIVILEGES = "Privileges";
    
    static final String LIST_OF_PERMISSIONS = "listOfPermissions";
    
    static final String DELEGATION_DEBUG = "amDelegation";
    
    static final String SUBJECT_ID_TYPES = "SubjectIdTypes";
    
    public static final Debug debug = Debug.getInstance(DELEGATION_DEBUG);
    
    private static DelegationInterface pluginInstance = null;
    
    private static Set subjectIdTypes = new CaseInsensitiveHashSet();
    
    private String orgName;
    
    private SSOToken token;
    private static final String AUTHN_USERS_ID = "id=All Authenticated Users,ou=role," + ServiceManager.getBaseDN();
    
    /**
     * Constructor of <code>DelegationManager</code> for the specified realm.
     * It requires a <code>SSOToken</code> which will be used to perform
     * delegation operations. The user needs to have "delegation" privilege in
     * the specified realm, or <code>DelegationException</code> will be
     * thrown.
     *
     * @param token  <code.SSOToken</code> of the user delegating privileges.
     * @param orgName  The name of the realm for which the user delegates
     *         privileges.
     *
     * @throws SSOException  if invalid or expired single-sign-on token
     * @throws DelegationException for any other abnormal condition
     */
    
    public DelegationManager(SSOToken token, String orgName)
    throws SSOException, DelegationException {
        SSOTokenManager.getInstance().validateToken(token);
        this.token = token;
        this.orgName = DNMapper.orgNameToDN(orgName);
        
        if (pluginInstance == null) {
            pluginInstance = getDelegationPlugin();
        }
    }
    
    /**
     * Returns all the names of the delegation privileges that are configured
     * with the realm.
     *
     * @return <code>Set</code> of <code>DelegationPrivilege</code> names
     * configured with the realm.
     *
     * @throws DelegationException  for any abnormal condition
     */
    public Set getConfiguredPrivilegeNames() throws DelegationException {
        Set privNames = null;
        Set globalPrivNames = null;
        Set orgPrivNames = null;
        ServiceConfig privsConfig;
        ServiceConfig sc;
        
        String subConfigName = null;
        int revisionNum = DelegationUtils.getRevisionNumber();
        if (revisionNum == DelegationUtils.AM70_DELEGATION_REVISION) {
            subConfigName = DelegationManager.PERMISSIONS;
        } else {
            subConfigName = DelegationManager.PRIVILEGES;
        }
        
        try {
            ServiceConfigManager scm = new ServiceConfigManager(
                DELEGATION_SERVICE, getAdminToken());
            // get the globally defined privilege names
            sc = scm.getGlobalConfig(null);
            if (sc != null) {
                privsConfig = sc.getSubConfig(subConfigName);
                if (privsConfig != null) {
                    globalPrivNames = privsConfig.getSubConfigNames();
                }
            }
            try {
                // get the organizationally defined privilege names
                sc = scm.getOrganizationConfig(orgName, null);
                if (sc != null) {
                    privsConfig = sc.getSubConfig(subConfigName);
                    if (privsConfig != null) {
                        orgPrivNames = privsConfig.getSubConfigNames();
                    }
                }
            } catch (SMSException ex) {
                //ignore if organization configuration is not present
            }
            // merge the privilege names
            if ((globalPrivNames != null) && (!globalPrivNames.isEmpty())) {
                privNames = globalPrivNames;
                if ((orgPrivNames != null) && (!orgPrivNames.isEmpty())) {
                    privNames.addAll(orgPrivNames);
                }
            } else {
                privNames = orgPrivNames;
            }
        } catch (Exception e) {
            throw new DelegationException(e);
        }
        return privNames;
    }
    
    /**
     * Returns all the delegation privileges associated with the realm.
     *
     * @return <code>Set</code> of <code>DelegationPrivilege</code> objects
     *         associated with the realm.
     *
     * @throws DelegationException for any abnormal condition
     */
    
    public Set getPrivileges() throws DelegationException {
        if (pluginInstance != null) {
            try {
                return pluginInstance.getPrivileges(token, orgName);
            } catch (SSOException se) {
                throw new DelegationException(se);
            }
        } else {
            throw new DelegationException(ResBundleUtils.rbName,
                "no_plugin_specified", null, null);
        }
    }
    
    /**
     * Returns all the delegation privileges associated with the realm and
     * applicable to a subject.
     *
     * @param universalId  The universal ID of the subject
     *
     * @return <code>Set</code> of applicable <code>DelegationPrivilege</code>
     *         objects.
     *
     * @throws DelegationException for any abnormal condition
     */
    
    public Set getPrivileges(String universalId) throws DelegationException {
        Set privileges = getPrivileges();
        if (universalId == null) {
            return privileges;
        }
        Set applicablePrivileges = new HashSet();
        if ((privileges != null) && (!privileges.isEmpty())) {
            AMIdentity identity = null;
            try {
                identity = IdUtils.getIdentity(token, universalId);
            } catch (IdRepoException idrepo) {
                throw (new DelegationException(idrepo.getMessage()));
            }
            for (Iterator i = privileges.iterator(); i.hasNext(); ) {
                DelegationPrivilege dp = (DelegationPrivilege)i.next();
                Set subjs = dp.getSubjects();
                if ((subjs != null) && (!subjs.isEmpty())) {
                    for (Iterator j = subjs.iterator(); j.hasNext(); ) {
                        String subject = (String)j.next();
                        if (subject.equals(AUTHN_USERS_ID)) {
                            //getPrivileges returned delegation privileges for this realm, hence if the subject is all
                            //authenticated users, then the privilege is always a match.
                            applicablePrivileges.add(dp);
                        } else {
                            try {
                                AMIdentity id = IdUtils.getIdentity(
                                        token, subject);
                                if (id.equals(identity)) {
                                    applicablePrivileges.add(dp);
                                    break;
                                }
                            } catch (IdRepoException e) {
                                /*
                                 * ignore this exception because Identity may
                                 * not exist.
                                 */
                            }
                        }
                    }
                }
            }
        }
        return applicablePrivileges;
    }
    
    /**
     * Adds a delegation privilege to a specific realm. The permission will be
     * added to the existing privilege in the event that this method is trying
     * to add to an existing privilege.
     *
     * @param privilege The delegation privilege to be added.
     * @throws DelegationException if any abnormal condition occurred.
     */
    public void addPrivilege(DelegationPrivilege privilege)
        throws DelegationException {
        if (debug.messageEnabled()) {
            debug.message("privilege=" + privilege);
        }
        String name = privilege.getName();
        Set subjects = privilege.getSubjects();
        validateSupportedSubjectTypes(subjects);
        DelegationPrivilege dp = new DelegationPrivilege(
            name, subjects, orgName);
        privilege = dp;
        
        if (pluginInstance != null) {
            try {
                pluginInstance.addPrivilege(token, orgName, privilege);
            } catch (SSOException se) {
                throw new DelegationException(se);
            }
        } else {
            throw new DelegationException(ResBundleUtils.rbName,
                "no_plugin_specified", null, null);
        }
    }
    
    /**
     * Removes a delegation privilege to the realm.
     *
     * @param privilegeName The name of the <code>DelegationPrivilege</code>
     *         to be removed.
     *
     * @throws DelegationException for any abnormal condition
     */
    
    public void removePrivilege(String privilegeName)
    throws DelegationException {
        if (pluginInstance != null) {
            try {
                pluginInstance.removePrivilege(token, orgName, privilegeName);
            } catch (SSOException se) {
                throw new DelegationException(se);
            }
        } else {
            throw new DelegationException(ResBundleUtils.rbName,
                "no_plugin_specified", null, null);
        }
    }
    
    /**
     * Returns a set of selected subjects matching the pattern in the given
     * realm. The pattern accepts "*" as the wild card for searching subjects.
     * For example, "a*c" matches with any subject starting with a and ending
     * with c.
     *
     * @param pattern a filter used to select the subjects.
     *
     * @return a <code>Set</code> of subjects associated with the realm.
     *
     * @throws DelegationException  for any abnormal condition
     */
    
    public Set getSubjects(String pattern) throws DelegationException {
        if (pluginInstance != null) {
            try {
                return pluginInstance.getSubjects(token, orgName,
                    subjectIdTypes, pattern);
            } catch (SSOException se) {
                throw new DelegationException(se);
            }
        } else {
            throw new DelegationException(ResBundleUtils.rbName,
                "no_plugin_specified", null, null);
        }
    }
    
    /**
     * Returns a set of realm names, based on the input parameter
     * <code>organizationNames</code>, in which the "user" has some
     * delegation permissions.
     *
     * @param organizationNames a <code>Set</code> of realm names.
     *
     * @return a <code>Set</code> of realm names in which the user has some
     *         delegation permissions. It is a subset of
     *         <code>organizationNames</code>
     * @throws DelegationException  for any abnormal condition
     */
    
    public Set getManageableOrganizationNames(Set organizationNames)
    throws DelegationException {
        if (pluginInstance != null) {
            try {
                return pluginInstance.getManageableOrganizationNames(token,
                    organizationNames);
            } catch (SSOException se) {
                throw new DelegationException(se);
            }
        } else {
            throw new DelegationException(ResBundleUtils.rbName,
                "no_plugin_specified", null, null);
        }
    }
    
    /**
     * Gets an instance of <code>DelegationInterface</code>
     * which is the default configured delegation plugin instance.
     */
    static DelegationInterface getDelegationPlugin()
    throws DelegationException {
        if (pluginInstance != null) {
            return pluginInstance;
        }
        return loadDelegationPlugin();
    }
    
    
    /**
     * Loads the default implementation of DelegationInterface
     */
    synchronized static DelegationInterface loadDelegationPlugin()
    throws DelegationException {
        if (pluginInstance == null) {
            try {
                // get super admin user token
                SSOToken privilegedToken = getAdminToken();
                ServiceSchemaManager ssm = new ServiceSchemaManager(
                    DELEGATION_SERVICE, privilegedToken);
                
                ServiceSchema globalSchema = ssm.getGlobalSchema();
                if (globalSchema != null) {
                    Map attributeDefaults = globalSchema.getAttributeDefaults();
                    if (attributeDefaults != null) {
                        subjectIdTypes.addAll((Set)attributeDefaults.get(
                            SUBJECT_ID_TYPES));
                    }
                }
                if (debug.messageEnabled()) {
                    debug.message("Configured Subject ID Types: "
                        + subjectIdTypes);
                }
                
                Set pluginNames = ssm.getPluginSchemaNames(
                    DELEGATION_PLUGIN_INTERFACE, null);
                if (pluginNames == null) {
                    throw new DelegationException(ResBundleUtils.rbName,
                        "no_plugin_specified", null, null);
                }
                if (debug.messageEnabled()) {
                    debug.message("pluginNames=" + pluginNames);
                }
                
                // for the time being, only support one plugin
                Iterator it = pluginNames.iterator();
                if (it.hasNext()) {
                    String pluginName = (String) it.next();
                    PluginSchema ps = ssm.getPluginSchema(pluginName,
                        DELEGATION_PLUGIN_INTERFACE, null);
                    if (ps == null) {
                        throw new DelegationException(ResBundleUtils.rbName,
                            "no_plugin_specified", null, null);
                    }
                    String className = ps.getClassName();
                    if (debug.messageEnabled()) {
                        debug.message("Plugin class name:" + className);
                    }
                    pluginInstance = (DelegationInterface) Class.forName(
                        className).newInstance();
                    pluginInstance.initialize(privilegedToken, null);
                    if (debug.messageEnabled()) {
                        debug.message("Successfully created "
                            + "a delegation plugin instance");
                    }
                } else {
                    throw new DelegationException(ResBundleUtils.rbName,
                        "no_plugin_specified", null, null);
                }
            } catch (Exception e) {
                debug.error("Unable to get an instance of plugin "
                    + "for delegation", e);
                pluginInstance = null;
                throw new DelegationException(e);
            }
        }
        return pluginInstance;
    }
    
    private static void validateSupportedSubjectTypes(Set subjects)
        throws DelegationException {
        if ((subjects != null) && !subjects.isEmpty()) {
            try {
                SSOToken adminToken = getAdminToken();
                for (Iterator i = subjects.iterator(); i.hasNext(); ) {
                    String uuid = (String)i.next();
                    AMIdentity amid = IdUtils.getIdentity(adminToken, uuid);
                    if (!subjectIdTypes.contains(amid.getType().getName())) {
                        throw new DelegationException(ResBundleUtils.rbName,
                            "un_supported_subject_type", null, null);
                    }
                }
            } catch (SSOException e) {
                throw new DelegationException(e);
            } catch (IdRepoException e) {
                throw new DelegationException(e);
            }
        }
    }
    
    /**
     * Return the SSOToken of the admin configured in serverconfig.xml
     */
    static SSOToken getAdminToken() throws SSOException {
        SSOToken adminToken = (SSOToken) AccessController
            .doPrivileged(AdminTokenAction.getInstance());
        if (adminToken == null) {
            throw (new SSOException(new DelegationException(
                ResBundleUtils.rbName, "getting_admin_token_failed", null,
                null)));
        }
        return (adminToken);
    }
}
