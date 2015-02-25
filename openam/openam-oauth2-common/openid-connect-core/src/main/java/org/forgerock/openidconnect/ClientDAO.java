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
 * information: "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright 2014 ForgeRock AS.
 */

package org.forgerock.openidconnect;

import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.oauth2.core.exceptions.UnauthorizedClientException;
import org.forgerock.openidconnect.exceptions.InvalidClientMetadata;

/**
 * Interface to do basic CRUD operations on a OAuth2Client.
 *
 * @since 12.0.0
 */
public interface ClientDAO {

    /**
     * Stores a client to a storage system.
     *
     * @param client The client to store
     * @throws InvalidClientMetadata If the client's registration details are invalid.
     */
    public void create(Client client, OAuth2Request request) throws InvalidClientMetadata;

    /**
     * Reads a client from a storage system.
     *
     * @param clientId The client id of the client to retrieve.
     * @return A Client read from the storage system.
     * @throws UnauthorizedClientException If the client's authorization fails.
     */
    public Client read(String clientId, OAuth2Request request) throws UnauthorizedClientException;

    /**
     * Updates a client already stored.
     *
     * @param client The updated client to use to update the storage.
     * @throws InvalidClientMetadata If the client's registration details are invalid.
     * @throws UnauthorizedClientException If the client's authorization fails.
     */
    public void update(Client client, OAuth2Request request) throws InvalidClientMetadata, UnauthorizedClientException;

    /**
     * Delete a client from the storage system.
     *
     * @param clientId The client id of the client to delete.
     * @throws UnauthorizedClientException If the client's authorization fails.
     */
    public void delete(String clientId, OAuth2Request request) throws UnauthorizedClientException;
}
