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
 * Copyright 2026 3A Systems, LLC.
 */

package com.sun.identity.entitlement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Unit tests for {@link EntitlementClassResolver}, the allowlist used by the entitlement
 * (de)serialization layer to instantiate nested subject / condition / resource-attribute members
 * safely (GHSA-573r-mwh6-jw8j, CWE-470 / CWE-502).
 * <p>
 * {@code singleThreaded} because the tests assert on the shared {@link GadgetProbe} static state.
 */
@Test(singleThreaded = true)
public class EntitlementClassResolverTest {

    private static final String GADGET = "com.sun.identity.entitlement.NonEntitlementGadget";
    private static final String CONCRETE_SUBJECT = "com.sun.identity.entitlement.NoSubject";
    private static final String CONCRETE_CONDITION = "com.sun.identity.entitlement.OrCondition";
    private static final String ABSTRACT_SUBJECT = "com.sun.identity.entitlement.LogicalSubject";
    private static final String SUBJECT_INTERFACE = "com.sun.identity.entitlement.EntitlementSubject";

    @BeforeMethod
    public void setUp() {
        GadgetProbe.reset();
    }

    @Test
    public void resolvesAndInstantiatesAllowedSubjectType() throws Exception {
        EntitlementSubject subject = EntitlementClassResolver.newInstance(
                CONCRETE_SUBJECT, EntitlementSubject.class);
        assertThat(subject).isInstanceOf(NoSubject.class);
    }

    @Test
    public void resolvesAndInstantiatesAllowedConditionType() throws Exception {
        EntitlementCondition condition = EntitlementClassResolver.newInstance(
                CONCRETE_CONDITION, EntitlementCondition.class);
        assertThat(condition).isInstanceOf(OrCondition.class);
    }

    @Test
    public void toleratesSurroundingWhitespaceInClassName() throws Exception {
        EntitlementSubject subject = EntitlementClassResolver.newInstance(
                "  " + CONCRETE_SUBJECT + "  ", EntitlementSubject.class);
        assertThat(subject).isInstanceOf(NoSubject.class);
    }

    @Test
    public void rejectsGadgetForSubjectWithoutLoadingOrConstructingIt() {
        Throwable thrown = catchThrowable(() ->
                EntitlementClassResolver.newInstance(GADGET, EntitlementSubject.class));

        assertThat(thrown).isInstanceOf(ClassNotFoundException.class);
        // The class must never be initialized (no static initializer) nor instantiated (no
        // no-arg constructor) - that is the whole point of the allowlist.
        assertThat(GadgetProbe.staticInitialised).isFalse();
        assertThat(GadgetProbe.constructed).isFalse();
    }

    @Test
    public void rejectsGadgetForConditionWithoutLoadingOrConstructingIt() {
        Throwable thrown = catchThrowable(() ->
                EntitlementClassResolver.newInstance(GADGET, EntitlementCondition.class));

        assertThat(thrown).isInstanceOf(ClassNotFoundException.class);
        assertThat(GadgetProbe.staticInitialised).isFalse();
        assertThat(GadgetProbe.constructed).isFalse();
    }

    @Test
    public void rejectsClassOfWrongEntitlementFamily() {
        // A real EntitlementSubject is not an EntitlementCondition, and vice versa.
        assertThat(catchThrowable(() ->
                EntitlementClassResolver.newInstance(CONCRETE_SUBJECT, EntitlementCondition.class)))
                .isInstanceOf(ClassNotFoundException.class);
        assertThat(catchThrowable(() ->
                EntitlementClassResolver.newInstance(CONCRETE_CONDITION, EntitlementSubject.class)))
                .isInstanceOf(ClassNotFoundException.class);
    }

    @Test
    public void rejectsAbstractType() {
        assertThat(catchThrowable(() ->
                EntitlementClassResolver.newInstance(ABSTRACT_SUBJECT, EntitlementSubject.class)))
                .isInstanceOf(ClassNotFoundException.class);
    }

    @Test
    public void rejectsInterfaceType() {
        assertThat(catchThrowable(() ->
                EntitlementClassResolver.newInstance(SUBJECT_INTERFACE, EntitlementSubject.class)))
                .isInstanceOf(ClassNotFoundException.class);
    }

