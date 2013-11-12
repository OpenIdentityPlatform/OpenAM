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
/*
 * Portions Copyrighted 2013 ForgeRock Inc
 */

#ifdef _MSC_VER
#include <stdio.h>
#include <stdlib.h>
#include <process.h>
#include <io.h>
#define	getpid	_getpid
#define vsnprintf _vsnprintf
#else
#include <unistd.h>
#include <fcntl.h>
#endif
#include <stdint.h>
#include <cerrno>
#include <sys/types.h>
#include <time.h>
#include <sys/stat.h>
#include <cstdio>
#include <stdexcept>
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
#include <iostream>
#include <sstream>
#include <iomanip>
#include <string>

#define REMOTE_LOG "RemoteLog"
#define ALL_LOG "all"

#ifdef _MSC_VER
#define TEMP_SIZE 8192 * 3

static void debug(const char *format, ...) {
    char tmp[TEMP_SIZE], *p = tmp;
    va_list args;
    va_start(args, format);
    p += vsnprintf(p, sizeof (tmp), format, args);
    va_end(args);
    while (p > tmp && isspace(p[-1]))
        p--;
    *p++ = '\n';
    *p = '\0';
    OutputDebugStringA(tmp);
}

static void rotate_log(HANDLE file, const char *fn, size_t ms) {
    BY_HANDLE_FILE_INFORMATION info;
    uint64_t fsize = 0;
    if (GetFileInformationByHandle(file, &info)) {
        fsize = ((DWORDLONG) (((DWORD) (info.nFileSizeLow)) | (((DWORDLONG) ((DWORD) (info.nFileSizeHigh))) << 32)));
    }
    if ((fsize + 1024) > ms) {
        char tmp[MAX_PATH];
        unsigned int idx = 1;
        do {
            ZeroMemory(&tmp[0], sizeof (tmp));
            sprintf_s(tmp, sizeof (tmp), "%s.%d", fn, idx);
            idx++;
        } while (_access(tmp, 0) == 0);
        if (CopyFileA(fn, tmp, FALSE)) {
            SetFilePointer(file, 0, NULL, FILE_BEGIN);
            SetEndOfFile(file);
        } else {
            debug("Could not copy %s file, error: %d", fn, GetLastError());
        }
    }
}

#endif

USING_PRIVATE_NAMESPACE

const Log::ModuleId Log::ALL_MODULES = 0;
const Log::ModuleId Log::REMOTE_MODULE = Log::ALL_MODULES + 1;
Mutex *Log::lockPtr = new Mutex();
Mutex *Log::rmtLockPtr = new Mutex();
std::vector<Log::Module> *Log::moduleList = NULL;
bool Log::initialized = false;
bool Log::remoteInitialized = false;
bool Log::logRotation = true;
long Log::maxLogFileSize = 0;
bool Log::auditLogRotation = true;
long Log::maxAuditLogFileSize = 0;
std::string Log::logFileName = "";
std::string Log::auditLogFileName = "";

#ifndef _MSC_VER
#define LOG_LOCK "/am_log_lock"
#define ALOG_LOCK "/am_alog_lock"
#define LOG_LOCK_DESTROY(name)         do {\
        sem_t *sem = sem_open(name, O_EXCL, S_IRUSR | S_IWUSR | S_IRGRP | S_IWGRP); \
        if (sem != SEM_FAILED) {sem_post(sem);sem_close(sem);} sem_unlink(name);} while (0)
ino_t Log::logInode = 0;
sem_t *Log::logRtLock = NULL;
ino_t Log::alogInode = 0;
sem_t *Log::alogRtLock = NULL;
#else
#define LOG_LOCK "Global\\am_log_lock"
#define ALOG_LOCK "Global\\am_alog_lock"
HANDLE Log::logRtLock = NULL;
HANDLE Log::alogRtLock = NULL;
#endif

Log::ModuleId Log::allModule, Log::remoteModule;
am_log_logger_func_t Log::loggerFunc = NULL;
char Log::lock[32];
char Log::alock[32];
int Log::lockInstanceId = 0;

#define getLevelString(x) \
(x == Log::LOG_AUTH_REMOTE)?sizeof(levelLabels) - 2:(x == Log::LOG_AUTH_LOCAL)?sizeof(levelLabels) - 1:static_cast<std::size_t>(x)

