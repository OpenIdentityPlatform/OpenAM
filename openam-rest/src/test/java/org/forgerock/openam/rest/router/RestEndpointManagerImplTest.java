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
 * Copyright 2014-2016 ForgeRock AS.
 */

package org.forgerock.openam.rest.router;

import java.util.HashSet;
import java.util.Set;
import org.forgerock.openam.rest.resource.CrestRouter;
import org.forgerock.openam.rest.RestEndpoints;
import org.forgerock.openam.rest.service.ServiceRouter;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.testng.Assert.assertEquals;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class RestEndpointManagerImplTest {

    private RestEndpointManagerImpl endpointManager;

    @BeforeMethod
    public void setUp() {

        RestEndpoints restEndpoints = mock(RestEndpoints.class);
        CrestRouter resourceRouter = mock(CrestRouter.class);
        CrestRouter frrestRouter = mock(CrestRouter.class);
        ServiceRouter serviceRouter = mock(ServiceRouter.class);
        Set<String> resourceRoutes = new HashSet<String>();
        Set<String> serviceRoutes = new HashSet<String>();
        Set<String> frrestRoutes = new HashSet<String>();


        given(restEndpoints.getResourceRouter()).willReturn(resourceRouter);
        given(restEndpoints.getFrestRouter()).willReturn(frrestRouter);
        given(restEndpoints.getJSONServiceRouter()).willReturn(serviceRouter);

        given(resourceRouter.getRoutes()).willReturn(resourceRoutes);
        given(frrestRouter.getRoutes()).willReturn(frrestRoutes);
        given(serviceRouter.getRoutes()).willReturn(serviceRoutes);

        resourceRoutes.add("/users");
        resourceRoutes.add("/groups");
        resourceRoutes.add("/config");
        resourceRoutes.add("/tokens");// this should not be returned

        frrestRoutes.add("/token");

        serviceRoutes.add("/authenticate");
        serviceRoutes.add("/other");

        endpointManager = new RestEndpointManagerImpl(restEndpoints);
    }

    @Test
    public void shouldFindServiceEndpoint() {

        //Given

        //When
        String endpoint = endpointManager.findEndpoint("/realm1/{realm2}/realm3/authenticate/{id}");

        //Then
        assertEquals(endpoint, "/authenticate");
    }

    @Test
    public void shouldFindCollectionResourceEndpoint() {

        //Given

        //When
        String endpoint = endpointManager.findEndpoint("/realm1/{realm2}/realm3/groups/{id}");

        //Then
        assertEquals(endpoint, "/groups");
    }

    @Test
    public void shouldFindSingletonResourceEndpoint() {

        //Given

        //When
        String endpoint = endpointManager.findEndpoint("/realm1/{realm2}/realm3/config/{id}");

        //Then
        assertEquals(endpoint, "/config");
    }

    @Test
    public void shouldFindTokenResourceEndpoint() {

        //Given

        //When
        String endpoint = endpointManager.findEndpoint("/frrest/oauth2/token/{id}");

        //Then
        assertEquals(endpoint, "/token");
    }

    @Test
    public void shouldNotFindTokenResourceEndpoint() {

        //Given

        //When
        String endpoint = endpointManager.findEndpoint("/frrest/oauth2/tokens/{id}_action=RevokeTokens");

        //Then
        assertEquals(endpoint, "/tokens");
    }


}
