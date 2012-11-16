/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: ListApplications.java,v 1.2 2009/11/19 01:02:02 veiming Exp $
 */

package com.sun.identity.cli.entitlement;

import com.sun.identity.cli.CLIException;
import com.sun.identity.cli.ExitCodes;
import com.sun.identity.cli.IArgument;
import com.sun.identity.cli.IOutput;
import com.sun.identity.cli.LogWriter;
import com.sun.identity.cli.RequestContext;
import com.sun.identity.entitlement.ApplicationManager;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.log.Level;
import java.util.Set;

public class ListApplications extends ApplicationImpl {
    /**
     * Services a Commandline Request.
     *
     * @param rc Request Context.
     * @throws CLIException if the request cannot serviced.
     */
    @Override
    public void handleRequest(RequestContext rc)
        throws CLIException {
        super.handleRequest(rc);

        String realm = getStringOptionValue(IArgument.REALM_NAME);
        String[] params = {realm};
        writeLog(LogWriter.LOG_ACCESS, Level.INFO,
            "ATTEMPT_LIST_APPLICATIONS", params);
        try {
            Set<String> names = ApplicationManager.getApplicationNames(
                getAdminSubject(), realm);
            IOutput writer = getOutputWriter();

            if ((names == null) || names.isEmpty()) {
                writer.printlnMessage(getResourceString(
                    "list-applications-no-entries"));
            } else {
                for (String n : names) {
                    writer.printlnMessage(n);
                }
            }
            writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                "SUCCEEDED_LIST_APPLICATIONS", params);
        } catch (EntitlementException ex) {
            String[] paramsEx = {realm, ex.getMessage()};
            writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                "FAILED_LIST_APPLICATIONS", paramsEx);
            throw new CLIException(ex, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        }
    }
}
