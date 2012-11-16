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
 * $Id: ListAgentMembership.java,v 1.4 2008/06/25 05:42:10 qcheng Exp $
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
import java.text.MessageFormat;
import java.util.Set;
import java.util.logging.Level;

/**
 * This command gets the members of an agent group.
 */
public class ListAgentMembership extends AuthenticatedCommand {
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
        String[] params = {realm, agentName};

        try {
            writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                "ATTEMPT_LIST_AGENT_MEMBERSHIP", params);
            AMIdentity amid = new AMIdentity(
                adminSSOToken, agentName, IdType.AGENT, realm, null); 
            
            if (!amid.isExists()) {
                String[] args = {realm, agentName, 
                    "agent did not exist"};
                writeLog(LogWriter.LOG_ERROR, Level.INFO,
                    "FAILED_LIST_AGENT_MEMBERSHIP", args);
                Object[] p = {agentName};
                String msg = MessageFormat.format(getResourceString(
                        "list-agent-membership-agent-not-found"), p);
                throw new CLIException(msg,
                    ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
            }
            
            Set groups = amid.getMemberships(IdType.AGENTGROUP);

            if ((groups != null) && !groups.isEmpty()) {
                AMIdentity a = (AMIdentity)groups.iterator().next();
                Object[] obj = {a.getName(), a.getUniversalId()};
                outputWriter.printlnMessage(MessageFormat.format(
                    getResourceString("list-agent-membership-succeeded"), obj));
            } else {
                outputWriter.printlnMessage(
                    getResourceString("list-agent-membership-no-members"));
            }
            writeLog(LogWriter.LOG_ACCESS, Level.INFO, 
                "SUCCEED_LIST_AGENT_MEMBERSHIP", params);
        } catch (IdRepoException e) {
            String[] args = {realm, agentName, e.getMessage()};
            debugError("ListAgentMembership.handleRequest", e);
            writeLog(LogWriter.LOG_ERROR, Level.INFO,
                "FAILED_LIST_AGENT_MEMBERSHIP", args);
            throw new CLIException(e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (SSOException e) {
            String[] args = {realm, agentName, e.getMessage()};
            debugError("ListAgentMembership.handleRequest", e);
            writeLog(LogWriter.LOG_ERROR, Level.INFO,
                "FAILED_LIST_AGENT_MEMBERSHIP", args);
            throw new CLIException(e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        }
    }
}
