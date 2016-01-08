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
* Copyright 2015-2016 ForgeRock AS.
*/
package org.forgerock.openam.core.rest.session;

import static org.forgerock.util.test.assertj.AssertJPromiseAssert.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.testng.AssertJUnit.*;

import com.iplanet.dpro.session.service.SessionService;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.shared.Constants;
import java.security.Principal;
import java.util.concurrent.ExecutionException;
import org.forgerock.authz.filter.api.AuthorizationResult;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.ForbiddenException;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openam.rest.resource.SSOTokenContext;
import org.forgerock.openam.utils.Config;
import org.forgerock.services.context.Context;
import org.forgerock.util.promise.Promise;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

public class TokenOwnerAuthzModuleTest {

    private TokenOwnerAuthzModule testModule;
    SessionService mockService;
    SSOTokenManager mockTokenManager;
    Context mockContext;

    @BeforeTest
    public void theSetUp() throws SSOException {
        mockService = mock(SessionService.class);
        mockTokenManager  = mock(SSOTokenManager.class);

        @SuppressWarnings("unchecked")
        Config<SessionService> mockConfig = mock(Config.class);
        given(mockConfig.get()).willReturn(mockService);
        mockContext = setupUser("universal_id");

        testModule = new TokenOwnerAuthzModule("tokenId", mockTokenManager, "deleteProperty");
    }

    @Test
    public void shouldAllowValidQueryParamToken() throws SSOException,
            ExecutionException, InterruptedException, BadRequestException {

        //given
        ActionRequest request = Requests.newActionRequest("resource", "deleteProperty");
        request.setAdditionalParameter("tokenId", "token");

        given(mockService.isSuperUser(eq("universal_id"))).willReturn(false);

        //when
        Promise<AuthorizationResult, ResourceException> result = testModule.authorizeAction(mockContext, request);

        //then
        assertThat(result).succeeded();
        assertTrue(result.get().isAuthorized());
    }

    @Test
    public void shouldFailDifferentOwnerQueryParamToken() throws SSOException,
            ExecutionException, InterruptedException, BadRequestException {

        //given
        ActionRequest request = Requests.newActionRequest("resource", "deleteProperty");
        request.setAdditionalParameter("tokenId", "token");

        given(mockService.isSuperUser(eq("universal_id"))).willReturn(false);
        given(mockService.isSuperUser(eq("john"))).willReturn(false);
        Context otherContext = setupUser("john");
        setupUser("universal_id");

        //when
        Promise<AuthorizationResult, ResourceException> result = testModule.authorizeAction(otherContext, request);

        //then
        assertThat(result).failedWithException().isInstanceOf(ForbiddenException.class);
    }

    @Test
    public void shouldFailInvalidQueryParamToken() throws SSOException,
            ExecutionException, InterruptedException, BadRequestException {

        //given
        ActionRequest request = Requests.newActionRequest("resource", "deleteProperty");
        request.setAdditionalParameter("tokenId", "token");

        given(mockService.isSuperUser(eq("universal_id"))).willReturn(false);
        given(mockTokenManager.createSSOToken(eq("token"))).willThrow(new SSOException(""));

        //when
        Promise<AuthorizationResult, ResourceException> result = testModule.authorizeAction(mockContext, request);

        //then
        assertThat(result).failedWithException().isInstanceOf(ForbiddenException.class);
    }

    private Context setupUser(String finalId) throws SSOException {
        Principal mockPrincipal = mock(Principal.class);

        SSOTokenContext tc = mock(SSOTokenContext.class);

        Context mockContext = mock(Context.class);
        given(mockContext.asContext(SSOTokenContext.class)).willReturn(tc);

        SSOToken mockToken = mock(SSOToken.class);
        given(tc.getCallerSSOToken()).willReturn(mockToken);
        given(mockToken.getProperty(eq(Constants.UNIVERSAL_IDENTIFIER))).willReturn("uid" + finalId);

        given(mockTokenManager.createSSOToken(eq("token"))).willReturn(mockToken);
        given(mockToken.getPrincipal()).willReturn(mockPrincipal);
        given(mockPrincipal.getName()).willReturn(finalId);

        return mockContext;
    }

    @Test
    public void testCreateIsForbidden() throws Exception{
        //given

        //when
        Promise<AuthorizationResult, ResourceException> promise = testModule.authorizeCreate(mockContext, mock(CreateRequest.class));

        //then
        assertThat(promise).failedWithException().isExactlyInstanceOf(ForbiddenException.class);
    }

    @Test
    public void testQueryIsForbidden() {
        //given

        //when
        Promise<AuthorizationResult, ResourceException> promise = testModule.authorizeQuery(mockContext, mock(QueryRequest.class));

        //then
        assertThat(promise).failedWithException().isExactlyInstanceOf(ForbiddenException.class);
    }

    @Test
    public void testReadIsForbidden() {
        //given

        //when
        Promise<AuthorizationResult, ResourceException> promise = testModule.authorizeQuery(mockContext, mock(QueryRequest.class));

        //then
        assertThat(promise).failedWithException().isExactlyInstanceOf(ForbiddenException.class);

    }

    @Test
    public void testUpdateIsForbidden() {
        //given

        //when
        Promise<AuthorizationResult, ResourceException> promise = testModule.authorizeUpdate(mockContext, mock(UpdateRequest.class));

        //then
        assertThat(promise).failedWithException().isExactlyInstanceOf(ForbiddenException.class);

    }

    @Test
    public void testPatchIsForbidden() {
        //given

        //when
        Promise<AuthorizationResult, ResourceException> promise = testModule.authorizePatch(mockContext, mock(PatchRequest.class));

        //then
        assertThat(promise).failedWithException().isExactlyInstanceOf(ForbiddenException.class);

    }

    @Test
    public void testDeleteIsForbidden() {
        //given

        //when
        Promise<AuthorizationResult, ResourceException> promise = testModule.authorizeDelete(mockContext, mock(DeleteRequest.class));

        //then
        assertThat(promise).failedWithException().isExactlyInstanceOf(ForbiddenException.class);

    }
}
