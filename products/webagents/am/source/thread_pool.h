/* The contents of this file are subject to the terms
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
 * Copyright 2006 Sun Microsystems Inc. All Rights Reserved
 *
 */ 
#ifndef __THREAD_POOL_H__
#define __THREAD_POOL_H__

#include <vector>
#include <prthread.h>
#include <prcvar.h>
#include "internal_macros.h"
#include "internal_exception.h"
#include "nspr_exception.h"
#include "log.h"
#include "thread_function.h"

void spin(void *);

BEGIN_PRIVATE_NAMESPACE

class ThreadPool {
 public:
    // throws std::bad_alloc, NSPRException
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

    // private static variables
    PRLock *lock;
    volatile bool exitNow;
    PRCondVar *condVar;
    std::vector<ThreadFunction*> workQueue;
    std::size_t maxThreads;
    std::vector<PRThread *> threads;
    volatile std::size_t activeThreads;
    static Log::ModuleId logID;
    friend class ThreadObject;
    friend void ::spin(void *);

    // Non exposed functions.
    ThreadPool(const ThreadPool &);
    ThreadPool();
    ThreadPool& operator=(const ThreadPool &) const;


    
    // Private functions.
    // throws std::bad_alloc, NSPRException, InternalException
    void createNewThread();
    void init(std::size_t, std::size_t);
};
END_PRIVATE_NAMESPACE
#endif
