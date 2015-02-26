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
 * $Id: AMOrganizationalUnit.java,v 1.4 2008/06/25 05:41:21 qcheng Exp $
 *
 */

package com.iplanet.am.sdk;

import java.util.Map;
import java.util.Set;

import com.iplanet.sso.SSOException;

/**
 * <p>
 * The <code>AMOrganizationalUnit</code> interface provides methods to manage
 * organizational unit <code>AMOrganizationalUnit</code> objects can be
 * obtained by using <code>AMStoreConnection</code>. A handle to this object
 * can be obtained by using the DN of the object.
 * 
 * <PRE>
 * 
 * AMStoreConnection amsc = new AMStoreConnection(ssotoken); if
 * (amsc.doesEntryExist(oDN)) { AMOrganizationalUnit org =
 * amsc.getOrganizationalUnit(oDN); }
 * 
 * </PRE>
 *
 * @deprecated  As of Sun Java System Access Manager 7.1.
 * @supported.all.api
 */
public interface AMOrganizationalUnit extends AMObject {
    /**
     * Creates organizations.
     * 
     * @param organizations
     *            The set of organizations names to be created.
     * @return Set set of Organization objects created.
     * 
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store
     * @throws SSOException
     *             if the single sign on token is no longer valid
     */
    public Set createOrganizations(Set organizations) throws AMException,
            SSOException;

    /**
     * Creates organizations and initializes their attributes.
     * 
     * @param organizations
     *            Map where the key is the name of the organization, and the
     *            value is a Map to represent Attribute-Value Pairs
     * @return Set set of Organization objects created.
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store
     * @throws SSOException
     *             if the single sign on token is no longer valid
     */
    public Set createOrganizations(Map organizations) throws AMException,
            SSOException;

    /**
     * Deletes organizations
     * 
     * @param organizations
     *            The set of organizations DNs to be deleted.
     * 
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store.
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     */
    public void deleteOrganizations(Set organizations) throws AMException,
            SSOException;

    /**
     * Gets the organization by DN
     * 
     * @param dn
     *            DN
     * @return The Organization object
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store.
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     */
    public AMOrganization getOrganization(String dn) throws AMException,
            SSOException;

    /**
     * Gets the organizations within the specified level.
     * 
     * @param level
     *            The search level starting from the organizational unit.
     * @return The Set of organizations DNs within the specified level.
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store.
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     */
    public Set getOrganizations(int level) throws AMException, SSOException;

    /**
     * Gets number of organizations within the specified level.
     * 
     * @param level
     *            The search level starting from the organizational unit.
     * @return Number of organizations within the specified level.
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store.
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     */
    public long getNumberOfOrganizations(int level) throws AMException,
            SSOException;

    /**
     * Searches for organizations in this organizational unit using wildcards.
     * Wildcards can be specified such as a*, *, *a.
     * 
     * @param wildcard
     *            wildcard pattern to be used in the search
     * @param level
     *            the search level that needs to be used (
     *            <code>AMConstants.SCOPE_ONE</code>
     *            or <code>AMConstants.SCOPE_SUB</code>)
     * @return Set Set of DNs of organizations matching the search
     * 
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store
     * @throws SSOException
     *             if the single sign on token is no longer valid
     */
    public Set searchOrganizations(String wildcard, int level)
            throws AMException, SSOException;

    /**
     * Searches for organizations in this organizational unit using wildcards.
     * Wildcards can be specified such as a*, *, *a.
     * 
     * @param wildcard
     *            wildcard pattern to be used in the search
     * @param searchControl
     *            specifies the search scope to be used, VLV ranges etc.
     * @return <code>AMSearchResults</code> which contains a Set of DNs of
     *         organizations matching the search.
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store.
     * @throws SSOException
     *             if the single sign on token is no longer valid
     */
    public AMSearchResults searchOrganizations(String wildcard,
            AMSearchControl searchControl) throws AMException, SSOException;

    /**
     * Searches for organizations in this organizational unit using wildcards
     * and attribute values. Wildcards can be specified such as a*, *, *a. To
     * further refine the search, attribute-value pairs can be specified so that
     * DNs of organizations with matching attribute-value pairs will be
     * returned.
     * 
     * @param wildcard
     *            wildcard pattern to be used in the search
     * @param avPairs
     *            attribute-value pairs to match when searching organizations
     * @param level
     *            the search level that needs to be used (
     *            <code>AMConstants.SCOPE_ONE</code>
     *            or <code>AMConstants.SCOPE_SUB</code>)
     * @return Set Set of DNs of organizations matching the search
     * 
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store
     * @throws SSOException
     *             if the single sign on token is no longer valid
     */
    public Set searchOrganizations(String wildcard, Map avPairs, int level)
            throws AMException, SSOException;

    /**
     * Searches for organizations in this organizational unit using wildcards
     * and attribute values. Wildcards can be specified such as a*, *, *a. To
     * further refine the search, attribute-value pairs can be specified so that
     * DNs of organizations with matching attribute-value pairs will be
     * returned.
     * 
     * @param wildcard
     *            wildcard pattern to be used in the search.
     * @param avPairs
     *            attribute-value pairs to match when searching organizations.
     * @param searchControl
     *            specifies the search scope to be used, VLV ranges etc.
     * @return <code>AMSearchResults</code> which contains a Set of DNs of
     *         organizations matching the search.
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store.
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     */
    public AMSearchResults searchOrganizations(String wildcard, Map avPairs,
            AMSearchControl searchControl) throws AMException, SSOException;

    /**
     * Creates sub-organizational units.
     * 
     * @param subOrganizationalUnits
     *            The set of sub-organizational units names to be created.
     * @return Set set of sub organizational unit objects created.
     * 
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store
     * @throws SSOException
     *             if the single sign on token is no longer valid
     */
    public Set createSubOrganizationalUnits(Set subOrganizationalUnits)
            throws AMException, SSOException;

    /**
     * Creates sub-organizational units and initializes their attributes.
     * 
     * @param subOrganizationalUnits
     *            Map where the key is the name of the sub organizational unit,
     *            and the value is a Map to represent Attribute-Value Pairs.
     * @return Set set of sub organizational unit objects created.
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store
     * @throws SSOException
     *             if the single sign on token is no longer valid
     */
    public Set createSubOrganizationalUnits(Map subOrganizationalUnits)
            throws AMException, SSOException;

    /**
     * Deletes sub organizational units
     * 
     * @param subOrganizationalUnits
     *            The set of sub organizational units DNs to be deleted.
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store
     * @throws SSOException
     *             if the single sign on token is no longer valid
     */
    public void deleteSubOrganizationalUnits(Set subOrganizationalUnits)
            throws AMException, SSOException;

    /**
     * Gets the sub-organizational unit by DN
     * 
     * @param dn
     *            distinguished name.
     * @return The sub <code>OrganizationalUnit</code> object.
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store
     * @throws SSOException
     *             if the single sign on token is no longer valid
     */
    public AMOrganizationalUnit getSubOrganizationalUnit(String dn)
            throws AMException, SSOException;

