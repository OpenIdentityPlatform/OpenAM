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
 * $Id: LogProvider.java,v 1.5 2008/08/06 17:29:26 exu Exp $
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

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;

import com.sun.identity.authentication.internal.AuthPrincipal;

import com.sun.identity.log.LogConstants;
import com.sun.identity.log.Logger;
import com.sun.identity.log.LogRecord;
import com.sun.identity.log.messageid.LogMessageProvider;
import com.sun.identity.log.messageid.MessageProviderFactory;

import com.sun.identity.plugin.log.LogException;
import com.sun.identity.plugin.session.SessionManager;
import com.sun.identity.plugin.session.SessionException;
import com.sun.identity.security.AdminTokenAction;

/**
 * This class is the AM implementation of the Open Federation Logger interface.
 */
public class LogProvider implements com.sun.identity.plugin.log.Logger {
    
    protected Logger accessLogger;
    protected Logger errorLogger;

    private LogMessageProvider msgProvider;

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
        accessLogger = (com.sun.identity.log.Logger)Logger.getLogger(
            componentName + ".access");
        errorLogger = (com.sun.identity.log.Logger)Logger.getLogger(
            componentName + ".error");
        try {
            msgProvider = MessageProviderFactory.getProvider(componentName);
        } catch (IOException e) {
            debug.error(
                "LogProvider.<init>: unable to create log message provider", e);
        }
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
            SSOToken authSSOToken = (SSOToken) AccessController.doPrivileged(
                AdminTokenAction.getInstance());
            LogRecord lr = getLogRecord(
                messageId, data, session, props, authSSOToken);
            if (lr != null) {
                accessLogger.log(lr, authSSOToken);
            }
        }
   }
    
    
    private LogRecord getLogRecord(
        String messageId, String data[], Object session, Map properties,
        SSOToken authSSOToken)
    {
        SSOToken ssoToken = null;
        if (session != null) {
            try {
                String sid = SessionManager.getProvider().getSessionID(
                    session);
                ssoToken = SSOTokenManager.getInstance().createSSOToken(sid);
            } catch (SessionException se) {
                debug.message("Error getting session provider: " , se);
            } catch (SSOException soe) {
                debug.message("Error creating SSOToken: " , soe);
            }
        }
        SSOToken realToken = (ssoToken != null) ? ssoToken : authSSOToken;
        LogRecord lr = msgProvider.createLogRecord(messageId, data, realToken);
        if ((properties != null) && (lr != null)) {
            String nameIDValue = (String) properties.get(LogConstants.NAME_ID);
            if ((nameIDValue != null) && (nameIDValue.length() > 0)) {
                lr.addLogInfo(LogConstants.NAME_ID, nameIDValue);
            }
            if (ssoToken == null) {
                String clientDomain = 
                    (String)properties.get(LogConstants.DOMAIN);
                if (clientDomain != null) {
                    lr.addLogInfo(LogConstants.DOMAIN, clientDomain);
                }
                String clientID = (String)properties.get(LogConstants.LOGIN_ID);
                if (clientID != null) {
                    lr.addLogInfo(LogConstants.LOGIN_ID, clientID);
                }
                String ipAddress = (String)properties.get(LogConstants.IP_ADDR);
                if (ipAddress != null) {
                    String hostName = ipAddress;
                    try {
                        if (Logger.resolveHostNameEnabled()) {
                            hostName = InetAddress.getByName(ipAddress).
                                getHostName();
                        }
                    } catch (Exception e) {
                        if (debug.messageEnabled()) {
                            debug.message(
                                "LogProvider:Unable to get Host for:"
                                + ipAddress);
                        }
                        hostName = ipAddress;
                    }
                    lr.addLogInfo(LogConstants.IP_ADDR, hostName);
                }

                String loginIDSid = 
                    (String)properties.get(LogConstants.LOGIN_ID_SID);
                if (loginIDSid != null) {
                    lr.addLogInfo(LogConstants.LOGIN_ID_SID, loginIDSid);
                }
                String moduleName = 
                    (String)properties.get(LogConstants.MODULE_NAME);
                if (moduleName != null) {
                    lr.addLogInfo(LogConstants.MODULE_NAME, moduleName);
                }
                String contextID  = 
                    (String)properties.get(LogConstants.CONTEXT_ID);
                if (contextID != null) {
                    lr.addLogInfo(LogConstants.CONTEXT_ID, contextID);
                }
            }
        }
        return lr;
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
            SSOToken authSSOToken = (SSOToken) AccessController.doPrivileged(
                AdminTokenAction.getInstance());
            LogRecord lr = getLogRecord(
                messageId, data, session, props, authSSOToken);
            if (lr != null) {
                errorLogger.log(lr, authSSOToken);
            }
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
        SSOToken authSSOToken = (SSOToken) AccessController.doPrivileged(
            AdminTokenAction.getInstance());
        if ((authSSOToken == null) || !logStatus) {
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
    public boolean isErrorLoggable(Level level) {
        SSOToken authSSOToken = (SSOToken) AccessController.doPrivileged(
            AdminTokenAction.getInstance());
        if ((authSSOToken == null) || !logStatus) {
            return false;
	}
        return errorLogger.isLoggable(level);

    }
}
