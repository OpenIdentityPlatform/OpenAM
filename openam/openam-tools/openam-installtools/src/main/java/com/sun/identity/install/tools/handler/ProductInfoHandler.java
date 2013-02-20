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
 * $Id: ProductInfoHandler.java,v 1.2 2008/06/25 05:51:26 qcheng Exp $
 *
 */

/*
 * Portions Copyrighted [2010] [ForgeRock AS]
 */
package com.sun.identity.install.tools.handler;

import java.util.ArrayList;
import java.util.Map;
import com.sun.identity.install.tools.admin.IToolsOptionHandler;
import com.sun.identity.install.tools.configurator.InstFinderStore;
import com.sun.identity.install.tools.configurator.InstallException;
import com.sun.identity.install.tools.util.Console;
import com.sun.identity.install.tools.util.Debug;
import com.sun.identity.install.tools.util.LocalizedMessage;
import java.util.List;

public class ProductInfoHandler extends ProductInfoHandlerBase implements
        IToolsOptionHandler {
    public boolean checkArguments(List arguments) {
        // There should be no arguments to this option. If there is any it
        // would an error.
        boolean validArgs = true;
        if (arguments.size() != 1) {
            String specifiedArgs = formatArgs(arguments);
            Debug.log("ProductInfoHandler: invalid argument(s) specified "
                    + "- " + specifiedArgs);
            printConsoleMessage(LOC_HR_MSG_INVALID_OPTION,
                    new Object[] { specifiedArgs });
            validArgs = false;
        }
        return validArgs;
    }

    public void displayHelp() {
        Console.println();
        Console.println(LocalizedMessage.get(
                LOC_HR_MSG_PRODUCTINFO_USAGE_DESC));
        Console.println();
        Console.println(LocalizedMessage.get(
                LOC_HR_MSG_PRODUCTINFO_USAGE_HELP));
        Console.println();
        Console.println(LocalizedMessage
                .get(LOC_HR_MSG_PRODUCTINFO_USAGE_EXAMPLE));
        Console.println();
    }

    public void handleRequest(List arguments) {
        try {
            String instanceName = (String) arguments.get(0);

            ArrayList summaryKeys = getIFinderSummaryKeys();
            InstFinderStore iFinderStore = InstFinderStore.getInstance();
            Map productDetails = iFinderStore.getProductDetails(instanceName,
                    summaryKeys);

            if (productDetails == null || productDetails.isEmpty()) {
                printConsoleMessage(LOC_HR_MSG_PRODUCTINFO_NOT_FOUND,
                        new Object[] { instanceName });
            } else { // Print out the summary details
                printProductDetails(productDetails, instanceName);
            }
        } catch (InstallException ie) {
            Debug.log("ProductInfoHandler: Failed to handle request ", ie);
            printConsoleMessage(LOC_HR_ERR_PRODUCTINFO, new Object[] { ie
                    .getMessage() });
        }
    }

    public static final String LOC_HR_MSG_INVALID_OPTION = 
        "HR_MSG_INVALID_OPTION";

    public static final String LOC_HR_MSG_PRODUCTINFO_USAGE_DESC = 
        "HR_MSG_PRODUCTINFO_USAGE_DESC";

    public static final String LOC_HR_MSG_PRODUCTINFO_USAGE_HELP = 
        "HR_MSG_PRODUCTINFO_USAGE_HELP";

    public static final String LOC_HR_MSG_PRODUCTINFO_USAGE_EXAMPLE = 
        "HR_MSG_PRODUCTINFO_USAGE_EXAMPLE";

    public static final String LOC_HR_ERR_PRODUCTINFO = "HR_ERR_PRODUCTINFO";

    public static final String LOC_HR_MSG_PRODUCTINFO_NOT_FOUND = 
        "HR_MSG_PRODUCTINFO_NOT_FOUND";
}
