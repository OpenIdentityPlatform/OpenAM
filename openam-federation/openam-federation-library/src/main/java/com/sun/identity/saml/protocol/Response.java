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
 * $Id: Response.java,v 1.3 2009/02/13 04:05:10 bina Exp $
 *
 */

/**
 * Portions Copyrighted 2014 ForgeRock AS
 */
package com.sun.identity.saml.protocol;

import com.sun.identity.common.SystemConfigurationUtil;
import com.sun.identity.shared.xml.XMLUtils; 

import com.sun.identity.shared.DateUtils;

import com.sun.identity.saml.assertion.Assertion;

import com.sun.identity.saml.common.SAMLConstants;
import com.sun.identity.saml.common.SAMLException;
import com.sun.identity.saml.common.SAMLRequesterException;
import com.sun.identity.saml.common.SAMLRequestVersionTooHighException;
import com.sun.identity.saml.common.SAMLRequestVersionTooLowException;
import com.sun.identity.saml.common.SAMLResponderException;
import com.sun.identity.saml.common.SAMLUtils;
import com.sun.identity.saml.common.SAMLVersionMismatchException;

import com.sun.identity.saml.xmlsig.XMLSignatureManager;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import java.text.ParseException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * This <code>Response</code> class represents a Response XML document.
 * The schema of Response is defined as the following:
 *
 * @supported.all.api
 */
public class Response extends AbstractResponse {

    protected Status	status		= null;
    protected List	assertions	= Collections.EMPTY_LIST;
    protected String	xmlString	= null;
    protected String	signatureString	= null;
    protected String    issuer 		= null; 

    // Response ID attribute name
    private static final String RESPONSE_ID_ATTRIBUTE = "ResponseID";

    /** default constructor */
    protected Response() {}
    
     /**
     * Return whether the signature on the object is valid or not.
     * @return true if the signature on the object is valid; false otherwise.
     */
    public boolean isSignatureValid() {
        if (signed & ! validationDone) {
	    valid = SAMLUtils.checkSignatureValid(
	    	xmlString, RESPONSE_ID_ATTRIBUTE, issuer); 
	     	
            validationDone = true;
        }
	return valid; 
    }

