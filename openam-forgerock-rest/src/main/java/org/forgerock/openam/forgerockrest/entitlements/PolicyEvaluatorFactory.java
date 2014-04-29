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

import com.sun.identity.entitlement.EntitlementException;

import javax.security.auth.Subject;

/**
 * Factory provides policy evaluators used to process policy requests.
 *
 * @since 12.0.0
 */
public interface PolicyEvaluatorFactory {

    /**
     * Given the subject looking to request policy decisions within an
     * application context, retrieve the relevant policy evaluator.
     *
     * @param subject
     *         the subject looking to request policy decisions
     * @param application
     *         the application context
     *
     * @return a policy evaluator
     *
     * @throws EntitlementException
     *         should an error occur retrieve a policy evaluator
     */
    public PolicyEvaluator getEvaluator(final Subject subject, final String application) throws EntitlementException;

}
