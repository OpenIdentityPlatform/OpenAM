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
package org.forgerock.openam.scripting.javascript;

import java.util.HashMap;
import java.util.Map;
import org.mozilla.javascript.NativeObject;

/**
 * Provides static helper functions to the Rhino JavaScript engine for
 * communicating between Java classes and scripts.
 */
public class JavaScriptMapFactory {

    /**
     * Converts a JavaScript object of any nature to a Java map. A null object will return
     * a null from this script.
     *
     * @param nativeObj The native object to convert to Java map
     * @return a map representing the object sent in. May be null.
     * @throws IllegalArgumentException if the keys to any of the values in the native object are not Strings or ints,
     * as it should be impossible to generate such objects.
     */
    public static Map<String, Object> javaScriptObjectToMap(NativeObject nativeObj) {

        if (nativeObj == null) {
            return null;
        }

        //getAllIds can return non-enumerables
        final Map<String, Object> toReturn = new HashMap<String, Object>(nativeObj.getIds().length);

        for (Object key : nativeObj.getIds()) {
            final Object value;

            if (key instanceof String) {
                value = nativeObj.get((String)key, nativeObj);
            } else if (key instanceof Integer) {
                value = nativeObj.get(((Integer)key).intValue(), nativeObj);
            } else {
                throw new IllegalArgumentException("Invalid JavaScript object representation.");
            }

            if (value != null) {
                toReturn.put(key.toString(), value);
            }
        }

        return toReturn;
    }

}
