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
 * Copyright 2013-2014 ForgeRock AS.
 */

package org.forgerock.openam.jaspi.modules.session;

import org.forgerock.openam.rest.router.RestEndpointManager;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.servlet.http.HttpServletRequest;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class EndpointMatcherTest {

    private EndpointMatcher endpointMatcher;

    private RestEndpointManager endpointManager;

    @BeforeMethod
    public void setUp() {

        endpointManager = mock(RestEndpointManager.class);

        endpointMatcher = new EndpointMatcher("", endpointManager);

        endpointMatcher.endpoint("/URI_A", "GET");
        endpointMatcher.endpoint("/URI_B", "POST");
        endpointMatcher.endpoint("/URI_C", "PUT", "QUERY_PARAM", "VALUEA", "VALUEB");
        endpointMatcher.endpoint("/URI_B/URI_A", "GET");
    }

    @Test
    public void shouldMatchEndpointWithNoQueryParams() {

        //Given
        HttpServletRequest request = mock(HttpServletRequest.class);

        given(request.getContextPath()).willReturn("/openam");
        given(request.getRequestURI()).willReturn("/openam/URI_A");
        given(request.getMethod()).willReturn("GET");

        given(endpointManager.findEndpoint("/URI_A")).willReturn("/URI_A");

        //When
        boolean matches = endpointMatcher.match(request);

        //Then
        assertTrue(matches);
    }

    @Test
    public void shouldNotMatchEndpointWithNoQueryParams() {

        //Given
        HttpServletRequest request = mock(HttpServletRequest.class);

        given(request.getContextPath()).willReturn("/openam");
        given(request.getRequestURI()).willReturn("/openam/URI_B");
        given(request.getMethod()).willReturn("GET");

        given(endpointManager.findEndpoint("/URI_B")).willReturn("/URI_B");

        //When
        boolean matches = endpointMatcher.match(request);

        //Then
        assertFalse(matches);
    }

    @Test
    public void shouldMatchEndpointWithQueryParams() {

        //Given
        HttpServletRequest request = mock(HttpServletRequest.class);

        given(request.getContextPath()).willReturn("/openam");
        given(request.getRequestURI()).willReturn("/openam/URI_C");
        given(request.getMethod()).willReturn("PUT");
        given(request.getQueryString()).willReturn("other1=valueA&QUERY_PARAM=VALUEA&other2=valueb");

        given(endpointManager.findEndpoint("/URI_C")).willReturn("/URI_C");

        //When
        boolean matches = endpointMatcher.match(request);

        //Then
        assertTrue(matches);
    }

    @Test
    public void shouldNotMatchEndpointWithQueryParams() {

        //Given
        HttpServletRequest request = mock(HttpServletRequest.class);

        given(request.getContextPath()).willReturn("/openam");
        given(request.getRequestURI()).willReturn("/openam/URI_C");
        given(request.getMethod()).willReturn("PUT");
        given(request.getQueryString()).willReturn("other1=valueA&QUERY_PARAM=VALUEC&other2=valueb");

        given(endpointManager.findEndpoint("/URI_C")).willReturn("/URI_C");

        //When
        boolean matches = endpointMatcher.match(request);

        //Then
        assertFalse(matches);
    }

    @Test
    public void shouldMatchEndpointWithPathInfo() {

        //Given
        HttpServletRequest request = mock(HttpServletRequest.class);

        given(request.getPathInfo()).willReturn("/URI_A");
        given(request.getMethod()).willReturn("GET");

        given(endpointManager.findEndpoint("/URI_A")).willReturn("/URI_A");

        //When
        boolean matches = endpointMatcher.match(request);

        //Then
        assertTrue(matches);
    }

    @Test
    public void shouldMatchEndpointWithResourceNameAndId() {

        //Given
        HttpServletRequest request = mock(HttpServletRequest.class);

        given(request.getPathInfo()).willReturn("/URI_A/URI_B");
        given(request.getMethod()).willReturn("GET");

        given(endpointManager.findEndpoint("/URI_A/URI_B")).willReturn("/URI_A");

        //When
        boolean matches = endpointMatcher.match(request);

        //Then
        assertTrue(matches);
    }
}
