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
 * $Id: policy_service.cpp,v 1.5 2008/06/25 08:14:34 qcheng Exp $
 *
 */
/*
 * Portions Copyrighted 2013 ForgeRock Inc
 */

#include <stdexcept>

#include "policy_service.h"
#include "xml_tree.h"

USING_PRIVATE_NAMESPACE
namespace {
    const char requestPrefix[] = {
	"<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
	"<RequestSet vers=\"1.0\" svcid=\"Policy\" reqid=\""
    };

    // The first of the next two prefixes is used for the first policy
    // request within a given request set.   The second is used for
    // subsequent requests within a given request set.
#define	POLICY_SERVICE_VERSION	"1.0"
#define	COMMON_POLICY_SERVICE_PREFIX_DATA \
	  "<Request><![CDATA[\n" \
	    "<PolicyService version=\"" POLICY_SERVICE_VERSION "\">\n" \
	      "<PolicyRequest requestId=\""
    const char firstPolicyServicePrefix[] = {
	"\">\n"
	  COMMON_POLICY_SERVICE_PREFIX_DATA
    };

    const char additionalPolicyServicePrefix[] = {
	  COMMON_POLICY_SERVICE_PREFIX_DATA
    };

    const char appSSOTokenPrefix[] = { "\" appSSOToken=\"" };

    const char addListenerRequestPrefix[] = {
	      "\">\n"
	        "<AddPolicyListener notificationURL=\""
    };

    const char adviceListRequest[] = {
	      "\">\n"
	        "<AdvicesHandleableByAMRequest/>\n"
	      "</PolicyRequest>\n"
	    "</PolicyService>]]>\n"
	  "</Request>\n"
         "</RequestSet>\n"
    };

    const char serviceNamePrefix[] = { "\" serviceName=\"" };

    const char addOrRemoveListenerRequestSuffix[] = {
	        "\"/>\n"
	      "</PolicyRequest>\n"
	    "</PolicyService>]]>\n"
	  "</Request>\n"
    };

    const char removeListenerRequestPrefix[] = {
	      "\">\n"
	        "<RemovePolicyListener notificationURL=\""
    };


    const char getResourceResultsPrefix[] = {
	      "\">\n"
	        "<GetResourceResults userSSOToken=\""
    };

    // Uses serviceNamePrefix defined above as part of AddPolicyListener XML.

    const char resourceNamePrefix[] = {
	        "\" resourceName=\""
    };

    const char resourceScopePrefix[] = {
	        "\" resourceScope=\""
    };

    const char envParametersPrefix[] = {
	        "\">\n"
	          "<EnvParameters>\n"
    };

    const char envParametersSuffix[] = {
	"</EnvParameters>\n"
    };

    const char getResponseDecisionsPrefix[] = {
	"<GetResponseDecisions>\n"
    };

    const char attributePrefix[] = {
	"<Attribute name=\""
    };

    const char attributeSuffix[] = {
	"\"/>\n"
    };

    const char getResponseDecisionsSuffix[] = {
	"</GetResponseDecisions>\n"
    };

    const char attributeValuePairPrefix[] = {
	"<AttributeValuePair>\n"
	"<Attribute name=\""
    };

    const char attributeValuePairNameSuffix[] = {
	"\"/>\n"
    };

    const char valuePrefix[] = {
	"<Value>"
    };

    const char valueSuffix[] = {
	"</Value>\n"
    };

    const char attributeValuePairSuffix[] = {
	"</AttributeValuePair>\n"
    };

    const char getResourceResultsSuffixNoRD[] = {
		  "</EnvParameters>\n"
		"</GetResourceResults>\n"
	      "</PolicyRequest>\n"
	    "</PolicyService>]]>\n"
	  "</Request>\n"
    };

    const char getResourceResultsSuffixWithRD[] = {
		  "</GetResponseDecisions>\n"
		"</GetResourceResults>\n"
	      "</PolicyRequest>\n"
	    "</PolicyService>]]>\n"
	  "</Request>\n"
    };

    const char requestSuffix[] = {
	"</RequestSet>\n"
    };

    const char selfValue[] = "self";
    const char subtreeValue[] = "subtree";

