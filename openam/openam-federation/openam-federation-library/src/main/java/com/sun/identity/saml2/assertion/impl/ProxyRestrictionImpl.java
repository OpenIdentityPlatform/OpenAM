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
 * $Id: ProxyRestrictionImpl.java,v 1.2 2008/06/25 05:47:44 qcheng Exp $
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
import com.sun.identity.saml2.assertion.ProxyRestriction;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2SDKUtils;

/**
 *  The <code>ProxyRestriction</code> specifies limitations that the 
 *  asserting party imposes on relying parties that in turn wish to
 *  act as asserting parties and issue subsequent assertions of their 
 *  own on the basis of the information contained in the original
 *  assertion. A relying party acting as an asserting party must not
 *  issue an assertion that itself violates the restrictions specified 
 *  in this condition on the basis of an assertion containing such
 *  a condition.
 */
public class ProxyRestrictionImpl implements ProxyRestriction {

    private int count;
    private boolean isMutable = true;
    private List audiences = new ArrayList();

    public static final String PROXY_RESTRICTION_ELEMENT = "ProxyRestriction";
    public static final String COUNT_ATTR = "Count";
    public static final String AUDIENCE_ELEMENT = "Audience";

   /** 
    * Default constructor
    */
    public ProxyRestrictionImpl() {
    }

    /**
     * This constructor is used to build <code>ProxyRestriction</code> 
     * object from a XML string.
     *
     * @param xml A <code>java.lang.String</code> representing
     *        a <code>ProxyRestriction</code> object
     * @exception SAML2Exception if it could not process the XML string
     */
    public ProxyRestrictionImpl(String xml) throws SAML2Exception {
        Document document = XMLUtils.toDOMDocument(xml, SAML2SDKUtils.debug);
        if (document != null) {
            Element rootElement = document.getDocumentElement();
            processElement(rootElement);
            makeImmutable();
        } else {
            SAML2SDKUtils.debug.error(
                "ProxyRestrictionImpl.processElement(): invalid XML input");
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
                "errorObtainingElement"));
        }
    }

    /**
     * This constructor is used to build <code>ProxyRestriction</code> 
     * object from a block of existing XML that has already been built 
     * into a DOM.
     *
     * @param element A <code>org.w3c.dom.Element</code> representing
     *        DOM tree for <code>ProxyRestriction</code> object
     * @exception SAML2Exception if it could not process the Element
     */
    public ProxyRestrictionImpl(Element element) throws SAML2Exception {
        processElement(element);
        makeImmutable();
    }

    private void processElement(Element element) throws SAML2Exception {
        if (element == null) {
            SAML2SDKUtils.debug.error(
                "ProxyRestrictionImpl.processElement(): invalid root element");
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
                "invalid_element"));
        }
        String elemName = element.getLocalName(); 
        if (elemName == null) {
            SAML2SDKUtils.debug.error(
                "ProxyRestrictionImpl.processElement(): local name missing");
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
                "missing_local_name"));
        }

        if (!elemName.equals(PROXY_RESTRICTION_ELEMENT)) {
            SAML2SDKUtils.debug.error(
                "ProxyRestrictionImpl.processElement(): "
                + "invalid local name " + elemName);
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
                "invalid_local_name"));
        }

        // starts processing attributes
        String attrValue = element.getAttribute(COUNT_ATTR);
        if ((attrValue != null) && (attrValue.trim().length() != 0)) {
            try {
                setCount(Integer.parseInt(attrValue));
            } catch (NumberFormatException e) {
                SAML2SDKUtils.debug.error(
                    "ProxyRestrictionImpl.processElement(): "
                    + "count is not an integer " + attrValue);
                throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
                    "invalid_count_number"));
            }
        }
                
        // starts processing subelements
        NodeList nodes = element.getChildNodes();
        int numOfNodes = nodes.getLength();
        if (numOfNodes < 1) {
            return;
        }
    
        int nextElem = 0;
        while (nextElem < numOfNodes) { 
            Node child = (Node)nodes.item(nextElem);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                String childName = child.getLocalName();
                if (childName != null) {
                    if (childName.equals(AUDIENCE_ELEMENT)) {
                        audiences.add(
                            XMLUtils.getElementValue((Element)child));
                    } else {
                        SAML2SDKUtils.debug.error(
                            "AudienceRestrictionImpl.processElement(): "
                            + "unexpected subelement " + childName);
                        throw new SAML2Exception(SAML2SDKUtils.bundle.
                            getString("unexpected_subelement"));
                    }
                }
            }
            nextElem++;
        }
    }
 
    /**
     *  Returns the maximum number of indirections that the asserting
     *  party permits to exist between this assertion and an assertion
     *  which has ultimately been issued on the basis of it.
     *
     *  @return the count number
     */
    public int getCount() {
        return count;
    }

    /**
     *  Sets the maximum number of indirections that the asserting
     *  party permits to exist between this assertion and an assertion
     *  which has ultimately been issued on the basis of it.
     *
     *  @param value the count number
     *  @exception SAML2Exception if the object is immutable
     */
    public void setCount(int value) throws SAML2Exception {
        if (!isMutable) {
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
                "objectImmutable"));
        }
        if (count < 0) {
            SAML2SDKUtils.debug.error(
                "AudienceRestrictionImpl.setCount(): count is negative");
            throw new SAML2Exception(SAML2SDKUtils.bundle.
                 getString("negative_count_number"));
        }   
        count = value;
    }

    /**
     *  Returns the list of audiences to whom the asserting party 
     *  permits new assertions to be issued on the basis of this 
     *  assertion.
     *
     *  @return a list of <code>String</code> represented audiences
     */
    public List getAudience() {
        return audiences;
    }

    /**
     *  Sets the list of audiences to whom the asserting party permits
     *  new assertions to be issued on the basis of this assertion.
     *
     *  @param audiences a list of <code>String</code> represented audiences
     *  @exception SAML2Exception if the object is immutable
     */
    public void setAudience(List audiences) throws SAML2Exception {
        if (!isMutable) {
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
                "objectImmutable"));
        }
        this.audiences = audiences;
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
        sb.append("<").append(appendNS).append(PROXY_RESTRICTION_ELEMENT).
            append(NS);
        if (count >= 0) {
            sb.append(" ").append(COUNT_ATTR).append("=\"").
                append(count).append("\"");
        }
        sb.append(">\n");

        int length = 0;
        if ((audiences != null) && ((length = audiences.size()) > 0)) {
            for (int i = 0; i < length; i++) {
                String au = (String)audiences.get(i);
                sb.append("<").append(appendNS).append(AUDIENCE_ELEMENT).
                    append(">").append(au).append("</").append(appendNS).
                    append(AUDIENCE_ELEMENT).append(">\n");
            }
        }
        sb.append("</").append(appendNS).
            append(PROXY_RESTRICTION_ELEMENT).append(">\n");

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
            if (audiences != null) {
                audiences = Collections.unmodifiableList(audiences);
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
