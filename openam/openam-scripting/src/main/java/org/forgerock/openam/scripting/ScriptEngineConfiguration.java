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

import org.forgerock.util.Reject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * Holds the current configuration options for script engine execution. This controls various aspects of script
 * execution, such as maximum time-outs, white-/black-lists for sandboxing, etc. This is an immutable value object to
 * ensure that a script engine can rely on the configuration not changing during execution of a script. Configuration
 * changes are made by updating the entire configuration object on the {@link StandardScriptEngineManager} as an
 * atomic action and will be picked up by new script executions, while existing executions will continue using the old
 * configuration. This ensures consistency of configuration parameters during script execution.
 *
 * @since 12.0.0
 */
public final class ScriptEngineConfiguration {
    /**
     * Constant to indicate no script timeout should be used.
     */
    public static final long NO_TIMEOUT = 0;

    /**
     * Constant to indicate that the thread pool queue should be unbounded.
     */
    public static final int UNBOUNDED_QUEUE_SIZE = -1;

    private final long scriptExecutionTimeout;
    private final List<Pattern> classWhiteList;
    private final List<Pattern> classBlackList;
    private final SecurityManager securityManager;
    private final int threadPoolCoreSize;
    private final int threadPoolMaxSize;
    private final int threadPoolQueueSize;
    private final long threadPoolIdleTimeout;

    /**
     * Constructs a script configuration with the given parameters.
     *
     * @param scriptExecutionTimeout the maximum length of time to allow a script to run, in seconds. Must be >= 0.
     * @param classWhiteList a list of patterns of allowed java classes that can be loaded or accessed by the script.
     *                       May not be null. May be empty to deny access to all Java classes (i.e., pure script only).
     * @param classBlackList a list of patterns of disallowed java classes that cannot be loaded or accessed by the
     *                       script. Overrides patterns from the whitelist. May not be null. May be empty.
     * @param securityManager the Java SecurityManager to consult when loading Java classes via the
     *                        {@link SecurityManager#checkPackageAccess(String)} method. May be null.
     * @param threadPoolCoreSize the core number of threads to keep in the thread pool used to execute scripts.
     * @param threadPoolMaxSize the maximum number of threads to use in the pool when the task queue is at capacity.
     * @param threadPoolQueueSize the size of the task queue to use to buffer pending script requests.
     * @param threadPoolIdleTimeout the amount of time (in seconds) to wait before terminating additional threads
     *                              beyond the core size after a queue backlog has been cleared.
     */
    ScriptEngineConfiguration(final long scriptExecutionTimeout, final List<Pattern> classWhiteList,
                              final List<Pattern> classBlackList, final SecurityManager securityManager,
                              final int threadPoolCoreSize, final int threadPoolMaxSize,
                              final int threadPoolQueueSize, final long threadPoolIdleTimeout) {
        Reject.ifNull(classWhiteList, classBlackList);
        Reject.ifTrue(scriptExecutionTimeout < 0);
        Reject.ifTrue(threadPoolCoreSize < 1);
        Reject.ifTrue(threadPoolMaxSize < 1);
        Reject.ifTrue(threadPoolQueueSize < -1);
        Reject.ifTrue(threadPoolIdleTimeout < 0);

        this.scriptExecutionTimeout = scriptExecutionTimeout;
        this.classWhiteList = new ArrayList<Pattern>(classWhiteList);
        this.classBlackList = new ArrayList<Pattern>(classBlackList);
        this.securityManager = securityManager;
        this.threadPoolCoreSize = threadPoolCoreSize;
        this.threadPoolMaxSize = threadPoolMaxSize;
        this.threadPoolQueueSize = threadPoolQueueSize;
        this.threadPoolIdleTimeout = threadPoolIdleTimeout;
    }

    /**
     * Returns the maximum script execution time limit, in seconds. Returns {@link #NO_TIMEOUT} to indicate no timeout.
     *
     * @return the maximum script execution time in seconds.
     */
    public long getScriptExecutionTimeout() {
        return scriptExecutionTimeout;
    }

    /**
     * Gets the current Java class name whitelist patterns. Every Java class that is accessed by a script must match
     * at least one of the regular expression patterns in this list, otherwise an error will be raised and script
     * execution halted. Patterns should match the fully-qualified class name.
     *
     * @return the Java class white-list. Never null. Unmodifiable.
     */
    public List<Pattern> getClassWhiteList() {
        return Collections.unmodifiableList(classWhiteList);
    }

    /**
     * Gets the current Java class name blacklist patterns. Any Java class that matches one of the whitelist patterns is
     * then matched against each pattern in this blacklist. If the fully-qualified class name matches any pattern in
     * this blacklist then access to that class is denied, and an error is raised.
     *
     * @return the Java class black-list. Never null. Unmodifiable.
     */
    public List<Pattern> getClassBlackList() {
        return Collections.unmodifiableList(classBlackList);
    }

