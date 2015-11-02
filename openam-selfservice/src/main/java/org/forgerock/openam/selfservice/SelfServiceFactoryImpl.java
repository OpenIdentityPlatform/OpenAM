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

import org.forgerock.json.resource.RequestHandler;
import org.forgerock.openam.selfservice.config.BasicStageConfigVisitor;
import org.forgerock.selfservice.core.AnonymousProcessService;
import org.forgerock.selfservice.core.ProcessStore;
import org.forgerock.selfservice.core.config.ProcessInstanceConfig;
import org.forgerock.selfservice.core.snapshot.SnapshotTokenHandlerFactory;
import org.forgerock.selfservice.stages.CommonConfigVisitor;

import javax.inject.Inject;

/**
 * Creates new anonymous process services based of the passed service config.
 *
 * @since 13.0.0
 */
class SelfServiceFactoryImpl implements SelfServiceFactory {

    private final SnapshotTokenHandlerFactory tokenHandlerFactory;
    private final ProcessStore processStore;
    private final BasicStageConfigVisitor stageConfigVisitor;

    /**
     * Constructs the default forgotten password service provider.
     *
     * @param tokenHandlerFactory
     *         snapshot token handler factory
     * @param processStore
     *         local process store
     * @param stageConfigVisitor
     *         stage config visitor
     */
    @Inject
    SelfServiceFactoryImpl(SnapshotTokenHandlerFactory tokenHandlerFactory,
            ProcessStore processStore, BasicStageConfigVisitor stageConfigVisitor) {
        this.tokenHandlerFactory = tokenHandlerFactory;
        this.processStore = processStore;
        this.stageConfigVisitor = stageConfigVisitor;
    }

    @Override
    public RequestHandler getService(ProcessInstanceConfig<CommonConfigVisitor> serviceConfig) {
        return new AnonymousProcessService<>(serviceConfig, stageConfigVisitor, tokenHandlerFactory, processStore);
    }

}
