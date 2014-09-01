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

package org.forgerock.openam.guice;

import com.google.inject.Injector;
import com.google.inject.Module;

import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.Set;

/**
 * Constructs the OpenAM Guice injector by scanning the classpath for all classes annotated with a particular
 * annotation and uses them to initialise Guice.
 * <p>
 * This class should not be used directly, but instead via the {@link InjectorHolder}.
 *
 * @author Phill Cunnington
 */
public class InjectorFactory {

    private final ClasspathScanner classpathScanner;
    private final GuiceModuleCreator moduleCreator;
    private final GuiceInjectorCreator injectorCreator;

    /**
     * Constructs an instance of the InjectorFactory.
     *
     * @param classpathScanner An instance of the ClasspathScanner.
     * @param moduleCreator An instance of the GuiceModuleCreator.
     * @param injectorCreator An instance of the GuiceInjectorCreator.
     */
    public InjectorFactory(ClasspathScanner classpathScanner, GuiceModuleCreator moduleCreator,
            GuiceInjectorCreator injectorCreator) {
        this.classpathScanner = classpathScanner;
        this.moduleCreator = moduleCreator;
        this.injectorCreator = injectorCreator;
    }

    /**
     * Creates a new Guice injector which is configured by all modules found on the classpath annotated with the
     * given annotation.
     *
     * @param moduleAnnotation The module annotation.
     * @return A Guice injector.
     */
    public synchronized Injector createInjector(Class<? extends Annotation> moduleAnnotation) {
        return injectorCreator.createInjector(createModules(moduleAnnotation));
    }

    /**
     * Uses the given module annotation to scan the classpath for the Guice modules and instantiates them.
     *
     * @param moduleAnnotation The module annotation.
     * @return A Set of Guice modules.
     */
    private synchronized Set<Module> createModules(Class<? extends Annotation> moduleAnnotation) {

        Set<Class<?>> moduleClasses = classpathScanner.getTypesAnnotatedWith(moduleAnnotation);

        Set<Module> modules = new HashSet<Module>();

        for (Class<?> moduleClass : moduleClasses) {
            if (!Module.class.isAssignableFrom(moduleClass)) {
                throw new IllegalArgumentException(moduleClass.getCanonicalName() + " is annotated with @"
                        + moduleAnnotation.getSimpleName() + " but does not implement the Module interface");
            }

            Module module = moduleCreator.createInstance((Class<? extends Module>)moduleClass);

            modules.add(module);
        }

        return modules;
    }
}
