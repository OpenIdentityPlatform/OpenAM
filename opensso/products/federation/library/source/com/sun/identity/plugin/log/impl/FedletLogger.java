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
 * $Id: FedletLogger.java,v 1.3 2008/08/06 17:28:14 exu Exp $
 *
 */

package com.sun.identity.plugin.log.impl;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Map;
import java.util.logging.Level;
import java.security.AccessController;
import java.text.MessageFormat;

import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.configuration.SystemPropertiesManager;
import com.sun.identity.plugin.log.LogException;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.LogRecord;

/**
 * This class is an implementation of the Open Federation Logger interface
 * for Fedlet deployment. The implementation uses JDK logger.
 */
public class FedletLogger implements com.sun.identity.plugin.log.Logger {
    
    protected Logger accessLogger;
    protected Logger errorLogger;

    private static Debug debug = Debug.getInstance("libPlugins");
    private static boolean logStatus = false;

    static {
        String status = SystemPropertiesManager.get(
            com.sun.identity.shared.Constants.AM_LOGSTATUS);
        logStatus = (status != null) && status.equalsIgnoreCase("ACTIVE");
    }

    /**
     * Initializes the logging for the component.
     *
     * @param componentName the component name.
     * @exception LogException if there is an error
     *	  during initialization.
     */
    public void init(String componentName) throws LogException {
        accessLogger = Logger.getLogger(componentName + ".access");
        errorLogger = Logger.getLogger(componentName + ".error");
    }
    
    /**
     * Logs message to the access logs. 
     *
     * @param level the log level , these are based on those
     *		defined in java.util.logging.Level, the values for
     *		level can be any one of the following : <br>
     *          <ul>
     *		<li>SEVERE (highest value)</li>
     *		<li>WARNING</li>
     *		<li>INFO</li>
     *		<li>CONFIG</li>
     *		<li>FINE</li>
     *		<li>FINER</li>
     *		<li>FINEST (lowest value)</li>
     *          </ul>
     * @param messageId the message or a message identifier.
     * @param data string array of dynamic data to be replaced in the message.
     * @param session the User's session object
     * @exception LogException if there is an error.
     */
    public void access(Level level,
        String messageId,
        String data[],
        Object session
    ) throws LogException {
        access(level, messageId, data, session, null);
    }

    /**
     * Writes access to a component into a log.
     * @param level indicating log level
     * @param messageId Message id
     * @param data string array of dynamic data only known during run time
     * @param session Session object (it could be null)
     * @param props representing log record columns
     * @exception LogException if there is an error.
     */
    public void access(
        Level level,
        String messageId,
        String data[],
        Object session,
        Map props) throws LogException
    {
        if (isAccessLoggable(level)) {
            LogRecord lr = new LogRecord(level, 
                formatMessage(messageId, data, session));
            accessLogger.log(lr);
        }
   }
   
    private static String formatMessage(String messageId, String[] param,
        Object session) {
        if ((param == null) || (param.length == 0)) {
            return messageId;
        } else {
            for (int i = 0; i < param.length; i++) {
                messageId = messageId + "\n{" + param[i] + "}";
            }
            if (session != null) {
                messageId = messageId + "\n{" + session.toString() + "}";
            }
            return messageId;
        }
    } 
    
    /**
     * Logs error messages to the error logs.
     *
     * @param level the log level , these are based on those
     *		defined in java.util.logging.Level, the values for
     *          level can be any one of the following : <br>
     *          <ul>
     *		<li>SEVERE (highest value)</li>
     *		<li>WARNING</li>
     *		<li>INFO</li>
     *		<li>CONFIG</li>
     *		<li>FINE</li>
     *		<li>FINER</li>
     *		<li>FINEST (lowest value)</li>
     *          </ul>
     * @param messageId the message or a message identifier.
     * @param data string array of dynamic data to be replaced in the message.
     * @param session the User's Session object.
     * @exception LogException if there is an error.
     */
    public void error(Level level, String messageId, String data[],
            Object session) throws LogException {
        error(level, messageId, data, session, null);
    }

    /**
     * Writes error occurred in a component into a log.
     * @param level indicating log level
     * @param messageId Message id
     * @param data string array of dynamic data only known during run time
     * @param session Session object (it could be null)
     * @param props log record columns
     * @exception LogException if there is an error.
     */
    public void error(
      Level level,
      String messageId,
      String data[],
      Object session,
      Map props) throws LogException
    {
        if (isErrorLoggable(level)) {
            LogRecord lr = new LogRecord(level, 
                formatMessage(messageId, data, session));
            errorLogger.log(lr);
        }
    }
    
    /**
     * Returns <code>true</code> if logging is enabled.
     *
     * @return <code>true</code> if logging is enabled.
     */
    public boolean isLogEnabled() {
        return logStatus;
    }
    
    /**
     * Checks if an access message of the given level would actually be logged
     * by this logger. This check is based on the Logger's effective level.
     *
     * @param level a message logging level defined in java.util.logging.Level.
     * @return true if the given message level is currently being logged.
     */
    public boolean isAccessLoggable(Level level) {
        return accessLogger.isLoggable(level);
    }
    
    /**
     * Checks if an error message of the given level would actually be logged
     * by this logger. This check is based on the Logger's effective level.
     *
     * @param level a message logging level defined in java.util.logging.Level.
     * @return true if the given message level is currently being logged.
     */
    public boolean isErrorLoggable(Level level) {
        return errorLogger.isLoggable(level);

    }
}
