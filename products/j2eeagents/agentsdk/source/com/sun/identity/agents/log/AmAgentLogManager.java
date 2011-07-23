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
 * $Id: AmAgentLogManager.java,v 1.3 2008/06/25 05:51:53 qcheng Exp $
 *
 */

package com.sun.identity.agents.log;

import com.sun.identity.agents.arch.AgentConfiguration;
import com.sun.identity.agents.arch.AgentException;
import com.sun.identity.agents.arch.IConfigurationListener;
import com.sun.identity.agents.arch.IModuleAccess;
import com.sun.identity.agents.arch.ISystemAccess;
import com.sun.identity.agents.arch.Manager;
import com.sun.identity.agents.arch.Module;
import com.sun.identity.agents.arch.ServiceFactory;

/**
 * The class manages the agent logging operations
 */
public class AmAgentLogManager extends Manager {
    
    private AmAgentLogManager() throws AgentException {
        super(AmAgentLogModule.getModule(), new String[] {});
        setAmAgentLog(ServiceFactory.getAmAgentLog(this));
    }

    private void setAmAgentLog(IAmAgentLog amAgentLog) {
        _amAgentLog = amAgentLog;
    }

    private IAmAgentLog getAmAgentLog() {
        return _amAgentLog;
    }
    
    private IAmAgentLog _amAgentLog;

    
    public static IModuleAccess getModuleAccess() {
        return getAmAgentLogModule().newModuleAccess();
    }

    public static IAmAgentLog getAmAgentLogInstance() {
        return getAmAgentLogManager().getAmAgentLog();
    }
    
    public static ISystemAccess getSystemAccess() {
        return getAmAgentLogManager().newSystemAccess();
    }

    private static AmAgentLogManager getAmAgentLogManager() {
        return _manager;
    }

    private static void notifyConfigurationChange() {
        if(isLogMessageEnabled()) {
            logMessage(SUBSYSTEM_NAME
                       + ": Received configuration notification");
            logMessage(SUBSYSTEM_NAME + ": Swapping manager instance");
        }

        try {
            AmAgentLogManager manager = new AmAgentLogManager();

            _manager = manager;
        } catch(Exception ex) {
            logError(SUBSYSTEM_NAME + ": Unable to swap manager instance",
                     ex);
            logError(SUBSYSTEM_NAME + ": Dynamic property load failed");
        }
    }

    static class AmAgentLogConfigurationListener
            implements IConfigurationListener {

        public void configurationChanged() {
            AmAgentLogManager.notifyConfigurationChange();
        }

        public String getName() {
            return SUBSYSTEM_NAME;
        }
    }
    
    private static void logError(String msg) {
        getAmAgentLogModule().logError(msg);
    }
    
    private static void logError(String msg, Throwable th) {
        getAmAgentLogModule().logError(msg, th);
    }
    
    private static void logMessage(String msg) {
        getAmAgentLogModule().logMessage(msg);
    }
    
    private static boolean isLogMessageEnabled() {
        return getAmAgentLogModule().isLogMessageEnabled();
    }
    
    private static Module getAmAgentLogModule() {
        return AmAgentLogModule.getModule();
    }

    private static AmAgentLogManager _manager = null;
    private static final String SUBSYSTEM_NAME = "AmAgentLog";
    
    static {
        try {
            _manager = new AmAgentLogManager();
        } catch(AgentException ex) {
            AmAgentLogModule.getModule().logError(
                    "AmAgentLogManager initialization failed", ex);

            throw new RuntimeException(
                "Exception caught in AmAgentLogManager initializer: "
                + ex.getMessage());
        }

        AgentConfiguration.addConfigurationListener(
            new AmAgentLogConfigurationListener());
    }
}
