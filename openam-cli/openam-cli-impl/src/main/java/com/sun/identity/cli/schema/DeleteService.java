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
 * $Id: DeleteService.java,v 1.6 2008/09/03 22:04:43 veiming Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.cli.schema;


import com.iplanet.am.sdk.AMException;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.cli.AuthenticatedCommand;
import com.sun.identity.cli.CLIException;
import com.sun.identity.cli.Debugger;
import com.sun.identity.cli.ExitCodes;
import com.sun.identity.cli.IArgument;
import com.sun.identity.cli.IOutput;
import com.sun.identity.cli.LogWriter;
import com.sun.identity.cli.RequestContext;
import com.sun.identity.policy.PolicyUtils;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceConfigManager;
import com.sun.identity.sm.ServiceManager;
import com.sun.identity.sm.ServiceSchemaManager;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

/**
 * Deletes a service schema.
 */
public class DeleteService extends AuthenticatedCommand {
    private static final String ARGUMENT_DELETE_POLICY_RULE =
        "deletepolicyrule";

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

        boolean continueFlag = isOptionSet(IArgument.CONTINUE);
        IOutput outputWriter = getOutputWriter();        
        List serviceNames = (List)rc.getOption(IArgument.SERVICE_NAME);
        ServiceManager ssm = null;
        boolean bError = false;
        
        try {
            ssm = new ServiceManager(adminSSOToken);
        } catch (SMSException e) {
            throw new CLIException(e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (SSOException e) {
            throw new CLIException(e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        }
        
        for (Iterator i = serviceNames.iterator(); i.hasNext(); ) {
            String name = (String)i.next();
            String[] param = {name};

            try {
                writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                    "ATTEMPT_DELETE_SERVICE", param);
                deleteService(rc, ssm, name, adminSSOToken);
                outputWriter.printlnMessage(
                    getResourceString("service-deleted"));
                writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                    "SUCCEED_DELETE_SERVICE", param);
            } catch (CLIException e) {
                bError = true;
                if (continueFlag) {
                    outputWriter.printlnError(
                        getResourceString("service-deletion-failed") +
                        e.getMessage());
                    if (isVerbose()) {
                        outputWriter.printlnError(Debugger.getStackTrace(e));
                    }
                } else {
                    throw e;
                }
            }
        }

        if (bError) {
            throw new CLIException(
                getResourceString("one-or-more-services-not-deleted"),
                ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        }
    }

    private void deleteService(
        RequestContext rc,
        ServiceManager ssm,
        String serviceName, 
        SSOToken adminSSOToken
    ) throws CLIException {
        try {
            ServiceConfigManager scm = new ServiceConfigManager(
                serviceName, adminSSOToken);

            if (scm.getGlobalConfig(null) != null) {
                scm.removeGlobalConfiguration(null);
            }

            Set versions = ssm.getServiceVersions(serviceName);
            for (Iterator iter = versions.iterator(); iter.hasNext(); ) {
                ssm.removeService(serviceName, (String)iter.next());
            }
            deletePolicyRule(rc, serviceName, adminSSOToken);
        } catch (SSOException e) {
            String[] args = {serviceName, e.getMessage()};
            writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                "FAILED_DELETE_SERVICE", args);
            throw new CLIException(e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (SMSException e) {
            String[] args = {serviceName, e.getMessage()};
            writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                "FAILED_DELETE_SERVICE", args);
            throw new CLIException(e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        }
    }

    private void deletePolicyRule(
        RequestContext rc,
        String serviceName,
        SSOToken adminSSOToken
    ) throws CLIException, SMSException, SSOException {
        IOutput outputWriter = getOutputWriter();
        List listDelPolicyRule = (List)rc.getOption(
            ARGUMENT_DELETE_POLICY_RULE);

        if (listDelPolicyRule != null) {
            ServiceSchemaManager ssm = new ServiceSchemaManager(
                serviceName, adminSSOToken);

            if (ssm == null) {
                if (isVerbose()) {
                    outputWriter.printlnMessage(getResourceString(
                        "delete-service-no-policy-rules"));
                }
            } else {
                if (ssm.getPolicySchema() == null) {
                    if (isVerbose()) {
                        outputWriter.printlnMessage(getResourceString(
                            "delete-service-no-policy-schema"));
                    }
                } else {
                    if (isVerbose()) {
                        outputWriter.printlnMessage(getResourceString(
                            "delete-service-delete-policy-rules"));
                    }
                    processCleanPolicies(serviceName, adminSSOToken);
                }
            }
        }
    }

    private void processCleanPolicies(
        String serviceName,
        SSOToken adminSSOToken
    ) throws CLIException {
        try {
            PolicyUtils.removePolicyRules(adminSSOToken, serviceName);
        } catch (SSOException e) {
            throw new CLIException(e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (AMException e) {
            throw new CLIException(e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        }
    }
}
