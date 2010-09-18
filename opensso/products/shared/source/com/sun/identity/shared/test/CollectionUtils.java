/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006 Sun Microsystems Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * https://opensso.dev.java.net/public/CDDLv1.0.html or
 * opensso/legal/CDDLv1.0.txt
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at opensso/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id: CollectionUtils.java,v 1.2 2008/06/25 05:53:05 qcheng Exp $
 *
 */

package com.sun.identity.shared.test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

/**
 * This class provides collections related helper methods.
 */
public class CollectionUtils {
    private CollectionUtils() {
    }

    /**
     * Returns set of string. This is a convenient method for adding a set of 
     * string into a map. In this project, we usually have the 
     * <code>Map&lt;String, Set&lt;String&gt;&gt; and many times, we just 
     * want to add a string to the map.
     *
     * @param key Key to the entry in the map.
     * @param map Map of String to Set of String.
     * @param value Value to be added to the map referenced by <code>key</code>.
     * @return Set of string.
     */
    public static Set<String> putSetIntoMap(
        String key,
        Map<String, Set<String>> map,
        String value
    ) {
        Set<String> set = new HashSet<String>();
        set.add(value);
        map.put(key, set);
        return set;
    }

    /**
     * Returns a map of String to Set of String from a formatted string.
     * The format is 
     * <pre>
     * &lt;key1&gt;=&lt;value11&gt;,&lt;value12&gt;...,&lt;value13&gt;;
     * &lt;key2&gt;=&lt;value21&gt;,&lt;value22&gt;...,&lt;value23&gt;; ...
     * &lt;keyn&gt;=&lt;valuen1&gt;,&lt;valuen2&gt;...,&lt;valuen3&gt;
     * </pre>
     *
     * @param str Formatted String.
     * @return a map of String to Set of String
     */
    public static Map<String, Set<String>> parseStringToMap(String str) {
        Map<String, Set<String>> map = new HashMap<String, Set<String>>();
        StringTokenizer st = new StringTokenizer(str, ";");
        while (st.hasMoreTokens()) {
            String token = st.nextToken();
            int idx = token.indexOf("=");
            if (idx != -1) {
                Set<String> set = new HashSet<String>();
                map.put(token.substring(0, idx).trim(), set);
                StringTokenizer st1 = new StringTokenizer(
                    token.substring(idx+1), ",");
                while (st1.hasMoreTokens()) {
                    set.add(st1.nextToken().trim());
                }
            }
        }
        return map;
    }
    
    /**
     * Returns a Set of String from a formatted string.
     * The format is 
     * <pre>
     * &lt;value1&gt;,&lt;value2&gt;...,&lt;value3&gt;
     * </pre>
     *
     * @param str Formatted String.
     * @return a map of String to Set of String
     */
    public static Set<String> parseStringToSet(String str) {
        Set<String> set = new HashSet<String>();
        StringTokenizer st = new StringTokenizer(str, ",");
        while (st.hasMoreTokens()) {
            set.add(st.nextToken().trim());
        }
        return set;
    }
    
    /**
     * Returns a cloned Map of String to Set of String.
     *
     * @param map Map to be cloned.
     * @return cloned map.
     */
    public static Map<String, Set<String>> cloneMap(
        Map<String, Set<String>> map
    ) {
        Map<String, Set<String>> clone = new HashMap<String, Set<String>>();
        for (String key : map.keySet()) {
            Set<String> set = new HashSet<String>();
            Set<String> orig = (Set<String>)map.get(key);
            set.addAll(orig);
            clone.put(key, set);
        }
        return clone;
    }

    /**
     * Returns a Map of String to empty set.
     *
     * @param keys Keys of the map.
     * @return Map of String to empty set.
     */    
    public static Map<String, Set<String>> getEmptyValuesMap(
        Set<String> keys
    ) {
        Map<String, Set<String>> map = new HashMap<String, Set<String>>();
        for (String key : keys) {
            map.put(key, new HashSet<String>());
        }
        return map;
    }
}
