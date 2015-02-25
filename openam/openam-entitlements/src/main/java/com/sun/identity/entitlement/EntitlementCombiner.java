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
 * $Id: EntitlementCombiner.java,v 1.4 2009/12/07 19:46:45 veiming Exp $
 *
 * Portions copyright 2010-2014 ForgeRock AS.
 */
package com.sun.identity.entitlement;

import com.sun.identity.entitlement.interfaces.ResourceName;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Encapsulates a Strategy for combining the results of two {@link com.sun.identity.entitlement.Entitlement}s.
 * Across the system, these can also be referred to as "decision combiners"; for example, the set of registered
 * EntitlementCombiners can be retrieved from the <code>/json/decisioncombiners</code> REST endpoint.
 *
 * This is the base class and is, for example, extended by {@link com.sun.identity.entitlement.DenyOverride}.
 *
 * <code>init</code> needs to be called after it is created.
 */
public abstract class EntitlementCombiner {

    private Set<String> actions;
    private boolean isDone;
    private boolean isRecursive;
    private List<Entitlement> results = new ArrayList<Entitlement>();
    private ResourceName resourceComparator;

    /**
     * root entitlement is the root entitlement when we are doing sub tree
     * evaluation (recursive = true); and is the entitlement decision for
     * single node evaluation (recursive = false).
     */
    private Entitlement rootE;

    /**
     * Initializes the combiner.
     *
     * @param realm Realm name.
     * @param applicationName Application Name.
     * @param normalisedResourceName The normalised resource name.
     * @param requestedResourceName The requested resource name.
     * @param actions Action names to be evaluated.
     * @param isRecursive <code>true<</code> for subtree evaluation.
     */
    public void init(String realm, String applicationName, String normalisedResourceName, String requestedResourceName,
                     Set<String> actions, boolean isRecursive) throws EntitlementException {
        Application application = ApplicationManager.getApplication(
                PrivilegeManager.superAdminSubject, realm, applicationName);
        init(normalisedResourceName, requestedResourceName, actions, isRecursive, application);
    }

    /**
     * Initializes the combiner.
     *
     * @param normalisedResourceName The normalised resource name.
     * @param requestedResourceName The requested resource name.
     * @param actions Action names to be evaluated.
     * @param isRecursive <code>true<</code> for subtree evaluation.
     * @param application The defining application.
     */
    public void init(String normalisedResourceName, String requestedResourceName, Set<String> actions,
                     boolean isRecursive, Application application) throws EntitlementException {
        this.isRecursive = isRecursive;
        this.actions = new HashSet<String>();

        rootE = new Entitlement(application.getName(), normalisedResourceName, Collections.EMPTY_MAP);
        rootE.setRequestedResourceName(requestedResourceName);
        resourceComparator = application.getResourceComparator();

        if (!isRecursive) { // single level
            if (actions != null && !actions.isEmpty()) {
                this.actions.addAll(actions);
            } else {
                this.actions.addAll(application.getActions().keySet());
            }
        } else {
            this.actions.addAll(application.getActions().keySet());
        }

        results.add(rootE);
    }

