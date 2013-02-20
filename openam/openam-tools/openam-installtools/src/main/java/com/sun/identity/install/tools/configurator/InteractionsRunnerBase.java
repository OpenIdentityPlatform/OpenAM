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
 * $Id: InteractionsRunnerBase.java,v 1.4 2008/08/29 20:23:39 leiming Exp $
 *
 */

package com.sun.identity.install.tools.configurator;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.HashMap;
import java.util.Map;

import com.sun.identity.install.tools.util.Debug;
import com.sun.identity.install.tools.util.LocalizedMessage;
        
abstract class InteractionsRunnerBase {

    InteractionsRunnerBase(InstallRunInfo installRunInfo,
            UserResponseHandler uHandler) throws InstallException {
        setUserResponseMap(new HashMap());
        setAllInteractions(new ArrayList());
        setAllConfiguredInteractionKeys(new ArrayList());
        setUserResponseHandler(uHandler);
        setInstallRunInfo(installRunInfo);
        createAllInteractions(installRunInfo);
    }

    abstract public void setStateAccessDataType(int index);

    abstract public IStateAccess getStateAccess();

    abstract public void createAllInteractions(InstallRunInfo installRunInfo)
            throws InstallException;

    abstract public void storeNonPersistentKeys(String key, int index);

    /**
     * Method to run all the interations and gather the state information.
     * 
     * @param startFromLast
     *            if true the last interaction is executed first. Needed if
     *            somebody does a back on common/instance interaction and the
     *            instance interaction has to start from the last one
     * @return a Map consisting of InteractionResults. The key is the
     *         Interaction key and value is InteractionResult
     * @throws InstallException
     */
    public void runInteractions(boolean startFromLast)
            throws InstallAbortException, InstallException {
        if (isActive()) {
            int interactionsCount = getAllInteractions().size();
            int index = (startFromLast) ? interactionsCount - 1 : 0;
            boolean exitStatus = false;
            InteractionResultStatus status = null;

            // Check for first and last interactions. Check if multiple
            // interactions are skipped in a row
            while (skipInteraction(index)) {
                index = (startFromLast) ? --index : ++index;
            }

            do {
                Debug.log("InteractionsRunnerBase: Running Interaction["
                        + index + "].");
                if (index >= getAllInteractions().size()) {
                    setFinalStatus(InteractionResultStatus.STATUS_CONTINUE);
                    return;
                }
                
                UserDataInteraction interaction = (UserDataInteraction) 
                    getAllInteractions().get(index);
                
                InteractionResult result = runInteraction(interaction, index);

                status = result.getStatus();
                switch (status.getIntValue()) {
                case InteractionResultStatus.INT_STATUS_CONTINUE:
                    Debug.log("InteractionsRunnerBase: Interaction "
                            + "resulted in CONTINUE @ Index: " + index);
                    storeSummaryDescription(index, result
                            .getSummaryDescription());
                    if (++index >= interactionsCount) {
                        exitStatus = true;
                    } else {
                        while (!exitStatus && skipInteraction(index)) {
                            storeSummaryDescription(index, null);
                            if (++index >= interactionsCount) {
                                exitStatus = true;
                            }
                        }
                    }
                    break;
                case InteractionResultStatus.INT_STATUS_BACK:
                    Debug.log("InteractionsRunnerBase: Interaction "
                            + "resulted in BACK @ Index: " + index);
                    if (--index < 0) {
                        exitStatus = true;
                    } else {
                        while (!exitStatus && skipInteraction(index)) {
                            if (--index < 0)
                                exitStatus = true;
                        }
                    }
                    break;
                case InteractionResultStatus.INT_STATUS_ABORT:
                    Debug.log("InteractionsRunnerBase: ABORT requested "
                            + "for Interaction[" + index + "].");
                    LocalizedMessage lMessage = 
                        LocalizedMessage.get(LOC_DR_MSG_USER_ABORT);
                    throw new InstallAbortException(lMessage);
                }
            } while (!exitStatus);
            setFinalStatus(status);
            Debug.log("InteractionsRunnerBase.runInteractions() Exiting at "
                    + "index: " + index);
        } else { // Nothing to execute. So continue
            setFinalStatus(InteractionResultStatus.STATUS_CONTINUE);
        }
    }

    private InteractionResult runInteraction(UserDataInteraction interaction,
            int index) throws InstallException {
        InteractionResult result = null;
        // Set the data type for iStateAccess (common or instance data).
        setStateAccessDataType(index);
        String interactionKey = interaction.getKey();

        if (getUserResponseHandler() != null) { // Silent Mode
            String userValue = getUserResponseHandler().getProperty(
                    interactionKey);
            if (userValue != null && userValue.trim().length() > 0) {
                getUserResponseMap().put(interactionKey, userValue);
            }
            result = interaction.interactSilent(getStateAccess(),
                    getUserResponseMap());
        } else { // Interactive mode
            /* If there's optional-display attribute for this interaction and
             * its value is false, installer just get its default value without 
             * displaying the interaction.
             */
            if (getInstallRunInfo().isCheckDisplay() && 
                        !interaction.getInteractionInfo().isDisplay()) {
                String strResult = 
                    interaction.processDefaultValFromAllSources(
                        getStateAccess());
                getStateAccess().put(interaction.getKey(), strResult);
                LocalizedMessage summaryDesc = 
                    LocalizedMessage.get(
                        InteractionConstants.LOC_IN_MESS_SUMMARY_DESC_FORMAT,
                        new Object[] { interaction.getSummaryDesc(), 
                            strResult });
                result = new InteractionResult(
                        InteractionResultStatus.STATUS_CONTINUE,
                        null, summaryDesc);
                
            } else {
                result = interaction.interact(getStateAccess());
            }
        }
        return result;
    }

