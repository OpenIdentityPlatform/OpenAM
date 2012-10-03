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
 * $Id: Assertion.java,v 1.3 2008/06/25 05:47:31 qcheng Exp $
 *
 */

  
package com.sun.identity.saml.assertion;

import com.sun.identity.shared.DateUtils;
import com.sun.identity.saml.common.*;
import com.sun.identity.saml.xmlsig.*;
import com.sun.identity.common.SystemConfigurationUtil;
import com.sun.identity.shared.xml.XMLUtils;
import java.util.Set;
import java.util.List;
import java.util.Date;
import java.text.ParseException;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Element;
import org.w3c.dom.Document;

/**
 *This object stands for <code>Assertion</code> element. An Assertion is a
 *package of information that supplies one or more <code>Statement</code> made 
 *by an issuer. There are three kinds of assertionsL Authentication, 
 *AuthorizationDecision and Attribute assertion.
 *@supported.all.api
 */
public class Assertion extends AssertionBase {          

    /**
     * Returns whether the signature on the object is valid or not.
     * @return true if the signature on the object is valid; false otherwise.
     */
    public boolean isSignatureValid() {
        if (signed & ! validationDone) {
            valid = SAMLUtils.checkSignatureValid(
                xmlString, ASSERTION_ID_ATTRIBUTE, _issuer); 
                
            validationDone = true;
        }
        return valid;
    }

    /**
     * Signs the Assertion.
     * @exception SAMLException If it could not sign the Assertion.
     */
    public void signXML() throws SAMLException {
        String certAlias =
            SystemConfigurationUtil.getProperty(
            "com.sun.identity.saml.xmlsig.certalias");
        signXML(certAlias);
    }

    /**
     * Signs the Assertion.
     *
     * @param certAlias certification Alias used to sign Assertion.
     * @exception SAMLException if it could not sign the Assertion.
     */
    public void signXML(String certAlias) throws SAMLException {
        if (signed) {
            if (SAMLUtils.debug.messageEnabled()) {
                SAMLUtils.debug.message("Assertion.signXML: the assertion is "
                    + "already signed.");
            }
            throw new SAMLException(
                SAMLUtils.bundle.getString("alreadySigned"));
        }

        if (certAlias == null) {
            if (SAMLUtils.debug.messageEnabled()) {
                SAMLUtils.debug.message("Assetion.signXML: couldn't obtain "
                    + "this site's cert alias.");
            }
            throw new SAMLResponderException(
                SAMLUtils.bundle.getString("cannotFindCertAlias"));
        }

        XMLSignatureManager manager = XMLSignatureManager.getInstance();
        if ((_majorVersion == 1) && (_minorVersion == 0)) { 
            SAMLUtils.debug.message("Assetion.signXML: sign with version 1.0");
            signatureString = manager.signXML(this.toString(true, true), 
                                              certAlias);
            // this block is used for later return of signature element by
            // getSignature() method
            signature = 
                XMLUtils.toDOMDocument(signatureString, SAMLUtils.debug)
                        .getDocumentElement();

        } else {
            SAMLUtils.debug.message("Assetion.signXML: sign with version 1.1");
            Document doc = XMLUtils.toDOMDocument(this.toString(true, true),
                                                  SAMLUtils.debug);
            // sign with SAML 1.1 spec & include cert in KeyInfo
            signature = manager.signXML(doc, certAlias, null, 
                ASSERTION_ID_ATTRIBUTE, getAssertionID(), true, null);
            signatureString = XMLUtils.print(signature);
        }
        signed = true;
        xmlString = this.toString(true, true);
    }


    /** 
     *Default constructor
     *Declaring protected to enable extensibility
     */
    protected Assertion() {
        super();
    }
   
    /**
     * Contructs <code>Assertion</code> object from a
     * block of existing XML that has already been built into a DOM.
     *
     * @param assertionElement A <code>org.w3c.dom.Element</code> representing 
     *        DOM tree for <code>Assertion</code> object
     * @exception SAMLException if it could not process the Element properly, 
     *            implying that there is an error in the sender or in the
     *            element definition.
     */
    public Assertion(org.w3c.dom.Element assertionElement) 
        throws SAMLException 
    {
        parseAssertionElement(assertionElement);
    }

