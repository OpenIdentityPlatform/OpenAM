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

package org.forgerock.openam.forgerockrest.entitlements.model.json;

import com.sun.identity.entitlement.Entitlement;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.JwtPrincipal;
import org.fest.assertions.Condition;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.jose.jws.JwsHeader;
import org.forgerock.json.jose.jws.SignedJwt;
import org.forgerock.json.jose.jws.handlers.NOPSigningHandler;
import org.forgerock.json.jose.jws.handlers.SigningHandler;
import org.forgerock.json.jose.jwt.Jwt;
import org.forgerock.json.jose.jwt.JwtClaimsSet;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.openam.forgerockrest.entitlements.PolicyEvaluator;
import org.forgerock.openam.rest.resource.RealmContext;
import org.forgerock.openam.rest.resource.SubjectContext;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.security.auth.Subject;
import java.security.Principal;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.fest.assertions.Assertions.assertThat;
import static org.forgerock.json.fluent.JsonValue.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Unit test for {@link PolicyRequest}.
 *
 * @since 12.0.0
 */
public class PolicyRequestTest {

    @Mock
    private SubjectContext subjectContext;
    @Mock
    private ActionRequest actionRequest;

    private Subject restSubject;
    private Subject policySubject;

    @BeforeMethod
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        restSubject = new Subject();
        policySubject = new Subject();
        policySubject.getPrincipals().add(mock(Principal.class));
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void shouldRejectNullContext() throws EntitlementException {
        MockRequest.getRequest(null, actionRequest);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void shouldRejectNullRequest() throws EntitlementException {
        ServerContext context = buildContextStructure("/abc");
        MockRequest.getRequest(context, null);
    }

    @Test
    public void shouldConstructPolicyRequest() throws EntitlementException {
        // Given...
        Map<String, List<String>> env = new HashMap<String, List<String>>();
        env.put("test", Arrays.asList("123", "456"));

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("subject", Collections.singletonMap("ssoToken", "some-value"));
        properties.put("application", "some-application");
        properties.put("environment", env);

        given(actionRequest.getContent()).willReturn(json(properties));
        given(subjectContext.getCallerSubject()).willReturn(restSubject);
        given(subjectContext.getSubject("some-value")).willReturn(policySubject);

        // When...
        ServerContext context = buildContextStructure("/abc");
        PolicyRequest request = MockRequest.getRequest(context, actionRequest);

        // Then...
        assertThat(request).isNotNull();
        assertThat(request.getRestSubject()).isEqualTo(restSubject);
        assertThat(request.getPolicySubject()).isEqualTo(policySubject);
        assertThat(request.getRealm()).isEqualTo("/abc");
        assertThat(request.getApplication()).isEqualTo("some-application");
        assertThat(request.getEnvironment()).is(new EnvMapCondition(env));

        verify(subjectContext).getCallerSubject();
        verify(actionRequest).getContent();
        verify(subjectContext).getSubject("some-value");
        verifyNoMoreInteractions(subjectContext, actionRequest);
    }

    @Test(expectedExceptions = EntitlementException.class)
    public void shouldRejectInvalidRestSubject() throws EntitlementException {
        // Given...
        given(subjectContext.getCallerSubject()).willReturn(null);

        // When...
        ServerContext context = buildContextStructure("/abc");
        MockRequest.getRequest(context, actionRequest);
    }

    @Test(expectedExceptions = EntitlementException.class)
    public void shouldRejectInvalidPolicySubject() throws EntitlementException {
        // Given...
        given(subjectContext.getCallerSubject()).willReturn(restSubject);

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("subject", "some-value");

        given(actionRequest.getContent()).willReturn(json(properties));
        given(subjectContext.getSubject("some-value")).willReturn(null);

        // When...
        ServerContext context = buildContextStructure("/abc");
        MockRequest.getRequest(context, actionRequest);
    }

    @Test
    public void shouldDefaultToAdminSubject() throws EntitlementException {
        // Given...
        given(subjectContext.getCallerSubject()).willReturn(restSubject);

        Map<String, Object> properties = new HashMap<String, Object>();
        given(actionRequest.getContent()).willReturn(json(properties));

        // When...
        ServerContext context = buildContextStructure("/abc");
        PolicyRequest request = MockRequest.getRequest(context, actionRequest);

        // Then...
        assertThat(request).isNotNull();
        assertThat(request.getRestSubject()).isEqualTo(restSubject);
        assertThat(request.getPolicySubject()).isEqualTo(restSubject);

        verify(subjectContext).getCallerSubject();
        verify(actionRequest).getContent();
        verifyNoMoreInteractions(subjectContext, actionRequest);
    }

    @Test
    public void shouldAllowJsonSubject() throws Exception {
        // Given
        final String subjectName = "test";
        given(subjectContext.getCallerSubject()).willReturn(restSubject);
        final JsonValue jwt = getJsonSubject(subjectName);
        given(actionRequest.getContent()).willReturn(json(object(field("subject", object(field("claims", jwt.asMap()))))));

        // When
        ServerContext context = buildContextStructure("/abc");
        PolicyRequest request = MockRequest.getRequest(context, actionRequest);

        // Then
        Subject policySubject = request.getPolicySubject();
        Set<JwtPrincipal> jwtPrincipals = policySubject.getPrincipals(JwtPrincipal.class);
        assertThat(jwtPrincipals).hasSize(1);
        assertThat(jwtPrincipals).contains(new JwtPrincipal(jwt));
    }

    @Test
    public void shouldAllowJwtSubject() throws Exception {
        // Given
        final String subjectName = "test";
        given(subjectContext.getCallerSubject()).willReturn(restSubject);
        Jwt jwt = getJwtSubject(subjectName);

        given(actionRequest.getContent()).willReturn(json(object(field("subject", object(field("jwt", jwt.build()))))));

        // When
        ServerContext context = buildContextStructure("/abc");
        PolicyRequest request = MockRequest.getRequest(context, actionRequest);

        // Then
        Subject policySubject = request.getPolicySubject();
        Set<JwtPrincipal> jwtPrincipals = policySubject.getPrincipals(JwtPrincipal.class);
        assertThat(jwtPrincipals).hasSize(1);
        assertThat(jwtPrincipals).contains(new JwtPrincipal(getJsonSubject(subjectName)));

    }

    private Jwt getJwtSubject(final String subjectName) {
        JwsHeader header = new JwsHeader(Collections.<String, Object>emptyMap());
        JwtClaimsSet claims = new JwtClaimsSet();
        claims.setSubject(subjectName);

        SigningHandler handler = new NOPSigningHandler();
        return new SignedJwt(header, claims, handler);
    }

    private JsonValue getJsonSubject(final String subjectName) {
        return json(object(field("sub", subjectName)));
    }

    @Test
    public void shouldDefaultToApplication() throws EntitlementException {
        // Given...
        given(subjectContext.getCallerSubject()).willReturn(restSubject);

        Map<String, Object> properties = new HashMap<String, Object>();
        given(actionRequest.getContent()).willReturn(json(properties));

        // When...
        ServerContext context = buildContextStructure("/abc");
        PolicyRequest request = MockRequest.getRequest(context, actionRequest);

        // Then...
        assertThat(request).isNotNull();
        assertThat(request.getApplication()).isEqualTo("iPlanetAMWebAgentService");

        verify(subjectContext).getCallerSubject();
        verify(actionRequest).getContent();
        verifyNoMoreInteractions(subjectContext, actionRequest);
    }

    @Test
    public void shouldDefaultToRealm() throws EntitlementException {
        // Given...
        given(subjectContext.getCallerSubject()).willReturn(restSubject);

        Map<String, Object> properties = new HashMap<String, Object>();
        given(actionRequest.getContent()).willReturn(json(properties));

        // When...
        ServerContext context = buildContextStructure("");
        PolicyRequest request = MockRequest.getRequest(context, actionRequest);

        // Then...
        assertThat(request).isNotNull();
        assertThat(request.getRealm()).isEqualTo("/");

        verify(subjectContext).getCallerSubject();
        verify(actionRequest).getContent();
        verifyNoMoreInteractions(subjectContext, actionRequest);
    }

    @Test
    public void shouldDefaultToEmptyEnvironment() throws EntitlementException {
        // Given...
        given(subjectContext.getCallerSubject()).willReturn(restSubject);

        Map<String, Object> properties = new HashMap<String, Object>();
        given(actionRequest.getContent()).willReturn(json(properties));

        // When...
        ServerContext context = buildContextStructure("");
        PolicyRequest request = MockRequest.getRequest(context, actionRequest);

        // Then...
        assertThat(request).isNotNull();
        assertThat(request.getEnvironment()).isNotNull();
        assertThat(request.getEnvironment()).isEmpty();

        verify(subjectContext).getCallerSubject();
        verify(actionRequest).getContent();
        verifyNoMoreInteractions(subjectContext, actionRequest);
    }

    private ServerContext buildContextStructure(final String realm) {
        RealmContext realmContext = new RealmContext(subjectContext);
        realmContext.addSubRealm(realm, realm);
        return new ServerContext(realmContext);
    }

    /**
     * Matches the expected environment map with the passed map.
     * <p/>
     * Expects the maps to be in the form Map<String, Set<String>>.
     */
    private final static class EnvMapCondition extends Condition<Map<?, ?>> {

        private final Map<?, ?> map;

        public EnvMapCondition(final Map<?, ?> map) {
            this.map = map;
        }

        @Override
        public boolean matches(final Map<?, ?> otherMap) {
            if (map == null && otherMap == null) {
                return true;
            }

            if (map == null || otherMap == null) {
                return false;
            }

            if (map.size() != otherMap.size()) {
                return false;
            }

            return matches(map, otherMap);
        }

        private boolean matches(final Map<?, ?> map, final Map<?, ?> otherMap) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {

                if (!otherMap.containsKey(entry.getKey())) {
                    return false;
                }

                final Object otherValue = otherMap.get(entry.getKey());

                if (!(entry.getValue() instanceof Collection)) {
                    return false;
                }

                if (!(otherValue instanceof Collection)) {
                    return false;
                }

                final Collection<?> collection = (Collection<?>)entry.getValue();
                final Collection<?> otherCollection = (Collection<?>)otherValue;

                if (collection.size() != otherCollection.size()) {
                    return false;
                }

                if (!matches(collection, otherCollection)) {
                    return false;
                }
            }

            return true;
        }

        private boolean matches(final Collection<?> collection, final Collection<?> otherCollection) {
            for (Object value : collection) {
                if (!otherCollection.contains(value)) {
                    return false;
                }
            }

            return true;
        }

    }

    /**
     * Concrete mock implementation of {@link PolicyRequest} for the aid of testing.
     */
    private static final class MockRequest extends PolicyRequest {

        private MockRequest(MockBuilder builder) {
            super(builder);
        }

        @Override
        public List<Entitlement> dispatch(PolicyEvaluator evaluator) throws EntitlementException {
            throw new UnsupportedOperationException("Not required for this test");
        }

        private static final class MockBuilder extends PolicyRequestBuilder<PolicyRequest> {

            MockBuilder(ServerContext context, ActionRequest request) throws EntitlementException {
                super(context, request);
            }

            @Override
            PolicyRequest build() {
                return new MockRequest(this);
            }
        }

        public static PolicyRequest getRequest(ServerContext context, ActionRequest request) throws EntitlementException {
            return new MockBuilder(context, request).build();
        }

    }

}
