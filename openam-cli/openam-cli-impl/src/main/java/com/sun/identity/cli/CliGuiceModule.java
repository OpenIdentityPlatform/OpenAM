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
 * Copyright 2015-2016 ForgeRock AS.
 */

package com.sun.identity.cli;

import org.forgerock.openam.entitlement.configuration.ResourceTypeConfiguration;
import org.forgerock.openam.entitlement.configuration.ResourceTypeConfigurationImpl;
import org.forgerock.openam.entitlement.constraints.ConstraintValidator;
import org.forgerock.openam.entitlement.constraints.ConstraintValidatorImpl;
import org.forgerock.openam.entitlement.service.ApplicationService;
import org.forgerock.openam.entitlement.service.ApplicationServiceFactory;
import org.forgerock.openam.entitlement.service.ApplicationServiceImpl;
import org.forgerock.openam.entitlement.service.EntitlementConfigurationFactory;
import org.forgerock.openam.entitlement.service.ResourceTypeService;
import org.forgerock.openam.entitlement.service.ResourceTypeServiceImpl;
import org.forgerock.openam.entitlement.utils.NullNotificationBroker;
import org.forgerock.openam.notifications.NotificationBroker;
import org.forgerock.openam.session.SessionCache;

import com.google.inject.AbstractModule;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.sun.identity.entitlement.EntitlementConfiguration;
import com.sun.identity.entitlement.opensso.EntitlementService;

/**
 * Guice module for bindings that are required for the command line tools to work but are declared
 * in other Guice modules but we do not want/cannot have all of the other bindings declared in them
 * as well.
 *
 * @since 13.0.0
 */
public class CliGuiceModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(NotificationBroker.class).to(NullNotificationBroker.class);
        bind(ResourceTypeConfiguration.class).to(ResourceTypeConfigurationImpl.class);
        bind(ResourceTypeService.class).to(ResourceTypeServiceImpl.class);
        bind(ConstraintValidator.class).to(ConstraintValidatorImpl.class);
        install(new FactoryModuleBuilder()
                .implement(ApplicationService.class, ApplicationServiceImpl.class)
                .build(ApplicationServiceFactory.class));

        install(new FactoryModuleBuilder()
                .implement(EntitlementConfiguration.class, EntitlementService.class)
                .build(EntitlementConfigurationFactory.class));

        bind(SessionCache.class).toInstance(SessionCache.getInstance());
    }
}
