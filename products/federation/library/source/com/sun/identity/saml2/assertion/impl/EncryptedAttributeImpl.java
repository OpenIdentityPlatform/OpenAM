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
 * $Id: EncryptedAttributeImpl.java,v 1.2 2008/06/25 05:47:43 qcheng Exp $
 *
 */



package com.sun.identity.saml2.assertion.impl;

import java.security.Key;

import org.w3c.dom.Element;
import org.w3c.dom.Document;

import com.sun.identity.shared.xml.XMLUtils;
import com.sun.identity.saml2.assertion.AssertionFactory;
import com.sun.identity.saml2.assertion.Attribute;
import com.sun.identity.saml2.assertion.EncryptedAttribute;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2SDKUtils;
import com.sun.identity.saml2.xmlenc.EncManager;

/**
 * This is an implementation of interface <code>EncryptedAttribute</code>.
 *
 * The <code>EncryptedAttribute</code> element represents a SAML attribute
 * in encrypted fashion. It's of type <code>EncryptedElementType</code>.
 * <p>
 * <pre>
 * &lt;element name="EncryptedAttribute"
 * type="{urn:oasis:names:tc:SAML:2.0:assertion}EncryptedElementType"/>
 * </pre>
 */
public class EncryptedAttributeImpl implements EncryptedAttribute {

    private String xmlString = null;

    // used by the constructors.
    private void parseElement(Element element)
        throws SAML2Exception
    {
        // make sure that the input xml block is not null
        if (element == null) {
            if (SAML2SDKUtils.debug.messageEnabled()) {
                SAML2SDKUtils.debug.message("EncryptedAttributeImpl." 
                    +"parseElement: Input is null.");
            }
            throw new SAML2Exception(
                      SAML2SDKUtils.bundle.getString("nullInput"));
        }
        // Make sure this is an EncryptedAttribute.
        String tag = null;
        tag = element.getLocalName();
        if ((tag == null) || (!tag.equals("EncryptedAttribute"))) {
            if (SAML2SDKUtils.debug.messageEnabled()) {
                SAML2SDKUtils.debug.message("EncryptedAttributeImpl."
                    +"parseElement: not EncryptedAttribute.");
            }
            throw new SAML2Exception(
                      SAML2SDKUtils.bundle.getString("wrongInput"));
        }
        
    }

    /**
     * Class constructor with <code>EncryptedAttribute</code> in
     * <code>Element</code> format.
     */
    public EncryptedAttributeImpl(org.w3c.dom.Element element)
        throws com.sun.identity.saml2.common.SAML2Exception
    {
        parseElement(element);
        xmlString = XMLUtils.print(element);
    }

    /**
     * Class constructor with <code>EncryptedAttribute</code> in xml string
     * format.
     */
    public EncryptedAttributeImpl(String xmlString)
        throws com.sun.identity.saml2.common.SAML2Exception
    {
        Document doc = XMLUtils.toDOMDocument(xmlString, SAML2SDKUtils.debug);
        if (doc == null) {
            throw new SAML2Exception(
                SAML2SDKUtils.bundle.getString("errorObtainingElement"));
        }
        parseElement(doc.getDocumentElement());
        this.xmlString = xmlString;
    }

    /**
     * Returns an instance of <code>Attribute</code> object.
     *
     * @param recipientPrivateKey Private key of the recipient used to
     *                            decrypt the secret key
     * @return <code>Attribute</code> object.
     * @throws SAML2Exception if error occurs.
     */
    public Attribute decrypt(Key recipientPrivateKey)
        throws SAML2Exception
    {
        Element el = EncManager.getEncInstance().
            decrypt(xmlString, recipientPrivateKey);

        return AssertionFactory.getInstance().
            createAttribute(el);
    }

    /**
     * Returns a String representation of the element.
     *
     * @return A string containing the valid XML for this element.
     *          By default name space name is prepended to the element name.
     * @throws SAML2Exception if the object does not conform to the schema.
     */
    public String toXMLString()
        throws SAML2Exception
    {
        return xmlString;
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
    public String toXMLString(boolean includeNS, boolean declareNS)
        throws SAML2Exception
    {
        return xmlString;
    }
}
