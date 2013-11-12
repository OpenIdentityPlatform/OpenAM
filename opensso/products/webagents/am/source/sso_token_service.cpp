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
 * $Id: sso_token_service.cpp,v 1.8 2008/09/13 01:11:53 robertis Exp $
 *
 */
/*
 * Portions Copyrighted 2013 ForgeRock Inc
 */

#include <climits>
#include <ctime>
#include <string>
#include <list>
#include "internal_macros.h"
#include "am.h"
#include "sso_token.h"
#include "naming_service.h"
#include "service_info.h"
#include "sso_token_service.h"
#include "utils.h"
#include "url.h"


USING_PRIVATE_NAMESPACE

#define DEF_SSOTOKEN_SERVICE_NAME "SSOTokenService"
#define DEF_COOKIENAME "iPlanetDirectoryPro"

using std::invalid_argument;
using std::string;
using std::list;


namespace {
    const unsigned long START_THREADS = 1;
    unsigned long DEFAULT_HASH_SIZE = 131;
    const unsigned long DEFAULT_TIMEOUT = 3;
    const std::size_t DEFAULT_MAX_THREADS = 10;
}



/* 
 * Throws:
 *	std::invalid_argument if any argument is invalid 
 *	InternalException upon other errors
 */
SSOTokenService::SSOTokenService(const char *serviceName,
		                 const Properties& initParams)
    : SessionService(serviceName, initParams),
    mServiceName(serviceName),
    mServiceParams(initParams, mLogID),
    mLogID(Log::addModule(serviceName)),
    mNamingServiceURL(initParams.get(AM_COMMON_NAMING_URL_PROPERTY, "")),
    mNamingServiceInfo(mNamingServiceURL),
    mNamingService(initParams),
    mDefSessionURL(""),
    mSessionServiceInfo(mDefSessionURL),
    mNotifEnabled(initParams.getBool(
                      AM_COMMON_NOTIFICATION_ENABLE_PROPERTY, false)),
    mNotifURL(initParams.get(AM_COMMON_NOTIFICATION_URL_PROPERTY, "")),
    mSSOTokenListenerTable(DEFAULT_HASH_SIZE,
                           ULONG_MAX), // entry never times out
    mSSOTokenListenerTableLock(),
    mCookieName(initParams.get(AM_COMMON_COOKIE_NAME_PROPERTY, DEF_COOKIENAME)),
    mLoadBalancerEnabled(initParams.getBool(
                             AM_COMMON_LOADBALANCE_PROPERTY, true)),
    mSSOTokenTable(DEFAULT_HASH_SIZE,
		   mServiceParams.getPositiveNumber(
                      AM_SSO_CHECK_CACHE_INTERVAL_PROPERTY, DEFAULT_TIMEOUT)),
    mHTCleaner(NULL),
    mThreadPool(NULL),
    mInitialized(false),
    mLock(),
    mSSOListeners(),
    mSSOListenersLock()
{
    // check service name.
    if (mServiceName.empty()) {
        string msg = "Service name is empty";
	Log::log(mLogID, Log::LOG_ERROR, 
		 "SSOTokenService::SSOTokenService(): "
                 "Service name is empty.");
	throw std::invalid_argument(msg);
    }
    // check naming service URL if any.
    if (!mNamingServiceURL.empty() && mNamingServiceURL.size() < MIN_URL_LEN) {
	Log::log(mLogID, Log::LOG_ERROR,
		 "SSOTokenService::SSOTokenService(): "
                 "Naming Service URL %s is invalid.", 
                 mNamingServiceURL.c_str());
	throw std::invalid_argument("Naming Service URL is invalid.");
    }
    // check session service URL if any.
    if (!mDefSessionURL.empty() && mDefSessionURL.size() < MIN_URL_LEN) {
	Log::log(mLogID, Log::LOG_ERROR,
		 "SSOTokenService::SSOTokenService(): "
                 "Default Session Service URL %s is invalid.", 
                 mDefSessionURL.c_str());
	throw std::invalid_argument("Default Session Service URL is invalid.");
    }
    // log warning if both naming service & default session url are null.
    if (mNamingServiceURL.empty() && mDefSessionURL.empty()) {
        // this means session_url needs to be passed on every get session.
        Log::log(mLogID, Log::LOG_WARNING,
		 "SSOTokenService::SSOTokenService(): "
                 "At least one of naming server URL or default Session URL "
                 " must be set in properties.");
    }

    // init notification
    // In java sso api, notification is enabled if 
    // com.iplanet.am.session.client.polling.enable is false.
    if (mNotifEnabled) {
	if (mNotifURL.empty() || mNotifURL.size() < MIN_URL_LEN) { 
	    Log::log(mLogID, Log::LOG_WARNING,
		     "SSOTokenService::SSOTokenService(): "
		     "SSOTokenService notification URL is invalid. "
                     "Notification Disabled.");
	    mNotifEnabled = false;
            mNotifURL = "";
	}
        else {
	    Log::log(mLogID, Log::LOG_INFO,
		     "SSOTokenService::SSOTokenService(): "
		     "SSOTokenService notification enabled, URL = %s.",
		     mNotifURL.c_str());
        }
    } else {
	Log::log(mLogID, Log::LOG_INFO,
		 "SSOTokenService::SSOTokenService(): "
		 "SSOTokenService notification not enabled.");
    }

    initialize();

    Log::log(mLogID, Log::LOG_INFO,
	     "SSOTokenService::SSOTokenService(): "
	     "SSOTokenService created.");
}

/* Throws 
 *	std::invalid_argument if any argument is invalid 
 *	InternalException upon other errors.
 */
