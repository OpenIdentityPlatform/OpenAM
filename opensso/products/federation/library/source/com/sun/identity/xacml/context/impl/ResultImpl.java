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
 * $Id: ResultImpl.java,v 1.4 2008/11/10 22:57:05 veiming Exp $
 *
 */

package com.sun.identity.xacml.context.impl;

import com.sun.identity.shared.xml.XMLUtils;

import com.sun.identity.xacml.common.XACMLConstants;
import com.sun.identity.xacml.common.XACMLException;
import com.sun.identity.xacml.common.XACMLSDKUtils;
import com.sun.identity.xacml.context.ContextFactory;
import com.sun.identity.xacml.context.Result;
import com.sun.identity.xacml.context.Decision;
import com.sun.identity.xacml.context.Status;
import com.sun.identity.xacml.policy.Obligations;
import com.sun.identity.xacml.policy.PolicyFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * The <code>Result</code> element contains decision, status and obligations
 * per resource id
 *
 * <p/>
 * schema
 * <pre>
 *  &lt;xs:complexType name="ResultType">
 *      &lt;xs:sequence>
 *          &lt;xs:element ref="xacml-context:Decision"/>
 *          &lt;xs:element ref="xacml-context:Status" minOccurs="0"/>
 *          &lt;xs:element ref="xacml:Obligations" minOccurs="0"/>
 *      &lt;xs:sequence>
 *      &lt;xs:attribute name="ResourceId" type="xs:string" use="optional"/>
 *  &lt;xs:complexType>
 *
 * </pre>
 */
public class ResultImpl implements Result {

    private String resourceId = null; //optional
    private Decision decision = null; //required
    private Status status = null; //optional
    private Obligations obligations = null; //optional

    private boolean mutable = true;

    /** 
     * Constructs a <code>Result</code> object
     */
    public ResultImpl() throws XACMLException {
    }

    /** 
     * Constructs a <code>Result</code> object from an XML string
     *
     * @param xml string representing a <code>Result</code> object
     * @throws SAMLException if the XML string could not be processed
     */
    public ResultImpl(String xml) throws XACMLException {
        Document document = XMLUtils.toDOMDocument(xml, XACMLSDKUtils.debug);
        if (document != null) {
            Element rootElement = document.getDocumentElement();
            processElement(rootElement);
            makeImmutable();
        } else {
            XACMLSDKUtils.debug.error(
                "ResultImpl.processElement(): invalid XML input");
            throw new XACMLException(
                XACMLSDKUtils.xacmlResourceBundle.getString(
                "errorObtainingElement"));
        }
    }

    /** 
     * Constructs a <code>Result</code> object from an XML DOM element
     *
     * @param element XML DOM element representing a <code>Result</code> 
     * object
     *
     * @throws SAMLException if the DOM element could not be processed
     */
    public ResultImpl(Element element) throws XACMLException {
        processElement(element);
        makeImmutable();
    }

    /**
     * Returns <code>resourceId</code> of this object
     * @return  <code>resourceId</code> of this object
     */
    public String getResourceId() {
        return resourceId;
    }

    /**
     * Sets <code>resourceId</code> of this object
     * @param resourceId <code>resourceId</code> of this object
     * @exception XACMLException if the object is immutable
     */
    public void setResourceId(String resourceId) throws XACMLException {
        if (!mutable) {
            throw new XACMLException(
                XACMLSDKUtils.xacmlResourceBundle.getString("objectImmutable"));
        }
        this.resourceId = resourceId;
    }

    /**
     * Returns the <code>Decision</code> of this object
     *
     * @return the <code>Decision</code> of this object
     */
    public Decision getDecision() {
        return decision;
    }

    /**
     * Sets the <code>Decision</code> of this object
     *
     * @exception XACMLException if the object is immutable
     * 
     */
    public void setDecision(Decision decision) throws XACMLException {
        if (!mutable) {
            throw new XACMLException(
                XACMLSDKUtils.xacmlResourceBundle.getString("objectImmutable"));
        }
        if (decision == null) {
            throw new XACMLException(
                XACMLSDKUtils.xacmlResourceBundle.getString("null_not_valid")); 
        }
        this.decision = decision;
    }

    /**
     * Returns the <code>Status</code> of this object
     *
     * @return the <code>Status</code> of this object
     */
    public Status getStatus() {
        return status; 
    }

    /**
     * Sets the <code>Status</code> of this object
     *
     * @exception XACMLException if the object is immutable
     */
    public void setStatus(Status status) throws XACMLException {
        if (!mutable) {
            throw new XACMLException(
                XACMLSDKUtils.xacmlResourceBundle.getString("objectImmutable"));
        }
        this.status = status;
    }

    /**
     * Returns the <code>Obligations</code> of this object
     *
     * @return the <code>Obligations</code> of this object
     */
    public Obligations getObligations() {
        return obligations;
    }

    /**
     * Sets the <code>Obligations</code> of this object
     * @param obligations <code>Obligations</code> to set
     *
     * @exception XACMLException if the object is immutable
     */
    public void setObligations(Obligations obligations) throws XACMLException {
        this.obligations = obligations;
    }


