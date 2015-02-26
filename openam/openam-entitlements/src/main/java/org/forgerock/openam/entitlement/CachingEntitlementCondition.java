/*
 * Copyright 2013 ForgeRock, Inc.
 *
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
 */
package org.forgerock.openam.entitlement;

import com.sun.identity.entitlement.ConditionDecision;
import com.sun.identity.entitlement.EntitlementCondition;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.shared.debug.Debug;
import java.util.Map;
import java.util.Set;
import javax.security.auth.Subject;

/**
 * During policy evaluation multiple matching entitlement privileges can contain the same entitlement conditions
 * and by default OpenAM evaluates all the privileges separately, hence it could evaluate the same conditions multiple
 * times unnecessarily. In case a given condition check is time-consuming this could lead to performance problems.
 * The purpose of the {@link CachingEntitlementCondition} is to make sure that the exact same entitlement conditions
 * are only executed once per privilege evaluations. This is being implemented using the {@link
 * PrivilegeEvluatorContext} object, which is shared across privilege evaluator threads. It's worthwhile to note, that
 * the context is only shared per a single privilege evaluation, hence different privilege evaluations have different
 * caches.
 *
 * @author Peter Major
 */
public class CachingEntitlementCondition implements EntitlementCondition {

    private static final Debug DEBUG = Debug.getInstance("Entitlement");
    private EntitlementCondition backingCondition;

    public CachingEntitlementCondition(EntitlementCondition backingCondition) {
        this.backingCondition = backingCondition;
    }

    /**
     * {@inheritDoc}
     */
    public void setDisplayType(String displayType) {
        backingCondition.setDisplayType(displayType);
    }

    /**
     * {@inheritDoc}
     */
    public String getDisplayType() {
        return backingCondition.getDisplayType();
    }

    /**
     * {@inheritDoc}
     */
    public void init(Map<String, Set<String>> parameters) {
        backingCondition.init(parameters);
    }

    /**
     * {@inheritDoc}
     */
    public void setState(String state) {
        backingCondition.setState(state);
    }

    /**
     * {@inheritDoc}
     */
    public String getState() {
        return backingCondition.getState();
    }

    /**
     * First checks whether the backing condition has been evaluated already, if no, then evaluates the condition once
     * and saves the result in the {@link PrivilegeEvaluatorContext#conditionDecisionCache}. If the result of the
     * condition is already cached, then it returns the cached result.
     * In case the context is not available for any reason, then the condition will be executed
     *
     * @param realm {@inheritDoc}
     * @param subject {@inheritDoc}
     * @param resourceName {@inheritDoc}
     * @param environment {@inheritDoc}
     * @return {@inheritDoc}
     * @throws EntitlementException {@inheritDoc}
     */
    public ConditionDecision evaluate(String realm, Subject subject, String resourceName,
            Map<String, Set<String>> environment) throws EntitlementException {
        String classMethod = "CachingEntitlementCondition.evaluate() ";
        PrivilegeEvaluatorContext context = PrivilegeEvaluatorContext.getCurrent();
        if (context == null) {
            DEBUG.warning(classMethod + "PrivilegeEvaluatorContext is not available, condition cache is discarded.");
            return backingCondition.evaluate(realm, subject, resourceName, environment);
        }

        //context is shared across evaluator threads, so we can synchronize on it. Different privilege evaluations have
        //different contexts as well.
        synchronized (context) {
            ConditionDecision cachedResult = context.getConditionDecisionCache().get(getState());
            if (cachedResult != null) {
                if (DEBUG.messageEnabled()) {
                    DEBUG.message(classMethod + "returning cached condition decision");
                }
                return cachedResult;
            }
            ConditionDecision result = backingCondition.evaluate(realm, subject, resourceName, environment);
            if (DEBUG.messageEnabled()) {
                DEBUG.message(classMethod + "caching condition decision \"" + result.isSatisfied()
                        + "\" for condition: " + getState());
            }
            context.getConditionDecisionCache().put(getState(), result);
            return result;
        }
    }
}
