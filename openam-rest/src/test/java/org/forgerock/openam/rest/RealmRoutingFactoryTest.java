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

package org.forgerock.openam.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.net.URI;
import java.util.Collections;
import java.util.Map;

import org.forgerock.http.Filter;
import org.forgerock.http.Handler;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.http.protocol.Status;
import org.forgerock.http.routing.UriRouterContext;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.NotFoundException;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.ResourcePath;
import org.forgerock.openam.core.realms.Realm;
import org.forgerock.openam.core.realms.RealmLookupException;
import org.forgerock.openam.core.realms.RealmTestHelper;
import org.forgerock.services.context.Context;
import org.forgerock.services.context.RootContext;
import org.mockito.ArgumentCaptor;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class RealmRoutingFactoryTest {

    private static final Map<String, String> BASE_URI_ROUTER_CONTEXT = Collections.singletonMap("realmId", "root");

    private RealmTestHelper realmTestHelper;

    private RealmRoutingFactory realmRoutingFactory;

    @BeforeMethod
    public void setup() throws Exception {
        realmTestHelper = new RealmTestHelper();
        realmTestHelper.setupRealmClass();

        realmRoutingFactory = new RealmRoutingFactory();
    }

    @AfterMethod
    public void tearDown() {
        realmTestHelper.tearDownRealmClass();
    }

    @Test
    public void itResolvesRealmFromHostname() throws Exception {

        Filter hostnameFilter = realmRoutingFactory.createHostnameFilter();
        Context context = new RootContext();
        Request request = new Request().setUri(URI.create("http://HOSTNAME"));
        Handler next = mock(Handler.class);

        Realm realm = realmTestHelper.mockDnsAlias("HOSTNAME", "REALM");

        hostnameFilter.filter(context, request, next);

        ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
        verify(next).handle(contextCaptor.capture(), eq(request));

        assertThat(contextCaptor.getValue().containsContext(RealmContext.class)).isTrue();
        RealmContext realmContext = contextCaptor.getValue().asContext(RealmContext.class);
        assertThat(realmContext.getRealm()).isEqualTo(realm);
    }

    @Test
    public void itHandlesChfRoutesOnRootRealm() throws Exception {

        Handler next = mock(Handler.class);
        Handler router = realmRoutingFactory.createRouter(next);
        Context context = new UriRouterContext(new RootContext(), "realms/root", "RESOURCE", BASE_URI_ROUTER_CONTEXT);
        Request request = new Request().setUri(URI.create("http://HOSTNAME/realms/root/RESOURCE"));

        router.handle(context, request);

        ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
        verify(next).handle(contextCaptor.capture(), eq(request));

        assertThat(contextCaptor.getValue().containsContext(RealmContext.class)).isTrue();
        RealmContext realmContext = contextCaptor.getValue().asContext(RealmContext.class);
        assertThat(realmContext.getRealm()).isEqualTo(Realm.root());
    }

    @Test
    public void itHandlesChfRoutesOnSubRealms() throws Exception {

        Handler next = mock(Handler.class);
        Handler router = realmRoutingFactory.createRouter(next);
        Context context = new UriRouterContext(new RootContext(), "realms/root", "realms/subrealm/realms/otherrealm/RESOURCE", BASE_URI_ROUTER_CONTEXT);
        Request request = new Request().setUri(URI.create("http://HOSTNAME/realms/root/realms/subrealm/realms/otherrealm/RESOURCE"));

        realmTestHelper.mockRealm("subrealm");
        Realm otherRealm = realmTestHelper.mockRealm("subrealm", "otherrealm");

        router.handle(context, request);

        ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
        verify(next).handle(contextCaptor.capture(), eq(request));

        assertThat(contextCaptor.getValue().containsContext(RealmContext.class)).isTrue();
        RealmContext realmContext = contextCaptor.getValue().asContext(RealmContext.class);
        assertThat(realmContext.getRealm()).isEqualTo(otherRealm);
    }

    @Test
    public void itHandlesChfRoutesOnInvalidRealms() throws Exception {

        Handler next = mock(Handler.class);
        Handler router = realmRoutingFactory.createRouter(next);
        Context context = new UriRouterContext(new RootContext(), "realms/root", "realms/invalidrealm/RESOURCE", BASE_URI_ROUTER_CONTEXT);
        Request request = new Request().setUri(URI.create("http://HOSTNAME/realms/root/realms/invalidrealm/RESOURCE"));

        realmTestHelper.mockInvalidRealm("invalidrealm");

        Response response = router.handle(context, request).getOrThrowUninterruptibly();

        verify(next, never()).handle(any(Context.class), eq(request));
        assertThat(response.getStatus()).isEqualTo(Status.NOT_FOUND);
        assertThat(response.getEntity().getString()).contains("Realm", "/invalidrealm", "not found");
    }

    @Test
    public void itHandlesCrestRoutesOnRootRealm() throws Exception {

        RequestHandler next = mock(RequestHandler.class);
        RequestHandler router = realmRoutingFactory.createRouter(next);
        Context context = new UriRouterContext(new RootContext(), "realms/root", "RESOURCE", BASE_URI_ROUTER_CONTEXT);
        ActionRequest request = mock(ActionRequest.class);
        given(request.getResourcePathObject()).willReturn(ResourcePath.resourcePath("RESOURCE"));

        router.handleAction(context, request);

        ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
        verify(next).handleAction(contextCaptor.capture(), any(ActionRequest.class));

        assertThat(contextCaptor.getValue().containsContext(RealmContext.class)).isTrue();
        RealmContext realmContext = contextCaptor.getValue().asContext(RealmContext.class);
        assertThat(realmContext.getRealm()).isEqualTo(Realm.root());
    }

    @Test
    public void itHandlesCrestRoutesOnSubRealms() throws Exception {

        RequestHandler next = mock(RequestHandler.class);
        RequestHandler router = realmRoutingFactory.createRouter(next);
        Context context = new UriRouterContext(new RootContext(), "realms/root", "RESOURCE", BASE_URI_ROUTER_CONTEXT);
        ActionRequest request = mock(ActionRequest.class);
        given(request.getResourcePathObject()).willReturn(ResourcePath.resourcePath("realms/subrealm/realms/otherrealm/RESOURCE"));

        realmTestHelper.mockRealm("subrealm");
        Realm otherRealm = realmTestHelper.mockRealm("subrealm", "otherrealm");

        router.handleAction(context, request);

        ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
        verify(next).handleAction(contextCaptor.capture(), any(ActionRequest.class));

        assertThat(contextCaptor.getValue().containsContext(RealmContext.class)).isTrue();
        RealmContext realmContext = contextCaptor.getValue().asContext(RealmContext.class);
        assertThat(realmContext.getRealm()).isEqualTo(otherRealm);
    }

    @Test
    public void itHandlesCrestRoutesOnInvalidRealms() throws Exception {

        RequestHandler next = mock(RequestHandler.class);
        RequestHandler router = realmRoutingFactory.createRouter(next);
        Context context = new UriRouterContext(new RootContext(), "realms/root", "RESOURCE", BASE_URI_ROUTER_CONTEXT);
        ActionRequest request = mock(ActionRequest.class);
        given(request.getResourcePathObject()).willReturn(ResourcePath.resourcePath("realms/invalidrealm/RESOURCE"));

        realmTestHelper.mockInvalidRealm("invalidrealm");

        try {
            router.handleAction(context, request).getOrThrowUninterruptibly();
        } catch (NotFoundException e) {
            verify(next, never()).handleAction(any(Context.class), eq(request));
            assertThat(e.getMessage()).contains("Realm", "/invalidrealm", "not found");
        }
    }
}
