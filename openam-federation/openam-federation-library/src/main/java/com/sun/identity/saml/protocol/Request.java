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
 * $Id: Request.java,v 1.2 2008/06/25 05:47:37 qcheng Exp $
 *
 * Portions Copyrighted 2016 ForgeRock AS.
 */

package com.sun.identity.saml.protocol;

import static org.forgerock.openam.utils.Time.*;

import com.sun.identity.common.SystemConfigurationUtil;
import com.sun.identity.shared.xml.XMLUtils;
import com.sun.identity.shared.DateUtils;
import com.sun.identity.saml.assertion.AssertionIDReference;
import com.sun.identity.saml.common.SAMLConstants;
import com.sun.identity.saml.common.SAMLException;
import com.sun.identity.saml.common.SAMLRequesterException;
import com.sun.identity.saml.common.SAMLRequestVersionTooHighException;
import com.sun.identity.saml.common.SAMLRequestVersionTooLowException;
import com.sun.identity.saml.common.SAMLResponderException;
import com.sun.identity.saml.common.SAMLUtils;

import com.sun.identity.saml.xmlsig.XMLSignatureManager;

import java.io.ByteArrayOutputStream;

import java.text.ParseException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * This <code>Request</code> class represents a Request XML document.
 * It extends from the abstract base class <code>AbstractRequest</code>.
 *
 * @supported.all.api
 */
public class Request extends AbstractRequest {
    /*
     * data members
     */

    protected Query	query		= null;
    protected List	assertionIDRefs	= Collections.EMPTY_LIST;
    protected List	artifacts	= Collections.EMPTY_LIST;
    protected int	contentType	= NOT_SUPPORTED;
    protected String	xmlString	= null;
    protected String	signatureString	= null;

    // Request ID attribute name
    private static final String REQUEST_ID_ATTRIBUTE = "RequestID";

    /**
     * The request is not supported.
     */
    public final static int NOT_SUPPORTED			= -1;

    /**
     * The request is an Authentication Query.
     */
    public final static int AUTHENTICATION_QUERY		= 0;

    /**
     * The request is an Authorization Decision Query.
     */
    public final static int AUTHORIZATION_DECISION_QUERY	= 1;

    /**
     * The request is an Assertion ID Reference.
     */
    public final static int ASSERTION_ID_REFERENCE		= 2;

    /**
     * The request is an Assertion Artifact.
     */
    public final static int ASSERTION_ARTIFACT			= 3;

    /**
     * The request is an Attribute Query.
     */
    public final static int ATTRIBUTE_QUERY			= 4;

    /*
     * Constructors
     */
    protected Request() {}

    /**
     * Method to sign the Request.
     * @exception SAMLException if could not sign the Request.
     */
    public void signXML() throws SAMLException {
	if (signed) {
	    if (SAMLUtils.debug.messageEnabled()) {
		SAMLUtils.debug.message("Request.signXML: the request is "
		    + "already signed.");
	    }
	    throw new SAMLException(
		SAMLUtils.bundle.getString("alreadySigned"));
	}
        String certAlias =    
            SystemConfigurationUtil.getProperty(
            "com.sun.identity.saml.xmlsig.certalias");
        if (certAlias == null) {
	    if (SAMLUtils.debug.messageEnabled()) {
		SAMLUtils.debug.message("Request.signXML: couldn't obtain "
		    + "this site's cert Alias.");
	    }
	    throw new SAMLResponderException(
		SAMLUtils.bundle.getString("cannotFindCertAlias"));
	}
	XMLSignatureManager manager = XMLSignatureManager.getInstance();
        if ((majorVersion == 1) && (minorVersion == 0)) {
            SAMLUtils.debug.message("Request.signXML: sign with version 1.0");
            signatureString = manager.signXML(this.toString(true, true),
                              certAlias);
            // this block is used for later return of signature element by
            // getSignature() method
            signature =
                XMLUtils.toDOMDocument(signatureString, SAMLUtils.debug)
                        .getDocumentElement();
        } else {
            Document doc = XMLUtils.toDOMDocument(this.toString(true, true),
                                                  SAMLUtils.debug);
            // sign with SAML 1.1 spec & include cert in KeyInfo
            signature = manager.signXML(doc, certAlias, null,
                REQUEST_ID_ATTRIBUTE, getRequestID(), true, null);
            signatureString = XMLUtils.print(signature);
        }
	signed = true;
	xmlString = this.toString(true, true);
    }

