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
 * Portions copyright 2026 3A Systems, LLC.
 */

package org.forgerock.openam.core.rest.session;


import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.resource.test.assertj.AssertJActionResponseAssert.assertThat;
import static org.forgerock.json.resource.test.assertj.AssertJResourceResponseAssert.assertThat;
import static org.forgerock.openam.core.rest.session.SessionResourceUtil.*;
import static org.forgerock.openam.core.rest.session.SessionResourceV2.REFRESH_ACTION_ID;
import static org.forgerock.openam.session.SessionConstants.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import com.iplanet.dpro.session.Session;
import com.iplanet.dpro.session.SessionException;
import com.iplanet.dpro.session.service.SessionService;
import com.iplanet.dpro.session.share.SessionBundle;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;
import org.forgerock.json.JsonPointer;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.ForbiddenException;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.NotSupportedException;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.json.resource.test.assertj.AssertJQueryResponseAssert;
import org.forgerock.openam.authentication.service.AuthUtilsWrapper;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.core.realms.RealmTestHelper;
import org.forgerock.openam.dpro.session.PartialSession.Builder;
import org.forgerock.openam.dpro.session.PartialSessionFactory;
import org.forgerock.openam.rest.RealmContext;
import org.forgerock.openam.rest.resource.SSOTokenContext;
import org.forgerock.openam.session.SessionPropertyWhitelist;
import org.forgerock.openam.test.apidescriptor.ApiAnnotationAssert;
import org.forgerock.openam.utils.CrestQuery;
import org.forgerock.opendj.ldap.DN;
import org.forgerock.services.context.Context;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.query.QueryFilter;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class SessionResourceV2Test {

    private static final String REALM_PATH = "/example/com";
    private static final JsonPointer REALM_FIELD = new JsonPointer("realm");

    private SSOTokenContext mockContext = mock(SSOTokenContext.class);

    private SSOToken ssoToken = mock(SSOToken.class);

    private AMIdentity amIdentity;
    private AuthUtilsWrapper authUtilsWrapper;
    private SSOTokenManager ssoTokenManager;
    private SessionResourceUtil sessionResourceUtil;
    private SessionPropertyWhitelist sessionPropertyWhitelist;
    private SessionService sessionService;
    private PartialSessionFactory partialSessionFactory;
    private RealmTestHelper realmTestHelper;

    private SessionResourceV2 sessionResource;

    @BeforeMethod
    public void setUp() throws Exception {

        ssoTokenManager = mock(SSOTokenManager.class);
        authUtilsWrapper = mock(AuthUtilsWrapper.class);
        sessionPropertyWhitelist = mock(SessionPropertyWhitelist.class);

        realmTestHelper = new RealmTestHelper();
        realmTestHelper.setupRealmClass();

        amIdentity = new AMIdentity(DN.valueOf("id=demo,dc=example,dc=com"), null);

        sessionService = mock(SessionService.class);
        partialSessionFactory = mock(PartialSessionFactory.class);

        sessionResourceUtil = new SessionResourceUtil(ssoTokenManager, null) {
            @Override
            public AMIdentity getIdentity(SSOToken ssoToken) throws IdRepoException, SSOException {
                return amIdentity;
            }

            @Override
            public String convertDNToRealm(String dn) {
                return REALM_PATH;
            }
        };
        sessionResource = new SessionResourceV2(ssoTokenManager, authUtilsWrapper,
                sessionResourceUtil, sessionPropertyWhitelist, sessionService, partialSessionFactory);
        given(mockContext.getCallerSSOToken()).willReturn(ssoToken);
    }

    @AfterMethod
    public void tearDown() {
        realmTestHelper.tearDownRealmClass();
    }

    @Test
    public void testActionInstanceIsUnsupported() {
        //given
        ActionRequest request = mock(ActionRequest.class);

        //when
        Promise<ActionResponse, ResourceException> result = sessionResource.actionInstance(mockContext, "resource",
                request);

        //then
        assertThat(result).failedWithException().isInstanceOf(NotSupportedException.class);
    }

    @Test
    public void testCreateInstanceIsUnsupported() {
        //given
        CreateRequest request = mock(CreateRequest.class);

        //when
        Promise<ResourceResponse, ResourceException> result = sessionResource.createInstance(mockContext, request);


        //then
        assertThat(result).failedWithException().isInstanceOf(NotSupportedException.class);
    }

    @Test
    public void testDeleteInstanceIsUnsupported() {
        //given
        DeleteRequest request = mock(DeleteRequest.class);

        //when
        Promise<ResourceResponse, ResourceException> result =
                sessionResource.deleteInstance(mockContext, "resId", request);

        //then
        assertThat(result).failedWithException().isInstanceOf(NotSupportedException.class);
    }

    @Test
    public void testPatchInstanceIsUnsupported() {
        //given
        PatchRequest request = mock(PatchRequest.class);

        //when
        Promise<ResourceResponse, ResourceException> result =
                sessionResource.patchInstance(mockContext, "resId", request);

        //then
        assertThat(result).failedWithException().isInstanceOf(NotSupportedException.class);
    }

    @Test
    public void testUpdateInstanceIsUnsupported() {
        //given
        UpdateRequest request = mock(UpdateRequest.class);

        //when
        Promise<ResourceResponse, ResourceException> result =
                sessionResource.updateInstance(mockContext, "resId", request);

        //then
        assertThat(result).failedWithException().isInstanceOf(NotSupportedException.class);
    }

    @Test
    public void actionInstanceShouldReturnFalseWhenTokenUnknown() throws SSOException {
        //Given
        ActionRequest request = mock(ActionRequest.class);
        given(ssoTokenManager.createSSOToken("unknown")).willThrow(SSOException.class);
        given(request.getAction()).willReturn(REFRESH_ACTION_ID);
        given(request.getAdditionalParameter("tokenId")).willReturn("unknown");


        //When
        Promise<ActionResponse, ResourceException> promise =
                sessionResource.actionCollection(mockContext, request);

        //Then
        assertThat(promise).succeeded().withContent().booleanAt("valid").isFalse();
    }

    @Test
    public void refreshActionShouldReturnIdleTimeToZero() throws SSOException {
        //Given
        ActionRequest request = mock(ActionRequest.class);
        given(ssoTokenManager.createSSOToken("tokenId")).willReturn(ssoToken);
        given(ssoToken.getIdleTime()).willReturn(0L);
        given(request.getAction()).willReturn(REFRESH_ACTION_ID);
        given(request.getAdditionalParameter("tokenId")).willReturn("tokenId");

        //When
        Promise<ActionResponse, ResourceException> promise =
                sessionResource.actionCollection(mockContext, request);

        //Then
        assertThat(promise).succeeded().withContent().longAt(IDLE_TIME).isEqualTo(0);
    }

    @Test
    public void readShouldReturnSessionInfoForValidToken() throws SSOException {
        //Given
        ActionRequest request = mock(ActionRequest.class);
        given(ssoTokenManager.retrieveValidTokenWithoutResettingIdleTime("tokenId")).willReturn(ssoToken);
        given(request.getAction()).willReturn("getSessionInfo");
        given(request.getAdditionalParameter("tokenId")).willReturn("tokenId");
        given(ssoTokenManager.isValidToken(ssoToken, false)).willReturn(true);
        given(partialSessionFactory.fromSSOToken(eq(ssoToken))).willReturn(
                new Builder()
                        .username("demo")
                        .universalId("universalId")
                        .realm(REALM_PATH)
                        .sessionHandle("shandle:badger")
                        .latestAccessTime("JUST_NOW")
                        .maxIdleExpirationTime("CLOSE")
                        .maxSessionExpirationTime("FAR")
                        .build());

        //When
        Promise<ActionResponse, ResourceException> promise =
                sessionResource.actionCollection(mockContext, request);

        //Then
        assertThat(promise).succeeded().withContent().stringAt(JSON_SESSION_USERNAME).isEqualTo("demo");
        assertThat(promise).succeeded().withContent().stringAt(JSON_SESSION_UNIVERSAL_ID).isEqualTo("universalId");
        assertThat(promise).succeeded().withContent().stringAt(JSON_SESSION_REALM).isEqualTo(REALM_PATH);
        assertThat(promise).succeeded().withContent().stringAt(JSON_SESSION_HANDLE).isEqualTo("shandle:badger");
        assertThat(promise).succeeded().withContent().stringAt(JSON_SESSION_LATEST_ACCESS_TIME).isEqualTo("JUST_NOW");
        assertThat(promise).succeeded().withContent().stringAt(JSON_SESSION_MAX_IDLE_EXPIRATION_TIME)
                .isEqualTo("CLOSE");
        assertThat(promise).succeeded().withContent().stringAt(JSON_SESSION_MAX_SESSION_EXPIRATION_TIME)
                .isEqualTo("FAR");
    }

    @Test
    public void getSessionPropertiesActionShouldReturnSessionProperties() throws Exception {
        //Given
        ActionRequest request = mock(ActionRequest.class);
        given(ssoTokenManager.retrieveValidTokenWithoutResettingIdleTime("tokenId")).willReturn(ssoToken);
        given(request.getAction()).willReturn("getSessionProperties");
        given(request.getAdditionalParameter("tokenId")).willReturn("tokenId");
        given(ssoTokenManager.isValidToken(ssoToken, false)).willReturn(true);
        setUpSessionProperties();

        //When
        Promise<ActionResponse, ResourceException> promise =
                sessionResource.actionCollection(mockContext, request);

        //Then
        assertThat(promise).succeeded().withContent().stringAt("foo").isEqualTo("bar");
        assertThat(promise).succeeded().withContent().stringAt("ping").isEqualTo("pong");
        assertThat(promise).succeeded().withContent().stringAt("woo").isEqualTo("");
    }

    @Test
    public void whenNullContentShouldReturnBadRequest() throws Exception {
        //given
        ActionRequest request = mock(ActionRequest.class);
        given(ssoTokenManager.retrieveValidTokenWithoutResettingIdleTime("tokenId")).willReturn(ssoToken);
        given(request.getAction()).willReturn("updateSessionProperties");
        given(request.getAdditionalParameter("tokenId")).willReturn("tokenId");
        given(ssoTokenManager.isValidToken(ssoToken, false)).willReturn(true);
        setUpSessionProperties();

        //when
        Promise<ActionResponse, ResourceException> promise = sessionResource.actionCollection(mockContext, request);

        //then
        assertThat(promise).failedWithException().isInstanceOf(BadRequestException.class);
    }

    @Test
    public void whenPropertyNotListedShouldReturnForbidden() throws Exception {
        //given
        ActionRequest request = mock(ActionRequest.class);
        given(ssoTokenManager.retrieveValidTokenWithoutResettingIdleTime("tokenId")).willReturn(ssoToken);
        given(request.getAction()).willReturn("updateSessionProperties");
        given(request.getAdditionalParameter("tokenId")).willReturn("tokenId");
        given(ssoTokenManager.isValidToken(ssoToken, false)).willReturn(true);
        Map<String, String> properties = setUpSessionProperties();
        given(request.getContent()).willReturn(json(properties));
        given(sessionPropertyWhitelist.isPropertyListed(ssoToken, REALM_PATH, properties.keySet())).willReturn(false);

        //when
        Promise<ActionResponse, ResourceException> promise = sessionResource.actionCollection(mockContext, request);

        //then
        assertThat(promise).failedWithException().isInstanceOf(ForbiddenException.class);
    }

    @Test
    public void whenPropertyNotSettableShouldReturnForbidden() throws Exception {
        //given
        ActionRequest request = mock(ActionRequest.class);
        given(ssoTokenManager.retrieveValidTokenWithoutResettingIdleTime("tokenId")).willReturn(ssoToken);
        given(request.getAction()).willReturn("updateSessionProperties");
        given(request.getAdditionalParameter("tokenId")).willReturn("tokenId");
        given(ssoTokenManager.isValidToken(ssoToken, false)).willReturn(true);
        Map<String, String> properties = setUpSessionProperties();
        given(request.getContent()).willReturn(json(properties));
        given(sessionPropertyWhitelist.isPropertyListed(ssoToken, REALM_PATH, properties.keySet())).willReturn(true);
        given(sessionPropertyWhitelist.isPropertyMapSettable(ssoToken, properties)).willReturn(false);

        //when
        Promise<ActionResponse, ResourceException> promise = sessionResource.actionCollection(mockContext, request);

        //then
        assertThat(promise).failedWithException().isInstanceOf(ForbiddenException.class);
    }

    @Test
    public void whenUpdatedPermittedPropertiesShouldGetUpdated() throws Exception {
        //given
        ActionRequest request = mock(ActionRequest.class);
        given(ssoTokenManager.retrieveValidTokenWithoutResettingIdleTime("tokenId")).willReturn(ssoToken);
        given(request.getAction()).willReturn("updateSessionProperties");
        given(request.getAdditionalParameter("tokenId")).willReturn("tokenId");
        given(ssoTokenManager.isValidToken(ssoToken, false)).willReturn(true);
        Map<String, String> properties = setUpSessionProperties();
        Map<String, String> updatedProperties = new HashMap<>();
        updatedProperties.put("foo", "baar");
        updatedProperties.put("ping", "poong");
        updatedProperties.put("woo", "hoo");
        given(request.getContent()).willReturn(json(updatedProperties));
        given(sessionPropertyWhitelist.isPropertyMapSettable(ssoToken, updatedProperties)).willReturn(true);

        //when
        sessionResource.actionCollection(mockContext, request);

        //then
        verify(ssoToken).setProperty("foo", "baar");
        verify(ssoToken).setProperty("ping", "poong");
        verify(ssoToken).setProperty("woo", "hoo");
    }

    @Test
    public void readShouldReturnFalseWhenTokenUnknown() throws SSOException {
        //Given
        ActionRequest request = mock(ActionRequest.class);
        given(ssoTokenManager.retrieveValidTokenWithoutResettingIdleTime("unknown")).willThrow(SSOException.class);
        given(request.getAction()).willReturn("getSessionInfo");
        given(request.getAdditionalParameter("tokenId")).willReturn("unknown");

        //When
        Promise<ActionResponse, ResourceException> promise =
                sessionResource.actionCollection(mockContext, request);

        //Then
        assertThat(promise).succeeded().withContent().booleanAt("valid").isFalse();
    }

    @Test
    public void queryShouldRefuseAFilterRealmOutsideTheRealmOfTheRequest() throws Exception {
        //given
        Context context = new RealmContext(mockContext, realmTestHelper.mockRealm("realmA"));

        //when
        Promise<QueryResponse, ResourceException> promise = queryRealm(context, "/realmB");

        //then
        AssertJQueryResponseAssert.assertThat(promise).failedWithException()
                .isInstanceOf(ForbiddenException.class);
        verify(sessionService, never()).getMatchingSessions(any(Session.class), any(CrestQuery.class));
    }

    @Test
    public void queryShouldRefuseTheTopLevelRealmWhenTheRequestIsMadeAgainstASubRealm() throws Exception {
        //given
        Context context = new RealmContext(mockContext, realmTestHelper.mockRealm("realmA"));

        //when
        Promise<QueryResponse, ResourceException> promise = queryRealm(context, "/");

        //then
        AssertJQueryResponseAssert.assertThat(promise).failedWithException()
                .isInstanceOf(ForbiddenException.class);
        verify(sessionService, never()).getMatchingSessions(any(Session.class), any(CrestQuery.class));
    }

    @Test
    public void queryShouldAcceptTheRealmOfTheRequest() throws Exception {
        //given
        Context context = new RealmContext(mockContext, realmTestHelper.mockRealm("realmA"));
        Session callerSession = givenACaller();
        givenTheSessionServiceReturnsASession();
        QueryResourceHandler handler = mock(QueryResourceHandler.class);

        //when
        Promise<QueryResponse, ResourceException> promise = queryRealm(context, "/realmA", handler);

        //then
        AssertJQueryResponseAssert.assertThat(promise).succeeded();
        verify(handler).handleResource(any(ResourceResponse.class));
        verify(sessionService).getMatchingSessions(eq(callerSession), any(CrestQuery.class));
    }

    @Test
    public void queryShouldAcceptASubRealmOfTheRealmOfTheRequest() throws Exception {
        //given the console queries a realm through the root realm endpoint
        Context context = new RealmContext(mockContext, Realm.root());
        Session callerSession = givenACaller();
        givenTheSessionServiceReturnsASession();

        //when
        Promise<QueryResponse, ResourceException> promise = queryRealm(context, "/realmA");

        //then
        AssertJQueryResponseAssert.assertThat(promise).succeeded();
        verify(sessionService).getMatchingSessions(eq(callerSession), any(CrestQuery.class));
    }

    @Test
    public void queryShouldRefuseAFilterWithoutARealm() throws Exception {
        //given
        Context context = new RealmContext(mockContext, Realm.root());
        QueryRequest request = mock(QueryRequest.class);
        given(request.getQueryFilter()).willReturn(QueryFilter.equalTo(new JsonPointer("username"), "demo"));

        //when
        Promise<QueryResponse, ResourceException> promise =
                sessionResource.queryCollection(context, request, mock(QueryResourceHandler.class));

        //then
        AssertJQueryResponseAssert.assertThat(promise).failedWithException()
                .isInstanceOf(BadRequestException.class);
        verify(sessionService, never()).getMatchingSessions(any(Session.class), any(CrestQuery.class));
    }

    @Test
    public void queryShouldRefuseAFilterNamingMoreThanOneRealm() throws Exception {
        //given
        Context context = new RealmContext(mockContext, Realm.root());
        QueryRequest request = mock(QueryRequest.class);
        given(request.getQueryFilter()).willReturn(QueryFilter.and(
                QueryFilter.equalTo(REALM_FIELD, "/realmA"),
                QueryFilter.equalTo(REALM_FIELD, "/realmB")));

        //when
        Promise<QueryResponse, ResourceException> promise =
                sessionResource.queryCollection(context, request, mock(QueryResourceHandler.class));

        //then
        AssertJQueryResponseAssert.assertThat(promise).failedWithException()
                .isInstanceOf(BadRequestException.class);
        verify(sessionService, never()).getMatchingSessions(any(Session.class), any(CrestQuery.class));
    }

    @Test
    public void queryShouldRefuseAFilterRealmWithAParentPathSegment() throws Exception {
        //given a realm which would name another realm once the '..' segment is resolved
        Context context = new RealmContext(mockContext, realmTestHelper.mockRealm("realmA"));

        //when
        Promise<QueryResponse, ResourceException> promise = queryRealm(context, "/realmA/../realmB");

        //then
        AssertJQueryResponseAssert.assertThat(promise).failedWithException()
                .isInstanceOf(BadRequestException.class);
        verify(sessionService, never()).getMatchingSessions(any(Session.class), any(CrestQuery.class));
    }

    @Test
    public void queryShouldRefuseAFilterTypeTheSessionQueryDoesNotSupport() throws Exception {
        //given a filter the session query cannot be built from
        Context context = new RealmContext(mockContext, Realm.root());
        QueryRequest request = mock(QueryRequest.class);
        given(request.getQueryFilter()).willReturn(QueryFilter.or(
                QueryFilter.equalTo(REALM_FIELD, "/realmA"),
                QueryFilter.equalTo(REALM_FIELD, "/realmB")));

        //when
        Promise<QueryResponse, ResourceException> promise =
                sessionResource.queryCollection(context, request, mock(QueryResourceHandler.class));

        //then
        AssertJQueryResponseAssert.assertThat(promise).failedWithException()
                .isInstanceOf(BadRequestException.class);
        verify(sessionService, never()).getMatchingSessions(any(Session.class), any(CrestQuery.class));
    }

    @Test
    public void queryShouldRefuseARequestWithoutARealmInItsContext() throws Exception {
        //given a context the realm of the request cannot be read from
        QueryRequest request = mock(QueryRequest.class);
        given(request.getQueryFilter()).willReturn(QueryFilter.equalTo(REALM_FIELD, "/realmA"));

        //when
        Promise<QueryResponse, ResourceException> promise =
                sessionResource.queryCollection(mockContext, request, mock(QueryResourceHandler.class));

        //then the request is refused rather than answered as if it were made against the top level realm
        AssertJQueryResponseAssert.assertThat(promise).failedWithException()
                .isInstanceOf(InternalServerErrorException.class);
        verify(sessionService, never()).getMatchingSessions(any(Session.class), any(CrestQuery.class));
    }

    @Test
    public void queryShouldReportTheSessionServiceRefusalAsForbidden() throws Exception {
        //given the session service checks the realm against the caller as well
        Context context = new RealmContext(mockContext, realmTestHelper.mockRealm("realmA"));
        givenACaller();
        given(sessionService.getMatchingSessions(any(Session.class), any(CrestQuery.class)))
                .willThrow(new SessionException(SessionBundle.rbName, NO_PRIVILEGE_ERROR_CODE, null));

        //when
        Promise<QueryResponse, ResourceException> promise = queryRealm(context, "/realmA");

        //then
        AssertJQueryResponseAssert.assertThat(promise).failedWithException()
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    public void queryShouldReportAnyOtherSessionFailureAsAServerError() throws Exception {
        //given
        Context context = new RealmContext(mockContext, realmTestHelper.mockRealm("realmA"));
        givenACaller();
        given(sessionService.getMatchingSessions(any(Session.class), any(CrestQuery.class)))
                .willThrow(new SessionException("the CTS is unavailable"));

        //when
        Promise<QueryResponse, ResourceException> promise = queryRealm(context, "/realmA");

        //then
        AssertJQueryResponseAssert.assertThat(promise).failedWithException()
                .isInstanceOf(InternalServerErrorException.class);
    }

    @Test
    public void shouldFailIfAnnotationsAreNotValid() {
        ApiAnnotationAssert.assertThat(SessionResourceV2.class).hasValidAnnotations();
    }

    private Session givenACaller() {
        Session callerSession = mock(Session.class);
        given(mockContext.getCallerSession()).willReturn(callerSession);
        return callerSession;
    }

    private void givenTheSessionServiceReturnsASession() throws Exception {
        given(sessionService.getMatchingSessions(any(Session.class), any(CrestQuery.class))).willReturn(
                Arrays.asList(new Builder().username("demo").realm("/realmA").build()));
    }

    private Promise<QueryResponse, ResourceException> queryRealm(Context context, String realm) {
        return queryRealm(context, realm, mock(QueryResourceHandler.class));
    }

    private Promise<QueryResponse, ResourceException> queryRealm(Context context, String realm,
            QueryResourceHandler handler) {
        QueryRequest request = mock(QueryRequest.class);
        given(request.getQueryFilter()).willReturn(QueryFilter.equalTo(REALM_FIELD, realm));
        return sessionResource.queryCollection(context, request, handler);
    }

    private Map<String, String> setUpSessionProperties() throws SSOException {
        Map<String, String> properties = new HashMap<>();
        properties.put("foo", "bar");
        properties.put("ping", "pong");
        properties.put("woo", null);
        for (String key : properties.keySet()) {
            given(ssoToken.getProperty(key)).willReturn(properties.get(key));
        }
        given(sessionPropertyWhitelist.getAllListedProperties(REALM_PATH)).willReturn(properties.keySet());
        return properties;
    }
}
