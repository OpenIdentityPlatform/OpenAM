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
 * $Id: Action.java,v 1.2 2008/06/25 05:47:31 qcheng Exp $
 *
 */


package com.sun.identity.saml.assertion;

import java.util.*; 
import org.w3c.dom.*; 
import com.sun.identity.saml.common.SAMLUtilsCommon;
import com.sun.identity.saml.common.SAMLConstants;
import com.sun.identity.saml.common.SAMLException;
import com.sun.identity.saml.common.SAMLRequesterException;
import com.sun.identity.shared.xml.XMLUtils;

/**
 *This class is designed for <code>Action</code> element in SAML core 
 *assertion. This element specifies an action on specified resource for
 *which permission is sought. 
 *@supported.all.api
 */
public class Action {
    //An action sought to be performed on the specified resource 
    protected String  _action = null; 
   
    //represent the attribute NameSpace of the <code>Action</code> element 
    protected String  _namespace = SAMLConstants.ACTION_NAMESPACE_NEGATION; 
    
    /**
     * Constructs an action element from an existing XML block.
     *
     * @param element representing a DOM tree element.
     * @exception SAMLException f there is an error in the sender or in 
     *            the element definition.
     */
    public Action(Element element) throws SAMLException{
        // make sure that the input xml block is not null
        if (element == null) {
            SAMLUtilsCommon.debug.message("Action: Input is null.");
            throw new SAMLRequesterException(
                      SAMLUtilsCommon.bundle.getString("nullInput"));
        }
        // Make sure this is as Action.
        String tag = null;
        tag = element.getLocalName(); 
        if ((tag == null) || (!tag.equals("Action"))) {
            SAMLUtilsCommon.debug.message("Action: wrong input");
            throw new SAMLRequesterException(
                      SAMLUtilsCommon.bundle.getString("wrongInput"));
        }
        
        // handle the attribute of <code>Action</code> element  
        // Note: element attributes are not children of ELEMENT_NODEs but
        // are properties of their associated ELEMENT_NODE. 
        NamedNodeMap atts = ((Node)element).getAttributes();  
        int attrCount = atts.getLength(); 
        int i = 0; 
        for (i = 0; i < attrCount; i++) {
            Node att = atts.item(i);
            if (att.getNodeType() == Node.ATTRIBUTE_NODE) {
                String attName = att.getLocalName();
                if (attName == null || attName.length() == 0) {
                    if (SAMLUtilsCommon.debug.messageEnabled()) {
                        SAMLUtilsCommon.debug.message("Action: Attribute Name" +
                                                "is either null or empty.");
                    }
                    throw new SAMLRequesterException(
                              SAMLUtilsCommon.bundle.getString("nullInput"));
                }
                if (attName.equals("Namespace")) {
                    _namespace = ((Attr)att).getValue().trim(); 
                }
                if ((_namespace == null) || (_namespace.length() == 0)) {
                    _namespace = SAMLConstants.ACTION_NAMESPACE_NEGATION;
                }
            }
        }
        //handle the children elements of <code>Action</code>
        NodeList  nodes = element.getChildNodes();
        int nodeCount = nodes.getLength();
        if (nodeCount > 0) {
            for (i = 0; i < nodeCount; i++) {
                Node currentNode = nodes.item(i);               
                if (currentNode.getNodeType() == Node.ELEMENT_NODE) {
                    if (SAMLUtilsCommon.debug.messageEnabled()) {
                        SAMLUtilsCommon.debug.message("Action: Wrong input");
                    }
                    throw new SAMLRequesterException(
                              SAMLUtilsCommon.bundle.getString("wrongInput"));
                }
            }
        }
        _action = XMLUtils.getElementValue(element); 
        // check if the action is null.
        if (_action == null) {
            if (SAMLUtilsCommon.debug.messageEnabled()) {
                SAMLUtilsCommon.debug.message("Action is null.");    
            }
            throw new SAMLRequesterException(
                      SAMLUtilsCommon.bundle.getString("missingElementValue"));
        }
        if (!isValid(_action, _namespace)) {
            if (SAMLUtilsCommon.debug.messageEnabled()) {
                SAMLUtilsCommon.debug.message("Action is invalid"); 
            }
            throw new SAMLRequesterException(
                      SAMLUtilsCommon.bundle.getString("invalidAction"));
        }
    }
    
