/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2015 ForgeRock AS.
 */

package org.forgerock.openam.uma;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.forgerock.json.fluent.JsonPointer;

/**
 *
 *
 * @since 13.0.0
 */
public final class PolicySearch {

    private final Collection<UmaPolicy> policies;

    /**
     *
     *
     * @param policies
     */
    public PolicySearch(Collection<UmaPolicy> policies) {
        this.policies = policies;
    }

    /**
     *
     *
     */
    public PolicySearch() {
        this.policies = new HashSet<UmaPolicy>();
    }

    /**
     *
     *
     * @param field
     * @param value
     * @return
     */
    public PolicySearch equals(JsonPointer field, Object value) {
        PolicySearch policySearch = new PolicySearch();

        if (new JsonPointer("/permissions/subject").equals(field)) {
            for (UmaPolicy policy : policies) {
                if (policy.getSubjects().contains(value)) {
                    policySearch.add(policy);
                }
            }
        } else if (new JsonPointer("/resourceServer").equals(field)) {
            for (UmaPolicy policy : policies) {
                if (policy.getResourceServerId().equals(value)) {
                    policySearch.add(policy);
                }
            }
        } else {
            throw new UnsupportedOperationException("Unsupported field, " + field.toString());
        }

        return policySearch;
    }

    private void add(UmaPolicy policy) {
        this.policies.add(policy);
    }

    /**
     *
     *
     * @return
     */
    public Collection<UmaPolicy> getPolicies() {
        return policies;
    }

    /**
     *
     *
     * @param search
     * @return
     */
    public PolicySearch combine(PolicySearch search) {
        HashSet<UmaPolicy> combinedPolicies = new HashSet<UmaPolicy>(this.policies);
        combinedPolicies.addAll(search.policies);
        return new PolicySearch(combinedPolicies);
    }

    /**
     *
     *
     * @param search
     * @return
     */
    public PolicySearch remove(PolicySearch search) {
        Set<UmaPolicy> subPolicies = new HashSet<UmaPolicy>(this.policies);
        subPolicies.removeAll(search.policies);
        return new PolicySearch(subPolicies);
    }
}
