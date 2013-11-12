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
 * $Id: sso_token_listener_entry.h,v 1.4 2008/09/13 01:11:53 robertis Exp $
 *
 */ 
/*
 * Portions Copyrighted 2013 ForgeRock Inc
 */

#ifndef __SSO_TOKEN_LISTENER_ENTRY_H__
#define __SSO_TOKEN_LISTENER_ENTRY_H__
#include <stdexcept>
#include <am_sso.h>
#include "internal_exception.h"
#include "sso_token.h"
#include "ref_cnt_ptr.h"
#include "mutex.h"
#include "scope_lock.h"
#include "session_info.h"
#include "sso_token_listener_thrfunc.h"

BEGIN_PRIVATE_NAMESPACE

class SSOTokenListenerEntry:public RefCntObj {
private:
    std::list<SSOTokenListenerThreadFunc *> mListeners;
    Mutex mLock;

public:
    friend class SSOTokenService;

    virtual ~SSOTokenListenerEntry() 
    { }

    SSOTokenListenerEntry(am_sso_token_listener_func_t listener,
                          void *args,
			  bool dispatchInSepThread) 
        : mListeners(), mLock()
    { 
        SSOTokenListenerThreadFunc *tf = 
            new SSOTokenListenerThreadFunc(listener, 
					   args,
					   dispatchInSepThread);
        mListeners.push_back(tf);
    }

    void addListener(am_sso_token_listener_func_t listener,
                     void *args,
		     bool dispatchInSepThread) 
    {
	// no need to lock here since caller should already have 
	// exclusive access to the entry.
        SSOTokenListenerThreadFunc *tf = 
            new SSOTokenListenerThreadFunc(listener, 
					   args,
					   dispatchInSepThread);
        mListeners.push_back(tf);
    }

    bool removeListener(am_sso_token_listener_func_t listener)
    {
        bool retVal = false;
        std::list<SSOTokenListenerThreadFunc *>::iterator 
               iter = mListeners.begin(), 
               end = mListeners.end();

        // remove all entries in the vector that have listener 
	// no need to lock here since caller should already have 
	// exclusive access to the entry.
	while (iter != end) {
            if ((*iter)->mListener == listener) {
		SSOTokenListenerThreadFunc *tf = *iter;
		iter = mListeners.erase(iter);
                delete tf;
		retVal = true;
	    }
 	    else {
		iter++;
	    }
	}
        
        return retVal;
    }
    
    int getNumListeners() {
        return (int) mListeners.size();
    }

};

typedef RefCntPtr<SSOTokenListenerEntry> SSOTokenListenerEntryRefCntPtr;

END_PRIVATE_NAMESPACE

#endif
