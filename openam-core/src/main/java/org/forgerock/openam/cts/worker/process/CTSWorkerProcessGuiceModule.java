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
 * Copyright 2016 ForgeRock AS.
 * Portions copyright 2025 3A Systems LLC.
 */

package org.forgerock.openam.cts.worker.process;

import static org.forgerock.openam.session.SessionEventType.IDLE_TIMEOUT;
import static org.forgerock.openam.session.SessionEventType.MAX_TIMEOUT;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import org.forgerock.openam.cts.impl.query.worker.CTSWorkerConstants;
import org.forgerock.openam.cts.impl.queue.TaskDispatcher;

/**
 * Guice Module for configuring bindings for the CTS Work Process classes.
 */
public class CTSWorkerProcessGuiceModule extends AbstractModule {

    @Override
    protected void configure() {
        install(new FactoryModuleBuilder()
                .implement(SessionExpiryBatchHandler.StateChangeResultHandler.class,
                        SessionExpiryBatchHandler.StateChangeResultHandler.class)
                .build(SessionExpiryBatchHandler.StateChangeResultHandlerFactory.class));
    }

    @Provides
    @Named(CTSWorkerConstants.MAX_SESSION_TIME_EXPIRED)
    @Inject
    SessionExpiryBatchHandler getMaxSessionExpiryBatchHandler(TaskDispatcher queue,
            SessionExpiryBatchHandler.StateChangeResultHandlerFactory stateChangeResultHandlerFactory) {
        return new SessionExpiryBatchHandler(queue, MAX_TIMEOUT, stateChangeResultHandlerFactory);
    }

    @Provides
    @Named(CTSWorkerConstants.SESSION_IDLE_TIME_EXPIRED)
    @Inject
    SessionExpiryBatchHandler getIdleTimeoutExpiryBatchHandler(TaskDispatcher queue,
            SessionExpiryBatchHandler.StateChangeResultHandlerFactory stateChangeResultHandlerFactory) {
        return new SessionExpiryBatchHandler(queue, IDLE_TIMEOUT, stateChangeResultHandlerFactory);
    }
}
