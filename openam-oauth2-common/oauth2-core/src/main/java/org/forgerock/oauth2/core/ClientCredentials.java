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

package org.forgerock.oauth2.core;

import java.util.Arrays;

/**
 * Encapsulates the Client's Credentials as specified when registered with the OAuth2 Provider.
 *
 * @since 12.0.0
 */
public final class ClientCredentials {

    private final String clientId;
    private final char[] clientSecret;

    /**
     * Constructs a new ClientCredentials instance.
     *
     * @param clientId The client's identifier.
     * @param clientSecret The client's secret.
     */
    public ClientCredentials(final String clientId, final char[] clientSecret) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    /**
     * Gets the client's identifier.
     *
     * @return The client's identifier.
     */
    public String getClientId() {
        return clientId;
    }

    /**
     * The client's secret.
     *
     * @return The client's secret.
     */
    public char[] getClientSecret() {
        return clientSecret;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ClientCredentials that = (ClientCredentials) o;

        if (!clientId.equals(that.clientId)) {
            return false;
        }
        if (!Arrays.equals(clientSecret, that.clientSecret)) {
            return false;
        }

        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        int result = clientId.hashCode();
        result = 31 * result + Arrays.hashCode(clientSecret);
        return result;
    }
}
