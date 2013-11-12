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
 * $Id: naming_service.cpp,v 1.12 2008/08/25 21:00:23 madan_ranganath Exp $
 *
 */ 
/*
 * Portions Copyrighted 2012-2013 ForgeRock Inc
 */

#include <sstream>
#include "naming_service.h"
#include "xml_tree.h"

USING_PRIVATE_NAMESPACE

namespace {
    const char requestPrefix[] = {
	"<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
	"<RequestSet vers=\"1.0\" svcid=\"com.iplanet.am.naming\" reqid=\""
    };

    const char namingRequestPrefix[] = {
	"\">\n"
	  "<Request><![CDATA[\n"
	    "<NamingRequest vers=\"3.0\" reqid=\""
    };

    const char sessidPrefix[] = {
	    "\" sessid=\""
    };

    const char preferredNamingPrefix[] = {
	    "\" preferredNamingURL=\""
    };

    const char requestSuffix[] = {
	      "\">\n"
	      "<GetNamingProfile>\n</GetNamingProfile>\n"
	    "</NamingRequest>]]>\n"
	  "</Request>\n"
	"</RequestSet>\n"
    };
    const std::size_t MAX_PORT_LENGTH = 5;
}

const NamingService::BodyChunk
NamingService::prefixChunk(requestPrefix, sizeof(requestPrefix) - 1);

const NamingService::BodyChunk
NamingService::namingPrefixChunk(namingRequestPrefix,
				 sizeof(namingRequestPrefix) - 1);

const NamingService::BodyChunk
NamingService::sessidPrefixChunk(sessidPrefix, sizeof(sessidPrefix) - 1);

const NamingService::BodyChunk
NamingService::preferredNamingPrefixChunk(preferredNamingPrefix, sizeof(preferredNamingPrefix) - 1);

const NamingService::BodyChunk
NamingService::suffixChunk(requestSuffix, sizeof(requestSuffix) - 1);

const std::string NamingService::loggingAttribute("iplanet-am-naming-logging-"
						  "url");
const std::string NamingService::policyAttribute("iplanet-am-naming-policy-"
						 "url");
const std::string NamingService::profileAttribute("iplanet-am-naming-profile-"
						  "url");
const std::string NamingService::sessionAttribute("iplanet-am-naming-session-"
						  "url");
const std::string NamingService::restAttribute("sun-naming-idsvcs-rest-url");
const std::string NamingService::loadbalancerCookieAttribute("am_load_balancer_cookie");
const std::string NamingService::invalidSessionMsgPrefix("SessionID ---");
const std::string NamingService::invalidSessionMsgSuffix("---is Invalid");

NamingService::NamingService(const Properties& props)
: BaseService("NamingService", props),
namingURL(props.get(AM_COMMON_NAMING_URL_PROPERTY)),
ignorePreferredNamingURL(props.getBool(AM_COMMON_IGNORE_PREFERRED_NAMING_URL_PROPERTY, true)) {
}

NamingService::~NamingService()
{
}

void NamingService::processAttribute(const std::string& name,
				     const std::string& value,
				     NamingInfo& namingInfo,
                     bool isAppSSOTokenPresent) const
{
    // If app ssotoken is not present in naming request,
    // then set all the services urls to namingInfo.properties.
    // This is required as naming response contains %tags rather
    // actual values, which needs to be parsed and tag swapped.
    // One such client is agent profile service, which does 
    // naming request without any app ssotoken.
    if(isAppSSOTokenPresent) {

        if (name == loggingAttribute) {
            namingInfo.loggingSvcInfo.setFromString(value);
        } else if (name == policyAttribute) {
            namingInfo.policySvcInfo.setFromString(value);
        } else if (name == profileAttribute) {
            namingInfo.profileSvcInfo.setFromString(value);
        } else if (name == sessionAttribute) {
            namingInfo.sessionSvcInfo.setFromString(value);
        } else if (name == loadbalancerCookieAttribute) {
            namingInfo.lbCookieStr = value;
        } else if (name == restAttribute) {
            namingInfo.restSvcInfo.setFromString(value);
        } else {
            namingInfo.extraProperties.set(name, value);
        }

    } else {
	namingInfo.extraProperties.set(name, value);
    }
}

/**
 * Throws XMLTree::ParseException 
 */
