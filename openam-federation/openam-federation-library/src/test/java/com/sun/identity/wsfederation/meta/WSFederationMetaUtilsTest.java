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
 * Copyright 2026 3A Systems, LLC.
 */
package com.sun.identity.wsfederation.meta;

import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;

import com.sun.identity.wsfederation.jaxb.entityconfig.BaseConfigType;
import com.sun.identity.wsfederation.jaxb.entityconfig.FederationConfigElement;
import com.sun.identity.wsfederation.jaxb.wsfederation.FederationElement;
import com.sun.identity.wsfederation.jaxb.wsfederation.TokenSigningKeyInfoElement;
import com.sun.identity.wsfederation.jaxb.wsse.SecurityTokenReferenceElement;
import com.sun.identity.wsfederation.jaxb.xmlsig.X509DataElement;
import com.sun.identity.wsfederation.jaxb.xmlsig.X509DataType;
import com.sun.identity.shared.xml.XMLUtils;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Test
public class WSFederationMetaUtilsTest {

    public void tokenSigningCertificateFoundAfterUnmarshalling() throws Exception {
        byte[] certBytes = new byte[]{1, 2, 3, 4, 5};

        com.sun.identity.wsfederation.jaxb.wsfederation.ObjectFactory objFactory =
                new com.sun.identity.wsfederation.jaxb.wsfederation.ObjectFactory();
        com.sun.identity.wsfederation.jaxb.wsse.ObjectFactory secextObjFactory =
                new com.sun.identity.wsfederation.jaxb.wsse.ObjectFactory();
        com.sun.identity.wsfederation.jaxb.xmlsig.ObjectFactory dsObjectFactory =
                new com.sun.identity.wsfederation.jaxb.xmlsig.ObjectFactory();

        FederationElement fed =
                objFactory.createFederationElement(objFactory.createFederationType());
        fed.getValue().setFederationID("wsfed-test-entity");

        TokenSigningKeyInfoElement tski =
                objFactory.createTokenSigningKeyInfoElement(objFactory.createTokenKeyInfoType());
        SecurityTokenReferenceElement str = secextObjFactory
                .createSecurityTokenReferenceElement(secextObjFactory.createSecurityTokenReferenceType());
        X509DataElement x509Data =
                dsObjectFactory.createX509DataElement(dsObjectFactory.createX509DataType());
        X509DataType.X509Certificate x509Cert =
                dsObjectFactory.createX509DataTypeX509Certificate(certBytes);
        x509Data.getValue().getX509IssuerSerialOrX509SKIOrX509SubjectName().add(x509Cert);
        str.getValue().getAny().add(x509Data);
        tski.getValue().setSecurityTokenReference(str);
        fed.getValue().getAny().add(tski);

        // programmatically built tree
        assertThat(WSFederationMetaUtils.findTokenSigningCertificate(fed)).isEqualTo(certBytes);

        // round-trip through XML: lax any content unmarshals X509Data as its element wrapper,
        // which is the shape seen when reading remote WS-Fed IdP metadata
        Marshaller m = WSFederationMetaUtils.getMetaJAXBContext().createMarshaller();
        StringWriter sw = new StringWriter();
        m.marshal(fed, sw);
        FederationElement unmarshalled =
                (FederationElement) WSFederationMetaUtils.convertStringToJAXB(sw.toString());
        assertThat(WSFederationMetaUtils.findTokenSigningCertificate(unmarshalled)).isEqualTo(certBytes);
    }

    public void attributesAreReadFromUnmarshalledEntityConfig() throws Exception {
        String entityConfig =
                "<FederationConfig xmlns=\"urn:sun:fm:wsfederation:1.0:federationconfig\""
                + " FederationID=\"wsfed-test-entity\" hosted=\"true\">\n"
                + "    <IDPSSOConfig metaAlias=\"/idp\">\n"
                + "        <Attribute name=\"signingCertAlias\">\n"
                + "            <Value>test</Value>\n"
                + "        </Attribute>\n"
                + "        <Attribute name=\"cotlist\">\n"
                + "            <Value>cot1</Value>\n"
                + "            <Value>cot2</Value>\n"
                + "        </Attribute>\n"
                + "    </IDPSSOConfig>\n"
                + "</FederationConfig>";
        FederationConfigElement config =
                (FederationConfigElement) WSFederationMetaUtils.convertStringToJAXB(entityConfig);
        BaseConfigType idpConfig = config.getValue().getIDPSSOConfigOrSPSSOConfig().get(0).getValue();

        assertThat(WSFederationMetaUtils.getAttributes(idpConfig))
                .containsOnlyKeys("signingCertAlias", "cotlist")
                .containsEntry("signingCertAlias", Collections.singletonList("test"))
                .containsEntry("cotlist", Arrays.asList("cot1", "cot2"));
        assertThat(WSFederationMetaUtils.getAttributes(idpConfig, "cotlist")).containsExactly("cot1", "cot2");
    }

    /**
     * The JAXB 1 runtime rejected an entity config with an unexpected element; JAXB 3 drops the
     * element unless the unmarshaller fails on errors.
     */
    public void unexpectedElementIsRejected() throws Exception {
        String entityConfig =
                "<FederationConfig xmlns=\"urn:sun:fm:wsfederation:1.0:federationconfig\""
                + " FederationID=\"wsfed-test-entity\" hosted=\"true\">"
                + "<IDPSSOConfig metaAlias=\"/idp\"><Unexpected/></IDPSSOConfig>"
                + "</FederationConfig>";

        assertThatThrownBy(() -> WSFederationMetaUtils.convertStringToJAXB(entityConfig))
                .isInstanceOf(JAXBException.class);
        assertThatThrownBy(() -> WSFederationMetaUtils.convertInputStreamToJAXB(
                new ByteArrayInputStream(entityConfig.getBytes(StandardCharsets.UTF_8))))
                .isInstanceOf(JAXBException.class);
        assertThatThrownBy(() -> WSFederationMetaUtils.convertNodeToJAXB(
                XMLUtils.toDOMDocument(entityConfig, WSFederationMetaUtils.debug)))
                .isInstanceOf(JAXBException.class);
    }
}
