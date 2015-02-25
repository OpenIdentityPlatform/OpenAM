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
 * $Id: FSAuthnRequestEnvelope.java,v 1.2 2008/06/25 05:46:43 qcheng Exp $
 * Portions Copyrighted 2014 ForgeRock AS
 */

package com.sun.identity.federation.message;

import com.sun.identity.federation.common.FSUtils;
import com.sun.identity.federation.common.IFSConstants;
import com.sun.identity.federation.message.common.FSMsgException;
import com.sun.identity.saml.common.SAMLUtils;
import com.sun.identity.shared.encode.Base64;
import com.sun.identity.shared.xml.XMLUtils;
import java.util.List;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * This  class defines methods for setting and retrieving attributes and
 * elements associated with a Liberty Authentication Request.
 *
 * @supported.all.api
 * @deprecated since 12.0.0
 */
@Deprecated
public class FSAuthnRequestEnvelope {
    private String assertionConsumerServiceURL = null;
    private List otherElements = null;
    private FSAuthnRequest authnRequest = null;
    private FSIDPList idpList = null;
    private String providerID = null;
    private String providerName = null;
    private boolean isPassive = false;
    private int minorVersion = IFSConstants.FF_11_PROTOCOL_MINOR_VERSION;
    
    /**
     * Default Constructor.
     */
    public FSAuthnRequestEnvelope() {
    }
    
    /**
     * Constructs a new <code>FSAuthnRequestEnvelope</code> object.
     *
     * @param authnRequest the authentication request
     * @param providerID the provider's identifier
     * @param providerName name of the provider
     * @param assertionConsumerServiceURL absolute url of the assertion
     * consumer service
     * @param idpList list of identity providers
     * @param isPassive true if identity provider must not interact
     *        with the <code>Principal</code>.
     */
    
    public FSAuthnRequestEnvelope(FSAuthnRequest authnRequest,
            String providerID,
            String providerName,
            String assertionConsumerServiceURL,
            FSIDPList idpList, boolean isPassive ) {
        this.authnRequest = authnRequest;
        this.providerID = providerID;
        this.providerName = providerName;
        this.assertionConsumerServiceURL = assertionConsumerServiceURL;
        this.idpList = idpList;
        this.isPassive = isPassive;
    }
    
    /**
     * Constructs a new <code>FSAuthnRequestEnvelope</code> object
     * from a Document Element.
     *
     * @param root the Document Element .
     * @throws FSMsgException if there is an error
     *         creating this object.
     */
    
    public FSAuthnRequestEnvelope(Element root) throws FSMsgException {
        if (root == null) {
            SAMLUtils.debug.message(
                    "FSAuthnRequestEnvelope.parseXML: null input.");
            throw new FSMsgException("nullInput",null);
        }
        String tag = null;
        if (((tag = root.getLocalName()) == null) ||
                (!tag.equals(IFSConstants.AUTHN_REQUEST_ENVELOPE))) {
            FSUtils.debug.message(
                    "FSAuthnRequestEnvelope.parseXML: wrong input.");
            throw new FSMsgException("wrongInput",null);
        }
        String ns = root.getNamespaceURI();
        if (ns == null) {
            FSUtils.debug.error("FSAuthnRequestEnvelope(Element):"
                    + " No namespace");
            throw new FSMsgException("wrongInput", null);
        }
        
        if (ns.equals(IFSConstants.FF_12_XML_NS)) {
            minorVersion = IFSConstants.FF_12_PROTOCOL_MINOR_VERSION;
        }
        
        NodeList nl = root.getChildNodes();
        Node child;
        String childName;
        int length = nl.getLength();
        for (int i = 0; i < length; i++) {
            child = nl.item(i);
            if ((childName = child.getLocalName()) != null) {
                if (childName.equals(
                        IFSConstants.ASSERTION_CONSUMER_SERVICE_URL)) {
                    assertionConsumerServiceURL =
                            XMLUtils.getElementValue((Element) child);
                } else if (childName.equals(IFSConstants.IDP_LIST)) {
                    idpList = new FSIDPList((Element) child);
                } else if (childName.equals(IFSConstants.AUTHN_REQUEST)) {
                    authnRequest = new FSAuthnRequest((Element) child);
                } else if (childName.equals(IFSConstants.PROVIDER_ID)) {
                    providerID = XMLUtils.getElementValue((Element) child);
                } else if (childName.equals(IFSConstants.PROVIDER_NAME)) {
                    providerName = XMLUtils.getElementValue((Element) child);
                } else if (childName.equals(IFSConstants.IS_PASSIVE)) {
                    String strIsPassive =
                            XMLUtils.getElementValue((Element) child);
                    boolean isPassive = false;
                    if (strIsPassive != null &&
                            strIsPassive.equals(IFSConstants.TRUE)) {
                        isPassive = true;
                    }
                }
            }
        }
    }
    