    /**
     * Gets the sub organizational units within the specified level.
     * 
     * @param level
     *            The search level starting from the organizational unit.
     * @return Set of sub organizational units DNs within the specified level.
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store
     * @throws SSOException
     *             if the single sign on token is no longer valid
     */
    public Set getSubOrganizationalUnits(int level) throws AMException,
            SSOException;

    /**
     * Gets number of sub organizational units within the specified level.
     * 
     * @param level
     *            The search level starting from the organizational unit.
     * @return Number of sub organizational units within the specified level.
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store
     * @throws SSOException
     *             if the single sign on token is no longer valid
     */
    public long getNumberOfSubOrganizationalUnits(int level)
            throws AMException, SSOException;

    /**
     * Searches for sub organizational units in this organizational unit using
     * wildcards. Wildcards can be specified such as a*, *, *a.
     * 
     * @param wildcard
     *            wildcard pattern to be used in the search
     * @param level
     *            the search level that needs to be used (
     *            <code>AMConstants.SCOPE_ONE</code>
     *            or <code>AMConstants.SCOPE_SUB</code>)
     * @return Set Set of DNs of sub organizational units matching the search
     * 
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store
     * @throws SSOException
     *             if the single sign on token is no longer valid
     */
    public Set searchSubOrganizationalUnits(String wildcard, int level)
            throws AMException, SSOException;

    /**
     * Searches for sub organizational units in this organizational unit using
     * wildcards. Wildcards can be specified such as a*, *, *a.
     * 
     * @param wildcard
     *            wildcard pattern to be used in the search.
     * @param searchControl
     *            specifies the search scope to be used, VLV ranges etc.
     * @return <code>AMSearchResults</code> which contains a Set of DNs of sub
     *         organizational units matching the search.
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store.
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     */
    public AMSearchResults searchSubOrganizationalUnits(String wildcard,
            AMSearchControl searchControl) throws AMException, SSOException;

    /**
     * Searches for sub organizational units in this organizational unit using
     * wildcards and attribute values. Wildcards can be specified such as a*, *,
     * *a. To further refine the search, attribute-value pairs can be specified
     * so that DNs of sub organizations with matching attribute-value pairs will
     * be returned.
     * 
     * @param wildcard
     *            wildcard pattern to be used in the search
     * @param avPairs
     *            attribute-value pairs to match when searching sub
     *            organizational units
     * @param level
     *            the search level that needs to be used (
     *            <code>AMConstants.SCOPE_ONE</code>
     *            or <code>AMConstants.SCOPE_SUB</code>)
     * @return Set Set of DNs of sub organizational units matching the search
     * 
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store
     * @throws SSOException
     *             if the single sign on token is no longer valid
     */
    public Set searchSubOrganizationalUnits(String wildcard, Map avPairs,
            int level) throws AMException, SSOException;

    /**
     * Searches for sub organizational units in this organizational unit using
     * wildcards and attribute values. Wildcards can be specified such as a*, *,
     * *a. To further refine the search, attribute-value pairs can be specified
     * so that DNs of sub organizations with matching attribute-value pairs will
     * be returned.
     * 
     * @param wildcard
     *            wildcard pattern to be used in the search.
     * @param avPairs
     *            attribute-value pairs to match when searching sub
     *            organizational units.
     * @param searchControl
     *            specifies the search scope to be used, VLV ranges etc.
     * @return <code>AMSearchResults</code> which contains a Set of DNs of sub
     *         organizational units matching the search.
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store.
     * @throws SSOException
     *             if the single sign on token is no longer valid
     */
    public AMSearchResults searchSubOrganizationalUnits(String wildcard,
            Map avPairs, AMSearchControl searchControl) throws AMException,
            SSOException;

    /**
     * Creates roles.
     * 
     * @param roles
     *            The set of Roles' names to be created.
     * @return Set set of Role objects created.
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store
     * @throws SSOException
     *             if the single sign on token is no longer valid
     */
    public Set createRoles(Set roles) throws AMException, SSOException;

    /**
     * Creates roles.
     * 
     * @param roles
     *            Map where the key is the name of the role, and the value is a
     *            Map to represent Attribute-Value Pairs
     * @return Set set of Role objects created.
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store
     * @throws SSOException
     *             if the single sign on token is no longer valid
     */
    public Set createRoles(Map roles) throws AMException, SSOException;

    /**
     * Deletes roles.
     * 
     * @param roles
     *            The set of roles' DNs to be deleted.
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store
     * @throws SSOException
     *             if the single sign on token is no longer valid
     */
    public void deleteRoles(Set roles) throws AMException, SSOException;

    /**
     * Gets the roles within the specified level.
     * 
     * @param level
     *            The search level starting from the organizational unit.
     * @return The Set of Roles' DNs within the specified level.
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store
     * @throws SSOException
     *             if the single sign on token is no longer valid
     */
    public Set getRoles(int level) throws AMException, SSOException;

    /**
     * Gets number of roles within the specified level.
     * 
     * @param level
     *            The search level starting from the organizational unit.
     * @return Number of roles within the specified level.
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store
     * @throws SSOException
     *             if the single sign on token is no longer valid
     */
    public long getNumberOfRoles(int level) throws AMException, SSOException;

    /**
     * Searches for roles in this organizational unit using wildcards. Wildcards
     * can be specified such as a*, *, *a.
     * 
     * @param wildcard
     *            wildcard pattern to be used in the search
     * @param level
     *            the search level that needs to be used (
     *            <code>AMConstants.SCOPE_ONE</code>
     *            or <code>AMConstants.SCOPE_SUB</code>)
     * @return Set Set of DNs of roles matching the search
     * 
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store
     * @throws SSOException
     *             if the single sign on token is no longer valid
     */
    public Set searchRoles(String wildcard, int level) throws AMException,
            SSOException;

    /**
     * Searches for roles in this organizational unit using wildcards. Wildcards
     * can be specified such as a*, *, *a.
     * 
     * @param wildcard
     *            wildcard pattern to be used in the search.
     * @param searchControl
     *            specifies the search scope to be used, VLV ranges etc.
     * @return <code>AMSearchResults</code> which contains a Set of DNs of
     *         roles matching the search.
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store.
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     */
    public AMSearchResults searchRoles(String wildcard,
            AMSearchControl searchControl) throws AMException, SSOException;

    /**
     * Searches for roles in this organizational unit using wildcards and
     * attribute values. Wildcards can be specified such as a*, *, *a. To
     * further refine the search, attribute-value pairs can be specified so that
     * DNs of roles with matching attribute-value pairs will be returned.
     * 
     * @param wildcard
     *            wildcard pattern to be used in the search
     * @param avPairs
     *            attribute-value pairs to match when searching roles
     * @param level
     *            the search level that needs to be used (
     *            <code>AMConstants.SCOPE_ONE</code>
     *            or <code>AMConstants.SCOPE_SUB</code>)
     * @return Set Set of DNs of roles matching the search
     * 
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store
     * @throws SSOException
     *             if the single sign on token is no longer valid
     */
    public Set searchRoles(String wildcard, Map avPairs, int level)
            throws AMException, SSOException;

