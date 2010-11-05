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
 * $Id: SubjectImpl.java,v 1.3 2008/06/25 05:48:13 qcheng Exp $
 *
 */

package com.sun.identity.xacml.context.impl;

import com.sun.identity.shared.xml.XMLUtils;
import com.sun.identity.xacml.common.XACMLConstants;
import com.sun.identity.xacml.common.XACMLException;
import com.sun.identity.xacml.common.XACMLSDKUtils;
import com.sun.identity.xacml.context.Attribute;
import com.sun.identity.xacml.context.ContextFactory;
import com.sun.identity.xacml.context.Subject;

import java.util.List;
import java.net.URI;
import java.util.ArrayList;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * The <code>Subject</code> element specifies information about a
 * subject of the <code>Request</code> context by listing a 
 * sequence of <code>Attribute</code> elements associated with the
 * subject. A subject is an entity associated with the access request.
 * <p>
 * <pre>
 * &lt;xs:complexType name="SubjectType">
 *  &lt;xs:sequence>
 *   &lt;xs:element ref="xacml-context:Attribute" minOccurs="0"
 *      maxOccurs="unbounded"/>
 * &lt;xs:sequence>
 * &lt;xs:attribute name="SubjectCategory" type="xs:anyURI" 
 *  default="urn:oasis:names:tc:xacml:1.0:subject-category:access-subject"/>
 * &lt;xs:complexType>
 * </pre>
 *@supported.all.api
 */
public class SubjectImpl implements Subject {
    private List  attributes ;
    private URI subjectCategory;
    private Attribute subjectCategoryAttribute;
    private boolean isMutable = true;
    private boolean needToCreateSubjectCategory = false;

   /** 
    * Default constructor
    */
    public SubjectImpl() {
    }

    /**
     * This constructor is used to build <code>Subject</code> object from a
     * XML string.
     *
     * @param xml A <code>java.lang.String</code> representing
     *        a <code>Subject</code> object
     * @exception XACMLException if it could not process the XML string
     */
    public SubjectImpl(String xml) throws XACMLException {
        Document document = XMLUtils.toDOMDocument(xml, XACMLSDKUtils.debug);
        if (document != null) {
            Element rootElement = document.getDocumentElement();
            processElement(rootElement);
            makeImmutable();
        } else {
            XACMLSDKUtils.debug.error(
                "SubjectImpl.processElement(): invalid XML input");
            throw new XACMLException(XACMLSDKUtils.xacmlResourceBundle.getString(
                "errorObtainingElement"));
        }
    }

    /**
     * This constructor is used to build <code>Subject</code> object from a
     * block of existing XML that has already been built into a DOM.
     *
     * @param element A <code>org.w3c.dom.Element</code> representing
     *        DOM tree for <code>Subject</code> object
     * @exception XACMLException if it could not process the Element
     */
    public SubjectImpl(Element element) throws XACMLException {
        processElement(element);
        makeImmutable();
    }

