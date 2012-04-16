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
 * $Id: AMUserImpl.java,v 1.7 2009/11/20 23:52:51 ww203982 Exp $
 *
 */

package com.iplanet.am.sdk;

import java.util.Map;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.security.AccessController;

import com.sun.identity.shared.ldap.util.DN;
import com.sun.identity.shared.ldap.util.RDN;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;

import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.SchemaType;
import com.sun.identity.sm.ServiceSchema;
import com.sun.identity.sm.ServiceSchemaManager;
import com.sun.identity.security.AdminTokenAction;

/**
 * The <code>AMUserImpl</code> implementation of interface AMUser
 *
 * @deprecated  As of Sun Java System Access Manager 7.1.
 */

class AMUserImpl extends AMObjectImpl implements AMUser {
    static String roleDNsAN = "nsroledn";

    static String statusAN = "inetUserStatus";

    static String nsroleAN = "nsrole";

    static RDN ContainerDefaultTemplateRoleRDN = new RDN(AMNamingAttrManager
            .getNamingAttr(FILTERED_ROLE)
            + "=" + CONTAINER_DEFAULT_TEMPLATE_ROLE);

    private static AMStoreConnection amsc = null;

    public AMUserImpl(SSOToken ssoToken, String DN) {
        super(ssoToken, DN, USER);
    }

    /**
     * Renames the user name (ie., naming attribute of user entry) in the
     * datastore.
     * 
     * <p>
     * <B>Note:</B> This operation directly commits the the user name changes
     * to the datastore. However, it does not save the modified/added
     * attributes. For saving them explictly to the datastore, use
     * {@link AMObject#store store()} method to save the attributes.
     * 
     * @param newName
     *            The new user name
     * @param deleteOldName
     *            if true deletes the old name, otherwise retains the old name.
     * @return the new <code>DN</code> value for the user
     * 
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     */
    public String rename(String newName, boolean deleteOldName)
            throws AMException, SSOException {
        entryDN = dsServices.renameEntry(token, profileType, entryDN, newName,
                deleteOldName);
        return entryDN;
    }

    /**
     * Gets all the filtered roles the user is in.
     * 
     * @return The Set of filtered role DN's the user is in.
     * 
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     */
    public Set getFilteredRoleDNs() throws AMException, SSOException {
        Set nsroleANSet = new HashSet(1);
        nsroleANSet.add(nsroleAN);
        Map nsrolesMap = getAttributesFromDataStore(nsroleANSet);

        Set nsroles = (Set) nsrolesMap.get(nsroleAN);
        Set nsroledns = getRoleDNs();
        Iterator iter = nsroledns.iterator();
        Set normdns = new HashSet();
        while (iter.hasNext()) {
            normdns.add((new DN((String) iter.next())).toRFCString()
                    .toLowerCase());
        }

        Set result = new HashSet();
        if (nsroles != null) {
            iter = nsroles.iterator();
        } else {
            return result;
        }

        getAMStoreConnection();
        while (iter.hasNext()) {
            String nsrole = (String) iter.next();
            DN nsroleDN = new DN(nsrole);
            if (!normdns.contains(nsroleDN.toRFCString().toLowerCase()))
            {
                RDN rdn = (RDN) nsroleDN.getRDNs().get(0);
                if (!rdn.equals(ContainerDefaultTemplateRoleRDN)
                        && isAMManagedRole(nsrole)) {
                    result.add(nsroleDN.toString());
                }
            }
        }
        return result;
    }

    /**
     * Gets all the static roles the user is in.
     * 
     * @return The Set of static role DN's the user is in.
     */
    public Set getRoleDNs() throws AMException, SSOException {
        return getAttribute(roleDNsAN);
    }

    private static void getAMStoreConnection() throws SSOException {
        if (amsc == null) {
            SSOToken internalToken = (SSOToken) AccessController
                    .doPrivileged(AdminTokenAction.getInstance());
            amsc = new AMStoreConnection(internalToken);
        }
    }

    private boolean isAMManagedRole(String nsrole) throws SSOException {
        try {
            int type = amsc.getAMObjectType(nsrole);
            if (type == AMObject.ROLE || type == AMObject.FILTERED_ROLE)
                return true;
            else
                return false;
        } catch (AMException e) {
            debug.message(nsrole + " is not an AM managed role");
            return false;
        }
    }

