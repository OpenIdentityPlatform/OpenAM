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
 * $Id: AudienceRestrictionCondition.java,v 1.2 2008/06/25 05:47:31 qcheng Exp $
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
import java.util.List;
import java.util.Vector;
import java.util.Iterator;

/**
 *This is an implementation of the abstract <code>Condition</code> class, which
 *specifes that the assertion this AuthenticationCondition is part of, is 
 *addressed to one or more specific audience.
 *@supported.all.api
 */

public class AudienceRestrictionCondition extends Condition {
    private SAMLConstants sc;
    /**
    _audience is a list of all the audience this condition is addressed to.
    Its implemented as a list of <code>String</code> objects each of them 
    containing an audience ( String). This list needs to have at least one 
    audience specified.  An audience is actually a URI. The URI MAY identify a 
    document that describes the terms and conditions of audience membership
    */
    private java.util.Vector _audience = new Vector();


    /**
     * Constructs an <code>AudienceRestrictionCondition</code> element from an
     * existing XML block.
     *
     * @param audienceRestrictionConditionElement A
     *        <code>org.w3c.dom.Element</code> representing DOM tree for
     *        <code>AudienceRestrictionCondition</code> object.
     * @exception SAMLException if it could not process the 
     *            <code>org.w3c.dom.Element</code> properly, implying that there
     *            is an error in the sender or in the element definition.
     */      
    public AudienceRestrictionCondition(
        org.w3c.dom.Element audienceRestrictionConditionElement)  
            throws SAMLException
    {
        Element elt = (Element)audienceRestrictionConditionElement;
        String eltName = elt.getLocalName();
        if (eltName == null)  {
            if (SAMLUtilsCommon.debug.messageEnabled())  { 
                SAMLUtilsCommon.debug.message("AudienceRestrictionCondition: "
                    + "null condition ");
            }
            throw new SAMLRequesterException(SAMLUtilsCommon.bundle.getString(
                "nullInput"));
        }
        if (!(eltName.equals("AudienceRestrictionCondition"))) {
            if (!(eltName.equals("Condition"))) {
                if (SAMLUtilsCommon.debug.messageEnabled())  { 
                    SAMLUtilsCommon.debug.message(
                        "AudienceRestrictionCondition: "
                        + "unsupported condition ");
                }
                throw new SAMLRequesterException(
                    SAMLUtilsCommon.bundle.getString(
                    "unsupportedCondition"));
            }
        }
        if (eltName.equals("Condition")) { // seems like extension type
            String type = elt.getAttribute("xsi:type");
            if (!(type.equals("AudienceRestrictionCondition"))) {
                if (SAMLUtilsCommon.debug.messageEnabled()) {
                    SAMLUtilsCommon.debug.message(
                        "AudienceRestrictionCondition: invalid condition");
                }
                throw new SAMLRequesterException(
                    SAMLUtilsCommon.bundle.getString(
                    "invalidElement"));
            }
        }
        NodeList nl = elt.getChildNodes();
        if (nl.getLength() <= 0 ) {
            if (SAMLUtilsCommon.debug.messageEnabled())  {
                SAMLUtilsCommon.debug.message("AudienceRestrictionCondition: "
                        + "no Audience in this Element");
            }
            throw new SAMLRequesterException(
                SAMLUtilsCommon.bundle.getString("noElement")) ;   
        }
        int length = nl.getLength();
        for (int n=0; n<length; n++) {
            Node child = (Node)nl.item(n);
            if (child.getNodeType() != Node.ELEMENT_NODE)  {
                continue;
            }
            String childName = child.getLocalName();
            if (childName.equals("Audience"))  {
                _audience.add(XMLUtils.getElementValue((Element)child));
            } else { 
                if (SAMLUtilsCommon.debug.messageEnabled())  {
                    SAMLUtilsCommon.debug.message(
                        "AudienceRestrictionCondition:"
                        +"  invalid element found");
                }
                throw new SAMLRequesterException(
                    SAMLUtilsCommon.bundle.getString("invalidElement")) ;   
            }
        }
    }       

