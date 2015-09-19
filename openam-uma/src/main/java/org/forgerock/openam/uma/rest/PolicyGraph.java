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

import static java.util.Collections.*;
import static org.forgerock.json.JsonValue.*;
import static org.forgerock.openam.uma.UmaConstants.BackendPolicy.*;
import static org.forgerock.openam.uma.UmaConstants.UmaPolicy.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.forgerock.guava.common.collect.Sets;
import org.forgerock.services.context.Context;
import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.BadRequestException;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.oauth2.resources.ResourceSetDescription;
import org.forgerock.openam.entitlement.rest.model.json.JsonPolicy;
import org.forgerock.openam.uma.UmaPolicy;
import org.forgerock.openam.uma.UmaPolicyUtils;
import org.forgerock.util.promise.ExceptionHandler;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.promise.Promises;
import org.forgerock.util.promise.ResultHandler;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DirectedMultigraph;

/**
 * Graph representation of the policy engine policies that represent all UMA policies for a single
 * resource set.
 * <p>
 * Clients should construct the object with the Resource representation of the policies, then
 * call the {@link #computeGraph()} method before using the other public methods.
 */
public class PolicyGraph implements QueryResourceHandler, ExceptionHandler<ResourceException>,
        ResultHandler<QueryResponse> {
    /**
     * @see JsonPolicy
     */
    static final String OWNER_KEY = "createdBy";
    static final String ACTIVE_KEY = "active";
    private final String resourceOwner;
    private final ResourceSetDescription resourceSet;
    private final DirectedMultigraph<Object, PolicyEdge> graph;
    private final Map<String, PolicyScopes> policyRights;
    private Map<String, Set<String>> rights;
    private Set<String> invalidUsers;
    private Boolean complete = null;

    /**
     * Construct a new instance to evaluate and update the graph of rights within the policy engine.
     * @param resourceSet The resource set being evaluated.
     */
    @Inject
    public PolicyGraph(ResourceSetDescription resourceSet) {
        this.resourceOwner = resourceSet.getResourceOwnerId();
        this.resourceSet = resourceSet;
        this.graph = new DirectedMultigraph<>(PolicyEdge.class);
        this.policyRights = new HashMap<>();
    }

    @Override
    public boolean handleResource(ResourceResponse policy) {
        JsonValue policyContent = policy.getContent();
        String subject = UmaPolicyUtils.getPolicySubject(policyContent);
        String owner = policyContent.get(OWNER_KEY).asString();
        synchronized (policyRights) {
            if (!policyRights.containsKey(subject)) {
                policyRights.put(subject, new PolicyScopes(subject));
            }
        }

        final PolicyScopes policyScopes = policyRights.get(subject);
        synchronized (policyScopes) {
            policyScopes.addScopes(owner, policyContent);
        }
        return true;
    }

    /**
     * Computes the graph of rights contained within the policy engine policies.
     */
    public void computeGraph() {
        if (complete == null || !complete) {
            throw new IllegalStateException("Policies not fully loaded");
        }

        rights = findRights();

        invalidUsers = new HashSet<>();
        for (Map.Entry<String, Set<String>> userRights : rights.entrySet()) {
            Set<String> grantedRights = userRights.getValue();
            PolicyScopes policyScopes = policyRights.get(userRights.getKey());
            Set<String> resharedRights = policyScopes.activeScopePolicies.keySet();
            Set<String> disabledRights = policyScopes.inactiveScopePolicies.keySet();
            if (!grantedRights.equals(resharedRights) || !Sets.intersection(disabledRights, grantedRights).isEmpty()) {
                invalidUsers.add(userRights.getKey());
            }
        }
    }

    /**
     * Update the policy engine's policies so that they correctly reflect the state of the
     * rights graph. Any users for whom the current policies are invalid (incorrectly
     * either active or inactive based on entitlements) will be iterated over, moving scopes
     * to the appropriate state based on that updated entitlement.
     * @param context The current context, for updates being made.
     * @param policyResourceDelegate The delegate to use for updating the policy engine.
     * @return A promise for all the updates made.
     */
    public Promise<List<List<ResourceResponse>>, ResourceException> update(Context context,
            PolicyResourceDelegate policyResourceDelegate) {
        checkState();
        List<Promise<List<ResourceResponse>, ResourceException>> promises = new ArrayList<>();
        Set<JsonValue> createdPolicies = new HashSet<>();
        Set<JsonValue> updatedPolicies = new HashSet<>();

        for (String user : invalidUsers) {
            Set<String> accessibleRights = rights.get(user);
            PolicyScopes policies = policyRights.get(user);

            Set<String> newRights = new HashSet<>(accessibleRights);
            newRights.removeAll(policies.activeScopePolicies.keySet());

            Set<String> newlyAccessibleRights = new HashSet<>(accessibleRights);
            newlyAccessibleRights.retainAll(policies.inactiveScopePolicies.keySet());

            Set<String> lostRights = new HashSet<>(policies.activeScopePolicies.keySet());
            lostRights.removeAll(accessibleRights);

            try {
                for (String scope : Sets.union(newRights, newlyAccessibleRights)) {
                    moveScope(policies.inactiveScopePolicies.get(scope), policies.activeOwnedPolicies, context,
                            policyResourceDelegate, newRights, createdPolicies, updatedPolicies, scope, true, promises,
                            user);
                }

                for (String scope : lostRights) {
                    moveScope(policies.activeScopePolicies.get(scope), policies.inactiveOwnedPolicies, context,
                            policyResourceDelegate, lostRights, createdPolicies, updatedPolicies, scope, false,
                            promises, user);
                }
            } catch (BadRequestException e) {
                return e.asPromise();
            }
        }

        if (!createdPolicies.isEmpty()) {
            promises.add(policyResourceDelegate.createPolicies(context, createdPolicies));
        }
        if (!updatedPolicies.isEmpty()) {
            promises.add(policyResourceDelegate.updatePolicies(context, updatedPolicies));
        }

        return Promises.when(promises);
    }

    /**
     * Moves the scopes that are incorrectly active/inactive to a policy that has the opposite state.
     * @param moveFrom A map of policy owners to policies the scope is incorrectly currently in.
     * @param moveTo A map of policy owner to existing policies the scope might be moved to.
     * @param context The context for passing to the policy resource delegate.
     * @param policyResourceDelegate To be used for deleting any policies that are emptied of scopes (actions).
     * @param allMovingRights All the scopes that need switching state.
     * @param createdPolicies Policies that are being created by this update.
     * @param updatedPolicies Policies that are being updated by this update.
     * @param scope The current scope being operated on.
     * @param newPolicyActive Whether the scope is being moved to active state.
     * @param promises Promises for all policy updates.
     * @param user The user for whom we are switching scope state.
     * @throws BadRequestException If the UmaPolicy cannot be created for new policy.
     */
    private void moveScope(Map<String, JsonValue> moveFrom, Map<String, JsonValue> moveTo, Context context,
            PolicyResourceDelegate policyResourceDelegate, Set<String> allMovingRights, Set<JsonValue> createdPolicies,
            Set<JsonValue> updatedPolicies, String scope, boolean newPolicyActive,
            List<Promise<List<ResourceResponse>, ResourceException>> promises, String user)
            throws BadRequestException {
        JsonPointer scopePointer = new JsonPointer(BACKEND_POLICY_ACTION_VALUES_KEY).child(scope);
        for (Map.Entry<String, JsonValue> ownedPolicy : moveFrom.entrySet()) {
            String owner = ownedPolicy.getKey();
            JsonValue policy = ownedPolicy.getValue();
            JsonValue ownedMoveTo = moveTo.get(owner);
            boolean policyToMoveToAlreadyExists = ownedMoveTo != null;
            if (policyToMoveToAlreadyExists) {
                ownedMoveTo.put(scopePointer, true);
                // If this policy is being created already, no need to update.
                if (!createdPolicies.contains(ownedMoveTo)) {
                    updatedPolicies.add(ownedMoveTo);
                }
                policy.remove(scopePointer);
            } else if (allScopesAreSwitchingState(allMovingRights, policy)) {
                policy.put(ACTIVE_KEY, true);
            } else {
                // Create a new policy to move to
                JsonValue newPolicy = UmaPolicy.valueOf(resourceSet, json(object(
                        field(POLICY_ID_KEY, resourceSet.getId()),
                        field(PERMISSIONS_KEY, array(
                                object(field(SUBJECT_KEY, user), field(SCOPES_KEY, array(scope)))
                        ))
                ))).asUnderlyingPolicies(owner).iterator().next();

                newPolicy.put(ACTIVE_KEY, newPolicyActive);

                createdPolicies.add(newPolicy);
                moveTo.put(owner, newPolicy);
                policy.remove(scopePointer);
            }
            if (policy.get(BACKEND_POLICY_ACTION_VALUES_KEY).size() == 0) {
                // No scopes left in the policy, so it can be removed.
                updatedPolicies.remove(policy);
                promises.add(policyResourceDelegate.deletePolicies(context, singleton(policy.get("_id").asString())));
            } else {
                updatedPolicies.add(policy);
            }
        }
    }

    private boolean allScopesAreSwitchingState(Set<String> allMovingRights, JsonValue policy) {
        return allMovingRights.containsAll(policy.get(BACKEND_POLICY_ACTION_VALUES_KEY).asMap().keySet());
    }

    /**
     * Get the validity state of the policy rights graph.
     * @throws IllegalStateException If {@link #computeGraph()} has not yet been called.
     * @return {@code true} if the policies correctly represent the rights graph, or {@code false} if updates
     * are needed for that to be the case.
     */
    public boolean isValid() {
        checkState();
        return invalidUsers.isEmpty();
    }

    private void checkState() {
        if (invalidUsers == null) {
            throw new IllegalStateException("Graph has not been computed");
        }
    }

    private Map<String, Set<String>> findRights() {
        Map<String, Set<String>> rights = new HashMap<>();
        if (graph.containsVertex(resourceOwner)) {
            for (PolicyEdge edge : graph.edgesOf(resourceOwner)) {
                findRights0(rights, new HashSet<>(singleton(resourceOwner)), edge, new HashSet<>(edge.actions));
            }
        }
        return rights;
    }

    private void findRights0(Map<String, Set<String>> rights, Set<String> visited, PolicyEdge edge,
            Set<String> scopes) {
        if (!rights.containsKey(edge.subject)) {
            rights.put(edge.subject, new HashSet<String>());
        }
        Set<String> receivedScopes = new HashSet<>(scopes);
        receivedScopes.retainAll(edge.actions);
        if (rights.get(edge.subject).containsAll(receivedScopes)) {
            return;
        }
        rights.get(edge.subject).addAll(receivedScopes);
        visited.add(edge.subject);
        for (PolicyEdge next : graph.edgesOf(edge.subject)) {
            if (!visited.contains(next.subject)) {
                findRights0(rights, visited, next, receivedScopes);
            }
        }
        visited.remove(edge.subject);
    }

    @Override
    public void handleException(ResourceException e) {
        complete = false;
    }

    @Override
    public void handleResult(QueryResponse queryResponse) {
        complete = true;
    }

    private static class PolicyEdge extends DefaultEdge {
        private final String owner;
        private final String subject;
        private final Collection<String> actions = new HashSet<>();

        private PolicyEdge(String owner, String subject) {
            this.owner = owner;
            this.subject = subject;
        }

        public Object getSource() {
            return owner;
        }

        public Object getTarget() {
            return subject;
        }

        public Collection<String> getActions() {
            return actions;
        }

        @Override
        public String toString() {
            return "(" + owner + " -> " + subject + " : " + actions + ")";
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof PolicyEdge
                    && ((PolicyEdge) obj).actions.equals(actions)
                    && ((PolicyEdge) obj).owner.equals(owner)
                    && ((PolicyEdge) obj).subject.equals(subject);
        }

        @Override
        public int hashCode() {
            return owner.hashCode() + (31 * subject.hashCode());
        }
    }

    private class PolicyScopes {

        private final Map<String, Map<String, JsonValue>> activeScopePolicies = new HashMap<>();
        private final Map<String, JsonValue> activeOwnedPolicies = new HashMap<>();
        private final Map<String, Map<String, JsonValue>> inactiveScopePolicies = new HashMap<>();
        private final Map<String, JsonValue> inactiveOwnedPolicies = new HashMap<>();
        private final Map<String, PolicyEdge> incomingEdges = new HashMap<>();
        private final String subject;

        private PolicyScopes(String subject) {
            this.subject = subject;
        }

        private void addScopes(String owner, JsonValue policy) {
            PolicyEdge edge;
            Set<String> scopes = UmaPolicyUtils.getPolicyScopes(policy);
            if (!incomingEdges.containsKey(owner)) {
                edge = new PolicyEdge(owner, subject);
                graph.addVertex(subject);
                graph.addVertex(owner);
                graph.addEdge(owner, subject, edge);
                incomingEdges.put(owner, edge);
            } else {
                edge = incomingEdges.get(owner);
            }
            edge.actions.addAll(scopes);

            Map<String, Map<String, JsonValue>> scopePolicies;
            Map<String, JsonValue> ownedPolicies;
            if (policy.get(ACTIVE_KEY).asBoolean()) {
                scopePolicies = activeScopePolicies;
                ownedPolicies = activeOwnedPolicies;
            } else {
                scopePolicies = inactiveScopePolicies;
                ownedPolicies = inactiveOwnedPolicies;
            }

            for (String scope : scopes) {
                if (!scopePolicies.containsKey(scope)) {
                    scopePolicies.put(scope, new HashMap<String, JsonValue>());
                }
                scopePolicies.get(scope).put(owner, policy);
            }
            ownedPolicies.put(owner, policy);
        }

    }
}
