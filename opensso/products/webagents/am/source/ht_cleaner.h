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

#ifndef __HT_CLEANER_H__
#define __HT_CLEANER_H__

#include <stdexcept>
#include <string>


#include "hash_table.h"
#include "http.h"
#include "internal_exception.h"
#include "nspr_exception.h"
#include "internal_macros.h"
#include "mutex.h"
#include "thread_pool.h"
#include "thread_function.h"

BEGIN_PRIVATE_NAMESPACE

using std::string;

template<typename T>
class HTCleaner :public ThreadFunction {
    private:
	Log::ModuleId htcID;
	HashTable<T> *ht;
        volatile PRTime sleepTime;
	PRLock *lock;
	PRCondVar *condVar;
	volatile bool stayAlive;
	mutable volatile bool doneExit;
	const char *message;
	
    public:
	/* Throws NSPRException upon NSPR error */
	HTCleaner(HashTable<T> *tbl,
		  PRTime cleanupInterval,const char *messStr)
	  : htcID(Log::addModule("Polling")),
	    ht(tbl), sleepTime(cleanupInterval),
	    lock(NULL), condVar(NULL), stayAlive(true), 
	    doneExit(false), message(messStr){
	    
	    lock = PR_NewLock();
	    if(lock == NULL) {
		throw NSPRException("HTCleaner::HTCleaner", "PR_NewLock",
				    PR_GetError());
	    } else 
	    
	    {
		condVar = PR_NewCondVar(lock);
		if(condVar == NULL) {
		    throw NSPRException("HTCleaner::HTCleaner", "PR_NewLock",
					PR_GetError());
		}
	    }
	}
	~HTCleaner() {
	    PR_DestroyCondVar(condVar);
	    condVar = NULL;
	    PR_DestroyLock(lock);
	    lock = NULL;
	    message = NULL;
	}
	
	inline void stopCleaning() {
	    stayAlive = false;
	    PR_Lock(lock);
	    PR_NotifyAllCondVar(condVar);
	    PR_Unlock(lock);
	}


void operator()() const {
    PRTime sleepCount = 0, rollover = 0;
#if defined(_AMD64_)
    PRIntervalTime tps = PR_TicksPerSecond();
#else
    PRTime tps = PR_TicksPerSecond();
#endif

    while(stayAlive) {

	/**
	 * NSPR documentation suggests that we
	 * don't give sleep timers that are more
	 * than 6 hours.  On some OS, the PRTime
	 * might roll over. Just to be safe we take
	 * five hour intervals.
	 * sleepCount is the # of 5 hour chunks
	 * rollover is the mins to sleep that is
	 * the remainder of time.
	 * NOTE: We calculate the timer value
	 * everytime bcoz, we can dynamically change
	 * it if needed using the set timer
	 */
	sleepCount = sleepTime/300;
	rollover = sleepTime % 300;

	/**
	 * First thing to do when we get in here.
	 * Take a good rest.  Long work ahead!.
	 */
	PR_Lock(lock);

	if(stayAlive)
	    for(PRTime counter = 0; counter < sleepCount; counter ++)
		if(stayAlive) PR_WaitCondVar(condVar, tps * 300 * 60);

	/* Wait for the reminder of the time */
	if(stayAlive)
	    PR_WaitCondVar(condVar, tps * rollover * 60);

	PR_Unlock(lock);

	if(stayAlive) {
	    Log::log(htcID, Log::LOG_INFO,
		     "Starting %s. Hash table size=%u.",message, ht->size());
	    ht->cleanup();
	    Log::log(htcID, Log::LOG_INFO,
		     "Finished %s. Hash table size=%u.",message, ht->size());
	}
    }
    doneExit = true;
    return;
}


};

END_PRIVATE_NAMESPACE

#endif	// not HT_CLEANER_H
