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
 * $Id: FSNameIdentifierMappingRequest.java,v 1.2 2008/06/25 05:46:44 qcheng Exp $
 * Portions Copyrighted 2014 ForgeRock AS
 */

package com.sun.identity.federation.message;

import com.sun.identity.shared.xml.XMLUtils;

import com.sun.identity.shared.Constants;
import com.sun.identity.shared.DateUtils;
import com.sun.identity.common.SystemConfigurationUtil;

import com.sun.identity.federation.common.FSUtils;
import com.sun.identity.federation.common.IFSConstants;
import com.sun.identity.federation.message.common.FSMsgException;

import com.sun.identity.saml.assertion.NameIdentifier;
import com.sun.identity.saml.common.SAMLConstants;
import com.sun.identity.saml.common.SAMLUtils;
import com.sun.identity.saml.common.SAMLException;
import com.sun.identity.saml.common.SAMLResponderException;

import com.sun.identity.saml.protocol.AbstractRequest;

import com.sun.identity.saml.xmlsig.XMLSignatureManager;

import java.util.Date;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * The class <code>FSNameIdentifierMappingRequest</code> is used to
 * create or parse <code>NameIdentifierMappingRequest<code>.
 *
 * @supported.all.api
 * @deprecated since 12.0.0
 */
@Deprecated
public class FSNameIdentifierMappingRequest extends AbstractRequest {
    
    private String providerID;
    private NameIdentifier nameIdentifier;
    private String targetNamespace;
    private int minorVersion = IFSConstants.FF_12_PROTOCOL_MINOR_VERSION;
    private String signatureString;
    
    /**
     * Constructor to create <code> FSNameIdentifierMappingRequest<code>.
     *
     * @param providerID the requesting provider's ID
     * @param nameIdentifier the <code>NameIdentifier</code> qualified by the
     *        requesting service provider
     * @param targetNamespace the provider ID of the other service provider
     *                        which the requesting service provider would
     *                        subsequently communicate with
     * @throws FSMsgException if there is an error creating the object.
     */
    public FSNameIdentifierMappingRequest(String providerID,
            NameIdentifier nameIdentifier, String targetNamespace)
            throws FSMsgException {
        this.providerID = providerID;
        this.nameIdentifier = nameIdentifier;
        this.targetNamespace = targetNamespace;
        this.requestID = SAMLUtils.generateID();
        setIssueInstant(new Date());
    }
    
