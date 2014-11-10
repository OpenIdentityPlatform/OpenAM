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

import com.iplanet.sso.SSOToken;
import com.sun.identity.entitlement.ConditionDecision;
import com.sun.identity.entitlement.EntitlementException;
import org.codehaus.jackson.map.ObjectMapper;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import javax.security.auth.Subject;

import java.io.IOException;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.fest.assertions.Assertions.*;
import static org.forgerock.json.fluent.JsonValue.array;
import static org.forgerock.json.fluent.JsonValue.field;
import static org.forgerock.json.fluent.JsonValue.json;
import static org.forgerock.json.fluent.JsonValue.object;
import static org.forgerock.openam.entitlement.conditions.environment.ConditionConstants.REQUEST_IP;
import static org.forgerock.openam.entitlement.conditions.environment.ConditionConstants.REQUEST_DNS_NAME;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.forgerock.openam.utils.CollectionUtils.*;

public class IPv4ConditionTest {

    private static final String NO_IP_ADDRESS = null;
    private static final String NO_DNS_NAME = null;

    private IPv4Condition condition;
    private SSOToken mockSsoToken;
    private Subject subject;
    private String ssoTokenIpAddress = null;

    @BeforeMethod
    public void setup() throws Exception {
        mockSsoToken = mock(SSOToken.class);
        subject = new Subject();
        subject.getPrivateCredentials().add(mockSsoToken);
        condition = new IPv4Condition();
    }

    // evaluation tests

    @Test
    public void testIsSatisfiedIfIpBetweenStartAndEndInclusive() throws Exception {
        // Given
        condition.setStartIp("192.168.0.1");
        condition.setEndIp("192.168.0.10");

        // Then
        assertConditionDecision(false, "192.168.0.0", NO_DNS_NAME);
        assertConditionDecision(true, "192.168.0.1", NO_DNS_NAME);
        assertConditionDecision(true, "192.168.0.5", NO_DNS_NAME);
        assertConditionDecision(true, "192.168.0.10", NO_DNS_NAME);
        assertConditionDecision(false, "192.168.0.11", NO_DNS_NAME);
    }

    @Test
    public void testIsSatisfiedIfIpWithinAnyRange() throws Exception {
        // Given
        /**
         * XXX: According to the {@link ConditionConstants.IP_RANGE} Javadoc it
         * should be possible to set a range using only a start IP address.
         * This syntax is accepted but will not evaluate correctly.
         */
//        condition.setIpRange(asList("192.168.0.1", "192.168.0.10-192.168.0.20"));
        condition.setIpRange(asList("192.168.0.1-192.168.0.1", "192.168.0.10-192.168.0.20"));

        // Then
        assertConditionDecision(false, "192.168.0.0", NO_DNS_NAME);
        assertConditionDecision(true, "192.168.0.1", NO_DNS_NAME);
        assertConditionDecision(false, "192.168.0.2", NO_DNS_NAME);

        assertConditionDecision(false, "192.168.0.9", NO_DNS_NAME);
        assertConditionDecision(true, "192.168.0.10", NO_DNS_NAME);
        assertConditionDecision(true, "192.168.0.20", NO_DNS_NAME);
        assertConditionDecision(false, "192.168.0.21", NO_DNS_NAME);
    }

    @Test
    public void testIsSatisfiedIfDnsNameMatchesExactly() throws Exception {
        // Given
        condition.setDnsName(asList("example.com", "example.net"));

        // Then
        /**
         * XXX: An IP address must be provided via environment
         */
//        assertConditionDecision(true, NO_IP_ADDRESS, "example.com");
//        assertConditionDecision(true, NO_IP_ADDRESS, "example.net");
//        assertConditionDecision(false, NO_IP_ADDRESS, "www.example.com");
        assertConditionDecision(true, "0.0.0.0", "example.com");
        assertConditionDecision(true, "0.0.0.0", "example.net");
        assertConditionDecision(false, "0.0.0.0", "www.example.com");
    }

    @Test
    public void testIsSatisfiedIfDnsNameMatchesWildcardPattern() throws Exception {
        // Given
        condition.setDnsName(asList("*.example.com", "*.example.net"));

        // Then
        /**
         * XXX: An IP address must be provided via environment
         */
//        assertConditionDecision(false, NO_IP_ADDRESS, "example.com");
//        assertConditionDecision(true, NO_IP_ADDRESS, "www.example.com");
//        assertConditionDecision(true, NO_IP_ADDRESS, "www.example.net");
        assertConditionDecision(false, "0.0.0.0", "example.com");
        assertConditionDecision(true, "0.0.0.0", "www.example.com");
        assertConditionDecision(true, "0.0.0.0", "www.example.net");
    }

