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
 * $Id: log.h,v 1.5 2009/07/22 22:59:06 subbae Exp $
 *
 * Abstract:
 *
 * Standard logging package for DSAME Remote Client SDK.
 *
 */
/*
 * Portions Copyrighted 2013 ForgeRock Inc
 */

#ifndef LOG_H
#define LOG_H

#include <cstdarg>
#include <string>
#include <vector>
#ifndef _MSC_VER
#include <semaphore.h>
#endif
#include <am_log.h>
#include "internal_macros.h"
#include "internal_exception.h"
#include "mutex.h"

BEGIN_PRIVATE_NAMESPACE

typedef void (*am_log_logger_func_t)(const char *moduleName,
                                     am_log_level_t level,
                                     const char *msg);

//
// Forward declaration of Properties class, because we cannot include
// properties.h, because that would create a circular dependency.
// Forward declarations of Http::CookiList, Properties, and ServiceInfo
// classes, because we cannot include the appropriate header files without
// creating circular dependencies.
//
namespace Http {
    struct Cookie;
    typedef std::vector<Http::Cookie> CookieList;
}

class Properties;
class ServiceInfo;
class SSOToken;
class LogRecord;
class LogService;

class Log {
public:
    enum Level {
	LOG_ALWAYS = -1,// always logged
	LOG_NONE,	// never logged, typically used to turn off a category
	LOG_ERROR,	// used for error messages
	LOG_WARNING,	// used for warning messages
	LOG_INFO,	// used for informational messages
	LOG_DEBUG,	// used for debug messages
	LOG_MAX_DEBUG,	// used for more detailed debug messages
	LOG_AUTH_REMOTE=128,	// used for logging to the remote server.
	LOG_AUTH_LOCAL=256
    };
    typedef unsigned int ModuleId;
    static const ModuleId ALL_MODULES;
    static const ModuleId REMOTE_MODULE;

    //
    // Sets the name of the file to use for logging.  If the specified name
    // is empty, then logging messages will be sent to the stderr stream.
    //
    // Parameters:
    //   name	name of the file in which to record logging messages
    //
    // Returns:
    //   true	if the logging file could be set
    //
    //   false	otherwise
    //
    //
    static bool setLogFile(const std::string& name) throw();

    //
    // Adds a new module to the list of known logging modules.  If a
    // module of the same name already exists, then the ModuleId of that
    // module is returned.
    //
    // Parameters:
    //   name	the name to associate with the new module
    //
    // Returns:
    //   the ModuleId of the logging module associated with name
    //   or 0 if the module could not be added. 
    //
    static ModuleId addModule(const std::string& name) throw();

    //
    // Sets the logging level for the specified module.
    //
    // Parameters:
    //   module	the id of the module to be modified
    //
    //   level	the new logging level for the module
    //
    // Returns:
    //   the previous logging level of the module, or LOG_NONE 
    //   if the level could not be set.
    //
    //
    static Level setModuleLevel(ModuleId module, Level level) throw();

    //
    // Sets the logging level for the modules listed in specified string.
    // The format of the string is:
    //
    //   <ModuleName>[:<Level>][,<ModuleName>[:<Level>]]*
    //
    // Optional spaces may occur before and after any commas.
    //
    // Parameters:
    //   moduleLevelString
    //		list of modules to set.
    //	Returns: 
    //	 AM_INVALID_ARGUMENT
    //		if any argument is invalid.
    //	 AM_FAILURE
    //		if any other error occurred.
    //
    static am_status_t 
	setLevelsFromString(const std::string& moduleLevelString) throw(); 

    //
    // Determines whether a logging message at the specified level and
    // associated with the specified module would be emitted.
    //
    // Parameters:
    //   module	the id of the module to be examined
    //
    //   level	the logging level to be checked
    //
    // Returns:
    //   true	if the message would be emitted
    //
    //   false	otherwise
    //
    static bool isLevelEnabled(ModuleId module, Level level) throw();

    //
    // The next two routines produce logging messages.  The message is
    // emitted only if the current level of the specified module is
    // greater than or equal to the specified level.
    //
    // Parameters:
    //   module	the id of the module to be associated with the message
    //
    //   level	the logging level of the message
    //
    //   format	a printf-style format string
    //
    //   the set of addition arguments needed by the format string either
    //   enumerated directly or passed using the standard va_list mechanism
    //   as appropriate to the call.
    //
    static void log(ModuleId module, Level level,
		    const char *format, ...) throw();

    static void log(ModuleId module, Level level,
		    const std::exception &exception) throw();


    static void log(ModuleId module, Level level,
		    const InternalException &exception) throw();


    static void vlog(ModuleId module, Level level, const char *format,
		     std::va_list args) throw(); 

