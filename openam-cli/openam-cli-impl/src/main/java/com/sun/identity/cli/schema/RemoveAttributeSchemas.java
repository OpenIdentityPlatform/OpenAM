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
 * $Id: RemoveAttributeSchemas.java,v 1.2 2008/06/25 05:42:18 qcheng Exp $
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
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceSchema;
import java.text.MessageFormat;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;

/**
 * Remove attribute schema(s).
 */
public class RemoveAttributeSchemas extends SchemaCommand {
    
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

        String schemaType = getStringOptionValue(IArgument.SCHEMA_TYPE);
        String serviceName = getStringOptionValue(IArgument.SERVICE_NAME);
        String subSchemaName = getStringOptionValue(IArgument.SUBSCHEMA_NAME);
        List attributeSchemaNames = (List)rc.getOption(
            IArgument.ATTRIBUTE_SCHEMA);

        ServiceSchema ss = getServiceSchema();
        IOutput outputWriter = getOutputWriter();
        String attributeSchemaName = null;

        try {
            for (Iterator i = attributeSchemaNames.iterator(); i.hasNext(); ) {
                attributeSchemaName = (String)i.next();
                String[] params = {serviceName, schemaType, subSchemaName,
                    attributeSchemaName};
                writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                    "ATTEMPT_REMOVE_ATTRIBUTE_SCHEMA", params);
                ss.removeAttributeSchema(attributeSchemaName);
                writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                    "SUCCEED_REMOVE_ATTRIBUTE_SCHEMA", params);
                outputWriter.printlnMessage(MessageFormat.format(
                    getResourceString("remove-attribute-schema-succeed"),
                    (Object[])params));
            }
        } catch (SSOException e) {
            String[] args = {serviceName, schemaType, subSchemaName,
                attributeSchemaName, e.getMessage()};
            debugError("RemoveAttributeSchemas.handleRequest", e);
            writeLog(LogWriter.LOG_ERROR, Level.INFO,
                "FAILED_REMOVE_ATTRIBUTE_SCHEMA", args);
            throw new CLIException(e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (SMSException e) {
            String[] args = {serviceName, schemaType, subSchemaName,
                attributeSchemaName, e.getMessage()};
            debugError("RemoveAttributeSchemas.handleRequest", e);
            writeLog(LogWriter.LOG_ERROR, Level.INFO,
                "FAILED_REMOVE_ATTRIBUTE_SCHEMA", args);
            throw new CLIException(e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        }
    }
}