    /**  
     *Constructs <code>AudienceRestrictionCondition</code> with a
     *<code>List</code> of audience for this condition, each of them 
     *being a String.
     *@param audience A List of audience to be included within this condition
     *@exception SAMLException if the <code>List</code> is empty or if there is 
     *some error in processing the contents of the <code>List</code>
     */
    public AudienceRestrictionCondition(List audience)  throws SAMLException {
        if (audience.isEmpty()) {
            if (SAMLUtilsCommon.debug.messageEnabled()) {
                SAMLUtilsCommon.debug.message("AudienceRestrictionCondition:  "
                    + "null input specified");
            }
            throw new SAMLRequesterException(SAMLUtilsCommon.bundle.getString(
                "nullInput")) ;   
        }
        _audience.addAll(audience);
    }
    
    /**  
     *Adds an audience to this Condition element
     *@param audience audience to be added
     *@return boolean indicating success or failure of operation
     */
    public  boolean  addAudience(java.lang.String audience) {
        if ((audience != null) && !(audience.length() == 0)) {
            _audience.add(audience);
            return true; 
        } else {
            return false;
        }
    }                      
                           
    /** 
     *Adds a <code>List</code> of audience held within this Condition element
     *@param audience A <code>List</code> of audience to be included within 
     *this condition
     *@return  boolean indicating success or failure of operation.
     */             
    public boolean setAudience(List audience ) {
        if (audience.isEmpty()) return false;
        _audience.addAll(audience);
        return true;
    }
                                      
    /** 
     *Returns list of Audience held within this Condition element
     *@return An the <code>List</code> of Audience within this Condition element
     */             
    public java.util.List getAudience() {
        return _audience;
    }
                                      
    /** 
     * Returns true if a particular audience string is contained within this
     * <code>AudienceRestrictionCondition</code> object
     *
     * @param audience audience to be checked
     * @return true if the audience exists.
     */             
    public boolean containsAudience(String audience) {
        if ((audience != null) && !(audience.length() == 0)) {
            if (_audience.contains((String)audience)) {
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    /**
     * Removes an audience from the <code>List</code> within this Condition
     * element
     * @param audience A string representing the value of the Audience 
     * @return boolean true/false representing success or failure of 
     *         the operation 
     */
    public boolean  removeAudience(java.lang.String audience) {
        if ((audience != null) && !(audience.length() == 0)) {
            _audience.remove(audience);
            return true; 
        } else return false;
    }
                           
    /**
     * Returns a String representation of the element.
     *
     * @return A string containing the valid XML for this element
     * By default name space name is prepended to the element name 
     * example <code>&lt;saml:AudienceRestrictionCondition&gt;</code>.
    */
    public java.lang.String toString() {
        // call toString() with includeNS true by default and declareNS false
        String xml = this.toString(true, false);
        return xml;
    }

    /**
     * Returns a String representation of the
     * <code>&lt;AudienceRestrictionCondition&gt;</code> element.
     *
     * @param includeNS Determines whether or not the namespace qualifier is 
     *        prepended to the Element when converted
     * @param declareNS Determines whether or not the namespace is declared
     *        within the Element.
     * @return A string containing the valid XML for this element
    */                       
    public java.lang.String toString(boolean includeNS, boolean declareNS) {
        StringBuffer xml = new StringBuffer(3000);
        String o = SAMLUtilsCommon.makeStartElementTagXML(
                        "AudienceRestrictionCondition", includeNS, declareNS);
        xml.append(o).append(sc.NL);
        Iterator it = _audience.iterator();
        while ( it.hasNext()) {
            o = SAMLUtilsCommon.makeStartElementTagXML(
                "Audience",includeNS, false);
            xml.append(o).append((String)it.next());
            o = SAMLUtilsCommon.makeEndElementTagXML("Audience",includeNS);
            xml.append(o);
        }
        o = SAMLUtilsCommon.makeEndElementTagXML(
                "AudienceRestrictionCondition",includeNS);
        xml.append(o);
        return xml.toString();
    }                                                       

    /** 
     * Evaluates this condition 
     * This method can be overridden by a plug-in which provides
     * means of evaluating this condition
     *
     * @return evaluation status.
     */
    public int  evaluate() {
        return Condition.INDETERMINATE;
    }
}

