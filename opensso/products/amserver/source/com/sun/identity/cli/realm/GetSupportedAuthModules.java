/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: GetSupportedAuthModules.java,v 1.4 2008/06/25 05:42:16 qcheng Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.cli.realm;

import com.iplanet.sso.SSOToken;
import com.sun.identity.authentication.config.AMAuthenticationManager;
import com.sun.identity.authentication.config.AMConfigurationException;
import com.sun.identity.cli.AuthenticatedCommand;
import com.sun.identity.cli.CLIException;
import com.sun.identity.cli.ExitCodes;
import com.sun.identity.cli.IOutput;
import com.sun.identity.cli.LogWriter;
import com.sun.identity.cli.RequestContext;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;

/**
 * This command gets the supported authentication modules.
 */
public class GetSupportedAuthModules extends AuthenticatedCommand {
    /**
     * Services a Commandline Request.
     *
     * @param rc Request Context.
     * @throws CLIException if the request cannot serviced.
     */
    public void handleRequest(RequestContext rc) 
        throws CLIException {
        super.handleRequest(rc);
        ldapLogin();
        
        SSOToken adminSSOToken = getAdminSSOToken();
        IOutput outputWriter = getOutputWriter();
        String[] params = {};
        writeLog(LogWriter.LOG_ACCESS, Level.INFO,
            "ATTEMPT_GET_SUPPORTED_AUTH_MODULES", params);

        try {
            AMAuthenticationManager mgr = new AMAuthenticationManager(
                adminSSOToken, "/");
            Set types = mgr.getAuthenticationTypes();

            if ((types != null) && !types.isEmpty()) {
                Set sorted = new TreeSet();
                sorted.addAll(types);

                for (Iterator iter = sorted.iterator(); iter.hasNext(); ) {
                    String type = (String)iter.next();
                    outputWriter.printlnMessage(type);
                }
            } else {
                outputWriter.printlnMessage(getResourceString(
                    "get-supported-no-supported-authtype"));
            }

            writeLog(LogWriter.LOG_ACCESS, Level.INFO, 
                "SUCCEED_GET_SUPPORTED_AUTH_MODULES", params);
        } catch (AMConfigurationException e) {
            String[] args = {e.getMessage()};
            debugError("GetSupportedAuthModules.handleRequest", e);
            writeLog(LogWriter.LOG_ERROR, Level.INFO,
                "FAILED_GET_SUPPORTED_AUTH_MODULES", args);
            throw new CLIException(e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        }
    }
}
