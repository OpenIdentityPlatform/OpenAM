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
 * $Id: log.cpp,v 1.8 2009/12/04 19:30:21 subbae Exp $
 *
 */ 
#if (defined(WINNT) || defined(_AMD64_))
#include <stdio.h>
#include <stdlib.h>
#include <process.h>
#define	getpid	_getpid
#define vsnprintf _vsnprintf
#else
#include <unistd.h>
#endif
#include <cerrno>
#include<sys/types.h>
#include<sys/stat.h>
#include <cstdio>
#include <stdexcept>

#include <prthread.h>
#include <prtime.h>
#include <prprf.h>

#include <am.h>
#include <am_web.h>
#include <am_types.h>
#include "http.h"
#include "log.h"
#include "nspr_exception.h"
#include "properties.h"
#include "scope_lock.h"
#include "log_service.h"
#include "log_record.h"
#include "service_info.h"
#include "url.h"

#define REMOTE_LOG "RemoteLog"
#define ALL_LOG "all"

USING_PRIVATE_NAMESPACE

const Log::ModuleId Log::ALL_MODULES = 0;
const Log::ModuleId Log::REMOTE_MODULE = Log::ALL_MODULES+1;
Mutex *Log::lockPtr = new Mutex();
Mutex *Log::rmtLockPtr = new Mutex();
std::vector<Log::Module> *Log::moduleList = NULL;
bool Log::initialized = false;
bool Log::remoteInitialized = false;
bool Log::logRotation = true;
long Log::maxLogFileSize = 0;
int Log::currentLogFileSize = 0;
int Log::currentAuditLogFileSize = 0;
std::string logFileName;
std::string auditLogFileName;

Log::ModuleId Log::allModule, Log::remoteModule;
am_log_logger_func_t Log::loggerFunc = NULL;


#define getLevelString(x) \
(x == Log::LOG_AUTH_REMOTE)?sizeof(levelLabels) - 2:(x == Log::LOG_AUTH_LOCAL)?sizeof(levelLabels) - 1:static_cast<std::size_t>(x)

namespace {
    std::FILE *logFile = stderr;
    std::FILE *auditLogFile = stderr;
    const char *levelLabels[] = {
      "None", "Error", "Warning", "Info", "Debug", "MaxDebug", "Always",
	"Auth-Remote", "Auth-Local"
    };
    const size_t numLabels = sizeof(levelLabels) / sizeof(levelLabels[0]);
    
    static LogService* rmtLogSvc = NULL;
}

inline Log::Module::Module(const std::string& modName)
    : name(modName), level(LOG_DEBUG)
{
}

// initialize module vector ONLY.
am_status_t Log::initialize()
    throw()
{
    am_status_t status = AM_SUCCESS;
    ScopeLock scopeLock(*lockPtr);
    if (!initialized) {
	try {
	    moduleList = new std::vector<Module>;
	    // init ALL MODULE
	    moduleList->push_back(Module(ALL_LOG));
            allModule = ALL_MODULES;
	    (*moduleList)[allModule].level = LOG_ERROR;
	    // init REMOTE MODULE
	    moduleList->push_back(Module(REMOTE_LOG));
            remoteModule = allModule+1;
	    (*moduleList)[remoteModule].level = 
		static_cast<Level>(LOG_AUTH_REMOTE|LOG_AUTH_LOCAL);

            initialized = true;
	    status = AM_SUCCESS;

	} catch (std::exception&) {
	    if (moduleList) {
		delete moduleList;
                moduleList = NULL;
            }
	    status = AM_FAILURE;
	} catch (...) {
	    status = AM_FAILURE;
	}
    }
    return status;
}

