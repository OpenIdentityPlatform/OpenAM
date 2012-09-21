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
 * $Id: RequestedAuthnContextImpl.java,v 1.2 2008/06/25 05:48:00 qcheng Exp $
 *
 */



package com.sun.identity.saml2.protocol.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.sun.identity.shared.xml.XMLUtils;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2SDKUtils;
import com.sun.identity.saml2.protocol.RequestedAuthnContext;

/**
 * Java content class for RequestedAuthnContextType complex type.
 * <p>The following schema fragment specifies the expected 
 *	content contained within this java content object. 
 * <p>
 * <pre>
 * &lt;complexType name="RequestedAuthnContextType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;choice>
 *         &lt;element ref="{urn:oasis:names:tc:SAML:2.0:assertion}AuthnContextClassRef" maxOccurs="unbounded"/>
 *         &lt;element ref="{urn:oasis:names:tc:SAML:2.0:assertion}AuthnContextDeclRef" maxOccurs="unbounded"/>
 *       &lt;/choice>
 *       &lt;attribute name="Comparison" type="{urn:oasis:names:tc:SAML:2.0:protocol}AuthnContextComparisonType" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 */
public class RequestedAuthnContextImpl implements RequestedAuthnContext {
    public final String elementName = "RequestedAuthnContext";
    private boolean mutable = false;
    private List authnContextClassRef = null;
    private List authnContextDeclRef = null;
    private String comparison = null;
    
    /**
     * Constructor
     */
    public RequestedAuthnContextImpl() {
        mutable = true;
    }

    /**
     * Constructor
     *
     * @param element the Document Element.
     * @throws SAML2Exception if there is an error creating this object.
     */
    public RequestedAuthnContextImpl(Element element) throws SAML2Exception {
    	parseElement(element);
        makeImmutable();
    }

    /**
     * Constructor
     *
     * @param xmlString the <code>RequestedAuthnContext</code> XML String. 
     * @throws SAML2Exception if there is an error creating this object.
     */
    public RequestedAuthnContextImpl(String xmlString) throws SAML2Exception {
    	Document doc = XMLUtils.toDOMDocument(xmlString, SAML2SDKUtils.debug);
	if (doc == null) {
            SAML2SDKUtils.debug.message("RequestedAuthnContextImpl :"
                      + "Input is null.");
            throw new SAML2Exception(
                      SAML2SDKUtils.bundle.getString("nullInput"));
	}
	parseElement(doc.getDocumentElement());
        makeImmutable();
    }

    private void parseElement(Element element) throws SAML2Exception {
    	String eltName = element.getLocalName();
        if (eltName == null)  {
	    if (SAML2SDKUtils.debug.messageEnabled()) {
	    	SAML2SDKUtils.debug.message("parseElement(Element): "
                    + "local name missing");
	    }
	    throw new SAML2Exception("");
        }
        
        comparison = element.getAttribute(
            SAML2Constants.COMPARISON);

        if (!(eltName.equals(elementName)))  {
	    if (SAML2SDKUtils.debug.messageEnabled()) {
	    	SAML2SDKUtils.debug.message("RequestedAuthnContextImpl: "
                    + "invalid element");
	    }
            throw new SAML2Exception(
                      SAML2SDKUtils.bundle.getString("wrongInput"));
        }
        
        // set AuthnContextClassRef or AuthnContextDeclRef property 
    	NodeList nl = element.getChildNodes();
    	int length = nl.getLength();

    	for(int i = 0; i < length; i++) {
    	    Node child = nl.item(i);
            String childName = child.getLocalName();

    	    if(childName == null) {
                continue;
            } 

            if(childName.equals("AuthnContextClassRef")) {
                if(authnContextDeclRef != null) {
               	    SAML2SDKUtils.debug.error("AuthnContext(Element): Should"
                             + "contain either <AuthnContextClassRef> or "
                             + "<AuthnContextDeclRef>");
                    throw new SAML2Exception(
                      SAML2SDKUtils.bundle.getString("wrongInput"));

	        }

    	        getAuthnContextClassRef().add(
                                XMLUtils.getElementValue((Element)child));
    	    } else if (childName.equals("AuthnContextDeclRef")) {
                if(authnContextClassRef != null) {
                    SAML2SDKUtils.debug.error("AuthnContext(Element): Should"
                            + "contain either <AuthnContextClassRef> or "
                            + "<AuthnContextDeclRef>");
                    throw new SAML2Exception(
                      SAML2SDKUtils.bundle.getString("wrongInput"));
	        }

    	        getAuthnContextDeclRef().add(
                                XMLUtils.getElementValue((Element)child));
            }
    	}
    }
	
    /**
     * Returns the value of AuthnContextClassRef property. 
     * 
     * @return List of String representing authentication context 
     *          class reference.
     * @see #setAuthnContextClassRef(List)
     */
    public List getAuthnContextClassRef() {
        if (authnContextClassRef == null) {
            authnContextClassRef = new ArrayList();
        }

    	return authnContextClassRef;
    }

    /**
     * Sets the value of AuthnContextClassRef property.
     * 
     * @param value List of String representing authentication context
     *          class referance.
     * @throws com.sun.identity.saml2.common.SAML2Exception
     *          if the object is immutable.
     * @see #getAuthnContextClassRef
     */
    public void setAuthnContextClassRef(List value) throws SAML2Exception {
    	if (!mutable) {
            throw new SAML2Exception(
                SAML2SDKUtils.bundle.getString("objectImmutable"));
    	}

        if ((authnContextDeclRef != null) && !authnContextDeclRef.isEmpty()) {
       	    SAML2SDKUtils.debug.error("setAuthnContextClassRef: Should"
                    + "contain either <AuthnContextClassRef> or "
                    + "<AuthnContextDeclRef>");
           throw new SAML2Exception(
             SAML2SDKUtils.bundle.getString("wrongInput"));
        }
        
        authnContextClassRef = value;

    	return;
    }
    
