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
 * $Id: AMPeopleContainer.java,v 1.4 2008/06/25 05:41:21 qcheng Exp $
 *
 */

package com.iplanet.am.sdk;

import java.util.Map;
import java.util.Set;

import com.iplanet.sso.SSOException;

/**
 * This interface provides methods to manage people container.
 * <code>AMPeopleContainer</code> objects can be obtained by using
 * <code>AMStoreConnection</code>. A handle to this object can be obtained by
 * using the DN of the object.
 * 
 * <PRE>
 * 
 * AMStoreConnection amsc = new AMStoreConnection(ssotoken); if
 * (amsc.doesEntryExist(pcDN)) { AMPeopleContainer pc =
 * amsc.getPeopleContainer(oDN); }
 * 
 * </PRE>
 *
 * @deprecated  As of Sun Java System Access Manager 7.1.
 * @supported.all.api
 */
public interface AMPeopleContainer extends AMObject {

    /**
     * Creates users in this people container. For each user the, object classes
     * specified by organization type attribute
     * <code>iplanet-am-required-services</code> of the service
     * <code>iPlanetAMAdminConsoleService</code> template are added. If a
     * corresponding template does not exist, the default values are picked up
     * from schema.
     * 
     * @param users
     *            The set of user names to be created in this people container.
     * @return Set Set of User objects created
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store
     * @throws SSOException
     *             if the single sign on token is no longer valid
     */
    public Set createUsers(Set users) throws AMException, SSOException;

    /**
     * Creates users and initializes their attributes. For each user the, object
     * classes specified by organization type attribute
     * <code>iplanet-am-required-services</code> of the service
     * <code>iPlanetAMAdminConsoleService</code> template are added. If a
     * corresponding template does not exist, the default values are picked up
     * from schema.
     * 
     * @param users
     *            Map where the key is the name of the user, and the value is a
     *            Map to represent Attribute-Value Pairs
     * @return Set Set of User objects created
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store
     * @throws SSOException
     *             if the single sign on token is no longer valid
     */
    public Set createUsers(Map users) throws AMException, SSOException;

    /**
     * Creates users and initializes their attributes.
     * 
     * @param users
     *            Map where the key is the name of the user, and the value is a
     *            Map to represent Attribute-Value Pairs
     * @param serviceNames
     *            Set of service names assigned to the users where the key is
     *            the name of the user, and the value is a Map to represent
     *            Attribute-Value Pairs
     * @return Set Set of User objects created
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store
     * @throws SSOException
     *             if the single sign on token is no longer valid
     */
    public Set createUsers(Map users, Set serviceNames) throws AMException,
            SSOException;

    /**
     * Create user and initializes the attributes. For each user the, object
     * classes specified by organization type attribute
     * <code>iplanet-am-required-services</code> of the service
     * <code>iPlanetAMAdminConsoleService</code> template are added. If a
     * corresponding template does not exist, the default values are picked up
     * from schema. Also services as defined in the arguments, are assigned to
     * the user, with default values being picked up from the service schema if
     * none are provided for required attributes of the service.
     * 
     * @param uid
     *            value of naming attribute for user.
     * @param attrMap
     *            attribute-values to be set in the user entry.
     * @param serviceNameAndAttrs
     *            service names and attributes to be assigned to the user.
     * @return AMUser object of newly created user.
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store.
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     */
    public AMUser createUser(String uid, Map attrMap, Map serviceNameAndAttrs)
            throws AMException, SSOException;

    /**
     * Deletes users from this people container.
     * 
     * @param users
     *            The set of user DN's to be deleted from the people container.
     * 
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store
     * @throws SSOException
     *             if the single sign on token is no longer valid
     */
    public void deleteUsers(Set users) throws AMException, SSOException;

    /**
     * Gets number of users in the people container.
     * 
     * @return Number of users in the people container.
     * 
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store
     * @throws SSOException
     *             if the single sign on token is no longer valid
     */
    public long getNumberOfUsers() throws AMException, SSOException;

