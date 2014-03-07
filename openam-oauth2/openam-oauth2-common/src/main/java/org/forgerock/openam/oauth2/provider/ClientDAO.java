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
 * Copyright 2014 ForgeRock AS. All rights reserved.
 */

package org.forgerock.openam.oauth2.provider;

import org.forgerock.openam.oauth2.exceptions.OAuthProblemException;
import org.forgerock.openam.oauth2.model.Client;

/**
 * Interface to do basic CRUD operations on a OAuth2Client.
 */
public interface ClientDAO {
    /**
     * Stores a client to a storage system.
     * @param client
     *          The client to store
     * @throws OAuthProblemException
     */
    public void create(Client client) throws OAuthProblemException;

    /**
     * Reads a client from a storage system.
     * @param clientId The client_id of the client to retrieve.
     * @return A Client read from the storage system.
     * @throws OAuthProblemException
     */
    public Client read(String clientId) throws OAuthProblemException;

    /**
     * Updates a client already stored.
     * @param client The updated client to use to update the storage.
     * @throws OAuthProblemException
     */
    public void update(Client client) throws OAuthProblemException;

    /**
     * Delete a client from the storage system.
     * @param client The client to delete.
     * @throws OAuthProblemException
     */
    public void delete(Client client) throws OAuthProblemException;

    /**
     * Delete a client from the storage system.
     * @param clientId The client_id of the client to delete.
     * @throws OAuthProblemException
     */
    public void delete(String clientId) throws OAuthProblemException;
}