    // The extra characters for each AttributeValuePair element and Value
    // element, respectively.  The constant is to adjust for the
    // terminating NUL character.
    const std::size_t avpOverhead = (sizeof(attributeValuePairPrefix) +
				     sizeof(attributeValuePairNameSuffix) +
				     sizeof(attributeValuePairSuffix) - 3);

    const std::size_t valueOverhead = (sizeof(valuePrefix) +
				       sizeof(valueSuffix) - 2);

    const char *policy_fetch_scope_str[] = { "self", "strict-subtree",
					     "subtree",
					     "response-attributes-only" };
}

const PolicyService::BodyChunk
PolicyService::prefixChunk(requestPrefix, sizeof(requestPrefix) - 1);

const PolicyService::BodyChunk
PolicyService::firstPolicyServicePrefixChunk(firstPolicyServicePrefix,
					     sizeof(firstPolicyServicePrefix) - 1);

const PolicyService::BodyChunk
PolicyService::additionalPolicyServicePrefixChunk(additionalPolicyServicePrefix,
						  sizeof(additionalPolicyServicePrefix) - 1);

const PolicyService::BodyChunk
PolicyService::appSSOTokenPrefixChunk(appSSOTokenPrefix,
				      sizeof(appSSOTokenPrefix) - 1);

const PolicyService::BodyChunk
PolicyService::adviceListRequestChunk(adviceListRequest,
				      sizeof(adviceListRequest) - 1);

const PolicyService::BodyChunk
PolicyService::addListenerRequestPrefixChunk(addListenerRequestPrefix,
					     sizeof(addListenerRequestPrefix) - 1);

const PolicyService::BodyChunk
PolicyService::removeListenerRequestPrefixChunk(removeListenerRequestPrefix,
						sizeof(removeListenerRequestPrefix) - 1);

const PolicyService::BodyChunk
PolicyService::serviceNamePrefixChunk(serviceNamePrefix,
				      sizeof(serviceNamePrefix) - 1);

const PolicyService::BodyChunk
PolicyService::addOrRemoveListenerRequestSuffixChunk(addOrRemoveListenerRequestSuffix,
					     sizeof(addOrRemoveListenerRequestSuffix) - 1);

const PolicyService::BodyChunk
PolicyService::getResourceResultsPrefixChunk(getResourceResultsPrefix,
					     sizeof(getResourceResultsPrefix) - 1);

const PolicyService::BodyChunk
PolicyService::resourceNamePrefixChunk(resourceNamePrefix,
				       sizeof(resourceNamePrefix) - 1);

const PolicyService::BodyChunk
PolicyService::resourceScopePrefixChunk(resourceScopePrefix,
					sizeof(resourceScopePrefix) - 1);

const PolicyService::BodyChunk
PolicyService::envParametersPrefixChunk(envParametersPrefix,
					sizeof(envParametersPrefix) - 1);

const PolicyService::BodyChunk
PolicyService::envParametersSuffixChunk(envParametersSuffix,
					sizeof(envParametersSuffix) - 1);

const PolicyService::BodyChunk
PolicyService::getResponseDecisionsPrefixChunk(getResponseDecisionsPrefix,
					       sizeof(getResponseDecisionsPrefix) - 1);

const PolicyService::BodyChunk
PolicyService::attributePrefixChunk(attributePrefix,
				    sizeof(attributePrefix) - 1);

const PolicyService::BodyChunk
PolicyService::attributeSuffixChunk(attributeSuffix,
				    sizeof(attributeSuffix) - 1);

const PolicyService::BodyChunk
PolicyService::getResourceResultsSuffixWithRDChunk(getResourceResultsSuffixWithRD,
					     sizeof(getResourceResultsSuffixWithRD) - 1);

const PolicyService::BodyChunk
PolicyService::getResourceResultsSuffixNoRDChunk(getResourceResultsSuffixNoRD,
					     sizeof(getResourceResultsSuffixNoRD) - 1);

const PolicyService::BodyChunk
PolicyService::suffixChunk(requestSuffix, sizeof(requestSuffix) - 1);

const PolicyService::BodyChunk
PolicyService::selfChunk(selfValue, sizeof(selfValue) - 1);

