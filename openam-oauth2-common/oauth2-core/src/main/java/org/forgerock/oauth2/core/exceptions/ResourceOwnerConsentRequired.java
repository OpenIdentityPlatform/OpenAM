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
 * Copyright 2014-2015 ForgeRock AS.
 */

package org.forgerock.oauth2.core.exceptions;

import java.util.Map;
import java.util.Set;

import org.forgerock.oauth2.core.UserInfoClaims;

/**
 * Thrown when the resource owner's consent is required before the authorization can be granted to a OAuth2 client.
 *
 * @since 12.0.0
 */
public class ResourceOwnerConsentRequired extends Exception {

    private final String clientName;
    private final String clientDescription;
    private final Map<String, String> scopeDesciptions;
    private final String userDisplayName;
    private final Map<String, String> claimDesciptions;
    private final UserInfoClaims claims;

    /**
     * Constructs a new ResourceOwnerConsentRequired instance with the specified client name, description and scope
     * descriptions.
     *
     * @param clientName The display name of the client.
     * @param clientDescription The display description of the client.
     * @param scopeDescriptions The display descriptions of the requested scopes.
     * @param claimDescriptions The display descriptions of the provided claims.
     * @param claims The claims being provided.
     * @param userDisplayName The displayable name of the user, if it can be deduced.
     */
    public ResourceOwnerConsentRequired(String clientName, String clientDescription,
            Map<String, String> scopeDescriptions, Map<String, String> claimDescriptions, UserInfoClaims claims,
            String userDisplayName) {
        this.clientName = clientName;
        this.clientDescription = clientDescription;
        this.scopeDesciptions = scopeDescriptions;
        this.claimDesciptions = claimDescriptions;
        this.claims = claims;
        this.userDisplayName = userDisplayName;
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
     * Gets the resource owner's display name.
     *
     * @return The name of the resource owner.
     */
    public String getUserDisplayName() {
        return userDisplayName;
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
    public Map<String, String> getScopeDescriptions() {
        return scopeDesciptions;
    }

    /**
     * Gets the claim descriptions.
     *
     * @return The desciption of the claims.
     */
    public Map<String, String> getClaimDescriptions() {
        return claimDesciptions;
    }

    /**
     * Gets the claim values.
     *
     * @return The values of the claims.
     */
    public UserInfoClaims getClaims() {
        return claims;
    }
}