    @Test
    public void testEvaluatesSsoTokenIpIfRequestIpNotProvided() throws Exception {
        // Given
        condition.setStartIp("192.168.0.1");
        condition.setEndIp("192.168.0.10");
        givenSsoTokenIpAddress("192.168.0.1");

        // Then
        /**
         * XXX: An IP address must be provided via environment
         */
//        assertConditionDecision(true, NO_IP_ADDRESS, NO_DNS_NAME);
        // The request IP is always preferred if available
        assertConditionDecision(false, "0.0.0.0", NO_DNS_NAME);
    }

    @Test
    public void testIsNotSatisifiedIfIncorrectIpVersionIsUsed() throws Exception {
        // Given
        condition.setStartIp("0.0.0.0");
        condition.setEndIp("0.0.0.0");

        // Then
        assertConditionDecision(false, "0000:0000:0000:0000:0000:0000:0000:0000", NO_DNS_NAME);
    }

    private void givenSsoTokenIpAddress(String ipAddress) throws Exception {
        InetAddress mockInetAddress = mock(InetAddress.class);
        given(mockSsoToken.getIPAddress()).willReturn(mockInetAddress);
        given(mockInetAddress.getHostAddress()).willReturn(ipAddress);
        this.ssoTokenIpAddress = ipAddress;
    }

    private void assertConditionDecision(boolean satisified, String ipAddress, String dnsName)
            throws EntitlementException {

        // Given
        Map<String, Set<String>> env = new HashMap<String, Set<String>>();
        if (ipAddress != null) {
            env.put(REQUEST_IP, asSet(ipAddress));
        }
        if (dnsName != null) {
            env.put(REQUEST_DNS_NAME, asSet(dnsName));
        }

        // When
        ConditionDecision result = condition.evaluate("/", subject, "resource", env);

        // Then
        String description = "\n\nEvaluating IPv4Condition:\n" + condition +
                "\n\nWith environment: " + env + "\nAnd SSOToken IP address: " + ssoTokenIpAddress;
        if (satisified) {
            assertThat(result.isSatisfied()).as(description).isTrue();
        } else {
            assertThat(result.isSatisfied()).as(description).isFalse();
        }
    }

    // config validation tests

