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
 * $Id: Subject.java,v 1.2 2008/06/25 05:47:33 qcheng Exp $
 *
 */

package com.sun.identity.saml.assertion;

import com.sun.identity.saml.common.SAMLUtilsCommon;
import com.sun.identity.saml.common.SAMLConstants;
import com.sun.identity.saml.common.SAMLException;
import com.sun.identity.saml.common.SAMLRequesterException;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;

/**
 *The <code>Subject</code> element specifies one or more subjects. It contains 
 *either or both of the following elements: 
 *<code>NameIdentifier</code>
 *An identification of a subject by its name and security domain.
 *<code>SubjectConfirmation</code>
 *Information that allows the subject to be authenticated.
 *
 *If a <code>Subject</code> element contains more than one subject 
 *specification, the issuer is asserting that the surrounding statement is 
 *true for all of the subjects specified. For example, if both a 
 *<code>NameIdentifier</code> and a <code>SubjectConfirmation</code> element 
 *are present, the issuer is asserting that the statement is true of both  
 *subjects being identified. A <code>Subject</code> element SHOULD NOT identify
 *more than one principal.
 *@supported.all.api 
 */


public class Subject {
    static SAMLConstants sc;
    protected SubjectConfirmation _subjectConfirmation;
    protected NameIdentifier _nameIdentifier;

    /**
     *Default constructor
     */
    protected Subject() {}
   
    /** 
     * Constructs a Subject object from a <code>NameIdentifier</code>
     * object and a <code>SubjectConfirmation</code> object.
     *
     * @param nameIdentifier <code>NameIdentifier</code> object.
     * @param subjectConfirmation <code>SubjectConfirmation</code> object.
     * @exception SAMLException if it could not process the 
     *            Element properly, implying that there is an error in the
     *            sender or in the element definition.
     */     
    public Subject(NameIdentifier nameIdentifier, SubjectConfirmation 
        subjectConfirmation)  throws SAMLException 
    {
        if ((nameIdentifier == null) && (subjectConfirmation == null)) {
            if (SAMLUtilsCommon.debug.messageEnabled()) {
                SAMLUtilsCommon.debug.message(
                    "Subject:  null NameIdentifier and SubjectConfirmation "
                        + " specified");
            }
            throw new SAMLRequesterException(
                    SAMLUtilsCommon.bundle.getString("nullInput")) ;   
        }
        if (nameIdentifier != null) {
            _nameIdentifier = nameIdentifier;
        }
        if (subjectConfirmation != null) {
            _subjectConfirmation = subjectConfirmation;
        }
    }

    /**
     * Checks for equality between this object and the Subject
     * passed down as parameter. If <code>NameIdentifier</code> is present,
     * checks for its equality by calling <code>Nameidentifier.equals()</code>.
     * if <code>SubjectConfirmation</code> is present calls
     * <code>equals()</code> method of <code>SubjectConfirmation</code> too
     * passing in the subject's <code>SubjectConfirmation</code> element.
     *
     * @param subject Subject to be checked.
     * @return true if this object and <code>subject</code> are equals.
     */
    public boolean equals(Subject subject) {
        boolean nidEqual=false;
        boolean scEqual=false;
        if (subject != null) { // the ones passed as a parameter is not null
            NameIdentifier nid = subject.getNameIdentifier();
            if (_nameIdentifier != null) { // this subject's nid
                // compare this nid and the passed as parameter one
                nidEqual = _nameIdentifier.equals(nid);
            } else  if (nid == null) {
                nidEqual= true; // passed one is null also stored nid is null
            }
            // nid is done so lets see subject confirmation now
            SubjectConfirmation sc = subject.getSubjectConfirmation();
            if (_subjectConfirmation != null ) { // this subject's SC
                scEqual = _subjectConfirmation.equals(sc);
            } else if (sc == null ) {
                scEqual= true;
            }
            if (!(nidEqual) || !(scEqual)) {
                return false;
            }
            return true; // reached here then they are equal
        }
        return false;
    }     
                
