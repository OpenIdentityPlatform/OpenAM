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
 * Copyright 2006 Sun Microsystems Inc
 */
/*
 * Portions Copyright 2014 ForgeRock AS
 */

package org.forgerock.openam.entitlement.conditions.environment;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.entitlement.ConditionDecision;
import com.sun.identity.entitlement.EntitlementConditionAdaptor;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.PrivilegeManager;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.openam.core.CoreWrapper;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.security.auth.Subject;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import static com.sun.identity.entitlement.EntitlementException.CONDITION_EVALUTATION_FAILED;
import static org.forgerock.openam.entitlement.conditions.environment.ConditionConstants.VALUE_CASE_INSENSITIVE;

/**
 * An implementation of an {@link com.sun.identity.entitlement.EntitlementCondition} that will check whether the
 * properties of a session match the configured values.
 *
 * @since 12.0.0
 */
public class SessionPropertyCondition extends EntitlementConditionAdaptor {

    private static final boolean IGNORE_VALUE_CASE_DEFAULT = true;
    private static final String DELIMITER = "|";

    private final Debug debug;
    private final CoreWrapper coreWrapper;

    private boolean ignoreValueCase = IGNORE_VALUE_CASE_DEFAULT;
    private Map<String, Set<String>> properties = new HashMap<String, Set<String>>();

    /**
     * Constructs a new SessionPropertyCondition instance.
     */
    public SessionPropertyCondition() {
        this(PrivilegeManager.debug, new CoreWrapper());
    }

    /**
     * Constructs a new SessionPropertyCondition instance.
     *
     * @param debug A Debug instance.
     * @param coreWrapper An instance of the CoreWrapper.
     */
    SessionPropertyCondition(Debug debug, CoreWrapper coreWrapper) {
        this.debug = debug;
        this.coreWrapper = coreWrapper;
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public void setState(String state) {
        try {
            JSONObject jo = new JSONObject(state);
            setState(jo);
            JSONObject props = jo.getJSONObject("properties");
            Iterator<String> keys = props.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                Set<String> s = new HashSet<String>();
                JSONArray set = props.getJSONArray(key);
                for (int i = 0; i < set.length(); i++) {
                    s.add(set.getString(i));
                }
                properties.put(key, s);
            }
            ignoreValueCase = jo.getBoolean(VALUE_CASE_INSENSITIVE);
        } catch (JSONException e) {
            debug.message("SessionPropertyCondition: Failed to set state", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getState() {
        return toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConditionDecision evaluate(String realm, Subject subject, String resourceName, Map<String, Set<String>> env)
            throws EntitlementException {

        boolean allowed = true;
        if (debug.messageEnabled()) {
            debug.message("SessionPropertyCondition.evaluate():entering, ignoreValueCase= "
                    + ignoreValueCase);
        }
        SSOToken token = (SSOToken) getValue(subject.getPrivateCredentials());
        if ((properties != null) && !properties.isEmpty()) {
            Set<String> names = properties.keySet();
            namesIterLoop:
            for (String name : names) {
                Set<String> values = properties.get(name);

                if (debug.messageEnabled()) {
                    debug.message("SessionPropertyCondition.evaluate():propertyName = " + name
                            + ",conditionValues = " + values);
                }

                if (name.equals(VALUE_CASE_INSENSITIVE) || values == null || values.isEmpty()) {
                    continue;
                }

                try {
                    String sessionValue = token.getProperty(name);
                    Set<String> sessionValues = null;
                    if (sessionValue != null && sessionValue.contains(DELIMITER)) {
                        sessionValues = coreWrapper.delimStringToSet(sessionValue, DELIMITER);
                    }

                    if (debug.messageEnabled()) {
                        debug.message("SessionPropertyCondition.evaluate():,sessionValue = " + sessionValue
                                + ",sessionValues = " + sessionValues);
                    }

                    if (sessionValue == null) {
                        allowed = false;
                        continue;
                    }

                    if (sessionValues != null) { //session, multivalued
                        if (!ignoreValueCase) { //caseExact match
                            for (String splitSessionValue : sessionValues) {
                                if (values.contains(splitSessionValue)) {
                                    continue namesIterLoop;
                                }
                            }
                        } else { //caseIgnore match
                            for (String splitSessionValue : sessionValues) {
                                for (String value : values) {
                                    if (splitSessionValue.equalsIgnoreCase(value)) {
                                        continue namesIterLoop;
                                    }
                                }
                            }
                        }
                    } else if (!ignoreValueCase) { //single session value, caseExact
                        if (values.contains(sessionValue)) {
                            continue;
                        }
                    } else { //single session value, caseIgnore match
                        for (String value : values) {
                            if (sessionValue.equalsIgnoreCase(value)) {
                                continue namesIterLoop;
                            }
                        }
                    }
                    allowed = false;
                } catch (SSOException e) {
                    debug.error("Condition evaluation failed", e);
                    throw new EntitlementException(CONDITION_EVALUTATION_FAILED, e);
                }

            }
        } else {
            debug.message("SessionPropertyCondition.evaluate():no parameter defined,defaulting allow=true");
            allowed = true;
        }

        if (debug.messageEnabled()) {
            debug.message("SessionPropertyCondition.evaluate():allowed= " + allowed);
        }

        return new ConditionDecision(allowed, Collections.<String, Set<String>>emptyMap());
    }

    private <T> T getValue(Set<T> values) {
        if (values != null && values.iterator().hasNext()) {
            return values.iterator().next();
        }
        return null;
    }

    private JSONObject toJSONObject() throws JSONException {
        JSONObject jo = new JSONObject();
        toJSONObject(jo);

        JSONObject props = new JSONObject();
        for (Map.Entry<String, Set<String>> prop : properties.entrySet()) {
            JSONArray array = new JSONArray();
            for (String s : prop.getValue()) {
                array.put(s);
            }
            props.put(prop.getKey(), array);
        }
        jo.put("properties", props);
        jo.put(VALUE_CASE_INSENSITIVE, ignoreValueCase);
        return jo;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        String s = null;
        try {
            s = toJSONObject().toString(2);
        } catch (JSONException e) {
            PrivilegeManager.debug.error("SessionPropertyCondition.toString()", e);
        }
        return s;
    }

    public boolean isIgnoreValueCase() {
        return ignoreValueCase;
    }

    public void setIgnoreValueCase(boolean ignoreValueCase) {
        this.ignoreValueCase = ignoreValueCase;
    }

    public Map<String, Set<String>> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, Set<String>> properties) {
        this.properties = properties;
    }

    @Override
    public void validate() throws EntitlementException {
        if (properties == null || properties.isEmpty()) {
            throw new EntitlementException(EntitlementException.PROPERTY_VALUE_NOT_DEFINED, "properties");
        }
    }
}
