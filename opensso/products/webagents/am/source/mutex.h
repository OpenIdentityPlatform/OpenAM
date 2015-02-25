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
 * $Id: mutex.h,v 1.3 2008/06/25 08:14:33 qcheng Exp $
 *
 * Abstract:
 *
 * Mutual exclusion synchronization object.
 *
 */
/*
 * Portions Copyrighted 2012-2013 ForgeRock Inc
 */

#ifndef MUTEX_H
#define MUTEX_H

#ifdef _MSC_VER
#define WIN32_LEAN_AND_MEAN
#include <windows.h>
#include <process.h>
#else
#include <pthread.h>
#include <time.h>
#include <errno.h>
#endif

#include "internal_macros.h"

BEGIN_PRIVATE_NAMESPACE

#ifdef _MSC_VER

class ConditionVariable;

class Mutex {
    CRITICAL_SECTION m;
    Mutex(const Mutex&);
    Mutex& operator=(const Mutex&);
    friend class ConditionVariable;
public:

    Mutex() {
        InitializeCriticalSection(&m);
    }

    ~Mutex() {
        DeleteCriticalSection(&m);
    }

    bool lock() {
        EnterCriticalSection(&m);
        return true;
    }

    bool unlock() {
        LeaveCriticalSection(&m);
        return true;
    }

    bool trylock() {
        return TryEnterCriticalSection(&m) != FALSE;
    }

};

class ConditionVariable {
    DWORD c;
    CRITICAL_SECTION d;

    enum {
        SIGNAL = 0,
        BROADCAST = 1,
        MAX = 2
    };
    HANDLE e[MAX];
    ConditionVariable(const ConditionVariable&);
    ConditionVariable& operator=(const ConditionVariable&);

public:

    ConditionVariable() {
        InitializeCriticalSection(&d);
        e[SIGNAL] = CreateEvent(NULL, FALSE, FALSE, NULL);
        e[BROADCAST] = CreateEvent(NULL, TRUE, FALSE, NULL);
        c = 0;
    }

    ~ConditionVariable() {
        ResetEvent(e[BROADCAST]);
        CloseHandle(e[SIGNAL]);
        CloseHandle(e[BROADCAST]);
        DeleteCriticalSection(&d);
    }

    void signal() {
        EnterCriticalSection(&d);
        BOOL w = c > 0;
        LeaveCriticalSection(&d);
        if (w)
            SetEvent(e[SIGNAL]);
    }

    void signalAll() {
        EnterCriticalSection(&d);
        BOOL w = c > 0;
        LeaveCriticalSection(&d);
        if (w)
            SetEvent(e[BROADCAST]);
    }

    void wait(Mutex &m, long msec = INFINITE) {
        EnterCriticalSection(&d);
        c++;
        LeaveCriticalSection(&d);
        LeaveCriticalSection(&(m.m));
        DWORD r = WaitForMultipleObjects(2, e, FALSE, msec);
        EnterCriticalSection(&d);
        c--;
        BOOL l = r == WAIT_OBJECT_0 + BROADCAST && c == 0;
        LeaveCriticalSection(&d);
        if (l)
            ResetEvent(e[BROADCAST]);
        EnterCriticalSection(&(m.m));
    }
};

/*
class ConditionVariable {
    CONDITION_VARIABLE v;
    ConditionVariable(const ConditionVariable&);
    ConditionVariable& operator=(const ConditionVariable&);

public:

    ConditionVariable() {
        InitializeConditionVariable(&v);
    }

    ~ConditionVariable() {
    }

    void signal() {
        WakeConditionVariable(&v);
    }

    void signalAll() {
        WakeAllConditionVariable(&v);
    }

    void wait(Mutex &m, long msec = INFINITE) {
        SleepConditionVariableCS(&v, &(m.m), msec);
    }

};
*/

class Thread {
    HANDLE handle;
    DWORD tid;
    int running;

public:

    Thread() : running(0), tid(0) {
    }

    virtual ~Thread() {
        if (running == 1) {
            CloseHandle(handle);
        }
    }

    static void* proc(void* arg) {
        return ((Thread*) arg)->run();
    }

    int start() {
        handle = CreateThread(NULL, 0, (LPTHREAD_START_ROUTINE) proc, this, 0, &tid);
        if (handle != NULL) {
            running = 1;
        }
        return running;
    }

    int join() {
        int result = -1;
        if (running == 1) {
            WaitForSingleObject(handle, INFINITE);
            result = 0;
            running = 0;
        }
        return result;
    }

    unsigned long id() {
        return tid;
    }

    virtual void* run() = 0;

};

#else

class Mutex {
    pthread_mutex_t m;
    pthread_mutexattr_t ma;
    Mutex(const Mutex&);
    Mutex& operator=(const Mutex&);
    friend class ConditionVariable;
public:

    Mutex() {
        pthread_mutexattr_init(&ma);
        pthread_mutexattr_settype(&ma, PTHREAD_MUTEX_ERRORCHECK);
        pthread_mutex_init(&m, &ma);
    }

    ~Mutex() {
        pthread_mutexattr_destroy(&ma);
        pthread_mutex_destroy(&m);
    }

    bool lock() {
        int res;
        res = pthread_mutex_lock(&m);
        if (res == 0) return true;
        return false;
    }

    bool unlock() {
        int res;
        res = pthread_mutex_unlock(&m);
        if (res == 0) return true;
        return false;
    }

    bool trylock() {
        int res;
        res = pthread_mutex_trylock(&m);
        if (res && (res != EBUSY)) return false;
        return !res;
    }

};

class ConditionVariable {
    pthread_cond_t v;
    ConditionVariable(const ConditionVariable&);
    ConditionVariable& operator=(const ConditionVariable&);

public:

    ConditionVariable() {
        pthread_cond_init(&v, NULL);
    }

    ~ConditionVariable() {
        pthread_cond_destroy(&v);
    }

    void signal() {
        pthread_cond_signal(&v);
    }

    void signalAll() {
        pthread_cond_broadcast(&v);
    }

    void wait(Mutex &m) {
        pthread_cond_wait(&v, &(m.m));
    }

    void wait(Mutex &m, long milliseconds) {
        struct timespec now, ts;
        clock_gettime(CLOCK_REALTIME, &now);
        ts.tv_sec = now.tv_sec + milliseconds / 1000;
        ts.tv_nsec = now.tv_nsec + (milliseconds % 1000) * 1000000;
        if (ts.tv_nsec >= 1000000000) {
            ts.tv_nsec -= 1000000000;
            ++ts.tv_sec;
        }
        pthread_cond_timedwait(&v, &(m.m), &ts);
    }
};

extern "C" void* ThreadProc(void *);

class Thread {
    pthread_t handle;
    int running;

public:

    Thread() : running(0) {
    }

    virtual ~Thread() {
    }

    int start() {
        int nerr = pthread_create(&handle, NULL, ThreadProc, static_cast<void*> (this));
        if (!nerr) {
            running = 1;
        }
        return running;
    }

    int join() {
        int result = -1;
        if (running == 1) {
            result = pthread_join(handle, NULL);
            running = 0;
        }
        return result;
    }

    pthread_t id() {
        return pthread_self();
    }

    virtual void* run() = 0;

};

#endif

END_PRIVATE_NAMESPACE

#endif
