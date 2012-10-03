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
 * $Id: AMGroup.java,v 1.4 2008/06/25 05:41:20 qcheng Exp $
 *
 */

package com.iplanet.am.sdk;

import java.util.Map;
import java.util.Set;

import com.iplanet.sso.SSOException;

/**
 * The <code>AMGroup</code> interface provides methods to manage group
 *
 * @deprecated  As of Sun Java System Access Manager 7.1.
 * @supported.all.api
 */
abstract public interface AMGroup extends AMObject {
    /**
     * Returns number of users in the group.
     * 
     * @return Number of users in the group.
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store.
     * @throws SSOException
     *             if the single sign on token no longer valid.
     */
    public long getNumberOfUsers() throws AMException, SSOException;

    /**
     * Returns the distinguished name of users in the group.
     * 
     * @return a set of user distinguished names in the group.
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store.
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     */
    public Set getUserDNs() throws AMException, SSOException;

    /**
     * Returns the distinguished name of users and nested groups in the group.
     * 
     * @return The distinguished name of users and nested groups in the group.
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store.
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     */
    public Set getUserAndGroupDNs() throws AMException, SSOException;

    /**
     * Nests the given group distinguished names in this the group. This will
     * effectively make the groups <code>members</code> of this group. And any
     * ACIs set for this group will be inherited by the nested groups and their
     * members.
     * 
     * @param groups
     *            The set of group distinguished names to be nested in this
     *            group.
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store.
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     */
    public void addNestedGroups(Set groups) throws AMException, SSOException;

    /**
     * Searches for users in this group using wildcards and attribute values.
     * Wildcards can be specified such as a*, *, *a.
     * 
     * @param wildcard
     *            wildcard pattern to be used in the search
     * @return Set of distinguished name of users matching the search
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store
     * @throws SSOException
     *             if the single sign on token is no longer valid
     */
    public Set searchUsers(String wildcard) throws AMException, SSOException;

    /**
     * Searches for users in this group using wildcards. Wildcards can be
     * specified such as a*, *, *a.
     * 
     * @param wildcard
     *            wildcard pattern to be used in the search
     * @param avPairs
     *            attribute-value pairs to match when searching users
     * @param searchControl
     *            specifies the size limit and time limit
     * @return <code>AMSearchResults</code> which contains a set of
     *         distinguished name of Users matching the search.
     * @throws AMException
     *             if there is an internal error in the access management store.
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     */
    public AMSearchResults searchUsers(String wildcard, Map avPairs,
            AMSearchControl searchControl) throws AMException, SSOException;

    /**
     * Searches for users in this group using attribute values. Wildcards such
     * as a*, *, *a can be specified for the attribute values. The distinguished
     * names of users with matching attribute-value pairs will be returned.
     * 
     * @param searchControl
     *            specifies the search scope to be used, VLV ranges etc.,
     * @param avfilter
     *            this attribute-value pairs filter will be logical AND with
     *            user search filter.
     * @return <code>AMSearchResults</code> which contains a set of
     *         distinguished names of Users matching the search.
     * @throws AMException
     *             if there is an internal error in the access management Store.
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     */
    public AMSearchResults searchUsers(AMSearchControl searchControl,
            String avfilter) throws AMException, SSOException;

    /**
     * Creates static groups in this group.
     * 
     * @param groupNames
     *            The set of static groups' names to be created in this group.
     * @return set of static group objects created.
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store.
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     */
    public Set createStaticGroups(Set groupNames) throws AMException,
            SSOException;

    /**
     * Creates static groups and initializes their attributes.
     * 
     * @param groups
     *            Map where the key is the name of the static group, and the
     *            value is a Map to represent Attribute-Value Pairs
     * @return Set of group objects created.
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store.
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     */
    public Set createStaticGroups(Map groups) throws AMException, SSOException;

    /**
     * Creates dynamic groups in this group.
     * 
     * @param groupNames
     *            The set of dynamic groups' names to be created in this group.
     * @return Set of dynamic group objects created
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store.
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     */
    public Set createDynamicGroups(Set groupNames) throws AMException,
            SSOException;

    /**
     * Creates dynamic groups and initializes their attributes.
     * 
     * @param groups
     *            Map of name of the dynamic group to attribute-value pairs map.
     * @return Set of dynamic group objects created.
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store.
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     */
    public Set createDynamicGroups(Map groups) throws AMException, SSOException;

