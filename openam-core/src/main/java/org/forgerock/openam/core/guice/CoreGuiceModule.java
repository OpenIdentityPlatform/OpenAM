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
 * Copyright 2013-2016 ForgeRock AS.
 * Portions Copyrighted 2015 Nomura Research Institute, Ltd.
 * Portions Copyrighted 2025 3A Systems LLC.
 */

package org.forgerock.openam.core.guice;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.SecureRandom;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.servlet.ServletContext;

import org.forgerock.guice.core.GuiceModule;
import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.audit.context.AMExecutorServiceFactory;
import org.forgerock.openam.auditors.RepoAuditor;
import org.forgerock.openam.auditors.SMSAuditFilter;
import org.forgerock.openam.auditors.SMSAuditor;
import org.forgerock.openam.blacklist.Blacklist;
import org.forgerock.openam.blacklist.CTSBlacklist;
import org.forgerock.openam.blacklist.ConfigurableSessionBlacklist;
import org.forgerock.openam.core.DNWrapper;
import org.forgerock.openam.core.realms.RealmGuiceModule;
import org.forgerock.openam.cts.CTSPersistentStore;
import org.forgerock.openam.cts.CoreTokenServiceGuiceModule;
import org.forgerock.openam.cts.adapters.OAuthAdapter;
import org.forgerock.openam.cts.adapters.SAMLAdapter;
import org.forgerock.openam.cts.adapters.TokenAdapter;
import org.forgerock.openam.cts.api.CoreTokenConstants;
import org.forgerock.openam.cts.api.tokens.SAMLToken;
import org.forgerock.openam.cts.worker.process.CTSWorkerProcessGuiceModule;
import org.forgerock.openam.entitlement.monitoring.PolicyMonitor;
import org.forgerock.openam.entitlement.monitoring.PolicyMonitorImpl;
import org.forgerock.openam.entitlement.service.EntitlementConfigurationFactory;
import org.forgerock.openam.federation.saml2.SAML2TokenRepository;
import org.forgerock.openam.identity.idm.AMIdentityRepositoryFactory;
import org.forgerock.openam.oauth2.OAuth2Constants;
import org.forgerock.openam.session.SessionGuiceModule;
import org.forgerock.openam.shared.concurrency.ThreadMonitor;
import org.forgerock.openam.sm.SMSConfigurationFactory;
import org.forgerock.openam.sm.ServerGroupConfiguration;
import org.forgerock.openam.sm.config.ConsoleConfigHandler;
import org.forgerock.openam.sm.config.ConsoleConfigHandlerImpl;
import org.forgerock.openam.sm.datalayer.api.DataLayerConstants;
import org.forgerock.openam.sso.providers.stateless.StatelessAdminRestriction;
import org.forgerock.openam.sso.providers.stateless.StatelessAdminRestriction.SuperUserDelegate;
import org.forgerock.openam.tokens.TokenType;
import org.forgerock.openam.utils.OpenAMSettings;
import org.forgerock.openam.utils.OpenAMSettingsImpl;
import org.forgerock.util.Function;
import org.forgerock.util.promise.NeverThrowsException;

import com.google.inject.AbstractModule;
import com.google.inject.Provider;
import com.google.inject.Provides;
import com.google.inject.TypeLiteral;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;
import com.iplanet.am.util.SecureRandomManager;
import com.iplanet.dpro.session.Session;
import com.iplanet.dpro.session.monitoring.SessionMonitoringStore;
import com.iplanet.dpro.session.service.SessionServiceConfig;
import com.iplanet.dpro.session.service.SessionTimeoutHandlerExecutor;
import com.iplanet.services.ldap.DSConfigMgr;
import com.iplanet.services.ldap.LDAPServiceException;
import com.iplanet.services.naming.WebtopNamingQuery;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.delegation.DelegationManager;
import com.sun.identity.entitlement.EntitlementConfiguration;
import com.sun.identity.entitlement.opensso.EntitlementService;
import com.sun.identity.idm.AMIdentityRepository;
import com.sun.identity.idm.IdRepoCreationListener;
import com.sun.identity.idm.RepoAuditorFactory;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.setup.ServicesDefaultValues;
import com.sun.identity.shared.configuration.SystemPropertiesManager;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.validation.URLValidator;
import com.sun.identity.sm.OrganizationConfigManager;
import com.sun.identity.sm.OrganizationConfigManagerFactory;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceConfigManager;
import com.sun.identity.sm.ServiceManagementDAO;
import com.sun.identity.sm.ServiceManagementDAOWrapper;
import com.sun.identity.sm.ldap.ConfigAuditorFactory;

/**
 * Guice Module for configuring bindings for the OpenAM Core classes.
 */
