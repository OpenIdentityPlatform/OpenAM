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
 * $Id: ObligationsImpl.java,v 1.3 2008/11/10 22:57:06 veiming Exp $
 *
 */

package com.sun.identity.xacml.policy.impl;

import com.sun.identity.shared.xml.XMLUtils;

import com.sun.identity.xacml.common.XACMLConstants;
import com.sun.identity.xacml.common.XACMLException;
import com.sun.identity.xacml.common.XACMLSDKUtils;
import com.sun.identity.xacml.context.Response;
import com.sun.identity.xacml.context.Result;
import com.sun.identity.xacml.policy.PolicyFactory;
import com.sun.identity.xacml.policy.Obligation;
import com.sun.identity.xacml.policy.Obligations;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * The <code>Obligations</code> element is a container of 
 * one or more <code>Obligation</code>s issuded by 
 * authorization authority.
 * @supported.all.api
 */
public class ObligationsImpl implements Obligations {

    /* schema
	<xs:element name="Obligations" type="xacml:ObligationsType"/>
	<xs:complexType name="ObligationsType">
            <xs:sequence>
                <xs:element ref="xacml:Obligation" maxOccurs="unbounded"/>
            </xs:sequence>
	</xs:complexType>
    */
    private List obligations = new ArrayList(); //Obligation+ 
    private boolean mutable = true;

    /**
     * Default constructor
     */
    public ObligationsImpl() {
    }

    /** 
     * Constructs an <code>ObligationsImpl</code> object from an XML string
     *
     * @param xml string representing an <code>ObligationsImpl</code> object
     * @throws XACMLException if the XML string could not be processed
     */
    public ObligationsImpl(String xml) throws XACMLException {
        Document document = XMLUtils.toDOMDocument(xml, XACMLSDKUtils.debug);
        if (document != null) {
            Element rootElement = document.getDocumentElement();
            processElement(rootElement);
        } else {
            XACMLSDKUtils.debug.error(
                "ResponseImpl.processElement(): invalid XML input");
            throw new XACMLException(
                XACMLSDKUtils.xacmlResourceBundle.getString(
                "errorObtainingElement"));
        }
    }
    /** 
     * Constructs an <code>ObligationsImpl</code> object from an XML DOM element
     *
     * @param element XML DOM element representing a
     *        <code>ObligationsImpl</code>  object.
     *
     * @throws SAMLException if the DOM element could not be processed
     */
    public ObligationsImpl(Element element) throws XACMLException {
        processElement(element);
        makeImmutable();
    }


    /**
     * Returns the <code>Obligation</code> objects set in this
     * <code>Obligations</code>
     * @return the <code>Obligation</code> objects set in this
     * <code>Obligations</code>
     */
    public List getObligations() {
        return obligations;
    }

    /**
     * Sets the <code>Obligation</code> objects of this
     * <code>Obligations</code>
     *
     * @param obligations the <code>Obligation</code> objects to set in this
     * <code>Obligations</code>
     * @throws XACMLException if the object is immutable.
     */
    public void setObligations(List obligations) throws XACMLException {
        if (!mutable) {
            throw new XACMLException(
                XACMLSDKUtils.xacmlResourceBundle.getString("objectImmutable"));
        }
        if (obligations != null) {
            Iterator iter = obligations.iterator();
            this.obligations = new ArrayList();
            while (iter.hasNext()) {
                Obligation obligation = (Obligation) iter.next();
                this.obligations.add(obligation);
            }
        } else {
            obligations = null;
        }
    }

    /**
     * Adds an <code>Obligation</code> to this object.
     *
     * @param obligation the <code>Obligation</code> to add.
     * @throws XACMLException if the object is immutable.
     */
    public void addObligation(Obligation obligation) throws XACMLException {
        if (!mutable) {
            throw new XACMLException(
                XACMLSDKUtils.xacmlResourceBundle.getString("objectImmutable"));
        }
        if (obligations == null) {
            obligations = new ArrayList();
        }
        obligations.add(obligation);
    }


   /**
    * Returns a string representation of this object
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
            nsPrefix = XACMLConstants.XACML_NS_PREFIX + ":";
        }
        if (declareNS) {
            nsDeclaration = XACMLConstants.XACML_NS_DECLARATION;
        }
        sb.append("<").append(nsPrefix).append(XACMLConstants.OBLIGATIONS)
            .append(" ").append(nsDeclaration).append(">\n");
        int length = 0;
        if (obligations != null) {
            length = obligations.size();
            for (int i = 0; i < length; i++) {
                Obligation obligation = (Obligation)obligations.get(i);
                sb.append(obligation.toXMLString(includeNSPrefix, false));
            }
        }
        sb.append("</").append(nsPrefix)
                .append(XACMLConstants.OBLIGATIONS).append(">\n");
        return sb.toString();
                
    }

   /**
    * Returns a string representation of this object
    *
    * @return a string representation
    * @exception XACMLException if conversion fails for any reason
    */
    public String toXMLString() throws XACMLException {
        return toXMLString(true, false);
    }

   /**
    * Makes this object immutable
    */
    public void makeImmutable() {
        mutable = false;
    }

   /**
    * Checks if this object is mutable
    *
    * @return <code>true</code> if the object is mutable,
    *         <code>false</code> otherwise
    */
    public boolean isMutable() {
        return mutable;
    }
    
    /** 
     * Initializes a <code>ObligationsImpl</code> object from an XML DOM element
     *
     * @param element XML DOM element representing a
     *        <code>ObligationsImpl</code> object
     *
     * @throws XACMLException if the DOM element could not be processed
     */
    private void processElement(Element element) throws XACMLException {
        if (element == null) {
            XACMLSDKUtils.debug.error(
                "ResponseImpl.processElement(): invalid root element");
            throw new XACMLException(
                XACMLSDKUtils.xacmlResourceBundle.getString("invalid_element"));
        }
        String elemName = element.getLocalName();
        if (elemName == null) {
            XACMLSDKUtils.debug.error(
                "ResponseImpl.processElement(): local name missing");
            throw new XACMLException(
                XACMLSDKUtils.xacmlResourceBundle.getString(
                "missing_local_name"));
        }

        if (!elemName.equals(XACMLConstants.OBLIGATIONS)) {
            XACMLSDKUtils.debug.error(
                "ResponseImpl.processElement: invalid local name " + elemName);
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
                    if (childName.equals(XACMLConstants.OBLIGATION)) {
                        obligations.add(
                            PolicyFactory.getInstance().createObligation(
                                    (Element)child));
                    } else {
                        XACMLSDKUtils.debug.error(
                            "ObligationsImpl.processElement(): "
                            + " invalid child element: " + elemName);
                        throw new XACMLException(
                            XACMLSDKUtils.xacmlResourceBundle.getString(
                            "invalid_child_name")); 
                    }
                }
            }
            nextElem++;
        }
    }

    
}
