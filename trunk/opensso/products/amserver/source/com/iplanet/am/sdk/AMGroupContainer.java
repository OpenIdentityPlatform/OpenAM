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
 * $Id: AMGroupContainer.java,v 1.4 2008/06/25 05:41:20 qcheng Exp $
 *
 */

package com.iplanet.am.sdk;

import java.util.Map;
import java.util.Set;

import com.iplanet.sso.SSOException;

/**
 * This interface provides methods to manage group container.
 * <code>AMGroupContainer</code> objects can be obtained by using
 * <code>AMStoreConnection</code>. A handle to this object can be obtained by
 * using the DN of the object.
 * 
 * <PRE>
 * 
 * AMStoreConnection amsc = new AMStoreConnection(ssotoken); if
 * (amsc.doesEntryExist(gcDN)) { AMGroupContainer dg =
 * amsc.getGroupContainer(gcDN); }
 * 
 * </PRE>
 *
 * @deprecated  As of Sun Java System Access Manager 7.1.
 * @supported.all.api
 */
public interface AMGroupContainer extends AMObject {
    /**
     * Creates sub group containers in this group container.
     * 
     * @param groupContainers
     *            The set of group container names to be created in this group
     *            container.
     * @return Set set of group container objects created.
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store.
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     */
    public Set createSubGroupContainers(Set groupContainers)
            throws AMException, SSOException;

    /**
     * Creates sub group containers and initializes their attributes.
     * 
     * @param groupContainers
     *            Map where the key is the name of the group container, and the
     *            value is a Map to represent Attribute-Value Pairs .
     * @return Set of group container objects created.
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store.
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     */
    public Set createSubGroupContainers(Map groupContainers)
            throws AMException, SSOException;

    /**
     * Returns the sub containers in this group container. It returns sub
     * containers either at one level or a whole subtree.
     * 
     * @param level
     *            <code>AMConstants.SCOPE_ONE</code> or
     *            <code>AMConstants.SCOPE_SUB</code> for returning sub
     *            containers.
     * @return The sub container distinguished names in this group container.
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store or if level is invalid.
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     */
    public Set getSubGroupContainerDNs(int level) throws AMException,
            SSOException;

    /**
     * Returns number of sub group containers in the group container.
     * 
     * @return Number of sub group containers in the group container.
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store.
     * @throws SSOException
     *             if the single sign token on is no longer valid.
     */
    public long getNumberOfSubGroupContainers() throws AMException,
            SSOException;

    /**
     * Deletes sub group containers in this group container.
     * 
     * @param groupContainers
     *            set of container distinguished name to be deleted from the
     *            group container.
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store.
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     */
    public void deleteSubGroupContainers(Set groupContainers)
            throws AMException, SSOException;

    /**
     * Searches for sub group containers in this group container using wildcards
     * and attribute values. Wildcards can be specified such as a*, *, *a. To
     * further refine the search, attribute-value pairs can be specified so that
     * distinguished name of group containers with matching attribute-value
     * pairs will be returned.
     * 
     * @param wildcard
     *            wildcard pattern to be used in the search.
     * @param avPairs
     *            attribute-value pairs to match when searching group
     *            Containers.
     * @param level
     *            the search level that needs to be used
     *            <code>AMConstants.SCOPE_ONE</code> or
     *            <code>AMConstants.SCOPE_SUB</code>.
     * @return Set distinguished name of group containers matching the search.
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     */
    public Set searchSubGroupContainers(String wildcard, Map avPairs, int level)
            throws AMException, SSOException;

    /**
     * Searches for group containers in this group container using wildcards and
     * attribute values. Wildcards can be specified such as a*, *, *a. To
     * further refine the search, attribute-value pairs can be specified so that
     * distinguished names of group containers with matching attribute-value
     * pairs will be returned.
     * 
     * @param wildcard
     *            wildcard pattern to be used in the search.
     * @param avPairs
     *            attribute-value pairs to match when searching group
     *            containers.
     * @param searchControl
     *            specifies the search scope to be used, VLV ranges etc.,
     * @return <code>AMSearchResults</code> object which contains the set
     *         distinguished name of group containers matching the search.
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store.
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     */
    public AMSearchResults searchSubGroupContainers(String wildcard,
            Map avPairs, AMSearchControl searchControl) throws AMException,
            SSOException;

    /**
     * Creates static groups in this group container.
     * 
     * @param groupNames
     *            The set of static groups' names to be created in this group
     *            container.
     * @return set of static group objects created.
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store.
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     * @throws AMException
     *             if an error is encountered when trying to create entries in
     *             the data store.
     */
    public Set createStaticGroups(Set groupNames) throws AMException,
            SSOException;

