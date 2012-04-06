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
 * $Id: AMOrganizationImpl.java,v 1.9 2008/06/25 05:41:21 qcheng Exp $
 *
 */

/**
 * Portions Copyrighted [2011] [ForgeRock AS]
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
import com.sun.identity.sm.ServiceSchema;
import com.sun.identity.sm.ServiceSchemaManager;

/**
 * The <code>AMOrganizationImpl</code> class implements interface
 * AMOrganization
 *
 * @deprecated  As of Sun Java System Access Manager 7.1.
 */

class AMOrganizationImpl extends AMObjectImpl implements AMOrganization {

    static String statusAN = "inetDomainStatus";

    public AMOrganizationImpl(SSOToken ssoToken, String DN) {
        super(ssoToken, DN, ORGANIZATION);
    }

    /**
     * Creates sub-organizations.
     * 
     * @param subOrganizations
     *            The set of sub-organizations names to be created.
     * @return Set set of sub Organization objects created.
     * 
     * @throws AMException
     *             if there is an internal error in the AM Store
     * @throws SSOException
     *             if the sign on is no longer valid
     */
    public Set createSubOrganizations(Set subOrganizations) throws AMException,
            SSOException {
        Iterator iter = subOrganizations.iterator();
        Set subOrgs = new HashSet();

        while (iter.hasNext()) {
            StringBuilder subOrgDNSB = new StringBuilder();
            subOrgDNSB.append(AMNamingAttrManager.getNamingAttr(ORGANIZATION))
                    .append("=").append((String) iter.next()).append(",")
                    .append(super.entryDN);

            AMOrganizationImpl subOrgImpl = new AMOrganizationImpl(super.token,
                    subOrgDNSB.toString());

            subOrgImpl.create();
            subOrgs.add(subOrgImpl);
        }

        return subOrgs;
    }

    /**
     * Creates sub-organizations and initializes their attributes.
     * 
     * @param subOrganizations
     *            Map where the key is the name of the suborganization, and the
     *            value is a Map to represent Attribute-Value Pairs
     * @return Set set of sub Organization objects created.
     * @throws AMException
     *             if there is an internal error in the AM Store
     * @throws SSOException
     *             if the sign on is no longer valid
     */
    public Set createSubOrganizations(Map subOrganizationsMap)
            throws AMException, SSOException {
        Iterator iter = subOrganizationsMap.keySet().iterator();
        Set subOrgs = new HashSet();

        while (iter.hasNext()) {
            String subOrgName = (String) iter.next();
            StringBuilder subOrgDNSB = new StringBuilder();
            subOrgDNSB.append(AMNamingAttrManager.getNamingAttr(ORGANIZATION))
                    .append("=").append(subOrgName).append(",").append(
                            super.entryDN);

            Map attributes = (Map) subOrganizationsMap.get(subOrgName);
            AMOrganizationImpl subOrgImpl = new AMOrganizationImpl(super.token,
                    subOrgDNSB.toString());
            subOrgImpl.setAttributes(attributes);
            subOrgImpl.create();
            subOrgs.add(subOrgImpl);
        }

        return subOrgs;
    }

    /**
     * Create sub-organization and initialize their attributes. Initializes
     * service objectclasses and attributes as provided in the <Code>
     * serviceNameAndAttrs </Code> map.
     * 
     * @param orgName
     *            name of organization to be created under this organization.
     * @param domainname,
     *            name of the domain (ex: sun.com, iplanet.com).
     * @param attrMap
     *            Map of attribute-value pairs to be set on the entry.
     * @param serviceNamesAndAttrs
     *            Map of service names and attribute-values for that service to
     *            be set in the org entry. <Code> serviceNameAndAttrs </Code>
     *            has service names keys and map of attribute-values (values are
     *            in a Set).
     * @return DN of organization created.
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store
     * @throws SSOException
     *             if the sign on is no longer valid
     */
    public String createOrganization(String orgName, String domainName,
            Map attrMap, Map serviceNamesAndAttrs) throws AMException,
            SSOException {
        String subOrgName = orgName;
        StringBuilder subOrgDNSB = new StringBuilder();
        subOrgDNSB.append(AMNamingAttrManager.getNamingAttr(ORGANIZATION));
        subOrgDNSB.append("=").append(subOrgName).append(",");
        subOrgDNSB.append(super.entryDN);

        AMOrganizationImpl subOrgImpl = new AMOrganizationImpl(super.token,
                subOrgDNSB.toString());
        Set domSet = new HashSet();
        domSet.add(domainName);
        if (attrMap == null) {
            attrMap = new HashMap();
        }
        attrMap.put("sunPreferredDomain", domSet);
        subOrgImpl.setAttributes(attrMap);
        if (serviceNamesAndAttrs != null && !serviceNamesAndAttrs.isEmpty()) {
            Set sNames = serviceNamesAndAttrs.keySet();
            subOrgImpl.setAttribute(SERVICE_STATUS_ATTRIBUTE, sNames);
            subOrgImpl.assignServices(serviceNamesAndAttrs, false);
        }
        subOrgImpl.create();
        return subOrgImpl.getDN();
    }

    /**
     * Deletes suborganizations
     * 
     * @param subOrganizations
     *            The set of suborganizations DN to be deleted.
     * 
     * @throws AMException
     *             if there is an internal error in the AM Store
     * @throws SSOException
     *             if the sign on is no longer valid
     */
    public void deleteSubOrganizations(Set subOrganizationDNs)
            throws AMException, SSOException {
        Iterator iter = subOrganizationDNs.iterator();

        while (iter.hasNext()) {
            String subOrganizationDN = (String) iter.next();
            AMOrganization org = new AMOrganizationImpl(super.token,
                    subOrganizationDN);
            org.delete();
        }
    }

    /**
     * Gets the sub-organizations by DN
     * 
     * @param dn
     *            DN
     * @return The sub Organization object
     */
    public AMOrganization getSubOrganization(String dn) throws AMException,
            SSOException {
        SSOTokenManager.getInstance().validateToken(super.token);
        return new AMOrganizationImpl(super.token, dn);
    }

    /**
     * Gets the suborganizations within the specified level.
     * 
     * @param level
     *            The search level starting from the organization.
     * @return The Set of suborganizations within the specified level.
     */
    public Set getSubOrganizations(int level) throws AMException, SSOException {
        return search(level, getSearchFilter(AMObject.ORGANIZATION));
    }

    /**
     * Gets number of suborganizations within the specified level.
     * 
     * @param level
     *            The search level starting from the organization.
     * @return Number of suborganizations within the specified level.
     */
    public long getNumberOfSubOrganizations(int level) throws AMException,
            SSOException {
        return getSubOrganizations(level).size();
    }