    /**
     * Creates assignable dynamic groups in this group.
     * 
     * @param groupNames
     *            The set of assignable dynamic groups' names to be created in
     *            this group.
     * @return Set of assignable dynamic group objects created.
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store.
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     */
    public Set createAssignableDynamicGroups(Set groupNames)
            throws AMException, SSOException;

    /**
     * Creates assignable dynamic groups and initializes their attributes.
     * 
     * @param groups
     *            Map of name of the assignable dynamic group to attribute-value
     *            pairs map;
     * @return Set of assignable dynamic group objects created.
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store.
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     */
    public Set createAssignableDynamicGroups(Map groups) throws AMException,
            SSOException;

    /**
     * Gets the groups in this group. It returns groups either at one level or a
     * whole subtree.
     * 
     * @param level
     *            the level (<code>AMConstants.SCOPE_ONE</code> or
     *            <code>AMConstants.SCOPE_SUB</code>) for returning groups.
     * @return The group distinguished names in this group.
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store or if the level is invalid.
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     */
    public Set getGroupDNs(int level) throws AMException, SSOException;

    /**
     * Returns the groups nested in this group.
     * 
     * @return The group distinguished names nested in this group.
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store.
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     */
    public Set getNestedGroupDNs() throws AMException, SSOException;

    /**
     * Removes groups which are nested in this group.
     * 
     * @param groups
     *            The set of user DN's to be removed from the static group.
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store
     * @throws SSOException
     *             if the single sign on token is no longer valid
     */
    public void removeNestedGroups(Set groups) throws AMException, SSOException;

    /**
     * Returns number of groups in the group. It returns number of groups either
     * at one level or a whole subtree.
     * 
     * @param level
     *            the level (<code>AMConstants.SCOPE_ONE</code> or
     *            <code>AMConstants.SCOPE_SUB</code>) for returning groups.
     * @return Number of groups in the group.
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store.
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     */
    public long getNumberOfGroups(int level) throws AMException, SSOException;

    /**
     * Deletes static groups in this group.
     * 
     * @param groupDNs
     *            The set of static group distinguished names to be deleted from
     *            this group.
     * 
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store.
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     */
    public void deleteStaticGroups(Set groupDNs) throws AMException,
            SSOException;

    /**
     * Deletes dynamic groups in this group.
     * 
     * @param groupDNs
     *            The set of dynamic group distinguished names to be deleted
     *            from this group.
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store
     * @throws SSOException
     *             if the single sign on token is no longer valid
     */
    public void deleteDynamicGroups(Set groupDNs) throws AMException,
            SSOException;

    /**
     * Deletes assignable dynamic groups in this group.
     * 
     * @param groupDNs
     *            The set of assignable dynamic group distinguished names to be
     *            deleted from this group.
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store.
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     */
    public void deleteAssignableDynamicGroups(Set groupDNs) throws AMException,
            SSOException;

    /**
     * Searches for groups in this group using wildcards. Wildcards can be
     * specified such as a*, *, *a.
     * 
     * @param wildcard
     *            wildcard pattern to be used in the search
     * @param level
     *            the search level that needs to be used (
     *            <code>AMConstants.SCOPE_ONE</code>
     *            or <code>AMConstants.SCOPE_TREE</code>).
     * @return Set of distinguished name of sub groups matching the search
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store.
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     */
    public Set searchGroups(String wildcard, int level) throws AMException,
            SSOException;

    /**
     * Searches for groups in this group using wildcards and attribute values.
     * Wildcards can be specified such as a*, *, *a.
     * 
     * @param wildcard
     *            wildcard pattern to be used in the search
     * @param avPairs
     *            attribute-value pairs to match when searching groups
     * @param level
     *            the search level that needs to be used (
     *            <code>AMConstants.SCOPE_ONE</code>
     *            or <code>AMConstants.SCOPE_TREE</code>)
     * @return Set of distinguished name of groups matching the search
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store.
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     */
    public Set searchGroups(String wildcard, Map avPairs, int level)
            throws AMException, SSOException;

    /**
     * Searches for groups in this group using wildcards and attribute values.
     * Wildcards can be specified such as a*, *, *a.
     * 
     * @param wildcard
     *            wildcard pattern to be used in the search
     * @param avPairs
     *            attribute-value pairs to match when searching groups
     * @param searchControl
     *            specifies the search scope to be used
     * @return <code>AMSearchResults</code> which contains a set of
     *         distinguished name of groups matching the search.
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store.
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     */
    public AMSearchResults searchGroups(String wildcard, Map avPairs,
            AMSearchControl searchControl) throws AMException, SSOException;
}
