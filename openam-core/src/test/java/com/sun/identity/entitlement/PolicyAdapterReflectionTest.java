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

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.sun.identity.entitlement.opensso.PolicyCondition;
import com.sun.identity.entitlement.opensso.PolicyResponseProvider;
import com.sun.identity.entitlement.opensso.PolicySubject;

/**
 * The legacy adapters {@link PolicySubject}, {@link PolicyCondition} and {@link PolicyResponseProvider}
 * are genuine {@code EntitlementSubject}/{@code EntitlementCondition}/{@code ResourceAttribute}s, so
 * they pass the outer allowlist and can be smuggled in as nested members of an imported policy. Each
 * then resolves its own {@code className} into a legacy {@code com.sun.identity.policy} type. These
 * tests confirm that path now rejects an arbitrary class without running its static initializer or
 * constructor (GHSA-573r-mwh6-jw8j, CWE-470).
 * <p>
 * {@code singleThreaded} because the tests assert on the shared {@link GadgetProbe} static state.
 */
@Test(singleThreaded = true)
public class PolicyAdapterReflectionTest {

    private static final String GADGET = "com.sun.identity.entitlement.NonEntitlementGadget";

    @BeforeMethod
    public void setUp() {
        GadgetProbe.reset();
    }

    private static void assertGadgetNeverTouched() {
        assertThat(GadgetProbe.staticInitialised).isFalse();
        assertThat(GadgetProbe.constructed).isFalse();
    }

    @Test
    public void policySubjectRejectsGadgetWithoutInstantiatingIt() {
        PolicySubject adapter = new PolicySubject("n", GADGET, Collections.<String>emptySet(), false);

        assertThat(catchThrowable(adapter::getPolicySubject)).isInstanceOf(EntitlementException.class);
        assertGadgetNeverTouched();
    }

    @Test
    public void policyConditionRejectsGadgetWithoutInstantiatingIt() {
        PolicyCondition adapter = new PolicyCondition("n", GADGET, Collections.<String, Set<String>>emptyMap());

        assertThat(catchThrowable(adapter::getPolicyCondition)).isInstanceOf(EntitlementException.class);
        assertGadgetNeverTouched();
    }

    @Test
    public void policyResponseProviderRejectsGadgetWithoutInstantiatingIt() {
        PolicyResponseProvider adapter =
                new PolicyResponseProvider("n", GADGET, "p", Collections.<String>emptySet());

        assertThat(catchThrowable(adapter::getResponseProvider)).isInstanceOf(EntitlementException.class);
        assertGadgetNeverTouched();
    }
}
