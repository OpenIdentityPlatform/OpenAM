/* -*- Mode: C -*- */
/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems, Inc. All Rights Reserved.
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
 * $Id: am_log.h,v 1.4 2008/08/19 19:11:36 veiming Exp $
 */

/*
 * Abstract:
 *
 * Types and functions for using OpenSSO Access 
 * Management SDK log objects.
 *
 */

#ifndef AM_LOG_H
#define AM_LOG_H

#include <stdarg.h>
#include <am_types.h>
#include <am_properties.h>

AM_BEGIN_EXTERN_C

typedef enum am_log_level {
    AM_LOG_ALWAYS = -1, /* always logged */
    AM_LOG_NONE,	/* never logged, typically used to turn off a module */
    AM_LOG_ERROR,	/* used for error messages */
    AM_LOG_WARNING,	/* used for warning messages */
    AM_LOG_INFO,	/* used for informational messages */
    AM_LOG_DEBUG,	/* used for debug messages */
    AM_LOG_MAX_DEBUG,    /* used for more detailed debug messages */
    AM_LOG_AUTH_REMOTE = 128, /* logged deny and/or allow */
    AM_LOG_AUTH_LOCAL = 256
} am_log_level_t;

typedef enum am_log_record_log_level {

    /* Log Level as defined by JDK 1.4 */

    AM_LOG_LEVEL_SEVERE = 1000,
    AM_LOG_LEVEL_WARNING = 900,
    AM_LOG_LEVEL_INFORMATION = 800,
    AM_LOG_LEVEL_CONFIG = 700,
    AM_LOG_LEVEL_FINE = 500,
    AM_LOG_LEVEL_FINER = 400,
    AM_LOG_LEVEL_FINEST = 300,

    /* Log Levels defined by OpenSSO */

    AM_LOG_LEVEL_SECURITY = 950,
    AM_LOG_LEVEL_CATASTROPHE = 850,
    AM_LOG_LEVEL_MISCONF = 750,
    AM_LOG_LEVEL_FAILURE = 650,
    AM_LOG_LEVEL_WARN = 550,
    AM_LOG_LEVEL_INFO = 450,
    AM_LOG_LEVEL_DEBUG = 350,
    AM_LOG_LEVEL_ALL = 250

} am_log_record_log_level_t;


typedef unsigned int am_log_module_id_t;
/* Log Record */
typedef struct am_log_record *am_log_record_t;

#define	AM_LOG_ALL_MODULES	((am_log_module_id_t) 0)
#define	AM_LOG_REMOTE_MODULE	((am_log_module_id_t) AM_LOG_ALL_MODULES+1)

/* Default log fields which can be used for adding log info */
#define HOST_NAME "HostName"
#define LOG_LEVEL "LogLevel"
#define LOGIN_ID "LoginID"
#define CLIENT_DOMAIN "Domain"
#define IP_ADDR "IPAddr"
#define MODULE_NAME "ModuleName"
#define LOGIN_ID_SID "LoginIDSid"

/*
 * Initializes the log service.
 *
 * Parameters:
 *   properties	name of the properties file.
 *
 * Returns:
 *   AM_SUCCESS
 *		if the logging file could be initialized
 *
 *   AM_FAILURE
 *		if any other error is detected
 */
AM_EXPORT am_status_t am_log_init(const am_properties_t properties);

/*
 * Sets the name of the file to use for logging.  If the specified name
 * is NULL or empty, then logging messages will be sent to the stderr
 * stream.
 *
 * Parameters:
 *   name	name of the file in which to record logging messages
 *
 * Returns:
 *   AM_SUCCESS
 *		if the logging file could be set
 *
 *   AM_NO_MEMORY
 *		if unable to allocate memory for internal data structures
 *
 *   AM_FAILURE
 *		if any other error is detected
 */
AM_EXPORT am_status_t am_log_set_log_file(const char *name);

/*
 * Adds a new module to the list of known logging modules.  If a
 * module of the same name already exists, then the module id of that
 * module is returned.
 *
 * Parameters:
 *   name	the name to associate with the new module
 *
 *   id_ptr	where to store the id of the logging module
 *
 * Returns:
 *   AM_SUCCESS
 *		if no error is detected
 *
 *   AM_INVALID_ARGUMENT
 *		if name or id_ptr is NULL
 *
 *   AM_NSPR_ERROR
 *		if unable to initialize to the logging package
 *
 *   AM_NO_MEMORY
 *		if unable to allocate memory for the new logging module
 *
 *   AM_FAILURE
 *		if any other error is detected
 */
