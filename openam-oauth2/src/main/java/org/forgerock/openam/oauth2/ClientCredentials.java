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
package org.forgerock.openam.oauth2;

import java.util.Arrays;

/**
 * Models the client's credentials
 *
 * @since 12.0.0
 */
public final class ClientCredentials {

    private final String clientId;
    private final char[] clientSecret;
    private final boolean isAuthenticated;
    private final boolean basicAuth;

    /**
     * Constructs a new ClientCredentials instance.
     *
     * @param clientId The client's identifier.
     * @param clientSecret The client's secret.
     * @param isAuthenticated If the process of getting the client credentials has authenticated the client. i.e.
     *                        Jwt assertion.
     * @param basicAuth Whether the Client's credentials where sent using the Basic Auth header.
     */
    public ClientCredentials(final String clientId, final char[] clientSecret, final boolean isAuthenticated,
                              final boolean basicAuth) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.isAuthenticated = isAuthenticated;
        this.basicAuth = basicAuth;
    }

    /**
     * True if the client uses basic auth, false otherwise.
     */
    public boolean usesBasicAuth() {
        return basicAuth;
    }

    /**
     * True if the client is authenticated, false otherwise.
     */
    public boolean isAuthenticated() {
        return isAuthenticated;
    }

    /**
     * Gets the client secret.
     */
    public char[] getClientSecret() {
        return clientSecret;
    }

    /**
     * Gets the client ID.
     */
    public String getClientId() {
        return clientId;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ClientCredentials that = (ClientCredentials) o;

        if (basicAuth != that.basicAuth) return false;
        if (!clientId.equals(that.clientId)) return false;
        if (!Arrays.equals(clientSecret, that.clientSecret)) return false;

        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        int result = clientId.hashCode();
        result = 31 * result + Arrays.hashCode(clientSecret);
        result = 31 * result + (basicAuth ? 1 : 0);
        return result;
    }
}
