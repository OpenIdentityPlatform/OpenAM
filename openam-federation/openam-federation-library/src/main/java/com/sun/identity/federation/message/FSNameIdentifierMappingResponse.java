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
 * $Id: FSNameIdentifierMappingResponse.java,v 1.2 2008/06/25 05:46:44 qcheng Exp $
 *
 * Portions Copyrighted 2014-2016 ForgeRock AS.
 */

package com.sun.identity.federation.message;

import static org.forgerock.openam.utils.Time.*;

import com.sun.identity.shared.xml.XMLUtils;

import com.sun.identity.shared.Constants;
import com.sun.identity.shared.DateUtils;
import com.sun.identity.common.SystemConfigurationUtil;

import com.sun.identity.federation.common.FSUtils;
import com.sun.identity.federation.common.IFSConstants;
import com.sun.identity.federation.message.common.FSMsgException;

import com.sun.identity.saml.assertion.NameIdentifier;

import com.sun.identity.saml.common.SAMLConstants;
import com.sun.identity.saml.common.SAMLException;
import com.sun.identity.saml.common.SAMLResponderException;

import com.sun.identity.saml.protocol.AbstractResponse;
import com.sun.identity.saml.protocol.Status;

import com.sun.identity.saml.xmlsig.XMLSignatureManager;

import java.util.Date;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * The class <code>FSNameIdentifierMappingResponse</code> is used to
 * create , parse the <code>NameIdentifierMappingResponse</code>.
 *
 * @supported.all.api
 * @deprecated since 12.0.0
 */
@Deprecated
public class FSNameIdentifierMappingResponse extends AbstractResponse {
    
    private String providerID;
    private Status status;
    private NameIdentifier nameIdentifier;
    private int minorVersion = IFSConstants.FF_12_PROTOCOL_MINOR_VERSION;
    private String signatureString;
    
    /**
     * Constructor to create <code>FSNameIdentifierMappingResponse</code> object.
     *
     * @param providerID the value of <code>ProviderID</code> attribute.
     * @param inResponseTo the value of <code>InResponseTo</code> attribute.
     * @param status the <code>Status</code> object.
     * @param nameIdentifier the resulting mapped identifier,
     *        <code>NameIdentifier</code>, for the desired identity federation.
     * @throws FSMsgException on an error.
     */
    public FSNameIdentifierMappingResponse(String providerID,
            String inResponseTo, Status status,
            NameIdentifier nameIdentifier) throws FSMsgException {
        this.providerID = providerID;
        this.inResponseTo = inResponseTo;
        this.status = status;
        this.nameIdentifier = nameIdentifier;
        this.responseID = FSUtils.generateID();
        setIssueInstant(newDate());
    }
    
