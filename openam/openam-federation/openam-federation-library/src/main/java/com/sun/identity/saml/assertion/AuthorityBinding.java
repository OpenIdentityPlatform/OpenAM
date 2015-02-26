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
 * $Id: AuthorityBinding.java,v 1.2 2008/06/25 05:47:32 qcheng Exp $
 *
 */


package com.sun.identity.saml.assertion;

import java.util.*; 
import org.w3c.dom.*; 
import com.sun.identity.saml.common.SAMLUtilsCommon;
import com.sun.identity.saml.common.SAMLConstants;
import com.sun.identity.saml.common.SAMLException;
import com.sun.identity.saml.common.SAMLRequesterException;

/**
 * The <code>AuthorityBinding</code> element may be used to indicate
 * to a replying party receiving an <code>AuthenticationStatement</code> that
 * a SAML authority may be available to provide additional information about
 * the subject of the statement. A single SAML authority may advertise its
 * presence over multiple protocol binding, at multiple locations, and as
 * more than one kind of authority by sending multiple elements as needed.
 * @supported.all.api
 */
public class AuthorityBinding  { 
    private String _binding = null;
    private String _location = null; 
    private int _authorityKind = AuthorityKindType.NOT_SUPPORTED;  
     
/**
 *The <code>AuthorityKindType</code> is an inner class defining constants for 
 *the representing the type of SAML protocol queries to which the authority 
 *described by this element will respond.
 *<br>1 implies <code>AUTHENTICATION</code>
 *<br>2 implies <code>ATTRIBUTE</code>
 *<br>3 implies <code>AUTHORIZATION</code>
 *<br>-1  implies <code>NOT_SUPPORTED</code>
 */
    public static class AuthorityKindType {

        /**
         * Authentication Query.
         */
        public static final int  AUTHENTICATION = 1;  // "authentication"

        /**
         * Attribute Query.
         */
        public static final int  ATTRIBUTE      = 2;       // "attribute"

        /**
         * Authorization Decision Query.
         */
        public static final int  AUTHORIZATION  = 3;   // "authorization"

        /**
         * Unsupported Query.
         */
        public static final int  NOT_SUPPORTED  = -1;  // not supported 
    }
    
