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
 * $Id: Status.java,v 1.2 2008/06/25 05:47:37 qcheng Exp $
 *
 */


package com.sun.identity.saml.protocol;

import com.sun.identity.shared.xml.XMLUtils; 

import com.sun.identity.saml.common.SAMLConstants;
import com.sun.identity.saml.common.SAMLException;
import com.sun.identity.saml.common.SAMLRequesterException;
import com.sun.identity.saml.common.SAMLUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * This class represents the Status element. It corresponds to 
 * <code>&lt;samlp:StatusType&gt;</code> in SAML protocol schema.
 *
 * @supported.all.api
 */
public class Status {

    private StatusCode statusCode 	= null;
    private String statusMessage	= null;
    private Element statusDetail	= null;
         
    /**
     * This is the default constructor of Status.
     */
    Status() {
    }
  
    /**
     * This constructor is used to construct a Status from a DOM element.      
     * @param status An DOM Element that's rooted by &lt;Status&gt;.
     * @exception SAMLException when an error occurs.
     */
    public Status(Element status) throws SAMLException {
	String tag = null;
	if (status == null) {
	    SAMLUtils.debug.message("Status: null input.");
	    throw new SAMLRequesterException(
			SAMLUtils.bundle.getString("nullInput"));
	}
	if (((tag = status.getLocalName()) == null) ||
	    (!tag.equals("Status"))) {
	    SAMLUtils.debug.message("Status: wrong input.");
	    throw new SAMLRequesterException(
			SAMLUtils.bundle.getString("wrongInput"));
	}

	NodeList nl = status.getChildNodes();
	Node child;
	String childName;
	String message;
	int length = nl.getLength();
	for (int k = 0; k < length; k++) {
	    child = nl.item(k);
	    if ((childName = child.getLocalName()) != null) {
		if (childName.equals("StatusCode")) {
		    if (statusCode != null) {
			if (SAMLUtils.debug.messageEnabled()) {
			    SAMLUtils.debug.message("Status: contained more"
				+ " than one <StatusCode>");
			}
			throw new SAMLRequesterException(
			    SAMLUtils.bundle.getString("moreElement"));
		    }
		    statusCode = new StatusCode((Element) child);
		} else if (childName.equals("StatusMessage")) {
		    message = XMLUtils.getElementValue((Element) child);
		    if ((message == null) ||
			(message.length() == 0)) {
			SAMLUtils.debug.message("Status: Empty StatusMessage.");
			throw new SAMLRequesterException(
			    SAMLUtils.bundle.getString("emptyElement"));
		    }
		    if (statusMessage != null) {
			if (SAMLUtils.debug.messageEnabled()) {
			    SAMLUtils.debug.message("Status: included more "
				+ "than one <StatusMessage>");
			}
			throw new SAMLRequesterException(
			    SAMLUtils.bundle.getString("moreElement"));
		    }
		    statusMessage = message;
		} else if (childName.equals("StatusDetail")) {
		    if (statusDetail != null) {
			if (SAMLUtils.debug.messageEnabled()) {
			    SAMLUtils.debug.message("Status: included more "
				+ "than one <StatusDetail>");
			}
			throw new SAMLRequesterException(
			    SAMLUtils.bundle.getString("moreElement"));
		    }
		    // set statusDetail
		    statusDetail = (Element) child;
		} else {
		    if (SAMLUtils.debug.messageEnabled()) {
			SAMLUtils.debug.message("Status: contained wrong"
			    + " element:" + childName);
		    }
		    throw new SAMLRequesterException(
			SAMLUtils.bundle.getString("wrongInput"));
		}
	    } // if childName != null
	} // end for loop

	if (statusCode == null) {
	    SAMLUtils.debug.message("Status: missing <StatusCode>.");
	    throw new SAMLRequesterException(
		SAMLUtils.bundle.getString("missingElement"));
	}
    }

