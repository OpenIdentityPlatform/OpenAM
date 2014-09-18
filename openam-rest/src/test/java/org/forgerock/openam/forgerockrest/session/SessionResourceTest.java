/*
 * Copyright 2013-2014 ForgeRock AS.
 *
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
 */

package org.forgerock.openam.forgerockrest.session;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenID;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.shared.ldap.util.DN;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResultHandler;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.openam.forgerockrest.session.query.SessionQueryManager;
import org.forgerock.openam.rest.resource.SSOTokenContext;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.security.Principal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.mock;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class SessionResourceTest {

    private SessionResource sessionResource;

    private SessionQueryManager sessionQueryManager;
    private SSOTokenManager ssoTokenManager;

    private AMIdentity amIdentity;

    @BeforeMethod
    public void setUp() throws IdRepoException {

        sessionQueryManager = mock(SessionQueryManager.class);
        ssoTokenManager = mock(SSOTokenManager.class);

        amIdentity = new AMIdentity(new DN("id=demo,dc=example,dc=com"), null);

        sessionResource = new SessionResource(sessionQueryManager, ssoTokenManager) {
            @Override
            AMIdentity getIdentity(SSOToken ssoToken) throws IdRepoException, SSOException {
                return amIdentity;
            }

            @Override
            String convertDNToRealm(String dn) {
                return "/";
            }
        };
    }

    @Test
    public void shouldUseSessionQueryManagerForAllSessionsQuery() {
        // Given
        String badger = "badger";
        String weasel = "weasel";

        SessionQueryManager mockManager = mock(SessionQueryManager.class);
        QueryRequest request = mock(QueryRequest.class);
        given(request.getQueryId()).willReturn(SessionResource.KEYWORD_ALL);
        QueryResultHandler handler = mock(QueryResultHandler.class);

        SessionResource resource = spy(new SessionResource(mockManager, null));
        List<String> list = Arrays.asList(new String[]{badger, weasel});
        doReturn(list).when(resource).getAllServerIds();

        // When
        resource.queryCollection(null, request, handler);

        // Then
        List<String> result = Arrays.asList(new String[]{badger, weasel});
        verify(mockManager, times(1)).getAllSessions(result);
    }

    @Test
    public void shouldQueryNamedServerInServerMode() {
        // Given
        String badger = "badger";

        SessionQueryManager mockManager = mock(SessionQueryManager.class);
        QueryResultHandler mockHandler = mock(QueryResultHandler.class);
        QueryRequest request = mock(QueryRequest.class);
        given(request.getQueryId()).willReturn(badger);

        SessionResource resource = spy(new SessionResource(mockManager, null));

        // When
        resource.queryCollection(null, request, mockHandler);

        // Then
        verify(resource, times(0)).getAllServerIds();

        List<String> result = Arrays.asList(new String[]{badger});
        verify(mockManager, times(1)).getAllSessions(result);
    }

    @Test
    public void actionCollectionShouldFailToValidateSessionWhenSSOTokenIdNotSet() {

        //Given
        final Map<String, Object> authzContext = new HashMap<String, Object>();
        authzContext.put("tokenId", null);
        final SSOTokenContext tokenContext = mock(SSOTokenContext.class);
        final ServerContext context = new ServerContext(tokenContext);
        final ActionRequest request = mock(ActionRequest.class);
        final ResultHandler<JsonValue> handler = mock(ResultHandler.class);

        given(request.getAction()).willReturn("validate");

        //When
        sessionResource.actionCollection(context, request, handler);

        //Then
        verify(handler).handleError(Matchers.isA(BadRequestException.class));
    }

    @Test
    public void actionCollectionShouldValidateSessionAndReturnTrueWhenSSOTokenValid() throws SSOException {

        //Given
        final Map<String, Object> authzContext = new HashMap<String, Object>();
        authzContext.put("tokenId", "SSO_TOKEN_ID");
        final SSOTokenContext tokenContext = mock(SSOTokenContext.class);
        final ServerContext context = new ServerContext(tokenContext);
        final ActionRequest request = mock(ActionRequest.class);
        final ResultHandler<JsonValue> handler = mock(ResultHandler.class);
        final SSOToken ssoToken = mock(SSOToken.class);
        final SSOTokenID ssoTokenId = mock(SSOTokenID.class);

        given(request.getAction()).willReturn("validate");
        given(tokenContext.getCallerSSOToken(ssoTokenManager)).willReturn(ssoToken);
        given(ssoTokenManager.isValidToken(ssoToken)).willReturn(true);
        given(ssoToken.getTokenID()).willReturn(ssoTokenId);
        given(ssoTokenId.toString()).willReturn("SSO_TOKEN_ID");

        //When
        sessionResource.actionCollection(context, request, handler);

        //Then
        final ArgumentCaptor<JsonValue> responseCaptor = ArgumentCaptor.forClass(JsonValue.class);
        verify(handler).handleResult(responseCaptor.capture());
        Assertions.assertThat(responseCaptor.getValue().get("valid").asBoolean()).isTrue();
        Assertions.assertThat(responseCaptor.getValue().get("uid").asString()).isEqualTo("demo");
        Assertions.assertThat(responseCaptor.getValue().get("realm").asString()).isEqualTo("/");
    }

    @Test
    public void actionInstanceShouldValidateSessionAndReturnFalseWhenSSOTokenCreationThrowsException()
            throws SSOException {

        //Given
        final ServerContext context = mock(ServerContext.class);
        final String resourceId = "SSO_TOKEN_ID";
        final ActionRequest request = mock(ActionRequest.class);
        final ResultHandler<JsonValue> handler = mock(ResultHandler.class);

        given(request.getAction()).willReturn("validate");
        doThrow(SSOException.class).when(ssoTokenManager).createSSOToken("SSO_TOKEN_ID");

        //When
        sessionResource.actionInstance(context, resourceId, request, handler);

        //Then
        final ArgumentCaptor<JsonValue> responseCaptor = ArgumentCaptor.forClass(JsonValue.class);
        verify(handler).handleResult(responseCaptor.capture());
        Assertions.assertThat(responseCaptor.getValue().get("valid").asBoolean()).isFalse();
    }

    @Test
    public void actionInstanceShouldValidateSessionAndReturnTrueWhenSSOTokenValid() throws SSOException {

        //Given
        final ServerContext context = mock(ServerContext.class);
        final String resourceId = "SSO_TOKEN_ID";
        final ActionRequest request = mock(ActionRequest.class);
        final ResultHandler<JsonValue> handler = mock(ResultHandler.class);
        final SSOToken ssoToken = mock(SSOToken.class);
        final Principal principal = mock(Principal.class);

        given(request.getAction()).willReturn("validate");
        given(ssoTokenManager.createSSOToken("SSO_TOKEN_ID")).willReturn(ssoToken);
        given(ssoTokenManager.isValidToken(ssoToken)).willReturn(true);
        given(ssoToken.getPrincipal()).willReturn(principal);
        given(principal.getName()).willReturn("PRINCIPAL");

        //When
        sessionResource.actionInstance(context, resourceId, request, handler);

        //Then
        final ArgumentCaptor<JsonValue> responseCaptor = ArgumentCaptor.forClass(JsonValue.class);
        verify(handler).handleResult(responseCaptor.capture());
        Assertions.assertThat(responseCaptor.getValue().get("valid").asBoolean()).isTrue();
        Assertions.assertThat(responseCaptor.getValue().get("uid").asString()).isEqualTo("demo");
        Assertions.assertThat(responseCaptor.getValue().get("realm").asString()).isEqualTo("/");
    }
}
