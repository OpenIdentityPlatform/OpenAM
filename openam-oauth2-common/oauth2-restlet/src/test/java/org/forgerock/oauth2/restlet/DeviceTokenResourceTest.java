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

import static java.util.Collections.singletonMap;
import static org.forgerock.json.JsonValue.*;
import static org.assertj.core.api.Assertions.*;
import static org.forgerock.openam.utils.CollectionUtils.asSet;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.forgerock.json.JsonValue;
import org.forgerock.oauth2.core.ClientRegistration;
import org.forgerock.oauth2.core.ClientRegistrationStore;
import org.forgerock.oauth2.core.DeviceCode;
import org.forgerock.oauth2.core.OAuth2ProviderSettings;
import org.forgerock.oauth2.core.OAuth2ProviderSettingsFactory;
import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.oauth2.core.OAuth2RequestFactory;
import org.forgerock.oauth2.core.TokenStore;
import org.forgerock.oauth2.core.exceptions.ClientAuthenticationFailureFactory;
import org.forgerock.oauth2.core.exceptions.InvalidClientException;
import org.forgerock.oauth2.core.exceptions.InvalidGrantException;
import org.forgerock.oauth2.core.exceptions.NotFoundException;
import org.forgerock.oauth2.core.exceptions.ServerException;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.restlet.Request;
import org.restlet.data.Method;
import org.restlet.data.Reference;
import org.restlet.ext.jackson.JacksonRepresentation;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class DeviceTokenResourceTest {

    private DeviceTokenResource resource;
    @Mock
    private TokenStore tokenStore;
    @Mock
    private ClientRegistrationStore clientRegistrationStore;
    @Mock
    private OAuth2ProviderSettings providerSettings;
    @Mock
    private Request request;

    @BeforeMethod
    public void setup() throws Exception {
        MockitoAnnotations.initMocks(this);

        ClientAuthenticationFailureFactory failureFactory = mock(ClientAuthenticationFailureFactory.class);
        InvalidClientException expectedResult = mock(InvalidClientException.class);
        when(expectedResult.getError()).thenReturn(new String("invalid_client"));
        when(failureFactory.getException()).thenReturn(expectedResult);
        when(failureFactory.getException(anyString())).thenReturn(expectedResult);
        when(failureFactory.getException(any(OAuth2Request.class), anyString())).thenReturn(expectedResult);

        when(request.getMethod()).thenReturn(Method.POST);

        resource = spy(new DeviceTokenResource(tokenStore, mockOAuth2RequestFactory(), clientRegistrationStore,
                mockProviderSettingsFactory(), null, failureFactory));

        when(providerSettings.getDeviceCodePollInterval()).thenReturn(5);

        when(resource.getRequest()).thenReturn(request);
    }

    @Test
    public void shouldIssueTokens() throws Exception {
        // Given
        mockRequestRealmClientIdClientSecretAndCode("REALM", "CLIENT_ID", "CLIENT_SECRET", "CODE");
        mockClientRegistration();
        mockDeviceCodeRead(issuedDeviceCode());

        // When
        JacksonRepresentation<Map<String, Object>> result =
                (JacksonRepresentation<Map<String, Object>>) resource.issueTokens(null);

        // Then
        assertThat(result.getObject()).containsOnly(entry("access_token", "TOKEN"));
    }

    @Test
    public void shouldCatchInvalidClients() throws Exception {
        // Given
        InvalidClientException expectedResult = mock(InvalidClientException.class);
        when(expectedResult.getError()).thenReturn(new String("invalid_client"));

        mockRequestRealmClientIdClientSecretAndCode("REALM", "CLIENT_ID", "CLIENT_SECRET", "CODE");
        given(clientRegistrationStore.get(anyString(), any(OAuth2Request.class)))
                .willThrow(expectedResult);

        // When
        try {
            resource.issueTokens(null);

            // Then - exception
            fail("Should have exception");
        } catch (OAuth2RestletException e) {
            assertThat(e.getError().equals("invalid_client"));
        }
    }

    @DataProvider
    public Object[][] invalidParameters() {
        return new Object[][] {
                { "CLIENT_ID", "CLIENT_SECRET", null },
                { "CLIENT_ID", null, "CODE" },
                { null, "CLIENT_SECRET", "CODE" },
        };
    }

    @Test(dataProvider = "invalidParameters")
    public void shouldCatchInvalidParameters(String clientId, String clientSecret, String code) throws Exception {
        // Given
        mockRequestRealmClientIdClientSecretAndCode("REALM", clientId, clientSecret, code);

        // When
        try {
            resource.issueTokens(null);

            // Then - exception
            fail("Should have exception");
        } catch (OAuth2RestletException e) {
            assertThat(e.getError().equals("bad_request"));
        }
    }

    @Test
    public void shouldCatchInvalidClientSecret() throws Exception {
        // Given
        mockRequestRealmClientIdClientSecretAndCode("REALM", "CLIENT_ID", "SECRET", "CODE");
        mockClientRegistration();

        // When
        try {
            resource.issueTokens(null);

            // Then - exception
            fail("Should have exception");
        } catch (OAuth2RestletException e) {
            assertThat(e.getError().equals("invalid_client"));
        }
    }

    @Test
    public void shouldCatchNonExistingDeviceCode() throws Exception {
        // Given
        mockRequestRealmClientIdClientSecretAndCode("REALM", "CLIENT_ID", "CLIENT_SECRET", "CODE");
        mockClientRegistration();
        mockDeviceCodeRead(null);

        // When
        try {
            resource.issueTokens(null);

            // Then - exception
            fail("Should have exception");
        } catch (OAuth2RestletException e) {
            assertThat(e.getError().equals("authorization_declined"));
        }
    }

    @Test
    public void shouldCatchExpiredDeviceCode() throws Exception {
        // Given
        mockRequestRealmClientIdClientSecretAndCode("REALM", "CLIENT_ID", "CLIENT_SECRET", "CODE");
        mockClientRegistration();
        mockDeviceCodeRead(deviceCode(field("expireTime", asSet("1"))));

        // When
        try {
            resource.issueTokens(null);

            // Then - exception
            fail("Should have exception");
        } catch (OAuth2RestletException e) {
            assertThat(e.getError().equals("bad_request"));
        }
    }

    @Test
    public void shouldCatchPendingDeviceCode() throws Exception {
        // Given
        mockRequestRealmClientIdClientSecretAndCode("REALM", "CLIENT_ID", "CLIENT_SECRET", "CODE");
        mockClientRegistration();
        mockDeviceCodeRead(deviceCode(field("expireTime", asSet(String.valueOf(System.currentTimeMillis() + 5000)))));

        // When
        try {
            resource.issueTokens(null);

            // Then - exception
            fail("Should have exception");
        } catch (OAuth2RestletException e) {
            assertThat(e.getError().equals("authorization_pending"));
        }
    }

    @Test
    public void shouldCatchTooRapidRequests() throws Exception {
        // Given
        mockRequestRealmClientIdClientSecretAndCode("REALM", "CLIENT_ID", "CLIENT_SECRET", "CODE");
        mockClientRegistration();
        mockDeviceCodeRead(deviceCode(
                field("expireTime", asSet(String.valueOf(System.currentTimeMillis() + 5000))),
                field("lastQueried", asSet(String.valueOf(System.currentTimeMillis())))
                ));

        // When
        try {
            resource.issueTokens(null);

            // Then - exception
            fail("Should have exception");
        } catch (OAuth2RestletException e) {
            assertThat(e.getError().equals("bad_request"));
        }
    }

    private void mockClientRegistration() throws Exception {
        ClientRegistration registration = mock(ClientRegistration.class);
        given(clientRegistrationStore.get(eq("CLIENT_ID"), any(OAuth2Request.class))).willReturn(registration);
        given(registration.getClientSecret()).willReturn("CLIENT_SECRET");
    }

    private void mockDeviceCodeRead(DeviceCode deviceCode) throws ServerException, NotFoundException, InvalidGrantException {
        given(tokenStore.readDeviceCode(eq("CLIENT_ID"), eq("CODE"), any(OAuth2Request.class))).willReturn(deviceCode);
    }

    private void mockRequestRealmClientIdClientSecretAndCode(String realm, String clientId, String clientSecret,
            String code) {
        given(request.getAttributes()).willReturn(new ConcurrentHashMap<>(singletonMap("realm", (Object) realm)));
        given(request.getResourceRef()).willReturn(new Reference()
                .addQueryParameter("client_id", clientId)
                .addQueryParameter("client_secret", clientSecret)
                .addQueryParameter("code", code));
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

    private static DeviceCode issuedDeviceCode(Map.Entry<String, Object>... fields) {
        DeviceCode deviceCode = deviceCode();
        deviceCode.setTokens(singletonMap("access_token", "TOKEN"));
        return deviceCode;
    }

    private static DeviceCode deviceCode(Map.Entry<String, Object>... fields) {
        try {
            final JsonValue json = json(object(
                    field("tokenName", asSet("device_code")),
                    field("id", asSet("123")),
                    field("user_code", asSet("456")),
                    field("realm", asSet("REALM")),
                    field("clientID", asSet("CLIENT_ID"))));
            for (Map.Entry<String, Object> field : fields) {
                json.put(field.getKey(), field.getValue());
            }
            return new DeviceCode(json);
        } catch (InvalidGrantException e) {
            throw new IllegalStateException(e);
        }
    }

}