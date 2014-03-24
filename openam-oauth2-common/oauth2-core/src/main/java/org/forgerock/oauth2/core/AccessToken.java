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

    public AccessToken(final CoreToken token) {
        this.token = token;
    }

    public CoreToken getCoreToken() {
        return token;
    }

    public Map<String, Object> toMap() {
        final Map<String, Object> tokenMap = token.convertToMap();
        tokenMap.putAll(extraData);
        return tokenMap;
    }

    public void addExtraData(final Map<String, Object> data) {
        extraData.putAll(data);
    }

    public void add(final String key, final String value) {
        extraData.put(key, value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AccessToken that = (AccessToken) o;

        if (extraData != null ? !extraData.equals(that.extraData) : that.extraData != null) return false;
        if (token != null ? !token.equals(that.token) : that.token != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = token != null ? token.hashCode() : 0;
        result = 31 * result + (extraData != null ? extraData.hashCode() : 0);
        return result;
    }
}
