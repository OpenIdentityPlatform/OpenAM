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
 * Copyright 2015-2016 ForgeRock AS.
 */

package org.forgerock.openam.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.Map;

import org.forgerock.http.Handler;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.http.protocol.Status;
import org.forgerock.http.routing.UriRouterContext;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.FilterChain;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.json.resource.http.CrestHttp;
import org.forgerock.openam.core.CoreWrapper;
import org.forgerock.openam.rest.query.QueryResponseHandler;
import org.forgerock.openam.rest.router.RestRealmValidator;
import org.forgerock.services.context.AttributesContext;
import org.forgerock.services.context.Context;
import org.forgerock.services.context.RootContext;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.promise.Promises;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.iplanet.sso.SSOToken;
import com.sun.identity.idm.IdRepoException;

public class RealmContextFilterTest {

    private static final String HOSTNAME = "HOSTNAME";
    private static final String DNS_ALIAS_HOSTNAME = "DNS-ALIAS-HOSTNAME";
    private static final String INVALID_DNS_ALIAS_HOSTNAME = "INVALID-DNS-ALIAS-HOSTNAME";
    private static final String JSON_PATH_ELEMENT = "json";
    private static final String ENDPOINT_PATH_ELEMENT = "ENDPOINT";
    private static final Map<String, String> EMPTY_VARIABLE_MAP = Collections.emptyMap();
    private static final String DNS_ALIS_SUB_REALM = "DNS_ALIAS_SUB_REALM";
    private static final String SUB_REALM = "SUB_REALM";
    private static final String SUB_REALM_ALIAS = "SUB_REALM_ALIAS";
    private static final String INVALID_SUB_REALM = "INVALID_SUB_REALM";
    private static final String OVERRIDE_REALM = "/OVERRIDE_REALM";
    private static final String OVERRIDE_REALM_ALIAS = "OVERRIDE_REALM_ALIAS";
    private static final String INVALID_OVERRIDE_REALM = "INVALID_OVERRIDER_REALM";

    private RealmContextFilter filter;

    @Mock
    private CoreWrapper coreWrapper;
    @Mock
    private RestRealmValidator realmValidator;
    @Mock
    private Handler handler;

    @SuppressWarnings("unchecked")
    @BeforeMethod
    public void setup() throws Exception {
        initMocks(this);
        filter = new RealmContextFilter(coreWrapper, realmValidator);

        given(coreWrapper.getOrganization(any(SSOToken.class), eq(ENDPOINT_PATH_ELEMENT)))
                .willThrow(IdRepoException.class);
    }

    @Test
    public void filterShouldConsumeRealmFromRequest() throws Exception {

        //Given
        Context context = mockContext(ENDPOINT_PATH_ELEMENT);
        Request request = createRequest(HOSTNAME, ENDPOINT_PATH_ELEMENT);

        mockDnsAlias(HOSTNAME, "/");

        //When
        filter.filter(context, request, handler);

        //Then
        ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
        verify(handler).handle(contextCaptor.capture(), eq(request));
        verifyRealmContext(contextCaptor.getValue(), "", "/", null);
        verifyUriRouterContext(contextCaptor.getValue(), "");
    }

    @Test
    public void filterShouldConsumeRealmFromRequestWithDnsAlias() throws Exception {

        //Given
        Context context = mockContext(ENDPOINT_PATH_ELEMENT);
        Request request = createRequest(DNS_ALIAS_HOSTNAME, ENDPOINT_PATH_ELEMENT);

        mockDnsAlias(DNS_ALIAS_HOSTNAME, "/" + DNS_ALIS_SUB_REALM);

        //When
        filter.filter(context, request, handler);

        //Then
        ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
        verify(handler).handle(contextCaptor.capture(), eq(request));
        verifyRealmContext(contextCaptor.getValue(), "/" + DNS_ALIS_SUB_REALM, "", null);
        verifyUriRouterContext(contextCaptor.getValue(), "");
    }

