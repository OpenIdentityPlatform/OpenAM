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
 * JSON to Map Parser
 * <p/>
 * Very simple JSON Parser to place any JSON Stream, File or String Object into a
 * usable Map for later transformation into a POJO or direct interrogation of the map.
 * <p/>
 * All Map Objects generated here are Final to protect integrity of Map.
 *
 * @author Jeff.Schenk@forgerock.com
 */

public class JsonToMapUtility {

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
     * JSON to Map from an InputStream
     *
     * @param streamContent
     * @return
     * @throws IOException
     */
    public static Map<String, Object> fromStream(InputStream streamContent) throws IOException {
        JsonFactory f = new MappingJsonFactory();
        JsonParser jp = f.createJsonParser(streamContent);
        return performParsing(jp);
    }

    /**
     * JSON to Map from Input File.
     *
     * @param fileContent
     * @return
     * @throws IOException
     */
    public static Map<String, Object> fromFile(File fileContent) throws IOException {
        JsonFactory f = new MappingJsonFactory();
        JsonParser jp = f.createJsonParser(fileContent);
        return performParsing(jp);
    }

    /**
     * JSON to Map from String Content.
     *
     * @param rawContent
     * @return
     * @throws IOException
     */
    public static Map<String, Object> fromString(String rawContent) throws IOException {
        JsonFactory f = new MappingJsonFactory();
        JsonParser jp = f.createJsonParser(rawContent);
        return performParsing(jp);
    }

    /**
     * JSON to Mao from byte[] Array.
     *
     * @param byteData
     * @return
     * @throws IOException
     */
    public static Map<String, Object> fromByteArray(byte[] byteData) throws IOException {
        JsonFactory f = new MappingJsonFactory();
        JsonParser jp = f.createJsonParser(byteData);
        return performParsing(jp);
    }

    /**
     * Private common helper method to perform the Mapping to a Simple Map Object for
     * upstream interrogation.
     *
     * @param jsonParser
     * @return
     * @throws IOException
     */
    private static Map<String, Object> performParsing(JsonParser jsonParser)
            throws IOException {
        // to allow auto-close of our source
        jsonParser.enable(JsonParser.Feature.AUTO_CLOSE_SOURCE);
        // to ensure we disallow C/C++ style comments in JSON (non-standard, disabled by default)
        jsonParser.disable(JsonParser.Feature.ALLOW_COMMENTS);
        // to allow (non-standard) unquoted field names in JSON:
        jsonParser.enable(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES);
        // to allow use of apostrophes (single quotes), non standard
        jsonParser.enable(JsonParser.Feature.ALLOW_SINGLE_QUOTES);
        return mapper.readValue(jsonParser, Map.class);
    }

    /**
     * Main to Transform a JSON File to a Map Object output.
     *
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        if ((args == null) || (args.length < 1)) {
            return;
        }
        try {
            for (String argument : args) {
                Map<String, Object> mapObject = fromFile(new File(argument));
                System.out.println(mapObject);
            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
        }
    }

}