const PolicyService::BodyChunk
PolicyService::subtreeChunk(subtreeValue, sizeof(subtreeValue) - 1);


/* Throws std::invalid_argument if any argument is invalid */
PolicyService::PolicyService(const SSOToken& agentTokenArg,
                             const Properties& props)
    : BaseService("PolicyService", props),
      agentToken(agentTokenArg)
{
    if (! agentToken.isValid()) {
	throw std::invalid_argument("PolicyService() invalid SSOToken");
    }
}

PolicyService::~PolicyService()
{
}

#define	COPY_ARRAY(_buffer, _offset, _array) \
do { \
    std::memcpy(&_buffer[_offset], _array, sizeof(_array) - 1); \
    _offset += sizeof(_array) - 1; \
} while (0)

#define	COPY_STRING(_buffer, _offset, _string) \
do { \
    std::memcpy(&_buffer[_offset], _string.c_str(), _string.size()); \
    _offset += _string.size(); \
} while (0)

/* Throws std::invalid_argument if any argument is invalid */
std::size_t PolicyService::addAttribute(char *buffer, std::size_t bufferLen,
					std::size_t offset,
					const std::string& attribute) const
{
    std::size_t curLen = offset;

    if (bufferLen >= (sizeof(attributeValuePairPrefix) +
		      sizeof(attributeValuePairNameSuffix) +
		      attribute.size() - 2 + offset)) {
	COPY_ARRAY(buffer, curLen, attributeValuePairPrefix);
	COPY_STRING(buffer, curLen, attribute);
	COPY_ARRAY(buffer, curLen, attributeValuePairNameSuffix);
    } else {
	throw std::invalid_argument("PolicyService::addAttribute() buffer too "
				    "short");
    }

    return curLen - offset;
}

/* Throws std::invalid_argument if any argument is invalid */
std::size_t PolicyService::addValue(char *buffer, std::size_t bufferLen,
				    std::size_t offset,
				    const std::string& value) const
{
    std::size_t curLen = offset;

    if (bufferLen >= (sizeof(valuePrefix) + sizeof(valueSuffix) +
		      value.size() - 2 + offset)) {
	COPY_ARRAY(buffer, curLen, valuePrefix);
	COPY_STRING(buffer, curLen, value);
	COPY_ARRAY(buffer, curLen, valueSuffix);
    } else {
	throw std::invalid_argument("PolicyService::addValue() buffer too "
				    "short");
    }

    return curLen - offset;
}

/* Throws std::invalid_argument if any argument is invalid */
std::size_t
PolicyService::addAttributeValuePair(char *buffer,
			     std::size_t bufferLen,
			     std::size_t offset,
			     const std::string& attribute,
			     const KeyValueMap::mapped_type& values) const
{
    std::size_t curLen = offset;

    curLen += addAttribute(buffer, bufferLen, curLen, attribute);
    for (KeyValueMap::mapped_type::const_iterator iter = values.begin();
	 iter != values.end();
	 ++iter) {
	curLen += addValue(buffer, bufferLen, curLen, *iter);
    }

    if (bufferLen >= (sizeof(attributeValuePairSuffix) - 1 + curLen)) {
	COPY_ARRAY(buffer, curLen, attributeValuePairSuffix);
    } else {
	throw std::invalid_argument("PolicyService::addAttributeValuePair() "
				    "buffer too short");
    }

    return curLen - offset;
}