// initialize module vector and log file, level and remote log service 
// according to parameters in properties.
am_status_t Log::initialize(const Properties& properties)
    throw()
{
    am_status_t status = AM_SUCCESS;
    // initialize module list.
    if (!initialized)
        status = initialize();

    if (status == AM_SUCCESS) {
	// initialize log file name and level from properties. 
	try {
	    ScopeLock myLock(*lockPtr);
	    logFileName = 
		properties.get(AM_AGENT_DEBUG_FILE_PROPERTY, "", false);
	    logRotation = 
	        properties.getBool(AM_AGENT_DEBUG_FILE_ROTATE_PROPERTY, true);
	    maxLogFileSize = 
	        properties.getPositiveNumber(AM_AGENT_DEBUG_FILE_SIZE_PROPERTY, 
                    DEBUG_FILE_DEFAULT_SIZE);
            if (maxLogFileSize < DEBUG_FILE_MIN_SIZE) {
               maxLogFileSize = DEBUG_FILE_MIN_SIZE;
            }

            auditLogFileName =
                properties.get(AM_AUDIT_LOCAL_LOG_FILE_PROPERTY, "", false);

	    if (! pSetLogFile(logFileName)) {
		log(ALL_MODULES, LOG_ERROR,
		    "Unable to open agent debug file: '%s', errno = %d",
		    logFileName.c_str(), errno);
            }

            if (!setAuditLogFile(
                   auditLogFileName,
                   properties.getBool(
                       AM_AUDIT_LOCAL_LOG_ROTATE_PROPERTY, true),
                   properties.getPositiveNumber(
                       AM_AUDIT_LOCAL_LOG_FILE_SIZE_PROPERTY, LOCAL_AUDIT_FILE_DEFAULT_SIZE))) 
            {
                log(ALL_MODULES, LOG_ERROR,
                    "Unable to open local audit file: '%s', errno = %d",
                    auditLogFileName.c_str(), errno);
            }

            // if no log level specified, set default to "all:LOG_INFO".
	    std::string logLevels = 
		properties.get(AM_AGENT_DEBUG_LEVEL_PROPERTY, "all:3", false);
	    status = pSetLevelsFromString(logLevels); 
	}
	catch (NSPRException& exn) {
	    status = AM_NSPR_ERROR;
	}
	catch (std::bad_alloc& exb) {
	    status = AM_NO_MEMORY;
	}
	catch (std::exception& exs) {
	    status = AM_INVALID_ARGUMENT;
	}
	catch (...) {
	    status = AM_FAILURE;
	}
    }
    return status;
}

// initialize remote log from the properties file. 
// ideally this is done in Log::initialize however due to 
// the dependency of remote log on connection which depends on log 
// (see base_init in am_main.cpp) this is done seperately.
am_status_t Log::initializeRemoteLog(const Properties& propertiesRef) 
    throw()
{
    am_status_t status = AM_SUCCESS;

    // Create logging servcie URL from naming service since no
    // sso token is available to read the naming service.

    try {
	const std::string namingservice("/namingservice");
	const std::string loggingservice("/loggingservice");
	const std::string namingURL(
	    propertiesRef.get(AM_COMMON_NAMING_URL_PROPERTY, ""));
	std::string logURL = namingURL;
	std::size_t pos = 0;

	pos = logURL.find (namingservice, pos);
	while (pos != std::string::npos) {
	    logURL.replace(pos, namingservice.size(), loggingservice);
	    pos = logURL.find (namingservice, pos + 1);
	}

	URL verifyURL(logURL);
	LogService *newLogSvc = 
	    new LogService(ServiceInfo(logURL),
			   propertiesRef,
			   propertiesRef.get(
			       AM_COMMON_CERT_DB_PASSWORD_PROPERTY,""),
			   propertiesRef.get(
			       AM_AUTH_CERT_ALIAS_PROPERTY, ""),
			   propertiesRef.getBool(
			       AM_COMMON_TRUST_SERVER_CERTS_PROPERTY, false),
			   1);
	Log::setRemoteInfo(newLogSvc);
    }
    catch (std::bad_alloc& exb) {
	status = AM_NO_MEMORY;
    }
    catch (std::exception& exs) {
	status = AM_INVALID_ARGUMENT;
    }
    catch (...) {
	status = AM_FAILURE;
    }
    return status;
}



