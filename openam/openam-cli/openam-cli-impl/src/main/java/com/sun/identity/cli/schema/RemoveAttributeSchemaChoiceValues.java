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
 * $Id: RemoveAttributeSchemaChoiceValues.java,v 1.3 2008/12/04 06:32:07 veiming Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.cli.schema;


import com.sun.identity.cli.CLIException;
import com.sun.identity.cli.ExitCodes;
import com.sun.identity.cli.IArgument;
import com.sun.identity.cli.IOutput;
import com.sun.identity.cli.LogWriter;
import com.sun.identity.cli.RequestContext;
import com.iplanet.sso.SSOException;
import com.sun.identity.sm.AttributeSchema;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceSchema;
import java.text.MessageFormat;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;

/**
 * Remove attribute choice values to attribute schema.
 */
public class RemoveAttributeSchemaChoiceValues extends SchemaCommand {
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
        String attributeName = getStringOptionValue(IArgument.ATTRIBUTE_NAME);
        List choiceValues = (List)rc.getOption(IArgument.CHOICE_VALUES);

        ServiceSchema ss = getServiceSchema();
        IOutput outputWriter = getOutputWriter();
        String[] params = {serviceName, schemaType, subSchemaName,
            attributeName};
        writeLog(LogWriter.LOG_ACCESS, Level.INFO,
            "ATTEMPT_REMOVE_ATTRIBUTE_SCHEMA_CHOICE_VALUE", params);

        try {
            AttributeSchema attrSchema = ss.getAttributeSchema(attributeName);

            if (attrSchema == null) {
                String[] args = {serviceName, schemaType, subSchemaName,
                    attributeName, "attribute schema does not exist"};
                attributeSchemaNoExist(attributeName,
                    "FAILED_REMOVE_ATTRIBUTE_SCHEMA_CHOICE_VALUE", args);
            }

            for (Iterator i = choiceValues.iterator(); i.hasNext(); ) {
                String choiceValue = (String)i.next();
                attrSchema.removeChoiceValue(choiceValue);
            }
            writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                "SUCCEED_REMOVE_ATTRIBUTE_SCHEMA_CHOICE_VALUE", params);
            outputWriter.printlnMessage(MessageFormat.format(
                getResourceString(
                    "attribute-schema-remove-choice-value-succeed"), 
                (Object[])params));
        } catch (SSOException e) {
            String[] args = {serviceName, schemaType, subSchemaName,
                attributeName, e.getMessage()};
            debugError("RemoveAttributeSchemaChoiceValues.handleRequest",e);
            writeLog(LogWriter.LOG_ERROR, Level.INFO,
                "FAILED_REMOVE_ATTRIBUTE_SCHEMA_CHOICE_VALUE", args);
            throw new CLIException(e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (SMSException e) {
            String[] args = {serviceName, schemaType, subSchemaName,
                attributeName, e.getMessage()};
            debugError("RemoveAttributeSchemaChoiceValues.handleRequest",e);
            writeLog(LogWriter.LOG_ERROR, Level.INFO,
                "FAILED_REMOVE_ATTRIBUTE_SCHEMA_CHOICE_VALUE", args);
            throw new CLIException(e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        }
    }
}
