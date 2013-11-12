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
 * $Id: am_policy.cpp,v 1.11 2009/10/28 21:56:20 subbae Exp $
 *
 */ 
/*
 * Portions Copyrighted 2013 ForgeRock Inc
 */

#include <cstring>
#include <stdexcept>
#include <string>
#include <am_policy.h>
#include <am_sso.h>

#include "internal_macros.h"
#include "internal_exception.h"
#include "properties.h"
#include "server_info.h"
#include "policy_engine.h"
#include "utils.h"
#include "url.h"

#include "service.h"

BEGIN_PRIVATE_NAMESPACE
static PolicyEngine *enginePtr;
DEFINE_BASE_INIT;
void policy_cleanup();
END_PRIVATE_NAMESPACE

USING_PRIVATE_NAMESPACE


void PRIVATE_NAMESPACE_NAME::policy_cleanup() {
    delete enginePtr;
    enginePtr = NULL;
    return;
}

#define AM_POLICY_SERVICE "AM_POLICY_SERVICE"

extern "C" am_status_t
am_policy_init(am_properties_t policy_config_params) {
    Log::ModuleId logID = Log::addModule(AM_POLICY_SERVICE);
    am_status_t status = AM_SUCCESS;

	if (policy_config_params) {
	    const Properties& propertiesRef =
		*reinterpret_cast<Properties *>(policy_config_params);

	    try {
		if(enginePtr == NULL) {
		    am_sso_init(policy_config_params);

		    // Now try creating the policy engine.
		    enginePtr = new PolicyEngine(propertiesRef);
		} else {
                    Log::log(logID, Log::LOG_DEBUG,
                             "Policy Service already initialized");
		}

	    }
            catch (const InternalException exc) {
                status = exc.getStatusCode();
                Log::log(logID, Log::LOG_ERROR, exc);
            }
	    catch (const std::bad_alloc& exc) {
		status = AM_NO_MEMORY;
		Log::log(logID, Log::LOG_ERROR, exc);
	    }  catch(std::exception &ex) {
		Log::log(enginePtr->getModuleID(), Log::LOG_ERROR, ex);
		return AM_FAILURE;
	    }
            catch (...) {
                status = AM_FAILURE;
                Log::log(logID, Log::LOG_ERROR,
                         "Unknown exception encountered.");
            }

	} else {
	    Log::log(logID, Log::LOG_DEBUG,
		     "am_policy_init(): "
                     "One or more invalid parameters is invalid.");
	    status = AM_INVALID_ARGUMENT;
	}
    return status;
}

/*
 * Method to initialize a policy evaluator object.
 */
extern "C" am_status_t
am_policy_service_init(const char *service_name,
		       const char *instance_name,
		       am_resource_traits_t rsrcTraits,
		       am_properties_t service_config_params,
		       am_policy_t *policy_handle_ptr)
{
    try {
	*policy_handle_ptr = enginePtr->create_service(service_name,
						       instance_name,
						       rsrcTraits,
						       *reinterpret_cast<Properties *>(service_config_params));
    } catch(InternalException &ie) {
	Log::log(enginePtr->getModuleID(), Log::LOG_ERROR,
		 "am_policy_init: InternalException in %s, Message is %s, Status code is %d.",
		 ie.getThrowingMethod(), ie.getMessage(),ie.getStatusCode());

	return ie.getStatusCode();
    } catch(NSPRException &ne) {
	Log::log(enginePtr->getModuleID(), Log::LOG_ERROR,
		 "am_policy_init: NSPRException inside %s when calling "
		 "NSPR method %s.",
		 ne.getThrowingMethod(), ne.getNsprMethod());
	return AM_NSPR_ERROR;
    } catch(std::bad_alloc) {
	Log::log(enginePtr->getModuleID(), Log::LOG_ERROR,
		 "am_policy_init: Memory allocation problem.");
	return AM_NO_MEMORY;
    } catch(std::exception &ex) {
	// Log:ERROR
	Log::log(enginePtr->getModuleID(), Log::LOG_ERROR, ex);
	return AM_FAILURE;
    } catch(...) {
	// Mother of all catches.
	Log::log(enginePtr->getModuleID(), Log::LOG_ERROR,
		 "am_policy_init: Unkown exception occured.");
	return AM_FAILURE;
    }

    return AM_SUCCESS;
}

/*
 * Method to close an initialized policy evaluator
 */
