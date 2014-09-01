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

import org.reflections.Reflections;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;

import java.lang.annotation.Annotation;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

/**
 * Provides classpath scanning for annotations via the Reflections library.
 * <p>
 * This class is used to create the Guice Injector, so it must be possible to construct this class without Guice.
 *
 * @author Phill Cunnington
 */
public class ClasspathScanner {

    private static final String IPLANET_BASE_SCAN_PACKAGE = "com.iplanet";
    private static final String SUN_IDENTITY_BASE_SCAN_PACKAGE = "com.sun.identity";
    private static final String FORGEROCK_BASE_SCAN_PACKAGE = "org.forgerock.openam";

    private Reflections reflections;

    /**
     * Find all classes in the classpath annotated with the given annotation.
     *
     * @param annotation The annotation to scan for.
     * @return A Set of annotated classes.
     */
    public Set<Class<?>> getTypesAnnotatedWith(Class<? extends Annotation> annotation) {
        return getReflections().getTypesAnnotatedWith(annotation);
    }

    /**
     * Lazily initialises the Reflections library to scan packages under "com.iplanet", "com.sun.identity" and
     * "org.forgerock.openam".
     *
     * Ignores packages under "java" and "javax".
     *
     * @return A Reflections instance.
     */
    private synchronized Reflections getReflections() {
        if (reflections == null) {

            Set<URL> javaClasspath = new HashSet<URL>();
            javaClasspath.addAll(ClasspathHelper.forPackage(IPLANET_BASE_SCAN_PACKAGE));
            javaClasspath.addAll(ClasspathHelper.forPackage(SUN_IDENTITY_BASE_SCAN_PACKAGE));
            javaClasspath.addAll(ClasspathHelper.forPackage(FORGEROCK_BASE_SCAN_PACKAGE));

            reflections = new Reflections(new ConfigurationBuilder()
                    .setScanners(new TypeAnnotationsScanner())
                    .setUrls(javaClasspath)
                    .filterInputsBy(new FilterBuilder()
                            .include(".*")
                            .exclude("java\\..*")
                            .exclude("javax\\..*")));
        }

        return reflections;
    }
}
