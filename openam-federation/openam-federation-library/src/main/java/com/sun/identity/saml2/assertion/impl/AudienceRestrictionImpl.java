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
 * $Id: AudienceRestrictionImpl.java,v 1.2 2008/06/25 05:47:42 qcheng Exp $
 *
 * Portions Copyrighted 2014 ForgeRock AS.
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
import com.sun.identity.saml2.assertion.AudienceRestriction;
import com.sun.identity.saml2.common.SAML2SDKUtils;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.common.SAML2Exception;

/**
 *  The <code>AudienceRestriction</code> specifies that the assertion
 *  is addressed to one or more specific <code>Audience</code>s.
 */
public class AudienceRestrictionImpl 
    extends ConditionAbstractImpl implements AudienceRestriction {

    private List<String> audiences = new ArrayList<String>(1);
    private boolean isMutable = true;

    public static String AUDIENCE_RESTRICTION_ELEMENT = "AudienceRestriction";
    public static String AUDIENCE_ELEMENT = "Audience";

   /** 
    * Default constructor
    */
    public AudienceRestrictionImpl() {
    }

    /**
     * This constructor is used to build <code>AudienceRestriction</code>
     * object from a XML string.
     *
     * @param xml A <code>java.lang.String</code> representing
     *        a <code>AudienceRestriction</code> object
     * @exception SAML2Exception if it could not process the XML string
     */
    public AudienceRestrictionImpl(String xml) throws SAML2Exception {
        Document document = XMLUtils.toDOMDocument(xml, SAML2SDKUtils.debug);
        if (document != null) {
            Element rootElement = document.getDocumentElement();
            processElement(rootElement);
            makeImmutable();
        } else {
            SAML2SDKUtils.debug.error(
                "AudienceRestrictionImpl.processElement(): invalid XML input");
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
                "errorObtainingElement"));
        }
    }

    /**
     * This constructor is used to build <code>AudienceRestriction</code>
     * object from a block of existing XML that has already been built 
     * into a DOM.
     *
     * @param element A <code>org.w3c.dom.Element</code> representing
     *        DOM tree for <code>AudienceRestriction</code> object
     * @exception SAML2Exception if it could not process the Element
     */
    public AudienceRestrictionImpl(Element element) 
        throws SAML2Exception {
        processElement(element);
        makeImmutable();
    }

    private void processElement(Element element) throws SAML2Exception {
        if (element == null) {
            SAML2SDKUtils.debug.error(
                "AudienceRestrictionImpl.processElement(): "
                + "invalid root element");
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
                "invalid_element"));
        }
        String elemName = element.getLocalName(); 
        if (elemName == null) {
            SAML2SDKUtils.debug.error(
                "AudienceRestrictionImpl.processElement(): "
                + "local name missing");
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
                "missing_local_name"));
        }

        if (!elemName.equals(AUDIENCE_RESTRICTION_ELEMENT)) {
            SAML2SDKUtils.debug.error(
                "AudienceRestrictionImpl.processElement(): "
                + "invalid local name " + elemName);
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
                "invalid_local_name"));
        }

        // starts processing subelements
        NodeList nodes = element.getChildNodes();
        int numOfNodes = nodes.getLength();
        if (numOfNodes < 1) {
            SAML2SDKUtils.debug.error(
                "AudienceRestrictionImpl.processElement(): "
                + "AudienceRestriction has no subelements");
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
                "missing_subelements"));
        }
    
        int nextElem = 0;
        boolean hasAudience = false; 
        // The next subelements should all be <Audiences>
        while (nextElem < numOfNodes) { 
            Node child = (Node)nodes.item(nextElem);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                String childName = child.getLocalName();
                if (childName != null) {
                    if (childName.equals(AUDIENCE_ELEMENT)) {
                        audiences.add(XMLUtils.getElementValue((Element)child));
                        hasAudience = true;
                    } else {
                        SAML2SDKUtils.debug.error(
                            "AudienceRestrictionImpl.processElement(): "
                            + "unexpected subelement " + childName);
                        throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
                           "unexpected_subelement"));
                    }
                }
            }
            nextElem++;
        }
        if (!hasAudience) {
            SAML2SDKUtils.debug.error(
                "AudienceRestrictionImpl.processElement(): "
                + "AudienceRestriction has no subelements");
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
                "missing_subelements"));
        }
    }

    /**
     * Returns a list of <code>String</code> represented audiences
     *
     * @return a list of <code>String</code> represented audiences
     */
    public List<String> getAudience() {
        return audiences;
    }

    /**
     * Sets the audiences
     *
     * @param audiences List of audiences as URI strings
     * @exception SAML2Exception if the object is immutable
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
        StringBuilder sb = new StringBuilder(2000);
        String NS = "";
        String appendNS = "";
        if (declareNS) {
            NS = SAML2Constants.ASSERTION_DECLARE_STR;
        }
        if (includeNSPrefix) {
            appendNS = SAML2Constants.ASSERTION_PREFIX;
        }
        sb.append("<").append(appendNS).
            append(AUDIENCE_RESTRICTION_ELEMENT).append(NS).append(">\n");
        if (audiences != null) {
            for (String audience : audiences) {
                sb.append("<").append(appendNS).append(AUDIENCE_ELEMENT).append(">").
                        append(XMLUtils.escapeSpecialCharacters(audience)).
                        append("</").append(appendNS).append(AUDIENCE_ELEMENT).append(">\n");
            }
        } else {
            SAML2SDKUtils.debug.error(
                "AudienceRestrictionImpl.processElement(): "
                + "AudienceRestriction has no subelements");
            throw new SAML2Exception(SAML2SDKUtils.bundle.getString(
                "missing_subelements"));
        }
        sb.append("</").append(appendNS).
            append(AUDIENCE_RESTRICTION_ELEMENT).append(">\n");
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
