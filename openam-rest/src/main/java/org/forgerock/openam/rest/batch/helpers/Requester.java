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
package org.forgerock.openam.rest.batch.helpers;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.http.Context;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.http.context.ServerContext;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openam.utils.StringUtils;
import org.forgerock.util.Reject;

/**
 * Helper class for performing requests to endpoints via an internal router. The intention is for this to
 * be utilised by the Script components of the REST SDK, however it exists as a general purpose internal
 * router helper.
 *
 * todo : support patch operation
 */
public class Requester {

    public static final String ROUTER = "requesterRouter";

    private final SDKResultHandlerFactory resultHandlerFactory;
    private final Provider<CrestRouter> realmRouterProvider;

    @Inject
    public Requester(@Named(ROUTER) Provider<CrestRouter> realmRouterProvider,
                     SDKResultHandlerFactory resultHandlerFactory) {
        this.realmRouterProvider = realmRouterProvider;
        this.resultHandlerFactory = resultHandlerFactory;
    }

    private ServerContext getServerContext(Context serverContext) {
        Reject.ifNull(serverContext, "Supplied context must contain an instance of a ServerContext.");

        return serverContext.asContext(ServerContext.class);
    }

    /**
     * Request to create a resource at a specified endpoint.
     *
     * @param location Endpoint destination of this request. May not be null.
     * @param resourceId Resource ID to be created. May be null.
     * @param payload Payload of the resource to generate. May not be null.
     * @param serverContext Server context of this request.
     *                      Must contain a {@link org.forgerock.http.context.ServerContext}.
     * @return The {@link org.forgerock.json.JsonValue} returned from the endpoint.
     * @throws ResourceException If any exception occurred during processing.
     */
    public JsonValue create(String location, String resourceId, JsonValue payload, Context serverContext)
            throws ResourceException {

        Reject.ifTrue(StringUtils.isEmpty(location), "The endpoint destination may not be null or empty.");
        Reject.ifNull(payload, "The payload object to create must not be null.");

        final CrestRouter realmRouter = realmRouterProvider.get();
        final ServerContext selectedContext = getServerContext(serverContext);
        final SDKServerResultHandler<Resource> resultHandler = resultHandlerFactory.getResourceResultHandler();
        final CreateRequest createRequest = Requests.newCreateRequest(location, payload);

        if (resourceId != null) {
            createRequest.setNewResourceId(resourceId);
        }

        realmRouter.handleCreate(selectedContext, createRequest, resultHandler);
        return resultHandler.getResource().getContent();
    }

    /**
     * Request to read a specified resource at a specified endpoint.
     *
     * @param location Endpoint destination of this request. May not be null.
     * @param resourceId Resource ID to read. May not be null.
     * @param serverContext Server context of this request.
     *                      Must contain a {@link org.forgerock.http.context.ServerContext}.
     * @return The {@link org.forgerock.json.JsonValue} returned from the endpoint.
     * @throws ResourceException If any exception occurred during processing.
     */
    public JsonValue read(String location, String resourceId, Context serverContext)
            throws ResourceException {

        Reject.ifTrue(StringUtils.isEmpty(location), "The endpoint destination may not be null or empty.");
        Reject.ifTrue(StringUtils.isEmpty(resourceId), "The resourceId to read may not be null or empty.");

        final CrestRouter realmRouter = realmRouterProvider.get();
        final ServerContext selectedContext = getServerContext(serverContext);
        final SDKServerResultHandler<Resource> resultHandler = resultHandlerFactory.getResourceResultHandler();
        final ReadRequest readRequest = Requests.newReadRequest(location, resourceId);
        realmRouter.handleRead(selectedContext, readRequest, resultHandler);
        return resultHandler.getResource().getContent();
    }

    /**
     * Request to update a specified resource at a specified endpoint.
     *
     * @param location Endpoint destination of this request. May not be null.
     * @param resourceId Resource ID to update. May not be null.
     * @param payload Payload of the updated resource. May not be null.
     * @param serverContext Server context of this request.
     *                      Must contain a {@link org.forgerock.http.context.ServerContext}.
     * @return The {@link org.forgerock.json.JsonValue} returned from the endpoint.
     * @throws ResourceException If any exception occurred during processing.
     */
    public JsonValue update(String location, String resourceId, JsonValue payload, Context serverContext)
            throws ResourceException {

        Reject.ifTrue(StringUtils.isEmpty(location), "The endpoint destination may not be null or empty.");
        Reject.ifTrue(StringUtils.isEmpty(resourceId), "The resourceId to update may not be null or empty.");
        Reject.ifNull(payload, "The payload object to create must not be null.");

        final CrestRouter realmRouter = realmRouterProvider.get();
        final ServerContext selectedContext = getServerContext(serverContext);
        final SDKServerResultHandler<Resource> resultHandler = resultHandlerFactory.getResourceResultHandler();
        final UpdateRequest updateRequest = Requests.newUpdateRequest(location, resourceId, payload);
        realmRouter.handleUpdate(selectedContext, updateRequest, resultHandler);
        return resultHandler.getResource().getContent();
    }

