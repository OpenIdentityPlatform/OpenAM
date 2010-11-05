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
 *
 */

package com.sun.identity.install.tools.configurator;

import java.util.ArrayList;

import com.sun.identity.install.tools.util.Debug;
import com.sun.identity.install.tools.util.LocalizedMessage;

public class CustomInstallDriver extends Driver {

    public void install(InstallLogger installLog) throws InstallException {
        install(null, INT_OPERATION_TYPE_REGULAR, installLog);
    }

    public void install(String fileName, int operationType,
            InstallLogger installLog) throws InstallException {
        Debug.log("CustomInstallDriver.install() Starting Install..");

        // Intialize the configuration loader & extract the uninstall run info
        ConfigurationLoader cl = new ConfigurationLoader();
        setRunInfo(cl.getCustomInstallRunInfo());

        // Set the Server Home Config Handler
        setServerLocatorHandler(getRunInfo());

        // Set the install type
        setOperationType(operationType);

        // When install is invoked with the above arguments:
        // if isSilentMode is true => fileName is user's response file
        // and if isSilentMode is false => fileName is the filer where the
        // user's recorded responses need to be saved.
        UserResponseHandler uHandler = null;
        if (getOperationType() == INT_OPERATION_TYPE_USE_RESPONSE) {
            Debug.log("CustomInstallDriver.install() 'useResponse' option was "
                    + "selected. Creating handler and loading data from file");
            uHandler = new UserResponseHandler(fileName);
            uHandler.load();
        }

        // Show the welcome message
        printConsoleMessageWithMarkers(getRunInfo().getWelcomeMessageInfo());

        InstFinderInteractionsRunner iFinderRunner = 
            new InstFinderInteractionsRunner(getRunInfo(), uHandler, 
                    getServerLocatorHandler());

        InteractionsRunner iRunner = new InteractionsRunner(getRunInfo(),
                uHandler);

        DisplaySummaryHandler summaryHandler = new DisplaySummaryHandler(true);

        // Execute all interactions
        executeAllInteractions(iFinderRunner, iRunner, summaryHandler);

        // Write the Version info to the log file
        writeVersionInfoToLog(installLog, LOC_DR_MSG_INSTALL_LOG_VERSION_DESC,
                LOC_DR_MSG_INSTALL_LOG_VERSION_DESC_LINE);

        // Write summary messages to install logs
        writeSummDispMessToInstLog(summaryHandler.getDisplayMessages(),
                installLog);

        // 2 - Run the tasks (common & instance)
        TaskRunner taskRunner = new TaskRunner(getRunInfo(), getInstallState()
                .getStateAccess(), installLog, isSilentMode(), true);
        taskRunner.runTasks();

        // Save all that needs to be saved before the state is changed in 3
        if (getOperationType() == INT_OPERATION_TYPE_SAVE_RESPONSE) {
            createResponseFile(fileName, iFinderRunner, iRunner);
        }

        // 3. Save the InstallState.
        updateStateAccess(getInstallState(), iFinderRunner, iRunner);
        getInstallState().saveState();

        // If installing for the first time, save the Product Home in the
        // Locator
        // file under Server Home.
        if (InstallState.isFreshInstall()) {
            getServerLocatorHandler().saveProductHome();
        }

        // Display and log Install Summary
        displayAndLogInstallSummary(installLog);

        // Print Install Log location at the end.
        printConsoleEmptyLine();
        printConsoleEmptyLine();
        printConsoleMessage(getLogFilePathMessage(installLog,
                LOC_DR_MSG_INSTALL_LOG_FILE_PATH));

        // Show the exit message
        printConsoleMessage(getRunInfo().getExitMessageInfo());
    }

    public void displayAndLogInstallSummary(InstallLogger installLog) {
        ArrayList messageList = new ArrayList();
        messageList.add(getSummaryInfoHeaderMessage());
        messageList.add(getSummaryInfoHeaderMessageFmtLine());
        messageList.add(getProductInstanceNameMessage());
        messageList.add(getProductConfigFilePathMessage());
	messageList.add(getAgentConfigFilePathMessage());
        messageList.add(getProductAuditLogsPathMessage());
        messageList.add(getProductDebugLogsPathMessage());

        int count = messageList.size();
        printConsoleEmptyLine();
        printConsoleEmptyLine();
        installLog.getLogger().logEmptyLine();
        for (int i = 0; i < count; i++) {
            LocalizedMessage message = (LocalizedMessage) messageList.get(i);
            printConsoleMessage(message);
            installLog.getLogger().log(message);
        }
    }

