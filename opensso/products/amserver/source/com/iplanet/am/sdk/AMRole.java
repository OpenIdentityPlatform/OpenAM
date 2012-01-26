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
 * $Id: AMRole.java,v 1.4 2008/06/25 05:41:22 qcheng Exp $
 *
 */

package com.iplanet.am.sdk;

import java.util.Map;
import java.util.Set;

import com.iplanet.sso.SSOException;

/**
 * <p>
 * The <code>Role</code> interface provides methods to manage role
 * <code>AMRole</code> objects can be obtained by using
 * <code>AMStoreConnection</code>. A handle to this object can be obtained by
 * using the DN of the object.
 * 
 * <PRE>
 * 
 * AMStoreConnection amsc = new AMStoreConnection(ssotoken); if
 * (amsc.doesEntryExist(rDN)) { AMRole role = amsc.getRole(rDN); }
 * 
 * </PRE>
 *
 * @deprecated  As of Sun Java System Access Manager 7.1.
 * @supported.all.api
 */
public interface AMRole extends AMObject {

    // Admin Role Types
    /**
     * Represents a Top Level Administrative Role
     */
    public static final int TOP_LEVEL_ADMIN_ROLE = 1;

    /**
     * Represents a General Administrative Role
     */
    public static final int GENERAL_ADMIN_ROLE = 2;

    /**
     * Represents a User Role
     */
    public static final int USER_ROLE = 3;

    /**
     * Gets the type of the role.
     * 
     * @return One of the possible values:
     *         <ul>
     *         <li><code>USER_ROLE</code>
     *         <li><code>GENERAL_ADMIN_ROLE</code>
     *         <li><code>TOP_LEVEL_ADMIN_ROLE</code>
     *         </ul>
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store
     * @throws SSOException
     *             if the single sign on token is no longer valid
     */
    public int getRoleType() throws AMException, SSOException;

    /**
     * Sets the type of the role.
     * 
     * @param roleType
     *            The type of the role.
     * 
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store
     * @throws SSOException
     *             if the single sign on token is no longer valid
     */
    public void setRoleType(int roleType) throws AMException, SSOException;

    /**
     * Adds users to the role.
     * 
     * @param users
     *            The set of user DN's to be added to the role.
     * 
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store
     * @throws SSOException
     *             if the single sign on token is no longer valid
     */
    public void addUsers(Set users) throws AMException, SSOException;

    /**
     * Removes users from the role.
     * 
     * @param users
     *            The set of user DN's to be removed from the role.
     * 
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store
     * @throws SSOException
     *             if the single sign on token is no longer valid
     */
    public void removeUsers(Set users) throws AMException, SSOException;

    /**
     * Gets number of users in the role.
     * 
     * @return Number of users in the role.
     * 
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store
     * @throws SSOException
     *             if the single sign on token is no longer valid
     */
    public long getNumberOfUsers() throws AMException, SSOException;

    /**
     * Gets the DNs of users in the role.
     * 
     * @return The DNs of users in the role.
     * 
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store
     * @throws SSOException
     *             if the single sign on token is no longer valid
     */
    public Set getUserDNs() throws AMException, SSOException;

    /**
     * Searches for users in this role using wildcards and attribute values.
     * Wildcards can be specified such as a*, *, *a.
     * 
     * @param wildcard
     *            wildcard pattern to be used in the search
     * @param level
     *            the search level that needs to be used (
     *            <code>AMConstants.SCOPE_ONE</code>
     *            or <code>AMConstants.SCOPE_SUB</code>)
     * @return Set DNs of Users matching the search
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store
     * @throws SSOException
     *             if the single sign on token is no longer valid
     */
    public Set searchUsers(String wildcard, int level) throws AMException,
            SSOException;

    /**
     * Searches for users in this people container using wildcards and attribute
     * values. Wildcards can be specified such as a*, *, *a.
     * 
     * @param wildcard
     *            wildcard pattern to be used in the search
     * @param searchControl
     *            specifies the search scope to be used, VLV ranges etc.,
     * @return <code>AMSearchResults</code> which contains a Set DNs of Users
     *         matching the search.
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store
     * @throws SSOException
     *             if the single sign on token is no longer valid
     */
    public AMSearchResults searchUsers(String wildcard,
            AMSearchControl searchControl) throws AMException, SSOException;

