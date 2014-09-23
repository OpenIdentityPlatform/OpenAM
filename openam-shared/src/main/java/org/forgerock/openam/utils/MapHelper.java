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
 * Copyright 2014 ForgeRock Inc.
 */
package org.forgerock.openam.utils;

import com.sun.identity.common.CaseInsensitiveHashMap;
import com.sun.identity.common.CaseInsensitiveHashSet;
import com.sun.identity.shared.debug.Debug;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.Set;

public class MapHelper {

    private static final Debug DEBUG = Debug.getInstance("workflow");

    /**
     * Read a properties file into a map of strings to sets of strings.  Lines whose first non whitespace character
     * is a hash are ignored as comments, while lines which do not contain an assignment are just ignored.
     *
     * @param fileName The property file name
     * @return A map of strings to sets of strings
     * @throws IOException if there is an IO problem when reading the file (like it doesn't exist, etc.)
     */
    public static Map<String, Set<String>> readMap(String fileName) throws IOException {
        InputStream is = null;

        try {
            is = MapHelper.class.getResourceAsStream(fileName);

            // No properties file
            if (is == null) {
                DEBUG.warning("Could not locate properties file " + fileName);
                return new CaseInsensitiveHashMap();
            }
            return readMap(is);
        } finally {
            IOUtils.closeIfNotNull(is);
        }
    }

    /**
     * Read a stream into a map of strings to sets of strings.  Lines whose first non whitespace character
     * is a hash are ignored as comments, while lines which do not contain an assignment are just ignored.
     *
     * @param is A stream to read properties from.
     * @return A map of strings to sets of strings
     * @throws IOException if there is an IO problem when reading the file (like it doesn't exist, etc.)
     */
    public static Map<String,Set<String>> readMap(InputStream is) throws IOException {

        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        try {
            Map<String, Set<String>> result = new CaseInsensitiveHashMap();
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();

                // ignore blank lines and comments
                if (line.length() == 0 || line.startsWith("#")) {
                    continue;
                }

                int idx = line.indexOf('=');
                if (idx != -1) {
                    String key = line.substring(0, idx);
                    String value = line.substring(idx + 1);
                    if (!value.isEmpty()) {
                        Set<String> values = result.get(key);
                        if (values == null) {
                            values = new CaseInsensitiveHashSet(1);
                        }
                        values.add(value);
                        result.put(key, values);
                    }
                }
            }
            return result;
        } finally {
            IOUtils.closeIfNotNull(br);
        }
    }
}
