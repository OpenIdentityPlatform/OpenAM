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
import com.google.inject.Key;

/**
 * A singleton holding the Guice Injector instance that other classes can call to get to use dependency injection.
 *
 * @author Phill Cunnington
 */
public enum InjectorHolder {

    /**
     * The Singleton instance of the InjectorHolder.
     */
    INSTANCE;

    private Injector injector;

    /**
     * Constructs an instance of the InjectorHolder and initialises the Guice Injector.
     */
    private InjectorHolder() {
        InjectorFactory injectorFactory = new InjectorFactory(new ClasspathScanner(),
                new GuiceModuleCreator(), new GuiceInjectorCreator());

        try {
            injector = injectorFactory.createInjector(AMGuiceModule.class);
        } catch (Exception e) {
            /**
             * This will occur during application server startup. The OpenAM
             * debugging framework will not be available at this point.
             *
             * The error gets consumed by the application server startup, which
             * is why we are printing the stack trace.
             **/
            e.printStackTrace();
            throw new IllegalStateException(e);
        }
    }

    /**
     * Returns the appropriate instance for the given injection type.
     * Avoid using this method, in favor of having Guice inject your dependencies ahead of time.
     *
     * Uses Guice to get an instance of the class given as a parameter.
     *
     * @param clazz The class to get an instance of.
     * @param <T> The type of class to get.
     * @return An instance of the class.
     */
    public static <T> T getInstance(Class<T> clazz) {
        return INSTANCE.injector.getInstance(clazz);
    }

    /**
     * Returns the appropriate instance for the given injection key.
     * Avoid using this method, in favor of having Guice inject your dependencies ahead of time.
     *
     * @param key The key that defines the class to get.
     * @param <T> The type of class defined by the key.
     * @return An instance of the class defined by the key.
     */
    public static <T> T getInstance(Key<T> key) {
        return INSTANCE.injector.getInstance(key);
    }

    /**
     * Retrieves the Guice injector.
     *
     * Use with care! Always prefer using getInstance(Class).
     *
     * @return The Guice injector.
     */
    static Injector getInjector() {
        return INSTANCE.injector;
    }
}
