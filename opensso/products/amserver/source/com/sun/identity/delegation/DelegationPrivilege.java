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
 * $Id: DelegationPrivilege.java,v 1.7 2008/06/25 05:43:24 qcheng Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.delegation;

import com.iplanet.sso.SSOException;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.DNMapper;
import com.sun.identity.sm.ServiceConfig;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

/**
 * The <code>DelegationPrivilege</code> class represents an access control
 * policy on a set of resources in a realm. It consists of a name, a set of
 * <code>DelegationPermission</code>, and a set of subjects. The name is the
 * name of the privilege. The <code>DelegationPermission</code> defines the
 * resource to which the delegation privilege applies to. The subject set
 * defines to whom the delegation privilege applies.
 */

public class DelegationPrivilege {
    static final Debug debug = DelegationManager.debug;

    static final String RESOURCE = "resource";

    static final String ACTIONS = "actions";

    static final String DELIMITER = "/";

    private String name;

    private Set permissions = new HashSet();

    private Set subjects;

    /**
     * Constructor for <code>DelegationPrivilege</code>. Constructs a
     * delegation privilege object with a name, a set of
     * <code>DelegationPermission</code>, and a set of subjects.
     * 
     * @param name  The name of the privilege
     * @param permissions  The set of <code>DelegationPermission</code> that 
     *        the  privilege contains.
     *
     * @param subjects The set of subjects that the privilege applies to
     * 
     * @throws DelegationException if any input value is incorrect.
     */

    public DelegationPrivilege(String name, Set permissions, Set subjects)
            throws DelegationException 
    {
        setName(name);
        setPermissions(permissions);
        setSubjects(subjects);
    }

    /**
     * Constructor for <code>DelegationPrivilege</code>.
     * 
     * @param name   The name of the privilege
     * @param subjects  The set of subjects the privilege applies to
     * @param orgName The name of the realm where the privilege is defined
     * 
     * @throws DelegationException if unable to create <code>
     *         DelegationPrivilege</code> instance.
     */
    public DelegationPrivilege(String name, Set subjects, String orgName) 
        throws DelegationException 
    {
        this.name = name;
        int revisionNum = DelegationUtils.getRevisionNumber();
        try {
            // convert the orgName to DN format
            orgName = DNMapper.orgNameToDN(orgName);

            Set permNames = null;
            Map attrs = null;
            if (revisionNum != DelegationUtils.AM70_DELEGATION_REVISION) {
                // Get service config for the privileges
                ServiceConfig priv = null;
                try {
                    if (debug.messageEnabled()) {
                        debug.message("DelegationPrivilege: " + 
                            "Getting org privileges; org=" + orgName);
                    }
                    priv = DelegationUtils.getPrivilegeConfig(
                        orgName, name, false);
                } catch (DelegationException de) {
                    if (debug.messageEnabled()) {
                        debug.message("DelegationPrivilege: privilege " + 
                            name + " not defined in realm " + orgName);
                    }
                    priv = null;
                }
                if (priv == null) {
                    debug.message(
                        "DelegationPrivilege<init>: Getting global privileges");
                    try {
                        priv = DelegationUtils.getPrivilegeConfig(
                            null, name, true);
                    } catch (DelegationException de) {
                        debug.error("DelegationPrivilege<init>: privilege " +
                            name + " is not defined in any configuration.", de);
                        String[] objs = {name};
                        throw new DelegationException(ResBundleUtils.rbName,
                            "privilege_not_configured", objs, null);
                    }
                }
                if (priv == null) {
                    String[] objs = {name};
                    throw new DelegationException(ResBundleUtils.rbName,
                        "privilege_not_configured", objs, null);
                }
                     
                // get the permission names defined in the privilege 
                attrs = priv.getAttributes();
                if ((attrs == null) || attrs.isEmpty()) {
                    throw new DelegationException(ResBundleUtils.rbName, 
                        "get_privilege_attrs_failed", null, null);
                }
    
                permNames = (Set)attrs.get(
                    DelegationManager.LIST_OF_PERMISSIONS);
                if ((permNames == null) || permNames.isEmpty()) {
                    throw new DelegationException(ResBundleUtils.rbName, 
                        "no_permission_defined_in_the_privilege", null, null);
                }
            } else {
                permNames = new HashSet();
                permNames.add(name);
            }

            Iterator it = permNames.iterator();
            while (it.hasNext()) {
                String permName = (String)it.next(); 
                // Get service config for the privileges
                ServiceConfig perm = null;
                try {
                    if (debug.messageEnabled()) {
                        debug.message("DelegationPrivilege: " + 
                            "Getting org permissions; org=" + orgName);
                    }
                    perm = DelegationUtils.getPermissionConfig(orgName, 
                               permName, false);
                } catch (DelegationException de) {
                    if (debug.messageEnabled()) {
                        debug.message("DelegationPrivilege: privilege " + 
                         permName + " not defined in realm " + orgName);
                    }
                    perm = null;
                }
                if (perm == null) {
                    if (debug.messageEnabled()) {
                        debug.message("DelegationPrivilege: " + 
                            "Getting global permissions");
                    }
                    try {
                        perm = DelegationUtils.getPermissionConfig(null,
                                   permName, true);
                    } catch (DelegationException de) {
                        debug.error("DelegationPrivilege: permission " +
                             permName + 
                             " is not defined in any configuration.", 
                             de);
                        String[] objs = {permName};
                        throw new DelegationException(ResBundleUtils.rbName,
                            "permission_not_configured", objs, null);
                    }
                }
                     
                // get the resource and actions defined in the privilege
                attrs = perm.getAttributes();
                if ((attrs == null) || attrs.isEmpty()) {
                    throw new DelegationException(ResBundleUtils.rbName,
                        "get_privilege_attrs_failed", null, null);
                }
    
                Set resources = (Set)attrs.get(RESOURCE);
                Set actions = (Set)attrs.get(ACTIONS);
                if ((resources == null) || (actions == null) 
                    || resources.isEmpty() || actions.isEmpty()) {
                    throw new DelegationException(ResBundleUtils.rbName,
                        "get_permission_res_or_actions_failed", null, null);
                }
    
                Iterator iter = resources.iterator(); 
                String resource = (String)iter.next();
    
                // replace the realm name tag with the real realm name
                resource = DelegationUtils.swapRealmTag(orgName, resource);
                StringTokenizer st = new StringTokenizer(resource, DELIMITER);
                String realmName = st.nextToken();
                String serviceName = null;
                String version = null;
                String configType = null;
                String subconfigName = null;
                if (st.hasMoreTokens()) {
                    serviceName = st.nextToken();
                    if (st.hasMoreTokens()) {
                        version = st.nextToken();
                        if (st.hasMoreTokens()) {
                            configType = st.nextToken();
                            if (st.hasMoreTokens()) {
                                subconfigName = st.nextToken();
                                while (st.hasMoreTokens()) {
                                    subconfigName += 
                                        DELIMITER + st.nextToken();
                                }
                            }
                        }
                    }
                }

                DelegationPermission dp = new DelegationPermission(
                        realmName, serviceName, version, configType,
                        subconfigName, actions, null); 
                permissions.add(dp);
            }
            setSubjects(subjects);
            if (debug.messageEnabled()) {
                debug.message("DelegationPrivilege: org=" + orgName
                  + "; privilege name=" + name + "; permissions=" 
                  + permissions + "; subjects=" + subjects);
            }
        } catch (SSOException sse) {
            debug.error("DelegationPrivilege: ", sse);
            throw new DelegationException(sse);
        }
    }

