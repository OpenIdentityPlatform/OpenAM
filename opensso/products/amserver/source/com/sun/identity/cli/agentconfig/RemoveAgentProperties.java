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
 * $Id: RemoveAgentProperties.java,v 1.3 2008/06/25 05:42:10 qcheng Exp $
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
import com.sun.identity.cli.IArgument;
import com.sun.identity.cli.IOutput;
import com.sun.identity.cli.LogWriter;
import com.sun.identity.cli.RequestContext;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdType;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;

/**
 * This command remove attribute values of an agent.
 */
public class RemoveAgentProperties extends AuthenticatedCommand {
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
        String realm = getStringOptionValue(IArgument.REALM_NAME);
        String agentName = getStringOptionValue(IArgument.AGENT_NAME);
        Set attributeNames = new HashSet();
        attributeNames.addAll(rc.getOption(IArgument.ATTRIBUTE_NAMES));
        String[] params = {realm, agentName, attributeNames.toString()};

        try {
            writeLog(LogWriter.LOG_ACCESS, Level.INFO, 
                "ATTEMPT_REMOVE_AGENT_PROPERTIES", params);
            AMIdentity amid = new AMIdentity(adminSSOToken, agentName,
                IdType.AGENTONLY, realm, null);
            amid.removeAttributes(attributeNames);
            outputWriter.printlnMessage(getResourceString(
                    "agent-remove-properties-succeeded"));
            writeLog(LogWriter.LOG_ACCESS, Level.INFO, 
                "SUCCEED_REMOVE_AGENT_PROPERTIES", params);
        } catch (IdRepoException e) {
            String[] args = {realm, agentName, e.getMessage()};
            debugError("RemoveAgentProperties.handleRequest", e);
            writeLog(LogWriter.LOG_ERROR, Level.INFO,
                "FAILED_REMOVE_AGENT_PROPERTIES", args);
            throw new CLIException(e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (SSOException e) {
            String[] args = {realm, agentName, e.getMessage()};
            debugError("RemoveAgentProperties.handleRequest", e);
            writeLog(LogWriter.LOG_ERROR, Level.INFO,
                "FAILED_REMOVE_AGENT_PROPERTIES", args);
            throw new CLIException(e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        }
    }
}
