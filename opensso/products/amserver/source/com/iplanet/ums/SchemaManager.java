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
 * $Id: SchemaManager.java,v 1.4 2009/01/28 05:34:50 ww203982 Exp $
 *
 */

package com.iplanet.ums;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;

import com.sun.identity.shared.ldap.LDAPAttribute;
import com.sun.identity.shared.ldap.LDAPAttributeSchema;
import com.sun.identity.shared.ldap.LDAPModification;
import com.sun.identity.shared.ldap.LDAPObjectClassSchema;
import com.sun.identity.shared.ldap.LDAPSchema;
import com.sun.identity.shared.ldap.util.LDIF;
import com.sun.identity.shared.ldap.util.LDIFContent;
import com.sun.identity.shared.ldap.util.LDIFModifyContent;
import com.sun.identity.shared.ldap.util.LDIFRecord;

import com.iplanet.services.util.I18n;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;

/**
 * The class manages the schema in the LDAP directory server.
 * 
 * <p>
 * Examples:
 * <p>
 * To add/delete schema definitions
 * <p>
 * 
 * <pre>
 * // Gets the schema manager associated with the login Context
 * SchemaManager mgr = SchemaManager.getSchemaManager(principal);
 * 
 * // Adds a new attribute type to the schema
 * mgr.addAttribute(&quot;newAttributeType&quot;, 
 *         &quot;1.2.3.4.5.6.5.4.3.2.1&quot;,
 *         &quot;A new attribute type&quot;, 
 *         SchmeaManager.ATTRIBUTE_SYNTAX_CIS, true, null,
 *         null);
 * 
 * // Removes a attribute type from the schema
 * mgr.removeAttribute(&quot;newAttributeType&quot;);
 * 
 * // Adds a new object class to the schema
 * mgr.addObjectClass(&quot;newObjectClass&quot;, 
 *         &quot;1.1.2.2.1.1.2.2&quot;, null,
 *         &quot;A new object class&quot;, required, optional);
 * 
 * // Removes a object class from the schema
 * mgr.removeObjectClass(&quot;newObjectClass&quot;);
 * 
 * </pre>
 */
public class SchemaManager implements java.io.Serializable, IUMSConstants {

    /**
     * The syntax of this attribute type is a case-insensitive string
     */
    public static final String ATTRIBUTE_SYNTAX_CIS = 
        "1.3.6.1.4.1.1466.115.121.1.15";

    /**
     * The syntax of this attribute type is a case-exact string
     */
    public static final String ATTRIBUTE_SYNTAX_CES = 
        "1.3.6.1.4.1.1466.115.121.1.26";

    /**
     * The syntax of this attribute type is a binary data
     */
    public static final String ATTRIBUTE_SYNTAX_BINARY = 
        "1.3.6.1.4.1.1466.115.121.1.5";

    /**
     * The syntax of this attribute type is an integer
     */
    public static final String ATTRIBUTE_SYNTAX_INT = 
        "1.3.6.1.4.1.1466.115.121.1.27";

    /**
     * The syntax of this attribute type is a telephone number
     */
    public static final String ATTRIBUTE_SYNTAX_TELEPHONE = 
        "1.3.6.1.4.1.1466.115.121.1.50";

    /**
     * The syntax of this attribute type is a distinguished name
     */
    public static final String ATTRIBUTE_SYNTAX_DN = 
        "1.3.6.1.4.1.1466.115.121.1.12";

    /**
     * Structural object class type
     */
    public static final int STRUCTURAL = 0;

    /**
     * Abstract object class type
     */
    public static final int ABSTRACT = 1;

    /**
     * Auxiliary object class type
     */
    public static final int AUXILIARY = 2;

    private static I18n i18n = I18n.getInstance(IUMSConstants.UMS_PKG);

    /**
     * Default constructor
     */
    protected SchemaManager() {
    }

