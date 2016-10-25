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
 */
package org.forgerock.openam.cts;

import static org.mockito.Mockito.mock;

import org.forgerock.guice.core.GuiceModules;
import org.forgerock.guice.core.GuiceTestCase;
import org.forgerock.openam.audit.AuditCoreGuiceModule;
import org.forgerock.openam.audit.AuditEventPublisher;
import org.forgerock.openam.audit.AuditServiceProvider;
import org.forgerock.openam.audit.configuration.AuditServiceConfigurationProvider;
import org.forgerock.openam.core.guice.CoreGuiceModule;
import org.forgerock.openam.core.guice.DataLayerGuiceModule;
import org.forgerock.openam.notifications.NotificationBroker;
import org.forgerock.openam.session.SessionGuiceModule;
import org.forgerock.openam.shared.guice.SharedGuiceModule;

import com.google.inject.Binder;

/**
 * Assists with the process of developing integration tests which require the CTS, so as to reduce
 * code duplication around required Guice dependencies.
 */
// TODO: This class will not operate until COMMONS-129 has been merged and a new release of forgerock-guice has been made.
@GuiceModules({CoreTokenServiceGuiceModule.class,
        DataLayerGuiceModule.class,
        AuditCoreGuiceModule.class,
        SessionGuiceModule.class,
        SharedGuiceModule.class,
        CoreGuiceModule.class})
public class CTSBasedGuiceTestCase extends GuiceTestCase {

    /**
     * When defining the Guice modules for the CTS, we bring in Session and therefore Audit.
     * As such we need to mock out an Audit provider.
     *
     * @param binder Non null.
     */
    @Override
    protected void configureOverrideBindings(Binder binder) {
        super.configureOverrideBindings(binder);

        // Required to remove need for openam-rest to depend on openam-audit
        binder.bind(AuditEventPublisher.class).toInstance(mock(AuditEventPublisher.class));
        binder.bind(AuditServiceConfigurationProvider.class).toInstance(mock(AuditServiceConfigurationProvider.class));
        binder.bind(AuditServiceProvider.class).toInstance(mock(AuditServiceProvider.class));

        // Required to remove need for openam-rest to depend on openam-notifications
        binder.bind(NotificationBroker.class).toInstance(mock(NotificationBroker.class));
    }
}
