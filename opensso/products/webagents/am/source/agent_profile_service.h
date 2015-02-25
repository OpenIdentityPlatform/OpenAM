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
 * $Id: agent_profile_service.h,v 1.13 2009/08/07 21:08:24 subbae Exp $
 *
 */

#ifndef AGENT_PROFILE_SERVICE_H
#define AGENT_PROFILE_SERVICE_H

#include "base_service.h"
#include "internal_macros.h"
#include "naming_info.h"
#include "naming_service.h"
#include "sso_token.h"
#include "service_info.h"

#include "thread_pool.h"
#include "auth_svc.h"
#include "agent_configuration.h"
#include "agent_config_cache.h"
#include "am_web.h"
#include "utils.h"
#include "sso_token_service.h"


BEGIN_PRIVATE_NAMESPACE

/*
* This service does the following functions:
* 1. Initial naming request to determine rest service url
* 2. Agent authentication.
* 3. Fetches agent profile properties using REST attribute service.
* 4. Agent logout
* 5. Agent appSSOToken validation
*/


class AgentProfileService: public BaseService {
public:
    explicit AgentProfileService(const Properties& props, Utils::boot_info_t boot_info_prop);
    virtual ~AgentProfileService();

    am_status_t getAgentAttributes(const std::string ssoToken,  
            const std::string sharedAgentProfileName,
            const std::string realmName,
            am_properties_t properties);
    am_status_t agentLogin();

    am_status_t agentLogout(const Properties &config); 
    am_status_t isRESTServiceAvailable();
    
    inline void setRepoType(std::string repType) {
        repositoryType = repType;
    }
    
    inline std::string getRepoType() {
        return repositoryType;
    }

    std::string getAgentSSOToken() {
        return agentSSOToken;
    }

    std::string getEncodedAgentSSOToken() {
        return encodedAgentSSOToken;
    }

    am_status_t fetchAndUpdateAgentConfigCache();
    am_status_t fetchAndUpdateAgentConfigCacheInternal(
                                     AgentConfigurationRefCntPtr& agentConfig);
    void deleteOldAgentConfigInstances();
    AgentConfigurationRefCntPtr getAgentConfigInstance(am_status_t &status);
    am_status_t validateAgentSSOToken(); 


private:

    ServiceInfo mRestSvcInfo; 
    ServiceInfo mAuthSvcInfo; 
    std::string mRestURL;
    std::string mAuthURL;
    std::string mNamingServiceURL;
    ServiceInfo mNamingServiceInfo;
    NamingService mNamingService;
    AuthContext mAuthCtx;
    bool agentAuthnd;
    std::string agentSSOToken; 
    std::string repositoryType;
    AgentConfigCache agentConfigCache;
    Utils::boot_info_t boot_info;
    std::string encodedAgentSSOToken; 

    Utils::url_info_list_t not_enforced_list_c;

    void setRestSvcInfo(std::string restURL);
    void setAuthSvcInfo(std::string restURL);

    am_status_t parseAgentResponse( const std::string xmlResponse,  
                           am_properties_t properties);
    void parseURL(std::string serviceURL, 
                                       bool isRestURL,
                                       std::string &parsedServiceURL);
    bool isListMapProperty(const char* propName); 
    void setEncodedAgentSSOToken(std::string agentSSOToken);

};

END_PRIVATE_NAMESPACE

#endif	/* not AGENT_PROFILE_SERVICE_H */