    /**
     * Constructs a schema manager to manage the schema associated with the
     * given principal
     * 
     * @param principal Authenticated principal.
     * @throws UMSException if failed to construct the schema manager.
     */
    protected SchemaManager(java.security.Principal principal)
        throws UMSException {
        m_datalayer = DataLayer.getInstance();
        m_principal = principal;
    }

    /**
     * Returns the schema manager assosciated with the given authenticated
     * Principal.
     * 
     * @param token Authenticated principal's single sign on token.
     * @return Schema manager associated with the given principal.
     * @throws UMSException if failed to get schema manager.
     */
    public static SchemaManager getSchemaManager(SSOToken token)
            throws UMSException {

        try {
            SSOTokenManager.getInstance().validateToken(token);
        } catch (SSOException se) {
            throw new UMSException(i18n.getString(IUMSConstants.INVALID_TOKEN),
                    se);
        }
        java.security.Principal principal = null;
        try {
            principal = token.getPrincipal();
        } catch (Exception e) {
            // PKB: Log an error
            return null;
        }

        return (new SchemaManager(principal));
    }

    /**
     * Returns the schema manager assosciated with the given authenticated
     * Principal.
     * 
     * @param principal Authenticated principal.
     * @return Schema manager associated with the given principal.
     * @throws UMSException if failed to get schema manager.
     */
    public static SchemaManager getSchemaManager(
        java.security.Principal principal
    ) throws UMSException {
        return new SchemaManager(principal);
    }

    private LDAPSchema getLDAPSchema() throws UMSException {
        if (m_schema == null || m_hasModified) {
            refresh();
            m_hasModified = false;
        }
        return m_schema;
    }

    /**
     * Refreshes the schema in memory. Should be called if want to retrieve
     * schema information and you are not sure whether or not the schema has
     * been modified outside of this SchemaManager
     * 
     * @throws UMSException
     *             failure
     */
    public void refresh() throws UMSException {
        m_schema = m_datalayer.getSchema(m_principal);
    }

    /**
     * Adds the attribute type definition to the schema
     * 
     * @param name
     *            Name of the attribute type
     * @param oid
     *            Object identifier (OID) of the attribute type in dotted-string
     *            format
     * @param description
     *            Description of attribute type
     * @param syntax
     *            Syntax of this attribute type in dotted-string format
     * @param single
     *            True if the attribute type is single-valued
     * @throws AttributeValueAlreadyExistsException
     *             if it already exists
     * @throws UMSException
     *             failure
     */
    public void addAttribute(String name, String oid, String description,
            String syntax, boolean single)
            throws AttributeValueAlreadyExistsException, UMSException {
        LDAPAttributeSchema newAttrType = new LDAPAttributeSchema(name, oid,
                description, syntax, single);
        m_datalayer.addSchema(m_principal, newAttrType);
        m_hasModified = true;
    }

    /**
     * Adds the attribute type definition to the schema
     * 
     * @param raw
     *            AttributeTypeDescription format used to construct the
     *            attribute type definition
     * @throws AttributeValueAlreadyExistsException
     *             if it already exists
     * @throws UMSException
     *             failure
     */
    public void addAttribute(String raw)
            throws AttributeValueAlreadyExistsException, UMSException {
        LDAPAttributeSchema newAttrType = new LDAPAttributeSchema(raw);
        m_datalayer.addSchema(m_principal, newAttrType);
        m_hasModified = true;
    }

    /**
     * Adds the object class definition to the schema
     * 
     * @param name
     *            Name of the object class
     * @param oid
     *            Object identifier (OID) of the object class in dotted-string
     *            format
     * @param superior
     *            Names of parent object classes
     * @param description
     *            Description of the object class
     * @param required
     *            Array of names of attributes required in this object class
     * @param optional
     *            Array of names of optional attributes allowed in this object
     *            class
     * @throws AttributeValueAlreadyExistsException
     *             if it already exists
     * @throws UMSException
     *             failure
     */
    public void addObjectClass(String name, String oid, String superior,
            String description, String[] required, String[] optional)
            throws AttributeValueAlreadyExistsException, UMSException {
        LDAPObjectClassSchema newObjClass = new LDAPObjectClassSchema(name,
                oid, superior, description, required, optional);
        m_datalayer.addSchema(m_principal, newObjClass);
        m_hasModified = true;
    }

