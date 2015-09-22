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

package org.forgerock.oauth2.restlet;

import static java.util.Collections.*;
import static org.assertj.core.api.Assertions.*;
import static org.forgerock.json.JsonValue.*;
import static org.forgerock.openam.utils.CollectionUtils.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.anySet;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.notNull;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.http.HttpServletRequest;

import org.forgerock.oauth2.core.ClientRegistrationStore;
import org.forgerock.oauth2.core.DeviceCode;
import org.forgerock.oauth2.core.OAuth2ProviderSettings;
import org.forgerock.oauth2.core.OAuth2ProviderSettingsFactory;
import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.oauth2.core.OAuth2RequestFactory;
import org.forgerock.oauth2.core.TokenStore;
import org.forgerock.oauth2.core.exceptions.BadRequestException;
import org.forgerock.oauth2.core.exceptions.InvalidGrantException;
import org.forgerock.oauth2.core.exceptions.NotFoundException;
import org.forgerock.oauth2.core.exceptions.ServerException;
import org.forgerock.openam.services.baseurl.BaseURLProvider;
import org.forgerock.openam.services.baseurl.BaseURLProviderFactory;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.restlet.Request;
import org.restlet.data.Method;
import org.restlet.data.Reference;
import org.restlet.ext.jackson.JacksonRepresentation;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class DeviceCodeResourceTest {

    private static final DeviceCode DEVICE_CODE = deviceCode();

    private DeviceCodeResource resource;
    @Mock
    private TokenStore tokenStore;
    @Mock
    private ClientRegistrationStore clientRegistrationStore;
    @Mock
    private OAuth2ProviderSettings providerSettings;
    @Mock
    private BaseURLProviderFactory baseURLProviderFactory;
    @Mock
    private Request request;

    @BeforeMethod
    public void setup() throws Exception {
        MockitoAnnotations.initMocks(this);

        when(request.getMethod()).thenReturn(Method.POST);

        resource = spy(new DeviceCodeResource(tokenStore, mockOAuth2RequestFactory(), clientRegistrationStore,
                mockProviderSettingsFactory(), baseURLProviderFactory));

        when(resource.getRequest()).thenReturn(request);
    }

    @Test
    public void shouldIssueCode() throws Exception {
        // Given
        mockDeviceCodeCreation();
        mockRequestRealmAndClientId("REALM", "CLIENT_ID");
        mockProviderSettings();

        // When
        JacksonRepresentation<Map<String, Object>> result =
                (JacksonRepresentation<Map<String, Object>>) resource.issueCode();

        // Then
        verify(clientRegistrationStore).get(eq("CLIENT_ID"), notNull(OAuth2Request.class));
        verifyNoMoreInteractions(baseURLProviderFactory);
        assertThat(result.getObject()).containsOnly(
                entry("device_code", DEVICE_CODE.getDeviceCode()),
                entry("user_code", DEVICE_CODE.getUserCode()),
                entry("interval", 5),
                entry("expires_in", 300),
                entry("verification_url", "URL")
        );
    }

    @Test
    public void shouldIssueCodeWithDefaultUrl() throws Exception {
        // Given
        mockDeviceCodeCreation();
        mockRequestRealmAndClientId("REALM", "CLIENT_ID");
        mockProviderSettingsWithoutUrl();
        mockBaseUrlProvider();

        request.getResourceRef()
                .addQueryParameter("scope", "some scopes")
                .addQueryParameter("nonce", "NONCE")
                .addQueryParameter("response_type", "RESPONSE_TYPE")
                .addQueryParameter("state", "STATE")
                .addQueryParameter("acr_values", "ACR_VALUES")
                .addQueryParameter("prompt", "PROMPT")
                .addQueryParameter("ui_locales", "UI_LOCALES")
                .addQueryParameter("login_hint", "LOGIN_HINT")
                .addQueryParameter("max_age", "55")
                .addQueryParameter("claims", "CLAIMS")
                .addQueryParameter("code_challenge", "CODE_CHALLENGE")
                .addQueryParameter("code_challenge_method", "CODE_CHALLENGE_METHOD");

        // When
        JacksonRepresentation<Map<String, Object>> result =
                (JacksonRepresentation<Map<String, Object>>) resource.issueCode();

        // Then
        verify(clientRegistrationStore).get(eq("CLIENT_ID"), notNull(OAuth2Request.class));
        verify(tokenStore).createDeviceCode(eq(asSet("some", "scopes")), eq("CLIENT_ID"), eq("NONCE"),
                eq("RESPONSE_TYPE"), eq("STATE"), eq("ACR_VALUES"), eq("PROMPT"), eq("UI_LOCALES"), eq("LOGIN_HINT"),
                eq(55), eq("CLAIMS"), any(OAuth2Request.class), eq("CODE_CHALLENGE"), eq("CODE_CHALLENGE_METHOD"));

        assertThat(result.getObject()).containsOnly(
                entry("device_code", DEVICE_CODE.getDeviceCode()),
                entry("user_code", DEVICE_CODE.getUserCode()),
                entry("interval", 5),
                entry("expires_in", 300),
                entry("verification_url", "BASE_URL/oauth2/device/user")
        );
    }

    @Test(expectedExceptions = BadRequestException.class)
    public void shouldNotIssueCodeWithoutClientId() throws Exception {
        // Given
        mockRequestRealmAndClientId("REALM", null);

        // When
        resource.issueCode();

        // Then - exception
    }

    private void mockBaseUrlProvider() {
        BaseURLProvider urlProvider = mock(BaseURLProvider.class);
        given(baseURLProviderFactory.get("REALM")).willReturn(urlProvider);
        given(urlProvider.getURL(any(HttpServletRequest.class))).willReturn("BASE_URL");
    }

    private void mockProviderSettings() throws ServerException {
        given(providerSettings.getVerificationUrl()).willReturn("URL");
        mockProviderSettingsWithoutUrl();
    }

    private void mockProviderSettingsWithoutUrl() throws ServerException {
        given(providerSettings.getDeviceCodeLifetime()).willReturn(300);
        given(providerSettings.getDeviceCodePollInterval()).willReturn(5);
    }

    private void mockRequestRealmAndClientId(String realm, String clientId) {
        given(request.getAttributes()).willReturn(new ConcurrentHashMap<>(singletonMap("realm", (Object) realm)));
        given(request.getResourceRef()).willReturn(new Reference().addQueryParameter("client_id", clientId));
    }

    private void mockDeviceCodeCreation() throws ServerException, NotFoundException {
        given(tokenStore.createDeviceCode(anySet(), anyString(), anyString(), anyString(), anyString(), anyString(),
                anyString(), anyString(), anyString(), anyInt(), anyString(), any(OAuth2Request.class), anyString(),
                anyString())).willReturn(DEVICE_CODE);
    }

    private OAuth2ProviderSettingsFactory mockProviderSettingsFactory() throws NotFoundException {
        OAuth2ProviderSettingsFactory providerSettingsFactory = mock(OAuth2ProviderSettingsFactory.class);
        when(providerSettingsFactory.get(any(OAuth2Request.class))).thenReturn(providerSettings);
        return providerSettingsFactory;
    }

    private OAuth2RequestFactory<Request> mockOAuth2RequestFactory() {
        OAuth2RequestFactory<Request> requestFactory = mock(OAuth2RequestFactory.class);
        when(requestFactory.create(any(Request.class))).then(new Answer<OAuth2Request>() {
            @Override
            public OAuth2Request answer(InvocationOnMock invocation) throws Throwable {
                return new RestletOAuth2Request((Request) invocation.getArguments()[0]);
            }
        });
        return requestFactory;
    }

    private static DeviceCode deviceCode() {
        try {
            return new DeviceCode(json(object(
                    field("tokenName", asSet("device_code")),
                    field("id", asSet("123")),
                    field("user_code", asSet("456")),
                    field("realm", asSet("/")),
                    field("clientID", asSet("CLIENT_ID")))));
        } catch (InvalidGrantException e) {
            throw new IllegalStateException(e);
        }
    }

}