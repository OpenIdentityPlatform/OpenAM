/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 ForgeRock Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 */
package org.forgerock.openam.cli.schema;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.cli.CLIException;
import com.sun.identity.cli.ExitCodes;
import com.sun.identity.cli.FormatUtils;
import com.sun.identity.cli.IArgument;
import com.sun.identity.cli.IOutput;
import com.sun.identity.cli.LogWriter;
import com.sun.identity.cli.RequestContext;
import com.sun.identity.cli.schema.SchemaCommand;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.SMSSchema;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceConfigManager;
import java.text.MessageFormat;
import java.util.StringTokenizer;
import java.util.logging.Level;

/**
 * This ssoadm command is able to retrieve the values of the subconfiguration
 * elements within SMS. One possibility is to use this command to retrieve
 * the SFO configuration.
 *
 * @author Peter Major
 */
public class GetSubConfiguration extends SchemaCommand {

    @Override
    public void handleRequest(RequestContext rc) throws CLIException {
        super.handleRequest(rc);
        ldapLogin();

        String serviceName = getStringOptionValue(IArgument.SERVICE_NAME);
        String subConfigName = getStringOptionValue(IArgument.SUB_CONFIGURATION_NAME);
        String realmName = getStringOptionValue(IArgument.REALM_NAME);

        if ((realmName == null) || (realmName.length() == 0)) {
            printGlobalSubConfig(serviceName, subConfigName);
        } else {
            printRealmSubConfig(realmName, serviceName, subConfigName);
        }
    }

        private void printRealmSubConfig(
        String realmName,
        String serviceName,
        String subConfigName
    ) throws CLIException {
        SSOToken adminSSOToken = getAdminSSOToken();
        IOutput outputWriter = getOutputWriter();
        String[] params = {realmName, subConfigName, serviceName};

        writeLog(LogWriter.LOG_ACCESS, Level.INFO,
            "ATTEMPT_GET_SUB_CONFIGURATION_IN_REALM", params);

        try {
            ServiceConfigManager scm = new ServiceConfigManager(
                serviceName, adminSSOToken);
            ServiceConfig sc = scm.getOrganizationConfig(realmName, null);

            if (sc == null) {
                sc = scm.createOrganizationConfig(realmName, null);
            }
            printSubConfig(sc, subConfigName);
            writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                "SUCCEED_GET_SUB_CONFIGURATION_IN_REALM", params);
            outputWriter.printlnMessage(MessageFormat.format(
                getResourceString("get-sub-configuration-to-realm-succeed"),
                    (Object[])params));
        } catch (SSOException e) {
            String[] args = {realmName, subConfigName, serviceName,
                e.getMessage()};
            debugError("GetSubConfiguration.printRealmSubConfig", e);
            writeLog(LogWriter.LOG_ERROR, Level.INFO,
                "FAILED_GET_SUB_CONFIGURATIONT_IN_REALM", args);
            throw new CLIException(e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (SMSException e) {
            String[] args = {realmName, subConfigName, serviceName,
                e.getMessage()};
            debugError("GetSubConfiguration.printRealmSubConfig", e);
            writeLog(LogWriter.LOG_ERROR, Level.INFO,
                "FAILED_GET_SUB_CONFIGURATIONT_IN_REALM", args);
            throw new CLIException(e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        }
    }
    
    private void printGlobalSubConfig(
            String serviceName,
            String subConfigName) throws CLIException {
        SSOToken adminSSOToken = getAdminSSOToken();
        IOutput outputWriter = getOutputWriter();
        String[] params = {subConfigName, serviceName};

        writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                "ATTEMPT_GET_SUB_CONFIGURATION", params);

        try {
            ServiceConfigManager scm = new ServiceConfigManager(
                    serviceName, adminSSOToken);
            ServiceConfig sc = scm.getGlobalConfig(null);
            printSubConfig(sc, subConfigName);
            writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                    "SUCCEED_GET_SUB_CONFIGURATION", params);
            outputWriter.printlnMessage(MessageFormat.format(
                    getResourceString("get-sub-configuration-succeed"),
                    (Object[]) params));
        } catch (SSOException e) {
            String[] args = {subConfigName, serviceName, e.getMessage()};
            debugError("GetSubConfiguration.printGlobalSubConfig", e);
            writeLog(LogWriter.LOG_ERROR, Level.INFO,
                    "FAILED_GET_SUB_CONFIGURATION", args);
            throw new CLIException(e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (SMSException e) {
            String[] args = {subConfigName, serviceName, e.getMessage()};
            debugError("GetSubConfiguration.printGlobalSubConfig", e);
            writeLog(LogWriter.LOG_ERROR, Level.INFO,
                    "FAILED_GET_SUB_CONFIGURATION", args);
            throw new CLIException(e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        }
    }

    private void printSubConfig(
            ServiceConfig sc,
            String subConfigName) throws SSOException, SMSException, CLIException {
        StringTokenizer st = new StringTokenizer(subConfigName, "/");
        int tokenCount = st.countTokens();

        for (int i = 1; i <= tokenCount; i++) {
            String scn = SMSSchema.unescapeName(st.nextToken());
            sc = sc.getSubConfig(scn);
        }

        sc.getAttributes();
        getOutputWriter().printlnMessage(
                FormatUtils.printAttributeValues("{0}={1}",
                sc.getAttributes()));
    }
}
