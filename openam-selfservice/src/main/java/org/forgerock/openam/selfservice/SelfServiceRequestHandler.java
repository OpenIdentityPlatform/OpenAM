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
import org.forgerock.json.resource.InternalServerErrorException;
import org.forgerock.json.resource.NotSupportedException;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.openam.rest.RealmContext;
import org.forgerock.openam.selfservice.config.SelfServiceConsoleConfig;
import org.forgerock.openam.selfservice.config.ServiceConfigProvider;
import org.forgerock.openam.selfservice.config.ServiceConfigProviderFactory;
import org.forgerock.openam.sm.config.ConsoleConfigBuilder;
import org.forgerock.openam.sm.config.ConsoleConfigHandler;
import org.forgerock.openam.sm.config.ConsoleConfigListener;
import org.forgerock.services.context.Context;
import org.forgerock.util.promise.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Abstract request handler used to setup the self services.
 *
 * @since 13.0.0
 */
final class SelfServiceRequestHandler<C extends SelfServiceConsoleConfig>
        extends AbstractRequestHandler implements ConsoleConfigListener {

    private static final Logger logger = LoggerFactory.getLogger(SelfServiceRequestHandler.class);

    private final Class<? extends ConsoleConfigBuilder<C>> consoleConfigBuilderType;
    private final ConsoleConfigHandler consoleConfigHandler;
    private final ServiceConfigProviderFactory providerFactory;
    private final SelfServiceFactory serviceFactory;

    private final Map<String, RequestHandler> serviceCache;

    /**
     * Constructs a new self service.
     *
     * @param consoleConfigBuilderType
     *         configuration extractor
     * @param consoleConfigHandler
     *         console configuration handler
     * @param providerFactory
     *         service provider factory
     */
    @Inject
    public SelfServiceRequestHandler(Class<? extends ConsoleConfigBuilder<C>> consoleConfigBuilderType,
            ConsoleConfigHandler consoleConfigHandler, ServiceConfigProviderFactory providerFactory,
            SelfServiceFactory serviceFactory) {

        serviceCache = new ConcurrentHashMap<>();
        this.consoleConfigBuilderType = consoleConfigBuilderType;
        this.consoleConfigHandler = consoleConfigHandler;
        this.providerFactory = providerFactory;
        this.serviceFactory = serviceFactory;

        consoleConfigHandler.registerListener(this, consoleConfigBuilderType);
    }

    @Override
    public Promise<ResourceResponse, ResourceException> handleRead(Context context, ReadRequest request) {
        try {
            return getService(context).handleRead(context, request);
        } catch (NotSupportedException nsE) {
            return nsE.asPromise();
        } catch (RuntimeException rE) {
            logger.error("Unable to handle read", rE);
            return new InternalServerErrorException("Unable to handle read", rE).asPromise();
        }
    }

    @Override
    public Promise<ActionResponse, ResourceException> handleAction(Context context, ActionRequest request) {
        try {
            return getService(context).handleAction(context, request);
        } catch (NotSupportedException nsE) {
            return nsE.asPromise();
        } catch (RuntimeException rE) {
            logger.error("Unable to handle action", rE);
            return new InternalServerErrorException("Unable to handle action", rE).asPromise();
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
        C consoleConfig = consoleConfigHandler.getConfig(realm, consoleConfigBuilderType);
        ServiceConfigProvider<C> serviceConfigProvider = providerFactory.getProvider(consoleConfig);

        if (!serviceConfigProvider.isServiceEnabled(consoleConfig)) {
            throw new NotSupportedException("Service not configured");
        }

        return serviceFactory.getService(realm, serviceConfigProvider.getServiceConfig(consoleConfig, context, realm));
    }

    @Override
    public final void configUpdate(String source, String realm) {
        synchronized (serviceCache) {
            serviceCache.remove(realm);
        }
    }

}
