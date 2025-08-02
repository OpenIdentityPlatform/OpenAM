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

package org.forgerock.openam.core.rest.sms;

import static org.forgerock.openam.core.rest.sms.SmsRealmProvider.REALMS_PATH;
import static org.forgerock.openam.forgerockrest.utils.MatchingResourcePath.resourcePath;
import static org.forgerock.openam.rest.RealmRoutingFactory.REALM_ROUTE;

import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.io.IOException;
import java.security.AccessController;
import java.util.HashSet;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.multibindings.Multibinder;
import com.iplanet.sso.SSOToken;
import com.sun.identity.authentication.config.AMAuthenticationManager;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.locale.AMResourceBundleCache;
import com.sun.identity.sm.SchemaType;
import org.forgerock.authz.filter.crest.api.CrestAuthorizationModule;
import org.forgerock.openam.core.rest.UiRolePredicate;
import org.forgerock.openam.forgerockrest.utils.MatchingResourcePath;
import org.forgerock.openam.rest.authz.AnyPrivilegeAuthzModule;
import org.forgerock.openam.rest.authz.PrivilegeWriteAndAnyPrivilegeReadOnlyAuthzModule;

/**
 * Guice module for binding the REST SMS REST endpoints.
 *
 * @since 14.0.0
 */
public class CoreRestSmsGuiceModule extends AbstractModule {

    @Override
    protected void configure() {
        install(new FactoryModuleBuilder()
                .implement(SmsRequestHandler.class, SmsRequestHandler.class)
                .build(SmsRequestHandlerFactory.class));

        install(new FactoryModuleBuilder()
                .implement(SmsCollectionProvider.class, SmsCollectionProvider.class)
                .build(SmsCollectionProviderFactory.class));

        install(new FactoryModuleBuilder()
                .implement(SmsServerPropertiesResource.class, SmsServerPropertiesResource.class)
                .build(SmsServerPropertiesResourceFactory.class));

        install(new FactoryModuleBuilder()
                .implement(SmsSingletonProvider.class, SmsSingletonProvider.class)
                .build(SmsSingletonProviderFactory.class));

        install(new FactoryModuleBuilder()
                .implement(SmsGlobalSingletonProvider.class, SmsGlobalSingletonProvider.class)
                .build(SmsGlobalSingletonProviderFactory.class));

        Multibinder<UiRolePredicate> userUiRolePredicates = Multibinder.newSetBinder(binder(),
                UiRolePredicate.class);

        MapBinder<MatchingResourcePath, CrestAuthorizationModule> smsGlobalAuthzModuleBinder =
                MapBinder.newMapBinder(binder(), MatchingResourcePath.class, CrestAuthorizationModule.class);
        smsGlobalAuthzModuleBinder.addBinding(resourcePath(REALMS_PATH)).to(AnyPrivilegeAuthzModule.class);
        smsGlobalAuthzModuleBinder.addBinding(resourcePath("authentication/modules/*"))
                .to(PrivilegeWriteAndAnyPrivilegeReadOnlyAuthzModule.class);
        smsGlobalAuthzModuleBinder.addBinding(resourcePath("services/scripting"))
                .to(PrivilegeWriteAndAnyPrivilegeReadOnlyAuthzModule.class);
        smsGlobalAuthzModuleBinder.addBinding(resourcePath("services/scripting/contexts"))
                .to(PrivilegeWriteAndAnyPrivilegeReadOnlyAuthzModule.class);
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
    @Named("serviceSupportedSchemaTypes")
    public Set<SchemaType> getServiceSupportedSchemaTypes() {
        Set<SchemaType> supportedSchemaTypes = new HashSet<>();
        supportedSchemaTypes.add(SchemaType.ORGANIZATION);
        supportedSchemaTypes.add(SchemaType.DYNAMIC);
        return supportedSchemaTypes;
    }

    @Provides
    @Named("authenticationServices")
    public Set<String> getAuthenticationServices() {
        return AMAuthenticationManager.getAuthenticationServiceNames();
    }

    @Provides
    @Named("hiddenServices")
    public Set<String> getHiddenServiceNames() {
        Set<String> hiddenServices = new HashSet<>();
        hiddenServices.add("iPlanetAMAuthConfiguration");
        hiddenServices.add("iPlanetAMAuthService");
        hiddenServices.add("RestSecurityTokenService");
        hiddenServices.add("SoapSecurityTokenService");
        hiddenServices.add("sunIdentityServerDiscoveryService");
        return hiddenServices;
    }

    @Provides
    @Named("AMResourceBundleCache")
    public AMResourceBundleCache getAMResourceBundleCache() {
        return AMResourceBundleCache.getInstance();
    }

    @Provides
    @Named("DefaultLocale")
    public Locale getDefaultLocale() { return Locale.getDefault(); }

    @Provides
    @Named("adminToken")
    public SSOToken getAdminToken() { return AccessController.doPrivileged(AdminTokenAction.getInstance()); }
}