    @Test
    public void filterShouldConsumeRealmFromRequestWithInvalidDnsAlias() throws Exception {

        //Given
        Context context = mockContext(ENDPOINT_PATH_ELEMENT);
        Request request = createRequest(INVALID_DNS_ALIAS_HOSTNAME, ENDPOINT_PATH_ELEMENT);

        mockInvalidDnsAlias(INVALID_DNS_ALIAS_HOSTNAME);

        //When
        Response response = filter.filter(context, request, handler).getOrThrowUninterruptibly();

        //Then
        assertThat(response.getStatus()).isSameAs(Status.BAD_REQUEST);
    }

    @Test
    public void filterShouldConsumeRealmFromRequestWithUriRealm() throws Exception {

        //Given
        Context context = mockContext(SUB_REALM + "/" + ENDPOINT_PATH_ELEMENT);
        Request request = createRequest(HOSTNAME, SUB_REALM + "/" + ENDPOINT_PATH_ELEMENT);

        mockDnsAlias(HOSTNAME, "/");
        mockRealmAlias("/" + SUB_REALM, "/" + SUB_REALM);

        //When
        filter.filter(context, request, handler);

        //Then
        ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
        verify(handler).handle(contextCaptor.capture(), eq(request));
        verifyRealmContext(contextCaptor.getValue(), "", "/" + SUB_REALM, null);
        verifyUriRouterContext(contextCaptor.getValue(), SUB_REALM);
    }

    @Test
    public void filterShouldConsumeRealmFromRequestWithUriRealmAlias() throws Exception {

        //Given
        Context context = mockContext(SUB_REALM_ALIAS + "/" + ENDPOINT_PATH_ELEMENT);
        Request request = createRequest(HOSTNAME, SUB_REALM_ALIAS + "/" + ENDPOINT_PATH_ELEMENT);

        mockDnsAlias(HOSTNAME, "/");
        mockRealmAlias(SUB_REALM_ALIAS, "/" + SUB_REALM);

        //When
        filter.filter(context, request, handler);

        //Then
        ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
        verify(handler).handle(contextCaptor.capture(), eq(request));
        verifyRealmContext(contextCaptor.getValue(), "", "/" + SUB_REALM, null);
        verifyUriRouterContext(contextCaptor.getValue(), SUB_REALM_ALIAS);
    }

    @Test
    public void filterShouldFailToConsumeRealmFromRequestWithInvalidUriRealm() throws Exception {

        //Given
        Context context = mockContext(INVALID_SUB_REALM + "/" + ENDPOINT_PATH_ELEMENT);
        Request request = createRequest(HOSTNAME, INVALID_SUB_REALM + "/" + ENDPOINT_PATH_ELEMENT);

        mockDnsAlias(HOSTNAME, "/");
        mockInvalidRealmAlias(INVALID_SUB_REALM);

        //When
        filter.filter(context, request, handler);

        //Then
        ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
        verify(handler).handle(contextCaptor.capture(), eq(request));
        verifyRealmContext(contextCaptor.getValue(), "", "/", null);
        verifyUriRouterContextForInvalidRealm(contextCaptor.getValue());
    }

    @Test
    public void filterShouldConsumeRealmFromRequestWithDnsAliasAndUriRealm() throws Exception {

        //Given
        Context context = mockContext(SUB_REALM + "/" + ENDPOINT_PATH_ELEMENT);
        Request request = createRequest(DNS_ALIAS_HOSTNAME, SUB_REALM + "/" + ENDPOINT_PATH_ELEMENT);

        mockDnsAlias(DNS_ALIAS_HOSTNAME, "/" + DNS_ALIS_SUB_REALM);
        mockRealmAlias("/" + DNS_ALIS_SUB_REALM + "/" + SUB_REALM, "/" + DNS_ALIS_SUB_REALM + "/" + SUB_REALM);

        //When
        filter.filter(context, request, handler);

        //Then
        ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
        verify(handler).handle(contextCaptor.capture(), eq(request));
        verifyRealmContext(contextCaptor.getValue(), "/" + DNS_ALIS_SUB_REALM, "/" + SUB_REALM, null);
        verifyUriRouterContext(contextCaptor.getValue(), SUB_REALM);
    }

