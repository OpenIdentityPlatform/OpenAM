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

import org.forgerock.oauth2.core.exceptions.InvalidClientException;

import java.util.Map;

/**
 * Handles the authentication of the OAuth2 Client.
 * <br/>
 * Root interface to be implemented to provide client authentication based on the client registration.
 *
 * @since 12.0.0
 */
public interface ClientAuthenticator {

    /**
     * Authenticates the OAuth2 Client.
     * <br/>
     * If authentication succeeds, then the authenticated client is returned, else if authentication fails
     * a InvalidClientException will be thrown.
     *
     * @param clientCredentials An instance of the ClientCredentials which contain the clients credentials.
     * @param context A {@code Map<String, Object>} containing OAuth2 Provider implementation specific context
     *                information.
     * @return The authenticated Client.
     * @throws InvalidClientException org.forgerock.oauth2.core.exceptions.InvalidClientException authenticating the
     * Client fails.
     */
    ClientRegistration authenticate(final ClientCredentials clientCredentials, final Map<String, Object> context)
            throws InvalidClientException;
}
