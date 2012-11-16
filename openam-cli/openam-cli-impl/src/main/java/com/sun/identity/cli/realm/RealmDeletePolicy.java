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
 * $Id: RealmDeletePolicy.java,v 1.4 2008/10/31 16:18:39 veiming Exp $
 */

package com.sun.identity.cli.realm;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.cli.AttributeValues;
import com.sun.identity.cli.AuthenticatedCommand;
import com.sun.identity.cli.CLIException;
import com.sun.identity.cli.ExitCodes;
import com.sun.identity.cli.IArgument;
import com.sun.identity.cli.IOutput;
import com.sun.identity.cli.LogWriter;
import com.sun.identity.cli.RequestContext;
import com.sun.identity.policy.PolicyException;
import com.sun.identity.policy.PolicyManager;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;

/**
 * Deletes policies in a realm.
 */
public class RealmDeletePolicy extends AuthenticatedCommand {
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
        String realm = getStringOptionValue(IArgument.REALM_NAME);
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
            PolicyManager pm = new PolicyManager(adminSSOToken, realm);
            String[] params = new String[2];
            params[0] = realm;

            for (Iterator i = policyNames.iterator(); i.hasNext(); ) {
                currentPolicyName = (String)i.next();
                params[1] = currentPolicyName;
                writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                    "ATTEMPT_DELETE_POLICY_IN_REALM", params);
                pm.removePolicy(currentPolicyName);
                writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                    "SUCCEED_DELETE_POLICY_IN_REALM", params);
            }

            String[] arg = {realm};
            outputWriter.printlnMessage(MessageFormat.format(
                getResourceString("delete-policy-in-realm-succeed"), 
                    (Object[])arg));
        } catch (PolicyException e) {
            String[] args = {realm, currentPolicyName, e.getMessage()};
            debugError("RealmDeletePolicy.handleRequest", e);
            writeLog(LogWriter.LOG_ERROR, Level.INFO,
                "FAILED_DELETE_POLICY_IN_REALM", args);
            throw new CLIException(e ,ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (SSOException e) {
            String[] args = {realm, currentPolicyName, e.getMessage()};
            debugError("RealmDeletePolicy.handleRequest", e);
            writeLog(LogWriter.LOG_ERROR, Level.INFO,
                "FAILED_DELETE_POLICY_IN_REALM", args);
            throw new CLIException(e ,ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        }
    }
}
