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

package org.forgerock.openam.forgerockrest.entitlements.model.json;

import com.sun.identity.entitlement.Entitlement;
import org.forgerock.util.Reject;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * Used to assist with json creation of a policy decision by translating the underlying {@link Entitlement}.
 *
 * @since 12.0.0
 */
public final class JsonDecision {

    private final Entitlement entitlement;

    public JsonDecision(final Entitlement entitlement) {
        Reject.ifNull(entitlement);
        this.entitlement = entitlement;
    }

    public String getResource() {
        return entitlement.getRequestedResourceName();
    }

    public Map<String, Boolean> getActions() {
        return entitlement.getActionValues() != null ?
                entitlement.getActionValues() : Collections.<String, Boolean>emptyMap();
    }

    public Map<String, Set<String>> getAttributes() {
        return entitlement.getAttributes() != null ?
                entitlement.getAttributes() : Collections.<String, Set<String>>emptyMap();
    }

    public Map<String, Set<String>> getAdvices() {
        return entitlement.getAdvices() != null ?
                entitlement.getAdvices() : Collections.<String, Set<String>>emptyMap();
    }

}
