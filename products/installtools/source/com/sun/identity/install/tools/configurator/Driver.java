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
 * $Id: Driver.java,v 1.4 2008/06/25 05:51:18 qcheng Exp $
 *
 */

package com.sun.identity.install.tools.configurator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.sun.identity.install.tools.util.Audit;
import com.sun.identity.install.tools.util.Console;
import com.sun.identity.install.tools.util.Debug;
import com.sun.identity.install.tools.util.LocalizedMessage;

abstract public class Driver implements InstallConstants {

    public Driver() {
    }

    public abstract void checkInstanceConfiguration(InstallState state)
            throws InstallException;

    public void executeAllInteractions(
            InstFinderInteractionsRunner iFinderRunner,
            InteractionsRunner iRunner, DisplaySummaryHandler summaryHandler)
            throws InstallException {
        // NOTE: 1. 
        // After identifying the instance, the InstallState.getStateAccess() 
        // returns a PersistentStateAccess object and this object gets 
        // modified/updated in the Interactions & the Driver. The InstallState
        // would be maintaining the reference to the same object. So, at any 
        // point call to InstallState.getStateAccess() gets handle to that 
        // same PersistentStateAccess object.
        //  
        // NOTE: 2. If the user aborts an InstallAbortException will be thrown 
        // which will be handled by the InstallHandler.        
        ExecutionStatus executionStatus = new ExecutionStatus(
                INT_RUN_IFINDER_INTERACTIONS, false);
        do {
            switch (executionStatus.getNextExecutionItem()) {
            case INT_RUN_IFINDER_INTERACTIONS:
                executionStatus = executeIFinderInteractions(iFinderRunner,
                        iRunner, executionStatus.getStartFromLastFlag());
                break;
            case INT_RUN_INTERACTIONS:
                executionStatus = executeInstanceInteractions(iRunner,
                        executionStatus.getStartFromLastFlag());
                break;
            case INT_RUN_DISPLAY_SUMMARY:
                executionStatus = displaySummary(iFinderRunner, iRunner,
                        summaryHandler);
                break;
            }
        } while (executionStatus.getNextExecutionItem() != INT_EXIT_LOOP);
    }

    public ExecutionStatus executeIFinderInteractions(
            InstFinderInteractionsRunner iFinderRunner,
            InteractionsRunner iRunner, boolean startFromLast)
            throws InstallException {
        Debug.log("Driver.executeIFinderInteractions() Running Instance \n"
                + "Finder interactions ..");
        // No need to get the last interaction status. If there is 
        // an Abort an Exception will be thrown.
        iFinderRunner.runInteractions(startFromLast);

        // IStateAccess at this point is a TransientStateAccess object 
        Map iFinderData = iFinderRunner.getStateAccess().getData();
        ArrayList useKeys = iFinderRunner.getAllInteractionKeys();

        // Save the install state for later use
        setInstallState(new InstallState(iFinderData, useKeys));
        checkInstanceConfiguration(getInstallState());

        ExecutionStatus executionStatus = null;
        if (iRunner.isActive()) { // Common AND/OR Instance interactions 
            // present Set the StateAccess using the InstallState for iRunner. 
            // The setting up of IStateAccess for iRunner needs to happen in 
            // this case block because we don't want it to happen when user
            // chooses to go BACK after seeing Display Summary!                
            iRunner.setStateAccess(getInstallState().getStateAccess());
            executionStatus = new ExecutionStatus(INT_RUN_INTERACTIONS, false);
        } else { // Common AND/OR Instance interactions NOT present
            executionStatus = new ExecutionStatus(INT_RUN_DISPLAY_SUMMARY,
                    false);
        }
        Debug.log("Driver.executeIFinderInteractions() after execution "
                + "StateAccess is: " + getInstallState().getStateAccess());

        return executionStatus;
    }

