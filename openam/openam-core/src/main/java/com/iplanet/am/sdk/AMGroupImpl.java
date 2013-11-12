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
 * $Id: AMGroupImpl.java,v 1.7 2009/11/20 23:52:51 ww203982 Exp $
 *
 */

/**
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.iplanet.am.sdk;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.sun.identity.shared.ldap.util.DN;
import com.sun.identity.shared.ldap.util.RDN;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.iplanet.ums.SearchControl;

/**
 * The <code>AMGroupImpl</code> class implements interface AMGroup
 * @deprecated  As of Sun Java System Access Manager 7.1.
 */
abstract class AMGroupImpl extends AMObjectImpl implements AMGroup {

    public AMGroupImpl(SSOToken ssoToken, String DN, int profileType) {
        super(ssoToken, DN, profileType);
    }

    /**
     * Gets number of users in the group.
     * 
     * @return Number of users in the group.
     */
    public long getNumberOfUsers() throws AMException, SSOException {
        return getUserDNs().size();
    }

    /**
     * Gets the user DN;s in the group.
     * 
     * @return The user DN's in the group.
     */
    public Set getUserDNs() throws AMException, SSOException {
        SSOTokenManager.getInstance().validateToken(super.token);
        Set members = dsServices.getMembers(super.token, super.entryDN,
                super.profileType);
        Set users = new HashSet();
        Iterator it = members.iterator();
        while (it.hasNext()) {
            String curr = (String) it.next();
            try {
                if (dsServices.getObjectType(token, curr) == AMObject.USER) {
                    users.add(curr);
                }
            } catch (AMException ame) {
                // member is not an identifiable managed type
                if (debug.messageEnabled()) {
                    debug.message("AMGroupImpl.getUserDNs: Unable to identify "
                            + " the type of object: " + curr);
                }
            }
        }
        return users;
    }

    /**
     * Searches for users in this group using wildcards and attribute values.
     * Wildcards can be specified such as a*, *, *a.
     * 
     * @param wildcard
     *            wildcard pattern to be used in the search
     * 
     * @return Set DNs of Users matching the search
     * 
     * @throws AMException
     *             if there is an internal error in the AM Store
     * @throws SSOException
     *             if the sign on is no longer valid
     */
    public Set searchUsers(String wildcard) throws AMException, SSOException {
        if ((wildcard == null) || (wildcard.length() == 0)) {
            throw new AMException(AMSDKBundle.getString("122", super.locale),
                    "122");
        }

        Set resultSet;
        Set usersSet = getUserDNs();

        if (wildcard.length() == 1) {
            if (wildcard.equals("*")) {
                resultSet = usersSet;
            } else {
                throw new AMException(AMSDKBundle
                        .getString("122", super.locale), "122");
            }
        } else {
            resultSet = new HashSet();

            if (wildcard.startsWith("*")) {
                String pattern = wildcard.substring(1);
                if (pattern.indexOf('*') != -1) {
                    throw new AMException(AMSDKBundle.getString("122",
                            super.locale), "122");
                }
                Iterator iter = usersSet.iterator();
                while (iter.hasNext()) {
                    DN userDN = new DN((String) iter.next());
                    RDN userRDN = (RDN) userDN.getRDNs().get(0);
                    String userName = userRDN.getValues()[0];
                    if (userName.endsWith(pattern)) {
                        resultSet.add(userDN.toString());
                    }
                }
            } else if (wildcard.endsWith("*")) {
                String pattern = wildcard.substring(0, wildcard.length() - 1);
                if (pattern.indexOf('*') != -1) {
                    throw new AMException(AMSDKBundle.getString("122",
                            super.locale), "122");
                }
                Iterator iter = usersSet.iterator();
                while (iter.hasNext()) {
                    DN userDN = new DN((String) iter.next());
                    RDN userRDN = (RDN) userDN.getRDNs().get(0);
                    String userName = userRDN.getValues()[0];
                    if (userName.startsWith(pattern)) {
                        resultSet.add(userDN.toString());
                    }
                }
            } else {
                throw new AMException(AMSDKBundle
                        .getString("122", super.locale), "122");
            }
        }

        return resultSet;
    }

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
     * 
     * @return AMSearchResults which contains a Set DNs of Users matching the
     *         search
     * 
     * @throws AMException
     *             if there is an internal error in the AM Store
     * @throws SSOException
     *             if the sign on is no longer valid
     */
    public AMSearchResults searchUsers(String wildcard, Map avPairs,
            AMSearchControl searchControl) throws AMException, SSOException {
        int scope;
        String base;
        String gfilter;

        if (profileType == DYNAMIC_GROUP
                || profileType == ASSIGNABLE_DYNAMIC_GROUP) {
            String[] array = dsServices.getGroupFilterAndScope(token, entryDN,
                    profileType);
            scope = Integer.parseInt(array[0]);
            base = array[1];
            gfilter = array[2];
        } else {
            scope = AMConstants.SCOPE_SUB;
            base = getOrganizationDN();
            gfilter = "(iplanet-am-static-group-dn=" + entryDN + ")";
        }

        String userFilter = "(&" + gfilter + "("
                + AMNamingAttrManager.getNamingAttr(USER) + "=" + wildcard
                + ")" + getSearchFilter(AMObject.USER) + ")";

        String filter = null;
        if (avPairs == null) {
            filter = userFilter;
        } else {
            if (avPairs.isEmpty()) {
                filter = userFilter;
            } else {
                StringBuilder filterSB = new StringBuilder();

                filterSB.append("(&").append(userFilter).append("(|");
                Iterator iter = avPairs.keySet().iterator();

                while (iter.hasNext()) {
                    String attributeName = (String) (iter.next());
                    Iterator iter2 = ((Set) (avPairs.get(attributeName)))
                            .iterator();
                    while (iter2.hasNext()) {
                        String attributeValue = (String) iter2.next();
                        filterSB.append("(").append(attributeName).append("=")
                                .append(attributeValue).append(")");
                    }
                }
                filterSB.append("))");
                filter = filterSB.toString();
            }
        }

        searchControl.setSearchScope(scope);
        SearchControl sc = searchControl.getSearchControl();
        String returnAttrs[] = searchControl.getReturnAttributes();
        return dsServices.search(super.token, base, filter, sc, returnAttrs);
    }

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
     * 
     * @return AMSearchResults which contains a Set DNs of Users matching the
     *         search
     * 
     * @throws AMException
     *             if there is an internal error in the AM Store
     * @throws SSOException
     *             if the sign on is no longer valid
     */
    public AMSearchResults searchUsers(AMSearchControl searchControl,
            String avfilter) throws AMException, SSOException {
        int scope;
        String base;
        String gfilter;

        if (profileType == DYNAMIC_GROUP
                || profileType == ASSIGNABLE_DYNAMIC_GROUP) {
            String[] array = dsServices.getGroupFilterAndScope(token, entryDN,
                    profileType);
            scope = Integer.parseInt(array[0]);
            base = array[1];
            gfilter = array[2];
        } else {
            scope = AMConstants.SCOPE_SUB;
            base = getOrganizationDN();
            gfilter = "(iplanet-am-static-group-dn=" + entryDN + ")";
        }

        String filter = "(&" + gfilter + getSearchFilter(AMObject.USER)
                + avfilter + ")";

        if (debug.messageEnabled()) {
            debug.message("AMGroupImpl.searchUsers: " + filter);
        }
        searchControl.setSearchScope(scope);
        SearchControl sc = searchControl.getSearchControl();
        String returnAttrs[] = searchControl.getReturnAttributes();
        return dsServices.search(super.token, base, filter, sc, returnAttrs);
    }

