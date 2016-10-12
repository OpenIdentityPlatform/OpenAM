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
 * Copyright 2014-2015 ForgeRock AS.
 */

package org.forgerock.openam.core.rest.session;

import static org.forgerock.util.test.assertj.AssertJPromiseAssert.assertThat;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.mock;
import static org.testng.AssertJUnit.*;

import java.util.concurrent.ExecutionException;
import org.forgerock.authz.filter.api.AuthorizationResult;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ForbiddenException;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.services.context.Context;
import org.forgerock.services.context.RootContext;
import org.forgerock.util.promise.Promise;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

public class SessionResourceAuthzModuleTest {

    private SessionResourceAuthzModule testModule;

    @BeforeTest
    public void beforeTest() {
        testModule = new SessionResourceAuthzModule(null);
    }

    @Test
    public void shouldAllowLogoutAction() throws ExecutionException, InterruptedException {
        //given
        Context mockContext = mock(Context.class);
        ActionRequest mockRequest = mock(ActionRequest.class);

        given(mockRequest.getAction()).willReturn("logout");

        //when
        Promise<AuthorizationResult, ResourceException> result = testModule.authorizeAction(mockContext, mockRequest);

        //then
        assertTrue(result.get().isAuthorized());
    }

    @Test
    public void shouldAllowValidateAction() throws ExecutionException, InterruptedException {
        //given
        Context mockContext = mock(Context.class);
        ActionRequest mockRequest = mock(ActionRequest.class);

        given(mockRequest.getAction()).willReturn("validate");

        //when
        Promise<AuthorizationResult, ResourceException> result = testModule.authorizeAction(mockContext, mockRequest);

        //then
        assertTrue(result.get().isAuthorized());
    }

    @Test
    public void shouldDeferAllOthers() {
        //given
        RootContext rootContext = new RootContext();
        ActionRequest mockRequest = mock(ActionRequest.class);

        given(mockRequest.getAction()).willReturn("something else");

        //when
        Promise<AuthorizationResult, ResourceException> promise = testModule.authorizeAction(rootContext, mockRequest);

        //then
        assertThat(promise).failedWithException().isExactlyInstanceOf(ForbiddenException.class);
    }

}