void
SSOTokenService::initialize()
{
    // lazy initialize of cache cleanup thread, 
    // called on first insert of hashtable.
    ScopeLock scopeLock(mLock);
    if (true==mInitialized) {
        Log::log(mLogID, Log::LOG_DEBUG,
                 "SSOTokenService::initialize(): "
                 "Cache already initialized");
        return;
    }
    // init thread pool for cache timeouts and notification.
    // each notification is passed to a thread to be processed.
    int numThreads = START_THREADS;
    if (mNotifEnabled) {
	numThreads += DEFAULT_MAX_THREADS;
    }

    try {
	mHTCleaner = new HTCleaner<SSOTokenEntry>(&mSSOTokenTable, 
                                   mServiceParams.getPositiveNumber(
			               AM_SSO_CHECK_CACHE_INTERVAL_PROPERTY, 
			               DEFAULT_TIMEOUT),
                                   "sso cache cleaner");
    } 
    catch(std::bad_alloc &bae) {
	throw InternalException(mServiceName, 
				"Memory allocation failure while "
				"creating hash table cleaner.",
				AM_NO_MEMORY);
    } 
    catch (...) {
	throw InternalException(mServiceName, 
				"Unknown exception when creating "
				"hash table cleaner",
				AM_FAILURE);
    }

    try {
	mThreadPool = new ThreadPool(START_THREADS, numThreads);
    } 
    catch(std::bad_alloc &bae) {
	throw InternalException(mServiceName, 
				"Memory allocation failure while"
				" creating thread pool.",
				AM_NO_MEMORY);
    }
    catch (...) {
	throw InternalException(mServiceName, 
				"Unknown exception when creating "
				"thread pool.",
				AM_FAILURE);
    }

    // start sso token timeout/hashtable cleanup thread.
    if (mThreadPool->dispatch(mHTCleaner) == false) {
        std::string msg = 
	         "SSOTokenService cache timeout thread dispatch failed";
	Log::log(mLogID, Log::LOG_ERROR, 
                 "SSOTokenService::initialize(): %s.", msg.c_str());
	throw InternalException(mServiceName, msg, AM_INIT_FAILURE);
    }
    Log::log(mLogID, Log::LOG_INFO,
             "SSOTokenService::initialize(): "
	     "dispatched hash table cleanup.");
    mInitialized = true;
    Log::log(mLogID, Log::LOG_INFO,
             "SSOTokenService::initialize(): "
	     "SSOTokenService cache initialized.");
    return;
}

SSOTokenService::~SSOTokenService() {
    ScopeLock scopeLock(mLock);
    if (mInitialized) {
        // Thread pool will free mHTCleaner pointer when it has 
        // stopped executing.
        mHTCleaner->stopCleaning();
        Log::log(mLogID, Log::LOG_DEBUG,
	         "SSOTokenService destructor: cache cleaner stopped.");
        if (mThreadPool) {
            delete mThreadPool;
            Log::log(mLogID, Log::LOG_DEBUG,
                     "SSOTokenService::~SSOTokenService(): "
	             "destructor: threads deleted.");
        }
	// Clean up sso listeners 
	std::list<SSOTokenListenerThreadFunc *>::iterator
	    iter = mSSOListeners.begin(),
	    end = mSSOListeners.end();
	while (iter != end) {
	    SSOTokenListenerThreadFunc *tf = 
		(SSOTokenListenerThreadFunc *)(*iter);
	    iter = mSSOListeners.erase(iter);
	    delete tf;
	}
    }
    else {
        Log::log(mLogID, Log::LOG_DEBUG,
                 "SSOTokenService::~SSOTokenService(): "
		 "cache not initialized.");
    }
}

/* Throws 
 *	InternalException upon errors.
 */
const ServiceInfo&
SSOTokenService::getServiceInfo(const std::string& ssoTokenID,
                                const ServiceInfo& serviceInfo,
                                const SSOTokenEntryRefCntPtr& entry,
                                NamingInfo& namingInfo) 
{
    const char *thisfunc = "SSOTokenService::getServiceInfo";

    if (serviceInfo.getNumberOfServers() > 0) {
	Log::log(mLogID, Log::LOG_DEBUG, 
                 "SSOTokenService::getServiceInfo(): "
                 "Using serviceInfo passed in.");
        return serviceInfo;
    }
    else if (entry) {
        const ServiceInfo& info = entry->getSessionServiceInfo();
	Log::log(mLogID, Log::LOG_DEBUG, 
                 "%s: Using service info from entry %s.",
		 thisfunc, (*info.begin()).getURL().c_str());
	// if entry was in cache, use server info from the entry.
        return info;
    }
    // get from naming url. if no naming url configured, or if 
    // error returned from naming url, use default session configured
    // from naming server.
    else if (!mNamingServiceURL.empty()) {
	// get session URL from naming service if any.
	am_status_t naming_sts;
	Http::CookieList cookieList;
	naming_sts = mNamingService.getProfile(mNamingServiceInfo, 
					       ssoTokenID,
					       cookieList,
					       namingInfo);
	if (naming_sts == AM_SUCCESS) {
            const ServiceInfo& info = namingInfo.getSessionSvcInfo();
	    Log::log(mLogID, Log::LOG_DEBUG, 
                     "%s: Using service info from naming service %s:%d.",
                     thisfunc, (*info.begin()).getHost().c_str(), 
                     (*info.begin()).getPort());
            return info;
	}
	else if (!mDefSessionURL.empty()) {
	    Log::log(mLogID, Log::LOG_WARNING, 
		     "%s: Error getting naming info for SSO Token ID %s, "
                     "error %d. Using default session URL %s.", thisfunc, 
		     ssoTokenID.c_str(), naming_sts, mDefSessionURL.c_str());
            return mSessionServiceInfo;
	}
	else {
	    Log::log(mLogID, Log::LOG_ERROR, 
                     "%s: Error getting naming info for SSO token ID %s " 
		     "and no default session URL configured. Error: %s",
		     thisfunc, ssoTokenID.c_str(), 
		     am_status_to_string(naming_sts));

            throw smi::InternalException(thisfunc, 
			   "Error getting naming info",  naming_sts);
	}
    }
    else if (mDefSessionURL.empty()) {
	// no default session url configured.
        std::string msg = 
		 "No naming URL or default session URL configured";
	Log::log(mLogID, Log::LOG_ERROR, 
                 "%s: %s.", thisfunc, msg.c_str());
        throw smi::InternalException(thisfunc, msg, AM_NOT_FOUND);
    }
    return mSessionServiceInfo;
}

