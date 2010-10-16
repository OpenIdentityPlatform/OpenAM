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
 * $Id: ConfirmInteraction.java,v 1.2 2008/06/25 05:51:17 qcheng Exp $
 *
 */

package com.sun.identity.install.tools.configurator;

import java.util.ArrayList;

import com.sun.identity.install.tools.util.Console;
import com.sun.identity.install.tools.util.LocalizedMessage;

public class ConfirmInteraction extends BaseInteraction {

    public ConfirmInteraction() {
        super();
        setUserOptions();
        setPromptMessage();
    }

    public ArrayList getDisplayMessages() {
        return null;
    }

    public String getPromptValue(IStateAccess state) throws InstallException {
        return getUserOptions(state).getDefaultOption().getDisplayItem();
    }

    public LocalizedMessage getPromptMessage(IStateAccess state) {
        return promptMessage;
    }

    public BaseOptions getUserOptions(IStateAccess state)
            throws InstallException {
        return options;
    }

    public InteractionResultStatus interact(IStateAccess state)
            throws InstallException {
        UserOptionItem selectedOption = null;

        do {
            // Now display available options
            Console.println();
            getUserOptions(state).display();

            // Display the prompt                      
            displayPromptMessage(state);

            // Now read user input
            String userInput = Console.readLine();
            if (userInput != null && userInput.trim().length() > 0) {
                selectedOption = getUserOptions(state).getSelectedOption(
                        userInput);
            } else if (getUserOptions(state).getDefaultOption() != null) {
                // Choose default if present
                selectedOption = getUserOptions(state).getDefaultOption();
            }

            // At this point if the selectedOption is still null, print an 
            // invalid option message
            if (selectedOption == null) {
                Console.println();
                Console.println(getInvalidOptionMessage());
            }

        } while (selectedOption == null);

        return getResultStatus(selectedOption);
    }

    private InteractionResultStatus getResultStatus(UserOptionItem option) {
        InteractionResultStatus status = 
            InteractionResultStatus.STATUS_CONTINUE;
        if (option.getDisplayItem().equals(STR_CONFIRM_YES_DISPLAY_ITEM)) {
            status = InteractionResultStatus.STATUS_ABORT;
        }
        return status;
    }

    private LocalizedMessage getInvalidOptionMessage() {
        return invalidOptionMessage;
    }

    private void setPromptMessage() {
        promptMessage = LocalizedMessage.get(LOC_IN_MSG_CONFIRM_PROMPT);
    }

    private void setUserOptions() {
        options = new SingleLineOptions();

        UserOptionItem yesOption = new UserOptionItem(
                STR_CONFIRM_YES_DISPLAY_ITEM, LOC_IN_MSG_CONFIRM_EXIT);
        UserOptionItem noOption = new UserOptionItem(
                STR_CONFIRM_NO_DISPLAY_ITEM, LOC_IN_MSG_CONFIRM_CONTINUE);

        options.add(yesOption);
        options.add(noOption);
        options.setDefaultOption(noOption);
        options.setIgnoreCaseFlag(true);
        options.setSeparator(SingleLineOptions.STR_COMMA_DELIMITER);
    }

    // All the available Run time options. Can never be null OR empty
    private SingleLineOptions options;

    // The prompt message that will be displayed for the user to input data
    private LocalizedMessage promptMessage;

    private LocalizedMessage invalidOptionMessage = 
        LocalizedMessage.get(LOC_IN_ERR_INVALID_OPTION);

    private static final String STR_CONFIRM_YES_DISPLAY_ITEM = "yes";

    private static final String STR_CONFIRM_NO_DISPLAY_ITEM = "no";

    private static final String LOC_IN_MSG_CONFIRM_EXIT = 
        "IN_MSG_CONFIRM_EXIT";

    private static final String LOC_IN_MSG_CONFIRM_CONTINUE = 
        "IN_MSG_CONFIRM_CONTINUE";

    private static final String LOC_IN_MSG_CONFIRM_PROMPT = 
        "IN_MSG_CONFIRM_PROMPT";

    private static final String LOC_IN_ERR_INVALID_OPTION = 
        "IN_ERR_INVALID_OPTION";
}
