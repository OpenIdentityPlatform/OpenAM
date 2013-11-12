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
 * $Id: am_main.cpp,v 1.12 2010/01/26 00:54:46 dknab Exp $
 *
 */
/*
 * Portions Copyrighted 2010-2013 ForgeRock Inc
 */

#include <cstring>
#include "am.h"
#include "connection.h"
#include "log.h"
#include "nspr_exception.h"
#include "properties.h"
#include "policy_engine.h"
#include "xml_tree.h"
#include "internal_macros.h"
#include "am_notify.h"
#include "version.h"
#include "mutex.h"


using std::string;

namespace {
    bool initialized;
    smi::Mutex initLock;
}

BEGIN_PRIVATE_NAMESPACE
DEFINE_BASE_INIT;
void policy_cleanup();	/* throws InternalException */
void sso_cleanup();	/* throws InternalException */
void auth_cleanup();	/* throws InternalException */
END_PRIVATE_NAMESPACE

USING_PRIVATE_NAMESPACE
void log_version_info() {
    Log::log(Log::ALL_MODULES, Log::LOG_ALWAYS,
            "=======================================");
    std::string versionStr("Version: ");
    versionStr.append(Version::getAgentVersion());
    if(Version::getERVersion() != NULL) {
        versionStr.append(" ");
        versionStr.append(Version::getERVersion());
    }
    if(Version::getFVBMarker() != NULL) {
        versionStr.append(" (");
        versionStr.append(Version::getFVBMarker());
        versionStr.append(" )");
    }
    Log::log(Log::ALL_MODULES, Log::LOG_ALWAYS, "%s",
            versionStr.c_str());

    if(Version::getBuildRev() != NULL) {
        Log::log(Log::ALL_MODULES, Log::LOG_ALWAYS,
            "%s", Version::getBuildRev());
    }
    if(Version::getBuildDate() != NULL) {
        Log::log(Log::ALL_MODULES, Log::LOG_ALWAYS,
            "Build Date: %s", Version::getBuildDate());
    }
    if(Version::getBuildMachine() != NULL) {
        Log::log(Log::ALL_MODULES, Log::LOG_ALWAYS,
            "Build Machine: %s", Version::getBuildMachine());
    }
    Log::log(Log::ALL_MODULES, Log::LOG_ALWAYS,
            "=======================================");
}

/**
 * Throws: InternalException upon error
 */
void PRIVATE_NAMESPACE_NAME::base_init(const Properties &propertiesRef, boolean_t initializeLog) {
    am_status_t status = AM_SUCCESS;

    ScopeLock myLock(initLock);
    if (!initialized) {
        
        try {

            // NOTE - The dependency here is
            // - connection::initialize depends on Log::initialize
            //   for the local log.
            // - remote log depends on XMLTree::initialize and
            //   connection::initialize and Log::initialize for local log
            // Ideally dependencies are taken care of in the classes
            // themselves.

            if (initializeLog) {
                Log::initialize(propertiesRef);
                log_version_info();
            }

            XMLTree::initialize();

            if (AM_SUCCESS == status) {
                status = Connection::initialize(propertiesRef);
            }
            if (AM_SUCCESS == status) {
                Log::initializeRemoteLog(propertiesRef);
                initialized = true;
            }

        } catch (const NSPRException& exc) {
            Log::log(Log::ALL_MODULES, Log::LOG_ERROR, exc);
            status = AM_NSPR_ERROR;
        } catch (const std::bad_alloc& exc) {
            Log::log(Log::ALL_MODULES, Log::LOG_ERROR, exc);
            status = AM_NO_MEMORY;
        } catch (const std::exception& exc) {
            Log::log(Log::ALL_MODULES, Log::LOG_ERROR,
                    "Unknown exception during base_init: %s",
                    exc.what());
            Log::log(Log::ALL_MODULES, Log::LOG_ERROR, exc);
            status = AM_FAILURE;
        }
    }
    if (status != AM_SUCCESS) {
        throw InternalException("base_init()", "Error while performing common initialization.", status);
    }
}

extern "C"
am_status_t am_shutdown_nss(void)
{
    //am_status_t status = NSS_IsInitialized() ? Connection::shutdown() : AM_SUCCESS;
    return AM_SUCCESS;
}