    /**
     * Returns the value of <code>MinorVersion</code> property.
     *
     * @return the  value of <code>MinorVersion</code> property.
     */
    public int getMinorVersion() {
        return minorVersion;
    }
    
    /**
     * Sets the value of <code>MinorVersion</code> property.
     *
     * @param minorVersion the value of <code>MinorVersion</code> property.
     * @see #setMinorVersion(int)
     */
    
    public void setMinorVersion(int minorVersion) {
        this.minorVersion = minorVersion;
    }
    
    /**
     * Returns the request as an XML Document String
     * based on the Liberty Request schema.
     *
     * @return XML String representing the request.
     * @throws <code>FSMsgException</code> if there is an error.
     */
    
    public String toXMLString() throws FSMsgException {
        return toXMLString(true, true);
    }
    
    /**
     * Creates a String representation of the &lt;lib:AuthnRequest&gt; element.
     * @param includeNS : Determines whether or not the namespace qualifier
     *        is prepended to the Element when converted
     * @param declareNS : Determines whether or not the namespace is declared
     *        within the Element.
     * @return String containing the valid XML for this element.
     * @throws FSMsgException if there is an error.
     */
    
    public String toXMLString(boolean includeNS,boolean declareNS)
    throws FSMsgException {
        return toXMLString(includeNS, declareNS, false);
    }
    
