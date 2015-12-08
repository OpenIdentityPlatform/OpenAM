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
 * Copyright 2014-2015 ForgeRock AS.
 */

package org.forgerock.openam.rest.authz;

import static org.fest.assertions.Assertions.assertThat;
import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.json.resource.test.assertj.AssertJActionResponseAssert.assertThat;
import static org.forgerock.json.resource.test.assertj.AssertJResourceResponseAssert.assertThat;
import static org.mockito.BDDMockito.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;

import com.iplanet.dpro.session.Session;
import com.iplanet.dpro.session.SessionID;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenID;
import com.sun.identity.delegation.DelegationEvaluator;
import com.sun.identity.delegation.DelegationException;
import com.sun.identity.delegation.DelegationPermission;
import com.sun.identity.delegation.DelegationPermissionFactory;
import org.forgerock.authz.filter.crest.AuthorizationFilters;
import org.forgerock.authz.filter.crest.api.CrestAuthorizationModule;
import org.forgerock.openam.core.CoreWrapper;
import org.forgerock.openam.session.SessionCache;
import org.forgerock.services.context.Context;
import org.forgerock.http.routing.RoutingMode;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.CollectionResourceProvider;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.FilterChain;
import org.forgerock.json.resource.ForbiddenException;
import org.forgerock.json.resource.PatchOperation;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.Responses;
import org.forgerock.json.resource.Router;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openam.rest.RealmContext;
import org.forgerock.openam.rest.resource.SubjectContext;
import org.forgerock.util.Function;
import org.forgerock.util.promise.NeverThrowsException;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.promise.Promises;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Unit test for {@link org.forgerock.openam.rest.authz.PrivilegeAuthzModule}.
 *
 * @since 12.0.0
 */
public class PrivilegeAuthzModuleTest {

    private static final Function<String, String, NeverThrowsException> DUMB_FUNC =
            new Function<String, String, NeverThrowsException>() {

                @Override
                public String apply(String ignore) throws NeverThrowsException {
                    return "ou=abc";
                }

            };

    private static final Map<String, String> EXTENSIONS = Collections.emptyMap();
    private static final Map<String, Set<String>> ENVIRONMENT = Collections.emptyMap();

    @Mock
    private DelegationPermissionFactory factory;
    @Mock
    private DelegationEvaluator evaluator;
    @Mock
    private SubjectContext subjectContext;
    @Mock
    private SSOToken token;
    @Mock
    private SSOTokenID tokenID;
    @Mock
    private CollectionResourceProvider provider;
    @Mock
    private CoreWrapper coreWrapper;
    @Mock
    private Session session;
    @Mock
    private SessionCache sessionCache;

    private CrestAuthorizationModule module;

    @BeforeMethod
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        final Map<String, PrivilegeDefinition> definitions = new HashMap<>();
        definitions.put("evaluate", PrivilegeDefinition.getInstance("evaluate", PrivilegeDefinition.Action.READ));
        definitions.put("blowup", PrivilegeDefinition.getInstance("destroy", PrivilegeDefinition.Action.MODIFY));

        given(session.getClientDomain()).willReturn("/abc");
        given(token.getTokenID()).willReturn(tokenID);
        given(coreWrapper.convertOrgNameToRealmName("realmdn")).willReturn("/abc");
        given(sessionCache.getSession(any(SessionID.class))).willReturn(session);

        module = new PrivilegeAuthzModule(evaluator, definitions, factory, sessionCache, coreWrapper);

