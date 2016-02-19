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
 * Copyright 2014-2016 ForgeRock AS.
 */

package org.forgerock.openam.oauth2;

import static java.util.Collections.*;
import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.data.Offset.offset;
import static org.forgerock.json.JsonValue.*;
import static org.forgerock.openam.utils.CollectionUtils.*;
import static org.forgerock.openam.utils.Time.*;
import static org.mockito.BDDMockito.any;
import static org.mockito.BDDMockito.anyString;
import static org.mockito.BDDMockito.*;
import static org.mockito.BDDMockito.verify;
import static org.mockito.BDDMockito.when;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.shared.debug.Debug;
import java.security.SecureRandom;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import org.forgerock.json.JsonValue;
import org.forgerock.oauth2.core.AccessToken;
import org.forgerock.oauth2.core.DeviceCode;
import org.forgerock.oauth2.core.OAuth2ProviderSettings;
import org.forgerock.oauth2.core.OAuth2ProviderSettingsFactory;
import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.oauth2.core.OAuth2UrisFactory;
import org.forgerock.oauth2.core.ResourceOwner;
import org.forgerock.oauth2.core.exceptions.ClientAuthenticationFailureFactory;
import org.forgerock.oauth2.core.exceptions.InvalidClientException;
import org.forgerock.oauth2.core.exceptions.InvalidGrantException;
import org.forgerock.oauth2.core.exceptions.NotFoundException;
import org.forgerock.oauth2.core.exceptions.ServerException;
import org.forgerock.oauth2.restlet.RestletOAuth2Request;
import org.forgerock.oauth2.restlet.RestletOAuth2RequestFactory;
import org.forgerock.openam.core.RealmInfo;
import org.forgerock.openam.cts.exceptions.CoreTokenException;
import org.forgerock.openam.oauth2.guice.OAuth2GuiceModule;
import org.forgerock.openam.rest.representations.JacksonRepresentationFactory;
import org.forgerock.openam.utils.RealmNormaliser;
import org.forgerock.openidconnect.OpenIdConnectClientRegistrationStore;
import org.forgerock.util.query.QueryFilter;
import org.restlet.Request;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class OpenAMTokenStoreTest {

    private OpenAMTokenStore openAMtokenStore;

    private OAuthTokenStore tokenStore;
    private OAuth2ProviderSettingsFactory providerSettingsFactory;
    private OAuth2UrisFactory<RealmInfo> oAuth2UrisFactory;
    private OpenIdConnectClientRegistrationStore clientRegistrationStore;
    private RealmNormaliser realmNormaliser;
    private SSOTokenManager ssoTokenManager;
    private CookieExtractor cookieExtractor;
    private Request request;
    private OAuth2AuditLogger auditLogger;
    private Debug debug;
    private ClientAuthenticationFailureFactory failureFactory;
    private RestletOAuth2RequestFactory oAuth2RequestFactory;

    @BeforeMethod
    public void setUp() {

        tokenStore = mock(OAuthTokenStore.class);
        providerSettingsFactory = mock(OAuth2ProviderSettingsFactory.class);
        oAuth2UrisFactory = mock(OAuth2UrisFactory.class);
        clientRegistrationStore = mock(OpenIdConnectClientRegistrationStore.class);
        realmNormaliser = mock(RealmNormaliser.class);
        ssoTokenManager = mock(SSOTokenManager.class);
        request = mock(Request.class);
        cookieExtractor = mock(CookieExtractor.class);
        auditLogger = mock(OAuth2AuditLogger.class);
        debug = mock(Debug.class);
        failureFactory = mock(ClientAuthenticationFailureFactory.class);

        oAuth2RequestFactory = new RestletOAuth2RequestFactory(new JacksonRepresentationFactory(new ObjectMapper()));

        ClientAuthenticationFailureFactory failureFactory = mock(ClientAuthenticationFailureFactory.class);
        InvalidClientException expectedResult = mock(InvalidClientException.class);
        when(expectedResult.getError()).thenReturn(new String("invalid_client"));
        when(failureFactory.getException()).thenReturn(expectedResult);
        when(failureFactory.getException(anyString())).thenReturn(expectedResult);
        when(failureFactory.getException(any(OAuth2Request.class), anyString())).thenReturn(expectedResult);

        openAMtokenStore = new OpenAMTokenStore(tokenStore, providerSettingsFactory, oAuth2UrisFactory,
                clientRegistrationStore, realmNormaliser, ssoTokenManager, cookieExtractor, auditLogger, debug,
                new SecureRandom(), failureFactory);
    }

    @Test
    public void shouldReadAccessToken() throws Exception {
        //Given
        JsonValue token = json(object(
                field("tokenName", Collections.singleton("access_token")),
                field("realm", Collections.singleton("/testrealm"))));
        given(tokenStore.read("TOKEN_ID")).willReturn(token);
        ConcurrentHashMap<String, Object> attributes = new ConcurrentHashMap<String, Object>();
        attributes.put("realm", "/testrealm");
        given(request.getAttributes()).willReturn(attributes);
        
        given(realmNormaliser.normalise("/testrealm")).willReturn("/testrealm");

        OAuth2Request request = oAuth2RequestFactory.create(this.request);

        //When
        AccessToken accessToken = openAMtokenStore.readAccessToken(request, "TOKEN_ID");

        //Then
        assertThat(accessToken).isNotNull();
        assertThat(request.getToken(AccessToken.class)).isSameAs(accessToken);
    }

    @Test (expectedExceptions = InvalidGrantException.class)
    public void shouldNotReadOtherRealmsAccessToken() throws Exception {
        //Given
        JsonValue token = json(object(
                field("tokenName", Collections.singleton("access_token")),
                field("realm", Collections.singleton("/otherrealm"))));
        given(tokenStore.read("TOKEN_ID")).willReturn(token);
        given(realmNormaliser.normalise("/otherrealm")).willReturn("/otherrealm");
        ConcurrentHashMap<String, Object> attributes = new ConcurrentHashMap<String, Object>();
        given(request.getAttributes()).willReturn(attributes);
        attributes.put("realm", "/testrealm");

        OAuth2Request request = oAuth2RequestFactory.create(this.request);

        //When
        AccessToken accessToken = openAMtokenStore.readAccessToken(request, "TOKEN_ID");

        //Then
        // expect InvalidGrantException
    }

    @Test (expectedExceptions = InvalidGrantException.class)
    public void shouldReadAccessTokenWhenNull() throws Exception {
        //Given
        given(tokenStore.read("TOKEN_ID")).willReturn(null);
        OAuth2Request request = oAuth2RequestFactory.create(this.request);

        //When
        openAMtokenStore.readAccessToken(request, "TOKEN_ID");

        //Then
        //Expected InvalidGrantException
    }

    @Test (expectedExceptions = ServerException.class)
    public void shouldFailToReadAccessToken() throws Exception {

        //Given
        doThrow(CoreTokenException.class).when(tokenStore).read("TOKEN_ID");
        OAuth2Request request = oAuth2RequestFactory.create(this.request);

        //When
        openAMtokenStore.readAccessToken(request, "TOKEN_ID");

        //Then
        //Expected ServerException
    }

    @Test (expectedExceptions = NotFoundException.class)
    public void shouldFailWhenNoProvider() throws Exception {

        //Given
        OAuth2Request request = oAuth2RequestFactory.create(this.request);
        doThrow(NotFoundException.class).when(providerSettingsFactory).get(request);

        //When
        openAMtokenStore.createAccessToken(null, null, null, null, null, null, null, null, null, null, request);

        //Then
        //Expected NotFoundException
    }

    @Test
    public void realmAgnosticTokenStoreShouldIgnoreRealmMismatch() throws Exception {
        //Given
        OpenAMTokenStore realmAgnosticTokenStore = new OAuth2GuiceModule.RealmAgnosticTokenStore(tokenStore,
                providerSettingsFactory, oAuth2UrisFactory, clientRegistrationStore, realmNormaliser, ssoTokenManager,
                cookieExtractor, auditLogger, debug, new SecureRandom(), failureFactory);
        JsonValue token = json(object(
                field("tokenName", Collections.singleton("access_token")),
                field("realm", Collections.singleton("/otherrealm"))));
        given(tokenStore.read("TOKEN_ID")).willReturn(token);
        ConcurrentHashMap<String, Object> attributes = new ConcurrentHashMap<String, Object>();
        given(request.getAttributes()).willReturn(attributes);
        attributes.put("realm", "/testrealm");

        OAuth2Request request = oAuth2RequestFactory.create(this.request);

        //When
        AccessToken accessToken = realmAgnosticTokenStore.readAccessToken(request, "TOKEN_ID");

        //Then
        assertThat(accessToken).isNotNull();
        assertThat(request.getToken(AccessToken.class)).isSameAs(accessToken);
    }

    @Test
    public void shouldCreateDeviceCode() throws Exception {
        // Given
        OAuth2ProviderSettings providerSettings = mock(OAuth2ProviderSettings.class);
        given(providerSettingsFactory.get(any(OAuth2Request.class))).willReturn(providerSettings);
        given(providerSettings.getDeviceCodeLifetime()).willReturn(10);
        given(tokenStore.query(any(QueryFilter.class))).willReturn(json(array()));
        final RestletOAuth2Request oauth2Request = oAuth2RequestFactory.create(this.request);
        given(request.getAttributes()).willReturn(new ConcurrentHashMap<>(singletonMap("realm", (Object) "MY_REALM")));
        given(realmNormaliser.normalise("MY_REALM")).willReturn("MY_REALM");
        ResourceOwner resourceOwner = mock(ResourceOwner.class);
        given(resourceOwner.getId()).willReturn("RESOURCE_OWNER_ID");

        // When
        DeviceCode code = openAMtokenStore.createDeviceCode(asSet("one", "two"), resourceOwner, "CLIENT ID", "NONCE",
                "RESPONSE TYPE", "STATE", "ACR VALUES", "PROMPT", "UI LOCALES", "LOGIN HINT", 55, "CLAIMS",
                oauth2Request, "CODE CHALLENGE", "CODE METHOD");

        // Then
        assertThat(code.getScope()).containsOnly("one", "two");
        assertThat(code.getClientId()).isEqualTo("CLIENT ID");
        assertThat(code.getNonce()).isEqualTo("NONCE");
        assertThat(code.getResponseType()).isEqualTo("RESPONSE TYPE");
        assertThat(code.getState()).isEqualTo("STATE");
        assertThat(code.getAcrValues()).isEqualTo("ACR VALUES");
        assertThat(code.getPrompt()).isEqualTo("PROMPT");
        assertThat(code.getUiLocales()).isEqualTo("UI LOCALES");
        assertThat(code.getLoginHint()).isEqualTo("LOGIN HINT");
        assertThat(code.getClaims()).isEqualTo("CLAIMS");
        assertThat(code.getCodeChallenge()).isEqualTo("CODE CHALLENGE");
        assertThat(code.getCodeChallengeMethod()).isEqualTo("CODE METHOD");
        assertThat(code.getMaxAge()).isEqualTo(55);
        assertThat(code.getTokenName()).isEqualTo("device_code");
        assertThat(code.getExpiryTime()).isCloseTo(currentTimeMillis() + 10000, offset(1000L));
        assertThat(code.getTokenId()).matches("[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}");
        assertThat(code.getUserCode()).matches("[" + OpenAMTokenStore.ALPHABET + "]{8}");
        assertThat(code.getRealm()).isEqualTo("MY_REALM");
    }

    @Test(expectedExceptions = InvalidGrantException.class)
    public void shouldThrowExceptionForInvalidDeviceCodeJsonValue() throws Exception {
        // Given
        given(tokenStore.read("123")).willReturn(json(object(field("tokenName", asSet("device_code")))));

        // When
        openAMtokenStore.readDeviceCode(null, "123", mock(OAuth2Request.class));
    }

    @Test
    public void shouldReadValidDeviceCode() throws Exception {
        // Given
        given(tokenStore.read("123")).willReturn(json(object(
                field("tokenName", asSet("device_code")),
                field("id", asSet("123")),
                field("user_code", asSet("456")),
                field("realm", asSet("/")),
                field("clientID", asSet("CLIENT_ID")))));
        final RestletOAuth2Request oauth2Request = oAuth2RequestFactory.create(this.request);
        given(request.getAttributes()).willReturn(new ConcurrentHashMap<>(singletonMap("realm", (Object) "/")));
        given(realmNormaliser.normalise("/")).willReturn("/");

        // When
        DeviceCode code = openAMtokenStore.readDeviceCode("CLIENT_ID", "123", oauth2Request);

        // Then
        assertThat(code.getTokenId()).isEqualTo("123");
        assertThat(code.getUserCode()).isEqualTo("456");
        assertThat(code.getClientId()).isEqualTo("CLIENT_ID");
    }

    @Test
    public void shouldUpdateDeviceCode() throws Exception {
        // Given
        DeviceCode code = new DeviceCode(json(object(
                field("tokenName", asSet("device_code")),
                field("id", asSet("123")),
                field("user_code", asSet("456")),
                field("realm", asSet("/")),
                field("clientID", asSet("CLIENT_ID")))));
        given(tokenStore.read("123")).willReturn(code);
        final RestletOAuth2Request oauth2Request = oAuth2RequestFactory.create(this.request);
        given(request.getAttributes()).willReturn(new ConcurrentHashMap<>(singletonMap("realm", (Object) "/")));
        given(realmNormaliser.normalise("/")).willReturn("/");

        // When
        openAMtokenStore.updateDeviceCode(code, oauth2Request);

        // Then
        verify(tokenStore).update(code);
    }

    @Test
    public void shouldDeleteDeviceCode() throws Exception {
        // Given
        DeviceCode code = new DeviceCode(json(object(
                field("tokenName", asSet("device_code")),
                field("id", asSet("123")),
                field("user_code", asSet("456")),
                field("realm", asSet("/")),
                field("clientID", asSet("CLIENT_ID")))));
        given(tokenStore.read("123")).willReturn(code);
        final RestletOAuth2Request oauth2Request = oAuth2RequestFactory.create(this.request);
        given(request.getAttributes()).willReturn(new ConcurrentHashMap<>(singletonMap("realm", (Object) "/")));
        given(realmNormaliser.normalise("/")).willReturn("/");

        // When
        openAMtokenStore.deleteDeviceCode("CLIENT_ID", "123", oauth2Request);

        // Then
        verify(tokenStore).delete("123");
    }
}
