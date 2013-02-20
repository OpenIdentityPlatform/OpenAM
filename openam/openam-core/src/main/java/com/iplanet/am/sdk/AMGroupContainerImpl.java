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
 * $Id: AMGroupContainerImpl.java,v 1.6 2008/06/25 05:41:20 qcheng Exp $
 *
 */

package com.iplanet.am.sdk;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.SchemaType;
import com.sun.identity.sm.ServiceSchema;
import com.sun.identity.sm.ServiceSchemaManager;

/**
 * The <code>AMGroupContainerImpl</code> class implements interface
 * AMGroupContainer
 *
 * @deprecated  As of Sun Java System Access Manager 7.1.
 */
class AMGroupContainerImpl extends AMObjectImpl implements AMGroupContainer {
    
    public AMGroupContainerImpl(SSOToken ssoToken, String DN) {
        super(ssoToken, DN, GROUP_CONTAINER);
    }

    /**
     * Creates sub GroupContainers in this group container.
     * 
     * @param groupContainerNames
     *            The set of GroupContainer names to be created in this group
     *            container.
     * @return Set set of GroupContainer objects created
     * @throws AMException
     *             if there is an internal error in the AM Store
     * @throws SSOException
     *             if the sign on is no longer valid
     */
    public Set createSubGroupContainers(Set groupContainerNames)
            throws AMException, SSOException {
        Iterator iter = groupContainerNames.iterator();
        Set groupContainers = new HashSet();

        while (iter.hasNext()) {
            String groupContainerDN = AMNamingAttrManager
                    .getNamingAttr(GROUP_CONTAINER)
                    + "=" + ((String) iter.next()) + "," + entryDN;
            AMGroupContainerImpl groupContainerImpl = new AMGroupContainerImpl(
                    token, groupContainerDN);
            groupContainerImpl.create();
            groupContainers.add(groupContainerImpl);
        }

        return groupContainers;
    }

    /**
     * Creates sub group containers and initializes their attributes.
     * 
     * @param groupContainersMap
     *            Map where the key is the name of the group container, and the
     *            value is a Map to represent Attribute-Value Pairs
     * 
     * @param Set
     *            Set of GroupContainer objects created.
     * @throws AMException
     *             if there is an internal error in the AM Store
     * @throws SSOException
     *             if the sign on is no longer valid
     */
    public Set createSubGroupContainers(Map groupContainersMap)
            throws AMException, SSOException {
        Iterator iter = groupContainersMap.keySet().iterator();
        Set groupContainers = new HashSet();

        while (iter.hasNext()) {
            String groupContainerName = (String) iter.next();
            String groupContainerDN = AMNamingAttrManager
                    .getNamingAttr(GROUP_CONTAINER)
                    + "=" + groupContainerName + "," + entryDN;

            Map attributes = (Map) groupContainersMap.get(groupContainerName);
            AMGroupContainerImpl groupContainerImpl = new AMGroupContainerImpl(
                    token, groupContainerDN);
            groupContainerImpl.setAttributes(attributes);
            groupContainerImpl.create();
            groupContainers.add(groupContainerImpl);
        }

        return groupContainers;
    }

    /**
     * Gets the sub containers in this group container. It returns sub
     * containers either at one level or a whole subtree.
     * 
     * @param level
     *            the level(SCOPE_ONE or SCOPE_SUB) for returning subcontainers
     * @return The sub container DNs in this group container.
     * 
     * @throws AMException
     *             if there is an internal error in the AM Store or if if the
     *             level is invalid
     * @throws SSOException
     *             if the sign on is no longer valid
     */
    public Set getSubGroupContainerDNs(int level) throws AMException,
            SSOException {
        return search(level, getSearchFilter(AMObject.GROUP_CONTAINER));
    }

    /**
     * Gets number of sub group containers in the group container.
     * 
     * @return Number of sub group containers in the group container.
     * 
     * @throws AMException
     *             if there is an internal error in the AM Store
     * @throws SSOException
     *             if the sign on is no longer valid
     */
    public long getNumberOfSubGroupContainers() throws AMException,
            SSOException {
        return getSubGroupContainerDNs(SCOPE_ONE).size();
    }

