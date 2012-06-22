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
 * $Id: ObligationImpl.java,v 1.3 2008/11/10 22:57:06 veiming Exp $
 *
 */

package com.sun.identity.xacml.policy.impl;

import com.sun.identity.shared.xml.XMLUtils;
import com.sun.identity.xacml.common.XACMLConstants;
import com.sun.identity.xacml.common.XACMLException;
import com.sun.identity.xacml.common.XACMLSDKUtils;
import com.sun.identity.xacml.policy.Obligation;

import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * The <code>Obligation</code> element is a container of 
 * one or more <code>AttributeAssignment</code>s issuded by 
 * authorization authority.
 * @supported.all.api
 *
 * <p>
 * <pre>
 *
 *   &lt;xs:element name="Obligation" type="xacml:ObligationType"/>
 *      &lt;xs:complexType name="ObligationType">
 *          &lt;xs:sequence>
 *              &lt;xs:element ref="xacml:AttributeAssignment" minOccurs="0" 
 *                  maxOccurs="unbounded"/>
 *              &lt;/xs:sequence>
 *              &lt;xs:attribute name="ObligationId" type="xs:anyURI" 
 *                  use="required"/>
 *              &lt;xs:attribute name="FulfillOn" type="xacml:EffectType" 
 *                  use="required"/>
 *          &lt;/xs:complexType>
 *   &lt;/xs:element>
 * </pre>
 */
public class ObligationImpl implements Obligation {

    // AttributeAssignment rquires AttributeId and DataType

    private URI obligationId = null; //required
    private String fulfillOn = null; //optional
    private List attributeAssignments = null; //required
    private boolean mutable = true;

    /**
     * Default constructor
     */
    public ObligationImpl() {
    }

    /**
     * This constructor is used to build <code>Obligation</code> object 
     * from an XML string.
     *
     * @param xml  a <code>String</code> representation of 
     * <code>Obligation</code> object
     * @exception XACMLException if it could not process the XML string
     */
    public ObligationImpl(String xml) throws XACMLException {
        Document document = XMLUtils.toDOMDocument(xml, XACMLSDKUtils.debug);
        if (document != null) {
            Element rootElement = document.getDocumentElement();
            processElement(rootElement);
            makeImmutable();
        } else {
            XACMLSDKUtils.debug.error(
                "ObligationImpl.processElement(): invalid XML input");
            throw new XACMLException(XACMLSDKUtils.xacmlResourceBundle.getString(
                "errorObtainingElement"));
        }
    }
    
    /**
     * This constructor is used to build <code>ObligationImpl</code> object 
     * from a  block of existing XML that has already been built into a DOM.
     *
     * @param element  DOM tree for <code>Request</code> object
     * @exception XACML2Exception if it could not process the Element
     */
    public ObligationImpl(Element element) throws XACMLException {
        processElement(element);
        makeImmutable();
    }

    private void processElement(Element element) throws XACMLException {
        String value = null;
        if (element == null) {
            XACMLSDKUtils.debug.error(
                "ObligationImpl.processElement(): invalid root element");
            throw new XACMLException( XACMLSDKUtils.xacmlResourceBundle.getString(
                "invalid_element"));
        }

      // First check that we're really parsing an Obligation
      if (! element.getLocalName().equals(XACMLConstants.OBLIGATION)) {
            XACMLSDKUtils.debug.error(
                "ObligationImpl.processElement(): invalid root element");
            throw new XACMLException( XACMLSDKUtils.xacmlResourceBundle.getString(
                "invalid_element"));
      }
      NamedNodeMap attrs = element.getAttributes();

      try {
          obligationId = new URI(attrs.getNamedItem(XACMLConstants.OBLIGATION_ID)
                .getNodeValue());
      } catch (Exception e) {
            throw new XACMLException( XACMLSDKUtils.xacmlResourceBundle.getString(
                "attribute_not_uri"));
      }
      if (obligationId == null) {
            throw new XACMLException( XACMLSDKUtils.xacmlResourceBundle.getString(
                "attribute_missing"));
      }
      try {
          fulfillOn = attrs.getNamedItem(XACMLConstants.FULFILL_ON)
                .getNodeValue();
      } catch (Exception e) {
            throw new XACMLException( XACMLSDKUtils.xacmlResourceBundle.getString(
                "error_parsing_attribute"));
      }
 
      // now we get the AttributeAssignment(s)
      // AttributeAssignment requires AttributeId and DataType 
      NodeList nodes = element.getChildNodes();
      for (int i = 0; i < nodes.getLength(); i++) {
          Node node = nodes.item(i);
          if ((node.getNodeType() == Node.ELEMENT_NODE) ) {
              if (node.getLocalName().equals(
                        XACMLConstants.ATTRIBUTE_ASSIGNMENT)) {
                  if (attributeAssignments == null) {
                      attributeAssignments = new ArrayList();
                  }
                  Element aa = (Element)node;
                  // aa should have attributes AtributeId, DataType
                  String aid = aa.getAttribute(XACMLConstants.ATTRIBUTE_ID);
                  if ((aid == null) || (aid.length() == 0)) {
                        throw new XACMLException(
                            XACMLSDKUtils.xacmlResourceBundle.getString(
                            "missing_attribute"));
                  }
                  String dt = aa.getAttribute(XACMLConstants.DATA_TYPE);
                  if ((dt == null) || (dt.length() == 0)) {
                        throw new XACMLException(
                            XACMLSDKUtils.xacmlResourceBundle.getString(
                            "missing_attribute"));
                  }
                  attributeAssignments.add(aa);
              } else {
                    throw new XACMLException(
                        XACMLSDKUtils.xacmlResourceBundle.getString(
                        "missing_element"));
              }
          } 
      }
    }



