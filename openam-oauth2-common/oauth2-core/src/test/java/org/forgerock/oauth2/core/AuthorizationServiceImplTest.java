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

package org.forgerock.oauth2.core;

import org.forgerock.oauth2.core.exceptions.AccessDeniedException;
import org.forgerock.oauth2.core.exceptions.ResourceOwnerConsentRequired;
import org.mockito.Matchers;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static org.mockito.Mockito.*;
import static org.mockito.BDDMockito.*;
import static org.testng.Assert.assertEquals;

/**
 * @since 12.0.0
 */
public class AuthorizationServiceImplTest {

    private AuthorizationServiceImpl authorizationService;

    private AuthorizeRequestValidator requestValidator;
    private ResourceOwnerSessionValidator resourceOwnerSessionValidator;
    private OAuth2ProviderSettings providerSettings;
    private ResourceOwnerConsentVerifier resourceOwnerConsentVerifier;
    private ClientRegistrationStore clientRegistrationStore;
    private AuthorizationTokenIssuer tokenIssuer;

    @BeforeMethod
    public void setUp() {

        requestValidator = mock(AuthorizeRequestValidator.class);
        List<AuthorizeRequestValidator> requestValidators = new ArrayList<AuthorizeRequestValidator>();
        requestValidators.add(requestValidator);
        resourceOwnerSessionValidator = mock(ResourceOwnerSessionValidator.class);
        OAuth2ProviderSettingsFactory providerSettingsFactory = mock(OAuth2ProviderSettingsFactory.class);
        resourceOwnerConsentVerifier = mock(ResourceOwnerConsentVerifier.class);
        clientRegistrationStore = mock(ClientRegistrationStore.class);
        tokenIssuer = mock(AuthorizationTokenIssuer.class);

        authorizationService = new AuthorizationServiceImpl(requestValidators, resourceOwnerSessionValidator,
                providerSettingsFactory, resourceOwnerConsentVerifier, clientRegistrationStore, tokenIssuer);

        providerSettings = mock(OAuth2ProviderSettings.class);
        given(providerSettingsFactory.get(Matchers.<OAuth2Request>anyObject())).willReturn(providerSettings);
    }

    @Test (expectedExceptions = ResourceOwnerConsentRequired.class)
    public void authorizeShouldThrowResourceOwnerConsentRequiredExceptionWhenResourceOwnerConsentRequired()
            throws Exception {

        //Given
        OAuth2Request request = mock(OAuth2Request.class);
        ResourceOwner resourceOwner = mock(ResourceOwner.class);
        ClientRegistration clientRegistration = mock(ClientRegistration.class);
        Set<String> validatedScope = new HashSet<String>();

        given(request.getLocale()).willReturn(Locale.ENGLISH);
        given(resourceOwnerSessionValidator.validate(request)).willReturn(resourceOwner);
        given(clientRegistrationStore.get(anyString(), eq(request))).willReturn(clientRegistration);
        given(providerSettings.validateAuthorizationScope(eq(clientRegistration), anySetOf(String.class), eq(request)))
                .willReturn(validatedScope);
        given(providerSettings.isConsentSaved(eq(resourceOwner), anyString(), eq(validatedScope))).willReturn(false);
        given(resourceOwnerConsentVerifier.verify(anyBoolean(), eq(request))).willReturn(false);

        //When
        authorizationService.authorize(request);

        //Then
        // Expect ResourceOwnerConsentRequired
    }

    @Test
    public void authorizeShouldIssueAuthorizationTokensWhenResourceOwnerHasSavedConsent() throws Exception {

        //Given
        OAuth2Request request = mock(OAuth2Request.class);
        ResourceOwner resourceOwner = mock(ResourceOwner.class);
        ClientRegistration clientRegistration = mock(ClientRegistration.class);
        Set<String> validatedScope = new HashSet<String>();

        given(resourceOwnerSessionValidator.validate(request)).willReturn(resourceOwner);
        given(clientRegistrationStore.get(anyString(), eq(request))).willReturn(clientRegistration);
        given(providerSettings.validateAuthorizationScope(eq(clientRegistration), anySetOf(String.class), eq(request)))
                .willReturn(validatedScope);
        given(providerSettings.isConsentSaved(eq(resourceOwner), anyString(), eq(validatedScope))).willReturn(false);
        given(resourceOwnerConsentVerifier.verify(anyBoolean(), eq(request))).willReturn(true);

        //When
        authorizationService.authorize(request);

        //Then
        verify(requestValidator).validateRequest(request);
        verify(tokenIssuer).issueTokens(request, clientRegistration, resourceOwner, validatedScope, providerSettings);
    }

