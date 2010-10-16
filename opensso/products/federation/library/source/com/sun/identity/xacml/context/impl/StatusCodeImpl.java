/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: StatusCodeImpl.java,v 1.3 2008/06/25 05:48:13 qcheng Exp $
 *
 */

package com.sun.identity.xacml.context.impl;

import com.sun.identity.shared.xml.XMLUtils;

import com.sun.identity.xacml.common.XACMLConstants;
import com.sun.identity.xacml.common.XACMLException;
import com.sun.identity.xacml.common.XACMLSDKUtils;
import com.sun.identity.xacml.context.StatusCode;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * The <code>StatusCode</code> element is a container of 
 * one or more <code>StatusCode</code>s issuded by authorization authority.
 * @supported.all.api
 * <p/>
 * <pre>
 *
 * Schema:
 *  &lt;xs:element name="StatusCode" type="xacml-context:StatusCodeType"/>
 *  &lt;xs:complexType name="StatusCodeType">
 *      &lt;xs:sequence>
 *          &lt;xs:element ref="xacml-context:StatusCode" minOccurs="0"/>
 *      &lt;xs:sequence>
 *      &lt;xs:attribute name="Value" type="xs:anyURI" use="required"/>
 *  &lt;xs:complexType>
 * </pre>
 */
public class StatusCodeImpl implements StatusCode {


    String value = null;
    String minorCodeValue = null;
    private boolean mutable = true;

    /** 
     * Constructs a <code>StatusCode</code> object
     */
    public StatusCodeImpl() throws XACMLException {
    }

    /** 
     * Constructs a <code>StatusCode</code> object from an XML string
     *
     * @param xml string representing a <code>StatusCode</code> object
     * @throws SAMLException if the XML string could not be processed
     */
    public StatusCodeImpl(String xml) throws XACMLException {
        Document document = XMLUtils.toDOMDocument(xml, XACMLSDKUtils.debug);
        if (document != null) {
            Element rootElement = document.getDocumentElement();
            processElement(rootElement);
            makeImmutable();
        } else {
            XACMLSDKUtils.debug.error(
                "StatusCodeImpl.processElement(): invalid XML input");
            throw new XACMLException(
                XACMLSDKUtils.xacmlResourceBundle.getString(
                "errorObtainingElement"));
        }
    }

    /** 
     * Constructs a <code>StatusCode</code> object from an XML DOM element
     *
     * @param element XML DOM element representing a <code>StatusCode</code> 
     * object
     *
     * @throws SAMLException if the DOM element could not be processed
     */
    public StatusCodeImpl(Element element) throws XACMLException {
        processElement(element);
        makeImmutable();
    }

    /**
     * Returns the <code>value</code> of this object
     *
     * @return the <code>value</code> of this object
     */
    public String getValue() {
        return value;
    }

    /**
     * Sets the <code>value</code> of this object
     *
     * @exception XACMLException if the object is immutable
     */
    public void setValue(String value) throws XACMLException {
        if (!mutable) {
            throw new XACMLException(
                XACMLSDKUtils.xacmlResourceBundle.getString(
                "objectImmutable"));
        }

        if (value == null) {
            throw new XACMLException(
                XACMLSDKUtils.xacmlResourceBundle.getString("null_not_valid"));
        }

        if (!XACMLSDKUtils.isValidStatusCode(value)) {
            throw new XACMLException(
                XACMLSDKUtils.xacmlResourceBundle.getString("invalid_value"));
        }
        this.value = value;
    }

    /**
     * Returns the <code>minorCodeValue</code> of this object
     *
     * @return the <code>minorCodeValue</code> of this object
     */
    public String getMinorCodeValue() {
        return minorCodeValue;
    }

    /**
     * Sets the <code>minorCodeValue</code> of this object
     *
     * @exception XACMLException if the object is immutable
     */
    public void setMinorCodeValue(String minorCodeValue) 
            throws XACMLException {
        if (!mutable) {
            throw new XACMLException(
                XACMLSDKUtils.xacmlResourceBundle.getString("objectImmutable"));
        }

        if (value == null) {
            throw new XACMLException(
                XACMLSDKUtils.xacmlResourceBundle.getString("null_not_valid"));
        }

        if (!XACMLSDKUtils.isValidMinorStatusCode(value)) {
            throw new XACMLException(
                XACMLSDKUtils.xacmlResourceBundle.getString("invalid_value"));
        }
        this.minorCodeValue = minorCodeValue;
    }

   /**
    * Returns a string representation
    *
    * @return a string representation
    * @exception XACMLException if conversion fails for any reason
    */
    public String toXMLString() throws XACMLException {
        return toXMLString(true, false);
    }

