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
 * $Id: agent_config_cache.h,v 1.3 2008/06/25 08:14:22 qcheng Exp $
 */
/*
 * Portions Copyrighted 2013 ForgeRock Inc
 */

#ifndef __AGENT_CONFIG_CACHE_H__
#define __AGENT_CONFIG_CACHE_H__

#include <stdexcept>
#include <string>
#include <time.h>
#include "hash_table.h"
#include "http.h"
#include "internal_exception.h"
#include "nspr_exception.h"
#include "internal_macros.h"
#include "mutex.h"
#include "am_web.h"
#include "agent_configuration.h"
#include "mutex.h"
#include "scope_lock.h"

BEGIN_PRIVATE_NAMESPACE

class AgentConfigCache {
private:
    Log::ModuleId logID;
    mutable Mutex lock;

    /**
     * Agent config cache hashtable, made it public for quick access
     */
    HashTable<AgentConfiguration> agentConfigCacheTable;

    /* Latest Agent Config instance key */
    std::string latestConfigKey;

    /* Updates the latest Agent Config instance key 
     * Need to have the scope lock to here as we are
     * updating the member variable
     */
    void setLatestAgentConfigKey() {
        ScopeLock myLock(lock);
        char hdr[64];

#ifdef _MSC_VER

        SYSTEMTIME lt;
        GetLocalTime(&lt);
        _snprintf(hdr, sizeof (hdr), "%d-%02d-%02d-%02d:%02d:%02d.%03d", lt.wYear, lt.wMonth, lt.wDay,
                lt.wHour, lt.wMinute, lt.wSecond, lt.wMilliseconds);

#else 

        struct tm now;
        struct timespec ts;
        unsigned short msec = 0;
        clock_gettime(CLOCK_REALTIME, &ts);
        msec = ts.tv_nsec / 1000000;
        localtime_r(&ts.tv_sec, &now);
        snprintf(hdr, sizeof (hdr),
                "%d-%02d-%02d-%02d:%02d:%02d.%03d",
                now.tm_year + 1900, now.tm_mon + 1, now.tm_mday,
                now.tm_hour, now.tm_min, now.tm_sec,
                msec);

#endif

        latestConfigKey = hdr;
    }

    friend class AgentProfileService;

public:
    /**
     * Agent Config cache constructor 
     */
    AgentConfigCache();

    /**
     * Agent Config destructor
     */
    ~AgentConfigCache();

    /**
     * Insert an entry in Agent config cache hash table      
     * @param agentConfigEntry: Agent config cache entry data (Ref Counter type)
     * @return bool: status if the insertion was true or false 
     * Throws: std::exception's from hashtable methods.
     */
    bool populateAgentConfigCacheTable(
            const AgentConfigurationRefCntPtr agentConfigEntry);

    /**
     * Returns the latest agent config instance key
     * Throws: InternalException, std::bad_alloc and other std::exception's 
     *	       from thread pool and hashtable calls. 
     */
    std::string getLatestAgentConfigKey() {
        return latestConfigKey;
    }

    /**
     * Returns the latest agent configuration instance from the hash table
     * @return AgentConfigurationRefCntPtr: Agent Config cache data Ref Ptr
     * Throws: std::exception's from hashtable methods.
     */
    AgentConfigurationRefCntPtr getLatestAgentConfigInstance();

    /**
     * Deletes all the old Agent config instances.  
     * Throws: std::exception's from hashtable methods.
     */
    void deleteOldAgentConfigInstances();
};

END_PRIVATE_NAMESPACE

#endif
