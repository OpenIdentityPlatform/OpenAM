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
* Copyright 2014 ForgeRock AS.
*/

package org.forgerock.openam.rest.router;

import org.forgerock.guice.core.InjectorHolder;

import javax.inject.Singleton;

/**
* A proxy implementation of the RestEndpointManager, which delegates all its calls to the "real" implemenation.
* <br/>
* Using this proxy instead of the "real" implementation prevents singletons in the core of OpenAM to be initialised
* prior to OpenAM being configured. As if this happens then some functions in OpenAM will be broken and a restart will
* be required for them to be restored.
*
* @since 12.0.0
*/
@Singleton
public class RestEndpointManagerProxy implements RestEndpointManager {

    /**
     * Enum to lazy init the CTSPersistentStore variable in a thread safe manner.
     */
    private enum EndpointManagerHolder {
        INSTANCE;

        private final RestEndpointManager endpointManager;

        private EndpointManagerHolder() {
            endpointManager = InjectorHolder.getInstance(RestEndpointManagerImpl.class);
        }

        static RestEndpointManager get() {
            return INSTANCE.endpointManager;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public EndpointType getEndpointType(String endpoint) {
        return EndpointManagerHolder.get().getEndpointType(endpoint);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String findEndpoint(String request) {
        return EndpointManagerHolder.get().findEndpoint(request);
    }
}
