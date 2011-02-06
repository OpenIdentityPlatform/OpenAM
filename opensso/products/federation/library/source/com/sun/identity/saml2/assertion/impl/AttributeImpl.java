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
 * $Id: AttributeImpl.java,v 1.4 2008/06/25 05:47:42 qcheng Exp $
 *
 */



package com.sun.identity.saml2.assertion.impl;

import java.security.Key;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.sun.identity.shared.xml.XMLUtils;
import com.sun.identity.saml2.assertion.AssertionFactory;
import com.sun.identity.saml2.assertion.Attribute;
import com.sun.identity.saml2.assertion.EncryptedAttribute;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2SDKUtils;
import com.sun.identity.saml2.xmlenc.EncManager;

/**
 * This is a default implementation of interface <code>Attribute</code>.
 *
 * The <code>Attribute</code> element identifies an attribute by name and
 * optionally includes its value(s). It has the <code>AttributeType</code>
 * complex type.
 * <p>
 * <pre>
 * &lt;complexType name="AttributeType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element ref="{urn:oasis:names:tc:SAML:2.0:assertion}
 *         AttributeValue" maxOccurs="unbounded" minOccurs="0"/>
 *        &lt;/sequence>
 *        &lt;attribute name="FriendlyName"
 *        type="{http://www.w3.org/2001/XMLSchema}string" />
 *        &lt;attribute name="Name" use="required"
 *        type="{http://www.w3.org/2001/XMLSchema}string" />
 *        &lt;attribute name="NameFormat"
 *        type="{http://www.w3.org/2001/XMLSchema}anyURI" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 */
public class AttributeImpl implements Attribute {

    private List attrValues = null;
    private List valueStrings = null;
    private String name = null;
    private String nameFormat = null;
    private String friendlyName = null;
    private Map anyMap = null;
    private boolean mutable = true;

    /**
     * Verifies if the input xmlstring can be converted to an AttributeValue
     * Element. If so, return the element value.
     */
    private String validateAndGetAttributeValue(String value)
        throws SAML2Exception
    {
        Document doc = XMLUtils.toDOMDocument(value, SAML2SDKUtils.debug);
        if (doc == null) {
            if (SAML2SDKUtils.debug.messageEnabled()) {
                SAML2SDKUtils.debug.message("AttributeImpl." 
                    + "validateAttributeValue:"
                    + " could not obtain AttributeValue element.");
            }
            throw new SAML2Exception(
                SAML2SDKUtils.bundle.getString("errorObtainingElement"));
        }
        Element element = doc.getDocumentElement();
        if (element == null) {
            if (SAML2SDKUtils.debug.messageEnabled()) {
                SAML2SDKUtils.debug.message("AttributeImpl."
                    +"validateAttributeValue: Input is null.");
            }
            throw new SAML2Exception(
                      SAML2SDKUtils.bundle.getString("nullInput"));
        }
        // Make sure this is an AttributeValue.
        String tag = element.getLocalName();
        if ((tag == null) || (!tag.equals("AttributeValue"))) {
            if (SAML2SDKUtils.debug.messageEnabled()) {
                SAML2SDKUtils.debug.message("AttributeImpl."
                    +"validateAttributeValue: not AttributeValue.");
            }
            throw new SAML2Exception(
                      SAML2SDKUtils.bundle.getString("wrongInput"));
        }
        return XMLUtils.getChildrenValue(element);
    }