    /**
     * Creates static groups in this group.
     * 
     * @param groupNames
     *            The set of static groups' names to be created in this group.
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
                    + ((String) iter.next()) + "," + super.entryDN;
            AMStaticGroupImpl groupImpl = new AMStaticGroupImpl(super.token,
                    groupDN);
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
                    + groupName + "," + super.entryDN;

            Map attributes = (Map) groupsMap.get(groupName);
            AMStaticGroupImpl groupImpl = new AMStaticGroupImpl(super.token,
                    groupDN);
            groupImpl.setAttributes(attributes);
            groupImpl.create();
            groups.add(groupImpl);
        }

        return groups;
    }

    /**
     * Creates dynamic groups in this group.
     * 
     * @param groupNames
     *            The set of dynamic groups' names to be created in this group.
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
                    + ((String) iter.next()) + "," + super.entryDN;
            AMDynamicGroupImpl groupImpl = new AMDynamicGroupImpl(super.token,
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
                    + groupName + "," + super.entryDN;

            Map attributes = (Map) groupsMap.get(groupName);
            AMDynamicGroupImpl groupImpl = new AMDynamicGroupImpl(super.token,
                    groupDN);
            groupImpl.setAttributes(attributes);
            groupImpl.create();
            groups.add(groupImpl);
        }

        return groups;
    }

    /**
     * Creates assignable dynamic groups in this group.
     * 
     * @param groupNames
     *            The set of assignable dynamic groups' names to be created in
     *            this group.
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
                    + ((String) iter.next()) + "," + super.entryDN;
            AMAssignableDynamicGroupImpl groupImpl = 
                new AMAssignableDynamicGroupImpl(super.token, groupDN);
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
                    + groupName + "," + super.entryDN;

            Map attributes = (Map) groupsMap.get(groupName);
            AMAssignableDynamicGroupImpl groupImpl = 
                new AMAssignableDynamicGroupImpl(super.token, groupDN);
            groupImpl.setAttributes(attributes);
            groupImpl.create();
            groups.add(groupImpl);
        }

        return groups;
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
        return searchGroups("*", level);
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
        return searchGroups("*", level).size();
    }

    /**
     * Deletes static groups in this group.
     * 
     * @param groupDNs
     *            The set of static group DN's to be deleted from this group.
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
            AMStaticGroup group = new AMStaticGroupImpl(super.token, groupDN);
            group.delete();
        }
    }

    /**
     * Deletes dynamic groups in this group.
     * 
     * @param groups
     *            The set of dynamic group DN's to be deleted from this group.
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
            AMDynamicGroup group = new AMDynamicGroupImpl(super.token, groupDN);
            group.delete();
        }
    }

    /**
     * Deletes assignable dynamic groups in this group.
     * 
     * @param groups
     *            The set of assignable dynamic group DN's to be deleted from
     *            this group.
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
                    super.token, groupDN);
            group.delete();
        }
    }

    /**
     * Searches for groups in this group using wildcards. Wildcards can be
     * specified such as a*, *, *a.
     * 
     * @param wildcard
     *            wildcard pattern to be used in the search
     * @param level
     *            the search level that needs to be used (AMConstants.SCOPE_ONE
     *            or AMConstansts.SCOPE_SUB)
     * 
     * @return Set DNs of sub groups matching the search
     * 
     * @throws AMException
     *             if there is an internal error in the AM Store
     * @throws SSOException
     *             if the sign on is no longer valid
     */
    public Set searchGroups(String wildcard, int level) throws AMException,
            SSOException {
        return searchGroups(wildcard, null, level);
    }

