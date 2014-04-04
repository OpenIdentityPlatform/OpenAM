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

import org.forgerock.oauth2.core.exceptions.InvalidRequestException;

import java.util.Map;
import java.util.Set;

/**
 * Interface to be implemented by the OAuth2 Provider implementation which will contain configuration information
 * that the OAuth2 process will use whilst processing OAuth2 requests.
 *
 * @since 12.0.0
 */
public interface OAuth2ProviderSettings {

    /**
     * Whether refresh tokens should be issued when an access token is issued.
     *
     * @return {@code true} if refresh tokens should be issued with access tokens.
     */
    boolean issueRefreshTokens();

    /**
     * Gets the allowed response types as configured for the OAuth2 Provider.
     *
     * @return A {@code Map<String, String>} of the allowed response types with their implementation class name.
     * @throws InvalidRequestException If a problem occurs whilst retrieving the response types map.
     */
    Map<String, String> getAllowedResponseTypes() throws InvalidRequestException;

    /**
     * Whether the resource owner has requested that their consent for granting a specific scope for a specific client.
     *
     * @param resourceOwnerId The resource owner's identifier.
     * @param clientId The client's identifier.
     * @param scope The requested scope.
     * @param context A {@code Map<String, Object>} containing OAuth2 Provider implementation specific context
     *                information.
     * @return {@code true} if the resource owner has previously saved their consent for the given client and the
     * exact scope.
     */
    boolean isConsentSaved(final String resourceOwnerId, final String clientId, final Set<String> scope,
            final Map<String, Object> context);

    /**
     * Saves the resource owner's consent for the specified client and scope for subsequent authorization requests.
     *
     * @param resourceOwnerId The resource owner's identifier.
     * @param clientId The client's identifier.
     * @param scope The requested scope.
     * @param context A {@code Map<String, Object>} containing OAuth2 Provider implementation specific context
     *                information.
     */
    void saveConsent(final String resourceOwnerId, final String clientId, final Set<String> scope,
            final Map<String, Object> context);

    /**
     * Gets any additional data the OAuth2 Provider wishes to be added to authorization code/access tokens returned
     * from the authorize endpoint.
     *
     * @param userConsentResponse The UserConsentResponse instance.
     * @return A {@code Map<String, Object>} of additional token data.
     */
    Map<String, Object> addAdditionalTokenData(final UserConsentResponse userConsentResponse);
}