void
SSOTokenService::buildCookieList(const std::string& ssoTokenID,
                                 Http::CookieList& cookieList, 
                                 const SSOTokenEntryRefCntPtr& entry)
{
    if (entry) {
	// Also pass all cookies set in the last response from server
	// except dpro, which we always set here. 
	// this is how loadbalancer works now (with WL) and best guess as 
	// to how it will work for other containers in the future. 
	Http::CookieList::const_iterator j = entry->getHttpCookieList().begin();
	Http::CookieList::const_iterator end = entry->getHttpCookieList().end();
	for (; j != end; j++) {
	    if ((*j).name == mCookieName) {
		Log::log(mLogID, Log::LOG_DEBUG,
                         "SSOTokenService::buildCookieList(): "
		         "Cached entry has dpro cookie "
		         "for sso token id %s.\n", ssoTokenID.c_str());
	    }
	    else {
		Log::log(mLogID, Log::LOG_DEBUG,
                         "SSOTokenService::buildCookieList(): "
		         "Passing cookie %s from cache for sso token id %s.\n", 
		         (*j).name.c_str(), ssoTokenID.c_str());
		cookieList.push_back(*j);
	    }
	}
    }
    Http::Cookie dproCookie(mCookieName, ssoTokenID);
    cookieList.push_back(dproCookie);
    Log::log(mLogID, Log::LOG_DEBUG,
             "SSOTokenService::buildCookieList(): "
	     "Added dpro cookie for sso token id %s.\n", 
             ssoTokenID.c_str());
    return;
}


am_status_t
SSOTokenService::getSessionInfo(const ServiceInfo& serviceInfo,
			        const std::string& ssoTokID,
			        Http::CookieList& cookieList,
                                bool resetIdleTimer,
			        SessionInfo& sessionInfo,
                                bool forceRefresh,
				bool xformToken)
{
    am_status_t sts = AM_FAILURE;

    /* From am_sso.cpp: 
     * Sometimes the user might have gotten the token
     * from the browser.  This is be actually done by
     * the user before passing it in, but we do not
     * offer a public method to do that.
     */
    /* 
     * Make this configurable since some users of this 
     * api have the sso token ID's not from the browser. 
     * url decoding it twice causes problems. 
     * This property name is chosen to be as close to the 
     * same property for java/server - 
     * com.iplanet.am.cookie.encode.
     */
    std::string ssoTokenID = ssoTokID;
    bool cookieEncoded = false;
    
    size_t pos = ssoTokID.find('%');

    if (pos != std::string::npos)
	cookieEncoded = true;
   
     if (xformToken && cookieEncoded) {
	Log::log(mLogID, Log::LOG_DEBUG, 
				"SSOTokenService::getSessionInfo(): "
			"Http decoding sso token ID %s.", ssoTokID.c_str());
	ssoTokenID = Http::decode(ssoTokID);
    }
	
    // find entry in cache, return cache entry if no need to go to server. 
    SSOTokenEntryRefCntPtr entry;
    entry = mSSOTokenTable.find(ssoTokenID);
    if (entry && !forceRefresh) {
        Log::log(mLogID, Log::LOG_DEBUG,
                "SSOTokenService::getSessionInfo(): "
                 "returning SSO Token %s found in cache.", 
                 ssoTokenID.c_str());
	sessionInfo = entry->getSessionInfo();
        sts = AM_SUCCESS;
    }
    else {
	NamingInfo namingInfo;
	try {
	    const ServiceInfo& svcInfo = getServiceInfo(ssoTokenID,
                                                        serviceInfo,
                                                        entry,
                                                        namingInfo);
	    Log::log(mLogID, Log::LOG_DEBUG,
                     "SSOTokenService::getSessionInfo(): "
		     "going to server %s.",
		     (*svcInfo.begin()).getURL().c_str());
            
            if (cookieList.empty()) {
               const std::string lbCookieName = namingInfo.getlbCookieName();
               const std::string lbCookieValue = namingInfo.getlbCookieValue();
            
               if (!lbCookieName.empty() && !lbCookieValue.empty()) {
                  Http::Cookie lbCookie(lbCookieName, lbCookieValue);
                  cookieList.push_back(lbCookie);
               }
            }
            
            // go to server
	    // pass dpro cookie in cookie list 
            if (mLoadBalancerEnabled && entry) 
                buildCookieList(ssoTokenID, cookieList, entry);
            
	    sts = SessionService::getSessionInfo(svcInfo,
						 ssoTokenID,
						 cookieList,
						 resetIdleTimer,
						 mNotifEnabled,
						 mNotifURL,
						 sessionInfo);
	    if (AM_SUCCESS==sts) {
		// remove old entry from cache and put new one in.
	        Log::log(mLogID, Log::LOG_DEBUG,
                         "SSOTokenService::getSessionInfo(): "
			 "adding newly created sessionInfo "
                         "for SSO Token ID %s to cache.", ssoTokenID.c_str());
		SSOTokenEntryRefCntPtr ssotokenEntry;
		SSOTokenEntryRefCntPtr oldEntry;
		ssotokenEntry = new SSOTokenEntry(sessionInfo, 
						  cookieList,
						  svcInfo);
		oldEntry = mSSOTokenTable.insert(ssoTokenID, ssotokenEntry);
		Log::log(mLogID, Log::LOG_MAX_DEBUG,
                         "SSOTokenService::getSessionInfo(): "
			 "SSO Token %s inserted in cache.", ssoTokenID.c_str());
		if (oldEntry) {
	            Log::log(mLogID, Log::LOG_MAX_DEBUG,
                             "SSOTokenService::getSessionInfo(): "
			     "old cached entry removed.");
 		}
		// initialize cache cleanup thread if not already.
		if (false==mInitialized)
		    initialize();
	    }
	    else {
                if (sts == AM_INVALID_SESSION) {
                    Log::log(mLogID, Log::LOG_INFO,
                         "SSOTokenService::getSessionInfo(): "
                         "Error %d for sso token ID %s.",
                         sts, ssoTokenID.c_str());
                } else {
		    Log::log(mLogID, Log::LOG_ERROR, 
                         "SSOTokenService::getSessionInfo(): "
			 "Error %d for sso token ID %s.",
			 sts, ssoTokenID.c_str());
                }
	    }
	} catch (smi::InternalException& exc) {
	    sts = exc.getStatusCode();
	    Log::log(mLogID, Log::LOG_ERROR,
                     "SSOTokenService::getSessionInfo(): "
		     "Internal Error %d for SSO Token ID %s.",
		     sts, ssoTokenID.c_str());
	} catch (std::exception& exc) {
	    sts = AM_FAILURE;
	    Log::log(mLogID, Log::LOG_ERROR,
                     "SSOTokenService::getSessionInfo(): "
		     "Exception occured while getting session "
		     "information for token:[%s]", ssoTokenID.c_str());
	    Log::log(mLogID, Log::LOG_ERROR, exc);
	} catch (...) {
	    Log::log(mLogID, Log::LOG_ERROR,
                     "SSOTokenService::getSessionInfo(): "
		     "Unknown exception for SSO Token ID %s.",
		     ssoTokenID.c_str());
            sts = AM_FAILURE;
	}
    }
    return sts;
}