    @Test
    public void filterShouldFailToConsumeRealmFromRequestWithDnsAliasAndUriRealmAlias() throws Exception {

        //Given
        Context context = mockContext(SUB_REALM_ALIAS + "/" + ENDPOINT_PATH_ELEMENT);
        Request request = createRequest(DNS_ALIAS_HOSTNAME, SUB_REALM_ALIAS + "/" + ENDPOINT_PATH_ELEMENT);

        mockDnsAlias(DNS_ALIAS_HOSTNAME, "/" + DNS_ALIS_SUB_REALM);
        mockRealmAlias("/" + DNS_ALIS_SUB_REALM + "/" + SUB_REALM_ALIAS, "/" + DNS_ALIS_SUB_REALM + "/" + SUB_REALM);
        mockInvalidRealmAlias(SUB_REALM_ALIAS);

        //When
        filter.filter(context, request, handler);

        //Then
        ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
        verify(handler).handle(contextCaptor.capture(), eq(request));
        verifyUriRouterContextForInvalidRealm(contextCaptor.getValue());
    }

    @Test
    public void filterShouldConsumeRealmFromRequestWithOverrideRealm() throws Exception {

        //Given
        Context context = mockContext(ENDPOINT_PATH_ELEMENT);
        Request request = createRequest(HOSTNAME, ENDPOINT_PATH_ELEMENT + "?realm=" + OVERRIDE_REALM + "/");

        mockDnsAlias(HOSTNAME, "/");
        mockRealmAlias(OVERRIDE_REALM, OVERRIDE_REALM);

        //When
        filter.filter(context, request, handler);

        //Then
        ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
        verify(handler).handle(contextCaptor.capture(), eq(request));
        verifyRealmContext(contextCaptor.getValue(), "/", "", OVERRIDE_REALM);
        verifyUriRouterContext(contextCaptor.getValue(), "");
    }

    @Test
    public void filterShouldConsumeRealmFromRequestWithOverrideRealmAlias() throws Exception {

        //Given
        Context context = mockContext(ENDPOINT_PATH_ELEMENT);
        Request request = createRequest(HOSTNAME, ENDPOINT_PATH_ELEMENT + "?realm=" + OVERRIDE_REALM_ALIAS);

        mockDnsAlias(HOSTNAME, "/");
        mockRealmAlias(OVERRIDE_REALM_ALIAS, OVERRIDE_REALM);

        //When
        filter.filter(context, request, handler);

        //Then
        ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
        verify(handler).handle(contextCaptor.capture(), eq(request));
        verifyRealmContext(contextCaptor.getValue(), "/", "", OVERRIDE_REALM);
        verifyUriRouterContext(contextCaptor.getValue(), "");
    }

    @Test
    public void filterShouldConsumeRealmFromRequestWithInvalidOverrideRealm() throws Exception {

        //Given
        Context context = mockContext(ENDPOINT_PATH_ELEMENT);
        Request request = createRequest(HOSTNAME, ENDPOINT_PATH_ELEMENT + "?realm=" + INVALID_OVERRIDE_REALM);

        mockDnsAlias(HOSTNAME, "/");
        mockInvalidRealmAlias(INVALID_OVERRIDE_REALM);

        //When
        Response response = filter.filter(context, request, handler).getOrThrowUninterruptibly();

        //Then
        verifyInvalidRealmResponse(response, INVALID_OVERRIDE_REALM);
    }

    @Test
    public void filterShouldConsumeRealmFromRequestWithDnsAliasAndOverrideRealm() throws Exception {

        //Given
        Context context = mockContext(ENDPOINT_PATH_ELEMENT);
        Request request = createRequest(DNS_ALIAS_HOSTNAME, ENDPOINT_PATH_ELEMENT + "?realm=" + OVERRIDE_REALM);

        mockDnsAlias(DNS_ALIAS_HOSTNAME, "/" + DNS_ALIS_SUB_REALM);
        mockRealmAlias(OVERRIDE_REALM, OVERRIDE_REALM);

        //When
        filter.filter(context, request, handler);

        //Then
        ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
        verify(handler).handle(contextCaptor.capture(), eq(request));
        verifyRealmContext(contextCaptor.getValue(), "/" + DNS_ALIS_SUB_REALM, "", OVERRIDE_REALM);
        verifyUriRouterContext(contextCaptor.getValue(), "");
    }

