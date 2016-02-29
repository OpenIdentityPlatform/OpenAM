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
package org.forgerock.openam.entitlement.service;

import javax.security.auth.Subject;

import org.forgerock.openam.core.guice.CoreGuiceModule;
import org.forgerock.openam.entitlement.configuration.ResourceTypeConfigurationImpl;
import org.forgerock.openam.entitlement.configuration.ResourceTypeServiceConfig;

import com.sun.identity.entitlement.EntitlementConfiguration;
import com.sun.identity.entitlement.opensso.EntitlementService;

/**
 * This implementation of {@link ApplicationServiceFactory} is used as an alternative to the Guice factory for
 * {@link ApplicationService}. We do not have Guice support in the Client SDK and as such have to construct this
 * class using reflection.
 *
 * @since 13.5.0
 * @see org.forgerock.openam.entitlement.utils.EntitlementUtils#getApplicationService(Subject, String)
 */
public final class ApplicationServiceFactoryImpl implements ApplicationServiceFactory {

    private ApplicationService applicationService = null;

    @Override
    public ApplicationService create(Subject subject, String realm) {

        EntitlementConfigurationFactory factory = new EntitlementConfigurationFactory() {
            @Override
            public EntitlementConfiguration create(Subject subject, String realm) {
                return new EntitlementService(subject, realm);
            }
        };

        applicationService = new ApplicationServiceImpl(subject, realm, factory, new ResourceTypeServiceImpl(
                new ResourceTypeConfigurationImpl(new CoreGuiceModule.DNWrapper(), new ResourceTypeServiceConfig())));

        return applicationService;
    }
}
