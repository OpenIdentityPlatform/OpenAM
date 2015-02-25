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
 * $Id: am_log.cpp,v 1.5 2008/07/15 20:12:38 subbae Exp $
 *
 */
/*
 * Portions Copyrighted 2013 ForgeRock Inc
 */


#include "am_log.h"

#include "internal_macros.h"
#include "nspr_exception.h"
#include "properties.h"
#include "http.h"
#include "url.h"
#include "service_info.h"
#include "sso_token.h"
#include "log_record.h"
#include "log_service.h"
#include "log.h"
#include "service_info.h"
BEGIN_PRIVATE_NAMESPACE
DEFINE_BASE_INIT;
END_PRIVATE_NAMESPACE

USING_PRIVATE_NAMESPACE

extern "C" am_status_t am_log_init(const am_properties_t properties)
{
    am_status_t status = AM_SUCCESS;
    if (properties == NULL) {
        status = AM_INVALID_ARGUMENT;
    }
    else {
	const Properties *prop =
		reinterpret_cast<const Properties *>(properties);
	try {
	    base_init(*prop, B_TRUE);
	}
	catch (InternalException& ex) {
	    status = ex.getStatusCode();
	}
	catch (...) {
	    status = AM_FAILURE;
	}
    }
    return status;
}


extern "C" am_status_t am_log_set_log_file(const char *name)
{
    am_status_t status = AM_FAILURE;

    if (!Log::isInitialized()) {
	status = AM_SERVICE_NOT_INITIALIZED;
    }
    else if (name == NULL || *name == '\0') {
	status = AM_INVALID_ARGUMENT;
    }
    else if (!Log::setLogFile(name)) {
	status = AM_LOG_FAILURE;
    } else {
	status = AM_SUCCESS;
    }

    return status;
}

extern "C" am_status_t am_log_add_module(const char *name,
					       am_log_module_id_t *idPtr)
{
    am_status_t status = AM_SUCCESS;

    if (!Log::isInitialized()) {
	status = AM_SERVICE_NOT_INITIALIZED;
    }
    else if (name == NULL || idPtr == NULL) {
	status = AM_INVALID_ARGUMENT;
    }
    else {
	*idPtr = Log::addModule(name);
	if (idPtr == NULL)
	    status = AM_FAILURE;
	else
	    status = AM_SUCCESS;
    }

    return status;
}

extern "C" am_log_level_t
am_log_set_module_level(am_log_module_id_t moduleID,
			   am_log_level_t level)
{
    am_log_level_t oldLevel = AM_LOG_NONE;
    if (Log::isInitialized()) {
	Log::Level logLevel, oldLogLevel;
	logLevel = static_cast<Log::Level>(static_cast<int>(level));
	oldLogLevel = Log::setModuleLevel(moduleID, logLevel);
	oldLevel = static_cast<am_log_level_t>
			    (static_cast<int>(oldLogLevel));
    }
    return oldLevel;
}

extern "C" am_status_t
am_log_set_levels_from_string(const char *moduleLevelString)
{
    am_status_t status = AM_SUCCESS;

    if (!Log::isInitialized()) {
        status = AM_SERVICE_NOT_INITIALIZED;
    }
    else if (!moduleLevelString) {
	status = AM_INVALID_ARGUMENT;
    }
    else {
	if (moduleLevelString) {
	    Log::setLevelsFromString(moduleLevelString);
	    status = AM_SUCCESS;
	}
    }
    return status;
}

extern "C" boolean_t am_log_is_level_enabled(am_log_module_id_t moduleID,
					  am_log_level_t level)
{
    boolean_t retVal = B_FALSE;
    if (Log::isInitialized()) {
        Log::Level logLevel = static_cast<Log::Level>(static_cast<int>(level));
        bool levelEnabled = Log::isLevelEnabled(moduleID, logLevel);
	retVal = levelEnabled ? B_TRUE : B_FALSE;
    }
    return retVal;
}

extern "C" boolean_t am_log_log(am_log_module_id_t moduleID,
				      am_log_level_t level,
				      const char *format, ...)
{
    boolean_t retVal = B_FALSE;

    if (Log::isInitialized()) {
	std::va_list args;
	va_start(args, format);
	Log::vlog(moduleID,
		  static_cast<Log::Level>(static_cast<int>(level)),
		  format, args);
	retVal = B_TRUE;
	va_end(args);
    }
    return retVal;
}