    /**
     * Adds the object class definition to the schema
     * 
     * @param name
     *            Name of the object class
     * @param oid
     *            Object identifier (OID) of the object class in dotted-string
     *            format
     * @param superior
     *            Names of parent object classes
     * @param description
     *            Description of the object class
     * @param required
     *            Array of names of attributes required in this object class
     * @param optional
     *            Array of names of optional attributes allowed in this object
     *            class
     * @param type
     *            Either ABSTRACT, STRUCTURAL, or AUXILIARY
     * @param aliases
     *            Names which are to be considered aliases for this object
     *            class; null if there are no aliases
     * @throws AttributeValueAlreadyExistsException
     *             if it already exists
     * @throws UMSException
     *             upon failure
     */
    public void addObjectClass(String name, String oid, String[] superior,
            String description, String[] required, String[] optional, int type,
            String[] aliases) throws AttributeValueAlreadyExistsException,
            UMSException {
        LDAPObjectClassSchema newObjClass = new LDAPObjectClassSchema(name,
                oid, superior, description, required, optional, type, aliases);
        m_datalayer.addSchema(m_principal, newObjClass);
        m_hasModified = true;
    }

    /**
     * Adds the object class definition to the schema
     * 
     * @param raw
     *            ObjectClassDescription format used to construct the object
     *            class definition
     * @throws AttributeValueAlreadyExistsException
     *             if it already exists
     * @throws UMSException
     *             failure
     */
    public void addObjectClass(String raw)
            throws AttributeValueAlreadyExistsException, UMSException {
        LDAPObjectClassSchema newObjClass = new LDAPObjectClassSchema(raw);
        m_datalayer.addSchema(m_principal, newObjClass);
        m_hasModified = true;
    }

    /**
     * Removes the attribute type definition from the schema
     * 
     * @param name
     *            Name of the attribute type
     * @throws UMSException
     *             failure
     */
    public void removeAttribute(String name) throws UMSException {
        LDAPAttributeSchema attrType = getLDAPSchema().getAttribute(name);
        if (attrType == null) {
            String args[] = new String[1];
            args[0] = name;
            String msg = i18n.getString(IUMSConstants.ATTRIBUTETYPE_NOT_FOUND,
                    args);
            throw new UMSException(msg);
        } else {
            m_datalayer.removeSchema(m_principal, attrType);
        }
        m_hasModified = true;
    }

    /**
     * Removes the object class definition from the schema
     * 
     * @param name
     *            Name of the object class
     * @throws UMSException
     *             failure
     */
    public void removeObjectClass(String name) throws UMSException {
        LDAPObjectClassSchema objClass = getLDAPSchema().getObjectClass(name);
        if (objClass == null) {
            String args[] = new String[1];
            args[0] = name;
            String msg = i18n.getString(IUMSConstants.OBJECTCLASS_NOT_FOUND,
                    args);
            throw new UMSException(msg);
        } else {
            m_datalayer.removeSchema(m_principal, objClass);
        }
        m_hasModified = true;
    }

    /**
     * Gets the AttributeTypeDescription format associated with the specified
     * attribute type name
     * 
     * @param name
     *            Name of the attribute type
     * @return AttributeTypeDescription format associated with the specified
     *         name, or null if not found
     * @throws UMSException
     *             failure
     */
    public String getAttribute(String name) throws UMSException {
        LDAPAttributeSchema attrType = getLDAPSchema().getAttribute(name);
        if (attrType != null) {
            return attrType.getValue();
        } else {
            return null;
        }
    }