void PolicyService::addEnvParameters(const KeyValueMap& envParameters,
				     Request& request) const
{
    KeyValueMap::const_iterator keyIter;
    std::size_t avpBufferLen = 0;

    for (keyIter = envParameters.begin();
	 keyIter != envParameters.end();
	 ++keyIter) {
	KeyValueMap::mapped_type::const_iterator valueIter;
	KeyValueMap::mapped_type::const_iterator endIter;

	avpBufferLen += keyIter->first.size() + avpOverhead;
	for (valueIter = keyIter->second.begin(), endIter = keyIter->second.end();
	     valueIter != endIter;
	     ++valueIter) {
	    avpBufferLen += valueIter->size() + valueOverhead;
	}
    }

    char *avpBuffer = new char[avpBufferLen];
    // Give ownership of the buffer to the request object, which will
    // ensure that the buffer is deleted, even if an exception is thrown.
    request.setExtraData(avpBuffer);

    std::size_t curLen = 0;
    KeyValueMap::mapped_type temp(1);

    for (keyIter = envParameters.begin();
	 keyIter != envParameters.end();
	 ++keyIter) {
	curLen += addAttributeValuePair(avpBuffer, avpBufferLen, curLen,
					keyIter->first, keyIter->second);
    }

    if (curLen < avpBufferLen) {
	Log::log(logModule, Log::LOG_WARNING,
		 "PolicyService::addEnvParameters() data shorter than "
		 "buffer (%u < %u)", curLen, avpBufferLen);
	Log::log(logModule, Log::LOG_WARNING, "%.*s", curLen, avpBuffer);
    }

    request.getBodyChunkList().push_back(BodyChunk(avpBuffer, curLen));
}

am_status_t
PolicyService::parseException(const XMLElement& element) const
{
    am_status_t status = AM_POLICY_FAILURE;
    std::string exceptionMsg;

    if (element.getValue(exceptionMsg)) {
	char appSSOTokenMsg[] = "Application sso token";
	char userSSOTokenMsg[] = "User's SSO token is invalid";
	if(strncmp(exceptionMsg.c_str() + 1, appSSOTokenMsg, 
	   sizeof(appSSOTokenMsg) - 1) == 0) {
	         status = AM_INIT_FAILURE;
	} else if(strncmp(exceptionMsg.c_str() + 1, userSSOTokenMsg, 
	       sizeof(userSSOTokenMsg) - 1) == 0) {
                 status = AM_INVALID_SESSION;
        }
	Log::log(logModule, Log::LOG_INFO,
		 "PolicyService::parseException() server side error: %s",
		 exceptionMsg.c_str());
    } else {
	Log::log(logModule, Log::LOG_INFO,
		 "PolicyService::parseException() server side error, no "
		 "message in exception");
    }

    return status;
}

am_status_t
PolicyService::parseListenerResponse(bool addOrRemove,
				     const XMLElement& element) const
{
    am_status_t status;
    const char *RESPONSE_ELEMENT=addOrRemove?ADD_LISTENER_RESPONSE:REMOVE_LISTENER_RESPONSE;

    if (element.isNamed(RESPONSE_ELEMENT)) {
	Log::log(logModule, Log::LOG_DEBUG,
		 "PolicyService::parseListenerResponse() server responded "
		 "OK");
	status = AM_SUCCESS;
    } else if (element.isNamed("Exception")) {
	status = parseException(element);
    } else {
	element.log(logModule, Log::LOG_ERROR);
	throw XMLTree::ParseException("Unexpected element in PolicyResponse");
    }

    return status;
}

am_status_t
PolicyService::parseGetPolicyResultsResponse(const XMLElement& element) const
{
    am_status_t status;

    if (element.isNamed(RESOURCE_RESULT)) {
	status = AM_SUCCESS;
    } else if (element.isNamed("Exception")) {
	status = parseException(element);
    } else {
	element.log(logModule, Log::LOG_ERROR);
	throw XMLTree::ParseException("unexpected element in PolicyResponse");
    }

    return status;
}

