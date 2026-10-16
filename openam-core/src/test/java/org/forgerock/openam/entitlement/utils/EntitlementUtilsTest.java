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
package org.forgerock.openam.entitlement.utils;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.testng.annotations.Test;

import com.sun.identity.entitlement.DenyOverride;
import com.sun.identity.entitlement.EntitlementCombiner;
import com.sun.identity.entitlement.interfaces.ISearchIndex;
import com.sun.identity.entitlement.util.ResourceNameSplitter;

/**
 * Tests for {@link EntitlementUtils#resolveExtensionClass(String, Class)} and the entitlement-combiner
 * resolvers built on top of it (GHSA-6jpj-522x-53vv, CWE-470 unsafe reflection): only instantiable
 * subtypes of the expected extension interface may resolve; everything else must be rejected with a
 * clean {@link ClassNotFoundException} — including classes that fail to link.
 */
public class EntitlementUtilsTest {

    // --- resolveExtensionClass ---

    @Test
    public void shouldResolveConcreteImplementation() throws Exception {
        assertEquals(EntitlementUtils.resolveExtensionClass(ResourceNameSplitter.class.getName(), ISearchIndex.class),
                ResourceNameSplitter.class);
    }

    @Test(expectedExceptions = ClassNotFoundException.class)
    public void shouldRejectClassNotImplementingExpectedType() throws Exception {
        // Loadable on every JRE, has a public no-arg constructor, but is not an ISearchIndex.
        EntitlementUtils.resolveExtensionClass("java.util.ArrayList", ISearchIndex.class);
    }

    @Test(expectedExceptions = ClassNotFoundException.class)
    public void shouldRejectAbstractSubtype() throws Exception {
        EntitlementUtils.resolveExtensionClass(AbstractIndex.class.getName(), ISearchIndex.class);
    }

    @Test(expectedExceptions = ClassNotFoundException.class)
    public void shouldRejectInterfaceItself() throws Exception {
        EntitlementUtils.resolveExtensionClass(ISearchIndex.class.getName(), ISearchIndex.class);
    }

    @Test(expectedExceptions = ClassNotFoundException.class)
    public void shouldRejectUnknownClass() throws Exception {
        EntitlementUtils.resolveExtensionClass("com.example.NoSuchClass", ISearchIndex.class);
    }

    /**
     * A class that fails to LINK (here: one of its superinterfaces is missing from the classloader)
     * throws {@link LinkageError}, not {@link ClassNotFoundException}, from {@code Class.forName}. The
     * resolver must translate it into a clean {@link ClassNotFoundException} so callers answer with a
     * Bad Request instead of a 500.
     * <p>
     * The broken classpath is built from the already-compiled nested types of this test: {@code Ext},
     * {@code Gone} and {@code BrokenImpl implements Ext, Gone} are copied into a directory-backed
     * classloader (with no parent, so nothing leaks from the test classpath) — except {@code Gone}.
     */
    @Test
    public void shouldTranslateLinkageErrorIntoClassNotFoundException() throws Exception {
        Path root = Files.createTempDirectory("resolve-extension-class");
        Path pkg = root.resolve("org/forgerock/openam/entitlement/utils");
        Files.createDirectories(pkg);
        copyClassResource("EntitlementUtilsTest$Ext.class", pkg);
        copyClassResource("EntitlementUtilsTest$BrokenImpl.class", pkg);
        // EntitlementUtilsTest$Gone.class is deliberately NOT copied.

        URLClassLoader loader = new URLClassLoader(new URL[] {root.toUri().toURL()}, null);
        try {
            Class<?> ext = Class.forName(EntitlementUtilsTest.class.getName() + "$Ext", false, loader);
            try {
                EntitlementUtils.resolveExtensionClass(EntitlementUtilsTest.class.getName() + "$BrokenImpl", ext);
                fail("expected ClassNotFoundException for a class that fails to link");
            } catch (ClassNotFoundException expected) {
                assertTrue(expected.getCause() instanceof LinkageError,
                        "cause must be the original LinkageError but was: " + expected.getCause());
            }
        } finally {
            loader.close();
        }
    }

    private static void copyClassResource(String name, Path targetDir) throws Exception {
        InputStream in = EntitlementUtilsTest.class.getResourceAsStream(name);
        assertTrue(in != null, "compiled test class resource not found: " + name);
        try {
            Files.copy(in, targetDir.resolve(name), StandardCopyOption.REPLACE_EXISTING);
        } finally {
            in.close();
        }
    }

    // --- entitlement combiner resolution ---

    @Test
    public void strictCombinerShouldResolveRegisteredShortName() throws Exception {
        assertEquals(EntitlementUtils.resolveEntitlementCombiner("DenyOverride"), DenyOverride.class);
    }

    @Test
    public void strictCombinerShouldResolveConcreteImplementationByClassName() throws Exception {
        assertEquals(EntitlementUtils.resolveEntitlementCombiner(DenyOverride.class.getName()), DenyOverride.class);
    }

    @Test(expectedExceptions = ClassNotFoundException.class)
    public void strictCombinerShouldRejectAbstractBaseClass() throws Exception {
        EntitlementUtils.resolveEntitlementCombiner(EntitlementCombiner.class.getName());
    }

    @Test(expectedExceptions = ClassNotFoundException.class)
    public void strictCombinerShouldRejectClassNotImplementingCombiner() throws Exception {
        EntitlementUtils.resolveEntitlementCombiner("java.util.ArrayList");
    }

    /** The lenient variant keeps its documented contract: any rejected name falls back to DenyOverride. */
    @Test
    public void lenientCombinerShouldFallBackToDenyOverrideForRejectedNames() {
        assertEquals(EntitlementUtils.getEntitlementCombiner(EntitlementCombiner.class.getName()),
                DenyOverride.class);
        assertEquals(EntitlementUtils.getEntitlementCombiner("com.example.NoSuchCombiner"), DenyOverride.class);
        assertEquals(EntitlementUtils.getEntitlementCombiner("java.util.ArrayList"), DenyOverride.class);
    }

    /** A loadable but non-instantiable (abstract) ISearchIndex subtype; must be rejected. */
    public static abstract class AbstractIndex implements ISearchIndex {
    }

    /** Superinterface present in the broken-classpath loader. */
    public interface Ext {
    }

    /** Superinterface deliberately missing from the broken-classpath loader. */
    public interface Gone {
    }

    /** Links only if both {@link Ext} and {@link Gone} are present; used to provoke a LinkageError. */
    public static final class BrokenImpl implements Ext, Gone {
    }
}
