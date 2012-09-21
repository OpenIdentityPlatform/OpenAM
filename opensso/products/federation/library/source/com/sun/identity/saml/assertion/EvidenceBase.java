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
 * $Id: EvidenceBase.java,v 1.2 2008/06/25 05:47:32 qcheng Exp $
 *
 */

package com.sun.identity.saml.assertion;

import com.sun.identity.saml.common.SAMLUtilsCommon;
import com.sun.identity.saml.common.SAMLConstants;
import com.sun.identity.saml.common.SAMLException;
import com.sun.identity.saml.common.SAMLRequesterException;
import com.sun.identity.shared.xml.XMLUtils;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;

/**
 *The <code>Evidence</code> element specifies an assertion either by
 *reference or by value. An assertion is specified by reference to the value of
 *the assertion's  <code>AssertionIDReference</code> element.
 *An assertion is specified by value by including the entire
 *<code>Assertion</code> object
 *
 * This class is an abstract base class for all Evidence implementations and
 * encapsulates common functionality.
 *@supported.all.api
 */
public abstract class EvidenceBase  {
    static SAMLConstants sc;
    private Set _assertionIDRef = new HashSet();
    private Set _assertion = new HashSet();

    /**
     * Constructs an <code>Evidence</code> object from a block of existing XML
     * that has already been built into a DOM.
     *
     * @param assertionSpecifierElement A <code>org.w3c.dom.Element</code> 
     *        representing DOM tree for <code>Evidence</code> object.
     * @exception SAMLException if it could not process the Element properly, 
     *            implying that there is an error in the sender or in the
     *            element definition.
     */
    public EvidenceBase(org.w3c.dom.Element assertionSpecifierElement) 
        throws SAMLException 
    {
        String elementName = assertionSpecifierElement.getLocalName();
        if (elementName == null)  {
            if (SAMLUtilsCommon.debug.messageEnabled()) {
                SAMLUtilsCommon.debug.message("Evidence:local name "
                        + "missing");
            }
            throw new SAMLRequesterException(SAMLUtilsCommon.bundle.getString
                ("nullInput")) ;   
        }
        if (!(elementName.equals("Evidence"))) {
            if (SAMLUtilsCommon.debug.messageEnabled()) {
                SAMLUtilsCommon.debug.message(
                   "Evidence: invalid root element");
            }
            throw new SAMLRequesterException(SAMLUtilsCommon.bundle.getString(
                "invalidElement")+":"+elementName) ;   
        }
        NodeList nl = assertionSpecifierElement.getChildNodes();
        int length = nl.getLength();
        if (length <= 0 ) {
            if (SAMLUtilsCommon.debug.messageEnabled())  {
                SAMLUtilsCommon.debug.message(elementName+":"
                    +"no sub elements found in this Element");
            }
            throw new SAMLRequesterException(SAMLUtilsCommon.bundle.getString(
                "noElement")) ;   
        }
        for (int n=0; n < length; n++) {
            Node child = (Node)nl.item(n);
            if (child.getNodeType() != Node.ELEMENT_NODE)  {
                continue;
            }
            String childName = child.getLocalName();
            if (childName.equals("Assertion")) {
                _assertion.add(createAssertion((Element)child));
            } else if (childName.equals("AssertionIDReference")) {
                _assertionIDRef.add(createAssertionIDReference(
                    XMLUtils.getElementValue((Element) child)));
            } else {
                if (SAMLUtilsCommon.debug.messageEnabled()) {
                    SAMLUtilsCommon.debug.message("Evidence:  "
                        + "invalid sub element");
                }
                throw new SAMLRequesterException(
                    SAMLUtilsCommon.bundle.getString("invalidElement")) ;   
            }
        }
    }    
    
