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
 * $Id: AttributeImpl.java,v 1.4 2008/11/10 22:57:05 veiming Exp $
 *
 */

package com.sun.identity.xacml.context.impl;

import com.sun.identity.shared.xml.XMLUtils;
import com.sun.identity.xacml.common.XACMLConstants;
import com.sun.identity.xacml.common.XACMLException;
import com.sun.identity.xacml.common.XACMLSDKUtils;
import com.sun.identity.xacml.context.Attribute;

import java.util.List;
import java.util.ArrayList;
import java.net.URI;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * The <code>Attribute</code> element specifies information about the
 * action/subject/resource requested in the <code>Request</code> context by 
 * listing a sequence of <code>Attribute</code> elements associated with 
 * the action.
 * <p>
 * <pre>
 * &lt;xs:element name="Attribute" type="xacml-context:AttributeType"/>
 * &lt;xs:complexType name="AttributeType">
 *    &lt;xs:sequence>
 *       &lt;xs:element ref="xacml-context:AttributeValue" 
 *        maxOccurs="unbounded"/>
 *    &lt;xs:sequence>
 *    &lt;xs:attribute name="AttributeId" type="xs:anyURI" use="required"/>
 *    &lt;xs:attribute name="DataType" type="xs:anyURI" use="required"/>
 *    &lt;xs:attribute name="Issuer" type="xs:string" use="optional"/>
 * &lt;xs:complexType>
 * </pre>
 *@supported.all.api
 */
public class AttributeImpl implements Attribute {


    URI id = null;
    URI type = null;
    String issuer = null;
    private List values ;
    private boolean isMutable = true;

   /** 
    * Default constructor
    */
    public AttributeImpl() {
    }

    /**
     * This constructor is used to build <code>Attribute</code> object from a
     * XML string.
     *
     * @param xml A <code>java.lang.String</code> representing
     *        an <code>Attribute</code> object
     * @exception XACMLException if it could not process the XML string
     */
    public AttributeImpl(String xml) throws XACMLException {
        Document document = XMLUtils.toDOMDocument(xml, XACMLSDKUtils.debug);
        if (document != null) {
            Element rootElement = document.getDocumentElement();
            processElement(rootElement);
            makeImmutable();
        } else {
            XACMLSDKUtils.debug.error(
                "AttributeImpl.processElement(): invalid XML input");
            throw new XACMLException(
                    XACMLSDKUtils.xacmlResourceBundle.getString(
                    "errorObtainingElement"));
        }
    }
    
    /**
     * This constructor is used to build <code>Request</code> object from a
     * block of existing XML that has already been built into a DOM.
     *
     * @param element A <code>org.w3c.dom.Element</code> representing
     *        DOM tree for <code>Request</code> object
     * @exception XACML2Exception if it could not process the Element
     */
    public AttributeImpl(Element element) throws XACMLException {
        processElement(element);
        makeImmutable();
    }

    private void processElement(Element element) throws XACMLException {
        String value = null;
        if (element == null) {
            XACMLSDKUtils.debug.error(
                "AttributeImpl.processElement(): invalid root element");
            throw new XACMLException( 
                XACMLSDKUtils.xacmlResourceBundle.getString(
                "invalid_element"));
        }

      // First check that we're really parsing an Attribute
      if (! element.getLocalName().equals(XACMLConstants.ATTRIBUTE)) {
            XACMLSDKUtils.debug.error(
                "AttributeImpl.processElement(): invalid root element");
            throw new XACMLException( 
                XACMLSDKUtils.xacmlResourceBundle.getString(
                "invalid_element"));
      }
      NamedNodeMap attrs = element.getAttributes();

      try {
          id = new URI(attrs.getNamedItem(XACMLConstants.ATTRIBUTE_ID)
                .getNodeValue());
      } catch (Exception e) {
            throw new XACMLException( 
                XACMLSDKUtils.xacmlResourceBundle.getString(
                "attribute_not_uri"));
      }
      if (id == null) {
            throw new XACMLException( 
                XACMLSDKUtils.xacmlResourceBundle.getString(
                "missing_attribute"));
      }
      try {
          type = new URI(attrs.getNamedItem(XACMLConstants.DATATYPE)
                .getNodeValue());
      } catch (Exception e) {
            throw new XACMLException( 
                XACMLSDKUtils.xacmlResourceBundle.getString(
                "attribute_not_uri"));
      }
      if (type == null) {
            throw new XACMLException( 
                XACMLSDKUtils.xacmlResourceBundle.getString(
                "missing_attribute"));
      }
      try {
          Node issuerNode = attrs.getNamedItem(XACMLConstants.ISSUER);
          if (issuerNode != null)
              issuer = issuerNode.getNodeValue();
  
      } catch (Exception e) {
            throw new XACMLException( 
                XACMLSDKUtils.xacmlResourceBundle.getString(
                "attribute_parsing_error"));
      }
 
      // now we get the attribute value
      NodeList nodes = element.getChildNodes();
      for (int i = 0; i < nodes.getLength(); i++) {
          Node node = nodes.item(i);
          if ((node.getNodeType() == Node.ELEMENT_NODE) ||
              (node.getNodeType() == Node.ATTRIBUTE_NODE)) {
              if (node.getLocalName().equals(XACMLConstants.ATTRIBUTE_VALUE)) {
                  if (values == null) {
                      values = new ArrayList();
                  }
                  values.add(node);
              }
          }
      }

      // make sure we got a value
      if (values ==null || values.isEmpty()) {
            throw new XACMLException( 
                XACMLSDKUtils.xacmlResourceBundle.getString(
                "missing_attribute_value"));
      }
    }

