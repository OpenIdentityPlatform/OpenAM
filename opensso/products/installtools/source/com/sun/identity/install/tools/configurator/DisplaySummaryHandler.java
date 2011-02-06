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
 * $Id: DisplaySummaryHandler.java,v 1.2 2008/06/25 05:51:18 qcheng Exp $
 *
 */

package com.sun.identity.install.tools.configurator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.sun.identity.install.tools.util.LocalizedMessage;

final public class DisplaySummaryHandler extends UserResponseInteraction {

    public DisplaySummaryHandler(boolean installMode) {
        super();
        setUserOptions(installMode);
        setPromptMessage();
        setResultStatusMap();
        setDisplayMessagesHeader();
        setUserOptionsHeader();
    }

    public Map getResultStatusMap() {
        return resultStatusMap;
    }

    public LocalizedMessage getDisplayMessagesHeader() {
        return displayMessagesHeader;
    }

    public ArrayList getDisplayMessages() {
        return displayMessages;
    }

    public LocalizedMessage getUserOptionsHeader() {
        return userOptionsHeader;
    }

    public LocalizedMessage getPromptMessage(IStateAccess state) {
        return promptMessage;
    }

    public BaseOptions getUserOptions(IStateAccess state)
            throws InstallException {
        return options;
    }

    public void setDisplayMessages(ArrayList summaryMessages) {
        displayMessages = summaryMessages;
    }

    private void setPromptMessage() {
        promptMessage = LocalizedMessage.get(LOC_IN_MSG_SUMM_PROMPT);
    }

    private void setUserOptions(boolean installMode) {
        // Initialize MultiLineOptions because the Summary Display user options
        // will be displayed in multiple lines.
        options = new MultiLineOptions();

        // Create the UserOptions & add them to the MultiLineOptions
        UserOptionItem continueOption = new UserOptionItem(
                STR_CONTINUE_DISPLAY_ITEM, getContinueLocaleKey(installMode));
        options.add(continueOption);
        options.add(new UserOptionItem(STR_BACK_DISPLAY_ITEM,
                LOC_IN_MSG_SUMM_BACK));
        options.add(new UserOptionItem(STR_START_OVER_DISPLAY_ITEM,
                LOC_IN_MSG_SUMM_START_OVER));
        options.add(new UserOptionItem(STR_ABORT_DISPLAY_ITEM,
                LOC_IN_MSG_SUMM_ABORT));
        options.setDefaultOption(continueOption);
    }

    private String getContinueLocaleKey(boolean installMode) {
        return (installMode) ? LOC_IN_MSG_SUMM_CONTINUE_INSTALL
                : LOC_IN_MSG_SUMM_CONTINUE_UNINSTALL;
    }

    private void setResultStatusMap() {
        resultStatusMap = new HashMap();

        resultStatusMap.put(STR_CONTINUE_DISPLAY_ITEM,
                InteractionResultStatus.STATUS_CONTINUE);
        resultStatusMap.put(STR_BACK_DISPLAY_ITEM,
                InteractionResultStatus.STATUS_BACK);
        resultStatusMap.put(STR_START_OVER_DISPLAY_ITEM,
                InteractionResultStatus.STATUS_START_OVER);
        resultStatusMap.put(STR_ABORT_DISPLAY_ITEM,
                InteractionResultStatus.STATUS_ABORT);
    }

    private void setUserOptionsHeader() {
        userOptionsHeader = LocalizedMessage.get(LOC_IN_MSG_SUMM_VERIFY);
    }

    private void setDisplayMessagesHeader() {
        displayMessagesHeader = LocalizedMessage.get(LOC_IN_MSG_SUMM_HEADER);
    }

    private Map resultStatusMap;

    private LocalizedMessage userOptionsHeader;

    private LocalizedMessage displayMessagesHeader;

    // An ArrayList of Localized Messages to display before showing options.
    // Can be null OR empty
    private ArrayList displayMessages;

    // All the available Run time options. Can never be null OR empty
    private MultiLineOptions options;

    // The prompt message that will be displayed for the user to input data
    private LocalizedMessage promptMessage;

    private static final String STR_CONTINUE_DISPLAY_ITEM = "1";

    private static final String STR_BACK_DISPLAY_ITEM = "2";

    private static final String STR_START_OVER_DISPLAY_ITEM = "3";

    private static final String STR_ABORT_DISPLAY_ITEM = "4";

    private static final String LOC_IN_MSG_SUMM_HEADER = 
        "IN_MSG_SUMM_HEADER";

    private static final String LOC_IN_MSG_SUMM_VERIFY = 
        "IN_MSG_SUMM_VERIFY";

    private static final String LOC_IN_MSG_SUMM_CONTINUE_INSTALL = 
        "IN_MSG_SUMM_CONTINUE_INSTALL";

    private static final String LOC_IN_MSG_SUMM_CONTINUE_UNINSTALL = 
        "IN_MSG_SUMM_CONTINUE_UNINSTALL";

    private static final String LOC_IN_MSG_SUMM_BACK = "IN_MSG_SUMM_BACK";

    private static final String LOC_IN_MSG_SUMM_START_OVER = 
        "IN_MSG_SUMM_START_OVER";

    private static final String LOC_IN_MSG_SUMM_ABORT = "IN_MSG_SUMM_ABORT";

    private static final String LOC_IN_MSG_SUMM_PROMPT = "IN_MSG_SUMM_PROMPT";

}
