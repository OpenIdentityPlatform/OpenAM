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
 * $Id: StaticGroup.java,v 1.4 2009/01/28 05:34:51 ww203982 Exp $
 *
 */

package com.iplanet.ums;

import java.security.Principal;
import java.util.Enumeration;

import com.sun.identity.shared.ldap.LDAPAttribute;
import com.sun.identity.shared.ldap.LDAPDN;

import com.iplanet.services.ldap.Attr;
import com.iplanet.services.ldap.AttrSet;
import com.iplanet.services.ldap.ModSet;
import com.iplanet.services.util.I18n;

/**
 * Represents a static group entry.
 * @supported.api
 */
public class StaticGroup extends PersistentObject implements
        IAssignableMembership {

    /**
     * Level indicator for no nesting of group membership. Use this level
     * indicator for getting direct membership in a group.
     * 
     * @supported.api
     */
    public static final int LEVEL_DIRECT = 0;

    /**
     * Level indicator for expanding nested membership to the fullest. Use this
     * level indicator in getting all direct and indirect members through nested
     * group behavior.
     * 
     * @supported.api
     */
    public static final int LEVEL_ALL = -1;

    /**
     * Internal maximum level used if no default is found in configuration.
     */
    static final int DEFAULT_MAX = 5;

    private static I18n i18n = I18n.getInstance(IUMSConstants.UMS_PKG);

    /**
     * Default constructor
     */
    protected StaticGroup() {
    }

    /**
     * Constructs a group object from an ID by reading from persistent storage.
     * 
     * @param session
     *            Authenticated session
     * @param guid
     *            Globally unique identifier for the group entry
     * @exception UMSException
     *                on failure to instantiate from persistent storage
     * @deprecated
     */
    StaticGroup(Principal principal, Guid guid) throws UMSException {
        super(principal, guid);
        verifyClass();
    }

    /**
     * Constructs a group object in memory using the default registered template
     * for StaticGroup. This is an in-memory representation of a new StaticGroup
     * object; the save method must be called to save the new object to
     * persistent storage.
     * 
     * @param attrSet
     *            Attribute/value set
     * @exception UMSException
     *                on failure to instantiate from persistent storage
     */
    StaticGroup(AttrSet attrSet) throws UMSException {
        this(TemplateManager.getTemplateManager().getCreationTemplate(_class,
                null), attrSet);
    }

    /**
     * Constructs a StaticGroup object in memory with
     * a given template. This one simply creates a Group object in memory; the
     * save method must be called to save the new object to persistent storage.
     * 
     * @param template
     *            Template for creating a group
     * @param attrSet
     *            Attribute/value set
     * @exception UMSException
     *                on failure to instantiate from persistent storage
     * @supported.api
     */
    public StaticGroup(CreationTemplate template, AttrSet attrSet)
            throws UMSException {
        super(template, attrSet);
    }

    /**
     * Adds a member to the group. The change is saved to
     * persistent storage.
     * 
     * @param guid
     *            Globally unique identifier for the member to be added
     * @exception UMSException
     *                on failure to save to persistent storage
     * @supported.api
     */
    public void addMember(Guid guid) throws UMSException {

        String id = LDAPDN.normalize(guid.getDn());

        PersistentObject entry = null;

        try {
            // entry = getUMSSession().getObject(guid);
            entry = UMSObject.getObject(getPrincipal(), guid);
        } catch (UMSException ignore) {
        }

        if (entry != null && entry instanceof StaticGroup) {
            StaticGroup g = (StaticGroup) entry;
            if (id.equalsIgnoreCase(getDN())
                    || g.hasMember(getGuid(), LEVEL_ALL)) {
                throw new UMSException(i18n
                        .getString(IUMSConstants.NO_RECURSION_ALLOW));
            }
        }

        modify(new Attr(MEMBER_ATTR_NAME, id), ModSet.ADD);
        save();
    }

    /**
     * Adds a member to the group. The change is saved to
     * persistent storage.
     * 
     * @param member
     *            Object to be added as member
     * @exception UMSException
     *                on failure to save to persistent storage
     * @supported.api
     */
    public void addMember(PersistentObject member) throws UMSException {
        addMember(member.getGuid());
    }

    /**
     * Adds a list of members to the group. The change is
     * saved to persistent storage.
     * 
     * @param guids
     *            Array of member guids to be added as members to the group
     * @exception UMSException
     *                on failure to save to persistent storage
     * @supported.api
     */
    public void addMembers(Guid[] guids) throws UMSException {
        if (guids == null) {
            String msg = i18n.getString(IUMSConstants.BAD_GUID);
            throw new IllegalArgumentException(msg);
        }

        for (int i = 0; i < guids.length; i++) {
            addMember(guids[i]);
        }
    }

    /**
     * Gets the members of the group.
     * 
     * @return SearchResults for members of the group
     * @exception Not
     *                thrown by this class
     * @supported.api
     */
    public SearchResults getMemberIDs() throws UMSException {
        return getMembers(LEVEL_DIRECT);
    }

    static int getMaxNestingLevel() {
        // PKB: Get it from the dsConfig manager
        // TO FIX
        return DEFAULT_MAX;
    }

    /**
     * Get members of the group.
     * 
     * @param level
     *            Nesting level
     * @return SearchResults for members of the group
     * @exception Not
     *                thrown by this class
     * @supported.api
     * 
     */
    public SearchResults getMembers(int level) throws UMSException {
        Attr attr = getAttribute(MEMBER_ATTR_NAME);
        if (attr == null) {
            return null;
        }

        if (level == LEVEL_ALL) {
            level = getMaxNestingLevel();
        }

        if (level == LEVEL_DIRECT) {
            return new SearchResults(getAttribute(MEMBER_ATTR_NAME));
        }

        Attr nestedMembers = new Attr(MEMBER_ATTR_NAME);
        LDAPAttribute la = attr.toLDAPAttribute();
        Enumeration en = la.getStringValues();

        while (en.hasMoreElements()) {
            String memberdn = (String) en.nextElement();
            PersistentObject entry = null;

            try {
                // entry = getUMSSession().getObject(new Guid(memberdn));
                entry = UMSObject.getObject(getPrincipal(), new Guid(memberdn));
            } catch (UMSException ignore) {
            }

            if (entry != null && entry instanceof StaticGroup) {
                SearchResults r = ((StaticGroup) entry).getMembers(level - 1);

                while (r.hasMoreElements()) {
                    PersistentObject member = null;

                    try {
                        member = r.next();
                        nestedMembers.addValue(member.getDN());
                    } catch (UMSException ignore) {
                    }
                }
            } else {
                nestedMembers.addValue(memberdn);
            }

            entry = null;
        }

        return new SearchResults(nestedMembers);
    }

    /**
     * Gets the member count.
     * 
     * @return Number of members of the group
     * @exception Not
     *                thrown by this class
     * @supported.api
     */
    public int getMemberCount() throws UMSException {
        return getMemberCount(LEVEL_DIRECT);
    }

    /**
     * Gets the member count.
     * 
     * @param level
     *            Nesting level
     * @return Number of members of the group
     * @exception Not
     *                thrown by this class
     * @supported.api
     */
    public int getMemberCount(int level) throws UMSException {

        if (level == LEVEL_ALL) {
            level = getMaxNestingLevel();
        }

        if (level == LEVEL_DIRECT) {
            Attr attr = getAttribute(MEMBER_ATTR_NAME);
            return (attr != null) ? attr.size() : 0;
        }

        SearchResults allMembers = getMembers(level);

        if (allMembers == null)
            return 0;
        int count = 0;
        while (allMembers.hasMoreElements()) {
            allMembers.next();
            count++;
        }
        return count;
    }

    /**
     * Gets a member given an index (zero-based).
     * 
     * @param index
     *            Zero-based index into the group container
     * @return The unique identifier for a member
     * @exception Not
     *                thrown by this class
     * @supported.api
     */
    public Guid getMemberIDAt(int index) throws UMSException {
        Attr attr = getAttribute(MEMBER_ATTR_NAME);
        String value = attr.getStringValues()[index];
        return (value != null) ? new Guid(value) : null;
    }

    /**
     * Gets a member given an index (zero-based).
     * 
     * @param index
     *            Zero-based index into the group container
     * @param level
     *            Nesting level
     * @return The unique identifier for a member
     * @exception Not
     *                thrown by this class
     * @supported.api
     */
    public Guid getMemberIDAt(int index, int level) throws UMSException {
        SearchResults allMembers = getMembers(level);
        if (allMembers == null) {
            return null;
        }

        int i = 0;
        while (allMembers.hasMoreElements()) {
            PersistentObject entry = allMembers.next();
            if (i++ == index) {
                return new Guid(entry.getDN());
            }
        }
        return null;
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
        String dn = guid.getDn();
        super.modify(new Attr(MEMBER_ATTR_NAME, LDAPDN.normalize(dn)),
                ModSet.DELETE);
        save();
    }

    /**
     * Removes a member from the group. The change is saved to persistent
     * storage.
     * 
     * @param member
     *            Object to be removed
     * @exception UMSException
     *                on failure to save to persistent storage
     * @supported.api
     */
    public void removeMember(PersistentObject member) throws UMSException {
        removeMember(member.getGuid());
    }

    /**
     * Removes all members of the group.
     * 
     * @exception UMSException
     *                on failure to save to persistent storage
     * @supported.api
     */
    public void removeAllMembers() throws UMSException {

        if (getMemberCount() == 0) {
            return;
        }

        ModSet modSet = new ModSet();

        // TODO: this should probably be REPLACE instead of DELETE, so it
        // works even if there are no members
        modSet.add(ModSet.DELETE, new LDAPAttribute(MEMBER_ATTR_NAME));

        modify(modSet);
        save();
    }

    /**
     * Checks if a given identifier is a member of the group.
     * 
     * @param guid
     *            Identity of member to be checked for membership
     * @return <code>true if it is a member
     * @exception   Not thrown by this class
     * @supported.api
     */
    public boolean hasMember(Guid guid) throws UMSException {
        return isMemberAtLevel(guid.getDn(), LEVEL_DIRECT);
    }

    private boolean isMemberAtLevel(String normalizedID, int level)
            throws UMSException {

        if (level == LEVEL_ALL) {
            level = getMaxNestingLevel();
        }

        SearchResults members = getMembers(level);

        while (members.hasMoreElements()) {
            PersistentObject entry = members.next();
            String entryDN = entry.getDN();
            if (Guid.equals(normalizedID, entryDN)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Checks if a given identifier is a member of the group.
     * 
     * @param guid
     *            Identity of member to be checked for membership
     * @param level
     *            Nesting level
     * @return <code>true</code> if it is a member
     * @exception Not
     *                thrown by this class
     * @supported.api
     */
    public boolean hasMember(Guid guid, int level) throws UMSException {

        if (level == LEVEL_ALL) {
            level = getMaxNestingLevel();
        }

        String id = guid.getDn();

        for (int i = LEVEL_DIRECT; i <= level; i++) {
            if (isMemberAtLevel(id, i)) {
                return true;
            }
        }
        return false;
    }

    private static final String MEMBER_ATTR_NAME = "uniquemember";

    private static final Class _class = new StaticGroup().getClass();
}
