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

import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;

import com.google.inject.PrivateModule;
import com.google.inject.Provides;
import org.forgerock.guice.core.GuiceModule;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.selfservice.core.ProcessContext;
import org.forgerock.selfservice.core.ProcessStore;
import org.forgerock.selfservice.core.ProgressStage;
import org.forgerock.selfservice.core.ProgressStageFactory;
import org.forgerock.selfservice.core.StageResponse;
import org.forgerock.selfservice.core.config.StageConfig;
import org.forgerock.selfservice.core.snapshot.SnapshotTokenConfig;
import org.forgerock.selfservice.core.snapshot.SnapshotTokenHandler;
import org.forgerock.selfservice.core.snapshot.SnapshotTokenHandlerFactory;
import org.forgerock.selfservice.stages.utils.RequirementsBuilder;

import javax.inject.Singleton;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Guice module to bind the self service features together.
 *
 * @since 13.0.0
 */
@GuiceModule
public final class SelfServiceGuiceModule extends PrivateModule {

    @Override
    protected void configure() {
        bind(ProcessStore.class).to(CTSProcessStoreImpl.class);
        bind(SnapshotTokenHandlerFactory.class).to(SnapshotTokenHandlerFactoryImpl.class);

        // Registration CREST services
        bind(ForgottenPasswordRequestHandler.class);
        bind(UserRegistrationRequestHandler.class);

        expose(ForgottenPasswordRequestHandler.class);
        expose(UserRegistrationRequestHandler.class);
    }

    @Provides
    @Singleton
    Map<SnapshotTokenConfig, SnapshotTokenHandler> getTokenHandlers() {
        Map<SnapshotTokenConfig, SnapshotTokenHandler> tokenHandlers = new HashMap<>();
        tokenHandlers.put(INTERIM_TYPE, new InterimSnapshotTokenHandler());
        return tokenHandlers;
    }

    @Provides
    @Singleton
    ProgressStageFactory getProgressStageFactory() {
        ProgressStageFactoryImpl stageFactory = new ProgressStageFactoryImpl();
        stageFactory.safePut(InterimConfig.class, new InterimStage());
        return stageFactory;
    }

    static final SnapshotTokenConfig INTERIM_TYPE = new SnapshotTokenConfig() {
        @Override
        public String getType() {
            return "interimType";
        }
    };

    private static final class InterimSnapshotTokenHandler implements SnapshotTokenHandler {

        @Override
        public String generate(JsonValue jsonValue) throws ResourceException {
            return UUID.randomUUID().toString();
        }

        @Override
        public void validate(String snapshotToken) throws ResourceException {
            // Do nothing.
        }

        @Override
        public JsonValue validateAndExtractState(String s) throws ResourceException {
            return json(object());
        }
    }

    static final class InterimConfig implements StageConfig {

        @Override
        public String getName() {
            return "Interim stage";
        }

    }

    private static final class InterimStage implements ProgressStage<InterimConfig> {

        @Override
        public JsonValue gatherInitialRequirements(ProcessContext context,
                                                   InterimConfig config) throws ResourceException {
            return RequirementsBuilder
                    .newEmptyRequirements();
        }

        @Override
        public StageResponse advance(ProcessContext context, InterimConfig config) throws ResourceException {
            return StageResponse
                    .newBuilder()
                    .build();
        }

    }

}
