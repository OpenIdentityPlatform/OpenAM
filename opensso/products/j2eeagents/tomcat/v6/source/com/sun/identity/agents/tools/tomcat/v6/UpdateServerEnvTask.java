/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010 ForgeRock AS. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 */

package com.sun.identity.agents.tools.tomcat.v6;

import java.io.File;
import java.util.Map;

import com.sun.identity.install.tools.configurator.IStateAccess;
import com.sun.identity.install.tools.configurator.ITask;
import com.sun.identity.install.tools.configurator.InstallException;
import com.sun.identity.install.tools.util.ConfigUtil;
import com.sun.identity.install.tools.util.Debug;
import com.sun.identity.install.tools.util.LocalizedMessage;
import com.sun.identity.install.tools.util.FileUtils;

/**
 *
 * @author Steve Ferris steve.ferris@forgerock.com
 */
public class UpdateServerEnvTask extends UpdateServerEnvBase implements
		ITask {
    public static final String LOC_TSK_MSG_UPDATE_SET_ENV_SCRIPT_EXECUTE
                    = "TSK_MSG_UPDATE_SET_ENV_SCRIPT_EXECUTE";

    public static final String LOC_TSK_MSG_UPDATE_SET_ENV_SCRIPT_ROLLBACK
                    = "TSK_MSG_UPDATE_SET_ENV_SCRIPT_ROLLBACK";

    public boolean execute(String name, IStateAccess stateAccess, Map properties)
    throws InstallException {
        boolean status = true;

        try {
            getAgentConfigLocation(stateAccess);
            super.getSetenvScriptFile(stateAccess);

            status = status && updateSetenvScript(stateAccess);
        } catch (Exception ex) {
            status = false;
            Debug.log("UpdateServerEnvTask.execute() - encountered exception " +
                      ex.getMessage(), ex);
        }

        return status;
    }

    private boolean updateSetenvScript(IStateAccess stateAccess) {
        boolean status = true;
        int index = -1;

        String addLine = super.constructAddJVMOptionString();

        File setenvScript = new File(_setenvFile);

        if (!setenvScript.exists()) {
            status = FileUtils.appendLinesToFile(_setenvFile, new String[] { addLine }, true);
            Debug.log("UpdateServerEnvTask.updateSetenvScript(): " +
                          "writing " + addLine + " to new file " + _setenvFile);
        } else {
            if (FileUtils.getFirstOccurence(_setenvFile, addLine, true,
				false, true, 0) == -1) {
                status = FileUtils.appendLinesToFile(_setenvFile, new String[] { addLine });
                Debug.log("UpdateServerEnvTask.updateSetenvScript(): " +
                          "writing " + addLine + " to " + _setenvFile);
            } else {
                Debug.log("UpdateServerEnvTask.updateSetenvScript(): " +
                          "agent JVM option already present");
            }
        }
        
        return status;
    }



    public LocalizedMessage getExecutionMessage(IStateAccess stateAccess,
                    Map properties) {
        super.getSetenvScriptFile(stateAccess);
        Object[] args = { _setenvFile };
        LocalizedMessage message = LocalizedMessage.get(
                        LOC_TSK_MSG_UPDATE_SET_ENV_SCRIPT_EXECUTE,
                        STR_TOMCAT_GROUP, args);

        return message;
    }

    public LocalizedMessage getRollBackMessage(IStateAccess stateAccess,
                    Map properties) {
        Object[] args = { _setenvFile };
        LocalizedMessage message = LocalizedMessage.get(
                        LOC_TSK_MSG_UPDATE_SET_ENV_SCRIPT_ROLLBACK,
                        STR_TOMCAT_GROUP, args);
        return message;
    }

    public boolean rollBack(String name, IStateAccess stateAccess,
                    Map properties) throws InstallException {
            boolean status = false;
        status = super.unconfigureServerEnv(stateAccess);
        return status;
    }
}