extern "C"
am_status_t am_policy_destroy(am_policy_t policy_handle)
{
    am_status_t retVal = AM_SUCCESS;

    try {
	enginePtr->destroy_service(policy_handle);
    } catch(InternalException &ie) {
	Log::log(enginePtr->getModuleID(), Log::LOG_ERROR,
		 "am_policy_destroy: InternalException in %s, Message is %s, Status code is %d.",
		 ie.getThrowingMethod(), ie.getMessage(),ie.getStatusCode());

	retVal = ie.getStatusCode();
    } catch(NSPRException &ne) {
	Log::log(enginePtr->getModuleID(), Log::LOG_ERROR,
		 "am_policy_destroy: NSPRException inside %s when calling "
		 "NSPR method %s.",
		 ne.getThrowingMethod(), ne.getNsprMethod());
	retVal = AM_NSPR_ERROR;
    } catch(std::bad_alloc) {
	Log::log(enginePtr->getModuleID(), Log::LOG_ERROR,
		 "am_policy_destroy: Memory allocation problem.");
	retVal = AM_NO_MEMORY;
    } catch(std::exception &ex) {
	// Log:ERROR
	Log::log(enginePtr->getModuleID(), Log::LOG_ERROR, ex);
	retVal = AM_FAILURE;
    } catch(...) {
	// Mother of all catches.
	Log::log(enginePtr->getModuleID(), Log::LOG_ERROR,
		 "am_policy_destroy: Unkown exception occured.");
	retVal = AM_FAILURE;
    }
    return retVal;
}

/**
 * Method to destroy am_policy_result_t.
 */
extern "C"
void am_policy_result_destroy(am_policy_result_t *result) {
    if(result != NULL) {
	if(result->advice_map != AM_MAP_NULL) {
	    am_map_destroy(result->advice_map);
	    result->advice_map = AM_MAP_NULL;
	}
	if(result->attr_profile_map != AM_MAP_NULL) {
	    am_map_destroy(result->attr_profile_map);
	    result->attr_profile_map = AM_MAP_NULL;
	}
	if(result->attr_session_map != AM_MAP_NULL) {
	    am_map_destroy(result->attr_session_map);
	    result->attr_session_map = AM_MAP_NULL;
	}
	if(result->attr_response_map != AM_MAP_NULL) {
	    am_map_destroy(result->attr_response_map);
	    result->attr_response_map = AM_MAP_NULL;
	}
    if (result->remote_user) {
        free((void *)result->remote_user);
        result->remote_user = NULL;
    }
    if (result->remote_user_passwd) {
        free((void *)result->remote_user_passwd);
        result->remote_user_passwd = NULL;
    }
	if(result->remote_IP) {
	    free((void *)result->remote_IP);
	    result->remote_IP = NULL;
	}
    }
    return;
}

/*
 * Method to evaluate a non-boolean policy question for a resource.
 */
extern "C" am_status_t
am_policy_evaluate(am_policy_t policy_handle,
		      const char *sso_token,
		      const char *resource_name,
		      const char *action_name,
		      const am_map_t env_table,
		      am_map_t policy_response_map_ptr,
		      am_policy_result_t *policy_res) {

    
    void* agent_config = am_web_get_agent_configuration();
    AgentConfigurationRefCntPtr* agentConfigPtr =
		(AgentConfigurationRefCntPtr*) agent_config;

    return am_policy_evaluate_ignore_url_notenforced(policy_handle,
		      sso_token,
		      resource_name,
		      action_name,
		      env_table,
		      policy_response_map_ptr,
		      policy_res,
		      AM_FALSE,
		      (*agentConfigPtr)->properties);
}

/*
 * Method to evaluate a non-boolean policy question for a resource.
 */
