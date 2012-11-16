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
 * $Id: AddAgentsToGroup.java,v 1.4 2008/06/25 05:42:09 qcheng Exp $
 *
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
import com.sun.identity.common.configuration.AgentConfiguration;
import com.sun.identity.common.configuration.ConfigurationException;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdType;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;

/**
 * This command will make an agent a member of agent group.
 */
public class AddAgentsToGroup extends AuthenticatedCommand {
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
        String agentGroupName = getStringOptionValue(
            IArgument.AGENT_GROUP_NAME);
        List agentNames = rc.getOption(IArgument.AGENT_NAMES);
        String[] params = {realm, agentGroupName, ""};
        String agentName = "";
        try {
            AMIdentity amidGroup = new AMIdentity(
                adminSSOToken, agentGroupName, IdType.AGENTGROUP, realm, null);
            
            for (Iterator i = agentNames.iterator(); i.hasNext(); ) {
                agentName = (String)i.next();
                params[2] = agentName;
                writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                    "ATTEMPT_ADD_AGENT_TO_GROUP", params);
                AMIdentity amid = new AMIdentity(
                    adminSSOToken, agentName, IdType.AGENTONLY, realm, null); 
                AgentConfiguration.AddAgentToGroup(amidGroup, amid);
                writeLog(LogWriter.LOG_ACCESS, Level.INFO, 
                    "SUCCEED_ADD_AGENT_TO_GROUP", params);
            }
            
            if (agentNames.size() > 1) {
                outputWriter.printlnMessage(getResourceString(
                    "add-agent-to-group-succeeded-pural"));
            } else {
                outputWriter.printlnMessage(getResourceString(
                    "add-agent-to-group-succeeded"));
            }
        } catch (IdRepoException e) {
            String[] args = {realm, agentGroupName, agentName, e.getMessage()};
            debugError("AddAgentsToGroup.handleRequest", e);
            writeLog(LogWriter.LOG_ERROR, Level.INFO,
                "FAILED_ADD_AGENT_TO_GROUP", args);
            throw new CLIException(e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (ConfigurationException e) {
            String[] args = {realm, agentGroupName, agentName, e.getMessage()};
            debugError("AddAgentsToGroup.handleRequest", e);
            writeLog(LogWriter.LOG_ERROR, Level.INFO,
                "FAILED_ADD_AGENT_TO_GROUP", args);
            throw new CLIException(e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (SSOException e) {
            String[] args = {realm, agentGroupName, agentName, e.getMessage()};
            debugError("AddAgentsToGroup.handleRequest", e);
            writeLog(LogWriter.LOG_ERROR, Level.INFO,
                "FAILED_ADD_AGENT_TO_GROUP", args);
            throw new CLIException(e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        }
    }
}
