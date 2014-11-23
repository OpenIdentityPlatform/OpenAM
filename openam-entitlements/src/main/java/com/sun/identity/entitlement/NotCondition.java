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
 * $Id: NotCondition.java,v 1.1 2009/08/19 05:40:33 veiming Exp $
 */

/**
 * Portions copyright 2014 ForgeRock AS.
 */

package com.sun.identity.entitlement;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.forgerock.util.Reject;
import org.json.JSONException;
import org.json.JSONObject;

import javax.security.auth.Subject;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * This class wrapped on an Entitlement Condition object to provide boolean
 * NOT.
 * Membership of <code>NotCondition</code> is satisfied in the user is not a
 * member of the nested <code>EntitlementCondition</code>.
 *
 * We @JsonIgnore getEConditions and setEConditions (NOTE the 's' on the end) so that
 * we don't indicate via JSON schema exposed that we take multiple condition types.
 *
 * We extend LogicalCondition but ensure that we are only allowing a single
 * {@link EntitlementCondition} to be referenced by this class.
 */
public class NotCondition extends LogicalCondition {
    private EntitlementCondition eCondition;
    private String pConditionName;

    /**
     * Constructs <code>NotCondition</code>
     */
    public NotCondition() {
    }

    /**
     * Constructs NotCondition
     *
     * @param eCondition wrapped <code>EntitlementCondition</code>(s)
     */
    public NotCondition(EntitlementCondition eCondition) {
        this.eCondition = eCondition;
    }

    /**
     * Constructs <code>NotCondition</code>.
     *
     * @param eConditions wrapped <code>EntitlementCondition</code>(s)
     * @param pConditionName subject name as used in OpenSSO policy,
     * this is releavant only when UserECondition was created from
     * OpenSSO policy Condition
     */
    public NotCondition(
        EntitlementCondition eConditions,
        String pConditionName
    ) {
        this.eCondition = eConditions;
        this.pConditionName = pConditionName;
    }

    /**
     * Sets state of the object
     *
     * @param state State of the object encoded as string
     */
    @Override
    public void setState(String state) {
        try {
            JSONObject jo = new JSONObject(state);
            setState(jo);
            pConditionName = (jo.has("pConditionName")) ?
                jo.optString("pConditionName") : null;

            JSONObject memberCondition = jo.optJSONObject("memberECondition");
            if (memberCondition != null) {
                String className = memberCondition.getString("className");
                Class cl = Class.forName(className);
                eCondition = (EntitlementCondition)cl.newInstance();
                eCondition.setState(memberCondition.getString("state"));
            }
        } catch (InstantiationException ex) {
            PrivilegeManager.debug.error("NotCondition.setState", ex);
        } catch (IllegalAccessException ex) {
            PrivilegeManager.debug.error("NotCondition.setState", ex);
        } catch (ClassNotFoundException ex) {
            PrivilegeManager.debug.error("NotCondition.setState", ex);
        } catch (JSONException ex) {
            PrivilegeManager.debug.error("NotCondition.setState", ex);
        }
    }

    /**
     * Returns state of the object.
     *
     * @return state of the object encoded as string.
     */
    @Override
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
    @Override
    public ConditionDecision evaluate(
        String realm,
        Subject subject,
        String resourceName,
        Map<String, Set<String>> environment
    ) throws EntitlementException {
        if (eCondition == null) {
            return new ConditionDecision(false, Collections.EMPTY_MAP);
        }

        ConditionDecision d = eCondition.evaluate(realm, subject, resourceName,
            environment);
        return new ConditionDecision(!d.isSatisfied(), Collections.EMPTY_MAP);
    }

    /**
     * Sets the nested <code>EntitlementCondition</code>(s).
     *
     * @param eCondition the nested <code>EntitlementCondition</code>(s)
     */
    public void setECondition(EntitlementCondition eCondition) {
        this.eCondition = eCondition;
    }

    /**
     * Returns the nested <code>EntitlementCondition</code>(s).
     *
     * @return the nested <code>EntitlementCondition</code>(s).
     */
    public EntitlementCondition getECondition() {
        return eCondition;
    }

    /**
     * Sets the nested <code>EntitlementCondition</code>(s).
     *
     * @param eConditions the nested <code>EntitlementCondition</code>(s)
     */
    @Override
    @JsonIgnore
    public void setEConditions(Set<EntitlementCondition> eConditions) {
        Reject.ifTrue(eConditions.size() > 1 || eConditions.size() < 1);

        eCondition = eConditions.iterator().next();
    }

    /**
     * Returns the nested <code>EntitlementCondition</code>(s).
     *
     * @return the nested <code>EntitlementCondition</code>(s).
     */
    @Override
    @JsonIgnore
    public Set<EntitlementCondition> getEConditions() {
        if (eCondition == null) {
            return null;
        }

        return Collections.singleton(eCondition);
    }

    /**
     * Sets OpenSSO policy Condition name
     * @param pConditionName subject name as used in OpenSSO policy,
     * this is releavant only when UserECondition was created from
     * OpenSSO policy Condition
     */
    @Override
    public void setPConditionName(String pConditionName) {
        this.pConditionName = pConditionName;
    }

    /**
     * Returns OpenSSO policy Condition name
     * @return  subject name as used in OpenSSO policy,
     * this is releavant only when UserECondition was created from
     * OpenSSO policy Condition
     */
    @Override
    public String getPConditionName() {
        return pConditionName;
    }

    /**
     * Returns JSONObject mapping of the object
     * @return JSONObject mapping of the object
     * @throws org.json.JSONException if can not map to JSONObject
     */
    @Override
    public JSONObject toJSONObject() throws JSONException {
        JSONObject jo = new JSONObject();
        toJSONObject(jo);
        jo.put("pConditionName", pConditionName);

        if (eCondition != null) {
            JSONObject subjo = new JSONObject();
            subjo.put("className", eCondition.getClass().getName());
            subjo.put("state", eCondition.getState());
            jo.put("memberECondition", subjo);
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
            PrivilegeManager.debug.error("NotCondition.toString()", e);
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
        if (!getClass().equals(obj.getClass())) {
            return false;
        }
        NotCondition object = (NotCondition) obj;

        if (eCondition == null) {
            if (object.eCondition != null) {
                return false;
            }
        } else {
            if (!eCondition.equals(object.eCondition)) {
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
     * Returns hash code of the object
     * @return hash code of the object
     */
    @Override
    public int hashCode() {
        int code = super.hashCode();
        
        if (eCondition != null) {
            code += eCondition.hashCode();
        }
        if (pConditionName != null) {
            code += pConditionName.hashCode();
        }
        return code;
    }

    @Override
    public void validate() throws EntitlementException {
        if (eCondition == null) {
            throw new EntitlementException(EntitlementException.PROPERTY_VALUE_NOT_DEFINED, "condition");
        }
    }
}