    /**
     * Deletes sub group containers in this group container.
     * 
     * @param groupContainers
     *            The set of container DN's to be deleted from the group
     *            container.
     * 
     * @throws AMException
     *             if there is an internal error in the AM Store
     * @throws SSOException
     *             if the sign on is no longer valid
     */
    public void deleteSubGroupContainers(Set groupContainers)
            throws AMException, SSOException {
        Iterator iter = groupContainers.iterator();
        while (iter.hasNext()) {
            String groupContainerDN = (String) iter.next();
            AMGroupContainer groupContainer = new AMGroupContainerImpl(token,
                    groupContainerDN);
            groupContainer.delete();
        }
    }

    /**
     * Searches for sub group containers in this group container using wildcards
     * and attribute values. Wildcards can be specified such as a*, *, *a. To
     * further refine the search, attribute-value pairs can be specifed so that
     * DNs of group containers with matching attribute-value pairs will be
     * returned.
     * 
     * @param wildcard
     *            wildcard pattern to be used in the search
     * @param avPairs
     *            attribute-value pairs to match when searching group Containers
     * @param level
     *            the search level that needs to be used (AMConstants.SCOPE_ONE
     *            or AMConstansts.SCOPE_SUB)
     * 
     * @return Set DNs of group containers matching the search
     * 
     * @throws AMException
     *             if there is an internal error in the AM Store
     * @throws SSOException
     *             if the sign on is no longer valid
     */
    public Set searchSubGroupContainers(String wildcard, Map avPairs, int level)
            throws AMException, SSOException {
        return searchObjects(
                AMNamingAttrManager.getNamingAttr(GROUP_CONTAINER),
                getSearchFilter(AMObject.GROUP_CONTAINER), wildcard, avPairs,
                level);
    }

    /**
     * Searches for group containers in this group container using wildcards and
     * attribute values. Wildcards can be specified such as a*, *, *a. To
     * further refine the search, attribute-value pairs can be specifed so that
     * DNs of group containers with matching attribute-value pairs will be
     * returned.
     * 
     * @param wildcard
     *            wildcard pattern to be used in the search
     * @param avPairs
     *            attribute-value pairs to match when searching users
     * @param searchControl
     *            specifies the search scope to be used, VLV ranges etc.,
     * 
     * @return AMSearchResults which contains a Set DNs of group containers
     *         matching the search
     * 
     * @throws AMException
     *             if there is an internal error in the AM Store
     * @throws SSOException
     *             if the sign on is no longer valid
     */
    public AMSearchResults searchSubGroupContainers(String wildcard,
            Map avPairs, AMSearchControl searchControl) throws AMException,
            SSOException {
        return searchObjects(
                AMNamingAttrManager.getNamingAttr(GROUP_CONTAINER),
                getSearchFilter(AMObject.GROUP_CONTAINER), wildcard, avPairs,
                searchControl);

    }

    /**
     * Creates static groups in this group container.
     * 
     * @param groupNames
     *            The set of static groups' names to be created in this group
     *            container.
     * @return Set set of static group objects created
     * @throws AMException
     *             if there is an internal error in the AM Store
     * @throws SSOException
     *             if the sign on is no longer valid
     */
    public Set createStaticGroups(Set groupNames) throws AMException,
            SSOException {
        Iterator iter = groupNames.iterator();
        Set groups = new HashSet();

        while (iter.hasNext()) {
            String groupDN = AMNamingAttrManager.getNamingAttr(GROUP) + "="
                    + ((String) iter.next()) + "," + entryDN;
            AMStaticGroupImpl groupImpl = new AMStaticGroupImpl(token, groupDN);
            groupImpl.create();
            groups.add(groupImpl);
        }

        return groups;
    }

    /**
     * Creates static groups and initializes their attributes.
     * 
     * @param users
     *            Map where the key is the name of the static group, and the
     *            value is a Map to represent Attribute-Value Pairs
     * 
     * @return Set Set of static group objects created.
     * @throws AMException
     *             if there is an internal error in the AM Store
     * @throws SSOException
     *             if the sign on is no longer valid
     */
    public Set createStaticGroups(Map groupsMap) throws AMException,
            SSOException {
        Iterator iter = groupsMap.keySet().iterator();
        Set groups = new HashSet();

        while (iter.hasNext()) {
            String groupName = (String) iter.next();
            String groupDN = AMNamingAttrManager.getNamingAttr(GROUP) + "="
                    + groupName + "," + entryDN;

            Map attributes = (Map) groupsMap.get(groupName);
            AMStaticGroupImpl groupImpl = new AMStaticGroupImpl(token, groupDN);
            groupImpl.setAttributes(attributes);
            groupImpl.create();
            groups.add(groupImpl);
        }

        return groups;
    }

