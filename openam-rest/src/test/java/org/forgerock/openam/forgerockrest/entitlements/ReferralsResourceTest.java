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
package org.forgerock.openam.forgerockrest.entitlements;

import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.ReferralPrivilege;
import com.sun.identity.entitlement.ReferralPrivilegeManager;
import com.sun.identity.shared.debug.Debug;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import javax.security.auth.Subject;
import static org.fest.assertions.Assertions.assertThat;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResultHandler;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openam.forgerockrest.entitlements.wrappers.ReferralWrapper;
import org.forgerock.openam.rest.resource.RealmContext;
import org.forgerock.openam.rest.resource.SSOTokenContext;
import org.mockito.ArgumentCaptor;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anySet;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertTrue;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class ReferralsResourceTest {

    private ReferralsResourceV1 referralsResource;

    private Debug debug;
    private ReferralPrivilegeManager privilegeManager;
    private ReferralWrapper referralWrapper;

    @BeforeMethod
    public void theSetUp() { //you need this

        debug = mock(Debug.class);
        referralWrapper = mock(ReferralWrapper.class);
        privilegeManager = mock(ReferralPrivilegeManager.class);
        referralsResource = new ReferralsResourceV1(debug) {

            @Override
            protected ReferralWrapper createReferralWrapper(JsonValue jsonValue) throws IOException {
                return referralWrapper;
            }

            @Override
            protected ReferralPrivilegeManager createPrivilegeManager(String realm, Subject callingSubject) {
                return privilegeManager;
            }

            @Override
            boolean isRequestRealmsValidPeerOrSubrealms(ServerContext serverContext, String realm,
                                                        Set<String> realms) {
                return true;
            }
        };

    }

    @Test
    public void shouldBadRequestIfSubjectNullOnCreate() {

        //given
        SSOTokenContext mockSSOTokenContext = mock(SSOTokenContext.class);
        RealmContext realmContext = new RealmContext(mockSSOTokenContext);
        ServerContext mockServerContext = new ServerContext(realmContext);
        CreateRequest mockCreateRequest = mock(CreateRequest.class);
        ResultHandler mockResultHandler = mock(ResultHandler.class);

        given(mockSSOTokenContext.getCallerSubject()).willReturn(null);

        //when
        referralsResource.createInstance(mockServerContext, mockCreateRequest, mockResultHandler);

        //then
        ArgumentCaptor<ResourceException> captor = ArgumentCaptor.forClass(ResourceException.class);
        verify(mockResultHandler, times(1)).handleError(captor.capture());
        assertThat(captor.getValue().getCode()).isEqualTo(ResourceException.BAD_REQUEST);
    }

    @Test
    public void shouldBadRequestIfJsonUnreadable() {
        //given
        SSOTokenContext mockSSOTokenContext = mock(SSOTokenContext.class);
        RealmContext realmContext = new RealmContext(mockSSOTokenContext);
        ServerContext mockServerContext = new ServerContext(realmContext);
        CreateRequest mockCreateRequest = mock(CreateRequest.class);
        ResultHandler mockResultHandler = mock(ResultHandler.class);
        Subject subject = new Subject();

        referralsResource = new ReferralsResourceV1(debug) {

            @Override
            protected ReferralWrapper createReferralWrapper(JsonValue jsonValue) throws IOException {
                throw new IOException();
            }
        };

        given(mockSSOTokenContext.getCallerSubject()).willReturn(subject);

        //when
        referralsResource.createInstance(mockServerContext, mockCreateRequest, mockResultHandler);

        //then
        ArgumentCaptor<ResourceException> captor = ArgumentCaptor.forClass(ResourceException.class);
        verify(mockResultHandler, times(1)).handleError(captor.capture());
        assertThat(captor.getValue().getCode()).isEqualTo(ResourceException.BAD_REQUEST);
    }

    @Test
    public void shouldConflictIfResourceExists() throws EntitlementException {
        //given
        SSOTokenContext mockSSOTokenContext = mock(SSOTokenContext.class);
        RealmContext realmContext = new RealmContext(mockSSOTokenContext);
        ServerContext mockServerContext = new ServerContext(realmContext);
        CreateRequest mockCreateRequest = mock(CreateRequest.class);
        ResultHandler mockResultHandler = mock(ResultHandler.class);
        Subject subject = new Subject();

        given(mockSSOTokenContext.getCallerSubject()).willReturn(subject);
        given(privilegeManager.canFindByName(anyString())).willReturn(true);
        given(referralWrapper.getName()).willReturn("referralName");

        //when
        referralsResource.createInstance(mockServerContext, mockCreateRequest, mockResultHandler);

        //then
        ArgumentCaptor<ResourceException> captor = ArgumentCaptor.forClass(ResourceException.class);
        verify(mockResultHandler, times(1)).handleError(captor.capture());
        assertThat(captor.getValue().getCode()).isEqualTo(ResourceException.CONFLICT);
    }

    @Test
    public void shouldErrorIfCannotCheckThatResourceExists() throws EntitlementException {
        //given
        SSOTokenContext mockSSOTokenContext = mock(SSOTokenContext.class);
        RealmContext realmContext = new RealmContext(mockSSOTokenContext);
        ServerContext mockServerContext = new ServerContext(realmContext);
        CreateRequest mockCreateRequest = mock(CreateRequest.class);
        ResultHandler mockResultHandler = mock(ResultHandler.class);
        Subject subject = new Subject();

        given(mockSSOTokenContext.getCallerSubject()).willReturn(subject);
        given(privilegeManager.canFindByName(anyString())).willThrow(new EntitlementException(1));
        given(referralWrapper.getName()).willReturn("referralName");

        //when
        referralsResource.createInstance(mockServerContext, mockCreateRequest, mockResultHandler);

        //then
        ArgumentCaptor<ResourceException> captor = ArgumentCaptor.forClass(ResourceException.class);
        verify(mockResultHandler, times(1)).handleError(captor.capture());
        assertThat(captor.getValue().getCode()).isEqualTo(ResourceException.INTERNAL_ERROR);
    }

    @Test
    public void shouldBadRequestIfRealmNotAcceptable() throws EntitlementException, IOException {

        //given
        SSOTokenContext mockSSOTokenContext = mock(SSOTokenContext.class);
        RealmContext realmContext = new RealmContext(mockSSOTokenContext);
        ServerContext mockServerContext = new ServerContext(realmContext);
        CreateRequest mockCreateRequest = mock(CreateRequest.class);
        ResultHandler mockResultHandler = mock(ResultHandler.class);
        Subject subject = new Subject();

        referralsResource = new ReferralsResourceV1(debug) {

            @Override
            protected ReferralWrapper createReferralWrapper(JsonValue jsonValue) throws IOException {
                return referralWrapper;
            }

            @Override
            protected ReferralPrivilegeManager createPrivilegeManager(String realm, Subject callingSubject) {
                return privilegeManager;
            }

            @Override
            boolean isRequestRealmsValidPeerOrSubrealms(ServerContext serverContext, String realm,
                                                                Set<String> realms) {
                return false;
            }
        };

        given(mockSSOTokenContext.getCallerSubject()).willReturn(subject);
        given(privilegeManager.canFindByName(anyString())).willReturn(false);
        given(referralWrapper.getName()).willReturn("referralName");

        //when
        referralsResource.createInstance(mockServerContext, mockCreateRequest, mockResultHandler);

        //then
        ArgumentCaptor<ResourceException> captor = ArgumentCaptor.forClass(ResourceException.class);
        verify(mockResultHandler, times(1)).handleError(captor.capture());
        assertThat(captor.getValue().getCode()).isEqualTo(ResourceException.BAD_REQUEST);
    }

    @Test
    public void shouldBadRequestIfNameContainsInvalidCharacters() throws EntitlementException, IOException {

        //given
        SSOTokenContext mockSSOTokenContext = mock(SSOTokenContext.class);
        RealmContext realmContext = new RealmContext(mockSSOTokenContext);
        ServerContext mockServerContext = new ServerContext(realmContext);
        CreateRequest mockCreateRequest = mock(CreateRequest.class);
        ResultHandler mockResultHandler = mock(ResultHandler.class);
        Subject subject = new Subject();

        referralsResource = new ReferralsResourceV1(debug) {

            @Override
            protected ReferralWrapper createReferralWrapper(JsonValue jsonValue) throws IOException {
                return referralWrapper;
            }

            @Override
            protected ReferralPrivilegeManager createPrivilegeManager(String realm, Subject callingSubject) {
                return privilegeManager;
            }

            @Override
            boolean isRequestRealmsValidPeerOrSubrealms(ServerContext serverContext, String realm,
                                                        Set<String> realms) {
                return false;
            }
        };

        given(mockSSOTokenContext.getCallerSubject()).willReturn(subject);
        given(privilegeManager.canFindByName(anyString())).willReturn(false);
        given(referralWrapper.getName()).willReturn("referral+name");

        //when
        referralsResource.createInstance(mockServerContext, mockCreateRequest, mockResultHandler);

        //then
        ArgumentCaptor<ResourceException> captor = ArgumentCaptor.forClass(ResourceException.class);
        verify(mockResultHandler, times(1)).handleError(captor.capture());
        assertThat(captor.getValue().getCode()).isEqualTo(ResourceException.BAD_REQUEST);
    }


    @Test
    public void shouldInternalErrorIfCannotSaveReferral() throws EntitlementException {

        //given
        SSOTokenContext mockSSOTokenContext = mock(SSOTokenContext.class);
        RealmContext realmContext = new RealmContext(mockSSOTokenContext);
        ServerContext mockServerContext = new ServerContext(realmContext);
        CreateRequest mockCreateRequest = mock(CreateRequest.class);
        ResultHandler mockResultHandler = mock(ResultHandler.class);
        Subject subject = new Subject();

        given(mockSSOTokenContext.getCallerSubject()).willReturn(subject);
        given(privilegeManager.canFindByName(anyString())).willReturn(false);
        doThrow(new EntitlementException(1)).when(privilegeManager).add(any(ReferralPrivilege.class));

        //when
        referralsResource.createInstance(mockServerContext, mockCreateRequest, mockResultHandler);

        //then
        ArgumentCaptor<ResourceException> captor = ArgumentCaptor.forClass(ResourceException.class);
        verify(mockResultHandler, times(1)).handleError(captor.capture());
        assertThat(captor.getValue().getCode()).isEqualTo(ResourceException.BAD_REQUEST);
    }

    @Test
    public void shouldNotFoundDeleteResourceThatDoesntExist() throws EntitlementException {

        //given
        SSOTokenContext mockSSOTokenContext = mock(SSOTokenContext.class);
        RealmContext realmContext = new RealmContext(mockSSOTokenContext);
        ServerContext mockServerContext = new ServerContext(realmContext);
        DeleteRequest mockDeleteRequest = mock(DeleteRequest.class);
        ResultHandler mockResultHandler = mock(ResultHandler.class);
        Subject subject = new Subject();

        given(mockSSOTokenContext.getCallerSubject()).willReturn(subject);
        given(privilegeManager.canFindByName(anyString())).willReturn(false);

        //when
        referralsResource.deleteInstance(mockServerContext, "ID", mockDeleteRequest, mockResultHandler);

        //then
        ArgumentCaptor<ResourceException> captor = ArgumentCaptor.forClass(ResourceException.class);
        verify(mockResultHandler, times(1)).handleError(captor.capture());
        assertThat(captor.getValue().getCode()).isEqualTo(ResourceException.NOT_FOUND);
    }

    @Test
    public void shouldInternalErrorDeleteResourceThatWontDelete() throws EntitlementException {

        //given
        SSOTokenContext mockSSOTokenContext = mock(SSOTokenContext.class);
        RealmContext realmContext = new RealmContext(mockSSOTokenContext);
        ServerContext mockServerContext = new ServerContext(realmContext);
        DeleteRequest mockDeleteRequest = mock(DeleteRequest.class);
        ResultHandler mockResultHandler = mock(ResultHandler.class);
        Subject subject = new Subject();

        given(mockSSOTokenContext.getCallerSubject()).willReturn(subject);
        given(privilegeManager.canFindByName(anyString())).willReturn(true);
        doThrow(new EntitlementException(1)).when(privilegeManager).remove(anyString());

        //when
        referralsResource.deleteInstance(mockServerContext, "ID", mockDeleteRequest, mockResultHandler);

        //then
        ArgumentCaptor<ResourceException> captor = ArgumentCaptor.forClass(ResourceException.class);
        verify(mockResultHandler, times(1)).handleError(captor.capture());
        assertThat(captor.getValue().getCode()).isEqualTo(ResourceException.INTERNAL_ERROR);
    }

    @Test
    public void shouldInternalErrorWhenCannotQueryDataset() throws EntitlementException {

        //given
        SSOTokenContext mockSSOTokenContext = mock(SSOTokenContext.class);
        RealmContext realmContext = new RealmContext(mockSSOTokenContext);
        ServerContext mockServerContext = new ServerContext(realmContext);
        QueryRequest mockQueryRequest = mock(QueryRequest.class);
        QueryResultHandler mockQueryResultHandler = mock(QueryResultHandler.class);
        Subject subject = new Subject();

        given(mockSSOTokenContext.getCallerSubject()).willReturn(subject);
        given(privilegeManager.searchNames(anySet())).willThrow(new EntitlementException(1));

        //when
        referralsResource.queryCollection(mockServerContext, mockQueryRequest, mockQueryResultHandler);

        //then
        ArgumentCaptor<ResourceException> captor = ArgumentCaptor.forClass(ResourceException.class);
        verify(mockQueryResultHandler, times(1)).handleError(captor.capture());
        assertThat(captor.getValue().getCode()).isEqualTo(ResourceException.INTERNAL_ERROR);
    }

    @Test
    public void shouldPerformQuery() throws EntitlementException {

        //given
        SSOTokenContext mockSSOTokenContext = mock(SSOTokenContext.class);
        RealmContext realmContext = new RealmContext(mockSSOTokenContext);
        ServerContext mockServerContext = new ServerContext(realmContext);
        QueryRequest mockQueryRequest = mock(QueryRequest.class);
        QueryResultHandler mockQueryResultHandler = mock(QueryResultHandler.class);
        Subject subject = new Subject();

        Set<String> nameSet = new HashSet<String>();
        nameSet.add("one");
        nameSet.add("two");

        ReferralPrivilege mockOne = new ReferralPrivilege("one",
                Collections.<String, Set<String>>emptyMap(), Collections.<String>emptySet());
        ReferralPrivilege mockTwo = new ReferralPrivilege("two",
                Collections.<String, Set<String>>emptyMap(), Collections.<String>emptySet());

        given(mockSSOTokenContext.getCallerSubject()).willReturn(subject);
        given(privilegeManager.searchNames(anySet())).willReturn(nameSet);
        given(privilegeManager.findByName("one")).willReturn(mockOne);
        given(privilegeManager.findByName("two")).willReturn(mockTwo);
        given(mockQueryResultHandler.handleResource(any(Resource.class))).willReturn(true);

        //when
        referralsResource.queryCollection(mockServerContext, mockQueryRequest, mockQueryResultHandler);

        //then
        verify(mockQueryResultHandler, times(2)).handleResource(any(Resource.class));
    }

    @Test
    public void shouldPerformCreate() throws EntitlementException, IOException {

        //given
        SSOTokenContext mockSSOTokenContext = mock(SSOTokenContext.class);
        RealmContext realmContext = new RealmContext(mockSSOTokenContext);
        ServerContext mockServerContext = new ServerContext(realmContext);
        CreateRequest mockCreateRequest = mock(CreateRequest.class);
        ResultHandler mockResultHandler = mock(ResultHandler.class);
        Subject subject = new Subject();
        JsonValue mockJson = mock(JsonValue.class);

        given(mockSSOTokenContext.getCallerSubject()).willReturn(subject);
        given(privilegeManager.canFindByName(anyString())).willReturn(false);
        given(referralWrapper.getName()).willReturn("NAME");
        given(referralWrapper.getLastModifiedDate()).willReturn(1000l);
        given(referralWrapper.toJsonValue()).willReturn(mockJson);

        //when
        referralsResource.createInstance(mockServerContext, mockCreateRequest, mockResultHandler);

        //then
        verify(mockResultHandler, times(1)).handleResult(any(Resource.class));
    }

    @Test
    public void shouldPerformDelete() throws EntitlementException, IOException {

        //given
        SSOTokenContext mockSSOTokenContext = mock(SSOTokenContext.class);
        RealmContext realmContext = new RealmContext(mockSSOTokenContext);
        ServerContext mockServerContext = new ServerContext(realmContext);
        DeleteRequest mockDeleteRequest = mock(DeleteRequest.class);
        ResultHandler mockResultHandler = mock(ResultHandler.class);
        Subject subject = new Subject();

        given(mockSSOTokenContext.getCallerSubject()).willReturn(subject);
        given(privilegeManager.canFindByName(anyString())).willReturn(true);

        //when
        referralsResource.deleteInstance(mockServerContext, "ID", mockDeleteRequest, mockResultHandler);

        //then
        verify(mockResultHandler, times(1)).handleResult(any(Resource.class));
    }

    @Test
    public void shouldNotFoundWhenReadingInvalidResource() throws EntitlementException {

        //given
        SSOTokenContext mockSSOTokenContext = mock(SSOTokenContext.class);
        RealmContext realmContext = new RealmContext(mockSSOTokenContext);
        ServerContext mockServerContext = new ServerContext(realmContext);
        ReadRequest mockReadRequest = mock(ReadRequest.class);
        ResultHandler mockResultHandler = mock(ResultHandler.class);
        Subject subject = new Subject();

        given(mockSSOTokenContext.getCallerSubject()).willReturn(subject);
        given(privilegeManager.findByName(anyString())).willThrow(new EntitlementException(1));

        //when
        referralsResource.readInstance(mockServerContext, "ID", mockReadRequest, mockResultHandler);

        //then
        ArgumentCaptor<ResourceException> captor = ArgumentCaptor.forClass(ResourceException.class);
        verify(mockResultHandler, times(1)).handleError(captor.capture());
        assertThat(captor.getValue().getCode()).isEqualTo(ResourceException.NOT_FOUND);
    }

    @Test
    public void shouldPerformRead() throws EntitlementException {
        //given
        SSOTokenContext mockSSOTokenContext = mock(SSOTokenContext.class);
        RealmContext realmContext = new RealmContext(mockSSOTokenContext);
        ServerContext mockServerContext = new ServerContext(realmContext);
        ReadRequest mockReadRequest = mock(ReadRequest.class);
        ResultHandler mockResultHandler = mock(ResultHandler.class);
        Subject subject = new Subject();
        ReferralPrivilege mockOne = new ReferralPrivilege("one",
                Collections.<String, Set<String>>emptyMap(), Collections.<String>emptySet());

        given(mockSSOTokenContext.getCallerSubject()).willReturn(subject);
        given(privilegeManager.findByName(anyString())).willReturn(mockOne);

        //when
        referralsResource.readInstance(mockServerContext, "one", mockReadRequest, mockResultHandler);

        //then
        verify(mockResultHandler, times(1)).handleResult(any(Resource.class));
    }

    @Test
    public void shouldBadRequestIfUpdateInvalidRequest() throws EntitlementException {

        //given
        SSOTokenContext mockSSOTokenContext = mock(SSOTokenContext.class);
        RealmContext realmContext = new RealmContext(mockSSOTokenContext);
        ServerContext mockServerContext = new ServerContext(realmContext);
        UpdateRequest mockUpdateRequest = mock(UpdateRequest.class);
        ResultHandler mockResultHandler = mock(ResultHandler.class);
        Subject subject = new Subject();

        referralsResource = new ReferralsResourceV1(debug) {

            @Override
            protected ReferralWrapper createReferralWrapper(JsonValue jsonValue) throws IOException {
                throw new IOException();
            }

            @Override
            protected ReferralPrivilegeManager createPrivilegeManager(String realm, Subject callingSubject) {
                return privilegeManager;
            }

            @Override
            boolean isRequestRealmsValidPeerOrSubrealms(ServerContext serverContext, String realm,
                                                        Set<String> realms) {
                return true;
            }
        };

        given(mockSSOTokenContext.getCallerSubject()).willReturn(subject);

        //when
        referralsResource.updateInstance(mockServerContext, "one", mockUpdateRequest, mockResultHandler);

        //then
        ArgumentCaptor<ResourceException> captor = ArgumentCaptor.forClass(ResourceException.class);
        verify(mockResultHandler, times(1)).handleError(captor.capture());
        assertThat(captor.getValue().getCode()).isEqualTo(ResourceException.BAD_REQUEST);
    }

    @Test
    public void shouldNotFoundIfNothingToUpdate() throws EntitlementException {

        //given
        SSOTokenContext mockSSOTokenContext = mock(SSOTokenContext.class);
        RealmContext realmContext = new RealmContext(mockSSOTokenContext);
        ServerContext mockServerContext = new ServerContext(realmContext);
        UpdateRequest mockUpdateRequest = mock(UpdateRequest.class);
        ResultHandler mockResultHandler = mock(ResultHandler.class);
        Subject subject = new Subject();

        given(mockSSOTokenContext.getCallerSubject()).willReturn(subject);
        given(privilegeManager.findByName(anyString())).willThrow(new EntitlementException(1));

        //when
        referralsResource.updateInstance(mockServerContext, "one", mockUpdateRequest, mockResultHandler);

        //then
        ArgumentCaptor<ResourceException> captor = ArgumentCaptor.forClass(ResourceException.class);
        verify(mockResultHandler, times(1)).handleError(captor.capture());
        assertThat(captor.getValue().getCode()).isEqualTo(ResourceException.NOT_FOUND);
    }

    @Test
    public void subRealmsAreCorrectlyDiscovered() throws EntitlementException {

        //given
        ReferralsResourceV1 resourceTest = new ReferralsResourceV1(debug);
        String realm = "/dance";
        Set<String> realms = new HashSet<String>();
        realms.add("/dance/with");
        realms.add("/dance/without");
        realms.add("/dance/with/me");
        Set<String> subRealms = new HashSet<String>();
        subRealms.add("with");
        subRealms.add("without");
        subRealms.add("with/me");
        Set<String> peerRealms = new HashSet<String>();

        //when
        boolean result = resourceTest.isRealmsValid(realm, realms,subRealms, peerRealms);

        //then
        assertTrue(result);
    }

    @Test
    public void invalidateSubRealmsAreCorrectlyDiscovered() throws EntitlementException {

        //given
        ReferralsResourceV1 resourceTest = new ReferralsResourceV1(debug);
        String realm = "/dance";
        Set<String> realms = new HashSet<String>();
        realms.add("/dance/with");
        realms.add("/dance/without");
        realms.add("/dance/with/me");
        Set<String> subRealms = new HashSet<String>();
        subRealms.add("along");
        subRealms.add("to");
        subRealms.add("the/tune");
        Set<String> peerRealms = new HashSet<String>();

        //when
        boolean result = resourceTest.isRealmsValid(realm, realms,subRealms, peerRealms);

        //then
        assertFalse(result);
    }

    @Test
    public void peerRealmsAreCorrectlyDiscovered() throws EntitlementException {
        //given
        ReferralsResourceV1 resourceTest = new ReferralsResourceV1(debug);
        String realm = "/dance";
        Set<String> realms = new HashSet<String>();
        realms.add("/with");
        realms.add("/me");
        Set<String> subRealms = new HashSet<String>();
        Set<String> peerRealms = new HashSet<String>();
        peerRealms.add("with");
        peerRealms.add("me");
        peerRealms.add("dance");

        //when
        boolean result = resourceTest.isRealmsValid(realm, realms,subRealms, peerRealms);

        //then
        assertTrue(result);
    }

    @Test
    public void invalidPeerRealmsAreCorrectlyDiscovered() throws EntitlementException {
        //given
        ReferralsResourceV1 resourceTest = new ReferralsResourceV1(debug);
        String realm = "/dance";
        Set<String> realms = new HashSet<String>();
        realms.add("/along");
        realms.add("/with");
        realms.add("/me");
        Set<String> subRealms = new HashSet<String>();
        Set<String> peerRealms = new HashSet<String>();
        peerRealms.add("with");
        peerRealms.add("me");
        peerRealms.add("dance");

        //when
        boolean result = resourceTest.isRealmsValid(realm, realms,subRealms, peerRealms);

        //then
        assertFalse(result);
    }

    @Test
    public void shouldBadRequestUpdateToInvalidRealms() throws EntitlementException {

        //given
        SSOTokenContext mockSSOTokenContext = mock(SSOTokenContext.class);
        RealmContext realmContext = new RealmContext(mockSSOTokenContext);
        ServerContext mockServerContext = new ServerContext(realmContext);
        UpdateRequest mockUpdateRequest = mock(UpdateRequest.class);
        ResultHandler mockResultHandler = mock(ResultHandler.class);
        Subject subject = new Subject();

        referralsResource = new ReferralsResourceV1(debug) {

            @Override
            protected ReferralWrapper createReferralWrapper(JsonValue jsonValue) throws IOException {
                return referralWrapper;
            }

            @Override
            protected ReferralPrivilegeManager createPrivilegeManager(String realm, Subject callingSubject) {
                return privilegeManager;
            }

            @Override
            boolean isRequestRealmsValidPeerOrSubrealms(ServerContext serverContext, String realm,
                                                        Set<String> realms) {
                return false;
            }
        };

        ReferralPrivilege mockOne = new ReferralPrivilege("one",
                Collections.<String, Set<String>>emptyMap(), Collections.<String>emptySet());

        given(mockSSOTokenContext.getCallerSubject()).willReturn(subject);
        given(privilegeManager.findByName(anyString())).willReturn(mockOne);

        //when
        referralsResource.updateInstance(mockServerContext, "one", mockUpdateRequest, mockResultHandler);

        //then
        ArgumentCaptor<ResourceException> captor = ArgumentCaptor.forClass(ResourceException.class);
        verify(mockResultHandler, times(1)).handleError(captor.capture());
        assertThat(captor.getValue().getCode()).isEqualTo(ResourceException.BAD_REQUEST);
    }

    @Test
    public void shouldConflictIfIntendedNamedReferralAlreadyExists() throws EntitlementException {

        //given
        SSOTokenContext mockSSOTokenContext = mock(SSOTokenContext.class);
        RealmContext realmContext = new RealmContext(mockSSOTokenContext);
        ServerContext mockServerContext = new ServerContext(realmContext);
        UpdateRequest mockUpdateRequest = mock(UpdateRequest.class);
        ResultHandler mockResultHandler = mock(ResultHandler.class);
        Subject subject = new Subject();

        ReferralPrivilege mockOne = new ReferralPrivilege("two",
                Collections.<String, Set<String>>emptyMap(), Collections.<String>emptySet());

        given(mockSSOTokenContext.getCallerSubject()).willReturn(subject);
        given(privilegeManager.findByName(anyString())).willReturn(mockOne);
        given(privilegeManager.canFindByName(anyString())).willReturn(true);

        //when
        referralsResource.updateInstance(mockServerContext, "one", mockUpdateRequest, mockResultHandler);

        //then
        ArgumentCaptor<ResourceException> captor = ArgumentCaptor.forClass(ResourceException.class);
        verify(mockResultHandler, times(1)).handleError(captor.capture());
        assertThat(captor.getValue().getCode()).isEqualTo(ResourceException.CONFLICT);
    }

    @Test
    public void shouldInternalErrorIfCannotCheckExistingResource() throws EntitlementException {

        //given
        SSOTokenContext mockSSOTokenContext = mock(SSOTokenContext.class);
        RealmContext realmContext = new RealmContext(mockSSOTokenContext);
        ServerContext mockServerContext = new ServerContext(realmContext);
        UpdateRequest mockUpdateRequest = mock(UpdateRequest.class);
        ResultHandler mockResultHandler = mock(ResultHandler.class);
        Subject subject = new Subject();

        ReferralPrivilege mockOne = new ReferralPrivilege("two",
                Collections.<String, Set<String>>emptyMap(), Collections.<String>emptySet());

        given(mockSSOTokenContext.getCallerSubject()).willReturn(subject);
        given(privilegeManager.findByName(anyString())).willReturn(mockOne);
        given(privilegeManager.canFindByName(anyString())).willThrow(new EntitlementException(1));

        //when
        referralsResource.updateInstance(mockServerContext, "one", mockUpdateRequest, mockResultHandler);

        //then
        ArgumentCaptor<ResourceException> captor = ArgumentCaptor.forClass(ResourceException.class);
        verify(mockResultHandler, times(1)).handleError(captor.capture());
        assertThat(captor.getValue().getCode()).isEqualTo(ResourceException.INTERNAL_ERROR);
    }

    @Test
    public void shouldPerformUpdate() throws EntitlementException {

        //given
        SSOTokenContext mockSSOTokenContext = mock(SSOTokenContext.class);
        RealmContext realmContext = new RealmContext(mockSSOTokenContext);
        ServerContext mockServerContext = new ServerContext(realmContext);
        UpdateRequest mockUpdateRequest = mock(UpdateRequest.class);
        ResultHandler mockResultHandler = mock(ResultHandler.class);
        Subject subject = new Subject();

        ReferralPrivilege mockOne = new ReferralPrivilege("two",
                Collections.<String, Set<String>>emptyMap(), Collections.<String>emptySet());

        given(mockSSOTokenContext.getCallerSubject()).willReturn(subject);
        given(privilegeManager.findByName(anyString())).willReturn(mockOne);
        given(privilegeManager.canFindByName(anyString())).willReturn(false);
        given(referralWrapper.getName()).willReturn("two");

        //when
        referralsResource.updateInstance(mockServerContext, "one", mockUpdateRequest, mockResultHandler);

        //then
        verify(mockResultHandler, times(1)).handleResult(any(Resource.class));
    }
}
