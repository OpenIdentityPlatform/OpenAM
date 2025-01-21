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
 * Portions copyright 2025 3A Systems LLC.
 */

package org.forgerock.openidconnect.ssoprovider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.*;
import static org.forgerock.openam.oauth2.OAuth2Constants.JWTTokenParams.*;
import static org.forgerock.openam.utils.CollectionUtils.asList;
import static org.forgerock.openam.utils.CollectionUtils.asSet;
import static org.mockito.BDDMockito.given;

import jakarta.servlet.http.HttpServletRequest;

import org.forgerock.json.JsonValue;
import org.forgerock.json.jose.jws.SignedJwt;
import org.forgerock.json.jose.jwt.JwtClaimsSet;
import org.forgerock.oauth2.core.OAuth2Jwt;
import org.forgerock.oauth2.core.OAuth2ProviderSettings;
import org.forgerock.oauth2.core.OAuth2ProviderSettingsFactory;
import org.forgerock.oauth2.core.exceptions.NotFoundException;
import org.forgerock.oauth2.core.exceptions.OAuth2ProviderNotFoundException;
import org.forgerock.openam.oauth2.CookieExtractor;
import org.forgerock.openidconnect.OpenIdConnectClientRegistration;
import org.forgerock.openidconnect.OpenIdConnectClientRegistrationStore;
import org.forgerock.openidconnect.OpenIdConnectTokenStore;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;

public class OpenIdConnectSSOProviderTest {
    private static final String COOKIE_NAME = "COOKIE";

    @Mock
    private SSOTokenManager mockTokenManager;

    @Mock
    private OpenIdConnectClientRegistrationStore mockClientStore;

    @Mock
    private OpenIdConnectClientRegistration mockClient;

    @Mock
    private OpenIdConnectTokenStore mockTokenStore;

    @Mock
    private OpenIdConnectSSOProvider.IdTokenParser mockTokenParser;

    @Mock
    private CookieExtractor mockCookieExtractor;

    @Mock
    private OAuth2Jwt mockJwt;

    @Mock
    private HttpServletRequest mockRequest;

    @Mock
    private SignedJwt mockSignedJwt;

    @Mock
    private SSOToken mockSsoToken;

    @Mock
    private OAuth2ProviderSettingsFactory mockProviderSettingsFactory;

    @Mock
    private OAuth2ProviderSettings mockProviderSettings;

    private JwtClaimsSet claimsSet;

    private OpenIdConnectSSOProvider ssoProvider;

    @BeforeMethod
    public void createProvider() {
        MockitoAnnotations.initMocks(this);

        claimsSet = new JwtClaimsSet();
        ssoProvider = new OpenIdConnectSSOProvider(mockTokenManager, mockClientStore, mockTokenStore,
                mockCookieExtractor, mockProviderSettingsFactory, COOKIE_NAME, mockTokenParser);

        given(mockJwt.getSignedJwt()).willReturn(mockSignedJwt);
        given(mockSignedJwt.getClaimsSet()).willReturn(claimsSet);
    }

    @Test
    public void shouldNotBeApplicableToNullTokenId() throws Exception {
        assertThat(ssoProvider.isApplicable((String) null)).isFalse();
    }

    @Test
    public void shouldNotBeApplicableToNullRequest() throws Exception {
        assertThat(ssoProvider.isApplicable((HttpServletRequest) null)).isFalse();
    }

    @Test
    public void shouldNotBeApplicableToRandomStrings() throws Exception {
        String tokenId = "random string";
        given(mockTokenParser.parse(tokenId)).willThrow(new SSOException(""));
        assertThat(ssoProvider.isApplicable(tokenId)).isFalse();
    }

    @Test
    public void shouldBeApplicableToAValidJwt() throws Exception {
        String tokenId = "a valid jwt";
        given(mockTokenParser.parse(tokenId)).willReturn(mockJwt);
        given(mockProviderSettingsFactory.getRealmProviderSettings("/")).willReturn(mockProviderSettings);
        given(mockProviderSettings.isOpenIDConnectSSOProviderEnabled()).willReturn(true);
        assertThat(ssoProvider.isApplicable(tokenId)).isTrue();
    }

    @Test
    public void shouldNotBeApplicableIfProviderIsDisabledInRealm() throws Exception {
        String tokenId = "a valid jwt";
        String realm = "universe";
        given(mockTokenParser.parse(tokenId)).willReturn(mockJwt);
        claimsSet.setClaim(REALM, realm);
        given(mockProviderSettingsFactory.getRealmProviderSettings(realm)).willReturn(mockProviderSettings);
        given(mockProviderSettings.isOpenIDConnectSSOProviderEnabled()).willReturn(false);
        assertThat(ssoProvider.isApplicable(tokenId)).isFalse();
    }

    @Test
    public void shouldNotBeApplicableIfProviderDoesNotExistInRealm() throws Exception {
        String tokenId = "a valid jwt";
        String realm = "universe";
        given(mockTokenParser.parse(tokenId)).willReturn(mockJwt);
        claimsSet.setClaim(REALM, realm);
        given(mockProviderSettingsFactory.getRealmProviderSettings(realm))
                .willThrow(new OAuth2ProviderNotFoundException(""));
        assertThat(ssoProvider.isApplicable(tokenId)).isFalse();
    }

