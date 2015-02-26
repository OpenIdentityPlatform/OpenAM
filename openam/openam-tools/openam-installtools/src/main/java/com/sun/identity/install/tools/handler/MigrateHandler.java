/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: MigrateHandler.java,v 1.2 2008/06/25 05:51:26 qcheng Exp $
 *
 */

/*
 * Portions Copyrighted [2010] [ForgeRock AS]
 */
package com.sun.identity.install.tools.handler;

import com.sun.identity.install.tools.admin.IToolsOptionHandler;
import com.sun.identity.install.tools.configurator.InstallAbortException;
import com.sun.identity.install.tools.configurator.InstallConstants;
import com.sun.identity.install.tools.configurator.MultipleMigrateDriver;
import com.sun.identity.install.tools.configurator.InstallException;
import com.sun.identity.install.tools.configurator.InstallLogger;
import com.sun.identity.install.tools.util.Console;
import com.sun.identity.install.tools.util.Debug;
import com.sun.identity.install.tools.util.LocalizedMessage;
import java.util.List;

/**
 * Migration handler, part of install framework.
 *
 */
public class MigrateHandler extends ConfigHandlerBase implements
        IToolsOptionHandler, InstallConstants {
    
    /** 
     * Constructor.
     */
    public MigrateHandler() {
        super();
    }
    
    /**
     * to make sure that migrate has no additional parameter.
     * @param arguments
     * @return true if there's no additional parameter, false otherwise.
     */
    public boolean checkArguments(List arguments) {
        // There should be no arguments to this option. If there is any it
        // would an error.
        boolean validArgs = true;
        if (arguments != null && arguments.size() > 0) {
            String specifiedArgs = formatArgs(arguments);
            Debug.log("MigrateHandler: invalid argument(s) specified - "
                    + specifiedArgs);
            printConsoleMessage(LOC_HR_MSG_INVALID_OPTION,
                    new Object[] { specifiedArgs });
            validArgs = false;
        }
        return validArgs;
    }
    
    /**
     * handle the request of migration.
     * @param arguments
     */
    public void handleRequest(List arguments) {
        
        try {
            // Create an InstallLogger
            InstallLogger migrateLog = new InstallLogger(STR_MIGRATE);
            MultipleMigrateDriver driver = new MultipleMigrateDriver();
            if (arguments.isEmpty()) {
                Debug.log("MigrateHandler: invoked with 0 arguments.");
                driver.migrate(migrateLog);
            } else if (arguments.size() == 2) {
                Debug.log("MigrateHandler: invoked with 1 or more arguments.");
                driver.migrate(migrateLog);
                
            }
        } catch (InstallAbortException ia) {
            Debug.log("MigrateHandler: User Requested Abort ", ia);
            Console.printlnRawText(ia.getMessage());
        } catch (InstallException ex) {
            Debug.log("MigrateHandler: Failed to process migrate request ",
                    ex);
            printConsoleMessage(LOC_HR_ERR_MIGRATE, new Object[] { ex
                    .getMessage() });
        }
    }
    
    /** 
     * display help message during migration.
     */
    public void displayHelp() {
        Console.println();
        Console.println(LocalizedMessage.get(LOC_HR_MSG_MIGRATE_USAGE_DESC));
        Console.println();
    }
    
    /* keys for migration messages. */
    public static final String LOC_HR_ERR_MIGRATE = "HR_ERR_MIGRATE";
    
    public static final String LOC_HR_MSG_MIGRATE_USAGE_DESC =
            "HR_MSG_MIGRATE_USAGE_DESC";
    
}
