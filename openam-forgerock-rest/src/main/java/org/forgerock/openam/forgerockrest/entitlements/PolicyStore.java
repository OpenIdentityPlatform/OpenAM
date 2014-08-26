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

package org.forgerock.openam.forgerockrest.entitlements;

import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.Privilege;
import org.forgerock.json.resource.QueryRequest;

import java.util.List;

/**
 * Interface used to decouple the resource from storage of entitlements policies (privileges). Provides basic
 * CRUDQ operations for policies.
 *
 * @since 12.0.0
 */
public interface PolicyStore {

    /**
     * Creates the given policy in the underlying store.
     *
     * @param policy the policy to create. Must have a valid name.
     * @throws EntitlementException if a policy with the same name already exists or if an error occurs creating the
     * policy.
     */
    Privilege create(Privilege policy) throws EntitlementException;

    /**
     * Reads the given named policy from the policy store.
     *
     * @param policyName the name of the policy to read.
     * @return the policy.
     * @throws EntitlementException if the policy does not exist or an error occurs.
     */
    Privilege read(String policyName) throws EntitlementException;

    /**
     * Updates the given policy to match the new definition.
     * @param existingName the existing policy name
     * @param policy the policy to update.
     * @throws EntitlementException if an error occurs or the policy does not exist.
     */
    Privilege update(String existingName, Privilege policy) throws EntitlementException;

    /**
     * Deletes the given policy from the policy store.
     * @param policyName the name of the policy to delete.
     * @throws EntitlementException if an error occurs or the policy does not exist.
     */
    void delete(String policyName) throws EntitlementException;

    /**
     * Queries the store for a set of policies that match the given query.
     *
     * @param request the query request.
     * @return the query results.
     * @throws EntitlementException if an error occurs or the query is invalid.
     */
    List<Privilege> query(QueryRequest request) throws EntitlementException;
}
