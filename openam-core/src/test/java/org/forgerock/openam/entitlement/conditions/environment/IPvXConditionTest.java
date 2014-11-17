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
import org.codehaus.jackson.map.JsonMappingException;
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

import static org.fest.assertions.Assertions.assertThat;
import static org.forgerock.json.fluent.JsonValue.*;
import static org.forgerock.openam.entitlement.conditions.environment.ConditionConstants.REQUEST_DNS_NAME;
import static org.forgerock.openam.entitlement.conditions.environment.ConditionConstants.REQUEST_IP;
import static org.forgerock.openam.utils.CollectionUtils.asList;
import static org.forgerock.openam.utils.CollectionUtils.asSet;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

public abstract class IPvXConditionTest<T extends Comparable<T>> {

    protected static final String NO_IP_ADDRESS = null;
    protected static final String NO_DNS_NAME = null;
    protected String ipMin;
    protected String ip1;
    protected String ip2;
    protected String ip9;
    protected String ip10;
    protected String ip15;
    protected String ip20;
    protected String ip21;
    protected String ipMax;

    protected IPvXCondition<T> condition;
    protected SSOToken mockSsoToken;
    protected Subject subject;
    protected String ssoTokenIpAddress = null;

    @BeforeMethod
    public void setup() throws Exception {
        mockSsoToken = mock(SSOToken.class);
        subject = new Subject();
        subject.getPrivateCredentials().add(mockSsoToken);
        condition = createCondition();
    }

    protected abstract IPvXCondition<T> createCondition();

    // evaluation tests

    @Test
    public void testIsSatisfiedIfIpBetweenStartAndEndInclusive() throws Exception {
        // Given
        condition.setStartIpAndEndIp(ip10, ip20);

        // Then
        assertConditionDecision(false, ip9, NO_DNS_NAME);
        assertConditionDecision(true, ip10, NO_DNS_NAME);
        assertConditionDecision(true, ip15, NO_DNS_NAME);
        assertConditionDecision(true, ip20, NO_DNS_NAME);
        assertConditionDecision(false, ip21, NO_DNS_NAME);
    }

    @Test
    public void testIsSatisfiedIfIpWithinAnyRange() throws Exception {
        // Given
        /**
         * TODO: OPENAM-4942
         * According to the {@link org.forgerock.openam.entitlement.conditions.environment.ConditionConstants.IP_RANGE}
         * Javadoc it should be possible to set a range using only a start IP address.
         * This syntax is accepted but will not evaluate correctly.
         */
//        condition.setIpRange(asList(ip1, ip10 + "-" + ip20));
        condition.setIpRange(asList(ip1 + "-" + ip1, ip10 + "-" + ip20));

        // Then
        assertConditionDecision(false, ipMin, NO_DNS_NAME);
        assertConditionDecision(true, ip1, NO_DNS_NAME);
        assertConditionDecision(false, ip2, NO_DNS_NAME);

        assertConditionDecision(false, ip9, NO_DNS_NAME);
        assertConditionDecision(true, ip10, NO_DNS_NAME);
        assertConditionDecision(true, ip15, NO_DNS_NAME);
        assertConditionDecision(true, ip20, NO_DNS_NAME);
        assertConditionDecision(false, ip21, NO_DNS_NAME);
    }

    @Test
    public void testIsSatisfiedIfDnsNameMatchesExactly() throws Exception {
        // Given
        condition.setDnsName(asList("example.com", "example.net"));

        // Then
        assertConditionDecision(true, NO_IP_ADDRESS, "example.com");
        assertConditionDecision(true, NO_IP_ADDRESS, "example.net");
        assertConditionDecision(false, NO_IP_ADDRESS, "www.example.com");
    }

    @Test
    public void testIsSatisfiedIfDnsNameMatchesSubDomainWildcardPattern() throws Exception {
        // Given
        condition.setDnsName(asList("*.example.com", "*.example.net"));

        // Then
        assertConditionDecision(false, NO_IP_ADDRESS, "example.com");
        assertConditionDecision(true, NO_IP_ADDRESS, "www.example.com");
        assertConditionDecision(true, NO_IP_ADDRESS, "www.example.net");
    }

    @Test
    public void testIsSatisfiedIfDnsNameMatchesAnyDomainWildcardPattern() throws Exception {
        // Given
        condition.setDnsName(asList("*"));

        // Then
        assertConditionDecision(true, NO_IP_ADDRESS, "any.domain.com");
    }

