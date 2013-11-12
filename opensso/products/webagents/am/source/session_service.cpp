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
 * $Id: session_service.cpp,v 1.5 2009/03/23 22:58:07 subbae Exp $
 *
 */
/*
 * Portions Copyrighted 2012-2013 ForgeRock Inc
 */
#include "am.h"
#include "session_service.h"
#include "xml_tree.h"
#include "agent_profile_service.h"
#include <cstring>


USING_PRIVATE_NAMESPACE

namespace {
    const char requestPrefix[] = {
        "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
                "<RequestSet vers=\"1.0\" svcid=\"Session\" reqid=\""
    };
    
    // The first of the next two prefixes is used for the first session
    // request within a given request set.   The second is used for
    // subsequent requests within a given request set.
#define	SESSION_SERVICE_VERSION	"1.0"
#define	COMMON_SESSION_REQUEST_PREFIX_DATA \
	  "<Request><![CDATA[\n" \
	    "<SessionRequest vers=\"" SESSION_SERVICE_VERSION "\" reqid=\""
    const char firstSessionRequestPrefix[] = {
        "\">\n"
                COMMON_SESSION_REQUEST_PREFIX_DATA
    };
    
    const char additionalSessionRequestPrefix[] = {
        COMMON_SESSION_REQUEST_PREFIX_DATA
    };
    
    const char encodedAppSSOTokenPrefix[] = {
        "\" requester=\""
    };
    
    const char getSessionRequestPrefix[] = {
        "\">\n"
                "<GetSession reset=\""
    };
    
    const char getSessionRequestMiddle[] = {
        "\">\n"
                "<SessionID>"
    };
    
    const char getSessionRequestSuffix[] = {
        "</SessionID>\n"
                "</GetSession>\n"
                "</SessionRequest>]]>\n"
                "</Request>\n"
    };
    
    const char destroySessionRequestPrefix[] = {
        "\">\n"
                "<DestroySession"
    };
    
    const char destroySessionRequestMiddleDestroySessionID[] = {
        ">\n"
                "<DestroySessionID>"
    };
    
    const char destroySessionRequestMiddleSessionID[] = {
        "</DestroySessionID>"
                "<SessionID>"
    };
    
    const char destroySessionRequestSuffix[] = {
        "</SessionID>\n"
                "</DestroySession>\n"
                "</SessionRequest>]]>\n"
                "</Request>\n"
    };
    
    const char setPropertyRequestPrefix[] = {
        "\">\n"
                "<SetProperty>\n"
                "<SessionID>"
    };
    
    const char setPropertyRequestMiddlePropName[] = {
        "</SessionID>"
                "<Property name=\""
    };
    
    const char setPropertyRequestMiddlePropValue[] = {
        "\" value=\""
    };
    
    const char setPropertyRequestSuffix[] = {
        "\">\n"
                "</Property>\n"
                "</SetProperty>\n"
                "</SessionRequest>]]>\n"
                "</Request>\n"
    };
    
    const char addListenerRequestPrefix[] = {
        "\">\n"
                "<AddSessionListener>\n"
                "<URL>"
    };
    
    const char addListenerRequestMiddle[] = {
        "</URL>\n"
                "<SessionID>"
    };
    
    const char addListenerRequestSuffix[] = {
        "</SessionID>\n"
                "</AddSessionListener>\n"
                "</SessionRequest>]]>\n"
                "</Request>\n"
    };
    
    const char requestSuffix[] = {
        "</RequestSet>\n"
    };
    
    const char falseValue[] = "false";
    const char trueValue[] = "true";
    const char appSSOTokenMsg[] = "Application token passed in, is invalid.";
}

extern AgentProfileService* agentProfileService;

const SessionService::BodyChunk
SessionService::prefixChunk(requestPrefix, sizeof(requestPrefix) - 1);

const SessionService::BodyChunk
SessionService::firstSessionRequestPrefixChunk(firstSessionRequestPrefix,
        sizeof(firstSessionRequestPrefix) - 1);

const SessionService::BodyChunk
SessionService::additionalSessionRequestPrefixChunk(additionalSessionRequestPrefix,
        sizeof(additionalSessionRequestPrefix) - 1);

const SessionService::BodyChunk
SessionService::encodedAppSSOTokenPrefixChunk(encodedAppSSOTokenPrefix,
        sizeof(encodedAppSSOTokenPrefix) - 1);