am_status_t
PolicyService::getPolicyDecisions(const ServiceInfo& service,
				  const SSOToken& userToken,
				  const Http::CookieList& cookieList,
				  const SessionInfo& sessionInfo,
				  const std::string& serviceName,
				  const std::string& resourceName,
				  policy_fetch_scope_t scope,
				  const KeyValueMap& envParameters,
				  const std::list<std::string> &attributes,
				  std::string& policyData)
{
    am_status_t status;
    std::string resName(resourceName);

    if (userToken.isValid()) {
	const std::size_t NUM_EXTRA_CHUNKS_WITHOUT_NOTIFICATION = 14;
	Request request(*this, prefixChunk, firstPolicyServicePrefixChunk,
			 NUM_EXTRA_CHUNKS_WITHOUT_NOTIFICATION);
	BodyChunk envParameterChunk;
	Http::Response response;
	BodyChunk agentTokenChunk(agentToken.getString());
	BodyChunk serviceNameChunk(serviceName);
	BodyChunkList& bodyChunkList = request.getBodyChunkList();

	// Do entity Reference conversions
	Utils::expandEntityRefs(resName);

	bodyChunkList.push_back(appSSOTokenPrefixChunk);
	bodyChunkList.push_back(agentTokenChunk);
	bodyChunkList.push_back(getResourceResultsPrefixChunk);
	bodyChunkList.push_back(BodyChunk(userToken.getString()));
	bodyChunkList.push_back(serviceNamePrefixChunk);
	bodyChunkList.push_back(serviceNameChunk);
	bodyChunkList.push_back(resourceNamePrefixChunk);
	bodyChunkList.push_back(BodyChunk(resName));
	bodyChunkList.push_back(resourceScopePrefixChunk);
	bodyChunkList.push_back(BodyChunk(policy_fetch_scope_str[scope]));
	bodyChunkList.push_back(envParametersPrefixChunk);

	addEnvParameters(envParameters, request);

	bodyChunkList.push_back(envParameterChunk);

	if(attributes.size() > 0) {
	    Log::log(logModule, Log::LOG_DEBUG,
		     "PolicyService::getPolicyResult()"
		     "Response decisions requested %d.",
		     attributes.size());
	    bodyChunkList.push_back(envParametersSuffixChunk);
	    bodyChunkList.push_back(getResponseDecisionsPrefixChunk);
	    std::list<std::string>::const_iterator iter;
	    for(iter = attributes.begin(); iter != attributes.end(); iter++) {
		std::string attr = (*iter);
		Log::log(logModule, Log::LOG_MAX_DEBUG,
			 "PolicyService::getPolicyResult()"
			 "Attribute name=%s.",
			 attr.c_str());
		bodyChunkList.push_back(attributePrefixChunk);
		bodyChunkList.push_back(BodyChunk(attr));
		bodyChunkList.push_back(attributeSuffixChunk);
	    }
	    bodyChunkList.push_back(getResourceResultsSuffixWithRDChunk);
	} else {
	    bodyChunkList.push_back(getResourceResultsSuffixNoRDChunk);
	}

	bodyChunkList.push_back(suffixChunk);

	status = doHttpPost(service, std::string(), cookieList,
			    bodyChunkList, response);
	if (AM_SUCCESS == status) {
	    try {
		std::vector<std::string> policyResponses;

		policyResponses = parseGenericResponse(response,
						       request.getGlobalId());
		if (policyResponses.empty()) {
		    throw XMLTree::ParseException("Policy Service returned "
						  "an empty ResponseSet");
		}

                XMLTree::Init xt;
		for (std::size_t i = 0;
		     i < policyResponses.size() && AM_SUCCESS == status;
		     ++i) {
		    XMLTree policyTree(false,
					policyResponses[i].c_str(),
					policyResponses[i].size());
		    XMLElement element = policyTree.getRootElement();
		    std::string version;

		    if (element.isNamed(POLICY_SERVICE) &&
			element.getAttributeValue(VERSION_STR, version) &&
			std::strcmp(version.c_str(),
				    POLICY_SERVICE_VERSION) == 0) {
			std::string requestId;

			element = element.getFirstSubElement();
			if (! element.isNamed(POLICY_RESPONSE)) {
			    throw XMLTree::ParseException("unexpected element "
							  "in PolicyService "
							  "element");
			}

			if (! element.getAttributeValue(REQUEST_ID_STR,
							requestId)) {
			    throw XMLTree::ParseException("no request id in "
							  "PolicyResponse");
			}

			XMLElement subElement = element.getFirstSubElement();

			if (std::strcmp(requestId.c_str(),
					request.getServiceId()) == 0) {
			    status = parseGetPolicyResultsResponse(subElement);
			    if (AM_SUCCESS == status) {
				policyData = policyResponses[i];
			    }
			} else {
			    throw XMLTree::ParseException("unknown request id "
							  "in Policy Service "
							  "data");
			}
		    } else {
			element.log(logModule, Log::LOG_ERROR);
			if (element.isNamed(POLICY_RESPONSE)) {
			    throw XMLTree::ParseException("version mismatch "
							  "in Policy Service "
							  "data");
			} else {
			    throw XMLTree::ParseException("unexpected element "
							  "in Policy Service "
							  "data");
			}
		    }
		}		    
	    } catch (const XMLTree::ParseException& exc) {
		Log::log(logModule, Log::LOG_ERROR,
			 "PolicyService::getPolicyDecisions() caught "
			 "exception: %s", exc.getMessage().c_str());
		status = AM_ERROR_PARSING_XML;
	    }
	}
    } else {
	status = AM_INVALID_ARGUMENT;
    }

    return status;
}

