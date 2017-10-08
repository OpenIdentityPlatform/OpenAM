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

import com.sun.identity.entitlement.ConditionDecision;
import com.sun.identity.entitlement.EntitlementCondition;
import com.sun.identity.entitlement.EntitlementException;
import org.json.JSONException;
import org.json.JSONObject;

import javax.security.auth.Subject;
import java.security.Principal;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * Sample policy condition based on the length of the subject names:
 * subject user names must be at least the specified length.
 */
public class SampleConditionType implements EntitlementCondition {

    /**
     * Key to the minimum length of the subject set for this condition.
     */
    public static final String LENGTH_FIELD = "nameLength";
    private int                nameLength   = 0; // Default minimum length

    /**
     * Minimum length of the subject's user name(s).
     * @return Minimum length of the subject's user name(s).
     */
    public int getNameLength() {
        return nameLength;
    }

    /**
     * Set the minimum length for the subject's user name(s).
     * @param nameLength Minimum length.
     */
    public void setNameLength(int nameLength) {
        this.nameLength = nameLength;
    }

    private String displayType;

    @Override
    public void setDisplayType(String displayType) {
        this.displayType = displayType;
    }

    @Override
    public String getDisplayType() {
        return displayType;
    }

    @Override
    public void init(Map<String, Set<String>> map) {
        for (String key : map.keySet()) {
            if (key.equalsIgnoreCase(LENGTH_FIELD)) {
                setNameLength(Integer.parseInt(getInitStringValue(map.get(key))));
            }
        }
    }

    private static String getInitStringValue(Set<String> set) {
        return ((set == null) || set.isEmpty()) ? "" : set.iterator().next();
    }

    @Override
    public void setState(String state) {
        try {
            JSONObject json = new JSONObject(state);
            setNameLength(json.getInt(LENGTH_FIELD));
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public String getState() {
        try {
            JSONObject json = new JSONObject();
            json.put(LENGTH_FIELD, getNameLength());
            return json.toString();
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void validate() throws EntitlementException {
        if (getNameLength() < 0) {
            throw new EntitlementException(
                    EntitlementException.INVALID_PROPERTY_VALUE, LENGTH_FIELD);
        }
    }

    @Override
    public ConditionDecision evaluate(String realm,
                                      Subject subject,
                                      String resource,
                                      Map<String, Set<String>> environment)
            throws EntitlementException {

        boolean authorized = true;

        for (Principal principal : subject.getPrincipals()) {

            String userDn = principal.getName();

            int start = userDn.indexOf('=');
            int end   = userDn.indexOf(',');
            if (end <= start) {
                throw new EntitlementException(
                        EntitlementException.CONDITION_EVALUATION_FAILED,
                        "Name is not a valid DN: " + userDn);
            }

            String userName = userDn.substring(start + 1, end);

            if (userName.length() < getNameLength()) {
                authorized = false;
            }

        }

        return new ConditionDecision(authorized, Collections.EMPTY_MAP);
    }
}
