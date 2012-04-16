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

import com.sun.identity.install.tools.configurator.IStateAccess;
import com.sun.identity.install.tools.configurator.ITask;
import com.sun.identity.install.tools.configurator.InstallException;
import com.sun.identity.install.tools.util.ConfigUtil;
import com.sun.identity.install.tools.util.Debug;
import com.sun.identity.install.tools.util.LocalizedMessage;
import com.sun.identity.install.tools.util.FileUtils;
import java.io.File;
import java.util.Collection;
import java.util.Map;

/**
 *
 * @author Steve Ferris steve.ferris@forgerock.com
 */
public class CopyAgentFilesTask extends AgentFilesBase implements ITask {
    public static final String LOC_TSK_MSG_COPY_AGENT_FILES_SCRIPT_EXECUTE
                    = "TSK_MSG_COPY_AGENT_FILES_SCRIPT_EXECUTE";

    public static final String LOC_TSK_MSG_COPY_AGENT_FILES_SCRIPT_ROLLBACK
                    = "TSK_MSG_COPY_AGENT_FILES_ROLLBACK";

    public boolean execute(String name, IStateAccess stateAccess, Map properties)
    throws InstallException {
        boolean status = true;

        try {
            super.getAgentConfigLocation(stateAccess);
            status = status && copyAgentJarFiles();
            status = status && copyAgentLocaleFiles();
        } catch (Exception ex) {
            status = false;
            Debug.log("CopyAgentFilesTask.execute() - encountered exception " +
                      ex.getMessage(), ex);
        }

        return status;
    }

    private boolean copyAgentJarFiles() {
        boolean status = true;
        String srcDir = _agentLibPath;
        String destDir = _catalinaHomeDir + STR_FORWARD_SLASH + STR_TOMCAT_COMMON_LIB;

        try {
            for (int i = 0; i < libJarFiles.length; i++) {
                FileUtils.copyJarFile(srcDir, destDir, libJarFiles[i]);
                Debug.log("CopyAgentFilesTask.copyAgentJarFiles() - copy " +
                           libJarFiles[i] + " from " + srcDir + " to " + destDir);
            }
        } catch (Exception ex) {
            Debug.log("CopyAgentFilesTask.copyAgentJarFiles() - " +
                        "Error occured while copying jar files from " + srcDir +
                        " to " + destDir + ": " + ex.getMessage());
            status = false;
        }

        return status;
    }

    private boolean copyAgentLocaleFiles() {
        boolean status = true;
        String srcDir = _agentLocalePath;
        String destDir = _catalinaHomeDir + STR_FORWARD_SLASH + STR_TOMCAT_COMMON_LIB;

        try {
            for (File localeFile : listAgentLocaleFiles()) {
                FileUtils.copyFile(srcDir + System.getProperty("file.separator") + localeFile.getName(),
                        destDir + System.getProperty("file.separator") + localeFile.getName());
                Debug.log("CopyAgentFilesTask.copyAgentLocaleFiles() - copy " +
                           localeFile.getName() + " from " + srcDir + " to " + destDir);
            }
        } catch (Exception ex) {
            Debug.log("CopyAgentFilesTask.copyAgentLocaleFiles() - " +
                        "Error occured while copying locale files from " + srcDir +
                        " to " + destDir + ": " + ex.getMessage());
            status = false;
        }

        return status;
    }

    public LocalizedMessage getExecutionMessage(IStateAccess stateAccess,
                    Map properties) {
        super.getAgentConfigLocation(stateAccess);
        LocalizedMessage message = LocalizedMessage.get(
                        LOC_TSK_MSG_COPY_AGENT_FILES_SCRIPT_EXECUTE,
                        STR_TOMCAT_GROUP, null);

        return message;
    }

    public LocalizedMessage getRollBackMessage(IStateAccess stateAccess,
                    Map properties) {
        LocalizedMessage message = LocalizedMessage.get(
                        LOC_TSK_MSG_COPY_AGENT_FILES_SCRIPT_ROLLBACK,
                        STR_TOMCAT_GROUP, null);
        return message;
    }

    public boolean rollBack(String name, IStateAccess stateAccess,
                    Map properties) throws InstallException {
            boolean status = false;
        status = super.removeAgentFiles(stateAccess);
        return status;
    }
}
