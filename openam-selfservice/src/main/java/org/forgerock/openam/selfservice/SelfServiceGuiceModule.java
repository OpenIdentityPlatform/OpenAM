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

import static org.forgerock.json.resource.Resources.newInternalConnectionFactory;

import com.google.inject.PrivateModule;
import com.google.inject.Provides;
import com.iplanet.sso.SSOToken;
import org.forgerock.guice.core.GuiceModule;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.Router;
import org.forgerock.openam.rest.ElevatedConnectionFactoryWrapper;
import org.forgerock.selfservice.core.ProcessStore;
import org.forgerock.selfservice.core.ProgressStageFactory;
import org.forgerock.selfservice.core.snapshot.SnapshotTokenHandlerFactory;
import org.forgerock.selfservice.stages.email.VerifyEmailAccountConfig;
import org.forgerock.selfservice.stages.email.VerifyEmailAccountStage;
import org.forgerock.selfservice.stages.email.VerifyUserIdConfig;
import org.forgerock.selfservice.stages.email.VerifyUserIdStage;
import org.forgerock.selfservice.stages.registration.UserRegistrationConfig;
import org.forgerock.selfservice.stages.registration.UserRegistrationStage;
import org.forgerock.selfservice.stages.reset.ResetStage;
import org.forgerock.selfservice.stages.reset.ResetStageConfig;
import org.forgerock.selfservice.stages.user.UserDetailsConfig;
import org.forgerock.selfservice.stages.user.UserDetailsStage;

import javax.inject.Named;
import javax.inject.Singleton;
import java.security.PrivilegedAction;

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
    @Named("SelfServiceConnectionFactory")
    ConnectionFactory getConnectionFactory(@Named("InternalCrestRouter") Router router,
            PrivilegedAction<SSOToken> ssoTokenPrivilegedAction) {
        ConnectionFactory internalConnectionFactory = newInternalConnectionFactory(router);
        return new ElevatedConnectionFactoryWrapper(internalConnectionFactory, ssoTokenPrivilegedAction);
    }

    @Provides
    @Singleton
    ProgressStageFactory getProgressStageFactory(@Named("SelfServiceConnectionFactory") ConnectionFactory connectionFactory) {
        ProgressStageFactoryImpl stageFactory = new ProgressStageFactoryImpl();
        stageFactory.safePut(VerifyUserIdConfig.class, new VerifyUserIdStage(connectionFactory));
        stageFactory.safePut(ResetStageConfig.class, new ResetStage(connectionFactory));
        stageFactory.safePut(VerifyEmailAccountConfig.class, new VerifyEmailAccountStage(connectionFactory));
        stageFactory.safePut(UserRegistrationConfig.class, new UserRegistrationStage(connectionFactory));
        stageFactory.safePut(UserDetailsConfig.class, new UserDetailsStage(connectionFactory));
        return stageFactory;
    }

}
