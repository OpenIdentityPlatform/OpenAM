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
 * $Id: AdviceImpl.java,v 1.4 2008/06/25 05:47:42 qcheng Exp $
 *
 */


package com.sun.identity.saml2.assertion.impl;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import com.sun.identity.shared.xml.XMLUtils;
import com.sun.identity.saml2.assertion.Advice;
import com.sun.identity.saml2.assertion.Assertion;
import com.sun.identity.saml2.assertion.AssertionIDRef;
import com.sun.identity.saml2.assertion.AssertionFactory;
import com.sun.identity.saml2.assertion.EncryptedAssertion;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2SDKUtils;

/** 
 * The <code>Advice</code> contains any additional information that the 
 * SAML authority wishes to provide.  This information may be ignored 
 * by applications without affecting either the semantics or the 
 * validity of the assertion. An <code>Advice</code> contains a mixture
 * of zero or more <code>Assertion</code>, <code>EncryptedAssertion</code>,
 * <code>AssertionIDRef</code>, and <code>AssertionURIRef</code>.
 */
public class AdviceImpl implements Advice {

    private List assertions = new ArrayList();
    private List encryptedAssertions = new ArrayList();
    private List assertionIDRefs = new ArrayList();
    private List assertionURIRefs = new ArrayList();
    private List additionalInfo = new ArrayList();
    private boolean isMutable = true;

    public static String ADVICE_ELEMENT = "Advice";
    public static String ASSERTION_URI_REF_ELEMENT = "AssertionURIRef";
    public static String ASSERTION_ID_REF_ELEMENT = "AssertionIDRef";
    public static String ASSERTION_ELEMENT = "Assertion";
    public static String ENCRYPTED_ASSERTION_ELEMENT = "EncryptedAssertion";

   /** 
    * Default constructor
    */
    public AdviceImpl() {
    }

