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
 * $Id: SetAttributeSchemaChoiceValues.java,v 1.4 2008/12/04 06:32:07 veiming Exp $
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
import com.sun.identity.sm.AttributeSchema;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceSchema;
import java.text.MessageFormat;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

/**
 * Sets attribute choice values to attribute schema.
 */
public class SetAttributeSchemaChoiceValues extends SchemaCommand {
    private static final String ARGUMENT_ADD = "add";

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
        String datafile = getStringOptionValue(IArgument.DATA_FILE);
        List choiceValues = rc.getOption(IArgument.CHOICE_VALUES);
        boolean toAdd = isOptionSet(ARGUMENT_ADD);

        if ((datafile == null) && (choiceValues == null)) {
            throw new CLIException(getResourceString("missing-choicevalues"),
                ExitCodes.INCORRECT_OPTION, rc.getSubCommand().getName());
        }

        Map attributeValues = AttributeValues.parse(
            getCommandManager(), datafile, choiceValues);

        ServiceSchema ss = getServiceSchema();
        IOutput outputWriter = getOutputWriter();
        String[] params = {serviceName, schemaType, subSchemaName,
            attributeName};

        if (toAdd) {
            writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                "ATTEMPT_ADD_ATTRIBUTE_SCHEMA_CHOICE_VALUES", params);
        } else {
            writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                "ATTEMPT_SET_ATTRIBUTE_SCHEMA_CHOICE_VALUES", params);
        }

        try {
            AttributeSchema attrSchema = ss.getAttributeSchema(
                attributeName);

            if (attrSchema == null) {
                String[] args = {serviceName, schemaType, subSchemaName,
                    attributeName, "attribute schema does not exist"};
                attributeSchemaNoExist(attributeName,
                    (toAdd) ? "FAILED_ADD_ATTRIBUTE_SCHEMA_CHOICE_VALUES" 
                    : "FAILED_SET_ATTRIBUTE_SCHEMA_CHOICE_VALUES", args);
            }

            if (toAdd) {
                addChoiceValues(attrSchema, attributeValues);
                writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                    "SUCCEED_ADD_ATTRIBUTE_SCHEMA_CHOICE_VALUES", params);
            } else {
                setChoiceValues(attrSchema, attributeValues);
                writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                    "SUCCEED_SET_ATTRIBUTE_SCHEMA_CHOICE_VALUES", params);
            }
            outputWriter.printlnMessage(MessageFormat.format(
                getResourceString(
                "attribute-schema-set-choice-value-succeed"),
                (Object[])params));
        } catch (SSOException e) {
            String[] args = {serviceName, schemaType, subSchemaName,
                attributeName, e.getMessage()};
            debugError("SetAttributeSchemaChoiceValues.handleRequest", e);
            writeLog(LogWriter.LOG_ERROR, Level.INFO,
                (toAdd) ? "FAILED_ADD_ATTRIBUTE_SCHEMA_CHOICE_VALUES" :
                    "FAILED_SET_ATTRIBUTE_SCHEMA_CHOICE_VALUES", args);
            throw new CLIException(e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (SMSException e) {
            String[] args = {serviceName, schemaType, subSchemaName,
                attributeName, e.getMessage()};
            debugError("SetAttributeSchemaChoiceValues.handleRequest", e);
            writeLog(LogWriter.LOG_ERROR, Level.INFO,
                (toAdd) ? "FAILED_ADD_ATTRIBUTE_SCHEMA_CHOICE_VALUES" :
                    "FAILED_SET_ATTRIBUTE_SCHEMA_CHOICE_VALUES", args);
            throw new CLIException(e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        }
    }

    private void addChoiceValues(
        AttributeSchema attrSchema,
        Map i18nKeyToValues
    ) throws CLIException, SMSException, SSOException
    {
        for (Iterator i = i18nKeyToValues.keySet().iterator(); i.hasNext(); ) {
            String i18nKey = (String)i.next();
            String value = (String)((Set)i18nKeyToValues.get(i18nKey)).
                iterator().next();
            attrSchema.addChoiceValue(value, i18nKey);
        }
    }

    private void setChoiceValues(
        AttributeSchema attrSchema,
        Map i18nKeyToValues
    ) throws CLIException, SMSException, SSOException
    {
        for (Iterator i = i18nKeyToValues.keySet().iterator(); i.hasNext(); ) {
            String i18nKey = (String)i.next();
            String value = (String)((Set)i18nKeyToValues.get(i18nKey)).
                iterator().next();
            attrSchema.removeChoiceValue(value);
            attrSchema.addChoiceValue(value, i18nKey);
        }
    }
}
