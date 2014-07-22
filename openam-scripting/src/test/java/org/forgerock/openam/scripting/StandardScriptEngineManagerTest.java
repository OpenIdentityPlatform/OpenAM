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


import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import static org.fest.assertions.Assertions.assertThat;

public class StandardScriptEngineManagerTest {

    private StandardScriptEngineManager testManager;

    @BeforeMethod
    public void createManager() {
        testManager = new StandardScriptEngineManager();
    }

    @Test
    public void shouldBePreconfiguredWithSupportedLanguages() {

        //given - these should be loaded automatically as we support them

        //when

        //then
        assertThat(testManager.getEngineByName(SupportedScriptingLanguage.JAVASCRIPT_ENGINE_NAME)).isNotNull();
        assertThat(testManager.getEngineByName(SupportedScriptingLanguage.GROOVY_ENGINE_NAME)).isNotNull();
    }

    @Test
    public void shouldBroadcastConfigChangesToAllListeners() {
        // Given
        ScriptEngineConfiguration newConfiguration = getTestConfiguration();
        List<TestConfigurationListener> listeners = registerListeners(100);

        // When
        testManager.setConfiguration(newConfiguration);

        // Then
        for (TestConfigurationListener listener : listeners) {
            assertThat(listener.configuration).isEqualTo(newConfiguration);
        }
    }

    @Test
    public void shouldUnregisterChangeListenersCorrectly() {
        // Given
        ScriptEngineConfiguration oldConfiguration = ScriptEngineConfiguration.builder().build();
        ScriptEngineConfiguration newConfiguration = getTestConfiguration();
        TestConfigurationListener a = new TestConfigurationListener();
        TestConfigurationListener b = new TestConfigurationListener();
        testManager.addConfigurationListener(a);
        testManager.addConfigurationListener(b);
        testManager.setConfiguration(oldConfiguration);

        // When
        testManager.removeConfigurationListener(b);
        testManager.setConfiguration(newConfiguration);

        // Then
        assertThat(a.configuration).isEqualTo(newConfiguration);
        assertThat(b.configuration).isEqualTo(oldConfiguration);
    }

    /**
     * Publishing of configuration data to registered listeners should avoid any race conditions. That is, in the case
     * of multiple updates to the configuration from multiple threads, the final configuration state should always be
     * the last change that was broadcast to listeners so that a listener cannot have a stale copy of the configuration
     * data.
     */
    @Test
    public void shouldEnsureLastUpdatedConfigurationIsLastBroadcastConfiguration() throws Exception {
        // Given
        ExecutorService threadPool = Executors.newCachedThreadPool();
        List<TestConfigurationListener> listeners = registerListeners(100);
        final int numPublishers = 10;
        final CyclicBarrier barrier = new CyclicBarrier(numPublishers + 1);

        // When
        for (int i = 0; i < numPublishers; ++i) {
            threadPool.execute(new TestConfigurationPublisher(i, barrier));
        }
        barrier.await(); // Start the test
        barrier.await(); // Wait for end
        threadPool.shutdownNow();

        // Then
        for (TestConfigurationListener listener : listeners) {
            // We don't know who will 'win', but all listeners should be consistent with the test manager at end of test
            assertThat(listener.configuration).isEqualTo(testManager.getConfiguration());
        }
    }

    /**
     * Returns a configuration that is unlikely to be the default.
     */
    private ScriptEngineConfiguration getTestConfiguration() {
        return ScriptEngineConfiguration.builder()
                .withThreadPoolMaxSize(101)
                .withThreadPoolCoreSize(52)
                .withBlackList(Arrays.asList(Pattern.compile("some black list")))
                .withWhiteList(Arrays.asList(Pattern.compile("some white list")))
                .withSecurityManager(new SecurityManager())
                .withThreadPoolIdleTimeout(9, TimeUnit.HOURS)
                .withTimeout(4, TimeUnit.NANOSECONDS)
                .build();
    }

    private List<TestConfigurationListener> registerListeners(final int numListeners) {
        List<TestConfigurationListener> listeners = new ArrayList<TestConfigurationListener>(numListeners);
        for (int i = 0; i < numListeners; i++) {
            TestConfigurationListener listener = new TestConfigurationListener();
            listeners.addAll(listeners);
            testManager.addConfigurationListener(listener);
        }

        return listeners;
    }

    private static class TestConfigurationListener implements StandardScriptEngineManager.ConfigurationListener {
        ScriptEngineConfiguration configuration;
        @Override
        public void onConfigurationChange(final ScriptEngineConfiguration newConfiguration) {
            this.configuration = newConfiguration;
        }
    }

    private class TestConfigurationPublisher implements Runnable {
        private final ScriptEngineConfiguration myConfiguration;
        private final CyclicBarrier barrier;

        TestConfigurationPublisher(final int i, final CyclicBarrier barrier) {
            // It is sufficient to just change one parameter to ensure configurations are different
            myConfiguration = ScriptEngineConfiguration.builder()
                    .withThreadPoolMaxSize(i + 100)
                    .build();
            this.barrier = barrier;
        }

        @Override
        public void run() {
            try {
                barrier.await();
                for (int i = 0; i < 200; ++i) {
                    testManager.setConfiguration(myConfiguration);
                }
                barrier.await();
            } catch (Exception e) {
                // Ignore
            }
        }
    }
}
