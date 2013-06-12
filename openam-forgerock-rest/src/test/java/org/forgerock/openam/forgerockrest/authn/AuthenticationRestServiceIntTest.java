///*
// * The contents of this file are subject to the terms of the Common Development and
// * Distribution License (the License). You may not use this file except in compliance with the
// * License.
// *
// * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
// * specific language governing permission and limitations under the License.
// *
// * When distributing Covered Software, include this CDDL Header Notice in each file and include
// * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
// * Header, with the fields enclosed by brackets [] replaced by your own identifying
// * information: "Portions copyright [year] [name of copyright owner]".
// *
// * Copyright 2013 ForgeRock Inc.
// */
//
//package org.forgerock.openam.forgerockrest.newauthn;
//
//import com.iplanet.dpro.session.SessionID;
//import com.sun.identity.authentication.AuthContext;
//import com.sun.identity.authentication.service.AuthException;
//import org.forgerock.openam.forgerockrest.authn.core.AuthContextLocalWrapper;
//import org.forgerock.openam.forgerockrest.authn.AuthenticationRestService;
//import org.forgerock.openam.forgerockrest.authn.core.CoreServicesWrapper;
//import org.forgerock.openam.forgerockrest.authn.RestAuthenticationHandler;
//import org.forgerock.openam.forgerockrest.authn.core.LoginAuthenticator;
//import org.forgerock.openam.forgerockrest.authn.RestLoginAuthenticator;
//import org.testng.annotations.BeforeClass;
//import org.testng.annotations.Test;
//
//import javax.security.auth.callback.Callback;
//import javax.servlet.http.HttpServletRequest;
//import javax.servlet.http.HttpServletResponse;
//import javax.ws.rs.core.HttpHeaders;
//
//import static org.mockito.Mockito.*;
//import static org.mockito.BDDMockito.*;
//
//public class AuthenticationRestServiceIntTest {
//
//    private AuthenticationRestService authenticationRestService;
//
//    private RestAuthenticationHandler restAuthenticationHandler;
//    private LoginAuthenticator loginAuthenticator;
//    private CoreServicesWrapper coreServicesWrapper;
//
//    @BeforeClass
//    public void setUp() {
//
//        coreServicesWrapper = mock(CoreServicesWrapper.class);
//        loginAuthenticator = new RestLoginAuthenticator(coreServicesWrapper);
////        restAuthenticationHandler = new RestAuthenticationHandler(loginAuthenticator);
//
//        authenticationRestService = new AuthenticationRestService(restAuthenticationHandler);
//    }
//
//    @Test
//    public void should() throws AuthException {
//
//        //Given
//        HttpHeaders headers = mock(HttpHeaders.class);
//        HttpServletRequest request = mock(HttpServletRequest.class);
//        HttpServletResponse response = mock(HttpServletResponse.class);
//        String authIndexType = null;
//        String authIndexValue = null;
//
//        SessionID sessionID = mock(SessionID.class);
//        AuthContextLocalWrapper authContextLocalWrapper = mock(AuthContextLocalWrapper.class);
//
//        given(coreServicesWrapper.getSessionIDFromRequest(request)).willReturn(sessionID);
//        given(coreServicesWrapper.getAuthContext(request, null, sessionID, false, false)).willReturn(authContextLocalWrapper);
//        given(coreServicesWrapper.isNewRequest(authContextLocalWrapper)).willReturn(true);
//        given(coreServicesWrapper.getEnvMap(request)).willReturn(null);
//
//        Callback[] callbacks1 = new Callback[]{};
//
//        given(authContextLocalWrapper.hasMoreRequirements()).willReturn(true).willReturn(false);
//        given(authContextLocalWrapper.getRequirements()).willReturn(callbacks1);
//        given(authContextLocalWrapper.getStatus()).willReturn(AuthContext.Status.SUCCESS);
//
//        //When
//        authenticationRestService.authenticate(headers, request, response, authIndexType, authIndexValue);
//
//        //Then
//
//    }
//}
