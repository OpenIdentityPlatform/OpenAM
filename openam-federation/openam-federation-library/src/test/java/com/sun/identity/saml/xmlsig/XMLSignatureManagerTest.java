/*
 * Copyright 2013 ForgeRock AS
 *
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 */

package com.sun.identity.saml.xmlsig;

import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.common.SAML2Utils;
import com.sun.identity.security.EncodeAction;
import com.sun.identity.shared.encode.URLEncDec;
import com.sun.identity.shared.xml.XMLUtils;
import org.forgerock.openam.utils.AMKeyProvider;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.security.AccessController;

public class XMLSignatureManagerTest {

    private static final String KEY_STORE_FILE = URLEncDec.decode(ClassLoader.getSystemResource("keystore.jks")
            .getFile());
    private static final String KEY_STORE_TYPE = "JKS";
    private static final String KEY_STORE_PASS = "testcase";
    private static final String DEFAULT_PRIVATE_KEY_PASS = "testcase";
    private static final String PRIVATE_KEY_PASS = "keypass";
    private static final String DEFAULT_PRIVATE_KEY_ALIAS = "defaultkey";
    private static final String PRIVATE_KEY_ALIAS = "privatekey";
    private static final String XML_DOCUMENT_TO_SIGN = "documenttosign.xml";
    private static final String SIGNED_XML_DOCUMENT_RESPONSEID = "signeddocument-responseid.xml";
    private static final String SIGNED_XML_DOCUMENT = "signeddocument.xml";

    private static final String ID_ATTRIBUTE_VALUE = "signme";
    private static final String RESPONSE_ID = "ResponseID";

    private XMLSignatureManager xmlSignatureManager;

    @BeforeClass
    public void setUp() {

        KeyProvider keyProvider =
                new AMKeyProvider(true, KEY_STORE_FILE, KEY_STORE_PASS, KEY_STORE_TYPE, DEFAULT_PRIVATE_KEY_PASS);
        SignatureProvider signatureProvider = new AMSignatureProvider();
        xmlSignatureManager = XMLSignatureManager.getInstance(keyProvider, signatureProvider);
    }

    @Test
    public void signXMLWithPrivateKeyUsingPassword() {

        Document documentToSign =
                XMLUtils.toDOMDocument(ClassLoader.getSystemResourceAsStream(XML_DOCUMENT_TO_SIGN), SAML2Utils.debug);
        Element signature = null;
        String encodedPrivatePass = AccessController.doPrivileged(new EncodeAction(PRIVATE_KEY_PASS));
        try {
            signature = xmlSignatureManager.signXMLUsingKeyPass(documentToSign, PRIVATE_KEY_ALIAS,
                    encodedPrivatePass, null, SAML2Constants.ID, ID_ATTRIBUTE_VALUE, true, null);
        } catch (XMLSignatureException e) {
            Assert.fail(e.getMessage());
        }

        Assert.assertNotNull(signature);
        NodeList nodes = documentToSign.getElementsByTagName("ds:Signature");
        Assert.assertTrue(nodes.getLength() > 0);
        Assert.assertTrue(signature.isEqualNode(nodes.item(0)));
    }

    @Test
    public void signXMLWithPrivateKeyAndNullPassword() {

        Document documentToSign =
                XMLUtils.toDOMDocument(ClassLoader.getSystemResourceAsStream(XML_DOCUMENT_TO_SIGN), SAML2Utils.debug);
        Element signature = null;
        try {
            signature = xmlSignatureManager.signXMLUsingKeyPass(documentToSign, PRIVATE_KEY_ALIAS,
                    null, null, SAML2Constants.ID, ID_ATTRIBUTE_VALUE, true, null);
            Assert.fail("Null private key exception expected.");
        } catch (XMLSignatureException e) {
        }

        Assert.assertNull(signature);
    }

