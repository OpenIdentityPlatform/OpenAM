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
 * $Id: SecurityAssertion.java,v 1.3 2009/10/01 18:42:07 mallas Exp $
 *
 */

package com.sun.identity.liberty.ws.security;


import com.sun.identity.shared.xml.XMLUtils;
import com.sun.identity.shared.DateUtils;
import com.sun.identity.liberty.ws.common.wsse.WSSEConstants;
import com.sun.identity.liberty.ws.soapbinding.SOAPBindingConstants;
import com.sun.identity.saml.assertion.Advice;
import com.sun.identity.saml.assertion.Assertion;
import com.sun.identity.saml.assertion.Conditions;
import com.sun.identity.saml.assertion.Statement;
import com.sun.identity.saml.assertion.Subject;
import com.sun.identity.saml.assertion.SubjectConfirmation;
import com.sun.identity.saml.assertion.SubjectStatement;

import com.sun.identity.saml.common.SAMLConstants;
import com.sun.identity.saml.common.SAMLException;
import com.sun.identity.saml.common.SAMLUtils;

import com.sun.identity.saml.xmlsig.XMLSignatureManager;

import java.io.ByteArrayOutputStream;

import java.text.ParseException;

import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * The <code>SecurityAssertion</code> class provides an extension to
 * <code>Assertion</code> class to support <code>ID-WSF</code>
 * <code>ResourceAccessStatement</code> and
 * <code>SessionContextStatement</code>.
 *
 * @supported.all.api
 */
public class SecurityAssertion extends Assertion {
    
    private String verifyingCertAlias = null;
    
    /**
     * This constructor creates a <code>SecurityAssertion</code> object
     * from a DOM Element.
     *
     * @param assertionElement A <code>org.w3c.dom.Element</code> representing
     *        DOM tree for <code>Assertion</code> object
     * @throws SAMLException if it could not process the Element properly,
     *         implying that there is an error in the sender or in the
     *         element definition.
     */
    public SecurityAssertion(org.w3c.dom.Element assertionElement)
    throws SAMLException {
        parseAssertionElement(assertionElement);
    }
    
    /**
     * Constructs <code>SecurityAssertion</code> object with the
     * <code>assertionID</code>, the issuer, time when assertion issued
     * and a <code>Set</code> of <code>Statement</code>(s) in the assertion.
     *
     * @param assertionID <code>assertionID</code> attribute contained within
     *        this <code>Assertion</code> if null, an <code>assertionID</code>
     *        is generated internally.
     * @param issuer String representing the issuer of this assertion.
     * @param issueInstant time instant of the issue. It has type
     *        <code>dateTime</code> which is built in to the W3C XML Schema
     *        Types specification.if null, current time is used.
     * @param statements Set of <code>Statement</code> objects within this
     *        <code>Assertion</code>. It could be of type
     *        <code>AuthenticationStatement</code>,
     *        <code>AuthorizationDecisionStatement</code> and
     *        <code>AttributeStatement</code>. Each Assertion can have multiple
     *        type of statements in it.
     * @throws SAMLException issuer is null or the size of statements is 0.
     */
    public SecurityAssertion(String assertionID,java.lang.String issuer,
            Date issueInstant,  Set statements) throws SAMLException {
        super(assertionID, issuer, issueInstant, statements);
    }
    
    /**
     * Constructs <code>SecurityAssertion</code> object with the
     * <code>assertionID</code>, the issuer, time when assertion issued, the
     * conditions when creating a new assertion and a <code>Set</code> of
     * <code>Statement</code>(s) in the assertion.
     *
     * @param assertionID String representing <code>AssertionID</code>
     *        contained within this <code>Assertion</code> if null its generated
     *        internally.
     * @param issuer String representing the issuer of this assertion.
     * @param issueInstant time instant of the issue. It has type
     *        <code>dateTime</code> which is built in to the W3C XML
     *        Schema Types specification. if null current time is used.
     * @param conditions <code>Conditions</code> under which the this
     *        <code>Assertion</code> is valid.
     * @param statements Set of <code>Statement</code> objects within this
     *        <code>Assertion</code>. It could be of type
     *        <code>AuthenticationStatement</code>,
     *        <code>AuthorizationDecisionStatement</code> and
     *        <code>AttributeStatement</code>. Each Assertion can have multiple
     *        type of statements in it.
     * @throws SAMLException issuer is null or the size of statements is 0.
     */
    public SecurityAssertion(String assertionID,java.lang.String issuer,
            Date issueInstant,  Conditions conditions, Set statements)
            throws SAMLException {
        super(assertionID, issuer, issueInstant, conditions, statements);
    }
    
