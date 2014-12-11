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
 * $Id: FSResponse.java,v 1.2 2008/06/25 05:46:45 qcheng Exp $
 * Portions Copyrighted 2014 ForgeRock AS
 */

package com.sun.identity.federation.message;

import java.text.ParseException;
import java.util.List;
import java.util.Collections;
import java.util.ArrayList;
import java.util.Iterator;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Document;

import com.sun.identity.shared.encode.Base64;
import com.sun.identity.shared.xml.XMLUtils;
import com.sun.identity.shared.DateUtils;
import com.sun.identity.saml.common.SAMLConstants;
import com.sun.identity.saml.common.SAMLException;
import com.sun.identity.saml.common.SAMLResponderException;
import com.sun.identity.saml.common.SAMLVersionMismatchException;
import com.sun.identity.saml.xmlsig.XMLSignatureManager;
import com.sun.identity.saml.protocol.Response;
import com.sun.identity.saml.protocol.Status;

import com.sun.identity.federation.message.common.FSMsgException;
import com.sun.identity.federation.common.FSUtils;
import com.sun.identity.federation.common.IFSConstants;

/**
 * This class contains methods for creating a Liberty <code>Response</code>.
 *
 * @supported.all.api
 * @deprecated since 12.0.0
 */
@Deprecated
public class FSResponse extends Response {
    protected String id = null;
    
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
    public void setID(String id) {
        this.id = id;
    }
    
    /**
     * Returns the signed <code>XML</code> string.
     *
     * @return the signed <code>XML</code> string.
     */
    public String getSignatureString(){
        return signatureString;
    }
    
    /**
     * Returns the <code>MinorVersion</code>.
     *
     * @return the <code>MinorVersion</code>.
     * @see #setMinorVersion(int)
     */
    
    public int getMinorVersion() {
        return minorVersion;
    }
    
    /**
     * Sets the <code>MinorVersion</code>.
     *
     * @param version the <code>MinorVersion</code>.
     * @see #getMinorVersion()
     */
    public void setMinorVersion(int version) {
        minorVersion = version;
    }
    
    /**
     * Constructor creates <code>FSResponse</code> object.
     *
     * @param responseID value of <code>ResponseId</code> attribute.
     * @param inResponseTo value of <code>inResponseTo</code> attribute.
     * @param status the <code>Status</code> object.
     * @param contents list containing response elements.
     * @throws SAMLException it there is an error creating this object.
     * @throws FSMsgException it there is an error creating this object.
     */
    public FSResponse(String responseID,
            String inResponseTo,
            Status status,
            List contents) throws SAMLException, FSMsgException {
        super( responseID, inResponseTo, status, contents);
    }
    
    public static FSResponse parseResponseXML(
            String xml
            ) throws SAMLException, FSMsgException {
        // parse the xml string
        FSUtils.debug.message("FSResponse.parseResponseXML: Called");
        Element root;
        Document doc = XMLUtils.toDOMDocument(xml, FSUtils.debug);
        if (doc == null) {
            FSUtils.debug.error("FSResponse.parseXML:Error "
                    + "while parsing input xml string");
            throw new FSMsgException("parseError",null);
        }
        root = doc.getDocumentElement();
        return new FSResponse(root);
    }
    
    /**
     * Constructor creates <code>FSResponse</code> object form
     * a Document Element.
     *
     * @param root the Document Element object.
     * @throws SAMLException if there is an error creating this object.
     * @throws FSMsgException if there is an error creating this object.
     */
    public FSResponse(Element root) throws SAMLException, FSMsgException {
        FSUtils.debug.message("FSResponse(Element): Called");
        if (root == null) {
            FSUtils.debug.message("FSResponse(Element): "
                    + "Input paramenter (root) is null");
            throw new FSMsgException("nullInput",null);
        }
        String tag = null;
        if (((tag = root.getLocalName()) == null) ||
                (!tag.equals("Response"))) {
            FSUtils.debug.message("FSResponse(Element): "
                    + "Root element name is not Response");
            throw new FSMsgException("wrongInput",null);
        }
        id = root.getAttribute("id");
        responseID = root.getAttribute("ResponseID");
        if ((responseID == null) || (responseID.length() == 0)) {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("FSResponse(Element): "
                        + "Response doesn't have ResponseID attribute");
            }
            String[] args = { IFSConstants.RESPONSE_ID };
            throw new FSMsgException("missingAttribute",args);
        }
        
