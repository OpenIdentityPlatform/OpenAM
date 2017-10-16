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

package org.forgerock.openam.core.rest.sms.tree;

import static org.forgerock.authz.filter.api.AuthorizationResult.*;
import static org.forgerock.json.JsonValue.*;
import static org.forgerock.json.resource.Responses.*;
import static org.forgerock.openam.forgerockrest.utils.MatchingResourcePath.resourcePath;
import static org.forgerock.util.promise.Promises.*;
import static org.forgerock.util.test.assertj.AssertJPromiseAssert.*;
import static org.forgerock.json.test.assertj.AssertJJsonValueAssert.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.util.Collections;
import java.util.Map;

import org.forgerock.authz.filter.api.AuthorizationResult;
import org.forgerock.authz.filter.crest.api.CrestAuthorizationModule;
import com.google.common.base.Predicate;
import org.forgerock.http.routing.RoutingMode;
import org.forgerock.http.routing.UriRouterContext;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.json.resource.NotSupportedException;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.Responses;
import org.forgerock.openam.forgerockrest.utils.MatchingResourcePath;
import org.forgerock.services.context.Context;
import org.forgerock.util.promise.Promise;
import org.mockito.ArgumentCaptor;
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
        Predicate<String> leafOneFunction = new Predicate<String>() {
            public boolean apply(String serviceName) {
                return "SERVICE_ONE".equals(serviceName);
            }
        };
        Predicate<String> leafTwoFunction = new Predicate<String>() {
            public boolean apply(String serviceName) {
                return "SERVICE_TWO".equals(serviceName);
            }
        };
        Predicate<String> leafThreeFunction = new Predicate<String>() {
            public boolean apply(String serviceName) {
                return "SERVICE_THREE".equals(serviceName);
            }
        };
        Predicate<String> leafFourFunction = new Predicate<String>() {
            public boolean apply(String serviceName) {
                return "NOT_AUTHORIZED".equals(serviceName);
            }
        };
        Predicate<String> leafFiveFunction = new Predicate<String>() {
            public boolean apply(String serviceName) {
                return serviceName.matches("OTHERSERVICE\\d");
            }
        };

        Map<MatchingResourcePath, CrestAuthorizationModule> authModules = Collections.singletonMap(
                resourcePath("/not-authorized/service"), authModule
        );
        routeTree = SmsRouteTreeBuilder.tree(authModules, defaultAuthModule,
                SmsRouteTreeBuilder.branch("branch1",
                        SmsRouteTreeBuilder.leaf("leaf1", leafOneFunction, false)),
                SmsRouteTreeBuilder.branch("branch2",
                        SmsRouteTreeBuilder.leaf("leaf2", leafTwoFunction, false),
                        SmsRouteTreeBuilder.leaf("leaf3", leafThreeFunction, false)),
                SmsRouteTreeBuilder.leaf("not-authorized", leafFourFunction, false),
                SmsRouteTreeBuilder.leaf("otherservices", leafFiveFunction, true));
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

    @Test
    public void shouldFindAllTypes() throws Exception {
        //Given
        RequestHandler handler1 = createTypeHandler(1, "one", false);
        RequestHandler handler2 = createTypeHandler(2, "two", false);
        RequestHandler handler3 = createTypeHandler(3, "three", true);

        Context context = new UriRouterContext(mock(Context.class), "", "/otherservices", Collections.<String, String>emptyMap());
        ActionRequest request = Requests.newActionRequest("/otherservices", "getAllTypes");

        Promise<AuthorizationResult, ResourceException> successResult = newResultPromise(accessPermitted());
        given(defaultAuthModule.authorizeAction(any(Context.class), any(ActionRequest.class))).willReturn(successResult);

        //When
        Promise<ActionResponse, ResourceException> result = routeTree.handleAction(context, request);

        //Then
        assertThat(result).succeeded();
        assertThat(result.getOrThrow().getJsonContent()).hasArray("result").hasSize(3).containsOnly(
                object(field("_id", "service1"), field("name", "one"), field("collection", false)),
                object(field("_id", "service2"), field("name", "two"), field("collection", false)),
                object(field("_id", "service3"), field("name", "three"), field("collection", true))
        );
        verifyGetTypeAction(handler1);
        verifyGetTypeAction(handler2);
        verifyGetTypeAction(handler3);
    }

    @Test
    public void shouldFindCreatableTypes() throws Exception {
        //Given
        RequestHandler handler1 = createTypeHandler(1, "one", false);
        RequestHandler handler2 = createTypeHandler(2, "two", false);
        RequestHandler handler3 = createTypeHandler(3, "three", true);

        Promise<ResourceResponse, ResourceException> exceptionPromise = new NotFoundException().asPromise();
        when(handler1.handleRead(any(Context.class), any(ReadRequest.class))).thenReturn(exceptionPromise);
        when(handler2.handleRead(any(Context.class), any(ReadRequest.class)))
                .thenReturn(Responses.newResourceResponse("1", "1", json(object())).asPromise());

        Context context = new UriRouterContext(mock(Context.class), "", "/otherservices", Collections.<String, String>emptyMap());
        ActionRequest request = Requests.newActionRequest("/otherservices", "getCreatableTypes");

        Promise<AuthorizationResult, ResourceException> successResult = newResultPromise(accessPermitted());
        given(defaultAuthModule.authorizeAction(any(Context.class), any(ActionRequest.class))).willReturn(successResult);
        given(defaultAuthModule.authorizeRead(any(Context.class), any(ReadRequest.class))).willReturn(successResult);

        //When
        Promise<ActionResponse, ResourceException> result = routeTree.handleAction(context, request);

        //Then
        assertThat(result).succeeded();
        verifyGetTypeAction(handler1);
        verifyGetTypeAction(handler2);
        verifyGetTypeAction(handler3);
        verify(handler1).handleRead(any(Context.class), any(ReadRequest.class));
        verify(handler2).handleRead(any(Context.class), any(ReadRequest.class));
        verifyNoMoreInteractions(handler3);
        assertThat(result.getOrThrow().getJsonContent()).hasArray("result").hasSize(2).containsOnly(
                object(field("_id", "service1"), field("name", "one"), field("collection", false)),
                object(field("_id", "service3"), field("name", "three"), field("collection", true))
        );
    }

    @Test
    public void shouldNotSupportGeneralActionsOnUnsupportedLeaves() throws Exception {
        RequestHandler handler1 = createTypeHandler(1, "one", false, "SERVICE_ONE");

        Context context = new UriRouterContext(mock(Context.class), "", "/branch1/leaf1", Collections.<String, String>emptyMap());
        ActionRequest request = Requests.newActionRequest("/branch1/leaf1", "getCreatableTypes");

        Promise<AuthorizationResult, ResourceException> successResult = newResultPromise(accessPermitted());
        given(defaultAuthModule.authorizeAction(any(Context.class), any(ActionRequest.class))).willReturn(successResult);
        given(defaultAuthModule.authorizeRead(any(Context.class), any(ReadRequest.class))).willReturn(successResult);

        //When
        Promise<ActionResponse, ResourceException> result = routeTree.handleAction(context, request);

        //Then
        assertThat(result).failedWithException().isInstanceOf(NotSupportedException.class);
        verifyNoMoreInteractions(handler1);
    }

    private void verifyGetTypeAction(RequestHandler handler) {
        ArgumentCaptor<ActionRequest> captor = ArgumentCaptor.forClass(ActionRequest.class);
        verify(handler).handleAction(any(Context.class), captor.capture());
        assertThat(captor.getValue().getAction()).isEqualTo("getType");
    }

    private RequestHandler createTypeHandler(int id, String name, boolean collection) {
        return createTypeHandler(id, name, collection, "OTHERSERVICE" + id);
    }

    private RequestHandler createTypeHandler(int id, String name, boolean collection, String serviceName) {
        RequestHandler handler = mock(RequestHandler.class);
        given(handler.handleAction(any(Context.class), any(ActionRequest.class)))
                .willReturn(
                        newActionResponse(json(object(
                                field("_id", "service" + id),
                                field("collection", collection),
                                field("name", name))))
                                .asPromise());
        routeTree.handles(serviceName).addRoute(RoutingMode.STARTS_WITH, "/service" + id, handler);
        return handler;
    }
}
