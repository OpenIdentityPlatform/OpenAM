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

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;

/**
 * Delegate for creating the Guice Injector, to allow for unit testing of the Guice initialisation code.
 *
 * @author Phill Cunnington
 */
public class GuiceInjectorCreator {

    /**
     * Creates the Guice injector.
     *
     * @see Guice#createInjector(Iterable)
     *
     * @param modules {@inheritDoc}
     * @return {@inheritDoc}
     */
    public Injector createInjector(Iterable<? extends Module> modules) {
        return Guice.createInjector(modules);
    }
}
