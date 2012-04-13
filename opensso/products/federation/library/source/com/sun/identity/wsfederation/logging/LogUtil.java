/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: LogUtil.java,v 1.6 2008/11/10 22:57:03 veiming Exp $
 *
 */


package com.sun.identity.wsfederation.logging;

import java.util.logging.Level;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.plugin.log.LogException;
import com.sun.identity.plugin.log.Logger;
import com.sun.identity.plugin.log.LogManager;
import com.sun.identity.wsfederation.common.WSFederationUtils;

/**
 * The <code>LogUtil</code> class defines methods which are used by
 * <code>WS-Federation</code> component to write logs.
 */
public abstract class LogUtil {
    private static Debug debug = WSFederationUtils.debug;

    /* Log Constants */
    public static String INVALID_SIGNATURE_ASSERTION = 
        "INVALID_SIGNATURE_ASSERTION";
    public static final String MISSING_CONDITIONS_NOT_ON_OR_AFTER =
        "MISSING_CONDITIONS_NOT_ON_OR_AFTER";
    public static final String MISSING_CONDITIONS_NOT_BEFORE =
        "MISSING_CONDITIONS_NOT_BEFORE";
    public static final String ASSERTION_NOT_YET_VALID =
        "ASSERTION_NOT_YET_VALID";
    public static final String ASSERTION_EXPIRED =
        "ASSERTION_EXPIRED";
    public static final String MISSING_WRESULT =
        "MISSING_WRESULT";
    public static final String MISSING_WCTX =
        "MISSING_WCTX";
    public static final String INVALID_WRESULT =
        "INVALID_WRESULT";
    public static final String CANT_FIND_SP_ACCOUNT_MAPPER =
        "CANT_FIND_SP_ACCOUNT_MAPPER";
    public static final String CANT_CREATE_SP_ACCOUNT_MAPPER =
        "CANT_CREATE_SP_ACCOUNT_MAPPER";
    public static final String CANT_CREATE_SESSION =
        "CANT_CREATE_SESSION";
    public static final String SSO_SUCCESSFUL =
        "SSO_SUCCESSFUL";
    public static final String UNTRUSTED_ISSUER =
        "UNTRUSTED_ISSUER";
    public static final String MISSING_SUBJECT =
        "MISSING_SUBJECT";
    public static final String GOT_FEDERATION = "GOT_FEDERATION";
    public static final String GOT_INVALID_ENTITY_DESCRIPTOR =
        "GOT_INVALID_ENTITY_DESCRIPTOR";
    public static final String CONFIG_ERROR_GET_ENTITY_DESCRIPTOR =
        "CONFIG_ERROR_GET_ENTITY_DESCRIPTOR";
    public static final String SET_ENTITY_DESCRIPTOR = "SET_ENTITY_DESCRIPTOR";
    public static final String CONFIG_ERROR_SET_ENTITY_DESCRIPTOR =
        "CONFIG_ERROR_SET_ENTITY_DESCRIPTOR";
    public static final String SET_INVALID_ENTITY_DESCRIPTOR =
        "SET_INVALID_ENTITY_DESCRIPTOR";
    public static final String ENTITY_DESCRIPTOR_CREATED =
        "ENTITY_DESCRIPTOR_CREATED";
    public static final String CONFIG_ERROR_CREATE_ENTITY_DESCRIPTOR =
        "CONFIG_ERROR_CREATE_ENTITY_DESCRIPTOR";
    public static final String CREATE_INVALID_ENTITY_DESCRIPTOR =
        "CREATE_INVALID_ENTITY_DESCRIPTOR";
    public static final String ENTITY_DESCRIPTOR_DELETED =
        "ENTITY_DESCRIPTOR_DELETED";
    public static final String CONFIG_ERROR_DELETE_ENTITY_DESCRIPTOR =
        "CONFIG_ERROR_DELETE_ENTITY_DESCRIPTOR";
    public static final String GOT_ENTITY_CONFIG = "GOT_ENTITY_CONFIG";
    public static final String GOT_INVALID_ENTITY_CONFIG =
        "GOT_INVALID_ENTITY_CONFIG";
    public static final String CONFIG_ERROR_GET_ENTITY_CONFIG =
        "CONFIG_ERROR_GET_ENTITY_CONFIG";
    public static final String NO_ENTITY_ID_SET_ENTITY_CONFIG =
        "NO_ENTITY_ID_SET_ENTITY_CONFIG";
    public static final String SET_ENTITY_CONFIG = "SET_ENTITY_CONFIG";
    public static final String CONFIG_ERROR_SET_ENTITY_CONFIG =
        "CONFIG_ERROR_SET_ENTITY_CONFIG";
    public static final String SET_INVALID_ENTITY_CONFIG =
        "SET_INVALID_ENTITY_CONFIG";
    public static final String NO_ENTITY_ID_CREATE_ENTITY_CONFIG =
        "NO_ENTITY_ID_CREATE_ENTITY_CONFIG";
    public static final String NO_ENTITY_DESCRIPTOR_CREATE_ENTITY_CONFIG =
        "NO_ENTITY_DESCRIPTOR_CREATE_ENTITY_CONFIG";
    public static final String ENTITY_CONFIG_EXISTS = "ENTITY_CONFIG_EXISTS";
    public static final String ENTITY_CONFIG_CREATED =
        "ENTITY_CONFIG_CREATED";
    public static final String CONFIG_ERROR_CREATE_ENTITY_CONFIG =
        "CONFIG_ERROR_CREATE_ENTITY_CONFIG";
    public static final String CREATE_INVALID_ENTITY_CONFIG =
        "CREATE_INVALID_ENTITY_CONFIG";
    public static final String NO_ENTITY_DESCRIPTOR_DELETE_ENTITY_CONFIG =
        "NO_ENTITY_DESCRIPTOR_DELETE_ENTITY_CONFIG";
    public static final String NO_ENTITY_CONFIG_DELETE_ENTITY_CONFIG =
        "NO_ENTITY_CONFIG_DELETE_ENTITY_CONFIG";
    public static final String ENTITY_CONFIG_DELETED =
        "ENTITY_CONFIG_DELETED";
    public static final String CONFIG_ERROR_DELETE_ENTITY_CONFIG =
        "CONFIG_ERROR_DELETE_ENTITY_CONFIG";
    public static final String CONFIG_ERROR_GET_ALL_HOSTED_ENTITIES =
        "CONFIG_ERROR_GET_ALL_HOSTED_ENTITIES";
    public static final String GOT_ALL_HOSTED_ENTITIES =
        "GOT_ALL_HOSTED_ENTITIES";
    public static final String CONFIG_ERROR_GET_ALL_REMOTE_ENTITIES =
        "CONFIG_ERROR_GET_ALL_REMOTE_ENTITIES";
    public static final String GOT_ALL_REMOTE_ENTITIES =
        "GOT_ALL_REMOTE_ENTITIES";
    public static final String CONFIG_ERROR_GET_ALL_ENTITIES =
        "CONFIG_ERROR_GET_ALL_ENTITIES";
    public static final String GOT_ALL_ENTITIES =
        "GOT_ALL_ENTITIES";                                
    public static final String ASSERTION_CREATED =
        "ASSERTION_CREATED";
    public static final String NO_ACS_URL = 
        "NO_ACS_URL";
    public static final String SLO_SUCCESSFUL =
        "SLO_SUCCESSFUL";
          
    private static final String WSFEDERATION_LOG = "WSFederation";
    private static Logger logger = null;

    static {
        try {
            logger = LogManager.getLogger(WSFEDERATION_LOG);
        } catch (LogException le) {
            debug.error("LogUtil.static: Error getting logger: ", le);
        }
    }
    
    /**
     * Logs message to ID-FF access logs.
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
        access(level, msgid, data, null);
    }

    /**
     * Logs message to ID-FF access logs.
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
        Level level, String msgid, String data[], Object session) 
    {
        if (logger != null) {
            try {
                logger.access(level, msgid, data, session);
            } catch (LogException le) {
                debug.error("LogUtil.access: Couldn't write log:", le);
            }
        }
    }
    
    /**
     * Logs error messages to ID-FF error log.
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
         error(level,msgid,data,null);
     }

     /** 
     * Logs error messages to ID-FF error log.
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
        Level level, String msgid, String data[], Object session) 
    {
        if (logger != null) {
            try {
                logger.error(level, msgid, data, session);
            } catch (LogException le) {
                debug.error("LogUtil.error: Couldn't write log:", le);
            }
        } 
    }

    /**
     * Returns <code>true</code> if the logging is enabled.
     *
     * @return <code>true</code> if the logging is enabled.
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
