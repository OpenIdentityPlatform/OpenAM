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
 * $Id: Attribute.java,v 1.4 2008/09/03 22:28:40 weisun2 Exp $
 *
 */


package com.sun.identity.saml.assertion;

import com.sun.identity.common.SystemConfigurationUtil;
import java.util.*; 
import org.w3c.dom.*; 
import com.sun.identity.saml.common.SAMLUtilsCommon;
import com.sun.identity.saml.common.SAMLConstants;
import com.sun.identity.saml.common.SAMLException;
import com.sun.identity.saml.common.SAMLRequesterException;
import com.sun.identity.shared.xml.XMLUtils;

/**
 * The <code>Attribute</code> element specifies an attribute of the assertion
 * subject. The <code>Attribute</code> element is an extension of the
 * <code>AttributeDesignator</code> element 
 * that allows the attribute value to be specified. 
 *
 * @supported.all.api 
 */
public class Attribute extends AttributeDesignator {
    protected  List _attributeValue; 
    
    /**
     * Constructs an attribute element from an existing XML block.
     *
     * @param element representing a DOM tree element.
     * @exception SAMLException if there is an error in the sender or in the
     *            element definition.
     */
    public Attribute(Element element) throws SAMLException {
        // make sure that the input xml block is not null
        if (element == null) {
            SAMLUtilsCommon.debug.message("Attribute: Input is null.");
            throw new SAMLRequesterException(
                      SAMLUtilsCommon.bundle.getString("nullInput"));
        }
        // Make sure this is an Attribute.
        String tag = null;
        tag = element.getLocalName(); 
        if ((tag == null) || (!tag.equals("Attribute"))) {
            SAMLUtilsCommon.debug.message("Attribute: wrong input");
            throw new SAMLRequesterException(
                      SAMLUtilsCommon.bundle.getString("wrongInput"));
        }
        int i = 0;
        // handle attributes  
        NamedNodeMap atts = element.getAttributes(); 
        int attrCount = atts.getLength(); 
        for (i = 0; i < attrCount; i++) {
            Node att = atts.item(i);
            if (att.getNodeType() == Node.ATTRIBUTE_NODE) {
                String attName = att.getLocalName();
                if(attName == null) {
                   attName = att.getNodeName();
                }
                if (attName == null || attName.length() == 0) {
                    if (SAMLUtilsCommon.debug.messageEnabled()) {
                        SAMLUtilsCommon.debug.message("Attribute:" +
                                    "Attribute Name is either null or empty.");
                    }
                    continue;
                    //throw new SAMLRequesterException(
                            //  SAMLUtilsCommon.bundle.getString("nullInput"));
                }
                if (attName.equals("AttributeName")) {
                    this._attributeName =((Attr)att).getValue().trim();
                } else if (attName.equals("AttributeNamespace")) {
                    this._attributeNameSpace = ((Attr)att).getValue().trim(); 
                }
            }
        }   
        // AttributeName is required 
        if (_attributeName == null || _attributeName.length() == 0) {
            if (SAMLUtilsCommon.debug.messageEnabled()) {
                SAMLUtilsCommon.debug.message("Attribute: "+
                                        "AttributeName is required attribute");
            }
            throw new SAMLRequesterException(
                      SAMLUtilsCommon.bundle.getString("missingAttribute"));
        }
        
        // AttributeNamespace is required 
        if (_attributeNameSpace == null || _attributeNameSpace.length() == 0) {
            if (SAMLUtilsCommon.debug.messageEnabled()) {
                SAMLUtilsCommon.debug.message("Attribute: "+
                                   "AttributeNamespace is required attribute");
            }
            throw new SAMLRequesterException(
                      SAMLUtilsCommon.bundle.getString("missingAttribute"));
        }
        
        // handle the children of Attribute element
        NodeList  nodes = element.getChildNodes();
        int nodeCount = nodes.getLength(); 
        if (nodeCount > 0) {
            for (i = 0; i < nodeCount; i++) {
                Node currentNode = nodes.item(i);               
                if (currentNode.getNodeType() == Node.ELEMENT_NODE) {
                    String tagName = currentNode.getLocalName();                    
                    String tagNS = currentNode.getNamespaceURI(); 
                    if ((tagName == null) || tagName.length() == 0 ||
                        tagNS == null || tagNS.length() == 0) {
                        if (SAMLUtilsCommon.debug.messageEnabled()) {
                            SAMLUtilsCommon.debug.message("Attribute:" +
                                  " The tag name or tag namespace of child" +
                                  " element is either null or empty.");
                        }
                        throw new SAMLRequesterException(
                             SAMLUtilsCommon.bundle.getString("nullInput"));
                    }
                    if (tagName.equals("AttributeValue") &&
                        tagNS.equals(SAMLConstants.assertionSAMLNameSpaceURI)) {
                        if (_attributeValue == null) {
                            _attributeValue = new ArrayList(); 
                        }
                        if (!(_attributeValue.add((Element)currentNode))){
                            if (SAMLUtilsCommon.debug.messageEnabled()) {
                                SAMLUtilsCommon.debug.message(
                                    "Attribute: failed to "+ 
                                    "add to the attribute value list.");
                            }
                            throw new SAMLRequesterException(
                                    SAMLUtilsCommon.bundle.getString(
                                    "addListError"));   
                        }
                    } else {
                         if (SAMLUtilsCommon.debug.messageEnabled()) {
                             SAMLUtilsCommon.debug.message("Attribute:" +
                                   "wrong element:" + tagName); 
                         }
                         throw new SAMLRequesterException(
                             SAMLUtilsCommon.bundle.getString("wrongInput"));
                    }
                } // end of if (currentNode.getNodeType() == Node.ELEMENT_NODE) 
            } // end of for loop 
        }  // end of if (nodeCount > 0)
            
        if (_attributeValue == null || _attributeValue.isEmpty()) {
            if (SAMLUtilsCommon.debug.messageEnabled()) {
                SAMLUtilsCommon.debug.message("Attribute: " +
                                "should contain at least one AttributeValue.");
            }
            throw new SAMLRequesterException(
                      SAMLUtilsCommon.bundle.getString("missingElement"));
        }
    }
    
