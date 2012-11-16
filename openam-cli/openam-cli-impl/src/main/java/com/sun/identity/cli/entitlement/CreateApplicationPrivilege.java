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
 * $Id: CreateApplicationPrivilege.java,v 1.2 2009/11/19 01:02:02 veiming Exp $
 */

package com.sun.identity.cli.entitlement;

import com.sun.identity.cli.CLIException;
import com.sun.identity.cli.ExitCodes;
import com.sun.identity.cli.IArgument;
import com.sun.identity.cli.LogWriter;
import com.sun.identity.cli.RequestContext;
import com.sun.identity.entitlement.ApplicationPrivilege;
import com.sun.identity.entitlement.ApplicationPrivilegeManager;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.SubjectImplementation;
import com.sun.identity.entitlement.opensso.SubjectUtils;
import java.text.MessageFormat;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import javax.security.auth.Subject;

/**
 *
 * @author dennis
 */
public class CreateApplicationPrivilege extends ApplicationPrivilegeBase {

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
        String name = getStringOptionValue(PARAM_NAME);

        String[] params = {realm, name};
        writeLog(LogWriter.LOG_ACCESS, Level.INFO,
            "ATTEMPT_CREATE_APPLICATION_PRIVILEGE", params);
        
        String description = getStringOptionValue(PARAM_DESCRIPTION);
        ApplicationPrivilege.PossibleAction actions = getActions();
        Set<SubjectImplementation> subjects = getSubjects(rc);

        try {
            Map<String, Set<String>> mapAppToResources =
                getApplicationResourcesMap(rc, realm);
            Subject userSubject = SubjectUtils.createSubject(
                getAdminSSOToken());
            ApplicationPrivilegeManager apm =
                ApplicationPrivilegeManager.getInstance(realm, userSubject);
            ApplicationPrivilege appPrivilege = new ApplicationPrivilege(name);
            appPrivilege.setDescription(description);
            appPrivilege.setActionValues(actions);
            appPrivilege.setApplicationResources(mapAppToResources);
            appPrivilege.setSubject(subjects);
            apm.addPrivilege(appPrivilege);

            Object[] msgParam = {name};
            getOutputWriter().printlnMessage(MessageFormat.format(
                getResourceString("create-application-privilege-succeeded"),
                msgParam));
            writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                "SUCCEEDED_CREATE_APPLICATION_PRIVILEGE", params);
        } catch (EntitlementException ex) {
            String[] paramExs = {realm, name, ex.getMessage()};
            writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                "FAILED_CREATE_APPLICATION_PRIVILEGE", paramExs);
            throw new CLIException(ex, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (CLIException ex) {
            String[] paramExs = {realm, name, ex.getMessage()};
            writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                "FAILED_CREATE_APPLICATION_PRIVILEGE", paramExs);
            throw ex;
        }
    }
}
