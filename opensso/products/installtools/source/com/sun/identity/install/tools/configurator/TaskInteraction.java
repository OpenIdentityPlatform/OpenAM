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
 * $Id: TaskInteraction.java,v 1.2 2008/06/25 05:51:24 qcheng Exp $
 *
 */

package com.sun.identity.install.tools.configurator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.sun.identity.install.tools.util.LocalizedMessage;

public class TaskInteraction extends UserResponseInteraction {

    public TaskInteraction() {
        super();
        setUserOptions();
        setPromptMessage();
        setResultStatusMap();
    }

    public LocalizedMessage getDisplayMessagesHeader() {
        return null;
    }

    public ArrayList getDisplayMessages() {
        return null;
    }

    public LocalizedMessage getPromptMessage(IStateAccess state) {
        return promptMessage;
    }

    public LocalizedMessage getUserOptionsHeader() {
        return null;
    }

    public BaseOptions getUserOptions(IStateAccess state)
            throws InstallException {
        return options;
    }

    public Map getResultStatusMap() {
        return resultStatusMap;
    }

    private void setUserOptions() {
        // Initialize MultiLineOptions because the Summary Display user options
        // will be displayed in multiple lines.
        options = new MultiLineOptions();

        // Create the UserOptions & add them to the MultiLineOptions
        UserOptionItem continueOptionItem = new UserOptionItem(
                STR_TASKS_CONTINUE_DISPLAY_ITEM, LOC_IN_MSG_TASKS_CONTINUE);
        options.add(continueOptionItem);
        options.add(new UserOptionItem(STR_TASKS_UNDO_DISPLAY_ITEM,
                LOC_IN_MSG_TASKS_UNDO));
        options.add(new UserOptionItem(STR_TASKS_ABORT_DISPLAY_ITEM,
                LOC_IN_MSG_TASKS_ABORT));

        options.setDefaultOption(continueOptionItem);
    }

    public void setResultStatusMap() {
        resultStatusMap = new HashMap();
        // NOTE: BACK Result status => Undo for tasks
        resultStatusMap.put(STR_TASKS_CONTINUE_DISPLAY_ITEM,
                InteractionResultStatus.STATUS_CONTINUE);
        resultStatusMap.put(STR_TASKS_UNDO_DISPLAY_ITEM,
                InteractionResultStatus.STATUS_BACK);
        resultStatusMap.put(STR_TASKS_ABORT_DISPLAY_ITEM,
                InteractionResultStatus.STATUS_ABORT);
    }

    private void setPromptMessage() {
        promptMessage = LocalizedMessage.get(LOC_IN_MSG_TASKS_PROMPT);
    }

    // Result Status Map
    private Map resultStatusMap;

    // All the available Run time options. Can never be null OR empty
    private MultiLineOptions options;

    // The prompt message that will be displayed for the user to input data
    private LocalizedMessage promptMessage;

    private static final String STR_TASKS_CONTINUE_DISPLAY_ITEM = "1";

    private static final String STR_TASKS_UNDO_DISPLAY_ITEM = "2";

    private static final String STR_TASKS_ABORT_DISPLAY_ITEM = "3";

    private static final String LOC_IN_MSG_TASKS_CONTINUE = 
        "IN_MSG_TASKS_CONTINUE";

    private static final String LOC_IN_MSG_TASKS_UNDO = 
        "IN_MSG_TASKS_UNDO";

    private static final String LOC_IN_MSG_TASKS_ABORT = "IN_MSG_TASKS_ABORT";

    private static final String LOC_IN_MSG_TASKS_PROMPT = 
        "IN_MSG_TASKS_PROMPT";
}
