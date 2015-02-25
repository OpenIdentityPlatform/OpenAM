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
package org.forgerock.openam.entitlement.monitoring;

import com.sun.identity.shared.debug.Debug;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.security.auth.Subject;

/**
 * Singleton through which all access to the policy monitoring stores is achieved.
 *
 * This singleton has its own ExecutorService which it uses to ensure that updates (which may block)
 * to the policy's statistics are done in a separate thread.
 *
 * It has one set of stores for each of the two modes that policy can be queried by - Self and Subtree,
 * it also contains a store for the internal (individual privilege) evaluation timings.
 */
@Singleton
public class PolicyMonitorImpl implements PolicyMonitor {

    //debug
    private final Debug debug;

    //self monitoring stores
    private final EvaluationMonitoringStore selfEvaluationMonitoringStore;
    private final EvaluationTimingStore selfEvaluationTimingStore;

    //subtree monitoring stores
    private final EvaluationMonitoringStore subtreeEvaluationMonitoringStore;
    private final EvaluationTimingStore subtreeEvaluationTimingStore;

    //internal monitoring store
    private final InternalEvaluationTimingStore internalEvaluationTimingStore;

    //for pushing off our monitoring writes to another thread
    private final ExecutorService executorService;
    public static final String EXECUTOR_BINDING_NAME = "POLICY_MONITORING_EXECUTOR";

    /**
     * Guice-powered constructor.
     *
     * @param debug Debug instance to use
     * @param executorService The executor service this module should use when passing off the adding of new data
     * @param selfEvaluationMonitoringStore Store for the SELF mode evaluation monitoring
     * @param selfEvaluationTimingStore Store for the SELF mode timing monitoring
     * @param subtreeEvaluationMonitoringStore Store for the SUBTREE mode evaluation monitoring
     * @param subtreeEvaluationTimingStore Store for the SELF mode timing monitoring
     * @param internalEvaluationTimingStore Store for the internal privilege evaluation monitoring
     */
    @Inject
    public PolicyMonitorImpl(@Named(POLICY_MONITOR_DEBUG) Debug debug,
                             @Named(EXECUTOR_BINDING_NAME) ExecutorService executorService,
                             final EvaluationMonitoringStore selfEvaluationMonitoringStore,
                             final EvaluationTimingStore selfEvaluationTimingStore,
                             final EvaluationMonitoringStore subtreeEvaluationMonitoringStore,
                             final EvaluationTimingStore subtreeEvaluationTimingStore,
                             final InternalEvaluationTimingStore internalEvaluationTimingStore) {
        this.debug = debug;
        this.executorService = executorService;
        this.selfEvaluationMonitoringStore = selfEvaluationMonitoringStore;
        this.selfEvaluationTimingStore = selfEvaluationTimingStore;
        this.subtreeEvaluationMonitoringStore = subtreeEvaluationMonitoringStore;
        this.subtreeEvaluationTimingStore = subtreeEvaluationTimingStore;
        this.internalEvaluationTimingStore = internalEvaluationTimingStore;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addEvaluation(final long duration, final String realm, final String applicationName,
                              final String resourceName, final Subject subject, final PolicyMonitoringType monitoringType) {
        if (monitoringType == PolicyMonitoringType.SUBTREE) {

            try {
                executorService.submit(new Runnable() {
                    public void run() {
                        subtreeEvaluationMonitoringStore.increment();
                        subtreeEvaluationTimingStore.addTiming(duration, realm, applicationName, resourceName, subject);
                    }
                });
            } catch (RejectedExecutionException ree) {
                debug.error("Unable to store evaluation time - task rejected from pool.", ree);
            }

        } else {

            try {
                executorService.submit(new Runnable() {
                    public void run() {
                        selfEvaluationMonitoringStore.increment();
                        selfEvaluationTimingStore.addTiming(duration, realm, applicationName, resourceName, subject);
                    }
                });
            } catch (RejectedExecutionException ree) {
                debug.error("Unable to store evaluation time - task rejected from pool", ree);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addEvaluation(final String policyName, final long duration, final String realm,
                       final String applicationName, final String resourceName, final Subject subject) {

        try {
            executorService.submit(new Callable<Void>() {
                public Void call() throws Exception {
                    internalEvaluationTimingStore.addTiming(policyName, duration, realm, applicationName, resourceName, subject);
                    return null;
                }
            });
        } catch (RejectedExecutionException ree) {
            debug.error("Unable to store evaluation time - task rejected from pool", ree);
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getEvaluationCumulativeCount(PolicyMonitoringType monitoringType) {
        if (monitoringType == PolicyMonitoringType.SUBTREE) {
            return subtreeEvaluationMonitoringStore.getEvaluationCumulativeCount();
        } else {
            return selfEvaluationMonitoringStore.getEvaluationCumulativeCount();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getEvaluationMaxRate(PolicyMonitoringType monitoringType) {
        if (monitoringType == PolicyMonitoringType.SUBTREE) {
            return subtreeEvaluationMonitoringStore.getMaximumEvaluationsPerPeriod();
        } else {
            return selfEvaluationMonitoringStore.getMaximumEvaluationsPerPeriod();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getEvaluationMinRate(PolicyMonitoringType monitoringType) {
        if (monitoringType == PolicyMonitoringType.SUBTREE) {
            return subtreeEvaluationMonitoringStore.getMinimumEvaluationsPerPeriod();
        } else {
            return selfEvaluationMonitoringStore.getMinimumEvaluationsPerPeriod();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getEvaluationAvgRate(PolicyMonitoringType monitoringType) {
        if (monitoringType == PolicyMonitoringType.SUBTREE) {
            return Math.round(subtreeEvaluationMonitoringStore.getAverageEvaluationsPerPeriod());
        } else {
            return Math.round(selfEvaluationMonitoringStore.getAverageEvaluationsPerPeriod());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getSlowestEvaluation(PolicyMonitoringType monitoringType) {
        if (monitoringType == PolicyMonitoringType.SUBTREE) {
            return subtreeEvaluationTimingStore.getSlowestEvaluation();
        } else {
            return selfEvaluationTimingStore.getSlowestEvaluation();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getAverageEvaluationTime(PolicyMonitoringType monitoringType) {
        if (monitoringType == PolicyMonitoringType.SUBTREE) {
            return subtreeEvaluationTimingStore.getDurationAverage();
        } else {
            return selfEvaluationTimingStore.getDurationAverage();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getSlowestInternalEvaluation() {
        return internalEvaluationTimingStore.getSlowestEvaluation();
    }

    /**
     * Exposed for ease-of-testing.
     *
     * @param monitoringType Mode of operation to return duration for
     * @return duration of the longest operation, in ms
     */
    protected long getSlowestEvaluationDuration(PolicyMonitoringType monitoringType) {
        if (monitoringType == PolicyMonitoringType.SUBTREE) {
            return subtreeEvaluationTimingStore.getSlowestEvaluationDuration();
        } else {
            return selfEvaluationTimingStore.getSlowestEvaluationDuration();
        }
    }

}