    /**
     * Returns the ObligationId of this <code>Obligation</code>
     * @return the <code>URI</code> representing ObligationId of this 
     * <code>Obligation</code>
     */
    public URI getObligationId() {
        return obligationId;
    }

    /**
     * Sets the ObligationId of the <code>Obligation</code>
     * @param obligationId <code>URI</code> representing the ObligationId.
     * @exception XACMLException if the object is immutable
     */
    public void setObligationId(URI obligationId) 
            throws XACMLException {
        this.obligationId = obligationId;
    }

    /**
     * Returns the FullFillOn effect type of this obligation
     * @return the FullFillOn effect type of this obligation
     */
    public String getFulfillOn() {
        return fulfillOn;
    }

    /**
     * Sets the FullFillOn effect type of this obligation
     *
     * @param fulfillOn FullFillOn effect type of this obligation
     */
    public void setFulfillOn(String fulfillOn) 
            throws XACMLException {
        this.fulfillOn = fulfillOn;
    }

    /**
     * Returns XML elements corresponding to 
     * <code>AttributeAssignment</code> elements for  this obligation.
     *
     * @return the XML elements corresponding to 
     * <code>AttributeAssignment</code> elements for  this obligation.
     */
    public List getAttributeAssignments() {
        return attributeAssignments;
    }

    /**
     * Sets XML elements corresponding to 
     * <code>AttributeAssignment</code> elements for  this obligation.
     *
     * @param attributeAssignments XML elements corresponding to 
     * <code>AttributeAssignment</code> elements for  this obligation.
     */
    public void setAttributeAssignments(List attributeAssignments) 
            throws XACMLException {
      if (attributeAssignments == null) {
          return;
      }
      Iterator iter = attributeAssignments.iterator();
      while (iter.hasNext()) {
          Object obj = iter.next();
          if (!(obj instanceof Element)) {
                throw new XACMLException(
                    XACMLSDKUtils.xacmlResourceBundle.getString(
                    "not_xml_element"));
          }
          Element elem = (Element)obj;
          String aid = elem.getAttribute(XACMLConstants.ATTRIBUTE_ID);
          if ((aid == null) || (aid.length() == 0)) {
                throw new XACMLException(
                    XACMLSDKUtils.xacmlResourceBundle.getString(
                    "missing_attribute"));
          }
          String dt = elem.getAttribute(XACMLConstants.DATA_TYPE);
          if ((dt == null) || (dt.length() == 0)) {
                throw new XACMLException(
                    XACMLSDKUtils.xacmlResourceBundle.getString(
                    "missing_attribute"));
          }
      }
      this.attributeAssignments = attributeAssignments;
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
        sb.append("<").append(nsPrefix)
                .append(XACMLConstants.OBLIGATION);
        if(declareNS) {
            sb.append(" ").append(nsDeclaration);
        }
        if (obligationId != null) {
            sb.append(" ").append(XACMLConstants.OBLIGATION_ID).append("=")
                .append(XACMLSDKUtils.quote(obligationId.toString()));
        }
        if (fulfillOn != null) {
            sb.append(" ").append(XACMLConstants.FULFILL_ON).append("=")
                .append(XACMLSDKUtils.quote(fulfillOn));
        }
        sb.append(">\n");
        int length = 0;
        if (attributeAssignments != null) {
            length = attributeAssignments.size();
            for (int i = 0; i < length; i++) {
                Element elem = (Element)attributeAssignments.get(i);
                sb.append(XMLUtils.print(elem));
            }
        }
        sb.append("</").append(nsPrefix)
                .append(XACMLConstants.OBLIGATION).append(">\n");
        return sb.toString();
    }

   /**
    * Returns a string representation of this object
    *
    * @return a string representation
    * @exception XACMLException if conversion fails for any reason
    */
    public String toXMLString() throws XACMLException {
        return toXMLString(true, true);
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
    
}