extern "C"
am_status_t am_cleanup(void) {
    Log::ModuleId logID = Log::addModule("am_cleanup()");
    am_status_t status = AM_SUCCESS;

    ScopeLock myLock(initLock);
    if (initialized) {
        initialized = false;
        try {
            /**
             * Call private interfaces to cleanup
             * static instances of services in the
             * respective files.
             */
            policy_cleanup();
            sso_cleanup();
            auth_cleanup();

            status = Connection::shutdown_in_child_process();
            
#if !defined(_MSC_VER) && !defined(__sun)
            libiconv_close();
#endif

            XMLTree::shutdown();

        } catch (const NSPRException& exc) {
            Log::log(logID, Log::LOG_ERROR, exc);
            status = AM_NSPR_ERROR;
        } catch (const std::exception& exc) {
            Log::log(logID, Log::LOG_ERROR, exc);
            status = AM_FAILURE;
        } catch (...) {
            Log::log(logID, Log::LOG_ERROR, "am_cleanup(): Unknown exception encountered");
            status = AM_FAILURE;
        }
        Log::shutdown();
    }
    return status;
}

extern "C"
void get_status_info(am_status_t status, const char ** name, const char ** msg)
{
    switch (status) {
        case AM_SUCCESS:
	    if (name) *name = "AM_SUCCESS";
	    if (msg) *msg = "success";
	    break;
        case AM_FAILURE:
            if (name) *name = "AM_FAILURE";
	    if (msg) *msg = "failure";
	    break;
        case AM_INIT_FAILURE:
            if (name) *name = "AM_INIT_FAILURE";
	    if (msg) *msg = "initialization failure";
	    break;
        case AM_AUTH_FAILURE:
            if (name) *name = "AM_AUTH_FAILURE";
	    if (msg) *msg = "OpenAM authentication service failure";
	    break;
        case AM_NAMING_FAILURE:
            if (name) *name = "AM_NAMING_FAILURE";
	    if (msg) *msg = "OpenAM naming service failure";
	    break;
        case AM_SESSION_FAILURE:
            if (name) *name = "AM_SESSION_FAILURE";
	    if (msg) *msg = "OpenAM session service failure";
	    break;
        case AM_POLICY_FAILURE:
            if (name) *name = "AM_POLICY_FAILURE";
	    if (msg) *msg = "OpenAM policy service failure";
	    break;
        case AM_NO_POLICY:
            if (name) *name = "AM_NO_POLICY";
	    if (msg) *msg = "no policy found";
	    break;
        case AM_INVALID_ARGUMENT:
            if (name) *name = "AM_INVALID_ARGUMENT";
	    if (msg) *msg = "invalid argument";
	    break;
        case AM_INVALID_VALUE:
            if (name) *name = "AM_INVALID_VALUE";
	    if (msg) *msg = "invalid value";
	    break;
        case AM_NOT_FOUND:
            if (name) *name = "AM_NOT_FOUND";
	    if (msg) *msg = "not found";
	    break;
        case AM_NO_MEMORY:
            if (name) *name = "AM_NO_MEMORY";
	    if (msg) *msg = "no memory";
	    break;
        case AM_NSPR_ERROR:
            if (name) *name = "AM_NSPR_ERROR";
	    if (msg) *msg = "NSPR error";
	    break;
        case AM_END_OF_FILE:
            if (name) *name = "AM_END_OF_FILE";
	    if (msg) *msg = "end of file";
	    break;
        case AM_BUFFER_TOO_SMALL:
            if (name) *name = "AM_BUFFER_TOO_SMALL";
	    if (msg) *msg = "buffer too small";
	    break;
        case AM_NO_SUCH_SERVICE_TYPE:
            if (name) *name = "AM_NO_SUCH_SERVICE_TYPE";
	    if (msg) *msg = "no such service type";
	    break;
        case AM_SERVICE_NOT_AVAILABLE:
            if (name) *name = "AM_SERVICE_NOT_AVAILABLE";
	    if (msg) *msg = "service not available";
	    break;
        case AM_ERROR_PARSING_XML:
            if (name) *name = "AM_ERROR_PARSING_XML";
	    if (msg) *msg = "error parsing XML";
	    break;
        case AM_INVALID_SESSION:
            if (name) *name = "AM_INVALID_SESSION";
	    if (msg) *msg = "invalid session";
	    break;
        case AM_INVALID_ACTION_TYPE:
            if (name) *name = "AM_INVALID_ACTION_TYPE";
	    if (msg) *msg = "invalid action type";
	    break;
        case AM_ACCESS_DENIED:
            if (name) *name = "AM_ACCESS_DENIED";
	    if (msg) *msg = "access denied";
	    break;
        case AM_HTTP_ERROR:
            if (name) *name = "AM_HTTP_ERROR";
	    if (msg) *msg = "HTTP error";
	    break;
        case AM_INVALID_FQDN_ACCESS:
            if (name) *name = "AM_INVALID_FQDN_ACCESS";
	    if (msg) *msg = "invalid FQDN access";
	    break;
        case AM_FEATURE_UNSUPPORTED:
            if (name) *name = "AM_FEATURE_UNSUPPORTED";
	    if (msg) *msg = "The feature or configuration is unsupported.";
	    break;
        case AM_AUTH_CTX_INIT_FAILURE:
            if (name) *name = "AM_AUTH_CTX_INIT_FAILURE";
	    if (msg) *msg = "Auth context initialization failure";
	    break;
        case AM_SERVICE_NOT_INITIALIZED:
            if (name) *name = "AM_SERVICE_NOT_INITIALIZED";
	    if (msg) *msg = "Service is uninitialized";
	    break;
        case AM_INVALID_RESOURCE_FORMAT:
            if (name) *name = "AM_INVALID_RESOURCE_FORMAT";
	    if (msg) *msg = "Resource name specified does not follow the "
		      "format required by the service";
	    break;
        case AM_NOTIF_NOT_ENABLED:
            if (name) *name = "AM_NOTIF_NOT_ENABLED";
	    if (msg) *msg = "Notification is not enabled or no notification URL set";
	    break;
        case AM_ERROR_DISPATCH_LISTENER:
            if (name) *name = "AM_ERROR_DISPATCH_LISTENER";
	    if (msg) *msg = "Error dispatching sso listener";
	    break;
        case AM_REMOTE_LOG_FAILURE:
            if (name) *name = "AM_REMOTE_LOG_FAILURE";
	    if (msg) *msg = "Remote log service encountered an error";
	    break;
        case AM_LOG_FAILURE:
            if (name) *name = "AM_LOG_FAILURE";
	    if (msg) *msg = "Log encountered an error";
	    break;
        case AM_REMOTE_LOG_NOT_INITIALIZED:
            if (name) *name = "AM_REMOTE_LOG_NOT_INITIALIZED";
	    if (msg) *msg = "Remote Log Service is not initialized";
	    break;
        case AM_REST_SERVICE_NOT_AVAILABLE:
            if (name) *name = "AM_REST_SERVICE_NOT_AVAILABLE";
	    if (msg) *msg = "REST service url not available";
	    break;
        case AM_REPOSITORY_TYPE_INVALID:
            if (name) *name = "AM_REPOSITORY_TYPE_INVALID";
	    if (msg) *msg = "Repository location value is invalid";
	    break;
        case AM_REST_ATTRS_SERVICE_FAILURE:
            if (name) *name = "AM_REST_ATTRS_SERVICE_FAILURE";
	    if (msg) *msg = "REST attributes service encountered an error";
	    break;
        case AM_REDIRECT_LOGOUT:
            if (name) *name = "AM_REDIRECT_LOGOUT";
	    if (msg) *msg = "redirect to logout";
	    break;
        case AM_AGENT_TIME_NOT_SYNC:
            if (name) *name = "AM_AGENT_TIME_NOT_SYNC";
	    if (msg) *msg = "time synchronization issue between agent and server.";
	    break;
	default:
	    if (name) *name = "unrecognized status code";
	    if (msg) *msg = "unknown error";
	    break;
    }
    return;
}

