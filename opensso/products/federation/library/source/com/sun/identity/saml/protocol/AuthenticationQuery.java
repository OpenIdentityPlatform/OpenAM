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
 * $Id: AuthenticationQuery.java,v 1.2 2008/06/25 05:47:36 qcheng Exp $
 *
 */


package com.sun.identity.saml.protocol;

import com.sun.identity.saml.assertion.Subject;

import com.sun.identity.saml.common.SAMLConstants;
import com.sun.identity.saml.common.SAMLException;
import com.sun.identity.saml.common.SAMLRequesterException;
import com.sun.identity.saml.common.SAMLUtils;

import java.util.List;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * This concrete class extends from the abstract base class
 * <code>SubjectQuery</code>.
 * It represents the query for an authentication assertion. It corresponds
 * to the <code>&lt;samlp:AuthenticationQueryType&gt;</code> in the SAML
 * protocol schema.
 *
 * @supported.all.api
 */
public class AuthenticationQuery extends SubjectQuery {

    protected String authMethod = null;
   
    /**
     * Default Constructor
     */
    protected AuthenticationQuery() {
    }
   
    /**
     * This constructor is used to build an Authentication Query from a DOM
     * tree that was built from the XML string.
     *
     * @param element the DOM tree element which contains an Authentication
     * Query.
     * @exception SAMLException when an error occurs.
     */
    public AuthenticationQuery(Element element) throws SAMLException {
	// make sure input is not null
	if (element == null) {
	    SAMLUtils.debug.message("AuthenticationQuery: null input.");
	    throw new SAMLRequesterException(
			SAMLUtils.bundle.getString("nullInput"));
	}

	// check if it's an AuthenticationQuery
	boolean valid = SAMLUtils.checkQuery(element, "AuthenticationQuery");
	if (!valid) {
	    SAMLUtils.debug.message("AuthenticationQuery: Wrong input.");
	    throw new SAMLRequesterException(
		SAMLUtils.bundle.getString("wrongInput"));
	}

	// Not checking whether Subject is in front of ConfirmatinMethod XXX
	// But it is checking that there is only one Subject, and
	// 0 or 1 ConfirmationMethod.

	NodeList nl = element.getChildNodes();
	Node child;
	String childName;
	int length = nl.getLength();
	// loop through all the children including TEXT and COMMENT
	for (int k = 0; k < length; k++) {
	    child = nl.item(k);
	    if ((childName = child.getLocalName()) != null) {
		if (childName.equals("Subject")) {
		    if (subject != null) {
	    		if (SAMLUtils.debug.messageEnabled()) {
			    SAMLUtils.debug.message("AuthenticationQuery: "
				+ "contained more than one <Subject>");
	    		}
	    		throw new SAMLRequesterException(
			    SAMLUtils.bundle.getString("moreElement"));
		    }
		    subject = new Subject((Element) child);
		} else {
	    	    if (SAMLUtils.debug.messageEnabled()) {
		    	SAMLUtils.debug.message("AuthenticationQuery: included"
				+ " wrong element:" + childName);
	            }
	            throw new SAMLRequesterException(
			SAMLUtils.bundle.getString("wrongInput"));
	    	}
	    } // end childName != null
	} // end for loop
	// make sure there is one Subject
	if (subject == null) {
	    SAMLUtils.debug.message("AuthenticationQuery: missing Subject.");
	    throw new SAMLRequesterException(
			SAMLUtils.bundle.getString("missingElement"));
	}

	// obtain the AuthenticationMethod attribute if any
	if (element.hasAttribute("AuthenticationMethod")) {
	    authMethod = element.getAttribute("AuthenticationMethod");
	}
    }
           
    /** 
     * Constructor.
     *
     * @param subject the Subject of the <code>AuthenticationQuery</code>.
     * @param authMethod the <code>AuthenticationMethod</code> in string
     *        format. It could be null.
     * @throws SAMLException
     */
    public AuthenticationQuery(Subject subject,
				String authMethod) 
				throws SAMLException {
	if (subject == null) {
	    SAMLUtils.debug.message("AuthenticationQuery: missing Subject.");
	    throw new SAMLRequesterException(
			SAMLUtils.bundle.getString("missingElement"));
	}
	this.subject = subject;
	this.authMethod = authMethod;
    }

    /**
     * Constructor.
     *
     * @param subject The Subject of the <code>AuthenticationQuery</code>.
     * @throws SAMLException
     */
    public AuthenticationQuery(Subject subject) throws SAMLException {
	if (subject == null) {
	    SAMLUtils.debug.message("AuthenticationQuery: missing Subject.");
	    throw new SAMLRequesterException(
			SAMLUtils.bundle.getString("missingElement"));
	}
	this.subject = subject;
    }

    /**
     * Returns the <code>AuthenticationMethod</code>.
     *
     * @return <code>AuthenticationMethod</code> in string format; or null
     *         if there is none.
     */
    public String  getAuthenticationMethod() {
	return authMethod;
    }
                        
    /**
     * Returns the type of this query.
     *
     * @return <code>Query.AUTHENTICATION_QUERY</code>.
     */
    public int getQueryType() {
	return Query.AUTHENTICATION_QUERY;
    }

    /**
     * Translates the <code>AuthenticationQuery</code> to an XML document 
     * String based on the <code>AuthenticationQuery</code> schema described
     * above.
     *
     * @return An XML String representing the <code>AuthenticationQuery</code>.
     */
    public String toString() {
	return this.toString(true, false);
    }

    /**
     * Returns a String representation of the <samlp:AuthenticationQuery> 
     * element.
     *
     * @param includeNS Determines whether or not the namespace qualifier
     * 	      is prepended to the Element when converted
     * @param declareNS Determines whether or not the namespace is declared
     *	      within the Element.
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
	xml.append("<").append(prefix).append("AuthenticationQuery").
		append(uri);
	if (authMethod != null) {
	    xml.append(" AuthenticationMethod=\"").append(authMethod).
		append("\"");
	}
	xml.append(">\n").append(subject.toString(true, true));
	xml.append("</").append(prefix).append("AuthenticationQuery>\n");
	return xml.toString();
    }
}
