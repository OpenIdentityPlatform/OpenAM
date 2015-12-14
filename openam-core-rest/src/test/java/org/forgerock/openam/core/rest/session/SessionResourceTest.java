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
 * Copyright 2013-2015 ForgeRock AS.
 */

package org.forgerock.openam.core.rest.session;

import static org.forgerock.json.JsonValue.*;
import static org.forgerock.json.resource.test.assertj.AssertJActionResponseAssert.*;
import static org.forgerock.openam.core.rest.session.SessionResource.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anySetOf;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.testng.AssertJUnit.*;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenID;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.authentication.spi.AMPostAuthProcessInterface;
import com.sun.identity.delegation.DelegationException;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.shared.debug.Debug;

import java.security.Principal;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.*;
import org.forgerock.openam.authentication.service.AuthUtilsWrapper;
import org.forgerock.openam.core.rest.session.query.SessionQueryManager;
import org.forgerock.openam.rest.RealmContext;
import org.forgerock.openam.rest.resource.SSOTokenContext;
import org.forgerock.openam.session.SessionPropertyWhitelist;
import org.forgerock.opendj.ldap.DN;
import org.forgerock.services.context.AttributesContext;
import org.forgerock.services.context.ClientContext;
import org.forgerock.services.context.Context;
import org.forgerock.services.context.SecurityContext;
import org.forgerock.services.context.RootContext;
import org.forgerock.http.session.Session;
import org.forgerock.http.session.SessionContext;
import org.forgerock.util.promise.Promise;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class SessionResourceTest {

    final SSOToken ssoToken = mock(SSOToken.class);
    final SSOTokenContext mockContext = mock(SSOTokenContext.class);

    private SessionResource sessionResource;
    private SSOTokenManager ssoTokenManager;
    private AuthUtilsWrapper authUtilsWrapper;
    private SessionPropertyWhitelist propertyWhitelist;
    private RealmContext realmContext;

    private AMIdentity amIdentity;

    private String headerResponse;
    private String urlResponse;
    private String cookieResponse;

    @BeforeMethod
    public void setUp() throws IdRepoException, SSOException {
        SessionQueryManager sessionQueryManager = mock(SessionQueryManager.class);
        ssoTokenManager = mock(SSOTokenManager.class);
        authUtilsWrapper = mock(AuthUtilsWrapper.class);
        propertyWhitelist = mock(SessionPropertyWhitelist.class);
        headerResponse = null;
        urlResponse = null;
        cookieResponse = null;

        given(mockContext.getCallerSSOToken()).willReturn(ssoToken);

        realmContext = new RealmContext(mockContext);

        amIdentity = new AMIdentity(DN.valueOf("id=demo,dc=example,dc=com"), null);

        configureWhitelist();
        sessionResource = new SessionResource(sessionQueryManager, ssoTokenManager, authUtilsWrapper,
                propertyWhitelist) {
            @Override
            AMIdentity getIdentity(SSOToken ssoToken) throws IdRepoException, SSOException {
                return amIdentity;
            }

            @Override
            String convertDNToRealm(String dn) {
                return "/";
            }

            @Override
            protected String getTokenIdFromHeader(Context context, String cookieName) {
                return headerResponse;
            }

            @Override
            protected String getTokenIdFromUrlParam(ActionRequest request) {
                return urlResponse;
            }

            @Override
            protected String getTokenIdFromCookie(Context context, String cookieName) {
                return cookieResponse;
            }
        };
    }

    private void configureWhitelist() {
        Set<String> whitelist = new HashSet<>();
        whitelist.add("one");
        whitelist.add("two");
        whitelist.add("three");

        given(propertyWhitelist.getAllListedProperties(any(SSOToken.class), any(String.class))).willReturn(whitelist);

    }

    @Test
    public void shouldUseSessionQueryManagerForAllSessionsQuery() {
        // Given
        String badger = "badger";
        String weasel = "weasel";

        SessionQueryManager mockManager = mock(SessionQueryManager.class);
        QueryRequest request = mock(QueryRequest.class);
        given(request.getQueryId()).willReturn(SessionResource.KEYWORD_ALL);
        QueryResourceHandler handler = mock(QueryResourceHandler.class);

        SessionResource resource = spy(new SessionResource(mockManager, null, null, null));
        List<String> list = Arrays.asList(badger, weasel);
        doReturn(list).when(resource).getAllServerIds();

        // When
        resource.queryCollection(null, request, handler);

        // Then
        List<String> result = Arrays.asList(badger, weasel);
        verify(mockManager, times(1)).getAllSessions(result);
    }

    @Test
    public void shouldQueryNamedServerInServerMode() {
        // Given
        String badger = "badger";

        SessionQueryManager mockManager = mock(SessionQueryManager.class);
        QueryResourceHandler mockHandler = mock(QueryResourceHandler.class);
        QueryRequest request = mock(QueryRequest.class);
        given(request.getQueryId()).willReturn(badger);

        SessionResource resource = spy(new SessionResource(mockManager, null, null, null));

        // When
        resource.queryCollection(null, request, mockHandler);

        // Then
        verify(resource, times(0)).getAllServerIds();

        List<String> result = Collections.singletonList(badger);
        verify(mockManager, times(1)).getAllSessions(result);
    }

    @Test
    public void actionCollectionShouldFailToValidateSessionWhenSSOTokenIdNotSet() {
        //Given
        final SSOTokenContext tokenContext = mock(SSOTokenContext.class);
        final Context context = ClientContext.newInternalClientContext(tokenContext);
        final ActionRequest request = mock(ActionRequest.class);

        given(request.getAction()).willReturn(VALIDATE_ACTION_ID);

        //When
        Promise<ActionResponse, ResourceException> promise = sessionResource.actionCollection(context, request);

        //Then
        assertThat(promise).failedWithException().isInstanceOf(BadRequestException.class);
    }

    @Test
    public void actionCollectionShouldValidateSessionAndReturnTrueWhenSSOTokenValid() throws SSOException {
        //Given
        cookieResponse = "SSO_TOKEN_ID";
        final SSOTokenContext tokenContext = mock(SSOTokenContext.class);
        final Context context = ClientContext.newInternalClientContext(tokenContext);
        final ActionRequest request = mock(ActionRequest.class);
        final SSOToken ssoToken = mock(SSOToken.class);
        final SSOTokenID ssoTokenId = mock(SSOTokenID.class);

        given(request.getAction()).willReturn(VALIDATE_ACTION_ID);
        given(tokenContext.getCallerSSOToken()).willReturn(ssoToken);
        given(ssoTokenManager.isValidToken(ssoToken)).willReturn(true);
        given(ssoToken.getTokenID()).willReturn(ssoTokenId);
        given(ssoTokenId.toString()).willReturn("SSO_TOKEN_ID");
        given(ssoTokenManager.createSSOToken(ssoTokenId.toString())).willReturn(ssoToken);

        //When
        Promise<ActionResponse, ResourceException> promise = sessionResource.actionCollection(context, request);

        //Then
        assertThat(promise).succeeded().withContent().booleanAt("valid").isTrue();
        assertThat(promise).succeeded().withContent().stringAt("uid").isEqualTo("demo");
        assertThat(promise).succeeded().withContent().stringAt("realm").isEqualTo("/");
    }

    @Test
    public void actionCollectionShouldLogoutSessionAndReturnEmptyJsonObjectWhenSSOTokenValid() throws SSOException {
        //Given
        cookieResponse = "SSO_TOKEN_ID";
        final AttributesContext attrContext = new AttributesContext(new SessionContext(new RootContext(), mock(Session.class)));
        final AdviceContext adviceContext = new AdviceContext(attrContext, Collections.<String>emptySet());
        final SecurityContext securityContext = new SecurityContext(adviceContext, null, null);
        final Context context = ClientContext.newInternalClientContext(new SSOTokenContext(mock(Debug.class), null, securityContext));
        final ActionRequest request = mock(ActionRequest.class);
        final SSOTokenID ssoTokenId = mock(SSOTokenID.class);

        given(request.getAction()).willReturn(LOGOUT_ACTION_ID);
        given(authUtilsWrapper.logout(ssoTokenId.toString(), null, null)).willReturn(true);

        //When
        Promise<ActionResponse, ResourceException> promise = sessionResource.actionCollection(context, request);

        //Then
        assertThat(promise).succeeded().withContent().stringAt("result").isEqualTo("Successfully logged out");
    }

    @Test
    public void actionInstanceShouldValidateSessionAndReturnFalseWhenSSOTokenCreationThrowsException()
            throws SSOException {
        //Given
        final Context context = mock(Context.class);
        final String resourceId = "SSO_TOKEN_ID";
        final ActionRequest request = mock(ActionRequest.class);

        given(request.getAction()).willReturn(VALIDATE_ACTION_ID);
        doThrow(SSOException.class).when(ssoTokenManager).createSSOToken("SSO_TOKEN_ID");

        //When
        Promise<ActionResponse, ResourceException> promise = sessionResource.actionInstance(context, resourceId, request);

        //Then
        assertThat(promise).succeeded().withContent().booleanAt("valid").isFalse();
    }

    @Test
    public void actionInstanceShouldValidateSessionAndReturnTrueWhenSSOTokenValid() throws SSOException {
        //Given
        final Context context = mock(Context.class);
        final String resourceId = "SSO_TOKEN_ID";
        final ActionRequest request = mock(ActionRequest.class);
        final SSOToken ssoToken = mock(SSOToken.class);
        final Principal principal = mock(Principal.class);

        given(request.getAction()).willReturn(VALIDATE_ACTION_ID);
        given(ssoTokenManager.createSSOToken("SSO_TOKEN_ID")).willReturn(ssoToken);
        given(ssoTokenManager.isValidToken(ssoToken)).willReturn(true);
        given(ssoToken.getPrincipal()).willReturn(principal);
        given(principal.getName()).willReturn("PRINCIPAL");

        //When
        Promise<ActionResponse, ResourceException> promise = sessionResource.actionInstance(context, resourceId, request);

        //Then
        assertThat(promise).succeeded().withContent().booleanAt("valid").isTrue();
        assertThat(promise).succeeded().withContent().stringAt("uid").isEqualTo("demo");
        assertThat(promise).succeeded().withContent().stringAt("realm").isEqualTo("/");
    }

    @Test
    public void actionInstanceShouldBeActiveWhenSSOTokenValid() throws SSOException {
        //Given
        final Context context = mock(Context.class);
        final String resourceId = "SSO_TOKEN_ID";
        final ActionRequest request = mock(ActionRequest.class);
        final SSOToken ssoToken = mock(SSOToken.class);

        given(request.getAction()).willReturn(IS_ACTIVE_ACTION_ID);
        given(ssoTokenManager.retrieveValidTokenWithoutResettingIdleTime("SSO_TOKEN_ID")).willReturn(ssoToken);
        given(ssoTokenManager.isValidToken(ssoToken, false)).willReturn(true);

        //When
        Promise<ActionResponse, ResourceException> promise = sessionResource.actionInstance(context, resourceId, request);

        //Then
        assertThat(promise).succeeded().withContent().booleanAt("active").isTrue();
    }

    @Test
    public void actionInstanceShouldRefreshWhenParameterPresentAndSSOTokenValid() throws SSOException {
        //Given
        final Context context = mock(Context.class);
        final String resourceId = "SSO_TOKEN_ID";
        final ActionRequest request = mock(ActionRequest.class);
        final SSOToken ssoToken = mock(SSOToken.class);

        given(request.getAction()).willReturn(IS_ACTIVE_ACTION_ID);
        given(request.getAdditionalParameter("refresh")).willReturn("true");
        given(ssoTokenManager.retrieveValidTokenWithoutResettingIdleTime("SSO_TOKEN_ID")).willReturn(ssoToken);
        given(ssoTokenManager.isValidToken(ssoToken, false)).willReturn(true);

        //When
        Promise<ActionResponse, ResourceException> promise = sessionResource.actionInstance(context, resourceId, request);

        //Then
        verify(ssoTokenManager).refreshSession(ssoToken);
        assertThat(promise).succeeded();
    }

    @Test
    public void actionInstanceShouldBeInactiveWhenSSOTokenInvalid() throws SSOException {
        //Given
        final Context context = mock(Context.class);
        final String resourceId = "SSO_TOKEN_ID";
        final ActionRequest request = mock(ActionRequest.class);
        final SSOToken ssoToken = mock(SSOToken.class);

        given(request.getAction()).willReturn(IS_ACTIVE_ACTION_ID);
        given(ssoTokenManager.retrieveValidTokenWithoutResettingIdleTime("SSO_TOKEN_ID")).willReturn(ssoToken);
        given(ssoTokenManager.isValidToken(ssoToken, false)).willReturn(false);

        //When
        Promise<ActionResponse, ResourceException> promise = sessionResource.actionInstance(context, resourceId, request);

        //Then
        assertThat(promise).succeeded().withContent().booleanAt("active").isFalse();
    }

    @Test
    public void actionInstanceShouldGiveTimeLeftWhenSSOTokenValid() throws SSOException {

        final int TIME_LEFT = 5000;

        //Given
        final Context context = mock(Context.class);
        final String resourceId = "SSO_TOKEN_ID";
        final ActionRequest request = mock(ActionRequest.class);
        final SSOToken ssoToken = mock(SSOToken.class);

        given(request.getAction()).willReturn(GET_MAX_TIME_ACTION_ID);
        given(ssoTokenManager.retrieveValidTokenWithoutResettingIdleTime("SSO_TOKEN_ID")).willReturn(ssoToken);
        given(ssoTokenManager.isValidToken(ssoToken, false)).willReturn(true);
        given(ssoToken.getTimeLeft()).willReturn((long) TIME_LEFT);

        //When
        Promise<ActionResponse, ResourceException> promise = sessionResource.actionInstance(context, resourceId, request);

        //Then
        assertThat(promise).succeeded().withContent().integerAt("maxtime").isEqualTo(TIME_LEFT);
    }

    @Test
    public void actionInstanceShouldGiveMinusOneForMaxTimeWhenSSOTokenInvalid() throws SSOException {
        //Given
        final Context context = mock(Context.class);
        final String resourceId = "SSO_TOKEN_ID";
        final ActionRequest request = mock(ActionRequest.class);
        final SSOToken ssoToken = mock(SSOToken.class);

        given(request.getAction()).willReturn(GET_MAX_TIME_ACTION_ID);
        given(ssoTokenManager.retrieveValidTokenWithoutResettingIdleTime("SSO_TOKEN_ID")).willReturn(ssoToken);
        given(ssoTokenManager.isValidToken(ssoToken, false)).willReturn(false);

        //When
        Promise<ActionResponse, ResourceException> promise = sessionResource.actionInstance(context, resourceId, request);

        //Then
        assertThat(promise).succeeded().withContent().integerAt("maxtime").isEqualTo(-1);
    }

    @Test
    public void actionInstanceShouldGiveIdleTimeWhenSSOTokenValid() throws SSOException {

        final int IDLE = 50;

        //Given
        final Context context = mock(Context.class);
        final String resourceId = "SSO_TOKEN_ID";
        final ActionRequest request = mock(ActionRequest.class);
        final SSOToken ssoToken = mock(SSOToken.class);

        given(request.getAction()).willReturn(GET_IDLE_ACTION_ID);
        given(ssoTokenManager.retrieveValidTokenWithoutResettingIdleTime("SSO_TOKEN_ID")).willReturn(ssoToken);
        given(ssoTokenManager.isValidToken(ssoToken, false)).willReturn(true);
        given(ssoToken.getIdleTime()).willReturn((long) IDLE);

        //When
        Promise<ActionResponse, ResourceException> promise = sessionResource.actionInstance(context, resourceId, request);

        //Then
        assertThat(promise).succeeded().withContent().integerAt("idletime").isEqualTo(IDLE);
    }

    @Test
    public void actionInstanceShouldGiveMinusOneForIdleTimeWhenSSOTokenInvalid() throws SSOException {
        //Given
        final Context context = mock(Context.class);
        final String resourceId = "SSO_TOKEN_ID";
        final ActionRequest request = mock(ActionRequest.class);
        final SSOToken ssoToken = mock(SSOToken.class);

        given(request.getAction()).willReturn(GET_IDLE_ACTION_ID);
        given(ssoTokenManager.retrieveValidTokenWithoutResettingIdleTime("SSO_TOKEN_ID")).willReturn(ssoToken);
        given(ssoTokenManager.isValidToken(ssoToken, false)).willReturn(false);

        //When
        Promise<ActionResponse, ResourceException> promise = sessionResource.actionInstance(context, resourceId, request);

        //Then
        assertThat(promise).succeeded().withContent().integerAt("idletime").isEqualTo(-1);
    }

    @Test
    public void actionInstanceShouldReturnNotSupportedForUnknownAction() throws SSOException {
        //Given
        final Context context = mock(Context.class);
        final String resourceId = "SSO_TOKEN_ID";
        final ActionRequest request = mock(ActionRequest.class);

        given(request.getAction()).willReturn("unknown-action");

        //When
        Promise<ActionResponse, ResourceException> promise = sessionResource.actionInstance(context, resourceId, request);

        //Then
        assertThat(promise).failedWithException().isExactlyInstanceOf(NotSupportedException.class);
    }

    @Test
    public void shouldReturnListOfWhitelistedProperties() throws SSOException {
        //given
        final String resourceId = "SSO_TOKEN_ID";
        final ActionRequest request = mock(ActionRequest.class);
        final SSOToken ssoToken = mock(SSOToken.class);
        given(ssoTokenManager.retrieveValidTokenWithoutResettingIdleTime("SSO_TOKEN_ID")).willReturn(ssoToken);
        given(ssoTokenManager.isValidToken(ssoToken, false)).willReturn(true);
        given(request.getAction()).willReturn(GET_PROPERTY_NAMES_ACTION_ID);

        //when
        Promise<ActionResponse, ResourceException> promise = sessionResource.actionInstance(realmContext, resourceId, request);

        //then
        assertThat(promise).succeeded().withContent().hasArray("properties").hasSize(3);
        assertThat(promise).succeeded().withContent().hasArray("properties").containsOnly("one", "two", "three");
    }

    @Test
    public void shouldReturnAllWhitelistedProperties() throws SSOException {
        //given
        final String resourceId = "SSO_TOKEN_ID";
        final ActionRequest request = mock(ActionRequest.class);
        final SSOToken ssoToken = mock(SSOToken.class);

        given(ssoTokenManager.retrieveValidTokenWithoutResettingIdleTime("SSO_TOKEN_ID")).willReturn(ssoToken);
        given(ssoTokenManager.isValidToken(ssoToken, false)).willReturn(true);
        given(request.getAction()).willReturn(GET_PROPERTY_ACTION_ID);

        given(ssoToken.getProperty(eq("one"))).willReturn("testOne");
        given(ssoToken.getProperty(eq("two"))).willReturn("testTwo");
        given(ssoToken.getProperty(eq("three"))).willReturn("testThree");

        //when
        Promise<ActionResponse, ResourceException> promise = sessionResource.actionInstance(realmContext, resourceId, request);

        //then
        assertThat(promise).succeeded().withContent().stringAt("one").isEqualTo("testOne");
        assertThat(promise).succeeded().withContent().stringAt("two").isEqualTo("testTwo");
        assertThat(promise).succeeded().withContent().stringAt("three").isEqualTo("testThree");
    }

    @Test
    public void shouldFailToGetNonWhitelistedProperty() throws SSOException, DelegationException {
        //given
        final String resourceId = "SSO_TOKEN_ID";
        final ActionRequest request = mock(ActionRequest.class);
        final JsonValue content = json(object(field("properties", array("invalid"))));

        given(request.getContent()).willReturn(content);
        given(ssoTokenManager.retrieveValidTokenWithoutResettingIdleTime("SSO_TOKEN_ID")).willReturn(ssoToken);
        given(ssoTokenManager.isValidToken(ssoToken, false)).willReturn(true);
        given(request.getAction()).willReturn(GET_PROPERTY_ACTION_ID);
        given(propertyWhitelist.userHasReadAdminPrivs(eq(ssoToken), any(String.class))).willReturn(false);

        //when
        Promise<ActionResponse, ResourceException> promise = sessionResource.actionInstance(realmContext, resourceId, request);

        //then
        assertThat(promise).failedWithException().isExactlyInstanceOf(ForbiddenException.class);
    }

    @Test
    public void shouldGetWhitelistedProperty() throws SSOException, DelegationException {
        //given
        final String resourceId = "SSO_TOKEN_ID";
        final ActionRequest request = mock(ActionRequest.class);
        final SSOToken ssoToken = mock(SSOToken.class);

        final JsonValue content = json(object(field("properties", array("one"))));

        given(request.getContent()).willReturn(content);
        given(ssoTokenManager.retrieveValidTokenWithoutResettingIdleTime("SSO_TOKEN_ID")).willReturn(ssoToken);
        given(ssoTokenManager.isValidToken(ssoToken, false)).willReturn(true);
        given(request.getAction()).willReturn(GET_PROPERTY_ACTION_ID);
        given(propertyWhitelist.isPropertyListed(any(SSOToken.class), any(String.class), anySetOf(String.class))).willReturn(true);
        given(ssoToken.getProperty(eq("one"))).willReturn("testOne");

        //when
        Promise<ActionResponse, ResourceException> promise = sessionResource.actionInstance(realmContext, resourceId, request);

        //then
        assertThat(promise).succeeded().withContent().stringAt("one").isEqualTo("testOne");
    }

    @Test
    public void shouldSetWhitelistedProperty() throws SSOException, DelegationException,
            ExecutionException, InterruptedException {
        //given
        final String resourceId = "SSO_TOKEN_ID";
        final ActionRequest request = mock(ActionRequest.class);
        final SSOToken ssoToken = mock(SSOToken.class);

        JsonValue jsonContent = json(object(field("one", "testOne")));

        given(ssoTokenManager.retrieveValidTokenWithoutResettingIdleTime("SSO_TOKEN_ID")).willReturn(ssoToken);
        given(ssoTokenManager.isValidToken(ssoToken, false)).willReturn(true);
        given(request.getAction()).willReturn(SET_PROPERTY_ACTION_ID);
        given(request.getContent()).willReturn(jsonContent);
        given(propertyWhitelist.isPropertyListed(any(SSOToken.class), any(String.class), anySetOf(String.class))).willReturn(true);

        //when
        Promise<ActionResponse, ResourceException> promise = sessionResource.actionInstance(realmContext, resourceId, request);

        //then
        verify(ssoToken).setProperty(eq("one"), eq("testOne"));
        assertThat(promise).succeeded();
        assertTrue(promise.get().getJsonContent().get("success").asBoolean().equals(true));
    }

    @Test
    public void shouldFailToSetNonWhitelistedProperty() throws SSOException, DelegationException {
        //given
        final String resourceId = "SSO_TOKEN_ID";
        final ActionRequest request = mock(ActionRequest.class);
        final SSOToken ssoToken = mock(SSOToken.class);

        JsonValue jsonContent = json(object(field("invalid", "testInvalid")));

        given(ssoTokenManager.retrieveValidTokenWithoutResettingIdleTime("SSO_TOKEN_ID")).willReturn(ssoToken);
        given(ssoTokenManager.isValidToken(ssoToken, false)).willReturn(true);
        given(request.getAction()).willReturn(SET_PROPERTY_ACTION_ID);
        given(request.getContent()).willReturn(jsonContent);
        given(propertyWhitelist.isPropertyListed(any(SSOToken.class), any(String.class), anySetOf(String.class))).willReturn(false);

        //when
        Promise<ActionResponse, ResourceException> promise = sessionResource.actionInstance(realmContext, resourceId, request);

        //then
        assertThat(promise).failedWithException().isExactlyInstanceOf(ForbiddenException.class);
    }

    @Test
    public void shouldFailToSetInvalidRequest() throws SSOException, DelegationException {
        //given
        final String resourceId = "SSO_TOKEN_ID";
        final ActionRequest request = mock(ActionRequest.class);
        final SSOToken ssoToken = mock(SSOToken.class);

        JsonValue jsonContent = json(object());

        given(ssoTokenManager.retrieveValidTokenWithoutResettingIdleTime("SSO_TOKEN_ID")).willReturn(ssoToken);
        given(ssoTokenManager.isValidToken(ssoToken, false)).willReturn(true);
        given(request.getAction()).willReturn(SET_PROPERTY_ACTION_ID);
        given(request.getContent()).willReturn(jsonContent);
        given(propertyWhitelist.isPropertyListed(any(SSOToken.class), any(String.class), anySetOf(String.class))).willReturn(false);

        //when
        Promise<ActionResponse, ResourceException> promise = sessionResource.actionInstance(realmContext, resourceId, request);

        //then
        assertThat(promise).failedWithException().isExactlyInstanceOf(BadRequestException.class);
    }

    @Test
    public void shouldDeleteWhitelistedProperty() throws SSOException, ExecutionException, InterruptedException, DelegationException {

        //given
        final String resourceId = "SSO_TOKEN_ID";
        final ActionRequest request = mock(ActionRequest.class);
        final JsonValue content = json(object(field("properties", array("one"))));

        given(ssoTokenManager.retrieveValidTokenWithoutResettingIdleTime("SSO_TOKEN_ID")).willReturn(ssoToken);
        given(ssoTokenManager.isValidToken(ssoToken, false)).willReturn(true);
        given(request.getAction()).willReturn(DELETE_PROPERTY_ACTION_ID);
        given(request.getContent()).willReturn(content);
        given(propertyWhitelist.isPropertyListed(any(SSOToken.class), any(String.class), anySetOf(String.class))).willReturn(true);

        //when
        Promise<ActionResponse, ResourceException> promise = sessionResource.actionInstance(realmContext, resourceId, request);

        //then
        verify(ssoToken).setProperty(eq("one"), eq(""));
        assertThat(promise).succeeded();
        assertTrue(promise.get().getJsonContent().get("success").asBoolean().equals(true));
    }

    @Test
    public void shouldReturnFailureWhenSetPropertySessionException() throws SSOException,
            ExecutionException, InterruptedException, DelegationException {
        //given
        final String resourceId = "SSO_TOKEN_ID";
        final ActionRequest request = mock(ActionRequest.class);
        final SSOToken ssoToken = mock(SSOToken.class);

        JsonValue jsonContent = json(object(field("one", "testOne")));

        given(ssoTokenManager.retrieveValidTokenWithoutResettingIdleTime("SSO_TOKEN_ID")).willReturn(ssoToken);
        given(ssoTokenManager.isValidToken(ssoToken, false)).willReturn(true);
        given(request.getAction()).willReturn(SET_PROPERTY_ACTION_ID);
        given(request.getContent()).willReturn(jsonContent);
        given(propertyWhitelist.isPropertyListed(any(SSOToken.class), any(String.class), anySetOf(String.class)))
                .willThrow(new SSOException("Error"));

        //when
        Promise<ActionResponse, ResourceException> promise = sessionResource.actionInstance(realmContext, resourceId, request);

        //then
        assertThat(promise).succeeded();
        assertTrue(promise.get().getJsonContent().get("success").asBoolean().equals(false));
    }

    @Test
    public void shouldReturnInternalErrorWhenSetPropertyFailsWithDelegationException() throws SSOException,
            ExecutionException, InterruptedException, DelegationException {

        //given
        final String resourceId = "SSO_TOKEN_ID";
        final ActionRequest request = mock(ActionRequest.class);
        JsonValue jsonContent = json(object(field("one", "testOne")));

        given(ssoTokenManager.retrieveValidTokenWithoutResettingIdleTime(resourceId)).willReturn(ssoToken);
        given(ssoTokenManager.isValidToken(ssoToken, false)).willReturn(true);
        given(request.getAction()).willReturn(SET_PROPERTY_ACTION_ID);
        given(request.getContent()).willReturn(jsonContent);
        given(propertyWhitelist.isPropertyListed(any(SSOToken.class), any(String.class), anySetOf(String.class)))
                .willThrow(new DelegationException("Error"));

        //when
        Promise<ActionResponse, ResourceException> promise = sessionResource.actionInstance(realmContext, resourceId, request);

        //then
        assertThat(promise).failedWithException().isInstanceOf(InternalServerErrorException.class);
    }

    @Test
    public void shouldReturnInternalErrorWhenGetPropertyFailsWithDelegationException() throws SSOException, DelegationException {
        //given
        final String resourceId = "SSO_TOKEN_ID";
        final ActionRequest request = mock(ActionRequest.class);
        final SSOToken ssoToken = mock(SSOToken.class);

        final JsonValue content = json(object(field("properties", array("one"))));

        given(request.getContent()).willReturn(content);
        given(ssoTokenManager.retrieveValidTokenWithoutResettingIdleTime(resourceId)).willReturn(ssoToken);
        given(ssoTokenManager.isValidToken(ssoToken, false)).willReturn(true);
        given(request.getAction()).willReturn(GET_PROPERTY_ACTION_ID);
        given(propertyWhitelist.isPropertyListed(any(SSOToken.class), any(String.class), anySetOf(String.class)))
                .willThrow(new DelegationException("Error"));

        //when
        Promise<ActionResponse, ResourceException> promise = sessionResource.actionInstance(realmContext, resourceId, request);

        //then
        assertThat(promise).failedWithException().isInstanceOf(InternalServerErrorException.class);
    }

    @Test
    public void shouldReturnFailureWhenDeletePropertySessionException() throws SSOException,
            ExecutionException, InterruptedException, DelegationException {

        //given
        final String resourceId = "SSO_TOKEN_ID";
        final ActionRequest request = mock(ActionRequest.class);
        final JsonValue content = json(object(field("properties", array("one"))));

        given(ssoTokenManager.retrieveValidTokenWithoutResettingIdleTime(resourceId)).willReturn(ssoToken);
        given(ssoTokenManager.isValidToken(ssoToken, false)).willReturn(true);
        given(request.getAction()).willReturn(DELETE_PROPERTY_ACTION_ID);
        given(request.getContent()).willReturn(content);
        given(propertyWhitelist.isPropertyListed(any(SSOToken.class), any(String.class), anySetOf(String.class)))
                .willThrow(new SSOException("Error"));

        //when
        Promise<ActionResponse, ResourceException> promise = sessionResource.actionInstance(realmContext, resourceId, request);

        //then
        assertThat(promise).succeeded();
        assertTrue(promise.get().getJsonContent().get("success").asBoolean().equals(false));
    }

    @Test
    public void shouldReturnInternalErrorWhenDeletePropertyFailsWithDelegationException() throws SSOException,
            ExecutionException, InterruptedException, DelegationException {

        //given
        final String resourceId = "SSO_TOKEN_ID";
        final ActionRequest request = mock(ActionRequest.class);
        final JsonValue content = json(object(field("properties", array("one"))));

        given(ssoTokenManager.retrieveValidTokenWithoutResettingIdleTime(resourceId)).willReturn(ssoToken);
        given(ssoTokenManager.isValidToken(ssoToken, false)).willReturn(true);
        given(request.getAction()).willReturn(DELETE_PROPERTY_ACTION_ID);
        given(request.getContent()).willReturn(content);
        given(propertyWhitelist.isPropertyListed(any(SSOToken.class), any(String.class), anySetOf(String.class)))
                .willThrow(new DelegationException("Error"));

        //when
        Promise<ActionResponse, ResourceException> promise = sessionResource.actionInstance(realmContext, resourceId, request);

        //then
        assertThat(promise).failedWithException().isInstanceOf(InternalServerErrorException.class);
    }

    @Test
    public void shouldFailToDeleteNonWhitelistedProperty() throws SSOException, DelegationException {

        //given
        final String resourceId = "SSO_TOKEN_ID";
        final ActionRequest request = mock(ActionRequest.class);
        final JsonValue content = json(object(field("properties", array("invalid"))));

        given(ssoTokenManager.retrieveValidTokenWithoutResettingIdleTime(resourceId)).willReturn(ssoToken);
        given(ssoTokenManager.isValidToken(ssoToken, false)).willReturn(true);
        given(request.getAction()).willReturn(DELETE_PROPERTY_ACTION_ID);
        given(request.getContent()).willReturn(content);
        given(propertyWhitelist.isPropertyListed(any(SSOToken.class), any(String.class), anySetOf(String.class))).willReturn(false);
        given(mockContext.getCallerSSOToken()).willReturn(ssoToken);
        given(propertyWhitelist.userHasReadAdminPrivs(eq(ssoToken), any(String.class))).willReturn(false);

        //when
        Promise<ActionResponse, ResourceException> promise = sessionResource.actionInstance(realmContext, resourceId, request);

        //then
        assertThat(promise).failedWithException().isExactlyInstanceOf(ForbiddenException.class);
    }

    @Test
    public void shouldFailToDeleteInvalidRequest() throws SSOException, DelegationException {

        //given
        final String resourceId = "SSO_TOKEN_ID";
        final ActionRequest request = mock(ActionRequest.class);
        final SSOToken ssoToken = mock(SSOToken.class);

        final JsonValue content = json(object());

        given(ssoTokenManager.retrieveValidTokenWithoutResettingIdleTime(resourceId)).willReturn(ssoToken);
        given(ssoTokenManager.isValidToken(ssoToken, false)).willReturn(true);
        given(request.getAction()).willReturn(DELETE_PROPERTY_ACTION_ID);
        given(request.getContent()).willReturn(content);
        given(propertyWhitelist.isPropertyListed(any(SSOToken.class), any(String.class), anySetOf(String.class))).willReturn(false);

        //when
        Promise<ActionResponse, ResourceException> promise = sessionResource.actionInstance(realmContext, resourceId, request);

        //then
        assertThat(promise).failedWithException().isExactlyInstanceOf(BadRequestException.class);
    }

    @Test
    public void shouldUsePAPLogoutRedirectUrlWhenSet() throws Exception {
        // Given
        final String sessionId = "SSO_TOKEN_ID";
        final String logoutUrl = "http://forgerock.com/about";
        final ActionRequest request = mock(ActionRequest.class);
        final HttpServletRequest httpServletRequest = mock(HttpServletRequest.class);
        final AttributesContext context = new AttributesContext(new AdviceContext(realmContext,
                Collections.<String>emptyList()));
        final SSOTokenID tokenId = mock(SSOTokenID.class);
        context.getAttributes().put(HttpServletRequest.class.getName(), httpServletRequest);

        given(request.getAction()).willReturn(LOGOUT_ACTION_ID);
        given(ssoTokenManager.createSSOToken(sessionId)).willReturn(ssoToken);
        given(ssoToken.getTokenID()).willReturn(tokenId);
        given(tokenId.toString()).willReturn(sessionId);
        given(authUtilsWrapper.logout(eq(sessionId), eq(httpServletRequest), any(HttpServletResponse.class)))
                .willReturn(true);
        given(authUtilsWrapper.getPostProcessLogoutURL(httpServletRequest)).willReturn(logoutUrl);


        // When
        ActionResponse response = sessionResource.actionInstance(context, sessionId, request)
                                                 .getOrThrowUninterruptibly();

        // Then
        assertThat(response).isNotNull().withContent().stringAt("goto").isEqualTo(logoutUrl);
    }
}
