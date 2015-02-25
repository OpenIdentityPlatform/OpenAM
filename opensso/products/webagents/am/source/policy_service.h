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
 * $Id: policy_service.h,v 1.4 2008/06/25 08:14:35 qcheng Exp $
 *
 */
/*
 * Portions Copyrighted 2013 ForgeRock Inc
 */

#ifndef POLICY_SERVICE_H
#define POLICY_SERVICE_H

#include <cstring>
#include <list>
#include <string>

#include "base_service.h"
#include "internal_macros.h"
#include "key_value_map.h"
#include "session_info.h"
#include "sso_token.h"
#include "xml_tree.h"
#include "xml_element.h"

BEGIN_PRIVATE_NAMESPACE
typedef enum policy_fetch {
    SCOPE_SELF = 0,
    SCOPE_STRICT_SUBTREE,
    SCOPE_SUBTREE,
    SCOPE_RESPONSE_ATTRIBUTE_ONLY
} policy_fetch_scope_t;

class PolicyService: public BaseService {
public:
    /* Throws std::invalid_argument if any argument is invalid */
    PolicyService(const SSOToken& agentToken, 
                const Properties& props);
    virtual ~PolicyService();

    am_status_t getPolicyDecisions(const ServiceInfo& service,
				      const SSOToken& userToken,
				      const Http::CookieList& cookieList,
				      const SessionInfo& sessionInfo,
				      const std::string& serviceName,
				      const std::string& resourceName,
				      policy_fetch_scope_t scope,
				      const KeyValueMap& envParameters,
				      const std::list<std::string> &attrs,
				      std::string& policyData);

    /* Throws 
     *	std::invalid_argument if any argument is invalid 
     *	XMLTree::ParseException on XML parse error 
     */
    void sendNotificationMsg(bool addOrRemove,
			     const ServiceInfo &service,
			     const std::string &serviceName,
			     const Http::CookieList &cookieList,
			     const std::string &notificationURL);

    void getAdvicesList(const ServiceInfo &serviceInfo,
			   const std::string &serviceName,
			   const Http::CookieList &cookieList,
			   KeyValueMap &adviceMap);

    int getRevisionNumber() {
	return revision;
    }

private:
    /* Throws std::invalid_argument if any argument is invalid */
    std::size_t addAttribute(char *buffer, std::size_t bufferLen,
			     std::size_t offset,
			     const std::string& attribute) const;

    /* Throws std::invalid_argument if any argument is invalid */
    std::size_t addValue(char *buffer, std::size_t bufferLen,
			 std::size_t offset, const std::string& value) const;

    /* Throws std::invalid_argument if any argument is invalid */
    std::size_t
    addAttributeValuePair(char *buffer,
			  std::size_t bufferLen,
			  std::size_t offset,
			  const std::string& attribute,
			  const KeyValueMap::mapped_type& values) const;

    am_status_t parseException(const XMLElement& element) const;

    am_status_t parseListenerResponse(bool addOrRemove,
					 const XMLElement& element) const;

    am_status_t parseAdviceListResponse(const XMLElement &subElement,
					KeyValueMap &adviceMap);


    am_status_t
    parseGetPolicyResultsResponse(const XMLElement& element) const;

    void addEnvParameters(const KeyValueMap& envParameters,
			  Request& request) const;

    static const BodyChunk prefixChunk;
    static const BodyChunk firstPolicyServicePrefixChunk;
    static const BodyChunk additionalPolicyServicePrefixChunk;
    static const BodyChunk appSSOTokenPrefixChunk;
    static const BodyChunk addListenerRequestPrefixChunk;
    static const BodyChunk removeListenerRequestPrefixChunk;
    static const BodyChunk serviceNamePrefixChunk;
    static const BodyChunk addOrRemoveListenerRequestSuffixChunk;
    static const BodyChunk adviceListRequestChunk;
    static const BodyChunk getResourceResultsPrefixChunk;
    static const BodyChunk userSSOTokenPrefixChunk;
    static const BodyChunk resourceNamePrefixChunk;
    static const BodyChunk resourceScopePrefixChunk;
    static const BodyChunk envParametersPrefixChunk;
    static const BodyChunk envParametersSuffixChunk;
    static const BodyChunk getResponseDecisionsPrefixChunk;
    static const BodyChunk attributePrefixChunk;
    static const BodyChunk attributeSuffixChunk;
    static const BodyChunk getResourceResultsSuffixWithRDChunk;
    static const BodyChunk getResourceResultsSuffixNoRDChunk;
    static const BodyChunk suffixChunk;
    static const BodyChunk selfChunk;
    static const BodyChunk subtreeChunk;

    SSOToken agentToken;
    int revision;
};

END_PRIVATE_NAMESPACE

#endif	// not POLICY_SERVICE_H
