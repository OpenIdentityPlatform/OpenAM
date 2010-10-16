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
 * $Id: CopyJarsTask.java,v 1.1 2009/06/12 22:03:03 huacui Exp $
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
import com.sun.identity.install.tools.util.ConfigUtil;
import com.sun.identity.install.tools.util.FileUtils;

/**
 * This task copies web services agent jar files into appserver lib 
 * directory.
 */
public class CopyJarsTask implements ITask, InstallConstants, IConfigKeys, IConstants {
    
    public boolean execute(String name, IStateAccess stateAccess,
            Map properties) throws InstallException {
        boolean status = false;
        String srcDir = ConfigUtil.getLibPath();
        String tgtDir = getASLibDir(stateAccess);
        // copy webservices-rt.jar to AS lib
        if (copyFile(srcDir, tgtDir, STR_WS_RT_JAR_FILE)) {
            // copy webservices-tools.jar to AS lib
            if (copyFile(srcDir, tgtDir, STR_WS_TOOLS_JAR_FILE)) {
                // copy webservices-tools.jar to AS lib/endorsed
                if (copyFile(srcDir, 
                             tgtDir + FILE_SEP + STR_AS_ENDORSED_DIR,
                             STR_WS_API_JAR_FILE)) {
                    // copy the AMConfig.properties to AS addons/opensso
                    srcDir = (String)stateAccess.get(
                              STR_CONFIG_DIR_PREFIX_TAG);
                    tgtDir = stateAccess.get(STR_KEY_AS_INSTALL_DIR) +
                     FILE_SEP + STR_ADDONS_DIR + FILE_SEP + STR_OPENSSO;
                    try {
                        File toDir = new File(tgtDir);
                        if (!toDir.isDirectory()) {
                            // create addons/opensso if not exists
                            toDir.mkdirs();
                        }
                        if (copyFile(srcDir, tgtDir, STR_AMCONFIG_FILE)) {
                            File fromDir = new File(ConfigUtil.getLibPath());
                            FileUtils.copyDirContents(fromDir, toDir);
                            status = true;      
                        }
                    } catch (Exception e) {
                        status = false;      
                    }
                }
            }
        }
        return status;
    }

    private boolean copyFile(String srcDir, String tgtDir, String fileName) 
        throws InstallException {
        boolean status = true;
        
        try {
            FileUtils.copyFile(srcDir + FILE_SEP + fileName, 
                               tgtDir + FILE_SEP + fileName);
            Debug.log("CopyJarsTask.execute() - copy " +
                    fileName +
                    " from " + srcDir + " to " + tgtDir);
        } catch (Exception e) {
            Debug.log(
                    "CopyJarsTask.execute() - Error occured while copying " +
                    fileName + " from " +
                    srcDir + " to " + tgtDir);
            status = false;
        }
        
        return status;
    }
    
    
    public boolean rollBack(String name, IStateAccess stateAccess,
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
*/      return true;
    }

    private boolean removeFile(String tgtDir, String fileName) 
        throws InstallException {
        boolean status = true;
        
        // Delete the web service jar file
        File file = new File(tgtDir, fileName);
        Debug.log("CopyJarsTask.rollBack () - " + tgtDir);
        
        if (!file.delete()) {
            Debug.log("CopyJarsTask.rollBack() Unable to delete file: " +
                    file.getAbsolutePath());
            status = false;
        }
        
        return status;
    }
    
    public LocalizedMessage getExecutionMessage(IStateAccess stateAccess,
            Map properties) {
        String libDir = getASLibDir(stateAccess);
        Object[] args = { libDir };
        LocalizedMessage message =
                LocalizedMessage.get(LOC_TSK_MSG_COPY_JARS_EXECUTE,
                STR_AS_GROUP, args);
        
        return message;
    }
    
    public LocalizedMessage getRollBackMessage(IStateAccess stateAccess,
            Map properties) {
        String libDir = getASLibDir(stateAccess);
        Object[] args = { libDir };
        LocalizedMessage message =
                LocalizedMessage.get(LOC_TSK_MSG_COPY_JARS_ROLLBACK,
                STR_AS_GROUP, args);
        
        return message;
    }
    
    private String getASLibDir(IStateAccess stateAccess) {
        String result = "";
        
        String libDir = (String)stateAccess.get(STR_KEY_AS_LIB_DIR);
        if (libDir != null && libDir.length() > 0) {
            result = libDir;
        } else {
            String asHomeDir =
                    (String) stateAccess.get(STR_KEY_AS_INSTALL_DIR);
            result = asHomeDir + FILE_SEP + STR_AS_LIB_DIR;
            // Update state information
            stateAccess.put(STR_KEY_AS_LIB_DIR, result);
        }
        
        return result;
    }
    
    public static final String LOC_TSK_MSG_COPY_JARS_EXECUTE =
            "TSK_MSG_COPY_JARS_EXECUTE";
    public static final String LOC_TSK_MSG_COPY_JARS_ROLLBACK =
            "TSK_MSG_COPY_JARS_ROLLBACK";
}
