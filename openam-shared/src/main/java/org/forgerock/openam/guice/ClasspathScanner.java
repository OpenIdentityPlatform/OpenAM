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

//TODO Not currently used as need support for jboss 7 VFS classpath handling and the org.reflections library cannot
//TODO be used due to licence issues and other librarys do not offer the same level of integration.
/**
 * Provides classpath scanning for annotations via the Scannotations library.
 * <p>
 * This class is used to create the Guice Injector, so it must be possible to construct this class without Guice.
 *
 * @author Phill Cunnington
 */
public class ClasspathScanner {

//    private static final String IPLANET_BASE_SCAN_PACKAGE = "com/iplanet";
//    private static final String SUN_IDENTITY_BASE_SCAN_PACKAGE = "com/sun/identity";
//    private static final String FORGEROCK_BASE_SCAN_PACKAGE = "org/forgerock/openam";
//
//    /**
//     * Find all classes in the classpath annotated with the given annotation.
//     *
//     * @param annotation The annotation to scan for.
//     * @return A Set of annotated classes.
//     */
//    public Set<Class<?>> getTypesAnnotatedWith(Class<? extends Annotation> annotation) {
//        Set<String> classNames = getAnnotationDB().getAnnotationIndex().get(annotation.getName());
//
//        Set<Class<?>> classes = new HashSet<Class<?>>();
//        for (String className : classNames) {
//            try {
//                classes.add(Class.forName(className));
//            } catch (ClassNotFoundException e) {
//                throw new RuntimeException("Class not found, " + className, e);
//            }
//        }
//        return classes;
//    }
//
//    /**
//     * Lazily initialises the Scannotations library to scan packages under "com.iplanet", "com.sun.identity" and
//     * "org.forgerock.openam".
//     *
//     * Ignores packages under "java" and "javax".
//     *
//     * @return An AnnotationDB instance.
//     */
//    private synchronized AnnotationDB getAnnotationDB() {
//
//        AnnotationDB annotationDB = new AnnotationDB();
//        annotationDB.setScanFieldAnnotations(false);
//        annotationDB.setScanMethodAnnotations(false);
//        annotationDB.setScanParameterAnnotations(false);
//        annotationDB.setIgnoredPackages(new String[]{"java\\..*", "javax\\..*"});
//        try {
//            annotationDB.scanArchives(getScanPackages());
//        } catch (IOException e) {
//            throw new RuntimeException("Error whilst scanning packages", e);
//        }
//
//        return annotationDB;
//    }
//
//    private URL[] getScanPackages() {
//
//        URL[] iplanetPackage = ClasspathUrlFinder.findResourceBases(IPLANET_BASE_SCAN_PACKAGE);
//        URL[] sunIdentityPackage = ClasspathUrlFinder.findResourceBases(SUN_IDENTITY_BASE_SCAN_PACKAGE);
//        URL[] forgeRockPackage = ClasspathUrlFinder.findResourceBases(FORGEROCK_BASE_SCAN_PACKAGE);
//
//        List<URL> packages = new ArrayList<URL>();
//        packages.addAll(Arrays.asList(iplanetPackage));
//        packages.addAll(Arrays.asList(sunIdentityPackage));
//        packages.addAll(Arrays.asList(forgeRockPackage));
//
//        return packages.toArray(new URL[0]);
//    }
}