    /**
     * Returns the issuer of the <code>Attribute</code>.
     * @return <code>String</code> representing the issuer. It MAY be an 
     * x500Name that binds to a public key or some other identification 
     * exchanged out-of-band by participating entities.
     */
    public String getIssuer() {
        return issuer;
    }

    /**
     * Sets the issuer of the <code>Attribute</code>.
     * @param issuer <code>String</code> representing the issuer. 
     * It MAY be an x500Name that binds to a public key or some other 
     * identification  exchanged out-of-band by participating entities. 
     * This is optional so return value could be null or an empty 
     * <code>String</code>.
     * @exception XACMLException if the object is immutable
     */
    public void setIssuer(String issuer) throws XACMLException {
        if (!isMutable) {
            throw new XACMLException(
                XACMLSDKUtils.xacmlResourceBundle.getString(
                "objectImmutable"));
        }
        this.issuer=issuer;
    }

    /**
     * Returns the AttributeId of the <code>Attribute</code>
     * which the attribute identifier.
     * @return the <code>URI</code> representing the data type.
     */
    public URI getAttributeId() {
        return id;
    }

    /**
     * Sets the attribiteId of the <code>>Attribute</code>
     * @param attributeId <code>URI</code> representing the attribite id.
     * @exception XACMLException if the object is immutable
     */
    public void setAttributeId(URI attributeId) throws XACMLException {
        if (!isMutable) {
            throw new XACMLException(
                XACMLSDKUtils.xacmlResourceBundle.getString(
                "objectImmutable"));
        }
        if (attributeId == null) {
                throw new XACMLException(
                    XACMLSDKUtils.xacmlResourceBundle.getString(
                    "null_not_valid")); 
        }
        id = attributeId;
    }
      
    /**
     * Returns the datatype of the contents of the <code>AttributeValue</code>
     * elements. This will be either a primitive datatype defined by XACML 2.0 
     * specification or a type ( primitive or structured) defined in a  
     * namespace declared in the <xacml-context> element.
     * @return the <code>URI</code> representing the data type.
     */
    public URI getDataType() {
        return type;
    }

    /**
     * Sets the data type of the contents of the <code>AttributeValue</code>
     * elements.
     * @param dataType <code>URI</code> representing the data type.
     * @exception XACMLException if the object is immutable
     */
    public void setDataType(URI dataType) throws XACMLException {
        if (!isMutable) {
            throw new XACMLException(
                XACMLSDKUtils.xacmlResourceBundle.getString(
                "objectImmutable"));
        }
        if (dataType == null) {
                throw new XACMLException(
                    XACMLSDKUtils.xacmlResourceBundle.getString(
                    "null_not_valid"));
        }
        type = dataType;
    }
      
    /**
     * Returns one to many values in the <code>AttributeValue</code> elements 
     * of this object
     *
     * @return the List containing <code>Element</code>s representing the 
     * <code>AttributeValue</code> of this object
     */
    public List getAttributeValues() {
        return values;
    }

    /**
     * Sets the <code>AttributeValue</code> elements of this object
     *
     * @param values a <code>List</code> containing Element representing 
     * <code>AttributeValue</code> of this object.
     *
     * @exception XACMLException if the object is immutable
     * An object is considered <code>immutable</code> if <code>
     * makeImmutable()</code> has been invoked on it. It can
     * be determined by calling <code>isMutable</code> on the object.
     */
    public void setAttributeValues(List values) throws XACMLException {
        if (!isMutable) {
            throw new XACMLException(
                XACMLSDKUtils.xacmlResourceBundle.getString(
                "objectImmutable"));
        }
        if (this.values == null) {
            this.values = new ArrayList();
        }
        if (values == null || values.isEmpty()) {
            throw new XACMLException(
                XACMLSDKUtils.xacmlResourceBundle.getString(
                "null_not_valid")); 

        }
        for (int i=0; i < values.size(); i++) {
            Element value = (Element)values.get(i);
            String elemName = value.getLocalName();
            if (elemName == null 
                    || !elemName.equals(XACMLConstants.ATTRIBUTE_VALUE)) {
                XACMLSDKUtils.debug.error(
                    "StatusMessageImpl.processElement():"
                    + "local name missing or incorrect");
                throw new XACMLException(
                    XACMLSDKUtils.xacmlResourceBundle.getString(
                        "missing_local_name"));
            }
            this.values.add(value);
        }
    }

