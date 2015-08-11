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
 * information: "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright 2015 ForgeRock AS.
 */

package org.forgerock.openam.rest.authz;

import static org.mockito.BDDMockito.given;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import com.iplanet.dpro.session.service.SessionService;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.authz.filter.api.AuthorizationResult;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openam.forgerockrest.utils.AgentIdentity;
import org.forgerock.openam.forgerockrest.utils.SpecialUserIdentity;
import org.forgerock.openam.rest.resource.SSOTokenContext;
import org.forgerock.openam.utils.Config;
import org.forgerock.util.promise.Promise;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.security.Principal;
import java.util.concurrent.ExecutionException;

public class STSTokenGenerationServiceAuthzModuleTest {
    @Mock
    private AgentIdentity mockAgentIdentity;
    @Mock
    private SSOToken mockSSOToken;
    @Mock
    private Debug mockDebug;
    @Mock
    private CreateRequest mockCreateRequest;
    @Mock
    private ReadRequest mockReadRequest;
    @Mock
    private UpdateRequest mockUpdateRequest;
    @Mock
    private DeleteRequest mockDeleteRequest;
    @Mock
    private PatchRequest mockPatchRequest;
    @Mock
    private ActionRequest mockActionRequest;
    @Mock
    private QueryRequest mockQueryRequest;
    @Mock
    private SSOTokenContext mockSSOTokenContext;
    @Mock
    private Principal mockPrincipal;
    @Mock
    private SpecialUserIdentity mockSpecialUserIdentity;
    @Mock
    private Config<SessionService> mockConfig;
    @Mock
    private SessionService mockSessionService;


    private STSTokenGenerationServiceAuthzModule authzModule;

    @BeforeTest
    public void setup() throws SSOException {
        MockitoAnnotations.initMocks(this);
        authzModule = new STSTokenGenerationServiceAuthzModule(mockConfig, mockAgentIdentity, mockSpecialUserIdentity, mockDebug);
        given(mockSSOTokenContext.getCallerSSOToken()).willReturn(mockSSOToken);
        given(mockSSOToken.getPrincipal()).willReturn(mockPrincipal);
        given(mockPrincipal.getName()).willReturn("irrelevant");
        given(mockConfig.get()).willReturn(mockSessionService);
    }

    @Test
    public void testCreateSTSAgentSuccess() throws ExecutionException, InterruptedException, SSOException {
        //given
        given(mockAgentIdentity.isSoapSTSAgent(mockSSOToken)).willReturn(true);
        given(mockSpecialUserIdentity.isSpecialUser(mockSSOToken)).willReturn(false);
        //when
        Promise<AuthorizationResult, ResourceException> result = authzModule.authorizeCreate(mockSSOTokenContext,
                mockCreateRequest);
        //then
        assertTrue(result.get().isAuthorized());
    }

    @Test
    public void testCreateSpecialUserSuccess() throws ExecutionException, InterruptedException {
        //given
        given(mockAgentIdentity.isSoapSTSAgent(mockSSOToken)).willReturn(false);
        given(mockSpecialUserIdentity.isSpecialUser(mockSSOToken)).willReturn(true);
        //when
        Promise<AuthorizationResult, ResourceException> result = authzModule.authorizeCreate(mockSSOTokenContext,
                mockCreateRequest);
        //then
        assertTrue(result.get().isAuthorized());
    }

    @Test
    public void testCreateAdminUserSuccess() throws ExecutionException, InterruptedException, SSOException {
        //given
        given(mockAgentIdentity.isSoapSTSAgent(mockSSOToken)).willReturn(false);
        given(mockSpecialUserIdentity.isSpecialUser(mockSSOToken)).willReturn(false);
        given(mockSSOToken.getProperty(Constants.UNIVERSAL_IDENTIFIER)).willReturn("test");
        given(mockSessionService.isSuperUser("test")).willReturn(true);
        //when
        Promise<AuthorizationResult, ResourceException> result = authzModule.authorizeCreate(mockSSOTokenContext,
                mockCreateRequest);
        //then
        assertTrue(result.get().isAuthorized());
    }

    @Test
    public void testReadSTSAgentSuccess() throws ExecutionException, InterruptedException, SSOException {
        //given
        given(mockAgentIdentity.isSoapSTSAgent(mockSSOToken)).willReturn(true);
        given(mockSpecialUserIdentity.isSpecialUser(mockSSOToken)).willReturn(false);
        //when
        Promise<AuthorizationResult, ResourceException> result = authzModule.authorizeRead(mockSSOTokenContext,
                mockReadRequest);
        //then
        assertTrue(result.get().isAuthorized());
    }

    @Test
    public void testReadSpecialUserSuccess() throws ExecutionException, InterruptedException {
        //given
        given(mockAgentIdentity.isSoapSTSAgent(mockSSOToken)).willReturn(false);
        given(mockSpecialUserIdentity.isSpecialUser(mockSSOToken)).willReturn(true);
        //when
        Promise<AuthorizationResult, ResourceException> result = authzModule.authorizeRead(mockSSOTokenContext,
                mockReadRequest);
        //then
        assertTrue(result.get().isAuthorized());
    }

    @Test
    public void testReadAdminUserSuccess() throws ExecutionException, InterruptedException, SSOException {
        //given
        given(mockAgentIdentity.isSoapSTSAgent(mockSSOToken)).willReturn(false);
        given(mockSpecialUserIdentity.isSpecialUser(mockSSOToken)).willReturn(false);
        given(mockSSOToken.getProperty(Constants.UNIVERSAL_IDENTIFIER)).willReturn("test");
        given(mockSessionService.isSuperUser("test")).willReturn(true);
        //when
        Promise<AuthorizationResult, ResourceException> result = authzModule.authorizeRead(mockSSOTokenContext,
                mockReadRequest);
        //then
        assertTrue(result.get().isAuthorized());
    }

