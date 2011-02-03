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
 * $Id: AddSubSchema.java,v 1.4 2008/06/25 05:42:17 qcheng Exp $
 *
 */

package com.sun.identity.cli.schema;


import com.sun.identity.cli.CLIException;
import com.sun.identity.cli.CommandManager;
import com.sun.identity.cli.ExitCodes;
import com.sun.identity.cli.IArgument;
import com.sun.identity.cli.IOutput;
import com.sun.identity.cli.LogWriter;
import com.sun.identity.cli.RequestContext;
import com.iplanet.sso.SSOException;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceSchema;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.logging.Level;

/**
 * Adds sub configuration.
 */
public class AddSubSchema extends SchemaCommand {
    final static String ARGUMENT_FILENAME = "filename";

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
        String schemaType = getStringOptionValue(IArgument.SCHEMA_TYPE);
        String subSchemaName = getStringOptionValue(IArgument.SUBSCHEMA_NAME);
        String fileName = getStringOptionValue(ARGUMENT_FILENAME);
        
        if (subSchemaName == null) {
            subSchemaName = "/";
        }

        IOutput outputWriter = getOutputWriter();
        String[] params = {serviceName, schemaType, subSchemaName};

        ServiceSchema ss = getServiceSchema();
        CommandManager mgr = getCommandManager();
        String url = mgr.getWebEnabledURL();
        writeLog(LogWriter.LOG_ACCESS, Level.INFO,
            "ATTEMPT_ADD_SUB_SCHEMA", params);

        try {
            if ((url != null) && (url.length() > 0)) {
                ss.addSubSchema(new ByteArrayInputStream(fileName.getBytes()));
            } else {
                ss.addSubSchema(new FileInputStream(fileName));
            }
            writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                "SUCCEED_ADD_SUB_SCHEMA", params);
            outputWriter.printlnMessage(MessageFormat.format(
                getResourceString("add-subschema-succeed"),
                    (Object[])params));
        } catch (SSOException e) {
            String[] args = {serviceName, schemaType, subSchemaName,
                e.getMessage()};
            debugError("AddSubSchema.handleRequest", e);
            writeLog(LogWriter.LOG_ERROR, Level.INFO,
                "FAILED_ADD_SUB_SCHEMA", args);
            throw new CLIException(e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (SMSException e) {
            String[] args = {serviceName, schemaType, subSchemaName,
                e.getMessage()};
            debugError("AddSubSchema.handleRequest", e);
            writeLog(LogWriter.LOG_ERROR, Level.INFO,
                "FAILED_ADD_SUB_SCHEMA", args);
            throw new CLIException(e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (IOException e) {
            String[] args = {serviceName, schemaType, subSchemaName,
                e.getMessage()};
            debugError("AddSubSchema.handleRequest", e);
            writeLog(LogWriter.LOG_ERROR, Level.INFO,
                "FAILED_ADD_SUB_SCHEMA", args);
            throw new CLIException(e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        }
    }
}
