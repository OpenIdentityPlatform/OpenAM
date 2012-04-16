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
 * $Id: AMOrganizationalUnitImpl.java,v 1.5 2008/06/25 05:41:21 qcheng Exp $
 *
 */

package com.iplanet.am.sdk;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;

import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.SchemaType;
import com.sun.identity.sm.ServiceConfig;

/**
 * The <code>AMOrganizationalUnitImpl</code> class implements interface
 * AMOrganizationalUnit
 *
 * @deprecated  As of Sun Java System Access Manager 7.1.
 */

class AMOrganizationalUnitImpl extends AMObjectImpl implements
        AMOrganizationalUnit {
    public AMOrganizationalUnitImpl(SSOToken ssoToken, String DN) {
        super(ssoToken, DN, ORGANIZATIONAL_UNIT);
    }

    /**
     * Creates organizations.
     * 
     * @param organizations
     *            The set of organizations names to be created.
     * @return Set set of Organization objects created.
     * 
     * @throws AMException
     *             if there is an internal error in the AM Store
     * @throws SSOException
     *             if the sign on is no longer valid
     */
    public Set createOrganizations(Set organizations) throws AMException,
            SSOException {
        Iterator iter = organizations.iterator();
        Set orgs = new HashSet();

        while (iter.hasNext()) {
            String orgDN = AMNamingAttrManager.getNamingAttr(ORGANIZATION)
                    + "=" + ((String) iter.next()) + "," + super.entryDN;

            AMOrganizationImpl orgImpl = new AMOrganizationImpl(super.token,
                    orgDN);

            orgImpl.create();
            orgs.add(orgImpl);
        }

        return orgs;
    }

    /**
     * Creates organizations and initializes their attributes.
     * 
     * @param organizationsMap
     *            Map where the key is the name of the organization, and the
     *            value is a Map to represent Attribute-Value Pairs
     * @return Set set of Organization objects created.
     * @throws AMException
     *             if there is an internal error in the AM Store
     * @throws SSOException
     *             if the sign on is no longer valid
     */
    public Set createOrganizations(Map organizationsMap) throws AMException,
            SSOException {
        Iterator iter = organizationsMap.keySet().iterator();
        Set orgs = new HashSet();

        while (iter.hasNext()) {
            String orgName = (String) iter.next();
            String orgDN = AMNamingAttrManager.getNamingAttr(ORGANIZATION)
                    + "=" + orgName + "," + super.entryDN;

            Map attributes = (Map) organizationsMap.get(orgName);
            AMOrganizationImpl orgImpl = new AMOrganizationImpl(super.token,
                    orgDN);
            orgImpl.setAttributes(attributes);
            orgImpl.create();
            orgs.add(orgImpl);
        }

        return orgs;
    }

    /**
     * Deletes organizations
     * 
     * @param organizationDNs
     *            The set of organizations DNs to be deleted.
     * 
     * @throws AMException
     *             if there is an internal error in the AM Store
     * @throws SSOException
     *             if the sign on is no longer valid
     */
    public void deleteOrganizations(Set organizationDNs) throws AMException,
            SSOException {
        Iterator iter = organizationDNs.iterator();

        while (iter.hasNext()) {
            String organizationDN = (String) iter.next();
            AMOrganization org = new AMOrganizationImpl(super.token,
                    organizationDN);
            org.delete();
        }
    }

    /**
     * Gets the organization by DN
     * 
     * @param dn
     *            DN
     * @return The Organization object
     */
    public AMOrganization getOrganization(String dn) throws AMException,
            SSOException {
        SSOTokenManager.getInstance().validateToken(super.token);
        return new AMOrganizationImpl(super.token, dn);
    }

    /**
     * Gets the organizations within the specified level.
     * 
     * @param level
     *            The search level starting from the organizational unit.
     * @return The Set of organizations DNs within the specified level.
     */
    public Set getOrganizations(int level) throws AMException, SSOException {
        return search(level, getSearchFilter(AMObject.ORGANIZATION));
    }

    /**
     * Gets number of organizations within the specified level.
     * 
     * @param level
     *            The search level starting from the organizational unit.
     * @return Number of organizations within the specified level.
     */
    public long getNumberOfOrganizations(int level) throws AMException,
            SSOException {
        return getOrganizations(level).size();
    }

    /**
     * Searches for organizations in this organizational unit using wildcards.
     * Wildcards can be specified such as a*, *, *a.
     * 
     * @param wildcard
     *            wildcard pattern to be used in the search
     * @param level
     *            the search level that needs to be used (AMConstants.SCOPE_ONE
     *            or AMConstansts.SCOPE_SUB)
     * 
     * @return Set Set of DNs of organizations matching the search
     * 
     * @throws AMException
     *             if there is an internal error in the AM Store
     * @throws SSOException
     *             if the sign on is no longer valid
     */
    public Set searchOrganizations(String wildcard, int level)
            throws AMException, SSOException {
        return searchOrganizations(wildcard, null, level);
    }

    /**
     * Searches for organizations in this organizational unit using wildcards.
     * Wildcards can be specified such as a*, *, *a.
     * 
     * @param wildcard
     *            wildcard pattern to be used in the search
     * @param searchControl
     *            specifies the search scope to be used, VLV ranges etc.,
     * 
     * @return AMSearchResults which contains a Set of DNs of organizations
     *         matching the search
     * 
     * @throws AMException
     *             if there is an internal error in the AM Store
     * @throws SSOException
     *             if the sign on is no longer valid
     */
    public AMSearchResults searchOrganizations(String wildcard,
            AMSearchControl searchControl) throws AMException, SSOException {
        return searchOrganizations(wildcard, null, searchControl);
    }

    /**
     * Searches for organizations in this organizational unit using wildcards
     * and attribute values. Wildcards can be specified such as a*, *, *a. To
     * further refine the search, attribute-value pairs can be specifed so that
     * DNs of organizations with matching attribute-value pairs will be
     * returned.
     * 
     * @param wildcard
     *            wildcard pattern to be used in the search
     * @param avPairs
     *            attribute-value pairs to match when searching organizations
     * @param level
     *            the search level that needs to be used (AMConstants.SCOPE_ONE
     *            or AMConstansts.SCOPE_SUB)
     * 
     * @return Set Set of DNs of organizations matching the search
     * 
     * @throws AMException
     *             if there is an internal error in the AM Store
     * @throws SSOException
     *             if the sign on is no longer valid
     */
    public Set searchOrganizations(String wildcard, Map avPairs, int level)
            throws AMException, SSOException {
        return searchObjects(AMNamingAttrManager.getNamingAttr(ORGANIZATION),
                getSearchFilter(AMObject.ORGANIZATION), wildcard, avPairs,
                level);
    }

    /**
     * Searches for organizations in this organizational unit using wildcards
     * and attribute values. Wildcards can be specified such as a*, *, *a. To
     * further refine the search, attribute-value pairs can be specifed so that
     * DNs of organizations with matching attribute-value pairs will be
     * returned.
     * 
     * @param wildcard
     *            wildcard pattern to be used in the search
     * @param avPairs
     *            attribute-value pairs to match when searching organizations
     * @param searchControl
     *            specifies the search scope to be used, VLV ranges etc.,
     * 
     * @return AMSearchResults which contains a Set of DNs of organizations
     *         matching the search
     * 
     * @throws AMException
     *             if there is an internal error in the AM Store
     * @throws SSOException
     *             if the sign on is no longer valid
     */
    public AMSearchResults searchOrganizations(String wildcard, Map avPairs,
            AMSearchControl searchControl) throws AMException, SSOException {
        return searchObjects(AMNamingAttrManager.getNamingAttr(ORGANIZATION),
                getSearchFilter(AMObject.ORGANIZATION), wildcard, avPairs,
                searchControl);
    }

    /**
     * Creates sub-organizational units.
     * 
     * @param subOrganizationalUnits
     *            The set of sub-organizational units names to be created.
     * @return Set set of sub OrganizationalUnit objects created.
     * 
     * @throws AMException
     *             if there is an internal error in the AM Store
     * @throws SSOException
     *             if the sign on is no longer valid
     */
    public Set createSubOrganizationalUnits(Set subOrganizationalUnits)
            throws AMException, SSOException {
        Iterator iter = subOrganizationalUnits.iterator();
        Set subOrgUnits = new HashSet();

        while (iter.hasNext()) {
            String subOrgUnitDN = AMNamingAttrManager
                    .getNamingAttr(ORGANIZATIONAL_UNIT)
                    + "=" + ((String) iter.next()) + "," + super.entryDN;

            AMOrganizationalUnitImpl subOrgUnitImpl = 
                new AMOrganizationalUnitImpl(super.token, subOrgUnitDN);

            subOrgUnitImpl.create();
            subOrgUnits.add(subOrgUnitImpl);
        }

        return subOrgUnits;
    }

    /**
     * Creates sub-organizational units and initializes their attributes.
     * 
     * @param subOrganizationalUnitsMap
     *            Map where the key is the name of the suborganizational unit,
     *            and the value is a Map to represent Attribute-Value Pairs
     * @return Set set of sub OrganizationalUnit objects created.
     * @throws AMException
     *             if there is an internal error in the AM Store
     * @throws SSOException
     *             if the sign on is no longer valid
     */
    public Set createSubOrganizationalUnits(Map subOrganizationalUnitsMap)
            throws AMException, SSOException {
        Iterator iter = subOrganizationalUnitsMap.keySet().iterator();
        Set subOrgUnits = new HashSet();

        while (iter.hasNext()) {
            String subOrgUnitName = (String) iter.next();
            String subOrgUnitDN = AMNamingAttrManager
                    .getNamingAttr(ORGANIZATIONAL_UNIT)
                    + "=" + subOrgUnitName + "," + super.entryDN;

            Map attributes = (Map) subOrganizationalUnitsMap
                    .get(subOrgUnitName);
            AMOrganizationalUnitImpl subOrgUnitImpl = 
                new AMOrganizationalUnitImpl(super.token, subOrgUnitDN);
            subOrgUnitImpl.setAttributes(attributes);
            subOrgUnitImpl.create();
            subOrgUnits.add(subOrgUnitImpl);
        }

        return subOrgUnits;
    }

    /**
     * Deletes suborganizational units
     * 
     * @param subOrganizationalUnitDNs
     *            The set of suborganizational units DNs to be deleted.
     * 
     * @throws AMException
     *             if there is an internal error in the AM Store
     * @throws SSOException
     *             if the sign on is no longer valid
     */
    public void deleteSubOrganizationalUnits(Set subOrganizationalUnitDNs)
            throws AMException, SSOException {
        Iterator iter = subOrganizationalUnitDNs.iterator();

        while (iter.hasNext()) {
            String subOrganizationalUnitDN = (String) iter.next();
            AMOrganizationalUnit subOrgUnit = new AMOrganizationalUnitImpl(
                    super.token, subOrganizationalUnitDN);
            subOrgUnit.delete();
        }
    }

    /**
     * Gets the sub-organizational unit by DN
     * 
     * @param dn
     *            DN
     * @return The sub OrganizationalUnit object
     */
    public AMOrganizationalUnit getSubOrganizationalUnit(String dn)
            throws AMException, SSOException {
        SSOTokenManager.getInstance().validateToken(super.token);
        return new AMOrganizationalUnitImpl(super.token, dn);
    }

    /**
     * Gets the suborganizational units within the specified level.
     * 
     * @param level
     *            The search level starting from the organizational unit.
     * @return The Set of suborganizational units DNs within the specified
     *         level.
     */
    public Set getSubOrganizationalUnits(int level) throws AMException,
            SSOException {
        return search(level, getSearchFilter(AMObject.ORGANIZATIONAL_UNIT));
    }

    /**
     * Gets number of suborganizational units within the specified level.
     * 
     * @param level
     *            The search level starting from the organizational unit.
     * @return Number of suborganizational units within the specified level.
     */
    public long getNumberOfSubOrganizationalUnits(int level)
            throws AMException, SSOException {
        return getSubOrganizationalUnits(level).size();
    }

    /**
     * Searches for sub organizational units in this organizational unit using
     * wildcards. Wildcards can be specified such as a*, *, *a.
     * 
     * @param wildcard
     *            wildcard pattern to be used in the search
     * @param level
     *            the search level that needs to be used (AMConstants.SCOPE_ONE
     *            or AMConstansts.SCOPE_SUB)
     * 
     * @return Set Set of DNs of sub organizational units matching the search
     * 
     * @throws AMException
     *             if there is an internal error in the AM Store
     * @throws SSOException
     *             if the sign on is no longer valid
     */
    public Set searchSubOrganizationalUnits(String wildcard, int level)
            throws AMException, SSOException {
        return searchSubOrganizationalUnits(wildcard, null, level);
    }

    /**
     * Searches for sub organizational units in this organizational unit using
     * wildcards. Wildcards can be specified such as a*, *, *a.
     * 
     * @param wildcard
     *            wildcard pattern to be used in the search
     * @param searchControl
     *            specifies the search scope to be used, VLV ranges etc.,
     * 
     * @return AMSearchResults which contains a Set of DNs of sub organizational
     *         units matching the search
     * 
     * @throws AMException
     *             if there is an internal error in the AM Store
     * @throws SSOException
     *             if the sign on is no longer valid
     */
    public AMSearchResults searchSubOrganizationalUnits(String wildcard,
            AMSearchControl searchControl) throws AMException, SSOException {
        return searchSubOrganizationalUnits(wildcard, null, searchControl);
    }

    /**
     * Searches for sub organizational units in this organizational unit using
     * wildcards and attribute values. Wildcards can be specified such as a*, *,
     * *a. To further refine the search, attribute-value pairs can be specifed
     * so that DNs of sub organizations with matching attribute-value pairs will
     * be returned.
     * 
     * @param wildcard
     *            wildcard pattern to be used in the search
     * @param avPairs
     *            attribute-value pairs to match when searching sub
     *            organizational units
     * @param level
     *            the search level that needs to be used (AMConstants.SCOPE_ONE
     *            or AMConstansts.SCOPE_SUB)
     * 
     * @return Set Set of DNs of sub organizational units matching the search
     * 
     * @throws AMException
     *             if there is an internal error in the AM Store
     * @throws SSOException
     *             if the sign on is no longer valid
     */
    public Set searchSubOrganizationalUnits(String wildcard, Map avPairs,
            int level) throws AMException, SSOException {
        return searchObjects(AMNamingAttrManager
                .getNamingAttr(ORGANIZATIONAL_UNIT),
                getSearchFilter(AMObject.ORGANIZATIONAL_UNIT), wildcard,
                avPairs, level);
    }

    /**
     * Searches for sub organizational units in this organizational unit using
     * wildcards and attribute values. Wildcards can be specified such as a*, *,
     * *a. To further refine the search, attribute-value pairs can be specifed
     * so that DNs of sub organizations with matching attribute-value pairs will
     * be returned.
     * 
     * @param wildcard
     *            wildcard pattern to be used in the search
     * @param avPairs
     *            attribute-value pairs to match when searching sub
     *            organizational units
     * @param searchControl
     *            specifies the search scope to be used, VLV ranges etc.,
     * 
     * @return AMSearchResults which contains a Set of DNs of sub organizational
     *         units matching the search
     * 
     * @throws AMException
     *             if there is an internal error in the AM Store
     * @throws SSOException
     *             if the sign on is no longer valid
     */
    public AMSearchResults searchSubOrganizationalUnits(String wildcard,
            Map avPairs, AMSearchControl searchControl) throws AMException,
            SSOException {
        return searchObjects(AMNamingAttrManager
                .getNamingAttr(ORGANIZATIONAL_UNIT),
                getSearchFilter(AMObject.ORGANIZATIONAL_UNIT), wildcard,
                avPairs, searchControl);
    }

    /**
     * Creates roles.
     * 
     * @param roles
     *            The set of Roles' names to be created.
     * @return Set set of Role objects created.
     * @throws AMException
     *             if there is an internal error in the AM Store
     * @throws SSOException
     *             if the sign on is no longer valid
     */
    public Set createRoles(Set roleNames) throws AMException, SSOException {
        Iterator iter = roleNames.iterator();
        Set roles = new HashSet();

        while (iter.hasNext()) {
            String roleDN = AMNamingAttrManager.getNamingAttr(ROLE) + "="
                    + ((String) iter.next()) + "," + super.entryDN;
            AMRoleImpl roleImpl = new AMRoleImpl(super.token, roleDN);
            roleImpl.create();
            roles.add(roleImpl);
        }

        return roles;
    }

    /**
     * Creates roles.
     * 
     * @param roles
     *            Map where the key is the name of the role, and the value is a
     *            Map to represent Attribute-Value Pairs
     * @return Set set of Role objects created.
     * @throws AMException
     *             if there is an internal error in the AM Store
     * @throws SSOException
     *             if the sign on is no longer valid
     */
    public Set createRoles(Map roles) throws AMException, SSOException {
        Iterator iter = roles.keySet().iterator();
        Set result = new HashSet();

        while (iter.hasNext()) {
            String roleName = (String) iter.next();
            String roleDN = AMNamingAttrManager.getNamingAttr(ROLE) + "="
                    + roleName + "," + super.entryDN;

            Map attributes = (Map) roles.get(roleName);
            AMRoleImpl roleImpl = new AMRoleImpl(super.token, roleDN);

            Set aciSet = null;
            if (!attributes.isEmpty()) {
                aciSet = (Set) attributes.remove("iplanet-am-role-aci-list");
            }
            if (aciSet != null) {
                Iterator iter2 = aciSet.iterator();
                Set newAciSet = new HashSet();
                while (iter2.hasNext()) {
                    String acis = (String) iter2.next();
                    StringTokenizer stz = new StringTokenizer(acis, "##");
                    while (stz.hasMoreTokens()) {
                        newAciSet.add(stz.nextToken());
                    }
                }

                attributes.put("iplanet-am-role-aci-list",
                        replaceAciListMacros(newAciSet, roleDN, entryDN, null,
                                null));
            }

            roleImpl.setAttributes(attributes);
            roleImpl.create();
            result.add(roleImpl);
        }

        return result;
    }

    /**
     * Deletes roles.
     * 
     * @param roles
     *            The set of roles' DNs to be deleted.
     * @throws AMException
     *             if there is an internal error in the AM Store
     * @throws SSOException
     *             if the sign on is no longer valid
     */
    public void deleteRoles(Set roleDNs) throws AMException, SSOException {
        Iterator iter = roleDNs.iterator();

        while (iter.hasNext()) {
            String roleDN = (String) iter.next();
            AMRole role = new AMRoleImpl(super.token, roleDN);
            role.delete();
        }
    }

    /**
     * Gets the roles within the specified level.
     * 
     * @param level
     *            The search level starting from the organizational unit.
     * @return The Set of role DN's within the specified level.
     */
    public Set getRoles(int level) throws AMException, SSOException {
        return search(level, getSearchFilter(AMObject.ROLE));
    }

    /**
     * Gets number of roles within the specified level.
     * 
     * @param level
     *            The search level starting from the organizational unit.
     * @return Number of roles within the specified level.
     */
    public long getNumberOfRoles(int level) throws AMException, SSOException {
        return getRoles(level).size();
    }

    /**
     * Searches for roles in this organizational unit using wildcards. Wildcards
     * can be specified such as a*, *, *a.
     * 
     * @param wildcard
     *            wildcard pattern to be used in the search
     * @param level
     *            the search level that needs to be used (AMConstants.SCOPE_ONE
     *            or AMConstansts.SCOPE_SUB)
     * 
     * @return Set Set of DNs of roles matching the search
     * 
     * @throws AMException
     *             if there is an internal error in the AM Store
     * @throws SSOException
     *             if the sign on is no longer valid
     */
    public Set searchRoles(String wildcard, int level) throws AMException,
            SSOException {
        return searchRoles(wildcard, null, level);
    }

    /**
     * Searches for roles in this organizational unit using wildcards. Wildcards
     * can be specified such as a*, *, *a.
     * 
     * @param wildcard
     *            wildcard pattern to be used in the search
     * @param searchControl
     *            specifies the search scope to be used, VLV ranges etc.,
     * 
     * @return AMSearchResults which contains a Set of DNs of roles matching the
     *         search
     * 
     * @throws AMException
     *             if there is an internal error in the AM Store
     * @throws SSOException
     *             if the sign on is no longer valid
     */
    public AMSearchResults searchRoles(String wildcard,
            AMSearchControl searchControl) throws AMException, SSOException {
        return searchRoles(wildcard, null, searchControl);
    }

    /**
     * Searches for roles in this organizational unit using wildcards and
     * attribute values. Wildcards can be specified such as a*, *, *a. To
     * further refine the search, attribute-value pairs can be specifed so that
     * DNs of roles with matching attribute-value pairs will be returned.
     * 
     * @param wildcard
     *            wildcard pattern to be used in the search
     * @param avPairs
     *            attribute-value pairs to match when searching users
     * @param level
     *            the search level that needs to be used (AMConstants.SCOPE_ONE
     *            or AMConstansts.SCOPE_SUB)
     * 
     * @return Set Set of DNs of roles matching the search
     * 
     * @throws AMException
     *             if there is an internal error in the AM Store
     * @throws SSOException
     *             if the sign on is no longer valid
     */
    public Set searchRoles(String wildcard, Map avPairs, int level)
            throws AMException, SSOException {
        return searchObjects(AMNamingAttrManager.getNamingAttr(ROLE),
                getSearchFilter(AMObject.ROLE), wildcard, avPairs, level);
    }

    /**
     * Searches for roles in this organizational unit using wildcards and
     * attribute values. Wildcards can be specified such as a*, *, *a. To
     * further refine the search, attribute-value pairs can be specifed so that
     * DNs of roles with matching attribute-value pairs will be returned.
     * 
     * @param wildcard
     *            wildcard pattern to be used in the search
     * @param avPairs
     *            attribute-value pairs to match when searching users
     * @param searchControl
     *            specifies the search scope to be used, VLV ranges etc.,
     * 
     * @return AMSearchResults which contains a Set of DNs of roles matching the
     *         search
     * 
     * @throws AMException
     *             if there is an internal error in the AM Store
     * @throws SSOException
     *             if the sign on is no longer valid
     */
    public AMSearchResults searchRoles(String wildcard, Map avPairs,
            AMSearchControl searchControl) throws AMException, SSOException {
        return searchObjects(AMNamingAttrManager.getNamingAttr(ROLE),
                getSearchFilter(AMObject.ROLE), wildcard, avPairs,
                searchControl);
    }

    /**
     * Creates filtered roles.
     * 
     * @param roles
     *            The set of filtered roles' names to be created.
     * @return Set set of FilteredRole objects created.
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store
     * @throws SSOException
     *             if the sign on is no longer valid
     */
    public Set createFilteredRoles(Set roleNames) throws AMException,
            SSOException {
        Iterator iter = roleNames.iterator();
        Set roles = new HashSet();

        while (iter.hasNext()) {
            String roleName = (String) iter.next();
            String roleDN = AMNamingAttrManager.getNamingAttr(FILTERED_ROLE)
                    + "=" + roleName + "," + entryDN;
            AMFilteredRoleImpl roleImpl = new AMFilteredRoleImpl(token, roleDN);
            roleImpl.create();
            roles.add(roleImpl);
        }

        return roles;
    }

    /**
     * Creates filtered roles.
     * 
     * @param roles
     *            Map where the key is the name of the filtered role, and the
     *            value is a Map to represent Attribute-Value Pairs
     * @return Set set of FilteredRole objects created.
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store
     * @throws SSOException
     *             if the sign on is no longer valid
     */
    public Set createFilteredRoles(Map roles) throws AMException, SSOException {
        Iterator iter = roles.keySet().iterator();
        Set result = new HashSet();

        while (iter.hasNext()) {
            String roleName = (String) iter.next();
            String roleDN = AMNamingAttrManager.getNamingAttr(FILTERED_ROLE)
                    + "=" + roleName + "," + entryDN;

            Map attributes = (Map) roles.get(roleName);
            AMFilteredRoleImpl roleImpl = new AMFilteredRoleImpl(token, roleDN);

            if (!attributes.isEmpty()) {
                Set aciSet = (Set) attributes
                        .remove("iplanet-am-role-aci-list");
                if ((aciSet != null) && (!aciSet.isEmpty())) {
                    Iterator iter2 = aciSet.iterator();
                    Set newAciSet = new HashSet();
                    while (iter2.hasNext()) {
                        String acis = (String) iter2.next();
                        StringTokenizer stz = new StringTokenizer(acis, "##");
                        while (stz.hasMoreTokens()) {
                            newAciSet.add(stz.nextToken());
                        }
                    }

                    attributes.put("iplanet-am-role-aci-list",
                            replaceAciListMacros(newAciSet, roleDN, entryDN,
                                    null, null));
                }
            }

            roleImpl.setAttributes(attributes);
            roleImpl.create();
            result.add(roleImpl);
        }

        return result;
    }

    /**
     * Deletes filtered roles.
     * 
     * @param roles
     *            The set of filtered roles' DNs to be deleted.
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store
     * @throws SSOException
     *             if the sign on is no longer valid
     */
    public void deleteFilteredRoles(Set roleDNs) throws AMException,
            SSOException {
        Iterator iter = roleDNs.iterator();

        while (iter.hasNext()) {
            String roleDN = (String) iter.next();
            AMFilteredRole role = new AMFilteredRoleImpl(token, roleDN);
            role.delete();
        }
    }

    /**
     * Gets the filtered roles within the specified level.
     * 
     * @param level
     *            The search level starting from the organizational unit.
     * @return The Set of filtered roles' DNs within the specified level.
     */
    public Set getFilteredRoles(int level) throws AMException, SSOException {
        return search(level, getSearchFilter(AMObject.FILTERED_ROLE));
    }

    /**
     * Gets number of filtered roles within the specified level.
     * 
     * @param level
     *            The search level starting from the organizational unit.
     * @return Number of filtered roles within the specified level.
     */
    public long getNumberOfFilteredRoles(int level) throws AMException,
            SSOException {
        return getFilteredRoles(level).size();
    }

    /**
     * Searches for filtered roles in this organizational unit using wildcards.
     * Wildcards can be specified such as a*, *, *a.
     * 
     * @param wildcard
     *            wildcard pattern to be used in the search
     * @param level
     *            the search level that needs to be used (AMConstants.SCOPE_ONE
     *            or AMConstants.SCOPE_SUB)
     * 
     * @return Set Set of DNs of filtered roles matching the search
     * 
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store
     * @throws SSOException
     *             if the sign on is no longer valid
     */
    public Set searchFilteredRoles(String wildcard, int level)
            throws AMException, SSOException {
        return searchFilteredRoles(wildcard, null, level);
    }

    /**
     * Searches for filtered roles in this organizational unit using wildcards.
     * Wildcards can be specified such as a*, *, *a.
     * 
     * @param wildcard
     *            wildcard pattern to be used in the search
     * @param searchControl
     *            specifies the search scope to be used, VLV ranges etc.,
     * 
     * @return AMSearchResults which contains a Set of DNs of filtered roles
     *         matching the search
     * 
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store
     * @throws SSOException
     *             if the sign on is no longer valid
     */
    public AMSearchResults searchFilteredRoles(String wildcard,
            AMSearchControl searchControl) throws AMException, SSOException {
        return searchFilteredRoles(wildcard, null, searchControl);
    }

    /**
     * Searches for filtered roles in this organizational unit using wildcards
     * and attribute values. Wildcards can be specified such as a*, *, *a. To
     * further refine the search, attribute-value pairs can be specifed so that
     * DNs of filtered roles with matching attribute-value pairs will be
     * returned.
     * 
     * @param wildcard
     *            wildcard pattern to be used in the search
     * @param avPairs
     *            attribute-value pairs to match when searching filtered roles
     * @param level
     *            the search level that needs to be used (AMConstants.SCOPE_ONE
     *            or AMConstants.SCOPE_SUB)
     * 
     * @return Set Set of DNs of filtered roles matching the search
     * 
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store
     * @throws SSOException
     *             if the sign on is no longer valid
     */
    public Set searchFilteredRoles(String wildcard, Map avPairs, int level)
            throws AMException, SSOException {
        return searchObjects(AMNamingAttrManager.getNamingAttr(FILTERED_ROLE),
                getSearchFilter(AMObject.FILTERED_ROLE), wildcard, avPairs,
                level);
    }

    /**
     * Searches for filtered roles in this organizational unit using wildcards
     * and attribute values. Wildcards can be specified such as a*, *, *a. To
     * further refine the search, attribute-value pairs can be specifed so that
     * DNs of filtered roles with matching attribute-value pairs will be
     * returned.
     * 
     * @param wildcard
     *            wildcard pattern to be used in the search
     * @param avPairs
     *            attribute-value pairs to match when searching filtered roles
     * @param searchControl
     *            specifies the search scope to be used, VLV ranges etc.,
     * 
     * @return AMSearchResults which contains a Set of DNs of filtered roles
     *         matching the search
     * 
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store
     * @throws SSOException
     *             if the sign on is no longer valid
     */
    public AMSearchResults searchFilteredRoles(String wildcard, Map avPairs,
            AMSearchControl searchControl) throws AMException, SSOException {
        return searchObjects(AMNamingAttrManager.getNamingAttr(FILTERED_ROLE),
                getSearchFilter(AMObject.FILTERED_ROLE), wildcard, avPairs,
                searchControl);
    }

    /**
     * Searches for all roles in this organizational unit using wildcards.
     * Wildcards can be specified such as a*, *, *a.
     * 
     * @param wildcard
     *            wildcard pattern to be used in the search
     * @param level
     *            the search level that needs to be used (AMConstants.SCOPE_ONE
     *            or AMConstants.SCOPE_SUB)
     * 
     * @return Set of DNs of all roles matching the search
     * 
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store
     * @throws SSOException
     *             if the sign on is no longer valid
     */
    public Set searchAllRoles(String wildcard, int level) throws AMException,
            SSOException {
        return searchAllRoles(wildcard, null, level);
    }

    /**
     * Searches for all roles in this organizational unit using wildcards.
     * Wildcards can be specified such as a*, *, *a.
     * 
     * @param wildcard
     *            wildcard pattern to be used in the search
     * @param searchControl
     *            specifies the search scope to be used, VLV ranges etc.,
     * 
     * @return AMSearchResults which contains a Set of DNs of all roles matching
     *         the search
     * 
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store
     * @throws SSOException
     *             if the sign on is no longer valid
     */
    public AMSearchResults searchAllRoles(String wildcard,
            AMSearchControl searchControl) throws AMException, SSOException {
        return searchAllRoles(wildcard, null, searchControl);
    }

    /**
     * Searches for all roles in this organizational unit using wildcards and
     * attribute values. Wildcards can be specified such as a*, *, *a. To
     * further refine the search, attribute-value pairs can be specifed so that
     * DNs of all roles with matching attribute-value pairs will be returned.
     * 
     * @param wildcard
     *            wildcard pattern to be used in the search
     * @param avPairs
     *            attribute-value pairs to match when searching all roles
     * @param level
     *            the search level that needs to be used (AMConstants.SCOPE_ONE
     *            or AMConstants.SCOPE_SUB)
     * 
     * @return Set of DNs of all roles matching the search
     * 
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store
     * @throws SSOException
     *             if the sign on is no longer valid
     */
    public Set searchAllRoles(String wildcard, Map avPairs, int level)
            throws AMException, SSOException {
        int[] objectTypes = { AMObject.ROLE, AMObject.FILTERED_ROLE };

        return searchObjects(objectTypes, wildcard, avPairs, level);
    }

    /**
     * Searches for all roles in this organizational unit using wildcards and
     * attribute values. Wildcards can be specified such as a*, *, *a. To
     * further refine the search, attribute-value pairs can be specifed so that
     * DNs of all roles with matching attribute-value pairs will be returned.
     * 
     * @param wildcard
     *            wildcard pattern to be used in the search
     * @param avPairs
     *            attribute-value pairs to match when searching all roles
     * @param searchControl
     *            specifies the search scope to be used, VLV ranges etc.,
     * 
     * @return AMSearchResults which contains a Set of DNs of all roles matching
     *         the search
     * 
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store
     * @throws SSOException
     *             if the sign on is no longer valid
     */
    public AMSearchResults searchAllRoles(String wildcard, Map avPairs,
            AMSearchControl searchControl) throws AMException, SSOException {
        int[] objectTypes = { AMObject.ROLE, AMObject.FILTERED_ROLE };

        return searchObjects(objectTypes, wildcard, avPairs, searchControl);
    }

    /**
     * Creates assignable dynamic groups.
     * 
     * @param assignableDynamicGroupNames
     *            The set of assignable dynamic groups's names to be created.
     * @return Set set of AssignableDynamicGroup objects created.
     * @throws AMException
     *             if there is an internal error in the AM Store
     * @throws SSOException
     *             if the sign on is no longer valid
     */
    public Set createAssignableDynamicGroups(Set assignableDynamicGroupNames)
            throws AMException, SSOException {
        Iterator iter = assignableDynamicGroupNames.iterator();
        Set assignableDynamicGroups = new HashSet();

        while (iter.hasNext()) {
            String assignableDynamicGroupDN = AMNamingAttrManager
                    .getNamingAttr(GROUP)
                    + "=" + ((String) iter.next()) + "," + super.entryDN;
            AMAssignableDynamicGroupImpl assignableDynamicGroupImpl = 
                new AMAssignableDynamicGroupImpl(super.token, 
                        assignableDynamicGroupDN);
            assignableDynamicGroupImpl.create();
            assignableDynamicGroups.add(assignableDynamicGroupImpl);
        }

        return assignableDynamicGroups;
    }

    /**
     * Deletes assignable dynamic groups.
     * 
     * @param assignableDynamicGroupDNs
     *            The set of assignable dynamic groups's DNs to be deleted.
     * 
     * @throws AMException
     *             if there is an internal error in the AM Store
     * @throws SSOException
     *             if the sign on is no longer valid
     */
    public void deleteAssignableDynamicGroups(Set assignableDynamicGroupDNs)
            throws AMException, SSOException {
        Iterator iter = assignableDynamicGroupDNs.iterator();

        while (iter.hasNext()) {
            String assignableDynamicGroupDN = (String) iter.next();
            AMAssignableDynamicGroup assignableDynamicGroup = 
                new AMAssignableDynamicGroupImpl(super.token, 
                        assignableDynamicGroupDN);
            assignableDynamicGroup.delete();
        }
    }

    /**
     * Gets the assignable dynamic groups within the specified level.
     * 
     * @param level
     *            The search level starting from the organizational unit.
     * @return The Set of DNs of AssignableDynamicGroups within the specified
     *         level.
     */
    public Set getAssignableDynamicGroups(int level) throws AMException,
            SSOException {
        return search(level, getSearchFilter(
                AMObject.ASSIGNABLE_DYNAMIC_GROUP));
    }

    /**
     * Gets number of assignable dynamic groups within the specified level.
     * 
     * @param level
     *            The search level starting from the organizational unit.
     * @return Number of assignable dynamic groups within the specified level.
     */
    public long getNumberOfAssignableDynamicGroups(int level)
            throws AMException, SSOException {
        return getAssignableDynamicGroups(level).size();
    }

    /**
     * Searches for assignable dynamic groups in this organizational unit using
     * wildcards. Wildcards can be specified such as a*, *, *a.
     * 
     * @param wildcard
     *            wildcard pattern to be used in the search
     * @param level
     *            the search level that needs to be used (AMConstants.SCOPE_ONE
     *            or AMConstansts.SCOPE_SUB)
     * 
     * @return Set Set of DNs of assignable dynamic groups matching the search
     * 
     * @throws AMException
     *             if there is an internal error in the AM Store
     * @throws SSOException
     *             if the sign on is no longer valid
     */
    public Set searchAssignableDynamicGroups(String wildcard, int level)
            throws AMException, SSOException {
        return searchAssignableDynamicGroups(wildcard, null, level);
    }

    /**
     * Searches for assignable dynamic groups in this organizational unit using
     * wildcards. Wildcards can be specified such as a*, *, *a.
     * 
     * @param wildcard
     *            wildcard pattern to be used in the search
     * @param searchControl
     *            specifies the search scope to be used, VLV ranges etc.,
     * 
     * @return AMSearchResults Set of DNs of assignable dynamic groups matching
     *         the search
     * 
     * @throws AMException
     *             if there is an internal error in the AM Store
     * @throws SSOException
     *             if the sign on is no longer valid
     */
    public AMSearchResults searchAssignableDynamicGroups(String wildcard,
            AMSearchControl searchControl) throws AMException, SSOException {
        return searchAssignableDynamicGroups(wildcard, null, searchControl);
    }

    /**
     * Searches for assignable dynamic groups in this organizational unit using
     * wildcards and attribute values. Wildcards can be specified such as a*, *,
     * *a. To further refine the search, attribute-value pairs can be specifed
     * so that DNs of dynamic groups with matching attribute-value pairs will be
     * returned.
     * 
     * @param wildcard
     *            wildcard pattern to be used in the search
     * @param avPairs
     *            attribute-value pairs to match when searching assignable
     *            dynamic groups
     * @param level
     *            the search level that needs to be used (AMConstants.SCOPE_ONE
     *            or AMConstansts.SCOPE_SUB)
     * 
     * @return Set Set of DNs of assignable dynamic groups matching the search
     * 
     * @throws AMException
     *             if there is an internal error in the AM Store
     * @throws SSOException
     *             if the sign on is no longer valid
     */
    public Set searchAssignableDynamicGroups(String wildcard, Map avPairs,
            int level) throws AMException, SSOException {
        return searchObjects(AMNamingAttrManager.getNamingAttr(GROUP),
                getSearchFilter(AMObject.ASSIGNABLE_DYNAMIC_GROUP), wildcard,
                avPairs, level);
    }

    /**
     * Searches for assignable dynamic groups in this organizational unit using
     * wildcards and attribute values. Wildcards can be specified such as a*, *,
     * *a. To further refine the search, attribute-value pairs can be specifed
     * so that DNs of dynamic groups with matching attribute-value pairs will be
     * returned.
     * 
     * @param wildcard
     *            wildcard pattern to be used in the search
     * @param avPairs
     *            attribute-value pairs to match when searching assignable
     *            dynamic groups
     * @param searchControl
     *            specifies the search scope to be used, VLV ranges etc.,
     * 
     * @return AMSearchResults which contains a Set of DNs of assignable dynamic
     *         groups matching the search
     * 
     * @throws AMException
     *             if there is an internal error in the AM Store
     * @throws SSOException
     *             if the sign on is no longer valid
     */
    public AMSearchResults searchAssignableDynamicGroups(String wildcard,
            Map avPairs, AMSearchControl searchControl) throws AMException,
            SSOException {
        return searchObjects(AMNamingAttrManager.getNamingAttr(GROUP),
                getSearchFilter(AMObject.ASSIGNABLE_DYNAMIC_GROUP), wildcard,
                avPairs, searchControl);
    }

    /**
     * Creates dynamic groups and initializes their attributes.
     * 
     * @param dynamicGroups
     *            Map where the key is the name of the dynamic group, and the
     *            value is a Map to represent Attribute-Value Pairs
     * 
     * @return Set of AMDynamicGroup objects created
     * @throws AMException
     *             if there is an internal error in the AM Store
     * @throws SSOException
     *             if the sign on is no longer valid
     */
    public Set createDynamicGroups(Map dynamicGroups) throws AMException,
            SSOException {
        Iterator iter = dynamicGroups.keySet().iterator();
        Set groups = new HashSet();

        while (iter.hasNext()) {
            String groupName = (String) iter.next();
            String groupDN = AMNamingAttrManager.getNamingAttr(GROUP) + "="
                    + groupName + "," + super.entryDN;

            Map attributes = (Map) dynamicGroups.get(groupName);
            AMDynamicGroupImpl groupImpl = new AMDynamicGroupImpl(super.token,
                    groupDN);
            groupImpl.setAttributes(attributes);
            groupImpl.create();
            groups.add(groupImpl);
        }

        return groups;
    }

    /**
     * Deletes dynamic groups.
     * 
     * @param dynamicGroupDNs
     *            The set of dynamic groups's DNs to be deleted.
     * 
     * @throws AMException
     *             if there is an internal error in the AM Store
     * @throws SSOException
     *             if the sign on is no longer valid
     */
    public void deleteDynamicGroups(Set dynamicGroupDNs) throws AMException,
            SSOException {
        Iterator iter = dynamicGroupDNs.iterator();

        while (iter.hasNext()) {
            String dynamicGroupDN = (String) iter.next();
            AMDynamicGroup dynamicGroup = new AMDynamicGroupImpl(super.token,
                    dynamicGroupDN);
            dynamicGroup.delete();
        }
    }

    /**
     * Gets the dynamic groups within the specified level.
     * 
     * @param level
     *            The search level starting from the organization unit.
     * @return The Set of DNs of DynamicGroups within the specified level.
     */
    public Set getDynamicGroups(int level) throws AMException, SSOException {
        return search(level, getSearchFilter(AMObject.DYNAMIC_GROUP));
    }

    /**
     * Gets number of dynamic groups within the specified level.
     * 
     * @param level
     *            The search level starting from the organization unit.
     * @return Number of dynamic groups within the specified level.
     */
    public long getNumberOfDynamicGroups(int level) throws AMException,
            SSOException {
        return getDynamicGroups(level).size();
    }

    /**
     * Searches for dynamic groups in this organization unit using wildcards.
     * Wildcards can be specified such as a*, *, *a.
     * 
     * @param wildcard
     *            wildcard pattern to be used in the search
     * @param level
     *            the search level that needs to be used (AMConstants.SCOPE_ONE
     *            or AMConstansts.SCOPE_SUB)
     * 
     * @return Set Set of DNs of dynamic groups matching the search
     * 
     * @throws AMException
     *             if there is an internal error in the AM Store
     * @throws SSOException
     *             if the sign on is no longer valid
     */
    public Set searchDynamicGroups(String wildcard, int level)
            throws AMException, SSOException {
        return searchDynamicGroups(wildcard, null, level);
    }

    /**
     * Searches for dynamic groups in this organization unit using wildcards.
     * Wildcards can be specified such as a*, *, *a.
     * 
     * @param wildcard
     *            wildcard pattern to be used in the search
     * @param searchControl
     *            specifies the search scope to be used, VLV ranges etc.,
     * 
     * @return AMSearchResults which contains a Set of DNs of dynamic groups
     *         matching the search
     * 
     * @throws AMException
     *             if there is an internal error in the AM Store
     * @throws SSOException
     *             if the sign on is no longer valid
     */
    public AMSearchResults searchDynamicGroups(String wildcard,
            AMSearchControl searchControl) throws AMException, SSOException {
        return searchDynamicGroups(wildcard, null, searchControl);
    }

    /**
     * Searches for dynamic groups in this organization unit using wildcards and
     * attribute values. Wildcards can be specified such as a*, *, *a. To
     * further refine the search, attribute-value pairs can be specifed so that
     * DNs of dynamic groups with matching attribute-value pairs will be
     * returned.
     * 
     * @param wildcard
     *            wildcard pattern to be used in the search
     * @param avPairs
     *            attribute-value pairs to match when searching dynamic groups
     * @param level
     *            the search level that needs to be used (AMConstants.SCOPE_ONE
     *            or AMConstansts.SCOPE_SUB)
     * 
     * @return Set Set of DNs of dynamic groups matching the search
     * 
     * @throws AMException
     *             if there is an internal error in the AM Store
     * @throws SSOException
     *             if the sign on is no longer valid
     */
    public Set searchDynamicGroups(String wildcard, Map avPairs, int level)
            throws AMException, SSOException {
        return searchObjects(AMNamingAttrManager.getNamingAttr(GROUP),
                getSearchFilter(AMObject.DYNAMIC_GROUP), wildcard, avPairs,
                level);
    }

    /**
     * Searches for dynamic groups in this organization unit using wildcards and
     * attribute values. Wildcards can be specified such as a*, *, *a. To
     * further refine the search, attribute-value pairs can be specifed so that
     * DNs of dynamic groups with matching attribute-value pairs will be
     * returned.
     * 
     * @param wildcard
     *            wildcard pattern to be used in the search
     * @param avPairs
     *            attribute-value pairs to match when searching dynamic groups
     * @param searchControl
     *            specifies the search scope to be used, VLV ranges etc.,
     * 
     * @return AMSearchResults which contains a Set of DNs of dynamic groups
     *         matching the search
     * 
     * @throws AMException
     *             if there is an internal error in the AM Store
     * @throws SSOException
     *             if the sign on is no longer valid
     */
    public AMSearchResults searchDynamicGroups(String wildcard, Map avPairs,
            AMSearchControl searchControl) throws AMException, SSOException {
        return searchObjects(AMNamingAttrManager.getNamingAttr(GROUP),
                getSearchFilter(AMObject.DYNAMIC_GROUP), wildcard, avPairs,
                searchControl);
    }

    /**
     * Creates static groups.
     * 
     * @param groups
     *            The set of static groups's names to be created.
     * @return Set set of AMStaticGroup objects created.
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
     * Deletes static groups.
     * 
     * @param groups
     *            The set of static groups to be deleted.
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
     * Gets the static groups within the specified level.
     * 
     * @param level
     *            The search level starting from the organizational unit.
     * @return The Set of static group DN's within the specified level.
     */
    public Set getStaticGroups(int level) throws AMException, SSOException {
        return search(level, getSearchFilter(AMObject.GROUP));
    }

    /**
     * Gets number of static groups within the specified level.
     * 
     * @param level
     *            The search level starting from the organizational unit.
     * @return Number of static groups within the specified level.
     */
    public long getNumberOfStaticGroups(int level) throws AMException,
            SSOException {
        return getStaticGroups(level).size();
    }

    /**
     * Searches for static groups in this organizational unit using wildcards.
     * Wildcards can be specified such as a*, *, *a.
     * 
     * @param wildcard
     *            wildcard pattern to be used in the search
     * @param level
     *            the search level that needs to be used (AMConstants.SCOPE_ONE
     *            or AMConstansts.SCOPE_SUB)
     * 
     * @return Set Set of DNs of static groups matching the search
     * 
     * @throws AMException
     *             if there is an internal error in the AM Store
     * @throws SSOException
     *             if the sign on is no longer valid
     */
    public Set searchStaticGroups(String wildcard, int level)
            throws AMException, SSOException {
        return searchStaticGroups(wildcard, null, level);
    }

    /**
     * Searches for static groups in this organizational unit using wildcards.
     * Wildcards can be specified such as a*, *, *a.
     * 
     * @param wildcard
     *            wildcard pattern to be used in the search
     * @param searchControl
     *            specifies the search scope to be used, VLV ranges etc.,
     * 
     * @return AMSearchResults which contains a Set of DNs of static groups
     *         matching the search
     * 
     * @throws AMException
     *             if there is an internal error in the AM Store
     * @throws SSOException
     *             if the sign on is no longer valid
     */
    public AMSearchResults searchStaticGroups(String wildcard,
            AMSearchControl searchControl) throws AMException, SSOException {
        return searchStaticGroups(wildcard, null, searchControl);
    }

    /**
     * Searches for static groups in this organizational unit using wildcards
     * and attribute values. Wildcards can be specified such as a*, *, *a. To
     * further refine the search, attribute-value pairs can be specifed so that
     * DNs of static groups with matching attribute-value pairs will be
     * returned.
     * 
     * @param wildcard
     *            wildcard pattern to be used in the search
     * @param avPairs
     *            attribute-value pairs to match when searching static group
     * @param level
     *            the search level that needs to be used (AMConstants.SCOPE_ONE
     *            or AMConstansts.SCOPE_SUB)
     * 
     * @return Set Set of DNs of static groups matching the search
     * 
     * @throws AMException
     *             if there is an internal error in the AM Store
     * @throws SSOException
     *             if the sign on is no longer valid
     */
    public Set searchStaticGroups(String wildcard, Map avPairs, int level)
            throws AMException, SSOException {
        return searchObjects(AMNamingAttrManager.getNamingAttr(GROUP),
                getSearchFilter(AMObject.GROUP), wildcard, avPairs, level);
    }

    /**
     * Searches for static groups in this organizational unit using wildcards
     * and attribute values. Wildcards can be specified such as a*, *, *a. To
     * further refine the search, attribute-value pairs can be specifed so that
     * DNs of static groups with matching attribute-value pairs will be
     * returned.
     * 
     * @param wildcard
     *            wildcard pattern to be used in the search
     * @param avPairs
     *            attribute-value pairs to match when searching static group
     * @param searchControl
     *            specifies the search scope to be used, VLV ranges etc.,
     * 
     * @return AMSearchResults which contains a Set of DNs of static groups
     *         matching the search
     * 
     * @throws AMException
     *             if there is an internal error in the AM Store
     * @throws SSOException
     *             if the sign on is no longer valid
     */
    public AMSearchResults searchStaticGroups(String wildcard, Map avPairs,
            AMSearchControl searchControl) throws AMException, SSOException {
        return searchObjects(AMNamingAttrManager.getNamingAttr(GROUP),
                getSearchFilter(AMObject.GROUP), wildcard, avPairs,
                searchControl);
    }

    /**
     * Searches for groups in this organizational unit using wildcards and
     * attribute values. Wildcards can be specified such as a*, *, *a.
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
     * Searches for groups in this organization using wildcards and attribute
     * values. Wildcards can be specified such as a*, *, *a.
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

    /**
     * Creates people containers.
     * 
     * @param peopleContainers
     *            The set of people containers' names to be created
     * @return Set set of PeopleContainer objects created
     * @throws AMException
     *             if there is an internal error in the AM Store
     * @throws SSOException
     *             if the sign on is no longer valid
     */
    public Set createPeopleContainers(Set peopleContainerNames)
            throws AMException, SSOException {
        Iterator iter = peopleContainerNames.iterator();
        Set peopleContainers = new HashSet();

        while (iter.hasNext()) {
            String peopleContainerDN = AMNamingAttrManager
                    .getNamingAttr(PEOPLE_CONTAINER)
                    + "=" + ((String) iter.next()) + "," + super.entryDN;
            AMPeopleContainerImpl peopleContainerImpl = 
                new AMPeopleContainerImpl(super.token, peopleContainerDN);
            peopleContainerImpl.create();
            peopleContainers.add(peopleContainerImpl);
        }

        return peopleContainers;
    }

    /**
     * Creates people containers and initializes their attributes.
     * 
     * @param peopleContainers
     *            Map where the key is the name of the peopleContainer, and the
     *            value is a Map to represent Attribute-Value Pairs
     * @return Set set of PeopleContainer objects created
     * @throws AMException
     *             if there is an internal error in the AM Store
     * @throws SSOException
     *             if the sign on is no longer valid
     */
    public Set createPeopleContainers(Map peopleContainersMap)
            throws AMException, SSOException {
        Iterator iter = peopleContainersMap.keySet().iterator();
        Set peopleContainers = new HashSet();

        while (iter.hasNext()) {
            String peopleContainerName = (String) iter.next();
            String peopleContainerDN = AMNamingAttrManager
                    .getNamingAttr(PEOPLE_CONTAINER)
                    + "=" + peopleContainerName + "," + super.entryDN;
            Map attributes = (Map) peopleContainersMap.get(peopleContainerName);
            AMPeopleContainerImpl peopleContainerImpl = 
                new AMPeopleContainerImpl(super.token, peopleContainerDN);
            peopleContainerImpl.setAttributes(attributes);
            peopleContainerImpl.create();
            peopleContainers.add(peopleContainerImpl);
        }

        return peopleContainers;
    }

    /**
     * Deletes people containers.
     * 
     * @param peopleContainers
     *            The set of people containers' RDN to be removed.
     * 
     * @throws AMException
     *             if there is an internal error in the AM Store
     * @throws SSOException
     *             if the sign on is no longer valid
     */
    public void deletePeopleContainers(Set peopleContainerDNs)
            throws AMException, SSOException {
        Iterator iter = peopleContainerDNs.iterator();

        while (iter.hasNext()) {
            String peopleContainerDN = (String) iter.next();
            AMPeopleContainer peopleContainer = new AMPeopleContainerImpl(
                    super.token, peopleContainerDN);
            peopleContainer.delete();
        }
    }

    /**
     * Gets the people containers within the specified level.
     * 
     * @param level
     *            The search level starting from the organizational unit.
     * @return The Set of PeopleContainer's within the specified level.
     */
    public Set getPeopleContainers(int level) throws AMException, SSOException {
        return search(level, getSearchFilter(AMObject.PEOPLE_CONTAINER));
    }

    /**
     * Gets number of people containers within the specified level.
     * 
     * @param level
     *            The search level starting from the organizational unit.
     * @return Number of people containers within the specified level.
     */
    public long getNumberOfPeopleContainers(int level) throws AMException,
            SSOException {
        return getPeopleContainers(level).size();
    }

    /**
     * Searches for people containers in this organizational unit using
     * wildcards and * attribute values. Wildcards can be specified such as 
     * a*, *, *a.
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
    public Set searchPeopleContainers(String wildcard, int level)
            throws AMException, SSOException {
        return searchPeopleContainers(wildcard, null, level);
    }

    /**
     * Searches for people containers in this organizational unit using
     * wildcards and * attribute values. Wildcards can be specified such as a*, 
     * *, *a.
     * 
     * @param wildcard
     *            wildcard pattern to be used in the search
     * @param searchControl
     *            specifies the search scope to be used, VLV ranges etc.,
     * 
     * @return AMSearchResults which contains a Set DNs of people containers
     *         matching the search
     * 
     * @throws AMException
     *             if there is an internal error in the AM Store
     * @throws SSOException
     *             if the sign on is no longer valid
     */
    public AMSearchResults searchPeopleContainers(String wildcard,
            AMSearchControl searchControl) throws AMException, SSOException {
        return searchPeopleContainers(wildcard, null, searchControl);
    }

    /**
     * Searches for people containers in this organizational unit using
     * wildcards and * attribute values. Wildcards can be specified such as a*,
     * *, *a. To further refine the search, attribute-value pairs can be 
     * specifed so that DNs of people containers with matching attribute-value 
     * pairs will be returned.
     * 
     * @param wildcard
     *            wildcard pattern to be used in the search
     * @param avPairs
     *            attribute-value pairs to match when searching users
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
    public Set searchPeopleContainers(String wildcard, Map avPairs, int level)
            throws AMException, SSOException {
        return searchObjects(AMNamingAttrManager
                .getNamingAttr(PEOPLE_CONTAINER),
                getSearchFilter(AMObject.PEOPLE_CONTAINER), wildcard, avPairs,
                level);
    }

    /**
     * Searches for people containers in this organizational unit using
     * wildcards and * attribute values. Wildcards can be specified such as a*,
     * *, *a. To further refine the search, attribute-value pairs can be 
     * specifed so that DNs of people containers with matching attribute-value
     * pairs will be returned.
     * 
     * @param wildcard
     *            wildcard pattern to be used in the search
     * @param avPairs
     *            attribute-value pairs to match when searching users
     * @param searchControl
     *            specifies the search scope to be used, VLV ranges etc.,
     * 
     * @return AMSearchResults which contains a Set DNs of people containers
     *         matching the search
     * 
     * @throws AMException
     *             if there is an internal error in the AM Store
     * @throws SSOException
     *             if the sign on is no longer valid
     */
    public AMSearchResults searchPeopleContainers(String wildcard, Map avPairs,
            AMSearchControl searchControl) throws AMException, SSOException {
        return searchObjects(AMNamingAttrManager
                .getNamingAttr(PEOPLE_CONTAINER),
                getSearchFilter(AMObject.PEOPLE_CONTAINER), wildcard, avPairs,
                searchControl);
    }

    /**
     * Creates group containers.
     * 
     * @param groupContainerNames
     *            The set of group containers' names to be created
     * @return Set set of GroupContainer objects created
     * @throws AMException
     *             if there is an internal error in the AM Store
     * @throws SSOException
     *             if the sign on is no longer valid
     */
    public Set createGroupContainers(Set groupContainerNames)
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
     * Creates group containers and initializes their attributes.
     * 
     * @param groupContainersMap
     *            Map where the key is the name of the groupContainer, and the
     *            value is a Map to represent Attribute-Value Pairs
     * @return Set set of GroupContainer objects created
     * @throws AMException
     *             if there is an internal error in the AM Store
     * @throws SSOException
     *             if the sign on is no longer valid
     */
    public Set createGroupContainers(Map groupContainersMap)
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
     * Deletes group containers.
     * 
     * @param groupContainers
     *            The set of group containers' DN to be removed.
     * 
     * @throws AMException
     *             if there is an internal error in the AM Store
     * @throws SSOException
     *             if the sign on is no longer valid
     */
    public void deleteGroupContainers(Set groupContainerDNs)
            throws AMException, SSOException {
        Iterator iter = groupContainerDNs.iterator();

        while (iter.hasNext()) {
            String groupContainerDN = (String) iter.next();
            AMGroupContainer groupContainer = new AMGroupContainerImpl(token,
                    groupContainerDN);
            groupContainer.delete();
        }
    }

    /**
     * Gets the group containers within the specified level.
     * 
     * @param level
     *            The search level starting from the organizational unit.
     * @return The Set of GroupContainer's within the specified level.
     */
    public Set getGroupContainers(int level) throws AMException, SSOException {
        return search(level, getSearchFilter(AMObject.GROUP_CONTAINER));
    }

    /**
     * Gets number of group containers within the specified level.
     * 
     * @param level
     *            The search level starting from the organizational unit.
     * @return Number of group containers within the specified level.
     */
    public long getNumberOfGroupContainers(int level) throws AMException,
            SSOException {
        return getGroupContainers(level).size();
    }

    /**
     * Searches for group containers in this organizational unit using wildcards
     * and attribute values. Wildcards can be specified such as a*, *, *a. To
     * further refine the search, attribute-value pairs can be specifed so that
     * DNs of group containers with matching attribute-value pairs will be
     * returned.
     * 
     * @param wildcard
     *            wildcard pattern to be used in the search
     * @param avPairs
     *            attribute-value pairs to match when searching users
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
    public Set searchGroupContainers(String wildcard, Map avPairs, int level)
            throws AMException, SSOException {
        return searchObjects(
                AMNamingAttrManager.getNamingAttr(GROUP_CONTAINER),
                getSearchFilter(AMObject.GROUP_CONTAINER), wildcard, avPairs,
                level);

    }

    /**
     * Searches for group containers in this organizational unit using wildcards
     * and attribute values. Wildcards can be specified such as a*, *, *a. To
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
    public AMSearchResults searchGroupContainers(String wildcard, Map avPairs,
            AMSearchControl searchControl) throws AMException, SSOException {
        return searchObjects(
                AMNamingAttrManager.getNamingAttr(GROUP_CONTAINER),
                getSearchFilter(AMObject.GROUP_CONTAINER), wildcard, avPairs,
                searchControl);

    }

    /**
     * Creates users in this organizational unit. For each user the, object
     * classes specified by organization type attribute
     * <code>iplanet-am-required-services</code> of the service
     * <code>iPlanetAMAdminConsoleService</code> template are added. If a
     * corresponding template does not exist, the default values are picked up
     * from schema.
     * 
     * @param users
     *            The set of user names to be created in this organizational
     *            unit.
     * @return Set Set of User objects created
     * @throws AMException
     *             if there is an internal error in the AM Store
     * @throws SSOException
     *             if the sign on is no longer valid
     */
    public Set createUsers(Set userNames) throws AMException, SSOException {
        Set usersSet = new HashSet();
        Set serviceNames = getOrgTypeAttributes(ADMINISTRATION_SERVICE,
                REQUIRED_SERVICES_ATTR);

        Set objectClasses = null;
        if ((serviceNames != null) && (!serviceNames.isEmpty())) {
            objectClasses = AMServiceUtils.getServiceObjectClasses(token,
                    serviceNames);
        }

        Iterator iter = userNames.iterator();
        while (iter.hasNext()) {
            String userDN = AMNamingAttrManager.getNamingAttr(USER) + "="
                    + ((String) iter.next()) + "," + super.entryDN;
            AMUserImpl user = new AMUserImpl(super.token, userDN);
            if (objectClasses != null && !objectClasses.isEmpty()) {
                user.setAttribute("objectclass", objectClasses);
            }
            user.create();
            usersSet.add(user);
        }

        return usersSet;
    }

    /**
     * Creates users and initializes their attributes.For each user the, object
     * classes specified by organization type attribute
     * <code>iplanet-am-required-services</code> of the service
     * <code>iPlanetAMAdminConsoleService</code> template are added. If a
     * corresponding template does not exist, the default values are picked up
     * from schema.
     * 
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
        Set usersSet = new HashSet();
        // Get the serviceNames from the console org template
        Set serviceNames = getOrgTypeAttributes(ADMINISTRATION_SERVICE,
                REQUIRED_SERVICES_ATTR);
        Set objectClasses = null;
        if ((serviceNames != null) && (!serviceNames.isEmpty())) {
            objectClasses = AMServiceUtils.getServiceObjectClasses(token,
                    serviceNames);
        }

        Iterator iter = usersMap.keySet().iterator();
        while (iter.hasNext()) {
            String userName = (String) iter.next();
            String userDN = AMNamingAttrManager.getNamingAttr(USER) + "="
                    + userName + "," + super.entryDN;
            AMUserImpl user = new AMUserImpl(super.token, userDN);
            Map userMap = (Map) usersMap.get(userName);
            user.setAttributes(userMap);
            if (objectClasses != null && !objectClasses.isEmpty()) {
                Set existingOC = (Set) userMap.get("objectclass");
                if (existingOC != null && !existingOC.isEmpty())
                    objectClasses.addAll(existingOC);
                user.setAttribute("objectclass", objectClasses);
            }
            user.create();
            usersSet.add(user);
        }
        return usersSet;
    }

    /**
     * Removes users from the organizational unit.
     * 
     * @param users
     *            The set of user DN's to be removed from the organizational
     *            unit.
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
     * Gets the names (DNs) of users in the organizational unit.
     * 
     * @return Set The names(DNs) of users in the organizational unit.
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
     * Gets number of users within the specified level.
     * 
     * @param level
     *            The search level starting from the organizational unit.
     * @return Number of users within the specified level.
     */
    public long getNumberOfUsers(int level) throws AMException, SSOException {
        return getUsers(level).size();
    }

    /**
     * Searches for users in this organizational unit using wildcards and
     * attribute values. Wildcards can be specified such as a*, *, *a.
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
     * Searches for users in this organizational unit using wildcards and
     * attribute values. Wildcards can be specified such as a*, *, *a.
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
     * Searches for users in this organizational unit using wildcards and
     * attribute values. Wildcards can be specified such as a*, *, *a. To
     * further refine the search, attribute-value pairs can be specifed so that
     * DNs of users with matching attribute-value pairs will be returned.
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
     * Searches for users in this organizational unit using wildcards and
     * attribute values. Wildcards can be specified such as a*, *, *a. To
     * further refine the search, attribute-value pairs can be specifed so that
     * DNs of users with matching attribute-value pairs will be returned.
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
     * Searches for users in this organizational unit using wildcards and
     * filter. Wildcards can be specified such as a*, *, *a. To further refine
     * the search, filter can be specifed so that DNs of users with matching
     * filter will be returned.
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
     * Searches for users in this organization attribute values. Wildcards such
     * as can be specified for the attribute values. The DNs of users with
     * matching attribute-value pairs will be returned.
     * 
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

    /**
     * Gets the number of services.
     * 
     * @return int number of services.
     */
    public long getNumberOfServices() throws AMException, SSOException {
        return getRegisteredServiceNames().size();
    }

    /**
     * Gets the names of registered services.
     * 
     * @return The Set of the names of registered services.
     */
    public Set getRegisteredServiceNames() throws AMException, SSOException {
        SSOTokenManager.getInstance().validateToken(super.token);
        return dsServices.getRegisteredServiceNames(super.token, super.entryDN);
    }

    /**
     * Register a service for this organizational unit.
     * 
     * @param serviceName
     *            The name of service to be registered
     * @param createTemplate
     *            true if to create default template
     * @param activate
     *            true if to activate the service
     * @throws AMException
     *             if the service does not exist or could not be registered.
     * @throws SSOException
     *             if the sign on is no longer valid.
     */
    public void registerService(String serviceName, boolean createTemplate,
            boolean activate) throws AMException, SSOException {
        if (debug.messageEnabled()) {
            debug.message("AMOrganizationalUnitImpl.registerService("
                    + serviceName + ", " + createTemplate + ", " + activate
                    + ")");
        }

        dsServices.registerService(super.token, super.entryDN, serviceName);

        Set serviceStatus = getAttribute(SERVICE_STATUS_ATTRIBUTE);
        if (!serviceStatus.equals(Collections.EMPTY_SET)) {
            Iterator iter = serviceStatus.iterator();
            while (iter.hasNext()) {
                String status = (String) iter.next();
                if (status.equalsIgnoreCase(serviceName)) {
                    Object args[] = { serviceName };
                    throw new AMException(AMSDKBundle.getString("464", args,
                            super.locale), "464", args);
                }
            }
        } else {
            serviceStatus = new HashSet();
        }

        serviceStatus.add(serviceName);
        setAttribute(SERVICE_STATUS_ATTRIBUTE, serviceStatus);
        try {
            store();
        } catch (AMException le) {
            // for existing DIT, if it is
            // object class violation, try add iplanet-am-managed-org-unit
            try {
                Set objectClass = getAttribute("objectclass");
                if (debug.messageEnabled()) {
                    debug.message("object class=" + objectClass);
                }
                if (!objectClass.contains("iplanet-am-managed-org-unit")) {
                    objectClass.add("iplanet-am-managed-org-unit");
                    setAttribute("objectclass", objectClass);
                    setAttribute(SERVICE_STATUS_ATTRIBUTE, serviceStatus);
                    store();
                } else {
                    throw le;
                }
            } catch (Exception e) {
                if (debug.messageEnabled()) {
                    debug.message("error adding objectclass", e);
                }
                // throw original exception
                throw le;
            }
        }

        if (createTemplate) {
            try {
                if (AMServiceUtils.serviceHasSubSchema(super.token,
                        serviceName, SchemaType.POLICY)) {
                    createTemplate(AMTemplate.POLICY_TEMPLATE, serviceName,
                            null);
                }

                if (AMServiceUtils.serviceHasSubSchema(super.token,
                        serviceName, SchemaType.DYNAMIC)) {
                    createTemplate(AMTemplate.DYNAMIC_TEMPLATE, serviceName,
                            null);
                }
            } catch (SMSException smsex) {
                throw new AMException(AMSDKBundle
                        .getString("451", super.locale), "451");
            }
        }
    }

    /**
     * Unregister a service for this organizational unit.
     * 
     * @param String
     *            serviceName to be unregistered
     * @throws AMException
     *             if the service does not exist or could not be unregistered.
     * @throws SSOException
     *             if the sign on is no longer valid.
     */
    public void unregisterService(String serviceName) throws AMException,
            SSOException {
        if (debug.messageEnabled()) {
            debug.message("AMOrganizationalUnitImpl.unregisterService("
                    + serviceName + ")");
        }

        SSOTokenManager.getInstance().validateToken(super.token);

        // First check if the Service is registered for sub-orgs if so throw an
        // error message
        if (isRegisteredForSubOrgs(serviceName)) {
            Object args[] = { serviceName };
            throw new AMException(AMSDKBundle.getString("445", args,
                    super.locale), "445", args);
        }

        try {
            if (AMServiceUtils.serviceHasSubSchema(super.token, serviceName,
                    SchemaType.DYNAMIC)) {

                dsServices.unRegisterService(super.token, super.entryDN,
                        super.profileType, serviceName,
                        AMTemplate.DYNAMIC_TEMPLATE);
            }

            if (AMServiceUtils.serviceHasSubSchema(super.token, serviceName,
                    SchemaType.ORGANIZATION)) {
                if (orgTemplateExists(serviceName)) {
                    AMTemplate template = getTemplate(serviceName,
                            AMTemplate.ORGANIZATION_TEMPLATE);
                    template.delete();
                }
            }

            Set serviceStatus = getAttribute(SERVICE_STATUS_ATTRIBUTE);
            Iterator iter = serviceStatus.iterator();
            while (iter.hasNext()) {
                String status = (String) iter.next();
                if (status.equalsIgnoreCase(serviceName)) {
                    serviceStatus.remove(status);
                    setAttribute(SERVICE_STATUS_ATTRIBUTE, serviceStatus);
                    store();
                    return;
                }
            }

            Object args[] = { serviceName };
            throw new AMException(AMSDKBundle.getString("463", args,
                    super.locale), "463", args);

        } catch (SMSException smsex) {
            Object args[] = { serviceName };
            throw new AMException(AMSDKBundle.getString("913", args,
                    super.locale), "913", args);
        }
    }

    public Set getUsers(int level) throws AMException, SSOException {
        return search(level, getSearchFilter(AMObject.USER));
    }

    /**
     * Unassigns the given policies from this organizational unit and its roles.
     * 
     * @param serviceName
     *            serviceName
     * @param policyDNs
     *            Set of policy DN string
     * 
     * @throws AMException
     *             if there is an internal problem with AM Store.
     * @throws SSOException
     *             if the sign-on is no longer valid.
     */
    public void unassignAllPolicies(String serviceName, Set policyDNs)
            throws AMException, SSOException {
        throw new UnsupportedOperationException();
    }

    /**
     * Modifies all the templates under this organizational unit that contain
     * any policyDN in given policyDNs.
     * 
     * @param serviceName
     *            serviceName
     * @param policyDNs
     *            Set of policy DN string
     * 
     * @throws AMException
     *             if there is an internal problem with AM Store.
     * @throws SSOException
     *             if the sign-on is no longer valid.
     */
    public void modifyAllPolicyTemplates(String serviceName, Set policyDNs)
            throws AMException, SSOException {
        throw new UnsupportedOperationException();
    }

    /**
     * Deletes all the named policy templates for this OrganizationalUnit
     * corresponding to the given policy. This includes Org based and role based
     * policy templates. This is a convienence method.
     * 
     * @param policyDN
     *            a policy DN string
     * @throws AMException
     *             if there is an internal problem with AM Store.
     * @throws SSOException
     *             if the sign-on is no longer valid.
     */
    public boolean deleteAllNamedPolicyTemplates(String policyDN)
            throws AMException, SSOException {
        throw new UnsupportedOperationException();
    }

    /**
     * Gets all the assigned policies for this OrganizationalUnit
     * 
     * @return Set a set of assigned policy DNs
     * @throws AMException
     *             if there is an internal problem with AM Store.
     * @throws SSOException
     *             if the sign-on is no longer valid.
     */
    public Set getAssignedPolicyDNs() throws AMException, SSOException {

        throw new UnsupportedOperationException();
    }

    /**
     * Checks if a policyDN is assigned to an org or a role.
     * 
     * @param policyDN
     *            a policy DN string
     * @param serviceName
     *            service name
     * @return boolean true if policy is assigned to an org or role. false
     *         otherwise
     */
    public boolean isPolicyAssigned(String policyDN, String serviceName)
            throws AMException, SSOException {
        throw new UnsupportedOperationException();
    }

    /**
     * Checks if an org template exists for the service.
     * 
     * @param serviceName
     *            service name
     * @return boolean true if the org template exists. false otherwise
     */
    public boolean orgTemplateExists(String serviceName) throws AMException,
            SSOException {
        ServiceConfig sc = AMServiceUtils.getOrgConfig(token, entryDN,
                serviceName);
        return (sc != null);
    }

    public void assignService(String serviceName, Map attrMap)
            throws AMException, SSOException {
    }

    /**
     * Protected method to be used to obtain organization attribute values for a
     * given serviceName and attribute name. Returns a null value if a template
     * value or default value for the attribute does not exist.
     */
    protected Set getOrgTypeAttributes(String serviceName, String attrName)
            throws SSOException {
        Set attrValues = null;
        try {
            AMTemplate amTemplate = getTemplate(serviceName,
                    AMTemplate.ORGANIZATION_TEMPLATE);
            attrValues = amTemplate.getAttribute(attrName);
            if (debug.messageEnabled()) {
                debug.message("AMOrganizationImpl."
                        + "getOrgTypeAttributes(): "
                        + "obtained from org template " + serviceName + " : "
                        + attrName + "\n" + super.entryDN + " : " + attrValues);
            }
        } catch (AMException ame) {
            // Template not found
            // Get default Service attribues
            try {
                Map defaultValues = AMServiceUtils.getServiceConfig(token,
                        ADMINISTRATION_SERVICE, SchemaType.ORGANIZATION);
                attrValues = (Set) defaultValues.get(attrName);
                if (debug.messageEnabled()) {
                    debug.message("AMOrganizationImpl."
                            + "getOrgTypeAttributes(): "
                            + "obtained from org defaults " + serviceName
                            + " : " + attrName + "\n" + super.entryDN + " : "
                            + attrValues);
                }
            } catch (Exception se) {
                debug.warning("AMOrganizationImpl."
                        + "getOrgTypeAttributes(): "
                        + "Error encountered in retrieving "
                        + "default org attrs for", se);
            }

        }
        return attrValues;
    }

    /**
     * Method which returns a true or false after verifying that the service is
     * registered for the immediate sub-orgs for this parent org.
     */
    private boolean isRegisteredForSubOrgs(String serviceName)
            throws SSOException {
        // Get sub organizations
        Map avPair = new HashMap();
        Set value = new HashSet();
        value.add(serviceName);
        avPair.put(SERVICE_STATUS_ATTRIBUTE, value);

        Set subOrgs = null;
        try {
            subOrgs = searchSubOrganizationalUnits("*", avPair, SCOPE_SUB);
        } catch (AMException ae) {
            // ignore expection. sub-orgs may not be present
        }
        // Note: if the set size is 1, it implies that only this
        // organizationalunit
        // was returned as search result.
        if ((subOrgs != null) && (!subOrgs.isEmpty()) && (subOrgs.size() > 1)) {
            return true;
        } else {
            return false;
        }
    }


    public Set createEntities(String stype, Set entities) throws AMException,
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
        Set resultSet = new HashSet();
        int createType = Integer.parseInt(type);
        Iterator it = entities.iterator();
        while (it.hasNext()) {
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

    public Set getSupportedTypes() throws AMException, SSOException {
        return getAttribute(AMConstants.CONTAINER_SUPPORTED_TYPES_ATTRIBUTE);
    }

    public void setSupportedTypes(Set sTypes) throws AMException, SSOException {
        setAttribute(AMConstants.CONTAINER_SUPPORTED_TYPES_ATTRIBUTE, sTypes);
        store();
    }

}
