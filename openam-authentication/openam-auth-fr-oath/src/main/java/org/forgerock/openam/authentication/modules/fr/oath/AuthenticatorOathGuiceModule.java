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

package org.forgerock.openam.authentication.modules.fr.oath;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.guice.core.GuiceModule;

/**
 * Guice bindings for the OATH two-step verification module.
 */
@GuiceModule
public class AuthenticatorOathGuiceModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(Debug.class).annotatedWith(Names.named("amAuthOATH")).toInstance(Debug.getInstance("amAuthOATH"));
    }

}