    @Test
    public void testEvaluatesSsoTokenIpIfRequestIpNotProvided() throws Exception {
        // Given
        condition.setStartIpAndEndIp(ipMin, ipMin);
        givenSsoTokenIpAddress(ipMin);

        // Then
        assertConditionDecision(true, NO_IP_ADDRESS, NO_DNS_NAME);
        // However, the request IP is always preferred if available
        assertConditionDecision(false, ipMax, NO_DNS_NAME);
    }

    @Test
    public abstract void testIsNotSatisifiedIfIncorrectIpVersionIsUsedUnlessDnsNameMatches() throws Exception;

    protected void givenSsoTokenIpAddress(String ipAddress) throws Exception {
        InetAddress mockInetAddress = mock(InetAddress.class);
        given(mockSsoToken.getIPAddress()).willReturn(mockInetAddress);
        given(mockInetAddress.getHostAddress()).willReturn(ipAddress);
        this.ssoTokenIpAddress = ipAddress;
    }

    protected void assertConditionDecision(boolean satisified, String ipAddress, String dnsName)
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

    @DataProvider(name = "validIpAddresses")
    public abstract Object[][] validIpAddresses();

    @Test(dataProvider = "validIpAddresses")
    public void testValidIpAddressesAreAccepted(String validIpAddress) throws EntitlementException {
        // Given
        // an valid IP address

        // When
        condition.setStartIpAndEndIp(validIpAddress, validIpAddress);
        condition.setIpRange(asList(validIpAddress + "-" + validIpAddress));

        // Then
        assertThat(condition.getStartIp()).isEqualTo(validIpAddress);
        assertThat(condition.getEndIp()).isEqualTo(validIpAddress);
        assertThat(condition.getIpRange()).isEqualTo(asList(validIpAddress, validIpAddress));
    }

    @DataProvider(name = "invalidIpAddresses")
    public abstract Object[][] invalidIpAddresses();

    @DataProvider(name = "invalidIpAddressesAndEmptyString")
    public Object[][] invalidIpAddressesAndEmptyString() {
        Object[][] results = invalidIpAddresses();
        Object[][] extendedResults = new Object[results.length+1][];
        System.arraycopy(results, 0, extendedResults, 1, results.length);
        extendedResults[0] = new Object[]{""};
        return extendedResults;
    }

    @Test(dataProvider = "invalidIpAddressesAndEmptyString", expectedExceptions = EntitlementException.class)
    public void testStartIpMustBeValidIfSet(String invalidStartIp) throws EntitlementException {
        condition.setStartIpAndEndIp(invalidStartIp, ipMax);
    }

    @Test(dataProvider = "invalidIpAddressesAndEmptyString", expectedExceptions = EntitlementException.class)
    public void testEndIpMustBeValidIfSet(String invalidEndIp) throws EntitlementException {
        condition.setStartIpAndEndIp(ipMin, invalidEndIp);
    }

    @Test(expectedExceptions = EntitlementException.class)
    public void testStartIpCannotBeGreaterThanEndIp() throws EntitlementException {
        condition.setStartIpAndEndIp(ipMax, ipMin);
    }

    @Test(expectedExceptions = EntitlementException.class)
    public void testStartIpCannotBeSetWithoutEndIp() throws EntitlementException {
        condition.setStartIpAndEndIp(ipMax, null);
    }

    @Test(expectedExceptions = EntitlementException.class)
    public void testEndIpCannotBeSetWithoutStartIp() throws EntitlementException {
        condition.setStartIpAndEndIp(null, ipMax);
    }

    @Test(dataProvider = "invalidIpAddresses", expectedExceptions = EntitlementException.class)
    public void testStartIpOfRangeMustBeValidIfSet(String invalidStartIp) throws EntitlementException {
        condition.setIpRange(asList(invalidStartIp));
    }

    @Test(dataProvider = "invalidIpAddresses", expectedExceptions = EntitlementException.class)
    public void testEndIpOfRangeMustBeValidIfSet(String invalidEndIp) throws EntitlementException {
        condition.setIpRange(asList(ipMin + "-" + invalidEndIp));
    }

    // TODO: Introduce stricter validation?
//    @Test(expectedExceptions = EntitlementException.class)
//    public void testIpRangeStartIpCannotBeGreaterThanIpRangeEndIp() throws EntitlementException {
//        condition.setIpRange(asList(ipMax + "-" + ipMin));
//    }

    @DataProvider(name = "invalidDnsNames")
    public Object[][] invalidDnsNames() {
        return new Object[][]{
                {"example.*"},       // star wildcard must be first character if used
                {"*example.com"},    // star wildcard must be followed by dot
                {"*.example.*.com"}  // star wildcard can only be used once
        };
    }

