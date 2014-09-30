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
package com.sun.identity.entitlement.xacml3;

import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.Privilege;
import com.sun.identity.entitlement.ResourceAttribute;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.IOException;

/**
 * Provides the ability to convert the ResourceAttribute representation to JSON and back again.
 *
 * Conversion performed by this class will use standard JSON serialisation/deserialisation.
 */
public class ResourceAttributeUtil {
    public static final int JSON_PARSE_ERROR = EntitlementException.JSON_PARSE_ERROR;

    // Chosen as it is unlikely to appear in a class name.
    public static final String SEP = "-z-";

    private final ObjectMapper mapper;

    /**
     * Create a default instance of the ResourceAttributeUtil.
     */
    public ResourceAttributeUtil() {
        this(new ObjectMapper());
    }

    /**
     * Constructor with dependencies exposed to allow testing.
     * @param mapper Non null mapper.
     */
    ResourceAttributeUtil(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * Convert a {@link Privilege} {@link ResourceAttribute} into JSON representation.
     *
     * @param attribute Non null attribute.
     * @return String format representation. Non null.
     * @throws EntitlementException If there was an unexpected error during conversion.
     */
    public String toJSON(ResourceAttribute attribute) throws EntitlementException {
        try {
            String className = attribute.getClass().getName();
            return className + SEP + mapper.writeValueAsString(attribute);
        } catch (IOException e) {
            throw new EntitlementException(JSON_PARSE_ERROR, e);
        }
    }

    /**
     * Convert the JSON text into a {@link ResourceAttribute}.
     *
     * @param json Non null, maybe empty.
     * @return Null if json was empty, otherwise a ResourceAttribute.
     * @throws EntitlementException If there was an unexpected error during conversion.
     */
    public ResourceAttribute fromJSON(String json) throws EntitlementException {
        try {
            return (ResourceAttribute) mapper.readValue(getJSON(json), getClass(json));
        } catch (IOException e) {
            throw new EntitlementException(JSON_PARSE_ERROR, e);
        }
    }

    private Class getClass(String json) throws EntitlementException {
        int pos = getSeparatorIndex(json);
        String classname = json.substring(0, pos);
        try {
            return Class.forName(classname);
        } catch (ClassNotFoundException e) {
            throw new EntitlementException(
                    EntitlementException.UNKNOWN_RESOURCE_ATTRIBUTE_CLASS,
                    new Object[]{classname}, e);
        }
    }

    private String getJSON(String json) throws EntitlementException {
        int pos = getSeparatorIndex(json);
        return json.substring(pos + SEP.length(), json.length());
    }

    private int getSeparatorIndex(String json) throws EntitlementException {
        int pos = json.indexOf(SEP);
        if (pos == -1) {
            throw new EntitlementException(JSON_PARSE_ERROR, new Object[]{json});
        }
        return pos;
    }
}