    /**
     * Searches for roles in this organizational unit using wildcards and
     * attribute values. Wildcards can be specified such as a*, *, *a. To
     * further refine the search, attribute-value pairs can be specified so that
     * DNs of roles with matching attribute-value pairs will be returned.
     * 
     * @param wildcard
     *            wildcard pattern to be used in the search.
     * @param avPairs
     *            attribute-value pairs to match when searching roles
     * @param searchControl
     *            specifies the search scope to be used, VLV ranges etc.
     * @return <code>AMSearchResults</code> which contains a Set of DNs of
     *         roles matching the search.
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store.
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     */
    public AMSearchResults searchRoles(String wildcard, Map avPairs,
            AMSearchControl searchControl) throws AMException, SSOException;

    /**
     * Creates filtered roles.
     * 
     * @param roles
     *            The set of filtered roles' names to be created.
     * @return set of <code>FilteredRole</code> objects created.
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store.
     * @throws SSOException
     *             if the single sign on token is no longer valid
     */
    public Set createFilteredRoles(Set roles) throws AMException, SSOException;

    /**
     * Creates filtered roles.
     * 
     * @param roles
     *            Map where the key is the name of the filtered role, and the
     *            value is a Map to represent Attribute-Value Pairs
     * @return set of <code>FilteredRole</code> objects created.
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store.
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     */
    public Set createFilteredRoles(Map roles) throws AMException, SSOException;

    /**
     * Deletes filtered roles.
     * 
     * @param roles
     *            The set of filtered roles' DNs to be deleted.
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store
     * @throws SSOException
     *             if the single sign on token is no longer valid
     */
    public void deleteFilteredRoles(Set roles) throws AMException, SSOException;

    /**
     * Gets the filtered roles within the specified level.
     * 
     * @param level
     *            The search level starting from the organization.
     * @return The Set of filtered roles' DNs within the specified level.
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store
     * @throws SSOException
     *             if the single sign on token is no longer valid
     */
    public Set getFilteredRoles(int level) throws AMException, SSOException;

    /**
     * Gets number of filtered roles within the specified level.
     * 
     * @param level
     *            The search level starting from the organization.
     * @return Number of filtered roles within the specified level.
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store
     * @throws SSOException
     *             if the single sign on token is no longer valid
     */
    public long getNumberOfFilteredRoles(int level) throws AMException,
            SSOException;

    /**
     * Searches for filtered roles in this organization using wildcards.
     * Wildcards can be specified such as a*, *, *a.
     * 
     * @param wildcard
     *            wildcard pattern to be used in the search
     * @param level
     *            the search level that needs to be used (
     *            <code>AMConstants.SCOPE_ONE</code>
     *            or <code>AMConstants.SCOPE_SUB</code>)
     * @return Set of DNs of filtered roles matching the search
     * 
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store
     * @throws SSOException
     *             if the single sign on token is no longer valid
     */
    public Set searchFilteredRoles(String wildcard, int level)
            throws AMException, SSOException;

    /**
     * Searches for filtered roles in this organization using wildcards.
     * Wildcards can be specified such as a*, *, *a.
     * 
     * @param wildcard
     *            wildcard pattern to be used in the search
     * @param searchControl
     *            specifies the search scope to be used, VLV ranges etc.
     * @return <code>AMSearchResults</code> which contains a Set of DNs of
     *         filtered roles matching the search.
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store.
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     */
    public AMSearchResults searchFilteredRoles(String wildcard,
            AMSearchControl searchControl) throws AMException, SSOException;

    /**
     * Searches for filtered roles in this organization using wildcards and
     * attribute values. Wildcards can be specified such as a*, *, *a. To
     * further refine the search, attribute-value pairs can be specified so that
     * DNs of filtered roles with matching attribute-value pairs will be
     * returned.
     * 
     * @param wildcard
     *            wildcard pattern to be used in the search
     * @param avPairs
     *            attribute-value pairs to match when searching filtered roles
     * @param level
     *            the search level that needs to be used (
     *            <code>AMConstants.SCOPE_ONE</code>
     *            or <code>AMConstants.SCOPE_SUB</code>).
     * @return Set of DNs of filtered roles matching the search
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store
     * @throws SSOException
     *             if the single sign on token is no longer valid
     */
    public Set searchFilteredRoles(String wildcard, Map avPairs, int level)
            throws AMException, SSOException;

    /**
     * Searches for filtered roles in this organization using wildcards and
     * attribute values. Wildcards can be specified such as a*, *, *a. To
     * further refine the search, attribute-value pairs can be specified so that
     * DNs of filtered roles with matching attribute-value pairs will be
     * returned.
     * 
     * @param wildcard
     *            wildcard pattern to be used in the search.
     * @param avPairs
     *            attribute-value pairs to match when searching filtered roles.
     * @param searchControl
     *            specifies the search scope to be used, VLV ranges etc.
     * @return <code>AMSearchResults</code> which contains a Set of DNs of
     *         filtered roles matching the search.
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store.
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     */
    public AMSearchResults searchFilteredRoles(String wildcard, Map avPairs,
            AMSearchControl searchControl) throws AMException, SSOException;

    /**
     * Searches for all roles in this organizational unit using wildcards.
     * Wildcards can be specified such as a*, *, *a.
     * 
     * @param wildcard
     *            wildcard pattern to be used in the search
     * @param level
     *            the search level that needs to be used (
     *            <code>AMConstants.SCOPE_ONE</code>
     *            or <code>AMConstants.SCOPE_SUB</code>)
     * @return Set of DNs of all roles matching the search
     * 
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store
     * @throws SSOException
     *             if the single sign on token is no longer valid
     */
    public Set searchAllRoles(String wildcard, int level) throws AMException,
            SSOException;

    /**
     * Searches for all roles in this organizational unit using wildcards.
     * Wildcards can be specified such as a*, *, *a.
     * 
     * @param wildcard
     *            wildcard pattern to be used in the search.
     * @param searchControl
     *            specifies the search scope to be used, VLV ranges etc.
     * @return <code>AMSearchResults</code> which contains a Set of DNs of all
     *         roles matching the search.
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store.
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     */
    public AMSearchResults searchAllRoles(String wildcard,
            AMSearchControl searchControl) throws AMException, SSOException;

