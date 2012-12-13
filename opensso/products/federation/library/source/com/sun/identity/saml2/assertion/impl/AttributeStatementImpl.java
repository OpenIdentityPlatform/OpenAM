/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: AttributeStatementImpl.java,v 1.2 2008/06/25 05:47:42 qcheng Exp $
 *
 */



package com.sun.identity.saml2.assertion.impl;

import java.util.Collections;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Element;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.sun.identity.shared.xml.XMLUtils;
import com.sun.identity.saml2.assertion.AssertionFactory;
import com.sun.identity.saml2.assertion.Attribute;
import com.sun.identity.saml2.assertion.EncryptedAttribute;
import com.sun.identity.saml2.assertion.AttributeStatement;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2SDKUtils;

/**
 * This is a default implementation of <code>AttributeStatement</code>.
 *
 * The <code>AttributeStatement</code> element describes a statement by
 * the SAML authority asserting that the assertion subject is associated with
 * the specified attributes. It is of type <code>AttributeStatementType</code>.
 * <p>
 * <pre>
 * &lt;complexType name="AttributeStatementType">
 *   &lt;complexContent>
 *     &lt;extension base="{urn:oasis:names:tc:SAML:2.0:assertion}
 *       StatementAbstractType">
 *       &lt;choice maxOccurs="unbounded">
 *         &lt;element ref="{urn:oasis:names:tc:SAML:2.0:assertion}Attribute"/>
 *         &lt;element ref="{urn:oasis:names:tc:SAML:2.0:assertion}
 *         EncryptedAttribute"/>
 *       &lt;/choice>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 */
public class AttributeStatementImpl implements AttributeStatement {

    private List attrs = null;
    private List encAttrs = null;
    private boolean mutable = true;

    // Validate the object according to the schema.
    private void validateData()
        throws SAML2Exception
    {
        if ((attrs == null || attrs.isEmpty()) &&
            (encAttrs == null || encAttrs.isEmpty()))
        {
            if (SAML2SDKUtils.debug.messageEnabled()) {
                SAML2SDKUtils.debug.message("AttributeStatementImpl."
                    + "validateData: missing Attribute or"
                    + " EncryptedAttribute element.");
            }
            throw new SAML2Exception(
                SAML2SDKUtils.bundle.getString("missingElement"));
        }

    }

    // used by the constructors.
    private void parseElement(Element element)
        throws SAML2Exception
    {
        // make sure that the input xml block is not null
        if (element == null) {
            if (SAML2SDKUtils.debug.messageEnabled()) {
                SAML2SDKUtils.debug.message("AttributeStatementImpl." 
                    + "parseElement: Input is null.");
            }
            throw new SAML2Exception(
                      SAML2SDKUtils.bundle.getString("nullInput"));
        }
        // Make sure this is an AttributeStatement.
        if (!SAML2SDKUtils.checkStatement(element, "AttributeStatement")) {
            if (SAML2SDKUtils.debug.messageEnabled()) {
                SAML2SDKUtils.debug.message("AttributeStatementImpl." 
                    +"parseElement: not AttributeStatement.");
            }
            throw new SAML2Exception(
                      SAML2SDKUtils.bundle.getString("wrongInput"));
        }

        // handle the sub elementsof the AuthnStatment
        NodeList nl = element.getChildNodes();
        Node child;
        String childName;
        int length = nl.getLength();
        for (int i = 0; i < length; i++) {
            child = nl.item(i);
            if ((childName = child.getLocalName()) != null) {
                if (childName.equals("Attribute")) {
                    Attribute attr = AssertionFactory.getInstance().
                        createAttribute((Element) child);
                    if (attrs == null) {
                        attrs = new ArrayList();
                    }
                    attrs.add(attr);
                } else if (childName.equals("EncryptedAttribute")) {
                    EncryptedAttribute encAttr = AssertionFactory.getInstance().
                        createEncryptedAttribute((Element) child);
                    if (encAttrs == null) {
                        encAttrs = new ArrayList();
                    }
                    encAttrs.add(encAttr);
                } else {
                    if (SAML2SDKUtils.debug.messageEnabled()) {
                        SAML2SDKUtils.debug.message("AttributeStatementImpl."
                            + "parse Element: Invalid element:" + childName);
                    }
                    throw new SAML2Exception(
                        SAML2SDKUtils.bundle.getString("invalidElement"));
                }
            }
        }
        validateData();
        if (attrs != null) {
            attrs = Collections.unmodifiableList(attrs);
        }
        if (encAttrs != null) {
            encAttrs = Collections.unmodifiableList(encAttrs);
        }
        mutable = false;
    }

    /**
     * Class constructor. Caller may need to call setters to populate the
     * object.
     */
    public AttributeStatementImpl() {
    }

