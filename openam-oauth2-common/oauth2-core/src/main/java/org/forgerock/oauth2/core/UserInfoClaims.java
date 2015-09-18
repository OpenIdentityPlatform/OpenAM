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

package org.forgerock.oauth2.core;

import java.util.List;
import java.util.Map;

/**
 * Simple bean that contains the values of claims, and the scopes that
 * provisioned them (if any).
 * @since 13.0.0
 */
public class UserInfoClaims {

    private final Map<String, Object> values;
    private final Map<String, List<String>> compositeScopes;

    public UserInfoClaims(Map<String, Object> values, Map<String, List<String>> compositeScopes) {
        this.values = values;
        this.compositeScopes = compositeScopes;
    }

    /**
     * Gets the values of the claims for the user information.
     * @return A map of claim names to values, which may be {@code String}s or {@code Map}s.
     */
    public Map<String, Object> getValues() {
        return values;
    }

    /**
     * For claims that are provisioned by composite scopes, this will contain the mapping
     * of scope name to a list of claim names.
     * @return A map of scope names to lists of claim names.
     */
    public Map<String, List<String>> getCompositeScopes() {
        return compositeScopes;
    }
}
