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
 */
package org.forgerock.openam.oauth2;

import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.mockito.BDDMockito.given;

import org.forgerock.json.jose.builders.JwtBuilderFactory;
import org.forgerock.oauth2.core.AccessToken;
import org.forgerock.oauth2.core.OAuth2ProviderSettings;
import org.forgerock.oauth2.core.OAuth2ProviderSettingsFactory;
import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.oauth2.core.OAuth2Uris;
import org.forgerock.oauth2.core.exceptions.InvalidClientException;
import org.forgerock.oauth2.core.exceptions.InvalidConfirmationKeyException;
import org.forgerock.oauth2.core.exceptions.NotFoundException;
import org.forgerock.oauth2.core.exceptions.ServerException;
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

import com.sun.identity.shared.debug.Debug;

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

    @BeforeMethod
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        tokenStore = new StatelessTokenStore(statefulTokenStore, new JwtBuilderFactory(), providerSettingsFactory,
                logger, clientRegistrationStore, realmNormaliser, oAuth2UrisFactory, tokenBlacklist,
                cts, tokenAdapter, utils);
    }

    @Test
    public void whenCnfIsPresentInRequestGetsAddedToToken() throws ServerException, NotFoundException,
            InvalidConfirmationKeyException, InvalidClientException, org.forgerock.json.resource.NotFoundException {

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
    public void whenCnfIsNotPresentInRequestItDoesNotGetAddedToToken() throws NotFoundException, InvalidClientException,
            org.forgerock.json.resource.NotFoundException, ServerException, InvalidConfirmationKeyException {

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

}