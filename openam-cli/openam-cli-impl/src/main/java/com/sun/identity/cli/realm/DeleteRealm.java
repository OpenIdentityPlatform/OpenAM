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
 * $Id: DeleteRealm.java,v 1.2 2008/06/25 05:42:15 qcheng Exp $
 *
 */

package com.sun.identity.cli.realm;


import com.iplanet.sso.SSOToken;
import com.sun.identity.cli.AuthenticatedCommand;
import com.sun.identity.cli.CLIException;
import com.sun.identity.cli.ExitCodes;
import com.sun.identity.cli.IArgument;
import com.sun.identity.cli.LogWriter;
import com.sun.identity.cli.RequestContext;
import com.sun.identity.sm.OrganizationConfigManager;
import com.sun.identity.sm.SMSException;
import java.util.logging.Level;

/**
 * Deletes realm command.
 */
public class DeleteRealm extends AuthenticatedCommand {
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
        SSOToken adminSSOToken = getAdminSSOToken();
        String realm = getStringOptionValue(IArgument.REALM_NAME);
        boolean recursive = isOptionSet(IArgument.RECURSIVE);
        String strRecursive = (recursive) ? "recursive" : "non recursive";

        String[] params = {realm, strRecursive};
        writeLog(LogWriter.LOG_ACCESS, Level.INFO, "ATTEMPT_DELETE_REALM",
            params);

        try {
            OrganizationConfigManager ocm = new OrganizationConfigManager(
                adminSSOToken, realm);
            ocm.deleteSubOrganization(null, recursive);
            getOutputWriter().printlnMessage(getResourceString(
                "delete-realm-succeed"));
            writeLog(LogWriter.LOG_ACCESS, Level.INFO, "SUCCEED_DELETE_REALM",
                params);
        } catch (SMSException e) {
            String[] args = {realm, strRecursive, e.getMessage()};
            debugError("DeleteRealm.handleRequest", e);
            writeLog(LogWriter.LOG_ERROR, Level.INFO, "FAILED_DELETE_REALM",
                args);
            throw new CLIException(e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        }
    }
}
