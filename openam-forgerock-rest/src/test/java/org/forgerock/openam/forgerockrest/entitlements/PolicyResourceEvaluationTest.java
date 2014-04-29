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

package org.forgerock.openam.forgerockrest.entitlements;

import com.sun.identity.entitlement.Entitlement;
import com.sun.identity.entitlement.EntitlementException;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.RequestType;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.openam.forgerockrest.guice.ForgerockRestGuiceModule;
import org.forgerock.openam.rest.resource.RealmContext;
import org.forgerock.openam.rest.resource.SubjectContext;
import org.forgerock.openam.utils.CollectionUtils;
import org.forgerock.util.promise.Function;
import org.forgerock.util.promise.NeverThrowsException;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.security.auth.Subject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * Unit test for the evaluation logic within {@link PolicyResource}.
 *
 * @since 12.0.0
 */
public class PolicyResourceEvaluationTest {

    @Mock
    private PolicyEvaluatorFactory factory;
    @Mock
    private PolicyEvaluator evaluator;
    @Mock
    private PolicyParser parser;
    @Mock
    private ActionRequest request;
    @Mock
    private SubjectContext subjectContext;
    @Mock
    private ResultHandler<JsonValue> jsonHandler;

    private Subject restSubject;
    private Subject policySubject;

    private PolicyResource policyResource;

    @BeforeMethod
    public void setupMocks() throws Exception {
        MockitoAnnotations.initMocks(this);
        restSubject = new Subject();
        policySubject = new Subject();

        // Use a real error handler as this is a core part of the functionality we are testing and doesn't need to be mocked
        EntitlementsResourceErrorHandler resourceErrorHandler =
                new EntitlementsResourceErrorHandler(ForgerockRestGuiceModule.getEntitlementsErrorHandlers());

        policyResource = new PolicyResource(factory, parser,
                mock(PolicyStoreProvider.class), resourceErrorHandler);
    }

    @Test
    public void shouldEvaluatePolicyRequest() throws EntitlementException {
        // Given...
        given(request.getAction()).willReturn("evaluate");
        given(subjectContext.getCallerSubject()).willReturn(restSubject);

        List<String> resources = Arrays.asList("resource1", "resource2");

        Map<String, List<String>> env = new HashMap<String, List<String>>();
        env.put("test", Arrays.asList("123", "456"));

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("subject", "some-value");
        properties.put("resources", resources);
        properties.put("application", "some-application");
        properties.put("environment", env);

        given(request.getContent()).willReturn(JsonValue.json(properties));
        given(subjectContext.getSubject("some-value")).willReturn(policySubject);
        given(factory.getEvaluator(restSubject, "some-application")).willReturn(evaluator);

        Set<String> resourceSet = new HashSet<String>(resources);
        Map<String, Set<String>> envSet = CollectionUtils.transformMap(env, new ListToSetMapper());
        List<Entitlement> decisions = Arrays.asList(new Entitlement());
        given(evaluator.evaluate("/abc", policySubject, resourceSet, envSet)).willReturn(decisions);

        JsonValue jsonDecision = JsonValue.json(new Object());
        given(parser.printEntitlements(decisions)).willReturn(jsonDecision);

        // When...
        ServerContext context = buildContextStructure("/abc");
        policyResource.actionCollection(context, request, jsonHandler);

        // Then...
        verify(request).getAction();
        verify(subjectContext).getCallerSubject();
        verify(request).getContent();
        verify(subjectContext).getSubject("some-value");
        verify(factory).getEvaluator(restSubject, "some-application");
        verify(evaluator).evaluate("/abc", policySubject, resourceSet, envSet);
        verify(parser).printEntitlements(decisions);
        verify(jsonHandler).handleResult(jsonDecision);
        verifyNoMoreInteractions(request, subjectContext, jsonHandler, factory, evaluator, parser);
    }

    @Test
    public void shouldHandleUnauthenticatedSubjectError() throws EntitlementException {
        // Given...
        given(request.getAction()).willReturn("evaluate");
        given(subjectContext.getCallerSubject()).willReturn(null);
        given(request.getRequestType()).willReturn(RequestType.ACTION);

        // When...
        ServerContext context = buildContextStructure("/abc");
        policyResource.actionCollection(context, request, jsonHandler);

        // Then...
        verify(request).getAction();
        verify(subjectContext).getCallerSubject();
        verify(request).getRequestType();
        verify(jsonHandler).handleError(argThat(new ResourceExceptionMatcher(403)));
        verifyNoMoreInteractions(request, subjectContext, jsonHandler, factory, evaluator, parser);
    }