    /**
     * Gets the names (DNs) of users in the people container.
     * 
     * @return Set The names(DNs) of users in the people container.
     * 
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store
     * @throws SSOException
     *             if the single sign on token is no longer valid
     */
    public Set getUserDNs() throws AMException, SSOException;

    /**
     * Creates sub people containers in this people container.
     * 
     * @param peopleContainersNames
     *            The set of people container names to be created in this people
     *            container.
     * @return set of <code>PeopleContainer</code> objects created.
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store
     * @throws SSOException
     *             if the single sign on token is no longer valid
     */
    public Set createSubPeopleContainers(Set peopleContainersNames)
            throws AMException, SSOException;

    /**
     * Creates sub people containers and initializes their attributes.
     * 
     * @param peopleContainers
     *            Map where the key is the name of the people container, and the
     *            value is a Map to represent Attribute-Value Pairs.
     * @return set of <code>PeopleContainer</code> objects created.
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store.
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     */
    public Set createSubPeopleContainers(Map peopleContainers)
            throws AMException, SSOException;

    /**
     * Gets the sub containers in this people container. It returns sub
     * containers either at one level or a whole subtree.
     * 
     * @param level
     *            the level (<code>SCOPE_ONE</code> or
     *            <code>SCOPE_TREE</code>) for returning sub containers
     * @return The sub container DNs in this people container.
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store and if the level is invalid.
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     */
    public Set getSubPeopleContainerDNs(int level) throws AMException,
            SSOException;

    /**
     * Gets number of sub people containers in the people container.
     * 
     * @return Number of sub people containers in the people container.
     * 
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store
     * @throws SSOException
     *             if the single sign on token is no longer valid
     */
    public long getNumberOfSubPeopleContainers() throws AMException,
            SSOException;

    /**
     * Deletes sub people containers in this people container.
     * 
     * @param peopleContainers
     *            The set of container DN's to be deleted from the people
     *            container.
     * 
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store
     * @throws SSOException
     *             if the single sign on token is no longer valid
     */
    public void deleteSubPeopleContainers(Set peopleContainers)
            throws AMException, SSOException;

    /**
     * Searches for users in this people container using wildcards and attribute
     * values. Wildcards can be specified such as a*, *, *a.
     * 
     * @param wildcard
     *            wildcard pattern to be used in the search
     * @param level
     *            the search level that needs to be used (
     *            <code>AMConstants.SCOPE_ONE</code>
     *            or <code>AMConstants.SCOPE_TREE</code>)
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
     * Searches for users in this organization using wildcards and attribute
     * values. Wildcards can be specified such as a*, *, *a. SDK users the
     * <code>userSearchTemplate</code>, if provided. Otherwise, it uses the
     * <code>BasicUserSearchTemplate</code>. Any <code>%U</code> in the
     * search template are replaced with the wildcard.
     * 
     * @param wildcard
     *            wildcard pattern to be used in the search
     * @param level
     *            the search level that needs to be used (
     *            <code>AMConstants.SCOPE_ONE</code>
     *            or <code>AMConstants.SCOPE_TREE</code>)
     * @param userSearchTemplate
     *            Name of search template to use. If null is passed then the
     *            default search template <code>BasicUserSearch</code> is
     *            used.
     * @param avPairs
     *            This option can be used to further qualify the search filter.
     *            The attribute-value pairs provided by this map are appended to
     *            the search filter.
     * 
     * @return Set DNs of Users matching the search
     * 
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store
     * @throws SSOException
     *             if the single sign on token is no longer valid
     */

    public Set searchUsers(String wildcard, int level,
            String userSearchTemplate, Map avPairs) throws AMException,
            SSOException;

