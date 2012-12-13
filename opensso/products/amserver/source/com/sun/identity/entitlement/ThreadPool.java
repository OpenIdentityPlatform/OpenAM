/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: ThreadPool.java,v 1.1 2009/08/19 05:40:34 veiming Exp $
 *
 */
package com.sun.identity.entitlement;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * <p>
 * This thread pool maintains a number of threads that run the tasks from a task
 * queue one by one. The tasks are handled in asynchronous mode, which means it
 * will not block the main thread to proceed while the task is being processed
 * by the thread pool.
 * <p>
 * This thread pool has a fixed size of threads. It maintains all the tasks to
 * be executed in a task queue. Each thread then in turn gets a task from the
 * queue to execute. If the tasks in the task queue reaches a certain number(the
 * threshold value), it will log an error message and ignore the new incoming
 * tasks until the number of un-executed tasks is less than the threshold value.
 * This guarantees the thread pool will not use up the system resources under
 * heavy load.
 */
public class ThreadPool {

    private int poolSize;
    private String poolName;
    private List<Runnable> taskList;
    private WorkerThread[] threads;
    private Lock lock = new ReentrantLock();
    private Condition hasTasks = lock.newCondition();

    /**
     * Constructs a thread pool with given parameters.
     * 
     * @param name
     *            name of the thread pool.
     * @param poolSize
     *            the thread pool size, indicates how many threads are created
     *            in the pool.
     */
    public ThreadPool(String name, int poolSize) {
        this.poolSize = poolSize;
        this.poolName = name;
        taskList = new LinkedList<Runnable>();
        threads = new WorkerThread[poolSize];
        createThreads();
    }

    /**
     * Create thread for the pool.
     *
     * @param threadsToCreate number of threads of the pool after creation
     */
    private synchronized  void createThreads() {
        for (int i = 0; i < poolSize; i++) {
            WorkerThread t = new WorkerThread(poolName, this);
            t.setDaemon(true);
            t.start();
            threads[i] = t;
        }
    }

    /**
     * Runs a user defined task.
     * 
     * @param task user defined task.
     * @throws ThreadPoolException
     */
    public final void run(Runnable task)
        throws ThreadPoolException {
        try {
            lock.lock();
            taskList.add(task);
            hasTasks.signal();
        } finally {
            lock.unlock();
        }
    }

    private class WorkerThread extends Thread {
        private ThreadPool pool;

        public WorkerThread(String name, ThreadPool pool) {
            setName(name);
            this.pool = pool;
        }

        /**
         * Starts the thread pool.
         */
        @Override
        public void run() {
            while (true) {
                Runnable task = null;
                try {
                    pool.lock.lock();
                    if (!pool.taskList.isEmpty()) {
                        task = taskList.remove(0);
                    } else {
                        pool.hasTasks.await();
                    }
                } catch (InterruptedException ex) {
                    PrivilegeManager.debug.error("WorkerThread.run", ex);
                } finally {
                    pool.lock.unlock();
                }
                if (task != null) {
                    task.run();
                }
            }
        }
    }
}