    //
    // Initializes the logging package.
    //
    // Parameters:
    //
    //   properties
    //		A Properties object that may contain configuration
    //		properties for logging levels (AM_LOG_LEVELS_PROPERTY)
    //		and output file (AM_LOG_FILE_PROPERTY).
    //
    // Returns:
    //    AM_NO_MEMORY
    //		if unable to allocate the memory for internal data
    //		structures
    //
    //    AM_NSPR_ERROR
    //		if unable to allocate the lock used to serialize writes
    //
    //    AM_FAILURE
    //		if any other type of error is detected
    //
    // NOTE:  This routine is automatically invoked by am_init or
    // the first call to a logging method.
    //
    static am_status_t initialize(const Properties& properties) throw();

    //
    // Initializes the default remote logging service.
    //
    // Parameters:
    //
    //   properties
    //		A Properties object that may contain configuration
    //		properties for logging levels (AM_LOG_LEVELS_PROPERTY)
    //		and output file (AM_LOG_FILE_PROPERTY).
    //
    // Returns:
    //
    //	  AM_INVALID_ARGUMENT
    //		if any argument is invalid.
    //	  AM_NO_MEMORY
    //		if there is no more memory
    //    AM_FAILURE
    //		if any other type of error is detected
    // 
    // NOTE:  This routine is automatically invoked by am_init or
    // the first call to a logging method.
    //
    static am_status_t initializeRemoteLog(const Properties& properties)
	    throw();

    //
    // Closes any logging file.
    //
    // NOTE:  This routine is automatically invoked by am_cleanup.
    //
    static void shutdown() throw();

    //
    // set up cookielist, ssotoken and logserviceinfo for remote logging.
    //
    static am_status_t setRemoteInfo(LogService *) throw();

    static bool isInitialized() { return initialized; } 

    //
    // set logger. this function will be called in place of the local write.
    // remote log will still go to remote if module level is remote.
    //
    static am_log_logger_func_t setLogger(am_log_logger_func_t logger);

    /* Log the log record in the specified log */
    static am_status_t rlog(const std::string& logName,
			    const LogRecord& record,
			    const std::string& loggedByTokenID) throw();

    /* Log the message to remote server with a user sso token id for 
     * the server to fill in with sso token id details.
     */
    static am_status_t rlog(ModuleId module, int remotelevel, 
	  const char *user_sso_token, const char *format, ...) throw();
    
    /*
     * Log url access audit message to local audit log file.
     *
     */
    static void doLocalAuditLog(ModuleId module, 
        Level level,
        const char* auditLogMsg,
        bool localAuditLogRotate,
        long localAuditFileSize) throw();
    
    /*
     * Log url access audit message to remote audit log file.
     *
     */
    static am_status_t doRemoteAuditLog(ModuleId module,
            int remote_log_level,
            const char *user_sso_token,
            const char *logMsg);

    /*
     * Log url access audit message. This calls doLocalAuditLog()
     * or doRemoteAuditLog() or both methods based on 
     * log.disposition property value.
     */
    static am_status_t
    auditLog(const char* auditDisposition,
            bool localAuditLogRotate,
            long localAuditFileSize,
            ModuleId module,
            int remoteLogLevel,
            const char *userSSOToken,
            const char *format,
            ...);

    /* Flush all log records in the log buffer */
    static am_status_t rmtflush() throw();

    static am_status_t initialize() throw();

    static am_status_t 
	setDebugFileSize(const long DebugFileSize); 
    static am_status_t 
	setDebugFileRotate(bool debugFileRotate);
    
    static void setLockId(int);
    static int getLockId();

private:
    struct Module {
	explicit Module(const std::string& modName);

	std::string name;
	Level level;
    };

    static Mutex *lockPtr;
    static Mutex *rmtLockPtr;
    static std::vector<Module> *moduleList;
    static ModuleId allModule, remoteModule;

    static am_log_logger_func_t loggerFunc;

    static bool initialized;
    static bool remoteInitialized;

    static bool pSetLogFile(const std::string& name) throw();
    static ModuleId pAddModule(const std::string& name) throw();
    static Level pSetModuleLevel(ModuleId module, Level level) throw();
    static am_status_t pSetLevelsFromString(
		    const std::string& moduleLevelString) throw();

    static bool setAuditLogFile(const std::string& name) throw();
    
    static bool logRotation;
    static long maxLogFileSize;
    static bool auditLogRotation;
    static long maxAuditLogFileSize;

    static void writeLog(const char *hdr, const char *logMsg);
    static void writeAuditLog(const char *hdr, const char *logMsg);

    static int lockInstanceId;
    static char lock[32];
    static char alock[32];
#ifndef _MSC_VER
    static ino_t logInode;
    static sem_t *logRtLock;
    static ino_t alogInode;
    static sem_t *alogRtLock;
#else
    static HANDLE logRtLock;
    static HANDLE alogRtLock;
#endif

    static std::string logFileName;
    static std::string auditLogFileName;

};

END_PRIVATE_NAMESPACE

#endif	/* not LOG_H */
