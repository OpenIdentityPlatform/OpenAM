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
 * Copyright 2026 3A Systems LLC.
 */

package com.sun.identity.authentication.modules.msisdn;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;

import org.forgerock.opendj.ldap.Filter;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class MSISDNValidationTest {

    @Test
    public void shouldAcceptValidMsisdnValues() {
        for (String msisdn : new String[] {
                "1234567",
                "+1234567",
                "15551234567",
                "+15551234567",
                "123456789012345",
                "+123456789012345",
                "1234567890123456",
                "+1234567890123456"}) {
            assertThat(MSISDNValidation.isValidMsisdn(msisdn))
                    .as("MSISDN value <%s> should be accepted", msisdn)
                    .isTrue();
        }
    }

    @Test
    public void shouldRejectInvalidMsisdnValues() {
        for (String msisdn : new String[] {
                null,
                "",
                "1",
                "123456",
                "+123456",
                "+",
                "*",
                "1555*1234567",
                "1555)(123",
                "1555\\123",
                "1555\u0000123",
                "+1555+1234567",
                " 15551234567 "}) {
            assertThat(MSISDNValidation.isValidMsisdn(msisdn))
                    .as("MSISDN value <%s> should be rejected", msisdn)
                    .isFalse();
        }
    }

    @Test
    public void shouldEscapeAssertionValueWhenBuildingLdapFilter() {
        String filter = Filter.equality("sunIdentityMSISDNNumber", "*").toString();

        assertThat(filter).isNotEqualTo("(sunIdentityMSISDNNumber=*)");
        assertThat(filter.toLowerCase(Locale.ROOT)).contains("\\2a");
    }

    @Test
    public void shouldFailClosedWhenTrustedGatewayListIsEmpty() {
        assertThat(MSISDN.isTrustedGateway(Collections.emptySet(), "127.0.0.1")).isFalse();
    }

    @Test
    public void shouldTrustAnyGatewayOnlyWhenExplicitlyConfigured() {
        assertThat(MSISDN.isTrustedGateway(Collections.singleton("any"), "127.0.0.1")).isTrue();
    }

    @Test
    public void shouldTreatNoneAsDenyAllGatewayValue() {
        assertThat(MSISDN.isTrustedGateway(Collections.singleton("none"), "127.0.0.1")).isFalse();
        assertThat(MSISDN.isTrustedGateway(new HashSet<>(Arrays.asList("any", "none")), "127.0.0.1")).isFalse();
    }

    @Test
    public void shouldTrustExplicitGatewayAddress() {
        assertThat(MSISDN.isTrustedGateway(Collections.singleton("127.0.0.1"), "127.0.0.1")).isTrue();
        assertThat(MSISDN.isTrustedGateway(Collections.singleton("192.0.2.10"), "127.0.0.1")).isFalse();
    }
}
