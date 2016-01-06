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
package org.forgerock.openidconnect.restlet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

import org.forgerock.oauth2.core.ClientRegistration;
import org.forgerock.oauth2.core.ClientRegistrationStore;
import org.forgerock.oauth2.core.OAuth2Constants;
import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.oauth2.core.OAuth2RequestFactory;
import org.forgerock.oauth2.core.exceptions.InvalidClientException;
import org.forgerock.oauth2.core.exceptions.NotFoundException;
import org.forgerock.oauth2.restlet.ExceptionHandler;
import org.forgerock.oauth2.restlet.OAuth2RestletException;
import org.forgerock.openidconnect.OpenIDConnectEndSession;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.representation.Representation;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.net.URI;
import java.security.SignatureException;
import java.util.Collections;

public class EndSessionTest {

    private String idToken;
    private EndSession endSession;
    private OAuth2Request oAuth2Request;
    private OpenIDConnectEndSession openIDConnectEndSession;
    private ClientRegistration client;

    @BeforeMethod
    public void setup() throws InvalidClientException, SignatureException, NotFoundException {
        idToken = "eyAidHlwIjogIkpXVCIsICJhbGciOiAiSFMyNTYiIH0.eyAidG9rZW5OYW1lIjogImlkX3Rva2VuIiwgImF6cCI6ICJOZXdPcG" +
                "VuSWRDbGllbnQiLCAic3ViIjogIlRlc3RVc2VyIiwgImF0X2hhc2giOiAibHhSNE1BcGV1aXl0dWxiVFI4OV9wQSIsICJpc3MiOi" +
                "AiaHR0cDovL29wZW5hbS5leGFtcGxlLmNvbTo4MDgwL29wZW5hbS9vYXV0aDIiLCAib3JnLmZvcmdlcm9jay5vcGVuaWRjb25uZW" +
                "N0Lm9wcyI6ICI2OTYzOTc4MC04NjkzLTQ1ODktOTk1Ni05ZThkM2UxZWI2YjQiLCAiaWF0IjogMTQzNjM1MjM4MiwgImF1dGhfdG" +
                "ltZSI6IDE0MzYzNTIzODIsICJleHAiOiAxNDM2MzUyOTgyLCAidG9rZW5UeXBlIjogIkpXVFRva2VuIiwgIm5vbmNlIjogIjEyMz" +
                "Q1IiwgInJlYWxtIjogIi8iLCAiYXVkIjogWyAiTmV3T3BlbklkQ2xpZW50IiBdLCAiY19oYXNoIjogIkY3RENrMkE5cDVmeUN0VF" +
                "hpYmF5V2ciIH0.0uIyHGAsr04gu9H4cJ57UPYVJmSJwjCakozPATlCcuE";
        oAuth2Request = mock(OAuth2Request.class);
        when(oAuth2Request.getParameter(OAuth2Constants.Params.END_SESSION_ID_TOKEN_HINT)).thenReturn(idToken);

        OAuth2RequestFactory<?, Request> requestFactory = mock(OAuth2RequestFactory.class);
        ExceptionHandler exceptionHandler = mock(ExceptionHandler.class);
        ClientRegistrationStore clientRegistrationStore = mock(ClientRegistrationStore.class);
        openIDConnectEndSession = mock(OpenIDConnectEndSession.class);
        endSession = new EndSession(requestFactory, openIDConnectEndSession, exceptionHandler, clientRegistrationStore);
        Request request = mock(Request.class);
        Response response = mock(Response.class);
        when(response.getEntity()).thenReturn(mock(Representation.class));
        endSession.setRequest(request);
        endSession.setResponse(response);
        when(requestFactory.create(any(Request.class))).thenReturn(oAuth2Request);
        client = mock(ClientRegistration.class);
        when(clientRegistrationStore.get(anyString(), any(OAuth2Request.class))).thenReturn(client);
    }

    @Test
    public void shouldAttemptEndSessionAndReturnRedirectAttempt() throws Exception {
        // given
        String requestedUri = "http://www.example.com";
        String registeredUri = "http://www.example.com";
        when(oAuth2Request.getParameter(OAuth2Constants.Params.POST_LOGOUT_REDIRECT_URI)).thenReturn(requestedUri);
        when(client.getPostLogoutRedirectUris()).thenReturn(Collections.singleton(new URI(registeredUri)));

        // when
        Representation result = endSession.endSession();

        // then
        verify(openIDConnectEndSession, times(1)).endSession(any(String.class));
        assertThat(result).isNotNull();
    }

    @Test
    public void shouldAttemptEndSessionAndFailMismatchRedirect() throws Exception {
        // given
        String requestedUri = "http://www.example.com";
        String registeredUri = "http://www.google.com";
        when(oAuth2Request.getParameter(OAuth2Constants.Params.POST_LOGOUT_REDIRECT_URI)).thenReturn(requestedUri);
        when(client.getPostLogoutRedirectUris()).thenReturn(Collections.singleton(new URI(registeredUri)));

        // when
        OAuth2RestletException exception = null;
        try {
            endSession.endSession();
        } catch (OAuth2RestletException e) {
            exception = e;
        }

        // then
        verify(openIDConnectEndSession, times(1)).endSession(any(String.class));
        assertThat(exception).isNotNull();
        assertThat(exception.getError()).isEqualTo("redirect_uri_mismatch");
    }

    @Test
    public void shouldAttemptEndSessionAndFailRelativeRedirect() throws Exception {
        // given
        String requestedUri = "example.com";
        String registeredUri = "http://www.example.com";
        when(oAuth2Request.getParameter(OAuth2Constants.Params.POST_LOGOUT_REDIRECT_URI)).thenReturn(requestedUri);
        when(client.getPostLogoutRedirectUris()).thenReturn(Collections.singleton(new URI(registeredUri)));

        // when
        OAuth2RestletException exception = null;
        try {
            endSession.endSession();
        } catch (OAuth2RestletException e) {
            exception = e;
        }

        // then
        verify(openIDConnectEndSession, times(1)).endSession(any(String.class));
        assertThat(exception).isNotNull();
        assertThat(exception.getError()).isEqualTo("relative_redirect_uri");
    }

    @Test
    public void shouldAttemptEndSessionAndNotRedirect() throws Exception {
        // given
        String requestedUri = "";
        String registeredUri = "http://www.example.com";
        when(oAuth2Request.getParameter(OAuth2Constants.Params.POST_LOGOUT_REDIRECT_URI)).thenReturn(requestedUri);
        when(client.getPostLogoutRedirectUris()).thenReturn(Collections.singleton(new URI(registeredUri)));

        // when
        Representation result = endSession.endSession();

        // then
        verify(openIDConnectEndSession, times(1)).endSession(any(String.class));
        assertThat(result).isNull();
    }
}
