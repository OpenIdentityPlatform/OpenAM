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
 * $Id: PolicySubject.java,v 1.1 2009/08/19 05:40:36 veiming Exp $
 */

package com.sun.identity.entitlement.opensso;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.EntitlementSubject;
import com.sun.identity.entitlement.PrivilegeManager;
import com.sun.identity.entitlement.SubjectAttributesCollector;
import com.sun.identity.entitlement.SubjectAttributesManager;
import com.sun.identity.entitlement.SubjectDecision;
import com.sun.identity.policy.PolicyException;
import com.sun.identity.policy.PolicyManager;
import com.sun.identity.security.AdminTokenAction;
import java.security.AccessController;
import java.util.Collections;
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
 * This subject wraps all OpenSSO policy subject.
 */
public class PolicySubject implements EntitlementSubject {
    private String name;
    private String className;
    private Set<String> values;
    private boolean exclusive;

    public PolicySubject() {
    }

    /**
     * Constructor.
     *
     * @param name Name of condition.
     * @param className Implementation class name.
     * @param values Values of this subject.
     * @param exclusive <code>true</code> to be exclusive.
     */
    public PolicySubject(
        String name,
        String className,
        Set<String> values,
        boolean exclusive
    ) {
        this.name = name;
        this.className = className;
        this.values = values;
        this.exclusive = exclusive;
    }

    /**
     * Returns name.
     * @return name.
     */
    public String getName() {
        return name;
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
     * Returns values.
     *
     * @return values.
     */
    public Set<String> getValues() {
        return values;
    }

    /**
     * Returns <code>true</code> if this is an exclusive subject.
     *
     * @return <code>true</code> if this is an exclusive subject.
     */
    public boolean isExclusive() {
        return exclusive;
    }

    /**
     * Sets states
     *
     * @param state State.
     */
    public void setState(String state) {
        try {
            JSONObject jo = new JSONObject(state);
            this.name = jo.optString("name");
            this.className = jo.optString("className");
            this.exclusive = jo.optBoolean("exclusive");
            this.values = getValues((JSONArray)jo.opt("values"));
        } catch (JSONException ex) {
            PrivilegeManager.debug.error("PolicySubject.setState", ex);
        }
    }

    private Set<String> getValues(JSONArray jo)
        throws JSONException {
        Set<String> result = new HashSet<String>();
        for (int i = 0; i < jo.length(); i++) {
            result.add(jo.getString(i));
        }
        return result;
    }

    /**
     * Returns state of this subject.
     * 
     * @return state of this subject.
     */
    public String getState() {
        JSONObject jo = new JSONObject();

        try {
            jo.put("name", name);
            jo.put("className", className);
            jo.put("exclusive", exclusive);
            jo.put("values", values);
            return jo.toString(2);
        } catch (JSONException ex) {
            PrivilegeManager.debug.error("PolicySubject.getState", ex);
        }
        return "";
    }

    /**
     * Returns search index attributes.
     *
     * @return search index attributes.
     */
    public Map<String, Set<String>> getSearchIndexAttributes() {
        Map<String, Set<String>> map = new HashMap<String, Set<String>>(4);
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
        return(Collections.EMPTY_SET);
    }

    /**
     * Returns subject decision.
     *
     * @param realm Realm name.
     * @param mgr Subject attribute manager
     * @param subject Subject to be evaluated.
     * @param resourceName Resource name to be evaluated.
     * @param environment Environment map.
     * @return subject decision.
     * @throws com.sun.identity.entitlement.EntitlementException if error
     *         occurs.
     */
    public SubjectDecision evaluate(
        String realm,
        SubjectAttributesManager mgr,
        Subject subject,
        String resourceName,
        Map<String, Set<String>> environment
    ) throws EntitlementException {
        SSOToken adminToken = (SSOToken) AccessController.doPrivileged(
            AdminTokenAction.getInstance());

        try {
            PolicyManager pm = new PolicyManager(adminToken, realm);
            com.sun.identity.policy.interfaces.Subject sbj =
                (com.sun.identity.policy.interfaces.Subject)
                Class.forName(className).newInstance();
            sbj.initialize(pm.getPolicyConfig());
            sbj.setValues(values);
            SSOToken token = getSSOToken(subject);
            boolean result = (token == null) ? true 
                    : sbj.isMember(token) ^ exclusive;
            return new SubjectDecision(result, Collections.EMPTY_MAP);
        } catch (SSOException ex) {
            throw new EntitlementException(508, ex);
        } catch (PolicyException ex) {
            throw new EntitlementException(508, ex);
        } catch (ClassNotFoundException ex) {
            throw new EntitlementException(508, ex);
        } catch (InstantiationException ex) {
            throw new EntitlementException(508, ex);
        } catch (IllegalAccessException ex) {
            throw new EntitlementException(508, ex);
        }
    }

    private static SSOToken getSSOToken(Subject subject) {
        // subject could be null, a case in point: evaluation ignoring subjects
        if (subject == null) {
            return null;
        }
        Set privateCred = subject.getPrivateCredentials();
        for (Iterator i = privateCred.iterator(); i.hasNext(); ) {
            Object o = i.next();
            if (o instanceof SSOToken) {
                return (SSOToken)o;
            }
        }
        return null;
    }
    
    /**
     * Returns <code>true</code> is this subject is an identity object.
     *
     * @return <code>true</code> is this subject is an identity object.
     */
    public boolean isIdentity() {
        return true;
    }
}
