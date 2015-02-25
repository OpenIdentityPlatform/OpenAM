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
 * $Id: log_service.h,v 1.3 2008/06/25 08:14:33 qcheng Exp $
 *
 *
 * Abstract:
 *
 * Service interface class for the DSAME "Naming" service.
 *
 */
/*
 * Portions Copyrighted 2012-2013 ForgeRock Inc
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

class LogService : public BaseService {
public:
    explicit LogService(const ServiceInfo &, const SSOToken &,
            const Http::CookieList &, const std::string &,
            const Properties &);

    explicit LogService(const ServiceInfo &,
            const Properties &,
            unsigned int bufferSize);

    virtual ~LogService();


    am_status_t logMessage(const std::string &message) throw ();

    am_status_t logMessage(const ServiceInfo& service,
            const SSOToken& loggedBySSOToken,
            const Http::CookieList& cookieList,
            const std::string& message,
            const std::string& logname) throw ();

    am_status_t sendLog(const std::string& logName,
            const LogRecord& record,
            const std::string& loggedByTokenID) throw ();

    am_status_t flushBuffer() throw ();

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

};

END_PRIVATE_NAMESPACE

#endif	/* not LOG_SERVICE_H */