    /**
     * Creates static groups and initializes their attributes.
     * 
     * @param groups
     *            Map where the key is the name of the static group, and the
     *            value is a Map to represent attribute-value Pairs.
     * @return Set of group objects created.
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     * @throws AMException
     *             if an error is encountered when trying to create entries in
     *             the data store.
     */
    public Set createStaticGroups(Map groups) throws AMException, SSOException;

    /**
     * Creates static group. Takes <code>serviceNameAndAttr</code> map so that
     * services can be assigned to the group which is just created.
     * 
     * @param name
     *            of group to be created.
     * @param attributes
     *            attributes to be set in group node.
     * @param serviceNameAndAttrs
     *            map of service name to attribute map where the map is like
     *            this:
     * 
     * <pre>
     * &lt;serviceName&gt;&lt;AttrMap&gt;
     *       (attrMap=&lt;attrName&gt;&lt;Set of attrvalues&gt;)
     * </pre>
     * 
     * @return the newly created group.
     * @throws AMException
     *             if an error is encountered when trying to create entries in
     *             the data store.
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     */
    public AMGroup createStaticGroup(String name, Map attributes,
            Map serviceNameAndAttrs) throws AMException, SSOException;

    /**
     * Creates dynamic groups in this group container.
     * 
     * @param groupNames
     *            The set of dynamic groups' names to be created in this group
     *            container.
     * @return Set of dynamic group objects created.
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
     *            map of dynamic group's name to its attribute-value pairs map.
     * @return Set of dynamic group objects created.
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store.
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     */
    public Set createDynamicGroups(Map groups) throws AMException, SSOException;

    /**
     * Creates dynamic group. Takes <code>serviceNameAndAttr</code> map so
     * that services can be assigned to the group which is just created.
     * 
     * @param name
     *            of group to be created.
     * @param attributes
     *            attributes to be set in group
     * @param serviceNameAndAttrs
     *            map of service name and attribute maps where the map is like
     *            this:
     * 
     * <pre>
     * &lt;serviceName&gt;&lt;AttrMap&gt;
     *      (attrMap=&lt;attrName&gt;&lt;Set of attrvalues&gt;)
     * </pre>
     * 
     * @return <code>AMGroup</code> object of newly created group.
     * @throws AMException
     *             if an error is encountered when trying to create entries in
     *             the data store.
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     */
    public AMGroup createDynamicGroup(String name, Map attributes,
            Map serviceNameAndAttrs) throws AMException, SSOException;

    /**
     * Creates assignable dynamic groups in this group container.
     * 
     * @param groupNames
     *            The set of assignable dynamic groups' names to be created in
     *            this group container.
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
     *            Map where the key is the name of the assignable dynamic group,
     *            and the value is a Map to represent attribute-value pairs.
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
     * Creates assignable dynamic group. Takes <code>serviceNameAndAttr</code>
     * map so that services can be assigned to the group which is just created.
     * 
     * @param name
     *            of group to be created.
     * @param attributes
     *            attribute-value pairs to be set.
     * @param serviceNameAndAttrs
     *            map of service name to attribute map where the map is like
     *            this:
     * 
     * <pre>
     * &lt;serviceName&gt;&lt;AttrMap&gt;
     *      (attrMap=&lt;attrName&gt;&lt;Set of attrvalues&gt;)
     * </pre>
     * 
     * @return <code>AMGroup</code> object of newly created group.
     * @throws AMException
     *             if an error is encountered when trying to create entries in
     *             the data store.
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     */
    public AMGroup createAssignableDynamicGroup(String name, Map attributes,
            Map serviceNameAndAttrs) throws AMException, SSOException;

    /**
     * Gets the groups in this group container. It returns groups either at one
     * level or a whole subtree.
     * 
     * @param level
     *            the level (<code>AMConstants.SCOPE_ONE</code> or
     *            <code>AMConstants.SCOPE_SUB</code>) for returning groups.
     * @return the group distinguished names in this group container.
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store or if the level is invalid.
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     */
    public Set getGroupDNs(int level) throws AMException, SSOException;

    /**
     * Returns number of groups in the group container. It returns number of
     * groups either at one level or a whole subtree.
     * 
     * @param level
     *            the level (<code>AMConstants.SCOPE_ONE</code> or
     *            <code>AMConstants.SCOPE_SUB</code>) for returning groups.
     * @return the number of groups in the group container.
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store.
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     */
    public long getNumberOfGroups(int level) throws AMException, SSOException;

