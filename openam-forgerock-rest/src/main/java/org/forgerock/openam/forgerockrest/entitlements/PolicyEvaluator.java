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

import javax.security.auth.Subject;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Attempts to evaluate and provide authorisation decisions based on policy requests.
 *
 * @since 12.0.0
 */
public interface PolicyEvaluator {

    /**
     * Given the various parts that make up a policy request, provides a set of policy decisions.
     *
     * @param realm
     *         the realm of which the policy resides
     * @param subject
     *         the subject requesting access to the set of resources
     * @param resourceNames
     *         the set of resources under protection
     * @param environment
     *         environment attributes
     *
     * @return list of corresponding policy decisions
     *
     * @throws EntitlementException
     *         should an error occur during the evaluation process
     */
    public List<Entitlement> evaluate(final String realm, final Subject subject, final Set<String> resourceNames,
                                      final Map<String, Set<String>> environment) throws EntitlementException;

}
