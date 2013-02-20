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
 * $Id: AMPeopleContainerImpl.java,v 1.5 2008/06/25 05:41:21 qcheng Exp $
 *
 */

/**
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.iplanet.am.sdk;

import java.util.Collections;
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
 * The <code>AMPeopleContainerImpl</code> class implements interface
 * AMPeopleContainer
 *
 * @deprecated  As of Sun Java System Access Manager 7.1.
 */
class AMPeopleContainerImpl extends AMObjectImpl implements AMPeopleContainer {
    public AMPeopleContainerImpl(SSOToken ssoToken, String DN) {
        super(ssoToken, DN, PEOPLE_CONTAINER);
    }

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
     *             if there is an internal error in the AM Store
     * @throws SSOException
     *             if the sign on is no longer valid
     */
    public Set createUsers(Set userNames) throws AMException, SSOException {
        Set usersSet = new HashSet();
        String parentOrgDN = getOrganizationDN();
        AMOrganizationImpl parentOrg = new AMOrganizationImpl(super.token,
                parentOrgDN);
        Set serviceNames = parentOrg.getOrgTypeAttributes(
                ADMINISTRATION_SERVICE, REQUIRED_SERVICES_ATTR);
        Set objectClasses = null;
        if ((serviceNames != null) && (!serviceNames.isEmpty())) {
            objectClasses = AMServiceUtils.getServiceObjectClasses(token,
                    serviceNames);
        }

        Iterator iter = userNames.iterator();
        while (iter.hasNext()) {
            StringBuilder userDNSB = new StringBuilder();
            userDNSB.append(AMNamingAttrManager.getNamingAttr(USER))
                    .append("=").append((String) iter.next()).append(",")
                    .append(super.entryDN);
            AMUserImpl user = new AMUserImpl(super.token, userDNSB.toString());
            if (objectClasses != null && !objectClasses.isEmpty()) {
                user.setAttribute("objectclass", objectClasses);
            }
            user.create();
            usersSet.add(user);
        }
        return usersSet;
    }

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
     *             if there is an internal error in the AM Store
     * @throws SSOException
     *             if the sign on is no longer valid
     */
    public Set createUsers(Map usersMap) throws AMException, SSOException {
        String parentOrgDN = getOrganizationDN();
        AMOrganizationImpl parentOrg = new AMOrganizationImpl(super.token,
                parentOrgDN);
        Set serviceNames = parentOrg.getOrgTypeAttributes(
                ADMINISTRATION_SERVICE, REQUIRED_SERVICES_ATTR);
        return createUsers(usersMap, serviceNames);
    }

    /**
     * Creates users and initializes their attributes.For each user the, object
     * classes specified by organization type attribute
     * <code>iplanet-am-required-services</code> of the service
     * <code>iPlanetAMAdminConsoleService</code> template are added. If a
     * corresponding template does not exist, the default values are picked up
     * from schema.
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
     *             if there is an internal error in the AM Store
     * @throws SSOException
     *             if the sign on is no longer valid
     */
    public Set createUsers(Map usersMap, Set serviceNames) throws AMException,
            SSOException {
        Set usersSet = new HashSet();

        Set objectClasses = null;
        if ((serviceNames != null) && (!serviceNames.isEmpty())) {
            objectClasses = AMServiceUtils.getServiceObjectClasses(token,
                    serviceNames);
        }

        Iterator iter = usersMap.keySet().iterator();
        while (iter.hasNext()) {
            String userName = (String) iter.next();
            String userDN = AMNamingAttrManager.getNamingAttr(USER) + "="
                    + userName + "," + entryDN;
            AMUserImpl user = new AMUserImpl(super.token, userDN);
            Map userMap = (Map) usersMap.get(userName);
            user.setAttributes(userMap);
            if (objectClasses != null && objectClasses.size() > 0) {
                Set existingOC = (Set) userMap.get("objectclass");
                if (existingOC != null && !existingOC.isEmpty())
                    objectClasses = AMCommonUtils.combineOCs(objectClasses,
                            existingOC);
                user.setAttribute("objectclass", objectClasses);
            }
            user.create();
            usersSet.add(user);
        }
        return usersSet;
    }

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
     * @param String
     *            uid, value of naming attribute for user.
     * @param Map
     *            attrMap attribute-values to be set in the user entry.
     * @param Map
     *            serviceNameAndAttr service names and attributes to be assigned
     *            to the user.
     * @return AMUser object of newly created user.
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store
     * @throws SSOException
     *             if the sign on is no longer valid
     */

