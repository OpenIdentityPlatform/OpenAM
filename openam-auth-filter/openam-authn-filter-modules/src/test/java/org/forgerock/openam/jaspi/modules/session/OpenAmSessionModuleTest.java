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

import org.forgerock.jaspi.runtime.JaspiRuntime;
import org.forgerock.openam.rest.router.RestEndpointManager;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.security.auth.message.AuthException;
import javax.security.auth.message.MessageInfo;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

public class OpenAmSessionModuleTest {

    private OpenAMSessionModule openAmSessionModule;

    private JaspiRuntime jaspiRuntime;
    private RestEndpointManager endpointManager;

    private OptionalSSOTokenSessionModule optionalSSOTokenSessionModule;
    private LocalSSOTokenSessionModule localSSOTokenSessionModule;

    @BeforeMethod
    public void setUp() throws AuthException, ServletException {
        optionalSSOTokenSessionModule = mock(OptionalSSOTokenSessionModule.class);
        localSSOTokenSessionModule = mock(LocalSSOTokenSessionModule.class);
        endpointManager = mock(RestEndpointManager.class);

        openAmSessionModule = new OpenAMSessionModule(endpointManager, optionalSSOTokenSessionModule, localSSOTokenSessionModule);
    }

    @Test
    public void shouldAllowUnauthenticatedRestAuthEndpointWithPOST() throws IOException, ServletException, AuthException {

        //Given
        HttpServletRequest request = mock(HttpServletRequest.class);
        MessageInfo messageInfo = mock(MessageInfo.class);
        given(request.getContextPath()).willReturn("/openam");
        given(request.getRequestURI()).willReturn("/openam/json/authenticate");
        given(request.getMethod()).willReturn("POST");
        given(request.getRequestURL()).willReturn(new StringBuffer("http://example.com:8080/openam"));
        given(request.getContextPath()).willReturn("/openam");
        given(messageInfo.getRequestMessage()).willReturn(request);

        given(endpointManager.findEndpoint("/authenticate")).willReturn("/authenticate");

        //When
        openAmSessionModule.validateRequest(messageInfo, null, null);

        //Then
        verify(localSSOTokenSessionModule, never()).validateRequest(messageInfo, null, null);
    }

    @Test
    public void shouldAllowUnauthenticatedRestUsersEndpointWithPOSTAndActionRegister() throws AuthException {

        //Given
        MessageInfo messageInfo = mock(MessageInfo.class);
        HttpServletRequest request = mock(HttpServletRequest.class);

        given(request.getContextPath()).willReturn("/openam");
        given(request.getRequestURI()).willReturn("/openam/json/users");
        given(request.getQueryString()).willReturn("other1=valueA&_action=register&other2=valueb");
        given(request.getMethod()).willReturn("POST");
        given(messageInfo.getRequestMessage()).willReturn(request);

        given(endpointManager.findEndpoint("/users")).willReturn("/users");

        //When
        openAmSessionModule.validateRequest(messageInfo, null, null);

        //Then
        verify(localSSOTokenSessionModule, never()).validateRequest(messageInfo, null, null);
    }

    @Test
    public void shouldAllowUnauthenticatedRestUsersEndpointWithPOSTAndActionConfirm() throws AuthException {

        //Given
        MessageInfo messageInfo = mock(MessageInfo.class);
        HttpServletRequest request = mock(HttpServletRequest.class);

        given(request.getContextPath()).willReturn("/openam");
        given(request.getRequestURI()).willReturn("/openam/json/users");
        given(request.getQueryString()).willReturn("other1=valueA&_action=confirm&other2=valueb");
        given(request.getMethod()).willReturn("POST");
        given(messageInfo.getRequestMessage()).willReturn(request);

        given(endpointManager.findEndpoint("/users")).willReturn("/users");

        //When
        openAmSessionModule.validateRequest(messageInfo, null, null);

        //Then
        verify(localSSOTokenSessionModule, never()).validateRequest(messageInfo, null, null);
    }

    @Test
    public void shouldAllowUnauthenticatedRestUsersEndpointWithPOSTAndActionForgotPassword() throws AuthException {

        //Given
        MessageInfo messageInfo = mock(MessageInfo.class);
        HttpServletRequest request = mock(HttpServletRequest.class);

        given(request.getContextPath()).willReturn("/openam");
        given(request.getRequestURI()).willReturn("/openam/json/users");
        given(request.getQueryString()).willReturn("other1=valueA&_action=forgotPassword&other2=valueb");
        given(request.getMethod()).willReturn("POST");
        given(messageInfo.getRequestMessage()).willReturn(request);

        given(endpointManager.findEndpoint("/users")).willReturn("/users");

        //When
        openAmSessionModule.validateRequest(messageInfo, null, null);

        //Then
        verify(localSSOTokenSessionModule, never()).validateRequest(messageInfo, null, null);
    }