const SessionService::BodyChunk
SessionService::getSessionRequestPrefixChunk(getSessionRequestPrefix,
        sizeof(getSessionRequestPrefix) - 1);

const SessionService::BodyChunk
SessionService::getSessionRequestMiddleChunk(getSessionRequestMiddle,
        sizeof(getSessionRequestMiddle) - 1);

const SessionService::BodyChunk
SessionService::getSessionRequestSuffixChunk(getSessionRequestSuffix,
        sizeof(getSessionRequestSuffix) - 1);

const SessionService::BodyChunk
SessionService::destroySessionRequestPrefixChunk(destroySessionRequestPrefix,
        sizeof(destroySessionRequestPrefix) - 1);

const SessionService::BodyChunk
SessionService::destroySessionRequestMiddleDestroySessionIDChunk(destroySessionRequestMiddleDestroySessionID,
        sizeof(destroySessionRequestMiddleDestroySessionID) - 1);

const SessionService::BodyChunk
SessionService::destroySessionRequestMiddleSessionIDChunk(destroySessionRequestMiddleSessionID,
        sizeof(destroySessionRequestMiddleSessionID) - 1);

const SessionService::BodyChunk
SessionService::destroySessionRequestSuffixChunk(destroySessionRequestSuffix,
        sizeof(destroySessionRequestSuffix) - 1);

const SessionService::BodyChunk
SessionService::setPropertyRequestPrefixChunk(setPropertyRequestPrefix,
        sizeof(setPropertyRequestPrefix) - 1);

const SessionService::BodyChunk
SessionService::setPropertyRequestMiddlePropNameChunk(setPropertyRequestMiddlePropName,
        sizeof(setPropertyRequestMiddlePropName) - 1);

const SessionService::BodyChunk
SessionService::setPropertyRequestMiddlePropValueChunk(setPropertyRequestMiddlePropValue,
        sizeof(setPropertyRequestMiddlePropValue) - 1);

const SessionService::BodyChunk
SessionService::setPropertyRequestSuffixChunk(setPropertyRequestSuffix,
        sizeof(setPropertyRequestSuffix) - 1);

const SessionService::BodyChunk
SessionService::addListenerRequestPrefixChunk(addListenerRequestPrefix,
        sizeof(addListenerRequestPrefix) - 1);

const SessionService::BodyChunk
SessionService::addListenerRequestMiddleChunk(addListenerRequestMiddle,
        sizeof(addListenerRequestMiddle) - 1);

const SessionService::BodyChunk
SessionService::addListenerRequestSuffixChunk(addListenerRequestSuffix,
        sizeof(addListenerRequestSuffix) - 1);

const SessionService::BodyChunk
SessionService::suffixChunk(requestSuffix, sizeof(requestSuffix) - 1);

const SessionService::BodyChunk
SessionService::falseChunk(falseValue, sizeof(falseValue) - 1);

const SessionService::BodyChunk
SessionService::trueChunk(trueValue, sizeof(trueValue) - 1);

SessionService::SessionService(const std::string &name,
        const Properties& props)
: BaseService(name, props) {
}

SessionService::~SessionService() {
}

am_status_t
SessionService::parseException(const XMLElement& element,
        const std::string& sessionId) const {
    am_status_t status;
    std::string exceptionMsg;
    
    if (element.getValue(exceptionMsg)) {
        std::string appSSOToken = agentProfileService->getAgentSSOToken();

        // The error message in an exception will potentially be localized.
        // By direct inspection of
        // com/iplanet/dpro/session/service/SessionService.java, however,
        // the two exceptions that indicate an invalid session are the only
        // ones that contain the SSOToken.
        //
        // This observation was true on March 29th, 2006, your mileage may
        // vary.
        if (exceptionMsg.find(sessionId) != std::string::npos) {
            Log::log(logModule, Log::LOG_DEBUG,
                    "SessionService::parseException() invalid session %s",
                    sessionId.c_str());
            status = AM_INVALID_SESSION;
        }  else if((appSSOToken.size() > 0 &&
               (exceptionMsg.find(appSSOToken) != std::string::npos)) ||
               (strncmp(exceptionMsg.c_str(), appSSOTokenMsg,
                   sizeof(appSSOTokenMsg) - 1) == 0)) {
            Log::log(logModule, Log::LOG_DEBUG,
                    "SessionService::parseException() "
            "invalid application ssotoken %s",
                    sessionId.c_str());
            status = AM_INVALID_APP_SSOTOKEN;
        } else {
            Log::log(logModule, Log::LOG_INFO,
                    "SessionService::parseException() server side error: %s",
                    exceptionMsg.c_str());
            status = AM_SESSION_FAILURE;
        }
    } else {
        Log::log(logModule, Log::LOG_INFO,
                "SessionService::parseException() server side error, no "
        "message in exception");
        status = AM_SESSION_FAILURE;
    }
    
    return status;
}