    /**
     * Searches for all roles in this organizational unit using wildcards and
     * attribute values. Wildcards can be specified such as a*, *, *a. To
     * further refine the search, attribute-value pairs can be specified so that
     * DNs of all roles with matching attribute-value pairs will be returned.
     * 
     * @param wildcard
     *            wildcard pattern to be used in the search
     * @param avPairs
     *            attribute-value pairs to match when searching all roles
     * @param level
     *            the search level that needs to be used (
     *            <code>AMConstants.SCOPE_ONE</code>
     *            or <code>AMConstants.SCOPE_SUB</code>)
     * @return Set of DNs of all roles matching the search
     * 
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store
     * @throws SSOException
     *             if the single sign on token is no longer valid
     */
    public Set searchAllRoles(String wildcard, Map avPairs, int level)
            throws AMException, SSOException;

    /**
     * Searches for all roles in this organizational unit using wildcards and
     * attribute values. Wildcards can be specified such as a*, *, *a. To
     * further refine the search, attribute-value pairs can be specified so that
     * DNs of all roles with matching attribute-value pairs will be returned.
     * 
     * @param wildcard
     *            wildcard pattern to be used in the search.
     * @param avPairs
     *            attribute-value pairs to match when searching all roles
     * @param searchControl
     *            specifies the search scope to be used, VLV ranges etc.,
     * @return <code>AMSearchResults</code> which contains a Set of DNs of all
     *         roles matching the search.
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store.
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     */
    public AMSearchResults searchAllRoles(String wildcard, Map avPairs,
            AMSearchControl searchControl) throws AMException, SSOException;

    /**
     * Creates assignable dynamic groups.
     * 
     * @param assignableDynamicGroups
     *            The set of assignable dynamic groups's names to be created.
     * @return set of <code>AssignableDynamicGroup</code> objects created.
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store
     * @throws SSOException
     *             if the single sign on token is no longer valid
     */
    public Set createAssignableDynamicGroups(Set assignableDynamicGroups)
            throws AMException, SSOException;

    /**
     * Deletes assignable dynamic groups.
     * 
     * @param assignableDynamicGroups
     *            The set of assignable dynamic groups's DNs to be deleted.
     * 
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store
     * @throws SSOException
     *             if the single sign on token is no longer valid
     */
    public void deleteAssignableDynamicGroups(Set assignableDynamicGroups)
            throws AMException, SSOException;

    /**
     * Returns the assignable dynamic groups within the specified level.
     * 
     * @param level
     *            The search level starting from the organizational unit.
     * @return The Set of DNs of <code>AssignableDynamicGroups</code> within
     *         the specified level.
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store
     * @throws SSOException
     *             if the single sign on token is no longer valid
     */
    public Set getAssignableDynamicGroups(int level) throws AMException,
            SSOException;

    /**
     * Gets number of assignable dynamic groups within the specified level.
     * 
     * @param level
     *            The search level starting from the organizational unit.
     * @return Number of assignable dynamic groups within the specified level.
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store
     * @throws SSOException
     *             if the single sign on token is no longer valid
     */
    public long getNumberOfAssignableDynamicGroups(int level)
            throws AMException, SSOException;

    /**
     * Searches for assignable dynamic groups in this organizational unit using
     * wildcards. Wildcards can be specified such as a*, *, *a.
     * 
     * @param wildcard
     *            wildcard pattern to be used in the search
     * @param level
     *            the search level that needs to be used (
     *            <code>AMConstants.SCOPE_ONE</code>
     *            or <code>AMConstants.SCOPE_SUB</code>)
     * @return Set Set of DNs of assignable dynamic groups matching the search
     * 
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store
     * @throws SSOException
     *             if the single sign on token is no longer valid
     */
    public Set searchAssignableDynamicGroups(String wildcard, int level)
            throws AMException, SSOException;

    /**
     * Searches for assignable dynamic groups in this organizational unit using
     * wildcards. Wildcards can be specified such as a*, *, *a.
     * 
     * @param wildcard
     *            wildcard pattern to be used in the search.
     * @param searchControl
     *            specifies the search scope to be used, VLV ranges etc.
     * @return <code>AMSearchResults</code> Set of DNs of assignable dynamic
     *         groups matching the search.
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store.
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     */
    public AMSearchResults searchAssignableDynamicGroups(String wildcard,
            AMSearchControl searchControl) throws AMException, SSOException;

    /**
     * Searches for assignable dynamic groups in this organizational unit using
     * wildcards and attribute values. Wildcards can be specified such as a*, *,
     * *a. To further refine the search, attribute-value pairs can be specified
     * so that DNs of dynamic groups with matching attribute-value pairs will be
     * returned.
     * 
     * @param wildcard
     *            wildcard pattern to be used in the search
     * @param avPairs
     *            attribute-value pairs to match when searching assignable
     *            dynamic groups
     * @param level
     *            the search level that needs to be used (
     *            <code>AMConstants.SCOPE_ONE</code>
     *            or <code>AMConstants.SCOPE_SUB</code>)
     * @return Set Set of DNs of assignable dynamic groups matching the search
     * 
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store
     * @throws SSOException
     *             if the single sign on token is no longer valid
     */
    public Set searchAssignableDynamicGroups(String wildcard, Map avPairs,
            int level) throws AMException, SSOException;

    /**
     * Searches for assignable dynamic groups in this organizational unit using
     * wildcards and attribute values. Wildcards can be specified such as a*, *,
     * *a. To further refine the search, attribute-value pairs can be specified
     * so that DNs of dynamic groups with matching attribute-value pairs will be
     * returned.
     * 
     * @param wildcard
     *            wildcard pattern to be used in the search
     * @param avPairs
     *            attribute-value pairs to match when searching assignable
     *            dynamic groups
     * @param searchControl
     *            specifies the search scope to be used, VLV ranges etc.
     * @return <code>AMSearchResults</code> which contains a Set of DNs of
     *         assignable dynamic groups matching the search.
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store.
     * @throws SSOException
     *             if the single sign on token is no longer valid
     */
    public AMSearchResults searchAssignableDynamicGroups(String wildcard,
            Map avPairs, AMSearchControl searchControl) throws AMException,
            SSOException;

    /**
     * Creates dynamic groups and initializes their attributes.
     * 
     * @param dynamicGroups
     *            Map where the key is the name of the dynamic group, and the
     *            value is a Map to represent Attribute-Value Pairs.
     * @return Set of <code>AMDynamicGroup</code> objects created
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store.
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     */
    public Set createDynamicGroups(Map dynamicGroups) throws AMException,
            SSOException;

    /**
     * Deletes dynamic groups.
     * 
     * @param dynamicGroups
     *            The set of dynamic groups's DNs to be deleted.
     * @throws AMException
     *             if there is an internal error in the access management data
     *             store.
     * @throws SSOException
     *             if the single sign on token is no longer valid
     */
    public void deleteDynamicGroups(Set dynamicGroups) throws AMException,
            SSOException;

    /**
     * Gets the dynamic groups within the specified level.
     * 
     * @param level
     *            The search level starting from the organization unit.
     * @return The Set of DNs of dynamic groups within the specified level.
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store
     * @throws SSOException
     *             if the single sign on token is no longer valid
     */
    public Set getDynamicGroups(int level) throws AMException, SSOException;