    /**
     * Sets the attribute values for this object
     *
     * @param stringValues a <code>List</code> containing
     *        <code>String<code> values of this object.
     * @throws XACMLException if the object is immutable
     *         An object is considered <code>immutable</code> if <code>
     *         makeImmutable()</code> has been invoked on it. It can
     *         be determined by calling <code>isMutable</code> on the object.
     */
    public void setAttributeStringValues(List stringValues) 
            throws XACMLException {
        if (!isMutable) {
            throw new XACMLException(
                XACMLSDKUtils.xacmlResourceBundle.getString(
                "objectImmutable"));
        }
        if (this.values == null) {
            this.values = new ArrayList();
        }
        if (stringValues == null || stringValues.isEmpty()) {
            throw new XACMLException(
                XACMLSDKUtils.xacmlResourceBundle.getString(
                "null_not_valid")); 

        }
        for (int i=0; i < stringValues.size(); i++) {
            String value = (String)(stringValues.get(i));
            StringBuffer sb = new StringBuffer(200);
            sb.append("<").append(XACMLConstants.ATTRIBUTE_VALUE)
                    .append(">").append(value)
                    .append("</").append(XACMLConstants.ATTRIBUTE_VALUE)
                    .append(">\n");
            Document document = XMLUtils.toDOMDocument(sb.toString(),
                    XACMLSDKUtils.debug);
            Element element = null;
            if (document != null) {
                element = document.getDocumentElement();
            }
            if (element != null) {
                this.values.add(element);
            }
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
            throws XACMLException 
    {
        StringBuffer sb = new StringBuffer(2000);
        StringBuffer NS = new StringBuffer(100);

        //TODO: remove the following 2 lines
        includeNSPrefix = false;
        declareNS = false;

        String appendNS = "";
        if (declareNS) {
            NS.append(XACMLConstants.CONTEXT_NS_DECLARATION)
            .append(XACMLConstants.SPACE);
            NS.append(XACMLConstants.XSI_NS_URI).append(XACMLConstants.SPACE)
            .append(XACMLConstants.CONTEXT_SCHEMA_LOCATION);
        }
        if (includeNSPrefix) {
            appendNS = XACMLConstants.CONTEXT_NS_PREFIX + ":";
        }
        sb.append("<").append(appendNS).append(XACMLConstants.ATTRIBUTE)
                .append(NS);
        sb.append(XACMLConstants.SPACE);
        if (id != null) {
            sb.append(XACMLConstants.ATTRIBUTE_ID).append("=").append("\"").
                    append(id.toString());
            sb.append("\"").append(XACMLConstants.SPACE);
        }
        if (type != null) {
            sb.append(XACMLConstants.DATATYPE).append("=").append("\"").
                    append(type.toString());
            sb.append("\"").append(XACMLConstants.SPACE);
        }
        if (issuer != null) {
            sb.append(XACMLConstants.ISSUER).append("=").append("\"")
                    .append(issuer).
                    append("\"");
        }
        sb.append(">");
        int length = 0;
        String xmlString = null;
        if (values != null && !values.isEmpty()) {
            for (int i=0; i < values.size(); i++) {
                Element value = (Element)values.get(i);
                sb.append("\n");
                // ignore trailing ":"
                if (includeNSPrefix && (value.getPrefix() == null)) {
                    value.setPrefix(appendNS.substring(0, appendNS.length()-1));
                }
                if(declareNS) {
                    int index = NS.indexOf("=");
                    String namespaceName = NS.substring(0, index);
                    String namespaceURI = NS.substring(index+1);
                    if (value.getNamespaceURI() == null) {
                        value.setAttribute(namespaceName, namespaceURI);
                        // does not seem to work to append namespace TODO
                    }
                }
                sb.append(XMLUtils.print(value));
             }
        } else { // values are empty put empty tags
            // This should not happen, not schema compliant
            /*
             sb.append("<").append(appendNS)
                     .append(XACMLConstants.ATTRIBUTE_VALUE);
             sb.append(NS).append(">").append("\n"); 
             sb.append("</").append(appendNS)
                     .append(XACMLConstants.ATTRIBUTE_VALUE);
             sb.append(">").append("\n");
             */
        }
        sb.append("\n</").append(appendNS).append(XACMLConstants.ATTRIBUTE);
        sb.append(">\n");
        return  sb.toString();
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
    public void makeImmutable() {//TODO
    }

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
