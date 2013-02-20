/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: StaticAttributes.java,v 1.2 2009/09/21 18:33:45 dillidorai Exp $
 */
/*
 * Portions Copyrighted 2012 ForgeRock Inc.
 */
package com.sun.identity.entitlement;

import com.sun.identity.shared.JSONUtils;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.security.auth.Subject;
import org.json.JSONObject;
import org.json.JSONException;

/**
 * Interface specification for entitlement <code>ResourceAttribute</code>
 */
public class StaticAttributes implements ResourceAttribute {
    private String propertyName;
    private Set<String> propertyValues;
    private String pResponseProviderName;

    /**
     * Constructs an instance of StaticAttributes
     */
    public StaticAttributes() {
        propertyValues = new HashSet<String>();
    }

    public String getPropertyName() {
        return propertyName;
    }

    public void setPropertyName(String propertyName) {
        this.propertyName = propertyName;
    }

    public Set<String> getPropertyValues() {
        return propertyValues;
    }

    public void setPropertyValues(Set<String> propertyValues) {
        this.propertyValues = new HashSet<String>();
        this.propertyValues.addAll(propertyValues);
    }


    /**
     * Returns resoruce attributes aplicable to the request
     *
     * @param adminSubject Subject who is performing the evaluation.
     * @param realm Realm name.
     * @param subject Subject who is under evaluation.
     * @param resourceName Resource name.
     * @param environment Environment parameters.
     * @return applicable resource attributes
     * @throws com.sun.identity.entitlement.EntitlementException
     * if can not get condition decision
     */
    public Map<String, Set<String>> evaluate(
        Subject adminSubject,
        String realm,
        Subject subject,
        String resourceName,
        Map<String, Set<String>> environment)
        throws EntitlementException {
        Map<String, Set<String>> map = new HashMap<String, Set<String>>();
        map.put(propertyName, propertyValues);
        return map;
    }

    /**
     * Returns the state of this object.
     *
     * @return state of this object.
     */
    public String getState() {
        try {
            return toJSONObject().toString();
        } catch (JSONException ex) {
            PrivilegeManager.debug.error("StaticAttribute.getState", ex);
            return "";
        }
    }

    /**
     * Sets the state of the object.
     *
     * @param s state of the object.
     */
    public void setState(String s) {
        if ((s != null) && (s.trim().length() > 0)) {
            try {
                JSONObject json = new JSONObject(s);
                propertyName = json.getString("propertyName");
                propertyValues = JSONUtils.getSet(json, "propertyValues");
                if (json.has("pResponseProviderName")) {
                    pResponseProviderName = json.getString("pResponseProviderName");
                }
            } catch (JSONException ex) {
                PrivilegeManager.debug.error("StaticAttribute.setState", ex);
            }
        }
    }

    /**
     * Returns JSONObject mapping of the object
     * @return JSONObject mapping  of the object
     */
    private JSONObject toJSONObject() throws JSONException {
        JSONObject jo = new JSONObject();
        jo.put("propertyName", propertyName);
        jo.put("propertyValues", propertyValues);
        jo.put("pResponseProviderName", pResponseProviderName);
        return jo;
    }

    /**
     * Returns string representation of the object
     * @return string representation of the object
     */
    @Override
    public String toString() {
        String s = null;
        try {
            s = toJSONObject().toString(2);
        } catch (JSONException e) {
            PrivilegeManager.debug.error("StaticAttributes.toString", e);
        }
        return s;
    }

    /**
     * Sets OpenSSO policy response provider name of the object
     * @param pResponseProviderName response provider name as used in OpenSSO
     *        policy, this is releavant only when StaticAttributes was created
     *        from OpenSSO policy Subject
     */
    public void setPResponseProviderName(String pResponseProviderName) {
        this.pResponseProviderName = pResponseProviderName;
    }

    /**
     * Returns OpenSSO policy response provider name of the object
     * @return response provider name as used in OpenSSO policy,
     * this is releavant only when StaticAttributes was created from
     * OpenSSO policy Subject
     */
    public String getPResponseProviderName() {
        return pResponseProviderName;
    }

    /**
     * Returns <code>true</code> if the passed in object is equal to this object
     * @param obj object to check for equality
     * @return  <code>true</code> if the passed in object is equal to this object
     */
    @Override
    public boolean equals(Object obj) {
        boolean equalled = true;
        if (obj == null) {
            return false;
        }
        if (!getClass().equals(obj.getClass())) {
            return false;
        }

        StaticAttributes object = (StaticAttributes) obj;
        if (propertyName == null) {
            if (object.propertyName != null) {
                return false;
            }
        } else {
            if (!propertyName.equals(object.propertyName)) {
                return false;
            }
        }

        if (propertyValues == null) {
            if (object.propertyValues != null) {
                return false;
            }
        } else {
            if (!propertyValues.equals(object.propertyValues)) {
                return false;
            }
        }

        if (pResponseProviderName == null) {
            if (object.getPResponseProviderName() != null) {
                return false;
            }
        } else {
            if (!pResponseProviderName.equals(
                    object.getPResponseProviderName())) {
                return false;
            }
        }
        return equalled;
    }

    /**
     * Returns hash code of the object
     * @return hash code of the object
     */
    @Override
    public int hashCode() {
        int code = 0;
        if (propertyName != null) {
            code += propertyName.hashCode();
        }
        if (propertyValues != null) {
            code += propertyValues.hashCode();
        }
        if (pResponseProviderName != null) {
             code += pResponseProviderName.hashCode();
        }
        return code;
    }
}