    /**
     * Class constructor with <code>AttributeStatement</code> in
     * <code>Element</code> format.
     */
    public AttributeStatementImpl(org.w3c.dom.Element element)
        throws com.sun.identity.saml2.common.SAML2Exception
    {
        parseElement(element);
    }

    /**
     * Class constructor with <code>AttributeStatement</code> in xml string
     * format.
     */
    public AttributeStatementImpl(String xmlString)
        throws com.sun.identity.saml2.common.SAML2Exception
    {
        Document doc = XMLUtils.toDOMDocument(xmlString, SAML2SDKUtils.debug);
        if (doc == null) {
            throw new SAML2Exception(
                SAML2SDKUtils.bundle.getString("errorObtainingElement"));
        }
        parseElement(doc.getDocumentElement());
    }

    /**
     * Returns <code>Attribute</code>(s) of the statement. 
     *
     * @return List of <code>Attribute</code>(s) in the statement.
     * @see #setAttribute(List)
     */
    public List getAttribute() {
        return attrs;
    }

    /**
     * Sets <code>Attribute</code>(s) of the statement.
     *
     * @param value List of new <code>Attribute</code>(s).
     * @throws SAML2Exception if the object is immutable.
     * @see #getAttribute()
     */
    public void setAttribute(List value)
        throws SAML2Exception
    {
        if (!mutable) {
            throw new SAML2Exception(
                SAML2SDKUtils.bundle.getString("objectImmutable"));
        }
        attrs = value;
    }

    /**
     * Returns <code>EncryptedAttribute</code>(s) of the statement. 
     *
     * @return List of <code>EncryptedAttribute</code>(s) in the statement.
     * @see #setEncryptedAttribute(List)
     */
    public List getEncryptedAttribute() {
        return encAttrs;
    }

    /**
     * Sets <code>EncryptedAttribute</code>(s) of the statement.
     *
     * @param value List of new <code>EncryptedAttribute</code>(s).
     * @throws SAML2Exception if the object is immutable.
     * @see #getEncryptedAttribute()
     */
    public void setEncryptedAttribute(List value)
        throws SAML2Exception
    {
        if (!mutable) {
            throw new SAML2Exception(
                SAML2SDKUtils.bundle.getString("objectImmutable"));
        }
        encAttrs = value;
    }

    /**
     * Makes the object immutable.
     */
    public void makeImmutable() {
        if (mutable) {
            if (attrs != null) {
                Iterator iter = attrs.iterator();
                while (iter.hasNext()) {
                    Attribute attr = (Attribute) iter.next();
                    attr.makeImmutable();
                }
                attrs = Collections.unmodifiableList(attrs);
            }
            if (encAttrs != null) {
                encAttrs = Collections.unmodifiableList(encAttrs);
            }
            mutable = false;
        }
    }

    /**
     * Returns the mutability of the object.
     *
     * @return <code>true</code> if the object is mutable;
     *          <code>false</code> otherwise.
     */
    public boolean isMutable() {
        return mutable;
    }

    /**
     * Returns a String representation of the element.
     *
     * @return A string containing the valid XML for this element.
     *          By default name space name is prepended to the element name.
     * @throws SAML2Exception if the object does not conform to the schema.
     */
    public java.lang.String toXMLString()
        throws SAML2Exception
    {
        return this.toXMLString(true, false);
    }

    /**
     * Returns a String representation of the element.
     *
     * @param includeNS Determines whether or not the namespace qualifier is
     *          prepended to the Element when converted
     * @param declareNS Determines whether or not the namespace is declared
     *          within the Element.
     * @return A string containing the valid XML for this element
     * @throws SAML2Exception if the object does not conform to the schema.
     */
    public java.lang.String toXMLString(boolean includeNS, boolean declareNS)
        throws SAML2Exception
    {
        validateData();
        StringBuffer result = new StringBuffer(1000);
        String prefix = "";
        String uri = "";
        if (includeNS) {
            prefix = SAML2Constants.ASSERTION_PREFIX;
        }
        if (declareNS) {
            uri = SAML2Constants.ASSERTION_DECLARE_STR;
        }

        result.append("<").append(prefix).append("AttributeStatement").
            append(uri).append(">");

        if (attrs != null) {
            Iterator iter = attrs.iterator();
            while (iter.hasNext()) {
                result.append(((Attribute) iter.next()).
                        toXMLString(includeNS, declareNS));
            }
        }
        if (encAttrs != null) {
            Iterator iter1 = encAttrs.iterator();
            while (iter1.hasNext()) {
                result.append(((EncryptedAttribute) iter1.next()).
                        toXMLString(includeNS, declareNS));
            }
        }
        result.append("</").append(prefix).append("AttributeStatement>");
        return result.toString();
    }

}

