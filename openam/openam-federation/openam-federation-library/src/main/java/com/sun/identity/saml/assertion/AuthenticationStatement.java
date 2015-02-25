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
 * $Id: AuthenticationStatement.java,v 1.3 2008/06/25 05:47:31 qcheng Exp $
 *
 */

/**
 * Portions Copyrighted 2014 ForgeRock AS
 */
package com.sun.identity.saml.assertion;

import java.text.*; 
import java.util.*;
import org.w3c.dom.*;
import com.sun.identity.shared.DateUtils;
import com.sun.identity.saml.common.SAMLUtilsCommon;
import com.sun.identity.saml.common.SAMLConstants;
import com.sun.identity.saml.common.SAMLException;
import com.sun.identity.saml.common.SAMLRequesterException;

/**
 * The <code>AuthenticationStatement</code> element supplies a 
 * statement by the issuer that its subject was authenticated by a
 * particular means at a particular time. The
 * <code>AuthenticationStatement</code> element is of type 
 * <code>AuthenticationStatementType</code>, which extends the
 * <code>SubjectStatementAbstractType</code> with the additional element and
 * attributes.
 * @supported.all.api
 */
public class AuthenticationStatement extends SubjectStatement {
    //The AuthenticationMethod attribute specifies the type of Authentication
    //that took place. 
    protected String  _authenticationMethod = null;
    
    //The AuthenticationInstant attribute specifies the time at which the 
    //authentication took place.
    protected Date  _authenticationInstant = null;
   
    //The SubjectLocality specifies the DNS domain name and IP address 
    //for the system entity from which the Subject was apparently authenticated.
    protected SubjectLocality  _subjectLocality = null ; 
    
    //The authority binding specifies the type of authority that performed 
    //the authentication. 
    protected List  _authorityBinding = null;  
    
    /**
     *Default constructor
     */
    protected AuthenticationStatement() {
    }
  