AM_EXPORT am_status_t
am_log_add_module(const char *name, am_log_module_id_t *id_ptr);

/*
 * Sets the logging level for the specified module.
 *
 * Parameters:
 *   moduleID	the id of the module to be modified
 *
 *   level	the new logging level for the module
 *
 * Returns:
 *   the previous logging level of the module or LOG_NONE if the specified
 *   module is invalid.
 */
AM_EXPORT am_log_level_t
am_log_set_module_level(am_log_module_id_t moduleID,
			   am_log_level_t level);

/*
 * Sets the logging level for the modules listed in specified string.
 * The format of the string is:
 *
 *   <ModuleName>[:<Level>][,<ModuleName>[:<Level>]]*
 *
 * Optional spaces may occur before and after any commas.
 *
 * Parameters:
 *   module_level_string
 *		list of modules to set.
 *
 * Returns:
 *    The same set of errors as am_log_add_module.
 */
AM_EXPORT am_status_t
am_log_set_levels_from_string(const char *module_level_string);

/*
 * Determines whether a logging message at the specified level and
 * associated with the specified module would be emitted.
 *
 * Parameters:
 *   module	the id of the module to be examined
 *
 *   level	the logging level to be checked
 *
 * Returns:
 *   !0		if the message would be emitted
 *
 *   0		otherwise
 */
AM_EXPORT boolean_t am_log_is_level_enabled(am_log_module_id_t moduleID,
					    am_log_level_t level);

/*
 * The next two routines produce logging messages.  The message is
 * emitted only if the current level of the specified module is
 * greater than or equal to the specified level.
 *
 * Parameters:
 *   module	the id of the module to be associated with the message
 *
 *   level	the logging level of the message
 *
 *   format	a printf-style format string
 *
 *   the set of addition arguments needed by the format string either
 *   enumerated directly or passed using the standard va_list mechanism
 *   as appropriate to the call.
 */
AM_EXPORT boolean_t am_log_log(am_log_module_id_t moduleID,
					  am_log_level_t level,
					  const char *format, ...);

AM_EXPORT boolean_t am_log_vlog(am_log_module_id_t moduleID,
					   am_log_level_t level,
					   const char *format,
					   va_list args);


AM_EXPORT am_status_t am_log_set_remote_info(const char *rem_log_url,
                                             const char *sso_token_id,
                                             const char *rem_log_name,
					     const am_properties_t log_props);

/* Instantiate a log record and initialize it with the given
 * log level and message
 */
AM_EXPORT am_status_t am_log_record_create(am_log_record_t *record_ptr,
                                           am_log_record_log_level_t log_level,
                                           const char *message);

/* Destroys the log record returned by am_log_record_create */
AM_EXPORT am_status_t am_log_record_destroy(am_log_record_t record);

/* Update log record with user's sso token information */
AM_EXPORT am_status_t am_log_record_populate(am_log_record_t record,
                                             const char *user_token_id);

/* Update log record with additional information */
AM_EXPORT am_status_t am_log_record_add_loginfo(am_log_record_t record,
                                                const char *key,
                                                const char *value);

/* Update log record with additional information.
 * Set all log info values as properties map
 * Note: 
 * The log_info is expected to have the required log info fields as key value 
 * pairs and user is expected to delete the am_properties_t pointer only when 
 * he is done with amsdk.
 */


AM_EXPORT am_status_t am_log_record_set_loginfo_props(am_log_record_t record,
                                                      am_properties_t log_info);

/* Convenience functions */
AM_EXPORT am_status_t am_log_record_set_log_level(am_log_record_t record,
                                                  am_log_record_log_level_t log_level);

AM_EXPORT am_status_t am_log_record_set_log_message(am_log_record_t record,
                                                    const char *message);

/* Log the log record in given log_name */
AM_EXPORT am_status_t am_log_log_record(am_log_record_t record,
                                  const char *log_name,
                                  const char *logged_by_token_id);

/* Flush all the log records in the log buffer */
AM_EXPORT am_status_t am_log_flush_remote_log();

AM_END_EXTERN_C

#endif	/* not AM_LOG_H */
