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
 * $Id: DecisionImpl.java,v 1.3 2008/06/25 05:48:12 qcheng Exp $
 *
 */

package com.sun.identity.xacml.context.impl;

import com.sun.identity.shared.xml.XMLUtils;

import com.sun.identity.xacml.common.XACMLConstants;
import com.sun.identity.xacml.common.XACMLException;
import com.sun.identity.xacml.common.XACMLSDKUtils;
import com.sun.identity.xacml.context.Decision;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * The <code>Decision</code> element is a container of 
 * one or more <code>Decision</code>s issued by policy decision point
 * @supported.all.api
 * <p/>
 * Schema:
 * <pre>
 * <xs:simpleType name="DecisionType">
 *     <xs:restriction base="xs:string">
 *         <xs:enumeration value="Permit"/>
 *         <xs:enumeration value="Deny"/>
 *         <xs:enumeration value="Indeterminate"/>
 *         <xs:enumeration value="NotApplicable"/>
 *     </xs:restriction>
 * </xs:simpleType>
 * </pre>
 * 
 */
public class DecisionImpl implements Decision {

    private String value = null;
    private boolean mutable = true;

    /** 
     * Constructs a <code>Decision</code> object
     */
    public DecisionImpl() throws XACMLException {
    }

    /** 
     * Constructs a <code>Decision</code> object from an XML string
     *
     * @param xml string representing a <code>Decision</code> object
     * @throws XACMLException if the XML string could not be processed
     */
    public DecisionImpl(String xml) throws XACMLException {
        Document document = XMLUtils.toDOMDocument(xml, XACMLSDKUtils.debug);
        if (document != null) {
            Element rootElement = document.getDocumentElement();
            processElement(rootElement);
            makeImmutable();
        } else {
            XACMLSDKUtils.debug.error(
                "DecisionImpl.processElement(): invalid XML input");
            throw new XACMLException(
                XACMLSDKUtils.xacmlResourceBundle.getString(
                "errorObtainingElement"));
        }
    }

    /** 
     * Constructs a <code>Decision</code> object from an XML DOM element
     *
     * @param element XML DOM element representing a <code>Decision</code> 
     * object
     *
     * @throws XACMLException if the DOM element could not be processed
     */
    public DecisionImpl(Element element) throws XACMLException {
        processElement(element);
        makeImmutable();
    }

    /**
     * Returns the <code>value</code>s of this object
     *
     * @return the <code>value</code>s of this object
     */
    public String getValue() {
        return value;
    }

    /**
     * Sets the <code>value</code>s of this object
     *
     * @exception XACMLException if the object is immutable
     */
    public void setValue(String value) throws XACMLException {
        if (!mutable) {
            throw new XACMLException(
                XACMLSDKUtils.xacmlResourceBundle.getString("objectImmutable"));
        }

        if (value == null) {
            throw new XACMLException(
                XACMLSDKUtils.xacmlResourceBundle.getString("null_not_valid"));
        }

        if (!XACMLSDKUtils.isValidDecision(value)) {
            throw new XACMLException(
                XACMLSDKUtils.xacmlResourceBundle.getString("invalid_value"));
        }
        this.value = value; 
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
        if (declareNS) {
            nsDeclaration = XACMLConstants.CONTEXT_NS_DECLARATION;
        }
        if (includeNSPrefix) {
            nsPrefix = XACMLConstants.CONTEXT_NS_PREFIX + ":";
        }
        sb.append("<").append(nsPrefix).append(XACMLConstants.DECISION)
                .append(nsDeclaration).append(">");
        if (value != null) {
            sb.append(value);
        }
        sb.append("</").append(nsPrefix).append(XACMLConstants.DECISION)
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
                "DecisionImpl.processElement(): invalid root element");
            throw new XACMLException(XACMLSDKUtils.xacmlResourceBundle.getString(
                "invalid_element"));
        }
        String elemName = element.getLocalName();
        if (elemName == null) {
            XACMLSDKUtils.debug.error(
                "DecisionImpl.processElement(): local name missing");
            throw new XACMLException(XACMLSDKUtils.xacmlResourceBundle.getString(
                "missing_local_name"));
        }

        if (!elemName.equals(XACMLConstants.DECISION)) {
            XACMLSDKUtils.debug.error(
                    "DecisionImpl.processElement(): invalid local name " 
                    + elemName);
            throw new XACMLException(XACMLSDKUtils.xacmlResourceBundle.getString(
                    "invalid_local_name"));
        }
        String elementValue = element.getTextContent();
        if (elementValue == null) {
            throw new XACMLException(
                    XACMLSDKUtils.xacmlResourceBundle.getString("null_not_valid"));
        }
        if (!XACMLSDKUtils.isValidDecision(elementValue.trim())) {
            throw new XACMLException(
                    XACMLSDKUtils.xacmlResourceBundle.getString("invalid_value"));
        } else {
            this.value = elementValue;
        }
    }
    
}
