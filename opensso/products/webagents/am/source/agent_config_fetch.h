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
 * $Id: agent_config_fetch.h,v 1.4 2009/08/07 21:08:24 subbae Exp $
 *
 * Abstract:
 * AgentConfigFetch: Timer based class when invoked calls the function
 * fetchAndUpdateAgentConfigCache() to fetch the latest agent configuration
 * data.
 */

/*
 * Portions Copyrighted 2010-2013 ForgeRock Inc
 */

#ifndef __AGENT_CONFIG_FETCH_H__
#define __AGENT_CONFIG_FETCH_H__

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
        
class AgentConfigFetch:public ThreadFunction {
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

    AgentConfigFetch(AgentProfileService *agentProfileServiceParam,
                     unsigned long fetchInterval, const char *messStr)
            : ThreadFunction("AgentConfigFetch"), htcID(Log::addModule("Polling")),
              agentProfileService(agentProfileServiceParam),
              sleepTime(fetchInterval), stayAlive(true),
              doneExit(false), message(messStr) {
        mLock = new Mutex();
        condVar = new ConditionVariable();
        if (!mLock || !condVar) {
            throw std::bad_alloc();
        }
    }
    ~AgentConfigFetch() {
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
        am_status_t sts = AM_SUCCESS;
        unsigned long tps = 1000, sleepCount = 0, rollover = 0;       
        while(stayAlive) {           
            
            sleepCount = sleepTime/300;
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
                        "Starting %s. Fetching latest Agent Config "
                        "Properties", message);

                Log::log(htcID, Log::LOG_INFO,
                        "First validate agent(app) ssotoken");
                sts = agentProfileService->validateAgentSSOToken();
                if( sts == AM_INVALID_SESSION) {
                    Log::log(htcID, Log::LOG_INFO,
                        "validation of  agent(app) ssotoken failed. "
                        "Redo agent authentication before fetching agent profile");
                    agentProfileService->agentLogin();
                  
                }
                agentProfileService->fetchAndUpdateAgentConfigCache();                

                Log::log(htcID, Log::LOG_INFO,
                        "Finished %s. Fetching latest Agent Config " 
                        "Properties", message);
            }
        }
        doneExit = true;
    }       
};

END_PRIVATE_NAMESPACE
        
#endif	// not AGENT_CONFIG_FETCH_H
