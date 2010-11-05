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
 * $Id: BackupFileTask.java,v 1.2 2008/06/25 05:51:17 qcheng Exp $
 *
 */

package com.sun.identity.install.tools.configurator;

import java.util.Map;

import com.sun.identity.install.tools.admin.ToolsConfiguration;
import com.sun.identity.install.tools.util.Debug;
import com.sun.identity.install.tools.util.FileUtils;
import com.sun.identity.install.tools.util.LocalizedMessage;

public class BackupFileTask implements ITask, InstallConstants {

    public boolean execute(String name, IStateAccess stateAccess, 
            Map properties) throws InstallException {
        boolean status = false;
        String fileName = getFileName(stateAccess, properties);
        if (fileName != null && fileName.length() > 0) {
            try {
                FileUtils.backupFile(fileName, STR_BACK_UP_FILE_SUFFIX);
                status = true;
            } catch (Exception e) {
                Debug.log("BackupFileTask.execute() - Error occurred "
                        + "while creating a back up for file: '" + fileName
                        + "'.");
            }
        }

        return status;
    }

    public LocalizedMessage getExecutionMessage(IStateAccess stateAccess,
            Map properties) {
        String fileName = getFileName(stateAccess, properties);
        Object[] args = { fileName };
        LocalizedMessage message = LocalizedMessage.get(
                LOC_TSK_MSG_BACKUP_FILE_EXECUTE, args);
        return message;
    }

    public LocalizedMessage getRollBackMessage(IStateAccess stateAccess,
            Map properties) {
        String fileName = getFileName(stateAccess, properties);
        Object[] args = { fileName };
        LocalizedMessage message = LocalizedMessage.get(
                LOC_TSK_MSG_BACKUP_FILE_ROLLBACK, args);
        return message;
    }

    public boolean rollBack(String name, IStateAccess state, Map properties)
            throws InstallException {
        // Nothing to do. We don't delete the backup files which were created.
        return true;
    }

    private String getFileName(IStateAccess stateAccess, Map properties) {
        String fileName = null;

        String fileNameKey = (String) properties
                .get(STR_BACK_UP_FILE_LOOKUP_KEY);
        if (fileNameKey != null && fileNameKey.length() > 0) {
            fileName = (String) stateAccess.get(fileNameKey);
        }

        return fileName;
    }

    public static final String STR_BACK_UP_FILE_SUFFIX = "-preAm"
            + ToolsConfiguration.getProductShortName();

    public static final String STR_BACK_UP_FILE_LOOKUP_KEY = 
        "BACK_UP_FILE_LOOKUP_KEY";

    public static final String LOC_TSK_MSG_BACKUP_FILE_EXECUTE = 
        "TSK_MSG_BACKUP_FILE_EXECUTE";

    public static final String LOC_TSK_MSG_BACKUP_FILE_ROLLBACK = 
        "TSK_MSG_BACKUP_FILE_ROLLBACK";
}
