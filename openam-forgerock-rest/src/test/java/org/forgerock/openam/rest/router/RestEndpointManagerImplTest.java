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

import org.forgerock.json.resource.CollectionResourceProvider;
import org.forgerock.json.resource.SingletonResourceProvider;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

import static org.forgerock.openam.forgerockrest.guice.RestEndpointGuiceProvider.ServiceProviderClass;
import static org.testng.Assert.assertEquals;

public class RestEndpointManagerImplTest {

    @Test
    public void shouldFindServiceEndpoint() {

        //Given
        Map<String, CollectionResourceProvider> collectionResourceEndpoints =
                new HashMap<String, CollectionResourceProvider>();
        Map<String, SingletonResourceProvider> singletonResourceEndpoints =
                new HashMap<String, SingletonResourceProvider>();
        Map<String, ServiceProviderClass> serviceEndpoints = new HashMap<String, ServiceProviderClass>();

        collectionResourceEndpoints.put("/users", null);
        collectionResourceEndpoints.put("/groups", null);
        singletonResourceEndpoints.put("/config", null);
        serviceEndpoints.put("/authenticate", null);
        serviceEndpoints.put("/other", null);

        RestEndpointManagerImpl endpointManager = new RestEndpointManagerImpl(collectionResourceEndpoints,
                singletonResourceEndpoints, serviceEndpoints);

        //When
        String endpointType = endpointManager.findEndpoint("/realm1/{realm2}/realm3/authenticate/{id}");

        //Then
        assertEquals(endpointType, "/authenticate");
    }

    @Test
    public void shouldFindCollectionResourceEndpoint() {

        //Given
        Map<String, CollectionResourceProvider> collectionResourceEndpoints =
                new HashMap<String, CollectionResourceProvider>();
        Map<String, SingletonResourceProvider> singletonResourceEndpoints =
                new HashMap<String, SingletonResourceProvider>();
        Map<String, ServiceProviderClass> serviceEndpoints = new HashMap<String, ServiceProviderClass>();

        collectionResourceEndpoints.put("/users", null);
        collectionResourceEndpoints.put("/groups", null);
        singletonResourceEndpoints.put("/config", null);
        serviceEndpoints.put("/authenticate", null);
        serviceEndpoints.put("/other", null);

        RestEndpointManagerImpl endpointManager = new RestEndpointManagerImpl(collectionResourceEndpoints,
                singletonResourceEndpoints, serviceEndpoints);

        //When
        String endpointType = endpointManager.findEndpoint("/realm1/{realm2}/realm3/groups/{id}");

        //Then
        assertEquals(endpointType, "/groups");
    }

    @Test
    public void shouldFindSingletonResourceEndpoint() {

        //Given
        Map<String, CollectionResourceProvider> collectionResourceEndpoints =
                new HashMap<String, CollectionResourceProvider>();
        Map<String, SingletonResourceProvider> singletonResourceEndpoints =
                new HashMap<String, SingletonResourceProvider>();
        new HashMap<String, SingletonResourceProvider>();
        Map<String, ServiceProviderClass> serviceEndpoints = new HashMap<String, ServiceProviderClass>();

        collectionResourceEndpoints.put("/users", null);
        collectionResourceEndpoints.put("/groups", null);
        singletonResourceEndpoints.put("/config", null);
        serviceEndpoints.put("/authenticate", null);
        serviceEndpoints.put("/other", null);

        RestEndpointManagerImpl endpointManager = new RestEndpointManagerImpl(collectionResourceEndpoints,
                singletonResourceEndpoints, serviceEndpoints);

        //When
        String endpointType = endpointManager.findEndpoint("/realm1/{realm2}/realm3/config/{id}");

        //Then
        assertEquals(endpointType, "/config");
    }
}
