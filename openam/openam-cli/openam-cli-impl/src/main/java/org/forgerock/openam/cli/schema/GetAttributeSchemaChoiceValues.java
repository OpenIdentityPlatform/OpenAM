/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011 ForgeRock AS. All Rights Reserved
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

import com.sun.identity.cli.CLIException;
import com.sun.identity.cli.FormatUtils;
import com.sun.identity.cli.IArgument;
import com.sun.identity.cli.IOutput;
import com.sun.identity.cli.LogWriter;
import com.sun.identity.cli.RequestContext;
import com.sun.identity.cli.schema.SchemaCommand;
import com.sun.identity.sm.AttributeSchema;
import com.sun.identity.sm.ServiceSchema;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

/**
 *
 * @author Peter Major
 */
public class GetAttributeSchemaChoiceValues extends SchemaCommand {

    @Override
    public void handleRequest(RequestContext rc) throws CLIException {
        super.handleRequest(rc);
        ldapLogin();

        String schemaType = getStringOptionValue(IArgument.SCHEMA_TYPE);
        String serviceName = getStringOptionValue(IArgument.SERVICE_NAME);
        String subSchemaName = getStringOptionValue(IArgument.SUBSCHEMA_NAME);
        String attributeName = getStringOptionValue(IArgument.ATTRIBUTE_NAME);

        ServiceSchema ss = getServiceSchema();
        IOutput outputWriter = getOutputWriter();
        String[] params = {serviceName, schemaType, subSchemaName,
            attributeName};
        writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                "ATTEMPT_GET_ATTRIBUTE_SCHEMA_CHOICE_VALUES", params);

        AttributeSchema attrSchema = ss.getAttributeSchema(attributeName);

        if (attrSchema == null) {
            String[] args = {serviceName, schemaType, subSchemaName,
                attributeName, "attribute schema does not exist"};
            attributeSchemaNoExist(attributeName,
                    "FAILED_GET_ATTRIBUTE_SCHEMA_CHOICE_VALUES", args);
        }

        writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                "SUCCEED_GET_ATTRIBUTE_SCHEMA_CHOICE_VALUES", params);
        getOutputWriter().printlnMessage(
                FormatUtils.formatMap(
                getResourceString("attribute-schema-i18nkey"),
                getResourceString("attribute-schema-choice-value"),
                getChoiceValues(attrSchema)));
    }

    private Map<String, String> getChoiceValues(AttributeSchema attrSchema) {
        Map<String, String> ret = new HashMap<String, String>();
        for (String value : attrSchema.getChoiceValues()) {
            ret.put(attrSchema.getChoiceValueI18NKey(value), value);
        }
        return ret;
    }
}
