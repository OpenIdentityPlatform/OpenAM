/*
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
 * $Id: FSFederationTerminationNotification.java,v 1.3 2008/06/25 05:46:44 qcheng Exp $
 *
 * Portions Copyrighted 2014-2016 ForgeRock AS.
 */

package com.sun.identity.federation.message;


import static org.forgerock.openam.utils.Time.*;

import com.sun.identity.federation.common.FSUtils;
import com.sun.identity.federation.common.IFSConstants;
import com.sun.identity.federation.message.common.FSMsgException;
import com.sun.identity.saml.assertion.NameIdentifier;
import com.sun.identity.saml.common.SAMLUtils;
import com.sun.identity.saml.common.SAMLConstants;
import com.sun.identity.saml.common.SAMLException;
import com.sun.identity.saml.common.SAMLResponderException;
import com.sun.identity.saml.protocol.AbstractRequest;
import com.sun.identity.saml.xmlsig.XMLSignatureManager;
import com.sun.identity.shared.encode.Base64;
import com.sun.identity.shared.DateUtils;
import com.sun.identity.shared.encode.URLEncDec;
import com.sun.identity.shared.xml.XMLUtils;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.w3c.dom.Element;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * This class has methods for creating object and message for
 * Federation Termination.
 *
 * @supported.all.api
 * @deprecated since 12.0.0
 */
@Deprecated

public class FSFederationTerminationNotification extends AbstractRequest {
    private String providerId;
    private NameIdentifier nameIdentifier;
    protected String xmlString;
    protected String signatureString;
    protected String id;
    private String relayState;    
    
