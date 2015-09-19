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

package org.forgerock.openam.http;

import static org.forgerock.http.routing.RoutingMode.EQUALS;
import static org.forgerock.http.routing.RoutingMode.STARTS_WITH;
import static org.forgerock.openam.http.HttpRoute.newHttpRoute;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

import javax.inject.Inject;
import javax.inject.Named;
import java.net.URI;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.google.inject.Binder;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;
import org.forgerock.guice.core.GuiceTestCase;
import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.services.context.Context;
import org.forgerock.http.Handler;
import org.forgerock.services.context.RootContext;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.routing.Router;
import org.mockito.Mock;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class HttpRouterProviderTest extends GuiceTestCase {

    private final static String HTTP_ROUTE_ONE_NAME = "HttpRouteOne";
    private final static String HTTP_ROUTE_TWO_NAME = "HttpRouteTwo";
    private final static String ROUTE_ONE_URI_TEMPLATE = "ROUTE_ONE_URI_TEMPLATE";
    private final static String ROUTE_TWO_URI_TEMPLATE = "ROUTE_TWO_URI_TEMPLATE";

    private HttpRouterProvider httpRouterProvider;

    private Set<HttpRouteProvider> httpRouteProviders;
    private HttpRoute httpRouteOne;
    private HttpRoute httpRouteTwo;

    @Mock
    private Handler httpRouteOneHandler;
    @Mock
    private Handler httpRouteTwoHandler;

    @BeforeClass
    public void setupClass() {
        initMocks(this);
        httpRouteOne = newHttpRoute(STARTS_WITH, ROUTE_ONE_URI_TEMPLATE, httpRouteOneHandler);
        httpRouteTwo = newHttpRoute(EQUALS, ROUTE_TWO_URI_TEMPLATE, httpRouteTwoHandler);
        TestHttpRouteProviderOne httpRouteProviderOne = new TestHttpRouteProviderOne();
        TestHttpRouteProviderTwo httpRouteProviderTwo = new TestHttpRouteProviderTwo();
        httpRouteProviders = new HashSet<>();
        httpRouteProviders.add(httpRouteProviderOne);
        httpRouteProviders.add(httpRouteProviderTwo);
    }

    @Override
    public void configure(Binder binder) {
        binder.bind(Key.get(HttpRoute.class, Names.named(HTTP_ROUTE_ONE_NAME))).toInstance(httpRouteOne);
        binder.bind(Key.get(HttpRoute.class, Names.named(HTTP_ROUTE_TWO_NAME))).toInstance(httpRouteTwo);
        binder.bind(new TypeLiteral<Iterable<HttpRouteProvider>>() { }).toInstance(httpRouteProviders);
    }

    @BeforeMethod
    public void setup() {
        httpRouterProvider = InjectorHolder.getInstance(HttpRouterProvider.class);
    }

    @DataProvider
    private Object[][] routeRequestsData() {
        return new Object[][]{
            {ROUTE_ONE_URI_TEMPLATE + "/*", httpRouteOneHandler},
            {ROUTE_TWO_URI_TEMPLATE, httpRouteTwoHandler},
        };
    }

    @Test(dataProvider = "routeRequestsData")
    public void shouldAddRoutesToRouter(String requestUri, Handler exceptedHandler) {

        //Given
        Context context = new RootContext();
        Request request = new Request().setUri(URI.create(requestUri));

        Router router = httpRouterProvider.get();

        //When
        router.handle(context, request);

        //Then
        verify(exceptedHandler).handle(any(Context.class), eq(request));
    }

    public static class TestHttpRouteProviderOne implements HttpRouteProvider {

        private HttpRoute route;

        @Override
        public Set<HttpRoute> get() {
            return Collections.singleton(route);
        }

        @Inject
        public void setHttpRoute(@Named(HTTP_ROUTE_ONE_NAME) HttpRoute route) {
            this.route = route;
        }
    }

    public static class TestHttpRouteProviderTwo implements HttpRouteProvider {

        private HttpRoute route;

        @Override
        public Set<HttpRoute> get() {
            return Collections.singleton(route);
        }

        @Inject
        public void setHttpRoute(@Named(HTTP_ROUTE_TWO_NAME) HttpRoute route) {
            this.route = route;
        }
    }
}
