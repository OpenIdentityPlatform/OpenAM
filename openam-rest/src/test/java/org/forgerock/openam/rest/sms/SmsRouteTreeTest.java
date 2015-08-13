///*
// * The contents of this file are subject to the terms of the Common Development and
// * Distribution License (the License). You may not use this file except in compliance with the
// * License.
// *
// * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
// * specific language governing permission and limitations under the License.
// *
// * When distributing Covered Software, include this CDDL Header Notice in each file and include
// * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
// * Header, with the fields enclosed by brackets [] replaced by your own identifying
// * information: "Portions copyright [year] [name of copyright owner]".
// *
// * Copyright 2015 ForgeRock AS.
// */
//
//package org.forgerock.openam.rest.sms;
//
//import static org.forgerock.openam.rest.sms.SmsRouteTree.*;
//import static org.mockito.Mockito.mock;
//import static org.mockito.Mockito.verify;
//
//import org.forgerock.guava.common.base.Function;
//import org.forgerock.http.Context;
//import org.forgerock.json.resource.ReadRequest;
//import org.forgerock.json.resource.RequestHandler;
//import org.forgerock.json.resource.Requests;
//import org.forgerock.json.resource.ResourceResponse;
//import org.forgerock.json.resource.Router;
//import org.mockito.Matchers;
//import org.testng.annotations.BeforeClass;
//import org.testng.annotations.DataProvider;
//import org.testng.annotations.Test;
//
//public class SmsRouteTreeTest {
//
//    private Router router;
//
//    @BeforeClass
//    public void setup() {
//        router = new Router();
//    }
//
//    @DataProvider(name = "handleRoutes")
//    private Object[][] getHandleRoutesData() {
//        Function<String, Boolean> leafOneFunction = new Function<String, Boolean>() {
//            @Override
//            public Boolean apply(String serviceName) {
//                return "SERVICE_ONE".equals(serviceName);
//            }
//        };
//        Function<String, Boolean> leafTwoFunction = new Function<String, Boolean>() {
//            @Override
//            public Boolean apply(String serviceName) {
//                return "SERVICE_TWO".equals(serviceName);
//            }
//        };
//        Function<String, Boolean> leafThreeFunction = new Function<String, Boolean>() {
//            @Override
//            public Boolean apply(String serviceName) {
//                return "SERVICE_THREE".equals(serviceName);
//            }
//        };
//        SmsRouteTree routeTree = tree(
//                branch("branch1",
//                        leaf("leaf1", leafOneFunction)),
//                branch("branch2",
//                        leaf("leaf2", leafTwoFunction),
//                        leaf("leaf3", leafThreeFunction)));
//        return new Object[][]{
//            {routeTree, "SERVICE_ONE", "/branch1/leaf1"},
//            {routeTree, "SERVICE_TWO", "/branch2/leaf2"},
//            {routeTree, "SERVICE_THREE", "/branch2/leaf3"},
//            {routeTree, "OTHER_SERVICE", ""},
//        };
//    }
//
//    @Test(dataProvider = "handleRoutes")
//    public void shouldHandleAddingRoutes(SmsRouteTree routeTree, String serviceName, String resourcePath) {
//
//        //Given
//        RequestHandler requestHandler = mock(RequestHandler.class);
//        Context context = mock(Context.class);
//        ReadRequest request = Requests.newReadRequest(resourcePath + "/handler");
//        ResultHandler<ResourceResponse> resultHandler = mock(ResultHandler.class);
//
//        //When
//        routeTree.handles(serviceName).addRoute(RoutingMode.STARTS_WITH, "/handler", requestHandler);
//        routeTree.handleRead(context, request, resultHandler);
//
//        //Then
//        verify(requestHandler).handleRead(Matchers.<Context>anyObject(), Matchers.<ReadRequest>anyObject(),
//                Matchers.<ResultHandler>anyObject());
//    }
//}
