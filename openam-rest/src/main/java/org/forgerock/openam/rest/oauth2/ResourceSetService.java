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

package org.forgerock.openam.rest.oauth2;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResult;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.oauth2.core.exceptions.NotFoundException;
import org.forgerock.oauth2.core.exceptions.ServerException;
import org.forgerock.oauth2.resources.ResourceSetDescription;
import org.forgerock.openam.oauth2.resources.ResourceSetStoreFactory;
import org.forgerock.openam.rest.resource.RealmContext;
import org.forgerock.openam.uma.UmaPolicy;
import org.forgerock.openam.uma.UmaPolicyService;
import org.forgerock.util.Pair;
import org.forgerock.util.promise.AsyncFunction;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.promise.PromiseImpl;
import org.forgerock.util.promise.Promises;
import org.forgerock.util.promise.SuccessHandler;

/**
 * Services for getting Resource Sets and optionally augmenting them with an associated UMA policy.
 *
 * @since 13.0.0
 */
public class ResourceSetService {

    private final ResourceSetStoreFactory resourceSetStoreFactory;
    private final UmaPolicyService policyService;

    /**
     * Creates an instance of a ResourceSetService.
     *
     * @param resourceSetStoreFactory An instance of the {@code ResourceSetStoreFactory}.
     * @param policyService An instance of the UmaPolicyService.
     */
    @Inject
    public ResourceSetService(ResourceSetStoreFactory resourceSetStoreFactory, UmaPolicyService policyService) {
        this.resourceSetStoreFactory = resourceSetStoreFactory;
        this.policyService = policyService;
    }

    private Promise<ResourceSetDescription, ResourceException> getResourceSet(String realm, String resourceSetId) {
        try {
            return Promises.newSuccessfulPromise(resourceSetStoreFactory.create(realm).read(resourceSetId));
        } catch (NotFoundException e) {
            return Promises.newFailedPromise(
                    (ResourceException) new org.forgerock.json.resource.NotFoundException("No resource set with id, "
                            + resourceSetId + ", found."));
        } catch (ServerException e) {
            return Promises.newFailedPromise((ResourceException) new InternalServerErrorException(e));
        }
    }

    /**
     * Gets a Resource Set, optionally with its associated policy, if one exists.
     *
     * @param context The context.
     * @param realm The realm.
     * @param resourceSetId The resource set Id.
     * @param augmentWithPolicy {@code true} to pull in UMA policies into the resource set.
     * @return A Promise containing the Resource Set or a ResourceException.
     */
    Promise<ResourceSetDescription, ResourceException> getResourceSet(final ServerContext context,
            String realm, final String resourceSetId, final boolean augmentWithPolicy) {
        return getResourceSet(realm, resourceSetId)
                .onSuccess(new SuccessHandler<ResourceSetDescription>() {
                    @Override
                    public void handleResult(ResourceSetDescription resourceSet) {
                        if (augmentWithPolicy) {
                            augmentResourceSetWithPolicy(context, resourceSetId, resourceSet);
                        }
                    }
                });
    }

    private Promise<ResourceSetDescription, ResourceException> augmentResourceSetWithPolicy(ServerContext context,
            final String resourceSetId, final ResourceSetDescription resourceSet) {
        return policyService.readPolicy(context, resourceSetId)
                .thenAsync(new AsyncFunction<UmaPolicy, ResourceSetDescription, ResourceException>() {
                    @Override
                    public Promise<ResourceSetDescription, ResourceException> apply(UmaPolicy result) throws ResourceException {
                        resourceSet.setPolicy(result.asJson());
                        return Promises.newSuccessfulPromise(resourceSet);
                    }
                }, new AsyncFunction<ResourceException, ResourceSetDescription, ResourceException>() {
                    @Override
                    public Promise<ResourceSetDescription, ResourceException> apply(ResourceException e) throws ResourceException {
                        return Promises.newSuccessfulPromise(resourceSet);
                    }
                });
    }

