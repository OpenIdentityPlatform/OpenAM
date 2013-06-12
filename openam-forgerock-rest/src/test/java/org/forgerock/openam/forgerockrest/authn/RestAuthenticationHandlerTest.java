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

package org.forgerock.openam.forgerockrest.authn;

import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.shared.locale.L10NMessageImpl;
import org.forgerock.json.jwt.JwtBuilder;
import org.forgerock.openam.forgerockrest.authn.callbackhandlers.RestAuthCallbackHandlerResponseException;
import org.forgerock.openam.forgerockrest.authn.core.LoginAuthenticator;
import org.json.JSONException;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.security.SignatureException;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class RestAuthenticationHandlerTest {

    private RestAuthenticationHandler restAuthenticationHandler;

    private LoginAuthenticator loginAuthenticator;
    private RestAuthCallbackHandlerManager restAuthCallbackHandlerManager;
    private JwtBuilder jwtBuilder;
    private AMAuthErrorCodeResponseStatusMapping amAuthErrorCodeResponseStatusMapping;
    private AuthIdHelper authIdHelper;

    @BeforeMethod
    public void setUp() {

        loginAuthenticator = mock(LoginAuthenticator.class);
        restAuthCallbackHandlerManager = mock(RestAuthCallbackHandlerManager.class);
        jwtBuilder = mock(JwtBuilder.class);
        amAuthErrorCodeResponseStatusMapping = mock(AMAuthErrorCodeResponseStatusMapping.class);
        authIdHelper = mock(AuthIdHelper.class);

        restAuthenticationHandler = new RestAuthenticationHandler(loginAuthenticator, restAuthCallbackHandlerManager,
                jwtBuilder, amAuthErrorCodeResponseStatusMapping, authIdHelper);
    }

    @Test
    public void shouldAuthenticateSuccessfullyWithNoIndexTypeRealmOrReqs() throws AuthLoginException, L10NMessageImpl,
            JSONException {

        //Given


        //When
//        restAuthenticationHandler.authenticate()

        //Then

    }


    @Test
    public void shouldAuthenticateSuccessfullyWithNoIndexTypeAndReqs() throws AuthLoginException, L10NMessageImpl,
            JSONException {

    }


    @Test
    public void shouldAuthenticateSuccessfullyWithIndexTypeButNoRealmOrReqs() throws AuthLoginException,
            L10NMessageImpl, JSONException {

    }


    @Test
    public void shouldAuthenticateSuccessfullyRequirementsInternally() throws AuthLoginException, L10NMessageImpl,
            JSONException, RestAuthCallbackHandlerResponseException {

    }


    @Test
    public void shouldAuthenticateSuccessfullyRequirementsExternally() throws AuthLoginException, L10NMessageImpl,
            JSONException, SignatureException, RestAuthCallbackHandlerResponseException, IOException {

    }


    @Test
    public void shouldFailAuthenticationWhenAuthStatusIsNotSuccessful() throws AuthLoginException, L10NMessageImpl,
            JSONException {

    }


    @Test
    public void shouldFailAuthenticationWhenL10MessageImplThrown() throws AuthLoginException, L10NMessageImpl,
            JSONException {

    }


    @Test
    public void shouldFailAuthenticationWhenAuthLoginExceptionThrown() throws AuthLoginException, L10NMessageImpl,
            JSONException {

    }


    @Test
    public void shouldProcessAuthenticationRequirementsWithSuccessfulAuthentication() throws AuthLoginException,
            L10NMessageImpl, JSONException, SignatureException {

    }
}
