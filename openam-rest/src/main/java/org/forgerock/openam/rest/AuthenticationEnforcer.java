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

package org.forgerock.openam.rest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.forgerock.services.context.Context;
import org.forgerock.http.protocol.Status;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.Filter;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.Response;
import org.forgerock.services.context.SecurityContext;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.util.promise.Promise;

/**
 * A Filter implementation which can be configured to enforce whether
 * authentication is required to access the endpoint.
 *
 * @since 13.0.0
 */
public class AuthenticationEnforcer implements Filter {

    private boolean exceptCreate = false;
    private boolean exceptRead = false;
    private boolean exceptUpdate = false;
    private boolean exceptDelete = false;
    private boolean exceptPatch = false;
    private List<String> exceptActions = new ArrayList<>();
    private boolean exceptQuery = false;

    /**
     * Marks authentication on create requests to the route as optional.
     *
     * @return This filter.
     */
    public AuthenticationEnforcer exceptCreate() {
        exceptCreate = true;
        return this;
    }

    /**
     * Marks authentication on read requests to the route as optional.
     *
     * @return This filter.
     */
    public AuthenticationEnforcer exceptRead() {
        exceptRead = true;
        return this;
    }

    /**
     * Marks authentication on update requests to the route as optional.
     *
     * @return This filter.
     */
    public AuthenticationEnforcer exceptUpdate() {
        exceptUpdate = true;
        return this;
    }

    /**
     * Marks authentication on delete requests to the route as optional.
     *
     * @return This filter.
     */
    public AuthenticationEnforcer exceptDelete() {
        exceptDelete = true;
        return this;
    }

    /**
     * Marks authentication on patch requests to the route as optional.
     *
     * @return This filter.
     */
    public AuthenticationEnforcer exceptPatch() {
        exceptPatch = true;
        return this;
    }

    /**
     * Marks authentication on action requests, with the specified action ids,
     * to the route as optional.
     *
     * @param actions The excluded actions.
     * @return This filter.
     */
    public AuthenticationEnforcer exceptActions(String... actions) {
        exceptActions.addAll(Arrays.asList(actions));
        return this;
    }

    /**
     * Marks authentication on query requests to the route as optional.
     *
     * @return This filter.
     */
    public AuthenticationEnforcer exceptQuery() {
        exceptQuery = true;
        return this;
    }

    @Override
    public Promise<ActionResponse, ResourceException> filterAction(Context context, ActionRequest request,
            RequestHandler next) {
        if (!exceptActions.contains(request.getAction()) && !isAuthenticated(context)) {
            return unauthorizedResponse();
        }
        return next.handleAction(context, request);
    }

    @Override
    public Promise<ResourceResponse, ResourceException> filterCreate(Context context, CreateRequest request,
            RequestHandler next) {
        if (!exceptCreate && !isAuthenticated(context)) {
            return unauthorizedResponse();
        }
        return next.handleCreate(context, request);
    }

    @Override
    public Promise<ResourceResponse, ResourceException> filterDelete(Context context, DeleteRequest request,
            RequestHandler next) {
        if (!exceptDelete && !isAuthenticated(context)) {
            return unauthorizedResponse();
        }
        return next.handleDelete(context, request);
    }

    @Override
    public Promise<ResourceResponse, ResourceException> filterPatch(Context context, PatchRequest request,
            RequestHandler next) {
        if (!exceptPatch && !isAuthenticated(context)) {
            return unauthorizedResponse();
        }
        return next.handlePatch(context, request);
    }

    @Override
    public Promise<QueryResponse, ResourceException> filterQuery(Context context, QueryRequest request,
            QueryResourceHandler handler, RequestHandler next) {
        if (!exceptQuery && !isAuthenticated(context)) {
            return unauthorizedResponse();
        }
        return next.handleQuery(context, request, handler);
    }

    @Override
    public Promise<ResourceResponse, ResourceException> filterRead(Context context, ReadRequest request,
            RequestHandler next) {
        if (!exceptRead && !isAuthenticated(context)) {
            return unauthorizedResponse();
        }
        return next.handleRead(context, request);
    }

    @Override
    public Promise<ResourceResponse, ResourceException> filterUpdate(Context context, UpdateRequest request,
            RequestHandler next) {
        if (!exceptUpdate && !isAuthenticated(context)) {
            return unauthorizedResponse();
        }
        return next.handleUpdate(context, request);
    }

    private boolean isAuthenticated(Context context) {
        return context.containsContext(SecurityContext.class)
                && ServerContextHelper.getCookieFromServerContext(context) != null;
    }

    private <T extends Response> Promise<T, ResourceException> unauthorizedResponse() {
        return ResourceException.getException(Status.UNAUTHORIZED.getCode(), "Access Denied").asPromise();
    }
}
