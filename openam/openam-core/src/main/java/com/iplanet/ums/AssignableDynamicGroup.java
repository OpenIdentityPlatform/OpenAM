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
 * $Id: AssignableDynamicGroup.java,v 1.6 2009/01/28 05:34:50 ww203982 Exp $
 *
 */
/**
 * Portions Copyrighted 2013 ForgeRock, Inc.
 */
package com.iplanet.ums;

import com.iplanet.services.ldap.Attr;
import com.iplanet.services.ldap.AttrSet;
import com.iplanet.services.ldap.ModSet;
import com.iplanet.services.util.I18n;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.ldap.LDAPUrl;
import com.sun.identity.shared.ldap.LDAPv2;
import com.sun.identity.shared.ldap.util.DN;

/**
 * Represents a dynamic group entry that uses memberOf as its filter. It checks
 * whether the user is the member of the specified group
 *
 * @supported.api
 */
public class AssignableDynamicGroup extends DynamicGroup implements
        IAssignableMembership {

    private static I18n i18n = I18n.getInstance(IUMSConstants.UMS_PKG);

    private static Debug debug;
    static {
        debug = Debug.getInstance(IUMSConstants.UMS_DEBUG);
    }

    /**
     * Default constructor
     *
     * @supported.api
     */
    public AssignableDynamicGroup() {
    }

    /**
     * Constructs an in memory AssignableDynamicGroup object. Default registered
     * template will be used. This is an in memory Group object and one needs to
     * call <code>save</code> method to save this newly created object to
     * persistent storage.
     * 
     * @param attrSet Attribute/value set.
     * @exception UMSException if fail to instantiate from persistent storage.
     */
    AssignableDynamicGroup(AttrSet attrSet) throws UMSException {
        this(TemplateManager.getTemplateManager().getCreationTemplate(_class,
                null), attrSet);
    }

    /**
     * Constructs an in memory <code>AssignableDynamicGroup</code> object with
     * a given template. This is an in memory Group object and one needs to
     * call save method to <code>save</code> this newly created object to
     * persistent storage.
     * 
     * @param template Template for creating a group.
     * @param attrSet Attribute/value set.
     * @exception UMSException if fail to instantiate from persistent storage.
     *
     * @supported.api
     */
    public AssignableDynamicGroup(CreationTemplate template, AttrSet attrSet)
            throws UMSException {
        super(template, attrSet);
    }

    /**
     * Constructs an in memory <code>AssignableDynamicGroup</code> object using
     * default registered for <code>AssignableDynamicGroup</code>. This is an
     * in memory Group object and one needs to call <code>save</code> method to
     * save this newly created object to persistent storage.
     * 
     * @param attrSet Attribute/value set, which should not contain
     *        <code>memberUrl</code>; any values of <code>memberUrl</code> will
     *        be overwritten by the explicit search criteria arguments.
     * @param base Search base for evaluating members of the group.
     * @param scope Search scope for evaluating members of the group the value
     *        has to be <code>LDAPv2.SCOPE_ONE</code> or
     *        <code>LDAPv2.SCOPE_SUB</code>.
     * @exception UMSException if fail to instantiate from persistent storage.
     */
    AssignableDynamicGroup(AttrSet attrSet, Guid baseGuid, int scope)
            throws UMSException {
        this(TemplateManager.getTemplateManager().getCreationTemplate(_class,
                null), attrSet, baseGuid, scope);
    }

    /**
     * Constructs an <code>AssignableDynamicGroup</code> object with a given
     * template. This is an in memory Group object and one needs to call
     * <code>save</code> method to save this newly created object to
     * persistent storage.
     * 
     * @param template Template for creating a group.
     * @param attrSet Attribute-value set which should not contain member URL;
     *        any values of member URL will be overwritten by the explicit
     *        search criteria arguments.
     * @param baseGuid Search base for evaluating members of the group
     * @param scope Search scope for evaluating members of the group has to be
     *        <code>LDAPv2.SCOPE_ONE</code> or <code>LDAPv2.SCOPE_SUB</code>.
     * @exception UMSException if fail to instantiate from persistent storage
     *
     * @supported.api
     */
    public AssignableDynamicGroup(CreationTemplate template, AttrSet attrSet,
            Guid baseGuid, int scope) throws UMSException {
        super(template, attrSet);
        // No host, port, or attributes in the URL
        // setUrl( new LDAPUrl( null, 0, base, (String[])null, scope, "" ) );
        setUrl(baseGuid, null, scope);
    }

    /**
     * Sets the search filter used to evaluate this dynamic group. For an 
     * <code>AssignableDynamicGroup</code>, the filter is always
     * <code>"memberof=THIS_DN"</code>, so this method should not generally be
     * called outside the package.
     * 
     * @param filter Search filter for evaluating members of the group the
     *        scope in the filter has to be <code>LDAPv2.SCOPE_ONE</code> or
     *        <code>LDAPv2.SCOPE_SUB</code>.
     *
     * @supported.api
     */
    public void setSearchFilter(String filter) {
        LDAPUrl url = getUrl();
        int scope = url.getScope();
        if (scope != LDAPv2.SCOPE_ONE && scope != LDAPv2.SCOPE_SUB) {
            String msg = i18n.getString(IUMSConstants.ILLEGAL_ADGROUP_SCOPE);
            throw new IllegalArgumentException(msg);
        }
        Guid baseGuid = new Guid(url.getDN());
        setUrl(baseGuid, filter, scope);
    }

    /**
     * Sets the GUID of the entity; used within the package.
     * 
     * @param guid GUID <code>REVIEW</code>: This method overloads the
     *        <code>PersistentObject.setGuid()</code> method. Hence the
     *        signature has to match, and we can't throw the
     *        <code>UMSException</code> that could be thrown from
     *        <code>"setSearchFilter"</code>. Is it enough to log such an
     *        error ???
     */
    protected void setGuid(Guid guid) {
        super.setGuid(guid);
        // setSearchFilter( "(" + "memberof=" + getDN() + ")" );
        try {
            setSearchFilter("memberof=" + getDN());
        } catch (Exception e) {
            // TODO - Log Exception
            if (debug.messageEnabled()) {
                debug.message("AssignableDynamicGroup.setGuid() : "
                        + "Exception : " + e.getMessage());
            }
        }
    }

    /**
     * Adds a member to the group. The change is saved to persistent storage.
     * 
     * @param userGuid Globally unique identifier for the member to be added.
     * @exception UMSException if fail to save to persistent storage or if the
     *            user is not within the scope of the group.
     *
     * @supported.api
     */
    public void addMember(Guid userGuid) throws UMSException {
        // UMSSession session = getUMSSession();
        if (getPrincipal() == null) {
            throw new IllegalArgumentException(i18n
                    .getString(IUMSConstants.NULL_PRINCIPAL));
        }

        addMember(UMSObject.getObject(getPrincipal(), userGuid));
    }

    /**
     * Adds a member to the group. The change is saved to persistent storage.
     * 
     * @param member Object to be added as member.
     * @exception UMSException if fail to save to persistent storage or if the
     *            user is not within the scope of the group.
     *
     * @supported.api
     */
    public void addMember(PersistentObject member) throws UMSException {
        // check whether the userGuid is within the scope of memberUrl
        DN userDN = new DN(member.getGuid().getDn());
        LDAPUrl memberUrl = getUrl();
        DN memberDN = new DN(memberUrl.getDN());

        if (!userDN.isDescendantOf(memberDN) || userDN.equals(memberDN)) {
            String args[] = new String[2];
            args[0] = userDN.toString();
            args[1] = memberUrl.toString();
            String msg = i18n.getString(IUMSConstants.USER_NOT_IN_GROUP_SCOPE,
                    args);
            throw new UMSException(msg);
        } else if (((userDN.countRDNs() - memberDN.countRDNs()) > 1)
                && (memberUrl.getScope() == LDAPv2.SCOPE_ONE)) {
            String args[] = new String[2];
            args[0] = userDN.toString();
            args[1] = memberUrl.toString();
            String msg = i18n.getString(IUMSConstants.USER_NOT_IN_GROUP_SCOPE,
                    args);
            throw new UMSException(msg);
        }
        member.modify(new Attr(MEMBER_ATTR_NAME, this.getDN()), ModSet.ADD);
        member.save();
    }

    /**
     * Adds a list of members to the group. The change is saved to persistent
     * storage.
     * 
     * @param guids Array of member GUIDs to be added as members to the group.
     * @exception UMSException if fail to save to persistent storage.
     *
     * @supported.api
     */
    public void addMembers(Guid[] guids) throws UMSException {
        if (guids == null) {
            throw new IllegalArgumentException(i18n
                    .getString(IUMSConstants.NULL_GUIDS));
        }
        for (int i = 0; i < guids.length; i++) {
            addMember(guids[i]);
        }
    }

    /**
     * Removes a member from the group. The change is saved to persistent
     * storage.
     * 
     * @param guid Unique identifier for the member to be removed.
     * @exception UMSException if fail to save to persistent storage.
     *
     * @supported.api
     */
    public void removeMember(Guid guid) throws UMSException {
        PersistentObject member = UMSObject.getObject(getPrincipal(), guid);
        removeMember(member);
    }

    /**
     * Removes a member from the group. The change is saved to persistent
     * storage.
     * 
     * @param member Object to be removed.
     * @exception UMSException if fail to save to persistent storage.
     *
     * @supported.api
     */
    public void removeMember(PersistentObject member) throws UMSException {
        member.modify(new Attr(MEMBER_ATTR_NAME, this.getDN()), ModSet.DELETE);
        member.save();
    }

    /**
     * Removes all members of the group.
     * 
     * @exception UMSException if fail to save to persistent storage.
     *
     * @supported.api
     */
    public void removeAllMembers() throws UMSException {

        String filter = getSearchFilter();
        if (filter == null) {
            return;
        }
        String[] attributesToGet = { "dn" };
        SearchResults searchResults = getMemberIDs(attributesToGet);
        while (searchResults.hasMoreElements()) {
            PersistentObject member = searchResults.next();
            member.setPrincipal(getPrincipal());
            removeMember(member);
        }
    }

    /**
     * Returns <code>true</code> if a given identifier is a member of the
     * group.
     * 
     * @param guid Identity of member to be checked for membership.
     * @return <code>true</code> if it is a member.
     * @exception UMSException if fail to read object for guid.
     *
     * @supported.api
     */
    public boolean hasMember(Guid guid) throws UMSException {
        if (getPrincipal() == null) {
            throw new IllegalArgumentException(i18n
                    .getString(IUMSConstants.NULL_PRINCIPAL));
        }
        PersistentObject object = UMSObject.getObject(getPrincipal(), guid);
        Attr attr = object.getAttribute(MEMBER_ATTR_NAME);
        if (attr == null) {
            if (debug.messageEnabled()) {
                debug.message("AssignableDynamicGroup.hasMember: no "
                        + "attribute " + MEMBER_ATTR_NAME + " in "
                        + guid.getDn());
            }
            return false;
        }

        // need to normalize DN to escape spaces and such
        // for accurate checking of membership
        // TODO: This ties guids to DNS. The methods to normalize and compare
        // should be managed separately.
        // TODO: The members should have been normalized before adding to
        // the group (i.e. when creating or modifying it), so it should not
        // be necessary to have normalizing code spread out in the classes
        // and methods.
        String normalized = getGuid().getDn();
        String[] members = attr.getStringValues();
        for (int i = 0; i < members.length; i++) {
            String target = members[i];
            if (debug.messageEnabled()) {
                debug.message("AssignableDynamicGroup.hasMember: comparing "
                        + normalized + " to " + target);
            }
            if (Guid.equals(normalized, target)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Saves the modification(s) to the object to persistent storage.
     * 
     * @return UMSException on failure to save to persistent storage.
     */
    /*
     * public void save () throws UMSException { String filter =
     * getSearchFilter(); if ( (filter == null) || (filter.length() < 1) ) {
     * setSearchFilter( "memberof=" + getDN() ); } super.save(); }
     */

    private static final String MEMBER_ATTR_NAME = "memberof";

    private static final Class _class = new AssignableDynamicGroup().getClass();
}
