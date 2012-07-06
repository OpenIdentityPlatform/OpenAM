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
 * $Id: PersistentObject.java,v 1.8 2009/07/02 20:27:01 hengming Exp $
 *
 */

/**
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.iplanet.ums;

import com.iplanet.am.sdk.AMException;
import java.io.Serializable;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Locale;

import com.sun.identity.shared.ldap.LDAPAttribute;
import com.sun.identity.shared.ldap.LDAPModification;
import com.sun.identity.shared.ldap.util.DN;
import com.sun.identity.shared.ldap.util.RDN;

import com.sun.identity.shared.debug.Debug;
import com.iplanet.services.ldap.Attr;
import com.iplanet.services.ldap.AttrSet;
import com.iplanet.services.ldap.ModSet;
import com.iplanet.services.ldap.aci.ACI;
import com.iplanet.services.ldap.aci.ACIParseException;
import com.iplanet.services.util.I18n;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.iplanet.ums.validation.Validation;

/**
 * Represents a persistent object in UMS. This is the base class for all objects
 * that Object Management Module (OMM) manages in UMS.
 *
 * @supported.api
 */
public class PersistentObject implements ISearch, Serializable, IUMSConstants {

    public static final String COMPUTED_MEMBER_ATTR_NAME = "nsRole";

    private static Debug debug;
    static {
        debug = Debug.getInstance(IUMSConstants.UMS_DEBUG);
    }

    private static I18n i18n = I18n.getInstance(IUMSConstants.UMS_PKG);

    /**
     * Default Constructor
     */
    protected PersistentObject() {
        super();
    }

    /**
     * Constructor for PersistentObject given an authenticated session and guid,
     * to instantiate from persistent storage.
     * 
     * @param session
     *            Valid and authenticated session
     * @param guid
     *            Globally unique identifier for the entity
     * @throws UMSException
     *             for failure to find the object
     * 
     */
    PersistentObject(Principal principal, Guid guid) throws UMSException {
        String dn = guid.getDn();

        if (principal == null || dn == null) {
            String msg;
            if (principal == null) {
                msg = i18n.getString(IUMSConstants.BAD_PRINCIPAL_HDL);
            } else {
                msg = i18n.getString(IUMSConstants.BAD_GUID);
            }

            throw new IllegalArgumentException(msg);
        }
        setGuid(guid);
        setPrincipal(principal);

        // If reading in the object turns out to be too expensive, comment
        // out the read. The read method will throw an exception for
        // an unfound object due to access rights or bad guid.
        // TODO: to be reviewed if we need to comment out this
        //
        read();
    }

    /**
     * Constructor for in memory object to be added to the system. You can make
     * the object persistent two ways:
     * <P>
     * 
     * 1) call add on parent object (recommended)
     * <P>
     * 2) call save(...) method after all attributes for in memory object are
     * set up properly.
     * <P>
     * 
     * @param template
     *            Object creation template. The template holds all the default
     *            values such as objectclass and requirted attributes to be
     *            supplied
     * @param attrSet
     *            Attribute set to construct the object in memory
     * @throws UMSException
     *             for failure to construct the object. The given attrSet needs
     *             to provide the required attribute(s) defined in template
     *
     * @supported.api
     */
    public PersistentObject(CreationTemplate template, AttrSet attrSet)
            throws UMSException {
        m_attrSet = attrSet;
        if (template == null) {
            throw new UMSException(BAD_TEMPLATE);
        }
        m_namingAttribute = template.getNamingAttribute();
        template.validateAttrSet(attrSet);
        template.validateAttributes(attrSet);
    }

    /**
     * Constructor for in memory object to be added to the system. You can make
     * the object persistent two ways:
     * <P>
     * 
     * 1) call add on parent object (recommended)
     * <P>
     * 2) call save(...) method after all attributes for in memory object are
     * set up properly.
     * <P>
     * 
     * @param template
     *            Object creation template. The template holds all the default
     *            values such as objectclass and requirted attributes to be
     *            supplied
     * @param attrSet
     *            Attribute set to construct the object in memory
     * @param namingAttribute
     *            Internal naming attribute (ex: "ou").
     * @throws UMSException
     *             for failure to construct the object. The given attrSet needs
     *             to provide the required attribute(s) defined in template
     */
    PersistentObject(CreationTemplate template, AttrSet attrSet,
            String namingAttribute) throws UMSException {

        m_attrSet = attrSet;
        if (template == null) {
            throw new UMSException(BAD_TEMPLATE);
        }
        template.validateAttrSet(attrSet);
        template.validateAttributes(attrSet);
        m_namingAttribute = namingAttribute;
    }

    /**
     * Gets an attribute of the object. If the attribute is not in memory, the
     * object is refreshed from persistent storage.
     * 
     * @param attrName
     *            Name of the attribute to be queried
     * @return Attribute value
     *
     * @supported.api
     */
    public Attr getAttribute(String attrName) {
        Attr attr = getAttributeFromCache(attrName);
        if ((attr == null) && isAttributeNotRead(attrName)
                && (getGuid() != null) && (getPrincipal() != null)) {
            try {
                attr = readAttributeFromDataStore(attrName);
            } catch (UMSException ex) {
                if (debug.warningEnabled()) {
                    debug.warning("PersistentObject.getAttribute: for DN: " +
                        getGuid() + " attribute: " + attrName, ex);
                }
            }
        }
        return attr;
    }

    /**
     * Gets an attribute value with a given locale
     * 
     * @param attrName
     *            Name of the attribute
     * @param locale
     *            Locale of attribute to be retrieved
     * @return Attribute value with the specified locale. May return null if the
     *         attribute with locale not found. No fallback mechanism is
     *         provided
     * @see com.iplanet.ums.PersistentObject#getAttribute(String)
     *
     * @supported.api
     */
    public Attr getAttribute(String attrName, Locale locale)
            throws UMSException {

        if (locale == null) {
            return getAttribute(attrName);
        }

        return getAttribute(Attr.getName(attrName, locale));
    }

