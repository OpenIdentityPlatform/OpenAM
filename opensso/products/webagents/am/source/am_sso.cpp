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
 * $Id: am_sso.cpp,v 1.8 2008/09/13 01:11:52 robertis Exp $
 *
 */
/*
 * Portions Copyrighted 2013 ForgeRock Inc
 */

#include <stdexcept>
#include <am_sso.h>
#include <string>
#include "internal_macros.h"
#include "log.h"
#include "sso_token.h"
#include "sso_token_service.h"
#include "utils.h"
#include "properties.h"
#include "am_properties.h"
#include "internal_exception.h"
#include "http.h"
#if !defined(_MSC_VER)
#include <limits.h>
#endif

BEGIN_PRIVATE_NAMESPACE
static SSOTokenService *SSOTokenSvc = NULL;
static Log::ModuleId ssoHdlModule;
boolean_t initializeLog = B_TRUE;
DEFINE_BASE_INIT;
void sso_cleanup();
END_PRIVATE_NAMESPACE

USING_PRIVATE_NAMESPACE

#define SESSION_DEBUG_MODULE "AM_SSO"
#define SESSION_SERVICE_NAME "AM_SSO_SERVICE"

SSOTokenService *get_sso_token_service() {
    return SSOTokenSvc;
}

extern "C"
am_status_t
am_sso_init(am_properties_t sso_config_params) {
    am_status_t status = AM_SUCCESS;
    ssoHdlModule = Log::addModule(SESSION_DEBUG_MODULE);
    if (sso_config_params) {
	const Properties& propertiesRef =
	    *reinterpret_cast<Properties *>(sso_config_params);

	try {
	    if(SSOTokenSvc == NULL) {
		base_init(propertiesRef, initializeLog);
		SSOTokenSvc = new SSOTokenService(SESSION_SERVICE_NAME,
                                                  propertiesRef);
	    } else {
		Log::log(ssoHdlModule, Log::LOG_WARNING,
			 "am_sso_init() : Session service "
			 "previously initialized.");
	    }
	}
        catch (InternalException& exc) {
	    status = exc.getStatusCode();
	    Log::log(ssoHdlModule, Log::LOG_ERROR, exc);
	}
        catch (const std::bad_alloc& exb) {
            status = AM_NO_MEMORY;
	    Log::log(ssoHdlModule, Log::LOG_ERROR, exb);
        }
        catch (const std::exception& exs) {
            status = AM_FAILURE;
	    Log::log(ssoHdlModule, Log::LOG_ERROR, exs);
        }
        catch (...) {
            status = AM_FAILURE;
	    Log::log(ssoHdlModule, Log::LOG_ERROR,
                     "Unknown exception encountered.");
        }
    } else {
	Log::log(ssoHdlModule, Log::LOG_ERROR,
                 "am_sso_init(): "
                 "One or more input parameters is invalid.");
	status = AM_INVALID_ARGUMENT;
    }
    return status;
}

extern "C"
void am_sso_set_initializeLog(boolean_t value) {
	initializeLog = value;
}

/**
 * cleanup sso module.
 * Throws: InternalException
 */
void PRIVATE_NAMESPACE_NAME::sso_cleanup() {
    delete(SSOTokenSvc);
    SSOTokenSvc = NULL;
    return;
}

extern "C"
am_status_t
am_sso_create_sso_token_handle(am_sso_token_handle_t *sso_token_handle,
			       const char *sso_token_id,
			       boolean_t reset_idle_timer)
{
    am_status_t retVal = AM_SUCCESS;

    if (NULL == SSOTokenSvc) {
        Log::log(ssoHdlModule, Log::LOG_ERROR,
                 "am_sso_create_sso_token_handle(): "
                 "SSO Service not initialized.");
        retVal = AM_SERVICE_NOT_INITIALIZED;
    }

    if (NULL == sso_token_handle) {
        Log::log(ssoHdlModule, Log::LOG_ERROR,
                 "am_sso_create_sso_token_handle(): "
                 "sso_token_handle parameter has an invalid value.");
        retVal = AM_INVALID_ARGUMENT;
    }

    if (NULL == sso_token_id) {
        retVal = AM_INVALID_SESSION;
    }

    if(retVal == AM_SUCCESS) {
	*sso_token_handle = NULL;
	SessionInfo *session = NULL;
	try {
            session = new SessionInfo();
	    Http::CookieList cookieList;
	    retVal = SSOTokenSvc->getSessionInfo(
			     ServiceInfo(),
			     sso_token_id,
			     cookieList,
			     reset_idle_timer==B_TRUE?true:false,
			     *session);
            if (retVal != AM_SUCCESS) {
		delete session;
		Log::log(ssoHdlModule, Log::LOG_ERROR,
			 "am_sso_create_sso_token_handle(): "
			 "Create session for sso token ID %s failed with %d.",
                         sso_token_id, retVal);
	    } else {
		*sso_token_handle =
		    reinterpret_cast<am_sso_token_handle_t>(session);
	    }
        }
        catch(std::bad_alloc &be) {
	    Log::log(ssoHdlModule, Log::LOG_ERROR,
		     "am_sso_create_sso_token_handle(): "
		     "Memory allocation failure while creating session for %s.",
                     sso_token_id);
	    return AM_NO_MEMORY;
	}
	catch(...) {
	    delete session;
	    Log::log(ssoHdlModule, Log::LOG_ERROR,
		     "am_sso_create_sso_token_handle(): "
		     "Unknown exception thrown while creating session for %s.",
                     sso_token_id);
	    retVal = AM_FAILURE;
	}
    }
    return retVal;
}