    /**
     * Creates a String representation of the &lt;lib:AuthnRequest&gt; element.
     *
     * @param includeNS  Determines whether or not the namespace qualifier
     *        is prepended to the Element when converted
     * @param declareNS  Determines whether or not the namespace is declared
     *        within the Element.
     * @param includeHeader Determines whether the output include the xml
     *        declaration header.
     * @return A string containing the valid XML for this element.
     * @throws <code>FSMsgException</code> if there is an error.
     */
    public String toXMLString(boolean includeNS,
            boolean declareNS,
            boolean includeHeader) throws FSMsgException {
        
        StringBuffer xml = new StringBuffer(300);
        if (includeHeader) {
            xml.append(IFSConstants.XML_PREFIX)
            .append(IFSConstants.DEFAULT_ENCODING)
            .append(IFSConstants.QUOTE)
            .append(IFSConstants.SPACE)
            .append(IFSConstants.QUESTION_MARK)
            .append(IFSConstants.RIGHT_ANGLE)
            .append(IFSConstants.NL);
        }
        String prefix = "";
        String uri = "";
        if (includeNS) {
            prefix = IFSConstants.LIB_PREFIX;
        }
        if (declareNS) {
            if(minorVersion == IFSConstants.FF_12_PROTOCOL_MINOR_VERSION) {
                uri = IFSConstants.LIB_12_NAMESPACE_STRING;
            } else {
                uri = IFSConstants.LIB_NAMESPACE_STRING;
            }
        }
        
        xml.append(IFSConstants.LEFT_ANGLE)
        .append(prefix)
        .append(IFSConstants.AUTHN_REQUEST_ENVELOPE)
        .append(uri)
        .append(IFSConstants.RIGHT_ANGLE);
        
        if (authnRequest != null){
            xml.append(authnRequest.toXMLString());
        }
        
        if (providerID != null && providerID.length() != 0){
            xml.append(IFSConstants.LEFT_ANGLE)
            .append(prefix)
            .append(IFSConstants.PROVIDER_ID)
            .append(uri)
            .append(IFSConstants.RIGHT_ANGLE)
            .append(providerID)
            .append(IFSConstants.START_END_ELEMENT)
            .append(prefix)
            .append(IFSConstants.PROVIDER_ID)
            .append(IFSConstants.RIGHT_ANGLE);
        }
        
        if (providerName != null && providerName.length() != 0){
            xml.append(IFSConstants.LEFT_ANGLE)
            .append(prefix)
            .append(IFSConstants.PROVIDER_NAME)
            .append(uri)
            .append(IFSConstants.RIGHT_ANGLE)
            .append(providerName)
            .append(IFSConstants.START_END_ELEMENT)
            .append(prefix)
            .append("ProviderName")
            .append(IFSConstants.PROVIDER_NAME)
            .append(IFSConstants.RIGHT_ANGLE);
        }
        
        if (assertionConsumerServiceURL != null &&
                assertionConsumerServiceURL.length() != 0) {
            xml.append(IFSConstants.LEFT_ANGLE)
            .append(prefix)
            .append(IFSConstants.ASSERTION_CONSUMER_SERVICE_URL)
            .append(uri)
            .append(IFSConstants.RIGHT_ANGLE)
            .append(assertionConsumerServiceURL)
            .append(IFSConstants.START_END_ELEMENT)
            .append(prefix)
            .append(IFSConstants.ASSERTION_CONSUMER_SERVICE_URL)
            .append(IFSConstants.RIGHT_ANGLE);
        }
        
        if (idpList != null){
            xml.append(idpList.toXMLString());
        }
        
        String strIsPassive = IFSConstants.FALSE;
        if (isPassive) {
            strIsPassive = IFSConstants.TRUE;
        }
        
        xml.append(IFSConstants.LEFT_ANGLE)
        .append(prefix)
        .append(IFSConstants.IS_PASSIVE)
        .append(IFSConstants.RIGHT_ANGLE)
        .append(strIsPassive)
        .append(IFSConstants.START_END_ELEMENT)
        .append(prefix)
        .append(IFSConstants.IS_PASSIVE)
        .append(IFSConstants.RIGHT_ANGLE);
        
        //Other elements needs to be handled here
        
        xml.append(IFSConstants.START_END_ELEMENT)
        .append(prefix)
        .append(IFSConstants.AUTHN_REQUEST_ENVELOPE)
        .append(IFSConstants.RIGHT_ANGLE);
        
        return xml.toString();
    }
    
    /**
     * Returns the <code>FSAuthnRequestEnvelope</code> object.
     *
     * @param xml the XML string to create this object from
     * @return <code>FSAuthnRequestEnvelope</code> object.
     * @throws FSMsgException if there is
     *         error creating the object.
     */
    
