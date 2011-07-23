/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2005 Sun Microsystems Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * https://opensso.dev.java.net/public/CDDLv1.0.html or
 * opensso/legal/CDDLv1.0.txt
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at opensso/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id: ThreadPool.java,v 1.3 2008/06/25 05:41:41 qcheng Exp $
 *
 */

package com.iplanet.services.util;

// TODO:
// Stopping individual tasks
// ThreadGroups
// Exception propagation or callbacks

import com.sun.identity.shared.debug.Debug;

/**
 * <p>
 * ThreadPool is a generic thread pool that manages and recycles threads instead
 * of creating them everytime some task needs to be run on a different thread.
 * Thread pooling saves the virtual machine the work of creating brand new
 * threads for every short-lived task. In addition, it minimizes overhead
 * associated with getting a thread started and cleaning it up after it dies. By
 * creating a pool of threads, a single thread from the pool can be reused any
 * number of times for different tasks.
 * </p>
 * <p>
 * This reduces response time because a thread is already constructed and
 * started and is simply waiting for its next task. This is particularly useful
 * when using many short-lived tasks. This may not be useful for long-lived
 * tasks.
 * </p>
 * <p>
 * In future, this class may be enhanced to provide support growing the size of
 * the pool at runtime to facilitate dynamic tuning.
 * </p>
 */

public final class ThreadPool {
    private static int nextThreadID = 1;

    private Debug debug;

    private String poolName;

    private IPSThread[] allThreadList;

    private IPSThread[] idleThreadList;

    /**
     * tail points to the last available idle thread in the idleThreadList. When
     * the idleThreadList is empty, tail is set to -1. IMPORTANT: tail MUST
     * always be accessed by acquiring a lock on idleThreadList. Otherwise, the
     * code will break.
     */
    private volatile int tail = -1;

    /**
     * <p>
     * Constructs a thread pool with the poolName and given number of threads.
     * </p>
     * 
     * @param poolName
     *            name of the thread pool
     * @param numThreads
     *            maximum number of threads in the thread pool.
     * @param daemon
     *            if true, all threads created will be daemon threads. If false,
     *            all threads created will be non-daemon threads.
     * @param debug
     *            Debug object to send debug messages to.
     * 
     * @throws IllegalArgumentException
     *             if poolName is null
     */
    public ThreadPool(String poolName, int numThreads, boolean daemon,
            Debug debug) throws IllegalArgumentException {
        if (poolName != null) {
            this.poolName = poolName;
        } else {
            throw new IllegalArgumentException(
                    "Must assign a non-null pool name to ThreadPool");
        }

        this.debug = debug;

        // ensure that there is at least one thread in the pool
        numThreads = Math.max(1, numThreads);

        // Normally Lists should be used. However, since this class is a
        // performance-critical low level code, arrays are used to fully
        // optmize.
        idleThreadList = new IPSThread[numThreads];
        allThreadList = new IPSThread[numThreads];

        // Now initialized only allThreads list. idleThreads list will be
        // updated by the threads when they are idle and ready.

        for (int i = 0; i < numThreads; ++i) {
            allThreadList[i] = new IPSThread(getNextIPSThreadID(), daemon);
        }
    }

    /** Suppress the no-arg constructor because it is not supported */
    private ThreadPool() {
    }

    /** Gets the unique name for the internal thread in the pool */
    private synchronized String getNextIPSThreadID() {
        return poolName + ".Thread#" + Integer.toString(nextThreadID++);
    }

    /**
     * Runs the user-defined task. To enable better debugging or profiling
     * capabilities, the <code>task</code> <code>Runnable</code> should
     * implement <code>toString()</code> to intuitively identify the task.
     * 
     * @param task
     *            the user-defined Runnable to be scheduled for execution on
     *            this thread pool
     * @throws InterruptedException
     *             when the thread invoking <code>run</code> is interrupted.
     */
    public final void run(Runnable task) throws InterruptedException {
        IPSThread ipsThread;
        synchronized (idleThreadList) {
            while (tail == -1) {
                if ((debug != null) && (debug.warningEnabled())) {
                    debug.warning(Thread.currentThread().getName()
                            + " waiting for an idle thread in " + toString());
                }
                idleThreadList.wait();
            }

            // Now there is at least one idle thread available
            ipsThread = idleThreadList[tail--];

            // Now that the idle thread is off the idleThreadList, there is no
            // danger of some other task being simultaneously assigned to that
            // idle thread. Release the lock on idleThreadList now so that
            // other tasks can be processed concurrently.
        }
        // logMessage(Thread.currentThread().getName() + " assigning task '" +
        // task + "' to " + ipsThread.getName());

        ipsThread.process(task);
    }

    /**
     * Stops all the idle threads in the pool. Note that these stopped threads
     * are no longer availble for future tasks because they are returned to
     * underlying virtual machine. Also note that none of the active threads in
     * the pool are stopped.
     */
    public final void stopIdleThreads() {
        synchronized (idleThreadList) {
            while (tail >= 0) {
                IPSThread idleThread = idleThreadList[tail];
                idleThreadList[tail--] = null;
                idleThread.stop();
            }
        }
    }

