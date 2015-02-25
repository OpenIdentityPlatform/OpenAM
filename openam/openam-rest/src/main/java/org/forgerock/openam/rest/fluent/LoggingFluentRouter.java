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
* Copyright 2014 ForgeRock AS.
*/
package org.forgerock.openam.rest.fluent;

import com.iplanet.sso.SSOException;
import com.sun.identity.shared.debug.Debug;
import javax.inject.Inject;
import javax.inject.Named;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResultHandler;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openam.forgerockrest.utils.RestLog;
import org.forgerock.openam.forgerockrest.utils.ServerContextUtils;
import org.forgerock.openam.rest.resource.CrestRouter;
import org.forgerock.openam.rest.resource.SSOTokenContext;

/**
 * Fluent Router which will audit any requests that pass through it.
 */
public class LoggingFluentRouter<T extends CrestRouter> extends FluentRouter<T> {

    private final Debug debug;
    private final RestLog restLog;

    @Inject
    public LoggingFluentRouter(@Named("frRest") Debug debug, RestLog restLog) {
        this.debug = debug;
        this.restLog = restLog;
    }

    /**
     * Handles performing an action on a resource, and optionally returns an
     * associated result. The request is first logged.
     *
     * @param context {@inheritDoc}
     * @param request {@inheritDoc}
     * @param handler {@inheritDoc}
     */
    @Override
    public void handleAction(ServerContext context, ActionRequest request, ResultHandler<JsonValue> handler) {
        final String resource = ServerContextUtils.getResourceId(request, context);
        final String action = ServerContextUtils.getActionString(request);

        logAccess(resource, action, context);
        super.handleAction(context, request, handler);
    }

    /**
     * Handles performing a create on a resource, and optionally returns an
     * associated result. The request is first logged.
     *
     * @param context {@inheritDoc}
     * @param request {@inheritDoc}
     * @param handler {@inheritDoc}
     */
    @Override
    public void handleCreate(ServerContext context, CreateRequest request, ResultHandler<Resource> handler) {
        final String resource = ServerContextUtils.getResourceId(request, context);
        final String action = ServerContextUtils.getCreateString(request);

        logAccess(resource, action, context);
        super.handleCreate(context, request, handler);
    }

    /**
     * Handles performing a delete on a resource, and optionally returns an
     * associated result. The request is first logged.
     *
     * @param context {@inheritDoc}
     * @param request {@inheritDoc}
     * @param handler {@inheritDoc}
     */
    @Override
    public void handleDelete(ServerContext context, DeleteRequest request, ResultHandler<Resource> handler) {
        final String resource = ServerContextUtils.getResourceId(request, context);
        final String action = ServerContextUtils.getDeleteString(request);

        logAccess(resource, action, context);
        super.handleDelete(context, request, handler);
    }

    /**
     * Handles performing a patch on a resource, and optionally returns an
     * associated result. The request is first logged.
     *
     * @param context {@inheritDoc}
     * @param request {@inheritDoc}
     * @param handler {@inheritDoc}
     */
    @Override
    public void handlePatch(ServerContext context, PatchRequest request, ResultHandler<Resource> handler) {
        final String resource = ServerContextUtils.getResourceId(request, context);
        final String action = ServerContextUtils.getPatchString(request);

        logAccess(resource, action, context);
        super.handlePatch(context, request, handler);
    }

    /**
     * Handles performing a query on a resource, and optionally returns an
     * associated result. The request is first logged.
     *
     * @param context {@inheritDoc}
     * @param request {@inheritDoc}
     * @param handler {@inheritDoc}
     */
    @Override
    public void handleQuery(ServerContext context, QueryRequest request, QueryResultHandler handler) {
        final String resource = ServerContextUtils.getResourceId(request, context);
        final String action = ServerContextUtils.getQueryString(request);

        logAccess(resource, action, context);
        super.handleQuery(context, request, handler);

    }

    /**
     * Handles performing a read on a resource, and optionally returns an
     * associated result. The request is first logged.
     *
     * @param context {@inheritDoc}
     * @param request {@inheritDoc}
     * @param handler {@inheritDoc}
     */
    @Override
    public void handleRead(ServerContext context, ReadRequest request, ResultHandler<Resource> handler) {
        final String resource = ServerContextUtils.getResourceId(request, context);
        final String action = ServerContextUtils.getReadString(request);

        logAccess(resource, action, context);
        super.handleRead(context, request, handler);
    }

    /**
     * Handles performing an update on a resource, and optionally returns an
     * associated result. The request is first logged.
     *
     * @param context {@inheritDoc}
     * @param request {@inheritDoc}
     * @param handler {@inheritDoc}
     */
    @Override
    public void handleUpdate(ServerContext context, UpdateRequest request, ResultHandler<Resource> handler) {
        final String resource = ServerContextUtils.getResourceId(request, context);
        final String action = ServerContextUtils.getUpdateString(request);

        logAccess(resource, action, context);
        super.handleUpdate(context, request, handler);
    }

    /**
     * Pushes off to our logging subsystem.
     */
    private void logAccess(String resource, String operation, ServerContext context) {

        if (!context.containsContext(SSOTokenContext.class)) {
            context = new SSOTokenContext(context);
        }

        SSOTokenContext ssoTokenContext = context.asContext(SSOTokenContext.class);

        try {
            restLog.auditAccessMessage(resource, operation, ssoTokenContext.getCallerSSOToken());
        } catch (SSOException e) {
            if (debug.warningEnabled()) {
                debug.warning("LoggingFluentRouter :: " +
                        "Error retrieving SSO Token from provided context, forced to log user as 'null'.", e);
                restLog.auditAccessMessage(resource, operation, null);
            }
        }

        restLog.debugOperationAttemptAsPrincipal(resource, operation, context, null, debug);
    }

}
