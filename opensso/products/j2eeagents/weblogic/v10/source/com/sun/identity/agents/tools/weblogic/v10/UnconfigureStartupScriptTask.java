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
 * $Id: UnconfigureStartupScriptTask.java,v 1.2 2008/06/25 05:52:21 qcheng Exp $
 *
 */

package com.sun.identity.agents.tools.weblogic.v10;

import java.io.File;
import java.util.Map;

import com.sun.identity.install.tools.configurator.ITask;
import com.sun.identity.install.tools.configurator.IStateAccess;
import com.sun.identity.install.tools.configurator.InstallException;
import com.sun.identity.install.tools.util.LocalizedMessage;
import com.sun.identity.install.tools.util.Debug;
import com.sun.identity.install.tools.util.FileUtils;

/**
 * This task delete the setAgentEnv script for Weblogic.
 */
public class UnconfigureStartupScriptTask extends StartupScriptBase
        implements ITask {
    
    public boolean execute(String name, IStateAccess stateAccess,
            Map properties) throws InstallException {
        
        boolean status = false;
        
        String agentEnvFile = 
                (String) stateAccess.get(STR_KEY_AGENT_ENV_FILE_PATH);
        File file = new File(agentEnvFile);
        if (file.delete()) {
            Debug.log("UnconfigureStartupScriptTask.execute() - Deleted " + 
                    agentEnvFile);
            status = true;
        }
        
        return status;
    }
    
    
    public boolean rollBack(String name, IStateAccess stateAccess,
            Map properties) throws InstallException {
        // Nothing to roll back during un-install
        return true;
    }
    
    public LocalizedMessage getExecutionMessage(IStateAccess stateAccess,
            Map properties) {
        String destFile = (String) stateAccess.get(STR_KEY_AGENT_ENV_FILE_PATH);
        Object[] args = { destFile };
        
        LocalizedMessage message =
                LocalizedMessage.get(
                LOC_TSK_MSG_UNCONFIGURE_STARTUP_SCRIPT_EXECUTE,
                STR_WL_GROUP, args);
        
        return message;
    }
    
    public LocalizedMessage getRollBackMessage(IStateAccess stateAccess,
            Map properties) {
        // No roll back during un-install
        return null;
    }
    
    public static final String LOC_TSK_MSG_UNCONFIGURE_STARTUP_SCRIPT_EXECUTE =
            "TSK_MSG_UNCONFIGURE_STARTUP_SCRIPT_EXECUTE";
}