    /**
     * Constructor to create <code>FSNameIdentifierMappingResponse</code> object.
     * This object is created from the Document Element.
     *
     * @param root the <code>NameIdentifierMappingResponse</code> Document
     *        Element
     * @throws FSMsgException if there is an error creating the object.
     */
    public FSNameIdentifierMappingResponse(Element root)
    throws FSMsgException {
        if (root == null) {
            FSUtils.debug.message(
                    "FSNameIdentifierMappingResponse: null element input");
            throw new FSMsgException("nullInputParameter",null);
        }
        String tag = null;
        if (((tag = root.getLocalName()) == null) ||
                (!tag.equals("NameIdentifierMappingResponse"))) {
            FSUtils.debug.message(
                    "FSNameIdentifierMappingRequest: wrong input");
            throw new FSMsgException("wrongInput",null);
        }
        
        // get IssueInstant
        String instantString = root.getAttribute(IFSConstants.ISSUE_INSTANT);
        if (instantString==null || instantString.length()==0) {
            FSUtils.debug.error("FSNameIdentifierMappingResponse: " +
                    "missing IssueInstant");
            String[] args = { IFSConstants.ISSUE_INSTANT };
            throw new FSMsgException("missingAttribute",args);
        } else {
            try {
                issueInstant = DateUtils.stringToDate(instantString);
            } catch (Exception e) {
                FSUtils.debug.error("FSNameIdentifierMappingResponse: " +
                        "could not parse IssueInstant.", e);
                throw new FSMsgException("wrongInput",null);
            }
        }
        
        // get ResponseID
        responseID = root.getAttribute(IFSConstants.RESPONSE_ID);
        if ((responseID == null) || (responseID.length() < 1)) {
            FSUtils.debug.error("FSNameIdentifierMappingResponse: " +
                    "response doesn't have ResponseID");
            String[] args = { IFSConstants.RESPONSE_ID };
            throw new FSMsgException("missingAttribute",args);
        }
        
        // get InResponseTo
        inResponseTo = root.getAttribute(IFSConstants.IN_RESPONSE_TO);
        if (inResponseTo == null) {
            FSUtils.debug.error("FSNameIdentifierMappingResponse: " +
                    "response doesn't have InResponseTo");
            String[] args = { IFSConstants.IN_RESPONSE_TO };
            throw new FSMsgException("missingAttribute",args);
        }
        
        // get and check versions
        parseMajorVersion(root.getAttribute(IFSConstants.MAJOR_VERSION));
        parseMinorVersion(root.getAttribute(IFSConstants.MINOR_VERSION));
        
        // get ProviderID, Status & NameIdentifier
        NodeList nl = root.getChildNodes();
        Node child;
        String childName;
        int length = nl.getLength();
        for (int i = 0; i < length; i++) {
            child = nl.item(i);
            if ((childName = child.getLocalName()) != null) {
                if (childName.equals(IFSConstants.STATUS)) {
                    try {
                        status = new Status((Element) child);
                    } catch (SAMLException se) {
                        FSUtils.debug.error("FSNameIdentifierMappingResponse:" +
                                " unable to initialize Status", se);
                        throw new FSMsgException("statusCreateError",null,se);
                    }
                } else if (childName.equals(IFSConstants.PROVIDER_ID)) {
                    providerID = XMLUtils.getElementValue((Element) child);
                } else if (childName.equals(IFSConstants.NAME_IDENTIFIER)) {
                    try {
                        nameIdentifier =
                                new NameIdentifier((Element) child);
                    } catch (SAMLException samle) {
                        FSUtils.debug.error("FSNameIdentifierMappingResponse:" +
                                " unable to initialize NameIdentifier", samle);
                        throw new FSMsgException("nameIdentifierCreateError",
                                null,samle);
                    }
                }
            }
        }
        
        // get signature
        List signs = XMLUtils.getElementsByTagNameNS1(
                root,
                SAMLConstants.XMLSIG_NAMESPACE_URI,
                SAMLConstants.XMLSIG_ELEMENT_NAME);
        int signsSize = signs.size();
        if (signsSize == 1) {
            Element elem = (Element)signs.get(0);
            setSignature(elem);
            signed = true;
        } else if (signsSize != 0) {
            FSUtils.debug.error("FSNameIdentifierMappingResponse: " +
                    "included more than one Signature element.");
            throw new FSMsgException("moreElement",null);
        }
    }
    
    /**
     * Creates <code>FSNameIdentifierMappingResponse</code> object.
     * This object is created by parsing the <code>XML</code> string.
     *
     * @param xml the <code>XML</code> string to be parse.
     * @return the <code>FSNameIdentifierMappingResponse</code> object.
     * @throws FSMsgException if there is an error in parsing the
     *            <code>XML</code> string.
     */
    public static FSNameIdentifierMappingResponse parseXML(String xml)
    throws FSMsgException {
        Document doc = XMLUtils.toDOMDocument(xml, FSUtils.debug);
        if (doc == null) {
            FSUtils.debug.error("FSNameIdentifierMappingResponse.parseXML: " +
                    "error while parsing input xml string");
            throw new FSMsgException("parseError",null);
        }
        Element root = doc.getDocumentElement();
        return new FSNameIdentifierMappingResponse(root);
    }
    
    /**
     * Returns the value of <code>ProviderID</code> attribute.
     *
     * @return the value of <code>ProviderID</code> attribute.
     */
    public String getProviderID() {
        return providerID;
    }
    
    /**
     * Returns the <code>Status</code>.
     *
     * @return the <code>Status</code>.
     */
    public Status getStatus() {
        return status;
    }
    
    /**
     * Returns the <code>NameIdentifier</code> object. This is the resulting
     * mapped name identifier for the desired identity federation.
     *
     * @return <code>NameIdentifier</code> object, the resulting mapped name
     *          identifier for the desired identity federation
     */
    public NameIdentifier getNameIdentifier() {
        return nameIdentifier;
    }
    
