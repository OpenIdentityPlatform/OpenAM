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
 * $Id: postcache_entry.h,v 1.3 2008/06/25 08:14:35 qcheng Exp $
 *
 * Abstract: 
 *
 * Each entry in the POST preservation table is comprised
 * of an instance of PostCacheEntry
 */

#ifndef __POSTCACHE_ENTRY_H__
#define __POSTCACHE_ENTRY_H__
#include <stdexcept>
#include "internal_exception.h"
#include "ref_cnt_ptr.h"

BEGIN_PRIVATE_NAMESPACE

class PostCacheEntry:public RefCntObj {
 private:

     // POST data in an individual entry (encoded)
     std::string postData;

     // Destination URL for the POST request
     std::string destUrl;

 public:

    /**
      * Constructor for post cache entry 
      * @param postvalue: POST cache entry value 
      * @param desturl: POST destination URL
    */
    inline PostCacheEntry(const char *postvalue,
			  const char *desturl) : RefCntObj(),
						 postData(postvalue),
						 destUrl(desturl) {
    }

    /**
      * Destructor for post cache entry 
    */
    inline ~PostCacheEntry() {
    }

    /**
      * Get the post cache data in an entry
      * return const char* : POST data value
    */
    inline const char *getPostData() {
	    return postData.c_str();
    }

    /**
      * Get the destination url in an entry
      * return const char* : POST destination URL
    */
    inline const char *getDestUrl() {
	    return destUrl.c_str();
    }
};

typedef RefCntPtr<PostCacheEntry> PostCacheEntryRefCntPtr;

END_PRIVATE_NAMESPACE
#endif