    /**
     * Constructor.
     * @param code <code>StatusCode</code>.
     * @param message A String that is the <code>StatusMessage</code> of the
     * 	      response. It could be null when there is no
     *        <code>StatusMessage</code>.
     * @param detail A DOM tree element that is the <code>StatusDetail</code>
     *        of the response. It could be null when there is no 
     *        <code>StatusDetail</code>.
     * @throws SAMLException
     */
    public Status(StatusCode code, String message, Element detail) 
						throws SAMLException {
	if (code == null) {
	    SAMLUtils.debug.message("Status: null input.");
	    throw new SAMLRequesterException(
			SAMLUtils.bundle.getString("nullInput"));
	}
	statusCode = code;

	if ((message != null) && (message.length() == 0)) {
	    SAMLUtils.debug.message("Status: Empty StatusMessage.");
	    throw new SAMLRequesterException(
			    SAMLUtils.bundle.getString("emptyElement"));
	}
	statusMessage = message;

	statusDetail = detail;
    }

    /**
     * Constructs a Status object from a <code>StatusCode</code>.
     * @param code <code>StatusCode</code>.
     * @throws SAMLException
     */
    public Status(StatusCode code) throws SAMLException {
	if (code == null) {
	    SAMLUtils.debug.message("Status: null input.");
	    throw new SAMLRequesterException(
			SAMLUtils.bundle.getString("nullInput"));
	}
	statusCode = code;
    }

    /**
     * Gets the <code>StatusCode</code> of the Response.
     * @return <code>StatusCode</code> of the response. 
     */
    public StatusCode getStatusCode() {
	return statusCode;
    }

    /**
     * Returns the <code>StatusMessage</code> of the Response.
     * @return A String that represents the <code>StatusMessage</code> of the
     *         response. null is returned when there is no
     *         <code>StatusMessage</code> in the response.
     */
    public String getStatusMessage() {
	return statusMessage;
    }

    /**
     * Gets the <code>StatusDetail</code> of the Response.
     * @return A DOM tree element that represents the <code>StatusDetail</code>
     *         of the response. Null is returned if no <code>StatusDetail</code>
     *         in the response.
     */
    public Element getStatusDetail() {
	return statusDetail;
    }

    /**
     * This method translates the <code>AssertionArtifact</code> to an XML
     * document String based on the SAML schema.
     *
     * @return An XML String representing the <code>AssertionArtifact</code>.
     */
    public String toString() {
	return toString(true, false);
    }

    /**
     * Creates a String representation of the <code>&lt;samlp:Status&gt;</code>
     * element.
     * @param includeNS Determines whether or not the namespace qualifier
     *        is prepended to the Element when converted
     * @param declareNS Determines whether or not the namespace is declared
     *        within the Element.
     * @return A string containing the valid XML for this element
     */   
    public String toString(boolean includeNS, boolean declareNS) {
	StringBuffer xml = new StringBuffer(200);
	String prefix = "";
	String uri = "";
	if (includeNS) {
	    prefix = SAMLConstants.PROTOCOL_PREFIX;
	}
	if (declareNS) {
	    uri = SAMLConstants.PROTOCOL_NAMESPACE_STRING;
	}
	xml.append("<").append(prefix).append("Status").append(uri).
	    append(">\n").append(statusCode.toString(includeNS, false));
	if (statusMessage != null) {
	    xml.append("<").append(prefix).append("StatusMessage>").
		    append(statusMessage).append("</").append(prefix).
		    append("StatusMessage>\n");
	}
	if (statusDetail != null) {
	    xml.append("<").append(prefix).append("StatusDetail>\n");
	    NodeList nl = statusDetail.getChildNodes();
	    int len = nl.getLength();
	    for (int i = 0; i < len; i++) {
		xml.append(XMLUtils.print(nl.item(i)));
	    }
	    xml.append("</").append(prefix).append("StatusDetail>\n");
	}
	xml.append("</").append(prefix).append("Status>\n");
	return xml.toString();
    }
}
