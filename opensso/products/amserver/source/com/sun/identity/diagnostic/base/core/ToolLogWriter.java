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
 * $Id: ToolLogWriter.java,v 1.2 2009/11/13 21:52:43 ak138937 Exp $
 *
 */

package com.sun.identity.diagnostic.base.core;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import com.iplanet.am.util.SystemProperties;
import com.sun.identity.diagnostic.base.core.common.DTException;
import com.sun.identity.diagnostic.base.core.common.ToolConstants;

/**
 * Writes log entries.
 */
public class ToolLogWriter {
    
    private static FileHandler fh = null;
    private static Logger logger = null;
    private static boolean enabled = false;
    
    private ToolLogWriter() {
    }
    
    /**
     * Initializes the logger with environment parameters.
     */
    public static void init() throws IOException {
        String logName = getLogName();
        logger = Logger.getLogger(ToolLogWriter.class.getName());
        fh = new FileHandler(logName, true);
        fh.setFormatter(new SimpleFormatter());
        logger.addHandler(fh);
        //log only above the log level specified
        logger.setLevel(getLogLevel());
        logger.setUseParentHandlers(false);
        String status = "";
        status = SystemProperties.get(ToolConstants.PROPERTY_LOG_ENABLED,
            "off");
        enabled = status.equalsIgnoreCase("on") ? true : false;
    }
    
    /**
     * Writes to log.
     *
     * @param bundleName Name of the resource bundle.
     * @param level Logging level of the message.
     * @param msgId ID for message.
     * @param msgdata array of log message "data".
     * @throws DTException if log cannot be written.
     */
    public static void log(
        String bundleName,
        Level level,
        String msgId,
        String[] msgdata
    ) throws DTException {
        if (enabled) {
            try {
                if (logger.isLoggable(level)) {
                    String msg = formatMsg(msgId, msgdata, bundleName);
                    if (msg != null) {
                        logger.log(level, msg);
                    }
                }
            } catch (Exception e) {
                throw new DTException(e.getMessage());
            }
        }
    }
    
    /**
     * Writes to log.
     *
     * @param bundle Resource bundle to be used.
     * @param level Logging level of the message.
     * @param msgId ID for message.
     * @param msgdata array of log message "data".
     * @throws DTException if log cannot be written.
     */
    public static void log(
        ResourceBundle bundle,
        Level level,
        String msgId,
        String[] msgdata
    ) throws DTException {
        if (enabled) {
            try {
                if (logger.isLoggable(level)) {
                    String msg = formatMsg(msgId, msgdata, bundle);
                    if (msg != null) {
                        logger.log(level, msg);
                    }
                }
            } catch (Exception e) {
                throw new DTException(e.getMessage());
            }
        }
    }

    /**
     * Writes to log.
     *
     * @param msg Message string.
     * @throws DTException if log cannot be written.
     */
    public static void log(
        String msg
    ) throws DTException {
        if (enabled) {
            try {
                if (msg != null) {
                    logger.info(msg);
                }
            } catch (Exception e) {
                throw new DTException(e.getMessage());
            }
        }
    }
    
    /**
     * Returns formatted message with bundle name.
     *
     * @param msg Message to be formatted.
     * @param args Arguments to be used for formatting.
     * @param bundleName Name of the resource bundle.
     * @return formatted message.
     */
    private static String formatMsg(
        String msg,
        Object[] args,
        String bundleName
    ) {
        String result = msg;
        ResourceBundle bundle = ResourceBundle.getBundle(
            bundleName, Locale.getDefault());
        String mId  = bundle.getString(msg);
        if (args == null || args.length == 0) {
            result = mId;
        } else {
            result = MessageFormat.format(mId, args);
        }
        return result;
    }
    
    /**
     * Returns formatted message from bundle.
     *
     * @param msg Message to be formatted.
     * @param args Arguments to be used for formatting.
     * @param bundle Resource bundle to use.
     * @return formatted message.
     */
    private static String formatMsg(
        String msg,
        Object[] args,
        ResourceBundle bundle
    ) {
        String result = msg;
        String mId  = bundle.getString(msg);
        if (args == null || args.length == 0) {
            result = mId;
        } else {
            result = MessageFormat.format(mId, args);
        }
        return result;
    }
    
    /**
     * Returns name of the log file.
     *
     * @return name of the log file.
     */
    private static String getLogName() {
        String logFile = null;
        logFile =  SystemProperties.get(ToolConstants.PROPERTY_LOG_FILENAME);
        return (logFile != null) ? logFile : SystemProperties.get(
            ToolConstants.DEF_LOGFILE_NAME);
    }
    
    /**
     * Returns level of log.
     *
     * @return level of the log.
     */
    private static Level getLogLevel() {
        String level = SystemProperties.get(ToolConstants.PROPERTY_LOG_LEVEL);
        return Level.parse(level);
    }
}
