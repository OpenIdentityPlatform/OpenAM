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
 * $Id: service.h,v 1.16 2008/11/10 22:56:37 madan_ranganath Exp $
 *
 * Abstract:
 *
 * This class encapsulates the policy information associated with a
 * particular service.
 *
 */
/*
 * Portions Copyrighted 2013 ForgeRock Inc
 */

#ifndef __SERVICE_H__
#define __SERVICE_H__

#include <stdexcept>
#include <string>
#include <vector>

#include "auth_svc.h"
#include "policy_service.h"
#include "hash_table.h"
#include "http.h"
#include "internal_exception.h"
#include "key_value_map.h"
#include "nspr_exception.h"
#include "internal_macros.h"
#include "mutex.h"
#include "naming_service.h"
#include "properties.h"
#include "policy_entry.h"
#include "session_info.h"
#include "xml_tree.h"
#include "thread_pool.h"
#include "thread_function.h"
#include "pnotify_handler.h"
#include "ht_cleaner.h"
#include "agent_config_fetch.h"
#include "agent_config_cache_cleanup.h"
#include "sso_token_service.h"
#include "audit_log.h"

BEGIN_PRIVATE_NAMESPACE

#define USER_ID_PARAM_TYPE_SESSION 1
#define USER_ID_PARAM_TYPE_LDAP 2


class SSOToken;

class Service {
 private:
    Log::ModuleId logID;
    Properties svcParams;    
    bool initialized;   
    std::string serviceName;
    std::string instanceName;
    bool notificationEnabled;
    std::string notificationURL;
    HashTable<PolicyEntry> policyTable;
    PolicyEntryRefCntPtr mPolicyEntry;
    SessionInfo mAppSessionInfo;
    am_resource_traits_t rsrcTraits;
    std::list<std::string> notenforcedList;
    
    bool threadPoolCreated;
    ThreadPool *tPool;
    HTCleaner<PolicyEntry> *htCleaner;
    
    bool threadPoolAgentFetchCreated;
    ThreadPool *tPoolAgentFetch;
    AgentConfigFetch *agentConfigFetch;

    bool threadPoolAgentConfigCleanupCreated;
    ThreadPool *tPoolAgentConfigCleanup;
    AgentConfigCacheCleanup *agentConfigCleanup;

    bool threadPoolAuditLogCreated;
    ThreadPool *tPoolAuditLog;
    AuditLog *auditLog;

    Mutex lock;
    ServiceInfo namingSvcInfo;
    AuthService authSvc;
    AuthContext authCtx;               // Agent's auth context
    NamingService namingSvc;
    PolicyService *policySvc;

    SSOTokenService &mSSOTokenSvc;  
    bool isLocalRepo;

    /* 
     * All functions throw:
     *	    std::invalid_argument if any argument is invalid 
     *	    XMLTree::ParseException upon XML parsing error
     *	    NSPRException upon NSPR error 
     *	    InternalException upon other errors
     */

    bool construct_policy_decision(PolicyDecision &, XMLElement &);

    void construct_auth_svc(PolicyEntryRefCntPtr);

    void do_agent_auth_login(SSOToken&);

    am_status_t do_agent_auth_logout();

    void initialize();

    void update_policy(const SSOToken &, const std::string &,
		       const std::string &,
		       const KeyValueMap &,
                       SessionInfo &,
		       policy_fetch_scope_t scope,
		       bool refetchPolicy,
                       PolicyEntryRefCntPtr &,
                       const std::list<std::string> &,
                       Properties &);

    bool do_update_policy(const SSOToken &ssoTok, const std::string &resName,
		       const std::string &actionName,
		       const KeyValueMap &env,
                       SessionInfo &sessionInfo,
		       policy_fetch_scope_t scope,
                       PolicyEntryRefCntPtr &,
                       const std::list<std::string> &,
                       Properties &);

    void update_policy_list(const SSOToken &,
			    const std::vector<std::string> &,
			    const std::string&, const KeyValueMap &,
                            PolicyEntryRefCntPtr &,
                            const std::list<std::string> &,
                            Properties &);

    void process_policy_response(PolicyEntryRefCntPtr &,
				 const KeyValueMap &,
				 const std::string &);

    const am_status_t destroyAppSSOToken();

    class PolicyHandler:public ThreadFunction {
    private:
	std::string resName;
	NotificationType nType;
	Service &service;

    public:
	PolicyHandler(Service &svc,
		      const std::string &rName,
		      NotificationType type): ThreadFunction("PolicyHandler"), resName(rName),
					     nType(type),
					     service(svc){}


	void operator()() const {
	    PolicyNotificationHandler pNotif(resName, nType);
	    (service.policyTable).for_each(pNotif);
	}

	virtual ~PolicyHandler() {
	}
    };

    friend class PolicyHandler;

 public:
    /* 
     * All functions throw:
     *	    std::invalid_argument if any argument is invalid 
     *	    XMLTree::ParseException upon XML parsing error
     *	    NSPRException upon NSPR error 
     *	    InternalException upon other errors
     */
    Service(const char * /*serviceName*/,
	    const char * /*instanceName*/,
	    const am_resource_traits_t /*rsrcTraits*/,
	    const Properties& /*initParams*/);
    ~Service();

    bool inline isServiceNamed(const std::string &svcName) const {
	return (serviceName == svcName);
    }

    inline const std::string &getServiceName() const {return serviceName;}
    inline bool isNotificationEnabled() const { return notificationEnabled; }

    bool inline operator==(const Service& svc) {
	if(NULL != &svc)
	    return false;
	return ((svc.serviceName == this->serviceName) &&
		(svc.instanceName == this->instanceName));
    }

    void sso_notify(const std::string &);

    void policy_notify(const std::string &,
		       NotificationType);
    
    void agent_config_change_notify();

    am_status_t invalidate_session(const char *ssoTokenId);

    am_status_t user_logout(const char *ssoTokenId,
                                   Properties& properties);
    
    am_status_t invalidate_user_session(const char *ssoTokenId, Properties& properties);

    void flushPolicyEntry(const SSOToken&);

    void setRemUserAndAttrs(am_policy_result_t *policy_res,
                            PolicyEntryRefCntPtr &uPolicyEntry,
                            const SessionInfo sessionInfo,
                            std::string& resName,
                            const std::vector<PDRefCntPtr>& results,
                            Properties& properties,
                            Properties& profileAttributesMap,
                            Properties& sessionAttributesMap,
                            Properties& responseAttributesMap) const;

    void getPolicyResult(const char * /*ssoToken*/,
			 const char * /*resName */,
			 const char * /*action Name*/,
			 const KeyValueMap & /*env*/,
			 am_map_t /*response*/,
			 am_policy_result_t * /*policy_result*/,
			 am_bool_t /*ignorePolicyResult*/,
			 Properties&  /*Agent Configuration properties*/);

private:
    std::vector<std::string> serverHandledAdvicesList;
    void construct_advice_string(const KeyValueMap &, std::string &) const;
    void add_attribute_value_pair_xml(const KeyValueMap::const_iterator &entry,
				      std::string &adviceStr) const;
    void reinitialize(); 


};

END_PRIVATE_NAMESPACE

#endif	// not __SERVICE_H__

