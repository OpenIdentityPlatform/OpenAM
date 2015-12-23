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

package org.forgerock.openam.uma.rest;

import static org.forgerock.json.JsonValue.*;
import static org.forgerock.json.resource.Requests.newQueryRequest;
import static org.forgerock.util.promise.Promises.newResultPromise;

import javax.inject.Inject;
import javax.security.auth.Subject;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sun.identity.entitlement.Entitlement;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.Evaluator;
import com.sun.identity.entitlement.JwtPrincipal;
import com.sun.identity.idm.AMIdentity;
import org.forgerock.json.JsonPointer;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.oauth2.core.exceptions.NotFoundException;
import org.forgerock.oauth2.core.exceptions.ServerException;
import org.forgerock.oauth2.resources.ResourceSetDescription;
import org.forgerock.openam.core.CoreWrapper;
import org.forgerock.openam.cts.api.fields.ResourceSetTokenField;
import org.forgerock.openam.oauth2.resources.ResourceSetStoreFactory;
import org.forgerock.openam.oauth2.rest.AggregateQuery;
import org.forgerock.openam.rest.RealmContext;
import org.forgerock.openam.sm.datalayer.api.DataLayerException;
import org.forgerock.openam.uma.ResourceSetSharedFilter;
import org.forgerock.openam.uma.UmaPolicy;
import org.forgerock.openam.uma.UmaPolicyService;
import org.forgerock.openam.uma.UmaProviderSettingsFactory;
import org.forgerock.opendj.ldap.Connection;
import org.forgerock.services.context.Context;
import org.forgerock.util.AsyncFunction;
import org.forgerock.util.Function;
import org.forgerock.util.Pair;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.promise.PromiseImpl;
import org.forgerock.util.promise.Promises;
import org.forgerock.util.promise.ResultHandler;
import org.forgerock.util.query.BaseQueryFilterVisitor;
import org.forgerock.util.query.QueryFilter;
import org.forgerock.util.query.QueryFilterVisitor;

/**
 * Services for getting Resource Sets and optionally augmenting them with an associated UMA policy.
 *
 * @since 13.0.0
 */
public class ResourceSetService {

    private final ResourceSetStoreFactory resourceSetStoreFactory;
    private final UmaPolicyService policyService;
    private final CoreWrapper coreWrapper;
    private final UmaProviderSettingsFactory umaProviderSettingsFactory;

    /**
     * Creates an instance of a ResourceSetService.
     *
     * @param resourceSetStoreFactory An instance of the {@code ResourceSetStoreFactory}.
     * @param policyService An instance of the UmaPolicyService.
     */
    @Inject
    public ResourceSetService(ResourceSetStoreFactory resourceSetStoreFactory, UmaPolicyService policyService,
            CoreWrapper coreWrapper, UmaProviderSettingsFactory umaProviderSettingsFactory) {
        this.resourceSetStoreFactory = resourceSetStoreFactory;
        this.policyService = policyService;
        this.coreWrapper = coreWrapper;
        this.umaProviderSettingsFactory = umaProviderSettingsFactory;
    }

    /**
     * Gets a Resource Set, optionally with its associated policy, if one exists.
     *
     * @param context           The context.
     * @param realm             The realm.
     * @param resourceSetId     The resource set Id.
     * @param resourceOwnerId   The resource owner Id.
     * @param augmentWithPolicy {@code true} to pull in UMA policies into the resource set.
     * @return A Promise containing the Resource Set or a ResourceException.
     */
    Promise<ResourceSetDescription, ResourceException> getResourceSet(final Context context,
            String realm, final String resourceSetId, String resourceOwnerId, final boolean augmentWithPolicy) {
        return getResourceSet(realm, resourceSetId, resourceOwnerId)
                .thenOnResult(new ResultHandler<ResourceSetDescription>() {
                    @Override
                    public void handleResult(ResourceSetDescription resourceSet) {
                        if (augmentWithPolicy) {
                            augmentWithPolicy(context, resourceSetId, resourceSet);
                        }
                    }
                });
    }