    // used by the constructors.
    private void parseElement(Element element)
        throws SAML2Exception
    {
        // make sure that the input xml block is not null
        if (element == null) {
            if (SAML2SDKUtils.debug.messageEnabled()) {
                SAML2SDKUtils.debug.message("AttributeImpl.parseElement: "
                    + "Input is null.");
            }
            throw new SAML2Exception(
                      SAML2SDKUtils.bundle.getString("nullInput"));
        }
        // Make sure this is an Attribute.
        String tag = element.getLocalName();
        if ((tag == null) || (!tag.equals("Attribute"))) {
            if (SAML2SDKUtils.debug.messageEnabled()) {
                SAML2SDKUtils.debug.message("AttributeImpl.parseElement: "
                    + "not Attribute.");
            }
            throw new SAML2Exception(
                      SAML2SDKUtils.bundle.getString("wrongInput"));
        }

        // handle the attributes of <Attribute> element
        NamedNodeMap atts = ((Node)element).getAttributes();
        if (atts != null) {
            int length = atts.getLength();
            for (int i = 0; i < length; i++) {
                Attr attr = (Attr) atts.item(i);
                String attrName = attr.getName();
                String attrValue = attr.getValue().trim();
                if (attrName.equals("Name")) {
                    name = attrValue;
                } else if (attrName.equals("NameFormat")) {
                    nameFormat = attrValue;
                } else if (attrName.equals("FriendlyName")) {
                    friendlyName = attrValue;
                } else {
                    if (!attrValue.equals(        
                        SAML2Constants.ASSERTION_NAMESPACE_URI))
                    {
                        if (anyMap == null) {
                            anyMap = new HashMap();
                        }
                        anyMap.put(attrName, attrValue);
                    }
                }
            }
        }

        // handle AttributeValue
        NodeList nl = element.getChildNodes();
        Node child;
        String childName;
        int length = nl.getLength();
        for (int i = 0; i < length; i++) {
            child = nl.item(i);
            if ((childName = child.getLocalName()) != null) {
                if (childName.equals("AttributeValue")) {
                    if (attrValues == null) {
                        attrValues = new ArrayList();
                    }
                    attrValues.add(XMLUtils.print(child));
                    if (valueStrings == null) {
                        valueStrings = new ArrayList();
                    }
                    valueStrings.add(XMLUtils.getChildrenValue((Element)child));
                } else {
                    if (SAML2SDKUtils.debug.messageEnabled()) {
                        SAML2SDKUtils.debug.message("Attributempl.parseElement"
                            + ": Invalid element:" + childName);
                    }
                    throw new SAML2Exception(
                        SAML2SDKUtils.bundle.getString("invalidElement"));
                }
            }
        }

        if (name == null || name.trim().length() == 0) {
            if (SAML2SDKUtils.debug.messageEnabled()) {
                SAML2SDKUtils.debug.message("AttributeImpl.parseElement:"
                    +" missing Name attribute.");
            }
            throw new SAML2Exception(
                SAML2SDKUtils.bundle.getString("missingAttribute"));
        }
        
        if (attrValues != null) {
            attrValues = Collections.unmodifiableList(attrValues);
        }
        if (valueStrings != null) {
            valueStrings = Collections.unmodifiableList(valueStrings);
        }
        if (anyMap != null) {
            anyMap = Collections.unmodifiableMap(anyMap);
        }
        mutable = false;
    }

    /**
     * Class constructor. Caller may need to call setters to populate the
     * object.
     */
    public AttributeImpl() {
    }

    /**
     * Class constructor with <code>Attribute</code> in
     * <code>Element</code> format.
     */
    public AttributeImpl(org.w3c.dom.Element element)
        throws com.sun.identity.saml2.common.SAML2Exception
    {
        parseElement(element);
    }

    /**
     * Class constructor with <code>Attribute</code> in xml string
     * format.
     */
    public AttributeImpl(String xmlString)
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
     * Makes the object immutable.
     */
    public void makeImmutable() {
        if (mutable) {
            if (attrValues != null) {
                attrValues = Collections.unmodifiableList(attrValues);
            }
            if (valueStrings != null) {
                valueStrings = Collections.unmodifiableList(valueStrings);
            }
            if (anyMap != null) {
                anyMap = Collections.unmodifiableMap(anyMap);
            }
            mutable = false;
        }
    }

    /**
     * Returns the mutability of the object.
     *
     * @return <code>true</code> if the object is mutable;
     *                <code>false</code> otherwise.
     */
    public boolean isMutable() {
        return mutable;
    }

    /**
     * Returns the <code>AttributeValue</code>(s) of the <code>Attribute</code>.
     *
     * @return List of xml String representing <code>AttributeValue</code>(s)
     *          of the <code>Attribute</code>.
     * @see #setAttributeValue(List)
     */
    public List getAttributeValue() {
        return attrValues;
    }

