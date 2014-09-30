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

package org.forgerock.openam.rest;

import org.forgerock.openam.rest.resource.CrestHttpServlet;
import org.forgerock.openam.rest.router.RestEndpointManager;
import org.forgerock.openam.rest.service.RestletServiceServlet;
import org.mockito.Matchers;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.testng.Assert.fail;

public class RestEndpointServletTest {

    private RestEndpointServlet restEndpointServlet;

    private CrestHttpServlet crestServlet;
    private RestletServiceServlet restletJSONServiceServlet;
    private RestletServiceServlet restletXACMLServiceServlet;
    private RestEndpointManager endpointManager;

    @BeforeMethod
    public void setUp() {

        crestServlet = mock(CrestHttpServlet.class);
        restletJSONServiceServlet = mock(RestletServiceServlet.class);
        restletXACMLServiceServlet = mock(RestletServiceServlet.class);
        endpointManager = mock(RestEndpointManager.class);

        restEndpointServlet = new RestEndpointServlet(crestServlet, restletJSONServiceServlet,
                restletXACMLServiceServlet, endpointManager);
    }

    @Test
    public void shouldCallInit() throws ServletException {

        //Given

        //When
        restEndpointServlet.init();

        //Then
        verify(crestServlet).init();
        verifyZeroInteractions(restletJSONServiceServlet);
        verifyZeroInteractions(restletXACMLServiceServlet);
    }

    @Test(expectedExceptions = ServletException.class)
    public void shouldHandleRequestWithNoPath() throws ServletException, IOException {

        //Given
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        given(request.getServletPath()).willReturn("/json");
        given(request.getPathInfo()).willReturn(null);

        //When
        restEndpointServlet.service(request, response);

        //Then
        verify(endpointManager).findEndpoint("");
    }

    @Test
    public void shouldHandleRequestWithJSONRestletServlet() throws ServletException, IOException {

        //Given
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        given(request.getServletPath()).willReturn("/json");
        given(request.getPathInfo()).willReturn("/users/demo/roles/");
        given(endpointManager.findEndpoint("/users/demo/roles")).willReturn("/users");
        given(endpointManager.getEndpointType("/users")).willReturn(RestEndpointManager.EndpointType.SERVICE);

        //When
        restEndpointServlet.service(request, response);

        //Then
        verify(endpointManager).findEndpoint("/users/demo/roles");
        verify(restletJSONServiceServlet).service(Matchers.<HttpServletRequest>anyObject(), eq(response));
        verifyZeroInteractions(restletXACMLServiceServlet);
        verifyZeroInteractions(crestServlet);
    }

    @Test
    public void shouldHandleRequestWithXACMLRestletServlet() throws ServletException, IOException {

        //Given
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        given(request.getServletPath()).willReturn("/xacml");

        //When
        restEndpointServlet.service(request, response);

        //Then
        verify(restletXACMLServiceServlet).service(Matchers.<HttpServletRequest>anyObject(), eq(response));
        verifyZeroInteractions(restletJSONServiceServlet);
        verifyZeroInteractions(crestServlet);
    }

    @Test
    public void shouldHandleRequestWithCrestServlet() throws ServletException, IOException {

        //Given
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        given(request.getServletPath()).willReturn("/json");
        given(request.getPathInfo()).willReturn("/users/demo/roles/");
        given(endpointManager.findEndpoint("/users/demo/roles")).willReturn("/users");
        given(endpointManager.getEndpointType("/users")).willReturn(RestEndpointManager.EndpointType.RESOURCE);

        //When
        restEndpointServlet.service(request, response);

        //Then
        verify(endpointManager).findEndpoint("/users/demo/roles");
        verify(crestServlet).service(request, response);
        verifyZeroInteractions(restletJSONServiceServlet);
        verifyZeroInteractions(restletXACMLServiceServlet);
    }

    @Test(expectedExceptions = ServletException.class)
    public void shouldHandleRequestWithNoRoute() throws ServletException, IOException {

        //Given
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        given(request.getServletPath()).willReturn("/json");
        given(request.getPathInfo()).willReturn("/users/demo/roles/");
        given(endpointManager.findEndpoint("/users/demo/roles")).willReturn(null);

        //When
        restEndpointServlet.service(request, response);

        //Then
        fail();
    }

    @Test
    public void shouldCallDestroy() {

        //Given

        //When
        restEndpointServlet.destroy();

        //Then
        verify(crestServlet).destroy();
        verify(restletJSONServiceServlet).destroy();
        verify(restletXACMLServiceServlet).destroy();
    }
}
