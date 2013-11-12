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
 * $Id: sso_token_entry.h,v 1.3 2008/06/25 08:14:38 qcheng Exp $
 *
 */ 
/*
 * Portions Copyrighted 2013 ForgeRock Inc
 */

#ifndef __SSO_TOKEN_ENTRY_H__
#define __SSO_TOKEN_ENTRY_H__
#include <stdexcept>
#include <am_map.h>
#include <am_policy.h>
#include "mutex.h"
#include "internal_exception.h"
#include "tree.h"
#include "sso_token.h"
#include "ref_cnt_ptr.h"
#include "mutex.h"
#include "scope_lock.h"
#include "session_info.h"
#include "service_info.h"
#include "naming_info.h"
#include "http.h"

BEGIN_PRIVATE_NAMESPACE

class SSOTokenEntry:public RefCntObj {
private:
    SessionInfo mSessionInfo;
    Http::CookieList mHttpCookieList;
    ServiceInfo mSessionSvcInfo;

public:
    // Friends
    friend class SSOTokenService;

    virtual ~SSOTokenEntry() 
    { }
    SSOTokenEntry() : mSessionInfo(SessionInfo()), 
                      mHttpCookieList(Http::CookieList()),
                      mSessionSvcInfo(ServiceInfo())
    { }
    SSOTokenEntry(const SessionInfo& sessionInfo,
                  const Http::CookieList cookieList,
                  const ServiceInfo serviceInfo) :
        mSessionInfo(sessionInfo),
        mHttpCookieList(cookieList),
        mSessionSvcInfo(serviceInfo)
    { }

    inline const SSOToken& getSSOToken() const {
	return mSessionInfo.getSSOToken();
    }

    inline const Http::CookieList& getHttpCookieList() const {
	return mHttpCookieList;
    }

    inline const SessionInfo &getSessionInfo() {
	return mSessionInfo;
    }

    inline const ServiceInfo &getSessionServiceInfo() {
	return mSessionSvcInfo;
    }

};

typedef RefCntPtr<SSOTokenEntry> SSOTokenEntryRefCntPtr;

END_PRIVATE_NAMESPACE

#endif