    @Test
    public void shouldExtractCookieFromConfiguredCookieName() throws Exception {
        String tokenId = "a valid jwt";
        given(mockCookieExtractor.extract(mockRequest, COOKIE_NAME)).willReturn(tokenId);
        given(mockTokenParser.parse(tokenId)).willReturn(mockJwt);
        given(mockProviderSettingsFactory.getRealmProviderSettings("/")).willReturn(mockProviderSettings);
        given(mockProviderSettings.isOpenIDConnectSSOProviderEnabled()).willReturn(true);
        assertThat(ssoProvider.isApplicable(mockRequest)).isTrue();
    }

    @Test(expectedExceptions = SSOException.class, expectedExceptionsMessageRegExp = "test_message")
    public void shouldRejectIdTokenIssuedToUnknownClient() throws Exception {
        // Given
        String tokenId = "a valid jwt";
        String clientId = "unknown_client";
        given(mockTokenParser.parse(tokenId)).willReturn(mockJwt);
        given(mockProviderSettingsFactory.getRealmProviderSettings("/")).willReturn(mockProviderSettings);
        given(mockProviderSettings.isOpenIDConnectSSOProviderEnabled()).willReturn(true);
        claimsSet.addAudience(clientId);
        given(mockClientStore.get(clientId, "/", null)).willThrow(new NotFoundException("test_message"));

        // When
        ssoProvider.createSSOToken(tokenId);

        // Then - exception
    }

    @Test(expectedExceptions = SSOException.class, expectedExceptionsMessageRegExp = "id_token has expired")
    public void shouldRejectExpiredIdTokens() throws Exception {
        // Given
        String tokenId = "a valid jwt";
        given(mockTokenParser.parse(tokenId)).willReturn(mockJwt);
        given(mockJwt.isExpired()).willReturn(true);

        // When
        ssoProvider.createSSOToken(tokenId);

        // Then - exception
    }

    @Test(expectedExceptions = SSOException.class, expectedExceptionsMessageRegExp = "invalid id_token")
    public void shouldRejectInvalidSignatures() throws Exception {
        // Given
        String tokenId = "malicious Jwt";
        String clientId = "client_id";
        given(mockTokenParser.parse(tokenId)).willReturn(mockJwt);
        given(mockJwt.isExpired()).willReturn(false);
        given(mockProviderSettingsFactory.getRealmProviderSettings("/")).willReturn(mockProviderSettings);
        given(mockProviderSettings.isOpenIDConnectSSOProviderEnabled()).willReturn(true);
        claimsSet.addAudience(clientId);
        given(mockClientStore.get(clientId, "/", null)).willReturn(mockClient);
        given(mockClient.verifyJwtIdentity(mockJwt)).willReturn(false);

        // When
        ssoProvider.createSSOToken(tokenId);

        // Then - exception
    }

    @Test(expectedExceptions = SSOException.class, expectedExceptionsMessageRegExp = "no session linked to id_token")
    public void shouldRejectJwtWithNoOpsClaim() throws Exception {
        // Given
        String tokenId = "a valid jwt";
        String clientId = "client_id";
        given(mockTokenParser.parse(tokenId)).willReturn(mockJwt);
        given(mockJwt.isExpired()).willReturn(false);
        given(mockProviderSettingsFactory.getRealmProviderSettings("/")).willReturn(mockProviderSettings);
        given(mockProviderSettings.isOpenIDConnectSSOProviderEnabled()).willReturn(true);
        claimsSet.addAudience(clientId);
        // no OPS claim
        given(mockClientStore.get(clientId, "/", null)).willReturn(mockClient);
        given(mockClient.verifyJwtIdentity(mockJwt)).willReturn(true);

        // When
        ssoProvider.createSSOToken(tokenId);

        // Then - exception
    }

    @Test(expectedExceptions = SSOException.class, expectedExceptionsMessageRegExp = "session not found")
    public void shouldRejectJwtIfSessionNotFound() throws Exception {
        // Given
        String tokenId = "a valid jwt";
        String clientId = "client_id";
        String ops = "session identifier";
        given(mockTokenParser.parse(tokenId)).willReturn(mockJwt);
        given(mockJwt.isExpired()).willReturn(false);
        given(mockProviderSettingsFactory.getRealmProviderSettings("/")).willReturn(mockProviderSettings);
        given(mockProviderSettings.isOpenIDConnectSSOProviderEnabled()).willReturn(true);
        claimsSet.addAudience(clientId);
        claimsSet.setClaim(OPS, ops);
        given(mockClientStore.get(clientId, "/", null)).willReturn(mockClient);
        given(mockClient.verifyJwtIdentity(mockJwt)).willReturn(true);
        given(mockTokenStore.read(ops)).willReturn(null);

        // When
        ssoProvider.createSSOToken(tokenId);

        // Then - exception
    }

