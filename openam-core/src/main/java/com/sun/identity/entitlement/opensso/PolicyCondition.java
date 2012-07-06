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
 * $Id: PolicyCondition.java,v 1.2 2010/01/08 22:12:49 farble1670 Exp $
 */

/*
 * Portions Copyrighted [2010] [ForgeRock AS]
 */

package com.sun.identity.entitlement.opensso;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.entitlement.ConditionDecision;
import com.sun.identity.entitlement.EntitlementConditionAdaptor;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.PrivilegeManager;
import com.sun.identity.policy.PolicyException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import javax.security.auth.Subject;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * This condition wraps all OpenSSO policy condition.
 */

public class PolicyCondition extends  EntitlementConditionAdaptor {
    private String className;
    private String name;
    private Map<String, Set<String>> properties;

    public PolicyCondition() {
    }
    
    /**
     * Constructor.
     *
     * @param name Name of condition.
     * @param className Implementation class name.
     * @param properties Properties of the condition.
     */
    public PolicyCondition(
        String name,
        String className,
        Map<String, Set<String>> properties) {
		this.name = name;
        this.className = className;
        this.properties = properties;
    }

    /**
     * Returns class name.
     *
     * @return class name.
     */
    public String getClassName() {
        return className;
    }

    /**
     * Returns name.
     *
     * @return name.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns properties.
     *
     * @return properties.
     */
    public Map<String, Set<String>> getProperties() {
        return properties;
    }

    /**
     * Sets the state of this condition.
     *
     * @param state State of this condition.
     */
    public void setState(String state) {
        try {
            JSONObject jo = new JSONObject(state);
            setState(jo);
            this.name = jo.optString("name");
            this.className = jo.optString("className");
            this.properties = getProperties((JSONObject)jo.opt("properties"));
        } catch (JSONException e) {
            PrivilegeManager.debug.error("PolicyCondition.setState", e);
        }
    }

    private Map<String, Set<String>> getProperties(JSONObject jo) 
        throws JSONException {
        Map<String, Set<String>> result = new HashMap<String, Set<String>>();
        for (Iterator i = jo.keys(); i.hasNext(); ) {
            String key = (String)i.next();
            JSONArray arr = (JSONArray)jo.opt(key);
            Set set = new HashSet<String>();
            result.put(key, set);

            for (int j = 0; j < arr.length(); j++) {
                set.add(arr.getString(j));
            }
        }
        return result;
    }

    /**
     * Returns state of this condition.
     *
     * @return state of this condition.
     */
    public String getState() {
        JSONObject jo = new JSONObject();

        try {
            toJSONObject(jo);
            jo.put("className", className);
            jo.put("name", name);
            jo.put("properties", properties);
            return jo.toString(2);
        } catch (JSONException ex) {
            PrivilegeManager.debug.error("PolicyCondition.getState", ex);
        }
        return "";
    }

    /**
     * Returns condition decision.
     *
     * @param realm Realm name.
     * @param subject Subject to be evaluated.
     * @param resourceName Resource name.
     * @param environment Environment map.
     * @return condition decision.
     * @throws com.sun.identity.entitlement.EntitlementException if error occur.
     */
    public ConditionDecision evaluate(
        String realm,
        Subject subject,
        String resourceName,
        Map<String, Set<String>> environment
    ) throws EntitlementException {
        try {
            com.sun.identity.policy.interfaces.Condition cond =
                (com.sun.identity.policy.interfaces.Condition)
                Class.forName(className).newInstance();
            cond.setProperties(properties);
            SSOToken token = (subject != null) ? getSSOToken(subject) : null;
            com.sun.identity.policy.ConditionDecision dec =
                cond.getConditionDecision(token, environment);
            return new ConditionDecision(dec.isAllowed(), dec.getAdvices(), dec.getTimeToLive());
        } catch (SSOException ex) {
            throw new EntitlementException(510, ex);
        } catch (PolicyException ex) {
            throw new EntitlementException(510, ex);
        } catch (ClassNotFoundException ex) {
            throw new EntitlementException(510, ex);
        } catch (InstantiationException ex) {
            throw new EntitlementException(510, ex);
        } catch (IllegalAccessException ex) {
            throw new EntitlementException(510, ex);
        }
    }

    private static SSOToken getSSOToken(Subject subject) {
        Set privateCred = subject.getPrivateCredentials();
        for (Iterator i = privateCred.iterator(); i.hasNext(); ) {
            Object o = i.next();
            if (o instanceof SSOToken) {
                return (SSOToken)o;
            }
        }
        return null;
    }

    @Override
    public boolean equals(Object obj) {
        if (!super.equals(obj)) {
            return false;
        }
        if (!getClass().equals(obj.getClass())) {
            return false;
        }
        PolicyCondition other = (PolicyCondition)obj;
        if (!compareString(this.className, other.className)) {
            return false;
        }
        if (!compareString(this.name, other.name)) {
            return false;
        }
        return compareMap(this.properties, other.properties);
    }

	@Override
	public String getDisplayType() {
		return "policy";
	}
}