    /**
     * Gets the ObjectClassDescription format associated with the specified
     * object class name
     * 
     * @param name
     *            Name of the object class
     * @return The ObjectClassDescription format associated with the specified
     *         name, or null if not found
     * @throws UMSException
     *             failure
     */
    public String getObjectClass(String name) throws UMSException {
        LDAPObjectClassSchema objClass = getLDAPSchema().getObjectClass(name);
        if (objClass != null) {
            return objClass.getValue();
        } else {
            return null;
        }
    }

    /**
     * Gets a collection of the names of the object classes for this attribute
     * 
     * @param attrName
     *            Name of the attribute
     * @return A collection of the names of the object classes for this
     *         attribute
     * @throws UMSException
     *             failure
     */
    public Collection getObjectClasses(String attrName) throws UMSException {
        Collection objClassNames = new ArrayList();
        Enumeration objClasses = getLDAPSchema().getObjectClasses();
        while (objClasses.hasMoreElements()) {
            LDAPObjectClassSchema objClass = (LDAPObjectClassSchema) objClasses
                    .nextElement();
            Enumeration requiredAttrNames = objClass.getRequiredAttributes();
            Enumeration optionalAttrNames = objClass.getOptionalAttributes();
            while (requiredAttrNames.hasMoreElements()) {
                String name = (String) requiredAttrNames.nextElement();
                if (name.equalsIgnoreCase(attrName)) {
                    objClassNames.add(objClass.getName());
                }
            }
            while (optionalAttrNames.hasMoreElements()) {
                String name = (String) optionalAttrNames.nextElement();
                if (name.equalsIgnoreCase(attrName)) {
                    objClassNames.add(objClass.getName());
                }
            }
        }
        return objClassNames;
    }

    /**
     * Returns a collection of the names of the required and optional attributes
     * for this object class.
     * 
     * @param objClassName Name of the object class.
     * @return A collection of the names of the required and optional attributes
     *         for this object class.
     * @throws UMSException if failed to get attribute names.
     */
    public Collection getAttributes(String objClassName) throws UMSException {
        Collection attributes = getRequiredAttributes(objClassName);
        attributes.addAll(getOptionalAttributes(objClassName));
        return attributes;
    }

    /**
     * Returns a collection of the names of the required attributes for this
     * object class.
     * 
     * @param objClassName Name of the object class.
     * @return a collection of the names of the required attributes for this
     *         object class.
     * @throws UMSException if failed to get attribute names.
     */
    public Collection getRequiredAttributes(String objClassName)
            throws UMSException {
        Collection attributeNames = new ArrayList();
        LDAPObjectClassSchema objClass = getLDAPSchema().getObjectClass(
                objClassName);
        if (objClass != null) {
            Enumeration en = objClass.getRequiredAttributes();
            while (en.hasMoreElements()) {
                attributeNames.add(en.nextElement());
            }
        }
        return attributeNames;
    }

    /**
     * Returns a collection of the names of the optional attributes for this
     * object class.
     * 
     * @param objClassName Name of the object class.
     * @return a collection of the names of the optional attributes for this
     *         object class.
     * @throws UMSException if failed to get attribute names.
     */
    public Collection getOptionalAttributes(String objClassName)
            throws UMSException {
        Collection attributeNames = new ArrayList();
        LDAPObjectClassSchema objClass = getLDAPSchema().getObjectClass(
                objClassName);
        if (objClass != null) {
            Enumeration en = objClass.getOptionalAttributes();
            while (en.hasMoreElements()) {
                attributeNames.add(en.nextElement());
            }
        }
        return attributeNames;
    }

    /**
     * Gets a collection of the names of the attributes for this object class
     * and will contain no attribute names in common with the specified list of
     * object classes
     * 
     * @param objClassName
     *            the name of the object class
     * @param objClassNames
     *            the names of other object classes
     * @return a collection of the names of the attributes for this object class
     *         and will contain no attribute names in common with the specified
     *         object classes
     * @throws UMSException
     *             failure
     */
    public Collection getUniqueAttributes(String objClassName,
            Collection objClassNames) throws UMSException {
        Collection attributes = getAttributes(objClassName);
        ArrayList objClasses = (ArrayList) objClassNames;
        for (int i = 0; i < objClasses.size(); i++) {
            attributes.removeAll(getAttributes((String) objClasses.get(i)));
        }
        return attributes;
    }

