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
 * $Id: SubjectConfirmation.java,v 1.2 2008/06/25 05:47:33 qcheng Exp $
 *
 */
  
package com.sun.identity.saml.assertion;

import com.sun.identity.saml.common.SAMLException;
import com.sun.identity.saml.common.SAMLUtilsCommon;
import com.sun.identity.saml.common.SAMLRequesterException;
import com.sun.identity.saml.common.SAMLConstants;
import com.sun.identity.shared.xml.XMLUtils;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import java.util.Set;
import java.util.Iterator;
import java.util.Collections;
import java.util.HashSet;

/**
 *The <code>SubjectConfirmation</code> element specifies a subject by specifying
 *data that authenticates the subject. 
 *@supported.all.api
 */
public class SubjectConfirmation {
    static SAMLConstants sc;
    private Set _confirmationMethodList = 
        Collections.synchronizedSet(new HashSet());
    private Element  _subjectConfirmationData=null;
    private Element _keyInfo=null; 
   
    /** 
     * Constructs a subject confirmation element from an existing 
     * XML block.
     *
     * @param subjectConfirmationElement a DOM Element representing the 
     *        <code>SubjectConfirmation</code> object. 
     * @throws SAMLException
     */
    public SubjectConfirmation(org.w3c.dom.Element subjectConfirmationElement) 
        throws SAMLException 
    {
        Element elt = (Element) subjectConfirmationElement;
        String eltName = elt.getLocalName();
        if (eltName == null)  {
            if (SAMLUtilsCommon.debug.messageEnabled()) {
                SAMLUtilsCommon.debug.message("SubjectConfirmation: local name "
                        + "missing");
            }
            throw new SAMLRequesterException(SAMLUtilsCommon.bundle.getString
                ("nullInput")) ;   
        }
        if (!(eltName.equals("SubjectConfirmation")))  {
            if (SAMLUtilsCommon.debug.messageEnabled()) {
                SAMLUtilsCommon.debug.message("SubjectConfirmation: "+
                    "invalid root element");
            }
            throw new SAMLRequesterException(SAMLUtilsCommon.bundle.getString(
                "invalidElement")) ;   
        }
        // TODO: Not checking for the sequence of elements
        NodeList nl = elt.getChildNodes();
        int length = nl.getLength();
        for (int n=0; n<length; n++) {
            Node child = (Node)nl.item(n);
            if (child.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            String childName = child.getLocalName();
            if (childName.equals("ConfirmationMethod")) {
                _confirmationMethodList.add(
                    XMLUtils.getElementValue((Element) child));
            }
            else if (childName.equals("SubjectConfirmationData")) {
                if (_subjectConfirmationData != null) {
                // this is the seconf time this is passed so flag error
                    if (SAMLUtilsCommon.debug.messageEnabled()) {
                        SAMLUtilsCommon.debug.message("SubjectConfirmation: "
                            + "SubjectConfirmationData already parsed");
                    }
                    throw new SAMLRequesterException(
                        SAMLUtilsCommon.bundle.getString("moreElement"));
                }
                _subjectConfirmationData = (Element) child;
            }
            else if (childName.equals("KeyInfo")) {
                if (_keyInfo != null) {
                    if (SAMLUtilsCommon.debug.messageEnabled()) {
                        SAMLUtilsCommon.debug.message("SubjectConfirmation: "
                            + "KeyInfo already parsed");
                    }
                    throw new SAMLRequesterException(
                        SAMLUtilsCommon.bundle.getString("moreElement"));
                }
                _keyInfo = (Element) child;
            } else {
                if (SAMLUtilsCommon.debug.messageEnabled()) 
                    SAMLUtilsCommon.debug.message("SubjectConfirmation: " + 
                        "unsupported element KeyInfo in SubjectConfirmation");
                throw new SAMLRequesterException(
                    SAMLUtilsCommon.bundle.getString("unsupportedElement"));
            }
        }       

        if (_confirmationMethodList.isEmpty()) {
            if (SAMLUtilsCommon.debug.messageEnabled()) 
                SAMLUtilsCommon.debug.message("SubjectConfirmation: Mandatory "
                    + "element confirmation method missing");
            throw new SAMLRequesterException(SAMLUtilsCommon.bundle.getString(
                "missingElement"));
        }
    }
          
    /**  
     * Constructor with a single confirmation method.
     *
     * @param confirmationMethod A URI (String) that identifies a protocol used
     *        to authenticate a <code>Subject</code>. Please refer to
     *        <code>draft-sstc-core-25</code> Section 7 for a list of URIs
     *        identifying common authentication protocols.
     * @exception SAMLException if the input data is null.
     */
    public SubjectConfirmation(java.lang.String confirmationMethod) 
        throws SAMLException 
    {
        if ((confirmationMethod == null) ||
            (confirmationMethod.length() == 0)) {
            if (SAMLUtilsCommon.debug.messageEnabled()) {
                SAMLUtilsCommon.debug.message("SubjectConfirmation:  null "
                    + "confirmationMethod specified");
            }
            throw new SAMLRequesterException(SAMLUtilsCommon.bundle.getString(
                "nullInput")) ;   
        }
        _confirmationMethodList.add(confirmationMethod);
    }
     
    /** 
     * Constructor for multiple confirmation methods
     * @param confirmationMethods a <code>Set</code> of
     *        <code>confirmationMethods</code>
     * @exception SAMLException if the <code>confirmationMethods</code> is
     *            empty.
     */    
    public SubjectConfirmation(Set confirmationMethods) throws SAMLException {
        if (confirmationMethods.isEmpty()) {
            if (SAMLUtilsCommon.debug.messageEnabled()) {
                SAMLUtilsCommon.debug.message("SubjectConfirmation:  No "
                    + "confirmationMethods  in the Set");
            }
            throw new SAMLRequesterException(SAMLUtilsCommon.bundle.getString(
                "nullInput")) ;   
        }
        _confirmationMethodList.addAll(confirmationMethods);
    }

    /** 
     * Adds a <code>confirmationMethod</code> to this
     * <code>SubjectConfirmation</code> element.
     *
     * @param confirmationMethod a String which is a URI (String) that 
     *        identifies a protocol used to authenticate a <code>Subject</code>.
     * @return true indicating success of the operation.
     */    
    public boolean addConfirmationMethod(String confirmationMethod) {
        if ((confirmationMethod == null) ||
            (confirmationMethod.length() == 0)) {
            return false;
        }
        _confirmationMethodList.add(confirmationMethod);
        return true;
    }
          
    /**
     * Constructs an <code>SubjectConfirmation</code> instance.
     *
     * @param confirmationMethods A set of <code>confirmationMethods</code>
     *        each of which is a URI (String) that identifies a protocol
     *        used to authenticate a <code>Subject</code>. Please refer to
     *        <code>draft-sstc-core-25</code> Section 7 for 
     *        a list of URIs identifying common authentication protocols.
     * @param subjectConfirmationData Additional authentication information to 
     *        be used by a specific authentication protocol. Can be passed as
     *        null if there is no <code>subjectConfirmationData</code> for the
     *        <code>SubjectConfirmation</code> object.
     * @param keyInfo An XML signature element that specifies a cryptographic 
     *        key held by the <code>Subject</code>. 
     * @exception SAMLException if the input data is invalid or 
     *            <code>confirmationMethods</code> is empty.
     */
    public SubjectConfirmation(Set confirmationMethods, 
                               Element subjectConfirmationData,
                               Element keyInfo) throws SAMLException
    {
        if (confirmationMethods.isEmpty()) {
            if (SAMLUtilsCommon.debug.messageEnabled()) {
                SAMLUtilsCommon.debug.message("SubjectConfirmation:  No "
                    + "confirmationMethods  in the Set");
            }
            throw new SAMLRequesterException(SAMLUtilsCommon.bundle.getString(
                "nullInput")) ;   
        }
        _confirmationMethodList.addAll(confirmationMethods);
        _subjectConfirmationData = subjectConfirmationData;   
        _keyInfo = keyInfo;   
    }
   
    /**
     *Gets Confirmation Method(s)
     *@return A <code>java.util.Set</code> of confirmation Methods. Each method 
     *contained within is a <code>String</code> representing the confirmation 
     * method.
     */
    public Set  getConfirmationMethod() {
        return _confirmationMethodList;
    }
   
    /**
     *Get Subject Confirmation Data
     *@return A String representation of the subject confirmation data with the 
     *Subject Confirmation element 
     */
    public Element getSubjectConfirmationData() {
        return _subjectConfirmationData;
    }
   
    /**
     * Sets the <code>SubjectConfirmationData</code>
     *
     * @param subjectConfirmationData A String representation of the subject 
     *        confirmation data within this <code>SubjectConfirmation</code>
     *        element 
     * @return true indicating success of the operation.
     */
    public boolean setSubjectConfirmationData(
        Element subjectConfirmationData) 
    {
        if (subjectConfirmationData == null) {
            return false;
        }
        _subjectConfirmationData = subjectConfirmationData;
        return true;
    }

    /**
     * Sets the <code>SubjectConfirmationData</code>.
     *
     * @param scDataString A String representation of the subject 
     *        confirmation data within this <code>SubjectConfirmation</code>
     *        element.
     * @return true if the operation succeed.
     */
    public boolean setSubjectConfirmationData(String scDataString) {
        if (scDataString == null) {
            return false;
        }
        StringBuffer sb = new StringBuffer(300);
        sb.append(SAMLUtilsCommon.makeStartElementTagXML(
                "SubjectConfirmationData", true, true)).
                append(scDataString).
                append(SAMLUtilsCommon.makeEndElementTagXML(
                        "SubjectConfirmationData", true));
        try {
            _subjectConfirmationData = XMLUtils.toDOMDocument(
                        sb.toString().trim(),
                        SAMLUtilsCommon.debug).getDocumentElement();
        } catch (Exception e) {
            if (SAMLUtilsCommon.debug.messageEnabled()) {
                SAMLUtilsCommon.debug.message("SubjectConfirmation: " +
                    "exception when setting scDataString.", e);
            }
            return false;
        }
        return true;
    }

    /**
     * Returns the key info.
     *
     * @return The key info.
    */
    public Element getKeyInfo() {
        return _keyInfo;
    }
   
    /**
     * Sets the key info.
     *
     * @param keyInfo <code>dsig.KeyInfo</code>.
     * @return true if operations succeeds.
     */
    public boolean setKeyInfo(Element keyInfo) {
        if (keyInfo != null) {
            _keyInfo = keyInfo;
            return true;
        } else {
            return false;
        }
    }
   
    /**
     * Checks for equality between this object and the
     * <code>SubjectConfirmation</code> passed down as parameter. Checks to
     * see that each have confirmations method present in the other one (does
     * not care about sequence)
     * Also does an exact match on <code>SubjectConfirmationData</code>.
     * Note: no check is done for <code>KeyInfo</code>.
     *
     * @param subjectConfirmation <code>SubjectConfirmation</code> to be
     *        checked.
     * @return true if the two are EXACTLY equal.
     */
    public boolean equals(SubjectConfirmation subjectConfirmation) {
        boolean cmEqual = true;
        boolean scDataEqual = true;
        if (subjectConfirmation != null) {
            Set confMethods = subjectConfirmation.getConfirmationMethod();
            int passedSize = confMethods.size();
            if (passedSize != _confirmationMethodList.size()) {
                cmEqual =  false;
            } else {
                Iterator it = confMethods.iterator();
                while (it.hasNext()) {
                    String confMethodStr = (String)it.next();
                    if(!_confirmationMethodList.contains(confMethodStr)) {
                        cmEqual=  false;
                        break;
                    }
                }
            }
            // came here hence confirmations methods are equal
            Element scData = subjectConfirmation.getSubjectConfirmationData();
            if (_subjectConfirmationData != null) {
                if (scData == null) {
                    scDataEqual = false;
                } else {
                    String thisString = printSCData(_subjectConfirmationData,
                                                        true, true);
                    String passString = printSCData(scData, true, true);
                    scDataEqual = thisString.equals(passString);
                }
            } else if (scData != null) {
                scDataEqual= false; // one is null not the other
            }
            if (!(cmEqual) || !(scDataEqual)) {
                return false;
            }
        } else {
            return false; // this object atleast has mandatory confirmation 
                          // method, while passed one is null hence false
        }
        return true; // if got here, then they are equal
    }

    private String printSCData(Element scData,
                                boolean includeNS,
                                boolean declareNS)
    {
        StringBuffer xml = new StringBuffer(1000);
        xml.append(SAMLUtilsCommon.makeStartElementTagXML(
                "SubjectConfirmationData", includeNS, declareNS));
        NodeList nl = scData.getChildNodes();
        for (int i = 0, len = nl.getLength(); i < len; i++) {
            xml.append(XMLUtils.print(nl.item(i)));
        }
        xml.append(SAMLUtilsCommon.makeEndElementTagXML(
                "SubjectConfirmationData", includeNS));
        return xml.toString();
    }

    /**
     * Returns a String representation of the  element
     *
     * @return A string containing the valid XML for this element
     *         By default name space name is prepended to the element name 
     *         example <code>&lt;saml:SubjectConfirmation&gt;</code>.
     */
    public java.lang.String toString() {
        // call toString() with includeNS true by default and declareNS false
        String xml = this.toString(true, false);
        return xml;
    }

    /**
     * Returns a String representation of the
     * <code>&lt;SubjectConfirmation&gt;</code> element.
     *
     * @param includeNS Determines whether or not the namespace qualifier is
     *        prepended to the Element when converted
     * @param declareNS Determines whether or not the namespace is declared
     *        within the Element.
     * @return A string containing the valid XML for this element.
    */ 
    public java.lang.String  toString(boolean includeNS, boolean declareNS) {
        StringBuffer xml = new StringBuffer(3000);
        String o = SAMLUtilsCommon.makeStartElementTagXML(
                  "SubjectConfirmation", includeNS, declareNS);
        xml.append(o).append(sc.NL);
        if (_confirmationMethodList.size() > 0 ) {
            Iterator it = _confirmationMethodList.iterator();
            while (it.hasNext()) {
                o = SAMLUtilsCommon.makeStartElementTagXML(
                    "ConfirmationMethod",includeNS, false);
                xml.append(o).append((String)it.next());
                o = SAMLUtilsCommon.makeEndElementTagXML(
                    "ConfirmationMethod",includeNS);
                xml.append(o);
            }
        }
        
        if (_subjectConfirmationData != null)  {
            xml.append(
                printSCData(_subjectConfirmationData, includeNS, false));
        }
        if (_keyInfo != null) {
            xml.append(XMLUtils.print(_keyInfo));
        }
        o = SAMLUtilsCommon.makeEndElementTagXML("SubjectConfirmation",
                                                 includeNS);
        xml.append(o);
        return xml.toString();
    }                           
}
