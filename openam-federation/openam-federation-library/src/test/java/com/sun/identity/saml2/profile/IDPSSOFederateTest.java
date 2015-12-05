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
 * Copyright 2015 ForgeRock AS.
 */

package com.sun.identity.saml2.profile;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.when;

import org.forgerock.openam.saml2.IDPRequestValidator;
import org.forgerock.openam.saml2.IDPSSOFederateRequest;
import org.forgerock.openam.saml2.SAML2ActorFactory;
import org.forgerock.openam.saml2.SAMLAuthenticator;
import org.forgerock.openam.saml2.SAMLAuthenticatorLookup;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;

public class IDPSSOFederateTest {

    @Mock
    private HttpServletRequest mockRequest;

    @Mock
    private HttpServletResponse mockResponse;

    @Mock
    private PrintWriter mockPrintWriter;

    @Mock
    private FederateCookieRedirector cookieRedirector;

    @Mock
    private SAML2ActorFactory actorFactory;

    @Mock
    private IDPRequestValidator validator;

    @Mock
    private SAMLAuthenticator authenticator;

    @Mock
    private SAMLAuthenticatorLookup authenticationLookup;

    private IDPSSOFederate idpSsoFederateRequest;

    @BeforeMethod
    public void initMocks() throws ServerFaultException, ClientFaultException {
        MockitoAnnotations.initMocks(this);
        when(actorFactory.getIDPRequestValidator(Mockito.anyString(), anyBoolean())).thenReturn(validator);
        when(actorFactory.getSAMLAuthenticator(
                any(IDPSSOFederateRequest.class),
                any(HttpServletRequest.class),
                any(HttpServletResponse.class),
                any(PrintWriter.class),
                anyBoolean())).thenReturn(authenticator);
        when(actorFactory.getSAMLAuthenticatorLookup(
                any(IDPSSOFederateRequest.class),
                any(HttpServletRequest.class),
                any(HttpServletResponse.class),
                any(PrintWriter.class))).thenReturn(authenticationLookup);
        idpSsoFederateRequest = new IDPSSOFederate(false, cookieRedirector, actorFactory);
    }

    @Test
    public void shouldBeTestable() throws Exception {
        idpSsoFederateRequest.process(mockRequest, mockResponse, mockPrintWriter, null);
    }

    @Test
    public void shouldNotCallAnyFurtherFunctionsAfterNeedSetLBCookieAndRedirectReturnsTrue() throws Exception {

        // Arrange
        when(cookieRedirector.needSetLBCookieAndRedirect(
                any(HttpServletRequest.class),
                any(HttpServletResponse.class),
                anyBoolean())).thenReturn(true);
        // Act
        idpSsoFederateRequest.process(mockRequest, mockResponse, mockPrintWriter, null);
        // Assert
        Mockito.verifyZeroInteractions(authenticator, authenticationLookup);
    }

    @Test
    public void shouldCallAuthenticateIfThereIsNoRequestId() throws Exception {

        // Arrange
        when(mockRequest.getParameter("ReqID")).thenReturn("");

        when(cookieRedirector.needSetLBCookieAndRedirect(
                any(HttpServletRequest.class),
                any(HttpServletResponse.class),
                anyBoolean())).thenReturn(false);

        // Act
        idpSsoFederateRequest.process(mockRequest, mockResponse, mockPrintWriter, null);

        // Assert
        Mockito.verify(authenticator).authenticate();
        Mockito.verifyZeroInteractions(authenticationLookup);
    }

    @Test
    public void shouldCallAuthenticateLookupIfThereIsARequestId() throws Exception {

        // Arrange
        when(mockRequest.getParameter("ReqID")).thenReturn("12345");

        when(cookieRedirector.needSetLBCookieAndRedirect(
                any(HttpServletRequest.class),
                any(HttpServletResponse.class),
                anyBoolean())).thenReturn(false);

        // Act
        idpSsoFederateRequest.process(mockRequest, mockResponse, mockPrintWriter, null);

        // Assert
        Mockito.verify(authenticationLookup).retrieveAuthenticationFromCache();
        Mockito.verifyZeroInteractions(authenticator);
    }
}