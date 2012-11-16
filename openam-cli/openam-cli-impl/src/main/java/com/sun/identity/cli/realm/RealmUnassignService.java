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
 * $Id: RealmUnassignService.java,v 1.2 2008/06/25 05:42:16 qcheng Exp $
 *
 */

package com.sun.identity.cli.realm;


import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.cli.AuthenticatedCommand;
import com.sun.identity.cli.CLIException;
import com.sun.identity.cli.ExitCodes;
import com.sun.identity.cli.IArgument;
import com.sun.identity.cli.IOutput;
import com.sun.identity.cli.LogWriter;
import com.sun.identity.cli.RequestContext;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.AMIdentityRepository;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.sm.OrganizationConfigManager;
import com.sun.identity.sm.SMSException;
import java.text.MessageFormat;
import java.util.Set;
import java.util.logging.Level;

/**
 * Unassign service from a realm.
 */
public class RealmUnassignService extends AuthenticatedCommand {

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
        String serviceName = getStringOptionValue(IArgument.SERVICE_NAME);
        IOutput outputWriter = getOutputWriter();

        try {
            OrganizationConfigManager ocm = new OrganizationConfigManager(
                adminSSOToken, realm);
            Set assignedServices = ocm.getAssignedServices();

            AMIdentityRepository amir = new AMIdentityRepository(
                adminSSOToken, realm);
            AMIdentity ai = amir.getRealmIdentity();
            Set dynAssignedServices = ai.getAssignedServices();

            String[] params = {realm, serviceName};
            boolean unassigned = false;

            if (assignedServices.contains(serviceName)) {
                writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                    "ATTEMPT_UNASSIGN_SERVICE_FROM_REALM", params);
                ocm.unassignService(serviceName);
                outputWriter.printlnMessage(MessageFormat.format(
                    getResourceString("unassign-service-from-realm-succeed"),
                        (Object[])params));
                writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                    "SUCCEED_UNASSIGN_SERVICE_FROM_REALM", params);
                unassigned = true;
            }

            if (dynAssignedServices.contains(serviceName)) {
                writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                    "ATTEMPT_UNASSIGN_SERVICE_FROM_REALM", params);
                ai.unassignService(serviceName);
                outputWriter.printlnMessage(MessageFormat.format(
                    getResourceString("unassign-service-from-realm-succeed"),
                        (Object[])params));
                writeLog(LogWriter.LOG_ACCESS, Level.INFO,
                    "SUCCEED_UNASSIGN_SERVICE_FROM_REALM", params);
                unassigned = true;
            }

            if (!unassigned) {
                outputWriter.printlnMessage(MessageFormat.format(
                    getResourceString(
                        "unassign-service-from-realm-service-not-assigned"),
                        (Object[])params));
            }
        } catch (SSOException e) {
            String[] args = {realm, e.getMessage()};
            debugError("RealmUnassignService.handleRequest", e);
            writeLog(LogWriter.LOG_ERROR, Level.INFO,
                "FAILED_UNASSIGN_SERVICE_FROM_REALM", args);
            throw new CLIException(e,ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (IdRepoException e) {
            String[] args = {realm, e.getMessage()};
            debugError("RealmUnassignService.handleRequest", e);
            writeLog(LogWriter.LOG_ERROR, Level.INFO,
                "FAILED_UNASSIGN_SERVICE_FROM_REALM", args);
            throw new CLIException(e,ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        } catch (SMSException e) {
            String[] args = {realm, e.getMessage()};
            debugError("RealmUnassignService.handleRequest", e);
            writeLog(LogWriter.LOG_ERROR, Level.INFO,
                "FAILED_UNASSIGN_SERVICE_FROM_REALM", args);
            throw new CLIException(e,ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
        }
    }
}
