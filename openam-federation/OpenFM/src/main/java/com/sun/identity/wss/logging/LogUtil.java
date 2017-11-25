/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: LogUtil.java,v 1.4 2009/06/22 23:28:46 rh221556 Exp $
 *
 */

package com.sun.identity.wss.logging;

import java.util.Map;
import java.util.logging.Level;
import com.sun.identity.plugin.log.LogException;
import com.sun.identity.plugin.log.Logger;
import com.sun.identity.plugin.log.LogManager;
import com.sun.identity.shared.configuration.SystemPropertiesManager;
import com.sun.identity.wss.security.WSSUtils;

/**
 * The <code>LogUtil</code> class defines methods which are used by
 * Web Services Security (STS and WSS) compoment to write logs.
 */
public class LogUtil {

    /* Log Constants for STS */
    public static final String UNSUPPORTED_TOKEN_TYPE="UNSUPPORTED_TOKEN_TYPE";
    public static final String CREATED_SAML11_ASSERTION=
        "CREATED_SAML11_ASSERTION";
    public static final String CREATED_SAML20_ASSERTION=
        "CREATED_SAML20_ASSERTION";
    public static final String ERROR_SIGNING_SAML_ASSERTION=
        "ERROR_SIGNING_SAML_ASSERTION";
    public static final String ERROR_CREATING_SAML11_ASSERTION=
        "ERROR_CREATING_SAML11_ASSERTION";
    public static final String ERROR_CREATING_SAML20_ASSERTION=
        "ERROR_CREATING_SAML20_ASSERTION";
    public static final String IDENTITY_SUBJECT_NAME=
        "IDENTITY_SUBJECT_NAME";
    public static final String ATTR_MAP_FOR_SP="ATTR_MAP_FOR_SP";
    public static final String SUCCESS_RETRIEVING_TOKEN_FROM_STS =
        "SUCCESS_RETRIEVING_TOKEN_FROM_STS";
    public static final String ERROR_RETRIEVING_TOKEN_FROM_STS =
        "ERROR_RETRIEVING_TOKEN_FROM_STS";

    /* Log Constants for WSS */
    public static final String SUCCESS_VALIDATE_REQUEST =
        "SUCCESS_VALIDATE_REQUEST";
    public static final String REQUEST_TO_BE_VALIDATED =
        "REQUEST_TO_BE_VALIDATED";
    public static final String RESPONSE_TO_BE_SECURED ="RESPONSE_TO_BE_SECURED";
    public static final String SUCCESS_SECURE_RESPONSE = 
        "SUCCESS_SECURE_RESPONSE";
    public static final String REQUEST_TO_BE_SECURED = "REQUEST_TO_BE_SECURED";
    public static final String SUCCESS_SECURE_REQUEST ="SUCCESS_SECURE_REQUEST";
    public static final String RESPONSE_TO_BE_VALIDATED =
        "RESPONSE_TO_BE_VALIDATED";
    public static final String SUCCESS_VALIDATE_RESPONSE =
        "SUCCESS_VALIDATE_RESPONSE";
    public static final String AUTHENTICATION_FAILED = "AUTHENTICATION_FAILED";
    public static final String ERROR_PARSING_SOAP_HEADERS = 
        "ERROR_PARSING_SOAP_HEADERS";
    public static final String ERROR_ADDING_SECURITY_HEADER = 
        "ERROR_ADDING_SECURITY_HEADER";
    public static final String SIGNATURE_VALIDATION_FAILED =
        "SIGNATURE_VALIDATION_FAILED";
    public static final String UNABLE_TO_SIGN = "UNABLE_TO_SIGN";
    public static final String UNABLE_TO_ENCRYPT = "UNABLE_TO_ENCRYPT";
    public static final String UNABLE_TO_DECRYPT = "UNABLE_TO_DECRYPT";

    /**
     * The Domain field. The Domain pertaining to the log record's
     * Data field.
     */
    public static final String DOMAIN = "Domain";
    /**
     * The LoginID field. The Login ID pertaining to the log record's
     * Data field.
     */
    public static final String LOGIN_ID = "LoginID";
    /**
     * The IPAddr field. The IP Address pertaining to the log record's
     * Data field.
     */
    public static final String IP_ADDR = "IPAddr";
    /**
     * The ModuleName field. The Module pertaining to the log record's
     * Data field.
     */
    public static final String MODULE_NAME = "ModuleName";
                                          
    private static boolean logStatus;
    private static final String WebServices_LOG = "WebServicesSecurity";
    private static Logger logger = null;

    
    static {
        
        String status = SystemPropertiesManager.get(
            com.sun.identity.shared.Constants.AM_LOGSTATUS);
        logStatus = (status != null) && status.equalsIgnoreCase("ACTIVE");
        
        try {
            logger = LogManager.getLogger(WebServices_LOG);
        } catch (LogException le) {
            WSSUtils.debug.error("LogUtil.static: Error getting logger:", le);
        }
    }
    
    /**
     * Logs message to Web Services access logs.
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
     */
    public static void access(Level level, String msgid, String data[]) {
        access(level, msgid, data, null, null);
    }

    /**
     * Logs message to Web Services access logs.
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
     */
    public static void access(
        Level level, String msgid, String data[], Object session) {
        access(level, msgid, data, session, null);
    }

    /**
     * Logs message to Web Services access logs.
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
     * @param props extra log fields
     */
    public static void access(
        Level level, String msgid, String data[],
        Object session, Map props) {
        if (isLogEnabled()) {
            if (logger != null) {
                try {
                    logger.access(level, msgid, data, session, props);
                } catch (LogException le) {
                    WSSUtils.debug.error(
                        "LogUtil.access: Couldn't write log:", le);
                }
            }
        }    
    }
    
    /**
     * Logs error messages to Web Services error log.
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
     */
     public static void error(Level level, String msgid, String data[]) {
         error(level,msgid,data,null, null);
     }

     /** 
     * Logs error messages to Web Services error log.
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
      */
     public static void error(
         Level level, String msgid, String data[], Object session) {
         error(level, msgid, data, session, null);
     }

     /** 
     * Logs error messages to Web Services error log.
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
     * @param props extra log fields
      */
    public static void error(
        Level level, String msgid, String data[],
        Object session, Map props) {
        if (isLogEnabled()) {
                if (logger != null) {
                    try {
                        logger.error(level, msgid, data, session, props);
                } catch (LogException le) {
                    WSSUtils.debug.error("LogUtil.error:Couldn't write log:",le);
                }
            }    
        }
    }
    /**
     * Checks if the logging is enabled.
     *
     * @return true if logging is enabled.
     */
    public static boolean isLogEnabled() {
        if (logger == null) {
            return false;
        } else {
            return logStatus;
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
