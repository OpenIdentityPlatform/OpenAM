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
 * Copyright 2016 ForgeRock AS.
 * Portions copyright 2026 3A Systems LLC.
 */
package org.forgerock.openam.oauth2;

import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.util.HashMap;
import java.util.Map;

import com.sun.identity.shared.debug.Debug;
import org.forgerock.json.jose.builders.JwtBuilderFactory;
import org.forgerock.oauth2.core.AccessToken;
import org.forgerock.oauth2.core.AuthorizationCode;
import org.forgerock.oauth2.core.OAuth2ProviderSettings;
import org.forgerock.oauth2.core.OAuth2ProviderSettingsFactory;
import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.oauth2.core.OAuth2Uris;
import org.forgerock.oauth2.core.RefreshToken;
import org.forgerock.openam.blacklist.Blacklist;
import org.forgerock.openam.blacklist.Blacklistable;
import org.forgerock.openam.cts.CTSPersistentStore;
import org.forgerock.openam.cts.adapters.TokenAdapter;
import org.forgerock.openam.utils.RealmNormaliser;
import org.forgerock.openidconnect.OpenIdConnectClientRegistrationStore;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Unit test for {@link StatelessTokenStore}.
 *
 * @since 14.0.0
 */
public final class StatelessTokenStoreTest {

    private StatelessTokenStore tokenStore;

    @Mock
    private StatefulTokenStore statefulTokenStore;
    @Mock
    private JwtBuilderFactory jwtBuilder;
    @Mock
    private OAuth2ProviderSettingsFactory providerSettingsFactory;
    @Mock
    private Debug logger;
    @Mock
    private OpenIdConnectClientRegistrationStore clientRegistrationStore;
    @Mock
    private RealmNormaliser realmNormaliser;
    @Mock
    private OAuth2UrisFactory oAuth2UrisFactory;
    @Mock
    private OAuth2Uris oAuth2Uris;
    @Mock
    private Blacklist<Blacklistable> tokenBlacklist;
    @Mock
    private CTSPersistentStore cts;
    @Mock
    private TokenAdapter<StatelessTokenMetadata> tokenAdapter;
    @Mock
    private OAuth2Utils utils;
    @Mock
    private OAuth2Request request;
    @Mock
    private OAuth2ProviderSettings settings;
    @Mock
    private OAuth2AccessTokenModifier accessTokenModifier;

