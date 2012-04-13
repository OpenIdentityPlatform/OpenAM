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
 * $Id: UserResponseInteraction.java,v 1.2 2008/06/25 05:51:25 qcheng Exp $
 *
 */

package com.sun.identity.install.tools.configurator;

import java.util.Map;

import com.sun.identity.install.tools.util.Console;
import com.sun.identity.install.tools.util.LocalizedMessage;

abstract public class UserResponseInteraction extends BaseInteraction {

    public UserResponseInteraction() {
        super();
    }

    /**
     * Method that needs to be implemented by the sub-classes to intialize the
     * result status Map. The map should be constructed with key = display item
     * of a UserOption with value of InteractionResultStatus object that
     * corresponds to that UserOption.
     * 
     * @return a Map constructed with <UserOption' display Item,
     *         InteractionResultStatus>.
     */
    abstract protected Map getResultStatusMap();

    abstract LocalizedMessage getDisplayMessagesHeader();

    abstract LocalizedMessage getUserOptionsHeader();

    public InteractionResultStatus interact(IStateAccess state)
            throws InstallException {
        UserOptionItem selectedOption = null;
        InteractionResultStatus status = null;
        do {
            Console.println();
            Console.println();

            // Display the interaction messages
            displayMessages();

            // Now display available options
            Console.println();
            if (getUserOptionsHeader() != null) {
                Console.println(getUserOptionsHeader());
            }

            getUserOptions(state).display();

            // Display the prompt
            displayPromptMessage(state);

            // Now read user inpu
            UserOptionItem defaultOption = getUserOptions(state)
                    .getDefaultOption();
            String userInput = Console.readLine();
            if (userInput != null && userInput.trim().length() > 0) {
                selectedOption = getUserOptions(state).getSelectedOption(
                        userInput);
            } else if (defaultOption != null) { // Choose default if present
                selectedOption = defaultOption;
            }

            // At this point if the selectedOption is still null, print an
            // invalid option message
            if (selectedOption == null) {
                Console.println();
                Console.println(getInvalidOptionMessage());
            } else { // Display confirmation if status is Abort or Exit
                String displayItem = selectedOption.getDisplayItem();
                status = (InteractionResultStatus) getResultStatusMap().get(
                        displayItem);
                status = confirmOnExitStatus(status, state);
            }
        } while (status == null);

        return status;
    }

    public void displayMessages() {
        // Display the header if present
        if (getDisplayMessagesHeader() != null
                && !getDisplayMessages().isEmpty()) { 
            // Display header only if there are messages to display
            Console.println(STR_DISPLAY_HEADER_MARKER);
            Console.println(getDisplayMessagesHeader());
            Console.println(STR_DISPLAY_HEADER_MARKER);
        }
        super.displayMessages();
    }

    protected InteractionResultStatus confirmOnExitStatus(
            InteractionResultStatus status, IStateAccess state)
            throws InstallException {
        InteractionResultStatus finalStatus = status;
        if (status.getIntValue() == InteractionResultStatus.INT_STATUS_ABORT) {
            // Confirm Once again!!
            ConfirmInteraction confirm = new ConfirmInteraction();
            InteractionResultStatus newStatus = confirm.interact(state);
            if (newStatus.getIntValue() == 
                InteractionResultStatus.INT_STATUS_CONTINUE) {
                // Set it to null, so the we can continue the caller's loop.
                finalStatus = null;
            }
        }

        return finalStatus;
    }

    public String getPromptValue(IStateAccess state) throws InstallException {
        String promptValue = null;
        if (getUserOptions(state).getDefaultOption() != null) {
            promptValue = getUserOptions(state).getDefaultOption()
                    .getDisplayItem();
        }
        return promptValue;
    }

    private LocalizedMessage getInvalidOptionMessage() {
        return invalidOptionMessage;
    }

    private static final String LOC_IN_ERR_INVALID_OPTION = 
        "IN_ERR_INVALID_OPTION";

    private LocalizedMessage invalidOptionMessage = LocalizedMessage
            .get(LOC_IN_ERR_INVALID_OPTION);

    private static final String STR_DISPLAY_HEADER_MARKER = 
        "-----------------------------------------------";
}