    private boolean skipInteraction(int index) {
        boolean status = false;

        Debug.log("InteractionsRunnerBase: Calling skipInteraction on: "
                + index);
        if (index >= getAllInteractions().size()) {
        	return false;
        }
        
        InteractionInfo iinfo = ((UserDataInteraction) 
                getAllInteractions().get(index)).getInteractionInfo();

        /*
         * Return if any match is found <skipIf key="ABC" ignorecase="true">
         * <value>ttt</value> <value>www</value> </skipIf>
         */
        SkipIfInfo skipIfInfo = iinfo.getSkipIfInfo();
        if (skipIfInfo != null) {
            String skipKey = skipIfInfo.getKey();
            ArrayList skipValues = skipIfInfo.getValues();
            boolean ignorecase = skipIfInfo.getIgnoreCase();

            IStateAccess stateAccess = getStateAccess();
            String stateValue = (String) stateAccess.get(skipKey);
            if (stateValue != null && skipValues != null) {
                Iterator it = skipValues.iterator();
                while (it.hasNext() && !status) {
                    if (ignorecase) {
                        if (stateValue.equalsIgnoreCase((String) it.next())) {
                            Debug.log("InteractionsRunnerBase: skipKey : "
                                    + skipKey + " = " + stateValue);
                            status = true;
                        }
                    } else {
                        if (stateValue.equals((String) it.next())) {
                            Debug.log("InteractionsRunnerBase: skipKey : "
                                    + skipKey + " = " + stateValue);
                            status = true;
                        }
                    }
                }
            }
        }
        return status;
    }

    public void initInteractions(ArrayList interactionsInfoList)
            throws InstallException {
        int interactionInfoListSize = interactionsInfoList.size();
        setAllInteractionKeys(new ArrayList(interactionInfoListSize));
        setAllSummaryDescriptions(new ArrayList(interactionInfoListSize));
        for (int i = 0; i < interactionInfoListSize; i++) {
            UserDataInteraction interaction = InteractionFactory
                    .createInteraction((InteractionInfo) interactionsInfoList
                            .get(i));
            getAllInteractions().add(interaction);
            getAllInteractionKeys().add(interaction.getKey());
            getAllConfiguredInteractionKeys().add(interaction.getKey());
            if (!interaction.isPersistent()) { // Store non persistent keys
                storeNonPersistentKeys(interaction.getKey(), i);
            }
        }
    }

    public void clear() {
        setFinalStatus(null);
        getUserResponseMap().clear();
        getAllSummaryDescriptions().clear();
    }

    private void storeSummaryDescription(int index, LocalizedMessage message) {
        // Save the Summary Description for later use. To maintain
        // sorted order, first remove and then add at that index
        if (index < getAllSummaryDescriptions().size()
                && getAllSummaryDescriptions().get(index) != null) {
            // Remove the previous description & add a new one
            getAllSummaryDescriptions().remove(index);
        }
        getAllSummaryDescriptions().add(index, message);
    }

    public boolean isActive() {
        return !getAllInteractions().isEmpty();
    }

    public InteractionResultStatus getFinalStatus() {
        return finalStatus;
    }

    public ArrayList getAllInteractions() {
        return allInteractions;
    }

    public ArrayList getAllInteractionKeys() {
        return allInteractionKeys;
    }

    public ArrayList getAllConfiguredInteractionKeys() {
        return allConfiguredInteractionKeys;
    }

    public UserResponseHandler getUserResponseHandler() {
        return userResponseHandler;
    }

    private Map getUserResponseMap() {
        return userResponseMap;
    }

    public ArrayList getAllSummaryDescriptions() {
        return allSummaryDescriptions;
    }

    public void setFinalStatus(InteractionResultStatus status) {
        finalStatus = status;
    }

    public void setAllInteractions(ArrayList list) {
        allInteractions = list;
    }

    public void setAllInteractionKeys(ArrayList list) {
        allInteractionKeys = list;
    }

    public void setAllConfiguredInteractionKeys(ArrayList list) {
        allConfiguredInteractionKeys = list;
    }

    public void setUserResponseHandler(UserResponseHandler uHandler) {
        userResponseHandler = uHandler;
    }

    public void setAllSummaryDescriptions(ArrayList list) {
        allSummaryDescriptions = list;
    }

    private void setUserResponseMap(Map map) {
        userResponseMap = map;
    }
    
    public InstallRunInfo getInstallRunInfo() {
        return installRunInfo;
    }
    
    private void setInstallRunInfo(InstallRunInfo installRunInfo) {
        this.installRunInfo = installRunInfo;
    }

    private Map userResponseMap;

    private InteractionResultStatus finalStatus;

    private ArrayList allSummaryDescriptions;

    // All these are Intialized only once
    private ArrayList allInteractionKeys; // We want to save it in order

    // All of them, including the ones that are not used (common + instance)
    private ArrayList allConfiguredInteractionKeys;

    private ArrayList allInteractions;

    private UserResponseHandler userResponseHandler;
    
    private InstallRunInfo installRunInfo;

    public static final String LOC_DR_MSG_USER_ABORT = "DR_MSG_USER_ABORT";

}
