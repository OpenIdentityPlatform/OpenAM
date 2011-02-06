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
 * $Id: AttributeDesignator.java,v 1.2 2008/06/25 05:47:31 qcheng Exp $
 *
 */


package com.sun.identity.saml.assertion;

import java.util.*; 
import java.net.*;
import org.w3c.dom.*; 
import com.sun.identity.saml.common.SAMLUtilsCommon;
import com.sun.identity.saml.common.SAMLConstants;
import com.sun.identity.saml.common.SAMLException;
import com.sun.identity.saml.common.SAMLRequesterException;

/**
 * The <code>AttributeDesignator</code> element identifies an attribute
 * name within an attribute namespace. The element is used in an attribute query
 * to request that attribute values within a specific namespace be returned. 
 * @supported.all.api
 */
public class AttributeDesignator {
    protected String _attributeName = null;
    //_attributeNameSpace type should be URI, for now, define as String 
    protected String _attributeNameSpace = null;      
    
    /**
     *Default constructor
     */
    protected AttributeDesignator() {
    }
  
    /**
     * Constructs an attribute designator element from an existing XML block.
     *
     * @param element representing a DOM tree element.
     * @exception SAMLException if that there is an error in the sender or
     *            in the element definition.
     */
    public AttributeDesignator(Element element) throws SAMLException { 
        // make sure that the input xml block is not null
        if (element == null) {
            SAMLUtilsCommon.debug.message(
            "AttributeDesignator: Input is null.");
            throw new SAMLRequesterException(
                      SAMLUtilsCommon.bundle.getString("nullInput"));
        }
        // Make sure this is an AttributeDesignator.
        String tag = null;
        tag = element.getLocalName(); 
        if ((tag == null) || (!tag.equals("AttributeDesignator"))) {
            SAMLUtilsCommon.debug.message("AttributeDesignator: wrong input");
            throw new SAMLRequesterException(
                      SAMLUtilsCommon.bundle.getString("wrongInput"));
        }
        
        // handle attributes  
        int i = 0; 
        NamedNodeMap atts = ((Node)element).getAttributes(); 
        int attrCount = atts.getLength(); 
        for (i = 0; i < attrCount; i++) {
            Node att = atts.item(i);
            if (att.getNodeType() == Node.ATTRIBUTE_NODE) {
                String attName = att.getLocalName();
                if (attName == null || attName.length() == 0) {
                    if (SAMLUtilsCommon.debug.messageEnabled()) {
                        SAMLUtilsCommon.debug.message("AttributeDesignator:" +
                                "Attribute Name is either null or empty.");
                    }
                    throw new SAMLRequesterException(
                        SAMLUtilsCommon.bundle.getString("nullInput"));
                }
                if (attName.equals("AttributeName")) {
                    _attributeName =((Attr)att).getValue().trim();
                } else if (attName.equals("AttributeNamespace")) {
                    _attributeNameSpace = ((Attr)att).getValue().trim(); 
                }
            }
        }
        // AttributeName is required 
        if (_attributeName == null || _attributeName.length() == 0) {
            if (SAMLUtilsCommon.debug.messageEnabled()) {
                SAMLUtilsCommon.debug.message("AttributeDesignator: "+
                                        "AttributeName is required attribute");
            }
            throw new SAMLRequesterException(
                        SAMLUtilsCommon.bundle.getString("missingAttribute"));
        }
        
        // AttributeNamespace is required 
        if (_attributeNameSpace == null || _attributeNameSpace.length() == 0) {
            if (SAMLUtilsCommon.debug.messageEnabled()) {
                SAMLUtilsCommon.debug.message("AttributeDesignator: "+
                                    "AttributeNamespace is required attribute");
            }
            throw new SAMLRequesterException(
                        SAMLUtilsCommon.bundle.getString("missingAttribute"));
        }
        
        // handle the children of AttributeDesignator element
        // Since AttributeDesignator does not have any child element_node, 
        // we will throw exception if we found any such child. 
        NodeList  nodes = element.getChildNodes();
        int nodeCount = nodes.getLength(); 
        if (nodeCount > 0) {
            for (i = 0; i < nodeCount; i++) {
                Node currentNode = nodes.item(i);               
                if (currentNode.getNodeType() == Node.ELEMENT_NODE) {
                    if (SAMLUtilsCommon.debug.messageEnabled()) {
                        SAMLUtilsCommon.debug.message(
                        "AttributeDesignator: illegal input!");
                    }
                    throw new SAMLRequesterException(
                              SAMLUtilsCommon.bundle.getString("wrongInput"));
                }
            }
        }
    }
    
    /**
     * Constructs an instance of <code>AttributeDesignator</code>.
     *
     * @param name the name of the attribute. 
     * @param nameSpace the namespace in which <code>AttributeName</code>
     *        elements are interpreted.
     * @exception SAMLException if there is an error in the sender or in the
     *            element definition.
     */
    public AttributeDesignator(String name, String nameSpace) 
                              throws SAMLException {
        if (name == null || name.length() == 0) {
            if (SAMLUtilsCommon.debug.messageEnabled()) {
                SAMLUtilsCommon.debug.message("AttributeDesignator:" +
                                        "AttributeName is required!");
            }
            throw new SAMLRequesterException(
                      SAMLUtilsCommon.bundle.getString("nullInput"));
        } else {
            _attributeName = name; 
        }
        if (nameSpace == null || nameSpace.length() == 0) {
            if (SAMLUtilsCommon.debug.messageEnabled()) {
                SAMLUtilsCommon.debug.message("AttributeDesignator: " +
                                        "AttributeNamespace is required!");
            }
            throw new SAMLRequesterException(
                      SAMLUtilsCommon.bundle.getString("nullInput"));
        } else { 
             _attributeNameSpace = nameSpace;
        }
    }   
    
    /**
     * Returns attribute name from the <code>AttributeDesignator</code>.
     *
     * @return A String representing the attribute name.
     */
    public String getAttributeName() {
        return _attributeName; 
    }
    
    /**
     * Returns attribute name space from the
     * <code>AttributeDesignator</code>.
     *
     * @return A String representing the attribute name space.
     */
    public String getAttributeNamespace() {
        return _attributeNameSpace; 
    }
    
    /** 
     * Returns a String representation of the 
     * <code>&lt;saml:AttributeDesignator&gt;</code> element.
     *
     * @return A string containing the valid XML for this element.
     */
    public String toString() {
        return (toString(true, false)); 
    }
    
    /**
     * Returns a String representation of the
     * <code>&lt;saml:AttributeDesignator&gt;</code> element.
     *
     * @param includeNS Determines whether or not the namespace qualifier is 
     *        to the Element when converted
     * @param declareNS Determines whether or not the namespace is declared
     *        within the Element.
     * @return A string containing the valid XML for this element.
     */
    public String toString(boolean includeNS, boolean declareNS) {
        StringBuffer result = new StringBuffer(200);
        String prefix = "";
        String uri = "";
        if (includeNS) {
            prefix = SAMLConstants.ASSERTION_PREFIX;
        }
        if (declareNS) {
            uri = SAMLConstants.assertionDeclareStr;
        }    
        result.append("<").append(prefix).append("AttributeDesignator ").
               append(uri).append(" AttributeName=\"").append(_attributeName).
               append("\" AttributeNamespace=\"").append(_attributeNameSpace).
               append("\" />\n");       
        return ((String)result.toString());
    }                       
}

