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
package org.forgerock.openam.entitlement.rest.wrappers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import com.sun.identity.entitlement.DenyOverride;
import com.sun.identity.entitlement.EntitlementCombiner;
import com.sun.identity.entitlement.URLResourceName;
import com.sun.identity.entitlement.interfaces.ISearchIndex;
import com.sun.identity.entitlement.util.ResourceNameIndexGenerator;
import com.sun.identity.entitlement.util.ResourceNameSplitter;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Tests for the {@code searchIndex} / {@code saveIndex} / {@code resourceComparator} setters of
 * {@link ApplicationWrapper}, which resolve an attacker-supplied class name from the entitlement
 * Applications REST request body.
 * <p>
 * The security-relevant behaviour (GHSA-6jpj-522x-53vv, CWE-470 unsafe reflection) is that an
 * arbitrary class name must NOT be loaded-and-initialised or instantiated: only an instantiable
 * subtype of the expected extension interface is accepted, and validation happens before any
 * static initialiser or constructor can run.
 */
public class ApplicationWrapperTest {

    private ApplicationWrapper wrapper;

    @BeforeMethod
    public void setUp() {
        wrapper = new ApplicationWrapper();
    }

    // --- valid implementations are accepted ---

    @Test
    public void acceptsValidSearchIndexImplementation() throws Exception {
        wrapper.setSearchIndex(ResourceNameSplitter.class.getName());
        assertThat(wrapper.getApplication().getSearchIndexClass()).isEqualTo(ResourceNameSplitter.class);
    }

    @Test
    public void acceptsValidSaveIndexImplementation() throws Exception {
        wrapper.setSaveIndex(ResourceNameIndexGenerator.class.getName());
        assertThat(wrapper.getApplication().getSaveIndexClass()).isEqualTo(ResourceNameIndexGenerator.class);
    }

    @Test
    public void acceptsValidResourceComparatorImplementation() throws Exception {
        wrapper.setResourceComparator(URLResourceName.class.getName());
        assertThat(wrapper.getApplication().getResourceComparatorClass()).isEqualTo(URLResourceName.class);
    }

    // --- null / empty remain no-ops ---

    @Test
    public void treatsNullAndEmptySearchIndexAsNoOp() throws Exception {
        wrapper.setSearchIndex(null);
        wrapper.setSearchIndex("");
        assertThat(wrapper.getApplication().getSearchIndexClass()).isNull();
    }

    // --- arbitrary (non-implementing) classes are rejected without executing their code ---
    // Each reject test uses its OWN gadget class: a class initialises at most once per classloader,
    // so sharing one gadget would make the static-initialiser assertion vacuous in every test after
    // the first. Distinct gadgets keep the assertion meaningful regardless of method order.

    @Test
    public void searchIndexRejectsArbitraryClassWithoutLoadingOrInstantiating() {
        Throwable thrown = catchThrowable(() -> wrapper.setSearchIndex(SearchGadget.class.getName()));
        assertThat(thrown).isInstanceOf(ClassNotFoundException.class);
        assertThat(Probes.searchStaticInit).as("static initialiser must not run").isFalse();
        assertThat(Probes.searchConstructor).as("no-arg constructor must not run").isFalse();
        assertThat(wrapper.getApplication().getSearchIndexClass()).isNull();
    }

    @Test
    public void saveIndexRejectsArbitraryClassWithoutLoadingOrInstantiating() {
        Throwable thrown = catchThrowable(() -> wrapper.setSaveIndex(SaveGadget.class.getName()));
        assertThat(thrown).isInstanceOf(ClassNotFoundException.class);
        assertThat(Probes.saveStaticInit).as("static initialiser must not run").isFalse();
        assertThat(Probes.saveConstructor).as("no-arg constructor must not run").isFalse();
        assertThat(wrapper.getApplication().getSaveIndexClass()).isNull();
    }

    @Test
    public void resourceComparatorRejectsArbitraryClassWithoutLoadingOrInstantiating() {
        Throwable thrown = catchThrowable(() -> wrapper.setResourceComparator(ComparatorGadget.class.getName()));
        assertThat(thrown).isInstanceOf(ClassNotFoundException.class);
        assertThat(Probes.comparatorStaticInit).as("static initialiser must not run").isFalse();
        assertThat(Probes.comparatorConstructor).as("no-arg constructor must not run").isFalse();
        assertThat(wrapper.getApplication().getResourceComparatorClass()).isNull();
    }

    /**
     * An existing classpath class with a public no-arg constructor that is NOT an entitlement
     * extension interface must be rejected AFTER loading, before instantiation. {@code java.util.ArrayList}
     * is present on every JRE, so the test exercises the load-then-type-reject path on all platforms.
     */
    @Test
    public void searchIndexRejectsExistingNonImplementingClass() {
        Throwable thrown = catchThrowable(() -> wrapper.setSearchIndex("java.util.ArrayList"));
        assertThat(thrown).isInstanceOf(ClassNotFoundException.class);
        assertThat(wrapper.getApplication().getSearchIndexClass()).isNull();
    }

