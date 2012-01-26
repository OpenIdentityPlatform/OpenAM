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
 * $Id: ManagedRole.java,v 1.5 2009/01/28 05:34:50 ww203982 Exp $
 *
 */

package com.iplanet.ums;

import java.security.Principal;

import com.sun.identity.shared.ldap.LDAPv2;
import com.sun.identity.shared.ldap.util.DN;

import com.iplanet.services.ldap.Attr;
import com.iplanet.services.ldap.AttrSet;
import com.iplanet.services.ldap.ModSet;
import com.iplanet.services.util.I18n;

/**
 * ManagedRole is a role implementation of the membership interface
 * IAssignableMembership. ManagedRole maps to nsManagedRoleDefinition of iPlanet
 * Directory Server. Member objects added to the role should allow nsRoleDN
 * attribute. When a member is added to the role, the DN of the role is added to
 * the member's nsRoleDN attribute. When a member is removed from the role, the
 * DN of the role is removed from the member's nsRoleDN attribute value.
 *
 * @supported.api
 */
public class ManagedRole extends BaseRole implements IAssignableMembership {

    private static I18n i18n = I18n.getInstance(IUMSConstants.UMS_PKG);

    /**
     * Name of the member attribute, which is modified when the member is added
     * to/removed from the role. To be added as a member of the role, the member
     * object should allow this attribute.
     * @supported.api
     */
    public static final String MEMBER_ATTR_NAME = "nsRoleDN";

    /**
     * Name of the computed member attribute, which would be computed by
     * Directory server for role, when the member entry is read.
     * @supported.api
     */
    public static final String COMPUTED_MEMBER_ATTR_NAME = "nsRole";

    /**
     * LDAP object classes that define the nsManagedRoleDefinition, the iPlanet
     * Directory Server object class, that maps to ManagedRole
     * @supported.api
     */
    public static final String[] MANAGEDROLE_OBJECTCLASSES = { "top",
            "ldapsubentry", "nsroledefinition", "nssimpleroledefinition",
            "nsmanagedroledefinition" };

    /**
     * The attribute that is must for ManagedRole. Any creation template for
     * ManagedRole should have this attribute
     * @supported.api
     */
    public static final String[] MANAGEDROLE_ATTRIBUTES = { "cn" };

    /**
     * No argument constructor
     * 
     * @supported.api
     */
    public ManagedRole() {
    }

    /**
     * Constructs a ManagedRole object in memory using the default template
     * registered for ManagedRole. The save method must be called to save the
     * new object to persistent storage.
     * 
     * @param name
     *            name for the role
     * @throws UMSException
     *             on failure to instantiate
     */
    ManagedRole(String name) throws UMSException {
        this(new AttrSet(new Attr("cn", name)));
    }

    /**
     * Constructs a ManagedRole object in memory using the default template
     * registered for ManagedRole. One needs to call save method to save the new
     * object to persistent storage.
     * 
     * @param attrSet
     *            Attribute/value set
     * @throws UMSException
     *             on failure to instantiate
     */
    ManagedRole(AttrSet attrSet) throws UMSException {
        this(TemplateManager.getTemplateManager().getCreationTemplate(_class,
                null), attrSet);
    }

    /**
     * Constructs a ManagedRole object in memory with a given template. One
     * needs to call save method to save the new object to persistent storage.
     * 
     * @param template
     *            Template for creating a group
     * @param attrSet
     *            Attribute/value set
     * @throws UMSException
     *             on failure to instantiate
     * @supported.api
     */
    public ManagedRole(CreationTemplate template, AttrSet attrSet)
            throws UMSException {
        super(template, attrSet);
    }

    /**
     * Adds a member to the role. The change is saved to persistent storage.
     * 
     * @param member
     *            Object to be added as member
     * @throws UMSException
     *             on failure to save to persistent storage
     * @supported.api
     */
    public void addMember(PersistentObject member) throws UMSException {
        member.modify(new Attr(MEMBER_ATTR_NAME, this.getDN()), ModSet.ADD);
        this.getDN();

        Principal principal = getPrincipal();
        if (principal == null) {
            throw new IllegalArgumentException(i18n
                    .getString(IUMSConstants.BAD_PRINCIPAL_HDL));
        }
        DataLayer.getInstance().addAttributeValue(principal, member.getGuid(),
                MEMBER_ATTR_NAME, this.getDN());

        // invalidate the cached computed role attribute
        member.getAttrSet().remove(COMPUTED_MEMBER_ATTR_NAME);
    }

    /**
     * Adds a member to the role. The change is saved to persistent storage.
     * 
     * @param guid Globally unique identifier for the member to be added.
     * @throws UMSException if fail to save to persistent storage.
     * @supported.api
     */
    public void addMember(Guid guid) throws UMSException {

        Principal principal = getPrincipal();
        if (principal == null) {
            throw new IllegalArgumentException(i18n
                    .getString(IUMSConstants.BAD_PRINCIPAL_HDL));
        }
        DataLayer.getInstance().addAttributeValue(principal, guid,
                MEMBER_ATTR_NAME, this.getDN());
    }

    /**
     * Adds a list of members to the role. The change is saved to persistent
     * storage.
     * 
     * @param guids
     *            Array of member guids to be added as members to the role
     * @throws UMSException
     *             on failure to save to persistent storage
     * @supported.api
     */
    public void addMembers(Guid[] guids) throws UMSException {
        if (guids == null) {
            String msg = i18n.getString(IUMSConstants.BAD_GUID);
            throw new IllegalArgumentException(msg);
        }
        if (guids == null) {
            throw new IllegalArgumentException(i18n
                    .getString(IUMSConstants.NULL_GUIDS));
        }
        for (int i = 0; i < guids.length; i++) {
            addMember(guids[i]);
        }
    }