/* Throws
 *	XMLTree::ParseException on XML parse error
 */
am_status_t
SessionService::parseGetSessionResponse(XMLElement element,
        const std::string& sessionId,
        SessionInfo& sessionInfo) const {
    am_status_t status;
    
    if (element.isNamed("GetSession")) {
        element = element.getFirstSubElement();
        if (element.isNamed("Session")) {
            sessionInfo.parseXML(element);
            Log::log(logModule, Log::LOG_DEBUG,
                    "SessionService::parseGetSessionResponse() retrieved "
            "information for %s", sessionId.c_str());
            status = AM_SUCCESS;
        } else if (element.isNamed("Exception")) {
            status = parseException(element, sessionId);
        } else {
            std::string element_name;
            bool res = element.getName(element_name);
            if (!res) element_name = "unknown";
            Log::log(logModule, Log::LOG_DEBUG,
                    "SessionService::parseGetSessionResponse() "
            "Unexpected element %s in GetSessionProfile.",
                    element_name.c_str());
            element.log(logModule, Log::LOG_ERROR);
            throw XMLTree::ParseException("unexpected element in "
                    "GetSessionProfile");
        }
    } else {
        Log::log(logModule, Log::LOG_DEBUG,
                "SessionService::parseGetSessionResponse() "
        "Expected GetSession element not found in GetSessionProfile");
        element.log(logModule, Log::LOG_ERROR);
        throw XMLTree::ParseException("unexpected element in SessionResponse");
    }
    
    return status;
}

/* Throws
 *	XMLTree::ParseException on XML parse error
 */
am_status_t
SessionService::parseAddListenerResponse(XMLElement element,
        const std::string& sessionId) const {
    am_status_t status;
    
    if (element.isNamed("AddSessionListener")) {
        element = element.getFirstSubElement();
        if (element.isNamed("OK")) {
            Log::log(logModule, Log::LOG_DEBUG,
                    "SessionService::parseAddListenerResponse() server "
            "responded OK");
            status = AM_SUCCESS;
        } else if (element.isNamed("Exception")) {
            status = parseException(element, sessionId);
        } else {
            std::string element_name;
            bool res = element.getName(element_name);
            if (!res) element_name = "unknown";
            Log::log(logModule, Log::LOG_DEBUG,
                    "SessionService::parseAddListenerResopnse() "
            "Unexpected element %s in GetSessionProfile.",
                    element_name.c_str());
            element.log(logModule, Log::LOG_ERROR);
            throw XMLTree::ParseException("unexpected element in "
                    "AddSessionListener");
        }
    } else {
        element.log(logModule, Log::LOG_ERROR);
        throw XMLTree::ParseException("unexpected element in SessionResponse");
    }
    
    return status;
}

/* Throws
 *	XMLTree::ParseException on XML parse error
 */
am_status_t
SessionService::parseDestroySessionResponse(XMLElement element,
        const std::string& sessionId) const {
    am_status_t status;
    
    if (element.isNamed("DestroySession")) {
        element = element.getFirstSubElement();
        if (element.isNamed("OK")) {
            Log::log(logModule, Log::LOG_DEBUG,
                    "SessionService::parseDestroySessionResponse() %s OK ",
                    sessionId.c_str());
            status = AM_SUCCESS;
        } else if (element.isNamed("Exception")) {
            status = parseException(element, sessionId);
        } else {
            element.log(logModule, Log::LOG_ERROR);
            throw XMLTree::ParseException("unexpected element in "
                    "DestroySessionProfile");
        }
    } else {
        element.log(logModule, Log::LOG_ERROR);
        throw XMLTree::ParseException("unexpected element in SessionResponse");
    }
    
    return status;
}

/* Throws
 *	XMLTree::ParseException on XML parse error
 */