    public LocalizedMessage getSummaryInfoHeaderMessage() {
        return LocalizedMessage.get(LOC_DR_MSG_PRODUCT_SUMM_INFO_BEGIN);
    }

    public LocalizedMessage getSummaryInfoHeaderMessageFmtLine() {
        return LocalizedMessage.get(LOC_DR_MSG_PRODUCT_SUMM_INFO_BEGIN_LINE);
    }

    public LocalizedMessage getProductInstanceNameMessage() {
        String instanceName = (String) getInstallState().getStateAccess()
                .getInstanceName();
        Object[] args = { instanceName };
        LocalizedMessage message = LocalizedMessage.get(
                LOC_DR_MSG_PRODUCT_INSTANCE_NAME, args);
        return message;
    }

    public LocalizedMessage getProductConfigFilePathMessage() {
        String productConfigFilePath = (String) getInstallState()
                .getStateAccess().get(STR_CONFIG_FILE_PATH_TAG);
        Object[] args = { productConfigFilePath };
        LocalizedMessage message = LocalizedMessage.get(
                LOC_DR_MSG_PRODUCT_CONFIG_FILE_NAME, args);
        return message;
    }

    public LocalizedMessage getProductAuditLogsPathMessage() {
        String productAuditDirectory = (String) getInstallState()
                .getStateAccess().get(STR_AUDIT_DIR_PREFIX_TAG);
        Object[] args = { productAuditDirectory };
        LocalizedMessage message = LocalizedMessage.get(
                LOC_DR_MSG_PRODUCT_AUDIT_DIR, args);
        return message;
    }

    public LocalizedMessage getProductDebugLogsPathMessage() {
        String productDebugDirectory = (String) getInstallState()
                .getStateAccess().get(STR_DEBUG_DIR_PREFIX_TAG);
        Object[] args = { productDebugDirectory };
        LocalizedMessage message = LocalizedMessage.get(
                LOC_DR_MSG_PRODUCT_DEBUG_DIR, args);
        return message;
    }

    public void checkInstanceConfiguration(InstallState installState)
            throws InstallException {
        Debug.log("InstallState: is " + installState);
        if (installState.isConfiguredInstance()) {
            Debug.log("CustomInstallDriver: Error - An Product instance '"
                    + installState.getInstanceName() + "' has already been "
                    + "configured for this Application Server instance.");
            throw new InstallException(LocalizedMessage.get(
                    LOC_DR_ERR_ALREADY_CONFIGURED_INST,
                    new Object[] { installState.getInstanceName() }));
        }
    }

    public LocalizedMessage getAgentConfigFilePathMessage() {
        String agentConfigFilePath = (String) getInstallState()
                .getStateAccess().get(STR_CONFIG_AGENT_CONFIG_FILE_PATH_TAG);
        Object[] args = { agentConfigFilePath };
        LocalizedMessage message = LocalizedMessage.get(
                LOC_DR_MSG_PRODUCT_AGENT_CONFIG_FILE_NAME, args);
        return message;
    }


    /** Field LOC_DR_ERR_ALREADY_CONFIGURED_INST * */
    public static final String LOC_DR_ERR_ALREADY_CONFIGURED_INST = 
        "DR_ERR_ALREADY_CONFIGURED_INST";

    public static final String LOC_DR_MSG_INSTALL_LOG_FILE_PATH = 
        "DR_MSG_INSTALL_LOG_FILE_PATH";

    public static final String LOC_DR_MSG_INSTALL_LOG_VERSION_DESC = 
        "DR_MSG_INSTALL_LOG_VERSION_DESC";

    public static final String LOC_DR_MSG_INSTALL_LOG_VERSION_DESC_LINE = 
        "DR_MSG_INSTALL_LOG_VERSION_DESC_LINE";
    
    public static final String LOC_DR_MSG_PRODUCT_AGENT_CONFIG_FILE_NAME = 
        "DR_MSG_PRODUCT_AGENT_CONFIG_FILE_NAME";
}
