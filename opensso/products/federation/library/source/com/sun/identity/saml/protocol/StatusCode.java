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
 * $Id: StatusCode.java,v 1.2 2008/06/25 05:47:37 qcheng Exp $
 *
 */


package com.sun.identity.saml.protocol;

import com.sun.identity.shared.xml.XMLUtils;

import com.sun.identity.saml.common.SAMLConstants;
import com.sun.identity.saml.common.SAMLException;
import com.sun.identity.saml.common.SAMLRequesterException;
import com.sun.identity.saml.common.SAMLUtils;

import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;

import org.w3c.dom.Element;

/**
 * This class represents the <code>StatusCode</code> and
 * <code>SubStatusCode</code> element. It corresponds to
 * <code>samlp:StatusCodeType</code> in SAML protocol schema.
 *
 * @supported.all.api
 */
public class StatusCode {

    private StatusCode subStatusCode 	= null;
    private String value		= null;

    /**
     * This is the default constructor of <code>StatusCode</code>.
     */
    StatusCode() {
    }
  
    /**
     * Constructs an instance of <code>StatusCode</code> from a DOM element.
     *
     * @param statusCode An DOM Element that's rooted by
     *        <code>&lt;StatusCode&gt;</code>.
     * @exception SAMLException when an error occurs.
     */
    public StatusCode(Element statusCode) throws SAMLException {
	if (statusCode == null) {
	    SAMLUtils.debug.message("StatusCode: null input.");
	    throw new SAMLRequesterException(
			SAMLUtils.bundle.getString("nullInput"));
	}
	String tagName = statusCode.getLocalName();
	if (!tagName.equals("StatusCode")) {
	    SAMLUtils.debug.message("StatusCode: Wrong input: " + tagName);
	    throw new SAMLRequesterException(
			SAMLUtils.bundle.getString("wrongInput"));
	}
	value = statusCode.getAttribute("Value");
	if ((value == null) || (value.length() == 0)) {
	    SAMLUtils.debug.message("StatusCode: empty attribute Value.");
	    throw new SAMLRequesterException(
			SAMLUtils.bundle.getString("missingAttribute"));
	}
	
	List subCodenl = XMLUtils.getElementsByTagNameNS1(
				(Element) statusCode,
				SAMLConstants.PROTOCOL_NAMESPACE_URI,
				"StatusCode");
	int length = subCodenl.size();
	if (length == 1) {
	    subStatusCode = new StatusCode((Element) subCodenl.get(0));
	} else if (length != 0) {
	    if (SAMLUtils.debug.messageEnabled()) {
		SAMLUtils.debug.message("StatusCode: Included more than one"
			+ " <StatusCode> in element " + tagName);
	    }
	    throw new SAMLRequesterException(
			SAMLUtils.bundle.getString("moreElement"));
	}
    }

    /**
     * Construct a <code>StatusCode</code> object from a value String and a sub
     * <code>StatusCode</code>.
     *
     * @param value The value of the <code>StatusCode</code>. This could be
     *        prefixed by <code>samlp:</code>. If it is not prefixed, or
     *        prefixed by prefix other than <code>samlp:</code>,
     *        <code>samlp:</code> will be used instead.
     * @param subCode The optional sub <code>StatusCode</code>.
     * @exception SAMLException if value string is null, empty, or contains
     *            wrong value.
     */
    public StatusCode(String value, StatusCode subCode) throws SAMLException {
	this.value = checkAndGetValue(value);
	subStatusCode = subCode;
    }

    /**
     * Construct a <code>StatusCode</code> object from a value String.
     *
     * @param value The value of the <code>StatusCode</code>. This could be
     *        prefixed by <code>samlp:</code>. It it is not prefixed, or
     *	      prefixed by prefix other than <code>samlp:</code>,
     *        <code>samlp:</code> will be used instead.
     * @exception SAMLException if value string is null, empty, or contains
     * 	          wrong value.
     */
    public StatusCode(String value) throws SAMLException {
	this.value = checkAndGetValue(value);
    }

    private String checkAndGetValue(String value) throws SAMLException {
	if ((value == null) || (value.length() == 0)) {
	    SAMLUtils.debug.message("StatusCode: empty attribute Value.");
	    throw new SAMLRequesterException(
			SAMLUtils.bundle.getString("missingAttribute"));
	}
	
	if (value.indexOf(":") == -1) {
	    return (SAMLConstants.PROTOCOL_PREFIX + value);
	} else {
	    StringTokenizer st = new StringTokenizer(value, ":");
	    if (st.countTokens() != 2) {
		SAMLUtils.debug.message("StatusCode: wrong attribute value.");
		throw new SAMLRequesterException(
			SAMLUtils.bundle.getString("wrongAttrValue"));
	    }
            return value;
	}
    }

    /**
     * Sets the sub <code>StatusCode</code>.
     * @param subcode <code>StatusCode</code> to be included.
     */
    public void setStatusCode(StatusCode subcode) {
	subStatusCode = subcode;
    }

    /**
     * Gets the sub <code>StatusCode</code> of the <code>StatusCode</code>.
     * @return <code>StatusCode</code>.
     */
    public StatusCode getStatusCode() {
	return subStatusCode;
    }

    /**
     * Gets the value of the <code>StatusCode</code>.
     * @return A String representing the value of the <code>StatusCode</code>.
     */
    public String getValue() {
	return value;
    }

    /**
     * Translates the <code>StatusCode</code> to an XML document String
     * based on the SAML schema.
     * @return An XML String representing the <code>StatusCode</code>.
     */
    public String toString() {
	return toString(true, false);
    }

    /**
     * Creates a String representation of the
     * <code>&lt;samlp:StatusCode&gt;</code> element.
     *
     * @param includeNS Determines whether or not the namespace qualifier
     *        is prepended to the Element when converted
     * @param declareNS Determines whether or not the namespace is declared
     *        within the Element.
     * @return A string containing the valid XML for this element.
     */   
    public String toString(boolean includeNS, boolean declareNS) {
	StringBuffer xml = new StringBuffer(100);
	String prefix = "";
	String uri = "";
	if (includeNS) {
	    prefix = SAMLConstants.PROTOCOL_PREFIX;
	}
	if (declareNS) {
	    uri = SAMLConstants.PROTOCOL_NAMESPACE_STRING;
	}
	String tag = "StatusCode";
	xml.append("<").append(prefix).append(tag).append(uri).
	    append(" Value=\"");
	if (value.startsWith(SAMLConstants.PROTOCOL_PREFIX)) {
	    xml.append(value);
	} else {
	    try {
		xml.append(checkAndGetValue(value));
	    } catch (SAMLException e) {
		SAMLUtils.debug.error("StatusCode.toString: ", e);
		xml.append(value);
	    }
	}
	xml.append("\">\n");
	if ((subStatusCode != null) &&
	    (subStatusCode != Collections.EMPTY_LIST)) {
	    xml.append(subStatusCode.toString(includeNS, false));
	}
	xml.append("</").append(prefix).append(tag).append(">\n");
	return xml.toString();
    }
}
