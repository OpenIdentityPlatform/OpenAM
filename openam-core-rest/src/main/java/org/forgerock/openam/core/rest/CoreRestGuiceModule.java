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

package org.forgerock.openam.core.rest;

import static com.google.inject.multibindings.MapBinder.*;
import static org.forgerock.http.handler.Handlers.*;
import static org.forgerock.http.routing.RouteMatchers.*;
import static org.forgerock.http.routing.RoutingMode.*;
import static org.forgerock.http.routing.Version.*;
import static org.forgerock.openam.audit.AuditConstants.Component.*;
import static org.forgerock.openam.forgerockrest.utils.MatchingResourcePath.resourcePath;

import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.Provides;
import com.google.inject.TypeLiteral;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;
import com.iplanet.am.util.SystemProperties;
import com.iplanet.dpro.session.service.SessionService;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.idsvcs.opensso.IdentityServicesImpl;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.debug.Debug;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import org.forgerock.authz.filter.crest.api.CrestAuthorizationModule;
import org.forgerock.guice.core.GuiceModule;
import org.forgerock.http.Handler;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.routing.RouteMatchers;
import org.forgerock.json.resource.ResourcePath;
import org.forgerock.openam.audit.AbstractHttpAccessAuditFilter;
import org.forgerock.openam.audit.AuditConstants.Component;
import org.forgerock.openam.audit.HttpAccessAuditFilterFactory;
import org.forgerock.openam.core.CoreWrapper;
import org.forgerock.openam.core.rest.authn.AuthenticationAccessAuditFilter;
import org.forgerock.openam.core.rest.authn.http.AuthenticationServiceV1;
import org.forgerock.openam.core.rest.authn.http.AuthenticationServiceV2;
import org.forgerock.openam.core.rest.cts.CoreTokenResource;
import org.forgerock.openam.core.rest.cts.CoreTokenResourceAuthzModule;
import org.forgerock.openam.core.rest.record.DebugRecorder;
import org.forgerock.openam.core.rest.record.DefaultDebugRecorder;
import org.forgerock.openam.core.rest.session.AnyOfAuthzModule;
import org.forgerock.openam.core.rest.session.SessionResourceAuthzModule;
import org.forgerock.openam.core.rest.sms.SmsCollectionProvider;
import org.forgerock.openam.core.rest.sms.SmsCollectionProviderFactory;
import org.forgerock.openam.core.rest.sms.SmsGlobalSingletonProvider;
import org.forgerock.openam.core.rest.sms.SmsGlobalSingletonProviderFactory;
import org.forgerock.openam.core.rest.sms.SmsRequestHandler;
import org.forgerock.openam.core.rest.sms.SmsRequestHandlerFactory;
import org.forgerock.openam.core.rest.sms.SmsSingletonProvider;
import org.forgerock.openam.core.rest.sms.SmsSingletonProviderFactory;
import org.forgerock.openam.cts.utils.JSONSerialisation;
import org.forgerock.openam.forgerockrest.utils.MailServerLoader;
import org.forgerock.openam.forgerockrest.utils.MatchingResourcePath;
import org.forgerock.openam.http.annotations.Endpoints;
import org.forgerock.openam.rest.authz.AdminOnlyAuthzModule;
import org.forgerock.openam.rest.authz.AnyPrivilegeAuthzModule;
import org.forgerock.openam.rest.authz.PrivilegeAuthzModule;
import org.forgerock.openam.rest.authz.PrivilegeWriteAndAnyPrivilegeReadOnlyAuthzModule;
import org.forgerock.openam.rest.resource.SSOTokenContext;
import org.forgerock.openam.rest.router.CTSPersistentStoreProxy;
import org.forgerock.openam.services.RestSecurityProvider;
import org.forgerock.openam.services.baseurl.BaseURLProviderFactory;
import org.forgerock.openam.sm.config.ConsoleConfigHandler;
import org.forgerock.openam.utils.Config;
import org.forgerock.services.routing.RouteMatcher;

/**
 * Guice module for binding the core REST endpoints.
 *
 * @since 13.0.0
 */
@GuiceModule
public class CoreRestGuiceModule extends AbstractModule {

