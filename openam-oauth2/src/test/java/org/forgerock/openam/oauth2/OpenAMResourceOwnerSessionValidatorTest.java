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

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.authentication.AuthContext;
import org.forgerock.oauth2.core.AuthenticationMethod;
import org.forgerock.oauth2.core.OAuth2ProviderSettings;
import org.forgerock.oauth2.core.OAuth2ProviderSettingsFactory;
import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.oauth2.core.exceptions.BadRequestException;
import org.forgerock.oauth2.core.exceptions.InteractionRequiredException;
import org.forgerock.oauth2.core.exceptions.ResourceOwnerAuthenticationRequired;
import org.restlet.Request;
import org.restlet.data.Reference;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.testng.Assert.*;

/**
 * @since 12.0.0
 */
public class OpenAMResourceOwnerSessionValidatorTest {

    private static final SSOToken ACTIVE_SESSION_TOKEN = mock(SSOToken.class);
    private static final SSOToken NO_SESSION_TOKEN = null;

    private OpenAMResourceOwnerSessionValidator resourceOwnerSessionValidator;

    private SSOTokenManager mockSSOTokenManager;
    private OAuth2ProviderSettingsFactory mockProviderSettingsFactory;
    private OAuth2Request mockOAuth2Request;
    private Request restletRequest;
    private HttpServletRequest mockHttpServletRequest;

    @BeforeMethod
    public void setUp() throws Exception {

        mockSSOTokenManager = mock(SSOTokenManager.class);
        mockProviderSettingsFactory = mock(OAuth2ProviderSettingsFactory.class);
        mockOAuth2Request = mock(OAuth2Request.class);
        restletRequest = new Request();
        mockHttpServletRequest = mock(HttpServletRequest.class);

        given(mockOAuth2Request.getParameter("realm")).willReturn("");
        given(mockOAuth2Request.getParameter("locale")).willReturn("");
        given(mockOAuth2Request.getParameter("module")).willReturn("");
        given(mockOAuth2Request.getParameter("service")).willReturn("");
        given(mockOAuth2Request.getRequest()).willReturn(restletRequest);

        given(mockHttpServletRequest.getRequestURI()).willReturn("/openam/oauth2/authorize");
        given(mockHttpServletRequest.getScheme()).willReturn("http");
        given(mockHttpServletRequest.getServerName()).willReturn("openam.example.com");
        given(mockHttpServletRequest.getServerPort()).willReturn(8080);

        resourceOwnerSessionValidator =
                new OpenAMResourceOwnerSessionValidator(mockSSOTokenManager, mockProviderSettingsFactory) {
            @Override
            HttpServletRequest getHttpServletRequest(Request req) {
                return mockHttpServletRequest;
            }
        };

    }

    @Test (expectedExceptions = BadRequestException.class)
    public void shouldFailIfInvalidCombinationOfPromptsArePresent() throws Exception {

        //Given
        mockPrompt("login none");
        mockSSOToken(NO_SESSION_TOKEN);

        //When
        resourceOwnerSessionValidator.validate(mockOAuth2Request);

        // Then
        // BadRequestException
    }

    @Test
    public void shouldForceAuthentication() throws Exception {

        //Given
        mockPrompt("");
        mockSSOToken(NO_SESSION_TOKEN);

        try {
            //When
            resourceOwnerSessionValidator.validate(mockOAuth2Request);
            fail();

        } catch (ResourceOwnerAuthenticationRequired ex) {
            // Then
            assertEquals("goto=http://openam.example.com:8080/openam/oauth2/authorize?prompt=",
                    ex.getRedirectUri().getQuery());
        }
    }

    @Test
    public void shouldForceReauthenticationWhenLoginPromptIsPresent() throws Exception {

        //Given
        mockPrompt("login");
        mockSSOToken(ACTIVE_SESSION_TOKEN);

        try {
            //When
            resourceOwnerSessionValidator.validate(mockOAuth2Request);
            fail();

        } catch (ResourceOwnerAuthenticationRequired ex) {
            // Then
            assertEquals("goto=http://openam.example.com:8080/openam/oauth2/authorize?prompt=",
                    ex.getRedirectUri().getQuery());
            verify(mockSSOTokenManager).destroyToken(ACTIVE_SESSION_TOKEN);
        }
    }

    @Test
    public void shouldForceReauthenticationWhenLoginAndConsentPromptsArePresent() throws Exception {

        //Given
        mockPrompt("login consent");
        mockSSOToken(ACTIVE_SESSION_TOKEN);

        try {
            //When
            resourceOwnerSessionValidator.validate(mockOAuth2Request);
            fail();

        } catch (ResourceOwnerAuthenticationRequired ex) {
            // Then
            assertEquals("goto=http://openam.example.com:8080/openam/oauth2/authorize?prompt=consent",
                    ex.getRedirectUri().getQuery());
            verify(mockSSOTokenManager).destroyToken(ACTIVE_SESSION_TOKEN);
        }
    }