    /**
     * Gets the Java SecurityManager to use when checking if a Java class or package should be exposed to a script. If
     * null then no additional checks are done beyond the white-/black-listing. If non-null, then the
     * {@link java.lang.SecurityManager#checkPackageAccess(String)} method will be called for every Java class that is
     * accessed by the script.
     *
     * @return the SecurityManager to use to check package access. May be null to indicate no security manager.
     */
    public SecurityManager getSecurityManager() {
        return securityManager;
    }

    /**
     * The number of threads to keep in the thread pool used to service script execution requests. The pool will grow
     * as new script executions are submitted up to this size initially before new tasks are queued to be processed
     * later.
     *
     * @return the core number of threads in the thread pool.
     */
    public int getThreadPoolCoreSize() {
        return threadPoolCoreSize;
    }

    /**
     * The maximum number of threads to use to service script execution requests once the task queue has reached
     * capacity. After the queue backlog has been serviced any additional threads beyond the core pool size (up to this
     * maximum size) will eventually be terminated (after the idle timeout has elapsed). This setting has no effect if
     * the queue is unbounded.
     *
     * @return the maximum number of threads to use when the task queue is at capacity.
     */
    public int getThreadPoolMaxSize() {
        return threadPoolMaxSize;
    }

    /**
     * The size of the queue to use for buffering tasks when all threads in the thread pool are in use. Use
     * {@link #UNBOUNDED_QUEUE_SIZE} to specify an unbounded queue (this disables the threadPoolMaxSize option). There
     * is a trade-off between queue size and maximum pool size. For long-running I/O-bound scripts (e.g., those making
     * REST calls) it is better to have a larger maximum pool size and a smaller queue size so that other threads can
     * run while some are blocked in I/O calls. The short queue size ensures that new requests do not wait too long to
     * be serviced. For scripts that are short-running and CPU-bound then the reverse is true: use a smaller pool size
     * to minimise thread scheduling and context-switching overhead, with a longer queue to avoid rejected execution
     * exceptions.
     *
     * @return the size of the queue to use for buffering script execution requests.
     */
    public int getThreadPoolQueueSize() {
        return threadPoolQueueSize;
    }

    /**
     * The number of seconds to wait before terminating additional threads (beyond core pool size, up to max pool size)
     * that were started to service a full task queue. A longer timeout will allow the pool to react more quickly to
     * another burst (as new threads do not need to be created and started), but the additional threads will consume
     * some resources (memory, O/S threads, etc) during this time. This setting has no effect on threads in the core
     * pool size as these are only terminated on shutdown.
     *
     * @return the thread idle timeout in seconds.
     */
    public long getThreadPoolIdleTimeoutSeconds() {
        return threadPoolIdleTimeout;
    }

    /**
     * Creates a fresh configuration builder.
     * @return a fresh configuration builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String toString() {
        return "ScriptEngineConfiguration{" +
                "scriptExecutionTimeout=" + scriptExecutionTimeout +
                ", threadPoolCoreSize=" + threadPoolCoreSize +
                ", threadPoolMaxSize=" + threadPoolMaxSize +
                ", threadPoolQueueSize=" + threadPoolQueueSize +
                ", threadPoolIdleTimeoutSeconds=" + threadPoolIdleTimeout +
                ", classWhiteList=" + classWhiteList +
                ", classBlackList=" + classBlackList +
                ", securityManager=" + securityManager +
                '}';
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final ScriptEngineConfiguration that = (ScriptEngineConfiguration) o;

        if (scriptExecutionTimeout != that.scriptExecutionTimeout) {
            return false;
        }
        if (threadPoolCoreSize != that.threadPoolCoreSize) {
            return false;
        }
        if (threadPoolMaxSize != that.threadPoolMaxSize) {
            return false;
        }
        if (threadPoolQueueSize != that.threadPoolQueueSize) {
            return false;
        }
        if (threadPoolIdleTimeout != that.threadPoolIdleTimeout) {
            return false;
        }
        if (!classBlackList.equals(that.classBlackList)) {
            return false;
        }
        if (!classWhiteList.equals(that.classWhiteList)) {
            return false;
        }
        if (securityManager != null ? !securityManager.equals(that.securityManager) : that.securityManager != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = Long.valueOf(scriptExecutionTimeout).hashCode();
        result = 31 * result + classWhiteList.hashCode();
        result = 31 * result + classBlackList.hashCode();
        result = 31 * result + (securityManager != null ? securityManager.hashCode() : 0);
        result = 31 * result + threadPoolCoreSize;
        result = 31 * result + threadPoolMaxSize;
        result = 31 * result + threadPoolQueueSize;
        result = 31 * result + Long.valueOf(threadPoolIdleTimeout).hashCode();
        return result;
    }

    /**
     * Builder pattern for constructing immutable script configuration instances using a fluent API.
     */
    public static final class Builder {
        private long timeout = NO_TIMEOUT;
        private List<Pattern> whiteList = new ArrayList<Pattern>();
        private List<Pattern> blackList = new ArrayList<Pattern>();
        private SecurityManager securityManager = null;
        private int coreSize = 1;
        private int maxSize = 1;
        private int queueSize = 10;
        private long idleTimeout = 60l;

