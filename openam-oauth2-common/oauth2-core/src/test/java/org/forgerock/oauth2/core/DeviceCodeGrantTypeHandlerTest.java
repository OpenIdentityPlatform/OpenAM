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
 * Copyright 2015-2016 ForgeRock AS.
 */

package org.forgerock.oauth2.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Fail.fail;
import static org.forgerock.json.JsonValue.*;
import static org.forgerock.oauth2.core.OAuth2Constants.Bearer.BEARER;
import static org.forgerock.oauth2.core.OAuth2Constants.Custom.CLAIMS;
import static org.forgerock.oauth2.core.OAuth2Constants.Params.*;
import static org.forgerock.oauth2.core.OAuth2Constants.TokenEndpoint.DEVICE_CODE;
import static org.forgerock.openam.utils.CollectionUtils.asSet;
import static org.forgerock.openam.utils.Time.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.eq;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.forgerock.json.JsonValue;
import org.forgerock.oauth2.core.exceptions.ClientAuthenticationFailureFactory;
import org.forgerock.oauth2.core.exceptions.InvalidClientException;
import org.forgerock.oauth2.core.exceptions.InvalidGrantException;
import org.forgerock.oauth2.core.exceptions.NotFoundException;
import org.forgerock.oauth2.core.exceptions.OAuth2Exception;
import org.forgerock.oauth2.core.exceptions.ServerException;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class DeviceCodeGrantTypeHandlerTest {

    private DeviceCodeGrantTypeHandler grantTypeHandler;
    @Mock
    private TokenStore tokenStore;
    @Mock
    private ClientRegistrationStore clientRegistrationStore;
    @Mock
    private OAuth2ProviderSettings providerSettings;
    @Mock
    private OAuth2Uris oAuth2Uris;
    @Mock
    private OAuth2Request request;
    @Mock
    private AccessToken accessToken;
    @Mock
    private RefreshToken refreshToken;
    private GrantTypeAccessTokenGenerator accessTokenGenerator;

    @BeforeMethod
    public void setup() throws Exception {
        initMocks(this);

        OAuth2ProviderSettingsFactory providerSettingsFactory = mock(OAuth2ProviderSettingsFactory.class);
        when(providerSettingsFactory.get(request)).thenReturn(providerSettings);
        when(providerSettings.getDeviceCodePollInterval()).thenReturn(5);
        when(providerSettings.validateRequestedClaims(anyString())).thenAnswer(new Answer<String>() {
            @Override
            public String answer(InvocationOnMock invocation) throws Throwable {
                return (String) invocation.getArguments()[0];
            }
        });

        OAuth2UrisFactory oAuth2UrisFactory = mock(OAuth2UrisFactory.class);
        when(oAuth2UrisFactory.get(request)).thenReturn(oAuth2Uris);

        ClientAuthenticator clientAuthenticator = mock(ClientAuthenticator.class);
        ClientRegistration clientRegistration = mock(ClientRegistration.class);
        when(clientAuthenticator.authenticate(eq(request), anyString())).thenReturn(clientRegistration);

        accessTokenGenerator = new GrantTypeAccessTokenGenerator(tokenStore);
        when(tokenStore.createAccessToken(anyString(), anyString(), anyString(), anyString(), anyString(), anyString(),
                anySetOf(String.class), any(RefreshToken.class), anyString(), anyString(), any(OAuth2Request.class)))
                .thenReturn(accessToken);
        when(tokenStore.createRefreshToken(anyString(), anyString(), anyString(), anyString(), anySetOf(String.class),
                any(OAuth2Request.class), anyString())).thenReturn(refreshToken);

        ClientAuthenticationFailureFactory failureFactory = mock(ClientAuthenticationFailureFactory.class);
        InvalidClientException expectedResult = mock(InvalidClientException.class);
        when(expectedResult.getError()).thenReturn("invalid_client");
        when(failureFactory.getException()).thenReturn(expectedResult);
        when(failureFactory.getException(anyString())).thenReturn(expectedResult);
        when(failureFactory.getException(any(OAuth2Request.class), anyString())).thenReturn(expectedResult);

        grantTypeHandler = new DeviceCodeGrantTypeHandler(providerSettingsFactory, clientAuthenticator, tokenStore,
                clientRegistrationStore, failureFactory, oAuth2UrisFactory, accessTokenGenerator);
    }

    @Test
    public void shouldIssueAccessToken() throws Exception {
        // Given
        Set<String> scope = new HashSet<>();
        mockRequestRealmClientIdClientSecretAndCode("REALM", "CLIENT_ID", "CLIENT_SECRET", "CODE", scope);
        mockClientRegistration();
        mockDeviceCodeRead(authorizeDeviceCode(field(CLAIMS, asSet("CLAIMS"))));

        // When
        grantTypeHandler.handle(request);

        // Then
        verify(tokenStore).createAccessToken(DEVICE_CODE, BEARER, null, "RESOURCE_OWNER_ID", "CLIENT_ID", null, scope,
                null, null, "CLAIMS", request);
        verify(accessToken, never()).addExtraData(eq(REFRESH_TOKEN), anyString());
    }

    @Test
    public void shouldIssueRefreshToken() throws Exception {
        // Given
        Set<String> scope = new HashSet<>();
        mockRequestRealmClientIdClientSecretAndCode("REALM", "CLIENT_ID", "CLIENT_SECRET", "CODE", scope);
        mockClientRegistration();
        mockDeviceCodeRead(authorizeDeviceCode(field(CLAIMS, asSet("CLAIMS"))));
        given(providerSettings.issueRefreshTokens()).willReturn(true);

        // When
        grantTypeHandler.handle(request);

        // Then
        verify(tokenStore).createAccessToken(DEVICE_CODE, BEARER, null, "RESOURCE_OWNER_ID", "CLIENT_ID", null, scope,
                refreshToken, null, "CLAIMS", request);
        verify(tokenStore).createRefreshToken(DEVICE_CODE, "CLIENT_ID", "RESOURCE_OWNER_ID", null, scope, request,
                "CLAIMS");
        verify(accessToken).addExtraData(eq(REFRESH_TOKEN), anyString());
    }

    @Test
    public void shouldCatchInvalidClients() throws Exception {
        // Given
        InvalidClientException expectedResult = mock(InvalidClientException.class);
        when(expectedResult.getError()).thenReturn("invalid_client");

        Set<String> scope = new HashSet<>();
        mockRequestRealmClientIdClientSecretAndCode("REALM", "CLIENT_ID", "CLIENT_SECRET", "CODE", scope);
        given(clientRegistrationStore.get(anyString(), any(OAuth2Request.class)))
                .willThrow(expectedResult);

        // When
        try {
            grantTypeHandler.handle(request);

            // Then - exception
            fail("Should have exception");
        } catch (OAuth2Exception e) {
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
        Set<String> scope = new HashSet<>();
        mockRequestRealmClientIdClientSecretAndCode("REALM", clientId, clientSecret, code, scope);

        // When
        try {
            grantTypeHandler.handle(request);

            // Then - exception
            fail("Should have exception");
        } catch (OAuth2Exception e) {
            assertThat(e.getError().equals("bad_request"));
        }
    }

    @Test
    public void shouldCatchInvalidClientSecret() throws Exception {
        // Given
        Set<String> scope = new HashSet<>();
        mockRequestRealmClientIdClientSecretAndCode("REALM", "CLIENT_ID", "SECRET", "CODE", scope);
        mockClientRegistration();

        // When
        try {
            grantTypeHandler.handle(request);

            // Then - exception
            fail("Should have exception");
        } catch (OAuth2Exception e) {
            assertThat(e.getError().equals("invalid_client"));
        }
    }

    @Test
    public void shouldCatchNonExistingDeviceCode() throws Exception {
        // Given
        Set<String> scope = new HashSet<>();
        mockRequestRealmClientIdClientSecretAndCode("REALM", "CLIENT_ID", "SECRET", "CODE", scope);
        mockClientRegistration();
        mockDeviceCodeRead(null);

        // When
        try {
            grantTypeHandler.handle(request);

            // Then - exception
            fail("Should have exception");
        } catch (OAuth2Exception e) {
            assertThat(e.getError().equals("authorization_declined"));
        }
    }

    @Test
    public void shouldCatchExpiredDeviceCode() throws Exception {
        // Given
        Set<String> scope = new HashSet<>();
        mockRequestRealmClientIdClientSecretAndCode("REALM", "CLIENT_ID", "SECRET", "CODE", scope);
        mockClientRegistration();
        mockDeviceCodeRead(deviceCode(field("expireTime", asSet("1"))));

        // When
        try {
            grantTypeHandler.handle(request);

            // Then - exception
            fail("Should have exception");
        } catch (OAuth2Exception e) {
            assertThat(e.getError().equals("bad_request"));
        }
    }

    @Test
    public void shouldCatchPendingDeviceCode() throws Exception {
        // Given
        Set<String> scope = new HashSet<>();
        mockRequestRealmClientIdClientSecretAndCode("REALM", "CLIENT_ID", "SECRET", "CODE", scope);
        mockClientRegistration();
        mockDeviceCodeRead(deviceCode(field("expireTime", asSet(String.valueOf(currentTimeMillis() + 5000)))));

        // When
        try {
            grantTypeHandler.handle(request);

            // Then - exception
            fail("Should have exception");
        } catch (OAuth2Exception e) {
            assertThat(e.getError().equals("authorization_pending"));
        }
    }

    @Test
    public void shouldCatchTooRapidRequests() throws Exception {
        // Given
        Set<String> scope = new HashSet<>();
        mockRequestRealmClientIdClientSecretAndCode("REALM", "CLIENT_ID", "SECRET", "CODE", scope);
        mockClientRegistration();
        mockDeviceCodeRead(deviceCode(
                field("expireTime", asSet(String.valueOf(currentTimeMillis() + 5000))),
                field("lastQueried", asSet(String.valueOf(currentTimeMillis())))
                ));

        // When
        try {
            grantTypeHandler.handle(request);

            // Then - exception
            fail("Should have exception");
        } catch (OAuth2Exception e) {
            assertThat(e.getError().equals("bad_request"));
        }
    }

    private void mockClientRegistration() throws Exception {
        ClientRegistration registration = mock(ClientRegistration.class);
        given(clientRegistrationStore.get(eq("CLIENT_ID"), any(OAuth2Request.class))).willReturn(registration);
        given(registration.getClientId()).willReturn("CLIENT_ID");
        given(registration.getClientSecret()).willReturn("CLIENT_SECRET");
    }

    private void mockDeviceCodeRead(DeviceCode deviceCode) throws ServerException, NotFoundException,
            InvalidGrantException {
        given(tokenStore.readDeviceCode(eq("CLIENT_ID"), eq("CODE"), any(OAuth2Request.class))).willReturn(deviceCode);
    }

    private void mockRequestRealmClientIdClientSecretAndCode(String realm, String clientId, String clientSecret,
            String code, Set<String> scope) {
        given(request.getParameter(REALM)).willReturn(realm);
        given(request.getParameter(CLIENT_ID)).willReturn(clientId);
        given(request.getParameter(CLIENT_SECRET)).willReturn(clientSecret);
        given(request.getParameter(CODE)).willReturn(code);
        given(request.getParameter(GRANT_TYPE)).willReturn(DEVICE_CODE);
        given(request.getParameter(SCOPE)).willReturn(scope);
    }

    private static DeviceCode authorizeDeviceCode(Map.Entry<String, Object>... fields) {
        DeviceCode deviceCode = deviceCode(fields);
        deviceCode.setAuthorized(true);
        return deviceCode;
    }

    private static DeviceCode deviceCode(Map.Entry<String, Object>... fields) {
        try {
            final JsonValue json = json(object(
                    field("tokenName", asSet("device_code")),
                    field("id", asSet("123")),
                    field("user_code", asSet("456")),
                    field("realm", asSet("REALM")),
                    field("clientID", asSet("CLIENT_ID")),
                    field("userName", asSet("RESOURCE_OWNER_ID"))));
            for (Map.Entry<String, Object> field : fields) {
                json.put(field.getKey(), field.getValue());
            }
            return new DeviceCode(json);
        } catch (InvalidGrantException e) {
            throw new IllegalStateException(e);
        }
    }
}
