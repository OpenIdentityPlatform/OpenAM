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
 * Portions Copyrighted 2012 ForgeRock AS
 */
#include "thread_pool.h"

USING_PRIVATE_NAMESPACE

Log::ModuleId ThreadPool::logID;
#define MAX_THREAD_WAKEUP_TIME 15

// Throws std::bad_alloc, NSPRException.
ThreadPool::ThreadPool(std::size_t startThreads,
		       std::size_t maxNumThreads) {
    logID = Log::addModule("ThreadPool");
    lock = NULL;
    exitNow = false;
    condVar = NULL;
    threadStarted = NULL;
    maxThreads = maxNumThreads;
    activeThreads = 0;
    workQueue.clear();
    threads.clear();
    init(startThreads, maxNumThreads);
}

/*
 * Throws:
 *	std::bad_alloc if out of memory.
 *	NSPRException upon NSPR error
 */
void
ThreadPool::init(std::size_t startThreads,
		 std::size_t maxNumThreads) {
    lock = NULL;
    maxThreads = maxNumThreads;
    activeThreads = 0;
    exitNow = false;
    condVar = NULL;
    threadStarted = NULL;

    lock = PR_NewLock();
    if(lock == NULL) {
	PRErrorCode error = PR_GetError();
	Log::log(logID, Log::LOG_ERROR,
		 "ThreadPool::ThreadPool(size_t, size_t) : "
		 "NSPRException : Error during PR_NewLock() : Code %s.",
		 PR_ErrorToString(error, PR_LANGUAGE_I_DEFAULT));
	throw NSPRException("ThreadPool::ThreadPool", "PR_NewLock", error);
    }

    condVar = PR_NewCondVar(lock);
    if(condVar == NULL) {
	PRErrorCode error = PR_GetError();
	Log::log(logID, Log::LOG_ERROR,
		 "ThreadPool::ThreadPool(size_t, size_t) : NSPRException : "
		 "Error  during PR_NewCondVar() : Error Code = %s.",
		 PR_ErrorToString(error, PR_LANGUAGE_I_DEFAULT));
	throw NSPRException("ThreadPool::ThreadPool", "PR_NewCondVar", error);
    }

    threadStarted = PR_NewCondVar(lock);
    if(threadStarted == NULL) {
	PRErrorCode error = PR_GetError();
	Log::log(logID, Log::LOG_ERROR,
		 "ThreadPool::ThreadPool(size_t, size_t) : NSPRException : "
		 "Error  during PR_NewCondVar() : Error Code = %s.",
		 PR_ErrorToString(error, PR_LANGUAGE_I_DEFAULT));
	throw NSPRException("ThreadPool::ThreadPool", "PR_NewCondVar", error);
    }

    maxThreads = maxNumThreads;
    for(std::size_t i = 0; i < startThreads; i++) {
	createNewThread();
    }
}

/* Throws
 *	std::bad_alloc if out of memory.
 *	NSPRException upon NSPR error
 *	InternalException upon other errors.
 */
void
ThreadPool::createNewThread()
{
    std::size_t x = activeThreads;
    std::size_t cnt = 0;

    if (activeThreads >= maxThreads)
        return;

    PR_Lock(lock);

    PRThread  *thread = PR_CreateThread(PR_SYSTEM_THREAD,
					::spin,
					this,
					PR_PRIORITY_NORMAL,
					PR_GLOBAL_THREAD,
					PR_JOINABLE_THREAD,
					0);
    threads.push_back(thread);
    if(thread == NULL) {
	PRErrorCode error = PR_GetError();
	throw NSPRException("ThreadPool::createNewThread", "PR_CreateThread",
			    error);
    }

    // We have to make sure that the new thread we create
    // actually gets into commision.  Otherwise, if we create
    // a thread pool and immediately try distructing it, the
    // ~ThreadPool will take a lock and prevent from all the
    // worker threads from entering the wait-for-work state.

    // This WaitCondVar will allow the newly created thread to run...
    // once the thread is run, It will mark the threadStarted, and
    // WaitCondVar will return Already Locked
    Log::log(ThreadPool::logID, Log::LOG_INFO,"::createNewThread Unlocking...");

    PR_WaitCondVar(threadStarted,PR_TicksPerSecond() * MAX_THREAD_WAKEUP_TIME);

    Log::log(ThreadPool::logID, Log::LOG_INFO,"::createNewThread Returning...");

    if(activeThreads == x) {
	    Log::log(logID, Log::LOG_ERROR,
		     "ThreadPool::createNewThread(): Attempt to create "
		     "thread failed.");
	    throw InternalException("ThreadPool::createNewThread()",
				    "Thread not started.  The host process "
				    "may not be multi-threaded.",
				    AM_FAILURE);
    }
    PR_Unlock(lock);

    return;
}

/*
 * NOTE
 * ====
 * The ThreadFunction in the input parameter is deleted after it has
 * finished executing.
 */