    public ExecutionStatus executeInstanceInteractions(
            InteractionsRunner iRunner, boolean startFromLast)
            throws InstallAbortException, InstallException {
        Debug.log("Driver.executeInstanceInteractions() Running instance"
                + " interactions ..");

        // Set Default execution status
        ExecutionStatus executionStatus = new ExecutionStatus(
                INT_RUN_DISPLAY_SUMMARY, false);
        iRunner.runInteractions(startFromLast);

        Debug.log("Driver.executeInstanceInteractions() after execution "
                + "StateAccess is: " + getInstallState().getStateAccess());

        if (iRunner.getFinalStatus().getIntValue() == 
            InteractionResultStatus.INT_STATUS_BACK) {
            Debug.log("Driver.executeInstanceInteractions() BACK requested "
                    + " after common/instance interactions. So, going back " 
                    +  "into InstanceFinder Interactions ..");
            // Update the execution status
            executionStatus.setNextExecutionItem(INT_RUN_IFINDER_INTERACTIONS);
            executionStatus.setStartFromLastFlag(true);
        }
        return executionStatus;
    }

    public ExecutionStatus displaySummary(
            InstFinderInteractionsRunner iFinderRunner,
            InteractionsRunner iRunner, DisplaySummaryHandler summaryHandler)
            throws InstallAbortException, InstallException {
        ExecutionStatus executionStatus = new ExecutionStatus(INT_EXIT_LOOP,
                false);
        if (!isSilentMode()) { // Display only in non silent mode!
            ArrayList summaryMessages = new ArrayList();
            summaryMessages.addAll(iFinderRunner.getAllSummaryDescriptions());
            summaryMessages.addAll(iRunner.getAllSummaryDescriptions());
            summaryHandler.setDisplayMessages(summaryMessages);

            InteractionResultStatus dStatus = summaryHandler
                    .interact(getInstallState().getStateAccess());
            if (dStatus.getIntValue() == 
                InteractionResultStatus.INT_STATUS_BACK) {
                int nextItem = (iRunner.isActive()) ? INT_RUN_INTERACTIONS
                        : INT_RUN_IFINDER_INTERACTIONS;
                executionStatus.setNextExecutionItem(nextItem);
                executionStatus.setStartFromLastFlag(true);
            } else if (dStatus.getIntValue() == 
                InteractionResultStatus.INT_STATUS_START_OVER) {
                // Set the state of iRunner to null as it is cleared
                // Update the iFinderRunner state to new TransientStateAccess
                iRunner.clear();
                iFinderRunner.clear();

                executionStatus
                        .setNextExecutionItem(INT_RUN_IFINDER_INTERACTIONS);
                executionStatus.setStartFromLastFlag(false);
            } else if (dStatus.getIntValue() == 
                InteractionResultStatus.INT_STATUS_ABORT) {
                Debug.log("Driver.displaySummary() User requested ABORT");
                LocalizedMessage lMessage = LocalizedMessage
                        .get(LOC_DR_MSG_USER_ABORT);
                throw new InstallAbortException(lMessage);
            }
        }
        return executionStatus;
    }

    public void createResponseFile(String fileName,
            InstFinderInteractionsRunner iFinderRunner,
            InteractionsRunner iRunner) throws InstallException {
        ArrayList allKeys = new ArrayList();
        allKeys.addAll(iFinderRunner.getAllConfiguredInteractionKeys());
        allKeys.addAll(iRunner.getAllConfiguredInteractionKeys());

        // All the non persistent keys don't exit in state at this time
        UserResponseHandler uHandler = new UserResponseHandler(fileName);
        uHandler.storeData(getInstallState().getStateAccess(), allKeys);
        uHandler.save();
    }

    public void updateStateAccess(InstallState installState,
            InstFinderInteractionsRunner iFinderRunner,
            InteractionsRunner iRunner) {
        // Update the StateAccess to remove keys which should not persist.
        PersistentStateAccess stateAccess = installState.getStateAccess();
        Set nonPersistCommonKeys = iRunner.getNonPersistentCommonKeys();
        stateAccess.setCommonDataFlag(true);
        stateAccess.removeKeys(nonPersistCommonKeys);

        Set nonPersistInstanceKeys = new HashSet();
        nonPersistInstanceKeys.addAll(iFinderRunner.getNonPersistentKeys());
        nonPersistInstanceKeys.addAll(iRunner.getNonPersistentInstanceKeys());
        stateAccess.setCommonDataFlag(false);
        stateAccess.removeKeys(nonPersistInstanceKeys);
    }

