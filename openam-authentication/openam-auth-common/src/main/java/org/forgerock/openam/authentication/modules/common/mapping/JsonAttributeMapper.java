/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright © 2011-2014 ForgeRock AS. All rights reserved.
 * Copyright © 2011 Cybernetica AS.
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

package org.forgerock.openam.authentication.modules.common.mapping;

import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.openam.utils.CollectionUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

/**
 * An {@code AttributeMapper} that takes its values from a JSON string.
 */
public class JsonAttributeMapper implements AttributeMapper<String> {

    private static Debug debug = Debug.getInstance("amAuth");
    private List<String> prefixedAttributes = null;
    private String prefix = null;
    private String bundleName;

    /**
     * Default constructor if not being given any prefix parameter
     */
    public JsonAttributeMapper() {}

    /**
     * Constructor with a prefix that will be prepended to all mapped values.
     * @param prefixedAttributesList Comma-separated list of attributes that need a prefix applied, or <code>*</code>.
     * @param prefix The prefix to be applied.
     */
    public JsonAttributeMapper(String prefixedAttributesList, String prefix) {
        this.prefix = prefix;
        this.prefixedAttributes = Arrays.asList(prefixedAttributesList.split(","));
    }

    /**
     * {@inheritDoc}
     */
    public void init(String bundleName) {
        this.bundleName = bundleName;
    }

    /**
     * {@inheritDoc}
     */
    public Map<String, Set<String>> getAttributes(Map<String, String> attributeMapConfiguration, String jsonText)
            throws AuthLoginException {

        if (debug.messageEnabled()) {
            debug.message("defaultAttributeMapper.getAttributes: " + attributeMapConfiguration);
        }
        JSONObject json;
        try {
            json = new JSONObject(jsonText);
        } catch (JSONException ex) {
            debug.error("OAuth.process(): JSONException: " + ex.getMessage());
            throw new AuthLoginException(bundleName, ex.getMessage(), null);
        }
        Map<String, Set<String>> attr = new HashMap<String, Set<String>>();
        String responseName = "";
        String localName;
        for(Map.Entry<String, String> entry : attributeMapConfiguration.entrySet()) {
            try {
                responseName = entry.getKey();
                localName = entry.getValue();
                
                //put full json response to attribute
                if("remote-json".equals(responseName)) {
                	attr.put(localName, CollectionUtils.asSet(json.toString()));
                	continue;
                }

                if (debug.messageEnabled()) {
                    debug.message("defaultAttributeMapper.getAttributes: " + responseName + ":" + localName);
                }

                String data;
                if (responseName != null && responseName.indexOf(".") != -1) {
                    StringTokenizer parts = new StringTokenizer(responseName, ".");
                    data = json.getJSONObject(parts.nextToken()).getString(parts.nextToken());
                } else {
                    data = json.getString(responseName);
                }

                if (prefix != null && (prefixedAttributes.contains(localName) || prefixedAttributes.contains("*"))) {
                    data = prefix + data;
                }
                attr.put(localName, CollectionUtils.asSet(data));
            } catch (JSONException ex) {
                debug.error("defaultAttributeMapper.getAttributes: Could not get the attribute" + responseName, ex);
            }
        }
        
	    return attr;

    }
}