    @Test
    public void filterShouldConsumeRealmFromRequestWithUriRealmAndOverrideRealm() throws Exception {

        //Given
        Context context = mockContext(SUB_REALM + "/" + ENDPOINT_PATH_ELEMENT);
        Request request = createRequest(HOSTNAME, SUB_REALM + "/" + ENDPOINT_PATH_ELEMENT + "?realm=" + OVERRIDE_REALM);

        mockDnsAlias(HOSTNAME, "/");
        mockRealmAlias("/" + SUB_REALM, "/" + SUB_REALM);
        mockRealmAlias(OVERRIDE_REALM_ALIAS, OVERRIDE_REALM);

        //When
        filter.filter(context, request, handler);

        //Then
        ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
        verify(handler).handle(contextCaptor.capture(), eq(request));
        verifyRealmContext(contextCaptor.getValue(), "", "/" + SUB_REALM, OVERRIDE_REALM);
        verifyUriRouterContext(contextCaptor.getValue(), SUB_REALM);
    }

    @Test
    public void filterShouldConsumeRealmFromRequestWithUriRealmAliasAndOverrideRealm() throws Exception {

        //Given
        Context context = mockContext(SUB_REALM_ALIAS + "/" + ENDPOINT_PATH_ELEMENT);
        Request request = createRequest(HOSTNAME, SUB_REALM_ALIAS + "/" + ENDPOINT_PATH_ELEMENT + "?realm=" + OVERRIDE_REALM);

        mockDnsAlias(HOSTNAME, "/");
        mockRealmAlias(SUB_REALM_ALIAS, "/" + SUB_REALM);
        mockRealmAlias(OVERRIDE_REALM, OVERRIDE_REALM);

        //When
        filter.filter(context, request, handler);

        //Then
        ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
        verify(handler).handle(contextCaptor.capture(), eq(request));
        verifyRealmContext(contextCaptor.getValue(), "", "/" + SUB_REALM, OVERRIDE_REALM);
        verifyUriRouterContext(contextCaptor.getValue(), SUB_REALM_ALIAS);
    }

    @Test
    public void filterShouldConsumeRealmFromRequestWithDnsAliasAndUriRealmAndOverrideRealm() throws Exception {

        //Given
        Context context = mockContext(SUB_REALM + "/" + ENDPOINT_PATH_ELEMENT);
        Request request = createRequest(DNS_ALIAS_HOSTNAME, SUB_REALM + "/" + ENDPOINT_PATH_ELEMENT + "?realm=" + OVERRIDE_REALM);

        mockDnsAlias(DNS_ALIAS_HOSTNAME, "/" + DNS_ALIS_SUB_REALM);
        mockRealmAlias("/" + DNS_ALIS_SUB_REALM + "/" + SUB_REALM, "/" + DNS_ALIS_SUB_REALM + "/" + SUB_REALM);
        mockRealmAlias(OVERRIDE_REALM, OVERRIDE_REALM);

        //When
        filter.filter(context, request, handler);

        //Then
        ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
        verify(handler).handle(contextCaptor.capture(), eq(request));
        verifyRealmContext(contextCaptor.getValue(), "/" + DNS_ALIS_SUB_REALM, "/" + SUB_REALM, OVERRIDE_REALM);
        verifyUriRouterContext(contextCaptor.getValue(), SUB_REALM);
    }

