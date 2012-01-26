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
 * $Id: SubjectImpl.java,v 1.3 2008/06/25 05:47:44 qcheng Exp $
 *
 */


package com.sun.identity.saml2.assertion.impl;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.sun.identity.shared.xml.XMLUtils;
import com.sun.identity.saml2.assertion.Subject;
import com.sun.identity.saml2.assertion.SubjectConfirmation;
import com.sun.identity.saml2.assertion.AssertionFactory;
import com.sun.identity.saml2.assertion.BaseID;
import com.sun.identity.saml2.assertion.EncryptedID;
import com.sun.identity.saml2.assertion.NameID;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2SDKUtils;

/** 
 * The <code>Subject</code> specifies the principal that is the subject
 * of all of the statements in the assertion. It contains an identifier,
 * a series of one or more subject confirmations, or both.
 */

public class SubjectImpl implements Subject {

    private List subjectConfirmations = new ArrayList();
    private BaseID baseId = null;
    private NameID nameId = null;
    private EncryptedID encryptedId = null;
    private boolean isMutable = true;

    public static final String SUBJECT_ELEMENT = "Subject";
    public static final String SUBJECT_CONFIRMATION_ELEMENT = 
                               "SubjectConfirmation";
    public static final String BASE_ID_ELEMENT = "BaseID";
    public static final String NAME_ID_ELEMENT = "NameID";
    public static final String ENCRYPTED_ID_ELEMENT = "EncryptedID";

   /** 
    * Default constructor
    */
    public SubjectImpl() {
    }

