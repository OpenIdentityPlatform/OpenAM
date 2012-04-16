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
 * $Id: AmAgentRemoteLog.java,v 1.3 2008/07/01 22:30:56 huacui Exp $
 *
 */

package com.sun.identity.agents.log;


import java.util.logging.Level;

import com.iplanet.sso.SSOToken;
import com.sun.identity.agents.arch.AgentBase;
import com.sun.identity.agents.arch.AgentConfiguration;
import com.sun.identity.agents.arch.AgentException;
import com.sun.identity.agents.arch.LocalizedMessage;
import com.sun.identity.agents.arch.Manager;
import com.sun.identity.agents.common.CommonFactory;
import com.sun.identity.agents.common.IApplicationSSOTokenProvider;
import com.sun.identity.log.LogRecord;
import com.sun.identity.log.Logger;


/**
 * The class does the agent remote logging
 */
public class AmAgentRemoteLog extends AgentBase 
implements ILogConfigurationConstants, IAmAgentRemoteLog 
{

    public AmAgentRemoteLog(Manager manager) {
        super(manager);
    }
    
    public void initialize() throws AgentException {
        try {
            setRemoteLoggingHandler();
        } catch(Exception ex) {
            throw new AgentException(
                "Failed to initialize AmAgentRemoteLog instance", ex);
        }        
    }

    /**
     * Logs a message for j2ee agents
     * @param token user SSO Token as a String
     * @param message Message String
     * @return boolean success of failure while logging
     */
    public boolean log(SSOToken token, LocalizedMessage message) 
    throws AgentException {

        boolean result = false;

        try {
            if(message == null) {
                throw new IllegalArgumentException("message = " + message);
            }

            if(token == null) {
                if(isLogWarningEnabled()) {
                    logWarning(
                        "AmAgentRemoteLog: No user SSO Token specified, "
                        + "using App SSO Token as default.");
                }

                token = getAppSSOToken();
            }

            LogRecord record = new LogRecord(getRemoteLoggingLevel(),
                                             message.toString(), token);

            getRemoteLogHandler().log(record, getAppSSOToken());

            result = true;
        } catch(Exception ex) {
            throw new AgentException("Unable to process log request", ex);
        }

        return result;
    }

    /**
     * Set the remote log handler if the remote logging flag is true
     * @throws Exception if the remote log handler cannot be instantiated
     */
    private void setRemoteLoggingHandler() throws Exception {

        setRemoteLoggingLevel(Level.INFO);

        if(getRemoteLogHandler() == null) {
            String remoteFileName = getConfigurationString(
                    CONFIG_REMOTE_LOG_FILE_NAME);
            
            if (remoteFileName == null || remoteFileName.trim().length() == 0) {
                throw new AgentException("Invalid remote log file name");
            }

            setRemoteLogHandler(
                (com.sun.identity.log.Logger) Logger.getLogger(
                    remoteFileName));
        }
    }

    /**
     * Get the app sso token
     *
     * @return SSOToken get the App SSO Token
     *
     */
    private SSOToken getAppSSOToken() throws AgentException {
        return AgentConfiguration.getAppSSOToken();
    }

    /**
     * Set the remote logging handler
     *
     *
     * @param logHandler
     *
     */
    private void setRemoteLogHandler(Logger logHandler) {
        _remoteLogHandler = logHandler;
    }

    /**
     * Get the remote logging handler
     *
     * @return the remote logging handler
     *
     */
    private Logger getRemoteLogHandler() {
        return _remoteLogHandler;
    }

    /**
     * Get the remote logging level
     *
     * @return Level logging level
     *
     */
    private Level getRemoteLoggingLevel() {
        return _remoteLoggingLevel;
    }

    /**
     * Set the remote logging level
     *
     * @param level Logging level
     *
     */
    private void setRemoteLoggingLevel(Level level) {
        _remoteLoggingLevel = level;
    }

    private Level _remoteLoggingLevel;
    private Logger _remoteLogHandler;
}

