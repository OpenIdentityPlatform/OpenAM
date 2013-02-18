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

package org.forgerock.openam.forgerockrest.authn;

import com.google.inject.Guice;
import com.google.inject.Injector;

/**
 * A singleton holding the Guice Injector instance that other classes can call to get to use dependency injection.
 */
public class InjectorHolder {

    private final Injector injector;

    /**
     * Uses Guice to create an instance of the class given as a parameter.
     *
     * @param clazz The class to get an instance of.
     * @param <T> The type of class to get.
     * @return An instance of the class.
     */
    public static <T> T getInstance(Class<T> clazz) {
        return SingletonHolder.INSTANCE.injector.getInstance(clazz);
    }

    /**
     * Constructs an instance of the InjectorHolder and initialises the Guice Injector.
     */
    private InjectorHolder() {
        injector = Guice.createInjector(new IdentityRestServiceGuiceModule());
    }

    /**
     * Holder class for the InjectorHolder singleton.
     */
    private static final class SingletonHolder {
        private static final InjectorHolder INSTANCE = new InjectorHolder();
    }
}
