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
 * Copyright © 2013-2014 ForgeRock AS. All rights reserved.
 */

package org.forgerock.openam.sts.config.user;

import org.forgerock.openam.sts.TokenType;
import org.testng.Assert;
import org.testng.annotations.Test;
import static org.testng.Assert.assertNotEquals;
import static org.testng.AssertJUnit.assertEquals;

import java.util.LinkedHashSet;
import java.util.Set;

public class DeploymentConfigTest {
    private static final String CLIENT_CERT = "client_cert";
    private final Set<String> tlsOffloadEngineHostIpAddrs;

    public DeploymentConfigTest() {
        tlsOffloadEngineHostIpAddrs = buildTLSOffloadHostIpSet();
    }

    @Test
    public void testEquals() {
        AuthTargetMapping atm = AuthTargetMapping.builder()
                .addMapping(TokenType.USERNAME, "module", "untmodule")
                .build();
        DeploymentConfig dc1 = DeploymentConfig.builder()
                .realm("a")
                .uriElement("b")
                .offloadedTwoWayTLSHeaderKey(CLIENT_CERT)
                .tlsOffloadEngineHostIpAddrs(tlsOffloadEngineHostIpAddrs)
                .authTargetMapping(atm)
                .build();

        DeploymentConfig dc2 = DeploymentConfig.builder()
                .realm("a")
                .uriElement("b")
                .offloadedTwoWayTLSHeaderKey(CLIENT_CERT)
                .tlsOffloadEngineHostIpAddrs(tlsOffloadEngineHostIpAddrs)
                .authTargetMapping(atm)
                .build();
        assertEquals(dc1, dc2);
        assertEquals(dc1.hashCode(), dc2.hashCode());
    }

    @Test
    public void testNotEquals() {
        AuthTargetMapping atm = AuthTargetMapping.builder()
                .addMapping(TokenType.USERNAME, "module", "untmodule")
                .build();
        DeploymentConfig dc1 = DeploymentConfig.builder()
                .realm("a")
                .uriElement("b")
                .authTargetMapping(atm)
                .build();

        DeploymentConfig dc2 = DeploymentConfig.builder()
                .realm("aa")
                .uriElement("b")
                .authTargetMapping(atm)
                .build();
        assertNotEquals(dc1, dc2);
        assertNotEquals(dc1.hashCode(), dc2.hashCode());
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void testRejectIfNull() {
        DeploymentConfig.builder()
                .realm("a")
                .uriElement("b")
                .build();
    }

    @Test
    public void testJsonRoundTrip() {
        AuthTargetMapping atm = AuthTargetMapping.builder()
                .addMapping(TokenType.USERNAME, "module", "untmodule")
                .build();
        DeploymentConfig rdc = DeploymentConfig.builder()
                .realm("a")
                .uriElement("b")
                .authTargetMapping(atm)
                .build();
        Assert.assertEquals(rdc, DeploymentConfig.fromJson(rdc.toJson()));

        atm = AuthTargetMapping.builder()
                .addMapping(TokenType.USERNAME, "module", "untmodule")
                .build();
        rdc = DeploymentConfig.builder()
                .realm("a")
                .uriElement("b")
                .authTargetMapping(atm)
                .offloadedTwoWayTLSHeaderKey(CLIENT_CERT)
                .tlsOffloadEngineHostIpAddrs(tlsOffloadEngineHostIpAddrs)
                .build();

        Assert.assertEquals(rdc, DeploymentConfig.fromJson(rdc.toJson()));
        Assert.assertEquals(rdc.getOffloadedTwoWayTlsHeaderKey(), CLIENT_CERT);
        Assert.assertEquals(rdc.getTlsOffloadEngineHostIpAddrs(), tlsOffloadEngineHostIpAddrs);
    }

    @Test
    public void testMapMarshalRoundTrip() {

        AuthTargetMapping atm = AuthTargetMapping.builder()
                .addMapping(TokenType.USERNAME, "module", "untmodule")
                .build();
        DeploymentConfig rdc = DeploymentConfig.builder()
                .realm("a")
                .uriElement("b")
                .authTargetMapping(atm)
                .build();

        assertEquals(rdc, DeploymentConfig.marshalFromAttributeMap(rdc.marshalToAttributeMap()));

        atm = AuthTargetMapping.builder()
                .addMapping(TokenType.USERNAME, "module", "untmodule")
                .addMapping(TokenType.X509, "module", "x509module")
                .build();
        rdc = DeploymentConfig.builder()
                .realm("a")
                .uriElement("b")
                .authTargetMapping(atm)
                .offloadedTwoWayTLSHeaderKey(CLIENT_CERT)
                .tlsOffloadEngineHostIpAddrs(tlsOffloadEngineHostIpAddrs)
                .build();
        assertEquals(rdc, DeploymentConfig.marshalFromAttributeMap(rdc.marshalToAttributeMap()));
        Assert.assertEquals(rdc.getOffloadedTwoWayTlsHeaderKey(), CLIENT_CERT);
        Assert.assertEquals(rdc.getTlsOffloadEngineHostIpAddrs(), tlsOffloadEngineHostIpAddrs);

        atm = AuthTargetMapping.builder()
                .addMapping(TokenType.USERNAME, "module", "untmodule")
                .addMapping(TokenType.X509, "module", "x509module")
                .build();
        rdc = DeploymentConfig.builder()
                .realm("a")
                .uriElement("b")
                .authTargetMapping(atm)
                .offloadedTwoWayTLSHeaderKey(CLIENT_CERT)
                .tlsOffloadEngineHostIpAddrs(tlsOffloadEngineHostIpAddrs)
                .build();

        assertEquals(rdc, DeploymentConfig.marshalFromAttributeMap(rdc.marshalToAttributeMap()));
        Assert.assertEquals(rdc.getOffloadedTwoWayTlsHeaderKey(), CLIENT_CERT);
        Assert.assertEquals(rdc.getTlsOffloadEngineHostIpAddrs(), tlsOffloadEngineHostIpAddrs);

    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void testHeaderSetButNoOffloadIps() {
        AuthTargetMapping atm = AuthTargetMapping.builder()
                .addMapping(TokenType.USERNAME, "module", "untmodule")
                .build();
        DeploymentConfig rdc = DeploymentConfig.builder()
                .realm("a")
                .uriElement("b")
                .offloadedTwoWayTLSHeaderKey(CLIENT_CERT)
                .authTargetMapping(atm)
                .build();
    }

    private Set<String> buildTLSOffloadHostIpSet() {
        LinkedHashSet<String> hostAddrs = new LinkedHashSet<String>(3);
        hostAddrs.add("15.23.44.32");
        hostAddrs.add("16.45.66.54");
        hostAddrs.add("15.88.67.88");
        return hostAddrs;
    }
}