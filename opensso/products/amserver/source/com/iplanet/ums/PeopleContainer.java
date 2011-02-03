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
 * $Id: PeopleContainer.java,v 1.4 2009/01/28 05:34:50 ww203982 Exp $
 *
 */

package com.iplanet.ums;

import java.security.Principal;

import com.sun.identity.shared.ldap.util.DN;

import com.iplanet.services.ldap.Attr;
import com.iplanet.services.ldap.AttrSet;

/**
 * Represents People Container in UMS. People Container is simply a container
 * for storing user entries.
 * 
 * @supported.api
 */
public class PeopleContainer extends PersistentObject {

    /**
     * No args constructor, used to contruct the right object as entries are
     * read from persistent storage.
     */
    protected PeopleContainer() throws UMSException {
    }

    /**
     * Constructor of People Container from supplied session and guid
     * identifying the People Container to be constructed.
     * 
     * @param session
     *            authenticated session object maintained by the Session Manager
     * @param guid
     *            globally unique identifier for the People Container
     */
    PeopleContainer(Principal principal, Guid guid) throws UMSException {
        super(principal, guid);
        verifyClass();
    }

    /**
     * Constructs an People Container object without a session. Unlike the
     * constructor with a session parameter , this one simply creates an object
     * in memory, using the default template. Call save() to save the object to
     * the persistent store.
     * 
     * @param attrSet
     *            attribute/value set
     * @supported.api
     */
    PeopleContainer(AttrSet attrSet) throws UMSException {
        this(TemplateManager.getTemplateManager().getCreationTemplate(_class,
                null), attrSet);
    }

    /**
     * Constructs a People Container object without a session. Unlike
     * constructor with session, this one simply creates People Container object
     * in memory. Call save() to save the object to persistent storage.
     * 
     * @param template
     *            template for the People Container, containing required and
     *            optional attributes, and possibly default values
     * @param attrSet
     *            attribute/value set 
     * @supported.api
     */
    public PeopleContainer(CreationTemplate template, AttrSet attrSet)
            throws UMSException {
        super(template, attrSet);
    }

    /**
     * Adds a new User object to the People Container.
     * 
     * @param user
     *            User object to be added to the container
     * @exception AccessRightsEsception
     *                if an access rights exception occurs
     * @exception EntryAlreadyExistsException
     *                if the entry already exists
     * @exception UMSException
     *                Fail to add the object 
     * @supported.api
     */
    public void addUser(User user) throws AccessRightsException,
            EntryAlreadyExistsException, UMSException {
        super.addChild(user);
    }

    /**
     * Adds a new People Container object to the People Container.
     * 
     * @param pc
     *            People Container object to be added to the container
     * @exception AccessRightsEsception
     *                if an access rights exception occurs
     * @exception EntryAlreadyExistsException
     *                if the entry already exists
     * @exception UMSException
     *                fails to add the object 
     * @supported.api
     */
    public void addChildPeopleContainer(PeopleContainer pc)
            throws AccessRightsException, EntryAlreadyExistsException,
            UMSException {
        super.addChild(pc);
    }

    /**
     * Removes an User object from the People Container.
     * 
     * @param user
     *            User object to be removed to the container
     * @exception AccessRightsEsception
     *                if an access rights exception occurs
     * @exception UMSException
     *                fails to remove the object 
     * @supported.api
     */
    public void removeUser(User user) throws AccessRightsException,
            UMSException {
        super.removeChild(user);
    }

    /**
     * Removes a People Container object from the People Container.
     * 
     * @param pc
     *            People Container object to be removed to the container
     * @exception AccessRightsEsception
     *                if an access rights exception occurs
     * @exception EntryNotFoundException
     *                if the entry is not found
     * @exception UMSException
     *                fails to remove the object 
     * @supported.api
     */
    public void removeChildPeopleContainer(PeopleContainer pc)
            throws AccessRightsException, EntryNotFoundException, UMSException {
        super.removeChild(pc);
    }

