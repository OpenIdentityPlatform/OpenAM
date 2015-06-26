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
 * $Id: NameIDImpl.java,v 1.3 2008/06/25 05:47:44 qcheng Exp $
 *
 * Portions Copyrighted 2015 ForgeRock AS.
 */


package com.sun.identity.saml2.assertion.impl;

import java.security.Key;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import com.sun.identity.shared.xml.XMLUtils;
import com.sun.identity.saml2.assertion.NameID;
import com.sun.identity.saml2.assertion.EncryptedID;
import com.sun.identity.saml2.assertion.AssertionFactory;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2SDKUtils;
import com.sun.identity.saml2.xmlenc.EncManager;

/**
 *  The <code>NameID</code> is used in various SAML assertion constructs
 *  such as <code>Subject</code> and <code>SubjectConfirmation</code>
 *  elements, and in various protocol messages.
 */
public class NameIDImpl extends NameIDTypeImpl implements NameID {

    public static final String NAME_ID_ELEMENT = "NameID";

    /** 
     * Default constructor
     */
    public NameIDImpl() {
    }

    /**
     * This constructor is used to build <code>NameID</code> object from a
     * XML string.
     *
     * @param xml A <code>java.lang.String</code> representing
     *        a <code>NameID</code> object
     * @exception SAML2Exception if it could not process the XML string
     */
    public NameIDImpl(String xml) throws SAML2Exception {
        Document document = XMLUtils.toDOMDocument(xml, SAML2SDKUtils.debug);
        if (document != null) {
            Element rootElement = document.getDocumentElement();
            processElement(rootElement);
            makeImmutable();
        } else {
            SAML2SDKUtils.debug.error(
                "NameIDImpl.processElement(): invalid XML input");
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
                "errorObtainingElement"));
        }
    }

    /**
     * This constructor is used to build <code>NameID</code> object from a
     * block of existing XML that has already been built into a DOM.
     *
     * @param element A <code>org.w3c.dom.Element</code> representing
     *        DOM tree for <code>NameID</code> object
     * @exception SAML2Exception if it could not process the Element
     */
    public NameIDImpl(Element element) throws SAML2Exception {
        processElement(element);
        makeImmutable();
    }

    private void processElement(Element element) throws SAML2Exception {
        if (element == null) {
            SAML2SDKUtils.debug.error(
                "NameIDImpl.processElement(): invalid root element");
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
                "invalid_element"));
        }
        String elemName = element.getLocalName(); 
        if (elemName == null) {
            SAML2SDKUtils.debug.error(
                "NameIDImpl.processElement(): local name missing");
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
                "missing_local_name"));
        }

        if (!elemName.equals(NAME_ID_ELEMENT)) {
            SAML2SDKUtils.debug.error(
                "NameIDImpl.processElement(): invalid local name " 
                + elemName);
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
                "invalid_local_name"));
        }
        getValueAndAttributes(element);
    }

    /**
     * Returns an <code>EncryptedID</code> object.
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
     * @return <code>EncryptedID</code> object
     * @throws SAML2Exception if error occurs during the encryption process.
     */
    public EncryptedID encrypt(
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
            "EncryptedID"
        );
        return AssertionFactory.getInstance().
            createEncryptedID(el);
    }
    
   /**
    * Returns a String representation
    * @param includeNSPrefix Determines whether or not the namespace 
    *        qualifier is prepended to the Element when converted
    * @param declareNS Determines whether or not the namespace is 
    *        declared within the Element.
    * @return A String representation
    * @exception SAML2Exception if something is wrong during conversion
    */
    public String toXMLString(boolean includeNSPrefix, boolean declareNS)
        throws SAML2Exception {
        StringBuffer sb = new StringBuffer(2000);
        String NS = "";
        String appendNS = "";
        if (declareNS) {
            NS = SAML2Constants.ASSERTION_DECLARE_STR;
        }
        if (includeNSPrefix) {
            appendNS = SAML2Constants.ASSERTION_PREFIX;
        }
        sb.append("<").append(appendNS).append(NAME_ID_ELEMENT).append(NS);
        String nameQualifier = getNameQualifier();
        if ((nameQualifier != null)
            && (nameQualifier.trim().length() != 0)) {
            sb.append(" ").append(NAME_QUALIFIER_ATTR).append("=\"").
                append(nameQualifier).append("\"");
        } 
        String spNameQualifier = getSPNameQualifier();
        if ((spNameQualifier != null)
            && (spNameQualifier.trim().length() != 0)) {
            sb.append(" ").append(SP_NAME_QUALIFIER_ATTR).append("=\"").
                append(spNameQualifier).append("\"");
        } 
        String format = getFormat();
        if ((format != null) && (format.trim().length() != 0)) {
            sb.append(" ").append(FORMAT_ATTR).append("=\"").
                append(format).append("\"");
        } 
        String spProvidedID = getSPProvidedID();
        if ((spProvidedID != null)
            && (spProvidedID.trim().length() != 0)) {
            sb.append(" ").append(SP_PROVIDED_ID_ATTR).append("=\"").
                append(spProvidedID).append("\"");
        } 
        sb.append(">");
        String value = getValue();
        if ((value != null) && (value.trim().length() != 0)) {
            sb.append(XMLUtils.escapeSpecialCharacters(value));
        } else {
            SAML2SDKUtils.debug.error(
                "NameIDImpl.processElement(): name identifier is missing");
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
                "missing_name_identifier"));
        } 
        sb.append("</").append(appendNS).append(NAME_ID_ELEMENT).
            append(">");
        return sb.toString();
    }

   /**
    * Returns a String representation
    *
    * @return A String representation
    * @exception SAML2Exception if something is wrong during conversion
    */
    public String toXMLString() throws SAML2Exception {
        return this.toXMLString(true, false);
    }
}