        inResponseTo = root.getAttribute("InResponseTo");
        if (inResponseTo == null) {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("FSResponse(Element): "
                        + "Response doesn't have InResponseTo attribute");
            }
            String[] args = { IFSConstants.IN_RESPONSE_TO };
            throw new FSMsgException("missingAttribute",args);
        }
        
        // Attribute IssueInstant
        String instantString = root.getAttribute("IssueInstant");
        if ((instantString == null) || (instantString.length() == 0)) {
            FSUtils.debug.message("FSResponse(Element): missing IssueInstant");
            String[] args = { IFSConstants.ISSUE_INSTANT };
            throw new FSMsgException("missingAttribute",args);
        } else {
            try {
                issueInstant = DateUtils.stringToDate(instantString);
            } catch (ParseException e) {
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message("FSResponse(Element): could not "
                            + "parse IssueInstant:", e);
                }
                throw new FSMsgException("wrongInput", null);
            }
        }
        parseMajorVersion(root.getAttribute("MajorVersion"));
        parseMinorVersion(root.getAttribute("MinorVersion"));
        setRecipient(root.getAttribute("Recipient"));
        NodeList nl = root.getChildNodes();
        Node child;
        String childName;
        int length = nl.getLength();
        for (int i = 0; i < length; i++) {
            child = nl.item(i);
            if ((childName = child.getLocalName()) != null) {
                if (childName.equals("Status")) {
                    if (status != null) {
                        if (FSUtils.debug.messageEnabled()) {
                            FSUtils.debug.message(
                                    "FSResponse(Element): included more"
                                    + " than one <Status>");
                        }
                        throw new FSMsgException("moreElement",null);
                    }
                    status = new Status((Element) child);
                } else if (childName.equals("Assertion")) {
                    if (assertions == Collections.EMPTY_LIST) {
                        assertions = new ArrayList();
                    }
                    assertions.add(new FSAssertion((Element) child));
                }else {
                    if (FSUtils.debug.messageEnabled()) {
                        FSUtils.debug.message(
                                "FSResponse(Element): included wrong "
                                + "element: " + childName);
                    }
                    throw new FSMsgException("wrongInput",null);
                }
            } // end if childName != null
        } // end for loop
        
        if (status == null) {
            FSUtils.debug.message(
                    "FSResponse(Element): missing element <Status>.");
            throw new FSMsgException("missingElement",null);
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
                FSUtils.debug.message("FSResponse(Element): included more than"
                        + " one Signature element.");
            }
            throw new FSMsgException("moreElement",null);
        }
        //end check for signature
    }
    
    /**
     * Sets the <code>MajorVersion</code> by parsing the version string.
     *
     * @param majorVer a String representing the <code>MajorVersion</code> to
     *        be set.
     * @throws SAMLException on error.
     * @throws FSMsgException if there is an error parsing the version string.
     */
    private void parseMajorVersion(String majorVer)
    throws SAMLException, FSMsgException {
        try {
            majorVersion = Integer.parseInt(majorVer);
        } catch (NumberFormatException e) {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("FSResponse(Element): invalid "
                        + "MajorVersion", e);
            }
            throw new FSMsgException("wrongInput",null);
        }
        
        if (majorVersion != SAMLConstants.PROTOCOL_MAJOR_VERSION) {
            if (majorVersion > SAMLConstants.PROTOCOL_MAJOR_VERSION) {
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message("FSResponse(Element):MajorVersion of"
                            + " the Response is too high.");
                }
                throw new SAMLVersionMismatchException(FSUtils.BUNDLE_NAME,
                        "responseVersionTooHigh",null);
            } else {
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message("FSResponse(Element):MajorVersion of"
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
     * @throws FSMsgException if there is an error
     *          parsing the version string.
     */
    private void parseMinorVersion(String minorVer)
    throws SAMLException, FSMsgException {
        try {
            minorVersion = Integer.parseInt(minorVer);
        } catch (NumberFormatException e) {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("FSResponse(Element): invalid "
                        + "MinorVersion", e);
            }
            throw new FSMsgException("wrongInput",null);
        }
        
        if (minorVersion > IFSConstants.FF_12_SAML_PROTOCOL_MINOR_VERSION) {
            FSUtils.debug.error("FSResponse(Element):MinorVersion of"
                    + " the Response is too high.");
            throw new SAMLVersionMismatchException(FSUtils.BUNDLE_NAME,
                    "responseVersionTooHigh",null);
        } else if (minorVersion <
                IFSConstants.FF_11_SAML_PROTOCOL_MINOR_VERSION) {
            FSUtils.debug.error("FSResponse(Element):MinorVersion of"
                    + " the Response is too low.");
            throw new SAMLVersionMismatchException(FSUtils.BUNDLE_NAME,
                    "responseVersionTooLow",null);
        }
    }
    
    /**
     * Returns a String representation of the Logout Response.
     *
     * @return a string containing the valid XML for this element
     * @throws FSMsgException if there is an error converting
     *         this object to a string.
     */
    public String toXMLString() throws FSMsgException {
        return this.toXMLString(true, true);
    }
    
    /**
     * Returns a String representation of the Logout Response.
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
    
    public String toXMLString(boolean includeNS,boolean declareNS,
            boolean includeHeader)  throws FSMsgException {
        FSUtils.debug.message("FSResponse.toXMLString(3): Called");
        StringBuffer xml = new StringBuffer(500);
        if (includeHeader) {
            xml.append("<?xml version=\"1.0\" encoding=\"").
                    append(SAMLConstants.DEFAULT_ENCODING).append("\" ?>");
        }
        String prefixSAML=null;
        String prefixLIB=null;
        String prefixSAML_PROTOCOL = "";
        String uriSAML_PROTOCOL = "";
        String uriSAML = "";
        String uriLIB = "";
        String uriDS="";
        String uriXSI="";
        
        if (includeNS) {
            prefixLIB = IFSConstants.LIB_PREFIX;
            prefixSAML = IFSConstants.ASSERTION_PREFIX;
            prefixSAML_PROTOCOL = IFSConstants.PROTOCOL_PREFIX;
        }
        if (declareNS) {
            if(minorVersion == IFSConstants.FF_12_SAML_PROTOCOL_MINOR_VERSION){
                uriLIB = IFSConstants.LIB_12_NAMESPACE_STRING;
            } else {
                uriLIB = IFSConstants.LIB_NAMESPACE_STRING;
            }
            uriSAML = IFSConstants.assertionDeclareStr;
            uriSAML_PROTOCOL = IFSConstants.PROTOCOL_NAMESPACE_STRING;
            uriDS = IFSConstants.DSSAMLNameSpace;
            uriXSI = IFSConstants.XSI_NAMESPACE_STRING;
        }
        
        String instantString = DateUtils.toUTCDateFormat(issueInstant);
        
        if((responseID != null) && (inResponseTo != null)){
            xml.append("<").append(prefixSAML_PROTOCOL).append("Response").
                    append(uriLIB).
                    append(uriSAML).append(uriSAML_PROTOCOL).append(" ").
                    append(uriDS).
                    append(" ").append(uriXSI).append(" ResponseID=\"").
                    append(responseID).append("\" ");
            if ((inResponseTo != null) && (inResponseTo.length() != 0)) {
                xml.append(" InResponseTo=\"").append(inResponseTo).
                        append("\" ");
            }
            if (minorVersion == IFSConstants.FF_11_PROTOCOL_MINOR_VERSION &&
                    id != null && !(id.length() == 0)){
                xml.append(" id=\"").append(id).append("\"");
            }
            xml.append(" MajorVersion=\"").
                    append(majorVersion).append("\" ").
                    append(" MinorVersion=\"").append(minorVersion).
                    append("\" ").
                    append(" IssueInstant=\"").append(instantString).
                    append("\"");
            if ((recipient != null) && (recipient.length() != 0)) {
                xml.append(" Recipient=\"").append(recipient).append("\" ");
            }
            xml.append(">");
        }
        
        if (signed) {
            if (signatureString != null) {
                xml.append(signatureString);
            } else if (signature != null) {
                signatureString = XMLUtils.print(signature);
                xml.append(signatureString);
            }
        }
        
        if(status != null)
            xml.append(status.toString(includeNS, false));
        
        if ((assertions != null) && (assertions != Collections.EMPTY_LIST)) {
            Iterator j = assertions.iterator();
            while (j.hasNext()) {
                xml.append(((FSAssertion) j.next()).
                        toXMLString(true,declareNS));
            }
        }
        
        xml.append("</").append(prefixSAML_PROTOCOL).append("Response>");
        return xml.toString();
    }
    
    /**
     * Returns <code>FSResponse</code> object. The object
     * is created by parsing an Base64 encoded response string.
     *
     * @param encodedRes the encoded response string
     * @throws FSMsgException if there is an error creating
     *            <code>FSResponse</code> object.
     * @throws FSMsgException if there is an error creating
     *            <code>FSResponse</code> object.
     */
    public static FSResponse parseBASE64EncodedString(
            String encodedRes) throws FSMsgException, SAMLException {
        FSUtils.debug.message("FSResponse.parseBASE64EncodedString:Called new");
        if (encodedRes != null) {
            String decodedAuthnRes = new String(Base64.decode(encodedRes));
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("FSResponse.parseBASE64EncodedString:"
                        + "Decoded AuthnResponse message: "
                        + decodedAuthnRes);
            }
            return parseResponseXML(decodedAuthnRes);
        } else{
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("FSResponse.parseBASE64EncodedString:"
                        + "null String passed in as argument.");
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
        FSUtils.debug.message("FSResponse.toBASE64EncodedString: Called");
        if ((responseID == null) || (responseID.length() == 0)){
            responseID = FSUtils.generateID();
            if (responseID == null) {
                FSUtils.debug.error("FSResponse.toBASE64EncodedString: "
                        + "couldn't generate ResponseID.");
                throw new FSMsgException("errorGenerateID",null);
            }
        }
        return Base64.encode(this.toXMLString().getBytes());
    }
    
    /**
     * Signs the Response.
     *
     * @param certAlias the Certificate Alias.
     * @throws XMLSignatureException if <code>FSAuthnRequest</code>
     *         cannot be signed.
     */
    public void signXML(String certAlias) throws SAMLException {
        FSUtils.debug.message("FSResponse.signXML: Called");
        if (signed) {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("FSResponse.signXML: the assertion is "
                        + "already signed.");
            }
            throw new SAMLResponderException(FSUtils.BUNDLE_NAME,
                    "alreadySigned",null);
        }
        if (certAlias == null || certAlias.length() == 0) {
            throw new SAMLResponderException(FSUtils.BUNDLE_NAME,
                    "cannotFindCertAlias",null);
        }
        try {
            XMLSignatureManager manager = XMLSignatureManager.getInstance();
            if (minorVersion == IFSConstants.FF_11_PROTOCOL_MINOR_VERSION) {
                signatureString = manager.signXML(
                        this.toXMLString(true, true),
                        certAlias, IFSConstants.DEF_SIG_ALGO,
                        IFSConstants.ID,
                        this.id, false);
            } else if (minorVersion ==
                    IFSConstants.FF_12_PROTOCOL_MINOR_VERSION) {
                signatureString = manager.signXML(
                        this.toXMLString(true, true),
                        certAlias, IFSConstants.DEF_SIG_ALGO,
                        IFSConstants.RESPONSE_ID,
                        this.getResponseID(), false);
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
     * Unsupported operation.
     */
    public void signXML() throws SAMLException {
        throw new SAMLException(FSUtils.BUNDLE_NAME,
                "unsupportedOperation",null);
    }
}
