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
 * $Id: UserResponseHandler.java,v 1.2 2008/06/25 05:51:25 qcheng Exp $
 *
 */

package com.sun.identity.install.tools.configurator;

import java.util.ArrayList;

import com.sun.identity.install.tools.admin.ToolsConfiguration;
import com.sun.identity.install.tools.util.LocalizedMessage;

class UserResponseHandler extends OrderedPropertyStore {

    /**
     * Constructs an user response object with user input data form the
     * UserResponse file.
     * 
     * @param fileName
     */
    public UserResponseHandler(String fileName) throws InstallException {
        setFile(fileName);
    }

    public void storeData(IStateAccess stateAccess, ArrayList allKeys) {
        int count = allKeys.size();
        for (int i = 0; i < count; i++) {
            String key = (String) allKeys.get(i);
            String value = (String) stateAccess.get(key);
            if (value == null) { // May be a non peristent key
                value = "";
            }
            setProperty(key, value);
        }
    }

    // ////////////////////////////////////////////////////////////////////////
    // Overridden abstract methods
    // ////////////////////////////////////////////////////////////////////////
    public String getFile() {
        return fileName;
    }

    public LocalizedMessage getLoadErrorMessage() {
        Object[] args = { getFile() };
        return LocalizedMessage.get(LOC_DR_ERR_LOAD_USER_RESPONSE, args);
    }

    public LocalizedMessage getSaveErrorMessage() {
        Object[] args = { getFile() };
        return LocalizedMessage.get(LOC_DR_ERR_SAVE_USER_RESPONSE, args);
    }

    public LocalizedMessage getInvalidLineErrorMessage(int lineNumber) {
        Object args[] = { getFile(), new Integer(lineNumber), 
                STR_KEY_VALUE_SEP };
        LocalizedMessage message = LocalizedMessage.get(
                LOC_DR_ERR_INVALID_LINE, args);
        return message;
    }

    public LocalizedMessage getInvalidKeyErrorMessage(int lineNumber) {
        Object args[] = { getFile(), new Integer(lineNumber), 
                STR_KEY_VALUE_SEP };
        LocalizedMessage message = LocalizedMessage.get(
                LOC_DR_ERR_INVALID_KEY_DEFINED, args);
        return message;
    }

    public String getFileHeader() {
        return STR_USER_RESPONSE_FILE_HEADER;
    }

    public String toString() {
        String displayStr = "*** BEGIN UserResponseHandler Data *******"
                + LINE_SEP + super.toString() + LINE_SEP
                + "*** END UserResponseHandler Data *********";
        return displayStr;
    }

    // ////////////////////////////////////////////////////////////////////////

    private void setFile(String fileName) {
        this.fileName = fileName;
    }

    private String fileName;

    // Constants
    private static final String LOC_DR_ERR_INVALID_KEY_DEFINED = 
        "DR_ERR_INVALID_KEY_DEFINED";

    private static final String LOC_DR_ERR_INVALID_LINE = 
        "DR_ERR_INVALID_LINE";

    private static final String LOC_DR_ERR_LOAD_USER_RESPONSE = 
        "DR_ERR_LOAD_USER_RESPONSE";

    private static final String LOC_DR_ERR_SAVE_USER_RESPONSE = 
        "DR_ERR_SAVE_USER_RESPONSE";

    private static final String STR_USER_RESPONSE_FILE_HEADER = "# "
            + ToolsConfiguration.getProductShortName() + " User Response File";
}