void Log::shutdown()
    throw()
{
    am_status_t status = AM_SUCCESS;
    if (initialized) {
	log(Log::ALL_MODULES, Log::LOG_MAX_DEBUG, 
            "Log::shutdown(): Log service being terminated.");

        // flush any outstanding remote log buffers.
	status = Log::rmtflush();
	if (status != AM_SUCCESS) {
	    log(Log::ALL_MODULES, Log::LOG_ERROR, 
                "Log::shutdown(): Error flushing remote log: %s.", 
		am_status_to_string(status));
	}

        {
        ScopeLock mylock(*lockPtr);
	pSetLogFile("");
        if (moduleList) {
            initialized = false;
            delete moduleList;
            moduleList = NULL;
        }
	}
        ScopeLock rmtLock(*rmtLockPtr);
        if (rmtLogSvc) {
            remoteInitialized = false;
	    delete rmtLogSvc;
            rmtLogSvc = NULL;
        }
    } 
}

am_status_t Log::setRemoteInfo(LogService *newLogService) 
    throw()
{
    am_status_t status = AM_SUCCESS;
    if(newLogService != NULL) {
	ScopeLock myLock(*rmtLockPtr);
	LogService *oldRemoteLogInfo = rmtLogSvc;
	rmtLogSvc = newLogService;

	if (oldRemoteLogInfo != NULL) {
	    delete oldRemoteLogInfo;
	}
	remoteInitialized = true;
    }
    else {
	status = AM_INVALID_ARGUMENT;
    }
    return status;
}

bool Log::setLogFile(const std::string& name)
    throw()
{
    return pSetLogFile(name);
}

bool Log::pSetLogFile(const std::string& name)
    throw()
{
    bool okay = true;
    std::string newName;
    FILE *newLogFile = NULL;
    int retValue = 0;

    if (name.size() > 0) {
     if (logRotation) {
          char hdr[100];
          PRUint32 len;
          int counter = 1;
          bool fileExists = true;

          if (logFile == stderr) { 
	     // Open the log file for the first time
             newLogFile = std::fopen(name.c_str(), "a");
             if (newLogFile != NULL) {
                 logFile = newLogFile;
             } else {
	       okay = false;
	     }
	  } else { 
	     // Start the process of log rotation
	     while (fileExists) {
                len = PR_snprintf(hdr, sizeof(hdr), "%d", counter);
                if (len > 0) {
                   std::string appendString(hdr);
                   newName = name + "-" + appendString;
                }
                if ((PR_Access(newName.c_str(),PR_ACCESS_EXISTS))==PR_SUCCESS) {
	            counter++;
                } else {
                    fileExists = false;
                }
             }
             std::fflush(logFile);
             std::fclose(logFile);
	     retValue = rename(name.c_str(), newName.c_str());
	     if (retValue != -1) {
	         newLogFile = std::fopen(name.c_str(), "a");
	         if (newLogFile != NULL) {
		     logFile = newLogFile;
		     currentLogFileSize = 0;
	         } else {
		    logFile = stderr;
                 }
             } else {
		logFile = stderr;
	     }
          }
       } else {
	   // logRotation is false, just create one log file and write to it
	   newLogFile = std::fopen(name.c_str(), "a");
	   if (newLogFile != NULL) {
	       logFile = newLogFile;
	   } else {
	       okay = false;
	   }
       }
    } else {
      okay = false;
    }

    return okay;
}

bool Log::setAuditLogFile(const std::string& name,
     bool localAuditLogRotate,
     long localAuditFileSize)