    /**
     * Gets number of dynamic groups within the specified level.
     * 
     * @param level
     *            The search level starting from the organization unit.
     * @return Number of dynamic groups within the specified level.
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store
     * @throws SSOException
     *             if the single sign on token is no longer valid
     */
    public long getNumberOfDynamicGroups(int level) throws AMException,
            SSOException;

    /**
     * Searches for dynamic groups in this organization unit using wildcards.
     * Wildcards can be specified such as a*, *, *a.
     * 
     * @param wildcard
     *            wildcard pattern to be used in the search
     * @param level
     *            the search level that needs to be used (
     *            <code>AMConstants.SCOPE_ONE</code>
     *            or <code>AMConstants.SCOPE_SUB</code>)
     * @return Set Set of DNs of dynamic groups matching the search
     * @throws AMException
     *             if there is an internal error in the access management data
     *             store
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     */
    public Set searchDynamicGroups(String wildcard, int level)
            throws AMException, SSOException;

    /**
     * Searches for dynamic groups in this organization unit using wildcards.
     * Wildcards can be specified such as a*, *, *a.
     * 
     * @param wildcard
     *            wildcard pattern to be used in the search
     * @param searchControl
     *            specifies the search scope to be used, VLV ranges etc.
     * @return <code>AMSearchResults</code> which contains a Set of DNs of
     *         dynamic groups matching the search.
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store.
     * @throws SSOException
     *             if the single sign on token is no longer valid
     */
    public AMSearchResults searchDynamicGroups(String wildcard,
            AMSearchControl searchControl) throws AMException, SSOException;

    /**
     * Searches for dynamic groups in this organization unit using wildcards and
     * attribute values. Wildcards can be specified such as a*, *, *a. To
     * further refine the search, attribute-value pairs can be specified so that
     * DNs of dynamic groups with matching attribute-value pairs will be
     * returned.
     * 
     * @param wildcard
     *            wildcard pattern to be used in the search
     * @param avPairs
     *            attribute-value pairs to match when searching dynamic groups.
     * @param level
     *            the search level that needs to be used (
     *            <code>AMConstants.SCOPE_ONE</code>
     *            or <code>AMConstants.SCOPE_SUB</code>).
     * @return Set of DNs of dynamic groups matching the search
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store.
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     */
    public Set searchDynamicGroups(String wildcard, Map avPairs, int level)
            throws AMException, SSOException;

    /**
     * Searches for dynamic groups in this organization unit using wildcards and
     * attribute values. Wildcards can be specified such as a*, *, *a. To
     * further refine the search, attribute-value pairs can be specified so that
     * DNs of dynamic groups with matching attribute-value pairs will be
     * returned.
     * 
     * @param wildcard
     *            wildcard pattern to be used in the search
     * @param avPairs
     *            attribute-value pairs to match when searching dynamic groups.
     * @param searchControl
     *            specifies the search scope to be used, VLV ranges etc.
     * @return <code>AMSearchResults</code> which contains a Set of DNs of
     *         dynamic groups matching the search.
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store.
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     */
    public AMSearchResults searchDynamicGroups(String wildcard, Map avPairs,
            AMSearchControl searchControl) throws AMException, SSOException;

    /**
     * Creates static groups.
     * 
     * @param groups
     *            The set of static groups's names to be created.
     * @return set of <code>AMStaticGroup</code> objects created.
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store
     * @throws SSOException
     *             if the single sign on token is no longer valid
     */
    public Set createStaticGroups(Set groups) throws AMException, SSOException;

    /**
     * Deletes static groups.
     * 
     * @param groups
     *            The set of static groups's DNs to be deleted.
     * 
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store
     * @throws SSOException
     *             if the single sign on token is no longer valid
     */
    public void deleteStaticGroups(Set groups) throws AMException, SSOException;

    /**
     * Gets the static groups within the specified level.
     * 
     * @param level
     *            The search level starting from the organizational unit.
     * @return The Set of DNs of static Groups within the specified level.
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store
     * @throws SSOException
     *             if the single sign on token is no longer valid
     */
    public Set getStaticGroups(int level) throws AMException, SSOException;

    /**
     * Gets number of static groups within the specified level.
     * 
     * @param level
     *            The search level starting from the organizational unit.
     * @return Number of static groups within the specified level.
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store
     * @throws SSOException
     *             if the single sign on token is no longer valid
     */
    public long getNumberOfStaticGroups(int level) throws AMException,
            SSOException;

    /**
     * Searches for static groups in this organizational unit using wildcards.
     * Wildcards can be specified such as a*, *, *a.
     * 
     * @param wildcard
     *            wildcard pattern to be used in the search
     * @param level
     *            the search level that needs to be used (
     *            <code>AMConstants.SCOPE_ONE</code>
     *            or <code>AMConstants.SCOPE_SUB</code>).
     * @return Set Set of DNs of static groups matching the search
     * 
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store
     * @throws SSOException
     *             if the single sign on token is no longer valid
     */
    public Set searchStaticGroups(String wildcard, int level)
            throws AMException, SSOException;

    /**
     * Searches for static groups in this organizational unit using wildcards.
     * Wildcards can be specified such as a*, *, *a.
     * 
     * @param wildcard
     *            wildcard pattern to be used in the search.
     * @param searchControl
     *            specifies the search scope to be used, VLV ranges etc.
     * @return <code>AMSearchResults</code> which contains a Set of DNs of
     *         static groups matching the search.
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store.
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     */
    public AMSearchResults searchStaticGroups(String wildcard,
            AMSearchControl searchControl) throws AMException, SSOException;

    /**
     * Searches for static groups in this organizational unit using wildcards
     * and attribute values. Wildcards can be specified such as a*, *, *a. To
     * further refine the search, attribute-value pairs can be specified so that
     * DNs of static groups with matching attribute-value pairs will be
     * returned.
     * 
     * @param wildcard
     *            wildcard pattern to be used in the search
     * @param avPairs
     *            attribute-value pairs to match when searching static groups
     * @param level
     *            the search level that needs to be used (
     *            <code>AMConstants.SCOPE_ONE</code>
     *            or <code>AMConstants.SCOPE_SUB</code>)
     * @return Set Set of DNs of static groups matching the search
     * 
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store
     * @throws SSOException
     *             if the single sign on token is no longer valid
     */
    public Set searchStaticGroups(String wildcard, Map avPairs, int level)
            throws AMException, SSOException;