am_status_t NamingService::parseNamingResponse(const std::string& data,
					       const std::string& sessionId,
					       NamingInfo& namingInfo,
                           bool isAppSSOTokenPresent) const 
{
    am_status_t status = AM_SUCCESS;

    Log::log(logModule, Log::LOG_MAX_DEBUG,
	     "NamingService()::parseNamingResponse(): Buffer to be parsed: %s",
	     data.c_str());

    XMLTree::Init xt;
    XMLTree namingTree(false, data.c_str(), data.size());
    XMLElement element = namingTree.getRootElement();

    if (element.isNamed("NamingResponse")) {
	element = element.getFirstSubElement();
	if (element.isNamed("GetNamingProfile")) {
	    element = element.getFirstSubElement();
	    if (element.isNamed("Attribute")) {
		while (element.isValid()) {
		    if(element.isNamed("Attribute")) {
			std::string name;
			std::string value;
			if (element.getAttributeValue("name", name) &&
			    element.getAttributeValue("value", value)) {
			    processAttribute(name, value, namingInfo, isAppSSOTokenPresent);
			} else {
			    throw XMLTree::ParseException("Attribute missing "
							  "name or value "
							  "attribute");
			}
		    } else {
			element.log(logModule, Log::LOG_WARNING);
		    }
		    element.nextSibling();
		}
		status = AM_SUCCESS;
		namingInfo.infoValid = true;
	    } else if (element.isNamed("Exception")) {
		std::string exceptionMsg;
		Log::log(logModule, Log::LOG_MAX_DEBUG,
			 "NamingService::parseNamingResponse(): "
			 "Got Exception in XML.");
		if (element.getValue(exceptionMsg)) {
		    std::string invalidSessionMsg;

		    invalidSessionMsg.reserve(invalidSessionMsgPrefix.size() +
					      sessionId.size() +
					      invalidSessionMsgSuffix.size());
		    invalidSessionMsg = invalidSessionMsgPrefix;
		    invalidSessionMsg.append(sessionId);
		    invalidSessionMsg.append(invalidSessionMsgSuffix);
		    if (exceptionMsg == invalidSessionMsg) {
			status = AM_INVALID_SESSION;
		    } else {
			Log::log(logModule, Log::LOG_INFO,
				 "NamingService::parseNamingResponse() server "
				 "side error: %s", exceptionMsg.c_str());
			status = AM_NAMING_FAILURE;
		    }
		} else {
		    Log::log(logModule, Log::LOG_INFO,
			     "NamingService::parseNamingResponse() server "
			     "side error, no message in exception");
		    status = AM_NAMING_FAILURE;
		}
	    } else {
		element.log(logModule, Log::LOG_ERROR);
		throw XMLTree::ParseException("unexpected element in "
					      "GetNamingProfile");
	    }
	} else {
	    element.log(logModule, Log::LOG_ERROR);
	    throw XMLTree::ParseException("unexpected element in "
					  "NamingResponse");
	}
    } else {
	element.log(logModule, Log::LOG_ERROR);
	throw XMLTree::ParseException("unexpected element in Naming Service "
				      "data");
    }

    Log::log(logModule, Log::LOG_DEBUG, "NamingService::parseNamingResponse() "
	     "returning with status %s.", am_status_to_string(status));
    return status;
}