am_status_t
SSOTokenService::destroySession(const ServiceInfo &serviceInfo,
                                SessionInfo &sessionInfo) {
    am_status_t sts = AM_FAILURE;
    const std::string &ssoTokenID = sessionInfo.getSSOToken().getString();
    sts = destroySession(serviceInfo, ssoTokenID);
    if(sts == AM_SUCCESS) {
	sessionInfo.setState(SessionInfo::DESTROYED);
    }
    return sts;
}

am_status_t
SSOTokenService::destroySession(const ServiceInfo& serviceInfo,
                                const std::string &ssoTokenID) {
    am_status_t sts = AM_FAILURE;

    // find token in the cache 
    SSOTokenEntryRefCntPtr entry;
    entry = mSSOTokenTable.find(ssoTokenID);
    if (!entry) {
	Log::log(mLogID, Log::LOG_DEBUG,
                 "SSOTokenService::destroySession(): "
	         "SSO Token %s to be destroyed/invalidated found in cache.",
                 ssoTokenID.c_str());
    }
   
    // add dpro cookie 
    Http::CookieList httpCookieList;
    if (mLoadBalancerEnabled && entry)
        buildCookieList(ssoTokenID, httpCookieList, entry);

    // destroy session.

    try {
	NamingInfo namingInfo;
	const ServiceInfo& svcInfo =
                getServiceInfo(ssoTokenID, serviceInfo, entry, namingInfo);
	Log::log(mLogID, Log::LOG_DEBUG,
                 "SSOTokenService::destroySession(): "
                 "going to server %s.", 
                 (*svcInfo.begin()).getURL().c_str());
        sts = SessionService::destroySession(
                          svcInfo,
			  ssoTokenID,
			  httpCookieList);
	if (AM_SUCCESS == sts) {
            if(entry) {
		entry->mSessionInfo.setState(SessionInfo::DESTROYED);
	    }
	    Log::log(mLogID, Log::LOG_DEBUG, 
                     "SSOTokenService::destroySession(): "
		     "invalidated SSO Token ID %s.",
		     ssoTokenID.c_str());
	    if (entry) {
		// if successful, remove entry from cache, since property names 
		// in the cache will already be out of date.
                // XXX should we remove from cache right away ? 
                // could mark it invalid and let cleaner remove it later.
		mSSOTokenTable.remove(ssoTokenID);
		Log::log(mLogID, Log::LOG_DEBUG, 
                         "SSOTokenService::destroySession(): "
			 "Removed invalidated SSO Token ID %s from cache.",
			 ssoTokenID.c_str());
	     }
	} else {
	    Log::log(mLogID, Log::LOG_ERROR, 
                     "SSOTokenService::destroySession(): "
		     "Error %d invalidating SSO Token %s.",
		     sts, ssoTokenID.c_str());
	}
    }
    catch (InternalException& exc) {
	sts = exc.getStatusCode();
	Log::log(mLogID, Log::LOG_ERROR,
                 "SSOTokenService::destroySession(): "
                 "Error %d destroy session SSO Token ID %s.",
                 sts, ssoTokenID.c_str());
    }
    catch (...) {
	Log::log(mLogID, Log::LOG_ERROR,
                 "SSOTokenService::destroySession(): "
		 "Unknown exception %d for SSO Token ID %s.",
		 ssoTokenID.c_str());
	sts = AM_FAILURE;
    }

    return sts;
}

