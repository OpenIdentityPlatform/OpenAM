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
 * Copyrighted 2015 Intellectual Reserve, Inc (IRI)
 */
package org.forgerock.openam.radius.server.config;

import com.sun.identity.shared.debug.Debug;

/**
 * Configuration values for the thread pool loaded from OpenAM's admin console constructs.
 */
public class ThreadPoolConfig {

    private static final Debug logger = Debug.getInstance(RadiusServerConstants.RADIUS_SERVER_LOGGER);

    private static final int DEFAULT_CORE_THREADS = 1;
    private static final int DEFAULT_KEEP_ALIVE_SECONDS = 10;
    private static final int DEFAULT_QUEUE_SIZE = 20;

    private final int keepAliveSeconds;
    private final int queueSize;
    private final int maxThreads;
    private final int coreThreads;

    /**
     * Constructs a ThreadPoolConfig.
     *
     * @param core
     *            -the core number of threads required.
     * @param max
     *            - the max number of threads to be used by the thread pool
     * @param queueSize
     *            - the size of the queue
     * @param keepAliveSeconds
     *            - when the number of threads is greater than the core, this is the maximum time that excess idle
     *            threads will wait for new tasks before terminating.
     */
    public ThreadPoolConfig(int core, int max, int queueSize, int keepAliveSeconds) {
        if (core < 1) {
            this.coreThreads = DEFAULT_CORE_THREADS;
            logger.warning("System configured to used invalid Radius Thread Pool Core size of " + core
                    + ". Using the value of " + DEFAULT_CORE_THREADS + " instead.");
        } else {
            this.coreThreads = core;
        }

        if (max < this.coreThreads) {
            this.maxThreads = coreThreads;
            logger.warning("System configured to use Radius Server 'Thread Pool Max Size' that is less than 'Thread "
                    + "Pool Core Size. Using size equal to Core Size - i.e. a static pool of size " + coreThreads);
        } else {
            this.maxThreads = max;
        }

        if (queueSize < 1 || queueSize > 1000) {
            this.queueSize = DEFAULT_QUEUE_SIZE;
            logger.warning("System configured to use an invalid Radius Server 'Thread Pool Queue Size' value of '"
                    + queueSize + "'. Using the default value of '" + DEFAULT_QUEUE_SIZE + "' instead.");
        } else {
            this.queueSize = queueSize;
        }

        if (keepAliveSeconds < 1 || keepAliveSeconds > 3600) {
            this.keepAliveSeconds = DEFAULT_KEEP_ALIVE_SECONDS;
            logger.warning("System configured to use an invalid Radius Server 'Thread Pool Keep-Alive Seconds' value of"
                    + " '"
                    + keepAliveSeconds
                    + "'. Using the default value of '"
                    + DEFAULT_KEEP_ALIVE_SECONDS
                    + "' instead.");
        } else {
            this.keepAliveSeconds = keepAliveSeconds;
        }
    }

    /**
     * Return the number of seconds that, when the number of threads is greater than the core, is the maximum time that
     * excess idle threads will wait for new tasks before terminating.
     *
     * @return the keepAliveSeconds
     */
    public int getKeepAliveSeconds() {
        return this.keepAliveSeconds;
    }

    /**
     * Return the size of the queue used to queue work items.
     *
     * @return the queueSize
     */
    public int getQueueSize() {
        return this.queueSize;
    }

    /**
     * Get the maximum number of threads the thread pool can hold.
     *
     * @return the maxThreads
     */
    public int getMaxThreads() {
        return this.maxThreads;
    }

    /**
     * Get the number of core threads - the thread pool should keep this many threads around even if they have no work
     * to do.
     *
     * @return the coreThreads
     */
    public int getCoreThreads() {
        return this.coreThreads;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || !(o instanceof ThreadPoolConfig)) {
            return false;
        }
        final ThreadPoolConfig t = (ThreadPoolConfig) o;
        return keepAliveSeconds == t.keepAliveSeconds && queueSize == t.queueSize && maxThreads == t.maxThreads
                && coreThreads == t.coreThreads;
    }

    // don't really need to being a good citizen
    @Override
    public int hashCode() {
        return keepAliveSeconds + queueSize + maxThreads + coreThreads;
    }
}
