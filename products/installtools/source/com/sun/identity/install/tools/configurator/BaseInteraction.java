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
 * $Id: BaseInteraction.java,v 1.2 2008/06/25 05:51:17 qcheng Exp $
 *
 */

package com.sun.identity.install.tools.configurator;

import java.util.ArrayList;

import com.sun.identity.install.tools.util.Console;
import com.sun.identity.install.tools.util.LocalizedMessage;

abstract public class BaseInteraction {

    public BaseInteraction() {
    }

    /**
     * Method that needs to be implemented by the sub-classes to return 
     * the Localized Messages that need to be displayed as part of the 
     * interaction.
     * 
     * @return ArrayList of LocalizedMessage objects or empty or null ArrayList
     */
    abstract public ArrayList getDisplayMessages();

    /**
     * Method that needs to be implemented by the sub-classes to return 
     * the ResponseOption's and return the appropriate BaseOptions. * viz., 
     * SingleLineOptions & MutliLineOptions. 
     *   
     * @param state IStateAccess
     * 
     * 
     * @return a BaseOptions with ResponseOption's populated.
     * @throws InstallException if an error occurred while getting the user 
     * options
     */
    abstract public BaseOptions getUserOptions(IStateAccess state)
            throws InstallException;

    /**
     * Method that needs to be implemented by the sub-classes to return the 
     * prompt message that needs to be displayed for the user to input data.
     * 
     * @param state IStateAccess
     * 
     * @return a LocalizedMessage Prompt
     */
    abstract public LocalizedMessage getPromptMessage(IStateAccess state);

    /**
     * Method that needs to be implemented by the sub-classes to return a 
     * prompt value (default value) that needs to be displayed for the user to
     * input data. If the prompt value other than null is returned then the 
     * prompt message will be some thing as in Example shown below:
     * Enter the Server Port [8080]: 
     * 
     * @param state IStateAccess
     * 
     * 
     * @return a String containing the default value or null if no value is 
     * present.
     * @throws InstallException TODO
     */
    abstract public String getPromptValue(IStateAccess state)
            throws InstallException;

    /**
     *  This method should not be called until the interaction initializes
     *  the UserOptionItem with three param constructor. Presently, only
     *  InstallInteraction uses this method.
     *  
     *  @param state the IStateAcess
     * 
     */
    public void displayOptionsHelp(IStateAccess state) throws InstallException 
    {
        ArrayList list = (ArrayList) getUserOptions(state)
            .getResponseOptions();
        for (int i = 0; i < list.size(); i++) {
            UserOptionItem opt = (UserOptionItem) list.get(i);
            if (opt.getHelpMessage() != null) {
                Console.println(opt.getHelpMessage(), new Object[] { 
                    opt.getDisplayItem() });
            }
        }
    }

    public void displayMessages() {
        if (getDisplayMessages() != null && !getDisplayMessages().isEmpty()) {
            int count = getDisplayMessages().size();
            for (int i = 0; i < count; i++) {
                LocalizedMessage message = (LocalizedMessage) 
                    getDisplayMessages().get(i);
                if (message != null) {
                    Console.println(message);
                }
            }
        }
    }

    protected void displayPromptMessage(IStateAccess state)
            throws InstallException {
        String displayMessage = null;
        if (getPromptValue(state) != null) {
            displayMessage = getPromptMessage(state).toString()
                    + STR_PROMPT_VALUE_START_DELIMITER + getPromptValue(state)
                    + STR_PROMPT_VALUE_END_DELIMITER;
        } else {
            displayMessage = getPromptMessage(state).toString()
                    + STR_PROMPT_DELIMITER;
        }
        Console.print(displayMessage);
    }

    private static final String STR_PROMPT_DELIMITER = ":";

    private static final String STR_PROMPT_VALUE_START_DELIMITER = " [";

    private static final String STR_PROMPT_VALUE_END_DELIMITER = "]:";
}