    /**
     * Queries resource sets across the resource set store and UMA policy store.
     *
     * @param context The context.
     * @param realm The realm.
     * @param query The aggregated query.
     * @param augmentWithPolicies {@code true} to pull in UMA policies into the resource set.
     * @return A Promise containing the Resource Sets or a ResourceException.
     */
    Promise<Collection<ResourceSetDescription>, ResourceException> getResourceSets(final ServerContext context,
            String realm, final ResourceSetWithPolicyQuery query, final boolean augmentWithPolicies) {
        final Set<ResourceSetDescription> resourceSets;
        try {
            resourceSets = resourceSetStoreFactory.create(realm).query(query.getResourceSetQuery());
        } catch (ServerException e) {
            return Promises.newFailedPromise((ResourceException) new InternalServerErrorException(e));
        }

        Promise<Collection<ResourceSetDescription>, ResourceException> resourceSetsPromise;
        if (query.getPolicyQuery() != null) {
            QueryRequest policyQuery = Requests.newQueryRequest("").setQueryFilter(query.getPolicyQuery());
            resourceSetsPromise = policyService.queryPolicies(context, policyQuery)
                    .thenAsync(new AsyncFunction<Pair<QueryResult, Collection<UmaPolicy>>, Collection<ResourceSetDescription>, ResourceException>() {
                        @Override
                        public Promise<Collection<ResourceSetDescription>, ResourceException> apply(Pair<QueryResult, Collection<UmaPolicy>> result) throws ResourceException {
                            try {
                                return Promises.newSuccessfulPromise(combine(context, query, resourceSets,
                                        result.getSecond(), augmentWithPolicies));
                            } catch (org.forgerock.oauth2.core.exceptions.NotFoundException e) {
                                return Promises.newFailedPromise(
                                        (ResourceException) new InternalServerErrorException(e));
                            } catch (ServerException e) {
                                return Promises.newFailedPromise(
                                        (ResourceException) new InternalServerErrorException(e));
                            }
                        }
                    });
        } else {
            if (augmentWithPolicies) {
                List<Promise<ResourceSetDescription, ResourceException>> promises
                        = new ArrayList<Promise<ResourceSetDescription, ResourceException>>();
                PromiseImpl<ResourceSetDescription, ResourceException> kicker = PromiseImpl.create();
                promises.add(kicker);
                for (ResourceSetDescription resourceSet : resourceSets) {
                    promises.add(augmentResourceSetWithPolicy(context, resourceSet.getId(), resourceSet));
                }
                resourceSetsPromise = Promises.when(promises)
                        .thenAsync(new AsyncFunction<List<ResourceSetDescription>, Collection<ResourceSetDescription>, ResourceException>() {
                            @Override
                            public Promise<Collection<ResourceSetDescription>, ResourceException> apply(List<ResourceSetDescription> resourceSets) {
                                Collection<ResourceSetDescription> resourceSetDescriptions
                                        = new HashSet<ResourceSetDescription>();
                                for (ResourceSetDescription rs : resourceSets) {
                                    if (rs != null) {
                                        resourceSetDescriptions.add(rs);
                                    }
                                }
                                return Promises.newSuccessfulPromise(resourceSetDescriptions);
                            }
                        });
                kicker.handleResult(null);
            } else {
                resourceSetsPromise = Promises.newSuccessfulPromise((Collection<ResourceSetDescription>) resourceSets);
            }
        }
        return resourceSetsPromise;
    }

    private Collection<ResourceSetDescription> combine(ServerContext context,
            ResourceSetWithPolicyQuery resourceSetWithPolicyQuery, Set<ResourceSetDescription> resourceSets,
            Collection<UmaPolicy> policies, boolean augmentWithPolicies)
            throws org.forgerock.oauth2.core.exceptions.NotFoundException, ServerException {

        Map<String, ResourceSetDescription> resourceSetsById = new HashMap<String, ResourceSetDescription>();
        Map<String, UmaPolicy> policiesById = new HashMap<String, UmaPolicy>();

        for (ResourceSetDescription resourceSet : resourceSets) {
            resourceSetsById.put(resourceSet.getId(), resourceSet);
        }

        for (UmaPolicy policy : policies) {
            policiesById.put(policy.getId(), policy);
        }

        if ("AND".equals(resourceSetWithPolicyQuery.getOperator())) {
            resourceSetsById.keySet().retainAll(policiesById.keySet());
            if (augmentWithPolicies) {
                for (ResourceSetDescription resourceSet : resourceSetsById.values()) {
                    resourceSet.setPolicy(policiesById.get(resourceSet.getId()).asJson());
                }
            }
        } else if ("OR".equals(resourceSetWithPolicyQuery.getOperator())) {

            if (augmentWithPolicies) {
                for (ResourceSetDescription resourceSet : resourceSetsById.values()) {
                    augmentResourceSetWithPolicy(context, resourceSet.getId(), resourceSet);
                }
            }

            for (Map.Entry<String, UmaPolicy> entry : policiesById.entrySet()) {
                ResourceSetDescription resourceSet;
                if (resourceSetsById.containsKey(entry.getKey())) {
                    resourceSet = resourceSetsById.get(entry.getKey());
                } else {
                    RealmContext realmContext = context.asContext(RealmContext.class);
                    resourceSet = resourceSetStoreFactory.create(realmContext.getResolvedRealm())
                            .read(entry.getKey());
                }
                if (augmentWithPolicies) {
                    resourceSet.setPolicy(entry.getValue().asJson());
                }
                resourceSetsById.put(entry.getKey(), resourceSet);
            }
        }

        return resourceSetsById.values();
    }
}
