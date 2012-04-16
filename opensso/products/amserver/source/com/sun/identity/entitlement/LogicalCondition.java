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
 * $Id: LogicalCondition.java,v 1.1 2009/08/19 05:40:33 veiming Exp $
 */
package com.sun.identity.entitlement;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.security.auth.Subject;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

public abstract class LogicalCondition extends EntitlementConditionAdaptor {
    private Set<EntitlementCondition> eConditions;
    private String pConditionName;

    /**
     * Constructor.
     */
    public LogicalCondition() {
    }

    /**
     * Constructor.
     *
     * @param eConditions wrapped <code>EntitlementCondition</code>(s)
     */
    public LogicalCondition(Set<EntitlementCondition> eConditions) {
        setEConditions(eConditions);
    }

    /**
     * Constructor.
     *
     * @param eConditions wrapped <code>EntitlementCondition</code>(s)
     * @param pConditionName subject name as used in OpenSSO policy,
     * this is releavant only when UserECondition was created from
     * OpenSSO policy Condition
     */
    public LogicalCondition(
        Set<EntitlementCondition> eConditions,
        String pConditionName
    ) {
        this.pConditionName = pConditionName;
        setEConditions(eConditions);
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
            pConditionName = (jo.has("pConditionName")) ?
                jo.optString("pConditionName") : null;
            JSONArray memberConditions = jo.optJSONArray("memberECondition");
            if (memberConditions != null) {
                eConditions = new HashSet<EntitlementCondition>();
                int len = memberConditions.length();
                for (int i = 0; i < len; i++) {
                    JSONObject memberCondition =
                        memberConditions.getJSONObject(i);
                    String className = memberCondition.getString("className");
                    Class cl = Class.forName(className);
                    EntitlementCondition ec =
                        (EntitlementCondition) cl.newInstance();
                    ec.setState(memberCondition.getString("state"));
                    eConditions.add(ec);
                }
            }
        } catch (InstantiationException ex) {
            PrivilegeManager.debug.error("LogicalCondition.setState", ex);
        } catch (IllegalAccessException ex) {
            PrivilegeManager.debug.error("LogicalCondition.setState", ex);
        } catch (ClassNotFoundException ex) {
            PrivilegeManager.debug.error("LogicalCondition.setState", ex);
        } catch (JSONException ex) {
            PrivilegeManager.debug.error("LogicalCondition.setState", ex);
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
     * <code>EntitlementCondition</code> evaluation
     *
     * @param realm Realm name.
     * @param subject EntitlementCondition who is under evaluation.
     * @param resourceName Resource name.
     * @param environment Environment parameters.
     * @return <code>ConditionDecision</code> of
     * <code>EntitlementCondition</code> evaluation
     * @throws EntitlementException if error occurs.
     */
    public abstract ConditionDecision evaluate(
        String realm,
        Subject subject,
        String resourceName,
        Map<String, Set<String>> environment
    ) throws EntitlementException;

    /**
     * Sets the nested <code>EntitlementCondition</code>(s).
     *
     * @param eConditions the nested <code>EntitlementCondition</code>(s)
     */
    public void setEConditions(Set<EntitlementCondition> eConditions) {
        this.eConditions = new HashSet<EntitlementCondition>();
        if (eConditions != null) {
            this.eConditions.addAll(eConditions);
        }
    }

    /**
     * Returns the nested <code>EntitlementCondition</code>(s).
     *
     * @return the nested <code>EntitlementCondition</code>(s).
     */
    public Set<EntitlementCondition> getEConditions() {
        return eConditions;
    }

    /**
     * Sets OpenSSO policy Condition name
     * @param pConditionName subject name as used in OpenSSO policy,
     * this is releavant only when UserECondition was created from
     * OpenSSO policy Condition
     */
    public void setPConditionName(String pConditionName) {
        this.pConditionName = pConditionName;
    }

    /**
     * Returns OpenSSO policy Condition name
     * @return  subject name as used in OpenSSO policy,
     * this is releavant only when UserECondition was created from
     * OpenSSO policy Condition
     */
    public String getPConditionName() {
        return pConditionName;
    }

    /**
     * Returns JSONObject mapping of the object
     * @return JSONObject mapping of the object
     * @throws org.json.JSONException if can not map to JSONObject
     */
    public JSONObject toJSONObject() throws JSONException {
        JSONObject jo = new JSONObject();
        toJSONObject(jo);
        jo.put("pConditionName", pConditionName);

        if ((eConditions != null) && !eConditions.isEmpty()) {
            for (EntitlementCondition eCondition : eConditions) {
                JSONObject subjo = new JSONObject();
                subjo.put("className", eCondition.getClass().getName());
                subjo.put("state", eCondition.getState());
                jo.append("memberECondition", subjo);
            }
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
            PrivilegeManager.debug.error("LogicalCondition.toString()", e);
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
        if (!super.equals(obj)) {
            return false;
        }
        LogicalCondition object = (LogicalCondition) obj;
        if (eConditions == null) {
            if (object.getEConditions() != null) {
                return false;
            }
        } else { // eConditions not null
            if ((object.getEConditions()) == null) {
                return false;
            } else if (!eConditions.containsAll(object.getEConditions())) {
                return false;
            } else if (!object.getEConditions().containsAll(eConditions)) {
                return false;
            }
        }
        if (pConditionName == null) {
            if (object.getPConditionName() != null) {
                return false;
            }
        } else {
            if (!pConditionName.equals(object.getPConditionName())) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns hash code of the object
     * @return hash code of the object
     */
    @Override
    public int hashCode() {
        int code = super.hashCode();
        if (eConditions != null) {
            for (EntitlementCondition eCondition : eConditions) {
                code += eCondition.hashCode();
            }
        }
        if (pConditionName != null) {
            code += pConditionName.hashCode();
        }
        return code;
    }
}