    /**
     * Gets all the static and filtered roles the user is in.
     * 
     * @return The Set of static and filtered role DN's the user is in.
     */
    public Set getAllRoleDNs() throws AMException, SSOException {
        Set nsroleANSet = new HashSet(1);
        nsroleANSet.add(nsroleAN);
        Map nsrolesMap = getAttributesFromDataStore(nsroleANSet);
        Set nsroles = (Set) nsrolesMap.get(nsroleAN);

        Set result = new HashSet();
        Iterator iter = nsroles.iterator();
        getAMStoreConnection();
        while (iter.hasNext()) {
            String nsrole = (String) iter.next();
            DN nsroleDN = new DN(nsrole);
            /**/
            //RDN rdn = (RDN) nsroleDN.getRDNs().firstElement();
            RDN rdn = (RDN) nsroleDN.getRDNs().get(0);
            /**/

            if (!rdn.equals(ContainerDefaultTemplateRoleRDN)
                    && isAMManagedRole(nsrole)) {
                result.add(nsroleDN.toString());
            }
        } // while

        return result;
    }

    /**
     * Assigns a role to the user.
     * 
     * @param role
     *            The Role that the user is assigned to.
     */
    public void assignRole(AMRole role) throws AMException, SSOException {
        assignRole(role.getDN());
    }

    /**
     * Assigns a role to the user.
     * 
     * @param roleDN
     *            The role DN that the user is assigned to.
     */
    public void assignRole(String roleDN) throws AMException, SSOException {
        SSOTokenManager.getInstance().validateToken(super.token);
        Set userDNs = new HashSet();
        userDNs.add(super.entryDN);

        dsServices.modifyMemberShip(super.token, userDNs, roleDN, ROLE,
                ADD_MEMBER);
    }

    /**
     * Removes a role that is assigned to the user.
     * 
     * @param role
     *            The Role that the user is assigned to.
     */
    public void removeRole(AMRole role) throws AMException, SSOException {
        removeRole(role.getDN());
    }

    /**
     * Removes a role that is assigned to the user.
     * 
     * @param roleDN
     *            The role DN that the user is assigned to.
     */
    public void removeRole(String roleDN) throws AMException, SSOException {
        SSOTokenManager.getInstance().validateToken(super.token);
        Set userDNs = new HashSet();
        userDNs.add(super.entryDN);

        dsServices.modifyMemberShip(super.token, userDNs, roleDN, ROLE,
                REMOVE_MEMBER);

    }

    /**
     * Gets all the static groups the user is in.
     * 
     * @return The Set of static group DN's the user is in.
     */
    public Set getStaticGroupDNs() throws AMException, SSOException {
        return getAttribute("iplanet-am-static-group-dn");
    }

    /**
     * Assigns a static group to the user.
     * 
     * @param group
     *            The AMStaticGroup that the user is assigned to.
     */
    public void assignStaticGroup(AMStaticGroup group) throws AMException,
            SSOException {
        assignStaticGroup(group.getDN());
    }

    /**
     * Assigns a static group to the user.
     * 
     * @param groupDN
     *            The static group DN that the user is assigned to.
     */
    public void assignStaticGroup(String groupDN) throws AMException,
            SSOException {
        SSOTokenManager.getInstance().validateToken(super.token);
        Set userDNs = new HashSet();
        userDNs.add(super.entryDN);

        dsServices.modifyMemberShip(super.token, userDNs, groupDN, GROUP,
                ADD_MEMBER);

    }

    /**
     * Removes a static group that is assigned to the user.
     * 
     * @param group
     *            The AMStaticGroup that the user is assigned to.
     */
    public void removeStaticGroup(AMStaticGroup group) throws AMException,
            SSOException {
        removeStaticGroup(group.getDN());
    }

    /**
     * Removes a static group that is assigned to the user.
     * 
     * @param groupDN
     *            The static group DN that the user is assigned to.
     */
    public void removeStaticGroup(String groupDN) throws AMException,
            SSOException {
        SSOTokenManager.getInstance().validateToken(super.token);
        Set userDNs = new HashSet();
        userDNs.add(super.entryDN);

        dsServices.modifyMemberShip(super.token, userDNs, groupDN, GROUP,
                REMOVE_MEMBER);

    }

    /**
     * Gets all the assignable dynamic groups the user is in.
     * 
     * @return The Set of assignable dynamic group DN's the user is in.
     * 
     * @throws AMException
     *             if there is an internal error in the AM Store
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     */
    public Set getAssignableDynamicGroupDNs() throws AMException, SSOException {
        return getAttribute("memberof");
    }

    /**
     * Assigns a assignable dynamic group to the user.
     * 
     * @param assignableDynamicGroup
     *            The AssignableDynamicGroup that the user is assigned to.
     * 
     * @throws AMException
     *             if there is an internal error in the AM Store
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     */
    public void assignAssignableDynamicGroup(
            AMAssignableDynamicGroup assignableDynamicGroup)
            throws AMException, SSOException {
        assignAssignableDynamicGroup(assignableDynamicGroup.getDN());
    }