    /**
     * Returns the privilege name in the privilege
     * 
     * @return the privilege name in the privilege
     */

    public String getName() {
        return name;
    }

    /**
     * Sets the privilege name in the privilege
     * 
     * @param name The privilege name in the delegation privilege
     * @throws DelegationException if name already exists in the realm
     */

    public void setName(String name) throws DelegationException {
        this.name = name;
    }

    /**
     * Returns the <code>DelegationPermission</code>s in the privilege
     * 
     * @return the <code>DelegationPermission</code>s in the privilege
     */

    public Set getPermissions() {
        return permissions;
    }

    /**
     * Sets the <code>DelegationPermission</code>s in the privilege
     * 
     * @param permissions The <code>DelegationPermission</code>s in the 
     *         delegation  privilege
     * @throws DelegationException if unable to set 
     *         <code>DelegationPermission</code>
     */

    public void setPermissions(Set permissions) throws DelegationException {
        this.permissions = permissions;
    }

    /**
     * Returns the subjects in the privilege
     * 
     * @return the subjects in the privilege
     */

    public Set getSubjects() {
        return subjects;
    }

    /**
     * Sets the subject names in the privilege
     * 
     * @param names  The subject names in the delegation privilege
     * @throws DelegationException  if unable to set subjects
     */

    public void setSubjects(Set names) throws DelegationException {
        subjects = new HashSet();
        if (names != null) {
            subjects.addAll(names);
        }
    }

     /**
      * Returns the <code>String</code> representation of this
      * object.
      *
      * @return the <code>String</code> representation of the
      * <code>DelegationPrivilege</code> object.
      */
    public String toString() {
        StringBuilder sb = new StringBuilder(200);
        sb.append("DelegationPrivilege Object:");
        sb.append("\nname=");
        sb.append(name);
        sb.append("\npermissions=");
        sb.append(permissions);
        sb.append("\nsubject=");
        sb.append(subjects);
        return sb.toString();
    }
}
