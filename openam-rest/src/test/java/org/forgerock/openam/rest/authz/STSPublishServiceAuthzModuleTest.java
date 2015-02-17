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
import static org.mockito.Mockito.mock;
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
import org.forgerock.openam.forgerockrest.utils.SoapSTSAgentIdentity;
import org.forgerock.openam.rest.resource.SSOTokenContext;
import org.forgerock.openam.utils.Config;
import org.forgerock.util.promise.Promise;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.security.Principal;
import java.util.concurrent.ExecutionException;

public class STSPublishServiceAuthzModuleTest {
    @Mock
    private Config<SessionService> mockConfig;
    @Mock
    private SessionService mockService;
    @Mock
    private SoapSTSAgentIdentity mockAgentIdentity;
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
    private SSOToken mockSSOToken;
    @Mock
    private Principal mockPrincipal;

    private STSPublishServiceAuthzModule testModule;

    @BeforeTest
    public void beforeTest() throws SSOException {
        MockitoAnnotations.initMocks(this);
        given(mockConfig.get()).willReturn(mockService);
        testModule = new STSPublishServiceAuthzModule(mockConfig, mockAgentIdentity, mock(Debug.class));
        given(mockSSOTokenContext.getCallerSSOToken()).willReturn(mockSSOToken);
        given(mockSSOToken.getProperty(Constants.UNIVERSAL_IDENTIFIER)).willReturn("test");
        given(mockSSOToken.getPrincipal()).willReturn(mockPrincipal);
        given(mockPrincipal.getName()).willReturn("irrelevant");
    }

    @Test
    public void testAdminAuthz() throws SSOException, InterruptedException, ExecutionException {
        //given
        given(mockService.isSuperUser("test")).willReturn(true);
        given(mockAgentIdentity.isSoapSTSAgent(mockSSOToken)).willReturn(false);
        //when
        Promise<AuthorizationResult, ResourceException> result = testModule.authorizeCreate(mockSSOTokenContext, mockCreateRequest);
        //then
        assertTrue(result.get().isAuthorized());
        //when
        result = testModule.authorizeDelete(mockSSOTokenContext, mockDeleteRequest);
        //then
        assertTrue(result.get().isAuthorized());
        //when
        result = testModule.authorizeUpdate(mockSSOTokenContext, mockUpdateRequest);
        //then
        assertTrue(result.get().isAuthorized());
        //when
        result = testModule.authorizeRead(mockSSOTokenContext, mockReadRequest);
        //then
        assertTrue(result.get().isAuthorized());
    }

    @Test
    public void testSoapSTSAgentAuthz() throws SSOException, InterruptedException, ExecutionException {
        //given
        given(mockService.isSuperUser("test")).willReturn(false);
        given(mockAgentIdentity.isSoapSTSAgent(mockSSOToken)).willReturn(true);
        //when
        Promise<AuthorizationResult, ResourceException> result = testModule.authorizeRead(mockSSOTokenContext, mockReadRequest);
        //then
        assertTrue(result.get().isAuthorized());
    }

    @Test
    public void testSoapSTSAgentRejection() throws SSOException, InterruptedException, ExecutionException {
        //given
        given(mockService.isSuperUser("test")).willReturn(false);
        given(mockAgentIdentity.isSoapSTSAgent(mockSSOToken)).willReturn(true);
        //when
        Promise<AuthorizationResult, ResourceException> result = testModule.authorizeCreate(mockSSOTokenContext, mockCreateRequest);
        //then
        assertFalse(result.get().isAuthorized());
        //when
        result = testModule.authorizeDelete(mockSSOTokenContext, mockDeleteRequest);
        //then
        assertFalse(result.get().isAuthorized());
        //when
        result = testModule.authorizeUpdate(mockSSOTokenContext, mockUpdateRequest);
        //then
        assertFalse(result.get().isAuthorized());
    }

    @Test
    public void testSoapSTSAgentIllegalOperation() throws SSOException, InterruptedException, ExecutionException {
        //given
        given(mockService.isSuperUser("test")).willReturn(false);
        given(mockAgentIdentity.isSoapSTSAgent(mockSSOToken)).willReturn(true);
        //when
        Promise<AuthorizationResult, ResourceException> result = testModule.authorizePatch(mockSSOTokenContext, mockPatchRequest);
        //then
        assertFalse(result.get().isAuthorized());
        //when
        result = testModule.authorizeQuery(mockSSOTokenContext, mockQueryRequest);
        //then
        assertFalse(result.get().isAuthorized());
        //when
        result = testModule.authorizeUpdate(mockSSOTokenContext, mockUpdateRequest);
        //then
        assertFalse(result.get().isAuthorized());
        //when
        result = testModule.authorizeAction(mockSSOTokenContext, mockActionRequest);
        //then
        assertFalse(result.get().isAuthorized());
    }

    @Test
    public void testAdminIllegalOperation() throws SSOException, InterruptedException, ExecutionException {
        //given
        given(mockService.isSuperUser("test")).willReturn(true);
        given(mockAgentIdentity.isSoapSTSAgent(mockSSOToken)).willReturn(false);
        //when
        Promise<AuthorizationResult, ResourceException> result = testModule.authorizePatch(mockSSOTokenContext, mockPatchRequest);
        //then
        assertFalse(result.get().isAuthorized());
        //when
        result = testModule.authorizeQuery(mockSSOTokenContext, mockQueryRequest);
        //then
        assertFalse(result.get().isAuthorized());
        //when
        result = testModule.authorizeAction(mockSSOTokenContext, mockActionRequest);
        //then
        assertFalse(result.get().isAuthorized());
    }
}