    /**
     * Convenience constructor of &lt;Action&gt;
     * @param namespace The attribute "namespace" of
     *        <code>&lt;Action&gt;</code> element
     * @param action A String representing an action
     * @exception SAMLException if there is an error in the sender or in
     *            the element definition.
     */
    public Action(String namespace, String action) throws SAMLException {
        if (namespace == null || namespace.length() == 0) { 
            if (SAMLUtilsCommon.debug.messageEnabled()) {
                SAMLUtilsCommon.debug.message("Action:Take default " + 
                                              "Attribute Namespace.");
            }
        } else {
            _namespace = namespace;     
        }
        if (action == null || action.length() == 0) {
            if (SAMLUtilsCommon.debug.messageEnabled()) {
                SAMLUtilsCommon.debug.message("Action:Action is " +
                                              "null or empty.");
            }
            throw new SAMLRequesterException(
                      SAMLUtilsCommon.bundle.getString("nullInput"));
        } else {
            _action = action; 
        }
        if (!isValid(_action, _namespace)) {
            if (SAMLUtilsCommon.debug.messageEnabled()) {
                SAMLUtilsCommon.debug.message("Action is invalid"); 
            }
            throw new SAMLRequesterException(
                      SAMLUtilsCommon.bundle.getString("invalidAction"));
        }
    }
    
