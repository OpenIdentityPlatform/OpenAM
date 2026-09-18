/*
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
 * Copyright 2014-2015 ForgeRock AS.
 * Portions Copyrighted 2025-2026 3A Systems, LLC.
 */

package com.sun.identity.saml2.common;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.identity.saml2.jaxb.assertion.AttributeElement;
import com.sun.identity.saml2.jaxb.assertion.AttributeValueElement;
import com.sun.identity.saml2.jaxb.metadataattr.EntityAttributesElement;
import com.sun.identity.saml2.meta.SAML2MetaUtils;
import com.sun.identity.shared.encode.URLEncDec;
import jakarta.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import org.apache.commons.lang3.RandomStringUtils;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class SAML2UtilsTest {

    @Test
    public void encodeDecodeTest() {

        // the max length of each random String to encode
        int maxStringLength = 300;
        // the number of encode/decode iterations we want to test
        int randomStringsCount = 3000;
        Random R = new Random();

        int i = 0;
        while (i < randomStringsCount) {
            int size = R.nextInt(maxStringLength);
            // We don't want any 0 length arrays
            while (size == 0) {
                size = R.nextInt(maxStringLength);
            }
            i++;
            String randomString = RandomStringUtils.randomAlphanumeric(size);
            String encoded = SAML2Utils.encodeForRedirect(randomString);
            String decoded = SAML2Utils.decodeFromRedirect(URLEncDec.decode(encoded));
            assertThat(decoded).isEqualTo(randomString);
        }
    }

    @Test
    public void getMappedAttributesTest() {

        List<String> mappings = new ArrayList<>(6);

        mappings.add("invalid entry");
        mappings.add("name1=value");
        mappings.add("name2=\"static value\"");
        mappings.add("name3=\"static cn=value\"");
        mappings.add("urn:oasis:names:tc:SAML:2.0:attrname-format:uri|urn:mace:dir:attribute-def:name4=value");
        mappings.add("urn:oasis:names:tc:SAML:2.0:attrname-format:uri|name5=\"static value\"");

        Map<String, String> mappedAttributes = SAML2Utils.getMappedAttributes(mappings);

        assertThat(mappedAttributes).isNotNull().hasSize(5);
        assertThat(mappedAttributes).containsEntry("name1", "value");
        assertThat(mappedAttributes).containsEntry("name2", "\"static value\"");
        assertThat(mappedAttributes).containsEntry("name3", "\"static cn=value\"");
        assertThat(mappedAttributes).containsEntry("urn:oasis:names:tc:SAML:2.0:attrname-format:uri|urn:mace:dir:attribute-def:name4", "value");
        assertThat(mappedAttributes).containsEntry("urn:oasis:names:tc:SAML:2.0:attrname-format:uri|name5", "\"static value\"");
    }

    @Test
    public void getAttributeValueStringExtractsTextFromUnmarshalledValues() throws Exception {
        String authnContext = "urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport";
        String xml = "<mdattr:EntityAttributes"
                + " xmlns:mdattr=\"urn:oasis:names:tc:SAML:metadata:attribute\""
                + " xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\">"
                + "<saml:Attribute Name=\"idp-authncontext\""
                + " NameFormat=\"urn:oasis:names:tc:SAML:2.0:attrname-format:unspecified\">"
                + "<saml:AttributeValue>" + authnContext + "</saml:AttributeValue>"
                + "</saml:Attribute>"
                + "</mdattr:EntityAttributes>";

        EntityAttributesElement eael = (EntityAttributesElement) SAML2MetaUtils.convertStringToJAXB(xml);
        AttributeElement attribute = (AttributeElement) eael.getValue().getAttributeOrAssertion().get(0);
        AttributeValueElement value = (AttributeValueElement) attribute.getValue().getAttributeValue().get(0);

        // the anyType-bound payload is a DOM node after unmarshalling; the helper must
        // still return the text content the pre-JAXB3 code exposed directly
        assertThat(SAML2Utils.getAttributeValueString(value)).isEqualTo(authnContext);
    }

    @Test
    public void getAttributeValueStringHandlesPlainPayloads() {
        assertThat(SAML2Utils.getAttributeValueString(null)).isNull();

        QName name = new QName("urn:oasis:names:tc:SAML:2.0:assertion", "AttributeValue");
        JAXBElement<Object> plain = new JAXBElement<>(name, Object.class, "plain-value");
        assertThat(SAML2Utils.getAttributeValueString(plain)).isEqualTo("plain-value");

        JAXBElement<Object> empty = new JAXBElement<>(name, Object.class, null);
        assertThat(SAML2Utils.getAttributeValueString(empty)).isNull();
    }
}