bool
ThreadPool::dispatch(ThreadFunction *fObj) {
    bool retVal = true;
    if(maxThreads > 0 && exitNow == false) {
	try {
	    if(workQueue.size() > 0 &&
	       (activeThreads < maxThreads)) {
		createNewThread();
	    }
	} catch(const std::bad_alloc &ba) {
	    // Log error message
	    Log::log(logID, Log::LOG_ERROR,
		     "ThreadPool::dispatch(): Memory allocation problem.");
	    retVal = false;
	} catch(const NSPRException &nspre) {
	    // Log error message
	    Log::log(logID, Log::LOG_ERROR, nspre);
	    retVal = false;
	} catch(...) {
	    Log::log(logID, Log::LOG_ERROR, "ThreadPool::dispatch(): "
		     "Unknown error while dispatching work to the "
		     "thread pool.");
	    retVal = false;
	}

	if(retVal) {
	    PR_Lock(lock);
	    workQueue.push_back(fObj);
	    PR_NotifyCondVar(condVar);
	    PR_Unlock(lock);
	    Log::log(logID, Log::LOG_DEBUG,
		     "ThreadPool::dispatch(): Successfully dispatched the "
		     "work.");
	}
    } else {
	retVal = false;
    }

    return retVal;
}

ThreadPool::~ThreadPool() {
    PR_Lock(lock);
    maxThreads = 0;
    workQueue.resize(0);
    exitNow = true;
    PR_NotifyAllCondVar(condVar);
    PR_Unlock(lock);

    std::vector<PRThread *>::iterator iter;
    Log::log(logID, Log::LOG_MAX_DEBUG,
	     "ThreadPool::~ThreadPool(): "
	     "Number of threads to be joined: %d.",
	     threads.size());

    for(iter = threads.begin(); iter != threads.end(); ++iter) {
	if(*iter != NULL)
	    PR_JoinThread(*iter);
    }

    if(activeThreads != 0) {
	Log::log(logID, Log::LOG_ERROR,
		 "ThreadPool::~ThreadPool(): "
		 "Active thread count is not zero.");
    } else {
	Log::log(logID, Log::LOG_DEBUG,
		 "ThreadPool::~ThreadPool(): All Threads joined.");
    }

    PR_DestroyCondVar(condVar);
    condVar = NULL;
    PR_DestroyCondVar(threadStarted);
    threadStarted = NULL;
	PR_DestroyLock(lock);
    lock = NULL;
    Log::log(logID, Log::LOG_INFO,
	     "ThreadPool::~ThreadPool(): ThreadPool destroyed.");
}

#if defined(HPUX) || defined(LINUX)
void
spin(void *args) {
#else
void
::spin(void *args) {
#endif
    ThreadPool *ptr = (ThreadPool *)args;
    if(ptr == NULL)
	return;

    Log::log(ThreadPool::logID, Log::LOG_INFO,
	     "::spin() : New Thread entered loop. "
	     "Active threads = %u : Work queue : %d.", ptr->activeThreads,
	     ptr->workQueue.size());

    PR_Lock(ptr->lock);
    ptr->activeThreads++;
    Log::log(ThreadPool::logID, Log::LOG_INFO,"Setting ThreadStarted");
    PR_NotifyCondVar(ptr->threadStarted);   // Let the caller know we have started

    while(ptr->activeThreads <= ptr->maxThreads &&
	  ptr->exitNow == false) {
	if(ptr->workQueue.size() > 0) {
	    ThreadFunction *func = *(ptr->workQueue.begin());
	    ptr->workQueue.erase(ptr->workQueue.begin());
            Log::log(ThreadPool::logID, Log::LOG_INFO,"::Spin Unlocking...");
	    PR_Unlock(ptr->lock);
	    try {
		Log::log(ThreadPool::logID, Log::LOG_DEBUG,
			 "::spin() : Thread Function calling 0x%x.", func);
		if(func != NULL && ptr->exitNow == false)
		    (*func)();
	    } catch(...) {
		// Log any exception that occured during exection.
		Log::log(ThreadPool::logID, Log::LOG_ERROR,
			 "::spin() : Thread Function call threw an exception.");
	    }
            if (func != NULL) {
		Log::log(ThreadPool::logID, Log::LOG_DEBUG,
			 "::spin() : function 0x%x done, deleting. ", func);
                delete func;
            }

	    PR_Lock(ptr->lock);
	} else {
	    if(ptr->exitNow == false) {
                Log::log(ThreadPool::logID, Log::LOG_INFO,"::Spin Waiting...");
		PR_WaitCondVar(ptr->condVar, PR_INTERVAL_NO_TIMEOUT);
            };
	    Log::log(ThreadPool::logID, Log::LOG_DEBUG,
		     "spin() : Thread awakened: "
		     "activeThreads = %u ; maxThreads = %u ; "
		     "workQueueSize = %u", ptr->activeThreads,
		     ptr->maxThreads, ptr->workQueue.size());
	}
    }
    ptr->activeThreads--;
    PR_Unlock(ptr->lock);

    Log::log(ThreadPool::logID, Log::LOG_INFO,
	     "::spin() : Thread exiting loop. Current active threads = %u.",
	     ptr->activeThreads);
    return;
}