    /**
     * Gets attribute values
     * 
     * @param attrs
     *            Array of strings representing attribute names
     * @return attribute value set for the return values
     * @see #getAttribute(String)
     *
     * @supported.api
     */
    public AttrSet getAttributes(String[] attrs) throws UMSException {
        return getAttributes(attrs, false);
    }

    /**
     * Gets attribute values
     * 
     * @param attrs
     *            Array of strings representing attribute names
     * @param cacheOnly
     *            if true, read attributes from cache only without contacting
     *            data stroe
     * @return attribute value set for the return values
     * @see #getAttribute(String)
     *
     * @supported.api
     */
    public AttrSet getAttributes(String[] attrs, boolean cacheOnly)
            throws UMSException {
        if (attrs == null) {
            throw new IllegalArgumentException(i18n
                    .getString(IUMSConstants.BAD_ATTRNAMES));
        }
        AttrSet attrSet = new AttrSet();
        if (!cacheOnly) {
            Collection attributesNotInCache = findAttributesNotRead(attrs);
            if ((!attributesNotInCache.isEmpty()) && (getGuid() != null)
                    && (getPrincipal() != null)) {
                readAttributesFromDataStore(attributesNotInCache);
            }
        }
        int length = attrs.length;
        for (int i = 0; i < length; i++) {
            Attr attr = getAttributeFromCache(attrs[i]);
            if (attr != null) {
                attrSet.add(attr);
            }
        }
        return attrSet;
    }

    /**
     * Returns attribute values with a specified locale.
     * 
     * @param attrNames Attribute names
     * @param locale Locale of the attributes to be queried
     * @return Attribute value set. May return null value for attribute(s) with
     *         unfound locale. No fallback mechanism is provided.
     * @see #getAttribute(String)
     *
     * @supported.api
     */
    public AttrSet getAttributes(String attrNames[], Locale locale)
            throws UMSException {
        if (locale == null)
            return getAttributes(attrNames);

        String[] namesWithLocale = new String[attrNames.length];

        for (int i = 0; i < attrNames.length; i++) {
            namesWithLocale[i] = Attr.getName(attrNames[i], locale);
        }

        return getAttributes(namesWithLocale);
    }

    /**
     * Set an attribute value for the entity.
     * <P>
     * IMPORTANT: To make the changes persistent, you need to call the save
     * method to save the changes.
     * <P>
     * 
     * @param attr
     *            Attribute and value
     *
     * @supported.api
     */
    public void setAttribute(Attr attr) {

        if (attr == null || (attr.getName() == null)) {
            throw new IllegalArgumentException(i18n
                    .getString(IUMSConstants.ADD_NULL_OBJ));
        }

        checkCache();

        if (m_attrSet == null)
            m_attrSet = new AttrSet();

        if (m_attrSet.contains(attr.getName())) {
            modify(attr, ModSet.REPLACE);
        } else {
            modify(attr, ModSet.ADD);
        }
    }

    /**
     * Sets an attribute value with a given locale for the entity.
     * <P>
     * IMPORTANT: To make the changes persistent, you need to call the save
     * method to save the changes.
     * <P>
     * 
     * @param attr
     *            Attribute and value
     * @param locale
     *            Intended locale of the attribute to be set
     *
     * @supported.api
     */
    public void setAttribute(Attr attr, Locale locale) {

        if (locale == null) {
            setAttribute(attr);
            return;
        }

        // TODO: ??? should check if adding Attr.setName method makes more
        // sense than recopying the data values of the passed in attribute
        //
        Attr attrWithLocale = new Attr(
                Attr.getName(attr.getBaseName(), locale));
        attrWithLocale.addValues(attr.getStringValues());
        setAttribute(attrWithLocale);
    }

    /**
     * Changes user password.
     * 
     * @param entryDN DN of the profile whose template is to be set
     * @param attrName password attribute name
     * @param oldPassword old password
     * @param newPassword new password
     * @throws AMException if an error occurs when changing user password
     * @throws SSOException If user's single sign on token is invalid.
     */
    public void changePassword(String entryDN, String attrName,
        String oldPassword, String newPassword) throws UMSException {

        DataLayer.getInstance().changePassword(getGuid(), attrName,
            oldPassword, newPassword);
    }

    /**
     * Removes attribute value for the entity.
     * <P>
     * IMPORTANT: To make the changes persistent, you need to call the save
     * method to save the changes.
     * <P>
     * 
     * @param attr
     *            Attribute to be removed
     *
     * @supported.api
     */
    public void removeAttribute(Attr attr) {
        checkCache();
        if (m_attrSet == null || m_attrSet.size() == 0) {
            return;
        }

        modify(attr, ModSet.DELETE);
    }

    /**
     * Gets names for all available attributes for this entity
     * 
     * @return Array of strings representing attribute names
     * 
     * @supported.api
     */
    public String[] getAttributeNames() {
        if (m_principal != null && m_guid != null && m_attrSet == null) {
            try {
                read();
            } catch (UMSException e) {
                // TODO log exception here.
                if (debug.messageEnabled()) {
                    debug.message("PersistentObject.getAttributeNames: " +
                            "UMSException: " + e.getMessage());
                }
            }
        }

        if (m_attrSet != null) {
            return m_attrSet.getAttributeNames();
        } else {
            return null;
        }
    }

