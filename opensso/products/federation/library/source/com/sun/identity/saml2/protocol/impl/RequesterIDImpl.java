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
 * $Id: RequesterIDImpl.java,v 1.2 2008/06/25 05:48:00 qcheng Exp $
 *
 */


package com.sun.identity.saml2.protocol.impl;


import com.sun.identity.shared.xml.XMLUtils;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2SDKUtils;
import com.sun.identity.saml2.protocol.RequesterID;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/** 
 * This interface identifies the requester entities in an 
 * <code>AuthnRequest</code> message.
 */

public class RequesterIDImpl implements RequesterID {
            
     private String requesterIdURI;
     private boolean isMutable=false;

    /**
     * Constructor to create the <code>RequesterID</code> Object.
     */

    public RequesterIDImpl() {
	isMutable=true;
    }

    /**
     * Constructor to create the <code>RequesterID</code> Object.
     *
     * @param element Document Element of <code>RequesterID</code> Object.
     * @throws SAML2Exception if <code>RequesterID<code> cannot be created.
     */

    public RequesterIDImpl(Element element) throws SAML2Exception {
	parseElement(element);
    }

    /**
     * Constructor to create the <code>RequesterID</code> Object.
     *
     * @param xmlString XML String Representation of <code>RequesterID</code>
     *	      object.
     * @throws SAML2Exception if <code>RequesterID<code> cannot be created.
     */
    public RequesterIDImpl(String xmlString) throws SAML2Exception {
	Document xmlDocument =
                   XMLUtils.toDOMDocument(xmlString,SAML2SDKUtils.debug);
	if (xmlDocument == null) {
            throw new SAML2Exception(	
		    SAML2SDKUtils.bundle.getString("errorObtainingElement"));
	}
        parseElement(xmlDocument.getDocumentElement());
    }

    /** 
     * Returns the value of the <code>RequesterID</code> URI.
     *
     * @return value of the <code>RequesterID</code> URI.
     * @see #setValue(String)
     */
    public String getValue() {
	return requesterIdURI;
    }
    
    /** 
     * Sets the value of the <code>RequesterID</code> URI.
     *
     * @param value of the <code>RequesterID<code> URI.
     * @throws SAML2Exception if the object is immutable.
     * @see #getValue
     */
    public void setValue(String value) throws SAML2Exception {
	if (isMutable) {
	    requesterIdURI=value;
	} else {
	    throw new SAML2Exception("objectImmutable");
	}
    }	
    
    /** 
     * Returns a String representation of this Object.
     *
     * @return a  String representation of this Object.
     * @throws SAML2Exception if cannot convert to String.
     */
    public String toXMLString() throws SAML2Exception {
	return toXMLString(true,false);
    }
    
    /** 
     * Returns a String representation
     *
     * @param includeNSPrefix determines whether or not the namespace 
     *        qualifier is prepended to the Element when converted
     * @param declareNS determines whether or not the namespace is declared
     *        within the Element.
     * @throws SAML2Exception if cannot convert to String.
     * @return a String representation of this Object.
     */
            
    public String toXMLString(boolean includeNSPrefix,boolean declareNS)
	throws SAML2Exception {
	String reqIDXMLString = null;
	if ((requesterIdURI != null) && (requesterIdURI.length() > 0)) {
	    StringBuffer xmlString = new StringBuffer(100);
	    xmlString.append(SAML2Constants.START_TAG);
	    if (includeNSPrefix) {
		xmlString.append(SAML2Constants.PROTOCOL_PREFIX);
	    }
	    xmlString.append(SAML2Constants.REQUESTERID);

	    if (declareNS) {
		xmlString.append(SAML2Constants.PROTOCOL_DECLARE_STR);
	    }
	    xmlString.append(SAML2Constants.END_TAG)
		     .append(SAML2Constants.NEWLINE)
	             .append(requesterIdURI)
		     .append(SAML2Constants.NEWLINE)
		     .append(SAML2Constants.SAML2_END_TAG)
	             .append(SAML2Constants.REQUESTERID)
	             .append(SAML2Constants.END_TAG);

	    reqIDXMLString = xmlString.toString();
	}
	return reqIDXMLString;
    }
    
    /** 
     * Makes this object immutable. 
     */
    public void makeImmutable() {
	if (isMutable) {
	    isMutable=false;
	}
    }
    
    /** 
     * Returns value true if object is mutable.
     *
     * @return true if object is mutable.
     */
    public boolean isMutable() {
	return isMutable;
    }

    void parseElement(Element element) {
        requesterIdURI = XMLUtils.getValueOfValueNode((Node)element);

    }
}
