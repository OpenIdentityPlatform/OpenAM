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
import static org.forgerock.json.JsonValue.json;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.MockitoAnnotations.initMocks;

import java.security.KeyPair;
import java.security.PublicKey;

import org.forgerock.json.JsonValue;
import org.forgerock.json.jose.common.JwtReconstruction;
import org.forgerock.json.jose.jws.JwsAlgorithm;
import org.forgerock.json.jose.jws.JwsHeader;
import org.forgerock.json.jose.jws.SignedJwt;
import org.forgerock.json.jose.jws.SigningManager;
import org.forgerock.json.jose.jws.handlers.SigningHandler;
import org.forgerock.json.jose.jwt.JwtClaimsSet;
import org.forgerock.oauth2.core.AccessToken;
import org.forgerock.oauth2.core.ClientRegistration;
import org.forgerock.oauth2.core.ClientRegistrationStore;
import org.forgerock.oauth2.core.OAuth2ProviderSettings;
import org.forgerock.oauth2.core.OAuth2ProviderSettingsFactory;
import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.oauth2.core.OAuth2Uris;
import org.forgerock.oauth2.core.OAuth2UrisFactory;
import org.forgerock.oauth2.core.exceptions.InvalidClientException;
import org.forgerock.oauth2.core.exceptions.NotFoundException;
import org.forgerock.oauth2.core.exceptions.ServerException;
import org.forgerock.openam.core.RealmInfo;
import org.mockito.Mock;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class IdTokenClaimGathererTest {

    private IdTokenClaimGatherer claimGatherer;

    @Mock
    private OAuth2ProviderSettings oAuth2ProviderSettings;
    @Mock
    private OAuth2Uris oAuth2Uris;
    @Mock
    private ClientRegistration clientRegistration;
    @Mock
    private JwtReconstruction jwtReconstruction;
    @Mock
    private SigningManager signingManager;
    @Mock
    private JwsHeader jwsHeader;
    @Mock
    private JwtClaimsSet claimsSet;
    @Mock
    private SignedJwt idToken;
    @Mock
    private OAuth2Request oAuth2Request;

    @BeforeMethod
    public void setup() throws Exception {
        initMocks(this);
        OAuth2ProviderSettingsFactory oAuth2ProviderSettingsFactory = mockOAuth2ProviderSettings();
        OAuth2UrisFactory<RealmInfo> oauth2UrisFactory = mockOAuth2Uris();
        ClientRegistrationStore clientRegistrationStore = mockClientRegistrationStore();
        claimGatherer = spy(new IdTokenClaimGatherer(oAuth2ProviderSettingsFactory, oauth2UrisFactory,
                clientRegistrationStore, jwtReconstruction, signingManager));
        given(jwtReconstruction.reconstructJwt(anyString(), eq(SignedJwt.class))).willReturn(idToken);
        given(idToken.getHeader()).willReturn(jwsHeader);
        given(idToken.getClaimsSet()).willReturn(claimsSet);
    }

    private OAuth2ProviderSettingsFactory mockOAuth2ProviderSettings() throws NotFoundException, ServerException {
        OAuth2ProviderSettingsFactory oAuth2ProviderSettingsFactory = mock(OAuth2ProviderSettingsFactory.class);
        given(oAuth2ProviderSettingsFactory.get(oAuth2Request)).willReturn(oAuth2ProviderSettings);
        PublicKey publicKey = mock(PublicKey.class);
        KeyPair keyPair = new KeyPair(publicKey, null);
        given(oAuth2ProviderSettings.getServerKeyPair()).willReturn(keyPair);
        return oAuth2ProviderSettingsFactory;
    }

    private OAuth2UrisFactory<RealmInfo> mockOAuth2Uris() throws NotFoundException, ServerException {
        OAuth2UrisFactory<RealmInfo> oAuth2UrisFactory = mock(OAuth2UrisFactory.class);
        given(oAuth2UrisFactory.get(oAuth2Request)).willReturn(oAuth2Uris);
        PublicKey publicKey = mock(PublicKey.class);
        KeyPair keyPair = new KeyPair(publicKey, null);
        given(oAuth2ProviderSettings.getServerKeyPair()).willReturn(keyPair);
        return oAuth2UrisFactory;
    }

    private ClientRegistrationStore mockClientRegistrationStore() throws InvalidClientException, NotFoundException {
        ClientRegistrationStore clientRegistrationStore = mock(ClientRegistrationStore.class);
        given(clientRegistrationStore.get("CLIENT_ID", oAuth2Request)).willReturn(clientRegistration);
        given(clientRegistration.getClientSecret()).willReturn("CLIENT_SECRET");
        return clientRegistrationStore;
    }

    @Test
    public void shouldGatherValidIdTokenClaimToken() {

        //Given
        AccessToken authorizationApiToken = mockAuthorizationApiToken();
        JsonValue claimToken = mockIdTokenClaimToken("ISSUER");

        setIdTokenAndOAuth2ProviderIssuers("ISSUER");

        //When
        String requestingPartyId = claimGatherer.getRequestingPartyId(oAuth2Request, authorizationApiToken, claimToken);

        //Then
        assertThat(requestingPartyId).isEqualTo("REQUESTING_PARTY_ID");
    }

    @Test
    public void shouldNotGatherIdTokenClaimTokenWithIncorrectIssuer() {

        //Given
        AccessToken authorizationApiToken = mockAuthorizationApiToken();
        JsonValue claimToken = mockIdTokenClaimToken("OTHER_ISSUER");

        setIdTokenAndOAuth2ProviderIssuers("ISSUER");

        //When
        String requestingPartyId = claimGatherer.getRequestingPartyId(oAuth2Request, authorizationApiToken, claimToken);

        //Then
        assertThat(requestingPartyId).isNull();
    }

    @Test
    public void shouldNotGatherIdTokenClaimTokenWhichIsIncorrectlySigned() {

        //Given
        AccessToken authorizationApiToken = mockAuthorizationApiToken();
        JsonValue claimToken = mockInvalidIdTokenClaimToken("ISSUER");

        setIdTokenAndOAuth2ProviderIssuers("ISSUER");

        //When
        String requestingPartyId = claimGatherer.getRequestingPartyId(oAuth2Request, authorizationApiToken, claimToken);

        //Then
        assertThat(requestingPartyId).isNull();
    }

    private AccessToken mockAuthorizationApiToken() {
        AccessToken authorizationApiToken = mock(AccessToken.class);
        given(authorizationApiToken.getClientId()).willReturn("CLIENT_ID");
        return authorizationApiToken;
    }

    private JsonValue mockIdTokenClaimToken(String issuer) {
        mockIdToken(true);
        given(claimsSet.getSubject()).willReturn("REQUESTING_PARTY_ID");
        given(claimsSet.getIssuer()).willReturn(issuer);
        return json("ID_TOKEN");
    }

    private JsonValue mockInvalidIdTokenClaimToken(String issuer) {
        mockIdToken(false);
        given(claimsSet.getSubject()).willReturn("REQUESTING_PARTY_ID");
        given(claimsSet.getIssuer()).willReturn(issuer);
        return json("ID_TOKEN");
    }

    private void mockIdToken(boolean isValid) {
        given(jwsHeader.getAlgorithm()).willReturn(JwsAlgorithm.HS256);
        given(idToken.getHeader()).willReturn(jwsHeader);
        SigningHandler signingHandler = mock(SigningHandler.class);
        given(signingManager.newHmacSigningHandler(any(byte[].class))).willReturn(signingHandler);
        given(idToken.verify(signingHandler)).willReturn(isValid);
    }

    private void setIdTokenAndOAuth2ProviderIssuers(String oAuth2ProviderIssuer) {
        try {
            given(oAuth2Uris.getIssuer()).willReturn(oAuth2ProviderIssuer);
        } catch (ServerException ignored) {
        }
    }
}
