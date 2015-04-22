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
package org.forgerock.openidconnect;

import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.mock;

import java.util.HashSet;
import java.util.Set;
import org.forgerock.oauth2.core.OAuth2Constants;
import org.forgerock.oauth2.core.OAuth2ProviderSettings;
import org.forgerock.oauth2.core.OAuth2ProviderSettingsFactory;
import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.oauth2.core.exceptions.InvalidClientException;
import org.forgerock.oauth2.core.exceptions.NotFoundException;
import org.forgerock.oauth2.core.exceptions.ServerException;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

public class SubjectTypeValidatorTest {

    SubjectTypeValidator subjectTypeValidator;
    OAuth2ProviderSettingsFactory mockProviderSettingsFactory;
    OpenIdConnectClientRegistrationStore mockClientRegistrationStore;

    @BeforeTest
    public void setUp() {
        this.mockProviderSettingsFactory = mock(OAuth2ProviderSettingsFactory.class);
        this.mockClientRegistrationStore = mock(OpenIdConnectClientRegistrationStore.class);
        this.subjectTypeValidator = new SubjectTypeValidator(mockProviderSettingsFactory, mockClientRegistrationStore);
    }

    @Test
    public void shouldValidateRequest() throws InvalidClientException, NotFoundException, ServerException {
        //given
        OAuth2ProviderSettings mockProviderSettings = mock(OAuth2ProviderSettings.class);
        OAuth2Request mockRequest = mock(OAuth2Request.class);
        OpenIdConnectClientRegistration mockClientRegistration = mock(OpenIdConnectClientRegistration.class);

        Set<String> subjectTypesSupported = new HashSet<String>();
        subjectTypesSupported.add("public");

        given(mockProviderSettingsFactory.get(mockRequest)).willReturn(mockProviderSettings);
        given(mockProviderSettings.getSupportedSubjectTypes()).willReturn(subjectTypesSupported);
        given(mockRequest.getParameter(OAuth2Constants.Params.CLIENT_ID)).willReturn("CLIENT_ID");
        given(mockClientRegistrationStore.get("CLIENT_ID", mockRequest)).willReturn(mockClientRegistration);
        given(mockClientRegistration.getSubjectType()).willReturn("public");

        //when
        subjectTypeValidator.validateRequest(mockRequest);

        //then
    }

    @Test (expectedExceptions = InvalidClientException.class)
    public void shouldFailSubjectTypeNotSupported() throws InvalidClientException, NotFoundException, ServerException {
        //given
        OAuth2ProviderSettings mockProviderSettings = mock(OAuth2ProviderSettings.class);
        OAuth2Request mockRequest = mock(OAuth2Request.class);
        OpenIdConnectClientRegistration mockClientRegistration = mock(OpenIdConnectClientRegistration.class);

        Set<String> subjectTypesSupported = new HashSet<String>();
        subjectTypesSupported.add("public");

        given(mockProviderSettingsFactory.get(mockRequest)).willReturn(mockProviderSettings);
        given(mockProviderSettings.getSupportedSubjectTypes()).willReturn(subjectTypesSupported);
        given(mockRequest.getParameter(OAuth2Constants.Params.CLIENT_ID)).willReturn("CLIENT_ID");
        given(mockClientRegistrationStore.get("CLIENT_ID", mockRequest)).willReturn(mockClientRegistration);
        given(mockClientRegistration.getSubjectType()).willReturn("pairwise");

        //when
        subjectTypeValidator.validateRequest(mockRequest);

        //then
    }

}
