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

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.authz.filter.api.AuthorizationResult;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openam.forgerockrest.utils.SpecialUserIdentity;
import org.forgerock.openam.rest.resource.SSOTokenContext;
import org.forgerock.util.promise.Promise;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.security.Principal;
import java.util.concurrent.ExecutionException;

public class SpecialUserOnlyAuthzModuleTest {
    @Mock
    private SpecialUserIdentity mockSpecialUserIdentity;
    @Mock
    private SSOTokenContext mockSSOTokenContext;
    @Mock
    private SSOToken mockSSOToken;
    @Mock
    private Principal mockPrincipal;
    private SpecialUserOnlyAuthzModule authzModule;

    @BeforeTest
    public void setup() throws SSOException {
        MockitoAnnotations.initMocks(this);
        given(mockSSOTokenContext.getCallerSSOToken()).willReturn(mockSSOToken);
        given(mockSSOToken.getPrincipal()).willReturn(mockPrincipal);
        given(mockPrincipal.getName()).willReturn("irrelevant");
        authzModule = new SpecialUserOnlyAuthzModule(mockSpecialUserIdentity, mock(Debug.class));
    }

    @Test
    public void specialUserAuthz() throws ExecutionException, InterruptedException {
        //given
        given(mockSpecialUserIdentity.isSpecialUser(mockSSOToken)).willReturn(true);
        //when
        Promise<AuthorizationResult, ResourceException> result = authzModule.authorize(mockSSOTokenContext);
        //then
        assertTrue(result.get().isAuthorized());
    }

    @Test
    public void specialUserDenied() throws ExecutionException, InterruptedException {
        //given
        given(mockSpecialUserIdentity.isSpecialUser(mockSSOToken)).willReturn(false);
        //when
        Promise<AuthorizationResult, ResourceException> result = authzModule.authorize(mockSSOTokenContext);
        //then
        assertFalse(result.get().isAuthorized());
    }
}
