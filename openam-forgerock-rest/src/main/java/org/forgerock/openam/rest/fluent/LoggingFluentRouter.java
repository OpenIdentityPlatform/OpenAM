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

import javax.inject.Named;
import com.sun.identity.shared.debug.Debug;
import javax.inject.Inject;
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
import org.forgerock.openam.forgerockrest.utils.LoggingUtils;
import org.forgerock.openam.rest.CrestRouter;
import org.forgerock.openam.rest.fluent.FluentRouter;
import org.forgerock.openam.rest.resource.RealmContext;
import org.forgerock.openam.rest.resource.SSOTokenContext;

/**
 * Fluent Router which will log any requests that pass through it.
 */
public class LoggingFluentRouter<T extends CrestRouter> extends FluentRouter<T> {

    private final Debug debug;

    @Inject
    public LoggingFluentRouter(@Named("frRest") Debug debug) {
        this.debug = debug;
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
        log(request.getResourceName(), "ACTION", context);
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
        log(request.getResourceName(), "CREATE", context);
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
        log(request.getResourceName(), "DELETE", context);
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
        log(request.getResourceName(), "PATCH", context);
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
        log(request.getResourceName(), "QUERY", context);
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
        log(request.getResourceName(), "READ", context);
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
        log(request.getResourceName(), "UPDATE", context);
        super.handleUpdate(context, request, handler);
    }

    /**
     * Pushes off to our logging subsystem.
     */
    private void log(String resource, String operation, ServerContext context) {
        String realm;
        try {
            RealmContext realmContext = context.asContext(RealmContext.class);
            realm = realmContext.getRealm().equals("") ? "/" : realmContext.getRealm();
        } catch (IllegalArgumentException iae) {
            //thrown if no realm context found
            realm = null;
        }

        //ensures if there's an SSOToken we can access the user's name
        if (!context.containsContext(SSOTokenContext.class)) {
            context = new SSOTokenContext(context);
        }

        LoggingUtils.logOperationAttemptAsPrincipal(resource, operation, context, realm, debug);
    }

}