   /**
    * Returns a string representation of this object
    *
    * @return a string representation of this object
    * @exception XACMLException if conversion fails for any reason
    */
    public String toXMLString() throws XACMLException {
        return this.toXMLString(true, false);
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
        String nsDeclaration = "";
        String nsPrefix = "";
        if (declareNS) {
            nsDeclaration = XACMLConstants.CONTEXT_NS_DECLARATION;
        }
        if (includeNSPrefix) {
            nsPrefix = XACMLConstants.CONTEXT_NS_PREFIX + ":";
        }
        sb.append("<").append(nsPrefix).append(XACMLConstants.RESULT)
                .append(nsDeclaration);
        sb.append(" ");
        if (resourceId != null) {
            sb.append(XACMLConstants.RESOURCE_ID)
                .append("=")
                .append(XACMLSDKUtils.quote(resourceId));
        }
        sb.append(">\n");
        if (decision != null) {
            sb.append(decision.toXMLString(includeNSPrefix, false));
        }
        if (status != null) {
            sb.append(status.toXMLString(includeNSPrefix, false));
        }
        if (obligations != null) {
            sb.append(obligations.toXMLString(includeNSPrefix, true));
        }
        sb.append("</").append(nsPrefix).append(XACMLConstants.RESULT)
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
        if (mutable) {
            if (decision != null) {
                decision.makeImmutable();
            }
            if (status != null) {
                status.makeImmutable();
            }
            mutable = false;
        }
    }

    private void processElement(Element element) throws XACMLException {
        if (element == null) {
            XACMLSDKUtils.debug.error(
                "ResultImpl.processElement(): invalid root element");
            throw new XACMLException(
                XACMLSDKUtils.xacmlResourceBundle.getString(
                "invalid_element"));
        }
        String elemName = element.getLocalName();
        if (elemName == null) {
            XACMLSDKUtils.debug.error(
                "ResultImpl.processElement(): local name missing");
            throw new XACMLException(XACMLSDKUtils.xacmlResourceBundle.getString(
                "missing_local_name"));
        }

        if (!elemName.equals(XACMLConstants.RESULT)) {
            XACMLSDKUtils.debug.error(
                "ResultImpl.processElement(): invalid local name " + elemName);
            throw new XACMLException(XACMLSDKUtils.xacmlResourceBundle.getString(
                "invalid_local_name"));
        }

        String resourceIdValue 
                = element.getAttribute(XACMLConstants.RESOURCE_ID);
        if ((resourceIdValue != null) || (resourceIdValue.length() != 0)) {
            resourceId = resourceIdValue;
        } 

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
        } else if (childCount > 3) {
            XACMLSDKUtils.debug.error(
                "ResultImpl.processElement(): invalid child element count: " 
                        + childCount);
            throw new XACMLException(XACMLSDKUtils.xacmlResourceBundle.getString(
                "invalid_child_count")); //FIXME: add i18n key
        }

        //process decision element
        Element firstChild = (Element)childElements.get(0);
        String firstChildName = firstChild.getLocalName();
        if (firstChildName.equals(XACMLConstants.DECISION)) {
            decision =  ContextFactory.getInstance()
                    .createDecision(firstChild);
        } else {
            XACMLSDKUtils.debug.error(
                "ResultImpl.processElement(): invalid first child element: " 
                        + firstChildName);
            throw new XACMLException(XACMLSDKUtils.xacmlResourceBundle.getString(
                "invalid_first_child")); //FIXME: add i18n key
        }

        //process status element
        if (childCount > 1) {
            Element secondChild = (Element)childElements.get(1);
            String secondChildName = secondChild.getLocalName();
            if (secondChildName.equals(XACMLConstants.STATUS)) {
                status =  ContextFactory.getInstance()
                        .createStatus(secondChild);
            } else if (secondChildName.equals(XACMLConstants.OBLIGATIONS)) {
                obligations =  PolicyFactory.getInstance()
                        .createObligations(secondChild);

            } else {
                XACMLSDKUtils.debug.error(
                    "ResultImpl.processElement(): invalid second child element: " 
                            + secondChildName);
                throw new XACMLException(
                    XACMLSDKUtils.xacmlResourceBundle.getString(
                    "invalid_second_child")); //FIXME: add i18n key
            }
            if (childCount > 2) {
                Element thirdChild = (Element)childElements.get(2);
                String thirdChildName = thirdChild.getLocalName();
                if (thirdChildName.equals(XACMLConstants.OBLIGATIONS) 
                        && (obligations == null) ) {
                    obligations =  PolicyFactory.getInstance()
                            .createObligations(thirdChild);
                } else {
                    XACMLSDKUtils.debug.error(
                        "ResultImpl.processElement(): invalid third child element: " 
                                + thirdChildName);
                    throw new XACMLException(
                        XACMLSDKUtils.xacmlResourceBundle.getString(
                        "invalid_third_child")); //FIXME: add i18n key
                }
            }
            if (childCount > 3) {
                Element thirdChild = (Element)childElements.get(3);
                String thirdChildName = thirdChild.getLocalName();
                XACMLSDKUtils.debug.error(
                    "ResultImpl.processElement(): invalid third child element: " 
                            + thirdChildName);
                throw new XACMLException(
                    XACMLSDKUtils.xacmlResourceBundle.getString(
                    "invalid_third_child")); //FIXME: add i18n key
            }
        }
    }

}
