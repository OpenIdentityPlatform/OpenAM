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
 * $Id: InstallInteraction.java,v 1.3 2008/06/25 05:51:20 qcheng Exp $
 *
 */

package com.sun.identity.install.tools.configurator;

import java.util.ArrayList;

import com.sun.identity.install.tools.util.Console;
import com.sun.identity.install.tools.util.Debug;
import com.sun.identity.install.tools.util.LocalizedMessage;

/**
 * 
 * Interaction class to handle user input. This interaction class can handle 
 * all user data input related interactions including password interactions. 
 * This class extends UserDataInteraction and implements all the necessary 
 * hooks to process options and user input in interactive mode.
 * 
 */
public class InstallInteraction extends UserDataInteraction implements
        InteractionConstants {

    /*
     * Constructor for Interaction class
     * 
     */
    public InstallInteraction(InteractionInfo info) throws InstallException {
        super(info);
        setUserOptions();
        setPromptMessage();
        setDisplayMessages();
    }

    /*
     * Method to be called for interact install @param state IStateAccess
     * 
     * @return InteractionResult
     */
    public InteractionResult interact(IStateAccess state)
            throws InstallException {
        setDefaultValue(state);
        return super.interact(state);
    }

    /*
     * Method to be process user input for interactive mode
     * 
     * @pararm userInput User input @param state IStateAccess
     * 
     * @return InteractionResult
     */
    public InteractionResult processData(String userInput, IStateAccess state)
            throws InstallException {

        InteractionResultStatus result = null;
        InteractionResult interResult = null;

        boolean isReq = getInteractionInfo().getRequiredFlag();
        String procInput = preProcessUserInput(userInput, state, isReq);

        if (procInput == null) {
            // Invalid user input
            Console.println();
            Console
                    .println(LocalizedMessage
                            .get(LOC_IN_WRN_INVALID_USER_INPUT));
        } else {
            if ((!isReq) && (procInput.equals(STR_IN_EMPTY_STRING))) {
                result = InteractionResultStatus.STATUS_CONTINUE;
                state.put(getKey(), getNormalizedValue(procInput));
            } else {
                CumulativeValResult cumRes = processValidators(procInput,
                        state, true);
                Debug.log("InstallInteraction.processData: " + 
                          cumRes.getWarningMessage());
                if (cumRes.getCumValResult()) {
                    if (cumRes.getWarningMessage() != null) {
                        Console.println();
                        Console.println();
                        Console.println(getWarning());
                        // Specific warning message
                        Console.println(cumRes.getWarningMessage());
                        Console.println();
                    }
                    result = InteractionResultStatus.STATUS_CONTINUE;
                    // Store the user input
                    state.put(getKey(), getNormalizedValue(procInput));
                    // Now copy the calc keys only if cum res = true
                    if (cumRes.getCalcKeyValPairs() != null) {
                        state.putData(cumRes.getCalcKeyValPairs());
                    }
                } else {
                    Console.println();
                    Console.println();
                    Console.println(getError());
                    // Specific error message
                    Console.println(cumRes.getErrorMessage());
                    Console.println();
                }
            }
        }

        LocalizedMessage summaryDesc = null;
        if ((result != null)
                && (result.getIntValue() == 
                    InteractionResultStatus.INT_STATUS_CONTINUE)) {
            summaryDesc = LocalizedMessage.get(LOC_IN_MESS_SUMMARY_DESC_FORMAT,
                    new Object[] { getSummaryDesc(), procInput });
            interResult = new InteractionResult(result, null, summaryDesc);
        }

        return interResult;

    }

    /*
     * Method to be process user option in interactive mode
     * 
     * @pararm option UserOptionItem @param state IStateAccess
     * 
     * @return InteractionResult
     */
    public InteractionResult processOption(UserOptionItem option,
            IStateAccess state) throws InstallException {

        InteractionResult result = null;
        if (option.getDisplayItem().equals(STR_IN_MSG_OPTION_HELP)) {
            Console.println();
            Console.println();
            Console.println(getHelp());
            Console.println();
            Console.println();
            displayOptionsHelp(state);
        } else if (option.getDisplayItem().equals(STR_IN_MSG_OPTION_BACK)) {
            result = new InteractionResult(InteractionResultStatus.STATUS_BACK,
                    null, null);
        } else if (option.getDisplayItem().equals(STR_IN_MSG_OPTION_EXIT)) {
            result = new InteractionResult(
                    InteractionResultStatus.STATUS_ABORT, null, null);
        } else if (option.getDisplayItem().equals(STR_IN_MSG_OPTION_CLR_DEF)) {
            // Put empty value for this interaction in IStateAccess
            clearValueInState(state);
            LocalizedMessage summaryDesc = LocalizedMessage.get(
                    LOC_IN_MESS_SUMMARY_DESC_FORMAT, new Object[] {
                            getSummaryDesc(), STR_IN_EMPTY_STRING });
            result = new InteractionResult(
                    InteractionResultStatus.STATUS_CONTINUE, null, 
                    summaryDesc);

        }

        return result;
    }

    public ArrayList getDisplayMessages() {
        return displayMessages;
    }

    public LocalizedMessage getHelp() {
        return getMessage(STR_IN_HELP_SUFFIX);
    }

    /*
     * This function need to now display an extra option depending on whether
     * default value is present ir not. @param state
     * 
     * @return BaseOptions
     */
    public BaseOptions getUserOptions(IStateAccess state)
            throws InstallException {
        if (!getInteractionInfo().getRequiredFlag()) {
            // create this option only for this case
            if (clearDefOpt == null) {
                clearDefOpt = new UserOptionItem(STR_IN_MSG_OPTION_CLR_DEF,
                        LOC_IN_MSG_OPTION_CLR_DEF, LOC_IN_MSG_OPTION_CLR_DEF
                                + STR_IN_HELP_SUFFIX);
            }

            String defaultValue = getDefaultValue(state);
            if (defaultValue != null && defaultValue.length() > 0
                    && !options.contains(clearDefOpt)) {
                options.add(clearDefOpt);
            } else if (options.contains(clearDefOpt)) {
                options.remove(clearDefOpt);
            }
        }
        return options;
    }

    public LocalizedMessage getPromptMessage(IStateAccess state) {
        return getPromptSuffix();
    }

    public String getPromptValue(IStateAccess state) throws InstallException {
        return defaultValue;
    }

    public void setIsFirstFlag(boolean isFirst) {
        super.setIsFirstFlag(isFirst);
        // Remove the back option, beacause it was already added while
        // initialization.
        if (isFirst) {
            options.remove(backOption);
        }
    }

    public String getDefaultValue(IStateAccess state) throws InstallException {

        String defValue = null;

        // Only for isreq = false, re-process the default value
        if (!getInteractionInfo().getRequiredFlag()) {
            defValue = processDefaultValFromAllSources(state);
            defaultValue = defValue;
            Debug.log("InstallInteraction.getDefaultValue() : Is Required "
                    + "= " + getInteractionInfo().getRequiredFlag()
                    + ", Default value = " + defaultValue);
        }

        if ((defaultValue != null)
                && (defaultValue.equals(STR_IN_EMPTY_STRING))) {
            defaultValue = null;
        }

        return defaultValue;
    }

    private void setDisplayMessages() {
        displayMessages = new ArrayList();
        displayMessages.add(getDescription());
    }

    private void setDefaultValue(IStateAccess state) throws InstallException {
        String defValue = null;
        if (getInteractionInfo().getRequiredFlag()) {
            defValue = processDefaultValFromAllSources(state);
        } else {
            defValue = processDefaultValFromStateOnly(state);
        }

        defaultValue = defValue;

    }

    private void setPromptMessage() {
        promptMessage = getPromptSuffix();
    }

    private void setUserOptions() {

        options = new SingleLineOptions();
        options.add(new UserOptionItem(STR_IN_MSG_OPTION_HELP,
                LOC_IN_MSG_OPTION_HELP, LOC_IN_MSG_OPTION_HELP
                        + STR_IN_HELP_SUFFIX));

        backOption = new UserOptionItem(STR_IN_MSG_OPTION_BACK,
                LOC_IN_MSG_OPTION_BACK, LOC_IN_MSG_OPTION_BACK
                        + STR_IN_HELP_SUFFIX);

        // At this point isFirst flag is not set. So, the back option will be
        // added and it will be removed later
        options.add(backOption);

        options.add(new UserOptionItem(STR_IN_MSG_OPTION_EXIT,
                LOC_IN_MSG_OPTION_EXIT, LOC_IN_MSG_OPTION_EXIT
                        + STR_IN_HELP_SUFFIX));

    }

    private ArrayList displayMessages;

    private SingleLineOptions options;

    private LocalizedMessage promptMessage;

    private String defaultValue;

    private UserOptionItem clearDefOpt;

    private UserOptionItem backOption;

}