extern "C"
am_status_t
am_sso_destroy_sso_token_handle(am_sso_token_handle_t sso_token_handle)
{
    am_status_t retVal = AM_FAILURE;

    if(sso_token_handle != NULL) {
	try {
	    SessionInfo *sessionInfo = reinterpret_cast<SessionInfo *>(sso_token_handle);
	    delete sessionInfo;
	    retVal = AM_SUCCESS;
	} catch(...) {
	    Log::log(ssoHdlModule, Log::LOG_ERROR,
		     "am_sso_destroy_sso_token_handle(): "
		     "Unknown exception thrown.");
	}
    } else {
	retVal = AM_INVALID_ARGUMENT;
	Log::log(ssoHdlModule, Log::LOG_ERROR,
		 "am_sso_create_sso_token_handle(): "
		 "One or more input parameters has an invalid value.");
    }
    return retVal;
}

/*
 * invalidates session on server.
 */
extern "C"
am_status_t
am_sso_invalidate_token(const am_sso_token_handle_t sso_token_handle)
{
    const char *thisfunc = "am_sso_invalidate_token()";
    am_status_t sts = AM_FAILURE;
    if (NULL==sso_token_handle) {
	Log::log(ssoHdlModule, Log::LOG_ERROR,
		 "%s: One or more input parameters has an invalid value.",
		 thisfunc);
        sts = AM_INVALID_ARGUMENT;
    }
    else if (NULL==SSOTokenSvc) {
	Log::log(ssoHdlModule, Log::LOG_ERROR,
		 "sso_token_id(): "
		 "SSO Service not initialized.");
        sts = AM_SERVICE_NOT_INITIALIZED;
    }
    else {
	try {
	    SessionInfo *sessionInfo =
                (reinterpret_cast<SessionInfo *>(sso_token_handle));
	    sts = SSOTokenSvc->destroySession(ServiceInfo(),
					      *sessionInfo);
	}
	catch (std::bad_alloc& exb) {
	    Log::log(ssoHdlModule, Log::LOG_ERROR,
	       "%s: Bad alloc exception thrown: %s", thisfunc, exb.what());
	    sts = AM_NO_MEMORY;
	}
	catch (...) {
	    Log::log(ssoHdlModule, Log::LOG_ERROR,
		     "%s: Unknown exception thrown.", thisfunc);
	    sts = AM_FAILURE;
	}
    }
    return sts;
}

extern "C"
unsigned long
am_sso_get_auth_level(const am_sso_token_handle_t sso_token_handle)
{
    unsigned long retVal = ULONG_MAX;

    if(sso_token_handle != NULL) {
	try {
	    const SessionInfo &sessionInfo = *(reinterpret_cast<const SessionInfo *>(sso_token_handle));
	    if(sessionInfo.isValid()) {
		const Properties &properties = sessionInfo.getProperties();
		retVal = (unsigned long) Utils::getNumber(properties.get("AuthLevel"));
	    }
	} catch(...) {
	    Log::log(ssoHdlModule, Log::LOG_ERROR,
		     "am_sso_get_auth_level(): "
		     "Unknown exception thrown.");
	    retVal = ULONG_MAX;
	}
    } else {
	retVal = ULONG_MAX;
	Log::log(ssoHdlModule, Log::LOG_ERROR,
		 "am_sso_get_auth_level(): "
		 "One or more input parameters has an invalid value.");
    }
    return retVal;
}

