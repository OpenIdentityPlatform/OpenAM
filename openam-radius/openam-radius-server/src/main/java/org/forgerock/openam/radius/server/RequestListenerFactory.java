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
/**
 *
 */
package org.forgerock.openam.radius.server;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Named;

import org.forgerock.guava.common.eventbus.EventBus;
import org.forgerock.openam.radius.server.config.RadiusServerConstants;
import org.forgerock.openam.radius.server.config.RadiusServiceConfig;
import org.forgerock.openam.radius.server.config.ThreadPoolConfig;
import org.forgerock.util.thread.ExecutorServiceFactory;

import com.sun.identity.shared.debug.Debug;

/**
 * A class that may be used to obtain created <code>RadiusRequestListener</code> objects.
 */
public class RequestListenerFactory {

    private static final Debug logger = Debug.getInstance(RadiusServerConstants.RADIUS_SERVER_LOGGER);

    /**
     * The executor service factory that should be used to create the ExecutorService used by the created.
     * <code>RadiusRequestListener</code>s
     */
    private final ExecutorServiceFactory executorServiceFactory;

    /**
     * The event bus to be used by the Request Listener.
     */
    private final EventBus eventBus;

    /**
     * A factory that a <code>RadiusRequestHandler</code> may use to create <code>AccessRequestHandler</code> instances.
     */
    private AccessRequestHandlerFactory accessRequestHandlerFactory;

    /**
     * Constructor.
     *
     * @param serviceFactory - a factory from which a ThreadPoolExecutor may be obtained.
     * @param eventBus is used to publish or register for notifications of RADIUS server events.
     * @param accessRequestHandlerFactory may be used to obtain access request handlers.
     */
    @Inject
    public RequestListenerFactory(ExecutorServiceFactory serviceFactory,
            @Named("RadiusEventBus") EventBus eventBus,
            AccessRequestHandlerFactory accessRequestHandlerFactory) {
        this.executorServiceFactory = serviceFactory;
        this.eventBus = eventBus;
        this.accessRequestHandlerFactory = accessRequestHandlerFactory;
    }

    /**
     * Factory method to obtain a new RadiusRequestListener.
     *
     * @param serviceConfig - the configuration of the RADIUS service.
     * @return a <code>RadiusRquestListener</code>
     * @throws RadiusLifecycleException - if a RquestListener can not be created.
     */
    public RadiusRequestListener getRadiusRequestListener(RadiusServiceConfig serviceConfig)
            throws RadiusLifecycleException {
        final ThreadPoolConfig poolConfig = serviceConfig.getThreadPoolConfig();
        final int coreSize = poolConfig.getCoreThreads();
        final int maxSize = poolConfig.getMaxThreads();
        final int idleTimeout = poolConfig.getKeepAliveSeconds();

        final ArrayBlockingQueue<Runnable> queue = new ArrayBlockingQueue<Runnable>(poolConfig.getQueueSize());
        final ExecutorService executorService = executorServiceFactory.createThreadPool(coreSize, maxSize, idleTimeout,
                TimeUnit.SECONDS, queue);
        return new RadiusRequestListener(serviceConfig, executorService, eventBus, accessRequestHandlerFactory);
    }


}
