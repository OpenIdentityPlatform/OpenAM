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
* Copyright 2014-2015 ForgeRock AS.
*/

package org.forgerock.openam.entitlement.rest;

import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anySet;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertTrue;

import javax.security.auth.Subject;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.ReferralPrivilege;
import com.sun.identity.entitlement.ReferralPrivilegeManager;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.services.context.Context;
import org.forgerock.services.context.ClientContext;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.ConflictException;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openam.entitlement.rest.wrappers.ReferralWrapper;
import org.forgerock.openam.rest.RealmContext;
import org.forgerock.openam.rest.resource.SSOTokenContext;
import org.forgerock.util.promise.Promise;
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
            boolean isRequestRealmsValidPeerOrSubrealms(Context serverContext, String realm,
                                                        Set<String> realms) {
                return true;
            }
        };

    }

    @Test (expectedExceptions = BadRequestException.class)
    public void shouldBadRequestIfSubjectNullOnCreate() throws ResourceException {

        //given
        SSOTokenContext mockSSOTokenContext = mock(SSOTokenContext.class);
        RealmContext realmContext = new RealmContext(mockSSOTokenContext);
        Context mockServerContext = ClientContext.newInternalClientContext(realmContext);
        CreateRequest mockCreateRequest = mock(CreateRequest.class);

        given(mockSSOTokenContext.getCallerSubject()).willReturn(null);

        //when
        Promise<ResourceResponse, ResourceException> result =
                referralsResource.createInstance(mockServerContext, mockCreateRequest);

        //then
        result.getOrThrowUninterruptibly();
    }

    @Test (expectedExceptions = BadRequestException.class)
    public void shouldBadRequestIfJsonUnreadable() throws ResourceException {
        //given
        SSOTokenContext mockSSOTokenContext = mock(SSOTokenContext.class);
        RealmContext realmContext = new RealmContext(mockSSOTokenContext);
        Context mockServerContext = ClientContext.newInternalClientContext(realmContext);
        CreateRequest mockCreateRequest = mock(CreateRequest.class);
        Subject subject = new Subject();

        referralsResource = new ReferralsResourceV1(debug) {

            @Override
            protected ReferralWrapper createReferralWrapper(JsonValue jsonValue) throws IOException {
                throw new IOException();
            }
        };

        given(mockSSOTokenContext.getCallerSubject()).willReturn(subject);

        //when
        Promise<ResourceResponse, ResourceException> result =
                referralsResource.createInstance(mockServerContext, mockCreateRequest);

        //then
        result.getOrThrowUninterruptibly();
    }

    @Test (expectedExceptions = ConflictException.class)
    public void shouldConflictIfResourceExists() throws EntitlementException, ResourceException {
        //given
        SSOTokenContext mockSSOTokenContext = mock(SSOTokenContext.class);
        RealmContext realmContext = new RealmContext(mockSSOTokenContext);
        Context mockServerContext = ClientContext.newInternalClientContext(realmContext);
        CreateRequest mockCreateRequest = mock(CreateRequest.class);
        Subject subject = new Subject();

        given(mockSSOTokenContext.getCallerSubject()).willReturn(subject);
        given(privilegeManager.canFindByName(anyString())).willReturn(true);
        given(referralWrapper.getName()).willReturn("referralName");

        //when
        Promise<ResourceResponse, ResourceException> result =
                referralsResource.createInstance(mockServerContext, mockCreateRequest);

        //then
        result.getOrThrowUninterruptibly();

    }

    @Test (expectedExceptions = InternalServerErrorException.class)
    public void shouldErrorIfCannotCheckThatResourceExists() throws EntitlementException, ResourceException {
        //given
        SSOTokenContext mockSSOTokenContext = mock(SSOTokenContext.class);
        RealmContext realmContext = new RealmContext(mockSSOTokenContext);
        Context mockServerContext = ClientContext.newInternalClientContext(realmContext);
        CreateRequest mockCreateRequest = mock(CreateRequest.class);
        Subject subject = new Subject();

        given(mockSSOTokenContext.getCallerSubject()).willReturn(subject);
        given(privilegeManager.canFindByName(anyString())).willThrow(new EntitlementException(1));
        given(referralWrapper.getName()).willReturn("referralName");

        //when
        Promise<ResourceResponse, ResourceException> result =
                referralsResource.createInstance(mockServerContext, mockCreateRequest);

        //then
        result.getOrThrowUninterruptibly();
    }

    @Test (expectedExceptions = BadRequestException.class)
    public void shouldBadRequestIfRealmNotAcceptable() throws ResourceException, EntitlementException {

        //given
        SSOTokenContext mockSSOTokenContext = mock(SSOTokenContext.class);
        RealmContext realmContext = new RealmContext(mockSSOTokenContext);
        Context mockServerContext = ClientContext.newInternalClientContext(realmContext);
        CreateRequest mockCreateRequest = mock(CreateRequest.class);
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
            boolean isRequestRealmsValidPeerOrSubrealms(Context serverContext, String realm,
                                                                Set<String> realms) {
                return false;
            }
        };

        given(mockSSOTokenContext.getCallerSubject()).willReturn(subject);
        given(privilegeManager.canFindByName(anyString())).willReturn(false);
        given(referralWrapper.getName()).willReturn("referralName");

        //when
        Promise<ResourceResponse, ResourceException> result =
                referralsResource.createInstance(mockServerContext, mockCreateRequest);

        //then
        result.getOrThrowUninterruptibly();
    }

    @Test (expectedExceptions = BadRequestException.class)
    public void shouldBadRequestIfNameContainsInvalidCharacters() throws EntitlementException, ResourceException {

        //given
        SSOTokenContext mockSSOTokenContext = mock(SSOTokenContext.class);
        RealmContext realmContext = new RealmContext(mockSSOTokenContext);
        Context mockServerContext = ClientContext.newInternalClientContext(realmContext);
        CreateRequest mockCreateRequest = mock(CreateRequest.class);
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
            boolean isRequestRealmsValidPeerOrSubrealms(Context serverContext, String realm,
                                                        Set<String> realms) {
                return false;
            }
        };

        given(mockSSOTokenContext.getCallerSubject()).willReturn(subject);
        given(privilegeManager.canFindByName(anyString())).willReturn(false);
        given(referralWrapper.getName()).willReturn("referral+name");

        //when
        Promise<ResourceResponse, ResourceException> result =
                referralsResource.createInstance(mockServerContext, mockCreateRequest);

        //then
        result.getOrThrowUninterruptibly();
    }


    @Test (expectedExceptions = InternalServerErrorException.class)
    public void shouldInternalErrorIfCannotSaveReferral() throws EntitlementException, ResourceException {

        //given
        SSOTokenContext mockSSOTokenContext = mock(SSOTokenContext.class);
        RealmContext realmContext = new RealmContext(mockSSOTokenContext);
        Context mockServerContext = ClientContext.newInternalClientContext(realmContext);
        CreateRequest mockCreateRequest = mock(CreateRequest.class);
        Subject subject = new Subject();

        given(mockSSOTokenContext.getCallerSubject()).willReturn(subject);
        given(privilegeManager.canFindByName(anyString())).willReturn(false);
        doThrow(new EntitlementException(1)).when(privilegeManager).add(any(ReferralPrivilege.class));

        //when
        Promise<ResourceResponse, ResourceException> result =
                referralsResource.createInstance(mockServerContext, mockCreateRequest);

        //then
        result.getOrThrowUninterruptibly();
    }

    @Test (expectedExceptions = NotFoundException.class)
    public void shouldNotFoundDeleteResourceThatDoesntExist() throws EntitlementException, ResourceException {

        //given
        SSOTokenContext mockSSOTokenContext = mock(SSOTokenContext.class);
        RealmContext realmContext = new RealmContext(mockSSOTokenContext);
        Context mockServerContext = ClientContext.newInternalClientContext(realmContext);
        DeleteRequest mockDeleteRequest = mock(DeleteRequest.class);
        Subject subject = new Subject();

        given(mockSSOTokenContext.getCallerSubject()).willReturn(subject);
        given(privilegeManager.canFindByName(anyString())).willReturn(false);

        //when
        Promise<ResourceResponse, ResourceException> result =
                referralsResource.deleteInstance(mockServerContext, "ID", mockDeleteRequest);

        //then
        result.getOrThrowUninterruptibly();
    }

    @Test (expectedExceptions = InternalServerErrorException.class)
    public void shouldInternalErrorDeleteResourceThatWontDelete() throws EntitlementException, ResourceException {

        //given
        SSOTokenContext mockSSOTokenContext = mock(SSOTokenContext.class);
        RealmContext realmContext = new RealmContext(mockSSOTokenContext);
        Context mockServerContext = ClientContext.newInternalClientContext(realmContext);
        DeleteRequest mockDeleteRequest = mock(DeleteRequest.class);
        Subject subject = new Subject();

        given(mockSSOTokenContext.getCallerSubject()).willReturn(subject);
        given(privilegeManager.canFindByName(anyString())).willReturn(true);
        doThrow(new EntitlementException(1)).when(privilegeManager).remove(anyString());

        //when
        Promise<ResourceResponse, ResourceException> result =
                referralsResource.deleteInstance(mockServerContext, "ID", mockDeleteRequest);

        //then
        result.getOrThrowUninterruptibly();
    }

    @Test (expectedExceptions = InternalServerErrorException.class)
    public void shouldInternalErrorWhenCannotQueryDataset() throws EntitlementException, ResourceException {

        //given
        SSOTokenContext mockSSOTokenContext = mock(SSOTokenContext.class);
        RealmContext realmContext = new RealmContext(mockSSOTokenContext);
        Context mockServerContext = ClientContext.newInternalClientContext(realmContext);
        QueryRequest mockQueryRequest = mock(QueryRequest.class);
        QueryResourceHandler mockQueryResultHandler = mock(QueryResourceHandler.class);
        Subject subject = new Subject();

        given(mockSSOTokenContext.getCallerSubject()).willReturn(subject);
        given(privilegeManager.searchNames(anySet())).willThrow(new EntitlementException(1));

        //when\
        Promise<QueryResponse, ResourceException> result =
           referralsResource.queryCollection(mockServerContext, mockQueryRequest, mockQueryResultHandler);

        //then
        result.getOrThrowUninterruptibly();
    }

    @Test
    public void shouldPerformQuery() throws EntitlementException {

        //given
        SSOTokenContext mockSSOTokenContext = mock(SSOTokenContext.class);
        RealmContext realmContext = new RealmContext(mockSSOTokenContext);
        Context mockServerContext = ClientContext.newInternalClientContext(realmContext);
        QueryRequest mockQueryRequest = mock(QueryRequest.class);
        QueryResourceHandler mockQueryResultHandler = mock(QueryResourceHandler.class);
        Subject subject = new Subject();

        Set<String> nameSet = new HashSet<>();
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
        given(mockQueryResultHandler.handleResource(any(ResourceResponse.class))).willReturn(true);

        //when
        Promise<QueryResponse, ResourceException> result =
                referralsResource.queryCollection(mockServerContext, mockQueryRequest, mockQueryResultHandler);

        //then
        verify(mockQueryResultHandler, times(2)).handleResource(any(ResourceResponse.class));
    }

    @Test
    public void shouldPerformCreate() throws EntitlementException, IOException {

        //given
        SSOTokenContext mockSSOTokenContext = mock(SSOTokenContext.class);
        RealmContext realmContext = new RealmContext(mockSSOTokenContext);
        Context mockServerContext = ClientContext.newInternalClientContext(realmContext);
        CreateRequest mockCreateRequest = mock(CreateRequest.class);
        Subject subject = new Subject();
        JsonValue mockJson = mock(JsonValue.class);

        given(mockSSOTokenContext.getCallerSubject()).willReturn(subject);
        given(privilegeManager.canFindByName(anyString())).willReturn(false);
        given(referralWrapper.getName()).willReturn("NAME");
        given(referralWrapper.getLastModifiedDate()).willReturn(1000l);
        given(referralWrapper.toJsonValue()).willReturn(mockJson);

        //when
        Promise<ResourceResponse, ResourceException> result =
            referralsResource.createInstance(mockServerContext, mockCreateRequest);

        //then
        assertTrue(result.getOrThrowUninterruptibly().getContent().equals(mockJson));
    }

    @Test
    public void shouldPerformDelete() throws EntitlementException, IOException {

        //given
        SSOTokenContext mockSSOTokenContext = mock(SSOTokenContext.class);
        RealmContext realmContext = new RealmContext(mockSSOTokenContext);
        Context mockServerContext = ClientContext.newInternalClientContext(realmContext);
        DeleteRequest mockDeleteRequest = mock(DeleteRequest.class);
        Subject subject = new Subject();

        given(mockSSOTokenContext.getCallerSubject()).willReturn(subject);
        given(privilegeManager.canFindByName(anyString())).willReturn(true);

        //when
        Promise<ResourceResponse, ResourceException> result =
                referralsResource.deleteInstance(mockServerContext, "ID", mockDeleteRequest);

        //then
        assertTrue(result.getOrThrowUninterruptibly().getId().equals("ID"));
        assertTrue(result.getOrThrowUninterruptibly().getRevision().equals("0"));
    }

    @Test (expectedExceptions = NotFoundException.class)
    public void shouldNotFoundWhenReadingInvalidResource() throws EntitlementException, ResourceException {

        //given
        SSOTokenContext mockSSOTokenContext = mock(SSOTokenContext.class);
        RealmContext realmContext = new RealmContext(mockSSOTokenContext);
        Context mockServerContext = ClientContext.newInternalClientContext(realmContext);
        ReadRequest mockReadRequest = mock(ReadRequest.class);
        Subject subject = new Subject();

        given(mockSSOTokenContext.getCallerSubject()).willReturn(subject);
        given(privilegeManager.findByName(anyString())).willThrow(new EntitlementException(1));

        //when
        Promise<ResourceResponse, ResourceException> result =
            referralsResource.readInstance(mockServerContext, "ID", mockReadRequest);

        //then
        result.getOrThrowUninterruptibly();
    }

    @Test
    public void shouldPerformRead() throws EntitlementException, ResourceException {
        //given
        SSOTokenContext mockSSOTokenContext = mock(SSOTokenContext.class);
        RealmContext realmContext = new RealmContext(mockSSOTokenContext);
        Context mockServerContext = ClientContext.newInternalClientContext(realmContext);
        ReadRequest mockReadRequest = mock(ReadRequest.class);
        Subject subject = new Subject();
        ReferralPrivilege mockOne = new ReferralPrivilege("one",
                Collections.<String, Set<String>>emptyMap(), Collections.<String>emptySet());

        given(mockSSOTokenContext.getCallerSubject()).willReturn(subject);
        given(privilegeManager.findByName(anyString())).willReturn(mockOne);

        //when
        Promise<ResourceResponse, ResourceException> result =
                referralsResource.readInstance(mockServerContext, "one", mockReadRequest);

        //then
        assertTrue(result.getOrThrowUninterruptibly().getId().equals("one"));
    }

    @Test (expectedExceptions = BadRequestException.class)
    public void shouldBadRequestIfUpdateInvalidRequest() throws EntitlementException, ResourceException {

        //given
        SSOTokenContext mockSSOTokenContext = mock(SSOTokenContext.class);
        RealmContext realmContext = new RealmContext(mockSSOTokenContext);
        Context mockServerContext = ClientContext.newInternalClientContext(realmContext);
        UpdateRequest mockUpdateRequest = mock(UpdateRequest.class);
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
            boolean isRequestRealmsValidPeerOrSubrealms(Context serverContext, String realm,
                                                        Set<String> realms) {
                return true;
            }
        };

        given(mockSSOTokenContext.getCallerSubject()).willReturn(subject);

        //when
        Promise<ResourceResponse, ResourceException> result =
                referralsResource.updateInstance(mockServerContext, "one", mockUpdateRequest);

        //then
        result.getOrThrowUninterruptibly();
    }

    @Test (expectedExceptions = NotFoundException.class)
    public void shouldNotFoundIfNothingToUpdate() throws EntitlementException, ResourceException {

        //given
        SSOTokenContext mockSSOTokenContext = mock(SSOTokenContext.class);
        RealmContext realmContext = new RealmContext(mockSSOTokenContext);
        Context mockServerContext = ClientContext.newInternalClientContext(realmContext);
        UpdateRequest mockUpdateRequest = mock(UpdateRequest.class);
        Subject subject = new Subject();

        given(mockSSOTokenContext.getCallerSubject()).willReturn(subject);
        given(privilegeManager.findByName(anyString())).willThrow(new EntitlementException(1));

        //when
        Promise<ResourceResponse, ResourceException> result =
                referralsResource.updateInstance(mockServerContext, "one", mockUpdateRequest);

        //then
        result.getOrThrowUninterruptibly();
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

    @Test (expectedExceptions = BadRequestException.class)
    public void shouldBadRequestUpdateToInvalidRealms() throws EntitlementException, ResourceException {

        //given
        SSOTokenContext mockSSOTokenContext = mock(SSOTokenContext.class);
        RealmContext realmContext = new RealmContext(mockSSOTokenContext);
        Context mockServerContext = ClientContext.newInternalClientContext(realmContext);
        UpdateRequest mockUpdateRequest = mock(UpdateRequest.class);
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
            boolean isRequestRealmsValidPeerOrSubrealms(Context serverContext, String realm,
                                                        Set<String> realms) {
                return false;
            }
        };

        ReferralPrivilege mockOne = new ReferralPrivilege("one",
                Collections.<String, Set<String>>emptyMap(), Collections.<String>emptySet());

        given(mockSSOTokenContext.getCallerSubject()).willReturn(subject);
        given(privilegeManager.findByName(anyString())).willReturn(mockOne);

        //when
        Promise<ResourceResponse, ResourceException> result =
                referralsResource.updateInstance(mockServerContext, "one", mockUpdateRequest);

        //then
        result.getOrThrowUninterruptibly();
    }

    @Test (expectedExceptions = ConflictException.class)
    public void shouldConflictIfIntendedNamedReferralAlreadyExists() throws EntitlementException, ResourceException {

        //given
        SSOTokenContext mockSSOTokenContext = mock(SSOTokenContext.class);
        RealmContext realmContext = new RealmContext(mockSSOTokenContext);
        Context mockServerContext = ClientContext.newInternalClientContext(realmContext);
        UpdateRequest mockUpdateRequest = mock(UpdateRequest.class);
        Subject subject = new Subject();

        ReferralPrivilege mockOne = new ReferralPrivilege("two",
                Collections.<String, Set<String>>emptyMap(), Collections.<String>emptySet());

        given(mockSSOTokenContext.getCallerSubject()).willReturn(subject);
        given(privilegeManager.findByName(anyString())).willReturn(mockOne);
        given(privilegeManager.canFindByName(anyString())).willReturn(true);

        //when
        Promise<ResourceResponse, ResourceException> result =
                referralsResource.updateInstance(mockServerContext, "one", mockUpdateRequest);

        //then
        result.getOrThrowUninterruptibly();
    }

    @Test (expectedExceptions = InternalServerErrorException.class)
    public void shouldInternalErrorIfCannotCheckExistingResource() throws EntitlementException, ResourceException {

        //given
        SSOTokenContext mockSSOTokenContext = mock(SSOTokenContext.class);
        RealmContext realmContext = new RealmContext(mockSSOTokenContext);
        Context mockServerContext = ClientContext.newInternalClientContext(realmContext);
        UpdateRequest mockUpdateRequest = mock(UpdateRequest.class);
        Subject subject = new Subject();

        ReferralPrivilege mockOne = new ReferralPrivilege("two",
                Collections.<String, Set<String>>emptyMap(), Collections.<String>emptySet());

        given(mockSSOTokenContext.getCallerSubject()).willReturn(subject);
        given(privilegeManager.findByName(anyString())).willReturn(mockOne);
        given(privilegeManager.canFindByName(anyString())).willThrow(new EntitlementException(1));

        //when
        Promise<ResourceResponse, ResourceException> result =
                referralsResource.updateInstance(mockServerContext, "one", mockUpdateRequest);

        //then
        result.getOrThrowUninterruptibly();
    }

    @Test
    public void shouldPerformUpdate() throws EntitlementException, ResourceException {

        //given
        SSOTokenContext mockSSOTokenContext = mock(SSOTokenContext.class);
        RealmContext realmContext = new RealmContext(mockSSOTokenContext);
        Context mockServerContext = ClientContext.newInternalClientContext(realmContext);
        UpdateRequest mockUpdateRequest = mock(UpdateRequest.class);
        Subject subject = new Subject();

        ReferralPrivilege mockOne = new ReferralPrivilege("two",
                Collections.<String, Set<String>>emptyMap(), Collections.<String>emptySet());

        given(mockSSOTokenContext.getCallerSubject()).willReturn(subject);
        given(privilegeManager.findByName(anyString())).willReturn(mockOne);
        given(privilegeManager.canFindByName(anyString())).willReturn(false);
        given(referralWrapper.getName()).willReturn("two");

        //when
        Promise<ResourceResponse, ResourceException> result =
                referralsResource.updateInstance(mockServerContext, "one", mockUpdateRequest);

        //then
        assertTrue(result.getOrThrowUninterruptibly().getId().equals("two"));
    }
}