throw () {
    bool okay = true;
    std::string newName;
    FILE *newAuditLogFile = NULL;
    int retValue = 0;

    if (name.size() > 0) {
        if (localAuditLogRotate) {
            char hdr[100];
            PRUint32 len;
            int counter = 1;
            bool fileExists = true;

            if (auditLogFile == stderr) {
                // Open the log file for the first time
                newAuditLogFile = std::fopen(name.c_str(), "a");
                if (newAuditLogFile != NULL) {
                    auditLogFile = newAuditLogFile;
                } else {
                    okay = false;
                }
            } else {
                // Start the process of log rotation
                while (fileExists) {
                    len = PR_snprintf(hdr, sizeof (hdr), "%d", counter);
                    if (len > 0) {
                        std::string appendString(hdr);
                        newName = name + "-" + appendString;
                    }
                    if ((PR_Access(newName.c_str(), PR_ACCESS_EXISTS)) == 
                            PR_SUCCESS) {
                        counter++;
                    } else {
                        fileExists = false;
                    }
                }
                std::fflush(auditLogFile);
                std::fclose(auditLogFile);
                retValue = rename(name.c_str(), newName.c_str());
                if (retValue != -1) {
                    newAuditLogFile = std::fopen(name.c_str(), "a");
                    if (newAuditLogFile != NULL) {
                        auditLogFile = newAuditLogFile;
                        currentAuditLogFileSize = 0;
                    } else {
                        auditLogFile = stderr;
                    }
                } else {
                    auditLogFile = stderr;
                }
            }
        } else {
            // localAuditLogRotate is false, just create one log file and write to it
            newAuditLogFile = std::fopen(name.c_str(), "a");
            if (newAuditLogFile != NULL) {
                auditLogFile = newAuditLogFile;
            } else {
                okay = false;
            }
        }
    } else {
        okay = false;
    }

    return okay;
}

Log::ModuleId Log::addModule(const std::string& name)
    throw()
{
    Log::ModuleId module = 0;
    am_status_t status = AM_SUCCESS;
    if (!initialized && 
	(status = initialize()) != AM_SUCCESS) {
	Log::log(Log::ALL_MODULES, Log::LOG_ERROR, 
		 "Log::addModule(): Error initializing log.");
    }
    else {
	ScopeLock myLock(*lockPtr);
	module = pAddModule(name);
    }
    return module;
}

// same as addModule but with no locking
Log::ModuleId Log::pAddModule(const std::string& name)
    throw()
{
    ModuleId module = 0;

    try {
	for (module = 0; module < moduleList->size(); ++module) {
	    if ((*moduleList)[module].name == name) {
		break;
	    }
	}
	if (module == moduleList->size()) {
	    moduleList->push_back(Module(name));
	    // The initial level defaults to the current level for ALL_MODULES.
	    (*moduleList)[module].level = (*moduleList)[ALL_MODULES].level;
	}
    }
    catch (...) {
	log(ALL_MODULES, LOG_ERROR,
	    "Could not add module %s. Unknown exception caught.", name.c_str());
    }
    return module;
}

Log::Level Log::setModuleLevel(ModuleId module, Level level)
    throw()
{
    Log::Level oldLevel = Log::LOG_NONE;
    am_status_t status = AM_SUCCESS;

    if (!initialized && 
	(status = initialize()) != AM_SUCCESS) {
	Log::log(Log::ALL_MODULES, Log::LOG_ERROR, 
		 "Log::setModuleLevel(): Cannot set module level. "
		 "Log initialization failed with %s", 
		 am_status_to_string(status));
	oldLevel= Log::LOG_NONE;
    }
    else {
	ScopeLock myLock(*lockPtr);
	oldLevel = pSetModuleLevel(module, level);
    }
    return oldLevel;
}

// same as setModuleLevel but with no locking.
Log::Level Log::pSetModuleLevel(ModuleId module, Level level)
    throw()
{
    Level oldLevel = LOG_NONE; 

    if (ALL_MODULES == module) {
	oldLevel = (*moduleList)[ALL_MODULES].level;
	for (unsigned int i = 0; i < moduleList->size(); i++) {
	    /* if the level is to turn of everything, then turn off
	     * remote logging too. Otherwise, don't change the remote
	     * logging settings. */
	    if(i != remoteModule || level == LOG_NONE)
		(*moduleList)[i].level = level;
	}
    } else if (module < moduleList->size()) {
	oldLevel = (*moduleList)[module].level;
        if (level <= LOG_NONE)
             level = LOG_NONE;
	(*moduleList)[module].level = level;
    } else {
	Log::log(Log::ALL_MODULES, Log::LOG_ERROR, "Invalid module %d", module);
	oldLevel = LOG_NONE;
    }
    return oldLevel;
}

am_status_t Log::setLevelsFromString(const std::string& logLevels)
    throw()
{
    am_status_t status = AM_SUCCESS;
    ScopeLock mylock(*lockPtr);
    status = pSetLevelsFromString(logLevels);
    return status;
}

