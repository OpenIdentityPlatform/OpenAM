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

package org.forgerock.openam.uma;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

import java.net.URI;
import java.util.Collections;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.forgerock.oauth2.core.exceptions.NotFoundException;
import org.forgerock.oauth2.core.exceptions.ServerException;
import org.mockito.Matchers;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.representation.Representation;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class UmaWellKnownConfigurationEndpointTest {

    private UmaWellKnownConfigurationEndpoint endpoint;

    private UmaProviderSettingsFactory providerSettingsFactory;
    private Response response;

    @BeforeMethod
    public void setup() {

        providerSettingsFactory = mock(UmaProviderSettingsFactory.class);

        endpoint = new UmaWellKnownConfigurationEndpoint(providerSettingsFactory);

        response = mock(Response.class);
        endpoint.setResponse(response);
    }

    private UmaProviderSettings setupProviderSettings() throws NotFoundException, ServerException {
        UmaProviderSettings providerSettings = mock(UmaProviderSettings.class);
        given(providerSettingsFactory.get(Matchers.<Request>anyObject())).willReturn(providerSettings);

        given(providerSettings.getVersion()).willReturn("VERSION");
        given(providerSettings.getIssuer()).willReturn(URI.create("ISSUER"));
        given(providerSettings.getSupportedPATProfiles()).willReturn(Collections.singleton("PAT_PROFILE"));
        given(providerSettings.getSupportedAATProfiles()).willReturn(Collections.singleton("AAT_PROFILE"));
        given(providerSettings.getSupportedRPTProfiles()).willReturn(Collections.singleton("RPT_PROFILE"));
        given(providerSettings.getSupportedPATGrantTypes()).willReturn(Collections.singleton("PAT_GRANT_TYPE"));
        given(providerSettings.getSupportedAATGrantTypes()).willReturn(Collections.singleton("AAT_GRANT_TYPE"));
        given(providerSettings.getTokenEndpoint()).willReturn(URI.create("TOKEN_ENDPOINT"));
        given(providerSettings.getAuthorizationEndpoint()).willReturn(URI.create("AUTHORIZATION_ENDPOINT"));
        given(providerSettings.getTokenIntrospectionEndpoint()).willReturn(URI.create("TOKEN_INTROSPECTION_ENDPOINT"));
        given(providerSettings.getResourceSetRegistrationEndpoint()).willReturn(URI.create("RESOURCE_SET_REGISTRATION_ENDPOINT"));
        given(providerSettings.getPermissionRegistrationEndpoint()).willReturn(URI.create("PERMISSION_REGISTRATION_ENDPOINT"));
        given(providerSettings.getRPTEndpoint()).willReturn(URI.create("RPT_ENDPOINT"));

        return providerSettings;
    }

    private UmaProviderSettings setupProviderSettingsWithOptionalConfiguration() throws NotFoundException, ServerException {
        UmaProviderSettings providerSettings = setupProviderSettings();
        given(providerSettings.getSupportedClaimTokenProfiles())
                .willReturn(Collections.singleton("CLAIM_TOKEN_PROFILE"));
        given(providerSettings.getSupportedUmaProfiles()).willReturn(Collections.singleton(URI.create("UMA_PROFILE")));
        given(providerSettings.getDynamicClientEndpoint()).willReturn(URI.create("DYNAMIC_CLIENT_ENDPOINT"));
        given(providerSettings.getRequestingPartyClaimsEndpoint()).willReturn(URI.create("REQUESTING_PARTY_CLAIMS_ENDPOINT"));
        return providerSettings;
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldGetRequiredUmaConfiguration() throws Exception {

        //Given
        setupProviderSettings();

        //When
        Representation configuration = endpoint.getConfiguration();

        //Then
        Map<String, Object> configurationResponse = (Map<String, Object>) new ObjectMapper()
                .readValue(configuration.getText(), Map.class);
        assertThat(configurationResponse).contains(entry("version", "VERSION"), entry("issuer", "ISSUER"),
                entry("pat_profiles_supported", Collections.singletonList("PAT_PROFILE")),
                entry("aat_profiles_supported", Collections.singletonList("AAT_PROFILE")),
                entry("rpt_profiles_supported", Collections.singletonList("RPT_PROFILE")),
                entry("pat_grant_types_supported", Collections.singletonList("PAT_GRANT_TYPE")),
                entry("aat_grant_types_supported", Collections.singletonList("AAT_GRANT_TYPE")),
                entry("token_endpoint", "TOKEN_ENDPOINT"), entry("authorization_endpoint", "AUTHORIZATION_ENDPOINT"),
                entry("introspection_endpoint", "TOKEN_INTROSPECTION_ENDPOINT"),
                entry("resource_set_registration_endpoint", "RESOURCE_SET_REGISTRATION_ENDPOINT"),
                entry("permission_registration_endpoint", "PERMISSION_REGISTRATION_ENDPOINT"),
                entry("rpt_endpoint", "RPT_ENDPOINT"));

        verifyZeroInteractions(response);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldGetOptionalUmaConfiguration() throws Exception {

        //Given
        setupProviderSettingsWithOptionalConfiguration();

        //When
        Representation configuration = endpoint.getConfiguration();

        //Then
        Map<String, Object> configurationResponse = (Map<String, Object>) new ObjectMapper()
                .readValue(configuration.getText(), Map.class);
        assertThat(configurationResponse).contains(entry("version", "VERSION"), entry("issuer", "ISSUER"),
                entry("pat_profiles_supported", Collections.singletonList("PAT_PROFILE")),
                entry("aat_profiles_supported", Collections.singletonList("AAT_PROFILE")),
                entry("rpt_profiles_supported", Collections.singletonList("RPT_PROFILE")),
                entry("pat_grant_types_supported", Collections.singletonList("PAT_GRANT_TYPE")),
                entry("aat_grant_types_supported", Collections.singletonList("AAT_GRANT_TYPE")),
                entry("token_endpoint", "TOKEN_ENDPOINT"), entry("authorization_endpoint", "AUTHORIZATION_ENDPOINT"),
                entry("introspection_endpoint", "TOKEN_INTROSPECTION_ENDPOINT"),
                entry("resource_set_registration_endpoint", "RESOURCE_SET_REGISTRATION_ENDPOINT"),
                entry("permission_registration_endpoint", "PERMISSION_REGISTRATION_ENDPOINT"),
                entry("rpt_endpoint", "RPT_ENDPOINT"),
                entry("claim_token_profiles_supported", Collections.singletonList("CLAIM_TOKEN_PROFILE")),
                entry("uma_profiles_supported", Collections.singletonList("UMA_PROFILE")),
                entry("dynamic_client_endpoint", "DYNAMIC_CLIENT_ENDPOINT"),
                entry("requesting_party_claims_endpoint", "REQUESTING_PARTY_CLAIMS_ENDPOINT"));

        verifyZeroInteractions(response);
    }

    @Test(expectedExceptions = NotFoundException.class)
    @SuppressWarnings("unchecked")
    public void shouldThrowNotFoundExceptionWhenUmaProviderNotConfigured() throws Exception {

        //Given
        doThrow(NotFoundException.class).when(providerSettingsFactory).get(Matchers.<Request>anyObject());

        //When
        endpoint.getConfiguration();

        //Then
        //Expected NotFoundException
    }
}