    /**
     * Gets the members of the role.
     * 
     * @param attributes
     *            Attributes to return
     * @return SearchResults to iterate over members of the role
     * @throws UMSException
     *             on failure to search
     */
    protected SearchResults getMemberIDs(String[] attributes)
            throws UMSException {

        Principal principal = getPrincipal();
        if (principal == null) {
            throw new IllegalArgumentException(i18n
                    .getString(IUMSConstants.BAD_PRINCIPAL_HDL));
        }
        // Review: PKB: The members of the role
        // must be under the role definition
        String dn = getGuid().getDn();
        DN tdn = new DN(dn);
        tdn = tdn.getParent();

        Guid guid = new Guid(tdn.toString());

        return DataLayer.getInstance().search(principal, guid,
                LDAPv2.SCOPE_SUB, "(" + MEMBER_ATTR_NAME + "=" + getDN() + ")",
                attributes, false, null);
    }

    /**
     * Gets the members of the role meeting the filter condition.
     * 
     * @param attributes
     *            Attributes to return
     * @param filter
     *            LDAP filter to select a subset of members
     * @return SearchResults to iterate over members of the role
     * @throws InvalidSearchFilterException
     *             on invalid search filter
     * @throws UMSException
     *             on failure to search
     */
    protected SearchResults getMemberIDs(String[] attributes, String filter)
            throws InvalidSearchFilterException, UMSException {

        Principal principal = getPrincipal();
        if (principal == null) {
            throw new IllegalArgumentException(i18n
                    .getString(IUMSConstants.BAD_PRINCIPAL_HDL));
        }
        DN dn = new DN(this.getDN());
        dn = dn.getParent();
        Guid guid = new Guid(dn.toString());
        return DataLayer.getInstance().search(
                principal,
                guid,
                LDAPv2.SCOPE_SUB,
                "( & " + " ( " + MEMBER_ATTR_NAME + "=" + getDN() + " ) "
                        + " ( " + filter + " ) " + " ) ", attributes, false,
                null);
    }

    /**
     * Gets the members of the group.
     * 
     * @return Iterator for unique identifiers for members of the role
     * @throws UMSException
     *             on failure to search
     * @supported.api
     */
    public SearchResults getMemberIDs() throws UMSException {
        String[] attributesToGet = { "objectclass" };
        return getMemberIDs(attributesToGet);
    }

    /**
     * Returns the members of the group meeting the filter condition.
     * 
     * @param filter LDAP filter to select a subset of members
     * @return <code>SearchResults</code> that can be used to iterate over the
     *         unique identifiers for members of the role.
     * @throws UMSException if fail to search.
     * @supported.api
     */
    public SearchResults getMemberIDs(String filter) throws UMSException {
        String[] attributesToGet = { "objectclass" };
        return getMemberIDs(attributesToGet);
    }

    /**
     * Gets the member count.
     * 
     * @return Number of members of the role
     * @throws UMSException
     *             on failure to search
     * @supported.api
     */
    public int getMemberCount() throws UMSException {
        int count = 0;
        // String[] attributesToGet = {"dn"};
        SearchResults searchResults = getMemberIDs();
        while (searchResults.hasMoreElements()) {
            searchResults.next().getDN();
            count++;
        }
        return count;
    }

    /**
     * Gets the GUID of the member at the given index (zero-based).
     * 
     * @param index
     *            Zero-based index into the group container
     * @return Unique identifier for a member
     * @throws UMSException
     *             on failure to search
     * @supported.api
     */
    public Guid getMemberIDAt(int index) throws UMSException {
        if (index < 0) {
            throw new IllegalArgumentException(Integer.toString(index));
        }
        // String[] attributesToGet = {"dn"};
        SearchResults searchResults = getMemberIDs();
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
     * Removes a member from the role. The change is saved to persistent
     * storage.
     * 
     * @param member
     *            member to be removed from the role
     * @exception UMSException
     *                on failure to save to persistent storage
     * @supported.api
     */
    public void removeMember(PersistentObject member) throws UMSException {

        Principal principal = getPrincipal();
        if (principal == null) {
            throw new IllegalArgumentException(i18n
                    .getString(IUMSConstants.BAD_PRINCIPAL_HDL));
        }
        member.modify(new Attr(MEMBER_ATTR_NAME, this.getDN()), ModSet.DELETE);
        // member.save();
        DataLayer.getInstance().removeAttributeValue(principal,
                member.getGuid(), MEMBER_ATTR_NAME, this.getDN());

    }

    /**
     * Removes a member from the group. The change is saved to persistent
     * storage.
     * 
     * @param guid
     *            Unique identifier for the member to be removed
     * @exception UMSException
     *                on failure to save to persistent storage
     * @supported.api
     */
    public void removeMember(Guid guid) throws UMSException {

        Principal principal = getPrincipal();
        if (principal == null) {
            throw new IllegalArgumentException(i18n
                    .getString(IUMSConstants.BAD_PRINCIPAL_HDL));
        }
        DataLayer.getInstance().removeAttributeValue(principal, guid,
                MEMBER_ATTR_NAME, this.getDN());
    }

    /**
     * Removes all members of the role.
     * 
     * @exception UMSException
     *                on failure to save to persistent storage
     * @supported.api
     */
    public void removeAllMembers() throws UMSException {
        SearchResults searchResults = getMemberIDs();
        while (searchResults.hasMoreElements()) {
            removeMember(searchResults.next());
        }
    }

    /**
     * Checks if a given identifier is a member of the role.
     * 
     * @param guid
     *            guid of the member to be checked for membership
     * @return <code>true</code> if it is a member
     * @exception UMSException
     *                on failure to read object for guid
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

    private static final Class _class = com.iplanet.ums.ManagedRole.class;
}
