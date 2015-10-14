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
import com.google.inject.TypeLiteral;
import com.google.inject.name.Named;
import com.iplanet.sso.SSOToken;
import org.forgerock.guice.core.GuiceModule;
import org.forgerock.http.Client;
import org.forgerock.http.HttpApplicationException;
import org.forgerock.http.handler.HttpClientHandler;
import org.forgerock.json.resource.ConnectionFactory;
import org.forgerock.json.resource.Router;
import org.forgerock.openam.rest.ElevatedConnectionFactoryWrapper;
import org.forgerock.openam.selfservice.config.BasicStageConfigVisitor;
import org.forgerock.openam.selfservice.config.ConsoleConfigExtractor;
import org.forgerock.openam.selfservice.config.ConsoleConfigHandler;
import org.forgerock.openam.selfservice.config.ConsoleConfigHandlerImpl;
import org.forgerock.openam.selfservice.config.ForgottenPasswordConsoleConfig;
import org.forgerock.openam.selfservice.config.ForgottenPasswordExtractor;
import org.forgerock.openam.selfservice.config.ServiceProviderFactory;
import org.forgerock.openam.selfservice.config.ServiceProviderFactoryImpl;
import org.forgerock.openam.selfservice.config.UserRegistrationConsoleConfig;
import org.forgerock.openam.selfservice.config.UserRegistrationExtractor;
import org.forgerock.selfservice.core.ProcessStore;
import org.forgerock.selfservice.core.snapshot.SnapshotTokenHandlerFactory;
import org.forgerock.selfservice.stages.CommonConfigVisitor;
import org.forgerock.selfservice.stages.SelfService;

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
        bind(ConsoleConfigHandler.class).to(ConsoleConfigHandlerImpl.class);
        bind(SnapshotTokenHandlerFactory.class).to(SnapshotTokenHandlerFactoryImpl.class);
        bind(ServiceProviderFactory.class).to(ServiceProviderFactoryImpl.class);
        bind(CommonConfigVisitor.class).to(BasicStageConfigVisitor.class);

        bind(new TypeLiteral<ConsoleConfigExtractor<UserRegistrationConsoleConfig>>() {})
                .to(UserRegistrationExtractor.class);
        bind(new TypeLiteral<ConsoleConfigExtractor<ForgottenPasswordConsoleConfig>>() {})
                .to(ForgottenPasswordExtractor.class);

        try {
            bind(Client.class)
                    .annotatedWith(SelfService.class)
                    .toInstance(new Client(new HttpClientHandler()));
        } catch (HttpApplicationException haE) {
            throw new HttpClientCreationException("Unable to create http client", haE);
        }

        // Registration CREST services
        expose(new TypeLiteral<SelfServiceRequestHandler<UserRegistrationConsoleConfig>>() {});
        expose(new TypeLiteral<SelfServiceRequestHandler<ForgottenPasswordConsoleConfig>>() {});

        // These have been exposed so they can be accessible to
        // reflection based instantiated service provider instances.
        expose(ProcessStore.class);
        expose(SnapshotTokenHandlerFactory.class);
        expose(CommonConfigVisitor.class);
    }

    @Provides
    @Singleton
    SelfServiceRequestHandler<UserRegistrationConsoleConfig> getUserRegistrationService(
            ConsoleConfigHandler configHandler,
            ConsoleConfigExtractor<UserRegistrationConsoleConfig> configExtractor,
            ServiceProviderFactory configProviderFactory) {

        return new SelfServiceRequestHandler<>(configHandler, configExtractor, configProviderFactory);
    }

    @Provides
    @Singleton
    SelfServiceRequestHandler<ForgottenPasswordConsoleConfig> getForgottenPasswordService(
            ConsoleConfigHandler configHandler,
            ConsoleConfigExtractor<ForgottenPasswordConsoleConfig> configExtractor,
            ServiceProviderFactory configProviderFactory) {

        return new SelfServiceRequestHandler<>(configHandler, configExtractor, configProviderFactory);
    }

    @Provides
    @Singleton
    @SelfService
    ConnectionFactory getConnectionFactory(@Named("InternalCrestRouter") Router router,
            PrivilegedAction<SSOToken> ssoTokenPrivilegedAction) {
        ConnectionFactory internalConnectionFactory = newInternalConnectionFactory(router);
        return new ElevatedConnectionFactoryWrapper(internalConnectionFactory, ssoTokenPrivilegedAction);
    }

}
