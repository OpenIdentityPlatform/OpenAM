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

import javax.inject.Inject;

import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.Filter;
import org.forgerock.json.resource.NotSupportedException;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResultHandler;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.Resource;
import org.forgerock.json.resource.ResultHandler;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.oauth2.core.exceptions.NotFoundException;
import org.forgerock.openam.forgerockrest.utils.RequestHolder;
import org.forgerock.openam.forgerockrest.utils.ServerContextUtils;
import org.forgerock.openam.uma.UmaProviderSettings;
import org.forgerock.openam.uma.UmaProviderSettingsFactory;

/**
 * Checks that an UMA Provider has been configured for the current realm, and returns not supported if
 * it is not.
 */
public class UmaEnabledFilter implements Filter {

    private final UmaProviderSettingsFactory umaProviderSettingsFactory;

    @Inject
    public UmaEnabledFilter(UmaProviderSettingsFactory umaProviderSettingsFactory) {
        this.umaProviderSettingsFactory = umaProviderSettingsFactory;
    }

    private boolean enabled(ServerContext serverContext, ResultHandler<?> resultHandler) {
        try {
            final String realm = ServerContextUtils.getRealm(serverContext);
            UmaProviderSettings settings = umaProviderSettingsFactory.get(RequestHolder.get(), realm);
            if (settings.isEnabled()) {
                return true;
            }
        } catch (NotFoundException e) { }
        resultHandler.handleError(new NotSupportedException("UMA is not currently supported in this realm"));
        return false;
    }

    @Override
    public void filterAction(ServerContext serverContext, ActionRequest request, ResultHandler<JsonValue> resultHandler, RequestHandler requestHandler) {
        if (enabled(serverContext, resultHandler)) {
            requestHandler.handleAction(serverContext, request, resultHandler);
        }
    }

    @Override
    public void filterCreate(ServerContext serverContext, CreateRequest request, ResultHandler<Resource> resultHandler, RequestHandler requestHandler) {
        if (enabled(serverContext, resultHandler)) {
            requestHandler.handleCreate(serverContext, request, resultHandler);
        }
    }

    @Override
    public void filterDelete(ServerContext serverContext, DeleteRequest request, ResultHandler<Resource> resultHandler, RequestHandler requestHandler) {
        if (enabled(serverContext, resultHandler)) {
            requestHandler.handleDelete(serverContext, request, resultHandler);
        }
    }

    @Override
    public void filterPatch(ServerContext serverContext, PatchRequest request, ResultHandler<Resource> resultHandler, RequestHandler requestHandler) {
        if (enabled(serverContext, resultHandler)) {
            requestHandler.handlePatch(serverContext, request, resultHandler);
        }
    }

    @Override
    public void filterQuery(ServerContext serverContext, QueryRequest request, QueryResultHandler queryResultHandler, RequestHandler requestHandler) {
        if (enabled(serverContext, queryResultHandler)) {
            requestHandler.handleQuery(serverContext, request, queryResultHandler);
        }
    }

    @Override
    public void filterRead(ServerContext serverContext, ReadRequest request, ResultHandler<Resource> resultHandler, RequestHandler requestHandler) {
        if (enabled(serverContext, resultHandler)) {
            requestHandler.handleRead(serverContext, request, resultHandler);
        }
    }

    @Override
    public void filterUpdate(ServerContext serverContext, UpdateRequest request, ResultHandler<Resource> resultHandler, RequestHandler requestHandler) {
        if (enabled(serverContext, resultHandler)) {
            requestHandler.handleUpdate(serverContext, request, resultHandler);
        }
    }
}
