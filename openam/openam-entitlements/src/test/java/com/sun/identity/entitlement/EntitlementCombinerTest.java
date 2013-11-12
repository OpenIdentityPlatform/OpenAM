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
 * Copyright 2013 ForgeRock Inc.
 */
package com.sun.identity.entitlement;

import com.sun.identity.entitlement.interfaces.ResourceName;
import org.forgerock.openam.utils.CollectionUtils;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.security.auth.Subject;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.Collections.EMPTY_MAP;
import static java.util.Collections.EMPTY_SET;
import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Unit tests for the abstract class EntitlementCombiner.
 *
 * @author andrew.forrest@forgerock.com
 */
public class EntitlementCombinerTest {

    private static final String APP_NAME = "testAppName";
    private static final String RESOURCE_NAME = "http://test.web.com:8080/hello/world/page.html";
    private static final boolean SELF_MODE = false;

    private EntitlementCombiner combiner;
    private Invoker invoker;
    private Application app;
    private ResourceName resourceName;

    @BeforeMethod
    public void setUp() throws Exception {
        invoker = mock(Invoker.class);
        combiner = new DumbEntitlementCombiner(invoker);
        app = mock(Application.class);
        resourceName = mock(ResourceName.class);

        given(app.getName()).willReturn(APP_NAME);
        given(app.getResourceComparator()).willReturn(resourceName);
        given(app.getActions()).willReturn(EMPTY_MAP);

        combiner.init(RESOURCE_NAME, EMPTY_SET, SELF_MODE, app);

        verify(app).getName();
        verify(app).getResourceComparator();
        verify(app).getActions();
        verifyNoMoreInteractions(invoker, app, resourceName);
    }

    /**
     * Given two entitlements both with action values and no advice, a
     * single entitlement should be created with action values merged.
     */
    @Test
    public void addUsingSelfModeNoAdvice() throws EntitlementException {
        // Given
        given(invoker.combine(Boolean.TRUE, Boolean.TRUE)).willReturn(Boolean.TRUE);
        given(invoker.isCompleted()).willReturn(false).willReturn(true);

        // When
        Map<String, Boolean> actionNames = new HashMap<String, Boolean>();
        actionNames.put("GET", Boolean.TRUE);
        actionNames.put("POST", Boolean.TRUE);

        List<Entitlement> entitlements = Arrays.asList(
                new Entitlement(APP_NAME, "http://test.web.com:8080/hello/*", actionNames),
                new Entitlement(APP_NAME, "http://test.web.com:8080/hello/world/page.html", actionNames));

        combiner.add(entitlements);
        entitlements = combiner.getResults();

        // Then
        assertThat(entitlements).hasSize(1);
        Entitlement entitlement = entitlements.get(0);
        assertThat(entitlement.getApplicationName()).isEqualTo(APP_NAME);
        assertThat(entitlement.getResourceName()).isEqualTo(RESOURCE_NAME);
        assertThat(entitlement.getActionValues()).isEqualTo(actionNames);
        assertThat(entitlement.getAdvices()).isEmpty();

        verify(invoker, times(2)).combine(Boolean.TRUE, Boolean.TRUE);
        verify(invoker, times(2)).isCompleted();
        verifyNoMoreInteractions(invoker, app, resourceName);
    }

    /**
     * Given two entitlements one with action values and one with advice, a single
     * entitlement should be created with no action values but with the advice.
     */
    @Test
    public void addUsingSelfModeWithAdvice() throws EntitlementException {
        // Given
        given(invoker.isCompleted()).willReturn(false).willReturn(true);

        // When
        Map<String, Boolean> actionNames = new HashMap<String, Boolean>();
        actionNames.put("GET", Boolean.TRUE);
        actionNames.put("POST", Boolean.TRUE);

        Map<String, Set<String>> advices = new HashMap<String, Set<String>>();
        advices.put("someAdvice", CollectionUtils.asSet("property1", "property2"));

        Entitlement eWithoutAdvice = new Entitlement(APP_NAME, "http://test.web.com:8080/hello/*", actionNames);
        Entitlement eWithAdvice = new Entitlement(APP_NAME, "http://test.web.com:8080/hello/world/page.html", EMPTY_MAP);
        eWithAdvice.setAdvices(advices);

        List<Entitlement> entitlements = Arrays.asList(eWithoutAdvice, eWithAdvice);

        combiner.add(entitlements);
        entitlements = combiner.getResults();

        // Then
        assertThat(entitlements).hasSize(1);
        Entitlement entitlement = entitlements.get(0);
        assertThat(entitlement.getApplicationName()).isEqualTo(APP_NAME);
        assertThat(entitlement.getResourceName()).isEqualTo(RESOURCE_NAME);
        assertThat(entitlement.getActionValues()).isEqualTo(EMPTY_MAP);
        assertThat(entitlement.getAdvices()).isEqualTo(advices);

        verify(invoker, times(2)).isCompleted();
        verifyNoMoreInteractions(invoker, app, resourceName);
    }


    // The invoker allows the test to monitor how the abstract methods are being interacted with.
    private static interface Invoker {

        /**
         * @see EntitlementCombiner
         */
        public boolean combine(Boolean b1, Boolean b2);

        /**
         * @see EntitlementCombiner#isCompleted()
         */
        public boolean isCompleted();

    }

    // Given the class under test is abstract, this dumb implementation assists with unit testing.
    private static class DumbEntitlementCombiner extends EntitlementCombiner {

        private final Invoker invoker;

        public DumbEntitlementCombiner(Invoker invoker) {
            this.invoker = invoker;
        }

        @Override
        protected boolean combine(Boolean b1, Boolean b2) {
            return invoker.combine(b1, b2);
        }

        @Override
        protected boolean isCompleted() {
            return invoker.isCompleted();
        }
    }

}
