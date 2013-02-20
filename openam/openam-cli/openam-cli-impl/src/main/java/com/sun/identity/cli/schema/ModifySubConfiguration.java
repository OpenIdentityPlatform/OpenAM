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
 * $Id: ModifySubConfiguration.java,v 1.4 2008/06/25 05:42:18 qcheng Exp $
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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.logging.Level;

/**
 * Modifies sub configuration.
 */
public class ModifySubConfiguration extends SchemaCommand {
    static final String ARGUMENT_OPERATION = "operation";

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
        String operation = getStringOptionValue(ARGUMENT_OPERATION);
        String datafile = getStringOptionValue(IArgument.DATA_FILE);
        List attrValues = rc.getOption(IArgument.ATTRIBUTE_VALUES);

        if ((datafile == null) && (attrValues == null)) {
            throw new CLIException(
                getResourceString("missing-attributevalues"),
                ExitCodes.INCORRECT_OPTION, rc.getSubCommand().getName());
        }

        Map attributeValues = AttributeValues.parse(
            getCommandManager(), datafile, attrValues);

        if ((realmName == null) || (realmName.length() == 0)) {
            modifySubConfigRoot(serviceName, subConfigName, attributeValues,
                operation);
        } else {
            modifySubConfigToRealm(realmName, serviceName, subConfigName,
                attributeValues, operation);
        }
    }

    private void modifySubConfigToRealm(
        String realmName,
        String serviceName,
        String subConfigName,
        Map attrValues,
        String operation
    ) throws CLIException {
        SSOToken adminSSOToken = getAdminSSOToken();
        IOutput outputWriter = getOutputWriter();
        String[] params = {realmName, subConfigName, serviceName};

        writeLog(LogWriter.LOG_ACCESS, Level.INFO,
            "ATTEMPT_MODIFY_SUB_CONFIGURATION_IN_REALM", params);

        try {
            ServiceConfigManager scm = new ServiceConfigManager(
                serviceName, adminSSOToken);
            ServiceConfig sc = scm.getOrganizationConfig(realmName, null);

            if (sc == null) {
                sc = scm.createOrganizationConfig(realmName, null);
            }
            modifySubConfig(sc, subConfigName, attrValues, operation);
            writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                "SUCCEED_MODIFY_SUB_CONFIGURATION_IN_REALM", params);
            outputWriter.printlnMessage(MessageFormat.format(
                getResourceString("modify-sub-configuration-to-realm-succeed"),
                    (Object[])params));
        } catch (SSOException e) {
            String[] args = {realmName, subConfigName, serviceName,
                e.getMessage()};
            debugError("ModifySubConfiguration.modifySubConfigToRealm", e);
            writeLog(LogWriter.LOG_ERROR, Level.INFO,
                "FAILED_MODIFY_SUB_CONFIGURATIONT_IN_REALM", args);
            throw new CLIException(e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (SMSException e) {
            String[] args = {realmName, subConfigName, serviceName,
                e.getMessage()};
            debugError("ModifySubConfiguration.modifySubConfigToRealm", e);
            writeLog(LogWriter.LOG_ERROR, Level.INFO,
                "FAILED_MODIFY_SUB_CONFIGURATIONT_IN_REALM", args);
            throw new CLIException(e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        }
    }

    private void modifySubConfigRoot(
        String serviceName,
        String subConfigName,
        Map attrValues,
        String operation
    ) throws CLIException {
        SSOToken adminSSOToken = getAdminSSOToken();
        IOutput outputWriter = getOutputWriter();
        String[] params = {subConfigName, serviceName};

        writeLog(LogWriter.LOG_ACCESS, Level.INFO,
            "ATTEMPT_MODIFY_SUB_CONFIGURATION", params);

        try {
            ServiceConfigManager scm = new ServiceConfigManager(
                serviceName, adminSSOToken);
            ServiceConfig sc = scm.getGlobalConfig(null);
            modifySubConfig(sc, subConfigName, attrValues, operation);
            writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                "SUCCEED_MODIFY_SUB_CONFIGURATION", params);
            outputWriter.printlnMessage(MessageFormat.format(
                getResourceString("modify-sub-configuration-succeed"),
                (Object[])params));
        } catch (SSOException e) {
            String[] args = {subConfigName, serviceName, e.getMessage()};
            debugError("ModifySubConfiguration.addSubConfigToRoot", e);
            writeLog(LogWriter.LOG_ERROR, Level.INFO,
                "FAILED_MODIFY_SUB_CONFIGURATION", args);
            throw new CLIException(e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (SMSException e) {
            String[] args = {subConfigName, serviceName, e.getMessage()};
            debugError("ModifySubConfiguration.addSubConfigToRoot", e);
            writeLog(LogWriter.LOG_ERROR, Level.INFO,
                "FAILED_MODIFY_SUB_CONFIGURATION", args);
            throw new CLIException(e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        }
    }

    private void modifySubConfig(
        ServiceConfig sc,
        String subConfigName,
        Map attrValues,
        String operation
    ) throws SSOException, SMSException, CLIException{
        StringTokenizer st = new StringTokenizer(subConfigName, "/");
        int tokenCount = st.countTokens();

        for (int i = 1; i <= tokenCount; i++) {
            String scn = SMSSchema.unescapeName(st.nextToken());
            sc = sc.getSubConfig(scn);
        }

        if (operation.equals("set")) {
            sc.setAttributes(attrValues);
        } else if (operation.equals("add")) {
            for (Iterator i = attrValues.keySet().iterator(); i.hasNext(); ) {
                String attrName = (String)i.next();
                sc.addAttribute(attrName, (Set)attrValues.get(attrName));
            }
        } else if (operation.equals("delete")) {
            for (Iterator i = attrValues.keySet().iterator(); i.hasNext(); ) {
                sc.removeAttribute((String)i.next());
            }
        } else {
            throw new CLIException(
                getResourceString("modify-sub-configuration-invalid-operation"),
                ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        }
    }
}
