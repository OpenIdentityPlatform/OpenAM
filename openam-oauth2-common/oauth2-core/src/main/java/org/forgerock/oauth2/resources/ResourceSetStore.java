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

package org.forgerock.oauth2.resources;

import java.util.Set;

import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.oauth2.core.exceptions.BadRequestException;
import org.forgerock.oauth2.core.exceptions.NotFoundException;
import org.forgerock.oauth2.core.exceptions.ServerException;
import org.forgerock.util.query.QueryFilter;

/**
 * Allows the storage of {@code ResourceSetDescription} objects in a store.
 *
 * @since 13.0.0
 */
public interface ResourceSetStore {

    /**
     * Create a {@code ResourceSetDescription}. The id field will be populated with the resulting identifier.
     *
     * @param request The OAuth2Request.
     * @param resourceSetDescription The resource set description being created.
     * @throws ServerException When an error occurs during creation.
     * @throws NotFoundException If the OAuth2ProviderSettings could not be found.
     */
    void create(OAuth2Request request, ResourceSetDescription resourceSetDescription) throws ServerException,
            NotFoundException, BadRequestException;

    /**
     * Reads a {@code ResourceSetDescription} out of the store using its OpenAM Unique ID.
     *
     * @param resourceSetId The resource set ID.
     * @param resourceOwnerId The resource owner id.
     * @return The {@code ResourceSetDescription}.
     * @throws NotFoundException If the resource set is not found.
     * @throws ServerException When the resource set description cannot be loaded.
     */
    ResourceSetDescription read(String resourceSetId, String resourceOwnerId) throws NotFoundException, ServerException;

    /**
     * Update a given {@code ResourceSetDescription} instance.
     *
     * @param resourceSetDescription The resource set description being updated.
     * @throws ServerException When the {@code ResourceSetDescription} cannot be found, or an error occurs during
     * update.
     */
    void update(ResourceSetDescription resourceSetDescription) throws NotFoundException, ServerException;

    /**
     * Remove a {@code ResourceSetDescription} with the given ID from the store.
     *
     * @param resourceSetId The identifier of the {@code ResourceSetDescription} being removed.
     * @param resourceOwnerId The resource owner id.
     * @throws ServerException When an error occurs during removal.
     */
    void delete(String resourceSetId, String resourceOwnerId) throws NotFoundException, ServerException;

    /**
     * Query the store for {@code ResourceSetDescription} instances.
     *
     * @param query The criteria of the query.
     * @return A set of all matching resource set descriptions.
     * @throws ServerException When an error occurs when querying the store.
     */
    Set<ResourceSetDescription> query(QueryFilter<String> query) throws ServerException;

    /**
     * Different ways to combine criteria in a filter.
     *
     * @since 13.0.0
     */
    public static enum FilterType {
        AND, OR
    }
}
