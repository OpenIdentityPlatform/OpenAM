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
 * $Id: FSLogoutNotification.java,v 1.4 2008/06/25 05:46:44 qcheng Exp $
 *
 * Portions Copyrighted 2014-2016 ForgeRock AS.
 */

package com.sun.identity.federation.message;

import static org.forgerock.openam.utils.Time.*;

import com.sun.identity.shared.xml.XMLUtils;
import com.sun.identity.shared.encode.Base64;
import com.sun.identity.shared.encode.URLEncDec;
import com.sun.identity.shared.DateUtils;
import com.sun.identity.saml.common.SAMLConstants;
import com.sun.identity.saml.common.SAMLUtils;
import com.sun.identity.saml.common.SAMLException;
import com.sun.identity.saml.common.SAMLResponderException;
import com.sun.identity.saml.protocol.AbstractRequest;
import com.sun.identity.saml.assertion.NameIdentifier;
import com.sun.identity.saml.xmlsig.XMLSignatureManager;

import com.sun.identity.federation.message.common.FSMsgException;
import com.sun.identity.federation.common.FSUtils;
import com.sun.identity.federation.common.IFSConstants;

import javax.servlet.http.HttpServletRequest;

import java.text.ParseException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Document;

/**
 * This class contains methods to construct a <code>LogoutRequest</code>
 * object.
 *
 * @supported.all.api
 * @deprecated since 12.0.0
 */
@Deprecated

public class FSLogoutNotification extends AbstractRequest {
    private String providerId;
    private NameIdentifier nameIdentifier;
    protected String sessionIndex;
    protected String xmlString = null;
    protected String signatureString = null;
    protected String id = null;
    private String relayState = null;
    protected Date notOnOrAfter = null;
    
    /**
     * Default Constructor.
     */
    public FSLogoutNotification() {
        setIssueInstant(newDate());
    }
    
