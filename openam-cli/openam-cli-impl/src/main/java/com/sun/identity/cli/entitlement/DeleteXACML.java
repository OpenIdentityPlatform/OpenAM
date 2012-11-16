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
 * $Id: DeleteXACML.java,v 1.1 2009/11/25 18:54:08 dillidorai Exp $
 */

package com.sun.identity.cli.entitlement;

import com.iplanet.sso.SSOToken;

import com.sun.identity.cli.AttributeValues;
import com.sun.identity.cli.AuthenticatedCommand;
import com.sun.identity.cli.CLIException;
import com.sun.identity.cli.ExitCodes;
import com.sun.identity.cli.IArgument;
import com.sun.identity.cli.IOutput;
import com.sun.identity.cli.LogWriter;
import com.sun.identity.cli.RequestContext;

import com.sun.identity.entitlement.EntitlementConfiguration;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.PrivilegeManager;
import com.sun.identity.entitlement.opensso.SubjectUtils;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;

import javax.security.auth.Subject;

/**
 * Deletes policies in a realm.
 */
public class DeleteXACML extends AuthenticatedCommand {
    static final String ARGUMENT_POLICY_NAMES = "policynames";
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
        Subject adminSubject = SubjectUtils.createSubject(adminSSOToken);
        String realm = getStringOptionValue(IArgument.REALM_NAME);

        // FIXME: change to use entitlementService.xacmlPrivilegEnabled()
        EntitlementConfiguration ec = EntitlementConfiguration.getInstance(
            adminSubject, "/");
        if(!ec.migratedToEntitlementService()) {
            String[] args = {realm, "ANY", 
                    "list-xacml not supported in  legacy policy mode"};
            debugError("DeleteXACML.handleRequest(): "
                    + "delete-xacml not supported in  legacy policy mode");
            writeLog(LogWriter.LOG_ERROR, Level.INFO,
                "FAILED_DELETE_POLICY_IN_REALM", 
                args);
            throw new CLIException(
                getResourceString( 
                    "delete-xacml-not-supported-in-legacy-policy-mode"), 
                ExitCodes.REQUEST_CANNOT_BE_PROCESSED,
                "delete-xacml");
        }

        List policyNames = (List)rc.getOption(ARGUMENT_POLICY_NAMES);
        String file = getStringOptionValue(IArgument.FILE);
        if (policyNames == null) {
            policyNames = new ArrayList();
        }
 
        if (file != null) {
            policyNames.addAll(AttributeValues.parseValues(file));
        }
 
        if (policyNames.isEmpty()) {
            throw new CLIException(getResourceString("missing-policy-names"),
                ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        }

        IOutput outputWriter = getOutputWriter();
        String currentPolicyName = null;

        try {
            PrivilegeManager pm = PrivilegeManager.getInstance(realm, adminSubject);
            String[] params = new String[2];
            params[0] = realm;

            for (Iterator i = policyNames.iterator(); i.hasNext(); ) {
                currentPolicyName = (String)i.next();
                params[1] = currentPolicyName;
                writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                    "ATTEMPT_DELETE_POLICY_IN_REALM", params);
                pm.removePrivilege(currentPolicyName);
                writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                    "SUCCEED_DELETE_POLICY_IN_REALM", params);
            }

            String[] arg = {realm};
            outputWriter.printlnMessage(MessageFormat.format(
                getResourceString("delete-policy-in-realm-succeed"), 
                    (Object[])arg));
        } catch (EntitlementException e) {
            String[] args = {realm, currentPolicyName, e.getMessage()};
            debugError("DeleteXACML.handleRequest", e);
            writeLog(LogWriter.LOG_ERROR, Level.INFO,
                "FAILED_DELETE_POLICY_IN_REALM", args);
            throw new CLIException(e ,ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        }
    }
}
