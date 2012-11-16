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
 * $Id: EmbeddedStatus.java,v 1.2 2008/09/19 23:36:43 beomsuk Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.cli.serverconfig;

import com.iplanet.sso.SSOToken;
import com.sun.identity.cli.IArgument;
import com.sun.identity.cli.CLIException;
import com.sun.identity.cli.ExitCodes;
import com.sun.identity.cli.IOutput;
import com.sun.identity.cli.LogWriter;
import com.sun.identity.cli.RequestContext;
import com.sun.identity.log.Logger;
import com.sun.identity.setup.EmbeddedOpenDS;
import java.io.ByteArrayOutputStream;
import java.text.MessageFormat;
import java.util.logging.Level;

/**
 * Delete a server instance.
 */
public class EmbeddedStatus extends ServerConfigBase {
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
        String[] params = {};
        
System.out.println("RYA : EMB: ");
        try {
            writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                "ATTEMPT_EMBEDDED_STATUS", params);

            String port = getStringOptionValue(IArgument.EMBEDDED_PORT);
            String passwd = getAdminPassword();
            if (passwd == null) {
                passwd = getStringOptionValue(IArgument.EMBEDDED_PASSWORD);
            }

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ByteArrayOutputStream boe = new ByteArrayOutputStream();

            Logger.token.set(adminSSOToken);
            int stat = EmbeddedOpenDS.getReplicationStatus(
                           port, passwd, bos, boe);
            Logger.token.set(null);

            String str = bos.toString();
            String stre = boe.toString();
            String[] params1 = {Integer.toString(stat)};
            outputWriter.printMessage(
                MessageFormat.format(getResourceString("embedded-status-status")
                                      ,(Object[])params1));
            outputWriter.printlnMessage("\n");
            outputWriter.printlnMessage(str);
            outputWriter.printlnMessage("\n");
            outputWriter.printlnMessage(stre);
            outputWriter.printlnMessage("\n");
            
            writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                "SUCCEED_EMBEDDED_STATUS", params);
        } catch (Exception e) {
            String[] args = {e.getMessage()};
            debugError("EmbeddedStatus.handleRequest", e);
            writeLog(LogWriter.LOG_ERROR, Level.INFO,
                "FAILED_EMBEDDED_STATUS", args);
            throw new CLIException(e,ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        }
    }
}
