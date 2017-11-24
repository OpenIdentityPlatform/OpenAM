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
 * $Id: KeyIdentifier.java,v 1.4 2008/07/30 05:00:44 mallas Exp $
 *
 * Portions Copyrighted 2014 ForgeRock AS
 */

package com.sun.identity.wss.security;

import java.security.cert.X509Certificate;
import javax.xml.transform.TransformerException;
import java.util.ResourceBundle;
import org.w3c.dom.Element;
import org.w3c.dom.Document;
import org.w3c.dom.Text;

import com.sun.identity.saml.common.SAMLUtils;
import org.apache.xpath.XPathAPI;
import com.sun.identity.shared.debug.Debug;

/**
 * This class <code>KeyIdentifier</code> can be be used to insert the
 * key identifier information into <code>SecurityTokenReference</code>.
 */
public class KeyIdentifier {

    private String valueType = null;
    private String encodingType = null;
    private String value = null;
    private String id = null;
    private X509Certificate cert = null;
    private static ResourceBundle bundle = WSSUtils.bundle;
    private static Debug debug = WSSUtils.debug;

    /**
     * Constructor.
     *
     * @param valueType the value type of the key value.
     *
     * @param encodingType the encoding type of the key value
     *
     * @param value the key identifier value.
     */
    public KeyIdentifier(String valueType, String encodingType, String value) {
        this.valueType = valueType;
        this.encodingType = encodingType;
        this.value = value; 
        this.id = SAMLUtils.generateID();
    }

    /**
     * Constructor
     * @param element KeyIdentifier element
     *
     * @exception SecurityException if the element parsing is failed.
     */
    public KeyIdentifier(Element element) throws SecurityException {
        if(element == null) {
           throw new IllegalArgumentException(
                    bundle.getString("nullInputParameter")) ;
        }

        if( (!WSSConstants.TAG_KEYIDENTIFIER.equals(element.getLocalName()))||
                 !(WSSConstants.WSSE_NS.equals(element.getNamespaceURI())) ) {
           debug.error("KeyIdentifier.Constructor:: Invalid key identifier");
            throw new SecurityException(
                  bundle.getString("invalidElement"));
        }

        valueType = element.getAttribute("ValueType");
        encodingType = element.getAttribute("EncodingType");

        if( (valueType == null) || (encodingType == null)) {
           debug.error("KeyIdentifier.constructor:: Key Identifier does not" +
           " have ValueType or EncodingType");
           throw new SecurityException(
                  bundle.getString("invalidElement"));
        }

        this.value = element.getFirstChild().getNodeValue().trim();
       
    }

    /**
     * Returns the <code>X509Certficate</code> for the value set in the
     * key identifier.
     */
    public X509Certificate getX509Certificate() throws SecurityException {
        if(cert != null) {
           return cert;
        }        
        cert = AMTokenProvider.getX509CertForKeyIdentifier(value);
        return cert;
    }

    /**
     * Returns the value type set in the key identifier.
     *
     * @return the value type.
     */
    public String getValueType() {
        return valueType;
    }

    /**
     * Returns the encoding type in the key identifier.
     *
     * @return the encoding type.
     */
    public String getEncodingType() {
        return encodingType;
    }

    /**
     * Returns the key identifier reference value.
     *
     * @return the key identifier reference value.
     */
    public String getReferenceValue() {
        return value;
    }

    /**
     * Return the value in the key identifier.
     * @return the value in the keu identifier.
     */
    public String getValue() {
        return value;
    }

    /**
     * Sets the value in the key identifier.
     * @param value the value in the key identifier.
     */
    public void setValue(String value) {
        this.value = value;
    }

    /**
     * Returns the id.
     * @return the id.
     */
    public String getId() {
        return id;
    }

    /**
     * Returns the security token that is referenced via the key identifier.
     * @param doc the SOAPMessage XML document
     * @return the security token element matches with the assertion.
     * @exception SecurityException if there is a failure.
     */ 
    public Element getTokenElement(Document doc) throws SecurityException {

        Element tokenElement = null;

        try {
            if(WSSConstants.ASSERTION_VALUE_TYPE.equals(valueType)) {
               tokenElement = (Element) XPathAPI.selectSingleNode(
                      doc,  "//*[@" + "AssertionID" + "=\"" + value + "\"]");               
            } else if(
                   WSSConstants.SAML2_ASSERTION_VALUE_TYPE.equals(valueType)) {
               tokenElement = (Element) XPathAPI.selectSingleNode(
                      doc,  "//*[@ID=\"" + value + "\"]");
            } else {
               Element nscontext =
                   org.apache.xml.security.utils.
                   XMLUtils.createDSctx(doc, WSSConstants.WSU_TAG,
                                        WSSConstants.WSU_NS);
               tokenElement =  (Element) XPathAPI.selectSingleNode(
                     doc,  "//*[@" + "wsu:Id" + "=\"" + value + "\"]");
            }
            return tokenElement;
        } catch (TransformerException te) {
            debug.error("SecurityTokenReference.getTokenElement: XPath "  +
            "exception.", te);
            throw new SecurityException(te.getMessage());
        }

    }

    /**
     * Adds the key identifier to the parent element.
     * @param parent the parent node that keyidentifier needs to be added.
     * @exception SecurityException if there is a failure.
     */
    public void addToParent(Element parent) throws SecurityException {
        try {
            if(parent == null) {
               throw new IllegalArgumentException(
                     bundle.getString("nullInputParameter"));
            }
            Document doc = parent.getOwnerDocument();
            Element keyIdentifier = doc.createElementNS(WSSConstants.WSSE_NS,
                    WSSConstants.TAG_KEYIDENTIFIER);
            keyIdentifier.setPrefix(WSSConstants.WSSE_TAG);
            keyIdentifier.setAttributeNS(WSSConstants.WSU_NS, 
                    WSSConstants.WSU_ID, id);
            if(valueType != null) {
               keyIdentifier.setAttributeNS(null, WSSConstants.TAG_VALUETYPE,
                                 valueType);
            }
            if(value == null) {
               throw new SecurityException(
                     bundle.getString("invalidKeyIdentifier"));
            }
            Text text = doc.createTextNode(value);
            keyIdentifier.appendChild(text);
            parent.appendChild(keyIdentifier);
        } catch (Exception ex) {
            debug.error("KeyIdentifier.addToParent::can not add to parent", ex);
            throw new SecurityException(
                  bundle.getString("cannotAddElement"));
        }
    }

}