    protected Subject createSubject(String username, String realm) {
        AMIdentity identity = coreWrapper.getIdentity(username, realm);
        JwtPrincipal principal = new JwtPrincipal(json(object(field("sub", identity.getUniversalId()))));
        Set<Principal> principals = new HashSet<>();
        principals.add(principal);
        return new Subject(false, principals, Collections.emptySet(), Collections.emptySet());
    }

    private Promise<Set<ResourceSetDescription>, ResourceException> getSharedResourceSets(final Context context,
            QueryRequest policyQuery, final String resourceOwnerId) {
        return policyService.queryPolicies(context, policyQuery)
                .thenAsync(new AsyncFunction<Pair<QueryResponse, Collection<UmaPolicy>>,
                        Set<ResourceSetDescription>, ResourceException>() {
                    @Override
                    public Promise<Set<ResourceSetDescription>, ResourceException> apply(
                            final Pair<QueryResponse, Collection<UmaPolicy>> result) {
                        final Set<ResourceSetDescription> sharedResourceSets = new HashSet<>();

                        String realm = RealmContext.getRealm(context);
                        try {
                            for (UmaPolicy sharedPolicy : result.getSecond()) {
                                if (!sharedResourceSets.contains(sharedPolicy.getResourceSet())) {
                                    if (isSharedWith(sharedPolicy.getResourceSet(), resourceOwnerId, realm)) {
                                        sharedResourceSets.add(sharedPolicy.getResourceSet());
                                    }
                                }
                            }
                        } catch (InternalServerErrorException isee) {
                            return isee.asPromise();
                        }
                        return newResultPromise(sharedResourceSets);
                    }
                });
    }

    /**
     * Filters the policies based on the provided query.
     * @param resourceSets The ResourceSets to filter.
     * @param query The filter to use.
     * @return The ResourceSets that match the query.
     */
    public Collection<ResourceSetDescription> filterPolicies(Set<ResourceSetDescription> resourceSets,
                                                             ResourceSetWithPolicyQuery query) {
        Collection<ResourceSetDescription> filteredResourceSets = new HashSet<>();
        filteredResourceSets.addAll(query.getResourceSetQuery().accept(RESOURCE_SET_QUERY_EVALUATOR,
                resourceSets));
        return filteredResourceSets;
    }

    /**
     * Checks whether a ResourceSet is accessible by a user.
     * @param resourceSet The resource set to check.
     * @param resourceUserId The id of the user to check.
     * @param realm The realm to check in.
     * @return @code{true} if the user can access that ResourceSet.
     */
    public boolean isSharedWith(ResourceSetDescription resourceSet, String resourceUserId, String realm)
            throws InternalServerErrorException{
        Subject subject = createSubject(resourceUserId, realm);
        try {
            Evaluator evaluator = umaProviderSettingsFactory.get(realm).getPolicyEvaluator(subject,
                    resourceSet.getClientId().toLowerCase());
            String sharedResourceName = "uma://" + resourceSet.getId();
            List<Entitlement> entitlements = evaluator.evaluate(realm, subject,
                    sharedResourceName, null, false);

            if (!entitlements.isEmpty() && !entitlements.iterator().next().getActionValues().isEmpty()) {
                return true;
            }
        } catch (EntitlementException | NotFoundException e) {
            throw new InternalServerErrorException(e);
        }
        return false;
    }

