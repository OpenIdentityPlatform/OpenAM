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
 * $Id: LogUtils.java,v 1.2 2008/06/25 05:47:34 qcheng Exp $
 *
 */

package com.sun.identity.saml.common;

import com.sun.identity.plugin.log.Logger;
import com.sun.identity.plugin.log.LogException;
import com.sun.identity.plugin.log.LogManager;
import java.util.logging.Level;

/**
 * The <code>LogUtils</code> class defines methods which are used by
 * SAML compoment to write logs.
 */
public class LogUtils {
    private static final String SAML_LOG = "SAML";
    private static Logger logger = null;

    /**
     * Constants for log message ASSERTION_CREATED.
     */
    public static final String ASSERTION_CREATED = "ASSERTION_CREATED";

    /**
     * Constants for log message ASSERTION_ARTIFACT_CREATED.
     */
    public static final String ASSERTION_ARTIFACT_CREATED = 
        "ASSERTION_ARTIFACT_CREATED";

    /**
     * Constants for log message ASSERTION_ARTIFACT_REMOVED.
     */
    public static final String ASSERTION_ARTIFACT_REMOVED = 
        "ASSERTION_ARTIFACT_REMOVED";

    /**
     * Constants for log message ASSERTION_REMOVED.
     */
    public static final String ASSERTION_REMOVED = 
        "ASSERTION_REMOVED";

    /**
     * Constants for log message ASSERTION_ARTIFACT_VERIFIED.
     */
    public static final String ASSERTION_ARTIFACT_VERIFIED = 
        "ASSERTION_ARTIFACT_VERIFIED";

    /**
     * Constants for log message AUTH_PROTOCOL_MISMATCH.
     */
    public static final String AUTH_PROTOCOL_MISMATCH = 
        "AUTH_PROTOCOL_MISMATCH";

    /**
     * Constants for log message INVALID_AUTH_TYPE.
     */
    public static final String INVALID_AUTH_TYPE = 
        "INVALID_AUTH_TYPE";

    /**
     * Constants for log message SOAP_RECEIVER_URL.
     */
    public static final String SOAP_RECEIVER_URL = 
        "SOAP_RECEIVER_URL";

    /**
     * Constants for log message NO_ASSERTION_IN_RESPONSE.
     */
    public static final String NO_ASSERTION_IN_RESPONSE = 
        "NO_ASSERTION_IN_RESPONSE";

    /**
     * Constants for log message MISMATCHED_ASSERTION_AND_ARTIFACT.
     */
    public static final String MISMATCHED_ASSERTION_AND_ARTIFACT = 
        "MISMATCHED_ASSERTION_AND_ARTIFACT";

    /**
     * Constants for log message ARTIFACT_TO_SEND.
     */
    public static final String ARTIFACT_TO_SEND = "ARTIFACT_TO_SEND";

    /**
     * Constants for log message WRONG_SOAP_URL.
     */
    public static final String WRONG_SOAP_URL = "WRONG_SOAP_URL";

    /**
     * Constants for log message SAML_ARTIFACT_QUERY.
     */
    public static final String SAML_ARTIFACT_QUERY = "SAML_ARTIFACT_QUERY";

    /**
     * Constants for log message NO_REPLY_FROM_SOAP_RECEIVER.
     */
    public static final String NO_REPLY_FROM_SOAP_RECEIVER = 
        "NO_REPLY_FROM_SOAP_RECEIVER";

    /**
     * Constants for log message REPLIED_SOAP_MESSAGE.
     */
    public static final String REPLIED_SOAP_MESSAGE = 
        "REPLIED_SOAP_MESSAGE";

    /**
     * Constants for log message NULL_SAML_RESPONSE.
     */
    public static final String NULL_SAML_RESPONSE = "NULL_SAML_RESPONSE"; 

    /**
     * Constants for log message INVALID_RESPONSE_SIGNATURE.
     */
    public static final String INVALID_RESPONSE_SIGNATURE = 
        "INVALID_RESPONSE_SIGNATURE"; 

    /**
     * Constants for log message ERROR_RESPONSE_STATUS.
     */
    public static final String ERROR_RESPONSE_STATUS = 
        "ERROR_RESPONSE_STATUS"; 

    /**
     * Constants for log message NULL_PARAMETER.
     */
    public static final String NULL_PARAMETER = "NULL_PARAMETER"; 

    /**
     * Constants for log message MISSING_TARGET.
     */
    public static final String MISSING_TARGET = "MISSING_TARGET"; 

    /**
     * Constants for log message REDIRECT_TO_URL.
     */
    public static final String REDIRECT_TO_URL = "REDIRECT_TO_URL"; 

    /**
     * Constants for log message TARGET_FORBIDDEN.
     */
    public static final String TARGET_FORBIDDEN = "TARGET_FORBIDDEN"; 

