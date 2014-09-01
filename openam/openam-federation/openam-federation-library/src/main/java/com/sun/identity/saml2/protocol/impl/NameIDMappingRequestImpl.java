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
 * $Id: NameIDMappingRequestImpl.java,v 1.3 2008/11/10 22:57:03 veiming Exp $
 *
 */

package com.sun.identity.saml2.protocol.impl;

import java.security.PublicKey;
import java.util.ListIterator;
import java.util.Set;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.sun.identity.saml.xmlsig.XMLSignatureException;
import com.sun.identity.saml2.assertion.AssertionFactory;
import com.sun.identity.saml2.assertion.BaseID;
import com.sun.identity.saml2.assertion.EncryptedID;
import com.sun.identity.saml2.assertion.NameID;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2SDKUtils;
import com.sun.identity.saml2.protocol.NameIDMappingRequest;
import com.sun.identity.saml2.protocol.NameIDPolicy;
import com.sun.identity.saml2.protocol.ProtocolFactory;
import com.sun.identity.saml2.protocol.impl.RequestAbstractImpl;
import com.sun.identity.shared.DateUtils;
import com.sun.identity.shared.xml.XMLUtils;

public class NameIDMappingRequestImpl extends RequestAbstractImpl
    implements NameIDMappingRequest {

    private EncryptedID encryptedID = null;
    private NameID nameID = null;
    private BaseID baseID = null;
    private NameIDPolicy nameIDPolicy = null;

    /**
     * Constructor to create <code>ManageNameIDRequest</code> Object. 
     */
    public NameIDMappingRequestImpl() {
        elementName = "NameIDMappingRequest";
        isMutable = true;
    }

    /**
     * Constructor to create <code>ManageNameIDRequest</code> Object. 
     *
     * @param element Document Element of <code>ManageNameIDRequest<code>
     *     object.
     * @throws SAML2Exception if <code>ManageNameIDRequest<code> cannot be
     *     created.
     */
    public NameIDMappingRequestImpl(Element element) throws SAML2Exception {
        parseDOMElement(element);
        if (isSigned) {
            signedXMLString = XMLUtils.print(element);
        }
        elementName = "NameIDMappingRequest";
        makeImmutable();
    }

    /**
     * Constructor to create <code>ManageNameIDRequest</code> Object. 
     *
     * @param xmlString XML Representation of the
     *     <code>ManageNameIDRequest<code> object.
     * @throws SAML2Exception if <code>ManageNameIDRequest<code> cannot be
     *     created.
     */
    public NameIDMappingRequestImpl(String xmlString) throws SAML2Exception {
        Document doc = XMLUtils.toDOMDocument(xmlString, SAML2SDKUtils.debug);
        if (doc == null) {
            throw new SAML2Exception("errorObtainingElement");
        }
        parseDOMElement(doc.getDocumentElement());
        if (isSigned) {
            signedXMLString = xmlString;
        }
        elementName = "NameIDMappingRequest";
        makeImmutable();
    }

    /**
     * Returns the value of the <code>encryptedID</code> property.
     * 
     * @return the value of the <code>encryptedID</code> property.
     * @see #setEncryptedID(EncryptedID)
     */
    public EncryptedID getEncryptedID()
    {
        return encryptedID;
    }

    /**
     * Sets the value of the <code>encryptedID</code> property.
     * 
     * @param value the value of the <code>encryptedID</code> property.
     * @exception SAML2Exception if <code>Object</code> is immutable.
     * @see #getEncryptedID
     */
    public void setEncryptedID(EncryptedID value)
        throws SAML2Exception {

        if (!isMutable) {
            throw new SAML2Exception(
                SAML2SDKUtils.bundle.getString("objectImmutable"));
        }

        encryptedID = value;
        return;
    }

    /**
     * Returns the value of the <code>nameID</code> property.
     * 
     * @return the value of the <code>nameID</code> property.
     * @see #setNameID(NameID)
     */
    public NameID getNameID()
    {
        return nameID;
    }

    /**
     * Sets the value of the <code>nameID</code> property.
     * 
     * @param value the value of the <code>nameID</code> property.
     * @exception SAML2Exception if <code>Object</code> is immutable.
     * @see #getNameID
     */
    public void setNameID(NameID value)
        throws SAML2Exception {

        if (!isMutable) {
            throw new SAML2Exception(
                SAML2SDKUtils.bundle.getString("objectImmutable"));
        }

        nameID = value;
        return;
    }

    /**
     * Returns the <code>NameIDPolicy</code> object.
     *
     * @return the <code>NameIDPolicy</code> object.
     * @see #setNameIDPolicy(NameIDPolicy)
     */
    public NameIDPolicy getNameIDPolicy() {
        return nameIDPolicy;
    }

    /**
     * Sets the <code>NameIDPolicy</code> object.
     *
     * @param nameIDPolicy the new <code>NameIDPolicy</code> object.
     * @throws SAML2Exception if the object is immutable.
     * @see #getNameIDPolicy
     */
    public void setNameIDPolicy(NameIDPolicy nameIDPolicy)
        throws SAML2Exception {

        if (!isMutable) {
            throw new SAML2Exception(
                SAML2SDKUtils.bundle.getString("objectImmutable"));
        }
        this.nameIDPolicy = nameIDPolicy;
    }

   /**
     * Returns the value of the baseID property.
     *
     * @return the value of the baseID property
     * @see #setBaseID(com.sun.identity.saml2.assertion.BaseID)
     */
    public BaseID getBaseID() {
        return baseID;
    }

    /**
     * Sets the value of the baseID property.
     *
     * @param value the value of the baseID property to be set
     * @throws SAML2Exception if the object is immutable
     * @see #getBaseID()
     */
    public void setBaseID(BaseID value) throws SAML2Exception {
        if (isMutable) {
            this.baseID = value;
        } else {
            throw new SAML2Exception(
                SAML2SDKUtils.bundle.getString("objectImmutable"));
        }
    }

    /**
     * Parses attributes of the Docuemnt Element for this object.
     *
     * @param element the Document Element of this object.
     * @throws SAML2Exception if error parsing the Document Element.
     */
    protected void parseDOMAttributes(Element element) throws SAML2Exception {
        super.parseDOMAttributes(element);
    }

    /**
     * Parses child elements of the Docuemnt Element for this object.
     *
     * @param iter the child elements iterator.
     * @throws SAML2Exception if error parsing the Document Element.
     */
    protected void parseDOMChileElements(ListIterator iter)
        throws SAML2Exception {

        super.parseDOMChileElements(iter);

        AssertionFactory assertionFactory = AssertionFactory.getInstance();
        if (iter.hasNext()) {
            Element childElement = (Element)iter.next();
            String localName = childElement.getLocalName();
            if (SAML2Constants.BASEID.equals(localName)) {
                baseID = assertionFactory.createBaseID(childElement);
            } else if (SAML2Constants.NAMEID.equals(localName)) {
                nameID = assertionFactory.createNameID(childElement);
            }else if (SAML2Constants.ENCRYPTEDID.equals(localName)) {
                encryptedID = assertionFactory.createEncryptedID(childElement);
            } else {
                throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
                    "nameIDMReqWrongID"));
            }
        } else {
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
                "nameIDMReqWrongID"));
        }

        if (iter.hasNext()) {
            Element childElement = (Element)iter.next();
            String localName = childElement.getLocalName();
            if (SAML2Constants.NAMEID_POLICY.equals(localName)) {
                nameIDPolicy = ProtocolFactory.getInstance().createNameIDPolicy(
                    childElement);
            } else {
                throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
                    "nameIDMReqMissingNameIDPolicy"));
            }
        } else {
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
                "nameIDMReqMissingNameIDPolicy"));
        }
    }

    protected void getXMLString(Set namespaces, StringBuffer attrs,
        StringBuffer childElements, boolean includeNSPrefix, boolean declareNS)
        throws SAML2Exception {

        validateData();

        if (declareNS) {
            namespaces.add(SAML2Constants.PROTOCOL_DECLARE_STR.trim());
            namespaces.add(SAML2Constants.ASSERTION_DECLARE_STR.trim());
        }

        super.getXMLString(namespaces, attrs, childElements, includeNSPrefix,
            declareNS);


        if (baseID != null) {
            childElements.append(baseID.toXMLString(includeNSPrefix,
                declareNS)).append(SAML2Constants.NEWLINE);
        }

        if (nameID != null) {
            childElements.append(nameID.toXMLString(includeNSPrefix,
                declareNS)).append(SAML2Constants.NEWLINE);
        }
                
        if (encryptedID != null) {
            childElements.append(encryptedID.toXMLString(includeNSPrefix,
                declareNS)).append(SAML2Constants.NEWLINE);
        }
        
        childElements.append(nameIDPolicy.toXMLString(includeNSPrefix,
            declareNS)).append(SAML2Constants.NEWLINE);
    }

    protected void validateData() throws SAML2Exception {
        int count = 0;
        if (nameID != null) {
            if ((encryptedID != null) || (baseID != null)) {
                throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
                    "nameIDMReqWrongID"));
            }
        } else if (encryptedID != null) {
            if (baseID != null) {
                throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
                    "nameIDMReqWrongID"));
            }
        } else if (baseID == null) {
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
                "nameIDMReqWrongID"));
        }
    
        if (nameIDPolicy == null) {
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
                "nameIDMReqMissingNameIDPolicy"));
        }
    }
    
    /**
     * Makes this object immutable.
     */
    public void makeImmutable() {
        if (isMutable) {
            super.makeImmutable();
            if ((nameID != null) && (nameID.isMutable())) {
                nameID.makeImmutable();
            }
            if ((baseID != null) && (baseID.isMutable())) {
                baseID.makeImmutable();
            }
            if ((nameIDPolicy != null) && (nameIDPolicy.isMutable())) {
                nameIDPolicy.makeImmutable();
            }

            isMutable = false;
        }
    }
}
