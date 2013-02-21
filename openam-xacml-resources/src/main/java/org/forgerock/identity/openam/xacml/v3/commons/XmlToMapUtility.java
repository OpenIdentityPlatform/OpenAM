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

import org.json.JSONException;
import org.json.JSONObject;
import org.json.XML;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Scanner;

/**
 * XML to Map Parser
 * <p/>
 * Very simple XML Parser to place any XML Stream, File or String Object into a
 * usable Map for later transformation into a POJO or direct interrogation of the map.
 * <p/>
 * All Map Objects generated here are Final to protect integrity of Map.
 *
 * @author Jeff.Schenk@forgerock.com
 */

public class XmlToMapUtility {

    /**
     * JSON to Map from String Content.
     *
     * @param rawContent
     * @return
     * @throws java.io.IOException
     */
    public static Map<String, Object> fromString(String rawContent) throws JSONException, IOException {
        return toMAP(rawContent);
    }

    /**
     * Private common helper method to perform the Mapping to a Simple Map Object for
     * upstream interrogation.
     *
     * @param xmlData
     * @return
     * @throws JSONException, java.io.IOException
     */
    private static Map<String, Object> toMAP(String xmlData)
            throws JSONException, IOException {
        if ((xmlData == null) || (xmlData.isEmpty())) {
            return null;
        }
        // Convert XML to a JSON Object.
        return JsonToMapUtility.fromString(toJSON(xmlData));
    }

    /**
     * Public Helper to Transform
     *
     * @param xmlData
     * @return
     * @throws JSONException
     */
    public static String toJSON(String xmlData)
            throws JSONException {
        if ((xmlData == null) || (xmlData.isEmpty())) {
            return null;
        }
        // Convert XML to a JSON Object.
        JSONObject xmlJsonObject = XML.toJSONObject(xmlData);
        return xmlJsonObject.toString();
    }

    /**
     * Main to Transform a XML File Content to a JSON Representation.
     *
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        if ((args == null) || (args.length < 1)) {
            return;
        }
        for (String argument : args) {
            InputStream inputStream = new FileInputStream(new File(argument));
            String rawContent = new Scanner(inputStream).useDelimiter("\\A").next();

            Map<String, Object> mapObjects = fromString(rawContent);
            System.out.println("XML to JSON Value: " + mapObjects);
        }
    }

}
