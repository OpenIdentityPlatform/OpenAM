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
 * $Id: ShowAgentTypes.java,v 1.5 2008/06/25 05:42:10 qcheng Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.cli.agentconfig;

import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOException;
import com.sun.identity.cli.AuthenticatedCommand;
import com.sun.identity.cli.CLIException;
import com.sun.identity.cli.ExitCodes;
import com.sun.identity.cli.IOutput;
import com.sun.identity.cli.LogWriter;
import com.sun.identity.cli.RequestContext;
import com.sun.identity.common.configuration.AgentConfiguration;
import com.sun.identity.sm.SMSException;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;

/**
 * This command shows the suppported agent types.
 */
public class ShowAgentTypes extends AuthenticatedCommand {
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

        try {
            writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                "ATTEMPT_SHOW_AGENT_TYPES", null);
            Set agentTypes = AgentConfiguration.getAgentTypes();

            if (!agentTypes.isEmpty()) {
                for (Iterator i = agentTypes.iterator(); i.hasNext();) {
                    outputWriter.printlnMessage((String)i.next());
                }
            } else {
                outputWriter.printlnMessage(getResourceString(
                    "show-agent-type-no-results"));
            }
            writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                "SUCCEED_SHOW_AGENT_TYPES", null);
        } catch (SMSException e) {
            String[] args = {e.getMessage()};
            debugError("ShowAgentTypes.handleRequest", e);
            writeLog(LogWriter.LOG_ERROR, Level.INFO,
                "FAILED_SHOW_AGENT_TYPES", args);
            throw new CLIException(e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (SSOException e) {
            String[] args = {e.getMessage()};
            debugError("ShowAgentTypes.handleRequest", e);
            writeLog(LogWriter.LOG_ERROR, Level.INFO,
                "FAILED_SHOW_AGENT_TYPES", args);
            throw new CLIException(e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        }
    }
}