/* throws XMLTree::ParseException */
am_status_t NamingService::getProfile(const ServiceInfo& service,
					 const std::string& ssoToken,
					 Http::CookieList& cookieList,
					 NamingInfo& namingInfo)
{
    am_status_t status = AM_FAILURE;
    const ServerInfo *serverInfo = NULL;
    char portBuf[MAX_PORT_LENGTH + 1];

    if (ssoToken.size() <= 0) {
	status = AM_INVALID_ARGUMENT;
    }
    else {
	const std::size_t NUM_EXTRA_CHUNKS = 3;
	std::size_t url_length = 0;
	char *preferredNamingURL = NULL;
	Request request(*this, prefixChunk, namingPrefixChunk,
			NUM_EXTRA_CHUNKS);
	Http::Response response;
	BodyChunkList& bodyChunkList = request.getBodyChunkList();

	bodyChunkList.push_back(sessidPrefixChunk);
	bodyChunkList.push_back(BodyChunk(ssoToken));

        if (!ignorePreferredNamingURL) {
	    bodyChunkList.push_back(preferredNamingPrefixChunk);

	    url_length = strlen(namingURL.c_str());
	    preferredNamingURL = (char *)malloc(url_length);
	    if (preferredNamingURL != NULL) {
	        ServiceInfo::const_iterator iter;
	        for (iter = service.begin(); (iter != service.end() && 
                     status == AM_FAILURE); ++iter) {
		    std::string protocol = (*iter).getProtocol();
		    std::string hostname = (*iter).getHost();
		    unsigned short portnumber = (*iter).getPort();
		    status = check_server_alive((*iter).useSSL(), hostname, portnumber);
		    if (status == AM_SUCCESS) {
		        strcpy(preferredNamingURL,protocol.c_str());
		        strcat(preferredNamingURL,"://");
		        strcat(preferredNamingURL,hostname.c_str());
		        strcat(preferredNamingURL,":");
		        snprintf(portBuf, sizeof(portBuf), "%u", portnumber);
		        strcat(preferredNamingURL,portBuf);
		    }
	        }
	    } else {
	        Log::log(logModule, Log::LOG_ERROR,
	        "NamingService::getProfile() unable to allocate memory %d for "
	        "preferredNamingURL", url_length);
	    }

	    if (preferredNamingURL != NULL) {
	        bodyChunkList.push_back(BodyChunk(preferredNamingURL));
	        free(preferredNamingURL);
            }
        }
	bodyChunkList.push_back(suffixChunk);

	status = doHttpPost(service, std::string(), cookieList,
			    bodyChunkList, response, 
			    false, true, &serverInfo);
	if (AM_SUCCESS == status) {
	    try {
		std::vector<std::string> namingResponses;

		namingResponses = parseGenericResponse(response,
						       request.getGlobalId());
		if (1 == namingResponses.size()) {
		    status = parseNamingResponse(namingResponses[0], ssoToken,
						 namingInfo, true);
                    if (status == AM_SUCCESS) {
                       const std::string lbCookieStr = namingInfo.getlbCookieStr();
                       if (!lbCookieStr.empty()) {
                           addLoadBalancerCookie(namingInfo, cookieList);
                        }
                    }
		} else {
		    Log::log(logModule, Log::LOG_ERROR,
			     "NamingService::getProfile() unexpected number "
			     "of responses (%u) received, unsupported "
			     "behavior", namingResponses.size());
		    for (unsigned int i = 0; i < namingResponses.size(); ++i) {
			Log::log(logModule, Log::LOG_ERROR,
				 "NamingService::getProfile() response %u: %s",
				 i, namingResponses[i].c_str());
		    }
		    status = AM_NAMING_FAILURE;
		}
	    } catch (const XMLTree::ParseException& exc) {
		Log::log(logModule, Log::LOG_ERROR,
			 "NamingService::getProfile() caught exception: %s",
			 exc.getMessage().c_str());
		status = AM_NAMING_FAILURE;
	    } 
	}
    } 

    Log::log(logModule, Log::LOG_DEBUG, "NamingService()::getProfile() "
	     "returning with error code %s.", am_status_to_string(status));

    return status;
}

void NamingService::addLoadBalancerCookie(NamingInfo& namingInfo, 
					  Http::CookieList& cookieList)
{    
    int i = 0; 
    int j = 0; 
    std::size_t cookieLen = 0;
    char *cookieName = NULL;
    char *cookieValue = NULL;
    char *tmplbCookie = NULL;
    char *tmpPtr = NULL; 
    
    const std::string lbCookieStr = namingInfo.getlbCookieStr();
    if (!lbCookieStr.empty()) {
        cookieLen = lbCookieStr.size()+1;
        tmplbCookie = (char *)malloc(cookieLen);
        cookieName = (char *)malloc(cookieLen);
        cookieValue = (char *)malloc(cookieLen); 
	if (tmplbCookie != NULL && cookieName != NULL && cookieValue != NULL) {
            memset(tmplbCookie,'\0',cookieLen);
            memset(cookieName,'\0',cookieLen);
            memset(cookieValue,'\0',cookieLen);
            
            strcpy(tmplbCookie, lbCookieStr.c_str()); 
            
            tmpPtr = strchr(tmplbCookie,'=');
            if (tmpPtr != NULL) {
                // Retrieve the cookie name
               strncpy(cookieName, tmplbCookie, tmpPtr-tmplbCookie);
           
               // Retrieve the cookie value
               // Skip the '=' character
               tmpPtr++;
               strcpy(cookieValue,tmpPtr);           
            
               std::string tmpCookieName(cookieName);
               std::string tmpCookieValue(cookieValue);
    
               if (!tmpCookieName.empty() && !tmpCookieValue.empty()) {
                   namingInfo.lbCookieName = tmpCookieName;
                   namingInfo.lbCookieValue = tmpCookieValue;
                   Http::Cookie lbCookie(namingInfo.lbCookieName, 
                                         namingInfo.lbCookieValue);
                   cookieList.push_back(lbCookie);
              }
            }    
            free(tmplbCookie);
            free(cookieName);
            free(cookieValue);
        } else {
            Log::log(logModule, Log::LOG_ERROR,
                     "NamingService::addLoadBalancerCookie() - Unable "
		     "to allocate memory");
	}
    }

    return;
}