    /**
     * Constructs an authentication statement element from an
     * existing XML block.
     *
     * @param element representing a DOM tree element.
     * @exception SAMLException if there is an error in the sender or in the
     *            element definition.
     */
    public AuthenticationStatement(Element element) throws SAMLException {
        // make sure input is not null
        if (element == null) {
            SAMLUtilsCommon.debug.message(
                "AuthenticationStatement: null input.");
            throw new SAMLRequesterException(
                SAMLUtilsCommon.bundle.getString("nullInput"));
        }
        // check if it's an AuthenticationStatement
        boolean valid = SAMLUtilsCommon.checkStatement(element,
                            "AuthenticationStatement");
        if (!valid) {
            SAMLUtilsCommon.debug.message(
                "AuthenticationStatement: Wrong input.");
            throw new SAMLRequesterException(
                SAMLUtilsCommon.bundle.getString("wrongInput"));
        }

        int i = 0; 
        //handle the attributes of AuthenticationStatement 
        NamedNodeMap atts = ((Node)element).getAttributes(); 
        int attCount = atts.getLength(); 
        for (i = 0; i < attCount; i++) {
            Node att = atts.item(i);
            if (att.getNodeType() == Node.ATTRIBUTE_NODE) {
                String attName = att.getLocalName();
                if (attName == null || attName.length() == 0) {
                    if (SAMLUtilsCommon.debug.messageEnabled()) {
                        SAMLUtilsCommon.debug.message(
                            "AuthenticationStatement:" +
                            "Attribute name is either null or empty.");
                    }
                    continue;
                    //throw new SAMLRequesterException(
                      //  SAMLUtilsCommon.bundle.getString("nullInput"));
                }
                if (attName.equals("AuthenticationMethod")) {
                    _authenticationMethod = ((Attr)att).getValue().trim();
                } else if (attName.equals("AuthenticationInstant")) {
                    try {
                        _authenticationInstant =
                            DateUtils.stringToDate(((Attr)att).getValue());   
                    } catch (ParseException pe ) {
                        SAMLUtilsCommon.debug.error(
                            "AuthenticationStatement:StringToDate", pe);
                        throw new SAMLRequesterException(
                            SAMLUtilsCommon.bundle.getString(
                            "wrongDateFormat"));
                    } // end of try...catch
                }
            }
        } // end of for loop 
        //Handle the children elements of AuthenticationStatement  
        NodeList nodes = element.getChildNodes();
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
                                "AuthenticationStatement: The" +
                                " tag name or tag namespace of child" +
                                " element is either null or empty.");
                        }
                        throw new SAMLRequesterException(
                            SAMLUtilsCommon.bundle.getString("nullInput"));
                    }
                    if (tagName.equals("Subject") &&
                        tagNS.equals(SAMLConstants.assertionSAMLNameSpaceURI)) {
                        if (this._subject != null) {
                            if (SAMLUtilsCommon.debug.messageEnabled()) {
                                SAMLUtilsCommon.debug.message("Authentication" +
                                   "Statement:should only contain one subject");
                            }
                            throw new SAMLRequesterException(
                                SAMLUtilsCommon.bundle.getString("oneElement"));
                        } else { 
                            this._subject =
                                createSubject((Element)currentNode); 
                        }
                    } else if (tagName.equals("SubjectLocality") &&
                        tagNS.equals(SAMLConstants.assertionSAMLNameSpaceURI)) {
                        if (_subjectLocality != null) {
                            if (SAMLUtilsCommon.debug.messageEnabled()) {
                                SAMLUtilsCommon.debug.message("Authentication"+
                                            "Statement: should at most " +
                                            "contain one SubjectLocality.");
                             }
                             throw new SAMLRequesterException(
                                 SAMLUtilsCommon.bundle.getString(
                                     "oneElement"));        
                        } else {
                             _subjectLocality = 
                                    createSubjectLocality((Element)currentNode);
                        }
                    } else if (tagName.equals("AuthorityBinding") &&
                        tagNS.equals(SAMLConstants.assertionSAMLNameSpaceURI)) {
                         if (_authorityBinding == null) {
                             _authorityBinding = new ArrayList(); 
                         }
                         if ((_authorityBinding.add(createAuthorityBinding(
                             (Element)currentNode))) == false) {
                             if (SAMLUtilsCommon.debug.messageEnabled()) {
                                 SAMLUtilsCommon.debug.message(
                                     "Authentication Statment: failed to" +
                                     " add to the AuthorityBinding list.");
                             }
                             throw new SAMLRequesterException(
                                 SAMLUtilsCommon.bundle.getString(
                                     "addListError"));   
                         }
                    } else {
                         if (SAMLUtilsCommon.debug.messageEnabled()) {
                             SAMLUtilsCommon.debug.message(
                                 "AuthenticationStatement:"+
                                 "Wrong element " + tagName + "included."); 
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
                SAMLUtilsCommon.debug.message(
                    "AuthenticationStatement should " +
                    "contain one subject.");
            }
            throw new SAMLRequesterException(
                        SAMLUtilsCommon.bundle.getString("missingElement"));
        }
    }
    
    /**
     * Constructs <code>Authentication statement</code>
     *
     * @param authMethod (optional) A String specifies the type of
     *        authentication that took place. 
     * @param authInstant (optional) A String specifies the time at which
     *        the authentication took place.
     * @param subject (required) A Subject object 
     * @exception SAMLException if there is an error in the sender.
     */   
    public AuthenticationStatement(String authMethod, Date authInstant,
        Subject subject) throws SAMLException {
        _authenticationMethod = authMethod;    
        _authenticationInstant = authInstant; 
        
        // check if the subject is null 
        if (subject == null) {
            if (SAMLUtilsCommon.debug.messageEnabled()) {
                SAMLUtilsCommon.debug.message(
                    "AuthenticationStatement:missing the subject.");
            }
            throw new SAMLRequesterException(
                SAMLUtilsCommon.bundle.getString("missingElement"));
        } else 
            this._subject = subject; 
    }
          
    /**
     * Constructs <code>AuthenticationStatement</code>
     *
     * @param authMethod (optional) A String specifies the type of
     *        authentication that took place. 
     * @param authInstant (optional) A String specifies the time at which the 
     *        authentication that took place.
     * @param subject (required) A Subject object 
     * @param subjectLocality (optional) A <code>SubjectLocality</code> object.
     * @param authorityBinding (optional) A List of
     *        <code>AuthorityBinding</code> objects.
     * @exception SAMLException if there is an error in the sender.
     */   
    public AuthenticationStatement(String authMethod, Date authInstant, 
        Subject subject, SubjectLocality subjectLocality,
        List authorityBinding) throws SAMLException {
        _authenticationMethod = authMethod;    
        _authenticationInstant = authInstant; 
        // check if the subject is null 
        if (subject == null) {
            if (SAMLUtilsCommon.debug.messageEnabled()) {
                SAMLUtilsCommon.debug.message(
                    "AuthenticationStatement: should" +
                    " contain one subject.");
            }
            throw new SAMLRequesterException(
                SAMLUtilsCommon.bundle.getString("oneElement"));
        } else {
            this._subject = subject; 
        }
        _subjectLocality = subjectLocality;  

        if (authorityBinding != null && !authorityBinding.isEmpty()) {
            if (_authorityBinding == null) {
                _authorityBinding = new ArrayList(); 
            }
            _authorityBinding = authorityBinding; 
        }
    }      
  
    /**
     * Returns the <code>SubjectLocality</code> from
     * <code>AuthenticationStatement</code>
     *
     * @return The <code>SubjectLocality</code> object within the authentication
     *         statement.
     */
    public SubjectLocality getSubjectLocality() {
        return _subjectLocality ; 
    }
    
    /**
     * Sets the <code>SubjectLocality</code> for
     * <code>AuthenticationStatement</code>.
     *
     * @param subjectlocality The <code>SubjectLocality</code> object within
     *        the <code>AuthenticationStatement</code>.
     * @return true if the operation is successful.
     */
    public boolean setSubjectLocality(SubjectLocality subjectlocality) {
        if (subjectlocality == null) {
            if (SAMLUtilsCommon.debug.messageEnabled()) {
                SAMLUtilsCommon.debug.message("AuthenticationStatement: " +
                                        "setSubjectLocality:Input is null.");
            }
            return false; 
        }
        _subjectLocality = subjectlocality ; 
        return true; 
    }
    
    /**
     * Returns <code>AuthenticationMethod</code> from authentication statement
     * @return A String representing the authentication method of the
     *         authentication statement.
     */
    public String getAuthenticaionMethod() {
        return _authenticationMethod; 
    }
    
    /**
     * Sets <code>AuthenticationMethod</code> for
     * <code>AuthenticationStatement</code>.
     *
     * @param authenticationmethod input authentication method 
     * @return true if the operation is successful. Otherwise return false.
     */
    public boolean setAuthenticaionMethod(String authenticationmethod) {
        if (authenticationmethod == null || 
            authenticationmethod.length() == 0) {
            if (SAMLUtilsCommon.debug.messageEnabled()) {
                SAMLUtilsCommon.debug.message("AuthenticationStatement: " +
                                      "setAuthenticationMethod:Input is null.");
            }
            return false; 
        }
        _authenticationMethod = authenticationmethod; 
        return true;  
    }
    
    /**
     * Returns <code>AuthenticationInstant</code> from authentication statement.
     * @return The date/time when the authentication statement is created.
     */
    public Date getAuthenticationInstant() {
        return _authenticationInstant;
    }
    
    /**
     * Sets <code>AuthenticationInstant</code> for
     * <code>AuthenticationStatement</code>.
     *
     * @param authenticationinstant The date/time when the authentication
     *        statement is created.
     * @return true if the operation is successful.
     */
    public boolean setAuthenticationInstant(Date authenticationinstant) {
        if (authenticationinstant == null) {
            if (SAMLUtilsCommon.debug.messageEnabled()) {
                SAMLUtilsCommon.debug.message("AuthenticationStatement: " +
                                     "setAuthenticationInstant:Input is null.");
            }
            return false; 
        }
        _authenticationInstant = authenticationinstant;
        return true;
    }
    
    /**
     * Returns the <code>AuthorityBinding</code> from
     * <code>AuthenticationStatement</code>.
     *
     * @return A list of the <code>AuthorityBinding</code> objects
     */
    public List getAuthorityBinding() { 
        return  _authorityBinding; 
    } 
    
    /**
     * Sets the <code>AuthorityBinding</code> for
     * <code>AuthenticationStatement</code>.
     *
     * @param authoritybinding A list of the <code>AuthorityBinding</code>
     *        objects.
     * @return true if the operation is successful.
     */
    public boolean setAuthorityBinding(List authoritybinding) { 
        if (authoritybinding == null || authoritybinding.isEmpty()) {
            if (SAMLUtilsCommon.debug.messageEnabled()) {
                SAMLUtilsCommon.debug.message("AuthenticationStatement: " +
                                        "setAuthorityBinding:Input is null.");
            }
            return false; 
        }
        _authorityBinding = authoritybinding; 
        return true;   
    }
    
    /**
     *Gets the real type of the Statement. 
     *This method returns Statement.AUTHENTICATION_STATEMENT. 
     *@return an integer which is Statement.AUTHENTICATION_STATEMENT. 
     */
    public int getStatementType() {
        return Statement.AUTHENTICATION_STATEMENT; 
    }
    
    /** 
     * Returns a String representation of the Authentication Statement.
     * 
     * @return A String representation of the
     *         <code>&lt;saml:AuthenticationStatement&gt;</code>
     *         element.
     */
    public String toString()  {
        return (toString(true, false)); 
    }
   
    /** 
     * Returns a String representation of the
     * <code>AuthenticationStatement</code>
     * @param includeNS Determines whether or not the namespace qualifier is 
     *        prepended  to the Element when converted
     * @param declareNS Determines whether or not the namespace is declared 
     *        within the Element.
     * @return A String representation of the 
     *         <code>&lt;saml:AuthenticationStatement&gt;</code> element.
     */                             
    public  String  toString(boolean includeNS, boolean declareNS)  {
        StringBuffer result = new StringBuffer(1000);
        String prefix = "";
        String uri = "";
        if (includeNS) {
            prefix = SAMLConstants.ASSERTION_PREFIX;
        }
        if (declareNS) {
            uri = SAMLConstants.assertionDeclareStr;
        }
        result.append("<").append(prefix).append("AuthenticationStatement").
               append(uri); 

        if (_authenticationMethod != null &&
            _authenticationMethod.length() != 0){
            result.append(" AuthenticationMethod=\"").
                   append(_authenticationMethod.replace("|", "%7C")).append("\"");
        }

        if (_authenticationInstant != null &&
            _authenticationInstant.toString().length() != 0
        ) {
            result.append(" AuthenticationInstant=\"")
                .append(DateUtils.toUTCDateFormat(_authenticationInstant))
                .append("\""); 
        }

        result.append(">\n").append(this._subject.toString(includeNS, false));
        if (_subjectLocality != null) {
            result.append(_subjectLocality.toString(includeNS, false));
        }
        if (_authorityBinding != null && !_authorityBinding.isEmpty()) {
                Iterator iter = this.getAuthorityBinding().iterator(); 
                while (iter.hasNext()) {
                    AuthorityBinding authBinding= 
                        (AuthorityBinding)iter.next(); 
                    result.append(authBinding.toString(includeNS, false)); 
                }
        }
        result.append("</").append(prefix).append("AuthenticationStatement>\n");
        return(result.toString());
    }

    protected Subject createSubject(Element subjectElement)
        throws SAMLException {
        return new Subject(subjectElement);
    }
    
    protected SubjectLocality createSubjectLocality(
        Element subjectLocalityElement)
        throws SAMLException {
        return new SubjectLocality(subjectLocalityElement);
    }
   
    protected AuthorityBinding createAuthorityBinding(
        Element authorityBindingElement) throws SAMLException {
        return new AuthorityBinding(authorityBindingElement);
    }
}
