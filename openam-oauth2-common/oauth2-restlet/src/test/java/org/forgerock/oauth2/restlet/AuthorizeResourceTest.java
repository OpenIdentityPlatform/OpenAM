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

import org.forgerock.oauth2.core.AuthorizationService;
import org.forgerock.oauth2.core.AuthorizationToken;
import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.oauth2.core.OAuth2RequestFactory;
import org.forgerock.openam.utils.CollectionUtils;
import org.forgerock.openam.xui.XUIState;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.representation.EmptyRepresentation;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Collections;

import static org.fest.assertions.Assertions.*;
import static org.mockito.Mockito.*;

public class AuthorizeResourceTest {

    private AuthorizeResource resource;
    private OAuth2Request o2request;
    private Request request;
    private Response response;
    private AuthorizationService service;
    private AuthorizeRequestHook hook;
    private AuthorizationToken authToken = new AuthorizationToken(Collections.singletonMap("fred", "fred"), false);
    private XUIState xuiState;

    @BeforeMethod
    public void setup() throws Exception {
        OAuth2Representation representation = mock(OAuth2Representation.class);
        OAuth2RequestFactory<Request> oauth2RequestFactory = mock(OAuth2RequestFactory.class);
        o2request = mock(OAuth2Request.class);
        request = mock(Request.class);
        response = mock(Response.class);
        hook = mock(AuthorizeRequestHook.class);
        service = mock(AuthorizationService.class);
        xuiState = mock(XUIState.class);

        when(oauth2RequestFactory.create(request)).thenReturn(o2request);

        resource = new AuthorizeResource(oauth2RequestFactory, service, null, representation,
                CollectionUtils.asSet(hook), xuiState);
        resource = spy(resource);
        doReturn(request).when(resource).getRequest();
        doReturn(response).when(resource).getResponse();
    }

    @Test
    public void shouldCallHooksInGet() throws Exception {
        //given
        when(service.authorize(o2request)).thenReturn(authToken);

        //when
        resource.authorize();

        //then
        verify(hook).beforeAuthorizeHandling(o2request, request, response);
        verify(hook).afterAuthorizeSuccess(o2request, request, response);
    }

    @Test
    public void shouldCallHooksInPost() throws Exception {
        //given
        when(service.authorize(o2request)).thenReturn(authToken);

        //when
        resource.authorize(new EmptyRepresentation());

        //then
        verify(hook).beforeAuthorizeHandling(o2request, request, response);
        verify(hook).afterAuthorizeSuccess(o2request, request, response);
    }

}