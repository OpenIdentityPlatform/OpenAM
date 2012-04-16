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
 * $Id: CopyMbeansTask.java,v 1.2 2008/06/25 05:52:21 qcheng Exp $
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
import com.sun.identity.install.tools.util.ConfigUtil;
import com.sun.identity.install.tools.util.FileUtils;

/**
 * This task copies Agent's mbean jar file into Weblogic's mbeantypes 
 * directory.
 */
public class CopyMbeansTask implements ITask, IConfigKeys {
    
    public boolean execute(String name, IStateAccess stateAccess,
            Map properties) throws InstallException {
        boolean status = true;
        String srcDir = ConfigUtil.getLibPath();
        String mbeansDir = getMbeanTypesDir(stateAccess);
        
        try {
            FileUtils.copyJarFile(srcDir, mbeansDir, STR_MBEANS_JAR_FILE);
            Debug.log("CopyMbeansTask.execute() - copy " +
                    STR_MBEANS_JAR_FILE +
                    " from " + srcDir + " to " + mbeansDir);
        } catch (Exception e) {
            Debug.log(
                    "CopyMbeansTask.execute() - Error occured while copying " +
                    STR_MBEANS_JAR_FILE + " from " +
                    srcDir + " to " + mbeansDir);
            status = false;
        }
        
        return status;
    }
    
    
    public boolean rollBack(String name, IStateAccess stateAccess,
            Map properties) throws InstallException {
        boolean status = true;
        
        // Delete the file amauthprovider.jar
        String mbeansDir = getMbeanTypesDir(stateAccess);
        File file = new File(mbeansDir, STR_MBEANS_JAR_FILE);
        Debug.log("CopyMbeansTask.rollBack () - " + mbeansDir);
        
        if (!file.delete()) {
            Debug.log("CopyMbeansTask.rollBack() Unable to delete file: " +
                    file.getAbsolutePath());
            status = false;
        }
        
        return status;
    }
    
    public LocalizedMessage getExecutionMessage(IStateAccess stateAccess,
            Map properties) {
        String mbeansDir = getMbeanTypesDir(stateAccess);
        Object[] args = { mbeansDir };
        LocalizedMessage message =
                LocalizedMessage.get(LOC_TSK_MSG_COPY_MBEANS_EXECUTE,
                STR_WL_GROUP, args);
        
        return message;
    }
    
    public LocalizedMessage getRollBackMessage(IStateAccess stateAccess,
            Map properties) {
        String mbeansDir = getMbeanTypesDir(stateAccess);
        Object[] args = { mbeansDir };
        LocalizedMessage message =
                LocalizedMessage.get(LOC_TSK_MSG_COPY_MBEANS_ROLLBACK,
                STR_WL_GROUP, args);
        
        return message;
    }
    
    private String getMbeanTypesDir(IStateAccess stateAccess) {
        String result = "";
        
        String mbeansDir = (String)stateAccess.get(STR_KEY_WEBLOGIC_MBEANS_DIR);
        if (mbeansDir != null && mbeansDir.length() > 0) {
            result = mbeansDir;
        } else {
            String wlhomeDir =
                    (String) stateAccess.get(STR_KEY_WEBLOGIC_HOME_DIR);
            result = wlhomeDir + STR_FORWARD_SLASH + "server" +
                    STR_FORWARD_SLASH + "lib" +
                    STR_FORWARD_SLASH + "mbeantypes";
            // Update state information
            stateAccess.put(STR_KEY_WEBLOGIC_MBEANS_DIR, result);
        }
        
        return result;
    }
    
    public static final String LOC_TSK_MSG_COPY_MBEANS_EXECUTE =
            "TSK_MSG_COPY_MBEANS_EXECUTE";
    public static final String LOC_TSK_MSG_COPY_MBEANS_ROLLBACK =
            "TSK_MSG_COPY_MBEANS_ROLLBACK";
}
