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
 * Copyright 2014-2016 ForgeRock AS.
 */

package org.forgerock.openam.examples;

import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.ResourceAttribute;
import org.json.JSONException;
import org.json.JSONObject;

import javax.security.auth.Subject;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Sample attribute provider that sets a static "sample" attribute value.
 */
public class SampleAttributeType implements ResourceAttribute {
    private String propertyName;
    private String pResponseProviderName;

    @Override
    public void setPropertyName(String name) {
        this.propertyName = name;
    }

    @Override
    public String getPropertyName() {
        return propertyName;
    }

    @Override
    public Set<String> getPropertyValues() {
        Set<String> propertyValues = new HashSet<String>();
        propertyValues.add("sample");
        return propertyValues;
    }

    /**
     * This does nothing. Property values are always {@code sample}.
     * @param values    These values are ignored.
     */
    public void setPropertyValues(Set<String> values) {
        // Silently ignore values for this implementation.
    }

    @Override
    public Map<String, Set<String>> evaluate(Subject adminSubject,
                                             String realm,
                                             Subject subject,
                                             String resourceName,
                                             Map<String, Set<String>> advices)
            throws EntitlementException {
        Map<String, Set<String>> map = new HashMap<String, Set<String>>();
        map.put(propertyName, getPropertyValues());
        return map;
    }

    @Override
    public void setPResponseProviderName(String pResponseProviderName) {
        this.pResponseProviderName = pResponseProviderName;
    }

    @Override
    public String getPResponseProviderName() {
        return pResponseProviderName;
    }

    @Override
    public String getState() {
        try {
            JSONObject json = new JSONObject();
            json.put("propertyName", propertyName);
            json.put("pResponseProviderName", pResponseProviderName);
            return json.toString();
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void setState(String state) {
        try {
            JSONObject json = new JSONObject(state);
            propertyName = json.getString("propertyName");
            if (json.has("pResponseProviderName")) {
                pResponseProviderName = json.getString("pResponseProviderName");
            }
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }
}
