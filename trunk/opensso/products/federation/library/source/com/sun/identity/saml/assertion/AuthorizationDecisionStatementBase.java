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
 * $Id: AuthorizationDecisionStatementBase.java,v 1.2 2008/06/25 05:47:32 qcheng Exp $
 *
 */


package com.sun.identity.saml.assertion;

import java.text.*; 
import java.util.*;
import org.w3c.dom.*;
import com.sun.identity.saml.common.SAMLUtilsCommon;
import com.sun.identity.saml.common.SAMLConstants;
import com.sun.identity.saml.common.SAMLException;
import com.sun.identity.saml.common.SAMLRequesterException;

/**
 * The <code>AuthorizationDecisionStatement</code> element supplies a statement 
 * by the issuer that the request for access by the specified subject to the 
 * specified resource has resulted in the specified decision on the basis of 
 * some optionally specified evidence. 
 *
 * This class is an abstract base class for all AuthorizationDecisionStatement
 * implementations and encapsulates common functionality.
 * @supported.all.api
 */
public abstract class AuthorizationDecisionStatementBase
    extends SubjectStatement {

/**
 *The <code>DecisionType</code> is an inner class defining constants for the 
 *type of Decisions than can be conveyed by an 
 *<code>AuthorizationDecisionStatement </code>.
 *<br>1 implies <code>PERMIT</code> 
 *<br>2 implies <code>DENY</code> 
 *<br>3 implies <code>INDETERMINATE </code> 
 *<br>4 implies <code>NOT_SUPPORTED</code>
 */

    public static class DecisionType {
        /**
         * The specified action is permitted.
         */
        public static final int  PERMIT         = 1;  

        /**
         * The specified action is denied.
         */
        public static final int  DENY           = 2;      

        /**
         * The issuer cannot determine whether the specified action is
         * permitted or denied.
         */
        public static final int  INDETERMINATE  = 3;   

        /**
         * The specified action is not supported.
         */
        public static final int  NOT_SUPPORTED  = -1;  
    }
    
    private String _resource = null;
    private int _decision = DecisionType.NOT_SUPPORTED;
    private List _action = null; 
    protected EvidenceBase _evidence = null; 
      
    /**
     *Default constructor 
     */
    protected AuthorizationDecisionStatementBase() {
    }
    
    /**
     * Constructs an <code>AuthorizationStatement</code> element from an 
     * existing XML block.
     *
     * @param element representing a DOM tree element 
     * @exception SAMLException if there is an error in the sender or in
     *            the element definition.
     */
    public AuthorizationDecisionStatementBase(Element element) 
                                          throws SAMLException {
        // make sure input is not null
        if (element == null) {
            if (SAMLUtilsCommon.debug.messageEnabled()) {
                SAMLUtilsCommon.debug.message(
                   "AuthorizationDecisionStatement: null input.");
            }
            throw new SAMLRequesterException(
                      SAMLUtilsCommon.bundle.getString("nullInput"));
        }

        // check if it's an AuthorizationDecisionStatement
        boolean valid = SAMLUtilsCommon.checkStatement(element,
                                             "AuthorizationDecisionStatement");
        if (!valid) {
            if (SAMLUtilsCommon.debug.messageEnabled()) {
                SAMLUtilsCommon.debug.message(
                    "AuthorizationDecisionStatement: Wrong input.");
            }
            throw new SAMLRequesterException(
                      SAMLUtilsCommon.bundle.getString("wrongInput"));
        }
                                       
        int i = 0; 
        //handle the attributes of AuthorizationDecisionStatement 
        NamedNodeMap atts = ((Node)element).getAttributes(); 
        int attCount = atts.getLength(); 
        for (i = 0; i < attCount; i++) {
            Node att = atts.item(i);
            if (att.getNodeType() == Node.ATTRIBUTE_NODE) {
                String attName = att.getLocalName();
                if (attName == null || attName.length() == 0) {
                    if (SAMLUtilsCommon.debug.messageEnabled()) {
                        SAMLUtilsCommon.debug.message("AuthorizationDecision" +
                          "Statement: Attribute name is either null or empty.");
                    }
                    throw new SAMLRequesterException(
                        SAMLUtilsCommon.bundle.getString("nullInput"));
                }
                if (attName.equals("Resource")) {
                    _resource = ((Attr)att).getValue().trim();
                    if (_resource == null || _resource.length() == 0) {
                        if (SAMLUtilsCommon.debug.messageEnabled()) {
                            SAMLUtilsCommon.debug.message(
                                "AuthorizationDecision" +
                                "Statement: Resource is null or empty.");
                        }
                        throw new SAMLRequesterException(
                            SAMLUtilsCommon.bundle.getString(
                            "missingAttributeValue"));
                    }
                } else if (attName.equals("Decision")) {
                    String decisionStr = ((Attr)att).getValue().trim();
                    if (decisionStr == null || decisionStr.length() == 0) {
                        if (SAMLUtilsCommon.debug.messageEnabled()) {
                            SAMLUtilsCommon.debug.message(
                                "AuthorizationDecision" +
                                "Statement: Decision is null or empty.");
                        }
                        throw new SAMLRequesterException(
                           SAMLUtilsCommon.bundle.getString(
                           "missingAttributeValue"));
                    }
                    if (decisionStr.equals("Permit")) 
                        _decision = DecisionType.PERMIT; 
                    else if (decisionStr.equals("Deny"))
                        _decision = DecisionType.DENY; 
                    else if (decisionStr.equals("Indeterminate"))
                        _decision = DecisionType.INDETERMINATE; 
                    else {
                        if (SAMLUtilsCommon.debug.messageEnabled()) {
                            SAMLUtilsCommon.debug.message(
                                "AuthorizationDecision" +
                                "Statement: The type of decision is illegal!");
                        }
                        throw new SAMLRequesterException(
                            SAMLUtilsCommon.bundle.getString("wrongInput"));  
                    }
                }
            }
        } // end of for loop 

        // Resource is required 
        if (_resource == null || _resource.length() == 0) {
            if (SAMLUtilsCommon.debug.messageEnabled()) {
                SAMLUtilsCommon.debug.message(
                    "AuthorizationDecisionStatement: "+
                    "Resource is required attribute.");
            }
            throw new SAMLRequesterException(
                        SAMLUtilsCommon.bundle.getString("missingAttribute"));
        }    
        // Decision is required 
        if (_decision == DecisionType.NOT_SUPPORTED) {
            if (SAMLUtilsCommon.debug.messageEnabled()) {
                SAMLUtilsCommon.debug.message(
                    "AuthorizationDecisionStatement: "+
                    "Decision is required attribute.");
            }
            throw new SAMLRequesterException(
                        SAMLUtilsCommon.bundle.getString("missingAttribute"));
        }
        //Handle the children elements of AuthorizationDecisionStatement  
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
                            SAMLUtilsCommon.debug.message(
                                "AuthorizationDecision" +
                                "Statement: The tag name or tag namespace" +
                                " of child element is either null or empty.");
                        }
                        throw new SAMLRequesterException(
                            SAMLUtilsCommon.bundle.getString("nullInput"));
                    }
                    if (tagName.equals("Subject") &&
                        tagNS.equals(SAMLConstants.assertionSAMLNameSpaceURI)) {
                        if (this._subject != null) {
                            if (SAMLUtilsCommon.debug.messageEnabled()) {
                                SAMLUtilsCommon.debug.message(
                                    "AuthorizationDecisionStatement: "+
                                    " should only contain one subject"); 
                            }
                            throw new SAMLRequesterException(
                                SAMLUtilsCommon.bundle.getString("oneElement"));
                            
                        } else 
                            this._subject = 
                                createSubject((Element)currentNode); 
                    } else if (tagName.equals("Action") &&
                        tagNS.equals(SAMLConstants.assertionSAMLNameSpaceURI)) {
                        if (_action == null) {
                            _action = new ArrayList(); 
                        }
                        if (!(_action.add(createAction((Element)
                            currentNode)))) {       
                            if (SAMLUtilsCommon.debug.messageEnabled()) {
                                SAMLUtilsCommon.debug.message("Authentication" 
                                            + "Statment: failed to add to the"
                                            + " AuthorityBinding list.");
                            }
                            throw new SAMLRequesterException(
                                SAMLUtilsCommon.bundle.getString(
                                "addListError"));   
                        }
                    } else if (tagName.equals("Evidence") &&
                        tagNS.equals(SAMLConstants.assertionSAMLNameSpaceURI)) {
                        _evidence = createEvidence((Element)currentNode);
                    } else {
                         if (SAMLUtilsCommon.debug.messageEnabled()) {
                             SAMLUtilsCommon.debug.message(
                                 "AuthorizationDecisionStatement: "+
                                 "wrong element:" + tagName); 
                         }
                         throw new SAMLRequesterException(
                             SAMLUtilsCommon.bundle.getString("wrongInput"));
                    }
                } // end of if (currentNode.getNodeType() == Node.ELEMENT_NODE) 
            } // end of for loop 
        }  // end of if (nodeCount > 0)
        
        // check if the subject is null 
        if (this._subject == null) {
            if (SAMLUtilsCommon.debug.messageEnabled()) {
                SAMLUtilsCommon.debug.message("AuthorizationDecisionStatement:"
                                 + " should contain exactly one subject.");
            }
            throw new SAMLRequesterException(
                        SAMLUtilsCommon.bundle.getString("missingElement"));
        }
        //check if the Action is null
        if (_action == null || _action.isEmpty()) {
            if (SAMLUtilsCommon.debug.messageEnabled()) {
                SAMLUtilsCommon.debug.message("AuthorizationDecisionStatement:"
                                 + " should at least contain one Action.");
            }
            throw new SAMLRequesterException(
                      SAMLUtilsCommon.bundle.getString("missingElement"));
        }
    }
      
    /**
     * Constructs an instance of <code>AuthorizationDecisionStatement</code>.
     *
     * @param subject (required) A Subject object
     * @param resource (required) A String identifying the resource to which 
     *        access authorization is sought.
     * @param decision (required) The decision rendered by the issuer with
     *        respect to the specified resource. The value is of the 
     *        <code>DecisionType</code> simple type. 
     * @param action (required) A List of Action objects specifying the set of 
     *        actions authorized to be performed on the specified resource.
     * @param evidence (optional) An Evidence object representing a set of 
     *        assertions that the issuer replied on in making decisions.  
     * @exception SAMLException if there is an error in the sender.
     */     
    public AuthorizationDecisionStatementBase(Subject subject, String resource, 
        int decision, List action, EvidenceBase evidence)
        throws SAMLException {
        // check if the subject is null 
        if (subject == null) {
            if (SAMLUtilsCommon.debug.messageEnabled()) {
                SAMLUtilsCommon.debug.message(
                    "AuthorizationDecisionStatement:" +
                    "missing the subject.");
            }
            throw new SAMLRequesterException(
                      SAMLUtilsCommon.bundle.getString("missingElement"));
        } else {
            this._subject = subject;      
        }
                                
        // Resource is required
        if (resource == null || resource.length() == 0) {
            if (SAMLUtilsCommon.debug.messageEnabled()) {
                SAMLUtilsCommon.debug.message(
                    "AuthorizationDecisionStatement: "+
                    "resource is required attribute");
            }
            throw new SAMLRequesterException(
                        SAMLUtilsCommon.bundle.getString("missingAttribute"));
        } else {
            _resource = resource;    
        }
            
        // check if the decision is null 
        if (decision < DecisionType.PERMIT  ||
           (decision > DecisionType.INDETERMINATE)) {
            if (SAMLUtilsCommon.debug.messageEnabled()) {
                SAMLUtilsCommon.debug.message(
                    "AuthorizationDecisionStatement:" +
                    "The type of decision is illegal.");
            }
            throw new SAMLRequesterException(
                      SAMLUtilsCommon.bundle.getString("wrongInput"));
        } else {
            _decision = decision;     
        }
        
        // Actions is required 
        if (action == null || action.isEmpty()) {
            if (SAMLUtilsCommon.debug.messageEnabled()) {
                SAMLUtilsCommon.debug.message(
                    "AuthorizationDecisionStatement: "+
                    "Action is required.");
            }
            throw new SAMLRequesterException(
                        SAMLUtilsCommon.bundle.getString("missingElement"));
        } else {
            if (_action == null) {
                _action = new ArrayList(); 
            }
            _action = action; 
        }
        
        _evidence = evidence;                       
    }
    
    /**
     * Constructs a <code>AuthorizationDecisionStatement</code> instance.
     *
     *@param subject (required) A Subject object
     *@param resource (required) A String identifying the resource to which 
     *       access authorization is sought. 
     *@param decision (required) The decision rendered by the issuer with
     *       respect to the specified resource. The value is of the 
     *       <code>DecisionType</code> simple type. 
     *@param action (required) A List of Action objects specifying the set of
     *       actions authorized to be performed on the  
     *       specified resource.
     *@exception SAMLException if there is an error in the sender.
     */   
    public AuthorizationDecisionStatementBase(Subject subject,
        String resource, int decision, List action) throws SAMLException {
        // check if the subject is null 
        if (subject == null) {
            if (SAMLUtilsCommon.debug.messageEnabled()) {
                SAMLUtilsCommon.debug.message(
                    "AuthorizationDecisionStatement:" +
                    "missing the subject.");
            }
            throw new SAMLRequesterException(
                      SAMLUtilsCommon.bundle.getString("missingElement"));
        } else {
            this._subject = subject;    
        }
        // Resource is required
        if (resource == null || resource.length() == 0) {
            if (SAMLUtilsCommon.debug.messageEnabled()) {
                SAMLUtilsCommon.debug.message(
                    "AuthorizationDecisionStatement: "+
                    "resource is required attribute");
            }
            throw new SAMLRequesterException(
                        SAMLUtilsCommon.bundle.getString("missingAttribute"));
        } else {
            _resource = resource;    
        }
        
        // check if the decision is null 
        if (decision <= DecisionType.PERMIT  ||
           (decision >= DecisionType.INDETERMINATE)) {
            if (SAMLUtilsCommon.debug.messageEnabled()) {
                SAMLUtilsCommon.debug.message(
                    "AuthorizationDecisionStatement:" +
                    "The type of decision is illegal.");
            }
            throw new SAMLRequesterException(
                SAMLUtilsCommon.bundle.getString("wrongInput"));
        } else { 
            _decision = decision;       
        }
        // Action is required 
        if (action == null || action.isEmpty()) {
            if (SAMLUtilsCommon.debug.messageEnabled()) {
                SAMLUtilsCommon.debug.message(
                    "AuthorizationDecisionStatement: "+
                    "Action is required.");
            }
            throw new SAMLRequesterException(
                        SAMLUtilsCommon.bundle.getString("missingElement"));
        } else {
            if (_action == null) {
                _action = new ArrayList(); 
            }
            _action = action; 
        }                   
    }            

    /**
     * Creates appropriate Evidence Instance
     * @param evidenceElement the Evidence Element
     * @return the Evidence Object
     */
    protected abstract EvidenceBase createEvidence(Element evidenceElement)
        throws SAMLException;
    
    /**
     * Creates appropriate Subject Instance
     * @param subjectElement the Subject Element
     * @return the Subject Object
     */
    protected abstract Subject createSubject(Element subjectElement)
        throws SAMLException;
    
    /**
     * Creates appropriate Action Instance
     * @param actionElement the Action Element
     * @return the Action Object
     */
    protected abstract Action createAction(Element actionElement)
        throws SAMLException;
                  
    /**
     * Returns the action(s) from <code>AuthorizationStatement</code>.
     *
     * @return A List of Action object(s) specifying the set of actions 
     *         authorized to be performed on the specified resource.
     */
    public List getAction() {
        return _action; 
    }
    
    /**
     * Returns the resource from <code>AuthorizationStatement</code>
     *
     * @return A String identifying the resource to which access authorization
     *         is sought.
     */
    public String getResource() {
        return _resource; 
    }
   
    /**
     * Returns the decision for <code>AuthorizationStatement</code>.
     *
     * @return The decision string such as
     * <code>permit</code>
     * <code>deny</code>
     * <code>indetereminate</code>
     */
     public int getDecision() {
         return _decision; 
     }
  
    /**
     * Returns the real type of statement. This method returns
     * <code>Statement.AUTHORIZATION_DECISION_STATEMENT</code>.
     *
     * @return <code>Statement.AUTHORIZATION_DECISION_STATEMENT</code>.
     */
    public int getStatementType() {
        return Statement.AUTHORIZATION_DECISION_STATEMENT; 
    }
    
   /**
    * Returns a String representation of the
    * <code>AuthorizationStatement</code>.
    *
    * @return A String representation of the 
    *         <code>&lt;AuthorizationDecisionStatement</code> element.
    */
    public String toString() {
        return toString(true, false); 
    }
    
    /** 
     * Returns a String representation of the
     * <code>AuthorizationStatement</code>.
     *
     * @param includeNS Determines whether or not the namespace qualifier is 
     *        prepended to the Element when converted
     * @param declareNS Determines whether or not the namespace is declared
     *        within the Element.
     * @return A String representation of the 
     *         <code>&lt;AuthorizationDecisionStatement&gt;</code> element.
     */                             
    public String toString(boolean includeNS, boolean declareNS) {
        StringBuffer result = new StringBuffer(3000);
        String prefix = "";
        String uri = "";
        if (includeNS) {
            prefix = SAMLConstants.ASSERTION_PREFIX;
        }
        if (declareNS) {
            uri = SAMLConstants.assertionDeclareStr;
        }
        result.append("<").append(prefix).
               append("AuthorizationDecisionStatement").
               append(uri).append(" Resource=\"").append(_resource).
               append("\" Decision=\"").append(decisionTypeConvert(_decision)).
               append("\">\n");         
        result.append(this._subject.toString(includeNS, false));
        Iterator iter = _action.iterator(); 
        while (iter.hasNext()) {
            Action act = (Action)iter.next(); 
            result.append(act.toString(includeNS, false)); 
        }
     
        if (_evidence != null) {
            result.append(_evidence.toString(includeNS, false));
        }
        result.append("</").append(prefix).
               append("AuthorizationDecisionStatement>\n");
        return(result.toString());
    }
    
    /**
     *Converts the number to the mapping string
     *@param number - an int
     *@return a mapping string 
     */
    private String decisionTypeConvert(int number){
        String result = ""; 
        switch(number) {
            case DecisionType.PERMIT : result = "Permit"; break; 
            case DecisionType.DENY: result = "Deny"; break ; 
            case DecisionType.INDETERMINATE: result = "Indeterminate"; break ; 
        }
        return result; 
    }   
}

