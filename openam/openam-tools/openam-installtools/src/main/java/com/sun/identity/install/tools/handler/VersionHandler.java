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
 * $Id: VersionHandler.java,v 1.2 2008/06/25 05:51:27 qcheng Exp $
 *
 */

/*
 * Portions Copyrighted [2010] [ForgeRock AS]
 */
package com.sun.identity.install.tools.handler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import com.sun.identity.install.tools.admin.IToolsOptionHandler;
import com.sun.identity.install.tools.configurator.InstallConstants;
import com.sun.identity.install.tools.util.Console;
import com.sun.identity.install.tools.util.Debug;
import com.sun.identity.install.tools.util.LocalizedMessage;
import java.util.List;

public class VersionHandler extends ConfigHandlerBase implements
        IToolsOptionHandler, InstallConstants {

    public VersionHandler() {
        super();
    }

    public boolean checkArguments(List arguments) {
        // There should be no arguments to this option. If there is any it
        // would an error.
        boolean validArgs = true;
        if (arguments != null && arguments.size() > 0) {
            String specifiedArgs = formatArgs(arguments);
            Debug.log("VersionHandler: invalid argument(s) specified - "
                    + specifiedArgs);
            printConsoleMessage(LOC_HR_MSG_INVALID_OPTION,
                    new Object[] { specifiedArgs });
            validArgs = false;
        }
        return validArgs;
    }

    public void handleRequest(List arguments) {
        BufferedReader br = null;
        try {
            InputStream is = this.getClass().getResourceAsStream(
                    STR_VERSION_FILE_NAME);
            br = new BufferedReader(new InputStreamReader(is));
            String line = null;
            while ((line = br.readLine()) != null) {
                // Print only the non hashed lines
                if (!line.startsWith("#")) {
                    Console.printlnRawText(line);
                }
            }
        } catch (Exception ex) {
            Debug.log("VersionHandler: An exception occurred while reading "
                    + "version file: ", ex);
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException io) {
                    // Ignore
                }
            }
        }
    }

    public void displayHelp() {
        Console.println();
        Console.println(LocalizedMessage.get(LOC_HR_MSG_VERSION_USAGE_DESC));
        Console.println();
    }

    public static final String LOC_HR_MSG_VERSION_USAGE_DESC = 
        "HR_MSG_VERSION_USAGE_DESC";

    public static final String LOC_HR_MSG_INVALID_OPTION = 
        "HR_MSG_INVALID_OPTION";
}
