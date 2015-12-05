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
 * Copyright 2015 ForgeRock AS.
 */
package org.forgerock.openam.rest.authz;

import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.mock;
import static org.testng.Assert.*;

import java.security.Principal;

import org.forgerock.authz.filter.api.AuthorizationResult;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openam.forgerockrest.utils.AgentIdentity;
import org.forgerock.openam.forgerockrest.utils.SpecialUserIdentity;
import org.forgerock.openam.rest.resource.SSOTokenContext;
import org.forgerock.openam.utils.Config;
import org.forgerock.util.promise.Promise;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.iplanet.dpro.session.service.SessionService;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.debug.Debug;

public class SpecialOrAdminOrAgentAuthzModuleTest {

    SessionService mockService = mock(SessionService.class);

    public SpecialOrAdminOrAgentAuthzModule testModule;
    private AgentIdentity mockAgentIdentity;
    private SpecialUserIdentity mockSpecialUserIdentity;

    @BeforeTest
    public void beforeTest() {
        Config<SessionService> mockConfig = mock(Config.class);
        given(mockConfig.get()).willReturn(mockService);
        mockAgentIdentity = mock(AgentIdentity.class);
        mockSpecialUserIdentity = mock(SpecialUserIdentity.class);
        testModule = new SpecialOrAdminOrAgentAuthzModule(mockSpecialUserIdentity, mockAgentIdentity, mockConfig,
                mock(Debug.class));
    }

    @Test
    public void shouldAuthorizeAdmin() throws Exception {

        //given
        SSOTokenContext mockSSOTokenContext = mock(SSOTokenContext.class);
        SSOToken mockSSOToken = mock(SSOToken.class);
        Principal principal = mock(Principal.class);
        given(mockSSOToken.getPrincipal()).willReturn(principal);

        given(mockSSOTokenContext.getCallerSSOToken()).willReturn(mockSSOToken);
        given(mockSSOToken.getProperty(Constants.UNIVERSAL_IDENTIFIER)).willReturn("test");
        given(mockAgentIdentity.isAgent(mockSSOToken)).willReturn(false);
        given(mockSpecialUserIdentity.isSpecialUser(mockSSOToken)).willReturn(false);
        given(mockService.isSuperUser("test")).willReturn(true);

        //when
        Promise<AuthorizationResult, ResourceException> result = testModule.authorize(mockSSOTokenContext);

        //then
        assertTrue(result.get().isAuthorized());

    }

    @Test
    public void shouldAuthorizeAgent() throws Exception {

        //given
        SSOTokenContext mockSSOTokenContext = mock(SSOTokenContext.class);
        SSOToken mockSSOToken = mock(SSOToken.class);
        Principal principal = mock(Principal.class);
        given(mockSSOToken.getPrincipal()).willReturn(principal);

        given(mockSSOTokenContext.getCallerSSOToken()).willReturn(mockSSOToken);
        given(mockSSOToken.getProperty(Constants.UNIVERSAL_IDENTIFIER)).willReturn("test");
        given(mockAgentIdentity.isAgent(mockSSOToken)).willReturn(true);
        given(mockSpecialUserIdentity.isSpecialUser(mockSSOToken)).willReturn(false);
        given(mockService.isSuperUser("test")).willReturn(false);

        //when
        Promise<AuthorizationResult, ResourceException> result = testModule.authorize(mockSSOTokenContext);

        //then
        assertTrue(result.get().isAuthorized());

    }

    @Test
    public void shouldAuthorizeSpecialUser() throws Exception {

        //given
        SSOTokenContext mockSSOTokenContext = mock(SSOTokenContext.class);
        SSOToken mockSSOToken = mock(SSOToken.class);
        Principal principal = mock(Principal.class);
        given(mockSSOToken.getPrincipal()).willReturn(principal);

        given(mockSSOTokenContext.getCallerSSOToken()).willReturn(mockSSOToken);
        given(mockSSOToken.getProperty(Constants.UNIVERSAL_IDENTIFIER)).willReturn("test");
        given(mockAgentIdentity.isAgent(mockSSOToken)).willReturn(false);
        given(mockSpecialUserIdentity.isSpecialUser(mockSSOToken)).willReturn(true);
        given(mockService.isSuperUser("test")).willReturn(false);

        //when
        Promise<AuthorizationResult, ResourceException> result = testModule.authorize(mockSSOTokenContext);

        //then
        assertTrue(result.get().isAuthorized());

    }

    @Test
    public void shouldFailNonAgentNonSuperUser() throws Exception {

        //given
        SSOTokenContext mockSSOTokenContext = mock(SSOTokenContext.class);
        SSOToken mockSSOToken = mock(SSOToken.class);
        Principal principal = mock(Principal.class);
        given(mockSSOToken.getPrincipal()).willReturn(principal);

        given(mockSSOTokenContext.getCallerSSOToken()).willReturn(mockSSOToken);
        given(mockSSOToken.getProperty(Constants.UNIVERSAL_IDENTIFIER)).willReturn("test");
        given(mockAgentIdentity.isAgent(mockSSOToken)).willReturn(false);
        given(mockSpecialUserIdentity.isSpecialUser(mockSSOToken)).willReturn(false);
        given(mockService.isSuperUser("test")).willReturn(false);

        //when
        Promise<AuthorizationResult, ResourceException> result = testModule.authorize(mockSSOTokenContext);

        //then
        assertFalse(result.get().isAuthorized());

    }

    @Test
    public void shouldErrorInvalidContext() throws Exception {
        //given
        SSOTokenContext mockSSOTokenContext = mock(SSOTokenContext.class);
        SSOToken mockSSOToken = mock(SSOToken.class);
        Principal principal = mock(Principal.class);
        given(mockSSOToken.getPrincipal()).willReturn(principal);

        given(mockSSOTokenContext.getCallerSSOToken()).willReturn(mockSSOToken);
        given(mockSSOToken.getProperty(Constants.UNIVERSAL_IDENTIFIER)).willThrow(new SSOException(""));

        //when
        Promise< AuthorizationResult, ResourceException> result = testModule.authorize(mockSSOTokenContext);

        //then
        assertFalse(result.get().isAuthorized());
    }
}