extern "C"
const char *
am_sso_get_auth_type(const am_sso_token_handle_t sso_token_handle)
{
    const char * retVal = NULL;
    if(sso_token_handle != NULL) {
	try {
	    const SessionInfo &sessionInfo = *(reinterpret_cast<const SessionInfo *>(sso_token_handle));
	    if(sessionInfo.isValid()) {
		const Properties &properties = sessionInfo.getProperties();
		retVal = properties.get("AuthType").c_str();
	    }
	} catch(...) {
	    Log::log(ssoHdlModule, Log::LOG_ERROR,
		     "am_sso_get_auth_type(): "
		     "Unknown exception thrown.");
	    retVal = NULL;
	}
    } else {
	retVal = NULL;
	Log::log(ssoHdlModule, Log::LOG_ERROR,
		 "am_sso_get_auth_type(): "
		 "One or more input parameters has an invalid value.");
    }
    return retVal;
}

extern "C"
const char *
am_sso_get_host(const am_sso_token_handle_t sso_token_handle)
{
    const char * retVal = NULL;
    if(sso_token_handle != NULL) {

	try {
	    const SessionInfo &sessionInfo = *(reinterpret_cast<const SessionInfo *>(sso_token_handle));
	    if(sessionInfo.isValid()) {
		const Properties &properties = sessionInfo.getProperties();
		retVal = properties.get("Host").c_str();
	    }
	} catch(...) {
	    Log::log(ssoHdlModule, Log::LOG_ERROR,
		     "am_sso_get_host(): "
		     "Unknown exception thrown.");
	    retVal = NULL;
	}
    } else {
	retVal = NULL;
	Log::log(ssoHdlModule, Log::LOG_ERROR,
		 "am_sso_get_host(): "
		 "One or more input parameters has an invalid value.");
    }
    return retVal;
}

extern "C"
const char *
am_sso_get_principal(const am_sso_token_handle_t sso_token_handle)
{
    const char * retVal = NULL;
    if(sso_token_handle != NULL) {
	try {
	    const SessionInfo &sessionInfo = *(reinterpret_cast<const SessionInfo *>(sso_token_handle));
	    if(sessionInfo.isValid()) {
		const Properties &properties = sessionInfo.getProperties();
		retVal = properties.get("Principal").c_str();
	    }
	} catch(...) {
	    Log::log(ssoHdlModule, Log::LOG_ERROR,
		     "am_sso_get_principal(): "
		     "Unknown exception thrown.");
	    retVal = NULL;
	}
    } else {
	retVal = NULL;
	Log::log(ssoHdlModule, Log::LOG_ERROR,
		 "am_sso_get_principal(): "
		 "One or more input parameters has an invalid value.");
    }
    return retVal;
}

/*
 * This routine is not exposed in public api.
 *
 * Parses a string expected to be a set of strings seperated by the
 * given character and returns the set of strings in a am_string_set.
 * white space is ignored.
 *
 * Parameters
 *     str
 *        string that is a set of strings seperated by a char such as '|'
 *     sep
 *        seperator character.
 *
 * Returns:
 *     a am_string_set_t from parsing.
 *
 * Examples:
 *     "A|B|C" results in a set of strings "A", "B", "C".
 *     "A|B|" results in a set of strings "A", "B", "".
 *
 */
extern "C"
am_string_set_t * am_get_char_seperated_string_set(const char *str, char sep)
{
    am_string_set_t *ret = NULL;
    int n_sep = 0;
    const char *ptr, *beg;
    int64_t len;
    char c;
    int i;

    /* find # of strings in the char seperated set */
    ptr = str;
    while ((c = *ptr++)) {
	if (c == sep)
	    n_sep++;
    }
    n_sep++;

    ret = am_string_set_allocate(n_sep);

    /* fill set */
    ptr = str;
    beg = str;
    i = 0;
    while ((c = *ptr++) && i < n_sep) {
	if (c == sep) {
            len = ptr-beg-1;
            ret->strings[i] = (char *)malloc(len+1);
            strncpy(ret->strings[i], beg, len);
            ret->strings[i][len] = '\0';
            beg = ptr;
            i++;
        }
    }
    len = ptr-beg;
    ret->strings[i] = (char *)malloc(len);
    strcpy(ret->strings[i], beg);

    return ret;
}