    /**
     * Creates static group. Takes serviceNameAndAttr map so that services can
     * be assigned to the group which is just created.
     * 
     * @param Name
     *            of group to be created
     * @param ServiceName
     *            and attrsMap where the map is like this: 
     *            <serviceName><AttrMap>
     *            (attrMap=<attrName><Set of attrvalues>)
     * @return AMGroup object of newly created group.
     * @throws AMException
     * @throws SSOException
     *             if the SSOtoken is no longer valid
     */
    public AMGroup createStaticGroup(String name, Map attributes,
            Map serviceNameAndAttrs) throws AMException, SSOException {
        return createGroup(name, attributes, serviceNameAndAttrs,
                AMObject.STATIC_GROUP);
    }

    protected AMGroup createGroup(String name, Map attributes,
            Map serviceNameAndAttrs, int type) throws AMException, SSOException 
    {
        String groupDN = AMNamingAttrManager.getNamingAttr(GROUP) + "=" + name
                + "," + super.entryDN;
        AMObjectImpl groupImpl;
        switch (type) {
        case AMObject.STATIC_GROUP:
            groupImpl = new AMStaticGroupImpl(super.token, groupDN);
            break;
        case AMObject.DYNAMIC_GROUP:
            groupImpl = new AMDynamicGroupImpl(super.token, groupDN);
            break;
        case AMObject.ASSIGNABLE_DYNAMIC_GROUP:
            groupImpl = new AMAssignableDynamicGroupImpl(super.token, groupDN);
            break;
        default:
            throw new UnsupportedOperationException();
        }

        if (serviceNameAndAttrs != null && !serviceNameAndAttrs.isEmpty()) {
            Set serviceNames = serviceNameAndAttrs.keySet();
            Set registered = dsServices.getRegisteredServiceNames(null,
                    getOrganizationDN());
            Iterator it = serviceNames.iterator();

            while (it.hasNext()) {
                String tmpS = (String) it.next();
                if (!registered.contains(tmpS)) {
                    Object[] args = { tmpS };
                    throw new AMException(AMSDKBundle.getString("459", args,
                            super.locale), "459", args);
                }
            }

            Set objectClasses = null;
            if ((serviceNames != null) && (!serviceNames.isEmpty())) {
                objectClasses = AMServiceUtils.getServiceObjectClasses(token,
                        serviceNames);
                Set userOCs = (Set) attributes.get("objectclass");
                objectClasses = AMCommonUtils
                        .combineOCs(userOCs, objectClasses);
            }
            it = serviceNames.iterator();
            while (it.hasNext()) {
                String thisService = (String) it.next();
                Map sAttrMap = (Map) serviceNameAndAttrs.get(thisService);
                // validate the attributes and add pick the defaults.
                try {
                    ServiceSchemaManager ssm = new ServiceSchemaManager(
                            thisService, token);
                    ServiceSchema ss = ssm.getSchema(SchemaType.GROUP);
                    sAttrMap = ss.validateAndInheritDefaults(sAttrMap, true);
                    sAttrMap = AMCommonUtils.removeEmptyValues(sAttrMap);
                    groupImpl.setAttributes(sAttrMap);
                } catch (SMSException se) {
                    debug.error("AMGroupContainerImpl.createStaticGroup: "
                            + "Data validation failed.. ", se);
                    Object args[] = { thisService };
                    throw new AMException(AMSDKBundle.getString("976", args,
                            super.locale), "976", args);
                }
            }
            if (objectClasses != null && !objectClasses.isEmpty()) {
                groupImpl.setAttribute("objectclass", objectClasses);
            }
        }
        groupImpl.setAttributes(attributes);
        groupImpl.create();
        return ((AMGroup) groupImpl);

    }