    private void processElement(Element element) throws XACMLException {
        if (element == null) {
            XACMLSDKUtils.debug.error(
                "SubjectImpl.processElement(): invalid root element");
            throw new XACMLException(XACMLSDKUtils.xacmlResourceBundle.getString(
                "invalid_element"));
        }
        String elemName = element.getLocalName(); 
        if (elemName == null) {
             XACMLSDKUtils.debug.error(
                "SubjectImpl.processElement(): local name missing");
            throw new XACMLException( XACMLSDKUtils.xacmlResourceBundle.getString(
                "missing_local_name"));
        }

        if (!elemName.equals(XACMLConstants.SUBJECT)) {
            XACMLSDKUtils.debug.error(
                "SubjectImpl.processElement(): invalid local name " +
                 elemName);
            throw new XACMLException(XACMLSDKUtils.xacmlResourceBundle.getString(
                "invalid_local_name"));
        }
        // starts processing subelements
        NodeList nodes = element.getChildNodes();
        int numOfNodes = nodes.getLength();
        if (numOfNodes >= 1) {
            ContextFactory factory = ContextFactory.getInstance();
            for (int nextElem = 0; nextElem < numOfNodes; nextElem++) {
                Node child = (Node)nodes.item(nextElem);
                if ((child.getNodeType() == Node.ELEMENT_NODE) ||
                    (child.getNodeType() == Node.ATTRIBUTE_NODE )) {
                    // The child nodes should be <Attribute> 
                    // or <SubjectCategory>
                    String attrChildName = child.getLocalName();
                    if (attrChildName.equals(XACMLConstants.ATTRIBUTE)) {
                        if (this.attributes == null) {
                        this.attributes = new ArrayList();
                        }
                        Attribute attribute = factory.getInstance().
                                createAttribute((Element)child);
                        attributes.add(attribute);
                    } else if (attrChildName.equals(
                            XACMLConstants.SUBJECT_CATEGORY)) {
                        try {
                            subjectCategory = new URI (child.getNodeValue());
                        } catch ( Exception e) {
                            throw new XACMLException(
                                XACMLSDKUtils.xacmlResourceBundle.getString( 
                                    "attribute_not_uri"));
                        }
                    } else {
                        XACMLSDKUtils.debug.error("RequestImpl."
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
    /**
     * Returns zero to many <code>Attribute</code> elements of this object
     * If no attributes and present, empty <code>List</code> will be returned.
     * Typically a <code>Subject</code> element will contain an <code>
     * Attribute</code> with an <code>AttributeId</code> of
     * "urn:oasis:names:tc:xacml:1.0:subject:subject-id", containing 
     * the identity of the <code>Subject</code>
     *
     * @return the <code>Attribute</code> elements of this object
     */
    public List getAttributes() { 
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
     * be determined by calling <code>isMutable</code> on the object.
     */
    public void setAttributes(List attributes) throws XACMLException {
        if (!isMutable) {
            throw new XACMLException(XACMLSDKUtils.xacmlResourceBundle.getString(
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
     * Returns the <code>SubjectCategory</code> of this object.
     * This is optional so could be null if not defined.
     * This attribute indicates the role that the parent <code>Subject</code> 
     * played in the formation of the access request. If this attribute is not 
     * present in the <code>Subject</code> element, then the
     * default value of 
     * urn:oasis:names:tc:xacml:1.0:subject-category:access-subject SHALL be
     * used, indicating that the <code>Subject</code> represents the entity 
     * ultimately responsible for initiating the access request.
     *
     * @return <code>URI</code> representing the 
     * <code>SubjectCategory</code> of this  object.
     */
    public URI getSubjectCategory() {
        try {
            if (subjectCategory == null) {
                subjectCategory = new URI(XACMLConstants.ACCESS_SUBJECT);
            }
        } catch (Exception e) { // cant do anything, return null
        }
        return subjectCategory;
    }

    /**
     * Sets the <code>SubjectCategory</code> of this object
     *
     * @param subjectCategory <code>URI</code> 
     *
     * @exception XACMLException if the object is immutable
     * An object is considered <code>immutable</code> if <code>
     * makeImmutable()</code> has been invoked on it. It can
     * be determined by calling <code>isMutable</code> on the object.
     */
    public void setSubjectCategory(URI subjectCategory) throws 
        XACMLException 
    {
        if (!isMutable) {
            throw new XACMLException(XACMLSDKUtils.xacmlResourceBundle.getString(
                "objectImmutable"));
        }
        if (subjectCategory != null) {
            this.subjectCategory = subjectCategory;
        } /*else {
            needToCreateSubjectCategory = true;
             try {
               subjectCategory = new URI(SUBJECT_CATEGORY_DEFAULT);
               List values = new ArrayList();
               values.add(subjectCategory.toString());
                 subjectCategoryAttribute = 
                     XACMLSDKUtils.createAttribute(values, new URI(
                     SUBJECT_CATEGORY_ID), new URI(URI_DATATYPE), null);
             } catch ( Exception e) {
                 throw new XACMLException(e);
             }
        }*/
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
            throws XACMLException
    {
        StringBuffer sb = new StringBuffer(2000);
        StringBuffer NS = new StringBuffer(100);

        //TODO: remove the 2 following line
        includeNSPrefix = false;
        declareNS = false;

        String appendNS = "";
        if (declareNS) {
            NS.append(XACMLConstants.CONTEXT_NS_DECLARATION)
                    .append(XACMLConstants.SPACE);
            NS.append(XACMLConstants.XSI_NS_URI)
                    .append(XACMLConstants.SPACE)
                    .append(XACMLConstants.CONTEXT_SCHEMA_LOCATION);
        }
        if (includeNSPrefix) {
            appendNS = XACMLConstants.CONTEXT_NS_PREFIX + ":";
        }
        sb.append("<").append(appendNS).append(XACMLConstants.SUBJECT)
                .append(NS);
        if (subjectCategory != null) {
            sb.append(" ").append(XACMLConstants.SUBJECT_CATEGORY).append("=");
            sb.append("\"").append(subjectCategory.toString()).append("\"");
        }
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
     /*   if (needToCreateSubjectCategory && subjectCategoryAttribute != null) {
                sb.append(subjectCategoryAttribute.toXMLString(
                    includeNSPrefix, false));
        }// its already covered in the previous list of attrs.
      */
        sb.append("</").append(appendNS).append(XACMLConstants.SUBJECT);
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
    public void makeImmutable() {}

   /**
    * Checks if the object is mutable
    *
    * @return <code>true</code> if the object is mutable,
    *         <code>false</code> otherwise
    */
    public boolean isMutable() {
        return isMutable;
    }
    
}