    /**
     * Sets the <code>AttributeValue</code>(s) of the <code>Attribute</code>.
     *
     * @param value List of xml String representing the new
     *          <code>AttributeValue</code> element(s).
     * @throws SAML2Exception if the object is immutable or the input can not
     *                be converted to <code>AttributeValue</code> element.
     * @see #getAttributeValue()
     */
    public void setAttributeValue(List value)
        throws SAML2Exception
    {
        if (!mutable) {
            throw new SAML2Exception(
                SAML2SDKUtils.bundle.getString("objectImmutable"));
        }
        if (value != null) {
            Iterator iter = value.iterator();
            attrValues = new ArrayList();
            valueStrings = new ArrayList();
            while (iter.hasNext()) {
                String attr = (String) iter.next();
                valueStrings.add(validateAndGetAttributeValue(attr));
                attrValues.add(attr);
            }
        } else {
            attrValues = null;
            valueStrings = null;
        }
    }

    /**
     * Returns the <code>AttributeValue</code>(s) of the <code>Attribute</code>.
     *
     * @return List of String representing the value of
     *          <code>AttributeValue</code>(s).
     * @see #setAttributeValueString(List)
     */
    public List getAttributeValueString() {
        return valueStrings;
    }


    /**
     * Sets the value of <code>AttributeValue</code> element(s).
     *
     * @param value List of String representing the value of the new
     *          <code>AttributeValue</code> element(s).
     * @throws SAML2Exception if the object is immutable.
     * @see #getAttributeValueString()
     */
    public void setAttributeValueString(List value)
        throws SAML2Exception
    {
        if (!mutable) {
            throw new SAML2Exception(
                SAML2SDKUtils.bundle.getString("objectImmutable"));
        }
        // for each value in the list; add tag; add to attrValues
        if (value != null) {
            attrValues = new ArrayList();
            valueStrings = new ArrayList();
            Iterator iter = value.iterator();
            while (iter.hasNext()) {
                String attr = (String) iter.next();
                attrValues.add("<saml:AttributeValue " +
                    "xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" " +
                    "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "+
                    "xsi:type=\"xs:string\">" + attr +
                    "</saml:AttributeValue>");
                valueStrings.add(attr);
            }
        } else {
            attrValues = null;
            valueStrings = null;
        }
    }

    /**
     * Returns the <code>Name</code> of the attribute.
     *
     * @return the <code>Name</code> of the attribute.
     * @see #setName(String)
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the <code>Name</code> of the attribute.
     *
     * @param value new <code>Name</code> of the attribute.
     * @throws SAML2Exception if the object is immutable.
     * @see #getName()
     */
    public void setName(String value)
        throws SAML2Exception
    {
        if (!mutable) {
            throw new SAML2Exception(
                SAML2SDKUtils.bundle.getString("objectImmutable"));
        }
        name = value;
    }

    /**
     * Returns the <code>NameFormat</code> of the attribute.
     *
     * @return the value of <code>NameFormat</code>.
     * @see #setNameFormat(String)
     */
    public String getNameFormat() {
        return nameFormat;
    }

    /**
     * Sets the <code>NameFormat</code> of the attribute.
     *
     * @param value new <code>NameFormat</code> of the attribute.
     * @throws SAML2Exception if the object is immutable.
     * @see #getNameFormat()
     */
    public void setNameFormat(String value)
        throws SAML2Exception
    {
        if (!mutable) {
            throw new SAML2Exception(
                SAML2SDKUtils.bundle.getString("objectImmutable"));
        }
        nameFormat = value;
    }

    /**
     * Returns the <code>FriendlyName</code> of the attribute.
     *
     * @return the value of <code>FriendlyName</code> of the attribute.
     * @see #setFriendlyName(String)
     */
    public String getFriendlyName() {
        return friendlyName;
    }

    /**
     * Sets the <code>FriendlyName</code> of the attribute.
     *
     * @param value new <code>FriendlyName</code> of the attribute.
     * @throws SAML2Exception if the object is immutable.
     * @see #getFriendlyName()
     */
    public void setFriendlyName(String value)
        throws SAML2Exception
    {
        if (!mutable) {
            throw new SAML2Exception(
                SAML2SDKUtils.bundle.getString("objectImmutable"));
        }
        friendlyName = value;
    }

    /**
     * Returns the <code>anyAttribute</code> of the attribute.
     *
     * @return A Map containing name/value pairs of <code>anyAttribute</code>.
     *                Both the name and value are String object types.
     * @see #setAnyAttribute(Map)
     */
    public Map getAnyAttribute() {
        return anyMap;
    }

