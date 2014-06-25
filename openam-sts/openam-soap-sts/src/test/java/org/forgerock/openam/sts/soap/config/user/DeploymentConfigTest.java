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

import org.forgerock.openam.sts.config.user.AuthTargetMapping;
import org.forgerock.openam.sts.TokenType;
import org.testng.annotations.Test;

import javax.xml.namespace.QName;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class DeploymentConfigTest {
    @Test
    public void testEquals() {
        AuthTargetMapping atm = AuthTargetMapping.builder()
                .addMapping(TokenType.USERNAME, "module", "untmodule")
                .build();
        DeploymentConfig dc1 = DeploymentConfig.builder()
                .realm("a")
                .uriElement("b")
                .wsdlLocation("wsdl_location")
                .serviceQName(new QName("service_namespace", "local_service"))
                .portQName(new QName("port_namespace", "local_port"))
                .authTargetMapping(atm)
                .build();

        DeploymentConfig dc2 = DeploymentConfig.builder()
                .realm("a")
                .uriElement("b")
                .wsdlLocation("wsdl_location")
                .serviceQName(new QName("service_namespace", "local_service"))
                .portQName(new QName("port_namespace", "local_port"))
                .authTargetMapping(atm)
                .build();
        assertTrue(dc1.equals(dc2));
        assertTrue(dc1.hashCode() == dc2.hashCode());
    }

    @Test
    public void testNotEquals() {
        AuthTargetMapping atm = AuthTargetMapping.builder()
                .addMapping(TokenType.USERNAME, "module", "untmodule")
                .build();
        DeploymentConfig dc1 = DeploymentConfig.builder()
                .realm("a")
                .uriElement("b")
                .wsdlLocation("wsdl_location")
                .serviceQName(new QName("service_namespace", "local_service"))
                .portQName(new QName("port_namespace", "local_port"))
                .authTargetMapping(atm)
                .build();

        DeploymentConfig dc2 = DeploymentConfig.builder()
                .realm("aa")
                .uriElement("b")
                .wsdlLocation("wsdl_location")
                .serviceQName(new QName("service_namespace", "local_service"))
                .portQName(new QName("port_namespace", "local_port"))
                .authTargetMapping(atm)
                .build();
        assertFalse(dc1.equals(dc2));
        assertFalse(dc1.hashCode() == dc2.hashCode());
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void testRejectIfNull() {
        AuthTargetMapping atm = AuthTargetMapping.builder()
                .addMapping(TokenType.USERNAME, "module", "untmodule")
                .build();

        DeploymentConfig dc3 = DeploymentConfig.builder()
                .realm("a")
                .uriElement("b")
                .authTargetMapping(atm)
                .build();

        DeploymentConfig dc4 = DeploymentConfig.builder()
                .realm("a")
                .uriElement("b")
                .build();
        assertFalse(dc3.equals(dc4));
    }
}
