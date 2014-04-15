/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: SecurityTokenReference.java,v 1.4 2008/06/25 05:50:09 qcheng Exp $
 *
 * Portions Copyrighted 2014 ForgeRock AS
 */

package com.sun.identity.wss.security;

import java.util.ResourceBundle;
import javax.xml.transform.TransformerException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import com.sun.identity.shared.debug.Debug;
import org.apache.xml.security.keys.content.X509Data;
import org.apache.xml.security.exceptions.XMLSecurityException;
import org.apache.xpath.XPathAPI;

import com.sun.identity.saml.common.SAMLUtils;
import com.sun.identity.saml.common.SAMLConstants;

/**
 * This class represents the <code>SecurityTokenReference</code> for
 * referencing the web services tokens that are used for message level
 * security in the SOAP header.
 */
public class SecurityTokenReference {

    public static final String KEYIDENTIFIER_REFERENCE = "KeyIdentifierRef";

    public static final String DIRECT_REFERENCE = "DirectReference";

    public static final String X509DATA_REFERENCE = "X509IssuerSerialRef";

    private Reference reference;
    private KeyIdentifier keyIdentifier;
    private X509Data x509Data;
    private String referenceType;
    private String id = null;
    private static ResourceBundle bundle = WSSUtils.bundle;
    private static Debug debug = WSSUtils.debug;

    // default constructor
    public SecurityTokenReference() {
        id = SAMLUtils.generateID();
    }

    /**
     * Constructor
     * @param element the security token reference.
     * @exception SecurityException if the token parsing fails.
     */
    public SecurityTokenReference(Element element) throws SecurityException {
        if(element == null) {
           throw new IllegalArgumentException(
                 bundle.getString("nullInputParameter"));
        }
        if( (!WSSConstants.TAG_SECURITYTOKEN_REFERENCE.equals(
                 element.getLocalName())) || 
            (!WSSConstants.WSSE_NS.equals(element.getNamespaceURI())) ) {
           throw new SecurityException(
                 bundle.getString("invalidElement"));
        }

        NodeList childs = element.getChildNodes();
        if(childs == null || childs.getLength() == 0 ) {
           debug.error("SecurityTokenReference.No references found");
           throw new SecurityException(
                 bundle.getString("invalidElement"));
        }

        for(int i=0; i < childs.getLength(); i++) {
            Node child = (Node)childs.item(i);
            if(child.getNodeType() != Node.ELEMENT_NODE) {
               continue;
            }

            String childName = child.getLocalName();
            if(WSSConstants.TAG_REFERENCE.equals(childName)) {
               reference = new Reference((Element)child); 
               referenceType = DIRECT_REFERENCE;

            } else if(WSSConstants.TAG_KEYIDENTIFIER.equals(childName)) {
               keyIdentifier = new KeyIdentifier((Element)child);
               referenceType = KEYIDENTIFIER_REFERENCE;

            } else if(WSSConstants.TAG_X509DATA.equals(childName)) {
               try {
                   x509Data = new X509Data((Element)child, null);
               } catch (XMLSecurityException xe) {
                   debug.error("SecurityTokenReference. invalid x509 data", xe);
                   throw new SecurityException(
                         bundle.getString("invalidElement"));
               }
               referenceType = X509DATA_REFERENCE;
            }
        }
    }

    /**
     * Returns the reference type
     *
     * @return the reference type.
     */
    public String getReferenceType() {
        return referenceType;
    }

    /**
     * Sets the reference type.
     *
     * @param referenceType the reference type.
     */
    public void setReferenceType(String referenceType) {
        this.referenceType = referenceType;
    }

    /**
     * Returns the <code>Reference</code>.
     *
     * @return the <code>Reference</code>.
     */
    public Reference getReference() {
        return reference;
    }

    /**
     * Sets the <code>Reference</code>
     *
     * @param reference the reference element.
     */
    public void setReference(Reference reference) {
        this.reference = reference;
        referenceType = DIRECT_REFERENCE;
    }

