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
package org.forgerock.openam.selfservice;

import org.forgerock.json.resource.AbstractRequestHandler;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.openam.rest.RealmContext;
import org.forgerock.selfservice.core.AnonymousProcessService;
import org.forgerock.selfservice.core.ProcessStore;
import org.forgerock.selfservice.core.ProgressStageFactory;
import org.forgerock.selfservice.core.config.ProcessInstanceConfig;
import org.forgerock.selfservice.core.snapshot.SnapshotTokenHandlerFactory;
import org.forgerock.services.context.Context;
import org.forgerock.util.promise.Promise;

import javax.inject.Inject;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Abstract request handler used to setup the self services.
 *
 * @since 13.0.0
 */
public abstract class AbstractSelfServiceRequestHandler extends AbstractRequestHandler {

    private final ProgressStageFactory stageFactory;
    private final SnapshotTokenHandlerFactory tokenHandlerFactory;
    private final ProcessStore localStore;

    private final ConcurrentMap<String, RequestHandler> serviceCache;

    @Inject
    public AbstractSelfServiceRequestHandler(ProgressStageFactory stageFactory,
            SnapshotTokenHandlerFactory tokenHandlerFactory, ProcessStore localStore) {
        serviceCache = new ConcurrentHashMap<>();

        this.stageFactory = stageFactory;
        this.tokenHandlerFactory = tokenHandlerFactory;
        this.localStore = localStore;
    }

    @Override
    public final Promise<ResourceResponse, ResourceException> handleRead(Context context, ReadRequest request) {
        return getService(context).handleRead(context, request);
    }

    @Override
    public final Promise<ActionResponse, ResourceException> handleAction(Context context, ActionRequest request) {
        return getService(context).handleAction(context, request);
    }

    private RequestHandler getService(Context context) {
        String realm = RealmContext.getRealm(context);
        RequestHandler result = serviceCache.get(realm);

        if (result == null) {
            result = createNewService(context, realm);
            RequestHandler old = serviceCache.putIfAbsent(realm, result);

            if (old != null) {
                result = old;
            }
        }

        return result;
    }

    private RequestHandler createNewService(Context context, String realm) {
        ProcessInstanceConfig config = getServiceConfig(context, realm);
        return new AnonymousProcessService(config, stageFactory, tokenHandlerFactory, localStore);
    }

    /**
     * Provides the CUSS configuration for the appropriate flow.
     *
     * @param context
     *         CREST context
     * @param realm
     *         the current realm
     *
     * @return service configuration
     */
    protected abstract ProcessInstanceConfig getServiceConfig(Context context, String realm);

}
