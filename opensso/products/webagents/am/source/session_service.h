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
 * $Id: session_service.h,v 1.4 2008/06/25 08:14:37 qcheng Exp $
 *
 * Abstract:
 *
 * Service interface class for the DSAME "Session" service.
 *
 */
/*
 * Portions Copyrighted 2013 ForgeRock Inc
 */

#ifndef SESSION_SERVICE_H
#define SESSION_SERVICE_H

#include "base_service.h"
#include "internal_macros.h"
#include "sso_token.h"
#include "session_info.h"
#include "xml_element.h"

BEGIN_PRIVATE_NAMESPACE

class SessionService: public BaseService {
public:
    /**
     * All functions throw XMLTree::ParseException on XML parse error 
     */
    SessionService(const std::string &name,
                const Properties& props);
    virtual ~SessionService();

    am_status_t validateSession(const ServiceInfo& service,
				   const SSOToken& ssoToken,
				   const Http::CookieList& cookieList = Http::CookieList(),
				   bool resetIdleTimer = false,
				   bool notificationEnabled = false,
				   const std::string& notificationURL = std::string());

    am_status_t getSessionInfo(const ServiceInfo& service,
				  const std::string& ssoToken,
				  const Http::CookieList& cookieList,
				  bool resetIdleTimer,
				  bool notificationEnabled,
				  const std::string& notificationURL,
				  SessionInfo& sessionInfo);

    am_status_t destroySession(const ServiceInfo& service,
			       const std::string& ssoToken,
			       const Http::CookieList& cookieList);

    am_status_t setProperty(const ServiceInfo& service,
			    SessionInfo& sessionInfo,
                            const std::string& name, 
                            const std::string& value, 
			    const Http::CookieList& cookieList);

    am_status_t addListener(const ServiceInfo& service,
				  const std::string& ssoToken,
				  const Http::CookieList& cookieList,
				  const std::string& notificationURL);

private:
    am_status_t parseAddListenerResponse(XMLElement element,
					    const std::string& sessionId)const;

    am_status_t parseException(const XMLElement& element,
				  const std::string& sessionId) const;

    am_status_t parseGetSessionResponse(XMLElement element,
					   const std::string& sessionId,
					   SessionInfo& sessionInfo) const;

    am_status_t parseDestroySessionResponse(XMLElement element,
					   const std::string& sessionId) const;

    am_status_t parseSetPropertyResponse(XMLElement element,
					 const std::string& sessionId) const;

    static const BodyChunk prefixChunk;
    static const BodyChunk firstSessionRequestPrefixChunk;
    static const BodyChunk additionalSessionRequestPrefixChunk;
    static const BodyChunk encodedAppSSOTokenPrefixChunk;
    static const BodyChunk getSessionRequestPrefixChunk;
    static const BodyChunk getSessionRequestMiddleChunk;
    static const BodyChunk getSessionRequestSuffixChunk;
    static const BodyChunk addListenerRequestPrefixChunk;
    static const BodyChunk addListenerRequestMiddleChunk;
    static const BodyChunk addListenerRequestSuffixChunk;
    static const BodyChunk destroySessionRequestPrefixChunk;
    static const BodyChunk destroySessionRequestMiddleDestroySessionIDChunk;
    static const BodyChunk destroySessionRequestMiddleSessionIDChunk;
    static const BodyChunk destroySessionRequestSuffixChunk;
    static const BodyChunk setPropertyRequestPrefixChunk;
    static const BodyChunk setPropertyRequestMiddlePropNameChunk;
    static const BodyChunk setPropertyRequestMiddlePropValueChunk;
    static const BodyChunk setPropertyRequestSuffixChunk;
    static const BodyChunk suffixChunk;
    static const BodyChunk falseChunk;
    static const BodyChunk trueChunk;

};

END_PRIVATE_NAMESPACE

#endif	// not SESSION_SERVICE_H
