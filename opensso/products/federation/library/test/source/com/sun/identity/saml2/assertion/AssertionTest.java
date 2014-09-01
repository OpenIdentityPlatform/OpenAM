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
 * $Id: AssertionTest.java,v 1.2 2008/06/25 05:48:25 qcheng Exp $
 *
 */
package com.sun.identity.saml2.assertion;

import com.sun.identity.saml2.assertion.Attribute;
import com.sun.identity.saml2.assertion.AttributeStatement;
import com.sun.identity.saml2.assertion.Advice;
import com.sun.identity.saml2.assertion.AssertionFactory;
import com.sun.identity.saml2.assertion.Assertion;
import com.sun.identity.saml2.assertion.AudienceRestriction;
import com.sun.identity.saml2.assertion.AuthnContext;
import com.sun.identity.saml2.assertion.AuthnStatement;
import com.sun.identity.saml2.assertion.Conditions;
import com.sun.identity.saml2.assertion.Issuer;
import com.sun.identity.saml2.assertion.NameID;
import com.sun.identity.saml2.assertion.Subject;
import com.sun.identity.saml2.assertion.SubjectConfirmation;
import com.sun.identity.saml2.assertion.SubjectConfirmationData;
import com.sun.identity.shared.xml.XMLUtils;
import com.sun.identity.shared.DateUtils;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.common.SAML2Exception;
import java.io.FileInputStream;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import javax.xml.parsers.ParserConfigurationException;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;
import com.sun.identity.shared.test.UnitTestBase;
import java.util.Iterator;
import java.util.List;

/**
 * Unit Test Cases to test
 * <code>com.sun.identity.saml2.assertion.Assertion</code> class.
 */

public class AssertionTest extends UnitTestBase {
    
    public AssertionTest() {
        super("FedLibrary-SAML2");
    }
    
    /**
     * Validates the <code>Assertion</code> object.
     *
     * @param xmlFile the file containing the Assertion XML.
     * @throws SAML2Exception if there is creating the <code>Assertion</code>
     *         object or the XML String does not conform to the XML Schema
     * @throws ParserConfigurationException if there is an error parsing the
     *         Assertion XML string.
     * @throws IOException if there is an error reading the file.
     * @throws SAXException if there is an error during XML parsing.
     */
    @Parameters({"assertion-filename"})
    @Test(groups = {"saml2"})
    public void validateAssertion(String xmlFile) throws SAML2Exception,
            ParserConfigurationException, IOException,
            SAXException {
        entering("validateAssertion",null);
        try {
            log(Level.INFO, "validateAssertion",xmlFile);
            FileInputStream fis = new FileInputStream(new File(xmlFile));
            Document doc = XMLUtils.toDOMDocument(fis, null);
            Element elt = doc.getDocumentElement();
            Assertion assertion =
                    AssertionFactory.getInstance().createAssertion(elt);
            
            // object to xml string
            String xmlString = assertion.toXMLString(true,true);
            assert (xmlString != null) :
                "Error creating XML String from Assertion object";
            log(Level.INFO, "validateAssertion",xmlString);
            
            assert(assertion.getID().
                    equals("s25233dd934e31a013f70201cff7646b89497861f3"))
                    : "Incorrect ID attribute value";
            
            assert(assertion.getVersion().equals("2.0")):"Incorrect Version";
            
            assert(DateUtils.toUTCDateFormat(assertion.getIssueInstant()).
                    equals("2006-12-13T02:22:30Z")) : "Invalid IssueInstant";
            
            validateIssuer(assertion.getIssuer());
            validateSubject(assertion.getSubject());
            validateConditions(assertion.getConditions());
            validateAuthnStatements(assertion.getAuthnStatements());
            validateAttributeStatement(assertion.getAttributeStatements());
            validateAdvice(assertion.getAdvice());
            
            log(Level.INFO, "createAssertion",xmlString);
        } finally {
            exiting("createAssertion");
        }
    }
    
    /**
     * Validates the <code>Issuer</code> element in the assertion.
     *
     * @param issuer the <code>Issuer</code> object.
     */
    private void validateIssuer(Issuer issuer) {
        // validate issuer
        assert(issuer.getFormat().equals(SAML2Constants.PERSISTENT)) :
            "Incorrect Format";
        
        assert(issuer.getNameQualifier().equals("test.com")) :
            "Incorrect Name Qualifier";
        
        assert(issuer.getSPNameQualifier().equals("sp.test.com")) :
            "Incorrect SP Name Qualifier";
        
        assert(issuer.getSPProvidedID().equals("sp1.test.com")) :
            "Incorrect SP Provider Identifier ";
        
        assert(issuer.getValue().equals("oCd/Q2KH7rgOSdX8KiIW54t4EUbC")) :
            "Incorrect Issuer Value";
    }
    
