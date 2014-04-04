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

import java.util.Map;

/**
 * Represents a resource owner granting authorization for a client with a specific scope.
 * <br/>
 * May contain multiple tokens to be sent to the client from the 'authorize' endpoint based on the response types
 * the client has requested.
 *
 * @since 12.0.0
 */
public class Authorization {

    private final Map<String, String> tokens;
    private final boolean fragment;

    /**
     * Constructs a new Authorization object.
     *
     * @param tokens The Map of tokens.
     * @param fragment {@code true} if the tokens should be returned in the fragment of the URI.
     */
    Authorization(final Map<String, String> tokens, final boolean fragment) {
        this.tokens = tokens;
        this.fragment = fragment;
    }

    /**
     * Gets the tokens.
     *
     * @return The tokens.
     */
    public Map<String, String> getTokens() {
        return tokens;
    }

    /**
     * Whether the tokens should be returned in the fragment of the URI.
     *
     * @return {@code true} if the tokens should be returned in the fragment of the URI.
     */
    public boolean isFragment() {
        return fragment;
    }
}
