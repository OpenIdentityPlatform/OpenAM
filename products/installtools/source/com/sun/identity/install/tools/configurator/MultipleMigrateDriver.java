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
 * $Id: MultipleMigrateDriver.java,v 1.4 2008/08/29 20:23:39 leiming Exp $
 *
 */

package com.sun.identity.install.tools.configurator;

import com.sun.identity.install.tools.util.FileUtils;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;

import com.sun.identity.install.tools.util.Console;
import com.sun.identity.install.tools.util.Debug;
import com.sun.identity.install.tools.util.LocalizedMessage;
import com.sun.identity.install.tools.util.ConfigUtil;
import java.io.File;

/**
 * This class migrates all previous product instances into
 * new product.
 */
public class MultipleMigrateDriver extends MigrateDriver {
    
    /**
     * Constructor.
     */
    public MultipleMigrateDriver() throws InstallException {
        super();
        Debug.log("MultipleMigrateDriver");
    }
    
    /**
     * start migrate product instances.
     * @param migrateLog
     */
    public void migrate(InstallLogger migrateLog) throws InstallException {
        migrate(null, INT_OPERATION_TYPE_REGULAR, migrateLog);
    }
    
    /**
     * start migrate product instances. keep this method's signature for 
     * future expansion.
     *
     * @param fileName
     * @param operationType
     * @param migrateLog
     */
    public void migrate(String fileName, int operationType,
            InstallLogger migrateLog) throws InstallException {
        
        Debug.log("MultipleMigrateDriver.migrate() - Starting migrate");
        
        // Set the install type
        setOperationType(operationType);
        
        // Show the welcome message
        printConsoleMessageWithMarkers(getRunInfo().getWelcomeMessageInfo());
        
        // read previous product home input by the user.
        String migrateFromProductHome = getMigrateFromProductHome();
        MigrateFromInstFinderStore.setProductHome(migrateFromProductHome);
        MigrateFromInstallDataStore.setProductHome(migrateFromProductHome);
        
        // Write the Version info to the log file
        writeVersionInfoToLog(migrateLog,
                LOC_DR_MSG_INSTALL_LOG_VERSION_DESC,
                LOC_DR_MSG_INSTALL_LOG_VERSION_DESC_LINE);
        
        // This object has to be created before we iterate
        InstFinderInteractionsRunner iFinderRunner = new
                InstFinderInteractionsRunner(getRunInfo(), null,
                getServerLocatorHandler());
        
        // Retrieve the instance finder keys to get all product details
        ArrayList instFinderKeys = iFinderRunner.getAllInteractionKeys();
        
        // get previous (or migrate from) product instance finder store.
        MigrateFromInstFinderStore migrateFromInstFinderStore =
                MigrateFromInstFinderStore.getInstance();
        Map allProductsDetails = migrateFromInstFinderStore
                .getAllProductDetails(instFinderKeys);
        
        if (allProductsDetails == null || allProductsDetails.isEmpty()) {
            LocalizedMessage productsNotFoundMsg = LocalizedMessage
                    .get(LOC_HR_MSG_MIGRATE_NONE_FOUND);
            Console.println(productsNotFoundMsg);
            Debug.log(productsNotFoundMsg.toString());
            
        } else {
            // Start migrating products one at a time
            Iterator iter = allProductsDetails.keySet().iterator();
            boolean firstTime = true;
            
            while (iter.hasNext()) {
                String instanceName = (String) iter.next();
                Map productDetails = (Map) allProductsDetails.get(
                        instanceName);
                
                Console.println();
                Console.println();
                LocalizedMessage productMigrateBeginMsg = LocalizedMessage
                        .get(LOC_HR_MSG_MIGRATE_PRODUCT_BEGIN_MSG);
                Console.println(productMigrateBeginMsg);
                Debug.log(productMigrateBeginMsg.toString());
                Console.println();
                
                
                // Now set the transient state access with product
                // details of the instance being migrated
                TransientStateAccess stateAccess = new TransientStateAccess(
                        productDetails);
                iFinderRunner.setStateAccess(
                        new TransientStateAccess(productDetails));
                
                if (firstTime) {
                    checkActiveProductHome(migrateFromProductHome,
                            stateAccess);
                    firstTime = false;
                }
                
                // create install state for previous product install
                MigrateFromInstallState migrateInstallState =
                        new MigrateFromInstallState(
                        productDetails,
                        instFinderKeys);
                
                // create and set new Install State
                InstallState installState = new InstallState(
                        productDetails,
                        instFinderKeys);
                setInstallState(installState);
                
                
                // Migrate the instance state data, do some mappings.
                prepareMigrate(migrateInstallState, installState);
                
                // Execute the migration
                migrateInternal(operationType, iFinderRunner, installState,
                        migrateLog);
            }
        }
        
        // Show the exit message
        printConsoleMessage(getRunInfo().getExitMessageInfo());
        
    }
    
