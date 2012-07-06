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
 * $Id: Conditions.java,v 1.4 2008/06/25 05:47:32 qcheng Exp $
 *
 */

package com.sun.identity.saml.assertion;

import com.sun.identity.shared.DateUtils;
import com.sun.identity.saml.common.SAMLUtilsCommon;
import com.sun.identity.saml.common.SAMLConstants;
import com.sun.identity.saml.common.SAMLException;
import com.sun.identity.saml.common.SAMLRequesterException;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import java.util.Set;
import java.util.HashSet;
import java.util.Collections;
import java.util.Iterator;
import java.text.ParseException;

/**
 *This <code>Conditions</code> is a set of <code>Condition</code>. 
 *The validity of an <code>Assertion</code> MAY be subject to a set of 
 *<code>Conditions</code>. Each <code>Condition</code> evaluates to a value 
 *that is Valid, Invalid or Indeterminate. 
 *
 *@supported.all.api
 */
public class Conditions {

    static SAMLConstants sc;

    private java.util.Date _notBefore=null;
    private java.util.Date _notOnOrAfter=null;

    /**
    A Set containing all the AudienceRestrictionCondition elements for this 
    <code>Conditions</code> object
    */
    private Set _arcs= Collections.synchronizedSet(new HashSet());

    protected DoNotCacheCondition doNotCache = null; 
    
    /**
     * Default Constructor
     */
    public Conditions() {}

    /**
     * Constructs an instance of <code>Conditions</code>.
     *
     * @param notBefore specifies the earliest time instant at which the 
     *        assertion is valid.
     * @param notOnOrAfter specifies the time instant at which the assertion 
     *        has expired.
     * @exception SAMLException if the <code>notBefore</code> instant is after
     *            <code>notOnOrAfter</code> instant.
     */
    public Conditions(java.util.Date notBefore, java.util.Date notOnOrAfter) 
        throws SAMLException 
    {
        if (notBefore != null) {
            if(notOnOrAfter != null) {
                if ((notBefore.after(notOnOrAfter)) || 
                    (notBefore.equals(notOnOrAfter))) 
                {
                    if (SAMLUtilsCommon.debug.messageEnabled()) {
                        SAMLUtilsCommon.debug.message("Conditions:  invalid   "
                            + " notBefore or notOnOrAfter");
                    }
                    throw new SAMLRequesterException(
                        SAMLUtilsCommon.bundle.getString("wrongInput")) ;   
                } else {
                    _notBefore = notBefore;
                    _notOnOrAfter = notOnOrAfter;
                }
            } else {
                _notBefore = notBefore;
            }
        } else { 
            _notOnOrAfter = notOnOrAfter;
        }
    }

    /** 
     * Constructs an instance of <code>Conditions</code>.
     *
     * @param notBefore specifies the earliest time instant at which the 
     *        assertion is valid.
     * @param notOnOrAfter specifies the time instant at which the assertion 
     *        has expired.
     * @param condition <code>Condition</code> object
     * @param arc the <code>&lt;AudienceRestrictionCondition&gt;</code> to be
     *        added.Can be null, if no audience restriction.
     * @exception SAMLException if there is a problem in input data and it 
     *            cannot be processed correctly.
     */
    public Conditions(java.util.Date notBefore, java.util.Date notOnOrAfter, 
                      Condition condition, AudienceRestrictionCondition arc)
                      throws SAMLException 
    {
        _notBefore = notBefore;
        _notOnOrAfter = notOnOrAfter;
        if ((notBefore != null) && (notOnOrAfter != null)) {
            if ((notBefore.after(notOnOrAfter)) || 
                (notBefore.equals(notOnOrAfter))) 
            {
                if (SAMLUtilsCommon.debug.messageEnabled()) {
                    SAMLUtilsCommon.debug.message("Conditions:  invalid  data");
                }
                throw new SAMLRequesterException(
                    SAMLUtilsCommon.bundle.getString("wrongInput")) ;   
            }
        }
        if (arc != null) {
            _arcs.add(arc);
        }
    }
    
    /** 
     * Constructs an instance of <code>Conditions</code>.
     *
     * @param notBefore specifies the earliest time instant at which the 
     *        assertion is valid.
     * @param notOnOrAfter specifies the time instant at which the assertion 
     *        has expired.
     * @param condition <code>Condition</code> object
     * @param arc the <code>&lt;AudienceRestrictionCondition&gt;</code> to be
     *        added. Can be null, if no audience restriction.
     * @param doNotCacheCnd <code>DoNotCacheCondition</code> object
     * @exception SAMLException if there is a problem in input data and it 
     *            cannot be processed correctly.
     */
    public Conditions(java.util.Date notBefore, java.util.Date notOnOrAfter, 
                      Condition condition, AudienceRestrictionCondition arc, 
                      DoNotCacheCondition doNotCacheCnd)
                      throws SAMLException 
    {  
        this(notBefore, notOnOrAfter, condition, arc); 
        if (doNotCacheCnd != null) {
            doNotCache = doNotCacheCnd;
        } 
    }
    