am_status_t
SessionService::parseSetPropertyResponse(XMLElement element,
        const std::string& sessionId) const {
    am_status_t status;
    
    if (element.isNamed("SetProperty")) {
        element = element.getFirstSubElement();
        if (element.isNamed("OK")) {
            Log::log(logModule, Log::LOG_DEBUG,
                    "SessionService::parseSetPropertyResponse() %s OK ",
                    sessionId.c_str());
            status = AM_SUCCESS;
        } else if (element.isNamed("Exception")) {
            status = parseException(element, sessionId);
        } else {
            element.log(logModule, Log::LOG_ERROR);
            throw XMLTree::ParseException("unexpected element in "
                    "SetPropertyProfile");
        }
    } else {
        element.log(logModule, Log::LOG_ERROR);
        throw XMLTree::ParseException("unexpected element in SessionResponse");
    }
    
    return status;
}

/* Throws
 *	XMLTree::ParseException on XML parse error
 */
am_status_t
SessionService::validateSession(const ServiceInfo& service,
        const SSOToken& ssoToken,
        const Http::CookieList& cookieList,
        bool resetIdleTimer,
        bool notificationEnabled,
        const std::string& notificationURL) {
    am_status_t status;
    SessionInfo sessionInfo;
    
    status = getSessionInfo(service, ssoToken.getString(), cookieList,
            resetIdleTimer, notificationEnabled, notificationURL, sessionInfo);
    if (AM_SUCCESS == status && ! sessionInfo.isValid()) {
        status = AM_INVALID_SESSION;
    }
    
    return status;
}

/* Throws
 *	XMLTree::ParseException on XML parse error
 */
