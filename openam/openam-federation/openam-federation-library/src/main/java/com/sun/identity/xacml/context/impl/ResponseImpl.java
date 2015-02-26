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
 * $Id: ResponseImpl.java,v 1.4 2008/11/10 22:57:05 veiming Exp $
 *
 */

package com.sun.identity.xacml.context.impl;

import com.sun.identity.shared.xml.XMLUtils;

import com.sun.identity.xacml.common.XACMLConstants;
import com.sun.identity.xacml.common.XACMLException;
import com.sun.identity.xacml.common.XACMLSDKUtils;
import com.sun.identity.xacml.context.ContextFactory;
import com.sun.identity.xacml.context.Response;
import com.sun.identity.xacml.context.Result;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * The <code>Response</code> element is a container of 
 * one or more <code>Result</code>s issuded by authorization authority.
 *
 *
 * <p/>
 * schema:
 * <pre>
 *      &lt;xs:complexType name="ResponseType">
 *          &lt;xs:sequence>
 *              &lt;xs:element ref="xacml-context:Result" 
 *                      maxOccurs="unbounded"/>
 *          &lt;xs:sequence>
 *      &lt;xs:complexType>
 * </pre>
 */
public class ResponseImpl implements Response {


    private List results = new ArrayList(); //Result+ 
    private boolean mutable = true;

    /** 
     * Constructs a <code>Response</code> object
     */
    public ResponseImpl() {
    }

    /** 
     * Constructs a <code>Response</code> object from an XML string
     *
     * @param xml string representing a <code>Response</code> object
     * @throws SAMLException if the XML string could not be processed
     */
    public ResponseImpl(String xml) throws XACMLException {
        Document document = XMLUtils.toDOMDocument(xml, XACMLSDKUtils.debug);
        if (document != null) {
            Element rootElement = document.getDocumentElement();
            processElement(rootElement);
            makeImmutable();
        } else {
            XACMLSDKUtils.debug.error(
                "ResponseImpl.processElement(): invalid XML input");
            throw new XACMLException(
                XACMLSDKUtils.xacmlResourceBundle.getString(
                "errorObtainingElement"));
        }
    }

    /** 
     * Constructs a <code>Response</code> object from an XML DOM element
     *
     * @param element XML DOM element representing a <code>Response</code> 
     * object
     *
     * @throws SAMLException if the DOM element could not be processed
     */
    public ResponseImpl(Element element) throws XACMLException {
        processElement(element);
        makeImmutable();
    }

    /**
     * Returns the <code>Result</code>s of this object
     *
     * @return the <code>Result</code>s of this object
     */
    public List getResults() {
        return results; 
    }

    /**
     * Sets the <code>Result</code>s of this object
     *
     * @param values the <code>Result</code>s of this object.
     * @throws XACMLException if the object is immutable.
     */
    public void setResults(List values) throws XACMLException {
        if (!mutable) {
            throw new XACMLException(
                XACMLSDKUtils.xacmlResourceBundle.getString(
                    "objectImmutable"));
        }
        if (values != null) {
            Iterator iter = values.iterator();
            results = new ArrayList();
            while (iter.hasNext()) {
                Result value = (Result) iter.next();
                results.add(value);
            }
        } else {
            results = null;
        }

    }

    /**
     * Adds a <code>Result</code> to this object
     *
     * @param result the <code>Result</code> to add
     * @throws XACMLException if the object is immutable
     */
    public void addResult(Result result) throws XACMLException {
        if (!mutable) {
            throw new XACMLException(
                XACMLSDKUtils.xacmlResourceBundle.getString("objectImmutable"));
        }
        if (results == null) {
            results = new ArrayList();
        }
        results.add(result);
    }

   /**
    * Returns a string representation of this object
    *
    * @return a string representation of this object
    * @exception XACMLException if conversion fails for any reason
    */
    public String toXMLString() throws XACMLException {
        //top level element, declare namespace
        return this.toXMLString(true, true);
    }

   /**
    * Returns a string representation of this object
    * @param includeNSPrefix Determines whether or not the namespace qualifier
    *        is prepended to the Element when converted
    * @param declareNS Determines whether or not the namespace is declared
    *        within the Element.
    * @return a string representation of this object
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
        sb.append("<").append(nsPrefix).append(XACMLConstants.RESPONSE).
            append(nsDeclaration).append(">\n");
        int length = 0;
        if (results != null) {
            length = results.size();
            for (int i = 0; i < length; i++) {
                Result result = (Result)results.get(i);
                sb.append(result.toXMLString(includeNSPrefix, false));
            }
        }
        sb.append("</").append(nsPrefix)
                .append(XACMLConstants.RESPONSE).append(">\n");
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
        if (mutable) {
            if (results != null) {
                int length = results.size();
                for (int i = 0; i < length; i++) {
                    Result result = (Result)results.get(i);
                    result.makeImmutable();
                }
                results = Collections.unmodifiableList(results);
            }
            mutable = false;
        }
    }

    /** 
     * Initializes a <code>Response</code> object from an XML DOM element
     *
     * @param element XML DOM element representing a <code>Response</code> 
     * object
     *
     * @throws SAMLException if the DOM element could not be processed
     */
    private void processElement(Element element) throws XACMLException {
        if (element == null) {
            XACMLSDKUtils.debug.error(
                "ResponseImpl.processElement(): invalid root element");
            throw new XACMLException(
                XACMLSDKUtils.xacmlResourceBundle.getString(
                "invalid_element"));
        }
        String elemName = element.getLocalName();
        if (elemName == null) {
            XACMLSDKUtils.debug.error(
                "ResponseImpl.processElement(): local name missing");
            throw new XACMLException(
                XACMLSDKUtils.xacmlResourceBundle.getString(
                "missing_local_name"));
        }

        if (!elemName.equals(XACMLConstants.RESPONSE)) {
            XACMLSDKUtils.debug.error(
                "ResponseImpl.processElement(): invalid local name " + elemName);
            throw new XACMLException(
                XACMLSDKUtils.xacmlResourceBundle.getString(
                "invalid_local_name"));
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
                    if (childName.equals(XACMLConstants.RESULT)) {
                        results.add(
                            ContextFactory.getInstance().createResult(
                                    (Element)child));
                            //XMLUtils.getElementValue((Element)child));
                    } else {
                        XACMLSDKUtils.debug.error(
                            "ResponseImpl.processElement(): "
                            + " invalid child element: " + elemName);
                        throw new XACMLException(
                            XACMLSDKUtils.xacmlResourceBundle.getString(
                            "invalid_child_name")); //FIXME: add i18n key
                    }
                }
            }
            nextElem++;
        }
    }

}
