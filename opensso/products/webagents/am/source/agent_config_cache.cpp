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
 * $Id: agent_config_cache.cpp,v 1.3 2008/06/25 08:14:22 qcheng Exp $
 *
 * Abstract:
 * Agent Config Cache: This class creates/updates a Hash table which 
 * containes the agent configuration instances.
 */
/*
 * Portions Copyrighted 2013 ForgeRock Inc
 */

#include "am_web.h"
#include "agent_config_cache.h"
#include "agent_configuration.h"

#define AGENT_CONFIG_CACHE "AgentConfigCache"

USING_PRIVATE_NAMESPACE

using std::invalid_argument;

namespace {
    // Default Hash table for Agent config cache data 
    const unsigned long DEFAULT_HASH_SIZE = 31;

    // Default time out in minutes for Agent config data entry
    const unsigned long DEFAULT_TIMEOUT = 3;
}

/**
 * Agent Config Cache constructor, 
 *
 */
AgentConfigCache::AgentConfigCache()
    : logID(Log::addModule(AGENT_CONFIG_CACHE)),
    lock(),
    latestConfigKey(),
    agentConfigCacheTable(DEFAULT_HASH_SIZE, DEFAULT_TIMEOUT)
{
}


AgentConfigCache::~AgentConfigCache()
{
}

/**
  * inserts a key-value pair into the hash table. 
  * @param value : Agent Configuration entry value
  * @return bool : Status whether insertion failed or not 
  *
  * Throws: std::exception from hashtable manipulations.
*/
bool AgentConfigCache::populateAgentConfigCacheTable(
				 const AgentConfigurationRefCntPtr value) 
{
    bool status = false;
    
    // Update the latest key
    setLatestAgentConfigKey();
    
    // Retrieve the new key
    std::string latestConfKey = getLatestAgentConfigKey();
    
    // insert the new Agent configuration object
    if (!latestConfKey.empty()) {
        agentConfigCacheTable.insert(latestConfKey,value);
        Log::log(logID, Log::LOG_INFO,
            "SIZE of Agent Config Hash table, size=%u.",
            agentConfigCacheTable.size());
        status =  true;
    } else {
        Log::log(logID, Log::LOG_INFO, 
                "Failed to insert entry in Agent Config Cache table : "
                "Invalid Data");
        status = false;
    }    
    return status;
}

/**
  * returns the value associated with a key. NULL if value is not present.
  * @return AgentConfigurationRefCntPtr: Agent Config cache data Ref Ptr
  * Throws: std::exception's from hashtable methods.
*/
AgentConfigurationRefCntPtr AgentConfigCache::getLatestAgentConfigInstance() 
{
     return agentConfigCacheTable.find_cac(getLatestAgentConfigKey());
}

/**
  * Deletes all the old Agent config instances.  
  * Throws: std::exception's from hashtable methods.
*/
void AgentConfigCache::deleteOldAgentConfigInstances() 
{
    Log::log(logID, Log::LOG_INFO,
            "Starting to clean Agent Config Hash table, size: %u",
            agentConfigCacheTable.size());
    
    agentConfigCacheTable.cleanup_cac(getLatestAgentConfigKey());
    
    Log::log(logID, Log::LOG_INFO,
            "Finished cleaning Agent Config Hash table size: %u",
            agentConfigCacheTable.size());
}