    /**
     * Searches for users in this role using wildcards and attribute values.
     * Wildcards can be specified such as a*, *, *a. To further refine the
     * search, attribute-value pairs can be specified so that DNs of users with
     * matching attribute-value pairs will be returned.
     * 
     * @param wildcard
     *            wildcard pattern to be used in the search
     * @param avPairs
     *            attribute-value pairs to match when searching users
     * @param level
     *            the search level that needs to be used (
     *            <code>AMConstants.SCOPE_ONE</code>
     *            or <code>AMConstants.SCOPE_SUB</code>)
     * @return Set DNs of Users matching the search
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store
     * @throws SSOException
     *             if the single sign on token is no longer valid
     */
    public Set searchUsers(String wildcard, Map avPairs, int level)
            throws AMException, SSOException;

    /**
     * Searches for users in this group using wildcards and attribute values.
     * Wildcards can be specified such as a*, *, *a. To further refine the
     * search, attribute-value pairs can be specified so that DNs of users with
     * matching attribute-value pairs will be returned.
     * 
     * @param wildcard
     *            wildcard pattern to be used in the search
     * @param avPairs
     *            attribute-value pairs to match when searching users
     * @param searchControl
     *            specifies the search scope to be used, VLV ranges etc.
     * @return <code>AMSearchResults</code> which contains a DNs of Users
     *         matching the search.
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store
     * @throws SSOException
     *             if the single sign on token is no longer valid
     */
    public AMSearchResults searchUsers(String wildcard, Map avPairs,
            AMSearchControl searchControl) throws AMException, SSOException;

    /**
     * Searches for users in this role using attribute values. Wildcards such as
     * a*, *, *a can be specified for the attribute values. The DNs of users
     * with matching attribute-value pairs will be returned.
     * 
     * @param searchControl
     *            specifies the search scope to be used, VLV ranges etc.
     * @param avfilter
     *            this attribute-value pairs filter will be logical AND with
     *            user search filter.
     * @return <code>AMSearchResults</code> which contains a Set DNs of Users
     *         matching the search.
     * @throws AMException
     *             if there is an internal error in the AM Store.
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     */
    public AMSearchResults searchUsers(AMSearchControl searchControl,
            String avfilter) throws AMException, SSOException;

    /**
     * Get requested templates defined for this role.
     * 
     * @param templateReqs
     *            a Map of services names and template types. The key in the Map
     *            entry is the service name as a String, and the value of the
     *            Map entry is a <code>java.lang.Integer</code> whose integer
     *            value is one of <code>AMTemplate.DYNAMIC_TEMPLATE</code>
     *        <code>AMTemplate.POLICY_TEMPLATE</code>
     *        <code>AMTemplate.ORGANIZATION_TEMPLATE</code>
     *        <code>AMTemplate.ALL_TEMPLATES</code>
     * @return a Set of <code>AMTemplate</code> objects representing the
     *         templates requested. If the <code>templateReqs</code> argument
     *         is null or empty, the returned set will contain the
     *         <code>AMTemplates</code> for each registered service which has
     *         a template defined. If there is no template defined for any
     *         registered services for this role, an empty Set will be returned.
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     */
    public Set getTemplates(Map templateReqs) throws AMException, SSOException;

    /**
     * Get requested policy templates defined for this role.
     * 
     * @param serviceNames
     *            a Set of services names, each specified as a
     *            <code>java.lang.String</code>.
     * @return set of <code>AMTemplate</code> objects representing the policy
     *         templates requested. If the <code>serviceNames</code> argument
     *         is null or empty, the returned set will contain the
     *         <code>AMTemplates</code> for each registered service which has
     *         a policy template defined. If there is no policy template defined
     *         for any registered services for this role, an empty Set will be
     *         returned.
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store.
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     */
    public Set getPolicyTemplates(Set serviceNames) throws AMException,
            SSOException;

    /**
     * Gets all the assigned policies created for this role
     * 
     * @return Set a set of assigned policy DNs
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     */
    public Set getAssignedPolicyDNs() throws AMException, SSOException;

}