am_status_t
SessionService::getSessionInfo(const ServiceInfo& service,
        const std::string& ssoToken,
        const Http::CookieList& cookieList,
        bool resetIdleTimer,
        bool notificationEnabled,
        const std::string& notificationURL,
        SessionInfo& sessionInfo) {
    am_status_t status = AM_FAILURE;
    
    // XXX remove.
    Log::log(logModule, Log::LOG_INFO,
            "Getting session info for %s.", ssoToken.c_str());
    
    if (ssoToken.size() > 0) {
        const std::size_t NUM_EXTRA_CHUNKS_WITHOUT_LISTENER = 6;
        const std::size_t NUM_EXTRA_CHUNKS_WITH_LISTENER =
        (NUM_EXTRA_CHUNKS_WITHOUT_LISTENER + 7);
        Request request(*this, prefixChunk, firstSessionRequestPrefixChunk,
                (notificationURL.size() > 0 ?
                    NUM_EXTRA_CHUNKS_WITH_LISTENER :
                    NUM_EXTRA_CHUNKS_WITHOUT_LISTENER));
        Http::Response response;
        BodyChunk ssoTokenChunk(ssoToken);
        char addListenerRequestId[Request::ID_BUF_LEN];
        BodyChunkList& bodyChunkList = request.getBodyChunkList();
        
        std::string encodedAppSSOToken =
                agentProfileService->getEncodedAgentSSOToken();
        BodyChunk encodedAppSSOTokenChunk(encodedAppSSOToken);
        bodyChunkList.push_back(encodedAppSSOTokenPrefixChunk);
        if (encodedAppSSOToken.size() > 0) {
            bodyChunkList.push_back(encodedAppSSOTokenChunk);
        }
        
        bodyChunkList.push_back(getSessionRequestPrefixChunk);
        bodyChunkList.push_back(resetIdleTimer ? trueChunk : falseChunk);
        bodyChunkList.push_back(getSessionRequestMiddleChunk);
        bodyChunkList.push_back(ssoTokenChunk);
        bodyChunkList.push_back(getSessionRequestSuffixChunk);
        
        if (notificationEnabled && notificationURL.size() > 0) {
            BodyChunk temp;
            
            bodyChunkList.push_back(additionalSessionRequestPrefixChunk);
            request.getNextServiceRequestIdAsString(
                    addListenerRequestId,
                    sizeof(addListenerRequestId));
            temp.data = addListenerRequestId;
            bodyChunkList.push_back(temp);
            
            bodyChunkList.push_back(encodedAppSSOTokenPrefixChunk);
            if (encodedAppSSOToken.size() > 0) {
                bodyChunkList.push_back(encodedAppSSOTokenChunk);
            }
            
            bodyChunkList.push_back(addListenerRequestPrefixChunk);
            bodyChunkList.push_back(BodyChunk(notificationURL));
            bodyChunkList.push_back(addListenerRequestMiddleChunk);
            bodyChunkList.push_back(ssoTokenChunk);
            bodyChunkList.push_back(addListenerRequestSuffixChunk);
        }
        
        bodyChunkList.push_back(suffixChunk);
        
        status = doHttpPost(service, std::string(), cookieList,
                bodyChunkList, response);
        if (AM_SUCCESS == status) {
            try {
                std::vector<std::string> sessionResponses;
                std::string sessionData;
                
                sessionResponses = parseGenericResponse(response,
                        request.getGlobalId());
                
                if (sessionResponses.empty()) {
                    Log::log(logModule, Log::LOG_ERROR,
                            "SessionService::getSessionInfo() empty response.");
                    throw XMLTree::ParseException("Session Service returned "
                            "an empty ResponseSet");
                }
                
                XMLTree::Init xt;
                for (std::size_t i = 0;
                i < sessionResponses.size() && AM_SUCCESS == status;
                ++i) {
                    XMLTree sessionTree(false,
                            sessionResponses[i].c_str(),
                            sessionResponses[i].size());
                    XMLElement element = sessionTree.getRootElement();
                    std::string version;
                    
                    if (element.isNamed("SessionResponse") &&
                    element.getAttributeValue("vers", version) &&
                    std::strcmp(version.c_str(),
                            SESSION_SERVICE_VERSION) == 0) {
                        std::string requestId;
                        
                        if (! element.getAttributeValue("reqid", requestId)) {
                            Log::log(logModule, Log::LOG_ERROR,
                                    "SessionService::getSessionInfo() "
                            "Missing reqid attribute in response.");
                            throw XMLTree::ParseException("no request id "
                                    "in Session Service "
                                    "data");
                        }
                        
                        XMLElement subElement = element.getFirstSubElement();
                        
                        if (std::strcmp(requestId.c_str(),
                                request.getServiceId()) == 0) {
                            status = parseGetSessionResponse(subElement,
                                    ssoToken,
                                    sessionInfo);
                        } else if (notificationEnabled &&
                                notificationURL.size() > 0 &&
                                std::strcmp(requestId.c_str(),
                                addListenerRequestId) == 0) {
                            status = parseAddListenerResponse(subElement,
                                    ssoToken);
                        } else {
                            Log::log(logModule, Log::LOG_ERROR,
                                    "SessionService::getSessionInfo() "
                            "Unknown request id in response data.");
                            throw XMLTree::ParseException("unknown request id "
                                    "in Session Service "
                                    "data");
                        }
                    } else {
                        element.log(logModule, Log::LOG_ERROR);
                        if (element.isNamed("SessionResponse")) {
                            Log::log(logModule, Log::LOG_ERROR,
                                    "SessionService::getSessionInfo() "
                            "version mismatch.");
                            throw XMLTree::ParseException("version mismatch "
                                    "in Session Service "
                                    "data");
                        } else {
                            Log::log(logModule, Log::LOG_ERROR,
                                    "SessionService::getSessionInfo() "
                            "unexpected element in response.");
                            throw XMLTree::ParseException("unexpected element "
                                    "in Session Service "
                                    "data");
                        }
                    }
                }
            } catch (const XMLTree::ParseException& exc) {
                Log::log(logModule, Log::LOG_ERROR,
                        "SessionService::getSessionInfo() caught exception: %s",
                        exc.getMessage().c_str());
                status = AM_ERROR_PARSING_XML;
            }
        }
    } else {
        Log::log(logModule, Log::LOG_ERROR,
                "SessionService::getSessionInfo() Invalid Argument");
        status = AM_INVALID_ARGUMENT;
    }
    return status;
}

/* Throws
 *	XMLTree::ParseException on XML parse error
 */
