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
 * Copyright 2013 ForgeRock Inc.
 */

package org.forgerock.openam.core.guice;

import com.google.inject.AbstractModule;
import com.google.inject.Provider;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;
import com.iplanet.dpro.session.service.SessionConstants;
import com.iplanet.dpro.session.service.SessionService;
import com.iplanet.services.ldap.DSConfigMgr;
import com.iplanet.services.ldap.LDAPServiceException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.common.ShutdownListener;
import com.sun.identity.common.ShutdownManager;
import com.sun.identity.common.configuration.ConfigurationObserver;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.DNMapper;
import com.sun.identity.sm.SMSEntry;
import com.sun.identity.sm.ServiceManagementDAO;
import com.sun.identity.sm.ServiceManagementDAOWrapper;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.openam.cts.CTSPersistentStore;
import org.forgerock.openam.cts.CoreTokenConfig;
import org.forgerock.openam.cts.ExternalTokenConfig;
import org.forgerock.openam.cts.adapters.OAuthAdapter;
import org.forgerock.openam.cts.adapters.TokenAdapter;
import org.forgerock.openam.cts.api.CoreTokenConstants;
import org.forgerock.openam.cts.impl.CTSConnectionFactory;
import org.forgerock.openam.cts.impl.LDAPConfig;
import org.forgerock.openam.entitlement.indextree.IndexChangeHandler;
import org.forgerock.openam.entitlement.indextree.IndexChangeManager;
import org.forgerock.openam.entitlement.indextree.IndexChangeManagerImpl;
import org.forgerock.openam.entitlement.indextree.IndexChangeMonitor;
import org.forgerock.openam.entitlement.indextree.IndexChangeMonitorImpl;
import org.forgerock.openam.entitlement.indextree.IndexTreeService;
import org.forgerock.openam.entitlement.indextree.IndexTreeServiceImpl;
import org.forgerock.openam.entitlement.indextree.events.IndexChangeObservable;
import org.forgerock.openam.guice.AMGuiceModule;
import org.forgerock.openam.guice.InjectorHolder;
import org.forgerock.openam.sm.DataLayerConnectionFactory;
import org.forgerock.openam.utils.ExecutorServiceFactory;
import org.forgerock.opendj.ldap.ConnectionFactory;
import org.forgerock.opendj.ldap.SearchResultHandler;

import javax.inject.Singleton;
import java.security.PrivilegedAction;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Guice Module for configuring bindings for the OpenAM Core classes.
 *
 * @author apforrest
 * @author robert.wapshott@forgerock.com
 */
