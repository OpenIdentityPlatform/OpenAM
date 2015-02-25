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
 * $Id: FSRequest.java,v 1.3 2008/06/25 05:46:45 qcheng Exp $
 * Portions Copyrighted 2014 ForgeRock AS
 */

/**
 * Portions Copyrighted 2012 ForgeRock AS
 */
package com.sun.identity.federation.message;

import java.util.List;
import java.util.Collections;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.StringTokenizer;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Document;
import com.sun.identity.saml.common.SAMLConstants;
import com.sun.identity.saml.common.SAMLException;
import com.sun.identity.saml.common.SAMLResponderException;
import com.sun.identity.saml.common.SAMLRequesterException;
import com.sun.identity.saml.common.SAMLRequestVersionTooHighException;
import com.sun.identity.saml.common.SAMLRequestVersionTooLowException;
import com.sun.identity.saml.xmlsig.XMLSignatureManager;
import com.sun.identity.saml.protocol.AssertionArtifact;
import com.sun.identity.saml.protocol.AttributeQuery;
import com.sun.identity.saml.protocol.AuthenticationQuery;
import com.sun.identity.saml.protocol.AuthorizationDecisionQuery;
import com.sun.identity.saml.protocol.Request;
import com.sun.identity.saml.protocol.Query;
import com.sun.identity.shared.DateUtils;
import com.sun.identity.saml.assertion.AssertionIDReference;
import com.sun.identity.federation.common.FSUtils;
import com.sun.identity.federation.common.IFSConstants;
import com.sun.identity.shared.xml.XMLUtils;

/**
 * This class represents a Liberty <code>Request</code>.
 * It extends from the abstract base class <code>AbstractRequest</code>.
 *
 * @supported.all.api
 * @deprecated since 12.0.0
 */
@Deprecated
public class FSRequest extends Request {
    /*
     * data members
     */
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
    
    /*
     * Default Constructor.
     */
    protected FSRequest() {
    }
    
    
    /**
     * Constructor creates <code>Request</code> object. This
     * shall only be used at the client side to construct a
     * <code>Request</code> object.
     * NOTE: The content here is just the body for the Request. The
     * constructor will add <code>MajorVersion</code>,
     * <code>MinorVersion</code>, etc. to form a complete Request.
     *
     * @param requestId the request identifier, if this
     *        value is null then one will be generated.
     * @param query <code>AuthenticationQuery</code> to be included in
     *        the Request.
     * @throws <code>SAMLException</code> on error.
     */
    public FSRequest(String requestId, Query query) throws SAMLException {
        super(requestId, query);
    }
    