am_status_t Log::pSetLevelsFromString(const std::string& logLevels)
    throw()
{
    am_status_t status = AM_SUCCESS;
    std::size_t offset = 0;

    while (offset < logLevels.size()) {
	offset = logLevels.find_first_not_of(' ', offset);
	if (offset < logLevels.size()) {
	    std::size_t end;
	    std::size_t next;
	    long level, oldLevel;
	    ModuleId module;

	    end = logLevels.find_first_of(':', offset);
	    next = logLevels.find_first_of(',', offset);
	    if (next < end) {
		end = next;
	    }
	    module = pAddModule(std::string(logLevels, offset, end - offset));
	    if (end != std::string::npos && ':' == logLevels[end++] &&
		end < logLevels.size() &&
		1 == std::sscanf(&logLevels.c_str()[end], "%ld", &level)) {
		oldLevel = pSetModuleLevel(module, static_cast<Level>(level));
		Log::log(Log::ALL_MODULES, Log::LOG_DEBUG, 
			 "Log::pSetLevelsFromString(): setting log level "
			 "for module %d to %d, old level %d.",
			 module, level, oldLevel);
	    }

	    if (next != std::string::npos) {
		offset = next + 1;
	    } else {
		offset = next;
	    }
	}
    }
    return status;
}

bool Log::isLevelEnabled(ModuleId module, Level level)
    throw()
{
    bool enabled;

    if (initialized) {

        if (module >= moduleList->size()) {
	    module = ALL_MODULES;
        }

        enabled = ((*moduleList)[module].level >= level);
    } else {
	enabled = false;
    }

    return enabled;
}

// this should not throw exception since it is called often in a 
// catch block to log an exception.
void Log::log(ModuleId module, Level level, const std::exception &ex) 
    throw()
{
    if(!isLevelEnabled(module, level))
	return;

    // Print the exception information
    log(module, level, "Exception encountered: %s.", ex.what());
    return;
}

// this should not throw exception since it is called often in a 
// catch block to log an error resulting from an exception.
void Log::log(ModuleId module, Level level, const InternalException &ex) 
    throw()
{
    if(!isLevelEnabled(module, level))
	return;

    am_status_t statusCode = ex.getStatusCode();
    const char *status_desc = am_status_to_string(statusCode);
    // Print the exception information
    log(module, level, "Exception InternalException thrown "
	"with message: \"%s\" and status message: %s", 
	ex.getMessage(), status_desc);
    return;
}

// this should not throw exception since it is called often in a 
// catch block to log an error resulting from an exception.
void Log::log(ModuleId module, Level level,
	      const char *format, ...) 
    throw()
{
    std::va_list args;

    va_start(args, format);
    vlog(module, level, format, args);
    va_end(args);
}