    /**
     * Constructs <code>SecurityAssertion</code> object with the
     * <code>assertionID</code>, the issuer, time when assertion issued,
     * the conditions when creating a new assertion, <code>Advice</code>
     * applicable to this <code>Assertion</code> and a <code>Set</code> of
     * <code>Statement</code>(s) in the assertion.
     *
     * @param assertionID <code>AssertionID</code> object contained within this
     *        <code>Assertion</code> if null its generated internally.
     * @param issuer String representing the issuer of this assertion.
     * @param issueInstant time instant of the issue. It has type
     *        <code>dateTime</code> which is built in to the W3C XML Schema
     *        Types specification. if null current time is used.
     * @param conditions <code>Conditions</code> under which the this
     *        <code>Assertion</code> is valid.
     * @param advice <code>Advice</code> applicable for this
     *        <code>Assertion</code>.
     * @param statements Set of <code>Statement</code> objects within this
     *        <code>Assertion</code>. It could be of type
     *        <code>AuthenticationStatement</code>,
     *        <code>AuthorizationDecisionStatement</code> and
     *        <code>AttributeStatement</code>. Each Assertion can have multiple
     *        type of statements in it.
     * @throws SAMLException issuer is null or the size of statements is 0.
     */
    public SecurityAssertion(String assertionID,java.lang.String issuer,
            Date issueInstant,  Conditions conditions, Advice advice,
            Set statements) throws SAMLException {
        super(assertionID, issuer, issueInstant, conditions,
                advice, statements);
    }
    
    /**
     * Sets the value of the certificate alias.
     *
     * @param certAlias the certificate alias.
     */
    public void setVerifyingCertAlias(String certAlias) {
        verifyingCertAlias = certAlias;
    }
    
    /**
     * Return whether the signature is valid.
     *
     * @return true if the signature is valid.
     */
    public boolean isSignatureValid() {
        if (signed & ! validationDone) {
            try {
                XMLSignatureManager manager = XMLSignatureManager.getInstance();
                valid = manager.verifyXMLSignature(xmlString,
                        ASSERTION_ID_ATTRIBUTE, verifyingCertAlias);
            } catch (Exception e) {
                if (SAMLUtils.debug.warningEnabled()) {
                    SAMLUtils.debug.warning(
                            "SecurityAssertion.isSignatureValid: "+
                            " signature validation exception", e);
                }
                valid = false;
            }
            if (SAMLUtils.debug.messageEnabled()) {
                SAMLUtils.debug.message("SAMLUtils.checkSignatureValid:"+
                        " valid = " + valid);
            }
            
            validationDone = true;
        }
        return valid;
    }
    