    /**
     * Destroys the thread pool. This stops all the threads, active and idle, in
     * the pool and releases all resources.
     */
    public final void destroy() {
        // stop the idle threads first to be more productive
        stopIdleThreads();

        try {
            // give the idle threads a chance to die
            Thread.sleep(500);
        } catch (InterruptedException e) {
            // no need to reassert InterruptedException here because the
            // pool is being shutdown
        }

        // now go through allThreadList and stop everything
        synchronized (allThreadList) {
            int numThreads = allThreadList.length;
            int i = 0;
            while (i < numThreads) {
                IPSThread ipsThread = allThreadList[i];
                allThreadList[i++] = null;
                if (ipsThread.isAlive()) {
                    ipsThread.stop();
                }
            }
        }
    }

    /**
     * Returns the string representation of this thread pool that includes the
     * name, size and the number of currently idle threads
     */
    public String toString() {
        synchronized (idleThreadList) {
            return poolName + "[" + allThreadList.length + " Total threads, "
                    + ((tail >= 0) ? (tail + 1) : 0) + " Idle threads]";
        }
    }

    /** Returns the name of this thread pool */
    public final String getName() {
        return poolName;
    }

    private final void logMessage(String msg) {
        if (debug == null) {
            return;
        }
        debug.message(msg);
    }

    /** ******************************************************************** */
    /** An inner class that actually executes the user-defined tasks. */
    private final class IPSThread {
        // This internal thread will handle only one task at a time
        private volatile Runnable task;

        private Thread thread;

        private volatile boolean stopped = false;

        IPSThread(String name, boolean daemon) {
            Runnable r = new Runnable() {
                public void run() {
                    try {
                        runTask();
                    } catch (Throwable t) {
                        // In case any exception slips through
                        if (debug != null) {
                            debug.error(IPSThread.this.thread.getName()
                                    + " caught exception that fell through", t);
                        }
                    }
                }
            };
            thread = new Thread(r, name);
            thread.setDaemon(daemon);
            thread.start();
        }

        /** Explicitly suppressed because it is not supported */
        private IPSThread() {
        }

        /**
         * Accepts the user-defined task for execution. Note that by design this
         * method is called only when this thread is idle.
         */
        final synchronized void process(Runnable task)
                throws InterruptedException {
            this.task = task;

            // Since only one thread can wait on this object, notifyAll() is
            // not used.
            notify();
        }

        /**
         * Actually runs the user-defined task. This thread adds itself to the
         * idleThreadList in the parent pool and waits to be assinged a task.
         * When the task is assinged, it goes ahead and executes it. While
         * executing, this thread is not on the idleThreadList.
         */
        private void runTask() {
            while (!stopped) {
                try {
                    // This thread is ready to rock-n-roll! Add this thread to
                    // the idleThreadList
                    synchronized (idleThreadList) {
                        idleThreadList[++tail] = this;

                        // If idleThreadList was empty, notify the waiting
                        // threads
                        if (tail == 0) {
                            idleThreadList.notifyAll();
                        }
                    }

                    // Now wait until the parent pool assigns this thread a task
                    synchronized (this) {
                        while (task == null) {
                            wait();
                        }
                    }

                    // logMessage(thread.getName() + " is running the task '" +
                    // task + "'");
                    try {
                        task.run();
                    } catch (Exception e) {
                        if (debug != null) {
                            debug.error(thread.getName()+ 
                                    " caught exception that fell through from "
                                                    + task + ".run()", e);
                        }
                    } finally {
                        // Clear the interrupted flag (in case it comes back
                        // set) so that if the loops goes again, the task.wait()
                        // does not mistakenly throw an InterruptedException.

                        Thread.interrupted();
                    }
                } catch (InterruptedException e) {
                    // This catch must be here (in addition to the one inside
                    // the corresponding try{}) so that a task is not run
                    // mistakenly after this thread is interrupted.

                    Thread.currentThread().interrupt(); // re-assert
                } catch (Throwable t) {
                    // Fatal exception occurred. But we don't want to stop this
                    // thread as that might only deplete the thread pool.
                    if (debug != null) {
                        debug.error(thread.getName()+ 
                                ": runTask() caught throwable. Investigate " +
                                "the problem", t);
                    }
                } finally {
                    // set task to null so that the task doesn't get executed
                    // repeatedly.
                    // logMessage(thread.getName() +
                    // " compeleted the task '" + task + "'");
                    task = null;
                }
            }
            if (debug != null) {
                debug.error(thread.getName() + " stopped.", (Throwable) null);
            }
        }

        /**
         * Stops this thread. This method may return before this thread actually
         * dies.
         */
        private final void stop() {
            logMessage(thread.getName() + " received stop() request.");
            stopped = true;
            thread.interrupt();
        }

        /** Gets the name of this thread */
        final String getName() {
            return thread.getName();
        }

        /** Checks if this thread is alive or dead */
        final boolean isAlive() {
            return thread.isAlive();
        }
    }
}
