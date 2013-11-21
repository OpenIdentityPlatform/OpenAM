/**
 * Copyright 2013 ForgeRock AS.
 *
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
 */
package org.forgerock.openam.utils;

import com.sun.identity.common.ShutdownListener;
import org.forgerock.openam.core.guice.CoreGuiceModule;

import javax.inject.Inject;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Responsible for generating ExecutorService instances which are automatically
 * wired up to shutdown when the ShutdownListener event triggers.
 *
 * This factory simplifies the creation of ExecutorServices which could overlook
 * the important step of registering with the ShutdownManager. Failure to do so
 * will prevent the server from shutting down.
 *
 * @author robert.wapshott@forgerock.com
 */
public class ExecutorServiceFactory {
    private final CoreGuiceModule.ShutdownManagerWrapper shutdownManager;

    /**
     * Create an instance of the factory.
     *
     * @param shutdownManager Required to ensure each ExecutorService will be shutdown.
     */
    @Inject
    public ExecutorServiceFactory(CoreGuiceModule.ShutdownManagerWrapper shutdownManager) {
        this.shutdownManager = shutdownManager;
    }

    /**
     * Generates a ScheduledExecutorService which has been pre-registered with the
     * ShutdownManager.
     *
     * @see Executors#newScheduledThreadPool(int)
     *
     * @param poolSize The size of the ScheduledExecutorService thread pool.
     *
     * @return A non null ScheduledExecutorService
     */
    public ScheduledExecutorService createScheduledService(int poolSize) {
        final ScheduledExecutorService service = Executors.newScheduledThreadPool(poolSize);
        registerShutdown(service);
        return service;
    }

    /**
     * Generates a Fixed Thread Pool ExecutorService which has been pre-registered with the
     * ShutdownManager.
     *
     * @see Executors#newFixedThreadPool(int)
     *
     * @param poolSize The size of the thread pool to initalise.
     *
     * @return A non null ExecutorService.
     */
    public ExecutorService createThreadPool(int poolSize) {
        ExecutorService service = Executors.newFixedThreadPool(poolSize);
        registerShutdown(service);
        return service;
    }

    /**
     * Registers a listener to trigger shutdown of the ExecutorService.
     * @param service
     */
    private void registerShutdown(final ExecutorService service) {
        shutdownManager.addShutdownListener(new ShutdownListener() {
            public void shutdown() {
                service.shutdown();
            }
        });
    }
}
