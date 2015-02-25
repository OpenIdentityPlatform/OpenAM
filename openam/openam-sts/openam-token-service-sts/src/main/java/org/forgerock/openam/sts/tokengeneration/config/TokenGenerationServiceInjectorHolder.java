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
 * information: "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright 2014 ForgeRock AS. All rights reserved.
 */

package org.forgerock.openam.sts.tokengeneration.config;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;

/**
 * Class used to create the injector corresponding to the bindings defining the TokenGenerationService. This class will
 * only be referenced by the TokenGenerationServiceConnectionFactoryProvider, when it is initialized by the CREST
 * servlet the first time the token-generation-service is invoked. This class serves as the bridge between the
 * non-guice CREST servlet context and the guice bindings which define the functionality of the token generation service.
 */
public enum TokenGenerationServiceInjectorHolder {
    /**
     * The Singleton instance of the InjectorHolder.
     */
    INSTANCE;

    private final Injector injector;

    private TokenGenerationServiceInjectorHolder() {
        try {
            injector = Guice.createInjector(new TokenGenerationModule());
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Returns the appropriate instance for the given injection key.
     * Avoid using this method, in favor of having Guice inject your dependencies ahead of time.
     * Is only called by the TokenGenerationServiceConnectionFactoryProvider.
     *
     * @param key The key that defines the class to get.
     * @param <T> The type of class defined by the key.
     * @return An instance of the class defined by the key.
     */
    public static <T> T getInstance(Key<T> key) {
        return INSTANCE.injector.getInstance(key);
    }

}
