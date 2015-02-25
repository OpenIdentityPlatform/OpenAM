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
 * $Id: thread_pool.cpp,v 1.4 2008/06/25 08:14:39 qcheng Exp $
 *
 */
/*
 * Portions Copyrighted 2012-2013 ForgeRock Inc
 */
#include "thread_pool.h"

USING_PRIVATE_NAMESPACE

#ifndef _MSC_VER

extern "C" {

    void* ThreadProc(void* arg) {
        return ((Thread*) arg)->run();
    }
}

#endif

Log::ModuleId ThreadPool::logID;

class WorkerThread : public Thread {
    Queue<ThreadFunction*>& queue;
    static Log::ModuleId logID;

public:

    WorkerThread(Queue<ThreadFunction*>& q) : queue(q) {
        logID = Log::addModule("WorkerThread");
    }

    void* run() {
        for (;;) {
            ThreadFunction* item = (ThreadFunction*) queue.remove();
            if (item) {
                Log::log(logID, Log::LOG_DEBUG,
                        "::run(): calling %s thread function.", item->getName().c_str());
                bool exitNow = (*item).exit();
                try {
                    (*item)();
                } catch (...) {
                    Log::log(logID, Log::LOG_ERROR,
                            "::run(): Thread Function call threw an exception.");
                }
                Log::log(logID, Log::LOG_DEBUG,
                        "::run(): %s thread function done, deleting. ", item->getName().c_str());
                delete item;
                if (exitNow) break;
            }
        }
        return NULL;
    }
};

Log::ModuleId WorkerThread::logID;

ThreadPool::ThreadPool(std::size_t startThreads, std::size_t maxNumThreads) {
    logID = Log::addModule("ThreadPool");
    exitNow = false;
    maxThreads = maxNumThreads;
    threads.clear();
    for (std::size_t i = 0; i < startThreads; i++) {
        createNewThread();
    }
}

void ThreadPool::createNewThread() {
    if (maxThreads > 0 && exitNow == false) {
        WorkerThread *thread = new WorkerThread(workQueue);
        if (thread) {
            thread->start();
            threads.push_back(thread);
        }
    }
}

/*
 * NOTE
 * ====
 * The ThreadFunction in the input parameter is deleted after it has
 * finished executing.
 */
bool ThreadPool::dispatch(ThreadFunction *fObj) {
    bool retVal = false;
    if (maxThreads > 0 && exitNow == false) {
        /* queue worker thread count is fixed (created per ThreadPool; startThreads) */
        if (fObj) {
            Log::log(logID, Log::LOG_DEBUG,
                    "::dispatch(): adding %s thread function to work queue", fObj->getName().c_str());
            workQueue.add(fObj);
            retVal = true;
        }
    }
    return retVal;
}

ThreadPool::~ThreadPool() {
    maxThreads = 0;
    exitNow = true;

    for (unsigned int i = 0; i < threads.size(); i++) {
        workQueue.add(new WorkerThreadExit());
    }

    std::vector<Thread*>::iterator iter;
    Log::log(logID, Log::LOG_MAX_DEBUG,
            "ThreadPool::~ThreadPool(): "
            "Number of threads to be joined: %d.",
            threads.size());

    for (iter = threads.begin(); iter != threads.end(); ++iter) {
        Thread *h = (*iter);
        h->join();
    }

    Log::log(logID, Log::LOG_DEBUG,
            "ThreadPool::~ThreadPool(): All Threads joined.");
    Log::log(logID, Log::LOG_INFO,
            "ThreadPool::~ThreadPool(): ThreadPool destroyed.");
}
