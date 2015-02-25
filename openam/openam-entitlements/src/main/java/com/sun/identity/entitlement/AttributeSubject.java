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
 * $Id: AttributeSubject.java,v 1.1 2009/08/19 05:40:32 veiming Exp $
 */
package com.sun.identity.entitlement;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.security.auth.Subject;

public class AttributeSubject implements SubjectImplementation {
    private String value;
    private String id;
    private boolean exclusive;

    /**
     * Constructor
     */
    public AttributeSubject() {

    }

    /**
     * Constructor
     * @param id Id of the attribute
     * @param value Value of the attribute
     */
    public AttributeSubject(String id, String value) {
        this.value = value;
        this.id = id;
    }

    /**
     * Returns attribute value.
     *
     * @return attribute value
     */
    public String getValue() {
        return value;
    }

    /**
     * Sets attribute value.
     *
     * @param value attribute value
     */
    public void setValue(String value) {
        this.value = value;
    }

    /**
     * Sets the Identifier.
     *
     * @param id Identifier.
     */
    public void setID(String id) {
        this.id = id;
    }

    /**
     * Returns the Identifier.
     * @return Identifier.
     */
    public String getID() {
        return id;
    }

    /**
     * Returns <code>SubjectDecision</code> of
     * <code>EntitlementSubject</code> evaluation
     *
     * @param realm Realm name.
     * @param subject EntitlementSubject who is under evaluation.
     * @param resourceName Resource name.
     * @param environment Environment parameters.
     * @return <code>SubjectDecision</code> of
     * <code>EntitlementSubject</code> evaluation
     * @throws com.sun.identity.entitlement,  EntitlementException in case
     * of any error
     */
    public SubjectDecision evaluate(
        String realm,
        SubjectAttributesManager mgr,
        Subject subject,
        String resourceName,
        Map<String, Set<String>> environment)
        throws EntitlementException {

        boolean satified = false;
        Set publicCreds = subject.getPublicCredentials();
        if ((publicCreds != null) && !publicCreds.isEmpty()) {
            Map<String, Set<String>> attributes = (Map<String, Set<String>>)
                publicCreds.iterator().next();
            Set<String> values = attributes.get(
                SubjectAttributesCollector.NAMESPACE_ATTR + getID());
            satified = (values != null) ? values.contains(getValue()) : false;
        }
        satified = satified ^ isExclusive();
        return new SubjectDecision(satified, Collections.EMPTY_MAP);
    }

    /**
     * Sets state of the object
     * @param state State of the object encoded as string
     */
    public void setState(String state) {
        try {
            JSONObject jo = new JSONObject(state);
            id = jo.has("id") ? jo.optString("id") : null;
            value = jo.has("value") ?
                    jo.optString("value") : null;
            exclusive = jo.has("exclusive") ?
                    Boolean.parseBoolean(jo.optString("exclusive")) : false;
        } catch (JSONException e) {
            PrivilegeManager.debug.error("AttributeSubject.setState", e);
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
     * Returns JSONObject mapping of the object.
     *
     * @return JSONObject mapping  of the object.
     */
    public JSONObject toJSONObject() throws JSONException {
        JSONObject jo = new JSONObject();
        jo.put("id", id);
        jo.put("value", value);
        if (exclusive) {
            jo.put("exclusive", exclusive);
        }
        return jo;
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
            PrivilegeManager.debug.error("EntitlementSubjectImpl.toString", e);
        }
        return s;
    }

    /**
     * Returns search index attributes.
     *
     * @return search index attributes.
     */
    public Map<String, Set<String>> getSearchIndexAttributes() {
        Map<String, Set<String>> map = new HashMap<String, Set<String>>(4);
        Set<String> set = new HashSet<String>();
        set.add(getValue());
        map.put(SubjectAttributesCollector.NAMESPACE_ATTR + getID(), set);

        return map;
    }

    /**
     * Returns required attribute names.
     *
     * @return required attribute names.
     */
    public Set<String> getRequiredAttributeNames() {
        Set<String> set = new HashSet<String>(2);
        set.add(SubjectAttributesCollector.NAMESPACE_ATTR + getID());
        return set;
    }

    /**
     * Returns <code>true</code> is this subject is an identity object.
     *
     * @return <code>true</code> is this subject is an identity object.
     */
    public boolean isIdentity() {
        return true;
    }

    /**
     * Returns <code>true</code> for exclusive.
     *
     * @return <code>true</code> for exclusive.
     */
    public boolean isExclusive() {
        return exclusive;
    }

    /**
     * Sets exclusive.
     *
     * @param flag <code>true</code> for exclusive.
     */
    public void setExclusive(boolean flag) {
        exclusive = flag;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AttributeSubject that = (AttributeSubject) o;

        if (exclusive != that.exclusive) return false;
        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (value != null ? !value.equals(that.value) : that.value != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = value != null ? value.hashCode() : 0;
        result = 31 * result + (id != null ? id.hashCode() : 0);
        result = 31 * result + (exclusive ? 1 : 0);
        return result;
    }
}