    /**
     *Check if the input action string is valid within its specified namespace.
     *@param action A String representing the action 
     *@param nameSpace The Actions element's namespace. There are four
     *          namespaces that are pre-defined. Action will be checked against
     *          these namespaces.
     *(1) urn:oasis:names:tc:SAML:1.0:action:rwedc
     *String used in the ActionNamespace attribute to refer to common sets of 
     *actions to perform on resources. 
     *Title: Read/Write/Execute/Delete/Control
     *Defined actions: Read Write Execute Delete Control 
     *These actions are interpreted in the normal manner, i.e. 
     *      Read:  The subject may read the resource 
     *      Write: The subject may modify the resource 
     *      Execute: The subject may execute the resource 
     *      Delete: The subject may delete the resource 
     *      Control: The subject may specify the access control policy for the 
     *              resource 
     *(2) urn:oasis:names:tc:SAML:1.0:action:rwedc-negation
     *String used in the ActionNamespace attribute to refer to common sets of 
     *actions to perform on resources. 
     *Title: Read/Write/Execute/Delete/Control with Negation
     *Defined actions:
     *Read Write Execute Delete Control ~Read ~Write ~Execute ~Delete ~Control
     *      Read:  The subject may read the resource 
     *      Write: The subject may modify the resource 
     *      Execute: The subject may execute the resource 
     *      Delete: The subject may delete the resource 
     *      Control: The subject may specify the access control policy for the 
     *              resource 
     *      ~Read:  The subject may NOT read the resource 
     *      ~Write: The subject may NOT modify the resource 
     *      ~Execute: The subject may NOT execute the resource 
     *      ~Delete: The subject may NOT delete the resource 
     *      ~Control: The subject may NOT specify the access control policy for
     *              the resource 
     *An application MUST NOT authorize both an action and its negated form.
     *(3) urn:oasis:names:tc:SAML:1.0:ghpp
     *String used in the ActionNamespace attribute to refer to common sets of 
     *actions to perform on resources. 
     *Title: Get/Head/Put/Post
     *Defined actions: 
     *          GET HEAD PUT POST 
     *These actions bind to the corresponding HTTP operations. For example a
     *subject authorized to perform the GET action on a resource is authorized
     *to retrieve it. The GET and HEAD actions loosely correspond to the 
     *conventional read permission and the PUT and POST actions to the write 
     *permission. The correspondence is not exact however since a HTTP GET 
     *operation may cause data to be modified and a POST operation may cause
     *modification to a resource other than the one specified in the request. 
     *For this reason a separate Action URI specifier is provided. 
     *(4) urn:oasis:names:tc:SAML:1.0:action:unix
     *String used in the ActionNamespace attribute to refer to common sets of 
     *actions to perform on resources. 
     *Title: UNIX File Permissions
     *Defined actions: 
     *The defined actions are the set of UNIX file access permissions expressed
     *in the numeric (octal) notation. The action string is a four digit numeric
     *code: extended user group world 
     *Where the extended access permission has the value  
     *                  +2 if sgid is set 
     *                  +4 if suid is set 
     *The user group and world access permissions have the value 
     *                  +1 if execute permission is granted 
     *                  +2 if write permission is granted 
     *                  +4 if read permission is granted 
     *For example 0754 denotes the UNIX file access permission:
     *user read, write 
     *and execute, group read and execute and world read. 
     *@return A boolean representation if the action is valid within its 
     *        specified name space. If the namespace param is not one of the
     *        four defined actions namespaces, true is returned.
     */ 
    private boolean isValid(String action, String namespace) {
       if (namespace.equals(SAMLConstants.ACTION_NAMESPACE)) {
           if (action.equals("Read")|| action.equals("Write") ||
               action.equals("Execute") || action.equals("Delete") ||
               action.equals("Control")) {
               return true;
           } else {
               return false; 
           }
       }
           
      if (namespace.equals(SAMLConstants.ACTION_NAMESPACE_NEGATION)) {
          if (action.equals("Read") || action.equals("~Read") ||
              action.equals("Write") || action.equals("~Write") ||
              action.equals("Execute") || action.equals("~Execute") ||
              action.equals("Delete") ||  action.equals("~Delete") ||
              action.equals("Control") || action.equals("~Control")) {
               return true;
           } else { 
               return false;
           }
      }
       
      if (namespace.equals(SAMLConstants.ACTION_NAMESPACE_GHPP)) {
          if (action.equals("GET") || action.equals("HEAD") ||
              action.equals("PUT") || action.equals("POST")) {
               return true;
           } else {
               return false; 
           }
      }
       
      if (namespace.equals(SAMLConstants.ACTION_NAMESPACE_UNIX)) {
          int permissionNum = 0; 
          try{
              permissionNum = Integer.parseInt(action); 
          } catch (NumberFormatException ne) {
              if (SAMLUtilsCommon.debug.messageEnabled()) {
                  SAMLUtilsCommon.debug.message("Actions: Unix " +
                                        "file permissions " +
                                        "error:" + ne.getMessage());
              }
              return false;
          }
          int quota = permissionNum/1000;
          int remain = permissionNum - 1000 * quota; 
          int tmp = 0; 
          if (quota == 0 || quota == 2 || quota == 4 || quota == 6) {
              for (int i = 0; i < 3; i++) {
                  tmp = remain / 10;
                  quota = remain - tmp * 10; 
                  if (quota < 0 || quota > 7)
                      return false; 
                  remain = tmp;
              } // end of for loop 
              return true; 
          } else { 
              return false;
          }
      }
      return true; 
    }
    
    /** 
     *Gets the action string
     *@return A String representing the action
     */
    public String getAction() {
        return  _action;
    }
    
    /** 
     *Gets the namespace of Action
     *@return A String representing the name space of the action
     */             
    public String getNameSpace() {
        return _namespace; 
    }
    
    /**
     *Creates a String representation of the <code>saml:Action</code> element
     *@return A string containing the valid XML for this element
     */
    public String toString() {
       return (this.toString(true, false)); 
   }
   
   /**  
    *Creates a String representation of the <code>saml:Action</code> element
    *@param     includeNS : Determines whether or not the namespace qualifier
    *                       is prepended to the Element when converted
    *@param     declareNS : Determines whether or not the namespace 
    *                       is declared within the Element.
    *@return A string containing the valid XML for this element
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
        
        result.append("<").append(prefix).append("Action ").
                append(uri).append(" Namespace=\"").append(_namespace).
                append("\">");  
        result.append(_action); 
        result.append("</").append(prefix).append("Action>\n");
        return ((String)result.toString());
    }                     
}

