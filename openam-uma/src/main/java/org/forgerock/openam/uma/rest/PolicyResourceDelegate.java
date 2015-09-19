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

import static org.forgerock.openam.uma.UmaConstants.UMA_BACKEND_POLICY_RESOURCE_HANDLER;
import static org.forgerock.util.promise.Promises.newExceptionPromise;
import static org.forgerock.util.promise.Promises.newResultPromise;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.forgerock.services.context.Context;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.util.AsyncFunction;
import org.forgerock.util.Pair;
import org.forgerock.util.promise.Promise;
import org.forgerock.util.promise.Promises;
import org.forgerock.util.promise.ResultHandler;

/**
 * Delegate that provide methods to create, update, delete and query sets of underlying backend policies.
 *
 * @since 13.0.0
 */
@Singleton
public class PolicyResourceDelegate {

    private final RequestHandler policyResource;

    /**
     * Constructs an instance of the {@code PolicyResourceDelegate}.
     *
     * @param policyResource An instance of the backend policy resource.
     */
    @Inject
    public PolicyResourceDelegate(@Named(UMA_BACKEND_POLICY_RESOURCE_HANDLER) RequestHandler policyResource) {
        this.policyResource = policyResource;
    }

    /**
     * <p>Creates the underlying backend policies.</p>
     *
     * <p>NOTE: if the creation of the underlying policies fails, any successfully
     * created underlying policies will be attempted to be deleted but if the deletion
     * fails, then the underlying policies may be in an inconsistent state.</p>
     *
     * @param context The request context.
     * @param policies The underlying policies to create.
     * @return A promise containing the list of created underlying policies or a {@code ResourceException} if
     * the creation fails.
     */
    public Promise<List<ResourceResponse>, ResourceException> createPolicies(Context context,
            Set<JsonValue> policies) {

        final List<String> policyIds = new ArrayList<String>();
        List<Promise<ResourceResponse, ResourceException>> promises = new ArrayList<>();
        for (JsonValue policy : policies) {
            promises.add(policyResource.handleCreate(context, Requests.newCreateRequest("", policy))
                    .thenOnResult(new ResultHandler<ResourceResponse>() {
                        @Override
                        public void handleResult(ResourceResponse result) {
                            //Save ids of created policies, in case a latter policy fails to be created,
                            // so we can roll back.
                            policyIds.add(result.getId());
                        }
                    }));
        }
        return Promises.when(promises)
                .thenAsync(new AsyncFunction<List<ResourceResponse>, List<ResourceResponse>, ResourceException>() {
                    @Override
                    public Promise<List<ResourceResponse>, ResourceException> apply(List<ResourceResponse> value) {
                        return newResultPromise(value);
                    }
                }, new UmaPolicyCreateFailureHandler(context, policyIds));
    }

    /**
     * <p>Updates the underlying backend policies.</p>
     *
     * <p>NOTE: if the update of the underlying policies fails, the underlying policies may
     * be in an inconsistent state.</p>
     *
     * @param context The request context.
     * @param policies The updated underlying policies to update.
     * @return A promise containing the list of updated underlying policies or a {@code ResourceException} if
     * the update failed.
     */
    public Promise<List<ResourceResponse>, ResourceException> updatePolicies(Context context,
            Set<JsonValue> policies) {
        List<Promise<ResourceResponse, ResourceException>> promises = new ArrayList<>();
        for (JsonValue policy : policies) {
            String policyName = policy.get("name").asString();
            promises.add(policyResource.handleUpdate(context, Requests.newUpdateRequest(policyName, policy)));
        }
        return Promises.when(promises);
    }

    /**
     * Queries the underlying backend policies.
     *
     * @param context The request context.
     * @param request The query request to execute on the backend policies.
     * @return A promise contain the {@code QueryResult} and list of underlying policies or a {@code ResourceException}
     * if the query failed.
     */
    public Promise<Pair<QueryResponse, List<ResourceResponse>>, ResourceException> queryPolicies(Context context,
            QueryRequest request) {
        final List<ResourceResponse> resources = new ArrayList<>();
        return policyResource.handleQuery(context, request, new QueryResourceHandler() {
            @Override
            public boolean handleResource(ResourceResponse resource) {
                resources.add(resource);
                return true;
            }
        })
                .thenAsync(new AsyncFunction<QueryResponse, Pair<QueryResponse, List<ResourceResponse>>, ResourceException>() {
                    @Override
                    public Promise<Pair<QueryResponse, List<ResourceResponse>>, ResourceException> apply(QueryResponse response) {
                        return newResultPromise(Pair.of(response, resources));
                    }
                });
    }

    /**
     * Queries the underlying backend policies.
     *
     * @param context The request context.
     * @param request The query request to execute on the backend policies.
     * @param resultHandler The handler for results.
     * @return A promise contain the {@code QueryResult} and list of underlying policies or a {@code ResourceException}
     * if the query failed.
     */
    public Promise<QueryResponse, ResourceException> queryPolicies(Context context,
            QueryRequest request, QueryResourceHandler resultHandler) {
        return policyResource.handleQuery(context, request, resultHandler);
    }

    /**
     * <p>Deletes the underlying backend policies.</p>
     *
     * <p>NOTE: if the deletion of the underlying policies fails, the underlying policies may
     * be in an inconsistent state.</p>
     *
     * @param context The request context.
     * @param policyIds The list of ids of the underlying backend policies to delete.
     * @return A promise containing the list of underlying policies that were deleted or a {@code ResourceException}
     * if the policies were failed to be deleted.
     */
    public Promise<List<ResourceResponse>, ResourceException> deletePolicies(Context context,
            Collection<String> policyIds) {
        List<Promise<ResourceResponse, ResourceException>> promises = new ArrayList<>();
        for (String policyId : policyIds) {
            promises.add(policyResource.handleDelete(context, Requests.newDeleteRequest(policyId)));
        }
        return Promises.when(promises);
    }

    /**
     * Failure handler for when creation of underlying backend policies fail.
     * This handler will attempt to delete any successfully created underlying policy,
     * if the deletion fails a {@code ResourceException} will be returned in the promise.
     *
     * @since 13.0.0
     */
    private final class UmaPolicyCreateFailureHandler
            implements AsyncFunction<ResourceException, List<ResourceResponse>, ResourceException> {

        private final Context context;
        private final List<String> policyIds;

        private UmaPolicyCreateFailureHandler(Context context, List<String> policyIds) {
            this.context = context;
            this.policyIds = policyIds;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Promise<List<ResourceResponse>, ResourceException> apply(final ResourceException error) {
            List<Promise<ResourceResponse, ResourceException>> promises = new ArrayList<>();
            for (String id : policyIds) {
                promises.add(policyResource.handleDelete(context, Requests.newDeleteRequest(id)));
            }
            Promise<List<ResourceResponse>, ResourceException> promise = Promises.when(promises)
                    .thenAsync(new AsyncFunction<List<ResourceResponse>, List<ResourceResponse>, ResourceException>() {
                        @Override
                        public Promise<List<ResourceResponse>, ResourceException> apply(List<ResourceResponse> value) {
                            //If we succeed in deleting then return the original error
                            return newExceptionPromise(error);
                        }
                    });
            return promise;
        }
    }
}