    /**
     * Creates dynamic groups in this group container.
     * 
     * @param groupNames
     *            The set of dynamic groups' names to be created in this group
     *            container.
     * @return Set set of dynamic group objects created
     * @throws AMException
     *             if there is an internal error in the AM Store
     * @throws SSOException
     *             if the sign on is no longer valid
     */
    public Set createDynamicGroups(Set groupNames) throws AMException,
            SSOException {
        Iterator iter = groupNames.iterator();
        Set groups = new HashSet();

        while (iter.hasNext()) {
            String groupDN = AMNamingAttrManager.getNamingAttr(GROUP) + "="
                    + ((String) iter.next()) + "," + entryDN;
            AMDynamicGroupImpl groupImpl = new AMDynamicGroupImpl(token,
                    groupDN);
            groupImpl.create();
            groups.add(groupImpl);
        }

        return groups;
    }

    /**
     * Creates dynamic groups and initializes their attributes.
     * 
     * @param groups
     *            Map where the key is the name of the dynamic group, and the
     *            value is a Map to represent Attribute-Value Pairs
     * 
     * @return Set of dynamic group objects created
     * @throws AMException
     *             if there is an internal error in the AM Store
     * @throws SSOException
     *             if the sign on is no longer valid
     */
    public Set createDynamicGroups(Map groupsMap) throws AMException,
            SSOException {
        Iterator iter = groupsMap.keySet().iterator();
        Set groups = new HashSet();

        while (iter.hasNext()) {
            String groupName = (String) iter.next();
            String groupDN = AMNamingAttrManager.getNamingAttr(GROUP) + "="
                    + groupName + "," + entryDN;

            Map attributes = (Map) groupsMap.get(groupName);
            AMDynamicGroupImpl groupImpl = new AMDynamicGroupImpl(token,
                    groupDN);
            groupImpl.setAttributes(attributes);
            groupImpl.create();
            groups.add(groupImpl);
        }

        return groups;
    }

    /**
     * Creates dynamic group. Takes serviceNameAndAttr map so that services can
     * be assigned to the group which is just created.
     * 
     * @param Name
     *            of group to be created
     * @param ServiceName
     *            and attrsMap where the map is like this: 
     *            <serviceName><AttrMap>
     *            (attrMap=<attrName><Set of attrvalues>)
     * @return AMGroup object of newly created group.
     * @throws AMException
     * @throws SSOException
     *             if the SSOtoken is no longer valid
     */
    public AMGroup createDynamicGroup(String name, Map attributes,
            Map serviceNameAndAttrs) throws AMException, SSOException {
        return createGroup(name, attributes, serviceNameAndAttrs,
                AMObject.DYNAMIC_GROUP);
    }

    /**
     * Creates assignable dynamic groups in this group container.
     * 
     * @param groupNames
     *            The set of assignable dynamic groups' names to be created in
     *            this group container.
     * @return Set of assignable dynamic group objects created
     * @throws AMException
     *             if there is an internal error in the AM Store
     * @throws SSOException
     *             if the sign on is no longer valid
     */
    public Set createAssignableDynamicGroups(Set groupNames)
            throws AMException, SSOException {
        Iterator iter = groupNames.iterator();
        Set groups = new HashSet();

        while (iter.hasNext()) {
            String groupDN = AMNamingAttrManager.getNamingAttr(GROUP) + "="
                    + ((String) iter.next()) + "," + entryDN;
            AMAssignableDynamicGroupImpl groupImpl = 
                new AMAssignableDynamicGroupImpl(token, groupDN);
            groupImpl.create();
            groups.add(groupImpl);
        }

        return groups;
    }

    /**
     * Creates assignable dynamic groups and initializes their attributes.
     * 
     * @param groups
     *            Map where the key is the name of the assignable dynamic group,
     *            and the value is a Map to represent Attribute-Value Pairs
     * 
     * @return Set of assignable dynamic group objects created
     * @throws AMException
     *             if there is an internal error in the AM Store
     * @throws SSOException
     *             if the sign on is no longer valid
     */
    public Set createAssignableDynamicGroups(Map groupsMap) throws AMException,
            SSOException {
        Iterator iter = groupsMap.keySet().iterator();
        Set groups = new HashSet();

        while (iter.hasNext()) {
            String groupName = (String) iter.next();
            String groupDN = AMNamingAttrManager.getNamingAttr(GROUP) + "="
                    + groupName + "," + entryDN;

            Map attributes = (Map) groupsMap.get(groupName);
            AMAssignableDynamicGroupImpl groupImpl = 
                new AMAssignableDynamicGroupImpl(token, groupDN);
            groupImpl.setAttributes(attributes);
            groupImpl.create();
            groups.add(groupImpl);
        }

        return groups;
    }