    @Test
    public void testDeleteSTSAgentSuccess() throws ExecutionException, InterruptedException, SSOException {
        //given
        given(mockAgentIdentity.isSoapSTSAgent(mockSSOToken)).willReturn(true);
        given(mockSpecialUserIdentity.isSpecialUser(mockSSOToken)).willReturn(false);
        //when
        Promise<AuthorizationResult, ResourceException> result = authzModule.authorizeDelete(mockSSOTokenContext,
                mockDeleteRequest);
        //then
        assertTrue(result.get().isAuthorized());
    }

    @Test
    public void testDeleteSpecialUserSuccess() throws ExecutionException, InterruptedException {
        //given
        given(mockAgentIdentity.isSoapSTSAgent(mockSSOToken)).willReturn(false);
        given(mockSpecialUserIdentity.isSpecialUser(mockSSOToken)).willReturn(true);
        //when
        Promise<AuthorizationResult, ResourceException> result = authzModule.authorizeDelete(mockSSOTokenContext,
                mockDeleteRequest);
        //then
        assertTrue(result.get().isAuthorized());
    }

    @Test
    public void testDeleteAdminUserSuccess() throws ExecutionException, InterruptedException, SSOException {
        //given
        given(mockAgentIdentity.isSoapSTSAgent(mockSSOToken)).willReturn(false);
        given(mockSpecialUserIdentity.isSpecialUser(mockSSOToken)).willReturn(false);
        given(mockSSOToken.getProperty(Constants.UNIVERSAL_IDENTIFIER)).willReturn("test");
        given(mockSessionService.isSuperUser("test")).willReturn(true);
        //when
        Promise<AuthorizationResult, ResourceException> result = authzModule.authorizeDelete(mockSSOTokenContext,
                mockDeleteRequest);
        //then
        assertTrue(result.get().isAuthorized());
    }

    @Test
    public void testQuerySTSAgentSuccess() throws ExecutionException, InterruptedException, SSOException {
        //given
        given(mockAgentIdentity.isSoapSTSAgent(mockSSOToken)).willReturn(true);
        given(mockSpecialUserIdentity.isSpecialUser(mockSSOToken)).willReturn(false);
        //when
        Promise<AuthorizationResult, ResourceException> result = authzModule.authorizeQuery(mockSSOTokenContext,
                mockQueryRequest);
        //then
        assertTrue(result.get().isAuthorized());
    }

    @Test
    public void testQuerySpecialUserSuccess() throws ExecutionException, InterruptedException {
        //given
        given(mockAgentIdentity.isSoapSTSAgent(mockSSOToken)).willReturn(false);
        given(mockSpecialUserIdentity.isSpecialUser(mockSSOToken)).willReturn(true);
        //when
        Promise<AuthorizationResult, ResourceException> result = authzModule.authorizeQuery(mockSSOTokenContext,
                mockQueryRequest);
        //then
        assertTrue(result.get().isAuthorized());
    }

    @Test
    public void testQueryAdminUserSuccess() throws ExecutionException, InterruptedException, SSOException {
        //given
        given(mockAgentIdentity.isSoapSTSAgent(mockSSOToken)).willReturn(false);
        given(mockSpecialUserIdentity.isSpecialUser(mockSSOToken)).willReturn(false);
        given(mockSSOToken.getProperty(Constants.UNIVERSAL_IDENTIFIER)).willReturn("test");
        given(mockSessionService.isSuperUser("test")).willReturn(true);
        //when
        Promise<AuthorizationResult, ResourceException> result = authzModule.authorizeQuery(mockSSOTokenContext,
                mockQueryRequest);
        //then
        assertTrue(result.get().isAuthorized());
    }

    @Test
    public void testNoAuthz() throws ExecutionException, InterruptedException, SSOException {
        //given
        given(mockAgentIdentity.isSoapSTSAgent(mockSSOToken)).willReturn(false);
        given(mockSpecialUserIdentity.isSpecialUser(mockSSOToken)).willReturn(false);
        given(mockSSOToken.getProperty(Constants.UNIVERSAL_IDENTIFIER)).willReturn("test");
        given(mockSessionService.isSuperUser("test")).willReturn(false);
        //when
        Promise<AuthorizationResult, ResourceException> result = authzModule.authorizeCreate(mockSSOTokenContext,
                mockCreateRequest);
        //then
        assertFalse(result.get().isAuthorized());
    }

    @Test
    public void testUpdateRejected() throws ExecutionException, InterruptedException {
        //when
        Promise<AuthorizationResult, ResourceException> result = authzModule.authorizeUpdate(mockSSOTokenContext, mockUpdateRequest);
        //then
        assertFalse(result.get().isAuthorized());
    }

    @Test
    public void testPatchRejected() throws ExecutionException, InterruptedException {
        //when
        Promise<AuthorizationResult, ResourceException> result = authzModule.authorizePatch(mockSSOTokenContext, mockPatchRequest);
        //then
        assertFalse(result.get().isAuthorized());
    }

    @Test
    public void testActionRejected() throws ExecutionException, InterruptedException {
        //when
        Promise<AuthorizationResult, ResourceException> result = authzModule.authorizeAction(mockSSOTokenContext,
                mockActionRequest);
        //then
        assertFalse(result.get().isAuthorized());
    }
}
