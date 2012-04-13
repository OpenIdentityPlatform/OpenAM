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
 */
package com.sun.identity.agents.install.appserver.v81;

import com.sun.identity.agents.install.appserver.AgentFilesBase;
import com.sun.identity.install.tools.configurator.IStateAccess;
import com.sun.identity.install.tools.configurator.ITask;
import com.sun.identity.install.tools.configurator.InstallException;
import com.sun.identity.install.tools.util.Debug;
import com.sun.identity.install.tools.util.FileUtils;
import com.sun.identity.install.tools.util.LocalizedMessage;
import java.io.File;
import java.util.Map;
import org.forgerock.openam.agents.install.appserver.VersionChecker;

/**
 *
 * @author Peter Major
 */
public class CopyAgentFilesTask extends AgentFilesBase implements ITask {

    public static final String LOC_TSK_MSG_COPY_AGENT_FILES_EXECUTE =
            "TSK_MSG_COPY_AGENT_FILES_EXECUTE";
    public static final String LOC_TSK_MSG_COPY_AGENT_FILES_ROLLBACK =
            "TSK_MSG_COPY_AGENT_FILES_ROLLBACK";

    public boolean execute(String name, IStateAccess stateAccess, Map properties)
            throws InstallException {
        boolean status = true;

        try {
            getAgentConfigLocation(stateAccess);
            if (VersionChecker.isGlassFishv3(stateAccess)) {
                status &= copyAgentJarFiles();
                status &= copyAgentLocaleFiles();
            }
        } catch (Exception ex) {
            status = false;
            Debug.log("CopyAgentFilesTask.execute() - encountered exception"
                    + ex.getMessage(), ex);
        }

        return status;
    }

    private boolean copyAgentJarFiles() {
        boolean status = true;

        try {
            for (int i = 0; i < libJarFiles.length; i++) {
                FileUtils.copyJarFile(agentLibPath, asLibDir, libJarFiles[i]);
                Debug.log("CopyAgentFilesTask.copyAgentJarFiles() - copy "
                        + libJarFiles[i] + " from " + agentLibPath + " to " + asLibDir);
            }
        } catch (Exception ex) {
            Debug.log("CopyAgentFilesTask.copyAgentJarFiles() - "
                    + "Error occured while copying jar files from " + agentLibPath
                    + " to " + asLibDir + ": " + ex.getMessage(), ex);
            status = false;
        }

        return status;
    }

    private boolean copyAgentLocaleFiles() {
        boolean status = true;

        try {
            for (File localeFile : listAgentLocaleFiles()) {
                FileUtils.copyFile(agentLocalePath + System.getProperty("file.separator") + localeFile.getName(),
                        asClassesDir + System.getProperty("file.separator") + localeFile.getName());
                Debug.log("CopyAgentFilesTask.copyAgentLocaleFiles() - copy "
                        + localeFile.getName() + " from " + agentLocalePath + " to " + asClassesDir);
            }
        } catch (Exception ex) {
            Debug.log("CopyAgentFilesTask.copyAgentLocaleFiles() - "
                    + "Error occured while copying locale files from " + agentLocalePath
                    + " to " + asClassesDir + ": " + ex.getMessage(), ex);
            status = false;
        }

        return status;
    }

    public LocalizedMessage getExecutionMessage(IStateAccess stateAccess,
            Map properties) {
        LocalizedMessage message = LocalizedMessage.get(
                LOC_TSK_MSG_COPY_AGENT_FILES_EXECUTE,
                IConstants.STR_AS_GROUP, null);

        return message;
    }

    public LocalizedMessage getRollBackMessage(IStateAccess stateAccess,
            Map properties) {
        LocalizedMessage message = LocalizedMessage.get(
                LOC_TSK_MSG_COPY_AGENT_FILES_ROLLBACK,
                IConstants.STR_AS_GROUP, null);
        return message;
    }

    public boolean rollBack(String name, IStateAccess stateAccess,
            Map properties) throws InstallException {
        boolean status = false;
        status = removeAgentFiles(stateAccess);
        return status;
    }
}