    /**
     * Searches for groups in this group using wildcards and attribute values.
     * Wildcards can be specified such as a*, *, *a.
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
     * Searches for groups in this group using wildcards and attribute values.
     * Wildcards can be specified such as a*, *, *a.
     * 
     * @param wildcard
     *            wildcard pattern to be used in the search
     * @param avPairs
     *            attribute-value pairs to match when searching groups
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

    public void addNestedGroups(Set groups) throws AMException, SSOException {
        SSOTokenManager.getInstance().validateToken(super.token);
        Map attrMap = new AMHashMap();
        attrMap.put(UNIQUE_MEMBER_ATTRIBUTE, groups);
        try {
            dsServices.setAttributes(token, entryDN, super.profileType,
                    attrMap, null, true);
        } catch (AMException am) {
            if (am.getErrorCode().equals("452")) {
                // Generic "unable to set attributes" exception
                debug.error("AMGroupImpl.addNestedGroups: Unable to " 
                        + "add groups: -> ", am);
                throw new AMException(AMSDKBundle
                        .getString("771", super.locale), "771");
            } else {
                throw am;
            }
        }
    }

    public Set getNestedGroupDNs() throws AMException, SSOException {
        Set attrNames = new HashSet();
        attrNames.add(UNIQUE_MEMBER_ATTRIBUTE);
        Map attrMap = dsServices.getAttributes(token, entryDN, attrNames,
                profileType);
        Set members = (Set) attrMap.get(UNIQUE_MEMBER_ATTRIBUTE);
        Set groups = new HashSet();
        Iterator it = members.iterator();
        while (it.hasNext()) {
            String curr = (String) it.next();
            try {
                if (dsServices.getObjectType(token, curr) != AMObject.USER) {
                    groups.add(curr);
                }
            } catch (AMException ame) {
                // member is not an identifiable managed type
                debug.error("AMGroupImpl.getNestedGroupDNs: Unable to "
                        + "identity the type of object: " + curr);
            }
        }
        return groups;
    }

    public void removeNestedGroups(Set groups) throws AMException, SSOException 
    {
        SSOTokenManager.getInstance().validateToken(super.token);
        Set attrNames = new HashSet();
        attrNames.add(UNIQUE_MEMBER_ATTRIBUTE);
        Map attrMap = dsServices.getAttributes(token, entryDN, attrNames,
                false, false, super.profileType);
        Set attrVals = (Set) attrMap.get(UNIQUE_MEMBER_ATTRIBUTE);
        attrVals.removeAll(groups);
        if (debug.messageEnabled()) {
            debug.message("AMGroupImpl.removeNestedGroups: Setting nested " 
                    + "groups to: " + attrVals.toString());
        }
        attrMap.put(UNIQUE_MEMBER_ATTRIBUTE, attrVals);
        try {
            dsServices.setAttributes(token, entryDN, super.profileType,
                    attrMap, null, false);
        } catch (AMException am) {
            if (am.getErrorCode().equals("452")) {
                // Genere "unable to set attributes" exception
                debug.error("AMGroupImpl.removeNestedGroups: Unable to " + 
                        "remove groups: -> ", am);
                throw new AMException(AMSDKBundle
                        .getString("772", super.locale), "772");
            } else {
                throw am;
            }
        }
    }

    public Set getUserAndGroupDNs() throws AMException, SSOException {
        // return users of group as defined (based on either filter
        // or uniquemember attribute
        Set users = getUserDNs();
        // return nested groups as set in the "uniquemember" attribute.
        Set groups = getNestedGroupDNs();
        groups.addAll(users);
        return groups;
    }

}