    /**
     * Constructs a new <code>Evidence></code> element containing a
     * set of <code>Assertion</code> objects.
     *
     * @param evidenceContent A set of <code>Assertion</code> and
     *        <code>AssertionIDReference</code> objects to be put within the
     *        <code>Evidence</code> element. The same Set contains both type
     *        of elements.
     * @exception SAMLException if the Set is empty or has invalid object.
     */
    public EvidenceBase(Set evidenceContent) throws SAMLException {
        if (evidenceContent.isEmpty()) {
            if (SAMLUtilsCommon.debug.messageEnabled()) {
                SAMLUtilsCommon.debug.message("Evidence:  null input "
                    + "specified");
            }
            throw new SAMLRequesterException(SAMLUtilsCommon.bundle.getString(
                "nullInput")) ;   
        }
        Iterator it = evidenceContent.iterator();
        while (it.hasNext()) {
            Object obj = it.next();
            if (obj instanceof AssertionBase) {
                _assertion.add((AssertionBase)obj);
            } else  if (obj instanceof AssertionIDReference) {
                _assertionIDRef.add((AssertionIDReference)obj);
            } else {
                SAMLUtilsCommon.debug.message(
                    "Evidence: Invalid input Element");
                throw new SAMLRequesterException(
                    SAMLUtilsCommon.bundle.getString("invalidElement")) ;
            }
        }
    }
  
    /**
     * Constructs an Evidence from a Set of <code>Assertion</code> and
     * <code>AssertionIDReference</code> objects.
     * 
     * @param assertionIDRef Set of <code>AssertionIDReference</code> objects.
     * @param assertion Set of <code>Assertion</code> objects.
     * @exception SAMLException if either Set is empty or has invalid object.
     */
    public EvidenceBase(Set assertionIDRef, Set assertion)
        throws SAMLException {
        if (assertionIDRef.isEmpty() && assertion.isEmpty()) {
            if (SAMLUtilsCommon.debug.messageEnabled()) {
                SAMLUtilsCommon.debug.message("Evidence:  null input "
                    + "specified");
            }
            throw new SAMLRequesterException(SAMLUtilsCommon.bundle.getString(
                "nullInput")) ;   
        }
        Iterator it = assertionIDRef.iterator();
        while (it.hasNext()) {
            Object assID = it.next();
            if (assID instanceof AssertionIDReference) {
                _assertionIDRef.add((AssertionIDReference)assID);
            }
            else {
                SAMLUtilsCommon.debug.message(
                    "Evidence: Invalid input Element");
                throw new SAMLRequesterException(
                    SAMLUtilsCommon.bundle.getString("invalidElement")) ;
            }
        }
        it = assertion.iterator();
        while (it.hasNext()) {
            Object ass = it.next();
            if (ass instanceof AssertionBase) {
                _assertion.add((AssertionBase)ass);
            }
            else {
                SAMLUtilsCommon.debug.message(
                    "Evidence: Invalid input Element");
                throw new SAMLRequesterException(
                    SAMLUtilsCommon.bundle.getString("invalidElement")) ;
            }
        }
    }

    /**
     * Adds an <code>Assertion</code> object into the Evidence object.
     * @param assertion <code>Assertion</code> to be added
     * @return true if operation succeeds.
    */
    public boolean addAssertion(AssertionBase assertion) {
        if (assertion == null) {
            if (SAMLUtilsCommon.debug.messageEnabled()) {
                SAMLUtilsCommon.debug.message("Evidence:  null input "
                    + "specified");
            }
            return false;
        } else {
            _assertion.add(assertion);
            return true;
        }
    }
        
           
    /**
     * Adds an <code>AssertionIDReference</code> object into the
     * <code>Evidence</code> object.
     *
     * @param assertionIDRef <code>AssertionIDReference</code> to be added.
     * @return true if operation succeeds.
    */
    public boolean addAssertionIDReference(AssertionIDReference assertionIDRef)
    {
        if (assertionIDRef == null) {
            if (SAMLUtilsCommon.debug.messageEnabled()) {
                SAMLUtilsCommon.debug.message("Evidence:  null input "
                    + "specified");
            }
            return false;
        } else {
            _assertionIDRef.add(assertionIDRef);
            return true;
        }
    }
        
