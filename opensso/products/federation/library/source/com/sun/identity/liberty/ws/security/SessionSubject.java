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
 * $Id: SessionSubject.java,v 1.2 2008/06/25 05:47:22 qcheng Exp $
 *
 */


package com.sun.identity.liberty.ws.security;


import org.w3c.dom.Node; 
import org.w3c.dom.NodeList;
import org.w3c.dom.Element;
import com.sun.identity.saml.common.SAMLException;
import com.sun.identity.saml.assertion.SubjectConfirmation;
import com.sun.identity.federation.message.FSSubject;
import com.sun.identity.federation.message.common.IDPProvidedNameIdentifier;
import com.sun.identity.federation.message.common.FSMsgException;
import com.sun.identity.federation.common.IFSConstants;
import com.sun.identity.liberty.ws.common.wsse.WSSEConstants;
import com.sun.identity.saml.common.SAMLUtils;
import com.sun.identity.saml.common.SAMLConstants;
import com.sun.identity.saml.assertion.NameIdentifier;
/** 
 * The <code>SessionSubject</code> class represents a liberty subject
 * with associated session status.
 *
 * @supported.all.api
 */
public class SessionSubject extends FSSubject {
    
    /**
     * Constructs a <code>SessionSubject<code> object from a
     * <code>NameIdentifier</code> object, <code>SubjectConfirmation</code> and
     * <code>IDPProvidedNameIdentifier</code> object.
     *
     * @param nameIdentifier <code>NameIdentifier</code> object.
     * @param subjectConfirmation <code>SubjectConfirmation</code> object.
     * @param idpNameIdentifier <code>IDPProvidedNameIdentifier</code> object.
     * @throws FSMsgException if <code>idpNameIdentifier</code> is null.
     * @throws SAMLException if both <code>nameIdentifier</code> and
     *            <code>subjectConfirmation</code> are null.
     */
    public SessionSubject(
            NameIdentifier nameIdentifier,
            SubjectConfirmation subjectConfirmation,
            IDPProvidedNameIdentifier idpNameIdentifier
            ) throws FSMsgException, SAMLException {
        super(nameIdentifier, subjectConfirmation, idpNameIdentifier);
    }

    /**
     * Constructs a <code>SessionSubject</code> object from a
     * <code>NameIdentifier</code> object and a
     * <code>IDPProvidedNameIdentifier</code> object.
     *
     * @param nameIdentifier <code>NameIdentifier</code> object.
     * @param idpNameIdentifier <code>IDPProvidedNameIdentifier</code> object.
     * @throws FSMsgException if <code>idpNameIdentifier</code> is null.
     * @throws SAMLException if <code>nameIdentifier</code> is null.
     */
    public SessionSubject(
            NameIdentifier nameIdentifier,
            IDPProvidedNameIdentifier idpNameIdentifier
            )  throws FSMsgException, SAMLException {
        super(nameIdentifier, idpNameIdentifier);
    }
    
