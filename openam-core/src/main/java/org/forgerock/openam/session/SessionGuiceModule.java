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
package org.forgerock.openam.session;

import java.util.Arrays;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import org.forgerock.guice.core.GuiceModule;
import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.openam.session.service.access.persistence.InternalSessionPersistenceStore;
import org.forgerock.openam.session.service.access.persistence.InternalSessionStore;
import org.forgerock.openam.session.service.access.persistence.InternalSessionStoreChain;
import org.forgerock.openam.session.service.access.persistence.SessionPersistenceManagerStep;
import org.forgerock.openam.session.service.access.persistence.TimeOutSessionFilterStep;
import org.forgerock.openam.session.service.access.persistence.caching.InMemoryInternalSessionCacheStep;
import org.forgerock.openam.sso.providers.stateless.StatelessSSOProvider;
import org.forgerock.openam.utils.Config;

import com.google.inject.Key;
import com.google.inject.PrivateModule;
import com.google.inject.Provider;
import com.google.inject.Provides;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;
import com.iplanet.dpro.session.operations.ServerSessionOperationStrategy;
import com.iplanet.dpro.session.operations.SessionOperationStrategy;
import com.iplanet.dpro.session.service.InternalSessionEventBroker;
import com.iplanet.dpro.session.service.InternalSessionListener;
import com.iplanet.dpro.session.service.SessionAuditor;
import com.iplanet.dpro.session.service.SessionLogging;
import com.iplanet.dpro.session.service.SessionNotificationPublisher;
import com.iplanet.dpro.session.service.SessionNotificationSender;
import com.iplanet.dpro.session.service.SessionServerConfig;
import com.iplanet.dpro.session.service.SessionService;
import com.iplanet.dpro.session.service.SessionTimeoutHandlerExecutor;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.stats.Stats;

/**
 * The Guice bindings for the Session and Session related code.
 */
@GuiceModule
public class SessionGuiceModule extends PrivateModule {
    @Override
    protected void configure() {
        bind(Debug.class)
                .annotatedWith(Names.named(SessionConstants.SESSION_DEBUG))
                .toInstance(Debug.getInstance(SessionConstants.SESSION_DEBUG));
        bind(SessionCache.class).toInstance(SessionCache.getInstance());

        bind(SessionPollerPool.class).toInstance(SessionPollerPool.getInstance());

        bind(SessionOperationStrategy.class).to(ServerSessionOperationStrategy.class);

        // TODO: Investigate whether or not this lazy-loading "Config<SessionService>" wrapper is still needed
        bind(new TypeLiteral<Config<SessionService>>() {
        }).toInstance(new Config<SessionService>() {
            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public SessionService get() {
                return InjectorHolder.getInstance(SessionService.class);
            }
        });

        bind(InternalSessionListener.class).to(InternalSessionEventBroker.class).in(Singleton.class);

        /*
         * Must use a provider to ensure initialisation happens after SystemProperties have been set.
         */
        bind(SessionCookies.class).toProvider(new Provider<SessionCookies>() {
            @Override
            public SessionCookies get() {
                return SessionCookies.getInstance();
            }
        });
        /*
         * Must use a provider to ensure initialisation happens after SystemProperties have been set.
         */
        bind(SessionURL.class).toProvider(new Provider<SessionURL>() {
            @Override
            public SessionURL get() {
                return SessionURL.getInstance();
            }
        });
        bind(SessionServiceURLService.class).toInstance(SessionServiceURLService.getInstance());

        bind(StatelessSSOProvider.class);

        /*
         * Must use a provider to ensure initialisation happens after SystemProperties have been set.
         */
        bind(Key.get(Stats.class, Names.named(SessionConstants.STATS_MASTER_TABLE))).toProvider(new Provider<Stats>() {
            @Override
            public Stats get() {
                return Stats.getInstance(SessionConstants.STATS_MASTER_TABLE);
            }
        });

        bind(SessionServerConfig.class);

        expose(Debug.class).annotatedWith(Names.named(SessionConstants.SESSION_DEBUG));
        expose(InternalSessionStore.class);
        expose(InternalSessionEventBroker.class);
        expose(new TypeLiteral<Config<SessionService>>() {});
        expose(SessionServerConfig.class);
        expose(SessionOperationStrategy.class);
        expose(SessionCache.class);
        expose(SessionCookies.class);
        expose(SessionPollerPool.class);
        expose(SessionServiceURLService.class);
        expose(StatelessSSOProvider.class);
        expose(Stats.class).annotatedWith(Names.named(SessionConstants.STATS_MASTER_TABLE));
    }

    @Provides
    @Inject
    @Singleton
    InternalSessionStore getInternalSessionStore(TimeOutSessionFilterStep timeOutSessionFilterStep,
                                                 InMemoryInternalSessionCacheStep internalSessionCacheStep,
                                                 SessionPersistenceManagerStep sessionPersistenceManagerStep,
                                                 InternalSessionPersistenceStore internalSessionPersistenceStore) {
        return new InternalSessionStoreChain(
                Arrays.asList(timeOutSessionFilterStep, internalSessionCacheStep, sessionPersistenceManagerStep),
                internalSessionPersistenceStore);
    }

    @Provides @Inject @Singleton
    InternalSessionEventBroker getSessionEventBroker(
            final SessionLogging sessionLogging,
            final SessionAuditor sessionAuditor,
            final SessionNotificationSender sessionNotificationSender,
            final SessionNotificationPublisher sessionNotificationPublisher,
            final SessionTimeoutHandlerExecutor sessionTimeoutHandlerExecutor) {

        return new InternalSessionEventBroker(
                sessionLogging, sessionAuditor, sessionNotificationSender, sessionNotificationPublisher,
                sessionTimeoutHandlerExecutor);
    }

    @Provides
    @Named(com.iplanet.dpro.session.service.SessionConstants.PRIMARY_SERVER_URL) @Inject
    @Singleton
    String getPrimaryServerURL(SessionServerConfig serverConfig) {
        return serverConfig.getPrimaryServerURL().toString();
    }
}