    /**
     * Validates the <code>Subject</code> element in the Assertion.
     *
     * @param subject the <code>Subject</code> in the assertion.
     */
    private void validateSubject(Subject subject) {
        // validate NameID
        NameID nameID = subject.getNameID();
        
        assert(nameID.getFormat().equals(SAML2Constants.PERSISTENT)) :
            "Incorrect Format";
        
        assert(nameID.getNameQualifier().equals("test.com")) :
            "Incorrect Name Qualifier";
        
        assert(nameID.getSPNameQualifier().equals("sp.test.com")) :
            "Incorrect SP Name Qualifier";
        
        assert(nameID.getSPProvidedID().equals("sp1.test.com")) :
            "Incorrect SP Provider Identifier ";
        
        assert(nameID.getValue().equals("oCd/Q2KH7rgOSdX8KiIW54t4EUbC")) :
            "Incorrect NameID  value";
        
        //validate SubjectConfirmation
        
        List scList = subject.getSubjectConfirmation();
        
        SubjectConfirmation sc = (SubjectConfirmation)scList.iterator().next();
        
        SubjectConfirmationData scData = sc.getSubjectConfirmationData();
        assert(DateUtils.toUTCDateFormat(scData.getNotOnOrAfter())
        .equals("2005-12-13T02:32:30Z")) : "Incorrect NotOnOrAfter";
        
        assert(scData.getRecipient().equals(
                "http://sp.test.sun.com:80/amserver/Consumer/metaAlias/sp"))
                : "Incorrect Receipient";
        
    }
    
    /**
     * Validates the <code>Conditions</code> object in the assertion.
     *
     * @param conditions the <code>Conditions<code> object.
     */
    private void validateConditions(Conditions conditions) {
        assert(DateUtils.toUTCDateFormat(conditions.getNotBefore())
        .equals("2006-12-13T02:22:30Z"))
        : "Invalid NotBefore Attribute Value";
        assert(DateUtils.toUTCDateFormat(conditions.getNotOnOrAfter())
        .equals("2006-12-13T02:32:30Z"))
        : "Invalid NotBefore Attribute Value";
        
        List audResList = conditions.getAudienceRestrictions();
        AudienceRestriction audRes =
                (AudienceRestriction) audResList.iterator().next();
        List audList = audRes.getAudience();
        String audience = (String)audList.iterator().next();
        assert(audience.equals("sp.test.sun.com")) : "Incorrect Audience";
    }
    
    /**
     * Validates the <code>AuthenticationStatement</code> elements in
     * the assertion.
     *
     * @param authnSts a list of <code>AuthnStatement</code> objects
     */
    private void validateAuthnStatements(List authnSts) {
        AuthnStatement authSt = (AuthnStatement)authnSts.iterator().next();
        assert(DateUtils.toUTCDateFormat(authSt.getAuthnInstant())
        .equals("2005-12-13T02:15:53Z"))
        : "Invalid AuthInstant Attribute Value in AuthnStatement";
        
        assert(authSt.getSessionIndex()
        .equals("s298e4a33f2221bc95c7dc4ba0951eff83a8fcd124"))
        :"Invalid SessionIndex in AuthnStatement";
        
        String authCtxClassRef =
                authSt.getAuthnContext().getAuthnContextClassRef();
        assert(authCtxClassRef
                .equals(SAML2Constants. CLASSREF_PASSWORD_PROTECTED_TRANSPORT))
                : "Incorrect AuthnContext Class Reference in AuthnStatement.";
    }
    
    /**
     * Validates the <code>Advice</code> element in the Assertion.
     *
     * @param advice the <code>Advice</code> object.
     */
    private void validateAdvice(Advice advice) {
        String anyURI=
                (String) ((advice.getAdditionalInfo().iterator()).next());
        assert(
                anyURI.equals(
                "<SAMLAdvice name=\"http://www.test.com/assertionAdvice\"/>"))
                : "Invalid AssertionURIRef in Advice";
    }
    
    /**
     * Validates the Attribute Statement in the Assertion.
     *
     * @param attrStList a list of <code>AttributeStatement</code> objects.
     */
    private void validateAttributeStatement(List attrStList) {
        AttributeStatement attrSt =
                (AttributeStatement) (attrStList.iterator().next());
        Attribute attr = (Attribute) (attrSt.getAttribute().iterator().next());
        assert(attr.getName().equals("attr name")) : "Invalid Attribute Name ";
        
    }
}