am_status_t
SSOTokenService::setProperty(const ServiceInfo& serviceInfo,
			     SessionInfo& sessionInfo,
                             const std::string& name,
                             const std::string& value)
{
    am_status_t sts = AM_FAILURE;

    SSOToken& ssoTok = sessionInfo.getSSOToken();
    const std::string& ssoTokenID = ssoTok.getString();
    // find token in the cache 
    SSOTokenEntryRefCntPtr entry;
    entry = mSSOTokenTable.find(ssoTokenID);
    if (!entry) {
	Log::log(mLogID, Log::LOG_DEBUG,
                 "SSOTokenService::setProperty(): "
	         "SSO Token %s to set property found in cache.",
                  ssoTokenID.c_str());
    }
    else {
	Log::log(mLogID, Log::LOG_DEBUG,
                 "SSOTokenService::setProperty(): "
	         "SSO Token %s to set property NOT found in cache.",
                 ssoTokenID.c_str());
    }
   
    // add dpro cookie 
    Http::CookieList httpCookieList;
    if (mLoadBalancerEnabled && entry) 
        buildCookieList(ssoTokenID, httpCookieList, entry);

    // set property
    try {
	NamingInfo namingInfo;
	const ServiceInfo& svcInfo =
                getServiceInfo(ssoTokenID, serviceInfo, entry, namingInfo);
	Log::log(mLogID, Log::LOG_DEBUG,
                 "SSOTokenService::setProperty(): "
                 "going to server %s.", 
                 (*svcInfo.begin()).getURL().c_str());
        sts = SessionService::setProperty(svcInfo,
			                  sessionInfo,
                                          name, 
                                          value, 
			                  httpCookieList);
	if (AM_SUCCESS==sts) {
	    if (entry) {
		// if successful, remove entry from cache since properties
		// in the cached entry are no longer valid.
		mSSOTokenTable.remove(ssoTokenID);
		Log::log(mLogID, Log::LOG_DEBUG, 
                         "SSOTokenService::setProperty(): "
			 "Removed SSO Token %s from cache.",
			 ssoTokenID.c_str());
	     }
	} else {
	    Log::log(mLogID, Log::LOG_ERROR, 
                     "SSOTokenService::setProperty(): "
		     "Error %d set property for SSO Token %s. ",
		     sts, ssoTokenID.c_str());
	}
    }
    catch (InternalException& exc) {
	Log::log(mLogID, Log::LOG_ERROR,
                 "SSOTokenService::setProperty(): "
                 "Internal exception in SSO Token ID %s, ",
                 "error status %d.", ssoTokenID.c_str(), exc.getStatusCode());
	sts = exc.getStatusCode();
    }
    catch (...) {
	Log::log(mLogID, Log::LOG_ERROR,
                 "SSOTokenService::setProperty(): "
		 "Unknown exception encountered for SSO token ID %s.",
		 ssoTokenID.c_str());
	sts = AM_FAILURE;
    }

    return sts;
}

am_status_t
SSOTokenService::addSSOListener(const am_sso_token_listener_func_t listener,
			        void  *args,
				bool dispatchInSepThread)
{
    am_status_t sts = AM_SUCCESS;
    if (false==mNotifEnabled) {
        Log::log(mLogID, Log::LOG_ERROR, 
                 "SSOTokenService::addSSOListener(): "
                 "Cannot add listener - notification not enabled.");
        sts = AM_NOTIF_NOT_ENABLED;
    }
    else {
	try {
	    SSOTokenListenerThreadFunc *tf =
		new SSOTokenListenerThreadFunc(listener, 
					       args,
					       dispatchInSepThread);
	    ScopeLock scopeLock(mSSOListenersLock);
	    mSSOListeners.push_back(tf);
	}
	catch (std::exception& exc) {
	    Log::log(mLogID, Log::LOG_ERROR,
		     "SSOTokenService::addSSOListener(): "
		     "Unexpected error occurred - %s.", exc.what());
	    sts = AM_FAILURE;
	}
    }
    return sts;
}


/*
 * if allSSOTokens is true, listen for all sso events, not just event 
 * for a particular sso token ID.
 */
