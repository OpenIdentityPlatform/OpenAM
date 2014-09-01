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
 * $Id: SignatureTest.java,v 1.2 2008/06/25 05:48:25 qcheng Exp $
 *
 */
/**
 * Portions Copyrighted 2012 ForgeRock Inc
 */
package com.sun.identity.saml.xmlsig;

import com.sun.identity.saml.assertion.Assertion;
import com.sun.identity.saml.common.SAMLException;
import com.sun.identity.saml.common.SAMLUtils;
import com.sun.identity.saml.xmlsig.XMLSignatureManager;
import com.sun.identity.shared.test.UnitTestBase;
import com.sun.identity.shared.xml.XMLUtils;
import java.io.File; 
import java.io.FileInputStream; 
import java.io.FileOutputStream; 
import java.io.FileReader;
import java.io.IOException;
import java.util.logging.Level;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class SignatureTest extends UnitTestBase {
    public SignatureTest() {
        super("FedLibrary-SAML");
    }
     
    @Parameters({"assertionsig-filename", "signature-filename"})
    @Test(groups = {"saml"})
    public void createSignature(String assertFile, String sigFile)
        throws SAMLException, ParserConfigurationException, IOException, 
        SAXException 
    {
        entering("createSignature", null);
        try {
            //build xml block 
            DocumentBuilder _documentBuilder = XMLUtils.getSafeDocumentBuilder(false);
            
            //Read xml file 
            InputSource is = new InputSource(new FileReader(assertFile));
            Document doc = _documentBuilder.parse(is);
            Element element = doc.getDocumentElement();     
            assert (element != null) : "Null input";      
            
            //Call signXML
            Assertion assertionInstance = new Assertion(element);
            assertionInstance.signXML();
            assert (assertionInstance.isSigned()) : "Failed in signing.";
            FileOutputStream f = new FileOutputStream(sigFile);
            f.write(assertionInstance.toString().getBytes()); 
            f.close();     
        } finally {
            exiting("createSignature");
        }
    }
    
    @Parameters({"signature-filename"})
    @Test(groups = {"saml"})
    public void verifySignature(String signatureFile)
        throws SAMLException, ParserConfigurationException, IOException, 
        SAXException 
    {   
        entering("verifySignature", null);
        try {
            DocumentBuilder _documentBuilder = XMLUtils.getSafeDocumentBuilder(false);
            InputSource is = new InputSource(new FileReader(signatureFile));
            Document doc = _documentBuilder.parse(is);
            Element element = doc.getDocumentElement();
            assert (element != null) : "Null input";      
            XMLSignatureManager manager = XMLSignatureManager.getInstance();
            boolean valid = manager.verifyXMLSignature(element, "AssertionID",
                null);
            assert valid : "Error while verifying signature.";
        } finally {
            exiting("verifySignature");
        }
    }    
}
