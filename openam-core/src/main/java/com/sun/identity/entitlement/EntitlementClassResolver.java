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

package com.sun.identity.entitlement;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;

/**
 * Resolves an externally-supplied class name into an instance of an expected entitlement type,
 * safely.
 * <p>
 * The entitlement (de)serialization layer stores the concrete implementation class of every nested
 * subject / condition / resource-attribute member as a {@code className} string and rebuilds the
 * object graph reflectively. Historically each call site did
 * {@code Class.forName(name)} followed by {@code newInstance()} and only then cast to the expected
 * type. That is unsafe reflection (CWE-470 / CWE-502): the one-argument
 * {@link Class#forName(String)} runs the target class's static initializer at load time and
 * {@code newInstance()} runs its no-argument constructor, so an attacker-controlled name executes
 * arbitrary classpath code <em>before</em> the trailing cast can reject it. A cast is not a guard.
 * <p>
 * This helper closes that gap: it loads the class <em>without</em> initializing it (the three-argument
 * {@link Class#forName(String, boolean, ClassLoader)} form with {@code initialize == false}) and
 * verifies it is an instantiable subtype of {@code expectedType} <em>before</em> it is ever
 * instantiated. Only genuine, allowed entitlement types are constructed.
 */
public final class EntitlementClassResolver {

    private EntitlementClassResolver() {
    }

    /**
     * Signals that the requested class was found on the classpath but refused: it is not a subtype
     * of the expected entitlement type, or it is not instantiable (abstract class or interface).
     * <p>
     * It extends {@link ClassNotFoundException} so that every pre-existing call site keeps
     * compiling and keeps treating a refusal as a resolution failure, while call sites that want to
     * report "found but refused" differently from "no such class" can catch this subtype first.
     */
    public static class RejectedTypeException extends ClassNotFoundException {

        private static final long serialVersionUID = 1L;

        public RejectedTypeException(String message) {
            super(message);
        }
    }

    /**
     * Loads, validates and instantiates {@code className} as an instance of {@code expectedType}.
     *
     * @param className the requested implementation class name (may carry surrounding whitespace)
     * @param expectedType the entitlement type the class must implement or extend
     * @param <T> the entitlement type
     * @return a new instance of the requested class, guaranteed to be a {@code expectedType}
     * @throws RejectedTypeException if the class exists but is not an instantiable subtype of
     *         {@code expectedType} (i.e. the name is not on the allowlist implied by
     *         {@code expectedType}); the class is never instantiated in this case
     * @throws ClassNotFoundException if the class cannot be loaded at all
     * @throws InstantiationException if the validated class cannot be instantiated, including when
     *         its no-argument constructor is missing or throws
     * @throws IllegalAccessException if the validated class's no-argument constructor is inaccessible
     */
    public static <T> T newInstance(String className, Class<T> expectedType)
            throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        if (expectedType == null) {
            throw new ClassNotFoundException("no expected entitlement type supplied");
        }
        if (className == null) {
            throw new ClassNotFoundException("null class name");
        }
        final String trimmed = className.trim();
        final Class<?> clazz;
        try {
            // Load WITHOUT initialising so a malicious class's static initializer cannot run before
            // the type check below rejects it.
            clazz = Class.forName(trimmed, false, classLoaderFor(expectedType));
        } catch (LinkageError e) {
            throw new ClassNotFoundException("Unable to load class " + trimmed, e);
        }
        // Modifier.isAbstract is true for interfaces as well as abstract classes, so this one check
        // rejects every non-instantiable type in addition to anything of the wrong type.
        if (!expectedType.isAssignableFrom(clazz) || Modifier.isAbstract(clazz.getModifiers())) {
            throw new RejectedTypeException(trimmed + " is not an instantiable "
                    + expectedType.getSimpleName() + " implementation");
        }
        return expectedType.cast(instantiate(clazz));
    }

    /**
     * Returns the loader to resolve against: the expected type's own loader, falling back to this
     * class's loader when that is the bootstrap loader (a JDK {@code expectedType} reports
     * {@code null}, against which nothing on the deployment classpath would resolve).
     */
    private static ClassLoader classLoaderFor(Class<?> expectedType) {
        ClassLoader loader = expectedType.getClassLoader();
        return (loader != null) ? loader : EntitlementClassResolver.class.getClassLoader();
    }

    /**
     * Instantiates an already-validated class through its no-argument constructor.
     * <p>
     * Uses {@code getDeclaredConstructor().newInstance()} rather than the deprecated
     * {@link Class#newInstance()}, which rethrows constructor exceptions undeclared and so lets a
     * member whose constructor throws a {@code RuntimeException} escape past every caller's
     * reflection {@code catch} clauses. Here a throwing constructor surfaces as a declared
     * {@link InstantiationException} instead.
     */
    private static Object instantiate(Class<?> clazz)
            throws InstantiationException, IllegalAccessException {
        try {
            return clazz.getDeclaredConstructor().newInstance();
        } catch (NoSuchMethodException e) {
            throw wrap(clazz, "has no accessible no-argument constructor", e);
        } catch (InvocationTargetException e) {
            Throwable cause = (e.getCause() != null) ? e.getCause() : e;
            throw wrap(clazz, "constructor threw " + cause, cause);
        } catch (LinkageError e) {
            // Loading without initialising moves <clinit> to this point, so a class that is a valid
            // type but fails to initialise reports it here: ExceptionInInitializerError on the first
            // attempt and NoClassDefFoundError ("could not initialize class") on every attempt
            // afterwards, the class being left in erroneous state. Both are LinkageErrors and both
            // have to be converted, or the second one escapes as an Error past every call site's
            // reflection catch clauses.
            throw wrap(clazz, "could not be initialized", e);
        }
    }

    private static InstantiationException wrap(Class<?> clazz, String reason, Throwable cause) {
        InstantiationException ie = new InstantiationException(clazz.getName() + " " + reason);
        ie.initCause(cause);
        return ie;
    }
}
