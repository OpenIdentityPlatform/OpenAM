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
 * $Id: FSSubject.java,v 1.2 2008/06/25 05:46:45 qcheng Exp $
 * Portions Copyrighted 2014 ForgeRock AS
 */


package com.sun.identity.federation.message;

import com.sun.identity.federation.common.FSUtils;
import com.sun.identity.federation.common.IFSConstants;
import com.sun.identity.federation.message.common.FSMsgException;
import com.sun.identity.federation.message.common.IDPProvidedNameIdentifier;
import com.sun.identity.saml.assertion.NameIdentifier;
import com.sun.identity.saml.assertion.Subject;
import com.sun.identity.saml.assertion.SubjectConfirmation;
import com.sun.identity.saml.common.SAMLConstants;
import com.sun.identity.saml.common.SAMLException;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * This class has methods to create <code>Subject</code> object.
 *
 * @supported.all.api
 * @deprecated since 12.0.0
 */
@Deprecated
public class FSSubject extends Subject {
    protected IDPProvidedNameIdentifier _idpNameIdentifier;
    
    /**
     * Default Constructor.
     */
    protected FSSubject() {}
    
    /**
     * Constructor creates <code>FSSubject</code> object.
     *
     * @param nameIdentifier the <code>NameIdentifier</code> of
     *        the subject.
     * @param subjectConfirmation the <code>SubjectConfirmation</code>
     *        object.
     * @param idpNameIdentifier the <code>IDPProvidedNameIdentifier</code>
     *         object.
     * @throws FSMsgException if there is an error creating this object.
     * @throws SAMLException if there is an error creating this object.
     */
    public FSSubject(NameIdentifier nameIdentifier,
            SubjectConfirmation subjectConfirmation,
            IDPProvidedNameIdentifier idpNameIdentifier)
            throws FSMsgException, SAMLException {
        super(nameIdentifier, subjectConfirmation);
        _idpNameIdentifier = idpNameIdentifier;
    }
    
    
    /**
     * Constructor creates <code>FSSubject</code> object.
     *
     * @param nameIdentifier the <code>NameIdentifier</code> of
     *        the subject.
     * @param idpNameIdentifier the <code>IDPProvidedNameIdentifier</code>
     *         object.
     * @throws FSMsgException if there is an error creating this object.
     * @throws SAMLException if there is an error creating this object.
     */
    public FSSubject(NameIdentifier nameIdentifier,
            IDPProvidedNameIdentifier idpNameIdentifier)
            throws FSMsgException, SAMLException {
        super(nameIdentifier);
        _idpNameIdentifier = idpNameIdentifier;
    }
    
    /**
     * Constructor creates <code>FSSubject</code> object from
     * the Document Element.
     *
     * @param subjectElement the Document Element
     * @throws FSMsgException if there is an error creating this object.
     * @throws SAMLException if there is an error creating this object.
     */
    public FSSubject(Element subjectElement)
    throws FSMsgException, SAMLException {
        FSUtils.debug.message("FSSubject(Element): Called");
        int elementCount=0;
        Element elt = (Element)subjectElement;
        String rootTagName = elt.getLocalName();
        if (rootTagName == null) {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("FSSubject: local name missing");
            }
            throw new FSMsgException("nullInput",null) ;
        }
        if (!(rootTagName.equals("Subject")))  {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("FSSubject: invalid root element");
            }
            throw new FSMsgException("invalidElement",null) ;
        }
        NodeList nl = subjectElement.getChildNodes();
        int length = nl.getLength();
        if (length == 0 ) {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("FSSubject: No sub elements found");
            }
            throw new FSMsgException("emptyElement",null) ;
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
                setSubjectConfirmation(new SubjectConfirmation((Element)child));
                elementCount++;
            }else if (childName.equals("IDPProvidedNameIdentifier"))  {
                _idpNameIdentifier =
                        new IDPProvidedNameIdentifier((Element)child);
                elementCount++;
            } else {
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message("FSSubject: Invalid element "
                            + "encountered.");
                }
                throw new FSMsgException("invalidElement",null) ;
            }
        }
        if (elementCount > 3 ) {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("FSSubject: more than allowed elements "
                        + "passed");
            }
            throw new FSMsgException("moreElement",null) ;
        }
        FSUtils.debug.message("FSSubject(Element): leaving");
    }
    
    /**
     * Constructor creates <code>FSSubject</code> object.
     *
     * @param subjectConfirmation the <code>SubjectConfirmation</code> object.
     * @throws SAMLException if there is an error creating this object.
     */
    public FSSubject(SubjectConfirmation subjectConfirmation)
    throws SAMLException {
        super(subjectConfirmation);
    }
    
    /**
     * Sets the Identity Provider's <code>NameIdentifier</code>.
     *
     * @param idpNameIdentifier the Identity Provider's
     *        <code>NameIdentifier</code>.
     */
    public boolean setIDPProvidedNameIdentifier(
            IDPProvidedNameIdentifier idpNameIdentifier) {
        if (idpNameIdentifier == null)  {
            if (FSUtils.debug.messageEnabled())  {
                FSUtils.debug.message("FSSubject:null IDPProvidedNameIdentifier"
                        + "specified");
            }
            return false;
        }
        _idpNameIdentifier = idpNameIdentifier;
        return true;
    }
    
    /**
     * Returns the Identity Provider's <code>NameIdentifier</code>.
     *
     * @return the Identity Provider's <code>NameIdentifier</code>.
     */
    public IDPProvidedNameIdentifier getIDPProvidedNameIdentifier() {
        return _idpNameIdentifier;
    }
    
    /**
     * Returns a String representation of this object.
     *
     * @return a string containing the valid XML for this element
     * @throws FSMsgException if there is an error converting
     *         this object ot a string.
     */
    public String toXMLString() throws FSMsgException {
        String xml = this.toXMLString(true, false);
        return xml;
    }
    
    /**
     * Returns a String representation of the Logout Response.
     *
     * @param includeNS : Determines whether or not the namespace qualifier
     *        is prepended to the Element when converted
     * @param declareNS : Determines whether or not the namespace is declared
     *        within the Element.
     * @return a string containing the valid XML for this element
     * @throws FSMsgException if there is an error converting
     *         this object ot a string.
     */
    public String toXMLString(boolean includeNS, boolean declareNS)
    throws FSMsgException {
        StringBuffer xml = new StringBuffer(3000);
        String prefix = "";
        String libprefix = "";
        String uri = "";
        String liburi = "";
        if (includeNS) {
            prefix = SAMLConstants.ASSERTION_PREFIX;
            libprefix = IFSConstants.LIB_PREFIX;
            
        }
        if (declareNS) {
            uri = SAMLConstants.assertionDeclareStr;
            liburi = IFSConstants.LIB_NAMESPACE_STRING;
        }
        
        xml.append("<").append(prefix).append("Subject").append(" ").
                append(uri).append(" ").append(liburi).append(" ").
                append("xsi:type").
                append("=\"").append(libprefix).append("SubjectType").
                append("\"").
                append(">");
        
        if (getNameIdentifier() != null ) {
            xml.append(getNameIdentifier().toString(includeNS, false));
        }
        if (getSubjectConfirmation() != null)  {
            xml.append(getSubjectConfirmation().toString(includeNS, false));
        }
        if (_idpNameIdentifier != null ) {
            xml.append(_idpNameIdentifier.toXMLString(includeNS, false));
        }
        xml.append("</").append(prefix).append("Subject").append(">");
        return xml.toString();
    }
}
