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
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;

import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.testng.Assert.*;

public class RestEndpointServletTest {

    private RestEndpointServlet restEndpointServlet;

    private CrestHttpServlet crestServlet;
    private RestletServiceServlet restletJSONServiceServlet;
    private RestletServiceServlet restletXACMLServiceServlet;
    private RestEndpointManager endpointManager;
    private RestletServiceServlet restletOAuth2ServiceServlet;

    @BeforeClass
    public void setupMocks() {
        restletJSONServiceServlet = mock(RestletServiceServlet.class);
        restletXACMLServiceServlet = mock(RestletServiceServlet.class);
        restletOAuth2ServiceServlet = mock(RestletServiceServlet.class);
    }

    @BeforeMethod
    public void setUp() {

        crestServlet = mock(CrestHttpServlet.class);
        reset(restletJSONServiceServlet);
        reset(restletXACMLServiceServlet);
        reset(restletOAuth2ServiceServlet);
        endpointManager = mock(RestEndpointManager.class);

        restEndpointServlet = new RestEndpointServlet(crestServlet, restletJSONServiceServlet,
                restletXACMLServiceServlet, restletOAuth2ServiceServlet, endpointManager);
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

    @DataProvider(name = "restletPaths")
    public Object[][] restletPathData() {
        return new Object[][] {
                {"/xacml", restletXACMLServiceServlet},
                {"/oauth2", restletOAuth2ServiceServlet}
        };
    }

    @Test(dataProvider = "restletPaths")
    public void shouldHandleRequestWithRestletServlet(String path, RestletServiceServlet servlet) throws Exception {

        //Given
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        given(request.getServletPath()).willReturn(path);

        //When
        restEndpointServlet.service(request, response);

        //Then
        verify(servlet).service(Matchers.<HttpServletRequest>anyObject(), eq(response));
        for (RestletServiceServlet s : Arrays.asList(restletJSONServiceServlet, restletXACMLServiceServlet, restletOAuth2ServiceServlet)) {
            if (s != servlet) {
                verifyZeroInteractions(s);
            }
        }
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