extern "C"
const char *am_status_to_string(am_status_t status)
{
    const char *name, *msg;

    get_status_info(status, &name, &msg);

    return msg;
}

extern "C"
const char *am_status_to_name(am_status_t status)
{
    //string name, msg;
    const char *name, *msg;

    get_status_info(status, &name, &msg);

    return name;
}

extern "C" int am_instance_id(const char *agent_bootstrap_file) {
    int id = 0;
#define INSTANCE_ID_KEY "com.forgerock.agents.instance.id"
    FILE *file = fopen(agent_bootstrap_file, "rt");
    if (file != NULL) {
        char line[64], key[64];
        while (fgets(line, sizeof (line), file) != NULL) {
            if (line[0] != '#' && memcmp(line, INSTANCE_ID_KEY, 32) == 0) {
                if (sscanf(line, "%[^=] = %d[^abcdefghijklmnopqrstuwzABCDEFGHIJKLMNOPQRSTUWZ]", key, &id) == 2) {
                    break;
                }
            }
        }
        fclose(file);
    }
    return id;
}

extern am_status_t
am_policy_handle_notification(am_policy_t hdl, const std::string& data);
extern am_status_t
am_sso_handle_notification(const std::string& data);

//
// returns AM_SUCCESS if both sso and policy notifications succeeded
// or not called at all.
// returns AM_INVALID_ARGUMENT if there was an error in the XML Tree.
// returns AM_SESSION_FAILURE if sso notification failed,
// AM_POLICY_FAILURE if policy notification failed,
// AM_FAILURE if both notifications failed.
// Actual failure status's are logged.
am_status_t
am_handle_notification(const std::string& data, am_policy_t policy_handle)
{
    Log::ModuleId logID = Log::addModule("am_handle_notification");

    am_status_t status = AM_SUCCESS;

    try {
	am_status_t sso_notif_status = AM_SUCCESS;
	am_status_t policy_notif_status = AM_SUCCESS;

	XMLTree tree(false, data.c_str(), data.size());
	XMLElement rootElement = tree.getRootElement();
	std::string nodeName;

	rootElement.getName(nodeName);
	Log::log(logID, Log::LOG_DEBUG,
		 "%s notification received", nodeName.c_str());
	// Session service notification.
	if(rootElement.isNamed(SESSION_NOTIFICATION)) {
	    Log::log(logID, Log::LOG_DEBUG,
		     "Passing notification to sso");
            sso_notif_status = am_sso_handle_notification(data);
	}
        // *Note*
        // Right now, always pass notification data to policy,
        // whether it's session or policy notification.
        // Later, when policy uses the sso api, pass only policy notifications
        // to policy.
        if (policy_handle) {
	    Log::log(logID, Log::LOG_DEBUG,
		     "Passing notification to policy");
            policy_notif_status =
                am_policy_handle_notification(policy_handle, data);
        }

	if (sso_notif_status != AM_SUCCESS) {
	    Log::log(logID, Log::LOG_ERROR,
		     "sso notification failed with status %s.",
		     am_status_to_string(sso_notif_status));
	    status = sso_notif_status;
	}
	if (policy_notif_status != AM_SUCCESS) {
	    Log::log(logID, Log::LOG_ERROR,
		     "policy notification failed with status %s.",
		     am_status_to_string(policy_notif_status));
	    if (status != AM_SUCCESS)
		status = policy_notif_status;
	}
    }
    catch(XMLTree::ParseException &ex) {
	Log::log(logID, Log::LOG_ERROR,
                 "XML Tree exception '%s' encountered.",
                 ex.getMessage().c_str());
        status = AM_INVALID_ARGUMENT;
    }
    catch (std::exception &exs) {
	Log::log(logID, Log::LOG_ERROR,
                 "am_handle_notification(): exception encountered: %s",
                 exs.what());
	status = AM_INVALID_ARGUMENT;
    }
    catch (...) {
	Log::log(logID, Log::LOG_ERROR,
                 "am_handle_notification(): Unknown exception encountered.");
	status = AM_FAILURE;
    }
    return status;
}