/* Throws 
 *	std::invalid_argument if any argument is invalid 
 *	XMLTree::ParseException on XML parse error 
 */
void
PolicyService::sendNotificationMsg(bool addOrRemove,
				   const ServiceInfo &service,
				   const std::string &serviceName,
				   const Http::CookieList &cookieList,
				   const std::string &notificationURL)
{
    am_status_t status = AM_SUCCESS;
    std::string notifURL(notificationURL);
    Request request(*this, prefixChunk, firstPolicyServicePrefixChunk,
		    10);

    Http::Response response;
    BodyChunk agentTokenChunk(agentToken.getString());
    BodyChunk serviceNameChunk(serviceName);
    BodyChunkList &bodyChunkList = request.getBodyChunkList();

    bodyChunkList.push_back(appSSOTokenPrefixChunk);
    bodyChunkList.push_back(agentTokenChunk);
    if(addOrRemove) {
	bodyChunkList.push_back(addListenerRequestPrefixChunk);
    } else {
	bodyChunkList.push_back(removeListenerRequestPrefixChunk);
    }

    // Do Entity Ref conversions.
    Utils::expandEntityRefs(notifURL);
    bodyChunkList.push_back(BodyChunk(notifURL));
    bodyChunkList.push_back(serviceNamePrefixChunk);
    bodyChunkList.push_back(serviceNameChunk);
    bodyChunkList.push_back(addOrRemoveListenerRequestSuffixChunk);
    bodyChunkList.push_back(suffixChunk);

    // Do the add Listener operation.
    status = doHttpPost(service, std::string(), cookieList,
			bodyChunkList, response);

    if(status == AM_SUCCESS) {
	std::vector<std::string> policyResponses;
	policyResponses = parseGenericResponse(response,
					       request.getGlobalId());
	if(policyResponses.empty()) {
	    throw XMLTree::ParseException("PolicyService::sendMsgToPolicyListener: "
					  "Returned an empty ResponseSet"
					  "while (de)registering notification.");
	}
        XMLTree::Init xt;
	XMLTree policyTree(false,
			   policyResponses[0].c_str(),
			   policyResponses[0].size());

	XMLElement element = policyTree.getRootElement();
	std::string version;
	std::string revisionStr;
	char rev[10] = {'\0'};
        revision = 0;

        if(element.isNamed(POLICY_SERVICE)) {
            if(element.getAttributeValue(VERSION_STR, version) &&
	       std::strcmp(version.c_str(), POLICY_SERVICE_VERSION) == 0) {
	        element.getAttributeValue(REVISION_STR, revisionStr);
                if (!revisionStr.empty()) {
                    strcpy(rev, revisionStr.c_str());
                    // get revision of server
                    revision=atoi(rev);
                }

		std::string requestId;
		element.getSubElement(POLICY_RESPONSE, element);
		if(element.isValid() && element.isNamed(POLICY_RESPONSE)) {
		    if(element.getAttributeValue(REQUEST_ID_STR,
						 requestId) &&
		       std::strcmp(requestId.c_str(),
				   request.getServiceId()) == 0) {
			XMLElement subElement;
			subElement = element.getFirstSubElement();
			status = parseListenerResponse(addOrRemove, subElement);
		    } else {
			throw XMLTree::ParseException("PolicyService::sendMsgToPolicyListener: Unknown Request ID in response.");
		    }
		} else {
		    throw XMLTree::ParseException("PolicyService::sendMsgToPolicyListener: "
						  "PolicyResponse not found in response.");
		}
	    } else {
	      throw InternalException("PolicyService::sendMsgToPolicyListener",
				      "Policy response version does not match.",
				      AM_POLICY_FAILURE);
	    }
	}
    }
    Log::log(logModule, Log::LOG_INFO,"Policy decision revision number: %d",revision);
    Log::log(logModule, Log::LOG_INFO,
	     "PolicyService::sendMsgToPolicyListener: "
	     "Successfully (de)registered for notification.");
}

