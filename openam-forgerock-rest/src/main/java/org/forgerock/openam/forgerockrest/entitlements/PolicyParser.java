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
 * Copyright 2014 ForgeRock, AS.
 */

package org.forgerock.openam.forgerockrest.entitlements;

import com.sun.identity.entitlement.Entitlement;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.Privilege;
import org.forgerock.json.fluent.JsonValue;

import java.util.List;

/**
 * Interface for parsing policies from some resource representation.
 *
 * @since 12.0.0
 */
public interface PolicyParser {
    /**
     * Parses the given JSON policy representation into an entitlements policy (privilege).
     *
     * @param id a unique id to assign to the policy.
     * @param representation the representation of the policy in JSON.
     * @return the equivalent policy.
     * @throws EntitlementException if an error occurs parsing the policy.
     */
    Privilege parsePolicy(String id, JsonValue representation) throws EntitlementException;

    /**
     * Converts an entitlements policy back to the JSON serialisation representation.
     *
     * @param policy the policy to convert.
     * @return the representation of the given policy.
     */
    JsonValue printPolicy(Privilege policy) throws EntitlementException;

    /**
     * Converts an entitlement decision into JSON.
     *
     * @param entitlement the entitlement to serialise.
     * @return the JSON representation of the entitlement.
     * @throws EntitlementException if an error occurs.
     */
    JsonValue printEntitlement(Entitlement entitlement) throws EntitlementException;

    /**
     * Converts a list of policy decisions to a json representation.
     *
     * @param entitlements
     *         the list of policy decisions
     *
     * @return the json representation
     *
     * @throws EntitlementException
     *         should an error occur during the transformation
     */
    JsonValue printEntitlements(final List<Entitlement> entitlements) throws EntitlementException;

}
