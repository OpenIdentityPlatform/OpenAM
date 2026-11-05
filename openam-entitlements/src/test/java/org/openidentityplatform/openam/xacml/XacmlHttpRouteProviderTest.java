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
 * Copyright 2026 3A Systems, LLC.
 */

package org.openidentityplatform.openam.xacml;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.http.routing.RouteMatchers.requestUriMatcher;
import static org.forgerock.http.routing.RoutingMode.STARTS_WITH;
import static org.forgerock.util.promise.Promises.newResultPromise;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import org.forgerock.http.Filter;
import org.forgerock.http.Handler;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.http.protocol.Status;
import org.forgerock.http.routing.DefaultVersionBehaviour;
import org.forgerock.http.routing.ResourceApiVersionBehaviourManager;
import org.forgerock.http.routing.RouteMatchers;
import org.forgerock.http.routing.Router;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.core.realms.RealmTestHelper;
import org.forgerock.openam.http.HttpRoute;
import org.forgerock.openam.http.authz.HttpContextFilter;
import org.forgerock.openam.rest.RealmContext;
import org.forgerock.openam.rest.RealmContextFilter;
import org.forgerock.openam.rest.RealmRoutingFactory;
import org.forgerock.openam.xacml.v3.rest.XacmlService;
import org.forgerock.services.context.Context;
import org.forgerock.services.context.RootContext;
import org.forgerock.util.promise.NeverThrowsException;
import org.forgerock.util.promise.Promise;
import org.mockito.ArgumentCaptor;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class XacmlHttpRouteProviderTest {

    private RealmTestHelper realmTestHelper;
    private RealmContextFilter realmContextFilter;
    private HttpContextFilter httpContextFilter;
    private Filter authenticationFilter;
    private Handler endpoint;
    private Realm legacyRealm;
    private XacmlHttpRouteProvider provider;

    /** Authentication stand-in that only calls the next handler. */
    public static class PassThroughFilter implements Filter {
        @Override
        public Promise<Response, NeverThrowsException> filter(Context context, Request request, Handler next) {
            return next.handle(context, request);
        }
    }

    @BeforeMethod
    public void setUp() throws Exception {
        realmTestHelper = new RealmTestHelper();
        realmTestHelper.setupRealmClass();
        realmTestHelper.mockDnsAlias("openam.example.org");            // the host resolves to the root realm
        legacyRealm = realmTestHelper.mockRealm("legacy");

        // Stands in for the real filter: no-op when a RealmContext exists, else resolves the legacy realm.
        realmContextFilter = mock(RealmContextFilter.class);
        given(realmContextFilter.filter(any(), any(), any())).willAnswer(invocation -> {
            Context context = invocation.getArgument(0);
            Handler next = invocation.getArgument(2);
            Context resolved = context.containsContext(RealmContext.class)
                    ? context : new RealmContext(context, legacyRealm);
            return next.handle(resolved, invocation.getArgument(1));
        });
        httpContextFilter = mock(HttpContextFilter.class);
        given(httpContextFilter.filter(any(), any(), any())).willAnswer(invocation ->
                ((Handler) invocation.getArgument(2)).handle(invocation.getArgument(0), invocation.getArgument(1)));
        authenticationFilter = spy(new PassThroughFilter());
        endpoint = mock(Handler.class);
        given(endpoint.handle(any(), any())).willReturn(newResultPromise(new Response(Status.OK)));

        ResourceApiVersionBehaviourManager behaviourManager = RouteMatchers.newResourceApiVersionBehaviourManager();
        behaviourManager.setDefaultVersionBehaviour(DefaultVersionBehaviour.LATEST);
        provider = new XacmlHttpRouteProvider();
        provider.setRealmRoutingFactory(new RealmRoutingFactory());
        provider.setRealmContextFilter(realmContextFilter);
        provider.setHttpContextFilter(httpContextFilter);
        provider.setAuthenticationFilter(authenticationFilter);
        provider.setResourceApiVersionFilter(RouteMatchers.resourceApiVersionContextFilter(behaviourManager));
        provider.setInvalidRealmNames(new HashSet<String>());
    }

    @AfterMethod
    public void tearDown() {
        realmTestHelper.tearDownRealmClass();
    }

    /** Mounts the handler at "xacml" the way HttpRouterProvider does. */
    private Response call(String method, String path, String acceptApiVersion) throws Exception {
        Router top = new Router();
        top.addRoute(requestUriMatcher(STARTS_WITH, "xacml"), provider.createHandler(endpoint));
        Request request = new Request().setMethod(method).setUri("http://openam.example.org:8080" + path);
        if (acceptApiVersion != null) {
            request.getHeaders().put("Accept-API-Version", acceptApiVersion);
        }
        return top.handle(new RootContext(), request).getOrThrowUninterruptibly();
    }

    private Realm realmSeenByEndpoint() throws Exception {
        ArgumentCaptor<Context> context = ArgumentCaptor.forClass(Context.class);
        verify(endpoint).handle(context.capture(), any());
        return context.getValue().asContext(RealmContext.class).getRealm();
    }

    @Test
    public void legacyPathGoesThroughRealmContextFilter() throws Exception {
        Response response = call("GET", "/xacml/policies", null);

        assertThat(response.getStatus()).isEqualTo(Status.OK);
        assertThat(realmSeenByEndpoint()).isSameAs(legacyRealm);
        verify(realmContextFilter, times(1)).filter(any(), any(), any());
    }

    @Test
    public void realmsRootSpellingResolvesTheRootRealm() throws Exception {
        Response response = call("GET", "/xacml/realms/root/policies", null);

        assertThat(response.getStatus()).isEqualTo(Status.OK);
        assertThat(realmSeenByEndpoint()).isEqualTo(Realm.root());
    }

    @Test
    public void nestedRealmsSpellingResolvesTheSubRealm() throws Exception {
        Realm sub = realmTestHelper.mockRealm("sub");

        Response response = call("GET", "/xacml/realms/root/realms/sub/policies", null);

        assertThat(response.getStatus()).isEqualTo(Status.OK);
        assertThat(realmSeenByEndpoint()).isEqualTo(sub);
    }

    @Test
    public void contentApiVersionCarriesProtocol1() throws Exception {
        Response response = call("GET", "/xacml/policies", null);

        assertThat(response.getHeaders().getFirst("Content-API-Version")).isEqualTo("protocol=1.0,resource=1.0");
    }

    @Test
    public void unknownResourceVersionAnswers404() throws Exception {
        Response response = call("GET", "/xacml/policies", "resource=2.0");

        assertThat(response.getStatus()).isEqualTo(Status.NOT_FOUND);
        verify(endpoint, never()).handle(any(), any());
    }

    @Test
    public void otherPathsAnswer404() throws Exception {
        assertThat(call("GET", "/xacml/other", null).getStatus()).isEqualTo(Status.NOT_FOUND);
        assertThat(call("GET", "/xacml/policies/x", null).getStatus()).isEqualTo(Status.NOT_FOUND);

        verify(endpoint, never()).handle(any(), any());
    }

    @Test
    public void authenticationAndHttpContextFiltersRun() throws Exception {
        call("GET", "/xacml/policies", null);

        verify(authenticationFilter, times(1)).filter(any(), any(), any());
        verify(httpContextFilter, times(1)).filter(any(), any(), any());
    }

    @Test
    public void getRegistersOneXacmlRouteAndReservesPolicies() throws Exception {
        Set<String> invalidRealmNames = new HashSet<String>();
        provider.setInvalidRealmNames(invalidRealmNames);
        provider.setXacmlService(mock(XacmlService.class));

        Set<HttpRoute> routes = provider.get();

        assertThat(routes).hasSize(1);
        HttpRoute route = routes.iterator().next();
        assertThat(routeProperty(route, "getMode")).isEqualTo(STARTS_WITH);
        assertThat(routeProperty(route, "getUriTemplate")).isEqualTo("xacml");
        assertThat(invalidRealmNames).contains("policies");
    }

    /** HttpRoute exposes its mode and URI template only package-privately. */
    private static Object routeProperty(HttpRoute route, String getter) throws Exception {
        Method method = HttpRoute.class.getDeclaredMethod(getter);
        method.setAccessible(true);
        return method.invoke(route);
    }
}
