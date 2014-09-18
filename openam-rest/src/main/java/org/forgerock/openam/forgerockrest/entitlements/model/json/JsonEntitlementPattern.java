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

import com.sun.identity.entitlement.Entitlement;
import org.codehaus.jackson.annotate.JsonIgnore;

import java.util.Map;
import java.util.Set;

/**
 * Provides a restricted view of an entitlement, ignoring any fields that are not applicable when the entitlement is
 * being used to define a policy rather than as an entitlement decision. It ignores properties such as TTL, advices,
 * attributes. NB: this is a horrible abuse of inheritance that violates Liskov substitutability. However, it is very
 * convenient and these classes are only used to shape JSON serialisation/deserialisation.
 *
 * @since 12.0.0
 */
public final class JsonEntitlementPattern extends JsonEntitlement {

    public JsonEntitlementPattern(Entitlement entitlement) {
        super(entitlement);
    }

    public JsonEntitlementPattern() {
        super();
    }

    @JsonIgnore
    @Override
    public String getName() {
        return super.getName();
    }

    @JsonIgnore
    @Override
    public long getTTL() {
        return super.getTTL();
    }

    @JsonIgnore
    @Override
    public Map<String, Set<String>> getAdvice() {
        return super.getAdvice();
    }

    @JsonIgnore
    @Override
    public Map<String, Set<String>> getAttributes() {
        return super.getAttributes();
    }
}