    /**
     * Assigns a assignable dynamic group to the user.
     * 
     * @param assignableDynamicGroupDN
     *            The assignable dynamic group DN that the user is assigned to.
     * 
     * @throws AMException
     *             if there is an internal error in the AM Store
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     */
    public void assignAssignableDynamicGroup(String assignableDynamicGroupDN)
            throws AMException, SSOException {
        SSOTokenManager.getInstance().validateToken(super.token);
        Set userDNs = new HashSet();
        userDNs.add(super.entryDN);

        dsServices.modifyMemberShip(super.token, userDNs,
                assignableDynamicGroupDN, ASSIGNABLE_DYNAMIC_GROUP, ADD_MEMBER);
    }

    /**
     * Removes a assignable dynamic group that is assigned to the user.
     * 
     * @param assignableDynamicGroup
     *            The AssignableDynamicGroup that the user is assigned to.
     * 
     * @throws AMException
     *             if there is an internal error in the AM Store
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     */
    public void removeAssignableDynamicGroup(
            AMAssignableDynamicGroup assignableDynamicGroup)
            throws AMException, SSOException {
        removeAssignableDynamicGroup(assignableDynamicGroup.getDN());
    }

    /**
     * Removes a assignable dynamic group that is assigned to the user.
     * 
     * @param assignableDynamicGroupDN
     *            The assignable dynamic group DN that the user is assigned to.
     * 
     * @throws AMException
     *             if there is an internal error in the AM Store
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     */
    public void removeAssignableDynamicGroup(String assignableDynamicGroupDN)
            throws AMException, SSOException {
        SSOTokenManager.getInstance().validateToken(super.token);
        Set userDNs = new HashSet();
        userDNs.add(super.entryDN);

        dsServices.modifyMemberShip(super.token, userDNs,
                assignableDynamicGroupDN, ASSIGNABLE_DYNAMIC_GROUP,
                REMOVE_MEMBER);
    }

    /**
     * Activates the user.
     */
    public void activate() throws AMException, SSOException {
        setStringAttribute(statusAN, "active");
        store();
    }

    /**
     * Deactivates the user.
     */
    public void deactivate() throws AMException, SSOException {
        setStringAttribute(statusAN, "inactive");
        store();
    }

    /**
     * Returns true if the user is activated.
     * 
     * @return true if the user is activated.
     * @throws AMException
     *             if there is an internal error in the AM Store.
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     */
    public boolean isActivated() throws AMException, SSOException {
        return getStringAttribute(statusAN).equalsIgnoreCase("active");
    }

    /**
     * Assigns services to the user.
     * 
     * @param serviceNames
     *            Set of service names
     * @throws AMException
     *             if there is an internal error in the AM Store
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     * @see com.iplanet.am.sdk.AMObjectImpl#assignServices(java.util.Map)
     */
    public void assignServices(Set serviceNames) throws AMException,
            SSOException {
        if (serviceNames == null || serviceNames.isEmpty()) {
            return;
        }

        Set assignedSerivces = getAssignedServices();
        Set newOCs = new HashSet();
        Set canAssign = new HashSet();

        Iterator iter = serviceNames.iterator();
        while (iter.hasNext()) {
            String serviceName = (String) iter.next();
            if (assignedSerivces.contains(serviceName)) {
                debug.error(AMSDKBundle.getString("125"));
                throw new AMException(AMSDKBundle
                        .getString("125", super.locale), "125");
            }
            canAssign.add(serviceName);
            Set serviceOCs = AMServiceUtils.getServiceObjectClasses(token,
                    canAssign);
            newOCs.addAll(serviceOCs);
        }

        Set oldOCs = getAttribute("objectclass");
        newOCs = AMCommonUtils.combineOCs(newOCs, oldOCs);
        setAttribute("objectclass", newOCs);
        store();

        // Check if the service has the schema type (User & Dynamic)
        // specified.
        // If not throw an exception.
        // The object class is assigned above even if the schema type
        // is not specified. The reason behind this is to support the
        // "COS" type attributes.

        Iterator it = canAssign.iterator();
        while (it.hasNext()) {
            String thisService = (String) it.next();
            try {
                ServiceSchemaManager ssm = new ServiceSchemaManager(
                        thisService, token);
                ServiceSchema ss = null;
                Object args[] = { thisService };

                ss = ssm.getSchema(SchemaType.USER);
                if (ss == null) {
                    ss = ssm.getSchema(SchemaType.DYNAMIC);
                }
                if (ss == null) {
                    debug.error(AMSDKBundle.getString("1001"));
                    throw new AMException(AMSDKBundle.getString("1001", args,
                            super.locale), "1001", args);
                }
            } catch (SMSException se) {
                debug.error("AMUserImpl: schema type validation failed-> "
                        + thisService, se);
            }
        }
    }
}
