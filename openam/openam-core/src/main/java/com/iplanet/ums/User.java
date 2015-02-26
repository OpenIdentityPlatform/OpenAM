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
 * $Id: User.java,v 1.5 2009/01/28 05:34:51 ww203982 Exp $
 *
 */

package com.iplanet.ums;

import java.security.Principal;
import java.util.Collection;
import java.util.Iterator;

import com.sun.identity.shared.ldap.util.DN;

import com.sun.identity.shared.debug.Debug;
import com.iplanet.services.ldap.Attr;
import com.iplanet.services.ldap.AttrSet;

/**
 * Represents a user entry in UMS.
 * 
 * @supported.api
 */
public class User extends PersistentObject {

    private static Debug debug;

    static {
        debug = Debug.getInstance(IUMSConstants.UMS_DEBUG);
    }

    /**
     * No args constructor; used to construct the right object as entries are
     * read from persistent storage.
     * 
     */
    protected User() throws UMSException {
        super();
    }

    /**
     * Construct user entry from session and a given guid.
     * 
     * @param session
     *            authenticated session maintained by Session Manager
     * @param guid
     *            globally unique identifier for the entity
     */
    User(Principal principal, Guid guid) throws UMSException {
        super(principal, guid);
        verifyClass();
    }

    /**
     * Construct User object without a session. Unlike the constructor with a
     * session parameter; this one simply creates a User object in memory, using
     * the default template. The save() method must be called to save the object
     * to the persistent store.
     * 
     * @param attrSet
     *            attribute/value set
     * 
     */
    User(AttrSet attrSet) throws UMSException {
        this(TemplateManager.getTemplateManager().getCreationTemplate(_class,
                null), attrSet);
    }

    /**
     * Construct User object without session. Unlike constructor with session,
     * this one simply creates a User object in memory. Call the save() method
     * to save the object to data store.
     * 
     * @param template
     *            template to the User
     * @param attrSet
     *            attribute/value set
     * 
     * @supported.api
     */
    public User(CreationTemplate template, AttrSet attrSet) throws UMSException 
    {
        super(template, attrSet);
    }

    /**
     * Return uid for the user
     * 
     * @return uid of the user
     * 
     * @supported.api
     */
    public String getUID() throws UMSException {
        return getAttribute(DEFAULT_NAMING_ATTR).getValue();
    }

    /**
     * Return attribute set according to a supplied search template. The search
     * template is used as attribute retrieval guidelines.
     * 
     * @param template
     *            Search template
     * @return attribute set with attribute names defined in the template
     * 
     * @supported.api
     */
    public AttrSet getAttributes(SearchTemplate template) throws UMSException {
        AttrSet attrSet = new AttrSet();
        String[] attrNames = template.getAttributeNames();

        for (int i = 0; i < attrNames.length; i++) {
            attrSet.add(getAttribute(attrNames[i]));
        }
        return attrSet;
    }

    /**
     * Return groups that the user is a member of. If the user is not a member
     * of a group as indicated in the "memberof" attribute in the user entry,
     * null is returned for this method.
     * 
     * @return String representation of guid for the group(s) that the user is a
     *         member of; null if the user is not recorded as a member of any
     *         group.
     * 
     * @supported.api
     */
    public String[] getGroups() throws UMSException {
        Attr memberOfGroups = getAttribute(MEMBER_ATTR_NAME);

        if (memberOfGroups == null || memberOfGroups.size() == 0) {
            return null;
        } else {
            return memberOfGroups.getStringValues();
        }
    }

    /**
     * Get the access rights associated with the user; this will return an
     * aggregation of all the attribute access rights granted by each of the
     * user's roles. The aggregation will only include from the 'guid' parameter
     * on up the DIT.
     * 
     * @param guid
     *            The starting location of the role (rights) aggregation.
     * @return AccessRightObject associated with the user
     * 
     * @supported.api
     */
    public AccessRightObject getAccessRight(Guid guid) throws UMSException,
            com.iplanet.services.ldap.aci.ACIParseException {
        AccessRightObject aro = new AccessRightObject();
        Collection roles = getRoles();
        Iterator it = roles.iterator();
        if (it != null) {
            if (debug.messageEnabled()) {
                debug.message("User.getAccessRight : Get rights for : "
                        + guid.getDn());
            }
            DN guidDn = new DN(guid.getDn());
            while (it.hasNext()) {
                Guid roleGuid = new Guid((String) it.next());
                DN roleGuidDn = new DN(roleGuid.getDn());
                if (debug.messageEnabled()) {
                    debug.message("User.getAccessRight : Role Dn : "
                            + roleGuid.getDn());
                }
                if (roleGuidDn.getParent().isDescendantOf(guidDn))
                    continue;
                BaseRole role = (BaseRole) UMSObject.getObject(getPrincipal(),
                        roleGuid);
                if (debug.messageEnabled()) {
                    debug.message("User.getAccessRight : Role "
                            + role.getGuid());
                }
                AccessRightObject right = role.getAccessRight();
                aro.grantReadPermission(right.getReadableAttributeNames());
                aro.grantWritePermission(right.getWritableAttributeNames());
                debug.message("User.getAccessRight : Done grant");
            }
        }
        return aro;
    }

    private static final String MEMBER_ATTR_NAME = "memberof";

    private static final String DEFAULT_NAMING_ATTR = "uid";

    static final String NEW_INSTANCE_FAILED = "newinstancefailed";

    private static final Class _class = com.iplanet.ums.User.class;
}
