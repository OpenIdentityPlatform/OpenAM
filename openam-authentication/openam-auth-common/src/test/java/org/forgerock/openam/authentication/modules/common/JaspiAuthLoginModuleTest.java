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
 * Copyright 2013-2016 ForgeRock AS.
 * Portions Copyrighted 2025 3A Systems LLC.
 */

package org.forgerock.openam.authentication.modules.common;

import static org.mockito.BDDMockito.any;
import static org.mockito.BDDMockito.eq;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.testng.AssertJUnit.*;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginException;
import javax.security.auth.message.AuthStatus;
import javax.security.auth.message.MessageInfo;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.mockito.Matchers;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.authentication.util.ISAuthConstants;

public class JaspiAuthLoginModuleTest {

    private JaspiAuthLoginModule jaspiAuthLoginModule;

    private boolean processMethodCalled = false;

    private JaspiAuthModuleWrapper jaspiAuthWrapper;

    private Map<String, Object> config = new HashMap<>();

    @BeforeMethod
    public void setUp() {

        jaspiAuthWrapper = mock(JaspiAuthModuleWrapper.class);

        jaspiAuthLoginModule = new JaspiAuthLoginModule("amAuthPersistentCookie", jaspiAuthWrapper) {
            @Override
            public Principal getPrincipal() {
                return null;
            }

            @Override
            protected Map<String, Object> generateConfig(Subject subject, Map sharedState, Map options) {
                return config;
            }

            @Override
            protected boolean process(MessageInfo messageInfo, Subject clientSubject, Callback[] callbacks) throws LoginException {
                processMethodCalled = true;
                return true;
            }
        };

        AMLoginModuleBinder amLoginModuleBinder = mock(AMLoginModuleBinder.class);

        jaspiAuthLoginModule.setAMLoginModule(amLoginModuleBinder);

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        given(amLoginModuleBinder.getHttpServletRequest()).willReturn(request);
        given(amLoginModuleBinder.getHttpServletResponse()).willReturn(response);
    }

    @Test
    public void shouldInitialiseAuthenticationModuleWrapper() throws Exception {

        //Given
        Subject subject = new Subject();
        Map sharedState = new HashMap();
        Map options = new HashMap();

        //When
        jaspiAuthLoginModule.init(subject, sharedState, options);

        //Then
        verify(jaspiAuthWrapper).initialize(any(CallbackHandler.class), eq(config));
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
            jaspiAuthLoginModule.process(callbacks, state);
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

        given(jaspiAuthWrapper.validateRequest(Matchers.<MessageInfo>anyObject(), Matchers.<Subject>anyObject()))
                .willReturn(AuthStatus.SUCCESS);

        //When
        int returnedState = jaspiAuthLoginModule.process(callbacks, state);

        //Then
        assertTrue(processMethodCalled);
        verify(jaspiAuthWrapper).validateRequest(Matchers.<MessageInfo>anyObject(), Matchers.<Subject>anyObject());
        assertEquals(returnedState, ISAuthConstants.LOGIN_SUCCEED);
    }

    @Test
    public void shouldProcessCallbacksWhenValidateRequestReturnsSendSuccess() throws LoginException {

        //Given
        Callback[] callbacks = new Callback[0];
        int state = ISAuthConstants.LOGIN_START;

        given(jaspiAuthWrapper.validateRequest(Matchers.<MessageInfo>anyObject(), Matchers.<Subject>anyObject()))
                .willReturn(AuthStatus.SEND_SUCCESS);

        //When
        int returnedState = jaspiAuthLoginModule.process(callbacks, state);

        //Then
        assertTrue(processMethodCalled);
        verify(jaspiAuthWrapper).validateRequest(Matchers.<MessageInfo>anyObject(), Matchers.<Subject>anyObject());
        assertEquals(returnedState, ISAuthConstants.LOGIN_SUCCEED);
    }

    @Test
    public void shouldProcessCallbacksWhenValidateRequestReturnsSendFailure() throws LoginException {

        //Given
        Callback[] callbacks = new Callback[0];
        int state = ISAuthConstants.LOGIN_START;

        given(jaspiAuthWrapper.validateRequest(Matchers.<MessageInfo>anyObject(), Matchers.<Subject>anyObject()))
                .willReturn(AuthStatus.SEND_FAILURE);

        //When
        boolean exceptionCaught = false;
        AuthLoginException exception = null;
        try {
            jaspiAuthLoginModule.process(callbacks, state);
        } catch (AuthLoginException e) {
            exceptionCaught = true;
            exception = e;
        }

        //Then
        assertTrue(processMethodCalled);
        verify(jaspiAuthWrapper).validateRequest(Matchers.<MessageInfo>anyObject(), Matchers.<Subject>anyObject());
        assertTrue(exceptionCaught);
        assertEquals(exception.getErrorCode(), "authFailed");
    }

    @Test
    public void shouldProcessCallbacksWhenValidateRequestReturnsSendContinue() throws LoginException {

        //Given
        Callback[] callbacks = new Callback[0];
        int state = ISAuthConstants.LOGIN_START;

        given(jaspiAuthWrapper.validateRequest(Matchers.<MessageInfo>anyObject(), Matchers.<Subject>anyObject()))
                .willReturn(AuthStatus.SEND_CONTINUE);

        //When
        int returnedState = jaspiAuthLoginModule.process(callbacks, state);

        //Then
        assertTrue(processMethodCalled);
        verify(jaspiAuthWrapper).validateRequest(Matchers.<MessageInfo>anyObject(), Matchers.<Subject>anyObject());
        assertEquals(returnedState, ISAuthConstants.LOGIN_IGNORE);
    }

}
