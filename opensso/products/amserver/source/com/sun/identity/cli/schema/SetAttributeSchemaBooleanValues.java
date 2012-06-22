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
 * $Id: SetAttributeSchemaBooleanValues.java,v 1.4 2008/12/04 06:32:07 veiming Exp $
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
import java.util.logging.Level;

/**
 * Sets attribute choice values to attribute schema.
 */
public class SetAttributeSchemaBooleanValues extends SchemaCommand {
    static final String ARGUMENT_TRUE_VALUE = "truevalue";
    static final String ARGUMENT_FALSE_VALUE = "falsevalue";
    static final String ARGUMENT_TRUE_I18N_KEY = "truei18nkey";
    static final String ARGUMENT_FALSE_I18N_KEY = "falsei18nkey";

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
        String trueValue = getStringOptionValue(ARGUMENT_TRUE_VALUE);
        String trueI18nKey = getStringOptionValue(ARGUMENT_TRUE_I18N_KEY);
        String falseValue = getStringOptionValue(ARGUMENT_FALSE_VALUE);
        String falseI18nKey = getStringOptionValue(ARGUMENT_FALSE_I18N_KEY);

        ServiceSchema ss = getServiceSchema();
        IOutput outputWriter = getOutputWriter();
        String[] params = {serviceName, schemaType, subSchemaName,
            attributeName};

        writeLog(LogWriter.LOG_ACCESS, Level.INFO,
            "ATTEMPT_SET_ATTRIBUTE_SCHEMA_BOOLEAN_VALUES", params);

        try {
            AttributeSchema attrSchema = ss.getAttributeSchema(
                attributeName);

            if (attrSchema == null) {
                String[] args = {serviceName, schemaType, subSchemaName,
                    attributeName, "attribute schema does not exist"};
                attributeSchemaNoExist(attributeName,
                    "FAILED_SET_ATTRIBUTE_SCHEMA_BOOLEAN_VALUES", args);
            }

            attrSchema.setBooleanValues(trueValue, trueI18nKey, falseValue, 
                falseI18nKey);
            writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                "SUCCEED_SET_ATTRIBUTE_SCHEMA_BOOLEAN_VALUES", params);
            outputWriter.printlnMessage(MessageFormat.format(
                getResourceString(
                "attribute-schema-set-boolean-values-succeed"),
                (Object[])params));
        } catch (SSOException e) {
            String[] args = {serviceName, schemaType, subSchemaName,
                attributeName, e.getMessage()};
            debugError("SetAttributeSchemaBooleanValues.handleRequest", e);
            writeLog(LogWriter.LOG_ERROR, Level.INFO,
                "FAILED_SET_ATTRIBUTE_SCHEMA_BOOLEAN_VALUES", args);
            throw new CLIException(e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (SMSException e) {
            String[] args = {serviceName, schemaType, subSchemaName,
                attributeName, e.getMessage()};
            debugError("SetAttributeSchemaBooleanValues.handleRequest", e);
            writeLog(LogWriter.LOG_ERROR, Level.INFO,
                "FAILED_SET_ATTRIBUTE_SCHEMA_BOOLEAN_VALUES", args);
            throw new CLIException(e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        }
    }
}
