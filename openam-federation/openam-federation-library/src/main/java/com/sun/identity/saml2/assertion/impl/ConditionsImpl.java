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
 * $Id: ConditionsImpl.java,v 1.3 2008/06/25 05:47:43 qcheng Exp $
 *
 */


package com.sun.identity.saml2.assertion.impl;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import java.text.ParseException;

import com.sun.identity.shared.xml.XMLUtils;

import com.sun.identity.saml2.assertion.Conditions;
import com.sun.identity.saml2.assertion.Condition;
import com.sun.identity.saml2.assertion.OneTimeUse;
import com.sun.identity.saml2.assertion.AudienceRestriction;
import com.sun.identity.saml2.assertion.ProxyRestriction;
import com.sun.identity.saml2.assertion.AssertionFactory;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2SDKUtils;
import com.sun.identity.shared.DateUtils;

/**
 * The <code>Conditions</code> defines the SAML constructs that place
 * constraints on the acceptable use if SAML <code>Assertion</code>s.
 */
public class ConditionsImpl implements Conditions {

    private Date notOnOrAfter;
    private List conditions = new ArrayList();
    private List audienceRestrictions = new ArrayList();
    private List oneTimeUses = new ArrayList();
    private List proxyRestrictions = new ArrayList();
    private Date notBefore;
    private boolean isMutable = true;

    public static final String CONDITIONS_ELEMENT = "Conditions"; 
    public static final String CONDITION_ELEMENT = "Condition"; 
    public static final String ONETIMEUSE_ELEMENT = "OneTimeUse"; 
    public static final String AUDIENCE_RESTRICTION_ELEMENT = 
                               "AudienceRestriction"; 
    public static final String PROXY_RESTRICTION_ELEMENT = 
                               "ProxyRestriction"; 
    public static final String NOT_BEFORE_ATTR = "NotBefore"; 
    public static final String NOT_ON_OR_AFTER_ATTR = "NotOnOrAfter"; 

    /** 
     * Default constructor
     */
    public ConditionsImpl() {
    }