am_status_t
SSOTokenService::addSSOTokenListener(
        const ServiceInfo& serviceInfo,
	SessionInfo  *sessionInfoPtr,
        const am_sso_token_listener_func_t listener,
        void  *args,
        const std::string& notificationURL,
	bool dispatchInSepThread)
{
    am_status_t sts = AM_SUCCESS;

    if (false==mNotifEnabled) {
        Log::log(mLogID, Log::LOG_ERROR, 
                 "SSOTokenService::addSSOTokenListener(): "
                 "Cannot add listener - notification not enabled.");
        sts = AM_NOTIF_NOT_ENABLED;
    }
    else {
	const std::string& ssoTokenID = 
                           sessionInfoPtr->getSSOToken().getString();

	// find token in the cache 
	SSOTokenEntryRefCntPtr entry;
	entry = mSSOTokenTable.find(ssoTokenID);
	if (entry) {
	    Log::log(mLogID, Log::LOG_DEBUG,
                     "SSOTokenService::addSSOTokenListener(): "
		     "SSO Token %s found in cache.",
		     ssoTokenID.c_str());
	} else {
	    Log::log(mLogID, Log::LOG_DEBUG,
                     "SSOTokenService::addSSOTokenListener(): "
		     "SSO Token %s NOT found in cache.",
		     ssoTokenID.c_str());
           
            Http::CookieList cookieList;
            // bring token into cache.
           sts = getSessionInfo(ServiceInfo(),
                                sessionInfoPtr->getSSOToken().getString(),
                                cookieList,
                                false,
                                *sessionInfoPtr); 
        }
        if (sts != AM_SUCCESS) {
	   Log::log(mLogID, Log::LOG_ERROR,
                    "SSOTokenService::addSSOTokenListener(): "
		    "Could not get Session Info for sso token id %s "
		    "error %d.",
		    sessionInfoPtr->getSSOToken().getString().c_str(),
		    sts); 
        }
        else {
            try {
		// Note that when notification is enabled, the notification 
		// url in the properties file was already passed to the 
		// server as part of get session. 
		// 
		// addListener will get error from the server if the 
                // session was invalid.
		// 
		if (notificationURL.size() > 0) {

		    // get service info of sso token.
		    NamingInfo namingInfo;
		    const ServiceInfo& svcInfo =
			    getServiceInfo(ssoTokenID, 
                                           serviceInfo, 
                                           entry, 
                                           namingInfo);
		    // add dpro cookie 
		    Http::CookieList httpCookieList;
		    if (mLoadBalancerEnabled && entry) 
			buildCookieList(ssoTokenID, httpCookieList, entry);

		    Log::log(mLogID, Log::LOG_DEBUG,
                             "SSOTokenService::addSSOTokenListener(): "
			     "sending notif URL %s for sso token %s.",
			     (*svcInfo.begin()).getHost().c_str(),
			     ssoTokenID.c_str());

		    sts = SessionService::addListener(svcInfo,
						      ssoTokenID,
						      httpCookieList,
						      notificationURL);
		    
		    if (AM_SUCCESS==sts) {
			Log::log(mLogID, Log::LOG_DEBUG, 
                                 "SSOTokenService::addSSOTokenListener(): "
				 "added notif URL %s for token %s.",
				 notificationURL.c_str(), ssoTokenID.c_str());
		    }
		    else {
			Log::log(mLogID, Log::LOG_ERROR, 
                                 "SSOTokenService::addSSOTokenListener(): "
				 "Error %d when adding notif URL %s "
				 "for token %s.",
				 sts, 
                                 notificationURL.c_str(), 
                                 ssoTokenID.c_str());
		    }
		}

		// Now add listener to listeners table.
		// don't add listener if adding the notification url failed.
		if (AM_SUCCESS == sts) {
		    // add listener function to token ID.
		    // lock the table so the entry is not removed while we add 
		    // a new entry or to an existing entry.
		    SSOTokenListenerEntryRefCntPtr ssoTokenListenerEntry;
		    SSOTokenListenerEntryRefCntPtr oldEntry;
		    {
			ScopeLock scopelock(mSSOTokenListenerTableLock);
			oldEntry = mSSOTokenListenerTable.find(ssoTokenID);
			if (oldEntry) {
			    oldEntry->addListener(listener, args,
						  dispatchInSepThread);
			}
			else {
			    ssoTokenListenerEntry = 
				   new SSOTokenListenerEntry(
					listener, args,
					dispatchInSepThread);
			    oldEntry = mSSOTokenListenerTable.insert(
					ssoTokenID, ssoTokenListenerEntry);
			}
		    }
		    Log::log(mLogID, Log::LOG_INFO,
                             "SSOTokenService::addSSOTokenListener(): "
			     "Added %s listener for SSO token %s.", 
			     oldEntry ? "" : "first",
                             ssoTokenID.c_str());
		}
            }
	    catch (InternalException& exc) {
		sts = exc.getStatusCode();
		Log::log(mLogID, Log::LOG_ERROR,
                         "SSOTokenService::addSSOTokenListener(): "
			 "Internal exception encountered for SSO Token ID %s, " 
			 "error status %d.", 
			 ssoTokenID.c_str(), sts);
	    }
	    catch (...) {
		Log::log(mLogID, Log::LOG_ERROR,
                         "SSOTokenService::addSSOTokenListener(): "
			 "Unknown exception encountered for SSO token ID %s.",
			 ssoTokenID.c_str());
		sts = AM_FAILURE;
	    }
        }
    }

    return sts;
}


/* 
 * Throws:
 *	XMLTree::ParseException upon XML parsing error
 *	NSPRException upon NSPR error 
 *	InternalException upon other errors
 */