    public void writeSummDispMessToInstLog(ArrayList summMessages,
            InstallLogger installLog) {
        if (summMessages != null && !summMessages.isEmpty()) {
            // Write headers            
            LocalizedMessage headerMsg = LocalizedMessage
                    .get(LOC_DR_MSG_SUMM_INFO_BEGIN);
            LocalizedMessage lineMsg = LocalizedMessage
                    .get(LOC_DR_MSG_SUMM_INFO_BEGIN_LINE);
            installLog.getLogger().logEmptyLine();
            installLog.getLogger().log(headerMsg);
            installLog.getLogger().log(lineMsg);

            // Write Summary
            for (int i = 0; i < summMessages.size(); i++) {
                LocalizedMessage message = (LocalizedMessage) summMessages
                        .get(i);
                if (message != null) {
                    installLog.getLogger().log(message);
                }
            }
            installLog.getLogger().logEmptyLine();
        }
    }

    public LocalizedMessage getLogFilePathMessage(InstallLogger installLog,
            String installLogLocKey) {
        Audit installAuditLog = installLog.getLogger();
        Object[] args = { installAuditLog.getAuditLogFileName() };
        LocalizedMessage message = LocalizedMessage.get(installLogLocKey, 
                args);        
        return message;
    }

    public void printConsoleMessage(LocalizedMessage message) {
        if (!isSilentMode()) {
            Console.println(message);
        }
    }

    public void printConsoleEmptyLine() {
        if (!isSilentMode()) {
            Console.println();
        }
    }