    /**
     * This constructor is used to build <code>Conditions</code> object
     * from a XML string.
     *
     * @param xml A <code>java.lang.String</code> representing
     *        a <code>Conditions</code> object
     * @exception SAML2Exception if it could not process the XML string
     */
    public ConditionsImpl(String xml) throws SAML2Exception {
        Document document = XMLUtils.toDOMDocument(xml, SAML2SDKUtils.debug);
        if (document != null) {
            Element rootElement = document.getDocumentElement();
            processElement(rootElement);
            makeImmutable();
        } else {
            SAML2SDKUtils.debug.error(
                "ConditionsImpl.processElement(): invalid XML input");
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
                "errorObtainingElement"));
        }
    }

    /**
     * This constructor is used to build <code>Conditions</code> object from a
     * block of existing XML that has already been built into a DOM.
     *
     * @param element A <code>org.w3c.dom.Element</code> representing
     *        DOM tree for <code>Conditions</code> object
     * @exception SAML2Exception if it could not process the Element
     */
    public ConditionsImpl(Element element) throws SAML2Exception {
        processElement(element);
        makeImmutable();
    }

    private void processElement(Element element) throws SAML2Exception {
        if (element == null) {
            SAML2SDKUtils.debug.error(
                "ConditionsImpl.processElement(): invalid root element");
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
                "invalid_element"));
        }
        String elemName = element.getLocalName(); 
        if (elemName == null) {
            SAML2SDKUtils.debug.error(
                "ConditionsImpl.processElement(): local name missing");
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
                "missing_local_name"));
        }

        if (!elemName.equals(CONDITIONS_ELEMENT)) {
            SAML2SDKUtils.debug.error(
                "ConditionsImpl.processElement(): invalid local name " 
                + elemName);
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
                "invalid_local_name"));
        }

        // starts processing attributes
        String attrValue = element.getAttribute(NOT_BEFORE_ATTR);
        if ((attrValue != null) && (attrValue.trim().length() != 0)) {
            try {
                notBefore = DateUtils.stringToDate(attrValue);   
            } catch (ParseException pe) {
                SAML2SDKUtils.debug.error("ConditionsImpl.processElement():"
                   + " invalid NotBefore attribute");
                throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
                    "invalid_date_format"));
            } 
        } 
        attrValue = element.getAttribute(NOT_ON_OR_AFTER_ATTR);
        if ((attrValue != null) && (attrValue.trim().length() != 0)) {
            try {
                notOnOrAfter = DateUtils.stringToDate(attrValue);   
            } catch (ParseException pe) {
                SAML2SDKUtils.debug.error("ConditionsImpl.processElement():"
                   + " invalid NotOnORAfter attribute");
                throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
                    "invalid_date_format"));
            } 
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
                    if (childName.equals(CONDITION_ELEMENT)) {
                        conditions.add(AssertionFactory.
                            getInstance().createCondition(
                            (Element)child));
                    } else if (childName.equals(AUDIENCE_RESTRICTION_ELEMENT)) {
                        audienceRestrictions.add(AssertionFactory.
                            getInstance().createAudienceRestriction(
                            (Element)child));
                    } else if (childName.equals(ONETIMEUSE_ELEMENT)) {
                        oneTimeUses.add(AssertionFactory.getInstance().
                            createOneTimeUse((Element)child));
                    } else if (childName.equals(PROXY_RESTRICTION_ELEMENT)) {
                        proxyRestrictions.add(AssertionFactory.
                            getInstance().createProxyRestriction(
                            (Element)child));
                    } else {
                        SAML2SDKUtils.debug.error("ConditionsImpl."
                            +"processElement(): unexpected subelement "
                            + childName);
                        throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
                           "unexpected_subelement"));
                    }
                }
            }
            nextElem++;
        }
    }

    /**
     * Returns the time instant at which the subject can no longer 
     *    be confirmed. 
     *
     * @return the time instant at which the subject can no longer 
     *    be confirmed.
     */
    public Date getNotOnOrAfter() {
        return notOnOrAfter;
    }

    /**
     * Sets the time instant at which the subject can no longer 
     *    be confirmed. 
     *
     * @param value the time instant at which the subject can no longer
     *    be confirmed.
     * @exception SAML2Exception if the object is immutable
     */
    public void setNotOnOrAfter(Date value) throws SAML2Exception {
        if (!isMutable) {
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
                "objectImmutable"));
        }  
        notOnOrAfter = value;
    }

    /** 
     *  Returns a list of <code>Condition</code>
     * 
     *  @return a list of <code>Condition</code>
     */
    public List getConditions() {
        return conditions;
    }
 
    /** 
     *  Returns a list of <code>AudienceRestriction</code>
     * 
     *  @return a list of <code>AudienceRestriction</code>
     */
    public List getAudienceRestrictions() {
        return audienceRestrictions;
    }
 
    /** 
     *  Returns a list of <code>OneTimeUse</code>
     * 
     *  @return a list of <code>OneTimeUse</code>
     */
    public List getOneTimeUses() {
        return oneTimeUses;
    }
 
    /** 
     *  Returns a list of <code>ProxyRestriction</code>  
     * 
     *  @return a list of <code>ProxyRestriction</code>
     */
    public List getProxyRestrictions() {
        return proxyRestrictions;
    }
 
    /** 
     *  Sets a list of <code>Condition</code>
     * 
     *  @param conditions a list of <code>Condition</code>
     *  @exception SAML2Exception if the object is immutable
     */
    public void setConditions(List conditions) throws SAML2Exception {
        if (!isMutable) {
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
                "objectImmutable"));
        } 
        this.conditions = conditions;
   }
 
    /** 
     *  Sets a list of <code>AudienceRestriction</code>
     * 
     *  @param ars a list of <code>AudienceRestriction</code>
     *  @exception SAML2Exception if the object is immutable
     */
    public void setAudienceRestrictions(List ars) throws SAML2Exception {
        if (!isMutable) {
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
                "objectImmutable"));
        } 
        audienceRestrictions = ars;
   }
 
    /** 
     *  Sets a list of <code>OneTimeUse</code>
     * 
     *  @param oneTimeUses a list of <code>OneTimeUse</code>
     *  @exception SAML2Exception if the object is immutable
     */
    public void setOneTimeUses(List oneTimeUses) throws SAML2Exception {
        if (!isMutable) {
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
                "objectImmutable"));
        } 
        this.oneTimeUses = oneTimeUses;
   }
 
    /** 
     *  Sets a list of <code>ProxyRestriction</code>  
     * 
     *  @param prs a list of <code>ProxyRestriction</code>
     *  @exception SAML2Exception if the object is immutable
     */
    public void setProxyRestrictions(List prs) throws SAML2Exception {
        if (!isMutable) {
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
                "objectImmutable"));
        } 
        proxyRestrictions = prs;
   }
 
    /**
     * Returns the time instant before which the subject cannot be confirmed.
     *
     * @return the time instant before which the subject cannot be confirmed.
     */
    public Date getNotBefore() {
        return notBefore;
    }

    /**
     * Sets the time instant before which the subject cannot 
     *     be confirmed.
     *
     * @param value the time instant before which the subject cannot 
     *     be confirmed.
     * @exception SAML2Exception if the object is immutable
     */
    public void setNotBefore(Date value) throws SAML2Exception {
        if (!isMutable) {
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
                "objectImmutable"));
        } 
        notBefore = value;
   }

    /**
     * Return true if a specific Date falls within the validity 
     * interval of this set of conditions.
     *
     * @param someTime Any time in milliseconds. 
     * @return true if <code>someTime</code> is within the valid 
     * interval of the <code>Conditions</code>.     
     */
    public boolean checkDateValidity(long someTime) {
        if (notBefore == null ) {
            if (notOnOrAfter == null) {
                return true;
            } else {
                if (someTime < notOnOrAfter.getTime()) {
                    return true;
                }
            }
        } else if (notOnOrAfter == null ) {
            if (someTime >= notBefore.getTime()) {
                return true;
            }
        } else if ((someTime >= notBefore.getTime()) && 
            (someTime < notOnOrAfter.getTime()))
        {
            return true; 
        }
        return false;
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
        sb.append("<").append(appendNS).append(CONDITIONS_ELEMENT).append(NS);
        String str = null;
        if (notBefore != null) {
            str = DateUtils.toUTCDateFormat(notBefore);
            sb.append(" ").append(NOT_BEFORE_ATTR).append("=\"").
                append(str).append("\"");
        } 
        if (notOnOrAfter != null) {
            str = DateUtils.toUTCDateFormat(notOnOrAfter);
            sb.append(" ").append(NOT_ON_OR_AFTER_ATTR).append("=\"").
                append(str).append("\"");
        } 
        sb.append(">\n");
        int length = 0;
        if (conditions != null) {
            length = conditions.size();
            for (int i = 0; i < length; i++) {
                Condition condition = (Condition)conditions.get(i);
                sb.append(condition.toXMLString(includeNSPrefix, false));
            }
        }
        if (audienceRestrictions != null) {
            length = audienceRestrictions.size();
            for (int i = 0; i < length; i++) {
                AudienceRestriction ar = 
                    (AudienceRestriction)audienceRestrictions.get(i);
                sb.append(ar.toXMLString(includeNSPrefix, false));
            }
        }
        if (oneTimeUses != null) {
            length = oneTimeUses.size();
            for (int i = 0; i < length; i++) {
                OneTimeUse ar = 
                    (OneTimeUse)oneTimeUses.get(i);
                sb.append(ar.toXMLString(includeNSPrefix, false));
            }
        }
        if (proxyRestrictions != null) {
            length = proxyRestrictions.size();
            for (int i = 0; i < length; i++) {
                ProxyRestriction pr = 
                    (ProxyRestriction)proxyRestrictions.get(i);
                sb.append(pr.toXMLString(includeNSPrefix, false));
            }
        }
        sb.append("</").append(appendNS).append(CONDITIONS_ELEMENT).
           append(">\n");
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
            if (conditions != null) {
                int length = conditions.size();
                for (int i = 0; i < length; i++) {
                    Condition condition = (Condition)conditions.get(i);
                    condition.makeImmutable();
                }
                conditions = Collections.unmodifiableList(conditions);
            }
            if (audienceRestrictions != null) {
                int length = audienceRestrictions.size();
                for (int i = 0; i < length; i++) {
                    AudienceRestriction ar =
                        (AudienceRestriction)audienceRestrictions.get(i);
                    ar.makeImmutable();
                }
                audienceRestrictions = Collections.unmodifiableList(
                                              audienceRestrictions);
            }
            if (oneTimeUses != null) {
                int length = oneTimeUses.size();
                for (int i = 0; i < length; i++) {
                    OneTimeUse oneTimeUse = (OneTimeUse)oneTimeUses.get(i);
                    oneTimeUse.makeImmutable();
                }
                oneTimeUses = Collections.unmodifiableList(oneTimeUses);
            }
            if (proxyRestrictions != null) {
                int length = proxyRestrictions.size();
                for (int i = 0; i < length; i++) {
                    ProxyRestriction pr =
                        (ProxyRestriction)proxyRestrictions.get(i);
                    pr.makeImmutable();
                }
                proxyRestrictions = Collections.unmodifiableList(
                                              proxyRestrictions);
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
