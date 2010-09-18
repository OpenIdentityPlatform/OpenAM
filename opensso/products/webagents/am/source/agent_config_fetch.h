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
 * Portions Copyrighted [2010] [ForgeRock AS]
 */

#ifndef __AGENT_CONFIG_FETCH_H__
#define __AGENT_CONFIG_FETCH_H__

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
#include "agent_profile_service.h"
#include "am_web.h"

BEGIN_PRIVATE_NAMESPACE
        
using std::string;

class AgentConfigFetch:public ThreadFunction {
private:
    Log::ModuleId htcID;
    AgentProfileService *agentProfileService;
    volatile PRTime sleepTime;
    PRLock *lock;
    PRCondVar *condVar;
    volatile bool stayAlive;
    mutable volatile bool doneExit;
    const char *message;
    
public:
    /* Throws NSPRException upon NSPR error */
    AgentConfigFetch(AgentProfileService *agentProfileServiceParam,
                     PRTime fetchInterval, const char *messStr)
            : htcID(Log::addModule("Polling")),
              agentProfileService(agentProfileServiceParam),
              sleepTime(fetchInterval),
              lock(NULL), condVar(NULL), stayAlive(true),
              doneExit(false), message(messStr) {
        
        lock = PR_NewLock();
        if (lock == NULL) {
            throw NSPRException("AgentConfigFetch::AgentConfigFetch", 
                    "PR_NewLock", PR_GetError());
        } else {
            condVar = PR_NewCondVar(lock);
            if(condVar == NULL) {
                throw NSPRException("AgentConfigFetch::AgentConfigFetch", 
                        "PR_NewLock",
                        PR_GetError());
            }
        }
    }
    ~AgentConfigFetch() {
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
        am_status_t sts = AM_SUCCESS;
        PRTime tps = PR_TicksPerSecond(), sleepCount = 0, rollover = 0;       
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
            if (stayAlive)
                for(PRTime counter = 0; counter < sleepCount; counter ++)
                    if(stayAlive) PR_WaitCondVar(condVar, tps * 300 * 60);
            
            /* Wait for the reminder of the time */
            if(stayAlive)
                PR_WaitCondVar(condVar, tps * rollover * 60);
            
            PR_Unlock(lock);
            
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
        return;
    }       
};

END_PRIVATE_NAMESPACE
        
#endif	// not AGENT_CONFIG_FETCH_H
