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
 * Copyright 2026 3A Systems, LLC.
 */

package org.forgerock.oauth2.restlet;

import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.testng.Assert.assertEquals;

import org.forgerock.oauth2.core.ClientRegistrationStore;
import org.forgerock.oauth2.core.DeviceCode;
import org.forgerock.oauth2.core.OAuth2ProviderSettings;
import org.forgerock.oauth2.core.OAuth2ProviderSettingsFactory;
import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.oauth2.core.OAuth2RequestFactory;
import org.forgerock.oauth2.core.TokenStore;
import org.forgerock.openam.oauth2.OAuth2Utils;
import org.forgerock.openam.rest.representations.JacksonRepresentationFactory;
import org.forgerock.openam.services.baseurl.BaseURLProviderFactory;
import org.restlet.Request;
import org.restlet.representation.EmptyRepresentation;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Validation of the PKCE parameters accepted by {@code /oauth2/device/code} (GHSA-h93p-35jm-rj83).
 */
public class DeviceCodeResourceTest {

    private static final String S256_CHALLENGE = "E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM";

    private DeviceCodeResource resource;
    private OAuth2Request o2request;
    private TokenStore tokenStore;

    @BeforeMethod
    public void setUp() throws Exception {
        Request request = mock(Request.class);
        o2request = mock(OAuth2Request.class);
        tokenStore = mock(TokenStore.class);
        OAuth2RequestFactory requestFactory = mock(OAuth2RequestFactory.class);
        OAuth2ProviderSettingsFactory providerSettingsFactory = mock(OAuth2ProviderSettingsFactory.class);
        OAuth2ProviderSettings providerSettings = mock(OAuth2ProviderSettings.class);
        OAuth2Utils oAuth2Utils = mock(OAuth2Utils.class);

        given(requestFactory.create(request)).willReturn(o2request);
        given(providerSettingsFactory.get(o2request)).willReturn(providerSettings);
        given(providerSettings.getVerificationUrl()).willReturn("https://am.example.com/oauth2/device/user");
        given(tokenStore.createDeviceCode(any(), any(), anyString(), nullable(String.class),
                anyString(), nullable(String.class), nullable(String.class), nullable(String.class),
                nullable(String.class), nullable(String.class), nullable(Integer.class), nullable(String.class),
                eq(o2request), nullable(String.class), nullable(String.class))).willReturn(mock(DeviceCode.class));

        given(o2request.getParameter("client_id")).willReturn("CLIENT_ID");
        given(o2request.getParameter("scope")).willReturn("openid");
        given(o2request.getParameter("response_type")).willReturn("token");

        resource = spy(new DeviceCodeResource(tokenStore, requestFactory, mock(ClientRegistrationStore.class),
                providerSettingsFactory, mock(BaseURLProviderFactory.class), mock(ExceptionHandler.class),
                mock(JacksonRepresentationFactory.class), oAuth2Utils));
        doReturn(request).when(resource).getRequest();
    }

    @DataProvider
    public Object[][] invalidChallengeMethods() {
        return new Object[][] {{"bogus"}, {"s256"}, {"PLAIN"}, {""}};
    }

    @Test(dataProvider = "invalidChallengeMethods")
    public void shouldRejectInvalidChallengeMethod(String codeChallengeMethod) throws Exception {
        given(o2request.getParameter("code_challenge")).willReturn(S256_CHALLENGE);
        given(o2request.getParameter("code_challenge_method")).willReturn(codeChallengeMethod);

        try {
            resource.issueCode(new EmptyRepresentation());
            fail("Expected OAuth2RestletException for code_challenge_method '" + codeChallengeMethod + "'");
        } catch (OAuth2RestletException e) {
            assertEquals(e.getStatus().getCode(), 400);
            assertEquals(e.getError(), "invalid_request");
            verify(tokenStore, never()).createDeviceCode(any(), any(), any(), any(), any(), any(), any(), any(),
                    any(), any(), any(), any(), any(), any(), any());
        }
    }

    @DataProvider
    public Object[][] validChallengeMethods() {
        return new Object[][] {{"S256"}, {"plain"}, {null}};
    }

    @Test(dataProvider = "validChallengeMethods")
    public void shouldBindValidChallengeToDeviceCode(String codeChallengeMethod) throws Exception {
        given(o2request.getParameter("code_challenge")).willReturn(S256_CHALLENGE);
        given(o2request.getParameter("code_challenge_method")).willReturn(codeChallengeMethod);

        resource.issueCode(new EmptyRepresentation());

        verify(tokenStore).createDeviceCode(any(), any(), eq("CLIENT_ID"), nullable(String.class),
                eq("token"), nullable(String.class), nullable(String.class), nullable(String.class),
                nullable(String.class), nullable(String.class), nullable(Integer.class), nullable(String.class),
                eq(o2request), eq(S256_CHALLENGE), eq(codeChallengeMethod));
    }

    /**
     * Without a code_challenge the method is never read, so it is ignored rather than rejected, as on
     * the authorization endpoint.
     */
    @Test
    public void shouldIgnoreChallengeMethodSentWithoutChallenge() throws Exception {
        given(o2request.getParameter("code_challenge_method")).willReturn("bogus");

        resource.issueCode(new EmptyRepresentation());

        verify(tokenStore).createDeviceCode(any(), any(), eq("CLIENT_ID"), nullable(String.class),
                eq("token"), nullable(String.class), nullable(String.class), nullable(String.class),
                nullable(String.class), nullable(String.class), nullable(Integer.class), nullable(String.class),
                eq(o2request), nullable(String.class), eq("bogus"));
    }

    @Test
    public void shouldIssueCodeWithoutChallenge() throws Exception {
        resource.issueCode(new EmptyRepresentation());

        verify(tokenStore).createDeviceCode(any(), any(), eq("CLIENT_ID"), nullable(String.class),
                eq("token"), nullable(String.class), nullable(String.class), nullable(String.class),
                nullable(String.class), nullable(String.class), nullable(Integer.class), nullable(String.class),
                eq(o2request), nullable(String.class), nullable(String.class));
    }
}
