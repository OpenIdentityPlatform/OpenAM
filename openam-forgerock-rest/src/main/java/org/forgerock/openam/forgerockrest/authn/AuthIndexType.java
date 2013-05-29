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
 * Copyright 2013 ForgeRock Inc.
 */

package org.forgerock.openam.forgerockrest.authn;

import com.sun.identity.authentication.AuthContext;

/**
 * Enum for AuthContext.IndexTypes.
 */
public enum AuthIndexType {

    USER(AuthContext.IndexType.USER),
    ROLE(AuthContext.IndexType.ROLE),
    SERVICE(AuthContext.IndexType.SERVICE),
    LEVEL(AuthContext.IndexType.LEVEL),
    MODULE(AuthContext.IndexType.MODULE_INSTANCE),
    RESOURCE(AuthContext.IndexType.RESOURCE),
    COMPOSITE(AuthContext.IndexType.COMPOSITE_ADVICE);

    private final AuthContext.IndexType indexType;

    /**
     * Creates the Enum.
     *
     * @param indexType The AuthContext.IndexType for the Enum.
     */
    private AuthIndexType(AuthContext.IndexType indexType) {
        this.indexType = indexType;
    }

    /**
     * Gets the AuthContext.IndexType for the enum.
     *
     * @return The corresponding AuthContext.IndexType.
     */
    public AuthContext.IndexType getIndexType() {
        return indexType;
    }

    /**
     * Gets the AuthIndexType Enum for the corresponding AuthContext.IndexType String.
     *
     * @param indexTypeString The AuthContext.IndexType String.
     * @return The AuthIndexType.
     */
    public static AuthIndexType getAuthIndexType(String indexTypeString) {
        for (AuthIndexType authIndexType : AuthIndexType.values()) {
            if (authIndexType.getIndexType().toString().equals(indexTypeString.toLowerCase())) {
                return authIndexType;
            }
        }

        throw new IllegalArgumentException("Unknown Authentication Index Type, " + indexTypeString);
    }
}
