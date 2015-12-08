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

package org.forgerock.openam.core.rest.sms;

import static org.forgerock.authz.filter.api.AuthorizationResult.*;
import static org.forgerock.json.JsonValue.*;
import static org.forgerock.json.resource.Responses.*;
import static org.forgerock.openam.core.rest.sms.SmsRouteTree.*;
import static org.forgerock.openam.forgerockrest.utils.MatchingResourcePath.resourcePath;
import static org.forgerock.util.promise.Promises.*;
import static org.forgerock.util.test.assertj.AssertJPromiseAssert.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.util.Collections;
import java.util.Map;

import org.forgerock.authz.filter.api.AuthorizationResult;
import org.forgerock.authz.filter.crest.api.CrestAuthorizationModule;
import org.forgerock.guava.common.base.Function;
import org.forgerock.http.routing.RoutingMode;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourcePath;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.openam.forgerockrest.utils.MatchingResourcePath;
import org.forgerock.services.context.Context;
import org.forgerock.util.promise.Promise;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class SmsRouteTreeTest {

    private SmsRouteTree routeTree;
    @Mock
    private CrestAuthorizationModule authModule;
    @Mock
    private CrestAuthorizationModule defaultAuthModule;

    @BeforeMethod
    public void setup() throws Exception {
        MockitoAnnotations.initMocks(this);
        Function<String, Boolean> leafOneFunction = new Function<String, Boolean>() {
            @Override
            public Boolean apply(String serviceName) {
                return "SERVICE_ONE".equals(serviceName);
            }
        };
        Function<String, Boolean> leafTwoFunction = new Function<String, Boolean>() {
            @Override
            public Boolean apply(String serviceName) {
                return "SERVICE_TWO".equals(serviceName);
            }
        };
        Function<String, Boolean> leafThreeFunction = new Function<String, Boolean>() {
            @Override
            public Boolean apply(String serviceName) {
                return "SERVICE_THREE".equals(serviceName);
            }
        };
        Function<String, Boolean> leafFourFunction = new Function<String, Boolean>() {
            @Override
            public Boolean apply(String serviceName) {
                return "NOT_AUTHORIZED".equals(serviceName);
            }
        };

        Map<MatchingResourcePath, CrestAuthorizationModule> authModules = Collections.singletonMap(
                resourcePath("/not-authorized/service"), authModule
        );
        routeTree = tree(authModules, defaultAuthModule,
                branch("branch1",
                        leaf("leaf1", leafOneFunction)),
                branch("branch2",
                        leaf("leaf2", leafTwoFunction),
                        leaf("leaf3", leafThreeFunction)),
                leaf("not-authorized", leafFourFunction));
    }

    @DataProvider(name = "handleRoutes")
    private Object[][] getHandleRoutesData() {
        return new Object[][]{
            {"SERVICE_ONE", "/branch1/leaf1"},
            {"SERVICE_TWO", "/branch2/leaf2"},
            {"SERVICE_THREE", "/branch2/leaf3"},
            {"OTHER_SERVICE", ""},
        };
    }

    @Test(dataProvider = "handleRoutes")
    public void shouldHandleAddingRoutes(String serviceName, String resourcePath) {

        //Given
        RequestHandler requestHandler = mock(RequestHandler.class);
        Context context = mock(Context.class);
        ReadRequest request = Requests.newReadRequest(resourcePath + "/handler");
        Promise<AuthorizationResult, ResourceException> successResult = newResultPromise(accessPermitted());
        given(defaultAuthModule.authorizeRead(any(Context.class), any(ReadRequest.class))).willReturn(successResult);

        //When
        SmsRouteTree handlerTree = routeTree.handles(serviceName);
        handlerTree.addRoute(RoutingMode.STARTS_WITH, "/handler", requestHandler);
        routeTree.handleRead(context, request);

        //Then
        verify(requestHandler).handleRead(any(Context.class), any(ReadRequest.class));
    }

    @Test
    public void shouldUseProvidedAuthModuleForMatchingPath() throws Exception {
        //Given
        RequestHandler requestHandler = mock(RequestHandler.class);
        Context context = mock(Context.class);
        ReadRequest request = Requests.newReadRequest("/not-authorized/service");
        Promise<AuthorizationResult, ResourceException> failResult = newResultPromise(accessDenied("no"));
        given(authModule.authorizeRead(any(Context.class), any(ReadRequest.class))).willReturn(failResult);

        //When
        routeTree.handles("NOT_AUTHORIZED").addRoute(RoutingMode.STARTS_WITH, "/service", requestHandler);
        Promise<ResourceResponse, ResourceException> result = routeTree.handleRead(context, request);

        //Then
        assertThat(result).failedWithException();
        verify(authModule).authorizeRead(any(Context.class), any(ReadRequest.class));
        verifyNoMoreInteractions(requestHandler, defaultAuthModule);
    }

    @Test
    public void shouldUseOtherAuthModuleForMatchingPath() throws Exception {
        //Given
        RequestHandler requestHandler = mock(RequestHandler.class);
        given(requestHandler.handleRead(any(Context.class), any(ReadRequest.class)))
                .willReturn(newResourceResponse("id", "1", json(object())).asPromise());

        Promise<AuthorizationResult, ResourceException> successResult = newResultPromise(accessPermitted());
        given(defaultAuthModule.authorizeRead(any(Context.class), any(ReadRequest.class))).willReturn(successResult);

        Context context = mock(Context.class);
        ReadRequest request = Requests.newReadRequest("/service");

        //When
        routeTree.handles("OTHERSERVICE").addRoute(RoutingMode.STARTS_WITH, "/service", requestHandler);
        Promise<ResourceResponse, ResourceException> result = routeTree.handleRead(context, request);

        //Then
        assertThat(result).succeeded();
        verify(defaultAuthModule).authorizeRead(any(Context.class), any(ReadRequest.class));
        verifyNoMoreInteractions(authModule);
    }
}
