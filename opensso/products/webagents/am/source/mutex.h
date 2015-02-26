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
 * Portions Copyrighted 2012 ForgeRock AS
 */

#ifndef MUTEX_H
#define MUTEX_H

#ifdef _MSC_VER
#include <windows.h>
#include <process.h>
#else
#include <pthread.h>
#include <errno.h>
#endif

#include "internal_macros.h"

BEGIN_PRIVATE_NAMESPACE

#ifdef _MSC_VER

class Mutex {
    CRITICAL_SECTION m;
    Mutex(const Mutex&);
    Mutex& operator=(const Mutex&);
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

#else

class Mutex {
    pthread_mutex_t m;
    pthread_mutexattr_t ma;
    Mutex(const Mutex&);
    Mutex& operator=(const Mutex&);
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

#endif

END_PRIVATE_NAMESPACE

#endif
