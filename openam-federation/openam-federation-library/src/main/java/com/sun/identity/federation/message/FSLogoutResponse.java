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
 * $Id: FSLogoutResponse.java,v 1.4 2008/06/25 05:46:44 qcheng Exp $
 * Portions Copyrighted 2014 ForgeRock AS
 */

package com.sun.identity.federation.message;

import com.sun.identity.shared.encode.Base64;
import com.sun.identity.shared.encode.URLEncDec;
import com.sun.identity.shared.xml.XMLUtils;

import com.sun.identity.shared.DateUtils;

import com.sun.identity.federation.message.common.FSMsgException;
import com.sun.identity.federation.common.FSUtils;
import com.sun.identity.federation.common.IFSConstants;

import com.sun.identity.saml.common.SAMLConstants;
import com.sun.identity.saml.common.SAMLException;
import com.sun.identity.saml.common.SAMLResponderException;
import com.sun.identity.saml.common.SAMLVersionMismatchException;

import com.sun.identity.saml.xmlsig.XMLSignatureManager;

import com.sun.identity.saml.protocol.AbstractResponse;
import com.sun.identity.saml.protocol.Status;
import com.sun.identity.saml.protocol.StatusCode;

import java.io.IOException;

import java.text.ParseException;

import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Document;

/**
 * This class has methods to create a Liberty <code>LogoutResponse</code>.
 *
 * @supported.all.api
 * @deprecated since 12.0.0
 */
@Deprecated
public class FSLogoutResponse extends AbstractResponse {
    private String providerId;
    private String relayState;
    private Status status;
    protected String xmlString;
    protected String signatureString;
    protected String id;
    private String inResponseTo;
    
    /**
     * Default Constructor.
     */
    public FSLogoutResponse() {
        try {
            setIssueInstant(new Date());
            providerId = new String();
            StatusCode statusCode = new StatusCode(IFSConstants.SAML_SUCCESS);
            status = new Status(statusCode);
            relayState = new String();
        } catch (SAMLException e){
            FSUtils.debug.error("FSLogoutResponse.constructor:", e);
        }
        
    }
    
    /**
     * Constructor creates <code>FSLogoutResponse</code> object.
     *
     * @param responseID the value of <code>ResponseID</code> attribute.
     * @param inResponseTo the value of <code>inResponseTo</code> attribute.
     * @param status the  Logout <code>Status</code>  object.
     * @param providerId the value of <code>ProviderID</code> attribute.
     * @param relayState the value of <code>RelayState</code> attribute.
     * @throws <code>FSMsgException</code> if this object cannot be created.
     */
    public FSLogoutResponse(String responseID,
            String inResponseTo,
            Status status,
            String providerId,
            String relayState)
            throws FSMsgException {
        if ((responseID == null) || (responseID.length() == 0)) {
            this.responseID = FSUtils.generateID();
            if (this.responseID == null) {
                throw new FSMsgException("errorGenerateID",null);
            }
        } else {
            this.responseID = responseID;
        }
        
        if (inResponseTo == null) {
            FSUtils.debug.message("Response: inResponseTo is null.");
            throw new FSMsgException("nullInput",null);
        }
        this.inResponseTo = inResponseTo;
        if (status == null) {
            FSUtils.debug.message("Response: missing <Status>.");
            throw new FSMsgException("missingElement",null);
        }
        this.status = status;
        this.providerId = providerId;
        this.relayState = relayState;
        setIssueInstant(new Date());
    }
    
