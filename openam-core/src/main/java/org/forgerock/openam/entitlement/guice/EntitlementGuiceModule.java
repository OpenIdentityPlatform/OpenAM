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
 * Copyright 2014 ForgeRock AS.
 */

package org.forgerock.openam.entitlement.guice;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import org.forgerock.guice.core.GuiceModule;
import org.forgerock.openam.entitlement.ConditionTypeRegistry;
import org.forgerock.openam.entitlement.EntitlementRegistry;
import org.forgerock.openam.entitlement.conditions.environment.OAuth2ScopeCondition;

import javax.inject.Inject;

/**
 * Guice module for entitlement bindings.
 *
 * @since 12.0.0
 */
@GuiceModule
public class EntitlementGuiceModule extends AbstractModule {

    /**
     * {@inheritDoc}
     */
    @Override
    protected void configure() {

    }

    @Provides
    ConditionTypeRegistry getConditionTypeRegistry() {
        ConditionTypeRegistry conditionTypeRegistry = new ConditionTypeRegistry();

        conditionTypeRegistry.addEnvironmentCondition(OAuth2ScopeCondition.class);

        return conditionTypeRegistry;
    }

    @Provides
    @Inject
    public EntitlementRegistry getEntitlementRegistry(ConditionTypeRegistry conditionTypeRegistry) {
        return new EntitlementRegistry(conditionTypeRegistry).load();
    }
}
