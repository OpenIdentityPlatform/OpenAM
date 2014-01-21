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

package org.forgerock.openam.rest.service;

import javax.inject.Inject;
import java.util.Map;

import static org.forgerock.openam.forgerockrest.guice.RestEndpointGuiceProvider.ServiceProviderClass;

/**
 * Simple class for containing the Map of registered service endpoints to make it easier to get from Guice.
 *
 * @since 12.0.0
 */
public class ServiceResourceEndpointMap {

    private final Map<String, ServiceProviderClass> serviceEndpoints;

    /**
     * Constructs a new instance of the ServiceResourceEndpointMap.
     *
     * @param serviceEndpoints
     */
    @Inject
    public ServiceResourceEndpointMap(final Map<String, ServiceProviderClass> serviceEndpoints) {
        this.serviceEndpoints = serviceEndpoints;
    }

    /**
     * Gets the Service Endpoints.
     *
     * @return The Map of Service Endpoints.
     */
    public Map<String, ServiceProviderClass> getServiceEndpoints() {
        return serviceEndpoints;
    }
}
