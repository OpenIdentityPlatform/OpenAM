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

package org.forgerock.openam.uma.extensions;

import org.forgerock.json.JsonPointer;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openam.uma.UmaPolicy;
import org.forgerock.util.query.QueryFilter;

/**
 * Extension filter that will be called before a resource is shared, after a
 * resource is shared, before a shared resource is modified and on a resource
 * no longer being shared.
 *
 * <p>Implementations of this interface can use the Guice setter based injection.</p>
 *
 * @since 13.0.0
 */
public interface ResourceDelegationFilter extends Comparable<ResourceDelegationFilter> {

    /**
     * Invoked before a resource "share" is created.
     *
     * <p>Changes to the {@literal umaPolicy} will be persisted.</p>
     *
     * @param umaPolicy The UMA policy representation.
     * @throws ResourceException If the resource "share" is not to be shared.
     */
    void beforeResourceShared(UmaPolicy umaPolicy) throws ResourceException;

    /**
     * Invoked after a resource "share" is created.
     *
     * <p>Changes to the {@literal umaPolicy} will <strong>not</strong> be persisted.</p>
     *
     * @param umaPolicy The UMA policy representation.
     */
    void afterResourceShared(UmaPolicy umaPolicy);

    /**
     * Invoked before a resource "share" is modified.
     *
     * <p>Changes to the {@literal updatedUmaPolicy} will be persisted.</p>
     *
     * @param currentUmaPolicy The current UMA policy representation.
     * @param updatedUmaPolicy The updated UMA policy representation.
     * @throws ResourceException If the resource "share" is not to be modified.
     */
    void beforeResourceSharedModification(UmaPolicy currentUmaPolicy, UmaPolicy updatedUmaPolicy)
            throws ResourceException;

    /**
     * Invoked before a resource "share" is deleted.
     *
     * @param umaPolicy The UMA policy representation.
     * @throws ResourceException If the resource "share" is not to be deleted.
     */
    void onResourceSharedDeletion(UmaPolicy umaPolicy) throws ResourceException;

    /**
     * Invoked before a users, owned and shared with, resource sets a queried.
     *
     * @param userId The id of the user making the query request.
     * @param queryFilter The incoming request query filter.
     * @return A {@code QueryFilter} which will be used to return a users resource sets.
     */
    QueryFilter<JsonPointer> beforeQueryResourceSets(String userId, QueryFilter<JsonPointer> queryFilter);
}
