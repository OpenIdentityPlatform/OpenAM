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
package org.forgerock.oauth2.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

import jakarta.servlet.http.HttpServletRequest;

import org.forgerock.oauth2.core.exceptions.InvalidClientException;
import org.forgerock.oauth2.core.exceptions.NotFoundException;
import org.forgerock.openam.oauth2.OAuth2Constants;
import org.forgerock.openam.rest.representations.JacksonRepresentationFactory;
import org.mockito.ArgumentCaptor;
import org.restlet.engine.adapter.HttpRequest;
import org.forgerock.openam.rest.jakarta.servlet.internal.ServletCall;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

public class OAuth2RequestFactoryTest {

    private static final String CLIENT_ID = "client_id";

    private ClientRegistrationStore clientRegistrationStore;
    private OAuth2RequestFactory factory;
    private HttpServletRequest httpServletRequest;

    @BeforeMethod
    public void setUpTest() {
        clientRegistrationStore = mock(ClientRegistrationStore.class);
        JacksonRepresentationFactory jacksonRepresentationFactory =
                new JacksonRepresentationFactory(new ObjectMapper());
        factory = new OAuth2RequestFactory(jacksonRepresentationFactory, clientRegistrationStore);
    }

    @Test
    public void clientRegistrationIsAddedToOAuth2Request() throws NotFoundException, InvalidClientException {
        ClientRegistration clientRegistration = mock(ClientRegistration.class);
        when(clientRegistrationStore.get(eq(CLIENT_ID), any(OAuth2Request.class))).thenReturn(clientRegistration);
        HttpRequest request = getRequest(CLIENT_ID);

        OAuth2Request oAuth2Request = factory.create(request);

        assertThat(oAuth2Request.getClientRegistration()).isEqualTo(clientRegistration);
    }

    @Test
    public void oAuth2RequestWithClientRegistrationIsAddedToHttpRequest()
            throws NotFoundException, InvalidClientException {
        ClientRegistration clientRegistration = mock(ClientRegistration.class);
        when(clientRegistrationStore.get(eq(CLIENT_ID), any(OAuth2Request.class))).thenReturn(clientRegistration);
        HttpRequest request = getRequest(CLIENT_ID);

        factory.create(request);

        ArgumentCaptor<OAuth2Request> argument = ArgumentCaptor.forClass(OAuth2Request.class);
        verify(httpServletRequest, times(1)).setAttribute(eq("OAUTH2_REQ_ATTR"), argument.capture());
        assertThat(argument.getValue().getClientRegistration()).isEqualTo(clientRegistration);
    }

    @Test
    public void clientRegistrationIsNullWhenClientIdIsNotProvided() throws NotFoundException, InvalidClientException {
        ClientRegistration clientRegistration = mock(ClientRegistration.class);
        when(clientRegistrationStore.get(eq(CLIENT_ID), any(OAuth2Request.class))).thenReturn(clientRegistration);
        HttpRequest request = getRequest(null);

        OAuth2Request oAuth2Request = factory.create(request);

        assertThat(oAuth2Request.getClientRegistration()).isNull();
    }

    @Test
    public void clientRegistrationIsNullWhenClientIdIsBlank() throws NotFoundException, InvalidClientException {
        ClientRegistration clientRegistration = mock(ClientRegistration.class);
        when(clientRegistrationStore.get(eq(CLIENT_ID), any(OAuth2Request.class))).thenReturn(clientRegistration);
        HttpRequest request = getRequest("");

        OAuth2Request oAuth2Request = factory.create(request);

        assertThat(oAuth2Request.getClientRegistration()).isNull();
    }

    @Test
    public void clientRegistrationIsNullWhenStoreThrowsException() throws NotFoundException, InvalidClientException {
        doThrow(InvalidClientException.class).when(clientRegistrationStore).get(eq(CLIENT_ID), any(OAuth2Request.class));
        HttpRequest request = getRequest(null);

        OAuth2Request oAuth2Request = factory.create(request);

        assertThat(oAuth2Request.getClientRegistration()).isNull();
    }

    private HttpRequest getRequest(String clientId) {
        httpServletRequest = mock(HttpServletRequest.class);
        when(httpServletRequest.getParameter(OAuth2Constants.Params.CLIENT_ID)).thenReturn(clientId);

        ServletCall servletCall = mock(ServletCall.class);
        when(servletCall.getRequest()).thenReturn(httpServletRequest);

        HttpRequest request = mock(HttpRequest.class);
        when(request.getHttpCall()).thenReturn(servletCall);

        return request;
    }
}
