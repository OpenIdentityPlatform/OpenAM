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

import java.util.HashMap;
import java.util.Map;

/**
 * Represents an OAuth2 Access Token.
 *
 * @since 12.0.0
 */
public class AccessToken {

    private final CoreToken token;
    private final Map<String, Object> extraData = new HashMap<String, Object>();

    /**
     * Constructs a new AccessToken backed by the specified CoreToken.
     *
     * @param token The backing CoreToken.
     */
    public AccessToken(final CoreToken token) {
        this.token = token;
    }

    /**
     * Gets the CoreToken backing this AccessToken.
     *
     * @return The CoreToken.
     */
    public CoreToken getCoreToken() {
        return token;
    }

    /**
     * Converts this AccessToken into a Map.
     *
     * @return A {@code Map<String, Object>} of the AccessToken.
     */
    public Map<String, Object> toMap() {
        final Map<String, Object> tokenMap = token.convertToMap();
        tokenMap.putAll(extraData);
        return tokenMap;
    }

    /**
     * Adds the specified map to the AccessToken.
     *
     * @param data The additional data to add.
     */
    public void addExtraData(final Map<String, Object> data) {
        extraData.putAll(data);
    }

    /**
     * Adds the specified key, value pair to the AccessToken.
     *
     * @param key The key.
     * @param value The value.
     */
    public void add(final String key, final String value) {
        extraData.put(key, value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        AccessToken that = (AccessToken) o;

        if (!extraData.equals(that.extraData)) {
            return false;
        }
        if (token != null ? !token.equals(that.token) : that.token != null) {
            return false;
        }

        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        int result = token != null ? token.hashCode() : 0;
        result = 31 * result + extraData.hashCode();
        return result;
    }
}
