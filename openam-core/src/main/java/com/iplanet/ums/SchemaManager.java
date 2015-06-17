/*
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
 * Portions Copyright 2015 ForgeRock AS.
 */

package com.iplanet.ums;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;

import com.iplanet.services.util.I18n;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import org.forgerock.opendj.ldap.schema.AttributeType;
import org.forgerock.opendj.ldap.schema.ObjectClass;
import org.forgerock.opendj.ldap.schema.Schema;

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
     * Auxiliary object class type
     */
    public static final int AUXILIARY = 2;

    private static I18n i18n = I18n.getInstance(IUMSConstants.UMS_PKG);

    /**
     * Constructs a schema manager to manage the schema associated with the
     * given principal
     * 
     * @param principal Authenticated principal.
     * @throws UMSException if failed to construct the schema manager.
     */
    private SchemaManager(Principal principal) throws UMSException {
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
    public static SchemaManager getSchemaManager(SSOToken token) throws UMSException {

        try {
            SSOTokenManager.getInstance().validateToken(token);
        } catch (SSOException se) {
            throw new UMSException(i18n.getString(IUMSConstants.INVALID_TOKEN), se);
        }
        try {
            return getSchemaManager(token.getPrincipal());
        } catch (Exception e) {
            // PKB: Log an error
            return null;
        }
    }

    /**
     * Returns the schema manager assosciated with the given authenticated
     * Principal.
     * 
     * @param principal Authenticated principal.
     * @return Schema manager associated with the given principal.
     * @throws UMSException if failed to get schema manager.
     */
    public static SchemaManager getSchemaManager(Principal principal) throws UMSException {
        return new SchemaManager(principal);
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
    public Collection<String> getObjectClasses(String attrName) throws UMSException {
        Collection<String> objClassNames = new ArrayList<>();
        Collection<ObjectClass> objClasses = getLDAPSchema().getObjectClasses();
        for (ObjectClass objClass : objClasses) {
            for (AttributeType attributeType : objClass.getRequiredAttributes()) {
                if (attributeType.getNameOrOID().equalsIgnoreCase(attrName)) {
                    objClassNames.add(objClass.getNameOrOID());
                }
            }
            for (AttributeType attributeType : objClass.getOptionalAttributes()) {
                if (attributeType.getNameOrOID().equalsIgnoreCase(attrName)) {
                    objClassNames.add(objClass.getNameOrOID());
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
    public Collection<String> getAttributes(String objClassName) throws UMSException {
        Collection<String> attributes = getRequiredAttributes(objClassName);
        attributes.addAll(getOptionalAttributes(objClassName));
        return attributes;
    }

    private Schema getLDAPSchema() throws UMSException {
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
    private void refresh() throws UMSException {
        m_schema = m_datalayer.getSchema(m_principal);
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
    private Collection<String> getRequiredAttributes(String objClassName)
            throws UMSException {
        Collection<String> attributeNames = new ArrayList<>();
        ObjectClass objClass = getLDAPSchema().getObjectClass(objClassName);
        if (objClass != null) {
            for (AttributeType attributeType : objClass.getRequiredAttributes()) {
                attributeNames.add(attributeType.getNameOrOID());
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
    private Collection<String> getOptionalAttributes(String objClassName)
            throws UMSException {
        Collection<String> attributeNames = new ArrayList<>();
        ObjectClass objClass = getLDAPSchema().getObjectClass(objClassName);
        if (objClass != null) {
            for (AttributeType attributeType : objClass.getOptionalAttributes()) {
                attributeNames.add(attributeType.getNameOrOID());
            }
        }
        return attributeNames;
    }

    /**
     * @serial
     */
    private Principal m_principal;

    /**
     * @serial
     */
    private DataLayer m_datalayer;

    /**
     * @serial
     */
    private Schema m_schema;

    /**
     * @serial
     */
    private boolean m_hasModified;

}
