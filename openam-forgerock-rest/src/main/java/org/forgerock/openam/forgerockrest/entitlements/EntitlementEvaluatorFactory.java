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
import com.sun.identity.entitlement.Evaluator;

import javax.security.auth.Subject;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Factory delivers up evaluators that call into the entitlements evaluator.
 *
 * @since 12.0.0
 */
public class EntitlementEvaluatorFactory implements PolicyEvaluatorFactory {

    @Override
    public PolicyEvaluator getEvaluator(final Subject subject, final String application) throws EntitlementException {
        return new EntitlementEvaluatorWrapper(new Evaluator(subject, application));
    }

    /**
     * Wraps the passed entitlement evaluator, delegating calls appropriately.
     * <p />
     * The purpose of this wrapper is to decouple hard dependencies to
     * the entitlement evaluator as it has various resource constraints.
     */
    private static final class EntitlementEvaluatorWrapper implements PolicyEvaluator {

        private final Evaluator evaluator;

        public EntitlementEvaluatorWrapper(final Evaluator evaluator) {
            this.evaluator = evaluator;
        }

        @Override
        public List<Entitlement> evaluate(final String realm, final Subject subject, final Set<String> resourceNames,
                                          final Map<String, Set<String>> environment) throws EntitlementException {
            return evaluator.evaluate(realm, subject, resourceNames, environment);
        }

    }

}
