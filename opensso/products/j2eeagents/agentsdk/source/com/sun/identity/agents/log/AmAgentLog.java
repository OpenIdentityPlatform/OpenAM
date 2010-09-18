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
 * $Id: AmAgentLog.java,v 1.2 2008/06/25 05:51:53 qcheng Exp $
 *
 */

package com.sun.identity.agents.log;



import com.iplanet.sso.SSOToken;
import com.sun.identity.agents.arch.AgentBase;
import com.sun.identity.agents.arch.AgentException;
import com.sun.identity.agents.arch.LocalizedMessage;
import com.sun.identity.agents.arch.Manager;
import com.sun.identity.agents.arch.ServiceFactory;

/**
 * Class AmAgentLog : Serves as a logging module
 *
 */
public class AmAgentLog extends AgentBase 
implements ILogConfigurationConstants, IAmAgentLog {

    public AmAgentLog(Manager manager) {
        super(manager);
    }
    
    public void initialize() throws AgentException {
        setLogModeFromConfiguration();
        if(isRemoteLogEnabled()) {
            try {
                setAmAgentRemoteLog(
                        ServiceFactory.getAmAgentRemoteLog(getManager()));
            } catch(Exception ex) {
                logError(
                    "AmAgentLog: Unable to create AmAgentRemoteLog instance, "
                    + "will attempt to use local logging only.", ex);
                setLogMode(INT_LOG_MODE_LOCAL);
            }
        }
        setAmAgentLocalLog(ServiceFactory.getAmAgentLocalLog(getManager()));        
    }

    /**
     * Read the Logging Config
     *
     * @throws AgentException
     */
    private void setLogModeFromConfiguration() throws AgentException {

        int mode = DEFAULT_INT_LOG_MODE;
        String disposition =
            getManager().getConfigurationString(CONFIG_LOG_DISPOSITION,
                                                STR_LOG_MODE_LOCAL);
        boolean invalidValueSpecified = false;

        if((disposition != null) && (disposition.length() > 0)) {
            if( !disposition.equals(STR_LOG_MODE_LOCAL)
                    && !disposition.equals(STR_LOG_MODE_REMOTE)
                    && !disposition.equals(STR_LOG_MODE_ALL)) {
                invalidValueSpecified = true;
            }
        } else {
            invalidValueSpecified = true;
        }

        if(invalidValueSpecified) {
            if(isLogWarningEnabled()) {
                logWarning("AmAgentLog: Invalid log mode specified: "
                           + disposition + ", using default value: "
                           + STR_LOG_MODE_LOCAL);
            }

            disposition = STR_LOG_MODE_LOCAL;
        }

        if(disposition.equals(STR_LOG_MODE_REMOTE)) {
            mode = INT_LOG_MODE_REMOTE;
        } else if(disposition.equals(STR_LOG_MODE_LOCAL)) {
            mode = INT_LOG_MODE_LOCAL;
        } else if(disposition.equals(STR_LOG_MODE_ALL)) {
            mode = INT_LOG_MODE_ALL;
        }

        setLogMode(mode);
    }

    /**
     * Logs a local/remote message for j2ee agents
     * This function internally decides whether to do local or remote logs
     * @param token user SSO Token as a String
     * @param message LocalizedMessage object
     * @return boolean success of failure while logging
     *
     * @throws AgentException
     */
    public boolean log(SSOToken token, LocalizedMessage message)
            throws AgentException {

        boolean messageLogged = false;

        if(isRemoteLogEnabled()) {
            try {
                messageLogged = getAmAgentRemoteLog().log(token, message);
            } catch(Exception ex) {
                logError(
                    "AmAgentLog: Failed to write remote log, trying local",
                    ex);
            }
        }

        if( !messageLogged || isLocalLogEnabled()) {
            try {
                messageLogged = getAmAgentLocalLog().log(message);
            } catch(Exception ex) {
                logError("AmAgentLog: Failed to write local log", ex);
            }
        }

        if( !messageLogged) {
            throw new AgentException("Unable to Log Message: token = "
                                     + token + ", message = " + message);
        }

        return messageLogged;
    }

    /**
     * Method setAmAgentRemoteLog
     *
     *
     * @param remoteLog
     *
     */
    private void setAmAgentRemoteLog(IAmAgentRemoteLog remoteLog) {
        _amAgentRemoteLog = remoteLog;
    }

    private IAmAgentRemoteLog getAmAgentRemoteLog() {
        return _amAgentRemoteLog;
    }

    private void setLogMode(int mode) throws AgentException {

        if((mode != INT_LOG_MODE_LOCAL) && (mode != INT_LOG_MODE_REMOTE)
                && (mode != INT_LOG_MODE_ALL)) {
            throw new AgentException("Invalid Log Mode: " + mode);
        }

        _logMode = mode;

        if(isLogMessageEnabled()) {
            logMessage("AmAgentLog: Mode changed to "
                       + getLogModeString(mode) + "(" + mode + ")");
        }
    }

    private String getLogModeString(int mode) {

        String result = "UNKNOWN";

        switch(mode) {

        case INT_LOG_MODE_LOCAL :
            result = STR_LOG_MODE_LOCAL;
            break;

        case INT_LOG_MODE_REMOTE :
            result = STR_LOG_MODE_REMOTE;
            break;

        case INT_LOG_MODE_ALL :
            result = STR_LOG_MODE_ALL;
            break;
        }

        return result;
    }

    private int getLogMode() {
        return _logMode;
    }

    private boolean isRemoteLogEnabled() {
        return((_logMode == INT_LOG_MODE_REMOTE)
               || (_logMode == INT_LOG_MODE_ALL));
    }

    private boolean isLocalLogEnabled() {
        return((_logMode == INT_LOG_MODE_LOCAL)
               || (_logMode == INT_LOG_MODE_ALL));
    }

    private void setAmAgentLocalLog(IAmAgentLocalLog amAgentLocalLog) {
        _amAgentLocalLog = amAgentLocalLog;
    }

    private IAmAgentLocalLog getAmAgentLocalLog() {
        return _amAgentLocalLog;
    }

    private IAmAgentRemoteLog _amAgentRemoteLog;
    private IAmAgentLocalLog  _amAgentLocalLog;
    private int              _logMode;
}
