/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010 ForgeRock AS. All Rights Reserved
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
import com.sun.identity.cli.IArgument;
import com.sun.identity.cli.IOutput;
import com.sun.identity.cli.LogWriter;
import com.sun.identity.cli.RequestContext;
import com.sun.identity.cli.schema.SchemaCommand;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceManager;
import java.text.MessageFormat;
import java.util.logging.Level;

/**
 * Remove a Plug-in schema from a service.
 *
 * @author Steve Ferris steve.ferris@forgerock.com
 */
public class RemovePluginSchema extends SchemaCommand {
    private static final String ARGUMENT_PLUGIN_NAME = "pluginname";
    private static final String ARGUMENT_INTERFACE_NAME = "interfacename";

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

        String serviceName = getStringOptionValue(IArgument.SERVICE_NAME);
        String interfaceName = getStringOptionValue(ARGUMENT_INTERFACE_NAME);
        String pluginName = getStringOptionValue(ARGUMENT_PLUGIN_NAME);

        ServiceManager sm = null;

        try {
            sm = new ServiceManager(adminSSOToken);
        } catch (SMSException smse) {
            throw new CLIException(smse, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (SSOException ssoe) {
            throw new CLIException(ssoe, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        }

        IOutput outputWriter = getOutputWriter();

        try {
            String[] params = {serviceName,
                               interfaceName,
                               pluginName};
            writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                "ATTEMPT_REMOVE_PLUGIN_SCHEMA", params);
            sm.removePluginSchema(serviceName,
                               interfaceName,
                               pluginName);
            String[] params2 = {serviceName,
                                pluginName};
            writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                "SUCCEED_REMOVE_PLUGIN_SCHEMA", params2);
            outputWriter.printlnMessage(MessageFormat.format(
                getResourceString("remove-plugin-schema-succeed"),
                (Object[])params));
        } catch (SSOException ssoe) {
            String[] args = {serviceName, pluginName, ssoe.getMessage()};
            debugError("RemovePluginSchema.handleRequest", ssoe);
            writeLog(LogWriter.LOG_ERROR, Level.INFO,
                "FAILED_REMOVE_PLUGIN_SCHEMA", args);
            outputWriter.printlnMessage(MessageFormat.format(
                getResourceString("remove-plugin-schema-failed"),
                (Object[])args));
            throw new CLIException(ssoe, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (SMSException smse) {
            String[] args = {serviceName, pluginName, smse.getMessage()};
            debugError("RemovePluginSchema.handleRequest", smse);
            writeLog(LogWriter.LOG_ERROR, Level.INFO,
                "FAILED_REMOVE_PLUGIN_SCHEMA", args);
            outputWriter.printlnMessage(MessageFormat.format(
                getResourceString("remove-plugin-schema-failed"),
                (Object[])args));
            throw new CLIException(smse, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        }
    }
}