    /**
     * This constructor shall only be used at the client side to construct a
     * Request object.
     * NOTE: The content here is just the body for the Request. The 
     * constructor will add <code>MajorVersion</code>,
     * <code>MinorVersion</code>, etc. to form a complete Request.
     * @param respondWiths A List of Strings representing
     *        <code>RespondWith</code> elements. It could be null when there is
     *        no <code>&lt;RespondWith&gt;</code>. Each string could be prefixed
     *        by <code>saml:</code>. If it is not prefixed, or prefixed by a
     *        prefix other than <code>saml:</code>, <code>saml:</code> will be
     *        used instead.
     * @param requestId If it's null, the constructor will create one.
     * @param contents A List of objects that are the contents of Request that
     *	      the client wants to send to the server. It could be an
     *        <code>AuthenticationQuery</code>,
     *        <code>AuthorizationDecisionQuery</code>,
     *        <code>AttributeQuery</code>, 1 or more
     *        <code>AssertionIDReference</code>, or 1 or more of
     *        <code>AssertionArtifact</code>.
     * @exception SAMLException if an error occurs.
     */
    public Request(List respondWiths,
			String requestId,
			List contents) throws SAMLException {
	Object temp = null;

	if ((respondWiths != null) &&
	    (respondWiths != Collections.EMPTY_LIST)) {
	    for (int i = 0, length = respondWiths.size(); i < length; i++) {
		temp = respondWiths.get(i);
		if (!(temp instanceof String)) {
		    if (SAMLUtils.debug.messageEnabled()) {
			SAMLUtils.debug.message("Request: wrong input for "
				+ "RespondWith");
		    }
		    throw new SAMLRequesterException(
			SAMLUtils.bundle.getString("wrongInput"));
		}
		if ((this.respondWiths == null) ||
		    (this.respondWiths.size() == 0)) {
		    this.respondWiths = new ArrayList();
		}
		(this.respondWiths).add(checkAndGetRespondWith((String)temp));
	    }
	}

	if ((requestId != null) && (requestId.length() != 0)) {
	    requestID = requestId;
	} else {
	    // random generate one
	    requestID = SAMLUtils.generateID();
	    if (requestID == null) {
		SAMLUtils.debug.error("Request: couldn't generate RequestID.");
		throw new SAMLRequesterException(
		    SAMLUtils.bundle.getString("errorGenerateID"));
	    }
	}

	parseContents(contents);
		issueInstant = newDate();
    }

    private String checkAndGetRespondWith(String respondWith)
						throws SAMLException
    {
	if ((respondWith == null) || (respondWith.length() == 0)) {
	    SAMLUtils.debug.message("Request: empty RespondWith Value.");
	    throw new SAMLRequesterException(
		SAMLUtils.bundle.getString("wrongInput"));
	}

	if (respondWith.indexOf(":") == -1) {
	    return (SAMLConstants.ASSERTION_PREFIX + respondWith);
	} else {
	    StringTokenizer st = new StringTokenizer(respondWith, ":");
	    if (st.countTokens() != 2) {
		SAMLUtils.debug.message("Request: wrong RespondWith value.");
		throw new SAMLRequesterException(
			SAMLUtils.bundle.getString("wrongInput"));
	    }
	    st.nextToken();
	    String temp = st.nextToken().trim();
	    if (temp.length() == 0) {
		SAMLUtils.debug.message("Request: wrong RespondWith value.");
		throw new SAMLRequesterException(
			SAMLUtils.bundle.getString("wrongInput"));
	    }
	    return (SAMLConstants.ASSERTION_PREFIX + temp);
	}
    }

