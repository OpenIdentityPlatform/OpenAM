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
 * $Id: auth_context.h,v 1.4 2008/06/25 08:14:31 qcheng Exp $
 *
 */ 
#ifndef _AUTH_CONTEXT_H_
#define _AUTH_CONTEXT_H_
#include <am_auth.h>
#include "internal_macros.h"
#include "service_info.h"

BEGIN_PRIVATE_NAMESPACE

class AuthService;

class AuthContext {
 public:
	  
    AuthContext():
	authStatus(AM_AUTH_STATUS_IN_PROGRESS),     
	orgName(""), certNickName(""), namingURL("") {
    }

    AuthContext(std::string &oName,
		std::string &certName,
		std::string &nURL): 
	authStatus(AM_AUTH_STATUS_IN_PROGRESS),
	orgName(oName), certNickName(certName), namingURL(nURL) {

    }

    ~AuthContext() {
	cleanupCallbacks();
    }

    inline const std::string &getOrganizationName() { return orgName; }

    inline const std::string &getSSOToken() {return ssoToken; }

    inline am_auth_status_t getStatus() { return authStatus; }

    inline std::vector<am_auth_callback_t> &getRequirements() {
	return callbacks; }

    void cleanupCallbacks();

    inline bool hasMoreRequirements() {
        return (callbacks.size() > 0);
    }

    friend class AuthService;

 private:
    am_auth_status_t authStatus;
    std::string orgName; // input parameter takes precedence over property
    std::string certNickName; // input parameter, BaseService has property
    std::string namingURL; // input parameter takes precedence over property
    ServiceInfo authSvcInfo; // contains auth URL(s) for each auth context
    std::string ssoToken;
    std::string authIdentifier;
    std::string subject;
    std::vector<am_auth_callback_t> callbacks;
    void cleanupCharArray(const char* &char_array);
    void cleanupStringList(const char** &string_list, size_t list_size);
    friend class AgentProfileService;

};

END_PRIVATE_NAMESPACE
#endif
