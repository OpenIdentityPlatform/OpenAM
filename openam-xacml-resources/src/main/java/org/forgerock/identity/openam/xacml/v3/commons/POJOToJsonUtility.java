/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2013 ForgeRock US, Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 */
package org.forgerock.identity.openam.xacml.v3.commons;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.MappingJsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/**
 * XACML Response Object to JSON Utility Class.
 * <p/>
 * Very simple Marshaller to place a XACML Response or any Object into a JSON Object.
 *
 * @author Jeff.Schenk@forgerock.com
 */

public class POJOToJsonUtility {

    /**
     * Thread-safe Object Mapper Instance.
     */
    private static ObjectMapper mapper = null;

    static {
        mapper = new ObjectMapper();
        // to allow serialization of "empty" POJOs (no properties to serialize)
        // (without this setting, an exception is thrown in those cases)
        mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        // Enable or Disable Additional Features Here...
    }

    /**
     * JSON to Map from String Content.
     *
     * @param object - Object to be Marshaled into a JSON Representation.
     * @return
     * @throws java.io.IOException
     */
    public static String toJSON(Object object) throws IOException {
        if (object == null) {
            return null;
        }
        // Return rendered Marshaled Object.
        return mapper.writeValueAsString(object);
    }


}