    /**
     * Default Constructor.
     */
    public FSFederationTerminationNotification() {
        try {
            setIssueInstant(newDate());
            providerId = new String();
            nameIdentifier = new NameIdentifier("Test", "Test");
        } catch(SAMLException e){
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message(
                        "FSFederationTerminationNotification.constructor:", e);
            }
        }
        
    }
    
    /**
     * Creates <code>FSFederationTerminationNotification<object> from
     * a Document Element.
     *
     * @param root the Document Element.
     * @throws FSMsgException if there is an error creating
     *         this object.
     */
    public FSFederationTerminationNotification(Element root)
    throws FSMsgException {
        String tag = null;
        if (root == null) {
            FSUtils.debug.message(
                    "FSFederationTerminationNotification(Element):null input.");
            throw new FSMsgException("nullInput",null);
        }
        if (((tag = root.getLocalName()) == null) ||
                (!tag.equals("FederationTerminationNotification"))) {
            FSUtils.debug.message(
                    "FSFederationTerminationNotification(Element):wrong input");
            throw new FSMsgException("wrongInput",null);
        }
        // Attribute IssueInstant
        String instantString = root.getAttribute(IFSConstants.ISSUE_INSTANT);
        if ((instantString == null) || (instantString.length() == 0)) {
            FSUtils.debug.message("FederationTerminationNotification(Element): "
                    + "missing IssueInstant");
            String[] args = { IFSConstants.ISSUE_INSTANT };
            throw new FSMsgException("missingAttribute",args);
        } else {
            try {
                issueInstant = DateUtils.stringToDate(instantString);
            } catch (ParseException e) {
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message("FederationTerminationNotification "
                            + " (Element): could not parse IssueInstant", e);
                }
                throw new FSMsgException("wrongInput", null);
            }
        }
        int length = 0;
        id = root.getAttribute(IFSConstants.ID);
        requestID = root.getAttribute(IFSConstants.REQUEST_ID);
        parseMajorVersion(root.getAttribute(IFSConstants.MAJOR_VERSION));
        parseMinorVersion(root.getAttribute(IFSConstants.MINOR_VERSION));
        NodeList contentnl = root.getChildNodes();
        Node child;
        String nodeName;
        length = contentnl.getLength();
        for (int i = 0; i < length; i++) {
            child = contentnl.item(i);
            if ((nodeName = child.getLocalName()) != null) {
                if (nodeName.equals(IFSConstants.RESPONDWITH)) {
                    if (respondWiths == Collections.EMPTY_LIST) {
                        respondWiths = new ArrayList();
                    }
                    respondWiths.add(XMLUtils.getElementValue((Element) child));
                } else if (nodeName.equals(IFSConstants.SIGNATURE)) {
                } else if (nodeName.equals(IFSConstants.PROVIDER_ID)) {
                    if (providerId != null) {
                        if (FSUtils.debug.messageEnabled()) {
                            FSUtils.debug.message(
                                    "FSFederationTerminationNotification(" +
                                    "Element : should contain only one " +
                                    "ProviderID.");
                        }
                        throw new FSMsgException("wrongInput",null);
                    }
                    providerId = XMLUtils.getElementValue((Element) child);
                } else if (nodeName.equals(IFSConstants.NAME_IDENTIFIER)) {
                    try{
                        this.nameIdentifier =
                                new NameIdentifier((Element)child);
                    } catch(SAMLException ex){
                        if (FSUtils.debug.messageEnabled()) {
                            FSUtils.debug.message(
                                    "FSFederationTerminationNotification "
                                    + "(Element): SAMLException "
                                    + "while constructing nameidentifier");
                        }
                        throw new FSMsgException("nameIdentifierCreateError",
                                                  null);
                    }
                } else if (nodeName.equals(IFSConstants.RELAY_STATE)){
                    if (relayState != null) {
                        if (FSUtils.debug.messageEnabled()) {
                            FSUtils.debug.message(
                                    "FSFederationTerminationNotification "
                                    + "(Element) :should contain only one "
                                    + "relayState.");
                        }
                        throw new FSMsgException("wrongInput",null);
                    }
                    relayState = XMLUtils.getElementValue((Element) child);
                } else {
                    if (FSUtils.debug.messageEnabled()) {
                        FSUtils.debug.message(
                                "FSFederationTerminationNotification(Element): "
                                + " invalid node" + nodeName);
                    }
                    throw new FSMsgException("wrongInput",null);
                }
            }
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
                        "FSFederationTerminationNotification(Element):"
                        + "included more than one Signature element.");
            }
            throw new FSMsgException("moreElement",null);
        }
    }
    
    /**
     * Creates <code>FSFederationTerminationNotification</code> object.
     *
     * @param requestId the request identifier.
     * @param providerID the provider identifier.
     * @param nameId the <code>NameIdentifier</code> object.
     * @throws FSMsgException if there is an error creating
     *         this object.
     */
    public FSFederationTerminationNotification(String requestId,
            String providerID,NameIdentifier nameId) throws FSMsgException {
        int length = 0;
        int i = 0;
        setIssueInstant(newDate());
        if ((respondWiths != null) &&
                (respondWiths != Collections.EMPTY_LIST)) {
            length = respondWiths.size();
            for (i = 0; i < length; i++) {
                Object temp = respondWiths.get(i);
                if (!(temp instanceof String)) {
                    if (FSUtils.debug.messageEnabled()) {
                        FSUtils.debug.message(
                                "FSFederationTerminationNotification:"
                                + "wrong input for RespondWith");
                    }
                    throw new FSMsgException("wrongInput",null);
                }
            }
            this.respondWiths = respondWiths;
        }
        
        if ((requestId != null) && (requestId.length() != 0)) {
            requestID = requestId;
        } else {
            // random generate one
            requestID = SAMLUtils.generateID();
            if (requestID == null) {
                FSUtils.debug.error("FSFederationTerminationNotification: "
                        + "couldn't generate RequestID.");
                throw new FSMsgException("errorGenerateID",null);
            }
        }
        this.providerId = providerID;
        this.nameIdentifier = nameId;
    }
    
    /**
     * Returns the string representation of this object.
     * This method translates the response to an XML document string based on
     * the Response schema described above.
     *
     * @return An XML String representing the response. NOTE: this is a
     *         complete SAML response xml string with ResponseID,
     *         MajorVersion, etc.
     * @throws FSMsgException if there is an error converting
     *         this object ot a string.
     */
    public String toXMLString(boolean includeNS, boolean declareNS)
    throws FSMsgException {
        return toXMLString(includeNS, declareNS, false);
    }
    
    
    /**
     * Returns a String representation of the &lt;samlp:Response&gt; element.
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
            boolean includeHeader) throws FSMsgException {
        if((providerId == null) || (providerId.length() == 0)){
            FSUtils.debug.error(
                    "FSFederationTerminationNotification.toXMLString"
                    + ": providerId is null in the request with requestId:"
                    + requestID);
            String[] args = { requestID };
            throw new FSMsgException("nullProviderIdWRequestId" ,args);
        }
        if ((requestID == null) || (requestID.length() == 0)){
            requestID = SAMLUtils.generateID();
            if (requestID == null) {
                FSUtils.debug.error("FSFederationTerminationNotification."
                        + "toXMLString: couldn't generate RequestID.");
                throw new FSMsgException("errorGenerateID",null);
            }
        }
        
        StringBuffer xml = new StringBuffer(300);
        if (includeHeader) {
            xml.append(IFSConstants.XML_PREFIX)
            .append(IFSConstants.QUOTE)
            .append(IFSConstants.SPACE)
            .append(IFSConstants.QUESTION_MARK)
            .append(IFSConstants.RIGHT_ANGLE);
        }
        String prefix = "";
        String uriSAML = "";
        String uri = "";
        if (includeNS) {
            prefix = IFSConstants.LIB_PREFIX;
        }
        if (declareNS) {
            uri = IFSConstants.LIB_NAMESPACE_STRING;
            if (minorVersion == IFSConstants.FF_12_PROTOCOL_MINOR_VERSION) {
                uri = IFSConstants.LIB_12_NAMESPACE_STRING;
            }
            uriSAML = IFSConstants.assertionDeclareStr;
        }
        
        String instantString = DateUtils.toUTCDateFormat(issueInstant);
        
        if(requestID != null){
            xml.append(IFSConstants.LEFT_ANGLE)
            .append(prefix)
            .append(IFSConstants.FEDERATION_TERMINATION_NOTICFICATION)
            .append(uri)
            .append(uriSAML)
            .append(IFSConstants.SPACE);
            
            if (minorVersion == IFSConstants.FF_11_PROTOCOL_MINOR_VERSION &&
                    id != null && !(id.length() == 0)) {
                xml.append(IFSConstants.SPACE)
                   .append("id")
                   .append(IFSConstants.EQUAL_TO)
                   .append(IFSConstants.QUOTE)
                   .append(id)
                   .append(IFSConstants.QUOTE)
                   .append(IFSConstants.SPACE);
            }
            xml.append(IFSConstants.REQUEST_ID)
            .append(IFSConstants.EQUAL_TO)
            .append(IFSConstants.QUOTE)
            .append(requestID)
            .append(IFSConstants.QUOTE)
            .append(IFSConstants.SPACE)
            .append(IFSConstants.MAJOR_VERSION)
            .append(IFSConstants.EQUAL_TO)
            .append(IFSConstants.QUOTE)
            .append(majorVersion)
            .append(IFSConstants.QUOTE)
            .append(IFSConstants.SPACE)
            .append(IFSConstants.MINOR_VERSION)
            .append(IFSConstants.EQUAL_TO)
            .append(IFSConstants.QUOTE)
            .append(minorVersion)
            .append(IFSConstants.QUOTE)
            .append(IFSConstants.SPACE)
            .append(IFSConstants.ISSUE_INSTANT)
            .append(IFSConstants.EQUAL_TO)
            .append(IFSConstants.QUOTE)
            .append(instantString)
            .append(IFSConstants.QUOTE)
            .append(IFSConstants.RIGHT_ANGLE);
            
            if ((respondWiths != null) &&
                    (respondWiths != Collections.EMPTY_LIST)) {
                Iterator i = respondWiths.iterator();
                while (i.hasNext()) {
                    xml.append(IFSConstants.LEFT_ANGLE)
                    .append(prefix)
                    .append(IFSConstants.RESPONDWITH)
                    .append(IFSConstants.RIGHT_ANGLE)
                    .append((String) i.next())
                    .append(IFSConstants.START_END_ELEMENT)
                    .append(prefix)
                    .append(IFSConstants.RESPONDWITH)
                    .append(IFSConstants.LEFT_ANGLE);
                }
            }
            
            if (signed) {
                if (signatureString != null) {
                    xml.append(signatureString);
                } else if (signature != null) {
                    signatureString = XMLUtils.print(signature);
                    xml.append(signatureString);
                }
            }
            
            xml.append(IFSConstants.LEFT_ANGLE)
            .append(prefix)
            .append(IFSConstants.PROVIDER_ID)
            .append(uri)
            .append(IFSConstants.RIGHT_ANGLE)
            .append(providerId)
            .append(IFSConstants.START_END_ELEMENT)
            .append(prefix)
            .append(IFSConstants.PROVIDER_ID)
            .append(IFSConstants.RIGHT_ANGLE);
            
            if (nameIdentifier != null) {
                xml.append(nameIdentifier.toString());
            }
            
            if (relayState != null) {
                xml.append(IFSConstants.LEFT_ANGLE)
                .append(prefix)
                .append(IFSConstants.RELAY_STATE)
                .append(uri)
                .append(IFSConstants.RIGHT_ANGLE)
                .append(providerId)
                .append(IFSConstants.START_END_ELEMENT)
                .append(prefix)
                .append(IFSConstants.RELAY_STATE)
                .append(IFSConstants.RIGHT_ANGLE);
            }
            
            xml.append(IFSConstants.START_END_ELEMENT)
            .append(prefix)
            .append(IFSConstants.FEDERATION_TERMINATION_NOTICFICATION)
            .append(IFSConstants.RIGHT_ANGLE);
        } else {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("FSFederationTerminationNotification."
                        + "toString: requestID is null ");
            }
            throw new FSMsgException("nullRequestID",null);
        }
        return xml.toString();
    }
    
    /**
     * Returns the string representation of this object.
     * This method translates the response to an XML document string.
     *
     * @return An XML String representing the response. NOTE: this is a
     *         complete SAML response xml string with ResponseID,
     *         MajorVersion, etc.
     */
    public String toXMLString() throws FSMsgException {
        return toXMLString(true, true);
    }
    /**
     * Returns the <code>FSAuthnRequest</code> object.
     *
     * @param xml the XML string to be parsed.
     * @return <code>FSAuthnRequest</code> object created from the XML string.
     * @throws FSMsgException if there is
     *         error creating the object.
     */
    public static FSFederationTerminationNotification parseXML(String xml)
    throws FSMsgException {
        Document doc = XMLUtils.toDOMDocument(xml, FSUtils.debug);
        if (doc == null) {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message(
                        "FSFederationTerminationNotification.parseXML:Error "
                        + "while parsing input xml string");
            }
            throw new FSMsgException("parseError",null);
        }
        Element root = doc.getDocumentElement();
        return new FSFederationTerminationNotification(root);
    }
    
    /**
     * Returns the value of <code>id</code> attribute.
     *
     * @return the value of <code>id</code> attribute.
     * @see #setID(String)
     */
    public String getID() {
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
     * Set the value of <code>RelayState</code> attribute.
     *
     * @param relayState the value of <code>RelayState</code> attribute.
     * @see #getRelayState()
     */
    public void setRelayState(String relayState){
        this.relayState = relayState;
    }
    
    /**
     * Returns the value of <code>RelayState</code> attribute.
     *
     * @return the value of <code>RelayState</code> attribute.
     * @see #setRelayState(String)
     */
    public String getRelayState() {
        return relayState;
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
     * @param version the value of <code>MinorVersion</code> attribute.
     * @see #getMinorVersion()
     */
    public void setMinorVersion(int version) {
        minorVersion = version;
    }
    /**
     * Returns the value of <code>ProviderID</code> attribute.
     *
     * @return the value of <code>ProviderID</code> attribute.
     * @see #setProviderId(String).
     */
    public String getProviderId() {
        return providerId;
    }
    
    /**
     * Sets the value of <code>ProviderID</code> attribute.
     *
     * @param providerID the value of <code>ProviderID</code> attribute.
     * @see #getProviderId()
     */
    public void setProviderId(String providerID) {
        this.providerId = providerID;
    }
    
    /**
     * Returns the <code>NameIdentifier</code> object.
     *
     * @return the <code>NameIdentifier</code> object.
     */
    public NameIdentifier getNameIdentifier() {
        return nameIdentifier;
    }
    
    /**
     * Sets the <code>NameIdentifier</code> object.
     *
     * @param nameId the <code>NameIdentifier</code> object.
     */
    public void setNameIdentifier(NameIdentifier nameId) {
        this.nameIdentifier = nameId;
    }
    
    /**
     * Returns an <code>URL</code> encoded query string.
     *
     * @return a <code>URL</code> encoded query string.
     * @throws FSMsgException if there is an error.
     */
    public String toURLEncodedQueryString() throws FSMsgException {
        if((providerId == null) || (providerId.length() == 0)) {
            FSUtils.debug.error("FSFederationTerminationNotification."
                    + "toURLEncodedQueryString: providerId is null in the "
                    + "request with requestId: " + requestID);
            String[] args = { requestID };
            throw new FSMsgException("nullProviderIdWRequestId",args);
        }
        if ((requestID == null) || (requestID.length() == 0)) {
            requestID = SAMLUtils.generateID();
            if (requestID == null) {
                FSUtils.debug.error("FSFederationTerminationNotification."
                        + "toURLEncodedQueryString: couldn't generate "
                        + "RequestID.");
                throw new FSMsgException("errorGenerateID",null);
            }
        }
        StringBuffer urlEncodedAuthnReq = new StringBuffer(300);
        urlEncodedAuthnReq.append(IFSConstants.REQUEST_ID)
        .append(IFSConstants.EQUAL_TO)
        .append(URLEncDec.encode(requestID))
        .append(IFSConstants.AMPERSAND);
        urlEncodedAuthnReq.append(IFSConstants.MAJOR_VERSION)
        .append(IFSConstants.EQUAL_TO)
        .append(majorVersion).append(IFSConstants.AMPERSAND);
        urlEncodedAuthnReq.append(IFSConstants.MINOR_VERSION)
        .append(IFSConstants.EQUAL_TO)
        .append(minorVersion).append(IFSConstants.AMPERSAND);
        
        if(issueInstant != null){
            urlEncodedAuthnReq.append(IFSConstants.ISSUE_INSTANT)
            .append(IFSConstants.EQUAL_TO)
            .append(URLEncDec.encode(DateUtils.toUTCDateFormat(issueInstant)))
            .append(IFSConstants.AMPERSAND);
        } else {
            FSUtils.debug.error("FSFederationTerminationNotification."
                    + "toURLEncodedQueryString: issueInstant missing");
            String[] args = { IFSConstants.ISSUE_INSTANT };
            throw new FSMsgException("missingAttribute",args);
        }
        if (providerId != null && providerId.length() != 0) {
            urlEncodedAuthnReq.append(IFSConstants.PROVIDER_ID)
            .append(IFSConstants.EQUAL_TO)
            .append(URLEncDec.encode(providerId))
            .append(IFSConstants.AMPERSAND);
        }
        
        if (nameIdentifier != null) {
            if (nameIdentifier.getName() != null &&
                    nameIdentifier.getName().length() != 0) {
                urlEncodedAuthnReq.append(IFSConstants.NAME)
                .append(IFSConstants.EQUAL_TO)
                .append(URLEncDec.encode(
                        nameIdentifier.getName()))
                        .append(IFSConstants.AMPERSAND)
                        .append(IFSConstants.NAME_IDENTIFIER)
                        .append(IFSConstants.EQUAL_TO)
                        .append(URLEncDec.encode(
                        nameIdentifier.getName()))
                        .append(IFSConstants.AMPERSAND);
            }
            if (nameIdentifier.getNameQualifier() != null &&
                    nameIdentifier.getNameQualifier().length() != 0) {
                urlEncodedAuthnReq.append(IFSConstants.NAME_QUALIFIER)
                .append(IFSConstants.EQUAL_TO)
                .append(URLEncDec.encode(
                        nameIdentifier.getNameQualifier()))
                        .append(IFSConstants.AMPERSAND);
            }
            if (nameIdentifier.getFormat() != null &&
                    nameIdentifier.getFormat().length() != 0) {
                urlEncodedAuthnReq.append(IFSConstants.NAME_FORMAT)
                .append(IFSConstants.EQUAL_TO)
                .append(URLEncDec.encode(
                        nameIdentifier.getFormat()))
                        .append(IFSConstants.AMPERSAND);
            }
        }
        
        if (relayState != null) {
            urlEncodedAuthnReq.append(IFSConstants.RELAY_STATE)
            .append(IFSConstants.EQUAL_TO)
            .append(URLEncDec.encode(relayState))
            .append(IFSConstants.AMPERSAND);
        }
        return urlEncodedAuthnReq.toString();
    }
    
    /**
     * Returns a <code>Base64</code> encoded string representing this
     * object.
     *
     * @return a <code>Base64</code> encoded string representing this
     *         object.
     * @throws FSMsgException if there is an error creating
     *         a <code>Base64</code> encoded string.
     */
    public String toBASE64EncodedString() throws FSMsgException {
        if((providerId == null) || (providerId.length() == 0)){
            FSUtils.debug.error("FSFederationTerminationNotification."
                    + "toURLEncodedQueryString: providerId is null in the "
                    + "request with requestId:" + requestID);
            String[] args = { requestID };
            throw new FSMsgException("nullProviderIdWRequestId",args);
        }
        if ((requestID == null) || (requestID.length() == 0)) {
            requestID = SAMLUtils.generateID();
            if (requestID == null) {
                FSUtils.debug.error("FSFederationTerminationNotification."
                        + "toURLEncodedQueryString: couldn't generate "
                        + "RequestID.");
                throw new FSMsgException("errorGenerateID",null);
            }
        }
        return Base64.encode(this.toXMLString().getBytes());
    }
    
    
    /**
     * Returns <code>FSFederationTerminationNotification</code> object. The
     * object is creating by parsing the <code>HttpServletRequest</code>
     * object.
     *
     * @param request the <code>HttpServletRequest</code> object.
     * @throws FSMsgException if there is an error
     *         creating <code>FSFederationTerminationNotification</code> object.
     */
    public static FSFederationTerminationNotification parseURLEncodedRequest(
            HttpServletRequest request
            ) throws FSMsgException, SAMLException {
        FSFederationTerminationNotification
                retFederationTerminationNotification =
                new FSFederationTerminationNotification();
        try{
            FSUtils.debug.message("checking minor version");
            retFederationTerminationNotification.majorVersion =
                    Integer.parseInt(
                    request.getParameter(IFSConstants.MAJOR_VERSION));
            retFederationTerminationNotification.minorVersion =
                    Integer.parseInt(request.getParameter(
                    IFSConstants.MINOR_VERSION));
        } catch(NumberFormatException ex){
            throw new FSMsgException("invalidNumber",null);
        }
        
        String requestID = request.getParameter(IFSConstants.REQUEST_ID);
        if (request != null) {
            retFederationTerminationNotification.requestID = requestID;
        } else {
            String[] args = { IFSConstants.REQUEST_ID };
            throw new FSMsgException("missingAttribute",args);
        }
        
        String instantString = request.getParameter(IFSConstants.ISSUE_INSTANT);
        if (instantString == null ||
                instantString.length() == 0) {
            String[] args = { IFSConstants.ISSUE_INSTANT };
            throw new FSMsgException("missingAttribute",args);
        }
        try{
            retFederationTerminationNotification.issueInstant =
                    DateUtils.stringToDate(instantString);
        } catch (ParseException e){
            throw new FSMsgException("parseError",null);
        }
        
        String providerID = request.getParameter(IFSConstants.PROVIDER_ID);
        if (providerID != null){
            retFederationTerminationNotification.providerId = providerID;
        } else {
            throw new FSMsgException("missingElement",null);
        }
        
        String nameFormat = request.getParameter(IFSConstants.NAME_FORMAT);
        
        String nameQualifier =
                request.getParameter(IFSConstants.NAME_QUALIFIER);
        
        
        String name = request.getParameter("Name");
        if (name == null) {
            throw new FSMsgException("missingNameIdentifier",null);
        }
        
        String relayState = request.getParameter(IFSConstants.RELAY_STATE);
        if (relayState != null) {
            retFederationTerminationNotification.relayState = relayState;
        }
        
        retFederationTerminationNotification.nameIdentifier =
                new NameIdentifier(name, nameQualifier, nameFormat);
        
        FSUtils.debug.message("Returning Termination Object");
        return retFederationTerminationNotification;
    }
    
    /**
     * Sets the <code>MajorVersion</code> by parsing the version string.
     *
     * @param majorVer a String representing the <code>MajorVersion</code> to
     *        be set.
     * @throws FSMsgException when the version mismatches.
     */
    private void parseMajorVersion(String majorVer) throws FSMsgException {
        try {
            majorVersion = Integer.parseInt(majorVer);
        } catch (NumberFormatException e) {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message(
                        "FSFederationTerminationNotification(Element): "
                        + "invalid MajorVersion", e);
            }
            throw new FSMsgException("wrongInput",null);
        }
        
        if (majorVersion != SAMLConstants.PROTOCOL_MAJOR_VERSION) {
            if (majorVersion > SAMLConstants.PROTOCOL_MAJOR_VERSION) {
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message(
                            "FSFederationTerminationNotification(Element):  "
                            + "MajorVersion of the "
                            + "FederationTerminationNotification is too high.");
                }
                throw new FSMsgException("requestVersionTooHigh",null);
            } else {
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message(
                            "FSFederationTerminationNotification(Element): "
                            + "MajorVersion of the "
                            + "FederationTerminationNotification is too low.");
                }
                throw new FSMsgException("requestVersionTooLow",null);
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
    private void parseMinorVersion(String minorVer) throws FSMsgException {
        try {
            minorVersion = Integer.parseInt(minorVer);
        } catch (NumberFormatException e) {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message(
                        "FSFederationTerminationNotification(Element): "
                        + "invalid MinorVersion", e);
            }
            throw new FSMsgException("wrongInput",null);
        }
         
        if (minorVersion != IFSConstants.FF_12_PROTOCOL_MINOR_VERSION &&
                minorVersion != IFSConstants.FF_11_PROTOCOL_MINOR_VERSION) {
         if (minorVersion > IFSConstants.FF_12_PROTOCOL_MINOR_VERSION) {
                FSUtils.debug.error("FSFedTerminationNot(Element):"
                        + " MinorVersion of the Response is too high.");
                throw new FSMsgException("responseVersionTooHigh",null);
            } else {
                FSUtils.debug.error("FSFedTerminationNot(Element): "
                    + " MinorVersion of the Response is too low:"
                    + minorVersion);
                throw new FSMsgException("responseVersionTooLow",null);
            }
        }
    }
    
    /**
     * Unsupported operation.
     */
    public void signXML() throws SAMLException {
        throw new SAMLException(FSUtils.BUNDLE_NAME,
                                "unsupportedOperation",null);
    }
    
    /**
     * Signs the <code>FSFederationTerminationNotification</code>.
     * object
     *
     * @param certAlias the Certificate Alias
     * @throws SAMLException if
     *         <code>FSFederationTerminationNotification</code>
     *         cannot be signed.
     */
    public void signXML(String certAlias) throws SAMLException {
        FSUtils.debug.message(
                "FSFederationTerminationNotification.signXML: Called");
        if (signed) {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message(
                        "FSFederationTerminationNotification.signXML: "
                        + "the assertion is already signed.");
            }
            throw new SAMLResponderException(FSUtils.BUNDLE_NAME,
                    "alreadySigned",null);
        }
        if (certAlias == null || certAlias.length() == 0) {
            throw new SAMLResponderException(
                    FSUtils.BUNDLE_NAME,"cannotFindCertAlias",null);
        }
        try{
            XMLSignatureManager manager = XMLSignatureManager.getInstance();
            if (minorVersion == IFSConstants.FF_11_PROTOCOL_MINOR_VERSION) {
                signatureString = manager.signXML(this.toXMLString(true, true),
                        certAlias, (String) null, IFSConstants.ID,
                        this.id, false);
            } else
                if (minorVersion == IFSConstants.FF_12_PROTOCOL_MINOR_VERSION) {
                    signatureString =
                        manager.signXML(this.toXMLString(true, true),
                        certAlias, (String) null,
                        IFSConstants.REQUEST_ID,
                        this.getRequestID(), false);
                } else {
                    if (FSUtils.debug.messageEnabled()) {
                        FSUtils.debug.message("invalid minor version.");
                    }
                }
            
            signature =
                    XMLUtils.toDOMDocument(signatureString, FSUtils.debug)
                    .getDocumentElement();
            
            signed = true;
            xmlString = this.toXMLString(true, true);
        } catch(Exception e){
            throw new SAMLResponderException(FSUtils.BUNDLE_NAME,
                    "signFailed",null);
        }
    }
    
    
    /**
     * Sets the <code>Element</code> signature.
     *
     * @param elem the <code>Element</code> object
     * @return true if signature is set otherwise false
     */
    public boolean setSignature(Element elem) {
        signatureString = XMLUtils.print(elem);
        return super.setSignature(elem);
    }
}
