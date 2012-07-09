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
 * $Id: NotSubject.java,v 1.1 2009/08/19 05:40:33 veiming Exp $
 */
package com.sun.identity.entitlement;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.security.auth.Subject;
import org.json.JSONObject;
import org.json.JSONException;

/**
 * This class wrapped on an Entitlement Subject object to provide boolean
 * NOT.
 * Membership of <code>NotSubject</code> is satisfied in the user is not a
 * member of the nested <code>EntitlementSubject</code>.
 */
public class NotSubject implements EntitlementSubject {
    private EntitlementSubject eSubject;
    private String pSubjectName;

    /**
     * Constructs NotSubject
     */
    public NotSubject() {
    }

    /**
     * Constructs NotSubject
     * @param eSubject nested EntitlementSubject
     */
    public NotSubject(EntitlementSubject eSubject) {
        this.eSubject = eSubject;
    }

    /**
     * Constructs NotSubject
     * @param eSubject eSubject nested EntitlementSubject
     * @param pSubjectName subject name as used in OpenSSO policy,
     * this is releavant only when NotrESubject was created from
     * OpenSSO policy Subject
     */
    public NotSubject(EntitlementSubject eSubject, String pSubjectName) {
        this.eSubject = eSubject;
        this.pSubjectName = pSubjectName;
    }

    /**
     * Sets state of the object
     * @param state State of the object encoded as string
     */
    public void setState(String state) {
        try {
            JSONObject jo = new JSONObject(state);
            JSONObject memberSubject = jo.optJSONObject("memberESubject");
            if (memberSubject != null) {
                String className = memberSubject.getString("className");
                Class cl = Class.forName(className);
                eSubject = (EntitlementSubject) cl.newInstance();
                eSubject.setState(memberSubject.getString("state"));

            }
            pSubjectName = jo.has("pSubjectName") ?
                jo.optString("pSubjectName") : null;
        } catch (JSONException e) {
            PrivilegeManager.debug.error("NotSubject.setState", e);
        } catch (InstantiationException e) {
            PrivilegeManager.debug.error("NotSubject.setState", e);
        } catch (ClassNotFoundException e) {
            PrivilegeManager.debug.error("NotSubject.setState", e);
        } catch (IllegalAccessException e) {
            PrivilegeManager.debug.error("NotSubject.setState", e);
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
     * Returns <code>SubjectDecision</code> of
     * <code>EntitlementSubject</code> evaluation.
     *
     * @param realm Realm name.
     * @param subject EntitlementSubject who is under evaluation.
     * @param resourceName Resource name.
     * @param environment Environment parameters.
     * @return <code>SubjectDecision</code> of
     * <code>EntitlementSubject</code> evaluation
     * @throws  EntitlementException if any errors occur.
     */
    public SubjectDecision evaluate(
        String realm,
        SubjectAttributesManager mgr,
        Subject subject,
        String resourceName,
        Map<String, Set<String>> environment
    ) throws EntitlementException {
        if (eSubject == null) {
            return new SubjectDecision(false, Collections.EMPTY_MAP);
        }

        SubjectDecision d = eSubject.evaluate(realm, mgr, subject,
            resourceName, environment);
        return new SubjectDecision(!d.isSatisfied(), Collections.EMPTY_MAP);
    }

    /**
     * Sets nested EntitlementSubject.
     *
     * @param eSubject nested EntitlementSubject.
     */
    public void setESubject(EntitlementSubject eSubject) {
        this.eSubject = eSubject;
    }

    /**
     * Returns nested EntitlementSubject.
     *
     * @return nested EntitlementSubject.
     */
    public EntitlementSubject getESubject() {
        return eSubject;
    }

    /**
     * Sets OpenSSO policy Subject name
     * @param pSubjectName subject name as used in OpenSSO policy,
     * this is releavant only when NotrESubject was created from
     * OpenSSO policy Subject
     */
    public void setPSubjectName(String pSubjectName) {
        this.pSubjectName = pSubjectName;
    }

    /**
     * Returns OpenSSO policy Subject name
     * @return subject name as used in OpenSSO policy,
     * this is releavant only when NotrESubject was created from
     * OpenSSO policy Subject
     */
    public String getPSubjectName() {
        return pSubjectName;
    }

    /**
     * Returns JSONObject mapping of the object
     * @return JSONObject mapping of the object
     * @throws org.json.JSONException if can not map to JSONObject
     */
    public JSONObject toJSONObject() throws JSONException {
        JSONObject jo = new JSONObject();
        jo.put("pSubjectName", pSubjectName);

        if (eSubject != null) {
            JSONObject subjo = new JSONObject();
            subjo.put("className", eSubject.getClass().getName());
            subjo.put("state", eSubject.getState());
            jo.put("memberESubject", subjo);
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
            JSONObject jo = toJSONObject();
            s = (jo == null) ? super.toString() : jo.toString(2);
        } catch (JSONException e) {
            PrivilegeManager.debug.error("NotSubject.toString()", e);
        }
        return s;
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
        NotSubject object = (NotSubject) obj;
        if (eSubject == null) {
            if (object.getESubject() != null) {
                return false;
            }
        } else {
            if (!eSubject.equals(object.getESubject())) {
                return false;
            }
        }
        if (pSubjectName == null) {
            if (object.getPSubjectName() != null) {
                return false;
            }
        } else {
            if (!pSubjectName.equals(object.getPSubjectName())) {
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
        if (eSubject != null) {
            code += eSubject.hashCode();
        }
        if (pSubjectName != null) {
            code += pSubjectName.hashCode();
        }
        return code;
    }

    /**
     * Returns search index attributes.
     *
     * @return search index attributes.
     */
    public Map<String, Set<String>> getSearchIndexAttributes() {
        Map<String, Set<String>> map = new HashMap<String, Set<String>>();
        Set<String> set = new HashSet<String>();
        set.add(SubjectAttributesCollector.ATTR_NAME_ALL_ENTITIES);
        map.put(SubjectAttributesCollector.NAMESPACE_IDENTITY, set);
        return map;
    }

    /**
     * Returns required attribute names.
     *
     * @return required attribute names.
     */
    public Set<String> getRequiredAttributeNames() {
        return (Collections.EMPTY_SET);
    }

    /**
     * Returns <code>true</code> is this subject is an identity object.
     *
     * @return <code>true</code> is this subject is an identity object.
     */
    public boolean isIdentity() {
        return (eSubject != null) ? eSubject.isIdentity() : false;
    }
}
