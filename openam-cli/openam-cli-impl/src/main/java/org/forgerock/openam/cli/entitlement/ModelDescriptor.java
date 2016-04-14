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

package org.forgerock.openam.cli.entitlement;

import static java.util.Collections.singleton;

import java.util.Collections;
import java.util.Set;

import org.forgerock.json.JsonValue;

/**
 * Represents the policy model endpoints to be used in conjunction with export/import.
 *
 * @since 14.0.0
 */
enum ModelDescriptor {

    RESOURCE_TYPE("resourcetypes", "uuid", singleton("20a13582-1f32-4f83-905f-f71ff4e2e00d")),
    APPLICATION("applications", "name", singleton("sunAMDelegationService")),
    POLICY("policies", "name");

    private static final ModelDescriptor[] PRECEDENT_ORDER = {RESOURCE_TYPE, APPLICATION, POLICY};

    private final String endpointIdentifier;
    private final String idField;
    private final Set<String> excludedResources;

    ModelDescriptor(String endpointIdentifier, String idField) {
        this(endpointIdentifier, idField, Collections.<String>emptySet());
    }

    ModelDescriptor(String endpointIdentifier, String idField, Set<String> excludedResources) {
        this.endpointIdentifier = endpointIdentifier;
        this.idField = idField;
        this.excludedResources = excludedResources;
    }

    static ModelDescriptor[] getPrecedentOrder() {
        return PRECEDENT_ORDER;
    }

    String getEndpointIdentifier() {
        return endpointIdentifier;
    }

    String getIdField() {
        return idField;
    }

    boolean isExcludedResource(JsonValue resource) {
        return excludedResources.contains(resource.get(idField).required().asString());
    }

}
