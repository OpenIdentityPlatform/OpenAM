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
package org.forgerock.openam.core.rest;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.Set;

import com.google.inject.Module;
import org.forgerock.guice.core.GuiceModuleLoader;
import org.forgerock.guice.core.InjectorConfiguration;
import org.forgerock.guice.core.InjectorHolder;

/**
 * Keeps the Guice modules on the classpath out of the injector a class under test builds when it first touches
 * {@link InjectorHolder} - {@link IdentityRestUtils} does so from a static initialiser - since loading them needs a
 * configured OpenAM.
 *
 * The module loader is a JVM wide setting and this deliberately does not restore it.  {@code InjectorHolder} reads
 * it once, when it builds the injector it then holds for the life of the JVM, so restoring it afterwards would
 * change nothing that comes later; and the loader it replaces is a package private class that cannot be
 * reconstructed from out here.
 *
 * For the same reason it has to be installed before anything in the JVM touches {@code InjectorHolder}, not merely
 * before the class that needs it runs.  Any earlier test that does - {@code SessionResourceV2Test} constructs an
 * {@code AMIdentity}, which asks for a {@code RepoAuditorFactory} and swallows the failure - builds the injector
 * out of the classpath modules, fails, and leaves {@code InjectorHolder} unusable for the rest of the run.  So the
 * classes that need it call this from a {@code @BeforeSuite} method, which TestNG runs before any test method of
 * the suite whatever order the classes are run in.  That still comes after TestNG has instantiated the test
 * classes, so it does not cover a test class that touches {@code InjectorHolder} while being constructed.
 */
final class InjectorHolderTestSupport {

    private static final GuiceModuleLoader NO_MODULES = new GuiceModuleLoader() {
        @Override
        public Set<Class<? extends Module>> getGuiceModules(Class<? extends Annotation> moduleAnnotation) {
            return Collections.emptySet();
        }
    };

    private InjectorHolderTestSupport() {
    }

    /**
     * Arranges for the injector to be built out of no modules at all.
     */
    static void withoutGuiceModules() {
        InjectorConfiguration.setGuiceModuleLoader(NO_MODULES);
    }
}