    /**
     * Constructor creates <code>Request</code> object. This
     * shall only be used at the client side to construct a
     * <code>Request</code> object.
     * NOTE: The content here is just the body for the Request. The
     * constructor will add <code>MajorVersion</code>,
     * <code>MinorVersion</code>, etc. to form a complete Request.
     *
     * @param requestId the request identifier, if this
     *        value is null then one will be generated.
     * @param contents a <code>List</code> of objects that are the contents
     *        of Request that the client wants to send to the server.
     *        It could be an :
     *            <code>AuthenticationQuery</code>,
     *            <code>AuthorizationDecisionQuery</code>,
     *            <code>AttributeQuery</code>, 1 or more
     *            <code>AssertionIDReference</code>, or 1 or more of
     *            <code>AssertionArtifact</code>.
     * @throws <code>SAMLException</code> throws errors on exception.
     */
    public FSRequest(String requestId, List contents) throws SAMLException {
        super(requestId, contents);
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
     * Parses the <code>XML</code> Document String to construct a
     * <code>Request</code> object. This method shall only be used at the server
     * side to reconstruct a Request object based on the XML document
     * received from client.
     *
     * @param xml the <code>XML</code> Document string.
     * @return the <code>Request</code> object.
     * @throws <code>SAMLException</code> on error.
     */
    public static Request parseXML(String xml) throws SAMLException {
        // parse the xml string
        Document doc = XMLUtils.toDOMDocument(xml, FSUtils.debug);
        Element root = doc.getDocumentElement();
        return new FSRequest(root);
    }
    
    /**
     * Constructor creates a <code>FSRequest</code> object from
     * a <code>XML</code> Document Element.
     *
     * @param root the <code>XML</code> Document Element.
     * @throws <code>SAMLException</code> on error.
     */
    public FSRequest(Element root) throws SAMLException {
        // Make sure this is a Request
        String tag = null;
        if (root == null) {
            FSUtils.debug.message("Request(Element): null input.");
            throw new SAMLRequesterException(FSUtils.BUNDLE_NAME,
                    "nullInput",null);
        }
        if (((tag = root.getLocalName()) == null) ||
                (!tag.equals("Request"))) {
            FSUtils.debug.message("Request(Element): wrong input");
            throw new SAMLRequesterException(FSUtils.BUNDLE_NAME,
                    "wrongInput",null);
        }
        
        id = root.getAttribute("id");
        List signs = XMLUtils.getElementsByTagNameNS1(root,
                SAMLConstants.XMLSIG_NAMESPACE_URI,
                SAMLConstants.XMLSIG_ELEMENT_NAME);
        int signsSize = signs.size();
        if (signsSize == 1) {
            XMLSignatureManager manager = XMLSignatureManager.getInstance();
            if (id == null) {
                valid = manager.verifyXMLSignature(root,
                        IFSConstants.REQUEST_ID, null);
            } else {
                valid = manager.verifyXMLSignature(root);
            }
            if (!valid) {
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message("Request(Element): couldn't verify"
                            + " Request's signature.");
                }
            }
            xmlString = XMLUtils.print(root);
            signed = true;
        } else if (signsSize != 0) {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("Request(Element): included more than"
                        + " one Signature element.");
            }
            throw new SAMLRequesterException(FSUtils.BUNDLE_NAME,
                    "moreElement",null);
        }
        
