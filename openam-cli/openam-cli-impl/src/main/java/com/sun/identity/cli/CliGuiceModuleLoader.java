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
 * Copyright 2015 ForgeRock AS.
 */

package com.sun.identity.cli;

import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.Set;

import org.forgerock.guice.core.GuiceModuleLoader;
import org.forgerock.openam.license.LicenseGuiceModule;
import org.forgerock.openam.license.LicensePresenterGuiceModule;
import org.forgerock.openam.shared.guice.SharedGuiceModule;

import com.google.inject.Module;

/**
 * Guice module loader for loading CLI specific Guice modules.
 *
 * <p>This is required so as to be able to load the {@link CliGuiceModule} which contains duplicate
 * bindings from CoreGuiceModule which cannot be loaded by the server.</p>
 *
 * @since 13.0.0
 */
public class CliGuiceModuleLoader implements GuiceModuleLoader {

    @Override
    public Set<Class<? extends Module>> getGuiceModules(Class<? extends Annotation> moduleAnnotation) {
        Set<Class<? extends Module>> moduleClasses = new HashSet<>();
        moduleClasses.add(LicensePresenterGuiceModule.class);
        moduleClasses.add(SharedGuiceModule.class);
        moduleClasses.add(LicenseGuiceModule.class);
        moduleClasses.add(CliGuiceModule.class);
        return moduleClasses;
    }
}