    /**
     * Modifies attribute values for the entity.
     * <P>
     * IMPORTANT: To make the changes persistent, you need to call the save
     * method to save the changes.
     * <P>
     * 
     * @param modSet
     *            Set of modification of attributes
     * @see ModSet
     *
     * @supported.api
     */
    public void modify(ModSet modSet) {
        checkCache();
        if (m_modSet == null) {
            m_modSet = new ModSet();
        }
        if (m_attrSet == null) {
            m_attrSet = new AttrSet();
        }

        int nMods = modSet.size();
        LDAPModification mod = null;

        for (int i = 0; i < nMods; i++) {
            mod = modSet.elementAt(i);
            switch (mod.getOp()) {
            case ModSet.ADD:
                m_attrSet.add(new Attr(mod.getAttribute()));
                break;
            case ModSet.DELETE:
                if (mod.getAttribute().size() == 0) {
                    m_attrSet.remove(mod.getAttribute().getName());
                } else {
                    LDAPAttribute attr = mod.getAttribute();
                    Enumeration en = attr.getStringValues();
                    while (en.hasMoreElements()) {
                        m_attrSet.remove(attr.getName(), (String) en
                                .nextElement());
                    }
                }
                break;
            case ModSet.REPLACE:
                m_attrSet.replace(new Attr(mod.getAttribute()));
                break;
            default:
                break;
            }

            m_modSet.add(mod.getOp(), mod.getAttribute());

        }
    }

    /**
     * Modifies the values of a single attribute for the entity.
     * <P>
     * IMPORTANT: To make the changes persistent, you need to call the save
     * method to save the changes.
     * <P>
     * 
     * @param attr
     *            Attribute value to be modified
     * @param op
     *            Operation type in the modification. Input values include
     * 
     * <pre>
     *               ModSet.ADD,
     *               ModSet.DELETE,
     *               ModSet.REPLACE
     * </pre>
     * 
     * @see ModSet
     *
     * @supported.api
     */
    public void modify(Attr attr, int op) {
        ModSet modSet = new ModSet();

        modSet.add(op, attr.toLDAPAttribute());
        modify(modSet);
    }

    /**
     * Modify a single attribute for the entity.
     * <P>
     * IMPORTANT: To make the changes persistent, you need to call the save
     * method to save the changes.
     * <P>
     * 
     * @param attrName
     *            Attribute name of the attribute to be modified
     * @param value
     *            String value of the attribute
     * @param op
     *            Operation type in the modification. Input values include
     * 
     * <pre>
     *                   ModSet.ADD,
     *                   ModSet.DELETE,
     *                   ModSet.REPLACE
     * </pre>
     * 
     * @see ModSet
     *
     * @supported.api
     */
    public void modify(String attrName, String value, int op) {
        ModSet modSet = new ModSet();

        modSet.add(op, new LDAPAttribute(attrName, value));
        modify(modSet);
    }

    /**
     * Get GUID of the given entity
     * 
     * @return the GUID.
     *
     * @supported.api
     */
    public Guid getGuid() {
        return m_guid;
    }

    /**
     * Renames the RDN to a new value. Note: The modified or added attribute
     * values are not saved by this call.
     * 
     * @param newRDN
     *            the new RDN value
     * @param deleteOldName
     *            if true old RDN value is deleted, otherwise the old value is
     *            retained.
     * 
     * @throws AccessRightsException
     *             if an access rights exception occurs.
     * @throws EntryNotFoundException
     *             if the entry is not found
     * @throws UMSException
     *             on failure to save to persistent storage
     *
     * @supported.api
     */
    public void rename(String newRDN, boolean deleteOldName)
            throws AccessRightsException, EntryNotFoundException, UMSException {
        String required = null;

        if (m_principal == null) {
            required = "principal";
        } else if (m_guid == null) {
            required = "guid";
        }
        if (required != null) {
            // TODO: This is not an illegal argument case. Should be
            // a more sophisticated exception.
            String args[] = new String[1];

            args[0] = required;
            String msg = i18n.getString(IUMSConstants.NO_REQUIRED, args);
            throw new UMSException(msg);
        }

        try {
            DataLayer.getInstance().rename(getPrincipal(), getGuid(), newRDN,
                    deleteOldName);
        } finally {
            // Must be set to new ID since the orignal DN would have changed now
            RDN rdn = new RDN(newRDN);
            DN parentDN = (new DN(m_guid.toString())).getParent();
            parentDN.addRDN(rdn);
            m_guid.setDn(parentDN.toRFCString());
        }
    }

    /**
     * Save the modification(s) to the object. Save the changes made so far for
     * the persistent object. In other words, make the changes persistent for
     * the object.
     * <P>
     * This save method takes no parameter. You use this save method when the
     * object is already instantiated. For example,
     * 
     * <pre>
     * User user = (User) UMSObject.getObject(principal, id);
     * user.modify(&quot;telephonenumber&quot;, 
     *      &quot;650.937.4444&quot;, ModSet.REPLACE);
     * user.save();
     * </pre>
     * 
     * <P>
     * 
     * @throws AccessRightsException
     *             if an access rights exception occurs.
     * @throws EntryNotFoundException
     *             if the entry is not found
     * @throws UMSException
     *             on failure to save to persistent storage
     *
     * @supported.api
     */
    public void save() throws AccessRightsException, EntryNotFoundException,
            UMSException {
        String required = null;
        if (m_modSet == null) {
            return;
        }
        if (m_principal == null) {
            required = "principal";
        } else if (m_guid == null) {
            required = "guid";
        }
        if (required != null) {
            // TODO: This is not an illegal argument case. Should be
            // a more sophisticated exception.
            String args[] = new String[1];

            args[0] = required;
            String msg = i18n.getString(IUMSConstants.NO_REQUIRED, args)
                    + " - "
                    + i18n.getString(IUMSConstants.OBJECT_NOT_PERSISTENT);
            throw new UMSException(msg);
        }

        try {
            DataLayer.getInstance().modify(getPrincipal(), getGuid(), m_modSet);
        } finally {
            // Remember to set this to null as the changes
            // are made persistent after this call
            m_modSet = null;
        }
    }

    /**
     * Gets the attribute name that specifies the ID (or RDN in terms of DN in
     * ldap) component in an object. Subclasses may choose to override this
     * function. For instance, User takes either "uid" or "cn" for its
     * identification
     * <P>
     * 
     * @return Attribute name for identification
     *
     * @supported.api
     */
    public String getNamingAttribute() {

        if (m_guid == null) {
            return m_namingAttribute;
        }

        DN dn = new DN(getDN());

        String[] components = dn.explodeDN(false);

        if (components != null && components.length > 0) {
            RDN rdn = new RDN(components[0]);
            return rdn.getTypes()[0];
        }
        return null;
    }