    /**
     * Creates assignable dynamic group. Takes serviceNameAndAttr map so that
     * services can be assigned to the group which is just created.
     * 
     * @param Name
     *            of group to be created
     * @param ServiceName
     *            and attrsMap where the map is like this: 
     *            <serviceName><AttrMap>
     *            (attrMap=<attrName><Set of attrvalues>)
     * @return AMGroup object of newly created group.
     * @throws AMException
     *             if there is an error when accessing the data store
     * @throws SSOException
     *             if the SSOtoken is no longer valid
     */
    public AMGroup createAssignableDynamicGroup(String name, Map attributes,
            Map serviceNameAndAttrs) throws AMException, SSOException {
        return createGroup(name, attributes, serviceNameAndAttrs,
                AMObject.ASSIGNABLE_DYNAMIC_GROUP);
    }

    /**
     * Gets the groups in this group. It returns groups either at one level or a
     * whole subtree.
     * 
     * @param level
     *            the level(SCOPE_ONE or SCOPE_SUB) for returning groups
     * @return The group DNs in this group.
     * 
     * @throws AMException
     *             if there is an internal error in the AM Store or if if the
     *             level is invalid
     * @throws SSOException
     *             if the sign on is no longer valid
     */
    public Set getGroupDNs(int level) throws AMException, SSOException {
        return searchGroups("*", null, level);
    }

    /**
     * Gets number of groups in the group. It returns number of groups either at
     * one level or a whole subtree.
     * 
     * @param level
     *            the level(SCOPE_ONE or SCOPE_SUB) for returning groups
     * @return Number of groups in the group.
     * 
     * @throws AMException
     *             if there is an internal error in the AM Store
     * @throws SSOException
     *             if the sign on is no longer valid
     */
    public long getNumberOfGroups(int level) throws AMException, SSOException {
        return getGroupDNs(level).size();
    }

    /**
     * Deletes static groups in this group container.
     * 
     * @param groupDNs
     *            The set of static group DN's to be deleted from this group
     *            container.
     * @throws AMException
     *             if there is an internal error in the AM Store
     * @throws SSOException
     *             if the sign on is no longer valid
     */
    public void deleteStaticGroups(Set groupDNs) throws AMException,
            SSOException {
        Iterator iter = groupDNs.iterator();
        while (iter.hasNext()) {
            String groupDN = (String) iter.next();
            AMStaticGroup group = new AMStaticGroupImpl(token, groupDN);
            group.delete();
        }
    }

    /**
     * Deletes dynamic groups in this group container.
     * 
     * @param groups
     *            The set of dynamic group DN's to be deleted from this group
     *            container.
     * @throws AMException
     *             if there is an internal error in the AM Store
     * @throws SSOException
     *             if the sign on is no longer valid
     */
    public void deleteDynamicGroups(Set groupDNs) throws AMException,
            SSOException {
        Iterator iter = groupDNs.iterator();
        while (iter.hasNext()) {
            String groupDN = (String) iter.next();
            AMDynamicGroup group = new AMDynamicGroupImpl(token, groupDN);
            group.delete();
        }
    }

    /**
     * Deletes assignable dynamic groups in this group container.
     * 
     * @param groups
     *            The set of assignable dynamic group DN's to be deleted from
     *            this group container.
     * @throws AMException
     *             if there is an internal error in the AM Store
     * @throws SSOException
     *             if the sign on is no longer valid
     */
    public void deleteAssignableDynamicGroups(Set groupDNs) throws AMException,
            SSOException {
        Iterator iter = groupDNs.iterator();
        while (iter.hasNext()) {
            String groupDN = (String) iter.next();
            AMAssignableDynamicGroup group = new AMAssignableDynamicGroupImpl(
                    token, groupDN);
            group.delete();
        }
    }