    public AMUser createUser(String uid, Map attrMap, Map serviceNameAndAttrs)
            throws AMException, SSOException {
        String parentOrgDN = getOrganizationDN();
        AMOrganizationImpl parentOrg = new AMOrganizationImpl(super.token,
                parentOrgDN);
        Set serviceNames = parentOrg.getOrgTypeAttributes(
                ADMINISTRATION_SERVICE, REQUIRED_SERVICES_ATTR);
        if (serviceNames == Collections.EMPTY_SET) {
            serviceNames = new HashSet();
        }
        Set assignServiceNames = serviceNameAndAttrs.keySet();
        Set registered = null;
        registered = dsServices.getRegisteredServiceNames(null,
                getOrganizationDN());
        Iterator it = assignServiceNames.iterator();

        while (it.hasNext()) {
            String tmpS = (String) it.next();
            if (!registered.contains(tmpS)) {
                Object[] args = { tmpS };
                throw new AMException(AMSDKBundle.getString("459", args,
                        super.locale), "459", args);
            }
        }

        it = assignServiceNames.iterator();
        while (it.hasNext()) {
            String tmp = (String) it.next();
            if (!serviceNames.contains(tmp)) {
                serviceNames.add(tmp);
            }
        }
        Set objectClasses = null;
        if ((serviceNames != null) && (!serviceNames.isEmpty())) {
            objectClasses = AMServiceUtils.getServiceObjectClasses(token,
                    serviceNames);
            Set userOCs = (Set) attrMap.get("objectclass");
            objectClasses = AMCommonUtils.combineOCs(userOCs, objectClasses);
        }

        String userDN = AMNamingAttrManager.getNamingAttr(USER) + "=" + uid
                + "," + super.entryDN;
        AMUserImpl user = new AMUserImpl(super.token, userDN);
        user.setAttributes(attrMap);
        it = assignServiceNames.iterator();
        while (it.hasNext()) {
            String thisService = (String) it.next();
            Map sAttrMap = (Map) serviceNameAndAttrs.get(thisService);
            // validate the attributes and add pick the defaults.
            try {
                ServiceSchemaManager ssm = new ServiceSchemaManager(
                        thisService, token);
                ServiceSchema ss = ssm.getSchema(SchemaType.USER);
                sAttrMap = ss.validateAndInheritDefaults(sAttrMap, true);
                sAttrMap = AMCommonUtils.removeEmptyValues(sAttrMap);
                user.setAttributes(sAttrMap);
            } catch (SMSException se) {
                debug.error("AMPeopleContainerImpl: data validation failed-> "
                        + thisService, se);
                Object args[] = { thisService };
                throw new AMException(AMSDKBundle.getString("976", args,
                        super.locale), "976", args);
            }
        }
        if (objectClasses != null && !objectClasses.isEmpty()) {
            user.setAttribute("objectclass", objectClasses);
        }
        user.create();
        return (user);
    }

    /**
     * Removes users from the people container.
     * 
     * @param users
     *            The set of user DN's to be removed from the people container.
     */
    public void deleteUsers(Set users) throws AMException, SSOException {
        Iterator iter = users.iterator();
        while (iter.hasNext()) {
            String userDN = (String) iter.next();
            AMUser user = new AMUserImpl(super.token, userDN);
            user.delete();
        }
    }

    /**
     * Gets number of users in the people container.
     * 
     * @return Number of users in the people container.
     */
    public long getNumberOfUsers() throws AMException, SSOException {
        return getUserDNs().size();
    }

    /**
     * Gets the names (DNs) of users in the people container.
     * 
     * @return Set The names(DNs) of users in the people container.
     * 
     * @throws AMException
     *             if there is an internal error in the AM Store
     * @throws SSOException
     *             if the sign on is no longer valid
     */
    public Set getUserDNs() throws AMException, SSOException {
        return search(SCOPE_ONE, getSearchFilter(AMObject.USER));
    }

