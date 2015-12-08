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
 * Copyright 2014-2015 ForgeRock AS.
 */

package org.forgerock.openam.rest.fluent;

import javax.inject.Inject;
import javax.inject.Named;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.services.context.Context;
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
import org.forgerock.openam.forgerockrest.utils.RestLog;
import org.forgerock.openam.forgerockrest.utils.ServerContextUtils;
import org.forgerock.openam.rest.resource.SSOTokenContext;
import org.forgerock.util.promise.Promise;

/**
 * CREST Filter which will audit any requests that pass through it.
 */
public class CrestLoggingFilter implements Filter {

    private final Debug debug;
    private final RestLog restLog;

    @Inject
    public CrestLoggingFilter(@Named("frRest") Debug debug, RestLog restLog) {
        this.debug = debug;
        this.restLog = restLog;
    }

    /**
     * Handles performing an action on a resource, and optionally returns an
     * associated result. The request is first logged.
     *
     * @param context {@inheritDoc}
     * @param request {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    public Promise<ActionResponse, ResourceException> filterAction(Context context, ActionRequest request,
            RequestHandler next) {
        final String resource = ServerContextUtils.getResourceId(request, context);
        final String action = ServerContextUtils.getActionString(request);

        logAccess(resource, action, context);
        return next.handleAction(context, request);
    }

    /**
     * Handles performing a create on a resource, and optionally returns an
     * associated result. The request is first logged.
     *
     * @param context {@inheritDoc}
     * @param request {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    public Promise<ResourceResponse, ResourceException> filterCreate(Context context, CreateRequest request,
            RequestHandler next) {
        final String resource = ServerContextUtils.getResourceId(request, context);
        final String action = ServerContextUtils.getCreateString(request);

        logAccess(resource, action, context);
        return next.handleCreate(context, request);
    }

    /**
     * Handles performing a delete on a resource, and optionally returns an
     * associated result. The request is first logged.
     *
     * @param context {@inheritDoc}
     * @param request {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    public Promise<ResourceResponse, ResourceException> filterDelete(Context context, DeleteRequest request,
            RequestHandler next) {
        final String resource = ServerContextUtils.getResourceId(request, context);
        final String action = ServerContextUtils.getDeleteString(request);

        logAccess(resource, action, context);
        return next.handleDelete(context, request);
    }

    /**
     * Handles performing a patch on a resource, and optionally returns an
     * associated result. The request is first logged.
     *
     * @param context {@inheritDoc}
     * @param request {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    public Promise<ResourceResponse, ResourceException> filterPatch(Context context, PatchRequest request,
            RequestHandler next) {
        final String resource = ServerContextUtils.getResourceId(request, context);
        final String action = ServerContextUtils.getPatchString(request);

        logAccess(resource, action, context);
        return next.handlePatch(context, request);
    }

    /**
     * Handles performing a query on a resource, and optionally returns an
     * associated result. The request is first logged.
     *
     * @param context {@inheritDoc}
     * @param request {@inheritDoc}
     * @param handler {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    public Promise<QueryResponse, ResourceException> filterQuery(Context context, QueryRequest request,
            QueryResourceHandler handler, RequestHandler next) {
        final String resource = ServerContextUtils.getResourceId(request, context);
        final String action = ServerContextUtils.getQueryString(request);

        logAccess(resource, action, context);
        return next.handleQuery(context, request, handler);

    }

    /**
     * Handles performing a read on a resource, and optionally returns an
     * associated result. The request is first logged.
     *
     * @param context {@inheritDoc}
     * @param request {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    public Promise<ResourceResponse, ResourceException> filterRead(Context context, ReadRequest request,
            RequestHandler next) {
        final String resource = ServerContextUtils.getResourceId(request, context);
        final String action = ServerContextUtils.getReadString(request);

        logAccess(resource, action, context);
        return next.handleRead(context, request);
    }

    /**
     * Handles performing an update on a resource, and optionally returns an
     * associated result. The request is first logged.
     *
     * @param context {@inheritDoc}
     * @param request {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override
    public Promise<ResourceResponse, ResourceException> filterUpdate(Context context, UpdateRequest request,
            RequestHandler next) {
        final String resource = ServerContextUtils.getResourceId(request, context);
        final String action = ServerContextUtils.getUpdateString(request);

        logAccess(resource, action, context);
        return next.handleUpdate(context, request);
    }

    /**
     * Pushes off to our logging subsystem.
     */
    private void logAccess(String resource, String operation, Context context) {
        try {
            SSOToken token = SSOTokenContext.getSsoToken(context);
            restLog.auditAccessMessage(resource, operation, token);
            if (token == null) {
                debug.message("CrestLoggingFilter :: no token from context, logging user as 'null'");
            }
        } catch (SSOException e) {
            if (debug.warningEnabled()) {
                debug.warning("CrestLoggingFilter :: " +
                        "Error retrieving SSO Token from provided context, forced to log user as 'null'.", e);
            }
            restLog.auditAccessMessage(resource, operation, null);
        }

        restLog.debugOperationAttemptAsPrincipal(resource, operation, context, null, debug);
    }

}
