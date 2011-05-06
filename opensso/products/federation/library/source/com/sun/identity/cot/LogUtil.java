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
 * $Id: LogUtil.java,v 1.2 2008/06/25 05:46:39 qcheng Exp $
 *
 */

package com.sun.identity.cot;

import com.sun.identity.plugin.log.Logger;
import com.sun.identity.plugin.log.LogException;
import com.sun.identity.plugin.log.LogManager;
import java.util.logging.Level;

/**
 * The <code>LogUtil</code> class provides methods which are used by
 * circle of trust manager to write logs.
 */
public class LogUtil {
    
    
    public static final String INVALID_COT_NAME = "INVALID_COT_NAME";
    public static final String CONFIG_ERROR_MODIFY_COT_DESCRIPTOR =
            "CONFIG_ERROR_MODIFY_COT_DESCRIPTOR";
    public static final String CONFIG_ERROR_GET_ALL_COT_DESCRIPTOR =
            "CONFIG_ERROR_GET_ALL_COT_DESCRIPTOR";
    public static final String NO_COT_NAME_CREATE_COT_DESCRIPTOR =
            "NO_COT_NAME_CREATE_COT_DESCRIPTOR";
    public static final String COT_EXISTS_CREATE_COT_DESCRIPTOR =
            "COT_EXISTS_CREATE_COT_DESCRIPTOR";
    public static final String INVALID_COT_TYPE="INVALID_COT_TYPE";
    public static final String CONFIG_ERROR_CREATE_COT_DESCRIPTOR =
            "CONFIG_ERROR_CREATE_COT_DESCRIPTOR";
    public static final String COT_DESCRIPTOR_CREATED="COT_DESCRIPTOR_CREATED";
    public static final String NULL_COT_NAME_ADD_COT_DESCRIPTOR=
            "NULL_COT_NAME_ADD_COT_DESCRIPTOR";
    public static final String NULL_ENTITYID_ADD_COT_DESCRIPTOR=
            "NULL_ENTITYID_ADD_COT_DESCRIPTOR";
    public static final String CONFIG_ERROR_ADD_COT_MEMBER =
            "CONFIG_ERROR_ADD_COT_MEMBER";
    public static final String NULL_COT_NAME_REMOVE_COT_MEMBER =
            "NULL_COT_NAME_REMOVE_COT_MEMBER";
    public static final String NULL_ENTITYID_REMOVE_COT_MEMBER =
            "NULL_ENTITYID_REMOVE_COT_MEMBER";
    public static final String CONFIG_ERROR_REMOVE_COT_MEMBER =
            "CONFIG_ERROR_REMOVE_COT_MEMBER";  
    public static final String NULL_COT_NAME_LIST_COT =
            "NULL_COT_NAME_LIST_COT";
    public static final String CONFIG_ERROR_LIST_COT_MEMBER =
            "CONFIG_ERROR_LIST_COT_MEMBER";
    public static final String COT_DESCRIPTOR_DELETED =
            "COT_DESCRIPTOR_DELETED";
    public static final String CONFIG_ERROR_DELETE_COT_DESCRIPTOR =
            "CONFIG_ERROR_DELETE_COT_DESCRIPTOR";
    public static final String INVALID_NAME_ERROR_DELETE_COT_DESCRIPTOR =
            "INVALID_NAME_ERROR_DELETE_COT_DESCRIPTOR";
    public static final String HAS_ENTITIES_DELETE_COT_DESCRIPTOR =
            "HAS_ENTITIES_DELETE_COT_DESCRIPTOR";
    public static final String INVALID_COT_TYPE_DELETE_COT_DESCRIPTOR =
            "INVALID_COT_TYPE_DELETE_COT_DESCRIPTOR";
    public static final String COT_FROM_CACHE="COT_FROM_CACHE";
    public static final String COT_DESCRIPTOR_RETRIEVED =
            "COT_DESCRIPTOR_RETRIEVED";
    public static final String CONFIG_ERROR_GET_COT_DESCRIPTOR =
            "CONFIG_ERROR_GET_COT_DESCRIPTOR";
    public static final String CONFIG_ERROR_GET_ALL_ACTIVE_COT =
            "CONFIG_ERROR_GET_ALL_ACTIVE_COT";
    public static final String CONFIG_ERROR_RETREIVE_COT =
            "CONFIG_ERROR_RETREIVE_COT";
    private static Logger logger = null;
    
    static {
        try {
            logger = LogManager.getLogger(COTConstants.COT);
        } catch (LogException le) {
            COTUtils.debug.message("Error getting logger:", le);
        }
    }
    
    /**
     * Logs message to COT access logs.
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
     * Logs message to COT access logs.
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
    public static void access(Level level, String msgid,
            String data[], Object session) {
        if (logger != null) {
            try {
                logger.access(level, msgid, data, session);
            } catch (LogException le) {
                COTUtils.debug.error("LogUtil.access: Couldn't write log:", le);
            }
        }
    }
    
    /**
     * Logs error messages to COT error log.
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
     * Logs error messages to COT error log.
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
    public static void error(Level level, String msgid,
            String data[], Object session) {
        if (logger != null) {
            try {
                logger.error(level, msgid, data, session);
            } catch (LogException le) {
                COTUtils.debug.error("LogUtil.error: Couldn't write log:", le);
            }
        }
    }
    
    /**
     * Checks if the logging is enabled.
     *
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
     *
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
     *
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
