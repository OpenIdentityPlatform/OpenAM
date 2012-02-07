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
 * $Id: EnvironmentImpl.java,v 1.3 2008/06/25 05:48:13 qcheng Exp $
 *
 */

package com.sun.identity.xacml.context.impl;

import com.sun.identity.xacml.common.XACMLException;
import com.sun.identity.xacml.common.XACMLConstants;
import com.sun.identity.xacml.common.XACMLSDKUtils;
import com.sun.identity.xacml.context.Environment;
import com.sun.identity.xacml.context.Attribute;
import com.sun.identity.xacml.context.ContextFactory;
import com.sun.identity.shared.xml.XMLUtils;

import java.util.List;
import java.net.URI;
import java.util.ArrayList;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


/**
 * The <code>Environment</code> element specifies information about the
 * environment requested in the <code>Request</code> context by listing a 
 * sequence of <code>Attribute</code> elements associated with the
 * environment.
 * <p>
 * <pre>
 * &lt;xs:element name="Environment" type="xacml-context:EnvironmentType"/>
 * &lt;xs:complexType name="EnvironmentType">
 *    &lt;xs:sequence>
 *       &lt;xs:element ref="xacml-context:Attribute" minOccurs="0"
 *       maxOccurs="unbounded"/>
 *    &lt;xs:sequence>
 * &lt;xs:complexType>
 * </pre>
 *@supported.all.api
 */
public class EnvironmentImpl implements Environment {
    
    private List  attributes ;
    private boolean mutable = true;
 
    
    /** Creates a new instance of EnvironmentImpl */
    public EnvironmentImpl() {
    }
    
    /**
     * This constructor is used to build <code>Environment</code> object from a
     * XML string.
     *
     * @param xml A <code>java.lang.String</code> representing
     *        a <code>Environment</code> object
     * @exception XACMLException if it could not process the XML string
     */
    
    public EnvironmentImpl(String xml) throws XACMLException {
        Document document = XMLUtils.toDOMDocument(xml, XACMLSDKUtils.debug);
        if (document != null) {
            Element rootElement = document.getDocumentElement();
            processElement(rootElement);
            makeImmutable();
        } else {
            XACMLSDKUtils.debug.error(
                "EnvironmentImpl.processElement(): invalid XML input");
            throw new XACMLException(
                 XACMLSDKUtils.xacmlResourceBundle.getString(
                "errorObtainingElement"));
        }
    }
    
    /**
     * This constructor is used to build <code>Environment</code> object from a
     * block of existing XML that has already been built into a DOM.
     *
     * @param element A <code>org.w3c.dom.Element</code> representing
     *        DOM tree for <code>Environment</code> object
     * @exception XACMLException if it could not process the Element
     */
    public EnvironmentImpl(Element element) throws XACMLException {
        processElement(element);
        makeImmutable();
    }

    private void processElement(Element element) throws XACMLException {
        if (element == null) {
            XACMLSDKUtils.debug.error(
                "EnvironmentImpl.processElement(): invalid root element");
            throw new XACMLException( 
                XACMLSDKUtils.xacmlResourceBundle.getString(
                "invalid_element"));
        }
        String elemName = element.getLocalName(); 
        if (elemName == null) {
             XACMLSDKUtils.debug.error(
                "EnvironmentImpl.processElement(): local name missing");
            throw new XACMLException( 
                XACMLSDKUtils.xacmlResourceBundle.getString(
                "missing_local_name"));
        }

        if (!elemName.equals(XACMLConstants.ENVIRONMENT)) {
            XACMLSDKUtils.debug.error(
                "EnvironmentImpl.processElement(): invalid local name " +
                 elemName);
            throw new XACMLException(
                XACMLSDKUtils.xacmlResourceBundle.getString(
                "invalid_local_name"));
        }
        // starts processing subelements
        NodeList nodes = element.getChildNodes();
        int numOfNodes = nodes.getLength();
        if (numOfNodes >= 1) {
            ContextFactory factory = ContextFactory.getInstance();
            for (int nextElem = 0; nextElem < numOfNodes; nextElem++) {
                Node child = (Node)nodes.item(nextElem);
                if (child.getNodeType() == Node.ELEMENT_NODE) {
                    // The child nodes should be <Attribute> 
                    String attrChildName = child.getLocalName();
                    if (attrChildName.equals(XACMLConstants.ATTRIBUTE)) {
                        if (this.attributes == null) {
                        this.attributes = new ArrayList();
                        }
                        Attribute attribute = factory.getInstance().
                                createAttribute((Element)child);
                        attributes.add(attribute);
                    } else {
                        XACMLSDKUtils.debug.error("EnvironmentImpl."
                            +"processElement(): Invalid element :"
                            +attrChildName);
                        throw new XACMLException(
                            XACMLSDKUtils.xacmlResourceBundle.getString( 
                                "invalid_element"));
                    }
                }
            }
         }
    }
    
    public java.util.List getAttributes() {
        return attributes;
    }
    
    /**
     * Sets the <code>Attribute</code> elements of this object
     *
     * @param attributes <code>Attribute</code> elements of this object
     * attributes could be an empty <code>List</code>, if no attributes
     * are present.
     *
     * @exception XACMLException if the object is immutable
     * An object is considered <code>immutable</code> if <code>
     * makeImmutable()</code> has been invoked on it. It can
     * be determined by calling <code>mutable</code> on the object.
     */
    public void setAttributes(java.util.List attributes) 
        throws XACMLException {
        if (!mutable) {
            throw new XACMLException(
                XACMLSDKUtils.xacmlResourceBundle.getString(
                "objectImmutable"));
        }
        if (attributes != null &&  !attributes.isEmpty()) {
            if (this.attributes == null) {
                this.attributes = new ArrayList();
            }
            this.attributes.addAll(attributes);
        }
    }
    
    /**
    * Returns a <code>String</code> representation of this object
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
        StringBuffer namespaceBuffer = new StringBuffer(100);
        String nsDeclaration = "";
        if (declareNS) {
            namespaceBuffer.append(XACMLConstants.CONTEXT_NS_DECLARATION).
                append(XACMLConstants.SPACE);
            namespaceBuffer.append(XACMLConstants.XSI_NS_URI).
                append(XACMLConstants.SPACE).append(XACMLConstants.
                CONTEXT_SCHEMA_LOCATION);
        }
        if (includeNSPrefix) {
            nsDeclaration = XACMLConstants.CONTEXT_NS_PREFIX + ":";
        }
        sb.append("<").append(nsDeclaration).append(XACMLConstants.
            ENVIRONMENT).append(namespaceBuffer);
        sb.append(">");
        int length = 0;
        if (attributes != null) {
            sb.append("\n");
            length = attributes.size();
            for (int i = 0; i < length; i++) {
                Attribute attr = (Attribute)attributes.get(i);
                sb.append(attr.toXMLString(includeNSPrefix, false));
            }
        }
        sb.append("</").append(nsDeclaration).
            append(XACMLConstants.ENVIRONMENT);
        sb.append(">\n");
        return sb.toString();
    }
    
    /**
    * Returns a string representation of this object
    *
    * @return a string representation of this object
    * @exception XACMLException if conversion fails for any reason
    */
    public String toXMLString() throws XACMLException {
        return toXMLString(true, false);
    }
    
    /**
    * Makes the object immutable
    */
    public void makeImmutable() {
        mutable = false;
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
    
}