    /**
     * Gets the parent object
     * 
     * @return PersistentObject representing the parent object
     * @throws UMSException
     *             on failure instantiating the parent object
     *
     * @supported.api
     */
    public PersistentObject getParentObject() throws UMSException {

        if (m_guid == null || m_principal == null) {
            String msg;

            if (m_principal == null) {
                msg = i18n.getString(IUMSConstants.BAD_PRINCIPAL_HDL);
            } else {
                msg = i18n.getString(IUMSConstants.BAD_GUID);
            }

            throw new IllegalArgumentException(msg);
        }

        PersistentObject parent = UMSObject.getObject(getPrincipal(),
                getParentGuid());
        return parent;
    }

    /**
     * Adds a child object to the persistent object container. All persistent
     * objects can add objects as a container. To override this behavior or
     * impose restrictions override the add method in a subclass so that e.g.
     * User.add( object ) is restricted or disallowed in certain ways.
     * 
     * @param object Child object to be added to this persistent container.
     * @throws AccessRightsException if an access rights exception occurs.
     * @throws EntryAlreadyExistsException if the entry already exists.
     * @throws UMSException if fail to add the given child object to the 
     *         container. Possible causes include
     *         <code>EntryAlreadyExists</code>, <code>AccessRights</code>
     *         violation.
     *
     * @supported.api
     */
    public void addChild(PersistentObject object) throws AccessRightsException,
            EntryAlreadyExistsException, UMSException {
        if (object == null) {
            String args[] = new String[1];

            args[0] = this.toString();
            String msg = i18n.getString(IUMSConstants.ADD_NULL_OBJ, args);

            throw new IllegalArgumentException(msg);
        }

        String idAttr = object.getNamingAttribute();
        String idValue = null;
        Attr idAttrObj = object.getAttribute(idAttr);
        if (idAttrObj != null) {
            idValue = idAttrObj.getValue();
        } else {
            throw new UMSException(BAD_NAMING_ATTR + idAttr);
        }

        if (idAttr == null || idValue == null || idValue.length() == 0) {
            String args[] = new String[1];

            args[0] = object.toString();
            String msg = i18n
                    .getString(IUMSConstants.COMPOSE_GUID_FAILED, args);

            throw new IllegalArgumentException(msg);
        }

        String childStr = null;

        if (getGuid().getDn().length() > 0) {
            childStr = idAttr + "=" + idValue + "," + getGuid().getDn();
        } else {
            childStr = idAttr + "=" + idValue;
        }

        Guid childGuid = new Guid(childStr);
        object.setGuid(childGuid);

        // Validation was done during the creation of the object
        // Validation.validateAttributes( object.getAttrSet(),
        // object.getClass(), this.getGUID() );

        DataLayer.getInstance().addEntry(getPrincipal(), childGuid,
                object.getAttrSet());

        object.setModSet(null);
        object.setPrincipal(getPrincipal());

        EntityManager em = EntityManager.getEntityManager();
        try {
            em.execute(getPrincipal(), object, m_guid);
        } catch (UMSException e) {
            // TODO - we should log error...
            if (debug.messageEnabled()) {
                debug.message("PersistentObject.addChild : UMSException : "
                        + e.getMessage());
            }
        }
    }

    /**
     * Removes a child object from a persistent object container. It is
     * important for constraints to be applied in overriding this method in
     * subclasses of PersistentObject. For example, Organization may choose not
     * to allow remove( object ) when object is an organization.
     * 
     * @param object Child object to be removed.
     * @throws AccessRightsException if an access rights exception occurs.
     * @throws EntryNotFoundException if the entry is not found.
     * @throws UMSException if fail to remove the child object. Possible causes
     *         includes EntryNotFount, AccessRights violation etc.
     *
     * @supported.api
     */
    public void removeChild(PersistentObject object)
            throws AccessRightsException, EntryNotFoundException, UMSException {
        String childStr;
        if (object == null) {
            String args[] = new String[1];

            args[0] = this.toString();
            String msg = i18n.getString(IUMSConstants.DEL_NULL_OBJ, args);
            throw new IllegalArgumentException(msg);
        }

        childStr = object.getGuid().getDn();

        // If this is an in-memory object, attempt to compose the guid
        // for the child object
        //
        if (childStr == null) {
            String idAttr = object.getNamingAttribute();
            String idValue = object.getAttribute(idAttr).getValue();

            if (idAttr == null || idValue == null || idValue.length() == 0) {
                String args[] = new String[1];

                args[0] = object.toString();
                String msg = i18n.getString(IUMSConstants.COMPOSE_GUID_FAILED,
                        args);

                throw new IllegalArgumentException(msg);
            }

            if (getGuid().getDn().length() > 0) {
                childStr = idAttr + "=" + idValue + "," + getGuid();
            } else {
                childStr = idAttr + "=" + idValue;
            }
        }

        DN parentEntry = new DN(getDN());
        DN childEntry = new DN(childStr);

        if (!childEntry.isDescendantOf(parentEntry)) {
            String msg = i18n.getString(IUMSConstants.BAD_CHILD_OBJ);
            // TODO: need review. Should we throw something
            // more meaningful
            throw new IllegalArgumentException(msg);
        }

        DataLayer.getInstance().deleteEntry(getPrincipal(), new Guid(childStr));

        // TODO: ??? do we need to mark the object that has been deleted
        // with an invalid session
        //
        object.setGuid(new Guid("DELETED"));
        object.setPrincipal(null);
    }

