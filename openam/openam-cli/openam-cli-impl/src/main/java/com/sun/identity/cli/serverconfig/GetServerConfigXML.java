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
 * $Id: GetServerConfigXML.java,v 1.3 2008/09/19 23:37:13 beomsuk Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.cli.serverconfig;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.cli.CLIException;
import com.sun.identity.cli.CLIUtil;
import com.sun.identity.cli.ExitCodes;
import com.sun.identity.cli.IArgument;
import com.sun.identity.cli.IOutput;
import com.sun.identity.cli.LogWriter;
import com.sun.identity.cli.RequestContext;
import com.sun.identity.common.configuration.ServerConfiguration;
import com.sun.identity.sm.SMSException;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.logging.Level;

/**
 * Display server configuration XML.
 */
public class GetServerConfigXML extends ServerConfigBase {
    /**
     * Returns Server Configuration XML.
     *
     * @param rc Request Context.
     * @throws CLIException if the request cannot serviced.
     */
    public void handleRequest(RequestContext rc) 
        throws CLIException {
        super.handleRequest(rc);
        ldapLogin();
        SSOToken adminSSOToken = getAdminSSOToken();
        String serverName = getStringOptionValue(IArgument.SERVER_NAME);
        String outfile = getStringOptionValue(IArgument.OUTPUT_FILE);
        IOutput outputWriter = getOutputWriter();
        
        try {
            if (ServerConfiguration.isLegacy(adminSSOToken)) {
                outputWriter.printMessage(getResourceString(
                    "serverconfig-no-supported"));
                return;
            }
            
            String[] params = {serverName};
            writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                "ATTEMPT_GET_SERVER_CONFIG_XML", params);

            String xml = ServerConfiguration.getServerConfigXML(
                adminSSOToken, serverName);
            if ((xml != null) && (xml.length() > 0)) {
                if ((outfile != null) && (outfile.length() > 0)) {
                    CLIUtil.writeToFile(outfile, xml);
                } else {
                    outputWriter.printlnMessage(xml);
                }
                outputWriter.printlnMessage(MessageFormat.format(
                    getResourceString("get-serverconfig-xml-succeeded"), 
                    (Object[])params));
            } else {
                outputWriter.printlnMessage(getResourceString(
                    "get-server-config-xml-no-result-no-results"));
            }
            
            writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                "SUCCEED_GET_SERVER_CONFIG_XML", params);
        } catch (IOException e) {
            String[] args = {serverName, e.getMessage()};
            debugError("GetServerConfigXML.handleRequest", e);
            writeLog(LogWriter.LOG_ERROR, Level.INFO,
                "FAILED_GET_SERVER_CONFIG_XML", args);
            throw new CLIException(e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (SSOException e) {
            String[] args = {serverName, e.getMessage()};
            debugError("GetServerConfigXML.handleRequest", e);
            writeLog(LogWriter.LOG_ERROR, Level.INFO,
                "FAILED_GET_SERVER_CONFIG_XML", args);
            throw new CLIException(e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (SMSException e) {
            String[] args = {serverName, e.getMessage()};
            debugError("GetServerConfigXML.handleRequest", e);
            writeLog(LogWriter.LOG_ERROR, Level.INFO,
                "FAILED_GET_SERVER_CONFIG_XML", args);
            throw new CLIException(e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        }
    }
}
