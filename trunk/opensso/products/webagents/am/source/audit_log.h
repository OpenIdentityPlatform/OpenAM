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
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 *
 * Abstract:
 * AuditLog: Timer based class when invoked calls the function
 * flushBuffer() to flush the log records from buffer 
 * data.
 */

/*
 * Portions Copyrighted 2010-2012 ForgeRock AS
 */

#ifndef __AUDIT_LOG_H__
#define __AUDIT_LOG_H__

#include <stdexcept>
#include <string>

#include "internal_exception.h"
#include "nspr_exception.h"
#include "internal_macros.h"
#include "mutex.h"
#include "thread_pool.h"
#include "thread_function.h"
#include "log_service.h"

BEGIN_PRIVATE_NAMESPACE

using std::string;

class AuditLog : public ThreadFunction {
private:
    Log::ModuleId htcID;
    LogService *logService;
    AgentProfileService *agentProfileService;
    volatile PRTime sleepTime;
    PRLock *lock;
    PRCondVar *condVar;
    volatile bool stayAlive;
    mutable volatile bool doneExit;
    const char *message;

public:

    /* Throws NSPRException upon NSPR error */
    AuditLog(LogService *logServiceParam,
            AgentProfileService *agentProfileServiceParam,
            PRTime fetchInterval, const char *messStr)
    : htcID(Log::addModule("Polling")),
    logService(logServiceParam),
    agentProfileService(agentProfileServiceParam), sleepTime(fetchInterval),
    lock(NULL), condVar(NULL), stayAlive(true), doneExit(false), message(messStr) {

        lock = PR_NewLock();
        if (lock == NULL) {
            throw NSPRException("AuditLog::AuditLog",
                    "PR_NewLock", PR_GetError());
        } else {
            condVar = PR_NewCondVar(lock);
            if (condVar == NULL) {
                throw NSPRException("AuditLog::AuditLog",
                        "PR_NewLock",
                        PR_GetError());
            }
        }
    }

    ~AuditLog() {
        PR_DestroyCondVar(condVar);
        condVar = NULL;
        PR_DestroyLock(lock);
        lock = NULL;
        message = NULL;
    }

    inline void stopFlushing() {
        stayAlive = false;
        PR_Lock(lock);
        PR_NotifyAllCondVar(condVar);
        PR_Unlock(lock);
    }

    void operator()() const {
        PRTime tps = PR_TicksPerSecond(), sleepCount = 0, rollover = 0;
        while (stayAlive) {
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
            sleepCount = sleepTime / 300;
            rollover = sleepTime % 300;

            PR_Lock(lock);
            if (stayAlive)
                for (PRTime counter = 0; counter < sleepCount; counter++)
                    if (stayAlive) PR_WaitCondVar(condVar, tps * 300 * 60);

            /* Wait for the reminder of the time */
            if (stayAlive)
                PR_WaitCondVar(condVar, tps * rollover * 60);

            PR_Unlock(lock);

            if (stayAlive) {

                if (agentProfileService->getAgentConfigInstance()->doRemoteLog == AM_TRUE) {
                    Log::log(htcID, Log::LOG_INFO, "Starting %s. logService->flushBuffer()", message);
                    if (logService != NULL) {
                        logService->flushBuffer();
                    } else {
                        Log::log(htcID, Log::LOG_ERROR, "LogService::flushBuffer() logService is NULL");
                    }
                    Log::log(htcID, Log::LOG_INFO, "Finished %s. logService->flushBuffer()", message);
                }
            }
        }
        doneExit = true;
        return;
    }

    void updateLogService(LogService *newLogService) {
        LogService *oldRemoteLogInfo = logService;
        logService = newLogService;
    }
};

END_PRIVATE_NAMESPACE

#endif	// not AUDIT_LOG_H
