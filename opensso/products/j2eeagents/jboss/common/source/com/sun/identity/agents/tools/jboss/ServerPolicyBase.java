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
 * $Id: ServerPolicyBase.java,v 1.1 2008/12/11 14:36:06 naghaon Exp $
 *
 */

package com.sun.identity.agents.tools.jboss;

import com.sun.identity.install.tools.configurator.IStateAccess;
import com.sun.identity.install.tools.configurator.InstallConstants;
import com.sun.identity.install.tools.util.ConfigUtil;
import com.sun.identity.install.tools.util.Debug;
import com.sun.identity.install.tools.util.FileEditor;
import com.sun.identity.install.tools.util.FileUtils;
import com.sun.identity.install.tools.util.DeletePattern;

/**
 * Adds java permissions to server.policy for agent's codebase
 *
 */
public class ServerPolicyBase implements InstallConstants,
        IConfigKeys, IConstants {
    
    public boolean addToServerPolicy(IStateAccess stateAccess) {
        boolean status = false;
        String serverPolicyFile = getServerPolicyFile(stateAccess);

        String homeDir = ConfigUtil.getHomePath();
        String agentLibPath = ConfigUtil.getLibPath();
        String agentLocaleDir = ConfigUtil.getLocaleDirPath();

        String instanceName = stateAccess.getInstanceName();
        StringBuffer confSb = new StringBuffer();
        confSb.append(homeDir).append(STR_FORWARD_SLASH);
        confSb.append(instanceName).append(STR_FORWARD_SLASH);
        confSb.append(STR_INSTANCE_CONFIG_DIR_NAME);
        String agentInstanceConfigDirPath = confSb.toString();

        StringBuffer logsSb = new StringBuffer();
        logsSb.append(homeDir).append(STR_FORWARD_SLASH);
        logsSb.append(instanceName).append(STR_FORWARD_SLASH);
        logsSb.append(STR_INSTANCE_LOGS_DIR_NAME);
        String agentInstanceLogDirPath = logsSb.toString();


        StringBuffer permsSb = new StringBuffer();
        permsSb.append(LINE_SEP);
        permsSb.append("grant codeBase \"file:").append(agentLibPath);
        permsSb.append("/-\" {").append(LINE_SEP);
        permsSb.append("       permission java.lang.RuntimePermission " +
            "\"accessClassInPackage.org.apache.*\";");
        permsSb.append(LINE_SEP);
        permsSb.append("       permission java.lang.RuntimePermission " +
            "\"getClassLoader\";");
        permsSb.append(LINE_SEP);
        permsSb.append("       permission java.lang.RuntimePermission " +
            "\"shutdownHooks\";");
        permsSb.append(LINE_SEP);
        permsSb.append("       permission java.io.FilePermission " + 
            "\""+agentInstanceConfigDirPath + "/-\", \"read\";");
        permsSb.append(LINE_SEP);
        permsSb.append("       permission java.util.PropertyPermission " +
            "\"*\", \"read,write\";");
        permsSb.append(LINE_SEP);
        permsSb.append("       permission java.io.FilePermission " + 
            "\"" + agentLocaleDir + "/-\", \"read\";");
        permsSb.append(LINE_SEP);
        permsSb.append("       permission java.io.FilePermission " + 
            "\""+ agentInstanceLogDirPath + "/-\", \"read,write\";");
        permsSb.append(LINE_SEP);
        permsSb.append("       permission java.net.SocketPermission  " +
            " \"*\",  \"connect,resolve\";");
        permsSb.append(LINE_SEP);
        permsSb.append("       permission java.util.logging.LoggingPermission " +
            "\"control\";");
        permsSb.append(LINE_SEP);
        permsSb.append("       permission java.io.FilePermission " +
            "\"null/serverconfig.xml\", \"read\";");
        permsSb.append(LINE_SEP);
        permsSb.append("};");

        try {
            FileUtils.appendDataToFile(serverPolicyFile, permsSb.toString());
            status = true;
        } catch (Exception e) {
            Debug.log("ServerPolicyBase.addToServerPolicy() - Error " +
                    "occurred while adding Agent Realm to '" + serverPolicyFile +
                    "'. ", e);
        }
        
        return status;
    }
    
    public boolean removeFromServerPolicy(IStateAccess stateAccess) {
        // Remove the lines with the match patterns from the login conf file
        String serverPolicyFileName = getServerPolicyFile(stateAccess);
        FileEditor fileEditor = new FileEditor(serverPolicyFileName);
        
        boolean status = false;
        try {
            DeletePattern pattern = new DeletePattern(ConfigUtil.getLibPath(),
                    DeletePattern.INT_MATCH_OCCURRANCE, 11);
            status = fileEditor.deleteLines(pattern);
        } catch (Exception e) {
            Debug.log("ServerPolicyBase.removeFromServerPolicy() - " +
                    "Exception occurred while removing the Agent Realm from " +
                    "file '" + serverPolicyFileName + "'. ", e);
        }
        return status;
    }
    
    public String getServerPolicyFile(IStateAccess stateAccess) {
        return (String) stateAccess.get(STR_KEY_JB_SERVER_POLICY_FILE);
    }
    
    
}
