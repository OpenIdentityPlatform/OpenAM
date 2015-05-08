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

package org.forgerock.openam.upgrade.steps.policy.conditions;

import com.sun.identity.entitlement.AndCondition;
import com.sun.identity.entitlement.AndSubject;
import com.sun.identity.entitlement.EntitlementCondition;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.EntitlementSubject;
import com.sun.identity.entitlement.NotCondition;
import com.sun.identity.entitlement.NotSubject;
import com.sun.identity.entitlement.OrCondition;
import com.sun.identity.entitlement.OrSubject;
import com.sun.identity.entitlement.Privilege;
import com.sun.identity.entitlement.opensso.PolicyCondition;
import com.sun.identity.entitlement.opensso.PolicySubject;
import java.util.HashSet;
import java.util.Set;
import static org.fest.assertions.Assertions.assertThat;
import org.forgerock.openam.upgrade.UpgradeException;
import org.mockito.ArgumentCaptor;
import static org.mockito.BDDMockito.given;
import org.mockito.Matchers;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class PolicyConditionUpgraderTest {

    private PolicyConditionUpgrader conditionUpgrader;

    private PolicyConditionUpgradeMap conditionUpgradeMap;

    @BeforeMethod
    public void setUp() {
        conditionUpgradeMap = mock(PolicyConditionUpgradeMap.class);

        conditionUpgrader = new PolicyConditionUpgrader(conditionUpgradeMap);
    }

    @DataProvider(name = "isPolicyWithSingleSubjectAndEnvironmentConditionUpgradableDataProvider")
    private Object[][] isPolicyWithSingleSubjectAndEnvironmentConditionUpgradableDataProvider() {
        return new Object[][]{
                {null, false, null, false, true},
                {EntitlementSubject.class, false, EntitlementCondition.class, false, false},
                {PolicySubject.class, false, EntitlementCondition.class, false, false},
                {PolicySubject.class, true, EntitlementCondition.class, false, false},
                {EntitlementSubject.class, false, PolicyCondition.class, false, false},
                {EntitlementSubject.class, false, PolicyCondition.class, true, false},
                {PolicySubject.class, false, PolicyCondition.class, false, false},
                {PolicySubject.class, true, PolicyCondition.class, false, false},
                {PolicySubject.class, false, PolicyCondition.class, true, false},
                {PolicySubject.class, true, PolicyCondition.class, true, true},
        };
    }

    @Test(dataProvider = "isPolicyWithSingleSubjectAndEnvironmentConditionUpgradableDataProvider")
    public void isPolicyWithSingleSubjectAndEnvironmentConditionUpgradable(Class<? extends EntitlementSubject> sub,
            boolean subInMap, Class<? extends EntitlementCondition> con, boolean conInMap, boolean expectedResult) {

        //Given
        Privilege policy = mock(Privilege.class);
        EntitlementSubject subject = null;
        if (sub != null) {
            subject = mock(sub);
        }
        EntitlementCondition condition = null;
        if (con != null) {
            condition = mock(con);
        }

        given(policy.getSubject()).willReturn(subject);
        given(policy.getCondition()).willReturn(condition);
        if (subject instanceof PolicySubject) {
            given(((PolicySubject) subject).getClassName()).willReturn("SUBJECT_CLASS_NAME");
        }
        if (condition instanceof PolicyCondition) {
            given(((PolicyCondition) condition).getClassName()).willReturn("CONDITION_CLASS_NAME");
        }
        given(conditionUpgradeMap.containsSubjectCondition("SUBJECT_CLASS_NAME")).willReturn(subInMap);
        given(conditionUpgradeMap.containsEnvironmentCondition("CONDITION_CLASS_NAME")).willReturn(conInMap);

        //When
        boolean upgradable = conditionUpgrader.isPolicyUpgradable(policy);

        //Then
         assertThat(upgradable).isEqualTo(expectedResult);
    }

    @DataProvider(name = "isPolicyWithOrSubjectConditionUpgradableDataProvider")
    private Object[][] isPolicyWithOrSubjectConditionUpgradableDataProvider() {
        return new Object[][]{
                {EntitlementSubject.class, false, EntitlementSubject.class, false, false},
                {PolicySubject.class, false, EntitlementSubject.class, false, false},
                {PolicySubject.class, true, EntitlementSubject.class, false, false},
                {EntitlementSubject.class, false, PolicySubject.class, false, false},
                {EntitlementSubject.class, false, PolicySubject.class, true, false},
                {PolicySubject.class, false, PolicySubject.class, false, false},
                {PolicySubject.class, true, PolicySubject.class, false, false},
                {PolicySubject.class, false, PolicySubject.class, true, false},
                {PolicySubject.class, true, PolicySubject.class, true, true},
        };
    }

    @Test(dataProvider = "isPolicyWithOrSubjectConditionUpgradableDataProvider")
    public void isPolicyWithOrSubjectConditionUpgradable(Class<? extends EntitlementSubject> sub1, boolean sub1InMap,
            Class<? extends EntitlementSubject> sub2, boolean sub2InMap, boolean expectedResult) {

        //Given
        Privilege policy = mock(Privilege.class);
        OrSubject orSubject = mock(OrSubject.class);
        Set<EntitlementSubject> orSubjects = new HashSet<EntitlementSubject>();
        EntitlementSubject subject1 = mock(sub1);
        EntitlementSubject subject2 = mock(sub2);
        orSubjects.add(subject1);
        orSubjects.add(subject2);

        given(policy.getSubject()).willReturn(orSubject);
        given(orSubject.getESubjects()).willReturn(orSubjects);
        if (subject1 instanceof PolicySubject) {
            given(((PolicySubject) subject1).getClassName()).willReturn("SUBJECT1_CLASS_NAME");
        }
        if (subject2 instanceof PolicySubject) {
            given(((PolicySubject) subject2).getClassName()).willReturn("SUBJECT2_CLASS_NAME");
        }
        given(conditionUpgradeMap.containsSubjectCondition("SUBJECT1_CLASS_NAME")).willReturn(sub1InMap);
        given(conditionUpgradeMap.containsSubjectCondition("SUBJECT2_CLASS_NAME")).willReturn(sub2InMap);

        //When
        boolean upgradable = conditionUpgrader.isPolicyUpgradable(policy);

        //Then
        assertThat(upgradable).isEqualTo(expectedResult);
    }

    @DataProvider(name = "isPolicyWithAndSubjectConditionUpgradableDataProvider")
    private Object[][] isPolicyWithAndSubjectConditionUpgradableDataProvider() {
        return new Object[][]{
                {EntitlementSubject.class, false, EntitlementSubject.class, false, false},
                {PolicySubject.class, false, EntitlementSubject.class, false, false},
                {PolicySubject.class, true, EntitlementSubject.class, false, false},
                {EntitlementSubject.class, false, PolicySubject.class, false, false},
                {EntitlementSubject.class, false, PolicySubject.class, true, false},
                {PolicySubject.class, false, PolicySubject.class, false, false},
                {PolicySubject.class, true, PolicySubject.class, false, false},
                {PolicySubject.class, false, PolicySubject.class, true, false},
                {PolicySubject.class, true, PolicySubject.class, true, true},
        };
    }

    @Test(dataProvider = "isPolicyWithAndSubjectConditionUpgradableDataProvider")
    public void isPolicyWithAndSubjectConditionUpgradable(Class<? extends EntitlementSubject> sub1, boolean sub1InMap,
            Class<? extends EntitlementSubject> sub2, boolean sub2InMap, boolean expectedResult) {

        //Given
        Privilege policy = mock(Privilege.class);
        AndSubject andSubject = mock(AndSubject.class);
        Set<EntitlementSubject> andSubjects = new HashSet<EntitlementSubject>();
        EntitlementSubject subject1 = mock(sub1);
        EntitlementSubject subject2 = mock(sub2);
        andSubjects.add(subject1);
        andSubjects.add(subject2);

        given(policy.getSubject()).willReturn(andSubject);
        given(andSubject.getESubjects()).willReturn(andSubjects);
        if (subject1 instanceof PolicySubject) {
            given(((PolicySubject) subject1).getClassName()).willReturn("SUBJECT1_CLASS_NAME");
        }
        if (subject2 instanceof PolicySubject) {
            given(((PolicySubject) subject2).getClassName()).willReturn("SUBJECT2_CLASS_NAME");
        }
        given(conditionUpgradeMap.containsSubjectCondition("SUBJECT1_CLASS_NAME")).willReturn(sub1InMap);
        given(conditionUpgradeMap.containsSubjectCondition("SUBJECT2_CLASS_NAME")).willReturn(sub2InMap);

        //When
        boolean upgradable = conditionUpgrader.isPolicyUpgradable(policy);

        //Then
        assertThat(upgradable).isEqualTo(expectedResult);
    }

    @DataProvider(name = "isPolicyWithNotSubjectConditionUpgradableDataProvider")
    private Object[][] isPolicyWithNotSubjectConditionUpgradableDataProvider() {
        return new Object[][]{
                {EntitlementSubject.class, false, false},
                {PolicySubject.class, false, false},
                {PolicySubject.class, true, true},
        };
    }

    @Test(dataProvider = "isPolicyWithNotSubjectConditionUpgradableDataProvider")
    public void isPolicyWithNotSubjectConditionUpgradable(Class<? extends EntitlementSubject> sub, boolean subInMap,
            boolean expectedResult) {

        //Given
        Privilege policy = mock(Privilege.class);
        NotSubject notSubject = mock(NotSubject.class);
        Set<EntitlementSubject> notSubjects = new HashSet<EntitlementSubject>();
        EntitlementSubject subject = mock(sub);
        notSubjects.add(subject);

        given(policy.getSubject()).willReturn(notSubject);
        given(notSubject.getESubjects()).willReturn(notSubjects);
        if (subject instanceof PolicySubject) {
            given(((PolicySubject) subject).getClassName()).willReturn("SUBJECT_CLASS_NAME");
        }
        given(conditionUpgradeMap.containsSubjectCondition("SUBJECT_CLASS_NAME")).willReturn(subInMap);

        //When
        boolean upgradable = conditionUpgrader.isPolicyUpgradable(policy);

        //Then
        assertThat(upgradable).isEqualTo(expectedResult);
    }

    @DataProvider(name = "isPolicyWithOrEnvironmentConditionUpgradableDataProvider")
    private Object[][] isPolicyWithOrEnvironmentConditionUpgradableDataProvider() {
        return new Object[][]{
                {EntitlementCondition.class, false, EntitlementCondition.class, false, false},
                {PolicyCondition.class, false, EntitlementCondition.class, false, false},
                {PolicyCondition.class, true, EntitlementCondition.class, false, false},
                {EntitlementCondition.class, false, PolicyCondition.class, false, false},
                {EntitlementCondition.class, false, PolicyCondition.class, true, false},
                {PolicyCondition.class, false, PolicyCondition.class, false, false},
                {PolicyCondition.class, true, PolicyCondition.class, false, false},
                {PolicyCondition.class, false, PolicyCondition.class, true, false},
                {PolicyCondition.class, true, PolicyCondition.class, true, true},
        };
    }

    @Test(dataProvider = "isPolicyWithOrEnvironmentConditionUpgradableDataProvider")
    public void isPolicyWithOrEnvironmentConditionUpgradable(Class<? extends EntitlementCondition> con1,
            boolean con1InMap, Class<? extends EntitlementCondition> con2, boolean con2InMap, boolean expectedResult) {

        //Given
        Privilege policy = mock(Privilege.class);
        OrCondition orCondition = mock(OrCondition.class);
        Set<EntitlementCondition> orConditions = new HashSet<EntitlementCondition>();
        EntitlementCondition condition1 = mock(con1);
        EntitlementCondition condition2 = mock(con2);
        orConditions.add(condition1);
        orConditions.add(condition2);

        given(policy.getCondition()).willReturn(orCondition);
        given(orCondition.getEConditions()).willReturn(orConditions);
        if (condition1 instanceof PolicyCondition) {
            given(((PolicyCondition) condition1).getClassName()).willReturn("CONDITION1_CLASS_NAME");
        }
        if (condition2 instanceof PolicyCondition) {
            given(((PolicyCondition) condition2).getClassName()).willReturn("CONDITION2_CLASS_NAME");
        }
        given(conditionUpgradeMap.containsEnvironmentCondition("CONDITION1_CLASS_NAME")).willReturn(con1InMap);
        given(conditionUpgradeMap.containsEnvironmentCondition("CONDITION2_CLASS_NAME")).willReturn(con2InMap);

        //When
        boolean upgradable = conditionUpgrader.isPolicyUpgradable(policy);

        //Then
        assertThat(upgradable).isEqualTo(expectedResult);
    }

    @DataProvider(name = "isPolicyWithAndEnvironmentConditionUpgradableDataProvider")
    private Object[][] isPolicyWithAndEnvironmentConditionUpgradableDataProvider() {
        return new Object[][]{
                {EntitlementCondition.class, false, EntitlementCondition.class, false, false},
                {PolicyCondition.class, false, EntitlementCondition.class, false, false},
                {PolicyCondition.class, true, EntitlementCondition.class, false, false},
                {EntitlementCondition.class, false, PolicyCondition.class, false, false},
                {EntitlementCondition.class, false, PolicyCondition.class, true, false},
                {PolicyCondition.class, false, PolicyCondition.class, false, false},
                {PolicyCondition.class, true, PolicyCondition.class, false, false},
                {PolicyCondition.class, false, PolicyCondition.class, true, false},
                {PolicyCondition.class, true, PolicyCondition.class, true, true},
        };
    }

    @Test(dataProvider = "isPolicyWithAndEnvironmentConditionUpgradableDataProvider")
    public void isPolicyWithAndEnvironmentConditionUpgradable(Class<? extends EntitlementCondition> con1,
            boolean con1InMap, Class<? extends EntitlementCondition> con2, boolean con2InMap, boolean expectedResult) {

        //Given
        Privilege policy = mock(Privilege.class);
        AndCondition andCondition = mock(AndCondition.class);
        Set<EntitlementCondition> andConditions = new HashSet<EntitlementCondition>();
        EntitlementCondition condition1 = mock(con1);
        EntitlementCondition condition2 = mock(con2);
        andConditions.add(condition1);
        andConditions.add(condition2);

        given(policy.getCondition()).willReturn(andCondition);
        given(andCondition.getEConditions()).willReturn(andConditions);
        if (condition1 instanceof PolicyCondition) {
            given(((PolicyCondition) condition1).getClassName()).willReturn("CONDITION1_CLASS_NAME");
        }
        if (condition2 instanceof PolicyCondition) {
            given(((PolicyCondition) condition2).getClassName()).willReturn("CONDITION2_CLASS_NAME");
        }
        given(conditionUpgradeMap.containsEnvironmentCondition("CONDITION1_CLASS_NAME")).willReturn(con1InMap);
        given(conditionUpgradeMap.containsEnvironmentCondition("CONDITION2_CLASS_NAME")).willReturn(con2InMap);

        //When
        boolean upgradable = conditionUpgrader.isPolicyUpgradable(policy);

        //Then
        assertThat(upgradable).isEqualTo(expectedResult);
    }

    @DataProvider(name = "isPolicyWithNotEnvironmentConditionUpgradableDataProvider")
    private Object[][] isPolicyWithNotEnvironmentConditionUpgradableDataProvider() {
        return new Object[][]{
                {EntitlementCondition.class, false, false},
                {PolicyCondition.class, false, false},
                {PolicyCondition.class, true, true},
        };
    }

    @Test(dataProvider = "isPolicyWithNotEnvironmentConditionUpgradableDataProvider")
    public void isPolicyWithNotEnvironmentConditionUpgradable(Class<? extends EntitlementCondition> condition,
            boolean conditionInMap, boolean expectedResult) {

        //Given
        Privilege policy = mock(Privilege.class);
        NotCondition notCondition = mock(NotCondition.class);
        Set<EntitlementCondition> notConditions = new HashSet<EntitlementCondition>();
        EntitlementCondition con = mock(condition);
        notConditions.add(con);

        given(policy.getCondition()).willReturn(notCondition);
        given(notCondition.getEConditions()).willReturn(notConditions);
        if (con instanceof PolicyCondition) {
            given(((PolicyCondition) con).getClassName()).willReturn("CONDITION_CLASS_NAME");
        }
        given(conditionUpgradeMap.containsEnvironmentCondition("CONDITION_CLASS_NAME")).willReturn(conditionInMap);

        //When
        boolean upgradable = conditionUpgrader.isPolicyUpgradable(policy);

        //Then
        assertThat(upgradable).isEqualTo(expectedResult);
    }

    @Test
    public void shouldMigratePolicyWithSingleSubjectAndEnvironmentCondition() throws EntitlementException,
            UpgradeException {

        //Given
        Privilege policy = mock(Privilege.class);
        PolicySubject subject = mock(PolicySubject.class);
        PolicyCondition condition = mock(PolicyCondition.class);
        EntitlementSubject migratedSubject = mock(EntitlementSubject.class);
        EntitlementCondition migratedCondition = mock(EntitlementCondition.class);

        given(policy.getSubject()).willReturn(subject);
        given(policy.getCondition()).willReturn(condition);
        given(subject.getClassName()).willReturn("SUBJECT_CLASS_NAME");
        given(condition.getClassName()).willReturn("CONDITION_CLASS_NAME");

        given(conditionUpgradeMap.migrateSubjectCondition(eq("SUBJECT_CLASS_NAME"), eq(subject),
                Matchers.<MigrationReport>anyObject())).willReturn(migratedSubject);
        given(conditionUpgradeMap.migrateEnvironmentCondition(eq("CONDITION_CLASS_NAME"), eq(condition),
                Matchers.<MigrationReport>anyObject())).willReturn(migratedCondition);

        //When
        conditionUpgrader.dryRunPolicyUpgrade(policy);

        //Then
        ArgumentCaptor<EntitlementSubject> subjectCaptor = ArgumentCaptor.forClass(EntitlementSubject.class);
        verify(policy).setSubject(subjectCaptor.capture());
        assertThat(subjectCaptor.getValue()).isEqualTo(migratedSubject);
        ArgumentCaptor<EntitlementCondition> conditionCaptor = ArgumentCaptor.forClass(EntitlementCondition.class);
        verify(policy).setCondition(conditionCaptor.capture());
        assertThat(conditionCaptor.getValue()).isEqualTo(migratedCondition);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldMigratePolicyWithOrSubjectCondition() throws EntitlementException, UpgradeException {

        //Given
        Privilege policy = mock(Privilege.class);
        OrSubject orSubject = mock(OrSubject.class);
        Set<EntitlementSubject> orSubjects = new HashSet<EntitlementSubject>();
        PolicySubject subject1 = mock(PolicySubject.class);
        PolicySubject subject2 = mock(PolicySubject.class);
        orSubjects.add(subject1);
        orSubjects.add(subject2);
        EntitlementSubject migratedSubject1 = mock(EntitlementSubject.class);
        EntitlementSubject migratedSubject2 = mock(EntitlementSubject.class);

        given(policy.getSubject()).willReturn(orSubject);
        given(orSubject.getESubjects()).willReturn(orSubjects);
        given(subject1.getClassName()).willReturn("SUBJECT1_CLASS_NAME");
        given(subject2.getClassName()).willReturn("SUBJECT2_CLASS_NAME");
        given(conditionUpgradeMap.migrateSubjectCondition(eq("SUBJECT1_CLASS_NAME"), eq(subject1),
                Matchers.<MigrationReport>anyObject())).willReturn(migratedSubject1);
        given(conditionUpgradeMap.migrateSubjectCondition(eq("SUBJECT2_CLASS_NAME"), eq(subject2),
                Matchers.<MigrationReport>anyObject())).willReturn(migratedSubject2);


        //When
        conditionUpgrader.dryRunPolicyUpgrade(policy);

        //Then
        ArgumentCaptor<Set> subjectCaptor = ArgumentCaptor.forClass(Set.class);
        verify(orSubject).setESubjects(subjectCaptor.capture());
        assertThat(subjectCaptor.getValue()).hasSize(2).contains(migratedSubject1, migratedSubject2);
        verify(policy, never()).setSubject(Matchers.<EntitlementSubject>anyObject());
        verify(policy, never()).setCondition(Matchers.<EntitlementCondition>anyObject());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldMigratePolicyWithAndSubjectCondition() throws EntitlementException, UpgradeException {

        //Given
        Privilege policy = mock(Privilege.class);
        AndSubject andSubject = mock(AndSubject.class);
        Set<EntitlementSubject> andSubjects = new HashSet<EntitlementSubject>();
        PolicySubject subject1 = mock(PolicySubject.class);
        PolicySubject subject2 = mock(PolicySubject.class);
        andSubjects.add(subject1);
        andSubjects.add(subject2);
        EntitlementSubject migratedSubject1 = mock(EntitlementSubject.class);
        EntitlementSubject migratedSubject2 = mock(EntitlementSubject.class);

        given(policy.getSubject()).willReturn(andSubject);
        given(andSubject.getESubjects()).willReturn(andSubjects);
        given(subject1.getClassName()).willReturn("SUBJECT1_CLASS_NAME");
        given(subject2.getClassName()).willReturn("SUBJECT2_CLASS_NAME");
        given(conditionUpgradeMap.migrateSubjectCondition(eq("SUBJECT1_CLASS_NAME"), eq(subject1),
                Matchers.<MigrationReport>anyObject())).willReturn(migratedSubject1);
        given(conditionUpgradeMap.migrateSubjectCondition(eq("SUBJECT2_CLASS_NAME"), eq(subject2),
                Matchers.<MigrationReport>anyObject())).willReturn(migratedSubject2);


        //When
        conditionUpgrader.dryRunPolicyUpgrade(policy);

        //Then
        ArgumentCaptor<Set> subjectCaptor = ArgumentCaptor.forClass(Set.class);
        verify(andSubject).setESubjects(subjectCaptor.capture());
        assertThat(subjectCaptor.getValue()).hasSize(2).contains(migratedSubject1, migratedSubject2);
        verify(policy, never()).setSubject(Matchers.<EntitlementSubject>anyObject());
        verify(policy, never()).setCondition(Matchers.<EntitlementCondition>anyObject());
    }

    @Test
    public void shouldMigratePolicyWithNotSubjectCondition() throws EntitlementException, UpgradeException {

        //Given
        Privilege policy = mock(Privilege.class);
        NotSubject notSubject = mock(NotSubject.class);
        Set<EntitlementSubject> notSubjects = new HashSet<EntitlementSubject>();
        PolicySubject subject = mock(PolicySubject.class);
        notSubjects.add(subject);
        EntitlementSubject migratedSubject = mock(EntitlementSubject.class);

        given(policy.getSubject()).willReturn(notSubject);
        given(notSubject.getESubjects()).willReturn(notSubjects);
        given(subject.getClassName()).willReturn("SUBJECT_CLASS_NAME");
        given(conditionUpgradeMap.migrateSubjectCondition(eq("SUBJECT_CLASS_NAME"), eq(subject),
                Matchers.<MigrationReport>anyObject())).willReturn(migratedSubject);

        //When
        conditionUpgrader.dryRunPolicyUpgrade(policy);

        //Then
        ArgumentCaptor<Set> subjectCaptor = ArgumentCaptor.forClass(Set.class);
        verify(notSubject).setESubjects(subjectCaptor.capture());
        assertThat(subjectCaptor.getValue()).hasSize(1).contains(migratedSubject);
        verify(policy, never()).setSubject(Matchers.<EntitlementSubject>anyObject());
        verify(policy, never()).setCondition(Matchers.<EntitlementCondition>anyObject());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldMigratePolicyWithOrEnvironmentCondition() throws EntitlementException, UpgradeException {

        //Given
        Privilege policy = mock(Privilege.class);
        OrCondition orCondition = mock(OrCondition.class);
        Set<EntitlementCondition> orConditions = new HashSet<EntitlementCondition>();
        PolicyCondition condition1 = mock(PolicyCondition.class);
        PolicyCondition condition2 = mock(PolicyCondition.class);
        orConditions.add(condition1);
        orConditions.add(condition2);
        EntitlementCondition migratedCondition1 = mock(EntitlementCondition.class);
        EntitlementCondition migratedCondition2 = mock(EntitlementCondition.class);

        given(policy.getCondition()).willReturn(orCondition);
        given(orCondition.getEConditions()).willReturn(orConditions);
        given(condition1.getClassName()).willReturn("CONDITION1_CLASS_NAME");
        given(condition2.getClassName()).willReturn("CONDITION2_CLASS_NAME");
        given(conditionUpgradeMap.migrateEnvironmentCondition(eq("CONDITION1_CLASS_NAME"), eq(condition1),
                Matchers.<MigrationReport>anyObject())).willReturn(migratedCondition1);
        given(conditionUpgradeMap.migrateEnvironmentCondition(eq("CONDITION2_CLASS_NAME"), eq(condition2),
                Matchers.<MigrationReport>anyObject())).willReturn(migratedCondition2);


        //When
        conditionUpgrader.dryRunPolicyUpgrade(policy);

        //Then
        ArgumentCaptor<Set> conditionsCaptor = ArgumentCaptor.forClass(Set.class);
        verify(orCondition).setEConditions(conditionsCaptor.capture());
        assertThat(conditionsCaptor.getValue()).hasSize(2).contains(migratedCondition1, migratedCondition2);
        verify(policy, never()).setSubject(Matchers.<EntitlementSubject>anyObject());
        verify(policy, never()).setCondition(Matchers.<EntitlementCondition>anyObject());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldMigratePolicyWithAndEnvironmentCondition() throws EntitlementException, UpgradeException {

        //Given
        Privilege policy = mock(Privilege.class);
        AndCondition andCondition = mock(AndCondition.class);
        Set<EntitlementCondition> andConditions = new HashSet<EntitlementCondition>();
        PolicyCondition condition1 = mock(PolicyCondition.class);
        PolicyCondition condition2 = mock(PolicyCondition.class);
        andConditions.add(condition1);
        andConditions.add(condition2);
        EntitlementCondition migratedCondition1 = mock(EntitlementCondition.class);
        EntitlementCondition migratedCondition2 = mock(EntitlementCondition.class);

        given(policy.getCondition()).willReturn(andCondition);
        given(andCondition.getEConditions()).willReturn(andConditions);
        given(condition1.getClassName()).willReturn("CONDITION1_CLASS_NAME");
        given(condition2.getClassName()).willReturn("CONDITION2_CLASS_NAME");
        given(conditionUpgradeMap.migrateEnvironmentCondition(eq("CONDITION1_CLASS_NAME"), eq(condition1),
                Matchers.<MigrationReport>anyObject())).willReturn(migratedCondition1);
        given(conditionUpgradeMap.migrateEnvironmentCondition(eq("CONDITION2_CLASS_NAME"), eq(condition2),
                Matchers.<MigrationReport>anyObject())).willReturn(migratedCondition2);


        //When
        conditionUpgrader.dryRunPolicyUpgrade(policy);

        //Then
        ArgumentCaptor<Set> conditionCaptor = ArgumentCaptor.forClass(Set.class);
        verify(andCondition).setEConditions(conditionCaptor.capture());
        assertThat(conditionCaptor.getValue()).hasSize(2).contains(migratedCondition1, migratedCondition2);
        verify(policy, never()).setSubject(Matchers.<EntitlementSubject>anyObject());
        verify(policy, never()).setCondition(Matchers.<EntitlementCondition>anyObject());
    }

    @Test
    public void shouldMigratePolicyWithNotEnvironmentCondition() throws EntitlementException, UpgradeException {

        //Given
        Privilege policy = mock(Privilege.class);
        NotCondition notCondition = mock(NotCondition.class);
        Set<EntitlementCondition> notConditions = new HashSet<EntitlementCondition>();
        PolicyCondition condition = mock(PolicyCondition.class);
        notConditions.add(condition);
        EntitlementCondition migratedCondition = mock(EntitlementCondition.class);

        given(policy.getCondition()).willReturn(notCondition);
        given(notCondition.getEConditions()).willReturn(notConditions);
        given(condition.getClassName()).willReturn("CONDITION_CLASS_NAME");
        given(conditionUpgradeMap.migrateEnvironmentCondition(eq("CONDITION_CLASS_NAME"), eq(condition),
                Matchers.<MigrationReport>anyObject())).willReturn(migratedCondition);

        //When
        conditionUpgrader.dryRunPolicyUpgrade(policy);

        //Then
        ArgumentCaptor<Set> conditionCaptor = ArgumentCaptor.forClass(Set.class);
        verify(notCondition).setEConditions(conditionCaptor.capture());
        assertThat(conditionCaptor.getValue()).hasSize(1).contains(migratedCondition);
        verify(policy, never()).setSubject(Matchers.<EntitlementSubject>anyObject());
        verify(policy, never()).setCondition(Matchers.<EntitlementCondition>anyObject());
    }
}
