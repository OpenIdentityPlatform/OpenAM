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

import org.forgerock.json.resource.VersionSelector;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.Restlet;
import org.restlet.data.Reference;
import org.restlet.data.Status;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import javax.servlet.http.HttpServletRequest;

import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.forgerock.openam.rest.service.VersionRouter.HEADER_X_VERSION_API;

public class VersionRouterTest {

    private VersionRouter router;

    private Restlet handlerOne;
    private Restlet handlerTwo;
    private Restlet handlerThree;

    private Request request;
    private Response response;
    private HttpServletRequest httpRequest;

    @BeforeClass
    public void setUpClass() {
        handlerOne = mock(Restlet.class);
        handlerTwo = mock(Restlet.class);
        handlerThree = mock(Restlet.class);
    }

    @BeforeMethod
    public void setUp() {

        router = new VersionRouter(new VersionSelector()) {
            @Override
            HttpServletRequest getHttpRequest(Request request) {
                return httpRequest;
            }
        };

        router.addVersion("1.0", handlerOne);
        router.addVersion("1.5", handlerTwo);
        router.addVersion("2.1", handlerThree);

        request = mock(Request.class);
        response = mock(Response.class);
        httpRequest = mock(HttpServletRequest.class);
    }

    @DataProvider(name = "data")
    private Object[][] dataProvider() {
        return new Object[][]{
                {"resource=3.0", true, null},
                {"resource=1.0", false, handlerTwo},
                {"resource=1.1", false, handlerTwo},
                {"resource=1.9", true, null},
                {"resource=2.1", false, handlerThree},
                {null, false, handlerThree}
        };
    }

    @Test (dataProvider = "data")
    public void shouldHandleVersionRoute(String requestedVersion, boolean expectedException, Restlet handler) {

        //Given
        given(httpRequest.getHeader(HEADER_X_VERSION_API)).willReturn(requestedVersion);
        Reference resourceRef = mock(Reference.class);
        given(request.getResourceRef()).willReturn(resourceRef);

        //When
        router.handle(request, response);

        //Then
        if (expectedException) {
            verify(response).setStatus(eq(Status.CLIENT_ERROR_NOT_FOUND), anyString());
            verifyZeroInteractions(handlerOne, handlerTwo, handlerThree);
        } else {
            verify(handler).handle(request, response);
        }
    }

    @Test
    public void shouldNotHandleVersionRouteWithNewerProtocolMinorVersion() {

        //Given
        given(httpRequest.getHeader(HEADER_X_VERSION_API)).willReturn("protocol=1.1,resource=1.0");

        //When
        router.handle(request, response);

        //Then
        verify(response).setStatus(eq(Status.CLIENT_ERROR_BAD_REQUEST), anyString());
        verifyZeroInteractions(handlerOne, handlerTwo, handlerThree);
    }

    @Test
    public void shouldNotHandleVersionRouteWithNewerProtocolMajorVersion() {

        //Given
        given(httpRequest.getHeader(HEADER_X_VERSION_API)).willReturn("protocol=2.0,resource=1.0");

        //When
        router.handle(request, response);

        //Then
        verify(response).setStatus(eq(Status.CLIENT_ERROR_BAD_REQUEST), anyString());
        verifyZeroInteractions(handlerOne, handlerTwo, handlerThree);
    }

    @Test
    public void shouldHandleVersionRouteWithOldestBehaviour() {

        //Given
        router.defaultToOldest();

        //When
        router.handle(request, response);

        //Then
        verify(handlerOne).handle(request, response);
    }
}
