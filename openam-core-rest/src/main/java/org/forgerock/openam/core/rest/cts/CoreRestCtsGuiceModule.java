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
 * Copyright 2016 ForgeRock AS.
 */

package org.forgerock.openam.core.rest.cts;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.iplanet.am.util.SystemProperties;
import com.iplanet.dpro.session.service.SessionService;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.openam.cts.utils.JSONSerialisation;
import org.forgerock.openam.rest.router.CTSPersistentStoreProxy;
import org.forgerock.openam.utils.Config;

/**
 * Guice module for binding the CTS REST endpoints.
 *
 * @since 14.0.0
 */
public class CoreRestCtsGuiceModule extends AbstractModule {

    @Override
    protected void configure() {
    }

    @Provides
    @Inject
    @Singleton
    public CoreTokenResource getCoreTokenResource(JSONSerialisation jsonSerialisation,
            CTSPersistentStoreProxy ctsPersistentStore, @Named("frRest") Debug debug) {
        return new CoreTokenResource(jsonSerialisation, ctsPersistentStore, debug);
    }

    @Provides
    @Inject
    @Singleton
    public CoreTokenResourceAuthzModule getCoreTokenResourceAuthzModule(
            Config<SessionService> sessionService, @Named("frRest") Debug debug) {
        boolean coreTokenResourceEnabled = SystemProperties.getAsBoolean(Constants.CORE_TOKEN_RESOURCE_ENABLED);
        return new CoreTokenResourceAuthzModule(sessionService, debug, coreTokenResourceEnabled);
    }
}