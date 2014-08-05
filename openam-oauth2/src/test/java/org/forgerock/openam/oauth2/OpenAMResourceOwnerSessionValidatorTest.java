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
import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.oauth2.core.exceptions.BadRequestException;
import org.forgerock.oauth2.core.exceptions.InteractionRequiredException;
import org.forgerock.oauth2.core.exceptions.LoginRequiredException;
import org.forgerock.oauth2.core.exceptions.ResourceOwnerAuthenticationRequired;
import org.restlet.Request;
import org.restlet.data.Reference;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.servlet.http.HttpServletRequest;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @since 12.0.0
 */
public class OpenAMResourceOwnerSessionValidatorTest {

    private static final SSOToken ACTIVE_SESSION_TOKEN = mock(SSOToken.class);
    private static final SSOToken NO_SESSION_TOKEN = null;

    private OpenAMResourceOwnerSessionValidator resourceOwnerSessionValidator;

    private SSOTokenManager mockSSOTokenManager;
    private OAuth2Request mockOAuth2Request;
    private Request restletRequest;
    private HttpServletRequest mockHttpServletRequest;

    @BeforeMethod
    public void setUp() throws Exception {

        mockSSOTokenManager = mock(SSOTokenManager.class);
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

        resourceOwnerSessionValidator = new OpenAMResourceOwnerSessionValidator(mockSSOTokenManager) {
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

    private void mockPrompt(String prompt) {
        restletRequest.setResourceRef(new Reference(
                "http://openam.example.com:8080/openam/oauth2/authorize?prompt=" + prompt));
        given(mockOAuth2Request.getParameter("prompt")).willReturn(prompt);
    }

    private void mockSSOToken(SSOToken ssoToken) throws SSOException {
        given(mockSSOTokenManager.createSSOToken(mockHttpServletRequest)).willReturn(ssoToken);
    }

}
