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
import org.forgerock.json.resource.NotSupportedException;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.openam.rest.RealmContext;
import org.forgerock.openam.selfservice.config.ConsoleConfig;
import org.forgerock.openam.selfservice.config.ConsoleConfigChangeListener;
import org.forgerock.openam.selfservice.config.ConsoleConfigHandler;
import org.forgerock.selfservice.core.AnonymousProcessService;
import org.forgerock.selfservice.core.ProcessStore;
import org.forgerock.selfservice.core.ProgressStageFactory;
import org.forgerock.selfservice.core.config.ProcessInstanceConfig;
import org.forgerock.selfservice.core.snapshot.SnapshotTokenHandlerFactory;
import org.forgerock.services.context.Context;
import org.forgerock.util.promise.Promise;

import javax.inject.Inject;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Abstract request handler used to setup the self services.
 *
 * @since 13.0.0
 */
abstract class AbstractSelfServiceRequestHandler
        extends AbstractRequestHandler implements ConsoleConfigChangeListener {

    private final ProgressStageFactory stageFactory;
    private final SnapshotTokenHandlerFactory tokenHandlerFactory;
    private final ProcessStore localStore;

    private final Map<String, RequestHandler> serviceCache;
    private final ConsoleConfigHandler consoleConfigHandler;

    @Inject
    public AbstractSelfServiceRequestHandler(ProgressStageFactory stageFactory,
            SnapshotTokenHandlerFactory tokenHandlerFactory, ProcessStore localStore,
            ConsoleConfigHandler consoleConfigHandler) {
        serviceCache = new ConcurrentHashMap<>();

        this.stageFactory = stageFactory;
        this.tokenHandlerFactory = tokenHandlerFactory;
        this.localStore = localStore;

        this.consoleConfigHandler = consoleConfigHandler;
        consoleConfigHandler.registerListener(this);
    }

    @Override
    public final Promise<ResourceResponse, ResourceException> handleRead(Context context, ReadRequest request) {
        try {
            return getService(context).handleRead(context, request);
        } catch (NotSupportedException nsE) {
            return nsE.asPromise();
        }
    }

    @Override
    public final Promise<ActionResponse, ResourceException> handleAction(Context context, ActionRequest request) {
        try {
            return getService(context).handleAction(context, request);
        } catch (NotSupportedException nsE) {
            return nsE.asPromise();
        }
    }

    private RequestHandler getService(Context context) throws NotSupportedException {
        String realm = RealmContext.getRealm(context);
        RequestHandler service = serviceCache.get(realm);

        if (service == null) {
            synchronized (serviceCache) {
                service = serviceCache.get(realm);

                if (service == null) {
                    service = createNewService(context, realm);
                    serviceCache.put(realm, service);
                }
            }
        }

        return service;
    }

    private RequestHandler createNewService(Context context, String realm) throws NotSupportedException {
        ConsoleConfig config = consoleConfigHandler.getConfig(realm);

        if (!isServiceEnabled(config)) {
            throw new NotSupportedException("Service not configured");
        }

        ProcessInstanceConfig serviceConfig = getServiceConfig(config, context, realm);
        return new AnonymousProcessService(serviceConfig, stageFactory, tokenHandlerFactory, localStore);
    }

    @Override
    public final void configUpdate(String realm) {
        synchronized (serviceCache) {
            serviceCache.remove(realm);
        }
    }

    /**
     * Determines whether the specific service is enabled.
     *
     * @param config
     *         the console config
     *
     * @return whether the service is enabled
     */
    protected abstract boolean isServiceEnabled(ConsoleConfig config);

    /**
     * Provides the self service configuration for the appropriate flow.
     *
     * @param config
     *         the console config
     * @param context
     *         CREST context
     * @param realm
     *         the current realm
     *
     * @return service configuration
     */
    protected abstract ProcessInstanceConfig getServiceConfig(ConsoleConfig config, Context context, String realm);

}