namespace {

#ifndef _MSC_VER
    std::FILE *logFile = stderr;
    std::FILE *auditLogFile = stderr;
#else
    HANDLE logFile = INVALID_HANDLE_VALUE;
    HANDLE auditLogFile = INVALID_HANDLE_VALUE;
#endif
    const char *levelLabels[] = {
        "None", "Error", "Warning", "Info", "Debug", "MaxDebug", "Always",
        "Auth-Remote", "Auth-Local"
    };
    const size_t numLabels = sizeof (levelLabels) / sizeof (levelLabels[0]);

    static LogService* rmtLogSvc = NULL;
}

void Log::setLockId(int id) {
    snprintf(lock, sizeof (lock), "%s_%d", LOG_LOCK, id);
    snprintf(alock, sizeof (alock), "%s_%d", ALOG_LOCK, id);
    lockInstanceId = id;
}

int Log::getLockId() {
    return lockInstanceId;
}

inline Log::Module::Module(const std::string& modName)
: name(modName), level(LOG_DEBUG) {
}

// initialize module vector ONLY.

am_status_t Log::initialize()
throw () {
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
            remoteModule = allModule + 1;
            (*moduleList)[remoteModule].level =
                    static_cast<Level> (LOG_AUTH_REMOTE | LOG_AUTH_LOCAL);

#ifndef _MSC_VER
            LOG_LOCK_DESTROY(lock);
            logRtLock = sem_open(lock, O_CREAT, S_IRUSR | S_IWUSR | S_IRGRP | S_IWGRP, 1);
            LOG_LOCK_DESTROY(alock);
            alogRtLock = sem_open(alock, O_CREAT, S_IRUSR | S_IWUSR | S_IRGRP | S_IWGRP, 1);
            if (logRtLock != SEM_FAILED && alogRtLock != SEM_FAILED) {
                initialized = true;
                status = AM_SUCCESS;
            } else {
                delete moduleList;
                moduleList = NULL;
                initialized = false;
                status = AM_FAILURE;
                LOG_LOCK_DESTROY(lock);
                LOG_LOCK_DESTROY(alock);
            }
#else
            logRtLock = CreateMutexA(NULL, FALSE, lock);
            if (logRtLock == NULL && GetLastError() == ERROR_ACCESS_DENIED) {
                logRtLock = OpenMutexA(SYNCHRONIZE, FALSE, lock);
            }
            alogRtLock = CreateMutexA(NULL, FALSE, alock);
            if (alogRtLock == NULL && GetLastError() == ERROR_ACCESS_DENIED) {
                alogRtLock = OpenMutexA(SYNCHRONIZE, FALSE, alock);
            }
            if (logRtLock != NULL && alogRtLock != NULL) {
                initialized = true;
                status = AM_SUCCESS;
            } else {
                delete moduleList;
                moduleList = NULL;
                initialized = false;
                status = AM_FAILURE;
                if (logRtLock != NULL) {
                    CloseHandle(logRtLock);
                    logRtLock = NULL;
                }
                if (alogRtLock != NULL) {
                    CloseHandle(alogRtLock);
                    alogRtLock = NULL;
                }
            }
#endif

        } catch (std::exception&) {
            delete moduleList;
            moduleList = NULL;
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
throw () {
    am_status_t status = AM_SUCCESS;
    // initialize module list.
    if (!initialized)
        status = initialize();

    if (status == AM_SUCCESS) {
        // initialize log file name and level from properties. 
        try {
            ScopeLock myLock(*lockPtr);
            logFileName = properties.get(AM_AGENT_DEBUG_FILE_PROPERTY, "", false);
            logRotation = properties.getBool(AM_AGENT_DEBUG_FILE_ROTATE_PROPERTY, true);
            maxLogFileSize = properties.getPositiveNumber(AM_AGENT_DEBUG_FILE_SIZE_PROPERTY,
                    DEBUG_FILE_DEFAULT_SIZE);
            if (maxLogFileSize < DEBUG_FILE_MIN_SIZE) {
                maxLogFileSize = DEBUG_FILE_MIN_SIZE;
            }

            if (!pSetLogFile(logFileName)) {
                log(ALL_MODULES, LOG_ERROR,
                        "Unable to open agent debug file: '%s', errno = %d",
                        logFileName.c_str(), errno);
            }

            auditLogFileName = properties.get(AM_AUDIT_LOCAL_LOG_FILE_PROPERTY, "", false);
            auditLogRotation = properties.getBool(AM_AUDIT_LOCAL_LOG_ROTATE_PROPERTY, true);
            maxAuditLogFileSize = properties.getPositiveNumber(AM_AUDIT_LOCAL_LOG_FILE_SIZE_PROPERTY,
                    LOCAL_AUDIT_FILE_DEFAULT_SIZE);
            if (maxAuditLogFileSize < DEBUG_FILE_MIN_SIZE) {
                maxAuditLogFileSize = DEBUG_FILE_MIN_SIZE;
            }

            if (!setAuditLogFile(auditLogFileName)) {
                log(ALL_MODULES, LOG_ERROR,
                        "Unable to open local audit file: '%s', errno = %d",
                        auditLogFileName.c_str(), errno);
            }

            // if no log level specified, set default to "all:LOG_INFO".
            std::string logLevels =
                    properties.get(AM_AGENT_DEBUG_LEVEL_PROPERTY, "all:3", false);
            status = pSetLevelsFromString(logLevels);
        } catch (NSPRException& exn) {
            status = AM_NSPR_ERROR;
        } catch (std::bad_alloc& exb) {
            status = AM_NO_MEMORY;
        } catch (std::exception& exs) {
            status = AM_INVALID_ARGUMENT;
        } catch (...) {
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
throw () {
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

        pos = logURL.find(namingservice, pos);
        while (pos != std::string::npos) {
            logURL.replace(pos, namingservice.size(), loggingservice);
            pos = logURL.find(namingservice, pos + 1);
        }

        URL verifyURL(logURL);
        LogService *newLogSvc =
                new LogService(ServiceInfo(logURL),
                propertiesRef, 1);
        Log::setRemoteInfo(newLogSvc);
    } catch (std::bad_alloc& exb) {
        status = AM_NO_MEMORY;
    } catch (std::exception& exs) {
        status = AM_INVALID_ARGUMENT;
    } catch (...) {
        status = AM_FAILURE;
    }
    return status;
}

void Log::shutdown()
throw () {
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

#ifndef _MSC_VER
        if (logFile != NULL && logFile != stderr) {
            std::fclose(logFile);
            logFile = NULL;
        }
        if (auditLogFile != NULL && auditLogFile != stderr) {
            std::fclose(auditLogFile);
            auditLogFile = NULL;
        }
        LOG_LOCK_DESTROY(lock);
        LOG_LOCK_DESTROY(alock);
#else
        if (logFile != INVALID_HANDLE_VALUE) {
            CloseHandle(logFile);
            logFile = INVALID_HANDLE_VALUE;
        }
        if (auditLogFile != INVALID_HANDLE_VALUE) {
            CloseHandle(auditLogFile);
            auditLogFile = INVALID_HANDLE_VALUE;
        }
        if (logRtLock != NULL) {
            CloseHandle(logRtLock);
            logRtLock = NULL;
        }
        if (alogRtLock != NULL) {
            CloseHandle(alogRtLock);
            alogRtLock = NULL;
        }
#endif
    }
}

am_status_t Log::setRemoteInfo(LogService *newLogService)
throw () {
    am_status_t status = AM_SUCCESS;
    if (newLogService != NULL) {
        ScopeLock myLock(*rmtLockPtr);
        LogService *oldRemoteLogInfo = rmtLogSvc;
        rmtLogSvc = newLogService;
        delete oldRemoteLogInfo;
        remoteInitialized = true;
    } else {
        status = AM_INVALID_ARGUMENT;
    }
    return status;
}

bool Log::setLogFile(const std::string& name)
throw () {
    return pSetLogFile(name);
}

bool Log::pSetLogFile(const std::string& name) throw () {
    bool okay = false;
    if (name.size() > 0) {
#ifndef _MSC_VER
        if (logFile == NULL || logFile == stderr) {
            logFile = std::fopen(name.c_str(), "a+");
        }
        if (logFile != NULL) {
            okay = true;
        }
#else
        if (logFile == INVALID_HANDLE_VALUE) {
            logFile = CreateFileA(name.c_str(), GENERIC_READ | GENERIC_WRITE, FILE_SHARE_READ | FILE_SHARE_WRITE | FILE_SHARE_DELETE,
                    NULL, OPEN_ALWAYS, FILE_ATTRIBUTE_NORMAL, NULL);
        }
        if (logFile != INVALID_HANDLE_VALUE) {
            okay = true;
        }
#endif
    }
    return okay;
}

bool Log::setAuditLogFile(const std::string& name) throw () {
    bool okay = false;
    if (name.size() > 0) {
#ifndef _MSC_VER
        if (auditLogFile == NULL || auditLogFile == stderr) {
            auditLogFile = std::fopen(name.c_str(), "a+");
        }
        if (auditLogFile != NULL) {
            okay = true;
        }
#else
        if (auditLogFile == INVALID_HANDLE_VALUE) {
            auditLogFile = CreateFileA(name.c_str(), GENERIC_READ | GENERIC_WRITE, FILE_SHARE_READ | FILE_SHARE_WRITE | FILE_SHARE_DELETE,
                    NULL, OPEN_ALWAYS, FILE_ATTRIBUTE_NORMAL, NULL);
        }
        if (auditLogFile != INVALID_HANDLE_VALUE) {
            okay = true;
        }
#endif
    }
    return okay;
}

Log::ModuleId Log::addModule(const std::string& name)
throw () {
    Log::ModuleId module = 0;
    am_status_t status = AM_SUCCESS;
    if (!initialized &&
            (status = initialize()) != AM_SUCCESS) {
        Log::log(Log::ALL_MODULES, Log::LOG_ERROR,
                "Log::addModule(): Error initializing log.");
    } else {
        ScopeLock myLock(*lockPtr);
        module = pAddModule(name);
    }
    return module;
}

// same as addModule but with no locking

Log::ModuleId Log::pAddModule(const std::string& name)
throw () {
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
    } catch (...) {
        log(ALL_MODULES, LOG_ERROR,
                "Could not add module %s. Unknown exception caught.", name.c_str());
    }
    return module;
}

Log::Level Log::setModuleLevel(ModuleId module, Level level)
throw () {
    Log::Level oldLevel = Log::LOG_NONE;
    am_status_t status = AM_SUCCESS;

    if (!initialized &&
            (status = initialize()) != AM_SUCCESS) {
        Log::log(Log::ALL_MODULES, Log::LOG_ERROR,
                "Log::setModuleLevel(): Cannot set module level. "
                "Log initialization failed with %s",
                am_status_to_string(status));
        oldLevel = Log::LOG_NONE;
    } else {
        ScopeLock myLock(*lockPtr);
        oldLevel = pSetModuleLevel(module, level);
    }
    return oldLevel;
}

// same as setModuleLevel but with no locking.

Log::Level Log::pSetModuleLevel(ModuleId module, Level level)
throw () {
    Level oldLevel = LOG_NONE;

    if (ALL_MODULES == module) {
        oldLevel = (*moduleList)[ALL_MODULES].level;
        for (unsigned int i = 0; i < moduleList->size(); i++) {
            /* if the level is to turn of everything, then turn off
             * remote logging too. Otherwise, don't change the remote
             * logging settings. */
            if (i != remoteModule || level == LOG_NONE)
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
throw () {
    am_status_t status = AM_SUCCESS;
    ScopeLock mylock(*lockPtr);
    status = pSetLevelsFromString(logLevels);
    return status;
}

am_status_t Log::pSetLevelsFromString(const std::string& logLevels)
throw () {
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
                oldLevel = pSetModuleLevel(module, static_cast<Level> (level));
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
throw () {
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
throw () {
    if (!isLevelEnabled(module, level))
        return;

    // Print the exception information
    log(module, level, "Exception encountered: %s.", ex.what());
    return;
}

// this should not throw exception since it is called often in a 
// catch block to log an error resulting from an exception.

void Log::log(ModuleId module, Level level, const InternalException &ex)
throw () {
    if (!isLevelEnabled(module, level))
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
throw () {
    std::va_list args;

    va_start(args, format);
    vlog(module, level, format, args);
    va_end(args);
}

// this should not throw exception since it is called often in a 
// catch block to log an error resulting from an exception.

void Log::vlog(ModuleId module, Level level, const char *format,
        std::va_list args) throw () {
    if (initialized) {
        if (module >= moduleList->size()) {
            module = ALL_MODULES;
        }

        char *logMsg = NULL;
        Utils::am_vasprintf(&logMsg, format, args);
        // call user defined logger if any.
        if (loggerFunc != NULL) {
            loggerFunc((*moduleList)[module].name.c_str(),
                    static_cast<am_log_level_t> (static_cast<int> (level)),
                    logMsg);
        }

        // do default log.
        if ((*moduleList)[module].level >= level) {

            // format: 
            // year-month-day hour:min:sec.usec level pid:thread module: msg
            // get level string		
            std::size_t levelLabelIndex = getLevelString(level);
            char levelStr[50];
            int llen;
            if (levelLabelIndex < numLabels) {
                llen = snprintf(levelStr, sizeof (levelStr),
                        "%s", levelLabels[levelLabelIndex]);
            } else {
                llen = snprintf(levelStr, sizeof (levelStr), "%d", level);
            }

            if (llen > 0) {
                // get time.
                char hdr[100];
                int len;

#ifdef _MSC_VER

                SYSTEMTIME lt;
                GetLocalTime(&lt);

                /* format header and message */
                len = _snprintf(hdr, sizeof (hdr), "%d-%02d-%02d %02d:%02d:%02d.%03d"
                        " %8s %u:%lu %s: %%s\r\n", lt.wYear, lt.wMonth, lt.wDay,
                        lt.wHour, lt.wMinute, lt.wSecond, lt.wMilliseconds,
                        levelStr,
                        getpid(), GetCurrentThreadId(),
                        (*moduleList)[module].name.c_str());

#else

                struct tm now;
                struct timespec ts;
                unsigned short msec = 0;
                clock_gettime(CLOCK_REALTIME, &ts);
                msec = ts.tv_nsec / 1000000;
                localtime_r(&ts.tv_sec, &now);

                /* format header and message */
                len = snprintf(hdr, sizeof (hdr),
                        "%d-%02d-%02d %02d:%02d:%02d.%03d"
                        " %8s %u:%lu %s: %%s\r\n",
                        now.tm_year + 1900, now.tm_mon + 1, now.tm_mday,
                        now.tm_hour, now.tm_min, now.tm_sec,
                        msec, levelStr,
                        getpid(), (unsigned long) pthread_self(),
                        (*moduleList)[module].name.c_str());
                
#endif

                if (len > 0) {
                    writeLog(hdr, logMsg);
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
                    if (status != AM_SUCCESS) {
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
        if (logMsg) free(logMsg);
    }
    return;
}

/*
 * Log url access audit message to local audit log file.
 *
 */
void Log::doLocalAuditLog(ModuleId module, Level level, const char* auditLogMsg,
        bool localAuditLogRotate, long localAuditFileSize) throw () {
    if (initialized) {
        char hdr[100];
        int len;

#ifdef _MSC_VER

        SYSTEMTIME lt;
        GetLocalTime(&lt);

        /* format header and message */
        len = _snprintf(hdr, sizeof (hdr), "%d-%02d-%02d %02d:%02d:%02d.%03d"
                " %8s %u:%lu %s: %%s\r\n", lt.wYear, lt.wMonth, lt.wDay,
                lt.wHour, lt.wMinute, lt.wSecond, lt.wMilliseconds,
                "Info",
                getpid(), GetCurrentThreadId(),
                "LocalAuditLog");

#else

        struct tm now;
        struct timespec ts;
        unsigned short msec = 0;
        clock_gettime(CLOCK_REALTIME, &ts);
        msec = ts.tv_nsec / 1000000;
        localtime_r(&ts.tv_sec, &now);

        /* format header and message */
        len = snprintf(hdr, sizeof (hdr),
                "%d-%02d-%02d %02d:%02d:%02d.%03d"
                " %8s %u:%lu %s: %%s\r\n",
                now.tm_year + 1900, now.tm_mon + 1, now.tm_mday,
                now.tm_hour, now.tm_min, now.tm_sec,
                msec, "Info",
                getpid(), (unsigned long) pthread_self(),
                "LocalAuditLog");

#endif
        if (len > 0) {
            writeAuditLog(hdr, auditLogMsg);
        }
    }
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
        Utils::am_vasprintf(&logMsg, format, args);
        va_end(args);
        if (logMsg != NULL) {
            logMessage = logMsg;
            if (logMsg[0] == '\0') {
                Log::log(Log::ALL_MODULES, Log::LOG_WARNING, "Log Record Message is empty");
                free(logMsg);
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
            free(logMsg);
        }
    }
    return status;
}

am_status_t Log::rlog(const std::string& logName,
        const LogRecord& record,
        const std::string& loggedByTokenID)
throw () {
    am_status_t status = AM_FAILURE;
    bool cookieEncoded = true;

    if (rmtLogSvc != NULL && remoteInitialized) {
        try {
            std::string loggedByID = loggedByTokenID;
            cookieEncoded = loggedByID.find('%') != std::string::npos;
            if (cookieEncoded) {
                loggedByID = Http::decode(loggedByTokenID);
            }
            status = rmtLogSvc->sendLog(logName,
                    record,
                    loggedByID);
        } catch (std::exception& exs) {
            Log::log(Log::ALL_MODULES, Log::LOG_ERROR,
                    "Log::rlog(): exception encountered %s.", exs.what());
            status = AM_INVALID_ARGUMENT;
        } catch (...) {
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
        const char *logMsg) {
    am_status_t status = AM_SUCCESS;
    std::string logMessage;
    bool cookieEncoded = false;

    if (rmtLogSvc == NULL || !remoteInitialized) {
        status = AM_SERVICE_NOT_INITIALIZED;
    } else {
        if (logMsg != NULL) {
            logMessage = logMsg;
            try {
                LogRecord logRecord(
                        static_cast<LogRecord::Level> (remote_log_level),
                        logMessage);
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
        ...) {
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
    if (&logMsg[0] != NULL) {
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
            } catch (...) {
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
throw () {
    am_status_t status = AM_FAILURE;

    if (rmtLogSvc != NULL && remoteInitialized) {

        status = rmtLogSvc->flushBuffer();
        if (status != AM_SUCCESS) {
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
Log::setLogger(am_log_logger_func_t logger_func) {
    am_log_logger_func_t oldLogger = NULL;
    if (initialized) {
        oldLogger = loggerFunc;
        loggerFunc = logger_func;
    }
    return oldLogger;
}

am_status_t Log::setDebugFileSize(const long debugFileSize) {
    am_status_t status = AM_SUCCESS;
    ScopeLock mylock(*lockPtr);
    maxLogFileSize = debugFileSize;
    if (maxLogFileSize < DEBUG_FILE_MIN_SIZE) {
        maxLogFileSize = DEBUG_FILE_MIN_SIZE;
    }
    return status;
}

am_status_t Log::setDebugFileRotate(bool debugFileRotate) {
    am_status_t status = AM_SUCCESS;
    ScopeLock mylock(*lockPtr);
    logRotation = debugFileRotate;
    return status;
}

#ifndef _MSC_VER

void Log::writeLog(const char *hdr, const char *logMsg) {
    struct flock fl;
    struct stat st;
    memset(&fl, 0, sizeof (fl));
    fl.l_whence = SEEK_SET;
    fl.l_start = 0L;
    fl.l_len = 0L;
    if (hdr == NULL) return;
    if (logFile != NULL && logFile != stderr) {
        if (logRotation) {
            /* check log file/size and try to rotate it only when size > maxLogFileSize */
            if (stat(logFileName.c_str(), &st) == 0 && (st.st_size + 1024) > maxLogFileSize) {
                sem_wait(logRtLock);
                struct flock rl;
                memset(&rl, 0, sizeof (rl));
                rl.l_whence = SEEK_SET;
                rl.l_start = 0L;
                rl.l_len = 0L;
                rl.l_type = F_WRLCK;
                /* re-check if someone has changed file right before we have entered here */
                if (stat(logFileName.c_str(), &st) != 0 || st.st_ino != logInode) {
                    logFile = std::freopen(logFileName.c_str(), "a+", logFile);
                    logInode = st.st_ino;
                }
                if (logFile != NULL) {
                    fcntl(fileno(logFile), F_GETLK, &rl);
                    /* rotate only when its not locked by some other writer */
                    if (rl.l_type == F_UNLCK) {
                        unsigned int idx = 1;
                        char tmp[4096];
                        do {
                            memset(&tmp[0], 0, sizeof (tmp));
                            snprintf(tmp, sizeof (tmp), "%s.%d", logFileName.c_str(), idx);
                            idx++;
                        } while (access(tmp, F_OK) == 0);
                        if (rename(logFileName.c_str(), tmp) != 0) {
                            fprintf(stderr, "Could not rotate log file %s (error: %d)\n", logFileName.c_str(), errno);
                        }
                    }
                } else {
                    fprintf(stderr, "Could not reopen log file %s (error: %d)\n",
                            logFileName.c_str(), errno);
                }
                sem_post(logRtLock);
            }
        }
        /* check if file is rotated, reopen if so */
        if (stat(logFileName.c_str(), &st) != 0 || st.st_ino != logInode) {
            logFile = std::freopen(logFileName.c_str(), "a+", logFile);
            logInode = st.st_ino;
        }
        fl.l_type = F_WRLCK;
        if (logFile != NULL) {
            if (fcntl(fileno(logFile), F_SETLKW, &fl) != -1) {
                std::fprintf(logFile, hdr, logMsg == NULL ? "(null)" : logMsg);
                std::fflush(logFile);
                fl.l_type = F_UNLCK;
                fcntl(fileno(logFile), F_SETLKW, &fl);
            }
        } else {
            fprintf(stderr, "Could not reopen log file %s (error: %d), redirecting output to stderr\n", logFileName.c_str(), errno);
            std::fprintf(stderr, hdr, logMsg == NULL ? "(null)" : logMsg);
        }
    } else {
        std::fprintf(stderr, hdr, logMsg == NULL ? "(null)" : logMsg);
    }
}

void Log::writeAuditLog(const char *hdr, const char *logMsg) {
    struct flock fl;
    struct stat st;
    memset(&fl, 0, sizeof (fl));
    fl.l_whence = SEEK_SET;
    fl.l_start = 0L;
    fl.l_len = 0L;
    if (hdr == NULL) return;
    if (auditLogFile != NULL && auditLogFile != stderr) {
        if (auditLogRotation) {
            /* check log file/size and try to rotate it only when size > maxAuditLogFileSize */
            if (stat(auditLogFileName.c_str(), &st) == 0 && (st.st_size + 1024) > maxAuditLogFileSize) {
                sem_wait(alogRtLock);
                struct flock rl;
                memset(&rl, 0, sizeof (rl));
                rl.l_whence = SEEK_SET;
                rl.l_start = 0L;
                rl.l_len = 0L;
                rl.l_type = F_WRLCK;
                /* re-check if someone has changed file right before we have entered here */
                if (stat(auditLogFileName.c_str(), &st) != 0 || st.st_ino != alogInode) {
                    auditLogFile = std::freopen(auditLogFileName.c_str(), "a+", auditLogFile);
                    alogInode = st.st_ino;
                }
                if (auditLogFile != NULL) {
                    fcntl(fileno(auditLogFile), F_GETLK, &rl);
                    /* rotate only when its not locked by some other writer */
                    if (rl.l_type == F_UNLCK) {
                        unsigned int idx = 1;
                        char tmp[4096];
                        do {
                            memset(&tmp[0], 0, sizeof (tmp));
                            snprintf(tmp, sizeof (tmp), "%s.%d", auditLogFileName.c_str(), idx);
                            idx++;
                        } while (access(tmp, F_OK) == 0);
                        if (rename(auditLogFileName.c_str(), tmp) != 0) {
                            fprintf(stderr, "Could not rotate log file %s (error: %d)\n", auditLogFileName.c_str(), errno);
                        }
                    }
                } else {
                    fprintf(stderr, "Could not reopen log file %s (error: %d)\n",
                            auditLogFileName.c_str(), errno);
                }
                sem_post(alogRtLock);
            }
        }
        /* check if file is rotated, reopen if so */
        if (stat(auditLogFileName.c_str(), &st) != 0 || st.st_ino != alogInode) {
            auditLogFile = std::freopen(auditLogFileName.c_str(), "a+", auditLogFile);
            alogInode = st.st_ino;
        }
        fl.l_type = F_WRLCK;
        if (auditLogFile != NULL) {
            if (fcntl(fileno(auditLogFile), F_SETLKW, &fl) != -1) {
                std::fprintf(auditLogFile, hdr, logMsg == NULL ? "(null)" : logMsg);
                std::fflush(auditLogFile);
                fl.l_type = F_UNLCK;
                fcntl(fileno(auditLogFile), F_SETLKW, &fl);
            }
        } else {
            fprintf(stderr, "Could not reopen log file %s (error: %d), redirecting output to stderr\n", auditLogFileName.c_str(), errno);
            std::fprintf(stderr, hdr, logMsg == NULL ? "(null)" : logMsg);
        }
    } else {
        std::fprintf(stderr, hdr, logMsg == NULL ? "(null)" : logMsg);
    }
}

#else

void Log::writeLog(const char *hdr, const char *logMsg) {
    int msg_size = 0;
    DWORD written, pos;
    if (hdr == NULL) return;
    if (logFile == INVALID_HANDLE_VALUE) {
        logFile = CreateFileA(logFileName.c_str(), GENERIC_READ | GENERIC_WRITE, FILE_SHARE_READ | FILE_SHARE_WRITE | FILE_SHARE_DELETE,
                NULL, OPEN_ALWAYS, FILE_ATTRIBUTE_NORMAL, NULL);
    }
    if (logFile != INVALID_HANDLE_VALUE) {
        if (logRotation) {
            if (WaitForSingleObject(logRtLock, INFINITE) == WAIT_OBJECT_0) {
                rotate_log(logFile, logFileName.c_str(), maxLogFileSize);
                ReleaseMutex(logRtLock);
            }
        }
        if ((pos = SetFilePointer(logFile, 0, NULL, FILE_END)) != INVALID_SET_FILE_POINTER) {
            char workbuf[TEMP_SIZE];
            msg_size = _snprintf(workbuf, sizeof (workbuf), hdr, logMsg == NULL ? "(null)" : logMsg);
            if (msg_size < 0) {
                msg_size = TEMP_SIZE - 1;
            }
            if (msg_size > 0 && LockFile(logFile, pos, 0, msg_size, 0)) {
                workbuf[msg_size] = 0;
                if (!WriteFile(logFile, (LPVOID) workbuf, msg_size, &written, NULL)) {
                    debug("%s file write failed, error: %d", logFileName.c_str(), GetLastError());
                }
                FlushFileBuffers(logFile);
                UnlockFile(logFile, pos, 0, msg_size, 0);
            }
        } else {
            debug("%s set file pointer failed, error: %d", logFileName.c_str(), GetLastError());
        }
    } else {
        debug("%s file open failed, error: %d", logFileName.c_str(), GetLastError());
    }
}

void Log::writeAuditLog(const char *hdr, const char *logMsg) {
    int msg_size = 0;
    DWORD written, pos;
    if (hdr == NULL) return;
    if (auditLogFile == INVALID_HANDLE_VALUE) {
        auditLogFile = CreateFileA(auditLogFileName.c_str(), GENERIC_READ | GENERIC_WRITE, FILE_SHARE_READ | FILE_SHARE_WRITE | FILE_SHARE_DELETE,
                NULL, OPEN_ALWAYS, FILE_ATTRIBUTE_NORMAL, NULL);
    }
    if (auditLogFile != INVALID_HANDLE_VALUE) {
        if (auditLogRotation) {
            if (WaitForSingleObject(alogRtLock, INFINITE) == WAIT_OBJECT_0) {
                rotate_log(auditLogFile, auditLogFileName.c_str(), maxAuditLogFileSize);
                ReleaseMutex(alogRtLock);
            }
        }
        if ((pos = SetFilePointer(auditLogFile, 0, NULL, FILE_END)) != INVALID_SET_FILE_POINTER) {
            char workbuf[TEMP_SIZE];
            msg_size = _snprintf(workbuf, sizeof (workbuf), hdr, logMsg == NULL ? "(null)" : logMsg);
            if (msg_size < 0) {
                msg_size = TEMP_SIZE - 1;
            }
            if (msg_size > 0 && LockFile(auditLogFile, pos, 0, msg_size, 0)) {
                workbuf[msg_size] = 0;
                if (!WriteFile(auditLogFile, (LPVOID) workbuf, msg_size, &written, NULL)) {
                    debug("%s file write failed, error: %d", auditLogFileName.c_str(), GetLastError());
                }
                FlushFileBuffers(auditLogFile);
                UnlockFile(auditLogFile, pos, 0, msg_size, 0);
            }
        } else {
            debug("%s set file pointer failed, error: %d", auditLogFileName.c_str(), GetLastError());
        }
    } else {
        debug("%s file open failed, error: %d", auditLogFileName.c_str(), GetLastError());
    }
}

#endif
