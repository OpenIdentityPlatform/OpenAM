/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: AddSubConfiguration.java,v 1.7 2008/06/25 05:42:17 qcheng Exp $
 *
 */

package com.sun.identity.cli.schema;


import com.sun.identity.cli.AttributeValues;
import com.sun.identity.cli.CLIException;
import com.sun.identity.cli.ExitCodes;
import com.sun.identity.cli.IArgument;
import com.sun.identity.cli.IOutput;
import com.sun.identity.cli.LogWriter;
import com.sun.identity.cli.RequestContext;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.SMSSchema;
import com.sun.identity.sm.ServiceConfigManager;
import com.sun.identity.sm.ServiceConfig;
import java.text.MessageFormat;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.logging.Level;

/**
 * Adds sub configuration.
 */
public class AddSubConfiguration extends SchemaCommand {
    private static final String OPTION_PRIORITY = "priority";
    
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

        String serviceName = getStringOptionValue(IArgument.SERVICE_NAME);
        String subConfigName = getStringOptionValue(
            IArgument.SUB_CONFIGURATION_NAME);
        String realmName = getStringOptionValue(IArgument.REALM_NAME);
        String subConfigId = getStringOptionValue(
            IArgument.SUB_CONFIGURATION_ID);
        String datafile = getStringOptionValue(IArgument.DATA_FILE);
        List attrValues = rc.getOption(IArgument.ATTRIBUTE_VALUES);

        if ((datafile == null) && (attrValues == null)) {
            throw new CLIException(
                getResourceString("missing-attributevalues"),
                ExitCodes.INCORRECT_OPTION, rc.getSubCommand().getName());
        }
        
        int priority = 0;
        String strPriority = getStringOptionValue(OPTION_PRIORITY);
        if ((strPriority != null) && (strPriority.length() > 0)) {
            try {
                priority = Integer.parseInt(strPriority);
            } catch (NumberFormatException ex) {
                throw new CLIException(getResourceString(
                    "add-sub-configuration-priority-no-integer"),
                    ExitCodes.INVALID_OPTION_VALUE);
            }
        }

        Map attributeValues = AttributeValues.parse(
            getCommandManager(), datafile, attrValues);

        if ((realmName == null) || (realmName.length() == 0)) {
            addSubConfigToRoot(serviceName, subConfigName, subConfigId,
                attributeValues, priority);
        } else {
            addSubConfigToRealm(realmName, serviceName, subConfigName,
                subConfigId, attributeValues, priority);
        }
    }

    private void addSubConfigToRealm(
        String realmName,
        String serviceName,
        String subConfigName,
        String subConfigId,
        Map attrValues,
        int priority
    ) throws CLIException {
        SSOToken adminSSOToken = getAdminSSOToken();
        IOutput outputWriter = getOutputWriter();
        String[] params = {realmName, subConfigName, serviceName};

        writeLog(LogWriter.LOG_ACCESS, Level.INFO,
            "ATTEMPT_ADD_SUB_CONFIGURATION_TO_REALM", params);

        try {
            ServiceConfigManager scm = new ServiceConfigManager(
                serviceName, adminSSOToken);
            ServiceConfig sc = scm.getOrganizationConfig(realmName, null);

            if (sc == null) {
                sc = scm.createOrganizationConfig(realmName, null);
            }
            addSubConfig(sc, subConfigName, subConfigId, attrValues, priority);
            writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                "SUCCEED_ADD_SUB_CONFIGURATION_TO_REALM", params);
            outputWriter.printlnMessage(MessageFormat.format(
                getResourceString("add-sub-configuration-to-realm-succeed"),
                (Object[])params));
        } catch (SSOException e) {
            String[] args = {realmName, subConfigName, serviceName,
                e.getMessage()};
            debugError("AddSubConfiguration.addSubConfigToRealm", e);
            writeLog(LogWriter.LOG_ERROR, Level.INFO,
                "FAILED_ADD_SUB_CONFIGURATIONT_TO_REALM", args);
            throw new CLIException(e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (SMSException e) {
            String[] args = {realmName, subConfigName, serviceName,
                e.getMessage()};
            debugError("AddSubConfiguration.addSubConfigToRealm", e);
            writeLog(LogWriter.LOG_ERROR, Level.INFO,
                "FAILED_ADD_SUB_CONFIGURATIONT_TO_REALM", args);
            throw new CLIException(e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        }
    }

    private void addSubConfigToRoot(
        String serviceName,
        String subConfigName,
        String subConfigId,
        Map attrValues,
        int priority
    ) throws CLIException {
        SSOToken adminSSOToken = getAdminSSOToken();
        IOutput outputWriter = getOutputWriter();
        String[] params = {subConfigName, serviceName};

        writeLog(LogWriter.LOG_ACCESS, Level.INFO,
            "ATTEMPT_ADD_SUB_CONFIGURATION", params);

        try {
            ServiceConfigManager scm = new ServiceConfigManager(
                serviceName, adminSSOToken);
            ServiceConfig sc = scm.getGlobalConfig(null);

            if (sc == null) {
                String[] args = {subConfigName, serviceName,
                    "no global configiration"};
                writeLog(LogWriter.LOG_ERROR, Level.INFO,
                    "FAILED_ADD_SUB_CONFIGURATION", args);
                String[] ar = {serviceName};
                String message = MessageFormat.format(
                    getResourceString("add-sub-configuration-no-global-config"),
                    (Object[])ar);
                throw new CLIException(message,
                    ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
            }

            addSubConfig(sc, subConfigName, subConfigId, attrValues, priority);
            writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                "SUCCEED_ADD_SUB_CONFIGURATION", params);
            outputWriter.printlnMessage(MessageFormat.format(
                getResourceString("add-sub-configuration-succeed"),
                (Object[])params));
        } catch (SSOException e) {
            String[] args = {subConfigName, serviceName, e.getMessage()};
            debugError("AddSubConfiguration.addSubConfigToRoot", e);
            writeLog(LogWriter.LOG_ERROR, Level.INFO,
                "FAILED_ADD_SUB_CONFIGURATION", args);
            throw new CLIException(e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (SMSException e) {
            e.printStackTrace();
            String[] args = {subConfigName, serviceName, e.getMessage()};
            debugError("AddSubConfiguration.addSubConfigToRoot", e);
            writeLog(LogWriter.LOG_ERROR, Level.INFO,
                "FAILED_ADD_SUB_CONFIGURATION", args);
            throw new CLIException(e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        }
    }

    private void addSubConfig(
        ServiceConfig sc,
        String subConfigName,
        String subConfigId,
        Map attrValues,
        int priority
    ) throws SSOException, SMSException {
        StringTokenizer st = new StringTokenizer(subConfigName, "/");
        int tokenCount = st.countTokens();

        for (int i = 1; i <= tokenCount; i++) {
            String scn = SMSSchema.unescapeName(st.nextToken());

            if (i != tokenCount) {
                sc = sc.getSubConfig(scn);
            } else {
                if (subConfigId == null) {
                    subConfigId = subConfigName;
                }

                sc.addSubConfig(scn, subConfigId, priority, attrValues);
             }
        }
    }
}
