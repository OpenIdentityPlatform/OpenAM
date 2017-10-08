/*
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
 * $Id: FSAssertion.java,v 1.2 2008/06/25 05:46:43 qcheng Exp $
 *
 * Portions Copyrighted 2014-2016 ForgeRock AS.
 */


package com.sun.identity.federation.message;

import static org.forgerock.openam.utils.Time.*;

import com.sun.identity.federation.common.FSUtils;
import com.sun.identity.federation.common.IFSConstants;
import com.sun.identity.federation.message.common.FSMsgException;
import com.sun.identity.saml.assertion.Advice;
import com.sun.identity.saml.assertion.Assertion;
import com.sun.identity.saml.assertion.AttributeStatement;
import com.sun.identity.saml.assertion.AuthorizationDecisionStatement;
import com.sun.identity.saml.assertion.Conditions;
import com.sun.identity.saml.assertion.Statement;
import com.sun.identity.saml.common.SAMLConstants;
import com.sun.identity.saml.common.SAMLException;
import com.sun.identity.saml.common.SAMLResponderException;
import com.sun.identity.saml.common.SAMLVersionMismatchException;
import com.sun.identity.saml.xmlsig.XMLSignatureManager;
import com.sun.identity.liberty.ws.security.SecurityAssertion;
import com.sun.identity.shared.DateUtils;
import com.sun.identity.shared.xml.XMLUtils;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * The class <code>FSAssertion</code> creates and parses Liberty
 * <code>Assertion</code> during the Single Sign-On process.
 * This class extends from SAML Assertion.
 *
 * @supported.all.api
 * @deprecated since 12.0.0
 */
@Deprecated
public class FSAssertion extends Assertion {
    
    /**
     * The Document Element of this object.
     */
    private Element domElement;
    
    /**
     * The <code>SAMLConstants</code> object.
     */
    static SAMLConstants sc;
    
    /**
     * The value of the <code>id</code> attribute in the <code>Assertion</code>.
     */
    protected String id;
    
    /**
     * The value of the <code>MinorVersion</Version> attribute in
     * the <code>Assertion</code>.
     */
    protected int minorVersion = IFSConstants.FF_11_ASSERTION_MINOR_VERSION;
    
    /**
     * List of Security <code>Assertions</code>.
     */
    private List securityAssertions;
    
    /**
     * The value of the <code>InResponseTo</code> attribute in the
     * <code>Assertion</code>.
     */
    protected String inResponseTo ;
    
