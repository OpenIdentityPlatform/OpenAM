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

import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.HashMap;
import java.util.Map;

import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.message.MessagePolicy;
import javax.security.auth.message.module.ServerAuthModule;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.mockito.Matchers;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class JaspiAuthModuleWrapperTest {

    private ServerAuthModule serverAuthModule;
    private JaspiAuthModuleWrapper<ServerAuthModule> jaspiAuthWrapper;

    @BeforeMethod
    public void setUp() {

        AMLoginModuleBinder amLoginModuleBinder = mock(AMLoginModuleBinder.class);
        serverAuthModule = mock(ServerAuthModule.class);

        jaspiAuthWrapper = new JaspiAuthModuleWrapper<ServerAuthModule>(serverAuthModule) {};

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        given(amLoginModuleBinder.getHttpServletRequest()).willReturn(request);
        given(amLoginModuleBinder.getHttpServletResponse()).willReturn(response);
    }

    @Test
    public void shouldInitialiseAuthenticationModule() throws Exception {

        //Given
        CallbackHandler callbackHandler = mock(CallbackHandler.class);
        Map config = new HashMap();

        //When
        jaspiAuthWrapper.initialize(callbackHandler, config);

        //Then
        verify(serverAuthModule).initialize(Matchers.<MessagePolicy>anyObject(), (MessagePolicy) isNull(),
                eq(callbackHandler), eq(config));
    }
}
