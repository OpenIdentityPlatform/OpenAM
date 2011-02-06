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
 * $Id: CreateLayoutTask.java,v 1.3 2008/06/25 05:51:18 qcheng Exp $
 *
 */

package com.sun.identity.install.tools.configurator;

import java.io.File;
import java.util.Map;

import com.sun.identity.install.tools.util.ConfigUtil;
import com.sun.identity.install.tools.util.Debug;
import com.sun.identity.install.tools.util.FileUtils;
import com.sun.identity.install.tools.util.LocalizedMessage;
import com.sun.identity.install.tools.admin.ToolsConfiguration;

public class CreateLayoutTask implements ITask, InstallConstants {

    public boolean execute(String name, IStateAccess stateAccess, 
            Map properties) throws InstallException {
        String productHome = ConfigUtil.getHomePath();
        String instanceName = stateAccess.getInstanceName();

        Debug.log("LayoutHandlerTask.execute() - Creating instance "
                + "directory layout for '" + instanceName);

        // Populate the directory paths
        String instanceDirPath = productHome + FILE_SEP + instanceName;
        String logsDirPath = instanceDirPath + FILE_SEP
                + INSTANCE_LOGS_DIR_NAME;
        String baseConfigDirPath = productHome +  FILE_SEP +
	        INSTANCE_CONFIG_DIR_NAME;
        String configDirPath = instanceDirPath + FILE_SEP
                + INSTANCE_CONFIG_DIR_NAME;
        String debugLogsDirPath = logsDirPath + FILE_SEP
                + INSTANCE_DEBUG_DIR_NAME;
        String auditLogsDirPath = logsDirPath + FILE_SEP
                + INSTANCE_AUDIT_DIR_NAME;

        createDir(instanceDirPath);
        createDir(configDirPath);
        createDir(logsDirPath);
        createDir(debugLogsDirPath);
        createDir(auditLogsDirPath);

        // Add related tokens - which are related to this Layout class
        stateAccess.put(STR_AUDIT_DIR_PREFIX_TAG, auditLogsDirPath);
        stateAccess.put(STR_CONFIG_DIR_PREFIX_TAG, configDirPath);
        stateAccess.put(STR_DEBUG_DIR_PREFIX_TAG, debugLogsDirPath);
        stateAccess.put(STR_DEBUG_LEVEL_TAG, STR_DEBUG_LEVEL_DEFAULT_VALUE);
        stateAccess.put(STR_LOG_CONFIG_FILE_PATH, baseConfigDirPath + FILE_SEP +
            STR_LOG_CONFIG_FILENAME);

        // All the operations should succeed. If one of them does not then an
        // Exception is thrown as it would be a Fatal Exception. Hence this 
        // the method always returns a true.        
        return true;
    }

    public boolean rollBack(String name, IStateAccess stateAccess,
            Map properties) throws InstallException {
        String productHome = ConfigUtil.getHomePath();
        String instanceName = stateAccess.getInstanceName();

        Debug.log("LayoutHandlerTask.rollBack() - Deleting instance "
                + "directory '" + instanceName + "' and its contents");

        // Populate the directory paths
        String instanceDirPath = productHome + FILE_SEP + instanceName;
        File instanceDir = new File(instanceDirPath);

        return FileUtils.removeDir(instanceDir);
    }

    public LocalizedMessage getExecutionMessage(IStateAccess stateAccess,
            Map properties) {
        Object[] args = { ToolsConfiguration.getProductShortName(),
                stateAccess.getInstanceName() };

        LocalizedMessage message = LocalizedMessage.get(
                LOC_TSK_MSG_CREATE_LAYOUT_EXECUTE, args);
        return message;
    }

    public LocalizedMessage getRollBackMessage(IStateAccess stateAccess,
            Map properties) {
        Object[] args = { stateAccess.getInstanceName() };
        LocalizedMessage message = LocalizedMessage.get(
                LOC_TSK_MSG_DELETE_LAYOUT_ROLL_BACK, args);
        return message;
    }

    private void createDir(String dirName) throws InstallException {
        Debug.log("LayoutHandlerTask.createDir() - Creating Dir for: "
                + dirName);
        File dir = new File(dirName);
        if (!dir.mkdir()) {
            Debug.log("LayoutHandlerTask.createDir() - Error Unable to "
                    + "create Dir for: " + dirName);
            // If the creation is not successful throw an Exception
            LocalizedMessage lMessage = LocalizedMessage.get(
                    LOC_DR_ERR_DIR_CREATE, new Object[] { dirName });
            throw new InstallException(lMessage);
        }
    }

    /** Field LOC_DR_ERR_DIR_CREATE **/
    public static final String LOC_DR_ERR_DIR_CREATE = "DR_ERR_DIR_CREATE";

    public static final String LOC_TSK_MSG_CREATE_LAYOUT_EXECUTE = 
        "TSK_MSG_CREATE_LAYOUT_EXECUTE";

    public static final String LOC_TSK_MSG_DELETE_LAYOUT_ROLL_BACK = 
        "TSK_MSG_CREATE_LAYOUT_ROLLBACK";

    public static final String STR_DEBUG_LEVEL_TAG = "DEBUG_LEVEL";

    public static final String STR_DEBUG_LEVEL_DEFAULT_VALUE = "error";

}