    /**
     * This constructor is used to build <code>Advice</code> object from a
     * XML string.
     *
     * @param xml A <code>java.lang.String</code> representing
     *        a <code>Advice</code> object
     * @exception SAMLException if it could not process the XML string
     */
    public AdviceImpl(String xml) throws SAML2Exception {
        Document document = XMLUtils.toDOMDocument(xml, SAML2SDKUtils.debug);
        if (document != null) {
            Element rootElement = document.getDocumentElement();
            processElement(rootElement);
            makeImmutable();
        } else {
            SAML2SDKUtils.debug.error(
                "AdviceImpl.processElement(): invalid XML input");
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
                "errorObtainingElement"));
        }
    }

    /**
     * This constructor is used to build <code>Advice</code> object from a
     * block of existing XML that has already been built into a DOM.
     *
     * @param element A <code>org.w3c.dom.Element</code> representing
     *        DOM tree for <code>Advice</code> object
     * @exception SAMLException if it could not process the Element
     */
    public AdviceImpl(Element element) throws SAML2Exception {
        processElement(element);
        makeImmutable();
    }

    private void processElement(Element element) throws SAML2Exception {
        if (element == null) {
            SAML2SDKUtils.debug.error(
                "AdviceImpl.processElement(): invalid root element");
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
                "invalid_element"));
        }
        String elemName = element.getLocalName();
        if (elemName == null) {
            SAML2SDKUtils.debug.error(
                "AdviceImpl.processElement(): local name missing");
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
                "missing_local_name"));
        }

        if (!elemName.equals(ADVICE_ELEMENT)) {
            SAML2SDKUtils.debug.error(
                "AdviceImpl.processElement(): invalid local name " + elemName);
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
                "invalid_local_name"));
        }

        // starts processing subelements
        NodeList nodes = element.getChildNodes();
        int numOfNodes = nodes.getLength();
        int nextElem = 0;

        while (nextElem < numOfNodes) { 
            Node child = (Node) nodes.item(nextElem);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                String childName = child.getLocalName();
                if (childName != null) {
                    String ns = child.getNamespaceURI();
                    if (SAML2Constants.ASSERTION_NAMESPACE_URI.equals(ns)) {

                        if (childName.equals(ASSERTION_ID_REF_ELEMENT)) {
                            assertionIDRefs.add(AssertionFactory.getInstance().
                                createAssertionIDRef((Element)child));
                        } else if (childName.equals(
                            ASSERTION_URI_REF_ELEMENT)) {
                            assertionURIRefs.add(
                                XMLUtils.getElementValue((Element)child));
                        } else if (childName.equals(ASSERTION_ELEMENT)) {
                            assertions.add(AssertionFactory.getInstance().
                                createAssertion((Element)child));
                        } else if (childName.equals(
                            ENCRYPTED_ASSERTION_ELEMENT)) {
                            encryptedAssertions.add(AssertionFactory.
                                getInstance().createEncryptedAssertion(
                                (Element)child));
                        } else if (childName.equals(
                            ASSERTION_URI_REF_ELEMENT)) {
                            assertionURIRefs.add(
                                XMLUtils.getElementValue((Element)child));
                        } else {
                            additionalInfo.add(
                                XMLUtils.print((Element)child));
                        }
                    } else {
                        additionalInfo.add(XMLUtils.print((Element)child));
                    }
                }
            }
            nextElem++;
        }
 
    }

    /** 
     *  Returns a list of <code>Assertion</code>
     * 
     *  @return a list of <code>Assertion</code>
     */
    public List getAssertions() {
        return assertions;
    }

    /** 
     *  Sets a list of <code>Assertion</code>
     * 
     *  @param assertions a list of <code>Assertion</code>
     *  @exception SAML2Exception if the object is immutable
     */
    public void setAssertions(List assertions) throws SAML2Exception {
        if (!isMutable) {
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
                "objectImmutable"));
        } 
        this.assertions = assertions;
    }

    /** 
     *  Returns a list of <code>AssertionIDRef</code>
     * 
     *  @return a list of <code>AssertionIDRef</code>
     */
    public List getAssertionIDRefs() {
        return assertionIDRefs;
    }

    /** 
     *  Sets a list of <code>AssertionIDRef</code>
     * 
     *  @param idRefs a list of <code>AssertionIDRef</code>
     *  @exception SAML2Exception if the object is immutable
     */
    public void setAssertionIDRefs(List idRefs) throws SAML2Exception {
        if (!isMutable) {
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
                "objectImmutable"));
        } 
        assertionIDRefs = idRefs;
    }

    /** 
     *  Returns a list of <code>AssertionURIRef</code>  
     * 
     *  @return a list of <code>AssertionURIRef</code>
     */
    public List getAssertionURIRefs() {
        return assertionURIRefs;
    }

    /** 
     *  Sets a list of <code>AssertionURIRef</code>  
     * 
     *  @param uriRefs a list of <code>AssertionURIRef</code>
     *  @exception SAML2Exception if the object is immutable
     */
    public void setAssertionURIRefs(List uriRefs) throws SAML2Exception {
        if (!isMutable) {
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
                "objectImmutable"));
        } 
        assertionURIRefs = uriRefs;
    }

    /** 
     *  Returns a list of <code>EncryptedAssertion</code>
     * 
     *  @return a list of <code>EncryptedAssertion</code>
     */
    public List getEncryptedAssertions() {
        return encryptedAssertions;
    }

    /** 
     *  Sets a list of <code>EncryptedAssertion</code>
     * 
     *  @param encryptedAssertions a list of <code>EncryptedAssertion</code>
     *  @exception SAML2Exception if the object is immutable
     */
    public void setEncryptedAssertions(List encryptedAssertions) 
        throws SAML2Exception {
        if (!isMutable) {
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
                "objectImmutable"));
        } 
        this.encryptedAssertions = encryptedAssertions;
    }
        
    /** 
     *  Returns a list of additional information
     * 
     *  @return a list of additional information
     */
    public List getAdditionalInfo() {
        return additionalInfo;
    }

    /** 
     *  Sets a list of additional information
     * 
     *  @param info a list of additional information
     *  @exception SAML2Exception if the object is immutable
     */
    public void setAdditionalInfo(List info) throws SAML2Exception {
        if (!isMutable) {
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
                "objectImmutable"));
        } 
        this.additionalInfo = info;
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
        sb.append("<").append(appendNS).append(ADVICE_ELEMENT).
            append(NS).append(">\n");
        int length = 0;
        if (assertionIDRefs != null) {
            length = assertionIDRefs.size();
            for (int i = 0; i < length; i++) {
                AssertionIDRef assertionIDRef =
                    (AssertionIDRef)assertionIDRefs.get(i);
                sb.append(assertionIDRef.toXMLString(includeNSPrefix, false));
            }
        }
        if (assertionURIRefs != null) {
            length = assertionURIRefs.size();
            for (int i = 0; i < length; i++) {
                String str = (String)assertionURIRefs.get(i);
                sb.append("<").append(appendNS).
                    append(ASSERTION_URI_REF_ELEMENT).append(">").
                    append(str).append("</").append(appendNS).
                    append(ASSERTION_URI_REF_ELEMENT).append(">\n");
            }
        }
        if (assertions != null) {
            length = assertions.size();
            for (int i = 0; i < length; i++) {
                Assertion assertion = (Assertion)assertions.get(i);
                sb.append(assertion.toXMLString(includeNSPrefix, false));
            }
        }
        if (encryptedAssertions != null) {
            length = encryptedAssertions.size();
            for (int i = 0; i < length; i++) {
                EncryptedAssertion ea = 
                    (EncryptedAssertion)encryptedAssertions.get(i);
                sb.append(ea.toXMLString(includeNSPrefix, false));
            }
        }
        if (additionalInfo != null) {
            length = additionalInfo.size();
            for (int i = 0; i < length; i++) {
                String str = (String)additionalInfo.get(i);
                sb.append(str).append("\n"); 
            }
        }
        sb.append("</").append(appendNS).append(ADVICE_ELEMENT).append(">");
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
            if (assertions != null) {
                int length = assertions.size();
                for (int i = 0; i < length; i++) {
                    Assertion assertion = (Assertion)assertions.get(i);
                    assertion.makeImmutable();
                }
                assertions = Collections.unmodifiableList(assertions);
            }
            if (encryptedAssertions != null) {
                encryptedAssertions = Collections.unmodifiableList(
                                              encryptedAssertions);
            }
            if (assertionIDRefs != null) {
                int length = assertionIDRefs.size();
                for (int i = 0; i < length; i++) {
                    AssertionIDRef assertionIDRef =
                        (AssertionIDRef)assertionIDRefs.get(i);
                    assertionIDRef.makeImmutable();
                }
                assertionIDRefs = Collections.unmodifiableList(
                                              assertionIDRefs);
            }
            if (assertionURIRefs != null) {
                assertionURIRefs = Collections.unmodifiableList(
                                              assertionURIRefs);
            }
            if (additionalInfo != null) {
                additionalInfo = Collections.unmodifiableList(
                                              additionalInfo);
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