    /**
     * Searches for users in this people container using wildcards and attribute
     * values. Wildcards can be specified such as a*, *, *a.
     * 
     * @param wildcard
     *            wildcard pattern to be used in the search
     * @param searchControl
     *            specifies the search scope to be used, VLV ranges etc.,
     * 
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
     * Searches for users in this group using wildcards and attribute values.
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
     *            or <code>AMConstants.SCOPE_TREE</code>)
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
     *            specifies the search scope to be used, VLV ranges etc.,
     * 
     * @return <code>AMSearchResults</code> which contains a Set DNs of Users
     *         matching the search.
     * 
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store
     * @throws SSOException
     *             if the single sign on token is no longer valid
     */
    public AMSearchResults searchUsers(String wildcard, Map avPairs,
            AMSearchControl searchControl) throws AMException, SSOException;

    /**
     * Searches for users in this people container using wildcards and attribute
     * values. Wildcards can be specified such as a*, *, *a. To further refine
     * the search, attribute-value pairs can be specified so that DNs of users
     * with matching attribute-value pairs will be returned.
     * 
     * @param wildcard
     *            wildcard pattern to be used in the search
     * @param searchControl
     *            specifies the search scope to be used, VLV ranges etc.,
     * @param avfilter
     *            this attribute-value pairs filter will be logical AND with
     *            user search filter.
     * @return <code>AMSearchResults</code> which contains a Set DNs of Users
     *         matching the search.
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store.
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     */
    public AMSearchResults searchUsers(String wildcard,
            AMSearchControl searchControl, String avfilter) throws AMException,
            SSOException;

    /**
     * Searches for users in this people container using attribute values.
     * Wildcards such as a*, *, *a can be specified for the attribute values.
     * The DNs of users with matching attribute-value pairs will be returned.
     * 
     * @param searchControl
     *            specifies the search scope to be used, VLV ranges etc.
     * @param avfilter
     *            this attribute-value pairs filter will be logical AND with
     *            user search filter.
     * @return <code>AMSearchResults</code> which contains a Set DNs of Users
     *         matching the search.
     * @throws AMException
     *             if there is an internal error in the AM Store
     * @throws SSOException
     *             if the single sign on token is no longer valid
     */
    public AMSearchResults searchUsers(AMSearchControl searchControl,
            String avfilter) throws AMException, SSOException;

    /**
     * Searches for users in this people container using wildcards and attribute
     * values. Wildcards can be specified such as a*, *, *a. To further refine
     * the search, attribute-value pairs can be specified so that DNs of users
     * with matching attribute-value pairs will be returned.
     * 
     * @param wildcard
     *            wildcard pattern to be used in the search
     * @param avPairs
     *            attribute-value pairs to match when searching users
     * @param userSearchTemplate
     *            Name of user search template to be used.
     * @param searchControl
     *            specifies the search scope to be used, VLV ranges etc.,
     * @return <code>AMSearchResults</code> which contains a Set DNs of Users
     *         matching the search.
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store.
     * @throws SSOException
     *             if the single sign on token is no longer valid
     */
    public AMSearchResults searchUsers(String wildcard, Map avPairs,
            String userSearchTemplate, AMSearchControl searchControl)
            throws AMException, SSOException;

    /**
     * Create resources in this organization. Resource is a specialized entity,
     * which is created in the people container and is a leaf node.
     * 
     * @param resourceNames
     *            Names of the resources to be created.
     * @return Set of <code>AMResource</code> objects
     * @throws AMException
     *             If there is an error when trying to save to the data store
     * @throws SSOException
     *             If the SSO Token is invalid.
     */
    public Set createResources(Set resourceNames) throws AMException,
            SSOException;

    /**
     * Create resources in this organization. Resource is a specialized entity,
     * which is created in the people container and is a leaf node.
     * 
     * @param resourceMap
     *            Map of resource names to be created and attributes to be set
     *            in the newly creates resource.
     * @return Set of <code>AMResource</code> objects.
     * @throws AMException
     *             If there is an error when trying to save to the data store
     * @throws SSOException
     *             If the SSO Token is invalid.
     */
    public Set createResources(Map resourceMap) throws AMException,
            SSOException;

