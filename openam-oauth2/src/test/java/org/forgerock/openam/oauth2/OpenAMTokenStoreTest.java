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
 * Copyright 2014 ForgeRock AS.
 */

package org.forgerock.openam.oauth2;

import com.iplanet.sso.SSOTokenManager;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.oauth2.core.AccessToken;
import org.forgerock.oauth2.core.OAuth2ProviderSettingsFactory;
import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.oauth2.core.exceptions.InvalidGrantException;
import org.forgerock.oauth2.core.exceptions.ServerException;
import org.forgerock.oauth2.restlet.RestletOAuth2Request;
import org.forgerock.openam.cts.exceptions.CoreTokenException;
import org.forgerock.openidconnect.OpenIdConnectClientRegistrationStore;
import org.restlet.Request;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Collections;

import static org.assertj.core.api.Assertions.*;
import static org.forgerock.json.fluent.JsonValue.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

public class OpenAMTokenStoreTest {

    private OpenAMTokenStore openAMtokenStore;

    private OAuthTokenStore tokenStore;
    private OAuth2ProviderSettingsFactory providerSettingsFactory;
    private OpenIdConnectClientRegistrationStore clientRegistrationStore;
    private RealmNormaliser realmNormaliser;
    private SSOTokenManager ssoTokenManager;
    private Request request;

    @BeforeMethod
    public void setUp() {

        tokenStore = mock(OAuthTokenStore.class);
        providerSettingsFactory = mock(OAuth2ProviderSettingsFactory.class);
        clientRegistrationStore = mock(OpenIdConnectClientRegistrationStore.class);
        realmNormaliser = mock(RealmNormaliser.class);
        ssoTokenManager = mock(SSOTokenManager.class);
        request = mock(Request.class);
        CookieExtractor cookieExtractor = mock(CookieExtractor.class);

        openAMtokenStore = new OpenAMTokenStore(tokenStore, providerSettingsFactory, clientRegistrationStore,
                realmNormaliser, ssoTokenManager, cookieExtractor);
    }

    @Test
    public void shouldReadAccessToken() throws Exception {
        //Given
        JsonValue token = json(object(field("tokenName", Collections.singleton("access_token"))));
        given(tokenStore.read("TOKEN_ID")).willReturn(token);

        OAuth2Request request = new RestletOAuth2Request(this.request);

        //When
        AccessToken accessToken = openAMtokenStore.readAccessToken(request, "TOKEN_ID");

        //Then
        assertThat(accessToken).isNotNull();
        assertThat(request.getToken(AccessToken.class)).isSameAs(accessToken);
    }

    @Test (expectedExceptions = InvalidGrantException.class)
    public void shouldReadAccessTokenWhenNull() throws Exception {
        //Given
        given(tokenStore.read("TOKEN_ID")).willReturn(null);
        OAuth2Request request = new RestletOAuth2Request(this.request);

        //When
        openAMtokenStore.readAccessToken(request, "TOKEN_ID");

        //Then
        //Expected InvalidGrantException
    }

    @Test (expectedExceptions = ServerException.class)
    public void shouldFailToReadAccessToken() throws Exception {

        //Given
        doThrow(CoreTokenException.class).when(tokenStore).read("TOKEN_ID");
        OAuth2Request request = new RestletOAuth2Request(this.request);

        //When
        openAMtokenStore.readAccessToken(request, "TOKEN_ID");

        //Then
        //Expected ServerException
    }
}
