/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: JSONUtils.java,v 1.1 2009/09/21 18:33:44 dillidorai Exp $
 */

package com.sun.identity.shared;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class JSONUtils {
    private JSONUtils() {
    }

    public static Set<String> getSet(JSONObject json, String key)
        throws JSONException {
        if (!json.has(key)) {
            return null;
        }

        Set<String> results = new HashSet<String>();
        JSONArray values = json.getJSONArray(key);
        for (int i = 0; i < values.length(); i++) {
            results.add((String) values.get(i));
        }
        return results;
    }


    public static Map<String, Set<String>> getMapStringSetString(
        JSONObject json,
        String key
    ) throws JSONException {
        if (!json.has(key)) {
            return null;
        }

        Map<String, Set<String>> results = new HashMap<String, Set<String>>();
        JSONObject js = (JSONObject)json.opt(key);
        for (Iterator i = js.keys(); i.hasNext(); ) {
            String k = (String)i.next();
            Object test = js.opt(k);
            if (test instanceof JSONArray) {
                JSONArray values = (JSONArray) test;
                Set<String> set = new HashSet<String>();
                results.put(k, set);
                for (int j = 0; j < values.length(); j++) {
                    set.add((String) values.get(j));
                }
            } else if (test instanceof Boolean) {
                Set<String> set = new HashSet<String>();
                results.put(k, set);
                set.add(test.toString());
            } else if (test.equals(JSONObject.NULL)) {
                results.put(k, null);
            }
        }
        return results;
    }

    public static Map<String, Boolean> getMapStringBoolean(
        JSONObject json,
        String key
    ) throws JSONException {
        if (!json.has(key)) {
            return null;
        }

        Map<String, Boolean> results = new HashMap<String, Boolean>();
        JSONObject js = (JSONObject)json.opt(key);

        for (Iterator i = js.keys(); i.hasNext(); ) {
            String k = (String)i.next();
            results.put(k, (Boolean)js.opt(k));
        }
        return results;
    }

    public static long getLong(
        JSONObject json,
        String key
    ) throws JSONException {
        if (!json.has(key)) {
            return 0;
        }

        String str = json.getString(key);
        try {
            return Long.parseLong(str);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