    /**
     * Sets the <code>MajorVersion</code> by parsing the version string.
     *
     * @param version a String representing the <code>MajorVersion</code> to
     *        be set.
     * @throws FSMsgException on error.
     */
    private void parseMajorVersion(String version) throws FSMsgException {
        try {
            majorVersion = Integer.parseInt(version);
        } catch (NumberFormatException e) {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("FSNameIdentifierMappingResponse." +
                        "parseMajorVersion:invalid MajorVersion:" + version, e);
            }
            throw new FSMsgException("wrongInput",null);
        }
        
        if (majorVersion != SAMLConstants.PROTOCOL_MAJOR_VERSION) {
            if (majorVersion > SAMLConstants.PROTOCOL_MAJOR_VERSION) {
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message("FSNameIdentifierMappingResponse." +
                            "parseMajorVersion: MajorVersion is too high");
                }
                throw new FSMsgException("requestVersionTooHigh",null);
            } else {
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message("FSNameIdentifierMappingResponse." +
                            "parseMajorVersion: MajorVersion is too low");
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
    private void parseMinorVersion(String version) throws FSMsgException {
        try {
            minorVersion = Integer.parseInt(version);
        } catch (NumberFormatException e) {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("FSNameIdentifierMappingResponse." +
                        "parseMinorVersion:invalid MinorVersion:" + version, e);
            }
            throw new FSMsgException("wrongInput",null);
        }
        if (minorVersion > IFSConstants.FF_12_PROTOCOL_MINOR_VERSION) {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("FSNameIdentifierMappingResponse." +
                    "parseMinorVersion: MinorVersion is too high");
            }
            throw new FSMsgException("requestVersionTooHigh",null);
        } else if (minorVersion < IFSConstants.FF_11_PROTOCOL_MINOR_VERSION) {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("FSNameIdentifierMappingResponse." +
                    "parseMinorVersion: MinorVersion is too low");
            }
            throw new FSMsgException("requestVersionTooLow",null);
        }
    }
    
    /**
     * Signs the XML document representing
     * <code>NameIdentifierMappingResponse</code> using the certificate
     * indicated by the property "com.sun.identity.saml.xmlsig.certalias"
     * in AMConfig.properties file.
     *
     * @throws SAMLException if there is an error signing
     *            the <code>XML</code> string.
     */
    public void signXML() throws SAMLException {
        String certAlias = SystemConfigurationUtil.getProperty(
                Constants.SAML_XMLSIG_CERT_ALIAS);
        signXML(certAlias);
    }
    
    /**
     * Signs the <code>XML</code> document representing
     * <code>NameIdentifierMappingResponse</code> using the specified
     * certificate.
     *
     * @param certAlias the alias/name of the certificate used for signing
     *                   the XML document
     * @throws SAMLException if there is an error signing
     *            the <code>XML</code> string or if the message is already
     *            signed.
     */
    public void signXML(String certAlias) throws SAMLException {
        FSUtils.debug.message("FSNameIdentifierMappingResponse.signXML");
        if (signed) {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("FSNameIdentifierMappingResponse.signXML:"
                        + " the response is already signed.");
            }
            throw new SAMLResponderException(FSUtils.BUNDLE_NAME,
                    "alreadySigned",null);
        }
        if (certAlias == null || certAlias.length() < 1) {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("FSNameIdentifierMappingResponse.signXML:"
                        + " null certAlias");
            }
            throw new SAMLResponderException(FSUtils.BUNDLE_NAME,
                    "cannotFindCertAlias",null);
        }
        try {
            XMLSignatureManager manager = XMLSignatureManager.getInstance();
            signatureString = manager.signXML(this.toXMLString(true, true),
                    certAlias, (String)null,
                    IFSConstants.RESPONSE_ID,
                    this.getResponseID(), false);
            signature = XMLUtils.toDOMDocument(signatureString, FSUtils.debug)
            .getDocumentElement();
            signed = true;
        } catch (Exception e){
            FSUtils.debug.error("FSNameIdentifierMappingResponse.signXML: " +
                    "unable to sign", e);
            throw new SAMLResponderException(
                    FSUtils.BUNDLE_NAME,"signFailed",null);
        }
    }
    
    /**
     * Returns the string representation of this object.
     *
     * @return An XML String representing the object.
     * @throws FSMsgException on error.
     */
    public String toXMLString()  throws FSMsgException {
        return toXMLString(true, true);
    }
    
    /**
     * Returns a String representation of this object.
     *
     * @param includeNS : Determines whether or not the namespace qualifier
     *        is prepended to the Element when converted
     * @param declareNS : Determines whether or not the namespace is declared
     *        within the Element.
     * @return a string containing the valid XML for this element
     * @throws FSMsgException if there is an error converting
     *         this object ot a string.
     */
    
    public String toXMLString(boolean includeNS, boolean declareNS)
    throws FSMsgException {
        return toXMLString(includeNS, declareNS, false);
    }
    
    /**
     * Returns a String representation of this object.
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
        
        String prefixLIB = "";
        String uriLIB = "";
        String uriSAML = "";
        if (includeNS) {
            prefixLIB = IFSConstants.LIB_PREFIX;
        }
        if (declareNS) {
            uriLIB = IFSConstants.LIB_12_NAMESPACE_STRING;
            uriSAML = IFSConstants.assertionDeclareStr;
        }
        String instantString = null;
        try {
            instantString = DateUtils.toUTCDateFormat(issueInstant);
        } catch (Exception e) {
            FSUtils.debug.error("FSNameIdentifierMappingResponse.toXMLString:" +
                    " could not convert issueInstant to String.", e);
        }
        // construct xml response
        StringBuffer xml = new StringBuffer(1000);
        if (includeHeader) {
            xml.append(IFSConstants.XML_PREFIX)
            .append(SAMLConstants.DEFAULT_ENCODING)
            .append(IFSConstants.QUOTE)
            .append(IFSConstants.SPACE)
            .append(IFSConstants.QUESTION_MARK)
            .append(IFSConstants.RIGHT_ANGLE)
            .append(IFSConstants.NL);
        }
        xml.append(IFSConstants.LEFT_ANGLE)
        .append(prefixLIB)
        .append(IFSConstants.NAMEID_MAPPING_RESPONSE)
        .append(uriLIB).append(uriSAML)
        .append(IFSConstants.SPACE)
        .append(IFSConstants.RESPONSE_ID)
        .append(IFSConstants.EQUAL_TO)
        .append(IFSConstants.QUOTE)
        .append(responseID)
        .append(IFSConstants.QUOTE)
        .append(IFSConstants.SPACE)
        .append(IFSConstants.IN_RESPONSE_TO)
        .append(IFSConstants.EQUAL_TO)
        .append(IFSConstants.QUOTE)
        .append(inResponseTo)
        .append(IFSConstants.QUOTE)
        .append(IFSConstants.SPACE)
        .append(IFSConstants.SPACE)
        .append(IFSConstants.MAJOR_VERSION)
        .append(IFSConstants.EQUAL_TO)
        .append(IFSConstants.QUOTE)
        .append(majorVersion)
        .append(IFSConstants.EQUAL_TO)
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
        .append(instantString)
        .append(IFSConstants.QUOTE)
        .append(IFSConstants.SPACE)
        .append(IFSConstants.RIGHT_ANGLE);
        if (signed) {
            if (signatureString != null) {
                xml.append(signatureString);
            } else if (signature != null) {
                signatureString = XMLUtils.print(signature);
                xml.append(signatureString);
            }
        }
        xml.append(IFSConstants.LEFT_ANGLE)
        .append(prefixLIB)
        .append(IFSConstants.PROVIDER_ID)
        .append(IFSConstants.RIGHT_ANGLE)
        .append(providerID)
        .append(IFSConstants.START_END_ELEMENT)
        .append(prefixLIB)
        .append(IFSConstants.PROVIDER_ID)
        .append(IFSConstants.RIGHT_ANGLE)
        .append(status.toString(includeNS, true));
        
        if (nameIdentifier != null) {
            xml.append(nameIdentifier.toString());
        }
        xml.append(IFSConstants.START_END_ELEMENT)
        .append(prefixLIB)
        .append(IFSConstants.NAMEID_MAPPING_RESPONSE)
        .append(IFSConstants.RIGHT_ANGLE);
        
        return xml.toString();
    }
}
