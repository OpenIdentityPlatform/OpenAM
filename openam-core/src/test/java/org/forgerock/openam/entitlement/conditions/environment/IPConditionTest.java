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
import org.json.JSONObject;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.security.auth.Subject;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import static org.fest.assertions.Assertions.*;
import static org.forgerock.openam.entitlement.conditions.environment.ConditionConstants.REQUEST_IP;
import static org.mockito.Mockito.*;
import static org.forgerock.openam.utils.CollectionUtils.*;

public class IPConditionTest {

    public static final String V6_BASE = "2001:0db8:85a3:0042:1000:8a2e:0370:733";
    public static final String V6_ADDRESS = V6_BASE+ "4";
    public static final String V6_INVALID = V6_BASE+ "2";
    public static final String V6_STATE = "{\"startIp\":\"" + V6_BASE + "3\",\"endIp\":\"" + V6_BASE + "5\"," +
            "\"dnsName\":[],\"ipRange\":[],\"ipVersion\":\"IPv6\"}";
    public static final String V4_BASE = "172.24.36.25";
    public static final String V4_ADDRESS = V4_BASE+ "4";
    public static final String V4_INVALID = V4_BASE+ "2";
    public static final String V4_STATE = "{\"startIp\":\"" + V4_BASE+ "3\",\"endIp\":\"" + V4_BASE + "5\"," +
            "\"dnsName\":[],\"ipRange\":[]}";
    private IPCondition condition;
    private SSOToken token;
    private Subject subject;

    @BeforeMethod
    public void setup() {
        token = mock(SSOToken.class);
        subject = new Subject();
        subject.getPrivateCredentials().add(token);
        condition = new IPCondition();
    }

    @Test
    public void testEvaluateSatisfied() throws Exception {
        // Given
        condition.setState(V4_STATE);
        Map<String, Set<String>> inRange = Collections.singletonMap(REQUEST_IP, asSet(V4_ADDRESS));

        // When
        ConditionDecision result = condition.evaluate("/", subject, "resource", inRange);

        // Then
        assertThat(result.isSatisfied()).isTrue();
    }

    @Test
    public void testEvaluateSatisfiedv6() throws Exception {
        // Given
        condition.setState(V6_STATE);
        Map<String, Set<String>> inRange = Collections.singletonMap(REQUEST_IP, asSet(V6_ADDRESS));

        // When
        ConditionDecision result = condition.evaluate("/", subject, "resource", inRange);

        // Then
        assertThat(result.isSatisfied()).isTrue();
    }

    @Test
    public void testEvaluateNotSatisfiedv6() throws Exception {
        // Given
        condition.setState(V6_STATE);
        Map<String, Set<String>> inRange = Collections.singletonMap(REQUEST_IP, asSet(V6_INVALID));

        // When
        ConditionDecision result = condition.evaluate("/", subject, "resource", inRange);

        // Then
        assertThat(result.isSatisfied()).isFalse();
    }

    @Test
    public void testDefaultsToIPv4() throws Exception {
        // Given
        condition.setState(V4_STATE);

        // When
        JSONObject o = new JSONObject(condition.getState());

        // Then
        assertThat(o.getString("ipVersion")).isEqualTo("IPv4");
    }

    @Test
    public void testEvaluateNotSatisfied() throws Exception {
        // Given
        condition.setState(V4_STATE);
        Map<String, Set<String>> outsideRange = Collections.singletonMap(REQUEST_IP, asSet(V4_INVALID));

        // When
        ConditionDecision result = condition.evaluate("/", subject, "resource", outsideRange);

        // Then
        assertThat(result.isSatisfied()).isFalse();
    }

    @Test
    public void testSetRangeWorksBeforeSetIpVersionV4() throws Exception {
        // Given
        Map<String, Set<String>> map = Collections.singletonMap(REQUEST_IP, asSet(V4_ADDRESS));
        condition.setStartIp(V4_ADDRESS);
        condition.setEndIp(V4_ADDRESS);

        // When
        condition.setIpRange(Arrays.asList(V4_ADDRESS + "-" + V4_ADDRESS));
        condition.setIpVersion("IPv4");
        ConditionDecision result = condition.evaluate("/", subject, "resource", map);

        // Then
        assertThat(result.isSatisfied()).isTrue();
    }

    @Test
    public void testSetRangeWorksBeforeSetIpVersionV6() throws Exception {
        // Given
        Map<String, Set<String>> map = Collections.singletonMap(REQUEST_IP, asSet(V6_ADDRESS));
        condition.setStartIp(V6_ADDRESS);
        condition.setEndIp(V6_ADDRESS);

        // When
        condition.setIpRange(Arrays.asList(V6_ADDRESS + "-" + V6_ADDRESS));
        condition.setIpVersion("IPv6");
        ConditionDecision result = condition.evaluate("/", subject, "resource", map);

        // Then
        assertThat(result.isSatisfied()).isTrue();
    }

    @Test
    public void testEvaluateDifferentVersion() throws Exception {
        // Given
        condition.setState(V4_STATE);
        Map<String, Set<String>> env = Collections.singletonMap(REQUEST_IP, asSet(V6_ADDRESS));

        // When
        ConditionDecision result = condition.evaluate("/", subject, "resource", env);

        // Then
        assertThat(result.isSatisfied()).isFalse();
    }
}