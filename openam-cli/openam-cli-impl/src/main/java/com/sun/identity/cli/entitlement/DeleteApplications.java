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
 * $Id: DeleteApplications.java,v 1.1 2009/08/19 05:40:31 veiming Exp $
 *
 * Portions Copyrighted 2016 ForgeRock AS.
 * Portions Copyrighted 2025 3A Systems LLC.
 */

package com.sun.identity.cli.entitlement;

import com.sun.identity.cli.CLIException;
import com.sun.identity.cli.ExitCodes;
import com.sun.identity.cli.IArgument;
import com.sun.identity.cli.IOutput;
import com.sun.identity.cli.LogWriter;
import com.sun.identity.cli.RequestContext;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.log.Level;
import java.text.MessageFormat;
import java.util.List;

import jakarta.inject.Inject;
import javax.security.auth.Subject;

import org.forgerock.openam.entitlement.service.ApplicationService;
import org.forgerock.openam.entitlement.service.ApplicationServiceFactory;

public class DeleteApplications extends ApplicationImpl {

    /**
     * Create a new instance.
     *
     * @param applicationServiceFactory The {@link ApplicationServiceFactory}.
     */
    @Inject
    public DeleteApplications(ApplicationServiceFactory applicationServiceFactory) {
        super(applicationServiceFactory);
    }

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
        List<String> appNames = (List)rc.getOption(PARAM_APPL_NAMES);
        String[] param = {realm};
        writeLog(LogWriter.LOG_ACCESS, Level.INFO,
            "ATTEMPT_DELETE_APPLICATIONS", param);

        Subject adminSubject = getAdminSubject();
        try {
            ApplicationService applicationService = applicationServiceFactory.create(adminSubject, "/");
            for (String a : appNames) {
                applicationService.deleteApplication(a);
            }
            IOutput writer = getOutputWriter();
            writer.printlnMessage(MessageFormat.format(getResourceString(
                "delete-applications-succeeded"), (Object[])param));
            writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                "SUCCEEDED_DELETE_APPLICATIONS", param);
        } catch (EntitlementException e) {
            String[] params = {realm, e.getMessage()};
            writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                "FAILED_DELETE_APPLICATIONS", params);
            throw new CLIException(e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        }
    }
}