    /**
     * Searches for sub organizations in this organization using wildcards.
     * Wildcards can be specified such as a*, *, *a.
     * 
     * @param wildcard
     *            wildcard pattern to be used in the search
     * @param level
     *            the search level that needs to be used (AMConstants.SCOPE_ONE
     *            or AMConstansts.SCOPE_SUB)
     * 
     * @return Set Set of DNs of Sub Organizations matching the search
     * 
     * @throws AMException
     *             if there is an internal error in the AM Store
     * @throws SSOException
     *             if the sign on is no longer valid
     */
    public Set searchSubOrganizations(String wildcard, int level)
            throws AMException, SSOException {
        return searchSubOrganizations(wildcard, null, level);
    }

    /**
     * Searches for sub organizations in this organization using wildcards.
     * Wildcards can be specified such as a*, *, *a.
     * 
     * @param wildcard
     *            wildcard pattern to be used in the search
     * @param searchControl
     *            specifies the search scope to be used, VLV ranges etc.,
     * 
     * @return AMSearchResults which contains a Set of DNs of Sub Organizations
     *         matching the search
     * 
     * @throws AMException
     *             if there is an internal error in the AM Store
     * @throws SSOException
     *             if the sign on is no longer valid
     */
    public AMSearchResults searchSubOrganizations(String wildcard,
            AMSearchControl searchControl) throws AMException, SSOException {
        return searchSubOrganizations(wildcard, null, searchControl);
    }

    /**
     * Searches for sub organizations in this organization using wildcards and
     * attribute values. Wildcards can be specified such as a*, *, *a. To
     * further refine the search, attribute-value pairs can be specifed so that
     * DNs of sub organizations with matching attribute-value pairs will be
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
     * @return Set Set of DNs of sub organizations matching the search
     * 
     * @throws AMException
     *             if there is an internal error in the AM Store
     * @throws SSOException
     *             if the sign on is no longer valid
     */
    public Set searchSubOrganizations(String wildcard, Map avPairs, int level)
            throws AMException, SSOException {
        return searchObjects(AMNamingAttrManager.getNamingAttr(ORGANIZATION),
                getSearchFilter(AMObject.ORGANIZATION), wildcard, avPairs,
                level);
    }

    /**
     * Searches for sub organizations in this organization using wildcards and
     * attribute values. Wildcards can be specified such as a*, *, *a. To
     * further refine the search, attribute-value pairs can be specifed so that
     * DNs of sub organizations with matching attribute-value pairs will be
     * returned.
     * 
     * @param wildcard
     *            wildcard pattern to be used in the search
     * @param avPairs
     *            attribute-value pairs to match when searching users
     * @param searchControl
     *            specifies the search scope to be used, VLV ranges etc.,
     * 
     * @return AMSearchResults which contains a Set of DNs of sub organizations
     *         matching the search
     * 
     * @throws AMException
     *             if there is an internal error in the AM Store
     * @throws SSOException
     *             if the sign on is no longer valid
     */
    public AMSearchResults searchSubOrganizations(String wildcard, Map avPairs,
            AMSearchControl searchControl) throws AMException, SSOException {
        return searchObjects(AMNamingAttrManager.getNamingAttr(ORGANIZATION),
                getSearchFilter(AMObject.ORGANIZATION), wildcard, avPairs,
                searchControl);
    }

    /**
     * Creates organizational units.
     * 
     * @param organizationalUnits
     *            The set of organizational units names to be created.
     * @return Set set of sub OrganizationalUnit objects created.
     * 
     * @throws AMException
     *             if there is an internal error in the AM Store
     * @throws SSOException
     *             if the sign on is no longer valid
     */
    public Set createOrganizationalUnits(Set organizationalUnits)
            throws AMException, SSOException {
        Iterator iter = organizationalUnits.iterator();
        Set orgUnits = new HashSet();

        while (iter.hasNext()) {
            String orgUnitDN = AMNamingAttrManager
                    .getNamingAttr(ORGANIZATIONAL_UNIT)
                    + "=" + ((String) iter.next()) + "," + super.entryDN;

            AMOrganizationalUnitImpl orgUnitImpl = new AMOrganizationalUnitImpl(
                    super.token, orgUnitDN);

            orgUnitImpl.create();
            orgUnits.add(orgUnitImpl);
        }

        return orgUnits;
    }

    /**
     * Creates organizational units and initializes their attributes.
     * 
     * @param organizationalUnitsMap
     *            Map where the key is the name of the organizational unit, and
     *            the value is a Map represent Attribute-Value Pairs
     * @return Set set of OrganizationalUnit objects created.
     * @throws AMException
     *             if there is an internal error in the AM Store
     * @throws SSOException
     *             if the sign on is no longer valid
     */
    public Set createOrganizationalUnits(Map organizationalUnitsMap)
            throws AMException, SSOException {
        Iterator iter = organizationalUnitsMap.keySet().iterator();
        Set orgUnits = new HashSet();

        while (iter.hasNext()) {
            String orgUnitName = (String) iter.next();
            String orgUnitDN = AMNamingAttrManager
                    .getNamingAttr(ORGANIZATIONAL_UNIT)
                    + "=" + orgUnitName + "," + super.entryDN;

            Map attributes = (Map) organizationalUnitsMap.get(orgUnitName);
            AMOrganizationalUnitImpl orgUnitImpl = new AMOrganizationalUnitImpl(
                    super.token, orgUnitDN);
            orgUnitImpl.setAttributes(attributes);
            orgUnitImpl.create();
            orgUnits.add(orgUnitImpl);
        }

        return orgUnits;
    }

    /**
     * Deletes organizational units
     * 
     * @param organizationalUnitDNs
     *            The set of organizational units DNs to be deleted.
     * 
     * @throws AMException
     *             if there is an internal error in the AM Store
     * @throws SSOException
     *             if the sign on is no longer valid
     */
    public void deleteOrganizationalUnits(Set organizationalUnitDNs)
            throws AMException, SSOException {
        Iterator iter = organizationalUnitDNs.iterator();

        while (iter.hasNext()) {
            String organizationalUnitDN = (String) iter.next();
            AMOrganizationalUnit orgUnit = new AMOrganizationalUnitImpl(
                    super.token, organizationalUnitDN);
            orgUnit.delete();
        }
    }

    /**
     * Gets the organizational unit by DN
     * 
     * @param dn
     *            DN
     * @return The OrganizationalUnit object
     */
    public AMOrganizationalUnit getOrganizationalUnit(String dn)
            throws AMException, SSOException {
        SSOTokenManager.getInstance().validateToken(super.token);
        return new AMOrganizationalUnitImpl(super.token, dn);
    }

    /**
     * Gets the organizational units within the specified level.
     * 
     * @param level
     *            The search level starting from the organization.
     * @return The Set of organizational units DNs within the specified level.
     */
    public Set getOrganizationalUnits(int level) throws AMException,
            SSOException {
        return search(level, getSearchFilter(AMObject.ORGANIZATIONAL_UNIT));
    }

    /**
     * Gets number of organizational units within the specified level.
     * 
     * @param level
     *            The search level starting from the organization.
     * @return Number of organizational units within the specified level.
     */
    public long getNumberOfOrganizationalUnits(int level) throws AMException,
            SSOException {
        return getOrganizationalUnits(level).size();
    }

