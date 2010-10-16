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
 * $Id: RemoveAgentLibsTask.java,v 1.2 2008/11/21 22:21:44 leiming Exp $
 *
 */

package com.sun.identity.agents.tools.websphere;

import java.io.File;
import java.util.Map;

import com.sun.identity.install.tools.configurator.ITask;
import com.sun.identity.install.tools.configurator.IStateAccess;
import com.sun.identity.install.tools.configurator.InstallException;
import com.sun.identity.install.tools.util.LocalizedMessage;
import com.sun.identity.install.tools.util.Debug;

/**
 * This class removes agent's jars installed under Websphere's lib/ext.
 */
public class RemoveAgentLibsTask implements ITask, IConfigKeys,IConstants {
    
    public boolean execute(String name, IStateAccess stateAccess,
            Map properties) throws InstallException {
        boolean status = true;
        String libExtDir = (String)stateAccess.get(STR_KEY_WAS_LIB_EXT);
        
        try {
            // Delete agent.jar and openssoclientsdk.jar
            libExtDir = (String)stateAccess.get(STR_KEY_WAS_LIB_EXT);
            File agentJar = new File(libExtDir, STR_AGENT_JAR);
            File clientsdkJar = new File(libExtDir, STR_OPENSSOCLIENTSDK_JAR);
            Debug.log("RemoveAgentLibsTask.execute () - " + libExtDir);
            
            if (!agentJar.delete()) {
                Debug.log(
                    "RemoveAgentLibsTask.rollBack() Unable to delete file: " +
                    agentJar.getAbsolutePath());
                status = false;
            }
            
            if (!clientsdkJar.delete()) {
                Debug.log(
                    "RemoveAgentLibsTask.rollBack() Unable to delete file: " +
                    clientsdkJar.getAbsolutePath());
                status = status && false;
            }
        } catch (Exception ex) {
            Debug.log(
                    "RemoveAgentLibsTask.execute() - Unable to delete " +
                    "agent lib jars",ex);
            status = false;
        }
        return status;
    }
    
    
    public boolean rollBack(String name, IStateAccess stateAccess,
            Map properties) throws InstallException {
        
        //Nothing to rollback
        return true;
    }
    
    public LocalizedMessage getExecutionMessage(IStateAccess stateAccess,
            Map properties) {
        String libExtDir = (String)stateAccess.get(STR_KEY_WAS_LIB_EXT);
        Object[] args = { libExtDir };
        LocalizedMessage message =
                LocalizedMessage.get(LOC_TSK_MSG_REMOVE_AGENT_LIB_EXECUTE,
                STR_WAS_GROUP, args);
        return message;
    }
    
    public LocalizedMessage getRollBackMessage(IStateAccess stateAccess,
            Map properties) {
        
        String libExtDir = (String)stateAccess.get(STR_KEY_WAS_LIB_EXT);
        Object[] args = { libExtDir };
        LocalizedMessage message =
                LocalizedMessage.get(LOC_TSK_MSG_REMOVE_AGENT_LIB_ROLLBACK,
                STR_WAS_GROUP, args);
        return message;
    }
    
    public static final String LOC_TSK_MSG_REMOVE_AGENT_LIB_EXECUTE =
            "TSK_MSG_REMOVE_AGENT_LIB_EXECUTE";
    public static final String LOC_TSK_MSG_REMOVE_AGENT_LIB_ROLLBACK =
            "TSK_MSG_REMOVE_AGENT_LIB_ROLLBACK";
}