// this should not throw exception since it is called often in a 
// catch block to log an error resulting from an exception.
void Log::vlog(ModuleId module, Level level, const char *format,
	       std::va_list args) 
    throw()
{
    if (initialized) {
	if (module >= moduleList->size()) {
	    module = ALL_MODULES;
	}

        char *logMsg = PR_vsmprintf(format, args);
	// call user defined logger if any.
	if (loggerFunc != NULL) {
	    loggerFunc((*moduleList)[module].name.c_str(),  
		       static_cast<am_log_level_t>(static_cast<int>(level)),
		       logMsg);
	}

	// do default log.
	if ((*moduleList)[module].level >= level) {

	    // format: 
	    // year-month-day hour:min:sec.usec level pid:thread module: msg
	    // get level string		
	    std::size_t levelLabelIndex = getLevelString(level);
	    char levelStr[50]; 
	    PRUint32 llen;
	    if (levelLabelIndex < numLabels) {
		llen = PR_snprintf(levelStr, sizeof(levelStr),
				       "%s", levelLabels[levelLabelIndex]);
	    } else {
		llen = PR_snprintf(levelStr, sizeof(levelStr), "%d", level);
	    }

	    if (llen > 0) { 
		// get time.
		PRExplodedTime now;
		PR_ExplodeTime(PR_Now(), PR_LocalTimeParameters, &now);
    
		// format header and msg.
		PRUint32 len;
		char hdr[100];
		len = PR_snprintf(hdr, sizeof(hdr), 
				  "%d-%02d-%02d %02d:%02d:%02d.%03d"
				  "%8s %u:%p %s: %%s\n",
				  now.tm_year, now.tm_month+1, now.tm_mday,
				  now.tm_hour, now.tm_min, now.tm_sec, 
				  now.tm_usec / 1000,
				  levelStr,
				  getpid(), PR_GetCurrentThread(),
				  (*moduleList)[module].name.c_str());
		if (len > 0) {
                  if (logRotation) {
                    if ((currentLogFileSize + 1000) < maxLogFileSize) {
		       std::fprintf(logFile, hdr, logMsg);
		       std::fflush(logFile);
                    } else {
    		      ScopeLock scopeLock(*lockPtr);
                      currentLogFileSize = ftell(logFile);
                      if ((currentLogFileSize + 1000) > maxLogFileSize) {
                         // Open a new log file
	                 if (!pSetLogFile(logFileName)) {
		                 log(ALL_MODULES, LOG_ERROR,
		                 "Unable to open log file: '%s', errno = %d",
		                 logFileName.c_str(), errno);
	                }
                      }
		      std::fprintf(logFile, hdr, logMsg);
		      std::fflush(logFile);
                    }
                    currentLogFileSize = ftell(logFile);
                  } else {
		      std::fprintf(logFile, hdr, logMsg);
		      std::fflush(logFile);
                  }
		}
	     }
	}

	// Remote Logging starts here.
	if (module == remoteModule) {
	    if (remoteInitialized) {
		bool doLogRemotely = 
		    ((*moduleList)[module].level >= Log::LOG_AUTH_REMOTE) &&
		    ((*moduleList)[module].level & level);

		if (doLogRemotely) {
		    am_status_t status;
		    status = rmtLogSvc->logMessage(logMsg);
		    if(status != AM_SUCCESS) {
			Log::log(Log::ALL_MODULES, Log::LOG_ERROR,
			    "Log::vlog(): Error logging message [%s] "
			    "to remote server. Error: %s.", 
			    logMsg, am_status_to_string(status));
		    }
		}
	    } else {
		Log::log(Log::ALL_MODULES, Log::LOG_ERROR,
		    "Log::vlog(): Remote logging service not initialized. "
		    "Cannot log message to remote server.");
	    }
	}
	PR_smprintf_free(logMsg);
    }
    return;
}

/*
 * Log url access audit message to local audit log file.
 *
 */
void Log::doLocalAuditLog(ModuleId module,
        Level level,
        const char* auditLogMsg,
        bool localAuditLogRotate,
        long localAuditFileSize)
throw () {
    if (initialized) {

        // get time.
        PRExplodedTime now;
        PR_ExplodeTime(PR_Now(), PR_LocalTimeParameters, &now);

        // format header and msg.
        PRUint32 len;
        char hdr[100];
        len = PR_snprintf(hdr, sizeof (hdr),
                "%d-%02d-%02d %02d:%02d:%02d.%03d"
                "%8s %u:%p %s: %%s\n",
                now.tm_year, now.tm_month + 1, now.tm_mday,
                now.tm_hour, now.tm_min, now.tm_sec,
                now.tm_usec / 1000,
                "Info",
                getpid(), PR_GetCurrentThread(),
                "LocalAuditLog");
        if (len > 0) {

            if (localAuditLogRotate) {
                if ((currentAuditLogFileSize + 1000) < 
                        localAuditFileSize) {
                    std::fprintf(auditLogFile, hdr, auditLogMsg);
                    std::fflush(auditLogFile);
                } else {
                    ScopeLock scopeLock(*lockPtr);
                    currentAuditLogFileSize = ftell(auditLogFile);
                    if ((currentAuditLogFileSize + 1000) > 
                            localAuditFileSize) {
                        // Open a new log file
                        if (!setAuditLogFile(auditLogFileName,
                                              localAuditLogRotate,
                                              localAuditFileSize)) {
                            log(ALL_MODULES, LOG_ERROR,
                                    "Unable to open audit log file: "
                                    "'%s', errno = %d",
                                    auditLogFileName.c_str(), errno);
                        }
                    }
                    std::fprintf(auditLogFile, hdr, auditLogMsg);
                    std::fflush(auditLogFile);
                }
                currentAuditLogFileSize = ftell(auditLogFile);
            } else {
                std::fprintf(auditLogFile, hdr, auditLogMsg);
                std::fflush(auditLogFile);
            }
        }
    }

    return;
}