extern "C" boolean_t am_log_vlog(am_log_module_id_t moduleID,
				       am_log_level_t level,
				       const char *format,
				       va_list args)
{
    boolean_t retVal = B_FALSE;
    if (Log::isInitialized()) {
	Log::vlog(moduleID,
		  static_cast<Log::Level>(static_cast<int>(level)),
		  format, args);
	retVal = B_TRUE;
    }
    return retVal;
}

extern "C" am_status_t am_log_set_remote_info(const char *rem_log_url,
                                              const char *sso_token_id,
                                              const char *rem_log_name,
					      const am_properties_t properties)
{
    am_status_t retVal = AM_SUCCESS;
    if (rem_log_url == NULL || sso_token_id == NULL ||
             rem_log_name == NULL || properties == NULL) {
	retVal = AM_INVALID_ARGUMENT;
    }
    else if (!Log::isInitialized()) {
        retVal = AM_SERVICE_NOT_INITIALIZED;
    }
    else {
	try {
	    SSOToken ssoToken = SSOToken(sso_token_id);
	    const Properties *prop =
			reinterpret_cast<const Properties *>(properties);
	    LogService *newLogSvc =
		new LogService(ServiceInfo(rem_log_url),
			ssoToken,
			Http::CookieList(), rem_log_name, *prop);
	    Log::setRemoteInfo(newLogSvc);
	}
	catch (InternalException& ex) {
	    retVal = ex.getStatusCode();
	}
	catch (...) {
	    retVal = AM_FAILURE;
	}
    }
    return retVal;
}



extern "C" am_status_t am_log_record_create(am_log_record_t *record_ptr,
                                            am_log_record_log_level_t log_level,
                                            const char *message)
{
    am_status_t retVal = AM_FAILURE;
    LogRecord *logRecord = NULL;

    if(record_ptr != NULL && message != NULL) {
	LogRecord::Level level =
                static_cast<LogRecord::Level>(static_cast<int>(log_level));
	logRecord = new LogRecord(level, message);
	*record_ptr = reinterpret_cast<am_log_record_t>(logRecord);
	retVal = AM_SUCCESS;
    } else {
        retVal = AM_INVALID_ARGUMENT;
    }
    return retVal;
}

extern "C" am_status_t am_log_record_destroy(am_log_record_t record)
{
    am_status_t retVal = AM_FAILURE;
    if(record != NULL) {
	LogRecord *logRecord = reinterpret_cast<LogRecord *>(record);
	delete logRecord;
	retVal = AM_SUCCESS;
    } else {
        retVal = AM_INVALID_ARGUMENT;
    }
    return retVal;
}


extern "C" am_status_t am_log_record_populate(am_log_record_t record,
                                              const char *user_token_id)
{
    am_status_t retVal = AM_FAILURE;
    if(record != NULL && user_token_id != NULL) {
        try {
	    std::string userTok = user_token_id;
	    if (userTok.find('%') != std::string::npos) {
	        userTok = Http::decode(user_token_id);
	    }
            LogRecord &logRecord =
                reinterpret_cast<LogRecord &>(*record);

            logRecord.populateTokenDetails(userTok);
            retVal = AM_SUCCESS;
        } catch(InternalException &iex) {
            retVal = AM_LOG_FAILURE;
        } catch(std::exception &aex) {
            retVal = AM_LOG_FAILURE;
        } catch(...) {
            retVal = AM_FAILURE;
        }
    } else {
        retVal = AM_INVALID_ARGUMENT;
    }
    return retVal;
}

extern "C" am_status_t am_log_record_add_loginfo(am_log_record_t record,
                                                 const char *key,
                                                 const char *value)
{
    am_status_t retVal = AM_FAILURE;
    if(record != NULL && key != NULL && value != NULL) {
        try {
            LogRecord &logRecord =
                reinterpret_cast<LogRecord &>(*record);

            logRecord.addLogInfo(key, value);
            retVal = AM_SUCCESS;
        } catch(InternalException &iex) {
            retVal = iex.getStatusCode();
        } catch(std::exception &aex) {
            retVal = AM_LOG_FAILURE;
        } catch(...) {
            retVal = AM_FAILURE;
        }
    } else {
        retVal = AM_INVALID_ARGUMENT;
    }
    return retVal;
}