    @Override
    protected void configure() {
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

        bind(DebugRecorder.class).to(DefaultDebugRecorder.class);

        MapBinder<RouteMatcher<Request>, Handler> chfEndpointHandlers = newMapBinder(binder(),
                new TypeLiteral<RouteMatcher<Request>>() {
                }, new TypeLiteral<Handler>() {
                });
        chfEndpointHandlers.addBinding(requestUriMatcher(EQUALS, "authenticate"))
                .to(Key.get(Handler.class, Names.named("AuthenticateHandler")));

        MapBinder<Component, AbstractHttpAccessAuditFilter> httpAccessAuditFilterMapBinder
                = newMapBinder(binder(), Component.class, AbstractHttpAccessAuditFilter.class);
        httpAccessAuditFilterMapBinder.addBinding(AUTHENTICATION).to(AuthenticationAccessAuditFilter.class);

        Multibinder<UiRolePredicate> userUiRolePredicates = Multibinder.newSetBinder(binder(),
                UiRolePredicate.class);

        userUiRolePredicates.addBinding().to(SelfServiceUserUiRolePredicate.class);
        userUiRolePredicates.addBinding().to(GlobalAdminUiRolePredicate.class);
        userUiRolePredicates.addBinding().to(RealmAdminUiRolePredicate.class);

        MapBinder<MatchingResourcePath, CrestAuthorizationModule> smsGlobalAuthzModuleBinder =
                MapBinder.newMapBinder(binder(), MatchingResourcePath.class, CrestAuthorizationModule.class);
        smsGlobalAuthzModuleBinder.addBinding(resourcePath("realms")).to(AnyPrivilegeAuthzModule.class);
        smsGlobalAuthzModuleBinder.addBinding(resourcePath("authentication/modules/*"))
                .to(PrivilegeWriteAndAnyPrivilegeReadOnlyAuthzModule.class);
        smsGlobalAuthzModuleBinder.addBinding(resourcePath("services/scripting"))
                .to(PrivilegeWriteAndAnyPrivilegeReadOnlyAuthzModule.class);
        smsGlobalAuthzModuleBinder.addBinding(resourcePath("services/scripting/contexts"))
                .to(PrivilegeWriteAndAnyPrivilegeReadOnlyAuthzModule.class);
    }

    @Provides
    @Named("AuthenticateHandler")
    @Inject
    Handler getAuthenticateHandler(@Named("InvalidRealmNames") Set<String> invalidRealms,
            HttpAccessAuditFilterFactory httpAuditFactory) {
        invalidRealms.add(firstPathSegment("authenticate"));
        org.forgerock.http.routing.Router authenticateVersionRouter = new org.forgerock.http.routing.Router();
        Handler authenticateHandlerV1 = Endpoints.from(AuthenticationServiceV1.class);
        Handler authenticateHandlerV2 = Endpoints.from(AuthenticationServiceV2.class);
        authenticateVersionRouter.addRoute(RouteMatchers.requestResourceApiVersionMatcher(version(1, 1)),
                authenticateHandlerV1);
        authenticateVersionRouter.addRoute(RouteMatchers.requestResourceApiVersionMatcher(version(2)),
                authenticateHandlerV2);
        return chainOf(authenticateVersionRouter, httpAuditFactory.createFilter(AUTHENTICATION));
    }

    /**
     * Returns the first path segment from a uri template. For example {@code /foo/bar} becomes {@code foo}.
     *
     * @param path
     *         the full uri template path.
     *
     * @return the first non-empty path segment.
     *
     * @throws IllegalArgumentException
     *         if the path contains no non-empty segments.
     */
    private static String firstPathSegment(final String path) {
        for (String part : path.split("/")) {
            if (!part.isEmpty()) {
                return part;
            }
        }
        throw new IllegalArgumentException("uriTemplate " + path + " is invalid");
    }

    @Provides
    @Named("UsersResource")
    @Inject
    @Singleton
    public IdentityResourceV1 getUsersResourceV1(MailServerLoader mailServerLoader,
            IdentityServicesImpl identityServices, CoreWrapper coreWrapper, RestSecurityProvider restSecurityProvider,
            ConsoleConfigHandler configHandler, Set<UiRolePredicate> uiRolePredicates) {
        return new IdentityResourceV1(IdentityResourceV1.USER_TYPE, mailServerLoader, identityServices, coreWrapper,
                restSecurityProvider, configHandler, uiRolePredicates);
    }

    @Provides
    @Named("UsersResource")
    @Inject
    @Singleton
    public IdentityResourceV2 getUsersResourceV2(MailServerLoader mailServerLoader,
            IdentityServicesImpl identityServices, CoreWrapper coreWrapper, RestSecurityProvider restSecurityProvider,
            ConsoleConfigHandler configHandler, BaseURLProviderFactory baseURLProviderFactory, Set<UiRolePredicate> uiRolePredicates) {
        return new IdentityResourceV2(IdentityResourceV2.USER_TYPE, mailServerLoader, identityServices, coreWrapper,
                restSecurityProvider, configHandler, baseURLProviderFactory, uiRolePredicates);
    }

    @Provides
    @Named("UsersResource")
    @Inject
    @Singleton
    public IdentityResourceV3 getUsersResource(MailServerLoader mailServerLoader,
            IdentityServicesImpl identityServices, CoreWrapper coreWrapper, RestSecurityProvider restSecurityProvider,
            ConsoleConfigHandler configHandler, BaseURLProviderFactory baseURLProviderFactory,
            @Named("PatchableUserAttributes") Set<String> patchableAttributes, Set<UiRolePredicate> uiRolePredicates) {
        return new IdentityResourceV3(IdentityResourceV2.USER_TYPE, mailServerLoader, identityServices, coreWrapper,
                restSecurityProvider, configHandler, baseURLProviderFactory, patchableAttributes, uiRolePredicates);
    }