// log a message to remote server with a user token id given 
// for the remote server to fill in with user token details.

am_status_t
Log::rlog(ModuleId module, int remote_log_level,
        const char *user_sso_token, const char *format, ...) throw () {
    am_status_t status = AM_SUCCESS;
    char *logMsg = NULL;
    std::string logMessage;
    bool cookieEncoded = false;

    if (rmtLogSvc == NULL || !remoteInitialized) {
        status = AM_SERVICE_NOT_INITIALIZED;
    } else {
        std::va_list args;
        va_start(args, format);
        logMsg = PR_vsmprintf(format, args);
        logMessage = logMsg;
        if (logMsg != NULL) {
            if (logMsg[0] == '\0') {
                Log::log(Log::ALL_MODULES, Log::LOG_WARNING, "Log Record Message is empty");
                if (logMsg != NULL) PR_smprintf_free(logMsg);
                va_end(args);
                return status;
            }
            try {
                LogRecord logRecord(static_cast<LogRecord::Level> (remote_log_level), logMessage);
                std::string userSSOToken = user_sso_token;
                cookieEncoded = userSSOToken.find('%') != std::string::npos;
                if (cookieEncoded) {
                    userSSOToken = Http::decode(std::string(user_sso_token));
                }
                logRecord.populateTokenDetails(userSSOToken);
                status = rmtLogSvc->sendLog("", logRecord, "");
            } catch (std::exception& exs) {
                status = AM_FAILURE;
            } catch (...) {
                status = AM_FAILURE;
            }
            PR_smprintf_free(logMsg);
        }
        va_end(args);
    }
    return status;
}

am_status_t Log::rlog(const std::string& logName,
		      const LogRecord& record,
		      const std::string& loggedByTokenID) 
    throw()
{
    am_status_t status = AM_FAILURE;
    bool cookieEncoded = true;

    if (rmtLogSvc != NULL && remoteInitialized ) {
	try {
	    std::string loggedByID = loggedByTokenID;
	    cookieEncoded = loggedByID.find('%') != std::string::npos;
	    if (cookieEncoded) {
		loggedByID = Http::decode(loggedByTokenID);
	    }
	    status = rmtLogSvc->sendLog(logName,
					record,
					loggedByID);
	}
	catch (std::exception& exs) {
	    Log::log(Log::ALL_MODULES, Log::LOG_ERROR,
		    "Log::rlog(): exception encountered %s.", exs.what());
	    status = AM_INVALID_ARGUMENT;
	}
	catch (...) {
	    Log::log(Log::ALL_MODULES, Log::LOG_ERROR,
		    "Log::rlog(): unknown exception encountered.");
	    status = AM_INVALID_ARGUMENT;
	}
    } else {
	Log::log(Log::ALL_MODULES, Log::LOG_ERROR,
		"Log::rlog(): Remote logging not initialized.");
        status = AM_REMOTE_LOG_NOT_INITIALIZED;
    }
    return status;
}

/*
 * Log url access audit message to remote audit log file.
 *
 */
am_status_t 
Log::doRemoteAuditLog(ModuleId module, 
        int remote_log_level, 
        const char *user_sso_token, 
        const char *logMsg)
{
    am_status_t status = AM_SUCCESS;
    std::string logMessage;
    bool cookieEncoded = false;

    if (rmtLogSvc == NULL || !remoteInitialized) { 
	status = AM_SERVICE_NOT_INITIALIZED;
    }
    else {
	if (logMsg != NULL) {
            logMessage = logMsg;
	    try {
		LogRecord logRecord(
			    static_cast<LogRecord::Level>(remote_log_level), 
			    logMessage);
	        std::string userSSOToken = user_sso_token;
		cookieEncoded = userSSOToken.find('%') != std::string::npos;
	        if (cookieEncoded) {
		    userSSOToken = Http::decode(std::string(user_sso_token));
	        }
		logRecord.populateTokenDetails(userSSOToken);
		status = rmtLogSvc->sendLog("", logRecord, "");	
	    }
	    catch (std::exception& exs) {
		status = AM_FAILURE;
	    }
	    catch (...) {
		status = AM_FAILURE;
	    }
	    
	}
    }
    return status;
}