    @Test
    public void rejectsUnknownClassName() {
        assertThat(catchThrowable(() ->
                EntitlementClassResolver.newInstance("com.example.DoesNotExist", EntitlementSubject.class)))
                .isInstanceOf(ClassNotFoundException.class);
    }

    @Test
    public void distinguishesRefusedTypeFromMissingClass() {
        // A class that is present but of the wrong type, and a present-but-abstract class, are
        // refusals - not "no such class". Call sites report the two differently to the administrator.
        assertThat(catchThrowable(() ->
                EntitlementClassResolver.newInstance(GADGET, EntitlementSubject.class)))
                .isInstanceOf(EntitlementClassResolver.RejectedTypeException.class);
        assertThat(catchThrowable(() ->
                EntitlementClassResolver.newInstance(ABSTRACT_SUBJECT, EntitlementSubject.class)))
                .isInstanceOf(EntitlementClassResolver.RejectedTypeException.class);
        assertThat(catchThrowable(() ->
                EntitlementClassResolver.newInstance(SUBJECT_INTERFACE, EntitlementSubject.class)))
                .isInstanceOf(EntitlementClassResolver.RejectedTypeException.class);

        // A genuinely absent class stays a plain ClassNotFoundException.
        assertThat(catchThrowable(() ->
                EntitlementClassResolver.newInstance("com.example.DoesNotExist", EntitlementSubject.class)))
                .isInstanceOf(ClassNotFoundException.class)
                .isNotInstanceOf(EntitlementClassResolver.RejectedTypeException.class);
    }

    @Test
    public void reportsThrowingConstructorAsInstantiationException() {
        // Class.newInstance() would rethrow the constructor's IllegalStateException undeclared,
        // escaping the reflection catch clauses of every deserialization call site.
        Throwable thrown = catchThrowable(() -> EntitlementClassResolver.newInstance(
                "com.sun.identity.entitlement.ThrowingConstructorSubject", EntitlementSubject.class));

        assertThat(thrown).isInstanceOf(InstantiationException.class);
        assertThat(thrown.getCause()).isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void reportsFailingStaticInitializerAsInstantiationExceptionOnEveryAttempt() {
        // Loading without initialization defers <clinit> to the instantiation attempt. The first
        // one reports ExceptionInInitializerError, and every one afterwards NoClassDefFoundError
        // because the class is left in erroneous state - both LinkageErrors, and both have to be
        // converted or they escape as Errors past the call sites' reflection catch clauses.
        Throwable first = catchThrowable(() -> EntitlementClassResolver.newInstance(
                "com.sun.identity.entitlement.FailingInitializerSubject", EntitlementSubject.class));

        assertThat(first).isInstanceOf(InstantiationException.class);
        assertThat(first.getCause()).isInstanceOf(ExceptionInInitializerError.class);

        Throwable second = catchThrowable(() -> EntitlementClassResolver.newInstance(
                "com.sun.identity.entitlement.FailingInitializerSubject", EntitlementSubject.class));

        assertThat(second).isInstanceOf(InstantiationException.class);
        assertThat(second.getCause()).isInstanceOf(NoClassDefFoundError.class);
    }

    @Test
    public void resolvesAgainstDeploymentClassLoaderForBootstrapExpectedType() throws Exception {
        // A JDK expectedType reports a null (bootstrap) loader, against which nothing on the
        // deployment classpath resolves; the resolver must fall back rather than silently find nothing.
        Object resolved = EntitlementClassResolver.newInstance(CONCRETE_SUBJECT, Object.class);
        assertThat(resolved).isInstanceOf(NoSubject.class);
    }

    @Test
    public void rejectsNullClassName() {
        assertThat(catchThrowable(() ->
                EntitlementClassResolver.newInstance(null, EntitlementSubject.class)))
                .isInstanceOf(ClassNotFoundException.class);
    }

    @Test
    public void rejectsNullExpectedType() {
        assertThat(catchThrowable(() ->
                EntitlementClassResolver.newInstance(CONCRETE_SUBJECT, null)))
                .isInstanceOf(ClassNotFoundException.class);
    }
}
