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

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.entitlement.ConditionDecision;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdType;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.openam.core.CoreWrapper;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.security.auth.Subject;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.openam.entitlement.conditions.environment.ConditionConstants.INVOCATOR_PRINCIPAL_UUID;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

public class AMIdentityMembershipConditionTest {

    private AMIdentityMembershipCondition condition;

    private CoreWrapper coreWrapper;
    private SSOToken adminToken;

    @BeforeMethod
    public void setUp() {

        Debug debug = mock(Debug.class);
        coreWrapper = mock(CoreWrapper.class);

        condition = new AMIdentityMembershipCondition(debug, coreWrapper);

        adminToken = mock(SSOToken.class);
        given(coreWrapper.getAdminToken()).willReturn(adminToken);
    }

    @Test
    public void conditionStateShouldParseProperties() {

        //Given

        //When
        condition.setState("{\"amIdentityName\": [\"IDENTITY_ONE\", \"IDENTITY_TWO\"]}");

        //Then
        assertThat(condition.getAmIdentityName()).containsOnly("IDENTITY_ONE", "IDENTITY_TWO");
    }

    @Test
    public void conditionStateShouldContainProperties() {

        //Given
        Set<String> amIdentityNames = new HashSet<String>();
        amIdentityNames.add("IDENTITY_ONE");
        amIdentityNames.add("IDENTITY_TWO");
        condition.setAmIdentityNames(amIdentityNames);

        //When
        String state = condition.getState();

        //Then
        assertThat(state).contains("\"amIdentityName\": [", "\"IDENTITY_ONE\"", "\"IDENTITY_TWO\"");
    }

    @Test
    public void conditionShouldEvaluateToFalseWhenInvocatorPrincipalNotSet() throws EntitlementException {

        //Given
        String realm = "REALM";
        Subject subject = new Subject();
        String resourceName = "RESOURCE_NAME";
        Map<String, Set<String>> env = new HashMap<String, Set<String>>();

        condition.setState("{\"amIdentityName\": [\"IDENTITY_ONE\", \"IDENTITY_TWO\"]}");

        //When
        ConditionDecision decision = condition.evaluate(realm, subject, resourceName, env);

        //Then
        assertThat(decision.isSatisfied()).isFalse();
        assertThat(decision.getAdvices()).isEmpty();
    }

    @Test
    public void conditionShouldEvaluateToFalseWhenInvocatorPrincipalIsEmpty() throws EntitlementException {

        //Given
        String realm = "REALM";
        Subject subject = new Subject();
        String resourceName = "RESOURCE_NAME";
        Map<String, Set<String>> env = new HashMap<String, Set<String>>();
        Set<String> invocatorUuids = new HashSet<String>();

        env.put(INVOCATOR_PRINCIPAL_UUID, invocatorUuids);
        condition.setState("{\"amIdentityName\": [\"IDENTITY_ONE\", \"IDENTITY_TWO\"]}");

        //When
        ConditionDecision decision = condition.evaluate(realm, subject, resourceName, env);

        //Then
        assertThat(decision.isSatisfied()).isFalse();
        assertThat(decision.getAdvices()).isEmpty();
    }

    @Test
    public void conditionShouldEvaluateToFalseWhenInvocatorPrincipalIsNull() throws EntitlementException {

        //Given
        String realm = "REALM";
        Subject subject = new Subject();
        String resourceName = "RESOURCE_NAME";
        Map<String, Set<String>> env = new HashMap<String, Set<String>>();

        env.put(INVOCATOR_PRINCIPAL_UUID, Collections.<String>singleton(null));
        condition.setState("{\"amIdentityName\": [\"IDENTITY_ONE\", \"IDENTITY_TWO\"]}");

        //When
        ConditionDecision decision = condition.evaluate(realm, subject, resourceName, env);

        //Then
        assertThat(decision.isSatisfied()).isFalse();
        assertThat(decision.getAdvices()).isEmpty();
    }

    @Test
    public void conditionShouldEvaluateToFalseWhenNoIdentitiesConfigured() throws EntitlementException {

        //Given
        String realm = "REALM";
        Subject subject = new Subject();
        String resourceName = "RESOURCE_NAME";
        Map<String, Set<String>> env = new HashMap<String, Set<String>>();

        env.put(INVOCATOR_PRINCIPAL_UUID, Collections.singleton("INVOCATOR_UUID"));

        //When
        ConditionDecision decision = condition.evaluate(realm, subject, resourceName, env);

        //Then
        assertThat(decision.isSatisfied()).isFalse();
        assertThat(decision.getAdvices()).isEmpty();
    }

