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
 * $Id: p_cache.h,v 1.3 2008/06/25 08:14:34 qcheng Exp $
 *
 * Abstract:
 *
 * Post Cache functionality is maintained by this class. It
 * keeps a handle to the thread, hash table for POST Cache 
 *
*/

#ifndef __P_CACHE_H__
#define __P_CACHE_H__

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
#include "ht_cleaner.h"
#include "am_web.h"
#include "postcache_entry.h"

BEGIN_PRIVATE_NAMESPACE

class PostCache {

 private:
    // Log Id for POST cache, not used now
    Log::ModuleId logID;

    // Handle to agent properties 
    const Properties svcParams;  

    // Thread pool for POST preservation 
    ThreadPool postTPool;

    // Instance of Hash table cleaner 
    HTCleaner<PostCacheEntry> *postCleaner;
    
 public:

    /**
     * Post cache hashtable, made it public for quick access
     */
    HashTable<PostCacheEntry> postCacheTable;

    /**
     * Post cache handle constructor 
     * Throws: InternalException, std::bad_alloc and other std::exception's 
     *	       from thread pool and hashtable calls. 
     */
    PostCache(const Properties &);

    /**
     * Post cache handle destructor 
     */
    inline ~PostCache() {
        /* Thread pool will free postCleaner pointer when it has finished
         * executing. */
	postCleaner->stopCleaning();
    }

    /**
     * Insert an entry in POST cache hash table  
     * @param key: POST cache entry key
     * @param post_entry: POST cache entry data (Ref Counter type)
     * @return int: status if the insertion was true or false 
     * Throws: std::exception's from hashtable methods.
     */
    bool post_hash_insert(const char *key,
			  const PostCacheEntryRefCntPtr post_entry);

    /**
     * returns the value associated with a key. NULL if key is not present.
     * @param key: POST cache entry key
     * @return PostCacheEntryRefCntPtr: POST cache data Ref Ptr
     * Throws: std::exception's from hashtable methods.
     */
    inline PostCacheEntryRefCntPtr post_hash_get(const char *key) {
	PostCacheEntryRefCntPtr retVal;
	if(key != NULL){
	    retVal = postCacheTable.find(key);
	}

	return retVal;
    }

    /** 
     * delete an entry for the POST hash table 
     * @param key: POST entry key to be removed 
     * Throws: std::exception's from hashtable methods.
     */
    inline void post_hash_remove(const char *key) {
	if(key != NULL){
	    postCacheTable.remove(key);
	}
    }

    /**
     * Calls Hash table cleanup
     * Throws: std::exception's from hashtable methods.
     */
    inline void post_hash_cleanup(){
	    postCacheTable.cleanup();
    }


};

END_PRIVATE_NAMESPACE

#endif	// not __P_CACHE_H__