    @Test (expectedExceptions = InteractionRequiredException.class)
    public void shouldFailIfUserIsNotAuthenticatedAndNonePromptIsPresent() throws Exception {

        //Given
        mockPrompt("none");
        mockSSOToken(NO_SESSION_TOKEN);

        //When
        resourceOwnerSessionValidator.validate(mockOAuth2Request);

        // Then
        // InteractionRequiredException
    }

    // OPENAM-4092: When the user has no SSO token and specifies prompt=consent the
    // user should be presented with the OpenAM login page
    //
    @Test (expectedExceptions = ResourceOwnerAuthenticationRequired.class)
    public void shouldFailIfUserIsNotAuthenticatedAndOnlyConsentPromptIsPresent() throws Exception {

        //Given
        mockPrompt("consent");
        mockSSOToken(NO_SESSION_TOKEN);

        //When
        resourceOwnerSessionValidator.validate(mockOAuth2Request);

        // Then
        // LoginRequiredException
    }

    @Test
    public void shouldUseAcrValuesIfSpecified() throws Exception {
        // Given
        String acrValues = "1 2 3";
        String service = "myAuthService";
        mockPrompt("login");
        mockSSOToken(NO_SESSION_TOKEN);
        given(mockOAuth2Request.getParameter("acr_values")).willReturn(acrValues);
        mockAcrValuesMap(Collections.<String, AuthenticationMethod>singletonMap("2",
                new OpenAMAuthenticationMethod(service, AuthContext.IndexType.SERVICE)));

        // When
        URI loginUri = null;
        try {
            resourceOwnerSessionValidator.validate(mockOAuth2Request);
            fail();
        } catch (ResourceOwnerAuthenticationRequired ex) {
            loginUri = ex.getRedirectUri();
        }

        // Then
        assertTrue(loginUri.getQuery().contains("service=" + service));
    }

    @Test
    public void shouldUseFirstAcrValueThatIsSupported() throws Exception {
        // Given
        String acrValues = "1 2 3";
        mockPrompt("login");
        mockSSOToken(NO_SESSION_TOKEN);
        given(mockOAuth2Request.getParameter("acr_values")).willReturn(acrValues);
        final Map<String, AuthenticationMethod> acrMap = new HashMap<String, AuthenticationMethod>();
        acrMap.put("2", new OpenAMAuthenticationMethod("service2", AuthContext.IndexType.SERVICE));
        acrMap.put("3", new OpenAMAuthenticationMethod("service3", AuthContext.IndexType.SERVICE));

        mockAcrValuesMap(acrMap);

        // When
        URI loginUri = null;
        try {
            resourceOwnerSessionValidator.validate(mockOAuth2Request);
            fail();
        } catch (ResourceOwnerAuthenticationRequired ex) {
            loginUri = ex.getRedirectUri();
        }

        // Then
        assertTrue(loginUri.getQuery().contains("service=service2"));
    }

    @Test
    public void shouldUseDefaultAuthChainIfNoAcrValuesSpecified() throws Exception {
        // Given
        mockPrompt("login");
        mockSSOToken(NO_SESSION_TOKEN);

        // When
        URI loginUri = null;
        try {
            resourceOwnerSessionValidator.validate(mockOAuth2Request);
            fail();
        } catch (ResourceOwnerAuthenticationRequired ex) {
            loginUri = ex.getRedirectUri();
        }

        // Then
        assertFalse(loginUri.getQuery().contains("service="));
    }

    @Test
    public void shouldUseDefaultAuthChainWhenNoSupportedAcrValue() throws Exception {
        // Given
        mockPrompt("login");
        mockSSOToken(NO_SESSION_TOKEN);
        given(mockOAuth2Request.getParameter("acr_values")).willReturn("not_supported");
        mockAcrValuesMap(Collections.<String, AuthenticationMethod>emptyMap());

        // When
        URI loginUri = null;
        try {
            resourceOwnerSessionValidator.validate(mockOAuth2Request);
            fail();
        } catch (ResourceOwnerAuthenticationRequired ex) {
            loginUri = ex.getRedirectUri();
        }

        // Then
        assertFalse(loginUri.getQuery().contains("service="));
    }

    private void mockPrompt(String prompt) {
        restletRequest.setResourceRef(new Reference(
                "http://openam.example.com:8080/openam/oauth2/authorize?prompt=" + prompt));
        given(mockOAuth2Request.getParameter("prompt")).willReturn(prompt);
    }

    private void mockSSOToken(SSOToken ssoToken) throws SSOException {
        given(mockSSOTokenManager.createSSOToken(mockHttpServletRequest)).willReturn(ssoToken);
    }

    private void mockAcrValuesMap(Map<String, AuthenticationMethod> mapping) throws Exception {
        final OAuth2ProviderSettings mockSettings = mock(OAuth2ProviderSettings.class);
        given(mockProviderSettingsFactory.get(mockOAuth2Request)).willReturn(mockSettings);
        given(mockSettings.getAcrMapping()).willReturn(mapping);
    }
}