    /**
     * Method that signs the Response.
     *
     * @exception SAMLException if could not sign the Response.
     */
    public void signXML() throws SAMLException {
	if (signed) {
	    if (SAMLUtils.debug.messageEnabled()) {
		SAMLUtils.debug.message("Response.signXML: the response is "
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
		SAMLUtils.debug.message("Response.signXML: couldn't obtain "
		    + "this site's cert alias.");
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
                RESPONSE_ID_ATTRIBUTE, getResponseID(), true, null);
            signatureString = XMLUtils.print(signature);
        }
	signed = true;
	xmlString = this.toString(true, true);
    }

    private void buildResponse(String responseID,
		    String inResponseTo,
		    Status status,
		    String recipient,
		    List contents) throws SAMLException
    {
	if ((responseID == null) || (responseID.length() == 0)) {
	    // generate one
	    this.responseID = SAMLUtils.generateID();
	    if (this.responseID == null) {
		throw new SAMLRequesterException(
			SAMLUtils.bundle.getString("errorGenerateID"));
	    }
	} else {
	    this.responseID = responseID;
	}

	this.inResponseTo = inResponseTo;

	this.recipient = recipient;

	issueInstant = new Date();

	if (status == null) {
	    SAMLUtils.debug.message("Response: missing <Status>.");
	    throw new SAMLRequesterException(
			SAMLUtils.bundle.getString("missingElement"));
	}
	this.status = status;

	if ((contents != null) &&
	    (contents != Collections.EMPTY_LIST)) {
	    int length = contents.size();
	    for (int i = 0; i < length; i++) {
		Object temp = contents.get(i);
		if (!(temp instanceof Assertion)) {
		    if (SAMLUtils.debug.messageEnabled()) {
			SAMLUtils.debug.message("Response: Wrong input "
				+ "for Assertion.");
		    }
		    throw new SAMLRequesterException(
				SAMLUtils.bundle.getString("wrongInput"));
		}
	    }
	    assertions = contents;
	}
    }

    /**
     * This constructor shall only be used at the server side to construct
     * a Response object.
     * NOTE: The content here is just the body for the Response. The
     * constructor will add the unique <code>ResponseID</code>,
     * <code>MajorVersion</code>, etc. to form a complete Response object.
     *
     * @param responseID If it's null, the constructor will create one.
     * @param inResponseTo the <code>RequestID</code> that this response is
     *        corresponding. It could be null or empty string "".
     * @param status The status of the response.
     * @param contents A List of Assertions that are the content of the
     *	      Response. It could be null when there is no Assertion.
     * @throws SAMLException if error occurs.
     */
    public Response(String responseID,
		    String inResponseTo,
		    Status status,
		    List contents) throws SAMLException
    {
	buildResponse(responseID, inResponseTo, status, null, contents);
    }

    /**
     * This constructor shall only be used at the server side to construct
     * a Response object.
     * NOTE: The content here is just the body for the Response. The
     * constructor will add the unique <code>ResponseID</code>,
     * <code>MajorVersion</code>, etc. to form a complete Response object.
     *
     * @param responseID If it's null, the constructor will create one.
     * @param inResponseTo the <code>RequestID</code> that this response is
     *        corresponding. It could be null or empty string "".
     * @param status The status of the response.
     * @param recipient The intended recipient of the response. It could be
     *	      null or empty string since it's optional.
     * @param contents A List of Assertions that are the content of the
     *        Response. It could be null when there is no Assertion.
     * @throws SAMLException if error occurs.
     */
    public Response(String responseID,
		    String inResponseTo,
		    Status status,
		    String recipient,
		    List contents) throws SAMLException
    {
	buildResponse(responseID, inResponseTo, status, recipient, contents);
    }

    /**
     * This constructor shall only be used at the server side to construct
     * a Response object.
     * NOTE: The content here is just the body for the Response. The
     * constructor will add the unique <code>ResponseID</code>,
     * <code>MajorVersion</code>, etc. to form a complete Response object.
     *
     * @param responseID If it's null, the constructor will create one.
     * @param status The status of the response.
     * @param recipient The intended recipient of the response. It could be
     *	      null or empty string since it's optional.
     * @param contents A List of Assertions that are the content of the
     *	      Response. It could be null when there is no Assertion.
     * @throws SAMLException if error occurs.
     */
    public Response(String responseID,
		    Status status,
		    String recipient,
		    List contents) throws SAMLException
    {
	buildResponse(responseID, null, status, recipient, contents);
    }

    /**
     * This constructor shall only be used at the server side to construct
     * a Response object.
     * NOTE: The content here is just the body for the Response. The
     * constructor will add the unique <code>ResponseID</code>,
     * <code>MajorVersion</code>, etc. to form a complete Response object.
     *
     * @param responseID If it's null, the constructor will create one.
     * @param status The status of the response.
     * @param contents A List of Assertions that are the content of the
     *	      Response. It could be null when there is no Assertion.
     * @throws SAMLException if error occurs.
     */
    public Response(String responseID,
		    Status status,
		    List contents) throws SAMLException
    {
	buildResponse(responseID, null, status, null, contents);
    }

    /**
     * Returns Response object based on the XML document received from server.
     * This method is used primarily at the client side. The schema of the XML
     * document is describe above.
     *
     * @param xml The Response XML document String.
     *		NOTE: this is a complete SAML response XML string with
     *		<code>ResponseID</code>, <code>MajorVersion</code>, etc.
     * @return Response object based on the XML document received from server.
     * @exception SAMLException if XML parsing failed
     */
    public static Response parseXML(String xml) throws SAMLException {
	// parse the xml string
	Document doc = XMLUtils.toDOMDocument(xml, SAMLUtils.debug);
	Element root = doc.getDocumentElement();

	return new Response(root);
    }

    /**
     * Returns Response object based on the XML document received from server.
     * This method is used primarily at the client side. The schema of the XML
     * document is describe above.
     *
     * @param is The Response XML <code>InputStream</code>.
     *         NOTE: The <code>InputStream</code> contains a complete 
     *         SAML response with
     *         <code>ResponseID</code>, <code>MajorVersion</code>, etc.
     * @return Response object based on the XML document received from server.
     * @exception SAMLException if XML parsing failed
     */
    public static Response parseXML(InputStream is) throws SAMLException {
	Document doc = XMLUtils.toDOMDocument(is, SAMLUtils.debug);
	Element root = doc.getDocumentElement();

	return new Response(root);
    }

    /**
     * Constructor.
     *
     * @param root <code>Response</code> element
     * @throws SAMLException if error occurs.
     */
    public Response(Element root) throws SAMLException {
	// Make sure this is a Response
	if (root == null) {
	    SAMLUtils.debug.message("Response(Element): null input.");
	    throw new SAMLRequesterException(
		SAMLUtils.bundle.getString("nullInput"));
	}
	String tag = null;
	if (((tag = root.getLocalName()) == null) ||
	    (!tag.equals("Response"))) {
	    SAMLUtils.debug.message("Response(Element): wrong input.");
	    throw new SAMLRequesterException(
		SAMLUtils.bundle.getString("wrongInput"));
	}

	List signs = XMLUtils.getElementsByTagNameNS1(root,
					SAMLConstants.XMLSIG_NAMESPACE_URI,
					SAMLConstants.XMLSIG_ELEMENT_NAME);
	int signsSize = signs.size();
	if (signsSize == 1) {
	    xmlString = XMLUtils.print(root);
	    signed = true;
	} else if (signsSize != 0) {
	    if (SAMLUtils.debug.messageEnabled()) {
		SAMLUtils.debug.message("Response(Element): included more than"
		    + " one Signature element.");
	    }
	    throw new SAMLRequesterException(
		SAMLUtils.bundle.getString("moreElement"));
	}

	// Attribute ResponseID
	responseID = root.getAttribute("ResponseID");
	if ((responseID == null) || (responseID.length() == 0)) {
	    if (SAMLUtils.debug.messageEnabled()) {
		SAMLUtils.debug.message("Response.parseXML: "
				+ "Reponse doesn't have ResponseID.");
	    }
	    throw new SAMLRequesterException(
		SAMLUtils.bundle.getString("missingAttribute"));
	}

	// Attribute InResponseTo
	if (root.hasAttribute("InResponseTo")) {
	    inResponseTo = root.getAttribute("InResponseTo");
	}

	// Attribute MajorVersion
	parseMajorVersion(root.getAttribute("MajorVersion"));

	parseMinorVersion(root.getAttribute("MinorVersion"));

	if (root.hasAttribute("Recipient")) {
	    recipient = root.getAttribute("Recipient");
	}

	// Attribute IssueInstant
	String instantString = root.getAttribute("IssueInstant");
	if ((instantString == null) || (instantString.length() == 0)) {
	    SAMLUtils.debug.message("Response(Element): missing IssueInstant");
	    throw new SAMLRequesterException(
		SAMLUtils.bundle.getString("missingAttribute"));
	} else {
	    try {
		issueInstant = DateUtils.stringToDate(instantString);
	    } catch (ParseException e) {
		SAMLUtils.debug.message(
		    "Resposne(Element): could not parse IssueInstant", e);
		throw new SAMLRequesterException(SAMLUtils.bundle.getString(
			"wrongInput"));
	    }
	}

	NodeList nl = root.getChildNodes();
	Node child;
	String childName;
	int length = nl.getLength();
	for (int i = 0; i < length; i++) {
	    child = nl.item(i);
	    if ((childName = child.getLocalName()) != null) {
		if (childName.equals("Signature")) {
		    signature = (Element) child;
		} else if (childName.equals("Status")) {
		    if (status != null) {
			if (SAMLUtils.debug.messageEnabled()) {
			    SAMLUtils.debug.message("Response: included more"
				+ " than one <Status>");
			}
			throw new SAMLRequesterException(
			    SAMLUtils.bundle.getString("moreElement"));
		    }
		    status = new Status((Element) child);
		} else if (childName.equals("Assertion")) {
                    if (assertions == Collections.EMPTY_LIST) {
		        assertions = new ArrayList();
		    }
                    Element canoEle = SAMLUtils.getCanonicalElement(child);
                    if (canoEle == null) {
                        throw new SAMLRequesterException(
                            SAMLUtils.bundle.getString("errorCanonical"));
                    }

                    Assertion oneAssertion= new Assertion(canoEle);
		    issuer = oneAssertion.getIssuer(); 
		    assertions.add(oneAssertion);
		} else {
		    if (SAMLUtils.debug.messageEnabled()) {
			SAMLUtils.debug.message("Response: included wrong "
			    + "element:" + childName);
		    }
		    throw new SAMLRequesterException(
			SAMLUtils.bundle.getString("wrongInput"));
		}
	    } // end if childName != null
	} // end for loop

	if (status == null) {
	    SAMLUtils.debug.message("Response: missing element <Status>.");
	    throw new SAMLRequesterException(
		SAMLUtils.bundle.getString("oneElement"));
	}
    }

    /**
     * Parse the input and set the majorVersion accordingly.
     * @param majorVer a String representing the MajorVersion to be set.
     * @exception SAMLException when the version mismatchs.
     */
    private void parseMajorVersion(String majorVer) throws SAMLException {
	try {
	    majorVersion = Integer.parseInt(majorVer);
	} catch (NumberFormatException e) {
	    if (SAMLUtils.debug.messageEnabled()) {
		SAMLUtils.debug.message("Response(Element): invalid "
		    + "MajorVersion", e);
	    }
	    throw new SAMLRequesterException(
		SAMLUtils.bundle.getString("wrongInput"));
	}

	if (majorVersion != SAMLConstants.PROTOCOL_MAJOR_VERSION) {
	    if (majorVersion > SAMLConstants.PROTOCOL_MAJOR_VERSION) {
		if (SAMLUtils.debug.messageEnabled()) {
		    SAMLUtils.debug.message("Response(Element):MajorVersion of"
			+ " the Response is too high.");
		}
		throw new SAMLVersionMismatchException(
		    SAMLUtils.bundle.getString("responseVersionTooHigh"));
	    } else {
		if (SAMLUtils.debug.messageEnabled()) {
		    SAMLUtils.debug.message("Response(Element):MajorVersion of"
			+ " the Response is too low.");
		}
		throw new SAMLVersionMismatchException(
		    SAMLUtils.bundle.getString("responseVersionTooLow"));
	    }
	}
    }

    /**
     * Parse the input and set the minorVersion accordingly.
     * @param minorVer a String representing the MinorVersion to be set.
     * @exception SAMLException when the version mismatchs.
     */
    private void parseMinorVersion(String minorVer) throws SAMLException {
	try {
	    minorVersion = Integer.parseInt(minorVer);
	} catch (NumberFormatException e) {
	    if (SAMLUtils.debug.messageEnabled()) {
		SAMLUtils.debug.message("Response(Element): invalid "
		    + "MinorVersion", e);
	    }
	    throw new SAMLRequesterException(
		SAMLUtils.bundle.getString("wrongInput"));
	}

	if (minorVersion > SAMLConstants.PROTOCOL_MINOR_VERSION_ONE) {
            if (SAMLUtils.debug.messageEnabled()) {
	        SAMLUtils.debug.message("Response(Element): MinorVersion"
				+ " of the Response is too high.");
            }
            throw new SAMLRequestVersionTooHighException(
			 SAMLUtils.bundle.getString("responseVersionTooHigh"));    
        } else if (minorVersion < SAMLConstants.PROTOCOL_MINOR_VERSION_ZERO) { 
            if (SAMLUtils.debug.messageEnabled()) {
	        SAMLUtils.debug.message("Response(Element): MinorVersion"
				+ " of the Response is too low.");
            }
            throw new SAMLRequestVersionTooLowException( 
			 SAMLUtils.bundle.getString("responseVersionTooLow"));
        }
    }

    /** 
     * This method returns the set of Assertions that is the content of
     * the response.
     * @return The set of Assertions that is the content of the response.
     *		It could be Collections.EMPTY_LIST when there is no Assertion
     *		in the response.
     */
    public List getAssertion() {
	return assertions;
    }

    /**
     * Add an assertion to the Response.
     * @param assertion The assertion to be added.
     * @return A boolean value: true if the operation is successful;
     *		false otherwise.
     */
    public boolean addAssertion(Assertion assertion) {
	if (signed) {
	    return false;
	}
	if (assertion == null) {
	    return false;
	}
	if ((assertions == null) || (assertions == Collections.EMPTY_LIST)) {
	    assertions = new ArrayList();
	}
	assertions.add(assertion);
	return true;
    }

    /**
     * Gets the Status of the Response.
     * @return The Status of the response.
     */
    public Status getStatus() {
	return status;
    }

    /**
     * Set the Status of the Response.
     *
     * @param status The Status of the Response to be set.
     * @return true if the operation is successful.
     */
    public boolean setStatus(Status status) {
	if (signed) {
	    return false;
	}
	if (status == null) {
	    return false;
	}
	this.status = status;
	return true;
    }

    /**
     * Set the signature for the Response.
     * @param elem ds:Signature element
     * @return A boolean value: true if the operation succeeds; false otherwise.
     */
    public boolean setSignature(Element elem) {
        signatureString = XMLUtils.print(elem); 
        return super.setSignature(elem); 
    }
    
    /**
     * This method translates the response to an XML document String based on
     * the Response schema described above.
     * @return An XML String representing the response. NOTE: this is a
     *		complete SAML response XML string with <code>ResponseID</code>,
     *		<code>MajorVersion</code>, etc.
     */
    public String toString() {
	return this.toString(true, true);
    }

    /**
     * Creates a String representation of the
     * <code>&lt;samlp:Response&gt;</code> element.
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
     * Creates a String representation of the
     * <code>&lt;samlp:Response&gt;</code> element.
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
	String uri = "";
	if (includeNS) {
	    prefix = SAMLConstants.PROTOCOL_PREFIX;
	}

	if (declareNS) {
	    uri = SAMLConstants.PROTOCOL_NAMESPACE_STRING;
	}

	String instantString = DateUtils.toUTCDateFormat(issueInstant);

	xml.append("<").append(prefix).append("Response").append(uri).
	    append(" ResponseID=\"").append(responseID).append("\"");
	if (inResponseTo != null) {
	    xml.append(" InResponseTo=\"").append(inResponseTo).append("\"");
	}
	xml.append(" MajorVersion=\"").append(majorVersion).append("\"").
	    append(" MinorVersion=\"").append(minorVersion).append("\"").
	    append(" IssueInstant=\"").append(instantString).append("\"");
	if (recipient != null) {
	    xml.append(" Recipient=\"").append(XMLUtils.escapeSpecialCharacters(recipient)).append("\"");
	}
	xml.append(">\n");

	if (signed) {
	    if (signatureString != null) {
		xml.append(signatureString);
	    } else if (signature != null) {
		signatureString = XMLUtils.print(signature);
		xml.append(signatureString);
	    }
	}

	xml.append(status.toString(includeNS, false));
	if ((assertions != null) && (assertions != Collections.EMPTY_LIST)) {
	    Iterator j = assertions.iterator();
	    while (j.hasNext()) {
		xml.append(((Assertion) j.next()).toString(true, true));
	    }
	}

	xml.append("</").append(prefix).append("Response>\n");
	return xml.toString();
    }
}
