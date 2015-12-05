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
import org.forgerock.openam.shared.security.crypto.KeyPairProvider;
import org.forgerock.openam.shared.security.crypto.KeyPairProviderFactory;
import org.forgerock.selfservice.core.AnonymousProcessService;
import org.forgerock.selfservice.core.ProcessStore;
import org.forgerock.selfservice.core.ProgressStageProvider;
import org.forgerock.selfservice.core.config.ProcessInstanceConfig;
import org.forgerock.selfservice.core.snapshot.SnapshotTokenHandlerFactory;

import javax.inject.Inject;

/**
 * Creates new anonymous process services based of the passed service config.
 *
 * @since 13.0.0
 */
class SelfServiceFactoryImpl implements SelfServiceFactory {

    private final ProgressStageProvider stageProvider;
    private final KeyPairInjector<SnapshotTokenHandlerFactory> keyPairInjector;
    private final KeyPairProviderFactory keyPairProviderFactory;
    private final ProcessStore processStore;

    /**
     * Constructs the default forgotten password service provider.
     *
     * @param stageProvider
     *         progress stage provider
     * @param keyPairInjector
     *         key pair provider injector
     * @param keyPairProviderFactory
     *         key pair provider factory
     * @param processStore
     *         local process store
     */
    @Inject
    SelfServiceFactoryImpl(ProgressStageProvider stageProvider,
            KeyPairInjector<SnapshotTokenHandlerFactory> keyPairInjector,
            KeyPairProviderFactory keyPairProviderFactory, ProcessStore processStore) {
        this.stageProvider = stageProvider;
        this.keyPairInjector = keyPairInjector;
        this.keyPairProviderFactory = keyPairProviderFactory;
        this.processStore = processStore;
    }

    @Override
    public RequestHandler getService(String realm, ProcessInstanceConfig serviceConfig) {
        KeyPairProvider keyPairProvider = keyPairProviderFactory.getProvider(realm);
        SnapshotTokenHandlerFactory tokenHandlerFactory = keyPairInjector.getInjectedWith(keyPairProvider);
        ClassLoader classLoader = getClass().getClassLoader();
        return new AnonymousProcessService(serviceConfig, stageProvider,
                tokenHandlerFactory, processStore, classLoader);
    }

}
