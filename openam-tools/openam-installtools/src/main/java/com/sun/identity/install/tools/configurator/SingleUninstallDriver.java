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
 * $Id: SingleUninstallDriver.java,v 1.2 2008/06/25 05:51:24 qcheng Exp $
 *
 */

package com.sun.identity.install.tools.configurator;

import com.sun.identity.install.tools.util.Debug;

public class SingleUninstallDriver extends UninstallDriver {

    public SingleUninstallDriver() throws InstallException {
        super();
    }

    public void uninstall(int operationType, InstallLogger uninstallLog,
            String fileName) throws InstallException {

        Debug.log("SingleUninstallDriver: Starting Uninstall");

        // Set the install type
        setOperationType(operationType);

        // When install is invoked with the above arguments:
        // if isSilentMode is true => fileName is user's response file
        // and if isSilentMode is false => fileName is the filer where the
        // user's recorded responses need to be saved.
        UserResponseHandler uHandler = null;
        if (getOperationType() == INT_OPERATION_TYPE_USE_RESPONSE) {
            // Load the responses
            uHandler = new UserResponseHandler(fileName);
            uHandler.load();
        }

        // Show the welcome message
        printConsoleMessageWithMarkers(getRunInfo().getWelcomeMessageInfo());

        // Write the Version info to the log file
        writeVersionInfoToLog(uninstallLog,
                LOC_DR_MSG_UNINSTALL_LOG_VERSION_DESC,
                LOC_DR_MSG_UNINSTALL_LOG_VERSION_DESC_LINE);

        InstFinderInteractionsRunner iFinderRunner = new 
            InstFinderInteractionsRunner(getRunInfo(), uHandler, 
                    getServerLocatorHandler());
        InteractionsRunner iRunner = new InteractionsRunner(getRunInfo(),
                uHandler);

        // Execute all interactions and tasks
        executeInteractionsAndTasks(iFinderRunner, iRunner, uninstallLog);

        // Save all that needs to be saved before state in changed
        if (getOperationType() == INT_OPERATION_TYPE_SAVE_RESPONSE) {
            createResponseFile(fileName, iFinderRunner, iRunner);
        }

        // Update the state information
        updateStateInformation(iFinderRunner, iRunner, uninstallLog);

        // Show the exit message
        printConsoleMessage(getRunInfo().getExitMessageInfo());

    }

}
