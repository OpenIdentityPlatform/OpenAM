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
 * $Id: AdviceBase.java,v 1.2 2008/06/25 05:47:31 qcheng Exp $
 *
 */


package com.sun.identity.saml.assertion;

import java.util.*; 
import org.w3c.dom.*; 
import com.sun.identity.saml.common.SAMLUtilsCommon;
import com.sun.identity.saml.common.SAMLConstants;
import com.sun.identity.saml.common.SAMLException;
import com.sun.identity.saml.common.SAMLRequesterException;
import com.sun.identity.shared.xml.XMLUtils;

/**
 *The <code>Advice</code> element contains additional information  
 *that the issuer wish to provide.
 *This information MAY be ignored by applications without affecting
 *either the semantics or validity. Advice elements MAY be specified in 
 *an extension schema. 
 *
 * This class is an abstract base class for all Advice implementations and
 * encapsulates common functionality.
 *
 *@supported.all.api
 */
public abstract class AdviceBase {
    protected List _assertionIDRef       = null; 
    protected List _assertion            = null; 
    protected List _otherElements        = null; 
    
    /**
     * Constructs an Advice element from an existing XML block.
     *
     * @param element representing a DOM tree element 
     * @exception SAMLException if there is an error in the sender or in the
     *            element definition.
     */
    public AdviceBase(Element element) throws SAMLException {
        // Make sure the input is not null.
        if (element == null) {
            SAMLUtilsCommon.debug.message("Advice: null input.");
            throw new SAMLRequesterException(
                      SAMLUtilsCommon.bundle.getString("nullInput"));
        }
        // Make sure this element is an Advice element. 
        String tag = null;
        tag = element.getLocalName(); 
        if ((tag == null) || (!tag.equals("Advice"))) {
            SAMLUtilsCommon.debug.message("Advice: wrong input");
            throw new SAMLRequesterException(
                      SAMLUtilsCommon.bundle.getString("wrongInput"));
        }
        NodeList  nodes = element.getChildNodes();
        int nodeCount = nodes.getLength();      
        if (nodeCount > 0) {
            for (int i = 0; i < nodeCount; i++) {
                Node currentNode = nodes.item(i);               
                if (currentNode.getNodeType() == Node.ELEMENT_NODE) {
                    String tagName = currentNode.getLocalName();
                    String tagNS = currentNode.getNamespaceURI(); 
                    if ((tagName == null) || tagName.length() == 0 ||
                        tagNS == null || tagNS.length() == 0) {
                        if (SAMLUtilsCommon.debug.messageEnabled()) {
                            SAMLUtilsCommon.debug.message("Advice: " +
                                "The tag name or tag namespace of child" + 
                                " element is either null or empty.");
                        }
                        throw new SAMLRequesterException(
                             SAMLUtilsCommon.bundle.getString("nullInput"));
                    }
                    if (tagName.equals("AssertionIDReference") &&
                        tagNS.equals(SAMLConstants.assertionSAMLNameSpaceURI)) {
                        AssertionIDReference  assertionid = 
                            createAssertionIDReference((Element)currentNode); 
                        if (_assertionIDRef == null) {
                            _assertionIDRef = new ArrayList(); 
                        }
                        if ((_assertionIDRef.add(assertionid))== false) {
                            if (SAMLUtilsCommon.debug.messageEnabled()) {
                                SAMLUtilsCommon.debug.message(
                                              "Advice:failed to add" +
                                              " to AssertionIDReference List.");
                            }
                            throw new SAMLRequesterException(
                                  SAMLUtilsCommon.bundle.getString(
                                  "addListError"));
                        }
                    } else if (tagName.equals("Assertion") &&
                        tagNS.equals(SAMLConstants.assertionSAMLNameSpaceURI)) {
                        AssertionBase assertion = 
                                       createAssertion((Element)currentNode); 

                        if (_assertion == null) {
                            _assertion = new ArrayList(); 
                        }
                        if ((_assertion.add(assertion))== false) {
                            if (SAMLUtilsCommon.debug.messageEnabled()) {
                                SAMLUtilsCommon.debug.message(
                                        "Advice:failed to add" +
                                        " to Assertion List.");
                            }
                            throw new SAMLRequesterException(
                                    SAMLUtilsCommon.bundle.getString(
                                        "addListError"));
                        }
                    } else {
                        if (_otherElements == null) 
                            _otherElements = new ArrayList(); 
                        if (( _otherElements.add(
                            (Element)currentNode))==false) {
                            if (SAMLUtilsCommon.debug.messageEnabled()) {
                                SAMLUtilsCommon.debug.message("Advice: failed "
                                        + "to add to other elements list.");
                            }
                            throw new SAMLRequesterException(
                                SAMLUtilsCommon.bundle.getString(
                                                 "addListError"));
                        }
                    }
                }
            } // end of for loop 
        } // end of if (nodeCount > 0) 
    }
    
