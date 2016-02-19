/*
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
 *
 * Portions Copyrighted 2010-2016 ForgeRock AS.
 * Portions Copyrighted 2013 Nomura Research Institute, Ltd
 */

package com.sun.identity.entitlement.opensso;

import static com.iplanet.am.util.SystemProperties.isServerMode;
import static org.forgerock.openam.utils.Time.*;

import com.sun.identity.entitlement.ConditionDecision;
import com.sun.identity.entitlement.Entitlement;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.Privilege;
import com.sun.identity.entitlement.PrivilegeType;
import com.sun.identity.entitlement.SubjectDecision;
import com.sun.identity.monitoring.MonitoringUtil;
import com.sun.identity.session.util.RestrictedTokenAction;
import com.sun.identity.session.util.RestrictedTokenContext;
import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.openam.entitlement.PolicyConstants;
import org.forgerock.openam.entitlement.monitoring.PolicyMonitor;
import org.json.JSONException;
import org.json.JSONObject;

import javax.security.auth.Subject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 *
 */
public class OpenSSOPrivilege extends Privilege {

    private String policyName;

    private final PolicyMonitor policyMonitor;

    public OpenSSOPrivilege() {
        super();

        if (isServerMode()) {
            policyMonitor = InjectorHolder.getInstance(PolicyMonitor.class);
        } else {
            policyMonitor = null;
        }
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
            final String normalisedResourceName,
            final String requestedResourceName,
            final Set<String> actionNames,
            final Map<String, Set<String>> environment,
            final boolean recursive,
            final Object context
    ) throws EntitlementException {
        try {
            return RestrictedTokenContext.doUsing(context,
                    new RestrictedTokenAction<List<Entitlement>>() {

                        @Override
                        public List<Entitlement> run() throws Exception {
                            long startTime = currentTimeMillis();

                            List<Entitlement> entitlements = internalEvaluate(
                                    adminSubject, realm, subject, applicationName,
                                    normalisedResourceName, actionNames, environment, recursive);

                            if (MonitoringUtil.isRunning()) {
                                long duration = currentTimeMillis() - startTime;
                                policyMonitor.addEvaluation(policyName, duration, realm,
                                        applicationName, normalisedResourceName, subject);
                            }

                            return entitlements;
                        }

                    });
        } catch (Exception ex) {
            PolicyConstants.DEBUG.error("OpenSSOPrivilege.evaluate", ex);
        }

        return Collections.emptyList();
    }

    private List<Entitlement> internalEvaluate(
            Subject adminSubject, String realm, Subject subject, String applicationName,
            String resourceName, Set<String> actionNames, Map<String, Set<String>> environment,
            boolean recursive) throws EntitlementException {

        Entitlement originalEntitlement = getEntitlement();

        if (!isActive()) {
            Entitlement entitlement = new Entitlement(originalEntitlement.getApplicationName(),
                    originalEntitlement.getResourceName(), Collections.<String>emptySet());
            return Arrays.asList(entitlement);
        }

        // First evaluate subject conditions.
        SubjectDecision subjectDecision = doesSubjectMatch(adminSubject, realm, subject, resourceName, environment);

        if (!subjectDecision.isSatisfied()) {
            Entitlement entitlement = new Entitlement(originalEntitlement.getApplicationName(),
                    originalEntitlement.getResourceName(), Collections.<String>emptySet());
            entitlement.setAdvices(subjectDecision.getAdvices());
            return Arrays.asList(entitlement);
        }

        // Second evaluate environment conditions.
        ConditionDecision conditionDecision = doesConditionMatch(realm, subject, resourceName, environment);

        if (!conditionDecision.isSatisfied()) {
            Entitlement entitlement = new Entitlement(originalEntitlement.getApplicationName(),
                    originalEntitlement.getResourceName(), Collections.<String>emptySet());
            entitlement.setAdvices(conditionDecision.getAdvice());
            entitlement.setTTL(conditionDecision.getTimeToLive());
            return Arrays.asList(entitlement);
        }

        // Finally verify the resource.
        Set<String> matchedResources = originalEntitlement.evaluate(
                adminSubject, realm, subject, applicationName, resourceName, actionNames, environment, recursive);

        if (PolicyConstants.DEBUG.messageEnabled()) {
            PolicyConstants.DEBUG.message("[PolicyEval] OpenSSOPrivilege.evaluate: resources=" + matchedResources);
        }

        // Retrieve the collection of response attributes base on the resource.
        Map<String, Set<String>> attributes = getAttributes(adminSubject, realm, subject, resourceName, environment);
        squashMaps(attributes, conditionDecision.getResponseAttributes());

        List<Entitlement> results = new ArrayList<>();

        for (String matchedResource : matchedResources) {
            Entitlement entitlement = new Entitlement(originalEntitlement.getApplicationName(),
                    matchedResource, originalEntitlement.getActionValues());
            entitlement.setAdvices(conditionDecision.getAdvice());
            entitlement.setAttributes(attributes);
            entitlement.setTTL(conditionDecision.getTimeToLive());
            results.add(entitlement);
        }

        return results;
    }

    /**
     * Squashes the second map into the first, merging sets with common keys.
     *
     * @param firstMap
     *         first map
     * @param secondMap
     *         second map
     */
    private void squashMaps(Map<String, Set<String>> firstMap, Map<String, Set<String>> secondMap) {
        if (firstMap.isEmpty()) {
            firstMap.putAll(secondMap);
            return;
        }

        for (Map.Entry<String, Set<String>> entry : secondMap.entrySet()) {
            if (firstMap.containsKey(entry.getKey())) {
                firstMap.get(entry.getKey()).addAll(entry.getValue());
            } else {
                firstMap.put(entry.getKey(), entry.getValue());
            }
        }
    }

    /**
     * Returns JSONObject mapping of  the object
     *
     * @return JSONObject mapping of  the object
     *
     * @throws JSONException
     *         if can not map to JSONObject
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
     * @param policyName
     *         Policy name.
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

}