    /**
     * Adds a set of entitlements to the overall entitlement decision. These
     * entitlements will be combined with existing decision.
     *
     * @param entitlements Set of entitlements.
     */
    public void add(List<Entitlement> entitlements) {
        if (!isRecursive) {
            for (Entitlement e : entitlements) {
                mergeActionValues(rootE, e);
                mergeAdvices(rootE, e);
                mergeAttributes(rootE, e);
                mergeTimeToLiveValue(rootE, e);
            }
        } else {
            boolean isRegExComparator = (resourceComparator instanceof 
                RegExResourceName);
            for (Entitlement e : entitlements) {
                boolean toAdd = true;
                for (Entitlement existing : results) {
                    ResourceMatch match = resourceComparator.compare(
                        e.getResourceName(), existing.getResourceName(), true);
                    if (match.equals(ResourceMatch.EXACT_MATCH)) {
                        mergeActionValues(existing, e);
                        mergeAdvices(existing, e);
                        mergeAttributes(existing, e);
                        mergeTimeToLiveValue(existing, e);
                        toAdd = false;
                    } else if (match.equals(ResourceMatch.SUB_RESOURCE_MATCH)) {
                        mergeActionValues(existing, e);
                        mergeAdvices(existing, e);
                        mergeAttributes(existing, e);
                        mergeTimeToLiveValue(existing, e);
                    } else if (match.equals(ResourceMatch.SUPER_RESOURCE_MATCH)) {
                        mergeActionValues(e, existing);
                        mergeAdvices(e, existing);
                        mergeAttributes(e, existing);
                        mergeTimeToLiveValue(existing, e);
                    } else if (!isRegExComparator &&
                        match.equals(ResourceMatch.WILDCARD_MATCH)) {
                        mergeActionValues(e, existing);
                        mergeAdvices(e, existing);
                        mergeAttributes(e, existing);
                        mergeTimeToLiveValue(existing, e);
                    }
                }

                if (toAdd) {
                    Entitlement tmp = new Entitlement(e.getApplicationName(),
                        e.getResourceName(), e.getActionValues());
                    tmp.setAttributes(e.getAttributes());
                    tmp.setAdvices(e.getAdvices());
                    tmp.setTTL(e.getTTL());
                    results.add(tmp);
                }
            }
        }
    }

    /**
     * Sets the action values of the first entitlement to be the union of all action values from the first and second
     * entitlements; if a particular action value is contained in both entitlements, then the two values are combined
     * (using the implementation-dependent) {@link #combine} method) before being added to the first entitlement.
     *
     * @param e1 Entitlement.
     * @param e2 Entitlement.
     */
    protected void mergeActionValues(Entitlement e1, Entitlement e2) {
        if (!e1.hasAdvice() && !e2.hasAdvice()) {
            Map<String, Boolean> result = new HashMap<String, Boolean>();
            Map<String, Boolean> a1 = e1.getActionValues();
            if (a1 == null) {
                a1 = Collections.EMPTY_MAP;
            }
            Map<String, Boolean> a2 = e2.getActionValues();
            if (a2 == null) {
                a2 = Collections.EMPTY_MAP;
            }

            Set<String> actionNames = new HashSet<String>();
            actionNames.addAll(a1.keySet());
            actionNames.addAll(a2.keySet());

            for (String n : actionNames) {
                Boolean b1 = a1.get(n);
                Boolean b2 = a2.get(n);

                if (b1 == null) {
                    result.put(n, b2);
                } else if (b2 == null) {
                    result.put(n, b1);
                } else {
                    Boolean b = Boolean.valueOf(combine(b1, b2));
                    result.put(n, b);
                }
            }
            e1.setActionValues(result);
        } else {
            // Advice is present and therefore more data is needed before any actions can be taken.
            e1.setActionNames(Collections.EMPTY_SET);
        }

        isDone = isCompleted();
    }

    /**
     * Sets the advices of the first entitlement to be the union of all advices from the first and second entitlements.
     *
     * @param e1 Entitlement.
     * @param e2 Entitlement.
     */
    protected void mergeAdvices(Entitlement e1, Entitlement e2) {
        Map<String, Set<String>> result = new HashMap<String, Set<String>>();
        Map<String, Set<String>> a1 = e1.getAdvices();
        if (a1 == null) {
            a1 = Collections.EMPTY_MAP;
        }
        Map<String, Set<String>> a2 = e2.getAdvices();
        if (a2 == null) {
            a2 = Collections.EMPTY_MAP;
        }

        Set<String> names = new HashSet<String>();
        names.addAll(a1.keySet());
        names.addAll(a2.keySet());

        for (String n : names) {
            Set<String> advice1 = a1.get(n);
            Set<String> advice2 = a2.get(n);

            Set<String> r = result.get(n);
            if (r == null) {
                r = new HashSet<String>();
                result.put(n, r);
            }

            if ((advice1 != null) && !advice1.isEmpty()) {
                r.addAll(advice1);
            }
            if ((advice2 != null) && !advice2.isEmpty()) {
                r.addAll(advice2);
            }
        }
        e1.setAdvices(result);
    }