    /**
     * Queries resource sets across the resource set store and UMA policy store.
     *
     * @param context             The context.
     * @param realm               The realm.
     * @param query               The aggregated query.
     * @param resourceOwnerId     The resource owner id.
     * @param augmentWithPolicies {@code true} to pull in UMA policies into the resource set.
     * @return A Promise containing the Resource Sets or a ResourceException.
     */
    Promise<Collection<ResourceSetDescription>, ResourceException> getResourceSets(final Context context,
            String realm, final ResourceSetWithPolicyQuery query, final String resourceOwnerId,
            final boolean augmentWithPolicies) {
        final Set<ResourceSetDescription> resourceSets;
        try {
            resourceSets = new ResourceSetSharedFilter(this, resourceOwnerId, realm)
                    .filter(resourceSetStoreFactory.create(realm).query(query.getResourceSetQuery()));
        } catch (ServerException e) {
            return new InternalServerErrorException(e).asPromise();
        }

        QueryRequest policyQuery = newQueryRequest("").setQueryId("searchAll");
        policyQuery.setQueryFilter(QueryFilter.<JsonPointer>alwaysTrue());
        return getSharedResourceSets(context, policyQuery, resourceOwnerId)
                .thenAsync(new AsyncFunction<Set<ResourceSetDescription>, Collection<ResourceSetDescription>,
                        ResourceException>() {
                    @Override
                    public Promise<Collection<ResourceSetDescription>, ResourceException> apply(
                            final Set<ResourceSetDescription> sharedResourceSets) {
                        //combine the owned ResourceSets with the shared ones, then filter based on the query
                        sharedResourceSets.addAll(resourceSets);
                        final Collection<ResourceSetDescription> filteredResourceSets = filterPolicies(resourceSets, query);

                        Promise<Collection<ResourceSetDescription>, ResourceException> resourceSetsPromise;
                        if (query.getPolicyQuery() != null) {
                            QueryRequest policyQuery = newQueryRequest("").setQueryFilter(query.getPolicyQuery());
                            resourceSetsPromise = policyService.queryPolicies(context, policyQuery)
                                    .thenAsync(new AsyncFunction<Pair<QueryResponse, Collection<UmaPolicy>>,
                                            Collection<ResourceSetDescription>, ResourceException>() {
                                        @Override
                                        public Promise<Collection<ResourceSetDescription>, ResourceException> apply(
                                                Pair<QueryResponse, Collection<UmaPolicy>> result) throws ResourceException {
                                            try {
                                                return newResultPromise(combine(context, query,
                                                        filteredResourceSets, result.getSecond(), augmentWithPolicies,
                                                        resourceOwnerId));
                                            } catch (org.forgerock.oauth2.core.exceptions.NotFoundException e) {
                                                return new InternalServerErrorException(e).asPromise();
                                            } catch (ServerException e) {
                                                return new InternalServerErrorException(e).asPromise();
                                            }
                                        }
                                    });
                        } else {
                            if (augmentWithPolicies) {
                                List<Promise<ResourceSetDescription, ResourceException>> promises
                                        = new ArrayList<>();
                                PromiseImpl<ResourceSetDescription, ResourceException> kicker = PromiseImpl.create();
                                promises.add(kicker);
                                for (ResourceSetDescription resourceSet : filteredResourceSets) {
                                    promises.add(augmentWithPolicy(context, resourceSet.getId(), resourceSet));
                                }
                                resourceSetsPromise = Promises.when(promises)
                                        .thenAsync(new AsyncFunction<List<ResourceSetDescription>, Collection<ResourceSetDescription>, ResourceException>() {
                                            @Override
                                            public Promise<Collection<ResourceSetDescription>, ResourceException> apply(List<ResourceSetDescription> resourceSets) {
                                                Collection<ResourceSetDescription> resourceSetDescriptions
                                                        = new HashSet<>();
                                                for (ResourceSetDescription rs : filteredResourceSets) {
                                                    if (rs != null) {
                                                        resourceSetDescriptions.add(rs);
                                                    }
                                                }
                                                return newResultPromise(resourceSetDescriptions);
                                            }
                                        });
                                kicker.handleResult(null);
                            } else {
                                resourceSetsPromise = newResultPromise(filteredResourceSets);
                            }
                        }
                        return resourceSetsPromise;
                    }
                });
    }

