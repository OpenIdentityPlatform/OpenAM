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
 * $Id: policy_engine.cpp,v 1.10 2009/08/27 21:41:30 subbae Exp $
 *
 */ 
/*
 * Portions Copyrighted 2013 ForgeRock Inc
 */

#include <string.h>
#include <list>
#include <map>
#include <stdexcept>
#include <utility>
#include <vector>
#include "am_types.h"
#include "log.h"
#include "policy_engine.h"
#if defined(AIX)
#include "sso_token_service.h"
#endif
#include "service.h"
#if !defined(_MSC_VER)
#include <limits.h>
#endif

#define POLICY_ENGINE "PolicyEngine"



using namespace PRIVATE_NAMESPACE_NAME;
using std::string;
using std::map;
using std::vector;
using std::list;
using std::pair;
using std::invalid_argument;

/**
 * Throws:
 *	NSPRException upon NSPR error 
 *	InternalException upon other errors
 */
PolicyEngine::PolicyEngine(const Properties& startupParams)
    : logID(Log::addModule(POLICY_ENGINE)), namingSvc(NULL),
      vectorLock(), services(), configParams(startupParams)
{
    log(Log::LOG_MAX_DEBUG, "Policy engine initalized.");
}

void
PolicyEngine::log(Log::Level level, const char *format, ...) {
    std::va_list args;
    va_start(args, format);
    Log::vlog(logID, level, format, args);
    va_end(args);
    return;
}


/**
 * Throws:
 *	NSPRException upon NSPR error 
 *	InternalException upon other errors
 */
am_policy_t
PolicyEngine::create_service(const char *serviceName,
			     const char *instance_name,
			     am_resource_traits_t rsrcTraits,
			     const Properties& service_params)
{
    am_policy_t hdl = UINT_MAX;
    Service *svc = NULL;

    // Create a service
    try {
	svc = new Service(serviceName, instance_name, rsrcTraits,
			  service_params);
    } catch(std::exception &ex) {
	Log::log(logID, Log::LOG_ERROR, ex);

	throw InternalException("PolicyEngine::create_service",
				"Invalid argument.",
				AM_INIT_FAILURE);
    }


    // Initialize notification/polling.
    hdl = addService(svc);

    // Make initial request to get the anonymous user data.
    return hdl;
}

/**
 * Throws:
 *	std::invalid_argument if handle is invalid.
 *	InternalException upon other errors
 */
void
PolicyEngine::destroy_service(am_policy_t hdl) 
{
    Service *serviceEntry = getService(hdl);
    if(serviceEntry != NULL) {
	Log::log(logID, Log::LOG_INFO, "Service::destroy_service(): "
		 "Destroying service %s.",
		 serviceEntry->getServiceName().c_str());
	services[hdl] = static_cast<Service *>(NULL);
	log(Log::LOG_DEBUG,
	    "PolicyEngine::~PolicyEngine() Destroying Service: %s",
	    serviceEntry->getServiceName().c_str());
	delete serviceEntry;
    } else
	throw InternalException("PolicyEngine::destroy_service",
				"Invalid policy handle.",
				AM_INVALID_ARGUMENT);
    return;
}

PolicyEngine::~PolicyEngine() {
    std::vector<Service *>::iterator iter;
    for(iter = services.begin(); iter != services.end(); iter++) {
	Service *svc = *iter;
	if(svc != NULL) {
	    log(Log::LOG_DEBUG,
		"PolicyEngine::~PolicyEngine() Destroying Service: %s",
		svc->getServiceName().c_str());
	    delete(svc);
	} else {
	    log(Log::LOG_WARNING,
		"PolicyEngine::~PolicyEngine() Found NULL service while "
		"destroying services.");
	}
    }
    // stop notification/polling
    // destroy connection classes
    // destroy parser
    // cleanup configuration information
}

/**
 * Throws InternalException upon error.
 */
Service *
PolicyEngine::getService(am_policy_t hdl)
{
    Service *serviceEntry = NULL;
    // Get the service entry corresponding to the policy handle.
    if(hdl > services.size()) {
	throw InternalException("PolicyEngine::getService",
				"Invalid policy handle.",
				AM_INVALID_ARGUMENT);
    }

    try {
	serviceEntry = services[hdl];
    } catch(std::out_of_range &ore) {
	serviceEntry = NULL;
    }

    if(serviceEntry == NULL) {
	throw InternalException("PolicyEngine::getService",
				"Invalid policy handle",
				AM_NO_SUCH_SERVICE_TYPE);
    }

    return serviceEntry;
}

/**
 * Throws:
 *	NSPRException upon NSPR error 
 *	InternalException upon other errors
 */