    @Test
    public void searchIndexRejectsUnknownClass() {
        Throwable thrown = catchThrowable(() -> wrapper.setSearchIndex("com.example.NoSuchClass"));
        assertThat(thrown).isInstanceOf(ClassNotFoundException.class);
        assertThat(wrapper.getApplication().getSearchIndexClass()).isNull();
    }

    /** An abstract implementation of an extension interface is loadable but not instantiable. */
    @Test
    public void searchIndexRejectsAbstractImplementation() {
        Throwable thrown = catchThrowable(() -> wrapper.setSearchIndex(AbstractSearchIndex.class.getName()));
        assertThat(thrown).isInstanceOf(ClassNotFoundException.class);
        assertThat(wrapper.getApplication().getSearchIndexClass()).isNull();
    }

    @Test
    public void treatsNullAndEmptySaveIndexAsNoOp() throws Exception {
        wrapper.setSaveIndex(null);
        wrapper.setSaveIndex("");
        assertThat(wrapper.getApplication().getSaveIndexClass()).isNull();
    }

    @Test
    public void treatsNullAndEmptyResourceComparatorAsNoOp() throws Exception {
        wrapper.setResourceComparator(null);
        wrapper.setResourceComparator("");
        assertThat(wrapper.getApplication().getResourceComparatorClass()).isNull();
    }

    // --- entitlementCombiner is the fourth reflective setter; a class name reaching it must not run
    //     the named class's static initializer before the EntitlementCombiner type check (CWE-470). ---

    @Test
    public void entitlementCombinerAcceptsRegisteredShortName() throws Exception {
        wrapper.setEntitlementCombiner("DenyOverride");
        assertThat(wrapper.getApplication().getEntitlementCombinerClass()).isEqualTo(DenyOverride.class);
    }

    @Test
    public void entitlementCombinerAcceptsValidImplementationByClassName() throws Exception {
        // A full class name misses the short-name registry and exercises the hardened Class.forName path.
        wrapper.setEntitlementCombiner(DenyOverride.class.getName());
        assertThat(wrapper.getApplication().getEntitlementCombinerClass()).isEqualTo(DenyOverride.class);
    }

    @Test
    public void entitlementCombinerRejectsArbitraryClassWithoutLoadingOrInstantiating() {
        Throwable thrown = catchThrowable(() -> wrapper.setEntitlementCombiner(CombinerGadget.class.getName()));
        // Probes are asserted FIRST: on vulnerable code the assertion must report that gadget code ran.
        assertThat(Probes.combinerStaticInit).as("static initialiser must not run").isFalse();
        assertThat(Probes.combinerConstructor).as("no-arg constructor must not run").isFalse();
        assertThat(thrown).isInstanceOf(ClassNotFoundException.class);
        assertThat(wrapper.getApplication().getEntitlementCombinerClass()).isNull();
    }

    /**
     * The abstract {@link EntitlementCombiner} base class is a loadable EntitlementCombiner subtype, but
     * not instantiable: accepting it used to store a combiner whose instantiation later fails, so
     * {@code Application.getEntitlementCombiner()} returned null and callers threw NullPointerException
     * (HTTP 500). It must be rejected up front like every other invalid name.
     */
    @Test
    public void entitlementCombinerRejectsAbstractBaseClass() {
        Throwable thrown = catchThrowable(() -> wrapper.setEntitlementCombiner(EntitlementCombiner.class.getName()));
        assertThat(thrown).isInstanceOf(ClassNotFoundException.class);
        assertThat(wrapper.getApplication().getEntitlementCombinerClass()).isNull();
    }

    @Test
    public void entitlementCombinerRejectsUnknownClass() {
        Throwable thrown = catchThrowable(() -> wrapper.setEntitlementCombiner("com.example.NoSuchCombiner"));
        assertThat(thrown).isInstanceOf(ClassNotFoundException.class);
        assertThat(wrapper.getApplication().getEntitlementCombinerClass()).isNull();
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
        static boolean combinerStaticInit;
        static boolean combinerConstructor;
    }

    /** Stand-in gadgets implementing none of the extension interfaces; flip their flags if run. */
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

    /** Not an EntitlementCombiner; neither its static initialiser nor constructor must ever run. */
    public static final class CombinerGadget {
        static {
            Probes.combinerStaticInit = true;
        }

        public CombinerGadget() {
            Probes.combinerConstructor = true;
        }
    }

    /** A loadable but non-instantiable (abstract) ISearchIndex subtype; must be rejected. */
    public static abstract class AbstractSearchIndex implements ISearchIndex {
    }
}