extern "C"
am_string_set_t *
am_sso_get_principal_set(const am_sso_token_handle_t sso_token_handle)
{
    am_string_set_t *retVal = NULL;
    if(sso_token_handle != NULL) {
	try {
	    const SessionInfo &sessionInfo = *(reinterpret_cast<const SessionInfo *>(sso_token_handle));
	    if(sessionInfo.isValid()) {
		const Properties &properties = sessionInfo.getProperties();
		const char *value = properties.get("Principals").c_str();
                if (NULL==value || *value == '\0') {
		    Log::log(ssoHdlModule, Log::LOG_DEBUG,
		             "am_sso_get_principal_set(): "
		             "empty string principal set for token ID %s.",
                             sessionInfo.getSSOToken().getString().c_str());
                }
                else {
                    retVal = am_get_char_seperated_string_set(value, '|');
		    Log::log(ssoHdlModule, Log::LOG_DEBUG,
			     "am_sso_get_principal_set(): "
			     "session ID %s has %d strings, first is %s.",
                             sessionInfo.getSSOToken().getString().c_str(),
                             retVal->size,
                             retVal->strings[0]);
                }
	    }
            else {
		Log::log(ssoHdlModule, Log::LOG_DEBUG,
			 "am_sso_get_principal_set(): "
			 "session ID %s is invalid.",
                         sessionInfo.getSSOToken().getString().c_str());
            }
	} catch(...) {
	    Log::log(ssoHdlModule, Log::LOG_ERROR,
		     "am_sso_get_principal_set(): "
		     "Unknown exception thrown.");
	}
    } else {
	Log::log(ssoHdlModule, Log::LOG_ERROR,
		 "am_sso_get_principal_set(): "
		 "One or more input parameters has an invalid value.");
    }
    return retVal;
}


extern "C"
time_t
am_sso_get_max_idle_time(const am_sso_token_handle_t sso_token_handle)
{
    time_t retVal = (time_t)-1;
    if(sso_token_handle != NULL) {
	try {
	    const SessionInfo &sessionInfo = *(reinterpret_cast<const SessionInfo *>(sso_token_handle));
	    if(sessionInfo.isValid()) {
                // server returned this in minutes, so convert it to seconds
		// to be consistent with other interfaces that return time,
		// and with definition of time_t.
		retVal = (time_t)(sessionInfo.getMaxIdleTime()*60);
	    }
	} catch(...) {
	    Log::log(ssoHdlModule, Log::LOG_ERROR,
		     "am_sso_get_max_idle_time(): "
		     "Unknown exception thrown.");
	}
    } else {
	Log::log(ssoHdlModule, Log::LOG_ERROR,
		 "am_sso_get_max_idle_time(): "
		 "One or more input parameters has an invalid value.");
    }
    return (time_t)retVal;
}


extern "C"
time_t
am_sso_get_max_session_time(const am_sso_token_handle_t sso_token_handle) {
    time_t retVal = (time_t)-1;
    if(sso_token_handle != NULL) {
	try {
	    const SessionInfo &sessionInfo = *(reinterpret_cast<const SessionInfo *>(sso_token_handle));
	    if(sessionInfo.isValid()) {
                // server returned this in minutes, so convert it to seconds
		// to be consistent with other interfaces that return time,
		// and with definition of time_t.
		retVal = (time_t)(sessionInfo.getMaxSessionTime()*60);
	    }
	} catch(...) {
	    Log::log(ssoHdlModule, Log::LOG_ERROR,
		     "am_sso_get_max_session_time(): "
		     "Unknown exception thrown.");
	}
    } else {
	Log::log(ssoHdlModule, Log::LOG_ERROR,
		 "am_sso_get_max_session_time(): "
		 "One or more input parameters has an invalid value.");
    }
    return (time_t)retVal;
}


extern "C"
const char *
am_sso_get_property(const am_sso_token_handle_t sso_token_handle,
		    const char *property_key,
		    boolean_t check_if_session_valid)
{
    const char * retVal = NULL;
    if(sso_token_handle != NULL && property_key != NULL) {

	try {
	    const SessionInfo &sessionInfo = *(reinterpret_cast<const SessionInfo *>(sso_token_handle));
	    if(B_FALSE == check_if_session_valid || sessionInfo.isValid()) {
		const Properties &properties = sessionInfo.getProperties();
		retVal = properties.get(property_key).c_str();
	    }
	} catch(...) {
	    Log::log(ssoHdlModule, Log::LOG_ERROR,
		     "am_sso_get_property(): Key %s not found in property.", 
                     property_key);
	}
    } else {
	retVal = 0;
	Log::log(ssoHdlModule, Log::LOG_ERROR,
		 "am_sso_get_property(): "
		 "One or more input parameters has an invalid value.");
    }
    return retVal;
}


extern "C"
const char *
am_sso_get_sso_token_id(const am_sso_token_handle_t sso_token_handle)
{
    const char * retVal = NULL;
    if(sso_token_handle != NULL) {
	if (NULL==SSOTokenSvc) {
	    Log::log(ssoHdlModule, Log::LOG_ERROR,
		     "am_sso_get_sso_token_id(): "
		     "SSO Service not initialized.");
	    return NULL;
	}
	try {
	    const SessionInfo &sessionInfo = *(reinterpret_cast<const SessionInfo *>(sso_token_handle));
            const SSOToken& ssoTok = sessionInfo.getSSOToken();

	    retVal = ssoTok.getString().c_str();
	} catch(...) {
	    Log::log(ssoHdlModule, Log::LOG_ERROR,
		     "am_sso_get_sso_token_id(): "
		     "Unknown exception thrown.");
	}
    } else {
	Log::log(ssoHdlModule, Log::LOG_ERROR,
		 "am_sso_get_sso_token_id(): "
		 "One or more input parameters has an invalid value.");
    }
    return retVal;
}