    protected void parseAssertionElement(Element assertionElement)
        throws SAMLException
    {
        if (SAMLUtils.debug.messageEnabled()) {
            SAMLUtils.debug.message("Assertion.parseAssertionElement:");
        }

        Element elt = (Element) assertionElement;
        String eltName = elt.getLocalName();
        if (eltName == null)  {
            if (SAMLUtils.debug.messageEnabled()) {
                SAMLUtils.debug.message("Assertion: local name missing");
            }
            throw new SAMLRequesterException(SAMLUtils.bundle.getString
                                        ("nullInput")) ;
        }
        if (!(eltName.equals("Assertion")))  {
            if (SAMLUtils.debug.messageEnabled()) {
                SAMLUtils.debug.message("Assertion: invalid root element");
            }
            throw new SAMLRequesterException(SAMLUtils.bundle.getString
                ("invalidElement")+ ":"+eltName) ;   
        }

        String read = elt.getAttribute("Issuer");
        if ((read == null) || (read.length() == 0)) {
            if (SAMLUtils.debug.messageEnabled()) {
                SAMLUtils.debug.message("Assertion: Issuer missing");
            }
            throw new SAMLRequesterException(
                SAMLUtils.bundle.getString("missingAttribute") +":"+"Issuer");
        } else  {
            _issuer = read;
        }

        List signs = XMLUtils.getElementsByTagNameNS1(assertionElement,
                                        SAMLConstants.XMLSIG_NAMESPACE_URI,
                                        SAMLConstants.XMLSIG_ELEMENT_NAME);
        int signsSize = signs.size();
        if (signsSize == 1) {
            // delay the signature validation till user call isSignatureValid()
            xmlString = XMLUtils.print(assertionElement);
            signed = true;
            validationDone = false;
        } else if (signsSize != 0) {
            if (SAMLUtils.debug.messageEnabled()) {
                SAMLUtils.debug.message("Assertion(Element): included more than"
                    + " one Signature element.");
            }
            throw new SAMLRequesterException(
                SAMLUtils.bundle.getString("moreElement"));
        }

        read = elt.getAttribute("MajorVersion");
        if ((read == null) || (read.length() == 0)) {
            if (SAMLUtils.debug.messageEnabled())  {
                SAMLUtils.debug.message("Assertion: MajorVersion missing");
            }
            throw new SAMLRequesterException(
                    SAMLUtils.bundle.getString("missingAttribute")+":"+
                        "MajorVersion");
        }
        else  {
            int ver = 0;
            try {
                ver = Integer.parseInt(read);
            } catch ( NumberFormatException ne ) {
                SAMLUtils.debug.error(
                        "Assertion: invalid integer in MajorVersion", ne);
                throw new SAMLRequesterException(
                        SAMLUtils.bundle.getString("invalidNumber")+":"+
                        "MajorVersion");
            }
            if (ver != sc.ASSERTION_MAJOR_VERSION) {
                if (ver < sc.ASSERTION_MAJOR_VERSION) {
                    if (SAMLUtils.debug.messageEnabled())  {
                        SAMLUtils.debug.message(
                            "Assertion: MajorVersion too low");
                    }
                    throw new SAMLVersionMismatchException(
                        SAMLUtils.bundle.getString("assertionVersionTooLow")
                        + ":"+"MajorVersion");
                } else if (ver > sc.ASSERTION_MAJOR_VERSION) {
                    if (SAMLUtils.debug.messageEnabled())  {
                        SAMLUtils.debug.message(
                            "Assertion: MajorVersion too high");
                    }
                    throw new SAMLVersionMismatchException(
                        SAMLUtils.bundle.getString("assertionVersionTooHigh")
                            +":"+"MajorVersion");
                }
            }
        }
        read = elt.getAttribute("MinorVersion");
        if ((read == null) || (read.length() == 0)) {
            if (SAMLUtils.debug.messageEnabled()) 
                SAMLUtils.debug.message("Assertion: MinorVersion missing");
            throw new SAMLRequesterException(
                                SAMLUtils.bundle.getString("missingAttribute")
                                    +":"+"MinorVersion");
        }
        else  {
            int ver = 0;
            try {
                ver = Integer.parseInt(read);
            } catch ( NumberFormatException ne ) {
                SAMLUtils.debug.error(
                        "Assertion: invalid integer in MinorVersion", ne);
                throw new SAMLRequesterException(
                        SAMLUtils.bundle.getString("invalidNumber")
                                    +":"+"MinorVersion");
            }

            if (SAMLUtils.debug.messageEnabled()) {
                SAMLUtils.debug.message("Assertion.parseAssertionElement: " +
                       "minMinorVersion = " + getMinAssertionMinorVersion() +
                       ", maxMinorVersion = " + getMaxAssertionMinorVersion());
            }

            if (ver < getMinAssertionMinorVersion()) {
                if (SAMLUtils.debug.messageEnabled())  {
                    SAMLUtils.debug.message("Assertion: MinorVersion too low");
                }
                throw new SAMLVersionMismatchException(
                        SAMLUtils.bundle.getString("assertionVersionTooLow"));
            } else if (ver > getMaxAssertionMinorVersion()) {
                if (SAMLUtils.debug.messageEnabled())  {
                    SAMLUtils.debug.message("Assertion: MinorVersion too high");
                }
                throw new SAMLVersionMismatchException(
                        SAMLUtils.bundle.getString("assertionVersionTooHigh")
                                    +":"+"MinorVersion");
            } else {
                _minorVersion=ver;
            }
        }
        read = elt.getAttribute("AssertionID");
        if ((read == null) || (read.length() == 0)) {
            if (SAMLUtils.debug.messageEnabled()) 
                SAMLUtils.debug.message("Assertion: AssertionID missing");
            throw new SAMLRequesterException(
                                SAMLUtils.bundle.getString("missingAttribute")
                                    +":"+"AssertionID");
        }
        else  {
            _assertionID = new AssertionIDReference(read);
        }

        read = elt.getAttribute("IssueInstant");
        if ((read == null) || (read.length() == 0)) {
            if (SAMLUtils.debug.messageEnabled())  {
                SAMLUtils.debug.message("Assertion: IssueInstant missing");
            }
            throw new SAMLRequesterException(
                                SAMLUtils.bundle.getString("missingAttribute")
                                    +":"+"IssueInstant");
        } else  {
            try {
                _issueInstant = DateUtils.stringToDate(read);
            } catch (ParseException pe) {
                if (SAMLUtils.debug.messageEnabled()) 
                    SAMLUtils.debug.message(
                    "Assertion: could not parse IssueInstant", pe);
               throw new SAMLRequesterException(SAMLUtils.bundle.getString(
                        "wrongInput") + " " + pe.getMessage());
            }
        }

        NodeList nl = assertionElement.getChildNodes();
        int length = nl.getLength();
        for (int n=0; n<length; n++) {
            Node child = (Node)nl.item(n);
            if (child.getNodeType() != Node.ELEMENT_NODE) continue;
            String childName = child.getLocalName();
            if (childName.equals("Conditions"))
                _conditions = new Conditions((Element)child);
            else if (childName.equals("Advice"))
                _advice = new Advice((Element)child);
            else if (childName.equals("AuthenticationStatement")) {
                _statements.add(new AuthenticationStatement((Element)child));
            }
            else if (childName.equals("AuthorizationDecisionStatement")) {
                _statements.add(new AuthorizationDecisionStatement(
                        (Element)child));
            }
            else if (childName.equals("AttributeStatement")) {
                _statements.add(new AttributeStatement((Element)child));
            }
            else if (childName.equals("Signature")) {
                signature = (Element) child;
            }
            else if (!processUnknownElement((Element)child)) {
                if (SAMLUtils.debug.messageEnabled()) {
                    SAMLUtils.debug.message(
                        "Assertion: invalid element in Assertion");
                }
                throw new SAMLRequesterException("invalidElement");
            }
        }
        if (_statements.isEmpty()) {
            if (SAMLUtils.debug.messageEnabled()) {
                SAMLUtils.debug.message(
                    "Assertion: mandatory statement missing");
            }
            throw new SAMLRequesterException("missingStatement");
        }
    } 
   
 
    /**
     *  Contructs <code>Assertion</code> object and populate the data members:
     * <code>assertionID</code>, the issuer, time when assertion issued and a
     * set of <code>Statement</code>(s) in the assertion.
     *
     * @param assertionID <code>assertionID</code> attribute contained within
     *        this <code>Assertion</code> if null, an <code>assertionID</code>
     *        is generated internally.
     * @param issuer The issuer of this assertion.
     * @param issueInstant time instant of the issue. It has type
     *        <code>dateTime</code> which is built in to the W3C XML Schema
     *        Types specification.if null, current time is used.
     * @param statements set of <code>Statement</code> objects within this 
     *        <code>Assertion</code>. It could be of type
     *        <code>AuthenticationStatement</code>, 
     *        <code>AuthorizationDecisionStatement</code> and
     *        <code>AttributeStatement</code>. Each Assertion can have multiple
     *        type of statements in it.
     * @exception SAMLException if there is an error in processing input.
     */
    public Assertion(String assertionID,java.lang.String issuer, 
        Date issueInstant,  Set statements) throws SAMLException
    {
        super(assertionID, issuer, issueInstant, statements);
    }