    /**
     * Create managed entities in this container. Supported entities, as defined
     * in the configuration service <code>DAI</code>, can be created using
     * this method.
     * 
     * @param type
     *            Type of entity to be create (For example "user" or "agent")
     * @param entities
     *            Set of names for the entities to be created
     * @return Set of <code>AMEntity</code> objects
     * @throws AMException
     *             If there is an error when trying to save to the data store
     * @throws SSOException
     *             If the SSO Token is invalid.
     */
    public Set createEntities(String type, Set entities) throws AMException,
            SSOException;

    /**
     * Creates entities and initializes their attributes. Supported entities, as
     * defined in the configuration service <code>DAI</code>, can be created
     * using this method.
     * 
     * @param stype
     *            Type of entity to be create (For example "user" or "agent")
     * @param entities
     *            Map where the key is the name of the entity, and the value is
     *            a Map to represent Attribute-Value Pairs
     * @return Set Set of <code>AMEntity</code> objects created
     * @throws AMException
     *             if an error is encountered when trying to create entity in
     *             the data store.
     * @throws SSOException
     *             if the single sign on token is no longer valid
     */
    public Set createEntities(String stype, Map entities) throws AMException,
            SSOException;

    /**
     * Searches for resources in this organization using wildcards and attribute
     * values. Wildcards can be specified such as a*, *, *a. SDK uses the
     * <code>resourceSearchTemplate</code>, if provided. Otherwise, it uses
     * the <code>BasicResourceSearchTemplate</code>.
     * 
     * @param wildcard
     *            wildcard pattern to be used in the search
     * @param level
     *            the search level that needs to be used (
     *            <code>AMConstants.SCOPE_ONE</code>
     *            or <code>AMConstants.SCOPE_TREE</code>)
     * @param resourceSearchTemplate
     *            Name of resource search template to be used.
     * @param avPairs
     *            This option can be used to further qualify the search filter.
     *            The attribute-value pairs provided by this map are appended to
     *            the search filter.
     * @return Set DNs of resources matching the search
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store.
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     */
    public Set searchResources(String wildcard, int level,
            String resourceSearchTemplate, Map avPairs) throws AMException,
            SSOException;

    /**
     * Searches for resources in this people container using wildcards and
     * attribute values. Wildcards can be specified such as a*, *, *a. To
     * further refine the search, attribute-value pairs can be specified so that
     * DNs of users with matching attribute-value pairs will be returned.
     * 
     * @param wildcard
     *            wildcard pattern to be used in the search
     * @param avPairs
     *            attribute-value pairs to match when searching users
     * @param rSearchTemplate
     *            Name of resource search template to be used.
     * @param searchControl
     *            specifies the search scope to be used, VLV ranges etc.,
     * @return <code>AMSearchResults</code> which contains a Set DNs of
     *         resources matching the search.
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store
     * @throws SSOException
     *             if the single sign on token is no longer valid
     */
    public AMSearchResults searchResources(String wildcard, Map avPairs,
            String rSearchTemplate, AMSearchControl searchControl)
            throws AMException, SSOException;

    /**
     * Searches for resources in this organization using wildcards and attribute
     * values. Wildcards can be specified such as a*, *, *a. SDK uses the
     * <code>eSearchTemplate</code>, if provided. Otherwise, it uses the
     * <code>BasicEntitySearch</code> template.
     * 
     * @param wildcard
     *            wildcard pattern to be used in the search
     * @param level
     *            the search level that needs to be used (
     *            <code>AMConstants.SCOPE_ONE</code>
     *            or <code>AMConstants.SCOPE_SUB</code>)
     * @param eSearchTemplate
     *            Name of search template to be used.
     * @param avPairs
     *            This option can be used to further qualify the search filter.
     *            The attribute-value pairs provided by this map are appended to
     *            the search filter.
     * @return Set DNs of resources matching the search
     * 
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store.
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     */
    public Set searchEntities(String wildcard, int level,
            String eSearchTemplate, Map avPairs) throws AMException,
            SSOException;

