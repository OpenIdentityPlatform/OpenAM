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
 * $Id: OpenSSOPrivilege.java,v 1.5 2009/10/07 01:36:55 veiming Exp $
 */

/*
 * Portions Copyrighted 2010 ForgeRock Inc
 * Portions Copyrighted 2013 Nomura Research Institute, Ltd
 */

package com.sun.identity.entitlement.opensso;

import com.sun.identity.entitlement.ConditionDecision;
import com.sun.identity.entitlement.Entitlement;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.Privilege;
import com.sun.identity.entitlement.PrivilegeManager;
import com.sun.identity.entitlement.PrivilegeType;
import com.sun.identity.entitlement.util.NetworkMonitor;
import com.sun.identity.session.util.RestrictedTokenAction;
import com.sun.identity.session.util.RestrictedTokenContext;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.security.auth.Subject;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * 
 */
public class OpenSSOPrivilege extends Privilege {
    private static final NetworkMonitor EVAL_SINGLE_LEVEL_MONITOR =
        NetworkMonitor.getInstance("privilegeSingleLevelEvaluation");
    private static final NetworkMonitor EVAL_SUB_TREE_MONITOR =
        NetworkMonitor.getInstance("privilegeSubTreeEvaluation");
    private String policyName;

    public OpenSSOPrivilege() {
       super();
    }

    @Override
    public PrivilegeType getType() {
        return PrivilegeType.OPENSSO;
    }

    @Override
    public List<Entitlement> evaluate(
        final Subject adminSubject,
        final String realm,
        final Subject subject,
        final String applicationName,
        final String resourceName,
        final Set<String> actionNames,
        final Map<String, Set<String>> environment,
        final boolean recursive,
        final Object context
    ) throws EntitlementException {
        List<Entitlement> results = null;
        
        try {
            results = (List<Entitlement>) RestrictedTokenContext.doUsing(context,
                            new RestrictedTokenAction() {
                                public Object run() throws Exception {
                                    return internalEvaluate(
                                                    adminSubject,
                                                    realm,
                                                    subject,
                                                    applicationName,
                                                    resourceName,
                                                    actionNames,
                                                    environment,
                                                    recursive
                                            );
                                }
                            });
        } catch (Exception ex) {
            PrivilegeManager.debug.error("OpenSSOPrivilege.evaluate", ex);
            results = new ArrayList<Entitlement>(0);
        }
        
        return results;
    }

    private List<Entitlement> internalEvaluate(
        Subject adminSubject,
        String realm,
        Subject subject,
        String applicationName,
        String resourceName,
        Set<String> actionNames,
        Map<String, Set<String>> environment,
        boolean recursive
    ) throws EntitlementException {
        long start = (recursive) ? EVAL_SUB_TREE_MONITOR.start() :
            EVAL_SINGLE_LEVEL_MONITOR.start();
        List<Entitlement> results = new ArrayList<Entitlement>();
        Set<ConditionDecision> decisions = new HashSet();

        if (!isActive()) {
            Entitlement origE = getEntitlement();
            Entitlement e = new Entitlement(origE.getApplicationName(),
                origE.getResourceName(), Collections.EMPTY_SET);
            results.add(e);
            return results;
        }

        Map<String, Set<String>> advices = new HashMap<String, Set<String>>();
        if (doesSubjectMatch(adminSubject, realm, advices, subject,
            resourceName, environment) &&
            doesConditionMatch(realm, advices, subject, resourceName,
            environment, decisions)
            ) {
            Entitlement origE = getEntitlement();
            Set<String> resources = origE.evaluate(adminSubject, realm,
                subject, applicationName, resourceName, actionNames,
                environment, recursive);

            if (PrivilegeManager.debug.messageEnabled()) {
                PrivilegeManager.debug.message(
                    "[PolicyEval] OpenSSOPrivilege.evaluate: resources=" +
                    resources.toString(), null);
            }
            for (String r : resources) {
                Entitlement e = new Entitlement(origE.getApplicationName(),
                    r, origE.getActionValues());
                e.setAttributes(getAttributes(adminSubject, realm, subject,
                    resourceName, environment));
                e.setAdvices(advices);
                e.setTTL(getLowestDecisionTTL(decisions));
                results.add(e);
            }
        } else {
            Entitlement origE = getEntitlement();
            Entitlement e = new Entitlement(origE.getApplicationName(),
                origE.getResourceName(), Collections.EMPTY_SET);
            e.setAdvices(advices);
            e.setTTL(getLowestDecisionTTL(decisions));
            results.add(e);
        }

        if (recursive) {
            EVAL_SUB_TREE_MONITOR.end(start);
        } else {
            EVAL_SINGLE_LEVEL_MONITOR.end(start);
        }

        return results;
    }

     /**
     * Returns JSONObject mapping of  the object
     * @return JSONObject mapping of  the object
     * @throws JSONException if can not map to JSONObject
     */
    @Override
    public JSONObject toJSONObject() throws JSONException {
        JSONObject jo = super.toJSONObject();
        if (policyName != null) {
            jo.put("policyName", policyName);
        }
        return jo;
    }

    protected void init(JSONObject jo) {
        policyName = jo.optString("policyName");
    }

    /**
     * Sets policy name.
     *
     * @param policyName Policy name.
     */
    public void setPolicyName(String policyName) {
        this.policyName = policyName;
    }

    /**
     * Returns policy name.
     *
     * @return policyName Policy name.
     */
    public String getPolicyName() {
        return this.policyName;
    }

    protected long getLowestDecisionTTL(Set<ConditionDecision> decisions) {
        long minTTL = Long.MAX_VALUE;

        for (ConditionDecision decision : decisions) {
            if (minTTL > decision.getTimeToLive()) {
                minTTL = decision.getTimeToLive();
            }
        }

        return minTTL;
    }
}
