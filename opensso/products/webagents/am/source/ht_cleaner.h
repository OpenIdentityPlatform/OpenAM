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
 * $Id: ht_cleaner.h,v 1.4 2008/09/13 01:11:53 robertis Exp $
 *
 * Abstract:
 *
 * Abstract The Hashtable cleaner thread 
 *
 */
/*
 * Portions Copyrighted 2013 ForgeRock Inc
 */

#ifndef __HT_CLEANER_H__
#define __HT_CLEANER_H__

#include <stdexcept>
#include "hash_table.h"
#include "http.h"
#include "internal_exception.h"
#include "internal_macros.h"
#include "mutex.h"
#include "thread_pool.h"
#include "thread_function.h"

BEGIN_PRIVATE_NAMESPACE

template<typename T>
class HTCleaner : public ThreadFunction {
private:
    Log::ModuleId htcID;
    HashTable<T> *ht;
    volatile unsigned long sleepTime;
    Mutex *mLock;
    ConditionVariable *condVar;
    volatile bool stayAlive;
    mutable volatile bool doneExit;
    const char *message;

public:

    HTCleaner(HashTable<T> *tbl,
            unsigned long cleanupInterval, const char *messStr)
    : ThreadFunction("HTCleaner"), htcID(Log::addModule("Polling")),
    ht(tbl), sleepTime(cleanupInterval), stayAlive(true),
    doneExit(false), message(messStr) {
        mLock = new Mutex();
        condVar = new ConditionVariable();
        if (!mLock || !condVar) {
            throw std::bad_alloc();
        }
    }

    ~HTCleaner() {
        delete mLock;
        mLock = NULL;
        delete condVar;
        condVar = NULL;
    }

    inline void stopCleaning() {
        stayAlive = false;
        mLock->lock();
        condVar->signalAll();
        mLock->unlock();
    }

    void operator()() const {
        unsigned long tps = 1000, sleepCount = 0, rollover = 0;
        while (stayAlive) {

            sleepCount = sleepTime / 300;
            rollover = sleepTime % 300;

            mLock->lock();
            if (stayAlive)
                for (unsigned long counter = 0; counter < sleepCount; counter++)
                    if (stayAlive) condVar->wait(*mLock, tps * 300 * 60);

            if (stayAlive)
                condVar->wait(*mLock, tps * rollover * 60);

            mLock->unlock();

            if (stayAlive) {
                Log::log(htcID, Log::LOG_INFO,
                        "Starting %s. Hash table size: %u", message, ht->size());
                ht->cleanup();
                Log::log(htcID, Log::LOG_INFO,
                        "Finished %s. Hash table size: %u", message, ht->size());
            }
        }
        doneExit = true;
    }

};

END_PRIVATE_NAMESPACE

#endif	// not HT_CLEANER_H
