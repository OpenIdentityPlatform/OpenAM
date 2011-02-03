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
 * $Id: AuthorizationDecisionQuery.java,v 1.2 2008/06/25 05:47:36 qcheng Exp $
 *
 */



package com.sun.identity.saml.protocol;

import com.sun.identity.saml.assertion.Action;
import com.sun.identity.saml.assertion.Evidence;
import com.sun.identity.saml.assertion.Subject;

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
 * This concrete class extends from the abstract base class
 * <code>SubjectQuery</code>.
 * It represents the query for an authorization decision assertion. It 
 * corresponds to the <code>&lt;samlp:AuthorizationDecisionQueryType&gt;</code>
 * in the SAML protocol schema.
 *
 * @supported.all.api
 */
public class AuthorizationDecisionQuery extends SubjectQuery {

    protected String resource = null;
    protected List actions = Collections.EMPTY_LIST;
    protected Evidence evidence = null;

    /**
     * Default Constructor
    */
    protected AuthorizationDecisionQuery() {
    }

    /**
     * This constructor is used to build an Authorization Decision Query from
     * a DOM tree that was built from the XML string.
     *
     * @param element the DOM tree element which contains an Authorization
     * 		Decision Query.
     * @exception SAMLException when an error occurs.
     */
    public AuthorizationDecisionQuery(Element element) 
					throws SAMLException {
	// make sure the input is not null
	if (element == null) {
	    SAMLUtils.debug.message("AuthorizationDecisionQuery: null input.");
	    throw new SAMLRequesterException(
			SAMLUtils.bundle.getString("nullInput"));
	}

	// make sure it's an AuthorizationDecisionQuery
	boolean valid = SAMLUtils.checkQuery(element,
					"AuthorizationDecisionQuery");
	if (!valid) {
	    SAMLUtils.debug.message("AuthorizationDecisionQuery: wrong inout.");
	    throw new SAMLRequesterException(
		SAMLUtils.bundle.getString("wrongInput"));
	}

	// getting the resource
	resource = element.getAttribute("Resource");
	if ((resource == null) || (resource.length() == 0)) {
	    if (SAMLUtils.debug.messageEnabled()) {
		SAMLUtils.debug.message("AuthorizationDecisionQuery: "
			+ "Missing attribute Resource.");
	    }
	    throw new SAMLRequesterException(
			SAMLUtils.bundle.getString("missingAttribute"));
	}

	// TODO not checking the sequence.

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
			    SAMLUtils.debug.message("AuthorizationDecisionQuery"
				+ ": contained more than one <Subject>");
			}
			throw new SAMLRequesterException(
			    SAMLUtils.bundle.getString("moreElement"));
		    }
		    subject = new Subject((Element) child);
		} else if (childName.equals("Action")) {
		    if (actions == Collections.EMPTY_LIST) {
			actions = new ArrayList();
		    }
		    actions.add(new Action((Element) child));
		} else if (childName.equals("Evidence")) {
		    if (evidence != null) {
			if (SAMLUtils.debug.messageEnabled()) {
			    SAMLUtils.debug.message("AuthorizationDecisionQuery"
				+ ": contained more than one <Evidence>");
			}
			throw new SAMLRequesterException(
			    SAMLUtils.bundle.getString("moreElement"));
		    }
		    evidence = new Evidence((Element) child);
		} else {
		    if (SAMLUtils.debug.messageEnabled()) {
			SAMLUtils.debug.message("AuthorizationDecisionQuery: "
				+ "included wrong element:" + childName);
		    }
		    throw new SAMLRequesterException(
			SAMLUtils.bundle.getString("wrongInput"));
		}
	    } // end childName != null
	} // end for loop

	// make sure there is one Subject
	if (subject == null) {
	    if (SAMLUtils.debug.messageEnabled()) {
		SAMLUtils.debug.message("AuthorizationDecisionQuery: missing "
			+ "<Subject>");
	    }
	    throw new SAMLRequesterException(
		SAMLUtils.bundle.getString("missingElement"));
	}

	// make sure there is at least one Action
	if (actions == Collections.EMPTY_LIST) {
	    if (SAMLUtils.debug.messageEnabled()) {
		SAMLUtils.debug.message("AuthorizationDecisionQuery: missing"
		    + " <Action>");
	    }
	    throw new SAMLRequesterException(
		SAMLUtils.bundle.getString("missingElement"));
	}
    }
 
    private void buildAuthZQuery(Subject theSubject,
				List theActions,
				Evidence theEvidence,
				String theResource)
				throws SAMLException {
	if (theSubject == null) {
	    if (SAMLUtils.debug.messageEnabled()) {
		SAMLUtils.debug.message("AuthorizationDecisionQuery: "
			+ "input <Subject> is null.");
	    }
	    throw new SAMLRequesterException(
			SAMLUtils.bundle.getString("nullInput"));
        }
	this.subject = theSubject;

	int length;
	Object temp = null;
	if ((theActions != null) &&
	    ((length = theActions.size()) != 0)) {
	    for (int i = 0; i < length; i++) {
		temp = theActions.get(i);
		if (!(temp instanceof Action)) {
		    if (SAMLUtils.debug.messageEnabled()) {
			SAMLUtils.debug.message("AuthorizationDecisionQuery: "
			    + "Wrong input for Action.");
		    }
		    throw new SAMLRequesterException(
			SAMLUtils.bundle.getString("wrongInput"));
		}
	    }
	    this.actions = theActions;
	}
	if (actions == Collections.EMPTY_LIST) {
	    if (SAMLUtils.debug.messageEnabled()) {
		SAMLUtils.debug.message("AuthorizationDecisionQuery: "
			+ "missing <Action> in input.");
	    }
	    throw new SAMLRequesterException(
			SAMLUtils.bundle.getString("missingElement"));
        }

	evidence = theEvidence;

	if ((theResource == null) || (theResource.length() == 0)) {
	    if (SAMLUtils.debug.messageEnabled()) {
		SAMLUtils.debug.message("AuthorizationDecisionQuery: "
			+ "Missing attribute Resource.");
	    }
	    throw new SAMLRequesterException(
			SAMLUtils.bundle.getString("missingAttribute"));
	}
	this.resource = theResource;
    }

    /** 
     * Constructor
     *
     * @param theSubject The subject of the query.
     * @param theActions The List of Actions of the query.
     * @param theEvidence The evidence of the query. It could be null when
     *		there is no Evidence in the query.
     * @param theResource A string representing the resource of the query.
     * @exception SAMLException when an error occurs.
     */
    public AuthorizationDecisionQuery(Subject theSubject,
				List theActions,
				Evidence theEvidence,
				String theResource) 
				throws SAMLException
    {
	buildAuthZQuery(theSubject, theActions, theEvidence, theResource);
    }
   
    /**
     * Constructor
     *
     * @param theSubject The subject of the query.
     * @param theActions The List of Actions of the query.
     * @param theResource A string representing the resource of the query.
     * @exception SAMLException when an error occurs.
     */       
    public AuthorizationDecisionQuery(Subject theSubject,
				List theActions,
				String theResource) 
				throws SAMLException {
	buildAuthZQuery(theSubject, theActions, null, theResource);
    }

    /**
     * Returns the List of Actions.
     * @return The Actions included in the query.
     */
    public List  getAction() {
	return actions;
    }
  
    /** 
     * Returns the <code>Evidence</code>
     *
     * @return the Evidence in the query. A null is returned 
     *         if there is no Evidence in the query.
     */                   
    public Evidence  getEvidence() {
	return evidence;
    }

    /**
     * Accessor for the Resource
     *
     * @return A string representing the resource.
     */
    public String getResource() {
	return resource;
    }

    /**
     * Returns the type of the query.
     *
     * @return an integer which is Query.AUTHORIZATION_DECISION_QUERY.
     */
    public int getQueryType() {
	return Query.AUTHORIZATION_DECISION_QUERY;
    }

    /**
     * This method translates the <code>AuthorizationDecisionQuery</code> to an
     * XML document String based on the <code>AuthorizationDecisionQuery</code>
     * schema.
     *
     * @return An XML String representing the
     *         <code>AuthorizationDecisionQuery</code>.
     */
    public String toString() {
        return this.toString(true, false);
    }

    /**
     * Create a String representation of the
     * <code>samlp:AuthorizationDecisionQuery</code> element.
     *
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
	xml.append("<").append(prefix).append("AuthorizationDecisionQuery").
		append(uri).append(" Resource=\"").append(resource).
		append("\">\n").
		append(subject.toString(true, true));

	Iterator iterator = actions.iterator();
	while (iterator.hasNext()) {
	    xml.append(((Action) iterator.next()).toString(true, true));
	}

	if (evidence != null) {
	    xml.append(evidence.toString(true, true));
	}
	xml.append("</").append(prefix).append("AuthorizationDecisionQuery>\n");
	return xml.toString();
    }
}
