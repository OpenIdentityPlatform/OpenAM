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
 * $Id: FSAuthenticationStatement.java,v 1.2 2008/06/25 05:46:43 qcheng Exp $
 * Portions Copyrighted 2014 ForgeRock AS
 */

package com.sun.identity.federation.message;

import com.sun.identity.federation.common.IFSConstants;
import com.sun.identity.federation.common.FSUtils;
import com.sun.identity.federation.message.common.AuthnContext;
import com.sun.identity.federation.message.common.FSMsgException;
import com.sun.identity.saml.assertion.AuthenticationStatement;
import com.sun.identity.saml.assertion.AuthorityBinding;
import com.sun.identity.saml.assertion.Subject;
import com.sun.identity.saml.assertion.SubjectLocality;
import com.sun.identity.saml.common.SAMLConstants;
import com.sun.identity.saml.common.SAMLException;
import com.sun.identity.shared.DateUtils;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * The <code>FSAuthenticationStatement</code> element represents an
 * authentication statement by the issuer that it's subject was authenticated
 * by a  particular means at a particular time.
 *
 * @supported.all.api
 * @deprecated since 12.0.0
 */
@Deprecated

public class FSAuthenticationStatement extends AuthenticationStatement {
    
    protected Date reauthenticateOnOrAfter;
    protected String sessionIndex = null;
    protected AuthnContext _authnContext;
    protected int minorVersion = IFSConstants.FF_11_PROTOCOL_MINOR_VERSION;
    
    /**
     * Default Constructor.
     */
    public FSAuthenticationStatement(){
    }
    
