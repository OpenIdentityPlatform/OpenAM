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
import com.sun.identity.cli.CommandManager;
import com.sun.identity.cli.ExitCodes;
import com.sun.identity.cli.IArgument;
import com.sun.identity.cli.IOutput;
import com.sun.identity.cli.LogWriter;
import com.sun.identity.cli.RequestContext;
import com.sun.identity.cli.schema.SchemaCommand;
import com.sun.identity.shared.xml.XMLUtils;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceManager;
import java.text.MessageFormat;
import java.util.logging.Level;
import org.w3c.dom.Document;

/**
 * Add a Plug-in schema to a service.
 *
 * @author Steve Ferris steve.ferris@forgerock.com
 */
public class AddPluginSchema extends SchemaCommand {
    private static final String ARGUMENT_INTERFACE_NAME = "interfacename";
    private static final String ARGUMENT_PLUGIN_NAME = "pluginname";
    private static final String ARGUMENT_I18N_KEY = "i18nkey";
    private static final String ARGUMENT_I18N_NAME = "i18nname";
    private static final String ARGUMENT_CLASS_NAME = "classname";

    private static final String SCHEMA_1 =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n\n" +
            "<ServicesConfiguration><Service name=\"";
    private static final String SCHEMA_2 = "\" version=\"1.0\"><PluginSchema className=\"";
    private static final String SCHEMA_3 = "\"  i18nFileName=\"";
    private static final String SCHEMA_4 = "\"  i18nKey=\"";
    private static final String SCHEMA_5 = "\"  interfaceName=\"";
    private static final String SCHEMA_6 = "\"  name=\"";
    private static final String SCHEMA_7 = "\"></PluginSchema></Service></ServicesConfiguration>";

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
        String i18nKey = getStringOptionValue(ARGUMENT_I18N_KEY);
        String i18nName = getStringOptionValue(ARGUMENT_I18N_NAME);
        String className = getStringOptionValue(ARGUMENT_CLASS_NAME);

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
                               pluginName,
                               i18nKey,
                               i18nName,
                               className};
            writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                "ATTEMPT_ADD_PLUGIN_SCHEMA", params);
            Document pluginDoc = createPluginSchemaXML(serviceName,
                                                       interfaceName,
                                                       pluginName,
                                                       i18nKey,
                                                       i18nName,
                                                       className);

            if (pluginDoc != null) {
                sm.addPluginSchema(pluginDoc);
                String[] params2 = {serviceName,
                                    pluginName};
                writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                    "SUCCEED_ADD_PLUGIN_SCHEMA", params2);
                outputWriter.printlnMessage(MessageFormat.format(
                    getResourceString("add-plugin-schema-succeed"),
                    (Object[])params));
            } else {
                String[] args = {serviceName, pluginName, "Null XML Document"};
                debugError("AddPluginSchema.handleRequest:: Null XML Document");
                writeLog(LogWriter.LOG_ERROR, Level.INFO,
                    "FAILED_ADD_PLUGIN_SCHEMA", args);
                throw new CLIException("Null XML Document", ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
            }
        } catch (SSOException ssoe) {
            String[] args = {serviceName, pluginName, ssoe.getMessage()};
            debugError("AddPluginSchema.handleRequest", ssoe);
            writeLog(LogWriter.LOG_ERROR, Level.INFO,
                "FAILED_ADD_PLUGIN_SCHEMA", args);
            outputWriter.printlnMessage(MessageFormat.format(
                    getResourceString("add-plugin-schema-failed"),
                    (Object[])args));
            throw new CLIException(ssoe, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (SMSException smse) {
            String[] args = {serviceName, pluginName, smse.getMessage()};
            debugError("AddPluginSchema.handleRequest", smse);
            writeLog(LogWriter.LOG_ERROR, Level.INFO,
                "FAILED_ADD_PLUGIN_SCHEMA", args);
            outputWriter.printlnMessage(MessageFormat.format(
                    getResourceString("add-plugin-schema-failed"),
                    (Object[])args));
            throw new CLIException(smse, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        }
    }

    protected Document createPluginSchemaXML(String serviceName,
                                         String interfaceName,
                                         String pluginName,
                                         String i18nKey,
                                         String i18nName,
                                         String className) {
        StringBuilder pluginSchemaBuffer = new StringBuilder();
        pluginSchemaBuffer.append(SCHEMA_1);
        pluginSchemaBuffer.append(serviceName);
        pluginSchemaBuffer.append(SCHEMA_2);
        pluginSchemaBuffer.append(className);
        pluginSchemaBuffer.append(SCHEMA_3);
        pluginSchemaBuffer.append(i18nName);
        pluginSchemaBuffer.append(SCHEMA_4);
        pluginSchemaBuffer.append(i18nKey);
        pluginSchemaBuffer.append(SCHEMA_5);
        pluginSchemaBuffer.append(interfaceName);
        pluginSchemaBuffer.append(SCHEMA_6);
        pluginSchemaBuffer.append(pluginName);
        pluginSchemaBuffer.append(SCHEMA_7);

        return XMLUtils.toDOMDocument(pluginSchemaBuffer.toString(), CommandManager.getDebugger());
    }
}

