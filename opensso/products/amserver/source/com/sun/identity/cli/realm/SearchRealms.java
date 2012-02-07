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
 * $Id: SearchRealms.java,v 1.3 2008/06/25 05:42:16 qcheng Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.cli.realm;


import com.iplanet.sso.SSOToken;
import com.sun.identity.cli.AuthenticatedCommand;
import com.sun.identity.cli.CLIException;
import com.sun.identity.cli.ExitCodes;
import com.sun.identity.cli.IArgument;
import com.sun.identity.cli.IOutput;
import com.sun.identity.cli.LogWriter;
import com.sun.identity.cli.RequestContext;
import com.sun.identity.sm.OrganizationConfigManager;
import com.sun.identity.sm.SMSException;
import java.text.MessageFormat;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;

/**
 * Searches for realms command.
 */
public class SearchRealms extends AuthenticatedCommand {
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
        String pattern = getStringOptionValue(IArgument.FILTER);
        boolean recursive = isOptionSet(IArgument.RECURSIVE);
        String strRecursive = (recursive) ? "recursive" : "non recursive";

        if ((pattern == null) || (pattern.trim().length() == 0)) {
            pattern = "*";
        }

        String[] params = {realm, pattern, strRecursive};
        writeLog(LogWriter.LOG_ACCESS, Level.INFO,
            "ATTEMPT_SEARCH_REALM", params);

        try {
            OrganizationConfigManager ocm = new OrganizationConfigManager(
                adminSSOToken, realm);
            Set results = ocm.getSubOrganizationNames(pattern, recursive);

            IOutput outputWriter = getOutputWriter();
            if ((results != null) && !results.isEmpty()) {
                String template = getResourceString("search-realm-results");
                String[] arg = new String[1];

                for (Iterator i = results.iterator(); i.hasNext(); ) {
                    arg[0] = (String)i.next();
                    outputWriter.printlnMessage(
                        MessageFormat.format(template, (Object[])arg));
                }

                outputWriter.printlnMessage(getResourceString(
                    "search-realm-succeed"));
            } else {
                outputWriter.printlnMessage(getResourceString(
                    "search-realm-no-results"));
            }
            
            writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                "SUCCEED_SEARCH_REALM", params);
        } catch (SMSException e) {
            String[] args = {realm, strRecursive, e.getMessage()};
            debugError("SearchRealms.handleRequest", e);
            writeLog(LogWriter.LOG_ERROR, Level.INFO,
                "FAILED_SEARCH_REALM", args);
            throw new CLIException(e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        }
    }
}