extern "C" am_status_t am_log_record_set_loginfo_props(am_log_record_t record,
                                                       am_properties_t log_info)
{
    am_status_t retVal = AM_FAILURE;
    if(record != NULL && log_info != NULL) {
	LogRecord &logRecord = reinterpret_cast<LogRecord &>(*record);
	Properties &logInfoProps = reinterpret_cast<Properties &>(*log_info);

	logRecord.setLogInfoProps(logInfoProps);
	retVal = AM_SUCCESS;
    } else {
        retVal = AM_INVALID_ARGUMENT;
    }
    return retVal;
}

extern "C" am_status_t am_log_record_set_log_level(
	am_log_record_t record, am_log_record_log_level_t log_level)
{
    am_status_t retVal = AM_FAILURE;
    if(record != NULL && log_level >= AM_LOG_LEVEL_FINEST &&
	    log_level <= AM_LOG_LEVEL_SEVERE) {
	LogRecord &logRecord = reinterpret_cast<LogRecord &>(*record);
	LogRecord::Level level =
                static_cast<LogRecord::Level>(static_cast<int>(log_level));
	logRecord.setLogLevel(level);
	retVal = AM_SUCCESS;
    } else {
        retVal = AM_INVALID_ARGUMENT;
    }
    return retVal;
}

extern "C" am_status_t am_log_record_set_log_message(am_log_record_t record,
                                                     const char *message)
{
    am_status_t retVal = AM_FAILURE;
    if(record != NULL && message != NULL) {
	LogRecord &logRecord = reinterpret_cast<LogRecord &>(*record);

	logRecord.setLogMessage(message);
	retVal = AM_SUCCESS;
    } else {
        retVal = AM_INVALID_ARGUMENT;
    }
    return retVal;
}

extern "C" am_status_t am_log_log_record(am_log_record_t record,
                                  const char *log_name,
                                  const char *logged_by_token_id)
{
    am_status_t retVal = AM_FAILURE;
    if(log_name != NULL && record != NULL && logged_by_token_id != NULL) {
	LogRecord &logRecord =
	    reinterpret_cast<LogRecord &>(*record);
	retVal = Log::rlog(log_name, logRecord, logged_by_token_id);
    } else {
        retVal = AM_INVALID_ARGUMENT;
    }
    return retVal;
}

extern "C" am_status_t am_log_flush_remote_log()
{
    am_status_t retVal = AM_FAILURE;
    retVal = Log::rmtflush();
    return retVal;
}

extern "C"
am_status_t am_log_set_logger(const am_log_logger_func_t logger_func,
	                      am_log_logger_func_t *old_logger_func)
{
    am_status_t status = AM_SUCCESS;
    am_log_logger_func_t oldLoggerFunc;

    if (!Log::isInitialized()) {
        status = AM_SERVICE_NOT_INITIALIZED;
    }
    else {
	oldLoggerFunc = Log::setLogger(logger_func);
	if (old_logger_func != NULL) {
	    *old_logger_func = oldLoggerFunc;
	    status = AM_SUCCESS;
	}
	else {
	    status = AM_FAILURE;
	}
    }
    return status;
}

/*
 * Set agent's debug file size.
 * debugFileSize must be > DEBUG_FILE_MIN_SIZE else
 * default value gets used.
 */
extern "C" am_status_t
am_log_set_debug_file_size(const long debugFileSize)
{
    am_status_t status = AM_SUCCESS;

    if (!Log::isInitialized()) {
        status = AM_SERVICE_NOT_INITIALIZED;
    } else if(debugFileSize > DEBUG_FILE_MIN_SIZE) {
        Log::setDebugFileSize(debugFileSize);
        status = AM_SUCCESS;
    } else {
        Log::setDebugFileSize(DEBUG_FILE_MIN_SIZE);
        status = AM_SUCCESS;
    }
    return status;
}

/*
 * Set agent's debug file rotate or not.
 */
extern "C" am_status_t
am_log_set_debug_file_rotate(boolean_t debugFileRotate)
{
    am_status_t status = AM_SUCCESS;

    if (!Log::isInitialized()) {
        status = AM_SERVICE_NOT_INITIALIZED;
    } else {
        Log::setDebugFileRotate(debugFileRotate == B_TRUE);
        status = AM_SUCCESS;
    }
    return status;
}