    /**
     * Constructs an <code>AuthorityBinding</code> element from an existing XML
     * block.
     *
     * @param element representing a DOM tree element.
     * @exception SAMLException if there is an error in the sender or in the
     *            element definition.
     */
    public AuthorityBinding(Element element) throws SAMLException {
        // make sure that the input xml block is not null
        if (element == null) {
            SAMLUtilsCommon.debug.message("AuthorityBinding: Input is null.");
            throw new SAMLRequesterException(
                      SAMLUtilsCommon.bundle.getString("nullInput"));
        }
        // Make sure this is an AuthorityBinding.
        String tag = null;
        tag = element.getLocalName(); 
        if ((tag == null) || (!tag.equals("AuthorityBinding"))) {
            SAMLUtilsCommon.debug.message("AuthorityBinding: wrong input");
            throw new SAMLRequesterException(
                      SAMLUtilsCommon.bundle.getString("wrongInput"));
        }
        
        int i = 0; 
        //handle the attributes of AuthorityBinding
        NamedNodeMap atts = ((Node)element).getAttributes(); 
        int attrCount = atts.getLength(); 
        for (i = 0; i < attrCount; i++) {
            Node att = atts.item(i);
            if (att.getNodeType() == Node.ATTRIBUTE_NODE) {
                String attName = att.getLocalName();
                if (attName == null || attName.length() == 0) {
                    if (SAMLUtilsCommon.debug.messageEnabled()) {
                        SAMLUtilsCommon.debug.message("AuthorityBinding: " +
                            "Attribute Name is either null or empty.");
                    }
                    throw new SAMLRequesterException(
                        SAMLUtilsCommon.bundle.getString("nullInput"));
                }
                if (attName.equals("AuthorityKind")) {
                    String kind = ((Attr)att).getValue().trim();
                    if (kind == null || kind.length() == 0) {
                        if (SAMLUtilsCommon.debug.messageEnabled()) {
                            SAMLUtilsCommon.debug.message("AuthorityBinding: "+
                                            "AuthorityKind is null or empty.");
                        }
                        throw new SAMLRequesterException(
                            SAMLUtilsCommon.bundle.getString(
                            "missingAttributeValue"));
                    }
                    if (kind.equals("samlp:AuthenticationQuery")) 
                        _authorityKind = AuthorityKindType.AUTHENTICATION; 
                    else if (kind.equals("samlp:AttributeQuery"))
                        _authorityKind = AuthorityKindType.ATTRIBUTE; 
                    else if (kind.equals("samlp:AuthorizationDecisionQuery"))
                        _authorityKind = AuthorityKindType.AUTHORIZATION; 
                    else {
                        if (SAMLUtilsCommon.debug.messageEnabled()) {
                            SAMLUtilsCommon.debug.message("AuthorityBinding: " +
                                           "The type of authority is illegal!");
                        }
                        throw new SAMLRequesterException(
                            SAMLUtilsCommon.bundle.getString("wrongInput"));  
                    }
                } else if (attName.equals("Binding")) { 
                    _binding = ((Attr)att).getValue().trim();
                    if (_binding == null || _binding.length() == 0) {
                        if (SAMLUtilsCommon.debug.messageEnabled()) {
                            SAMLUtilsCommon.debug.message("AuthorityBinding: "+
                                "Binding URI is null or empty.");
                        }
                        throw new SAMLRequesterException(
                            SAMLUtilsCommon.bundle.getString(
                            "missingAttributeValue"));
                    }
                }  else if (attName.equals("Location")) {
                     _location = ((Attr)att).getValue().trim();
                     if (_location == null || _location.length() == 0) {
                         if (SAMLUtilsCommon.debug.messageEnabled()) {
                             SAMLUtilsCommon.debug.message("AuthorityBinding:"
                                 + " Location URI is null or empty.");
                         }
                        throw new SAMLRequesterException(
                            SAMLUtilsCommon.bundle.getString(
                            "missingAttributeValue"));
                     }
                }
            }  // end of  if (att.getNodeType() == Node.ATTRIBUTE_NODE)
        }  // end of for loop 
         
        // AuthorityKind is required 
        if ( _authorityKind == AuthorityKindType.NOT_SUPPORTED) {
            if (SAMLUtilsCommon.debug.messageEnabled()) {
                SAMLUtilsCommon.debug.message("AuthorityBinding: " +
                                        "AuthorityKind is required attribute");
            }
            throw new SAMLRequesterException(
                        SAMLUtilsCommon.bundle.getString("missingAttribute"));
        }   
        // Location is required 
        if ( _location == null) {
            if (SAMLUtilsCommon.debug.messageEnabled()) {
                SAMLUtilsCommon.debug.message("AuthorityBinding: Location is " +
                                        "required attribute");
            }
            throw new SAMLRequesterException(
                        SAMLUtilsCommon.bundle.getString("missingAttribute"));
        }
        // Binding is required 
        if ( _binding == null) {
            if (SAMLUtilsCommon.debug.messageEnabled()) {
                SAMLUtilsCommon.debug.message("AuthorityBinding: Binding is" +
                                        " required attribute");
            }
            throw new SAMLRequesterException(
                        SAMLUtilsCommon.bundle.getString("missingAttribute"));
        }
        // handle the children elements of AuthorityBinding
        // Since it does not contain any child element_node, 
        // we will throw exception if we found any such child. 
        NodeList  nodes = element.getChildNodes();
        int nodeCount = nodes.getLength(); 
        if (nodeCount > 0) {
            for (i = 0; i < nodeCount; i++) {
                Node currentNode = nodes.item(i);               
                if (currentNode.getNodeType() == Node.ELEMENT_NODE) {
                    if (SAMLUtilsCommon.debug.messageEnabled()) {
                        SAMLUtilsCommon.debug.message("AuthorityBinding: " +
                                                " illegal input!");
                    }
                    throw new SAMLRequesterException(
                        SAMLUtilsCommon.bundle.getString("wrongInput"));
                }
            }
        }
    }
    