    /**
     * Checks the contents of the Request and set the class members accordingly.
     *
     * Used by this class only.
     * @param contents A List that contains the contents of the request. 
     *	      it could be a query, 1 or more <code>AssertionIDReference</code>,
     *	      or 1 or more <code>AssertionArtifact</code>.
     * @exception SAMLException when an error occurs during the process.
     */
    private void parseContents(List contents) throws SAMLException {
	// check contents and set the contentType appropriately
	int length = 0;
	int i = 0;
	if ((contents == null) ||
	    ((length = contents.size()) == 0)) {
	    SAMLUtils.debug.message("Request: empty content.");
	    throw new SAMLRequesterException(
			SAMLUtils.bundle.getString("wrongInput"));
	}
	for (i = 0; i < length; i++) {
	    Object temp = contents.get(i);
	    if (temp instanceof AuthenticationQuery) {
		// make sure this is the first one on the list
		if ((contentType != NOT_SUPPORTED) ||
		    // and make sure there is no other elements on the list
		    (i != (length - 1))) {
	    	    if (SAMLUtils.debug.messageEnabled()) {
			SAMLUtils.debug.message("Request: should contain only"
				+ " one AuthenticationQuery.");
	    	    }
		    throw new SAMLRequesterException(
			SAMLUtils.bundle.getString("wrongInput"));
		}
		contentType = AUTHENTICATION_QUERY;
		query = (AuthenticationQuery) temp;
	    } else if (temp instanceof AuthorizationDecisionQuery) {
		// make sure this is the first one on the list
		if ((contentType != NOT_SUPPORTED) ||
		    // and make sure there is no other elements on the list
		    (i != (length - 1))) {
	    	    if (SAMLUtils.debug.messageEnabled()) {
			SAMLUtils.debug.message("Request: should contain only"
				+ " one AuthorizationDecisionQuery.");
	    	    }
		    throw new SAMLRequesterException(
			SAMLUtils.bundle.getString("wrongInput"));
		}
		contentType = AUTHORIZATION_DECISION_QUERY;
		query = (AuthorizationDecisionQuery) temp;
	    } else if (temp instanceof AttributeQuery) {
		// make sure this is the first one on the list
		if ((contentType != NOT_SUPPORTED) ||
		    // and make sure there is no other elements on the list
		    (i != (length - 1))) {
	    	    if (SAMLUtils.debug.messageEnabled()) {
			SAMLUtils.debug.message("Request: should contain only"
				+ " one AttributeQuery.");
	    	    }
		    throw new SAMLRequesterException(
			SAMLUtils.bundle.getString("wrongInput"));
		}
		contentType = ATTRIBUTE_QUERY;
		query = (AttributeQuery) temp;
	    } else if (temp instanceof AssertionIDReference) {
		// if this is not the first element on the list , and if the
		// the previously assigned elements are not AssertionIDReference
		if ((contentType != NOT_SUPPORTED) &&
		    (contentType != ASSERTION_ID_REFERENCE)) {
	    	    if (SAMLUtils.debug.messageEnabled()) {
			SAMLUtils.debug.message("Request: should contain"
				+ " one or more AssertionIDReference.");
	    	    }
		    throw new SAMLRequesterException(
			SAMLUtils.bundle.getString("wrongInput"));
		}
		contentType = ASSERTION_ID_REFERENCE;
		if (assertionIDRefs == Collections.EMPTY_LIST) {
		    assertionIDRefs = new ArrayList();
		}
		assertionIDRefs.add((AssertionIDReference) temp);
	    } else if (temp instanceof AssertionArtifact) {
		// if this is not the first element on the list, and if the
		// previously assigned elements are not AssertionArtifact:
		if ((contentType != NOT_SUPPORTED) &&
		    (contentType != ASSERTION_ARTIFACT)) {
	    	    if (SAMLUtils.debug.messageEnabled()) {
			SAMLUtils.debug.message("Request: should contain "
				+ " one or more AssertionArtifact.");
	    	    }
		    throw new SAMLRequesterException(
			SAMLUtils.bundle.getString("wrongInput"));
		}
		contentType = ASSERTION_ARTIFACT;
		if (artifacts == Collections.EMPTY_LIST) {
		    artifacts = new ArrayList();
		}
		artifacts.add((AssertionArtifact) temp);
	    } else { // everything else
		SAMLUtils.debug.message("Request: wrong input.");
		throw new SAMLRequesterException(
			SAMLUtils.bundle.getString("wrongInput"));
	    }
	}
    }

