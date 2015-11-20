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

package org.forgerock.openam.sm.config;

import java.util.Objects;

/**
 * Key used to assist with indexing in caching.
 *
 * @since 13.0.0
 */
final class CacheKey {

    private final String source;
    private final String realm;

    private CacheKey(String source, String realm) {
        this.source = source;
        this.realm = realm;
    }

    String getSource() {
        return source;
    }

    String getRealm() {
        return realm;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof CacheKey)) {
            return false;
        }
        CacheKey cacheKey = (CacheKey) other;
        return Objects.equals(source, cacheKey.source) &&
                Objects.equals(realm, cacheKey.realm);
    }

    @Override
    public int hashCode() {
        return Objects.hash(source, realm);
    }

    static CacheKey newInstance(String source, String realm) {
        return new CacheKey(source, realm);
    }

}