    /**
     * Constructs an instance of <code>Attribute</code>.
     *
     * @param name A String representing <code>AttributeName</code> (the name
     *        of the attribute). 
     * @param nameSpace A String representing the namespace in which
     *        <code>AttributeName</code> elements are interpreted.
     * @param values A List of DOM element representing the
     *        <code>AttributeValue</code> object.
     * @exception SAMLException if there is an error in the sender or in the
     *            element definition.
     */
    public Attribute(String name, String nameSpace, List values) 
                                                    throws SAMLException {
        super(name, nameSpace);  
        if (values == null || values.isEmpty()) {
            if (SAMLUtilsCommon.debug.messageEnabled()) {
                SAMLUtilsCommon.debug.message("Attribute: AttributeValue is" + 
                                        "required.");  
            }
            throw new SAMLRequesterException(
                      SAMLUtilsCommon.bundle.getString("nullInput")); 
        }       
        if (_attributeValue == null) {
            _attributeValue = new ArrayList();                 
        }
        // Make sure this is a list of AttributeValue
        Iterator iter = values.iterator();  
        String tag = null; 
        while (iter.hasNext()) {
            tag = ((Element) iter.next()).getLocalName(); 
            if ((tag == null) || (!tag.equals("AttributeValue"))) {
                if (SAMLUtilsCommon.debug.messageEnabled()) {
                    SAMLUtilsCommon.debug.message(
                        "AttributeValue: wrong input.");
                }
                throw new SAMLRequesterException(
                    SAMLUtilsCommon.bundle.getString("wrongInput"));
            }
        }
        _attributeValue = values; 
    }
    
    /**
     * Constructs an instance of <code>Attribute</code>.
     *
     * @param name The name of the attribute. 
     * @param nameSpace The namespace in which <code>AttributeName</code>
     *        elements are interpreted.
     * @param attributeValue an <code>AttributeValue</code> object.
     * @exception SAMLException if there is an error in the sender or in
     *            the element definition.
     */
    public Attribute(String name, String nameSpace, String attributeValue) 
                                                    throws SAMLException {
        super(name, nameSpace);
        String escapeAttVal = SystemConfigurationUtil.getProperty(
            SAMLConstants.ESCAPE_ATTR_VALUE, "true"); 
        boolean escapeAtt = "true".equalsIgnoreCase(escapeAttVal) ?
            true : false; 
        if (escapeAtt) {     
            this.addAttributeValue(XMLUtils.
                escapeSpecialCharacters(attributeValue));
        } else {
            this.addAttributeValue(attributeValue);
        }
    }
    
    /**
     * Returns <code>AttributeValue</code> from the Attribute.
     *
     * @return A list of DOM Element representing the
     * <code>AttributeValue</code> block.
     * @throws SAMLException
     */
    public  List getAttributeValue()  throws SAMLException {
        return _attributeValue; 
    }
    
