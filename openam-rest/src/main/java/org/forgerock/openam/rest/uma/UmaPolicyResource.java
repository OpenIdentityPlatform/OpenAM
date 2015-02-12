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

package org.forgerock.openam.rest.uma;

import static org.forgerock.json.fluent.JsonValue.json;
import static org.forgerock.json.fluent.JsonValue.object;

import javax.inject.Inject;
import java.util.Collection;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.CollectionResourceProvider;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.NotSupportedException;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResult;
import org.forgerock.json.resource.QueryResultHandler;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openam.forgerockrest.entitlements.query.QueryResultHandlerBuilder;
import org.forgerock.openam.uma.UmaPolicy;
import org.forgerock.openam.uma.UmaPolicyService;
import org.forgerock.util.Pair;
import org.forgerock.util.promise.FailureHandler;
import org.forgerock.util.promise.SuccessHandler;

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
    public void createInstance(ServerContext context, CreateRequest request, final ResultHandler<Resource> handler) {
        umaPolicyService.createPolicy(context, request.getContent())
                .then(new SuccessHandler<UmaPolicy>() {
                    @Override
                    public void handleResult(UmaPolicy result) {
                        handler.handleResult(new Resource(result.getId(), result.getRevision(), json(object())));
                    }
                }, new FailureHandler<ResourceException>() {
                    @Override
                    public void handleError(ResourceException error) {
                        handler.handleError(error);
                    }
                });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void readInstance(ServerContext context, final String resourceId, ReadRequest request,
            final ResultHandler<Resource> handler) {
        umaPolicyService.readPolicy(context, resourceId)
                .then(new SuccessHandler<UmaPolicy>() {
                    @Override
                    public void handleResult(UmaPolicy result) {
                        handler.handleResult(new Resource(result.getId(), result.getRevision(), result.asJson()));
                    }
                }, new FailureHandler<ResourceException>() {
                    @Override
                    public void handleError(ResourceException error) {
                        handler.handleError(error);
                    }
                });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateInstance(ServerContext context, String resourceId, UpdateRequest request,
            final ResultHandler<Resource> handler) {
        umaPolicyService.updatePolicy(context, resourceId, request.getContent())
                .then(new SuccessHandler<UmaPolicy>() {
                    @Override
                    public void handleResult(UmaPolicy result) {
                        handler.handleResult(new Resource(result.getId(), result.getRevision(), result.asJson()));
                    }
                }, new FailureHandler<ResourceException>() {
                    @Override
                    public void handleError(ResourceException error) {
                        handler.handleError(error);
                    }
                });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteInstance(ServerContext context, final String resourceId, DeleteRequest request,
            final ResultHandler<Resource> handler) {
        umaPolicyService.deletePolicy(context, resourceId)
                .then(new SuccessHandler<Void>() {
                    @Override
                    public void handleResult(Void result) {
                        handler.handleResult(new Resource(resourceId, "0", json(object())));
                    }
                }, new FailureHandler<ResourceException>() {
                    @Override
                    public void handleError(ResourceException error) {
                        handler.handleError(error);
                    }
                });
    }

    /**
     * {@inheritDoc}
     */
    //No patch support on PolicyResource so we will patch our policy representation and then do an update
    @Override
    public void patchInstance(ServerContext context, String resourceId, PatchRequest request,
            ResultHandler<Resource> handler) {
        handler.handleError(new NotSupportedException());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void actionCollection(ServerContext context, ActionRequest request, ResultHandler<JsonValue> handler) {
        handler.handleError(new NotSupportedException());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void actionInstance(ServerContext context, String resourceId, ActionRequest request,
            ResultHandler<JsonValue> handler) {
        handler.handleError(new NotSupportedException());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void queryCollection(ServerContext context, QueryRequest request, QueryResultHandler handler) {
        final QueryResultHandler resultHandler = new QueryResultHandlerBuilder(handler)
                .withPaging(request.getPageSize(), request.getPagedResultsOffset()).build();
        umaPolicyService.queryPolicies(context, request)
                .then(new SuccessHandler<Pair<QueryResult, Collection<UmaPolicy>>>() {
                    @Override
                    public void handleResult(Pair<QueryResult, Collection<UmaPolicy>> result) {
                        for (UmaPolicy policy : result.getSecond()) {
                            resultHandler.handleResource(new Resource(policy.getId(), policy.getRevision(), policy.asJson()));
                        }
                        resultHandler.handleResult(result.getFirst());
                    }
                }, new FailureHandler<ResourceException>() {
                    @Override
                    public void handleError(ResourceException error) {
                        resultHandler.handleError(error);
                    }
                });
    }
}
