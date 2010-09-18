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
 * $Id: Logger.java,v 1.3 2008/06/25 05:47:28 qcheng Exp $
 *
 */

package com.sun.identity.plugin.log;

import java.util.Map;
import java.util.logging.Level;

/**
 * This interface defines methods which will be invoked by the
 * the Federation Framework to write federation related
 * events and errors to the access and error log files.
 *
 * @supported.all.api
 */

public interface Logger {
    
    /**
     * Initializes the logging for the component.
     *
     * @param componentName the component name.
     * @exception LogException if there is an error
     *	  during initialization.
     */
    public void init(String componentName) throws LogException;
    
    /**
     * Logs message to the access logs. 
     *
     * @param level the log level , these are based on those
     *		defined in java.util.logging.Level, the values for
     *		level can be any one of the following : <br>
     *          <ul>
     *		<li>SEVERE (highest value) <br>
     *		<li>WARNING <br>
     *		<li>INFO <br>
     *		<li>CONFIG <br>
     *		<li>FINE <br>
     *		<li>FINER <br>
     *		<li>FINEST (lowest value) <br>
     *          </ul>
     * @param messageID the message or a message identifier.
     * @param data string array of dynamic data to be replaced in the message.
     * @param session the User's session object
     * @exception LogException if there is an error.
     */
    
    public void access(Level level,
                       String messageID,
                       String data[],
                       Object session) throws LogException;
    
    
    /**
     * Writes access to a component into a log.
     * @param level indicating log level
     * @param msgid Message id
     * @param data string array of dynamic data only known during run time
     * @param session Session object (it could be null)
     * @param props representing log record columns
     * @exception LogException if there is an error.
     */
    public abstract void access(
        Level level,
        String msgid,
        String data[],
        Object session,
        Map props) throws LogException;
 

    /**
     * Logs error messages to the error logs.
     *
     * @param level the log level , these are based on those
     *		defined in java.util.logging.Level, the values for
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
     * @param messageId the message or a message identifier.
     * @param data string array of dynamic data to be replaced in the message.
     * @param session the User's Session object.
     * @exception LogException if there is an error.
     */
    public void error(Level level,String messageId,String data[],
                      Object session) throws LogException;
    
    /**
     * Writes error occurred in a component into a log.
     * @param level indicating log level
     * @param msgid Message id
     * @param data string array of dynamic data only known during run time
     * @param session Session object (it could be null)
     * @param props log record columns
     * @exception LogException if there is an error.
     */
    public abstract void error(
      Level level,
      String msgid,
      String data[],
      Object session,
      Map props) throws LogException;

    /**
     * Checks if the logging is enabled.
     *
     * @return true if logging is enabled.
     */
    public boolean isLogEnabled() ;
    
    /**
     * Checks if an access message of the given level would actually be logged
     * by this logger. This check is based on the Logger's effective level.
     *
     * @param level a message logging level defined in java.util.logging.Level.
     * @return true if the given message level is currently being logged.
     */
    public boolean isAccessLoggable(Level level);
    
    /**
     * Checks if an error message of the given level would actually be logged
     * by this logger. This check is based on the Logger's effective level.
     *
     * @param level a message logging level defined in java.util.logging.Level.
     * @return true if the given message level is currently being logged.
     */
    public boolean isErrorLoggable(Level level);
}