    /**
     * Contructs <code>Assertion</code> object and  populate the data members: 
     * the <code>assertionID</code>, the issuer, time when assertion issued,  
     * the conditions when creating a new assertion and a set of
     * <code>Statement</code>(s) in the assertion.
     *
     * @param assertionID <code>AssertionID</code> contained within this 
     *        <code>Assertion</code> if null its generated internally.
     * @param issuer The issuer of this assertion.
     * @param issueInstant time instant of the issue. It has type
     *        <code>dateTime</code> which is built in to the W3C XML Schema
     *        Types specification. if null, current time is used.
     * @param conditions <code>Conditions</code> under which the this 
     *        <code>Assertion</code> is valid.
     * @param statements Set of <code>Statement</code> objects within this 
     *        <code>Assertion</code>. It could be of type
     *        <code>AuthenticationStatement</code>,
     *        <code>AuthorizationDecisionStatement</code> and 
     *        <code>AttributeStatement</code>. Each Assertion can have multiple
     *        type of statements in it.
     * @exception SAMLException if there is an error in processing input.
     */
    public Assertion(String assertionID,java.lang.String issuer, 
        Date issueInstant,  Conditions conditions, Set statements) 
        throws SAMLException
    {
        super(assertionID, issuer, issueInstant, conditions, statements);
    }
   