    /**
     * Gets a collection of the names of the attributes for this object class
     * and will contain no attribute names in common with all other object
     * classes in the schema
     * 
     * @param objClassName
     *            the name of the object class
     * @return a collection of the names of the attributes for this object class
     *         and will contain no attribute names in common with all other
     *         object classes in the schema
     * @throws UMSException
     *             failure
     */
    public Collection getUniqueAttributes(String objClassName)
            throws UMSException {
        Collection attributes = getAttributes(objClassName);
        ArrayList attrs = new ArrayList(attributes);
        for (int i = 0; i < attrs.size(); i++) {
            String attr = (String) attrs.get(i);
            if (getObjectClasses(attr).size() > 1) {
                attributes.remove(attr);
            }
        }
        return attributes;
    }

    private String getNameFromAttribute(String raw) {
        LDAPAttributeSchema attrType = new LDAPAttributeSchema(raw);
        return attrType.getName();
    }

    private String getNameFromObjectClass(String raw) {
        LDAPObjectClassSchema objClass = new LDAPObjectClassSchema(raw);
        return objClass.getName();
    }

    /**
     * Adds/modifies/removes LDAP schema definitions(attribute type and object
     * class) from a specified LDIF file
     * 
     * @param file
     *            the name of the LDIF file to parse
     * @throws UMSException
     *             failure
     */
    public void modifySchema(String file) throws UMSException {
        LDAPModification mods[] = null;

        try {
            // Gets the handle of the LDIF file
            LDIF ldif = new LDIF(file);
            LDIFRecord rec = ldif.nextRecord();

            for (; rec != null; rec = ldif.nextRecord()) {
                LDIFContent content = rec.getContent();

                if (content instanceof LDIFModifyContent) {
                    mods = ((LDIFModifyContent) content).getModifications();

                    for (int i = 0; i < mods.length; i++) {
                        LDAPAttribute attr = mods[i].getAttribute();
                        String attrTypeRawString = 
                            attr.getStringValueArray()[0];
                        String objClassRawString = 
                            attr.getStringValueArray()[0];
                        int operation = mods[i].getOp();

                        if (operation == LDAPModification.ADD) {
                            if (attr.getName().equalsIgnoreCase(
                                    "attributetypes")) {
                                addAttribute(attrTypeRawString);
                            } else if (attr.getName().equalsIgnoreCase(
                                    "objectclasses")) {
                                addObjectClass(objClassRawString);
                            }
                        } else if (operation == LDAPModification.REPLACE) {
                            if (attr.getName().equalsIgnoreCase(
                                    "attributetypes")) {
                                removeAttribute(
                                        getNameFromAttribute(
                                                attrTypeRawString));
                                addAttribute(attrTypeRawString);
                            } else if (attr.getName().equalsIgnoreCase(
                                    "objectclasses")) {
                                removeObjectClass(
                                        getNameFromObjectClass(
                                                objClassRawString));
                                addObjectClass(objClassRawString);
                            }
                        } else if (operation == LDAPModification.DELETE) {
                            if (attr.getName().equalsIgnoreCase(
                                    "attributetypes")) {
                                removeAttribute(
                                        getNameFromAttribute(
                                                attrTypeRawString));
                            } else if (attr.getName().equalsIgnoreCase(
                                    "objectclasses")) {
                                removeObjectClass(
                                        getNameFromObjectClass(
                                                objClassRawString));
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            String msg = i18n.getString(IUMSConstants.READING_LDIF_FAILED);
            throw new UMSException(msg, e);
        }

    }

    /**
     * @serial
     */
    private java.security.Principal m_principal;

    /**
     * @serial
     */
    private DataLayer m_datalayer;

    /**
     * @serial
     */
    private LDAPSchema m_schema;

    /**
     * @serial
     */
    private boolean m_hasModified;

}
