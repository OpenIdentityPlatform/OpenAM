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
 * $Id: LogicalSubject.java,v 1.1 2009/08/19 05:40:33 veiming Exp $
 */
package com.sun.identity.entitlement;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Base class for <code>OrSubejct</code> and <code>AndSubejct</code>.
 */
public abstract class LogicalSubject implements EntitlementSubject {
    private Set<EntitlementSubject> eSubjects;
    private String pSubjectName;

    /**
     * Constructor.
     */
    public LogicalSubject() {
    }

    /**
     * Constructor.
     *
     * @param eSubjects wrapped EntitlementSubject(s)
     */
    public LogicalSubject(Set<EntitlementSubject> eSubjects) {
        this.eSubjects = eSubjects;
    }

    /**
     * Constructor.
     *
     * @param eSubjects wrapped EntitlementSubject(s)
     * @param pSubjectName subject name as used in OpenSSO policy,
     * this is releavant only when UserESubject was created from
     * OpenSSO policy Subject
     */
    public LogicalSubject(
        Set<EntitlementSubject> eSubjects,
        String pSubjectName
    ) {
        this.eSubjects = eSubjects;
        this.pSubjectName = pSubjectName;
    }

    /**
     * Sets state of the object
     * @param state State of the object encoded as string
     */
    public void setState(String state) {
        try {
            JSONObject jo = new JSONObject(state);
            JSONArray memberSubjects = jo.optJSONArray("memberESubjects");
            if(memberSubjects != null) {
                eSubjects = new HashSet<EntitlementSubject>();
                int len = memberSubjects.length();
                for (int i = 0; i < len; i++) {
                    JSONObject memberSubject = memberSubjects.getJSONObject(i);
                    String className = memberSubject.getString("className");
                    Class cl = Class.forName(className);
                    EntitlementSubject es = (EntitlementSubject)cl.newInstance();
                    es.setState(memberSubject.getString("state"));
                    eSubjects.add(es);
                }
            }
            if (jo.optString("pSubjectName").length() > 0) {
                pSubjectName = jo.optString(pSubjectName);
            } else {
                pSubjectName = null;
            }
        } catch (JSONException e) {
            PrivilegeManager.debug.error("LogicalSubject.setState", e);
        } catch (InstantiationException e) {
            PrivilegeManager.debug.error("LogicalSubject.setState", e);
        } catch (ClassNotFoundException e) {
            PrivilegeManager.debug.error("LogicalSubject.setState", e);
        } catch (IllegalAccessException e) {
            PrivilegeManager.debug.error("LogicalSubject.setState", e);
        }
    }

    /**
     * Returns state of the object
     * @return state of the object encoded as string
     */
    public String getState() {
        return toString();
    }

    /**
     * Sets the nested EntitlementSubject(s)
     * @param eSubjects the nested EntitlementSubject(s)
     */
    public void setESubjects(Set<EntitlementSubject> eSubjects) {
        this.eSubjects = eSubjects;
    }

    /**
     * Returns the nested EntitlementSubject(s)
     * @return  the nested EntitlementSubject(s)
     */
    public Set<EntitlementSubject> getESubjects() {
        return eSubjects;
    }

    /**
     * Sets OpenSSO policy Subject name
     * @param pSubjectName subject name as used in OpenSSO policy,
     * this is releavant only when UserESubject was created from
     * OpenSSO policy Subject
     */
    public void setPSubjectName(String pSubjectName) {
        this.pSubjectName = pSubjectName;
    }

    /**
     * Returns OpenSSO policy Subject name
     * @return  subject name as used in OpenSSO policy,
     * this is releavant only when UserESubject was created from
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
        if (pSubjectName != null) {
            jo.put("pSubjectName", pSubjectName);
        }
        if (eSubjects == null) {
            return jo;
        }
        for (EntitlementSubject eSubject : eSubjects) {
            JSONObject subjo = new JSONObject();
            subjo.put("className", eSubject.getClass().getName());
            subjo.put("state", eSubject.getState());
            jo.append("memberESubjects", subjo);
        }
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
            JSONObject jo = toJSONObject();
            s = (jo == null) ? super.toString() : jo.toString(2);
        } catch (JSONException e) {
            PrivilegeManager.debug.error("LogicalSubject.toString", e);
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
        LogicalSubject object = (LogicalSubject) obj;
        if (eSubjects == null) {
            if (object.getESubjects() != null) {
                return false;
            }
        } else { // eSubjects not null
            if ((object.getESubjects()) == null) {
                return false;
            } else if (!eSubjects.containsAll(object.getESubjects())) {
                return false;
            } else if (!object.getESubjects().containsAll(eSubjects)) {
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
        if (eSubjects != null) {
            for (EntitlementSubject eSubject : eSubjects) {
                code += eSubject.hashCode();
            }
        }
        if (pSubjectName != null) {
            code += pSubjectName.hashCode();
        }
        return code;
    }

    /**
     * Returns the search index attributes.
     *
     * @return the search index attributes.
     */
    public Map<String, Set<String>> getSearchIndexAttributes() {
        Map<String, Set<String>> results = new HashMap<String, Set<String>>();
        if (eSubjects == null) {
            return results;
        }
        
        for (EntitlementSubject e : eSubjects) {
            Map<String, Set<String>> map = e.getSearchIndexAttributes();
            if ((map != null) && !map.isEmpty()) {
                for (String s : map.keySet()) {
                    Set<String> val = map.get(s);

                    if (s.equals(SubjectAttributesCollector.NAMESPACE_IDENTITY)
                        &&
                        val.contains(
                        SubjectAttributesCollector.ATTR_NAME_ALL_ENTITIES)) {
                        return createSearchAllIndexMap();
                    }

                    Set<String> set = results.get(s);
                    if (set == null) {
                        set = new HashSet<String>();
                        results.put(s, set);
                    }
                    set.addAll(val);
                }
            }
        }
        return results;
    }

    private Map<String, Set<String>> createSearchAllIndexMap() {
        Map<String, Set<String>> map = new HashMap<String, Set<String>>();
        Set<String> set = new HashSet<String>();
        set.add(SubjectAttributesCollector.ATTR_NAME_ALL_ENTITIES);
        map.put(SubjectAttributesCollector.NAMESPACE_IDENTITY, set);
        return map;
    }

    /**
     * Returns the required attribute names.
     *
     * @return the required attribute names.
     */
    public Set<String> getRequiredAttributeNames() {
        Set<String> results = new HashSet<String>();
        if (eSubjects == null) {
            return results;
        }
        for (EntitlementSubject e : eSubjects) {
            results.addAll(e.getRequiredAttributeNames());
        }
        return results;
    }

   /**
     * Returns <code>true</code> is this subject is an identity object.
     *
     * @return <code>true</code> is this subject is an identity object.
     */
    public boolean isIdentity() {
        if ((eSubjects != null) && !eSubjects.isEmpty()) {
            for (EntitlementSubject e : eSubjects) {
                if (e.isIdentity()) {
                    return true;
                }
            }
        }
        return false;
    }
}