/*
 * just returns local state of token handle.
 * does not call server.
 */
extern "C"
boolean_t
am_sso_is_valid_token(const am_sso_token_handle_t sso_token_handle)
{
    boolean_t retVal = B_FALSE;
    if (NULL==sso_token_handle) {
        Log::log(ssoHdlModule, Log::LOG_ERROR,
                 "am_sso_is_valid_token(): "
                 "One or more input parameters is NULL or invalid.");
    }
    else {
        SessionInfo *sessionInfo = reinterpret_cast<SessionInfo *>(sso_token_handle);
        retVal = sessionInfo->isValid() ? B_TRUE : B_FALSE;
    }
    return retVal;
}

/*
 * will get session from server and
 * return session state as provided by server.
 * also renews the session in the local cache.
 */
extern "C"
am_status_t
am_sso_validate_token(const am_sso_token_handle_t sso_token_handle)
{
    const char *thisfunc = "am_sso_validate_token()";
    am_status_t sts = AM_FAILURE;

    if (NULL==SSOTokenSvc) {
        Log::log(ssoHdlModule, Log::LOG_ERROR,
                 "%s: SSO is not initialized.", thisfunc);
        sts = AM_SERVICE_NOT_INITIALIZED;
    }
    else if (NULL==sso_token_handle) {
        Log::log(ssoHdlModule, Log::LOG_ERROR,
                 "%s: One or more input parameters is NULL or invalid.",
		 thisfunc);
        sts = AM_INVALID_ARGUMENT;
    }
    else {
	try {
	    SessionInfo *sessionInfo=
			    reinterpret_cast<SessionInfo *>(sso_token_handle);
            Http::CookieList cookieList;
	    std::string ssoTokenId = sessionInfo->getSSOToken().getString();
	    sts = SSOTokenSvc->getSessionInfo(ServiceInfo(),
					      ssoTokenId,
					      cookieList,
					      false,
					      *sessionInfo,
					      true);
	    if (sts == AM_SUCCESS) {
		sts = sessionInfo->isValid() ? AM_SUCCESS : AM_INVALID_SESSION;
	    }
	    else {
		Log::log(ssoHdlModule, Log::LOG_ERROR,
			 "%s: Internal error while validating SSO token ID %s.",
			 thisfunc, ssoTokenId.c_str());
	    }
	}
	catch (InternalException& ex) {
	    Log::log(ssoHdlModule, Log::LOG_ERROR, ex);
	    sts = AM_FAILURE;
	}
	catch (std::bad_alloc& exb) {
	    Log::log(ssoHdlModule, Log::LOG_ERROR, exb);
	    sts = AM_NO_MEMORY;
	}
	catch (std::exception& exs) {
	    Log::log(ssoHdlModule, Log::LOG_ERROR, exs);
	    sts = AM_FAILURE;
	}
	catch (...) {
	    Log::log(ssoHdlModule, Log::LOG_ERROR,
		     "%s: Unknown exception encountered.");
	    sts = AM_FAILURE;
	}
    }
    return sts;
}


/*
 * will get session from server and
 * return session state as provided by server.
 * also renews the session in the local cache.
 */
extern "C"
am_status_t
am_sso_refresh_token(const am_sso_token_handle_t sso_token_handle)
{
    const char *thisfunc = "am_sso_refresh_token()";
    am_status_t sts = AM_FAILURE;

    if (NULL==SSOTokenSvc) {
        Log::log(ssoHdlModule, Log::LOG_ERROR,
                 "am_sso_refresh_token(): "
                 "SSO is not initialized.");
        sts = AM_SERVICE_NOT_INITIALIZED;
    }
    else if (NULL==sso_token_handle) {
        Log::log(ssoHdlModule, Log::LOG_ERROR,
                 "am_sso_refresh_token(): "
                 "One or more input parameters is NULL or invalid.");
        sts = AM_INVALID_ARGUMENT;
    }
    else {
	try {
	    SessionInfo *sessionInfo=
		reinterpret_cast<SessionInfo *>(sso_token_handle);
            Http::CookieList cookieList;
	    std::string ssoTokenId = sessionInfo->getSSOToken().getString();
	    sts = SSOTokenSvc->getSessionInfo(ServiceInfo(),
					      ssoTokenId,
					      cookieList,
					      true,
					      *sessionInfo,
					      true);
	    if (sts == AM_SUCCESS) {
		sts = sessionInfo->isValid() ? AM_SUCCESS : AM_INVALID_SESSION;
	    }
	    else {
		Log::log(ssoHdlModule, Log::LOG_ERROR,
			 "%s: Internal error while validating SSO token ID %s.",
			 thisfunc, ssoTokenId.c_str());
	    }
	}
	catch (InternalException& ex) {
	    sts = ex.getStatusCode();
	}
	catch (std::exception& exs) {
	    Log::log(ssoHdlModule, Log::LOG_ERROR,
		     "%s: Unexpected exception [%s] encountered. ",
		     thisfunc, exs.what());
	    sts = AM_FAILURE;
	}
	catch (...) {
	    Log::log(ssoHdlModule, Log::LOG_ERROR,
		     "%s: Unknown exception encountered", thisfunc);
	    sts = AM_FAILURE;
	}

    }
    return sts;
}