am_status_t
SSOTokenService::handleNotif(const std::string& notifData)
{
    am_status_t sts = AM_SUCCESS;
    am_status_t sts1 = AM_SUCCESS;

    // parse incoming data.
    // get session id, state, notification type, and time.

    try {
        XMLTree::Init xt;
        XMLTree tree(false, notifData.c_str(), notifData.size());
        XMLElement rootElement = tree.getRootElement();
	XMLElement sessionElem;
	XMLElement typeElem;
	XMLElement timeElem;
	std::string sessionID, state, typeStr, timeStr;
	long typeVal;
	time_t timeVal;

	// parse notification
	if(!rootElement.isNamed(SESSION_NOTIFICATION)) {
	    Log::log(mLogID, Log::LOG_ERROR,
                     "SSOTokenService::handleNotif(): "
		     "Not a session notification.");
	    sts = AM_ERROR_PARSING_XML;
	}
	else if (!rootElement.getSubElement(SESSION, sessionElem) ||
	         !rootElement.getSubElement(SESSION_NOTIF_TYPE, typeElem) ||
	         !rootElement.getSubElement(SESSION_NOTIF_TIME, timeElem))
        {
            Log::log(mLogID, Log::LOG_ERROR,
                     "SSOTokenService::handleNotif(): "
                     "Session notification missing "
                     "session, type, or time element.");
            sts = AM_ERROR_PARSING_XML;
        }
        // parse type, time.
        else if (!typeElem.getValue(typeStr) || 
                 !timeElem.getValue(timeStr)) 
        {
            Log::log(mLogID, Log::LOG_ERROR,
                     "SSOTokenService::handleNotif(): "
                     "Session notification error getting "
                     "notification type or time element value.");
            sts = AM_ERROR_PARSING_XML;
        }
        else if (sscanf(typeStr.c_str(), "%d", &typeVal) != 1 ||
                 sscanf(timeStr.c_str(), "%lld", &timeVal) != 1) 
        {
            Log::log(mLogID, Log::LOG_ERROR,
                     "SSOTokenService::handleNotif(): "
                     "Session notification error parsing "
                     "notification type or time.");
            sts = AM_ERROR_PARSING_XML;
        }
        // get session id
        else if (!sessionElem.getAttributeValue(SESSION_ID_ATTRIBUTE, 
                                                sessionID)) {
            Log::log(mLogID, Log::LOG_ERROR,
                     "SSOTokenService::handleNotif(): "
                     "Session ID missing from Notification message.");
            sts = AM_ERROR_PARSING_XML;
        }
	// get session state
        else if (!sessionElem.getAttributeValue(SESSION_STATE_ATTRIBUTE,
                                                state)) {
            Log::log(mLogID, Log::LOG_ERROR,
                     "SSOTokenService::handleNotif(): "
                     "Session state missing from Notification message.");
            sts = AM_ERROR_PARSING_XML;
        }
        else {
	    // everything parsed ok. 

            // update cache
            if (strcasecmp(state.c_str(), SESSION_STATE_VALUE_VALID)!=0) {
                try {
                    SSOTokenEntryRefCntPtr entry = 
				mSSOTokenTable.remove(sessionID);
                    Log::log(mLogID, Log::LOG_DEBUG,
                             "SSOTokenService::handleNotif(): "
                             "destroyed Session ID %s removed from cache.",
                             sessionID.c_str());
		    if (!entry) {
                        Log::log(mLogID, Log::LOG_DEBUG,
                                 "SSOTokenService::handleNotif(): "
                                 "destroyed Session ID %s not found in cache.",
                                 sessionID.c_str());
		    }
                }
                catch (...) {
                    // session ID not in cache.
                    Log::log(mLogID, Log::LOG_DEBUG,
                             "SSOTokenService::handleNotif(): "
                             "destroyed Session ID %s not found in cache.",
                             sessionID.c_str());
                }
            }

	    Log::log(mLogID, Log::LOG_DEBUG,
                     "SSOTokenService::handleNotif(): "
		     "Calling listeners for SSO Token %s.",
                     sessionID.c_str());

            am_sso_token_event_type_t event_type = getEventType(typeVal);

	    // call listeners for all events.
	    // call all listeners and return sts if any fail.
	    sts1 = callSSOListeners(sessionID, 
				    sessionElem, event_type, timeVal); 

	    // call listeners for events for a session ID.	
	    sts = callSSOTokenListeners(sessionID, 
					sessionElem, event_type, timeVal);

	    if (sts == AM_SUCCESS && sts1 != AM_SUCCESS)
		sts = sts1;

	}
    }
    catch (XMLTree::ParseException &exc) {
        const char *msg = exc.what();
	Log::log(mLogID, Log::LOG_ERROR,
                 "SSOTokenService::handleNotif(): "
		 "error %s parsing XML message.",
                 msg == NULL ? "" : msg);
	sts = AM_ERROR_PARSING_XML;
    }
    
    Log::log(mLogID, Log::LOG_DEBUG,
             "SSOTokenService::handleNotif(): "
             "Return %d.", sts);

    return sts;
}

am_status_t 
SSOTokenService::callSSOTokenListeners(
	const std::string& sessionID, 
	const XMLElement& sessionElem, 
	const am_sso_token_event_type_t event_type, 
	const time_t event_time) 
{
    am_status_t sts = AM_SUCCESS;
    am_status_t sts1 = AM_SUCCESS;
    try {
	SSOTokenListenerEntryRefCntPtr listenerEntry;
	{ 
	    // lock the table so another thread does not add an entry 
	    // or listener for the session ID at the same time.
	    ScopeLock scopelock(mSSOTokenListenerTableLock);
	    listenerEntry = mSSOTokenListenerTable.remove(sessionID);
	} 
	if (!listenerEntry) {
	    Log::log(mLogID, Log::LOG_INFO,
		     "SSOTokenService::callSSOTokenListeners(): "
		     "No sso token listeners found for SSO token id %s.",
		     sessionID.c_str());
	    // if no listeners found, we're done.
	}
	else {
	    Log::log(mLogID, Log::LOG_INFO,
		     "SSOTokenService::callSSOTokenListeners(): "
		     "Calling listeners for SSO token id %s.", 
		     sessionID.c_str());

	    // no need to copy the list or thread func like callSSOListener 
	    // because it is already removed from the table
	    std::list<SSOTokenListenerThreadFunc *>::const_iterator 
		    iter = listenerEntry->mListeners.begin(),
		    end = listenerEntry->mListeners.end();
	    for (; iter != end; iter++) {
		sts1 = callTheListener((*iter), sessionID, sessionElem, 
				       event_type, event_time);
		if (sts1 != AM_SUCCESS)
		    sts = sts1;
	    }
	}
    }
    catch (std::exception& exc) {
	sts = AM_FAILURE;
	Log::log(mLogID, Log::LOG_ERROR,
		 "SSOTokenService::callSSOTokenListeners(): "
		 "Unexpected exception '%s' occurred." ,
		 exc.what());
    }
    return sts;
}

am_status_t 
SSOTokenService::callSSOListeners(const std::string& sessionID, 
				  const XMLElement& sessionElem, 
				  const am_sso_token_event_type_t event_type, 
				  const time_t event_time) 
{
    am_status_t sts = AM_SUCCESS;
    am_status_t sts1 = AM_SUCCESS;
    try {
	Log::log(mLogID, Log::LOG_INFO,
		 "SSOTokenService::callSSOListeners(): "
		 "Calling SSO listeners."); 
	std::list<SSOTokenListenerThreadFunc *> callList;
	// clone the list (including entries) so we don't have to 
	// lock the list during dispatch.
	{
	    ScopeLock scopelock(mSSOListenersLock);
	    std::list<SSOTokenListenerThreadFunc *>::const_iterator 
	        iter = mSSOListeners.begin(),
	        end = mSSOListeners.end();
	    for (; iter != end; iter++) {
	        callList.push_back(new SSOTokenListenerThreadFunc(
				   *(SSOTokenListenerThreadFunc *)(*iter)));
	    }
	}
	std::list<SSOTokenListenerThreadFunc *>::const_iterator 
	    call_iter = callList.begin(),
	    call_end = callList.end();
	for (; call_iter != call_end; call_iter++) {
	    sts1 = callTheListener(*call_iter, sessionID, sessionElem, 
				   event_type, event_time);
	    if (sts1 != AM_SUCCESS)  // error already logged 
		sts = sts1;
	}
    }
    catch (std::exception& exc) {
	sts = AM_FAILURE;
	Log::log(mLogID, Log::LOG_ERROR,
		 "SSOTokenService::callSSOListeners(): "
		 "Unexpected exception '%s' occurred when calling ." 
		 "SSO listeners for session ID %s.",
		 exc.what(), sessionID.c_str());
    }
    return sts;
}