    @Test
    public void filterShouldFailToConsumeRealmFromRequestOnExceptionWhenResolvingServerName() throws Exception {

        //Given
        Context context = mockContext(ENDPOINT_PATH_ELEMENT);
        Request request = createRequest(HOSTNAME, ENDPOINT_PATH_ELEMENT);

        IdRepoException exception = mock(IdRepoException.class);
        given(exception.getMessage()).willReturn("EXCEPTION_MESSAGE");

        doThrow(exception).when(coreWrapper).getOrganization(any(SSOToken.class), eq(HOSTNAME));

        //When
        Response response = filter.filter(context, request, handler).getOrThrowUninterruptibly();

        //Then
        assertThat(response.getStatus()).isSameAs(Status.BAD_REQUEST);
        assertThat(response.getEntity().getJson()).isEqualTo(
                new BadRequestException("FQDN \"HOSTNAME\" is not valid.").toJsonValue().getObject());
    }

    @Test(dataProvider = "CRUDPAQ")
    public void filterShouldConsumeRealmFromCRUDPAQRequest(Request request, String postURIString) throws Exception {

        //Given
        RequestHandler requestHandler = mock(RequestHandler.class);
        String path = ENDPOINT_PATH_ELEMENT;
        Context context = mockContext(path);
        request.setUri(createRequestURI(HOSTNAME, path, postURIString));

        mockDnsAlias(HOSTNAME, "/");

        //When
        Handler httpHandler = getHttpHandler(requestHandler);
        httpHandler.handle(context, request).getOrThrowUninterruptibly();

        //Then
        ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
        ArgumentCaptor<org.forgerock.json.resource.Request> requestCaptor = ArgumentCaptor.forClass(org.forgerock.json.resource.Request.class);

        collectCRUDPAQArguments(requestHandler, contextCaptor, requestCaptor);

        verifyRealmContext(contextCaptor.getValue(), "", "/", null);
        verifyUriRouterContext(contextCaptor.getValue(), "");
        verifyResolvedResourcePath(requestCaptor.getValue(), ENDPOINT_PATH_ELEMENT);
    }

    @Test(dataProvider = "CRUDPAQ")
    public void filterShouldConsumeRealmFromCRUDPAQRequestWithSubrealm(Request request, String postURIString) throws Exception {

        //Given
        RequestHandler requestHandler = mock(RequestHandler.class);
        String path = SUB_REALM + "/" + ENDPOINT_PATH_ELEMENT;
        Context context = mockContext(path);
        request.setUri(createRequestURI(HOSTNAME, path, postURIString));

        mockDnsAlias(HOSTNAME, "/");
        mockRealmAlias("/" + SUB_REALM, "/" + SUB_REALM);

        //When
        Handler httpHandler = getHttpHandler(requestHandler);
        httpHandler.handle(context, request).getOrThrowUninterruptibly();

        //Then
        ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
        ArgumentCaptor<org.forgerock.json.resource.Request> requestCaptor = ArgumentCaptor.forClass(org.forgerock.json.resource.Request.class);

        collectCRUDPAQArguments(requestHandler, contextCaptor, requestCaptor);

        verifyRealmContext(contextCaptor.getValue(), "", "/" + SUB_REALM, null);
        verifyUriRouterContext(contextCaptor.getValue(), SUB_REALM);
        verifyResolvedResourcePath(requestCaptor.getValue(), ENDPOINT_PATH_ELEMENT);
    }


    @Test(dataProvider = "CRUDPAQ")
    public void filterShouldConsumeRealmFromCRUDPAQRequestWithInvalidSubrealm(Request request, String postURIString) throws Exception {

        //Given
        RequestHandler requestHandler = mock(RequestHandler.class);
        String path = INVALID_SUB_REALM + "/" + ENDPOINT_PATH_ELEMENT;
        Context context = mockContext(path);
        request.setUri(createRequestURI(HOSTNAME, path, postURIString));

        mockDnsAlias(HOSTNAME, "/");
        mockInvalidRealmAlias(INVALID_SUB_REALM);

        //When
        Handler httpHandler = getHttpHandler(requestHandler);
        httpHandler.handle(context, request).getOrThrowUninterruptibly();

        //Then
        ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
        ArgumentCaptor<org.forgerock.json.resource.Request> requestCaptor = ArgumentCaptor.forClass(org.forgerock.json.resource.Request.class);

        collectCRUDPAQArguments(requestHandler, contextCaptor, requestCaptor);

        verifyRealmContext(contextCaptor.getValue(), "", "/", null);
        verifyUriRouterContextForInvalidRealm(contextCaptor.getValue());
        verifyResolvedResourcePath(requestCaptor.getValue(), INVALID_SUB_REALM + "/" + ENDPOINT_PATH_ELEMENT);
    }

