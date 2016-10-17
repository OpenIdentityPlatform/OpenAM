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
 * Copyright 2016 ForgeRock AS.
 */

package org.forgerock.openam.core.rest.session;

import static org.forgerock.openam.core.rest.session.SessionPropertiesResource.TOKEN_HASH_PARAM_NAME;
import static org.forgerock.util.test.assertj.AssertJPromiseAssert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.testng.AssertJUnit.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.shared.Constants;
import org.forgerock.authz.filter.api.AuthorizationResult;
import org.forgerock.http.routing.UriRouterContext;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.ForbiddenException;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourcePath;
import org.forgerock.openam.rest.resource.SSOTokenContext;
import org.forgerock.services.context.Context;
import org.forgerock.util.promise.Promise;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

public class SessionPropertiesResourceAuthzModuleTest {


    private SessionPropertiesResourceAuthzModule module;
    private Context context;
    private SSOTokenManager ssoTokenManager;
    private SSOToken ssoToken;
    private SSOToken userSsoToken;
    private SSOTokenContext ssoTokenContext;
    private UriRouterContext uriRouterContext;
    private Map<String, String> templateVariables;
    private TokenHashToIDMapper hashToIdMapper;

    @BeforeTest
    public void beforeTest() {
        context = mock(Context.class);
        ssoTokenManager = mock(SSOTokenManager.class);
        ssoToken = mock(SSOToken.class);
        userSsoToken = mock(SSOToken.class);
        ssoTokenContext = mock(SSOTokenContext.class);
        hashToIdMapper = mock(TokenHashToIDMapper.class);
        module = new SessionPropertiesResourceAuthzModule(ssoTokenManager, hashToIdMapper);
        templateVariables = new HashMap<>();
        templateVariables.put(TOKEN_HASH_PARAM_NAME, "tokenId");
        uriRouterContext = new UriRouterContext(context, "" + "", "", templateVariables);
    }


    @Test
    public void shouldAllowWhenTokenOwner() throws SSOException, ExecutionException, InterruptedException {
        //given
        given(context.asContext(SSOTokenContext.class)).willReturn(ssoTokenContext);
        given(ssoTokenContext.getCallerSSOToken()).willReturn(ssoToken);
        given(ssoToken.getProperty(Constants.UNIVERSAL_IDENTIFIER)).willReturn("loggedInUser");
        given(context.asContext(UriRouterContext.class)).willReturn(uriRouterContext);
        given(ssoTokenManager.createSSOToken(templateVariables.get(TOKEN_HASH_PARAM_NAME))).willReturn(userSsoToken);
        given(userSsoToken.getProperty(Constants.UNIVERSAL_IDENTIFIER)).willReturn("loggedInUser");
        ReadRequest request = mock(ReadRequest.class);
        given(request.getResourcePathObject()).willReturn(new ResourcePath());
        given(hashToIdMapper.map(context, "tokenId")).willReturn("tokenId");

        //when
        Promise<AuthorizationResult, ResourceException> promise = module.authorizeRead(context, request);

        //then
        assertTrue(promise.get().isAuthorized());
    }


    @Test
    public void shouldDenyWhenNotTokenOwner() throws SSOException {
        //given
        given(context.asContext(SSOTokenContext.class)).willReturn(ssoTokenContext);
        given(ssoTokenContext.getCallerSSOToken()).willReturn(ssoToken);
        given(ssoToken.getProperty(Constants.UNIVERSAL_IDENTIFIER)).willReturn("loggedInUser");
        given(context.asContext(UriRouterContext.class)).willReturn(uriRouterContext);
        given(ssoTokenManager.createSSOToken(templateVariables.get(TOKEN_HASH_PARAM_NAME))).willReturn(userSsoToken);
        given(userSsoToken.getProperty(Constants.UNIVERSAL_IDENTIFIER)).willReturn("anotherUser");
        ReadRequest request = mock(ReadRequest.class);
        given(request.getResourcePathObject()).willReturn(new ResourcePath());

        //when
        Promise<AuthorizationResult, ResourceException> promise = module.authorizeRead(context, request);

        //then
        assertThat(promise).failedWithException().isExactlyInstanceOf(ForbiddenException.class);
    }


    @Test
    public void testDeleteIsForbidden() {
        //when
        Promise<AuthorizationResult, ResourceException> promise = module.authorizeDelete(context, mock(DeleteRequest.class));

        //then
        assertThat(promise).failedWithException().isExactlyInstanceOf(ForbiddenException.class);
    }

    @Test
    public void testActionIsForbidden() {
        //when
        Promise<AuthorizationResult, ResourceException> promise = module.authorizeAction(context, mock(ActionRequest.class));

        //then
        assertThat(promise).failedWithException().isExactlyInstanceOf(ForbiddenException.class);
    }

    @Test
    public void testQueryIsForbidden() {
        //when
        Promise<AuthorizationResult, ResourceException> promise = module.authorizeQuery(context, mock(QueryRequest.class));

        //then
        assertThat(promise).failedWithException().isExactlyInstanceOf(ForbiddenException.class);
    }

    @Test
    public void testCreateIsForbidden() {
        //when
        Promise<AuthorizationResult, ResourceException> promise = module.authorizeCreate(context, mock(CreateRequest.class));

        //then
        assertThat(promise).failedWithException().isExactlyInstanceOf(ForbiddenException.class);
    }
}