    /**
     * Adds <code>AttributeValue</code> to the Attribute.
     *
     * @param value A String representing <code>AttributeValue</code>. 
     * @exception SAMLException 
     */
    public void addAttributeValue(String value) throws SAMLException {
        if (value == null || value.length() == 0) {
             if (SAMLUtilsCommon.debug.messageEnabled()) {
                 SAMLUtilsCommon.debug.message(
                     "addAttributeValue: Input is null");
            }
            throw new SAMLRequesterException(
                      SAMLUtilsCommon.bundle.getString("nullInput"));   
        }
        StringBuffer sb = new StringBuffer(300);
        sb.append("<").append(SAMLConstants.ASSERTION_PREFIX).
                append("AttributeValue").
                append(SAMLConstants.assertionDeclareStr).
                append(">").append(value).append("</").
                append(SAMLConstants.ASSERTION_PREFIX).
                append("AttributeValue>");
        try {
            Element ele = XMLUtils.toDOMDocument(
                        sb.toString().trim(),
                        SAMLUtilsCommon.debug).getDocumentElement();
            if (_attributeValue == null) {
                _attributeValue = new ArrayList(); 
            }
            if (!(_attributeValue.add(ele))){
                if (SAMLUtilsCommon.debug.messageEnabled()) {
                    SAMLUtilsCommon.debug.message("Attribute: failed to "+ 
                        "add to the attribute value list.");
                }
                throw new SAMLRequesterException(
                    SAMLUtilsCommon.bundle.getString("addListError"));   
            }
        } catch (Exception e) {
            SAMLUtilsCommon.debug.error("addAttributeValue error", e);
            throw new SAMLRequesterException("Exception in addAttributeValue" +
                e.getMessage()); 
        }
    }
    
    /**
     * Adds <code>AttributeValue</code> to the Attribute.
     *
     * @param element An Element object representing
     *        <code>AttributeValue</code>.
     * @exception SAMLException 
     */
    public void addAttributeValue(Element element) throws SAMLException {
        if (element == null) {
            if (SAMLUtilsCommon.debug.messageEnabled()) {
                SAMLUtilsCommon.debug.message(
                    "addAttributeValue: input  is null.");
            }
            throw new SAMLRequesterException(
                      SAMLUtilsCommon.bundle.getString("nullInput"));   
        }
            
        String tag = element.getLocalName(); 
        if ((tag == null) || (!tag.equals("AttributeValue"))) {
            if (SAMLUtilsCommon.debug.messageEnabled()) {
                SAMLUtilsCommon.debug.message("AttributeValue: wrong input.");
            }
            throw new SAMLRequesterException(
                          SAMLUtilsCommon.bundle.getString("wrongInput"));
        }
        try {
            if (_attributeValue == null) {
                _attributeValue = new ArrayList(); 
            }
            if (!(_attributeValue.add(element))){
                if (SAMLUtilsCommon.debug.messageEnabled()) {
                    SAMLUtilsCommon.debug.message("Attribute: failed to "+ 
                        "add to the attribute value list.");
                }
                throw new SAMLRequesterException(
                    SAMLUtilsCommon.bundle.getString("addListError"));   
            }
        } catch (Exception e) {
            SAMLUtilsCommon.debug.error("addAttributeValue error", e);
            throw new SAMLRequesterException("Exception in addAttributeValue" +
                                            e.getMessage()); 
        }
    }
    
    
    /**
     * Returns a String representation of the
     * <code>&lt;saml:Attribute&gt;</code> element,
     *
     * @return A string containing the valid XML for this element.
     */
    public String  toString() {
        return toString(true, false); 
    }
    
    /**
     * Returns a String representation of the
     * <code>&lt;saml:Attribute&gt;</code> element.
     *
     * @param includeNS Determines whether or not the namespace qualifier 
     *        is prepended to the Element when converted
     * @param declareNS Determines whether or not the namespace is declared 
     *        within the Element.
     * @return A string containing the valid XML for this element
     */
    public String  toString(boolean includeNS, boolean declareNS) {
        StringBuffer result = new StringBuffer(1000);
        String prefix = "";
        String uri = "";
        if (includeNS) {
            prefix = SAMLConstants.ASSERTION_PREFIX;
        }
        if (declareNS) {
            uri = SAMLConstants.assertionDeclareStr;
        } 
        result.append("<").append(prefix).append("Attribute").append(uri).
               append(" AttributeName=\"").append(_attributeName).
               append("\" AttributeNamespace=\"").append(_attributeNameSpace).
               append("\">\n");         
        
        Iterator iter = _attributeValue.iterator();  
        while (iter.hasNext()) {
            result.append(XMLUtils.printAttributeValue((Element)iter.next(), 
                          prefix)).append("\n"); 
        }
        result.append("</").append(prefix).append("Attribute>\n");
        return result.toString();
   }    
}