    /**
     * Creates sub PeopleContainers in this people container.
     * 
     * @param peopleContainers
     *            The set of peopleContainer names to be created in this people
     *            container.
     * @return Set set of PeopleContainer objects created
     * @throws AMException
     *             if there is an internal error in the AM Store
     * @throws SSOException
     *             if the sign on is no longer valid
     */
    public Set createSubPeopleContainers(Set peopleContainerNames)
            throws AMException, SSOException {
        Iterator iter = peopleContainerNames.iterator();
        Set peopleContainers = new HashSet();

        while (iter.hasNext()) {
            StringBuffer peopleContainerDNSB = new StringBuffer();
            peopleContainerDNSB.append(
                    AMNamingAttrManager.getNamingAttr(PEOPLE_CONTAINER))
                    .append("=").append((String) iter.next()).append(",")
                    .append(super.entryDN);
            AMPeopleContainerImpl peopleContainerImpl = 
                new AMPeopleContainerImpl(super.token, 
                        peopleContainerDNSB.toString());
            peopleContainerImpl.create();
            peopleContainers.add(peopleContainerImpl);
        }

        return peopleContainers;
    }

    /**
     * Creates sub people containers and initializes their attributes.
     * 
     * @param users
     *            Map where the key is the name of the people container, and the
     *            value is a Map to represent Attribute-Value Pairs
     * 
     * @param Set
     *            Set of PeopleContainer objects created.
     * @throws AMException
     *             if there is an internal error in the AM Store
     * @throws SSOException
     *             if the sign on is no longer valid
     */
    public Set createSubPeopleContainers(Map peopleContainersMap)
            throws AMException, SSOException {
        Iterator iter = peopleContainersMap.keySet().iterator();
        Set peopleContainers = new HashSet();

        while (iter.hasNext()) {
            String peopleContainerName = (String) iter.next();
            StringBuffer peopleContainerDNSB = new StringBuffer();
            peopleContainerDNSB.append(
                    AMNamingAttrManager.getNamingAttr(PEOPLE_CONTAINER))
                    .append("=").append(peopleContainerName).append(",")
                    .append(super.entryDN);
            Map attributes = (Map) peopleContainersMap.get(peopleContainerName);
            AMPeopleContainerImpl peopleContainerImpl = 
                new AMPeopleContainerImpl(super.token, 
                        peopleContainerDNSB.toString());
            peopleContainerImpl.setAttributes(attributes);
            peopleContainerImpl.create();
            peopleContainers.add(peopleContainerImpl);
        }

        return peopleContainers;
    }

    /**
     * Gets the sub containers in this people container. It returns sub
     * containers either at one level or a whole subtree.
     * 
     * @param level
     *            the level(SCOPE_ONE or SCOPE_SUB) for returning subcontainers
     * @return The sub container DNs in this people container.
     * 
     * @throws AMException
     *             if there is an internal error in the AM Store or if if the
     *             level is invalid
     * @throws SSOException
     *             if the sign on is no longer valid
     */
    public Set getSubPeopleContainerDNs(int level) throws AMException,
            SSOException {
        return search(level, getSearchFilter(AMObject.PEOPLE_CONTAINER));
    }

    /**
     * Gets number of sub peoople containers in the people container.
     * 
     * @return Number of sub people containers in the people container.
     * 
     * @throws AMException
     *             if there is an internal error in the AM Store
     * @throws SSOException
     *             if the sign on is no longer valid
     */
    public long getNumberOfSubPeopleContainers() throws AMException,
            SSOException {
        return getSubPeopleContainerDNs(SCOPE_ONE).size();
    }

    /**
     * Deletes sub people containers in this people container.
     * 
     * @param peopleContainers
     *            The set of user DN's to be deleted from the people container.
     * 
     * @throws AMException
     *             if there is an internal error in the AM Store
     * @throws SSOException
     *             if the sign on is no longer valid
     */
    public void deleteSubPeopleContainers(Set peopleContainers)
            throws AMException, SSOException {
        Iterator iter = peopleContainers.iterator();
        while (iter.hasNext()) {
            String peopleContainerDN = (String) iter.next();
            AMPeopleContainer peopleContainer = new AMPeopleContainerImpl(
                    super.token, peopleContainerDN);
            peopleContainer.delete();
        }
    }

    /**
     * Searches for users in this people container using wildcards and attribute
     * values. Wildcards can be specified such as a*, *, *a.
     * 
     * @param wildcard
     *            wildcard pattern to be used in the search
     * @param level
     *            the search level that needs to be used (AMConstants.SCOPE_ONE
     *            or AMConstansts.SCOPE_SUB)
     * 
     * @return Set DNs of Users matching the search
     * 
     * @throws AMException
     *             if there is an internal error in the AM Store
     * @throws SSOException
     *             if the sign on is no longer valid
     */
    public Set searchUsers(String wildcard, int level) throws AMException,
            SSOException {
        return searchUsers(wildcard, null, level);
    }

