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
 * Copyright 2014-2015 ForgeRock AS.
 * Portions copyright 2025-2026 3A Systems, LLC.
 */

package org.forgerock.openam.rest;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.reset;
import static org.mockito.Mockito.*;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Collections;

import org.forgerock.openam.rest.service.RestletServiceServlet;
import org.mockito.ArgumentMatchers;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class RestEndpointServletTest {

    private RestEndpointServlet restEndpointServlet;

    private RestletServiceServlet restletOAuth2ServiceServlet;
    private RestletServiceServlet restletUMAServiceServlet;

    @BeforeClass
    public void setupMocks() {
        restletOAuth2ServiceServlet = mock(RestletServiceServlet.class);
        restletUMAServiceServlet = mock(RestletServiceServlet.class);
    }

    @BeforeMethod
    public void setUp() {
        reset(restletOAuth2ServiceServlet, restletUMAServiceServlet);

        restEndpointServlet = new RestEndpointServlet(restletOAuth2ServiceServlet, restletUMAServiceServlet);
    }

    @DataProvider(name = "restletPaths")
    public Object[][] restletPathData() {
        return new Object[][] {
                {"/oauth2", restletOAuth2ServiceServlet},
                {"/uma", restletUMAServiceServlet}
        };
    }

    @Test(dataProvider = "restletPaths", enabled = false)
    public void shouldHandleRequestWithRestletServlet(String path, HttpServlet servlet) throws Exception {

        //Given
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        given(request.getHeaderNames()).willReturn(Collections.enumeration(Collections.<String>emptySet()));
        given(request.getAttributeNames()).willReturn(Collections.enumeration(Collections.<String>emptySet()));
        given(request.getServletPath()).willReturn(path);

        //When
        restEndpointServlet.service(request, response);

        //Then
        verify(servlet).service(ArgumentMatchers.<HttpServletRequest>anyObject(), eq(response));
    }

    @Test
    public void unknownServletPathAnswers404() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        given(request.getServletPath()).willReturn("/xacml");

        restEndpointServlet.service(request, response);

        verify(response).sendError(HttpServletResponse.SC_NOT_FOUND);
        verifyZeroInteractions(restletOAuth2ServiceServlet, restletUMAServiceServlet);
    }

    @Test
    public void shouldCallDestroy() {

        //Given

        //When
        restEndpointServlet.destroy();

        //Then
        verify(restletOAuth2ServiceServlet).destroy();
        verify(restletUMAServiceServlet).destroy();
    }
}