    private Context mockContext(String remainingUri) {
        return new UriRouterContext(new AttributesContext(new RootContext()), JSON_PATH_ELEMENT, remainingUri, EMPTY_VARIABLE_MAP);
    }

    private Request createRequest(String hostname, String path) {
        return new Request().setUri(createRequestURI(hostname, path, ""));
    }

    private URI createRequestURI(String hostname, String path, String postString) {
        return URI.create("http://" + hostname + "/json/" + path + postString);
    }

    private void mockDnsAlias(String alias, String realm) throws Exception {
        mockRealmAlias(alias, realm);
        given(coreWrapper.isValidFQDN(anyString())).willReturn(true);
    }

    private void mockInvalidDnsAlias(String alias) throws Exception {
        mockInvalidRealmAlias(alias);
        given(coreWrapper.isValidFQDN(anyString())).willReturn(false);
    }

    private void mockRealmAlias(String alias, String realm) throws Exception {
        given(coreWrapper.getOrganization(any(SSOToken.class), eq(alias))).willReturn(realm);
        given(coreWrapper.convertOrgNameToRealmName(realm)).willReturn(realm);
        given(realmValidator.isRealm(realm)).willReturn(true);
    }

    private void mockInvalidRealmAlias(String alias) throws Exception {
        doThrow(IdRepoException.class).when(coreWrapper).getOrganization(any(SSOToken.class), eq(alias));
    }

    private void verifyRealmContext(Context context, String expectedDnsAliasRealm,
            String expectedRelativeRealm, String expectedOverrideRealm) {
        assertThat(context.containsContext(RealmContext.class)).isTrue();
        RealmContext realmContext = context.asContext(RealmContext.class);
        if (expectedDnsAliasRealm.isEmpty()) {
            assertThat(realmContext.getBaseRealm()).isEqualTo(expectedRelativeRealm);
        } else {
            assertThat(realmContext.getBaseRealm()).isEqualTo(expectedDnsAliasRealm);
        }
        assertThat(realmContext.getRelativeRealm()).isEqualTo(expectedRelativeRealm.isEmpty() ? "/" : expectedRelativeRealm);
        if (expectedOverrideRealm == null) {
            assertThat(realmContext.getResolvedRealm()).isEqualTo(expectedDnsAliasRealm + expectedRelativeRealm);
        } else {
            assertThat(realmContext.getResolvedRealm()).isEqualTo(expectedOverrideRealm);
        }
    }

    private void verifyUriRouterContext(Context context, String matchedUri) {
        UriRouterContext routerContext = context.asContext(UriRouterContext.class);
        if (matchedUri.isEmpty()) {
            assertThat(routerContext.getBaseUri()).isEqualTo(JSON_PATH_ELEMENT);
        } else {
            assertThat(routerContext.getBaseUri()).isEqualTo(JSON_PATH_ELEMENT + "/" + matchedUri);
        }
        assertThat(routerContext.getMatchedUri()).isEqualTo(matchedUri);
        assertThat(routerContext.getRemainingUri()).isEqualTo(ENDPOINT_PATH_ELEMENT);
    }

    private void verifyUriRouterContextForInvalidRealm(Context context) {
        UriRouterContext routerContext = context.asContext(UriRouterContext.class);
        assertThat(routerContext.getBaseUri()).isEqualTo(JSON_PATH_ELEMENT);
        assertThat(routerContext.getMatchedUri()).isEmpty();
    }

    private void verifyInvalidRealmResponse(Response response, String expectedInvalidRealm) throws IOException {
        assertThat(response.getStatus()).isSameAs(Status.BAD_REQUEST);
        assertThat(response.getEntity().getJson()).isEqualTo(
                new BadRequestException("Invalid realm, " + expectedInvalidRealm).toJsonValue().getObject());
    }