    /**
     * Sets the key identifier.
     *
     * @param keyIdentifier the key identifier.
     */
    public void setKeyIdentifier(KeyIdentifier keyIdentifier) {
        this.keyIdentifier = keyIdentifier;
        referenceType = KEYIDENTIFIER_REFERENCE;
    }

    /**
     * Returns the key identifier
     *
     * @return the key identifier
     */
    public KeyIdentifier getKeyIdentifier() {
        return keyIdentifier;
    }

    /**
     * Returns the security token reference id.
     * @return the security token reference id.
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the security token reference id.
     * @param id the security token reference id.
     */
    public void setId(String id) {
        this.id = id;
    } 

    /**
     * Returns the X509data.
     * 
     * @return the X509Data.
     */
    public X509Data getX509IssuerSerial() {
        return x509Data;
    }

    /**
     * Sets the X509Data.
     *
     * @param x509Data the X509 data.
     */
    public void setX509IssuerSerial(X509Data x509Data) {
        this.x509Data = x509Data;
        referenceType = X509DATA_REFERENCE;
    }

    /**
     * Returns the referenced security token via the Reference URI.
     *
     * @return the security token that is referenece via the reference URI.
     */
    public Element getTokenElement(Document doc) throws SecurityException {

        Reference ref = getReference();
        String uri = ref.getURI();
        if ((uri.length() == 0) || (uri.charAt(0) != '#')) {
            return null;
        }
        uri = uri.substring(1);

        Element tokenElement = null;
        String valueType = ref.getValueType();

        try {
            if(WSSConstants.ASSERTION_VALUE_TYPE.equals(valueType)) {
               tokenElement = (Element) XPathAPI.selectSingleNode(
                      doc,  "//*[@" + "AssertionID" + "=\"" + uri + "\"]");
            } else if(
                   WSSConstants.SAML2_ASSERTION_VALUE_TYPE.equals(valueType)) {
               tokenElement = (Element) XPathAPI.selectSingleNode(
                      doc,  "//*[@ID=\"" + uri + "\"]");
            } else {
               Element nscontext =  
                   org.apache.xml.security.utils.
                   XMLUtils.createDSctx(doc, WSSConstants.WSU_TAG, 
                                        WSSConstants.WSU_NS);
               tokenElement =  (Element) XPathAPI.selectSingleNode(
                     doc,  "//*[@" + "wsu:Id" + "=\"" + uri + "\"]");
            }
            return tokenElement;
        } catch (TransformerException te) {
            debug.error("SecurityTokenReference.getTokenElement: XPath "  +
            "exception.", te);
            throw new SecurityException(te.getMessage());
        }
    }


    /**
     * Adds the securitytoken reference to the parent element.
     * @param parent the parent node that securitytoken reference
     *         needs to be added.
     * @exception SecurityException if there is a failure.
     */
    public void addToParent(Element parent) throws SecurityException {
        try {
            if(parent == null) {
               throw new IllegalArgumentException(
                     bundle.getString("nullInputParameter"));
            }
            Document doc = parent.getOwnerDocument();
            Element securityTokenRef = doc.createElementNS(WSSConstants.WSSE_NS,
                    WSSConstants.TAG_SECURITYTOKEN_REFERENCE);

            securityTokenRef.setPrefix(WSSConstants.WSSE_TAG);
            securityTokenRef.setAttributeNS(SAMLConstants.NS_XMLNS,
                    SAMLConstants.TAG_XMLNS, WSSConstants.WSSE_NS);
            securityTokenRef.setAttributeNS(SAMLConstants.NS_XMLNS,
                    SAMLConstants.TAG_XMLNS, WSSConstants.WSU_NS);
            securityTokenRef.setAttributeNS(WSSConstants.WSU_NS, 
                    WSSConstants.WSU_ID, id); 

            if(reference != null) {
               reference.addToParent(securityTokenRef);
            }
 
            if(keyIdentifier != null) {
               keyIdentifier.addToParent(securityTokenRef);
            }

            parent.appendChild(securityTokenRef);
        } catch (Exception ex) {
            debug.error("SecurityTokenReference.addToParent::can not add " +
             "to parent", ex);
            throw new SecurityException(
                 bundle.getString("cannotAddElement"));
        }
    }

}