    /**
     * Request to delete a specified resource at a specified endpoint.
     *
     * @param location Endpoint destination of this request. May not be null.
     * @param resourceId Resource ID to delete. May not be null.
     * @param serverContext Server context of this request.
     *                      Must contain a {@link org.forgerock.http.context.ServerContext}.
     * @return The {@link org.forgerock.json.JsonValue} returned from the endpoint.
     * @throws ResourceException If any exception occurred during processing.
     */
    public JsonValue delete(String location, String resourceId, Context serverContext)
            throws ResourceException {

        Reject.ifTrue(StringUtils.isEmpty(location), "The endpoint destination may not be null or empty.");
        Reject.ifTrue(StringUtils.isEmpty(resourceId), "The resourceId to delete may not be null or empty.");

        final CrestRouter realmRouter = realmRouterProvider.get();
        final ServerContext selectedContext = getServerContext(serverContext);
        final SDKServerResultHandler<Resource> resultHandler = resultHandlerFactory.getResourceResultHandler();
        final DeleteRequest deleteRequest = Requests.newDeleteRequest(location, resourceId);
        realmRouter.handleDelete(selectedContext, deleteRequest, resultHandler);
        return resultHandler.getResource().getContent();
    }

    /**
     * Request to perform an action at a specified endpoint.
     *
     * @param location Endpoint destination of this request. May not be null.
     * @param resourceId Specific resource ID to perform action on. May be null.
     * @param actionId act ID to delete. May not be null.
     * @param serverContext Server context of this request.
     *                      Must contain a {@link org.forgerock.http.context.ServerContext}.
     * @return The {@link org.forgerock.json.JsonValue} returned from the endpoint.
     * @throws ResourceException If any exception occurred during processing.
     */
    public JsonValue action(String location, String resourceId, String actionId, JsonValue payload,
                                    Context serverContext) throws ResourceException {

        Reject.ifTrue(StringUtils.isEmpty(location), "The endpoint destination may not be null or empty.");
        Reject.ifTrue(StringUtils.isEmpty(actionId), "The specific action to perform may not be null or empty.");

        final CrestRouter realmRouter = realmRouterProvider.get();
        final ServerContext selectedContext = getServerContext(serverContext);
        final SDKServerResultHandler<JsonValue> resultHandler = resultHandlerFactory.getJsonValueResultHandler();
        final ActionRequest actionRequest = Requests.newActionRequest(location, actionId);

        if (payload != null) {
            actionRequest.setContent(payload);
        }

        if (resourceId != null) {
            actionRequest.setResourceName(resourceId);
        }

        realmRouter.handleAction(selectedContext, actionRequest, resultHandler);
        return resultHandler.getResource();

    }

    /**
     * Request to perform a query at a specified endpoint.
     *
     * @param location Endpoint destination of this request. May not be null.
     * @param queryId Specific query ID to perform. May be null.
     * @param serverContext Server context of this request.
     *                      Must contain a {@link org.forgerock.http.context.ServerContext}.
     * @return The {@link org.forgerock.json.JsonValue} returned from the endpoint.
     * @throws ResourceException If any exception occurred during processing.
     */
    public JsonValue query(String location, String queryId, Context serverContext)
            throws ResourceException {

        Reject.ifTrue(StringUtils.isEmpty(location), "The endpoint destination may not be null or empty.");

        final CrestRouter realmRouter = realmRouterProvider.get();
        final ServerContext selectedContext = getServerContext(serverContext);
        final SDKServerQueryResultHandler resultHandler = resultHandlerFactory.getQueryResultHandler();
        final QueryRequest queryRequest = Requests.newQueryRequest(location);

        if (queryId != null) {
            queryRequest.setQueryId(queryId);
        }

        realmRouter.handleQuery(selectedContext, queryRequest, resultHandler);
        return resultHandler.getResource();
    }
}
