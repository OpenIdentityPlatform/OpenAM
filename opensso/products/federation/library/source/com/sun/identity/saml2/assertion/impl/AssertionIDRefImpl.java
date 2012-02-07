/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: AssertionIDRefImpl.java,v 1.2 2008/06/25 05:47:42 qcheng Exp $
 *
 */

package com.sun.identity.saml2.assertion.impl;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.sun.identity.saml2.assertion.AssertionIDRef;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2Utils;
import com.sun.identity.shared.xml.XMLUtils;

/**
 * This class represents the AssertionIDRef element.
 * <p>The following schema fragment specifies the expected 	
 * content contained within this java content object. 	
 * <p>
 * <pre>
 * &lt;element name="AssertionIDRef" type="NCName"/>
 * </pre>
 *
 */
public class AssertionIDRefImpl implements AssertionIDRef {
    
    private String value;
    private boolean mutable = true;

    /**
     * Class constructor. Caller may need to call setters to populate the
     * object.
     */
    public AssertionIDRefImpl() {
    }

    /**
     * Class constructor with <code>AssertionIDRef</code> in
     * <code>Element</code> format.
     *
     * @param element A <code>Element</code> representing DOM tree for
     *     <code>AssertionIDRef</code> object
     * @exception SAML2Exception if it could not process the Element
     */
    public AssertionIDRefImpl(Element element) throws SAML2Exception
    {
        parseElement(element);
    }

    /**
     * Class constructor with <code>AssertionIDRef</code> in xml string format.
     *
     * @param xmlString A <code>String</code> representing a
     *     <code>AssertionIDRef</code> object
     * @exception SAML2Exception if it could not process the XML string
     */
    public AssertionIDRefImpl(String xmlString) throws SAML2Exception
    {
        Document doc = XMLUtils.toDOMDocument(xmlString, SAML2Utils.debug);
        if (doc == null) {
            throw new SAML2Exception(SAML2Utils.bundle.getString(
                "errorObtainingElement"));
        }
        parseElement(doc.getDocumentElement());
    }

    /**
     * Returns the value of the <code>AssertionIDRef</code>.
     *
     * @return the value of this <code>AssertionIDRef</code>.
     * @see #setValue(String)
     */
    public String getValue()
    {
        return value;
    }

    /**
     * Sets the value of this <code>AssertionIDRef</code>.
     *
     * @param value new <code>AssertionIDRef</code>.
     * @throws SAML2Exception if the object is immutable.
     * @see #getValue()
     */
    public void setValue(String value) throws SAML2Exception
    {
        if (!mutable) {
            throw new SAML2Exception(SAML2Utils.bundle.getString(
                "objectImmutable"));
        }
        this.value = value;
    }

    /**
     * Makes the object immutable.
     */
    public void makeImmutable() {
        mutable = false;
    }

    /**
     * Returns the mutability of the object.
     *
     * @return true if the object is mutable; false otherwise.
     */
    public boolean isMutable() {
        return mutable;
    }

    /**
     * Returns a String representation of the element.
     *
     * @return A string containing the valid XML for this element.
     *     By default name space name is prepended to the element name.
     * @throws SAML2Exception if the object does not conform to the schema.
     */
    public String toXMLString() throws SAML2Exception
    {
        return toXMLString(true, false);
    }

    /**
     * Returns a String representation of the element.
     *
     * @param includeNS Determines whether or not the namespace qualifier is
     *     prepended to the Element when converted
     * @param declareNS Determines whether or not the namespace is declared
     *     within the Element.
     * @return A string containing the valid XML for this element
     * @throws SAML2Exception if the object does not conform to the schema.
     */
    public String toXMLString(boolean includeNS, boolean declareNS)
        throws SAML2Exception
    {
        if ((value == null) || (value.trim().length() == 0)) {
            if (SAML2Utils.debug.messageEnabled()) {
                SAML2Utils.debug.message("AssertionIDRefImpl.toXMLString: "+
                    "AssertionIDRef value is null or empty.");
            }
            throw new SAML2Exception(SAML2Utils.bundle.getString(
                "emptyElementValue"));
        }

        String prefix = "";
        String uri = "";
        if (includeNS) {
            prefix = SAML2Constants.ASSERTION_PREFIX;
        }
        if (declareNS) {
            uri = SAML2Constants.ASSERTION_DECLARE_STR;
        }

        return ("<" + prefix + SAML2Constants.ASSERTION_ID_REF + uri + ">" +
            value + "</" + prefix + SAML2Constants.ASSERTION_ID_REF + ">");
    }

    private void parseElement(Element element) throws SAML2Exception
    {

        if (element == null) {
            SAML2Utils.debug.message("AssertionIDRefImpl.parseElement:"+
                " Input is null.");
            throw new SAML2Exception(SAML2Utils.bundle.getString(
                "nullInput"));
        }

        String tag = element.getLocalName();
        if (!SAML2Constants.ASSERTION_ID_REF.equals(tag)) {
            SAML2Utils.debug.message("AssertionIDRefImpl.parseElement: " +
                "Element local name is not AssertionIDRef.");
            throw new SAML2Exception(SAML2Utils.bundle.getString("wrongInput"));
        }

        NodeList  nodes = element.getChildNodes();
        int nodeCount = nodes.getLength();
        if (nodeCount > 0) {
            for (int i = 0; i < nodeCount; i++) {
                Node currentNode = nodes.item(i);
                if (currentNode.getNodeType() == Node.ELEMENT_NODE) {
                    if (SAML2Utils.debug.messageEnabled()) {
                        SAML2Utils.debug.message(
                            "AssertionIDRefImpl.parseElement: " +
                            "AssertionIDRef can't have child element.");
                    }
                    throw new SAML2Exception(SAML2Utils.bundle.getString(
                        "wrongInput"));
                }
            }
        }

        value = XMLUtils.getElementValue(element);
        if ((value == null) || (value.trim().length() == 0)) {
            if (SAML2Utils.debug.messageEnabled()) {
                SAML2Utils.debug.message("AssertionIDRefImpl.parseElement: " +
                    "AssertionIDRef value is null or empty.");
            }
            throw new SAML2Exception(SAML2Utils.bundle.getString(
                "emptyElementValue"));
        }
        mutable = false;
    }
}
