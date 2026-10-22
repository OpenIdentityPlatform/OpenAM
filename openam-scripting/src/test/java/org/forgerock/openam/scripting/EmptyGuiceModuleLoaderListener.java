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

package org.forgerock.openam.scripting;

import java.util.Collections;

import org.forgerock.guice.core.InjectorConfiguration;
import org.forgerock.guice.core.InjectorHolder;
import org.testng.IExecutionListener;

/**
 * Has {@link InjectorHolder} build its injector from no modules in every TestNG run of this module.
 *
 * <p>The {@code GuiceTestCase} subclasses here ask the holder for its injector before each test, and then register an
 * injector of their own built from {@code @GuiceModules}. The holder builds the first injector of the JVM from the
 * module loader in place at that moment and never consults the loader again. The default loader collects every Guice
 * module on the test classpath, Guice cannot build an injector from those, and the holder class then stays broken for
 * every later test in the same JVM. So the empty loader has to be in place before any class reaches the holder.</p>
 *
 * <p>TestNG loads this listener through {@code META-INF/services/org.testng.ITestNGListener} and calls
 * {@link #onExecutionStart()} before it creates any test instance. That covers constructors and field initializers,
 * and every run of the module, whichever classes it includes and however surefire forks it.</p>
 */
public class EmptyGuiceModuleLoaderListener implements IExecutionListener {

    @Override
    public void onExecutionStart() {
        InjectorConfiguration.setGuiceModuleLoader(moduleAnnotation -> Collections.emptySet());
    }

    @Override
    public void onExecutionFinish() {
    }
}