am_status_t
SSOTokenService::callTheListener(
	SSOTokenListenerThreadFunc *listenerThrFunc,
	const std::string& sessionID, 
	const XMLElement& sessionElem, 
	const am_sso_token_event_type_t event_type, 
	const time_t event_time) 
{
    am_status_t sts = AM_SUCCESS;
    try {
	listenerThrFunc->updateSessionInfo(sessionElem);
	listenerThrFunc->setEvent(event_type, event_time);
	if (listenerThrFunc->doDispatchInSepThread() == true) {
	    if (mThreadPool->dispatch(listenerThrFunc) == true) {
		Log::log(mLogID, Log::LOG_INFO,
			 "SSOTokenService::callTheListener():"
			 " Dispatched listener 0x%x for "
			 "sso token ID %s.",
			 listenerThrFunc->getListener(),
			 sessionID.c_str());
	    }
	    else {
		// exact error from dispatch is logged.
		// it is either a memory or nspr error.
		Log::log(mLogID, Log::LOG_ERROR,
			 "SSOTokenService::callTheListener():"
			 " Error dispatching listener 0x%x for "
			 "sso token ID %s. listener removed.",
			 listenerThrFunc->getListener(),
			 sessionID.c_str());
		sts = AM_ERROR_DISPATCH_LISTENER;
	    }
	}
	else {
	    try {
		listenerThrFunc->callListener();
	    }
	    catch (std::exception& ex) {
		Log::log(mLogID, Log::LOG_ERROR,
			 "SSOTokenService::callTheListener():"
			 " exception '%s' from calling listener"
			 " 0x%x.",
			 ex.what(), listenerThrFunc->getListener());
	    }
	    // delete it after it's called just like thread pool 
	    delete listenerThrFunc;
	}
    }
    catch(std::exception& exc) {
	const char *m = exc.what();
	sts = AM_ERROR_PARSING_XML;
	Log::log(mLogID, Log::LOG_ERROR,
		 "SSOTokenService::callTheListener(): "
		 "Error %s parsing XML for sso token ID %s." ,
		 m==NULL ? "" : m,
		 sessionID.c_str());
    }
    return sts;
}

am_status_t
SSOTokenService::removeSSOTokenListener(const std::string& ssoTokenID, 
                                        const am_sso_token_listener_func_t
                                                               listener)
{
    am_status_t sts = AM_SUCCESS;
    try {
	// lock the table so another thread won't remove the entry 
	// from callSSOTOkenListeners or add an entry at the same time.
        SSOTokenListenerEntryRefCntPtr ssoTokenListenerEntry, oldEntry;
	bool removed = false;  // found and removed.
	{
	    ScopeLock scopelock(mSSOTokenListenerTableLock);
	    ssoTokenListenerEntry = mSSOTokenListenerTable.find(ssoTokenID);
	    if (ssoTokenListenerEntry) {
	        removed = ssoTokenListenerEntry->removeListener(listener);
                if (removed && ssoTokenListenerEntry->getNumListeners()==0) {
                    oldEntry = mSSOTokenListenerTable.remove(ssoTokenID);
                }
            }
	}
        if (removed) {
	    Log::log(mLogID, Log::LOG_INFO, 
		     "SSOTokenService::removeSSOTokenListener(): "
		     "%s listener for sso token id %s removed.", 
		     oldEntry ? "last" : "",
		     ssoTokenID.c_str());
	}
	else {
	    Log::log(mLogID, Log::LOG_ERROR, 
		     "SSOTokenService::removeSSOTokenListener(): "
		     "listener for sso token id %s not found.", 
		     ssoTokenID.c_str());
	    sts = AM_NOT_FOUND;
        }
    } catch (...) {
	// entry not found.
        Log::log(mLogID, Log::LOG_ERROR, 
		 "SSOTokenService::remoteSSOTokenListener(): "
                 "listener for sso token id %s not found.", 
                 ssoTokenID.c_str());
        sts = AM_NOT_FOUND;
    }
    return sts;
}

am_status_t
SSOTokenService::removeSSOListener(const am_sso_token_listener_func_t listener)
{ 
    am_status_t retVal = AM_NOT_FOUND;
    if (listener != NULL) {
	try {
	    {
		ScopeLock scopelock(mSSOListenersLock);
		std::list<SSOTokenListenerThreadFunc *>::iterator 
		    iter = mSSOListeners.begin(),
		    end = mSSOListeners.end();
		while (iter != end) {
		    if ((*iter)->getListener() == listener) {
			SSOTokenListenerThreadFunc *tf = 
			    (SSOTokenListenerThreadFunc *)(*iter);
			iter = mSSOListeners.erase(iter);
			delete tf;
			retVal = AM_SUCCESS;
		    }
		    else {
			++iter;
		    }
		}
	    }
	    if (AM_NOT_FOUND == retVal) {
	      	Log::log(mLogID, Log::LOG_DEBUG,
		         "SSOTokenService::removeSSOListener(): "
		         "SSO listener 0x%x removed.", listener);
	    }
	}
	catch (std::exception& exc) {
	    Log::log(mLogID, Log::LOG_ERROR,
		     "SSOTokenService::removeSSOListener(): "
		     "Unexpected error occurred - %s.", exc.what());
	    retVal = AM_FAILURE;
	}
    }
    return retVal;
}