    /**
     * This constructor shall only be used at the client side to construct a
     * Request object.
     * NOTE: The content here is just the body for the Request. The 
     * constructor will add <code>MajorVersion</code>,
     * <code>MinorVersion</code>, etc. to form a complete Request.
     *
     * @param requestId If it's null, the constructor will create one.
     * @param query A Query to be included in the Request.
     * @throws SAMLException if an error occurs.
     */
    public Request(String requestId, Query query) throws SAMLException {
	if ((requestId != null) && (requestId.length() != 0)) {
	    requestID = requestId;
	} else {
	    // random generate one 
	    requestID = SAMLUtils.generateID();
	    if (requestID == null) {
		SAMLUtils.debug.error("Request: couldn't generate RequestID.");
		throw new SAMLRequesterException(
		    SAMLUtils.bundle.getString("errorGenerateID"));
	    }
	}

	if (query == null) {
	    SAMLUtils.debug.message("Request: empty content.");
	    throw new SAMLRequesterException(
			SAMLUtils.bundle.getString("nullInput"));
	}

	if (query instanceof AuthenticationQuery) {
	    contentType = AUTHENTICATION_QUERY;
	} else if (query instanceof AuthorizationDecisionQuery) {
	    contentType = AUTHORIZATION_DECISION_QUERY;
	} else if (query instanceof AttributeQuery) {
	    contentType = ATTRIBUTE_QUERY;
	} else {
	    if (SAMLUtils.debug.messageEnabled()) {
		SAMLUtils.debug.message("Request: this type of query is not"
				+ " supported.");
	    }
	    throw new SAMLResponderException(
			SAMLUtils.bundle.getString("queryNotSupported"));
	}
	this.query = query;
		issueInstant = newDate();
    }

    /**
     * This constructor shall only be used at the client side to construct a
     * Request object.
     * NOTE: The content here is just the body for the Request. The 
     * constructor will add <code>MajorVersion</code>,
     * <code>MinorVersion</code>, etc. to form a complete Request.
     *
     * @param requestId If it's null, the constructor will create one.
     * @param contents A List of objects that are the contents of Request that
     *	      the client wants to send to the server. It could be an
     *        <code>AuthenticationQuery</code>,
     *        <code>AuthorizationDecisionQuery</code>, 
     *        <code>AttributeQuery</code>, 1 or more
     *        <code>AssertionIDReference</code>, or 1 or more of
     *        <code>AssertionArtifact</code>.
     * @throws SAMLException if an error occurs.
     */
    public Request(String requestId, List contents) throws SAMLException {
	if (requestId != null) {
	    requestID = requestId;
	} else {
	    // random generate one
	    requestID = SAMLUtils.generateID();
	    if (requestID == null) {
		throw new SAMLRequesterException(
		    SAMLUtils.bundle.getString("errorGenerateID"));
	    }
	}
	parseContents(contents);
		issueInstant = newDate();
    }

    /**
     * This method shall only be used at the server side to reconstruct
     * a Request object based on the XML document received from client.
     * The schema of this XML document is described above.
     *
     * @param xml The Request XML String.
     *		NOTE: this is a complete SAML request XML string with
     *		<code>RequestID</code>, <code>MajorVersion</code>, etc.
     * @return Request object
     * @exception SAMLException if an error occurs.
     */
    public static Request parseXML(String xml) throws SAMLException {
	// parse the xml string
	Document doc = XMLUtils.toDOMDocument(xml, SAMLUtils.debug);
	Element root = doc.getDocumentElement();

	return new Request(root);
    }