    /**
     * Searches for static groups in this organizational unit using wildcards
     * and attribute values. Wildcards can be specified such as a*, *, *a. To
     * further refine the search, attribute-value pairs can be specified so that
     * DNs of static groups with matching attribute-value pairs will be
     * returned.
     * 
     * @param wildcard
     *            wildcard pattern to be used in the search.
     * @param avPairs
     *            attribute-value pairs to match when searching static groups.
     * @param searchControl
     *            specifies the search scope to be used, VLV ranges etc.
     * @return <code>AMSearchResults</code> which contains a Set of DNs of
     *         static groups matching the search.
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store.
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     */
    public AMSearchResults searchStaticGroups(String wildcard, Map avPairs,
            AMSearchControl searchControl) throws AMException, SSOException;

    /**
     * Searches for groups in this organizational unit using wildcards and
     * attribute values. Wildcards can be specified such as a*, *, *a.
     * 
     * @param wildcard
     *            wildcard pattern to be used in the search
     * @param avPairs
     *            attribute-value pairs to match when searching groups
     * @param level
     *            the search level that needs to be used (
     *            <code>AMConstants.SCOPE_ONE</code>
     *            or <code>AMConstants.SCOPE_SUB</code>)
     * @return Set DNs of groups matching the search
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store
     * @throws SSOException
     *             if the single sign on token is no longer valid
     */
    public Set searchGroups(String wildcard, Map avPairs, int level)
            throws AMException, SSOException;

    /**
     * Searches for groups in this organizational unit using wildcards and
     * attribute values. Wildcards can be specified such as a*, *, *a.
     * 
     * @param wildcard
     *            wildcard pattern to be used in the search.
     * @param avPairs
     *            attribute-value pairs to match when searching groups.
     * @param searchControl
     *            specifies the search scope to be used.
     * @return <code>AMSearchResults</code> which contains Set a of DNs of
     *         groups matching the search.
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store.
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     */
    public AMSearchResults searchGroups(String wildcard, Map avPairs,
            AMSearchControl searchControl) throws AMException, SSOException;

    /**
     * Creates people containers.
     * 
     * @param peopleContainers
     *            set of people containers' names to be created.
     * @return set of <code>PeopleContainer</code> objects created
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store.
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     */
    public Set createPeopleContainers(Set peopleContainers) throws AMException,
            SSOException;

    /**
     * Creates people containers and initializes their attributes.
     * 
     * @param peopleContainers
     *            Map where the key is the name of the people container, and the
     *            value is a Map to represent attribute-value pairs.
     * @return set of people container objects created.
     * @throws AMException
     *             if there is an internal error in the access management data
     *             store.
     * @throws SSOException
     *             if the single sign on token is no longer valid
     */
    public Set createPeopleContainers(Map peopleContainers) throws AMException,
            SSOException;

    /**
     * Creates people containers and initializes their attributes.
     * 
     * @param peopleContainers
     *            Map where the key is the name of the people container, and the
     *            value is a Map to represent Attribute-Value Pairs.
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store.
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     */
    public void deletePeopleContainers(Set peopleContainers)
            throws AMException, SSOException;

    /**
     * Gets the people containers within the specified level.
     * 
     * @param level
     *            The search level starting from the organizational unit.
     * @return The Set of people containers within the specified level.
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store
     * @throws SSOException
     *             if the single sign on token is no longer valid
     */
    public Set getPeopleContainers(int level) throws AMException, SSOException;

    /**
     * Gets number of people containers within the specified level.
     * 
     * @param level
     *            The search level starting from the organizational unit.
     * @return long Number of people containers within the specified level.
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store
     * @throws SSOException
     *             if the single sign on token is no longer valid
     */
    public long getNumberOfPeopleContainers(int level) throws AMException,
            SSOException;

    /**
     * Searches for people containers in this organizational unit using
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
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store
     * @throws SSOException
     *             if the single sign on token is no longer valid
     */
    public Set searchPeopleContainers(String wildcard, int level)
            throws AMException, SSOException;

    /**
     * Searches for people containers in this organizational unit using
     * wildcards and attribute values. Wildcards can be specified such as a*, *,
     * *a.
     * 
     * @param wildcard
     *            wildcard pattern to be used in the search.
     * @param searchControl
     *            specifies the search scope to be used, VLV ranges etc.
     * @return <code>AMSearchResults</code> which contains a Set DNs of people
     *         containers matching the search.
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store.
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     */
    public AMSearchResults searchPeopleContainers(String wildcard,
            AMSearchControl searchControl) throws AMException, SSOException;

    /**
     * Searches for people containers in this organizational unit using
     * wildcards * and * attribute values. Wildcards can be specified such as
     * a*, *, *a. To further refine the search, attribute-value pairs can be
     * specified so that DNs of people containers with matching attribute-value
     * pairs will be returned.
     * 
     * @param wildcard
     *            pattern to be used in the search.
     * @param avPairs
     *            attribute-value pairs to match when searching people
     *            containers.
     * @param level
     *            the search level that needs to be used (
     *            <code>AMConstants.SCOPE_ONE</code>
     *            or <code>AMConstants.SCOPE_SUB</code>).
     * @return Set DNs of people containers matching the search.
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store.
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     */
    public Set searchPeopleContainers(String wildcard, Map avPairs, int level)
            throws AMException, SSOException;

    /**
     * Searches for people containers in this organizational unit using
     * wildcards * and * attribute values. Wildcards can be specified such as
     * a*, *, *a. To further refine the search, attribute-value pairs can be
     * specified so that DNs of people containers with matching attribute-value
     * pairs will be returned.
     * 
     * @param wildcard
     *            wildcard pattern to be used in the search
     * @param avPairs
     *            attribute-value pairs to match when searching people
     *            containers.
     * @param searchControl
     *            specifies the search scope to be used, VLV ranges etc.
     * @return <code>AMSearchResults</code> which contains a Set DNs of people
     *         containers matching the search.
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store.
     * @throws SSOException
     *             if the single sign on token is no longer valid
     */
    public AMSearchResults searchPeopleContainers(String wildcard, Map avPairs,
            AMSearchControl searchControl) throws AMException, SSOException;

    /**
     * Creates group containers.
     * 
     * @param groupContainers
     *            The set of group containers' names to be created
     * @return set of <code>GroupContainer</code> objects created.
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store.
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     */
    public Set createGroupContainers(Set groupContainers) throws AMException,
            SSOException;

    /**
     * Creates group containers and initializes their attributes.
     * 
     * @param groupContainers
     *            Map where the key is the name of the
     *            <code>groupContainer</code>, and the value is a Map to
     *            represent Attribute-Value Pairs.
     * @return set of <code>GroupContainer</code> objects created.
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store.
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     */
    public Set createGroupContainers(Map groupContainers) throws AMException,
            SSOException;

    /**
     * Deletes group containers.
     * 
     * @param groupContainers
     *            The set of group containers' DN to be deleted.
     * 
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store
     * @throws SSOException
     *             if the single sign on token is no longer valid
     */
    public void deleteGroupContainers(Set groupContainers) throws AMException,
            SSOException;