extern "C" am_status_t
am_policy_evaluate_ignore_url_notenforced(am_policy_t policy_handle,
		      const char *sso_token,
		      const char *resource_name,
		      const char *action_name,
		      const am_map_t env_table,
		      am_map_t policy_response_map_ptr,
		      am_policy_result_t *policy_res,
		      am_bool_t ignorePolicyResult,
		      am_properties_t properties) {
    try {
	enginePtr->policy_evaluate(policy_handle,
				   sso_token,
				   resource_name,
				   action_name,
				   env_table,
				   policy_response_map_ptr,
				   policy_res,
				   ignorePolicyResult,
				   *reinterpret_cast<Properties *>(properties));
    } catch(InternalException &ie) {
	Log::Level lvl = Log::LOG_ERROR;
	if(ie.getStatusCode() == AM_INVALID_SESSION) {
	    lvl = Log::LOG_INFO;
	} else  if(ie.getStatusCode() == AM_NO_POLICY) {
            lvl = Log::LOG_WARNING;
        }

	Log::log(enginePtr->getModuleID(), lvl,
		 "am_policy_evaluate: InternalException in %s with error message:%s and code:%d",
		 ie.getThrowingMethod(), ie.getMessage(), ie.getStatusCode());

	return ie.getStatusCode();
    } catch(NSPRException &ne) {
	Log::log(enginePtr->getModuleID(), Log::LOG_ERROR,
		 "am_policy_evaluate: NSPRException inside %s when "
		 "calling NSPR method %s",
		 ne.getThrowingMethod(), ne.getNsprMethod());
	return AM_NSPR_ERROR;
    } catch(std::bad_alloc &ex) {
	Log::log(enginePtr->getModuleID(), Log::LOG_ERROR,
		 "am_policy_evaluate: Memory allocation problem.");
	return AM_NO_MEMORY;
    } catch(std::exception &ex) {
	// Log:ERROR
	Log::log(enginePtr->getModuleID(), Log::LOG_ERROR, ex);
	return AM_FAILURE;
    } catch(...) {
	// Mother of all catches.
	Log::log(enginePtr->getModuleID(), Log::LOG_ERROR,
		 "am_policy_evaluate: Unknown exception occured.");
	return AM_FAILURE;
    }

    return AM_SUCCESS;
}

/*
 * Method to check if notification is enabled in the SDK.
 *
 * Returns:
 *  If notification is enabled returns non-zero, otherwise zero.
 */
extern "C" boolean_t
am_policy_is_notification_enabled(am_policy_t policy_handle) {
    try {
	return (enginePtr->isNotificationEnabled(policy_handle)==true)?B_TRUE:B_FALSE;
    }  catch(InternalException &ie) {
	Log::log(enginePtr->getModuleID(), Log::LOG_ERROR,
		 "am_policy_is_notification_enabled: InternalException in %s "
		 "with error message:%s and code:%s",
		 ie.getThrowingMethod(), ie.getMessage(),
		 am_status_to_string(ie.getStatusCode()));
    } catch(NSPRException &ne) {
	Log::log(enginePtr->getModuleID(), Log::LOG_ERROR,
		 "am_policy_is_notification_enabled: NSPRException inside %s "
		 "when calling NSPR method %s.",
		 ne.getThrowingMethod(), ne.getNsprMethod());
    } catch(std::bad_alloc &ex) {
	Log::log(enginePtr->getModuleID(), Log::LOG_ERROR,
	 "am_policy_is_notification_enabled: Memory allocation problem.");
    } catch(std::exception &ex) {
	// Log:ERROR
	Log::log(enginePtr->getModuleID(), Log::LOG_ERROR, ex);
    } catch(...) {
	// Mother of all catches.
	Log::log(enginePtr->getModuleID(), Log::LOG_ERROR,
		 "am_policy_is_notification_enabled: Unknown exception occured.");
	return B_FALSE;
    }

    return B_FALSE;
}


/*
 * Method to refresh policy cache when a policy notification is received
 * by the client.
 */
extern "C" am_status_t
am_policy_notify(am_policy_t policy_handle,
		    const char *notification_data,
		    size_t notification_data_len,
                    boolean_t configChangeNotificationEnabled)
{
    try {
	enginePtr->policy_notify(policy_handle, notification_data,
				 notification_data_len,
                                 configChangeNotificationEnabled ? true : false);
    } catch(InternalException &ie) {
	Log::log(enginePtr->getModuleID(), Log::LOG_ERROR,
		 "am_policy_notify: InternalException in %s with error message:%s and code:%d",
		 ie.getThrowingMethod(), ie.getMessage(), ie.getStatusCode());
	return ie.getStatusCode();
    } catch(NSPRException &ne) {
	Log::log(enginePtr->getModuleID(), Log::LOG_ERROR,
		 "am_policy_notify(): NSPRException inside %s "
		 "when calling NSPR method %s.",
		 ne.getThrowingMethod(), ne.getNsprMethod());
	return AM_NSPR_ERROR;
    } catch(std::bad_alloc &ex) {
	Log::log(enginePtr->getModuleID(), Log::LOG_ERROR,
		 "am_policy_notify(): Memory allocation problem.");
	return AM_NO_MEMORY;
    } catch(std::exception &ex) {
	// Log:ERROR
	Log::log(enginePtr->getModuleID(), Log::LOG_ERROR, ex);
	return AM_FAILURE;
    } catch(...) {
	// Mother of all catches.
	Log::log(enginePtr->getModuleID(), Log::LOG_ERROR,
		 "am_policy_notify(): Unknown error occured.");
	return AM_FAILURE;
    }
    return AM_SUCCESS;
}

