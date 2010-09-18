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
 * $Id: DeleteLayoutTask.java,v 1.2 2008/06/25 05:51:18 qcheng Exp $
 *
 */

package com.sun.identity.install.tools.configurator;

import java.io.File;
import java.util.Map;

import com.sun.identity.install.tools.util.Debug;
import com.sun.identity.install.tools.util.FileUtils;
import com.sun.identity.install.tools.util.LocalizedMessage;
import com.sun.identity.install.tools.util.ConfigUtil;

public class DeleteLayoutTask implements ITask, InstallConstants {

    public boolean execute(String name, IStateAccess stateAccess, 
            Map properties) throws InstallException {
        String configDirPath = getConfigDirPath(stateAccess);
        Debug.log("DeleteLayoutTask.execute() - Deleting directory '"
                + configDirPath + "' and its contents.");

        File configDir = new File(configDirPath);

        return FileUtils.removeDir(configDir);
    }

    public LocalizedMessage getExecutionMessage(IStateAccess stateAccess,
            Map properties) {
        Object[] args = { getConfigDirPath(stateAccess) };
        LocalizedMessage message = LocalizedMessage.get(
                LOC_TSK_MSG_DELETE_LAYOUT_EXECUTE, args);
        return message;
    }

    public LocalizedMessage getRollBackMessage(IStateAccess stateAccess,
            Map properties) {
        // There is no roll back for uninstall tasks
        return null;
    }

    public boolean rollBack(String name, IStateAccess state, Map properties)
            throws InstallException {

        // Nothing to roll back during uninstall
        return true;
    }

    private String getConfigDirPath(IStateAccess stateAccess) {
        String productHome = ConfigUtil.getHomePath();
        String configDirPath = productHome + FILE_SEP
                + stateAccess.getInstanceName() + FILE_SEP
                + INSTANCE_CONFIG_DIR_NAME;
        return configDirPath;
    }

    public static final String LOC_TSK_MSG_DELETE_LAYOUT_EXECUTE = 
        "TSK_MSG_DELETE_LAYOUT_EXECUTE";
}
