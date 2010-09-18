/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: MigrateDriver.java,v 1.3 2008/06/25 05:51:21 qcheng Exp $
 *
 */

package com.sun.identity.install.tools.configurator;

import com.sun.identity.install.tools.util.Debug;

/**
 * This class migrate one product instance at a time.
 */
public class MigrateDriver extends Driver {
    
    public MigrateDriver() throws InstallException {
        super();
        
        // Intialize the configuration loader & extract the migrate run info
        ConfigurationLoader cl = new ConfigurationLoader();
        setRunInfo(cl.getMigrateRunInfo());
        setMigrateServerLocatorHandler(getRunInfo());
        
        Debug.log("MigrateDriver");
    }
    
    /*
     * Function to execute migration tasks.
     *
     * @param migrateLog
     * @throws InstallException
     *
     */
    public void executeInteractionsAndTasks (
    		InteractionsRunner iRunner,
            InstallLogger migrateLog)
            throws InstallException {
    	
    	boolean done = false;
    	while (!done) {
    		iRunner.runInteractions(false);
    		if (iRunner.getFinalStatus().getIntValue() == 
                InteractionResultStatus.INT_STATUS_CONTINUE) {
    			done = true;
    		}
    	}
        
        DisplaySummaryHandler summaryHandler = new DisplaySummaryHandler(true);
        
        // Write summary messages to migrate logs
        writeSummDispMessToInstLog(summaryHandler.getDisplayMessages(),
                migrateLog);
        
        // Run the tasks (common & instance)
        TaskRunner taskRunner = new TaskRunner(getRunInfo(), getInstallState()
        .getStateAccess(), migrateLog, isSilentMode(), true);
        taskRunner.runTasks();
        
    }
    
    /*
     * Function to update the state information in memory and persistent store
     *
     * @param iFinderRunner @param iRunner @param migrateLog
     *
     * @throws InstallException
     */
    public void updateStateInformation(
            InstFinderInteractionsRunner iFinderRunner,
            InteractionsRunner iRunner, InstallLogger migrateLog)
            throws InstallException {
        
        // Save the InstallState.
        updateStateAccess(getInstallState(), iFinderRunner, iRunner);
        getInstallState().saveState();
        
        // If migrating for the first time, save the Product Home in the
        // Locator file under Server Home.
        if (InstallState.isFreshInstall()) {
            ((MigrateServerLocatorHandler)getServerLocatorHandler()).
                    backupProductHome();
            InstallDataStore.getInstallDataStore().setIsExistingStore(true);
        }
    }
    
    public void checkInstanceConfiguration(InstallState installState)
            throws InstallException {
        
        Debug.log("MigrateDriver.checkInstanceConfiguration() - " +
                "InstallState: is " + installState);
    }
    
    /** Field LOC_DR_ERR_ALREADY_CONFIGURED_INST * */
    
    public static final String LOC_DR_MSG_INSTALL_LOG_VERSION_DESC =
            "DR_MSG_INSTALL_LOG_VERSION_DESC";
    
    public static final String LOC_DR_MSG_INSTALL_LOG_VERSION_DESC_LINE =
            "DR_MSG_INSTALL_LOG_VERSION_DESC_LINE";
    
}