    @Test (expectedExceptions = AccessDeniedException.class)
    public void authorizeShouldThrowAccessDeniedExceptionWhenResourceOwnerDoesNotGiveConsent() throws Exception {

        //Given
        OAuth2Request request = mock(OAuth2Request.class);
        boolean consentGiven = false;
        boolean saveConsent = false;
        ResourceOwner resourceOwner = mock(ResourceOwner.class);
        ClientRegistration clientRegistration = mock(ClientRegistration.class);

        given(clientRegistrationStore.get(anyString(), eq(request))).willReturn(clientRegistration);
        given(resourceOwnerSessionValidator.validate(request)).willReturn(resourceOwner);

        //When
        authorizationService.authorize(request, consentGiven, saveConsent);

        //Then
        // Expect AccessDeniedException
    }

    @Test
    public void authorizeShouldIssueAuthorizationTokensWhenResourceOwnerHasGivenConsent() throws Exception {

        //Given
        OAuth2Request request = mock(OAuth2Request.class);
        boolean consentGiven = true;
        boolean saveConsent = false;
        ResourceOwner resourceOwner = mock(ResourceOwner.class);
        ClientRegistration clientRegistration = mock(ClientRegistration.class);

        given(resourceOwnerSessionValidator.validate(request)).willReturn(resourceOwner);
        given(clientRegistrationStore.get(anyString(), eq(request))).willReturn(clientRegistration);

        //When
        authorizationService.authorize(request, consentGiven, saveConsent);

        //Then
        verify(requestValidator).validateRequest(request);
        verify(providerSettings, never()).saveConsent(eq(resourceOwner), anyString(), anySetOf(String.class));
        verify(tokenIssuer).issueTokens(eq(request), eq(clientRegistration), eq(resourceOwner), anySetOf(String.class),
                eq(providerSettings));
    }

    @Test
    public void authorizeShouldSaveConsentWhenResourceOwnerRequests() throws Exception {

        //Given
        OAuth2Request request = mock(OAuth2Request.class);
        boolean consentGiven = true;
        boolean saveConsent = true;
        ResourceOwner resourceOwner = mock(ResourceOwner.class);
        ClientRegistration clientRegistration = mock(ClientRegistration.class);

        given(resourceOwnerSessionValidator.validate(request)).willReturn(resourceOwner);
        given(clientRegistrationStore.get(anyString(), eq(request))).willReturn(clientRegistration);

        //When
        authorizationService.authorize(request, consentGiven, saveConsent);

        //Then
        verify(requestValidator).validateRequest(request);
        verify(providerSettings).saveConsent(eq(resourceOwner), anyString(), anySetOf(String.class));
        verify(tokenIssuer).issueTokens(eq(request), eq(clientRegistration), eq(resourceOwner), anySetOf(String.class),
                eq(providerSettings));
    }

    @Test
    public void authorizeShouldThrowAccessDeniedExceptionWithFragmentParameters() throws Exception {

        //Given
        OAuth2Request request = mock(OAuth2Request.class);
        given(request.getParameter("response_type")).willReturn("id_token");
        boolean consentGiven = false;
        boolean saveConsent = false;
        ResourceOwner resourceOwner = mock(ResourceOwner.class);
        ClientRegistration clientRegistration = mock(ClientRegistration.class);

        given(clientRegistrationStore.get(anyString(), eq(request))).willReturn(clientRegistration);
        given(clientRegistration.getAllowedScopes()).willReturn(Collections.singleton("openid"));
        given(resourceOwnerSessionValidator.validate(request)).willReturn(resourceOwner);

        //When
        try {
            authorizationService.authorize(request, consentGiven, saveConsent);
        } catch(AccessDeniedException e) {
            //Then
            assertEquals(e.getParameterLocation(), OAuth2Constants.UrlLocation.FRAGMENT);
        }
    }

    @Test
    public void authorizeShouldThrowAccessDeniedExceptionWithQueryParameters() throws Exception {

        //Given
        OAuth2Request request = mock(OAuth2Request.class);
        given(request.getParameter("response_type")).willReturn("code");
        boolean consentGiven = false;
        boolean saveConsent = false;
        ResourceOwner resourceOwner = mock(ResourceOwner.class);
        ClientRegistration clientRegistration = mock(ClientRegistration.class);

        given(clientRegistrationStore.get(anyString(), eq(request))).willReturn(clientRegistration);
        given(resourceOwnerSessionValidator.validate(request)).willReturn(resourceOwner);

        //When
        try {
            authorizationService.authorize(request, consentGiven, saveConsent);
        } catch(AccessDeniedException e) {
            //Then
            assertEquals(e.getParameterLocation(), OAuth2Constants.UrlLocation.QUERY);
        }
    }
}