/*
 * returns -1 on any kind of error.
 * returns time left in the sso token handle.
 * does not call server.
 */
extern "C"
time_t
am_sso_get_time_left(const am_sso_token_handle_t sso_token_handle)
{
    time_t retVal = (time_t)-1;
    if (NULL==sso_token_handle) {
	Log::log(ssoHdlModule, Log::LOG_ERROR,
		 "am_sso_get_time_left(): "
		 "One or more input parameters is NULL.");
    }
    else {
	try {
	    const SessionInfo &sessionInfo = *(reinterpret_cast<const SessionInfo *>(sso_token_handle));
	    if(sessionInfo.isValid()) {
		/* PRTime corresponds to time_t in ANSI C per prtime.h */
		retVal = (time_t)sessionInfo.getRemainingSessionTime();
	    }
	} catch(...) {
	    Log::log(ssoHdlModule, Log::LOG_ERROR,
		     "am_sso_get_time_left(): "
		     "Unknown exception thrown.");
	}
    }
    return retVal;
}

/*
 * returns -1 on any kind of error.
 * returns idle time in the sso token handle.
 * does not call server.
 */
extern "C"
time_t
am_sso_get_idle_time(const am_sso_token_handle_t sso_token_handle)
{
    time_t retVal = (time_t)-1;
    if(sso_token_handle != NULL) {
	try {
	    const SessionInfo &sessionInfo = *(reinterpret_cast<const SessionInfo *>(sso_token_handle));
	    if(sessionInfo.isValid()) {
                /* PRTime corresponds to time_t in ANSI C per prtime.h */
                retVal = (time_t)sessionInfo.getIdleTime();
	    }
	} catch(...) {
	    Log::log(ssoHdlModule, Log::LOG_ERROR,
		     "am_sso_get_idle_time(): "
		     "Unknown exception encountered.");
	}
    } else {
	Log::log(ssoHdlModule, Log::LOG_ERROR,
		 "am_sso_get_idle_time(): "
		 "One or more input parameters is NULL");
    }
    return retVal;
}

extern "C"
am_status_t
am_sso_set_property(am_sso_token_handle_t sso_token_handle,
                    const char *name,
                    const char *value)
{
    am_status_t sts = AM_FAILURE;
    if(NULL==sso_token_handle || NULL==name || NULL==value) {
	Log::log(ssoHdlModule, Log::LOG_ERROR,
		 "am_sso_set_property(): "
		 "One or more input parameters has an invalid value.");
        sts = AM_INVALID_ARGUMENT;
    }
    else if (NULL==SSOTokenSvc) {
	Log::log(ssoHdlModule, Log::LOG_ERROR,
		 "sso_token_id(): "
		 "SSO Service not initialized.");
        sts = AM_SERVICE_NOT_INITIALIZED;
    }
    else {
	try {
	    SessionInfo &sessionInfo =
		 *(reinterpret_cast<SessionInfo *>(sso_token_handle));
	    sts = SSOTokenSvc->setProperty(ServiceInfo(),
					   sessionInfo,
					   name,
					   value);
	}
	catch (...) {
	    Log::log(ssoHdlModule, Log::LOG_ERROR,
		     "am_sso_set_property(): "
		     "Unknown exception thrown.");
	}
    }
    return sts;
}

