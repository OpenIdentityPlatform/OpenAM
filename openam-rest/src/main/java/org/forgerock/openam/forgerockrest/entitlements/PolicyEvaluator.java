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
 * Copyright 2014 ForgeRock AS.
 */

package org.forgerock.openam.forgerockrest.entitlements;

import com.sun.identity.entitlement.Entitlement;
import com.sun.identity.entitlement.EntitlementException;
import org.forgerock.openam.forgerockrest.entitlements.model.json.BatchPolicyRequest;
import org.forgerock.openam.forgerockrest.entitlements.model.json.PolicyRequest;
import org.forgerock.openam.forgerockrest.entitlements.model.json.TreePolicyRequest;

import java.util.List;

/**
 * Attempts to evaluate and provide authorisation decisions based on policy requests.
 *
 * @since 12.0.0
 */
public interface PolicyEvaluator {

    /**
     * Given a batch policy request, provides a set of policy
     * decisions that correspond to each resource definition.
     *
     * @param request
     *         a non-null batch request
     *
     * @return list of corresponding policy decisions
     *
     * @throws EntitlementException
     *         should an error occur during the evaluation process
     */
    public List<Entitlement> evaluateBatch(final BatchPolicyRequest request) throws EntitlementException;

    /**
     * Given a tree policy request, provides a set of policy decisions for each
     * defined policy that matches the single resource definition and below.
     *
     * @param request
     *         a non-null tree request
     *
     * @return list of policy decisions
     *
     * @throws EntitlementException
     *         should an error occur during the evaluation process
     */
    public List<Entitlement> evaluateTree(final TreePolicyRequest request) throws EntitlementException;

    /**
     * Given a generic policy request, routes the request to the appropriate evaluation method.
     *
     * @param request
     *         a non-null policy request
     *
     * @return list of policy decisions appropriate for the request type
     *
     * @throws EntitlementException
     *         should an error occur during the evaluation process
     */
    public List<Entitlement> routePolicyRequest(final PolicyRequest request) throws EntitlementException;

}
