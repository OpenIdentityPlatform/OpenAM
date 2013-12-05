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
 * Copyright 2013 ForgeRock AS.
 */

package org.forgerock.openam.jaspi.filter;

import org.forgerock.json.resource.NotFoundException;
import org.forgerock.openam.forgerockrest.RestDispatcher;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.servlet.http.HttpServletRequest;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.*;
import static org.mockito.BDDMockito.*;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class EndpointMatcherTest {

    private EndpointMatcher endpointMatcher;

    private RestDispatcher restDispatcher;

    @BeforeMethod
    public void setUp() {

        restDispatcher = mock(RestDispatcher.class);

        endpointMatcher = new EndpointMatcher("", restDispatcher);

        endpointMatcher.endpoint("/URI_A", "GET");
        endpointMatcher.endpoint("/URI_B", "POST");
        endpointMatcher.endpoint("/URI_C", "PUT", "QUERY_PARAM", "VALUEA", "VALUEB");
        endpointMatcher.endpoint("/URI_B/URI_A", "GET");
    }

    @Test
    public void shouldMatchEndpointWithNoQueryParams() throws NotFoundException {

        //Given
        HttpServletRequest request = mock(HttpServletRequest.class);

        given(request.getContextPath()).willReturn("/openam");
        given(request.getRequestURI()).willReturn("/openam/URI_A");
        given(request.getMethod()).willReturn("GET");
        Map<String, String> details = new HashMap<String, String>();
        details.put("resourceName", "/URI_A");
        given(restDispatcher.getRequestDetails("/URI_A")).willReturn(details);

        //When
        boolean matches = endpointMatcher.match(request);

        //Then
        assertTrue(matches);
    }

    @Test
    public void shouldNotMatchEndpointWithNoQueryParams() throws NotFoundException {

        //Given
        HttpServletRequest request = mock(HttpServletRequest.class);

        given(request.getContextPath()).willReturn("/openam");
        given(request.getRequestURI()).willReturn("/openam/URI_B");
        given(request.getMethod()).willReturn("GET");
        Map<String, String> details = new HashMap<String, String>();
        details.put("resourceName", "/URI_B");
        given(restDispatcher.getRequestDetails("/URI_B")).willReturn(details);

        //When
        boolean matches = endpointMatcher.match(request);

        //Then
        assertFalse(matches);
    }

    @Test
    public void shouldMatchEndpointWithQueryParams() throws NotFoundException {

        //Given
        HttpServletRequest request = mock(HttpServletRequest.class);

        given(request.getContextPath()).willReturn("/openam");
        given(request.getRequestURI()).willReturn("/openam/URI_C");
        given(request.getMethod()).willReturn("PUT");
        given(request.getQueryString()).willReturn("other1=valueA&QUERY_PARAM=VALUEA&other2=valueb");
        Map<String, String> details = new HashMap<String, String>();
        details.put("resourceName", "/URI_C");
        given(restDispatcher.getRequestDetails("/URI_C")).willReturn(details);

        //When
        boolean matches = endpointMatcher.match(request);

        //Then
        assertTrue(matches);
    }

    @Test
    public void shouldNotMatchEndpointWithQueryParams() throws NotFoundException {

        //Given
        HttpServletRequest request = mock(HttpServletRequest.class);

        given(request.getContextPath()).willReturn("/openam");
        given(request.getRequestURI()).willReturn("/openam/URI_C");
        given(request.getMethod()).willReturn("PUT");
        given(request.getQueryString()).willReturn("other1=valueA&QUERY_PARAM=VALUEC&other2=valueb");
        Map<String, String> details = new HashMap<String, String>();
        details.put("resourceName", "/URI_C");
        given(restDispatcher.getRequestDetails("/URI_C")).willReturn(details);

        //When
        boolean matches = endpointMatcher.match(request);

        //Then
        assertFalse(matches);
    }

    @Test
    public void shouldMatchEndpointWithPathInfo() throws NotFoundException {

        //Given
        HttpServletRequest request = mock(HttpServletRequest.class);

        given(request.getPathInfo()).willReturn("/URI_A");
        given(request.getMethod()).willReturn("GET");
        Map<String, String> details = new HashMap<String, String>();
        details.put("resourceName", "/URI_A");
        given(restDispatcher.getRequestDetails("/URI_A")).willReturn(details);

        //When
        boolean matches = endpointMatcher.match(request);

        //Then
        assertTrue(matches);
    }

    @Test
    public void shouldMatchEndpointWithResourceNameAndId() throws NotFoundException {

        //Given
        HttpServletRequest request = mock(HttpServletRequest.class);

        given(request.getPathInfo()).willReturn("/URI_A");
        given(request.getMethod()).willReturn("GET");
        Map<String, String> details = new HashMap<String, String>();
        details.put("resourceId", "URI_A");
        details.put("resourceName", "/URI_B");
        given(restDispatcher.getRequestDetails("/URI_A")).willReturn(details);

        //When
        boolean matches = endpointMatcher.match(request);

        //Then
        assertTrue(matches);
    }
}
