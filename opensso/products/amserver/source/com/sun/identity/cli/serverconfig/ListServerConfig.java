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
 * $Id: ListServerConfig.java,v 1.7 2010/01/15 18:10:55 veiming Exp $
 *
 */

package com.sun.identity.cli.serverconfig;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.cli.CLIException;
import com.sun.identity.cli.ExitCodes;
import com.sun.identity.cli.FormatUtils;
import com.sun.identity.cli.IArgument;
import com.sun.identity.cli.IOutput;
import com.sun.identity.cli.LogWriter;
import com.sun.identity.cli.RequestContext;
import com.sun.identity.common.configuration.ServerConfiguration;
import com.sun.identity.sm.SMSException;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Properties;
import java.util.logging.Level;

/**
 * Searches for realms command.
 */
public class ListServerConfig extends ServerConfigBase {
    private static final String OPTION_WITH_DEFAULTS = "withdefaults";
    /**
     * Lists Server Configuration.
     *
     * @param rc Request Context.
     * @throws CLIException if the request cannot serviced.
     */
    @Override
    public void handleRequest(RequestContext rc) 
        throws CLIException {
        super.handleRequest(rc);
        ldapLogin();
        SSOToken adminSSOToken = getAdminSSOToken();
        String serverName = getStringOptionValue(IArgument.SERVER_NAME);
        IOutput outputWriter = getOutputWriter();
        
        try {
            if (ServerConfiguration.isLegacy(adminSSOToken)) {
                outputWriter.printMessage(getResourceString(
                    "serverconfig-no-supported"));
                return;
            }
            
            String[] params = {serverName};
            writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                "ATTEMPT_LIST_SERVER_CONFIG", params);

            if (serverName.equals(DEFAULT_SVR_CONFIG)) {
                Properties prop = ServerConfiguration.getDefaults(adminSSOToken);
                outputWriter.printlnMessage(
                    FormatUtils.formatProperties(prop));
            } else {
                Properties prop = ServerConfiguration.getServerInstance(
                    adminSSOToken, serverName);

                if ((prop != null) && !prop.isEmpty()) {
                    if (isOptionSet(OPTION_WITH_DEFAULTS)) {
                        Properties defProp = ServerConfiguration.getDefaults(
                            adminSSOToken);
                        defProp.putAll(prop);
                        prop = defProp;
                    }
                    outputWriter.printlnMessage(
                        FormatUtils.formatProperties(prop));

                    String siteName = ServerConfiguration.getServerSite(
                        adminSSOToken, serverName);
                    if (siteName != null) {
                        Object[] args = {siteName};
                        outputWriter.printlnMessage(MessageFormat.format(
                            getResourceString("list-server-site-name"), args));
                    }

                    String serverId = ServerConfiguration.getServerID(
                        adminSSOToken, serverName);
                    if (serverId != null) {
                        Object[] args = {serverId};
                        outputWriter.printlnMessage(MessageFormat.format(
                            getResourceString("list-server-id"), args));
                    }

                } else {
                    outputWriter.printlnMessage(getResourceString(
                        "list-server-config-no-results"));
                }
            }
            
            writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                "SUCCEED_LIST_SERVER_CONFIG", params);
        } catch (IOException e) {
            String[] args = {serverName, e.getMessage()};
            debugError("ListServerConfig.handleRequest", e);
            writeLog(LogWriter.LOG_ERROR, Level.INFO,
                "FAILED_LIST_SERVER_CONFIG", args);
            throw new CLIException(e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (SSOException e) {
            String[] args = {serverName, e.getMessage()};
            debugError("ListServerConfig.handleRequest", e);
            writeLog(LogWriter.LOG_ERROR, Level.INFO,
                "FAILED_LIST_SERVER_CONFIG", args);
            throw new CLIException(e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (SMSException e) {
            String[] args = {serverName, e.getMessage()};
            debugError("ListServerConfig.handleRequest", e);
            writeLog(LogWriter.LOG_ERROR, Level.INFO,
                "FAILED_LIST_SERVER_CONFIG", args);
            throw new CLIException(e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        }
    }
}
