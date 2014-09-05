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
* Copyright 2014 ForgeRock AS.
*/
package org.forgerock.openam.rest.authz;

import com.iplanet.dpro.session.service.SessionService;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import org.forgerock.authz.filter.api.AuthorizationResult;
import org.forgerock.json.resource.Request;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.openam.utils.Config;
import org.forgerock.util.promise.Promise;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.testng.Assert.assertTrue;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

public class SessionResourceAuthzModuleTest {

    Config<SessionService> mockConfig = mock(Config.class);
    SessionService mockService = mock(SessionService.class);

    public SessionResourceAuthzModule testModule = new SessionResourceAuthzModule(mockConfig);

    @BeforeTest
    public void beforeTest() {
        given(mockConfig.get()).willReturn(mockService);
    }

    @Test
    public void shouldAllowLogoutAction() throws ExecutionException, InterruptedException {
        //given
        ServerContext mockContext = mock(ServerContext.class);
        Request mockRequest = mock(Request.class);
        Map<String, String> parameterMap = new HashMap<String, String>();
        parameterMap.put("_action", "logout");

        given(mockRequest.getAdditionalParameters()).willReturn(parameterMap);

        //when
        Promise<AuthorizationResult, ResourceException> result = testModule.authorize(mockRequest, mockContext);

        //then
        assertTrue(result.get().isAuthorized());

    }

    @Test
    public void shouldAllowValidateAction() throws ExecutionException, InterruptedException {
        //given
        ServerContext mockContext = mock(ServerContext.class);
        Request mockRequest = mock(Request.class);
        Map<String, String> parameterMap = new HashMap<String, String>();
        parameterMap.put("_action", "validate");

        given(mockRequest.getAdditionalParameters()).willReturn(parameterMap);

        //when
        Promise<AuthorizationResult, ResourceException> result = testModule.authorize(mockRequest, mockContext);

        //then
        assertTrue(result.get().isAuthorized());
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void shouldDeferAllOthers() {
        //given
        ServerContext mockContext = mock(ServerContext.class);
        Request mockRequest = mock(Request.class);
        Map<String, String> parameterMap = new HashMap<String, String>();
        parameterMap.put("_action", "notValidateOrLogout");

        given(mockRequest.getAdditionalParameters()).willReturn(parameterMap);

        //when
        testModule.authorize(mockRequest, mockContext);

        //then
        // we should catch an IllegalArgumentException as we pass into super.authorize, as we have no SSOTokenContext

    }

}

