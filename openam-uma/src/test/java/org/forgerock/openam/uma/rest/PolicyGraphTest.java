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

package org.forgerock.openam.uma.rest;

import static java.util.Arrays.*;
import static org.assertj.core.api.Assertions.*;
import static org.forgerock.json.JsonValue.*;
import static org.forgerock.openam.uma.UmaConstants.BackendPolicy.*;
import static org.forgerock.openam.uma.UmaConstants.UmaPolicy.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.anySet;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.MockitoAnnotations.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.forgerock.services.context.Context;
import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.Responses;
import org.forgerock.oauth2.resources.ResourceSetDescription;
import org.forgerock.oauth2.resources.ResourceSetStore;
import org.forgerock.openam.oauth2.resources.ResourceSetStoreFactory;
import org.forgerock.openam.uma.UmaPolicyUtils;
import org.forgerock.openam.uma.rest.PolicyGraph;
import org.forgerock.openam.uma.rest.PolicyResourceDelegate;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.promise.Promises;
import org.forgerock.util.test.assertj.AssertJPromiseAssert;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * The tests in this class are centred around a Policy Graph as follows:
 *
 * <pre>
 *     +--------- EDIT ----------- Alice ------- VIEW, DELETE ---+
 *     |                             |                           |
 *     |                           VIEW                          |
 *     |                             |                           |
 *     V                             V                           V
 *    Bob ---- DELETE, EDIT ----> Charlie                      Dave
 *     ^                             |                           |
 *     |                           VIEW                          |
 *     |                             |                           |
 *     |                             V                           |
 *     +---------- DELETE --------- Ed <----- VIEW, DELETE ------+
 * </pre>
 * For all tests, this will be the current graph that will be computed from
 * the policies, and where updates are required, the graph will be making
 * the policies match this graph.
 */
public class PolicyGraphTest {

    private static final String ALICE = "Alice";
    private static final String BOB = "Bob";
    private static final String CHARLIE = "Charlie";
    private static final String DAVE = "Dave";
    private static final String ED = "Ed";
    private static final String VIEW = "VIEW";
    private static final String EDIT = "EDIT";
    private static final String DELETE = "DELETE";
    private static final String RESOURCE_SET_ID = "RESOURCE_SET_ID";
    private static final ResourceSetDescription RESOURCE_SET =
            new ResourceSetDescription(RESOURCE_SET_ID, "CLIENT_ID", ALICE, null);
    private static final List<ResourceResponse> VALID_POLICIES = asList(
            makePolicy(ALICE, BOB, true, EDIT),
            makePolicy(ALICE, CHARLIE, true, VIEW),
            makePolicy(ALICE, DAVE, true, VIEW, DELETE),
            makePolicy(DAVE, ED, true, VIEW, DELETE),
            makePolicy(CHARLIE, ED, true, VIEW),
            makePolicy(ED, BOB, true, DELETE),
            makePolicy(BOB, CHARLIE, true, EDIT, DELETE)
    );

    @Mock
    private ResourceSetStore resourceSetStore;
    @Mock
    private ResourceSetStoreFactory resourceSetStoreFactory;
    @Mock
    private PolicyResourceDelegate delegate;

