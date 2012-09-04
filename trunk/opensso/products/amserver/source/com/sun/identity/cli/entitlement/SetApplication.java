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
 * $Id: SetApplication.java,v 1.2 2009/11/19 01:02:02 veiming Exp $
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
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.log.Level;
import java.text.MessageFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.security.auth.Subject;

public class SetApplication extends ApplicationImpl {
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
        String datafile = getStringOptionValue(IArgument.DATA_FILE);
        List attrValues = rc.getOption(IArgument.ATTRIBUTE_VALUES);

        if ((datafile == null) && (attrValues == null)) {
            throw new CLIException(
                getResourceString("missing-attributevalues"),
                ExitCodes.INCORRECT_OPTION, rc.getSubCommand().getName());
        }
        Map<String, Set<String>> attributeValues = AttributeValues.parse(
            getCommandManager(), datafile, attrValues);
        
        String[] params = {realm, appName};
        writeLog(LogWriter.LOG_ACCESS, Level.INFO,
            "ATTEMPT_SET_APPLICATION", params);

        Subject adminSubject = getAdminSubject();
        try {
            Application appl = ApplicationManager.getApplication(adminSubject,
                realm, appName);
            Object[] param = {appName};

            if (appl == null) {
                throw new CLIException(MessageFormat.format(getResourceString(
                    "set-application-not-found"), param),
                    ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
            }

            setApplicationAttributes(appl, attributeValues, false);
            ApplicationManager.saveApplication(getAdminSubject(), realm, appl);
            getOutputWriter().printlnMessage(
                MessageFormat.format(getResourceString(
                "set-application-modified"), param));
            writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                "SUCCEEDED_SET_APPLICATION", params);
        } catch (EntitlementException e) {
            String[] paramExs = {realm, appName, e.getMessage()};
            writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                "FAILED_SET_APPLICATION", paramExs);
            throw new CLIException(e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (CLIException e) {
            String[] paramExs = {realm, appName, e.getMessage()};
            writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                "FAILED_SET_APPLICATION", paramExs);
            throw e;
        }
    }
}