extern "C"
am_status_t
am_sso_add_listener(const am_sso_token_listener_func_t listener,
                    void *args,
		    boolean_t dispatch_in_sep_thread)
{
    am_status_t sts = AM_FAILURE;

    if (NULL==SSOTokenSvc) {
        Log::log(ssoHdlModule, Log::LOG_ERROR,
                 "am_sso_add_listener(): "
                 "SSO is not initialized.");
        sts = AM_SERVICE_NOT_INITIALIZED;
    }
    else if (NULL==listener) {
        Log::log(ssoHdlModule, Log::LOG_ERROR,
                 "am_sso_add_listener(): "
                 "One or more input parameters is NULL or invalid.");
        sts = AM_INVALID_ARGUMENT;
    }
    else {
        try {
            sts = SSOTokenSvc->addSSOListener(listener,
                                              args,
					      dispatch_in_sep_thread ? true : false);
        }
        catch (InternalException& exc) {
            Log::log(ssoHdlModule, Log::LOG_ERROR,
                     "am_sso_add_listener(): "
                     "Internal Exception encountered: %s",
		     exc.getMessage());
            sts = exc.getStatusCode();
        }
        catch (std::exception& exc) {
            Log::log(ssoHdlModule, Log::LOG_ERROR,
                     "am_sso_add_listener(): "
                     "Unexpected exception encountered: %s",
		     exc.what());
        }
	catch (...) {
            Log::log(ssoHdlModule, Log::LOG_ERROR,
                     "am_sso_add_listener(): "
                     "Unknown exception encountered.");
	}
    }

    return sts;
}

extern "C"
am_status_t
am_sso_add_sso_token_listener(am_sso_token_handle_t sso_token_handle,
                              const am_sso_token_listener_func_t listener,
                              void *args,
			      boolean_t dispatch_to_sep_thread)
{
    am_status_t sts = AM_FAILURE;

    if (NULL==SSOTokenSvc) {
        Log::log(ssoHdlModule, Log::LOG_ERROR,
                 "am_sso_add_sso_token_listener(): "
                 "SSO is not initialized.");
        sts = AM_SERVICE_NOT_INITIALIZED;
    }
    else if (NULL==sso_token_handle || NULL==listener) {
        Log::log(ssoHdlModule, Log::LOG_ERROR,
                 "am_sso_add_sso_token_listener(): "
                 "One or more input parameters is NULL or invalid.");
        sts = AM_INVALID_ARGUMENT;
    }
    else {
        try {
	    SessionInfo *sessionInfoPtr =
                  reinterpret_cast<SessionInfo *>(sso_token_handle);
            sts = SSOTokenSvc->addSSOTokenListener(ServiceInfo(),
                                                   sessionInfoPtr,
						   listener,
                                                   args,
                                                   "",
						   dispatch_to_sep_thread ? true : false);
        }
        catch (InternalException& exc) {
            Log::log(ssoHdlModule, Log::LOG_ERROR,
                     "am_sso_add_sso_token_listener(): "
                     "Internal Exception encountered: %s",
		     exc.getMessage());
            sts = exc.getStatusCode();
        }
        catch (std::exception& exc) {
            Log::log(ssoHdlModule, Log::LOG_ERROR,
                     "am_sso_add_sso_token_listener(): "
                     "Unexpected exception encountered: %s", exc.what());
	    sts = AM_SESSION_FAILURE;
        }
        catch (...) {
            Log::log(ssoHdlModule, Log::LOG_ERROR,
                     "am_sso_add_sso_token_listener(): "
                     "Unknown exception encountered");
	    sts = AM_SESSION_FAILURE;
	}
    }

    return sts;
}


//
// Note this function is not exposed in api.
// It needs to be here to access SSOTokenSvc.
// If the services were singletons wouldn't have to do this.
//
am_status_t
am_sso_handle_notification(const std::string& notifData)
{
    am_status_t sts = AM_FAILURE;

    if (NULL==SSOTokenSvc) {
        Log::log(ssoHdlModule, Log::LOG_ERROR,
                 "am_sso_handle_notification(): service not initialized.");
        sts = AM_SERVICE_NOT_INITIALIZED;
    }
    else if (notifData.size() == 0) {
        Log::log(ssoHdlModule, Log::LOG_ERROR,
                 "am_sso_handle_notification(): "
                 "input parameter is invalid.");
        sts = AM_INVALID_ARGUMENT;
    }
    else {
        try {
            sts = SSOTokenSvc->handleNotif(notifData);
        }
        catch (InternalException& exc) {
            sts = exc.getStatusCode();
            Log::log(ssoHdlModule, Log::LOG_ERROR,
                     "am_sso_handle_notification(): "
                     "Internal Exception encountered: %s",
                     exc.getMessage());
        }
        catch (NSPRException& exc) {
            Log::log(ssoHdlModule, Log::LOG_ERROR,
                     "am_sso_handle_notification(): "
                     "NSPR Exception encountered: "
		     "Throwing method: %s, NSPR method: %s",
		     exc.getThrowingMethod(),
		     exc.getNsprMethod());
            sts = AM_NSPR_ERROR;
        }
        catch (std::exception& exc) {
            Log::log(ssoHdlModule, Log::LOG_ERROR,
                     "am_sso_handle_notification(): "
                     "Unexpected exception encountered: %s",
		     exc.what());
        }
        catch (...) {
            Log::log(ssoHdlModule, Log::LOG_ERROR,
                     "am_sso_handle_notification(): "
                     "Unknown exception encountered.");
        }
    }
    return sts;
}