    /**
     * Removes an <code>Assertion</code> object from the <code>Evidence</code>
     * object.
     *
     * @param assertion <code>Assertion</code> to be removed.
     * @return true if the operation succeeds, Returns failure of the
     *         <code>Assertion</code> is the only element inside the
     *         <code>Evidence</code>.
     */
    public boolean removeAssertion(AssertionBase assertion)
    {
        if (assertion != null) {
            if (_assertionIDRef.size() + _assertion.size() > 1 ) {
                _assertion.remove(assertion);
                return true;
            }
        } else {
            if (SAMLUtilsCommon.debug.messageEnabled()) {
                SAMLUtilsCommon.debug.message("Evidence:  null input "
                    + "specified");
            }
        }
        return false;
    }

    /**
     * Removes an <code>AssertionIDReference</code> object from the
     * <code>Evidence</code> object.
     *
     * @param assertionIDRef <code>AssertionIDReference</code> to be removed
     * @return true if the operation succeeds, Returns false if the
     * <code>AssertionIDReference</code> is the only element
     * inside the <code>Evidence</code>.
     */
    public boolean removeAssertionIDReference(AssertionIDReference 
        assertionIDRef)
    {
        if (assertionIDRef != null) {
            if (_assertionIDRef.size() + _assertion.size() > 1 ) {
                _assertionIDRef.remove(assertionIDRef);
                return true;
            }
        } else {
            if (SAMLUtilsCommon.debug.messageEnabled()) {
                SAMLUtilsCommon.debug.message("Evidence:  null input "
                    + "specified");
            }
        }
        return false;
    }
        
    /**
     *Get <code>java.util.Set</code> of  <code>AssertionIDReference</code> 
     *objects in the <code>Evidence</code>
     *@return <code>java.util.Set</code> of <code>AssertionIDReference</code> 
     *objects within this Evidence.
     */
    public Set getAssertionIDReference() {
        return _assertionIDRef;
    }
   
    /**
     *Get <code>java.util.Set</code> of  <code>Assertion</code> 
     *objects in the <code>Evidence</code>
     *@return <code>java.util.Set</code> of <code>Assertion</code> 
     *objects within this Evidence.
     */
    public Set getAssertion() {
        return _assertion;
    }
                             
    /**
     * Returns a String representation of the element.
     *
     * @return A string containing the valid XML for this element
     *         By default name space name is prepended to the element name
     *         example <code>&lt;saml:Evidence&gt;</code>.
     */
    public java.lang.String toString() {
        // call toString() with includeNS true by default and declareNS false
        String xml = this.toString(true, false);
        return xml;
    }

    /**
     * Returns a String representation of the <code>&lt;Evidence&gt;</code>
     * element (or of the <code>&lt;Evidence&gt;</code> element).
     *
     * @param includeNS Determines whether or not the namespace qualifier is 
     *        prepended to the <code>Element</code> when converted.
     * @param declareNS Determines whether or not the namespace is declared
     *        within the Element.
     * @return The string containing the valid XML for this element .The top 
     *         level element is <code>Evidence</code>.
     */           
    public java.lang.String toString(boolean includeNS, boolean declareNS) {
        StringBuffer xml = new StringBuffer(3000);
        String o=null;
        o = SAMLUtilsCommon.makeStartElementTagXML("Evidence",
                                                   includeNS, declareNS);
        xml.append(o).append(sc.NL);
        Iterator it = _assertion.iterator();
        while (it.hasNext()) {
            AssertionBase assertion = (AssertionBase)it.next();
            xml.append(assertion.toString(includeNS, false));
        }
        it = _assertionIDRef.iterator();
        while (it.hasNext()) {
            AssertionIDReference aidRef = (AssertionIDReference)it.next();
            xml.append(aidRef.toString(includeNS, false));
        }
        o = SAMLUtilsCommon.makeEndElementTagXML("Evidence",includeNS);
        xml.append(o);
        return xml.toString();
    }

    /**
     * Creates appropriate Assertion Instance
     * @param assertionElement the assertion Element
     * @return the assertion instance
     */
    protected abstract AssertionBase
        createAssertion(Element assertionElement) 
        throws SAMLException;
    
    /**
     * Creates appropriate AssertionIDReference Instance
     * @param assertionID  the assertion ID String
     * @return the AssertionIDReference instance
     */
    protected abstract AssertionIDReference
        createAssertionIDReference(String assertionID) 
        throws SAMLException;
}
