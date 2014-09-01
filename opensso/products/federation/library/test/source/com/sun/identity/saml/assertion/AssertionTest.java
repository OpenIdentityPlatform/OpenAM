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
 * $Id: AssertionTest.java,v 1.2 2008/06/25 05:48:24 qcheng Exp $
 *
 */
/**
 * Portions Copyrighted 2012 ForgeRock Inc
 */
package com.sun.identity.saml.assertion;

import com.sun.identity.shared.xml.XMLUtils;
import com.sun.identity.saml.common.SAMLException;
import com.sun.identity.shared.test.UnitTestBase;
import java.io.FileReader;
import java.io.IOException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import java.util.HashSet;
import java.io.FileInputStream; 
import java.io.File; 

public class AssertionTest extends UnitTestBase {
    public AssertionTest() {
        super("FedLibrary-SAML");
    }
    
    @Parameters({"assertion-filename"})
    @Test(groups = {"saml"})
    public void createAssertion(String assertionFile)
        throws SAMLException, ParserConfigurationException, IOException, 
        SAXException 
    {
        entering("createAssertion", null);
        try {
            DocumentBuilder _documentBuilder = XMLUtils.getSafeDocumentBuilder(false);
            InputSource is = new InputSource(new FileReader(assertionFile));
            Document doc = _documentBuilder.parse(is);
            Element element = doc.getDocumentElement();
            Assertion ass = new Assertion(element);
            assert ass.getIssuer().equals("https://test.identity.com");
        } finally {
            exiting("createAssertion");
        }
    }
    
    @Parameters({"subject-filename"})
    @Test(groups = {"saml"})
    public void createSubject(String subjectFile)
        throws SAMLException, ParserConfigurationException, IOException, 
        SAXException 
    {
        entering("createSubject", null);
	try {
	    NameIdentifier nid1 = 
	        new NameIdentifier("testuser@sun.com","sun.com","#emailAddress");
	    HashSet<String> confMethods = new HashSet<String>();
	    boolean addConf = confMethods.add(new String("one confirmation")); 
	    assert addConf;
	    SubjectConfirmation sconf1 = new SubjectConfirmation(confMethods);
	    String scDataString = 
	        "<saml:SubjectConfirmationData xmlns:saml=\"urn:oasis:names:tc:SAML:1.0:assertion\">test</saml:SubjectConfirmationData>";
	    Element scData = 
	        XMLUtils.toDOMDocument(scDataString, null).getDocumentElement();
       	    sconf1.setSubjectConfirmationData(scData);
       	    Subject sub1 = new Subject(nid1, sconf1);
       	    assert (sub1.toString(true, true) != null);
	    FileInputStream fis = new FileInputStream(new File(subjectFile));
            Document doc = XMLUtils.toDOMDocument(fis, null);
            Element elt = doc.getDocumentElement();
	    Subject sub2 = new Subject(elt);
            assert (sub1.equals(sub2) == false);
	} finally {
	    exiting("createSubject");
        }
   }
   
   @Parameters({"attributestatement-filename"})
   @Test(groups = {"saml"})
   public void createAttributeStatement(String attributeFile)
       throws SAMLException, ParserConfigurationException, IOException, 
       SAXException 
   {
       entering("createAttributeStatement", null);
       try { 
           FileInputStream fis = new FileInputStream(new File(attributeFile));
           Document doc = XMLUtils.toDOMDocument(fis, null);
           Element elt = doc.getDocumentElement();
	   AttributeStatement attrStatement = new AttributeStatement(elt);
           assert (attrStatement.getStatementType() == 3); 
           assert (attrStatement.toString() != null); 
       } finally {
	   exiting("createAttributeStatement");
       }
   }
}