    /**
     * Returns List of String representing authentication context
     *          declaration reference.
     *
     * @return List of String representing authentication context 
     *          declaration reference.
     * @see #setAuthnContextDeclRef(List)
     */
    public List getAuthnContextDeclRef() {
        if (authnContextDeclRef == null) {
            authnContextDeclRef = new ArrayList();
        }

    	return authnContextDeclRef;
    }

    /**
     * Sets the value of the <code>AuthnContextDeclRef</code> property.
     *
     * @param value List of String representing authentication context
     *          declaration referance.
     * @throws com.sun.identity.saml2.common.SAML2Exception
     *          if the object is immutable.
     * @see #getAuthnContextDeclRef
     */
    public void setAuthnContextDeclRef(List value) throws SAML2Exception {
    	if (!mutable) {
            throw new SAML2Exception(
                SAML2SDKUtils.bundle.getString("objectImmutable"));
    	}

        if ((authnContextClassRef != null) && !authnContextClassRef.isEmpty()) {
       	    SAML2SDKUtils.debug.error("setAuthnContextDeclRef: Should"
                    + "contain either <AuthnContextClassRef> or "
                    + "<AuthnContextDeclRef>");
           throw new SAML2Exception(
             SAML2SDKUtils.bundle.getString("wrongInput"));
        }
    	
        authnContextDeclRef = value;
    	return;
    }

    /**
     * Returns the value of comparison property.
     * 
     * @return An String representing comparison method.
     * @see #setComparison(String)
     */
    public String getComparison() {
    	return comparison;
    }

    /**
     * Sets the value of the comparison property.
     * 
     * @param value An String representing comparison method.
     * @throws com.sun.identity.saml2.common.SAML2Exception
     *          if the object is immutable.
     * @see #getComparison
     */
    public void setComparison(String value) throws SAML2Exception {
    	if (!mutable) {
            throw new SAML2Exception(
                SAML2SDKUtils.bundle.getString("objectImmutable"));
        }

        comparison = value;
    	return;
    }

    /**
     * Returns an XML Representation of this object.
     *
     * @return A string containing the valid XML for this element
     * @throws com.sun.identity.saml2.common.SAML2Exception
     *          if unable to get the XML string. 
     */
    public String toXMLString() throws SAML2Exception {
        return toXMLString(true, false);
    }

    /**
     * Converts into an XML String.
     *
     * @param includeNSPrefix Determines whether or not the namespace qualifier
     *          is prepended to the Element when converted
     * @param declareNS Determines whether or not the namespace is declared
     *          within the Element.
     * @return A string containing the valid XML for this element
     * @throws com.sun.identity.saml2.common.SAML2Exception
     *          if unable to get the XML string. 
     */
    public String toXMLString(boolean includeNSPrefix, boolean declareNS) 
    throws SAML2Exception {
        StringBuffer xml = new StringBuffer();

        String NS="";
        String NSP="";
        String assertNS="";
        String assertNSP="";

        if (declareNS) {
            NS = SAML2Constants.PROTOCOL_DECLARE_STR;
            assertNS = SAML2Constants.ASSERTION_DECLARE_STR;
        }
        if (includeNSPrefix) {
            NSP = SAML2Constants.PROTOCOL_PREFIX;
            assertNSP = SAML2Constants.ASSERTION_PREFIX;
        }

        xml.append("<").append(NSP).append(elementName);
        xml.append(NS).append(" ");

        if (comparison == null) {
       	    comparison = "exact";
        }
        
        xml.append("Comparison=\"");
        xml.append(comparison);
        xml.append("\">");

        if ((authnContextClassRef != null) && (authnContextDeclRef != null)) {
       	    throw new SAML2Exception("");
        }

        if ((authnContextClassRef != null) && 
            (authnContextClassRef != Collections.EMPTY_LIST)) {
            Iterator it = authnContextClassRef.iterator();
            while (it.hasNext()) {
        	String element = (String)it.next();
                xml.append("<").append(assertNSP);
                xml.append("AuthnContextClassRef").append(assertNS).append(">");
                xml.append(element);
                xml.append("</").append(assertNSP);
                xml.append("AuthnContextClassRef").append(">");
            }
        }

        if ((authnContextDeclRef != null) && 
            (authnContextDeclRef != Collections.EMPTY_LIST)) {
            Iterator it = authnContextDeclRef.iterator();
            while (it.hasNext()) {
        	String element = (String)it.next();
                xml.append("<").append(assertNSP);
                xml.append("AuthnContextDeclRef").append(assertNS).append(">");
                xml.append(element);
                xml.append("</").append(assertNSP);
                xml.append("AuthnContextDeclRef").append(">");
            }
        }

        xml.append("</").append(NSP).append(elementName).append(">");

        return xml.toString();    
    }
       
    /**
     * Makes the obejct immutable
     */
    public void makeImmutable() {
        mutable = false;

        if(authnContextClassRef != null) {
            authnContextClassRef =
                    Collections.unmodifiableList(authnContextClassRef);
        }

        if(authnContextDeclRef != null) {
            authnContextDeclRef =
                    Collections.unmodifiableList(authnContextDeclRef);
        }

        return;
    }
    
    /**
     * Returns true if the object is mutable
     *
     * @return true if the object is mutable
     */
    public boolean isMutable() {
       return mutable;
    }
}
