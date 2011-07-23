/* The contents of this file are subject to the terms
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
 * Copyright 2006 Sun Microsystems Inc. All Rights Reserved
 *
 *
 * Abstract:
 *
 * Service interface class for the DSAME "Naming" service.
 *
 */

#ifndef LOG_SERVICE_H
#define LOG_SERVICE_H

#include "base_service.h"
#include "internal_macros.h"
#include "naming_info.h"
#include "sso_token.h"
#include "log_record.h"
#include "mutex.h"

BEGIN_PRIVATE_NAMESPACE

class LogService: public BaseService {
public:
    explicit LogService(const ServiceInfo &, const SSOToken &,
			const Http::CookieList &, const std::string &,
			const Properties &,
                        const std::string &cert_passwd,
                        const std::string &cert_nick_name,
                        bool alwaysTrustServerCert);

    explicit LogService(const ServiceInfo &,
                        const Properties &,
                        const std::string &cert_passwd,
                        const std::string &cert_nick_name,
                        bool alwaysTrustServerCert,
                        unsigned int bufferSize);
    virtual ~LogService();


    am_status_t logMessage(const std::string &message) throw();

    am_status_t logMessage(const ServiceInfo& service,
			      const SSOToken& loggedBySSOToken,
			      const Http::CookieList& cookieList,
                              const std::string& message,
                              const std::string& logname) throw();

    am_status_t sendLog(const std::string& logName,
                        const LogRecord& record,
                        const std::string& loggedByTokenID) throw();

    am_status_t flushBuffer() throw();

private:

    am_status_t addLogDetails(const std::string& logName,
                       const LogRecord& record,
                       const std::string& loggedByTokenID);

    ServiceInfo serviceInfo;
    SSOToken loggedByToken;
    Http::CookieList cookieList;
    std::string remoteLogName;
    char* encodedMessage;

    BodyChunkList remoteBodyChunkList;
    Request *remoteRequest;
    unsigned int bufferSize;
    unsigned int bufferCount;
    bool remoteBodyChunkListInitialized;
    Mutex mLock;

    static const BodyChunk requestPrefixChunk;
    static const BodyChunk logRequestPrefixChunk;
    static const BodyChunk additionalRequestPrefixChunk;
    static const BodyChunk logLogPrefixChunk;
    static const BodyChunk logSidPrefixChunk;
    static const BodyChunk logLogSuffixChunk;
    static const BodyChunk logRecordPrefixChunk;
    static const BodyChunk logRecordTypeChunk;
    static const BodyChunk logRecordSuffixChunk;
    static const BodyChunk logLevelPrefixChunk;
    static const BodyChunk logLevelSuffixChunk;
    static const BodyChunk logRecMsgPrefixChunk;
    static const BodyChunk logRecMsgSuffixChunk;
    static const BodyChunk logInfoMapPrefixChunk;
    static const BodyChunk logInfoMapSuffixChunk;
    static const BodyChunk logInfoKeyPrefixChunk;
    static const BodyChunk logInfoKeySuffixChunk;
    static const BodyChunk logInfoValuePrefixChunk;
    static const BodyChunk logInfoValueSuffixChunk;
    static const BodyChunk requestSuffixChunk;
    static const BodyChunk requestSetSuffixChunk;

};

END_PRIVATE_NAMESPACE

#endif	/* not LOG_SERVICE_H */
