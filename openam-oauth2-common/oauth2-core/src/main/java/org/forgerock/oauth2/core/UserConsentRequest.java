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

import java.util.Set;

/**
 * Represents a request to get user consent to gain authorization to access a particular resource.
 *
 * @since 12.0.0
 */
public final class UserConsentRequest {

    private final boolean consentRequired;
    private final Set<String> scope;
    private final String displayName;
    private final String displayDescription;
    private final Set<String> scopeDescription;

    /**
     * Constructs a new UserConsentRequest.
     *
     * @param consentRequired {@code true} if the user's consent is required.
     */
    UserConsentRequest(final boolean consentRequired) {
        this(consentRequired, null, null, null, null);
    }

    /**
     * Constructs a new UserConsentRequest.
     *
     * @param consentRequired {@code true} if the user's consent is required.
     * @param scope The scope.
     * @param displayName The client's display name.
     * @param displayDescription The client's display description.
     * @param scopeDescription The scope description.
     */
    UserConsentRequest(final boolean consentRequired, final Set<String> scope, final String displayName,
            final String displayDescription, final Set<String> scopeDescription) {
        this.consentRequired = consentRequired;
        this.scope = scope;
        this.displayName = displayName;
        this.displayDescription = displayDescription;
        this.scopeDescription = scopeDescription;
    }

    /**
     * Whether the user's consent is required.
     *
     * @return {@code true} if the user's consent is required.
     */
    public boolean isConsentRequired() {
        return consentRequired;
    }

    /**
     * Gets the scope.
     *
     * @return The scope.
     */
    public Set<String> getScope() {
        return scope;
    }

    /**
     * Gets the client's display name.
     *
     * @return The client's display name.
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Gets the client's display description.
     *
     * @return The client's display description.
     */
    public String getDisplayDescription() {
        return displayDescription;
    }

    /**
     * Gets the scope description.
     *
     * @return The scope description.
     */
    public Set<String> getScopeDescription() {
        return scopeDescription;
    }
}
