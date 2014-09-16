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

package org.forgerock.openam.authentication.modules.common.mapping;

import com.sun.identity.shared.debug.Debug;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

public class MappingUtils {
    private static Debug debug = Debug.getInstance("amAuth");
    private static final String EQUALS = "=";

    /**
     * This method parses out the local_attribute=source_attributes as they are encapsulated in the authN module
     * configurations into a more usable, Map<String, String> format.
     * @param configuredMappings The set of local=source mappings.
     * @return The derived Map instance.
     */
    public static Map<String, String> parseMappings(Set<String> configuredMappings) {
        Map<String, String> parsedMappings = new HashMap<String, String>();
        for (String mapping : configuredMappings) {
            if (mapping.indexOf(EQUALS) == -1) {
                continue;
            }
            StringTokenizer tokenizer = new StringTokenizer(mapping, EQUALS);
            final String key = tokenizer.nextToken();
            final String value = tokenizer.nextToken();
            /*
            The ldap_attr=spource_attr mapping is user-generated, so I want to warn about duplicate entries. In a
            HashMap, repeated insertion of the same key will over-write previous entries, but I want to warn about
            duplicate entries, and will persist the first entry.
             */
            if (!parsedMappings.containsKey(key)) {
                parsedMappings.put(key, value);
            } else {
                if (debug.warningEnabled()) {
                    debug.warning("In MappingUtils.parseMappings, the user-entered attribute mappings contain " +
                            "duplicate entries. The first entry will be preserved: " + configuredMappings);
                }
            }
        }
        if (parsedMappings.isEmpty()) {
            throw new IllegalArgumentException("The mapping Set does not contain any mappings in format " +
                    "local_attribute=source_attribute.");
        }
        return Collections.unmodifiableMap(parsedMappings);
    }

}
