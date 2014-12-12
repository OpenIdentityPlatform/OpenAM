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
import org.forgerock.json.resource.NotSupportedException;
import org.forgerock.json.resource.RequestType;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.openam.forgerockrest.entitlements.model.json.PolicyRequest;
import org.forgerock.openam.forgerockrest.guice.ForgerockRestGuiceModule;
import org.forgerock.openam.rest.resource.RealmContext;
import org.forgerock.openam.rest.resource.SubjectContext;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.security.auth.Subject;
import java.util.Arrays;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.isA;
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
    private PolicyRequestFactory requestFactory;
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
    @Mock
    private PolicyRequest policyRequest;

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

        policyResource = new PolicyResource(factory, requestFactory, parser,
                mock(PolicyStoreProvider.class), resourceErrorHandler);
    }

    @Test
    public void shouldMakeBatchEvaluation() throws EntitlementException {
        // Given...
        given(request.getAction()).willReturn("evaluate");

        ServerContext context = buildContextStructure("/abc");
        given(requestFactory.buildRequest(PolicyAction.EVALUATE, context, request)).willReturn(policyRequest);
        given(policyRequest.getRestSubject()).willReturn(restSubject);
        given(policyRequest.getApplication()).willReturn("some-application");
        given(factory.getEvaluator(restSubject, "some-application")).willReturn(evaluator);
        given(policyRequest.getApplication()).willReturn("some-application");
        given(policyRequest.getRealm()).willReturn("/abc");

        List<Entitlement> decisions = Arrays.asList(new Entitlement());
        given(evaluator.routePolicyRequest(policyRequest)).willReturn(decisions);

        JsonValue jsonDecision = JsonValue.json(new Object());
        given(parser.printEntitlements(decisions)).willReturn(jsonDecision);

        // When...
        policyResource.actionCollection(context, request, jsonHandler);

        // Then...
        verify(request).getAction();
        verify(requestFactory).buildRequest(PolicyAction.EVALUATE, context, request);
        verify(policyRequest).getRestSubject();
        verify(policyRequest, times(2)).getApplication();
        verify(policyRequest).getRealm();
        verify(factory).getEvaluator(restSubject, "some-application");
        verify(evaluator).routePolicyRequest(policyRequest);
        verify(parser).printEntitlements(decisions);
        verify(jsonHandler).handleResult(jsonDecision);
        verifyNoMoreInteractions(request, subjectContext, requestFactory,
                policyRequest, factory, evaluator, parser, jsonHandler);
    }

    @Test
    public void shouldMakeTreeEvaluation() throws EntitlementException {
        // Given...
        given(request.getAction()).willReturn("evaluateTree");

        ServerContext context = buildContextStructure("/abc");
        given(requestFactory.buildRequest(PolicyAction.TREE_EVALUATE, context, request)).willReturn(policyRequest);
        given(policyRequest.getRestSubject()).willReturn(restSubject);
        given(policyRequest.getApplication()).willReturn("some-application");
        given(factory.getEvaluator(restSubject, "some-application")).willReturn(evaluator);
        given(policyRequest.getApplication()).willReturn("some-application");
        given(policyRequest.getRealm()).willReturn("/abc");

        List<Entitlement> decisions = Arrays.asList(new Entitlement());
        given(evaluator.routePolicyRequest(policyRequest)).willReturn(decisions);

        JsonValue jsonDecision = JsonValue.json(new Object());
        given(parser.printEntitlements(decisions)).willReturn(jsonDecision);

        // When...
        policyResource.actionCollection(context, request, jsonHandler);

        // Then...
        verify(request).getAction();
        verify(requestFactory).buildRequest(PolicyAction.TREE_EVALUATE, context, request);
        verify(policyRequest).getRestSubject();
        verify(policyRequest, times(2)).getApplication();
        verify(policyRequest).getRealm();
        verify(factory).getEvaluator(restSubject, "some-application");
        verify(evaluator).routePolicyRequest(policyRequest);
        verify(parser).printEntitlements(decisions);
        verify(jsonHandler).handleResult(jsonDecision);
        verifyNoMoreInteractions(request, subjectContext, requestFactory,
                policyRequest, factory, evaluator, parser, jsonHandler);
    }

    @Test
    public void shouldHandleEntitlementExceptions() throws EntitlementException {
        // Given...
        given(request.getAction()).willReturn("evaluate");

        ServerContext context = buildContextStructure("/abc");
        EntitlementException eE = new EntitlementException(EntitlementException.INVALID_VALUE);
        given(requestFactory.buildRequest(PolicyAction.EVALUATE, context, request)).willThrow(eE);
        given(request.getRequestType()).willReturn(RequestType.ACTION);

        // When...
        policyResource.actionCollection(context, request, jsonHandler);

        // Then...
        verify(request).getAction();
        verify(requestFactory).buildRequest(PolicyAction.EVALUATE, context, request);
        verify(request).getRequestType();
        verify(jsonHandler).handleError(argThat(new ResourceExceptionMatcher(ResourceException.BAD_REQUEST)));
        verifyNoMoreInteractions(request, subjectContext, requestFactory,
                policyRequest, factory, evaluator, parser, jsonHandler);
    }

    @Test
    public void shouldHandleUnknownAction() {
        // Given...
        given(request.getAction()).willReturn("unknownAction");

        // When...
        ServerContext context = buildContextStructure("/abc");
        policyResource.actionCollection(context, request, jsonHandler);

        // Then...
        verify(request).getAction();
        verify(jsonHandler).handleError(isA(NotSupportedException.class));
        verifyNoMoreInteractions(request, subjectContext, requestFactory,
                policyRequest, factory, evaluator, parser, jsonHandler);

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
        return new ServerContext(new RealmContext(subjectContext));
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
