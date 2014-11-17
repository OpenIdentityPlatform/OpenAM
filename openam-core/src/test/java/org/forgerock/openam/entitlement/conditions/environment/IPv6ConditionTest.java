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

import com.googlecode.ipv6.IPv6Address;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.forgerock.openam.utils.CollectionUtils.asList;

public class IPv6ConditionTest extends IPvXConditionTest<IPv6Address> {

    @BeforeMethod
    public void setup() throws Exception {
        super.setup();
        ipMin = "::";
        ip1 = "::1";
        ip2 = "::2";
        ip9 = "::9";
        ip10 = "::10";
        ip15 = "::15";
        ip20 = "::20";
        ip21 = "::21";
        ipMax = IPv6Address.MAX.toLongString();
    }

    @Override
    protected IPvXCondition<IPv6Address> createCondition() {
        return new IPv6Condition();
    }

    // evaluation tests

    @Override
    @Test
    public void testIsNotSatisifiedIfIncorrectIpVersionIsUsedUnlessDnsNameMatches() throws Exception {
        // Given
        condition.setStartIpAndEndIp("::1", "::1");
        condition.setDnsName(asList("example.com"));

        // Then
        assertConditionDecision(false, "127.0.0.1", NO_DNS_NAME);
        // If DNS matches, it doesn't matter if the IP address version is incorrect:
        assertConditionDecision(true, "127.0.0.1", "example.com");
    }

    // config validation tests

    @Override
    @DataProvider(name = "validIpAddresses")
    public Object[][] validIpAddresses() {
        return new Object[][]{
                {"0000:0000:0000:0000:0000:0000:0000:0000"},     // minimum (long form)
                {"::"},                                          // minimum (short form)
                {"ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff"},     // maximum (long form)
                {"::1"},                                         // loop-back (short form)
                {"::ffff:192.0.2.128"},                          // address mapped from IPv4
                {"2001:0db8:85a3:0000:0000:8a2e:0370:7334"},     // random example (long form)
                {"2001:0db8:85a3::8a2e:0370:7334"}               // random example (short form)
        };
    }

    @Override
    @DataProvider(name = "invalidIpAddresses")
    public Object[][] invalidIpAddresses() {
        return new Object[][]{
                {"not-a-number"},                                   // non-numeric
                {"0000:0000:0000:0000:0000:0000:0000"},             // too few parts (without using '::' abbreviation)
                {"0000:0000:0000:0000:0000:0000:0000:0000:0000"},   // too many parts
                {"ffff:ffff:ffff:ffff:ffff:ffff:ffff:fffg"},        // part outside of range
                {"127.0.0.1"}                                       // incorrect version
        };
    }

    // serialization / deserialization tests

}