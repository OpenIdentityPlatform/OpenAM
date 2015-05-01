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
 * $Id: AndCondition.java,v 1.3 2010/01/12 21:29:58 veiming Exp $
 *
 * Portions Copyrighted 2015 ForgeRock AS.
 */
package com.sun.identity.entitlement;

import org.forgerock.openam.utils.CollectionUtils;

import javax.security.auth.Subject;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * <code>EntitlementCondition</code> wrapper on a set of
 * <code>EntitlementCondition</code>(s) to provide
 * boolean OR logic Membership is of <code>AndCondition</code> is satisfied
 * if the user is a member of any of the wrapped
 * <code>EntitlementCondition</code>.
 */
public class AndCondition extends LogicalCondition {
    /**
     * Constructs <code>AndCondition</code>
     */
    public AndCondition() {
        super();
    }

    /**
     * Constructs AndCondition
     *
     * @param eConditions wrapped <code>EntitlementCondition</code>(s)
     */
    public AndCondition(Set<EntitlementCondition> eConditions) {
        super(eConditions);
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
    public ConditionDecision evaluate(String realm, Subject subject, String resourceName,
                                      Map<String, Set<String>> environment) throws EntitlementException {

        Set<EntitlementCondition> conditions = getEConditions();

        if (CollectionUtils.isEmpty(conditions)) {
            return ConditionDecision
                    .newSuccessBuilder()
                    .build();
        }

        Map<String, Set<String>> advices = new HashMap<>();
        Map<String, Set<String>> responseAttributes = new HashMap<>();

        for (EntitlementCondition condition : conditions) {

            ConditionDecision decision = condition.evaluate(realm, subject, resourceName, environment);
            advices.putAll(decision.getAdvices());
            responseAttributes.putAll(decision.getResponseAttributes());

            if (!decision.isSatisfied()) {
                return ConditionDecision
                        .newFailureBuilder()
                        .setAdvices(advices)
                        .build();
            }
        }

        return ConditionDecision
                .newSuccessBuilder()
                .setResponseAttributes(responseAttributes)
                .build();
    }

}