    /**
     * Constructor.
     *
     * @param root <code>Request</code> element
     * @throws SAMLException
     */
    public Request(Element root) throws SAMLException {
	// Make sure this is a Request
	String tag = null;
	if (root == null) {
	    SAMLUtils.debug.message("Request(Element): null input.");
	    throw new SAMLRequesterException(
				SAMLUtils.bundle.getString("nullInput"));
	}
	if (((tag = root.getLocalName()) == null) ||
	    (!tag.equals("Request"))) {
	    SAMLUtils.debug.message("Request(Element): wrong input");
	    throw new SAMLRequesterException(
				SAMLUtils.bundle.getString("wrongInput"));
	}

	List signs = XMLUtils.getElementsByTagNameNS1(root,
					SAMLConstants.XMLSIG_NAMESPACE_URI,
					SAMLConstants.XMLSIG_ELEMENT_NAME);
	int signsSize = signs.size();
	if (signsSize == 1) {
	    XMLSignatureManager manager = XMLSignatureManager.getInstance();
            valid = manager.verifyXMLSignature(root,
                REQUEST_ID_ATTRIBUTE, null);
	    if (!valid) {
		if (SAMLUtils.debug.messageEnabled()) {
		    SAMLUtils.debug.message("Request(Element): couldn't verify"
			+ " Request's signature.");
		}
	    }
	    xmlString = XMLUtils.print(root);
	    signed = true;
	} else if (signsSize != 0) {
	    if (SAMLUtils.debug.messageEnabled()) {
		SAMLUtils.debug.message("Request(Element): included more than"
		    + " one Signature element.");
	    }
	    throw new SAMLRequesterException(
		SAMLUtils.bundle.getString("moreElement"));
	}

	// Attribute RequestID
	requestID = root.getAttribute("RequestID");
	if ((requestID == null) || (requestID.length() == 0)) {
	    if (SAMLUtils.debug.messageEnabled()) {
		SAMLUtils.debug.message("Request(Element): Request doesn't "
					+ "have a RequestID.");
	    }
	    throw new SAMLRequesterException(
				SAMLUtils.bundle.getString("missingAttribute"));
	}

	// Attribute MajorVersion
	parseMajorVersion(requestID, root.getAttribute("MajorVersion"));

	// Attribute MinorVersion
	parseMinorVersion(requestID, root.getAttribute("MinorVersion"));

	// Attribute IssueInstant
	String instantString = root.getAttribute("IssueInstant");
	if ((instantString == null) || (instantString.length() == 0)) {
	    SAMLUtils.debug.message("Request(Element): missing IssueInstant");
	    throw new SAMLRequesterException(
		SAMLUtils.bundle.getString("missingAttribute"));
	} else {
	    try {
		issueInstant = DateUtils.stringToDate(instantString);
	    } catch (ParseException e) {
		SAMLUtils.debug.message(
		    "Request(Element): could not parse IssueInstant", e);
		throw new SAMLRequesterException(SAMLUtils.bundle.getString(
			"wrongInput"));
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
			if (SAMLUtils.debug.messageEnabled()) {
			    SAMLUtils.debug.message("Request(Element): wrong "
				+ "RespondWith value.");
			}
			throw new SAMLRequesterException(
			    SAMLUtils.bundle.getString("wrongInput"));
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
			if (SAMLUtils.debug.messageEnabled()) {
			    SAMLUtils.debug.message("Request(Element): should"
				+ "contain only one AuthenticationQuery.");
			} 
			throw new SAMLRequesterException(
			    SAMLUtils.bundle.getString("wrongInput"));
		    }
		    contentType = AUTHENTICATION_QUERY;
		    query = new AuthenticationQuery((Element) child);
	        } else if (nodeName.equals("AuthorizationDecisionQuery")) {
		    // make sure content is not assigned already
		    if (contentType != NOT_SUPPORTED) {
			if (SAMLUtils.debug.messageEnabled()) {
			    SAMLUtils.debug.message("Request(Element): should"
				+ "contain only one "
				+ "AuthorizationDecisionQuery.");
			} 
			throw new SAMLRequesterException(
			    SAMLUtils.bundle.getString("wrongInput"));
		    }
		    contentType = AUTHORIZATION_DECISION_QUERY;
		    query = new AuthorizationDecisionQuery((Element) child);
		} else if (nodeName.equals("AttributeQuery")) {
		    // make sure content is not assigned already
		    if (contentType != NOT_SUPPORTED) {
			if (SAMLUtils.debug.messageEnabled()) {
			    SAMLUtils.debug.message("Request(Element): should"
				+ "contain only one AttributeQuery.");
			} 
			throw new SAMLRequesterException(
			    SAMLUtils.bundle.getString("wrongInput"));
		    }
		    contentType = ATTRIBUTE_QUERY;
		    query = new AttributeQuery((Element) child);
		} else if (nodeName.equals("AssertionIDReference")) {
		    // make sure the content has no other elements assigned
		    if ((contentType != NOT_SUPPORTED) &&
			(contentType != ASSERTION_ID_REFERENCE)) {
			if (SAMLUtils.debug.messageEnabled()) {
			    SAMLUtils.debug.message("Request(Element): "
				+ "contained mixed contents.");
			} 
			throw new SAMLRequesterException(
			    SAMLUtils.bundle.getString("wrongInput"));
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
			if (SAMLUtils.debug.messageEnabled()) {
			    SAMLUtils.debug.message("Request(Element): "
				+ "contained mixed contents.");
			} 
			throw new SAMLRequesterException(
			    SAMLUtils.bundle.getString("wrongInput"));
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
		    if (SAMLUtils.debug.messageEnabled()) {
			SAMLUtils.debug.message("Request(Element): invalid"
				+ " node" + nodeName);
		    }
		    throw new SAMLRequesterException(
			SAMLUtils.bundle.getString("wrongInput"));
		} // check nodeName
	    } // if nodeName != null
	} // done for the nodelist loop

