/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010 ForgeRock AS All Rights Reserved
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
 * 
 *
 */

package com.sun.identity.cli.realm;


import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.cli.AuthenticatedCommand;
import com.sun.identity.cli.CLIException;
import com.sun.identity.cli.CommandManager;
import com.sun.identity.cli.ExitCodes;
import com.sun.identity.cli.IArgument;
import com.sun.identity.cli.IOutput;
import com.sun.identity.cli.LogWriter;
import com.sun.identity.cli.RequestContext;
import com.sun.identity.policy.NameNotFoundException;
import com.sun.identity.policy.PolicyException;
import com.sun.identity.policy.PolicyManager;
import com.sun.identity.policy.PolicyUtils;
import java.text.MessageFormat;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.logging.Level;

/**
 * Updates policy in a realm.
 */
public class RealmUpdatePolicy extends AuthenticatedCommand {

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
        String datafile = getStringOptionValue(IArgument.XML_FILE);
        IOutput outputWriter = getOutputWriter();

        
        CommandManager mgr = getCommandManager();
        String url = mgr.getWebEnabledURL();
        String[] params = {realm};
        writeLog(LogWriter.LOG_ACCESS, Level.INFO,
            "ATTEMPT_UPDATE_POLICY_IN_REALM", params);

        try {
            // Get the policy object
            PolicyManager pm = new PolicyManager(adminSSOToken, realm);

            // Read the input and try to update the policy
            if ((url != null) && (url.length() > 0)) {
                ByteArrayInputStream bis = new ByteArrayInputStream(
                    datafile.getBytes());
                PolicyUtils.createOrReplacePolicies(pm, bis, true);
            } else {
                FileInputStream fis = new FileInputStream(datafile);
                PolicyUtils.createOrReplacePolicies(pm, fis, true);
            }

            writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                "SUCCEED_UPDATE_POLICY_IN_REALM", params);
            outputWriter.printlnMessage(MessageFormat.format(
                getResourceString("update-policy-in-realm-succeed"),
                (Object[])params));
        } catch (NameNotFoundException e) {
            String[] args = {realm, e.getMessage()};
            debugError("RealmUpdatePolicy.handleRequest", e);

            outputWriter.printlnMessage(MessageFormat.format(
                getResourceString("update-policy-in-realm-name-not-found"),
                (Object[])params));
            
            writeLog(LogWriter.LOG_ERROR, Level.INFO,
                "FAILED_UPDATE_POLICY_IN_REALM", args);
            throw new CLIException(e ,ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (PolicyException e) {
            String[] args = {realm, e.getMessage()};
            debugError("RealmUpdatePolicy.handleRequest", e);
            writeLog(LogWriter.LOG_ERROR, Level.INFO,
                "FAILED_UPDATE_POLICY_IN_REALM", args);
            throw new CLIException(e ,ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (FileNotFoundException e) {
            String[] args = {realm, e.getMessage()};
            debugError("RealmUpdatePolicy.handleRequest", e);
            writeLog(LogWriter.LOG_ERROR, Level.INFO,
                "FAILED_UPDATE_POLICY_IN_REALM", args);
            throw new CLIException(e ,ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (SSOException e) {
            String[] args = {realm, e.getMessage()};
            debugError("RealmUpdatePolicy.handleRequest", e);
            writeLog(LogWriter.LOG_ERROR, Level.INFO,
                "FAILED_UPDATE_POLICY_IN_REALM", args);
            throw new CLIException(e ,ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        }
    }
    
}
