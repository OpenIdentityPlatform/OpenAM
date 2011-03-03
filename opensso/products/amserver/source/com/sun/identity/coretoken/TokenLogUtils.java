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
 * $Id: TokenLogUtils.java,v 1.1 2009/11/19 00:07:40 qcheng Exp $
 *
 */


package com.sun.identity.coretoken;

import com.iplanet.sso.SSOToken;
import com.sun.identity.log.LogRecord;
import java.util.logging.Level;
import com.sun.identity.log.Logger;
import com.sun.identity.log.messageid.LogMessageProvider;
import com.sun.identity.log.messageid.MessageProviderFactory;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.configuration.SystemPropertiesManager;
import java.io.IOException;
import java.security.AccessController;

/**
 * The <code>TokenLogUtils</code> class defines methods which are used by
 * Core Token Service to write access and erro logs.
 */
public class TokenLogUtils {

    /* Logging id Constants */
    public static final String TOKEN_CREATE_SUCCESS = "TOKEN_CREATE_SUCCESS";
    public static final String TOKEN_READ_SUCCESS = "TOKEN_READ_SUCCESS";
    public static final String TOKEN_UPDATE_SUCCESS = "TOKEN_UPDATE_SUCCESS";
    public static final String TOKEN_SEARCH_SUCCESS = "TOKEN_SEARCH_SUCCESS";
    public static final String TOKEN_DELETE_SUCCESS = "TOKEN_DELETE_SUCCESS";
    public static final String EXPIRED_TOKEN_DELETE_SUCCESS =
            "EXPIRED_TOKEN_DELETE_SUCCESS";
    public static final String UNABLE_TO_CREATE_TOKEN =
            "UNABLE_TO_CREATE_TOKEN";
    public static final String UNABLE_TO_READ_TOKEN = "UNABLE_TO_READ_TOKEN";
    public static final String UNABLE_TO_UPDATE_TOKEN =
            "UNABLE_TO_UPDATE_TOKEN";
    public static final String UNABLE_TO_SEARCH_TOKEN =
            "UNABLE_TO_SEARCH_TOKEN";
    public static final String UNABLE_TO_DELETE_TOKEN =
            "UNABLE_TO_DELETE_TOKEN";

    private static final String CORETOKEN_LOG = "CoreToken";
    private static final String LOG_MSG_XML = "CoreToken";
    // NameID of a token is Base64 encoded SHA-1 of token.id, this is a
    // individual logging field used to coordinate log entries for a token.id
    // since we can't reveal the actual token.id in log
    public static final String TOKEN_NAME_ID = "NameID";
    private static Logger accessLogger = null;
    private static Logger errorLogger = null;
    private static boolean logActive = false;

    static {
        String status = SystemPropertiesManager.get(
            com.sun.identity.shared.Constants.AM_LOGSTATUS);
        logActive = (status != null) && status.equalsIgnoreCase("ACTIVE");
        accessLogger = (com.sun.identity.log.Logger)
                Logger.getLogger(CORETOKEN_LOG + ".access");
        errorLogger = (com.sun.identity.log.Logger)
                Logger.getLogger(CORETOKEN_LOG + ".error");
    }

    /**
     * Logs message to core token access logs.
     *
     * @param level the log level , these are based on those
     *          defined in java.util.logging.Level, the values for
     *          level can be any one of the following : <br>
     *          <ul>
     *          <li>SEVERE (highest value) <br>
     *          <li>WARNING <br>
     *          <li>INFO <br>
     *          <li>CONFIG <br>
     *          <li>FINE <br>
     *          <li>FINER <br>
     *          <li>FINEST (lowest value) <br>
     *          </ul>
     * @param msgid the message or a message identifier.
     * @param data string array of dynamic data to be replaced in the message.
     * @param session the User's session object
     * @param nameId value for NameID logging field
     */
    public static void access(
        Level level, String msgid, String data[],
        SSOToken session, String nameId) {
        if (logActive) {
            try {
                if (isAccessLoggable(level)) {
                    SSOToken adminToken = (SSOToken) AccessController
                        .doPrivileged(AdminTokenAction.getInstance());
                    LogMessageProvider msgProvider =
                        MessageProviderFactory.getProvider(LOG_MSG_XML);
                    LogRecord logRec = msgProvider.createLogRecord(msgid,
                            data, session);
                    logRec.addLogInfo(TokenLogUtils.TOKEN_NAME_ID, nameId);
                    if (logRec != null) {
                        accessLogger.log(logRec, adminToken);
                    }
                }
            } catch (IOException le) {
                CoreTokenUtils.debug.error(
                    "TokenLogUtils.error:Couldn't write error log:",le);
            }
        }
    }

    /** 
     * Logs error messages to core token error log.
     *
     * @param level the log level , these are based on those
     *          defined in java.util.logging.Level, the values for
     *          level can be any one of the following : <br>
     *          <ul>
     *          <li>SEVERE (highest value) <br>
     *          <li>WARNING <br>
     *          <li>INFO <br>
     *          <li>CONFIG <br>
     *          <li>FINE <br>
     *          <li>FINER <br>
     *          <li>FINEST (lowest value) <br>
     *          </ul>
     * @param msgid the message or a message identifier.
     * @param data string array of dynamic data to be replaced in the message.
     * @param session the User's Session object.
     * @param nameId value for NameID logging field
      */
    public static void error(
        Level level, String msgid, String data[],
        SSOToken session, String nameId) {
        if (logActive) {
            try {
                if (isErrorLoggable(level)) {
                    SSOToken adminToken = (SSOToken) AccessController
                        .doPrivileged(AdminTokenAction.getInstance());
                    LogMessageProvider msgProvider =
                        MessageProviderFactory.getProvider(LOG_MSG_XML);
                    LogRecord logRec = msgProvider.createLogRecord(msgid,
                            data, session);
                    logRec.addLogInfo(TokenLogUtils.TOKEN_NAME_ID, nameId);
                    if (logRec != null) {
                        errorLogger.log(logRec, adminToken);
                    }
                }
            } catch (IOException le) {
                CoreTokenUtils.debug.error(
                    "TokenLogUtils.error:Couldn't write error log:",le);
            }
        } 
    }

    /**
     * Checks if an access message of the given level would actually be logged
     * by this logger. This check is based on the Logger's effective level.
     *
     * @param level a message logging level defined in java.util.logging.Level.
     * @return true if the given message level is currently being logged.
     */
    public static boolean isAccessLoggable(Level level) {
        SSOToken authSSOToken = (SSOToken) AccessController.doPrivileged(
            AdminTokenAction.getInstance());
        if ((authSSOToken == null) || !logActive) {
            return false;
        }
        return accessLogger.isLoggable(level);
    }

    /**
     * Checks if an error message of the given level would actually be logged
     * by this logger. This check is based on the Logger's effective level.
     *
     * @param level a message logging level defined in java.util.logging.Level.
     * @return true if the given message level is currently being logged.
     */
    public static boolean isErrorLoggable(Level level) {
        SSOToken authSSOToken = (SSOToken) AccessController.doPrivileged(
            AdminTokenAction.getInstance());
        if ((authSSOToken == null) || !logActive) {
            return false;
        }
        return errorLogger.isLoggable(level);

    }

}
