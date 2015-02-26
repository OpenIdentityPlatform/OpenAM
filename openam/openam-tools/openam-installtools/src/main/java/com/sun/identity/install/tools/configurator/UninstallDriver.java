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
 * $Id: UninstallDriver.java,v 1.2 2008/06/25 05:51:24 qcheng Exp $
 *
 */

package com.sun.identity.install.tools.configurator;

import com.sun.identity.install.tools.util.Debug;
import com.sun.identity.install.tools.util.LocalizedMessage;

abstract class UninstallDriver extends Driver {

    public UninstallDriver() throws InstallException {

        // Intialize the configuration loader & extract the uninstall run info
        ConfigurationLoader cl = new ConfigurationLoader();
        setRunInfo(cl.getUnInstallRunInfo());

        // Set the Server Home Config Handler
        setServerLocatorHandler(getRunInfo());
    }

    public abstract void uninstall(int operationType,
            InstallLogger uninstallLog, String fileName)
            throws InstallException;

    /*
     * 
     * Function to update the state information in memory and persistent store
     * 
     * @param iFinderRunner @param iRunner @param uninstallLog
     * 
     * @throws InstallException
     */
    public void updateStateInformation(
            InstFinderInteractionsRunner iFinderRunner,
            InteractionsRunner iRunner, InstallLogger uninstallLog)
            throws InstallException {

        // Update the PersistentStateAccess to remove non persistent keys
        updateStateAccess(getInstallState(), iFinderRunner, iRunner);

        // 3. Save the InstallState. The InstallState will automatically remove
        // the state files if it is the last instance.
        getInstallState().removeInstance();
        getInstallState().saveState();

        // Print Uninstall Log location at the end.
        printConsoleEmptyLine();
        printConsoleEmptyLine();
        printConsoleMessage(getLogFilePathMessage(uninstallLog,
                LOC_DR_MSG_UNINSTALL_LOG_FILE_PATH));

    }

    /*
     * 
     * Function to execute all interactions and tasks
     * 
     * @param iFinderRunner @param iRunner @param uninstallLog
     * 
     * @throws InstallException
     * 
     */
    public void executeInteractionsAndTasks(
            InstFinderInteractionsRunner iFinderRunner,
            InteractionsRunner iRunner, InstallLogger uninstallLog)
            throws InstallException {

        DisplaySummaryHandler summaryHandler = new DisplaySummaryHandler(
                false);

        // Execute all interactions
        executeAllInteractions(iFinderRunner, iRunner, summaryHandler);

        // Write summary messages to uninstall logs
        writeSummDispMessToInstLog(summaryHandler.getDisplayMessages(),
                uninstallLog);

        // 1 - Run the tasks (common & instance)
        // TODO: if a task fails just log and continue no prompt
        TaskRunner taskRunner = new TaskRunner(getRunInfo(), getInstallState()
                .getStateAccess(), uninstallLog, isSilentMode(), false);
        taskRunner.runTasks();

        // If installing for the first time, save the Product Home in the
        // Locator
        // file under Server Home.
        if (InstallState.getInstanceCount() == 1) {
            // We are removing the last instance. So. remove the locator file
            getServerLocatorHandler().removeProductHome();
        }

    }

    /*
     * Check if InstallState associated with the instance is configured or not
     * 
     * @param installState @throws InstallException
     * 
     */
    public void checkInstanceConfiguration(InstallState installState)
            throws InstallException {
        if (!installState.isConfiguredInstance()) {
            Debug.log("UninstallDriver: Error - No Product instance '"
                    + "found with the Instance Finder Data supplied");
            throw new InstallException(LocalizedMessage.get(
                    LOC_DR_ERR_NOT_CONFIGURED_INST, new Object[] { installState
                            .getInstanceName() }));
        }
    }

    /** Field LOC_DR_ERR_ALREADY_CONFIGURED_INST * */
    public static final String LOC_DR_ERR_NOT_CONFIGURED_INST = 
        "DR_ERR_NOT_CONFIGURED_INST";

    public static final String LOC_DR_MSG_UNINSTALL_LOG_FILE_PATH = 
        "DR_MSG_UNINSTALL_LOG_FILE_PATH";

    public static final String LOC_DR_MSG_UNINSTALL_LOG_VERSION_DESC = 
        "DR_MSG_UNINSTALL_LOG_VERSION_DESC";

    public static final String LOC_DR_MSG_UNINSTALL_LOG_VERSION_DESC_LINE = 
        "DR_MSG_UNINSTALL_LOG_VERSION_DESC_LINE";
}
