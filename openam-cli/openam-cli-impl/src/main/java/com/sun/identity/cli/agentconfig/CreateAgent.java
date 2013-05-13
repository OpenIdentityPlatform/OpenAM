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
 * $Id: CreateAgent.java,v 1.9 2008/11/01 03:07:14 veiming Exp $
 *
 */

/*
 * Portions Copyrighted 2011-2013 ForgeRock, Inc.
 */

package com.sun.identity.cli.agentconfig;

import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOException;
import com.sun.identity.cli.AttributeValues;
import com.sun.identity.cli.AuthenticatedCommand;
import com.sun.identity.cli.CLIConstants;
import com.sun.identity.cli.CLIException;
import com.sun.identity.cli.ExitCodes;
import com.sun.identity.cli.IArgument;
import com.sun.identity.cli.LogWriter;
import com.sun.identity.cli.RequestContext;
import com.sun.identity.common.configuration.AgentConfiguration;
import com.sun.identity.common.configuration.ConfigurationException;
import com.sun.identity.idm.AMIdentityRepository;
import com.sun.identity.idm.IdOperation;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdType;
import com.sun.identity.shared.FQDNUrl;
import com.sun.identity.sm.SMSException;
import java.net.MalformedURLException;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

/**
 * This command creates agent.
 */
public class CreateAgent extends AuthenticatedCommand {
    private static final String AGENT_URL = "agenturl";

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

        String realm = getStringOptionValue(IArgument.REALM_NAME);
        String agentName = getStringOptionValue(IArgument.AGENT_NAME);
        String agentType = getStringOptionValue(IArgument.AGENT_TYPE);
        String datafile = getStringOptionValue(IArgument.DATA_FILE);
        List attrValues = rc.getOption(IArgument.ATTRIBUTE_VALUES);
        Map attributeValues = Collections.EMPTY_MAP;
        
        if ((datafile != null) || (attrValues != null)) {
            attributeValues = AttributeValues.parse(getCommandManager(),
                datafile, attrValues);        
        }
        
        if ((attributeValues == null) || attributeValues.isEmpty()) {
            throw new CLIException(
                getResourceString("agent-creation-pwd-needed"),
                ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        }

        String serverURL = getStringOptionValue(IArgument.SERVER_URL);
        String agentURL = getStringOptionValue(AGENT_URL);
        boolean webJ2EEAgent = agentType.equals("WebAgent") ||
            agentType.equals("J2EEAgent");

        if (!webJ2EEAgent) {
            if (serverURL != null) {
                throw new CLIException(
                    getResourceString("does-not-support-server-url"),
                    ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
            }
            if (agentURL != null) {
                throw new CLIException(
                    getResourceString("does-not-support-agent-url"),
                    ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
            }
        } else {
            if (agentURL != null && serverURL == null) {
                throw new CLIException(getResourceString("server-url-missing"), ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
            }
            if (serverURL != null && agentURL == null) {
                throw new CLIException(getResourceString("agent-url-missing"), ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
            }
            if (serverURL == null && agentURL == null && attributeValues.size() == 1) {
                //only the password is provided
                throw new CLIException(getResourceString("missing-urls"), ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
            }
        }


        boolean hasPassword = false;
        for (Iterator i = attributeValues.keySet().iterator();
            (i.hasNext() && !hasPassword);
        ) {
            String k = (String)i.next();
            if (k.equals(CLIConstants.ATTR_SCHEMA_AGENT_PWD)) {
                Set values = (Set)attributeValues.get(k);
                if ((values != null) && !values.isEmpty()) {
                    String pwd = (String)values.iterator().next();
                    hasPassword = (pwd.trim().length() > 0);
                }
            }
        }
        
        if (!hasPassword) {
            throw new CLIException(
                getResourceString("agent-creation-pwd-needed"),
                ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        }

        String[] params = {realm, agentType, agentName};
        writeLog(LogWriter.LOG_ACCESS, Level.INFO, "ATTEMPT_CREATE_AGENT",
            params);
        try {
            AMIdentityRepository amir = new AMIdentityRepository(
                adminSSOToken, realm);
            Set set = amir.getAllowedIdOperations(IdType.AGENTONLY);
            if (!set.contains(IdOperation.CREATE)) {
                String[] args = {realm};
                throw new CLIException(MessageFormat.format(
                    getResourceString("does-not-support-agent-creation"),
                    (Object[])args),
                    ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
            }

            if (webJ2EEAgent) {
                if (serverURL != null) {
                    FQDNUrl fqdnServerURL = null;
                    try {
                        fqdnServerURL = new FQDNUrl(serverURL);
                    } catch (MalformedURLException e) {
                        throw new CLIException(getResourceString(
                            "server-url-invalid"),
                            ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
                    }

                    FQDNUrl fqdnAgentURL = null;
                    try {
                        fqdnAgentURL = new FQDNUrl(agentURL);
                    } catch (MalformedURLException e) {
                        throw new CLIException(getResourceString(
                            "agent-url-invalid"),
                            ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
                    }

                    Map map = AgentConfiguration.getDefaultValues(
                        agentType, false);
                    map.putAll(attributeValues);
                    AgentConfiguration.tagswapAttributeValues(map, agentType,
                        fqdnServerURL, fqdnAgentURL);

                    // Remove any default values that have not been replaced by values
                    // supplied when calling create agent. These are in the form of
                    // propertyname[n] where n is a value starting from 0
                    AgentConfiguration.removeDefaultDuplicates(attributeValues, map);

                    AgentConfiguration.createAgent(adminSSOToken, realm,
                        agentName, agentType, map);
                } else {
                    AgentConfiguration.createAgent(adminSSOToken, realm,
                        agentName, agentType, attributeValues);
                }
            } else {
                AgentConfiguration.createAgent(adminSSOToken, realm, agentName, 
                    agentType, attributeValues);
            }

            getOutputWriter().printlnMessage(MessageFormat.format(
                getResourceString("create-agent-succeeded"),
                (Object[])params));
            writeLog(LogWriter.LOG_ACCESS, Level.INFO, "SUCCEED_CREATE_AGENT",
                params);
        } catch (ConfigurationException e) {
            String[] args = {realm, agentType, agentName, e.getMessage()};
            debugError("CreateAgent.handleRequest", e);
            writeLog(LogWriter.LOG_ERROR, Level.INFO, "FAILED_CREATE_AGENT",
                args);
            throw new CLIException(e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (IdRepoException e) {
            String[] args = {realm, agentType, agentName, e.getMessage()};
            debugError("CreateAgent.handleRequest", e);
            writeLog(LogWriter.LOG_ERROR, Level.INFO, "FAILED_CREATE_AGENT",
                args);
            throw new CLIException(e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (SMSException e) {
            String[] args = {realm, agentType, agentName, e.getMessage()};
            debugError("CreateAgent.handleRequest", e);
            writeLog(LogWriter.LOG_ERROR, Level.INFO, "FAILED_CREATE_AGENT",
                args);
            throw new CLIException(e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);            
        } catch (SSOException e) {
            String[] args = {realm, agentType, agentName, e.getMessage()};
            debugError("CreateAgent.handleRequest", e);
            writeLog(LogWriter.LOG_ERROR, Level.INFO, "FAILED_CREATE_AGENT",
                args);
            throw new CLIException(e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        }
    }
}
