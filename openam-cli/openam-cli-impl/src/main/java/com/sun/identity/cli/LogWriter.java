/**
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
 * $Id: LogWriter.java,v 1.3 2008/06/25 05:42:09 qcheng Exp $
 *
 */

package com.sun.identity.cli;


import com.iplanet.sso.SSOToken;
import com.sun.identity.log.LogRecord;
import com.sun.identity.log.Logger;
import com.sun.identity.log.messageid.LogMessageProvider;
import com.sun.identity.log.messageid.MessageProviderFactory;
import com.sun.identity.security.AdminTokenAction;
import java.security.AccessController;
import java.util.logging.Level;

/**
 * Writes audit log entries.
 */
public class LogWriter {
    private static final String LOG_MSG_XML = "CLI";

    /**
     * Access Log Type.
     */
    public final static int LOG_ACCESS = 0;

    /**
     * Error Log Type.
     */
    public final static int LOG_ERROR = 1;

    private LogWriter() {
    }

    /**
     * Writes to log.
     *
     * @param mgr Command Manager Object.
     * @param type Type of log message.
     * @param level Logging level of the message.
     * @param msgid ID for message.
     * @param msgdata array of log message "data".
     * @param ssoToken Single Sign On Token of the user who committed the
     *        operation.
     * @throws CLIException if log cannot be written.
     */
    public static void log(
        CommandManager mgr,
        int type,
        Level level,
        String msgid,
        String[] msgdata,
        SSOToken ssoToken
    ) throws CLIException {
        if (!mgr.isLogOff()) {
            Logger logger = null;
            String logName = mgr.getLogName();
            switch (type) {
                case LOG_ERROR:
                    logger = (com.sun.identity.log.Logger)
                        Logger.getLogger(logName + ".error");
                    break;
                default:
                    logger = (com.sun.identity.log.Logger)
                        Logger.getLogger(logName + ".access");
            }
            if (logger.isLoggable(level)) {
                try {
                    LogMessageProvider msgProvider =
                        MessageProviderFactory.getProvider(LOG_MSG_XML);
                    SSOToken adminSSOToken = (SSOToken)AccessController.
                        doPrivileged(AdminTokenAction.getInstance());

                    if (ssoToken == null) {
                        ssoToken = adminSSOToken;
                    }

                    LogRecord logRec = msgProvider.createLogRecord(msgid,
                        msgdata, ssoToken);
                    if (logRec != null) {
                        logger.log(logRec, adminSSOToken);
                    }
                } catch (Exception e) {
                    throw new CLIException(e, ExitCodes.CANNOT_WRITE_LOG);
                }
            }
        }
    }
}