        // Attribute RequestID
        requestID = root.getAttribute("RequestID");
        if ((requestID == null) || (requestID.length() == 0)) {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("Request(Element): Request doesn't "
                        + "have a RequestID.");
            }
            String[] args = { IFSConstants.REQUEST_ID };
            throw new SAMLRequesterException(FSUtils.BUNDLE_NAME,
                    "missingAttribute",args);
        }
        
        // Attribute MajorVersion
        parseMajorVersion(root.getAttribute("MajorVersion"));
        
        // Attribute MinorVersion
        parseMinorVersion(root.getAttribute("MinorVersion"));
        
        // Attribute IssueInstant
        String instantString = root.getAttribute("IssueInstant");
        if ((instantString == null) || (instantString.length() == 0)) {
            FSUtils.debug.message("Request(Element): missing IssueInstant");
            String[] args = { IFSConstants.ISSUE_INSTANT };
            throw new SAMLRequesterException(FSUtils.BUNDLE_NAME,
                    "missingAttribute",args);
        } else {
            try {
                issueInstant = DateUtils.stringToDate(instantString);
            } catch (Exception e) {
                FSUtils.debug.message(
                        "Request(Element): could not parse IssueInstant", e);
                throw new SAMLRequesterException(FSUtils.BUNDLE_NAME,
                        "wrongInput",null);
            }
        }
        
        // get the contents of the request
        NodeList contentnl = root.getChildNodes();
        Node child;
        String nodeName;
        String respondWith;
        for (int i = 0, length = contentnl.getLength(); i < length; i++) {
            child = contentnl.item(i);
            if ((nodeName = child.getLocalName()) != null) {
                if (nodeName.equals("RespondWith")) {
                    respondWith = XMLUtils.getElementValue((Element) child);
                    if (respondWith.length() == 0) {
                        if (FSUtils.debug.messageEnabled()) {
                            FSUtils.debug.message("Request(Element): wrong "
                                    + "RespondWith value.");
                        }
                        throw new SAMLRequesterException(FSUtils.BUNDLE_NAME,
                                "wrongInput",null);
                    }
                    if (respondWiths == Collections.EMPTY_LIST) {
                        respondWiths = new ArrayList();
                    }
                    respondWiths.add(respondWith);
                } else if (nodeName.equals("Signature")) {
                    signature = (Element) child;
                } else if (nodeName.equals("AuthenticationQuery")) {
                    // make sure the content is not assigned already
                    if (contentType != NOT_SUPPORTED) {
                        if (FSUtils.debug.messageEnabled()) {
                            FSUtils.debug.message("Request(Element): should"
                                    + "contain only one AuthenticationQuery.");
                        }
                        throw new SAMLRequesterException(FSUtils.BUNDLE_NAME,
                                "wrongInput",null);
                    }
                    contentType = AUTHENTICATION_QUERY;
                    query = new AuthenticationQuery((Element) child);
                } else if (nodeName.equals("AuthorizationDecisionQuery")) {
                    // make sure content is not assigned already
                    if (contentType != NOT_SUPPORTED) {
                        if (FSUtils.debug.messageEnabled()) {
                            FSUtils.debug.message("Request(Element): should"
                                    + "contain only one "
                                    + "AuthorizationDecisionQuery.");
                        }
                        throw new SAMLRequesterException(FSUtils.BUNDLE_NAME,
                                "wrongInput",null);
                    }
                    contentType = AUTHORIZATION_DECISION_QUERY;
                    query = new AuthorizationDecisionQuery((Element) child);
                } else if (nodeName.equals("AttributeQuery")) {
                    // make sure content is not assigned already
                    if (contentType != NOT_SUPPORTED) {
                        if (FSUtils.debug.messageEnabled()) {
                            FSUtils.debug.message("Request(Element): should"
                                    + "contain only one AttributeQuery.");
                        }
                        throw new SAMLRequesterException(FSUtils.BUNDLE_NAME,
                                "wrongInput",null);
                    }
                    contentType = ATTRIBUTE_QUERY;
                    query = new AttributeQuery((Element) child);
                } else if (nodeName.equals("AssertionIDReference")) {
                    // make sure the content has no other elements assigned
                    if ((contentType != NOT_SUPPORTED) &&
                            (contentType != ASSERTION_ID_REFERENCE)) {
                        if (FSUtils.debug.messageEnabled()) {
                            FSUtils.debug.message("Request(Element): "
                                    + "contained mixed contents.");
                        }
                        throw new SAMLRequesterException(FSUtils.BUNDLE_NAME,
                                "wrongInput",null);
                    }
                    contentType = ASSERTION_ID_REFERENCE;
                    if (assertionIDRefs == Collections.EMPTY_LIST) {
                        assertionIDRefs = new ArrayList();
                    }
                    assertionIDRefs.add(new AssertionIDReference(
                            XMLUtils.getElementValue((Element) child)));
                } else if (nodeName.equals("AssertionArtifact")) {
                    // make sure the content has no other elements assigned
                    if ((contentType != NOT_SUPPORTED) &&
                            (contentType != ASSERTION_ARTIFACT)) {
                        if (FSUtils.debug.messageEnabled()) {
                            FSUtils.debug.message("Request(Element): "
                                    + "contained mixed contents.");
                        }
                        throw new SAMLRequesterException(FSUtils.BUNDLE_NAME,
                                "wrongInput",null);
                    }
                    contentType = ASSERTION_ARTIFACT;
                    if (artifacts == Collections.EMPTY_LIST) {
                        artifacts = new ArrayList();
                    }
                    artifacts.add(new AssertionArtifact(
                            XMLUtils.getElementValue((Element) child)));
                } else if (nodeName.equals("Query") ||
                        nodeName.equals("SubjectQuery")) {
                    parseQuery(child);
                } else {
                    if (FSUtils.debug.messageEnabled()) {
                        FSUtils.debug.message("Request(Element): invalid"
                                + " node" + nodeName);
                    }
                    throw new SAMLRequesterException(FSUtils.BUNDLE_NAME,
                            "wrongInput",null);
                } // check nodeName
            } // if nodeName != null
        } // done for the nodelist loop
        
        if (contentType == NOT_SUPPORTED) {
            FSUtils.debug.message("Request: empty content.");
            throw new SAMLRequesterException(FSUtils.BUNDLE_NAME,
                    "wrongInput",null);
        }
    }
    
    /* Returns the value of <code>RespondWith</code> attribute.
     *
     * @return value of the <code>RespondWith</code> attribute.
     * @throws <code>SAMLException</code> on error.
     */
    private String checkAndGetRespondWith(String respondWith)
    throws SAMLException {
        if ((respondWith == null) || (respondWith.length() == 0)) {
            FSUtils.debug.message("Request: empty RespondWith Value.");
            throw new SAMLRequesterException(FSUtils.BUNDLE_NAME,
                    "wrongInput",null);
        }
        
        if (respondWith.indexOf(":") == -1) {
            return (SAMLConstants.ASSERTION_PREFIX + respondWith);
        } else {
            StringTokenizer st = new StringTokenizer(respondWith, ":");
            if (st.countTokens() != 2) {
                FSUtils.debug.message("Request: wrong RespondWith value.");
                throw new SAMLRequesterException(FSUtils.BUNDLE_NAME,
                        "wrongInput",null);
            }
            st.nextToken();
            String temp = st.nextToken().trim();
            if (temp.length() == 0) {
                FSUtils.debug.message("Request: wrong RespondWith value.");
                throw new SAMLRequesterException(FSUtils.BUNDLE_NAME,
                        "wrongInput",null);
            }
            return (SAMLConstants.ASSERTION_PREFIX + temp);
        }
    }
    
    
    /**
     * Sets the <code>MajorVersion</code> by parsing the version string.
     *
     * @param majorVer a String representing the <code>MajorVersion</code> to
     *        be set.
     * @throws <code>FSMsgException</code> on error.
     */
    
    private void parseMajorVersion(String majorVer) throws SAMLException {
        try {
            majorVersion = Integer.parseInt(majorVer);
        } catch (NumberFormatException e) {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("Request(Element): invalid "
                        + "MajorVersion", e);
            }
            throw new SAMLRequesterException(FSUtils.BUNDLE_NAME,
                    "wrongInput",null);
        }
        
        if (majorVersion != SAMLConstants.PROTOCOL_MAJOR_VERSION) {
            if (majorVersion > SAMLConstants.PROTOCOL_MAJOR_VERSION) {
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message("Request(Element):MajorVersion of "
                            + "the Request is too high.");
                }
                throw new SAMLRequestVersionTooHighException(
                        FSUtils.BUNDLE_NAME,"requestVersionTooHigh",null);
            } else {
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message("Request(Element):MajorVersion of "
                            + "the Request is too low.");
                }
                throw new SAMLRequestVersionTooLowException(FSUtils.BUNDLE_NAME,
                        "requestVersionTooLow",null);
            }
        }
    }
    
    /**
     * Sets the <code>MinorVersion</code> by parsing the version string.
     *
     * @param minorVer a String representing the <code>MinorVersion</code> to
     *        be set.
     * @throws <code>SAMLException</code> when the version mismatchs.
     */
    private void parseMinorVersion(String minorVer) throws SAMLException {
        try {
            minorVersion = Integer.parseInt(minorVer);
        } catch (NumberFormatException e) {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("Request(Element): invalid "
                        + "MinorVersion", e);
            }
            throw new SAMLRequesterException(FSUtils.BUNDLE_NAME,
                    "wrongInput",null);
        }
        
        if(minorVersion > IFSConstants.FF_12_SAML_PROTOCOL_MINOR_VERSION) {
            FSUtils.debug.error("Request(Element): MinorVersion"
                    + " of the Request is too high.");
            throw new SAMLRequestVersionTooHighException(FSUtils.BUNDLE_NAME,
                    "requestVersionTooHigh",null);
        } else if (minorVersion <
                IFSConstants.FF_11_SAML_PROTOCOL_MINOR_VERSION) {
            FSUtils.debug.error("Request(Element): MinorVersion"
                    + " of the Request is too low.");
            throw new SAMLRequestVersionTooLowException(FSUtils.BUNDLE_NAME,
                    "requestVersionTooLow",null);
        }
    }
    
    /**
     * Parses the Query or <code>SubjectQuery</code> represented by
     * a DOM tree Node. It then checks and sets data members if it is a
     * supported query, such as <code>AuthenticationQuery</code>,
     * <code>AttributeQeury</code>, or <code>AuthorizationDecisionQuery</code>.
     *
     * @param child a <code>DOM</code> Node.
     * @throws <code>SAMLException</code> if the <code>Query</code> is invalid.
     */
    private void parseQuery(Node child) throws SAMLException {
        NamedNodeMap nm = child.getAttributes();
        int len = nm.getLength();
        String attrName;
        String attrValue;
        Attr attr;
        boolean found = false;
        for (int j = 0; j < len; j++) {
            attr = (Attr) nm.item(j);
            attrName = attr.getLocalName();
            if ((attrName != null) && (attrName.equals("type"))) {
                attrValue = attr.getNodeValue();
                if (attrValue.equals("AuthenticationQueryType")) {
                    if (contentType != NOT_SUPPORTED) {
                        if (FSUtils.debug.messageEnabled()) {
                            FSUtils.debug.message("Request(Element): should"
                                    + " contain only one AuthenticationQuery.");
                        }
                        throw new SAMLRequesterException(FSUtils.BUNDLE_NAME,
                                "wrongInput",null);
                    }
                    contentType = AUTHENTICATION_QUERY;
                    query = new AuthenticationQuery((Element) child);
                } else if (attrValue.equals(
                        "AuthorizationDecisionQueryType")) {
                    if (contentType != NOT_SUPPORTED) {
                        if (FSUtils.debug.messageEnabled()) {
                            FSUtils.debug.message("Request(Element): should "
                                    + "contain one "
                                    + "AuthorizationDecisionQuery.");
                        }
                        throw new SAMLRequesterException(FSUtils.BUNDLE_NAME,
                                "wrongInput",null);
                    }
                    contentType = AUTHORIZATION_DECISION_QUERY;
                    query = new AuthorizationDecisionQuery((Element) child);
                } else if (attrValue.equals("AttributeQueryType")) {
                    if (contentType != NOT_SUPPORTED) {
                        if (FSUtils.debug.messageEnabled()) {
                            FSUtils.debug.message("Request(Element): should "
                                    + "contain one AttributeQuery.");
                        }
                        throw new SAMLRequesterException(FSUtils.BUNDLE_NAME,
                                "wrongInput",null);
                    }
                    contentType = ATTRIBUTE_QUERY;
                    query = new AttributeQuery((Element) child);
                } else {
                    if (FSUtils.debug.messageEnabled()) {
                        FSUtils.debug.message("Request(Element): This type of"
                                + " " + attrName + " is not supported.");
                    }
                    throw new SAMLResponderException(FSUtils.BUNDLE_NAME,
                            "queryNotSupported",null);
                } // check typevalue
                found = true;
                break;
            } // if found type attribute
        } // end attribute for loop
        // if not found type
        if (!found) {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("Request(Element): missing"
                        + " xsi:type definition in " + child.getLocalName());
            }
            throw new SAMLRequesterException(FSUtils.BUNDLE_NAME,
                    "wrongInput",null);
        }
    }
    
    /**
     * Creates a String representation of the <code>&lt;samlp:Request&gt;</code>
     * element.
     *
     * @return a <code>XML</code> String representing the request.
     */
    public String toXMLString() {
        return toXMLString(true, true);
    }
    
    /**
     * Creates a String representation of the <code>&lt;samlp:Request&gt;</code>
     * element.
     *
     * @param includeNS Determines whether or not the names pace qualifier
     *        is prepended to the Element when converted
     * @param declareNS Determines whether or not the name space is declared
     *        within the Element.
     * @return a string containing the valid XML for this object.
     */
    public String toXMLString(boolean includeNS, boolean declareNS) {
        return toXMLString(includeNS, declareNS, false);
    }
    
    /**
     * Creates a String representation of the <code>&lt;samlp:Request&gt;</code>
     * element.
     *
     * @param includeNS Determines whether or not the name space qualifier
     *        is prepended to the Element when converted
     * @param declareNS Determines whether or not the name space is declared
     *        within the Element.
     * @param includeHeader Determines whether the output include the XML
     *              declaration header.
     * @return a string containing the valid XML for this object.
     */
    public String toXMLString(boolean includeNS,boolean declareNS,
            boolean includeHeader) {
        if (signed && (xmlString != null)) {
            return xmlString;
        }
        
        StringBuffer xml = new StringBuffer(300);
        if (includeHeader) {
            xml.append("<?xml version=\"1.0\" encoding=\"").
                    append(SAMLConstants.DEFAULT_ENCODING).append("\" ?>\n");
        }
        String prefix = "";
        String libprefix = "";
        String uri = "";
        String liburi = "";
        String uriXSI="";
        if (includeNS) {
            prefix = SAMLConstants.PROTOCOL_PREFIX;
            libprefix = IFSConstants.LIB_PREFIX;
        }
        if (declareNS) {
            uri = SAMLConstants.PROTOCOL_NAMESPACE_STRING;
            if(minorVersion == IFSConstants.FF_12_SAML_PROTOCOL_MINOR_VERSION){
                liburi = IFSConstants.LIB_12_NAMESPACE_STRING;
            } else {
                liburi = IFSConstants.LIB_NAMESPACE_STRING;
            }
            uriXSI = IFSConstants.XSI_NAMESPACE_STRING;
        }
        
        String instantString = DateUtils.toUTCDateFormat(issueInstant);
        
        xml.append("<").append(prefix).append("Request").append(uri).
                //append(" xmlns=\"http://www.w3.org/2000/xmlns/\"").append(uri).
                append(" ").append(liburi).append(" ").append(uriXSI);
        if(minorVersion == IFSConstants.FF_11_SAML_PROTOCOL_MINOR_VERSION) {
            if(id != null && !(id.length() == 0)){
                xml.append(" id=\"").append(id).append("\"");
            }
        }
        xml.append(" RequestID=\"").append(requestID).append("\"").
                append(" MajorVersion=\"").append(majorVersion).append("\"").
                append(" MinorVersion=\"").append(minorVersion).append("\"").
                append(" IssueInstant=\"").append(instantString).append("\"");
        
        if(minorVersion == IFSConstants.FF_11_SAML_PROTOCOL_MINOR_VERSION) {
            xml.append(" xsi:type").append("=\"").append(libprefix).
                    append("SignedSAMLRequestType").append("\"");
        }
        xml.append(">");
        if((respondWiths != null) && (respondWiths != Collections.EMPTY_LIST)){
            Iterator i = respondWiths.iterator();
            String respondWith = null;
            while (i.hasNext()) {
                respondWith = (String) i.next();
                xml.append("<").append(prefix).append("RespondWith>");
                if (respondWith.startsWith(SAMLConstants.ASSERTION_PREFIX)) {
                    xml.append(respondWith);
                } else {
                    try {
                        xml.append(checkAndGetRespondWith(respondWith));
                    } catch (SAMLException e) {
                        FSUtils.debug.error("Request.toString: ", e);
                        xml.append(respondWith);
                    }
                }
                xml.append("</").append(prefix).append("RespondWith>");
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
        Iterator j;
        switch (contentType) {
            case AUTHENTICATION_QUERY:
                xml.append(((AuthenticationQuery)query)
                   .toString(includeNS, false));
                break;
            case AUTHORIZATION_DECISION_QUERY:
                xml.append(((AuthorizationDecisionQuery)query)
                   .toString(includeNS,false));
                break;
            case ATTRIBUTE_QUERY:
                xml.append(((AttributeQuery)query).toString(includeNS, false));
                break;
            case ASSERTION_ID_REFERENCE:
                j = assertionIDRefs.iterator();
                while (j.hasNext()) {
                    xml.append(((AssertionIDReference) j.next()).
                            toString(true, true));
                }
                break;
            case ASSERTION_ARTIFACT:
                j = artifacts.iterator();
                while (j.hasNext()) {
                    xml.append(((AssertionArtifact)
                    j.next()).toString(includeNS, false));
                }
                break;
            default:
                break;
        }
        
        xml.append("</").append(prefix).append("Request>");
        return xml.toString();
    }
}
