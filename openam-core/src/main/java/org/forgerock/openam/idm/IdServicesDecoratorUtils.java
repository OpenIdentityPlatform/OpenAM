/*
 * Copyright 2014 ForgeRock, AS.
 *
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
 */

package org.forgerock.openam.idm;

import com.iplanet.am.sdk.AMHashMap;

import java.util.Map;

/**
 * Common utility methods for IdServices decorators.
 *
 * @since 12.0.0
 */
public final class IdServicesDecoratorUtils {
    private IdServicesDecoratorUtils() {
        throw new UnsupportedOperationException("utility class");
    }

    /**
     * Converts all keys in the given map to lower case.
     *
     * @param attrs the attributes to convert to lower case.
     * @return a copy of the given attributes with all keys converted to lower case, or null if the input is null.
     */
    public static Map toLowerCaseKeys(Map attrs) {
        if (attrs != null) {
            AMHashMap lowerCaseMap = new AMHashMap(isBinary(attrs));
            for (Object key : attrs.keySet()) {
                lowerCaseMap.put(key == null ? null : key.toString().toLowerCase(), attrs.get(key));
            }
            return lowerCaseMap;
        }
        return attrs;
    }

    /**
     * Determines whether the given map contains binary values or not.
     *
     * @param map the map to check
     * @return true if the map is known to contain binary values, otherwise false.
     */
    private static boolean isBinary(Map map) {
        if (map instanceof AMHashMap) {
            return ((AMHashMap) map).isByteValues();
        } else if (map != null && !map.isEmpty()) {
            return map.values().iterator().next() instanceof byte[][];
        }
        return false;
    }
}
