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
 * $Id: StatusCodeImpl.java,v 1.2 2008/06/25 05:48:00 qcheng Exp $
 *
 * Portions copyright 2014 ForgeRock AS.
 */


package com.sun.identity.saml2.protocol.impl;

import com.sun.identity.shared.xml.XMLUtils;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2SDKUtils;
import com.sun.identity.saml2.protocol.StatusCode;
import com.sun.identity.saml2.protocol.ProtocolFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * This class defines methods for <code>StatusCode</code> element.
 */

public class StatusCodeImpl implements StatusCode {
    
    private boolean isMutable = false;
    private StatusCode statusCode = null;
    private String statusCodeValue = null;
    
    /**
     * Constructs the <code>StatusCode</code> Object.
     */
    public StatusCodeImpl() {
        isMutable=true;
    }
    
    /**
     * Constructs the <code>StatusCode</code> Object.
     *
     * @param element the Document Element of <code>StatusCode</code> object.
     * @throws SAML2Exception if <code>StatusCode</code> cannot be created.
     */
    
    public StatusCodeImpl(Element element) throws SAML2Exception {
        parseElement(element);
    }
    
    /**
     * Constructs the <code>StatusCode</code> Object.
     *
     * @param xmlString the XML String representation of this object.
     * @throws SAML2Exception if <code>StatusCode</code> cannot be created.
     */
    public StatusCodeImpl(String xmlString) throws SAML2Exception {
        Document xmlDocument =
        XMLUtils.toDOMDocument(xmlString,SAML2SDKUtils.debug);
        if (xmlDocument == null) {
            throw new SAML2Exception(
            SAML2SDKUtils.bundle.getString("errorObtainingElement"));
        }
        parseElement(xmlDocument.getDocumentElement());
    }
    
    /**
     * Returns the value of the statusCode property.
     *
     * @return the value of the statusCode property
     * @see #setStatusCode(StatusCode)
     */
    public StatusCode getStatusCode() {
        return statusCode;
    }
    
    /**
     * Sets the value of the statusCode property.
     *
     * @param value the value of the statusCode property to be set
     * @throws SAML2Exception if the object is immutable
     * @see #getStatusCode
     */
    public void setStatusCode(StatusCode value) throws SAML2Exception {
        if (isMutable) {
            this.statusCode = value;
        } else {
            throw new SAML2Exception(
            SAML2SDKUtils.bundle.getString("objectImmutable"));
        }
    }
    
    /**
     * Returns the status code value of the value property.
     *
     * @return the value of the value property
     * @see #setValue(String)
     */
    public java.lang.String getValue() {
        return statusCodeValue;
    }
    
    /**
     * Sets the status code value of the value property.
     *
     * @param value the value of the value property to be set
     * @exception SAML2Exception if the object is immutable
     * @see #getValue
     */
    public void setValue(java.lang.String value) throws SAML2Exception {
        if (isMutable) {
            this.statusCodeValue = value;
        } else {
            throw new SAML2Exception(
            SAML2SDKUtils.bundle.getString("objectImmutable"));
        }
    }
    
    /**
     * Returns the <code>StatusCode</code> in an XML document String format
     * based on the <code>StatusCode</code> schema described above.
     *
     * @return An XML String representing the <code>StatusCode</code>.
     * @throws SAML2Exception if some error occurs during conversion to
     *         <code>String</code>.
     */
    public String toXMLString() throws SAML2Exception {
        return toXMLString(true,false);
    }
    
    /**
     * Returns the <code>StatusCode</code> in an XML document String format
     * based on the <code>StatusCode</code> schema described above.
     *
     * @param includeNSPrefix Determines whether or not the namespace qualifier 
     *        is prepended to the Element when converted
     * @param declareNS Determines whether or not the namespace is declared
     *        within the Element.
     * @return A XML String representing the <code>StatusCode</code>.
     * @throws SAML2Exception if some error occurs during conversion to
     *         <code>String</code>.
     */
    public String toXMLString(boolean includeNSPrefix,
    boolean declareNS) throws SAML2Exception {
        String xmlStr = null;
        if ((statusCodeValue != null) && (statusCodeValue.length() != 0)) {
            StringBuffer xmlString = new StringBuffer(500);
            xmlString.append(SAML2Constants.START_TAG);
            if (includeNSPrefix) {
                xmlString.append(SAML2Constants.PROTOCOL_PREFIX);
            }
            xmlString.append(SAML2Constants.STATUS_CODE);
            if (declareNS) {
                xmlString.append(SAML2Constants.PROTOCOL_DECLARE_STR);
            }
            xmlString.append(SAML2Constants.SPACE);
            
            xmlString.append(SAML2Constants.VALUE)
            .append(SAML2Constants.EQUAL)
            .append(SAML2Constants.QUOTE)
            .append(statusCodeValue)
            .append(SAML2Constants.QUOTE);
            
            xmlString.append(SAML2Constants.END_TAG);
            
            if (statusCode != null) {
                xmlString.append(SAML2Constants.NEWLINE)
                .append(statusCode.toXMLString(includeNSPrefix,declareNS));
            }
            
            xmlString.append(SAML2Constants.NEWLINE)
            .append(SAML2Constants.SAML2_END_TAG)
            .append(SAML2Constants.STATUS_CODE)
            .append(SAML2Constants.END_TAG);
            
            xmlStr = xmlString.toString();
        }
        return xmlStr;
    }
    
    /**
     * Makes this object immutable.
     */
    public void makeImmutable() {
        if (isMutable) {
            if ((statusCode != null) && (statusCode.isMutable())) {
                statusCode.makeImmutable();
            }
            isMutable = false;
        }
    }
    
    /**
     * Returns value true if object is mutable.
     *
     * @return true if object is mutable.
     */
    public boolean isMutable() {
        return isMutable;
    }
    
    /* Parses the <code>StatusCode</code> Element. */
    private void parseElement(Element element) throws SAML2Exception {
        ProtocolFactory protoFactory = ProtocolFactory.getInstance();
        statusCodeValue = element.getAttribute(SAML2Constants.VALUE);
        validateStatusCodeValue(statusCodeValue);
        
        NodeList nList = element.getChildNodes();
        if ((nList != null) && (nList.getLength() > 0)) {
            for (int i = 0; i < nList.getLength(); i++) {
                Node childNode = nList.item(i);
                String cName = childNode.getLocalName();
                if (cName != null) {
                    if (cName.equals(SAML2Constants.STATUS_CODE)) {
                        statusCode =
                        protoFactory.createStatusCode((Element)childNode);
                    }
                }
            }
        }
    }
    
    /* validates the required attribute Value */
    void validateStatusCodeValue(String statusCodeValue) throws SAML2Exception {
        if ((statusCodeValue == null) || (statusCodeValue.length() == 0)) {
            SAML2SDKUtils.debug.message("statusCodeValue is required");
            throw new SAML2Exception(
            SAML2SDKUtils.bundle.getString("missingStatusCodeValue"));
        }
    }
}
