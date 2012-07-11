/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: JSONEntitlement.java,v 1.3 2009/11/12 18:37:38 veiming Exp $
 */

package com.sun.identity.entitlement;

// import com.sun.identity.rest.DecisionResource;
import com.sun.identity.shared.JSONUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class JSONEntitlement {
    public static final String JSON_DECISION_ARRAY_KEY = "results";
    private String resourceName;
    private Map<String, Set<String>> advices;
    private Map<String, Set<String>> attributes;
    private Map<String, Boolean> actionValues;

    /**
     * Constructor.
     *
     * @param resourceName reource name.
     * @param actionValues action values.
     */
    public JSONEntitlement(String resourceName,
        Map<String, Boolean> actionValues,
        Map<String, Set<String>> advices,
        Map<String, Set<String>> attributes) {
        this.resourceName = resourceName;
        this.actionValues = actionValues;
        this.advices = advices;
        this.attributes = attributes;
    }

    /**
     * Constructor.
     *
     * @param jo JSON object.
     * @throws JSONException if <code>jo</code> is not well formed.
     */
    public JSONEntitlement(JSONObject jo) throws JSONException {
        resourceName = jo.optString("resourceName");
        actionValues = JSONUtils.getMapStringBoolean(jo, "actionsValues");
        advices = JSONUtils.getMapStringSetString(jo, "advices");
        attributes = JSONUtils.getMapStringSetString(jo, "attributes");
    }

    /**
     * Returns resource name.
     *
     * @return resource name.
     */
    public String getResourceName() {
        return resourceName;
    }

    public Map<String, Boolean> getActionValues() {
        return actionValues;
    }

    public Boolean getActionValue(String action) {
        return (actionValues != null) ? actionValues.get(action) : null;
    }

    public Map<String, Set<String>> getAdvices() {
        return advices;
    }

    public Map<String, Set<String>> getAttributes() {
        return attributes;
    }

    public JSONObject toJSONObject() throws JSONException {
        JSONObject jo = new JSONObject();
        jo.put("actionsValues", actionValues);
        jo.put("resourceName", resourceName);

        if (advices != null) {
            jo.put("advices", advices);
        }

        if (attributes != null) {
            jo.put("attributes", attributes);
        }

        return jo;
    }

    public static List<JSONEntitlement> getEntitlements(JSONObject jo)
        throws JSONException {
        if (!jo.has(JSON_DECISION_ARRAY_KEY)) {
            return Collections.EMPTY_LIST;
        }

        List<JSONEntitlement> results = new ArrayList<JSONEntitlement>();
        JSONArray array = jo.getJSONArray(
            JSON_DECISION_ARRAY_KEY);
        for (int i = 0; i < array.length(); i++) {
            results.add(new JSONEntitlement(array.getJSONObject(i)));
        }
        return results;
    }
}