   /**
    * Returns a string representation
    * @param includeNSPrefix Determines whether or not the namespace qualifier
    *        is prepended to the Element when converted
    * @param declareNS Determines whether or not the namespace is declared
    *        within the Element.
    * @return a string representation
    * @exception XACMLException if conversion fails for any reason
     */
    public String toXMLString(boolean includeNSPrefix, boolean declareNS)
            throws XACMLException {
        StringBuffer sb = new StringBuffer(2000);
        String nsPrefix = "";
        String nsDeclaration = "";
        if (includeNSPrefix) {
            nsPrefix = XACMLConstants.CONTEXT_NS_PREFIX + ":";
        }
        if (declareNS) {
            nsDeclaration = XACMLConstants.CONTEXT_NS_DECLARATION;
        }
        sb.append("<").append(nsPrefix)
                .append(XACMLConstants.STATUS_CODE)
                .append(" ")
                .append(nsDeclaration);
        if (value != null) {
            sb.append(XACMLConstants.VALUE)
            .append("=")
            .append(XACMLSDKUtils.quote(value));
        }
        sb.append(">");
        if (minorCodeValue != null) {
            sb.append("<").append(nsPrefix)
                    .append(XACMLConstants.STATUS_CODE)
                    .append(" ")
                    .append(nsDeclaration)
                    .append(XACMLConstants.VALUE)
                    .append("=")
                    .append(XACMLSDKUtils.quote(minorCodeValue))
                    .append(">");
                    sb.append("</").append(nsPrefix)
                            .append(XACMLConstants.STATUS_CODE)
                            .append(">");
        }
        sb.append("</").append(nsPrefix).append(XACMLConstants.STATUS_CODE)
                .append(">\n");
        return sb.toString();
    }

   /**
    * Checks if the object is mutable
    *
    * @return <code>true</code> if the object is mutable,
    *         <code>false</code> otherwise
    */
    public boolean isMutable() {
        return mutable;
    }
    
   /**
    * Makes the object immutable
    */
    public void makeImmutable() {
        mutable = false;
    }

    private void processElement(Element element) throws XACMLException {
        if (element == null) {
            XACMLSDKUtils.debug.error(
                "StatusMessageImpl.processElement(): invalid root element");
            throw new XACMLException(XACMLSDKUtils.xacmlResourceBundle.getString(
                "invalid_element"));
        }
        String elemName = element.getLocalName();
        if (elemName == null) {
            XACMLSDKUtils.debug.error(
                "StatusMessageImpl.processElement(): local name missing");
            throw new XACMLException(XACMLSDKUtils.xacmlResourceBundle.getString(
                "missing_local_name"));
        }

        if (!elemName.equals(XACMLConstants.STATUS_CODE)) {
            XACMLSDKUtils.debug.error(
                    "StatusMessageImpl.processElement(): invalid local name " 
                    + elemName);
            throw new XACMLException(XACMLSDKUtils.xacmlResourceBundle.getString(
                    "invalid_local_name"));
        }
        String attrValue = element.getAttribute(XACMLConstants.VALUE);
        if ((attrValue == null) || (attrValue.length() == 0)) {
            XACMLSDKUtils.debug.error(
                "StatusCodeImpl.processElement(): statuscode missing");
            throw new XACMLException(XACMLSDKUtils.xacmlResourceBundle.getString(
                "missing_status_code")); //i18n
        } 
        if (!XACMLSDKUtils.isValidStatusMessage(attrValue.trim())) {
            throw new XACMLException(
                    XACMLSDKUtils.xacmlResourceBundle.getString(
                    "invalid_value"));
        } else {
            this.value = attrValue;
        }
        //process child StatusCode element
        NodeList nodes = element.getChildNodes();
        int numOfNodes = nodes.getLength();
        List childElements = new ArrayList(); 
        int i = 0;
        while (i < numOfNodes) { 
            Node child = (Node) nodes.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                childElements.add(child);
            }
           i++;
        }
        int childCount = childElements.size();
        if (childCount > 1) {
            XACMLSDKUtils.debug.error(
                "ResultImpl.processElement(): invalid child element count: " 
                        + childCount);
            throw new XACMLException(XACMLSDKUtils.xacmlResourceBundle.getString(
                "invalid_child_count"));
        }
        if (childCount == 1) {
            Element childElement = (Element)childElements.get(0);
            elemName = childElement.getLocalName();
            if (elemName == null) {
                XACMLSDKUtils.debug.error(
                    "StatusMessageImpl.processElement(): local name missing");
                throw new XACMLException(
                    XACMLSDKUtils.xacmlResourceBundle.getString(
                    "missing_local_name"));
            }

            if (!elemName.equals(XACMLConstants.STATUS_CODE)) {
                XACMLSDKUtils.debug.error(
                        "StatusMessageImpl.processElement(): invalid local name " 
                        + elemName);
                throw new XACMLException(
                    XACMLSDKUtils.xacmlResourceBundle.getString(
                        "invalid_local_name"));
            }
            attrValue = childElement.getAttribute(XACMLConstants.VALUE);
            if ((attrValue == null) || (attrValue.length() == 0)) {
                XACMLSDKUtils.debug.error(
                    "StatusCodeImpl.processElement(): minor statuscode missing");
                throw new XACMLException(
                    XACMLSDKUtils.xacmlResourceBundle.getString(
                    "missing_minor_status_code"));
            } 
            if (!XACMLSDKUtils.isValidStatusMessage(attrValue.trim())) {
                throw new XACMLException(
                        XACMLSDKUtils.xacmlResourceBundle.getString(
                        "invalid_value"));
            } else {
                this.minorCodeValue = attrValue;
            }
        } else {
        }
    }

}
