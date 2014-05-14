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
 * Copyright 2013 ForgeRock AS.
 */

package org.forgerock.openam.entitlement.monitoring;

import javax.security.auth.Subject;

/**
 * An interface to a data structure for storing monitoring information about policy evaluations.
 */
public interface PolicyMonitor {

    public static final String POLICY_MONITOR_DEBUG = "amEntitlements";

    /**
     * Adds an evaluation which should be recorded by the PolicyMonitor. This relates to
     * all policies being applied to a request.
     *
     * @param duration Total length of time in ms the evaluation took to complete
     * @param realm Realm in which the evaluation took place
     * @param applicationName Application name against which the evaluation was made
     * @param resourceName Resource against which the evaluation was made
     * @param subject The subject making the evaluation
     * @param monitoringType Whether the evaluation was of SUBTREE or SELF mode
     */
    void addEvaluation(long duration, String realm, String applicationName, String resourceName, Subject subject,
                       PolicyMonitoringType monitoringType);

    /**
     * Adds an individual, internal evaluation which should be recorded by the PolicyMonitor. This
     * relates to the specific policy being evaluated, and not to all the policies being
     * applied to a request.
     *
     * @param duration Total length of time in ms the evaluation took to complete
     * @param realm Realm in which the evaluation took place
     * @param applicationName Application name against which the evaluation was made
     * @param resourceName Resource against which the evaluation was made
     * @param subject The subject making the evaluation
     */
    void addEvaluation(String policyName, long duration, String realm,
                       String applicationName, String resourceName, Subject subject);

    /**
     * Returns information on the slowest performing evaluation.
     *
     * @param monitoringType SUBTREE or SELF
     * @return information on the slowest performing evaluation
     */
    String getSlowestEvaluation(PolicyMonitoringType monitoringType);

    /**
     * Average length of time an evaluation takes to complete.
     *
     * @param monitoringType SUBTREE or SELF
     * @return The average length of time it takes to complete an evaluation in ms
     */
    long getAverageEvaluationTime(PolicyMonitoringType monitoringType);

    /**
     * Total number of evaluations which have taken place.
     *
     * @param monitoringType SUBTREE or SELF
     * @return The total number of evaluations which have occured
     */
    long getEvaluationCumulativeCount(PolicyMonitoringType monitoringType);

    /**
     * The maximum rate at which evaluations have taken place.
     *
     * @param monitoringType SUBTREE or SELF
     * @return The maximum rate at which evaluations have been performed
     */
    long getEvaluationMaxRate(PolicyMonitoringType monitoringType);

    /**
     * The minimum rate at which evaluations have taken place.
     *
     * @param monitoringType SUBTREE or SELF
     * @return The minimum rate at which evaluations have been performed
     */
    long getEvaluationMinRate(PolicyMonitoringType monitoringType);

    /**
     * The average rate at which evaluations have taken place.
     *
     * @param monitoringType SUBTREE or SELF
     * @return The average rate at which evaluations have been performed
     */
    long getEvaluationAvgRate(PolicyMonitoringType monitoringType);

    /**
     * Returns the slowest individual policy evaluation.
     *
     * @return information on the slowest performing policy evaluation
     */
    String getSlowestInternalEvaluation();

}