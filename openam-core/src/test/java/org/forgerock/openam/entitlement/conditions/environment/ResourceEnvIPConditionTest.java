package org.forgerock.openam.entitlement.conditions.environment;/*
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

import com.iplanet.sso.SSOToken;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.shared.debug.Debug;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.security.auth.Subject;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class ResourceEnvIPConditionTest {

    private ResourceEnvIPCondition condition;
    private SSOToken token;
    private Subject subject;

    @BeforeMethod
    public void setUp() {
        Debug debug = mock(Debug.class);
        condition = new ResourceEnvIPCondition(debug);
        token = mock(SSOToken.class);
        subject = new Subject();
        subject.getPrivateCredentials().add(token);
    }

    @Test
    public void conditionStateShouldParseResourceEnvIPConditionValue() {

        //Given

        //When
        condition.setState("{\"resourceEnvIPConditionValue\": [\"IF IP=[127.0.0.1] THEN module=LDAP\"]}");

        //Then
        assertThat(condition.getResourceEnvIPConditionValue().iterator().next())
                .isEqualTo("IF IP=[127.0.0.1] THEN module=LDAP");
    }

    @Test
    public void conditionStateShouldContainAuthLevel() {

        //Given
        condition.setResourceEnvIPConditionValue(Collections.singleton("IF IP=[127.0.0.1] THEN module=LDAP"));

        //When
        String state = condition.getState();

        //Then
        assertThat(state).contains("\"resourceEnvIPConditionValue\":", "IF IP=[127.0.0.1] THEN module=LDAP");
    }

    @Test(expectedExceptions = EntitlementException.class)
    public void conditionShouldThrowEntitlementExceptionWhenEvaluatingInvalidValue() throws EntitlementException {

        //Given
        String realm = "REALM";
        String resourceName = "RESOURCE_NAME";
        Map<String, Set<String>> env = new HashMap<String, Set<String>>();
        condition.setState("{\"resourceEnvIPConditionValue\": [\"IF IP[127.0.0.1] THEN moduleLDAP\"]}");

        //When
        condition.evaluate(realm, subject, resourceName, env);

        //Then
        //Expected EntitlementException
    }
}
