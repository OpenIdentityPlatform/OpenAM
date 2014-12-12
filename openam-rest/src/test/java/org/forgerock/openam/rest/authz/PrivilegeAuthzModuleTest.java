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

package org.forgerock.openam.rest.authz;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.delegation.DelegationEvaluator;
import com.sun.identity.delegation.DelegationException;
import com.sun.identity.delegation.DelegationPermission;
import com.sun.identity.delegation.DelegationPermissionFactory;
import org.fest.assertions.Assertions;
import org.forgerock.authz.filter.crest.AuthorizationFilters;
import org.forgerock.authz.filter.crest.api.CrestAuthorizationModule;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.CollectionResourceProvider;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.FilterChain;
import org.forgerock.json.resource.PatchOperation;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResultHandler;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.Router;
import org.forgerock.json.resource.RoutingMode;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openam.rest.resource.RealmContext;
import org.forgerock.openam.rest.resource.SubjectContext;
import org.forgerock.util.promise.Function;
import org.forgerock.util.promise.NeverThrowsException;
import org.mockito.ArgumentCaptor;
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

import static org.mockito.BDDMockito.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.isA;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

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
    private CollectionResourceProvider provider;
    @Mock
    private ResultHandler<Resource> handler;
    @Mock
    private ResultHandler<JsonValue> jsonHandler;

    private CrestAuthorizationModule module;

    @BeforeMethod
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        final Map<String, PrivilegeDefinition> definitions = new HashMap<String, PrivilegeDefinition>();
        definitions.put("evaluate", PrivilegeDefinition.getInstance("evaluate", PrivilegeDefinition.Action.READ));
        definitions.put("blowup", PrivilegeDefinition.getInstance("destroy", PrivilegeDefinition.Action.MODIFY));
        module = new PrivilegeAuthzModule(evaluator, definitions, factory);
    }

    @Test
    public void crestReadIsAllowed() throws SSOException, DelegationException {
        // Given...
        final Set<String> actions = new HashSet<String>(Arrays.asList("READ"));
        final DelegationPermission permission = new DelegationPermission(
                "/abc", "rest", "1.0", "policies", "read", actions, EXTENSIONS, DUMB_FUNC);
        given(factory.newInstance("/abc", "rest", "1.0", "policies", "read", actions, EXTENSIONS))
                .willReturn(permission);

        given(subjectContext.getCallerSSOToken()).willReturn(token);
        given(evaluator.isAllowed(eq(token), eq(permission), eq(ENVIRONMENT))).willReturn(true);

        // When...
        final FilterChain chain = AuthorizationFilters.createFilter(provider, module);
        final Router router = new Router();
        router.addRoute(RoutingMode.STARTS_WITH, "/policies", chain);

        final RealmContext context = new RealmContext(subjectContext);
        context.addSubRealm("abc", "abc");
        final ReadRequest request = Requests.newReadRequest("/policies/123");
        router.handleRead(context, request, handler);

        // Then...
        verify(factory).newInstance("/abc", "rest", "1.0", "policies", "read", actions, EXTENSIONS);
        verify(subjectContext).getCallerSSOToken();
        verify(evaluator).isAllowed(token, permission, ENVIRONMENT);
        // Required by the following verify.
        given(subjectContext.toJsonValue()).willReturn(JsonValue.json(new Object()));
        verify(provider).readInstance(isA(ServerContext.class), eq("123"), isA(ReadRequest.class), same(handler));
        verifyNoMoreInteractions(factory, subjectContext, evaluator, handler, token, provider);
    }

    @Test
    public void crestQueryIsAllowed() throws SSOException, DelegationException {
        // Given...
        final Set<String> actions = new HashSet<String>(Arrays.asList("READ"));
        final DelegationPermission permission = new DelegationPermission(
                "/abc", "rest", "1.0", "policies", "read", actions, EXTENSIONS, DUMB_FUNC);
        given(factory.newInstance("/abc", "rest", "1.0", "policies", "read", actions, EXTENSIONS))
                .willReturn(permission);

        given(subjectContext.getCallerSSOToken()).willReturn(token);
        given(evaluator.isAllowed(eq(token), eq(permission), eq(ENVIRONMENT))).willReturn(true);

        // When...
        final FilterChain chain = AuthorizationFilters.createFilter(provider, module);
        final Router router = new Router();
        router.addRoute(RoutingMode.STARTS_WITH, "/policies", chain);

        final RealmContext context = new RealmContext(subjectContext);
        context.addSubRealm("abc", "abc");
        final QueryRequest request = Requests.newQueryRequest("/policies");
        final QueryResultHandler handler = mock(QueryResultHandler.class);
        router.handleQuery(context, request, handler);

        // Then...
        verify(factory).newInstance("/abc", "rest", "1.0", "policies", "read", actions, EXTENSIONS);
        verify(subjectContext).getCallerSSOToken();
        verify(evaluator).isAllowed(token, permission, ENVIRONMENT);
        // Required by the following verify.
        given(subjectContext.toJsonValue()).willReturn(JsonValue.json(new Object()));
        verify(provider).queryCollection(isA(ServerContext.class), isA(QueryRequest.class), same(handler));
        verifyNoMoreInteractions(factory, subjectContext, evaluator, handler, token, provider);
    }

    @Test
    public void crestCreateIsAllowed() throws SSOException, DelegationException {
        // Given...
        final Set<String> actions = new HashSet<String>(Arrays.asList("MODIFY"));
        final DelegationPermission permission = new DelegationPermission(
                "/abc", "rest", "1.0", "policies", "modify", actions, EXTENSIONS, DUMB_FUNC);
        given(factory.newInstance("/abc", "rest", "1.0", "policies", "modify", actions, EXTENSIONS))
                .willReturn(permission);

        given(subjectContext.getCallerSSOToken()).willReturn(token);
        given(evaluator.isAllowed(eq(token), eq(permission), eq(ENVIRONMENT))).willReturn(true);

        // When...
        final FilterChain chain = AuthorizationFilters.createFilter(provider, module);
        final Router router = new Router();
        router.addRoute(RoutingMode.STARTS_WITH, "/policies", chain);

        final RealmContext context = new RealmContext(subjectContext);
        context.addSubRealm("abc", "abc");
        final CreateRequest request = Requests.newCreateRequest("/policies", JsonValue.json(new Object()));
        router.handleCreate(context, request, handler);

        // Then...
        verify(factory).newInstance("/abc", "rest", "1.0", "policies", "modify", actions, EXTENSIONS);
        verify(subjectContext).getCallerSSOToken();
        verify(evaluator).isAllowed(token, permission, ENVIRONMENT);
        // Required by the following verify.
        given(subjectContext.toJsonValue()).willReturn(JsonValue.json(new Object()));
        verify(provider).createInstance(isA(ServerContext.class), isA(CreateRequest.class), same(handler));
        verifyNoMoreInteractions(factory, subjectContext, evaluator, handler, token, provider);
    }

    @Test
    public void crestUpdateIsAllowed() throws SSOException, DelegationException {
        // Given...
        final Set<String> actions = new HashSet<String>(Arrays.asList("MODIFY"));
        final DelegationPermission permission = new DelegationPermission(
                "/abc", "rest", "1.0", "policies", "modify", actions, EXTENSIONS, DUMB_FUNC);
        given(factory.newInstance("/abc", "rest", "1.0", "policies", "modify", actions, EXTENSIONS))
                .willReturn(permission);

        given(subjectContext.getCallerSSOToken()).willReturn(token);
        given(evaluator.isAllowed(eq(token), eq(permission), eq(ENVIRONMENT))).willReturn(true);

        // When...
        final FilterChain chain = AuthorizationFilters.createFilter(provider, module);
        final Router router = new Router();
        router.addRoute(RoutingMode.STARTS_WITH, "/policies", chain);

        final RealmContext context = new RealmContext(subjectContext);
        context.addSubRealm("abc", "abc");
        final UpdateRequest request = Requests.newUpdateRequest("/policies/123", JsonValue.json(new Object()));
        router.handleUpdate(context, request, handler);

        // Then...
        verify(factory).newInstance("/abc", "rest", "1.0", "policies", "modify", actions, EXTENSIONS);
        verify(subjectContext).getCallerSSOToken();
        verify(evaluator).isAllowed(token, permission, ENVIRONMENT);
        // Required by the following verify.
        given(subjectContext.toJsonValue()).willReturn(JsonValue.json(new Object()));
        verify(provider).updateInstance(isA(ServerContext.class), eq("123"), isA(UpdateRequest.class), same(handler));
        verifyNoMoreInteractions(factory, subjectContext, evaluator, handler, token, provider);
    }

    @Test
    public void crestDeleteIsAllowed() throws SSOException, DelegationException {
        // Given...
        final Set<String> actions = new HashSet<String>(Arrays.asList("MODIFY"));
        final DelegationPermission permission = new DelegationPermission(
                "/abc", "rest", "1.0", "policies", "modify", actions, EXTENSIONS, DUMB_FUNC);
        given(factory.newInstance("/abc", "rest", "1.0", "policies", "modify", actions, EXTENSIONS))
                .willReturn(permission);

        given(subjectContext.getCallerSSOToken()).willReturn(token);
        given(evaluator.isAllowed(eq(token), eq(permission), eq(ENVIRONMENT))).willReturn(true);

        // When...
        final FilterChain chain = AuthorizationFilters.createFilter(provider, module);
        final Router router = new Router();
        router.addRoute(RoutingMode.STARTS_WITH, "/policies", chain);

        final RealmContext context = new RealmContext(subjectContext);
        context.addSubRealm("abc", "abc");
        final DeleteRequest request = Requests.newDeleteRequest("/policies/123");
        router.handleDelete(context, request, handler);

        // Then...
        verify(factory).newInstance("/abc", "rest", "1.0", "policies", "modify", actions, EXTENSIONS);
        verify(subjectContext).getCallerSSOToken();
        verify(evaluator).isAllowed(token, permission, ENVIRONMENT);
        // Required by the following verify.
        given(subjectContext.toJsonValue()).willReturn(JsonValue.json(new Object()));
        verify(provider).deleteInstance(isA(ServerContext.class), eq("123"), isA(DeleteRequest.class), same(handler));
        verifyNoMoreInteractions(factory, subjectContext, evaluator, handler, token, provider);
    }

    @Test
    public void crestPatchIsAllowed() throws SSOException, DelegationException {
        // Given...
        final Set<String> actions = new HashSet<String>(Arrays.asList("MODIFY"));
        final DelegationPermission permission = new DelegationPermission(
                "/abc", "rest", "1.0", "policies", "modify", actions, EXTENSIONS, DUMB_FUNC);
        given(factory.newInstance("/abc", "rest", "1.0", "policies", "modify", actions, EXTENSIONS))
                .willReturn(permission);

        given(subjectContext.getCallerSSOToken()).willReturn(token);
        given(evaluator.isAllowed(eq(token), eq(permission), eq(ENVIRONMENT))).willReturn(true);

        // When...
        final FilterChain chain = AuthorizationFilters.createFilter(provider, module);
        final Router router = new Router();
        router.addRoute(RoutingMode.STARTS_WITH, "/policies", chain);

        final RealmContext context = new RealmContext(subjectContext);
        context.addSubRealm("abc", "abc");
        final PatchRequest request = Requests.newPatchRequest("/policies/123", PatchOperation.add("abc", "123"));
        router.handlePatch(context, request, handler);

        // Then...
        verify(factory).newInstance("/abc", "rest", "1.0", "policies", "modify", actions, EXTENSIONS);
        verify(subjectContext).getCallerSSOToken();
        verify(evaluator).isAllowed(token, permission, ENVIRONMENT);
        // Required by the following verify.
        given(subjectContext.toJsonValue()).willReturn(JsonValue.json(new Object()));
        verify(provider).patchInstance(isA(ServerContext.class), eq("123"), isA(PatchRequest.class), same(handler));
        verifyNoMoreInteractions(factory, subjectContext, evaluator, handler, token, provider);
    }

    @Test
    public void crestActionEvaluateIsAllowed() throws SSOException, DelegationException {
        // Given...
        final Set<String> actions = new HashSet<String>(Arrays.asList("READ"));
        final DelegationPermission permission = new DelegationPermission(
                "/abc", "rest", "1.0", "policies", "evaluate", actions, EXTENSIONS, DUMB_FUNC);
        given(factory.newInstance("/abc", "rest", "1.0", "policies", "evaluate", actions, EXTENSIONS))
                .willReturn(permission);

        given(subjectContext.getCallerSSOToken()).willReturn(token);
        given(evaluator.isAllowed(eq(token), eq(permission), eq(ENVIRONMENT))).willReturn(true);

        // When...
        final FilterChain chain = AuthorizationFilters.createFilter(provider, module);
        final Router router = new Router();
        router.addRoute(RoutingMode.STARTS_WITH, "/policies", chain);

        final RealmContext context = new RealmContext(subjectContext);
        context.addSubRealm("abc", "abc");
        final ActionRequest request = Requests.newActionRequest("/policies", "evaluate");
        router.handleAction(context, request, jsonHandler);

        // Then...
        verify(factory).newInstance("/abc", "rest", "1.0", "policies", "evaluate", actions, EXTENSIONS);
        verify(subjectContext).getCallerSSOToken();
        verify(evaluator).isAllowed(token, permission, ENVIRONMENT);
        // Required by the following verify.
        given(subjectContext.toJsonValue()).willReturn(JsonValue.json(new Object()));
        verify(provider).actionCollection(isA(ServerContext.class), isA(ActionRequest.class), same(jsonHandler));
        verifyNoMoreInteractions(factory, subjectContext, evaluator, jsonHandler, token, provider);
    }

    @Test
    public void crestActionBlowupIsAllowed() throws SSOException, DelegationException {
        // Given...
        final Set<String> actions = new HashSet<String>(Arrays.asList("MODIFY"));
        final DelegationPermission permission = new DelegationPermission(
                "/abc", "rest", "1.0", "policies", "destroy", actions, EXTENSIONS, DUMB_FUNC);
        given(factory.newInstance("/abc", "rest", "1.0", "policies", "destroy", actions, EXTENSIONS))
                .willReturn(permission);

        given(subjectContext.getCallerSSOToken()).willReturn(token);
        given(evaluator.isAllowed(eq(token), eq(permission), eq(ENVIRONMENT))).willReturn(true);

        // When...
        final FilterChain chain = AuthorizationFilters.createFilter(provider, module);
        final Router router = new Router();
        router.addRoute(RoutingMode.STARTS_WITH, "/policies", chain);

        final RealmContext context = new RealmContext(subjectContext);
        context.addSubRealm("abc", "abc");
        final ActionRequest request = Requests.newActionRequest("/policies", "blowup");
        router.handleAction(context, request, jsonHandler);

        // Then...
        verify(factory).newInstance("/abc", "rest", "1.0", "policies", "destroy", actions, EXTENSIONS);
        verify(subjectContext).getCallerSSOToken();
        verify(evaluator).isAllowed(token, permission, ENVIRONMENT);
        // Required by the following verify.
        given(subjectContext.toJsonValue()).willReturn(JsonValue.json(new Object()));
        verify(provider).actionCollection(isA(ServerContext.class), isA(ActionRequest.class), same(jsonHandler));
        verifyNoMoreInteractions(factory, subjectContext, evaluator, jsonHandler, token, provider);
    }

    @Test
    public void crestActionNoMappingFails() throws SSOException, DelegationException {
        // When...
        final FilterChain chain = AuthorizationFilters.createFilter(provider, module);
        final Router router = new Router();
        router.addRoute(RoutingMode.STARTS_WITH, "/policies", chain);

        final ServerContext context = new RealmContext(subjectContext);
        final ActionRequest request = Requests.newActionRequest("/policies", "unknownAction");
        router.handleAction(context, request, jsonHandler);

        // Then...
        final ArgumentCaptor<ResourceException> exceptionCapture = ArgumentCaptor.forClass(ResourceException.class);
        verify(jsonHandler).handleError(exceptionCapture.capture());
        final ResourceException exception = exceptionCapture.getValue();
        Assertions.assertThat(exception.getCode()).isEqualTo(ResourceException.FORBIDDEN);

        verifyNoMoreInteractions(factory, subjectContext, evaluator, jsonHandler, token, provider);
    }

    @Test
    public void crestRequestNotAllowed() throws SSOException, DelegationException {
        // Given...
        final Set<String> actions = new HashSet<String>(Arrays.asList("MODIFY"));
        final DelegationPermission permission = new DelegationPermission(
                "/abc", "rest", "1.0", "policies", "modify", actions, EXTENSIONS, DUMB_FUNC);
        given(factory.newInstance("/abc", "rest", "1.0", "policies", "modify", actions, EXTENSIONS))
                .willReturn(permission);

        given(subjectContext.getCallerSSOToken()).willReturn(token);
        given(evaluator.isAllowed(eq(token), eq(permission), eq(ENVIRONMENT))).willReturn(false);

        // When...
        final FilterChain chain = AuthorizationFilters.createFilter(provider, module);
        final Router router = new Router();
        router.addRoute(RoutingMode.STARTS_WITH, "/policies", chain);

        final RealmContext context = new RealmContext(subjectContext);
        context.addSubRealm("abc", "abc");
        final CreateRequest request = Requests.newCreateRequest("/policies", JsonValue.json(new Object()));
        router.handleCreate(context, request, handler);

        // Then...
        verify(factory).newInstance("/abc", "rest", "1.0", "policies", "modify", actions, EXTENSIONS);
        verify(subjectContext).getCallerSSOToken();
        verify(evaluator).isAllowed(token, permission, ENVIRONMENT);

        final ArgumentCaptor<ResourceException> exceptionCapture = ArgumentCaptor.forClass(ResourceException.class);
        verify(handler).handleError(exceptionCapture.capture());
        final ResourceException exception = exceptionCapture.getValue();
        Assertions.assertThat(exception.getCode()).isEqualTo(ResourceException.FORBIDDEN);

        verifyNoMoreInteractions(factory, subjectContext, evaluator, handler, token, provider);
    }

}
