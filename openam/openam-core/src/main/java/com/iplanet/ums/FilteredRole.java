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
 * $Id: FilteredRole.java,v 1.4 2009/01/28 05:34:50 ww203982 Exp $
 *
 */

package com.iplanet.ums;

import java.security.Principal;

import com.sun.identity.shared.ldap.LDAPv2;

import com.iplanet.services.ldap.Attr;
import com.iplanet.services.ldap.AttrSet;
import com.iplanet.services.util.I18n;

/**
 * FilteredRole is a role implementation of the membership interface
 * IFilteredRole. FilteredRole maps to nsFilteredRoleDefinition of iPlanet
 * Directory Server.
 * @supported.api
 */
public class FilteredRole extends BaseRole implements IFilteredMembership,
        IUMSConstants {

    /**
     * Name of the filter attribute, which controls membership.
     *
     * @supported.api
     */
    public static final String FILTER_ATTR_NAME = "nsRoleFilter";

    /**
     * LDAP object classes that define the nsFilteredRoleDefinition, the iPlanet
     * Directory Server object class that maps to FilteredRole.
     *
     * @supported.api
     */
    public static final String[] FILTEREDROLE_OBJECTCLASSES = { "top",
            "ldapsubentry", "nsroledefinition", "nscomplexroledefinition",
            "nsfilteredroledefinition" };

    /**
     * The attributes that are required for FilteredRole. Any creation template
     * for FilteredRole should have these attributes.
     *
     * @supported.api
     */
    public static final String[] FILTEREDROLE_ATTRIBUTES = { "cn",
            "nsRoleFilter" };

    private static I18n i18n = I18n.getInstance(IUMSConstants.UMS_PKG);

    /**
     * No argument constructor
     */
    public FilteredRole() {
    }

    /**
     * Constructs a FilteredRole object in-memory using the default template
     * registered for FilteredRole. The save method must be called to save the
     * new object to persistent storage.
     * 
     * @param name
     *            name for the role
     * @param filter
     *            the filter that controls membership
     * @exception UMSException
     *                on failure to instantiate
     */
    public FilteredRole(String name, String filter) throws UMSException {
        this(new AttrSet(new Attr("cn", name)));
    }

    /**
     * Constructs a FilteredRole object in-memory using the default registered
     * template for FilteredRole. One needs to call the save method to save the
     * new object to persistent storage.
     * 
     * @param attrSet
     *            Attribute/value set
     * @exception UMSException
     *                on failure to instantiate
     */
    FilteredRole(AttrSet attrSet) throws UMSException {
        this(TemplateManager.getTemplateManager().getCreationTemplate(_class,
                null), attrSet);
    }

    /**
     * Constructs a FilteredRole object in memory
     * with a given template. The save method must be called to save the new
     * object to persistent storage.
     * 
     * @param template
     *            Template for creating a group
     * @param attrSet
     *            Attribute/value set
     * @exception UMSException
     *                on failure to instantiate
     *
     * @supported.api
     */
    public FilteredRole(CreationTemplate template, AttrSet attrSet)
            throws UMSException {
        super(template, attrSet);
    }

    /**
     * Sets the filter that controls the membership.
     * 
     * @param filter
     *            the filter that controls the membership
     * @throws UMSException
     *             if there is any error while setting the filter
     *
     * @supported.api
     */
    public void setFilter(String filter) throws UMSException {
        setAttribute(new Attr(FILTER_ATTR_NAME, filter));
    }

    /**
     * Gets the filter that controls the membership.
     * 
     * @return the filter that controls the membership
     * @throws UMSException
     *             if there is any error while getting the filter
     *
     * @supported.api
     */
    public String getFilter() throws UMSException {
        return getAttribute(FILTER_ATTR_NAME).getValue();
    }

    /**
     * TO DO : incomplete, fix the logic of building the base and filter Gets
     * the members of the role
     * 
     * @param attributes
     *            Attributes to return
     * @return SearchResults for iterating through the unique identifiers of
     *         members of the role
     * @throws UMSException
     *             on failure to search
     */
    protected SearchResults getMembers(String[] attributes) throws UMSException
    {
        Guid guid = getGuid();
        String base = guid.getDn();
        int index = base.indexOf(",");
        if (index > 0) {
            base = base.substring(index + 1);
        }
        Guid bguid = new Guid(base);
        Principal principal = getPrincipal();
        if (principal == null) {
            throw new IllegalArgumentException(i18n
                    .getString(IUMSConstants.BAD_PRINCIPAL_HDL));
        }

        return DataLayer.getInstance().search(principal, bguid,
                LDAPv2.SCOPE_SUB, getFilter(), attributes, false, null);
    }

    /**
     * TO DO : incomplete, fix the logic of building the base and filter Gets
     * the members of the role
     * 
     * @param attributes
     *            Attributes to return
     * @param filter
     *            an LDAP filter to select a subset of members
     * @return SearchResults for iterating through the unique identifiers of
     *         members of the role
     * @throws InvalidSearchFilterException
     *             on invalid search filter
     * @throws UMSException
     *             on failure to search
     */
    protected SearchResults getMembers(String[] attributes, String filter)
            throws InvalidSearchFilterException, UMSException {
        Guid guid = getGuid();
        String base = guid.getDn();
        int index = base.indexOf(",");
        if (index > 0) {
            base = base.substring(index + 1);
        }
        Guid bguid = new Guid(base);
        return DataLayer.getInstance().search(
                getPrincipal(),
                bguid,
                LDAPv2.SCOPE_SUB,
                " ( & " + " ( " + getFilter() + ")" + " ( " + filter + " ) "
                        + " ) ", attributes, false, null);
    }

    /**
     * Gets the members of the role.
     * 
     * @return SearchResults for iterating through the unique identifiers of
     *         members of the role
     * @throws UMSException
     *             on failure to search
     *
     * @supported.api
     */
    public SearchResults getMemberIDs() throws UMSException {
        String[] attributesToGet = { "objectclass" };
        return getMembers(attributesToGet);
    }

    /**
     * Gets the members of the role meeting an LDAP filter
     * condition.
     * 
     * @param filter
     *            an LDAP filter to select a subset of members
     * @return SearchResults for iterating through the unique identifiers of
     *         members of the role
     * @exception UMSException
     *                on failure to search
     *
     * @supported.api
     */
    public SearchResults getMemberIDs(String filter) throws UMSException {
        String[] attributesToGet = { "objectclass" };
        return getMembers(attributesToGet, filter);
    }

    /**
     * Gets the member count.
     * 
     * @return Number of members of the role
     * @exception UMSException
     *                on failure to search
     *
     * @supported.api
     */
    public int getMemberCount() throws UMSException {
        int count = 0;
        String[] attributesToGet = { "dn" };
        SearchResults searchResults = getMembers(attributesToGet);
        while (searchResults.hasMoreElements()) {
            searchResults.next().getDN();
            count++;
        }
        return count;
    }

    /**
     * Gets a member given an index (zero based).
     * 
     * @param index
     *            Zero-based index into the group container
     * @return Unique identifier for a member
     * @exception UMSException
     *                on failure to search
     *
     * @supported.api
     */
    public Guid getMemberIDAt(int index) throws UMSException {
        if (index < 0) {
            throw new IllegalArgumentException(Integer.toString(index));
        }
        String[] attributesToGet = { "dn" };
        SearchResults searchResults = getMembers(attributesToGet);
        int srIndex = 0;
        while (searchResults.hasMoreElements()) {
            String s = searchResults.next().getDN();
            if (srIndex == index) {
                searchResults.abandon();
                return new Guid(s);
            }
            srIndex++;
        }
        throw new ArrayIndexOutOfBoundsException(Integer.toString(index));
    }

    /**
     * Checks if a given identifier is a member of the role.
     * 
     * @param po
     *            member to be checked for membership
     * @return <code>true</code> if it is a member
     * @exception UMSException
     *                on failure to read object for guid
     */
    // public boolean isMember(PersistentObject po)
    // throws UMSException {
    // return hasMember(po, true);
    // }
    /**
     * Checks if a given identifier is a member of the role.
     * 
     * @param guid
     *            guid of the member to be checked for membership
     * @return <code>true</code> if it is a member
     * @exception UMSException
     *                on failure to read object for guid
     *
     * @supported.api
     */
    public boolean hasMember(Guid guid) throws UMSException {
        Principal principal = getPrincipal();
        if (principal == null) {
            throw new IllegalArgumentException(i18n
                    .getString(IUMSConstants.BAD_PRINCIPAL_HDL));
        }
        PersistentObject member = UMSObject.getObject(principal, guid);
        return hasMember(member);
    }

    private static final Class _class = com.iplanet.ums.FilteredRole.class;
}