    /**
     * Deletes static groups in this group container.
     * 
     * @param groupDNs
     *            The set of static group distinguished name to be deleted from
     *            this group container.
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store.
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     */
    public void deleteStaticGroups(Set groupDNs) throws AMException,
            SSOException;

    /**
     * Deletes dynamic groups in this group container.
     * 
     * @param groupDNs
     *            The set of dynamic group distinguished names to be deleted
     *            from this group container.
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store.
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     */
    public void deleteDynamicGroups(Set groupDNs) throws AMException,
            SSOException;

    /**
     * Deletes assignable dynamic groups in this group container.
     * 
     * @param groupDNs
     *            The set of assignable dynamic group distinguished names to be
     *            deleted from this group container.
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store.
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     */
    public void deleteAssignableDynamicGroups(Set groupDNs) throws AMException,
            SSOException;

    /**
     * Searches for groups in this group container using wildcards and attribute
     * values. Wildcards can be specified such as a*, *, *a.
     * 
     * @param wildcard
     *            pattern to be used in the search.
     * @param avPairs
     *            attribute-value pairs to match when searching groups.
     * @param level
     *            the search level that needs to be used
     *            <code>AMConstants.SCOPE_ON</code> or
     *            <code>AMConstants.SCOPE_SUB</code>.
     * @return Set of distinguished name of groups matching the search.
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store.
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     */
    public Set searchGroups(String wildcard, Map avPairs, int level)
            throws AMException, SSOException;

    /**
     * Searches for groups in this group container using wildcards. Wildcards
     * can be specified such as a*, *, *a. Uses the
     * <code>groupSearchTemplate</code>, if provided. Otherwise the default
     * search templates for the types of groups are used.
     * 
     * @param wildcard
     *            pattern to be used in the search.
     * @param level
     *            the search level that needs to be used
     *            <code>AMConstants.SCOPE_ONE</code> or
     *            <code>AMConstants.SCOPE_SUB</code>.
     * @param groupSearchTemplate
     *            name of the search template to be used to perform this search.
     * @param avPairs
     *            This option can be used to further qualify the search filter.
     *            The attribute-value pairs provided by this map are appended to
     *            the search filter.
     * @return Set of distinguished name of assignable dynamic groups matching
     *         the search.
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store.
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     */
    public Set searchGroups(String wildcard, int level,
            String groupSearchTemplate, Map avPairs) throws AMException,
            SSOException;

    /**
     * Searches for assignable dynamic groups in this group container using
     * wildcards and attribute values. Wildcards can be specified such as a*, *,
     * *a. To further refine the search, attribute-value pairs can be specified
     * so that distinguished name of dynamic groups with matching
     * attribute-value pairs will be returned.
     * 
     * @param wildcard
     *            pattern to be used in the search.
     * @param avPairs
     *            attribute-value pairs to match when searching assignable
     *            dynamic groups.
     * @param groupSearchTemplate
     *            Name of search template to be used to perform the search.
     * @param searchControl
     *            specifies the search scope to be used, VLV ranges etc.
     * @return <code>AMSearchResults</code> which contains a set of
     *         distinguished name of assignable dynamic groups matching the
     *         search.
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store.
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     */
    public AMSearchResults searchGroups(String wildcard, Map avPairs,
            String groupSearchTemplate, AMSearchControl searchControl)
            throws AMException, SSOException;

    /**
     * Searches for groups in this group container using wildcards and attribute
     * values. Wildcards can be specified such as a*, *, *a.
     * 
     * @param wildcard
     *            pattern to be used in the search.
     * @param avPairs
     *            attribute-value pairs to match when searching groups.
     * @param searchControl
     *            specifies the search scope to be used.
     * @return <code>AMSearchResults</code> which contains Set a of
     *         distinguished name of groups matching the search.
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store.
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     */
    public AMSearchResults searchGroups(String wildcard, Map avPairs,
            AMSearchControl searchControl) throws AMException, SSOException;

    /**
     * Searches for static groups in this group container using wildcards and
     * attribute values. Wildcards can be specified such as a*, *, *a.
     * 
     * @param wildcard
     *            pattern to be used in the search.
     * @param avPairs
     *            attribute-value pairs to match when searching groups.
     * @param searchControl
     *            specifies the search scope to be used.
     * @return <code>AMSearchResults</code> which contains Set a of
     *         distinguished name of groups matching the search.
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store.
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     */
    public AMSearchResults searchStaticGroups(String wildcard, Map avPairs,
            AMSearchControl searchControl) throws AMException, SSOException;

}
