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
 * Copyright 2012-2014 ForgeRock AS.
 */

package org.forgerock.openam.forgerockrest;

import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.ServiceConfig;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

public class ServiceConfigUtils {

    private static Debug debug = Debug.getInstance("frRest");

    public static Long getLongAttribute(ServiceConfig serviceConfig, String attributeName) {
        Map<String, Set<String>> attributes = serviceConfig.getAttributes();
        Set<String> attribute = attributes.get(attributeName);
        if (attribute != null && !attribute.isEmpty()) {
            try {
                return Long.decode(attribute.iterator().next());
            } catch (NumberFormatException e) {
                debug.error("RestUtils.getLongAttribute() :: " +
                        "Number format exception decoding Long attribute  " + e);
                return null;
            }
        } else {
            return null;
        }
    }

    public static Boolean getBooleanAttribute(ServiceConfig serviceConfig, String attributeName) {
        Map<String, Set<String>> attributes = serviceConfig.getAttributes();
        Set<String> attribute = attributes.get(attributeName);
        if (attribute != null && !attribute.isEmpty()) {
            return Boolean.valueOf(attribute.iterator().next());
        } else {
            return null;
        }
    }

    public static String getStringAttribute(ServiceConfig serviceConfig, String attributeName) {
        Map<String, Set<String>> attributes = serviceConfig.getAttributes();
        Set<String> attribute = attributes.get(attributeName);
        if (attribute != null && !attribute.isEmpty()) {
            return attribute.iterator().next();
        } else {
            return null;
        }
    }

    public static Set<String> getSetAttribute(ServiceConfig serviceConfig, String attributeName) {
        Map<String, Set<String>> attributes = serviceConfig.getAttributes();
        Set attribute = (Set)attributes.get(attributeName);
        if (attribute == null) {
            return Collections.EMPTY_SET;
        } else {
            return attribute;
        }
    }
}
