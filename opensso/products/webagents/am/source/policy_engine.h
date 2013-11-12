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
 * $Id: policy_engine.h,v 1.8 2008/09/13 01:11:53 robertis Exp $
 *
 */
/*
 * Portions Copyrighted 2013 ForgeRock Inc
 */
#ifndef __POLICY_ENGINE_H__
#define __POLICY_ENGINE_H__
#include <vector>
#include <algorithm>
#include <string>
#include <stdexcept>

#include <am_policy.h>

#include "internal_exception.h"
#include "internal_macros.h"
#include "log.h"
#include "mutex.h"
#include "nspr_exception.h"
#include "properties.h"
#include "scope_lock.h"

BEGIN_PRIVATE_NAMESPACE

class SSOEntry;
class SSOToken;
class ResourceName;
class Service;
class NamingService;

/**
 * Engine class for managing policy for a particular service.
 * One instance of this object is created during each am_policy_init call.
 */
class PolicyEngine {
 private:
    Log::ModuleId logID;
    NamingService *namingSvc;

    void log(Log::Level, const char *, ...);
    /* Throws InternalException upon error */
    Service *getService(am_policy_t); 

    Mutex vectorLock;
    std::vector<Service *> services;

    Properties configParams;

 public:
    inline Log::ModuleId getModuleID() { return logID; }

    /* Throws:
     *	NSPRException upon NSPR error 
     *	InternalException upon other errors
     */
    PolicyEngine(const Properties& /*startup_params */);
    PolicyEngine(const std::string &, const Properties& /*serviceInitParams*/);
    am_policy_t create_service(const char * /*service Name */,
				  const char * /*instance name*/,
				  am_resource_traits_t rsrcTraits,
				  const Properties& /* service parameters */
				  ); 
    /* Throws:
     *	std::invalid_argument if policy handle is invalid.
     *	InternalException upon other errors
     */
    void destroy_service(am_policy_t); 

    /* Throws:
     *	std::invalid_argument if any argument is invalid
     *	NSPRException upon NSPR error.
     *	InternalException upon other errors.
     */
    void policy_evaluate(am_policy_t, const char *,
			 const char *, const char *, const am_map_t,
			 am_map_t,
			 am_policy_result_t *,
			 am_bool_t,
                 	 Properties& );

    /* Throws:
     *	NSPRException upon NSPR error 
     *	InternalException upon other errors
     */
    void policy_notify(am_policy_t, 
                       const char *, 
                       size_t,
                       bool configChangeNotificationEnabled = true);

    /* Throws:
     *	NSPRException upon NSPR error 
     *	InternalException upon other errors
     */
    void handleNotif(am_policy_t hdl, const std::string&);

    /* Throws:
     *	NSPRException upon NSPR error 
     *	InternalException upon other errors
     */
    void policy_notification_handler(Service *, 
                                     const std::string&,
                                     bool configChangeNotificationEnabled = true);

    /* Throws:
     *	NSPRException upon NSPR error 
     *	InternalException upon other errors
     */
    am_status_t invalidate_session(am_policy_t hdl, const char *ssoTokenId);
    am_status_t user_logout(am_policy_t hdl, 
                                   const char *ssoTokenId,
                                   Properties& );

    /* Throws InternalException upon error */
    bool isNotificationEnabled(am_policy_t); 

    inline am_policy_t addService(Service *svc) {
	ScopeLock scopeLck(vectorLock);
	am_policy_t x = (am_policy_t) services.size();
	services.push_back(svc);
	return x;
    }

    inline void removeService(Service *svc) {
	ScopeLock scopeLck(vectorLock);
	std::vector<Service *>::iterator iter = std::find(services.begin(),
							 services.end(),
							 svc);
	services.erase(iter);
    }

    ~PolicyEngine();
    bool isAccessAllowed();

    inline Service *getServicePublic(am_policy_t hdl){
	return getService(hdl);
    }	
};

END_PRIVATE_NAMESPACE

#endif
