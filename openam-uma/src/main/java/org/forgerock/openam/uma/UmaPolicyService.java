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
 * Copyright 2015 ForgeRock AS.
 */

package org.forgerock.openam.uma;

import java.util.Collection;

import org.forgerock.services.context.Context;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.util.Pair;
import org.forgerock.util.promise.Promise;

/**
 * Provides the contract for accessing and managing UMA policies that are in a backend store.
 *
 * @since 13.0.0
 */
public interface UmaPolicyService {

    /**
     * Creates an UMA policy in the backend store.
     *
     * @param context The request context.
     * @param policy The UMA policy to create.
     * @return A promise containing the created {@code UmaPolicy} or a {@code ResourceException}.
     */
    Promise<UmaPolicy, ResourceException> createPolicy(Context context, JsonValue policy);

    /**
     * Reads an UMA policy from the backend store.
     *
     * @param context The request context.
     * @param resourceSetId The unique ID of the UMA policy.
     * @return A promise containing the {@code UmaPolicy} or a {@code ResourceException}.
     */
    Promise<UmaPolicy, ResourceException> readPolicy(Context context, String resourceSetId);

    /**
     * Updates an UMA policy in the backend store.
     *
     * @param context The request context.
     * @param resourceSetId The unique ID of the UMA policy.
     * @param policy The UMA policy to replace the current policy.
     * @return A promise containing the updated {@code UmaPolicy} or a {@code ResourceException}.
     */
    Promise<UmaPolicy, ResourceException> updatePolicy(Context context, String resourceSetId, JsonValue policy);

    /**
     * Deletes an UMA policy from the backend store.
     *
     * @param context The request context.
     * @param resourceSetId The unique ID of the UMA policy.
     * @return A promise containing {@code null} or a {@code ResourceException}.
     */
    Promise<Void, ResourceException> deletePolicy(Context context, String resourceSetId);

    /**
     *
     *
     * @param context
     * @param request
     * @return
     */
    Promise<Pair<QueryResponse, Collection<UmaPolicy>>, ResourceException> queryPolicies(Context context,
            QueryRequest request);
}