    /**
     * Constructor creates <code>FSLogoutNotification</code> object
     * from Document Element.
     *
     * @param root the Document Element object.
     * @throws FSMsgException if there is an error creating this
     *         object.
     */
    public FSLogoutNotification(Element root)  throws FSMsgException {
        String tag = null;
        if (root == null) {
            FSUtils.debug.message("FSLogoutNotification(Element): null input.");
            throw new FSMsgException("nullInput",null);
        }
        if (((tag = root.getLocalName()) == null) ||
                (!tag.equals(IFSConstants.LOGOUT_REQUEST))) {
            FSUtils.debug.message("FSLogoutNotification(Element): wrong input");
            throw new FSMsgException("wrongInput",null);
        }
        
        // get the IssueInstant Attribute
        String instantString = root.getAttribute(IFSConstants.ISSUE_INSTANT);
        if ((instantString == null) || (instantString.length() == 0)) {
            FSUtils.debug.message(
                    "LogoutRequest(Element): missing IssueInstant");
            String[] args = { IFSConstants.ISSUE_INSTANT };
            throw new FSMsgException("missingAttribute",args);
        } else {
            try {
                issueInstant = DateUtils.stringToDate(instantString);
            } catch (Exception e) {
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message("LogoutRequest(Element): could not "
                            + "parse IssueInstant:" + e.getMessage());
                }
                throw new FSMsgException("wrongInput",null);
            }
        }
        // get the NotOnOrAfter Attribute
        String notAfter = root.getAttribute(IFSConstants.NOT_ON_OR_AFTER);
        if (notAfter != null && notAfter.length() != 0) {
            try {
                notOnOrAfter = DateUtils.stringToDate(notAfter);
            } catch (Exception ex) {
                if(FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message("LogoutRequest(Element): unable to" +
                            "parse not on or after", ex);
                }
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
                    respondWiths.add(
                            XMLUtils.getElementValue((Element) child));
                } else if (nodeName.equals(IFSConstants.SIGNATURE)) {
                } else if (nodeName.equals(IFSConstants.PROVIDER_ID)) {
                    if (providerId != null) {
                        if (FSUtils.debug.messageEnabled()) {
                            FSUtils.debug.message("FSLogoutNotification "
                                    + "(Element): should contain only " 
                                    + "one ProviderID.");
                        }
                        throw new FSMsgException("wrongInput",null);
                    }
                    providerId = XMLUtils.getElementValue((Element) child);
                } else if (nodeName.equals(IFSConstants.RELAY_STATE)) {
                    if (relayState != null) {
                        if (FSUtils.debug.messageEnabled()) {
                            FSUtils.debug.message("FSLogoutNotification "
                                    + "(Element): should contain only one "
                                    + "relayState.");
                        }
                        throw new FSMsgException("wrongInput",null);
                    }
                    relayState = XMLUtils.getElementValue((Element) child);
                } else if (nodeName.equals(IFSConstants.NAME_IDENTIFIER)) {
                    try {
                        this.nameIdentifier =
                                new NameIdentifier((Element) child);
                    } catch(SAMLException ex){
                        if (FSUtils.debug.messageEnabled()) {
                            FSUtils.debug.message("FSLogoutNotification "
                                    + "(Element): SAMLException while "
                                    + "nconstructing ameidentifier");
                        }
                        throw new FSMsgException("nameIdentifierCreateError",
                                null,ex);
                    }
                } else if (nodeName.equals(IFSConstants.SESSION_INDEX)) {
                    if (sessionIndex != null) {
                        if (FSUtils.debug.messageEnabled()) {
                            FSUtils.debug.message(
                                    "FSLogoutNotification(Element): "
                                    + "should contain only one SessionIndex.");
                        }
                        throw new FSMsgException("wrongInput",null);
                    }
                    sessionIndex = XMLUtils.getElementValue((Element) child);
                } else {
                    if (FSUtils.debug.messageEnabled()) {
                        FSUtils.debug.message("FSLogoutNotification(Element): "
                                + "invalid node" + nodeName);
                    }
                    throw new FSMsgException("wrongInput",null);
                }
            }
        }
        
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
                        "FSLogoutNotification(Element): included more than"
                        + " one Signature element.");
            }
            throw new FSMsgException("moreElement",null);
        }
        //end check for signature
    }
    
    /**
     * Consturctor creates <code>FSLogoutNotification</code> object.
     *
     * @param requestId the <code>RequestId</code> attribute.
     * @param providerID the <code>ProviderID</code> attribute.
     * @param nameId the <code>NameIdentifier</code> object.
     * @param relayState the <code>RelayState</code> attribute.
     * @throws FSMsgException if there is an error creating
     *         this object.
     */
    public FSLogoutNotification(String requestId,String providerID,
            NameIdentifier nameId, String relayState)
            throws FSMsgException {
        setIssueInstant(newDate());
        if ((requestId != null) && (requestId.length() != 0)) {
            requestID = requestId;
        } else {
            requestID = SAMLUtils.generateID();
            if (requestID == null) {
                FSUtils.debug.error(
                        "FSLogoutNotification: couldn't generate RequestID.");
                throw new FSMsgException("errorGenerateID",null);
            }
        }
        this.relayState = relayState;
        this.providerId = providerID;
        this.nameIdentifier = nameId;
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
     * Sets the value of <code>RelayState</code> attribute.
     *
     * @param relayState the value of <code>RelayState</code> attribute.
     */
    public void setRelayState(String relayState) {
        this.relayState = relayState;
    }
    
    /**
     * Returns the value of <code>RelayState</code> attribute.
     *
     * @return the value of <code>RelayState</code> attribute.
     */
    public String getRelayState() {
        return this.relayState;
    }
    
    /**
     * Returns a signed <code>XML</code> string.
     *
     * @return a signed <code>XML</code> string.
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
     * @param version the value of <code>MinorVersion</code> attribute.
     * @see #getMinorVersion()
     */
    public void setMinorVersion(int version) {
        minorVersion = version;
    }
    
    /**
     * Returns the string representation of this object.
     *
     * @param includeNS  determines whether or not the namespace qualifier
     *        is prepended to the Element when converted
     * @param declareNS : Determines whether or not the namespace is declared
     *        within the Element.
     * @return a string containing the valid <code>XML</code> for this element
     * @throws FSMsgException if there is an error creating
     *         <code>XML</code> string from this object.
     */
    
    public String toXMLString(boolean includeNS, boolean declareNS)
    throws FSMsgException {
        return toXMLString(includeNS, declareNS, false);
    }
    
    /**
     * Returns the string representation of this object.
     *
     * @param includeNS determines whether or not the namespace qualifier
     *        is prepended to the Element when converted
     * @param declareNS Determines whether or not the namespace is declared
     *        within the Element.
     * @param includeHeader Determines whether the output include the xml
     *        declaration header.
     * @return a string containing the valid <code>XML</code> for this element
     * @throws FSMsgException if there is an error creating
     *         <code>XML</code> string from this object.
     */
    public String toXMLString(boolean includeNS,boolean declareNS,
            boolean includeHeader) throws FSMsgException {
        if((providerId == null) || (providerId.length() == 0)){
            FSUtils.debug.error("FSLogoutNotification.toXMLString: "
                    + "providerId is null in the request with requestId:"
                    + requestID);
            String[] args = { requestID };
            throw new FSMsgException("nullProviderIdWRequestId" , args);
        }
        if ((requestID == null) || (requestID.length() == 0)){
            requestID = SAMLUtils.generateID();
            if (requestID == null) {
                FSUtils.debug.error("FSLogoutNotification.toXMLString: "
                        + "couldn't generate RequestID.");
                throw new FSMsgException("errorGenerateID",null);
            }
        }
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
        String uriSAML = "";
        if (includeNS) {
            prefix = IFSConstants.LIB_PREFIX;
        }
        if (declareNS) {
            if(minorVersion == IFSConstants.FF_12_PROTOCOL_MINOR_VERSION) {
                uri = IFSConstants.LIB_12_NAMESPACE_STRING;
            } else {
                uri = IFSConstants.LIB_NAMESPACE_STRING;
            }
            uriSAML = IFSConstants.assertionDeclareStr;
        }
        
        String instantString = DateUtils.toUTCDateFormat(issueInstant);
        if (notOnOrAfter == null) {
            notOnOrAfter = new Date(issueInstant.getTime() +
                    IFSConstants.ASSERTION_TIMEOUT_ALLOWED_DIFFERENCE);
        }
        String notAfter = DateUtils.toUTCDateFormat(notOnOrAfter);
        
        if (requestID != null){
            xml.append(IFSConstants.LEFT_ANGLE)
            .append(prefix)
            .append(IFSConstants.LOGOUT_REQUEST)
            .append(uri)
            .append(uriSAML);
            
            if (minorVersion == IFSConstants.FF_11_PROTOCOL_MINOR_VERSION &&
                    id != null && !(id.length() == 0)) {
                xml.append(IFSConstants.SPACE)
                .append(IFSConstants.ID)
                .append(IFSConstants.EQUAL_TO)
                .append(IFSConstants.QUOTE)
                .append(id)
                .append(IFSConstants.QUOTE)
                .append(IFSConstants.SPACE);
            }
            xml.append(IFSConstants.SPACE)
            .append(IFSConstants.REQUEST_ID)
            .append(IFSConstants.EQUAL_TO)
            .append(IFSConstants.QUOTE)
            .append(requestID)
            .append(IFSConstants.QUOTE)
            .append(IFSConstants.SPACE)
            .append(IFSConstants.SPACE)
            .append(IFSConstants.MAJOR_VERSION)
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
            .append(IFSConstants.QUOTE);
            
            if (minorVersion == IFSConstants.FF_12_PROTOCOL_MINOR_VERSION) {
                xml.append(IFSConstants.SPACE)
                .append(IFSConstants.NOT_ON_OR_AFTER)
                .append(IFSConstants.EQUAL_TO)
                .append(IFSConstants.QUOTE)
                .append(notAfter)
                .append(IFSConstants.QUOTE);
            }
            xml.append(IFSConstants.RIGHT_ANGLE);
            
            if((respondWiths != null) &&
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
                    .append(IFSConstants.RIGHT_ANGLE);
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
            
            if ((sessionIndex != null) && sessionIndex.length() != 0){
                xml.append("<").append(prefix).append("SessionIndex").
                        append(uri).
                        append(">").append(sessionIndex).append("</").
                        append(prefix).append("SessionIndex").append(">");
            }
            
            if (relayState != null && relayState.length() != 0) {
                xml.append(IFSConstants.LEFT_ANGLE)
                .append(prefix)
                .append(IFSConstants.RELAY_STATE)
                .append(uri)
                .append(IFSConstants.RIGHT_ANGLE)
                .append(relayState)
                .append(IFSConstants.START_END_ELEMENT)
                .append(prefix)
                .append(IFSConstants.RELAY_STATE)
                .append(IFSConstants.RIGHT_ANGLE);
            }
            xml.append(IFSConstants.START_END_ELEMENT)
               .append(prefix)
               .append(IFSConstants.LOGOUT_REQUEST)
               .append(IFSConstants.RIGHT_ANGLE);
        } else {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("FSLogoutNotification.toString: "
                        + "requestID is null ");
            }
            throw new FSMsgException("nullRequestID",null);
        }
        return xml.toString();
    }
    
    /**
     * Returns the string representation of this object.
     *
     * @return a string containing the valid <code>XML</code> for this element
     * @throws FSMsgException if there is an error creating
     *         <code>XML</code> string from this object.
     */
    public String toXMLString() throws FSMsgException {
        return toXMLString(true, true);
    }
    
    /**
     * Constructor create <code>FSLogoutNotification</code> from a
     * <code>XML</code> string.
     *
     * @param xml the <code>XML</code> string.
     * @throws FSMsgException if there is an error creating
     *         this object.
     */
    public static FSLogoutNotification parseXML(String xml)
    throws FSMsgException {
        Document doc = XMLUtils.toDOMDocument(xml, FSUtils.debug);
        if (doc == null) {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("FSLogoutNotification.parseXML:Error "
                        + "while parsing input xml string");
            }
            throw new FSMsgException("parseError",null);
        }
        Element root = doc.getDocumentElement();
        return new FSLogoutNotification(root);
    }
    
    /**
     * Returns value of <code>ProviderID</code> attribute.
     *
     * @return value of <code>ProviderID</code> attribute.
     * @see #setProviderId(String)
     */
    public String getProviderId() {
        return providerId;
    }
    
    /**
     * Sets value of <code>ProviderID</code> attribute.
     *
     * @param providerID value of <code>ProviderID</code> attribute.
     * @see #getProviderId()
     */
    public void setProviderId(String providerID) {
        this.providerId = providerID;
    }
    
    /**
     * Returns value of <code>SessionIndex</code> attribute.
     *
     * @return value of <code>SessionIndex</code> attribute.
     * @see #setSessionIndex(String)
     */
    public String getSessionIndex() {
        return sessionIndex;
    }
    
    /**
     * Sets value of <code>SessionIndex</code> attribute.
     *
     * @param sessionIndex value of <code>SessionIndex</code> attribute.
     * @see #getSessionIndex
     */
    public void setSessionIndex(String sessionIndex) {
        this.sessionIndex = sessionIndex;
    }
    
    /**
     * Returns the <code>NameIdentifier</code> object.
     *
     * @return the <code>NameIdentifier</code> object.
     * @see #setNameIdentifier(NameIdentifier)
     */
    public NameIdentifier getNameIdentifier() {
        return nameIdentifier;
    }
    
    /**
     * Sets the <code>NameIdentifier</code> object.
     *
     * @param nameId the <code>NameIdentifier</code> object.
     * @see #getNameIdentifier
     */
    public void setNameIdentifier(NameIdentifier nameId) {
        this.nameIdentifier = nameId;
    }
    
    /**
     * Returns an URL Encoded String.
     *
     * @return a url encoded query string.
     * @throws FSMsgException if there is an error.
     */
    public String toURLEncodedQueryString() throws FSMsgException {
        if((providerId == null) || (providerId.length() == 0)){
            FSUtils.debug.error("FSLogoutNotification.toURLEncodedQueryString: "
                    + "providerId is null in the request with requestId:"
                    + requestID);
            String[] args = { requestID };
            throw new FSMsgException("nullProviderIdWRequestId",args);
        }
        if ((requestID == null) || (requestID.length() == 0)){
            requestID = SAMLUtils.generateID();
            if (requestID == null) {
                FSUtils.debug.error(
                        "FSLogoutNotification.toURLEncodedQueryString: "
                        + "couldn't generate RequestID.");
                throw new FSMsgException("errorGenerateID",null);
            }
        }
        StringBuffer urlEncodedAuthnReq = new StringBuffer(300);
        urlEncodedAuthnReq.append(IFSConstants.REQUEST_ID)
        .append(IFSConstants.EQUAL_TO)
        .append(URLEncDec.encode(requestID))
        .append(IFSConstants.AMPERSAND)
        .append(IFSConstants.MAJOR_VERSION)
        .append(IFSConstants.EQUAL_TO)
        .append(majorVersion)
        .append(IFSConstants.AMPERSAND)
        .append(IFSConstants.MINOR_VERSION)
        .append(IFSConstants.EQUAL_TO)
        .append(minorVersion)
        .append(IFSConstants.AMPERSAND);
        
        if(issueInstant != null){
            urlEncodedAuthnReq.append(IFSConstants.ISSUE_INSTANT)
            .append(IFSConstants.EQUAL_TO)
            .append(URLEncDec.encode(
                    DateUtils.toUTCDateFormat(issueInstant)))
                    .append(IFSConstants.AMPERSAND);
            
            if(minorVersion == IFSConstants.FF_12_PROTOCOL_MINOR_VERSION) {
                notOnOrAfter = new Date(issueInstant.getTime() +
                        IFSConstants.ASSERTION_TIMEOUT_ALLOWED_DIFFERENCE);
                urlEncodedAuthnReq.append(IFSConstants.NOT_ON_OR_AFTER)
                .append(IFSConstants.EQUAL_TO)
                .append(URLEncDec.encode(
                        DateUtils.toUTCDateFormat(notOnOrAfter)))
                        .append(IFSConstants.AMPERSAND);
            }
        } else {
            FSUtils.debug.error("FSLogoutNotification."
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
        
        if (sessionIndex != null && sessionIndex.length() != 0) {
            urlEncodedAuthnReq.append(IFSConstants.SESSION_INDEX)
            .append(IFSConstants.EQUAL_TO)
            .append(URLEncDec.encode(sessionIndex))
            .append(IFSConstants.AMPERSAND);
        }
        
        if (relayState != null && relayState.length() != 0) {
            urlEncodedAuthnReq.append(IFSConstants.RELAY_STATE)
            .append(IFSConstants.EQUAL_TO)
            .append(URLEncDec.encode(relayState))
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
        return urlEncodedAuthnReq.toString();
    }
    
    /**
     * Returns a Base64 Encoded String.
     *
     * @return a Base64 Encoded String.
     * @throws FSMsgException if there is an error encoding
     *         the string.
     */
    public String toBASE64EncodedString() throws FSMsgException {
        if((providerId == null) || (providerId.length() == 0)){
            FSUtils.debug.error("FSLogoutNotification.toURLEncodedQueryString: "
                    + "providerId is null in the request with requestId:"
                    + requestID);
            String[] args = { requestID };
            throw new FSMsgException("nullProviderIdWRequestId",args);
        }
        if ((requestID == null) || (requestID.length() == 0)){
            requestID = SAMLUtils.generateID();
            if (requestID == null) {
                FSUtils.debug.error(
                        "FSLogoutNotification.toURLEncodedQueryString: "
                        + "couldn't generate RequestID.");
                throw new FSMsgException("errorGenerateID",null);
            }
        }
        return Base64.encode(this.toXMLString().getBytes());
    }
    
    /**
     * Returns <code>FSLogoutNotification</code> object. The
     * object is created by parsing the <code>HttpServletRequest</code>
     * object.
     *
     * @param request the <code>HttpServletRequest</code> object.
     * @return <code>FSLogoutNotification</code> object.
     * @throws FSMsgException if there is an error
     *         creating <code>FSAuthnRequest</code> object.
     */
    
    public static FSLogoutNotification parseURLEncodedRequest(
            HttpServletRequest request) throws FSMsgException {
        try {
            FSLogoutNotification retLogoutNotification =
                    new FSLogoutNotification();
            String requestID = request.getParameter("RequestID");
            if(requestID != null) {
                retLogoutNotification.requestID = requestID;
            } else {
                String[] args = { IFSConstants.REQUEST_ID };
                throw new FSMsgException("missingAttribute",args);
            }
            try{
                retLogoutNotification.majorVersion =
                        Integer.parseInt(request.getParameter(
                        IFSConstants.MAJOR_VERSION));
                FSUtils.debug.message("Majorversion : "
                        + retLogoutNotification.majorVersion);
                retLogoutNotification.minorVersion =
                        Integer.parseInt(request.getParameter(
                        IFSConstants.MINOR_VERSION));
                FSUtils.debug.message("Minorversion : "
                        + retLogoutNotification.minorVersion);
            } catch (NumberFormatException ex) {
                FSUtils.debug.message("FSLogoutNotification. "
                        + "parseURLEncodedRequest:Major/Minor version problem");
                throw new FSMsgException("invalidNumber",null);
            }
            String instantString =
                    request.getParameter(IFSConstants.ISSUE_INSTANT);
            if (instantString == null || instantString.length() == 0) {
                String[] args = { IFSConstants.ISSUE_INSTANT };
                throw new FSMsgException("missingAttribute",args);
            }
            try{
                retLogoutNotification.issueInstant =
                        DateUtils.stringToDate(instantString);
            } catch (ParseException e){
                throw new FSMsgException("parseError",null);
            }
            String notAfter =
                    request.getParameter(IFSConstants.NOT_ON_OR_AFTER);
            if (notAfter != null && notAfter.length() != 0) {
                try {
                    retLogoutNotification.notOnOrAfter =
                            DateUtils.stringToDate(notAfter);
                } catch (ParseException pe) {
                    FSUtils.debug.message("FSLogoutNotification.parseURLEncoded"
                            +  "Request: parsing exception", pe);
                }
            }
            
            String providerId = request.getParameter(IFSConstants.PROVIDER_ID);
            if (providerId != null) {
                retLogoutNotification.providerId = providerId;
            } else {
                throw new FSMsgException("missingElement",null);
            }
            
            String sessionIndex =
                    request.getParameter(IFSConstants.SESSION_INDEX);
            if (sessionIndex != null) {
                retLogoutNotification.sessionIndex = sessionIndex;
            }
            
            String relayState = request.getParameter(IFSConstants.RELAY_STATE);
            if (relayState != null) {
                retLogoutNotification.relayState = relayState;
            }
            
            String nameFormat = request.getParameter(IFSConstants.NAME_FORMAT);
            String nameQualifier =
                    request.getParameter(IFSConstants.NAME_QUALIFIER);
            String name = request.getParameter(IFSConstants.NAME);
            
            if (name == null) {
                name = request.getParameter(IFSConstants.NAME_IDENTIFIER);
            }
            
            if (name == null) {
                throw new FSMsgException("missingElement",null);
            }
            
            retLogoutNotification.nameIdentifier =
                    new NameIdentifier(name, nameQualifier, nameFormat);
            
            FSUtils.debug.message("Returning Logout Object");
            return retLogoutNotification;
        } catch(Exception e) {
            throw new FSMsgException("parseError",null);
        }
    }
    
    /**
     * Sets the <code>MajorVersion</code> by parsing the version string.
     *
     * @param majorVer a String representing the <code>MajorVersion</code> to
     *        be set.
     * @throws FSMsgException when the version mismatchs.
     */
    private void parseMajorVersion(String majorVer) throws FSMsgException {
        try {
            majorVersion = Integer.parseInt(majorVer);
        } catch (NumberFormatException e) {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("FSLogoutNotification(Element): invalid "
                        + "MajorVersion", e);
            }
            throw new FSMsgException("wrongInput",null);
        }
        
        if (majorVersion != SAMLConstants.PROTOCOL_MAJOR_VERSION) {
            if (majorVersion > SAMLConstants.PROTOCOL_MAJOR_VERSION) {
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message("FSLogoutNotification(Element): "
                            + "MajorVersion of the LogoutRequest is too high.");
                }
                throw new FSMsgException("requestVersionTooHigh",null);
            } else {
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message("FSLogoutNotification(Element): "
                            + "MajorVersion of the LogoutRequest is too low.");
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
     * @throws FSMsgException when the version mismatchs.
     */
    
    private void parseMinorVersion(String minorVer) throws FSMsgException {
        try {
            minorVersion = Integer.parseInt(minorVer);
        } catch (NumberFormatException e) {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("FSLogoutNotification(Element): invalid "
                        + "MinorVersion", e);
            }
            throw new FSMsgException("wrongInput",null);
        }
        
        if (minorVersion > IFSConstants.FF_12_PROTOCOL_MINOR_VERSION) {
            FSUtils.debug.error("FSLogoutNotification(Element): "
                    + "MinorVersion of the LogoutRequest is too high.");
            throw new FSMsgException("requestVersionTooHigh",null);
	} else if (minorVersion < IFSConstants.FF_11_PROTOCOL_MINOR_VERSION) {
            FSUtils.debug.error("FSLogoutNotification(Element): "
                    + "MinorVersion of the LogoutRequest is too low.");
            throw new FSMsgException("requestVersionTooLow",null);
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
     * Signs the <code>FSLogoutNotification</code> object.
     *
     * @param certAlias the Certificate Alias
     * @throws SAMLException if
     *         <code>FSFederationTerminationNotification</code>
     *         cannot be signed.
     */
    public void signXML(String certAlias) throws SAMLException {
        FSUtils.debug.message("FSLogoutNotification.signXML: Called");
        if (signed) {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("FSLogoutNotification.signXML: "
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
                        certAlias,null,IFSConstants.ID,
                        this.id, false);
            } else if (minorVersion ==
                    IFSConstants.FF_12_PROTOCOL_MINOR_VERSION) {
                signatureString = manager.signXML(
                        this.toXMLString(true, true),
                        certAlias, null,
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
