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
 * $Id: ConfigHandlerBase.java,v 1.2 2008/06/25 05:51:25 qcheng Exp $
 *
 */

/*
 * Portions Copyrighted [2010] [ForgeRock AS]
 */
package com.sun.identity.install.tools.handler;

import java.io.File;
import java.io.IOException;
import com.sun.identity.install.tools.util.Console;
import com.sun.identity.install.tools.util.Debug;
import com.sun.identity.install.tools.util.LocalizedMessage;
import java.util.List;

abstract public class ConfigHandlerBase {

    protected boolean verifyArguments(List arguments) {
        // Arguments to the -install option can be:
        // 1. <empty>
        // 2. -useResponses <responseFile>
        // 3. -saveResponses <responseFile>
        //         
        boolean isCheckSuccessful = true;
        if (arguments.size() == 2) {
            Debug.log("ConfigHandlerBase: invoked with 2 arguments - "
                    + arguments);
            String type = (String) arguments.get(0);
            String responseFileName = (String) arguments.get(1);
            if ((type != null) && type.trim().equals(STR_SAVE_RESPONSES)) {
                isCheckSuccessful = isValidSaveStateFile(responseFileName);
            } else if ((type != null) && type.trim().equals(STR_USE_RESPONSES))
            {
                isCheckSuccessful = isValidUseStateFile(responseFileName);
            } else {
                Debug.log("ConfigHandlerBase: Error - invalid option " + type
                        + "specified.");
                printConsoleMessage(LOC_HR_MSG_INVALID_OPTION,
                        new Object[] { formatArgs(arguments) });
                isCheckSuccessful = false;
            }
        } else if (arguments.size() == 1 || arguments.size() > 2) {
            Debug.log("ConfigHandlerBase: Error - invoked with "
                    + "incomplete/invald arguments.");
            printConsoleMessage(LOC_HR_MSG_INVALID_OPTION,
                    new Object[] { formatArgs(arguments) });
            isCheckSuccessful = false;
        }

        return isCheckSuccessful;
    }

    protected boolean isValidSaveStateFile(String fileName) {
        boolean validationSuccess = false;
        if ((fileName == null) || (fileName.trim().length() == 0)) {
            printConsoleMessage(LOC_HR_ERR_RESP_FILE_NOT_SPECIFIED,
                    new Object[] { STR_SAVE_RESPONSES });
        } else {
            File file = new File(fileName);
            if (!file.exists()) {
                // Test it out.
                try {
                    boolean created = file.createNewFile();
                    if (!created) {
                        // Just a test. So, we don't end up with error at the
                        // end of the install
                        printConsoleMessage(LOC_HR_ERR_RESP_FILE_EXISTS,
                                new Object[] { fileName });
                    } else {
                        // Delete this file. We will re-create later. Otherwise
                        // this file will be not be cleaned even if the
                        // installer aborts.
                        file.delete();
                        // Set the flag only after delete.
                        validationSuccess = true;
                    }
                } catch (IOException ie) {
                    printConsoleMessage(LOC_HR_ERR_RESP_FILE_CREATE,
                            new Object[] { fileName });
                }
            }
        }
        return validationSuccess;
    }

    protected boolean isValidUseStateFile(String fileName) {
        boolean validationSuccess = false;
        if ((fileName == null) || (fileName.trim().length() == 0)) {
            printConsoleMessage(LOC_HR_ERR_RESP_FILE_NOT_SPECIFIED,
                    new Object[] { STR_USE_RESPONSES });
        } else {
            File file = new File(fileName);
            if (!file.exists()) {
                printConsoleMessage(LOC_HR_ERR_RESP_FILE_NOT_FOUND,
                        new Object[] { fileName });
            } else if (!file.canRead()) {
                printConsoleMessage(LOC_HR_ERR_RESP_FILE_READ,
                        new Object[] { fileName });
            } else if (file.isDirectory()) {
                printConsoleMessage(LOC_ERR_RESP_FILE_IS_DIR,
                        new Object[] { fileName });
            } else {
                validationSuccess = true;
            }
        }
        return validationSuccess;
    }

    protected String formatArgs(List arguments) {
        int count = arguments.size();
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < count; i++) {
            String arg = (String) arguments.get(i);
            sb.append(arg).append(" ");
        }
        return sb.toString();
    }

    protected void printConsoleMessage(String message) {
        printConsoleMessage(message, null);
    }

    protected void printConsoleMessage(String message, Object[] args) {
        LocalizedMessage lMessage;
        if (args != null) {
            lMessage = LocalizedMessage.get(message, args);
        } else {
            lMessage = LocalizedMessage.get(message);
        }
        Console.println();
        Console.println(lMessage);
        Console.println();
    }

    public static final String LOC_HR_ERR_RESP_FILE_EXISTS = 
        "HR_ERR_RESP_FILE_EXISTS";

    public static final String LOC_HR_ERR_RESP_FILE_NOT_FOUND = 
        "HR_ERR_RESP_FILE_NOT_FOUND";

    public static final String LOC_HR_ERR_RESP_FILE_CREATE = 
        "HR_ERR_RESP_FILE_CREATE";

    public static final String LOC_HR_ERR_RESP_FILE_READ = 
        "HR_ERR_RESP_FILE_READ";
    
    public static final String LOC_ERR_RESP_FILE_IS_DIR = 
        "LOC_ERR_RESP_FILE_IS_DIR";

    public static final String LOC_HR_ERR_RESP_FILE_NOT_SPECIFIED = 
        "HR_ERR_RESP_FILE_NOT_SPECIFIED";

    public static final String LOC_HR_MSG_INVALID_OPTION = 
        "HR_MSG_INVALID_OPTION";

    public static final String STR_SAVE_RESPONSES = "--saveResponse";

    public static final String STR_USE_RESPONSES = "--useResponse";
}
