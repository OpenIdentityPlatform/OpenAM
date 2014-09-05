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
 * information: "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright 2014 ForgeRock AS. All rights reserved.
 */

package org.forgerock.openam.sts;

import org.forgerock.openam.utils.CollectionUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * This class encapsulates a set of public static methods to aid in marshalling Rest and Soap STS instance config state,
 * ultimately a json native format, into the Map<String, Set<String>> of attributes required for SMS persistence. As part
 * of this conversion, a Map<String, Object>, returned from JsonValue.asMap, must be transformed into a Map<String, Set<String>>.
 * Likewise, when going in the other direction (SMS map to JsonValue), the Map<String, Set<String>> needs to be converted
 * back to a Map<String,Object> so that a JsonValue can be constructed therefrom.
 *
 * Static methods cannot be mocked (without bcel), and thus should be kept to a minimum. The preferred guice interface/implementation
 * method was not adopted because this method is consumed from the config/user classes, like RestSTSInstanceConfig, which
 * do not have dependencies injected, as they define a client SDK, which facilitates the programmatic publishing of
 * STS instances.
 */
public class MapMarshallUtils {
    /**
     * Marshals from a Map<String,Object> produced by jsonValue.asMap to the Map<String, Set<String>> expected by the
     * SMS.
     * It is tempting to make this generic much more generic, and change the parameter to a JsonValue, as that would
     * preserve the types of the values, and then handle each of the Values differently, depending upon whether the
     * the JsonValue corresponding to the keys is a List, a Set, or a Map (a Map would entail a recursive call). The
     * problem with this approach is that this method cannot know the types of the List and Set, and that many of these
     * types implement custom string parsing schemes, as they must be entered in the AdminConsole. So this method is
     * pretty basic, and works correctly for the primitive value types, but all custom value types must be handled
     * specifically by the marshalToAttributeMap callers.

     * @param jsonMap
     * @return the Map<String, Set<String>> attributes expected by the SMS
     */
    public static Map<String, Set<String>> toSmsMap(Map<String, Object> jsonMap) {
        Map<String, Set<String>> smsMap = new HashMap<String, Set<String>>(jsonMap.size());
        for (Map.Entry<String, Object> entry : jsonMap.entrySet()) {
            if (entry.getValue() != null) {
                smsMap.put(entry.getKey(), CollectionUtils.asSet(entry.getValue().toString()));
            } else {
                smsMap.put(entry.getKey(), Collections.EMPTY_SET);
            }
        }
        return smsMap;
    }

    /**
     * Note that this method will only return the first element in the Set<String>. Its intent is only to aid in the
     * conversion between String and Set<String> for non-complex values. I would like to exclude Set instances with
     * a cardinality != 1, but encapsulated complex types will be passed the Map<String,Set<String>> corresponding to the
     * top-level object when the object hierarchy is re-constituted from SMS state, which means that some Map entries
     * will have Set<String> instances with a cardinality != 1. Yet this should not affect the leaf objects, as they do
     * not encapsulate any other complex objects.
     * @param attributeMap The map of STS instance attributes obtained from the SMS
     * @return the Map<String, Object> format which can be used to construct a JsonValue
     */
    public static Map<String, Object> toJsonValueMap(Map<String, Set<String>> attributeMap) {
        Map<String, Object> jsonValueMap = new HashMap<String, Object>(attributeMap.size());
        for (Map.Entry<String, Set<String>> entry : attributeMap.entrySet()) {
            if ((entry.getValue() != null) && !entry.getValue().isEmpty()) {
                jsonValueMap.put(entry.getKey(), entry.getValue().iterator().next());
            } else {
                jsonValueMap.put(entry.getKey(), null);
            }
        }
        return jsonValueMap;
    }

    /**
     * This method is called by AuthTargetMapping to marshal the contextMap from the Map<String, Object> returned by the
     * jsonValue.asMap to the Map<String, String> stored by the AuthTargetMapping. The AuthTargetMapping specifies the
     * authIndexType and authIndexValue for each type of token validated by an STS instance, and the contextMap provides
     * any additional context required to consume the rest authN to invoke the particular target defined by the authIndexType
     * and authIndexValue. For example the OpenID Connect ID Token authN module allows the user to specify the header name
     * which will reference the token. This information must be reflected in the AuthTargetMapping if it is to validate
     * OIDC tokens against this authN module. The contextMap is used to provide this information. Because this state must
     * ultimately be stored in the SMS as a String, I cannot store the Map<String,Object> native to json maps, but rather
     * must marshal this representation to a Map<String,String>.
     * @param objectMap  The Map, representing the AuthTargetMapping context, as returned from jsonValue.asMap. Can
     *                   possibly be null, in case the AuthTarget in the AuthTargetMapping does not have a context value.
     * @return The Map<String,String> transformation of the objectMap.
     */
    public static Map<String,String> objectValueToStringValueMap(Map<String,Object> objectMap) {
        if (objectMap == null) {
            return null;
        }
        Map<String,String> stringMap = new HashMap<String, String>(objectMap.size());
        for (Map.Entry<String,Object> entry : objectMap.entrySet()) {
            stringMap.put(entry.getKey(), entry.getValue().toString());
        }
        return stringMap;
    }
}
