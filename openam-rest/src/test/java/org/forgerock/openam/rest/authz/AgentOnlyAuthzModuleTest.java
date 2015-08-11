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
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openam.forgerockrest.utils.AgentIdentity;
import org.forgerock.openam.rest.resource.SSOTokenContext;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.security.Principal;

public class AgentOnlyAuthzModuleTest {

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

    private AgentOnlyAuthzModule authzModule;

    @BeforeTest
    public void setup() throws SSOException {
        MockitoAnnotations.initMocks(this);
        authzModule = new AgentOnlyAuthzModule(mockAgentIdentity, mockDebug);
        given(mockSSOTokenContext.getCallerSSOToken()).willReturn(mockSSOToken);
        given(mockSSOToken.getPrincipal()).willReturn(mockPrincipal);
        given(mockPrincipal.getName()).willReturn("irrelevant");
    }

    @Test
    public void testAgentSuccess() throws Exception {
        //given
        given(mockAgentIdentity.isAgent(mockSSOToken)).willReturn(true);

        //then
        assertTrue(authzModule.authorizeCreate(mockSSOTokenContext, mockCreateRequest).get().isAuthorized());
        assertTrue(authzModule.authorizeRead(mockSSOTokenContext, mockReadRequest).get().isAuthorized());
        assertTrue(authzModule.authorizeUpdate(mockSSOTokenContext, mockUpdateRequest).get().isAuthorized());
        assertTrue(authzModule.authorizeDelete(mockSSOTokenContext, mockDeleteRequest).get().isAuthorized());
        assertTrue(authzModule.authorizePatch(mockSSOTokenContext, mockPatchRequest).get().isAuthorized());
        assertTrue(authzModule.authorizeAction(mockSSOTokenContext, mockActionRequest).get().isAuthorized());
        assertTrue(authzModule.authorizeQuery(mockSSOTokenContext, mockQueryRequest).get().isAuthorized());
    }

    @Test
    public void testNoAuthz() throws Exception {
        //given
        given(mockAgentIdentity.isAgent(mockSSOToken)).willReturn(false);

        //then
        assertFalse(authzModule.authorizeCreate(mockSSOTokenContext, mockCreateRequest).get().isAuthorized());
        assertFalse(authzModule.authorizeRead(mockSSOTokenContext, mockReadRequest).get().isAuthorized());
        assertFalse(authzModule.authorizeUpdate(mockSSOTokenContext, mockUpdateRequest).get().isAuthorized());
        assertFalse(authzModule.authorizeDelete(mockSSOTokenContext, mockDeleteRequest).get().isAuthorized());
        assertFalse(authzModule.authorizePatch(mockSSOTokenContext, mockPatchRequest).get().isAuthorized());
        assertFalse(authzModule.authorizeAction(mockSSOTokenContext, mockActionRequest).get().isAuthorized());
        assertFalse(authzModule.authorizeQuery(mockSSOTokenContext, mockQueryRequest).get().isAuthorized());
    }
}