const char *am_resource_match_to_string(int rm)
{
    const char *str = NULL;
    switch (rm) {
        case 0:
            str = "AM_SUB_RESOURCE_MATCH";
            break;
        case 1:
            str = "AM_EXACT_MATCH";
            break;
        case 2:
            str = "AM_SUPER_RESOURCE_MATCH";
            break;
        case 3:
            str = "AM_NO_MATCH";
            break;
        case 4:
            str = "AM_EXACT_PATTERN_MATCH";
            break;
    }
    return str;
}

extern "C" am_resource_match_t
am_policy_compare_urls(const am_resource_traits_t *rsrcTraits,
		       const char *policyResourceName,
		       const char *resourceName,
		       boolean_t usePatterns)
{
    const char *thisfunc = "am_policy_compare_urls";
    Log::ModuleId logID = Log::addModule(AM_POLICY_SERVICE);
    am_resource_match_t ret = AM_NO_MATCH;
    try {
        std::string urls(policyResourceName);
        if (urls.find("*", 0) != std::string::npos) {
            if (urls == "*" || urls.find(" *", 0) != std::string::npos || urls.find("* ", 0) != std::string::npos) {
                /**
                 * current pattern matching algorithm forbids:
                 * - wildcard only (i.e. "all allowed")
                 * - white-space before/after wildcard, though this is unlikely to be matched, because url list 
                 *   is passed down to an agent as space separated value object
                 */
                Log::log(logID, Log::LOG_MAX_DEBUG,
                        "%s: Comparison of \"%s\" and \"%s\" returned AM_NO_MATCH (invalid matching pattern)",
                        thisfunc, resourceName,
                        policyResourceName);
                return AM_NO_MATCH;
            }
        }
        ret = Utils::compare(policyResourceName, resourceName,
			     rsrcTraits,
			     true, usePatterns==B_TRUE);
	Log::log(logID, Log::LOG_MAX_DEBUG,
             "%s: Comparison of \"%s\" and \"%s\" returned %s "
             "(usePatterns=%s)", thisfunc, resourceName,
             policyResourceName, am_resource_match_to_string(ret),
             usePatterns==B_TRUE?"true":"false");
    } 
    catch (std::exception& ex) {
	Log::log(logID, Log::LOG_ERROR,
		 "am_policy_compare_urls(): unexpected exception "
		 "[%s] encountered. Returning no match.", ex.what());
    }
    catch (...) {
	Log::log(logID, Log::LOG_ERROR,
		 "am_policy_compare_urls(): unexpected exception "
		 "encountered. Returning no match.");
	ret = AM_NO_MATCH;
    }
    return ret;
}

extern "C" boolean_t
am_policy_get_url_resource_root(const char *resourceName,
				   char *resourceRoot, size_t length)
{
    boolean_t status = B_FALSE;
    const char *func = "am_policy_get_url_resource_root()";
    if (resourceRoot != NULL) {
	try {
	    URL urlObject(resourceName, strlen(resourceName));

	    std::string root;
	    urlObject.getRootURL(root);

	    // If there is enough room for the URL and the terminating
	    // NUL, then everything is okay.
	    if (root.size() < length) {
		std::strcpy(resourceRoot, root.c_str());
		status = B_TRUE;
	    }
	} catch(const std::exception &ex) {
	    Log::log(Log::ALL_MODULES, Log::LOG_ERROR,
		     "%s: Exception %s thrown.", func, ex.what());
	} catch(...) {
	    Log::log(Log::ALL_MODULES, Log::LOG_ERROR,
		     "%s: Unknown exception thrown.", func);
	}
    }

    return status;
}

extern "C" boolean_t
am_policy_resource_has_patterns(const char *resourceName) {
    if(resourceName == NULL)
	return B_FALSE;
    return (strchr(resourceName, '*') != NULL)?B_TRUE:B_FALSE;
}

extern "C" void
am_policy_resource_canonicalize(const char *resource, char **c_resource) {
    try {
	std::string urlStr;
	URL url(resource);
    url.getCanonicalizedURLString(urlStr); 
	*c_resource = strdup(urlStr.c_str());
    } catch(InternalException &ex) {
	Log::log(Log::ALL_MODULES, Log::LOG_ERROR, ex);
    } catch (...) {
	Log::log(Log::ALL_MODULES, Log::LOG_ERROR,
		 "am_policy_resource_canonicalize(): Unexpected exception.");
    }
    return;
}