    @Test
    public void shouldAllowUnauthenticatedRestUsersEndpointWithPOSTAndActionForgotPasswordReset() throws AuthException {

        //Given
        MessageInfo messageInfo = mock(MessageInfo.class);
        HttpServletRequest request = mock(HttpServletRequest.class);

        given(request.getContextPath()).willReturn("/openam");
        given(request.getRequestURI()).willReturn("/openam/json/users");
        given(request.getQueryString()).willReturn("other1=valueA&_action=forgotPasswordReset&other2=valueb");
        given(request.getMethod()).willReturn("POST");
        given(messageInfo.getRequestMessage()).willReturn(request);

        given(endpointManager.findEndpoint("/users")).willReturn("/users");

        //When
        openAmSessionModule.validateRequest(messageInfo, null, null);

        //Then
        verify(localSSOTokenSessionModule, never()).validateRequest(messageInfo, null, null);
    }

    @Test
    public void shouldNotAllowUnauthenticatedRestUsersEndpointWithPOSTAndActionForgotIdFromSession() throws AuthException {

        //Given
        MessageInfo messageInfo = mock(MessageInfo.class);
        HttpServletRequest request = mock(HttpServletRequest.class);

        given(request.getContextPath()).willReturn("/openam");
        given(request.getRequestURI()).willReturn("/openam/json/users");
        given(request.getQueryString()).willReturn("other1=valueA&_action=idFromSession&other2=valueb");
        given(request.getMethod()).willReturn("POST");
        given(request.getRequestURL()).willReturn(new StringBuffer("http://www.example.com"));
        given(messageInfo.getRequestMessage()).willReturn(request);

        given(endpointManager.findEndpoint("/users")).willReturn("/users");

        //When
        openAmSessionModule.validateRequest(messageInfo, null, null);

        //Then
        verify(localSSOTokenSessionModule).validateRequest(messageInfo, null, null);
    }

    @Test
    public void shouldNotAllowUnauthenticatedRestUsersEndpointWithGET() throws AuthException {

        //Given
        MessageInfo messageInfo = mock(MessageInfo.class);
        HttpServletRequest request = mock(HttpServletRequest.class);

        given(request.getContextPath()).willReturn("/openam");
        given(request.getRequestURI()).willReturn("/openam/json/users");
        given(request.getMethod()).willReturn("GET");
        given(request.getRequestURL()).willReturn(new StringBuffer("http://www.example.com"));
        given(messageInfo.getRequestMessage()).willReturn(request);

        given(endpointManager.findEndpoint("/users")).willReturn("/users");

        //When
        openAmSessionModule.validateRequest(messageInfo, null, null);

        //Then
        verify(localSSOTokenSessionModule).validateRequest(messageInfo, null, null);
    }

    @Test
    public void shouldNotAllowUnauthenticatedRestUsersEndpointWithPUT() throws AuthException {

        //Given
        MessageInfo messageInfo = mock(MessageInfo.class);
        HttpServletRequest request = mock(HttpServletRequest.class);

        given(request.getContextPath()).willReturn("/openam");
        given(request.getRequestURI()).willReturn("/openam/json/users");
        given(request.getMethod()).willReturn("PUT");
        given(request.getRequestURL()).willReturn(new StringBuffer("http://www.example.com"));
        given(messageInfo.getRequestMessage()).willReturn(request);

        given(endpointManager.findEndpoint("/users")).willReturn("/users");

        //When
        openAmSessionModule.validateRequest(messageInfo, null, null);

        //Then
        verify(localSSOTokenSessionModule).validateRequest(messageInfo, null, null);
    }

    @Test
    public void shouldNotAllowUnauthenticatedRestUsersEndpointWithPATCH() throws AuthException {

        //Given
        MessageInfo messageInfo = mock(MessageInfo.class);
        HttpServletRequest request = mock(HttpServletRequest.class);

        given(request.getContextPath()).willReturn("/openam");
        given(request.getRequestURI()).willReturn("/openam/json/users");
        given(request.getMethod()).willReturn("PATCH");
        given(request.getRequestURL()).willReturn(new StringBuffer("http://www.example.com"));
        given(messageInfo.getRequestMessage()).willReturn(request);

        given(endpointManager.findEndpoint("/users")).willReturn("/users");

        //When
        openAmSessionModule.validateRequest(messageInfo, null, null);

        //Then
        verify(localSSOTokenSessionModule).validateRequest(messageInfo, null, null);
    }

    @Test
    public void shouldNotAllowUnauthenticatedRestUsersEndpointWithDELETE() throws AuthException {

        //Given
        MessageInfo messageInfo = mock(MessageInfo.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain filterChain = mock(FilterChain.class);

        given(request.getContextPath()).willReturn("/openam");
        given(request.getRequestURI()).willReturn("/openam/json/users");
        given(request.getMethod()).willReturn("DELETE");
        given(request.getRequestURL()).willReturn(new StringBuffer("http://www.example.com"));
        given(messageInfo.getRequestMessage()).willReturn(request);

        given(endpointManager.findEndpoint("/users")).willReturn("/users");

        //When
        openAmSessionModule.validateRequest(messageInfo, null, null);

        //Then
        verify(localSSOTokenSessionModule).validateRequest(messageInfo, null, null);
    }
}
