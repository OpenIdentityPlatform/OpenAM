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

package org.forgerock.openam.rest.resource;

import java.util.List;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResult;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.util.Pair;
import org.forgerock.util.promise.Promise;

/**
 * <p>Represents the contract with a set of resources.</p>
 *
 * <p>Offers the same contract as a {@link org.forgerock.json.resource.RequestHandler}
 * but instead of requiring the implementation to call methods on a
 * {@link org.forgerock.json.resource.ResultHandler} to signal the end of processing,
 * it returns a {@link Promise} that will be must be completed when processing
 * has completed.</p>
 *
 * @see org.forgerock.json.resource.RequestHandler
 * @since 13.0.0
 */
public interface PromisedRequestHandler {

    /**
     * <p>Handles performing an action on a resource, and optionally returns an
     * associated result. The execution of an action is allowed to incur side
     * effects.</p>
     *
     * <p>On completion, the action result (or null) or on failure, an exception
     * must be set on the returned promise.</p>
     *
     * @param context The request server context, such as associated principal.
     * @param request The action request.
     * @return A promise containing the action result or a {@code ResourceException}.
     * @see org.forgerock.json.resource.RequestHandler#handleAction(ServerContext,
     * ActionRequest, org.forgerock.json.resource.ResultHandler)
     */
    Promise<JsonValue, ResourceException> handleAction(ServerContext context, ActionRequest request);

    /**
     * Adds a new JSON resource, setting the resulting resource on the returned promise.
     *
     * @param context The request server context, such as associated principal.
     * @param request The create request.
     * @return A promise containing the created resource or a {@code ResourceException}.
     * @see org.forgerock.json.resource.RequestHandler#handleCreate(ServerContext,
     * CreateRequest, org.forgerock.json.resource.ResultHandler)
     */
    Promise<Resource, ResourceException> handleCreate(ServerContext context, CreateRequest request);

    /**
     * Deletes a JSON resource, setting the resulting resource on the returned promise.
     *
     * @param context The request server context, such as associated principal.
     * @param request The delete request.
     * @return A promise containing the deleted resource or a {@code ResourceException}.
     * @see org.forgerock.json.resource.RequestHandler#handleDelete(ServerContext,
     * DeleteRequest, org.forgerock.json.resource.ResultHandler)
     */
    Promise<Resource, ResourceException> handleDelete(ServerContext context, DeleteRequest request);

    /**
     * Updates a JSON resource by applying a set of changes to its existing
     * content, setting the resulting resource on the returned promise.
     *
     * @param context The request server context, such as associated principal.
     * @param request The patch request.
     * @return A promise containing the patches resource or a {@code ResourceException}.
     * @see org.forgerock.json.resource.RequestHandler#handlePatch(ServerContext,
     * PatchRequest, org.forgerock.json.resource.ResultHandler)
     */
    Promise<Resource, ResourceException> handlePatch(ServerContext context, PatchRequest request);

    /**
     * Searches for all JSON resources matching a user specified set of
     * criteria, setting the resulting query result and resources on the
     * returned promise.
     *
     * @param context The request server context, such as associated principal.
     * @param request The query request.
     * @return A promise containing the query result and list of resources or a {@code ResourceException}.
     * @see org.forgerock.json.resource.RequestHandler#handlePatch(ServerContext,
     * PatchRequest, org.forgerock.json.resource.ResultHandler)
     */
    Promise<Pair<QueryResult, List<Resource>>, ResourceException> handleQuery(ServerContext context,
            QueryRequest request);

    /**
     * Reads a JSON resource, setting the resulting resource on the returned
     * promise.
     *
     * @param context The request server context, such as associated principal.
     * @param request The read request.
     * @return A promise containing the resource or a {@code ResourceException}.
     * @see org.forgerock.json.resource.RequestHandler#handleRead(ServerContext,
     * ReadRequest, org.forgerock.json.resource.ResultHandler)
     */
    Promise<Resource, ResourceException> handleRead(ServerContext context, ReadRequest request);

    /**
     * Updates a JSON resource by replacing its existing content with new
     * content, setting the resulting resource on the returned promise.
     *
     * @param context The request server context, such as associated principal.
     * @param request The update request.
     * @return A promise containing the updated resource or a {@code ResourceException}.
     * @see org.forgerock.json.resource.RequestHandler#handleUpdate(ServerContext,
     * UpdateRequest, org.forgerock.json.resource.ResultHandler)
     */
    Promise<Resource, ResourceException> handleUpdate(ServerContext context, UpdateRequest request);
}
