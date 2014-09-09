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

package com.sun.identity.common.configuration;

import org.forgerock.util.Pair;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parse values out of a map (strings, basically), which come to us in the form:
 *
 * [google]=AuthChainSocialGoogle
 * [twitter]=AuthChainSocialTwitter
 * [flickr]=AuthChainSocialFlickr
 *
 * and break these into Pair objects containing:
 *
 * (google,AuthChainSocialGoogle)
 * (twitter,AuthChainSocialTwitter)
 * (flickr,AuthChainSocialTwitter)
 *
 * Note that the regular expressions are COPIED from MapValueValidator.  I would liked to have used the ones defined
 * there, but they were not quite what I needed (I have added two groups).  I considered:
 * - using the original values and altering them in code here
 * - altering the originals (by adding two groups, I don't think I would change any behaviour, but I can't be sure)
 * anyway, I thought the first too extreme and the second too dangerous
 */
public class MapValueParser {

    /**
     * Please refer to KEY_WITH_NO_BRACKETS in MapValueValidator, where all the heavy lifting has
     * already been done.
     */
    private static final String regExp = "\\s*\\[\\s*([[\\S]&&[^\\[]&&[^\\]]]+[[^\\[]&&[^\\]]]*)\\s*\\]\\s*=(.*)";

    private static final Pattern pattern = Pattern.compile(regExp);

    /**
     * Pass in a set of name=value pairs to this function and get back a hashmap of values, keyed by name.
     *
     * @param entries The set of strings
     * @return a hashmap of values, keyed by name
     */
    public Map<String, String> parse(Set<String> entries) {

        Map<String, String> result = new HashMap<String, String>();

        if (entries != null) {
            for (String entry : entries) {
                Pair<String, String> pair = parse(entry);
                if (pair != null) {
                    result.put(pair.getFirst(), pair.getSecond());
                }
            }
        }
        return result;
    }

    /**
     * Pass in a single string of the form "[name]=value" to this function and get back a Pair object, containing
     * the name and value (split out into members of the Pair).
     *
     * @param entry a single [name]=value pair
     * @return a Pair object containing "name" and "value" strings as members of the Pair, or null if there was no
     * match, or if either name or value are null.  Note that both "name" and "value" are trimmed.
     */
    public Pair<String, String> parse(String entry) {

        if (entry.length() > 0) {
            Matcher m = pattern.matcher(entry);
            if (m.matches()) {
                String name = m.group(1);
                String value = m.group(2);
                if (name != null) {
                    name = name.trim();
                    if (value == null) {
                        value = "";
                    } else {
                        value = value.trim();
                    }
                    return Pair.of(name, value);
                }
            }
        }
        return null;
    }

    /**
     * The incoming set of strings contains the [name]=value pairs.  Search for the entry containing the name
     * specified and return the corresponding value, or null if "name" was not found.
     *
     * @param name The name to search for
     * @param entries The entries to search in
     * @return the value if found, or null if not
     */
    public String getValueForName(String name, Set<String> entries) {
        if (entries != null) {
            for (String entry : entries) {
                Pair<String, String> pair = parse(entry);
                if (pair != null && pair.getFirst().equals(name)) {
                    return pair.getSecond();
                }
            }
        }
        return null;
    }
}