    @Test
    public void conditionShouldEvaluateToFalseWhenInvocatorIdentityIsNull() throws EntitlementException,
            IdRepoException, SSOException {

        //Given
        String realm = "REALM";
        Subject subject = new Subject();
        String resourceName = "RESOURCE_NAME";
        Map<String, Set<String>> env = new HashMap<String, Set<String>>();

        env.put(INVOCATOR_PRINCIPAL_UUID, Collections.singleton("INVOCATOR_UUID"));
        condition.setState("{\"amIdentityName\": [\"IDENTITY_ONE\", \"IDENTITY_TWO\"]}");

        given(coreWrapper.getIdentity(adminToken, "INVOCATOR_UUID")).willReturn(null);

        //When
        ConditionDecision decision = condition.evaluate(realm, subject, resourceName, env);

        //Then
        assertThat(decision.isSatisfied()).isFalse();
        assertThat(decision.getAdvices()).isEmpty();
    }

    @Test
    public void conditionShouldEvaluateToFalseWhenConfiguredIdentityIsNull() throws EntitlementException,
            IdRepoException, SSOException {

        //Given
        String realm = "REALM";
        Subject subject = new Subject();
        String resourceName = "RESOURCE_NAME";
        Map<String, Set<String>> env = new HashMap<String, Set<String>>();
        AMIdentity invocatorIdentity = mock(AMIdentity.class);

        env.put(INVOCATOR_PRINCIPAL_UUID, Collections.singleton("INVOCATOR_UUID"));
        condition.setState("{\"amIdentityName\": [\"IDENTITY\"]}");

        given(coreWrapper.getIdentity(adminToken, "INVOCATOR_UUID")).willReturn(invocatorIdentity);
        given(coreWrapper.getIdentity(adminToken, "IDENTITY")).willReturn(null);

        //When
        ConditionDecision decision = condition.evaluate(realm, subject, resourceName, env);

        //Then
        assertThat(decision.isSatisfied()).isFalse();
        assertThat(decision.getAdvices()).isEmpty();
    }

    @Test
    public void conditionShouldEvaluateToTrueWhenUsingIdentitiesMatch() throws EntitlementException, IdRepoException,
            SSOException {

        //Given
        String realm = "REALM";
        Subject subject = new Subject();
        String resourceName = "RESOURCE_NAME";
        Map<String, Set<String>> env = new HashMap<String, Set<String>>();
        AMIdentity invocatorIdentity = mock(AMIdentity.class);
        AMIdentity identity = invocatorIdentity;

        env.put(INVOCATOR_PRINCIPAL_UUID, Collections.singleton("INVOCATOR_UUID"));
        condition.setState("{\"amIdentityName\": [\"IDENTITY\"]}");

        given(coreWrapper.getIdentity(adminToken, "INVOCATOR_UUID")).willReturn(invocatorIdentity);
        given(coreWrapper.getIdentity(adminToken, "IDENTITY")).willReturn(identity);

        //When
        ConditionDecision decision = condition.evaluate(realm, subject, resourceName, env);

        //Then
        assertThat(decision.isSatisfied()).isTrue();
        assertThat(decision.getAdvices()).isEmpty();
    }

    @Test
    public void conditionShouldEvaluateToFalseWhenConfiguredIdentityCannotHaveMembers() throws EntitlementException,
            IdRepoException, SSOException {

        //Given
        String realm = "REALM";
        Subject subject = new Subject();
        String resourceName = "RESOURCE_NAME";
        Map<String, Set<String>> env = new HashMap<String, Set<String>>();
        AMIdentity invocatorIdentity = mock(AMIdentity.class);
        AMIdentity identity = mock(AMIdentity.class);
        IdType invocatorIdType = mock(IdType.class);
        IdType identityIdType = mock(IdType.class);

        env.put(INVOCATOR_PRINCIPAL_UUID, Collections.singleton("INVOCATOR_UUID"));
        condition.setState("{\"amIdentityName\": [\"IDENTITY\"]}");

        given(coreWrapper.getIdentity(adminToken, "INVOCATOR_UUID")).willReturn(invocatorIdentity);
        given(coreWrapper.getIdentity(adminToken, "IDENTITY")).willReturn(identity);
        given(invocatorIdentity.getType()).willReturn(invocatorIdType);
        given(identity.getType()).willReturn(identityIdType);
        given(identityIdType.canHaveMembers()).willReturn(null);

        //When
        ConditionDecision decision = condition.evaluate(realm, subject, resourceName, env);

        //Then
        assertThat(decision.isSatisfied()).isFalse();
        assertThat(decision.getAdvices()).isEmpty();
    }