    /**
     * Searches for users in this people container using wildcards and attribute
     * values. Wildcards can be specified such as a*, *, *a.
     * 
     * @param wildcard
     *            wildcard pattern to be used in the search
     * @param searchControl
     *            specifies the search scope to be used, VLV ranges etc.,
     * 
     * @return AMSearchResults which contains a Set DNs of Users matching the
     *         search
     * 
     * @throws AMException
     *             if there is an internal error in the AM Store
     * @throws SSOException
     *             if the sign on is no longer valid
     */
    public AMSearchResults searchUsers(String wildcard,
            AMSearchControl searchControl) throws AMException, SSOException {
        return searchUsers(wildcard, null, searchControl);
    }

    /**
     * Searches for users in this group using wildcards and attribute values.
     * Wildcards can be specified such as a*, *, *a. To further refine the
     * search, attribute-value pairs can be specifed so that DNs of users with
     * matching attribute-value pairs will be returned.
     * 
     * @param wildcard
     *            wildcard pattern to be used in the search
     * @param avPairs
     *            attribute-value pairs to match when searching users
     * @param level
     *            the search level that needs to be used (AMConstants.SCOPE_ONE
     *            or AMConstansts.SCOPE_SUB)
     * 
     * @return Set DNs of Users matching the search
     * 
     * @throws AMException
     *             if there is an internal error in the AM Store
     * @throws SSOException
     *             if the sign on is no longer valid
     */
    public Set searchUsers(String wildcard, Map avPairs, int level)
            throws AMException, SSOException {
        return searchObjects(AMNamingAttrManager.getNamingAttr(USER),
                getSearchFilter(AMObject.USER), wildcard, avPairs, level);
    }

    /**
     * Searches for users in this group using wildcards and attribute values.
     * Wildcards can be specified such as a*, *, *a. To further refine the
     * search, attribute-value pairs can be specifed so that DNs of users with
     * matching attribute-value pairs will be returned.
     * 
     * @param wildcard
     *            wildcard pattern to be used in the search
     * @param avPairs
     *            attribute-value pairs to match when searching users
     * @param searchControl
     *            specifies the search scope to be used, VLV ranges etc.,
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
        return searchObjects(AMNamingAttrManager.getNamingAttr(USER),
                getSearchFilter(AMObject.USER), wildcard, avPairs,
                searchControl);
    }

    /**
     * Searches for users in this people container using wildcards and attribute
     * values. Wildcards can be specified such as a*, *, *a. To further refine
     * the search, attribute-value pairs can be specifed so that DNs of users
     * with matching attribute-value pairs will be returned.
     * 
     * @param wildcard
     *            wildcard pattern to be used in the search
     * @param searchControl
     *            specifies the search scope to be used, VLV ranges etc.,
     * @param avfilter
     *            this attribute-value pairs filter will be & with user search
     *            filter
     * 
     * @return AMSearchResults which contains a Set DNs of Users matching the
     *         search
     * 
     * @throws AMException
     *             if there is an internal error in the AM Store
     * @throws SSOException
     *             if the sign on is no longer valid
     */
    public AMSearchResults searchUsers(String wildcard,
            AMSearchControl searchControl, String avfilter) throws AMException,
            SSOException {
        return searchObjects(AMNamingAttrManager.getNamingAttr(USER),
                getSearchFilter(AMObject.USER), wildcard, searchControl,
                avfilter);
    }

