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
 *  Copyright 2016 ForgeRock AS.
 *
 */

package org.forgerock.oauth2.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.openam.oauth2.OAuth2Constants.Params.CLIENT_ID;
import static org.forgerock.openam.oauth2.OAuth2Constants.Params.REDIRECT_URI;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.net.URI;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.forgerock.oauth2.core.exceptions.InvalidClientException;
import org.forgerock.oauth2.core.exceptions.InvalidRequestException;
import org.forgerock.oauth2.core.exceptions.NotFoundException;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * @since 14.0.0
 */
public class RedirectUriResolverTest {


    public static final String SINGLE_REGISTERED_URI = "http://singleregistereduri";

    public static final String REDIRECT_URI_PARAMETER = "http://resolvefromparameter";

    private ClientRegistrationStore clientRegistrationStore;

    private ClientRegistration clientRegistration;

    private RedirectUriResolver resolver;

    private Set<URI> uriSetWithMany;

    private Set<URI> uriSetWithOne;

    @BeforeMethod
    public void setup() {
        clientRegistrationStore = mock(ClientRegistrationStore.class);
        clientRegistration = mock(ClientRegistration.class);
        resolver = new RedirectUriResolver(clientRegistrationStore);

        uriSetWithMany = new HashSet<>(Arrays.asList(new URI[]{URI.create("http://one"),
                URI.create("http://two"), URI.create("http://three")}));

        uriSetWithOne = new HashSet<>(Arrays.asList(new URI[]{URI.create(SINGLE_REGISTERED_URI)}));
    }


    @Test
    public void shouldResolveFromRequestParameterWithManyRegisteredUris() throws NotFoundException, InvalidClientException, InvalidRequestException {
        //given
        OAuth2Request request = mock(OAuth2Request.class);
        given(request.getParameter(REDIRECT_URI)).willReturn(REDIRECT_URI_PARAMETER);
        given(request.getParameter(CLIENT_ID)).willReturn("client_id");
        given(clientRegistration.getRedirectUris()).willReturn(uriSetWithMany);
        given(clientRegistrationStore.get("client_id", request)).willReturn(clientRegistration);


        //when
        String result = resolver.resolve(request);


        //then
        assertThat(result).isEqualTo(REDIRECT_URI_PARAMETER);


    }

    @Test
    public void shouldResolveFromRequestParameterWithOneRegisteredUris() throws NotFoundException, InvalidClientException, InvalidRequestException {
        //given
        OAuth2Request request = mock(OAuth2Request.class);
        given(request.getParameter(REDIRECT_URI)).willReturn(REDIRECT_URI_PARAMETER);
        given(request.getParameter(CLIENT_ID)).willReturn("client_id");
        given(clientRegistration.getRedirectUris()).willReturn(uriSetWithOne);
        given(clientRegistrationStore.get("client_id", request)).willReturn(clientRegistration);


        //when
        String result = resolver.resolve(request);


        //then
        assertThat(result).isEqualTo(REDIRECT_URI_PARAMETER);


    }


    @Test
    public void shouldResolveWhenNoParameterPresentAndOneRegisteredUri() throws InvalidRequestException, NotFoundException, InvalidClientException {

        //given
        OAuth2Request request = mock(OAuth2Request.class);
        given(request.getParameter(REDIRECT_URI)).willReturn(null);
        given(request.getParameter(CLIENT_ID)).willReturn("client_id");
        given(clientRegistration.getRedirectUris()).willReturn(uriSetWithOne);
        given(clientRegistrationStore.get("client_id", request)).willReturn(clientRegistration);


        //when
        String result = resolver.resolve(request);


        //then
        assertThat(result).isEqualTo(SINGLE_REGISTERED_URI);


    }


    @Test(expectedExceptions = InvalidRequestException.class)
    public void shouldThrowExceptionWithParameterAndNullRegisteredUri() throws NotFoundException, InvalidClientException, InvalidRequestException {

        //given
        OAuth2Request request = mock(OAuth2Request.class);
        given(request.getParameter(REDIRECT_URI)).willReturn("http://someuri");
        given(request.getParameter(CLIENT_ID)).willReturn("client_id");
        given(clientRegistration.getRedirectUris()).willReturn(null);
        given(clientRegistrationStore.get("client_id", request)).willReturn(clientRegistration);


        //when
        resolver.resolve(request);


        //then
        //should throw invalid request  exception

    }

    @Test(expectedExceptions = InvalidRequestException.class)
    public void shouldThrowExceptionWithParameterAndEmptyRegisteredUri() throws NotFoundException, InvalidClientException, InvalidRequestException {

        //given
        OAuth2Request request = mock(OAuth2Request.class);
        given(request.getParameter(REDIRECT_URI)).willReturn("http://someuri");
        given(request.getParameter(CLIENT_ID)).willReturn("client_id");
        given(clientRegistration.getRedirectUris()).willReturn(new HashSet<URI>());
        given(clientRegistrationStore.get("client_id", request)).willReturn(clientRegistration);


        //when
        resolver.resolve(request);


        //then
        //should throw invalid request  exception

    }

    @Test(expectedExceptions = InvalidRequestException.class)
    public void shouldThrowExceptionWhenNoParameterAndNullRegisteredUri() throws NotFoundException, InvalidClientException, InvalidRequestException {

        //given
        OAuth2Request request = mock(OAuth2Request.class);
        given(request.getParameter(REDIRECT_URI)).willReturn(null);
        given(request.getParameter(CLIENT_ID)).willReturn("client_id");
        given(clientRegistration.getRedirectUris()).willReturn(null);
        given(clientRegistrationStore.get("client_id", request)).willReturn(clientRegistration);


        //when
        resolver.resolve(request);


        //then
        //should throw invalid request  exception

    }

    @Test(expectedExceptions = InvalidRequestException.class)
    public void shouldThrowExceptionWhenNoParameterAndManyRegisteredUri() throws NotFoundException, InvalidClientException, InvalidRequestException {

        //given
        OAuth2Request request = mock(OAuth2Request.class);
        given(request.getParameter(REDIRECT_URI)).willReturn(null);
        given(request.getParameter(CLIENT_ID)).willReturn("client_id");
        given(clientRegistration.getRedirectUris()).willReturn(uriSetWithMany);
        given(clientRegistrationStore.get("client_id", request)).willReturn(clientRegistration);


        //when
        resolver.resolve(request);


        //then
        //should throw invalid request  exception

    }
}