    private void collectCRUDPAQArguments(RequestHandler requestHandler, ArgumentCaptor<Context> contextCaptor, ArgumentCaptor<org.forgerock.json.resource.Request> requestCaptor) {
        //The cast on each line is safe, as that function can only be called with that type of request
        //The atLeast(0) call results in each line being optional (they don't all need to be called)
        verify(requestHandler, atLeast(0)).handleCreate(contextCaptor.capture(), (CreateRequest) requestCaptor.capture());
        verify(requestHandler, atLeast(0)).handleRead(contextCaptor.capture(), (ReadRequest) requestCaptor.capture());
        verify(requestHandler, atLeast(0)).handleUpdate(contextCaptor.capture(), (UpdateRequest) requestCaptor.capture());
        verify(requestHandler, atLeast(0)).handleDelete(contextCaptor.capture(), (DeleteRequest) requestCaptor.capture());
        verify(requestHandler, atLeast(0)).handlePatch(contextCaptor.capture(), (PatchRequest) requestCaptor.capture());
        verify(requestHandler, atLeast(0)).handleAction(contextCaptor.capture(), (ActionRequest) requestCaptor.capture());
        verify(requestHandler, atLeast(0)).handleQuery(contextCaptor.capture(), (QueryRequest) requestCaptor.capture(), any(QueryResponseHandler.class));
    }

    private Handler getHttpHandler(RequestHandler requestHandler) {
        ResourceResponse response = mock(ResourceResponse.class);
        Promise<ResourceResponse, ResourceException> result = Promises.newResultPromise(response);

        given(requestHandler.handleCreate(any(Context.class), any(CreateRequest.class))).willReturn(result);
        given(requestHandler.handleRead(any(Context.class), any(ReadRequest.class))).willReturn(result);
        given(requestHandler.handleUpdate(any(Context.class), any(UpdateRequest.class))).willReturn(result);
        given(requestHandler.handleDelete(any(Context.class), any(DeleteRequest.class))).willReturn(result);
        given(requestHandler.handlePatch(any(Context.class), any(PatchRequest.class))).willReturn(result);
        given(requestHandler.handleAction(any(Context.class), any(ActionRequest.class)))
                .willReturn(Promises.<ActionResponse, ResourceException>newResultPromise(mock(ActionResponse.class)));
        given(requestHandler.handleQuery(any(Context.class), any(QueryRequest.class), any(QueryResponseHandler.class)))
                .willReturn(Promises.<QueryResponse, ResourceException>newResultPromise(mock(QueryResponse.class)));
        FilterChain filterChain = new FilterChain(requestHandler, filter);
        return CrestHttp.newHttpHandler(filterChain);

    }

    @DataProvider(name = "CRUDPAQ")
    public Object[][] createCRUDPAQRequests() {
        return new Object[][] {
            { newJsonHttpRequest("POST").setEntity(new String("{}")), "?_action=create" }, // create
            { newJsonHttpRequest("GET"), "" }, //read
            { getUpdateRequest(), "" }, //update
            { newJsonHttpRequest("DELETE"), "" }, //delete
            { newJsonHttpRequest("PATCH").setEntity(new String("[]")), "" }, //patch
            { newJsonHttpRequest("POST"), "?_action=other" }, //action
            { newJsonHttpRequest("GET"), "?_queryFilter=true"} //query
        };
    }

    private Request getUpdateRequest() {
        Request request = newJsonHttpRequest("PUT");
        request.getHeaders().put("If-Match", "*");
        request.setEntity(new String("{}"));
        return withContentType(request);
    }

    private Request newJsonHttpRequest(String method) {
        Request request = new Request().setMethod(method);
        request.getHeaders().put("Content-Type", "application/json");
        return request;
    }

    private Request withContentType(Request request) {
        request.getHeaders().put("Content-Type", "application/json");
        return request;
    }

    private void verifyResolvedResourcePath(org.forgerock.json.resource.Request request, String matchedResourcePath) {
        assertThat(request.getResourcePath()).isEqualTo(matchedResourcePath);
    }
}
