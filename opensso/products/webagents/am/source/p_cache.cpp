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
 * $Id: p_cache.cpp,v 1.4 2008/06/25 08:14:34 qcheng Exp $
 *
 * Abstract:
 *
 * Post Cache functionality is maintained by this class
 * 
 */
/*
 * Portions Copyrighted 2013 ForgeRock Inc
 */

#include "am_web.h"
#include "am_policy.h"
#include "p_cache.h"

#define POST_CACHE "PostCache"

USING_PRIVATE_NAMESPACE

using std::invalid_argument;

namespace {
    // Default Hash table for POST data 
    const unsigned long DEFAULT_HASH_SIZE = 31;

    // Default time out in minutes for POST data entry
    const unsigned long DEFAULT_TIMEOUT = 10;

    // Default Max number of threads for thread pool
    const std::size_t DEFAULT_MAX_THREADS = 10;
}

/**
 * Post cache thread constructor, creates threadpool, hashtable and 
 * post hashtable cleaner
 *
 * Throws: InternalException, std::bad_alloc and 
 *         other std::exception's from thread pool and hashtable calls.
 */
PostCache::PostCache(const Properties &initParams)
    : logID(Log::addModule(POST_CACHE)), 
    svcParams(initParams, logID),
    postTPool(1, DEFAULT_MAX_THREADS),
    postCleaner(NULL),
    postCacheTable(DEFAULT_HASH_SIZE,
		   svcParams.getPositiveNumber(AM_WEB_POST_CACHE_ENTRY_LIFETIME,
					 DEFAULT_TIMEOUT)) {

    try {
        postCleaner = new HTCleaner<PostCacheEntry>(
                          &postCacheTable,
		          svcParams.getPositiveNumber(
                              AM_WEB_POST_CACHE_ENTRY_LIFETIME,
			      DEFAULT_TIMEOUT),
		          "POST cache cleanup");
    }
    catch(std::bad_alloc &bae) {
	throw InternalException("post cache", 
				"Memory allocation failure while "
				"creating hash table cleaner.",
				AM_NO_MEMORY);
    } 
    catch (...) {
	throw InternalException("post cache", 
				"Unknown exception when creating "
				"hash table cleaner",
				AM_FAILURE);
    }

    /* Adding Post cleanup thread for the Post Cache hash table */
    /* Thread pool will free the pointer when it has finished executing. */
    bool retVal = postTPool.dispatch(postCleaner);

    if(retVal == true){
	Log::log(logID, Log::LOG_INFO, "POST cache thread started");
    }else {
	std::string msg("Failed to start POST cache thread");
	Log::log(logID, Log::LOG_INFO, msg.c_str());
	throw InternalException("PostCache::PostCache", msg, AM_INIT_FAILURE);
    }
}

/**
  * inserts a key-value pair into the hash table. If an existing key is passed
  * the value is overwritten with the new value.
  * @param key : POST data insert key
  * @param value : POST data entry value
  * @return bool : Status whether insertion failed or not 
  *
  * Throws: std::exception from hashtable manipulations.
*/
bool PostCache::post_hash_insert(const char *key, 
				 const PostCacheEntryRefCntPtr value) 
{
    if(key != NULL){
	if(value != NULL){
	    postCacheTable.insert(key,value);
	    return true;
	} else {
	    Log::log(logID, Log::LOG_INFO, "Failed to insert entry in POST cache table : Invalid Data");
	    return false;
	}
    } else { 
	Log::log(logID, Log::LOG_INFO, "Failed to insert entry in POST cache table : Invalid key");
	return false;
    }

    return false;
}

