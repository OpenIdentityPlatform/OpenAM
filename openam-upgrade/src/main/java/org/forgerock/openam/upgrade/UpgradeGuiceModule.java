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
package org.forgerock.openam.upgrade;

import static org.forgerock.openam.utils.CollectionUtils.asSet;

import com.google.inject.PrivateModule;
import com.google.inject.Provides;
import org.forgerock.guice.core.GuiceModule;
import org.forgerock.openam.upgrade.steps.RemoveRedundantDefaultApplication;
import org.forgerock.openam.upgrade.steps.UpgradeEntitlementSubConfigsStep;
import org.forgerock.openam.upgrade.steps.policy.UpgradeResourceTypeStep;

import javax.inject.Named;
import java.util.Set;

/**
 * Guice module to define upgrade bindings.
 *
 * @since 13.0.0
 */
@GuiceModule
public final class UpgradeGuiceModule extends PrivateModule {

    @Override
    protected void configure() {
        bind(RemoveRedundantDefaultApplication.class);
        bind(UpgradeResourceTypeStep.class);

        expose(RemoveRedundantDefaultApplication.class);
        expose(UpgradeResourceTypeStep.class);
    }

    @Provides
    @Named("removedDefaultApplications")
    Set<String> getRemovedDefaultApplications() {
        return asSet("crestPolicyService", "sunIdentityServerDiscoveryService",
                "sunIdentityServerLibertyPPService", "openProvisioning", "paycheck", "calendar", "im", "sunBank");
    }

}
