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
package org.forgerock.openam.cors;

import java.util.ArrayList;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class CORSServiceTest {

    private CORSService testService;
    private HttpServletRequest mockRequest;
    private HttpServletResponse mockResponse;

    @BeforeMethod
    public void setUp() {
        ArrayList<String> origins = new ArrayList<String>();
        origins.add("www.google.com");
        ArrayList<String> methods = new ArrayList<String>();
        methods.add("POST");
        methods.add("DELETE");
        methods.add("OPTIONS");
        ArrayList<String> acceptedHeaders = new ArrayList<String>();
        acceptedHeaders.add("axe-ept-id-header");
        ArrayList<String> exposedHeaders = new ArrayList<String>();
        exposedHeaders.add("x-posed-header");

        testService = new CORSService(origins, methods, acceptedHeaders,
                exposedHeaders, 999, true, null);

        mockRequest = mock(HttpServletRequest.class);
        mockResponse = mock(HttpServletResponse.class);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void NullOriginsThrowIllegalArgument() {
        //given
        ArrayList<String> list = new ArrayList<String>();
        list.add("one");

        //when
        CORSService service = new CORSService(null, list, list, list, 0, false, null);

        //then

    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void EmptyOriginsThrowIllegalArgument() {
        //given
        ArrayList<String> list = new ArrayList<String>();
        list.add("one");
        ArrayList<String> list2 = new ArrayList<String>();

        //when
        CORSService service = new CORSService(list2, list, list, list, 0, false, null);

        //then
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void NullMethodsThrowIllegalArgument() {
        //given
        ArrayList<String> list = new ArrayList<String>();
        list.add("one");

        //when
        CORSService service = new CORSService(list, null, list, list, 0, false, null);

        //then
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void EmptyMethodsThrowIllegalArgument() {
        //given
        ArrayList<String> list = new ArrayList<String>();
        list.add("one");
        ArrayList<String> list2 = new ArrayList<String>();

        //when
        CORSService service = new CORSService(list, list2, list, list, 0, false, null);

        //then
    }

    @Test
    public void shouldNotTouchResponseAsOriginNull() {
        //given
        given(mockRequest.getHeader(CORSConstants.ORIGIN)).willReturn(null);

        //when
        testService.handleRequest(mockRequest, mockResponse);

        //then
        verifyZeroInteractions(mockResponse);
    }

    @Test
    public void shouldNotTouchResponseAsOriginEmpty() {
        //given
        given(mockRequest.getHeader(CORSConstants.ORIGIN)).willReturn("");

        //when
        testService.handleRequest(mockRequest, mockResponse);

        //then
        verifyZeroInteractions(mockResponse);
    }

    @Test
    public void shouldNotTouchResponseAsOriginInvalid() {
        //given
        given(mockRequest.getHeader(CORSConstants.ORIGIN)).willReturn("www.yahoo.com");

        //when
        testService.handleRequest(mockRequest, mockResponse);

        //then
        verifyZeroInteractions(mockResponse);
    }

    @Test
    public void shouldNotTouchResponseAsOriginCaseInvalid() {
        //given
        given(mockRequest.getHeader(CORSConstants.ORIGIN)).willReturn("www.GOOGLE.com");

        //when
        testService.handleRequest(mockRequest, mockResponse);

        //then
        verifyZeroInteractions(mockResponse);
    }


    @Test
    public void shouldNotTouchResponseAsMethodInvalid() {
        //given
        given(mockRequest.getHeader(CORSConstants.ORIGIN)).willReturn("www.google.com");
        given(mockRequest.getMethod()).willReturn("PUT");

        //when
        testService.handleRequest(mockRequest, mockResponse);

        //then
        verifyZeroInteractions(mockResponse);
    }

    @Test
    public void shouldFollowNormalFlowApplyOriginCredsAndExpose() {
        //given
        given(mockRequest.getHeader(CORSConstants.ORIGIN)).willReturn("www.google.com");
        given(mockRequest.getMethod()).willReturn("POST");

        //when
        testService.handleRequest(mockRequest, mockResponse);

        //then
        verify(mockResponse, times(1)).setHeader(eq(CORSConstants.AC_ALLOW_ORIGIN), anyString());
        verify(mockResponse, times(1)).setHeader(eq(CORSConstants.AC_ALLOW_CREDS), anyString());
        verify(mockResponse, times(1)).setHeader(eq(CORSConstants.VARY), eq(CORSConstants.ORIGIN));
        verify(mockResponse, times(1)).setHeader(eq(CORSConstants.AC_EXPOSE_HEADERS), anyString());
    }

    @Test
    public void shouldFollowNormalFlowJustApplyOrigin() {
        //given
        ArrayList<String> origins = new ArrayList<String>();
        origins.add("*");
        ArrayList<String> methods = new ArrayList<String>();
        methods.add("POST");

        testService = new CORSService(origins, methods, null, null, 0, false, null);

        given(mockRequest.getHeader(CORSConstants.ORIGIN)).willReturn("www.google.com");
        given(mockRequest.getMethod()).willReturn("POST");

        //when
        testService.handleRequest(mockRequest, mockResponse);

        //then
        verify(mockResponse, times(1)).setHeader(eq(CORSConstants.AC_ALLOW_ORIGIN), anyString());
    }

    @Test
    public void shouldFollowPreflightFlow() {
        given(mockRequest.getHeader(CORSConstants.ORIGIN)).willReturn("www.google.com");
        given(mockRequest.getHeader(CORSConstants.AC_REQUEST_METHOD)).willReturn("POST");
        given(mockRequest.getMethod()).willReturn("OPTIONS");

        //when
        testService.handleRequest(mockRequest, mockResponse);

        //then
        verify(mockResponse, times(1)).setHeader(eq(CORSConstants.AC_ALLOW_METHODS), anyString());
        verify(mockResponse, times(1)).setHeader(eq(CORSConstants.AC_ALLOW_HEADERS), anyString());
        verify(mockResponse, times(1)).setIntHeader(eq(CORSConstants.AC_MAX_AGE), anyInt());
        verify(mockResponse, times(1)).setHeader(eq(CORSConstants.AC_ALLOW_ORIGIN), anyString());
        verify(mockResponse, times(1)).setHeader(eq(CORSConstants.AC_ALLOW_CREDS), anyString());
        verify(mockResponse, times(1)).setHeader(eq(CORSConstants.VARY), eq(CORSConstants.ORIGIN));
    }

    @Test
    public void shouldDoNothingIfPreflightAndNotOptions() {
        given(mockRequest.getHeader(CORSConstants.AC_REQUEST_METHOD)).willReturn("POST");
        given(mockRequest.getMethod()).willReturn("GET");

        //when
        testService.handleRequest(mockRequest, mockResponse);

        //then
        verifyZeroInteractions(mockResponse);
    }

    @Test
    public void shouldDoNothingIfPreflightAndNullRequestMethod() {
        given(mockRequest.getHeader(CORSConstants.AC_REQUEST_METHOD)).willReturn(null);
        given(mockRequest.getMethod()).willReturn("GET");

        //when
        testService.handleRequest(mockRequest, mockResponse);

        //then
        verifyZeroInteractions(mockResponse);
    }

    @Test
    public void shouldDoNothingIfPreflightAndEmptyRequestMethod() {
        given(mockRequest.getHeader(CORSConstants.AC_REQUEST_METHOD)).willReturn("");
        given(mockRequest.getMethod()).willReturn("GET");

        //when
        testService.handleRequest(mockRequest, mockResponse);

        //then
        verifyZeroInteractions(mockResponse);
    }

    @Test
    public void testInvalidHostnameFailsValidation() {

        ArrayList<String> origins = new ArrayList<String>();
        origins.add("www.google.com");
        ArrayList<String> methods = new ArrayList<String>();
        methods.add("POST");
        ArrayList<String> acceptedHeaders = new ArrayList<String>();
        acceptedHeaders.add("axe-ept-id-header");
        ArrayList<String> exposedHeaders = new ArrayList<String>();
        exposedHeaders.add("x-posed-header");
        exposedHeaders.add("x-posed-header2");

        testService = new CORSService(origins, methods, acceptedHeaders,
                exposedHeaders, 0, true, "www.openam.com");

        given(mockRequest.getHeader(CORSConstants.ORIGIN)).willReturn("www.google.com");
        given(mockRequest.getMethod()).willReturn("POST");

        //when
        testService.handleRequest(mockRequest, mockResponse);

        //then
        verifyZeroInteractions(mockResponse);
    }


    @Test
    public void testHandleNormalIncludesExposedHeadersInResponse() {

        ArrayList<String> origins = new ArrayList<String>();
        origins.add("www.google.com");
        ArrayList<String> methods = new ArrayList<String>();
        methods.add("POST");
        ArrayList<String> acceptedHeaders = new ArrayList<String>();
        acceptedHeaders.add("axe-ept-id-header");
        ArrayList<String> exposedHeaders = new ArrayList<String>();
        exposedHeaders.add("x-posed-header");
        exposedHeaders.add("x-posed-header2");

        testService = new CORSService(origins, methods, acceptedHeaders,
                exposedHeaders, 0, true, null);

        given(mockRequest.getHeader(CORSConstants.ORIGIN)).willReturn("www.google.com");
        given(mockRequest.getMethod()).willReturn("POST");

        //when
        testService.handleRequest(mockRequest, mockResponse);

        //then
        verify(mockResponse, times(1)).setHeader(eq(CORSConstants.AC_ALLOW_ORIGIN), anyString());
        verify(mockResponse, times(1)).setHeader(eq(CORSConstants.AC_ALLOW_CREDS), anyString());
        verify(mockResponse, times(1)).setHeader(eq(CORSConstants.VARY), eq(CORSConstants.ORIGIN));
        verify(mockResponse, times(1)).setHeader(CORSConstants.AC_EXPOSE_HEADERS,
                "x-posed-header,x-posed-header2");
    }

}
