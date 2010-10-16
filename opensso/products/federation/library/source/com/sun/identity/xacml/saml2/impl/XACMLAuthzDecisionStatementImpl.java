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
 * $Id: XACMLAuthzDecisionStatementImpl.java,v 1.4 2008/11/10 22:57:06 veiming Exp $
 *
 */



package com.sun.identity.xacml.saml2.impl;

import com.sun.identity.shared.xml.XMLUtils;

import com.sun.identity.xacml.common.XACMLConstants;
import com.sun.identity.xacml.common.XACMLException;
import com.sun.identity.xacml.common.XACMLSDKUtils;
import com.sun.identity.xacml.context.ContextFactory;
import com.sun.identity.xacml.context.Request;
import com.sun.identity.xacml.context.Response;
import com.sun.identity.xacml.saml2.XACMLAuthzDecisionStatement;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * This is the default implementation of interface <code>XACMLAuthzDecisionStatement</code>.
 *
 * <p>
 * <pre>
 * &lt;xs:element name="XACMLAuthzDecisionStatement"
 *          type="xacml-saml:XACMLAuthzDecisionStatementType"/>
 * &lt;xs:complexType name="XACMLAuthzDecisionStatementType">
 *   &lt;xs:complexContent>
 *     &lt;xs:extension base="saml:StatementAbstractType">
 *      &lt;xs:sequence>
 *        &lt;xs:element ref="xacml-context:Response"/>
 *        &lt;xs:element ref="xacml-context:Request"  minOccurs="0"/>
 *      &lt;xs:sequence>
 *    &lt;xs:extension>
 *  &lt;xs:complexContent>
 *&lt;xs:complexType>
 * </pre>
 * </p>
 *
 * Schema for the base type is
 * <p>
 * <pre>
 * &lt;complexType name="StatementAbstractType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * </p>
 *
 */
