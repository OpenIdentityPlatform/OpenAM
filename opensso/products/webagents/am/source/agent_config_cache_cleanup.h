/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: agent_config_cache_cleanup.h,v 1.3 2008/06/25 08:14:22 qcheng Exp $
 *
 * Abstract:
 * AgentConfigCleanup: Timer based class when invoked calls the function
 * deleteOldAgentConfigInstances() to delete any old agent configuration
 * instances
 */
/*
 * Portions Copyrighted 2010-2013 ForgeRock Inc
 */

#ifndef __AGENT_CONFIG_CACHE_CLEANUP_H__
#define __AGENT_CONFIG_CACHE_CLEANUP_H__

#include <stdexcept>
#include "hash_table.h"
#include "http.h"
#include "internal_exception.h"
#include "internal_macros.h"
#include "mutex.h"
#include "thread_pool.h"
#include "thread_function.h"
#include "agent_profile_service.h"
#include "am_web.h"

BEGIN_PRIVATE_NAMESPACE

class AgentConfigCacheCleanup : public ThreadFunction {
private:
    Log::ModuleId htcID;
    AgentProfileService *agentProfileService;
    volatile unsigned long sleepTime;
    Mutex *mLock;
    ConditionVariable *condVar;
    volatile bool stayAlive;
    mutable volatile bool doneExit;
    const char *message;

public:

    AgentConfigCacheCleanup(AgentProfileService *agentProfileServiceParam,
            unsigned long cleanupInterval, const char *messStr)
    : ThreadFunction("AgentConfigCacheCleanup"), htcID(Log::addModule("Polling")),
    agentProfileService(agentProfileServiceParam),
    sleepTime(cleanupInterval), stayAlive(true),
    doneExit(false), message(messStr) {
        mLock = new Mutex();
        condVar = new ConditionVariable();
        if (!mLock || !condVar) {
            throw std::bad_alloc();
        }
    }

    ~AgentConfigCacheCleanup() {
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
                        "Starting %s. Deleting old Agent Config "
                        "instances", message);

                agentProfileService->deleteOldAgentConfigInstances();

                Log::log(htcID, Log::LOG_INFO,
                        "Finished %s. Deleting old Agent Config "
                        "instances", message);
            }
        }
        doneExit = true;
    }
};

END_PRIVATE_NAMESPACE

#endif	// not AGENT_CONFIG_FETCH_H