    /**
     * This constructor is used to build <code>Subject</code> object 
     * from a XML string.
     *
     * @param xml A <code>java.lang.String</code> representing
     *        a <code>Subject</code> object
     * @exception SAML2Exception if it could not process the XML string
     */
    public SubjectImpl(String xml) throws SAML2Exception {
        Document document = XMLUtils.toDOMDocument(xml, SAML2SDKUtils.debug);
        if (document != null) {
            Element rootElement = document.getDocumentElement();
            processElement(rootElement);
            makeImmutable();
        } else {
            SAML2SDKUtils.debug.error(
                "SubjectImpl.processElement(): invalid XML input");
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
                "errorObtainingElement"));
        }
    }

    /**
     * This constructor is used to build <code>Subject</code> object 
     * from a block of existing XML that has already been built into 
     * a DOM.
     *
     * @param element A <code>org.w3c.dom.Element</code> representing
     *        DOM tree for <code>Subject</code> object
     * @exception SAML2Exception if it could not process the Element
     */

    public SubjectImpl(Element element) throws SAML2Exception {
        processElement(element);
        makeImmutable();
    }

    private void processElement(Element element) throws SAML2Exception {
        if (element == null) {
            SAML2SDKUtils.debug.error(
                "SubjectImpl.processElement(): invalid root element");
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
                "invalid_element"));
        }
        String elemName = element.getLocalName(); 
        if (elemName == null) {
            SAML2SDKUtils.debug.error(
                "SubjectImpl.processElement(): local name missing");
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
                "missing_local_name"));
        }

        if (!elemName.equals(SUBJECT_ELEMENT)) {
            SAML2SDKUtils.debug.error(
                "SubjectImpl.processElement(): invalid local name " 
                + elemName);
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
                "invalid_local_name"));
        }

        // starts processing subelements
        NodeList nodes = element.getChildNodes();
        int numOfNodes = nodes.getLength();
        if (numOfNodes < 1) {
            SAML2SDKUtils.debug.error(
                "SubjectImpl.processElement(): subject has no subelements");
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
                "missing_subelements"));
        }
    
        int nextElem = 0;
        Node child = (Node)nodes.item(nextElem);
        while (child.getNodeType() != Node.ELEMENT_NODE) {
            if (++nextElem >= numOfNodes) {
                SAML2SDKUtils.debug.error("SubjectImpl.processElement():"
                    + " subject has no subelements");
                throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
                    "missing_subelements"));
            }
            child = (Node)nodes.item(nextElem);
        }

        String childName = child.getLocalName();
        if (childName != null) {
            if (childName.equals(SUBJECT_CONFIRMATION_ELEMENT)) {
                subjectConfirmations.add(AssertionFactory.getInstance().
                    createSubjectConfirmation((Element)child));
            } else if (childName.equals(BASE_ID_ELEMENT)) {
                baseId = AssertionFactory.getInstance().
                         createBaseID((Element)child);
            } else if (childName.equals(NAME_ID_ELEMENT)) {
                nameId = AssertionFactory.getInstance().
                         createNameID((Element)child);
            } else if (childName.equals(ENCRYPTED_ID_ELEMENT)) {
                encryptedId = AssertionFactory.getInstance().
                         createEncryptedID((Element)child);
            } else {
                SAML2SDKUtils.debug.error("SubjectImpl.processElement(): "
                    + "unexpected subelement " + childName);
                throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
                      "unexpected_subelement"));
            }
        }
        
        if (++nextElem >= numOfNodes) {
            return;
        }

        // The next subelements are all <SubjectConfirmation>    
        while (nextElem < numOfNodes) { 
            child = (Node)nodes.item(nextElem);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                childName = child.getLocalName();
                if (childName != null) {
                    if (childName.equals(SUBJECT_CONFIRMATION_ELEMENT)) {
                        subjectConfirmations.add(AssertionFactory.
                            getInstance().createSubjectConfirmation(
                            (Element)child));
                    } else {
                        SAML2SDKUtils.debug.error("SubjectImpl."
                            +"processElement(): unexpected subelement " 
                            + childName);
                        throw new SAML2Exception(SAML2SDKUtils.bundle.
                            getString("unexpected_subelement"));
                    }
                }
            }
            nextElem++;
        }
    }

    /**
     *  Returns the encrypted identifier
     *
     *  @return the encrypted identifier
     */
    public EncryptedID getEncryptedID() {
        return encryptedId;
    }

    /**
     *  Sets the encrypted identifier
     *
     *  @param value the encrypted identifier
     *  @exception SAML2Exception if the object is immutable
     */
    public void setEncryptedID(EncryptedID value) 
        throws SAML2Exception{
        if (!isMutable) {
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
                "objectImmutable"));
        }
        encryptedId = value;
    } 

    /**
     *  Returns the identifier in <code>NameID</code> format
     *
     *  @return the identifier in <code>NameID</code> format
     */
    public NameID getNameID() {
        return nameId;
    }

    /**
     *  Sets the identifier in <code>NameID</code> format
     *
     *  @param value the identifier in <code>NameID</code> format
     *  @exception SAML2Exception if the object is immutable
     */
    public void setNameID(NameID value) throws SAML2Exception {
        if (!isMutable) {
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
                "objectImmutable"));
        }
        nameId = value;
    } 

    /** 
     * Returns a list of subject confirmations
     *
     * @return a list of subject confirmations
     */
    public List getSubjectConfirmation() {
        return subjectConfirmations;
    }

    /** 
     * Sets a list of subject confirmations
     *
     * @param confirmations a list of subject confirmations
     * @exception SAML2Exception if the object is immutable
     */
    public void setSubjectConfirmation(List confirmations) 
        throws SAML2Exception {
        if (!isMutable) {
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
                "objectImmutable"));
        }
        subjectConfirmations = confirmations;
    } 
    

    /**
     *  Returns the identifier in <code>BaseID</code> format
     *
     *  @return the identifier in <code>BaseID</code> format
     */
    public BaseID getBaseID() {
        return baseId;
    }

    /**
     *  Sets the identifier in <code>BaseID</code> format
     *
     *  @param value the identifier in <code>BaseID</code> format
     *  @exception SAML2Exception if the object is immutable
     */
    public void setBaseID(BaseID value) throws SAML2Exception {
        if (!isMutable) {
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
                "objectImmutable"));
        }
        baseId = value;
    } 

   /**
    * Returns a String representation
    * @param includeNSPrefix Determines whether or not the namespace 
    *        qualifier is prepended to the Element when converted
    * @param declareNS Determines whether or not the namespace is 
    *        declared within the Element.
    * @return A String representation
    * @exception SAML2Exception if something is wrong during conversion
    */
    public String toXMLString(boolean includeNSPrefix, boolean declareNS)
        throws SAML2Exception {

        StringBuffer sb = new StringBuffer(2000);
        String NS = "";
        String appendNS = "";
        if (declareNS) {
            NS = SAML2Constants.ASSERTION_DECLARE_STR;
        }
        if (includeNSPrefix) {
            appendNS = SAML2Constants.ASSERTION_PREFIX;
        }
        sb.append("<").append(appendNS).append(SUBJECT_ELEMENT).
            append(NS).append(">\n");

        boolean idFound = false;
        if (baseId != null) {
            sb.append(baseId.toXMLString(includeNSPrefix, false));
            idFound = true;
        }

        if (nameId != null) {
            if (idFound) {
                SAML2SDKUtils.debug.error("SubjectImpl.toXMLString(): "
                    + "more than one types of id specified");
                throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
                      "too_many_ids_specified"));
            } else {
                sb.append(nameId.toXMLString(includeNSPrefix, false));
                idFound = true;
            }
        }

        if (encryptedId != null) {
            if (idFound) {
                SAML2SDKUtils.debug.error("SubjectImpl.toXMLString(): "
                    + "more than one types of id specified");
                throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
                      "too_many_ids_specified"));
            } else {
                sb.append(encryptedId.toXMLString(includeNSPrefix, false));
                idFound = true;
            }
        }

        int length = subjectConfirmations.size();
        if (length == 0) {
            if (!idFound) {
                SAML2SDKUtils.debug.error("SubjectImpl.toXMLString(): Need at "
                    + "least one id or one subject confirmation in a subject");
                throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
                    "need_at_least_one_id_or_on_SubjectConfirmation"));
            }
        } else {
            for (int i = 0; i < length; i++) {
                SubjectConfirmation sc = 
                    (SubjectConfirmation)subjectConfirmations.get(i);
                sb.append(sc.toXMLString(includeNSPrefix, false));
            }
        }
        sb.append("</").append(appendNS).append(SUBJECT_ELEMENT).append(">");
        return sb.toString();
    }

   /**
    * Returns a String representation
    *
    * @return A String representation
    * @exception SAML2Exception if something is wrong during conversion
    */
    public String toXMLString() throws SAML2Exception {
        return this.toXMLString(true, false);
    }

   /**
    * Makes the object immutable
    */
    public void makeImmutable() {
        if (isMutable) {
            if (subjectConfirmations != null) {
                int length = subjectConfirmations.size();
                for (int i = 0; i < length; i++) {
                    SubjectConfirmation subjectConfirmation = 
                        (SubjectConfirmation)subjectConfirmations.get(i);
                    subjectConfirmation.makeImmutable();
                }
                subjectConfirmations = 
                    Collections.unmodifiableList(subjectConfirmations);
            }
            if (baseId != null) {
                baseId.makeImmutable();
            }
            if (nameId != null) {
                nameId.makeImmutable();
            }
            isMutable = false;
        }
    }

   /**
    * Returns true if the object is mutable
    *
    * @return true if the object is mutable
    */
    public boolean isMutable() {
        return isMutable;
    }
}
