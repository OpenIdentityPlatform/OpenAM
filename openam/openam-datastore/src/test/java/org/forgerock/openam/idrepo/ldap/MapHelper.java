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
 * Copyright 2013 ForgeRock Inc.
 */
package org.forgerock.openam.idrepo.ldap;

import com.sun.identity.common.CaseInsensitiveHashMap;
import com.sun.identity.common.CaseInsensitiveHashSet;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.Set;

public class MapHelper {

    public static Map<String, Set<String>> readMap(String fileName) throws IOException {
        Map<String, Set<String>> ret = new CaseInsensitiveHashMap();
        InputStream is = MapHelper.class.getResourceAsStream(fileName);
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        String line;
        while ((line = br.readLine()) != null) {
            if (line.startsWith("#")) {
                continue;
            }
            int idx = line.indexOf('=');
            if (idx != -1) {
                String key = line.substring(0, idx);
                String value = line.substring(idx + 1);
                if (!value.isEmpty()) {
                    Set<String> values = ret.get(key);
                    if (values == null) {
                        values = new CaseInsensitiveHashSet(1);
                    }
                    values.add(value);
                    ret.put(key, values);
                }
            }
        }
        return ret;
    }
}
