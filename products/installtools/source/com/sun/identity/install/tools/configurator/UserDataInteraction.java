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
 * $Id: UserDataInteraction.java,v 1.4 2008/07/15 21:19:50 leiming Exp $
 *
 */

package com.sun.identity.install.tools.configurator;

import java.util.Map;

import com.sun.identity.install.tools.util.Console;
import com.sun.identity.install.tools.util.Debug;
import com.sun.identity.install.tools.util.LocalizedMessage;

/**
 * Abstract class for user data related interactions in both interactive and
 * silent mode.
 */
abstract public class UserDataInteraction extends BaseInteraction implements
        InteractionConstants {

    /**
     * Constructor for abstract clas 
     * 
     * @param info InteractionInfo
     */
    public UserDataInteraction(InteractionInfo info) throws InstallException {
        super();
        setInteractionInfo(info);
    }

    /**
     * An abstract method to be implemented by child class to process user data
     * for interactive input 
     * 
     * @pararm userInput User Input 
     * @param state IStateAccess state
     * 
     * @return InteractionResult
     */
    abstract public InteractionResult processData(String userInput,
            IStateAccess state) throws InstallException;

    /**
     * abstract method to be implemented by child class to process user options
     * for interactive input 
     * 
     * @pararm userInput User Input 
     * @param state IStateAccess state
     * 
     * @return InteractionResult
     */
    abstract public InteractionResult processOption(UserOptionItem option,
            IStateAccess state) throws InstallException;

    /*
     * Method to interact with the user for silent attrs
     * 
     */
    public InteractionResult interactSilent(IStateAccess state, Map map)
            throws InstallException {

        InteractionResult interResult = null;
        InteractionResultStatus result = null;
        boolean isReq = getInteractionInfo().getRequiredFlag();

        // Pre-process user input from silent state file
        String procInput = preProcessSilentInput(map);
        if ((!isReq) && (procInput.equals(STR_IN_EMPTY_STRING))) {
            result = InteractionResultStatus.STATUS_CONTINUE;
            state.put(getKey(), getNormalizedValue(procInput));
        } else {
            CumulativeValResult cumRes = processValidators(procInput, state,
                    false);
            if (cumRes.getCumValResult()) {
                if (cumRes.getWarningMessage() != null) {
                    Console.println();
                    Console.println(LocalizedMessage.get(
                            LOC_VA_WRN_VAL_MESSAGE) + 
                            getSummaryDescription().toString());
                    Console.println(LocalizedMessage.get(
                            LOC_VA_WRN_VAL_INSTALL_LOG));
                }
                result = InteractionResultStatus.STATUS_CONTINUE;
                state.put(getKey(), getNormalizedValue(procInput));
                if (cumRes.getCalcKeyValPairs() != null) {
                    state.putData(cumRes.getCalcKeyValPairs());
                }
            } else {
                Debug.log("Interaction failed to continue since one or"
                        + " more of the validators failed.");
                throw new InstallException(getError());
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

    /**
     * Function to pre-process user input passed from state file during silent
     * install
     * 
     * @param userResponseMap Map 
     * 
     * @return String processedInput
     * 
     * @throws InstallException
     * 
     */
    public String preProcessSilentInput(Map userResponseMap)
            throws InstallException {
        String processedInput = null;
        String userInput = null;
        boolean isReq = getInteractionInfo().getRequiredFlag();

        if (userResponseMap != null) {

            userInput = (String) userResponseMap.get(getKey());
            Debug.log("Key = " + getKey() + " has entry = " + userInput
                    + ", in response file");

            if ((userInput == null) || (userInput.trim().length() == 0)) {
                Debug.log("Key = " + getKey() + " has NO entry = " + userInput
                        + ", in response file");
                if (isReq) {
                    Debug.log("ERROR : Invalid user input = " + userInput
                            + " for key =" + getKey() + " in response file");
                    throw new InstallException(LocalizedMessage
                            .get(LOC_IN_ERR_INVALID_USER_INPUT));
                } else {
                    processedInput = STR_IN_EMPTY_STRING;
                }
            } else {
                processedInput = userInput;
            }
        } else {
            Debug.log("ERROR : Null message map passed to interaction, cannot "
                    + "continue.");
            throw new InstallException(LocalizedMessage
                    .get(LOC_IN_ERR_SILENT_INST_FAILED));
        }

        return processedInput;
    }

    /**
     * Method that interacts with user in interactive mode. Child class will
     * need to override this method and implement the appropriate hooks
     * processOption and processData
     * 
     * @param state IStateAccess 
     * 
     * @return InteractionResult
     * 
     */
    public InteractionResult interact(IStateAccess state)
            throws InstallException {

        InteractionResult status = null;
        do {
            Console.println();
            Console.println();

            // Display the interaction messages
            displayMessages();

            // Now display available options
            getUserOptions(state).display();

            // Now display the prompt message. Also show prompt value if 
            // present
            displayPromptMessage(state);

            // Now read user input
            String userInput = Console.readLine();
            if (userInput != null && userInput.trim().length() > 0) {
                // First Check if it is one of selected options. If so call
                // method to handle the selection.
                UserOptionItem selectedOption = getUserOptions(state)
                        .getSelectedOption(userInput);
                if (selectedOption != null) { // Process Option
                    status = processOption(selectedOption, state);
                } else { // Process User Data
                    status = processData(userInput, state);
                }
            } else { // Can't be an UserOption. So, Process as Data
                status = processData(userInput, state);
            }

            // Display confirmation if status is Abort or Exit
            if (status != null) {
                status = confirmOnExitStatus(status, state);
            }
        } while (status == null);

        return status;
    }

    /**
     * Method to check for exit condition @param result InteractionResult 
     * object
     * 
     * @param state IStateAccess
     * 
     * @return InteractionResult @throws InstallException
     */
    protected InteractionResult confirmOnExitStatus(InteractionResult result,
            IStateAccess state) throws InstallException {

        InteractionResult finalResult = result;
        if (result.getStatus().getIntValue() == 
            InteractionResultStatus.INT_STATUS_ABORT) {
            // Confirm Once again!!
            ConfirmInteraction confirm = new ConfirmInteraction();
            InteractionResultStatus newStatus = confirm.interact(state);
            if (newStatus.getIntValue() == 
                InteractionResultStatus.INT_STATUS_CONTINUE) {
                // Set it to null, so the we can continue the caller's loop.
                finalResult = null;
            }
        }
        return finalResult;
    }

    /**
     * Helper function to pre-process user input
     * 
     * @param userInput User Input 
     * @param state IStateAccess Object 
     * @param isReq
     * 
     * Is required flag @return String Processed input result string
     * 
     * @throws InstallException
     * 
     */
    public String preProcessUserInput(String userInput, IStateAccess state,
            boolean isReq) throws InstallException {

        String procRes = null;
        if (isReq) {
            procRes = preProcUserInputIfRequired(userInput, state);
        } else {
            procRes = preProcUserInputIfOptional(userInput, state);
        }

        return procRes;
    }

    /**
     * Helper function to process user input when required attr is true
     * 
     * @param String User Input 
     * @param state IStateAccess Object 
     * @return String user input if required
     * 
     * @throws InstallException
     * 
     */
    String preProcUserInputIfRequired(String userInput, IStateAccess state)
            throws InstallException {

        String processedInput = null;
        String defValue = processDefaultValFromAllSources(state);

        if ((userInput == null) || (userInput.trim().length() == 0)) {
            if ((defValue != null) && (defValue.length() > 0)) {
                processedInput = defValue;
            }
        } else {
            processedInput = userInput;
        }

        return processedInput;
    }

    /**
     * Helper function to process user input when required attr is false
     * 
     * @param userInput String user input 
     * @param state IStateAccess Object
     * 
     * @return String processed input
     * 
     */
    String preProcUserInputIfOptional(String userInput, IStateAccess state)
            throws InstallException {

        String processedInput = null;
        String defValue = processDefaultValFromAllSources(state);

        if ((userInput == null) || (userInput.trim().length() == 0)) {
            if ((defValue != null) && (defValue.length() > 0)) {
                processedInput = defValue;
            } else {
                processedInput = STR_IN_EMPTY_STRING;
            }
        } else {
            processedInput = userInput;
        }

        return processedInput;
    }

    /**
     * Validate user input
     * 
     * @param procInput
     *            processed user input
     * @param state
     *            IStateAccess interface
     * @param isInter
     *            is interactive mode
     * 
     * @return CumulativeValResult
     * 
     * @throws InstallException
     * 
     */
    public CumulativeValResult processValidators(String procInput,
            IStateAccess state, boolean isInter) throws InstallException {
        boolean exitStatus = false;
        boolean returnStat = false;
        CumulativeValResult cumulRes = new CumulativeValResult();

        ValidationInfo[] validators = getInteractionInfo().getValidations();
        ValidationResult validRes = null;

        for (int i = 0; i < validators.length; i++) {
            ValidationInfo validInfo = (ValidationInfo) validators[i];
            IValidation validator = loadValidatorObject(validInfo);
            String validatorName = validInfo.getName();
            Map props = validInfo.getPropertiesMap();
            if (validator != null) {
                if ((validatorName != null) && (validatorName.length() > 0)) {
                    validRes = validator.validate(validatorName, procInput,
                            props, state);

                    if (validRes != null) {
                      switch (validRes.getStatus().getIntValue()) {
                        case ValidationResultStatus.INT_STATUS_SUCCESS:
                            if ((validRes.getData() != null)
                                    && (validRes.getData().size() > 0)) {
                                cumulRes.getCalcKeyValPairs().putAll(
                                        validRes.getData());
                            }
                            break;
                        case ValidationResultStatus.INT_STATUS_WARNING:
                             if (validRes.getMessage() != null) {
                                     cumulRes.setWarningMessage(validRes
                                                               .getMessage());
                                 Debug.log(validRes.getMessage().toString());
                                 if ((validRes.getData() != null)
                                    && (validRes.getData().size() > 0)) {
                                    cumulRes.getCalcKeyValPairs().putAll(
                                             validRes.getData());
                                 }
                            }
                            break;
                        case ValidationResultStatus.INT_STATUS_FAILED:
                            if (validRes.getMessage() != null) {
                                if (isInter) {
                                    // Only one error message is possible
                                    cumulRes.setErrorMessage(validRes
                                            .getMessage());
                                }
                                Debug.log(validRes.getMessage().toString());
                            }
                            exitStatus = true;
                            break;
                        default:
                        }
                    }
                }
            }

            if (exitStatus) {
                break;
            }
        }

        if (validRes != null) {
            if (validRes.getStatus().getIntValue() != 
                ValidationResultStatus.INT_STATUS_FAILED) {
                returnStat = true;
            }
        }

        cumulRes.setCumValResult(returnStat);
        return cumulRes;

    }

    /*
     * Helper method to load validator object
     * 
     */
    private IValidation loadValidatorObject(ValidationInfo validInfo)
            throws InstallException {

        IValidation validator = null;
        if (validInfo != null) {
            String validatorClass = validInfo.getClassName();
            if (validatorClass != null) {
                try {
                    validator = (IValidation) Class.forName(validatorClass)
                            .newInstance();
                } catch (Exception ex) {
                    Debug.log("Failed to instantiate validator class = "
                            + validatorClass + " : ", ex);
                    LocalizedMessage lMessage = LocalizedMessage.get(
                            LOC_IN_ERR_INTERACTION_RUN,
                            new Object[] { getKey() });
                    throw new InstallException(lMessage, ex);
                }
            }
        }

        return validator;
    }

    /**
     * 
     * Calculate default value for the interaction. If the default value is 
     * null or empty string, it will be returned to the caller for further
     * processing.
     * 
     * @param state IStateAccess interface 
     * 
     * @return String default value for the interaction
     * 
     * @throws InstallException
     * 
     */
    public String processDefaultValFromAllSources(IStateAccess state)
            throws InstallException {

        String result = null;
        result = (String) state.get(getKey());
        if (result == null) {
            DefaultValueFinderInfo defInfo = getInteractionInfo()
                    .getDefaultValueFinderInfo();
            if (defInfo != null) {
                String defaultVal = defInfo.getValue();
                String defValFinderClassName = defInfo.getClassName();
                if ((defValFinderClassName != null)
                        && (defValFinderClassName.length() > 0)) {
                    try {
                        IDefaultValueFinder defValFinder = 
                            (IDefaultValueFinder) Class.forName(
                                    defValFinderClassName).newInstance();
                        if (defValFinder != null) {
                            result = defValFinder.getDefaultValue(getKey(),
                                    state, defaultVal);
                        }
                    } catch (Exception ex) {
                        Debug.log(
                                "Failed to load Default value finder class = "
                                        + defValFinderClassName + " : ", ex);
                        LocalizedMessage lMessage = LocalizedMessage.get(
                                LOC_IN_ERR_INTERACTION_RUN,
                                new Object[] { getKey() });
                        throw new InstallException(lMessage);
                    }
                }
            }
        }

        return result;
    }

    /**
     * 
     * Calculate default value for the interaction from state only. This
     * function will be called for isReq=false
     * 
     * @param state IStateAccess interface 
     * 
     * @return String default value for the interaction
     * 
     * @throws InstallException
     * 
     */
    public String processDefaultValFromStateOnly(IStateAccess state)
            throws InstallException {

        String result = null;
        result = (String) state.get(getKey());
        return result;
    }

    /**
     * 
     * Clear value from state for this interaction since user entered empty
     * input
     * 
     * @param state IStateAccess interface @return boolean
     * 
     * @throws InstallException
     * 
     */
    public void clearValueInState(IStateAccess state) throws InstallException {
        state.put(getKey(), STR_IN_EMPTY_STRING);

    }

    public LocalizedMessage getSummaryDescription() {
        return getMessage(STR_IN_SUMMARY_DESCRIPTION_SUFFIX);
    }

    public String getKey() {
        return getInteractionInfo().getLookupKey();
    }

    public DefaultValueFinderInfo getDefaultValueFinderInfo() {
        return getInteractionInfo().getDefaultValueFinderInfo();
    }

    public LocalizedMessage getHelp() {
        return getMessage(STR_IN_HELP_SUFFIX);
    }

    public LocalizedMessage getPromptSuffix() {
        return getMessage(STR_IN_PROMPT_SUFFIX);
    }

    public LocalizedMessage getDescription() {
        return getMessage(STR_IN_DESCRIPTION_SUFFIX);
    }

    public LocalizedMessage getSummaryDesc() {
        return getMessage(STR_IN_SUMMARY_DESCRIPTION_SUFFIX);
    }

    public LocalizedMessage getError() {
        return getMessage(STR_IN_ERROR_SUFFIX);
    }
    
    public LocalizedMessage getWarning() {
        return getMessage(STR_IN_WARNING_SUFFIX);
    }

    public LocalizedMessage getMessage(String suffix) {
        String key = getInteractionInfo().getI18NInfo().getKey() + suffix;
        String group = getInteractionInfo().getI18NInfo().getGroup();

        return LocalizedMessage.get(key, group);
    }

    public boolean isFirst() {
        return isFirst;
    }

    public boolean isPersistent() {
        return getInteractionInfo().isPersistent();
    }

    public void setIsFirstFlag(boolean flag) {
        isFirst = flag;
    }

    public InteractionInfo getInteractionInfo() {
        return info;
    }

    public void setInteractionInfo(InteractionInfo info)
            throws InstallException {
        this.info = info;
        setValueNormalizer(info);
    }

    private IValueNormalizer getValueNormalizer() {
        return valueNormalizer;
    }

    private void setValueNormalizer(InteractionInfo interactionInfo)
            throws InstallException {
        // Initialize the IValueNormalizerClass
        String className = interactionInfo.getValueNormalizerClass();
        if (className != null) {
            try {
                valueNormalizer = (IValueNormalizer) Class.forName(className)
                        .newInstance();
            } catch (Exception ex) {
                Debug.log("UserDataInteraction.setValueNormalizer: "
                        + "Failed to set IValueNormalizer: " + className, ex);
                throw new InstallException(LocalizedMessage
                        .get(LOC_IN_ERR_FAILED_TO_CREATE_INTER), ex);
            }
        }
    }

    protected String getNormalizedValue(String input) throws InstallException {
        String result = input;
        if (getValueNormalizer() != null) {
            try {
                result = getValueNormalizer().getCanonicalForm(input);
            } catch (Exception ex) {
                Debug.log("UserDataInteraction.getNormalizedValue: input: "
                        + input + ", processing failed with exception", ex);
                throw new InstallException(LocalizedMessage
                        .get(LOC_IN_ERR_INTERACTION_RUN), ex);
            }
            Debug.log("UserDataInteraction.getNormalizedValue: input: " + input
                    + ", normalized value: " + result);
        }

        return result;
    }

    private InteractionInfo info;

    private boolean isFirst = false;

    private IValueNormalizer valueNormalizer;
}