void
PolicyEngine::policy_notify(am_policy_t policy_handle, 
                            const char *data,
                            size_t len,
        bool configChangeNotificationEnabled) {

    Service *serviceEntry = getService(policy_handle);
    try {
        XMLTree tree(false, data, len);
        XMLElement element = tree.getRootElement();
        if (element.isNamed(NOTIFICATION_SET)) {
            std::string version;
            std::string notifID;

            if (element.getAttributeValue(VERSION, version) &&
                    std::strcmp(version.c_str(), NOTIFICATION_SET_VERSION) == 0) {

                std::string notificationData;

                for (element = element.getFirstSubElement();
                        element.isNamed(NOTIFICATION); element.nextSibling()) {
                    if (element.getValue(notificationData)) {
                        log(Log::LOG_DEBUG,
                                "PolicyEngine::policy_notify :"
                                "Handling notification.");
                        policy_notification_handler(serviceEntry,
                                notificationData,
                                configChangeNotificationEnabled);

                    } else {
                        log(Log::LOG_WARNING,
                                "PolicyEngine::policy_notify : "
                                "Cannot get the value of the Notification "
                                "element.");
                    }
                }
            }
        }
    } catch (XMLTree::ParseException &ex) {
        throw InternalException("PolicyEngine::policy_notify",
                ex.getMessage(),
                AM_INVALID_ARGUMENT);
    }
    return;
}

/**
 * Handles both policy and session notifications.
 *
 * Throws:
 *	NSPRException upon NSPR error 
 *	InternalException upon other errors
 */
void
PolicyEngine::handleNotif(am_policy_t hdl, const std::string& notifData)
{
    Service *serviceEntry = getService(hdl);
    policy_notification_handler(serviceEntry, notifData);
}

/**
 * Throws:
 *	NSPRException upon NSPR error 
 *	InternalException upon other errors
 */
void
PolicyEngine::policy_notification_handler(Service *serviceEntry,
					  const std::string &data,
                                          bool configChangeNotificationEnabled)
{
    try {
        XMLTree::Init xt;
	XMLTree tree(false, data.c_str(), data.size());
	XMLElement rootElement = tree.getRootElement();
	string nodeName;

        // Agent Config Change notification.
	if (rootElement.isNamed(AGENT_CONFIG_CHANGE_NOTIFICATION)) {
            if(configChangeNotificationEnabled == true) {
                serviceEntry->agent_config_change_notify();
            }
            return;
        }
        
	// Session service notification.
	if(rootElement.isNamed(SESSION_NOTIFICATION)) {
	    XMLElement sessionElem;
	    if(rootElement.getSubElement(SESSION, sessionElem)) {
		// Session object obtained.

		log(Log::LOG_INFO,
		    "PolicyEngine::policy_notification_handler: "
		    "Parsing Session Notification");

		string sessionID;
		string state;

		// Only if there is a session id
		if(sessionElem.getAttributeValue(SESSION_ID_ATTRIBUTE,
						 sessionID)) {
		    // Only if there is a state attribute
		    if(sessionElem.getAttributeValue(SESSION_STATE_ATTRIBUTE,
						     state)) {
                        serviceEntry->sso_notify(sessionID);
		    }
		}
		
	    }
	    return;
	}

	if(!rootElement.isNamed(POLICY_SERVICE)) {
	    rootElement.getName(nodeName);
	    log(Log::LOG_ERROR, "PolicyEngine::policy_notification_handler: "
		"Root node must be PolicyService.  Root of input: %s.",
		nodeName.c_str());
	    throw InternalException("PolicyEngine::policy_notification_handler",
				    "Invalid root element in XML input.",
				    AM_INVALID_ARGUMENT);
	}

	// Policy service notification.
	XMLElement notif;
	// Get the policy change notification element.
	if(rootElement.getSubElement(POLICY_CHANGE_NOTIFICATION, notif)) {
	    string svcName;

	    log(Log::LOG_INFO,
		"PolicyEngine::policy_notification_handler:"
		"Parsing Policy Change Notification");

	    // Get the resource name
	    if(notif.getAttributeValue(SERVICE_NAME, svcName)) {
		std::vector<Service *>::iterator iter = services.begin();
		for(;iter != services.end(); iter++) {
		    if((*iter)->isServiceNamed(svcName))
			break;
		}

		if(iter != services.end()) {
		    for(XMLElement resNameNode = notif.getFirstSubElement();
			resNameNode.isValid();
			resNameNode.nextSibling(RESOURCE_NAME)) {

			/**
			 * This places changes according to the new
			 * notification type.
			 */
			string resName;
			if(resNameNode.getValue(resName)) {
			    NotificationType type = NOTIFICATION_DELETE;

			    string action;
			    notif.getAttributeValue(NOTIFICATION_TYPE, action);
			    if(action != NOTIF_TYPE_MODIFIED &&
			       action != NOTIF_TYPE_ADDED &&
			       action != NOTIF_TYPE_DELETED) {
				log(Log::LOG_WARNING,
				    "PolicyEngine::policy_notification_handler:"
				    " Skipping notification, action %s not "
				    "understood for resource %s.",
				    action.c_str(),
				    resName.c_str());
				continue;
			    }

			    if(action == NOTIF_TYPE_MODIFIED)
				type = NOTIFICATION_MODIFY;

			    if(action == NOTIF_TYPE_ADDED)
				type = NOTIFICATION_ADD;

			    if(action == NOTIF_TYPE_DELETED)
				type = NOTIFICATION_DELETE;

			    (*iter)->policy_notify(resName, type);

			} else {
			    log(Log::LOG_WARNING,
				"PolicyEngine::policy_notification_handler: "
				"Skipped notification. Could not extract"
				"resource name.");
			}

		    }
		} else {
		    log(Log::LOG_WARNING,
			"PolicyEngine::policy_notification_handler: "
			"Service name %s not found.", svcName.c_str());
		}
	    }
	} else if(rootElement.getSubElement(SUBJECT_CHANGE_NOTIFICATION,
					    notif)) {
	    log(Log::LOG_INFO,
		"PolicyEngine::policy_notification_handler: "
		"Parsing Subject Change Notification");
	}


    } catch(XMLTree::ParseException &ex) {
	throw InternalException("PolicyEngine::policy_notification_handler",
				ex.getMessage(),
				AM_INVALID_ARGUMENT);
    }
    return;
}

