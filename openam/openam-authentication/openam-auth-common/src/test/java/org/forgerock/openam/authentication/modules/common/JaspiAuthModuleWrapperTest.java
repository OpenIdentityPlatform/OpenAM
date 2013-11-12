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
 * Copyright 2013 ForgeRock Inc.
 */

package org.forgerock.openam.authentication.modules.common;

import com.iplanet.sso.SSOToken;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.authentication.spi.AuthenticationException;
import com.sun.identity.authentication.util.ISAuthConstants;
import org.mockito.Matchers;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginException;
import javax.security.auth.message.AuthException;
import javax.security.auth.message.AuthStatus;
import javax.security.auth.message.MessageInfo;
import javax.security.auth.message.MessagePolicy;
import javax.security.auth.message.module.ServerAuthModule;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class JaspiAuthModuleWrapperTest {

    private JaspiAuthModuleWrapper<ServerAuthModule> jaspiAuthModuleWrapper;

    private AMLoginModuleBinder amLoginModuleBinder;
    private ServerAuthModule serverAuthModule;
    private Map<String, Object> config = new HashMap<String, Object>();
    private boolean processMethodCalled = false;
    private boolean onLoginSuccessMethodCalled = false;

    @BeforeMethod
    public void setUp() {

        amLoginModuleBinder = mock(AMLoginModuleBinder.class);
        serverAuthModule = mock(ServerAuthModule.class);

        jaspiAuthModuleWrapper = new JaspiAuthModuleWrapper<ServerAuthModule>(serverAuthModule,
                "amAuthPersistentCookie") {

            @Override
            protected Map<String, Object> initialize(Subject subject, Map sharedState, Map options) {
                return config;
            }

            @Override
            protected boolean process(MessageInfo messageInfo, Subject clientSubject, Callback[] callbacks)
                    throws LoginException {
                processMethodCalled = true;
                return true;
            }

            @Override
            protected Map<String, Object> initialize(Map requestParamsMap, HttpServletRequest request,
                    HttpServletResponse response, SSOToken ssoToken) throws AuthenticationException {
                return config;
            }

            @Override
            protected void onLoginSuccess(MessageInfo messageInfo, Map requestParamsMap, HttpServletRequest request,
                    HttpServletResponse response, SSOToken ssoToken) throws AuthenticationException {
                onLoginSuccessMethodCalled = true;
            }

            @Override
            public Principal getPrincipal() {
                return null;
            }
        };

        jaspiAuthModuleWrapper.setAMLoginModule(amLoginModuleBinder);

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        given(amLoginModuleBinder.getHttpServletRequest()).willReturn(request);
        given(amLoginModuleBinder.getHttpServletResponse()).willReturn(response);
    }

    @Test
    public void shouldInitialiseAuthenticationModule() throws AuthException {

        //Given
        Subject subject = new Subject();
        Map sharedState = new HashMap();
        Map options = new HashMap();

        //When
        jaspiAuthModuleWrapper.init(subject, sharedState, options);

        //Then
        verify(serverAuthModule).initialize(Matchers.<MessagePolicy>anyObject(), (MessagePolicy) isNull(),
                Matchers.<CallbackHandler>anyObject(), eq(config));
    }

    @Test
    public void shouldProcessCallbacksAndThrowInvalidStateException() throws LoginException {

        //Given
        Callback[] callbacks = new Callback[0];
        int state = 0;

        //When
        boolean exceptionCaught = false;
        AuthLoginException exception = null;
        try {
            jaspiAuthModuleWrapper.process(callbacks, state);
        } catch (AuthLoginException e) {
            exceptionCaught = true;
            exception = e;
        }

        //Then
        assertTrue(exceptionCaught);
        assertEquals(exception.getErrorCode(), "incorrectState");
    }

    @Test
    public void shouldProcessCallbacksWhenValidateRequestReturnsSuccess() throws LoginException {

        //Given
        Callback[] callbacks = new Callback[0];
        int state = ISAuthConstants.LOGIN_START;

        given(serverAuthModule.validateRequest(Matchers.<MessageInfo>anyObject(), Matchers.<Subject>anyObject(),
                (Subject) isNull())).willReturn(AuthStatus.SUCCESS);

        //When
        int returnedState = jaspiAuthModuleWrapper.process(callbacks, state);

        //Then
        assertTrue(processMethodCalled);
        verify(serverAuthModule).validateRequest(Matchers.<MessageInfo>anyObject(), Matchers.<Subject>anyObject(),
                (Subject) isNull());
        assertEquals(returnedState, ISAuthConstants.LOGIN_SUCCEED);
    }

    @Test
    public void shouldProcessCallbacksWhenValidateRequestReturnsSendSuccess() throws LoginException {

        //Given
        Callback[] callbacks = new Callback[0];
        int state = ISAuthConstants.LOGIN_START;

        given(serverAuthModule.validateRequest(Matchers.<MessageInfo>anyObject(), Matchers.<Subject>anyObject(),
                (Subject) isNull())).willReturn(AuthStatus.SEND_SUCCESS);

        //When
        int returnedState = jaspiAuthModuleWrapper.process(callbacks, state);

        //Then
        assertTrue(processMethodCalled);
        verify(serverAuthModule).validateRequest(Matchers.<MessageInfo>anyObject(), Matchers.<Subject>anyObject(),
                (Subject) isNull());
        assertEquals(returnedState, ISAuthConstants.LOGIN_SUCCEED);
    }

    @Test
    public void shouldProcessCallbacksWhenValidateRequestReturnsSendFailure() throws LoginException {

        //Given
        Callback[] callbacks = new Callback[0];
        int state = ISAuthConstants.LOGIN_START;

        given(serverAuthModule.validateRequest(Matchers.<MessageInfo>anyObject(), Matchers.<Subject>anyObject(),
                (Subject) isNull())).willReturn(AuthStatus.SEND_FAILURE);

        //When
        boolean exceptionCaught = false;
        AuthLoginException exception = null;
        try {
            jaspiAuthModuleWrapper.process(callbacks, state);
        } catch (AuthLoginException e) {
            exceptionCaught = true;
            exception = e;
        }

        //Then
        assertTrue(processMethodCalled);
        verify(serverAuthModule).validateRequest(Matchers.<MessageInfo>anyObject(), Matchers.<Subject>anyObject(),
                (Subject) isNull());
        assertTrue(exceptionCaught);
        assertEquals(exception.getErrorCode(), "authFailed");
    }

    @Test
    public void shouldProcessCallbacksWhenValidateRequestReturnsSendContinue() throws LoginException {

        //Given
        Callback[] callbacks = new Callback[0];
        int state = ISAuthConstants.LOGIN_START;

        given(serverAuthModule.validateRequest(Matchers.<MessageInfo>anyObject(), Matchers.<Subject>anyObject(),
                (Subject) isNull())).willReturn(AuthStatus.SEND_CONTINUE);

        //When
        int returnedState = jaspiAuthModuleWrapper.process(callbacks, state);

        //Then
        assertTrue(processMethodCalled);
        verify(serverAuthModule).validateRequest(Matchers.<MessageInfo>anyObject(), Matchers.<Subject>anyObject(),
                (Subject) isNull());
        assertEquals(returnedState, ISAuthConstants.LOGIN_IGNORE);
    }

    @Test
    public void shouldCallOnLoginSuccessAndThrowAuthenticationExceptionWhenAuthExceptionCaught()
            throws AuthenticationException, AuthException {

        //Given
        Map requestParamsMap = new HashMap();
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        SSOToken ssoToken = mock(SSOToken.class);

        doThrow(AuthException.class).when(serverAuthModule).initialize(Matchers.<MessagePolicy>anyObject(),
                (MessagePolicy) isNull(), Matchers.<CallbackHandler>anyObject(), eq(config));

        //When
        boolean exceptionCaught = false;
        AuthenticationException exception = null;
        try {
            jaspiAuthModuleWrapper.onLoginSuccess(requestParamsMap, request, response, ssoToken);
        } catch (AuthenticationException e) {
            exceptionCaught = true;
            exception = e;
        }

        //Then
        verify(serverAuthModule).initialize(Matchers.<MessagePolicy>anyObject(), (MessagePolicy) isNull(),
                Matchers.<CallbackHandler>anyObject(), eq(config));
        verify(serverAuthModule, never()).secureResponse(Matchers.<MessageInfo>anyObject(), (Subject) isNull());
        assertTrue(exceptionCaught);
        assertEquals(exception.getErrorCode(), "authFailed");
    }

    @Test
    public void shouldCallOnLoginSuccessWhenSecureResponseReturnsSendSuccess() throws AuthenticationException,
            AuthException {

        //Given
        Map requestParamsMap = new HashMap();
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        SSOToken ssoToken = mock(SSOToken.class);

        given(serverAuthModule.secureResponse(Matchers.<MessageInfo>anyObject(), (Subject) isNull()))
                .willReturn(AuthStatus.SEND_SUCCESS);

        //When
        jaspiAuthModuleWrapper.onLoginSuccess(requestParamsMap, request, response, ssoToken);

        //Then
        verify(serverAuthModule).initialize(Matchers.<MessagePolicy>anyObject(), (MessagePolicy) isNull(),
                Matchers.<CallbackHandler>anyObject(), eq(config));
        assertTrue(onLoginSuccessMethodCalled);
        verify(serverAuthModule).secureResponse(Matchers.<MessageInfo>anyObject(), (Subject) isNull());
    }

    @Test
    public void shouldCallOnLoginSuccessWhenSecureResponseReturnsSendFailure() throws AuthenticationException,
            AuthException {

        //Given
        Map requestParamsMap = new HashMap();
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        SSOToken ssoToken = mock(SSOToken.class);

        given(serverAuthModule.secureResponse(Matchers.<MessageInfo>anyObject(), (Subject) isNull()))
                .willReturn(AuthStatus.SEND_FAILURE);

        //When
        boolean exceptionCaught = false;
        AuthenticationException exception = null;
        try {
            jaspiAuthModuleWrapper.onLoginSuccess(requestParamsMap, request, response, ssoToken);
        } catch (AuthenticationException e) {
            exceptionCaught = true;
            exception = e;
        }

        //Then
        verify(serverAuthModule).initialize(Matchers.<MessagePolicy>anyObject(), (MessagePolicy) isNull(),
                Matchers.<CallbackHandler>anyObject(), eq(config));
        assertTrue(onLoginSuccessMethodCalled);
        verify(serverAuthModule).secureResponse(Matchers.<MessageInfo>anyObject(), (Subject) isNull());
        assertTrue(exceptionCaught);
        assertEquals(exception.getErrorCode(), "authFailed");
    }

    @Test
    public void shouldCallOnLoginSuccessWhenSecureResponseReturnsSendContinue() throws AuthenticationException,
            AuthException {

        //Given
        Map requestParamsMap = new HashMap();
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        SSOToken ssoToken = mock(SSOToken.class);

        given(serverAuthModule.secureResponse(Matchers.<MessageInfo>anyObject(), (Subject) isNull()))
                .willReturn(AuthStatus.SEND_CONTINUE);

        //When
        boolean exceptionCaught = false;
        AuthenticationException exception = null;
        try {
            jaspiAuthModuleWrapper.onLoginSuccess(requestParamsMap, request, response, ssoToken);
        } catch (AuthenticationException e) {
            exceptionCaught = true;
            exception = e;
        }

        //Then
        verify(serverAuthModule).initialize(Matchers.<MessagePolicy>anyObject(), (MessagePolicy) isNull(),
                Matchers.<CallbackHandler>anyObject(), eq(config));
        assertTrue(onLoginSuccessMethodCalled);
        verify(serverAuthModule).secureResponse(Matchers.<MessageInfo>anyObject(), (Subject) isNull());
        assertTrue(exceptionCaught);
        assertEquals(exception.getErrorCode(), "authFailed");
    }

    @Test
    public void shouldCallOnLoginSuccessWhenSecureResponseReturnsElse() throws AuthenticationException, AuthException {

        //Given
        Map requestParamsMap = new HashMap();
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        SSOToken ssoToken = mock(SSOToken.class);

        given(serverAuthModule.secureResponse(Matchers.<MessageInfo>anyObject(), (Subject) isNull()))
                .willReturn(AuthStatus.SUCCESS);

        //When
        boolean exceptionCaught = false;
        AuthenticationException exception = null;
        try {
            jaspiAuthModuleWrapper.onLoginSuccess(requestParamsMap, request, response, ssoToken);
        } catch (AuthenticationException e) {
            exceptionCaught = true;
            exception = e;
        }

        //Then
        verify(serverAuthModule).initialize(Matchers.<MessagePolicy>anyObject(), (MessagePolicy) isNull(),
                Matchers.<CallbackHandler>anyObject(), eq(config));
        assertTrue(onLoginSuccessMethodCalled);
        verify(serverAuthModule).secureResponse(Matchers.<MessageInfo>anyObject(), (Subject) isNull());
        assertTrue(exceptionCaught);
        assertEquals(exception.getErrorCode(), "authFailed");
    }

    @Test
    public void shouldCallOnLoginFailureAndDoNothing() throws AuthenticationException {

        //Given
        Map requestParamsMap = new HashMap();
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        //When
        jaspiAuthModuleWrapper.onLoginFailure(requestParamsMap, request, response);

        //Then
        verifyZeroInteractions(amLoginModuleBinder, serverAuthModule);
    }

    @Test
    public void shouldCallOnLogoutAndDoNothing() {

        //Given
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        SSOToken ssoToken = mock(SSOToken.class);

        //When
        jaspiAuthModuleWrapper.onLogout(request, response, ssoToken);

        //Then
        verifyZeroInteractions(amLoginModuleBinder, serverAuthModule);
    }
}
