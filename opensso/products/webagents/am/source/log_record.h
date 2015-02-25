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
 * $Id: log_record.h,v 1.4 2008/08/04 19:22:12 huacui Exp $
 *
 */ 
/*
 * Portions Copyrighted 2013 ForgeRock Inc
 */

#ifndef LOG_RECORD_H
#define LOG_RECORD_H

#include <string>
#include <iostream>
#include <map>
#include "internal_macros.h"
#include "properties.h"
#include "sso_token.h"

BEGIN_PRIVATE_NAMESPACE

class LogRecord {
public:
    enum Level {
        /* Log Level as defined by JDK 1.4 */
        L_SEVERE = 1000,
        L_WARNING = 900,
        L_INFORMATION = 800,
        L_CONFIG = 700,
        L_FINE = 500,
        L_FINER = 400,
        L_FINEST = 300,

	/* Log Levels defined by OpenAM
	 * keeping names consistent with IS LogLevel names */
	LL_SECURITY = 950,
	LL_CATASTROPHE = 850,
	LL_MISCONF = 750,
	LL_FAILURE = 650,
	LL_WARN = 550,
	LL_INFO = 450,
	LL_DEBUG = 350,
	LL_ALL = 250
    };

    LogRecord(const Level& level, const std::string& msg)
	      throw():logLevel(level), mesg(msg), logInfoProps() {}

    /* Update log record with user's sso token information */
    void populateTokenDetails(const std::string& loginIDTokenID) {
        logInfoProps.set("LoginIDSid", loginIDTokenID);

    }

    /* set log level */
    void setLogLevel(const Level& level) throw() {
        logLevel = level;
    }
    /* set log message */
    void setLogMessage(const std::string& message) throw() {
        mesg = message;
    }

    /* Update log record with additional information */
    void addLogInfo(const std::string& key, const std::string& value) {
        logInfoProps.set(key, value);
    }

    /* Update log record with additional information.
     * Set all log info values as properties map
     */
    void setLogInfoProps(const Properties& logInfo) throw() {
        logInfoProps = logInfo;
    }

    /* return logLevel as string */
    Level getLogLevel() const {
	return logLevel;
    }

    /* return log message */
    const std::string& getLogMessage() const { return mesg;}

    /* return loginfoMap */
    const Properties& getLogInfo() const { return logInfoProps;}

private:
    Level logLevel;
    std::string mesg;
    Properties logInfoProps;
};

END_PRIVATE_NAMESPACE

#endif	/* not LOG_RECORD_H */