    /**
     * Contructs <code>Assertion</code> object and populate the data members: 
     * the <code>ssertionID</code>, the issuer, time when assertion issued,
     * the conditions when creating a new assertion , <code>Advice</code>
     * applicable to this <code>Assertion</code> and a set of
     * <code>Statement</code>(s) in the assertion.
     *
     * @param assertionID <code>AssertionID</code> object contained within this
     *        <code>Assertion</code> if null its generated internally.
     * @param issuer The issuer of this assertion.
     * @param issueInstant Time instant of the issue. It has type
     *        <code>dateTime</code> which is built in to the W3C XML Schema
     *        Types specification. if null, current time is used.
     * @param conditions <code>Conditions</code> under which the this 
     *        <code>Assertion</code> is valid.
     * @param advice <code>Advice</code> applicable for this
     *        <code>Assertion</code>.
     * @param statements Set of <code>Statement</code> objects within this 
     *         <code>Assertion</code>. It could be of type
     *         <code>AuthenticationStatement</code>,
     *         <code>AuthorizationDecisionStatement</code> and 
     *         <code>AttributeStatement</code>. Each Assertion can have
     *         multiple type of statements in it.
     * @exception SAMLException if there is an error in processing input.
     */
    public Assertion(String assertionID,java.lang.String issuer, 
        Date issueInstant,  Conditions conditions, Advice advice, 
        Set statements) throws SAMLException
    {
        super(assertionID, issuer, issueInstant, conditions, advice,statements);
    }

    /**
     * Returns the advice of an assertion.
     *
     * @return <code>Advice</code> object containing advice information of the
     *         assertion.
     */
    public Advice getAdvice() {
        return (Advice)_advice; 
    }

    protected AdviceBase createAdvice(Element adviceElement) 
        throws SAMLException {
        return new Advice(adviceElement);
    }
  
    protected  AuthorizationDecisionStatementBase
        createAuthorizationDecisionStatement(Element authDecisionElement)
            throws SAMLException {
        return new AuthorizationDecisionStatement(authDecisionElement);
    }
  
    protected  AuthenticationStatement
        createAuthenticationStatement(Element authenticationElement)
            throws SAMLException {
        return new AuthenticationStatement(authenticationElement);
    }
  
    protected  AttributeStatement
        createAttributeStatement(Element attributeElement)
        throws SAMLException {
        return new AttributeStatement(attributeElement);
    }
  
    protected  AssertionIDReference
        createAssertionIDReference(Element assertionIDRefElement)
            throws SAMLException {
        return  new AssertionIDReference(assertionIDRefElement);
    }
  
    protected  AssertionIDReference
        createAssertionIDReference(String assertionID) throws SAMLException {
        return  new AssertionIDReference(assertionID);
    }
  
    protected  Conditions
        createConditions(Element conditionsElement) throws SAMLException {
        return new Conditions(conditionsElement);
    }

    protected boolean processUnknownElement(Element element)
        throws SAMLException
    {
        if (SAMLUtils.debug.messageEnabled()) {
            SAMLUtils.debug.message("Assertion.processUnknownElement:");
        }
        return false;
    }

    protected int getMinAssertionMinorVersion() {
        return sc.ASSERTION_MINOR_VERSION_ZERO;
    }

    protected int getMaxAssertionMinorVersion() {
        return sc.ASSERTION_MINOR_VERSION_ONE;
    }
}
