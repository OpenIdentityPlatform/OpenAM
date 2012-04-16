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
 * $Id: Organization.java,v 1.3 2008/06/25 05:41:45 qcheng Exp $
 *
 */

package com.iplanet.ums;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;

import com.iplanet.services.ldap.AttrSet;
import com.iplanet.services.util.I18n;

/**
 * Represents an organization object. An organization can have child
 * organizations and groups, or other child objects.
 * 
 * <pre>
 *  o=vortex.com  (site)
 *       o=hp     (organization)
 *           uid=jdoe
 *           
 *       o=sun    (organization)
 *           ou=buyerclub  
 *               uid=joe    
 * </pre>
 *
 * @supported.api
 */
public class Organization extends PersistentObject {

    private static I18n i18n = I18n.getInstance(IUMSConstants.UMS_PKG);

    /**
     * No args constructor, used to contruct the right object as entries are
     * read from persistent storage
     * 
     */
    protected Organization() throws UMSException {
    }

    /**
     * Constructs Organization from supplied session and guid identifying the
     * organization to be constructed.
     * 
     * @param session
     *            authenticated session object maintained by Session Manager
     * @param guid
     *            globally unique identifier for the organization
     */
    Organization(Principal principal, Guid guid) throws UMSException {
        super(principal, guid);
        verifyClass();
    }

    /**
     * Constructs Organization object without a session. Unlike the constructor
     * with a session parameter , this one simply creates a Group object in
     * memory, using the default template. Call the save() method to save the
     * object to the persistent store.
     * 
     * @param attrSet
     *            attribute/value set
     */
    Organization(AttrSet attrSet) throws UMSException {
        this(TemplateManager.getTemplateManager().getCreationTemplate(_class,
                null), attrSet);
    }

    /**
     * Constructs Organization object without session. Unlike the constructor
     * with session, this one simply creates Organization object in memory. Call
     * the save() method to save the object to persistent storage.
     * 
     * @param template
     *            template for the organization
     * @param attrSet
     *            attribute/value set
     * @supported.api
     */
    public Organization(CreationTemplate template, AttrSet attrSet)
            throws UMSException {
        super(template, attrSet);
    }

    /**
     * Adds a new object to the organization.
     * 
     * @param object
     *            object to be added to the organization
     * @exception AccessRightsException
     *                if an access rights exception occurs
     * @exception EntryAlreadyExistsException
     *                if the entry already exists
     * @exception UMSException
     *                Fail to add the object
     * @supported.api
     */
    public void addChild(PersistentObject object) throws AccessRightsException,
            EntryAlreadyExistsException, UMSException {
        Principal principal = getPrincipal();

        if (principal == null) {
            String msg = i18n.getString(IUMSConstants.BAD_PRINCIPAL_HDL);
            throw new IllegalArgumentException(msg);
        } else if (object == null) {
            String msg = i18n.getString(IUMSConstants.BAD_OBJ_TO_ADD);
            throw new IllegalArgumentException(msg);
        }

        if (object instanceof User) {
            String pcId = getPeopleContainer((User) object);
            if (pcId != null) {
                PeopleContainer pc = new PeopleContainer(getPrincipal(),
                        new Guid(pcId));
                pc.addUser((User) object);
            } else {
                // no match and no default value found
                // For now, the user will be addedd to the organization.
                // May want to add to the default people
                // container(ou=People) instead.
                super.addChild(object);
            }

        } else {

            super.addChild(object);
        }
    }

    /**
     * Removes an object from the organization.
     * 
     * @param object
     *            object to be removed to the organization
     * @exception AccessRightsException
     *                if an access rights exception occurs
     * @exception EntryNotFoundException
     *                if the entry is not found
     * @exception UMSException
     *                Fail to remove the object
     * @supported.api
     */
    public void removeChild(PersistentObject object)
            throws AccessRightsException, EntryNotFoundException, UMSException {
        if (object != null && getPrincipal() != null) {
            super.removeChild(object);
        }
    }

    /**
     * Returns the name of the organization.
     * 
     * @return name of the organization
     * @supported.api
     */
    public String getName() throws UMSException {
        return getAttribute(getNamingAttribute()).getValue();
    }

    /**
     * Get roles associated with the organization
     * <P>
     * TODO: Not yet implemented
     * 
     * @return guid identifying roles object under the organization
     * 
     * public String[] getRolesArray(){ //getRoles() {
     * 
     * return null; }
     */

    /**
     * Get groups of which the organization is a member.
     * <P>
     * TODO: Not yet implemented
     * 
     * @return guids identifying groups that the organization is a member of
     * 
     * public String[] getGroups() { return null; }
     */

    /**
     * Gets all People Containers under the organization.
     * 
     * @return guids identifying People Containers under the organization
     * @exception UMSException
     *                Failure
     * @supported.api
     */
    public Collection getPeopleContainerGuids() throws UMSException {
        Collection pcs = new ArrayList();
        SearchTemplate template = TemplateManager.getTemplateManager()
                .getSearchTemplate("BasicPeopleContainerSearch", getGuid());
        SearchResults results = search(template, null);
        while (results.hasMoreElements()) {
            pcs.add(results.next().getGuid());
        }
        return pcs;
    }

    /**
     * Gets People Container associated with the user.
     * 
     * @param user
     *            user object to lookup
     * @return guid identifying People Container associated with the user, null
     *         if no match found
     * @exception UMSException
     *                Failure
     */
    private String getPeopleContainer(User user) throws UMSException {
        PCMappingTable mt = PCMappingTable.getPCMappingTable(this);
        return mt.getPeopleContainer(user);
    }

    /**
     * Adds rule for determining which People Container the user is supposed to
     * be in.
     * 
     * @param filter
     *            filter representation of the rule. Accepts filter string with
     *            the following format:
     *            <P>
     * 
     * <PRE>
     * 
     * &ltfilter&gt ::= &ltand&gt | &ltitem&gt &ltand&gt ::= '(' '&'
     *            &ltitemlist&gt ')' &ltitemlist&gt ::= &ltitem&gt | &ltitem&gt
     *            &ltitemlist&gt &ltitem&gt ::= '(' &ltattr&gt '=' &ltvalue&gt
     *            ')'
     * 
     * </PRE>
     * 
     * @param guid
     *            guid of the People Container to which the rule is applied.
     * @exception UMSException
     *                Failure
     * @supported.api
     */
    public void addPeopleContainerRule(Guid guid, String filter)
            throws UMSException {
        PCMappingTable mt = PCMappingTable.getPCMappingTable(this);
        mt.addRule(guid, filter);
    }

    /**
     * Removes the rule applying to the given People Container guid with the
     * given filter string.
     * 
     * @param filter
     *            filter string of the rule to be removed
     * @param guid
     *            guid of which the rule applies to
     * @exception UMSException
     *                Failure 
     * @supported.api
     */
    public void removePeopleContainerRule(Guid guid, String filter)
            throws UMSException {
        PCMappingTable mt = PCMappingTable.getPCMappingTable(this);
        mt.removeRule(guid, filter);
    }

    /**
     * Sets the default People Container for user entries under the
     * organization.
     * 
     * @param guid
     *            guid of the default People Container
     * @exception UMSException
     *                Failure
     * @supported.api
     */
    public void setDefaultPeopleContainer(Guid guid) throws UMSException {
        PCMappingTable mt = PCMappingTable.getPCMappingTable(this);
        mt.setDefault(guid);
    }

    private static final Class _class = com.iplanet.ums.Organization.class;
}
