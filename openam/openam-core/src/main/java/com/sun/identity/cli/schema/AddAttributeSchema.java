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
 * $Id: AddAttributeSchema.java,v 1.4 2008/06/25 05:42:17 qcheng Exp $
 *
 */

package com.sun.identity.cli.schema;


import com.iplanet.sso.SSOException;
import com.sun.identity.cli.CLIConstants;
import com.sun.identity.cli.CLIException;
import com.sun.identity.cli.CommandManager;
import com.sun.identity.cli.Debugger;
import com.sun.identity.cli.ExitCodes;
import com.sun.identity.cli.IArgument;
import com.sun.identity.cli.IOutput;
import com.sun.identity.cli.LogWriter;
import com.sun.identity.cli.RequestContext;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceSchema;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;

/**
 * Adds attribute schema command.
 */
public class AddAttributeSchema extends SchemaCommand {
    static final String ARGUMENT_SCHEMA_FILES = "attributeschemafile";

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

        List listContinues = (List)rc.getOption(IArgument.CONTINUE);
        boolean continueFlag = (listContinues != null);
        IOutput outputWriter = getOutputWriter();        
        List fileNames = (List)rc.getOption(ARGUMENT_SCHEMA_FILES);
        String serviceName = getStringOptionValue(IArgument.SERVICE_NAME);
        String schemaType = getStringOptionValue(IArgument.SCHEMA_TYPE);
        ServiceSchema ss = getServiceSchema();
        
        CommandManager mgr = getCommandManager();
        String url = mgr.getWebEnabledURL();
        if ((url != null) && (url.length() > 0)) {
            String[] param = {CLIConstants.WEB_INPUT};
            writeLog(LogWriter.LOG_ACCESS, Level.INFO, 
                "ATTEMPT_ADD_ATTRIBUTE_SCHEMA", param);
            addAttributeSchemaXML(ss, serviceName, schemaType,
                (String)fileNames.iterator().next());
            outputWriter.printlnMessage(getResourceString(
                "attribute-schema-added"));
            writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                "SUCCEED_ADD_ATTRIBUTE_SCHEMA", param);
        } else {
            for (Iterator i = fileNames.iterator(); i.hasNext(); ) {
                String name = (String)i.next();
                String[] param = {serviceName, schemaType, name};

                try {
                    writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                        "ATTEMPT_ADD_ATTRIBUTE_SCHEMA", param);
                    addAttributeSchema(ss, serviceName, schemaType, name);
                    outputWriter.printlnMessage(
                        getResourceString("attribute-schema-added"));
                    writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                        "SUCCEED_ADD_ATTRIBUTE_SCHEMA", param);
                } catch (CLIException e) {
                    if (continueFlag) {
                        outputWriter.printlnError(
                            getResourceString("add-attribute-schema-failed") +
                            e.getMessage());
                        if (isVerbose()) {
                            outputWriter.printlnError(Debugger.getStackTrace(e));
                        }
                    } else {
                        throw e;
                    }
                }
            }
        }
    }

    private void addAttributeSchemaXML(
        ServiceSchema ss,
        String serviceName,
        String schemaType,
        String xml
    ) throws CLIException {
        ByteArrayInputStream bis = null;
        try {
            bis = new ByteArrayInputStream(xml.getBytes());
            ss.addAttributeSchema(bis);
        } catch (SSOException e) {
            String[] args = {CLIConstants.WEB_INPUT, schemaType, xml, 
                e.getMessage()};
            writeLog(LogWriter.LOG_ACCESS, Level.INFO, 
                "FAILED_ADD_ATTRIBUTE_SCHEMA", args);
            throw new CLIException(e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (SMSException e) {
            String[] args = {CLIConstants.WEB_INPUT, schemaType, xml, 
                e.getMessage()};
            writeLog(LogWriter.LOG_ACCESS, Level.INFO, 
                "FAILED_ADD_ATTRIBUTE_SCHEMA", args);
            throw new CLIException(e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } finally {
            if (bis != null) {
                try {
                    bis.close();
                } catch (IOException ie) {
                    //ignore if file input stream cannot be closed.
                }
            }
        }
    }

    private void addAttributeSchema(
        ServiceSchema ss,
        String serviceName, 
        String schemaType,
        String fileName
    ) throws CLIException {
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(fileName);
            ss.addAttributeSchema(fis);
        } catch (IOException e) {
            String[] args = {serviceName, schemaType, fileName, e.getMessage()};
            writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                "FAILED_ADD_ATTRIBUTE_SCHEMA", args);
            throw new CLIException(e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (SMSException e) {
            String[] args = {serviceName, schemaType, fileName, e.getMessage()};
            writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                "FAILED_ADD_ATTRIBUTE_SCHEMA", args);
            throw new CLIException(e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (SSOException e) {
            String[] args = {serviceName, schemaType, fileName, e.getMessage()};
            writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                "FAILED_ADD_ATTRIBUTE_SCHEMA", args);
            throw new CLIException(e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException ie) {
                    //igore if file input stream cannot be closed.
                }
            }
        }
    }
}