    /**
     * Removes an object given its unique ID. This method expects the given
     * child ID is a descendant (something being contained) in "this" object
     * 
     * @param childGuid Unique entry identification for the child to be removed.
     * @throws AccessRights if an access rights exception occurs.
     * @throws EntryNotFoundException if the entry is not found.
     * @throws UMSException if failure to remove the entry from the persistent
     *         store. Possible causes include AccessRights violation,
     *         EntryNotFound etc.
     *
     * @supported.api
     */
    public void removeChild(Guid childGuid) throws AccessRightsException,
            EntryNotFoundException, UMSException {
        DN parentEntry = new DN(getDN());
        DN childEntry = new DN(childGuid.getDn());

        if (!childEntry.isDescendantOf(parentEntry)) {
            String msg = i18n.getString(IUMSConstants.BAD_CHILD_OBJ);

            throw new IllegalArgumentException(msg);
        }

        DataLayer.getInstance().deleteEntry(getPrincipal(), childGuid);
    }

    /**
     * Remove itself from the persistent store. This method removes the object
     * at hand from the persistent storage but keeps its internal data so that
     * the ums client can save it to somewhere else or make reference to its
     * internal data
     * <P>
     * 
     * @throws AccessRights
     *             Exception if an access rights exception occurs.
     * @throws EntryNotFoundException
     *             if the entry is not found
     * @throws UMSException
     *             from UMSSession.removeObject( principal, guid)
     *
     * @supported.api
     */
    public void remove() throws AccessRightsException, EntryNotFoundException,
            UMSException {
        // REVIEW: should we keep this method where an object can delete itself
        // we don't allow an object to add itself ^%$#@

        // If this is an in memory object with no reference to an entry,
        // don't do anything
        //
        if (m_guid == null || m_principal == null)
            return;

        // Remove the object from persitent store
        //
        DataLayer.getInstance().deleteEntry(getPrincipal(), getGuid());

        // Now reset it as a memory object with no reference to an entry on
        // the persistent store. Possible use of this object in a move
        // implementation. Call save(principal, namingAttr, parentGUID) to
        // achieve
        // the more functionality
        //
        setGuid(null);
        setPrincipal(null);
    }