//
// Note this function is not exposed in api.
// It needs to be here to access enginePtr.
// if we make enginePtr a singleton wouldn't have to do this.
//
am_status_t
am_policy_handle_notification(am_policy_t policy_handle,
                              const std::string& notifData)
{
    Log::ModuleId logID = Log::addModule(AM_POLICY_SERVICE);
    am_status_t sts = AM_FAILURE;

    if (NULL==enginePtr) {
        Log::log(logID, Log::LOG_ERROR,
                 "am_policy_handle_notification(): service not initialized.");
        sts = AM_SERVICE_NOT_INITIALIZED;
    }
    else if (policy_handle || notifData.size()==0) {
        Log::log(logID, Log::LOG_ERROR,
                 "am_policy_handle_notification(): "
                 "one or more input parameters is invalid.");
        sts = AM_INVALID_ARGUMENT;
    }
    else {
        try {
            enginePtr->handleNotif(policy_handle, notifData);
        }
        catch (InternalException& exc) {
            sts = exc.getStatusCode();
            Log::log(logID, Log::LOG_ERROR,
                     "am_policy_handle_notification(): "
                     "Internal Exception encountered: '%s'",
                     exc.getMessage());
        }
        catch (NSPRException& exc) {
            Log::log(logID, Log::LOG_ERROR,
                     "am_policy_handle_notification(): "
                     "NSPR Exception encountered.");
            Log::log(logID, Log::LOG_ERROR, exc);
            sts = AM_NSPR_ERROR;
        }
        catch (std::exception& exc) {
            Log::log(logID, Log::LOG_ERROR,
                     "am_policy_handle_notification(): "
                     "Unknown Exception %s encountered.", exc.what());
            Log::log(logID, Log::LOG_ERROR, exc);
        }
        catch (...) {
            Log::log(logID, Log::LOG_ERROR,
                     "am_policy_handle_notification(): "
                     "Unknown Exception encountered.");
	}
    }
    return sts;
}

extern "C" am_status_t
am_policy_invalidate_session(am_policy_t policy_handle,
                             const char *ssoTokenId)
{
    Log::ModuleId logID = Log::addModule(AM_POLICY_SERVICE);
    am_status_t status = AM_FAILURE;
    try {
        status = enginePtr->invalidate_session(policy_handle, ssoTokenId);
    }
    catch (InternalException& ex) {
	Log::log(logID, Log::LOG_ERROR,
		 "am_policy_invalidate_session(): "
		 "Internal Exception encountered: '%s'",
		 ex.getMessage());
	status = ex.getStatusCode();
    }
    catch (NSPRException& ex) {
	Log::log(logID, Log::LOG_ERROR,
		 "am_policy_invalidate_session(): "
		 "NSPR Exception encountered");
	Log::log(logID, Log::LOG_ERROR, ex);
	status = AM_NSPR_ERROR;
    }
    catch (std::exception& ex) {
	Log::log(logID, Log::LOG_ERROR,
		 "am_policy_invalidate_session(): "
		 "Unknown Exception encountered: %s", ex.what());
	Log::log(logID, Log::LOG_ERROR, ex);
        status = AM_FAILURE;
    }
    catch (...) {
	Log::log(logID, Log::LOG_ERROR,
		 "am_policy_invalidate_session(): "
		 "Unknown Exception encountered.");
        status = AM_FAILURE;
    }
    return status;
}

extern "C" am_status_t
am_policy_user_logout(am_policy_t policy_handle,
                             const char *ssoTokenId,
                             am_properties_t properties) 
{
    Log::ModuleId logID = Log::addModule(AM_POLICY_SERVICE);
    am_status_t status = AM_FAILURE;
    try {
        status = enginePtr->user_logout(policy_handle, 
                     ssoTokenId,
                     *reinterpret_cast<Properties *>(properties));
    } catch (InternalException& ex) {
	Log::log(logID, Log::LOG_ERROR,
		 "am_policy_user_logout(): "
		 "Internal Exception encountered: '%s'",
		 ex.getMessage());
	status = ex.getStatusCode();
    }
    catch (NSPRException& ex) {
	Log::log(logID, Log::LOG_ERROR,
		 "am_policy_user_logout(): "
		 "NSPR Exception encountered");
	Log::log(logID, Log::LOG_ERROR, ex);
	status = AM_NSPR_ERROR;
    }
    catch (std::exception& ex) {
	Log::log(logID, Log::LOG_ERROR,
		 "am_policy_user_logout(): "
		 "Unknown Exception encountered: %s", ex.what());
	Log::log(logID, Log::LOG_ERROR, ex);
        status = AM_FAILURE;
    }
    catch (...) {
	Log::log(logID, Log::LOG_ERROR,
		 "am_policy_user_logout(): "
		 "Unknown Exception encountered.");
        status = AM_FAILURE;
    }
    return status;
}
