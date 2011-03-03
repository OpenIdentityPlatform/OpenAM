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
 * $Id: AMUser.java,v 1.4 2008/06/25 05:41:23 qcheng Exp $
 *
 */

package com.iplanet.am.sdk;

import java.util.Set;

import com.iplanet.sso.SSOException;

/**
 * This interface provides methods to manage user. <code>AMUser</code> objects
 * can be obtained by using <code>AMStoreConnection</code>. A handle to this
 * object can be obtained by using the DN of the object.
 * 
 * <PRE>
 * AMStoreConnection amsc = new AMStoreConnection(ssotoken); if
 * (amsc.doesEntryExist(uDN)) { AMUser user = amsc.getUser(uDN); }
 * </PRE>
 * 
 *
 * @deprecated  As of Sun Java System Access Manager 7.1.
 * @supported.all.api
 */
public interface AMUser extends AMObject {

    /**
     * Renames the user name (ie., naming attribute of user entry) in the data
     * store.
     * 
     * <p>
     * <B>Note:</B> This operation directly commits the the user name changes
     * to the data store. However, it does not save the modified/added
     * attributes. For saving them explicitly to the data store, use
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
            throws AMException, SSOException;

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
    public Set getFilteredRoleDNs() throws AMException, SSOException;

    /**
     * Gets all the static roles the user is in.
     * 
     * @return The Set of static role DN's the user is in.
     * 
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     */
    public Set getRoleDNs() throws AMException, SSOException;

    /**
     * Gets all the static and filtered roles the user is in.
     * 
     * @return The Set of static and filtered role DN's the user is in.
     * 
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     */
    public Set getAllRoleDNs() throws AMException, SSOException;

    /**
     * Assigns a role to the user.
     * 
     * @param role
     *            The Role that the user is assigned to.
     * 
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     */
    public void assignRole(AMRole role) throws AMException, SSOException;

    /**
     * Assigns a role to the user.
     * 
     * @param roleDN
     *            The role DN that the user is assigned to.
     * 
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     */
    public void assignRole(String roleDN) throws AMException, SSOException;

    /**
     * Removes a role that is assigned to the user.
     * 
     * @param role
     *            The Role that the user is assigned to.
     * 
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     */
    public void removeRole(AMRole role) throws AMException, SSOException;

    /**
     * Removes a role that is assigned to the user.
     * 
     * @param roleDN
     *            The role DN that the user is assigned to.
     * 
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     */
    public void removeRole(String roleDN) throws AMException, SSOException;

    /**
     * Gets all the static groups the user is in.
     * 
     * @return The Set of static group DN's the user is in.
     * 
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     */
    public Set getStaticGroupDNs() throws AMException, SSOException;

    /**
     * Assigns a static group to the user.
     * 
     * @param group
     *            The static group that the user is assigned to.
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store.
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     */
    public void assignStaticGroup(AMStaticGroup group) throws AMException,
            SSOException;

    /**
     * Assigns a static group to the user.
     * 
     * @param groupDN
     *            The static group DN that the user is assigned to.
     * 
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     */
    public void assignStaticGroup(String groupDN) throws AMException,
            SSOException;

    /**
     * Removes a static group that is assigned to the user.
     * 
     * @param group
     *            The static group that the user is assigned to.
     * 
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     */
    public void removeStaticGroup(AMStaticGroup group) throws AMException,
            SSOException;

    /**
     * Removes a static group that is assigned to the user.
     * 
     * @param groupDN
     *            The static group DN that the user is assigned to.
     * 
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     */
    public void removeStaticGroup(String groupDN) throws AMException,
            SSOException;

    /**
     * Gets all the assignable dynamic groups the user is in.
     * 
     * @return The Set of assignable dynamic group DN's the user is in.
     * 
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     */
    public Set getAssignableDynamicGroupDNs() throws AMException, SSOException;

    /**
     * Assigns a assignable dynamic group to the user.
     * 
     * @param assignableDynamicGroup
     *            The assignable dynamic group that the user is assigned to.
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     */
    public void assignAssignableDynamicGroup(
            AMAssignableDynamicGroup assignableDynamicGroup)
            throws AMException, SSOException;

    /**
     * Assigns a assignable dynamic group to the user.
     * 
     * @param assignableDynamicGroupDN
     *            The assignable dynamic group DN that the user is assigned to.
     * 
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     */
    public void assignAssignableDynamicGroup(String assignableDynamicGroupDN)
            throws AMException, SSOException;

    /**
     * Removes a assignable dynamic group that is assigned to the user.
     * 
     * @param assignableDynamicGroup
     *            The assignable dynamic group that the user is assigned to.
     * 
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     */
    public void removeAssignableDynamicGroup(
            AMAssignableDynamicGroup assignableDynamicGroup)
            throws AMException, SSOException;

    /**
     * Removes a assignable dynamic group that is assigned to the user.
     * 
     * @param assignableDynamicGroupDN
     *            The assignable dynamic group DN that the user is assigned to.
     * 
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     */
    public void removeAssignableDynamicGroup(String assignableDynamicGroupDN)
            throws AMException, SSOException;

    /**
     * Activates the user.
     * 
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     */
    public void activate() throws AMException, SSOException;

    /**
     * Deactivates the user.
     * 
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     */
    public void deactivate() throws AMException, SSOException;

    /**
     * Returns true if the user is activated.
     * 
     * @return true if the user is activated.
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     */
    public boolean isActivated() throws AMException, SSOException;

    /**
     * Gets all service names that are assigned to the user.
     * 
     * @return The Set of service names that are assigned to the user.
     * 
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     */
    public Set getAssignedServices() throws AMException, SSOException;

    /**
     * Assigns services to the user.
     * 
     * @param serviceNames
     *            Set of service names
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     * @see com.iplanet.am.sdk.AMObject#assignServices(java.util.Map)
     */
    public void assignServices(Set serviceNames) throws AMException,
            SSOException;

}