    public void writeVersionInfoToLog(InstallLogger installLog,
            String descriptionLocKey, String descrptionFmtLineLocKey) {
        BufferedReader br = null;
        try {
            InputStream is = this.getClass().getResourceAsStream(
                    STR_VERSION_FILE_NAME);
            br = new BufferedReader(new InputStreamReader(is));
            String line = null;

            // First write the headers
            Object[] formatArgs = { STR_BEGIN_END_LINE_MARKER };
            LocalizedMessage formatMessage = LocalizedMessage.get(
                    LOC_DR_MSG_VERSION_INFO, formatArgs);
            LocalizedMessage descriptionMsg = LocalizedMessage
                    .get(descriptionLocKey);
            LocalizedMessage descriptionFmtLineMsg = LocalizedMessage
                    .get(descrptionFmtLineLocKey);
            installLog.getLogger().log(formatMessage);
            installLog.getLogger().log(descriptionMsg);
            installLog.getLogger().log(descriptionFmtLineMsg);

            while ((line = br.readLine()) != null) {
                // Print only the non hashed lines
                if (!line.startsWith("#")) {
                    Object[] args = { line };
                    LocalizedMessage message = LocalizedMessage.get(
                            LOC_DR_MSG_VERSION_INFO, args);
                    installLog.getLogger().log(message);
                }
            }
            installLog.getLogger().log(formatMessage);
        } catch (Exception ex) {
            Debug.log("Driver: An exception occurred while reading "
                    + "version file: ", ex);
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException io) {
                    // Ignore
                    Debug.log("Driver: An exception occurred while closing "
                            + "version file stream: ", io);
                }
            }
        }
    }

    /*
     *  Used to print the welcome and exit message for console mode
     */
    public void printConsoleMessageWithMarkers(I18NInfo messageInfo) {
        //Only in console mode
        if (!isSilentMode()) {
            LocalizedMessage message = LocalizedMessage.get(messageInfo
                    .getKey(), messageInfo.getGroup());
            Console.println();
            Console.println();
            Console.println(STR_BEGIN_END_LINE_MARKER);
            Console.println(message);
            Console.println(STR_BEGIN_END_LINE_MARKER);
        }
    }

    /*
     *  Used to print the welcome and exit message for console mode
     */
    public void printConsoleMessage(I18NInfo messageInfo) {
        //Only in console mode
        if (!isSilentMode()) {
            LocalizedMessage message = LocalizedMessage.get(messageInfo
                    .getKey(), messageInfo.getGroup());
            Console.println();
            Console.println(message);
            Console.println();
        }
    }

    public boolean isSilentMode() {
        return (getOperationType() == INT_OPERATION_TYPE_USE_RESPONSE);
    }

    public InstallState getInstallState() {
        return _installState;
    }

    public int getOperationType() {
        return _operationType;
    }

    public InstallRunInfo getRunInfo() {
        return _runInfo;
    }

    public void setInstallState(InstallState installState) {
        _installState = installState;
    }

    public ServerLocatorHandler getServerLocatorHandler() {
        return _serverLocatorHandler;
    }

    public void setRunInfo(InstallRunInfo info) {
        _runInfo = info;
        Debug.log("Run Info: " + info);
    }

    public void setServerLocatorHandler(InstallRunInfo installRunInfo) {
        String locatorClass = installRunInfo.getHomeDirLocator();
        _serverLocatorHandler = new ServerLocatorHandler(locatorClass);
    }

    public void setMigrateServerLocatorHandler(InstallRunInfo installRunInfo) {
        String locatorClass = installRunInfo.getHomeDirLocator();
        _serverLocatorHandler = new MigrateServerLocatorHandler(locatorClass);
    }
    
    public void setOperationType(int type) {
        _operationType = type;
    }

    ///////////////////////////////////////////////////////////////////////////
    // VARIABLE DECLARATIONS
    ///////////////////////////////////////////////////////////////////////////
    private int _operationType;

    private InstallRunInfo _runInfo; // Can be of Install or UnInstall   

    private InstallState _installState;

    private ServerLocatorHandler _serverLocatorHandler;

    public static final String LOC_DR_MSG_USER_ABORT = "DR_MSG_USER_ABORT";

    /** Field LOC_DR_MSG_SUMM_INFO_BEGIN **/
    public static final String LOC_DR_MSG_SUMM_INFO_BEGIN = 
        "DR_MSG_SUMM_INFO_BEGIN";

    /** Field LOC_DR_MSG_SUMM_INFO_BEGIN_LINE **/
    public static final String LOC_DR_MSG_SUMM_INFO_BEGIN_LINE = 
        "DR_MSG_SUMM_INFO_BEGIN_LINE";

    /** Field LOC_DR_MSG_SUMM_INFO_END **/
    public static final String LOC_DR_MSG_SUMM_INFO_END = 
        "DR_MSG_SUMM_INFO_END";

    /** Field LOC_DR_MSG_PRODUCT_INSTANCE_NAME **/
    public static final String LOC_DR_MSG_PRODUCT_INSTANCE_NAME = 
        "DR_MSG_PRODUCT_INSTANCE_NAME";

    /** Field LOC_DR_MSG_PRODUCT_CONFIG_FILE_NAME **/
    public static final String LOC_DR_MSG_PRODUCT_CONFIG_FILE_NAME = 
        "DR_MSG_PRODUCT_CONFIG_FILE_NAME";

    /** Field LOC_DR_MSG_PRODUCT_DEBUG_DIR **/
    public static final String LOC_DR_MSG_PRODUCT_DEBUG_DIR = 
        "DR_MSG_PRODUCT_DEBUG_DIR";

    /** Field LOC_DR_MSG_PRODUCT_AUDIT_DIR **/
    public static final String LOC_DR_MSG_PRODUCT_AUDIT_DIR = 
        "DR_MSG_PRODUCT_AUDIT_DIR";

    /** Field LOC_DR_MSG_VERSION_INFO **/
    public static final String LOC_DR_MSG_VERSION_INFO = 
        "DR_MSG_VERSION_INFO";

    /** Field LOC_DR_MSG_PRODUCT_AGENT_TAGS_FILE_NAME **/
    public static final String LOC_DR_MSG_PRODUCT_AGENT_TAGS_FILE_NAME = 
        "DR_MSG_PRODUCT_AGENT_TAGS_FILE_NAME";


    // Constants
    public static final int INT_RUN_IFINDER_INTERACTIONS = 0;

    public static final int INT_RUN_INTERACTIONS = 1;

    public static final int INT_RUN_DISPLAY_SUMMARY = 2;

    public static final int INT_EXIT_LOOP = 3;

    public class ExecutionStatus {

        ExecutionStatus(int nextItem, boolean flag) {
            setNextExecutionItem(nextItem);
            setStartFromLastFlag(flag);
        }

        public int getNextExecutionItem() {
            return nextExecutionItem;
        }

        public boolean getStartFromLastFlag() {
            return startFromLast;
        }

        public void setNextExecutionItem(int nextItem) {
            nextExecutionItem = nextItem;
        }

        public void setStartFromLastFlag(boolean flag) {
            startFromLast = flag;
        }

        private int nextExecutionItem;

        private boolean startFromLast;
    }

}