/*
 * Log url access audit message. This calls doLocalAuditLog()
 * or doRemoteAuditLog() or both methods based on 
 * log.disposition property value.
 */
am_status_t 
Log::auditLog(const char* auditDisposition,
        bool localAuditLogRotate,
        long localAuditFileSize,
        ModuleId module, 
        int remoteLogLevel, 
        const char *userSSOToken, 
        const char *format, 
        ...)
{
    am_status_t status = AM_SUCCESS;

    int size = MSG_MAX_LEN;
    std::vector<char> logMsg(size);
    va_list args;
    va_start(args, format);
    int needed = vsnprintf(&logMsg[0], logMsg.size(), format, args);
    va_end(args);
    if (needed >= size) {
        logMsg.resize(needed + 1);
        va_start(args, format);
        needed = vsnprintf(&logMsg[0], logMsg.size(), format, args);
        va_end(args);
    }
    if(&logMsg[0] != NULL) {
        if ((strcasecmp(auditDisposition, AUDIT_DISPOSITION_REMOTE) == 0) ||
            (strcasecmp(auditDisposition, AUDIT_DISPOSITION_ALL) == 0)) {
            status = doRemoteAuditLog(module,
                remoteLogLevel,
                userSSOToken,
                &logMsg[0]
                );
        }
        if (status != AM_SUCCESS ||
            (strcasecmp(auditDisposition, AUDIT_DISPOSITION_LOCAL) == 0) ||
            (strcasecmp(auditDisposition, AUDIT_DISPOSITION_ALL) == 0)) {
            try {
                doLocalAuditLog(module,
                    Log::LOG_INFO,
                    &logMsg[0],
                    localAuditLogRotate,
                    localAuditFileSize);
            } catch(...) {
                status = AM_FAILURE;    
            }
        }
    }
    if (status != AM_SUCCESS) {
        Log::log(Log::ALL_MODULES, Log::LOG_ERROR,
                "Log::auditLog(): Both local and remote audit logging failed.");
    }
    return status;
}

am_status_t Log::rmtflush() 
    throw()
{
    am_status_t status = AM_FAILURE;

    if (rmtLogSvc != NULL && remoteInitialized ) {

        status = rmtLogSvc->flushBuffer();
        if(status != AM_SUCCESS) {
	    log(ALL_MODULES, LOG_ERROR,
		"Log::rmtflush(): Error flushing buffer to remote server: %s",
		am_status_to_string(status));
        }
    } else {
	log(ALL_MODULES, LOG_ERROR,
	    "Log::rmtflush(): Error flushing buffer to remote server: "
	    "remote log service not initialized.");
    }
    return status;
}


am_log_logger_func_t 
Log::setLogger(am_log_logger_func_t logger_func) 
{
    am_log_logger_func_t oldLogger = NULL;
    if (initialized) {
        oldLogger = loggerFunc;
        loggerFunc = logger_func;
    }
    return oldLogger;
}

am_status_t Log::setDebugFileSize(const long debugFileSize)
{
    am_status_t status = AM_SUCCESS;
    ScopeLock mylock(*lockPtr);
    maxLogFileSize = debugFileSize;
    if (maxLogFileSize < DEBUG_FILE_MIN_SIZE) {
        maxLogFileSize = DEBUG_FILE_MIN_SIZE;
    }
    return status;
}

am_status_t Log::setDebugFileRotate(bool debugFileRotate)
{
    am_status_t status = AM_SUCCESS;
    ScopeLock mylock(*lockPtr);
    logRotation = debugFileRotate;
    return status;
}
