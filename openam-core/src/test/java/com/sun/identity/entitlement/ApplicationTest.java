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
 * Copyright 2014-2015 ForgeRock AS.
 * Portions Copyrighted 2026 3A Systems, LLC
 */

package com.sun.identity.entitlement;

import com.sun.identity.entitlement.interfaces.ResourceName;
import com.sun.identity.entitlement.util.ResourceNameIndexGenerator;
import com.sun.identity.entitlement.util.ResourceNameSplitter;
import org.forgerock.openam.entitlement.EntitlementRegistry;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.util.Collections;

public class ApplicationTest {

    private Application testApplication;

    @BeforeTest
    public void theSetUp() {
        testApplication = new Application();
    }

    @Test
    public void shouldDeferToApplicationTypeComparator() throws IllegalAccessException, InstantiationException {

        //given
        ApplicationType appType = new ApplicationType(null, null, null, null, null);
        ResourceName appTypeResourceName = appType.getResourceComparator();
        testApplication.setApplicationType(appType);

        //when
        ResourceName result = testApplication.getResourceComparator();

        //then
        assertEquals(appTypeResourceName, result);
    }

    @Test
    public void shouldNotDeferToApplicationTypeComparator() throws IllegalAccessException, InstantiationException {

        //given
        ApplicationType appType = new ApplicationType(null, null, null, null, null);
        Class resourceNameClass = PrefixResourceName.class;
        testApplication.setApplicationType(appType);
        testApplication.setResourceComparator(resourceNameClass);

        //when
        ResourceName result = testApplication.getResourceComparator(false);

        //then
        assertEquals(resourceNameClass, result.getClass());

    }

    @Test
    public void shouldCreateEditableClone() throws IllegalAccessException, InstantiationException {
        //given

        //when
        Application clone = testApplication.clone();

        //then
        assertEquals(clone.isEditable(), true);
    }

    @Test
    public void shouldAcceptValidSearchIndex() throws Exception {
        Application app = new Application();
        app.setSearchIndex(ResourceNameSplitter.class);
        assertEquals(app.getSearchIndexClass(), ResourceNameSplitter.class);
    }

    @Test
    public void shouldAcceptValidSaveIndex() throws Exception {
        Application app = new Application();
        app.setSaveIndex(ResourceNameIndexGenerator.class);
        assertEquals(app.getSaveIndexClass(), ResourceNameIndexGenerator.class);
    }

    // Each reject test uses its OWN gadget class: a class initialises at most once per classloader,
    // so sharing one gadget would make the static-initialiser assertion vacuous in every test after
    // the first. Distinct gadgets keep the assertion meaningful regardless of method order.

    @Test
    public void shouldRejectSearchIndexThatIsNotAnImplementationWithoutInstantiating() {
        Application app = new Application();
        Throwable thrown = catchSetterFailure(() -> app.setSearchIndex(SearchGadget.class));
        // Probes are asserted FIRST: on vulnerable code the setter fails with an unrelated
        // ClassCastException, and the probe assertions must still report that gadget code ran.
        assertFalse(Probes.searchStaticInit, "static initialiser must not run");
        assertFalse(Probes.searchConstructor, "no-arg constructor must not run");
        assertTrue(thrown instanceof InstantiationException,
                "expected InstantiationException but was: " + thrown);
        assertNull(app.getSearchIndexClass());
    }

    @Test
    public void shouldRejectSaveIndexThatIsNotAnImplementationWithoutInstantiating() {
        Application app = new Application();
        Throwable thrown = catchSetterFailure(() -> app.setSaveIndex(SaveGadget.class));
        assertFalse(Probes.saveStaticInit, "static initialiser must not run");
        assertFalse(Probes.saveConstructor, "no-arg constructor must not run");
        assertTrue(thrown instanceof InstantiationException,
                "expected InstantiationException but was: " + thrown);
        assertNull(app.getSaveIndexClass());
    }

    @Test
    public void shouldRejectResourceComparatorThatIsNotAnImplementationWithoutInstantiating() {
        Application app = new Application();
        Throwable thrown = catchSetterFailure(() -> app.setResourceComparator(ComparatorGadget.class));
        assertFalse(Probes.comparatorStaticInit, "static initialiser must not run");
        assertFalse(Probes.comparatorConstructor, "no-arg constructor must not run");
        assertTrue(thrown instanceof InstantiationException,
                "expected InstantiationException but was: " + thrown);
        assertNull(app.getResourceComparatorClass());
    }

    private static Throwable catchSetterFailure(ThrowingSetter setter) {
        try {
            setter.call();
            return null;
        } catch (Throwable t) {
            return t;
        }
    }

    private interface ThrowingSetter {
        void call() throws InstantiationException, IllegalAccessException;
    }

    /**
     * Per-gadget tripwire flags, kept in a separate holder so reading a flag never initialises the
     * gadget it belongs to (which would defeat the static-initialiser assertion).
     */
    static final class Probes {
        static boolean searchStaticInit;
        static boolean searchConstructor;
        static boolean saveStaticInit;
        static boolean saveConstructor;
        static boolean comparatorStaticInit;
        static boolean comparatorConstructor;
    }

    /** Stand-in classes that are not a search/save index or resource comparator; flip flags if run. */
    public static final class SearchGadget {
        static {
            Probes.searchStaticInit = true;
        }

        public SearchGadget() {
            Probes.searchConstructor = true;
        }
    }

    public static final class SaveGadget {
        static {
            Probes.saveStaticInit = true;
        }

        public SaveGadget() {
            Probes.saveConstructor = true;
        }
    }

    public static final class ComparatorGadget {
        static {
            Probes.comparatorStaticInit = true;
        }

        public ComparatorGadget() {
            Probes.comparatorConstructor = true;
        }
    }

}
