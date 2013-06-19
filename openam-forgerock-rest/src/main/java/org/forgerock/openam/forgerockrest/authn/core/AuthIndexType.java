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

package org.forgerock.openam.forgerockrest.authn.core;

import com.sun.identity.authentication.AuthContext;

/**
 * Enum for AuthContext.IndexTypes.
 */
public enum AuthIndexType {

    /** The empty AuthIndexType.  */
    NONE(null),
    /** The User AuthIndexType. */
    USER(AuthContext.IndexType.USER),
    /** The Role AuthIndexType. */
    ROLE(AuthContext.IndexType.ROLE),
    /** The Sevice AuthIndexType. */
    SERVICE(AuthContext.IndexType.SERVICE),
    /** The Level AuthIndexType. */
    LEVEL(AuthContext.IndexType.LEVEL),
    /** The Module AuthIndexType. */
    MODULE(AuthContext.IndexType.MODULE_INSTANCE),
    /** The Resource AuthIndexType. */
    RESOURCE(AuthContext.IndexType.RESOURCE),
    /** The Composite AuthIndexType. */
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
     * Gets the AuthIndexType Enum when either given the corresponding AuthContext.IndexType String, or the
     * AuthIndexType Enum as string.
     *
     * @param indexTypeString The AuthContext.IndexType String or AuthIndexType enum as a String.
     * @return The AuthIndexType.
     */
    public static AuthIndexType getAuthIndexType(String indexTypeString) {
        if (indexTypeString == null) {
            return NONE;
        } else {
            for (AuthIndexType authIndexType : AuthIndexType.values()) {
                if (AuthIndexType.NONE.equals(authIndexType)) {
                    continue;
                }
                if (indexTypeString.toLowerCase().equals(authIndexType.getIndexType().toString())) {
                    return authIndexType;
                }
            }
        }
        return AuthIndexType.valueOf(indexTypeString.toUpperCase());
    }
}