    /**
     * Constructor to create <code> FSNameIdentifierMappingRequest<code> from
     * the Document Element.
     *
     * @param root the <code>NameIdentifierMappingRequest</code> Document
     *        element.
     * @throws FSMsgException if there is an error.
     */
    public FSNameIdentifierMappingRequest(Element root) throws FSMsgException {
        if (root == null) {
            FSUtils.debug.message(
                    "FSNameIdentifierMappingRequest: null element input.");
            throw new FSMsgException("nullInputParameter",null);
        }
        String tag = null;
        if (((tag = root.getLocalName()) == null) ||
                (!tag.equals(IFSConstants.NAMEID_MAPPING_REQUEST))) {
            FSUtils.debug.message(
                    "FSNameIdentifierMappingRequest: wrong input");
            throw new FSMsgException("wrongInput",null);
        }
        
        // get IssueInstant
        String instantString = root.getAttribute(IFSConstants.ISSUE_INSTANT);
        if (instantString==null || instantString.length()==0) {
            FSUtils.debug.error("FSNameIdentifierMappingRequest: " +
                    "missing IssueInstant");
            String[] args = { IFSConstants.ISSUE_INSTANT };
            throw new FSMsgException("missingAttribute",args);
        } else {
            try {
                issueInstant = DateUtils.stringToDate(instantString);
            } catch (Exception e) {
                FSUtils.debug.error("FSNameIdentifierMappingRequest: " +
                        "could not parse IssueInstant.", e);
                throw new FSMsgException("wrongInput",null);
            }
        }
        
        // get RequestID
        requestID = root.getAttribute(IFSConstants.REQUEST_ID);
        
        // get and check versions
        parseMajorVersion(root.getAttribute(IFSConstants.MAJOR_VERSION));
        parseMinorVersion(root.getAttribute(IFSConstants.MINOR_VERSION));
        
        // get ProviderID, NameIdentifier & TargetNamespace
        NodeList contentnl = root.getChildNodes();
        Node child;
        String nodeName;
        int length = contentnl.getLength();
        for (int i = 0; i < length; i++) {
            child = contentnl.item(i);
            if ((nodeName = child.getLocalName()) != null) {
                if (nodeName.equals(IFSConstants.PROVIDER_ID)) {
                    providerID = XMLUtils.getElementValue((Element) child);
                } else if (nodeName.equals(IFSConstants.NAME_IDENTIFIER)) {
                    try {
                        nameIdentifier =
                                new NameIdentifier((Element) child);
                    } catch (SAMLException samle) {
                        FSUtils.debug.error("FSNameIdentifierMappingRequest: " +
                                "unable to initialize NameIdentifier", samle);
                        throw new FSMsgException(
                                "nameIdentifierCreateError",null,samle);
                    }
                } else if (nodeName.equals(IFSConstants.TARGET_NAME_SPACE)) {
                    targetNamespace = XMLUtils.getElementValue((Element) child);
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
            FSUtils.debug.error("FSNameIdentifierMappingRequest: " +
                    "included more than one Signature element.");
            throw new FSMsgException("moreElement",null);
        }
    }
    
    /**
     * Returns <code>FSNameIdentifierMappingRequest</code> object. This
     * object is created by parsing the <code>XML</code> string.
     *
     * @param xml <code>XML</code> String
     * @return the <code>FSNameIdentifierMappingRequest</code> object.
     * @throws FSMsgException if there is an error creating this object.
     */
    public static FSNameIdentifierMappingRequest parseXML(String xml)
    throws FSMsgException {
        Document doc = XMLUtils.toDOMDocument(xml, FSUtils.debug);
        if (doc == null) {
            FSUtils.debug.error("FSNameIdentifierMappingRequest.parseXML: " +
                    "error while parsing input xml string");
            throw new FSMsgException("parseError",null);
        }
        Element root = doc.getDocumentElement();
        return new FSNameIdentifierMappingRequest(root);
    }
    
    /**
     * Returns the <code>ProviderID</code> attribute. This
     * is the requesting Service Providers's identifier.
     *
     * @return the <code>ProviderID</code> attribute.
     */
    public String getProviderID() {
        return providerID;
    }
    
    /**
     * Returns the <code>NameIdentifier</code> object  qualified by the
     * requesting service provider .
     *
     * @return the <code>NameIdentifier</code> object  qualified by the
     * requesting service provider .
     */
    public NameIdentifier getNameIdentifier() {
        return nameIdentifier;
    }
    
    /**
     *  Returns the value of <code>TargetNamespace</code> attribute.
     *
     * @return the value of <code>TargetNamespace</code> attribute.
     */
    public String getTargetNamespace() {
        return targetNamespace;
    }
    
    /**
     * Sets the <code>MajorVersion</code> by parsing the version string.
     *
     * @param majorVer a String representing the <code>MajorVersion</code> to
     *        be set.
     * @throws FSMsgException when the version mismatches.
     */
    private void parseMajorVersion(String version) throws FSMsgException {
        try {
            majorVersion = Integer.parseInt(version);
        } catch (NumberFormatException e) {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("FSNameIdentifierMappingRequest." +
                        "parseMajorVersion: invalid MajorVersion: " + version, e);
            }
            throw new FSMsgException("wrongInput",null);
        }
        
        if (majorVersion != SAMLConstants.PROTOCOL_MAJOR_VERSION) {
            if (majorVersion > SAMLConstants.PROTOCOL_MAJOR_VERSION) {
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message("FSNameIdentifierMappingRequest." +
                            "parseMajorVersion: MajorVersion is too high");
                }
                throw new FSMsgException("requestVersionTooHigh",null);
            } else {
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message("FSNameIdentifierMappingRequest." +
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
     * @throws FSMsgException when the version mismatches.
     */
    private void parseMinorVersion(String version) throws FSMsgException {
        try {
            minorVersion = Integer.parseInt(version);
        } catch (NumberFormatException e) {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("FSNameIdentifierMappingRequest." +
                        "parseMinorVersion: invalid MinorVersion: " + version, e);
            }
            throw new FSMsgException("wrongInput",null);
        }

        if (minorVersion > IFSConstants.FF_12_PROTOCOL_MINOR_VERSION) {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("FSNameIdentifierMappingRequest." +
                    "parseMinorVersion: MinorVersion is too high");
            }
            throw new FSMsgException("requestVersionTooHigh",null);
        } else if (minorVersion < IFSConstants.FF_11_PROTOCOL_MINOR_VERSION) {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("FSNameIdentifierMappingRequest." +
                    "parseMinorVersion: MinorVersion is too low");
            }
            throw new FSMsgException("requestVersionTooLow",null);
        }
    }
    
    /**
     * Signs the XML document representing
     * <code>NameIdentifierMappingRequest</code> using the certificate
     * indicated by the property "com.sun.identity.saml.xmlsig.certalias"
     * in AMConfig.properties file.
     *
     * @throws SAMLException if there is an error signing the XML document.
     */
    public void signXML() throws SAMLException {
        String certAlias = SystemConfigurationUtil.getProperty(
                Constants.SAML_XMLSIG_CERT_ALIAS);
        signXML(certAlias);
    }
    
    /**
     * Signs the XML document representing
     * <code>NameIdentifierMappingRequest</code> using the specified
     * certificate.
     *
     * @param certAlias the alias (name) of the certificate used for signing
     *                   the XML document
     * @throws SAMLException it there is an error.
     */
    public void signXML(String certAlias) throws SAMLException {
        FSUtils.debug.message("FSNameIdentifierMappingRequest.signXML");
        if (signed) {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("FSNameIdentifierMappingRequest.signXML: "
                        + "the request is already signed.");
            }
            throw new SAMLResponderException(FSUtils.BUNDLE_NAME,
                    "alreadySigned",null);
        }
        if (certAlias==null || certAlias.length()==0) {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("FSNameIdentifierMappingRequest.signXML: "
                        + "null certAlias");
            }
            throw new SAMLResponderException(FSUtils.BUNDLE_NAME,
                    "cannotFindCertAlias",null);
        }
        try {
            XMLSignatureManager manager = XMLSignatureManager.getInstance();
            signatureString = manager.signXML(this.toXMLString(true, true),
                    certAlias, (String) null, IFSConstants.REQUEST_ID,
                    this.getRequestID(), false);
            signature = XMLUtils.toDOMDocument(signatureString, FSUtils.debug)
            .getDocumentElement();
            signed = true;
        } catch (Exception e){
            FSUtils.debug.error("FSNameIdentifierMappingRequest.signXML: " +
                    "unable to sign", e);
            throw new SAMLResponderException(FSUtils.BUNDLE_NAME,
                    "signFailed",null);
            
        }
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
     * Returns the string representation of this object.
     *
     * @return An XML String representing the response.
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
        
