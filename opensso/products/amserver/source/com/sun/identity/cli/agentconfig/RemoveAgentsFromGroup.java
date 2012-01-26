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
 * $Id: RemoveAgentsFromGroup.java,v 1.5 2008/07/14 21:33:15 veiming Exp $
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
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdType;
import com.sun.identity.sm.SMSException;
import java.text.MessageFormat;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;

/**
 * This command removes agents of an agent group.
 */
public class RemoveAgentsFromGroup extends AuthenticatedCommand {
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
            AMIdentity agentGroup = new AMIdentity(
                adminSSOToken, agentGroupName, IdType.AGENTGROUP, realm, null);
            if (!agentGroup.isExists()) {
                Object[] p = {agentGroupName};
                throw new CLIException(MessageFormat.format(
                    getResourceString(
                        "remove-agent-to-group-agent-invalid-group"),
                    p), ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
            }
            
            validateAgents(realm, adminSSOToken, agentGroup, agentNames);
            
            for (Iterator i = agentNames.iterator(); i.hasNext(); ) {
                agentName = (String)i.next();
                params[2] = agentName;
                writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                    "ATTEMPT_REMOVE_AGENT_FROM_GROUP", params);
                AMIdentity amid = new AMIdentity(
                    adminSSOToken, agentName, IdType.AGENTONLY, realm, null); 
                AgentConfiguration.removeAgentGroup(amid, agentGroup);
                writeLog(LogWriter.LOG_ACCESS, Level.INFO, 
                    "SUCCEED_REMOVE_AGENT_FROM_GROUP", params);
            }
            
            if (agentNames.size() > 1) {
                outputWriter.printlnMessage(getResourceString(
                    "remove-agent-to-group-succeeded-pural"));
            } else {
                outputWriter.printlnMessage(getResourceString(
                    "remove-agent-to-group-succeeded"));
            }
        } catch (IdRepoException e) {
            String[] args = {realm, agentGroupName, agentName, e.getMessage()};
            debugError("RemoveAgentsFromGroup.handleRequest", e);
            writeLog(LogWriter.LOG_ERROR, Level.INFO,
                "FAILED_REMOVE_AGENT_FROM_GROUP", args);
            throw new CLIException(e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (SSOException e) {
            String[] args = {realm, agentGroupName, agentName, e.getMessage()};
            debugError("RemoveAgentsFromGroup.handleRequest", e);
            writeLog(LogWriter.LOG_ERROR, Level.INFO,
                "FAILED_REMOVE_AGENT_FROM_GROUP", args);
            throw new CLIException(e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (SMSException e) {
            String[] args = {realm, agentGroupName, agentName, e.getMessage()};
            debugError("RemoveAgentsFromGroup.handleRequest", e);
            writeLog(LogWriter.LOG_ERROR, Level.INFO,
                "FAILED_REMOVE_AGENT_FROM_GROUP", args);
            throw new CLIException(e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        }
    }

    private void validateAgents(
        String realm,
        SSOToken adminSSOToken,
        AMIdentity agentGroup, 
        List agentNames
    ) throws CLIException, IdRepoException, SSOException {
        for (Iterator i = agentNames.iterator(); i.hasNext();) {
            String agentName = (String) i.next();
            AMIdentity amid = new AMIdentity(
                adminSSOToken, agentName, IdType.AGENTONLY, realm, null);
            if (!amid.isMember(agentGroup)) {
                Object[] p = {agentName, agentGroup.getName()};
                throw new CLIException(MessageFormat.format(
                    getResourceString("remove-agent-to-group-agent-not-member"),
                    p), ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
            }
        }
    }
}
