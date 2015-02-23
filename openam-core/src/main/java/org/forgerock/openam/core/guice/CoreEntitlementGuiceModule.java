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
package org.forgerock.openam.core.guice;

import com.google.inject.AbstractModule;
import org.forgerock.guice.core.GuiceModule;
import org.forgerock.openam.entitlement.configuration.ResourceTypeConfiguration;
import org.forgerock.openam.entitlement.configuration.ResourceTypeConfigurationImpl;
import org.forgerock.openam.entitlement.service.ResourceTypeService;
import org.forgerock.openam.entitlement.service.ResourceTypeServiceImpl;

/**
 * Guice model for defining object bindings for all things related
 * to entitlements that is implemented within the core maven module.
 *
 * @since 13.0.0
 */
@GuiceModule
public class CoreEntitlementGuiceModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(ResourceTypeConfiguration.class).to(ResourceTypeConfigurationImpl.class);
        bind(ResourceTypeService.class).to(ResourceTypeServiceImpl.class);
    }

}
