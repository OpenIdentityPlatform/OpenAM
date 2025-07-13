/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: ServerPropertyValidator.java,v 1.5 2008/09/02 23:44:07 babysunil Exp $
 *
 * Portions Copyrighted 2025 3A Systems, LLC.
 */

/**
 * Portions Copyrighted 2011-2016 ForgeRock AS.
 */
package com.sun.identity.common.configuration;

import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.ServiceAttributeValidator;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.StringTokenizer;
import org.apache.commons.lang3.StringUtils;
import org.forgerock.json.JsonException;
import org.forgerock.json.JsonPointer;

/**
 * Validates the values of server configuration properties.
 */
public class ServerPropertyValidator implements ServiceAttributeValidator {

    private static final Debug debug = Debug.getInstance("amServerProperties");
    private static Map<String, List<String>> keyToPossibleValues = new HashMap<>();
    private static Set<String> arrayKeys = new HashSet<>();
    private static Set<String> mapKeys = new HashSet<>();
    private static Set<String> integerKeys = new HashSet<>();
    private static Set<String> floatKeys = new HashSet<>();

    private static final String MAP = "map";
    private static final String INTEGER = "integer";
    private static final String FLOAT = "float";

    static {
        initialize();
    }

    private static void initialize() {
        ResourceBundle rb = ResourceBundle.getBundle("validserverconfig");
        for (Enumeration<String> keys = rb.getKeys(); keys.hasMoreElements(); ) {
            String key = keys.nextElement();
            String value = rb.getString(key);

            if (key.endsWith(".*")) {
                arrayKeys.add(key.substring(0, key.length() -1));
            } else if (value.equals(MAP)) {
                mapKeys.add(key);
            } else if (value.equals(INTEGER)) {
                integerKeys.add(key);
            } else if (value.equals(FLOAT)) {
                floatKeys.add(key);
            } else {
                if (value.length() > 0) {
                    StringTokenizer st = new StringTokenizer(value, ",");
                    List<String> list = new ArrayList<>(st.countTokens());
                    while (st.hasMoreTokens()) {
                        list.add(st.nextToken());
                    }
                    keyToPossibleValues.put(key, list);
                } else {
                    keyToPossibleValues.put(key, Collections.<String>emptyList());
                }
            }
        }
    }

    /**
     * Validates a set of server configuration properties.
     *
     * @param properties Set of String of this format name=value.
     */
    public boolean validate(Set<String> properties)  {
        try {
            validateProperty(properties);
            return true;
        } catch (UnknownPropertyNameException  | ConfigurationException ex) {
            return false;
        }
    }

    /**
     * Validates a set of server configuration properties.
     *
     * @param properties Set of String of this format name=value.
     * @throws UnknownPropertyNameException if property name is not valid.
     * @throws ConfigurationException if properties is not in proper format.
     */
    public static void validateProperty(Set<String> properties)
        throws UnknownPropertyNameException, ConfigurationException {
        try {
            Map propertiesAsMap = ServerConfiguration.getProperties(properties);
            validate(propertiesAsMap);
        } catch (IOException ex) {
            throw new ConfigurationException(ex.getMessage());
        }
    }

    /**
     * Validates a set of server configuration properties.
     *
     * @param properties Map of property name to value.
     * @throws UnknownPropertyNameException if property name is not valid.
     * @throws ConfigurationException if property name and value are not in
     *         proper format.
     */
    public static void validate(Map<String, String> properties) throws UnknownPropertyNameException, ConfigurationException {
        Set<String> unknownPropertyNames = new HashSet<>();

        for (final String key : properties.keySet()) {
            String value = properties.get(key);

            if ((value.length() > 0) && (!value.contains("%"))) {
                try {
                    //we only want to run the specific one(s) of these required, hence ORing here
                    boolean valid = validateMap(key) || validateNumber(key, value) || validate(key, value);
                } catch (UnknownPropertyNameException e) {
                    debug.error("Invalid server property {}", key, e);
                    unknownPropertyNames.add(key);
                }
            }
        }

        if (!unknownPropertyNames.isEmpty()) {
            throw new UnknownPropertyNameException("unknown.properties",
                    new String[]{ StringUtils.join(unknownPropertyNames, ", ") });
        }
    }

    private static boolean validateMap(String key)
        throws UnknownPropertyNameException, ConfigurationException {

        boolean validated = false;

        try {
            new JsonPointer(key);
        } catch (JsonException ex) {
            String[] param = {key};
            throw new ConfigurationException("invalid.map.property", param);
        }

        if (key.endsWith("]")) {
            int startBracket = key.indexOf('[');
            if (startBracket == -1) {
                String[] param = {key};
                throw new ConfigurationException("invalid.map.property", param);
            }

            String k = key.substring(0, startBracket);
            if (!mapKeys.contains(k) && !arrayKeys.contains(k)) {
                String[] param = {key};
                throw new UnknownPropertyNameException("unknown.property", param);
            }

            if (arrayKeys.contains(k)) {
                int endBracket = key.indexOf('[');
                String ind = key.substring(startBracket+1, endBracket);
                try {
                    Integer.parseInt(ind);
                } catch (NumberFormatException ex) {
                    String[] param = {key};
                    throw new ConfigurationException("invalid.array.property", param);
                }
            }
            validated = true;
        }
        return validated;
    }

    private static boolean validateNumber(String key, String value)
        throws UnknownPropertyNameException, ConfigurationException {
        boolean validated = false;
        if (floatKeys.contains(key)) {
            try {
                Float.parseFloat(value);
            } catch (NumberFormatException ex) {
                String[] param = {key};
                throw new ConfigurationException("invalid.float.property", param);
            }
            validated = true;
        } else if (integerKeys.contains(key)) {
            try {
                Integer.parseInt(value);
            } catch (NumberFormatException ex) {
                String[] param = {key};
                throw new ConfigurationException("invalid.integer.property", param);
            }
            validated = true;
        }
        return validated;
    }

    private static boolean validate(String key, String value)
        throws UnknownPropertyNameException, ConfigurationException {
        if (!keyToPossibleValues.keySet().contains(key)) {
            String[] param = {key};
            throw new UnknownPropertyNameException("unknown.property", param);
        }

        if (value.length() > 0) {
            List possibleValues = (List) keyToPossibleValues.get(key);
            if (!possibleValues.isEmpty()) {
                if (!possibleValues.contains(value)) {
                    String[] param = {key};
                    throw new ConfigurationException("invalid.value.property", param);
                }
            }
        }
        return true;
    }

    /**
     * Returns the true value of a property.
     *
     * @param propertyKey Key of property.
     * @return the true value of a property.
     */
    public static String getTrueValue(String propertyKey) {
        List list = (List) keyToPossibleValues.get(propertyKey);
        return (String) list.get(0);
    }

    /**
     * Returns the false value of a property.
     *
     * @param propertyKey Key of property.
     * @return the false value of a property.
     */
    public static String getFalseValue(String propertyKey) {
        List list = (List) keyToPossibleValues.get(propertyKey);
        return (String) list.get(1);
    }
}