@GuiceModule
public class CoreGuiceModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(new AdminTokenType()).toProvider(new AdminTokenProvider()).in(Singleton.class);
        bind(ServiceManagementDAO.class).to(ServiceManagementDAOWrapper.class).in(Singleton.class);
        bind(DNWrapper.class).in(Singleton.class);
        bind(URLValidator.class).toInstance(URLValidator.getInstance());

        bind(new TypeLiteral<TokenAdapter<JsonValue>>() {
        })
                .annotatedWith(Names.named(OAuth2Constants.CoreTokenParams.OAUTH_TOKEN_ADAPTER))
                .to(OAuthAdapter.class);

        bind(DSConfigMgr.class).toProvider(new Provider<DSConfigMgr>() {
            public DSConfigMgr get() {
                try {
                    return DSConfigMgr.getStableDSConfigMgr();
                } catch (LDAPServiceException e) {
                    throw new IllegalStateException(e);
                }
            }
        });

        bind(SSOTokenManager.class).toProvider(new Provider<SSOTokenManager>() {
            public SSOTokenManager get() {
                try {
                    return SSOTokenManager.getInstance();
                } catch (SSOException e) {
                    throw new IllegalStateException(e);
                }
            }
        }).in(Singleton.class);

        bind(Debug.class).annotatedWith(Names.named(DataLayerConstants.DATA_LAYER_DEBUG))
                .toInstance(Debug.getInstance(DataLayerConstants.DATA_LAYER_DEBUG));
        bind(Debug.class).annotatedWith(Names.named("amSMS"))
                .toInstance(Debug.getInstance("amSMS"));
        bind(Debug.class).annotatedWith(Names.named("amIdm"))
        		.toInstance(Debug.getInstance("amIdm"));
        bind(Debug.class).annotatedWith(Names.named(PolicyMonitor.POLICY_MONITOR_DEBUG))
                .toInstance(Debug.getInstance(PolicyMonitor.POLICY_MONITOR_DEBUG));
        bind(Debug.class).annotatedWith(Names.named(OAuth2Constants.DEBUG_LOG_NAME))
                .toInstance(Debug.getInstance(OAuth2Constants.DEBUG_LOG_NAME));

        // Policy Monitoring
        bind(PolicyMonitor.class).to(PolicyMonitorImpl.class);

        // SAML2 token repository dependencies
        bind(new TypeLiteral<TokenAdapter<SAMLToken>>() {
        }).to(SAMLAdapter.class);

        bind(new TypeLiteral<Function<String, String, NeverThrowsException>>() {
        })
                .annotatedWith(Names.named("tagSwapFunc"))
                .toInstance(new Function<String, String, NeverThrowsException>() {

                    @Override
                    public String apply(String text) {
                        return ServicesDefaultValues.tagSwap(text, true);
                    }

                });

        install(new FactoryModuleBuilder()
                .implement(AMIdentityRepository.class, AMIdentityRepository.class)
                .build(AMIdentityRepositoryFactory.class));

        install(new FactoryModuleBuilder()
                .implement(SMSAuditor.class, SMSAuditor.class)
                .build(ConfigAuditorFactory.class));
        
        install(new FactoryModuleBuilder()
                .implement(RepoAuditor.class, RepoAuditor.class)
                .build(RepoAuditorFactory.class));


        Multibinder.newSetBinder(binder(), SMSAuditFilter.class);

        Multibinder.newSetBinder(binder(), IdRepoCreationListener.class);

        bind(ConsoleConfigHandler.class).to(ConsoleConfigHandlerImpl.class);

        /* Entitlement bindings */
        install(new FactoryModuleBuilder()
                .implement(EntitlementConfiguration.class, EntitlementService.class)
                .build(EntitlementConfigurationFactory.class));

        install(new FactoryModuleBuilder()
                .implement(OrganizationConfigManager.class, OrganizationConfigManager.class)
                .build(OrganizationConfigManagerFactory.class));

        install(new RealmGuiceModule());
        install(new CoreTokenServiceGuiceModule());
        install(new CTSWorkerProcessGuiceModule());
        install(new SessionGuiceModule());
    }

    @Provides
    @Named("iPlanetAMAuthService")
    OpenAMSettings getSmsAuthServiceSettings() {
        return new OpenAMSettingsImpl("iPlanetAMAuthService", "1.0");
    }

    /**
     * Returns a secure random number generator suitable for generating shared secrets and other key material. This
     * uses the provider configured by system property
     * {@code com.iplanet.security.SecureRandomFactoryImpl}. By default this is the SHA-1 PRNG algorithm, seeded from
     * the system entropy pool.
     *
     * @return the configured secure random number generator.
     * @see SecureRandomManager
     * @throws Exception if an error occurs trying to obtain the secure random instance.
     */
    @Provides @Singleton
    SecureRandom getSecureRandom() throws Exception {
        return SecureRandomManager.getSecureRandom();
    }

    @Provides @Inject @Named(PolicyMonitorImpl.EXECUTOR_BINDING_NAME)
    ExecutorService getPolicyMonitoringExecutorService(AMExecutorServiceFactory esf) {
        return esf.createFixedThreadPool(5, "PolicyMonitoring");
    }

    @Provides @Inject @Named(SessionMonitoringStore.EXECUTOR_BINDING_NAME)
    ExecutorService getSessionMonitoringExecutorService(AMExecutorServiceFactory esf) {
        return esf.createFixedThreadPool(5, "SessionMonitoring");
    }

    @Provides @Inject @Named(SessionTimeoutHandlerExecutor.EXECUTOR_BINDING_NAME)
    ExecutorService getSessionTimeoutHandlerExecutorService(AMExecutorServiceFactory esf) {
        return esf.createCachedThreadPool("SessionTimeoutHandler");
    }

    @Provides @Inject @Named(CoreTokenConstants.CTS_SMS_CONFIGURATION)
    ServerGroupConfiguration getCTSServerConfiguration(SMSConfigurationFactory factory) {
        return factory.getSMSConfiguration();
    }

    @Provides @Singleton
    SAML2TokenRepository getSAML2TokenRepository() {

        final String DEFAULT_REPOSITORY_CLASS =
                "org.forgerock.openam.cts.impl.SAML2CTSPersistentStore";

        final String REPOSITORY_CLASS_PROPERTY =
                "com.sun.identity.saml2.plugins.SAML2RepositoryImpl";

        final String CTS_SAML2_REPOSITORY_CLASS_NAME =
                SystemPropertiesManager.get(REPOSITORY_CLASS_PROPERTY, DEFAULT_REPOSITORY_CLASS);

        SAML2TokenRepository result;
        try {
            // Use Guice to create class to get all of its dependency goodness
            result = InjectorHolder.getInstance(
            Class.forName(CTS_SAML2_REPOSITORY_CLASS_NAME).asSubclass(SAML2TokenRepository.class));
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }

        return result;
    }

    @Provides
    @Inject
    @Named(DelegationManager.DELEGATION_SERVICE)
    ServiceConfigManager getServiceConfigManagerForDelegation(final PrivilegedAction<SSOToken> adminTokenAction) {
        try {
            final SSOToken adminToken = AccessController.doPrivileged(adminTokenAction);
            return new ServiceConfigManager(DelegationManager.DELEGATION_SERVICE, adminToken);

        } catch (SMSException smsE) {
            throw new IllegalStateException("Failed to retrieve the service config manager for delegation", smsE);
        } catch (SSOException ssoE) {
            throw new IllegalStateException("Failed to retrieve the service config manager for delegation", ssoE);
        }
    }

    /**
     * Provides instances of the OrganizationConfigManager which requires an Admin
     * token to perform its operations.
     *
     * @param provider Non null.
     * @return Non null.
     */
    @Provides @Inject
    OrganizationConfigManager getOrganizationConfigManager(AdminTokenProvider provider) {
        SSOToken token = AccessController.doPrivileged(AdminTokenAction.getInstance());
        try {
            return new OrganizationConfigManager(token, "/");
        } catch (SMSException e) {
            throw new IllegalStateException(e);
        }
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

    @Provides
    @Named("AdminToken")
    SSOToken provideAdminSSOToken(Provider<PrivilegedAction<SSOToken>> adminTokenActionProvider) {
        return AccessController.doPrivileged(adminTokenActionProvider.get());
    }

    // provides our stored servlet context to classes which require it
    @Provides @Named(ServletContextCache.CONTEXT_REFERENCE)
    ServletContext getServletContext() {
        return ServletContextCache.getStoredContext();
    }

    @Provides @Singleton @Inject
    public CTSBlacklist<Session> getCtsSessionBlacklist(CTSPersistentStore cts, AMExecutorServiceFactory esf,
            ThreadMonitor threadMonitor, WebtopNamingQuery serverConfig, SessionServiceConfig serviceConfig) {
        ScheduledExecutorService scheduledExecutorService = esf.createScheduledService(1, "SessionBlacklistingThread");
        long purgeDelayMs = serviceConfig.getSessionBlacklistPurgeDelay(TimeUnit.MILLISECONDS);
        long pollIntervalMs = serviceConfig.getSessionBlacklistPollInterval(TimeUnit.MILLISECONDS);
        return new CTSBlacklist<>(cts, TokenType.SESSION_BLACKLIST, scheduledExecutorService, threadMonitor,
                serverConfig, purgeDelayMs, pollIntervalMs);
    }

    @Provides @Singleton @Inject
    public static Blacklist<Session> getSessionBlacklist(final CTSBlacklist<Session> ctsBlacklist,
            final SessionServiceConfig serviceConfig) {

        return ConfigurableSessionBlacklist.createConfigurableSessionBlacklist(ctsBlacklist, serviceConfig);
    }

    @Provides @Singleton @Inject
    public SuperUserDelegate getSuperUserDelegate() {
        return StatelessAdminRestriction.createAuthDDelegate();
    }

}