extern "C"
am_status_t
am_sso_remove_sso_token_listener(
	const am_sso_token_handle_t sso_token_handle,
        const am_sso_token_listener_func_t listener)
{
    am_status_t sts = AM_FAILURE;

    if (NULL==SSOTokenSvc) {
        Log::log(ssoHdlModule, Log::LOG_ERROR,
                 "am_sso_remove_sso_token_listener(): "
                 "SSO is not initialized.");
        sts = AM_SERVICE_NOT_INITIALIZED;
    }
    else if (NULL==sso_token_handle || NULL==listener) {
        Log::log(ssoHdlModule, Log::LOG_ERROR,
                 "am_sso_remove_sso_token_listener(): "
                 "One or more input parameters is NULL or invalid.");
        sts = AM_INVALID_ARGUMENT;
    }
    else {
	try {
	    SessionInfo &sessionInfo =
		*(reinterpret_cast<SessionInfo *>(sso_token_handle));
            SSOToken& ssoTok = sessionInfo.getSSOToken();
            const std::string& sso_token_id = ssoTok.getString();
            if (sso_token_id.size() <= 0) {
                Log::log(ssoHdlModule, Log::LOG_ERROR,
                     "am_sso_remove_sso_token_listener(): "
                     "One or more input parameters is NULL or invalid.");
                sts = AM_INVALID_ARGUMENT;
            }
            else {
	        sts = SSOTokenSvc->removeSSOTokenListener(sso_token_id,
							  listener);
	        if (sts == AM_SUCCESS) {
		    Log::log(ssoHdlModule, Log::LOG_DEBUG,
			 "am_sso_remove_sso_token_listener(): "
			 "listener for SSO Token ID %s successfully removed.",
			 sso_token_id.c_str());
	        }
	        else {
		    Log::log(ssoHdlModule, Log::LOG_ERROR,
		             "am_sso_remove_sso_token_listener(): "
		             "Could not remove sso token listener for "
                             "token id %s. Error %d.",
		             sso_token_id.c_str(), sts);
	        }
            }
        }
        catch (std::exception& ex) {
	    Log::log(ssoHdlModule, Log::LOG_ERROR,
		     "am_sso_remove_sso_token_listener(): "
		     "Unexpected error when remove sso token listener."
                     "Error mesg: '%s'", ex.what());
            sts = AM_FAILURE;
        }
        catch (...) {
	    Log::log(ssoHdlModule, Log::LOG_ERROR,
		     "am_sso_remove_sso_token_listener(): "
		     "Unknown error when remove sso token listener.");
            sts = AM_FAILURE;
        }
    }
    return sts;
}


extern "C"
am_status_t
am_sso_remove_listener(const am_sso_token_listener_func_t listener_func)
{
    am_status_t sts = AM_FAILURE;

    if (NULL==SSOTokenSvc) {
        Log::log(ssoHdlModule, Log::LOG_ERROR,
                 "am_sso_remove_listener(): "
                 "SSO is not initialized.");
        sts = AM_SERVICE_NOT_INITIALIZED;
    }
    else if (NULL==listener_func) {
        Log::log(ssoHdlModule, Log::LOG_ERROR,
                 "am_sso_remove_listener(): "
                 "One or more input paramters is NULL.");
        sts = AM_INVALID_ARGUMENT;
    }
    else {
	try {
	    sts = SSOTokenSvc->removeSSOListener(listener_func);
	    if (sts == AM_SUCCESS) {
		Log::log(ssoHdlModule, Log::LOG_DEBUG,
		     "am_sso_remove_listener(): "
		     "listener 0x%x removed.", listener_func);
	    }
	    else {
		Log::log(ssoHdlModule, Log::LOG_ERROR,
			 "am_sso_remove_listener(): "
			 "Could not remove sso listener 0x%x, status %d.",
			 listener_func, sts);
	    }
        }
        catch (std::exception& ex) {
	    Log::log(ssoHdlModule, Log::LOG_ERROR,
		     "am_sso_remove_listener(): "
		     "Unexpected error when removing sso listener, 0x%x."
                     "Error message: %s", listener_func, ex.what());
            sts = AM_FAILURE;
        }
        catch (...) {
	    Log::log(ssoHdlModule, Log::LOG_ERROR,
		     "am_sso_remove_listener(): "
		     "Unknown error when removing listener 0x%x.",
		     listener_func);
            sts = AM_FAILURE;
        }
    }
    return sts;
}

