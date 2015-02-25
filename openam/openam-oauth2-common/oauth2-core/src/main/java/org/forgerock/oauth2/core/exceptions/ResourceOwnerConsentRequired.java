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

package org.forgerock.oauth2.core.exceptions;

import java.util.Set;

/**
 * Thrown when the resource owner's consent is required before the authorization can be granted to a OAuth2 client.
 *
 * @since 12.0.0
 */
public class ResourceOwnerConsentRequired extends Exception {

    private final String clientName;
    private final String clientDescription;
    private final Set<String> scopeDesciptions;

    /**
     * Constructs a new ResourceOwnerConsentRequired instance with the specified client name, description and scope
     * descriptions.
     *
     * @param clientName The display name of the client.
     * @param clientDescription The display description of the client.
     * @param scopeDescriptions The display descriptions of the requested scopes.
     */
    public ResourceOwnerConsentRequired(final String clientName, final String clientDescription,
            final Set<String> scopeDescriptions) {
        this.clientName = clientName;
        this.clientDescription = clientDescription;
        this.scopeDesciptions = scopeDescriptions;
    }

    /**
     * Gets the client's name.
     *
     * @return The name of the client.
     */
    public String getClientName() {
        return clientName;
    }

    /**
     * Gets the client's description.
     *
     * @return The description of the client.
     */
    public String getClientDescription() {
        return clientDescription;
    }

    /**
     * Gets the scope descriptions.
     *
     * @return The desciption of the scopes.
     */
    public Set<String> getScopeDescriptions() {
        return scopeDesciptions;
    }
}
