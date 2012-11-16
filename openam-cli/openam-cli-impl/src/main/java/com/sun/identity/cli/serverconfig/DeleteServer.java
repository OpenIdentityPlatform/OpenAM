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
 * $Id: DeleteServer.java,v 1.3 2008/09/19 23:36:43 beomsuk Exp $
 *
 */

package com.sun.identity.cli.serverconfig;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.cli.CLIException;
import com.sun.identity.cli.ExitCodes;
import com.sun.identity.cli.IArgument;
import com.sun.identity.cli.IOutput;
import com.sun.identity.cli.LogWriter;
import com.sun.identity.cli.RequestContext;
import com.sun.identity.common.configuration.ServerConfiguration;
import com.sun.identity.sm.SMSException;
import java.text.MessageFormat;
import java.util.logging.Level;

/**
 * Delete a server intance.
 */
public class DeleteServer extends ServerConfigBase {
    private static final String SERVER_CONFIG_XML_FILE = "serverconfigxml";
    
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

        String serverName = getStringOptionValue(IArgument.SERVER_NAME);
        String[] params = {serverName};

        try {
            if (ServerConfiguration.isLegacy(adminSSOToken)) {
                outputWriter.printMessage(getResourceString(
                    "serverconfig-no-supported"));
                return;
            }
            
            writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                "ATTEMPT_DELETE_SERVER", params);
            if (ServerConfiguration.isServerInstanceExist(
                adminSSOToken, serverName)) {
                ServerConfiguration.deleteServerInstance(
                    adminSSOToken, serverName);
                outputWriter.printlnMessage(MessageFormat.format(
                    getResourceString("delete-server-config-succeeded"),
                    (Object[])params));
            } else {
                outputWriter.printlnMessage(MessageFormat.format(
                    getResourceString("delete-server-config-dont-exists"),
                    (Object[])params));
            }
            
            writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                "SUCCEED_DELETE_SERVER", params);
        } catch (SSOException e) {
            String[] args = {serverName, e.getMessage()};
            debugError("DeleteServer.handleRequest", e);
            writeLog(LogWriter.LOG_ERROR, Level.INFO,
                "FAILED_DELETE_SERVER", args);
            throw new CLIException(e,ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (SMSException e) {
            String[] args = {serverName, e.getMessage()};
            debugError("DeleteServer.handleRequest", e);
            writeLog(LogWriter.LOG_ERROR, Level.INFO,
                "FAILED_DELETE_SERVER", args);
            throw new CLIException(e,ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        }
    }
}