    @BeforeMethod
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        tokenStore = new StatelessTokenStore(statefulTokenStore, new JwtBuilderFactory(), providerSettingsFactory,
                logger, clientRegistrationStore, realmNormaliser, oAuth2UrisFactory, tokenBlacklist,
                cts, tokenAdapter, utils, accessTokenModifier);
    }

    @Test
    public void whenCnfIsPresentInRequestGetsAddedToToken() throws Exception {
        // Given
        given(providerSettingsFactory.get(request)).willReturn(settings);
        given(clientRegistrationStore.get("client-id", request)).willReturn(null);
        given(request.getParameter("realm")).willReturn("/abc");
        given(realmNormaliser.normalise("/abc")).willReturn("/def");
        given(oAuth2UrisFactory.get(request)).willReturn(oAuth2Uris);
        given(oAuth2Uris.getIssuer()).willReturn("some-issuer");
        given(settings.getTokenSigningAlgorithm()).willReturn("HS256");
        given(settings.getSupportedIDTokenSigningAlgorithms()).willReturn(singleton("HS256"));
        given(settings.getTokenHmacSharedSecret()).willReturn("c2VjcmV0");
        given(utils.getConfirmationKey(request)).willReturn(json(object(field("jwk", object()))));

        // When
        AccessToken token = tokenStore.createAccessToken("authorization_code", "exmple", "123-456-789", "owner-id",
                "client-id", "http://a/b.com", singleton("open"), null, "qwerty", "some-claim", request);

        // Then
        assertThat(token.getConfirmationKey().isNotNull()).isTrue();
        assertThat(token.getConfirmationKey().isDefined("jwk")).isTrue();
    }

    @Test
    public void whenCnfIsNotPresentInRequestItDoesNotGetAddedToToken() throws Exception {
        // Given
        given(providerSettingsFactory.get(request)).willReturn(settings);
        given(clientRegistrationStore.get("client-id", request)).willReturn(null);
        given(request.getParameter("realm")).willReturn("/abc");
        given(realmNormaliser.normalise("/abc")).willReturn("/def");
        given(oAuth2UrisFactory.get(request)).willReturn(oAuth2Uris);
        given(oAuth2Uris.getIssuer()).willReturn("some-issuer");
        given(settings.getTokenSigningAlgorithm()).willReturn("HS256");
        given(settings.getSupportedIDTokenSigningAlgorithms()).willReturn(singleton("HS256"));
        given(settings.getTokenHmacSharedSecret()).willReturn("c2VjcmV0");
        given(utils.getConfirmationKey(request)).willReturn(null);

        // When
        AccessToken token = tokenStore.createAccessToken("authorization_code", "exmple", "123-456-789", "owner-id",
                "client-id", "http://a/b.com", singleton("open"), null, "qwerty", "some-claim", request);

        // Then
        assertThat(token.getConfirmationKey().isNull()).isTrue();
    }

    @Test
    public void whenAuthorizationCodePresentAcrAndAmrGetAddedToAccessToken() throws Exception {
        // Given
        givenBaseProviderSettings();
        given(utils.getConfirmationKey(request)).willReturn(null);

        AuthorizationCode authorizationCode = mock(AuthorizationCode.class);
        given(authorizationCode.getAuthModules()).willReturn("DataStore");
        given(authorizationCode.getAuthenticationContextClassReference()).willReturn("urn:mace:incommon:iap:silver");
        given(authorizationCode.getSessionId()).willReturn(null);
        given(request.getToken(AuthorizationCode.class)).willReturn(authorizationCode);

        // When
        AccessToken token = tokenStore.createAccessToken("authorization_code", "exmple", "123-456-789", "owner-id",
                "client-id", "http://a/b.com", singleton("open"), null, "qwerty", "some-claim", request);

        // Then
        assertThat(token.getTokenInfo().get("acr")).isEqualTo("urn:mace:incommon:iap:silver");
        assertThat(token.getTokenInfo().get("authModules")).isEqualTo("DataStore");
    }

    @Test
    public void whenRefreshTokenPresentAcrAndAmrGetAddedToAccessToken() throws Exception {
        // Given
        givenBaseProviderSettings();
        given(utils.getConfirmationKey(request)).willReturn(null);

        RefreshToken currentRefreshToken = mock(RefreshToken.class);
        given(currentRefreshToken.getAuthModules()).willReturn("LDAP");
        given(currentRefreshToken.getAuthenticationContextClassReference()).willReturn("urn:mace:incommon:iap:bronze");
        given(request.getToken(RefreshToken.class)).willReturn(currentRefreshToken);

        // When
        AccessToken token = tokenStore.createAccessToken("refresh_token", "exmple", "123-456-789", "owner-id",
                "client-id", "http://a/b.com", singleton("open"), null, "qwerty", "some-claim", request);

        // Then
        assertThat(token.getTokenInfo().get("acr")).isEqualTo("urn:mace:incommon:iap:bronze");
        assertThat(token.getTokenInfo().get("authModules")).isEqualTo("LDAP");
    }

    @Test
    public void whenRefreshTokenPresentItOverridesAuthorizationCodeAcrAndAmr() throws Exception {
        // Given
        givenBaseProviderSettings();
        given(utils.getConfirmationKey(request)).willReturn(null);

        AuthorizationCode authorizationCode = mock(AuthorizationCode.class);
        given(authorizationCode.getAuthModules()).willReturn("DataStore");
        given(authorizationCode.getAuthenticationContextClassReference()).willReturn("acr-from-code");
        given(authorizationCode.getSessionId()).willReturn(null);
        given(request.getToken(AuthorizationCode.class)).willReturn(authorizationCode);

        RefreshToken currentRefreshToken = mock(RefreshToken.class);
        given(currentRefreshToken.getAuthModules()).willReturn("LDAP");
        given(currentRefreshToken.getAuthenticationContextClassReference()).willReturn("acr-from-refresh");
        given(request.getToken(RefreshToken.class)).willReturn(currentRefreshToken);

        // When
        AccessToken token = tokenStore.createAccessToken("refresh_token", "exmple", "123-456-789", "owner-id",
                "client-id", "http://a/b.com", singleton("open"), null, "qwerty", "some-claim", request);

        // Then
        assertThat(token.getTokenInfo().get("acr")).isEqualTo("acr-from-refresh");
        assertThat(token.getTokenInfo().get("authModules")).isEqualTo("LDAP");
    }

    @Test
    public void whenNoAcrOrAmrAvailableTheyAreNotAddedToAccessToken() throws Exception {
        // Given
        givenBaseProviderSettings();
        given(utils.getConfirmationKey(request)).willReturn(null);

        // When
        AccessToken token = tokenStore.createAccessToken("client_credentials", "exmple", "123-456-789", "owner-id",
                "client-id", "http://a/b.com", singleton("open"), null, "qwerty", "some-claim", request);

        // Then
        assertThat(token.getTokenInfo()).doesNotContainKey("acr");
        assertThat(token.getTokenInfo()).doesNotContainKey("authModules");
    }

    @Test
    public void whenModificationScriptReturnsClaimsTheyAreMergedIntoAccessToken() throws Exception {
        // Given
        givenBaseProviderSettings();
        given(utils.getConfirmationKey(request)).willReturn(null);

        Map<String, Object> modifiedClaims = new HashMap<>();
        modifiedClaims.put("department", "engineering");
        modifiedClaims.put("custom_number", 42);
        given(accessTokenModifier.getModifiedClaims(any(OAuth2Request.class), anyString(), anyString(), anyString(),
                any(), any())).willReturn(modifiedClaims);

        // When
        AccessToken token = tokenStore.createAccessToken("client_credentials", "exmple", "123-456-789", "owner-id",
                "client-id", "http://a/b.com", singleton("open"), null, "qwerty", "some-claim", request);

        // Then
        assertThat(token.getTokenInfo().get("department")).isEqualTo("engineering");
        assertThat(token.getTokenInfo().get("custom_number")).isEqualTo(42);
    }

    @Test
    public void whenModificationScriptReturnsClaimsTheyAreMergedIntoRefreshToken() throws Exception {
        // Given
        givenBaseProviderSettings();

        Map<String, Object> modifiedClaims = new HashMap<>();
        modifiedClaims.put("department", "engineering");
        given(accessTokenModifier.getModifiedClaims(any(OAuth2Request.class), anyString(), anyString(), anyString(),
                any(), any())).willReturn(modifiedClaims);

        // When
        RefreshToken token = tokenStore.createRefreshToken("client_credentials", "client-id", "owner-id",
                "http://a/b.com", singleton("open"), request, "some-claim");

        // Then
        assertThat(token.getTokenInfo().get("department")).isEqualTo("engineering");
    }

    @Test
    public void whenModificationScriptReturnsNoClaimsTokenIsUnchanged() throws Exception {
        // Given
        givenBaseProviderSettings();
        given(utils.getConfirmationKey(request)).willReturn(null);
        given(accessTokenModifier.getModifiedClaims(any(OAuth2Request.class), anyString(), anyString(), anyString(),
                any(), any())).willReturn(new HashMap<String, Object>());

        // When
        AccessToken token = tokenStore.createAccessToken("client_credentials", "exmple", "123-456-789", "owner-id",
                "client-id", "http://a/b.com", singleton("open"), null, "qwerty", "some-claim", request);

        // Then
        assertThat(token.getTokenInfo()).doesNotContainKey("department");
    }

    private void givenBaseProviderSettings() throws Exception {
        given(providerSettingsFactory.get(request)).willReturn(settings);
        given(clientRegistrationStore.get("client-id", request)).willReturn(null);
        given(request.getParameter("realm")).willReturn("/abc");
        given(realmNormaliser.normalise("/abc")).willReturn("/def");
        given(oAuth2UrisFactory.get(request)).willReturn(oAuth2Uris);
        given(oAuth2Uris.getIssuer()).willReturn("some-issuer");
        given(settings.getTokenSigningAlgorithm()).willReturn("HS256");
        given(settings.getSupportedIDTokenSigningAlgorithms()).willReturn(singleton("HS256"));
        given(settings.getTokenHmacSharedSecret()).willReturn("c2VjcmV0");
    }

}