    /**
     * Gets a string representation of the object
     * 
     * @return String representation of the object
     *
     * @supported.api
     */
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Object ID        :").append(m_guid).append("\n");
        sb.append("Naming attribute :").append(getNamingAttribute()).append("\n");
        sb.append("Class            :").append(getClass().getName()).append("\n");
        sb.append("Principal        :").append(m_principal).append("\n");
        sb.append("Attribute Set    :").append(m_attrSet).append("\n");
        return sb.toString();
    }

    /**
     * Gets the parent guid
     * 
     * @return string representation of the parent guid public String
     *         getParentID() { return getParentID(null); }
     */

    /**
     * Gets the parent guid
     * 
     * @return String representation of the parent guid
     */
    public Guid getParentGuid() {
        if (m_guid == null || m_principal == null) {
            return null;
        }

        DN dn = new DN(getDN());
        return new Guid(dn.getParent().toString());
    }

    /**
     * Gets the immediate children, subject to search filter constraints. Only
     * the IDs and object classes of each child are returned.
     * <P>
     * 
     * @param filter
     *            Search filter
     * @param searchControl
     *            Search control
     * @return Result child IDs in Vector
     * @throws InvalidSearchFilterException
     *             if the search filter is invalid
     * @throws UMSException
     *             on searching for immediate children in the container
     *
     * @supported.api
     */
    public SearchResults getChildren(String filter, SearchControl searchControl)
            throws InvalidSearchFilterException, UMSException {
        // default is one level search scope in getChildren
        //
        int scope = SearchControl.SCOPE_ONE;
        if (searchControl != null) {
            scope = searchControl.getSearchScope(scope);
        }

        SearchResults results = DataLayer.getInstance().searchIDs(
                getPrincipal(), getGuid(), scope, filter, searchControl);
        results.setPrincipal(getPrincipal());
        return results;
    }

    /**
     * Gets the immediate children, returning only specified attributes
     * 
     * @param filter
     *            Search filter
     * @param resultAttributeNames
     *            Names of attributes to retrieve
     * @param searchControl
     *            Search control object
     * @return SearchResults
     * @throws InvalidSearchFilterException
     *             on invalid search filter
     * @throws UMSException
     *             on failure with searhing
     * 
     * @supported.api
     */
    public SearchResults getChildren(String filter,
            String[] resultAttributeNames, SearchControl searchControl)
            throws InvalidSearchFilterException, UMSException {

        // default is one level search scope in getChildren
        //
        int scope = SearchControl.SCOPE_ONE;
        if (searchControl != null) {
            scope = searchControl.getSearchScope(scope);
        }

        SearchResults searchResults = DataLayer.getInstance().search(
                getPrincipal(), getGuid(), scope, filter, resultAttributeNames,
                false, searchControl);
        searchResults.setPrincipal(getPrincipal());

        return searchResults;
    }

    /**
     * Gets all immediate children under current node based on search criteria
     * specified in template, and returns attributes specified there. Search
     * behavior is controlled by searchControl.
     * <P>
     * 
     * Returning attributes are determined by the search template
     * <P>
     * 
     * @param template
     *            Search template
     * @param searchControl
     *            Search control, use default setting if searchControl == null
     * @throws UMSException
     *             on failure with searching
     *
     * @supported.api
     */
    public SearchResults getChildren(SearchTemplate template,
            SearchControl searchControl) throws UMSException {
        return getChildren(template.getSearchFilter(), template
                .getAttributeSet().getAttributeNames(), searchControl);
    }

    /**
     * Gets only the IDs and object classes of all objects at the current level
     * and below which match the search filter.
     * 
     * @param filter
     *            Search filter
     * @param searchControl
     *            Search control object
     * @throws InvalidSearchFilterException
     *             on invalid search filter
     * @throws UMSException
     *             on failure with searching
     *
     * @supported.api
     */
    public SearchResults search(String filter, SearchControl searchControl)
            throws InvalidSearchFilterException, UMSException {

        // Default search scope is Subtree type of search
        //
        int scope = SearchControl.SCOPE_SUB;
        if (searchControl != null) {
            scope = searchControl.getSearchScope(scope);
        }

        SearchResults results = DataLayer.getInstance().searchIDs(
                getPrincipal(), getGuid(), scope, filter, searchControl);
        results.setPrincipal(getPrincipal());
        return results;
    }

    /**
     * Gets the specified attributes of all objects at the current level and
     * below which match the search filter.
     * 
     * @param filter
     *            Search filter
     * @param resultAttributeNames
     *            Names of attributes to retrieve
     * @param searchControl
     *            Search control object
     * @return SearchResults
     * @throws InvalidSearchFilterException
     *             on invalid search filter
     * @throws UMSException
     *             on failure with searching
     * 
     *
     * @supported.api
     */
    public SearchResults search(String filter, String[] resultAttributeNames,
            SearchControl searchControl) throws InvalidSearchFilterException,
            UMSException {

        // Default search scope is Subtree type of search
        //
        int scope = SearchControl.SCOPE_SUB;
        if (searchControl != null) {
            scope = searchControl.getSearchScope(scope);
        }

        SearchResults results = DataLayer.getInstance().search(getPrincipal(),
                getGuid(), scope, filter, resultAttributeNames, false,
                searchControl);
        results.setPrincipal(getPrincipal());
        return results;
    }

    /**
     * Gets the attributes specified in the template for all objects at the
     * current level and below which match the search filter in the template.
     * Search behavior is controlled by searchControl.
     * <P>
     * 
     * @param template
     *            Search template
     * @param searchControl
     *            Search control, use default setting if searchControl == null
     * @throws UMSException
     *             on failure to search
     *
     * @supported.api
     */
    public SearchResults search(SearchTemplate template,
            SearchControl searchControl) throws UMSException {
        return search(template.getSearchFilter(), template.getAttributeSet()
                .getAttributeNames(), searchControl);
    }

    /**
     * Gets the DN of the entity
     * 
     * @return String representing the distinguished name of the entry
     * 
     */
    public String getDN() {
        if (m_guid != null)
            return m_guid.getDn();
        else
            return null;
    }

    /**
     * Sets the GUID of the entity; used within the package
     * 
     * @param guid
     *            String representation of guid
     */
    protected void setGuid(Guid guid) {

        m_guid = guid;
    }

    /**
     * Return the authenticated principal that is used to instantiate this
     * object
     * 
     * @return authenticated principal that is used to instantiate this object,
     *         return null if no authenticated principal is associated with this
     *         object
     */
    Principal getPrincipal() {
        return m_principal;
    }

    /**
     * Set current authenticated session; used within the package
     * 
     * @param session
     *            A valid authenticated session
     */
    void setPrincipal(Principal principal) {
        m_principal = principal;
    }

    /**
     * Sets the attribute set
     * 
     * @param attrSet
     *            The attribute set to be assigned as a reference (not a deep
     *            copy)
     */
    protected void setAttrSet(AttrSet attrSet) {
        m_attrSet = attrSet;
    }

    /**
     * Gets the attribute set as a reference, not a deep copy
     * 
     * @return The in-memory attribute set
     */
    protected AttrSet getAttrSet() {
        return m_attrSet;
    }

    /**
     * Checks if javaclass of the persistent object is the expected class
     * defined in its objectclass attribute.
     * 
     * @throws UMSException
     *             when the objectclass maps to a javaclass different from the
     *             one being constructed.
     * @see com.iplanet.ums.TempateManager#getJavaClassForEntry
     */
    void verifyClass() throws UMSException {
        Class expectedClass = 
            TemplateManager.getTemplateManager().getJavaClassForEntry(
                    this.getGuid().getDn(), this.getAttrSet());

        // TODO: need to be reviewed and see if subclasses of entity are
        // allowed.
        // e.g. PersistentObject -> User -> MailUser kind of inheritence, do we
        // accept the formation of User class for MailUser.
        //
        if (this.getClass() != expectedClass) {
            String msg = i18n.getString(IUMSConstants.UNMATCHED_CLASS);
            // TODO: review for better exception
            throw new IllegalArgumentException(msg);
        }
    }

    /**
     * Maps to a DN from naming attribute value of a persistent object.
     * 
     * @param namingAttribute Naming attribute of the object.
     * @param name Naming attribute value of the object.
     * @param parentID Array of its parent names, all assumed to take 
     *        <code>o</code> as the naming attributes.
     */
    static public String idToDN(
        String namingAttribute,
        String name,
        String[] parentID
    ) {
        StringBuilder sb = new StringBuilder();

        sb.append(namingAttribute).append("=").append(name);
        for (int i = 0; i < parentID.length; i++) {
            if (parentID[i] != null) {
                // TODO: ??? This is hardcoded to take "o=something" as the
                // parent node(s). Needs a flexible scheme to handle
                // flexible DIT.
                sb.append(",o=").append(parentID[i]);
            }
        }

        return sb.toString();
    }

    /**
     * Maps a dn to guid
     * <P>
     * TODO: Not yet implemented
     * <P>
     */
    static String dnToGuid(String dn) {
        // TODO: Need to fill in base 64 encoding <P>
        //
        return dn;
    }

    /**
     * Maps a guid to dn
     * <P>
     * TODO: Not yet implemented
     * <P>
     */
    static String guidToDN(String guid) {
        // TODO: Need to fill in base 64 encoding <P>
        //
        return guid;
    }

    /**
     * Reads in the object from persistent store, assuming that the guid and
     * session are valid
     * 
     * @throws UMSException
     *             on failure in reading the object from persistent store
     */
    synchronized private void read() throws UMSException {
        if (m_principal == null || m_guid == null) {
            // TODO: there should be some warning to the client here
            return;
        }

        m_attrSet = DataLayer.getInstance().read(getPrincipal(), getGuid());
    }

    private void checkCache() {
        if (m_principal != null && m_guid != null && m_attrSet == null) {
            try {
                read();
            } catch (UMSException e) {
                // TODO: there should be some warning to the client here
                if (debug.messageEnabled()) {
                    debug.message("PersistentObject.checkCache() : " +
                            "UMSException : " + e.getMessage());
                }
            }
        }
    }

    /**
     * Internal use only, set the internal modset. Can be used to nullify the
     * internal state of m_modSet
     * 
     * @param modSet
     *            Modification Set to be used for the internal
     *            <code>modSet</code>.
     */
    void setModSet(ModSet modSet) {
        m_modSet = modSet;
    }

    /**
     * Checks if this object is a member of an IMembership. Roles and Groups
     * implement IMembership
     * 
     * @param im
     *            Role or Group against which the membership is to be checked
     * @return <code>true</code> if this object is a member of the
     *         IMembership, <code>false</code> otherwise
     * @throws UMSException
     *             propagates any exception from the datalayer
     *
     * @supported.api
     */
    public boolean isMemberOf(IMembership im) throws UMSException {
        return im.hasMember(getGuid());
    }

    /**
     * Gets the list of GUIDS roles assosciated with this object
     * 
     * @return list that lets iterating over role GUIDs
     * @throws UMSException
     *             propagates any exception from the datalayer
     *
     * @supported.api
     */
    public Collection getRoles() throws UMSException {
        ArrayList roleList = new ArrayList();
        Attr roleAttr = getAttribute(COMPUTED_MEMBER_ATTR_NAME);
        if (roleAttr != null && roleAttr.getStringValues() != null) {
            roleList.addAll(Arrays.asList(roleAttr.getStringValues()));
        }
        return roleList;
    }

    /**
     * Returns all the ACIs of this object.
     * 
     * @return collecion of ACIs of this object.
     * @throws ACIParseException if any error
     *
     * @supported.api
     */
    public Collection getACI() throws ACIParseException, UMSException {
        Collection acis = new ArrayList();
        Attr aciAttr = getAttribute(ACI.ACI);
        if (aciAttr != null) {
            String[] aciTexts = aciAttr.getStringValues();
            int size = aciTexts.length;
            for (int i = 0; i < size; i++) {
                acis.add(ACI.valueOf(aciTexts[i]));
            }
        }
        return acis;
    }

    /**
     * Returns all the ACIs of this object with the given name.
     * 
     * @param name Name of the ACI to get.
     * @return collecion of ACIs of this object.
     * @throws ACIParseException in case of any error
     *
     * @supported.api
     */
    public Collection getACI(String name) throws ACIParseException,
            UMSException {
        Collection acis = new ArrayList();
        Attr aciAttr = getAttribute(ACI.ACI);
        if (aciAttr != null) {
            String[] aciTexts = aciAttr.getStringValues();
            int size = aciTexts.length;
            for (int i = 0; i < size; i++) {
                ACI aci = ACI.valueOf(aciTexts[i]);
                if (aci.getName().equals(name)) {
                    acis.add(aci);
                }
            }
        }
        return acis;
    }

    /**
     * Adds an ACI to this object.
     * 
     * @param aci ACI added to be added to this object.
     * @throws AccessRightsException if an access rights exception occurs.
     * @throws UMSException if any error
     *
     * @supported.api
     */
    public void addACI(ACI aci) throws AccessRightsException, UMSException {
        Attr attr = new Attr(ACI.ACI, aci.toString());
        modify(attr, ModSet.ADD);
        save();
    }

    /**
     * Deletes an ACI of this object
     * 
     * @param aci ACI to be deleted.
     * @throws AccessRightsException if an access rights exception occurs.
     * @throws UMSException if any error.
     *
     * @supported.api
     */
    public void deleteACI(ACI aci) throws AccessRightsException, UMSException {
        Attr attr = new Attr(ACI.ACI, aci.getACIText());
        modify(attr, ModSet.DELETE);
        save();
    }

    /**
     * Replaces an ACI of this object
     * 
     * @param oldACI ACI to be replaced.
     * @param newACI the new ACI.
     * @throws AccessRightsException if an access rights exception occurs.
     * @throws UMSException if any error.
     *
     * @supported.api
     */
    public void replaceACI(ACI oldACI, ACI newACI)
            throws AccessRightsException, UMSException {
        Attr attr = new Attr(ACI.ACI, oldACI.getACIText());
        modify(attr, ModSet.DELETE);
        attr = new Attr(ACI.ACI, newACI.toString());
        modify(attr, ModSet.ADD);
        save();
    }

    /**
     * Adds value for an attribute and saves the change in the database.
     * 
     * @param token Authenticated prinicpal's single sign on token.
     * @param guid Identifiation of the entry to which to add the attribute
     *        value.
     * @param name Name of the attribute to which value is being added.
     * @param value Value to be added to the attribute.
     * @throws UMSException if any exception from the data layer.
     *
     * @supported.api
     */
    public static void addAttributeValue(
        SSOToken token,
        Guid guid,
        String name,
        String value
    ) throws UMSException {
        if (guid == null) {
            throw new IllegalArgumentException(i18n
                    .getString(IUMSConstants.NULL_GUIDS));
        }
        if (token == null) {
            throw new IllegalArgumentException(i18n
                    .getString(IUMSConstants.NULL_TOKEN));
        }
        try {
            SSOTokenManager.getInstance().validateToken(token);
        } catch (SSOException se) {
            throw new UMSException(i18n.getString(IUMSConstants.INVALID_TOKEN),
                    se);
        }

        Attr attr = new Attr(name, value);
        attr = null;
        Validation.validateAttribute(attr, UMSObject.getObject(token, guid)
                .getClass(), guid);
        try {
            DataLayer.getInstance().addAttributeValue(token.getPrincipal(),
                    guid, name, value);
        } catch (SSOException se) {
            throw new UMSException(i18n.getString(IUMSConstants.BAD_TOKEN_HDL),
                    se);
        }
    }

    /**
     * Removes value for an attribute and saves the change in the database.
     * 
     * @param token Authenticated prinicpal's single sign on token.
     * @param guid Identification of the entry from which to remove the
     *        attribute value.
     * @param name Name of the attribute from which value is being removed.
     * @param value Value to be removed from the attribute.
     * @throws UMSException if any exception from the data layer.
     *
     * @supported.api
     */
    public static void removeAttributeValue(SSOToken token, Guid guid,
            String name, String value) throws UMSException {
        if (guid == null) {
            throw new IllegalArgumentException(i18n
                    .getString(IUMSConstants.NULL_GUIDS));
        }
        if (token == null) {
            throw new IllegalArgumentException(i18n
                    .getString(IUMSConstants.NULL_TOKEN));
        }
        try {
            SSOTokenManager.getInstance().validateToken(token);
        } catch (SSOException se) {
            throw new UMSException(i18n.getString(IUMSConstants.INVALID_TOKEN),
                    se);
        }

        try {
            DataLayer.getInstance().removeAttributeValue(token.getPrincipal(),
                    guid, name, value);
        } catch (SSOException se) {
            throw new UMSException(i18n.getString(IUMSConstants.BAD_TOKEN_HDL),
                    se);
        }
    }

    /**
     * Check if the object is persistent in the system
     * 
     * @return true if the object is persistent in the system and false
     *         otherwise
     */
    protected boolean isPersistent() {
        return m_principal != null && m_guid != null
                && m_guid.getDn().length() > 0;
    }

    /**
     * Find the names of attributes not read from data store so far
     * 
     * @param attrNames
     *            names of attributes to get
     * @return collection of names of attributes not read from data store so far
     */
    private Collection findAttributesNotRead(String[] attrNames) {
        ArrayList attributesNotInCache = new ArrayList();
        if (m_attrSet == null) {
            m_attrSet = new AttrSet();
        }
        if (m_nullAttributes == null) {
            m_nullAttributes = new ArrayList();
        }
        int length = attrNames.length;
        for (int i = 0; i < length; i++) {
            if ((m_attrSet.getAttribute(attrNames[i]) == null)
                    && !m_nullAttributes.contains(attrNames[i])) {
                attributesNotInCache.add(attrNames[i]);
            }
        }
        return attributesNotInCache;
    }

    /**
     * Find whether the attribute was not read from data store so far
     * 
     * @param attrName
     *            name of attribute to check
     * @return <code>true</code> if the attribute was not read, otherwise
     *         <code>false</code>
     */
    private boolean isAttributeNotRead(String attrName) {
        boolean attributeNotRead = false;
        if (m_attrSet == null) {
            m_attrSet = new AttrSet();
        }
        if (m_nullAttributes == null) {
            m_nullAttributes = new ArrayList();
        }
        if ((m_attrSet.getAttribute(attrName) == null)
                && !m_nullAttributes.contains(attrName)) {
            attributeNotRead = true;
        }
        return attributeNotRead;
    }

    /**
     * Read the attributes from data store
     * 
     * @param attrNames
     *            names of attributes to get
     * @return collection of Attr read from data store
     */
    private Collection readAttributesFromDataStore(Collection attrNames)
            throws UMSException {
        Collection attributes = DataLayer.getInstance().getAttributes(
                getPrincipal(), getGuid(), attrNames);
        if (attributes == null) {
            String[] args = { getDN() };
            throw new UMSException(i18n.getString(
                    IUMSConstants.READ_ATTRIBUTES_ERROR, args));
        }
        Collection foundAttributes = new ArrayList();
        if (m_attrSet == null) {
            m_attrSet = new AttrSet();
        }
        if (m_nullAttributes == null) {
            m_nullAttributes = new ArrayList();
        }
        Iterator iter = attributes.iterator();
        while (iter.hasNext()) {
            Attr attr = (Attr) iter.next();
            foundAttributes.add(attr.getName());
            m_attrSet.replace(attr);
        }
        iter = attrNames.iterator();
        while (iter.hasNext()) {
            String attrName = (String) iter.next();
            if (!foundAttributes.contains(attrName)
                    && !m_nullAttributes.contains(attrName)) {
                m_nullAttributes.add(attrName);
            }
        }
        return attributes;
    }

    /**
     * Read the attribute from data store
     * 
     * @param attrName
     *            names of attributes to get
     * @return Attr read from datastore
     */
    private Attr readAttributeFromDataStore(String attrName) throws UMSException {
        Attr attr = DataLayer.getInstance().getAttribute(getPrincipal(),
                getGuid(), attrName);
        if (m_attrSet == null) {
            m_attrSet = new AttrSet();
        }
        if (m_nullAttributes == null) {
            m_nullAttributes = new ArrayList();
        }
        if (attr != null) {
            m_attrSet.replace(attr);
        } else if (!m_nullAttributes.contains(attrName)) {
            m_nullAttributes.add(attrName);
        }
        return attr;
    }

    /**
     * Get the attribute from cache, does not read data store
     * 
     * @param attrName
     *            name of attribute to get
     * @return Attr read from cache
     */
    private Attr getAttributeFromCache(String attrName) {
        Attr attr = null;
        if (m_attrSet != null) {
            attr = m_attrSet.getAttribute(attrName);
        }
        return attr;
    }

    /**
     * Authenticated session in constructing the object
     * 
     * @serial
     */
    private Principal m_principal;

    /**
     * Identification of the object
     * 
     * @serial
     */
    private Guid m_guid;

    /**
     * Internal cache for attributes
     * 
     * @serial
     */
    private AttrSet m_attrSet;

    /**
     * Internal cache for attributes read from directory and found to be null
     * 
     * @serial
     */
    private ArrayList m_nullAttributes;

    /**
     * Internal cache for changes made to the object
     * 
     * @serial
     */
    private ModSet m_modSet;

    /**
     * Internal naming attribute (ex: "ou")
     * 
     * @serial
     */
    private String m_namingAttribute = null;
}
