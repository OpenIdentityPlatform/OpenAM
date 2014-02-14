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
 * information: "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright 2013-2014 ForgeRock AS. All rights reserved.
 */

package org.forgerock.openam.sts.soap.config.user;

import org.forgerock.openam.sts.AMSTSConstants;
import org.forgerock.openam.sts.AuthTargetMapping;
import org.forgerock.openam.sts.TokenType;
import org.forgerock.openam.sts.config.user.KeystoreConfig;
import org.testng.annotations.Test;

import javax.xml.namespace.QName;

import java.io.UnsupportedEncodingException;

import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertTrue;

/**
 */
public class SoapSTSInstanceConfigTest {
    @Test
    public void testEquals() throws UnsupportedEncodingException {
        SoapSTSInstanceConfig ric1 = createInstanceConfig("/bob", "http://localhost:8080/openam");
        SoapSTSInstanceConfig ric2 = createInstanceConfig("/bob", "http://localhost:8080/openam");
        assertTrue(ric1.equals(ric2));
        assertTrue(ric1.hashCode() == ric2.hashCode());
    }

    @Test
    public void testNotEquals() throws UnsupportedEncodingException {

        SoapSTSInstanceConfig ric1 = createInstanceConfig("/bob", "http://localhost:8080/openam");
        SoapSTSInstanceConfig ric2 = createInstanceConfig("/bobo", "http://localhost:8080/openam");
        assertFalse(ric1.equals(ric2));
        assertFalse(ric1.hashCode() == ric2.hashCode());

        SoapSTSInstanceConfig ric3 = createInstanceConfig("/bob", "http://localhost:8080/openam");
        SoapSTSInstanceConfig ric4 = createInstanceConfig("/bob", "http://localhost:8080/");
        assertFalse(ric3.equals(ric4));
        assertFalse(ric3.hashCode() == ric4.hashCode());

    }

    @Test(expectedExceptions = NullPointerException.class)
    public void testRejectIfNull() throws UnsupportedEncodingException {
        createIncompleteInstanceConfig();
    }

    private SoapSTSInstanceConfig createInstanceConfig(String uriElement, String amDeploymentUrl) throws UnsupportedEncodingException {
        AuthTargetMapping mapping = AuthTargetMapping.builder().build();

        DeploymentConfig deploymentConfig =
                DeploymentConfig.builder()
                        .portQName(new QName("port_namespace", "port_local"))
                        .serviceQName(new QName("service_namespace", "service_local"))
                        .wsdlLocation("wsdl_loc")
                        .realm("realm")
                        .uriElement(uriElement)
                        .authTargetMapping(mapping)
                        .build();

        KeystoreConfig keystoreConfig =
                KeystoreConfig.builder()
                        .fileName("stsstore.jks")
                        .password("stsspass".getBytes(AMSTSConstants.UTF_8_CHARSET_ID))
                        .encryptionKeyAlias("mystskey")
                        .signatureKeyAlias("mystskey")
                        .encryptionKeyPassword("stskpass".getBytes(AMSTSConstants.UTF_8_CHARSET_ID))
                        .signatureKeyPassword("stskpass".getBytes(AMSTSConstants.UTF_8_CHARSET_ID))
                        .build();

        return SoapSTSInstanceConfig.builder()
                .deploymentConfig(deploymentConfig)
                .amDeploymentUrl(amDeploymentUrl)
                .amJsonRestBase("/json")
                .amRestAuthNUriElement("/authenticate")
                .amRestLogoutUriElement("/sessions/?_action=logout")
                .amRestIdFromSessionUriElement("/users/?_action=idFromSession")
                .amSessionCookieName("iPlanetDirectoryPro")
                .keystoreConfig(keystoreConfig)
                .issuerName("Cornholio")
                .build();
    }

    private SoapSTSInstanceConfig createIncompleteInstanceConfig() throws UnsupportedEncodingException {
        //leave out the AuthTargetMapping

        DeploymentConfig deploymentConfig =
                DeploymentConfig.builder()
                        .uriElement("whatever")
                        .build();

        KeystoreConfig keystoreConfig =
                KeystoreConfig.builder()
                        .fileName("stsstore.jks")
                        .password("stsspass".getBytes(AMSTSConstants.UTF_8_CHARSET_ID))
                        .encryptionKeyAlias("mystskey")
                        .signatureKeyAlias("mystskey")
                        .encryptionKeyPassword("stskpass".getBytes(AMSTSConstants.UTF_8_CHARSET_ID))
                        .signatureKeyPassword("stskpass".getBytes(AMSTSConstants.UTF_8_CHARSET_ID))
                        .build();

        return SoapSTSInstanceConfig.builder()
                .deploymentConfig(deploymentConfig)
                .amDeploymentUrl("whatever")
                .amJsonRestBase("/json")
                .amRestAuthNUriElement("/authenticate")
                .amRestLogoutUriElement("/sessions/?_action=logout")
                .amRestIdFromSessionUriElement("/users/?_action=idFromSession")
                .amSessionCookieName("iPlanetDirectoryPro")
                .keystoreConfig(keystoreConfig)
                .issuerName("Cornholio")
                .build();
    }
}
