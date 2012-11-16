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
 * $Id: RemoveServerConfig.java,v 1.4 2008/11/07 20:27:05 veiming Exp $
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
import java.io.IOException;
import java.text.MessageFormat;
import java.util.List;
import java.util.logging.Level;

/**
 * Remove Server configuration.
 */
public class RemoveServerConfig extends ServerConfigBase {
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
        List propertyNames = rc.getOption(IArgument.PROPERTY_NAMES);
        
        try {
            if (ServerConfiguration.isLegacy(adminSSOToken)) {
                outputWriter.printMessage(getResourceString(
                    "serverconfig-no-supported"));
                return;
            }

            String[] params = {serverName};
            writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                "ATTEMPT_REMOVE_SERVER_CONFIG", params);
            
            if (serverName.equals(DEFAULT_SVR_CONFIG)) {
                ServerConfiguration.removeServerConfiguration(
                    adminSSOToken, ServerConfiguration.DEFAULT_SERVER_CONFIG,
                    propertyNames);
            } else {
                if (ServerConfiguration.isServerInstanceExist(
                    adminSSOToken, serverName)) {
                    ServerConfiguration.removeServerConfiguration(
                        adminSSOToken, serverName, propertyNames);
                    outputWriter.printlnMessage(MessageFormat.format(
                        getResourceString("remove-server-config-succeeded"),
                        (Object[]) params));
                } else {
                    outputWriter.printlnMessage(MessageFormat.format(
                        getResourceString("remove-server-config-does-not-exists"),
                        (Object[]) params));
                }
            }

            writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                "SUCCEED_REMOVE_SERVER_CONFIG", params);
        } catch (IOException e) {
            String[] args = {serverName, e.getMessage()};
            debugError("RemoveServerConfig.handleRequest", e);
            writeLog(LogWriter.LOG_ERROR, Level.INFO,
                "FAILED_REMOVE_SERVER_CONFIG", args);
            throw new CLIException(e,ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (SSOException e) {
            String[] args = {serverName, e.getMessage()};
            debugError("RemoveServerConfig.handleRequest", e);
            writeLog(LogWriter.LOG_ERROR, Level.INFO,
                "FAILED_REMOVE_SERVER_CONFIG", args);
            throw new CLIException(e,ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (SMSException e) {
            String[] args = {serverName, e.getMessage()};
            debugError("RemoveServerConfig.handleRequest", e);
            writeLog(LogWriter.LOG_ERROR, Level.INFO,
                "FAILED_REMOVE_SERVER_CONFIG", args);
            throw new CLIException(e,ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        }
    }
}