    @Test(dataProvider = "invalidDnsNames", expectedExceptions = EntitlementException.class)
    public void testInvalidDnsNamesAreRejected(String invalidDnsName) throws EntitlementException {
        condition.setDnsName(asList(invalidDnsName));
    }

    @DataProvider(name = "validDnsNames")
    public Object[][] validDnsNames() {
        return new Object[][]{
                {"www.example.com"}, // explicit dns name
                {"*"},               // single star wildcard always first character if used
                {"*.example.com"},   // and always followed by dot '.' before further text
                {""},                // blank (other than wildcard checks, there's limited validation)
        };
    }

    @Test(dataProvider = "validDnsNames")
    public void testValidDnsNamesAreAccepted(String validDnsName) throws EntitlementException {
        condition.setDnsName(asList(validDnsName));
        assertThat(condition.getDnsName().get(0)).isEqualTo(validDnsName);
    }

    @Test
    public void testConfigurationParametersAreNotMutuallyExclusive() throws EntitlementException {
        // Given

        // When
        condition.setStartIpAndEndIp(ipMin, ipMax);
        condition.setIpRange(asList(ipMin + "-" + ipMax));
        condition.setDnsName(asList("example.com"));

        // Then
        assertThat(condition.getStartIp()).isEqualTo(ipMin);
        assertThat(condition.getEndIp()).isEqualTo(ipMax);
        assertThat(condition.getIpRange()).isEqualTo(asList(ipMin, ipMax));
        assertThat(condition.getDnsName()).isEqualTo(asList("example.com"));
    }

    // serialization / deserialization tests

    @Test
    public void testCanSerializeStateToJsonAndBack() throws EntitlementException {

        // Given
        condition.setStartIpAndEndIp(ipMin, ipMax);
        condition.setDnsName(asList("*.example.com", "*.example.net"));
        condition.setIpRange(asList(ipMin + "-" + ipMin, ipMax + "-" + ipMax));

        IPvXCondition copy = createCondition();

        // When
        copy.setState(condition.getState());

        // Then
        assertThat(copy.getStartIp()).isEqualTo(ipMin);
        assertThat(copy.getEndIp()).isEqualTo(ipMax);
        assertThat(copy.getDnsName()).isEqualTo(asList("*.example.com", "*.example.net"));
        assertThat(copy.getIpRange()).isEqualTo(asList(ipMin, ipMin, ipMax, ipMax));
    }

    @Test
    public void testJacksonJsonDeserializationIsFieldOrderAgnostic() throws IOException {

        // Given
        final String startIp = ipMin;
        final String endIp = ipMin;
        final String dnsName = "www.example.com";
        final ObjectMapper mapper = new ObjectMapper();

        // When
        final IPvXCondition ipCondition = mapper.readValue(
                json(object(
                        field("startIp", startIp),
                        field("endIp", endIp),
                        field("ipRange", array(startIp + "-" + endIp)),
                        field("dnsName", array(dnsName)))).toString(),
                condition.getClass());

        // Then
        assertThat(startIp).isEqualTo(ipCondition.getStartIp());
        assertThat(endIp).isEqualTo(ipCondition.getEndIp());
        assertThat(asList(startIp, endIp)).isEqualTo(ipCondition.getIpRange());
        assertThat(asList(dnsName)).isEqualTo(ipCondition.getDnsName());
    }

    @Test
    public void testJacksonJsonDeserializationCanAcceptMissingFields() throws IOException {

        // Given
        final String startIp = ipMin;
        final String endIp = ipMin;
        final ObjectMapper mapper = new ObjectMapper();

        // When
        final IPvXCondition ipCondition = mapper.readValue(
                json(object(
                        field("startIp", startIp),
                        field("endIp", endIp))).toString(),
                condition.getClass());

        // Then
        assertThat(startIp).isEqualTo(ipCondition.getStartIp());
        assertThat(endIp).isEqualTo(ipCondition.getEndIp());
        assertThat(ipCondition.getIpRange()).isEmpty();
        assertThat(ipCondition.getDnsName()).isEmpty();
    }

    @Test(expectedExceptions = JsonMappingException.class)
    public void testJacksonJsonDeserializationRejectsConfigurationIfNoDnsNameOrIpRangeDefined() throws Exception {
        // Given
        final ObjectMapper mapper = new ObjectMapper();
        final String emptyJsonObject = json(object()).toString();

        // When
        mapper.readValue(emptyJsonObject, condition.getClass());

        // Then
        // Expect Exception as least one of the following must be provided:
        // * ipStart and ipEnd
        // * ipRanges with at least one entry
        // * dnsName with at least one entry
    }

}