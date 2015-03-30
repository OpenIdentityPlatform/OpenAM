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
 * $Id: CreateApplication.java,v 1.1 2009/08/19 05:40:31 veiming Exp $
 */

package com.sun.identity.cli.entitlement;

import com.sun.identity.cli.AttributeValues;
import com.sun.identity.cli.CLIException;
import com.sun.identity.cli.ExitCodes;
import com.sun.identity.cli.IArgument;
import com.sun.identity.cli.LogWriter;
import com.sun.identity.cli.RequestContext;
import com.sun.identity.entitlement.Application;
import com.sun.identity.entitlement.ApplicationManager;
import com.sun.identity.entitlement.ApplicationType;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.log.Level;
import java.text.MessageFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CreateApplication extends ApplicationImpl {
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
        String appName = getStringOptionValue(PARAM_APPL_NAME);
        String appTypeName = getStringOptionValue(PARAM_APPL_TYPE_NAME);
        String datafile = getStringOptionValue(IArgument.DATA_FILE);
        List attrValues = rc.getOption(IArgument.ATTRIBUTE_VALUES);

        if ((datafile == null) && (attrValues == null)) {
            throw new CLIException(
                getResourceString("missing-attributevalues"),
                ExitCodes.INCORRECT_OPTION, rc.getSubCommand().getName());
        }
        Map<String, Set<String>> attributeValues = AttributeValues.parse(
            getCommandManager(), datafile, attrValues);

        ApplicationType applicationType = getApplicationType(appTypeName);
        String[] params = {realm, appName};
        writeLog(LogWriter.LOG_ACCESS, Level.INFO,
            "ATTEMPT_CREATE_APPLICATION", params);
        try {
            Application appl = ApplicationManager.newApplication(realm,
                appName, applicationType);
            setApplicationAttributes(appl, attributeValues,
                true);
            ApplicationManager.saveApplication(getAdminSubject(), realm, appl);
            String[] param = {appName};
            getOutputWriter().printlnMessage(
                MessageFormat.format(getResourceString(
                "create-application-succeeded"), (Object[])param));
            writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                "SUCCEEDED_CREATE_APPLICATION", params);
        } catch (EntitlementException e) {
            String[] paramExs = {realm, appName, e.getMessage()};
            writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                "FAILED_CREATE_APPLICATION", paramExs);
            throw new CLIException(e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (CLIException e) {
            String[] paramExs = {realm, appName, e.getMessage()};
            writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                "FAILED_CREATE_APPLICATION", paramExs);
            throw e;
        }
    }
}