    /**
     * Constructor to create an <code>FSAssertion</code> object
     * from the Document Element.
     *
     * @param assertionElement the <code>Assertion</code> Document Element.
     * @throws FSMsgException if the document element is null
     *         or cannot be retrieved.
     * @throws SAMLException if the SAML Assertion version is
     *         incorrect
     */
    public FSAssertion(Element assertionElement )
    throws FSMsgException, SAMLException {
        FSUtils.debug.message("FSAssertion(Element):  Called");
        Element elt = (Element) assertionElement;
        String eltName = elt.getLocalName();
        if (eltName == null)  {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("FSAssertion: local name missing");
            }
            throw new FSMsgException("nullInput", null) ;
        }
        if (!(eltName.equals(IFSConstants.ASSERTION)))  {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("FSAssertion: invalid root element");
            }
            String[] args = { eltName };
            throw new FSMsgException("invalidElement" , args) ;
        }
        domElement = assertionElement;
        id = elt.getAttribute(IFSConstants.ID);
        String read = elt.getAttribute(IFSConstants.MAJOR_VERSION);
        if ((read == null) || (read.length() == 0)) {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("FSAssertion: MajorVersion missing");
            }
            String[] args = { "MajorVersion" };
            throw new FSMsgException("missingAttribute", args);
        } else  {
            int ver = 0;
            try {
                ver = Integer.parseInt(read);
            } catch ( NumberFormatException ne ) {
                FSUtils.debug.error("FSAssertion: invalid integer " +
                        "in MajorVersion", ne);
                throw new FSMsgException("invalidNumber",null);
            }
            if (ver != sc.ASSERTION_MAJOR_VERSION) {
                if(ver < sc.ASSERTION_MAJOR_VERSION) {
                    FSUtils.debug.error("FSAssertion: MajorVersion too low");
                    throw new SAMLVersionMismatchException(FSUtils.BUNDLE_NAME,
                            "assertionVersionTooLow",null);
                } else if (ver > sc.ASSERTION_MAJOR_VERSION) {
                    FSUtils.debug.error("FSAssertion: MajorVersion too high");
                    throw new SAMLVersionMismatchException(FSUtils.BUNDLE_NAME,
                            "assertionVersionTooHigh",null);
                }
            }
        }
        read = elt.getAttribute(IFSConstants.MINOR_VERSION);
        if ((read == null) || (read.length() == 0)) {
            FSUtils.debug.error("FSAssertion: MinorVersion missing");
            String[] args = { "MinorVersion" };
            throw new FSMsgException("missingAttribute",args);
        } else  {
            try {
                minorVersion = Integer.parseInt(read);
            } catch ( NumberFormatException ne ) {
                FSUtils.debug.error(
                        "FSAssertion: invalid integer in MinorVersion", ne);
                throw new FSMsgException("invalidNumber",null);
            }
            if (minorVersion < IFSConstants.FF_11_ASSERTION_MINOR_VERSION) {
                FSUtils.debug.error("FSAssertion: MinorVersion too low");
                throw new SAMLVersionMismatchException(FSUtils.BUNDLE_NAME,
                        "assertionVersionTooLow",null);
            } else if (minorVersion >
                    IFSConstants.FF_12_POST_ASSERTION_MINOR_VERSION) {
                FSUtils.debug.error("FSAssertion: MinorVersion too high");
                throw new SAMLVersionMismatchException(FSUtils.BUNDLE_NAME,
                        "assertionMinorVersionTooHigh",null);
            }
        }
        read = elt.getAttribute(IFSConstants.ASSERTION_ID);
        if ((read == null) || (read.length() == 0)) {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("FSAssertion: AssertionID missing");
            }
            String[] args = { IFSConstants.ASSERTION_ID };
            throw new FSMsgException("missingAttribute",args);
        } else {
            setAssertionID(read);
        }
        read = elt.getAttribute(IFSConstants.ISSUER);
        if ((read == null) || (read.length() == 0)) {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("FSAssertion: Issuer missing");
            }
            String[] args = { IFSConstants.ISSUER };
            throw new FSMsgException("missingAttribute",args);
        } else {
            setIssuer(read);
        }
        read = elt.getAttribute(IFSConstants.IN_RESPONSE_TO);
        if ((read == null) || (read.length() == 0)) {
            if (FSUtils.debug.messageEnabled())  {
                FSUtils.debug.message("FSAssertion: InResponseTo missing");
            }
            String[] args = { IFSConstants.IN_RESPONSE_TO };
            throw new FSMsgException("missingAttribute",args);
        } else  {
            inResponseTo = read;
        }
        read = elt.getAttribute(IFSConstants.ISSUE_INSTANT);
        if ((read == null) || (read.length() == 0)) {
            if (FSUtils.debug.messageEnabled())  {
                FSUtils.debug.message("FSAssertion: IssueInstant missing");
            }
            String[] args = { IFSConstants.ISSUE_INSTANT };
            throw new FSMsgException("missingAttribute",args);
        } else  {
            try {
                setIssueInstant(DateUtils.stringToDate(read));
            } catch (ParseException pe) {
                FSUtils.debug.message(
                        "FSAssertion: could not parse IssueInstant", pe);
                throw new FSMsgException("wrongInput",null);
            }
        }
        boolean statementFound = false;
        NodeList nl = assertionElement.getChildNodes();
        int length = nl.getLength();
        for (int n=0; n<length; n++) {
            Node child = (Node)nl.item(n);
            if (child.getNodeType() != Node.ELEMENT_NODE) continue;
            String childName = child.getLocalName();
            if (childName.equals(IFSConstants.CONDITIONS)){
                setConditions(new Conditions((Element)child));
            } else if (childName.equals(IFSConstants.ADVICE)){
                /**
                 * The SAML Advice could not parse this advice as it does not
                 * anything about Resource Access Statement. Hence commenting
                 * the following and parsing in this assertion only. Currently
                 * the FSAssertion does not have any advice element besides for
                 * the credential.
                 */
                parseAdvice((Element)child);
            } else if (childName.equals(IFSConstants.AUTHENTICATIONSTATEMENT)) {
                addStatement(new FSAuthenticationStatement((Element)child));
                statementFound=true;
            } else if (childName.equals(IFSConstants.AUTHZDECISIONSTATEMENT)) {
                addStatement(new AuthorizationDecisionStatement(
                        (Element)child));
                statementFound=true;
            } else if (childName.equals(IFSConstants.ATTRIBUTESTATEMENT)) {
                addStatement(new AttributeStatement((Element)child));
                statementFound=true;
            } else if (childName.equals(IFSConstants.SIGNATURE)) {
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message("FSAssertion: Signature found");
                }
            } else {
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message(
                            "FSAssertion: invalid element in Assertion");
                }
                throw new FSMsgException("invalidElement", null);
            }
        }
        //check for signature
        List signs = XMLUtils.getElementsByTagNameNS1(assertionElement,
                SAMLConstants.XMLSIG_NAMESPACE_URI,
                SAMLConstants.XMLSIG_ELEMENT_NAME);
        int signsSize = signs.size();
        if (signsSize == 1) {
            Element elem = (Element)signs.get(0);
            setSignature(elem);
            xmlString = XMLUtils.print(assertionElement);
            signed = true;
        } else if (signsSize != 0) {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("FSAssertion(Element): included more than"
                        + " one Signature element.");
            }
            throw new FSMsgException("moreElement", null);
        }
        //end check for signature
        if (!statementFound) {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("Assertion: mandatory statement missing");
            }
            throw new FSMsgException("missingStatement",null);
        }
        FSUtils.debug.message("FSAssertion(Element): leaving");
    }
    
    /**
     * Constructor to create <code>FSAssertion</code> object.
     *
     * @param assertionID the <code>AssertionID</code> element.
     * @param issuer the <code>Issuer</code> element.
     * @param issueInstant the <code>IssueInstant</code> element.
     * @param statements the <code>Statement</code> elements.
     *        List of statements that need to be added in assertion.
     * @param inResponseTo value of <code>InResponseTo</code> attribute in the
     *        assertion.
     * @throws FSMsgException if the document element is null
     *         or cannot be retrieved.
     * @throws SAMLException if the SAML Assertion version is
     *         incorrect.
     */
    public FSAssertion(String assertionID,String issuer,Date issueInstant,
            Set statements,String inResponseTo)
            throws FSMsgException, SAMLException {
        super(assertionID, issuer, issueInstant, statements);
        this.inResponseTo = inResponseTo;
    }
    
    /**
     * Constructor to create <code>FSAssertion</code> object.
     *
     * @param assertionID the <code>AssertionID</code> element.
     * @param issuer the <code>Issuer</code> element.
     * @param issueInstant the <code>IssueInstant</code> element.
     * @param conditions the <code>Conditions</code> object.
     * @param statements the <code>Statement</code> elements.
     *        List of statements that need to be added in assertion.
     * @param inResponseTo value of <code>InResponseTo</code> attribute in
     *        the assertion.
     * @throws FSMsgException if the document element is null
     *         or cannot be retrieved.
     * @throws SAMLException if the SAML Assertion version is
     *         incorrect.
     */
    public FSAssertion(String assertionID,String issuer,Date issueInstant,
            Conditions conditions,Set statements,String inResponseTo)
            throws FSMsgException, SAMLException {
        super(assertionID, issuer, issueInstant, conditions, statements);
        this.inResponseTo = inResponseTo;
    }
    
    /**
     * Constructor to create an <code>FSAssertion</code> object.
     *
     * @param assertionID the <code>AssertionID</code> element.
     * @param issuer the <code>Issuer</code> element.
     * @param issueInstant the <code>IssueInstant</code> element.
     * @param conditions the <code>Conditions</code> object.
     * @param advice the <code>Advice</code> object.
     * @param statements the <code>Statement</code> elements.
     *        List of statements that need to be added in assertion.
     * @param inResponseTo value of <code>InResponseTo</code> attribute
     *        in the assertion.
     * @throws FSMsgException if the document element is null
     *         or cannot be retrieved.
     * @throws SAMLException if the SAML Assertion version is
     *         incorrect.
     */
    public FSAssertion(String assertionID,String issuer,Date issueInstant,
            Conditions conditions,Advice advice,Set statements,
            String inResponseTo)
            throws FSMsgException, SAMLException {
        super(assertionID, issuer, issueInstant,conditions, advice, statements);
        this.inResponseTo = inResponseTo;
    }
    
    /**
     * Returns value of <code>id</code> attribute.
     *
     * @return value of <code>id</code> attribute.
     * @see #setID(String)
     */
    public String getID(){
        return id;
    }
    
    /**
     * Sets  value of <code>id<code> attribute.
     *
     * @param id value of <code>id</code> attribute.
     * @see #getID
     */
    public void setID(String id){
        this.id = id;
    }
    
    /**
     * Returns the <code>MinorVersion</code> attribute.
     *
     * @return the <code>MinorVersion</code> attribute.
     * @see #setMinorVersion(int)
     */
    public int getMinorVersion() {
        return minorVersion;
    }
    
    /**
     * Sets the <code>MinorVersion</code> attribute.
     *
     * @param version the <code>MinorVersion</code> attribute.
     * @see #getMinorVersion
     */
    public void setMinorVersion(int version) {
        minorVersion = version;
    }
    
    /**
     * Returns the Document Element for this object.
     *
     * @return the Document Element for this object.
     */
    public Element getDOMElement() {
        return domElement;
    }
    
    /**
     * Returns the value of <code>InResponseTo</code> attribute.
     *
     * @return the value of <code>InResponseTo</code> attribute.
     * @see #setInResponseTo(String)
     */
    public String getInResponseTo() {
        return inResponseTo;
    }
    
    /**
     * Sets the value of <code>InResponseTo</code> attribute.
     *
     * @param inResponseTo value of <code>InResponseTo</code> attribute.
     * @see #getInResponseTo
     */
    public void setInResponseTo(String inResponseTo) {
        this.inResponseTo = inResponseTo;
    }
    
    /**
     * Returns Signed XML String.
     *
     * @return Signed XML String.
     */
    public String getSignedXMLString(){
        return xmlString;
    }
    
    /**
     * Returns the <code>Signature</code> string.
     *
     * @return the <code>Signature</code> string.
     */
    public String getSignatureString(){
        return signatureString;
    }
    
    /**
     * Checks validity of time in the assertion.
     *
     * @return true if time is valid otherwise false.
     */
    public boolean isTimeValid() {
        boolean isTimeValid = true;
        Conditions conditions = getConditions();
        if (conditions != null)  {
            isTimeValid = conditions.checkDateValidity(
                    currentTimeMillis());
        }
        return isTimeValid;
    }
    
    /**
     * Adds the <code>Statement</code> object to the
     * Statment's object Set.
     *
     * @param statement the <code>Statement</code> object.
     * @return false if statement is null else true.
     */
    public boolean addStatement(Statement statement) {
        boolean addedStmt = false;
        if (statement != null) {
            super.addStatement(statement);
            addedStmt = true;
        }
        return addedStmt;
    }
    
    /**
     * Returns a <code>XML</code> String representation of this object.
     *
     * @return a String representation of this Object.
     * @throws FSMsgException if there is an error creating
     *         the <code>XML</code> string.
     */
    
    public String toXMLString() throws FSMsgException {
        return this.toXMLString(true, true);
    }
    
    /**
     * Returns a <code>XML</code> String representation of this object.
     *
     * @param includeNS determines whether or not the namespace qualifier is
     *                prepended to the Element when converted
     * @param declareNS determines whether or not the namespace is declared
     *                within the Element.
     * @return a string containing the valid <code>XML</code> for this object.
     * @throws FSMsgException if there is an error creating
     *         the <code>XML</code> string.
     */
    
    public java.lang.String toXMLString(boolean includeNS,boolean declareNS)
    throws FSMsgException {
        StringBuffer xml = new StringBuffer(3000);
        String NS="";
        String appendNS="";
        String libNS="";
        String libAppendNS="";
        String uriXSI="";
        if (declareNS) {
            NS=sc.assertionDeclareStr;
            if(minorVersion == IFSConstants.FF_12_POST_ASSERTION_MINOR_VERSION
                    || minorVersion ==
                    IFSConstants.FF_12_ART_ASSERTION_MINOR_VERSION) {
                libNS = IFSConstants.LIB_12_NAMESPACE_STRING;
            } else {
                libNS = IFSConstants.LIB_NAMESPACE_STRING;
            }
            uriXSI = IFSConstants.XSI_NAMESPACE_STRING;
        }
        if (includeNS) {
            appendNS= SAMLConstants.ASSERTION_PREFIX;
            libAppendNS = IFSConstants.LIB_PREFIX;
        }
        String dateStr = null;
        if (getIssueInstant() != null)  {
            dateStr = DateUtils.toUTCDateFormat(getIssueInstant());
        }
        xml.append(IFSConstants.LEFT_ANGLE)
        .append(appendNS).append(IFSConstants.ASSERTION)
        .append(IFSConstants.SPACE)
        .append(NS).append(IFSConstants.SPACE).append(uriXSI)
        .append(IFSConstants.SPACE).append(libNS)
        .append(IFSConstants.SPACE);
        
        if (minorVersion == IFSConstants.FF_11_ASSERTION_MINOR_VERSION &&
                id != null && !(id.length() == 0)) {
            xml.append(IFSConstants.SPACE).append(IFSConstants.ID)
            .append(IFSConstants.EQUAL_TO).append(IFSConstants.QUOTE)
            .append(id).append(IFSConstants.QUOTE)
            .append(IFSConstants.SPACE);
        }
        xml.append(IFSConstants.MAJOR_VERSION)
        .append(IFSConstants.EQUAL_TO).append(IFSConstants.QUOTE)
        .append(getMajorVersion()).append(IFSConstants.QUOTE)
        .append(IFSConstants.SPACE).append(IFSConstants.MINOR_VERSION)
        .append(IFSConstants.EQUAL_TO).append(IFSConstants.QUOTE)
        .append(minorVersion).append(IFSConstants.QUOTE)
        .append(IFSConstants.SPACE).append(IFSConstants.ASSERTION_ID)
        .append(IFSConstants.EQUAL_TO).append(IFSConstants.QUOTE)
        .append(getAssertionID()).append(IFSConstants.QUOTE)
        .append(IFSConstants.SPACE).append(IFSConstants.ISSUER)
        .append(IFSConstants.EQUAL_TO).append(IFSConstants.QUOTE)
        .append(getIssuer()).append(IFSConstants.QUOTE)
        .append(IFSConstants.SPACE).append(IFSConstants.ISSUE_INSTANT)
        .append(IFSConstants.EQUAL_TO).append(IFSConstants.QUOTE)
        .append(dateStr).append(IFSConstants.QUOTE)
        .append(IFSConstants.SPACE).append(IFSConstants.IN_RESPONSE_TO)
        .append(IFSConstants.EQUAL_TO).append(IFSConstants.QUOTE)
        .append(inResponseTo).append(IFSConstants.QUOTE)
        .append(IFSConstants.SPACE)
        .append(IFSConstants.XSI_TYPE)
        .append(IFSConstants.EQUAL_TO).append(IFSConstants.QUOTE)
        .append(libAppendNS)
        .append(IFSConstants.ASSERTION_TYPE).append(IFSConstants.QUOTE)
        .append(IFSConstants.RIGHT_ANGLE).append(sc.NL);
        
        if (getConditions() != null) {
            xml.append(getConditions().toString(includeNS, false));
        }
        if (getAdvice() != null) {
            xml.append(getAdvice().toString(includeNS, false));
        }
        
        Iterator i = getStatement().iterator();
        while (i.hasNext()) {
            Statement st = (Statement)i.next();
            if(st instanceof FSAuthenticationStatement){
                xml.append(((FSAuthenticationStatement)st).toXMLString(
                        includeNS, false));
            } else if(st instanceof AttributeStatement) {
                xml.append(((AttributeStatement)st).toString(includeNS, false));
            }
        }
        if (signed) {
            if (signatureString != null) {
                xml.append(signatureString);
            } else if (signature != null) {
                signatureString = XMLUtils.print(signature);
                xml.append(signatureString);
            }
        }
        xml.append(IFSConstants.START_END_ELEMENT)
        .append(appendNS).append(IFSConstants.ASSERTION)
        .append(IFSConstants.RIGHT_ANGLE)
        .append(IFSConstants.NL);
        
        return xml.toString();
    }
    
    /**
     * Signs the <code>Assertion</code>.
     *
     * @param certAlias the alias/name of the certificate.
     * @throws SAMLException if <code>FSAssertion</code>
     *            cannot be signed.
     */
    public void signXML(String certAlias) throws SAMLException {
        FSUtils.debug.message("FSAssertion.signXML: Called");
        if (signed) {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("FSAssertion.signXML: the assertion is "
                        + "already signed.");
            }
            throw new SAMLResponderException(
                    FSUtils.BUNDLE_NAME,"alreadySigned",null);
        }
        if (certAlias == null || certAlias.length() == 0) {
            throw new SAMLResponderException(FSUtils.BUNDLE_NAME,
                    "cannotFindCertAlias",null);
        }
        
        try {
            XMLSignatureManager manager = XMLSignatureManager.getInstance();
            if (minorVersion == IFSConstants.FF_11_ASSERTION_MINOR_VERSION) {
                signatureString = manager.signXML(this.toXMLString(true, true),
                        certAlias, (String) null,
                        IFSConstants.ID, this.id,
                        false);
            } else if (minorVersion ==
                    IFSConstants.FF_12_POST_ASSERTION_MINOR_VERSION
                    || minorVersion ==
                    IFSConstants.FF_12_ART_ASSERTION_MINOR_VERSION) {
                signatureString =
                        manager.signXML(this.toXMLString(true, true),
                        certAlias, (String) null,
                        IFSConstants.ASSERTION_ID,
                        this.getAssertionID(), false);
            } else {
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message("invalid minor version.");
                }
            }
            signature = XMLUtils.toDOMDocument(signatureString, FSUtils.debug)
            .getDocumentElement();
            signed = true;
            xmlString = this.toXMLString(true, true);
        } catch(Exception e){
            FSUtils.debug.message(" Exception :" + e.getMessage());
            throw new SAMLResponderException(e);
        }
    }
    
    /**
     * Sets the <code>Element's</code> signature.
     *
     * @param elem the <code>Element</code> object
     * @return true if signature is set otherwise false
     */
    public boolean setSignature(Element elem) {
        signatureString = XMLUtils.print(elem);
        return super.setSignature(elem);
    }
    
    /**
     * Parses the advice element to extract the Security <code>Assertion</code>.
     *
     * @param element the <code>Advice</code> Element.
     */
    public void parseAdvice(Element element) {
        NodeList nl = element.getChildNodes();
        int length = nl.getLength();
        for (int n=0; n<length; n++) {
            Node child = (Node)nl.item(n);
            if (child.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            String childName = child.getLocalName();
            if (childName.equals("Assertion")) {
                try {
                    if (securityAssertions == null) {
                        securityAssertions = new ArrayList();
                    }
                    securityAssertions.add(
                            new SecurityAssertion((Element)child));
                } catch (Exception ex) {
                    FSUtils.debug.error("FSAssertion.parseAdvice: Error in" +
                            "parsing security assertion", ex);
                }
            }
        }
        if ((securityAssertions != null) && (!securityAssertions.isEmpty())) {
            _advice = new Advice(null, securityAssertions, null);
        }
    }
    
    /**
     * Returns the discovery service credentials from the boot strap.
     *
     * @return the discovery service credentials from the boot strap.
     */
    public List getDiscoveryCredential() {
        return securityAssertions;
    }
}
