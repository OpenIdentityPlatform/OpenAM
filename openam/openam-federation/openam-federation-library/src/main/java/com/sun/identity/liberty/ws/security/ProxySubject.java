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
 * $Id: ProxySubject.java,v 1.2 2008/06/25 05:47:20 qcheng Exp $
 *
 */


package com.sun.identity.liberty.ws.security;

import com.sun.identity.saml.common.SAMLUtils;
import com.sun.identity.saml.common.SAMLException;
import com.sun.identity.saml.common.SAMLRequesterException;

import com.sun.identity.saml.assertion.NameIdentifier;
import com.sun.identity.saml.assertion.SubjectConfirmation;
import com.sun.identity.saml.assertion.Subject;

import com.sun.identity.liberty.ws.common.wsse.WSSEConstants;

import org.w3c.dom.Element; 
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/** 
 * The <code>ProxySubject</code> class represents the identity of a proxy,
 * the confirmation key and confirmation obligation the proxy must posess and
 * demonstrate for authentication purpose.
 *
 * @supported.all.api
 */
public class ProxySubject extends Subject {
    
    /**
     * Constructs a <code>ProxySubject</code> object from a
     * <code>NameIdentifier</code> object and a
     * <code>SubjectConfirmation</code> object.
     *
     * @param nameIdentifier <code>NameIdentifier</code> object.
     * @param subjectConfirmation <code>SubjectConfirmation</code> object.
     * @throws SAMLException if <code>nameIdentifier</code> and
     *            <code>SubjectConfirmation</code> are null;
     */
    public ProxySubject(NameIdentifier nameIdentifier, SubjectConfirmation
            subjectConfirmation)  throws SAMLException {
        super(nameIdentifier, subjectConfirmation);
    }
    
    /**
     * Checks for equality between this object and the <code>ProxySubject</code>
     * passed down as parameter.
     *
     * @param subject <code>ProxySubject</code> to be checked
     * @return true if the two are EXACTLY equal.
     */
    public boolean equals(ProxySubject subject) {
        return super.equals(subject);
    }
    
    /**
     * Constructs a <code>ProxySubject</code> object from a
     * <code>NameIdentifier</code> object.
     *
     * @param nameIdentifier <code>NameIdentifier</code> object.
     * @throws SAMLException if <code>nameIdentifier</code> is null.
     */
    public ProxySubject(NameIdentifier nameIdentifier)  throws SAMLException {
        super(nameIdentifier);
    }
    
    /**
     * Constructs a <code>ProxySubject</code> object from a DOM Element.
     * which has already been built into a DOM.
     *
     * @param subjectElement An Element representing DOM tree for
     *        <code>ProxySubject</code> object.
     * @throws SAMLException if it could not process the
     *            Element properly, implying that there is an error in the
     *            sender or in the element definition.
     */
    public ProxySubject(org.w3c.dom.Element subjectElement)
    throws SAMLException {
        int elementCount=0;
        Element elt = (Element)subjectElement;
        String eltName = elt.getLocalName();
        if (eltName == null)  {
            if (SAMLUtils.debug.messageEnabled()) {
                SAMLUtils.debug.message("ProxySubject: local name missing");
            }
            throw new SAMLRequesterException(SAMLUtils.bundle.getString
                    ("nullInput")) ;
        }
        if (!(eltName.equals("ProxySubject")))  {
            if (SAMLUtils.debug.messageEnabled()) {
                SAMLUtils.debug.message("ProxySubject: invalid root element");
            }
            throw new SAMLRequesterException(SAMLUtils.bundle.getString(
                    "invalidElement")) ;
        }
        NodeList nl = subjectElement.getChildNodes();
        int length = nl.getLength();
        if (length == 0 ) {
            if (SAMLUtils.debug.messageEnabled()) {
                SAMLUtils.debug.message("Subject: No sub elements found");
            }
            throw new SAMLRequesterException(SAMLUtils.bundle.getString(
                    "emptyElement")) ;
        }
        // TODO: sequence is not checked as yet
        for (int n=0; n < length; n++) {
            Node child = (Node)nl.item(n);
            if (child.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            String childName = child.getLocalName();
            if (childName.equals("NameIdentifier"))  {
                setNameIdentifier(new NameIdentifier((Element)child));
                elementCount++;
            } else if (childName.equals("SubjectConfirmation"))  {
                setSubjectConfirmation(new SubjectConfirmation((Element)child));
                elementCount++;
            } else {
                if (SAMLUtils.debug.messageEnabled()) {
                    SAMLUtils.debug.message("Subject: Invalid element "
                            + "encountered.");
                }
                throw new SAMLRequesterException(SAMLUtils.bundle.getString(
                        "invalidElement")) ;
            }
        }
        if (elementCount > 2 ) {
            if (SAMLUtils.debug.messageEnabled()) {
                SAMLUtils.debug.message("Subject: more than allowed elements "
                        + "passed");
            }
            throw new SAMLRequesterException(SAMLUtils.bundle.getString(
                    "moreElement")) ;
        }
    }
    
    /**
     * Constructs a <code>ProxySubject</code> object from a
     * <code>SubjectConfirmation</code> object.
     *
     * @param subjectConfirmation <code>SubjectConfirmation</code> object to be
     *        added to the object.
     * @throws SAMLException if <code>subjectConfirmation</code> is null.
     */
    public ProxySubject(SubjectConfirmation subjectConfirmation)
    throws SAMLException {
        super(subjectConfirmation);
    }
    
    /**
     * Creates a String representation of the element.
     *
     * @return A string containing the valid XML for this element.
     *         By default name space name is prepended to the element name
     *         example <code>&lt;saml:Subject&gt;</code>
     */
    public java.lang.String toString() {
        // call toString() with includeNS true by default and declareNS false
        String xml = this.toString(true, false);
        return xml;
    }
    
    /**
     * Creates a String representation of the <code>&lt;Subject&gt;</code>
     * element.
     *
     * @param includeNS if true prepends all elements by their Namespace
     *        name example <code>&lt;saml:Subject&gt;</code>.
     * @param declareNS if true includes the namespace within the
     *        generated XML.
     * @return String containing the valid XML for this element.
     */
    public java.lang.String toString(boolean includeNS, boolean declareNS) {
        StringBuffer xml = new StringBuffer(3000);
        String secprefix = "";
        String secNS = "";
        String secNSString = "";
        
        if (includeNS) {
            secprefix = WSSEConstants.TAG_SEC + ":";
        }
        
        if (declareNS) {
            secNS = WSSEConstants.NS_SEC;
            secNSString = " " + WSSEConstants.TAG_XMLNS + ":" +
                    WSSEConstants.TAG_SEC + "=" + "\"" + secNS + "\"";
        }
        
        xml.append("<").append(secprefix).
                append(WSSEConstants.TAG_PROXYSUBJECT).
                append(secNSString).append(">");
        
        if (getNameIdentifier() != null ) {
            xml.append(getNameIdentifier().toString(includeNS, declareNS));
        }
        if (getSubjectConfirmation() != null)  {
            xml.append(getSubjectConfirmation().toString(includeNS, declareNS));
        }
        xml.append("</").append(secprefix).
                append(WSSEConstants.TAG_PROXYSUBJECT).append(">");
        return xml.toString();
    }
}

