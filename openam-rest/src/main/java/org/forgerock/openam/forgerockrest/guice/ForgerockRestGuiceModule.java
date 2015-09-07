/*
 * Copyright 2014-2015 ForgeRock AS.
 *
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
 */

package org.forgerock.openam.forgerockrest.guice;


import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import com.google.inject.AbstractModule;
import com.google.inject.Provider;
import com.google.inject.Provides;
import com.google.inject.TypeLiteral;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.name.Names;
import com.iplanet.am.util.SystemProperties;
import com.iplanet.dpro.session.service.SessionService;
import com.sun.identity.delegation.DelegationEvaluator;
import com.sun.identity.delegation.DelegationEvaluatorImpl;
import com.sun.identity.idsvcs.opensso.IdentityServicesImpl;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.guice.core.GuiceModule;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openam.core.CoreWrapper;
import org.forgerock.openam.cts.utils.JSONSerialisation;
import org.forgerock.openam.entitlement.EntitlementRegistry;
import org.forgerock.openam.errors.ExceptionMappingHandler;
import org.forgerock.openam.forgerockrest.IdentityResourceV1;
import org.forgerock.openam.forgerockrest.IdentityResourceV2;
import org.forgerock.openam.forgerockrest.cts.CoreTokenResource;
import org.forgerock.openam.forgerockrest.utils.AgentIdentity;
import org.forgerock.openam.forgerockrest.utils.AgentIdentityImpl;
import org.forgerock.openam.forgerockrest.utils.MailServerLoader;
import org.forgerock.openam.forgerockrest.utils.RestLog;
import org.forgerock.openam.forgerockrest.utils.SpecialUserIdentity;
import org.forgerock.openam.forgerockrest.utils.SpecialUserIdentityImpl;
import org.forgerock.openam.rest.authz.CoreTokenResourceAuthzModule;
import org.forgerock.openam.rest.authz.PrivilegeDefinition;
import org.forgerock.openam.rest.record.DebugRecorder;
import org.forgerock.openam.rest.record.DefaultDebugRecorder;
import org.forgerock.openam.rest.router.CTSPersistentStoreProxy;
import org.forgerock.openam.rest.router.DelegationEvaluatorProxy;
import org.forgerock.openam.rest.sms.SmsCollectionProvider;
import org.forgerock.openam.rest.sms.SmsCollectionProviderFactory;
import org.forgerock.openam.rest.sms.SmsGlobalSingletonProvider;
import org.forgerock.openam.rest.sms.SmsGlobalSingletonProviderFactory;
import org.forgerock.openam.rest.sms.SmsRequestHandler;
import org.forgerock.openam.rest.sms.SmsRequestHandlerFactory;
import org.forgerock.openam.rest.sms.SmsSingletonProvider;
import org.forgerock.openam.rest.sms.SmsSingletonProviderFactory;
import org.forgerock.openam.services.RestSecurityProvider;
import org.forgerock.openam.services.baseurl.BaseURLProviderFactory;
import org.forgerock.openam.utils.AMKeyProvider;
import org.forgerock.openam.utils.Config;
import org.forgerock.util.SignatureUtil;

/**
 * Guice Module for configuring bindings for the AuthenticationRestService classes.
 */
@GuiceModule
public class ForgerockRestGuiceModule extends AbstractModule {

    /**
     * {@inheritDoc}
     */
    @Override
    protected void configure() {
        bind(AMKeyProvider.class).in(Singleton.class);
        bind(SignatureUtil.class).toProvider(new Provider<SignatureUtil>() {
            public SignatureUtil get() {
                return SignatureUtil.getInstance();
            }
        });

        bind(EntitlementRegistry.class).toInstance(EntitlementRegistry.load());

        bind(Debug.class).annotatedWith(Names.named("frRest")).toInstance(Debug.getInstance("frRest"));

        bind(DelegationEvaluatorImpl.class).in(Singleton.class);
        bind(DelegationEvaluator.class).to(DelegationEvaluatorProxy.class).in(Singleton.class);

        bind(DebugRecorder.class).to(DefaultDebugRecorder.class);
        bind(SpecialUserIdentity.class).to(SpecialUserIdentityImpl.class);
        bind(AgentIdentity.class).to(AgentIdentityImpl.class);

        install(new FactoryModuleBuilder()
                .implement(SmsRequestHandler.class, SmsRequestHandler.class)
                .build(SmsRequestHandlerFactory.class));

        install(new FactoryModuleBuilder()
                .implement(SmsCollectionProvider.class, SmsCollectionProvider.class)
                .build(SmsCollectionProviderFactory.class));

        install(new FactoryModuleBuilder()
                .implement(SmsSingletonProvider.class, SmsSingletonProvider.class)
                .build(SmsSingletonProviderFactory.class));

        install(new FactoryModuleBuilder()
                .implement(SmsGlobalSingletonProvider.class, SmsGlobalSingletonProvider.class)
                .build(SmsGlobalSingletonProviderFactory.class));
    }

