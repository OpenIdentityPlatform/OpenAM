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

import org.forgerock.http.Context;
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
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.util.promise.Promise;

/**
 *
 */
public class AuthenticationFilter implements Filter {

    private final Filter authenticationFilter;
    private final AuthenticationModule authenticationModule;

    AuthenticationFilter(Filter authenticationFilter, AuthenticationModule authenticationModule) {
        this.authenticationFilter = authenticationFilter;
        this.authenticationModule = authenticationModule;
    }

    public AuthenticationFilter exceptCreate() {
        authenticationModule.exceptCreate();
        return this;
    }

    public AuthenticationFilter exceptRead() {
        authenticationModule.exceptRead();
        return this;
    }

    public AuthenticationFilter exceptUpdate() {
        authenticationModule.exceptUpdate();
        return this;
    }

    public AuthenticationFilter exceptDelete() {
        authenticationModule.exceptDelete();
        return this;
    }

    public AuthenticationFilter exceptPatch() {
        authenticationModule.exceptPatch();
        return this;
    }

    public AuthenticationFilter exceptActions(String... actions) {
        authenticationModule.exceptActions(actions);
        return this;
    }

    public AuthenticationFilter exceptQuery() {
        authenticationModule.exceptQuery();
        return this;
    }

    @Override
    public Promise<ActionResponse, ResourceException> filterAction(Context context, ActionRequest request,
            RequestHandler next) {
        return authenticationFilter.filterAction(context, request, next);
    }

    @Override
    public Promise<ResourceResponse, ResourceException> filterCreate(Context context, CreateRequest request,
            RequestHandler next) {
        return authenticationFilter.filterCreate(context, request, next);
    }

    @Override
    public Promise<ResourceResponse, ResourceException> filterDelete(Context context, DeleteRequest request,
            RequestHandler next) {
        return authenticationFilter.filterDelete(context, request, next);
    }

    @Override
    public Promise<ResourceResponse, ResourceException> filterPatch(Context context, PatchRequest request,
            RequestHandler next) {
        return authenticationFilter.filterPatch(context, request, next);
    }

    @Override
    public Promise<QueryResponse, ResourceException> filterQuery(Context context, QueryRequest request,
            QueryResourceHandler handler, RequestHandler next) {
        return authenticationFilter.filterQuery(context, request, handler, next);
    }

    @Override
    public Promise<ResourceResponse, ResourceException> filterRead(Context context, ReadRequest request,
            RequestHandler next) {
        return authenticationFilter.filterRead(context, request, next);
    }

    @Override
    public Promise<ResourceResponse, ResourceException> filterUpdate(Context context, UpdateRequest request,
            RequestHandler next) {
        return authenticationFilter.filterUpdate(context, request, next);
    }
}