am_status_t NamingService::check_server_alive(bool ssl, std::string hostname, unsigned short portnumber) {
    am_status_t status = AM_FAILURE;

    std::string empty;

    if (getUseProxy()) return AM_SUCCESS;

    std::ostringstream sstm;
    sstm << (ssl ? "https://" : "http://") << hostname << ":" << portnumber << "/";

    Connection::ConnHeaderMap emptyHdrs;
    ServerInfo si(sstm.str());
    Connection conn(si);
    status = conn.sendRequest("HEAD", empty, emptyHdrs, empty);
    int http_status = conn.httpStatusCode();

    if (status == AM_SUCCESS) {
        Log::log(Log::ALL_MODULES, Log::LOG_DEBUG, "NamingService::check_server_alive(): returned success (HTTP %d)", http_status);
    } else {
        Log::log(Log::ALL_MODULES, Log::LOG_ERROR, "NamingService::check_server_alive(): returned error (HTTP %d, status: %s)",
                http_status, am_status_to_string(status));
    }
    return status;
}

/**
 * Performs naming request. The request doesn't contain any
 * sso token. The response contains %host, %protocol and %port
 * elements. 
 *
 * throws XMLTree::ParseException 
*/
am_status_t NamingService::doNamingRequest(const ServiceInfo& service,
                                         Http::CookieList& cookieList,
                                         NamingInfo& namingInfo)
{
    am_status_t status = AM_FAILURE;
    const ServerInfo *serverInfo = NULL;
    const std::size_t NUM_EXTRA_CHUNKS = 3;
    Request request(*this, prefixChunk, namingPrefixChunk,
                        NUM_EXTRA_CHUNKS);
    Http::Response response;
    BodyChunkList& bodyChunkList = request.getBodyChunkList();

    bodyChunkList.push_back(suffixChunk);
    
    status = doHttpPost(service, std::string(), cookieList,
                            bodyChunkList, response, 
                            false, true, &serverInfo);
    if (AM_SUCCESS == status) {
        try {
            std::vector<std::string> namingResponses;

            namingResponses = parseGenericResponse(response,
                                               request.getGlobalId());
            if (1 == namingResponses.size()) {
                status = parseNamingResponse(namingResponses[0], "",
                                             namingInfo, false);
                if (status == AM_SUCCESS) {
                   const std::string lbCookieStr = namingInfo.getlbCookieStr();
                   if (!lbCookieStr.empty()) {
                       addLoadBalancerCookie(namingInfo, cookieList);
                    }
                }
            } else {
                Log::log(logModule, Log::LOG_ERROR,
                     "NamingService::doNamingRequest() unexpected number "
                     "of responses (%u) received, unsupported "
                     "behavior", namingResponses.size());
                for (unsigned int i = 0; i < namingResponses.size(); ++i) {
                    Log::log(logModule, Log::LOG_ERROR,
                         "NamingService::doNamingRequest() response %u: %s",
                         i, namingResponses[i].c_str());
                }
                status = AM_NAMING_FAILURE;
            }
        } catch (const XMLTree::ParseException& exc) {
            Log::log(logModule, Log::LOG_ERROR,
                 "NamingService::doNamingRequest() caught exception: %s",
                 exc.getMessage().c_str());
            status = AM_NAMING_FAILURE;
        } catch (std::exception &exs) {
            Log::log(logModule, Log::LOG_ERROR,
                    "aNamingService::doNamingRequest(): exception encountered: %s",
                    exs.what());
            status = AM_NAMING_FAILURE;
        } catch (...) {
            Log::log(logModule, Log::LOG_ERROR,
                    "NamingService::doNamingRequest(): Unknown exception thrown.");
            status = AM_NAMING_FAILURE;
        }
    }

    Log::log(logModule, Log::LOG_DEBUG, "NamingService()::doNamingRequest() "
             "returning with error code %s.", am_status_to_string(status));

    return status;
}

