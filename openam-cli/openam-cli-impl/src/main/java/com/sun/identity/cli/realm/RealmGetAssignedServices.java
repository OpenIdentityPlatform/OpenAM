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
 * $Id: RealmGetAssignedServices.java,v 1.2 2008/06/25 05:42:16 qcheng Exp $
 *
 */

package com.sun.identity.cli.realm;


import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.cli.AuthenticatedCommand;
import com.sun.identity.cli.CLIException;
import com.sun.identity.cli.ExitCodes;
import com.sun.identity.cli.FormatUtils;
import com.sun.identity.cli.IArgument;
import com.sun.identity.cli.IOutput;
import com.sun.identity.cli.LogWriter;
import com.sun.identity.cli.RequestContext;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.AMIdentityRepository;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.sm.OrganizationConfigManager;
import com.sun.identity.sm.SMSException;
import java.util.Set;
import java.util.logging.Level;

/**
 * Gets assigned service of realm command.
 */
public class RealmGetAssignedServices extends AuthenticatedCommand {
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
        boolean incMandatory = isOptionSet(IArgument.MANDATORY);
        String strMandatory = (incMandatory) ? "include mandatory" :
            "exclude mandatory";

        String[] params = {realm, strMandatory};
        writeLog(LogWriter.LOG_ACCESS, Level.INFO,
            "ATTEMPT_GET_ASSIGNED_SERVICES_OF_REALM", params);

        try {
            OrganizationConfigManager ocm = new OrganizationConfigManager(
                adminSSOToken, realm);
            Set serviceNames = ocm.getAssignedServices(incMandatory);
            Set dynamicServiceNames = getAssignedDynamicServiceNames(
                adminSSOToken, realm);

            if ((dynamicServiceNames != null) && !dynamicServiceNames.isEmpty()
            ) {
                if ((serviceNames != null) && !serviceNames.isEmpty()) {
                    serviceNames.addAll(dynamicServiceNames);
                } else {
                    serviceNames = dynamicServiceNames;
                }
            }

            IOutput outputWriter = getOutputWriter();

            if ((serviceNames != null) && !serviceNames.isEmpty()) {
                String msg = getResourceString(
                    "realm-get-assigned-services-results");
                outputWriter.printlnMessage(FormatUtils.printServiceNames(
                    serviceNames, msg, adminSSOToken));
                outputWriter.printlnMessage(getResourceString(
                    "realm-get-assigned-services-succeed"));
            } else {
                outputWriter.printlnMessage(getResourceString(
                    "realm-get-assigned-services-no-services"));
            }
            
            writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                "SUCCEED_GET_ASSIGNED_SERVICES_OF_REALM", params);
        } catch (SSOException e) {
            String[] args = {realm, e.getMessage()};
            debugError("RealmGetAssignedServices.handleRequest", e);
            writeLog(LogWriter.LOG_ERROR, Level.INFO,
                "FAILED_GET_ASSIGNED_SERVICES_OF_REALM", args);
        } catch (IdRepoException e) {
            String[] args = {realm, e.getMessage()};
            debugError("RealmGetAssignedServices.handleRequest", e);
            writeLog(LogWriter.LOG_ERROR, Level.INFO,
                "FAILED_GET_ASSIGNED_SERVICES_OF_REALM", args);
        } catch (SMSException e) {
            String[] args = {realm, e.getMessage()};
            debugError("RealmGetAssignedServices.handleRequest", e);
            writeLog(LogWriter.LOG_ERROR, Level.INFO,
                "FAILED_GET_ASSIGNED_SERVICES_OF_REALM", args);
            throw new CLIException(e, ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        }
    }

    private Set getAssignedDynamicServiceNames(
        SSOToken adminSSOToken,
        String realm
    ) throws SMSException, IdRepoException, SSOException {
        AMIdentityRepository repo = new AMIdentityRepository(
            adminSSOToken, realm);
        AMIdentity ai = repo.getRealmIdentity();
        return ai.getAssignedServices();
    }
}