    @DataProvider(name = "invalidIpAddresses")
    public Object[][] invalidIpAddresses() {
        return new Object[][]{
                {""},                // blank
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

    @Test(dataProvider = "invalidIpAddresses", expectedExceptions = EntitlementException.class)
    public void testStartIpMustBeValidIfSet(String invalidStartIp) throws EntitlementException {
        condition.setStartIp(invalidStartIp);
    }

    @Test(dataProvider = "invalidIpAddresses", expectedExceptions = EntitlementException.class)
    public void testEndIpMustBeValidIfSet(String invalidStartIp) throws EntitlementException {
        condition.setEndIp(invalidStartIp);
    }

//    // XXX: startIp > endIp is not prevented
//    @Test(expectedExceptions = EntitlementException.class)
//    public void testStartIpCannotBeGreaterThanEndIp() {
//        // Given
//        String invalidState = json(object(field("startIp", "0.0.0.1"), field("endIp", "0.0.0.0"))).toString();
//
//        // When
//        // XXX: Performing this validation in setStartIp and setEndIp could be messy; therefore,
//        //      this tests assumes that setState is a good place to put this inter-field validation.
//        //      However, if neither the old nor new EntitlementCondition classes are supported API,
//        //      then it would be better to replace setStartIp and setEndIp with setSingleIpRange(startIp, endIp).
//        //      Validation can then be added to this method rather than after JSON deserialization.
//        condition.setState(invalidState);
//
//        // Then
//        // expect EntitlementException
//    }

//    // XXX: ipRange IP addresses are not validated
//    @Test(dataProvider = "invalidIpAddresses", expectedExceptions = EntitlementException.class)
//    public void testStartIpOfRangeMustBeValidIfSet(String invalidStartIp) throws EntitlementException {
//        condition.setIpRange(asList(invalidStartIp + "-0.0.0.0"));
//    }

//    // XXX: ipRange IP addresses are not validated
//    @Test(dataProvider = "invalidIpAddresses", expectedExceptions = EntitlementException.class)
//    public void testEndIpOfRangeMustBeValidIfSet(String invalidStartIp) throws EntitlementException {
//        condition.setIpRange(asList("0.0.0.0-" + invalidStartIp));
//    }

//    // XXX: startIp > endIp is not prevented
//    @Test(expectedExceptions = EntitlementException.class)
//    public void testIpRangeStartIpCannotBeGreaterThanIpRangeEndIp() {
//        condition.setIpRange(asList("0.0.0.1-0.0.0.0"));
//    }

    @DataProvider(name = "invalidDnsNames")
    public Object[][] invalidDnsNames() {
        return new Object[][]{
                {"example.*"},       // star wildcard must be first character if used
                {"*example.com"},    // star wildcard must be followed by dot
                {"*.example.*.com"}  // star wildcard can only be used once
        };
    }

    @Test(dataProvider = "invalidDnsNames") //, expectedExceptions = EntitlementException.class)
    public void testInvalidDnsNamesAreRejected(String invalidDnsName) throws EntitlementException {
        String description = "invalid dns: \"" + invalidDnsName + "\"";
        assertThat(IPvXCondition.isValidDnsName(invalidDnsName)).as(description).isFalse();
//        // XXX: DNS names are not validated
//        condition.setDnsName(asList(invalidDnsName));
    }

    @DataProvider(name = "validDnsNames")
    public Object[][] validDnsNames() {
        return new Object[][]{
                {"www.example.com"}, // explicit dns name
                {"*.example.com"},   // single star wildcard used as first character and followed by dot
                {""},                // blank (other than wildcard checks, there's limited validation)
        };
    }

    @Test(dataProvider = "validDnsNames")
    public void testValidDnsNamesAreAccepted(String validDnsName) throws EntitlementException {
        condition.setDnsName(asList(validDnsName));
        assertThat(condition.getDnsName().get(0)).isEqualTo(validDnsName);
    }

    // XXX: Poor configuration is not identified until first call to evaluate
//    @Test(expectedExceptions = EntitlementException.class)
//    public void testRejectsConfigurationIfNoDnsNameOrIpRangeDefined() throws Exception {
//        // At least one of the following must be provided:
//        // * ipStart and ipEnd
//        // * ipRanges with at least one entry
//        // * dnsName with at least one entry
//        condition.setState("{}");
//    }

    @Test
    public void testConfigurationParametersAreNotMutuallyExclusive() throws EntitlementException {
        // Given

        // When
        condition.setStartIp("192.168.0.1");
        condition.setEndIp("192.168.0.10");
        condition.setIpRange(asList("192.168.0.20-192.168.0.22"));
        condition.setDnsName(asList("example.com"));

        // Then
        assertThat(condition.getStartIp()).isEqualTo("192.168.0.1");
        assertThat(condition.getEndIp()).isEqualTo("192.168.0.10");
        assertThat(condition.getIpRange()).isEqualTo(asList("192.168.0.20", "192.168.0.22"));
        assertThat(condition.getDnsName()).isEqualTo(asList("example.com"));
    }

    // serialization / deserialization tests

    @Test
    public void testCanSerializeStateToJsonAndBack() throws EntitlementException {

        // Given
        condition.setStartIp("192.168.0.1");
        condition.setEndIp("192.168.0.10");
        condition.setDnsName(asList("*.example.com", "*.example.net"));
        condition.setIpRange(asList("192.168.0.1-192.168.0.1", "192.168.0.10-192.168.0.20"));

        IPv4Condition copy = new IPv4Condition();

        // When
        copy.setState(condition.getState());

        // Then
        assertThat(copy.getStartIp()).isEqualTo("192.168.0.1");
        assertThat(copy.getEndIp()).isEqualTo("192.168.0.10");
        assertThat(copy.getDnsName()).isEqualTo(asList("*.example.com", "*.example.net"));
        assertThat(copy.getIpRange()).isEqualTo(asList("192.168.0.1", "192.168.0.1", "192.168.0.10", "192.168.0.20"));
    }

    @Test
    public void testJacksonJsonDeserializationIsFieldOrderAgnostic() throws IOException {

        // Given
        final String startIp = "0.0.0.0";
        final String endIp = "0.0.0.0";
        final String dnsName = "www.example.com";
        final ObjectMapper mapper = new ObjectMapper();

        // When
        final IPv4Condition ipCondition = mapper.readValue(
                json(object(
                        field("startIp", startIp),
                        field("endIp", endIp),
                        field("ipRange", array(startIp + "-" + endIp)),
                        field("dnsName", array(dnsName)))).toString(),
                IPv4Condition.class);

        // Then
        assertThat(startIp).isEqualTo(ipCondition.getStartIp());
        assertThat(endIp).isEqualTo(ipCondition.getEndIp());
        assertThat(asList(startIp, endIp)).isEqualTo(ipCondition.getIpRange());
        assertThat(asList(dnsName)).isEqualTo(ipCondition.getDnsName());
    }

    @Test
    public void testJacksonJsonDeserializationCanAcceptMissingFields() throws IOException {

        // Given
        final String startIp = "0.0.0.0";
        final String endIp = "0.0.0.0";
        final ObjectMapper mapper = new ObjectMapper();

        // When
        final IPv4Condition ipCondition = mapper.readValue(
                json(object(
                        field("startIp", startIp),
                        field("endIp", endIp))).toString(),
                IPv4Condition.class);

        // Then
        assertThat(startIp).isEqualTo(ipCondition.getStartIp());
        assertThat(endIp).isEqualTo(ipCondition.getEndIp());
        assertThat(ipCondition.getIpRange()).isEmpty();
        assertThat(ipCondition.getDnsName()).isEmpty();
    }

}