    /**
     * Determines if the <code>SecurityAssertion</code> contains SAML Bearer
     * confirmation method.
     *
     * @return true if the <code>SecurityAssertion</code> contains SAML Bearer
     *         confirmation.
     */
    public boolean isBearer() {
        if (_statements == null || _statements.isEmpty()) {
            return false;
        }
        
        Iterator iter = _statements.iterator();
        while(iter.hasNext()) {
            Object statement = iter.next();
            if (!(statement instanceof SubjectStatement)) {
                continue;
            }
            Subject subject = ((SubjectStatement)statement).getSubject();
            if (subject == null) {
                continue;
            }
            SubjectConfirmation sc = subject.getSubjectConfirmation();
            if (sc == null) {
                continue;
            }
            Set confirmationMethods = sc.getConfirmationMethod();
            if (confirmationMethods == null || confirmationMethods.isEmpty()) {
                continue;
            }
            if (confirmationMethods.contains(
                    SAMLConstants.CONFIRMATION_METHOD_BEARER)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Determines if the <code>SecurityAssertion</code> contains SAML Bearer
     * confirmation method. If it is, return its Subject. Otherwise, return
     * null.
     *
     * @return Subject if the <code>SecurityAssertion</code> contains SAML
     *         Bearer confirmation.
     */
    public Subject getBearerSubject() {
        if (_statements == null || _statements.isEmpty()) {
            return null;
        }
        
        Iterator iter = _statements.iterator();
        while(iter.hasNext()) {
            Object statement = iter.next();
            if (!(statement instanceof SubjectStatement)) {
                continue;
            }
            Subject subject = ((SubjectStatement)statement).getSubject();
            if (subject == null) {
                continue;
            }
            SubjectConfirmation sc = subject.getSubjectConfirmation();
            if (sc == null) {
                continue;
            }
            Set confirmationMethods = sc.getConfirmationMethod();
            if (confirmationMethods == null || confirmationMethods.isEmpty()) {
                continue;
            }
            if (confirmationMethods.contains(
                    SAMLConstants.CONFIRMATION_METHOD_BEARER)) {
                return subject;
            }
        }
        return null;
    }
    
    /**
     * Create a String representation of the  element.
     * @return A string containing the valid XML for this element.
     * By default name space name is prepended to the element name
     * example <code>&lt;saml:Assertion&gt;</code>.
     *
     * @return the String representation of this element.
     */
    public java.lang.String toString() {
        // call toString() with includeNS true by default and declareNS false
        return this.toString(true, false);
    }
    
    /**
     * Creates a String representation of the <code>&lt;Assertion&gt;</code>
     * element.
     *
     * @param includeNS if true prepends all elements by their Namespace
     *        name example <code>&lt;saml:Assertion&gt</code>;
     * @param declareNS if true includes the namespace within the generated
     *        XML.
     * @return A string containing the valid XML for this element.
     */
    public java.lang.String toString(boolean includeNS, boolean declareNS) {
        if (signed && (xmlString != null)) {
            return xmlString;
        }
        
        StringBuffer xml = new StringBuffer(3000);
        String NS="";
        String appendNS="";
        if (declareNS) {
            NS=SAMLConstants.assertionDeclareStr;
        }
        if (includeNS) {
            appendNS="saml:";
        }
        String dateStr = null;
        
        if (_issueInstant != null)  {
            dateStr = DateUtils.toUTCDateFormat(_issueInstant);
        }
        
        xml.append("<").append(appendNS).append("Assertion").append(" ").
                append(NS).append(" ").append("MajorVersion").append("=\"").
                append(_majorVersion).append("\"").append(" ").
                append("MinorVersion").append("=\"").append(_minorVersion).
                append("\"").append(" ").append("AssertionID=\"").
                append(_assertionID.getAssertionIDReference()).append("\"").
                append(" ").append("Issuer").append("=\"").append(_issuer).
                append("\"").append(" ").append("IssueInstant").append("=\"").
                append(dateStr).append("\"").
                append(" ").append(">").append(sc.NL);
        if (_conditions != null) {
            xml.append(_conditions.toString(includeNS, false));
        }
        if (_advice != null) {
            xml.append(_advice.toString(includeNS, false));
        }
        Iterator i = getStatement().iterator();
        while (i.hasNext()) {
            Statement st = (Statement)i.next();
            xml.append(st.toString(includeNS, declareNS));
        }
        if (signed && (signatureString != null)) {
            xml.append(signatureString);
        }
        String o = SAMLUtils.makeEndElementTagXML("Assertion", includeNS);
        xml.append(o);
        return xml.toString();
    }
    
    protected boolean processUnknownElement(Element element)
    throws SAMLException {
        if (super.processUnknownElement(element)) {
            if (SAMLUtils.debug.messageEnabled()) {
                SAMLUtils.debug.message(
                        "SecurityAssertion.processUnknownElement: " +
                        "super returns true");
            }
            return true;
        }
        
        if (SAMLUtils.debug.messageEnabled()) {
            SAMLUtils.debug.message(
                    "SecurityAssertion.processUnknownElement: " +
                    "super returns false");
        }
        String localName = element.getLocalName();
        if (localName.equals("ResourceAccessStatement")) {
            _statements.add(new ResourceAccessStatement(element));
            return true;
        }
        
        if (localName.equals("SessionContextStatement")) {
            _statements.add(new SessionContextStatement(element));
            return true;
        }
        
        return false;
    }
    
    protected int getMinAssertionMinorVersion() {
        return sc.ASSERTION_MINOR_VERSION;
    }
    
    protected int getMaxAssertionMinorVersion() {
        return sc.ASSERTION_MINOR_VERSION;
    }
    
    /**
     * Add the <code>Assertion</code> to the Document Element.
     *
     * @param headerE the element to be updated.
     * @throws Exception if there is an error.
     */
    public void addToParent(Element headerE) throws Exception {
        
        Document doc = headerE.getOwnerDocument();
        
        Element securityE =
                doc.createElementNS(WSSEConstants.NS_WSSE_WSF11,
                WSSEConstants.TAG_WSSE + ":" +
                WSSEConstants.TAG_SECURITYT);
        
        securityE.setAttributeNS(SOAPBindingConstants.NS_XML,
                WSSEConstants.TAG_XML_WSSE,
                WSSEConstants.NS_WSSE_WSF11);
        
        headerE.appendChild(securityE);
        
        Document assertionDoc = XMLUtils.toDOMDocument(
                toString(true, true), SAMLUtils.debug);
        
        Element assertionE = assertionDoc.getDocumentElement();
        securityE.appendChild(doc.importNode(assertionE, true));
        
    }
}
