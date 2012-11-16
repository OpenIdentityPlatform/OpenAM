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
 * $Id: DeleteSubConfiguration.java,v 1.3 2008/06/25 05:42:17 qcheng Exp $
 *
 */

package com.sun.identity.cli.schema;


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
import java.util.StringTokenizer;
import java.util.logging.Level;

/**
 * Delete sub configuration.
 */
public class DeleteSubConfiguration extends SchemaCommand {
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

        if ((realmName == null) || (realmName.length() == 0)) {
            deleteSubConfigToRoot(serviceName, subConfigName);
        } else {
            deleteSubConfigFromRealm(realmName, serviceName, subConfigName);
        }
    }

    private void deleteSubConfigFromRealm(
        String realmName,
        String serviceName,
        String subConfigName
    ) throws CLIException {
        SSOToken adminSSOToken = getAdminSSOToken();
        IOutput outputWriter = getOutputWriter();
        String[] params = {realmName, subConfigName, serviceName};

        writeLog(LogWriter.LOG_ACCESS, Level.INFO,
            "ATTEMPT_DELETE_SUB_CONFIGURATION_TO_REALM", params);

        try {
            ServiceConfigManager scm = new ServiceConfigManager(
                serviceName, adminSSOToken);
            ServiceConfig sc = scm.getOrganizationConfig(realmName, null);

            if (sc == null) {
                sc = scm.createOrganizationConfig(realmName, null);
            }
            deleteSubConfig(sc, subConfigName);
            writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                "SUCCEED_DELETE_SUB_CONFIGURATION_TO_REALM", params);
            outputWriter.printlnMessage(MessageFormat.format(
                getResourceString("delete-sub-configuration-to-realm-succeed"),
                    (Object[])params));
        } catch (SSOException e) {
            String[] args = {realmName, subConfigName, serviceName,
                e.getMessage()};
            debugError(
                "DeleteSubConfiguration.deleteSubConfigFromRealm", e);
            writeLog(LogWriter.LOG_ERROR, Level.INFO,
                "FAILED_DELETE_SUB_CONFIGURATIONT_TO_REALM", args);
            throw new CLIException(e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (SMSException e) {
            String[] args = {realmName, subConfigName, serviceName,
                e.getMessage()};
            debugError(
                "DeleteSubConfiguration.deleteSubConfigFromRealm", e);
            writeLog(LogWriter.LOG_ERROR, Level.INFO,
                "FAILED_DELETE_SUB_CONFIGURATIONT_TO_REALM", args);
            throw new CLIException(e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        }
    }

    private void deleteSubConfigToRoot(String serviceName, String subConfigName)
        throws CLIException {
        SSOToken adminSSOToken = getAdminSSOToken();
        IOutput outputWriter = getOutputWriter();
        String[] params = {subConfigName, serviceName};

        writeLog(LogWriter.LOG_ACCESS, Level.INFO,
            "ATTEMPT_DELETE_SUB_CONFIGURATION", params);

        try {
            ServiceConfigManager scm = new ServiceConfigManager(
                serviceName, adminSSOToken);
            ServiceConfig sc = scm.getGlobalConfig(null);
            deleteSubConfig(sc, subConfigName);
            writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                "SUCCEED_DELETE_SUB_CONFIGURATION", params);
            outputWriter.printlnMessage(MessageFormat.format(
                getResourceString("delete-sub-configuration-succeed"),
                (Object[])params));
        } catch (SSOException e) {
            String[] args = {subConfigName, serviceName, e.getMessage()};
            debugError("DeleteSubConfiguration.deleteSubConfigToRoot", e);
            writeLog(LogWriter.LOG_ERROR, Level.INFO,
                "FAILED_DELETE_SUB_CONFIGURATION", args);
            throw new CLIException(e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (SMSException e) {
            String[] args = {subConfigName, serviceName, e.getMessage()};
            debugError("DeleteSubConfiguration.deleteSubConfigToRoot", e);
            writeLog(LogWriter.LOG_ERROR, Level.INFO,
                "FAILED_DELETE_SUB_CONFIGURATION", args);
            throw new CLIException(e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        }
    }

    private void deleteSubConfig(ServiceConfig sc, String subConfigName)
        throws SSOException, SMSException {
        StringTokenizer st = new StringTokenizer(subConfigName, "/");
        int tokenCount = st.countTokens();

        for (int i = 1; i <= tokenCount; i++) {
            String scn = SMSSchema.unescapeName(st.nextToken());

            if (i != tokenCount) {
                sc = sc.getSubConfig(scn);
            } else {
                sc.removeSubConfig(scn);
             }
        }
    }
}