    @Test(expectedExceptions = SSOException.class, expectedExceptionsMessageRegExp = "no session linked to id_token")
    public void shouldRejectJwtIfNoSessionLinked() throws Exception {
        // Given
        String tokenId = "a valid jwt";
        String clientId = "client_id";
        String ops = "session identifier";
        JsonValue token = json(object());
        given(mockTokenParser.parse(tokenId)).willReturn(mockJwt);
        given(mockJwt.isExpired()).willReturn(false);
        given(mockProviderSettingsFactory.getRealmProviderSettings("/")).willReturn(mockProviderSettings);
        given(mockProviderSettings.isOpenIDConnectSSOProviderEnabled()).willReturn(true);
        claimsSet.addAudience(clientId);
        claimsSet.setClaim(OPS, ops);
        given(mockClientStore.get(clientId, "/", null)).willReturn(mockClient);
        given(mockClient.verifyJwtIdentity(mockJwt)).willReturn(true);
        given(mockTokenStore.read(ops)).willReturn(token);

        // When
        ssoProvider.createSSOToken(tokenId);

        // Then - exception
    }

    @Test
    public void shouldUseStoredSessionIdWhenFound() throws Exception {
        // Given
        String tokenId = "a valid jwt";
        String clientId = "client_id";
        String ops = "session identifier";
        String sessionId = "a valid session id";

        given(mockTokenParser.parse(tokenId)).willReturn(mockJwt);
        given(mockJwt.isExpired()).willReturn(false);
        given(mockProviderSettingsFactory.getRealmProviderSettings("/")).willReturn(mockProviderSettings);
        given(mockProviderSettings.isOpenIDConnectSSOProviderEnabled()).willReturn(true);
        claimsSet.addAudience(clientId);
        claimsSet.setClaim(OPS, ops);
        given(mockClientStore.get(clientId, "/", null)).willReturn(mockClient);
        given(mockClient.verifyJwtIdentity(mockJwt)).willReturn(true);
        given(mockTokenStore.read(ops)).willReturn(json(object(field(LEGACY_OPS, asList(sessionId)))));
        given(mockTokenManager.createSSOToken(sessionId)).willReturn(mockSsoToken);

        // When
        SSOToken result = ssoProvider.createSSOToken(tokenId);

        // Then
        assertThat(result).isSameAs(mockSsoToken);
    }

    @Test
    public void shouldUseSSOTokenClaimWhenPresent() throws Exception {
        // Given
        String tokenId = "a valid jwt";
        String clientId = "client_id";
        String sessionId = "a valid session id";

        given(mockTokenParser.parse(tokenId)).willReturn(mockJwt);
        given(mockJwt.isExpired()).willReturn(false);
        given(mockProviderSettingsFactory.getRealmProviderSettings("/")).willReturn(mockProviderSettings);
        given(mockProviderSettings.isOpenIDConnectSSOProviderEnabled()).willReturn(true);
        claimsSet.addAudience(clientId);
        claimsSet.setClaim(SSOTOKEN, sessionId);
        given(mockClientStore.get(clientId, "/", null)).willReturn(mockClient);
        given(mockClient.verifyJwtIdentity(mockJwt)).willReturn(true);
        given(mockTokenManager.createSSOToken(sessionId)).willReturn(mockSsoToken);

        // When
        SSOToken result = ssoProvider.createSSOToken(tokenId);

        // Then
        assertThat(result).isSameAs(mockSsoToken);

    }

    @Test(expectedExceptions = SSOException.class, expectedExceptionsMessageRegExp = ".*not enabled.*")
    public void shouldRejectTokenIfProviderNotEnabledInRealm() throws Exception {
        // Given
        String tokenId = "a valid jwt";
        given(mockTokenParser.parse(tokenId)).willReturn(mockJwt);
        given(mockJwt.isExpired()).willReturn(false);
        given(mockProviderSettingsFactory.getRealmProviderSettings("/"))
                .willThrow(new OAuth2ProviderNotFoundException(""));

        // When
        ssoProvider.createSSOToken(tokenId);

        // Then - exception
    }

    @DataProvider
    public Object[][] invalidJwts() {
        return new Object[][] {
                {"AtoZatoz0to9-_*.AtoZ.invalidCharacter"},
                {"AtoZatoz0to9-_.wrongNumberOfComponents"},
                {"AtoZatoz0to9-_.AtoZatoz0to9-_.AtoZatoz0to9-_..emptyComponent"}
        };
    }

    @Test(dataProvider = "invalidJwts",
            expectedExceptions = SSOException.class, expectedExceptionsMessageRegExp = ".*not a valid JWT.*")
    public void parserShouldNotParseInvalidJwt(String invalidJwt) throws Exception {
        new OpenIdConnectSSOProvider.IdTokenParser().parse(invalidJwt);
    }

    @Test
    public void parserShouldParseValidJwt() throws Exception {
        String validJwt =
                "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJhIn0.1pXop7_jwVZJMlpF46G0GofoKnUy29zHEtwhtaCXyXI";
        assertThat(new OpenIdConnectSSOProvider.IdTokenParser().parse(validJwt).getSubject()).isEqualTo("a");
    }
}