    /**
     * Constructs a <code>Conditions</code> element from an existing XML block.
     *
     * @param conditionsElement A <code>org.w3c.dom.Element</code> representing 
     *        DOM tree for <code>Conditions</code> object
     * @exception SAMLException if it could not process the Element properly, 
     *            implying that there is an error in the sender or in the
     *            element definition.
     */      
    public Conditions(org.w3c.dom.Element conditionsElement)  
        throws SAMLException 
    {
        Element elt = (Element)conditionsElement;
        String eltName = elt.getLocalName();
        if (eltName == null)  {
            if (SAMLUtilsCommon.debug.messageEnabled()) {
                SAMLUtilsCommon.debug.message("Conditions: local name missing");
            }
            throw new SAMLRequesterException(SAMLUtilsCommon.bundle.getString
                ("nullInput")) ;   
        }
        if (!(eltName.equals("Conditions")))  {
            if (SAMLUtilsCommon.debug.messageEnabled()) {
                SAMLUtilsCommon.debug.message(
                    "Conditions: invalid root element");
            }
            throw new SAMLRequesterException(
                SAMLUtilsCommon.bundle.getString(
                "invalidElement")+":"+eltName) ;   
        }
        String dt = elt.getAttribute("NotBefore");
        if ((dt != null) && (dt.length() != 0))  {
            try {
                _notBefore = DateUtils.stringToDate(dt);
            } catch (ParseException pe) {
                if (SAMLUtilsCommon.debug.messageEnabled()) {
                    SAMLUtilsCommon.debug.message(
                        "Conditions: could not parse  "
                        + "NotBefore or NotOnOrAfter ");
                }
                throw new SAMLRequesterException(
                    SAMLUtilsCommon.bundle.getString(
                    "wrongInput")+pe.getMessage());
            }
            
        }
        dt = elt.getAttribute("NotOnOrAfter");
        if ((dt != null) && (dt.length() != 0))  {
            try {
                _notOnOrAfter = DateUtils.stringToDate(
                            elt.getAttribute("NotOnOrAfter"));
            } catch (ParseException pe) {
                if (SAMLUtilsCommon.debug.messageEnabled()) {
                    SAMLUtilsCommon.debug.message("Conditions: could not " +
                        "parse NotBefore or NotOnOrAfter ");
                }
                throw new SAMLRequesterException(
                    SAMLUtilsCommon.bundle.getString("wrongInput")+
                    pe.getMessage());
            }
        }
        NodeList nl = conditionsElement.getChildNodes();
        int length = nl.getLength();
        for (int n=0; n<length; n++) {
            Node child = (Node)nl.item(n);
            if (child.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            String childName = child.getLocalName();
            if (childName.equals("AudienceRestrictionCondition")) {
                _arcs.add(createAudienceRestrictionCondition((Element)child));
            } else if (childName.equals("DoNotCacheCondition")) {
                doNotCache = createDoNotCacheCondition((Element)child);
            } else {
                // may be extension type defined by xsi:type
                String type = ((Element)child).getAttribute("xsi:type");
                if (type.equals("AudienceRestrictionCondition")) {
                    _arcs.add(createAudienceRestrictionCondition(
                    (Element)child));
                } else if (type.equals("DoNotCacheCondition")) {
                   doNotCache = createDoNotCacheCondition((Element)child);
                } else {    
                    if (SAMLUtilsCommon.debug.messageEnabled()) {
                        SAMLUtilsCommon.debug.message("Conditions: unsupported "
                            + "condition, cannot determine extension ");
                    }
                    throw new SAMLRequesterException(
                        SAMLUtilsCommon.bundle.getString(
                        "unsupportedCondition"));
                }
            }
        }
    }       
          
    /**
     * Returns the earliest time at which the assertion is valid held in this
     * <code>Conditions</code> object.
     *
     * @return A Date containing the <code>NotBefore</code> time held within
     *         this <code>Conditions</code> element.
     */
    public java.util.Date getNotBefore() {
        return _notBefore;
    }
   
    /**
     * Returns the time instant held within this <code>Conditions</code> object
     * at which the <code>Assertion</code> has expired.
     *
     * @return time instant (at which assertion has expired) held within this
     * <code>Conditions</code> element.
     */
    public java.util.Date getNotOnorAfter() {
        return _notOnOrAfter; 
    }

                           
    /**  
     * Adds an audience restriction condition within this
     * <code>Conditions</code> Element.
     *
     * @param arc a <code>AudienceRestrictionCondition</code> to be added to 
     *        this <code>Conditions</code>.
     * @return true if the operation succeeds.
     */
    public  boolean  addAudienceRestrictionCondition(
                AudienceRestrictionCondition arc) 
    {
        if (arc != null) {
            _arcs.add(arc);
            return true; 
        } else {
            return false;
        }
    }                      
                           
    /**
     * Returns true if a specific Date falls within the validity interval of
     * this set of conditions.
     *
     * @param someTime Any time in milliseconds. 
     * @return true if <code>someDate</code> is within the valid interval of the
     *         <code>Conditions</code>.     
     */
    public boolean checkDateValidity(long someTime) {
        if (_notBefore == null ) {
            if (_notOnOrAfter == null) {
                return true;
            } else {
                if (someTime < _notOnOrAfter.getTime()) {
                    return true;
                }
            }
        } else if (_notOnOrAfter == null ) {
            if (someTime >= _notBefore.getTime()) {
                return true;
            }
        } else if ((someTime >= _notBefore.getTime()) && 
            (someTime < _notOnOrAfter.getTime()))
        {
            return true; 
        }
        return false;
    }
    
                                      
    /** 
     * Returns a set of the <code>AudienceRestrictionCondition</code> elements
     * held within this <code>Conditions</code> element.
     *
     * @return A set of the audience restriction conditions. Each element
     *         contained within is an object of
     *         <code>AudienceRestrictionCondition</code> type.
     */
    public java.util.Set getAudienceRestrictionCondition() {
        return _arcs;
    }
                 
    /**
     * Removes an audience restriction condition
     * <code>AudienceRestrictionCondition</code>from this
     * <code>Conditions</code> object wherein the specified audience has been
     * defined.
     *
     * @param audience A string representing audience.
     * @return true if the operation succeeds.
    */
    public boolean  removeAudienceRestrictionCondition(String audience) {
        Iterator it = _arcs.iterator();
        while (it.hasNext()) {
            AudienceRestrictionCondition arc = 
                (AudienceRestrictionCondition)it.next();
            if (arc.containsAudience(audience)) {
                return arc.removeAudience(audience);
            }
        }
        return false; 
    }
                
    /** 
     * Sets <code>DoNotCacheCondition</code> elements held within this
     * <code>Conditions</code> element.
     *
     * @param doNotCacheCnd an <code>DoNotCacheCondition</code> object.
     */
    public void setDoNotCacheCondition(DoNotCacheCondition doNotCacheCnd) {
        if (doNotCacheCnd != null) {
            doNotCache = doNotCacheCnd;
        }
    }
    
    /** 
     * Returns <code>DoNotCacheCondition</code> elements held within this 
     * <code>Conditions</code> element
     *
     * @return an <code>DoNotCacheCondition</code> object if Conditions contains
     *         any <code>DoNotCacheCondition</code>, otherwise return null.
     */
    public DoNotCacheCondition getDoNotCacheCondition() {
        return doNotCache;
    }
    
    /**
     * Returns a String representation of the element.
     *
     * @return A string containing the valid XML for this element
     *         By default name space name is prepended to the element name 
     *         example <code>&lt;saml:Conditions&gt;</code>.
     */
    public java.lang.String toString() {
        // call toString() with includeNS true by default and declareNS false
        String xml = this.toString(true, false);
        return xml;
    }

    /**  
     * Returns a String representation of the <code>&lt;Conditions&gt;</code>
     * element.
     *
     * @param includeNS Determines whether or not the namespace qualifier is 
     *        prepended to the Element when converted
     * @param declareNS Determines whether or not the namespace is declared
     *        within the Element.
     * @return A string containing the valid XML for this element.
    */                       
    public java.lang.String  toString(boolean includeNS, boolean declareNS) {
        StringBuffer xml = new StringBuffer(3000);
        String NS="";
        String appendNS="";
        if (declareNS) NS=sc.assertionDeclareStr;
        if (includeNS) appendNS="saml:";
        xml.append("<").append(appendNS).append("Conditions").append(" ").
            append(NS).append(" ");

        if (_notBefore != null) {
            xml.append("NotBefore")
                .append("=\"")
                .append(DateUtils.toUTCDateFormat(_notBefore))
                .append("\"")
                .append(" ");
        }

        if (_notOnOrAfter != null) {
            xml.append("NotOnOrAfter")
                .append("=\"")
                .append(DateUtils.toUTCDateFormat(_notOnOrAfter))
                .append("\"")
                .append(" ");
        }

        xml.append(">").append(sc.NL);

        Iterator it;
        if (_arcs.size() > 0 ) {
            it = _arcs.iterator();
            while (it.hasNext()) {
                xml.append(((AudienceRestrictionCondition)it.next()).
                    toString(includeNS, false));
                // false above as we dont want to have nested multiple 
                // declarations of namespace
            }
        }
        
        if (doNotCache != null) {
            xml.append(doNotCache.toString(includeNS, false));
        }
        String o = SAMLUtilsCommon.makeEndElementTagXML("Conditions",includeNS);
        xml.append(o);
        return xml.toString();
    }                                                       

    protected AudienceRestrictionCondition
        createAudienceRestrictionCondition(Element audienceRestrictionElement)
            throws SAMLException {
        return new AudienceRestrictionCondition(audienceRestrictionElement);
    }
    
    protected DoNotCacheCondition
        createDoNotCacheCondition(Element doNotCacheConditionElement)
            throws SAMLException {
        return new DoNotCacheCondition(doNotCacheConditionElement);
    }

}
