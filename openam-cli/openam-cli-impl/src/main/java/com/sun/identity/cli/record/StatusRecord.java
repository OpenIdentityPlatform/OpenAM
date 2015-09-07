/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2015 ForgeRock AS.
 */

package com.sun.identity.cli.record;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.cli.AuthenticatedCommand;
import com.sun.identity.cli.CLIException;
import com.sun.identity.cli.IArgument;
import com.sun.identity.cli.IOutput;
import com.sun.identity.cli.LogWriter;
import com.sun.identity.cli.RequestContext;
import com.sun.identity.cli.rest.RestCommand;
import com.sun.identity.common.configuration.ServerConfiguration;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.SMSException;
import org.forgerock.openam.core.rest.record.RecordConstants;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;


/**
 * Get the current record Status by calling the records REST endpoint.
 */
public class StatusRecord extends AuthenticatedCommand {

    private final Debug debug = Debug.getInstance(RecordConstants.DEBUG_INSTANCE_NAME);


    /**
     * Record
     * @param rc Request Context.
     * @throws CLIException
     */
    public void handleRequest(RequestContext rc) throws CLIException {
        super.handleRequest(rc);
        ldapLogin();

        IOutput outputWriter = getOutputWriter();
        String serverName = getStringOptionValue(IArgument.SERVER_NAME);

        debug.message("Recording status: serverURL : '{}'", serverName);
        String[] argsAttempt = {serverName};
        writeLog(LogWriter.LOG_ACCESS, Level.INFO, "ATTEMPT_STATUS_RECORD", argsAttempt);

        SSOToken adminSSOToken = getAdminSSOToken();
        try {
            if (!ServerConfiguration.isServerInstanceExist(adminSSOToken, serverName)) {
                String message = "ServerName '" + serverName + "' doesn't exist";
                debug.error(message);
                outputWriter.printlnMessage(message);

                String[] args = {serverName , message};
                writeLog(LogWriter.LOG_ERROR, Level.INFO, "FAILED_STATUS_RECORD", args);
                return;
            }

            Map<String, String> headers = new HashMap<String, String>();
            headers.put("Content-Type", "application/json");
            RestCommand restCommand = new RestCommand();
            String result = restCommand.sendRestCommand(adminSSOToken.getTokenID(), new URL(serverName + "/json/"
                            + RecordConstants.RECORD_REST_ENDPOINT + "?_action=" + RecordConstants.STATUS_ACTION),
                    "POST", headers, "");
            debug.message("Recording status with success. Result : '{}'", result);

            if (result.isEmpty()) {
                outputWriter.printlnMessage("Result from server is empty. An error occurred. See debug logs for more " +
                        "information");

                String[] args = {serverName , "Result from server is empty. An error occurred."};
                writeLog(LogWriter.LOG_ERROR, Level.INFO, "FAILED_STATUS_RECORD", args);
            }

            String[] args = {serverName , result};
            writeLog(LogWriter.LOG_ACCESS, Level.INFO, "SUCCESS_STATUS_RECORD", args);
            outputWriter.printlnMessage(result);

        } catch (IOException | SMSException | SSOException e) {
            debug.error("An error occurred", e);
            outputWriter.printlnMessage(e.getMessage());

            String[] args = {serverName , e.getMessage()};
            writeLog(LogWriter.LOG_ERROR, Level.INFO, "FAILED_STATUS_RECORD", args);
        }
    }
}
