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
 * Portions Copyrighted 2010-2013 ForgeRock Inc
 */

#ifndef __AUDIT_LOG_H__
#define __AUDIT_LOG_H__

#include <stdexcept>
#include "internal_exception.h"
#include "internal_macros.h"
#include "mutex.h"
#include "thread_pool.h"
#include "thread_function.h"
#include "log_service.h"

BEGIN_PRIVATE_NAMESPACE

class AuditLog : public ThreadFunction {
private:
    Log::ModuleId htcID;
    LogService *logService;
    AgentProfileService *agentProfileService;
    volatile unsigned long sleepTime;
    Mutex *mLock;
    ConditionVariable *condVar;
    volatile bool stayAlive;
    mutable volatile bool doneExit;
    const char *message;


public:

    AuditLog(LogService *logServiceParam,
            AgentProfileService *agentProfileServiceParam,
            unsigned long fetchInterval, const char *messStr)
    : ThreadFunction("AuditLog"), htcID(Log::addModule("Polling")),
    logService(logServiceParam),
    agentProfileService(agentProfileServiceParam), sleepTime(fetchInterval),
    stayAlive(true), doneExit(false), message(messStr) {
        mLock = new Mutex();
        condVar = new ConditionVariable();
        if (!mLock || !condVar) {
            throw std::bad_alloc();
        }
    }

    ~AuditLog() {
        delete mLock;
        mLock = NULL;
        delete condVar;
        condVar = NULL;
    }

    inline void stopFlushing() {
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
                am_status_t status = AM_FAILURE;
                AgentConfigurationRefCntPtr instance = agentProfileService->getAgentConfigInstance(status);
                if (instance && status == AM_SUCCESS && instance->doRemoteLog == AM_TRUE) {
                    Log::log(htcID, Log::LOG_INFO, "Starting %s.", message);
                    if (logService != NULL) {
                        logService->flushBuffer();
                    } else {
                        Log::log(htcID, Log::LOG_ERROR, "LogService::flushBuffer() logService is NULL");
                    }
                    Log::log(htcID, Log::LOG_INFO, "Finished %s.", message);
                }
            }
        }
        doneExit = true;
    }

    void updateLogService(LogService *newLogService) {
        LogService *oldRemoteLogInfo = logService;
        logService = newLogService;
    }
};

END_PRIVATE_NAMESPACE

#endif	// not AUDIT_LOG_H
