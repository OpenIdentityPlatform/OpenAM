/*
 * Copyright 2014 ForgeRock, AS.
 *
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
 */

package org.forgerock.openam.forgerockrest.entitlements.model.json;

import com.sun.identity.entitlement.EntitlementCondition;
import org.forgerock.openam.entitlement.EntitlementRegistry;

/**
 * Maps string names to fully-qualified class names based on a name registry. Used to allow short names for conditions
 * in JSON to be mapped to concrete instances by Jackson.
 *
 * @since 12.0.0
 */
public final class EntitlementConditionTypeIdResolver extends EntitlementsRegistryTypeIdResolver<EntitlementCondition> {
    @Override
    protected String getShortName(EntitlementRegistry registry, EntitlementCondition value) {
        return registry.getConditionName(value);
    }

    @Override
    protected Class<? extends EntitlementCondition> getType(EntitlementRegistry registry, String shortName) {
        return registry.getConditionType(shortName);
    }
}