    /**
     * Migrate the instance state data, make some preparation before
     * migration.
     *
     * @param migrateInstallState
     * @param installState
     */
    private void prepareMigrate(MigrateFromInstallState migrateInstallState,
            InstallState installState) {
        
        installState.setStateAccess(
                migrateInstallState.getStateAccess());
        StateData instanceData =
                installState.getStateAccess().
                getInstanceData();
        
        String migrateInstanceName = instanceData.getInstanceName();
        installState.getStateAccess().put(STR_INSTANCE_NAME_MIGRATE_TAG, 
                migrateInstanceName);
        instanceData.setInstanceName(
                installState.getInstanceName());
        
        String migrateInstanceConfigDir = (String)installState.getStateAccess().
                get(STR_CONFIG_AGENT_CONFIG_FILE_PATH_TAG);
        if (migrateInstanceConfigDir == null ||
                migrateInstanceConfigDir.length() == 0) {
            migrateInstanceConfigDir = (String)installState.getStateAccess().
                    get(STR_CONFIG_FILE_PATH_TAG);
        }
        
        installState.getStateAccess().put(STR_CONFIG_DIR_PREFIX_MIGRATE_TAG,
                migrateInstanceConfigDir);
        
        // clear intermediate values generated by previous migration.
        String migrateValue = (String)installState.getStateAccess().
                get(STR_SAVE_REMOTE_AGENT_INSTALL_DIR_KEY);
        if (migrateValue != null) {
            installState.getStateAccess().remove(
                    STR_SAVE_REMOTE_AGENT_INSTALL_DIR_KEY);
        }
        migrateValue = (String)installState.getStateAccess().
                get(STR_SAVE_AGENT_INSTANCE_NAME_KEY);
        if (migrateValue != null) {
            installState.getStateAccess().remove(
                    STR_SAVE_AGENT_INSTANCE_NAME_KEY);
        }
        
    }
    
    /**
     * execute tasks of migrate each instance.
     *
     * @operationType
     * @iFinderRunner
     * @migrateLog
     *
     * @throws InstallException
     *
     */
    private void migrateInternal(int operationType,
            InstFinderInteractionsRunner iFinderRunner, 
            InstallState installState,
            InstallLogger migrateLog) throws InstallException {
        
        InteractionsRunner iRunner = new InteractionsRunner(getRunInfo(),
                null);
        iRunner.setStateAccess(installState.getStateAccess());
        
        // Execute all interactions and tasks
        executeInteractionsAndTasks(iRunner, migrateLog);
        
        // Update the state information
        updateStateInformation(iFinderRunner, iRunner, migrateLog);
        
    }
    
    /**
     * read from the user the product home to be migrated.
     *
     * @return
     * @throws InstallException
     */
    private String getMigrateFromProductHome() throws InstallException {
        String migrateFromProductHome = null;
        
        I18NInfo i18Info =
                new I18NInfo(LOC_MIGRATE_FROM_PRODUCT_HOME, STR_GROUP);
        InteractionInfo interactionInfo = new InteractionInfo(
                STR_MIGRATE_FROM_PRODUCT_HOME, null, i18Info, null, true, false,
                "install", true, null);
        ValidationInfo validationInfo = new ValidationInfo(
            "DIR_EXISTS",
            new HashMap(),
            "com.sun.identity.install.tools.configurator.FileSystemValidator");
        interactionInfo.addValidationInfo(validationInfo);
        UserDataInteraction interaction = InteractionFactory.createInteraction(
                interactionInfo);
        
        TransientStateAccess stateAccess =
                new TransientStateAccess(new HashMap());
        boolean done = false;
        while (!done) {
            done  = false;
            InteractionResult interactionResult =
                    interaction.interact(stateAccess);
            switch (interactionResult.getStatus().getIntValue()) {
                case InteractionResultStatus.INT_STATUS_CONTINUE:
                    done = true;
                    break;
                    
                case InteractionResultStatus.INT_STATUS_BACK:
                    stateAccess.clear();
                    continue;
                    
                case InteractionResultStatus.INT_STATUS_ABORT:
                    Debug.log("MultipleMigrateDriver: ABORT requested ");
                    LocalizedMessage lMessage =
                            LocalizedMessage.get(LOC_DR_MSG_USER_ABORT);
                    throw new InstallAbortException(lMessage);
            } // end of switch
            
            migrateFromProductHome =
                    (String)stateAccess.get(interactionInfo.getLookupKey());
            
            // normalize the input product home
            File file = new File(migrateFromProductHome);
            if (file.isDirectory()) {
                migrateFromProductHome = file.getAbsolutePath();
            }

            // convert Windows path
            migrateFromProductHome = migrateFromProductHome.replace('\\', '/');
            if (!verifyMigrateFromProductHome(migrateFromProductHome)) {
                stateAccess.clear();
                continue;
            }
            
        } // end of while loop.
        
        return migrateFromProductHome;
    }
    
