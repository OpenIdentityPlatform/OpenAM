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
 * $Id: AttributeLookupCondition.java,v 1.1 2009/08/19 05:40:32 veiming Exp $
 */

package com.sun.identity.entitlement;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import javax.security.auth.Subject;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * This condition evaluates if a given attribute from subject matches with
 * the one in resource.
 */
public class AttributeLookupCondition extends EntitlementConditionAdaptor {
    /**
     * User Macro
     */
    public static final String MACRO_USER = "$USER";
    /**
     * Resource Macro
     */
    public static final String MACRO_RESOURCE = "$RES";

    private String key;
    private String value;
    private String pConditionName = "";

    /**
     * Constructor.
     */
    public AttributeLookupCondition() {
    }

    /**
     * Constructor.
     *
     * @param key Matching key.
     * @param value Matching value.
     */
    public AttributeLookupCondition(String key, String value) {
        this.key = key;
        this.value = value;
    }

    /**
     * Sets state of the object
     *
     * @param state State of the object encoded as string
     */
    public void setState(String state) {
        try {
            JSONObject jo = new JSONObject(state);
            setState(jo);
            key = jo.optString("key");
            value = jo.optString("value");
            pConditionName = jo.optString("pConditionName");
        } catch (JSONException e) {
            PrivilegeManager.debug.error("AttributeLookupCondition.setState",e);
        }
    }

    /**
     * Returns state of the object.
     *
     * @return state of the object encoded as string.
     */
    public String getState() {
        return toString();
    }

    /**
     * Returns <code>ConditionDecision</code> of
     * <code>EntitlementCondition</code> evaluation.
     *
     * @param realm Realm name.
     * @param subject EntitlementCondition who is under evaluation.
     * @param resourceName Resource name.
     * @param environment Environment parameters.
     * @return <code>ConditionDecision</code> of
     * <code>EntitlementCondition</code> evaluation
     * @throws EntitlementException if error occurs.
     */
    public ConditionDecision evaluate(
        String realm,
        Subject subject,
        String resourceName,
        Map<String, Set<String>> environment)
        throws EntitlementException {
        String evalKey = null;

        // e.g. $USER.postaladdress;
        int idxUserMacro = key.indexOf(MACRO_USER);
        if (idxUserMacro != -1) {
            String attrName = key.substring(MACRO_USER.length() +1);
            evalKey = getAttributeFromSubject(subject, attrName);
        } else {
            evalKey = key;
        }


        // e.g. $RES.postaladdress;
        String searchKey = value.replace(MACRO_RESOURCE, resourceName);
        Set<String> evalValues = environment.get(searchKey);
        String evalVal = ((evalValues == null) || evalValues.isEmpty()) ?
            null : (String)evalValues.iterator().next();

        if ((evalKey == null) && (evalVal != null)) {
            return getFailedDecision(key, evalVal);
        } else if ((evalVal == null) && (evalKey != null)) {
            return getFailedDecision(value, evalKey);
        } else if ((evalVal == null) && (evalKey == null)) {
            return getFailedDecision(key, value);
        }

        return new ConditionDecision(
            evalValues.contains(evalKey), Collections.EMPTY_MAP);
    }

    private ConditionDecision getFailedDecision(String prefix, String suffix) {
        Map<String, Set<String>> advices = new HashMap<String, Set<String>>();
        Set<String> set = new HashSet<String>();
        set.add(prefix + "=" + suffix);
        advices.put(getClass().getName(), set);
        return new ConditionDecision(false, advices);
    }
    
    private String getAttributeFromSubject(Subject subject, String attrName) {
        Set publicCreds = subject.getPublicCredentials();
        String attrValue = null;
        
        for (Iterator i = publicCreds.iterator(); 
            i.hasNext() && (attrValue == null); ) {
            Object o = i.next();
            if (o instanceof String) {
                String v = (String)o;
                if (v.startsWith(attrName + "=")) {
                    attrValue = v.substring(attrName.length() + 1);
                }
            }
        }
        
        return attrValue;
    }

    /**
     * Returns matching key.
     *
     * @return matching key.
     */
    public String getKey() {
        return key;
    }

    /**
     * Returns OpenSSO policy Condition name.
     *
     * @return subject name as used in OpenSSO policy,
     *         this is releavant only when UserECondition was created from
     *         OpenSSO policy Condition.
     */
    public String getPConditionName() {
        return pConditionName;
    }

    /**
     * Returns matching value.
     *
     * @return matching value.
     */
    public String getValue() {
        return value;
    }

    /**
     * Sets matching key.
     *
     * @param key Matching key.
     */
    public void setKey(String key) {
        this.key = key;
    }

    /**
     * Sets OpenSSO policy Condition name
     * @param pConditionName subject name as used in OpenSSO policy,
     *        this is releavant only when UserECondition was created from
     *        OpenSSO policy Condition.
     */
    public void setPConditionName(String pConditionName) {
        this.pConditionName = pConditionName;
    }

    /**
     * Set matching value.
     * @param value Matching value.
     */
    public void setValue(String value) {
        this.value = value;
    }

    /**
     * Returns JSONObject mapping of the object.
     *
     * @return JSONObject mapping  of the object.
     */
    public JSONObject toJSONObject() throws JSONException {
        JSONObject jo = new JSONObject();
        toJSONObject(jo);
        jo.put("key", key);
        jo.put("value", value);
        jo.put("pConditionName", pConditionName);
        return jo;
    }

    /**
     * Returns <code>true</code> if the passed in object is equal to this object
     * @param obj object to check for equality
     * @return <code>true</code> if the passed in object is equal to this object
     */
    @Override
    public boolean equals(Object obj) {
        if (!super.equals(obj)) {
            return false;
        }
        if (!getClass().equals(obj.getClass())) {
            return false;
        }
        AttributeLookupCondition object = (AttributeLookupCondition) obj;
        if (key == null) {
            if (object.key != null) {
                return false;
            }
        } else {
            if (!key.equals(object.key)) {
                return false;
            }
        }
        if (value == null) {
            if (object.value != null) {
                return false;
            }
        } else {
            if (!value.equals(object.value)) {
                return false;
            }
        }
        if (pConditionName == null) {
            if (object.pConditionName != null) {
                return false;
            }
        } else {
            if (!pConditionName.equals(object.pConditionName)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns hash code of the object.
     *
     * @return hash code of the object.
     */
    @Override
    public int hashCode() {
        int code = super.hashCode();
        if (key != null) {
            code += key.hashCode();
        }
        if (value != null) {
            code += value.hashCode();
        }
        if (pConditionName != null) {
            code += pConditionName.hashCode();
        }
        return code;
    }

    /**
     * Returns string representation of the object.
     * 
     * @return string representation of the object.
     */
    @Override
    public String toString() {
        String s = null;
        try {
            s = toJSONObject().toString(2);
        } catch (JSONException e) {
            PrivilegeManager.debug.error("AttributeLookupCondition.setState",e);
        }
        return s;
    }

}
