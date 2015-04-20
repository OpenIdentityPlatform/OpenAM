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
package org.forgerock.openam.entitlement.guice;

import com.google.inject.AbstractModule;
import com.google.inject.Provider;
import com.google.inject.Provides;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.name.Names;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.entitlement.EntitlementConfiguration;
import com.sun.identity.entitlement.opensso.SubjectUtils;
import com.sun.identity.entitlement.xacml3.XACMLConstants;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceConfigManager;
import org.forgerock.guice.core.GuiceModule;
import org.forgerock.openam.entitlement.configuration.ResourceTypeConfiguration;
import org.forgerock.openam.entitlement.configuration.ResourceTypeConfigurationImpl;
import org.forgerock.openam.entitlement.constraints.ConstraintValidator;
import org.forgerock.openam.entitlement.constraints.ConstraintValidatorImpl;
import org.forgerock.openam.entitlement.indextree.IndexChangeHandler;
import org.forgerock.openam.entitlement.indextree.IndexChangeManager;
import org.forgerock.openam.entitlement.indextree.IndexChangeManagerImpl;
import org.forgerock.openam.entitlement.indextree.IndexChangeMonitor;
import org.forgerock.openam.entitlement.indextree.IndexChangeMonitorImpl;
import org.forgerock.openam.entitlement.indextree.IndexTreeService;
import org.forgerock.openam.entitlement.indextree.IndexTreeServiceImpl;
import org.forgerock.openam.entitlement.indextree.events.IndexChangeObservable;
import org.forgerock.openam.entitlement.service.ApplicationService;
import org.forgerock.openam.entitlement.service.ApplicationServiceFactory;
import org.forgerock.openam.entitlement.service.ApplicationServiceImpl;
import org.forgerock.openam.entitlement.service.ResourceTypeService;
import org.forgerock.openam.entitlement.service.ResourceTypeServiceImpl;
import org.forgerock.openam.entitlement.utils.EntitlementUtils;
import org.forgerock.opendj.ldap.SearchResultHandler;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * Guice model for defining object bindings for all things related
 * to entitlements that is implemented within the entitlements maven module.
 *
 * @since 13.0.0
 */
@GuiceModule
public class EntitlementGuiceModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(IndexChangeObservable.class).in(Singleton.class);
        bind(SearchResultHandler.class).to(IndexChangeHandler.class).in(Singleton.class);
        bind(IndexChangeManager.class).to(IndexChangeManagerImpl.class).in(Singleton.class);
        bind(IndexChangeMonitor.class).to(IndexChangeMonitorImpl.class).in(Singleton.class);
        bind(IndexTreeService.class).to(IndexTreeServiceImpl.class).in(Singleton.class);

        bind(EntitlementConfiguration.class).toProvider(new Provider<EntitlementConfiguration>() {

            @Override
            public EntitlementConfiguration get() {
                return EntitlementConfiguration.getInstance(SubjectUtils.createSuperAdminSubject(), "/");
            }

        }).in(Singleton.class);

        bind(Debug.class).annotatedWith(Names.named(XACMLConstants.DEBUG))
                .toInstance(Debug.getInstance(XACMLConstants.DEBUG));

        bind(ResourceTypeConfiguration.class).to(ResourceTypeConfigurationImpl.class);
        bind(ResourceTypeService.class).to(ResourceTypeServiceImpl.class);
        bind(ConstraintValidator.class).to(ConstraintValidatorImpl.class);

        install(new FactoryModuleBuilder()
                .implement(ApplicationService.class, ApplicationServiceImpl.class)
                .build(ApplicationServiceFactory.class));
    }

    @Provides
    @Inject
    @Named(EntitlementUtils.SERVICE_NAME)
    ServiceConfigManager getServiceConfigManagerForEntitlements(final PrivilegedAction<SSOToken> adminTokenAction) {
        try {
            final SSOToken adminToken = AccessController.doPrivileged(adminTokenAction);
            return new ServiceConfigManager(EntitlementUtils.SERVICE_NAME, adminToken);
        } catch (SMSException smsE) {
            throw new IllegalStateException("Failed to retrieve the service config manager for entitlements", smsE);
        } catch (SSOException ssoE) {
            throw new IllegalStateException("Failed to retrieve the service config manager for entitlements", ssoE);
        }
    }



}