    /**
     * Searches for users in this people container using wildcards and attribute
     * values. Wildcards can be specified such as a*, *, *a. To further refine
     * the search, attribute-value pairs can be specifed so that DNs of users
     * with matching attribute-value pairs will be returned.
     * 
     * @param wildcard
     *            wildcard pattern to be used in the search
     * @param searchControl
     *            specifies the search scope to be used, VLV ranges etc.,
     * @param avfilter
     *            this attribute-value pairs filter will be & with user search
     *            filter
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
        return searchObjects(getSearchFilter(AMObject.USER), searchControl,
                avfilter);
    }

    public Set searchUsers(String wildcard, int level,
            String userSearchTemplate, Map avPairs) throws AMException,
            SSOException {
        if (AMCompliance.isComplianceUserDeletionEnabled()
                && AMCompliance.isAncestorOrgDeleted(super.token,
                        super.entryDN, AMObject.PEOPLE_CONTAINER)) {
            if (debug.warningEnabled()) {
                debug.warning("AMOrganization.searchUser: "
                        + "ancestor org is deleted for: " + super.entryDN
                        + " :returning empty set");
            }
            return Collections.EMPTY_SET;
        }
        return searchObjects(AMNamingAttrManager.getNamingAttr(USER),
                getSearchFilter(AMObject.USER, userSearchTemplate), wildcard,
                avPairs, level);
    }

    public Set createResources(Set resourceNames) throws AMException,
            SSOException {
        Set resSet = new HashSet();
        Iterator iter = resourceNames.iterator();
        while (iter.hasNext()) {
            String userDN = AMNamingAttrManager.getNamingAttr(RESOURCE) + "="
                    + ((String) iter.next()) + "," + super.entryDN;
            AMResourceImpl user = new AMResourceImpl(super.token, userDN);
            user.create();
            resSet.add(user);
        }

        return resSet;
    }

    public Set searchResources(String wildcard, int level,
            String resourceSearchTemplate, Map avPairs) throws AMException,
            SSOException {
        return searchObjects(AMNamingAttrManager.getNamingAttr(RESOURCE),
                getSearchFilter(AMObject.RESOURCE, resourceSearchTemplate),
                wildcard, avPairs, level);

    }

    public void deleteResources(Set resources) throws AMException, SSOException 
    {
        Iterator iter = resources.iterator();
        while (iter.hasNext()) {
            String rDN = (String) iter.next();
            AMResource resource = new AMResourceImpl(super.token, rDN);
            resource.delete();
        }
    }

    /**
     * Searches for sub people containers in this people container using
     * wildcards and attribute values. Wildcards can be specified such as a*, *,
     * *a.
     * 
     * @param wildcard
     *            wildcard pattern to be used in the search
     * @param level
     *            the search level that needs to be used (AMConstants.SCOPE_ONE
     *            or AMConstansts.SCOPE_SUB)
     * 
     * @return Set DNs of people containers matching the search
     * 
     * @throws AMException
     *             if there is an internal error in the AM Store
     * @throws SSOException
     *             if the sign on is no longer valid
     */
    public Set searchSubPeopleContainers(String wildcard, int level)
            throws AMException, SSOException {
        return searchSubPeopleContainers(wildcard, null, level);
    }