    private static final AsyncFunction<ResourceException, Void, ResourceException> IGNORE_NOTFOUNDEXCEPTION =
            new AsyncFunction<ResourceException, Void, ResourceException>() {
                public Promise<Void, ResourceException> apply(ResourceException e) {
                    if (e instanceof org.forgerock.json.resource.NotFoundException) {
                        return newResultPromise(null);
                    } else {
                        return e.asPromise();
                    }
                }
            };
    /**
     * Revokes all UMA policies for a user's resource sets.
     *
     * @param context         The context.
     * @param realm           The realm.
     * @param resourceOwnerId The resource owner id.
     * @return A Promise containing {@code null} or a ResourceException.
     */
    Promise<Void, ResourceException> revokeAllPolicies(final Context context, String realm,
                                                       String resourceOwnerId) {
        ResourceSetWithPolicyQuery query = new ResourceSetWithPolicyQuery();
        query.setResourceSetQuery(QueryFilter.<String>alwaysTrue());
        return getResourceSets(context, realm, query, resourceOwnerId, false)
                .thenAsync(new AsyncFunction<Collection<ResourceSetDescription>, Void, ResourceException>() {
                    @Override
                    public Promise<Void, ResourceException> apply(Collection<ResourceSetDescription> resourceSets) {
                        List<Promise<Void, ResourceException>> promises
                                = new ArrayList<Promise<Void, ResourceException>>();
                        PromiseImpl<Void, ResourceException> kicker = PromiseImpl.create();
                        promises.add(kicker);
                        for (ResourceSetDescription resourceSet : resourceSets) {
                            promises.add(policyService.deletePolicy(context, resourceSet.getId())
                                    .thenCatchAsync(IGNORE_NOTFOUNDEXCEPTION));
                        }
                        Promise<List<Void>, ResourceException> when = Promises.when(promises);
                        kicker.handleResult(null);
                        return when.thenAsync(new AsyncFunction<List<Void>, Void, ResourceException>() {
                            @Override
                            public Promise<Void, ResourceException> apply(List<Void> voids) {
                                return newResultPromise(null);
                            }
                        });
                    }
                });
    }

    private Promise<ResourceSetDescription, ResourceException> getResourceSet(String realm, String resourceSetId,
                                                                              String resourceOwnerId) {
        try {
            ResourceSetDescription resourceSet = resourceSetStoreFactory.create(realm)
                    .read(resourceSetId, new ResourceSetSharedFilter(this, resourceOwnerId, realm));
            return newResultPromise(resourceSet);
        } catch (NotFoundException e) {
            return new org.forgerock.json.resource.NotFoundException("No resource set with id, " + resourceSetId
                    + ", found.").asPromise();
        } catch (ServerException e) {
            return new InternalServerErrorException(e).asPromise();
        }
    }

    private Promise<ResourceSetDescription, ResourceException> augmentWithPolicy(Context context,
            final String resourceSetId, final ResourceSetDescription resourceSet) {
        return policyService.readPolicy(context, resourceSetId)
                .thenAsync(new AsyncFunction<UmaPolicy, ResourceSetDescription, ResourceException>() {
                    @Override
                    public Promise<ResourceSetDescription, ResourceException> apply(UmaPolicy result) throws ResourceException {
                        resourceSet.setPolicy(result.asJson());
                        return newResultPromise(resourceSet);
                    }
                }, new AsyncFunction<ResourceException, ResourceSetDescription, ResourceException>() {
                    @Override
                    public Promise<ResourceSetDescription, ResourceException> apply(ResourceException e) throws ResourceException {
                        return newResultPromise(resourceSet);
                    }
                });
    }

    private Collection<ResourceSetDescription> combine(Context context,
            ResourceSetWithPolicyQuery resourceSetWithPolicyQuery, Collection<ResourceSetDescription> resourceSets,
            Collection<UmaPolicy> policies, boolean augmentWithPolicies, String resourceOwnerId)
            throws org.forgerock.oauth2.core.exceptions.NotFoundException, ServerException {

        Map<String, ResourceSetDescription> resourceSetsById = new HashMap<String, ResourceSetDescription>();
        Map<String, UmaPolicy> policiesById = new HashMap<String, UmaPolicy>();

        for (ResourceSetDescription resourceSet : resourceSets) {
            resourceSetsById.put(resourceSet.getId(), resourceSet);
        }

        for (UmaPolicy policy : policies) {
            policiesById.put(policy.getId(), policy);
        }

        if (AggregateQuery.Operator.AND.equals(resourceSetWithPolicyQuery.getOperator())) {
            resourceSetsById.keySet().retainAll(policiesById.keySet());
            if (augmentWithPolicies) {
                for (ResourceSetDescription resourceSet : resourceSetsById.values()) {
                    resourceSet.setPolicy(policiesById.get(resourceSet.getId()).asJson());
                }
            }
        } else if (AggregateQuery.Operator.OR.equals(resourceSetWithPolicyQuery.getOperator())) {

            if (augmentWithPolicies) {
                for (ResourceSetDescription resourceSet : resourceSetsById.values()) {
                    augmentWithPolicy(context, resourceSet.getId(), resourceSet);
                }
            }

            for (Map.Entry<String, UmaPolicy> entry : policiesById.entrySet()) {
                ResourceSetDescription resourceSet;
                if (resourceSetsById.containsKey(entry.getKey())) {
                    resourceSet = resourceSetsById.get(entry.getKey());
                } else {
                    RealmContext realmContext = context.asContext(RealmContext.class);
                    resourceSet = resourceSetStoreFactory.create(realmContext.getResolvedRealm())
                            .read(entry.getKey(), resourceOwnerId);
                }
                if (augmentWithPolicies) {
                    resourceSet.setPolicy(entry.getValue().asJson());
                }
                resourceSetsById.put(entry.getKey(), resourceSet);
            }
        }

        return resourceSetsById.values();
    }

