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
package org.forgerock.openam.core.rest.session;

public class TokenOwnerOrSuperUserAuthzModuleTest {
/**
    private TokenOwnerAuthzModule testModule;
    SessionService mockService;
    SSOTokenManager mockTokenManager;
    Context mockContext;
    SSOToken mockToken;

    @BeforeTest
    public void theSetUp() throws SSOException {
        mockService = mock(SessionService.class);
        mockTokenManager  = mock(SSOTokenManager.class);

        @SuppressWarnings("unchecked")
        Config<SessionService> mockConfig = mock(Config.class);
        given(mockConfig.get()).willReturn(mockService);

        SSOTokenContext tc = mock(SSOTokenContext.class);

        mockToken = mock(SSOToken.class);
        given(tc.getCallerSSOToken()).willReturn(mockToken);
        given(mockToken.getProperty(eq(Constants.UNIVERSAL_IDENTIFIER))).willReturn("universal_id");

        mockContext = mock(Context.class);
        given(mockContext.asContext(SSOTokenContext.class)).willReturn(tc);

        testModule = new TokenOwnerAuthzModule(mockConfig, mock(Debug.class), "tokenId",
                mockTokenManager, "deleteProperty");
    }

    @Test
    public void shouldSuperInvalidAction() throws SSOException, ExecutionException, InterruptedException {

        //given
        given(mockService.isSuperUser("universal_id")).willReturn(false);
        ActionRequest request = Requests.newActionRequest("resource", "invalid");

        //when
        Promise<AuthorizationResult, ResourceException> result = testModule.authorizeAction(mockContext, request);

        //then
        assertThat(result).succeeded();
        assertFalse(result.get().isAuthorized());
        assertEquals(result.get().getReason(), "User is not an administrator.");
    }

    @Test
    public void shouldAllowValidQueryParamToken() throws SSOException,
            ExecutionException, InterruptedException, BadRequestException {

        //given
        ActionRequest request = Requests.newActionRequest("resource", "deleteProperty");
        request.setAdditionalParameter("tokenId", "token");

        given(mockService.isSuperUser(eq("universal_id"))).willReturn(false);
        setupUsers("universal_id");

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
        setupUsers("john");

        //when
        Promise<AuthorizationResult, ResourceException> result = testModule.authorizeAction(mockContext, request);

        //then
        assertThat(result).succeeded();
        assertFalse(result.get().isAuthorized());
    }

    @Test
    public void shouldPassDifferentOwnerQueryParamAdminToken() throws SSOException,
            ExecutionException, InterruptedException, BadRequestException {

        //given
        ActionRequest request = Requests.newActionRequest("resource", "deleteProperty");
        request.setAdditionalParameter("tokenId", "token");
        given(mockService.isSuperUser(eq("universal_id"))).willReturn(true);
        setupUsers("john");

        //when
        Promise<AuthorizationResult, ResourceException> result = testModule.authorizeAction(mockContext, request);

        //then
        assertThat(result).succeeded();
        assertTrue(result.get().isAuthorized());
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

    private void setupUsers(String finalId) throws SSOException {
        Principal mockPrincipal = mock(Principal.class);

        given(mockTokenManager.createSSOToken(eq("token"))).willReturn(mockToken);
        given(mockToken.getPrincipal()).willReturn(mockPrincipal);
        given(mockPrincipal.getName()).willReturn(finalId);
    } **/
}
