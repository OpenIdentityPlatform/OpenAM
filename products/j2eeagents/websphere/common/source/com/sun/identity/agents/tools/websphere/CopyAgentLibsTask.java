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
 * $Id: CopyAgentLibsTask.java,v 1.2 2008/11/21 22:21:43 leiming Exp $
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
import com.sun.identity.install.tools.util.ConfigUtil;
import com.sun.identity.install.tools.util.FileUtils;

/**
 * This class copies agent's jars into Websphere's lib/ext directory.
 */
public class CopyAgentLibsTask implements ITask, IConfigKeys, IConstants {
    
    public boolean execute(String name, IStateAccess stateAccess,
            Map properties) throws InstallException {
        boolean status = true;
        String srcDir = ConfigUtil.getLibPath();
        String libExtDir = (String) stateAccess.get(STR_KEY_WAS_LIB_EXT);
        
        if ((libExtDir != null) && (libExtDir.length() > 0)) {
            try {
                Debug.log("CopyAgentLibsTask.execute() - copy " +
                        STR_AGENT_JAR +
                        " from " + srcDir + " to " + libExtDir);
                FileUtils.copyJarFile(srcDir, libExtDir, STR_AGENT_JAR);
                
                Debug.log("CopyAgentLibsTask.execute() - copy " +
                        STR_OPENSSOCLIENTSDK_JAR +
                        " from " + srcDir + " to " + libExtDir);
                FileUtils.copyJarFile(srcDir, 
                        libExtDir, STR_OPENSSOCLIENTSDK_JAR);
                
            } catch (Exception e) {
                Debug.log("CopyAgentLibsTask.execute() - " +
                        "Error occured while copying " +
                        "agent jars from " + srcDir + " to " + libExtDir);
                status = false;
            }
        } else {
            Debug.log("CopyAgentLibsTask.execute() - " +
                    "Error occured while copying " +
                    "agent jar files, invalid dest directory =" + libExtDir);
            status = false;
        }
        return status;
    }
    
    public boolean rollBack(String name, IStateAccess stateAccess,
            Map properties) throws InstallException {
        
        boolean status = true;
        try {
            // Delete agent.jar and openssoclientsdk.jar
            String libExtDir = (String) stateAccess.get(STR_KEY_WAS_LIB_EXT);
            File agentJar = new File(libExtDir, STR_AGENT_JAR);
            File clientsdkJar = new File(libExtDir, STR_AGENT_JAR);
            Debug.log("CopyAgentLibsTask.rollBack () - " + libExtDir);
            
            if (!agentJar.delete()) {
                Debug.log("CopyAgentLibsTask.rollBack() " +
                        "Unable to delete file: " +
                        agentJar.getAbsolutePath());
                status = false;
            }
            
            if (!clientsdkJar.delete()) {
                Debug.log("CopyAgentLibsTask.rollBack() " +
                        "Unable to delete file: " +
                        clientsdkJar.getAbsolutePath());
                status = false;
            }
        } catch (Exception ex) {
            Debug.log("CopyAgentLibsTask.rollBack() - Unable to delete " +
                    "agent lib jars", ex);
            status = false;
        }
        return status;
    }
    
    public LocalizedMessage getExecutionMessage(IStateAccess stateAccess,
            Map properties) {
        
        String libExtDir = (String) stateAccess.get(STR_KEY_WAS_LIB_EXT);
        Object[] args = {libExtDir};
        LocalizedMessage message =
                LocalizedMessage.get(LOC_TSK_MSG_COPY_AGENT_LIB_EXECUTE,
                STR_WAS_GROUP, args);
        return message;
    }
    
    public LocalizedMessage getRollBackMessage(IStateAccess stateAccess,
            Map properties) {
        String libExtDir = (String) stateAccess.get(STR_KEY_WAS_LIB_EXT);
        Object[] args = {libExtDir};
        LocalizedMessage message =
                LocalizedMessage.get(LOC_TSK_MSG_COPY_AGENT_LIB_ROLLBACK,
                STR_WAS_GROUP, args);
        return message;
    }
    public static final String LOC_TSK_MSG_COPY_AGENT_LIB_EXECUTE =
            "TSK_MSG_COPY_AGENT_LIB_EXECUTE";
    public static final String LOC_TSK_MSG_COPY_AGENT_LIB_ROLLBACK =
            "TSK_MSG_COPY_AGENT_LIB_ROLLBACK";
}