    /**
     * Gets the group containers within the specified level.
     * 
     * @param level
     *            The search level starting from the organizational unit.
     * @return The Set of group containers within the specified level.
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store
     * @throws SSOException
     *             if the single sign on token is no longer valid
     */
    public Set getGroupContainers(int level) throws AMException, SSOException;

    /**
     * Gets number of group containers within the specified level.
     * 
     * @param level
     *            The search level starting from the organizational unit.
     * @return long Number of group containers within the specified level.
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store
     * @throws SSOException
     *             if the single sign on token is no longer valid
     */
    public long getNumberOfGroupContainers(int level) throws AMException,
            SSOException;

    /**
     * Searches for group containers in this organizational unit using wildcards
     * and attribute values. Wildcards can be specified such as a*, *, *a. To
     * further refine the search, attribute-value pairs can be specified so that
     * DNs of group containers with matching attribute-value pairs will be
     * returned.
     * 
     * @param wildcard
     *            wildcard pattern to be used in the search
     * @param avPairs
     *            attribute-value pairs to match when searching group containers
     * @param level
     *            the search level that needs to be used (
     *            <code>AMConstants.SCOPE_ONE</code>
     *            or <code>AMConstants.SCOPE_SUB</code>)
     * @return Set DNs of group containers matching the search
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store
     * @throws SSOException
     *             if the single sign on token is no longer valid
     */
    public Set searchGroupContainers(String wildcard, Map avPairs, int level)
            throws AMException, SSOException;

    /**
     * Searches for group containers in this organizational unit using wildcards
     * and attribute values. Wildcards can be specified such as a*, *, *a. To
     * further refine the search, attribute-value pairs can be specified so that
     * DNs of group containers with matching attribute-value pairs will be
     * returned.
     * 
     * @param wildcard
     *            pattern to be used in the search.
     * @param avPairs
     *            attribute-value pairs to match when searching group
     *            containers.
     * @param searchControl
     *            specifies the search scope to be used, VLV ranges etc.
     * @return <code>AMSearchResults</code> which contains a Set DNs of group
     *         containers matching the search.
     * @throws AMException
     *             if there is an internal error in the access management data
     *             store.
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     */
    public AMSearchResults searchGroupContainers(String wildcard, Map avPairs,
            AMSearchControl searchControl) throws AMException, SSOException;

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
     *             if there is an internal error in the access management data
     *             store
     * @throws SSOException
     *             if the single sign on token is no longer valid
     */
    public Set createUsers(Set users) throws AMException, SSOException;

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
     * @return Set Set of User objects created
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store
     * @throws SSOException
     *             if the single sign on token is no longer valid
     */
    public Set createUsers(Map users) throws AMException, SSOException;

    /**
     * Deletes users from this organizational unit.
     * 
     * @param users
     *            The set of user DN's to be deleted from the organizational
     *            unit.
     * 
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store
     * @throws SSOException
     *             if the single sign on token is no longer valid
     */
    public void deleteUsers(Set users) throws AMException, SSOException;

    /**
     * Gets the names (DNs) of users in the organizational unit.
     * 
     * @return Set The names(DNs) of users in the organizational unit.
     * 
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store
     * @throws SSOException
     *             if the single sign on token is no longer valid
     */
    public Set getUserDNs() throws AMException, SSOException;

    /**
     * Gets number of users within the specified level.
     * 
     * @param level
     *            The search level starting from the organizational unit.
     * @return Number of users within the specified level.
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store
     * @throws SSOException
     *             if the single sign on token is no longer valid
     */
    public long getNumberOfUsers(int level) throws AMException, SSOException;

    /**
     * Searches for users in this organizational unit using wildcards and
     * attribute values. Wildcards can be specified such as a*, *, *a.
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
     * Searches for users in this organizational unit using wildcards and
     * attribute values. Wildcards can be specified such as a*, *, *a.
     * 
     * @param wildcard
     *            wildcard pattern to be used in the search.
     * @param searchControl
     *            specifies the search scope to be used, VLV ranges etc.
     * @return <code>AMSearchResults</code> which contains a Set DNs of Users
     *         matching the search.
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store.
     * @throws SSOException
     *             if the single sign on token is no longer valid
     */
    public AMSearchResults searchUsers(String wildcard,
            AMSearchControl searchControl) throws AMException, SSOException;

    /**
     * Searches for users in this organizational unit using wildcards and
     * attribute values. Wildcards can be specified such as a*, *, *a. To
     * further refine the search, attribute-value pairs can be specified so that
     * DNs of users with matching attribute-value pairs will be returned.
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
     * Searches for users in this organizational unit using wildcards and
     * attribute values. Wildcards can be specified such as a*, *, *a. To
     * further refine the search, attribute-value pairs can be specified so that
     * DNs of users with matching attribute-value pairs will be returned.
     * 
     * @param wildcard
     *            wildcard pattern to be used in the search
     * @param avPairs
     *            attribute-value pairs to match when searching users
     * @param searchControl
     *            specifies the search scope to be used, VLV ranges etc.
     * @return <code>AMSearchResults</code> which contains a Set of DNs of
     *         Users matching the search.
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store.
     * @throws SSOException
     *             if the single sign on token is no longer valid
     */
    public AMSearchResults searchUsers(String wildcard, Map avPairs,
            AMSearchControl searchControl) throws AMException, SSOException;

    /**
     * Searches for users in this organizational unit using wildcards and
     * filter. Wildcards can be specified such as a*, *, *a. To further refine
     * the search, filter can be specified so that DNs of users with matching
     * filter will be returned.
     * 
     * @param wildcard
     *            wildcard pattern to be used in the search.
     * @param searchControl
     *            specifies the search scope to be used, VLV ranges etc.
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
     * Searches for users in this organization attribute values. Wildcards such
     * as can be specified for the attribute values. The DNs of users with
     * matching attribute-value pairs will be returned.
     * 
     * @param searchControl
     *            specifies the search scope to be used, VLV ranges etc.
     * @param avfilter
     *            this attribute-value pairs filter will be logical AND with
     *            user search filter.
     * @return <code>AMSearchResults</code> which contains a Set DNs of Users
     *         matching the search.
     * @throws AMException
     *             if there is an internal error in the access management data
     *             store.
     * @throws SSOException
     *             if the single sign on token is no longer valid
     */
    public AMSearchResults searchUsers(AMSearchControl searchControl,
            String avfilter) throws AMException, SSOException;

    /**
     * Returns the number of services.
     * 
     * @return number of services.
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store
     * @throws SSOException
     *             if the single sign on token is no longer valid
     */
    public long getNumberOfServices() throws AMException, SSOException;

    /**
     * Gets the names of registered services.
     * 
     * @return The Set of the names of registered services.
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store
     * @throws SSOException
     *             if the single sign on token is no longer valid
     */
    public Set getRegisteredServiceNames() throws AMException, SSOException;

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
     *             if the single sign on token is no longer valid.
     */
    public void registerService(String serviceName, boolean createTemplate,
            boolean activate) throws AMException, SSOException;