    /**
     * check if the product home input by the user has the locator file of
     * the product to be migrated from.
     */
    private boolean verifyMigrateFromProductHome(String migrateFromProductHome)
    throws InstallException {
        
        if (migrateFromProductHome == null ||
                migrateFromProductHome.length() == 0) {
            Debug.log("MultipleMigrateDriver.verifyMigrateFromProductHome() :" +
                    "empty product home by the user");
            Console.println(
                    LocalizedMessage.get(LOC_HR_MSG_MIGRATE_NONE_FOUND));
            return false;
        }
        
        if (!migrateFromProductHome.equals(ConfigUtil.getHomePath())) {
            Debug.log("MultipleMigrateDriver.verifyMigrateFromProductHome() : "
                    + "Migrate from product home: "
                    + migrateFromProductHome + ", New product home: "
                    + ConfigUtil.getHomePath());
            
            String translateFile = migrateFromProductHome +
                    MigrateFromInstFinderStore.getRelativeTranslateFile();
            if (!FileUtils.isFileValid(translateFile)) {
                Console.println(
                        LocalizedMessage.get(LOC_HR_MSG_MIGRATE_NONE_FOUND));
                return false;
            }
            
        } else {
            Debug.log("MultipleMigrateDriver - Error:"
                    + "This product has been already migrated!");
            
            throw new InstallException(LocalizedMessage.get(
                    LOC_DR_ERR_PRODUCT_ALREADY_MIGRATED));
        }
        
        return true;
    }
    
    /**
     * check if the product to be migrated from is the one which is active or
     * configured on the application server instance.
     *
     * @migrateFromProductHome the product home input by the user
     * @stateAccess IStateAccess
     * @throws InstallException
     */
    private void checkActiveProductHome(String migrateFromProductHome,
            IStateAccess stateAccess) throws InstallException {
        
        String productHomeInLocator =
                getServerLocatorHandler().getProductHome(stateAccess);
        if (!productHomeInLocator.equals(migrateFromProductHome)) {
            Debug.log("MultipleMigrateDriver.checkActiveProductHome() - " +
                    "active product home:" + productHomeInLocator +
                    " input product home:" + migrateFromProductHome);
            
            Object[] args = new Object[] {migrateFromProductHome};
            throw new InstallException(LocalizedMessage.get(
                    LOC_HR_MSG_MIGRATE_NOT_ACTIVE_PRODUCT, args));
            
        }
    }
    
    public static final String LOC_HR_MSG_MIGRATE_NONE_FOUND  =
            "HR_MSG_MIGRATE_NONE_FOUND";
    
    public static final String LOC_HR_MSG_MIGRATE_PRODUCT_BEGIN_MSG =
            "HR_MSG_MIGRATE_PRODUCT_BEGIN_MSG";
    
    public static final String LOC_HR_ERR_MIGRATE_PRODUCT_EX_MSG =
            "HR_ERR_MIGRATE_PRODUCT_EX_MSG";
    
    public static final String STR_MIGRATE_FROM_PRODUCT_HOME =
            "MIGRATE_FROM_PRODUCT_HOME";
    
    public static final String LOC_MIGRATE_FROM_PRODUCT_HOME =
            "INT_MIGRATE_FROM_PRODUCT_HOME";
    
    public static final String LOC_HR_MSG_MIGRATE_NOT_ACTIVE_PRODUCT =
            "HR_MSG_MIGRATE_NOT_ACTIVE_PRODUCT";
    
    public static final String STR_GROUP = LocalizedMessage.STR_TOOLSMSG_GROUP;
}
