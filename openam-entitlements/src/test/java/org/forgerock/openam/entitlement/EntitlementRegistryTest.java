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

package org.forgerock.openam.entitlement;

import com.sun.identity.entitlement.EntitlementCombiner;
import com.sun.identity.entitlement.EntitlementCondition;
import com.sun.identity.entitlement.EntitlementSubject;
import com.sun.identity.entitlement.JwtClaimSubject;
import com.sun.identity.entitlement.NumericAttributeCondition;
import com.sun.identity.entitlement.ResourceAttribute;
import com.sun.identity.entitlement.StaticAttributes;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class EntitlementRegistryTest {
    private static final String COND_NAME = "mockCond";
    private static final String SUBJ_NAME = "mockSubj";
    private static final String ATTR_NAME = "mockAttr";
    private static final String COMBINER_NAME = "mockCombiner";

    private EntitlementRegistry testRegistry;


    @BeforeMethod
    public void setup() {
        testRegistry = new EntitlementRegistry();

        testRegistry.registerConditionType(COND_NAME, MockEntitlementCondition.class);
        testRegistry.registerSubjectType(SUBJ_NAME, MockEntitlementSubject.class);
        testRegistry.registerAttributeType(ATTR_NAME, MockResourceAttribute.class);
        testRegistry.registerDecisionCombiner(COMBINER_NAME, MockCombiner.class);
    }

    // Stub classes for testing
    abstract static class MockEntitlementCondition implements EntitlementCondition {}
    abstract static class MockEntitlementSubject implements EntitlementSubject {}
    abstract static class MockResourceAttribute implements ResourceAttribute {}
    abstract static class MockCombiner extends EntitlementCombiner {}

    @Test(expectedExceptions = NameAlreadyRegisteredException.class)
    public void shouldRejectDuplicateConditionNames() {
        testRegistry.registerConditionType(COND_NAME, EntitlementCondition.class);
    }

    @Test
    public void shouldAllowReregisteringTheSameConditionType() {
        testRegistry.registerConditionType(COND_NAME, MockEntitlementCondition.class);
    }

    @Test(expectedExceptions = NameAlreadyRegisteredException.class)
    public void shouldRejectDuplicateSubjectNames() {
        testRegistry.registerSubjectType(SUBJ_NAME, EntitlementSubject.class);
    }

    @Test
    public void shouldAllowReregisteringTheSameSubjectType() {
        testRegistry.registerSubjectType(SUBJ_NAME, MockEntitlementSubject.class);
    }

    @Test(expectedExceptions = NameAlreadyRegisteredException.class)
    public void shouldRejectDuplicateAttributeNames() {
        testRegistry.registerAttributeType(ATTR_NAME, ResourceAttribute.class);
    }

    @Test
    public void shouldAllowReregisteringTheSameAttributeType() {
        testRegistry.registerAttributeType(ATTR_NAME, MockResourceAttribute.class);
    }

    @Test(expectedExceptions = NameAlreadyRegisteredException.class)
    public void shouldRejectDuplicateCombinerNames() {
        testRegistry.registerDecisionCombiner(COMBINER_NAME, EntitlementCombiner.class);
    }

    @Test
    public void shouldAllowReregisteringTheSameCombinerType() {
        testRegistry.registerDecisionCombiner(COMBINER_NAME, MockCombiner.class);
    }

    @Test
    public void shouldGenerateShortNameForConditions() {
        // Given

        // When
        testRegistry.registerConditionType(MockEntitlementCondition.class);

        // Then
        assertThat(testRegistry.getConditionType("MockEntitlement")).isSameAs(MockEntitlementCondition.class);
    }

    @Test
    public void shouldGenerateShortNameForSubjects() {
        // Given

        // When
        testRegistry.registerSubjectType(MockEntitlementSubject.class);

        // Then
        assertThat(testRegistry.getSubjectType("MockEntitlement")).isSameAs(MockEntitlementSubject.class);
    }

    @Test
    public void shouldGenerateShortNameForAttributes() {
        // Given

        // When
        testRegistry.registerAttributeType(MockResourceAttribute.class);

        // Then
        assertThat(testRegistry.getAttributeType("MockResource")).isSameAs(MockResourceAttribute.class);
    }

    @Test
    public void shouldGenerateShortNameForCombiners() {
        // Given

        // When
        testRegistry.registerDecisionCombiner(MockCombiner.class);

        // Then
        assertThat(testRegistry.getCombinerType("MockCombiner")).isSameAs(MockCombiner.class);
    }

    @Test
    public void shouldReturnCorrectConditionName() {
        // Given
        String name = "test";
        EntitlementCondition testCondition = new NumericAttributeCondition();
        testRegistry.registerConditionType(EntitlementCondition.class);
        testRegistry.registerConditionType(name, NumericAttributeCondition.class);

        // When
        String result = testRegistry.getConditionName(testCondition);

        // Then
        assertThat(result).isEqualTo(name);
    }

    @Test
    public void shouldNotReturnSuperTypeConditionNames() {
        // Given
        EntitlementCondition testCondition = new NumericAttributeCondition();
        // Only super-type registered
        testRegistry.registerConditionType(EntitlementCondition.class);

        // When
        String result = testRegistry.getConditionName(testCondition);

        // Then
        assertThat(result).isNull();
    }

    @Test
    public void shouldReturnCorrectSubjectName() {
        // Given
        String name = "test";
        EntitlementSubject testSubject = new JwtClaimSubject();
        testRegistry.registerSubjectType(EntitlementSubject.class);
        testRegistry.registerSubjectType(name, JwtClaimSubject.class);

        // When
        String result = testRegistry.getSubjectName(testSubject);

        // Then
        assertThat(result).isEqualTo(name);
    }

    @Test
    public void shouldNotReturnSuperTypeSubjectNames() {
        // Given
        EntitlementSubject testSubject = new JwtClaimSubject();
        // Only super-type registered
        testRegistry.registerSubjectType(EntitlementSubject.class);

        // When
        String result = testRegistry.getSubjectName(testSubject);

        // Then
        assertThat(result).isNull();
    }

    @Test
    public void shouldReturnCorrectAttributeName() {
        // Given
        String name = "test";
        ResourceAttribute testAttribute = new StaticAttributes();
        testRegistry.registerAttributeType(ResourceAttribute.class);
        testRegistry.registerAttributeType(name, StaticAttributes.class);

        // When
        String result = testRegistry.getAttributeName(testAttribute);

        // Then
        assertThat(result).isEqualTo(name);
    }

    @Test
    public void shouldNotReturnSuperTypeAttributeNames() {
        // Given
        ResourceAttribute testAttribute = new StaticAttributes();
        // Only super-type registered
        testRegistry.registerAttributeType(ResourceAttribute.class);

        // When
        String result = testRegistry.getAttributeName(testAttribute);

        // Then
        assertThat(result).isNull();
    }
}