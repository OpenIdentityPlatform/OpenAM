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

import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;
import static org.forgerock.json.resource.Responses.newResourceResponse;
import static org.forgerock.util.promise.Promises.newResultPromise;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.forgerock.services.context.Context;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.CollectionResourceProvider;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.NotSupportedException;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openam.rest.query.QueryResponsePresentation;
import org.forgerock.openam.uma.UmaPolicy;
import org.forgerock.openam.uma.UmaPolicyService;
import org.forgerock.util.AsyncFunction;
import org.forgerock.util.Pair;
import org.forgerock.util.promise.Promise;

/**
 * REST endpoint for UMA policy management.
 *
 * @since 13.0.0
 */
public class UmaPolicyResource implements CollectionResourceProvider {

    private final UmaPolicyService umaPolicyService;

    /**
     * Creates an instance of the {@code UmaPolicyResource}.
     *
     * @param umaPolicyService An instance of the {@code UmaPolicyService}.
     */
    @Inject
    public UmaPolicyResource(UmaPolicyService umaPolicyService) {
        this.umaPolicyService = umaPolicyService;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Promise<ResourceResponse, ResourceException> createInstance(Context context, CreateRequest request) {
        return umaPolicyService.createPolicy(context, request.getContent())
                .thenAsync(new AsyncFunction<UmaPolicy, ResourceResponse, ResourceException>() {
                    @Override
                    public Promise<ResourceResponse, ResourceException> apply(UmaPolicy result) {
                        return newResultPromise(newResourceResponse(result.getId(), result.getRevision(), json(object())));
                    }
                });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Promise<ResourceResponse, ResourceException> readInstance(Context context, final String resourceId,
            ReadRequest request) {
        return umaPolicyService.readPolicy(context, resourceId)
                .thenAsync(new AsyncFunction<UmaPolicy, ResourceResponse, ResourceException>() {
                    @Override
                    public Promise<ResourceResponse, ResourceException> apply(UmaPolicy result) {
                        return newResultPromise(newResourceResponse(result.getId(), result.getRevision(), result.asJson()));
                    }
                });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Promise<ResourceResponse, ResourceException> updateInstance(Context context, String resourceId,
            UpdateRequest request) {
        return umaPolicyService.updatePolicy(context, resourceId, request.getContent())
                .thenAsync(new AsyncFunction<UmaPolicy, ResourceResponse, ResourceException>() {
                    @Override
                    public Promise<ResourceResponse, ResourceException> apply(UmaPolicy result) {
                        return newResultPromise(newResourceResponse(result.getId(), result.getRevision(), result.asJson()));
                    }
                });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Promise<ResourceResponse, ResourceException> deleteInstance(Context context, final String resourceId,
            DeleteRequest request) {
        return umaPolicyService.deletePolicy(context, resourceId)
                .thenAsync(new AsyncFunction<Void, ResourceResponse, ResourceException>() {
                    @Override
                    public Promise<ResourceResponse, ResourceException> apply(Void value) throws ResourceException {
                        return newResultPromise(newResourceResponse(resourceId, "0", json(object())));
                    }
                });
    }

    /**
     * {@inheritDoc}
     */
    //No patch support on PolicyResource so we will patch our policy representation and then do an update
    @Override
    public Promise<ResourceResponse, ResourceException> patchInstance(Context context, String resourceId,
            PatchRequest request) {
        return new NotSupportedException().asPromise();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Promise<ActionResponse, ResourceException> actionCollection(Context context, ActionRequest request) {
        return new NotSupportedException().asPromise();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Promise<ActionResponse, ResourceException> actionInstance(Context context, String resourceId,
            ActionRequest request) {
        return new NotSupportedException().asPromise();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Promise<QueryResponse, ResourceException> queryCollection(Context context, final QueryRequest request,
            final QueryResourceHandler handler) {
        return umaPolicyService.queryPolicies(context, request)
                .thenAsync(new AsyncFunction<Pair<QueryResponse, Collection<UmaPolicy>>, QueryResponse, ResourceException>() {
                    @Override
                    public Promise<QueryResponse, ResourceException> apply(Pair<QueryResponse, Collection<UmaPolicy>> result) {
                        List<ResourceResponse> values = new ArrayList<>();
                        for (UmaPolicy policy : result.getSecond()) {
                            values.add(newResourceResponse(policy.getId(), null, policy.asJson()));
                        }
                        return QueryResponsePresentation.perform(handler, request, values);
                    }
                });
    }
}
