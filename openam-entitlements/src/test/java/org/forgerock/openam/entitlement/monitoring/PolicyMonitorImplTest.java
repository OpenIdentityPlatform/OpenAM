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
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import javax.security.auth.Subject;
import org.forgerock.openam.shared.monitoring.RateTimer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.testng.Assert.assertEquals;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class PolicyMonitorImplTest {

    private PolicyMonitorImpl testPolicyMonitor;
    private Debug mockDebug = mock(Debug.class);
    private ExecutorService service = new CallerRunsExecutor();
    private EvaluationMonitoringStore selfEvaluationMonitoringStore;
    private EvaluationMonitoringStore subtreeEvaluationMonitoringStore;
    private EvaluationTimingStore selfEvaluationTimingStore;
    private EvaluationTimingStore subtreeEvaluationTimingStore;
    private InternalEvaluationTimingStore internalEvaluationTimingStore;

    private EntitlementConfigurationWrapper mockWrapper = mock(EntitlementConfigurationWrapper.class);
    private Subject mockSubject = new Subject();

    @BeforeMethod
    public void setUp() {
        selfEvaluationMonitoringStore = new EvaluationMonitoringStore(new RateTimer());
        subtreeEvaluationMonitoringStore = new EvaluationMonitoringStore(new RateTimer());
        selfEvaluationTimingStore = new EvaluationTimingStore(mockWrapper);
        subtreeEvaluationTimingStore = new EvaluationTimingStore(mockWrapper);
        internalEvaluationTimingStore = new InternalEvaluationTimingStore(mockWrapper);

        testPolicyMonitor = new PolicyMonitorImpl(mockDebug, service, selfEvaluationMonitoringStore,
                selfEvaluationTimingStore, subtreeEvaluationMonitoringStore, subtreeEvaluationTimingStore,
                internalEvaluationTimingStore);
    }

    @Test
    public void testExecutorServiceIsCalledWithCorrectModeSubtree() {
        //given
        EvaluationMonitoringStore mockSubtreeEvaluationMonitoringStore = mock(EvaluationMonitoringStore.class);
        EvaluationTimingStore mockSubtreeEvaluationTimingStore = mock(EvaluationTimingStore.class);

        testPolicyMonitor = new PolicyMonitorImpl(mockDebug, service, selfEvaluationMonitoringStore,
                selfEvaluationTimingStore, mockSubtreeEvaluationMonitoringStore, mockSubtreeEvaluationTimingStore,
                internalEvaluationTimingStore);

        //when
        testPolicyMonitor.addEvaluation(1l, null, null, null, mockSubject, PolicyMonitoringType.SUBTREE);

        //then
        verify(mockSubtreeEvaluationMonitoringStore, times(1)).increment();
        verify(mockSubtreeEvaluationTimingStore, times(1)).addTiming(1l, null, null, null, mockSubject);
    }

    @Test
    public void testExecutorServiceIsCalledWithCorrectModeSelf() {
        //given
        EvaluationMonitoringStore mockSelfEvaluationMonitoringStore = mock(EvaluationMonitoringStore.class);
        EvaluationTimingStore mockSelfEvaluationTimingStore = mock(EvaluationTimingStore.class);

        testPolicyMonitor = new PolicyMonitorImpl(mockDebug, service, mockSelfEvaluationMonitoringStore,
                mockSelfEvaluationTimingStore, subtreeEvaluationMonitoringStore, subtreeEvaluationTimingStore,
                internalEvaluationTimingStore);

        //when
        testPolicyMonitor.addEvaluation(1l, null, null, null, mockSubject, PolicyMonitoringType.SELF);

        //then
        verify(mockSelfEvaluationMonitoringStore, times(1)).increment();
        verify(mockSelfEvaluationTimingStore, times(1)).addTiming(1l, null, null, null, mockSubject);
    }

    @Test
    public void testExecutorServiceIsCalledForInternalTiming() {
        //given
        InternalEvaluationTimingStore mockInternalEvaluationTimingStore = mock(InternalEvaluationTimingStore.class);

        testPolicyMonitor = new PolicyMonitorImpl(mockDebug, service, selfEvaluationMonitoringStore,
                selfEvaluationTimingStore, subtreeEvaluationMonitoringStore, subtreeEvaluationTimingStore,
                mockInternalEvaluationTimingStore);

        //when
        testPolicyMonitor.addEvaluation(null, 1l, null, null, null, mockSubject);

        //then
        verify(mockInternalEvaluationTimingStore, times(1)).addTiming(null, 1l, null, null, null, mockSubject);
    }

    @Test
    public void testPolicyMonitoringTypeAffectsResult() {
        //given

        //when
        testPolicyMonitor.addEvaluation(500l, null, null, null, mockSubject, PolicyMonitoringType.SELF);
        testPolicyMonitor.addEvaluation(250l, null, null, null, mockSubject, PolicyMonitoringType.SUBTREE);

        //then
        assertEquals(testPolicyMonitor.getSlowestEvaluationDuration(PolicyMonitoringType.SELF), 500l);
        assertEquals(testPolicyMonitor.getSlowestEvaluationDuration(PolicyMonitoringType.SUBTREE), 250l);
    }

    @Test
    public void testSlowestEvaluationIsIdentifiedAndKept() {
        //given
        testPolicyMonitor.addEvaluation(1l, null, null, null, mockSubject, PolicyMonitoringType.SELF);
        testPolicyMonitor.addEvaluation(2l, null, null, null, mockSubject, PolicyMonitoringType.SELF);
        testPolicyMonitor.addEvaluation(3l, null, null, null, mockSubject, PolicyMonitoringType.SELF);
        testPolicyMonitor.addEvaluation(100l, null, null, null, mockSubject, PolicyMonitoringType.SELF);
        testPolicyMonitor.addEvaluation(5l, null, null, null, mockSubject, PolicyMonitoringType.SELF);
        testPolicyMonitor.addEvaluation(6l, null, null, null, mockSubject, PolicyMonitoringType.SELF);
        testPolicyMonitor.addEvaluation(7l, null, null, null, mockSubject, PolicyMonitoringType.SELF);

        //when
        long slowest = testPolicyMonitor.getSlowestEvaluationDuration(PolicyMonitoringType.SELF);

        //then
        assertEquals(slowest, 100l);
    }

    // Executor that runs everything in the calling thread without a pool
    private class CallerRunsExecutor extends AbstractExecutorService {

        private volatile boolean shutdown;

        public void shutdown() {
            shutdown = true;
        }

        public List<Runnable> shutdownNow() {
            return null;
        }

        public boolean isShutdown() {
            return shutdown;
        }

        public boolean isTerminated() {
            return shutdown;
        }

        public boolean awaitTermination(long time, TimeUnit unit) throws InterruptedException {
            return true;
        }

        public void execute(Runnable runnable) {
            runnable.run();
        }

    }
}