void
PolicyService::getAdvicesList(const ServiceInfo &service,
				const std::string &serviceName,
				const Http::CookieList &cookieList,
				KeyValueMap &adviceMap) {
    am_status_t status = AM_SUCCESS;
    Request request(*this, prefixChunk, firstPolicyServicePrefixChunk,
		    10);

    Http::Response response;
    BodyChunk agentTokenChunk(agentToken.getString());
    BodyChunk serviceNameChunk(serviceName);
    BodyChunkList &bodyChunkList = request.getBodyChunkList();

    bodyChunkList.push_back(appSSOTokenPrefixChunk);
    bodyChunkList.push_back(agentTokenChunk);
    bodyChunkList.push_back(adviceListRequestChunk);

    // Do the add Listener operation.
    status = doHttpPost(service, std::string(), cookieList,
			bodyChunkList, response);

    if(AM_SUCCESS == status) {
	std::vector<std::string> policyResponses;
	policyResponses = parseGenericResponse(response,
					       request.getGlobalId());
	if(policyResponses.empty()) {
	    throw XMLTree::ParseException("Policy Service returned "
					 "an empty ResponseSet");
	}
        XMLTree::Init xt;
	for(std::size_t i = 0;
	    i < policyResponses.size() && AM_SUCCESS == status; ++i) {
	    XMLTree adviceTree(false,
			       policyResponses[i].c_str(),
			       policyResponses[i].size());
	    XMLElement element = adviceTree.getRootElement();
	    std::string version;
	    if (element.isNamed(POLICY_SERVICE) &&
		element.getAttributeValue(VERSION_STR, version) &&
		std::strcmp(version.c_str(),
			    POLICY_SERVICE_VERSION) == 0) {
		std::string requestId;
		element = element.getFirstSubElement();
		if(!element.isNamed(POLICY_RESPONSE)) {
		    throw XMLTree::ParseException("unexpected element "
						  "in PolicyService "
						  "element");
		}
		if(!element.getAttributeValue(REQUEST_ID_STR,
					      requestId)) {
		    throw XMLTree::ParseException("No request id in "
						  "PolicyResponse.");
		}
		if(std::strcmp(requestId.c_str(),
			       request.getServiceId()) != 0) {
		    throw XMLTree::ParseException("unknown request id "
						  "in Policy Service "
						  "data");
		}
		XMLElement subElement = element.getFirstSubElement();
		status = parseAdviceListResponse(subElement, adviceMap);
	    } else {
		element.log(logModule, Log::LOG_ERROR);
		if (element.isNamed(POLICY_RESPONSE)) {
		    throw XMLTree::ParseException("version mismatch "
						  "in Policy Service "
						  "data");
		} else {
		    throw XMLTree::ParseException("unexpected element "
						  "in Policy Service "
						  "data");
		}
	    }
	}
    } else {
	throw InternalException("PolicyService::updateAdviceList()",
				"Error while getting advice list.",
				status);
    }
}

am_status_t
PolicyService::parseAdviceListResponse(const XMLElement &element,
				       KeyValueMap &adviceMap) {
    am_status_t status = AM_SUCCESS;
    if(element.isNamed("Exception")) {
	parseException(element);
	status = AM_POLICY_FAILURE;
    } else if(element.isNamed(ADVICE_LIST_RESPONSE)) {
	XMLElement avpElement = element.getFirstSubElement();
	if(avpElement.isNamed(ATTRIBUTE_VALUE_PAIR)) {
	   adviceMap.insert(avpElement); 
	} else {
	    Log::log(logModule, Log::LOG_ERROR,
		     "PolicyService::parseAdviceListResponse(): "
		     "Invalid element encountered.");
	    status = AM_POLICY_FAILURE;
	}
    }
    return status;
}
