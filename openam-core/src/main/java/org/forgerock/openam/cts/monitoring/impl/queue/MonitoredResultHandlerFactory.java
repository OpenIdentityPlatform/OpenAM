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
package org.forgerock.openam.cts.monitoring.impl.queue;

import org.forgerock.openam.cts.CTSOperation;
import org.forgerock.openam.cts.api.tokens.Token;
import org.forgerock.openam.cts.exceptions.CoreTokenException;
import org.forgerock.openam.sm.datalayer.api.query.PartialToken;
import org.forgerock.openam.cts.impl.queue.AsyncResultHandlerFactory;
import org.forgerock.openam.sm.datalayer.api.ResultHandler;
import org.forgerock.openam.cts.impl.queue.ResultHandlerFactory;
import org.forgerock.openam.cts.monitoring.CTSOperationsMonitoringStore;

import javax.inject.Inject;
import java.util.Collection;

/**
 * Implementation enables monitoring of each operation that is processed by the asynchronous task
 * processors in a suitably generic way.
 */
public class MonitoredResultHandlerFactory implements ResultHandlerFactory {
    private final AsyncResultHandlerFactory factory;
    private final CTSOperationsMonitoringStore store;

    /**
     * @param factory Non null implementation to delegate to.
     * @param store Non null store to report operations to.
     */
    @Inject
    public MonitoredResultHandlerFactory(AsyncResultHandlerFactory factory, CTSOperationsMonitoringStore store) {
        this.factory = factory;
        this.store = store;
    }

    /**
     * @return A monitoring enabled handler wrapping the delegated implementation.
     */
    @Override
    public ResultHandler<Token, CoreTokenException> getCreateHandler() {
        return new TokenMonitoringResultHandler(factory.getCreateHandler(), store, CTSOperation.CREATE);
    }

    /**
     * @return A monitoring enabled handler wrapping the delegated implementation.
     */
    @Override
    public ResultHandler<Token, CoreTokenException> getReadHandler() {
        return new TokenMonitoringResultHandler(factory.getReadHandler(), store, CTSOperation.READ);
    }

    /**
     * @return A monitoring enabled handler wrapping the delegated implementation.
     */
    @Override
    public ResultHandler<Token, CoreTokenException> getUpdateHandler() {
        return new TokenMonitoringResultHandler(factory.getUpdateHandler(), store, CTSOperation.UPDATE);
    }

    /**
     * @return A monitoring enabled handler wrapping the delegated implementation.
     */
    @Override
    public ResultHandler<String, CoreTokenException> getDeleteHandler() {
        return new DefaultMonitoringResultHandler<String, CoreTokenException>(factory.getDeleteHandler(), store, CTSOperation.DELETE);
    }

    /**
     * @return A monitoring enabled handler wrapping the delegated implementation.
     */
    @Override
    public ResultHandler<Collection<Token>, CoreTokenException> getQueryHandler() {
        return new DefaultMonitoringResultHandler<Collection<Token>, CoreTokenException>(
                factory.getQueryHandler(), store, CTSOperation.LIST);
    }

    /**
     * @return A monitoring enabled handler wrapping the delegated implementation.
     */
    @Override
    public ResultHandler<Collection<PartialToken>, CoreTokenException> getPartialQueryHandler() {
        return new DefaultMonitoringResultHandler<Collection<PartialToken>, CoreTokenException>(
                factory.getPartialQueryHandler(), store, CTSOperation.LIST);
    }

    /**
     * @return A non monitored handler as this is a composite operation.
     */
    @Override
    public ResultHandler<Collection<PartialToken>, CoreTokenException> getDeleteOnQueryHandler() {
        return new DefaultMonitoringResultHandler<Collection<PartialToken>, CoreTokenException>(
                factory.getDeleteOnQueryHandler(), store, CTSOperation.LIST);
    }
}
