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
package org.forgerock.openam.scripting.rest.batch.helpers;

import static org.forgerock.util.promise.Promises.newResultPromise;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.forgerock.services.context.Context;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.Router;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openam.utils.JsonArray;
import org.forgerock.openam.utils.JsonValueBuilder;
import org.forgerock.openam.utils.StringUtils;
import org.forgerock.util.AsyncFunction;
import org.forgerock.util.Reject;
import org.forgerock.util.promise.Promise;

/**
 * Helper class for performing requests to endpoints via an internal router. The intention is for this to
 * be utilised by the Script components of the REST SDK, however it exists as a general purpose internal
 * router helper.
 *
 * todo : support patch operation
 */
public class Requester {

    private final Provider<Router> router;

    @Inject
    public Requester(@Named("CrestRootRouter") Provider<Router> router) {
        this.router = router;
    }

    /**
     * Request to create a resource at a specified endpoint.
     *
     * @param location Endpoint destination of this request. May not be null.
     * @param resourceId Resource ID to be created. May be null.
     * @param payload Payload of the resource to generate. May not be null.
     * @param context Context of this request.
     * @return The {@link org.forgerock.json.JsonValue} returned from the endpoint.
     * @throws ResourceException If any exception occurred during processing.
     */
    public JsonValue create(String location, String resourceId, JsonValue payload, Context context)
            throws ResourceException {

        Reject.ifTrue(StringUtils.isEmpty(location), "The endpoint destination may not be null or empty.");
        Reject.ifNull(payload, "The payload object to create must not be null.");

        final Router rootRouter = router.get();
        final CreateRequest createRequest = Requests.newCreateRequest(location, payload);

        if (resourceId != null) {
            createRequest.setNewResourceId(resourceId);
        }

        return rootRouter.handleCreate(context, createRequest).getOrThrowUninterruptibly().getContent();
    }

    /**
     * Request to read a specified resource at a specified endpoint.
     *
     * @param location Endpoint destination of this request. May not be null.
     * @param resourceId Resource ID to read. May not be null.
     * @param context Context of this request.
     * @return The {@link org.forgerock.json.JsonValue} returned from the endpoint.
     * @throws ResourceException If any exception occurred during processing.
     */
    public JsonValue read(String location, String resourceId, Context context)
            throws ResourceException {

        Reject.ifTrue(StringUtils.isEmpty(location), "The endpoint destination may not be null or empty.");
        Reject.ifTrue(StringUtils.isEmpty(resourceId), "The resourceId to read may not be null or empty.");

        final Router rootRouter = router.get();
        final ReadRequest readRequest = Requests.newReadRequest(location, resourceId);
        return rootRouter.handleRead(context, readRequest).getOrThrowUninterruptibly().getContent();
    }

    /**
     * Request to update a specified resource at a specified endpoint.
     *
     * @param location Endpoint destination of this request. May not be null.
     * @param resourceId Resource ID to update. May not be null.
     * @param payload Payload of the updated resource. May not be null.
     * @param context Context of this request.
     * @return The {@link org.forgerock.json.JsonValue} returned from the endpoint.
     * @throws ResourceException If any exception occurred during processing.
     */
    public JsonValue update(String location, String resourceId, JsonValue payload, Context context)
            throws ResourceException {

        Reject.ifTrue(StringUtils.isEmpty(location), "The endpoint destination may not be null or empty.");
        Reject.ifTrue(StringUtils.isEmpty(resourceId), "The resourceId to update may not be null or empty.");
        Reject.ifNull(payload, "The payload object to create must not be null.");

        final Router rootRouter = router.get();
        final UpdateRequest updateRequest = Requests.newUpdateRequest(location, resourceId, payload);
        return rootRouter.handleUpdate(context, updateRequest).getOrThrowUninterruptibly().getContent();
    }

    /**
     * Request to delete a specified resource at a specified endpoint.
     *
     * @param location Endpoint destination of this request. May not be null.
     * @param resourceId Resource ID to delete. May not be null.
     * @param context Context of this request.
     * @return The {@link org.forgerock.json.JsonValue} returned from the endpoint.
     * @throws ResourceException If any exception occurred during processing.
     */
    public JsonValue delete(String location, String resourceId, Context context)
            throws ResourceException {

        Reject.ifTrue(StringUtils.isEmpty(location), "The endpoint destination may not be null or empty.");
        Reject.ifTrue(StringUtils.isEmpty(resourceId), "The resourceId to delete may not be null or empty.");

        final Router rootRouter = router.get();
        final DeleteRequest deleteRequest = Requests.newDeleteRequest(location, resourceId);
        return rootRouter.handleDelete(context, deleteRequest).getOrThrowUninterruptibly().getContent();
    }

    /**
     * Request to perform an action at a specified endpoint.
     *
     * @param location Endpoint destination of this request. May not be null.
     * @param resourceId Specific resource ID to perform action on. May be null.
     * @param actionId act ID to delete. May not be null.
     * @param context Context of this request.
     * @return The {@link org.forgerock.json.JsonValue} returned from the endpoint.
     * @throws ResourceException If any exception occurred during processing.
     */
    public JsonValue action(String location, String resourceId, String actionId, JsonValue payload,
                                    Context context) throws ResourceException {

        Reject.ifTrue(StringUtils.isEmpty(location), "The endpoint destination may not be null or empty.");
        Reject.ifTrue(StringUtils.isEmpty(actionId), "The specific action to perform may not be null or empty.");

        final Router rootRouter = router.get();
        final ActionRequest actionRequest = Requests.newActionRequest(location, actionId);

        if (payload != null) {
            actionRequest.setContent(payload);
        }

        if (resourceId != null) {
            actionRequest.setResourcePath(resourceId);
        }

        return rootRouter.handleAction(context, actionRequest).getOrThrowUninterruptibly().getJsonContent();

    }

    /**
     * Request to perform a query at a specified endpoint.
     *
     * @param location Endpoint destination of this request. May not be null.
     * @param queryId Specific query ID to perform. May be null.
     * @param context Context of this request.
     * @return The {@link org.forgerock.json.JsonValue} returned from the endpoint.
     * @throws ResourceException If any exception occurred during processing.
     */
    public JsonValue query(String location, String queryId, Context context)
            throws ResourceException {

        Reject.ifTrue(StringUtils.isEmpty(location), "The endpoint destination may not be null or empty.");

        final Router rootRouter = router.get();
        final QueryRequest queryRequest = Requests.newQueryRequest(location);

        if (queryId != null) {
            queryRequest.setQueryId(queryId);
        }

        final InMemoryQueryResourceHandler resourceHandler = new InMemoryQueryResourceHandler();
        return rootRouter.handleQuery(context, queryRequest, resourceHandler)
                .thenAsync(new AsyncFunction<QueryResponse, JsonValue, ResourceException>() {
                    @Override
                    public Promise<JsonValue, ResourceException> apply(QueryResponse value) {
                        final JsonArray responses = JsonValueBuilder.jsonValue().array("results");
                        for (ResourceResponse resource : resourceHandler.getResources()) {
                            responses.add(resource.getContent());
                        }
                        return newResultPromise(responses.build().build());
                    }
                }).getOrThrowUninterruptibly();
    }

    private static final class InMemoryQueryResourceHandler implements QueryResourceHandler {

        private final List<ResourceResponse> resources = new ArrayList<>();

        @Override
        public boolean handleResource(ResourceResponse resource) {
            resources.add(resource);
            return true;
        }

        Collection<ResourceResponse> getResources() {
            return resources;
        }
    }
}