    /**
     * Searches for entities in this people container using wildcards and
     * attribute values. Wildcards can be specified such as a*, *, *a. To
     * further refine the search, attribute-value pairs can be specified so that
     * DNs of users with matching attribute-value pairs will be returned.
     * 
     * @param wildcard
     *            wildcard pattern to be used in the search
     * @param avPairs
     *            attribute-value pairs to match when searching users
     * @param eSearchTemplate
     *            Name of search template to be used.
     * @param searchControl
     *            specifies the search scope to be used, VLV ranges etc.,
     * @return <code>AMSearchResults</code> which contains a Set DNs of
     *         resources matching the search.
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store
     * @throws SSOException
     *             if the single sign on token is no longer valid
     */
    public AMSearchResults searchEntities(String wildcard, Map avPairs,
            String eSearchTemplate, AMSearchControl searchControl)
            throws AMException, SSOException;

    /**
     * Searches for entities in this people container using wildcards and a
     * filter. Wildcards can be specified such as a*, *, *a. To further refine
     * the search, a filter can be passed, which is used to further qualify the
     * basic entity search filter.
     * 
     * @param wildcard
     *            wildcard pattern to be used in the search
     * @param searchControl
     *            specifies the search scope to be used, VLV ranges etc.
     * @param avfilter
     *            this attribute-value pairs filter will be logical AND with
     *            user search filter.
     * @param eSearchTemplate
     *            Name of search template to be used. If a null is passed then
     *            the default search template for entities
     *            <code>BasicEntitySearch</code> is used.
     * @return <code>AMSearchResults</code> which contains a Set DNs of Users
     *         matching the search.
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store.
     * @throws SSOException
     *             if the single sign on token is no longer valid
     */
    public AMSearchResults searchEntities(String wildcard,
            AMSearchControl searchControl, String avfilter,
            String eSearchTemplate) throws AMException, SSOException;

    /**
     * Deletes a set of resources in this people container.
     * 
     * @param resources
     *            The set of resource DNs to be removed.
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store.
     * @throws SSOException
     *             if the single sign on token is no longer valid
     */
    public void deleteResources(Set resources) throws AMException, SSOException;

    /**
     * Deletes a set of resources in this people container.
     * 
     * @param resources
     *            The set of resource DNs to be removed.
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store.
     * @throws SSOException
     *             if the single sign on token is no longer valid
     */
    public void deleteEntities(Set resources) throws AMException, SSOException;

    /**
     * Searches for sub people containers in this people container using
     * wildcards and attribute values. Wildcards can be specified such as a*, *,
     * *a.
     * 
     * @param wildcard
     *            wildcard pattern to be used in the search
     * @param level
     *            the search level that needs to be used (
     *            <code>AMConstants.SCOPE_ONE</code>
     *            or <code>AMConstants.SCOPE_SUB</code>)
     * @return Set DNs of people containers matching the search
     * 
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store
     * @throws SSOException
     *             if the single sign on token is no longer valid
     */
    public Set searchSubPeopleContainers(String wildcard, int level)
            throws AMException, SSOException;

    /**
     * Searches for sub people containers in this people container using
     * wildcards and attribute values. Wildcards can be specified such as a*, *,
     * *a. To further refine the search, attribute-value pairs can be specified
     * so that DNs of people containers with matching attribute-value pairs will
     * be returned.
     * 
     * @param wildcard
     *            wildcard pattern to be used in the search
     * @param avPairs
     *            attribute-value pairs to match when searching people
     *            Containers
     * @param level
     *            the search level that needs to be used (
     *            <code>AMConstants.SCOPE_ONE</code>
     *            or <code>AMConstants.SCOPE_SUB</code>)
     * @return Set DNs of people containers matching the search
     * 
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store
     * @throws SSOException
     *             if the single sign on token is no longer valid
     */
    public Set searchSubPeopleContainers(
            String wildcard, Map avPairs, int level)
            throws AMException, SSOException;
}