        Session session = mock(Session.class);
        given(subjectContext.getCallerSession()).willReturn(session);
        given(session.getClientDomain()).willReturn("realmdn");
    }

    @Test
    public void crestReadIsAllowed() throws SSOException, DelegationException {
        // Given...
        final Set<String> actions = new HashSet<>(Arrays.asList("READ"));
        final DelegationPermission permission = new DelegationPermission(
                "/abc", "rest", "1.0", "policies", "read", actions, EXTENSIONS, DUMB_FUNC);
        given(factory.newInstance("/abc", "rest", "1.0", "policies", "read", actions, EXTENSIONS))
                .willReturn(permission);

        given(subjectContext.getCallerSSOToken()).willReturn(token);
        given(evaluator.isAllowed(token, permission, ENVIRONMENT)).willReturn(true);

        JsonValue jsonValue = json(object(field("someKey", "someValue")));
        Promise<ResourceResponse, ResourceException> promise = Promises
                .newResultPromise(Responses.newResourceResponse("1", "1.0", jsonValue));
        given(provider.readInstance(isA(Context.class), eq("123"), isA(ReadRequest.class))).willReturn(promise);

        // When...
        final FilterChain chain = AuthorizationFilters.createAuthorizationFilter(provider, module);
        final Router router = new Router();
        router.addRoute(RoutingMode.STARTS_WITH, Router.uriTemplate("/policies"), chain);
        final RealmContext context = new RealmContext(subjectContext);
        final ReadRequest request = Requests.newReadRequest("/policies/123");
        context.setSubRealm("abc", "abc");
        Promise<ResourceResponse, ResourceException> result = router.handleRead(context, request);

        // Then...
        assertThat(result).succeeded().withContent().stringAt("someKey").isEqualTo("someValue");
    }

    @Test
    public void crestQueryIsAllowed() throws SSOException, DelegationException, ResourceException {
        // Given...
        final Set<String> actions = new HashSet<>(Arrays.asList("READ"));
        final DelegationPermission permission = new DelegationPermission(
                "/abc", "rest", "1.0", "policies", "read", actions, EXTENSIONS, DUMB_FUNC);
        given(factory.newInstance("/abc", "rest", "1.0", "policies", "read", actions, EXTENSIONS))
                .willReturn(permission);

        given(subjectContext.getCallerSSOToken()).willReturn(token);
        given(evaluator.isAllowed(eq(token), eq(permission), eq(ENVIRONMENT))).willReturn(true);

        QueryResourceHandler handler = mock(QueryResourceHandler.class);
        Promise<QueryResponse, ResourceException> promise = Promises.newResultPromise(Responses.newQueryResponse("abc-def"));
        given(provider.queryCollection(isA(Context.class), isA(QueryRequest.class), isA(QueryResourceHandler.class)))
                .willReturn(promise);

        // When...
        final FilterChain chain = AuthorizationFilters.createAuthorizationFilter(provider, module);
        final Router router = new Router();
        router.addRoute(RoutingMode.STARTS_WITH, Router.uriTemplate("/policies"), chain);

        final RealmContext context = new RealmContext(subjectContext);
        context.setSubRealm("abc", "abc");
        final QueryRequest request = Requests.newQueryRequest("/policies");
        Promise<QueryResponse, ResourceException> result = router.handleQuery(context, request, handler);

        // Then...
        QueryResponse response = result.getOrThrowUninterruptibly();
        assertThat(response.getPagedResultsCookie()).isEqualTo("abc-def");
    }

    @Test
    public void crestCreateIsAllowed() throws SSOException, DelegationException {
        // Given...
        final Set<String> actions = new HashSet<>(Arrays.asList("MODIFY"));
        final DelegationPermission permission = new DelegationPermission(
                "/abc", "rest", "1.0", "policies", "modify", actions, EXTENSIONS, DUMB_FUNC);
        given(factory.newInstance("/abc", "rest", "1.0", "policies", "modify", actions, EXTENSIONS))
                .willReturn(permission);

        given(subjectContext.getCallerSSOToken()).willReturn(token);
        given(evaluator.isAllowed(eq(token), eq(permission), eq(ENVIRONMENT))).willReturn(true);

        JsonValue jsonValue = json(object(field("someKey", "someValue")));
        Promise<ResourceResponse, ResourceException> promise = Promises
                .newResultPromise(Responses.newResourceResponse("1", "1.0", jsonValue));
        given(provider.createInstance(isA(Context.class), isA(CreateRequest.class))).willReturn(promise);

        // When...
        final FilterChain chain = AuthorizationFilters.createAuthorizationFilter(provider, module);
        final Router router = new Router();
        router.addRoute(RoutingMode.STARTS_WITH, Router.uriTemplate("/policies"), chain);

        final RealmContext context = new RealmContext(subjectContext);
        context.setSubRealm("abc", "abc");
        final CreateRequest request = Requests.newCreateRequest("/policies", JsonValue.json(new Object()));
        Promise<ResourceResponse, ResourceException> result = router.handleCreate(context, request);

        // Then...
        assertThat(result).succeeded().withContent().stringAt("someKey").isEqualTo("someValue");
    }

    @Test
    public void crestUpdateIsAllowed() throws SSOException, DelegationException {
        // Given...
        final Set<String> actions = new HashSet<>(Arrays.asList("MODIFY"));
        final DelegationPermission permission = new DelegationPermission(
                "/abc", "rest", "1.0", "policies", "modify", actions, EXTENSIONS, DUMB_FUNC);
        given(factory.newInstance("/abc", "rest", "1.0", "policies", "modify", actions, EXTENSIONS))
                .willReturn(permission);

        given(subjectContext.getCallerSSOToken()).willReturn(token);
        given(evaluator.isAllowed(eq(token), eq(permission), eq(ENVIRONMENT))).willReturn(true);

        JsonValue jsonValue = json(object(field("someKey", "someValue")));
        Promise<ResourceResponse, ResourceException> promise = Promises
                .newResultPromise(Responses.newResourceResponse("1", "1.0", jsonValue));
        given(provider.updateInstance(isA(Context.class), eq("123"), isA(UpdateRequest.class))).willReturn(promise);

        // When...
        final FilterChain chain = AuthorizationFilters.createAuthorizationFilter(provider, module);
        final Router router = new Router();
        router.addRoute(RoutingMode.STARTS_WITH, Router.uriTemplate("/policies"), chain);

        final RealmContext context = new RealmContext(subjectContext);
        context.setSubRealm("abc", "abc");
        final UpdateRequest request = Requests.newUpdateRequest("/policies/123", JsonValue.json(new Object()));
        Promise<ResourceResponse, ResourceException> result = router.handleUpdate(context, request);

        // Then...
        assertThat(result).succeeded().withContent().stringAt("someKey").isEqualTo("someValue");
    }

    @Test
    public void crestDeleteIsAllowed() throws SSOException, DelegationException {
        // Given...
        final Set<String> actions = new HashSet<>(Arrays.asList("MODIFY"));
        final DelegationPermission permission = new DelegationPermission(
                "/abc", "rest", "1.0", "policies", "modify", actions, EXTENSIONS, DUMB_FUNC);
        given(factory.newInstance("/abc", "rest", "1.0", "policies", "modify", actions, EXTENSIONS))
                .willReturn(permission);

        given(subjectContext.getCallerSSOToken()).willReturn(token);
        given(evaluator.isAllowed(eq(token), eq(permission), eq(ENVIRONMENT))).willReturn(true);

        JsonValue jsonValue = json(object(field("someKey", "someValue")));
        Promise<ResourceResponse, ResourceException> promise = Promises
                .newResultPromise(Responses.newResourceResponse("1", "1.0", jsonValue));
        given(provider.deleteInstance(isA(Context.class), eq("123"), isA(DeleteRequest.class))).willReturn(promise);

        // When...
        final FilterChain chain = AuthorizationFilters.createAuthorizationFilter(provider, module);
        final Router router = new Router();
        router.addRoute(RoutingMode.STARTS_WITH, Router.uriTemplate("/policies"), chain);

        final RealmContext context = new RealmContext(subjectContext);
        context.setSubRealm("abc", "abc");
        final DeleteRequest request = Requests.newDeleteRequest("/policies/123");
        Promise<ResourceResponse, ResourceException> result = router.handleDelete(context, request);

        // Then...
        assertThat(result).succeeded().withContent().stringAt("someKey").isEqualTo("someValue");
    }

    @Test
    public void crestPatchIsAllowed() throws SSOException, DelegationException {
        // Given...
        final Set<String> actions = new HashSet<>(Arrays.asList("MODIFY"));
        final DelegationPermission permission = new DelegationPermission(
                "/abc", "rest", "1.0", "policies", "modify", actions, EXTENSIONS, DUMB_FUNC);
        given(factory.newInstance("/abc", "rest", "1.0", "policies", "modify", actions, EXTENSIONS))
                .willReturn(permission);

        given(subjectContext.getCallerSSOToken()).willReturn(token);
        given(evaluator.isAllowed(eq(token), eq(permission), eq(ENVIRONMENT))).willReturn(true);

        JsonValue jsonValue = json(object(field("someKey", "someValue")));
        Promise<ResourceResponse, ResourceException> promise = Promises
                .newResultPromise(Responses.newResourceResponse("1", "1.0", jsonValue));
        given(provider.patchInstance(isA(Context.class), eq("123"), isA(PatchRequest.class))).willReturn(promise);

        // When...
        final FilterChain chain = AuthorizationFilters.createAuthorizationFilter(provider, module);
        final Router router = new Router();
        router.addRoute(RoutingMode.STARTS_WITH, Router.uriTemplate("/policies"), chain);

        final RealmContext context = new RealmContext(subjectContext);
        context.setSubRealm("abc", "abc");
        final PatchRequest request = Requests.newPatchRequest("/policies/123", PatchOperation.add("abc", "123"));
        Promise<ResourceResponse, ResourceException> result = router.handlePatch(context, request);

        // Then...
        assertThat(result).succeeded().withContent().stringAt("someKey").isEqualTo("someValue");
    }

    @Test
    public void crestActionEvaluateIsAllowed() throws SSOException, DelegationException {
        // Given...
        final Set<String> actions = new HashSet<>(Arrays.asList("READ"));
        final DelegationPermission permission = new DelegationPermission(
                "/abc", "rest", "1.0", "policies", "evaluate", actions, EXTENSIONS, DUMB_FUNC);
        given(factory.newInstance("/abc", "rest", "1.0", "policies", "evaluate", actions, EXTENSIONS))
                .willReturn(permission);

        given(subjectContext.getCallerSSOToken()).willReturn(token);
        given(evaluator.isAllowed(eq(token), eq(permission), eq(ENVIRONMENT))).willReturn(true);

        JsonValue jsonValue = json(object(field("someKey", "someValue")));
        Promise<ActionResponse, ResourceException> promise = Promises
                .newResultPromise(Responses.newActionResponse(jsonValue));
        given(provider.actionCollection(isA(Context.class), isA(ActionRequest.class))).willReturn(promise);

        // When...
        final FilterChain chain = AuthorizationFilters.createAuthorizationFilter(provider, module);
        final Router router = new Router();
        router.addRoute(RoutingMode.STARTS_WITH, Router.uriTemplate("/policies"), chain);

        final RealmContext context = new RealmContext(subjectContext);
        context.setSubRealm("abc", "abc");
        final ActionRequest request = Requests.newActionRequest("/policies", "evaluate");
        Promise<ActionResponse, ResourceException> result = router.handleAction(context, request);

        // Then...
        assertThat(result).succeeded().withContent().stringAt("someKey").isEqualTo("someValue");
    }

    @Test
    public void crestActionBlowupIsAllowed() throws SSOException, DelegationException {
        // Given...
        final Set<String> actions = new HashSet<>(Arrays.asList("MODIFY"));
        final DelegationPermission permission = new DelegationPermission(
                "/abc", "rest", "1.0", "policies", "destroy", actions, EXTENSIONS, DUMB_FUNC);
        given(factory.newInstance("/abc", "rest", "1.0", "policies", "destroy", actions, EXTENSIONS))
                .willReturn(permission);

        given(subjectContext.getCallerSSOToken()).willReturn(token);
        given(evaluator.isAllowed(eq(token), eq(permission), eq(ENVIRONMENT))).willReturn(true);

        JsonValue jsonValue = json(object(field("someKey", "someValue")));
        Promise<ActionResponse, ResourceException> promise = Promises
                .newResultPromise(Responses.newActionResponse(jsonValue));
        given(provider.actionCollection(isA(Context.class), isA(ActionRequest.class))).willReturn(promise);

        // When...
        final FilterChain chain = AuthorizationFilters.createAuthorizationFilter(provider, module);
        final Router router = new Router();
        router.addRoute(RoutingMode.STARTS_WITH, Router.uriTemplate("/policies"), chain);

        final RealmContext context = new RealmContext(subjectContext);
        context.setSubRealm("abc", "abc");
        final ActionRequest request = Requests.newActionRequest("/policies", "blowup");
        Promise<ActionResponse, ResourceException> result = router.handleAction(context, request);

        // Then...
        assertThat(result).succeeded().withContent().stringAt("someKey").isEqualTo("someValue");
    }

    @Test
    public void crestActionNoMappingFails() throws SSOException, DelegationException {
        // When...
        final FilterChain chain = AuthorizationFilters.createAuthorizationFilter(provider, module);
        final Router router = new Router();
        router.addRoute(RoutingMode.STARTS_WITH, Router.uriTemplate("/policies"), chain);

        final Context context = new RealmContext(subjectContext);
        final ActionRequest request = Requests.newActionRequest("/policies", "unknownAction");
        Promise<ActionResponse, ResourceException> promise = router.handleAction(context, request);

        // Then...
        assertThat(promise).failedWithException().isInstanceOf(ForbiddenException.class);
    }

    @Test
    public void crestRequestNotAllowed() throws SSOException, DelegationException {
        // Given...
        final Set<String> actions = new HashSet<>(Arrays.asList("MODIFY"));
        final DelegationPermission permission = new DelegationPermission(
                "/abc", "rest", "1.0", "policies", "modify", actions, EXTENSIONS, DUMB_FUNC);
        given(factory.newInstance("/abc", "rest", "1.0", "policies", "modify", actions, EXTENSIONS))
                .willReturn(permission);

        given(subjectContext.getCallerSSOToken()).willReturn(token);
        given(evaluator.isAllowed(eq(token), eq(permission), eq(ENVIRONMENT))).willReturn(false);

        // When...
        final FilterChain chain = AuthorizationFilters.createAuthorizationFilter(provider, module);
        final Router router = new Router();
        router.addRoute(RoutingMode.STARTS_WITH, Router.uriTemplate("/policies"), chain);

        final RealmContext context = new RealmContext(subjectContext);
        context.setSubRealm("abc", "abc");
        final CreateRequest request = Requests.newCreateRequest("/policies", JsonValue.json(new Object()));
        Promise<ResourceResponse, ResourceException> promise = router.handleCreate(context, request);

        // Then...
        assertThat(promise).failedWithException().isInstanceOf(ForbiddenException.class);
    }

}
