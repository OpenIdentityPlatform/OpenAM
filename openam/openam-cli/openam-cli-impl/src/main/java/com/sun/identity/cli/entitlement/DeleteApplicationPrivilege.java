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
 * $Id: DeleteApplicationPrivilege.java,v 1.1 2009/11/10 19:01:03 veiming Exp $
 */

package com.sun.identity.cli.entitlement;

import com.sun.identity.cli.CLIException;
import com.sun.identity.cli.ExitCodes;
import com.sun.identity.cli.IArgument;
import com.sun.identity.cli.LogWriter;
import com.sun.identity.cli.RequestContext;
import com.sun.identity.entitlement.ApplicationPrivilegeManager;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.opensso.SubjectUtils;
import java.util.List;
import java.util.logging.Level;
import javax.security.auth.Subject;

/**
 *
 * @author dennis
 */
public class DeleteApplicationPrivilege extends ApplicationPrivilegeBase {
    public static final String PARAM_NAMES = "names";

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
        List<String> names = rc.getOption(PARAM_NAMES);

        String[] params = new String[2];
        params[0] = realm;

        Subject userSubject = SubjectUtils.createSubject(
            getAdminSSOToken());
        ApplicationPrivilegeManager apm =
            ApplicationPrivilegeManager.getInstance(realm, userSubject);
        String curAppName = null;

        try {
            for (String name : names) {
                curAppName = name;
                params[1] = name;
                writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                    "ATTEMPT_DELETE_APPLICATION_PRIVILEGE", params);
                apm.removePrivilege(name);
                writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                    "SUCCEEDED_DELETE_APPLICATION_PRIVILEGE", params);
            }

            String msg = (names.size() > 1) ?
                getResourceString("delete-application-privileges-succeeded") :
                getResourceString("delete-application-privilege-succeeded");
            getOutputWriter().printlnMessage(msg);
        } catch (EntitlementException ex) {
            String[] paramExs = {realm, curAppName, ex.getMessage()};
            writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                "FAILED_DELETE_APPLICATION_PRIVILEGE", paramExs);
            throw new CLIException(ex, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        }

    }
}