public class XACMLAuthzDecisionStatementImpl 
        implements XACMLAuthzDecisionStatement {

    private Response response = null;
    private Request request = null;
    private boolean mutable = true;

    /**
     * Constructs an <code>XACMLAuthzDecisionStatement</code> object
     */
    public XACMLAuthzDecisionStatementImpl() {
    }

    /** 
     * Constructs an <code>XACMLAuthzDecisionStatementImpl</code> object 
     * from an XML string
     *
     * @param xml string representing an 
     * <code>XACMLAuthzDecisionStatementImpl</code> object
     *
     * @exception XACMLException if the XML string could not be processed
     */
    public XACMLAuthzDecisionStatementImpl(String xml)
            throws XACMLException {
        Document document = XMLUtils.toDOMDocument(xml, XACMLSDKUtils.debug);
        if (document != null) {
            Element rootElement = document.getDocumentElement();
            processElement(rootElement);
            makeImmutable();
        } else {
            XACMLSDKUtils.debug.error(
                "DecisionImpl.processElement(): invalid XML input");
            throw new XACMLException(XACMLSDKUtils.xacmlResourceBundle.getString(
                "errorObtainingElement"));
        }
    }

    /** 
     * Constructs an <code>XACMLAuthzDecisionStatementImpl</code> object 
     * from an XML DOM element
     *
     * @param element XML DOM element representing an 
     * <code>XACMLAuthzDecisionStatementImpl</code> 
     * object
     *
     * @throws XACMLException if the DOM element could not be processed
     */
    public XACMLAuthzDecisionStatementImpl(org.w3c.dom.Element element)
            throws XACMLException {
        processElement(element);
        makeImmutable();
    }

    /**
     * Returns <code>Response</code> element of this object
     *
     * @return the <code>Response</code> element of this object
     */
   public Response getResponse() {
       return response;
   }

    /**
     * Sets <code>Response</code> element of this object
     *
     * @param response XACML context <code>Response</code> element to be 
     *        set in this object.
     * @throws XACMLException if the object is immutable and response is
     *         null.
     */
   public void setResponse(Response response) 
        throws XACMLException {
           if (!mutable) {
            throw new XACMLException(
                XACMLSDKUtils.xacmlResourceBundle.getString("objectImmutable"));
        }

        if (response == null) {
            throw new XACMLException(
                XACMLSDKUtils.xacmlResourceBundle.getString("null_not_valid"));
        }

        this.response = response; 
    }


    /**
     * Returns <code>Request</code> element of this object
     *
     * @return the <code>Request</code> element of this object
     */
   public Request getRequest() {
       return request;
   }

    /**
     * Sets <code>Request</code> element of this object.
     *
     * @param request XACML context <code>Request</code> element to be 
     *        set in this object.
     * @throws XACMLException if the object is immutable.
     */
   public void setRequest(Request request) 
        throws XACMLException {
           if (!mutable) {
            throw new XACMLException(
                XACMLSDKUtils.xacmlResourceBundle.getString("objectImmutable"));
        }
        this.request = request; 
   }

   /**
    * Returns a string representation
    *
    * @return a string representation
    * @exception XACMLException if conversion fails for any reason
    */
    public String toXMLString() throws XACMLException {
        //top level element, declare namespace
        return toXMLString(true, true);
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
        String xacmlSamlNsPrefix = "";
        String xacmlSamlNsDeclaration = "";
        if (declareNS) {
            xacmlSamlNsDeclaration = XACMLConstants.XACML_SAML_NS_DECLARATION;
        }
        if (includeNSPrefix) {
            xacmlSamlNsPrefix = XACMLConstants.XACML_SAML_NS_PREFIX;
        }
        sb.append("\n<")
                .append(XACMLConstants.SAML_NS_PREFIX)
                .append(XACMLConstants.SAML_STATEMENT)
                .append(XACMLConstants.SAML_NS_DECLARATION)
                .append(XACMLConstants.XSI_TYPE_XACML_AUTHZ_DECISION_STATEMENT)
                .append(XACMLConstants.XSI_NS_DECLARATION)
                .append(XACMLConstants.XACML_SAML_NS_DECLARATION)
                .append(">\n");
        if (response != null) {
            sb.append(response.toXMLString(includeNSPrefix, true));
        }
        if (request != null) {
            sb.append(request.toXMLString(includeNSPrefix, true));
        }
        sb.append("</")
                .append(XACMLConstants.SAML_NS_PREFIX)
                .append(XACMLConstants.SAML_STATEMENT)
                .append(">");
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

        if (!elemName.equals(XACMLConstants.SAML_STATEMENT)) {
            XACMLSDKUtils.debug.error(
                    "DecisionImpl.processElement(): invalid local name " 
                    + elemName);
            throw new XACMLException(XACMLSDKUtils.xacmlResourceBundle.getString(
                    "invalid_local_name"));
        }
        //TODO: add a check for xsi:type

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
        if (childCount < 1) {
            XACMLSDKUtils.debug.error(
                "ResultImpl.processElement(): invalid child element count: " 
                        + childCount);
            throw new XACMLException(XACMLSDKUtils.xacmlResourceBundle.getString(
                "invalid_child_count")); //FIXME: add i18n key
        } else if (childCount > 2) {
            XACMLSDKUtils.debug.error(
                "ResultImpl.processElement(): invalid child element count: " 
                        + childCount);
            throw new XACMLException(XACMLSDKUtils.xacmlResourceBundle.getString(
                "invalid_child_count")); //FIXME: add i18n key
        }

        //process Response element
        Element firstChild = (Element)childElements.get(0);
        String firstChildName = firstChild.getLocalName();
        if (firstChildName.equals(XACMLConstants.RESPONSE)) {
            response =  ContextFactory.getInstance()
                    .createResponse(firstChild);
        } else {
            XACMLSDKUtils.debug.error(
                "ResultImpl.processElement(): invalid first child element: " 
                        + firstChildName);
            throw new XACMLException(XACMLSDKUtils.xacmlResourceBundle.getString(
                "invalid_first_child")); //FIXME: add i18n key
        }

        //process Request element
        if (childCount > 1) {
            Element secondChild = (Element)childElements.get(1);
            String secondChildName = secondChild.getLocalName();
            if (secondChildName.equals(XACMLConstants.REQUEST)) {
                request =  ContextFactory.getInstance()
                        .createRequest(secondChild);

            } else {
                XACMLSDKUtils.debug.error(
                    "ResultImpl.processElement(): invalid second child element: " 
                            + secondChildName);
                throw new XACMLException(XACMLSDKUtils.xacmlResourceBundle.getString(
                    "invalid_second_child")); //FIXME: add i18n key
            }
            if (childCount > 2) {
                Element thirdChild = (Element)childElements.get(2);
                String thirdChildName = thirdChild.getLocalName();
                XACMLSDKUtils.debug.error(
                    "ResultImpl.processElement(): invalid third child element: " 
                            + thirdChildName);
                throw new XACMLException(XACMLSDKUtils.xacmlResourceBundle.getString(
                    "invalid_third_child")); //FIXME: add i18n key
            }
        }

    }

}