    /**
     * Constructs a <code>SessionSubject</code> object from a DOM element. 
     * which has already been built into a DOM.
     *
     * @param subjectElement An Element representing DOM tree for Subject object
     * @throws SAMLException if can not create the object of
     *            <code>NameIdentifier</code> or
     *            <code>SubjectConfirmation</code> inside the DOM element.
     * @throws FSMsgException if it could not process the Element properly,
     *            implying that there is an error in the sender or in the
     *            element definition.
     */
    public SessionSubject(
            Element subjectElement
            )  throws FSMsgException, SAMLException {
        SecurityTokenManager.debug.message("SessionSubject(Element): Called");
        int elementCount=0;
        Element elt = (Element)subjectElement;
        String rootTagName = elt.getLocalName();
        String rootTagNS = elt.getNamespaceURI();
        if (rootTagName == null) {
            if (SecurityTokenManager.debug.messageEnabled()) {
                SecurityTokenManager.debug.message(
                        "SessionSubject: local name missing");
            }
            throw new FSMsgException(SAMLUtils.bundle.getString
                    ("nullInput")) ;
        }
        if (!(rootTagName.equals("SessionSubject")))  {
            if (SecurityTokenManager.debug.messageEnabled()) {
                SecurityTokenManager.debug.message(
                        "SessionSubject: invalid root element");
            }
            throw new FSMsgException(SAMLUtils.bundle.getString(
                    "invalidElement")) ;
        }
        NodeList nl = subjectElement.getChildNodes();
        int length = nl.getLength();
        if (length == 0 ) {
            if (SecurityTokenManager.debug.messageEnabled()) {
                SecurityTokenManager.debug.message(
                        "SessionSubject: No sub elements found");
            }
            throw new FSMsgException(SAMLUtils.bundle.getString(
                    "emptyElement")) ;
        }
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
                super.setSubjectConfirmation(
                        new SubjectConfirmation((Element)child));
                elementCount++;
            }else if (childName.equals("IDPProvidedNameIdentifier"))  {
                _idpNameIdentifier =
                        new IDPProvidedNameIdentifier((Element)child);
                elementCount++;
            }else {
                if (SecurityTokenManager.debug.messageEnabled()) {
                    SecurityTokenManager.debug.message(
                            "SessionSubject: Invalid element encountered.");
                }
                throw new FSMsgException(SAMLUtils.bundle.getString(
                        "invalidElement")) ;
            }
        }
        if (elementCount > 3 ) {
            if (SecurityTokenManager.debug.messageEnabled()) {
                SecurityTokenManager.debug.message(
                        "SessionSubject: more than allowed elements passed");
            }
            throw new FSMsgException(SAMLUtils.bundle.getString(
                    "moreElement")) ;
        }
        if (_idpNameIdentifier == null) {
            if (SecurityTokenManager.debug.messageEnabled())  {
                SecurityTokenManager.debug.message(
                        "SessionSubject: mandatory IDPProvidedNameIdentifier "
                        + "missing");
            }
            throw new FSMsgException(SAMLUtils.bundle.getString(
                    "missingElement")) ;
        }
        SecurityTokenManager.debug.message("SessionSubject(Element): leaving");
        
    }

    /**
     * Constructs a <code>SessionSubject</code> object from a
     * <code>SubjectConfirmation</code> object.
     *
     * @param subjectConfirmation <code>SubjectConfirmation</code> object to be
     *        added to the object.
     * @throws SAMLException if <code>subjectConfirmation</code> is null.
     */
    public SessionSubject(SubjectConfirmation subjectConfirmation)
    throws SAMLException {
        super(subjectConfirmation);
    }
    
    /**
     * Returns a String representation of the  element.
     *
     * @return a string containing the valid XML for this element
     *         By default name space name is prepended to the element name
     *         example <code>&lt;saml:Subject&gt;</code>.
     * @throws FSMsgException if could not create a String
     *            representation of this element.
     */
    public String toXMLString() throws FSMsgException {
        return this.toXMLString(true, false);
        
    }

    /**
     * Returns a String representation of the <code>&lt;Subject&gt;</code> 
     * element.
     *
     * @param includeNS if true prepends all elements by their Namespace
     * name example <code>&lt;saml:Subject&gt;</code>
     *
     * @param declareNS if true includes the namespace within the
     *        generated XML.
     * @return a string containing the valid XML for this element.
     * @throws FSMsgException if could not create a String
     *            representation of this element.
     */
    public String toXMLString(
            boolean includeNS,
            boolean declareNS
            ) throws FSMsgException {
        SAMLConstants sc;
        StringBuffer xml = new StringBuffer(3000);
        String libprefix = "";
        String secprefix = "";
        String liburi = "";
        String secNS = "";
        String secNSString = "";
        
        if (includeNS) {
            libprefix = IFSConstants.LIB_PREFIX;
            secprefix = WSSEConstants.TAG_SEC + ":";
        }
        if (declareNS) {
            liburi = IFSConstants.LIB_NAMESPACE_STRING;
            secNS = WSSEConstants.NS_SEC;
            secNSString = " " + WSSEConstants.TAG_XMLNS + ":" +
                    WSSEConstants.TAG_SEC + "=\"" + secNS + "\"";
        }
        
        xml.append("<").append(secprefix).
                append(WSSEConstants.TAG_SESSIONSUBJECT).
                append(secNSString).append(">");
        
        if (getNameIdentifier() != null ) {
            xml.append(getNameIdentifier().toString(includeNS, declareNS));
        }
        if (getSubjectConfirmation() != null)  {
            xml.append(getSubjectConfirmation().toString(includeNS, declareNS));
        }
        if (_idpNameIdentifier != null ) {
            xml.append(_idpNameIdentifier.toXMLString(includeNS, declareNS));
        }
        xml.append("</").append(secprefix).
                append(WSSEConstants.TAG_SESSIONSUBJECT).append(">");
        return xml.toString();
    }
}