	if (contentType == NOT_SUPPORTED) {
	    SAMLUtils.debug.message("Request: empty content.");
	    throw new SAMLRequesterException(
			SAMLUtils.bundle.getString("wrongInput"));
	}
    }

    /**
     * Parse the input and set the majorVersion accordingly.
     * @param majorVer a String representing the MajorVersion to be set.
     * @exception SAMLException when the version mismatchs.
     */
    private void parseMajorVersion(String reqID, String majorVer)
                                   throws SAMLException {
	try {
	    majorVersion = Integer.parseInt(majorVer);
	} catch (NumberFormatException e) {
	    if (SAMLUtils.debug.messageEnabled()) {
		SAMLUtils.debug.message("Request(Element): invalid "
			+ "MajorVersion", e);
	    }
	    throw new SAMLRequesterException(
		SAMLUtils.bundle.getString("wrongInput"));
	}

	if (majorVersion != SAMLConstants.PROTOCOL_MAJOR_VERSION) { 
	    if (majorVersion > SAMLConstants.PROTOCOL_MAJOR_VERSION) {
		if (SAMLUtils.debug.messageEnabled()) {
		    SAMLUtils.debug.message("Request(Element):MajorVersion of "
				+ "the Request is too high.");
		}
		throw new SAMLRequestVersionTooHighException(reqID + "|"+
			SAMLUtils.bundle.getString("requestVersionTooHigh"));
	    } else {
		if (SAMLUtils.debug.messageEnabled()) {
		    SAMLUtils.debug.message("Request(Element):MajorVersion of "
				+ "the Request is too low.");
		}
		throw new SAMLRequestVersionTooLowException(reqID + "|"+
			SAMLUtils.bundle.getString("requestVersionTooLow"));
	    }
	}

    }

    /**
     * Parse the input and set the minorVersion accordingly.
     * @param minorVer a String representing the MinorVersion to be set.
     * @exception SAMLException when the version mismatchs.
     */
    private void parseMinorVersion(String reqID, String minorVer) 
                 throws SAMLException {
	try {
	    minorVersion = Integer.parseInt(minorVer);
	} catch (NumberFormatException e) {
	    if (SAMLUtils.debug.messageEnabled()) {
		SAMLUtils.debug.message("Request(Element): invalid "
			+ "MinorVersion", e);
	    }
	    throw new SAMLRequesterException(
		SAMLUtils.bundle.getString("wrongInput"));
	}
    
        if (minorVersion > SAMLConstants.PROTOCOL_MINOR_VERSION_ONE) {
            if (SAMLUtils.debug.messageEnabled()) {
	        SAMLUtils.debug.message("Request(Element): MinorVersion"
				+ " of the Request is too high.");
            }
            throw new SAMLRequestVersionTooHighException(reqID + "|"+
			 SAMLUtils.bundle.getString("requestVersionTooHigh"));    
        } else if (minorVersion < SAMLConstants.PROTOCOL_MINOR_VERSION_ZERO) { 
            if (SAMLUtils.debug.messageEnabled()) {
	        SAMLUtils.debug.message("Request(Element): MinorVersion"
				+ " of the Request is too low.");
            }
            throw new SAMLRequestVersionTooLowException( reqID + "|"+
			 SAMLUtils.bundle.getString("requestVersionTooLow"));
        }
    }

    /**
     * This method parses the Query or SubjectQuery represented by a DOM tree
     * Node. It then checks and sets data members if it is a supported query,
     * such as AuthenticationQuery, AttributeQeury, or 
     * <code>AuthorizationDecisionQuery</code>.
     * @param child A DOM Node to be parsed.
     * @exception SAMLException if it's not a supported query.
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
			if (SAMLUtils.debug.messageEnabled()) {
			    SAMLUtils.debug.message("Request(Element): should"
				+ " contain only one AuthenticationQuery.");
			} 
		        throw new SAMLRequesterException(
			    SAMLUtils.bundle.getString("wrongInput"));
		    }
		    contentType = AUTHENTICATION_QUERY;
		    query = new AuthenticationQuery((Element) child);
		} else if (attrValue.equals(
					"AuthorizationDecisionQueryType")) {
		    if (contentType != NOT_SUPPORTED) {
		        if (SAMLUtils.debug.messageEnabled()) {
			    SAMLUtils.debug.message("Request(Element): should "
				+ "contain one AuthorizationDecisionQuery.");
			}
			throw new SAMLRequesterException(SAMLUtils.
				bundle.getString("wrongInput"));
		    }
		    contentType = AUTHORIZATION_DECISION_QUERY;
		    query = new AuthorizationDecisionQuery((Element) child);
		} else if (attrValue.equals("AttributeQueryType")) {
		    if (contentType != NOT_SUPPORTED) {
		        if (SAMLUtils.debug.messageEnabled()) {
			    SAMLUtils.debug.message("Request(Element): should "
				+ "contain one AttributeQuery.");
			}
			throw new SAMLRequesterException(SAMLUtils.
				bundle.getString("wrongInput"));
		    }
		    contentType = ATTRIBUTE_QUERY;
		    query = new AttributeQuery((Element) child);
		} else {
		    if (SAMLUtils.debug.messageEnabled()) {
			SAMLUtils.debug.message("Request(Element): This type of"
				+ " " + attrName + " is not supported.");
		    }
		    throw new SAMLResponderException(
			SAMLUtils.bundle.getString("queryNotSupported"));
		} // check typevalue
		found = true;
		break;
	    } // if found type attribute
	} // end attribute for loop
	// if not found type
	if (!found) {
	    if (SAMLUtils.debug.messageEnabled()) {
		SAMLUtils.debug.message("Request(Element): missing"
			+ " xsi:type definition in " + child.getLocalName());
	    }
	    throw new SAMLRequesterException(
		SAMLUtils.bundle.getString("wrongInput"));
	}
    }

    /**
     * Gets the query of the Request.
     *
     * @return the query included in the request; or null if the
     *         <code>contentType</code> of the request is not
     *         <code>AUTHENTICATION_QUERY</code>,
     *	       <code>AUTHORIZATION_DECISION_QUERY</code>, or
     *         <code>ATTRIBUTE_QUERY</code>.
     */
    public Query getQuery() {
	return query;
    }

    /**
     * Gets the <code>AssertionIDReference</code>(s) of the Request.
     * @return a List of <code>AssertionIDReference</code>s included in the
     *         request; or <code>Collections.EMPTY_LIST</code> if the
     *         <code>contentType</code> of the request is not
     *         <code>ASSERTION_ID_REFERENCE</code>.
     */
    public List getAssertionIDReference() {
	return assertionIDRefs;
    }

    /**
     * Gets the <code>AssertionArtifact</code>(s) of the Request.
     * @return a List of <code>AssertionArtifact</code>s included in the
     *         request; or <code>Collections.EMPTY_LIST</code> if the
     *         <code>contentType</code> of the request is not
     *         <code>ASSERTION_ARTIFACT</code>.
     */
    public List getAssertionArtifact() {
	return artifacts;
    }

    /**
     * Returns the type of content this Request has.
     *
     * @return The type of the content. The possible values are defined in
     *         Request.
     */
    public int getContentType() {
	return contentType;
    }
    
    /**
     * Set the signature for the Response.
     *
     * @param elem <code>ds:Signature</code> element
     * @return true if the operation succeeds.
     */
    public boolean setSignature(Element elem) {
        signatureString = XMLUtils.print(elem); 
        return super.setSignature(elem); 
    }

    /**
     * This method translates the request to an XML document String based on
     * the Request schema described above.
     * NOTE: this is a complete SAML request XML string with
     * <code>RequestID</code>, <code>MajorVersion</code>, etc.
     *
     * @return An XML String representing the request.
     */
    public String toString() {
	return toString(true, true);
    }

    /**
     * Returns a String representation of the
     * <code>&lt;samlp:Request&gt;</code> element.
     *
     * @param includeNS Determines whether or not the namespace qualifier
     *        is prepended to the Element when converted
     * @param declareNS Determines whether or not the namespace is declared
     *        within the Element.
     * @return A string containing the valid XML for this element
     */
    public String toString(boolean includeNS, boolean declareNS) {
	return toString(includeNS, declareNS, false);
    }

    /**
     * Returns a String representation of the
     * <code>&lt;samlp:Request&gt;</code> element.
     *
     * @param includeNS Determines whether or not the namespace qualifier
     *        is prepended to the Element when converted
     * @param declareNS Determines whether or not the namespace is declared
     *        within the Element.
     * @param includeHeader Determines whether the output include the XML
     *	      declaration header.
     * @return A string containing the valid XML for this element
     */
    public String toString(boolean includeNS,
			boolean declareNS,
			boolean includeHeader)
    {
	if (signed && (xmlString != null)) {
	    return xmlString;
	}

	StringBuffer xml = new StringBuffer(300);
	if (includeHeader) {
	    xml.append("<?xml version=\"1.0\" encoding=\"").
		append(SAMLConstants.DEFAULT_ENCODING).append("\" ?>\n");
	}
	String prefix = "";
	String uri = "";
	if (includeNS) {
	    prefix = SAMLConstants.PROTOCOL_PREFIX;
	}
	if (declareNS) {
	    uri = SAMLConstants.PROTOCOL_NAMESPACE_STRING;
	}
	String instantString = DateUtils.toUTCDateFormat(issueInstant);

	xml.append("<").append(prefix).append("Request").append(uri).
	    append(" RequestID=\"").append(requestID).append("\"").
	    append(" MajorVersion=\"").append(majorVersion).append("\"").
	    append(" MinorVersion=\"").append(minorVersion).append("\"").
	    append(" IssueInstant=\"").append(instantString).append("\"").
	    append(">\n");
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
			SAMLUtils.debug.error("Request.toString: ", e);
			xml.append(respondWith);
		    }
		}
		xml.append("</").append(prefix).append("RespondWith>\n");
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
	    xml.append(((AuthenticationQuery)query).toString(includeNS, false));
	    break;
	case AUTHORIZATION_DECISION_QUERY:
	    xml.append(((AuthorizationDecisionQuery)query).toString(includeNS,
								    false));
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

	xml.append("</").append(prefix).append("Request>\n");
	return xml.toString();
    }
}