    private static final QueryFilterVisitor<Set<ResourceSetDescription>, Set<ResourceSetDescription>, String>
            RESOURCE_SET_QUERY_EVALUATOR =
            new BaseQueryFilterVisitor<Set<ResourceSetDescription>, Set<ResourceSetDescription>, String>() {
                @Override
                public Set<ResourceSetDescription> visitAndFilter(Set<ResourceSetDescription> resourceSetDescriptions,
                        List<QueryFilter<String>> list) {
                    for (QueryFilter<String> filter : list) {
                        resourceSetDescriptions.retainAll(filter.accept(this, resourceSetDescriptions));
                    }

                    return resourceSetDescriptions;
                }

                @Override
                public Set<ResourceSetDescription> visitOrFilter(Set<ResourceSetDescription> resourceSetDescriptions,
                        List<QueryFilter<String>> list) {
                    Set<ResourceSetDescription> matching = new HashSet<>(resourceSetDescriptions.size());
                    for (QueryFilter<String> filter : list) {
                        matching.addAll(filter.accept(this, resourceSetDescriptions));
                    }
                    return matching;
                }

                @Override
                public Set<ResourceSetDescription> visitBooleanLiteralFilter(
                        Set<ResourceSetDescription> resourceSetDescriptions, boolean value) {
                    if (value) {
                        return resourceSetDescriptions;
                    } else {
                        return Collections.emptySet();
                    }
                }

                @Override
                public Set<ResourceSetDescription> visitContainsFilter(
                        Set<ResourceSetDescription> resourceSetDescriptions,String fieldName, Object value) {
                    Set<ResourceSetDescription> results = new HashSet<>();

                    for (ResourceSetDescription resourceSetDesc : resourceSetDescriptions) {
                        if (fieldName.equals("name")) {
                            if (resourceSetDesc.getName().toLowerCase().contains(((String) value).toLowerCase())) {
                                results.add(resourceSetDesc);
                            }
                        }
                    }

                    return results;
                }

                @Override
                public Set<ResourceSetDescription> visitEqualsFilter(
                        Set<ResourceSetDescription> resourceSetDescriptions, String fieldName, Object value) {
                    Set<ResourceSetDescription> results = new HashSet<>();

                    for (ResourceSetDescription resourceSetDescription : resourceSetDescriptions) {
                        if (fieldName.equals(ResourceSetTokenField.RESOURCE_OWNER_ID)) {
                            if (resourceSetDescription.getResourceOwnerId().equals(value)) {
                                results.add(resourceSetDescription);
                            }
                        } else if (fieldName.equals(ResourceSetTokenField.RESOURCE_SET_ID)) {
                            if (resourceSetDescription.getId().equals(value)) {
                                results.add(resourceSetDescription);
                            }
                        }
                    }

                    return results;
                }

                @Override
                public Set<ResourceSetDescription> visitNotFilter(Set<ResourceSetDescription> resourceSetDescriptions,
                        QueryFilter<String> queryFilter) {
                    Set<ResourceSetDescription> excluded = queryFilter.accept(this, resourceSetDescriptions);
                    resourceSetDescriptions.removeAll(excluded);
                    return resourceSetDescriptions;
                }
            };
}