    /**
     * Unregister a service for this organizational unit.
     * 
     * @param serviceName
     *            service name to be unregistered
     * @throws AMException
     *             if the service does not exist or could not be unregistered.
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     */
    public void unregisterService(String serviceName) throws AMException,
            SSOException;

    /**
     * Unassigns the given policies from this organizational unit and its roles.
     * 
     * @param serviceName
     *            service name.
     * @param policyDNs
     *            Set of policy DN string
     * 
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store.
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     */
    public void unassignAllPolicies(String serviceName, Set policyDNs)
            throws AMException, SSOException;

    /**
     * Modifies all the templates under this organizational unit that contain
     * any <code>policyDN</code> in given <code>policyDNs</code>.
     * 
     * @param serviceName
     *            service name.
     * @param policyDNs
     *            Set of policy DN string.
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store.
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     */
    public void modifyAllPolicyTemplates(String serviceName, Set policyDNs)
            throws AMException, SSOException;

    /**
     * Deletes all the named policy templates for this Organizational Unit
     * corresponding to the given policy. This includes organizational based and
     * role based policy templates. This is a convenience method.
     * 
     * @param policyDN
     *            a policy DN string.
     * @return true if templates existed and were deleted.
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store.
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     */
    public boolean deleteAllNamedPolicyTemplates(String policyDN)
            throws AMException, SSOException;

    /**
     * Returns all the assigned policies for this organizational unit.
     * 
     * @return a set of assigned policy DNs
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store.
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     */
    public Set getAssignedPolicyDNs() throws AMException, SSOException;

    /**
     * Returns true if a <code>policyDN</code> is assigned to this
     * organizational unit or a role.
     * 
     * @param policyDN
     *            a policy DN.
     * @param serviceName
     *            service name.
     * @return true if policy is assigned to an organization or role.
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store
     * @throws SSOException
     *             if the single sign on token is no longer valid
     */
    public boolean isPolicyAssigned(String policyDN, String serviceName)
            throws AMException, SSOException;

    /**
     * Returns true if an organizational template exists for the service.
     * 
     * @param serviceName
     *            service name
     * @return true if the organizational template exists.
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store
     * @throws SSOException
     *             if the single sign on token is no longer valid
     */
    public boolean orgTemplateExists(String serviceName) throws AMException,
            SSOException;

    /**
     * Assign service to the organizational unit. Also sets the attributes as
     * provided in the map. If the service is already assigned to the user than
     * it sets the attribute-values as defined in the attribute map. Attribute
     * value is validated before being set. Any values for attributes already
     * there are replaced with the ones provided in the attribute map.
     * 
     * @param serviceName
     *            Name of service to be assigned to user.
     * @param attrMap
     *            Map of attribute name and values (as a Set) to be set
     * @throws AMException
     *             if an error is encounters when trying to access/retrieve data
     *             from the data store.
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     */
    public void assignService(String serviceName, Map attrMap)
            throws AMException, SSOException;

    /**
     * Unassigns services from the organizational unit. Also removes service
     * specific attributes, if defined in the user entry.
     * 
     * @param serviceNames
     *            Set of service names.
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store.
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     */
    public void unassignServices(Set serviceNames) throws AMException,
            SSOException;

    /**
     * Create managed entities in this container. Supported entities, as defined
     * in the configuration service <code>DAI</code>, can be created using
     * this method.
     * 
     * @param type
     *            Type of entity to be create (For example "user" or "agent"
     * @param entities
     *            Set of names for the entities to be created
     * @return Set of <code>AMEntity</code> objects
     * @throws AMException
     *             If there is an error when trying to save to the data store
     * @throws SSOException
     *             if the single sign on token is invalid.
     */
    public Set createEntities(String type, Set entities) throws AMException,
            SSOException;

    /**
     * Creates entities and initializes their attributes. Supported entities, as
     * defined in the configuration service <code>DAI</code>, can be created
     * using this method.
     * 
     * @param stype
     *            Type of entity to be create (For example "user" or "agent"
     * @param entities
     *            Map where the key is the name of the entity, and the value is
     *            a Map to represent Attribute-Value Pairs
     * @return Set of <code>AMEntity</code> objects created
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
     *            Name of search template to be used. If a null is passed then
     *            the default search template for entities
     *            <code>BasicEntitySearch</code> is used.
     * @param avPairs
     *            This option can be used to further qualify the search filter.
     *            The attribute-value pairs provided by this map are appended to
     *            the search filter.
     * @return Set DNs of resources matching the search.
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
     * Searches for resources in this people container using wildcards and
     * attribute values. Wildcards can be specified such as a*, *, *a. To
     * further refine the search, attribute-value pairs can be specified so that
     * DNs of users with matching attribute-value pairs will be returned.
     * 
     * @param wildcard
     *            wildcard pattern to be used in the search.
     * @param avPairs
     *            attribute-value pairs to match when searching users
     * @param eSearchTemplate
     *            Name of search template to be used.
     * @param searchControl
     *            specifies the search scope to be used, VLV ranges etc.
     * @return <code>AMSearchResults</code> which contains a Set DNs of
     *         resources matching the search.
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store.
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     */
    public AMSearchResults searchEntities(String wildcard, Map avPairs,
            String eSearchTemplate, AMSearchControl searchControl)
            throws AMException, SSOException;

    /**
     * Searches for entities in this container using wildcards and a filter.
     * Wildcards can be specified such as a*, *, *a. To further refine the
     * search, a filter can be passed, which is used to further qualify the
     * basic entity search filter.
     * 
     * @param wildcard
     *            pattern to be used in the search.
     * @param searchControl
     *            specifies the search scope to be used, VLV ranges etc.
     * @param avfilter
     *            this attribute-value pairs filter will be logical AND with
     *            user search filter.
     * @param eSearchTemplate
     *            Name of search template to be used. If a null is passed then
     *            the default search template for entities
     *            <code>BasicEntitySearch</code> is used.
     * @return <code>AMSearchResults</code> which contains a Set DNs of users
     *         matching the search.
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store.
     * @throws SSOException
     *             if the single sign on token is no longer valid.
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
     *             if the single sign on token is no longer valid.
     */
    public void deleteEntities(Set resources) throws AMException, SSOException;

    /**
     * Returns a set of the supported entity names in this container. This will
     * help the application or console determine what kind of entities are
     * stored in this container, and accordingly look for them.
     * 
     * @return Set of names of the supported objects in this container.
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store.
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     */
    public Set getSupportedTypes() throws AMException, SSOException;

    /**
     * Sets the list of supported types for this container.
     * 
     * @param sTypes
     *            The set of supported entity names.
     * @throws AMException
     *             if an error is encountered when trying to access/retrieve
     *             data from the data store.
     * @throws SSOException
     *             if the single sign on token is no longer valid.
     */
    public void setSupportedTypes(Set sTypes) throws AMException, SSOException;

}
