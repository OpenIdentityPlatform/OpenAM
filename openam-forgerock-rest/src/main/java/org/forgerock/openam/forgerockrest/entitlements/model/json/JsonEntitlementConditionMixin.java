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
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.annotate.JsonTypeInfo;
import org.codehaus.jackson.map.annotate.JsonTypeIdResolver;

import java.util.Set;

/**
 * Jackson JSON mixin to rename/ignore properties in entitlement conditions.
 *
 * @since 12.0.0
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CUSTOM, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonTypeIdResolver(EntitlementConditionTypeIdResolver.class)
public abstract class JsonEntitlementConditionMixin {

    @JsonIgnore
    public abstract String getState();

    @JsonIgnore
    public abstract String getDisplayType();

    @JsonProperty("name")
    public abstract String getPConditionName();

    @JsonProperty("conditions")
    public abstract Set<EntitlementCondition> getEConditions();

    @JsonProperty("condition")
    public abstract EntitlementCondition getECondition();

}