    /**
     * Gets the current number of users.
     * 
     * @return the current number of users 
     * @supported.api
     */
    public long getUserCount() throws UMSException {
        /*
         * String value = getAttribute( NUM_USER_ATTR_NAME ).getValue(); return
         * (new Long( value )).longValue();
         */
        SearchTemplate template = TemplateManager.getTemplateManager()
                .getSearchTemplate(BASIC_USER_SEARCH, getParentGuid());
        SearchResults results = getChildren(template, null);
        long count = 0;
        while (results.hasMoreElements()) {
            results.next();
            count++;
        }
        return count;
    }

    /**
     * Gets the current number of People Containers.
     * 
     * @return the current number of People Containers
     * @supported.api
     */
    public long getChildPeopleContainerCount() throws UMSException {
        /*
         * String value = getAttribute( NUM_PEOPLECONTAINER_ATTR_NAME
         * ).getValue(); return (new Long( value )).longValue();
         */
        SearchTemplate template = TemplateManager.getTemplateManager()
                .getSearchTemplate(BASIC_PEOPLECONTAINER_SEARCH,
                        getParentGuid());
        SearchResults results = getChildren(template, null);
        long count = 0;
        while (results.hasMoreElements()) {
            results.next();
            count++;
        }
        return count;
    }

    /**
     * Sets max user limit for a People Container.
     * 
     * @param limit
     *            number of users allowed in a People Container
     * @supported.api
     */
    public void setMaxUserLimit(long limit) throws UMSException {
        String value = (new Long(limit)).toString();
        setAttribute(new Attr(MAX_USER_ATTR_NAME, value));
    }

    /**
     * Sets max children People Container limit for a People Container.
     * 
     * @param limit
     *            number of children People Containers allowed
     * @supported.api
     */
    public void setMaxChildPeopleContainerLimit(long limit) throws UMSException
    {
        String value = (new Long(limit)).toString();
        setAttribute(new Attr(MAX_PEOPLECONTAINER_ATTR_NAME, value));
    }

    /**
     * Gets the user limit constraint.
     * 
     * @return user limit constraint 
     * @supported.api
     */
    public long getMaxUserLimit() throws UMSException {
        String value = getAttribute(MAX_USER_ATTR_NAME).getValue();
        return (new Long(value)).longValue();
    }

    /**
     * Gets the container limit constraint.
     * 
     * @return container limit constraint 
     * @supported.api
     */
    public long getMaxChildPeopleContainerLimit() throws UMSException {
        String value = getAttribute(MAX_PEOPLECONTAINER_ATTR_NAME).getValue();
        return (new Long(value)).longValue();
    }

    /**
     * Checks if a given user is a member of the container.
     * 
     * @param user
     *            User object to be checked
     * @return true if it is a member 
     * @supported.api
     */
    public boolean isMember(User user) throws UMSException {
        DN userdn = new DN(user.getDN());
        DN pcdn = new DN(this.getDN());

        if (userdn.getParent().equals(pcdn)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Return name of the People Container.
     * 
     * @return name of the People Container 
     * @supported.api
     */
    public String getName() throws UMSException {
        return (getAttribute(getNamingAttribute()).getValue());
    }

    /**
     * Resource keys
     */
    private static final Class _class = com.iplanet.ums.PeopleContainer.class;

    private static final String MAX_USER_ATTR_NAME = "nsMaxUsers";

    private static final String MAX_PEOPLECONTAINER_ATTR_NAME = 
        "nsMaxPeopleContainers";

    private static final String BASIC_USER_SEARCH = "BasicUserSearch";

    private static final String BASIC_PEOPLECONTAINER_SEARCH = 
        "BasicPeopleContainerSearch";

    static final String MULTIPLE_PEOPLE_CONTAINERS_SUPPORT = 
        "MultiplePeopleContainersSupport";

}
