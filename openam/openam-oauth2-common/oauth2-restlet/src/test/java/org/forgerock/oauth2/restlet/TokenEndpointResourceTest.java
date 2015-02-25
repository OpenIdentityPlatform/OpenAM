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

package org.forgerock.oauth2.restlet;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.oauth2.core.AccessToken;
import org.forgerock.oauth2.core.AccessTokenService;
import org.forgerock.oauth2.core.OAuth2Constants;
import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.oauth2.core.OAuth2RequestFactory;
import org.forgerock.openam.utils.CollectionUtils;
import org.restlet.Context;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.Status;
import org.restlet.representation.EmptyRepresentation;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

import static org.forgerock.oauth2.core.OAuth2Constants.CoreTokenParams.TOKEN_NAME;
import static org.forgerock.oauth2.core.OAuth2Constants.Token.OAUTH_ACCESS_TOKEN;

public class TokenEndpointResourceTest {

    private OAuth2RequestFactory<Request> requestFactory;
    private AccessTokenService accessTokenService;
    private ExceptionHandler exceptionHandler;
    private TokenRequestHook hook;

    private TokenEndpointResource tokenEndpointResource;

    @BeforeClass
    @SuppressWarnings("unchecked")
    public void setUp() {
        requestFactory = mock(OAuth2RequestFactory.class);
        accessTokenService = mock(AccessTokenService.class);
        OAuth2Representation representation = new OAuth2Representation();
        exceptionHandler = new ExceptionHandler(representation);
        hook = mock(TokenRequestHook.class);

        tokenEndpointResource = new TokenEndpointResource(requestFactory, accessTokenService, exceptionHandler,
                CollectionUtils.asSet(hook));
    }

    @Test
    public void shouldThrowServerErrorForExceptionsThatAreNotOAuth2RestletExceptions() {

        //Given
        Context context = new Context();
        Request request = new Request();
        Response response = new Response(request);
        tokenEndpointResource.init(context, request, response);

        //When
        tokenEndpointResource.doCatch(new NullPointerException());

        //Then
        assertEquals(response.getStatus(), Status.CLIENT_ERROR_BAD_REQUEST);
        assertEquals(response.getEntityAsText(), "{\"error\":\"server_error\"}");
    }

    @Test
    public void testToken() throws Exception {

        //Given
        Context context = new Context();
        Request request = new Request();
        Response response = new Response(request);
        tokenEndpointResource.init(context, request, response);
        doReturn(new AccessToken(null, OAUTH_ACCESS_TOKEN, null))
                .when(accessTokenService).requestAccessToken(any(OAuth2Request.class));

        //When
        tokenEndpointResource.token(new EmptyRepresentation());

        //Then
        verify(hook).afterTokenHandling(any(OAuth2Request.class), eq(request), eq(response));
    }

}
