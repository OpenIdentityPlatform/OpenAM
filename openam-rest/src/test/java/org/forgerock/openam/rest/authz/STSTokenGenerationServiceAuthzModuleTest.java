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

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
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
import org.forgerock.openam.forgerockrest.utils.SoapSTSAgentIdentity;
import org.forgerock.openam.forgerockrest.utils.SpecialUserIdentity;
import org.forgerock.openam.rest.resource.SSOTokenContext;
import org.forgerock.util.promise.Promise;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.security.Principal;
import java.util.concurrent.ExecutionException;

public class STSTokenGenerationServiceAuthzModuleTest {
    @Mock
    private SoapSTSAgentIdentity mockSoapSTSAgentIdentity;
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

    private STSTokenGenerationServiceAuthzModule authzModule;

    @BeforeTest
    public void setup() throws SSOException {
        MockitoAnnotations.initMocks(this);
        authzModule = new STSTokenGenerationServiceAuthzModule(mockSoapSTSAgentIdentity, mockSpecialUserIdentity, mockDebug);
        given(mockSSOTokenContext.getCallerSSOToken()).willReturn(mockSSOToken);
        given(mockSSOToken.getPrincipal()).willReturn(mockPrincipal);
        given(mockPrincipal.getName()).willReturn("irrelevant");
    }

    @Test
    public void testSTSAgentSuccess() throws ExecutionException, InterruptedException, SSOException {
        //given
        given(mockSoapSTSAgentIdentity.isSoapSTSAgent(mockSSOToken)).willReturn(true);
        given(mockSpecialUserIdentity.isSpecialUser(mockSSOToken)).willReturn(false);
        //when
        Promise<AuthorizationResult, ResourceException> result = authzModule.authorizeAction(mockSSOTokenContext, mockActionRequest);
        //then
        assertTrue(result.get().isAuthorized());
    }

    @Test
    public void testSpecialUserSuccess() throws ExecutionException, InterruptedException {
        //given
        given(mockSoapSTSAgentIdentity.isSoapSTSAgent(mockSSOToken)).willReturn(false);
        given(mockSpecialUserIdentity.isSpecialUser(mockSSOToken)).willReturn(true);
        //when
        Promise<AuthorizationResult, ResourceException> result = authzModule.authorizeAction(mockSSOTokenContext, mockActionRequest);
        //then
        assertTrue(result.get().isAuthorized());
    }

    @Test
    public void testNoAuthz() throws ExecutionException, InterruptedException {
        //given
        given(mockSoapSTSAgentIdentity.isSoapSTSAgent(mockSSOToken)).willReturn(false);
        given(mockSpecialUserIdentity.isSpecialUser(mockSSOToken)).willReturn(false);
        //when
        Promise<AuthorizationResult, ResourceException> result = authzModule.authorizeAction(mockSSOTokenContext, mockActionRequest);
        //then
        assertFalse(result.get().isAuthorized());
    }

    @Test
    public void testReadRejected() throws ExecutionException, InterruptedException {
        //when
        Promise<AuthorizationResult, ResourceException> result = authzModule.authorizeRead(mockSSOTokenContext, mockReadRequest);
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
    public void testDeleteRejected() throws ExecutionException, InterruptedException {
        //when
        Promise<AuthorizationResult, ResourceException> result = authzModule.authorizeDelete(mockSSOTokenContext, mockDeleteRequest);
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
    public void testCreateRejected() throws ExecutionException, InterruptedException {
        //when
        Promise<AuthorizationResult, ResourceException> result = authzModule.authorizeCreate(mockSSOTokenContext, mockCreateRequest);
        //then
        assertFalse(result.get().isAuthorized());
    }

    @Test
    public void testQueryRejected() throws ExecutionException, InterruptedException {
        //when
        Promise<AuthorizationResult, ResourceException> result = authzModule.authorizeQuery(mockSSOTokenContext, mockQueryRequest);
        //then
        assertFalse(result.get().isAuthorized());
    }
}