    /**
     * Sets the attributes of the first entitlement to be the union of all attributes from the first and second
     * entitlements.
     *
     * @param e1 Entitlement
     * @param e2 Entitlement
     */
    protected void mergeAttributes(Entitlement e1, Entitlement e2) {
        Map<String, Set<String>> result = new HashMap<String, Set<String>>();
        Map<String, Set<String>> a1 = e1.getAttributes();
        if (a1 == null) {
            a1 = Collections.EMPTY_MAP;
        }
        Map<String, Set<String>> a2 = e2.getAttributes();
        if (a2 == null) {
            a2 = Collections.EMPTY_MAP;
        }

        Set<String> names = new HashSet<String>();
        names.addAll(a1.keySet());
        names.addAll(a2.keySet());
        
        for (String n : names) {
            Set<String> attr1 = a1.get(n);
            Set<String> attr2 = a2.get(n);

            Set<String> r = result.get(n);
            if (r == null) {
                r = new HashSet<String>();
                result.put(n, r);
            }

            if ((attr1 != null) && !attr1.isEmpty()) {
                r.addAll(attr1);
            }
            if ((attr2 != null) && !attr2.isEmpty()) {
                r.addAll(attr2);
            }
        }
        e1.setAttributes(result);
    }

    /**
     * Merges time to live values. The lowest of the TTL values is set as the TTL.
     *
     * @param e1 Entitlement
     * @param e2 Entitlement
     */
    protected void mergeTimeToLiveValue(Entitlement e1, Entitlement e2) {
        if (e1.getTTL() > e2.getTTL()) {
            e1.setTTL(e2.getTTL());
        }
    }

    /**
     * Returns action names.
     *
     * @return action names.
     */
    protected Set<String> getActions() {
        return actions;
    }

    /**
     * Returns <code>true</code> if this entitlement combiner is working on sub tree evaluation.
     *
     * @return <code>true</code> if this entitlement combiner is working on sub tree evaluation.
     */
    protected boolean isRecursive() {
        return isRecursive;
    }

    /**
     * Returns the entitlement which will act as the root for sub tree evaluations.
     *
     * @return root entitlement for sub tree evaluations.
     */
    protected Entitlement getRootE() {
        return rootE;
    }

    /**
     * Returns the resource comparator.
     *
     * @return resource comparator.
     */
    protected ResourceName getResourceComparator() {
        return resourceComparator;
    }

    /**
     * Returns <code>true</code> if policy decision can also be determined.
     *
     * @return <code>true</code> if policy decision can also be determined.
     */
    public boolean isDone() {
        return isDone;
    }

    /**
     * Returns entitlements which are the result of combining a set of entitlements.
     *
     * @return entitlement results.
     */
    public List<Entitlement> getResults() {
        return results;
    }

    /**
     * Returns the result of combining two entitlement decisions.
     *
     * @param b1 entitlement decision.
     * @param b2 entitlement decision.
     * @return result of combining two entitlement decisions.
     */
    protected abstract boolean combine(Boolean b1, Boolean b2);

    /**
     * Returns <code>true</code> if policy decision can also be determined.
     * This method is called by derived classes. #isDone method shall be set if this returns true.
     *
     * @return <code>true</code> if policy decision can also be determined.
     */
    protected abstract boolean isCompleted();

    /**
     * Returns the name of this class for ease of reference.
     *
     * @return The simple name of this instance's class
     */
    public String getName() {
        return getClass().getSimpleName();
    }
}
