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

package com.sun.identity.saml2.xmlsig;


import com.sun.identity.saml.xmlsig.KeyProvider;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2Utils;
import com.sun.identity.shared.encode.URLEncDec;
import com.sun.identity.shared.xml.XMLUtils;
import org.forgerock.openam.utils.AMKeyProvider;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class SigProviderTest {

    private static final String KEY_STORE_FILE = URLEncDec.decode(ClassLoader.getSystemResource("keystore.jks")
            .getFile());
    private static final String KEY_STORE_TYPE = "JKS";
    private static final String KEY_STORE_PASS = "testcase";
    private static final String DEFAULT_PRIVATE_KEY_PASS = "testcase";
    private static final String DEFAULT_PRIVATE_KEY_ALIAS = "defaultkey";
    private static final String XML_DOCUMENT_TO_SIGN = "documenttosign.xml";
    private static final String SIGNED_XML_DOCUMENT = "signeddocument.xml";
    private static final String ID_ATTRIBUTE_VALUE = "signme";

    private KeyProvider keyProvider = null;
    private SigProvider sigProvider = null;

    @BeforeClass
    public void setUp() {

        keyProvider = new AMKeyProvider(true, KEY_STORE_FILE, KEY_STORE_PASS, KEY_STORE_TYPE, DEFAULT_PRIVATE_KEY_PASS);
        sigProvider = SigManager.getSigInstance();
    }

    @Test
    public void testSigning() {

        String documentToSignXML = XMLUtils.print(
                    XMLUtils.toDOMDocument(ClassLoader.getSystemResourceAsStream(XML_DOCUMENT_TO_SIGN),
                            SAML2Utils.debug), "UTF-8");

        // Test the signing of an XML document
        Element signature = null;
        try {
            signature = sigProvider.sign(
                    documentToSignXML,
                    ID_ATTRIBUTE_VALUE,
                    keyProvider.getPrivateKey(DEFAULT_PRIVATE_KEY_ALIAS),
                    keyProvider.getX509Certificate(DEFAULT_PRIVATE_KEY_ALIAS));
        } catch (SAML2Exception e) {
            Assert.fail(e.getMessage());
        }
        Assert.assertNotNull(signature);
        NodeList nodes = signature.getOwnerDocument().getElementsByTagName("ds:Signature");
        Assert.assertTrue(nodes.getLength() > 0);
        Assert.assertTrue(signature.isEqualNode(nodes.item(0)));
    }

    @Test
    public void testVerifySignature() {

        String signedDocumentXML = XMLUtils.print(
                    XMLUtils.toDOMDocument(ClassLoader.getSystemResourceAsStream(SIGNED_XML_DOCUMENT),
                            SAML2Utils.debug), "UTF-8");

        // Verify that the signed document has a valid signature
        boolean verified = false;
        try {
            verified = sigProvider.verify(signedDocumentXML,
                    ID_ATTRIBUTE_VALUE, keyProvider.getX509Certificate(DEFAULT_PRIVATE_KEY_ALIAS));
        } catch (SAML2Exception e) {
            Assert.fail(e.getMessage());
        }
        Assert.assertTrue(verified);
    }
}