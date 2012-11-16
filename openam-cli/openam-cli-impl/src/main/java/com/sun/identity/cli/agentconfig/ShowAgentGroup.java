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
 * $Id: ShowAgentGroup.java,v 1.5 2008/06/25 05:42:10 qcheng Exp $
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
import com.sun.identity.common.configuration.AgentConfiguration;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdType;
import com.sun.identity.sm.AttributeSchema;
import com.sun.identity.sm.SMSException;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.MessageFormat;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

/**
 * This command gets attribute values of an agent group.
 */
public class ShowAgentGroup extends AuthenticatedCommand {
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
        String outfile = getStringOptionValue(IArgument.OUTPUT_FILE);
        String[] params = {realm, agentGroupName};

        try {
            writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                "ATTEMPT_SHOW_AGENT_GROUP", params);
            AMIdentity amid = new AMIdentity(adminSSOToken, agentGroupName,
                IdType.AGENTGROUP, realm, null);
            if (!amid.isExists()) {
                String[] args = {realm, agentGroupName, 
                    "agent group did not exist"};
                writeLog(LogWriter.LOG_ERROR, Level.INFO,
                    "FAILED_SHOW_AGENT_GROUP", args);
                Object[] p = {agentGroupName};
                String msg = MessageFormat.format(
                    getResourceString("show-agent-group-does-not-exist"), p);
                throw new CLIException(msg,
                    ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
            }
            Map values = AgentConfiguration.getAgentGroupAttributes(
                adminSSOToken, realm, agentGroupName);

            Set passwords = AgentConfiguration.getAttributesSchemaNames(
                amid, AttributeSchema.Syntax.PASSWORD);

            if ((values != null) && !values.isEmpty()) {
                StringBuilder buff = new StringBuilder();
                for (Iterator i = values.keySet().iterator(); i.hasNext();) {
                    String attrName = (String)i.next();

                    if (passwords.contains(attrName)) {
                        buff.append(attrName).append("=********\n");
                    } else {
                        Set vals = (Set)values.get(attrName);

                        if (vals.isEmpty()) {
                            buff.append(attrName).append("=").append("\n");
                        } else {
                            for (Iterator j = vals.iterator(); j.hasNext(); ){
                                String val = (String)j.next();
                                buff.append(attrName).append("=").append(val)
                                    .append("\n");
                            }
                        }
                    }
                }
                if (outfile == null) {
                    outputWriter.printlnMessage(buff.toString());
                } else {
                    writeToFile(outfile, buff.toString());
                    outputWriter.printlnMessage(getResourceString(
                        "show-agent-group-to-file"));
                }
            } else {
                outputWriter.printlnMessage(getResourceString(
                    "show-agent-group-no-attributes"));
            }
            writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                "SUCCEED_SHOW_AGENT_GROUP", params);
        } catch (SMSException e) {
            String[] args = {realm, agentGroupName, e.getMessage()};
            debugError("ShowAgentGroup.handleRequest", e);
            writeLog(LogWriter.LOG_ERROR, Level.INFO,
                "FAILED_SHOW_AGENT_GROUP", args);
            throw new CLIException(e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (IdRepoException e) {
            String[] args = {realm, agentGroupName, e.getMessage()};
            debugError("ShowAgentGroup.handleRequest", e);
            writeLog(LogWriter.LOG_ERROR, Level.INFO,
                "FAILED_SHOW_AGENT_GROUP", args);
            throw new CLIException(e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (SSOException e) {
            String[] args = {realm, agentGroupName, e.getMessage()};
            debugError("ShowAgentGroup.handleRequest", e);
            writeLog(LogWriter.LOG_ERROR, Level.INFO,
                "FAILED_SHOW_AGENT_GROUP", args);
            throw new CLIException(e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        }
    }

    private void writeToFile(String outfile, String content)
        throws CLIException {
        FileOutputStream fout = null;
        PrintWriter pwout = null;

        try {
            fout = new FileOutputStream(outfile, true);
            pwout = new PrintWriter(fout, true);
            pwout.write(content);
            pwout.flush();
        } catch (FileNotFoundException e) {
            debugError("ShowAgentGroup.writeToFile", e);
            throw new CLIException(e, ExitCodes.IO_EXCEPTION);
        } catch (SecurityException e) {
            debugError("ShowAgentGroup.writeToFile", e);
            throw new CLIException(e, ExitCodes.IO_EXCEPTION);
        } finally {
            try {
                if (fout != null) {
                    fout.close();
                }
                if (pwout != null) {
                    pwout.close();
                }
            } catch (IOException ex) {
                //do nothing
            }
        }
    }
}