    /**
     * Searches for organizational units in this organization using wildcards.
     * Wildcards can be specified such as a*, *, *a.
     * 
     * @param wildcard
     *            wildcard pattern to be used in the search
     * @param level
     *            the search level that needs to be used (AMConstants.SCOPE_ONE
     *            or AMConstansts.SCOPE_SUB)
     * 
     * @return Set Set of DNs of organizational units matching the search
     * 
     * @throws AMException
     *             if there is an internal error in the AM Store
     * @throws SSOException
     *             if the sign on is no longer valid
     */
    public Set searchOrganizationalUnits(String wildcard, int level)
            throws AMException, SSOException {
        return searchOrganizationalUnits(wildcard, null, level);
    }

    /**
     * Searches for organizational units in this organization using wildcards.
     * Wildcards can be specified such as a*, *, *a.
     * 
     * @param wildcard
     *            wildcard pattern to be used in the search
     * @param searchControl
     *            specifies the search scope to be used, VLV ranges etc.,
     * 
     * @return AMSearchResults which contains a Set of DNs of organizational
     *         units matching the search
     * 
     * @throws AMException
     *             if there is an internal error in the AM Store
     * @throws SSOException
     *             if the sign on is no longer valid
     */
    public AMSearchResults searchOrganizationalUnits(String wildcard,
            AMSearchControl searchControl) throws AMException, SSOException {
        return searchOrganizationalUnits(wildcard, null, searchControl);
    }

    /**
     * Searches for organizational units in this organization using wildcards
     * and attribute values. Wildcards can be specified such as a*, *, *a. To
     * further refine the search, attribute-value pairs can be specifed so that
     * DNs of organizational units with matching attribute-value pairs will be
     * returned.
     * 
     * @param wildcard
     *            wildcard pattern to be used in the search
     * @param avPairs
     *            attribute-value pairs to match when searching organizational
     *            units
     * @param level
     *            the search level that needs to be used (AMConstants.SCOPE_ONE
     *            or AMConstansts.SCOPE_SUB)
     * 
     * @return Set Set of DNs of organizational units matching the search
     * 
     * @throws AMException
     *             if there is an internal error in the AM Store
     * @throws SSOException
     *             if the sign on is no longer valid
     */
    public Set searchOrganizationalUnits(String wildcard, Map avPairs, 
            int level) throws AMException, SSOException {
        return searchObjects(AMNamingAttrManager
                .getNamingAttr(ORGANIZATIONAL_UNIT),
                getSearchFilter(AMObject.ORGANIZATIONAL_UNIT), wildcard,
                avPairs, level);
    }