    /**
     * Constructor
     *
     * @param assertionidreference A List of <code>AssertionIDReference</code>.
     * @param assertion A List of Assertion
     * @param otherelement A List of any element defined as 
     *        <code>any namespace="##other" processContents="lax"</code>; 
     */
    public AdviceBase(List assertionidreference, List assertion,
        List otherelement) { 
        if (assertionidreference != null && !assertionidreference.isEmpty()) {
            if (_assertionIDRef == null) {
                _assertionIDRef = new ArrayList(); 
            }
            _assertionIDRef = assertionidreference; 
        }
        if (assertion != null && !assertion.isEmpty()) {
            if (_assertion == null) {
                _assertion = new ArrayList(); 
            }
            _assertion = assertion; 
        }
        if (otherelement != null) {
            if (_otherElements == null) {
                _otherElements = new ArrayList(); 
            }
            _otherElements = otherelement; 
        }
    }

    /**
     * Creates appropriate Assertion Instance
     * @param assertionElement the assertion Element
     * @return the assertion instance
     */
    protected abstract AssertionBase
        createAssertion(Element assertionElement) throws SAMLException;
    
    /**
     * Creates appropriate AssertionIDReference Instance
     * @param assertionIDRefElement the assertion ID reference Element
     * @return the assertion ID reference instance
     */
    protected abstract AssertionIDReference
        createAssertionIDReference(Element assertionIDRefElement) 
        throws SAMLException;
     
    /**
     * Returns access to the <code>AssertionIDReference</code> in this
     * Advice element.
     *
     * @return A list of <code>AssertionIDReference</code> in this Advice
     * element.
     */
    public List getAssertionIDReference() {
        return _assertionIDRef; 
    }
    
    /**
     *Gets access to the Assertion in this Advice element
     *@return A list of Assertion in this Advice element
     */
    public List getAssertion() {
        return _assertion; 
    }
    
    /**
     *Gets other element contained within the Advice element  
     *@return A list of other elements.
     */
    public List getOtherElement() {
        return _otherElements; 
    }
   
    /** 
     *Creates a String representation of the <code>Advice</code> element
     *@return A String representing the valid XML for this element
     */
    public String toString() {
        return toString(true, false); 
    }
    
    /**
     * Returns a String representation of the
     *         <code>&lt;saml:Advice&gt;</code> element.
     *
     * @param includeNS Determines whether or not the namespace qualifier
     *        is prepended to the Element when converted
     * @param declareNS Determines whether or not the namespace is declared
     *        within the Element.
     * @return A string containing the valid XML for this element
     */
    public String toString(boolean includeNS, boolean declareNS) {
        StringBuffer result = new StringBuffer(1000);
        Iterator iter = null; 
        String prefix = "";
        String uri = "";
        if (includeNS) {
            prefix = SAMLConstants.ASSERTION_PREFIX;
        }
        if (declareNS) {
            uri = SAMLConstants.assertionDeclareStr;
        }
        
        result.append("<").append(prefix).append("Advice").append(uri).
               append(">\n");
        if (_assertionIDRef != null && (!_assertionIDRef.isEmpty())) {
            iter = _assertionIDRef.iterator();
            while (iter.hasNext()) {
                result.append(((AssertionIDReference)iter.next()).
                              toString(includeNS, false));
            }
        }
        if (_assertion != null && (!_assertion.isEmpty())) {
            iter = _assertion.iterator();
            while (iter.hasNext()) {
                result.append(((AssertionBase)iter.next()).
                                toString(includeNS, false));
            }
        }
        if (_otherElements != null && !(_otherElements.isEmpty())) {
            iter = _otherElements.iterator();  
            while (iter.hasNext()) {
                result.append(XMLUtils.print((Element)iter.next()));
            }
        }
        result.append("</").append(prefix).append("Advice>\n");
        return result.toString();
    }                                                                 

}