    /** 
     * Constructs a Subject object from a <code>NameIdentifier</code> object.
     *
     * @param nameIdentifier <code>NameIdentifier</code> object.
     * @exception SAMLException if it could not process the <code>Element</code>
     *            properly, implying that there is an error in the sender or in
     *            the element definition.
    */     
    public Subject(NameIdentifier nameIdentifier)  throws SAMLException { 
        if (nameIdentifier == null) {
            if (SAMLUtilsCommon.debug.messageEnabled()) {
                SAMLUtilsCommon.debug.message("Subject:  null NameIdentifier "
                    + "specified");
            }
            throw new SAMLRequesterException(SAMLUtilsCommon.bundle.getString(
                "nullInput")) ;   
        }
        _nameIdentifier = nameIdentifier;
    }
                
    /** 
     * Constructs a subject element from an existing XML block
     * which has already been built into a DOM.
     *
     * @param subjectElement An Element representing DOM tree for Subject object
     * @exception SAMLException if it could not process the Element properly,
     *            implying that there is an error in the sender or in the 
     *            element definition.
     */     
    public Subject(org.w3c.dom.Element subjectElement)  throws SAMLException {
        int elementCount=0;
        Element elt = (Element)subjectElement;
        String eltName = elt.getLocalName();
        if (eltName == null)  {
            if (SAMLUtilsCommon.debug.messageEnabled()) {
                SAMLUtilsCommon.debug.message("Subject: local name missing");
            }
            throw new SAMLRequesterException(SAMLUtilsCommon.bundle.getString
                ("nullInput")) ;   
        }
        if (!(eltName.equals("Subject")))  {
            if (SAMLUtilsCommon.debug.messageEnabled()) {
                SAMLUtilsCommon.debug.message("Subject: invalid root element");
            }
            throw new SAMLRequesterException(SAMLUtilsCommon.bundle.getString(
                "invalidElement")) ;   
        }
        NodeList nl = subjectElement.getChildNodes();
        int length = nl.getLength();
        if (length == 0 ) {
            if (SAMLUtilsCommon.debug.messageEnabled()) {
                SAMLUtilsCommon.debug.message("Subject: No sub elements found");
            }
            throw new SAMLRequesterException(SAMLUtilsCommon.bundle.getString(
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
                _nameIdentifier = createNameIdentifier((Element)child);
                elementCount++;
            }
            else if (childName.equals("SubjectConfirmation"))  {
                _subjectConfirmation = 
                    createSubjectConfirmation((Element)child);
                elementCount++;
            }
            else {
                if (SAMLUtilsCommon.debug.messageEnabled()) {
                    SAMLUtilsCommon.debug.message("Subject: Invalid element "
                        + "encountered.");
                }
                throw new SAMLRequesterException(
                    SAMLUtilsCommon.bundle.getString("invalidElement")) ;   
            }
        }      
        if (elementCount > 2 ) {
            if (SAMLUtilsCommon.debug.messageEnabled()) {
                SAMLUtilsCommon.debug.message("Subject: more than allowed " +
                    "elements passed");
            }
            throw new SAMLRequesterException(SAMLUtilsCommon.bundle.getString(
                "moreElement")) ;   
        }
    }

    /** 
     * Constructs a Subject object from a <code>SubjectConfirmation</code>
     * object. 
     *
     * @param subjectConfirmation <code>SubjectConfirmation</code> object to
     *        be added to the object.
     * @exception SAMLException if <code>subjectConfirmation</code> is null.
     */     
    public Subject(SubjectConfirmation subjectConfirmation)   
        throws SAMLException 
    {
        if (subjectConfirmation == null) {
            if (SAMLUtilsCommon.debug.messageEnabled()) {
                SAMLUtilsCommon.debug.message("Subject:  null " + 
                    "SubjectConfirmation specified");
            }
            throw new SAMLRequesterException(
                SAMLUtilsCommon.bundle.getString("nullInput")) ;   
        }
        _subjectConfirmation = subjectConfirmation;
    }
                 
    /** 
     * Sets the subject confirmation to the subject
     *
     * @param subjectConfirmation <code>SubjectConfirmation</code> to be set.
     * @return true if operation succeed.
     */
    public boolean setSubjectConfirmation(SubjectConfirmation 
        subjectConfirmation)  
    {
        if (subjectConfirmation == null)  {
            if (SAMLUtilsCommon.debug.messageEnabled())  {
                SAMLUtilsCommon.debug.message("Subject:  null " +
                    "SubjectConfirmation specified");
            }
            return false;
        }
        _subjectConfirmation = subjectConfirmation;
        return true;
    }
 
    /** 
     * Removes subject confirmation from the subject.
     *
     * @return true if the operation succeeds.
    */
    public boolean removeSubjectConfirmation()  {
        if (_nameIdentifier == null) {
            if (SAMLUtilsCommon.debug.messageEnabled())  {
                SAMLUtilsCommon.debug.message("Subject:At least one of " + 
                    "NameIdentifier and SubjectConfirmation is mandatory");
                
            }
            return false;
        }
        _subjectConfirmation = null;
        return true;
    }
 
    /** 
     * Sets the <code>NameIdentifier</code> to the subject.
     *
     * @param nameIdentifier <code>NameIdentifier</code> to be set.
     * @return true if the operation succeeds.
    */
    public boolean setNameIdentifier(NameIdentifier 
        nameIdentifier)  
    {
        if (nameIdentifier == null)  {
            if (SAMLUtilsCommon.debug.messageEnabled())  {
                SAMLUtilsCommon.debug.message("Subject:  null nameIdentifier "
                    + "specified");
            }
            return false;
        }
        _nameIdentifier = nameIdentifier;
        return true;
    }
 
    /** 
     * Removes <code>NameIdentifier</code> from the subject.
     *
     * @return true if operation succeeds.
    */
    public boolean removeNameIdentifier()  {
        if (_subjectConfirmation == null) {
            if (SAMLUtilsCommon.debug.messageEnabled())  {
                SAMLUtilsCommon.debug.message("Subject:At least one of " +
                    "NameIdentifier and SubjectConfirmation is mandatory");
                
            }
            return false;
        }
        _nameIdentifier = null;
        return true;
    }

    /**
     *Gets the <code>NameIdentifier</code> within the Subject element 
     *@return <code>NameIdentifier</code> object, within this Subject.
     */
    public NameIdentifier getNameIdentifier() {
        return _nameIdentifier;
    }
   
    /**
     *Gets the <code>SubjectConfirmation</code> within the Subject element 
     *@return <code>SubjectConfirmation</code> object, within this Subject if 
     *exists else null
     */
    public SubjectConfirmation getSubjectConfirmation() {
        return _subjectConfirmation;
    }

    /**
     * Returns a String representation of the element.
     *
     * @return A string containing the valid XML for this element
     *         By default name space name is prepended to the element name
     *         example <code>&lt;saml:Subject&gt;</code>
     */
    public java.lang.String toString() {
        // call toString() with includeNS true by default and declareNS false
        String xml = this.toString(true, false);
        return xml;
    }

   
    /**
     * Returns a String representation of the <code>&lt;Subject&gt;</code>
     * element.
     *
     * @param includeNS if true prepends all elements by their Namespace 
     *        name example <code>&lt;saml:Subject&gt;</code>.
     * @param declareNS if true includes the namespace within the 
     *        generated XML.
     * @return A string containing the valid XML for this element.
     */
    public java.lang.String toString(boolean includeNS, boolean declareNS) {
        StringBuffer xml = new StringBuffer(3000);
        String o = SAMLUtilsCommon.makeStartElementTagXML(
                    "Subject", includeNS, declareNS);
        xml.append(o).append(sc.NL);
        if (_nameIdentifier != null ) {
            xml.append(_nameIdentifier.toString(includeNS, false));
            // false above as we dont want to have nested multiple 
            // declarations of namespace
        }
        if (_subjectConfirmation != null)  {
            xml.append(_subjectConfirmation.toString(includeNS, false));
        }
        o = SAMLUtilsCommon.makeEndElementTagXML("Subject",includeNS);
        xml.append(o);
        return xml.toString();
    }       

    protected NameIdentifier
        createNameIdentifier(Element nameIdentifierElement)
        throws SAMLException {
        return new NameIdentifier(nameIdentifierElement);
    }
    
    protected SubjectConfirmation
        createSubjectConfirmation(Element subjectConfirmationElement)
        throws SAMLException {
        return new SubjectConfirmation(subjectConfirmationElement);
    }

}

