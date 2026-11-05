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

package org.openidentityplatform.openam.oauth2.webfinger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.http.routing.RouteMatchers.requestUriMatcher;
import static org.forgerock.http.routing.RoutingMode.STARTS_WITH;
import static org.forgerock.util.promise.Promises.newResultPromise;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;

import org.forgerock.http.Handler;
import org.forgerock.http.header.ContentTypeHeader;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.http.protocol.Status;
import org.forgerock.http.routing.Router;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.core.realms.RealmTestHelper;
import org.forgerock.openam.http.HttpRoute;
import org.forgerock.openam.rest.RealmContext;
import org.forgerock.openam.rest.RealmRoutingFactory;
import org.forgerock.openam.services.baseurl.BaseURLProviderFactory;
import org.forgerock.openidconnect.OpenIDConnectProviderDiscovery;
import org.forgerock.services.context.Context;
import org.forgerock.services.context.RootContext;
import org.mockito.ArgumentCaptor;
import org.openidentityplatform.openam.oauth2.OAuth2HttpAccessAuditFilter;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class WebFingerHttpRouteProviderTest {

    private static final String QUERY =
            "?resource=acct:demo@openam.example.org&rel=http://openid.net/specs/connect/1.0/issuer";

    private RealmTestHelper realmTestHelper;
    private Realm sub;
    private OAuth2HttpAccessAuditFilter auditFilter;
    private Handler endpoint;
    private WebFingerHttpRouteProvider provider;

    @BeforeMethod
    public void setUp() throws Exception {
        realmTestHelper = new RealmTestHelper();
        realmTestHelper.setupRealmClass();
        realmTestHelper.mockDnsAlias("openam.example.org");          // the host resolves to the root realm
        // A bare name is also looked up: the real lookup finds it among the sub-realms (realms/<sub>).
        sub = realmTestHelper.mockRealmAlias("sub", "sub");
        realmTestHelper.mockInvalidRealm("bogus");
        realmTestHelper.mockInvalidRealm("sub", "bogus");
        realmTestHelper.mockMalformedRealm("a,b");                  // the DN parser rejects the comma
        realmTestHelper.mockMalformedRealm("sub", "a,b");
        realmTestHelper.mockInvalidRealm("nonsense");
        realmTestHelper.mockInvalidRealm("webfinger");
        // Stands in for the real filter: records the context and calls the next handler.
        auditFilter = mock(OAuth2HttpAccessAuditFilter.class);
        given(auditFilter.filter(any(), any(), any())).willAnswer(invocation ->
                ((Handler) invocation.getArgument(2)).handle(invocation.getArgument(0), invocation.getArgument(1)));
        endpoint = mock(Handler.class);
        given(endpoint.handle(any(), any())).willReturn(newResultPromise(new Response(Status.OK)));
        provider = new WebFingerHttpRouteProvider();
        provider.setRealmRoutingFactory(new RealmRoutingFactory());
        provider.setAuditFilter(auditFilter);
    }

    @AfterMethod
    public void tearDown() {
        realmTestHelper.tearDownRealmClass();
    }

    private Response call(String method, String pathAndQuery) throws Exception {
        return call(method, "openam.example.org", pathAndQuery);
    }

    /** Mounts the handler at ".well-known" the way HttpRouterProvider does. */
    private Response call(String method, String host, String pathAndQuery) throws Exception {
        Router top = new Router();
        top.addRoute(requestUriMatcher(STARTS_WITH, ".well-known"), provider.createHandler(endpoint));
        Request request = new Request().setMethod(method)
                .setUri("http://" + host + ":8080/.well-known" + pathAndQuery);
        return top.handle(new RootContext(), request).getOrThrowUninterruptibly();
    }

    private Realm realmSeenByEndpoint() throws Exception {
        ArgumentCaptor<Context> context = ArgumentCaptor.forClass(Context.class);
        verify(endpoint).handle(context.capture(), any());
        return context.getValue().asContext(RealmContext.class).getRealm();
    }

    @DataProvider
    public Object[][] realmSpellings() {
        return new Object[][] {
            {"/webfinger" + QUERY, "/"},
            {"/realms/root/webfinger" + QUERY, "/"},
            {"/realms/root/realms/sub/webfinger" + QUERY, "/sub"},
            {"/realms/sub/webfinger" + QUERY, "/sub"},
            {"/sub/webfinger" + QUERY, "/sub"},
            {"/webfinger" + QUERY + "&realm=/sub", "/sub"},
            {"/webfinger" + QUERY + "&realm=/sub&realm=/", "/sub"},
            // The realm parameter wins over the path, as at the last hop of RestletRealmRouter.
            {"/sub/webfinger" + QUERY + "&realm=/", "/"},
            {"/realms/root/realms/sub/webfinger" + QUERY + "&realm=/", "/"},
        };
    }

    @Test(dataProvider = "realmSpellings")
    public void resolvesTheRealmOfEverySpelling(String pathAndQuery, String realm) throws Exception {
        Response response = call("GET", pathAndQuery);

        assertThat(response.getStatus()).isEqualTo(Status.OK);
        assertThat(realmSeenByEndpoint().asPath()).isEqualTo(realm);
    }

    @Test
    public void resolvesTheRealmOfTheHostName() throws Exception {
        realmTestHelper.mockDnsAlias("sub.example.org", "sub");

        Response response = call("GET", "sub.example.org", "/webfinger" + QUERY);

        assertThat(response.getStatus()).isEqualTo(Status.OK);
        assertThat(realmSeenByEndpoint().asPath()).isEqualTo("/sub");
    }

    @Test
    public void theRealmsKeywordWinsOverATopLevelRealmNamedRealms() throws Exception {
        // Master resolved this path as the sub-realm x of a top-level realm named "realms"; /json resolves x.
        realmTestHelper.mockRealm("realms");
        realmTestHelper.mockRealm("realms", "x");
        realmTestHelper.mockRealmAlias("x", "x");

        Response response = call("GET", "/realms/x/webfinger" + QUERY);

        assertThat(response.getStatus()).isEqualTo(Status.OK);
        assertThat(realmSeenByEndpoint().asPath()).isEqualTo("/x");
    }

    @Test
    public void auditsWithTheFinalRealm() throws Exception {
        call("GET", "/webfinger" + QUERY + "&realm=/sub");

        ArgumentCaptor<Context> context = ArgumentCaptor.forClass(Context.class);
        verify(auditFilter).filter(context.capture(), any(), any());
        assertThat(context.getValue().asContext(RealmContext.class).getRealm()).isEqualTo(sub);
    }

    @DataProvider
    public Object[][] unknownRealms() {
        return new Object[][] {
            {"/webfinger" + QUERY + "&realm=/bogus", "/bogus"},
            {"/bogus/webfinger" + QUERY, "/bogus"},
            {"/sub/bogus/webfinger" + QUERY, "/sub/bogus"},
            // CHF decodes the segment. Master looked the raw segment up, a valid but unknown realm, and answered 404.
            {"/a%2Cb/webfinger" + QUERY, "/a,b"},
            {"/sub/a%2Cb/webfinger" + QUERY, "/sub/a,b"},
            {"/a%2Fb/webfinger" + QUERY, "/a/b"},
            {"/sub/a%2Fb/webfinger" + QUERY, "/sub/a/b"},
        };
    }

    @Test(dataProvider = "unknownRealms")
    public void answersAnUnknownRealmWith404InTheOAuth2Shape(String pathAndQuery, String realm) throws Exception {
        Response response = call("GET", pathAndQuery);

        assertThat(response.getStatus()).isEqualTo(Status.NOT_FOUND);
        assertThat(response.getHeaders().getFirst(ContentTypeHeader.NAME))
                .isEqualTo("application/json; charset=UTF-8");
        assertThat(response.getEntity().getJson())
                .isEqualTo(Map.of("error", "Not Found", "error_description", "Realm \"" + realm + "\" not found"));
        verify(endpoint, never()).handle(any(), any());
        verify(auditFilter, never()).filter(any(), any(), any());
    }

    @Test
    public void answersAnUnknownRealmInTheRealmsSpellingWithACrest404() throws Exception {
        Response response = call("GET", "/realms/root/realms/bogus/webfinger" + QUERY);

        assertThat(response.getStatus()).isEqualTo(Status.NOT_FOUND);
        Map<?, ?> json = (Map<?, ?>) response.getEntity().getJson();
        assertThat(json.get("code")).isEqualTo(404);
        assertThat(json.containsKey("error_description")).isFalse();
        verify(endpoint, never()).handle(any(), any());
        verify(auditFilter, never()).filter(any(), any(), any());
    }

    @Test
    public void answersOtherPathsWith404() throws Exception {
        assertThat(call("GET", "/nonsense").getStatus()).isEqualTo(Status.NOT_FOUND);
        assertThat(call("GET", "/sub").getStatus()).isEqualTo(Status.NOT_FOUND);
        assertThat(call("GET", "/webfinger/x").getStatus()).isEqualTo(Status.NOT_FOUND);

        verify(endpoint, never()).handle(any(), any());
    }

    @Test
    public void getRegistersOneWellKnownRoute() throws Exception {
        provider.setWebFingerService(
                new WebFingerService(new OpenIDConnectProviderDiscovery(), mock(BaseURLProviderFactory.class)));

        Set<HttpRoute> routes = provider.get();

        assertThat(routes).hasSize(1);
        HttpRoute route = routes.iterator().next();
        assertThat(routeProperty(route, "getMode")).isEqualTo(STARTS_WITH);
        assertThat(routeProperty(route, "getUriTemplate")).isEqualTo(".well-known");
    }

    /** HttpRoute exposes its mode and URI template only package-privately. */
    private static Object routeProperty(HttpRoute route, String getter) throws Exception {
        Method method = HttpRoute.class.getDeclaredMethod(getter);
        method.setAccessible(true);
        return method.invoke(route);
    }
}
