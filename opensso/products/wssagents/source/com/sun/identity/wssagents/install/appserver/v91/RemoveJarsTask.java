/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: RemoveJarsTask.java,v 1.1 2009/06/12 22:03:03 huacui Exp $
 *
 */

package com.sun.identity.agents.install.appserver.v91;

import java.io.File;
import java.util.Map;

import com.sun.identity.agents.install.appserver.IConfigKeys;
import com.sun.identity.install.tools.configurator.ITask;
import com.sun.identity.install.tools.configurator.IStateAccess;
import com.sun.identity.install.tools.configurator.InstallConstants;
import com.sun.identity.install.tools.configurator.InstallException;
import com.sun.identity.install.tools.util.LocalizedMessage;
import com.sun.identity.install.tools.util.Debug;

/**
 * This task remove agent web service jar files from AS lib directory
 * during uninstall.
 */
public class RemoveJarsTask implements ITask, InstallConstants, IConfigKeys, IConstants {
    
    public boolean execute(String name, IStateAccess stateAccess,
            Map properties) throws InstallException {

/*
        String libDir = getASLibDir(stateAccess);
        if (removeFile(libDir, STR_WS_RT_JAR_FILE)) {
            if (removeFile(libDir, STR_WS_TOOLS_JAR_FILE)) {
                return removeFile(libDir + FILE_SEP + STR_AS_ENDORSED_DIR, 
                                  STR_WS_API_JAR_FILE);
            }
        }
        return false;
*/
        return true;
    }

    private boolean removeFile(String tgtDir, String fileName)
        throws InstallException {
        boolean status = true;

        // Delete the jar file
        File file = new File(tgtDir, fileName);
        Debug.log("RemoveJarsTask.execute() - " + tgtDir);

        if (!file.delete()) {
            Debug.log("RemoveJarsTask.execute() Unable to delete file: " +
                    file.getAbsolutePath());
            status = false;
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
        String libDir = getASLibDir(stateAccess);
        Object[] args = { libDir };
        LocalizedMessage message =
                LocalizedMessage.get(
                LOC_TSK_MSG_REMOVE_JARS_EXECUTE, STR_AS_GROUP, args);
        
        return message;
    }
    
    public LocalizedMessage getRollBackMessage(IStateAccess stateAccess,
            Map properties) {
        // No roll back during un-install
        return null;
    }
    
    private String getASLibDir(IStateAccess stateAccess) {
        String result = "";
        
        String asHomeDir = (String) stateAccess.get(STR_KEY_AS_INSTALL_DIR);
        String osName = System.getProperty(STR_OS_NAME_PROPERTY);
        if (osName.toLowerCase().startsWith(STR_WINDOWS)) {
            result = asHomeDir + "\\lib\\";
        } else {
            result = asHomeDir + "/lib";
        }
        
        return result;
    }
    
    public static final String LOC_TSK_MSG_REMOVE_JARS_EXECUTE =
            "TSK_MSG_REMOVE_JARS_EXECUTE";
}
