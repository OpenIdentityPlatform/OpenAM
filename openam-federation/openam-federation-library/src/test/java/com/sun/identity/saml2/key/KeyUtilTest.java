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
 * 
 * Portions copyright 2026 3A Systems LLC
 */

package com.sun.identity.saml2.key;

import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.common.SAML2Utils;
import com.sun.identity.saml2.meta.SAML2MetaException;
import com.sun.identity.saml2.meta.SAML2MetaUtils;
import com.sun.identity.saml2.jaxb.metadata.*;
import com.sun.identity.saml2.jaxb.xmlenc.EncryptionMethodType;
import com.sun.identity.shared.xml.XMLUtils;
import org.apache.xml.security.encryption.XMLCipher;
import java.io.InputStream;
import java.util.List;

import jakarta.xml.bind.JAXBElement;
import org.testng.Assert;
import org.testng.annotations.Test;

import jakarta.xml.bind.JAXBException;

public class KeyUtilTest {

    private static final String XML_DOCUMENT_TO_LOAD = "no-use-keydescriptor-metadata.xml";
    private static final String ENCRYPTION_KEYSIZE_METADATA = "sp-metadata-encryption-keysize.xml";
    private static final String ENCRYPTION_KEYSIZE_ENTITY_ID = "https://sp.example.com/encryption-keysize";

    /**
     * A test to verify fixing the case where an IDPs metadata contains at least two KeyDescriptor elements
     * without "use" attribute.
     */
    @Test
    public void testNoUseKeyDescriptorEntityDescriptor() throws SAML2MetaException, JAXBException {

        // Load test metadata
        String idpMetadata = XMLUtils.print(
                XMLUtils.toDOMDocument(ClassLoader.getSystemResourceAsStream(XML_DOCUMENT_TO_LOAD),
                    SAML2Utils.debug), "UTF-8");
        EntityDescriptorElement element = SAML2MetaUtils.getEntityDescriptorElement(idpMetadata);
        List<JAXBElement<? extends RoleDescriptorType>> descriptors = element.getValue().getRoleDescriptorOrIDPSSODescriptorOrSPSSODescriptor();
        for (JAXBElement<? extends RoleDescriptorType> descriptor : descriptors) {
            if (descriptor.getValue() instanceof IDPSSODescriptorType) {
                KeyDescriptorElement type = KeyUtil.getKeyDescriptor(descriptor.getValue(), "signing");
                Assert.assertNotNull(type);
                break;
            }
        }
    }

    /**
     * EncryptionMethod is mixed content, so text can come before the KeySize element; the KeySize
     * element must be found by type, not by index.
     */
    @Test
    public void encInfoReadsKeySizeFromIndentedEncryptionMethod() throws Exception {
        KeyUtil.clear();
        SPSSODescriptorType spsso = loadEncryptionKeySizeSPSSODescriptor();

        EncInfo encInfo = KeyUtil.getEncInfo(spsso, ENCRYPTION_KEYSIZE_ENTITY_ID, SAML2Constants.SP_ROLE);

        Assert.assertNotNull(encInfo);
        Assert.assertNotNull(encInfo.getWrappingKey());
        Assert.assertEquals(encInfo.getDataEncAlgorithm(), XMLCipher.AES_256);
        Assert.assertEquals(encInfo.getDataEncStrength(), 256);
    }

    @Test
    public void pepEncInfoReadsKeySizeFromIndentedEncryptionMethod() throws Exception {
        KeyUtil.clear();
        SPSSODescriptorType spsso = loadEncryptionKeySizeSPSSODescriptor();
        ObjectFactory objectFactory = new ObjectFactory();
        XACMLAuthzDecisionQueryDescriptorElement pepDescriptor = objectFactory
                .createXACMLAuthzDecisionQueryDescriptorElement(
                        objectFactory.createXACMLAuthzDecisionQueryDescriptorType());
        pepDescriptor.getValue().getKeyDescriptor().addAll(spsso.getKeyDescriptor());

        EncInfo encInfo = KeyUtil.getPEPEncInfo(pepDescriptor, ENCRYPTION_KEYSIZE_ENTITY_ID);

        Assert.assertNotNull(encInfo);
        Assert.assertEquals(encInfo.getDataEncAlgorithm(), XMLCipher.AES_256);
        Assert.assertEquals(encInfo.getDataEncStrength(), 256);
    }

    /**
     * The console shows the key size of a provider through this method.
     */
    @Test
    public void keySizeIsFoundAfterWhitespaceText() throws Exception {
        SPSSODescriptorType spsso = loadEncryptionKeySizeSPSSODescriptor();
        EncryptionMethodType em = KeyUtil.getKeyDescriptor(spsso, "encryption").getValue()
                .getEncryptionMethod().get(0).getValue();

        Assert.assertTrue(em.getContent().get(0) instanceof String);
        Assert.assertEquals(KeyUtil.getKeySize(em), 256);
    }

    /**
     * Loads the SP descriptor with a whitespace String in front of the KeySize element of each
     * EncryptionMethod. SAML2MetaUtils drops the formatting whitespace of mixed content, so put it back.
     */
    private SPSSODescriptorType loadEncryptionKeySizeSPSSODescriptor() throws Exception {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(ENCRYPTION_KEYSIZE_METADATA)) {
            EntityDescriptorElement element = (EntityDescriptorElement) SAML2MetaUtils.convertInputStreamToJAXB(is);
            for (JAXBElement<? extends RoleDescriptorType> descriptor
                    : element.getValue().getRoleDescriptorOrIDPSSODescriptorOrSPSSODescriptor()) {
                if (descriptor.getValue() instanceof SPSSODescriptorType) {
                    SPSSODescriptorType spsso = (SPSSODescriptorType) descriptor.getValue();
                    for (KeyDescriptorElement keyDescriptor : spsso.getKeyDescriptor()) {
                        for (EncryptionMethodElement em : keyDescriptor.getValue().getEncryptionMethod()) {
                            em.getValue().getContent().add(0, "\n                ");
                        }
                    }
                    return spsso;
                }
            }
        }
        throw new AssertionError("No SPSSODescriptor in " + ENCRYPTION_KEYSIZE_METADATA);
    }
}