    /**
     * Constants for log message FAILED_TO_CREATE_SSO_TOKEN.
     */
    public static final String FAILED_TO_CREATE_SSO_TOKEN = 
        "FAILED_TO_CREATE_SSO_TOKEN"; 

    /**
     * Constants for log message ACCESS_GRANTED.
     */
    public static final String ACCESS_GRANTED = "ACCESS_GRANTED"; 

    /**
     * Constants for log message MISSING_RESPONSE.
     */
    public static final String MISSING_RESPONSE = "MISSING_RESPONSE"; 

    /**
     * Constants for log message RESPONSE_MESSAGE_ERROR.
     */
    public static final String RESPONSE_MESSAGE_ERROR = 
         "RESPONSE_MESSAGE_ERROR"; 

    /**
     * Constants for log message INVALID_RESPONSE.
     */
    public static final String INVALID_RESPONSE = "INVALID_RESPONSE";

    /**
     * Constants for log message SOAP_MESSAGE_FACTORY_ERROR.
     */
    public static final String SOAP_MESSAGE_FACTORY_ERROR = 
        "SOAP_MESSAGE_FACTORY_ERROR";

    /**
     * Constants for log message UNTRUSTED_SITE.
     */
    public static final String UNTRUSTED_SITE = "UNTRUSTED_SITE";

    /**
     * Constants for log message INVALID_REQUEST.
     */
    public static final String INVALID_REQUEST = "INVALID_REQUEST";

    /**
     * Constants for log message SOAP_REQUEST_MESSAGE.
     */
    public static final String SOAP_REQUEST_MESSAGE = "SOAP_REQUEST_MESSAGE";

    /**
     * Constants for log message BUILD_RESPONSE_ERROR.
     */
    public static final String BUILD_RESPONSE_ERROR = "BUILD_RESPONSE_ERROR";

    /**
     * Constants for log message SENDING_RESPONSE.
     */
    public static final String SENDING_RESPONSE = "SENDING_RESPONSE";

    /**
     * Constants for log message SOAP_FAULT_ERROR.
     */
    public static final String SOAP_FAULT_ERROR = "SOAP_FAULT_ERROR";
    
    static {
        try {
            logger = LogManager.getLogger(SAML_LOG);
        } catch (LogException e) {
            SAMLUtils.debug.message("LogUtils.static: ", e);
        }
    }
    
    /**
     * Logs SAML specific access conditions to the SAML access log
     * (amSAML.access).
     * @param level java.util.logging.Level indicating log level
     * @param msgId message ID for the log entry.
     * @param data string array of access messages to be logged.
     */
    public static void access(Level level, String msgId, String data[]) {
        access(level, msgId, data, null); 
    }

    /** 
     * Logs SAML specific access conditions to the SAML access log
     * (amSAML.access).
     * @param level java.util.logging.Level indicating log level
     * @param msgId message ID for the log entry.
     * @param data string array of access messages to be logged.
     * @param session the user's session object. 
     */
    public static void access(Level level, String msgId, 
        String data[], Object session) {
        try {
            logger.access(level, msgId, data, session);
        } catch (LogException le) {
            SAMLUtils.debug.error("LogUtils.access: " +
                "Error writing to log:", le);
        }
    }

    /**
     * Logs SAML specific errror conditions to the SAML error log
     * (amSAML.error).
     * @param level java.util.logging.Level indicating log level
     * @param msgId message ID for the log entry.
     * @param data string array of error messages to be logged.
     */
    public static void error(Level level, String msgId, String data[]) {
        error(level, msgId, data, null); 
    }

    /** 
     * Logs SAML specific errror conditions to the SAML error log
     * (amSAML.error).
     * @param level java.util.logging.Level indicating log level
     * @param msgId message ID for the log entry.
     * @param data string array of error messages to be logged.
     * @param session the user's session object.
     */
    public static void error(Level level, String msgId,
        String data[], Object session) {
        try {
            logger.error(level, msgId, data, session);
        } catch (LogException le) {
            SAMLUtils.debug.error("LogUtils.error: Error writing to" +
                " log:", le);
        }
    }

    /**
     * Checks if logging is enabled.
     * @return true if logging is enabled.
     */
    public boolean isLogEnabled() {
        if (logger == null) {
            return false;
        } else {
            return logger.isLogEnabled();
        }
    }
    
    /**
     * Checks if an access message of the given level would actually be logged
     * by this logger. This check is based on the Loggers effective level.
     * @param level a message logging level.
     * @return true if the given message level is currently being logged.
     */
    public static boolean isAccessLoggable(Level level) {
        if (logger == null) {
            return false;
        } else {
            return logger.isAccessLoggable(level);
        }
    }

    /**
     * Checks if an error message of the given level would actually be logged
     * by this logger. This check is based on the Loggers effective level.
     * @param level a message logging level.
     * @return true if the given message level is currently being logged.
     */
    public static boolean isErrorLoggable(Level level) {
        if (logger == null) {
            return false;
        } else {
            return logger.isErrorLoggable(level);
        }
    }
}
