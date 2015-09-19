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

import com.iplanet.sso.SSOToken;
import com.sun.identity.idm.IdRepoException;
import org.forgerock.services.context.Context;
import org.forgerock.http.Handler;
import org.forgerock.services.context.RootContext;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.http.protocol.Status;
import org.forgerock.http.routing.UriRouterContext;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.openam.core.CoreWrapper;
import org.forgerock.openam.rest.router.RestRealmValidator;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

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
        assertThat(response.getStatus()).isSameAs(Status.INTERNAL_SERVER_ERROR);
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
        assertThat(response.getStatus()).isSameAs(Status.INTERNAL_SERVER_ERROR);
        assertThat(response.getEntity().getJson()).isEqualTo(
                new InternalServerErrorException("EXCEPTION_MESSAGE").toJsonValue().getObject());
    }

    private Context mockContext(String remainingUri) {
        return new UriRouterContext(new RootContext(), JSON_PATH_ELEMENT, remainingUri, EMPTY_VARIABLE_MAP);
    }

    private Request createRequest(String hostname, String path) {
        return new Request().setUri(URI.create("http://" + hostname + "/json/" + path));
    }

    private void mockDnsAlias(String alias, String realm) throws Exception {
        mockRealmAlias(alias, realm);
    }

    private void mockInvalidDnsAlias(String alias) throws Exception {
        mockInvalidRealmAlias(alias);
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
}