        String prefix = "";
        String uriLIB = "";
        String uriSAML = "";
        if (includeNS) {
            prefix = IFSConstants.LIB_PREFIX;
        }
        if (declareNS) {
            uriLIB = IFSConstants.LIB_12_NAMESPACE_STRING;
            uriSAML = IFSConstants.assertionDeclareStr;
        }
        String instantString = null;
        try {
            instantString = DateUtils.toUTCDateFormat(issueInstant);
        } catch (Exception e) {
            FSUtils.debug.error("FSNameIdentifierMappingRequest.toXMLString: " +
                    "could not convert issueInstant to String.", e);
        }
        
        // construct xml request
        StringBuffer xml = new StringBuffer(1000);
        if (includeHeader) {
            xml.append(IFSConstants.XML_PREFIX)
            .append(IFSConstants.DEFAULT_ENCODING)
            .append(IFSConstants.QUOTE)
            .append(IFSConstants.SPACE)
            .append(IFSConstants.QUESTION_MARK)
            .append(IFSConstants.RIGHT_ANGLE)
            .append(IFSConstants.NL);
        }
        xml.append(IFSConstants.LEFT_ANGLE)
        .append(prefix)
        .append(IFSConstants.NAMEID_MAPPING_REQUEST)
        .append(uriLIB).append(uriSAML)
        .append(IFSConstants.SPACE)
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
        .append(prefix)
        .append(IFSConstants.PROVIDER_ID)
        .append(IFSConstants.RIGHT_ANGLE)
        .append(providerID)
        .append(IFSConstants.START_END_ELEMENT)
        .append(prefix)
        .append(IFSConstants.PROVIDER_ID)
        .append(IFSConstants.RIGHT_ANGLE);
        
        if (nameIdentifier != null) {
            xml.append(nameIdentifier.toString());
        }
        
        xml.append(IFSConstants.LEFT_ANGLE)
        .append(prefix)
        .append(IFSConstants.TARGET_NAME_SPACE)
        .append(IFSConstants.RIGHT_ANGLE)
        .append(targetNamespace)
        .append(IFSConstants.START_END_ELEMENT)
        .append(prefix)
        .append(IFSConstants.TARGET_NAME_SPACE)
        .append(IFSConstants.RIGHT_ANGLE)
        .append(IFSConstants.START_END_ELEMENT)
        .append(prefix)
        .append(IFSConstants.NAMEID_MAPPING_REQUEST)
        .append(IFSConstants.RIGHT_ANGLE);
        
        return xml.toString();
    }
}
