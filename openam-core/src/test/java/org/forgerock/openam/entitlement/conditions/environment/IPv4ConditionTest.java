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
 * Copyright 2014 ForgeRock AS.
 */

package org.forgerock.openam.entitlement.conditions.environment;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.forgerock.openam.utils.CollectionUtils.*;

public class IPv4ConditionTest extends IPvXConditionTest<Long> {

    @BeforeMethod
    public void setup() throws Exception {
        super.setup();
        ipMin = "0.0.0.0";
        ip1 = "0.0.0.1";
        ip2 = "0.0.0.2";
        ip9 = "0.0.0.9";
        ip10 = "0.0.0.10";
        ip15 = "0.0.0.15";
        ip20 = "0.0.0.20";
        ip21 = "0.0.0.21";
        ipMax = "255.255.255.255";
    }

    @Override
    protected IPvXCondition<Long> createCondition() {
        return new IPv4Condition();
    }

    // evaluation tests

    @Override
    @Test
    public void testIsNotSatisifiedIfIncorrectIpVersionIsUsedUnlessDnsNameMatches() throws Exception {
        // Given
        condition.setStartIpAndEndIp("0.0.0.0", "0.0.0.0");
        condition.setDnsName(asList("example.com"));

        // Then
        assertConditionDecision(false, "0000:0000:0000:0000:0000:0000:0000:0000", NO_DNS_NAME);
        // If DNS matches, it doesn't matter if the IP address version is incorrect:
        assertConditionDecision(true, "0000:0000:0000:0000:0000:0000:0000:0000", "example.com");
    }

    // config validation tests

    @Override
    @DataProvider(name = "validIpAddresses")
    public Object[][] validIpAddresses() {
        return new Object[][]{
                {"0.0.0.0"},         // minimum
                {"255.255.255.255"}, // maximum
                {"127.0.0.1"}        // loop-back
        };
    }

    @Override
    @DataProvider(name = "invalidIpAddresses")
    public Object[][] invalidIpAddresses() {
        return new Object[][]{
                {"not-a-number"},    // non-numeric
                {"0.0.0"},           // too few parts
                {"0.0.0.0.0"},       // too many parts
                {"0.0.0.256"},       // part outside of range
                {"0.0.256.0"},
                {"0.256.0.0"},
                {"256.0.0.0"},
                {"0000:0000:0000:0000:0000:0000:0000:0000"} // incorrect version
        };
    }

    // serialization / deserialization tests

}