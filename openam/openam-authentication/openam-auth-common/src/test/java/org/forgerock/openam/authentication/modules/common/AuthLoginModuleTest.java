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

import com.sun.identity.authentication.spi.AuthLoginException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.login.LoginException;
import java.security.Principal;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class AuthLoginModuleTest {

    private AuthLoginModule authLoginModule;

    private AMLoginModuleBinder amLoginModuleBinder;

    @BeforeClass
    public void setUp() {

        amLoginModuleBinder = mock(AMLoginModuleBinder.class);

        authLoginModule = new AuthLoginModule() {
            @Override
            public void init(Subject subject, Map sharedState, Map options) {
            }

            @Override
            public int process(Callback[] callbacks, int state) throws LoginException {
                return 0;
            }

            @Override
            public Principal getPrincipal() {
                return null;
            }
        };
        authLoginModule.setAMLoginModule(amLoginModuleBinder);
    }

    @Test
    public void shouldGetCallbackHandler() {

        //Given

        //When
        authLoginModule.getCallbackHandler();

        //Then
        verify(amLoginModuleBinder).getCallbackHandler();
    }

    @Test
    public void shouldGetHttpServletRequest() {

        //Given

        //When
        authLoginModule.getHttpServletRequest();

        //Then
        verify(amLoginModuleBinder).getHttpServletRequest();
    }

    @Test
    public void shouldGetHttpServletResponse() {

        //Given

        //When
        authLoginModule.getHttpServletResponse();

        //Then
        verify(amLoginModuleBinder).getHttpServletResponse();
    }

    @Test
    public void shouldGetRequestOrg() {

        //Given

        //When
        authLoginModule.getRequestOrg();

        //Then
        verify(amLoginModuleBinder).getRequestOrg();
    }

    @Test
    public void shouldSetUserSessionProperty() throws AuthLoginException {

        //Given
        String name = "NAME";
        String value = "VALUE";

        //When
        authLoginModule.setUserSessionProperty(name, value);

        //Then
        verify(amLoginModuleBinder).setUserSessionProperty(name, value);
    }
}
