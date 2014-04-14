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

import com.sun.identity.entitlement.ResourceAttribute;
import org.forgerock.openam.entitlement.EntitlementRegistry;

/**
 * Type id resolver for resource attribute subclasses. Converts simple attribute names into
 * {@link com.sun.identity.entitlement.ResourceAttribute} implementations.
 *
 * @since 12.0.0
 */
public final class ResourceAttributeTypeIdResolver extends EntitlementsRegistryTypeIdResolver<ResourceAttribute> {
    @Override
    protected String getShortName(EntitlementRegistry registry, ResourceAttribute value) {
        return registry.getAttributeName(value);
    }

    @Override
    protected Class<? extends ResourceAttribute> getType(EntitlementRegistry registry, String shortName) {
        return registry.getAttributeType(shortName);
    }
}