    /**
     * Searches for organizational units in this organization using wildcards
     * and attribute values. Wildcards can be specified such as a*, *, *a. To
     * further refine the search, attribute-value pairs can be specifed so that
     * DNs of organizational units with matching attribute-value pairs will be
     * returned.
     * 
     * @param wildcard
     *            wildcard pattern to be used in the search
     * @param avPairs
     *            attribute-value pairs to match when searching organizational
     *            units
     * @param searchControl
     *            specifies the search scope to be used, VLV ranges etc.,
     * 
     * @return AMSearchResults Set of DNs of organizational units matching the
     *         search
     * 
     * @throws AMException
     *             if there is an internal error in the AM Store
     * @throws SSOException
     *             if the sign on is no longer valid
     */
    public AMSearchResults searchOrganizationalUnits(String wildcard,
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
            StringBuilder roleDNSB = new StringBuilder();
            roleDNSB.append(AMNamingAttrManager.getNamingAttr(ROLE))
                    .append("=").append((String) iter.next()).append(",")
                    .append(super.entryDN);
            AMRoleImpl roleImpl = new AMRoleImpl(super.token, roleDNSB
                    .toString());
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

            Set aciSet = new HashSet();
            if (!attributes.isEmpty()) {
                aciSet = (Set) attributes.remove("iplanet-am-role-aci-list");
            }
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
     *            The search level starting from the organization.
     * @return The Set of role DN's within the specified level.
     */
    public Set getRoles(int level) throws AMException, SSOException {
        return search(level, getSearchFilter(AMObject.ROLE));
    }

    /**
     * Gets number of roles within the specified level.
     * 
     * @param level
     *            The search level starting from the organization.
     * @return Number of roles within the specified level.
     */
    public long getNumberOfRoles(int level) throws AMException, SSOException {
        return getRoles(level).size();
    }

    /**
     * Searches for roles in this organization using wildcards. Wildcards can be
     * specified such as a*, *, *a.
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
     * Searches for roles in this organization using wildcards. Wildcards can be
     * specified such as a*, *, *a.
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
     * Searches for roles in this organization using wildcards and attribute
     * values. Wildcards can be specified such as a*, *, *a. To further refine
     * the search, attribute-value pairs can be specifed so that DNs of roles
     * with matching attribute-value pairs will be returned.
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
     * Searches for roles in this organization using wildcards and attribute
     * values. Wildcards can be specified such as a*, *, *a. To further refine
     * the search, attribute-value pairs can be specifed so that DNs of roles
     * with matching attribute-value pairs will be returned.
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
     *            The search level starting from the organization.
     * @return The Set of filtered roles' DNs within the specified level.
     */
    public Set getFilteredRoles(int level) throws AMException, SSOException {
        return search(level, getSearchFilter(AMObject.FILTERED_ROLE));
    }

    /**
     * Gets number of filtered roles within the specified level.
     * 
     * @param level
     *            The search level starting from the organization.
     * @return Number of filtered roles within the specified level.
     */
    public long getNumberOfFilteredRoles(int level) throws AMException,
            SSOException {
        return getFilteredRoles(level).size();
    }

    /**
     * Searches for filtered roles in this organization using wildcards.
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
     * Searches for filtered roles in this organization using wildcards.
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
     * Searches for filtered roles in this organization using wildcards and
     * attribute values. Wildcards can be specified such as a*, *, *a. To
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
     * Searches for filtered roles in this organization using wildcards and
     * attribute values. Wildcards can be specified such as a*, *, *a. To
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
     * Searches for all roles in this organization using wildcards. Wildcards
     * can be specified such as a*, *, *a.
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
     * Searches for all roles in this organization using wildcards. Wildcards
     * can be specified such as a*, *, *a.
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
     * Searches for all roles in this organization using wildcards and attribute
     * values. Wildcards can be specified such as a*, *, *a. To further refine
     * the search, attribute-value pairs can be specifed so that DNs of all
     * roles with matching attribute-value pairs will be returned.
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
     * Searches for all roles in this organization using wildcards and attribute
     * values. Wildcards can be specified such as a*, *, *a. To further refine
     * the search, attribute-value pairs can be specifed so that DNs of all
     * roles with matching attribute-value pairs will be returned.
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
     *            The search level starting from the organization.
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
     *            The search level starting from the organization.
     * @return Number of assignable dynamic groups within the specified level.
     */
    public long getNumberOfAssignableDynamicGroups(int level)
            throws AMException, SSOException {
        return getAssignableDynamicGroups(level).size();
    }

    /**
     * Searches for assignable dynamic groups in this organization using
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
     * Searches for assignable dynamic groups in this organization using
     * wildcards. Wildcards can be specified such as a*, *, *a. Uses the
     * groupSearchTemplate, if provided. Otherwise the default search template
     * is used.
     * 
     * @param wildcard
     *            wildcard pattern to be used in the search
     * @param level
     *            the search level that needs to be used (AMConstants.SCOPE_ONE
     *            or AMConstants.SCOPE_SUB)
     * 
     * @return Set Set of DNs of assignable dynamic groups matching the search
     * 
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store
     * @throws SSOException
     *             if the sign on is no longer valid
     */

    public Set searchAssignableDynamicGroups(String wildcard, int level,
            String groupSearchTemplate, Map avPairs) throws AMException,
            SSOException {
        if (AMCompliance.isComplianceUserDeletionEnabled()
                && AMCompliance.isAncestorOrgDeleted(super.token,
                        super.entryDN, AMObject.ORGANIZATION)) {
            if (debug.warningEnabled()) {
                debug.warning("AMOrganization.searchAssDynGroup: "
                        + "ancestor org is deleted for: " + super.entryDN
                        + " :returning empty set");
            }
            return Collections.EMPTY_SET;
        }
        return searchObjects(AMNamingAttrManager.getNamingAttr(GROUP),
                getSearchFilter(AMObject.ASSIGNABLE_DYNAMIC_GROUP,
                        groupSearchTemplate), wildcard, avPairs, level);

    }

    /**
     * Searches for assignable dynamic groups in this organization using
     * wildcards. Wildcards can be specified such as a*, *, *a.
     * 
     * @param wildcard
     *            wildcard pattern to be used in the search
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
            AMSearchControl searchControl) throws AMException, SSOException {
        return searchAssignableDynamicGroups(wildcard, null, searchControl);
    }

    /**
     * Searches for assignable dynamic groups in this organization using
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
     * Searches for assignable dynamic groups in this organization using
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
     *            The search level starting from the organization.
     * @return The Set of DNs of DynamicGroups within the specified level.
     */
    public Set getDynamicGroups(int level) throws AMException, SSOException {
        return search(level, getSearchFilter(AMObject.DYNAMIC_GROUP));
    }

    /**
     * Gets number of dynamic groups within the specified level.
     * 
     * @param level
     *            The search level starting from the organization.
     * @return Number of dynamic groups within the specified level.
     */
    public long getNumberOfDynamicGroups(int level) throws AMException,
            SSOException {
        return getDynamicGroups(level).size();
    }

    /**
     * Searches for dynamic groups in this organization using wildcards.
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
     * Searches for dynamic groups in this organization using wildcards.
     * Wildcards can be specified such as a*, *, *a. Uses the
     * groupSearchTemplate, if provided. Otherwise uses the the default
     * GroupSearch template.
     * 
     * @param wildcard
     *            wildcard pattern to be used in the search
     * @param level
     *            the search level that needs to be used (AMConstants.SCOPE_ONE
     *            or AMConstants.SCOPE_SUB)
     * 
     * @return Set Set of DNs of dynamic groups matching the search
     * 
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store
     * @throws SSOException
     *             if the sign on is no longer valid
     */
    public Set searchDynamicGroups(String wildcard, int level,
            String groupSearchTemplate, Map avPairs) throws AMException,
            SSOException {
        if (AMCompliance.isComplianceUserDeletionEnabled()
                && AMCompliance.isAncestorOrgDeleted(super.token,
                        super.entryDN, AMObject.ORGANIZATION)) {
            if (debug.warningEnabled()) {
                debug.warning("AMOrganization.searchAssDynGroup: "
                        + "ancestor org is deleted for: " + super.entryDN
                        + " :returning empty set");
            }
            return Collections.EMPTY_SET;
        }
        return searchObjects(AMNamingAttrManager.getNamingAttr(GROUP),
                getSearchFilter(AMObject.DYNAMIC_GROUP, groupSearchTemplate),
                wildcard, avPairs, level);

    }

    /**
     * Searches for dynamic groups in this organization using wildcards.
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
     * Searches for dynamic groups in this organization using wildcards and
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
     * Searches for dynamic groups in this organization using wildcards and
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
            StringBuffer groupDNSB = new StringBuffer();
            groupDNSB.append(AMNamingAttrManager.getNamingAttr(GROUP)).append(
                    "=").append((String) iter.next()).append(",").append(
                    super.entryDN);
            AMStaticGroupImpl groupImpl = new AMStaticGroupImpl(super.token,
                    groupDNSB.toString());
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
     *            The search level starting from the organization.
     * @return The Set of static group DN's within the specified level.
     */
    public Set getStaticGroups(int level) throws AMException, SSOException {
        return search(level, getSearchFilter(AMObject.GROUP));
    }

    /**
     * Gets number of static groups within the specified level.
     * 
     * @param level
     *            The search level starting from the organization.
     * @return Number of static groups within the specified level.
     */
    public long getNumberOfStaticGroups(int level) throws AMException,
            SSOException {
        return getStaticGroups(level).size();
    }

    /**
     * Searches for static groups in this organization using wildcards.
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
     * Searches for static groups in this organization using wildcards.
     * Wildcards can be specified such as a*, *, *a. Uses the
     * groupSearchTemplate, if provided. If it is null, default search templates
     * are used.
     * 
     * @param wildcard
     *            wildcard pattern to be used in the search
     * @param level
     *            the search level that needs to be used (AMConstants.SCOPE_ONE
     *            or AMConstants.SCOPE_SUB)
     * 
     * @return Set Set of DNs of static groups matching the search
     * 
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store
     * @throws SSOException
     *             if the sign on is no longer valid
     */
    public Set searchStaticGroups(String wildcard, int level,
            String groupSearchTemplate, Map avPairs) throws AMException,
            SSOException {
        if (AMCompliance.isComplianceUserDeletionEnabled()
                && AMCompliance.isAncestorOrgDeleted(super.token,
                        super.entryDN, AMObject.ORGANIZATION)) {
            if (debug.messageEnabled()) {
                debug.message("AMOrganization.searchAssDynGroup: "
                        + "ancestor org is deleted for: " + super.entryDN
                        + " :returning empty set");
            }
            return Collections.EMPTY_SET;
        }
        return searchObjects(AMNamingAttrManager.getNamingAttr(GROUP),
                getSearchFilter(AMObject.GROUP, groupSearchTemplate), wildcard,
                avPairs, level);

    }

    /**
     * Searches for static groups in this organization using wildcards.
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
     * Searches for static groups in this organization using wildcards and
     * attribute values. Wildcards can be specified such as a*, *, *a. To
     * further refine the search, attribute-value pairs can be specifed so that
     * DNs of static groups with matching attribute-value pairs will be
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
     * Searches for static groups in this organization using wildcards and
     * attribute values. Wildcards can be specified such as a*, *, *a. To
     * further refine the search, attribute-value pairs can be specifed so that
     * DNs of static groups with matching attribute-value pairs will be
     * returned.
     * 
     * @param wildcard
     *            wildcard pattern to be used in the search
     * @param avPairs
     *            attribute-value pairs to match when searching users
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
     * Searches for groups in this organization using wildcards and attribute
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
     *            The search level starting from the organization.
     * @return The Set of PeopleContainer's within the specified level.
     */
    public Set getPeopleContainers(int level) throws AMException, SSOException {
        return search(level, getSearchFilter(AMObject.PEOPLE_CONTAINER));
    }

    /**
     * Gets number of people containers within the specified level.
     * 
     * @param level
     *            The search level starting from the organization.
     * @return Number of people containers within the specified level.
     */
    public long getNumberOfPeopleContainers(int level) throws AMException,
            SSOException {
        return getPeopleContainers(level).size();
    }

    /**
     * Searches for people containers in this organization using wildcards and
     * attribute values. Wildcards can be specified such as a*, *, *a.
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
     * Searches for people containers in this organization using wildcards and
     * attribute values. Wildcards can be specified such as a*, *, *a.
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
     * Searches for people containers in this organization using wildcards and
     * attribute values. Wildcards can be specified such as a*, *, *a. To
     * further refine the search, attribute-value pairs can be specifed so that
     * DNs of people containers with matching attribute-value pairs will be
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
     * Searches for people containers in this organization using wildcards and
     * attribute values. Wildcards can be specified such as a*, *, *a. To
     * further refine the search, attribute-value pairs can be specifed so that
     * DNs of people containers with matching attribute-value pairs will be
     * returned.
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
     *            The search level starting from the organization.
     * @return The Set of GroupContainer's within the specified level.
     */
    public Set getGroupContainers(int level) throws AMException, SSOException {
        return search(level, getSearchFilter(AMObject.GROUP_CONTAINER));
    }

    /**
     * Gets number of group containers within the specified level.
     * 
     * @param level
     *            The search level starting from the organization.
     * @return Number of group containers within the specified level.
     */
    public long getNumberOfGroupContainers(int level) throws AMException,
            SSOException {
        return getGroupContainers(level).size();
    }

    /**
     * Searches for group containers in this organization using wildcards and
     * attribute values. Wildcards can be specified such as a*, *, *a. To
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
     * Searches for group containers in this organization using wildcards and
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
    public AMSearchResults searchGroupContainers(String wildcard, Map avPairs,
            AMSearchControl searchControl) throws AMException, SSOException {
        return searchObjects(
                AMNamingAttrManager.getNamingAttr(GROUP_CONTAINER),
                getSearchFilter(AMObject.GROUP_CONTAINER), wildcard, avPairs,
                searchControl);

    }

    /**
     * Creates users in this organization. For each user the, object classes
     * specified by organization type attribute
     * <code>iplanet-am-required-services</code> of the service
     * <code>iPlanetAMAdminConsoleService</code> template are added. If a
     * corresponding template does not exist, the default values are picked up
     * from schema.
     * 
     * @param users
     *            The set of user names to be created in this organization.
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

        Set usersSet = new HashSet();
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
        Set serviceNames = getOrgTypeAttributes(ADMINISTRATION_SERVICE,
                REQUIRED_SERVICES_ATTR);
        if (serviceNames == Collections.EMPTY_SET) {
            serviceNames = new HashSet();
        }

        Set assignServiceNames = serviceNameAndAttrs.keySet();
        Set registered = dsServices.getRegisteredServiceNames(null, entryDN);
        Iterator it = assignServiceNames.iterator();
        while (it.hasNext()) {
            String tmpS = (String) it.next();
            if (!registered.contains(tmpS)) {
                Object[] args = { tmpS };
                throw new AMException(AMSDKBundle.getString("459", args,
                        super.locale), "459", args);
            }
        }

        // Verify if all these Services are registered with the organization
        // Otherwise throw an exception.
        it = assignServiceNames.iterator();
        while (it.hasNext()) {
            String tmp = (String) it.next();
            serviceNames.add(tmp);
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
                debug.error("AMOrganizationImpl: Data validation failed-> "
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
     * Removes users from the organization.
     * 
     * @param users
     *            The set of user DN's to be removed from the organization.
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
     * Gets the names (DNs) of users in the organization.
     * 
     * @return Set The names(DNs) of users in the organization.
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
     * Returns AMUser object of user in this organization (or in subrogs), whose
     * naming attribute exactly matches with uid. If userSearchTemplate is not
     * null, then this search template is used otherwise the
     * BasicUserSearchTemplate is used. Any %U in the search filter are replaced
     * with uid. If the search returns more than one user, an exception is
     * thrown because this is a violation of the namespace constraint.
     * 
     * @param String
     *            uid - namingAttribute value for user
     * @param String
     *            userSearchTemplate
     * @return AMUser object of user found.
     * @throws AMException
     * @throws SSOException
     */
    public AMUser getUser(String uid, String userSearchTemplate)
            throws AMException, SSOException {
        if (AMCompliance.isComplianceUserDeletionEnabled()
                && AMCompliance.isAncestorOrgDeleted(super.token,
                        super.entryDN, AMObject.ORGANIZATION)) {
            if (debug.messageEnabled()) {
                debug.message("AMOrganization.getUser: "
                        + "ancestor org is deleted for: " + super.entryDN
                        + " :returning null");
            }
            return null;
        }

        Set users = searchUsers(uid, AMConstants.SCOPE_SUB, userSearchTemplate,
                null);
        if (users.size() > 1) {
            Object args[] = { uid };
            throw new AMException(AMSDKBundle.getString("969", args,
                    super.locale), "969", args);
        } else {
            Iterator it = users.iterator();
            if (it.hasNext())
                return (new AMUserImpl(token, (String) it.next()));
        }
        return (null);
    }

    /**
     * Gets number of users within the specified level.
     * 
     * @param level
     *            The search level starting from the organization.
     * @return Number of users within the specified level.
     */
    public long getNumberOfUsers(int level) throws AMException, SSOException {
        return getUsers(level).size();
    }

    /**
     * Searches for users in this organization using wildcards and attribute
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
     * Searches for users in this organization using wildcards and attribute
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

    /**
     * Searches for users in this organization using wildcards and attribute
     * values. Wildcards can be specified such as a*, *, *a. SDK users the
     * userSearchTemplate, if provided. Otherwise, it uses the
     * BasicUserSearchTemplate. Any %U in the search template are replaced with
     * the wildcard.
     * 
     * @param wildcard
     *            wildcard pattern to be used in the search
     * @param level
     *            the search level that needs to be used (AMConstants.SCOPE_ONE
     *            or AMConstants.SCOPE_SUB)
     * 
     * @return Set DNs of Users matching the search
     * 
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store
     * @throws SSOException
     *             if the sign on is no longer valid
     */
    public Set searchUsers(String wildcard, int level,
            String userSearchTemplate, Map avPairs) throws AMException,
            SSOException {
        if (AMCompliance.isComplianceUserDeletionEnabled()
                && AMCompliance.isAncestorOrgDeleted(super.token,
                        super.entryDN, AMObject.ORGANIZATION)) {
            if (debug.warningEnabled()) {
                debug.warning("AMOrganization.searchAssDynGroup: "
                        + "ancestor org is deleted for: " + super.entryDN
                        + " :returning empty set");
            }
            return Collections.EMPTY_SET;
        }
        return searchObjects(AMNamingAttrManager.getNamingAttr(USER),
                getSearchFilter(AMObject.USER, userSearchTemplate), wildcard,
                avPairs, level);
    }

    public AMSearchResults searchUsers(String wildcard,
            AMSearchControl searchControl) throws AMException, SSOException {

        return searchUsers(wildcard, null, searchControl);
    }

    /**
     * Searches for users in this organization using wildcards and attribute
     * values. Wildcards can be specified such as a*, *, *a. To further refine
     * the search, attribute-value pairs can be specifed so that DNs of users
     * with matching attribute-value pairs will be returned.
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
     * Searches for users in this organization using wildcards and attribute
     * values. Wildcards can be specified such as a*, *, *a. To further refine
     * the search, attribute-value pairs can be specifed so that DNs of users
     * with matching attribute-value pairs will be returned.
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
     * Searches for users in this organization using wildcards and attribute
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
        return dsServices.getRegisteredServiceNames(super.token, 
            super.entryDN);
    }

    /**
     * Register a service for this organization.
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
    public synchronized void registerService(String serviceName,
            boolean createTemplate, boolean activate) throws AMException,
            SSOException {
        if (debug.messageEnabled()) {
            debug.message("AMOrganizationImpl.registerService(" + serviceName
                    + ", " + createTemplate + ", " + activate + ")");
        }

        try {
            dsServices.registerService(super.token, super.entryDN, serviceName);
        } catch (AMException le) {
            if (!le.getErrorCode().equalsIgnoreCase("464")) {
                throw le;
            }
        }

        Set serviceStatus = getAttribute(SERVICE_STATUS_ATTRIBUTE);
        if (!serviceStatus.equals(Collections.EMPTY_SET)) {
            Iterator iter = serviceStatus.iterator();
            while (iter.hasNext()) {
                String status = (String) iter.next();
                if (status.equalsIgnoreCase(serviceName)) {
                    // check if service is loaded by default during
                    // organization/realm creation
                    Set services = com.sun.identity.sm.ServiceManager
                            .servicesAssignedByDefault();
                    if (services != null && !services.isEmpty()) {
                        for (Iterator items = services.iterator(); items
                                .hasNext();) {
                            String dService = (String) items.next();
                            if (serviceName.toLowerCase().equals(
                                    dService.toLowerCase())) {
                                // Done for Comms in AM 7.0 since AuthService
                                // and AuthLDAPService will be automatically
                                // registered
                                return;
                            }
                        }
                    } else {
                        Object args[] = { serviceName };
                        throw new AMException(AMSDKBundle.getString("464",
                                args, super.locale), "464", args);
                    }
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
            // for existing DIT, Could be an object class violation, try
            // add sunManagedOrganization
            try {
                Set objectClass = getAttribute("objectclass");
                if (debug.messageEnabled()) {
                    debug.message("object class=" + objectClass);
                }
                if (!objectClass.contains("sunManagedOrganization")) {
                    objectClass.add("sunManagedOrganization");
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
     * Unregister a service for this organization.
     * 
     * @param String
     *            serviceName to be unregistered
     * @throws AMException
     *             if the service does not exist or could not be unregistered.
     * @throws SSOException
     *             if the sign on is no longer valid.
     */
    public synchronized void unregisterService(String serviceName)
            throws AMException, SSOException {
        if (debug.messageEnabled()) {
            debug.message("AMOrganizationImpl.unregisterService(" + serviceName
                    + ")");
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

                try {
                    dsServices.unRegisterService(super.token, super.entryDN,
                            super.profileType, serviceName,
                            AMTemplate.DYNAMIC_TEMPLATE);
                } catch (AMException le) {
                    if (debug.messageEnabled()) {
                        debug.message("AMOrganizationImpl.unregister "
                                + "exception in dsManager.unRegisterService"
                                + "DYNAMIC_TEMPLATE serviceName="
                                + serviceName);
                    }
                }
            }
            if (AMServiceUtils.serviceHasSubSchema(super.token, serviceName,
                    SchemaType.ORGANIZATION)) {
                if (orgTemplateExists(serviceName)) {
                    AMTemplate template = getTemplate(serviceName,
                            AMTemplate.ORGANIZATION_TEMPLATE);
                    dsServices.unRegisterService(super.token, super.entryDN,
                            super.profileType, serviceName,
                            AMTemplate.ORGANIZATION_TEMPLATE);
                    // Delete the template
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
                    break;
                }
            }
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
     * Unassigns the given policies from this organization and its roles.
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
     * Modifies all the templates under this organization that contain any
     * policyDN in given policyDNs.
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
     * Deletes all the named policy templates for this Organization
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
     * Gets all the assigned policies created for this Organization
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
     *            service name string
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
            subOrgs = searchSubOrganizations("*", avPair, SCOPE_SUB);
        } catch (AMException ae) {
            // ignore expection. sub-orgs may not be present
        }

        // Note: if the set size is 1, it implies that only this organization
        // was
        // returned as search result.
        if ((subOrgs != null) && (!subOrgs.isEmpty()) && (subOrgs.size() > 1)) {
            return true;
        } else {
            return false;
        }

    }

