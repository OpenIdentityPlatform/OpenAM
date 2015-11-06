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

package com.sun.identity.setup;

import javax.servlet.FilterConfig;
import javax.servlet.ServletContextEvent;

import com.google.inject.Injector;
import com.google.inject.Key;

/**
 * <p>A thread-safe singleton holding the system startup Guice Injector instance that system
 * startup and bootstrapping classes can call to get to use dependency injection during the system
 * startup process.</p>
 *
 * The SystemStartupInjectorHolder should ONLY be used from
 * {@link AMSetupFilter#init(FilterConfig)} and NOWHERE else!
 *
 * @see com.google.inject.Injector
 *
 * @since 13.0.0
 */
final class SystemStartupInjectorHolder {

    private static SystemStartupInjectorHolder instance;

    private final Injector injector;

    private SystemStartupInjectorHolder(Injector injector) {
        this.injector = injector;
    }

    /**
     * <p>Initialises the system startup injector holder with a specific Guice injector
     * instance.</p>
     *
     * This should ONLY be called from the
     * {@link SystemStartupInjectorListener#contextInitialized(ServletContextEvent)} and
     * {@link AMSetupFilter} tests and NOWHERE else!
     *
     * @param injector The system startup injector.
     */
    static synchronized void initialise(Injector injector) {
        if (instance == null) {
            instance = new SystemStartupInjectorHolder(injector);
        }
    }

    /**
     * <p>Retrieves the system startup Guice injector.</p>
     *
     * This should ONLY be called from {@link AMSetupFilter#init(FilterConfig)} and NOWHERE else!
     *
     * @return The configured Guice injector.
     */
    static SystemStartupInjectorHolder getInstance() {
        if (instance == null) {
            throw new IllegalStateException("SystemStartupInjectorHolder has not been initialised!");
        }
        return instance;
    }

    /**
     * <p>Uses the Guice injector to return the appropriate instance for the given injection
     * type.</p>
     *
     * Avoid using this method, in favour of having Guice inject your dependencies ahead of time.
     *
     * @param type The class to get an instance of.
     * @param <T> The type of class to get.
     * @return A non-null instance of the class.
     */
    <T> T getInstance(Class<T> type) {
        return injector.getInstance(type);
    }

    /**
     * <p>Uses the Guice injector to return the appropriate instance for the given injection
     * key.</p>
     *
     * Avoid using this method, in favour of having Guice inject your dependencies ahead of time.
     *
     * @param key The key that defines the class to get.
     * @param <T> The type of class defined by the key.
     * @return A non-null instance of the class defined by the key.
     */
    <T> T getInstance(Key<T> key) {
        return injector.getInstance(key);
    }
}
