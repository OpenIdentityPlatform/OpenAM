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

package org.forgerock.openam.rest.resource;

import static org.fest.assertions.Assertions.assertThat;
import static org.forgerock.json.fluent.JsonValue.json;
import static org.forgerock.json.fluent.JsonValue.object;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.idm.IdRepoException;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.CollectionResourceProvider;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResult;
import org.forgerock.json.resource.QueryResultHandler;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.Resources;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.json.resource.servlet.HttpServletAdapter;
import org.forgerock.openam.core.CoreWrapper;
import org.forgerock.openam.rest.router.RestRealmValidator;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class CrestRealmRouterTest {


    private RestRealmValidator realmValidator;
    private CoreWrapper coreWrapper;

    private CrestRealmRouter router;

    private CollectionResourceProvider usersProvider;
    private CollectionResourceProvider groupsProvider;

    @BeforeClass
    private void setUpClass() {
        usersProvider = mock(CollectionResourceProvider.class);
        groupsProvider = mock(CollectionResourceProvider.class);
    }

    @BeforeMethod
    public void setUp() {
        reset(usersProvider, groupsProvider);

        realmValidator = mock(RestRealmValidator.class);
        coreWrapper = mock(CoreWrapper.class);
        router = new CrestRealmRouter(realmValidator, coreWrapper);

        router.addRoute("/users", collection(usersProvider));
        router.addRoute("/groups", collection(groupsProvider));

        given(realmValidator.isRealm(anyString())).willReturn(true);
    }

    @DataProvider(name = "data")
    private Object[][] dataProvider() {
        return new Object[][]{
                {"", "/users/alice", "alice", "/", usersProvider},
                {"/realm1/realm2", "/users/alice", "alice", "/realm1/realm2", usersProvider},
                {"", "/groups/admin","admin",  "/", groupsProvider},
                {"/realm1/realm2", "/groups/admin", "admin", "/realm1/realm2", groupsProvider},
        };
    }

    @Test(dataProvider = "data")
    public void shouldRouteToRealmWithDynamicURIRealm(String uriRealm, String resource, String resourceName,
            String expectedRealm, CollectionResourceProvider expectedProvider) throws Exception {

        //Given
        HttpServletRequest request = setUpRequest("openam.example.com", uriRealm, null, resource);
        HttpServletResponse response = setUpResponse();
        SSOToken adminToken = mock(SSOToken.class);
        setUpServer(adminToken);

        //When
        routeRequest(router, request, response);

        //Then
        ArgumentCaptor<ServerContext> contextCaptor = ArgumentCaptor.forClass(ServerContext.class);
        verify(expectedProvider).readInstance(contextCaptor.capture(), eq(resourceName),
                Matchers.<ReadRequest>anyObject(), Matchers.<ResultHandler<Resource>>anyObject());
        RealmContext realmContext = contextCaptor.getValue().asContext(RealmContext.class);
        assertThat(realmContext.getResolvedRealm()).isEqualTo(expectedRealm);
    }

    @DataProvider(name = "realmRoutingDataProvider")
    private Object[][] realmRoutingDataProvider() {
        return new Object[][]{
                //http://openam.example.com:8080/openam/json/users/demo
                {"openam.example.com", "", null, "/"},
                //http://alias.example.com:8080/openam/json/users/demo
                {"alias.example.com", "", null, "/otherRealm"},
                //http://openam.example.com:8080/openam/json/users/demo?realm=/realm
                {"openam.example.com", "", "/realm", "/realm"},
                //http://openam.example.com:8080/openam/json/users/demo?realm=/realmAlias
                {"openam.example.com", "", "/realmAlias", "/realm"},
                //http://openam.example.com:8080/openam/json/realm/users/demo
                {"openam.example.com", "/realm", null, "/realm"},
                //http://openam.example.com:8080/openam/json/realmAlias/users/demo
                {"openam.example.com", "/realmAlias", null, "/realm"},
        };
    }

    @Test(dataProvider = "realmRoutingDataProvider")
    public void shouldRouteToRealm(String hostname, String uriRealm, String queryRealm, String expectedRealm)
            throws Exception {

        //Given
        HttpServletRequest request = setUpRequest(hostname, uriRealm, queryRealm);
        HttpServletResponse response = setUpResponse();
        SSOToken adminToken = mock(SSOToken.class);
        setUpServer(adminToken);

        CrestRealmRouter router = new CrestRealmRouter(realmValidator, coreWrapper);
        CollectionResourceProvider mockResource = mock(CollectionResourceProvider.class);
        CollectionResourceProvider resource = collection(mockResource);
        router.addRoute("users", resource);

        //When
        routeRequest(router, request, response);

        //Then
        ArgumentCaptor<ServerContext> contextCaptor = ArgumentCaptor.forClass(ServerContext.class);
        verify(mockResource).readInstance(contextCaptor.capture(), eq("demo"), Matchers.<ReadRequest>anyObject(),
                Matchers.<ResultHandler<Resource>>anyObject());
        ServerContext serverContext = contextCaptor.getValue();
        RealmContext realmContext = serverContext.asContext(RealmContext.class);
        final String relativeRealm = realmContext.getResolvedRealm();
        assertThat(relativeRealm).isEqualTo(expectedRealm);
    }

    private HttpServletRequest setUpRequest(String hostname, String uriRealm, String queryRealm) {
        return setUpRequest(hostname, uriRealm, queryRealm, "/users/demo");
    }

    private HttpServletRequest setUpRequest(String hostname, String uriRealm, String queryRealm, String resource) {

        String queryParam = "";
        if (queryRealm != null) {
            queryParam += "?realm=" + queryRealm;
        }

        HttpServletRequest request = mock(HttpServletRequest.class);
        given(request.getMethod()).willReturn("GET");
        given(request.getRequestURL()).willReturn(new StringBuffer("http://" + hostname + ":8080/openam/json"
                + uriRealm + resource + queryParam));
        given(request.getContextPath()).willReturn("/openam");
        given(request.getServletPath()).willReturn("/json");
        given(request.getRequestURI()).willReturn("/openam/json" + uriRealm + resource);
        given(request.getHeaderNames()).willReturn(Collections.enumeration(Collections.emptySet()));
        Map<String, String[]> parameterMap = new HashMap<String, String[]>();
        given(request.getParameterMap()).willReturn(parameterMap);
        if (queryRealm != null) {
            parameterMap.put("realm", new String[]{queryRealm});
        }

        return request;
    }

    private HttpServletResponse setUpResponse() throws IOException {
        HttpServletResponse response = mock(HttpServletResponse.class);
        ServletOutputStream outputStream = mock(ServletOutputStream.class);
        given(response.getOutputStream()).willReturn(outputStream);
        return response;
    }

    private void setUpServer(SSOToken adminToken) throws IdRepoException, SSOException {
        given(coreWrapper.getAdminToken()).willReturn(adminToken);

        given(coreWrapper.getOrganization(adminToken, "openam.example.com")).willReturn("ROOT_REALM_DN");
        given(coreWrapper.getOrganization(adminToken, "alias.example.com")).willReturn("REALM_ALIAS_DN");
        given(coreWrapper.convertOrgNameToRealmName("ROOT_REALM_DN")).willReturn("/");
        given(coreWrapper.convertOrgNameToRealmName("REALM_ALIAS_DN")).willReturn("/otherRealm");

        given(realmValidator.isRealm("/realm")).willReturn(true);
        given(realmValidator.isRealm("/realmAlias")).willReturn(false);
        given(coreWrapper.getOrganization(adminToken, "realmAlias")).willReturn("REALM_DN");
        given(coreWrapper.convertOrgNameToRealmName("REALM_DN")).willReturn("/realm");
    }

    private void routeRequest(RequestHandler requestHandler, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException {
        ServletContext servletContext = mock(ServletContext.class);
        given(servletContext.getMajorVersion()).willReturn(2);
        new HttpServletAdapter(servletContext, Resources.newInternalConnectionFactory(requestHandler))
                .service(request, response);
    }

    private static CollectionResourceProvider collection(final CollectionResourceProvider provider) {
        return new CollectionResourceProvider() {

            @Override
            public void actionCollection(ServerContext context, ActionRequest request,
                    ResultHandler<JsonValue> handler) {
                provider.actionCollection(context, request, handler);
                handler.handleResult(json(object()));
            }

            @Override
            public void actionInstance(ServerContext context, String resourceId, ActionRequest request,
                    ResultHandler<JsonValue> handler) {
                provider.actionInstance(context, resourceId, request, handler);
                handler.handleResult(json(object()));
            }

            @Override
            public void createInstance(ServerContext context, CreateRequest request, ResultHandler<Resource> handler) {
                provider.createInstance(context, request, handler);
                handler.handleResult(new Resource("ID", "1", json(object())));
            }

            @Override
            public void deleteInstance(ServerContext context, String resourceId, DeleteRequest request,
                    ResultHandler<Resource> handler) {
                provider.deleteInstance(context, resourceId, request, handler);
                handler.handleResult(new Resource(resourceId, "1", json(object())));
            }

            @Override
            public void patchInstance(ServerContext context, String resourceId, PatchRequest request,
                    ResultHandler<Resource> handler) {
                provider.patchInstance(context, resourceId, request, handler);
                handler.handleResult(new Resource(resourceId, "1", json(object())));
            }

            @Override
            public void queryCollection(ServerContext context, QueryRequest request, QueryResultHandler handler) {
                provider.queryCollection(context, request, handler);
                handler.handleResult(new QueryResult());
            }

            @Override
            public void readInstance(ServerContext context, String resourceId, ReadRequest request,
                    ResultHandler<Resource> handler) {
                provider.readInstance(context, resourceId, request, handler);
                handler.handleResult(new Resource(resourceId, "1", json(object())));
            }

            @Override
            public void updateInstance(ServerContext context, String resourceId, UpdateRequest request,
                    ResultHandler<Resource> handler) {
                provider.updateInstance(context, resourceId, request, handler);
                handler.handleResult(new Resource(resourceId, "1", json(object())));
            }
        };
    }
}
