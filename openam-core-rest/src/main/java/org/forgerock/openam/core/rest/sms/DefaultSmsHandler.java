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
package org.forgerock.openam.core.rest.sms;

import static org.forgerock.json.resource.Responses.*;
import static org.forgerock.openam.rest.RestConstants.*;
import static org.forgerock.util.promise.Promises.*;

import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.NotSupportedException;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openam.rest.RestUtils;
import org.forgerock.services.context.Context;
import org.forgerock.util.promise.Promise;

/**
 * Blocks requests to unsupported functions to the endpoint with unsupported messages,
 * while defining the set of supported actions for all endpoints below this level.
 */
public abstract class DefaultSmsHandler implements RequestHandler {

    /**
     * Provides support for handleAction of getTemplate, getSchema, getAllTypes and getCreatableTypes.
     * The impl. of each of these is handled by the concrete implementations of the DefaultSmsHandler -
     * each of the auto-generated endpoints (those created by analysing service schemas) have their
     * basic functionality supported in {@link SmsResourceProvider}.
     */
    @Override
    public Promise<ActionResponse, ResourceException> handleAction(Context context, ActionRequest request) {
        try {
            if (request.getAction().equals(TEMPLATE)) {
                return newResultPromise(newActionResponse(getTemplate(context, request)));
            } else if (SCHEMA.equals(request.getAction())) {
                return newResultPromise(newActionResponse(getSchema(context, request)));
            } else if (GET_ALL_TYPES.equals(request.getAction())) {
                return newResultPromise(newActionResponse(getAllTypes(context, request)));
            } else if (GET_CREATABLE_TYPES.equals(request.getAction())) {
                return newResultPromise(newActionResponse(getCreatableTypes(context, request)));
            } else {
                return new NotSupportedException("Action not supported: " + request.getAction()).asPromise();
            }
        } catch (NotSupportedException e) {
            return new NotSupportedException("Action not supported: " + request.getAction()).asPromise();
        } catch (InternalServerErrorException e) {
            return new InternalServerErrorException("Internal error executing: " + request.getAction()).asPromise();
        }
    }

    /**
     * Gets the subtypes of this service endpoint which have not yet been created, or can
     * still be instantiated.
     *
     * @param context Context of the request.
     * @param request Request to perform action
     * @return JsonValue containing all subtypes of this service.
     * @throws NotSupportedException If this method is not supported for a given endpoint.
     * @throws InternalServerErrorException If this method errors handling the request.
     */
    protected abstract JsonValue getCreatableTypes(Context context, ActionRequest request)
            throws NotSupportedException, InternalServerErrorException;

    /**
     * Gets the template of this service endpoint.
     *
     * @param context Context of the request.
     * @param request Request to perform action
     * @return JsonValue containing all subtypes of this service.
     * @throws NotSupportedException If this method is not supported for a given endpoint.
     * @throws InternalServerErrorException If this method errors handling the request.
     */
    protected abstract JsonValue getTemplate(Context context, ActionRequest request)
            throws NotSupportedException, InternalServerErrorException;


    /**
     * Gets the schema of this service endpoint.
     *
     * @param context Context of the request.
     * @param request Request to perform action
     * @return JsonValue containing all subtypes of this service.
     * @throws NotSupportedException If this method is not supported for a given endpoint.
     * @throws InternalServerErrorException If this method errors handling the request.
     */
    protected abstract JsonValue getSchema(Context context, ActionRequest request)
            throws NotSupportedException, InternalServerErrorException;

    /**
     * Gets all subtypes of this service endpoint. This will return a list of
     * all subtypes regardless of whether they can be instantiated in the system at this point.
     *
     * @param context Context of the request.
     * @param request Request to perform action
     * @return JsonValue containing all subtypes of this service.
     * @throws NotSupportedException If this method is not supported for a given endpoint.
     * @throws InternalServerErrorException If this method errors handling the request.
     */
    protected abstract JsonValue getAllTypes(Context context, ActionRequest request)
            throws NotSupportedException, InternalServerErrorException;

    @Override
    public Promise<ResourceResponse, ResourceException> handleCreate(Context context, CreateRequest createRequest) {
        return RestUtils.generateUnsupportedOperation();
    }

    @Override
    public Promise<ResourceResponse, ResourceException> handleDelete(Context context, DeleteRequest deleteRequest) {
        return RestUtils.generateUnsupportedOperation();
    }

    @Override
    public Promise<ResourceResponse, ResourceException> handlePatch(Context context, PatchRequest patchRequest) {
        return RestUtils.generateUnsupportedOperation();
    }

    @Override
    public Promise<QueryResponse, ResourceException> handleQuery(Context context, QueryRequest queryRequest,
                                                                 QueryResourceHandler queryResourceHandler) {
        return RestUtils.generateUnsupportedOperation();
    }

    @Override
    public Promise<ResourceResponse, ResourceException> handleRead(Context context, ReadRequest readRequest) {
        return RestUtils.generateUnsupportedOperation();
    }

    @Override
    public Promise<ResourceResponse, ResourceException> handleUpdate(Context context, UpdateRequest updateRequest) {
        return RestUtils.generateUnsupportedOperation();
    }
}
