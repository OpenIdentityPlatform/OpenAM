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

package org.forgerock.openidconnect;

import org.forgerock.common.ClientStore;
import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.oauth2.core.exceptions.UnauthorizedClientException;
import org.forgerock.openidconnect.exceptions.InvalidClientMetadata;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @since 12.0.0
 */
@Singleton
public class ClientDAOImpl implements ClientDAO {

    private final ClientStore clientStore;

    @Inject
    public ClientDAOImpl(final ClientStore clientStore) {
        this.clientStore = clientStore;
    }

    public void create(Client client, OAuth2Request request) throws InvalidClientMetadata {
        clientStore.create(client);
    }

    public Client read(String clientId, OAuth2Request request) throws UnauthorizedClientException {
        return clientStore.get(clientId);
    }

    public void update(Client client, OAuth2Request request) throws InvalidClientMetadata, UnauthorizedClientException {
        clientStore.update(client);
    }

    public void delete(String clientId, OAuth2Request request) throws UnauthorizedClientException {
        clientStore.delete(clientId);
    }
}
