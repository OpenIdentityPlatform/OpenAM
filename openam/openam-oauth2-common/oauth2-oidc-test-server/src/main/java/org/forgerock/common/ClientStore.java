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

package org.forgerock.common;

import org.forgerock.openidconnect.Client;

import javax.inject.Singleton;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @since 12.0.0
 */
@Singleton
public class ClientStore {

    private final Map<String, Client> clients = new ConcurrentHashMap<String, Client>();

    {
        clients.put("MyApp", new Client(
                "MyApp",
                "Confidential",
                Collections.singletonList("http://localhost:8080/"),
                Arrays.asList(new String[]{"openid"}),
                Arrays.asList(new String[]{}),
                Arrays.asList(new String[]{"en|MyApp Display Name"}),
                Arrays.asList(new String[]{"en|MyApp Display Description"}),
                "MyApp Name",
                "",
                "HS256",
                null,
                null,
                null,
                null,
                "cangetin",
                Arrays.asList(new String[]{"code", "id_token", "token"}),
                null));
    }

    public void create(Client client) {
        clients.put(client.getClientID(), client);
    }

    public Client get(String clientId) {
        return clients.get(clientId);
    }

    public void update(Client client) {
        clients.put(client.getClientID(), client);
    }

    public void delete(String clientId) {
        clients.remove(clientId);
    }
}