am_status_t
SessionService::destroySession(const ServiceInfo& service,
        const std::string& ssoToken,
        const Http::CookieList& cookieList) {
    if (ssoToken.size()==0)
        return AM_INVALID_ARGUMENT;
    
    am_status_t status;
    const std::size_t DESTROY_SESSION_NUM_CHUNKS = 8;
    Request request(*this, prefixChunk, firstSessionRequestPrefixChunk,
            DESTROY_SESSION_NUM_CHUNKS);
    Http::Response response;
    BodyChunk ssoTokenChunk(ssoToken);
    BodyChunkList& bodyChunkList = request.getBodyChunkList();
    
    std::string encodedAppSSOToken =
            agentProfileService->getEncodedAgentSSOToken();
    BodyChunk encodedAppSSOTokenChunk(encodedAppSSOToken);
    bodyChunkList.push_back(encodedAppSSOTokenPrefixChunk);
    if (encodedAppSSOToken.size() > 0) {
        bodyChunkList.push_back(encodedAppSSOTokenChunk);
    }
    
    bodyChunkList.push_back(destroySessionRequestPrefixChunk);
    bodyChunkList.push_back(destroySessionRequestMiddleDestroySessionIDChunk);
    bodyChunkList.push_back(ssoTokenChunk);
    bodyChunkList.push_back(destroySessionRequestMiddleSessionIDChunk);
    bodyChunkList.push_back(ssoTokenChunk);
    bodyChunkList.push_back(destroySessionRequestSuffixChunk);
    bodyChunkList.push_back(suffixChunk);
    
    status = doHttpPost(service, std::string(), cookieList,
            bodyChunkList, response);
    if (AM_SUCCESS == status) {
        try {
            std::vector<std::string> sessionResponses;
            std::string sessionData;
            
            sessionResponses = parseGenericResponse(response,
                    request.getGlobalId());
            
            if (sessionResponses.empty()) {
                Log::log(logModule, Log::LOG_ERROR,
                        "SessionService::destroySession() received empty "
                "responses.");
                throw XMLTree::ParseException(
                        "Session Service destroy session returned "
                        "an empty ResponseSet");
            }
            
            XMLTree::Init xt;
            for (std::size_t i = 0;
            i < sessionResponses.size() && AM_SUCCESS == status;
            ++i) {
                XMLTree sessionTree(false,
                        sessionResponses[i].c_str(),
                        sessionResponses[i].size());
                XMLElement element = sessionTree.getRootElement();
                std::string version;
                
                if (element.isNamed("SessionResponse") &&
                element.getAttributeValue("vers", version) &&
                std::strcmp(version.c_str(),
                        SESSION_SERVICE_VERSION) == 0) {
                    std::string requestId;
                    
                    if (! element.getAttributeValue("reqid", requestId)) {
                        throw XMLTree::ParseException("no request id "
                                "in Session Service "
                                "data");
                    }
                    
                    XMLElement subElement = element.getFirstSubElement();
                    
                    if (std::strcmp(requestId.c_str(),
                            request.getServiceId()) == 0) {
                        status = parseDestroySessionResponse(subElement,
                                ssoToken);
                    } else {
                        throw XMLTree::ParseException("unknown request id "
                                "in Session Service "
                                "data");
                    }
                } else {
                    element.log(logModule, Log::LOG_ERROR);
                    if (element.isNamed("SessionResponse")) {
                        throw XMLTree::ParseException("version mismatch "
                                "in Session Service "
                                "data");
                    } else {
                        throw XMLTree::ParseException("unexpected element "
                                "in Session Service "
                                "data");
                    }
                }
            }
        } catch (const XMLTree::ParseException& exc) {
            Log::log(logModule, Log::LOG_ERROR,
                    "SessionService::destroySessionInfo() caught exception: %s",
                    exc.getMessage().c_str());
            status = AM_ERROR_PARSING_XML;
        }
    }
    return status;
}


/* Throws
 *	XMLTree::ParseException on XML parse error
 */
