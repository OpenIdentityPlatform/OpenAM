/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: OpenSSOLogger.java,v 1.1 2009/10/22 21:03:33 veiming Exp $
 */

package com.sun.identity.entitlement.opensso;

import com.iplanet.sso.SSOToken;
import com.sun.identity.entitlement.log.ELogRecord;
import com.sun.identity.entitlement.log.LoggerFactory;
import com.sun.identity.log.LogRecord;
import com.sun.identity.log.messageid.LogMessageProvider;
import com.sun.identity.log.messageid.MessageProviderFactory;
import com.sun.identity.security.AdminTokenAction;
import java.io.IOException;
import java.security.AccessController;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.security.auth.Subject;

/**
 *
 * @author dennis
 */
public class OpenSSOLogger {
    public enum LogLevel {ERROR, MESSAGE};
    private static final String LOG_MSG_XML = "Entitlement";
    private static final String LOG_NAME = "entitlement";

    private OpenSSOLogger() {
    }

    public static void log(
        LogLevel type,
        Level level,
        String msgid,
        String[] msgdata,
        Subject logFor
    ) {
        Logger logger = (type.equals(LogLevel.ERROR)) ? LoggerFactory.getLogger(
            LOG_NAME + ".error") : LoggerFactory.getLogger(LOG_NAME + ".access");
        if (logger.isLoggable(level)) {
            try {
                LogMessageProvider msgProvider =
                    MessageProviderFactory.getProvider(LOG_MSG_XML);
                LogRecord logRec = msgProvider.createLogRecord(msgid,
                    msgdata, SubjectUtils.getSSOToken(logFor));
                if (logRec != null) {
                    logger.log(getERecord(logRec));
                }
            } catch (IOException ex) {
                Logger.getLogger(OpenSSOLogger.class.getName()).
                    log(Level.SEVERE, null, ex);
            }
        }
    }

    private static ELogRecord getERecord(LogRecord rec) {
        SSOToken adminSSOToken = (SSOToken) AccessController.doPrivileged(
            AdminTokenAction.getInstance());
        ELogRecord eLogRec = new ELogRecord(rec.getLevel(), rec.getMessage(),
            SubjectUtils.createSubject(adminSSOToken),
            SubjectUtils.createSubject((SSOToken)rec.getLogFor()));
        
        Map map = rec.getLogInfoMap();
        for (Object k : map.keySet()) {
            eLogRec.addLogInfo((String)k, map.get(k));
        }
        
        return eLogRec;
    }
}
