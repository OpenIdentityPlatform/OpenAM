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

import java.net.URI;
import java.util.Map;
import java.util.Set;

/**
 * Represents a clients registration details on the OAuth2 Provider.
 *
 * @since 12.0.0
 */
public interface ClientRegistration {

    /**
     * Whether the client is registered as a confidential or public client.
     *
     * @return {@code true} if the client is confidential.
     */
    boolean isConfidential();

    /**
     * The client's identifier.
     *
     * @return The client's identifier.
     */
    String getClientId();

    /**
     * The scopes that have been configured as allowed for the client.
     *
     * @return The client's allowed scopes.
     */
    Set<String> getAllowedScopes();

    /**
     * A {@code Map<String, String>} of the client's allowed scopes mapped to their descriptions in the specified
     * locale.
     *
     * @param locale The locale the scope descriptions should be returned in.
     * @return The Client's allowed scopes and their description.
     */
    Map<String, String> getAllowedScopeDescriptions(final String locale);

    /**
     * The scopes that have been configured as default for the client.
     *
     * @return The client's default scopes.
     */
    Set<String> getDefaultScopes();

    /**
     * The redirect uris that have been registered for the client.
     *
     * @return A {@code Set<URI>} of client redirect uris
     */
    Set<URI> getRedirectUris();

    /**
     * The response types that have been configured as allowed for the client.
     *
     * @return The client's allowed response types.
     */
    Set<String> getAllowedResponseTypes();

    /**
     * The displayable name of the client.
     *
     * @param locale The locale the display name should be returned in.
     * @return The client's display name.
     */
    String getDisplayName(final String locale);

    /**
     * The displayable description of the client.
     *
     * @param locale The locale the display description should be returned in.
     * @return The client's display description.
     */
    String getDisplayDescription(final String locale);

    /**
     * The type of access token the client registration has requested, (i.e. Bearer).
     *
     * @return The type of access token.
     */
    String getAccessTokenType();
}
