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
 * $Id: UpdateServerConfig.java,v 1.6 2009/01/31 04:43:12 veiming Exp $
 *
 */

package com.sun.identity.cli.serverconfig;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.cli.AttributeValues;
import com.sun.identity.cli.CLIException;
import com.sun.identity.cli.ExitCodes;
import com.sun.identity.cli.IArgument;
import com.sun.identity.cli.IOutput;
import com.sun.identity.cli.LogWriter;
import com.sun.identity.cli.RequestContext;
import com.sun.identity.common.configuration.ConfigurationException;
import com.sun.identity.common.configuration.ServerConfiguration;
import com.sun.identity.common.configuration.UnknownPropertyNameException;
import com.sun.identity.sm.SMSException;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/**
 * Set attribute values of a realm.
 */
public class UpdateServerConfig extends ServerConfigBase {
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
        String datafile = getStringOptionValue(IArgument.DATA_FILE);
        List attrValues = rc.getOption(IArgument.ATTRIBUTE_VALUES);

        if ((datafile == null) && (attrValues == null)) {
            throw new CLIException(
                getResourceString("missing-attributevalues"),
                ExitCodes.INCORRECT_OPTION, rc.getSubCommand().getName());
        }

        Map attributeValues = AttributeValues.parse(
            getCommandManager(), datafile, attrValues);
        String[] params = {serverName};
        
        try {
            if (ServerConfiguration.isLegacy(adminSSOToken)) {
                outputWriter.printMessage(getResourceString(
                    "serverconfig-no-supported"));
                return;
            }

            writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                "ATTEMPT_UPDATE_SERVER_CONFIG", params);

            if (serverName.equals(DEFAULT_SVR_CONFIG)) {
                try {
                    ServerConfiguration.setServerInstance(adminSSOToken, 
                        ServerConfiguration.DEFAULT_SERVER_CONFIG, 
                        attributeValues);
                    } catch (UnknownPropertyNameException ex) {
                        outputWriter.printlnMessage(
                            ex.getL10NMessage(getCommandManager().getLocale()));
                        outputWriter.printlnMessage("");
                    }
                    outputWriter.printlnMessage(MessageFormat.format(
                        getResourceString("update-server-config-succeeded"),
                        (Object[])params));
            } else {
                if (ServerConfiguration.isServerInstanceExist(
                    adminSSOToken, serverName)) {
                    try {
                        ServerConfiguration.setServerInstance(
                            adminSSOToken, serverName, attributeValues);
                    } catch (UnknownPropertyNameException ex) {
                        outputWriter.printlnMessage(
                            getResourceString("update-server-config-unknown"));
                        outputWriter.printlnMessage("");
                    }
                    outputWriter.printlnMessage(MessageFormat.format(
                        getResourceString("update-server-config-succeeded"),
                        (Object[])params));
                } else {
                    outputWriter.printlnMessage(MessageFormat.format(
                        getResourceString("update-server-config-does-not-exists"),
                        (Object[])params));
                }
            }
            
            writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                "SUCCEED_UPDATE_SERVER_CONFIG", params);
        } catch (ConfigurationException e) {
            String[] args = {serverName, e.getMessage()};
            debugError("UpdateServerConfig.handleRequest", e);
            writeLog(LogWriter.LOG_ERROR, Level.INFO,
                "FAILED_UPDATE_SERVER_CONFIG", args);
            throw new CLIException(e,ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (IOException e) {
            String[] args = {serverName, e.getMessage()};
            debugError("UpdateServerConfig.handleRequest", e);
            writeLog(LogWriter.LOG_ERROR, Level.INFO,
                "FAILED_UPDATE_SERVER_CONFIG", args);
            throw new CLIException(e,ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (SSOException e) {
            String[] args = {serverName, e.getMessage()};
            debugError("UpdateServerConfig.handleRequest", e);
            writeLog(LogWriter.LOG_ERROR, Level.INFO,
                "FAILED_UPDATE_SERVER_CONFIG", args);
            throw new CLIException(e,ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (SMSException e) {
            String[] args = {serverName, e.getMessage()};
            debugError("UpdateServerConfig.handleRequest", e);
            writeLog(LogWriter.LOG_ERROR, Level.INFO,
                "FAILED_UPDATE_SERVER_CONFIG", args);
            throw new CLIException(e,ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        }
    }
}