    @BeforeMethod
    public void setup() throws Exception {
        initMocks(this);
        given(resourceSetStoreFactory.create(anyString())).willReturn(resourceSetStore);
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void shouldRequireComputeGraphCall() throws Exception {
        // Given
        PolicyGraph graph = new PolicyGraph(RESOURCE_SET);

        // When
        graph.isValid();
    }

    @Test
    public void shouldDetectValidRights() throws Exception {
        // Given
        List<ResourceResponse> policies = VALID_POLICIES;
        PolicyGraph graph = makePolicyGraph(policies);

        // When
        graph.computeGraph();

        // Then
        assertThat(graph.isValid()).isTrue();
    }

    /*
      VIEW scope is accessible to Dave, so his VIEW reshare to Ed should be active, not inactive.
     */
    @Test
    public void shouldDetectInvalidRightsTree() throws Exception {
        // Given
        List<ResourceResponse> policies = excludePolicies(DAVE, ED);
        policies.add(makePolicy(DAVE, ED, true, DELETE));
        policies.add(makePolicy(DAVE, ED, false, VIEW));

        PolicyGraph graph = makePolicyGraph(policies);

        // When
        graph.computeGraph();

        // Then
        assertThat(graph.isValid()).isFalse();
    }

    /*
      Dave had previously re-shared VIEW scope to ED, but that was made invalid when
      he lost that scope himself. He has been re-granted that scope so the VIEW scope
      should move the active policy and the empty inactive policy should be deleted.
     */
    @Test
    public void shouldUpdateInvalidRightsTree() throws Exception {
        // Given
        List<ResourceResponse> policies = excludePolicies(DAVE, ED);
        policies.add(makePolicy(DAVE, ED, true, DELETE));
        policies.add(makePolicy(DAVE, ED, false, VIEW));

        PolicyGraph graph = makePolicyGraph(policies);
        graph.computeGraph();

        given(delegate.updatePolicies(isNull(Context.class), anySet()))
                .willReturn(Promises.<List<ResourceResponse>, ResourceException>newResultPromise(Collections.<ResourceResponse>emptyList()));

        given(delegate.deletePolicies(isNull(Context.class), anySet()))
                .willReturn(Promises.<List<ResourceResponse>, ResourceException>newResultPromise(Collections.<ResourceResponse>emptyList()));

        // When
        Promise<List<List<ResourceResponse>>, ResourceException> promise = graph.update(null, delegate);

        // Then
        AssertJPromiseAssert.assertThat(promise).succeeded();
        assertThat(UmaPolicyUtils.getPolicyScopes(policyUpdated())).containsOnly(VIEW, DELETE);
        assertThat(policyDeleted()).isEqualTo("Dave-Ed-false");
        verifyNoMoreInteractions(delegate);
    }

    /*
      Dave had removed Ed's ability to DELETE, so Ed's resharing policy to Bob had been
      made inactive. Dave has re-granted Ed's DELETE, so the inactive policy can be made
      active again.
     */
    @Test
    public void shouldSwitchAllScopesInvalid() throws Exception {
        // Given
        List<ResourceResponse> policies = excludePolicies(ED, BOB);
        policies.add(makePolicy(ED, BOB, false, DELETE));

        PolicyGraph graph = makePolicyGraph(policies);
        graph.computeGraph();

        given(delegate.updatePolicies(isNull(Context.class), anySet()))
                .willReturn(Promises.<List<ResourceResponse>, ResourceException>newResultPromise(Collections.<ResourceResponse>emptyList()));

        // When
        Promise<List<List<ResourceResponse>>, ResourceException> promise = graph.update(null, delegate);

        // Then
        AssertJPromiseAssert.assertThat(promise).succeeded();
        assertThat(policyUpdated().get("active").asBoolean()).isTrue();
        verifyNoMoreInteractions(delegate);
    }

    /*
      Alice had removed Dave's ability to VIEW, EDIT and DELETE, so Dave's resharing
      policies to Ed had been made inactive. Alice has re-granted Dave's VIEW and DELETE,
      so those need to be active, while EDIT stays inactive.
     */
    @Test
    public void shouldCreatePolicyWhenMakingValid() throws Exception {
        // Given
        List<ResourceResponse> policies = excludePolicies(DAVE, ED);
        policies.add(makePolicy(DAVE, ED, false, VIEW, DELETE, EDIT));

        PolicyGraph graph = makePolicyGraph(policies);
        graph.computeGraph();

        given(resourceSetStore.read(anyString(), anyString()))
                .willReturn(new ResourceSetDescription(RESOURCE_SET_ID, "RESOURCE_SERVER_ID", ALICE, null));

        given(delegate.updatePolicies(isNull(Context.class), anySet()))
                .willReturn(Promises.<List<ResourceResponse>, ResourceException>newResultPromise(Collections.<ResourceResponse>emptyList()));

        given(delegate.createPolicies(isNull(Context.class), anySet()))
                .willReturn(Promises.<List<ResourceResponse>, ResourceException>newResultPromise(Collections.<ResourceResponse>emptyList()));

        // When
        Promise<List<List<ResourceResponse>>, ResourceException> promise = graph.update(null, delegate);

        // Then
        AssertJPromiseAssert.assertThat(promise).succeeded();
        JsonValue created = policyCreated();
        assertThat(UmaPolicyUtils.getPolicyScopes(created)).containsOnly(VIEW, DELETE);
        assertThat(created.get("active").asBoolean()).isTrue();
        assertThat(UmaPolicyUtils.getPolicyScopes(policyUpdated())).containsOnly(EDIT);
        verifyNoMoreInteractions(delegate);
    }

    /*
      Alice has removed Dave's rights to EDIT, so EDIT needs removing from the
      active Dave -> Ed policy, and adding to an inactive policy.
     */
    @Test
    public void shouldRemoveLostRights() throws Exception {
        // Given
        List<ResourceResponse> policies = excludePolicies(DAVE, ED);
        policies.add(makePolicy(DAVE, ED, true, VIEW, DELETE, EDIT));

        PolicyGraph graph = makePolicyGraph(policies);
        graph.computeGraph();

        given(resourceSetStore.read(anyString(), anyString()))
                .willReturn(new ResourceSetDescription(RESOURCE_SET_ID, "RESOURCE_SERVER_ID", ALICE, null));

        given(delegate.updatePolicies(isNull(Context.class), anySet()))
                .willReturn(Promises.<List<ResourceResponse>, ResourceException>newResultPromise(Collections.<ResourceResponse>emptyList()));

        given(delegate.createPolicies(isNull(Context.class), anySet()))
                .willReturn(Promises.<List<ResourceResponse>, ResourceException>newResultPromise(Collections.<ResourceResponse>emptyList()));

        // When
        Promise<List<List<ResourceResponse>>, ResourceException> promise = graph.update(null, delegate);

        // Then
        AssertJPromiseAssert.assertThat(promise).succeeded();
        JsonValue created = policyCreated();
        assertThat(UmaPolicyUtils.getPolicyScopes(created)).containsOnly(EDIT);
        assertThat(created.get("active").asBoolean()).isFalse();
        assertThat(UmaPolicyUtils.getPolicyScopes(policyUpdated())).containsOnly(VIEW, DELETE);
        verifyNoMoreInteractions(delegate);
    }

    private PolicyGraph makePolicyGraph(List<ResourceResponse> policies) {
        PolicyGraph graph = new PolicyGraph(RESOURCE_SET);
        for (ResourceResponse policy : policies) {
            graph.handleResource(policy);
        }
        graph.handleResult(Responses.newQueryResponse());
        return graph;
    }

    private String policyDeleted() {
        ArgumentCaptor<Set> policyIdCaptor = ArgumentCaptor.forClass(Set.class);
        verify(delegate).deletePolicies(isNull(Context.class), policyIdCaptor.capture());
        assertThat(policyIdCaptor.getValue()).hasSize(1);
        return (String) policyIdCaptor.getValue().iterator().next();
    }

    private JsonValue policyUpdated() {
        ArgumentCaptor<Set> policyCaptor = ArgumentCaptor.forClass(Set.class);
        verify(delegate).updatePolicies(isNull(Context.class), policyCaptor.capture());
        assertThat(policyCaptor.getValue()).hasSize(1);
        return (JsonValue) policyCaptor.getValue().iterator().next();
    }

    private JsonValue policyCreated() {
        ArgumentCaptor<Set> policyCaptor = ArgumentCaptor.forClass(Set.class);
        verify(delegate).createPolicies(isNull(Context.class), policyCaptor.capture());
        assertThat(policyCaptor.getValue()).hasSize(1);
        return (JsonValue) policyCaptor.getValue().iterator().next();
    }

    private static ResourceResponse makePolicy(String owner, String subject, boolean active, String... scopes) {
        String policyId = owner + "-" + subject + "-" + active;
        JsonValue policy = json(object(
                field("_id", policyId),
                field(PolicyGraph.OWNER_KEY, owner),
                field(SUBJECT_KEY, object(field(BACKEND_POLICY_SUBJECT_CLAIM_VALUE_KEY, subject))),
                field(PolicyGraph.ACTIVE_KEY, active)
        ));
        for (String scope : scopes) {
            policy.putPermissive(new JsonPointer(BACKEND_POLICY_ACTION_VALUES_KEY + "/" + scope), true);
        }
        return Responses.newResourceResponse(policyId, String.valueOf(policyId.hashCode()), policy);
    }

    private static List<ResourceResponse> excludePolicies(String owner, String subject) {
        List<ResourceResponse> resources = new ArrayList<>(VALID_POLICIES);
        for(Iterator<ResourceResponse> i = resources.iterator(); i.hasNext(); ) {
            JsonValue policy = i.next().getContent();
            if (owner.equals(policy.get(PolicyGraph.OWNER_KEY).asString()) &&
                    subject.equals(UmaPolicyUtils.getPolicySubject(policy))) {
                i.remove();
            }
        }
        return resources;
    }

}