@AMGuiceModule
public class CoreGuiceModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(new AdminTokenType()).toProvider(new AdminTokenProvider()).in(Singleton.class);
        bind(ServiceManagementDAO.class).to(ServiceManagementDAOWrapper.class).in(Singleton.class);
        bind(DNWrapper.class).in(Singleton.class);
        bind(IndexChangeObservable.class).in(Singleton.class);
        bind(ShutdownManagerWrapper.class).in(Singleton.class);
        bind(SearchResultHandler.class).to(IndexChangeHandler.class).in(Singleton.class);
        bind(IndexChangeManager.class).to(IndexChangeManagerImpl.class).in(Singleton.class);
        bind(IndexChangeMonitor.class).to(IndexChangeMonitorImpl.class).in(Singleton.class);
        bind(IndexTreeService.class).to(IndexTreeServiceImpl.class).in(Singleton.class);
        bind(new TypeLiteral<TokenAdapter<JsonValue>>(){}).to(OAuthAdapter.class);

        /**
         * Configuration data for Data Layer LDAP connections.
         * Using a provider to defer initialisation of the factory until
         * it is needed.
         */
        bind(DataLayerConnectionFactory.class).in(Singleton.class);
        bind(DSConfigMgr.class).toProvider(new Provider<DSConfigMgr>() {
            public DSConfigMgr get() {
                try {
                    return DSConfigMgr.getDSConfigMgr();
                } catch (LDAPServiceException e) {
                    throw new IllegalStateException(e);
                }
            }
        }).in(Singleton.class);

        /**
         * Core Token Service bindings are divided into a number of logical groups.
         */
        // CTS General
        bind(Debug.class).annotatedWith(Names.named(CoreTokenConstants.CTS_DEBUG)).toInstance(Debug.getInstance(CoreTokenConstants.CTS_DEBUG));
        bind(Debug.class).annotatedWith(Names.named(CoreTokenConstants.CTS_REAPER_DEBUG)).toInstance(Debug.getInstance(CoreTokenConstants.CTS_REAPER_DEBUG));
        bind(CoreTokenConstants.class).in(Singleton.class);
        bind(CTSPersistentStore.class).in(Singleton.class);
        bind(CoreTokenConfig.class).in(Singleton.class);
        // CTS Connection Management
        bind(ConnectionFactory.class).to(CTSConnectionFactory.class).in(Singleton.class);
        bind(LDAPConfig.class).toProvider(new Provider<LDAPConfig>() {
            public LDAPConfig get() {
                return new LDAPConfig(SMSEntry.getRootSuffix());
            }
        }).in(Singleton.class);
        bind(ExternalTokenConfig.class).in(Singleton.class);
        bind(ConfigurationObserver.class).toProvider(new Provider<ConfigurationObserver>() {
            public ConfigurationObserver get() {
                return ConfigurationObserver.getInstance();
            }
        }).in(Singleton.class);
        // CTS Worker Thread Pools
        bind(ScheduledExecutorService.class)
                .annotatedWith(Names.named(CoreTokenConstants.CTS_SCHEDULED_SERVICE))
                .toProvider(new Provider<ScheduledExecutorService>() {
                    public ScheduledExecutorService get() {
                        ExecutorServiceFactory factory = InjectorHolder.getInstance(ExecutorServiceFactory.class);
                        return factory.createScheduledService(1);
                    }
                });
        bind(ExecutorService.class)
                .annotatedWith(Names.named(CoreTokenConstants.CTS_WORKER_POOL))
                .toProvider(new Provider<ExecutorService>() {
                    public ExecutorService get() {
                        ExecutorServiceFactory factory = InjectorHolder.getInstance(ExecutorServiceFactory.class);
                        return factory.createThreadPool(5);
                    }
                });

        /**
         * Session related dependencies.
         */
        bind(SessionService.class).toProvider(new Provider<SessionService>() {
            public SessionService get() {
                return SessionService.getSessionService();
            }
        }).in(Singleton.class);
        bind(Debug.class)
                .annotatedWith(Names.named(SessionConstants.SESSION_DEBUG))
                .toInstance(Debug.getInstance(SessionConstants.SESSION_DEBUG));
    }

    // Implementation exists to capture the generic type of the PrivilegedAction.
    private static class AdminTokenType extends TypeLiteral<PrivilegedAction<SSOToken>> {
    }

    // Simple provider implementation to return the static instance of AdminTokenAction.
    private static class AdminTokenProvider implements Provider<PrivilegedAction<SSOToken>> {
        public PrivilegedAction<SSOToken> get() {
            // Provider used over bind(..).getInstance(..) to enforce a lazy loading approach.
            return AdminTokenAction.getInstance();
        }
    }

    /**
     * Wrapper class to remove coupling to DNMapper static methods.
     * <p/>
     * Until DNMapper is refactored, this class can be used to assist with DI.
     */
    public static class DNWrapper {

        /**
         * @see com.sun.identity.sm.DNMapper#orgNameToDN(String)
         */
        public String orgNameToDN(String orgName) {
            return DNMapper.orgNameToDN(orgName);
        }

        /**
         * @see DNMapper#orgNameToRealmName(String)
         */
        public String orgNameToRealmName(String orgName) {
            return DNMapper.orgNameToRealmName(orgName);
        }

    }

    /**
     * Wrap class to remove coupling to ShutdownManager static methods.
     * <p/>
     * Until ShutdownManager is refactored, this class can be used to assist with DI.
     */
    public static class ShutdownManagerWrapper {

        /**
         * @see com.sun.identity.common.ShutdownManager#addShutdownListener(com.sun.identity.common.ShutdownListener)
         */
        public void addShutdownListener(ShutdownListener listener) {
            ShutdownManager shutdownManager = ShutdownManager.getInstance();

            try {
                if (shutdownManager.acquireValidLock()) {
                    // Add the listener.
                    shutdownManager.addShutdownListener(listener);
                }
            } finally {
                shutdownManager.releaseLockAndNotify();
            }
        }

        /**
         * @see com.sun.identity.common.ShutdownManager#removeShutdownListener(com.sun.identity.common.ShutdownListener)
         */
        public void removeShutdownListener(ShutdownListener listener) {
            ShutdownManager shutdownManager = ShutdownManager.getInstance();

            try {
                if (shutdownManager.acquireValidLock()) {
                    // Remove the listener.
                    shutdownManager.removeShutdownListener(listener);
                }
            } finally {
                shutdownManager.releaseLockAndNotify();
            }
        }
    }

}
