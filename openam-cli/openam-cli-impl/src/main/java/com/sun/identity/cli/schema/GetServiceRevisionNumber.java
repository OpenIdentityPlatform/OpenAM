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
 * $Id: GetServiceRevisionNumber.java,v 1.2 2008/06/25 05:42:17 qcheng Exp $
 *
 */

package com.sun.identity.cli.schema;


import com.sun.identity.cli.CLIException;
import com.sun.identity.cli.IArgument;
import com.sun.identity.cli.IOutput;
import com.sun.identity.cli.LogWriter;
import com.sun.identity.cli.RequestContext;
import com.sun.identity.sm.ServiceSchemaManager;
import java.text.MessageFormat;
import java.util.logging.Level;

/**
 * Gets service revision number.
 */
public class GetServiceRevisionNumber extends SchemaCommand {
    /**
     * Handles request.
     *
     * @param rc Request Context.
     * @throws CLIException if request cannot be processed.
     */
    public void handleRequest(RequestContext rc) 
        throws CLIException {
        super.handleRequest(rc);
        ldapLogin();

        String serviceName = getStringOptionValue(IArgument.SERVICE_NAME);
        ServiceSchemaManager ssm = getServiceSchemaManager();
        IOutput outputWriter = getOutputWriter();

        String[] params = {serviceName};
        writeLog(LogWriter.LOG_ACCESS, Level.INFO,
            "ATTEMPT_GET_SERVICE_REVISION_NUMBER", params);
        String revisionNumber = Integer.toString(ssm.getRevisionNumber());
        String[] args = {serviceName, revisionNumber};
        outputWriter.printlnMessage(MessageFormat.format(getResourceString(
            "service-schema-get-revision-number-succeed"), (Object[])args));
        writeLog(LogWriter.LOG_ACCESS, Level.INFO,
            "SUCCEED_GET_SERVICE_REVISION_NUMBER", params);
    }
}
