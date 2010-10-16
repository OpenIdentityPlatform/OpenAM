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
 * $Id: UserOptionItem.java,v 1.2 2008/06/25 05:51:25 qcheng Exp $
 *
 */

package com.sun.identity.install.tools.configurator;

import com.sun.identity.install.tools.util.LocalizedMessage;

/**
 * Class that encapulates a UserOption. A UserOption is an option displayed to
 * the user. One or more of these UserOption's may be a part of an interaction.
 * <br>
 * EXAMPLES: <br>
 * Interaction Example 1: <br>
 * 
 * <pre>
 *   1. Continue with installation 
 *   2. Start Over 
 *   3. Exit
 * </pre>
 * 
 * <br>
 * Here 1, 2 & 3 are display items and the rest of the Strings are localized
 * messages for each of those option. The localized message will be of the
 * format: <br>
 * {0}. Continue with installation <br>
 * Interaction Example 2: <br> [ ? : Help, < : Back, ] <br>
 * Here ? and < are display items. The localized messages will looks like <br>
 * 
 * <pre>
 *   {0} : Help
 *   {0} : Back
 * </pre>
 */
public class UserOptionItem {

    public UserOptionItem(String displayItem, String i18NKey) {
        setDisplayItem(displayItem);
        setDisplayMessage(i18NKey);
    }

    /*
     * This constructor is used by the InstallInteraction to display help
     * messages for options. All other Interactions should not use this
     * constructor until they have a need to display help for the options. If
     * this contructor is used, appropriate i18NKey_HELP messages has to be
     * added to the corresponding locale file.
     * 
     * @param displayItem @param i18NKey @param i18NHelpKey
     * 
     */
    public UserOptionItem(String displayItem, String i18NKey, 
            String i18NHelpKey) {
        setDisplayItem(displayItem);
        setDisplayMessage(i18NKey);
        setHelpMessage(i18NHelpKey);
    }

    public boolean equals(String userInputItem) {
        return getDisplayItem().equals(userInputItem);
    }

    public LocalizedMessage getDisplayMessage() {
        return message;
    }

    public LocalizedMessage getHelpMessage() {
        return helpMessage;
    }

    protected String getDisplayItem() {
        return displayItem;
    }

    private void setDisplayMessage(String i18NKey) {
        Object[] args = { getDisplayItem() };
        message = LocalizedMessage.get(i18NKey, args);
    }

    private void setDisplayItem(String item) {
        displayItem = item;
    }

    private void setHelpMessage(String helpKey) {
        Object[] args = { getDisplayItem() };
        helpMessage = LocalizedMessage.get(helpKey, args);
    }

    private String displayItem;

    private LocalizedMessage message;

    private LocalizedMessage helpMessage;
}
