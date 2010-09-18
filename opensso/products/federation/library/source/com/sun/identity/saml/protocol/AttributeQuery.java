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
 * $Id: AttributeQuery.java,v 1.2 2008/06/25 05:47:36 qcheng Exp $
 *
 */


package com.sun.identity.saml.protocol;

import com.sun.identity.saml.assertion.AttributeDesignator;
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
 * It represents the query for an attribute assertion. It corresponds
 * to the <code>samlp:AttributeQueryType</code> in the SAML protocol schema.
 *
 * @supported.all.api
 */
public class AttributeQuery extends SubjectQuery {

    /**
     * O or more AttributeDesignator of this query.
     */
    protected List attributeDesignators = Collections.EMPTY_LIST;

    protected String resource = null;
   
    /**
     * Default constructor 
     */
    protected AttributeQuery() {
    }
   
    /**
     * This constructor is used to build an Attribute Query from a DOM
     * tree that was built from the XML string.
     *
     * @param element the DOM tree element which contains an Attribute Query.
     * @throws SAMLException
     */
    public AttributeQuery(Element element) throws SAMLException {
	// make sure the input is not null
	if (element == null) {
	    SAMLUtils.debug.message("AttributeQuery(Element): null input.");
	    throw new SAMLRequesterException(
		SAMLUtils.bundle.getString("nullInput"));
	}

	// make sure it's an AttributeQuery
	boolean valid = SAMLUtils.checkQuery(element, "AttributeQuery");
	if (!valid) {
	    SAMLUtils.debug.message("AttributeQuery: wrong input.");
	    throw new SAMLRequesterException(
		SAMLUtils.bundle.getString("wrongInput"));
	}

	if (element.hasAttribute("Resource")) {
	    resource = element.getAttribute("Resource");
	}

	NodeList nl = element.getChildNodes();
	Node child;
	String childName;
	for (int k = 0, length = nl.getLength(); k < length; k++) {
	    child = nl.item(k);
	    if ((childName = child.getLocalName()) != null) {
		if (childName.equals("Subject")) {
		    if (subject != null) {
			if (SAMLUtils.debug.messageEnabled()) {
			    SAMLUtils.debug.message("AttributeQuery(Element): "
				+ "contained more than one Subject");
			}
			throw new SAMLRequesterException(
			    SAMLUtils.bundle.getString("moreElement"));
		    }
		    subject = new Subject((Element) child);
		} else if (childName.equals("AttributeDesignator")) {
		    if (attributeDesignators == Collections.EMPTY_LIST) {
			attributeDesignators = new ArrayList();
		    }
		    attributeDesignators.add(
				new AttributeDesignator((Element) child));
		} else {
		    if (SAMLUtils.debug.messageEnabled()) {
			SAMLUtils.debug.message("AttributeQuery(Element): "
			    + "included wrong element:" + childName);
		    }
		    throw new SAMLRequesterException(
			SAMLUtils.bundle.getString("wrongInput"));
		}
	    } // end childName != null
	} // end for loop

	// make sure there is one Subject
	if (subject == null) {
	    SAMLUtils.debug.message("AttributeQuery: missing Subject");
	    throw new SAMLRequesterException(
		SAMLUtils.bundle.getString("missingElement"));
	}
    }

    /**
     * Constructor.
     * @param theSubject Subject of this query.
     * @param designators List of <code>AttributeDesignators</code> of this
     *        query.
     * @param theResource the Resource attribute of this query in String format.
     * @throws SAMLException
     */
    public AttributeQuery(Subject theSubject,
			List designators,
			String theResource)
					throws SAMLException
    {
	buildAttributeQuery(theSubject, designators, theResource);
    }

    /**
     * Constructor to construct an <code>AttributeQuery</code> with a list of 
     * <code>AttributeDesignators</code>.
     *
     * @param theSubject Subject of this query.
     * @param designators List of <code>AttributeDesignators</code> of this
     *        query.
     * @throws SAMLException
     */       
    public  AttributeQuery(Subject theSubject,
				List designators) throws SAMLException {
	buildAttributeQuery(theSubject, designators, null);
    }

    /**
     * Constructor to construct an <code>AttributeQuery</code> with 0
     * <code>AttributeDesignator</code>, and no Resource attribute.
     *
     * @param theSubject Subject of this query.
     * @throws SAMLException
     */
    public AttributeQuery(Subject theSubject) throws SAMLException {
	buildAttributeQuery(theSubject, null, null);
    }
 
    private void buildAttributeQuery(Subject theSubject,
					List designators,
					String theResource)
					throws SAMLException {
	if (theSubject == null) {
	    SAMLUtils.debug.message("AttributeQuery: missing subject.");
	    throw new SAMLRequesterException(
			SAMLUtils.bundle.getString("missingElement"));
	}
	this.subject = theSubject;

	int length;
	Object temp = null;
	if ((designators != null) &&
	    ((length = designators.size()) != 0)) {
	    for (int i = 0; i < length; i++) {
		temp = designators.get(i);
		if (!(temp instanceof AttributeDesignator)) {
		    if (SAMLUtils.debug.messageEnabled()) {
			SAMLUtils.debug.message("AttributeQuery: Wrong input "
			    + "for AttributeDesignator.");
		    }
		    throw new SAMLRequesterException(
			SAMLUtils.bundle.getString("wrongInput"));
		}
	    }
	    this.attributeDesignators = designators;
	}

	this.resource = theResource;
    }

    /**
     * Returns the Resource String.
     *
     * @return the Resource associated with this query in String format; or
     *		null if there is none.
     */
    public String getResource() {
	return resource;
    }

    /**
     * Returns the <code>AttributeDesignators</code>.
     *
     * @return the <code>AttributeDesignators</code>(s) associated with this
     *         query. It could contain 0 or more
     *         <code>AttributeDesignators</code>.
     */
    public List getAttributeDesignator() {
    	return attributeDesignators; 
    }

    /**
     * Returns the type of the query.
     *
     * @return an integer which is Query.ATTRIBUTE_QUERY.
     */
    public int getQueryType() {
	return Query.ATTRIBUTE_QUERY;
    }

    /**
     * Returns the String Representation of the <code>AttributeQuery</code> 
     * object.
     *
     * @return XML String representing the <code>AttributeQuery</code>.
     */
    public String toString() {
        return toString(true, false);
    }

    /**
     * Returns a String representation of the <code>samlp:AttributeQuery</code>
     * element.
     *
     * @param includeNS Determines whether or not the namespace qualifier
     * 	      is prepended to the Element when converted.
     * @param declareNS Determines whether or not the namespace is declared
     *	      within the Element.
     * @return A string containing the valid XML for this element.
     */
    public String toString(boolean includeNS, boolean declareNS) {
	StringBuffer xml = new StringBuffer(300);
	String prefix = "";
	String uri = "";
	if (includeNS) {
	    prefix = SAMLConstants.PROTOCOL_PREFIX;
	}
	if (declareNS) {
	    uri = SAMLConstants.PROTOCOL_NAMESPACE_STRING;
	}
	xml.append("<").append(prefix).append("AttributeQuery").append(uri);
	if (resource != null) {
	    xml.append(" Resource=\"").append(resource).append("\"");
	}
	xml.append(">\n").append(subject.toString(true, true));
	Iterator iterator = attributeDesignators.iterator();
	while (iterator.hasNext()) {
	    xml.append(((AttributeDesignator) iterator.next()).
						toString(true, true));
	}
	xml.append("</").append(prefix).append("AttributeQuery>\n");
	return xml.toString();
    }
}
