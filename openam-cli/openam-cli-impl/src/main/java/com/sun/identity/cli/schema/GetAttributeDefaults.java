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
 * $Id: GetAttributeDefaults.java,v 1.3 2008/06/25 05:42:17 qcheng Exp $
 *
 */

package com.sun.identity.cli.schema;


import com.sun.identity.cli.CLIException;
import com.sun.identity.cli.FormatUtils;
import com.sun.identity.cli.IArgument;
import com.sun.identity.cli.IOutput;
import com.sun.identity.cli.LogWriter;
import com.sun.identity.cli.RequestContext;
import com.sun.identity.sm.AttributeSchema;
import com.sun.identity.sm.ServiceSchema;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

/**
 * Gets default attribute values of schema.
 */
public class GetAttributeDefaults extends SchemaCommand {

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

        ServiceSchema ss = getServiceSchema();
        IOutput outputWriter = getOutputWriter();

        String[] params = {serviceName, schemaType, subSchemaName};
        writeLog(LogWriter.LOG_ACCESS, Level.INFO,
            "ATTEMPT_GET_SCHEMA_ATTR_DEFAULTS", params);
        Map attrValues = ss.getAttributeDefaults();
        retainValues(rc, attrValues);
        maskPasswordValues(ss, attrValues);

        if (!attrValues.isEmpty()) {
            outputWriter.printlnMessage(FormatUtils.printAttributeValues(
                getResourceString("schema-get-attribute-defaults-result"),
                attrValues));
            outputWriter.printlnMessage(getResourceString(
                "schema-get-attribute-defaults-succeed"));
        } else {
            outputWriter.printlnMessage(getResourceString(
                "schema-get-attribute-defaults-no-matching-attr"));
        }

        writeLog(LogWriter.LOG_ACCESS, Level.INFO,
            "SUCCEED_GET_SCHEMA_ATTR_DEFAULTS", params);
    }

    private void retainValues(RequestContext rc, Map attrValues) {
        List attrNames = rc.getOption(IArgument.ATTRIBUTE_NAMES);
        if ((attrNames != null) && !attrNames.isEmpty()) {
            Set attributeNames = new HashSet();
            attributeNames.addAll(attrNames);

            for (Iterator i = attrValues.keySet().iterator(); i.hasNext();){
                String name = (String)i.next();
                if (!attributeNames.contains(name)) {
                    i.remove();
                }
            }
        }
    }

    private void maskPasswordValues(ServiceSchema ss, Map attrValues) {
        for (Iterator i = attrValues.keySet().iterator(); i.hasNext();){
            String name = (String)i.next();
            Set values = (Set)attrValues.get(name);
            if (values != null) {
                AttributeSchema attrSchema = ss.getAttributeSchema(name);
                AttributeSchema.Syntax syntax = attrSchema.getSyntax();
                if (syntax == AttributeSchema.Syntax.PASSWORD) {
                    attrValues.put(name, maskPasswordField(values));
                }
            }
        }
    }

    private Set maskPasswordField(Set pwdValues) {
        int size = pwdValues.size();
        Set masked = new HashSet(size *2);
        for (int i = 0; i < size; i++) {
            masked.add("********");
        }
        return masked;
    }
}