/**
 * Throws InternalException upon error.
 */
bool
PolicyEngine::isNotificationEnabled(am_policy_t hdl)
{
    // Get the service entry corresponding to the policy handle.
    if(hdl > services.size()) {
	throw InternalException("PolicyEngine::isNotificationEnabled",
				"Invalid policy handle.",
				AM_INVALID_ARGUMENT);
    }

    Service *serviceEntry = services[hdl];

    if(serviceEntry == NULL) {
	throw InternalException("PolicyEngine::isNotificationEnabled",
				"Invalid policy handle",
				AM_NO_SUCH_SERVICE_TYPE);
    }

    return serviceEntry->isNotificationEnabled();
}

/**
 * Throws:
 *	std::invalid_argument if any argument is invalid.
 *	NSPRException upon NSPR error 
 *	InternalException upon other errors
 */
void
PolicyEngine::policy_evaluate(am_policy_t hdl, const char *ssoToken,
			      const char *resName, const char *actionName,
			      const am_map_t env,
			      am_map_t response, am_policy_result_t *policy_res,
	                      am_bool_t ignorePolicyResult,
	                      Properties& properties)
{

    Service *serviceEntry = getService(hdl);

    if(ssoToken == NULL) {
	// XXX - process global not enforced list here?
	throw InternalException("PolicyEngine::policy_evaluate()",
				"Invalid ssoToken",
				AM_INVALID_SESSION);
    }
    if(resName == NULL || actionName == NULL || env == NULL ||
       policy_res == NULL) {
	throw InternalException("PolicyEngine::policy_evaluate()",
				"Invalid argument passed. One or more value"
				" is NULL.", AM_INVALID_ARGUMENT);
    }

    // Get the action decision...
    const KeyValueMap &kvmap = reinterpret_cast<const KeyValueMap &>(*env);
    try {
	serviceEntry->getPolicyResult(ssoToken, resName, actionName,
				      kvmap, response, policy_res, ignorePolicyResult, properties);
    } catch(XMLTree::ParseException &ex) {
	throw InternalException("PolicyEngine::policy_evaluate",
				ex.getMessage(),
				AM_FAILURE);
    }

    return;
}


/**
 * Throws:
 *	NSPRException upon NSPR error 
 *	InternalException upon other errors
 */
am_status_t
PolicyEngine::invalidate_session(am_policy_t hdl, const char *ssoTokenId)
{
    if (ssoTokenId == NULL || '\0' == *ssoTokenId) {
	log(Log::LOG_DEBUG,
	    "PolicyEngine::invalidate_session(): "
	    "null or empty ssoToken");
        return AM_INVALID_ARGUMENT;
    }

    Service *serviceEntry = getService(hdl);
    if (serviceEntry == NULL) {
	log(Log::LOG_DEBUG,
	    "PolicyEngine::invalidate_session(): "
            "Invalid policy handle - no service found.");
        return AM_INVALID_ARGUMENT;
    }
    return serviceEntry->invalidate_session(ssoTokenId);
}

/**
 * Throws:
 *	NSPRException upon NSPR error 
 *	InternalException upon other errors
 */
am_status_t
PolicyEngine::user_logout(am_policy_t hdl, 
                                 const char *ssoTokenId,
                                 Properties& properties)
{
    if (ssoTokenId == NULL || '\0' == *ssoTokenId) {
	log(Log::LOG_DEBUG,
	    "PolicyEngine::user_logout(): "
	    "null or empty ssoToken");
        return AM_INVALID_ARGUMENT;
    }

    Service *serviceEntry = getService(hdl);
    if (serviceEntry == NULL) {
	log(Log::LOG_DEBUG,
	    "PolicyEngine::user_logout(): "
            "Invalid policy handle - no service found.");
        return AM_INVALID_ARGUMENT;
    }
    return serviceEntry->user_logout(ssoTokenId,
                                            properties);
}