    /**
     * Searches for sub people containers in this people container using
     * wildcards and attribute values. Wildcards can be specified such as a*, *,
     * *a. To further refine the search, attribute-value pairs can be specifed
     * so that DNs of people containers with matching attribute-value pairs will
     * be returned.
     * 
     * @param wildcard
     *            wildcard pattern to be used in the search
     * @param avPairs
     *            attribute-value pairs to match when searching people
     *            Containers
     * @param level
     *            the search level that needs to be used (AMConstants.SCOPE_ONE
     *            or AMConstansts.SCOPE_SUB)
     * 
     * @return Set DNs of people containers matching the search
     * 
     * @throws AMException
     *             if there is an internal error in the AM Store
     * @throws SSOException
     *             if the sign on is no longer valid
     */
    public Set searchSubPeopleContainers(String wildcard, Map avPairs, 
            int level) throws AMException, SSOException {
        return searchObjects(AMNamingAttrManager
                .getNamingAttr(PEOPLE_CONTAINER),
                getSearchFilter(AMObject.PEOPLE_CONTAINER), wildcard, avPairs,
                level);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.iplanet.am.sdk.AMOrganization#searchUsers(java.lang.String,
     *      java.util.Map, java.lang.String, com.iplanet.am.sdk.AMSearchControl)
     */
    public AMSearchResults searchUsers(String wildcard, Map avPairs,
            String userSearchTemplate, AMSearchControl searchControl)
            throws AMException, SSOException {
        return searchObjects(AMNamingAttrManager.getNamingAttr(USER),
                getSearchFilter(AMObject.USER, userSearchTemplate), wildcard,
                avPairs, searchControl);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.iplanet.am.sdk.AMPeopleContainer#createResources(java.util.Map)
     */
    public Set createResources(Map resourceMap) throws AMException,
            SSOException {
        if (resourceMap == null) {
            return Collections.EMPTY_SET;
        }
        Set resSet = new HashSet();
        Iterator iter = resourceMap.keySet().iterator();
        while (iter.hasNext()) {
            String rDN = ((String) iter.next());
            String userDN = AMNamingAttrManager.getNamingAttr(RESOURCE) + "="
                    + rDN + "," + super.entryDN;
            AMResourceImpl user = new AMResourceImpl(super.token, userDN);
            Map attrMap = (Map) resourceMap.get(rDN);
            user.setAttributes(attrMap);
            user.create();
            resSet.add(user);
        }

        return resSet;
    }

    public AMSearchResults searchResources(String wildcard, Map avPairs,
            String rSearchTemplate, AMSearchControl searchControl)
            throws AMException, SSOException {
        return searchObjects(AMNamingAttrManager.getNamingAttr(RESOURCE),
                getSearchFilter(AMObject.RESOURCE, rSearchTemplate), wildcard,
                avPairs, searchControl);
    }

    public Set createEntities(String stype, Set entities) throws AMException,
            SSOException {
        Set resultSet = new HashSet();
        if (stype.equalsIgnoreCase("user")) {
            // create users.
            Set uSet = createUsers(entities);
            Iterator it = uSet.iterator();
            while (it.hasNext()) {
                AMUser u = (AMUser) it.next();
                AMEntity e = new AMEntityImpl(token, u.getDN());
                resultSet.add(e);
            }
            return resultSet;
        }
        String type = (String) AMCommonUtils.supportedTypes.get(stype
                .toLowerCase());
        if (type == null) {
            throw new AMException(AMSDKBundle.getString("117", super.locale),
                    "117");
        }

        int createType = Integer.parseInt(type);
        Iterator it = entities.iterator();
        while (it.hasNext()) {
            ;
            String rDN = ((String) it.next());
            String userDN = AMNamingAttrManager.getNamingAttr(createType) + "="
                    + rDN + "," + super.entryDN;
            AMEntityImpl user = new AMEntityImpl(super.token, userDN);
            user.create(stype);
            resultSet.add(user);
        }
        return resultSet;
    }

    public void deleteEntities(Set resources) throws AMException, SSOException {
        Iterator iter = resources.iterator();
        while (iter.hasNext()) {
            String rDN = (String) iter.next();
            AMEntity resource = new AMEntityImpl(super.token, rDN);
            resource.delete();
        }
    }

    public Set searchEntities(String wildcard, int level,
            String eSearchTemplate, Map avPairs) throws AMException,
            SSOException {
        if (eSearchTemplate == null) {
            eSearchTemplate = "BasicEntitySearch";
        }
        return searchObjects(AMNamingAttrManager.getNamingAttr(USER),
                getSearchFilter(AMObject.USER, eSearchTemplate), wildcard,
                avPairs, level);

    }

    public AMSearchResults searchEntities(String wildcard, Map avPairs,
            String eSearchTemplate, AMSearchControl searchControl)
            throws AMException, SSOException {
        if (eSearchTemplate == null) {
            eSearchTemplate = "BasicEntitySearch";
        }
        return searchObjects(AMNamingAttrManager.getNamingAttr(USER),
                getSearchFilter(AMObject.USER, eSearchTemplate), wildcard,
                avPairs, searchControl);
    }

    public Set createEntities(String stype, Map entities) throws AMException,
            SSOException {
        if (stype.equalsIgnoreCase("user")) {
            // create users.
            Set uSet = createUsers(entities);
            Set resultSet = new HashSet();
            Iterator it = uSet.iterator();
            while (it.hasNext()) {
                AMUser u = (AMUser) it.next();
                AMEntity e = new AMEntityImpl(token, u.getDN());
                resultSet.add(e);
            }
            return resultSet;
        }
        String type = (String) AMCommonUtils.supportedTypes.get(stype
                .toLowerCase());
        if (type == null) {
            throw new AMException(AMSDKBundle.getString("117", super.locale),
                    "117");
        }
        int createType = Integer.parseInt(type);
        Set entitySet = new HashSet();
        Iterator iter = entities.keySet().iterator();
        while (iter.hasNext()) {
            String userName = (String) iter.next();
            String userDN = AMNamingAttrManager.getNamingAttr(createType) + "="
                    + userName + "," + super.entryDN;
            AMEntityImpl user = new AMEntityImpl(super.token, userDN);
            Map userMap = (Map) entities.get(userName);
            user.setAttributes(userMap);
            user.create(stype);
            entitySet.add(user);
        }
        return entitySet;
    }

    public AMSearchResults searchEntities(String wildcard,
            AMSearchControl searchControl, String avfilter,
            String eSearchTemplate) throws AMException, SSOException {
        if (eSearchTemplate == null) {
            eSearchTemplate = "BasicEntitySearch";
        }
        return searchObjects(AMNamingAttrManager.getNamingAttr(USER),
                getSearchFilter(AMObject.USER, eSearchTemplate), wildcard,
                searchControl, avfilter);
    }

}