extern "C"
am_status_t
am_notify(const char *data, am_policy_t policy_handle)
{
    Log::ModuleId logID = Log::addModule("am_notify()");
    am_status_t sts = AM_FAILURE;

    if (NULL==data || '\0'==*data) {
        Log::log(logID, Log::LOG_ERROR,
                 "Notification data is NULL or empty string.");
        sts = AM_INVALID_ARGUMENT;
    }
    else {
        try {

	    XMLTree::Init xt;
            XMLTree tree(false, data, strlen(data));
	    XMLElement element = tree.getRootElement();
	    if(element.isNamed(NOTIFICATION_SET)) {
		std::string version;
		std::string notifID;

		if(element.getAttributeValue(VERSION, version) &&
		    std::strcmp(version.c_str(),
			        NOTIFICATION_SET_VERSION) == 0) {
		    std::string notificationData;
		    for(element = element.getFirstSubElement();
			element.isNamed(NOTIFICATION); element.nextSibling()) {
			if(element.getValue(notificationData)) {
			    Log::log(logID, Log::LOG_DEBUG,
				     "Handling notification.");
			    sts = am_handle_notification(notificationData,
							 policy_handle);
			    Log::log(logID, Log::LOG_DEBUG,
				     "handled notification with sts %d.", sts);
			} else {
			    std::string nodeName;
			    element.getName(nodeName);
			    Log::log(logID, Log::LOG_WARNING,
				     "Cannot get notification data for "
				     "notification type '%s' received. "
				     "Notification ignored.",
				     nodeName.c_str());
			}
		    }
		}
		else {
		    Log::log(logID, Log::LOG_WARNING,
			     "Unexpected notification version '%s' received.",
			     version.c_str());
		}
	    }
	    else {
		std::string rootName;
		element.getName(rootName);
		Log::log(logID, Log::LOG_WARNING,
			 "Invalid root element '%s', "
			 "not a notification set--message ignored.",
			 rootName.c_str());
	    }
	}
	catch (smi::InternalException& ex) {
	    sts = ex.getStatusCode();
	    Log::log(logID, Log::LOG_ERROR,
		     "am_notify(): "
		     "Internal error occurred, error code %d, message: %s",
		     sts, ex.getMessage());
	}
	catch (std::exception &exc) {
	    Log::log(logID, Log::LOG_ERROR,
		     "am_notify(): "
		     "Unexpected error occurred: %s", exc.what());
	    sts = AM_FAILURE;
	}
	catch (...) {
	    Log::log(logID, Log::LOG_ERROR,
		     "am_notify(): "
		     "Unknown error occurred");
	}
    }
    return sts;
}


