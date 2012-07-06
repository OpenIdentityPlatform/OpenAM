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
 * $Id: ListApplicationTypes.java,v 1.1 2009/08/19 05:40:31 veiming Exp $
 */

package com.sun.identity.cli.entitlement;

import com.sun.identity.cli.CLIException;
import com.sun.identity.cli.IOutput;
import com.sun.identity.cli.LogWriter;
import com.sun.identity.cli.RequestContext;
import com.sun.identity.entitlement.ApplicationTypeManager;
import com.sun.identity.log.Level;
import java.util.Set;

public class ListApplicationTypes extends ApplicationImpl {
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

        writeLog(LogWriter.LOG_ACCESS, Level.INFO,
            "ATTEMPT_LIST_APPLICATION_TYPES", null);
        Set<String> names = ApplicationTypeManager.getApplicationTypeNames(
            getAdminSubject());
        IOutput writer = getOutputWriter();

        if ((names == null) || names.isEmpty()) {
            writer.printlnMessage(getResourceString(
                "list-applications-type-no-entries"));
        } else {
            for (String n : names) {
                writer.printlnMessage(n);
            }
        }
        writeLog(LogWriter.LOG_ACCESS, Level.INFO,
            "SUCCEEDED_LIST_APPLICATION_TYPES", null);
    }
}