    @Test
    public void signXMLWithPrivateKeyUsingDefaultPassword() {

        Document documentToSign =
                XMLUtils.toDOMDocument(ClassLoader.getSystemResourceAsStream(XML_DOCUMENT_TO_SIGN), SAML2Utils.debug);
        Element signature = null;
        try {
            signature = xmlSignatureManager.signXML(documentToSign, PRIVATE_KEY_ALIAS,
                    null, SAML2Constants.ID, ID_ATTRIBUTE_VALUE, true, null);
            Assert.fail("Null private key exception expected.");
        } catch (XMLSignatureException e) {
        }

        Assert.assertNull(signature);
    }

    @Test
    public void signXMLWithDefaultPrivateKeyAndNullPassword() {

        Document documentToSign =
                XMLUtils.toDOMDocument(ClassLoader.getSystemResourceAsStream(XML_DOCUMENT_TO_SIGN), SAML2Utils.debug);
        Element signature = null;
        try {
            // Passing null for password should trigger using default keystore password to load private key
            signature = xmlSignatureManager.signXMLUsingKeyPass(documentToSign, DEFAULT_PRIVATE_KEY_ALIAS,
                    null, null, SAML2Constants.ID, ID_ATTRIBUTE_VALUE, true, null);
        } catch (XMLSignatureException e) {
            Assert.fail(e.getMessage());
        }

        Assert.assertNotNull(signature);
        NodeList nodes = documentToSign.getElementsByTagName("ds:Signature");
        Assert.assertTrue(nodes.getLength() > 0);
        Assert.assertTrue(signature.isEqualNode(nodes.item(0)));
    }

    @Test
    public void signXMLWithDefaultPrivateKey() {

        Document documentToSign =
                XMLUtils.toDOMDocument(ClassLoader.getSystemResourceAsStream(XML_DOCUMENT_TO_SIGN), SAML2Utils.debug);
        Element signature = null;
        try {
            // Should trigger using default keystore password to load private key
            signature = xmlSignatureManager.signXML(documentToSign, DEFAULT_PRIVATE_KEY_ALIAS,
                    null, SAML2Constants.ID, ID_ATTRIBUTE_VALUE, true, null);
        } catch (XMLSignatureException e) {
            Assert.fail(e.getMessage());
        }

        Assert.assertNotNull(signature);
        NodeList nodes = documentToSign.getElementsByTagName("ds:Signature");
        Assert.assertTrue(nodes.getLength() > 0);
        Assert.assertTrue(signature.isEqualNode(nodes.item(0)));
    }

    @Test
    public void verifyDocumentResponseID() {

        // Test that a signed document can be verified with an ID
        // from the set of "AssertionID", "RequestID", "ResponseID"
        Document signedDocument =
                XMLUtils.toDOMDocument(ClassLoader.getSystemResourceAsStream(SIGNED_XML_DOCUMENT_RESPONSEID),
                        SAML2Utils.debug);
        boolean verified = false;
        try {
            verified = xmlSignatureManager.verifyXMLSignature(signedDocument.getDocumentElement(),
                    RESPONSE_ID, DEFAULT_PRIVATE_KEY_ALIAS);
        } catch (XMLSignatureException e) {
            Assert.fail(e.getMessage());
        }
        Assert.assertTrue(verified);
    }

    @Test
    public void verifyDocument() {

        // Test that a signed document can be verified
        Document signedDocument =
                XMLUtils.toDOMDocument(ClassLoader.getSystemResourceAsStream(SIGNED_XML_DOCUMENT), SAML2Utils.debug);
        boolean verified = false;
        try {
            verified = xmlSignatureManager.verifyXMLSignature(signedDocument.getDocumentElement(),
                    SAML2Constants.ID, DEFAULT_PRIVATE_KEY_ALIAS);
        } catch (XMLSignatureException e) {
            Assert.fail(e.getMessage());
        }
        Assert.assertTrue(verified);
    }
}