    /**
     * Sets the <code>anyAttribute</code> of the attribute.
     *
     * @param value Map of name/value pairs to be set. Both the name and value
     *                are String object types.
     * @throws SAML2Exception if the object is immutable.
     * @see #getAnyAttribute()
     */
    public void setAnyAttribute(Map value)
        throws SAML2Exception
    {
        if (!mutable) {
            throw new SAML2Exception(
                SAML2SDKUtils.bundle.getString("objectImmutable"));
        }
        anyMap = value;
    }

    /**
     * Returns an <code>EncryptedAttribute</code> object.
     *
     * @param recipientPublicKey Public key used to encrypt the data encryption
     *                           (secret) key, it is the public key of the
     *                           recipient of the XML document to be encrypted.
     * @param dataEncAlgorithm Data encryption algorithm.
     * @param dataEncStrength Data encryption strength.
     * @param recipientEntityID Unique identifier of the recipient, it is used
     *                          as the index to the cached secret key so that
     *                          the key can be reused for the same recipient;
     *                          It can be null in which case the secret key will
     *                          be generated every time and will not be cached
     *                          and reused. Note that the generation of a secret
     *                          key is a relatively expensive operation.
     * @return <code>EncryptedAttribute</code> object
     * @throws SAML2Exception if error occurs during the encryption process.
     */
    public EncryptedAttribute encrypt(
        Key recipientPublicKey,
        String dataEncAlgorithm,
        int dataEncStrength,
        String recipientEntityID)
        
        throws SAML2Exception
    {
        Element el = EncManager.getEncInstance().encrypt(
            toXMLString(true, true),
            recipientPublicKey,
            dataEncAlgorithm,
            dataEncStrength,
            recipientEntityID,
            "EncryptedAttribute"
        );        
        return AssertionFactory.getInstance().
            createEncryptedAttribute(el);
    }

    /**
     * Returns a String representation of the element.
     *
     * @return A string containing the valid XML for this element.
     *         By default name space name is prepended to the element name.
     * @throws SAML2Exception if the object does not conform to the schema.
     */
    public String toXMLString()
        throws SAML2Exception
    {
        return this.toXMLString(true, false);
    }

    /**
     * Returns a String representation of the element.
     *
     * @param includeNS Determines whether or not the namespace qualifier is
     *                prepended to the Element when converted
     * @param declareNS Determines whether or not the namespace is declared
     *                within the Element.
     * @return A string containing the valid XML for this element
     * @throws SAML2Exception if the object does not conform to the schema.
     */
    public String toXMLString(boolean includeNS, boolean declareNS)
        throws SAML2Exception
    {
        if (name == null || name.trim().length() == 0) {
            if (SAML2SDKUtils.debug.messageEnabled()) {
                SAML2SDKUtils.debug.message("AttributeImpl.toXMLString:"
                     + " missing Attribute Name.");
            }
            throw new SAML2Exception(
                SAML2SDKUtils.bundle.getString("missingAttribute"));
        }

        StringBuffer result = new StringBuffer(1000);
        String prefix = "";
        String uri = "";
        if (includeNS) {
            prefix = SAML2Constants.ASSERTION_PREFIX;
        }
        if (declareNS) {
            uri = SAML2Constants.ASSERTION_DECLARE_STR;
        }

        result.append("<").append(prefix).append("Attribute").
            append(uri).append(" Name=\"").append(name).append("\"");
        if (nameFormat != null && nameFormat.trim().length() != 0) {
            result.append(" NameFormat=\"").append(nameFormat).append("\"");
        }
        if (friendlyName != null && friendlyName.trim().length() != 0) {
            result.append(" FriendlyName=\"").append(friendlyName).append("\"");
        }
        if (anyMap != null) {
            Iterator keyIter = anyMap.keySet().iterator();
            while (keyIter.hasNext()) {
                String key = (String) keyIter.next();
                String value = (String) anyMap.get(key);
                if (value == null) {
                    value = "";
                }
                result.append(" ").append(key).append("=\"").append(value).
                    append("\"");
            }
        }
        result.append(">");
        if (attrValues != null) {
            Iterator iter = attrValues.iterator();
            while (iter.hasNext()) {
                result.append((String) iter.next());
            }
        }
        result.append("</").append(prefix).append("Attribute>");
        return result.toString();

    }
}
