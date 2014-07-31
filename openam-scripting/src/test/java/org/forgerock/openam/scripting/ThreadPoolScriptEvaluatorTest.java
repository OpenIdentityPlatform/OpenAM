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

package org.forgerock.openam.scripting;

import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import javax.script.Bindings;
import javax.script.SimpleBindings;
import static org.fest.assertions.Assertions.assertThat;
import static org.forgerock.openam.scripting.StandardScriptEvaluatorTest.getGroovyScript;
import static org.forgerock.openam.scripting.StandardScriptEvaluatorTest.getJavascript;
import org.forgerock.util.thread.ExecutorServiceFactory;
import org.forgerock.util.thread.listener.ShutdownListener;
import org.forgerock.util.thread.listener.ShutdownManager;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;


public class ThreadPoolScriptEvaluatorTest {
    private Future mockFuture;
    private StandardScriptEngineManager scriptEngineManager;
    private ScriptEvaluator mockEvaluator;

    private ThreadPoolScriptEvaluator testEvaluator;

    @BeforeMethod
    public void setupTests() {
        mockFuture = mock(Future.class);
        mockEvaluator = mock(ScriptEvaluator.class);
        scriptEngineManager = new StandardScriptEngineManager();

        testEvaluator = new ThreadPoolScriptEvaluator(scriptEngineManager, new FakeFutureExecutor(), mockEvaluator);
    }


    @Test
    public void shouldStopJavaScriptExecutionWhenTimeoutReached() throws Exception {
        //given
        ScriptObject loopScript = getJavascript("while(true) { }");
        setTimeout(1);

        //when
        testEvaluator.<Void>evaluateScript(loopScript, null);

        //then
        verify(mockFuture).get(1, TimeUnit.SECONDS);
    }

    @Test
    public void shouldNotStopJavaScriptExecutionWhenNoTimeoutConfigured() throws Exception {
        //given
        ScriptObject loopScript = getJavascript("while(true) { }");
        setTimeout(0);

        //when
        testEvaluator.<Void>evaluateScript(loopScript, null);

        //then
        verify(mockFuture).get(); // No timeout
    }

    @Test
    public void shouldNotStopGroovyScriptExecutionWhenTimeoutReached() throws Exception {
        //given
        ScriptObject loopScript = getGroovyScript("while(true) { }");
        setTimeout(0);

        //when
        testEvaluator.<Void>evaluateScript(loopScript, null);

        //then
        verify(mockFuture).get();
    }

    @Test
    public void shouldStopGroovyScriptExecutionWhenTimeoutReached() throws Exception {
        //given
        ScriptObject loopScript = getGroovyScript("while(true) { }");
        setTimeout(1);

        //when
        testEvaluator.<Void>evaluateScript(loopScript, null);

        //then
        verify(mockFuture).get(1, TimeUnit.SECONDS);
    }

    @Test
    public void shouldReconfigureThreadPool() throws Exception {
        // Given
        // Configure thread pool with core = max = 1 threads
        final ThreadPoolExecutor threadPool = new ThreadPoolExecutor(1, 1, 1, TimeUnit.MINUTES,
                new LinkedBlockingQueue<Runnable>());
        testEvaluator = new ThreadPoolScriptEvaluator(scriptEngineManager, threadPool, mockEvaluator);
        final int newCoreSize = 50;
        final int newMaxSize = 100;
        final ScriptEngineConfiguration newConfiguration = ScriptEngineConfiguration.builder()
                .withThreadPoolCoreSize(newCoreSize)
                .withThreadPoolMaxSize(newMaxSize)
                .build();

        // When
        scriptEngineManager.setConfiguration(newConfiguration);

        // Then
        assertThat(threadPool.getCorePoolSize()).isEqualTo(newCoreSize);
        assertThat(threadPool.getMaximumPoolSize()).isEqualTo(newMaxSize);
    }

    @Test
    public void shouldDelegateToConfiguredScriptEvaluator() throws Exception {

        testEvaluator = new ThreadPoolScriptEvaluator(scriptEngineManager,
                Executors.newSingleThreadExecutor(), mockEvaluator);
        ScriptObject testScript = getGroovyScript("x + 1");
        Bindings bindings = new SimpleBindings();
        bindings.put("x", 3);
        int expectedResult = 42;
        given(mockEvaluator.evaluateScript(testScript, bindings)).willReturn(expectedResult);

        // When
        Number result = testEvaluator.evaluateScript(testScript, bindings);

        // Then
        verify(mockEvaluator).evaluateScript(testScript, bindings);
        assertThat(result.intValue()).isEqualTo(expectedResult);
    }

    private void setTimeout(int timeout) {
        scriptEngineManager.setConfiguration(ScriptEngineConfiguration.builder()
                .withTimeout(timeout, TimeUnit.SECONDS).build());
    }

    // Executor that runs everything in the calling thread without a pool and returns a fake future when submit called
    private class FakeFutureExecutor extends AbstractExecutorService {

        public <T> java.util.concurrent.Future<T> submit(java.util.concurrent.Callable<T> tCallable) {
            return mockFuture;
        }

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
            return;
        }

    }

    //extended to allow us to mock this out, using a shutdownmanager
    private class ExtendedExecutorServiceFactory extends ExecutorServiceFactory {

        private ShutdownManager shutdownmanager;

        public ExtendedExecutorServiceFactory(ShutdownManager shutdownManager) {
            super(shutdownManager);
            this.shutdownmanager = shutdownManager;
        }

        protected ShutdownManager getShutdownManager() {
            return shutdownmanager;
        }

        private void registerShutdown(final ExecutorService service) {
            getShutdownManager().addShutdownListener(new ShutdownListener() {
                public void shutdown() {
                    service.shutdownNow();
                }
            });
        }
    }
}