        /**
         * Sets the maximum amount of time (in seconds) to allow a script to execute before interrupting it. Use
         * {@link #NO_TIMEOUT} for no timeout.
         *
         * @param timeout the script execution timeout. Must be >= 0.
         * @param unit the time units of the timeout.
         * @return this.
         */
        public Builder withTimeout(final long timeout, final TimeUnit unit) {
            Reject.ifFalse(timeout >= 0, "Timeout must be >= 0");
            this.timeout = TimeUnit.SECONDS.convert(timeout, unit);
            return this;
        }

        /**
         * Sets the Java class white-list for classes that a script should be allowed to access.
         *
         * @param whiteList the class white list. Must not be null.
         * @return this.
         */
        public Builder withWhiteList(final List<Pattern> whiteList) {
            Reject.ifNull(whiteList);
            this.whiteList = whiteList;
            return this;
        }

        /**
         * Sets the Java class black-list for classes that a script should be forbidden from accessing. Applies after
         * the whitelist.
         *
         * @param blackList the class black list. Must not be null.
         * @return this.
         */
        public Builder withBlackList(final List<Pattern> blackList) {
            Reject.ifNull(blackList);
            this.blackList = blackList;
            return this;
        }

        /**
         * Sets the security manager to use when checking access to Java packages in script execution.
         *
         * @param securityManager the security manager to use. Use null to disable.
         * @return this.
         */
        public Builder withSecurityManager(final SecurityManager securityManager) {
            this.securityManager = securityManager;
            return this;
        }

        /**
         * Sets the security manager to use when checking access to Java packages to the configured system security
         * manager (if enabled).
         *
         * @see System#getSecurityManager()
         * @return this.
         */
        public Builder withSystemSecurityManager() {
            return withSecurityManager(System.getSecurityManager());
        }

        /**
         * Sets the core size of the thread pool to use for executing scripts (see
         * {@link org.forgerock.openam.scripting.ThreadPoolScriptEvaluator}). This is the number of threads that will
         * be created to service requests before new requests are queued.
         *
         * @param coreSize the number of threads to keep in the thread pool. Must be >= 1.
         * @return this.
         */
        public Builder withThreadPoolCoreSize(final int coreSize) {
            Reject.ifTrue(coreSize < 1, "Must configure at least one thread");
            this.coreSize = coreSize;
            return this;
        }

        /**
         * Sets the maximum size of the thread pool to use for executing scripts (see
         * {@link org.forgerock.openam.scripting.ThreadPoolScriptEvaluator}). This is the maximum number of threads that
         * will be created once the queue reaches capacity.
         *
         * @param maxSize the maximum number of threads to use in the thread pool.
         * @return this.
         */
        public Builder withThreadPoolMaxSize(final int maxSize) {
            Reject.ifTrue(maxSize < 1, "Must configure at least one thread");
            this.maxSize = maxSize;
            return this;
        }

        /**
         * Sets the size of the queue to use for buffering requests for script execution once all core threads are in
         * use. Use {@link #UNBOUNDED_QUEUE_SIZE} for no limit (in which case the maximum pool size option has no
         * effect). Once the core thread pool is at capacity then new script execution requests will be queued up to
         * this limit. Once this limit is reached, then additional threads will be created to service the queue, up to
         * the maximum queue size.
         *
         * @param queueSize the maximum queue size or {@link #UNBOUNDED_QUEUE_SIZE} for no limit. Cannot be negative if
         *                  specified.
         * @return this.
         */
        public Builder withThreadPoolQueueSize(final int queueSize) {
            Reject.ifTrue(queueSize < UNBOUNDED_QUEUE_SIZE, "Queue size cannot be negative");
            this.queueSize = queueSize;
            return this;
        }

        /**
         * Sets the timeout after which additional threads (beyond the core pool size) will be terminated once pending
         * requests on the queue have been satisfied.
         *
         * @param timeout the thread idle timeout.
         * @param units the time unit of the timeout.
         * @return this.
         */
        public Builder withThreadPoolIdleTimeout(final long timeout, final TimeUnit units) {
            Reject.ifTrue(timeout < 0, "Idle timeout cannot be negative");
            this.idleTimeout = TimeUnit.SECONDS.convert(timeout, units);
            return this;
        }

        /**
         * Builds the script engine configuration object from the specified parameters.
         *
         * @return a non-null script engine configuration.
         * @throws java.lang.IllegalStateException if inconsistent parameters have been specified.
         */
        public ScriptEngineConfiguration build() {
            if (maxSize < coreSize) {
                throw new IllegalStateException("Maximum thread pool size is less than core size");
            }
            return new ScriptEngineConfiguration(timeout, whiteList, blackList, securityManager, coreSize, maxSize,
                    queueSize, idleTimeout);
        }
    }
}
