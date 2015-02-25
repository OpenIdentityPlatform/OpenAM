/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: thread_pool.h,v 1.3 2008/06/25 08:14:39 qcheng Exp $
 *
 */
/*
 * Portions Copyrighted 2013 ForgeRock Inc
 */

#ifndef __THREAD_POOL_H__
#define __THREAD_POOL_H__

#include <vector>
#include <queue>
#include "internal_macros.h"
#include "internal_exception.h"
#include "log.h"
#include "thread_function.h"
#include "scope_lock.h"

BEGIN_PRIVATE_NAMESPACE

template <typename T>
class Queue {
public:

    Queue() : mutex(), condv() {
    }

    T remove() {
        mutex.lock();
        while (queue_.empty()) {
            condv.wait(mutex);
        }
        T val = queue_.front();
        queue_.pop();
        mutex.unlock();
        return val;
    }

    void add(const T& item) {
        mutex.lock();
        queue_.push(item);
        mutex.unlock();
        condv.signal();
    }

    int size() {
        int size = 0;
        mutex.lock();
        size = queue_.size();
        mutex.unlock();
        return size;
    }

private:
    std::queue<T> queue_;
    Mutex mutex;
    ConditionVariable condv;

    Queue(const Queue&);
    Queue& operator=(const Queue&);

};

class ThreadPool {
public:

    ThreadPool(std::size_t startThreads,
            std::size_t maxNumThreads);

    /**
     * By calling dispatch, the function that needs to be invoked,
     * gets added to the job queue.  The next available thread picks
     * up the job and performs it.
     */
    bool dispatch(ThreadFunction*);

    ~ThreadPool();
private:

    volatile bool exitNow;
    size_t maxThreads;
    Queue<ThreadFunction*> workQueue;
    std::vector<Thread*> threads;
    static Log::ModuleId logID;
    ThreadPool(const ThreadPool &);
    ThreadPool();
    ThreadPool& operator=(const ThreadPool &) const;

    void createNewThread();
};
END_PRIVATE_NAMESPACE
#endif
