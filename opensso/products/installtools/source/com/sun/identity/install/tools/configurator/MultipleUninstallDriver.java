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
 * $Id: MultipleUninstallDriver.java,v 1.2 2008/06/25 05:51:22 qcheng Exp $
 *
 */

package com.sun.identity.install.tools.configurator;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

import com.sun.identity.install.tools.util.Console;
import com.sun.identity.install.tools.util.Debug;
import com.sun.identity.install.tools.util.LocalizedMessage;

public class MultipleUninstallDriver extends UninstallDriver {

    public MultipleUninstallDriver() throws InstallException {
        super();
    }

    public void uninstall(int operationType, InstallLogger uninstallLog,
            String fileName) throws InstallException {

        Debug.log("MultipleUninstallDriver: Starting Uninstall");

        // Set the install type
        setOperationType(operationType);

        // Show the welcome message
        printConsoleMessageWithMarkers(getRunInfo().getWelcomeMessageInfo());

        // Write the Version info to the log file
        writeVersionInfoToLog(uninstallLog,
                LOC_DR_MSG_UNINSTALL_LOG_VERSION_DESC,
                LOC_DR_MSG_UNINSTALL_LOG_VERSION_DESC_LINE);

        // This object has to be created before we iterate
        InstFinderInteractionsRunner iFinderRunner = new 
            InstFinderInteractionsRunner(getRunInfo(), null, 
                    getServerLocatorHandler());

        // Retrieve the inst finder keys to get all product details
        ArrayList instFinderKeys = iFinderRunner.getAllInteractionKeys();
        InstFinderStore iFinderStore = InstFinderStore.getInstance();
        Map allProductsDetails = iFinderStore
                .getAllProductDetails(instFinderKeys);

        if (allProductsDetails == null || allProductsDetails.isEmpty()) {
            LocalizedMessage productsNotFoundMsg = LocalizedMessage
                    .get(LOC_HR_MSG_UNINSTALL_ALL_NONE_FOUND);
            Console.println(productsNotFoundMsg);
            Debug.log(productsNotFoundMsg.toString());
        } else {
            // Start uninstalling products one at a time
            Iterator iter = allProductsDetails.keySet().iterator();
            while (iter.hasNext()) {
                String instanceName = (String) iter.next();
                Map productDetails = (Map) allProductsDetails.get(
                        instanceName);

                Console.println();
                Console.println();
                LocalizedMessage productUninstallBeginMsg = LocalizedMessage
                        .get(LOC_HR_MSG_UNINSTALL_PRODUCT_BEGIN_MSG);
                Console.println(productUninstallBeginMsg);
                Debug.log(productUninstallBeginMsg.toString());
                Console.println();

                try {
                    // Now set the transient state access with product
                    // details of the instance being uninstalled
                    iFinderRunner.setStateAccess(new TransientStateAccess(
                            productDetails));
                    // Execute the common code
                    uninstallInternal(operationType, iFinderRunner,
                            uninstallLog);
                } catch (InstallAbortException iae) {
                    Debug.log("Failed to Uninstall product instance "
                            + instanceName + " with ex : ", iae);
                    Console.print(LocalizedMessage.get(LOC_DR_MSG_USER_ABORT));
                } catch (Exception ex) {
                    Debug.log(
                            "Failed to Uninstall product instance with ex : ",
                            ex);
                    Console.print(LocalizedMessage
                            .get(LOC_HR_ERR_UNINSTALL_PRODUCT_EX_MSG),
                            new Object[] { ex.getMessage() });
                }
            }
        }

        // Show the exit message
        printConsoleMessage(getRunInfo().getExitMessageInfo());

    }

    /*
     * Tasks to be performed for each uninstall
     * 
     * @operationType @iFinderRunner @uninstallLog
     * 
     * @throws InstallException
     * 
     */
    public void uninstallInternal(int operationType,
            InstFinderInteractionsRunner iFinderRunner,
            InstallLogger uninstallLog) throws InstallException {

        InteractionsRunner iRunner = new InteractionsRunner(getRunInfo(), 
                null);

        // Execute all interactions and tasks
        executeInteractionsAndTasks(iFinderRunner, iRunner, uninstallLog);

        // Update the state information
        updateStateInformation(iFinderRunner, iRunner, uninstallLog);

    }

    public static final String LOC_HR_MSG_UNINSTALL_ALL_NONE_FOUND = 
        "HR_MSG_UNINSTALL_ALL_NONE_FOUND";

    public static final String LOC_HR_MSG_UNINSTALL_PRODUCT_BEGIN_MSG = 
        "HR_MSG_UNINSTALL_PRODUCT_BEGIN_MSG";

    public static final String LOC_HR_ERR_UNINSTALL_PRODUCT_EX_MSG = 
        "HR_ERR_UNINSTALL_PRODUCT_EX_MSG";

    public static final String LOC_DR_MSG_USER_ABORT = "DR_MSG_USER_ABORT";
}