    /**
     * Constructor creates <code>FSLogoutResponse</code> object from
     * a Document element.
     *
     * @param root the Document element object.
     * @throws FSMsgException if this object cannot be created.
     * @throws SAMLException if there is an error.
     */
    public FSLogoutResponse(Element root) throws FSMsgException, SAMLException {
        if (root == null) {
            FSUtils.debug.message("FSLogoutResponse.parseXML: null input.");
            throw new FSMsgException("nullInput",null);
        }
        String tag = null;
        if (((tag = root.getLocalName()) == null) ||
                (!tag.equals(IFSConstants.LOGOUT_RESPONSE))) {
            FSUtils.debug.message("FSLogoutResponse.parseXML: wrong input.");
            throw new FSMsgException("wrongInput",null);
        }
        
        id = root.getAttribute(IFSConstants.ID);
        
        // Attribute ResponseID
        responseID = root.getAttribute(IFSConstants.RESPONSE_ID);
        if ((responseID == null) || (responseID.length() == 0)) {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("FSLogoutResponse.parseXML: "
                        + "Reponse doesn't have ResponseID.");
            }
            String[] args = { IFSConstants.RESPONSE_ID };
            throw new FSMsgException("missingAttribute",args);
        }
        parseMajorVersion(root.getAttribute(IFSConstants.MAJOR_VERSION));
        parseMinorVersion(root.getAttribute(IFSConstants.MINOR_VERSION));
        // Attribute InResponseTo
        inResponseTo = root.getAttribute(IFSConstants.IN_RESPONSE_TO);
        if (inResponseTo == null) {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("FSLogoutResponse.parseXML: "
                        + "Response doesn't have InResponseTo.");
            }
            String[] args = { IFSConstants.IN_RESPONSE_TO };
            throw new FSMsgException("missingAttribute",args);
        }
        // Attribute IssueInstant
        String instantString = root.getAttribute(IFSConstants.ISSUE_INSTANT);
        if ((instantString == null) || (instantString.length() == 0)) {
            FSUtils.debug.message("FSLogoutResponse(Element): "
                    + "missing IssueInstant");
            String[] args = { IFSConstants.ISSUE_INSTANT };
            throw new FSMsgException("missingAttribute",args);
        } else {
            try {
                issueInstant = DateUtils.stringToDate(instantString);
            } catch (ParseException e) {
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message(
                            "FSLogoutResponse(Element): could not " +
                            "parse IssueInstant", e);
                }
                throw new FSMsgException("wrongInput", null);
            }
        }
        
        NodeList nl = root.getChildNodes();
        Node child;
        String childName;
        int length = nl.getLength();
        for (int i = 0; i < length; i++) {
            child = nl.item(i);
            if ((childName = child.getLocalName()) != null) {
                if (childName.equals(IFSConstants.STATUS)) {
                    if (status != null) {
                        if (FSUtils.debug.messageEnabled()) {
                            FSUtils.debug.message(
                                    "FSLogoutResponse: included more"
                                    + " than one <Status>");
                        }
                        throw new FSMsgException("moreElement",null);
                    }
                    status = new Status((Element) child);
                } else if (childName.equals(IFSConstants.SIGNATURE)) {
                } else if (childName.equals(IFSConstants.PROVIDER_ID)) {
                    if (providerId != null) {
                        if (FSUtils.debug.messageEnabled()) {
                            FSUtils.debug.message(
                                    "FSLogoutResponse: included more"
                                    + " than one providerId");
                        }
                        throw new FSMsgException("moreElement",null);
                    }
                    providerId = XMLUtils.getElementValue((Element) child);
                } else if (childName.equals(IFSConstants.RELAY_STATE)) {
                    relayState = XMLUtils.getElementValue((Element) child);
                } else {
                    if (FSUtils.debug.messageEnabled()) {
                        FSUtils.debug.message("FSLogoutResponse: included wrong"
                                + " element:" + childName);
                    }
                    throw new FSMsgException("wrongInput",null);
                }
            } // end if childName != null
        } // end for loop
        
        if (status == null) {
            FSUtils.debug.message("FSLogoutResponse:missing element <Status>.");
            throw new FSMsgException("oneElement",null);
        }
        
        if (providerId == null) {
            FSUtils.debug.message(
                    "FSLogoutResponse: missing element providerId.");
            throw new FSMsgException("oneElement",null);
        }
        
        //check for signature
        List signs = XMLUtils.getElementsByTagNameNS1(root,
                SAMLConstants.XMLSIG_NAMESPACE_URI,
                SAMLConstants.XMLSIG_ELEMENT_NAME);
        int signsSize = signs.size();
        if (signsSize == 1) {
            Element elem = (Element)signs.get(0);
            setSignature(elem);
            xmlString = XMLUtils.print(root);
            signed = true;
        } else if (signsSize != 0) {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message(
                        "FSLogoutResponse(Element): included more than"
                        + " one Signature element.");
            }
            throw new FSMsgException("moreElement",null);
        }
    }
    
    /**
     * Returns the value of <code>RelayState</code> attribute.
     *
     * @return the value of <code>RelayState</code> attribute.
     * @see #setRelayState(String)
     */
    public String getRelayState(){
        return relayState;
    }
    
    /**
     * Set the value of <code>RelayState</code> attribute.
     *
     * @param relayState the value of <code>RelayState</code> attribute.
     * @see #getRelayState()
     */
    public void setRelayState(String relayState){
        this.relayState = relayState;
    }
    
    /**
     * Returns the value of <code>InResponseTo</code> attribute.
     *
     * @return the value of <code>InResponseTo</code> attribute.
     * @see #setResponseTo(String)
     */
    public String getResponseTo(){
        return inResponseTo;
    }
    
    /**
     * Sets the value of <code>InResponseTo</code> attribute.
     *
     * @param inResponseTo the value of <code>InResponseTo</code> attribute.
     * @see #getResponseTo
     */
    public void setResponseTo(String inResponseTo){
        this.inResponseTo = inResponseTo;
    }
    
    /**
     * Returns the value of <code>id</code> attribute.
     *
     * @return the value of <code>id</code> attribute.
     * @see #setID(String)
     */
    public String getID(){
        return id;
    }
    
    /**
     * Sets the value of <code>id</code> attribute.
     *
     * @param id the value of <code>id</code> attribute.
     * @see #getID()
     */
    public void setID(String id){
        this.id = id;
    }
    
    /**
     * Returns the value of <code>ProviderID</code> attribute.
     *
     * @return the value of <code>ProviderID</code> attribute.
     * @see #setProviderId(String).
     */
    
    public String getProviderId(){
        return providerId;
    }
    
    /**
     * Sets the value of  <code>ProviderID</code> attribute.
     *
     * @param providerId the value of  <code>ProviderID</code> attribute.
     * @see #getProviderId()
     */
    public void setProviderId(String providerId){
        this.providerId = providerId;
    }
    
    /**
     * Returns the Signed <code>LogoutResponse</code> string.
     *
     * @return signatureString the Signed <code>LogoutResponse</code> string.
     */
    public String getSignatureString(){
        return signatureString;
    }
    
    /**
     * Returns the value of <code>MinorVersion</code> attribute.
     *
     * @return the value of <code>MinorVersion</code> attribute.
     * @see #setMinorVersion(int)
     */
    public int getMinorVersion() {
        return minorVersion;
    }
    
    /**
     * Sets the value of <code>MinorVersion</code> attribute.
     *
     * @param version thevalue of <code>MinorVersion</code> attribute.
     * @see #getMinorVersion()
     */
    public void setMinorVersion(int version) {
        minorVersion = version;
    }
    
    /**
     * Returns the Logout <code>Status</code>.
     *
     * @return the Logout <code>Status</code>.
     * @see #setStatus(String)
     * @see #setStatus(Status)
     */
    public Status getStatus() {
        return status;
    }
    
    /**
     * Sets the Logout <code>Status</code>.
     *
     * @param status the Logout <code>Status</code code.
     * @see #getStatus
     */
    public void setStatus(String status) {
        try {
            StatusCode statusCode = new StatusCode(status);
            this.status = new Status(statusCode);
        } catch (Exception e) {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("FSLogoutResponse(Element): could not "
                        + "set attribute:", e);
            }
        }
    }
    
    /**
     * Sets the Logout <code>Status</code>.
     *
     * @param status the Logout <code>Status</code object.
     * @see #getStatus
     */
    public void setStatus(Status status) {
        this.status=status;
    }
    
    /**
     * Sets the <code>MajorVersion</code> by parsing the version string.
     *
     * @param majorVer a String representing the <code>MajorVersion</code> to
     *        be set.
     * @throws FSMsgException on error.
     * @throws SAMLException when the version mismatchs.
     */
    private void parseMajorVersion(String majorVer)
    throws FSMsgException, SAMLException {
        try {
            majorVersion = Integer.parseInt(majorVer);
        } catch (NumberFormatException e) {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("Response(Element): invalid "
                        + "MajorVersion", e);
            }
            throw new FSMsgException("wrongInput",null);
        }
        
        if (majorVersion != SAMLConstants.PROTOCOL_MAJOR_VERSION) {
            if (majorVersion > SAMLConstants.PROTOCOL_MAJOR_VERSION) {
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message("Response(Element):MajorVersion of"
                            + " the Response is too high.");
                }
                throw new SAMLVersionMismatchException(FSUtils.BUNDLE_NAME,
                        "responseVersionTooHigh",null);
            } else {
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message("Response(Element):MajorVersion of"
                            + " the Response is too low.");
                }
                throw new SAMLVersionMismatchException(FSUtils.BUNDLE_NAME,
                        "responseVersionTooLow",null);
            }
        }
    }
    
    /**
     * Sets the <code>MinorVersion</code> by parsing the version string.
     *
     * @param minorVer a String representing the <code>MinorVersion</code> to
     *        be set.
     * @throws SAMLException when the version mismatchs.
     */
    private void parseMinorVersion(String minorVer)
    throws FSMsgException, SAMLException {
        try {
            minorVersion = Integer.parseInt(minorVer);
        } catch (NumberFormatException e) {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("Response(Element): invalid "
                        + "MinorVersion", e);
            }
            throw new FSMsgException("wrongInput",null);
        }
        if (minorVersion > IFSConstants.FF_12_PROTOCOL_MINOR_VERSION) {
            FSUtils.debug.error("Response(Element):MinorVersion of"
                               + " the Response is too high.");
            throw new SAMLVersionMismatchException(FSUtils.BUNDLE_NAME,
                        "responseVersionTooHigh",null);
        } else if (minorVersion < IFSConstants.FF_11_PROTOCOL_MINOR_VERSION) {
            FSUtils.debug.error("Response(Element):MinorVersion of"
                                  + " the Response is too low.");
            throw new SAMLVersionMismatchException(FSUtils.BUNDLE_NAME,
                        "responseVersionTooLow",null);
        }
    }
    
    /**
     * Returns the <code>FSLogoutResponse</code> object.
     *
     * @param xml the XML string to be parsed.
     * @return <code>FSLogoutResponse</code> object created from the XML string.
     * @throws FSMsgException if there is
     *         error creating the object.
     */
    
    public static FSLogoutResponse parseXML(String xml) throws FSMsgException {
        FSLogoutResponse logoutResponse = null;
        try{
            Document doc = XMLUtils.toDOMDocument(xml, FSUtils.debug);
            Element root = doc.getDocumentElement();
            logoutResponse = new FSLogoutResponse(root);
        }catch(SAMLException ex){
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("FSLogoutResponse.parseXML: "
                        + "Error while parsing input xml string");
            }
            throw new FSMsgException("parseError",null, ex);
        }
        return logoutResponse;
    }
    
    /**
     * Returns a String representation of the <code>LogoutResponse</code>
     * object. This method translates the response to an XML string.
     *
     * @return An XML String representing the logout response.
     */
    public String toXMLString()  throws FSMsgException {
        return this.toXMLString(true, true);
    }
    
    /**
     * Returns a String representation of the <code>LogoutResponse</code>
     * object.
     *
     * @return An XML String representing the logout response.
     * @throws FSMsgException if there is an error converting
     *         this object ot a string.
     */
    public String toXMLString(boolean includeNS, boolean declareNS)
    throws FSMsgException {
        return toXMLString(includeNS, declareNS, false);
    }
    
    /**
     * Returns a String representation of the <code>LogoutResponse</code>
     * object.
     *
     * @param includeNS Determines whether or not the namespace qualifier
     *        is prepended to the Element when converted
     * @param declareNS Determines whether or not the namespace is declared
     *        within the Element.
     * @param includeHeader Determines whether the output include the xml
     *        declaration header.
     * @return a string containing the valid XML for this element
     * @throws FSMsgException if there is an error converting
     *         this object ot a string.
     */
    
    public String toXMLString(boolean includeNS, boolean declareNS,
            boolean includeHeader)  throws FSMsgException {
        StringBuffer xml = new StringBuffer(300);
        if (includeHeader) {
            xml.append(IFSConstants.XML_PREFIX)
            .append(SAMLConstants.DEFAULT_ENCODING)
            .append(IFSConstants.QUOTE)
            .append(IFSConstants.SPACE)
            .append(IFSConstants.QUESTION_MARK)
            .append(IFSConstants.RIGHT_ANGLE)
            .append(IFSConstants.NL);
        }
        String prefixLIB = "";
        String uriLIB = "";
        if (includeNS) {
            prefixLIB = IFSConstants.LIB_PREFIX;
        }
        
        if (declareNS) {
            if(minorVersion == IFSConstants.FF_12_PROTOCOL_MINOR_VERSION) {
                uriLIB = IFSConstants.LIB_12_NAMESPACE_STRING;
            } else {
                uriLIB = IFSConstants.LIB_NAMESPACE_STRING;
            }
        }
        
        String instantString = DateUtils.toUTCDateFormat(issueInstant);
        
        if((providerId == null) || (providerId.length() == 0)){
            FSUtils.debug.error("FSLogoutResponse.toXMLString: "
                    + "providerId is null in the response with responseId:"
                    + responseID);
            String[] args = { responseID };
            throw new FSMsgException("nullProviderIdWResponseId",args);
        }
        
        xml.append(IFSConstants.LEFT_ANGLE)
        .append(prefixLIB)
        .append(IFSConstants.LOGOUT_RESPONSE)
        .append(uriLIB)
        .append(IFSConstants.SPACE);
        
        if (minorVersion == IFSConstants.FF_11_PROTOCOL_MINOR_VERSION &&
                id != null && !(id.length() == 0)) {
            xml.append(IFSConstants.ID)
            .append(IFSConstants.EQUAL_TO)
            .append(IFSConstants.QUOTE)
            .append(id)
            .append(IFSConstants.QUOTE)
            .append(IFSConstants.SPACE)
            .append(IFSConstants.SPACE);
        }
        
        if (responseID != null) {
            xml.append(IFSConstants.RESPONSE_ID)
            .append(IFSConstants.EQUAL_TO)
            .append(IFSConstants.QUOTE)
            .append(responseID)
            .append(IFSConstants.QUOTE)
            .append(IFSConstants.SPACE)
            .append(IFSConstants.SPACE);
        }
        
        if (inResponseTo != null) {
            xml.append(IFSConstants.IN_RESPONSE_TO)
            .append(IFSConstants.EQUAL_TO)
            .append(IFSConstants.QUOTE)
            .append(inResponseTo)
            .append(IFSConstants.QUOTE)
            .append(IFSConstants.SPACE)
            .append(IFSConstants.SPACE);
        }
        
        xml.append(IFSConstants.MAJOR_VERSION)
        .append(IFSConstants.EQUAL_TO)
        .append(IFSConstants.QUOTE)
        .append(majorVersion)
        .append(IFSConstants.QUOTE)
        .append(IFSConstants.SPACE)
        .append(IFSConstants.SPACE)
        .append(IFSConstants.MINOR_VERSION)
        .append(IFSConstants.EQUAL_TO)
        .append(IFSConstants.QUOTE)
        .append(minorVersion)
        .append(IFSConstants.QUOTE)
        .append(IFSConstants.SPACE)
        .append(IFSConstants.SPACE)
        .append(IFSConstants.ISSUE_INSTANT)
        .append(IFSConstants.EQUAL_TO)
        .append(IFSConstants.QUOTE)
        .append(instantString)
        .append(IFSConstants.QUOTE)
        .append(IFSConstants.RIGHT_ANGLE);

        if (signed) {
            if (signatureString != null) {
                xml.append(signatureString);
            } else if (signature != null) {
                signatureString = XMLUtils.print(signature);
                xml.append(signatureString);
            }
        }
        
        if (providerId != null) {
            xml.append(IFSConstants.LEFT_ANGLE)
            .append(prefixLIB)
            .append(IFSConstants.PROVIDER_ID)
            .append(IFSConstants.RIGHT_ANGLE)
            .append(providerId)
            .append(IFSConstants.START_END_ELEMENT)
            .append(prefixLIB)
            .append(IFSConstants.PROVIDER_ID)
            .append(IFSConstants.RIGHT_ANGLE);
        }
        
        if (status != null) {
            xml.append(status.toString(includeNS, true));
        }
        
        if (relayState != null) {
            xml.append(IFSConstants.LEFT_ANGLE)
            .append(prefixLIB)
            .append(IFSConstants.RELAY_STATE)
            .append(IFSConstants.RIGHT_ANGLE)
            .append(relayState)
            .append(IFSConstants.START_END_ELEMENT)
            .append(prefixLIB)
            .append(IFSConstants.RELAY_STATE)
            .append(IFSConstants.RIGHT_ANGLE);
        }
        
        xml.append(IFSConstants.START_END_ELEMENT)
        .append(prefixLIB)
        .append(IFSConstants.LOGOUT_RESPONSE)
        .append(IFSConstants.RIGHT_ANGLE);
        
        return xml.toString();
    }
    
    /**
     * Returns <code>FSLogoutResponse</code> object. The object
     * is created by parsing an Base64 encode authentication
     * request String.
     *
     * @param encodedRes the encode string
     * @throws FSMsgException if there is an error creating this object.
     * @throws SAMLException if there is an error creating this object.
     */
    public static FSLogoutResponse parseBASE64EncodedString(String encodedRes)
    throws FSMsgException, SAMLException {
        if (encodedRes != null){
            String decodedNameRegRes = new String(Base64.decode(encodedRes));
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("FSLogoutResponse."
                        + "parseBASE64EncodedString: decoded input string: "
                        + decodedNameRegRes);
            }
            return parseXML(decodedNameRegRes);
        } else {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message(
                        "FSLogoutResponse.parseBASE64EncodedString"
                        + ": null String passed in as argument.");
            }
            throw new FSMsgException("nullInput",null);
        }
    }
    
    /**
     * Returns a Base64 Encoded String.
     *
     * @return a Base64 Encoded String.
     * @throws FSMsgException if there is an error encoding the string.
     */
    public String toBASE64EncodedString() throws FSMsgException {
        if ((responseID == null) || (responseID.length() == 0)){
            responseID = FSUtils.generateID();
            if (responseID == null) {
                FSUtils.debug.error("FSLogoutResponse.toBASE64EncodedString: "
                        + "couldn't generate ResponseID.");
                throw new FSMsgException("errorGenerateID",null);
            }
        }
        return Base64.encode(this.toXMLString().getBytes());
    }
    
    /**
     * Unsupported operation.
     */
    public void signXML() {
    }
    
    /**
     * Signs the <code>LogoutResponse</code>.
     *
     * @param certAlias the Certificate Alias.
     * @throws XMLSignatureException if this object cannot be signed.
     */
    public void signXML(String certAlias) throws SAMLException {
        FSUtils.debug.message("FSLogoutResponse.signXML: Called");
        if (signed) {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("FSLogoutResponse.signXML: "
                        + "the assertion is already signed.");
            }
            throw new SAMLResponderException(FSUtils.BUNDLE_NAME,
                    "alreadySigned",null);
        }
        if (certAlias == null || certAlias.length() == 0) {
            throw new SAMLResponderException(FSUtils.BUNDLE_NAME,
                    "cannotFindCertAlias",null);
        }
        try{
            XMLSignatureManager manager = XMLSignatureManager.getInstance();
            if (minorVersion == IFSConstants.FF_11_PROTOCOL_MINOR_VERSION) {
                signatureString = manager.signXML(this.toXMLString(true, true),
                        certAlias,
                        IFSConstants.DEF_SIG_ALGO,
                        IFSConstants.ID,
                        this.id, false);
            } else if (minorVersion ==
                    IFSConstants.FF_12_PROTOCOL_MINOR_VERSION) {
                signatureString =
                        manager.signXML(this.toXMLString(true, true),
                        certAlias, IFSConstants.DEF_SIG_ALGO,
                        IFSConstants.RESPONSE_ID,
                        this.getResponseID(), false);
            } else {
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message("invalid minor version.");
                }
            }
            
            signature = XMLUtils.toDOMDocument(signatureString, FSUtils.debug)
            .getDocumentElement();
            
            signed = true;
            xmlString = this.toXMLString(true, true);
        } catch (Exception e) {
            throw new SAMLResponderException(FSUtils.BUNDLE_NAME,
                    "signFailed",null);
        }
    }
    
    /**
     * Sets the Signature.
     *
     * @param elem the Document Element.
     * @return true if success otherwise false.
     */
    public boolean setSignature(Element elem) {
        signatureString = XMLUtils.print(elem);
        return super.setSignature(elem);
    }
    
    /**
     * Returns an URL Encoded String.
     *
     * @return a url encoded query string.
     * @throws FSMsgException if there is an error.
     */
    public String toURLEncodedQueryString() throws FSMsgException {
        if ((providerId == null) || (providerId.length() == 0)) {
            FSUtils.debug.error("FSLogoutResponse."
                    + "toURLEncodedQueryString: providerId is null in "
                    + "the response ");
            throw new FSMsgException("nullProviderIdInRequest",null);
        }
        if ((responseID == null) || (responseID.length() == 0)){
            responseID = FSUtils.generateID();
            if (responseID == null) {
                FSUtils.debug.error("FSNameRegistrationRequest."
                        + "toURLEncodedQueryString: couldn't generate "
                        + "responseID.");
                throw new FSMsgException("errorGenerateID",null);
            }
        }
        
        StringBuffer urlEncodedAuthnReq = new StringBuffer(300);
        urlEncodedAuthnReq.append(IFSConstants.RESPONSE_ID)
        .append(IFSConstants.EQUAL_TO)
        .append(URLEncDec.encode(responseID))
        .append(IFSConstants.AMPERSAND);
        
        if((inResponseTo != null) && (inResponseTo.length() > 0)) {
            urlEncodedAuthnReq.append(IFSConstants.IN_RESPONSE_TO)
            .append(IFSConstants.EQUAL_TO)
            .append(URLEncDec.encode(inResponseTo))
            .append(IFSConstants.AMPERSAND);
        }
        urlEncodedAuthnReq.append(IFSConstants.MAJOR_VERSION)
        .append(IFSConstants.EQUAL_TO)
        .append(majorVersion)
        .append(IFSConstants.AMPERSAND)
        .append(IFSConstants.MINOR_VERSION)
        .append(IFSConstants.EQUAL_TO)
        .append(minorVersion)
        .append(IFSConstants.AMPERSAND);
        
        if (issueInstant != null) {
            urlEncodedAuthnReq.append(IFSConstants.ISSUE_INSTANT)
            .append(IFSConstants.EQUAL_TO)
            .append(URLEncDec.encode(
                    DateUtils.toUTCDateFormat(issueInstant)))
                    .append(IFSConstants.AMPERSAND);
        } else {
            FSUtils.debug.error("FSLogoutResponse."
                    + "toURLEncodedQueryString: issueInstant missing");
            String[] args = { IFSConstants.ISSUE_INSTANT };
            throw new FSMsgException("missingAttribute",args);
            }
            if(providerId != null && providerId.length() != 0) {
                urlEncodedAuthnReq.append(IFSConstants.PROVIDER_ID)
                .append(IFSConstants.EQUAL_TO)
                .append(URLEncDec.encode(providerId))
                .append(IFSConstants.AMPERSAND);
            }
            
            if(relayState != null && relayState.length() != 0) {
                urlEncodedAuthnReq.append(IFSConstants.RELAY_STATE)
                .append(IFSConstants.EQUAL_TO)
                .append(URLEncDec.encode(relayState))
                .append(IFSConstants.AMPERSAND);
            }
            
            if (status != null) {
                urlEncodedAuthnReq.append(IFSConstants.VALUE)
                .append(IFSConstants.EQUAL_TO)
                .append(URLEncDec.encode(
                        status.getStatusCode().getValue()))
                        .append(IFSConstants.AMPERSAND);
            }
            return urlEncodedAuthnReq.toString();
        }
        
        
        /**
         * Returns <code>FSLogoutResponse</code> object. The
         * object is creating by parsing the <code>HttpServletRequest</code>
         * object.
         *
         * @param request the <code>HttpServletRequest</code> object.
         * @throws FSMsgException if there is an error creating this object.
         * @throws SAMLException if there is an error.
         */
        public static FSLogoutResponse parseURLEncodedRequest(
                HttpServletRequest request)
                throws FSMsgException, SAMLException {
            FSLogoutResponse retLogoutResponse = new FSLogoutResponse();
            try {
                FSUtils.debug.message("checking minor version");
                retLogoutResponse.majorVersion =
                        Integer.parseInt(
                              request.getParameter(IFSConstants.MAJOR_VERSION));
                retLogoutResponse.minorVersion =
                        Integer.parseInt(
                              request.getParameter(IFSConstants.MINOR_VERSION));
            } catch(NumberFormatException ex){
                throw new FSMsgException("invalidNumber",null);
            }
            
            String requestID = request.getParameter(IFSConstants.RESPONSE_ID);
            if (requestID != null) {
                retLogoutResponse.responseID = requestID ;
            } else {
                String[] args = { IFSConstants.RESPONSE_ID };
                throw new FSMsgException("missingAttribute",args);
            }
            retLogoutResponse.inResponseTo =
                    request.getParameter(IFSConstants.IN_RESPONSE_TO);
            
            String instantString =
                    request.getParameter(IFSConstants.ISSUE_INSTANT);
            if (instantString == null || instantString.length() == 0) {
                String[] args = { IFSConstants.ISSUE_INSTANT };
                throw new FSMsgException("missingAttribute",args);
            }
            try{
                retLogoutResponse.issueInstant =
                        DateUtils.stringToDate(instantString);
            } catch (ParseException e){
                throw new FSMsgException("parseError",null);
            }
            FSUtils.debug.message(" get provider Id");
            String providerID = request.getParameter(IFSConstants.PROVIDER_ID);
            if (providerID != null) {
                retLogoutResponse.providerId = providerID;
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message("ProviderID : "
                            + retLogoutResponse.providerId);
                }
            } else {
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message("ProviderID : "
                            + retLogoutResponse.providerId);
                }
                throw new FSMsgException("missingElement",null);
            }
            
            String relayState = request.getParameter(IFSConstants.RELAY_STATE);
            if (relayState != null) {
                retLogoutResponse.relayState = relayState;
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message("RelayState:"
                            + retLogoutResponse.relayState);
                }
            }
            
            String value = request.getParameter(IFSConstants.VALUE);
            if (value != null){
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message("Status : " + value);
                }
                StatusCode statusCode = new StatusCode(value);
                retLogoutResponse.status = new Status(statusCode);
            } else {
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message("Status : " + value);
                }
                throw new FSMsgException("missingElement",null);
            }
            
            FSUtils.debug.message("Returning Logout response Object");
            return retLogoutResponse;
        }
    }