    /**
     *Constructs <code>AuthorityBinding</code>
     *@param authKind A integer representing the type of SAML protocol queries
     *       to which the authority described by this element will 
     *       respond. If you do NO specify this attribute, pass in 
     *       value "-1". 
     *@param location A URI describing how to locate and communicate with the 
     *       authority, the exact syntax of which depends on the 
     *       protocol binding in use.  
     *@param binding A String representing a URI reference identifying the  
     *       SAML protocol binding to use in  communicating with the 
     *       authority.         
     *@exception SAMLException if there is an error in the sender or in the
     *           element definition.
     */         
    public AuthorityBinding(int authKind, String location, String binding)
                                                    throws SAMLException {
        if (location == null || location.length() == 0){
            if (SAMLUtilsCommon.debug.messageEnabled()) {
                  SAMLUtilsCommon.debug.message("AuthorityBinding: The " +
                      "Location URI can not be null or empty!");
            }
            throw new SAMLRequesterException(
                        SAMLUtilsCommon.bundle.getString("nullInput"));
        } else {
            _location = location;
        }
        
         if (binding == null || binding.length() == 0){
            if (SAMLUtilsCommon.debug.messageEnabled()) {
                  SAMLUtilsCommon.debug.message("AuthorityBinding: The " +
                      "binding URI can not be null or empty!");
            }
            throw new SAMLRequesterException(
                        SAMLUtilsCommon.bundle.getString("nullInput"));
        } else {
            _binding = binding;
        }
        
        if (authKind >= AuthorityKindType.AUTHENTICATION &&
            authKind <= AuthorityKindType.AUTHORIZATION ) {
            _authorityKind = authKind; 
        } else {
            if (SAMLUtilsCommon.debug.messageEnabled()) {
                 SAMLUtilsCommon.debug.message("AuthorityBinding:The type of "+
                                          "authority is illegal!");
            }
            throw new SAMLRequesterException(
                        SAMLUtilsCommon.bundle.getString("wrongInput"));   
        }
    }
    
    /**
     *Gets the Location URI
     *@return  A String representing the URI describing how to locate and 
     *          communicate with the authority, the exact syntax of which
     *          depends on the protocol binding in use.  
     */
    public String getLocation() {
        return _location;
    }
    
    /**
     *Gets the binding URI 
     *@return A String representing a URI reference identifying the SAML 
     *        protocol binding to use in  communicating with the authority.  
     */
    public String getBinding() {
        return _binding;
    }
    
    /**
     * Returns the <code>AuthorityKind</code>.
     *
     * @return A integer representing the type of SAML protocol queries to which
     *        the authority described by this element will respond.
     */
    public int getAuthorityKind() {
        return _authorityKind; 
    }
    
    /** 
     * Returns a String representation of the <code>AuthorityBinding</code>
     *
     * @return A String representation of the
     *        <code>AuthorityBinding</code> element.
     */
    public String toString() {
        return (toString(true, false)); 
    }
   
    /** 
     * Returns a String representation of the <code>AuthorityBinding</code>
     * @param includeNS Determines whether or not the namespace qualifier is 
     *        prepended to the Element when converted
     * @param declareNS Determines whether or not the namespace is declared 
     *        within the Element.
     * @return A String representation of the <code>AuthorityBinding</code>
     *        element.
     */                             
    public String toString(boolean includeNS, boolean declareNS) {
        StringBuffer result  = new StringBuffer(300); 
        String prefix = "";
        String uri = "";
        if (includeNS) {
            prefix = SAMLConstants.ASSERTION_PREFIX;
        }
        if (declareNS) {
            uri = SAMLConstants.assertionDeclareStr;
        }
        result.append("<").append(prefix).append("AuthorityBinding").
               append(uri).append(" AuthorityKind=\"");
        switch(_authorityKind) {
            case AuthorityKindType.AUTHENTICATION: 
                 result.append("samlp:AuthenticationQuery");
                 break; 
            case AuthorityKindType.ATTRIBUTE: 
                 result.append("samlp:AttributeQuery");
                 break ; 
            case AuthorityKindType.AUTHORIZATION:
                 result.append("samlp:AuthorizationDecisionQuery");
                 break ;  
        }
        result.append("\"").append(" Location=\"").append(_location).
               append("\"").append(" Binding=\"").append(_binding).
               append("\"").append(" />\n");
        return(result.toString());
    }
}