    protected AMGroup createGroup(String name, Map attributes,
            Map serviceNameAndAttrs, int type) 
        throws AMException, SSOException {
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
            Set registered = dsServices
                    .getRegisteredServiceNames(null, entryDN);
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
                }
                Object args[] = { thisService };
                throw new AMException(AMSDKBundle.getString("976", args,
                        super.locale), "976", args);
            }
            if (objectClasses != null && !objectClasses.isEmpty()) {
                groupImpl.setAttribute("objectclass", objectClasses);
            }
        }
        groupImpl.setAttributes(attributes);
        groupImpl.create();
        return ((AMGroup) groupImpl);

    }

    public AMSearchResults searchAssignableDynamicGroups(String wildcard,
            Map avPairs, String groupSearchTemplate,
            AMSearchControl searchControl) throws AMException, SSOException { 
        return searchObjects(AMNamingAttrManager.getNamingAttr(GROUP),
                getSearchFilter(AMObject.ASSIGNABLE_DYNAMIC_GROUP,
                        groupSearchTemplate), wildcard, avPairs, searchControl);
    }

    public AMSearchResults searchDynamicGroups(String wildcard, Map avPairs,
            String groupSearchTemplate, AMSearchControl searchControl)
            throws AMException, SSOException {
        return searchObjects(AMNamingAttrManager.getNamingAttr(GROUP),
                getSearchFilter(AMObject.DYNAMIC_GROUP, groupSearchTemplate),
                wildcard, avPairs, searchControl);
    }

    public AMSearchResults searchStaticGroups(String wildcard, Map avPairs,
            String groupSearchTemplate, AMSearchControl searchControl)
            throws AMException, SSOException {
        return searchObjects(AMNamingAttrManager.getNamingAttr(GROUP),
                getSearchFilter(AMObject.STATIC_GROUP, groupSearchTemplate),
                wildcard, avPairs, searchControl);
    }

    public AMSearchResults searchUsers(String wildcard, Map avPairs,
            String userSearchTemplate, AMSearchControl searchControl)
            throws AMException, SSOException {
        return searchObjects(AMNamingAttrManager.getNamingAttr(USER),
                getSearchFilter(AMObject.USER, userSearchTemplate), wildcard,
                avPairs, searchControl);
    }

    public Set createEntities(int type, Map entityNamesAndAttrs)
            throws AMException, SSOException {
        AMEntityType entityType = (AMEntityType) 
            AMCommonUtils.supportedEntitiesBasedOnType.get(
                    Integer.toString(type));
        AMStoreConnection amsc = new AMStoreConnection(token);
        String entityTypeName = amsc.getAMObjectName(type);
        String containerDN = entityType.getContainerRDN();
        int containerType = entityType.getContainerType();
        String createDN = entryDN;
        if (containerDN != null && containerDN.length() > 0) {
            String nString = AMNamingAttrManager.getNamingAttr(containerType);
            createDN = nString + "=" + containerDN + "," + entryDN;
        }
        switch (containerType) {
        case ORGANIZATIONAL_UNIT:
            AMOrganizationalUnit ou = amsc.getOrganizationalUnit(createDN);
            if (ou.isExists()) {
                Set supportedTypes = ou.getSupportedTypes();
                if (!supportedTypes.contains(entityTypeName)) {
                    supportedTypes.add(entityTypeName);
                    ou.setSupportedTypes(supportedTypes);
                }
            } else {
                Set supportedTypes = new HashSet();
                supportedTypes.add(entityTypeName);
                Map ouMap = new HashMap();
                Map ouAttrsMap = new HashMap();
                ouAttrsMap.put(AMConstants.CONTAINER_SUPPORTED_TYPES_ATTRIBUTE,
                        supportedTypes);
                ouMap.put(containerDN, ouAttrsMap);
                createOrganizationalUnits(ouMap);
                ou = amsc.getOrganizationalUnit(createDN);
            }
            return ou.createEntities(entityTypeName, entityNamesAndAttrs);
        case PEOPLE_CONTAINER:
            AMPeopleContainer pc = amsc.getPeopleContainer(createDN);
            if (pc.isExists()) {
                return pc.createEntities(entityTypeName, entityNamesAndAttrs);
            } else {
                Set pcSet = new HashSet();
                pcSet.add(containerDN);
                createPeopleContainers(pcSet);
                pc = amsc.getPeopleContainer(createDN);
            }
            return pc.createEntities(entityTypeName, entityNamesAndAttrs);
        case ORGANIZATION:
        default:
            return createEntitiesUnderOrg(type, entityNamesAndAttrs);
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.iplanet.am.sdk.AMOrganization#createEntities(int, java.util.Set)
     */
    public Set createEntities(int type, Set entityNames) throws AMException,
            SSOException {
        AMEntityType entityType = (AMEntityType) 
            AMCommonUtils.supportedEntitiesBasedOnType.get(
                    Integer.toString(type));
        AMStoreConnection amsc = new AMStoreConnection(token);
        String entityTypeName = amsc.getAMObjectName(type);
        String containerDN = entityType.getContainerRDN();
        int containerType = entityType.getContainerType();
        String createDN = entryDN;
        if (containerDN != null && containerDN.length() > 0) {
            String nString = AMNamingAttrManager.getNamingAttr(containerType);
            createDN = nString + "=" + containerDN + "," + entryDN;
        }
        switch (containerType) {
        case ORGANIZATIONAL_UNIT:
            AMOrganizationalUnit ou = amsc.getOrganizationalUnit(createDN);
            if (ou.isExists()) {
                Set supportedTypes = ou.getSupportedTypes();
                if (!supportedTypes.contains(entityTypeName)) {
                    supportedTypes.add(entityTypeName);
                    ou.setSupportedTypes(supportedTypes);
                }
            } else {
                Set supportedTypes = new HashSet();
                supportedTypes.add(entityTypeName);
                Map ouMap = new HashMap();
                Map ouAttrsMap = new HashMap();
                ouAttrsMap.put(AMConstants.CONTAINER_SUPPORTED_TYPES_ATTRIBUTE,
                        supportedTypes);
                ouMap.put(containerDN, ouAttrsMap);
                createOrganizationalUnits(ouMap);
                ou = amsc.getOrganizationalUnit(createDN);
            }
            return ou.createEntities(entityTypeName, entityNames);
        case PEOPLE_CONTAINER:
            AMPeopleContainer pc = amsc.getPeopleContainer(createDN);
            if (pc.isExists()) {
                return pc.createEntities(entityTypeName, entityNames);
            } else {
                Set pcSet = new HashSet();
                pcSet.add(containerDN);
                createPeopleContainers(pcSet);
                pc = amsc.getPeopleContainer(createDN);
            }
            return pc.createEntities(entityTypeName, entityNames);
        case ORGANIZATION:
        default:
            return createEntitiesUnderOrg(type, entityNames);
        }
    }

    public void deleteEntities(int type, Set entityDNs) throws AMException,
            SSOException {
        Iterator iter = entityDNs.iterator();
        while (iter.hasNext()) {
            String rDN = (String) iter.next();
            AMEntity resource = new AMEntityImpl(super.token, rDN);
            resource.delete();
        }

    }

    public Set searchEntities(int type, String wildcard, int scope, Map avPairs)
            throws AMException, SSOException {
        AMEntityType entityType = (AMEntityType)
            AMCommonUtils.supportedEntitiesBasedOnType.get(
                    Integer.toString(type));
        AMStoreConnection amsc = new AMStoreConnection(token);

        String containerDN = entityType.getContainerRDN();
        int containerType = entityType.getContainerType();
        String searchTemplate = entityType.getSearchTemplate();
        String searchDN = entryDN;
        if (containerDN != null && containerDN.length() > 0) {
            String nString = AMNamingAttrManager.getNamingAttr(containerType);
            searchDN = nString + "=" + containerDN + "," + entryDN;
        }
        switch (containerType) {
        case ORGANIZATIONAL_UNIT:
            AMOrganizationalUnit ou = amsc.getOrganizationalUnit(searchDN);
            if (ou.isExists()) {
                return ou.searchEntities(wildcard, AMConstants.SCOPE_ONE,
                        searchTemplate, avPairs);
            } else {
                return Collections.EMPTY_SET;
            }

        case PEOPLE_CONTAINER:
            AMPeopleContainer pc = amsc.getPeopleContainer(searchDN);
            if (pc.isExists()) {
                pc.searchEntities(wildcard, AMConstants.SCOPE_ONE,
                        searchTemplate, avPairs);
            } else {
                return Collections.EMPTY_SET;
            }

        case ORGANIZATION:
        default:
            return searchObjects(AMNamingAttrManager.getNamingAttr(USER),
                    getSearchFilter(AMObject.USER, searchTemplate), wildcard,
                    avPairs, scope);
        }

    }

    public AMSearchResults searchEntities(int type, String wildcard,
            Map avPairs, AMSearchControl ctrls) throws AMException,
            SSOException {
        AMEntityType entityType = (AMEntityType) 
            AMCommonUtils.supportedEntitiesBasedOnType.get(
                    Integer.toString(type));
        AMStoreConnection amsc = new AMStoreConnection(token);

        String containerDN = entityType.getContainerRDN();
        int containerType = entityType.getContainerType();
        String searchTemplate = entityType.getSearchTemplate();
        String searchDN = entryDN;
        if (containerDN != null && containerDN.length() > 0) {
            String nString = AMNamingAttrManager.getNamingAttr(containerType);
            searchDN = nString + "=" + containerDN + "," + entryDN;
        }

        switch (containerType) {
        case ORGANIZATIONAL_UNIT:
            AMOrganizationalUnit ou = amsc.getOrganizationalUnit(searchDN);
            if (ou.isExists()) {
                return ou.searchEntities(wildcard, avPairs, searchTemplate,
                        ctrls);
            } else {
                /*
                 * The very first time when entities gets selected from the
                 * Navigation menu of IS console, there will be no container in
                 * the directory. Only it gets created when a new entity gets
                 * created. So return null in this case, and console is checking
                 * for null and assigns Collections.EMPTY_SET to the entity
                 * display'.
                 */
                return null;
            }
        case PEOPLE_CONTAINER:
            AMPeopleContainer pc = amsc.getPeopleContainer(searchDN);
            if (pc.isExists()) {
                pc.searchEntities(wildcard, avPairs, searchTemplate, ctrls);
            } else {
                return null;
            }

        case ORGANIZATION:
        default:
            return searchObjects(AMNamingAttrManager.getNamingAttr(USER),
                    getSearchFilter(AMObject.USER, searchTemplate), wildcard,
                    avPairs, ctrls);
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.iplanet.am.sdk.AMOrganization#searchEntities(int,
     *      java.lang.String, java.lang.String,
     *      com.iplanet.am.sdk.AMSearchControl)
     */
    public AMSearchResults searchEntities(int type, String wildcard,
            String avfilter, AMSearchControl ctrl) throws AMException,
            SSOException {
        AMEntityType entityType = (AMEntityType) 
            AMCommonUtils.supportedEntitiesBasedOnType.get(
                    Integer.toString(type));
        AMStoreConnection amsc = new AMStoreConnection(token);

        String containerDN = entityType.getContainerRDN();
        int containerType = entityType.getContainerType();
        String searchTemplate = entityType.getSearchTemplate();
        String searchDN = entryDN;
        if (containerDN != null && containerDN.length() > 0) {
            String nString = AMNamingAttrManager.getNamingAttr(containerType);
            searchDN = nString + "=" + containerDN + "," + entryDN;
        }

        switch (containerType) {
        case ORGANIZATIONAL_UNIT:
            AMOrganizationalUnit ou = amsc.getOrganizationalUnit(searchDN);
            if (ou.isExists()) {
                return ou.searchEntities(wildcard, ctrl, avfilter,
                        searchTemplate);
            } else {
                throw new AMException(AMSDKBundle
                        .getString("461", super.locale), "461");
            }

        case PEOPLE_CONTAINER:
            AMPeopleContainer pc = amsc.getPeopleContainer(searchDN);
            if (pc.isExists()) {
                pc.searchEntities(wildcard, ctrl, avfilter, searchTemplate);
            } else {
                throw new AMException(AMSDKBundle
                        .getString("461", super.locale), "461");
            }

        case ORGANIZATION:
        default:
            return searchObjects(AMNamingAttrManager.getNamingAttr(USER),
                    getSearchFilter(AMObject.USER), wildcard, ctrl, avfilter);
        }
    }

    protected Set createEntitiesUnderOrg(int type, Map entityNamesAndAttrs)
            throws AMException, SSOException {
        
        if (debug.messageEnabled()) {
            debug.message("AMOrganizationImpl.createEntities enter.  " 
                    + "entityNamesAndAttrs=" + entityNamesAndAttrs);
        }
        String stype = (String) AMCommonUtils.supportedNames.get(Integer
                .toString(type));
        if (stype == null) {
            throw new AMException(AMSDKBundle.getString("117", super.locale),
                    "117");
        }
        if (type == AMObject.USER) {
            // create users.
            Set uSet = createUsers(entityNamesAndAttrs);
            Set resultSet = new HashSet();
            Iterator it = uSet.iterator();
            while (it.hasNext()) {
                AMUser u = (AMUser) it.next();
                AMEntity e = new AMEntityImpl(token, u.getDN());
                resultSet.add(e);
            }
            return resultSet;
        }
        Set entitySet = new HashSet();

        Iterator iter = entityNamesAndAttrs.keySet().iterator();
        while (iter.hasNext()) {
            String userName = (String) iter.next();
            String userDN = AMNamingAttrManager.getNamingAttr(type) + "="
                    + userName + "," + super.entryDN;
            AMEntityImpl user = new AMEntityImpl(super.token, userDN);
            Map userMap = (Map) entityNamesAndAttrs.get(userName);
            user.setAttributes(userMap);

            user.create(stype);
            entitySet.add(user);
        }
        return entitySet;
    }

    protected Set createEntitiesUnderOrg(int type, Set entityNames)
            throws AMException, SSOException {
        String stype = (String) AMCommonUtils.supportedNames.get(Integer
                .toString(type));
        if (stype == null) {
            throw new AMException(AMSDKBundle.getString("117", super.locale),
                    "117");
        }
        if (type == AMObject.USER) {
            // create users.
            Set uSet = createUsers(entityNames);
            Set resultSet = new HashSet();
            Iterator it = uSet.iterator();
            while (it.hasNext()) {
                AMUser u = (AMUser) it.next();
                AMEntity e = new AMEntityImpl(token, u.getDN());
                resultSet.add(e);
            }
            return resultSet;
        }
        Set entitySet = new HashSet();
        Iterator iter = entityNames.iterator();
        while (iter.hasNext()) {
            String userName = (String) iter.next();
            String userDN = AMNamingAttrManager.getNamingAttr(type) + "="
                    + userName + "," + super.entryDN;
            AMEntityImpl user = new AMEntityImpl(super.token, userDN);
            user.create(stype);
            entitySet.add(user);
        }
        return entitySet;
    }

    /**
     * Returns true if the organization is activated.
     * 
     * @return true if the organization is activated.
     * @throws AMException
     *             if there is an internal error in the AM Store.
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     */
    public boolean isActivated() throws AMException, SSOException {
        return getStringAttribute(statusAN).equalsIgnoreCase("active");
    }

}
