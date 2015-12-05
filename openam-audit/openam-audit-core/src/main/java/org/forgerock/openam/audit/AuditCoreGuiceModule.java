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
package org.forgerock.openam.audit;

import com.google.inject.AbstractModule;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.multibindings.MapBinder;
import org.forgerock.guice.core.GuiceModule;
import org.forgerock.openam.audit.servlet.Auditor;
import org.forgerock.openam.audit.servlet.AuditorFactory;
import org.forgerock.util.time.TimeService;

/**
 * Guice Module for configuring bindings for the OpenAM Audit Core classes.
 */
@GuiceModule
public class AuditCoreGuiceModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(AuditServiceProvider.class).to(AuditServiceProviderImpl.class);
        bind(AuditEventPublisher.class).to(AuditEventPublisherImpl.class);
        bind(TimeService.class).toInstance(TimeService.SYSTEM);

        install(new FactoryModuleBuilder().implement(Auditor.class, Auditor.class).build(AuditorFactory.class));

        // Initial binding for a Map of Component to AbstractHttpAccessAuditFilter
        // which other Guice modules will populate.
        MapBinder.newMapBinder(binder(), AuditConstants.Component.class, AbstractHttpAccessAuditFilter.class);
    }

}