    @Provides
    @Named("UsersResource")
    @Inject
    @Singleton
    public IdentityResourceV1 getUsersResourceV1(MailServerLoader mailServerLoader,
            IdentityServicesImpl identityServices, CoreWrapper coreWrapper, RestSecurityProvider restSecurityProvider) {
        return new IdentityResourceV1(IdentityResourceV1.USER_TYPE, mailServerLoader, identityServices, coreWrapper,
                restSecurityProvider);
    }

    @Provides
    @Named("GroupsResource")
    @Inject
    @Singleton
    public IdentityResourceV1 getGroupsResourceV1(MailServerLoader mailServerLoader,
            IdentityServicesImpl identityServices, CoreWrapper coreWrapper, RestSecurityProvider restSecurityProvider) {
        return new IdentityResourceV1(IdentityResourceV1.GROUP_TYPE, mailServerLoader, identityServices, coreWrapper,
                restSecurityProvider);
    }

    @Provides
    @Singleton
    public RestLog getRestLog() {
        return new RestLog();
    }

    @Provides
    @Named("AgentsResource")
    @Inject
    @Singleton
    public IdentityResourceV1 getAgentsResourceV1(MailServerLoader mailServerLoader,
            IdentityServicesImpl identityServices, CoreWrapper coreWrapper, RestSecurityProvider restSecurityProvider) {
        return new IdentityResourceV1(IdentityResourceV1.AGENT_TYPE, mailServerLoader, identityServices, coreWrapper,
                restSecurityProvider);
    }

    @Provides
    @Named("UsersResource")
    @Inject
    @Singleton
    public IdentityResourceV2 getUsersResource(MailServerLoader mailServerLoader,
            IdentityServicesImpl identityServices, CoreWrapper coreWrapper, RestSecurityProvider
            restSecurityProvider, BaseURLProviderFactory baseURLProviderFactory) {
        return new IdentityResourceV2(IdentityResourceV2.USER_TYPE, mailServerLoader, identityServices, coreWrapper,
                restSecurityProvider, baseURLProviderFactory);
    }

    @Provides
    @Named("GroupsResource")
    @Inject
    @Singleton
    public IdentityResourceV2 getGroupsResource(MailServerLoader mailServerLoader,
            IdentityServicesImpl identityServices, CoreWrapper coreWrapper, RestSecurityProvider
            restSecurityProvider, BaseURLProviderFactory baseURLProviderFactory) {
        return new IdentityResourceV2(IdentityResourceV2.GROUP_TYPE, mailServerLoader, identityServices, coreWrapper,
                restSecurityProvider, baseURLProviderFactory);
    }

    @Provides
    @Named("AgentsResource")
    @Inject
    @Singleton
    public IdentityResourceV2 getAgentsResource(MailServerLoader mailServerLoader,
            IdentityServicesImpl identityServices, CoreWrapper coreWrapper, RestSecurityProvider
            restSecurityProvider, BaseURLProviderFactory baseURLProviderFactory) {
        return new IdentityResourceV2(IdentityResourceV2.AGENT_TYPE, mailServerLoader, identityServices, coreWrapper,
                restSecurityProvider, baseURLProviderFactory);
    }

    @Provides
    @Inject
    @Singleton
    public CoreTokenResource getCoreTokenResource(JSONSerialisation jsonSerialisation,
            CTSPersistentStoreProxy ctsPersistentStore, @Named("frRest") Debug debug) {
        return new CoreTokenResource(jsonSerialisation, ctsPersistentStore, debug);
    }

    @Provides
    @Inject
    @Singleton
    public CoreTokenResourceAuthzModule getCoreTokenResourceAuthzModule(
            Config<SessionService> sessionService, @Named("frRest") Debug debug) {
        boolean coreTokenResourceEnabled = SystemProperties.getAsBoolean(Constants.CORE_TOKEN_RESOURCE_ENABLED);
        return new CoreTokenResourceAuthzModule(sessionService, debug, coreTokenResourceEnabled);
    }

    @Provides
    @Singleton
    public Map<String, PrivilegeDefinition> getPrivilegeDefinitions() {
        final Map<String, PrivilegeDefinition> definitions = new HashMap<String, PrivilegeDefinition>();

        final PrivilegeDefinition evaluateDefinition = PrivilegeDefinition
                .getInstance("evaluate", PrivilegeDefinition.Action.READ);
        definitions.put("evaluate", evaluateDefinition);
        definitions.put("evaluateTree", evaluateDefinition);
        definitions.put("schema", PrivilegeDefinition
                .getInstance("schema", PrivilegeDefinition.Action.READ));
        definitions.put("template", PrivilegeDefinition
                .getInstance("template", PrivilegeDefinition.Action.READ));

        return definitions;
    }

    @Provides
    @Singleton
    @Named("ServerAttributeSyntax")
    public Properties getServerAttributeSyntax() throws IOException {
        Properties syntaxProperties = new Properties();
        syntaxProperties.load(getClass().getClassLoader().getResourceAsStream("validserverconfig.properties"));
        return syntaxProperties;
    }

    @Provides
    @Singleton
    @Named("ServerAttributeTitles")
    public Properties getServerAttributeTitles() throws IOException {
        Properties titleProperties = new Properties();
        titleProperties.load(getClass().getClassLoader().getResourceAsStream("amConsole.properties"));
        return titleProperties;
    }
}