    @Test
    public void shouldHandleMissingPolicySubject() throws EntitlementException {
        // Given...
        given(request.getAction()).willReturn("evaluate");
        given(subjectContext.getCallerSubject()).willReturn(restSubject);

        List<String> resources = Arrays.asList("resource1", "resource2");

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("resources", resources);

        given(request.getContent()).willReturn(JsonValue.json(properties));
        given(factory.getEvaluator(restSubject, "iPlanetAMWebAgentService")).willReturn(evaluator);

        Set<String> resourceSet = new HashSet<String>(resources);
        Map<String, Set<String>> envSet = Collections.emptyMap();
        List<Entitlement> decisions = Arrays.asList(new Entitlement());
        given(evaluator.evaluate("/abc", policySubject, resourceSet, envSet)).willReturn(decisions);

        JsonValue jsonDecision = JsonValue.json(new Object());
        given(parser.printEntitlements(decisions)).willReturn(jsonDecision);

        // When...
        ServerContext context = buildContextStructure("/abc");
        policyResource.actionCollection(context, request, jsonHandler);

        // Then...
        verify(request).getAction();
        verify(subjectContext).getCallerSubject();
        verify(request).getContent();
        verify(factory).getEvaluator(restSubject, "iPlanetAMWebAgentService");
        verify(evaluator).evaluate("/abc", restSubject, resourceSet, envSet);
        verify(parser).printEntitlements(decisions);
        verify(jsonHandler).handleResult(jsonDecision);
        verifyNoMoreInteractions(request, subjectContext, jsonHandler, factory, evaluator, parser);
    }

    @Test
    public void shouldHandleInvalidPolicySubjectError() throws EntitlementException {
        // Given...
        given(request.getAction()).willReturn("evaluate");
        given(subjectContext.getCallerSubject()).willReturn(restSubject);

        List<String> resources = Arrays.asList("resource1", "resource2");

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("subject", "some-invalid-value");
        properties.put("resources", resources);

        given(request.getContent()).willReturn(JsonValue.json(properties));
        given(subjectContext.getSubject("some-invalid-value")).willReturn(null);
        given(request.getRequestType()).willReturn(RequestType.ACTION);

        // When...
        ServerContext context = buildContextStructure("/abc");
        policyResource.actionCollection(context, request, jsonHandler);

        // Then...
        verify(request).getAction();
        verify(subjectContext).getCallerSubject();
        verify(request).getContent();
        verify(subjectContext).getSubject("some-invalid-value");
        verify(request).getRequestType();
        verify(jsonHandler).handleError(argThat(new ResourceExceptionMatcher(400)));
        verifyNoMoreInteractions(request, subjectContext, jsonHandler, factory, evaluator, parser);
    }

    @Test
    public void shouldHandleMissingResourceError() {
        // Given...
        given(request.getAction()).willReturn("evaluate");
        given(subjectContext.getCallerSubject()).willReturn(restSubject);

        List<String> resources = new ArrayList<String>();

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("resources", resources);

        given(request.getContent()).willReturn(JsonValue.json(properties));
        given(request.getRequestType()).willReturn(RequestType.ACTION);

        // When...
        ServerContext context = buildContextStructure("/abc");
        policyResource.actionCollection(context, request, jsonHandler);

        // Then...
        verify(request).getAction();
        verify(subjectContext).getCallerSubject();
        verify(request).getContent();
        verify(request).getRequestType();
        verify(jsonHandler).handleError(argThat(new ResourceExceptionMatcher(400)));
        verifyNoMoreInteractions(request, subjectContext, jsonHandler, factory, evaluator, parser);

    }

    /**
     * Creates a server context hierarchy based on the passed realm.
     *
     * @param realm
     *         the realm
     *
     * @return the server context hierarchy
     */
    private ServerContext buildContextStructure(final String realm) {
        return new ServerContext(new RealmContext(subjectContext, realm));
    }

    /**
     * Mapper that maps lists to sets.
     */
    private final static class ListToSetMapper implements Function<List<String>, Set<String>, NeverThrowsException> {

        @Override
        public Set<String> apply(final List<String> value) {
            return new HashSet<String>(value);
        }

    }

    /**
     * Given a resource exception verifies that the code matches the expected code.
     */
    private final static class ResourceExceptionMatcher extends BaseMatcher<ResourceException> {

        private final int expectedCode;

        public ResourceExceptionMatcher(final int expectedCode) {
            this.expectedCode = expectedCode;
        }

        @Override
        public boolean matches(final Object o) {
            if (!(o instanceof ResourceException)) {
                return false;
            }

            ResourceException exception = (ResourceException)o;
            return exception.getCode() == expectedCode;
        }

        @Override
        public void describeTo(final Description description) {
            description.appendText("Expected resource exception code was " + expectedCode);
        }

    }
}
