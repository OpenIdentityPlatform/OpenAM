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
 * $Id: ProtocolTest.java,v 1.2 2008/06/25 05:48:25 qcheng Exp $
 *
 */

package com.sun.identity.saml.protocol;

import com.sun.identity.shared.xml.XMLUtils;
import com.sun.identity.saml.assertion.Assertion;
import com.sun.identity.saml.assertion.Statement;
import com.sun.identity.saml.assertion.AuthenticationStatement;
import com.sun.identity.saml.assertion.Subject;
import com.sun.identity.saml.assertion.SubjectConfirmation;
import com.sun.identity.saml.common.SAMLException;
import com.sun.identity.saml.common.SAMLUtils;
import com.sun.identity.shared.test.UnitTestBase;
import java.io.FileInputStream; 
import java.io.File; 
import java.io.FileReader;
import java.io.IOException;
import java.util.Set;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.Date;
import javax.xml.parsers.ParserConfigurationException;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class ProtocolTest extends UnitTestBase {
    public ProtocolTest() {
        super("FedLibrary-SAML");
    }
     
    @Test(groups = {"saml"})
    public void createArtifact()
        throws SAMLException, ParserConfigurationException, IOException, 
        SAXException 
    {
        entering("createArtifact", null);
        try {
            AssertionArtifact art =
                new AssertionArtifact(
                "AAHS4iwF9/rZI8hfxi0X0Puc2e6vebBZ/lJ0Vp/OIlH8RwVuiRlDizAx");
            AssertionArtifact art2 = 
                new  AssertionArtifact(art.getSourceID(),
                art.getAssertionHandle());
            assert (art2.toString() != null) : 
                "Error while construct the Artifact.";
            log(Level.INFO, "createArtifact",  art2.toString());
        } finally {
            exiting("createArtifact");
        }
    } 
    
    @Parameters({"request-filename"})
    @Test(groups = {"saml"})
    public void createRequest(String requestFile)
        throws SAMLException, ParserConfigurationException, IOException, 
        SAXException 
    {
        entering("createRequest", null);
        try {
            FileInputStream fis = new FileInputStream(new File(requestFile));
            Document doc = XMLUtils.toDOMDocument(fis, null);
            Element elt = doc.getDocumentElement();
	    Request req = new Request(elt);
            assert (req.getMajorVersion() == 1) : "wrong Major Version";
            assert (req.getMinorVersion() == 0) : "Wrong Minor Version";
            assert (req.getRequestID().equals("toDSAME-54321")) :
                "Wrong Provider ID";
            assert (req.toString(true, true) != null) :
                "Error in outputing XML string for the SAML Request";
            log(Level.INFO, "createRequest", req.toString(true, true));     
            req.setRequestID("newRequestID");
            req.setIssueInstant(new Date());
            req.addRespondWith("newRespondWith");
            assert (req.getRequestID().equals("newRequestID")) :
                "Wrong Request ID";
            log(Level.INFO, "createRequest", req.toString(true, true));   
        } finally {
            exiting("createRequest");
        }
    } 
      
    @Parameters({"response-filename"})
    @Test(groups = {"saml"})
    public void parseResponse(String responseFile)
        throws SAMLException, ParserConfigurationException, IOException, 
        SAXException 
    {
        entering("parseResponse", null);
        try {
            FileInputStream fis = new FileInputStream(new File(responseFile));
            Document doc = XMLUtils.toDOMDocument(fis, null);
            Element elt = doc.getDocumentElement();
	    Response resp = new Response(elt);
            assert (resp.getMajorVersion() == 1) : "wrong Major Version";
            assert (resp.getMinorVersion() == 0) : "Wrong Minor Version";
            assert (resp.getResponseID().equals("toSM-54321")) :
                "Wrong Response ID";
            assert (resp.toString(true, true) != null) :
                "Error in outputing XML string for the SAML Response";
            log(Level.INFO, "parseResponse", resp.toString(true, true));   
        } finally {
            exiting("parseResponse");
        }
    } 
    
    @Test(groups = {"saml"})
    public void createResponse()
        throws SAMLException, ParserConfigurationException, IOException, 
        SAXException 
    {
        entering("createResponse", null);
        try {   
            Status newStatus = new Status(new StatusCode("newStatusCodeValue"));
	    Response resp = new Response("newResponseID", "newResponseTo",
	        newStatus, null);
            AuthenticationStatement newStatement = 
                new AuthenticationStatement(
                "newAuthMethod", new Date(), 
                new Subject(new SubjectConfirmation("newConfirmationMethod")));
            Set<Statement> statementSet = new HashSet<Statement>();
            statementSet.add(newStatement);
            Assertion newAssertion = new Assertion("newAssertionID", 
                "newIssuer", new Date(), statementSet);
            resp.addAssertion(newAssertion);
            resp.setRecipient("newRecipient");
            assert (resp.getRecipient().equals("newRecipient")) :
                "Wrong Recipient";
            assert (resp.getResponseID().equals("newResponseID")) :
                "Wrong Response ID";
            assert (resp.toString(true, true) != null) :
                "Error in outputing XML string for the SAML Response";
            log(Level.INFO, "createResponse", resp.toString(true, true));   
        } finally {
            exiting("createResponse");
        }
    } 
}