    @Test
    public void conditionShouldEvaluateToFalseWhenConfiguredIdentityHasEmptySetOfAllowedMemberTypes()
            throws EntitlementException, IdRepoException, SSOException {

        //Given
        String realm = "REALM";
        Subject subject = new Subject();
        String resourceName = "RESOURCE_NAME";
        Map<String, Set<String>> env = new HashMap<String, Set<String>>();
        AMIdentity invocatorIdentity = mock(AMIdentity.class);
        AMIdentity identity = mock(AMIdentity.class);
        IdType invocatorIdType = mock(IdType.class);
        IdType identityIdType = mock(IdType.class);

        env.put(INVOCATOR_PRINCIPAL_UUID, Collections.singleton("INVOCATOR_UUID"));
        condition.setState("{\"amIdentityName\": [\"IDENTITY\"]}");

        given(coreWrapper.getIdentity(adminToken, "INVOCATOR_UUID")).willReturn(invocatorIdentity);
        given(coreWrapper.getIdentity(adminToken, "IDENTITY")).willReturn(identity);
        given(invocatorIdentity.getType()).willReturn(invocatorIdType);
        given(identity.getType()).willReturn(identityIdType);
        given(identityIdType.canHaveMembers()).willReturn(Collections.emptySet());

        //When
        ConditionDecision decision = condition.evaluate(realm, subject, resourceName, env);

        //Then
        assertThat(decision.isSatisfied()).isFalse();
        assertThat(decision.getAdvices()).isEmpty();
    }

    @Test
    public void conditionShouldEvaluateToFalseWhenConfiguredIdentityCanHaveMembersButInvocatorIsNotAMember()
            throws EntitlementException, IdRepoException, SSOException {

        //Given
        String realm = "REALM";
        Subject subject = new Subject();
        String resourceName = "RESOURCE_NAME";
        Map<String, Set<String>> env = new HashMap<String, Set<String>>();
        AMIdentity invocatorIdentity = mock(AMIdentity.class);
        AMIdentity identity = mock(AMIdentity.class);
        IdType invocatorIdType = mock(IdType.class);
        IdType identityIdType = mock(IdType.class);

        env.put(INVOCATOR_PRINCIPAL_UUID, Collections.singleton("INVOCATOR_UUID"));
        condition.setState("{\"amIdentityName\": [\"IDENTITY\"]}");

        given(coreWrapper.getIdentity(adminToken, "INVOCATOR_UUID")).willReturn(invocatorIdentity);
        given(coreWrapper.getIdentity(adminToken, "IDENTITY")).willReturn(identity);
        given(invocatorIdentity.getType()).willReturn(invocatorIdType);
        given(identity.getType()).willReturn(identityIdType);
        given(identityIdType.canHaveMembers()).willReturn(Collections.singleton(invocatorIdType));
        given(invocatorIdentity.isMember(identity)).willReturn(false);

        //When
        ConditionDecision decision = condition.evaluate(realm, subject, resourceName, env);

        //Then
        assertThat(decision.isSatisfied()).isFalse();
        assertThat(decision.getAdvices()).isEmpty();
    }

    @Test
    public void conditionShouldEvaluateToTrueWhenConfiguredIdentityCanHaveMembersButInvocatorIsAMember()
            throws EntitlementException, IdRepoException, SSOException {

        //Given
        String realm = "REALM";
        Subject subject = new Subject();
        String resourceName = "RESOURCE_NAME";
        Map<String, Set<String>> env = new HashMap<String, Set<String>>();
        AMIdentity invocatorIdentity = mock(AMIdentity.class);
        AMIdentity identity = mock(AMIdentity.class);
        IdType invocatorIdType = mock(IdType.class);
        IdType identityIdType = mock(IdType.class);

        env.put(INVOCATOR_PRINCIPAL_UUID, Collections.singleton("INVOCATOR_UUID"));
        condition.setState("{\"amIdentityName\": [\"IDENTITY\"]}");

        given(coreWrapper.getIdentity(adminToken, "INVOCATOR_UUID")).willReturn(invocatorIdentity);
        given(coreWrapper.getIdentity(adminToken, "IDENTITY")).willReturn(identity);
        given(invocatorIdentity.getType()).willReturn(invocatorIdType);
        given(identity.getType()).willReturn(identityIdType);
        given(identityIdType.canHaveMembers()).willReturn(Collections.singleton(invocatorIdType));
        given(invocatorIdentity.isMember(identity)).willReturn(true);

        //When
        ConditionDecision decision = condition.evaluate(realm, subject, resourceName, env);

        //Then
        assertThat(decision.isSatisfied()).isTrue();
        assertThat(decision.getAdvices()).isEmpty();
    }
}