    /**
     * Searches for groups in this group container using wildcards and attribute
     * values. Wildcards can be specified such as a*, *, *a.
     * 
     * @param wildcard
     *            wildcard pattern to be used in the search
     * @param avPairs
     *            attribute-value pairs to match when searching groups
     * @param level
     *            the search level that needs to be used (AMConstants.SCOPE_ONE
     *            or AMConstansts.SCOPE_SUB)
     * 
     * @return Set DNs of groups matching the search
     * 
     * @throws AMException
     *             if there is an internal error in the AM Store
     * @throws SSOException
     *             if the sign on is no longer valid
     */
    public Set searchGroups(String wildcard, Map avPairs, int level)
            throws AMException, SSOException {
        String filter = "(|" + getSearchFilter(AMObject.GROUP)
                + getSearchFilter(AMObject.DYNAMIC_GROUP)
                + getSearchFilter(AMObject.ASSIGNABLE_DYNAMIC_GROUP) + ")";

        return searchObjects(AMNamingAttrManager.getNamingAttr(GROUP), filter,
                wildcard, avPairs, level);
    }

    /**
     * Searches for groups in this group container using wildcards and attribute
     * values. Wildcards can be specified such as a*, *, *a.
     * 
     * @param wildcard
     *            wildcard pattern to be used in the search
     * @param avPairs
     *            attributevalue pairs to match when searching groups
     * @param searchControl
     *            specifies the search scope to be used
     * 
     * @return AMSearchResults which contains Set a of DNs of groups matching
     *         the search
     * 
     * @throws AMException
     *             if there is an internal error in the AM Store
     * @throws SSOException
     *             if the sign on is no longer valid
     */
    public AMSearchResults searchGroups(String wildcard, Map avPairs,
            AMSearchControl searchControl) throws AMException, SSOException {
        String filter = "(|" + getSearchFilter(AMObject.GROUP)
                + getSearchFilter(AMObject.DYNAMIC_GROUP)
                + getSearchFilter(AMObject.ASSIGNABLE_DYNAMIC_GROUP) + ")";

        return searchObjects(AMNamingAttrManager.getNamingAttr(GROUP), filter,
                wildcard, avPairs, searchControl);
    }

    /**
     * Searches for groups in this group container using wildcards. Wildcards
     * can be specified such as a*, *, *a. Uses the groupSearchTemplate, if
     * provided. Otherwise the default search template is used.
     * 
     * @param wildcard
     *            wildcard pattern to be used in the search
     * @param level
     *            the search level that needs to be used (AMConstants.SCOPE_ONE
     *            or AMConstants.SCOPE_SUB)
     * @param groupSearchTemplate
     *            name of the search template to be used to perform this search.
     * @param avPairs
     *            This option can be used to further qualify the search filter.
     *            The attribute-value pairs provided by this map are appended to
     *            the search filter.
     * @return Set of DNs of groups matching the search
     * 
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store
     * @throws SSOException
     *             if the sign on is no longer valid
     */

    public Set searchGroups(String wildcard, int level,
            String groupSearchTemplate, Map avPairs) throws AMException,
            SSOException {
        String filter;
        if (groupSearchTemplate != null && groupSearchTemplate.length() > 0) {
            filter = getSearchFilter(AMObject.GROUP, groupSearchTemplate);
        } else {
            filter = "(|" + getSearchFilter(AMObject.GROUP)
                    + getSearchFilter(AMObject.DYNAMIC_GROUP)
                    + getSearchFilter(AMObject.ASSIGNABLE_DYNAMIC_GROUP) + ")";
        }
        return searchObjects(AMNamingAttrManager.getNamingAttr(GROUP), filter,
                wildcard, avPairs, level);
    }

    public AMSearchResults searchGroups(String wildcard, Map avPairs,
            String groupSearchTemplate, AMSearchControl searchControl)
            throws AMException, SSOException {
        String filter;
        if (groupSearchTemplate != null && groupSearchTemplate.length() > 0) {
            filter = getSearchFilter(AMObject.GROUP, groupSearchTemplate);
        } else {
            filter = "(|" + getSearchFilter(AMObject.GROUP)
                    + getSearchFilter(AMObject.DYNAMIC_GROUP)
                    + getSearchFilter(AMObject.ASSIGNABLE_DYNAMIC_GROUP) + ")";
        }
        return searchObjects(AMNamingAttrManager.getNamingAttr(GROUP), filter,
                wildcard, avPairs, searchControl);
    }

    public AMSearchResults searchStaticGroups(String wildcard, Map avPairs,
            AMSearchControl searchControl) throws AMException, SSOException {
        String filter = getSearchFilter(AMObject.GROUP);

        return searchObjects(AMNamingAttrManager.getNamingAttr(GROUP), filter,
                wildcard, avPairs, searchControl);
    }

}
