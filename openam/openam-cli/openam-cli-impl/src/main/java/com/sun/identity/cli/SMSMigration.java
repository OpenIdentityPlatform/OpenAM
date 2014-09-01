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
 * $Id: SMSMigration.java,v 1.2 2008/06/25 05:42:09 qcheng Exp $
 *
 */

package com.sun.identity.cli;


import com.iplanet.sso.SSOToken;
import com.sun.identity.sm.SMSMigration70;
import java.util.logging.Level;

/**
 * Migrates SMS data.
 */
public class SMSMigration extends AuthenticatedCommand {
    private static final String ARGUMENT_ENTRY_DN = "entrydn";
    
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

        String entryDN = getStringOptionValue(ARGUMENT_ENTRY_DN);
        String[] params = {entryDN};

        writeLog(LogWriter.LOG_ACCESS, Level.INFO,
            "ATTEMPT_MIGRATION_ENTRY", params);
        SMSMigration70.migrate63To70(adminSSOToken, entryDN);
        getOutputWriter().printlnMessage(getResourceString(
            "sms-migration-succeed"));
            
        writeLog(LogWriter.LOG_ACCESS, Level.INFO,
            "SUCCEED_MIGRATION_ENTRY", params);
    }
}