am_status_t
SessionService::setProperty(const ServiceInfo& service,
        SessionInfo& sessionInfo,
        const std::string& name,
        const std::string& value,
        const Http::CookieList& cookieList) {
    SSOToken& ssoTok = sessionInfo.getSSOToken();
    const std::string& ssoToken = ssoTok.getString();
    
    am_status_t status;
    const std::size_t SET_PROPERTY_NUM_CHUNKS = 9;
    Request request(*this, prefixChunk, firstSessionRequestPrefixChunk,
            SET_PROPERTY_NUM_CHUNKS);
    Http::Response response;
    BodyChunk ssoTokenChunk(ssoToken);
    BodyChunk nameChunk(name);
    BodyChunk valueChunk(value);
    
    BodyChunkList& bodyChunkList = request.getBodyChunkList();
    
    std::string encodedAppSSOToken =
            agentProfileService->getEncodedAgentSSOToken();
    BodyChunk encodedAppSSOTokenChunk(encodedAppSSOToken);
    bodyChunkList.push_back(encodedAppSSOTokenPrefixChunk);
    if (encodedAppSSOToken.size() > 0) {
        bodyChunkList.push_back(encodedAppSSOTokenChunk);
    }
    
    /* set property request */
    bodyChunkList.push_back(setPropertyRequestPrefixChunk);
    bodyChunkList.push_back(ssoTokenChunk);
    bodyChunkList.push_back(setPropertyRequestMiddlePropNameChunk);
    bodyChunkList.push_back(nameChunk);
    bodyChunkList.push_back(setPropertyRequestMiddlePropValueChunk);
    bodyChunkList.push_back(valueChunk);
    bodyChunkList.push_back(setPropertyRequestSuffixChunk);
    
    /* tag along a get session request to update session info */
    char getSessionRequestId[Request::ID_BUF_LEN];
    BodyChunk getSessionRequestIdChunk;
    bodyChunkList.push_back(additionalSessionRequestPrefixChunk);
    request.getNextServiceRequestIdAsString(getSessionRequestId,
            sizeof(getSessionRequestId));
    getSessionRequestIdChunk.data = getSessionRequestId;
    bodyChunkList.push_back(getSessionRequestIdChunk);
    
    bodyChunkList.push_back(encodedAppSSOTokenPrefixChunk);
    if (encodedAppSSOToken.size() > 0) {
        bodyChunkList.push_back(encodedAppSSOTokenChunk);
    }
    
    bodyChunkList.push_back(getSessionRequestPrefixChunk);
    bodyChunkList.push_back(trueChunk); // set prop always resets idle time
    bodyChunkList.push_back(getSessionRequestMiddleChunk);
    bodyChunkList.push_back(ssoTokenChunk);
    bodyChunkList.push_back(getSessionRequestSuffixChunk);
    
    bodyChunkList.push_back(suffixChunk);
    
    status = doHttpPost(service, std::string(), cookieList,
            bodyChunkList, response);
    if (AM_SUCCESS == status) {
        try {
            std::vector<std::string> sessionResponses;
            std::string sessionData;
            
            sessionResponses = parseGenericResponse(response,
                    request.getGlobalId());
            
            if (sessionResponses.empty()) {
                Log::log(logModule, Log::LOG_ERROR,
                        "SessionService::setProperty() received empty "
                "responses.");
                throw XMLTree::ParseException(
                        "Session Service set property returned "
                        "an empty ResponseSet");
            }
            
            XMLTree::Init xt;
            for (std::size_t i = 0;
            i < sessionResponses.size() && AM_SUCCESS == status;
            ++i) {
                XMLTree sessionTree(false,
                        sessionResponses[i].c_str(),
                        sessionResponses[i].size());
                XMLElement element = sessionTree.getRootElement();
                std::string version;
                
                if (element.isNamed("SessionResponse") &&
                element.getAttributeValue("vers", version) &&
                std::strcmp(version.c_str(),
                        SESSION_SERVICE_VERSION) == 0) {
                    std::string requestId;
                    
                    if (! element.getAttributeValue("reqid", requestId)) {
                        throw XMLTree::ParseException("no request id "
                                "in Session Service "
                                "data");
                    }
                    
                    XMLElement subElement = element.getFirstSubElement();
                    
                    if (std::strcmp(requestId.c_str(),
                            request.getServiceId()) == 0) {
                        status = parseSetPropertyResponse(subElement, ssoToken);
                    } else if (std::strcmp(requestId.c_str(),
                            getSessionRequestId) == 0) {
                        status = parseGetSessionResponse(subElement,
                                ssoToken,
                                sessionInfo);
                    } else {
                        throw XMLTree::ParseException("unknown request id "
                                "in Session Service "
                                "data");
                    }
                } else {
                    element.log(logModule, Log::LOG_ERROR);
                    if (element.isNamed("SessionResponse")) {
                        throw XMLTree::ParseException("version mismatch "
                                "in Session Service "
                                "data");
                    } else {
                        throw XMLTree::ParseException("unexpected element "
                                "in Session Service "
                                "data");
                    }
                }
            }
        } catch (const XMLTree::ParseException& exc) {
            Log::log(logModule, Log::LOG_ERROR,
                    "SessionService::setProperty() caught exception: %s",
                    exc.getMessage().c_str());
            status = AM_ERROR_PARSING_XML;
        }
    }
    return status;
}


/* Throws
 *	XMLTree::ParseException on XML parse error
 */
