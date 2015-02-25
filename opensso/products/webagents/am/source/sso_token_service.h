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
 * $Id: sso_token_service.h,v 1.7 2008/09/13 01:11:53 robertis Exp $
 *
 */
/*
 * Portions Copyrighted 2013 ForgeRock Inc
 */

#ifndef __SSO_TOKEN_SERVICE_H__
#define __SSO_TOKEN_SERVICE_H__

#include <stdexcept>
#include <string>

#include <am_sso.h>

#include "xml_element.h"
#include "hash_table.h"
#include "http.h"
#include "internal_exception.h"
#include "nspr_exception.h"
#include "internal_macros.h"
#include "mutex.h"
#include "naming_service.h"
#include "properties.h"
#include "session_service.h"
#include "thread_pool.h"
#include "thread_function.h"
#include "pnotify_handler.h"
#include "ht_cleaner.h"
#include "sso_token_entry.h"   
#include "am_sso.h"
#include "sso_token_listener_entry.h"   

BEGIN_PRIVATE_NAMESPACE

class SSOToken;

class SSOTokenService: public SessionService {
private:
    static am_sso_token_event_type_t getEventType(int t) {
	am_sso_token_event_type_t retVal;
	switch (t) {
	    case 1:
		retVal = AM_SSO_TOKEN_EVENT_TYPE_IDLE_TIMEOUT;
		break;
	    case 2:
		retVal = AM_SSO_TOKEN_EVENT_TYPE_MAX_TIMEOUT;
		break;
	    case 3:
		retVal = AM_SSO_TOKEN_EVENT_TYPE_LOGOUT;
		break;
	    case 5:
		retVal = AM_SSO_TOKEN_EVENT_TYPE_DESTROY;
		break;
	    default:
		// CREATION and REACTIVATION are not received and not 
                // handled currently.
		retVal = AM_SSO_TOKEN_EVENT_TYPE_UNKNOWN;
		break;
	}
	return retVal;
    }

    std::string mServiceName;

    Properties mServiceParams;    
    Log::ModuleId mLogID;

    std::string mNamingServiceURL;
    ServiceInfo mNamingServiceInfo;
    NamingService mNamingService;
  
    std::string mDefSessionURL;
    ServiceInfo mSessionServiceInfo;

    bool mNotifEnabled;
    std::string mNotifURL;
    HashTable<SSOTokenListenerEntry> mSSOTokenListenerTable;
    Mutex mSSOTokenListenerTableLock;

    std::string mCookieName;
    bool mLoadBalancerEnabled;

    HashTable<SSOTokenEntry> mSSOTokenTable;
    HTCleaner<SSOTokenEntry> *mHTCleaner;
    ThreadPool *mThreadPool;   // for cache timeout & notifications

    bool mInitialized;
    Mutex mLock;

    std::list<SSOTokenListenerThreadFunc *> mSSOListeners;
    Mutex mSSOListenersLock;


    /* 
     * All functions throw:
     *	std::invalid_argument if any argument is invalid 
     *	XMLTree::ParseException upon XML parsing error
     *	NSPRException upon NSPR error 
     *	InternalException upon other errors
     */

    void initialize(); 

    const ServiceInfo& getServiceInfo(const std::string& ssoTokenID,
                                      const ServiceInfo& serviceInfo,
                                      const SSOTokenEntryRefCntPtr& entry,
                                      NamingInfo& namingInfo);

    void buildCookieList(const std::string& ssoTokenID,
                         Http::CookieList& cookieList, 
                         const SSOTokenEntryRefCntPtr& entry);
                         
    am_status_t callSSOTokenListeners(
			const std::string& sessionID, 
			const XMLElement& sessionElem, 
			const am_sso_token_event_type_t event_type, 
			const time_t event_time);

    am_status_t callSSOListeners(
			const std::string& sessionID, 
			const XMLElement& sessionElem, 
			const am_sso_token_event_type_t event_type, 
			const time_t event_time);


    am_status_t callTheListener(
			SSOTokenListenerThreadFunc *listenerThrFunc,
			const std::string& sessionID, 
			const XMLElement& sessionElem, 
			const am_sso_token_event_type_t event_type, 
			const time_t event_time);
    
public:
    /* 
     * All functions throw:
     *	std::invalid_argument if any argument is invalid 
     *	XMLTree::ParseException upon XML parsing error
     *	NSPRException upon NSPR error 
     *	InternalException upon other errors
     */

    SSOTokenService(const char *serviceName, const Properties& initParams);

    virtual ~SSOTokenService();

    bool inline isServiceNamed(const std::string &svcName) {
	return (mServiceName == svcName);
    }

    inline bool isNotificationEnabled() const { return mNotifEnabled; }

    bool inline operator==(const SSOTokenService& svc) {
	if (NULL != &svc)
	    return false;
	return ((svc.mServiceName == this->mServiceName));
    }

    am_status_t handleNotif(const std::string& notifData);

    const Properties& getProperties() { return mServiceParams; }

    am_status_t getSessionInfo(const ServiceInfo& serviceInfo,
			       const std::string& ssoTokenID,
			       Http::CookieList& cookieList,
                               bool resetIdleTimer,
			       SessionInfo& sessionInfo,
                               bool forceRefresh=false,
			       bool xformToken=true);

    am_status_t destroySession(const ServiceInfo& serviceInfo,
			       SessionInfo& sessionInfo);

    am_status_t destroySession(const ServiceInfo& serviceInfo,
			       const std::string& ssoTokenID);

    am_status_t setProperty(const ServiceInfo& serviceInfo,
			    SessionInfo& sessionInfo,
                            const std::string& name,
                            const std::string& value);

    am_status_t addSSOTokenListener(const ServiceInfo& serviceInfo,
                                    SessionInfo  *sessionInfo,
                                    const am_sso_token_listener_func_t listener,
                                    void *args,
                                    const std::string& notifURL,
				    bool dispatchInSepThread=true);

    am_status_t removeSSOTokenListener(const std::string& sso_token_id, 
                                       const am_sso_token_listener_func_t lsnr);

    am_status_t addSSOListener(
			const am_sso_token_listener_func_t listener,
			void *args,
			bool dispatchInSepThread);

    am_status_t removeSSOListener(
			const am_sso_token_listener_func_t listener);
    inline void removeSSOTokenTableEntry(std::string ssoToken)
    {
        mSSOTokenTable.remove(ssoToken);
    }

};


END_PRIVATE_NAMESPACE

#endif	// not __SSO_TOKEN_SERVICE_H__