    /**
     * Constructor to create <code>FSAuthenticationStatement</code> object.
     *
     * @param authMethod the Authentication method in the statement.
     * @param authInstant the authentication date in the statement.
     * @param subject the Subject in the statement.
     * @param authnContext the Authentication Context.
     * @throws FSMsgException if there is error
     *         creating the object.
     * @throws SAMLException if the version is incorrect.
     */
    public FSAuthenticationStatement(
            String authMethod,
            Date authInstant,
            Subject subject,
            AuthnContext authnContext
            ) throws FSMsgException, SAMLException {
        super(authMethod, authInstant, subject);
        if (authnContext == null) {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("FSAuthenticationStatement: missing" +
                        "AuthnContext");
            }
        } else {
            this._authnContext = authnContext;
        }
    }
    
    /**
     * Constructor for create <code>FSAuthenticationStatement</code> object.
     *
     * @param authMethod the Authentication method in the statement.
     * @param authInstant the authentication date in the statement.
     * @param subject the <code>Subject</code> in the statement.
     * @param subjectLocality the <code>SubjectLocality</code> in the statement.
     * @param authorityBinding a List of <code>AuthorityBinding</code> objects.
     * @param authnContext the Authentication Context.
     * @throws FSMsgException if there is an error
     *         creating the object.
     * @throws SAMLException on error.
     */
    public FSAuthenticationStatement(
            String authMethod,
            Date authInstant,
            Subject subject,
            SubjectLocality subjectLocality,
            List authorityBinding,
            AuthnContext authnContext
            ) throws FSMsgException, SAMLException {
        super(authMethod,
                authInstant,
                subject,
                subjectLocality,
                authorityBinding);
        
        // check if the AuthnContext is null
        if (authnContext == null) {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("FSAuthenticationStatement: missing" +
                        "AuthnContext.");
            }
        } else {
            this._authnContext = authnContext;
        }
    }
    
    /**
     * Constructs an <code>FSAuthenticationStatement</code> object from a
     * Document Element.
     *
     * @param element the Document Element object.
     * @throws FSMsgException if document element is null
     *         or required attributes cannot be retrieved from the element.
     * @throws SAMLException on error.
     */
    public FSAuthenticationStatement(Element element)
    throws FSMsgException, SAMLException {
        FSUtils.debug.message("FSAuthenticationStatement(Element):  Called");
        if (element == null) {
            FSUtils.debug.message("FSAuthenticationStatement: null input.");
            throw new FSMsgException("nullInput",null);
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
                    if (FSUtils.debug.messageEnabled()) {
                        FSUtils.debug.message("FSAuthenticationStatement:" +
                                "Attribute name is either null or empty.");
                    }
                    throw new FSMsgException("nullInput", null);
                }
                if (attName.equals(IFSConstants.AUTHENTICATION_METHOD)) {
                    _authenticationMethod = ((Attr)att).getValue().trim();
                } else if (attName.equals(IFSConstants.AUTHENTICATION_INSTANT)){
                    try {
                        _authenticationInstant =
                                DateUtils.stringToDate(((Attr)att).getValue());
                    } catch (ParseException pe ) {
                        FSUtils.debug.error("FSAuthenticationStatement:" +
                                "StringToDate: ", pe);
                        throw new FSMsgException("wrongDateFormat",null);
                    } // end of try...catch
                } else if (attName.equals(IFSConstants.REAUTH_ON_OR_AFTER)) {
                    try {
                        reauthenticateOnOrAfter =
                                DateUtils.stringToDate(((Attr)att).getValue());
                    } catch (ParseException pe ) {
                        FSUtils.debug.error("FSAuthenticationStatement:" +
                                "StringToDate: ", pe);
                        throw new FSMsgException("wrongDateFormat",null);
                    }
                } else if (attName.equals(IFSConstants.SESSION_INDEX)) {
                    sessionIndex =
                            ((Attr)att).getValue().trim();
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
                        if (FSUtils.debug.messageEnabled()) {
                            FSUtils.debug.message("FSAuthenticationStatement: "+
                                    "The  tag name or tag namespace of child" +
                                    " element is either null or empty.");
                        }
                        throw new FSMsgException("nullInput",null);
                    }
                    if (tagName.equals(IFSConstants.AUTH_SUBJECT)) {
                        if (this._subject != null) {
                            if (FSUtils.debug.messageEnabled()) {
                                FSUtils.debug.message(
                                        "FSAuthenticationStatement" +
                                        ":should only contain one subject");
                            }
                            throw new FSMsgException("oneElement",null);
                        } else {
                            this._subject =
                                    new FSSubject((Element) currentNode);
                        }
                    } else if (tagName.equals(IFSConstants.SUBJECT_LOCALITY)) {
                        if (_subjectLocality != null) {
                            if (FSUtils.debug.messageEnabled()) {
                                FSUtils.debug.message(
                                        "FSAuthenticationStatement"+
                                        "Statement: should at most " +
                                        "contain one SubjectLocality.");
                            }
                            throw new FSMsgException("oneElement",null);
                        } else {
                            _subjectLocality =
                                    new SubjectLocality((Element)currentNode);
                        }
                    } else if (tagName.equals(IFSConstants.AUTHN_CONTEXT) &&
                            (tagNS.equals(
                                    IFSConstants.libertyMessageNamespaceURI)||
                            tagNS.equals(IFSConstants.FF_12_XML_NS))) {
                        
                        if (_authnContext != null) {
                            if (FSUtils.debug.messageEnabled()) {
                                FSUtils.debug.message("FSAuthentication"+
                                        "Statement: should not contain more " +
                                        "than  one AuthnContext element.");
                            }
                            throw new FSMsgException("oneElement",null);
                        } else {
                            _authnContext =
                                    new AuthnContext((Element)currentNode);
                        }
                    } else if (tagName.equals(IFSConstants.AUTHORITY_BINDING)) {
                        if (_authorityBinding == null) {
                            _authorityBinding = new ArrayList();
                        }
                        if ((_authorityBinding.add(new AuthorityBinding(
                                (Element)currentNode))) == false) {
                            if (FSUtils.debug.messageEnabled()) {
                                FSUtils.debug.message(
                                        "FSAuthenticationStatement"+
                                        ": failed to add to the" +
                                        " AuthorityBinding list.");
                            }
                            throw new FSMsgException("addListError",null);
                        }
                    } else {
                        if (FSUtils.debug.messageEnabled()) {
                            FSUtils.debug.message("FSAuthenticationStatement:"+
                                    "Wrong element "
                                    + tagName + "included.");
                        }
                        throw new FSMsgException("wrongInput",null);
                    }
                } // end of if (currentNode.getNodeType() == Node.ELEMENT_NODE)
            } // end of for loop
        }  // end of if (nodeCount > 0)
        // check if the subject is null
        if (this._subject == null) {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("FSAuthenticationStatement should " +
                        "contain one subject.");
            }
            throw new FSMsgException("missingElement",null);
        }
        FSUtils.debug.message("FSAuthenticationStatement(Element): leaving");
    }
    
    
    /**
     * Returns the value of <code>SessionIndex</code> attribute.
     *
     * @return the value of </code>SessionIndex</code> attribute.
     * @see #setSessionIndex(String)
     */
    public String getSessionIndex(){
        return sessionIndex;
    }
    
    /**
     * Sets the <code>SessionIndex</code> attribute.
     *
     * @param sessionIndex the value of <code>SessionIndex</code> attribute.
     * @see #getSessionIndex
     */
    public void setSessionIndex(String sessionIndex){
        this.sessionIndex = sessionIndex;
    }
    
    /**
     * Returns the re-authentication date for this
     * authentication statement.
     *
     * @return the re-authentication date for this object.
     * @see #setReauthenticateOnOrAfter
     */
    public Date getReauthenticateOnOrAfter(){
        return reauthenticateOnOrAfter;
    }
    
    /**
     * Sets re-authentication date for this authentication
     * statement.
     *
     * @param reauthenticateOnOrAfter the date object.
     * @see #getReauthenticateOnOrAfter
     */
    public void setReauthenticateOnOrAfter(Date reauthenticateOnOrAfter){
        this.reauthenticateOnOrAfter = reauthenticateOnOrAfter;
    }
    
    /**
     * Returns the Authentication Context in this
     * authentication statement.
     *
     * @return the Authentication Context object.
     * @see #setAuthnContext(AuthnContext)
     */
    public AuthnContext getAuthnContext(){
        return _authnContext;
    }
    
    /**
     * Sets the Authentication Context object.
     *
     * @param authnContext the Authentication Context object.
     * @see #getAuthnContext
     */
    public void setAuthnContext(AuthnContext authnContext){
        this._authnContext = authnContext;
    }
    
    /**
     * Returns the value of <code>MinorVersion</code> attribute.
     *
     * @return the value of <code>MinorVersion</code> attribute.
     * @see #setMinorVersion(int)
     */
    public int getMinorVersion() {
        return minorVersion;
    }
    
    /**
     * Sets the value of <code>MinorVersion</code> attribute.
     *
     * @param version the <code>MinorVersion</code> attribute.
     * @see #getMinorVersion
     */
    public void setMinorVersion(int version) {
        minorVersion = version;
    }
    
    /**
     * Returns a String representation of this object.
     *
     * @throws FSMsgException if there is an error creating
     *            the string.
     * @return a String representation of this Object.
     */
    public String toXMLString() throws FSMsgException {
        return (toXMLString(true, false));
    }
    
    /**
     * Returns a String representation of this object.
     *
     * @param includeNS Determines whether or not the namespace qualifier is
     *                prepended to the Element when converted
     * @param declareNS Determines whether or not the namespace is declared
     *                within the Element.
     * @return A string containing the valid XML for this object.
     * @throws FSMsgException if there is an error creating
     *         the string.
     */
    public String toXMLString(boolean includeNS,boolean declareNS)
    throws FSMsgException {
        StringBuffer result = new StringBuffer(1000);
        String prefix = "";
        String libprefix = "";
        String uri = "";
        String liburi = "";
        if (includeNS) {
            prefix = SAMLConstants.ASSERTION_PREFIX;
            libprefix = IFSConstants.LIB_PREFIX;
        }
        if (declareNS) {
            if (minorVersion == IFSConstants.FF_12_PROTOCOL_MINOR_VERSION) {
                liburi = IFSConstants.LIB_12_NAMESPACE_STRING;
            } else {
                liburi = IFSConstants.LIB_NAMESPACE_STRING;
            }
            uri = SAMLConstants.assertionDeclareStr;
        }
        
        result.append(IFSConstants.LEFT_ANGLE).append(prefix)
        .append(IFSConstants.AUTHENTICATIONSTATEMENT)
        .append(uri).append(IFSConstants.SPACE).append(liburi)
        .append(IFSConstants.SPACE);
        
        if ((_authenticationMethod != null) &&
                _authenticationMethod.length() != 0) {
            result.append(IFSConstants.AUTHENTICATION_METHOD).append("=\"")
            .append(_authenticationMethod).append("\" ");
        }
        
        if (_authenticationInstant != null) {
            result.append(IFSConstants.AUTHENTICATION_INSTANT).append("=\"")
            .append(DateUtils.toUTCDateFormat(_authenticationInstant))
            .append("\" ");
        }
        
        if (reauthenticateOnOrAfter != null) {
            result.append(IFSConstants.REAUTH_ON_OR_AFTER).append("=\"")
            .append(DateUtils.toUTCDateFormat(reauthenticateOnOrAfter))
            .append("\" ");
        }
        
        if (sessionIndex != null) {
            result.append(IFSConstants.SESSION_INDEX).append("=\"")
            .append(sessionIndex).append("\" ");
        }
        
        result.append("xsi:type")
        .append("=\"")
        .append(libprefix)
        .append(IFSConstants.AUTHENTICATIONSTATEMENT_TYPE)
        .append(IFSConstants.QUOTE)
        .append(IFSConstants.RIGHT_ANGLE);
        
        if (getSubject() != null) {
            result.append(
                    ((FSSubject)getSubject()).toXMLString(includeNS, false));
        }
        
        if (_subjectLocality != null) {
            result.append(_subjectLocality.toString(includeNS, false));
        }
        
        if ((_authorityBinding != null) && (!_authorityBinding.isEmpty())) {
            Iterator iter = this.getAuthorityBinding().iterator();
            while (iter.hasNext()) {
                AuthorityBinding authBinding =
                        (AuthorityBinding)iter.next();
                result.append(authBinding.toString(includeNS, false));
            }
        }
        if (_authnContext != null) {
            result.append(_authnContext.toXMLString(includeNS, false));
        }
        result.append(IFSConstants.START_END_ELEMENT).append(prefix)
        .append(IFSConstants.AUTHENTICATIONSTATEMENT)
        .append(IFSConstants.RIGHT_ANGLE);
        return(result.toString());
    }
}