am_status_t
SessionService::addListener(const ServiceInfo& service,
        const std::string& ssoToken,
        const Http::CookieList& cookieList,
        const std::string& notificationURL) {
    am_status_t status = AM_FAILURE;
    
    if (ssoToken.size() <= 0 || service.getNumberOfServers() <= 0 ||
            notificationURL.size() <= 0) {
        Log::log(logModule, Log::LOG_ERROR,
                "SessionService::addListener() "
        "One or more input parameters invalid.");
        status = AM_INVALID_ARGUMENT;
    }
    else {
        const std::size_t ADD_LISTENER_NUM_CHUNKS = 7;
        Request request(*this, prefixChunk, firstSessionRequestPrefixChunk,
                ADD_LISTENER_NUM_CHUNKS);
        Http::Response response;
        BodyChunk ssoTokenChunk(ssoToken);
        BodyChunkList& bodyChunkList = request.getBodyChunkList();
        
        std::string encodedAppSSOToken =
                agentProfileService->getEncodedAgentSSOToken();
        BodyChunk encodedAppSSOTokenChunk(encodedAppSSOToken);
        bodyChunkList.push_back(encodedAppSSOTokenPrefixChunk);
        if (encodedAppSSOToken.size() > 0) {
            bodyChunkList.push_back(encodedAppSSOTokenChunk);
        }
        
        bodyChunkList.push_back(addListenerRequestPrefixChunk);
        bodyChunkList.push_back(BodyChunk(notificationURL));
        bodyChunkList.push_back(addListenerRequestMiddleChunk);
        bodyChunkList.push_back(ssoTokenChunk);
        bodyChunkList.push_back(addListenerRequestSuffixChunk);
        bodyChunkList.push_back(suffixChunk);
        
        status = doHttpPost(service, std::string(), cookieList,
                bodyChunkList, response);
        if (AM_SUCCESS == status) {
            try {
                std::vector<std::string> sessionResponses;
                std::string sessionData;
                
                sessionResponses = parseGenericResponse(response,
                        request.getGlobalId());
                
                if (sessionResponses.empty()) {
                    Log::log(logModule, Log::LOG_ERROR,
                            "SessionService::addListener() empty response.");
                    throw XMLTree::ParseException("Session Service returned "
                            "an empty ResponseSet");
                }
                
                XMLTree::Init xt;
                for (std::size_t i = 0;
                i < sessionResponses.size() && AM_SUCCESS == status;
                ++i) {
                    XMLTree sessionTree(false,
                            sessionResponses[i].c_str(),
                            sessionResponses[i].size());
                    XMLElement element = sessionTree.getRootElement();
                    std::string version;
                    
                    if (!element.isValid()) {
                        Log::log(logModule, Log::LOG_ERROR,
                            "SessionService::addListener() invalid response (%d): %s", i, sessionResponses[i].c_str());
                        throw XMLTree::ParseException("Session Service returned an invalid ResponseSet");
                    } 
                    
                    if (element.isNamed("SessionResponse") &&
                    element.getAttributeValue("vers", version) &&
                    std::strcmp(version.c_str(),
                            SESSION_SERVICE_VERSION) == 0) {
                        std::string requestId;
                        
                        if (! element.getAttributeValue("reqid", requestId)) {
                            Log::log(logModule, Log::LOG_ERROR,
                                    "SessionService::addListener() "
                            "Missing reqid attribute in response.");
                            throw XMLTree::ParseException("no request id "
                                    "in Session Service "
                                    "data");
                        }
                        
                        XMLElement subElement = element.getFirstSubElement();
                        
                        if (std::strcmp(requestId.c_str(),
                                request.getServiceId()) == 0) {
                            status = parseAddListenerResponse(subElement,
                                    ssoToken);
                        } else {
                            Log::log(logModule, Log::LOG_ERROR,
                                    "SessionService::addListener() "
                            "Unknown request id in response data.");
                            throw XMLTree::ParseException("unknown request id "
                                    "in Session Service "
                                    "data");
                        }
                    } else {
                        element.log(logModule, Log::LOG_ERROR);
                        if (element.isNamed("SessionResponse")) {
                            Log::log(logModule, Log::LOG_ERROR,
                                    "SessionService::addListener() "
                            "version mismatch.");
                            throw XMLTree::ParseException("version mismatch "
                                    "in Session Service "
                                    "data");
                        } else {
                            Log::log(logModule, Log::LOG_ERROR,
                                    "SessionService::addListener() "
                            "unexpected element in response.");
                            throw XMLTree::ParseException("unexpected element "
                                    "in Session Service "
                                    "data");
                        }
                    }
                }
            } catch (const XMLTree::ParseException& exc) {
                Log::log(logModule, Log::LOG_ERROR,
                        "SessionService::addListener() caught exception: %s",
                        exc.getMessage().c_str());
                status = AM_ERROR_PARSING_XML;
            }
        }
    }
    return status;
}