    public static FSAuthnRequestEnvelope parseXML(String xml)
    throws FSMsgException {
        Document doc = XMLUtils.toDOMDocument(xml, FSUtils.debug);
        if (doc == null) {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message(
                        "FSAuthnRequestEnvelope.parseXML:Error "
                        + "while parsing input xml string");
            }
            throw new FSMsgException("parseError",null);
        }
        Element root = doc.getDocumentElement();
        return new FSAuthnRequestEnvelope(root);
    }
    
    /**
     * Returns the value of <code>AssertionConsumerServiceURL</code> attribute.
     *
     * @return the value of <code>AssertionConsumerServiceURL</code> attribute.
     * @see #setAssertionConsumerServiceURL(String)
     */
    public String getAssertionConsumerServiceURL() {
        return assertionConsumerServiceURL;
    }
    
    /**
     * Sets the value of <code>AssertionConsumerServiceURL</code> attribute.
     *
     * @param assertionConsumerURL the value of
     *        <code>AssertionConsumerServiceURL</code> attribute.
     * @see #getAssertionConsumerServiceURL
     */
    
    public void setAssertionConsumerServiceURL(String assertionConsumerURL) {
        this.assertionConsumerServiceURL = assertionConsumerURL;
    }
    
    /**
     * Returns the <code>FSAuthnRequest</code> object.
     *
     * @return the <code>FSAuthnRequest</code> object.
     * @see #setAuthnRequest(FSAuthnRequest)
     */
    public FSAuthnRequest getAuthnRequest() {
        return authnRequest;
    }
    
    /**
     * Sets the <code>FSAuthnRequest</code> object.
     *
     * @param authnRequest the <code>FSAuthnRequest</code> object.
     * @see #getAuthnRequest
     */
    public void setAuthnRequest(FSAuthnRequest authnRequest) {
        this.authnRequest = authnRequest;
    }
    
    /**
     * Returns the <code>FSIDPList</code> object.
     *
     * return the <code>FSIDPList</code> object.
     * @see #setIDPList(FSIDPList)
     */
    public FSIDPList getIDPList() {
        return idpList;
    }
    
    /**
     * Sets the <code>FSIDPList</code> object.
     *
     * @param idpList the <code>FSIDPList</code> object.
     * @see #getIDPList
     */
    public void setIDPList(FSIDPList idpList) {
        this.idpList = idpList;
    }
    
    /**
     * Returns a list of elements.
     *
     * @return list of elements.
     * @see #setOtherElements(List)
     */
    public List getOtherElements() {
        return otherElements;
    }
    
    /**
     * Sets a list of elements.
     *
     * @param otherElements a list of elements.
     * @see #getOtherElements
     */
    public void setOtherElements(List otherElements) {
        this.otherElements = otherElements;
    }
    
    /**
     * Returns <code>FSAuthnRequestEnvelope</code> object. The object
     * is created by parsing an Base64 encode authentication
     * request String.
     *
     * @param encodedReq the encoded string.
     * @throws <code>FSMsgException</code> if there is an error
     *         creating <code>FSAuthnRequestEnvelope</code> object.
     */
    
    public static FSAuthnRequestEnvelope parseBASE64EncodedString(
            String encodedReq) throws FSMsgException {
        if (encodedReq != null) {
            String decodedAuthnReq = new String(Base64.decode(encodedReq));
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("FSAuthnRequestEnvelope."
                        + "parseBASE64EncodedString: decoded input string: \n"
                        + decodedAuthnReq);
            }
            return parseXML(decodedAuthnReq);
        } else {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message(
                        "FSAuthnRequestEnvelope.parseBASE64EncodedString: null "
                        + " String passed in as argument.");
            }
            throw new FSMsgException("nullInput",null);
        }
    }
    
    /**
     * Returns a Base64 Encoded Authentication Request String.
     *
     * @return a Base64 Encoded Authentication Request String.
     * @throws FSMsgException if there is an error encoding
     *         the string.
     */
    public String toBASE64EncodedString() throws FSMsgException {
        if((assertionConsumerServiceURL == null) ||
                (assertionConsumerServiceURL.length() == 0)) {
            FSUtils.debug.error("FSAuthnRequestEnvelope.toBASE64EncodedString:"
                    + "assertionConsumerServiceURL is null in the "
                    + "FSAuthnRequestEnvelope");
            throw new FSMsgException(
                    "noAssertionConsumerServiceURLElement",null);
        }
        if (authnRequest == null){
            FSUtils.debug.error("FSAuthnRequestEnvelope.toBASE64EncodedString:"
                    + "authnRequest is null in the FSAuthnRequestEnvelope");
            throw new FSMsgException("noAuthnRequestElement",null);
        }
        return Base64.encode(this.toXMLString().getBytes());
    }
}