    @Provides
    @Named("GroupsResource")
    @Inject
    @Singleton
    public IdentityResourceV1 getGroupsResourceV1(MailServerLoader mailServerLoader,
            IdentityServicesImpl identityServices, CoreWrapper coreWrapper, RestSecurityProvider restSecurityProvider,
            ConsoleConfigHandler configHandler, Set<UiRolePredicate> uiRolePredicates) {
        return new IdentityResourceV1(IdentityResourceV1.GROUP_TYPE, mailServerLoader, identityServices, coreWrapper,
                restSecurityProvider, configHandler, uiRolePredicates);
    }

    @Provides
    @Named("GroupsResource")
    @Inject
    @Singleton
    public IdentityResourceV2 getGroupsResourceV2(MailServerLoader mailServerLoader,
            IdentityServicesImpl identityServices, CoreWrapper coreWrapper, RestSecurityProvider restSecurityProvider,
            ConsoleConfigHandler configHandler, BaseURLProviderFactory baseURLProviderFactory, Set<UiRolePredicate> uiRolePredicates) {
        return new IdentityResourceV2(IdentityResourceV2.GROUP_TYPE, mailServerLoader, identityServices, coreWrapper,
                restSecurityProvider, configHandler, baseURLProviderFactory, uiRolePredicates);
    }

    @Provides
    @Named("GroupsResource")
    @Inject
    @Singleton
    public IdentityResourceV3 getGroupsResource(MailServerLoader mailServerLoader,
            IdentityServicesImpl identityServices, CoreWrapper coreWrapper, RestSecurityProvider restSecurityProvider,
            ConsoleConfigHandler configHandler, BaseURLProviderFactory baseURLProviderFactory, Set<UiRolePredicate> uiRolePredicates) {
        return new IdentityResourceV3(IdentityResourceV2.GROUP_TYPE, mailServerLoader, identityServices,
                coreWrapper, restSecurityProvider, configHandler, baseURLProviderFactory, Collections.<String>emptySet(), uiRolePredicates);
    }

    @Provides
    @Named("AgentsResource")
    @Inject
    @Singleton
    public IdentityResourceV1 getAgentsResourceV1(MailServerLoader mailServerLoader,
            IdentityServicesImpl identityServices, CoreWrapper coreWrapper, RestSecurityProvider restSecurityProvider,
            ConsoleConfigHandler configHandler, Set<UiRolePredicate> uiRolePredicates) {
        return new IdentityResourceV1(IdentityResourceV1.AGENT_TYPE, mailServerLoader, identityServices, coreWrapper,
                restSecurityProvider, configHandler, uiRolePredicates);
    }

    @Provides
    @Named("AgentsResource")
    @Inject
    @Singleton
    public IdentityResourceV2 getAgentsResourceV2(MailServerLoader mailServerLoader,
            IdentityServicesImpl identityServices, CoreWrapper coreWrapper, RestSecurityProvider restSecurityProvider,
            ConsoleConfigHandler configHandler, BaseURLProviderFactory baseURLProviderFactory, Set<UiRolePredicate> uiRolePredicates) {
        return new IdentityResourceV2(IdentityResourceV2.AGENT_TYPE, mailServerLoader, identityServices, coreWrapper,
                restSecurityProvider, configHandler, baseURLProviderFactory, uiRolePredicates);
    }

    @Provides
    @Named("AgentsResource")
    @Inject
    @Singleton
    public IdentityResourceV3 getAgentsResource(MailServerLoader mailServerLoader,
            IdentityServicesImpl identityServices, CoreWrapper coreWrapper, RestSecurityProvider restSecurityProvider,
            ConsoleConfigHandler configHandler, BaseURLProviderFactory baseURLProviderFactory, Set<UiRolePredicate> uiRolePredicates) {
        return new IdentityResourceV3(IdentityResourceV2.AGENT_TYPE, mailServerLoader, identityServices,
                coreWrapper, restSecurityProvider, configHandler, baseURLProviderFactory, Collections.<String>emptySet(), uiRolePredicates);
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

    @Provides
    @Inject
    public AnyOfAuthzModule getSessionResourceAuthzModule(SSOTokenManager ssoTokenManager,
            PrivilegeAuthzModule privilegeAuthzModule,
            AdminOnlyAuthzModule adminOnlyAuthzModule) {
        SessionResourceAuthzModule sessionResourceAuthzModule = new SessionResourceAuthzModule(ssoTokenManager);
        List<CrestAuthorizationModule> authzList = new ArrayList<>(3);
        authzList.add(adminOnlyAuthzModule);
        authzList.add(privilegeAuthzModule);
        authzList.add(sessionResourceAuthzModule);
        return new AnyOfAuthzModule(authzList);

    }

    @Provides
    @Named("PatchableUserAttributes")
    public Set<String> getPatchableUserAttributes() {
        Set<String> patchableAttributes = new HashSet<>();
        patchableAttributes.add("userPassword");
        patchableAttributes.add("kbaInfo");
        return patchableAttributes;
    }

}

